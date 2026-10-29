package br.com.comprafacil.ui.auth

import android.util.Patterns
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import br.com.comprafacil.AppContainer
import br.com.comprafacil.data.Tokens
import br.com.comprafacil.data.api.CadastroRequest
import br.com.comprafacil.data.api.LoginRequest
import br.com.comprafacil.data.api.Resultado
import br.com.comprafacil.data.api.chamar
import kotlinx.coroutines.launch

class AuthViewModel(private val app: AppContainer) : ViewModel() {

    var nome by mutableStateOf("")
    var email by mutableStateOf("")
    var senha by mutableStateOf("")
    var carregando by mutableStateOf(false)
        private set
    var erro by mutableStateOf<String?>(null)
        private set

    val expirou = app.sessao.expirou

    fun avisoVisto() = app.sessao.avisoVisto()

    fun entrar() {
        val emailLimpo = email.trim()
        erro = when {
            emailLimpo.isEmpty() || senha.isEmpty() -> "Informe o e-mail e a senha."
            else -> null
        }
        if (erro != null) return

        viewModelScope.launch {
            carregando = true
            erro = fazerLogin(emailLimpo, senha)
            carregando = false
        }
    }

    fun cadastrar() {
        val emailLimpo = email.trim()
        erro = when {
            nome.isBlank() -> "Informe seu nome."
            !Patterns.EMAIL_ADDRESS.matcher(emailLimpo).matches() -> "Informe um e-mail válido."
            senha.length < 8 -> "A senha precisa ter pelo menos 8 caracteres."
            else -> null
        }
        if (erro != null) return

        viewModelScope.launch {
            carregando = true
            when (val r = chamar { app.api.cadastrar(CadastroRequest(nome.trim(), emailLimpo, senha)) }) {
                is Resultado.Ok -> erro = fazerLogin(emailLimpo, senha)
                is Resultado.Falha -> erro = r.mensagem
            }
            carregando = false
        }
    }

    /** Devolve a mensagem de erro, ou null se entrou. A navegação segue a sessão. */
    private suspend fun fazerLogin(email: String, senha: String): String? =
        when (val r = chamar { app.api.entrar(LoginRequest(email, senha)) }) {
            is Resultado.Ok -> {
                val refresh = r.valor.refresh ?: return "O servidor não devolveu a sessão."
                app.sessao.salvar(Tokens(r.valor.access, refresh))
                null
            }
            is Resultado.Falha ->
                if (r.status == 401) "E-mail ou senha incorretos." else r.mensagem
        }
}
