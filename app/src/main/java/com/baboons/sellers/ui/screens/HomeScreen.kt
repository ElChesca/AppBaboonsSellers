package com.baboons.sellers.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.ui.res.painterResource
import com.baboons.sellers.R
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
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
import androidx.hilt.navigation.compose.hiltViewModel
import com.baboons.sellers.viewmodel.HomeViewModel
import com.baboons.sellers.data.local.entities.*
import com.baboons.sellers.util.TokenStore
import com.baboons.sellers.ui.theme.*
import android.widget.Toast
import java.util.Locale
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    onClienteClick: (clienteId: Int) -> Unit,
    onLogout: () -> Unit,
    onVerPedidos: () -> Unit,
    onVerMapa: () -> Unit,
    onVerStock: () -> Unit,
    onVerResumen: () -> Unit,
    onClienteCart: (clienteId: Int) -> Unit,
    viewModel: HomeViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()

    val clientes by viewModel.clientes.collectAsState()
    val hojasRuta by viewModel.hojasRuta.collectAsState()
    val selectedHrId by viewModel.selectedHojaRutaId.collectAsState()
    val hojaRutaClientes by viewModel.hojaRutaClientes.collectAsState()
    val isSyncing by viewModel.isSyncing.collectAsState()
    val negocioNombre by viewModel.negocioNombre.collectAsState()
    val pedidos by viewModel.pedidos.collectAsState()
    
    var searchQuery by remember { mutableStateOf("") }
    var showNovedadDialog by remember { mutableStateOf(false) }

    // Cálculos de Indicadores (KPIs)
    val totalClientesEnRuta: Int = remember(hojaRutaClientes, selectedHrId) {
        hojaRutaClientes.count { it.hoja_ruta_id == selectedHrId }
    }
    
    val clientesConPedidoIds: Set<Int> = remember(pedidos, selectedHrId) {
        pedidos.filter { it.hoja_ruta_id == selectedHrId }.map { it.cliente_id }.toSet()
    }
    
    val clientesVisitadosIds: Set<Int> = remember(hojaRutaClientes, selectedHrId) {
        hojaRutaClientes.filter { it.hoja_ruta_id == selectedHrId && it.visitado }.map { it.cliente_id }.toSet()
    }

    val resultadosVisita: Map<Int, String> = remember(hojaRutaClientes, selectedHrId) {
        hojaRutaClientes.filter { it.hoja_ruta_id == selectedHrId && it.resultado_visita != null }
            .associate { it.cliente_id to it.resultado_visita!! }
    }

    val totalCompletados: Int = remember(clientesConPedidoIds, clientesVisitadosIds) {
        (clientesConPedidoIds.asIterable() + clientesVisitadosIds.asIterable()).toSet().size
    }

    val pendientesCount: Int = totalClientesEnRuta - totalCompletados
    val pedidosCount: Int = clientesConPedidoIds.size
    val progresoPercent: Int = if (totalClientesEnRuta > 0) ((totalCompletados.toFloat() / totalClientesEnRuta.toFloat()) * 100).toInt() else 0

    // Auto-seleccionar la primera ruta si no hay ninguna seleccionada
    LaunchedEffect(hojasRuta) {
        if (selectedHrId == null && hojasRuta.isNotEmpty()) {
            viewModel.selectHojaRuta(hojasRuta.first().id)
        }
    }

    val filteredClientesByHr = remember(clientes, selectedHrId, searchQuery, hojaRutaClientes) {
        clientes.filter { cliente ->
            val matchesHr = if (selectedHrId == null) {
                // Si no hay ruta seleccionada, no mostramos nada o mostramos todas las de este vendedor
                false
            } else {
                // ✨ Buscamos si el cliente está en esta ruta
                hojaRutaClientes.any { it.hoja_ruta_id == selectedHrId && it.cliente_id == cliente.id }
            }
            
            val matchesSearch = searchQuery.isBlank() ||
                cliente.nombre.contains(searchQuery, ignoreCase = true) ||
                (cliente.direccion?.contains(searchQuery, ignoreCase = true) == true)
            matchesHr && matchesSearch
        }
    }

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ModalDrawerSheet(
                drawerContainerColor = BaboonsSurface,
                drawerShape = RoundedCornerShape(topEnd = 16.dp, bottomEnd = 16.dp)
            ) {
                Spacer(modifier = Modifier.height(24.dp))
                // Header del Drawer
                Column(modifier = Modifier.padding(20.dp)) {
                    Surface(
                        modifier = Modifier.size(60.dp),
                        shape = RoundedCornerShape(12.dp),
                        color = BaboonsSurface,
                        border = BorderStroke(0.5.dp, BaboonsBorder)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Image(
                                painter = painterResource(id = R.drawable.ic_logo),
                                contentDescription = "Logo",
                                modifier = Modifier.size(48.dp)
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = TokenStore.getNombre(context) ?: "Vendedor",
                        fontWeight = FontWeight.Bold,
                        fontSize = 20.sp,
                        color = BaboonsTextPrimary
                    )
                    Text(
                        text = negocioNombre,
                        fontSize = 14.sp,
                        color = BaboonsTextSecondary
                    )
                }
                HorizontalDivider(modifier = Modifier.padding(horizontal = 20.dp, vertical = 10.dp), color = BaboonsBorder)
                
                // Items del Menú
                NavigationDrawerItem(
                    label = { Text("Inicio", fontWeight = FontWeight.Medium) },
                    selected = true,
                    onClick = { scope.launch { drawerState.close() } },
                    icon = { Icon(Icons.Default.Home, null) },
                    modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding),
                    colors = NavigationDrawerItemDefaults.colors(
                        selectedContainerColor = BaboonsBlue.copy(alpha = 0.1f),
                        selectedIconColor = BaboonsBlue,
                        selectedTextColor = BaboonsBlue
                    )
                )
                NavigationDrawerItem(
                    label = { Text("Mis Pedidos", fontWeight = FontWeight.Medium) },
                    selected = false,
                    onClick = { 
                        scope.launch { 
                            drawerState.close()
                            onVerPedidos() 
                        } 
                    },
                    icon = { Icon(Icons.Default.ListAlt, null) },
                    modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
                )
                NavigationDrawerItem(
                    label = { Text("Consultar Stock", fontWeight = FontWeight.Medium) },
                    selected = false,
                    onClick = {
                        scope.launch {
                            drawerState.close()
                            onVerStock()
                        }
                    },
                    icon = { Icon(Icons.Default.Warehouse, null) },
                    modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
                )
                NavigationDrawerItem(
                    label = { Text("Resumen de Jornada", fontWeight = FontWeight.Medium) },
                    selected = false,
                    onClick = {
                        scope.launch {
                            drawerState.close()
                            onVerResumen()
                        }
                    },
                    icon = { Icon(Icons.Default.Assessment, null) },
                    modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
                )
                NavigationDrawerItem(
                    label = { Text("Reportar Novedad", fontWeight = FontWeight.Medium) },
                    selected = false,
                    onClick = {
                        scope.launch {
                            drawerState.close()
                            showNovedadDialog = true
                        }
                    },
                    icon = { Icon(Icons.Default.ReportProblem, null) },
                    modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding),
                    colors = NavigationDrawerItemDefaults.colors(unselectedIconColor = BaboonsWarning, unselectedTextColor = BaboonsWarning)
                )
                NavigationDrawerItem(
                    label = { Text("Mapa de Ruta", fontWeight = FontWeight.Medium) },
                    selected = false,
                    onClick = { 
                        scope.launch { 
                            drawerState.close()
                            onVerMapa() 
                        } 
                    },
                    icon = { Icon(Icons.Default.Map, null) },
                    modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
                )
                
                Spacer(modifier = Modifier.weight(1f))
                
                HorizontalDivider(modifier = Modifier.padding(horizontal = 20.dp), color = BaboonsBorder)
                NavigationDrawerItem(
                    label = { Text("Cerrar Sesión", fontWeight = FontWeight.Medium) },
                    selected = false,
                    onClick = { onLogout() },
                    icon = { Icon(Icons.AutoMirrored.Filled.Logout, null) },
                    modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding),
                    colors = NavigationDrawerItemDefaults.colors(unselectedIconColor = BaboonsError, unselectedTextColor = BaboonsError)
                )
                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    ) {
        Scaffold(
            containerColor = BaboonsBackground,
            topBar = {
                TopAppBar(
                    navigationIcon = {
                        IconButton(onClick = { scope.launch { drawerState.open() } }) {
                            Icon(Icons.Default.Menu, null)
                        }
                    },
                    title = {
                        val currentDate = java.text.SimpleDateFormat("dd/MM/yyyy", java.util.Locale.getDefault()).format(java.util.Date())
                        Column {
                            Text(negocioNombre, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                            Text(currentDate, style = MaterialTheme.typography.labelSmall, color = Color.White.copy(alpha = 0.8f))
                        }
                    },
                    actions = {
                        IconButton(
                            onClick = { 
                                val id = TokenStore.getNegocioId(context)
                                if (id != -1) viewModel.sync(id)
                            },
                            enabled = !isSyncing
                        ) { 
                            if (isSyncing) {
                                CircularProgressIndicator(modifier = Modifier.size(20.dp), color = Color.White, strokeWidth = 2.dp)
                            } else {
                                Icon(Icons.Default.Sync, null) 
                            }
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = BaboonsBlue, titleContentColor = Color.White, actionIconContentColor = Color.White, navigationIconContentColor = Color.White)
                )
            }
        ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding).padding(horizontal = 16.dp)) {
            
            // ✨ Indicadores (KPIs) de Progreso
            Row(
                modifier = Modifier.fillMaxWidth().padding(top = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                KPIBox(modifier = Modifier.weight(1f), label = "Pendientes", value = pendientesCount.toString(), color = BaboonsBlue)
                KPIBox(modifier = Modifier.weight(1f), label = "Pedidos", value = pedidosCount.toString(), color = BaboonsBlue)
                KPIBox(modifier = Modifier.weight(1f), label = "Progreso", value = "$progresoPercent%", color = if (progresoPercent == 100) BaboonsSuccess else BaboonsBlue)
            }

            // ✨ Selector de Hoja de Ruta (Dropdown)
            if (hojasRuta.isNotEmpty()) {
                var expanded by remember { mutableStateOf(false) }
                val currentHrName = hojasRuta.find { it.id == selectedHrId }?.let { "Ruta #${it.numero_interno ?: it.id} (${it.estado})" } ?: "Seleccionar Ruta"

                Box(modifier = Modifier.fillMaxWidth().padding(top = 16.dp)) {
                    Surface(
                        modifier = Modifier.fillMaxWidth().clickable { expanded = true },
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
                    DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }, modifier = Modifier.fillMaxWidth(0.8f)) {
                        hojasRuta.forEach { hr ->
                            DropdownMenuItem(
                                text = { Text("Ruta #${hr.numero_interno ?: hr.id} (${hr.estado})") },
                                onClick = { viewModel.selectHojaRuta(hr.id); expanded = false }
                            )
                        }
                    }
                }
            }

            // Buscador
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                modifier = Modifier.fillMaxWidth().padding(vertical = 16.dp),
                placeholder = { Text("Buscar cliente...", color = BaboonsTextSecondary) },
                leadingIcon = { Icon(Icons.Default.Search, null, tint = BaboonsBlue) },
                shape = RoundedCornerShape(12.dp),
                singleLine = true,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = BaboonsBlue,
                    unfocusedBorderColor = BaboonsBorder,
                    focusedContainerColor = BaboonsSurface,
                    unfocusedContainerColor = BaboonsSurface,
                    cursorColor = BaboonsBlue
                )
            )

            Text(
                text = "${filteredClientesByHr.size} clientes en esta ruta",
                style = MaterialTheme.typography.labelMedium,
                color = BaboonsTextSecondary,
                modifier = Modifier.padding(bottom = 8.dp)
            )

            LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp), contentPadding = PaddingValues(bottom = 32.dp), modifier = Modifier.weight(1f)) {
                items(filteredClientesByHr) { cliente ->
                    val hasOrder = clientesConPedidoIds.contains(cliente.id)
                    val isVisited = clientesVisitadosIds.contains(cliente.id)
                    
                    ClienteCard(
                        cliente = cliente, 
                        isVisited = isVisited,
                        hasOrder = hasOrder,
                        resultadoVisita = resultadosVisita[cliente.id],
                        onToggleVisit = { 
                            selectedHrId?.let { hrId -> 
                                viewModel.toggleVisita(cliente.id, hrId, !isVisited) 
                            } 
                        },
                        onClick = { onClienteClick(cliente.id) },
                        onClickCart = { onClienteCart(cliente.id) }
                    )
                }
            }

            // ✨ Footer con Versión
            Text(
                text = "Multinegocio Baboons © 2026 - v1.6.3-staging",
                fontSize = 10.sp,
                color = BaboonsTextSecondary,
                textAlign = TextAlign.Center,
            )
        }
    }

    // Modal de novedad: vive acá adentro para usar el mismo VM que tiene la ruta elegida
    if (showNovedadDialog) {
        NovedadDialog(
            clientesRuta = filteredClientesByHr,
            onReportar = { tipo, obs, clienteId, cb ->
                viewModel.reportarNovedad(tipo, obs, clienteId) { ok, msg ->
                    if (ok) Toast.makeText(context, "Novedad reportada al admin", Toast.LENGTH_SHORT).show()
                    cb(ok, msg)
                }
            },
            onDismiss = { showNovedadDialog = false }
        )
    }
}
}

