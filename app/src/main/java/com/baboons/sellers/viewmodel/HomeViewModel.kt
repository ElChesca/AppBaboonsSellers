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
    private val productoDao: ProductoDao,
    private val cobroLocalDao: CobroLocalDao,
    private val api: com.baboons.sellers.data.remote.BaboonsApi,
    private val syncRepository: SyncRepository,
    private val app: Application
) : ViewModel() {

    private val _negocioNombre = MutableStateFlow(TokenStore.getNegocioNombre(app) ?: "Mi Negocio")
    val negocioNombre = _negocioNombre.asStateFlow()

    private val _isSyncing = MutableStateFlow(false)
    val isSyncing = _isSyncing.asStateFlow()

    // Hay UNA instancia de este VM por NavBackStackEntry: la ruta seleccionada
    // vive en un companion (compartida por todas las instancias) y se persiste.
    companion object {
        private val _sharedSelectedHojaRutaId = MutableStateFlow<Int?>(null)
    }
    val selectedHojaRutaId: StateFlow<Int?> = _sharedSelectedHojaRutaId

    init {
        if (_sharedSelectedHojaRutaId.value == null) {
            _sharedSelectedHojaRutaId.value = TokenStore.getHojaRutaId(app).takeIf { it > 0 }
        }
    }
    
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

    val cobros: StateFlow<List<CobroLocalEntity>> = cobroLocalDao.getAll()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    val productos: StateFlow<List<ProductoEntity>> = productoDao.getAll()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    fun selectHojaRuta(id: Int?) {
        _sharedSelectedHojaRutaId.value = id
        TokenStore.saveHojaRutaId(app, id ?: -1)
    }

    fun toggleVisita(clienteId: Int, hojaRutaId: Int, visitado: Boolean) {
        viewModelScope.launch {
            // Marca local inmediata
            hojaRutaClienteDao.updateVisita(hojaRutaId, clienteId, visitado)
            // Aviso al server (el admin lo ve en la web). Si falla, se revierte.
            val item = hojaRutaClientes.value.firstOrNull {
                it.hoja_ruta_id == hojaRutaId && it.cliente_id == clienteId
            }
            val itemId = item?.item_id ?: return@launch
            try {
                val resp = api.updateVisitaItem(
                    hojaRutaId, itemId,
                    com.baboons.sellers.data.remote.VisitaRequest(visitado = visitado)
                )
                if (!resp.isSuccessful) {
                    hojaRutaClienteDao.updateVisita(hojaRutaId, clienteId, !visitado)
                }
            } catch (e: Exception) {
                hojaRutaClienteDao.updateVisita(hojaRutaId, clienteId, !visitado)
            }
        }
    }

    /** Resultado de la visita: marca visitado=true y guarda la observación (Entregado/No estaba/...) */
    fun marcarResultadoVisita(clienteId: Int, hojaRutaId: Int, resultado: String, obs: String, onResult: (Boolean, String) -> Unit) {
        viewModelScope.launch {
            val observaciones = listOf(resultado, obs).filter { it.isNotBlank() }.joinToString(" — ")
            val item = hojaRutaClientes.value.firstOrNull {
                it.hoja_ruta_id == hojaRutaId && it.cliente_id == clienteId
            }
            val itemId = item?.item_id
            if (itemId == null) {
                onResult(false, "Este cliente no vino con item_id; resincronizá")
                return@launch
            }
            try {
                val resp = api.updateVisitaItem(
                    hojaRutaId, itemId,
                    com.baboons.sellers.data.remote.VisitaRequest(visitado = true, observaciones = observaciones)
                )
                if (resp.isSuccessful) {
                    hojaRutaClienteDao.updateResultadoVisita(hojaRutaId, clienteId, true, observaciones)
                    onResult(true, "Visita registrada")
                } else {
                    onResult(false, "Error del servidor (${resp.code()})")
                }
            } catch (e: Exception) {
                onResult(false, "Sin conexión: ${e.message}")
            }
        }
    }

    fun registrarCobro(clienteId: Int, hojaRutaId: Int, monto: Double, metodo: String, onResult: (Boolean, String) -> Unit) {
        viewModelScope.launch {
            val negocioId = TokenStore.getNegocioId(app)
            try {
                val resp = api.registrarCobro(
                    hojaRutaId,
                    com.baboons.sellers.data.remote.CobroRequest(clienteId, negocioId, monto, metodo)
                )
                if (resp.isSuccessful) {
                    val nombre = clientes.value.firstOrNull { it.id == clienteId }?.nombre ?: "Cliente #$clienteId"
                    cobroLocalDao.insertar(
                        com.baboons.sellers.data.local.entities.CobroLocalEntity(
                            cliente_id = clienteId, cliente_nombre = nombre,
                            hoja_ruta_id = hojaRutaId, monto = monto, metodo_pago = metodo
                        )
                    )
                    onResult(true, "Cobro registrado — el admin lo confirma")
                }
                else onResult(false, "Error del servidor (${resp.code()}): ${resp.errorBody()?.string() ?: ""}")
            } catch (e: Exception) {
                onResult(false, "Sin conexión: ${e.message}")
            }
        }
    }

    fun pedirRefuerzo(items: List<com.baboons.sellers.data.remote.RefuerzoItem>, onResult: (Boolean, String) -> Unit) {
        viewModelScope.launch {
            val hrId = _sharedSelectedHojaRutaId.value
                ?: TokenStore.getHojaRutaId(app).takeIf { it > 0 }
            if (hrId == null) {
                onResult(false, "No hay ruta seleccionada")
                return@launch
            }
            try {
                val resp = api.pedirRefuerzo(hrId, com.baboons.sellers.data.remote.RefuerzoRequest(items))
                if (resp.isSuccessful) onResult(true, "Refuerzo enviado al depósito")
                else onResult(false, "Error del servidor (${resp.code()})")
            } catch (e: Exception) {
                onResult(false, "Sin conexión: ${e.message}")
            }
        }
    }

    /** Novedad de ruta (camión roto, cliente cerrado, etc.) -> notificación al admin. */
    fun reportarNovedad(tipo: String, obs: String, clienteId: Int?, onResult: (Boolean, String) -> Unit) {
        viewModelScope.launch {
            val hrId = _sharedSelectedHojaRutaId.value
                ?: TokenStore.getHojaRutaId(app).takeIf { it > 0 }
            if (hrId == null) {
                onResult(false, "No hay ruta seleccionada")
                return@launch
            }
            try {
                val resp = api.reportarNovedad(
                    hrId,
                    com.baboons.sellers.data.remote.NovedadRequest(tipo, obs, clienteId)
                )
                if (resp.isSuccessful) onResult(true, "Novedad reportada al admin")
                else onResult(false, "Error del servidor (${resp.code()})")
            } catch (e: Exception) {
                onResult(false, "Sin conexión: ${e.message}")
            }
        }
    }

    /** Gasto operativo en ruta (peaje, combustible, etc.) -> gastos_operativos del ERP. */
    fun registrarGasto(monto: Double, categoria: String, obs: String, onResult: (Boolean, String) -> Unit) {
        viewModelScope.launch {
            val hrId = _sharedSelectedHojaRutaId.value
                ?: TokenStore.getHojaRutaId(app).takeIf { it > 0 }
            if (hrId == null) {
                onResult(false, "No hay ruta seleccionada")
                return@launch
            }
            try {
                val resp = api.registrarGasto(
                    hrId,
                    com.baboons.sellers.data.remote.GastoRequest(monto, categoria, obs)
                )
                if (resp.isSuccessful) onResult(true, "Gasto registrado")
                else onResult(false, "Error del servidor (${resp.code()})")
            } catch (e: Exception) {
                onResult(false, "Sin conexión: ${e.message}")
            }
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
