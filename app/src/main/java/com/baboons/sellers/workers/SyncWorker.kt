package com.baboons.sellers.workers

import android.content.Context
import androidx.room.Room
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.baboons.sellers.data.local.AppDatabase
import com.baboons.sellers.data.remote.RetrofitClient
import com.baboons.sellers.util.TokenStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import android.util.Log

class SyncWorker(
    appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        Log.d("SyncWorker", "Iniciando doWork() de SyncWorker...")
        try {
            // 1. Instanciar dependencias manualmente para el Background Worker
            val db = Room.databaseBuilder(
                applicationContext,
                AppDatabase::class.java,
                "baboons_sellers.db"
            ).addMigrations(AppDatabase.MIGRATION_11_12)
                .fallbackToDestructiveMigration().build()
            
            val dao = db.pedidoPendienteDao()
            val api = RetrofitClient.getApi { TokenStore.getToken(applicationContext) }
            val negocioId = TokenStore.getNegocioId(applicationContext)
            
            if (negocioId == null || negocioId <= 0) {
                Log.e("SyncWorker", "Negocio ID nulo o invalido")
                return@withContext Result.failure()
            }

            // 2. Obtener pedidos pendientes
            val pendientes = dao.getPendientes()
            Log.d("SyncWorker", "Pedidos pendientes encontrados: ${pendientes.size}")
            
            if (pendientes.isEmpty()) {
                return@withContext Result.success()
            }

            // 3. Enviar al backend (POST)
            val body = com.baboons.sellers.data.remote.PedidosOfflineRequest(pendientes)
            val response = api.syncPedidosOffline(negocioId, body)
            
            Log.d("SyncWorker", "Respuesta Backend Code: ${response.code()}")

            if (response.isSuccessful) {
                // 4. Si es exitoso, marcar todos como "enviado"
                pendientes.forEach { pedido ->
                    dao.actualizarEstado(pedido.uuid_local, "enviado")
                }
                Log.d("SyncWorker", "Sincronizacion exitosa. Pedidos actualizados a 'enviado'.")
                Result.success()
            } else {
                Log.e("SyncWorker", "Error del backend HTTP: ${response.errorBody()?.string()}")
                // Si hay error HTTP (por ejemplo 500 o 400), incrementar intentos
                pendientes.forEach { pedido ->
                    dao.incrementarIntentos(pedido.uuid_local)
                }
                Result.retry()
            }
        } catch (e: Exception) {
            Log.e("SyncWorker", "Excepcion capturada: ${e.message}")
            // Si hay error de red o timeout
            Result.retry()
        }
    }
}
