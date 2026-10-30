@file:OptIn(ExperimentalMaterial3Api::class)

package br.com.comprafacil.ui.nota

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuAnchorType
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import br.com.comprafacil.data.api.ItemNotaNaLista
import br.com.comprafacil.ui.comum.AvisoErro
import br.com.comprafacil.ui.comum.BotaoPrincipal
import br.com.comprafacil.ui.comum.Cartao
import br.com.comprafacil.ui.comum.NivelOrcamento
import br.com.comprafacil.ui.comum.appViewModel
import br.com.comprafacil.ui.comum.data
import br.com.comprafacil.ui.comum.moeda
import br.com.comprafacil.ui.comum.nomeProprio
import br.com.comprafacil.ui.comum.numero
import br.com.comprafacil.ui.comum.quantidadeComMedida
import br.com.comprafacil.ui.comum.situacaoDoOrcamento
import br.com.comprafacil.ui.theme.Cores
import java.math.BigDecimal

/** "2 un × R$ 7,29" ou "1,235 kg × R$ 6,99/kg". */
private fun descricaoDoPreco(item: ItemNotaNaLista) =
    "${quantidadeComMedida(item.quantidade, item.porKg)} × ${moeda(item.preco)}${if (item.porKg) "/kg" else ""}"

@Composable
fun TelaNotaNaLista(
    listaId: Int,
    link: String,
    onVoltar: () -> Unit,
    onLerOutra: () -> Unit,
    onAbrirLista: (Int) -> Unit,
    onConcluida: (Int) -> Unit,
) {
    val vm = appViewModel(key = "nota-lista-$listaId-$link") { NotaNaListaViewModel(it, listaId, link) }

    LaunchedEffect(vm.concluida) { vm.concluida?.let { onConcluida(it.id) } }

    Scaffold(
        containerColor = Cores.Fundo,
        topBar = {
            TopAppBar(
                title = { Text(if (vm.cruzamento == null) "" else "Conferir a nota") },
                navigationIcon = {
                    IconButton(onClick = onVoltar) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Voltar para a lista") }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Cores.Fundo),
            )
        },
        bottomBar = {
            if (vm.cruzamento != null && vm.jaImportada == null) {
                Surface(color = Color.White, shadowElevation = 8.dp) {
                    Column(
                        Modifier.navigationBarsPadding().padding(horizontal = 16.dp, vertical = 10.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        vm.erroConcluir?.let { AvisoErro(it) }
                        BotaoPrincipal(
                            "Concluir com a nota",
                            vm::concluir,
                            Modifier.fillMaxWidth(),
                            carregando = vm.concluindo,
                            icone = Icons.Default.Check,
                        )
                        Text(
                            "Os preços da nota substituem os planejados.",
                            style = MaterialTheme.typography.bodySmall,
                            color = Cores.Secundario,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.fillMaxWidth(),
                        )
                    }
                }
            }
        },
    ) { padding ->
        Box(Modifier.padding(padding).fillMaxSize()) {
            val c = vm.cruzamento
            val jaImportada = vm.jaImportada
            when {
                vm.lendo -> Lendo()
                c == null -> FalhaLeitura(vm.erro ?: "Não foi possível ler a nota.", vm::ler, onLerOutra)
                jaImportada != null -> JaImportada(onVer = { onAbrirLista(jaImportada) }, onLerOutra = onLerOutra)
                else -> ConteudoNotaNaLista(vm)
            }
        }
    }
}

@Composable
private fun ConteudoNotaNaLista(vm: NotaNaListaViewModel) {
    val c = vm.cruzamento ?: return
    val comprados = vm.comprados
    val foraDaLista = vm.foraDaLista
    val esquecidos = vm.esquecidos

    LazyColumn(
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        item { ResumoNota(vm) }

        if (comprados.isNotEmpty()) {
            item { TituloGrupo("Comprados · ${comprados.size}") }
            items(comprados, key = { "c${it.indice}" }) { item -> CartaoComprado(vm, item) }
        }

        if (foraDaLista.isNotEmpty()) {
            item { TituloGrupo("Fora da lista · ${foraDaLista.size}") }
            items(foraDaLista, key = { "f${it.indice}" }) { item ->
                CartaoForaDaLista(vm, item, podeLigar = c.itensLista.isNotEmpty())
            }
        }

        if (esquecidos.isNotEmpty()) {
            item { TituloGrupo("Não vieram na nota · ${esquecidos.size}") }
            item {
                Cartao(Modifier.fillMaxWidth()) {
                    Column {
                        esquecidos.forEachIndexed { i, item ->
                            if (i > 0) HorizontalDivider(color = Cores.Linha)
                            Text(item.nome, modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp))
                        }
                    }
                }
                Text(
                    "Ficam na lista como \"não comprado\". Depois você pode passá-los para a próxima lista.",
                    style = MaterialTheme.typography.bodySmall,
                    color = Cores.Secundario,
                    modifier = Modifier.padding(top = 8.dp, start = 4.dp),
                )
            }
        }
    }
}

@Composable
private fun TituloGrupo(texto: String) {
    Text(
        texto.uppercase(),
        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
        color = Cores.Secundario,
        modifier = Modifier.padding(top = 12.dp, start = 4.dp),
    )
}

@Composable
private fun ResumoNota(vm: NotaNaListaViewModel) {
    val c = vm.cruzamento ?: return
    val gasto = vm.gasto
    val planejado = vm.planejado
    val situacao = situacaoDoOrcamento(gasto, c.orcamento)

    Cartao(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(
                listOf(nomeProprio(c.loja).ifBlank { "Loja" }, data(c.emitidaEm)).filter { it.isNotBlank() }.joinToString(" · "),
                style = MaterialTheme.typography.labelMedium,
                color = Cores.Secundario,
            )
            Row(verticalAlignment = Alignment.Bottom) {
                Text(moeda(gasto), style = MaterialTheme.typography.headlineMedium)
                if (situacao.nivel != NivelOrcamento.SEM) {
                    Text(
                        "de ${moeda(situacao.orcamento)}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Cores.Secundario,
                        modifier = Modifier.padding(start = 8.dp, bottom = 4.dp),
                    )
                }
            }
            if (situacao.nivel != NivelOrcamento.SEM) {
                LinearProgressIndicator(
                    progress = { situacao.usado },
                    color = situacao.corBarra,
                    trackColor = Cores.Linha,
                    drawStopIndicator = {},
                    modifier = Modifier.fillMaxWidth().height(8.dp),
                )
                Text(
                    situacao.resumo,
                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                    color = situacao.corTexto,
                )
            }
            if (planejado.signum() > 0) {
                Text(
                    "Para os itens planejados, você esperava gastar ${moeda(planejado)}.",
                    style = MaterialTheme.typography.bodySmall,
                    color = Cores.Secundario,
                )
            }
        }
    }
}

