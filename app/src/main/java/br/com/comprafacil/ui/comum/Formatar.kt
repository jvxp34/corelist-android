package br.com.comprafacil.ui.comum

import androidx.compose.ui.graphics.Color
import br.com.comprafacil.ui.theme.Cores
import java.math.BigDecimal
import java.math.RoundingMode
import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.text.Normalizer
import java.text.NumberFormat
import java.time.OffsetDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

val PT_BR: Locale = Locale.forLanguageTag("pt-BR")

private val formatoMoeda = NumberFormat.getCurrencyInstance(PT_BR)
private val formatoNumero = DecimalFormat("#,##0.###", DecimalFormatSymbols(PT_BR))
private val formatoCampo = DecimalFormat("0.00", DecimalFormatSymbols(PT_BR))
private val formatoCampoQtd = DecimalFormat("0.###", DecimalFormatSymbols(PT_BR))
private val formatoData = DateTimeFormatter.ofPattern("dd/MM/yyyy", PT_BR)
private val formatoDiaMes = DateTimeFormatter.ofPattern("dd/MM", PT_BR)

/** "R$ 24,90" (com espaço comum, que quebra melhor na tela). */
fun moeda(valor: BigDecimal): String =
    synchronized(formatoMoeda) { formatoMoeda.format(valor) }.replace(' ', ' ')

fun numero(valor: BigDecimal): String = synchronized(formatoNumero) { formatoNumero.format(valor) }

/** Valor para preencher um campo de preço: "24,90". */
fun precoParaCampo(valor: BigDecimal?): String =
    valor?.let { synchronized(formatoCampo) { formatoCampo.format(it) } } ?: ""

/** Valor para preencher um campo de quantidade: "1,5". */
fun quantidadeParaCampo(valor: BigDecimal): String =
    synchronized(formatoCampoQtd) { formatoCampoQtd.format(valor) }

/** "2 un" ou "1,25 kg". */
fun quantidadeComMedida(quantidade: BigDecimal, porKg: Boolean): String =
    numero(quantidade) + if (porKg) " kg" else " un"

/** Lê o que o usuário digitou ("24,90", "R$ 1.234,50", "3.5"). */
fun lerDecimal(texto: String): BigDecimal? {
    var limpo = texto.replace("R$", "").replace(" ", "").replace(' '.toString(), "").trim()
    if (limpo.isEmpty()) return null
    if (limpo.contains(',')) limpo = limpo.replace(".", "").replace(',', '.')
    return limpo.toBigDecimalOrNull()?.takeIf { it.signum() >= 0 }
}

private fun dataLocal(iso: String?) = iso?.let {
    runCatching { OffsetDateTime.parse(it).atZoneSameInstant(ZoneId.systemDefault()) }.getOrNull()
}

fun data(iso: String?): String = dataLocal(iso)?.format(formatoData) ?: ""

fun diaMes(iso: String?): String = dataLocal(iso)?.format(formatoDiaMes) ?: ""

/** "SUPERMERCADO DB LTDA" -> "Supermercado Db Ltda". */
fun nomeProprio(texto: String): String = texto.lowercase(PT_BR).split(" ")
    .joinToString(" ") { palavra -> palavra.replaceFirstChar { it.titlecase(PT_BR) } }

/** Para buscas: sem acento e sem maiúsculas. */
fun normalizar(texto: String): String = Normalizer.normalize(texto, Normalizer.Form.NFD)
    .replace(Regex("\\p{Mn}+"), "")
    .lowercase(PT_BR)
    .trim()

// ---- Orçamento (mesmas regras de utils/formatar.ts do site) ----

/** A partir desta fração do orçamento usado, avisamos que está perto do limite. */
private val LIMITE_ALERTA = BigDecimal("0.9")

enum class NivelOrcamento { SEM, OK, PERTO, ESTOUROU }

data class SituacaoOrcamento(
    val nivel: NivelOrcamento,
    val orcamento: BigDecimal,
    val saldo: BigDecimal,
    /** Fração usada, de 0 a 1 (limitada para a barra). */
    val usado: Float,
) {
    val rotulo: String
        get() = when (nivel) {
            NivelOrcamento.SEM -> "Sem orçamento"
            NivelOrcamento.OK -> "Dentro do orçamento"
            NivelOrcamento.PERTO -> "Perto do limite"
            NivelOrcamento.ESTOUROU -> "Passou do orçamento"
        }

    /** "Resta R$ 20,00" ou "Estourou R$ 5,00". */
    val resumo: String
        get() = when (nivel) {
            NivelOrcamento.SEM -> "Sem orçamento"
            NivelOrcamento.ESTOUROU -> "Estourou ${moeda(saldo.negate())}"
            else -> "Resta ${moeda(saldo)}"
        }

    val corTexto: Color
        get() = when (nivel) {
            NivelOrcamento.SEM -> Cores.Secundario
            NivelOrcamento.OK -> Cores.OkTexto
            NivelOrcamento.PERTO -> Cores.PertoTexto
            NivelOrcamento.ESTOUROU -> Cores.EstourouTexto
        }

    val corBarra: Color
        get() = when (nivel) {
            NivelOrcamento.SEM -> Cores.SemBarra
            NivelOrcamento.OK -> Cores.OkBarra
            NivelOrcamento.PERTO -> Cores.PertoBarra
            NivelOrcamento.ESTOUROU -> Cores.EstourouBarra
        }

    val corChip: Color
        get() = when (nivel) {
            NivelOrcamento.SEM -> Cores.SemChip
            NivelOrcamento.OK -> Cores.OkChip
            NivelOrcamento.PERTO -> Cores.PertoChip
            NivelOrcamento.ESTOUROU -> Cores.EstourouChip
        }

    val corChipTexto: Color
        get() = when (nivel) {
            NivelOrcamento.SEM -> Cores.SemChipTexto
            NivelOrcamento.OK -> Cores.OkChipTexto
            NivelOrcamento.PERTO -> Cores.PertoChipTexto
            NivelOrcamento.ESTOUROU -> Cores.EstourouChipTexto
        }
}

fun situacaoDoOrcamento(total: BigDecimal, orcamento: BigDecimal?): SituacaoOrcamento {
    if (orcamento == null || orcamento.signum() <= 0) {
        return SituacaoOrcamento(NivelOrcamento.SEM, BigDecimal.ZERO, BigDecimal.ZERO, 0f)
    }
    val saldo = orcamento - total
    val usado = total.divide(orcamento, 4, RoundingMode.HALF_UP)
    val nivel = when {
        saldo.signum() < 0 -> NivelOrcamento.ESTOUROU
        usado >= LIMITE_ALERTA -> NivelOrcamento.PERTO
        else -> NivelOrcamento.OK
    }
    return SituacaoOrcamento(nivel, orcamento, saldo, usado.toFloat().coerceIn(0f, 1f))
}
