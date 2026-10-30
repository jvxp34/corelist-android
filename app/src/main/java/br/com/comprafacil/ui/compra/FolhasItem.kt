@file:OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)

package br.com.comprafacil.ui.compra

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.AssistChip
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import br.com.comprafacil.data.api.MEDIDA_KG
import br.com.comprafacil.data.api.MEDIDA_UNIDADE
import br.com.comprafacil.data.api.Produto
import br.com.comprafacil.data.api.ProdutoBusca
import br.com.comprafacil.ui.comum.BotaoPrincipal
import br.com.comprafacil.ui.comum.CampoPreco
import br.com.comprafacil.ui.comum.CampoQuantidade
import br.com.comprafacil.ui.comum.lerDecimal
import br.com.comprafacil.ui.comum.moeda
import br.com.comprafacil.ui.comum.normalizar
import br.com.comprafacil.ui.comum.precoParaCampo
import br.com.comprafacil.ui.theme.Cores
import java.math.BigDecimal
import java.math.RoundingMode

/** Formulário de um item: medida, quantidade e preço. */
@Composable
private fun FormularioItem(
    inicial: DadosItem,
    textoBotao: String,
    salvando: Boolean,
    onSalvar: (DadosItem) -> Unit,
    onRemover: (() -> Unit)?,
) {
    var medida by rememberSaveable { mutableStateOf(inicial.medida) }
    var quantidade by remember { mutableStateOf(inicial.quantidade) }
    var preco by rememberSaveable { mutableStateOf(precoParaCampo(inicial.preco)) }
    val porKg = medida == MEDIDA_KG
    val precoLido = lerDecimal(preco)
    val precoInvalido = preco.isNotBlank() && precoLido == null

    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
        Text("Como você compra?", style = MaterialTheme.typography.labelLarge)
        SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
            SegmentedButton(
                selected = !porKg,
                onClick = {
                    if (porKg) {
                        medida = MEDIDA_UNIDADE
                        // Por unidade a quantidade é inteira.
                        quantidade = quantidade.setScale(0, RoundingMode.CEILING).max(BigDecimal.ONE)
                    }
                },
                shape = SegmentedButtonDefaults.itemShape(0, 2),
            ) { Text("Unidade") }
            SegmentedButton(
                selected = porKg,
                onClick = { medida = MEDIDA_KG },
                shape = SegmentedButtonDefaults.itemShape(1, 2),
            ) { Text("Peso (kg)") }
        }

        Text(if (porKg) "Peso" else "Quantidade", style = MaterialTheme.typography.labelLarge)
        CampoQuantidade(quantidade, porKg, { quantidade = it }, Modifier.fillMaxWidth())

        CampoPreco(preco, { preco = it }, if (porKg) "Preço do kg" else "Preço da unidade")
        if (precoInvalido) {
            Text("Confira o preço.", color = Cores.EstourouTexto, style = MaterialTheme.typography.bodySmall)
        }

        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("Subtotal", style = MaterialTheme.typography.bodyMedium, color = Cores.Secundario, modifier = Modifier.weight(1f))
            Text(
                precoLido?.let { moeda(it.multiply(quantidade)) } ?: "sem preço",
                style = MaterialTheme.typography.titleMedium,
            )
        }

        BotaoPrincipal(
            textoBotao,
            { onSalvar(DadosItem(quantidade, medida, precoLido)) },
            Modifier.fillMaxWidth(),
            carregando = salvando,
            habilitado = !precoInvalido,
        )
        if (onRemover != null) {
            TextButton(onClick = onRemover, enabled = !salvando, modifier = Modifier.align(Alignment.CenterHorizontally)) {
                Icon(Icons.Outlined.DeleteOutline, null, tint = Cores.EstourouTexto)
                Text("Remover da lista", color = Cores.EstourouTexto, modifier = Modifier.padding(start = 6.dp))
            }
        }
    }
}

