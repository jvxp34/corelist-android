package br.com.comprafacil.ui.nota

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import br.com.comprafacil.AppContainer
import br.com.comprafacil.data.api.ConcluirComNota
import br.com.comprafacil.data.api.CruzamentoNota
import br.com.comprafacil.data.api.EscolhaItemNota
import br.com.comprafacil.data.api.ItemListaNaNota
import br.com.comprafacil.data.api.ItemNotaNaLista
import br.com.comprafacil.data.api.LinkNota
import br.com.comprafacil.data.api.Lista
import br.com.comprafacil.data.api.Resultado
import br.com.comprafacil.data.api.chamar
import java.math.BigDecimal
import kotlinx.coroutines.launch
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonPrimitive

/** O que a pessoa escolheu para um item da nota (mesma ideia do site). */
class EscolhaNota(listItem: Int?, nome: String) {
    var listItem by mutableStateOf(listItem)
    var nome by mutableStateOf(nome)
    var incluir by mutableStateOf(true)
}

/**
 * Nota fiscal lida dentro de uma lista em andamento: cruza o que foi
 * planejado com o que veio na nota e conclui a lista com os preços reais.
 */
class NotaNaListaViewModel(
    private val app: AppContainer,
    private val listaId: Int,
    private val link: String,
) : ViewModel() {

    var cruzamento by mutableStateOf<CruzamentoNota?>(null)
        private set
    var lendo by mutableStateOf(true)
        private set
    var erro by mutableStateOf<String?>(null)
        private set
    var concluindo by mutableStateOf(false)
        private set
    var erroConcluir by mutableStateOf<String?>(null)
        private set
    var jaImportada by mutableStateOf<Int?>(null)
        private set

    /** A lista concluída pela nota (a tela some e abre a compra). */
    var concluida by mutableStateOf<Lista?>(null)
        private set

    var escolhas by mutableStateOf<Map<Int, EscolhaNota>>(emptyMap())
        private set

    init {
        ler()
    }

    fun ler() {
        viewModelScope.launch {
            lendo = true
            erro = null
            when (val r = chamar { app.api.lerNotaNaLista(listaId, LinkNota(link)) }) {
                is Resultado.Ok -> {
                    val c = r.valor
                    cruzamento = c
                    jaImportada = c.listaImportada
                    escolhas = c.itens.associate { it.indice to EscolhaNota(it.listItem, it.nome) }
                }
                is Resultado.Falha -> erro = r.mensagem
            }
            lendo = false
        }
    }

    // ---- Grupos calculados a partir das escolhas ----

    val incluidos: List<ItemNotaNaLista>
        get() = cruzamento?.itens.orEmpty().filter { escolhas[it.indice]?.incluir == true }

    val comprados: List<ItemNotaNaLista>
        get() = incluidos.filter { escolhas[it.indice]?.listItem != null }

    val foraDaLista: List<ItemNotaNaLista>
        get() = incluidos.filter { escolhas[it.indice]?.listItem == null }

    private val usados: Set<Int>
        get() = comprados.mapNotNull { escolhas[it.indice]?.listItem }.toSet()

    val esquecidos: List<ItemListaNaNota>
        get() = cruzamento?.itensLista.orEmpty().filter { it.id !in usados }

    fun itemDaLista(id: Int?): ItemListaNaNota? = cruzamento?.itensLista?.firstOrNull { it.id == id }

    /** Itens da lista que este item da nota ainda pode ocupar. */
    fun opcoesPara(item: ItemNotaNaLista): List<ItemListaNaNota> {
        val atual = escolhas[item.indice]?.listItem
        return cruzamento?.itensLista.orEmpty().filter { it.id == atual || it.id !in usados }
    }

    val gasto: BigDecimal
        get() = incluidos.fold(BigDecimal.ZERO) { s, i -> s + i.total }

    /** Quanto a pessoa esperava gastar nos itens planejados que vieram na nota. */
    val planejado: BigDecimal
        get() = comprados.fold(BigDecimal.ZERO) { s, i ->
            val plano = itemDaLista(escolhas[i.indice]?.listItem)
            val preco = plano?.preco ?: return@fold s
            s + preco.multiply(plano.quantidade)
        }

    fun concluir() {
        if (cruzamento == null) return
        val itens = incluidos
        if (itens.isEmpty()) {
            erroConcluir = "Mantenha pelo menos um produto da nota."
            return
        }
        if (itens.any { escolhas[it.indice]?.nome.isNullOrBlank() }) {
            erroConcluir = "Dê um nome a todos os produtos."
            return
        }
        viewModelScope.launch {
            concluindo = true
            erroConcluir = null
            val corpo = ConcluirComNota(
                link = link,
                itens = itens.map { item ->
                    val e = escolhas.getValue(item.indice)
                    val nome = e.nome.trim()
                    EscolhaItemNota(
                        indice = item.indice,
                        listItem = e.listItem,
                        // A sugestão do catálogo só vale se a pessoa não renomeou o item.
                        produto = if (e.listItem == null && nome == item.nome) item.produtoSugerido?.id else null,
                        nome = nome,
                    )
                },
            )
            when (val r = chamar { app.api.concluirComNota(listaId, corpo) }) {
                is Resultado.Ok -> concluida = r.valor
                is Resultado.Falha -> {
                    val id = r.corpo?.get("list_id")?.jsonPrimitive?.intOrNull
                    if (r.status == 409) jaImportada = id ?: listaId else erroConcluir = r.mensagem
                }
            }
            concluindo = false
        }
    }
}
