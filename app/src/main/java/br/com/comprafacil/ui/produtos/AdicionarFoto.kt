package br.com.comprafacil.ui.produtos

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.PhotoCamera
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import br.com.comprafacil.AppContainer
import br.com.comprafacil.data.api.CodigoDeBarras
import br.com.comprafacil.data.api.Produto
import br.com.comprafacil.data.api.Resultado
import br.com.comprafacil.data.api.chamar
import br.com.comprafacil.ui.comum.appViewModel
import br.com.comprafacil.ui.theme.Cores
import kotlinx.coroutines.launch

class AdicionarFotoViewModel(private val app: AppContainer, private val produtoId: Int) : ViewModel() {
    var salvando by mutableStateOf(false)
        private set
    var aviso by mutableStateOf<String?>(null)

    fun usarCodigo(lido: String, onCompletar: (Produto) -> Unit) {
        val codigo = lido.filter(Char::isDigit)
        if (codigo.isEmpty()) return
        viewModelScope.launch {
            salvando = true
            aviso = null
            when (val r = chamar { app.api.completarPeloCodigo(produtoId, CodigoDeBarras(codigo)) }) {
                is Resultado.Ok -> {
                    val (produto, encontrado) = r.valor
                    onCompletar(produto)
                    if (!encontrado || produto.imagem.isNullOrBlank()) {
                        aviso = "Código salvo, mas esse produto ainda não tem foto nas bases abertas."
                    }
                }
                is Resultado.Falha -> aviso = r.mensagem
            }
            salvando = false
        }
    }
}

/**
 * Para produto sem foto (por exemplo, veio da nota fiscal): lê o código de
 * barras da embalagem e puxa foto, marca e embalagem (só o que estiver vazio).
 */
@Composable
fun AdicionarFoto(produtoId: Int, nome: String, onCompletar: (Produto) -> Unit) {
    val vm = appViewModel(key = "foto-$produtoId") { AdicionarFotoViewModel(it, produtoId) }
    var lendo by remember { mutableStateOf(false) }
    val context = LocalContext.current
    val pedir = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { ok ->
        if (ok) lendo = true
    }

    Column {
        Row(
            Modifier
                .clickable(enabled = !vm.salvando, onClickLabel = "Adicionar foto de $nome pelo código de barras") {
                    vm.aviso = null
                    val temCamera = ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) ==
                        PackageManager.PERMISSION_GRANTED
                    if (temCamera) lendo = true else pedir.launch(Manifest.permission.CAMERA)
                }
                .padding(vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(Icons.Outlined.PhotoCamera, null, tint = Cores.Marca, modifier = Modifier.size(14.dp))
            Text(
                if (vm.salvando) "Buscando foto…" else "Adicionar foto",
                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                color = Cores.Marca,
                modifier = Modifier.padding(start = 6.dp),
            )
        }
        vm.aviso?.let {
            Text(it, style = MaterialTheme.typography.labelSmall, color = Cores.PertoTexto)
        }
    }

    if (lendo) {
        DialogoLerCodigo(
            titulo = "Adicionar foto",
            dica = "Aponte para o código de barras da embalagem deste produto.",
            onFechar = { lendo = false },
            onCodigo = {
                lendo = false
                vm.usarCodigo(it, onCompletar)
            },
        )
    }
}

/** Crédito exigido pela licença das bases abertas (igual ao site). */
@Composable
fun CreditoOpenFoodFacts(modifier: Modifier = Modifier) {
    Text(
        "Fotos e dados de alguns produtos: Open Food Facts (fotos em CC BY-SA, dados em ODbL).",
        style = MaterialTheme.typography.labelSmall,
        color = Cores.Secundario,
        modifier = modifier,
    )
}
