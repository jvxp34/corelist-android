package br.com.comprafacil.ui.produtos

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.LocationOn
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import br.com.comprafacil.data.api.Loja
import br.com.comprafacil.ui.comum.EstadoLocalizacao
import br.com.comprafacil.ui.comum.Localizacao
import br.com.comprafacil.ui.comum.Origem
import br.com.comprafacil.ui.comum.rememberLocalizacao
import br.com.comprafacil.ui.comum.textoDistancia
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

    // ---- Perto de mim (a posição fica só nesta tela, não é salva) ----
    var origem by mutableStateOf<Origem?>(null)
        private set
    /** Raio em km (2, 5 ou 10); null = toda a cidade. */
    var raio by mutableStateOf<Int?>(5)
        private set
    var endereco by mutableStateOf<String?>(null)
        private set
    private var enderecoJob: Job? = null

    fun mudarOrigem(nova: Origem?) {
        if (nova == origem) return
        origem = nova
        endereco = null
        enderecoJob?.cancel()
        if (nova != null) {
            enderecoJob = viewModelScope.launch {
                // O mapa às vezes demora na primeira vez: tenta de novo uma vez.
                repeat(2) { tentativa ->
                    val texto = (chamar { app.api.ondeEstou(nova.lat, nova.lng) } as? Resultado.Ok)?.valor?.texto
                    if (texto != null) {
                        endereco = texto
                        return@launch
                    }
                    if (tentativa == 0) delay(2000)
                }
            }
        }
        buscar()
    }

    fun mudarRaio(novo: Int?) {
        raio = novo
        buscar()
    }

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
            val o = origem
            when (val r = chamar { app.api.precos(texto.trim(), dias, o?.lat, o?.lng, if (o != null) raio else null) }) {
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
    val localizacao = rememberLocalizacao()
    LaunchedEffect(localizacao.origem) { vm.mudarOrigem(localizacao.origem) }

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
            PertoDeMim(localizacao, vm)
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
                                when {
                                    vm.origem != null && vm.raio != null -> "Nenhuma loja com preço até ${vm.raio} km de você"
                                    termo.isNotEmpty() -> "Ainda não temos preço de \"$termo\""
                                    else -> "Ainda não há preços da cidade"
                                },
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
                            "${nomeComDistancia(agora.loja)} · ${quandoFoi(agora.data)}",
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
                    "Menor preço em ${menor.dias} dias: ${precoComMedida(menor.preco, menor.medida)} no ${nomeComDistancia(menor.loja)} (${quandoFoi(menor.data)})",
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
                nomeComDistancia(linha.loja),
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

/** "Assaí Atacadista · a 1,2 km" quando a busca usa a localização. */
private fun nomeComDistancia(loja: Loja): String =
    nomeDaLoja(loja) + (textoDistancia(loja.distanciaKm)?.let { " · a $it" } ?: "")

/** "Lojas perto de mim" e, com a localização, o raio e onde a pessoa está. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun PertoDeMim(localizacao: Localizacao, vm: MenorPrecoViewModel) {
    Column(Modifier.fillMaxWidth().padding(top = 10.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        if (localizacao.estado == EstadoLocalizacao.ATIVA) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Outlined.LocationOn, null, tint = Cores.Marca, modifier = Modifier.size(18.dp))
                Text(
                    "Perto de mim:",
                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                    modifier = Modifier.padding(start = 4.dp).weight(1f),
                )
                IconButton(onClick = localizacao::desligar, modifier = Modifier.size(36.dp)) {
                    Icon(Icons.Outlined.Close, "Parar de usar minha localização", tint = Cores.Secundario)
                }
            }
            FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                (RAIOS_KM + listOf(null)).forEach { opcao ->
                    FilterChip(
                        selected = vm.raio == opcao,
                        onClick = { vm.mudarRaio(opcao) },
                        label = { Text(if (opcao != null) "até $opcao km" else "toda a cidade") },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = Cores.Marca,
                            selectedLabelColor = Color.White,
                        ),
                    )
                }
            }
            vm.origem?.let { OndeEstou(it, vm.endereco) }
        } else {
            val buscando = localizacao.estado == EstadoLocalizacao.BUSCANDO
            OutlinedButton(
                onClick = localizacao::pedir,
                enabled = !buscando,
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(1.dp, Cores.MarcaClara),
                colors = ButtonDefaults.outlinedButtonColors(containerColor = Cores.MarcaClara, contentColor = Cores.MarcaEscura),
            ) {
                Icon(Icons.Outlined.LocationOn, null, modifier = Modifier.size(16.dp))
                Text(
                    if (buscando) "Buscando sua localização…" else "Lojas perto de mim",
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(start = 6.dp),
                )
            }
        }
        localizacao.mensagem?.let { aviso ->
            // Verde, âmbar e vermelho ficam só para o orçamento: aviso neutro.
            Text(
                aviso,
                style = MaterialTheme.typography.bodySmall,
                color = Cores.Texto,
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Cores.Linha, RoundedCornerShape(12.dp))
                    .padding(horizontal = 12.dp, vertical = 8.dp),
            )
        }
    }
}

/** "Você está perto de: Rua X, Bairro" e o botão para ver o ponto no mapa. */
@Composable
private fun OndeEstou(origem: Origem, endereco: String?) {
    val context = LocalContext.current
    Row(
        Modifier
            .fillMaxWidth()
            .background(Cores.MarcaClara, RoundedCornerShape(12.dp))
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(Icons.Outlined.LocationOn, null, tint = Cores.Marca, modifier = Modifier.size(16.dp))
        Text(
            if (endereco != null) "Você está perto de: $endereco" else "Usando sua localização",
            style = MaterialTheme.typography.bodySmall,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f).padding(horizontal = 6.dp),
        )
        Text(
            "Ver no mapa",
            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
            color = Cores.Marca,
            modifier = Modifier.clickable {
                val uri = Uri.parse("geo:${origem.lat},${origem.lng}?q=${origem.lat},${origem.lng}(Você está aqui)")
                runCatching { context.startActivity(Intent(Intent.ACTION_VIEW, uri)) }
            },
        )
    }
}

private val RAIOS_KM = listOf(2, 5, 10)
