package com.baboons.sellers.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import com.baboons.sellers.data.local.dao.*
import com.baboons.sellers.data.local.entities.*

@Database(
    entities = [
        ClienteEntity::class,
        ProductoEntity::class,
        PrecioEntity::class,
        PedidoPendienteEntity::class,
        ZonaEntity::class,
        UnidadEntity::class,
        ListaPrecioEntity::class,
        HojaRutaEntity::class,
        HojaRutaClienteEntity::class,
        PosicionPendienteEntity::class,
        CobroLocalEntity::class
    ],
    version = 11,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun clienteDao(): ClienteDao
    abstract fun productoDao(): ProductoDao
    abstract fun precioDao(): PrecioDao
    abstract fun pedidoPendienteDao(): PedidoPendienteDao
    abstract fun zonaDao(): ZonaDao
    abstract fun unidadDao(): UnidadDao
    abstract fun listaPrecioDao(): ListaPrecioDao
    abstract fun hojaRutaDao(): HojaRutaDao
    abstract fun hojaRutaClienteDao(): HojaRutaClienteDao
    abstract fun posicionPendienteDao(): PosicionPendienteDao
    abstract fun cobroLocalDao(): CobroLocalDao
}
