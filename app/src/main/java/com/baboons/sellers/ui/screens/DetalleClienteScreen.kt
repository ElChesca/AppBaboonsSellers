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
import com.baboons.sellers.data.local.entities.PedidoPendienteEntity
import com.baboons.sellers.ui.theme.*
import org.json.JSONArray
import org.json.JSONObject

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun DetalleClienteScreen(
    clienteId: Int,
    clientes: List<ClienteEntity>,
    onCrearPedido: (Int) -> Unit,
    onRegistrarCobro: (Double, String, (Boolean, String) -> Unit) -> Unit = { _, _, cb -> cb(false, "") },
    onMarcarResultado: (String, String, (Boolean, String) -> Unit) -> Unit = { _, _, cb -> cb(false, "") },
    esAutoVenta: Boolean = false,
    pedidosCliente: List<PedidoPendienteEntity> = emptyList(),
    onRegistrarRebotes: (pedidoId: Int, items: List<ReboteMarcado>, (Boolean, String) -> Unit) -> Unit = { _, _, cb -> cb(false, "") },
    onCobrado: () -> Unit = {},
    onBack: () -> Unit
) {
    val cliente = clientes.find { it.id == clienteId } ?: return
    var mostrarCobro by remember { mutableStateOf(false) }
    var montoCobro by remember { mutableStateOf("") }
    var rechazos by remember { mutableStateOf(mapOf<String, Int>()) }
    var motivosRechazo by remember { mutableStateOf(mapOf<String, String>()) }
    var resultado by remember { mutableStateOf("") }
    var obs by remember { mutableStateOf("") }
    var msg by remember { mutableStateOf<String?>(null) }
    var msgError by remember { mutableStateOf(false) }

    if (mostrarCobro) {
        CobroDialog(
            montoInicial = montoCobro,
            onDismiss = { mostrarCobro = false },
            onConfirm = { monto, metodo ->
                onRegistrarCobro(monto, metodo) { ok, m ->
                    if (ok) onCobrado()
                    else { msg = m; msgError = true; mostrarCobro = false }
                }
            }
        )
    }

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
            if (!esAutoVenta) return@Scaffold
            Surface(
                modifier = Modifier.fillMaxWidth(),
                tonalElevation = 8.dp,
                shadowElevation = 16.dp,
                color = BaboonsSurface
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth().navigationBarsPadding().padding(16.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    if (esAutoVenta) {
                        Button(
                            onClick = { onCrearPedido(clienteId) },
                            modifier = Modifier.weight(1f).height(56.dp),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = BaboonsBlue)
                        ) {
                            Icon(Icons.Default.Add, contentDescription = null)
                            Spacer(Modifier.width(6.dp))
                            Text("VENDER", fontWeight = FontWeight.Bold)
                        }
                    }
                    Button(
                        onClick = { mostrarCobro = true },
                        modifier = Modifier.weight(1f).height(56.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = BaboonsSuccess)
                    ) {
                        Icon(Icons.Default.AttachMoney, contentDescription = null)
                        Spacer(Modifier.width(6.dp))
                        Text("COBRAR", fontWeight = FontWeight.Bold)
                    }
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
            item {
                if (!esAutoVenta) {
                    val direccion = cliente.direccion?.takeIf { it.isNotBlank() }
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = BaboonsSurface),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp)) {
                            Text(cliente.nombre, fontWeight = FontWeight.Bold, maxLines = 2)
                            Text(
                                listOfNotNull(direccion, "Saldo $${String.format("%,.0f", cliente.saldo_ctacte)}").joinToString(" · "),
                                color = BaboonsTextSecondary,
                                style = MaterialTheme.typography.bodySmall,
                                maxLines = 2
                            )
                        }
                    }
                } else {
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
            }

            if (esAutoVenta) item { Spacer(Modifier.height(16.dp)) }

            if (esAutoVenta) item {
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

            if (!esAutoVenta && pedidosCliente.isEmpty()) {
                item {
                    Spacer(Modifier.height(16.dp))
                    Text("Este cliente no tiene un pedido en la hoja.", color = BaboonsTextSecondary, style = MaterialTheme.typography.bodySmall)
                }
            }
            if (!esAutoVenta) {
                items(pedidosCliente, key = { it.uuid_local }) { pedido ->
                    PedidoDescargaCard(
                        pedido = pedido,
                        rechazos = rechazos,
                        motivos = motivosRechazo,
                        onRechazo = { clave, cant -> rechazos = rechazos + (clave to cant) },
                        onMotivo = { clave, motivo -> motivosRechazo = motivosRechazo + (clave to motivo) },
                        onCobrar = { neto, marcados, pedidoId ->
                            val texto = if (neto % 1.0 == 0.0) neto.toLong().toString() else String.format("%.2f", neto)
                            if (marcados.isEmpty()) {
                                montoCobro = texto
                                mostrarCobro = true
                            } else if (pedidoId == null) {
                                msg = "Sincroniza para poder guardar el rebote."
                                msgError = true
                            } else {
                                onRegistrarRebotes(pedidoId, marcados) { ok, m ->
                                    if (ok) {
                                        montoCobro = texto
                                        mostrarCobro = true
                                    } else {
                                        msg = m
                                        msgError = true
                                    }
                                }
                            }
                        }
                    )
                }
            }

            if (!esAutoVenta) item {
                Spacer(Modifier.height(24.dp))
                Text("RESULTADO DE LA VISITA", color = BaboonsTextSecondary, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                Spacer(Modifier.height(8.dp))
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = BaboonsSurface),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            listOf("Entregado", "No estaba", "Parcial", "Rechazado").forEach { r ->
                                FilterChip(
                                    selected = resultado == r,
                                    onClick = { resultado = r },
                                    label = { Text(r, fontSize = 12.sp) }
                                )
                            }
                        }
                        Spacer(Modifier.height(8.dp))
                        OutlinedTextField(
                            value = obs,
                            onValueChange = { obs = it },
                            modifier = Modifier.fillMaxWidth(),
                            placeholder = { Text("Observación (opcional)") },
                            singleLine = true
                        )
                        Spacer(Modifier.height(8.dp))
                        Button(
                            onClick = {
                                onMarcarResultado(resultado, obs) { ok, m ->
                                    if (ok) { resultado = ""; obs = ""; onBack() }
                                    else { msg = m; msgError = true }
                                }
                            },
                            enabled = resultado.isNotBlank(),
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.buttonColors(containerColor = BaboonsBlue)
                        ) { Text("Guardar resultado", fontWeight = FontWeight.Bold) }
                    }
                }
            }

            msg?.let {
                item {
                    Spacer(Modifier.height(12.dp))
                    Surface(
                        color = if (msgError) BaboonsError.copy(alpha = 0.12f) else BaboonsSuccess.copy(alpha = 0.12f),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(it, modifier = Modifier.padding(12.dp), color = if (msgError) BaboonsError else BaboonsSuccess, fontWeight = FontWeight.Medium)
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
fun CobroDialog(montoInicial: String = "", onDismiss: () -> Unit, onConfirm: (Double, String) -> Unit) {
    var monto by remember { mutableStateOf(montoInicial) }
    var metodo by remember { mutableStateOf("Efectivo") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Registrar Cobro", fontWeight = FontWeight.Bold) },
        text = {
            Column {
                OutlinedTextField(
                    value = monto,
                    onValueChange = { monto = it.filter { c -> c.isDigit() || c == '.' || c == ',' } },
                    label = { Text("Monto") },
                    prefix = { Text("$") },
                    singleLine = true
                )
                Spacer(Modifier.height(12.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf("Efectivo", "Transferencia").forEach { m ->
                        FilterChip(selected = metodo == m, onClick = { metodo = m }, label = { Text(m) })
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val v = monto.replace(",", ".").toDoubleOrNull()
                    if (v != null && v > 0) onConfirm(v, metodo)
                },
                colors = ButtonDefaults.buttonColors(containerColor = BaboonsSuccess)
            ) { Text("Confirmar cobro") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancelar") } }
    )
}

data class ReboteMarcado(val productoId: Int, val cantidad: Double, val motivo: String)

private data class ItemCarga(val productoId: Int, val nombre: String, val cantidad: Double, val precio: Double)

private val motivosRebote = listOf(
    "Rechazo Parcial", "Rotura / Merma", "Local Cerrado", "No tiene Dinero", "Error de Carga / Faltante", "Otro"
)

private fun claveRechazo(uuid: String, productoId: Int) = "$uuid:$productoId"

private fun serverPedidoId(uuid: String): Int? =
    if (uuid.startsWith("server-")) uuid.removePrefix("server-").toIntOrNull() else null

private fun parseItemsCarga(json: String): List<ItemCarga> {
    return try {
        val arr = JSONArray(json)
        (0 until arr.length()).mapNotNull { i ->
            val o = arr.optJSONObject(i) ?: return@mapNotNull null
            val nombre = listOf("nombre", "producto_nombre", "descripcion")
                .firstNotNullOfOrNull { key -> o.optString(key).takeIf { it.isNotBlank() } }
                ?: "Item"
            val cantidad = o.optDouble("cantidad", 0.0)
            val precio = when {
                o.has("precio_unitario") -> o.optDouble("precio_unitario", 0.0)
                cantidad > 0 && o.has("subtotal") -> o.optDouble("subtotal", 0.0) / cantidad
                else -> 0.0
            }
            ItemCarga(o.optInt("producto_id", 0), nombre, cantidad, precio)
        }
    } catch (_: Exception) {
        emptyList()
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PedidoDescargaCard(
    pedido: PedidoPendienteEntity,
    rechazos: Map<String, Int>,
    motivos: Map<String, String>,
    onRechazo: (String, Int) -> Unit,
    onMotivo: (String, String) -> Unit,
    onCobrar: (Double, List<ReboteMarcado>, Int?) -> Unit
) {
    val items = remember(pedido.items_json) { parseItemsCarga(pedido.items_json) }
    val pedidoId = serverPedidoId(pedido.uuid_local)
    var motivoAbierto by remember { mutableStateOf<String?>(null) }
    val lineas = items.map { item ->
        val clave = claveRechazo(pedido.uuid_local, item.productoId)
        val rechazado = (rechazos[clave] ?: 0).coerceIn(0, item.cantidad.toInt())
        val queda = (item.cantidad - rechazado).coerceAtLeast(0.0)
        Triple(item, clave, rechazado to (queda * item.precio))
    }
    val totalPedido = if (items.isEmpty()) pedido.total else items.sumOf { it.cantidad * it.precio }
    val totalRechazo = lineas.sumOf { (item, _, reb) -> reb.first * item.precio }
    val aCobrar = (totalPedido - totalRechazo).coerceAtLeast(0.0)
    val marcados = lineas.mapNotNull { (item, clave, reb) ->
        if (reb.first <= 0 || item.productoId == 0) null
        else ReboteMarcado(item.productoId, reb.first.toDouble(), motivos[clave] ?: motivosRebote.first())
    }

    Column(modifier = Modifier.padding(top = 12.dp)) {
        Text(
            "PEDIDO${pedidoId?.let { " #$it" } ?: ""}",
            color = BaboonsTextSecondary,
            fontWeight = FontWeight.Bold,
            fontSize = 12.sp
        )
        Spacer(Modifier.height(8.dp))
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = BaboonsSurface),
            shape = RoundedCornerShape(12.dp)
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                if (items.isEmpty()) {
                    Text(
                        "Sincroniza de nuevo para bajar los items de este pedido.",
                        color = BaboonsTextSecondary,
                        style = MaterialTheme.typography.bodySmall
                    )
                    Spacer(Modifier.height(8.dp))
                }
                lineas.forEach { (item, clave, reb) ->
                    val (rechazado, quedaImporte) = reb
                    val totalItem = item.cantidad * item.precio
                    Column(modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(item.nombre, fontWeight = FontWeight.Bold, maxLines = 2)
                                Text(
                                    "${item.cantidad.trimCantidad()} x $${String.format("%,.0f", item.precio)}",
                                    color = BaboonsTextSecondary,
                                    style = MaterialTheme.typography.bodySmall
                                )
                            }
                            Text(
                                "$${String.format("%,.0f", totalItem)}",
                                fontWeight = FontWeight.Bold,
                                color = BaboonsBlue,
                                softWrap = false
                            )
                        }
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 4.dp)) {
                            Text("Rebote", style = MaterialTheme.typography.labelSmall, color = BaboonsTextSecondary)
                            IconButton(onClick = { onRechazo(clave, (rechazado - 1).coerceAtLeast(0)) }, modifier = Modifier.size(32.dp)) {
                                Icon(Icons.Default.Remove, contentDescription = "Menos rebote", modifier = Modifier.size(16.dp))
                            }
                            Text(rechazado.toString(), fontWeight = FontWeight.Bold, modifier = Modifier.width(24.dp), textAlign = androidx.compose.ui.text.style.TextAlign.Center)
                            IconButton(
                                onClick = { onRechazo(clave, (rechazado + 1).coerceAtMost(item.cantidad.toInt())) },
                                modifier = Modifier.size(32.dp)
                            ) {
                                Icon(Icons.Default.Add, contentDescription = "Mas rebote", modifier = Modifier.size(16.dp))
                            }
                            if (rechazado > 0) {
                                TextButton(onClick = { motivoAbierto = clave }) {
                                    Text(motivos[clave] ?: motivosRebote.first(), fontSize = 11.sp, maxLines = 1)
                                }
                                DropdownMenu(expanded = motivoAbierto == clave, onDismissRequest = { motivoAbierto = null }) {
                                    motivosRebote.forEach { motivo ->
                                        DropdownMenuItem(
                                            text = { Text(motivo) },
                                            onClick = { onMotivo(clave, motivo); motivoAbierto = null }
                                        )
                                    }
                                }
                            }
                        }
                        if (rechazado > 0) {
                            Text(
                                "Queda $${String.format("%,.0f", quedaImporte)}",
                                color = BaboonsWarning,
                                style = MaterialTheme.typography.labelSmall
                            )
                        }
                    }
                    HorizontalDivider(color = BaboonsBorder, thickness = 0.5.dp)
                }
                Spacer(Modifier.height(8.dp))
                FilaTotal("Total del pedido", totalPedido, BaboonsTextPrimary)
                if (totalRechazo > 0) FilaTotal("Rebotes", -totalRechazo, BaboonsWarning)
                FilaTotal("A cobrar", aCobrar, BaboonsBlue)
                Spacer(Modifier.height(8.dp))
                Button(
                    onClick = { onCobrar(aCobrar, marcados, pedidoId) },
                    enabled = items.isNotEmpty(),
                    modifier = Modifier.fillMaxWidth().height(48.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = BaboonsSuccess)
                ) {
                    Icon(Icons.Default.AttachMoney, contentDescription = null)
                    Spacer(Modifier.width(6.dp))
                    Text("COBRAR $${String.format("%,.0f", aCobrar)}", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
private fun FilaTotal(label: String, monto: Double, color: Color) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label, modifier = Modifier.weight(1f), color = BaboonsTextSecondary, style = MaterialTheme.typography.bodySmall)
        Text(
            "$${String.format("%,.0f", monto)}",
            fontWeight = FontWeight.Bold,
            color = color,
            softWrap = false
        )
    }
}

private fun Double.trimCantidad(): String =
    if (this % 1.0 == 0.0) toInt().toString() else String.format("%.2f", this)

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
