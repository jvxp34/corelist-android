package br.com.comprafacil.ui.produtos

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ExpandLess
import androidx.compose.material.icons.outlined.ExpandMore
import androidx.compose.material.icons.outlined.QrCodeScanner
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.Sell
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.viewModelScope
import br.com.comprafacil.AppContainer
import br.com.comprafacil.data.api.PrecoNaLoja
import br.com.comprafacil.data.api.ResumoPreco
import br.com.comprafacil.data.api.Resultado
import br.com.comprafacil.data.api.chamar
import br.com.comprafacil.ui.comum.AvisoErro
import br.com.comprafacil.ui.comum.BotaoComoChegar
import br.com.comprafacil.ui.comum.BotaoPrincipal
import br.com.comprafacil.ui.comum.Carregando
import br.com.comprafacil.ui.comum.Cartao
import br.com.comprafacil.ui.comum.EstadoVazio
import br.com.comprafacil.ui.comum.LogoDaLoja
import br.com.comprafacil.ui.comum.appViewModel
import br.com.comprafacil.ui.comum.nomeDaLoja
import br.com.comprafacil.ui.comum.precoComMedida
import br.com.comprafacil.ui.comum.quandoFoi
import br.com.comprafacil.ui.theme.Cores
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

val PERIODOS = listOf(7, 30, 90)

class MenorPrecoViewModel(private val app: AppContainer) : ViewModel() {
    var texto by mutableStateOf("")
        private set
    var dias by mutableStateOf(30)
        private set
    var resultados by mutableStateOf<List<ResumoPreco>?>(null)
        private set
    var carregando by mutableStateOf(false)
        private set
    var erro by mutableStateOf<String?>(null)
        private set
    private var job: Job? = null

    fun mudarTexto(novo: String) {
        texto = novo
        buscar(espera = 250)
    }

    fun mudarDias(novo: Int) {
        dias = novo
        buscar()
    }

    fun buscar(espera: Long = 0) {
        job?.cancel()
        job = viewModelScope.launch {
            if (espera > 0) delay(espera) // espera a pessoa parar de digitar
            carregando = true
            when (val r = chamar { app.api.precos(texto.trim(), dias) }) {
                is Resultado.Ok -> {
                    resultados = r.valor.produtos
                    erro = null
                }
                is Resultado.Falha -> erro = r.mensagem
            }
            carregando = false
        }
    }
}

/** Preços de Manaus a partir das notas fiscais (mesma tela do site). */
@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
@Composable
fun TelaMenorPreco(onLerNota: () -> Unit) {
    val vm = appViewModel { MenorPrecoViewModel(it) }
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) { vm.buscar() }

    Column(Modifier.fillMaxSize().statusBarsPadding()) {
        Column(Modifier.padding(start = 20.dp, end = 20.dp, top = 16.dp)) {
            Text("Produtos com menor preço", style = MaterialTheme.typography.headlineSmall)
            Text(
                "Preços reais de Manaus, das notas fiscais lidas no CompraFácil. Ninguém vê quem comprou: só a loja, o preço e a data.",
                style = MaterialTheme.typography.bodyMedium,
                color = Cores.Secundario,
                modifier = Modifier.padding(top = 4.dp, bottom = 12.dp),
            )
            OutlinedTextField(
                value = vm.texto,
                onValueChange = vm::mudarTexto,
                placeholder = { Text("Buscar produto (ex.: arroz)") },
                leadingIcon = { Icon(Icons.Outlined.Search, null) },
                singleLine = true,
                shape = MaterialTheme.shapes.medium,
                modifier = Modifier.fillMaxWidth(),
            )
            SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth().padding(top = 10.dp)) {
                PERIODOS.forEachIndexed { i, periodo ->
                    SegmentedButton(
                        selected = vm.dias == periodo,
                        onClick = { vm.mudarDias(periodo) },
                        shape = SegmentedButtonDefaults.itemShape(i, PERIODOS.size),
                    ) { Text("$periodo dias") }
                }
            }
        }

        val resultados = vm.resultados
        val termo = vm.texto.trim()
        when {
            resultados == null && vm.erro != null -> br.com.comprafacil.ui.comum.ErroComTentarDeNovo(vm.erro!!, onTentar = { vm.buscar() })
            resultados == null -> Carregando()
            else -> PullToRefreshBox(isRefreshing = vm.carregando, onRefresh = { vm.buscar() }, modifier = Modifier.fillMaxSize()) {
                LazyColumn(
                    contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 12.dp, bottom = 24.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.fillMaxSize(),
                ) {
                    vm.erro?.let { item { AvisoErro(it) } }
                    if (resultados.isEmpty()) {
                        item {
                            EstadoVazio(
                                Icons.Outlined.Sell,
                                if (termo.isNotEmpty()) "Ainda não temos preço de \"$termo\"" else "Ainda não há preços da cidade",
                                "Os preços aparecem quando alguém lê uma nota fiscal com esse produto.",
                                acao = { BotaoPrincipal("Ler uma nota fiscal", onLerNota, icone = Icons.Outlined.QrCodeScanner) },
                            )
                        }
                    } else if (termo.isEmpty()) {
                        item {
                            Text(
                                "PREÇOS MAIS RECENTES",
                                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                                color = Cores.Secundario,
                            )
                        }
                    }
                    items(resultados, key = { it.produto.id }) { CartaoDoProduto(it) }
                    if (resultados.any { !it.produto.imagem.isNullOrBlank() }) {
                        item { CreditoOpenFoodFacts(Modifier.padding(top = 4.dp)) }
                    }
                }
            }
        }
    }
}

