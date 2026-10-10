package com.baboons.sellers.data.remote

import com.baboons.sellers.BuildConfig
import com.google.gson.annotations.SerializedName
import com.baboons.sellers.data.local.entities.PedidoPendienteEntity
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.*
import java.util.concurrent.TimeUnit

// ─── DTOs ────────────────────────────────────────────────────────────────────

data class PaginationDto(
    val current_page: Int,
    val items_per_page: Int,
    val total_items: Int,
    val total_pages: Int
)

data class PaginatedResponse<T>(
    val data: List<T>,
    val pagination: PaginationDto?
)

data class LoginRequest(
    @SerializedName("nombre") val email: String, 
    val password: String
)

data class LoginResponse(
    val token: String,
    val nombre: String,
    val rol: String,
    val negocio_id: Int?,
    val negocio_nombre: String?
)

data class NegocioDto(val id: Int, val nombre: String, val tipo_app: String)

data class ZonaDto(
    val id: Int,
    val nombre: String,
    val descripcion: String?
)

data class UnidadDto(
    val id: Int,
    val nombre: String,
    val abreviatura: String?
)

data class ClienteDto(
    val id: Int,
    val nombre: String,
    val direccion: String?,
    val telefono: String?,
    val email: String?,
    val zona_id: Int?,
    val latitud: Double?,
    val longitud: Double?,
    val saldo_ctacte: Double?,
    val ultimo_pago_monto: Double?,
    val ultimo_pago_fecha: String?,
    val ultimo_pago_medio: String?,
    val hoja_ruta_id: Int? = null
)

data class ProductoDto(
    val id: Int,
    val nombre: String,
    val codigo_barras: String?,
    val stock: Double,
    val unidad: String?,
    val precio_base: Double?,
    val imagen_url: String?
)

data class PrecioDto(
    val id: Int,
    val producto_id: Int,
    val lista_precio_id: Int,
    val valor: Double
)

data class ListaPrecioDto(
    val id: Int,
    val nombre_lista: String,
    val moneda: String?,
    val margen_sugerido: Double?,
    val descripcion_regla: String?
)

data class HojaRutaDto(
    val id: Int,
    val estado: String,
    val fecha: String,
    val vendedor_id: Int,
    val turnos: String? = null,
    val numero_interno: Int? = null
)

data class PedidoItemDto(
    val producto_id: Int,
    val nombre: String,
    val cantidad: Double,
    val precio_unitario: Double,
    val lista_precio_id: Int?
)

data class CrearPedidoRequest(
    val cliente_id: Int,
    val items: List<PedidoItemDto>,
    val observaciones: String?,
    val uuid_local: String,  // Para deduplicación en el backend
    val forma_pago: String = "cuenta_corriente"
)

data class CrearPedidoResponse(
    val id: Int,
    val uuid_local: String?,
    val mensaje: String
)

data class PedidosOfflineRequest(
    val pedidos: List<com.baboons.sellers.data.local.entities.PedidoPendienteEntity>
)

data class PedidoDto(
    val uuid_local: String,
    val cliente_id: Int,
    val cliente_nombre: String,
    val negocio_id: Int,
    val total: Double,
    val items_json: String?,
    val observaciones: String?,
    val hoja_ruta_id: Int?,
    val estado_sync: String?,
    val creado_en: Long?
)

data class SyncSnapshotResponse(
    val timestamp: String,
    val negocio_id: Int,
    val negocio_nombre: String? = null,
    val clientes: List<ClienteDto>,
    val productos: List<ProductoDto>,
    val listas_precios: List<ListaPrecioDto>,
    val precios: List<PrecioDto>,
    val zonas: List<ZonaDto>,
    val unidades: List<UnidadDto>,
    val hojas_ruta: List<HojaRutaDto>? = null,
    val hoja_ruta_clientes: List<HojaRutaClienteDto>? = null,
    val pedidos_recientes: List<PedidoDto>? = null
)

data class HojaRutaClienteDto(
    val id: Int,
    val hoja_ruta_id: Int,
    val cliente_id: Int
)

// ─── Tracking GPS del chofer ─────────────────────────────────────────────────

