package br.com.comprafacil.data.api

import java.math.BigDecimal
import kotlinx.serialization.KSerializer
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.json.JsonDecoder
import kotlinx.serialization.json.jsonPrimitive

/** O DRF manda decimais como texto ("24.90"). Aceita texto ou número e envia texto. */
object DecimalSerializer : KSerializer<BigDecimal> {
    override val descriptor = PrimitiveSerialDescriptor("Decimal", PrimitiveKind.STRING)

    override fun deserialize(decoder: Decoder): BigDecimal {
        val texto = if (decoder is JsonDecoder) {
            decoder.decodeJsonElement().jsonPrimitive.content
        } else {
            decoder.decodeString()
        }
        return texto.toBigDecimal()
    }

    override fun serialize(encoder: Encoder, value: BigDecimal) {
        encoder.encodeString(value.toPlainString())
    }
}

typealias Decimal = @Serializable(with = DecimalSerializer::class) BigDecimal

// ---- Conta ----

@Serializable
data class LoginRequest(val email: String, val password: String)

@Serializable
data class CadastroRequest(
    @SerialName("first_name") val nome: String,
    val email: String,
    val password: String,
)

@Serializable
data class RefreshRequest(val refresh: String)

@Serializable
data class TokenPair(val access: String, val refresh: String? = null)

@Serializable
data class Usuario(
    val id: Int,
    val email: String,
    @SerialName("first_name") val nome: String = "",
)

// ---- Listas ----

const val MEDIDA_UNIDADE = "unit"
const val MEDIDA_KG = "kg"

@Serializable
data class Item(
    val id: Int,
    val product: Int,
    val quantity: Decimal = BigDecimal.ONE,
    val measure: String = MEDIDA_UNIDADE,
    val price: Decimal? = null,
    @SerialName("is_completed") val comprado: Boolean = false,
) {
    val porKg: Boolean get() = measure == MEDIDA_KG

    /** Calculado aqui para refletir na hora as mudanças locais. */
    val subtotal: BigDecimal? get() = price?.multiply(quantity)
}

@Serializable
data class Lista(
    val id: Int,
    val name: String,
    val budget: Decimal? = null,
    @SerialName("is_completed") val concluida: Boolean = false,
    @SerialName("completed_at") val concluidaEm: String? = null,
    val store: String? = null,
    val items: List<Item> = emptyList(),
    @SerialName("created_at") val criadaEm: String? = null,
) {
    val total: BigDecimal
        get() = items.fold(BigDecimal.ZERO) { soma, item -> soma + (item.subtotal ?: BigDecimal.ZERO) }
}

@Serializable
data class NovaLista(val name: String, val budget: Decimal? = null)

@Serializable
data class NovoItem(
    val product: Int,
    val quantity: Decimal,
    val measure: String,
    val price: Decimal? = null,
)

// ---- Produtos ----

@Serializable
data class Produto(
    val id: Int,
    val name: String,
    val price: Decimal? = null,
    val unit: String? = null,
    val brand: String? = null,
    @SerialName("package_size") val embalagem: String? = null,
    val barcode: String? = null,
    @SerialName("image_url") val imagem: String? = null,
    @SerialName("data_source") val origem: String? = null,
) {
    /** "Camil · 5 kg": marca e embalagem, o que existir (igual ao site). */
    val detalhes: String
        get() = listOf(brand?.trim(), (embalagem?.takeIf { it.isNotBlank() } ?: unit)?.trim())
            .filter { !it.isNullOrBlank() }
            .joinToString(" · ")
}

@Serializable
data class NovoProduto(
    val name: String,
    val brand: String = "",
    @SerialName("package_size") val embalagem: String = "",
    val barcode: String = "",
    @SerialName("image_url") val imagem: String = "",
    @SerialName("data_source") val origem: String = "manual",
)

@Serializable
data class SugestaoProduto(
    val name: String = "",
    val brand: String? = null,
    @SerialName("package_size") val embalagem: String? = null,
    @SerialName("image_url") val imagem: String? = null,
    val barcode: String = "",
    @SerialName("data_source") val origem: String? = null,
)

@Serializable
data class ConsultaCodigo(
    val origem: String,
    val produto: Produto? = null,
    val sugestao: SugestaoProduto? = null,
)

// ---- Nota fiscal (NFC-e) ----

@Serializable
data class LinkNota(val link: String)

@Serializable
data class ProdutoRef(val id: Int, val name: String)

@Serializable
data class ItemNota(
    val nome: String,
    @SerialName("codigo_loja") val codigoLoja: String? = null,
    val unidade: String? = null,
    val medida: String = MEDIDA_UNIDADE,
    val quantidade: Decimal,
    val preco: Decimal,
    val total: Decimal,
    val produto: ProdutoRef? = null,
)

@Serializable
data class NotaLida(
    val chave: String = "",
    val loja: String = "",
    val cnpj: String? = null,
    @SerialName("emitida_em") val emitidaEm: String? = null,
    val total: Decimal,
    val itens: List<ItemNota> = emptyList(),
    @SerialName("lista_importada") val listaImportada: Int? = null,
)

@Serializable
data class ItemImportar(
    val nome: String,
    val quantidade: Decimal,
    val medida: String,
    val preco: Decimal,
)

@Serializable
data class ImportarNota(
    val link: String,
    val nome: String,
    val itens: List<ItemImportar>,
)
