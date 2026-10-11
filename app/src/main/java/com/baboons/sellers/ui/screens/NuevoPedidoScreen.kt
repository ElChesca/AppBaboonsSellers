package com.baboons.sellers.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.ExperimentalLayoutApi
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
    esAutoVenta: Boolean = false,
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
    var stockBordo by remember { mutableStateOf<List<StockAutoventaDto>>(emptyList()) }
    var cargandoStock by remember { mutableStateOf(esAutoVenta) }
    var errorStock by remember { mutableStateOf<String?>(null) }
    var metodoPago by remember { mutableStateOf("Efectivo") }

    val totalCarrito = carrito.sumOf { it.subtotal }
    val stockPorProducto = remember(stockBordo) { stockBordo.associate { it.producto_id to it.cantidad } }

    LaunchedEffect(esAutoVenta, hojaRutaId) {
        if (!esAutoVenta || hojaRutaId == null) return@LaunchedEffect
        cargandoStock = true
        errorStock = null
        try {
            val resp = api.getStockAutoventa(hojaRutaId)
            if (resp.isSuccessful) stockBordo = resp.body().orEmpty()
            else errorStock = mensajeErrorApi(resp)
        } catch (e: Exception) {
            errorStock = "Sin conexión para ver el stock del camión"
        } finally {
            cargandoStock = false
        }
    }

    val productosBordo = remember(stockBordo) {
        stockBordo.map {
            ProductoEntity(
                id = it.producto_id,
                nombre = it.producto_nombre,
                stock = it.cantidad,
                precio_base = it.precio_venta
            )
        }
    }

    // Filtrar productos
    LaunchedEffect(busqueda, productos, productosBordo, esAutoVenta) {
        val base = if (esAutoVenta) productosBordo else productos
        productosFiltrados = if (busqueda.isBlank()) base
        else base.filter {
            it.nombre.contains(busqueda, ignoreCase = true) ||
            it.codigo_barras?.contains(busqueda) == true
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (esAutoVenta) "Venta a bordo" else "Nuevo Pedido", fontWeight = FontWeight.Bold) },
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
                    text = { Text("${if (esAutoVenta) "Ver venta" else "Ver pedido"} · $${String.format("%,.0f", totalCarrito)}") }
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
                    val tope = if (esAutoVenta) stockPorProducto[item.productoId] ?: item.cantidad else Double.MAX_VALUE
                    val nueva = nuevaCantidad.coerceAtMost(tope)
                    carrito = if (nueva <= 0) {
                        carrito.filter { it.productoId != item.productoId }
                    } else {
                        carrito.map { if (it.productoId == item.productoId) it.copy(cantidad = nueva) else it }
                    }
                },
                metodoPago = if (esAutoVenta) metodoPago else null,
                onMetodoPago = { metodoPago = it },
                textoConfirmar = if (esAutoVenta) "CONFIRMAR VENTA" else "CONFIRMAR PEDIDO",
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
                            if (esAutoVenta) {
                                if (hojaRutaId == null || negocioId == null) {
                                    errorMsg = "No hay una hoja de Auto-Venta seleccionada"
                                    return@launch
                                }
                                val resp = api.registrarVentaBordo(
                                    hojaRutaId,
                                    VentaBordoRequest(
                                        cliente_id = clienteId,
                                        negocio_id = negocioId,
                                        metodo_pago = metodoPago,
                                        items = carrito.map {
                                            VentaBordoItem(it.productoId, it.cantidad, it.precioUnitario)
                                        }
                                    )
                                )
                                if (!resp.isSuccessful) {
                                    errorMsg = mensajeErrorApi(resp)
                                    return@launch
                                }
                                val cliente = viewModel.getCliente(clienteId)
                                viewModel.guardarVentaSincronizada(
                                    PedidoPendienteEntity(
                                        uuid_local = UUID.randomUUID().toString(),
                                        cliente_id = clienteId,
                                        cliente_nombre = cliente?.nombre ?: "Cliente #$clienteId",
                                        negocio_id = negocioId,
                                        items_json = gson.toJson(carrito.map {
                                            PedidoItemDto(it.productoId, it.nombre, it.cantidad, it.precioUnitario, it.listaPrecioId)
                                        }),
                                        total = totalCarrito,
                                        observaciones = observaciones.ifBlank { null },
                                        hoja_ruta_id = hojaRutaId,
                                        estado_sync = "sincronizado",
                                        creado_en = System.currentTimeMillis()
                                    )
                                ) {
                                    mensajeExito = "Venta registrada. El admin confirma el cobro."
                                    scope.launch {
                                        kotlinx.coroutines.delay(1500)
                                        onPedidoGuardado()
                                    }
                                }
                                return@launch
                            }
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

                if (esAutoVenta && cargandoStock) {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = BaboonsBlue)
                    }
                } else if (esAutoVenta && errorStock != null) {
                    Column(
                        Modifier.fillMaxSize().padding(24.dp),
                        verticalArrangement = Arrangement.Center,
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(errorStock!!, color = BaboonsError, textAlign = TextAlign.Center)
                        Spacer(Modifier.height(12.dp))
                        Button(onClick = {
                            scope.launch {
                                if (hojaRutaId == null) return@launch
                                cargandoStock = true
                                errorStock = null
                                try {
                                    val resp = api.getStockAutoventa(hojaRutaId)
                                    if (resp.isSuccessful) stockBordo = resp.body().orEmpty()
                                    else errorStock = mensajeErrorApi(resp)
                                } catch (e: Exception) {
                                    errorStock = "Sin conexión para ver el stock del camión"
                                } finally {
                                    cargandoStock = false
                                }
                            }
                        }) { Text("Reintentar") }
                    }
                } else if (productosFiltrados.isEmpty() && busqueda.isBlank()) {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text(
                            if (esAutoVenta) "No hay mercadería cargada en el camión" else "No hay productos (Sincroniza para ver inventario)",
                            color = BaboonsTextSecondary,
                            textAlign = TextAlign.Center
                        )
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
                                etiquetaStock = if (esAutoVenta) "En el camión" else "Stock",
                                onAdd = { p, precio, listaId, nombreLista ->
                                    val tope = if (esAutoVenta) stockPorProducto[p.id] ?: 0.0 else Double.MAX_VALUE
                                    val enCarrito = carrito.filter { it.productoId == p.id }.sumOf { it.cantidad }
                                    if (enCarrito + 1 > tope) {
                                        errorMsg = "No hay más stock en el camión de ${p.nombre}"
                                    } else {
                                        val existing = carrito.find { it.productoId == p.id && it.listaPrecioId == listaId }
                                        carrito = if (existing != null) {
                                            carrito.map { if (it.productoId == p.id && it.listaPrecioId == listaId) it.copy(cantidad = it.cantidad + 1) else it }
                                        } else {
                                            carrito + ItemCarrito(p.id, p.nombre, 1.0, precio, listaId, nombreLista)
                                        }
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
    etiquetaStock: String = "Stock",
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
                    Text("$etiquetaStock: ${producto.stock} | Código: ${producto.codigo_barras ?: "N/A"}", color = BaboonsTextSecondary, fontSize = 12.sp)
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

private fun mensajeErrorApi(resp: retrofit2.Response<*>): String {
    val raw = resp.errorBody()?.string().orEmpty()
    return try {
        com.google.gson.JsonParser.parseString(raw).asJsonObject.get("error")?.asString
            ?: "Error del servidor (${resp.code()})"
    } catch (e: Exception) {
        raw.ifBlank { "Error del servidor (${resp.code()})" }
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

@OptIn(ExperimentalLayoutApi::class)
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
    mensajeExito: String?,
    metodoPago: String? = null,
    onMetodoPago: (String) -> Unit = {},
    textoConfirmar: String = "CONFIRMAR PEDIDO"
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
                if (metodoPago != null) {
                    Spacer(modifier = Modifier.height(16.dp))
                    Text("COBRO", color = BaboonsTextSecondary, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(8.dp))
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf("Efectivo", "Transferencia", "Cuenta Corriente").forEach { m ->
                            FilterChip(
                                selected = metodoPago == m,
                                onClick = { onMetodoPago(m) },
                                label = { Text(m) }
                            )
                        }
                    }
                }
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
            Column(modifier = Modifier.navigationBarsPadding().padding(20.dp)) {
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
                    else Text(textoConfirmar, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}
