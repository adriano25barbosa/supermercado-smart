package com.example.supermercadosmart.ui.screens

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BrightnessAuto
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.supermercadosmart.data.ThemeMode

/**
 * Seção "Tema" dos menus ⋮ (Sistema / Claro / Escuro, com ✓ no escolhido).
 * Usada na tela "Minhas listas" e dentro de cada lista.
 */
@Composable
fun ThemeMenuSection(
    themeMode: ThemeMode,
    onSelect: (ThemeMode) -> Unit,
    showDivider: Boolean = true
) {
    if (showDivider) {
        HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
    }
    Text(
        "Tema",
        style = MaterialTheme.typography.labelMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)
    )
    ThemeMode.values().forEach { mode ->
        DropdownMenuItem(
            text = { Text(mode.label) },
            leadingIcon = {
                Icon(
                    when (mode) {
                        ThemeMode.SYSTEM -> Icons.Default.BrightnessAuto
                        ThemeMode.LIGHT -> Icons.Default.LightMode
                        ThemeMode.DARK -> Icons.Default.DarkMode
                    },
                    contentDescription = null
                )
            },
            trailingIcon = {
                if (mode == themeMode) {
                    Icon(
                        Icons.Default.Check,
                        contentDescription = "Selecionado",
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            },
            onClick = { onSelect(mode) }
        )
    }
}
