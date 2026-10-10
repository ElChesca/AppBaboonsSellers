package com.baboons.sellers.data.repository

import androidx.room.withTransaction
import com.baboons.sellers.data.local.AppDatabase
import com.baboons.sellers.data.local.entities.*
import com.baboons.sellers.data.remote.*
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SyncRepository @Inject constructor(
    private val api: BaboonsApi,
    private val db: AppDatabase,
    @dagger.hilt.android.qualifiers.ApplicationContext private val context: android.content.Context
) {
    /**
     * Descarga y guarda todos los datos maestros necesarios para el funcionamiento offline.
     */
    suspend fun syncSnapshot(negocioId: Int): Result<Unit> {
        return try {
            val response = api.getSyncSnapshot(negocioId)
            if (response.isSuccessful && response.body() != null) {
                val snapshot = response.body()!!
                

                db.withTransaction<Unit> {
                    // 1. Limpiar datos antiguos
                    db.clienteDao().deleteAll()
                    db.productoDao().deleteAll()
                    db.precioDao().deleteAll()
                    db.listaPrecioDao().deleteAll()
                    db.zonaDao().deleteAll()
                    db.unidadDao().deleteAll()

                    // 2. Insertar nuevos datos mapeados de DTO a Entity
                    db.clienteDao().upsertAll(snapshot.clientes.map { it.toEntity() })
                    db.productoDao().upsertAll(snapshot.productos.map { it.toEntity() })
                    db.precioDao().upsertAll(snapshot.precios.map { it.toEntity() })
                    db.listaPrecioDao().upsertAll(snapshot.listas_precios.map { it.toEntity() })
                    db.zonaDao().upsertAll(snapshot.zonas.map { it.toEntity() })
                    db.unidadDao().upsertAll(snapshot.unidades.map { it.toEntity() })
                    
                    snapshot.hojas_ruta?.let { hrList ->
                        db.hojaRutaDao().deleteAll()
                        db.hojaRutaDao().upsertAll(hrList.map { it.toEntity() })
                    } ?: run {
                        val hrResp = api.getHojasRutaActivas(negocioId)
                        if (hrResp.isSuccessful && hrResp.body() != null) {
                            db.hojaRutaDao().deleteAll()
                            db.hojaRutaDao().upsertAll(hrResp.body()!!.map { it.toEntity() })
                        }
                    }

                    // ✨ Guardar asignaciones de clientes a rutas preservedando el estado local
                    snapshot.hoja_ruta_clientes?.let { mappingList ->
                        val existingMappings = db.hojaRutaClienteDao().getAllList()
                        db.hojaRutaClienteDao().deleteAll()
                        db.hojaRutaClienteDao().upsertAll(mappingList.map { dto ->
                            val isVisited = existingMappings.any { 
                                it.hoja_ruta_id == dto.hoja_ruta_id && it.cliente_id == dto.cliente_id && it.visitado 
                            }
                            HojaRutaClienteEntity(dto.hoja_ruta_id, dto.cliente_id, isVisited)
                        })
                    }

                    // ✨ Guardar el nombre real del negocio para que persista
                    snapshot.negocio_nombre?.let { 
                        com.baboons.sellers.util.TokenStore.saveNegocioNombre(context, it)
                    }

                    // ✨ Guardar pedidos recientes (historial sincronizado)
                    snapshot.pedidos_recientes?.let { recentList ->
                        db.pedidoPendienteDao().borrarSincronizados()
                        recentList.forEach { pedido ->
                           db.pedidoPendienteDao().insertar(pedido.toEntity())
                        }
                    }
                }
                Result.success(Unit)
            } else {
                Result.failure(Exception("Error en sync: ${response.code()} - ${response.message()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun uploadPendingOrders(negocioId: Int): Result<Int> {
        val pendientes = db.pedidoPendienteDao().getPendientes()
        if (pendientes.isEmpty()) return Result.success(0)

        // Enviamos todo el bloque de pedidos pendientes
        return try {
            val response = api.syncPedidosOffline(negocioId, PedidosOfflineRequest(pendientes))
            if (response.isSuccessful) {
                // Si el backend recibió el bloque, marcamos todos como completados
                // (En una implementación más fina, el backend devolvería qué IDs fallaron)
                pendientes.forEach {
                    db.pedidoPendienteDao().actualizarEstado(it.uuid_local, "sincronizado")
                }
                Result.success(pendientes.size)
            } else {
                Result.failure(Exception("Error al subir: ${response.code()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // Extension functions para mapeo directo (ayudan a mantener el repositorio limpio)
    
    private fun PedidoDto.toEntity() = PedidoPendienteEntity(
        uuid_local = uuid_local,
        cliente_id = cliente_id,
        cliente_nombre = cliente_nombre,
        negocio_id = negocio_id,
        items_json = items_json ?: "[]",
        total = total,
        observaciones = observaciones,
        hoja_ruta_id = hoja_ruta_id,
        estado_sync = estado_sync ?: "sincronizado",
        creado_en = creado_en ?: System.currentTimeMillis()
    )

    private fun ClienteDto.toEntity() = ClienteEntity(
        id = id,
        nombre = nombre,
        direccion = direccion,
        telefono = telefono,
        email = email,
        zona_id = zona_id,
        latitud = latitud,
        longitud = longitud,
        saldo_ctacte = saldo_ctacte ?: 0.0,
        ultimo_pago_monto = ultimo_pago_monto,
        ultimo_pago_fecha = ultimo_pago_fecha,
        ultimo_pago_medio = ultimo_pago_medio,
        hoja_ruta_id = hoja_ruta_id
    )

    private fun ProductoDto.toEntity() = ProductoEntity(
        id = id,
        nombre = nombre,
        codigo_barras = codigo_barras,
        stock = stock,
        unidad = unidad ?: "unidad",
        precio_base = precio_base ?: 0.0,
        imagen_url = imagen_url
    )

    private fun PrecioDto.toEntity() = PrecioEntity(
        id = id,
        producto_id = producto_id,
        lista_id = lista_precio_id,
        valor = valor
    )

    private fun ListaPrecioDto.toEntity() = ListaPrecioEntity(
        id = id,
        nombre_lista = nombre_lista,
        moneda = moneda,
        margen_sugerido = margen_sugerido,
        descripcion_regla = descripcion_regla
    )

    private fun ZonaDto.toEntity() = ZonaEntity(
        id = id,
        nombre = nombre,
        descripcion = descripcion
    )

    private fun UnidadDto.toEntity() = UnidadEntity(
        id = id,
        nombre = nombre,
        abreviatura = abreviatura
    )

    private fun HojaRutaDto.toEntity() = HojaRutaEntity(
        id = id,
        estado = estado,
        fecha = fecha,
        vendedor_id = vendedor_id,
        turnos = turnos,
        numero_interno = numero_interno
    )
}
