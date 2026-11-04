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
import br.com.comprafacil.ui.comum.limitarCasas
import br.com.comprafacil.ui.comum.moeda
import br.com.comprafacil.ui.comum.quantidadeComMedida
import br.com.comprafacil.ui.comum.situacaoDoOrcamento
import br.com.comprafacil.ui.nota.ehLinkDeNota
import br.com.comprafacil.data.api.FalaDoChat
import br.com.comprafacil.data.api.Loja
import br.com.comprafacil.data.api.PerguntaAoAssistente
import br.com.comprafacil.data.api.RespostaDoAssistente
import br.com.comprafacil.data.api.PrecosDaCidade
import br.com.comprafacil.ui.comum.linkComoChegar
import br.com.comprafacil.ui.comum.nomeDaLoja
import br.com.comprafacil.ui.comum.quandoFoi
import br.com.comprafacil.ui.comum.textoDistancia
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
    fun leOsPrecosDaCidade() {
        val precos = JsonApi.decodeFromString<PrecosDaCidade>(recurso("precos.json"))
        assertEquals(30, precos.dias)
        assertTrue(precos.produtos.isNotEmpty())
        val primeiro = precos.produtos.first()
        val loja = primeiro.lojas.first().loja
        assertEquals("Assaí Atacadista", nomeDaLoja(loja))
        assertEquals("assai", loja.icone)
        assertTrue(primeiro.menorNoPeriodo!!.preco > BigDecimal.ZERO)
    }

    @Test
    fun notaAchaAListaEDistanciaDaLoja() {
        val nota = JsonApi.decodeFromString<NotaLida>(
            """{"total":"69.92","itens":[],"listas_parecidas":[{"id":7,"nome":"Próxima compra","itens_na_nota":5,"total_itens":7}]}""",
        )
        assertEquals("Próxima compra", nota.listasParecidas.single().nome)
        assertEquals(5, nota.listasParecidas.single().itensNaNota)
        // Resposta sem o campo (backend antigo) continua funcionando.
        assertTrue(JsonApi.decodeFromString<NotaLida>("""{"total":"1.00"}""").listasParecidas.isEmpty())

        val loja = JsonApi.decodeFromString<Loja>("""{"id":1,"nome":"SENDAS","distancia_km":1.2}""")
        assertEquals(1.2, loja.distanciaKm!!, 0.0)
        assertEquals("1,2 km", textoDistancia(1.2))
        assertEquals("350 m", textoDistancia(0.35))
        assertEquals("3 km", textoDistancia(3.0))
        assertNull(textoDistancia(null))
    }

    @Test
    fun respostaDoAssistente() {
        // Resposta real do backend local, sem IA configurada.
        val r = JsonApi.decodeFromString<RespostaDoAssistente>(
            """{"resposta":"IOG VIG 800G BATIDO: o mais barato é R$ 9,90 no Assaí Atacadista (a 1,3 km).","fonte":"sem_ia","fontes":[],
               "produtos":[{"id":17,"nome":"IOG VIG 800G BATIDO","embalagem":null,"imagem_url":null,"preco":"9.90","medida":"unit","loja":"Assaí Atacadista"}],
               "lojas":[{"nome":"SENDAS DISTRIBUIDORA S/A","marca":"Assaí Atacadista","icone":"assai","distancia_km":1.3,"url":"https://www.google.com/maps/dir/?api=1"}]}""",
        )
        assertEquals("sem_ia", r.fonte)
        assertEquals(BigDecimal("9.90"), r.produtos.single().preco)
        assertEquals("assai", r.lojas.single().icone)
        // O histórico vai só com papel e texto.
        val corpo = JsonApi.encodeToString(
            PerguntaAoAssistente.serializer(),
            PerguntaAoAssistente(listOf(FalaDoChat("usuario", "café?"))),
        )
        assertEquals("""{"mensagens":[{"papel":"usuario","texto":"café?"}]}""", corpo)
    }

    @Test
    fun comoChegarEQuandoFoi() {
        val loja = Loja(1, "SENDAS DISTRIBUIDORA S/A", endereco = "Av. Rodrigo Otavio, 2595, Manaus, AM")
        assertEquals("Sendas Distribuidora S/A", nomeDaLoja(loja))
        val link = linkComoChegar(loja)
        assertTrue(link.startsWith("https://www.google.com/maps/dir/?api=1&destination="))
        assertTrue(link.endsWith("&travelmode=driving&dir_action=navigate"))
        assertFalse(link.contains(" "))

        val hoje = java.time.LocalDate.of(2026, 10, 30)
        assertEquals("hoje", quandoFoi("2026-10-30T15:00:00Z", hoje))
        assertEquals("ontem", quandoFoi("2026-10-29T15:00:00Z", hoje))
        assertEquals("há 7 dias", quandoFoi("2026-10-23T18:28:28Z", hoje))
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
    fun pesoComAte3Casas() {
        assertEquals("1,235", limitarCasas("1,23567", 3))
        assertEquals("1.2", limitarCasas("1.2", 3))
        assertEquals("12", limitarCasas("12", 3))
        assertEquals("1,234", limitarCasas("1,2,34", 3))
        assertEquals("1,235 kg", quantidadeComMedida(BigDecimal("1.235"), porKg = true))
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
