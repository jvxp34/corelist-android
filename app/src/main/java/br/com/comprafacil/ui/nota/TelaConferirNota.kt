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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.ReceiptLong
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.QrCodeScanner
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import br.com.comprafacil.data.api.Lista
import br.com.comprafacil.data.api.MEDIDA_KG
import br.com.comprafacil.ui.comum.AvisoErro
import br.com.comprafacil.ui.comum.BotaoPrincipal
import br.com.comprafacil.ui.comum.BotaoSecundario
import br.com.comprafacil.ui.comum.Cartao
import br.com.comprafacil.ui.comum.appViewModel
import br.com.comprafacil.ui.comum.data
import br.com.comprafacil.ui.comum.moeda
import br.com.comprafacil.ui.comum.nomeProprio
import br.com.comprafacil.ui.comum.quantidadeComMedida
import br.com.comprafacil.ui.theme.Cores

@Composable
fun TelaConferirNota(
    link: String,
    onVoltar: () -> Unit,
    onLerOutra: () -> Unit,
    onAbrirLista: (Int) -> Unit,
    onInicio: () -> Unit,
) {
    val vm = appViewModel(key = "nota-$link") { NotaViewModel(it, link) }
    val importada = vm.importada

    if (importada != null) {
        TelaCompraConcluida(importada, onVerCompra = { onAbrirLista(importada.id) }, onInicio = onInicio)
        return
    }

    Scaffold(
        containerColor = Cores.Fundo,
        topBar = {
            TopAppBar(
                title = { Text(if (vm.nota == null) "" else "Confira os itens") },
                navigationIcon = {
                    IconButton(onClick = onVoltar) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Voltar") }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Cores.Fundo),
            )
        },
        bottomBar = {
            if (vm.nota != null && vm.jaImportada == null) {
                Surface(color = Color.White, shadowElevation = 8.dp) {
                    Column(
                        Modifier.navigationBarsPadding().padding(horizontal = 16.dp, vertical = 10.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        vm.erroImportar?.let { AvisoErro(it) }
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("Total", color = Cores.Secundario, modifier = Modifier.weight(1f))
                            Text(moeda(vm.total), style = MaterialTheme.typography.titleMedium)
                        }
                        BotaoPrincipal(
                            "Salvar compra",
                            vm::importar,
                            Modifier.fillMaxWidth(),
                            carregando = vm.importando,
                            icone = Icons.Default.Check,
                        )
                    }
                }
            }
        },
    ) { padding ->
        Box(Modifier.padding(padding).fillMaxSize()) {
            val nota = vm.nota
            val jaImportada = vm.jaImportada
            when {
                vm.lendo -> Lendo()
                nota == null -> FalhaLeitura(vm.erro ?: "Não foi possível ler a nota.", vm::ler, onLerOutra)
                jaImportada != null -> JaImportada(onVer = { onAbrirLista(jaImportada) }, onLerOutra = onLerOutra)
                else -> LazyColumn(
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    item {
                        Cartao(Modifier.fillMaxWidth()) {
                            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Text("Nota lida", style = MaterialTheme.typography.labelMedium, color = Cores.Marca)
                                Text(nomeProprio(nota.loja).ifBlank { "Loja" }, style = MaterialTheme.typography.titleMedium)
                                Text(
                                    listOf(data(nota.emitidaEm), "${nota.itens.size} itens", moeda(nota.total))
                                        .filter { it.isNotBlank() }.joinToString(" · "),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = Cores.Secundario,
                                )
                            }
                        }
                    }
                    item {
                        OutlinedTextField(
                            value = vm.nomeCompra,
                            onValueChange = { vm.nomeCompra = it },
                            label = { Text("Nome da compra") },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
                            shape = MaterialTheme.shapes.medium,
                            modifier = Modifier.fillMaxWidth(),
                        )
                    }
                    item {
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 4.dp)) {
                            Icon(Icons.Outlined.Info, null, tint = Cores.Secundario, modifier = Modifier.size(18.dp))
                            Text(
                                "A nota traz nomes abreviados. Corrija o que quiser e remova o que não for seu.",
                                style = MaterialTheme.typography.bodySmall,
                                color = Cores.Secundario,
                                modifier = Modifier.padding(start = 8.dp),
                            )
                        }
                    }
                    items(vm.itens, key = { it.id }) { item ->
                        Cartao(Modifier.fillMaxWidth()) {
                            Column(Modifier.padding(start = 12.dp, end = 4.dp, top = 8.dp, bottom = 12.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    OutlinedTextField(
                                        value = item.nome,
                                        onValueChange = { item.nome = it },
                                        singleLine = true,
                                        textStyle = MaterialTheme.typography.bodyLarge,
                                        shape = MaterialTheme.shapes.medium,
                                        modifier = Modifier.weight(1f),
                                    )
                                    IconButton(onClick = { vm.remover(item) }) {
                                        Icon(Icons.Outlined.Close, "Remover ${item.nome}", tint = Cores.Secundario)
                                    }
                                }
                                val o = item.original
                                val porKg = o.medida == MEDIDA_KG
                                Row(Modifier.padding(top = 6.dp, end = 12.dp)) {
                                    Text(
                                        "${quantidadeComMedida(o.quantidade, porKg)} × ${moeda(o.preco)}${if (porKg) "/kg" else ""}",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = Cores.Secundario,
                                        modifier = Modifier.weight(1f),
                                    )
                                    Text(moeda(o.preco.multiply(o.quantidade)), style = MaterialTheme.typography.titleSmall)
                                }
                                if (o.produto != null) {
                                    Text(
                                        "Já está no catálogo",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = Cores.OkTexto,
                                        modifier = Modifier.padding(top = 2.dp),
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun Lendo() {
    Column(
        Modifier.fillMaxSize().padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(20.dp),
    ) {
        Spacer(Modifier.height(32.dp))
        Box(Modifier.size(104.dp).background(Cores.MarcaClara, CircleShape), contentAlignment = Alignment.Center) {
            Icon(Icons.AutoMirrored.Outlined.ReceiptLong, null, tint = Cores.Marca, modifier = Modifier.size(48.dp))
        }
        Text("Lendo sua nota…", style = MaterialTheme.typography.headlineSmall)
        Text(
            "Estamos buscando os itens na página pública da Sefaz-AM. Leva só alguns segundos.",
            style = MaterialTheme.typography.bodyMedium,
            color = Cores.Secundario,
            textAlign = TextAlign.Center,
        )
        Cartao(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                Etapa("QR code lido", feita = true)
                Etapa("Buscando a nota na Sefaz-AM", feita = false)
            }
        }
    }
}

@Composable
private fun Etapa(texto: String, feita: Boolean) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        if (feita) {
            Box(Modifier.size(28.dp).background(Cores.OkTexto, CircleShape), contentAlignment = Alignment.Center) {
                Icon(Icons.Default.Check, null, tint = Color.White, modifier = Modifier.size(16.dp))
            }
        } else {
            CircularProgressIndicator(Modifier.size(28.dp), color = Cores.Marca, trackColor = Cores.MarcaClara, strokeWidth = 3.dp)
        }
        Text(texto, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.padding(start = 12.dp))
    }
}

@Composable
private fun FalhaLeitura(mensagem: String, onTentar: () -> Unit, onLerOutra: () -> Unit) {
    Column(Modifier.fillMaxSize().padding(24.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text("Não deu para ler a nota", style = MaterialTheme.typography.headlineSmall)
        AvisoErro(mensagem)
        BotaoPrincipal("Tentar de novo", onTentar, Modifier.fillMaxWidth())
        BotaoSecundario("Ler outra nota", onLerOutra, Modifier.fillMaxWidth(), icone = Icons.Outlined.QrCodeScanner)
    }
}

@Composable
private fun JaImportada(onVer: () -> Unit, onLerOutra: () -> Unit) {
    Column(
        Modifier.fillMaxSize().padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Spacer(Modifier.height(32.dp))
        Box(Modifier.size(88.dp).background(Cores.MarcaClara, CircleShape), contentAlignment = Alignment.Center) {
            Icon(Icons.AutoMirrored.Outlined.ReceiptLong, null, tint = Cores.Marca, modifier = Modifier.size(40.dp))
        }
        Text("Esta nota já foi importada", style = MaterialTheme.typography.headlineSmall, textAlign = TextAlign.Center)
        Text(
            "Cada nota entra uma vez só, para a compra não contar em dobro no seu histórico.",
            style = MaterialTheme.typography.bodyMedium,
            color = Cores.Secundario,
            textAlign = TextAlign.Center,
        )
        BotaoPrincipal("Ver a compra", onVer, Modifier.fillMaxWidth())
        BotaoSecundario("Ler outra nota", onLerOutra, Modifier.fillMaxWidth(), icone = Icons.Outlined.QrCodeScanner)
    }
}

@Composable
private fun TelaCompraConcluida(lista: Lista, onVerCompra: () -> Unit, onInicio: () -> Unit) {
    Column(
        Modifier
            .fillMaxSize()
            .background(Cores.Fundo)
            .padding(24.dp)
            .navigationBarsPadding(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Spacer(Modifier.height(56.dp))
        Box(Modifier.size(96.dp).background(Cores.OkChip, CircleShape), contentAlignment = Alignment.Center) {
            Icon(Icons.Default.Check, null, tint = Cores.OkTexto, modifier = Modifier.size(48.dp))
        }
        Text("Compra concluída!", style = MaterialTheme.typography.headlineSmall)
        Text(
            listOfNotNull(lista.name, data(lista.concluidaEm).ifBlank { null }).joinToString(" · "),
            color = Cores.Secundario,
            textAlign = TextAlign.Center,
        )
        Cartao(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row {
                    Text("Gasto", color = Cores.Secundario, modifier = Modifier.weight(1f))
                    Text(moeda(lista.total), style = MaterialTheme.typography.titleMedium)
                }
                Text(
                    "${lista.items.size} preços reais salvos. Na próxima lista, o app já sabe quanto você costuma pagar.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Cores.Secundario,
                )
            }
        }
        Spacer(Modifier.weight(1f))
        BotaoPrincipal("Ver a compra", onVerCompra, Modifier.fillMaxWidth())
        BotaoSecundario("Voltar ao início", onInicio, Modifier.fillMaxWidth())
    }
}
