package com.baboons.sellers

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import android.Manifest
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.baboons.sellers.ui.screens.LoginScreen
import com.baboons.sellers.ui.screens.HomeScreen
import com.baboons.sellers.ui.screens.NuevoPedidoScreen
import com.baboons.sellers.ui.screens.PedidosPendientesScreen
import com.baboons.sellers.ui.screens.SyncScreen
import com.baboons.sellers.ui.screens.*
import com.baboons.sellers.ui.theme.SellersTheme
import com.baboons.sellers.util.TokenStore
import com.baboons.sellers.workers.SyncWorker
import com.baboons.sellers.viewmodel.HomeViewModel
import androidx.hilt.navigation.compose.hiltViewModel
import dagger.hilt.android.AndroidEntryPoint
import androidx.work.*
import java.util.concurrent.TimeUnit

// Rutas de navegación
object Routes {
    const val LOGIN = "login"
    const val HOME = "home"
    const val SYNC = "sync/{negocioId}"
    const val DETALLE_CLIENTE = "detalle_cliente/{clienteId}"
    const val NUEVO_PEDIDO = "nuevo_pedido/{clienteId}/{hojaRutaId}"
    const val MIS_PEDIDOS = "mis_pedidos"
    const val PEDIDOS_PENDIENTES = "pedidos_pendientes"
    const val MAPA = "mapa"
    const val STOCK = "stock"

    fun nuevoPedido(clienteId: Int, hrId: Int?) = "nuevo_pedido/$clienteId/${hrId ?: -1}"
    fun detalleCliente(clienteId: Int) = "detalle_cliente/$clienteId"
    fun sync(negocioId: Int) = "sync/$negocioId"
}

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Pedir permisos de ubicación para el Mapa
        val locationPermissionRequest = registerForActivityResult(
            ActivityResultContracts.RequestMultiplePermissions()
        ) { permissions ->
            when {
                permissions.getOrDefault(Manifest.permission.ACCESS_FINE_LOCATION, false) -> {}
                permissions.getOrDefault(Manifest.permission.ACCESS_COARSE_LOCATION, false) -> {}
                else -> { /* No hay permisos, el mapa no mostrará MyLocation */ }
            }
        }
        val perms = mutableListOf(
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION
        )
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            perms.add(Manifest.permission.POST_NOTIFICATIONS)
        }
        locationPermissionRequest.launch(perms.toTypedArray())

        // Tracking GPS: si hay sesión activa, arranca el servicio al abrir la app
        if (TokenStore.isLoggedIn(this)) {
            com.baboons.sellers.service.LocationTrackingService.start(this)
        }

        // Iniciar el worker de fondo...
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()
            
        val syncRequest = PeriodicWorkRequestBuilder<SyncWorker>(15, TimeUnit.MINUTES)
            .setConstraints(constraints)
            .build()
            
        WorkManager.getInstance(this).enqueueUniquePeriodicWork(
            "OfflineSyncWork",
            ExistingPeriodicWorkPolicy.KEEP,
            syncRequest
        )

        enableEdgeToEdge()
        setContent {
            SellersTheme {
                MainNavigation()
            }
        }
    }
}

