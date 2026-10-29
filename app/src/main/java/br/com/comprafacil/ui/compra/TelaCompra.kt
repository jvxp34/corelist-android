@file:OptIn(ExperimentalMaterial3Api::class)

package br.com.comprafacil.ui.compra

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.MoreVert
import androidx.compose.material.icons.outlined.ShoppingCart
import androidx.compose.material.icons.outlined.WarningAmber
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import br.com.comprafacil.data.api.Item
import br.com.comprafacil.data.api.Lista
import br.com.comprafacil.ui.comum.BotaoPrincipal
import br.com.comprafacil.ui.comum.CampoPreco
import br.com.comprafacil.ui.comum.Carregando
import br.com.comprafacil.ui.comum.Cartao
import br.com.comprafacil.ui.comum.ErroComTentarDeNovo
import br.com.comprafacil.ui.comum.EstadoVazio
import br.com.comprafacil.ui.comum.NivelOrcamento
import br.com.comprafacil.ui.comum.SituacaoOrcamento
import br.com.comprafacil.ui.comum.appViewModel
import br.com.comprafacil.ui.comum.data
import br.com.comprafacil.ui.comum.lerDecimal
import br.com.comprafacil.ui.comum.moeda
import br.com.comprafacil.ui.comum.nomeProprio
import br.com.comprafacil.ui.comum.precoParaCampo
import br.com.comprafacil.ui.comum.quantidadeComMedida
import br.com.comprafacil.ui.comum.situacaoDoOrcamento
import br.com.comprafacil.ui.theme.Cores

