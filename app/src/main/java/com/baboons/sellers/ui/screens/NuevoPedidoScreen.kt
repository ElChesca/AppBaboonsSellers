package com.baboons.sellers.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.*
import com.baboons.sellers.data.local.entities.PedidoPendienteEntity
import com.baboons.sellers.data.local.entities.ProductoEntity
import com.baboons.sellers.data.remote.*
import com.baboons.sellers.util.TokenStore
import com.baboons.sellers.ui.theme.*
import com.baboons.sellers.viewmodel.NuevoPedidoViewModel
import androidx.hilt.navigation.compose.hiltViewModel
import com.google.gson.Gson
import kotlinx.coroutines.launch
import java.util.UUID
import androidx.work.*

// ── Modelo de ítem en el carrito ─────────────────────────────────────────────
data class ItemCarrito(
    val productoId: Int,
    val nombre: String,
    val cantidad: Double,
    val precioUnitario: Double,
    val listaPrecioId: Int?,
    val nombreLista: String
) {
    val subtotal get() = cantidad * precioUnitario
}

// ── Pantalla Nuevo Pedido ─────────────────────────────────────────────────────
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NuevoPedidoScreen(
    clienteId: Int,
    hojaRutaId: Int?,
    onPedidoGuardado: () -> Unit,
    onBack: () -> Unit,
    viewModel: NuevoPedidoViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val api = remember { RetrofitClient.getApi { TokenStore.getToken(context) } }
    val negocioId = TokenStore.getNegocioId(context)
    val gson = remember { Gson() }

    // Estado de productos
    val productos by viewModel.productos.collectAsState()
    var productosFiltrados by remember { mutableStateOf<List<ProductoEntity>>(emptyList()) }
    var busqueda by remember { mutableStateOf("") }

    // Carrito
    var carrito by remember { mutableStateOf<List<ItemCarrito>>(emptyList()) }
    var showCarrito by remember { mutableStateOf(false) }
    var observaciones by remember { mutableStateOf("") }

    // UI states
    var guardando by remember { mutableStateOf(false) }
    var mensajeExito by remember { mutableStateOf<String?>(null) }
    var errorMsg by remember { mutableStateOf<String?>(null) }

    val totalCarrito = carrito.sumOf { it.subtotal }

    // Filtrar productos
    LaunchedEffect(busqueda, productos) {
        productosFiltrados = if (busqueda.isBlank()) productos
        else productos.filter {
            it.nombre.contains(busqueda, ignoreCase = true) ||
            it.codigo_barras?.contains(busqueda) == true
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Nuevo Pedido", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Volver")
                    }
                },
                actions = {
                    // Badge con cantidad de items en carrito
                    BadgedBox(
                        badge = {
                            if (carrito.isNotEmpty()) {
                                Badge { Text(carrito.sumOf { it.cantidad.toInt() }.toString()) }
                            }
                        },
                        modifier = Modifier.padding(end = 8.dp)
                    ) {
                        IconButton(onClick = { showCarrito = !showCarrito }) {
                            Icon(Icons.Default.ShoppingCart, contentDescription = "Carrito")
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = BaboonsBlue,
                    titleContentColor = Color.White,
                    actionIconContentColor = Color.White,
                    navigationIconContentColor = Color.White
                )
            )
        },
        containerColor = BaboonsBackground,
        floatingActionButton = {
            if (carrito.isNotEmpty() && !showCarrito) {
                ExtendedFloatingActionButton(
                    onClick = { showCarrito = true },
                    containerColor = BaboonsBlue,
                    contentColor = Color.White,
                    icon = { Icon(Icons.Default.ShoppingCart, contentDescription = null) },
                    text = { Text("Ver pedido · $${String.format("%,.0f", totalCarrito)}") }
                )
            }
        }
    ) { paddingValues ->

        if (showCarrito) {
            // Vista del carrito / confirmación
            CarritoView(
                carrito = carrito,
                observaciones = observaciones,
                onObservacionesChange = { observaciones = it },
                onCambiarCantidad = { item, nuevaCantidad ->
                    carrito = if (nuevaCantidad <= 0) {
                        carrito.filter { it.productoId != item.productoId }
                    } else {
                        carrito.map { if (it.productoId == item.productoId) it.copy(cantidad = nuevaCantidad) else it }
                    }
                },
                onEliminar = { item ->
                    carrito = carrito.filter { it.productoId != item.productoId }
                    if (carrito.isEmpty()) showCarrito = false
                },
                onVolver = { showCarrito = false },
                guardando = guardando,
                mensajeExito = mensajeExito,
                onConfirmar = {
                    guardando = true
                    scope.launch {
                        try {
                            val uuid = UUID.randomUUID().toString()
                            val items = carrito.map { item ->
                                PedidoItemDto(
                                    producto_id = item.productoId,
                                    nombre = item.nombre,
                                    cantidad = item.cantidad,
                                    precio_unitario = item.precioUnitario,
                                    lista_precio_id = item.listaPrecioId
                                )
                            }

                            // Eliminar acceso dual directo. Ahora SIEMPRE pasa por Room (Offline First Total)

                            // Guardar offline con UUID local
                            val itemsJson = gson.toJson(items)
                            
                            // Obtener el nombre real del cliente
                            val cliente = viewModel.getCliente(clienteId)
                            val cNombre = cliente?.nombre ?: "Cliente #$clienteId"

                            val pedidoOffline = PedidoPendienteEntity(
                                uuid_local = uuid,
                                cliente_id = clienteId,
                                cliente_nombre = cNombre,
                                negocio_id = negocioId ?: 1,
                                items_json = itemsJson,
                                total = totalCarrito,
                                observaciones = observaciones.ifBlank { null },
                                hoja_ruta_id = hojaRutaId,
                                estado_sync = "pendiente",
                                creado_en = System.currentTimeMillis()
                            )
                            viewModel.guardarPedidoOffline(pedidoOffline) {
                                // Forzar intento de sincronización inmediata
                                val syncRequest = OneTimeWorkRequestBuilder<com.baboons.sellers.workers.SyncWorker>().build()
                                WorkManager.getInstance(context).enqueue(syncRequest)

                                mensajeExito = "📱 Pedido guardado localmente"
                                scope.launch {
                                    kotlinx.coroutines.delay(2000)
                                    onPedidoGuardado()
                                }
                            }

                        } catch (e: Exception) {
                            errorMsg = "Error: ${e.message}"
                        } finally {
                            guardando = false
                        }
                    }
                }
            )
        } else {
            // Lista de productos
            Column(modifier = Modifier.padding(paddingValues)) {
                // Búsqueda
                OutlinedTextField(
                    value = busqueda,
                    onValueChange = { busqueda = it },
                    placeholder = { Text("Buscar producto o código...") },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                    trailingIcon = {
                        if (busqueda.isNotEmpty()) {
                            IconButton(onClick = { busqueda = "" }) {
                                Icon(Icons.Default.Clear, contentDescription = null)
                            }
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 10.dp),
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = BaboonsBlue,
                        unfocusedBorderColor = BaboonsBorder,
                        focusedTextColor = BaboonsTextPrimary,
                        unfocusedTextColor = BaboonsTextSecondary,
                        cursorColor = BaboonsBlue,
                        focusedContainerColor = BaboonsSurface,
                        unfocusedContainerColor = BaboonsSurface,
                        focusedPlaceholderColor = BaboonsTextSecondary,
                        unfocusedPlaceholderColor = BaboonsTextSecondary
                    )
                )

                if (productosFiltrados.isEmpty() && busqueda.isBlank()) {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text("No hay productos (Sincroniza para ver inventario)", color = BaboonsTextSecondary)
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(productosFiltrados) { producto ->
                            ProductoItem(
                                producto = producto,
                                onAdd = { p, precio, listaId, nombreLista ->
                                    val existing = carrito.find { it.productoId == p.id && it.listaPrecioId == listaId }
                                    carrito = if (existing != null) {
                                        carrito.map { if (it.productoId == p.id && it.listaPrecioId == listaId) it.copy(cantidad = it.cantidad + 1) else it }
                                    } else {
                                        carrito + ItemCarrito(p.id, p.nombre, 1.0, precio, listaId, nombreLista)
                                    }
                                }
                            )
                        }
                    }
                }
            }
        }

        // Toasts / Mensajes
        if (mensajeExito != null) {
            AlertDialog(
                onDismissRequest = { mensajeExito = null },
                confirmButton = {},
                text = { Text(mensajeExito!!, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth()) },
                containerColor = BaboonsSurface,
                textContentColor = BaboonsTextPrimary
            )
        }

        if (errorMsg != null) {
            AlertDialog(
                onDismissRequest = { errorMsg = null },
                confirmButton = { TextButton(onClick = { errorMsg = null }) { Text("OK") } },
                title = { Text("Error") },
                text = { Text(errorMsg!!) }
            )
        }
    }
}

