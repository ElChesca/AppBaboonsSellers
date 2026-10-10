package com.baboons.sellers.di

import android.content.Context
import androidx.room.Room
import com.baboons.sellers.BuildConfig
import com.baboons.sellers.data.local.AppDatabase
import com.baboons.sellers.data.remote.BaboonsApi
import com.baboons.sellers.data.remote.RetrofitClient
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): AppDatabase {
        return Room.databaseBuilder(
            context,
            AppDatabase::class.java,
            "baboons_sellers.db"
        ).fallbackToDestructiveMigration().build()
    }

    @Provides
    @Singleton
    fun provideBaboonsApi(@ApplicationContext context: Context): BaboonsApi {
        return RetrofitClient.getApi { 
            com.baboons.sellers.util.TokenStore.getToken(context) 
        } 
    }

    // DAOs
    @Provides
    fun provideClienteDao(db: AppDatabase) = db.clienteDao()

    @Provides
    fun provideProductoDao(db: AppDatabase) = db.productoDao()

    @Provides
    fun providePrecioDao(db: AppDatabase) = db.precioDao()

    @Provides
    fun providePedidoPendienteDao(db: AppDatabase) = db.pedidoPendienteDao()

    @Provides
    fun provideZonaDao(db: AppDatabase) = db.zonaDao()

    @Provides
    fun provideUnidadDao(db: AppDatabase) = db.unidadDao()

    @Provides
    fun provideListaPrecioDao(db: AppDatabase) = db.listaPrecioDao()

    @Provides
    fun provideHojaRutaDao(db: AppDatabase) = db.hojaRutaDao()

    @Provides
    fun provideHojaRutaClienteDao(db: AppDatabase) = db.hojaRutaClienteDao()

    @Provides
    fun providePosicionPendienteDao(db: AppDatabase) = db.posicionPendienteDao()
}
