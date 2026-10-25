package com.example.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val DarkColorScheme = darkColorScheme(
    primary = EmeraldPrimary,
    onPrimary = Color.Black,
    primaryContainer = EmeraldContainer,
    onPrimaryContainer = Color(0xFFA7F3D0),
    secondary = SemaforoYellow,
    onSecondary = Color.Black,
    error = SemaforoRed,
    onError = Color.White,
    background = BackgroundDark,
    onBackground = Color(0xFFF1F5F9),
    surface = SurfaceDark,
    onSurface = Color(0xFFE2E8F0),
    surfaceVariant = SurfaceCard,
    onSurfaceVariant = Color(0xFF94A3B8),
    outline = BorderDark
)

@Composable
fun AgroVigiaTheme(
    darkTheme: Boolean = true,
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = DarkColorScheme,
        content = content
    )
}
