package br.com.comprafacil

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Modifier
import br.com.comprafacil.ui.Navegacao
import br.com.comprafacil.ui.theme.CompraFacilTema
import br.com.comprafacil.ui.theme.Cores

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        val container = (application as CompraFacilApp).container
        setContent {
            CompraFacilTema {
                Box(Modifier.fillMaxSize().background(Cores.Fundo)) {
                    Navegacao(container)
                }
            }
        }
    }
}
