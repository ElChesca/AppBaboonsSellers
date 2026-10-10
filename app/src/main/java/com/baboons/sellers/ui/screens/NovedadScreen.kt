package com.baboons.sellers.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Store
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.baboons.sellers.data.local.entities.ClienteEntity
import com.baboons.sellers.ui.theme.BaboonsError
import com.baboons.sellers.ui.theme.BaboonsTextSecondary
import com.baboons.sellers.ui.theme.BaboonsWarning

/**
 * Modal de reporte de novedad de ruta (camión roto, cliente cerrado, etc.).
 * Vive dentro del Home: usa la misma instancia del ViewModel que tiene
 * la hoja de ruta seleccionada, así que nunca pierde el contexto.
 * Pega en POST /hojas-ruta/{id}/novedades -> notificación + tab Novedades en la web.
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun NovedadDialog(
    clientesRuta: List<ClienteEntity>,
    onReportar: (tipo: String, obs: String, clienteId: Int?, (Boolean, String) -> Unit) -> Unit,
    onDismiss: () -> Unit
) {
    var tipo by remember { mutableStateOf("") }
    var clienteId by remember { mutableStateOf<Int?>(null) }
    var obs by remember { mutableStateOf("") }
    var mensaje by remember { mutableStateOf<String?>(null) }
    var enviando by remember { mutableStateOf(false) }

    val tipos = listOf(
        "camion_roto" to "Camión roto",
        "cliente_cerrado" to "Cliente cerrado",
        "direccion_incorrecta" to "Dirección incorrecta",
        "demora" to "Demora",
        "otro" to "Otro"
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Reportar Novedad", fontWeight = FontWeight.Bold) },
        text = {
            Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                Text("¿QUÉ PASÓ?", color = BaboonsTextSecondary, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                Spacer(Modifier.height(6.dp))
                FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    tipos.forEach { (key, label) ->
                        FilterChip(
                            selected = tipo == key,
                            onClick = { tipo = key },
                            label = { Text(label, fontSize = 12.sp) }
                        )
                    }
                }

                Spacer(Modifier.height(14.dp))
                Text("CLIENTE (OPCIONAL)", color = BaboonsTextSecondary, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                Spacer(Modifier.height(6.dp))
                FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    FilterChip(
                        selected = clienteId == null,
                        onClick = { clienteId = null },
                        label = { Text("General (sin cliente)", fontSize = 12.sp) }
                    )
                    clientesRuta.forEach { c ->
                        FilterChip(
                            selected = clienteId == c.id,
                            onClick = { clienteId = c.id },
                            label = {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.Store, null, modifier = Modifier.size(14.dp))
                                    Spacer(Modifier.width(4.dp))
                                    Text(c.nombre, fontSize = 12.sp)
                                }
                            }
                        )
                    }
                }

                Spacer(Modifier.height(14.dp))
                OutlinedTextField(
                    value = obs,
                    onValueChange = { obs = it },
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = { Text("Observación (opcional): ej. pinchó una rueda en Ruta 22...") },
                    minLines = 2
                )

                mensaje?.let {
                    Spacer(Modifier.height(10.dp))
                    Surface(
                        color = BaboonsError.copy(alpha = 0.12f),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(it, modifier = Modifier.padding(10.dp), color = BaboonsError, fontWeight = FontWeight.Medium)
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    enviando = true
                    onReportar(tipo, obs.trim(), clienteId) { ok, msg ->
                        enviando = false
                        if (ok) onDismiss() else mensaje = msg
                    }
                },
                enabled = tipo.isNotBlank() && !enviando,
                colors = ButtonDefaults.buttonColors(containerColor = BaboonsWarning)
            ) {
                Text(if (enviando) "Enviando..." else "Reportar", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancelar") }
        }
    )
}
