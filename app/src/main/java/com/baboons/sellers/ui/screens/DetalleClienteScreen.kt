package com.baboons.sellers.ui.screens

import androidx.compose.foundation.background
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
import androidx.compose.ui.unit.*
import com.baboons.sellers.data.local.entities.ClienteEntity
import com.baboons.sellers.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DetalleClienteScreen(
    clienteId: Int,
    clientes: List<ClienteEntity>,
    onCrearPedido: (Int) -> Unit,
    onBack: () -> Unit
) {
    val cliente = clientes.find { it.id == clienteId } ?: return

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Detalle de Cliente", fontWeight = FontWeight.Bold) },
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
        },
        containerColor = BaboonsBackground,
        bottomBar = {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                tonalElevation = 8.dp,
                shadowElevation = 16.dp,
                color = BaboonsSurface
            ) {
                Button(
                    onClick = { onCrearPedido(clienteId) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                        .height(56.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = BaboonsBlue)
                ) {
                    Icon(Icons.Default.Add, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text("LEVANTAR PEDIDO", fontWeight = FontWeight.Bold)
                }
            }
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp)
        ) {
            // Card de Info Principal
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = BaboonsSurface),
                    shape = RoundedCornerShape(16.dp),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(cliente.nombre, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                        Spacer(Modifier.height(8.dp))
                        
                        InfoRow(icon = Icons.Default.LocationOn, label = "Dirección", value = cliente.direccion ?: "N/A")
                        InfoRow(icon = Icons.Default.Phone, label = "Teléfono", value = cliente.telefono ?: "N/A")
                        InfoRow(icon = Icons.Default.Email, label = "Email", value = cliente.email ?: "N/A")
                    }
                }
            }

            item { Spacer(Modifier.height(16.dp)) }

            // Card de Estado Financiero
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = BaboonsSurface),
                    shape = RoundedCornerShape(16.dp),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("Estado de Cuenta", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        Spacer(Modifier.height(12.dp))
                        
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Saldo Actual", color = BaboonsTextSecondary)
                            val saldo = cliente.saldo_ctacte
                            Text(
                                text = "$${String.format("%,.0f", saldo)}",
                                color = if (saldo > 0) BaboonsError else if (saldo < 0) BaboonsSuccess else BaboonsTextPrimary,
                                style = MaterialTheme.typography.headlineMedium,
                                fontWeight = FontWeight.ExtraBold
                            )
                        }

                        if (cliente.ultimo_pago_monto != null) {
                            Spacer(Modifier.height(16.dp))
                            Divider(color = BaboonsBackground)
                            Spacer(Modifier.height(16.dp))
                            
                            Text("Último Pago del Chofer", style = MaterialTheme.typography.labelMedium, color = BaboonsSuccess, fontWeight = FontWeight.Bold)
                            Spacer(Modifier.height(4.dp))
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.CheckCircle, null, tint = BaboonsSuccess, modifier = Modifier.size(16.dp))
                                Spacer(Modifier.width(8.dp))
                                Text(
                                    "$${String.format("%,.0f", cliente.ultimo_pago_monto)}",
                                    fontWeight = FontWeight.Bold,
                                    color = BaboonsTextPrimary
                                )
                                Spacer(Modifier.width(8.dp))
                                Text("(${cliente.ultimo_pago_fecha ?: "N/A"})", color = BaboonsTextSecondary, fontSize = 12.sp)
                            }
                            Text(
                                "Medio: ${cliente.ultimo_pago_medio ?: "No especificado"}",
                                style = MaterialTheme.typography.bodySmall,
                                color = BaboonsTextSecondary,
                                modifier = Modifier.padding(start = 24.dp)
                            )
                        }
                    }
                }
            }

            item {
                Spacer(Modifier.height(24.dp))
                Text("HISTORIAL RECIENTE", color = BaboonsTextSecondary, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                Spacer(Modifier.height(8.dp))
            }
            
            // Aquí iría el historial de pedidos reales. Por ahora mostramos placeholder
            item {
                Text("Sincroniza para ver el historial de pedidos...", color = BaboonsTextSecondary, style = MaterialTheme.typography.bodySmall)
            }
        }
    }
}

@Composable
fun InfoRow(icon: androidx.compose.ui.graphics.vector.ImageVector, label: String, value: String) {
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(vertical = 4.dp)) {
        Icon(icon, null, tint = BaboonsBlue, modifier = Modifier.size(20.dp))
        Spacer(Modifier.width(12.dp))
        Column {
            Text(label, fontSize = 10.sp, color = BaboonsTextSecondary)
            Text(value, fontSize = 14.sp, color = BaboonsTextPrimary)
        }
    }
}
