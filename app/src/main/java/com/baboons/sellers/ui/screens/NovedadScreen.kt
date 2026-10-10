package com.baboons.sellers.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Store
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.baboons.sellers.data.local.entities.ClienteEntity
import com.baboons.sellers.ui.theme.BaboonsBlue
import com.baboons.sellers.ui.theme.BaboonsError
import com.baboons.sellers.ui.theme.BaboonsSuccess
import com.baboons.sellers.ui.theme.BaboonsSurface
import com.baboons.sellers.ui.theme.BaboonsTextSecondary
import com.baboons.sellers.ui.theme.BaboonsWarning

/**
 * Reporte de novedad de ruta (camión roto, cliente cerrado, etc.).
 * Pega en POST /hojas-ruta/{id}/novedades -> notificación + tab Novedades en la web.
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun NovedadScreen(
    clientesRuta: List<ClienteEntity>,
    onReportar: (tipo: String, obs: String, clienteId: Int?, (Boolean, String) -> Unit) -> Unit,
    onBack: () -> Unit
) {
    var tipo by remember { mutableStateOf("") }
    var clienteId by remember { mutableStateOf<Int?>(null) }
    var obs by remember { mutableStateOf("") }
    var mensaje by remember { mutableStateOf<String?>(null) }
    var esError by remember { mutableStateOf(false) }
    var enviando by remember { mutableStateOf(false) }

    val tipos = listOf(
        "camion_roto" to "Camión roto",
        "cliente_cerrado" to "Cliente cerrado",
        "direccion_incorrecta" to "Dirección incorrecta",
        "demora" to "Demora",
        "otro" to "Otro"
    )

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Reportar Novedad", fontWeight = FontWeight.Bold) },
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
        bottomBar = {
            Button(
                onClick = {
                    enviando = true
                    onReportar(tipo, obs.trim(), clienteId) { ok, msg ->
                        enviando = false
                        if (ok) onBack() else {
                            esError = true
                            mensaje = msg
                        }
                    }
                },
                enabled = tipo.isNotBlank() && !enviando,
                modifier = Modifier.fillMaxWidth().navigationBarsPadding().padding(16.dp),
                colors = ButtonDefaults.buttonColors(containerColor = BaboonsWarning)
            ) {
                Text(
                    if (enviando) "Enviando..." else "Reportar Novedad",
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                Text("¿QUÉ PASÓ?", color = BaboonsTextSecondary, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                Spacer(Modifier.height(8.dp))
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = BaboonsSurface),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    FlowRow(
                        modifier = Modifier.padding(16.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        tipos.forEach { (key, label) ->
                            FilterChip(
                                selected = tipo == key,
                                onClick = { tipo = key },
                                label = { Text(label, fontSize = 12.sp) }
                            )
                        }
                    }
                }
            }

            item {
                Text("CLIENTE (OPCIONAL)", color = BaboonsTextSecondary, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                Spacer(Modifier.height(8.dp))
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = BaboonsSurface),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    FlowRow(
                        modifier = Modifier.padding(16.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
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
                }
            }

            item {
                Text("OBSERVACIÓN (OPCIONAL)", color = BaboonsTextSecondary, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    value = obs,
                    onValueChange = { obs = it },
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = { Text("Ej: pinchó una rueda en Ruta 22...") },
                    minLines = 3
                )
            }

            mensaje?.let {
                item {
                    Surface(
                        color = if (esError) BaboonsError.copy(alpha = 0.12f) else BaboonsSuccess.copy(alpha = 0.12f),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            it,
                            modifier = Modifier.padding(12.dp),
                            color = if (esError) BaboonsError else BaboonsSuccess,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }
        }
    }
}
