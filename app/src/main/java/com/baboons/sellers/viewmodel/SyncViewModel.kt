package com.baboons.sellers.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.baboons.sellers.data.repository.SyncRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed class SyncState {
    object Idle : SyncState()
    object Loading : SyncState()
    data class Success(val message: String) : SyncState()
    data class Error(val message: String) : SyncState()
}

@HiltViewModel
class SyncViewModel @Inject constructor(
    private val syncRepository: SyncRepository
) : ViewModel() {

    private val _syncState = MutableStateFlow<SyncState>(SyncState.Idle)
    val syncState: StateFlow<SyncState> = _syncState

    /**
     * Dispara la sincronización inicial de datos maestros.
     */
    fun startSync(negocioId: Int) {
        viewModelScope.launch {
            _syncState.value = SyncState.Loading
            
            val result = syncRepository.syncSnapshot(negocioId)
            
            _syncState.value = result.fold(
                onSuccess = { SyncState.Success("Sincronización completa.") },
                onFailure = { SyncState.Error(it.message ?: "Error desconocido") }
            )
        }
    }
    
    fun resetState() {
        _syncState.value = SyncState.Idle
    }
}
