@file:OptIn(ExperimentalMaterial3Api::class)

package br.com.comprafacil.ui.produtos

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Inventory2
import androidx.compose.material.icons.outlined.QrCodeScanner
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.viewModelScope
import br.com.comprafacil.AppContainer
import br.com.comprafacil.data.api.NovoProduto
import br.com.comprafacil.data.api.Produto
import br.com.comprafacil.data.api.Resultado
import br.com.comprafacil.data.api.chamar
import br.com.comprafacil.ui.comum.AvisoErro
import br.com.comprafacil.ui.comum.BotaoPrincipal
import br.com.comprafacil.ui.comum.BotaoSecundario
import br.com.comprafacil.ui.comum.Carregando
import br.com.comprafacil.ui.comum.Cartao
import br.com.comprafacil.ui.comum.ErroComTentarDeNovo
import br.com.comprafacil.ui.comum.EstadoVazio
import br.com.comprafacil.ui.comum.appViewModel
import br.com.comprafacil.ui.comum.normalizar
import br.com.comprafacil.ui.nota.FORMATOS_PRODUTO
import br.com.comprafacil.ui.nota.LeitorCamera
import br.com.comprafacil.ui.theme.Cores
import kotlinx.coroutines.launch

class ProdutosViewModel(private val app: AppContainer) : ViewModel() {
    var produtos by mutableStateOf<List<Produto>?>(null)
        private set
    var carregando by mutableStateOf(false)
        private set
    var erro by mutableStateOf<String?>(null)
        private set

    // Formulário de cadastro
    var codigo by mutableStateOf("")
    var nome by mutableStateOf("")
    var marca by mutableStateOf("")
    var embalagem by mutableStateOf("")
    private var imagem = ""
    var origem by mutableStateOf("manual")
        private set
    var buscando by mutableStateOf(false)
        private set
    var salvando by mutableStateOf(false)
        private set
    var avisoForm by mutableStateOf<String?>(null)
        private set
    var erroForm by mutableStateOf<String?>(null)
        private set

    fun carregar() {
        if (carregando) return
        viewModelScope.launch {
            carregando = true
            when (val r = chamar { app.api.produtos() }) {
                is Resultado.Ok -> {
                    produtos = r.valor.sortedBy { normalizar(it.name) }
                    erro = null
                }
                is Resultado.Falha -> erro = r.mensagem
            }
            carregando = false
        }
    }

    fun limparForm() {
        codigo = ""; nome = ""; marca = ""; embalagem = ""; imagem = ""
        origem = "manual"; avisoForm = null; erroForm = null
    }

    /** Busca o código no catálogo e, se não achar, no Open Food Facts. */
    fun buscarCodigo(lido: String) {
        codigo = lido.filter(Char::isDigit)
        if (codigo.isEmpty()) return
        viewModelScope.launch {
            buscando = true
            erroForm = null
            avisoForm = null
            when (val r = chamar { app.api.buscarPorCodigo(codigo) }) {
                is Resultado.Ok -> {
                    val c = r.valor
                    val p = c.produto
                    val s = c.sugestao
                    when {
                        p != null -> avisoForm = "“${p.name}” já está no catálogo."
                        s != null -> {
                            nome = s.name
                            marca = s.brand.orEmpty()
                            embalagem = s.embalagem.orEmpty()
                            imagem = s.imagem.orEmpty()
                            origem = s.origem ?: "openfoodfacts"
                            avisoForm = "Dados encontrados no Open Food Facts. Confira antes de salvar."
                        }
                    }
                }
                is Resultado.Falha -> erroForm = r.mensagem
            }
            buscando = false
        }
    }

