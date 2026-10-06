package com.example.supermercadosmart.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

// Todas as cores do Material 3 definidas (as que ficam de fora caem no lilás padrão do Material)
private val LightColors = lightColorScheme(
    primary = ForestGreen,
    onPrimary = CardWhite,
    primaryContainer = SageLight,
    onPrimaryContainer = ForestGreen,
    inversePrimary = Sage,
    secondary = ForestGreenLight,
    onSecondary = CardWhite,
    secondaryContainer = SageLight,
    onSecondaryContainer = ForestGreen,
    tertiary = Color(0xFF8A6A1F),
    onTertiary = CardWhite,
    tertiaryContainer = Color(0xFFF5E3B3),
    onTertiaryContainer = Color(0xFF3A2C05),
    background = WarmSand,
    onBackground = TextDark,
    surface = CardWhite,
    onSurface = TextDark,
    surfaceVariant = WarmSandDark,
    onSurfaceVariant = Color(0xFF5B6660),
    surfaceTint = ForestGreen,
    inverseSurface = Color(0xFF2A332D),
    inverseOnSurface = Color(0xFFEEF3EF),
    error = AlertRed,
    onError = CardWhite,
    errorContainer = AlertRedLight,
    onErrorContainer = Color(0xFF7A1219),
    outline = Color(0xFF8A958E),
    outlineVariant = Color(0xFFD9D2C3),
    surfaceBright = CardWhite,
    surfaceDim = Color(0xFFE3DBCB),
    surfaceContainerLowest = CardWhite,
    surfaceContainerLow = Color(0xFFFBF7F0),
    surfaceContainer = Color(0xFFF7F2E9),
    surfaceContainerHigh = Color(0xFFF3EDE2),
    surfaceContainerHighest = Color(0xFFEDE6D8)
)

private val DarkColors = darkColorScheme(
    primary = Sage,
    onPrimary = Color(0xFF0B2A1C),
    primaryContainer = NightCartContainer,
    onPrimaryContainer = SageLight,
    inversePrimary = ForestGreenLight,
    secondary = Color(0xFFA9CBB6),
    onSecondary = Color(0xFF0B2A1C),
    secondaryContainer = Color(0xFF2A4536),
    onSecondaryContainer = SageLight,
    tertiary = SandAccent,
    onTertiary = Color(0xFF3A2C05),
    tertiaryContainer = Color(0xFF574316),
    onTertiaryContainer = Color(0xFFF5E3B3),
    background = NightBackground,
    onBackground = NightOnSurface,
    surface = NightSurface,
    onSurface = NightOnSurface,
    surfaceVariant = NightSurfaceVariant,
    onSurfaceVariant = NightOnSurfaceMuted,
    surfaceTint = Sage,
    inverseSurface = NightOnSurface,
    inverseOnSurface = Color(0xFF1B2620),
    error = AlertRedBright,
    onError = Color(0xFF4A0A10),
    errorContainer = Color(0xFF5C1A20),
    onErrorContainer = AlertRedLight,
    outline = NightOutline,
    outlineVariant = Color(0xFF34433A),
    surfaceBright = Color(0xFF33413A),
    surfaceDim = NightBackground,
    surfaceContainerLowest = Color(0xFF0A110D),
    surfaceContainerLow = Color(0xFF131D17),
    surfaceContainer = Color(0xFF18241D),
    surfaceContainerHigh = Color(0xFF1F2C24),
    surfaceContainerHighest = Color(0xFF27352D)
)

/** Se o tema escuro está ativo (para desenhos e detalhes que não vêm do esquema de cores). */
val LocalDarkTheme = staticCompositionLocalOf { false }

@Composable
fun SupermercadoSmartTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    // As barras do sistema são ajustadas na MainActivity (enableEdgeToEdge), conforme o tema
    CompositionLocalProvider(LocalDarkTheme provides darkTheme) {
        MaterialTheme(
            colorScheme = if (darkTheme) DarkColors else LightColors,
            typography = AppTypography,
            content = content
        )
    }
}
