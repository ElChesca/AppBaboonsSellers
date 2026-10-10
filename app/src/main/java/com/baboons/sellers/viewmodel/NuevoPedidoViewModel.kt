package com.baboons.sellers.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.baboons.sellers.data.local.dao.PedidoPendienteDao
import com.baboons.sellers.data.local.dao.ProductoDao
import com.baboons.sellers.data.local.entities.PedidoPendienteEntity
import com.baboons.sellers.data.local.entities.ProductoEntity
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class NuevoPedidoViewModel @Inject constructor(
    private val productoDao: com.baboons.sellers.data.local.dao.ProductoDao,
    private val clienteDao: com.baboons.sellers.data.local.dao.ClienteDao,
    private val pedidoPendienteDao: com.baboons.sellers.data.local.dao.PedidoPendienteDao
) : ViewModel() {

    suspend fun getCliente(id: Int) = clienteDao.getById(id)
    
    val productos: StateFlow<List<ProductoEntity>> = productoDao.getAll()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )
        
    fun guardarPedidoOffline(pedido: PedidoPendienteEntity, enExito: () -> Unit) {
        viewModelScope.launch {
            // Guardar en Room para cola de sincronización
            pedidoPendienteDao.insertar(pedido)
            // Lógica optimista: descontar stock de los productos inmediatamente para no sobrevender
            // (La resolución final se hace en servidor)
            // Se puede iterar sobre el JSON si es necesario descontar localmente,
            // pero por simplicidad solo insertamos el pedido aquí.
            enExito()
        }
    }
}