    fun salvar(onOk: () -> Unit) {
        if (nome.isBlank()) {
            erroForm = "Informe o nome do produto."
            return
        }
        viewModelScope.launch {
            salvando = true
            erroForm = null
            val corpo = NovoProduto(
                name = nome.trim(),
                brand = marca.trim(),
                embalagem = embalagem.trim(),
                barcode = codigo.trim(),
                imagem = imagem,
                origem = origem,
            )
            when (val r = chamar { app.api.criarProduto(corpo) }) {
                is Resultado.Ok -> {
                    produtos = (produtos.orEmpty() + r.valor).sortedBy { normalizar(it.name) }
                    limparForm()
                    onOk()
                }
                is Resultado.Falha -> erroForm = r.mensagem
            }
            salvando = false
        }
    }
}

@Composable
fun TelaProdutos() {
    val vm = appViewModel { ProdutosViewModel(it) }
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) { vm.carregar() }
    var busca by rememberSaveable { mutableStateOf("") }
    var cadastrando by rememberSaveable { mutableStateOf(false) }

    Column(Modifier.fillMaxSize().statusBarsPadding()) {
        Column(Modifier.padding(start = 20.dp, end = 20.dp, top = 16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Produtos", style = MaterialTheme.typography.headlineSmall, modifier = Modifier.weight(1f))
                Button(
                    onClick = {
                        vm.limparForm()
                        cadastrando = true
                    },
                    shape = MaterialTheme.shapes.medium,
                    colors = ButtonDefaults.buttonColors(containerColor = Cores.Marca),
                    contentPadding = PaddingValues(horizontal = 16.dp),
                    modifier = Modifier.height(44.dp),
                ) {
                    Icon(Icons.Default.Add, null, Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Cadastrar")
                }
            }
            Text(
                "O catálogo é compartilhado: o que você cadastra aparece para todos.",
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

        val produtos = vm.produtos
        when {
            produtos == null && vm.erro != null -> ErroComTentarDeNovo(vm.erro!!, vm::carregar)
            produtos == null -> Carregando()
            else -> PullToRefreshBox(isRefreshing = vm.carregando, onRefresh = vm::carregar, modifier = Modifier.fillMaxSize()) {
                val termo = normalizar(busca)
                val mostrados = remember(produtos, termo) {
                    produtos.filter {
                        termo.isEmpty() || normalizar(it.name).contains(termo) || it.barcode.orEmpty().contains(termo)
                    }
                }
                LazyColumn(
                    contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 12.dp, bottom = 24.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxSize(),
                ) {
                    if (mostrados.isEmpty()) {
                        item {
                            EstadoVazio(
                                Icons.Outlined.Inventory2,
                                if (busca.isBlank()) "Catálogo vazio" else "Nada encontrado",
                                "Cadastre um produto digitando o nome ou lendo o código de barras.",
                            )
                        }
                    }
                    items(mostrados, key = { it.id }) { p ->
                        Cartao(Modifier.fillMaxWidth()) {
                            Column(Modifier.padding(horizontal = 16.dp, vertical = 12.dp)) {
                                Text(p.name, style = MaterialTheme.typography.bodyLarge)
                                val extra = listOfNotNull(
                                    p.detalhes.takeIf { it.isNotBlank() },
                                    p.barcode?.takeIf { it.isNotBlank() },
                                ).joinToString(" · ")
                                if (extra.isNotBlank()) {
                                    Text(extra, style = MaterialTheme.typography.bodySmall, color = Cores.Secundario)
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (cadastrando) {
        FolhaCadastro(vm, onFechar = { cadastrando = false })
    }
}

@Composable
private fun FolhaCadastro(vm: ProdutosViewModel, onFechar: () -> Unit) {
    var lendoCodigo by remember { mutableStateOf(false) }
    val context = LocalContext.current
    val pedir = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { ok ->
        if (ok) lendoCodigo = true
    }
    val abrirLeitor = {
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
            lendoCodigo = true
        } else {
            pedir.launch(Manifest.permission.CAMERA)
        }
    }

    ModalBottomSheet(
        onDismissRequest = onFechar,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = Color.White,
    ) {
        Column(
            Modifier
                .verticalScroll(rememberScrollState())
                .imePadding()
                .navigationBarsPadding()
                .padding(horizontal = 20.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text("Cadastrar produto", style = MaterialTheme.typography.titleLarge)
            BotaoSecundario(
                if (vm.buscando) "Buscando…" else "Ler código de barras",
                abrirLeitor,
                Modifier.fillMaxWidth(),
                icone = Icons.Outlined.QrCodeScanner,
                habilitado = !vm.buscando,
            )
            OutlinedTextField(
                value = vm.codigo,
                onValueChange = { vm.codigo = it.filter(Char::isDigit) },
                label = { Text("Código de barras (opcional)") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                trailingIcon = {
                    if (vm.codigo.length >= 8) {
                        IconButton(onClick = { vm.buscarCodigo(vm.codigo) }) { Icon(Icons.Outlined.Search, "Buscar código") }
                    }
                },
                shape = MaterialTheme.shapes.medium,
                modifier = Modifier.fillMaxWidth(),
            )
            vm.avisoForm?.let {
                Text(it, style = MaterialTheme.typography.bodySmall, color = Cores.MarcaEscura)
            }
            Campo(vm.nome, { vm.nome = it }, "Nome")
            Campo(vm.marca, { vm.marca = it }, "Marca (opcional)")
            Campo(vm.embalagem, { vm.embalagem = it }, "Embalagem (ex.: 5 kg, 395 g)")
            vm.erroForm?.let { AvisoErro(it) }
            BotaoPrincipal("Salvar produto", { vm.salvar(onFechar) }, Modifier.fillMaxWidth(), carregando = vm.salvando)
            if (vm.origem == "openfoodfacts") {
                // Crédito exigido pela licença dos dados (igual ao site).
                Text(
                    "Dados de produto: Open Food Facts (openfoodfacts.org), licença ODbL.",
                    style = MaterialTheme.typography.bodySmall,
                    color = Cores.Secundario,
                )
            }
        }
    }

    if (lendoCodigo) {
        DialogoLerCodigo(
            onFechar = { lendoCodigo = false },
            onCodigo = {
                lendoCodigo = false
                vm.buscarCodigo(it)
            },
        )
    }
}

@Composable
private fun Campo(valor: String, onChange: (String) -> Unit, rotulo: String) {
    OutlinedTextField(
        value = valor,
        onValueChange = onChange,
        label = { Text(rotulo) },
        singleLine = true,
        keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
        shape = MaterialTheme.shapes.medium,
        modifier = Modifier.fillMaxWidth(),
    )
}

@Composable
private fun DialogoLerCodigo(onFechar: () -> Unit, onCodigo: (String) -> Unit) {
    var lido by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { lido = false }
    Dialog(onDismissRequest = onFechar, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Column(
            Modifier.fillMaxSize().background(Color(0xFF0F1020)).padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text(
                    "Código de barras",
                    style = MaterialTheme.typography.titleMedium,
                    color = Color.White,
                    modifier = Modifier.weight(1f),
                )
                IconButton(onClick = onFechar) { Icon(Icons.Outlined.Close, "Fechar", tint = Color.White) }
            }
            Spacer(Modifier.weight(1f))
            Box(
                Modifier.fillMaxWidth().aspectRatio(1.4f).clip(RoundedCornerShape(20.dp)).background(Color(0xFF1B1D33)),
            ) {
                LeitorCamera(
                    formatos = FORMATOS_PRODUTO,
                    lanterna = false,
                    onCodigo = {
                        if (!lido) {
                            lido = true
                            onCodigo(it)
                        }
                    },
                    modifier = Modifier.fillMaxSize(),
                )
            }
            Text(
                "Aponte para o código de barras da embalagem.",
                color = Color(0xFFC7C9D3),
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(top = 16.dp),
            )
            Spacer(Modifier.weight(1f))
        }
    }
}
