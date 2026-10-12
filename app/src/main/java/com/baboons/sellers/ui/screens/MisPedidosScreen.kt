package com.baboons.sellers.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.baboons.sellers.data.local.entities.PedidoPendienteEntity
import com.baboons.sellers.ui.theme.*
import com.baboons.sellers.viewmodel.MisPedidosViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MisPedidosScreen(
    onBack: () -> Unit,
    viewModel: MisPedidosViewModel = hiltViewModel()
) {
    val pedidos by viewModel.filteredPedidos.collectAsState()
    val hojasRuta by viewModel.hojasRuta.collectAsState()
    val selectedHrId by viewModel.selectedHrId.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()

    Scaffold(
        containerColor = BaboonsBackground,
        topBar = {
            TopAppBar(
                title = { Text("Mis Pedidos", fontWeight = FontWeight.Bold) },
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
        Column(modifier = Modifier.padding(padding).fillMaxSize().padding(horizontal = 16.dp)) {
            
            // Auto-seleccionar la primera ruta si no hay ninguna seleccionada
            LaunchedEffect(hojasRuta) {
                if (selectedHrId == null && hojasRuta.isNotEmpty()) {
                    viewModel.setSelectedHr(hojasRuta.first().id)
                }
            }
            
            // Selector de Hoja de Ruta
            var hrExpanded by remember { mutableStateOf(false) }
            val currentHrName = hojasRuta.find { it.id == selectedHrId }?.let { "Ruta #${it.id}" } ?: "Seleccionar Ruta"

            Box(modifier = Modifier.fillMaxWidth().padding(top = 16.dp)) {
                Surface(
                    modifier = Modifier.fillMaxWidth().clickable { hrExpanded = true },
                    shape = RoundedCornerShape(12.dp),
                    color = BaboonsSurface,
                    border = BorderStroke(1.dp, BaboonsBorder)
                ) {
                    Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Place, null, tint = BaboonsBlue)
                        Spacer(Modifier.width(12.dp))
                        Text(currentHrName, fontWeight = FontWeight.Bold, color = BaboonsTextPrimary, modifier = Modifier.weight(1f))
                        Icon(Icons.Default.ArrowDropDown, null, tint = BaboonsTextSecondary)
                    }
                }
                DropdownMenu(expanded = hrExpanded, onDismissRequest = { hrExpanded = false }, modifier = Modifier.fillMaxWidth(0.9f)) {
                    hojasRuta.forEach { hr ->
                        DropdownMenuItem(text = { Text("Ruta #${hr.id}") }, onClick = { viewModel.setSelectedHr(hr.id); hrExpanded = false })
                    }
                }
            }

            // Buscador por cliente
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { viewModel.setSearchQuery(it) },
                modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp),
                placeholder = { Text("Filtrar por cliente...", color = BaboonsTextSecondary) },
                leadingIcon = { Icon(Icons.Default.Search, null, tint = BaboonsBlue) },
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = BaboonsBlue,
                    unfocusedBorderColor = BaboonsBorder,
                    focusedContainerColor = BaboonsSurface,
                    unfocusedContainerColor = BaboonsSurface
                )
            )

            if (pedidos.isEmpty()) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("No se encontraron pedidos", color = BaboonsTextSecondary)
                        if (selectedHrId != null || searchQuery.isNotEmpty()) {
                            TextButton(onClick = { viewModel.setSelectedHr(null); viewModel.setSearchQuery("") }) {
                                Text("Limpiar filtros", color = BaboonsBlue)
                            }
                        }
                    }
                }
            } else {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    contentPadding = PaddingValues(bottom = 24.dp)
                ) {
                    items(pedidos) { pedido ->
                        PedidoItemCard(
                            pedido,
                            tipo = etiquetaTipoHoja(hojasRuta.find { it.id == pedido.hoja_ruta_id }?.tipo_ruta)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun PedidoItemCard(pedido: PedidoPendienteEntity, tipo: String) {
    val date = Date(pedido.creado_en)
    val formattedDate = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault()).format(date)
    val isSynced = pedido.estado_sync != "pendiente"

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = BaboonsSurface),
        border = BorderStroke(0.5.dp, BaboonsBorder)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.Top) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(pedido.cliente_nombre, fontWeight = FontWeight.Bold, color = BaboonsTextPrimary, fontSize = 16.sp)
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.CalendarToday, null, modifier = Modifier.size(12.dp), tint = BaboonsTextSecondary)
                        Spacer(Modifier.width(4.dp))
                        Text(formattedDate, fontSize = 12.sp, color = BaboonsTextSecondary)
                    }
                }
                
                // Badge Estado
                Surface(
                    color = if (isSynced) BaboonsSuccess.copy(alpha = 0.1f) else BaboonsBlue.copy(alpha = 0.1f),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Row(modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            if (isSynced) Icons.Default.CloudDone else Icons.Default.CloudOff,
                            contentDescription = null,
                            tint = if (isSynced) BaboonsSuccess else BaboonsBlue,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(Modifier.width(4.dp))
                        Text(
                            text = if (isSynced) "EN WEB" else "CELULAR",
                            color = if (isSynced) BaboonsSuccess else BaboonsBlue,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(12.dp))
            HorizontalDivider(color = BaboonsBorder, thickness = 0.5.dp)
            Spacer(modifier = Modifier.height(12.dp))
            
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.Bottom) {
                Column {
                    if (pedido.hoja_ruta_id != null) {
                        Text("Ruta #${pedido.hoja_ruta_id} · $tipo", color = BaboonsBlue, fontWeight = FontWeight.Medium, fontSize = 12.sp)
                    }
                    if (!pedido.observaciones.isNullOrBlank()) {
                        Text("Obs: ${pedido.observaciones}", style = MaterialTheme.typography.bodySmall, color = BaboonsTextSecondary)
                    }
                }
                Text("$" + String.format(Locale.getDefault(), "%,.0f", pedido.total), color = BaboonsTextPrimary, fontWeight = FontWeight.ExtraBold, fontSize = 18.sp)
            }
        }
    }
}
