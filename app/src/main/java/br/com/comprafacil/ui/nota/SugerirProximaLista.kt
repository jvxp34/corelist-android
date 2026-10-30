package br.com.comprafacil.ui.nota

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.PlaylistAdd
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import br.com.comprafacil.AppContainer
import br.com.comprafacil.data.api.Item
import br.com.comprafacil.data.api.Produto
import br.com.comprafacil.data.api.RepetirLista
import br.com.comprafacil.data.api.Resultado
import br.com.comprafacil.data.api.chamar
import br.com.comprafacil.ui.comum.AvisoErro
import br.com.comprafacil.ui.comum.BotaoPrincipal
import br.com.comprafacil.ui.comum.BotaoSecundario
import br.com.comprafacil.ui.comum.Carregando
import br.com.comprafacil.ui.comum.Cartao
import br.com.comprafacil.ui.comum.appViewModel
import br.com.comprafacil.ui.comum.precoComMedida
import br.com.comprafacil.ui.comum.quantidadeComMedida
import br.com.comprafacil.ui.produtos.FotoDoProduto
import br.com.comprafacil.ui.theme.Cores
import kotlinx.coroutines.async
import kotlinx.coroutines.launch

class SugerirProximaViewModel(private val app: AppContainer, private val compraId: Int) : ViewModel() {
    /** Itens comprados (null enquanto carrega). */
    var itens by mutableStateOf<List<Item>?>(null)
        private set
    var produtos by mutableStateOf<Map<Int, Produto>>(emptyMap())
        private set
    var marcados by mutableStateOf<Set<Int>>(emptySet())
        private set
    var nome by mutableStateOf("Próxima compra")
    var criando by mutableStateOf(false)
        private set
    var erro by mutableStateOf<String?>(null)
        private set
    /** Falhou ao carregar: a tela segue direto para a compra. */
    var desistir by mutableStateOf(false)
        private set

    init {
        viewModelScope.launch {
            val catalogo = async { chamar { app.api.produtos() } }
            when (val r = chamar { app.api.listas() }) {
                is Resultado.Ok -> {
                    val comprados = r.valor.firstOrNull { it.id == compraId }?.items.orEmpty().filter { it.comprado }
                    produtos = (catalogo.await() as? Resultado.Ok)?.valor.orEmpty().associateBy { it.id }
                    marcados = comprados.map { it.id }.toSet()
                    itens = comprados
                }
                is Resultado.Falha -> desistir = true
            }
        }
    }

    fun alternar(id: Int) {
        marcados = if (id in marcados) marcados - id else marcados + id
    }

    fun alternarTodos() {
        val todos = itens.orEmpty().map { it.id }.toSet()
        marcados = if (marcados.size == todos.size) emptySet() else todos
    }

    fun criar(onCriada: (Int) -> Unit) {
        if (marcados.isEmpty()) {
            erro = "Marque ao menos um produto."
            return
        }
        viewModelScope.launch {
            criando = true
            erro = null
            // O preço pago vira o planejado e o orçamento é o mesmo da compra.
            when (val r = chamar { app.api.repetirLista(compraId, RepetirLista(nome.trim(), marcados.toList())) }) {
                is Resultado.Ok -> onCriada(r.valor.id)
                is Resultado.Falha -> {
                    erro = r.mensagem
                    criando = false
                }
            }
        }
    }
}

/**
 * Depois de salvar uma compra pela nota: pergunta se cria a próxima lista com
 * os produtos comprados, todos marcados (a pessoa desmarca o que não precisa).
 */
