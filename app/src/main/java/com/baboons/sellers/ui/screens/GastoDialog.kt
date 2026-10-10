package com.baboons.sellers.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.baboons.sellers.ui.theme.BaboonsError
import com.baboons.sellers.ui.theme.BaboonsTextSecondary
import com.baboons.sellers.ui.theme.BaboonsWarning

/**
 * Modal de gasto operativo en ruta (peaje, combustible, casilla, etc.).
 * Vive dentro del Home (mismo VM con la ruta seleccionada).
 * Pega en POST /hojas-ruta/{id}/gastos -> gastos_operativos del ERP.
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun GastoDialog(
    onRegistrar: (monto: Double, categoria: String, obs: String, (Boolean, String) -> Unit) -> Unit,
    onDismiss: () -> Unit
) {
    var monto by remember { mutableStateOf("") }
    var categoria by remember { mutableStateOf("") }
    var obs by remember { mutableStateOf("") }
    var mensaje by remember { mutableStateOf<String?>(null) }
    var enviando by remember { mutableStateOf(false) }

    // Mismas categorías que la vista web del chofer
    val categorias = listOf(
        "Casilla Bromatológica LP",
        "Casilla Bromatológica Juana Koslay",
        "Combustible (Emergencia)",
        "Peajes",
        "Colaciones / Comidas",
        "Repuestos / Mecánico",
        "Varios (Detallar en obs.)"
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Registrar Gasto en Ruta", fontWeight = FontWeight.Bold) },
        text = {
            Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                OutlinedTextField(
                    value = monto,
                    onValueChange = { monto = it.filter { c -> c.isDigit() || c == '.' || c == ',' } },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Monto") },
                    prefix = { Text("$") },
                    singleLine = true
                )

                Spacer(Modifier.height(14.dp))
                Text("CATEGORÍA", color = BaboonsTextSecondary, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                Spacer(Modifier.height(6.dp))
                FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    categorias.forEach { cat ->
                        FilterChip(
                            selected = categoria == cat,
                            onClick = { categoria = cat },
                            label = { Text(cat, fontSize = 11.sp) }
                        )
                    }
                }

                Spacer(Modifier.height(14.dp))
                OutlinedTextField(
                    value = obs,
                    onValueChange = { obs = it },
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = { Text("Observaciones: ej. Ticket Nro 1234, o detalle del gasto...") },
                    minLines = 2
                )

                Spacer(Modifier.height(8.dp))
                Text(
                    "Recordá pedir y guardar el comprobante para entregar en la rendición de caja.",
                    fontSize = 11.sp,
                    color = BaboonsTextSecondary
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
                    val v = monto.replace(",", ".").toDoubleOrNull()
                    if (v == null || v <= 0) {
                        mensaje = "Ingresá un monto válido"
                        return@Button
                    }
                    enviando = true
                    onRegistrar(v, categoria, obs.trim()) { ok, msg ->
                        enviando = false
                        if (ok) onDismiss() else mensaje = msg
                    }
                },
                enabled = categoria.isNotBlank() && !enviando,
                colors = ButtonDefaults.buttonColors(containerColor = BaboonsWarning)
            ) {
                Text(if (enviando) "Guardando..." else "Guardar Gasto", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancelar") }
        }
    )
}
