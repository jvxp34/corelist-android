package br.com.comprafacil.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.List
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Inventory2
import androidx.compose.material.icons.outlined.QrCodeScanner
import androidx.compose.material.icons.outlined.Sell
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavDestination
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import br.com.comprafacil.AppContainer
import br.com.comprafacil.ui.auth.TelaCadastro
import br.com.comprafacil.ui.auth.TelaLogin
import br.com.comprafacil.ui.compra.TelaCompra
import br.com.comprafacil.ui.inicio.TelaInicio
import br.com.comprafacil.ui.listas.TelaListas
import br.com.comprafacil.ui.nota.TelaConferirNota
import br.com.comprafacil.ui.nota.TelaLerNota
import br.com.comprafacil.ui.produtos.TelaMenorPreco
import br.com.comprafacil.ui.produtos.TelaProdutos
import br.com.comprafacil.ui.theme.Cores
import kotlinx.serialization.Serializable

@Serializable object RotaLogin
@Serializable object RotaCadastro
@Serializable object RotaInicio
@Serializable data class RotaListas(val novaLista: Boolean = false)
@Serializable object RotaProdutos
@Serializable object RotaMenorPreco
@Serializable data class RotaCompra(val id: Int)
@Serializable object RotaLerNota
@Serializable data class RotaConferirNota(val link: String)

private fun NavHostController.irParaInicio(rota: Any) = navigate(rota) {
    popUpTo(graph.id) { inclusive = true }
    launchSingleTop = true
}

/** Troca de aba sem empilhar telas (como numa barra de navegação comum). */
private fun NavHostController.irParaAba(rota: Any) = navigate(rota) {
    popUpTo(graph.findStartDestination().id) { saveState = true }
    launchSingleTop = true
    restoreState = true
}

private data class Aba(val rotulo: String, val icone: ImageVector, val rota: Any, val ativa: (NavDestination) -> Boolean)

private val ABAS_ESQUERDA = listOf(
    Aba("Início", Icons.Outlined.Home, RotaInicio) { it.hasRoute<RotaInicio>() },
    Aba("Listas", Icons.AutoMirrored.Outlined.List, RotaListas()) { it.hasRoute<RotaListas>() },
)
private val ABAS_DIREITA = listOf(
    Aba("Produtos", Icons.Outlined.Inventory2, RotaProdutos) { it.hasRoute<RotaProdutos>() },
    Aba("Menor preço", Icons.Outlined.Sell, RotaMenorPreco) { it.hasRoute<RotaMenorPreco>() },
)