@Composable
fun ProductoItem(
    producto: ProductoEntity,
    onAdd: (ProductoEntity, Double, Int?, String) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = BaboonsSurface),
        border = BorderStroke(1.dp, BaboonsBorder),
        onClick = { expanded = !expanded }
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(producto.nombre, fontWeight = FontWeight.Bold, color = BaboonsTextPrimary, fontSize = 16.sp)
                    Text("Stock: ${producto.stock} | Código: ${producto.codigo_barras ?: "N/A"}", color = BaboonsTextSecondary, fontSize = 12.sp)
                }
                Icon(
                    if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                    contentDescription = null,
                    tint = BaboonsTextSecondary
                )
            }

            AnimatedVisibility(visible = expanded) {
                Column(modifier = Modifier.padding(top = 12.dp)) {
                    HorizontalDivider(color = BaboonsBorder, thickness = 0.5.dp)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("Selecciona una lista de precios:", color = BaboonsBlue, fontSize = 12.sp, fontWeight = FontWeight.Medium)
                    Spacer(modifier = Modifier.height(4.dp))

                    // Lista Base
                    PrecioRow(nombre = "Precio Base", precio = producto.precio_base) {
                        onAdd(producto, producto.precio_base, null, "Base")
                    }
                }
            }
        }
    }
}