@Composable
fun TelaSugerirProximaLista(compraId: Int, onCriada: (Int) -> Unit, onAgoraNao: () -> Unit) {
    val vm = appViewModel(key = "proxima-$compraId") { SugerirProximaViewModel(it, compraId) }
    val itens = vm.itens

    // Sem nada comprado (ou sem rede) não há o que sugerir: vai para a compra.
    LaunchedEffect(itens, vm.desistir) {
        if (vm.desistir || (itens != null && itens.isEmpty())) onAgoraNao()
    }

    if (itens == null || itens.isEmpty()) {
        Box(Modifier.fillMaxSize().background(Cores.Fundo)) { Carregando() }
        return
    }

    Column(Modifier.fillMaxSize().background(Cores.Fundo).statusBarsPadding()) {
        LazyColumn(
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.weight(1f),
        ) {
            item {
                Row(
                    Modifier
                        .fillMaxWidth()
                        .background(Cores.OkChip, RoundedCornerShape(12.dp))
                        .padding(horizontal = 14.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(Icons.Default.Check, null, tint = Cores.OkTexto, modifier = Modifier.size(18.dp))
                    Text(
                        "Compra salva no histórico.",
                        color = Cores.OkTexto,
                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                        modifier = Modifier.padding(start = 8.dp),
                    )
                }
            }
            item {
                Row(verticalAlignment = Alignment.Top) {
                    Box(Modifier.size(44.dp).background(Cores.MarcaClara, CircleShape), contentAlignment = Alignment.Center) {
                        Icon(Icons.AutoMirrored.Outlined.PlaylistAdd, null, tint = Cores.Marca)
                    }
                    Column(Modifier.padding(start = 12.dp)) {
                        Text("Criar a próxima lista com estes produtos?", style = MaterialTheme.typography.titleLarge)
                        Text(
                            "O preço que você pagou vira o preço planejado. Desmarque o que não precisa comprar de novo.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = Cores.Secundario,
                            modifier = Modifier.padding(top = 4.dp),
                        )
                    }
                }
            }
            item {
                OutlinedTextField(
                    value = vm.nome,
                    onValueChange = { if (it.length <= 200) vm.nome = it },
                    label = { Text("Nome da lista") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
                    shape = MaterialTheme.shapes.medium,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
            item {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        "${vm.marcados.size} de ${itens.size} produtos",
                        style = MaterialTheme.typography.bodySmall,
                        color = Cores.Secundario,
                        modifier = Modifier.weight(1f),
                    )
                    TextButton(onClick = vm::alternarTodos) {
                        Text(if (vm.marcados.size == itens.size) "Desmarcar todos" else "Marcar todos")
                    }
                }
                Cartao(Modifier.fillMaxWidth()) {
                    Column {
                        itens.forEachIndexed { i, item ->
                            if (i > 0) HorizontalDivider(color = Cores.Linha)
                            LinhaSugerida(item, vm.produtos[item.product], item.id in vm.marcados) { vm.alternar(item.id) }
                        }
                    }
                }
            }
        }

        Surface(color = Color.White, shadowElevation = 8.dp) {
            Column(
                Modifier.navigationBarsPadding().padding(horizontal = 16.dp, vertical = 10.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                vm.erro?.let { AvisoErro(it) }
                BotaoPrincipal(
                    "Criar lista",
                    { vm.criar(onCriada) },
                    Modifier.fillMaxWidth(),
                    carregando = vm.criando,
                    habilitado = vm.marcados.isNotEmpty(),
                )
                BotaoSecundario("Agora não", onAgoraNao, Modifier.fillMaxWidth(), habilitado = !vm.criando)
            }
        }
    }
}

@Composable
private fun LinhaSugerida(item: Item, produto: Produto?, marcado: Boolean, onAlternar: () -> Unit) {
    val nome = produto?.name ?: "Produto #${item.product}"
    Row(
        Modifier.fillMaxWidth().clickable(onClick = onAlternar).padding(horizontal = 4.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Checkbox(checked = marcado, onCheckedChange = { onAlternar() }, colors = CheckboxDefaults.colors(checkedColor = Cores.Marca))
        FotoDoProduto(produto?.imagem, nome, tamanho = 36.dp)
        Column(Modifier.weight(1f).padding(start = 10.dp, end = 12.dp)) {
            Text(nome, style = MaterialTheme.typography.bodyMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(
                quantidadeComMedida(item.quantity, item.porKg) +
                    (item.price?.let { " · " + precoComMedida(it, item.measure) } ?: ""),
                style = MaterialTheme.typography.bodySmall,
                color = Cores.Secundario,
            )
        }
    }
}
