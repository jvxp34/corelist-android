package br.com.comprafacil.ui.inicio

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Logout
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.outlined.QrCodeScanner
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.viewModelScope
import br.com.comprafacil.AppContainer
import br.com.comprafacil.data.api.Lista
import br.com.comprafacil.data.api.Resultado
import br.com.comprafacil.data.api.Usuario
import br.com.comprafacil.data.api.chamar
import br.com.comprafacil.ui.comum.BotaoPrincipal
import br.com.comprafacil.ui.comum.Cartao
import br.com.comprafacil.ui.comum.Logo
import br.com.comprafacil.ui.comum.NivelOrcamento
import br.com.comprafacil.ui.comum.PT_BR
import br.com.comprafacil.ui.comum.appViewModel
import br.com.comprafacil.ui.comum.diaMes
import br.com.comprafacil.ui.comum.moeda
import br.com.comprafacil.ui.comum.situacaoDoOrcamento
import br.com.comprafacil.ui.theme.Cores
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import kotlinx.coroutines.async
import kotlinx.coroutines.launch

class InicioViewModel(private val app: AppContainer) : ViewModel() {
    var usuario by mutableStateOf<Usuario?>(null)
        private set
    var listas by mutableStateOf<List<Lista>?>(null)
        private set
    var carregando by mutableStateOf(false)
        private set
    var erro by mutableStateOf<String?>(null)
        private set

    /** A lista em andamento mais recente. */
    val emAndamento get() = listas.orEmpty().filter { !it.concluida }.maxByOrNull { it.id }

    val ultimas
        get() = listas.orEmpty().filter { it.concluida }
            .sortedWith(compareByDescending<Lista> { it.concluidaEm ?: "" }.thenByDescending { it.id })
            .take(3)

    fun carregar() {
        if (carregando) return
        viewModelScope.launch {
            carregando = true
            val eu = async { chamar { app.api.eu() } }
            when (val r = chamar { app.api.listas() }) {
                is Resultado.Ok -> {
                    listas = r.valor
                    erro = null
                }
                is Resultado.Falha -> erro = r.mensagem
            }
            (eu.await() as? Resultado.Ok)?.let { usuario = it.valor }
            carregando = false
        }
    }
}

