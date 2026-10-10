package com.baboons.sellers.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.baboons.sellers.data.local.entities.ProductoEntity
import com.baboons.sellers.data.remote.RefuerzoItem
import com.baboons.sellers.ui.theme.BaboonsBlue
import com.baboons.sellers.ui.theme.BaboonsError
import com.baboons.sellers.ui.theme.BaboonsSuccess

/**
 * Consultar Stock del depósito + Pedido de Refuerzo al galpón.
 * El stock viene del último sync; el refuerzo pega en POST /hojas-ruta/{id}/refuerzos.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StockScreen(
    productos: List<ProductoEntity>,
    onPedirRefuerzo: (List<RefuerzoItem>, (Boolean, String) -> Unit) -> Unit,
    onBack: () -> Unit
) {
    var query by remember { mutableStateOf("") }
    var mensaje by remember { mutableStateOf<String?>(null) }
    var esError by remember { mutableStateOf(false) }
    var enviando by remember { mutableStateOf(false) }
    val cantidades = remember { mutableStateMapOf<Int, Double>() }

    val filtrados = remember(query, productos) {
        if (query.isBlank()) productos
        else productos.filter {
            it.nombre.contains(query, ignoreCase = true) ||
                (it.codigo_barras?.contains(query, ignoreCase = true) == true)
        }
    }

    val seleccionados = cantidades.filter { it.value > 0 }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Consultar Stock", fontWeight = FontWeight.Bold) },
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
            if (seleccionados.isNotEmpty()) {
                Button(
                    onClick = {
                        enviando = true
                        onPedirRefuerzo(
                            seleccionados.map { (pid, qty) -> RefuerzoItem(pid, qty) }
                        ) { ok, msg ->
                            enviando = false
                            esError = !ok
                            mensaje = msg
                            if (ok) cantidades.clear()
                        }
                    },
                    enabled = !enviando,
                    modifier = Modifier.fillMaxWidth().padding(16.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = BaboonsBlue)
                ) {
                    Text(
                        if (enviando) "Enviando..." else "Pedir Refuerzo (${seleccionados.size} items)",
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding)) {
            Surface(
                color = Color(0xFFD1ECF1),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    "Buscá un producto para ver el stock central y tocá (+) para pedir refuerzo de carga al galpón.",
                    modifier = Modifier.padding(12.dp),
                    style = MaterialTheme.typography.bodySmall
                )
            }

            OutlinedTextField(
                value = query,
                onValueChange = { query = it },
                modifier = Modifier.fillMaxWidth().padding(12.dp),
                placeholder = { Text("Buscar producto...") },
                leadingIcon = { Icon(Icons.Default.Search, null) },
                singleLine = true
            )

            mensaje?.let {
                Surface(
                    color = if (esError) BaboonsError.copy(alpha = 0.12f) else BaboonsSuccess.copy(alpha = 0.12f),
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(
                        it,
                        modifier = Modifier.padding(12.dp),
                        color = if (esError) BaboonsError else BaboonsSuccess,
                        fontWeight = FontWeight.Medium
                    )
                }
            }

            LazyColumn(modifier = Modifier.fillMaxSize()) {
                items(filtrados, key = { it.id }) { p ->
                    Card(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 4.dp),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(p.nombre, fontWeight = FontWeight.Bold)
                                p.codigo_barras?.let {
                                    Text(it, style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                                }
                                Text(
                                    "${p.stock} ${p.unidad}",
                                    color = if (p.stock <= 0) BaboonsError else BaboonsSuccess,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                IconButton(onClick = {
                                    val cur = cantidades[p.id] ?: 0.0
                                    if (cur > 0) cantidades[p.id] = cur - 1 else cantidades.remove(p.id)
                                }) { Icon(Icons.Default.Remove, null) }
                                Text(
                                    "${(cantidades[p.id] ?: 0.0).toInt()}",
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.width(28.dp).wrapContentWidth(Alignment.CenterHorizontally)
                                )
                                IconButton(onClick = {
                                    cantidades[p.id] = (cantidades[p.id] ?: 0.0) + 1
                                }) { Icon(Icons.Default.Add, null) }
                            }
                        }
                    }
                }
            }
        }
    }
}