@Composable
fun FolhaItem(
    titulo: String,
    inicial: DadosItem,
    textoBotao: String,
    salvando: Boolean,
    onFechar: () -> Unit,
    onSalvar: (DadosItem) -> Unit,
    onRemover: (() -> Unit)? = null,
) {
    ModalBottomSheet(
        onDismissRequest = onFechar,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = androidx.compose.ui.graphics.Color.White,
    ) {
        Column(
            Modifier
                .verticalScroll(rememberScrollState())
                .imePadding()
                .navigationBarsPadding()
                .padding(horizontal = 20.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(titulo, style = MaterialTheme.typography.titleLarge)
            FormularioItem(inicial, textoBotao, salvando, onSalvar, onRemover)
        }
    }
}

/**
 * Adicionar à lista: busca no servidor (lists/produtos/?q=), atalhos com os
 * mais comprados e "Cadastrar" quando não acha. O último preço e a medida
 * que a pessoa usou já vêm preenchidos.
 */
@Composable
fun FolhaAdicionar(
    atalhos: List<ProdutoBusca>,
    resultados: List<ProdutoBusca>,
    termoBuscado: String,
    buscando: Boolean,
    salvando: Boolean,
    onBuscar: (String) -> Unit,
    onFechar: () -> Unit,
    onCadastrar: (String, (Produto) -> Unit) -> Unit,
    onAdicionar: (Produto, DadosItem) -> Unit,
) {
    var busca by rememberSaveable { mutableStateOf("") }
    var escolhido by remember { mutableStateOf<ProdutoBusca?>(null) }

    ModalBottomSheet(
        onDismissRequest = onFechar,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = androidx.compose.ui.graphics.Color.White,
    ) {
        val produto = escolhido
        if (produto != null) {
            Column(
                Modifier
                    .verticalScroll(rememberScrollState())
                    .imePadding()
                    .navigationBarsPadding()
                    .padding(horizontal = 20.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = { escolhido = null }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Escolher outro produto")
                    }
                    Column(Modifier.weight(1f)) {
                        Text(produto.name, style = MaterialTheme.typography.titleLarge, maxLines = 2, overflow = TextOverflow.Ellipsis)
                        if (produto.detalhes.isNotBlank()) {
                            Text(produto.detalhes, style = MaterialTheme.typography.bodySmall, color = Cores.Secundario)
                        }
                    }
                }
                FormularioItem(
                    inicial = DadosItem(
                        BigDecimal.ONE,
                        if (produto.ultimaMedida == MEDIDA_KG) MEDIDA_KG else MEDIDA_UNIDADE,
                        produto.ultimoPreco?.takeIf { it.signum() > 0 },
                    ),
                    textoBotao = "Adicionar à lista",
                    salvando = salvando,
                    onSalvar = { onAdicionar(produto.comoProduto(), it) },
                    onRemover = null,
                )
            }
        } else {
            val termo = busca.trim()
            // Enquanto o servidor não responde, não mostra o resultado de outro termo.
            val achados = if (termo.isNotEmpty() && termoBuscado == termo) resultados else emptyList()
            val existeIgual = achados.any { normalizar(it.name) == normalizar(termo) }
            val maisComprados = atalhos.filter { it.vezes > 0 }.take(8)

            Column(Modifier.imePadding().padding(horizontal = 20.dp)) {
                Text("Adicionar produto", style = MaterialTheme.typography.titleLarge)
                OutlinedTextField(
                    value = busca,
                    onValueChange = {
                        busca = it
                        onBuscar(it)
                    },
                    placeholder = { Text("Buscar produto") },
                    leadingIcon = { Icon(Icons.Outlined.Search, null) },
                    trailingIcon = {
                        if (buscando) CircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.dp, color = Cores.Marca)
                    },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
                    shape = MaterialTheme.shapes.medium,
                    modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp),
                )
                LazyColumn(Modifier.fillMaxWidth().heightIn(max = 420.dp)) {
                    if (termo.isEmpty()) {
                        if (maisComprados.isNotEmpty()) {
                            item {
                                Text(
                                    "Você costuma comprar",
                                    style = MaterialTheme.typography.labelLarge,
                                    color = Cores.Secundario,
                                    modifier = Modifier.padding(bottom = 8.dp),
                                )
                                FlowRow(
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    verticalArrangement = Arrangement.spacedBy(8.dp),
                                ) {
                                    maisComprados.forEach { p ->
                                        AssistChip(onClick = { escolhido = p }, label = { Text(p.name) })
                                    }
                                }
                            }
                        } else {
                            item {
                                Text(
                                    "Digite o nome do produto para buscar ou cadastrar.",
                                    color = Cores.Secundario,
                                    modifier = Modifier.padding(vertical = 16.dp),
                                )
                            }
                        }
                    } else {
                        items(achados, key = { it.id }) { p ->
                            Column(
                                Modifier
                                    .fillMaxWidth()
                                    .clickable { escolhido = p }
                                    .padding(vertical = 12.dp),
                            ) {
                                Text(p.name, style = MaterialTheme.typography.bodyLarge)
                                val extra = listOfNotNull(
                                    p.detalhes.takeIf { it.isNotBlank() },
                                    p.ultimoPreco?.takeIf { it.signum() > 0 }?.let {
                                        "último ${moeda(it)}${if (p.ultimaMedida == MEDIDA_KG) "/kg" else ""}"
                                    },
                                ).joinToString(" · ")
                                if (extra.isNotBlank()) {
                                    Text(extra, style = MaterialTheme.typography.bodySmall, color = Cores.Secundario)
                                }
                            }
                            HorizontalDivider(color = Cores.Linha)
                        }
                        if (!existeIgual && !buscando) {
                            item {
                                Row(
                                    Modifier
                                        .fillMaxWidth()
                                        .clickable(enabled = !salvando) {
                                            onCadastrar(termo) { novo ->
                                                escolhido = ProdutoBusca(novo.id, novo.name, novo.brand, novo.embalagem)
                                            }
                                        }
                                        .padding(vertical = 14.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                ) {
                                    Icon(Icons.Default.Add, null, tint = Cores.Marca)
                                    Text(
                                        "Cadastrar “$termo”",
                                        color = Cores.Marca,
                                        style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.SemiBold),
                                        modifier = Modifier.padding(start = 12.dp),
                                    )
                                }
                            }
                        }
                    }
                }
                Spacer(Modifier.navigationBarsPadding().padding(bottom = 12.dp))
            }
        }
    }
}

