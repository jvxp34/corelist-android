@file:OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)

package br.com.comprafacil.ui.assistente

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.LocationOn
import androidx.compose.material.icons.outlined.OpenInNew
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import br.com.comprafacil.AppContainer
import br.com.comprafacil.data.api.FalaDoChat
import br.com.comprafacil.data.api.FonteDaResposta
import br.com.comprafacil.data.api.Loja
import br.com.comprafacil.data.api.LojaDoChat
import br.com.comprafacil.data.api.PerguntaAoAssistente
import br.com.comprafacil.data.api.ProdutoDoChat
import br.com.comprafacil.data.api.Resultado
import br.com.comprafacil.data.api.chamar
import br.com.comprafacil.ui.comum.AvisoErro
import br.com.comprafacil.ui.comum.EstadoLocalizacao
import br.com.comprafacil.ui.comum.LogoDaLoja
import br.com.comprafacil.ui.comum.Origem
import br.com.comprafacil.ui.comum.appViewModel
import br.com.comprafacil.ui.comum.precoComMedida
import br.com.comprafacil.ui.comum.rememberLocalizacao
import br.com.comprafacil.ui.comum.textoDistancia
import br.com.comprafacil.ui.produtos.FotoDoProduto
import br.com.comprafacil.ui.theme.Cores
import kotlinx.coroutines.launch

/** Perguntas prontas, as mesmas do site. */
val SUGESTOES_ASSISTENTE = listOf(
    "Onde tem arroz mais barato perto de mim?",
    "Qual o menor preço do café?",
    "Onde compro leite mais barato perto de mim?",
)

/** Uma fala na tela; as respostas trazem também produtos, lojas e fontes. */
data class FalaNaTela(
    val papel: String,
    val texto: String,
    val produtos: List<ProdutoDoChat> = emptyList(),
    val lojas: List<LojaDoChat> = emptyList(),
    val fontes: List<FonteDaResposta> = emptyList(),
)

class AssistenteViewModel(private val app: AppContainer) : ViewModel() {
    val falas = mutableStateListOf<FalaNaTela>()
    var enviando by mutableStateOf(false)
        private set
    var erro by mutableStateOf<String?>(null)
        private set
    /** Quem escreveu a última resposta (gemini, groq, claude ou sem_ia). */
    var fonte by mutableStateOf<String?>(null)
        private set

    fun enviar(pergunta: String, origem: Origem?) {
        val limpa = pergunta.trim().take(500)
        if (limpa.isEmpty() || enviando) return
        falas += FalaNaTela("usuario", limpa)
        erro = null
        enviando = true
        viewModelScope.launch {
            val corpo = PerguntaAoAssistente(
                mensagens = falas.map { FalaDoChat(it.papel, it.texto) },
                // 3 casas (~100 m): basta para a distância, como no site.
                lat = origem?.lat?.let { Math.round(it * 1000) / 1000.0 },
                lng = origem?.lng?.let { Math.round(it * 1000) / 1000.0 },
            )
            when (val r = chamar { app.api.perguntarAoAssistente(corpo) }) {
                is Resultado.Ok -> {
                    val v = r.valor
                    fonte = v.fonte
                    falas += FalaNaTela("assistente", v.resposta, v.produtos, v.lojas, v.fontes)
                }
                is Resultado.Falha -> erro = r.mensagem
            }
            enviando = false
        }
    }
}

/**
 * Chat de preços: pergunta onde comprar mais barato. O servidor usa a IA
 * (quando configurada), os mercados perto e os preços das notas fiscais.
 */
