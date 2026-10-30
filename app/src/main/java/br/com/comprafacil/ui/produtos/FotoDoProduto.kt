package br.com.comprafacil.ui.produtos

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Inventory2
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import br.com.comprafacil.ui.theme.Cores
import coil.compose.AsyncImage

/** Foto do produto (Open Food Facts) ou um ícone neutro quando não tem foto. */
@Composable
fun FotoDoProduto(url: String?, nome: String, tamanho: Dp = 48.dp) {
    var falhou by remember(url) { mutableStateOf(false) }
    val forma = RoundedCornerShape(10.dp)
    val base = Modifier.size(tamanho).clip(forma).border(1.dp, Cores.Linha, forma).background(Color.White)

    if (!url.isNullOrBlank() && !falhou) {
        AsyncImage(
            model = url,
            contentDescription = nome,
            contentScale = ContentScale.Fit,
            onError = { falhou = true },
            modifier = base.padding(2.dp),
        )
    } else {
        Box(base.background(Cores.Fundo), contentAlignment = Alignment.Center) {
            Icon(Icons.Outlined.Inventory2, null, tint = Cores.Secundario, modifier = Modifier.size(tamanho / 2))
        }
    }
}
