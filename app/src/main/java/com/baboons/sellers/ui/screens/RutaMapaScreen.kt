package com.baboons.sellers.ui.screens

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.platform.LocalContext
import com.baboons.sellers.data.local.entities.ClienteEntity
import com.baboons.sellers.data.remote.RetrofitClient
import com.baboons.sellers.ui.theme.BaboonsBlue
import com.baboons.sellers.util.TokenStore
import kotlinx.coroutines.delay
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.LatLngBounds
import com.google.maps.android.compose.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RutaMapaScreen(
    clientes: List<ClienteEntity>,
    onSelectCliente: (Int) -> Unit,
    onBack: () -> Unit
) {
    val cameraPositionState = rememberCameraPositionState()
    val context = LocalContext.current
    var rastro by remember { mutableStateOf<List<LatLng>>(emptyList()) }

    // ✨ Rastro del chofer: trae la traza del día y la refresca cada 60s
    LaunchedEffect(Unit) {
        val api = RetrofitClient.getApi { TokenStore.getToken(context) }
        val negocioId = TokenStore.getNegocioId(context)
        while (true) {
            try {
                val resp = api.getMiRastro(negocioId)
                if (resp.isSuccessful) {
                    rastro = resp.body()?.map { LatLng(it.lat, it.lng) } ?: emptyList()
                }
            } catch (_: Exception) { }
            delay(60_000)
        }
    }

    // ✨ Auto-ajuste de cámara para mostrar todos los clientes de la ruta
    LaunchedEffect(clientes) {
        if (clientes.isNotEmpty()) {
            val validClientes = clientes.filter { it.latitud != null && it.longitud != null }
            if (validClientes.isNotEmpty()) {
                val builder = LatLngBounds.Builder()
                validClientes.forEach { 
                    builder.include(LatLng(it.latitud!!, it.longitud!!)) 
                }
                val bounds = builder.build()
                // Animamos la cámara para que encuadre todos los puntos con un margen de 150px
                cameraPositionState.animate(
                    CameraUpdateFactory.newLatLngBounds(bounds, 150),
                    1000
                )
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Mapa de Ruta", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Volver")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = BaboonsBlue,
                    titleContentColor = Color.White,
                    navigationIconContentColor = Color.White
                )
            )
        }
    ) { padding ->
        GoogleMap(
            modifier = Modifier.fillMaxSize().padding(padding),
            cameraPositionState = cameraPositionState,
            uiSettings = MapUiSettings(
                myLocationButtonEnabled = true, 
                zoomControlsEnabled = true,
                mapToolbarEnabled = true
            ),
            properties = MapProperties(isMyLocationEnabled = true)
        ) {
            if (rastro.size >= 2) {
                Polyline(points = rastro, color = Color(0xFFE67E22), width = 8f)
            }
            clientes.forEach { cliente ->
                if (cliente.latitud != null && cliente.longitud != null) {
                    Marker(
                        state = MarkerState(position = LatLng(cliente.latitud, cliente.longitud)),
                        title = cliente.nombre,
                        snippet = "Ver detalle del cliente",
                        onInfoWindowClick = { onSelectCliente(cliente.id) },
                        icon = BitmapDescriptorFactory.defaultMarker(
                            if (cliente.saldo_ctacte > 0) BitmapDescriptorFactory.HUE_RED 
                            else BitmapDescriptorFactory.HUE_BLUE
                        )
                    )
                }
            }
        }
    }
}
