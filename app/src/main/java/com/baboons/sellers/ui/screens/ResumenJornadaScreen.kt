package com.baboons.sellers.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.baboons.sellers.data.local.entities.*
import com.baboons.sellers.ui.theme.*

/**
 * Resumen de Jornada: cierre del dia del chofer — visitas, pedidos y cobros.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ResumenJornadaScreen(
    clientes: List<ClienteEntity>,
    hojaRutaClientes: List<HojaRutaClienteEntity>,
    pedidos: List<PedidoPendienteEntity>,
    cobros: List<CobroLocalEntity>,
    hojasRuta: List<HojaRutaEntity>,
    hojaRutaId: Int?,
    onBack: () -> Unit
) {
    val itemsHr = hojaRutaClientes.filter { it.hoja_ruta_id == hojaRutaId }
    val visitados = itemsHr.filter { it.visitado }
    val pedidosHr = pedidos.filter { it.hoja_ruta_id == hojaRutaId }
    val tipoHoja = etiquetaTipoHoja(hojasRuta.find { it.id == hojaRutaId }?.tipo_ruta)
    val cobrosHr = cobros.filter { it.hoja_ruta_id == hojaRutaId }
    val totalPedidos = pedidosHr.sumOf { it.total }
    val totalCobrado = cobrosHr.sumOf { it.monto }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Resumen de Jornada", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, "Volver") }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = BaboonsBlue,
                    titleContentColor = Color.White,
                    navigationIconContentColor = Color.White
                )
            )
        }
    ) { padding ->
        LazyColumn(modifier = Modifier.fillMaxSize().padding(padding).padding(16.dp)) {
            // KPIs
            item {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                    KpiCard("Visitados", "${visitados.size}/${itemsHr.size}", BaboonsBlue, Modifier.weight(1f))
                    KpiCard("Pedidos", "${pedidosHr.size}", BaboonsBlue, Modifier.weight(1f))
                }
                Spacer(Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                    KpiCard("Total Pedidos", "$${String.format("%,.0f", totalPedidos)}", BaboonsBlue, Modifier.weight(1f))
                    KpiCard("Cobrado", "$${String.format("%,.0f", totalCobrado)}", BaboonsSuccess, Modifier.weight(1f))
                }
                Spacer(Modifier.height(20.dp))
            }

            // Visitas
            item { SeccionTitulo("VISITAS (${visitados.size})") }
            if (visitados.isEmpty()) {
                item { Text("Sin visitas registradas todavía.", color = BaboonsTextSecondary, style = MaterialTheme.typography.bodySmall) }
            }
            items(visitados) { v ->
                val nombre = clientes.firstOrNull { it.id == v.cliente_id }?.nombre ?: "Cliente #${v.cliente_id}"
                Card(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 3.dp),
                    shape = RoundedCornerShape(10.dp),
                    colors = CardDefaults.cardColors(containerColor = BaboonsSurface)
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text(nombre, fontWeight = FontWeight.Bold)
                        v.resultado_visita?.let { Text(it, color = BaboonsWarning, style = MaterialTheme.typography.bodySmall) }
                    }
                }
            }

            // Pedidos
            item { SeccionTitulo("PEDIDOS · $tipoHoja (${pedidosHr.size})") }
            if (pedidosHr.isEmpty()) {
                item { Text("Sin pedidos en esta ruta.", color = BaboonsTextSecondary, style = MaterialTheme.typography.bodySmall) }
            }
            items(pedidosHr) { p ->
                val tipo = etiquetaTipoHoja(hojasRuta.find { it.id == p.hoja_ruta_id }?.tipo_ruta)
                Card(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 3.dp),
                    shape = RoundedCornerShape(10.dp),
                    colors = CardDefaults.cardColors(containerColor = BaboonsSurface)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                p.cliente_nombre,
                                fontWeight = FontWeight.Bold,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                tipo,
                                style = MaterialTheme.typography.labelSmall,
                                color = if (tipo == "Auto-Venta") BaboonsWarning else BaboonsBlue,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                p.estado_sync,
                                style = MaterialTheme.typography.labelSmall,
                                color = if (p.estado_sync == "pendiente") BaboonsError else BaboonsSuccess
                            )
                        }
                        Text(
                            "$${String.format("%,.0f", p.total)}",
                            fontWeight = FontWeight.Bold,
                            color = BaboonsBlue,
                            softWrap = false,
                            modifier = Modifier.padding(start = 12.dp)
                        )
                    }
                }
            }

            // Cobros
            item { SeccionTitulo("COBROS (${cobrosHr.size})") }
            if (cobrosHr.isEmpty()) {
                item { Text("Sin cobros registrados.", color = BaboonsTextSecondary, style = MaterialTheme.typography.bodySmall) }
            }
            items(cobrosHr) { c ->
                Card(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 3.dp),
                    shape = RoundedCornerShape(10.dp),
                    colors = CardDefaults.cardColors(containerColor = BaboonsSurface)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(c.cliente_nombre, fontWeight = FontWeight.Bold, maxLines = 2, overflow = TextOverflow.Ellipsis)
                            Text(c.metodo_pago, style = MaterialTheme.typography.labelSmall, color = BaboonsTextSecondary)
                        }
                        Text(
                            "$${String.format("%,.0f", c.monto)}",
                            fontWeight = FontWeight.Bold,
                            color = BaboonsSuccess,
                            softWrap = false,
                            modifier = Modifier.padding(start = 12.dp)
                        )
                    }
                }
            }
            item { Spacer(Modifier.height(24.dp)) }
        }
    }
}

@Composable
private fun KpiCard(titulo: String, valor: String, color: Color, modifier: Modifier = Modifier) {
    Card(modifier = modifier, colors = CardDefaults.cardColors(containerColor = BaboonsSurface), shape = RoundedCornerShape(12.dp)) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(valor, fontWeight = FontWeight.ExtraBold, color = color, style = MaterialTheme.typography.headlineSmall)
            Text(titulo, style = MaterialTheme.typography.labelSmall, color = BaboonsTextSecondary)
        }
    }
}

@Composable
private fun SeccionTitulo(texto: String) {
    Text(texto, color = BaboonsTextSecondary, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelMedium, modifier = Modifier.padding(top = 16.dp, bottom = 8.dp))
}
