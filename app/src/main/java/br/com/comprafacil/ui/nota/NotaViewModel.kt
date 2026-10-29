package br.com.comprafacil.ui.nota

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import br.com.comprafacil.AppContainer
import br.com.comprafacil.data.api.ImportarNota
import br.com.comprafacil.data.api.ItemImportar
import br.com.comprafacil.data.api.ItemNota
import br.com.comprafacil.data.api.LinkNota
import br.com.comprafacil.data.api.Lista
import br.com.comprafacil.data.api.NotaLida
import br.com.comprafacil.data.api.Resultado
import br.com.comprafacil.data.api.chamar
import br.com.comprafacil.ui.comum.diaMes
import br.com.comprafacil.ui.comum.nomeProprio
import java.math.BigDecimal
import kotlinx.coroutines.launch
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonPrimitive

/** Item da nota que o usuário confere (o nome pode ser corrigido). */
class ItemConferido(val id: Int, val original: ItemNota) {
    var nome by mutableStateOf(original.nome)
}

class NotaViewModel(private val app: AppContainer, private val link: String) : ViewModel() {

    var nota by mutableStateOf<NotaLida?>(null)
        private set
    var lendo by mutableStateOf(true)
        private set
    var erro by mutableStateOf<String?>(null)
        private set
    var importando by mutableStateOf(false)
        private set
    var erroImportar by mutableStateOf<String?>(null)
        private set

    /** Lista criada pela importação (mostra a tela "Compra concluída"). */
    var importada by mutableStateOf<Lista?>(null)
        private set

    /** A nota já tinha sido importada antes: id da compra. */
    var jaImportada by mutableStateOf<Int?>(null)
        private set

    var nomeCompra by mutableStateOf("")
    val itens = mutableStateListOf<ItemConferido>()

    val total: BigDecimal
        get() = itens.fold(BigDecimal.ZERO) { s, i -> s + i.original.preco.multiply(i.original.quantidade) }

    init {
        ler()
    }

    fun ler() {
        viewModelScope.launch {
            lendo = true
            erro = null
            when (val r = chamar { app.api.lerNota(LinkNota(link)) }) {
                is Resultado.Ok -> {
                    val n = r.valor
                    nota = n
                    jaImportada = n.listaImportada
                    itens.clear()
                    itens.addAll(n.itens.mapIndexed { i, item -> ItemConferido(i, item) })
                    // Mesmo padrão do servidor: "Loja 31/10".
                    val loja = nomeProprio(n.loja).ifBlank { "Compra" }
                    nomeCompra = listOf(loja, diaMes(n.emitidaEm)).filter { it.isNotBlank() }.joinToString(" ")
                }
                is Resultado.Falha -> erro = r.mensagem
            }
            lendo = false
        }
    }

    fun remover(item: ItemConferido) {
        itens.remove(item)
    }

    fun importar() {
        if (itens.isEmpty()) {
            erroImportar = "Deixe ao menos um item para importar."
            return
        }
        if (itens.any { it.nome.isBlank() }) {
            erroImportar = "Todo item precisa de um nome."
            return
        }
        viewModelScope.launch {
            importando = true
            erroImportar = null
            val corpo = ImportarNota(
                link = link,
                nome = nomeCompra.trim(),
                itens = itens.map {
                    ItemImportar(it.nome.trim(), it.original.quantidade, it.original.medida, it.original.preco)
                },
            )
            when (val r = chamar { app.api.importarNota(corpo) }) {
                is Resultado.Ok -> importada = r.valor
                is Resultado.Falha -> {
                    val id = r.corpo?.get("list_id")?.jsonPrimitive?.intOrNull
                    if (r.status == 409 && id != null) jaImportada = id else erroImportar = r.mensagem
                }
            }
            importando = false
        }
    }
}
