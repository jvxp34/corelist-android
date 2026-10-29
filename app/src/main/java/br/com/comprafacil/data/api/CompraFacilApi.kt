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
}
