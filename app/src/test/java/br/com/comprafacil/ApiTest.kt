package br.com.comprafacil

import br.com.comprafacil.data.Tokens
import br.com.comprafacil.data.api.GuardaTokens
import br.com.comprafacil.data.api.ImportarNota
import br.com.comprafacil.data.api.ItemImportar
import br.com.comprafacil.data.api.JsonApi
import br.com.comprafacil.data.api.Lista
import br.com.comprafacil.data.api.NotaLida
import br.com.comprafacil.data.api.NovoItem
import br.com.comprafacil.data.api.Produto
import br.com.comprafacil.data.api.Rede
import br.com.comprafacil.data.api.Resultado
import br.com.comprafacil.data.api.chamar
import br.com.comprafacil.ui.comum.NivelOrcamento
import br.com.comprafacil.ui.comum.lerDecimal
import br.com.comprafacil.ui.comum.moeda
import br.com.comprafacil.ui.comum.situacaoDoOrcamento
import br.com.comprafacil.ui.nota.ehLinkDeNota
import br.com.comprafacil.ui.produtos.precosPagos
import java.math.BigDecimal
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import okhttp3.mockwebserver.Dispatcher
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import okhttp3.mockwebserver.RecordedRequest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

private fun recurso(nome: String) =
    ApiTest::class.java.classLoader!!.getResource(nome)!!.readText(Charsets.UTF_8)

private class TokensEmMemoria(var tokens: Tokens?) : GuardaTokens {
    var expirou = false
    override fun atual() = tokens
    override fun salvarBloqueando(tokens: Tokens) {
        this.tokens = tokens
    }
    override fun limparBloqueando(expirou: Boolean) {
        tokens = null
        this.expirou = expirou
    }
}

class ApiTest {

    // ---- Respostas reais do backend local (conta demo) ----

    @Test
    fun leListasDoBackendComTotaisIguaisAosDoServidor() {
        val texto = recurso("listas.json")
        val listas = JsonApi.decodeFromString<List<Lista>>(texto)
        val totaisServidor = JsonApi.parseToJsonElement(texto).jsonArray
            .map { BigDecimal(it.jsonObject["total"]!!.toString().trim('"')) }

        assertTrue(listas.isNotEmpty())
        listas.zip(totaisServidor).forEach { (lista, total) ->
            assertEquals("total de ${lista.name}", 0, lista.total.compareTo(total))
        }
        assertTrue(listas.any { it.concluida } && listas.any { !it.concluida })
    }

    @Test
    fun leProdutosDoBackend() {
        val produtos = JsonApi.decodeFromString<List<Produto>>(recurso("produtos.json"))
        assertTrue(produtos.all { it.name.isNotBlank() })
    }

    @Test
    fun menorPrecoUsaSoComprasConcluidas() {
        val listas = JsonApi.decodeFromString<List<Lista>>(recurso("listas.json"))
        val produtos = JsonApi.decodeFromString<List<Produto>>(recurso("produtos.json")).associateBy { it.id }
        val precos = precosPagos(listas, produtos)
        assertTrue(precos.isNotEmpty())
        precos.forEach { assertTrue(it.menor <= it.media && it.media > BigDecimal.ZERO) }
    }

    // ---- Envio ----

