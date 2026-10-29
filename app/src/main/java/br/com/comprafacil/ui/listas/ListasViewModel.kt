package br.com.comprafacil.ui.listas

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import br.com.comprafacil.AppContainer
import br.com.comprafacil.data.api.Lista
import br.com.comprafacil.data.api.NovaLista
import br.com.comprafacil.data.api.Resultado
import br.com.comprafacil.data.api.chamar
import java.math.BigDecimal
import kotlinx.coroutines.launch

class ListasViewModel(private val app: AppContainer) : ViewModel() {

    var listas by mutableStateOf<List<Lista>?>(null)
        private set
    var carregando by mutableStateOf(false)
        private set
    var erro by mutableStateOf<String?>(null)
        private set
    var criando by mutableStateOf(false)
        private set
    var erroCriar by mutableStateOf<String?>(null)
        private set

    val ativas get() = listas.orEmpty().filter { !it.concluida }.sortedByDescending { it.id }

    // Concluídas: a mais recente primeiro.
    val concluidas
        get() = listas.orEmpty().filter { it.concluida }
            .sortedWith(compareByDescending<Lista> { it.concluidaEm ?: "" }.thenByDescending { it.id })

    fun carregar() {
        if (carregando) return
        viewModelScope.launch {
            carregando = true
            when (val r = chamar { app.api.listas() }) {
                is Resultado.Ok -> {
                    listas = r.valor
                    erro = null
                }
                is Resultado.Falha -> erro = r.mensagem
            }
            carregando = false
        }
    }

    fun limparErroCriar() {
        erroCriar = null
    }

    fun criar(nome: String, orcamento: BigDecimal?, onCriada: (Int) -> Unit) {
        if (nome.isBlank()) {
            erroCriar = "Dê um nome para a lista."
            return
        }
        viewModelScope.launch {
            criando = true
            when (val r = chamar { app.api.criarLista(NovaLista(nome.trim(), orcamento)) }) {
                is Resultado.Ok -> {
                    listas = listas.orEmpty() + r.valor
                    erroCriar = null
                    onCriada(r.valor.id)
                }
                is Resultado.Falha -> erroCriar = r.mensagem
            }
            criando = false
        }
    }
}