@Composable
fun MainNavigation() {
    val context = LocalContext.current
    val navController = rememberNavController()
    
    // Inyectamos el ViewModel aquí para compartir la lista de clientes entre pantallas
    val homeViewModel: HomeViewModel = hiltViewModel()

    // Determinar pantalla inicial según si hay sesión activa
    val startDestination = if (TokenStore.isLoggedIn(context)) Routes.HOME else Routes.LOGIN

    NavHost(navController = navController, startDestination = startDestination) {

        composable(Routes.LOGIN) {
            LoginScreen(
                onLoginSuccess = {
                    // El negocioId ya está guardado en TokenStore
                    val nId = TokenStore.getNegocioId(context)
                    navController.navigate(Routes.sync(nId)) {
                        popUpTo(Routes.LOGIN) { inclusive = true }
                    }
                }
            )
        }

        composable(Routes.SYNC) { backStack ->
            val nId = backStack.arguments?.getString("negocioId")?.toIntOrNull() ?: 1
            SyncScreen(
                negocioId = nId,
                onSyncComplete = {
                    navController.navigate(Routes.HOME) {
                        popUpTo(Routes.SYNC) { inclusive = true }
                    }
                }
            )
        }

        composable(Routes.HOME) {
            val selectedHrId by homeViewModel.selectedHojaRutaId.collectAsState()
            val hrClientes by homeViewModel.hojaRutaClientes.collectAsState(initial = emptyList())
            HomeScreen(
                onClienteClick = { id -> 
                    val hrId = selectedHrId ?: hrClientes.find { it.cliente_id == id }?.hoja_ruta_id
                    navController.navigate(Routes.nuevoPedido(id, hrId)) 
                },
                onLogout = {
                    com.baboons.sellers.service.LocationTrackingService.stop(context)
                    TokenStore.clearSession(context)
                    navController.navigate(Routes.LOGIN) { popUpTo(0) { inclusive = true } }
                },
                onVerPedidos = { navController.navigate(Routes.MIS_PEDIDOS) },
                onVerMapa = { navController.navigate(Routes.MAPA) },
                onVerStock = { navController.navigate(Routes.STOCK) }
            )
        }

        composable(Routes.DETALLE_CLIENTE) { backStack ->
            val clienteId = backStack.arguments?.getString("clienteId")?.toIntOrNull() ?: return@composable
            val selectedHrId by homeViewModel.selectedHojaRutaId.collectAsState()
            val clientes by homeViewModel.clientes.collectAsState(initial = emptyList())
            val hrClientes by homeViewModel.hojaRutaClientes.collectAsState(initial = emptyList())
            DetalleClienteScreen(
                clienteId = clienteId,
                clientes = clientes,
                onCrearPedido = { id -> 
                    val hrId = selectedHrId ?: hrClientes.find { it.cliente_id == id }?.hoja_ruta_id
                    navController.navigate(Routes.nuevoPedido(id, hrId)) 
                },
                onBack = { navController.popBackStack() }
            )
        }

        composable(Routes.NUEVO_PEDIDO) { backStack ->
            val clienteId = backStack.arguments?.getString("clienteId")?.toIntOrNull() ?: return@composable
            val hrIdRaw = backStack.arguments?.getString("hojaRutaId")?.toIntOrNull()
            val hrId = if (hrIdRaw == -1) null else hrIdRaw
            
            NuevoPedidoScreen(
                clienteId = clienteId,
                hojaRutaId = hrId,
                onPedidoGuardado = { navController.popBackStack(Routes.HOME, false) },
                onBack = { navController.popBackStack() }
            )
        }

        composable(Routes.MIS_PEDIDOS) {
            MisPedidosScreen(onBack = { navController.popBackStack() })
        }

        composable(Routes.PEDIDOS_PENDIENTES) {
            PedidosPendientesScreen(onBack = { navController.popBackStack() })
        }

        composable(Routes.STOCK) {
            val productos by homeViewModel.productos.collectAsState()
            StockScreen(
                productos = productos,
                onPedirRefuerzo = { items, cb -> homeViewModel.pedirRefuerzo(items, cb) },
                onBack = { navController.popBackStack() }
            )
        }

        composable(Routes.MAPA) {
            val selectedHrId by homeViewModel.selectedHojaRutaId.collectAsState()
            val allClientes by homeViewModel.clientes.collectAsState(initial = emptyList())
            val hrClientes by homeViewModel.hojaRutaClientes.collectAsState(initial = emptyList())
            
            // Filtrar clientes por ruta seleccionada (Igual que en HomeScreen)
            val filteredMapClientes = remember(allClientes, selectedHrId, hrClientes) {
                allClientes.filter { cliente ->
                    if (selectedHrId == null) {
                        hrClientes.any { it.cliente_id == cliente.id }
                    } else {
                        hrClientes.any { it.hoja_ruta_id == selectedHrId && it.cliente_id == cliente.id }
                    }
                }
            }

            RutaMapaScreen(
                clientes = filteredMapClientes,
                onSelectCliente = { id -> navController.navigate(Routes.detalleCliente(id)) },
                onBack = { navController.popBackStack() }
            )
        }
    }
}