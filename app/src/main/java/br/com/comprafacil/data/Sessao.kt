package br.com.comprafacil.data

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import br.com.comprafacil.data.api.GuardaTokens
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking

private val Context.dataStore by preferencesDataStore(name = "sessao")

data class Tokens(val access: String, val refresh: String)

/**
 * Guarda os tokens no DataStore e mantém uma cópia em memória, que o
 * OkHttp lê sem suspender.
 */
class Sessao(private val context: Context) : GuardaTokens {

    private val chaveAccess = stringPreferencesKey("access_token")
    private val chaveRefresh = stringPreferencesKey("refresh_token")

    private val _tokens = MutableStateFlow<Tokens?>(null)
    val tokens: StateFlow<Tokens?> = _tokens.asStateFlow()

    private val _expirou = MutableStateFlow(false)

    /** true quando a sessão acabou sozinha (o login mostra "Sua sessão expirou"). */
    val expirou: StateFlow<Boolean> = _expirou.asStateFlow()

    /** Lido uma vez ao abrir o app. */
    fun carregar() = runBlocking {
        val dados = context.dataStore.data.first()
        val access = dados[chaveAccess]
        val refresh = dados[chaveRefresh]
        _tokens.value = if (access != null && refresh != null) Tokens(access, refresh) else null
    }

    override fun atual(): Tokens? = _tokens.value

    suspend fun salvar(tokens: Tokens) {
        _tokens.value = tokens
        _expirou.value = false
        context.dataStore.edit {
            it[chaveAccess] = tokens.access
            it[chaveRefresh] = tokens.refresh
        }
    }

    /** Para o Authenticator do OkHttp, que roda fora de corrotinas. */
    override fun salvarBloqueando(tokens: Tokens) = runBlocking { salvar(tokens) }

    suspend fun limpar(expirou: Boolean = false) {
        _expirou.value = expirou
        _tokens.value = null
        context.dataStore.edit {
            it.remove(chaveAccess)
            it.remove(chaveRefresh)
        }
    }

    override fun limparBloqueando(expirou: Boolean) = runBlocking { limpar(expirou) }

    fun avisoVisto() {
        _expirou.value = false
    }
}
