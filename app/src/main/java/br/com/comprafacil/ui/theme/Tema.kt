package br.com.comprafacil.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.ExperimentalTextApi
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import br.com.comprafacil.R

// Mesmas cores do site (tokens do Tailwind em src/index.css).
object Cores {
    val Marca = Color(0xFF4F46E5)
    val MarcaEscura = Color(0xFF4338CA)
    val MarcaClara = Color(0xFFEEF2FF)
    val Texto = Color(0xFF0F172A)
    val Secundario = Color(0xFF64748B)
    val Linha = Color(0xFFE2E8F0)
    val Fundo = Color(0xFFF8FAFC)
    val Superficie = Color.White

    // Verde, âmbar e vermelho: só para a situação do orçamento.
    val OkTexto = Color(0xFF15803D)
    val OkBarra = Color(0xFF16A34A)
    val OkChip = Color(0xFFDCFCE7)
    val OkChipTexto = Color(0xFF166534)
    val PertoTexto = Color(0xFFB45309)
    val PertoBarra = Color(0xFFF59E0B)
    val PertoChip = Color(0xFFFEF3C7)
    val PertoChipTexto = Color(0xFF92400E)
    val EstourouTexto = Color(0xFFB91C1C)
    val EstourouBarra = Color(0xFFEF4444)
    val EstourouChip = Color(0xFFFEE2E2)
    val EstourouChipTexto = Color(0xFF991B1B)
    val SemChip = Color(0xFFF1F5F9)
    val SemChipTexto = Color(0xFF334155)
    val SemBarra = Color(0xFFCBD5E1)
}

val Poppins = FontFamily(
    Font(R.font.poppins_semibold, FontWeight.SemiBold),
    Font(R.font.poppins_bold, FontWeight.Bold),
)

@OptIn(ExperimentalTextApi::class)
private fun inter(peso: Int) = Font(
    R.font.inter,
    FontWeight(peso),
    variationSettings = FontVariation.Settings(FontVariation.weight(peso)),
)

val Inter = FontFamily(inter(400), inter(500), inter(600), inter(700))

private val esquema = lightColorScheme(
    primary = Cores.Marca,
    onPrimary = Color.White,
    primaryContainer = Cores.MarcaClara,
    onPrimaryContainer = Cores.MarcaEscura,
    secondary = Cores.MarcaEscura,
    onSecondary = Color.White,
    secondaryContainer = Cores.MarcaClara,
    onSecondaryContainer = Cores.MarcaEscura,
    background = Cores.Fundo,
    onBackground = Cores.Texto,
    surface = Cores.Superficie,
    onSurface = Cores.Texto,
    surfaceVariant = Color(0xFFF1F5F9),
    onSurfaceVariant = Cores.Secundario,
    surfaceContainerLowest = Color.White,
    surfaceContainerLow = Color.White,
    surfaceContainer = Color.White,
    surfaceContainerHigh = Color.White,
    surfaceContainerHighest = Color(0xFFF1F5F9),
    outline = Color(0xFFCBD5E1),
    outlineVariant = Cores.Linha,
    error = Color(0xFFDC2626),
    onError = Color.White,
    errorContainer = Color(0xFFFEE2E2),
    onErrorContainer = Color(0xFF991B1B),
)

private val base = Typography()

private fun TextStyle.titulo(peso: FontWeight = FontWeight.SemiBold) =
    copy(fontFamily = Poppins, fontWeight = peso, color = Color.Unspecified)

private fun TextStyle.corpo() = copy(fontFamily = Inter)

private val tipografia = Typography(
    displayLarge = base.displayLarge.titulo(FontWeight.Bold),
    displayMedium = base.displayMedium.titulo(FontWeight.Bold),
    displaySmall = base.displaySmall.titulo(FontWeight.Bold),
    headlineLarge = base.headlineLarge.titulo(FontWeight.Bold),
    headlineMedium = base.headlineMedium.titulo(FontWeight.Bold),
    headlineSmall = base.headlineSmall.titulo(),
    titleLarge = base.titleLarge.titulo().copy(fontSize = 20.sp),
    titleMedium = base.titleMedium.titulo(),
    titleSmall = base.titleSmall.corpo().copy(fontWeight = FontWeight.SemiBold),
    bodyLarge = base.bodyLarge.corpo(),
    bodyMedium = base.bodyMedium.corpo(),
    bodySmall = base.bodySmall.corpo(),
    labelLarge = base.labelLarge.corpo().copy(fontWeight = FontWeight.SemiBold),
    labelMedium = base.labelMedium.corpo().copy(fontWeight = FontWeight.Medium),
    labelSmall = base.labelSmall.corpo().copy(fontWeight = FontWeight.Medium),
)

private val formas = Shapes(
    extraSmall = RoundedCornerShape(8.dp),
    small = RoundedCornerShape(10.dp),
    medium = RoundedCornerShape(14.dp),
    large = RoundedCornerShape(18.dp),
    extraLarge = RoundedCornerShape(24.dp),
)

@Composable
fun CompraFacilTema(conteudo: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = esquema,
        typography = tipografia,
        shapes = formas,
        content = conteudo,
    )
}
