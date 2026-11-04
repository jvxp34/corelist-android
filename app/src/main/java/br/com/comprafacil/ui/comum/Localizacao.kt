package br.com.comprafacil.ui.comum

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationManager
import android.os.CancellationSignal
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import androidx.core.location.LocationManagerCompat
import java.util.Locale
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withTimeoutOrNull

/** Posição do aparelho (só a usada para buscar lojas perto; não é guardada). */
data class Origem(val lat: Double, val lng: Double)

enum class EstadoLocalizacao { DESLIGADA, BUSCANDO, ATIVA, NEGADA, INDISPONIVEL }

/** O que fazer quando a localização não vem (mesma ideia do site). */
fun mensagemDaLocalizacao(estado: EstadoLocalizacao): String? = when (estado) {
    EstadoLocalizacao.NEGADA ->
        "A localização está bloqueada para o app. Abra Configurações › Apps › CompraFácil › Permissões › " +
            "Localização › Permitir, e tente de novo."
    EstadoLocalizacao.INDISPONIVEL ->
        "Não consegui achar sua localização. Confira se a localização (GPS) do celular está ligada e tente de novo."
    else -> null
}

/** "350 m" ou "1,2 km". */
fun textoDistancia(km: Double?): String? = when {
    km == null -> null
    km < 1 -> "${Math.round(km * 1000)} m"
    else -> String.format(Locale.forLanguageTag("pt-BR"), "%.1f km", km).replace(",0 km", " km")
}

class Localizacao internal constructor(
    private val context: Context,
    private val scope: CoroutineScope,
) {
    var estado by mutableStateOf(EstadoLocalizacao.DESLIGADA)
        private set
    var origem by mutableStateOf<Origem?>(null)
        private set
    val mensagem: String? get() = mensagemDaLocalizacao(estado)

    internal var pedirPermissao: () -> Unit = {}

    private fun temPermissao() = listOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION)
        .any { ContextCompat.checkSelfPermission(context, it) == PackageManager.PERMISSION_GRANTED }

    /** Só quando a pessoa toca em "Lojas perto de mim". */
    fun pedir() {
        if (temPermissao()) buscar() else pedirPermissao()
    }

    internal fun aoResponderPermissao(concedida: Boolean) {
        if (concedida) buscar() else {
            origem = null
            estado = EstadoLocalizacao.NEGADA
        }
    }

    fun desligar() {
        origem = null
        estado = EstadoLocalizacao.DESLIGADA
    }

    private fun buscar() {
        estado = EstadoLocalizacao.BUSCANDO
        scope.launch {
            val posicao = posicaoAtual(context)
            if (posicao != null) {
                origem = Origem(posicao.latitude, posicao.longitude)
                estado = EstadoLocalizacao.ATIVA
            } else {
                origem = null
                estado = EstadoLocalizacao.INDISPONIVEL
            }
        }
    }
}

/** Localização do aparelho, pedida só quando a pessoa quer ("Perto de mim"). */
@Composable
fun rememberLocalizacao(): Localizacao {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val localizacao = remember { Localizacao(context.applicationContext, scope) }
    val pedir = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { r ->
        localizacao.aoResponderPermissao(r.values.any { it })
    }
    localizacao.pedirPermissao = {
        pedir.launch(arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION))
    }
    return localizacao
}

/**
 * Primeiro uma posição recente (até 5 min) ou rápida pela rede; se não vier,
 * uma segunda tentativa pelo GPS, que demora mais (igual ao site).
 */
@SuppressLint("MissingPermission")
private suspend fun posicaoAtual(context: Context): Location? {
    val lm = context.getSystemService(LocationManager::class.java) ?: return null
    if (!LocationManagerCompat.isLocationEnabled(lm)) return null

    val recente = listOf(LocationManager.NETWORK_PROVIDER, LocationManager.GPS_PROVIDER)
        .filter { runCatching { lm.isProviderEnabled(it) }.getOrDefault(false) }
        .mapNotNull { runCatching { lm.getLastKnownLocation(it) }.getOrNull() }
        .filter { System.currentTimeMillis() - it.time < 5 * 60 * 1000 }
        .maxByOrNull { it.time }
    if (recente != null) return recente

    for ((provedor, espera) in listOf(LocationManager.NETWORK_PROVIDER to 10_000L, LocationManager.GPS_PROVIDER to 20_000L)) {
        if (!runCatching { lm.isProviderEnabled(provedor) }.getOrDefault(false)) continue
        val achou = withTimeoutOrNull(espera) { umaPosicao(lm, provedor, context) }
        if (achou != null) return achou
    }
    return null
}

@SuppressLint("MissingPermission")
private suspend fun umaPosicao(lm: LocationManager, provedor: String, context: Context): Location? =
    suspendCancellableCoroutine { cont ->
        val cancelar = CancellationSignal()
        cont.invokeOnCancellation { cancelar.cancel() }
        LocationManagerCompat.getCurrentLocation(lm, provedor, cancelar, ContextCompat.getMainExecutor(context)) {
            if (cont.isActive) cont.resumeWith(Result.success(it))
        }
    }