@Composable
fun TelaAssistente(perguntaInicial: String?, onVoltar: () -> Unit) {
    val vm = appViewModel { AssistenteViewModel(it) }
    val localizacao = rememberLocalizacao()
    var texto by rememberSaveable { mutableStateOf("") }
    val rolagem = rememberLazyListState()

    // Veio de uma sugestão do Início: pergunta na hora, uma vez só.
    var perguntou by rememberSaveable { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        if (!perguntou && !perguntaInicial.isNullOrBlank() && vm.falas.isEmpty()) {
            perguntou = true
            vm.enviar(perguntaInicial, localizacao.origem)
        }
    }
    LaunchedEffect(vm.falas.size, vm.enviando) {
        val ultimo = vm.falas.size + (if (vm.enviando) 1 else 0)
        if (ultimo > 0) rolagem.animateScrollToItem(ultimo)
    }

    val enviar = {
        vm.enviar(texto, localizacao.origem)
        texto = ""
    }

    Scaffold(
        containerColor = Cores.Fundo,
        topBar = {
            TopAppBar(
                title = { Text("Assistente de preços") },
                navigationIcon = {
                    IconButton(onClick = onVoltar) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Voltar") }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Cores.Fundo),
            )
        },
        bottomBar = {
            Surface(color = Color.White, shadowElevation = 8.dp) {
                Column(Modifier.navigationBarsPadding().imePadding().padding(horizontal = 16.dp, vertical = 10.dp)) {
                    vm.erro?.let { AvisoErro(it, Modifier.padding(bottom = 8.dp)) }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        OutlinedTextField(
                            value = texto,
                            onValueChange = { if (it.length <= 500) texto = it },
                            placeholder = { Text("Ex.: onde o feijão está mais barato?") },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(
                                capitalization = KeyboardCapitalization.Sentences,
                                imeAction = ImeAction.Send,
                            ),
                            keyboardActions = KeyboardActions(onSend = { enviar() }),
                            shape = MaterialTheme.shapes.medium,
                            modifier = Modifier.weight(1f),
                        )
                        FilledIconButton(
                            onClick = enviar,
                            enabled = !vm.enviando && texto.isNotBlank(),
                            colors = IconButtonDefaults.filledIconButtonColors(containerColor = Cores.Marca),
                            modifier = Modifier.padding(start = 8.dp).size(48.dp),
                        ) { Icon(Icons.AutoMirrored.Filled.Send, "Enviar pergunta") }
                    }
                    Text(
                        creditoDaResposta(vm.fonte),
                        style = MaterialTheme.typography.labelSmall,
                        color = Cores.Secundario,
                        modifier = Modifier.padding(top = 6.dp),
                    )
                }
            }
        },
    ) { padding ->
        LazyColumn(
            state = rolagem,
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.padding(padding).fillMaxSize(),
        ) {
            item { CabecalhoLocalizacao(localizacao.estado, localizacao::pedir, localizacao.mensagem) }
            if (vm.falas.isEmpty() && !vm.enviando) {
                item {
                    Text(
                        "Pergunte onde comprar mais barato perto de você.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Cores.Secundario,
                    )
                    SugestoesDePergunta(Modifier.padding(top = 10.dp)) { vm.enviar(it, localizacao.origem) }
                }
            }
            itemsIndexed(vm.falas) { _, fala -> BalaoDaFala(fala) }
            if (vm.enviando) {
                item { Text("Procurando os preços…", style = MaterialTheme.typography.bodyMedium, color = Cores.Secundario) }
            }
        }
    }
}

/** Mesmo crédito do site: as bases usadas e a IA que escreveu, quando houver. */
private fun creditoDaResposta(fonte: String?): String =
    "Mercados do OpenStreetMap, promoções da internet e preços das notas fiscais do app; os preços podem ter mudado." +
        when (fonte) {
            "gemini" -> " Resposta escrita pelo Gemini, do Google."
            "groq" -> " Resposta escrita por um modelo aberto, via Groq."
            "claude" -> " Resposta escrita pelo Claude, da Anthropic."
            else -> ""
        }

@Composable
fun SugestoesDePergunta(modifier: Modifier = Modifier, onEscolher: (String) -> Unit) {
    FlowRow(modifier, horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        SUGESTOES_ASSISTENTE.forEach { sugestao ->
            Text(
                sugestao,
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier
                    .border(1.dp, Cores.Linha, RoundedCornerShape(50))
                    .background(Color.White, RoundedCornerShape(50))
                    .clickable { onEscolher(sugestao) }
                    .padding(horizontal = 12.dp, vertical = 8.dp),
            )
        }
    }
}

@Composable
private fun CabecalhoLocalizacao(estado: EstadoLocalizacao, onPedir: () -> Unit, mensagem: String?) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        if (estado == EstadoLocalizacao.ATIVA) {
            Row(
                Modifier.background(Cores.MarcaClara, RoundedCornerShape(50)).padding(horizontal = 10.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(Icons.Outlined.LocationOn, null, tint = Cores.MarcaEscura, modifier = Modifier.size(14.dp))
                Text(
                    "Usando sua localização",
                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                    color = Cores.MarcaEscura,
                    modifier = Modifier.padding(start = 4.dp),
                )
            }
        } else {
            TextButton(onClick = onPedir, enabled = estado != EstadoLocalizacao.BUSCANDO) {
                Icon(Icons.Outlined.LocationOn, null, modifier = Modifier.size(16.dp))
                Text(
                    if (estado == EstadoLocalizacao.BUSCANDO) "Buscando…" else "Usar minha localização",
                    modifier = Modifier.padding(start = 4.dp),
                )
            }
        }
        mensagem?.let {
            Text(
                it,
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.fillMaxWidth().background(Cores.Linha, RoundedCornerShape(12.dp)).padding(10.dp),
            )
        }
    }
}