@Composable
fun PrecioRow(nombre: String, precio: Double, onAdd: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onAdd() }
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(nombre, color = BaboonsTextSecondary, modifier = Modifier.weight(1f))
        Text("$${String.format("%,.0f", precio)}", color = BaboonsTextPrimary, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 8.dp))
        Icon(Icons.Default.AddCircle, contentDescription = null, tint = BaboonsSuccess)
    }
}

@Composable
fun CarritoView(
    carrito: List<ItemCarrito>,
    observaciones: String,
    onObservacionesChange: (String) -> Unit,
    onCambiarCantidad: (ItemCarrito, Double) -> Unit,
    onEliminar: (ItemCarrito) -> Unit,
    onVolver: () -> Unit,
    onConfirmar: () -> Unit,
    guardando: Boolean,
    mensajeExito: String?
) {
    Column(modifier = Modifier.fillMaxSize().background(BaboonsBackground)) {
        // Cabecera
        Row(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onVolver) { Icon(Icons.Default.Close, contentDescription = null, tint = BaboonsTextPrimary) }
            Text("Revisar Pedido", style = MaterialTheme.typography.titleLarge, color = BaboonsTextPrimary, fontWeight = FontWeight.Bold)
        }

        LazyColumn(modifier = Modifier.weight(1f).padding(horizontal = 16.dp)) {
            items(carrito) { item ->
                Card(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                    colors = CardDefaults.cardColors(containerColor = BaboonsSurface),
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, BaboonsBorder)
                ) {
                    Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(item.nombre, color = BaboonsTextPrimary, fontWeight = FontWeight.Bold)
                            Text("Lista: ${item.nombreLista} · $${String.format("%,.0f", item.precioUnitario)}", color = BaboonsTextSecondary, fontSize = 12.sp)
                        }
 
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            IconButton(onClick = { onCambiarCantidad(item, item.cantidad - 1) }) {
                                Icon(Icons.Default.RemoveCircleOutline, contentDescription = null, tint = BaboonsError)
                            }
                            Text(item.cantidad.toInt().toString(), color = BaboonsTextPrimary, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 4.dp))
                            IconButton(onClick = { onCambiarCantidad(item, item.cantidad + 1) }) {
                                Icon(Icons.Default.AddCircleOutline, contentDescription = null, tint = BaboonsBlue)
                            }
                        }
                    }
                }
            }

            item {
                Spacer(modifier = Modifier.height(16.dp))
                Text("Observaciones", color = BaboonsTextSecondary, fontWeight = FontWeight.Bold)
                OutlinedTextField(
                    value = observaciones,
                    onValueChange = onObservacionesChange,
                    modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                    placeholder = { Text("Ej: Entregar por la tarde...") },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = BaboonsTextPrimary,
                        unfocusedTextColor = BaboonsTextPrimary,
                        focusedContainerColor = BaboonsSurface,
                        unfocusedContainerColor = BaboonsSurface,
                        focusedBorderColor = BaboonsBlue,
                        unfocusedBorderColor = BaboonsBorder
                    )
                )
                Spacer(modifier = Modifier.height(32.dp))
            }
        }

        // Footer con Total y Botón
        Surface(
            color = BaboonsSurface,
            shadowElevation = 8.dp,
            border = BorderStroke(1.dp, BaboonsBorder)
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Total Estimado", color = BaboonsTextSecondary)
                    Text("$${String.format("%,.0f", carrito.sumOf { it.subtotal })}", color = BaboonsBlue, fontWeight = FontWeight.ExtraBold, fontSize = 20.sp)
                }
                Spacer(modifier = Modifier.height(16.dp))
                Button(
                    onClick = onConfirmar,
                    modifier = Modifier.fillMaxWidth().height(56.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = BaboonsBlue),
                    enabled = !guardando
                ) {
                    if (guardando) CircularProgressIndicator(color = Color.White, modifier = Modifier.size(24.dp))
                    else Text("CONFIRMAR PEDIDO", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}
