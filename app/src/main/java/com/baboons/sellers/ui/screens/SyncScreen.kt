package com.baboons.sellers.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.baboons.sellers.viewmodel.SyncState
import com.baboons.sellers.viewmodel.SyncViewModel
import com.baboons.sellers.ui.theme.*

@Composable
fun SyncScreen(
    negocioId: Int,
    onSyncComplete: () -> Unit,
    viewModel: SyncViewModel = hiltViewModel()
) {
    val state by viewModel.syncState.collectAsState()

    // Disparar la sincronización al entrar
    LaunchedEffect(negocioId) {
        viewModel.startSync(negocioId)
    }

    // Si tuvo éxito, navegamos al Home
    LaunchedEffect(state) {
        if (state is SyncState.Success) {
            onSyncComplete()
        }
    }

    Scaffold(
        containerColor = BaboonsBackground
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(24.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            when (state) {
                is SyncState.Loading -> {
                    CircularProgressIndicator(modifier = Modifier.size(64.dp))
                    Spacer(modifier = Modifier.height(24.dp))
                    Text(
                        text = "Sincronizando Datos...",
                        style = MaterialTheme.typography.headlineSmall
                    )
                    Text(
                        text = "Descargando catálogo y clientes para trabajar offline.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                is SyncState.Error -> {
                    Text(
                        text = "Error al Sincronizar",
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.headlineSmall
                    )
                    Text(text = (state as SyncState.Error).message)
                    Spacer(modifier = Modifier.height(16.dp))
                    Button(onClick = { viewModel.startSync(negocioId) }) {
                        Text("Reintentar")
                    }
                }
                else -> { /* Iniciando... */ }
            }
        }
    }
}
