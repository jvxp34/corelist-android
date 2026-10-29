package br.com.comprafacil.ui.produtos

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.Sell
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.viewModelScope
import br.com.comprafacil.AppContainer
import br.com.comprafacil.data.api.Lista
import br.com.comprafacil.data.api.Produto
import br.com.comprafacil.data.api.Resultado
import br.com.comprafacil.data.api.chamar
import br.com.comprafacil.ui.comum.AvisoErro
import br.com.comprafacil.ui.comum.Carregando
import br.com.comprafacil.ui.comum.Cartao
import br.com.comprafacil.ui.comum.EstadoVazio
import br.com.comprafacil.ui.comum.appViewModel
import br.com.comprafacil.ui.comum.data
import br.com.comprafacil.ui.comum.moeda
import br.com.comprafacil.ui.comum.nomeProprio
import br.com.comprafacil.ui.comum.normalizar
import br.com.comprafacil.ui.theme.Cores
import java.math.BigDecimal
import java.math.RoundingMode
import kotlinx.coroutines.async
import kotlinx.coroutines.launch

/** O que o usuário já pagou por um produto, a partir das compras concluídas. */
data class PrecoPago(
    val produtoId: Int,
    val nome: String,
    val porKg: Boolean,
    val menor: BigDecimal,
    val ondeMenor: String,
    val quandoMenor: String,
    val ultimo: BigDecimal,
    val media: BigDecimal,
    val vezes: Int,
)

fun precosPagos(listas: List<Lista>, produtos: Map<Int, Produto>): List<PrecoPago> {
    data class Compra(val preco: BigDecimal, val porKg: Boolean, val lista: Lista)

    return listas.filter { it.concluida }
        .flatMap { l -> l.items.mapNotNull { i -> i.price?.let { i.product to Compra(it, i.porKg, l) } } }
        .groupBy({ it.first }, { it.second })
        .map { (id, compras) ->
            val porData = compras.sortedBy { it.lista.concluidaEm ?: "" }
            val menor = compras.minBy { it.preco }
            val soma = compras.fold(BigDecimal.ZERO) { s, c -> s + c.preco }
            PrecoPago(
                produtoId = id,
                nome = produtos[id]?.name ?: "Produto #$id",
                porKg = porData.last().porKg,
                menor = menor.preco,
                ondeMenor = menor.lista.store?.takeIf { it.isNotBlank() }?.let { nomeProprio(it) } ?: menor.lista.name,
                quandoMenor = data(menor.lista.concluidaEm),
                ultimo = porData.last().preco,
                media = soma.divide(BigDecimal(compras.size), 2, RoundingMode.HALF_UP),
                vezes = compras.size,
            )
        }
        .sortedBy { normalizar(it.nome) }
}

class MenorPrecoViewModel(private val app: AppContainer) : ViewModel() {
    var precos by mutableStateOf<List<PrecoPago>?>(null)
        private set
    var carregando by mutableStateOf(false)
        private set
    var erro by mutableStateOf<String?>(null)
        private set

    fun carregar() {
        if (carregando) return
        viewModelScope.launch {
            carregando = true
            val catalogo = async { chamar { app.api.produtos() } }
            val listas = chamar { app.api.listas() }
            val produtos = (catalogo.await() as? Resultado.Ok)?.valor.orEmpty().associateBy { it.id }
            when (listas) {
                is Resultado.Ok -> {
                    precos = precosPagos(listas.valor, produtos)
                    erro = null
                }
                is Resultado.Falha -> erro = listas.mensagem
            }
            carregando = false
        }
    }
}

@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
@Composable
fun TelaMenorPreco() {
    val vm = appViewModel { MenorPrecoViewModel(it) }
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) { vm.carregar() }
    var busca by rememberSaveable { mutableStateOf("") }

    Column(Modifier.fillMaxSize().statusBarsPadding()) {
        Column(Modifier.padding(start = 20.dp, end = 20.dp, top = 16.dp)) {
            Text("Menor preço", style = MaterialTheme.typography.headlineSmall)
            Text(
                "O menor preço que você já pagou em cada produto, nas suas compras concluídas.",
                style = MaterialTheme.typography.bodyMedium,
                color = Cores.Secundario,
                modifier = Modifier.padding(top = 4.dp, bottom = 12.dp),
            )
            OutlinedTextField(
                value = busca,
                onValueChange = { busca = it },
                placeholder = { Text("Buscar produto") },
                leadingIcon = { Icon(Icons.Outlined.Search, null) },
                singleLine = true,
                shape = MaterialTheme.shapes.medium,
                modifier = Modifier.fillMaxWidth(),
            )
        }

        val precos = vm.precos
        when {
            precos == null && vm.erro != null -> br.com.comprafacil.ui.comum.ErroComTentarDeNovo(vm.erro!!, vm::carregar)
            precos == null -> Carregando()
            else -> PullToRefreshBox(isRefreshing = vm.carregando, onRefresh = vm::carregar, modifier = Modifier.fillMaxSize()) {
                val termo = normalizar(busca)
                val mostrados = remember(precos, termo) {
                    precos.filter { termo.isEmpty() || normalizar(it.nome).contains(termo) }
                }
                LazyColumn(
                    contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 12.dp, bottom = 24.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.fillMaxSize(),
                ) {
                    vm.erro?.let { item { AvisoErro(it) } }
                    if (precos.isEmpty()) {
                        item {
                            EstadoVazio(
                                Icons.Outlined.Sell,
                                "Ainda sem preços",
                                "Conclua uma compra com preços ou leia uma nota fiscal. Os preços que você pagou aparecem aqui.",
                            )
                        }
                    }
                    items(mostrados, key = { it.produtoId }) { LinhaPreco(it) }
                }
            }
        }
    }
}

@Composable
private fun LinhaPreco(p: PrecoPago) {
    val sufixo = if (p.porKg) "/kg" else ""
    Cartao(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Row(verticalAlignment = Alignment.Top) {
                Text(
                    p.nome,
                    style = MaterialTheme.typography.titleSmall,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f).padding(end = 8.dp),
                )
                Text(moeda(p.menor) + sufixo, style = MaterialTheme.typography.titleMedium, color = Cores.OkTexto)
            }
            Text(
                "Menor preço: ${p.ondeMenor}${if (p.quandoMenor.isNotBlank()) " · ${p.quandoMenor}" else ""}",
                style = MaterialTheme.typography.bodySmall,
                color = Cores.Secundario,
            )
            if (p.vezes > 1) {
                Text(
                    "Último: ${moeda(p.ultimo)}$sufixo · média ${moeda(p.media)}$sufixo · ${p.vezes} compras",
                    style = MaterialTheme.typography.bodySmall,
                    color = Cores.Secundario,
                )
            }
        }
    }
}