data class PosicionTrackingDto(
    val lat: Double,
    val lng: Double,
    val accuracy: Double? = null,
    val speed: Double? = null,
    val ts: Long? = null
)

data class TrackingBatchRequest(
    val posiciones: List<PosicionTrackingDto>
)

data class RastroDto(
    val lat: Double,
    val lng: Double,
    val precision_m: Double? = null,
    val velocidad_kmh: Double? = null,
    val ts: String? = null
)

// ─── API Interface ────────────────────────────────────────────────────────────

interface BaboonsApi {

    // Auth
    @POST("api/login")
    suspend fun login(@Body request: LoginRequest): Response<LoginResponse>

    // Negocios del usuario
    @GET("api/negocios")
    suspend fun getMisNegocios(): Response<List<NegocioDto>>

    // Clientes
    @GET("api/negocios/{negocioId}/clientes")
    suspend fun getClientes(@Path("negocioId") negocioId: Int, @Query("page") page: Int = 1, @Query("per_page") perPage: Int = 1000): Response<PaginatedResponse<ClienteDto>>

    // Productos / Inventario
    @GET("api/negocios/{negocioId}/inventario")
    suspend fun getProductos(@Path("negocioId") negocioId: Int, @Query("page") page: Int = 1, @Query("per_page") perPage: Int = 1000): Response<PaginatedResponse<ProductoDto>>

    // Precios por producto (verificador de precios móvil existente)
    @GET("api/negocios/{negocioId}/mobile/check-producto/{barcode}")
    suspend fun getProductoByBarcode(
        @Path("negocioId") negocioId: Int,
        @Path("barcode") barcode: String
    ): Response<ProductoDto>

    @POST("api/negocios/{negocioId}/sync/pedidos-offline")
    suspend fun syncPedidosOffline(@Path("negocioId") negocioId: Int, @Body body: PedidosOfflineRequest): Response<Any>

    // Crear pedido (¡ya existe en el backend!)
    @POST("api/negocios/{negocioId}/pedidos")
    suspend fun crearPedido(
        @Path("negocioId") negocioId: Int,
        @Body request: CrearPedidoRequest
    ): Response<CrearPedidoResponse>

    // Snapshot para sync inicial (NUEVO a crear en Flask)
    @GET("api/negocios/{negocioId}/sync/snapshot")
    suspend fun getSyncSnapshot(@Path("negocioId") negocioId: Int): Response<SyncSnapshotResponse>

    // Nuevo endpoint específico para rutas activas (más robusto)
    @GET("api/negocios/{negocioId}/mobile/hojas-ruta-activas")
    suspend fun getHojasRutaActivas(@Path("negocioId") negocioId: Int): Response<List<HojaRutaDto>>

    // Tracking GPS del chofer: envía una o varias posiciones
    @POST("api/negocios/{negocioId}/mobile/tracking")
    suspend fun postTracking(@Path("negocioId") negocioId: Int, @Body body: TrackingBatchRequest): Response<Any>

    // Rastro propio del vendedor para el mapa de la app
    @GET("api/negocios/{negocioId}/mobile/mi-rastro")
    suspend fun getMiRastro(@Path("negocioId") negocioId: Int, @Query("fecha") fecha: String? = null): Response<List<RastroDto>>
}

// ─── Cliente HTTP ─────────────────────────────────────────────────────────────

object RetrofitClient {
    fun getApi(tokenProvider: () -> String?): BaboonsApi {
        val logging = HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.HEADERS
        }

        val httpClient = OkHttpClient.Builder()
            .addInterceptor { chain ->
                val token = tokenProvider()
                val req = if (token != null && token.isNotBlank()) {
                    chain.request().newBuilder()
                        .addHeader("Authorization", "Bearer $token")
                        .build()
                } else {
                    chain.request()
                }
                chain.proceed(req)
            }
            .addInterceptor(logging)
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(60, TimeUnit.SECONDS)
            .build()

        return Retrofit.Builder()
            .baseUrl(BuildConfig.BASE_URL)
            .client(httpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(BaboonsApi::class.java)
    }
}
