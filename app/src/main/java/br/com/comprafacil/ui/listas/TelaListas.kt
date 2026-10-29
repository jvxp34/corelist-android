@file:OptIn(ExperimentalMaterial3Api::class)

package br.com.comprafacil.ui.listas

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.automirrored.outlined.ReceiptLong
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Surface
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.outlined.QrCodeScanner
import androidx.compose.material.icons.outlined.ShoppingCart
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import br.com.comprafacil.data.api.Lista
import br.com.comprafacil.ui.comum.AvisoErro
import br.com.comprafacil.ui.comum.BotaoPrincipal
import br.com.comprafacil.ui.comum.BotaoSecundario
import br.com.comprafacil.ui.comum.CampoPreco
import br.com.comprafacil.ui.comum.Carregando
import br.com.comprafacil.ui.comum.Cartao
import br.com.comprafacil.ui.comum.ChipOrcamento
import br.com.comprafacil.ui.comum.ErroComTentarDeNovo
import br.com.comprafacil.ui.comum.EstadoVazio
import br.com.comprafacil.ui.comum.Logo
import br.com.comprafacil.ui.comum.NivelOrcamento
import br.com.comprafacil.ui.comum.appViewModel
import br.com.comprafacil.ui.comum.data
import br.com.comprafacil.ui.comum.lerDecimal
import br.com.comprafacil.ui.comum.moeda
import br.com.comprafacil.ui.comum.nomeProprio
import br.com.comprafacil.ui.comum.situacaoDoOrcamento
import br.com.comprafacil.ui.theme.Cores

@Composable
fun TelaListas(
    onAbrirLista: (Int) -> Unit,
    onLerNota: () -> Unit,
    abrirNova: Boolean = false,
) {
    val vm = appViewModel { ListasViewModel(it) }
    var aba by rememberSaveable { mutableIntStateOf(0) }
    var criando by rememberSaveable { mutableStateOf(abrirNova) }

    // Recarrega ao voltar de outra tela (uma compra concluída muda de aba).
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) { vm.carregar() }

    Column(Modifier.fillMaxSize().statusBarsPadding()) {
        Row(
            Modifier.fillMaxWidth().padding(start = 20.dp, end = 20.dp, top = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text("Minhas listas", style = MaterialTheme.typography.headlineSmall, modifier = Modifier.weight(1f))
            Button(
                onClick = { criando = true },
                shape = MaterialTheme.shapes.medium,
                colors = ButtonDefaults.buttonColors(containerColor = Cores.Marca),
                contentPadding = PaddingValues(horizontal = 16.dp),
                modifier = Modifier.height(44.dp),
            ) {
                Icon(Icons.Default.Add, null, Modifier.size(18.dp))
                Spacer(Modifier.width(6.dp))
                Text("Nova")
            }
        }
        Text(
            "Crie listas, defina um orçamento e acompanhe na hora da compra.",
            style = MaterialTheme.typography.bodyMedium,
            color = Cores.Secundario,
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp),
        )
        Row(Modifier.padding(horizontal = 20.dp, vertical = 4.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Pilula("Em andamento · ${vm.ativas.size}", aba == 0) { aba = 0 }
            Pilula("Concluídas · ${vm.concluidas.size}", aba == 1) { aba = 1 }
        }

        val listas = vm.listas
        when {
            listas == null && vm.erro != null -> ErroComTentarDeNovo(vm.erro!!, vm::carregar)
            listas == null -> Carregando()
            else -> PullToRefreshBox(
                isRefreshing = vm.carregando,
                onRefresh = vm::carregar,
                modifier = Modifier.fillMaxSize(),
            ) {
                val mostradas = if (aba == 0) vm.ativas else vm.concluidas
                LazyColumn(
                    contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 12.dp, bottom = 24.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.fillMaxSize(),
                ) {
                    vm.erro?.let { item { AvisoErro(it) } }
                    if (mostradas.isEmpty()) {
                        item {
                            if (aba == 0) {
                                EstadoVazio(
                                    Icons.Outlined.ShoppingCart,
                                    "Nenhuma lista em andamento",
                                    "Crie uma lista, defina o orçamento e acompanhe quanto ainda resta enquanto compra.",
                                    acao = { BotaoPrincipal("Criar lista", { criando = true }, icone = Icons.Default.Add) },
                                )
                            } else {
                                EstadoVazio(
                                    Icons.AutoMirrored.Outlined.ReceiptLong,
                                    "Nenhuma compra concluída",
                                    "Conclua uma lista ou leia o QR code de uma nota fiscal para ver suas compras aqui.",
                                    acao = { BotaoSecundario("Ler nota fiscal", onLerNota, icone = Icons.Outlined.QrCodeScanner) },
                                )
                            }
                        }
                    }
                    items(mostradas, key = { it.id }) { lista ->
                        CartaoLista(lista, onClick = { onAbrirLista(lista.id) })
                    }
                }
            }
        }
    }

    if (criando) {
        DialogoNovaLista(
            carregando = vm.criando,
            erro = vm.erroCriar,
            onCancelar = {
                criando = false
                vm.limparErroCriar()
            },
            onCriar = { nome, orcamento ->
                vm.criar(nome, orcamento) { id ->
                    criando = false
                    onAbrirLista(id)
                }
            },
        )
    }
}