@Composable
fun KPIBox(modifier: Modifier, label: String, value: String, color: Color) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = BaboonsSurface),
        border = BorderStroke(0.5.dp, BaboonsBorder)
    ) {
        Column(
            modifier = Modifier.padding(12.dp).fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(text = value, fontWeight = FontWeight.ExtraBold, fontSize = 20.sp, color = color)
            Text(text = label, fontSize = 10.sp, color = BaboonsTextSecondary, fontWeight = FontWeight.Medium)
        }
    }
}

@Composable
fun ClienteCard(
    cliente: ClienteEntity, 
    isVisited: Boolean,
    hasOrder: Boolean,
    resultadoVisita: String? = null,
    onToggleVisit: () -> Unit,
    onClick: () -> Unit,
    onClickCart: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = BaboonsSurface),
        border = BorderStroke(if (hasOrder) 2.dp else 0.5.dp, if (hasOrder) BaboonsBlue else BaboonsBorder),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier.padding(16.dp).fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Avatar con indicador de visita
            Box {
                Surface(
                    modifier = Modifier.size(44.dp),
                    shape = RoundedCornerShape(8.dp),
                    color = if (hasOrder) BaboonsBlue else if (isVisited) BaboonsTextSecondary.copy(alpha = 0.1f) else BaboonsBlue.copy(alpha = 0.1f)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text(
                            text = cliente.nombre.take(1).uppercase(),
                            color = if (hasOrder) Color.White else BaboonsBlue,
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp
                        )
                    }
                }
                if (isVisited && !hasOrder) {
                    Surface(
                        modifier = Modifier.size(16.dp).align(Alignment.BottomEnd).offset(4.dp, 4.dp),
                        shape = RoundedCornerShape(8.dp),
                        color = BaboonsWarning,
                        border = BorderStroke(2.dp, BaboonsSurface)
                    ) {
                        Icon(Icons.Default.Check, null, tint = Color.White, modifier = Modifier.padding(2.dp))
                    }
                }
            }

            Spacer(Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = cliente.nombre,
                    fontWeight = FontWeight.Bold,
                    color = BaboonsTextPrimary,
                    fontSize = 16.sp
                )
                Text(
                    text = cliente.direccion ?: "Sin dirección",
                    style = MaterialTheme.typography.bodySmall,
                    color = BaboonsTextSecondary,
                    maxLines = 1
                )
                resultadoVisita?.let {
                    Text(
                        text = it,
                        style = MaterialTheme.typography.labelSmall,
                        color = BaboonsWarning,
                        fontWeight = FontWeight.Medium,
                        maxLines = 1
                    )
                }
            }

            // Botones de acción
            Row(verticalAlignment = Alignment.CenterVertically) {
                // Botón Visita (Ojo)
                IconButton(onClick = onToggleVisit) {
                    Icon(
                        imageVector = if (isVisited) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                        contentDescription = "Visita",
                        tint = if (isVisited) BaboonsWarning else BaboonsTextSecondary.copy(alpha = 0.4f)
                    )
                }
                
                // Botón Pedido (Carrito)
                IconButton(onClick = onClickCart) {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = if (hasOrder) BaboonsBlue else BaboonsBlue.copy(alpha = 0.1f),
                        modifier = Modifier.size(36.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                Icons.Default.ShoppingCart, 
                                null, 
                                tint = if (hasOrder) Color.White else BaboonsBlue,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}
