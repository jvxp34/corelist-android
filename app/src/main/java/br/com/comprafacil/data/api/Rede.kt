package br.com.comprafacil.data.api

import br.com.comprafacil.BuildConfig
import br.com.comprafacil.data.Tokens
import java.io.IOException
import java.util.concurrent.TimeUnit
import kotlinx.serialization.json.Json
import okhttp3.Authenticator
import okhttp3.Interceptor
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response
import okhttp3.Route
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory

val JsonApi = Json {
    ignoreUnknownKeys = true
    explicitNulls = false
    coerceInputValues = true
}

private val TIPO_JSON = "application/json".toMediaType()

// Rotas que não levam o token: um token vencido nelas daria 401 à toa.
private val ROTAS_SEM_TOKEN = listOf("auth/login/", "auth/register/", "auth/login/refresh/", "auth/logout/")

private fun semToken(request: Request): Boolean =
    ROTAS_SEM_TOKEN.any { request.url.encodedPath.endsWith("/$it") }

/** Resultado da renovação do token. */
private sealed interface Renovacao {
    data class Nova(val tokens: Tokens) : Renovacao
    /** O servidor recusou o refresh: a sessão acabou. */
    data object Recusada : Renovacao
    /** Sem internet ou servidor fora: não dá para saber, a sessão continua. */
    data object SemResposta : Renovacao
}

/** Onde a Rede lê e grava os tokens (a Sessao no app; uma versão em memória nos testes). */
interface GuardaTokens {
    fun atual(): Tokens?
    fun salvarBloqueando(tokens: Tokens)
    fun limparBloqueando(expirou: Boolean)
}

class Rede(private val sessao: GuardaTokens, url: String = BuildConfig.API_URL) {

    val baseUrl: String = url.trimEnd('/') + "/"

    private val clienteBase: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        // A leitura da nota consulta a Sefaz, que às vezes demora.
        .readTimeout(40, TimeUnit.SECONDS)
        .apply {
            if (BuildConfig.DEBUG) {
                addInterceptor(HttpLoggingInterceptor().setLevel(HttpLoggingInterceptor.Level.BASIC))
            }
        }
        .build()

    private val cliente: OkHttpClient = clienteBase.newBuilder()
        .addInterceptor(ColocaToken())
        .authenticator(RenovaToken())
        .build()

    val api: CompraFacilApi = Retrofit.Builder()
        .baseUrl(baseUrl)
        .client(cliente)
        .addConverterFactory(JsonApi.asConverterFactory(TIPO_JSON))
        .build()
        .create(CompraFacilApi::class.java)

    private inner class ColocaToken : Interceptor {
        override fun intercept(chain: Interceptor.Chain): Response {
            val request = chain.request()
            val access = sessao.atual()?.access
            if (access == null || semToken(request)) return chain.proceed(request)
            return chain.proceed(comToken(request, access))
        }
    }

    /**
     * Em 401: renova o token uma vez e repete a chamada. O refresh é rotacionado
     * (o antigo deixa de valer), então só uma renovação roda por vez e o par
     * novo é salvo antes de liberar as outras chamadas.
     */
    private inner class RenovaToken : Authenticator {
        private val trava = Any()

        override fun authenticate(route: Route?, response: Response): Request? {
            // priorResponse != null: esta chamada já foi repetida uma vez.
            if (semToken(response.request) || response.priorResponse != null) return null

            synchronized(trava) {
                val atual = sessao.atual() ?: return null
                val usado = response.request.header("Authorization")?.removePrefix("Bearer ")

                // Outra chamada já renovou enquanto esta esperava.
                if (usado != null && usado != atual.access) {
                    return comToken(response.request, atual.access)
                }

                return when (val r = renovar(atual.refresh)) {
                    is Renovacao.Nova -> {
                        sessao.salvarBloqueando(r.tokens)
                        comToken(response.request, r.tokens.access)
                    }
                    Renovacao.Recusada -> {
                        sessao.limparBloqueando(expirou = true)
                        null
                    }
                    Renovacao.SemResposta -> null
                }
            }
        }

        private fun renovar(refresh: String): Renovacao {
            val corpo = JsonApi.encodeToString(RefreshRequest.serializer(), RefreshRequest(refresh))
            val request = Request.Builder()
                .url(baseUrl + "auth/login/refresh/")
                .post(corpo.toRequestBody(TIPO_JSON))
                .build()

            return try {
                clienteBase.newCall(request).execute().use { resposta ->
                    when {
                        resposta.isSuccessful -> {
                            val par = JsonApi.decodeFromString(TokenPair.serializer(), resposta.body!!.string())
                            Renovacao.Nova(Tokens(par.access, par.refresh ?: refresh))
                        }
                        resposta.code in 400..499 -> Renovacao.Recusada
                        else -> Renovacao.SemResposta
                    }
                }
            } catch (_: IOException) {
                Renovacao.SemResposta
            }
        }
    }

    private fun comToken(request: Request, access: String) =
        request.newBuilder().header("Authorization", "Bearer $access").build()
}
