package br.com.comprafacil.data.api

import kotlinx.serialization.json.JsonObject
import okhttp3.ResponseBody
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.PATCH
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query

interface CompraFacilApi {

    @POST("auth/login/")
    suspend fun entrar(@Body corpo: LoginRequest): TokenPair

    @POST("auth/register/")
    suspend fun cadastrar(@Body corpo: CadastroRequest): JsonObject

    @POST("auth/logout/")
    suspend fun sair(@Body corpo: RefreshRequest): Response<ResponseBody>

    @GET("auth/me/")
    suspend fun eu(): Usuario

    @GET("lists/")
    suspend fun listas(): List<Lista>

    @POST("lists/")
    suspend fun criarLista(@Body corpo: NovaLista): Lista

    @PATCH("lists/{id}/")
    suspend fun editarLista(@Path("id") id: Int, @Body corpo: JsonObject): Lista

    @DELETE("lists/{id}/")
    suspend fun excluirLista(@Path("id") id: Int): Response<ResponseBody>

    @POST("lists/{id}/complete/")
    suspend fun concluirLista(@Path("id") id: Int): Lista

    @POST("lists/{id}/items/")
    suspend fun adicionarItem(@Path("id") listaId: Int, @Body corpo: NovoItem): Item

    @PATCH("lists/{id}/items/{itemId}/")
    suspend fun editarItem(
        @Path("id") listaId: Int,
        @Path("itemId") itemId: Int,
        @Body corpo: JsonObject,
    ): Item

    @DELETE("lists/{id}/items/{itemId}/")
    suspend fun removerItem(@Path("id") listaId: Int, @Path("itemId") itemId: Int): Response<ResponseBody>

    /** Sem [termo], devolve os produtos que a pessoa mais compra. */
    @GET("lists/produtos/")
    suspend fun buscarProdutos(@Query("q") termo: String? = null): List<ProdutoBusca>

    @GET("products/")
    suspend fun produtos(): List<Produto>

    @POST("products/")
    suspend fun criarProduto(@Body corpo: NovoProduto): Produto

    @GET("products/lookup/")
    suspend fun buscarPorCodigo(@Query("barcode") codigo: String): ConsultaCodigo

    @POST("lists/nfce/ler/")
    suspend fun lerNota(@Body corpo: LinkNota): NotaLida

    @POST("lists/nfce/importar/")
    suspend fun importarNota(@Body corpo: ImportarNota): Lista

    /** Preços da cidade. Sem [termo], os mais recentes. [dias]: 7, 30 ou 90. */
    @GET("lists/precos/")
    suspend fun precos(@Query("q") termo: String, @Query("dias") dias: Int): PrecosDaCidade

    @GET("lists/{id}/precos/")
    suspend fun precosDaLista(@Path("id") listaId: Int): PrecosDaLista

    /** Lê a nota e cruza com a lista em andamento: comprados, fora da lista e esquecidos. */
    @POST("lists/{id}/nfce/ler/")
    suspend fun lerNotaNaLista(@Path("id") listaId: Int, @Body corpo: LinkNota): CruzamentoNota

    /** Conclui a lista com os preços e quantidades da nota. 409 = nota já importada. */
    @POST("lists/{id}/nfce/concluir/")
    suspend fun concluirComNota(@Path("id") listaId: Int, @Body corpo: ConcluirComNota): Lista

    /** Cria uma lista nova em andamento com produtos de uma compra. */
    @POST("lists/{id}/repetir/")
    suspend fun repetirLista(@Path("id") listaId: Int, @Body corpo: RepetirLista): Lista
}