@Composable
private fun CartaoComprado(vm: NotaNaListaViewModel, item: ItemNotaNaLista) {
    val escolha = vm.escolhas[item.indice] ?: return
    val plano = vm.itemDaLista(escolha.listItem)

    Cartao(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Row(verticalAlignment = Alignment.Top) {
                Column(Modifier.weight(1f).padding(end = 8.dp)) {
                    Text(plano?.nome ?: item.nome, style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.SemiBold))
                    Text(
                        "Na nota: ${item.nome} · ${descricaoDoPreco(item)}",
                        style = MaterialTheme.typography.bodySmall,
                        color = Cores.Secundario,
                    )
                }
                Text(moeda(item.total), style = MaterialTheme.typography.titleSmall)
            }

            // Compara o preço da unidade (ou do kg): a quantidade pode ter mudado.
            val precoPlanejado = plano?.preco
            if (precoPlanejado != null) {
                val diferenca = item.preco - precoPlanejado
                if (diferenca.abs() >= BigDecimal("0.01")) {
                    val porQue = if (item.porKg) "por kg" else "por unidade"
                    Text(
                        if (diferenca.signum() > 0) "${moeda(diferenca)} $porQue a mais que o planejado"
                        else "${moeda(diferenca.negate())} $porQue a menos que o planejado",
                        style = MaterialTheme.typography.bodySmall,
                        // Verde, âmbar e vermelho ficam só para o orçamento.
                        color = Cores.Secundario,
                    )
                }
            }
            if (plano != null && plano.quantidade.compareTo(item.quantidade) != 0) {
                Text(
                    "Planejou ${numero(plano.quantidade)}, comprou ${numero(item.quantidade)}.",
                    style = MaterialTheme.typography.bodySmall,
                    color = Cores.Secundario,
                )
            }
            SeletorItemDaLista(vm, item)
        }
    }
}

