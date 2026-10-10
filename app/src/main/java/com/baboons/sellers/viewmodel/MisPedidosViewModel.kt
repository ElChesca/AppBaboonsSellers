package com.baboons.sellers.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.baboons.sellers.data.local.dao.PedidoPendienteDao
import com.baboons.sellers.data.local.dao.HojaRutaDao
import com.baboons.sellers.data.local.entities.PedidoPendienteEntity
import com.baboons.sellers.data.local.entities.HojaRutaEntity
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import javax.inject.Inject

@HiltViewModel
class MisPedidosViewModel @Inject constructor(
    private val pedidoPendienteDao: PedidoPendienteDao,
    private val hojaRutaDao: HojaRutaDao
) : ViewModel() {

    val allPedidos: StateFlow<List<PedidoPendienteEntity>> = pedidoPendienteDao.getAll()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val hojasRuta: StateFlow<List<HojaRutaEntity>> = hojaRutaDao.getAll()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _selectedHrId = MutableStateFlow<Int?>(null)
    val selectedHrId = _selectedHrId.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery = _searchQuery.asStateFlow()

    val filteredPedidos = combine(allPedidos, _selectedHrId, _searchQuery, hojasRuta) { pedidos, hrId, query, routes ->
        // Si no hay ruta seleccionada pero hay rutas disponibles, podríamos auto-seleccionar la primera
        // Pero para el filtrado, si hrId es null, no mostramos nada para forzar la selección.
        pedidos.filter { pedido ->
            val matchesHr = if (hrId == null) false else pedido.hoja_ruta_id == hrId
            val matchesSearch = query.isBlank() || 
                pedido.cliente_nombre.contains(query, ignoreCase = true)
            matchesHr && matchesSearch
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun setSelectedHr(id: Int?) {
        _selectedHrId.value = id
    }

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }
}