@Composable
fun TelaCompra(listaId: Int, onVoltar: () -> Unit) {
    val vm = appViewModel(key = "compra-$listaId") { CompraViewModel(it, listaId) }
    val snackbar = remember { SnackbarHostState() }
    var menu by remember { mutableStateOf(false) }
    var adicionando by rememberSaveable { mutableStateOf(false) }
    var editandoId by rememberSaveable { mutableStateOf<Int?>(null) }
    var editandoLista by rememberSaveable { mutableStateOf(false) }
    var confirmarConclusao by rememberSaveable { mutableStateOf(false) }
    var confirmarExclusao by rememberSaveable { mutableStateOf(false) }

    LaunchedEffect(vm) { vm.mensagens.collect { snackbar.showSnackbar(it) } }

    val lista = vm.lista

    Scaffold(
        containerColor = Cores.Fundo,
        snackbarHost = { SnackbarHost(snackbar) },
        topBar = {
            TopAppBar(
                title = {
                    Text(lista?.name ?: "Lista", maxLines = 1, overflow = TextOverflow.Ellipsis)
                },
                navigationIcon = {
                    IconButton(onClick = onVoltar) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Voltar")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Cores.Fundo),
                actions = {
                    if (lista != null) {
                        IconButton(onClick = { menu = true }) {
                            Icon(Icons.Outlined.MoreVert, contentDescription = "Mais opções")
                        }
                        DropdownMenu(expanded = menu, onDismissRequest = { menu = false }) {
                            if (!lista.concluida) {
                                DropdownMenuItem(
                                    text = { Text("Editar nome e orçamento") },
                                    leadingIcon = { Icon(Icons.Outlined.Edit, null) },
                                    onClick = {
                                        menu = false
                                        editandoLista = true
                                    },
                                )
                            }
                            DropdownMenuItem(
                                text = { Text(if (lista.concluida) "Excluir compra" else "Excluir lista") },
                                leadingIcon = { Icon(Icons.Outlined.DeleteOutline, null) },
                                onClick = {
                                    menu = false
                                    confirmarExclusao = true
                                },
                            )
                        }
                    }
                },
            )
        },
        floatingActionButton = {
            if (lista != null && !lista.concluida) {
                ExtendedFloatingActionButton(
                    onClick = { adicionando = true },
                    icon = { Icon(Icons.Default.Add, null) },
                    text = { Text("Adicionar") },
                    containerColor = Cores.Marca,
                    contentColor = Color.White,
                )
            }
        },
        bottomBar = {
            if (lista != null && !lista.concluida && lista.items.isNotEmpty()) {
                Surface(color = Color.White, shadowElevation = 8.dp) {
                    Column(Modifier.navigationBarsPadding().padding(horizontal = 16.dp, vertical = 10.dp)) {
                        BotaoPrincipal(
                            "Concluir compra",
                            { confirmarConclusao = true },
                            Modifier.fillMaxWidth(),
                            icone = Icons.Default.Check,
                            carregando = vm.salvando && confirmarConclusao,
                        )
                    }
                }
            }
        },
    ) { padding ->
        Column(Modifier.padding(padding).fillMaxSize()) {
            when {
                lista == null && vm.carregando -> Carregando()
                lista == null -> ErroComTentarDeNovo(vm.erro ?: "Não foi possível abrir a lista.", vm::carregar)
                else -> ConteudoCompra(
                    lista = lista,
                    nomeDoProduto = vm::nomeDoProduto,
                    onMarcar = vm::alternarComprado,
                    onEditar = { editandoId = it.id },
                    onAdicionar = { adicionando = true },
                )
            }
        }
    }

    // ---- Folhas e diálogos ----

    val itemEditado = lista?.items?.firstOrNull { it.id == editandoId }
    if (itemEditado != null) {
        FolhaItem(
            titulo = vm.nomeDoProduto(itemEditado.product),
            inicial = DadosItem(itemEditado.quantity, itemEditado.measure, itemEditado.price),
            textoBotao = "Salvar",
            salvando = vm.salvando,
            onFechar = { editandoId = null },
            onSalvar = { dados -> vm.salvarItem(itemEditado, dados) { editandoId = null } },
            onRemover = { vm.removerItem(itemEditado) { editandoId = null } },
        )
    }

    if (adicionando) {
        FolhaAdicionar(
            produtos = vm.produtos.values.toList(),
            salvando = vm.salvando,
            onFechar = { adicionando = false },
            onCadastrar = vm::cadastrarProduto,
            onAdicionar = { produto, dados -> vm.adicionarItem(produto, dados) { adicionando = false } },
        )
    }

    if (editandoLista && lista != null) {
        DialogoEditarLista(
            lista = lista,
            salvando = vm.salvando,
            onCancelar = { editandoLista = false },
            onSalvar = { nome, orcamento -> vm.editarLista(nome, orcamento) { editandoLista = false } },
        )
    }

    if (confirmarConclusao && lista != null) {
        val semMarcar = lista.items.count { !it.comprado }
        AlertDialog(
            onDismissRequest = { confirmarConclusao = false },
            title = { Text("Concluir compra?") },
            text = {
                Text(
                    buildString {
                        append("Total de ${moeda(lista.total)}. A compra vai para as concluídas e os preços entram no seu histórico.")
                        if (semMarcar > 0) {
                            append("\n\n$semMarcar ${if (semMarcar == 1) "item não foi marcado" else "itens não foram marcados"}, mas continuam na compra.")
                        }
                    },
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    confirmarConclusao = false
                    vm.concluir()
                }) { Text("Concluir") }
            },
            dismissButton = { TextButton(onClick = { confirmarConclusao = false }) { Text("Voltar") } },
        )
    }

    if (confirmarExclusao && lista != null) {
        AlertDialog(
            onDismissRequest = { confirmarExclusao = false },
            title = { Text(if (lista.concluida) "Excluir esta compra?" else "Excluir esta lista?") },
            text = { Text("“${lista.name}” e os itens dela serão apagados. Não dá para desfazer.") },
            confirmButton = {
                TextButton(onClick = {
                    confirmarExclusao = false
                    vm.excluir(onVoltar)
                }) { Text("Excluir", color = Cores.EstourouTexto) }
            },
            dismissButton = { TextButton(onClick = { confirmarExclusao = false }) { Text("Cancelar") } },
        )
    }
}

