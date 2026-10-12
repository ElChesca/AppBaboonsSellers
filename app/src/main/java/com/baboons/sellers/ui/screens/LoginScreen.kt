package com.baboons.sellers.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.*
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.*
import com.baboons.sellers.data.remote.LoginRequest
import com.baboons.sellers.data.remote.RetrofitClient
import com.baboons.sellers.util.TokenStore
import com.baboons.sellers.util.JwtUtils
import com.baboons.sellers.ui.theme.*
import kotlinx.coroutines.launch

@Composable
fun LoginScreen(onLoginSuccess: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }
    var isLoading by remember { mutableStateOf(false) }
    var errorMsg by remember { mutableStateOf<String?>(null) }

    val api = remember { RetrofitClient.getApi { null } } // Sin token para login

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(BaboonsBackground) // ✨ Fondo Blanco Figma
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // Logo / Título
            Text(
                text = "🐒", // ✨ Babuino oficial
                fontSize = 72.sp,
                modifier = Modifier.padding(bottom = 8.dp)
            )
            Text(
                text = "Baboons Sellers",
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold,
                color = BaboonsTextPrimary // ✨ Texto Negro
            )
            Text(
                text = "App de Preventa",
                fontSize = 14.sp,
                color = BaboonsTextSecondary,
                modifier = Modifier.padding(bottom = 48.dp)
            )

            // Card de login
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(
                    containerColor = BaboonsSurface // ✨ Tarjeta Blanca
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Text(
                        text = "Iniciar Sesión",
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 18.sp,
                        color = BaboonsTextPrimary
                    )

                    // Email
                    OutlinedTextField(
                        value = email,
                        onValueChange = { email = it; errorMsg = null },
                        label = { Text("Email") },
                        leadingIcon = { Icon(Icons.Default.Email, contentDescription = null, tint = BaboonsBlue) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = BaboonsBlue,
                            focusedLabelColor = BaboonsBlue,
                            cursorColor = BaboonsBlue,
                            focusedTextColor = BaboonsTextPrimary,
                            unfocusedTextColor = BaboonsTextSecondary,
                            unfocusedBorderColor = BaboonsBorder,
                            unfocusedLabelColor = BaboonsTextSecondary
                        )
                    )

                    // Password
                    OutlinedTextField(
                        value = password,
                        onValueChange = { password = it; errorMsg = null },
                        label = { Text("Contraseña") },
                        leadingIcon = { Icon(Icons.Default.Lock, contentDescription = null, tint = BaboonsBlue) },
                        trailingIcon = {
                            IconButton(onClick = { passwordVisible = !passwordVisible }) {
                                Icon(
                                    if (passwordVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                    contentDescription = null,
                                    tint = BaboonsTextSecondary
                                )
                            }
                        },
                        visualTransformation = if (passwordVisible) VisualTransformation.None
                                               else PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = BaboonsBlue,
                            focusedLabelColor = BaboonsBlue,
                            cursorColor = BaboonsBlue,
                            focusedTextColor = BaboonsTextPrimary,
                            unfocusedTextColor = BaboonsTextSecondary,
                            unfocusedBorderColor = BaboonsBorder,
                            unfocusedLabelColor = BaboonsTextSecondary
                        )
                    )

                    // Error
                    AnimatedVisibility(visible = errorMsg != null) {
                        errorMsg?.let {
                            Text(
                                text = it,
                                color = BaboonsError,
                                fontSize = 13.sp,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }

                    // Botón Login
                    Button(
                        onClick = {
                            if (email.isBlank() || password.isBlank()) {
                                errorMsg = "Completá todos los campos"
                                return@Button
                            }
                            isLoading = true
                            errorMsg = null
                            scope.launch {
                                try {
                                    val response = api.login(LoginRequest(email.trim(), password))
                                    if (response.isSuccessful) {
                                        val body = response.body()!!
                                        val token = body.token
                                        
                                        // Decodificar el token
                                        val claims = JwtUtils.decodeToken(token)
                                        
                                        // ✨ Guardado "a fuego" (commit) para evitar problemas de sincronización
                                        TokenStore.saveSessionImmediate(
                                            context,
                                            token = token,
                                            nombre = (claims["nombre"] as? String) ?: "Usuario",
                                            rol = (claims["rol"] as? String) ?: "vendedor",
                                            negocioId = (claims["negocio_id"] as? Number)?.toInt(),
                                            negocioNombre = (claims["negocio_nombre"] as? String) ?: "Mi Negocio"
                                        )
                                        onLoginSuccess()
                                    } else {
                                        errorMsg = when (response.code()) {
                                            401 -> "Usuario o contraseña incorrectos"
                                            403 -> "Sin acceso a esta aplicación"
                                            else -> "Error del servidor (${response.code()})"
                                        }
                                    }
                                } catch (e: Exception) {
                                    errorMsg = "Sin conexión. Verificá tu internet."
                                } finally {
                                    isLoading = false
                                }
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp),
                        enabled = !isLoading,
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = BaboonsBlue
                        )
                    ) {
                        if (isLoading) {
                            CircularProgressIndicator(
                                color = Color.White,
                                modifier = Modifier.size(20.dp),
                                strokeWidth = 2.dp
                            )
                        } else {
                            Text("Ingresar", fontWeight = FontWeight.SemiBold, fontSize = 16.sp)
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(32.dp))
            Text(
                text = "Multinegocio Baboons © 2026 - v1.8.3-staging", // ✨ Marca de versión inyectada
                fontSize = 12.sp,
                color = BaboonsTextSecondary
            )
        }
    }
}
