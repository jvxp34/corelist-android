package br.com.comprafacil

import android.app.Application
import br.com.comprafacil.data.Sessao
import br.com.comprafacil.data.api.CompraFacilApi
import br.com.comprafacil.data.api.RefreshRequest
import br.com.comprafacil.data.api.Rede
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

/** Objetos compartilhados pelo app (sem framework de injeção). */
class AppContainer(app: Application) {
    val sessao = Sessao(app).also { it.carregar() }
    val api: CompraFacilApi = Rede(sessao).api
    private val escopo = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    /** Limpa a sessão na hora e avisa o servidor em segundo plano, sem esperar. */
    fun sair() {
        val refresh = sessao.atual()?.refresh
        escopo.launch {
            sessao.limpar()
            if (refresh != null) runCatching { api.sair(RefreshRequest(refresh)) }
        }
    }
}

class CompraFacilApp : Application() {
    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
    }
}