@Composable
fun Navegacao(app: AppContainer) {
    val nav = rememberNavController()
    val tokens by app.sessao.tokens.collectAsStateWithLifecycle()
    val logado = tokens != null
    val inicio = remember { if (app.sessao.atual() != null) RotaInicio else RotaLogin }
    val entrada by nav.currentBackStackEntryAsState()
    val destino = entrada?.destination

    // A sessão manda: entrou vai para o início; saiu ou expirou volta ao login.
    LaunchedEffect(logado) {
        val atual = nav.currentBackStackEntry?.destination
        val naEntrada = atual == null || atual.hasRoute<RotaLogin>() || atual.hasRoute<RotaCadastro>()
        if (logado && naEntrada) nav.irParaInicio(RotaInicio)
        if (!logado && !naEntrada) nav.irParaInicio(RotaLogin)
    }

    val mostrarBarra = destino != null && (ABAS_ESQUERDA + ABAS_DIREITA).any { it.ativa(destino) }

    Scaffold(
        containerColor = Cores.Fundo,
        contentWindowInsets = WindowInsets(0),
        bottomBar = {
            if (mostrarBarra && destino != null) {
                BarraInferior(destino, onAba = { nav.irParaAba(it) }, onLerNota = { nav.navigate(RotaLerNota) })
            }
        },
    ) { padding ->
        NavHost(
            navController = nav,
            startDestination = inicio,
            modifier = Modifier.padding(padding).fillMaxSize(),
        ) {
            composable<RotaLogin> {
                TelaLogin(onCriarConta = { nav.navigate(RotaCadastro) { launchSingleTop = true } })
            }
            composable<RotaCadastro> {
                TelaCadastro(onJaTenhoConta = { if (!nav.popBackStack()) nav.irParaInicio(RotaLogin) })
            }
            composable<RotaInicio> {
                TelaInicio(
                    onAbrirLista = { nav.navigate(RotaCompra(it)) },
                    onNovaLista = { nav.irParaAba(RotaListas(novaLista = true)) },
                    onLerNota = { nav.navigate(RotaLerNota) },
                    onVerListas = { nav.irParaAba(RotaListas()) },
                    onSair = app::sair,
                )
            }
            composable<RotaListas> { e ->
                TelaListas(
                    onAbrirLista = { nav.navigate(RotaCompra(it)) },
                    onLerNota = { nav.navigate(RotaLerNota) },
                    abrirNova = e.toRoute<RotaListas>().novaLista,
                )
            }
            composable<RotaProdutos> { TelaProdutos() }
            composable<RotaMenorPreco> { TelaMenorPreco() }
            composable<RotaCompra> { e ->
                TelaCompra(listaId = e.toRoute<RotaCompra>().id, onVoltar = { nav.popBackStack() })
            }
            composable<RotaLerNota> {
                TelaLerNota(
                    onVoltar = { nav.popBackStack() },
                    onLinkLido = { link ->
                        nav.navigate(RotaConferirNota(link)) { popUpTo<RotaLerNota> { inclusive = true } }
                    },
                )
            }
            composable<RotaConferirNota> { e ->
                TelaConferirNota(
                    link = e.toRoute<RotaConferirNota>().link,
                    onVoltar = { nav.popBackStack() },
                    onLerOutra = { nav.navigate(RotaLerNota) { popUpTo<RotaConferirNota> { inclusive = true } } },
                    onAbrirLista = { id ->
                        nav.navigate(RotaCompra(id)) { popUpTo<RotaConferirNota> { inclusive = true } }
                    },
                    onInicio = { nav.irParaAba(RotaInicio) },
                )
            }
        }
    }
}

@Composable
private fun BarraInferior(destino: NavDestination, onAba: (Any) -> Unit, onLerNota: () -> Unit) {
    Surface(color = Color.White) {
        Column(Modifier.navigationBarsPadding()) {
            HorizontalDivider(color = Cores.Linha)
            Row(
                Modifier.fillMaxWidth().height(64.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                ABAS_ESQUERDA.forEach { ItemBarra(it, it.ativa(destino), onAba, Modifier.weight(1f)) }
                Box(Modifier.weight(1f), contentAlignment = Alignment.Center) {
                    Box(
                        Modifier
                            .offset(y = (-14).dp)
                            .size(56.dp)
                            .shadow(10.dp, CircleShape, ambientColor = Cores.Marca, spotColor = Cores.Marca)
                            .background(Cores.Marca, CircleShape)
                            .clickable(role = Role.Button, onClickLabel = "Ler nota fiscal", onClick = onLerNota),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(Icons.Outlined.QrCodeScanner, contentDescription = "Ler nota fiscal", tint = Color.White)
                    }
                }
                ABAS_DIREITA.forEach { ItemBarra(it, it.ativa(destino), onAba, Modifier.weight(1f)) }
            }
        }
    }
}

@Composable
private fun ItemBarra(aba: Aba, ativa: Boolean, onAba: (Any) -> Unit, modifier: Modifier) {
    val cor = if (ativa) Cores.MarcaEscura else Cores.Secundario
    Column(
        modifier
            .height(64.dp)
            .clickable(role = Role.Tab) { onAba(aba.rota) },
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Icon(aba.icone, contentDescription = null, tint = cor, modifier = Modifier.size(22.dp))
        Text(
            aba.rotulo,
            color = cor,
            fontSize = 11.sp,
            fontWeight = if (ativa) FontWeight.SemiBold else FontWeight.Normal,
            style = MaterialTheme.typography.labelSmall,
            maxLines = 1,
            modifier = Modifier.padding(top = 4.dp),
        )
    }
}