@Composable
private fun CartaoDoProduto(resumo: ResumoPreco) {
    var aberto by rememberSaveable(resumo.produto.id) { mutableStateOf(false) }
    // Atualiza na hora quando a pessoa adiciona a foto pelo código de barras.
    var produto by remember(resumo.produto) { mutableStateOf(resumo.produto) }
    val agora = resumo.maisBaratoAgora
    val menor = resumo.menorNoPeriodo
    val detalhes = listOfNotNull(produto.brand, produto.embalagem).filter { it.isNotBlank() }.joinToString(" · ")

    Cartao(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.Top) {
                FotoDoProduto(produto.imagem, produto.name)
                Column(Modifier.weight(1f).padding(start = 12.dp)) {
                    Text(produto.name, style = MaterialTheme.typography.titleSmall, maxLines = 2, overflow = TextOverflow.Ellipsis)
                    if (detalhes.isNotBlank()) {
                        Text(detalhes, style = MaterialTheme.typography.bodySmall, color = Cores.Secundario)
                    }
                    if (produto.imagem.isNullOrBlank()) {
                        AdicionarFoto(produto.id, produto.name) { novo ->
                            produto = produto.copy(
                                brand = novo.brand,
                                embalagem = (novo.embalagem?.takeIf { it.isNotBlank() } ?: novo.unit).orEmpty().trim(),
                                imagem = novo.imagem,
                            )
                        }
                    }
                }
            }

            if (agora != null) {
                Row(
                    Modifier
                        .padding(top = 12.dp)
                        .fillMaxWidth()
                        .background(Cores.MarcaClara, RoundedCornerShape(12.dp))
                        .padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    LogoDaLoja(agora.loja)
                    Column(Modifier.weight(1f).padding(horizontal = 12.dp)) {
                        Text(
                            "MAIS BARATO AGORA",
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                            color = Cores.MarcaEscura,
                        )
                        Text(precoComMedida(agora.preco, agora.medida), style = MaterialTheme.typography.headlineSmall)
                        Text(
                            "${nomeDaLoja(agora.loja)} · ${quandoFoi(agora.data)}",
                            style = MaterialTheme.typography.bodySmall,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                    BotaoComoChegar(agora.loja)
                }
            } else {
                Text(
                    "Ninguém registrou este produto hoje nem ontem. Os preços abaixo podem ter mudado.",
                    style = MaterialTheme.typography.bodySmall,
                    color = Cores.Secundario,
                    modifier = Modifier
                        .padding(top = 12.dp)
                        .fillMaxWidth()
                        .background(Cores.Fundo, RoundedCornerShape(12.dp))
                        .padding(12.dp),
                )
            }

            if (menor != null) {
                Text(
                    "Menor preço em ${menor.dias} dias: ${precoComMedida(menor.preco, menor.medida)} no ${nomeDaLoja(menor.loja)} (${quandoFoi(menor.data)})",
                    style = MaterialTheme.typography.bodySmall,
                    color = Cores.Secundario,
                    modifier = Modifier.padding(top = 10.dp),
                )
            }

            val n = resumo.lojas.size
            if (n > 0) {
                Row(
                    Modifier.padding(top = 8.dp).clickable { aberto = !aberto }.padding(vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(if (aberto) Icons.Outlined.ExpandLess else Icons.Outlined.ExpandMore, null, tint = Cores.Marca, modifier = Modifier.size(18.dp))
                    Text(
                        when {
                            aberto && n == 1 -> "Esconder a loja"
                            aberto -> "Esconder lojas"
                            n == 1 -> "Ver a loja"
                            else -> "Ver as $n lojas"
                        },
                        color = Cores.Marca,
                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                        modifier = Modifier.padding(start = 4.dp),
                    )
                }
            }
            if (aberto) {
                resumo.lojas.forEach { linha ->
                    HorizontalDivider(color = Cores.Linha)
                    LinhaDaLoja(linha)
                }
            }
        }
    }
}

@Composable
private fun LinhaDaLoja(linha: PrecoNaLoja) {
    Row(Modifier.fillMaxWidth().padding(vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
        LogoDaLoja(linha.loja, tamanho = 28.dp)
        Column(Modifier.weight(1f).padding(horizontal = 10.dp)) {
            Text(
                nomeDaLoja(linha.loja),
                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            linha.loja.endereco?.takeIf { it.isNotBlank() }?.let {
                Text(it, style = MaterialTheme.typography.labelSmall, color = Cores.Secundario, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(quandoFoi(linha.data), style = MaterialTheme.typography.labelSmall, color = Cores.Secundario)
                if (linha.desatualizado) {
                    Text(
                        "pode ter mudado",
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                        color = Cores.Texto,
                        maxLines = 1,
                        softWrap = false,
                        modifier = Modifier
                            .padding(start = 6.dp)
                            .background(Cores.Linha, RoundedCornerShape(50))
                            .padding(horizontal = 6.dp, vertical = 1.dp),
                    )
                }
            }
        }
        // Preço em cima e "Como chegar" embaixo: numa linha só não cabe no celular.
        Column(horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(
                precoComMedida(linha.preco, linha.medida),
                style = MaterialTheme.typography.titleSmall,
                color = if (linha.desatualizado) Cores.Secundario else Cores.Texto,
            )
            BotaoComoChegar(linha.loja, pequeno = true)
        }
    }
}