@Composable
private fun ConteudoCompra(
    lista: Lista,
    nomeDoProduto: (Int) -> String,
    onMarcar: (Item) -> Unit,
    onEditar: (Item) -> Unit,
    onAdicionar: () -> Unit,
) {
    val faltam = lista.items.filter { !it.comprado }
    val noCarrinho = lista.items.filter { it.comprado }
    val semPreco = lista.items.count { it.price == null }

    LazyColumn(
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 100.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.fillMaxSize(),
    ) {
        item { ResumoOrcamento(lista) }

        if (semPreco > 0) {
            item {
                Row(Modifier.padding(vertical = 2.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Outlined.WarningAmber, null, tint = Cores.PertoTexto, modifier = Modifier.padding(end = 6.dp))
                    Text(
                        if (semPreco == 1) "1 produto sem preço: o total pode estar incompleto."
                        else "$semPreco produtos sem preço: o total pode estar incompleto.",
                        style = MaterialTheme.typography.bodySmall,
                        color = Cores.PertoTexto,
                    )
                }
            }
        }

        if (lista.items.isEmpty()) {
            item {
                EstadoVazio(
                    Icons.Outlined.ShoppingCart,
                    "Lista vazia",
                    "Adicione os produtos que você quer comprar, com a quantidade e o preço.",
                    acao = { BotaoPrincipal("Adicionar produto", onAdicionar, icone = Icons.Default.Add) },
                )
            }
        }

        if (lista.concluida) {
            if (lista.items.isNotEmpty()) item { TituloSecao("Itens (${lista.items.size})", "Toque num item para corrigir o preço ou a quantidade.") }
            items(lista.items, key = { it.id }) { LinhaItem(it, nomeDoProduto(it.product), onMarcar, onEditar, mostrarCheck = false) }
        } else {
            if (faltam.isNotEmpty()) {
                item { TituloSecao("Falta pegar (${faltam.size})") }
                items(faltam, key = { it.id }) { LinhaItem(it, nomeDoProduto(it.product), onMarcar, onEditar) }
            }
            if (noCarrinho.isNotEmpty()) {
                item { TituloSecao("No carrinho (${noCarrinho.size})") }
                items(noCarrinho, key = { it.id }) { LinhaItem(it, nomeDoProduto(it.product), onMarcar, onEditar) }
            }
        }
    }
}

@Composable
private fun TituloSecao(texto: String, ajuda: String? = null) {
    Column(Modifier.padding(top = 10.dp, bottom = 2.dp)) {
        Text(texto, style = MaterialTheme.typography.titleSmall, color = Cores.Secundario)
        if (ajuda != null) Text(ajuda, style = MaterialTheme.typography.bodySmall, color = Cores.Secundario)
    }
}

@Composable
private fun ResumoOrcamento(lista: Lista) {
    val total = lista.total
    val situacao = situacaoDoOrcamento(total, lista.budget)
    val noCarrinho = lista.items.filter { it.comprado }
    val totalCarrinho = noCarrinho.fold(java.math.BigDecimal.ZERO) { s, i -> s + (i.subtotal ?: java.math.BigDecimal.ZERO) }

    Cartao(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp)) {
            if (lista.concluida) {
                Text(
                    listOfNotNull(
                        "Concluída em ${data(lista.concluidaEm)}",
                        lista.store?.takeIf { it.isNotBlank() }?.let { nomeProprio(it) },
                    ).joinToString(" · "),
                    style = MaterialTheme.typography.bodySmall,
                    color = Cores.Secundario,
                )
                Spacer(Modifier.height(8.dp))
            }

            DestaqueSaldo(situacao)

            Spacer(Modifier.height(14.dp))
            HorizontalDivider(color = Cores.Linha)
            Spacer(Modifier.height(12.dp))

            Row {
                Valor("Total da lista", moeda(total), Modifier.weight(1f))
                if (situacao.nivel != NivelOrcamento.SEM) {
                    Valor("Orçamento", moeda(situacao.orcamento), Modifier.weight(1f))
                }
                if (!lista.concluida) {
                    Valor("No carrinho", moeda(totalCarrinho), Modifier.weight(1f))
                }
            }

            if (!lista.concluida && lista.items.isNotEmpty()) {
                Spacer(Modifier.height(10.dp))
                Text(
                    "${noCarrinho.size} de ${lista.items.size} itens no carrinho",
                    style = MaterialTheme.typography.bodySmall,
                    color = Cores.Secundario,
                )
            }
        }
    }
}

