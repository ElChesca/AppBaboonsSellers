package com.baboons.sellers.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import com.baboons.sellers.data.local.entities.PedidoPendienteEntity
import com.baboons.sellers.ui.theme.*
import com.baboons.sellers.viewmodel.PedidosPendientesViewModel
import com.baboons.sellers.workers.SyncWorker
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PedidosPendientesScreen(
    onBack: () -> Unit,
    viewModel: PedidosPendientesViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val pedidos by viewModel.pedidos.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Mis Ventas y Sincronizacion", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Volver")
                    }
                },
                actions = {
                    IconButton(onClick = {
                        android.widget.Toast.makeText(context, "Verificando...", android.widget.Toast.LENGTH_SHORT).show()
                        viewModel.sincronizarManualmente(context) { mensaje ->
                            android.widget.Toast.makeText(context, mensaje, android.widget.Toast.LENGTH_LONG).show()
                        }
                    }) {
                        Icon(Icons.Default.Sync, contentDescription = "Sincronizar", tint = Color(0xFF3B82F6))
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color(0xFF1E293B),
                    titleContentColor = Color.White,
                    navigationIconContentColor = Color(0xFF94A3B8)
                )
            )
        },
        containerColor = Color(0xFF0F172A)
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            
            // Header Stats
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
                shape = RoundedCornerShape(12.dp)
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceAround,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val pendientes = pedidos.count { it.estado_sync == "pendiente" }
                    val enviados = pedidos.count { it.estado_sync == "enviado" }
                    val totalVendido = pedidos.sumOf { it.total }
                    
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(pendientes.toString(), fontSize = 24.sp, fontWeight = FontWeight.Bold, color = Color(0xFFF59E0B))
                        Text("Por Enviar", color = Color(0xFF94A3B8), fontSize = 12.sp)
                    }
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(enviados.toString(), fontSize = 24.sp, fontWeight = FontWeight.Bold, color = Color(0xFF10B981))
                        Text("Enviados", color = Color(0xFF94A3B8), fontSize = 12.sp)
                    }
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("$" + String.format("%,.0f", totalVendido), fontSize = 20.sp, fontWeight = FontWeight.Bold, color = Color(0xFF3B82F6))
                        Text("Total Vendido", color = Color(0xFF94A3B8), fontSize = 12.sp)
                    }
                }
            }
            
            if (pedidos.isEmpty()) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("Aun no has creado ningun pedido este turno.", color = Color(0xFF64748B))
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(pedidos) { pedido ->
                        PedidoOfflineItem(pedido)
                    }
                }
            }
        }
    }
}

@Composable
fun PedidoOfflineItem(pedido: PedidoPendienteEntity) {
    val date = Date(pedido.creado_en)
    val formattedDate = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault()).format(date)
    val isEnviado = pedido.estado_sync == "enviado"
    
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.Top) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(pedido.cliente_nombre, fontWeight = FontWeight.Bold, fontSize = 16.sp, color = Color.White)
                    Text(formattedDate, fontSize = 12.sp, color = Color(0xFF94A3B8))
                }
                
                // Badge Estado
                Row(
                    modifier = Modifier
                        .background(
                            if (isEnviado) Color(0xFF064E3B) else Color(0xFF78350F),
                            shape = RoundedCornerShape(16.dp)
                        )
                        .padding(horizontal = 8.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        if (isEnviado) Icons.Default.CheckCircle else Icons.Default.ErrorOutline, 
                        contentDescription = null, 
                        tint = if (isEnviado) Color(0xFF10B981) else Color(0xFFF59E0B),
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = if (isEnviado) "Enviado" else "Pendiente",
                        color = if (isEnviado) Color(0xFF34D399) else Color(0xFFFBBF24),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(12.dp))
            HorizontalDivider(color = Color(0xFF334155), thickness = 0.5.dp)
            Spacer(modifier = Modifier.height(12.dp))
            
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(if (pedido.intentos_sync > 0) "Intentos de red: ${pedido.intentos_sync}" else "Listo para enviar", color = Color(0xFF64748B), fontSize = 12.sp)
                Text("Total: $${String.format("%,.0f", pedido.total)}", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
            }
        }
    }
}
