package br.com.comprafacil.ui.compra

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import br.com.comprafacil.AppContainer
import br.com.comprafacil.data.api.Item
import br.com.comprafacil.data.api.Lista
import br.com.comprafacil.data.api.NovoItem
import br.com.comprafacil.data.api.NovoProduto
import br.com.comprafacil.data.api.Produto
import br.com.comprafacil.data.api.Resultado
import br.com.comprafacil.data.api.chamar
import br.com.comprafacil.data.api.exigirSucesso
import java.math.BigDecimal
import kotlinx.coroutines.async
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject

/** O que o usuário escolheu no formulário do item. */
data class DadosItem(val quantidade: BigDecimal, val medida: String, val preco: BigDecimal?)

class CompraViewModel(private val app: AppContainer, private val listaId: Int) : ViewModel() {

    var lista by mutableStateOf<Lista?>(null)
        private set
    var produtos by mutableStateOf<Map<Int, Produto>>(emptyMap())
        private set
    var carregando by mutableStateOf(true)
        private set
    var erro by mutableStateOf<String?>(null)
        private set
    var salvando by mutableStateOf(false)
        private set

    private val _mensagens = Channel<String>(Channel.BUFFERED)
    val mensagens = _mensagens.receiveAsFlow()

    init {
        carregar()
    }

    fun nomeDoProduto(id: Int) = produtos[id]?.name ?: "Produto #$id"

    fun carregar() {
        viewModelScope.launch {
            carregando = true
            // A rota lists/<id>/ só devolve listas ativas; a lista geral traz as duas.
            val listas = async { chamar { app.api.listas() } }
            val catalogo = async { chamar { app.api.produtos() } }

            when (val r = catalogo.await()) {
                is Resultado.Ok -> produtos = r.valor.associateBy { it.id }
                is Resultado.Falha -> erro = r.mensagem
            }
            when (val r = listas.await()) {
                is Resultado.Ok -> {
                    lista = r.valor.firstOrNull { it.id == listaId }
                    erro = if (lista == null) "Esta lista não existe mais." else null
                }
                is Resultado.Falha -> erro = r.mensagem
            }
            carregando = false
        }
    }

    private fun trocarItem(novo: Item) {
        lista = lista?.let { l -> l.copy(items = l.items.map { if (it.id == novo.id) novo else it }) }
    }

    fun alternarComprado(item: Item) {
        val marcado = item.copy(comprado = !item.comprado)
        trocarItem(marcado) // na hora; volta atrás se a API falhar
        viewModelScope.launch {
            val corpo = buildJsonObject { put("is_completed", JsonPrimitive(marcado.comprado)) }
            when (val r = chamar { app.api.editarItem(listaId, item.id, corpo) }) {
                is Resultado.Ok -> trocarItem(r.valor)
                is Resultado.Falha -> {
                    trocarItem(item)
                    _mensagens.send(r.mensagem)
                }
            }
        }
    }

    fun salvarItem(item: Item, dados: DadosItem, onOk: () -> Unit) {
        val corpo = buildJsonObject {
            put("quantity", JsonPrimitive(dados.quantidade.toPlainString()))
            put("measure", JsonPrimitive(dados.medida))
            put("price", dados.preco?.let { JsonPrimitive(it.toPlainString()) } ?: JsonNull)
        }
        executar({ app.api.editarItem(listaId, item.id, corpo) }) {
            trocarItem(it)
            onOk()
        }
    }

    fun removerItem(item: Item, onOk: () -> Unit) {
        executar({ app.api.removerItem(listaId, item.id).exigirSucesso() }) {
            lista = lista?.let { l -> l.copy(items = l.items.filter { it.id != item.id }) }
            onOk()
            _mensagens.send("${nomeDoProduto(item.product)} saiu da lista.")
        }
    }

    fun adicionarItem(produto: Produto, dados: DadosItem, onOk: () -> Unit) {
        executar({
            app.api.adicionarItem(listaId, NovoItem(produto.id, dados.quantidade, dados.medida, dados.preco))
        }) { novo ->
            produtos = produtos + (produto.id to produto)
            lista = lista?.let { it.copy(items = it.items + novo) }
            onOk()
        }
    }

    /** Cadastra no catálogo um produto que ainda não existe. */
    fun cadastrarProduto(nome: String, onOk: (Produto) -> Unit) {
        executar({ app.api.criarProduto(NovoProduto(name = nome.trim())) }) {
            produtos = produtos + (it.id to it)
            onOk(it)
        }
    }

    fun editarLista(nome: String, orcamento: BigDecimal?, onOk: () -> Unit) {
        if (nome.isBlank()) {
            viewModelScope.launch { _mensagens.send("Dê um nome para a lista.") }
            return
        }
        val corpo = buildJsonObject {
            put("name", JsonPrimitive(nome.trim()))
            put("budget", orcamento?.let { JsonPrimitive(it.toPlainString()) } ?: JsonNull)
        }
        executar({ app.api.editarLista(listaId, corpo) }) { nova ->
            lista = lista?.copy(name = nova.name, budget = nova.budget)
            onOk()
        }
    }

    fun concluir() {
        executar({ app.api.concluirLista(listaId) }) {
            lista = it
            _mensagens.send("Compra concluída! Ela foi para as compras concluídas.")
        }
    }

    fun excluir(onOk: () -> Unit) {
        executar({ app.api.excluirLista(listaId).exigirSucesso() }) { onOk() }
    }

    private fun <T> executar(chamada: suspend () -> T, onOk: suspend (T) -> Unit) {
        if (salvando) return
        viewModelScope.launch {
            salvando = true
            when (val r = chamar(chamada)) {
                is Resultado.Ok -> onOk(r.valor)
                is Resultado.Falha -> _mensagens.send(r.mensagem)
            }
            salvando = false
        }
    }
}