@Composable
private fun DestaqueSaldo(situacao: SituacaoOrcamento) {
    when (situacao.nivel) {
        NivelOrcamento.SEM -> {
            Text("Sem orçamento", style = MaterialTheme.typography.titleMedium)
            Text(
                "Defina um orçamento em ⋮ › Editar para ver quanto ainda resta.",
                style = MaterialTheme.typography.bodySmall,
                color = Cores.Secundario,
            )
        }
        else -> {
            Text(
                if (situacao.nivel == NivelOrcamento.ESTOUROU) "Passou do orçamento em" else "Ainda resta",
                style = MaterialTheme.typography.labelMedium,
                color = situacao.corTexto,
            )
            Text(
                moeda(situacao.saldo.abs()),
                style = MaterialTheme.typography.headlineMedium,
                color = situacao.corTexto,
            )
            Spacer(Modifier.height(8.dp))
            LinearProgressIndicator(
                progress = { situacao.usado },
                color = situacao.corBarra,
                trackColor = Cores.Linha,
                drawStopIndicator = {},
                modifier = Modifier.fillMaxWidth().height(8.dp),
            )
            Spacer(Modifier.height(6.dp))
            Text(situacao.rotulo, style = MaterialTheme.typography.bodySmall, color = situacao.corTexto)
        }
    }
}

@Composable
private fun Valor(rotulo: String, valor: String, modifier: Modifier = Modifier) {
    Column(modifier) {
        Text(rotulo, style = MaterialTheme.typography.labelSmall, color = Cores.Secundario)
        Text(valor, style = MaterialTheme.typography.titleSmall)
    }
}

@Composable
private fun LinhaItem(
    item: Item,
    nome: String,
    onMarcar: (Item) -> Unit,
    onEditar: (Item) -> Unit,
    mostrarCheck: Boolean = true,
) {
    Cartao(Modifier.fillMaxWidth()) {
        Row(
            Modifier
                .clickable { onEditar(item) }
                .padding(start = if (mostrarCheck) 4.dp else 16.dp, end = 16.dp, top = 10.dp, bottom = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (mostrarCheck) {
                Checkbox(
                    checked = item.comprado,
                    onCheckedChange = { onMarcar(item) },
                    colors = CheckboxDefaults.colors(checkedColor = Cores.Marca),
                )
            }
            Column(Modifier.weight(1f).padding(end = 8.dp)) {
                Text(
                    nome,
                    style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Medium),
                    color = if (item.comprado && mostrarCheck) Cores.Secundario else Cores.Texto,
                    textDecoration = if (item.comprado && mostrarCheck) TextDecoration.LineThrough else null,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                val qtd = quantidadeComMedida(item.quantity, item.porKg)
                Text(
                    when (val preco = item.price) {
                        null -> "$qtd · sem preço"
                        else -> "$qtd × ${moeda(preco)}${if (item.porKg) "/kg" else ""}"
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = Cores.Secundario,
                )
            }
            Text(
                item.subtotal?.let { moeda(it) } ?: "—",
                style = MaterialTheme.typography.titleSmall,
            )
        }
    }
}

@Composable
private fun DialogoEditarLista(
    lista: Lista,
    salvando: Boolean,
    onCancelar: () -> Unit,
    onSalvar: (String, java.math.BigDecimal?) -> Unit,
) {
    var nome by rememberSaveable { mutableStateOf(lista.name) }
    var orcamento by rememberSaveable { mutableStateOf(precoParaCampo(lista.budget)) }

    AlertDialog(
        onDismissRequest = onCancelar,
        title = { Text("Editar lista") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = nome,
                    onValueChange = { nome = it },
                    label = { Text("Nome") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
                    shape = MaterialTheme.shapes.medium,
                    modifier = Modifier.fillMaxWidth(),
                )
                CampoPreco(orcamento, { orcamento = it }, "Orçamento (deixe vazio para não usar)")
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onSalvar(nome, lerDecimal(orcamento)?.takeIf { it.signum() > 0 }) },
                enabled = !salvando,
            ) { Text("Salvar") }
        },
        dismissButton = { TextButton(onClick = onCancelar) { Text("Cancelar") } },
    )
}

