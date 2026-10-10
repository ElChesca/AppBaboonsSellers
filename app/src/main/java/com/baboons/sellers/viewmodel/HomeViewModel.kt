package com.baboons.sellers.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.baboons.sellers.data.local.dao.*
import com.baboons.sellers.data.local.entities.*
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import com.baboons.sellers.data.repository.SyncRepository
import com.baboons.sellers.util.TokenStore
import android.app.Application
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val clienteDao: ClienteDao,
    private val hojaRutaDao: HojaRutaDao,
    private val hojaRutaClienteDao: HojaRutaClienteDao,
    private val pedidoPendienteDao: PedidoPendienteDao,
    private val syncRepository: SyncRepository,
    private val app: Application
) : ViewModel() {

    private val _negocioNombre = MutableStateFlow(TokenStore.getNegocioNombre(app) ?: "Mi Negocio")
    val negocioNombre = _negocioNombre.asStateFlow()

    private val _isSyncing = MutableStateFlow(false)
    val isSyncing = _isSyncing.asStateFlow()

    private val _selectedHojaRutaId = MutableStateFlow<Int?>(null)
    val selectedHojaRutaId = _selectedHojaRutaId.asStateFlow()
    
    val clientes: StateFlow<List<ClienteEntity>> = clienteDao.getAll()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    val hojasRuta: StateFlow<List<HojaRutaEntity>> = hojaRutaDao.getAll()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    val hojaRutaClientes: StateFlow<List<HojaRutaClienteEntity>> = hojaRutaClienteDao.getAll()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    val pedidos: StateFlow<List<PedidoPendienteEntity>> = pedidoPendienteDao.getAll()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    fun selectHojaRuta(id: Int?) {
        _selectedHojaRutaId.value = id
    }

    fun toggleVisita(clienteId: Int, hojaRutaId: Int, visitado: Boolean) {
        viewModelScope.launch {
            hojaRutaClienteDao.updateVisita(hojaRutaId, clienteId, visitado)
        }
    }

    fun sync(negocioId: Int) {
        viewModelScope.launch {
            _isSyncing.value = true
            // 1. Subir lo que tenemos pendiente localmente
            syncRepository.uploadPendingOrders(negocioId)
            // 2. Descargar datos actualizados
            syncRepository.syncSnapshot(negocioId)
            _negocioNombre.value = TokenStore.getNegocioNombre(app) ?: "Mi Negocio"
            _isSyncing.value = false
        }
    }
}
