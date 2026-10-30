package br.com.comprafacil.ui.comum

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Navigation
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.foundation.BorderStroke
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import br.com.comprafacil.R
import br.com.comprafacil.data.api.Loja
import br.com.comprafacil.data.api.MEDIDA_KG
import br.com.comprafacil.ui.theme.Cores
import java.math.BigDecimal
import java.net.URLEncoder
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.temporal.ChronoUnit

private val MANAUS: ZoneId = ZoneId.of("America/Manaus")

/** "Assaí Atacadista" quando a rede é conhecida; senão a razão social arrumada. */
fun nomeDaLoja(loja: Loja): String = loja.marca?.takeIf { it.isNotBlank() } ?: nomeDaLoja(loja.nome)

/** "SENDAS DISTRIBUIDORA S/A" -> "Sendas Distribuidora S/A" (igual ao site). */
fun nomeDaLoja(texto: String): String =
    nomeProprio(texto).split(" ").joinToString(" ") { palavra ->
        if (palavra.lowercase(PT_BR) in setOf("ltda", "s/a", "me", "epp")) palavra.uppercase(PT_BR) else palavra
    }

/** URL do Google Maps já com a rota até a loja. */
fun linkComoChegar(loja: Loja): String {
    val destino = loja.endereco?.takeIf { it.isNotBlank() }?.let { "${loja.nome}, $it" } ?: "${loja.nome}, Manaus, AM"
    val codificado = URLEncoder.encode(destino, "UTF-8")
    return "https://www.google.com/maps/dir/?api=1&destination=$codificado&travelmode=driving&dir_action=navigate"
}

fun abrirComoChegar(context: Context, loja: Loja) {
    val intent = Intent(Intent.ACTION_VIEW, Uri.parse(linkComoChegar(loja)))
    try {
        context.startActivity(intent)
    } catch (_: ActivityNotFoundException) {
        Toast.makeText(context, "Nenhum app de mapas encontrado.", Toast.LENGTH_SHORT).show()
    }
}

/** "hoje", "ontem" ou "há 5 dias", contando os dias em Manaus. */
fun quandoFoi(iso: String, hoje: LocalDate = LocalDate.now(MANAUS)): String {
    val dia = runCatching { Instant.parse(iso).atZone(MANAUS).toLocalDate() }
        .recoverCatching { java.time.OffsetDateTime.parse(iso).atZoneSameInstant(MANAUS).toLocalDate() }
        .getOrNull() ?: return ""
    val dias = ChronoUnit.DAYS.between(dia, hoje)
    return when {
        dias <= 0 -> "hoje"
        dias == 1L -> "ontem"
        else -> "há $dias dias"
    }
}

/** "R$ 6,99" ou "R$ 6,99/kg". */
fun precoComMedida(preco: BigDecimal, medida: String): String =
    moeda(preco) + if (medida == MEDIDA_KG) "/kg" else ""

private fun logoDa(icone: String?): Int? = when (icone) {
    "assai" -> R.drawable.loja_assai
    "atacadao" -> R.drawable.loja_atacadao
    "carrefour" -> R.drawable.loja_carrefour
    "db" -> R.drawable.loja_db
    else -> null
}

/** "A C Rodrigues Da Silva" -> "AC". */
private fun iniciais(nome: String): String =
    nome.replace(Regex("[^\\p{L}\\s]"), " ").split(Regex("\\s+"))
        .filter { it.isNotEmpty() }
        .take(2)
        .joinToString("") { it.first().uppercase(PT_BR) }

/** Logo da rede (redes conhecidas) ou um círculo com as iniciais da loja. */
@Composable
fun LogoDaLoja(loja: Loja, tamanho: Dp = 40.dp) {
    val logo = logoDa(loja.icone)
    val forma = CircleShape
    val base = Modifier.size(tamanho).clip(forma).border(1.dp, Cores.Linha, forma)
    if (logo != null) {
        Image(
            painter = painterResource(logo),
            contentDescription = null,
            modifier = base.background(androidx.compose.ui.graphics.Color.White).padding(2.dp),
        )
    } else {
        Box(base.background(Cores.MarcaClara), contentAlignment = Alignment.Center) {
            Text(
                iniciais(nomeDaLoja(loja)),
                color = Cores.MarcaEscura,
                fontWeight = FontWeight.SemiBold,
                fontSize = if (tamanho < 32.dp) 11.sp else 14.sp,
            )
        }
    }
}

@Composable
fun BotaoComoChegar(loja: Loja, pequeno: Boolean = false) {
    val context = LocalContext.current
    OutlinedButton(
        onClick = { abrirComoChegar(context, loja) },
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(1.dp, Cores.MarcaClara),
        colors = ButtonDefaults.outlinedButtonColors(containerColor = Cores.MarcaClara, contentColor = Cores.MarcaEscura),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = if (pequeno) 10.dp else 12.dp),
        modifier = Modifier.height(if (pequeno) 36.dp else 40.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Outlined.Navigation, null, modifier = Modifier.size(if (pequeno) 14.dp else 16.dp))
            Text(
                "Como chegar",
                fontSize = if (pequeno) 12.sp else 14.sp,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.padding(start = 6.dp),
            )
        }
    }
}
