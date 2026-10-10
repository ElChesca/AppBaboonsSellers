package com.baboons.sellers.data.local.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "clientes")
data class ClienteEntity(
    @PrimaryKey val id: Int,
    val nombre: String,
    val direccion: String? = null,
    val telefono: String? = null,
    val email: String? = null,
    val zona_id: Int? = null,
    val latitud: Double? = null,
    val longitud: Double? = null,
    val saldo_ctacte: Double = 0.0,
    val ultimo_pago_monto: Double? = null,
    val ultimo_pago_fecha: String? = null,
    val ultimo_pago_medio: String? = null,
    val hoja_ruta_id: Int? = null, // ✨ ID de la ruta asignada
    val updated_at: Long = System.currentTimeMillis()
)

@Entity(tableName = "hojas_ruta")
data class HojaRutaEntity(
    @PrimaryKey val id: Int,
    val estado: String,      // "activa" | "completada" | "pendiente"
    val fecha: String,
    val vendedor_id: Int,
    val turnos: String? = null,
    val numero_interno: Int? = null
)

@Entity(tableName = "hoja_ruta_clientes", primaryKeys = ["hoja_ruta_id", "cliente_id"])
data class HojaRutaClienteEntity(
    val hoja_ruta_id: Int,
    val cliente_id: Int,
    val visitado: Boolean = false,
    val item_id: Int? = null, // id de hoja_ruta_items en el server (para PUT visita)
    val resultado_visita: String? = null // "No estaba — porton cerrado" (local, se muestra en la tarjeta)
)

@Entity(tableName = "zonas")
data class ZonaEntity(
    @PrimaryKey val id: Int,
    val nombre: String,
    val descripcion: String? = null
)

@Entity(tableName = "unidades")
data class UnidadEntity(
    @PrimaryKey val id: Int,
    val nombre: String,
    val abreviatura: String? = null
)

@Entity(tableName = "productos")
data class ProductoEntity(
    @PrimaryKey val id: Int,
    val nombre: String,
    val codigo_barras: String? = null,
    val stock: Double = 0.0,
    val unidad: String = "unidad",
    val precio_base: Double = 0.0,
    val imagen_url: String? = null,
    val updated_at: Long = System.currentTimeMillis()
)

@Entity(tableName = "precios")
data class PrecioEntity(
    @PrimaryKey val id: Int,
    val producto_id: Int,
    val lista_id: Int,
    val valor: Double
)

@Entity(tableName = "listas_precios")
data class ListaPrecioEntity(
    @PrimaryKey val id: Int,
    val nombre_lista: String,
    val moneda: String? = null,
    val margen_sugerido: Double? = null,
    val descripcion_regla: String? = null
)

@Entity(tableName = "pedidos_pendientes")
data class PedidoPendienteEntity(
    @PrimaryKey val uuid_local: String,
    val cliente_id: Int,
    val cliente_nombre: String,
    val negocio_id: Int,
    val items_json: String,           // JSON de la lista de items
    val total: Double,
    val observaciones: String? = null,
    val hoja_ruta_id: Int? = null,
    val estado_sync: String = "pendiente", // "pendiente" | "enviado" | "error" | "conflicto"
    val error_mensaje: String? = null,
    val creado_en: Long = System.currentTimeMillis(),
    val intentos_sync: Int = 0
)

// Cola offline de posiciones GPS del tracking del chofer
@Entity(tableName = "posiciones_pendientes")
data class PosicionPendienteEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val lat: Double,
    val lng: Double,
    val accuracy: Double? = null,
    val speed: Double? = null,
    val ts: Long = System.currentTimeMillis()
)