    @Test
    fun decimaisVaoComoTextoESemCamposNulos() {
        val json = JsonApi.encodeToString(NovoItem.serializer(), NovoItem(3, BigDecimal("0.8"), "kg", null))
        assertEquals("""{"product":3,"quantity":"0.8","measure":"kg"}""", json)

        val nota = ImportarNota("http://x", "Compra", listOf(ItemImportar("ARROZ", BigDecimal("1.00"), "unit", BigDecimal("21.90"))))
        assertTrue(JsonApi.encodeToString(ImportarNota.serializer(), nota).contains(""""preco":"21.90""""))
    }

    @Test
    fun leRespostaDaLeituraDaNota() {
        val texto = """
            {"chave":"1326","loja":"SUPERMERCADO DB LTDA","cnpj":"04","emitida_em":"2026-10-20T18:31:00-04:00",
             "total":"86.23","lista_importada":null,
             "itens":[{"nome":"CARNE MOIDA","codigo_loja":"77","unidade":"KG","medida":"kg","quantidade":"0.850",
                       "preco":"22.90","total":"19.47","produto":null}]}
        """.trimIndent()
        val nota = JsonApi.decodeFromString<NotaLida>(texto)
        assertEquals("kg", nota.itens.single().medida)
        assertEquals(0, BigDecimal("0.850").compareTo(nota.itens.single().quantidade))
        assertNull(nota.listaImportada)
    }

    // ---- Regras da tela ----

    @Test
    fun situacaoDoOrcamentoIgualAoSite() {
        assertEquals(NivelOrcamento.SEM, situacaoDoOrcamento(BigDecimal.TEN, null).nivel)
        assertEquals(NivelOrcamento.OK, situacaoDoOrcamento(BigDecimal("50"), BigDecimal("100")).nivel)
        assertEquals(NivelOrcamento.PERTO, situacaoDoOrcamento(BigDecimal("90"), BigDecimal("100")).nivel)
        val estourou = situacaoDoOrcamento(BigDecimal("112.5"), BigDecimal("100"))
        assertEquals(NivelOrcamento.ESTOUROU, estourou.nivel)
        assertEquals("Estourou R$ 12,50", estourou.resumo)
        assertEquals("Resta R$ 68,25", situacaoDoOrcamento(BigDecimal("81.75"), BigDecimal("150")).resumo)
    }

    @Test
    fun leValoresDigitados() {
        assertEquals(BigDecimal("24.90"), lerDecimal("24,90"))
        assertEquals(BigDecimal("1234.50"), lerDecimal("R$ 1.234,50"))
        assertEquals(BigDecimal("3.5"), lerDecimal("3.5"))
        assertNull(lerDecimal(""))
        assertNull(lerDecimal("abc"))
        assertEquals("R$ 24,90", moeda(BigDecimal("24.9")))
    }

    @Test
    fun reconheceLinkImpressoDaNota() {
        assertTrue(ehLinkDeNota("http://sistemas.sefaz.am.gov.br/nfceweb/consultarNFCe.jsp?p=1326|3|1"))
        assertTrue(ehLinkDeNota("https://sistemas.sefaz.am.gov.br/nfceweb/consultarNFCe.jsp?p=1"))
        assertFalse(ehLinkDeNota("https://exemplo.com/qr"))
        assertFalse(ehLinkDeNota("7891000100103"))
    }

    // ---- Sessão e renovação do token ----

    private fun servidor(trataRefresh: (RecordedRequest) -> MockResponse): MockWebServer {
        val s = MockWebServer()
        s.dispatcher = object : Dispatcher() {
            override fun dispatch(request: RecordedRequest): MockResponse = when (request.path) {
                "/api/auth/login/refresh/" -> trataRefresh(request)
                "/api/lists/" ->
                    if (request.getHeader("Authorization") == "Bearer novo-access") {
                        MockResponse().setBody("[]")
                    } else {
                        MockResponse().setResponseCode(401).setBody("""{"detail":"Token vencido"}""")
                    }
                "/api/auth/register/" ->
                    MockResponse().setResponseCode(400).setBody(recurso("erro_cadastro.json"))
                else -> MockResponse().setResponseCode(404)
            }
        }
        s.start()
        return s
    }

    @Test
    fun em401RenovaUmaVezSalvaORefreshNovoERepete() = runBlocking {
        val s = servidor {
            assertTrue(it.body.readUtf8().contains("refresh-antigo"))
            MockResponse().setBody("""{"access":"novo-access","refresh":"refresh-novo"}""")
        }
        val tokens = TokensEmMemoria(Tokens("access-vencido", "refresh-antigo"))
        val api = Rede(tokens, s.url("/api").toString()).api

        val r = chamar { api.listas() }

        assertTrue(r is Resultado.Ok)
        assertEquals(Tokens("novo-access", "refresh-novo"), tokens.tokens)
        assertEquals(1, (0 until s.requestCount).count { s.takeRequest().path == "/api/auth/login/refresh/" })
        s.shutdown()
    }

    @Test
    fun refreshRecusadoEncerraASessaoComAviso() = runBlocking {
        val s = servidor { MockResponse().setResponseCode(401).setBody("""{"detail":"Token is blacklisted"}""") }
        val tokens = TokensEmMemoria(Tokens("access-vencido", "refresh-antigo"))
        val api = Rede(tokens, s.url("/api").toString()).api

        val r = chamar { api.listas() }

        assertTrue(r is Resultado.Falha && r.status == 401)
        assertNull(tokens.tokens)
        assertTrue(tokens.expirou)
        s.shutdown()
    }

    @Test
    fun servidorForaNaoDerrubaASessao() = runBlocking {
        val s = servidor { MockResponse().setResponseCode(503) }
        val tokens = TokensEmMemoria(Tokens("access-vencido", "refresh-antigo"))
        val api = Rede(tokens, s.url("/api").toString()).api

        chamar { api.listas() }

        assertEquals("refresh-antigo", tokens.tokens?.refresh)
        assertFalse(tokens.expirou)
        s.shutdown()
    }

    @Test
    fun erroDeCampoViraMensagemComRotulo() = runBlocking {
        val s = servidor { MockResponse() }
        val api = Rede(TokensEmMemoria(null), s.url("/api").toString()).api

        val r = chamar {
            api.cadastrar(br.com.comprafacil.data.api.CadastroRequest("A", "x", "1"))
        }

        assertTrue(r is Resultado.Falha)
        assertTrue((r as Resultado.Falha).mensagem.startsWith("E-mail: "))
        // Cadastro não leva token nem tenta renovar.
        assertNull(s.takeRequest().getHeader("Authorization"))
        s.shutdown()
    }
}