@Composable
private fun BalaoDaFala(fala: FalaNaTela) {
    val minha = fala.papel == "usuario"
    Box(Modifier.fillMaxWidth(), contentAlignment = if (minha) Alignment.CenterEnd else Alignment.CenterStart) {
        Column(Modifier.widthIn(max = 320.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(
                fala.texto,
                style = MaterialTheme.typography.bodyLarge,
                color = if (minha) Color.White else Cores.Texto,
                modifier = Modifier
                    .then(
                        if (minha) Modifier.background(Cores.Marca, RoundedCornerShape(18.dp))
                        else Modifier.background(Color.White, RoundedCornerShape(18.dp)).border(1.dp, Cores.Linha, RoundedCornerShape(18.dp)),
                    )
                    .padding(horizontal = 14.dp, vertical = 10.dp),
            )
            fala.produtos.forEach { ProdutoCitado(it) }
            if (fala.lojas.isNotEmpty()) LojasCitadas(fala.lojas)
            // Os lugares do mapa já aparecem nas lojas: aqui, só a internet.
            val fontes = fala.fontes.filter { it.tipo != "maps" || fala.lojas.isEmpty() }
            if (fontes.isNotEmpty()) FontesDaResposta(fontes)
        }
    }
}

@Composable
private fun ProdutoCitado(produto: ProdutoDoChat) {
    Row(
        Modifier
            .fillMaxWidth()
            .background(Color.White, RoundedCornerShape(12.dp))
            .border(1.dp, Cores.Linha, RoundedCornerShape(12.dp))
            .padding(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        FotoDoProduto(produto.imagem, produto.nome, tamanho = 40.dp)
        Column(Modifier.padding(start = 10.dp)) {
            Text(produto.nome, style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold), maxLines = 2)
            val preco = produto.preco
            Text(
                if (preco != null) precoComMedida(preco, produto.medida.orEmpty()) + (produto.loja?.let { " · $it" } ?: "")
                else produto.embalagem ?: "Foto ilustrativa",
                style = MaterialTheme.typography.bodySmall,
                color = Cores.Secundario,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

/** Logos das lojas citadas, com a distância; o toque abre a rota no mapa. */
@Composable
private fun LojasCitadas(lojas: List<LojaDoChat>) {
    val context = LocalContext.current
    FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
        lojas.forEach { loja ->
            val nome = loja.marca ?: loja.nome
            Row(
                Modifier
                    .background(Color.White, RoundedCornerShape(50))
                    .border(1.dp, Cores.Linha, RoundedCornerShape(50))
                    .clickable(onClickLabel = "Como chegar em $nome") { abrir(context, loja.url) }
                    .padding(start = 3.dp, end = 10.dp, top = 3.dp, bottom = 3.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                LogoDaLoja(Loja(0, loja.nome, loja.marca, loja.icone), tamanho = 24.dp)
                Text(
                    nome,
                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(start = 6.dp).widthIn(max = 160.dp),
                )
                textoDistancia(loja.distanciaKm)?.let {
                    Text(" · $it", style = MaterialTheme.typography.labelMedium, color = Cores.Secundario)
                }
            }
        }
    }
}

@Composable
private fun FontesDaResposta(fontes: List<FonteDaResposta>) {
    val context = LocalContext.current
    FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
        fontes.forEach { f ->
            Row(
                Modifier
                    .background(Color.White, RoundedCornerShape(50))
                    .border(BorderStroke(1.dp, Cores.Linha), RoundedCornerShape(50))
                    .clickable { abrir(context, f.url) }
                    .padding(horizontal = 10.dp, vertical = 5.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(
                    when (f.tipo) {
                        "maps" -> Icons.Outlined.LocationOn
                        "busca" -> Icons.Outlined.Search
                        else -> Icons.Outlined.OpenInNew
                    },
                    null,
                    tint = Cores.Secundario,
                    modifier = Modifier.size(12.dp),
                )
                Text(
                    f.titulo.ifBlank { f.url },
                    style = MaterialTheme.typography.labelSmall,
                    color = Cores.Secundario,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(start = 4.dp).widthIn(max = 200.dp),
                )
            }
        }
    }
}

private fun abrir(context: android.content.Context, url: String) {
    runCatching { context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url))) }
}

/** Cartão do Início: abre o chat, já com a pergunta quando a pessoa toca numa sugestão. */
@Composable
fun CartaoAssistente(onAbrir: (String?) -> Unit) {
    br.com.comprafacil.ui.comum.Cartao(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(40.dp).background(Cores.MarcaClara, RoundedCornerShape(12.dp)), contentAlignment = Alignment.Center) {
                    Icon(Icons.Outlined.AutoAwesome, null, tint = Cores.Marca, modifier = Modifier.size(20.dp))
                }
                Column(Modifier.padding(start = 12.dp)) {
                    Text("Assistente de preços", style = MaterialTheme.typography.titleMedium)
                    Text(
                        "Pergunte onde comprar mais barato perto de você.",
                        style = MaterialTheme.typography.bodySmall,
                        color = Cores.Secundario,
                    )
                }
            }
            SugestoesDePergunta { onAbrir(it) }
            Text(
                "Fazer outra pergunta",
                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                color = Cores.Marca,
                modifier = Modifier.clickable { onAbrir(null) }.padding(vertical = 4.dp),
            )
        }
    }
}
