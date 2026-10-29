package br.com.comprafacil.ui.nota

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.outlined.ContentPaste
import androidx.compose.material.icons.outlined.FlashlightOff
import androidx.compose.material.icons.outlined.FlashlightOn
import androidx.compose.material.icons.outlined.NoPhotography
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import br.com.comprafacil.ui.comum.BotaoPrincipal
import kotlinx.coroutines.delay

private val FundoEscuro = Color(0xFF0F1020)
private val CaixaEscura = Color(0xFF1B1D33)
private val Moldura = Color(0xFFA5B4FC)
private val TextoClaro = Color(0xFFC7C9D3)

/** O QR code impresso traz a página pública da nota na Sefaz-AM. */
fun ehLinkDeNota(texto: String): Boolean {
    val t = texto.trim().lowercase()
    return (t.startsWith("http://") || t.startsWith("https://")) &&
        t.contains("sefaz.am.gov.br") && t.contains("nfce")
}

@Composable
fun TelaLerNota(onVoltar: () -> Unit, onLinkLido: (String) -> Unit) {
    val context = LocalContext.current
    var temPermissao by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED,
        )
    }
    var pediu by rememberSaveable { mutableStateOf(false) }
    val pedir = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {
        temPermissao = it
        pediu = true
    }
    LaunchedEffect(Unit) { if (!temPermissao && !pediu) pedir.launch(Manifest.permission.CAMERA) }

    var lanterna by remember { mutableStateOf(false) }
    var aviso by remember { mutableStateOf<String?>(null) }
    var colando by rememberSaveable { mutableStateOf(false) }
    var link by rememberSaveable { mutableStateOf("") }
    var jaLeu by remember { mutableStateOf(false) }

    // O aviso de "não é nota" some sozinho.
    LaunchedEffect(aviso) {
        if (aviso != null) {
            delay(3500)
            aviso = null
        }
    }

    Column(
        Modifier
            .fillMaxSize()
            .background(FundoEscuro)
            .safeDrawingPadding()
            .imePadding(),
    ) {
        Row(
            Modifier.fillMaxWidth().padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            BotaoRedondo(onVoltar, "Voltar", Icons.AutoMirrored.Filled.ArrowBack)
            Text(
                "Ler nota fiscal",
                style = MaterialTheme.typography.titleMedium,
                color = Color.White,
                textAlign = TextAlign.Center,
                modifier = Modifier.weight(1f),
            )
            BotaoRedondo(
                onClick = { lanterna = !lanterna },
                descricao = if (lanterna) "Desligar lanterna" else "Ligar lanterna",
                icone = if (lanterna) Icons.Outlined.FlashlightOff else Icons.Outlined.FlashlightOn,
                habilitado = temPermissao,
            )
        }

        Box(Modifier.weight(1f).fillMaxWidth().padding(24.dp), contentAlignment = Alignment.Center) {
            val forma = RoundedCornerShape(24.dp)
            Box(
                Modifier
                    .fillMaxWidth()
                    .aspectRatio(1f)
                    .clip(forma)
                    .background(CaixaEscura)
                    .border(BorderStroke(4.dp, Moldura), forma),
                contentAlignment = Alignment.Center,
            ) {
                if (temPermissao) {
                    LeitorCamera(
                        formatos = FORMATO_QR,
                        lanterna = lanterna,
                        onSemLanterna = {
                            lanterna = false
                            aviso = "Este aparelho não tem lanterna."
                        },
                        onCodigo = { valor ->
                            if (jaLeu) return@LeitorCamera
                            if (ehLinkDeNota(valor)) {
                                jaLeu = true
                                onLinkLido(valor.trim())
                            } else {
                                aviso = "Esse QR code não é de uma nota fiscal do Amazonas."
                            }
                        },
                        modifier = Modifier.fillMaxSize(),
                    )
                } else {
                    Column(
                        Modifier.padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        Icon(Icons.Outlined.NoPhotography, null, tint = Moldura, modifier = Modifier.size(40.dp))
                        Text(
                            "Para ler o QR code, o app precisa usar a câmera.",
                            color = Color.White,
                            textAlign = TextAlign.Center,
                        )
                        OutlinedButton(
                            onClick = { pedir.launch(Manifest.permission.CAMERA) },
                            border = BorderStroke(1.dp, Color.White.copy(alpha = 0.4f)),
                        ) { Text("Permitir câmera", color = Color.White) }
                        if (pediu) {
                            Text(
                                "Se não aparecer o pedido, libere a câmera nas configurações do Android.",
                                color = TextoClaro,
                                style = MaterialTheme.typography.bodySmall,
                                textAlign = TextAlign.Center,
                            )
                        }
                    }
                }
            }
        }

        Column(
            Modifier.fillMaxWidth().padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            val atencao = aviso
            Text(
                atencao ?: "Aponte para o QR code da nota",
                style = MaterialTheme.typography.titleMedium,
                color = if (atencao != null) Color(0xFFFCD34D) else Color.White,
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(6.dp))
            Text(
                "Ele fica no rodapé do cupom fiscal. A leitura é automática.",
                style = MaterialTheme.typography.bodyMedium,
                color = TextoClaro,
                textAlign = TextAlign.Center,
            )
        }

        Column(
            Modifier.fillMaxWidth().padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            if (colando) {
                val clipboard = LocalClipboardManager.current
                val ler = { if (link.isNotBlank()) onLinkLido(link.trim()) }
                OutlinedTextField(
                    value = link,
                    onValueChange = { link = it },
                    placeholder = { Text("Cole o link da nota") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri, imeAction = ImeAction.Go),
                    keyboardActions = KeyboardActions(onGo = { ler() }),
                    trailingIcon = {
                        IconButton(onClick = { clipboard.getText()?.text?.let { link = it } }) {
                            Icon(Icons.Outlined.ContentPaste, "Colar", tint = Color.White)
                        }
                    },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        focusedBorderColor = Moldura,
                        unfocusedBorderColor = Color.White.copy(alpha = 0.3f),
                        focusedPlaceholderColor = TextoClaro,
                        unfocusedPlaceholderColor = TextoClaro,
                        cursorColor = Color.White,
                    ),
                    shape = MaterialTheme.shapes.medium,
                    modifier = Modifier.fillMaxWidth(),
                )
                BotaoPrincipal("Ler esta nota", ler, Modifier.fillMaxWidth(), habilitado = link.isNotBlank())
            } else {
                OutlinedButton(
                    onClick = { colando = true },
                    border = BorderStroke(1.dp, Color.White.copy(alpha = 0.3f)),
                    shape = MaterialTheme.shapes.medium,
                    modifier = Modifier.fillMaxWidth().height(50.dp),
                ) {
                    Icon(Icons.Outlined.ContentPaste, null, tint = Color.White)
                    Spacer(Modifier.size(8.dp))
                    Text("Colar o link da nota", color = Color.White)
                }
            }
        }
    }
}

@Composable
private fun BotaoRedondo(
    onClick: () -> Unit,
    descricao: String,
    icone: ImageVector,
    habilitado: Boolean = true,
) {
    IconButton(
        onClick = onClick,
        enabled = habilitado,
        modifier = Modifier.size(44.dp),
        colors = IconButtonDefaults.iconButtonColors(
            containerColor = Color.White.copy(alpha = 0.12f),
            contentColor = Color.White,
            disabledContainerColor = Color.White.copy(alpha = 0.05f),
            disabledContentColor = Color.White.copy(alpha = 0.3f),
        ),
    ) { Icon(icone, contentDescription = descricao) }
}