@Composable
private fun Pilula(texto: String, selecionada: Boolean, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(50),
        color = if (selecionada) Cores.Texto else Color.White,
        border = if (selecionada) null else BorderStroke(1.dp, Cores.Linha),
        modifier = Modifier.height(40.dp),
    ) {
        Box(Modifier.padding(horizontal = 16.dp), contentAlignment = Alignment.Center) {
            Text(
                texto,
                style = MaterialTheme.typography.labelLarge,
                color = if (selecionada) Color.White else Cores.Texto,
            )
        }
    }
}

@Composable
private fun CartaoLista(lista: Lista, onClick: () -> Unit) {
    val total = lista.total
    val situacao = situacaoDoOrcamento(total, lista.budget)
    val comprados = lista.items.count { it.comprado }

    Cartao(Modifier.fillMaxWidth(), onClick = onClick) {
        Column(Modifier.padding(16.dp)) {
            Text(
                lista.name,
                style = MaterialTheme.typography.titleMedium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Spacer(Modifier.height(2.dp))
            val detalhe = if (lista.concluida) {
                listOfNotNull(
                    "Concluída em ${data(lista.concluidaEm)}",
                    lista.store?.takeIf { it.isNotBlank() }?.let { nomeProprio(it) },
                ).joinToString(" · ")
            } else {
                val n = lista.items.size
                if (n == 0) "Nenhum item ainda" else "$comprados de $n ${if (n == 1) "item" else "itens"} no carrinho"
            }
            Text(
                detalhe,
                style = MaterialTheme.typography.bodySmall,
                color = Cores.Secundario,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Spacer(Modifier.height(12.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("Total", style = MaterialTheme.typography.labelSmall, color = Cores.Secundario)
                    Text(moeda(total), style = MaterialTheme.typography.titleMedium)
                }
                ChipOrcamento(situacao)
            }
            if (situacao.nivel != NivelOrcamento.SEM) {
                Spacer(Modifier.height(10.dp))
                LinearProgressIndicator(
                    progress = { situacao.usado },
                    color = situacao.corBarra,
                    trackColor = Cores.Linha,
                    drawStopIndicator = {},
                    modifier = Modifier.fillMaxWidth().height(6.dp),
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    "Orçamento de ${moeda(situacao.orcamento)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = Cores.Secundario,
                )
            }
        }
    }
}

@Composable
private fun DialogoNovaLista(
    carregando: Boolean,
    erro: String?,
    onCancelar: () -> Unit,
    onCriar: (String, java.math.BigDecimal?) -> Unit,
) {
    var nome by rememberSaveable { mutableStateOf("") }
    var orcamento by rememberSaveable { mutableStateOf("") }
    val criar = { onCriar(nome, lerDecimal(orcamento)?.takeIf { it.signum() > 0 }) }

    AlertDialog(
        onDismissRequest = onCancelar,
        title = { Text("Nova lista") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = nome,
                    onValueChange = { nome = it },
                    label = { Text("Nome") },
                    placeholder = { Text("Ex.: Compra do mês") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
                    shape = MaterialTheme.shapes.medium,
                    modifier = Modifier.fillMaxWidth(),
                )
                CampoPreco(orcamento, { orcamento = it }, "Orçamento (opcional)")
                Text(
                    "Com orçamento, o app mostra quanto ainda resta e avisa quando estourar.",
                    style = MaterialTheme.typography.bodySmall,
                    color = Cores.Secundario,
                )
                erro?.let { AvisoErro(it) }
            }
        },
        confirmButton = {
            TextButton(onClick = criar, enabled = !carregando) { Text(if (carregando) "Criando…" else "Criar") }
        },
        dismissButton = { TextButton(onClick = onCancelar) { Text("Cancelar") } },
    )
}

