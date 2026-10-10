package com.baboons.sellers.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.baboons.sellers.data.local.dao.PedidoPendienteDao
import com.baboons.sellers.data.local.entities.PedidoPendienteEntity
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject
import com.baboons.sellers.data.remote.RetrofitClient
import com.baboons.sellers.util.TokenStore
import android.app.Application
import android.content.Context
import android.widget.Toast
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@HiltViewModel
class PedidosPendientesViewModel @Inject constructor(
    private val pedidoPendienteDao: PedidoPendienteDao,
    private val app: Application // Para acceder a TokenStore
) : ViewModel() {

    // Exponer todos los pedidos guardados localmente
    val pedidos: StateFlow<List<PedidoPendienteEntity>> = pedidoPendienteDao.getAll()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    fun sincronizarManualmente(context: Context, onTerminado: (String) -> Unit) {
        viewModelScope.launch(Dispatchers.IO) {
            val pendientes = pedidoPendienteDao.getPendientes()
            if (pendientes.isEmpty()) {
                withContext(Dispatchers.Main) { onTerminado("No hay pedidos pendientes.") }
                return@launch
            }

            try {
                val api = RetrofitClient.getApi { TokenStore.getToken(app) }
                val negocioId = TokenStore.getNegocioId(app)

                if (negocioId <= 0) {
                    withContext(Dispatchers.Main) { onTerminado("Error: Sin local/negocio asociado.") }
                    return@launch
                }

                val body = com.baboons.sellers.data.remote.PedidosOfflineRequest(pendientes)
                val response = api.syncPedidosOffline(negocioId, body)

                if (response.isSuccessful) {
                    pendientes.forEach {
                        pedidoPendienteDao.actualizarEstado(it.uuid_local, "enviado")
                    }
                    withContext(Dispatchers.Main) { onTerminado("Exito: ${pendientes.size} pedidos enviados.") }
                } else {
                    val errorMsg = response.errorBody()?.string() ?: "Error desconocido"
                    pendientes.forEach { pedidoPendienteDao.incrementarIntentos(it.uuid_local) }
                    withContext(Dispatchers.Main) { onTerminado("Error del servidor: HTTP ${response.code()}") }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) { onTerminado("Error de red: ${e.message}") }
            }
        }
    }
}
