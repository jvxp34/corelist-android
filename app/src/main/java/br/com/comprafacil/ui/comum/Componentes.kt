package br.com.comprafacil.ui.comum

import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.outlined.ErrorOutline
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import br.com.comprafacil.AppContainer
import br.com.comprafacil.CompraFacilApp
import br.com.comprafacil.R
import br.com.comprafacil.ui.theme.Cores
import java.math.BigDecimal

/** ViewModel com acesso aos objetos do app. */
@Composable
inline fun <reified VM : ViewModel> appViewModel(
    key: String? = null,
    crossinline criar: (AppContainer) -> VM,
): VM {
    val app = LocalContext.current.applicationContext as CompraFacilApp
    return viewModel(key = key, factory = viewModelFactory { initializer { criar(app.container) } })
}

@Composable
fun Logo(modifier: Modifier = Modifier, tamanho: Dp = 40.dp) {
    Row(modifier, verticalAlignment = Alignment.CenterVertically) {
        Box(
            Modifier
                .size(tamanho)
                .background(Cores.Marca, RoundedCornerShape(tamanho / 3.5f)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                painterResource(R.drawable.ic_logo),
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(tamanho * 0.55f),
            )
        }
        Spacer(Modifier.width(10.dp))
        Text(
            "CompraFácil",
            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
            color = Cores.Texto,
        )
    }
}

@Composable
fun BotaoPrincipal(
    texto: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    carregando: Boolean = false,
    habilitado: Boolean = true,
    icone: ImageVector? = null,
    cor: Color = Cores.Marca,
) {
    Button(
        onClick = onClick,
        enabled = habilitado && !carregando,
        modifier = modifier.height(52.dp),
        shape = MaterialTheme.shapes.medium,
        colors = ButtonDefaults.buttonColors(containerColor = cor),
    ) {
        if (carregando) {
            CircularProgressIndicator(Modifier.size(20.dp), color = Color.White, strokeWidth = 2.dp)
        } else {
            if (icone != null) {
                Icon(icone, contentDescription = null, Modifier.size(20.dp))
                Spacer(Modifier.width(8.dp))
            }
            Text(texto, style = MaterialTheme.typography.labelLarge)
        }
    }
}

@Composable
fun BotaoSecundario(
    texto: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icone: ImageVector? = null,
    habilitado: Boolean = true,
) {
    OutlinedButton(
        onClick = onClick,
        enabled = habilitado,
        modifier = modifier.height(52.dp),
        shape = MaterialTheme.shapes.medium,
        border = BorderStroke(1.dp, Cores.Linha),
    ) {
        if (icone != null) {
            Icon(icone, contentDescription = null, Modifier.size(20.dp))
            Spacer(Modifier.width(8.dp))
        }
        Text(texto, style = MaterialTheme.typography.labelLarge)
    }
}

/** Cartão branco com borda fina, como os do site. */
@Composable
fun Cartao(
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    conteudo: @Composable () -> Unit,
) {
    val cores = CardDefaults.cardColors(containerColor = Color.White)
    val borda = BorderStroke(1.dp, Cores.Linha)
    if (onClick != null) {
        Card(onClick = onClick, modifier = modifier, shape = MaterialTheme.shapes.large, colors = cores, border = borda) {
            conteudo()
        }
    } else {
        Card(modifier = modifier, shape = MaterialTheme.shapes.large, colors = cores, border = borda) {
            conteudo()
        }
    }
}

@Composable
fun ChipOrcamento(situacao: SituacaoOrcamento, modifier: Modifier = Modifier) {
    Surface(modifier, color = situacao.corChip, shape = RoundedCornerShape(50)) {
        Text(
            situacao.resumo,
            color = situacao.corChipTexto,
            style = MaterialTheme.typography.labelMedium,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
        )
    }
}

@Composable
fun AvisoErro(texto: String, modifier: Modifier = Modifier) {
    Surface(modifier.fillMaxWidth(), color = Cores.EstourouChip, shape = MaterialTheme.shapes.medium) {
        Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Outlined.ErrorOutline, contentDescription = null, tint = Cores.EstourouTexto)
            Spacer(Modifier.width(10.dp))
            Text(texto, color = Cores.EstourouChipTexto, style = MaterialTheme.typography.bodyMedium)
        }
    }
}

@Composable
fun Carregando(modifier: Modifier = Modifier) {
    Box(modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        CircularProgressIndicator(color = Cores.Marca)
    }
}

