package com.baboons.sellers.data.local.dao

import androidx.room.*
import com.baboons.sellers.data.local.entities.*
import kotlinx.coroutines.flow.Flow

@Dao
interface ClienteDao {
    @Query("SELECT * FROM clientes ORDER BY nombre ASC")
    fun getAll(): Flow<List<ClienteEntity>>

    @Query("SELECT * FROM clientes WHERE zona_id = :zonaId ORDER BY nombre ASC")
    fun getByZona(zonaId: Int): Flow<List<ClienteEntity>>

    @Query("SELECT * FROM clientes WHERE nombre LIKE '%' || :q || '%' OR telefono LIKE '%' || :q || '%' LIMIT 50")
    suspend fun buscar(q: String): List<ClienteEntity>

    @Query("SELECT * FROM clientes WHERE id = :id")
    suspend fun getById(id: Int): ClienteEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(clientes: List<ClienteEntity>)

    @Query("DELETE FROM clientes")
    suspend fun deleteAll()
}

@Dao
interface ProductoDao {
    @Query("SELECT * FROM productos ORDER BY nombre ASC")
    fun getAll(): Flow<List<ProductoEntity>>

    @Query("SELECT * FROM productos WHERE nombre LIKE '%' || :q || '%' OR codigo_barras LIKE '%' || :q || '%' LIMIT 50")
    suspend fun buscar(q: String): List<ProductoEntity>

    @Query("SELECT * FROM productos WHERE codigo_barras = :codigo")
    suspend fun getByBarcode(codigo: String): ProductoEntity?

    @Query("SELECT * FROM productos WHERE id = :id")
    suspend fun getById(id: Int): ProductoEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(productos: List<ProductoEntity>)

    @Query("UPDATE productos SET stock = stock - :cantidad WHERE id = :id")
    suspend fun descontarStock(id: Int, cantidad: Double)

    @Query("DELETE FROM productos")
    suspend fun deleteAll()
}

@Dao
interface PrecioDao {
    @Query("SELECT * FROM precios WHERE producto_id = :productoId")
    suspend fun getByProducto(productoId: Int): List<PrecioEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(precios: List<PrecioEntity>)

    @Query("DELETE FROM precios")
    suspend fun deleteAll()
}

@Dao
interface ZonaDao {
    @Query("SELECT * FROM zonas ORDER BY nombre ASC")
    fun getAll(): Flow<List<ZonaEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(zonas: List<ZonaEntity>)

    @Query("DELETE FROM zonas")
    suspend fun deleteAll()
}

@Dao
interface UnidadDao {
    @Query("SELECT * FROM unidades")
    fun getAll(): Flow<List<UnidadEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(unidades: List<UnidadEntity>)

    @Query("DELETE FROM unidades")
    suspend fun deleteAll()
}

@Dao
interface ListaPrecioDao {
    @Query("SELECT * FROM listas_precios WHERE moneda = :moneda OR :moneda IS NULL")
    fun getByMoneda(moneda: String? = null): Flow<List<ListaPrecioEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(listas: List<ListaPrecioEntity>)

    @Query("DELETE FROM listas_precios")
    suspend fun deleteAll()
}

@Dao
interface PedidoPendienteDao {
    @Query("SELECT * FROM pedidos_pendientes ORDER BY creado_en DESC")
    fun getAll(): Flow<List<PedidoPendienteEntity>>

    @Query("SELECT * FROM pedidos_pendientes WHERE estado_sync = 'pendiente' ORDER BY creado_en ASC")
    suspend fun getPendientes(): List<PedidoPendienteEntity>

    @Query("SELECT COUNT(*) FROM pedidos_pendientes WHERE estado_sync = 'pendiente'")
    fun cuentaPendientes(): Flow<Int>

    @Insert(onConflict = OnConflictStrategy.REPLACE) // Cambiado a REPLACE para que el historial sobreescriba registros locales
    suspend fun insertar(pedido: PedidoPendienteEntity)

    @Query("UPDATE pedidos_pendientes SET estado_sync = :estado, error_mensaje = :error WHERE uuid_local = :uuid")
    suspend fun actualizarEstado(uuid: String, estado: String, error: String? = null)

    @Query("UPDATE pedidos_pendientes SET intentos_sync = intentos_sync + 1 WHERE uuid_local = :uuid")
    suspend fun incrementarIntentos(uuid: String)
    @Query("DELETE FROM pedidos_pendientes WHERE estado_sync = 'sincronizado'")
    suspend fun borrarSincronizados()
}

@Dao
interface HojaRutaDao {
    @Query("SELECT * FROM hojas_ruta ORDER BY id DESC")
    fun getAll(): Flow<List<HojaRutaEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(rutas: List<HojaRutaEntity>)

    @Query("SELECT * FROM hojas_ruta WHERE id = :id")
    suspend fun getById(id: Int): HojaRutaEntity?

    @Query("DELETE FROM hojas_ruta")
    suspend fun deleteAll()
}

@Dao
interface PosicionPendienteDao {
    @Insert
    suspend fun insertar(pos: PosicionPendienteEntity)

    @Query("SELECT * FROM posiciones_pendientes ORDER BY ts ASC LIMIT :limit")
    suspend fun getPendientes(limit: Int = 100): List<PosicionPendienteEntity>

    @Delete
    suspend fun borrar(posiciones: List<PosicionPendienteEntity>)

    @Query("SELECT COUNT(*) FROM posiciones_pendientes")
    suspend fun count(): Int
}

@Dao
interface HojaRutaClienteDao {
    @Query("SELECT * FROM hoja_ruta_clientes")
    fun getAll(): kotlinx.coroutines.flow.Flow<List<HojaRutaClienteEntity>>

    @Query("SELECT * FROM hoja_ruta_clientes")
    suspend fun getAllList(): List<HojaRutaClienteEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(items: List<HojaRutaClienteEntity>)

    @Query("UPDATE hoja_ruta_clientes SET visitado = :visitado WHERE hoja_ruta_id = :hojaRutaId AND cliente_id = :clienteId")
    suspend fun updateVisita(hojaRutaId: Int, clienteId: Int, visitado: Boolean)

    @Query("UPDATE hoja_ruta_clientes SET visitado = :visitado, resultado_visita = :resultado WHERE hoja_ruta_id = :hojaRutaId AND cliente_id = :clienteId")
    suspend fun updateResultadoVisita(hojaRutaId: Int, clienteId: Int, visitado: Boolean, resultado: String?)

    @Query("DELETE FROM hoja_ruta_clientes")
    suspend fun deleteAll()
}
