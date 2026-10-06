package com.example.supermercadosmart.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.sp
import androidx.core.view.WindowCompat

private val LightColors = lightColorScheme(
    primary = ForestGreen,
    onPrimary = CardWhite,
    primaryContainer = SageLight,
    onPrimaryContainer = ForestGreen,
    secondary = ForestGreenLight,
    background = WarmSand,
    onBackground = TextDark,
    surface = CardWhite,
    onSurface = TextDark,
    error = AlertRed,
    errorContainer = AlertRedLight
)

private val DarkColors = darkColorScheme(
    primary = Sage,
    onPrimary = ForestGreen,
    primaryContainer = ForestGreenLight,
    onPrimaryContainer = SageLight,
    secondary = SageLight,
    background = Color0F(),
    onBackground = CardWhite,
    surface = Color1A(),
    onSurface = CardWhite,
    error = AlertRed,
    errorContainer = AlertRedLight
)

// pequenos helpers para evitar import direto de valores mágicos em dark theme
private fun Color0F() = androidx.compose.ui.graphics.Color(0xFF121212)
private fun Color1A() = androidx.compose.ui.graphics.Color(0xFF1E1E1E)

val AppTypography = Typography(
    headlineLarge = TextStyle(fontWeight = FontWeight.Bold, fontSize = 28.sp),
    headlineMedium = TextStyle(fontWeight = FontWeight.Bold, fontSize = 22.sp),
    titleLarge = TextStyle(fontWeight = FontWeight.SemiBold, fontSize = 20.sp),
    titleMedium = TextStyle(fontWeight = FontWeight.SemiBold, fontSize = 16.sp),
    bodyLarge = TextStyle(fontWeight = FontWeight.Normal, fontSize = 16.sp),
    bodyMedium = TextStyle(fontWeight = FontWeight.Normal, fontSize = 14.sp),
    labelLarge = TextStyle(fontWeight = FontWeight.Medium, fontSize = 14.sp)
)

@Composable
fun SupermercadoSmartTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColors else LightColors
    val view = LocalView.current
    if (!view.isInEditMode) {
        val context = LocalContext.current
        val window = (context as? Activity)?.window
        if (window != null) {
            window.statusBarColor = colorScheme.primary.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !darkTheme
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = AppTypography,
        content = content
    )
}