@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
@Composable
fun TelaInicio(
    onAbrirLista: (Int) -> Unit,
    onNovaLista: () -> Unit,
    onLerNota: () -> Unit,
    onVerListas: () -> Unit,
    onSair: () -> Unit,
) {
    val vm = appViewModel { InicioViewModel(it) }
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) { vm.carregar() }
    var menu by remember { mutableStateOf(false) }
    var confirmarSaida by remember { mutableStateOf(false) }

    PullToRefreshBox(isRefreshing = vm.carregando && vm.listas != null, onRefresh = vm::carregar) {
        Column(
            Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .statusBarsPadding()
                .padding(horizontal = 20.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Logo(Modifier.weight(1f), tamanho = 36.dp)
                Box {
                    val inicial = vm.usuario?.nome?.trim()?.firstOrNull()?.uppercaseChar()?.toString() ?: "?"
                    Box(
                        Modifier
                            .size(40.dp)
                            .background(Cores.MarcaClara, CircleShape)
                            .clickable { menu = true }
                            .semantics { contentDescription = "Minha conta" },
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(inicial, color = Cores.MarcaEscura, style = MaterialTheme.typography.titleSmall)
                    }
                    DropdownMenu(expanded = menu, onDismissRequest = { menu = false }) {
                        vm.usuario?.let {
                            Text(
                                it.email,
                                style = MaterialTheme.typography.bodySmall,
                                color = Cores.Secundario,
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                            )
                        }
                        DropdownMenuItem(
                            text = { Text("Sair") },
                            leadingIcon = { Icon(Icons.AutoMirrored.Outlined.Logout, null) },
                            onClick = {
                                menu = false
                                confirmarSaida = true
                            },
                        )
                    }
                }
            }

            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                val hoje = LocalDate.now().format(DateTimeFormatter.ofPattern("EEEE, d 'de' MMMM", PT_BR))
                Text(hoje.replaceFirstChar { it.titlecase(PT_BR) }, style = MaterialTheme.typography.bodySmall, color = Cores.Secundario)
                val nome = vm.usuario?.nome?.trim()?.split(" ")?.firstOrNull()?.takeIf { it.isNotBlank() }
                Text(
                    if (nome != null) "Olá, $nome! O que vamos comprar hoje?" else "O que vamos comprar hoje?",
                    style = MaterialTheme.typography.headlineSmall,
                )
            }

            vm.erro?.takeIf { vm.listas == null }?.let {
                br.com.comprafacil.ui.comum.ErroComTentarDeNovo(it, vm::carregar)
            }

            vm.emAndamento?.let { CartaoEmAndamento(it, onContinuar = { onAbrirLista(it.id) }) }

            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Atalho("Nova lista", Icons.Default.Add, onNovaLista, Modifier.weight(1f))
                Atalho("Ler nota fiscal", Icons.Outlined.QrCodeScanner, onLerNota, Modifier.weight(1f))
            }

            if (vm.ultimas.isNotEmpty()) {
                Cartao(Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(horizontal = 18.dp, vertical = 12.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("Últimas compras", style = MaterialTheme.typography.titleSmall, modifier = Modifier.weight(1f))
                            TextButton(onClick = onVerListas) { Text("Ver todas") }
                        }
                        vm.ultimas.forEach { lista ->
                            HorizontalDivider(color = Cores.Linha)
                            val situacao = situacaoDoOrcamento(lista.total, lista.budget)
                            Row(
                                Modifier.fillMaxWidth().clickable { onAbrirLista(lista.id) }.padding(vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Text(
                                    buildString {
                                        append(lista.name)
                                        diaMes(lista.concluidaEm).takeIf { it.isNotBlank() }?.let { append(" · $it") }
                                    },
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    style = MaterialTheme.typography.bodyMedium,
                                    modifier = Modifier.weight(1f),
                                )
                                if (situacao.nivel == NivelOrcamento.ESTOUROU) {
                                    Text("passou · ", color = Cores.EstourouTexto, style = MaterialTheme.typography.bodySmall)
                                }
                                Text(moeda(lista.total), style = MaterialTheme.typography.titleSmall)
                            }
                        }
                    }
                }
            }
            Spacer(Modifier.height(8.dp))
        }
    }

    if (confirmarSaida) {
        AlertDialog(
            onDismissRequest = { confirmarSaida = false },
            title = { Text("Sair da conta?") },
            text = { Text("Você vai precisar entrar de novo para ver suas listas.") },
            confirmButton = {
                TextButton(onClick = {
                    confirmarSaida = false
                    onSair()
                }) { Text("Sair") }
            },
            dismissButton = { TextButton(onClick = { confirmarSaida = false }) { Text("Cancelar") } },
        )
    }
}

@Composable
private fun CartaoEmAndamento(lista: Lista, onContinuar: () -> Unit) {
    val situacao = situacaoDoOrcamento(lista.total, lista.budget)
    val comprados = lista.items.count { it.comprado }
    Cartao(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text("Compra em andamento", style = MaterialTheme.typography.labelMedium, color = Cores.MarcaEscura)
            Text(lista.name, style = MaterialTheme.typography.titleMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Row(verticalAlignment = Alignment.Bottom) {
                Text(moeda(lista.total), style = MaterialTheme.typography.headlineSmall, modifier = Modifier.weight(1f))
                if (situacao.nivel != NivelOrcamento.SEM) {
                    Text("de ${moeda(situacao.orcamento)}", style = MaterialTheme.typography.bodySmall, color = Cores.Secundario)
                }
            }
            if (situacao.nivel != NivelOrcamento.SEM) {
                LinearProgressIndicator(
                    progress = { situacao.usado },
                    color = situacao.corBarra,
                    trackColor = Cores.Linha,
                    drawStopIndicator = {},
                    modifier = Modifier.fillMaxWidth().height(10.dp),
                )
            }
            Row {
                Text(
                    if (situacao.nivel == NivelOrcamento.SEM) "Sem orçamento" else situacao.resumo,
                    color = situacao.corTexto,
                    style = MaterialTheme.typography.labelLarge,
                )
                Text(
                    " · $comprados de ${lista.items.size} no carrinho",
                    color = Cores.Secundario,
                    style = MaterialTheme.typography.bodySmall,
                )
            }
            BotaoPrincipal("Continuar compra", onContinuar, Modifier.fillMaxWidth())
        }
    }
}

@Composable
private fun Atalho(texto: String, icone: ImageVector, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Cartao(modifier, onClick = onClick) {
        Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Icon(icone, null, tint = Cores.MarcaEscura)
            Text(texto, style = MaterialTheme.typography.titleSmall)
        }
    }
}
