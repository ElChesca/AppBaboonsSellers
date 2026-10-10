package com.baboons.sellers.ui.theme

import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

// ✨ Sellers Theme: Light Theme by Default (Figma style)
private val LightColorScheme = lightColorScheme(
    primary = BaboonsBlue,
    onPrimary = Color.White,
    primaryContainer = BaboonsBlue.copy(alpha = 0.1f),
    onPrimaryContainer = BaboonsBlue,
    
    secondary = BaboonsSuccess,
    onSecondary = Color.White,
    
    background = BaboonsBackground,
    onBackground = BaboonsTextPrimary,
    
    surface = BaboonsSurface,
    onSurface = BaboonsTextPrimary,
    
    surfaceVariant = BaboonsBorder,
    onSurfaceVariant = BaboonsTextSecondary,
    
    error = BaboonsError,
    onError = Color.White,
    
    outline = BaboonsBorder
)

@Composable
fun SellersTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = LightColorScheme,
        typography = Typography(),
        content = content
    )
}