@Composable
private fun CartaoForaDaLista(vm: NotaNaListaViewModel, item: ItemNotaNaLista, podeLigar: Boolean) {
    val escolha = vm.escolhas[item.indice] ?: return

    Cartao(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(start = 12.dp, end = 4.dp, top = 8.dp, bottom = 12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                OutlinedTextField(
                    value = escolha.nome,
                    onValueChange = { escolha.nome = it },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
                    textStyle = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.SemiBold),
                    shape = MaterialTheme.shapes.medium,
                    modifier = Modifier.weight(1f),
                )
                IconButton(onClick = { escolha.incluir = false }) {
                    Icon(Icons.Outlined.DeleteOutline, "Tirar ${item.nome} da compra", tint = Cores.Secundario)
                }
            }
            Row(Modifier.padding(top = 6.dp, end = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(descricaoDoPreco(item), style = MaterialTheme.typography.bodySmall, color = Cores.Secundario)
                    val sugerido = item.produtoSugerido
                    if (sugerido != null && escolha.nome.trim() == item.nome) {
                        Text(
                            sugerido.name,
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                            color = Cores.MarcaEscura,
                            modifier = Modifier
                                .padding(top = 4.dp)
                                .background(Cores.MarcaClara, RoundedCornerShape(50))
                                .padding(horizontal = 8.dp, vertical = 2.dp),
                        )
                    }
                }
                Text(moeda(item.total), style = MaterialTheme.typography.titleSmall)
            }
            if (podeLigar) {
                Box(Modifier.padding(end = 12.dp)) { SeletorItemDaLista(vm, item) }
            }
        }
    }
}

/** "Na sua lista:" escolhe o item planejado (ou Fora da lista). */
@Composable
private fun SeletorItemDaLista(vm: NotaNaListaViewModel, item: ItemNotaNaLista) {
    val escolha = vm.escolhas[item.indice] ?: return
    var aberto by remember { mutableStateOf(false) }
    val atual = vm.itemDaLista(escolha.listItem)

    Spacer(Modifier.height(4.dp))
    ExposedDropdownMenuBox(expanded = aberto, onExpandedChange = { aberto = it }) {
        OutlinedTextField(
            value = atual?.nome ?: "Fora da lista",
            onValueChange = {},
            readOnly = true,
            singleLine = true,
            label = { Text("Na sua lista") },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = aberto) },
            shape = MaterialTheme.shapes.medium,
            modifier = Modifier.fillMaxWidth().menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable),
        )
        ExposedDropdownMenu(expanded = aberto, onDismissRequest = { aberto = false }) {
            DropdownMenuItem(
                text = { Text("Fora da lista") },
                onClick = {
                    escolha.listItem = null
                    aberto = false
                },
            )
            vm.opcoesPara(item).forEach { opcao ->
                DropdownMenuItem(
                    text = { Text(opcao.nome) },
                    onClick = {
                        escolha.listItem = opcao.id
                        aberto = false
                    },
                )
            }
        }
    }
}