@Composable
fun EstadoVazio(
    icone: ImageVector,
    titulo: String,
    texto: String,
    modifier: Modifier = Modifier,
    acao: (@Composable () -> Unit)? = null,
) {
    Column(
        modifier.fillMaxWidth().padding(horizontal = 32.dp, vertical = 48.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(
            Modifier.size(64.dp).background(Cores.MarcaClara, RoundedCornerShape(20.dp)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(icone, contentDescription = null, tint = Cores.Marca, modifier = Modifier.size(30.dp))
        }
        Spacer(Modifier.height(16.dp))
        Text(titulo, style = MaterialTheme.typography.titleMedium, textAlign = TextAlign.Center)
        Spacer(Modifier.height(6.dp))
        Text(
            texto,
            style = MaterialTheme.typography.bodyMedium,
            color = Cores.Secundario,
            textAlign = TextAlign.Center,
        )
        if (acao != null) {
            Spacer(Modifier.height(20.dp))
            acao()
        }
    }
}

@Composable
fun ErroComTentarDeNovo(mensagem: String, onTentar: () -> Unit, modifier: Modifier = Modifier) {
    EstadoVazio(
        icone = Icons.Outlined.ErrorOutline,
        titulo = "Não deu certo",
        texto = mensagem,
        modifier = modifier,
        acao = { BotaoSecundario("Tentar de novo", onTentar) },
    )
}

@Composable
fun CampoPreco(
    valor: String,
    onChange: (String) -> Unit,
    rotulo: String,
    modifier: Modifier = Modifier,
) {
    OutlinedTextField(
        value = valor,
        onValueChange = { novo -> onChange(novo.filter { it.isDigit() || it == ',' || it == '.' }) },
        label = { Text(rotulo) },
        prefix = { Text("R$ ") },
        singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
        shape = MaterialTheme.shapes.medium,
        modifier = modifier.fillMaxWidth(),
    )
}

/**
 * Quantidade com botões − e +. Por unidade vai de 1 em 1 (mínimo 1);
 * por peso, de 0,1 kg em 0,1 kg (mínimo 0,1). O campo também aceita digitar.
 */
@Composable
fun CampoQuantidade(
    valor: BigDecimal,
    porKg: Boolean,
    onChange: (BigDecimal) -> Unit,
    modifier: Modifier = Modifier,
) {
    val passo = if (porKg) BigDecimal("0.1") else BigDecimal.ONE
    val minimo = passo
    var texto by remember { mutableStateOf(quantidadeParaCampo(valor)) }

    // Mantém o texto em dia quando o valor muda por fora (botões, troca de medida).
    LaunchedEffect(valor) {
        if (lerDecimal(texto)?.compareTo(valor) != 0) texto = quantidadeParaCampo(valor)
    }

    Row(modifier, verticalAlignment = Alignment.CenterVertically) {
        FilledTonalIconButton(
            onClick = { onChange((valor - passo).max(minimo)) },
            enabled = valor > minimo,
            modifier = Modifier.size(48.dp),
        ) { Icon(Icons.Default.Remove, contentDescription = "Diminuir") }

        OutlinedTextField(
            value = texto,
            onValueChange = { novo ->
                val filtrado = novo.filter { it.isDigit() || it == ',' || it == '.' }
                    .let { if (porKg) it else it.filter(Char::isDigit) }
                texto = filtrado
                lerDecimal(filtrado)?.takeIf { it.signum() > 0 }?.let(onChange)
            },
            singleLine = true,
            suffix = { Text(if (porKg) "kg" else "un") },
            keyboardOptions = KeyboardOptions(
                keyboardType = if (porKg) KeyboardType.Decimal else KeyboardType.Number,
            ),
            textStyle = MaterialTheme.typography.titleMedium.copy(textAlign = TextAlign.Center),
            shape = MaterialTheme.shapes.medium,
            modifier = Modifier.weight(1f).padding(horizontal = 10.dp),
        )

        FilledTonalIconButton(
            onClick = { onChange(valor + passo) },
            modifier = Modifier.size(48.dp),
        ) { Icon(Icons.Default.Add, contentDescription = "Aumentar") }
    }
}

val PaddingTela = PaddingValues(horizontal = 16.dp, vertical = 12.dp)

val EspacoLista = Arrangement.spacedBy(10.dp)
