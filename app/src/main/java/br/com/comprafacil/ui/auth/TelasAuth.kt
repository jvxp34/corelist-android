package br.com.comprafacil.ui.auth

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Visibility
import androidx.compose.material.icons.outlined.VisibilityOff
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import br.com.comprafacil.ui.comum.AvisoErro
import br.com.comprafacil.ui.comum.BotaoPrincipal
import br.com.comprafacil.ui.comum.Cartao
import br.com.comprafacil.ui.comum.Logo
import br.com.comprafacil.ui.comum.appViewModel
import br.com.comprafacil.ui.theme.Cores

@Composable
fun TelaLogin(onCriarConta: () -> Unit) {
    val vm = appViewModel { AuthViewModel(it) }
    val expirou by vm.expirou.collectAsStateWithLifecycle()

    LayoutAuth(
        titulo = "Entrar",
        subtitulo = "Suas compras organizadas. Seu orçamento sob controle.",
    ) {
        if (expirou) {
            Surface(color = Cores.MarcaClara, shape = MaterialTheme.shapes.medium) {
                Row(Modifier.fillMaxWidth().padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Outlined.Info, contentDescription = null, tint = Cores.Marca)
                    Text(
                        "Sua sessão expirou. Entre de novo.",
                        color = Cores.MarcaEscura,
                        modifier = Modifier.padding(start = 10.dp),
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }
            }
        }
        CampoEmail(vm.email) { vm.email = it }
        CampoSenha(vm.senha, { vm.senha = it }, onConcluir = vm::entrar)
        vm.erro?.let { AvisoErro(it) }
        BotaoPrincipal("Entrar", vm::entrar, Modifier.fillMaxWidth(), carregando = vm.carregando)
        TextButton(
            onClick = {
                vm.avisoVisto()
                onCriarConta()
            },
            modifier = Modifier.align(Alignment.CenterHorizontally),
        ) { Text("Não tem conta? Criar conta") }
    }
}

@Composable
fun TelaCadastro(onJaTenhoConta: () -> Unit) {
    val vm = appViewModel { AuthViewModel(it) }

    LayoutAuth(titulo = "Criar conta", subtitulo = "Leva menos de um minuto.") {
        OutlinedTextField(
            value = vm.nome,
            onValueChange = { vm.nome = it },
            label = { Text("Seu nome") },
            singleLine = true,
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
            shape = MaterialTheme.shapes.medium,
            modifier = Modifier.fillMaxWidth(),
        )
        CampoEmail(vm.email) { vm.email = it }
        CampoSenha(vm.senha, { vm.senha = it }, onConcluir = vm::cadastrar, ajuda = "Pelo menos 8 caracteres.")
        vm.erro?.let { AvisoErro(it) }
        BotaoPrincipal("Criar conta", vm::cadastrar, Modifier.fillMaxWidth(), carregando = vm.carregando)
        TextButton(onClick = onJaTenhoConta, modifier = Modifier.align(Alignment.CenterHorizontally)) {
            Text("Já tenho conta. Entrar")
        }
    }
}

@Composable
private fun LayoutAuth(
    titulo: String,
    subtitulo: String,
    conteudo: @Composable androidx.compose.foundation.layout.ColumnScope.() -> Unit,
) {
    Column(
        Modifier
            .fillMaxSize()
            .safeDrawingPadding()
            .imePadding()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Spacer(Modifier.height(24.dp))
        Logo(tamanho = 44.dp)
        Spacer(Modifier.height(28.dp))
        Cartao(Modifier.widthIn(max = 460.dp).fillMaxWidth()) {
            Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                Text(titulo, style = MaterialTheme.typography.headlineSmall)
                Text(subtitulo, style = MaterialTheme.typography.bodyMedium, color = Cores.Secundario)
                conteudo()
            }
        }
    }
}

@Composable
private fun CampoEmail(valor: String, onChange: (String) -> Unit) {
    OutlinedTextField(
        value = valor,
        onValueChange = onChange,
        label = { Text("E-mail") },
        singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email, imeAction = ImeAction.Next),
        shape = MaterialTheme.shapes.medium,
        modifier = Modifier.fillMaxWidth(),
    )
}

@Composable
private fun CampoSenha(
    valor: String,
    onChange: (String) -> Unit,
    onConcluir: () -> Unit,
    ajuda: String? = null,
) {
    var visivel by remember { mutableStateOf(false) }
    OutlinedTextField(
        value = valor,
        onValueChange = onChange,
        label = { Text("Senha") },
        singleLine = true,
        supportingText = ajuda?.let { { Text(it) } },
        visualTransformation = if (visivel) VisualTransformation.None else PasswordVisualTransformation(),
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password, imeAction = ImeAction.Done),
        keyboardActions = KeyboardActions(onDone = { onConcluir() }),
        trailingIcon = {
            IconButton(onClick = { visivel = !visivel }) {
                Icon(
                    if (visivel) Icons.Outlined.VisibilityOff else Icons.Outlined.Visibility,
                    contentDescription = if (visivel) "Esconder senha" else "Mostrar senha",
                )
            }
        },
        shape = MaterialTheme.shapes.medium,
        modifier = Modifier.fillMaxWidth(),
    )
}
