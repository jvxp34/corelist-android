package br.com.comprafacil.data.api

import java.io.IOException
import kotlinx.coroutines.CancellationException
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.jsonObject
import retrofit2.HttpException

sealed interface Resultado<out T> {
    data class Ok<T>(val valor: T) : Resultado<T>
    data class Falha(
        val mensagem: String,
        val status: Int? = null,
        val corpo: JsonObject? = null,
    ) : Resultado<Nothing>
}

/** Roda uma chamada da API e transforma erros em mensagens em português. */
suspend fun <T> chamar(bloco: suspend () -> T): Resultado<T> = try {
    Resultado.Ok(bloco())
} catch (e: CancellationException) {
    throw e
} catch (e: HttpException) {
    falhaHttp(e.code(), e.response()?.errorBody()?.string())
} catch (_: IOException) {
    Resultado.Falha("Não foi possível falar com o servidor. Confira sua internet e tente de novo.")
} catch (_: SerializationException) {
    Resultado.Falha("O servidor respondeu de um jeito inesperado.")
} catch (_: IllegalArgumentException) {
    Resultado.Falha("O servidor respondeu de um jeito inesperado.")
}

/** Para chamadas que devolvem Response (DELETE, logout): erro vira exceção. */
fun <T> retrofit2.Response<T>.exigirSucesso(): retrofit2.Response<T> {
    if (!isSuccessful) throw HttpException(this)
    return this
}

private val ROTULOS = mapOf(
    "email" to "E-mail",
    "password" to "Senha",
    "first_name" to "Nome",
    "name" to "Nome",
    "budget" to "Orçamento",
    "price" to "Preço",
    "quantity" to "Quantidade",
    "measure" to "Medida",
    "link" to "Link",
    "barcode" to "Código de barras",
    "itens" to "Itens",
)

private fun falhaHttp(status: Int, texto: String?): Resultado.Falha {
    val corpo = texto?.let { runCatching { JsonApi.parseToJsonElement(it).jsonObject }.getOrNull() }
    val mensagem = corpo?.let { primeiraMensagem(it) } ?: when (status) {
        401 -> "Sua sessão expirou. Entre de novo."
        403 -> "Você não tem permissão para fazer isso."
        404 -> "Não encontrado."
        in 500..599 -> "O servidor está com problemas. Tente de novo daqui a pouco."
        else -> "Algo deu errado (erro $status)."
    }
    return Resultado.Falha(mensagem, status, corpo)
}

private fun primeiraMensagem(corpo: JsonObject): String? {
    (corpo["detail"] as? JsonPrimitive)?.let { return it.content }
    for ((campo, valor) in corpo) {
        val texto = textoDe(valor) ?: continue
        val rotulo = ROTULOS[campo]
        return if (rotulo == null) texto else "$rotulo: $texto"
    }
    return null
}

private fun textoDe(valor: JsonElement): String? = when (valor) {
    is JsonPrimitive -> valor.content
    is JsonArray -> valor.firstNotNullOfOrNull { textoDe(it) }
    is JsonObject -> primeiraMensagem(valor)
}
