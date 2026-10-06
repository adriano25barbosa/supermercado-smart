package com.example.supermercadosmart

import android.graphics.Color
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.supermercadosmart.data.ThemeMode
import com.example.supermercadosmart.data.ThemePreference
import com.example.supermercadosmart.ui.screens.MainScreen
import com.example.supermercadosmart.ui.theme.SupermercadoSmartTheme
import com.example.supermercadosmart.viewmodel.ShoppingViewModel

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            SupermercadoSmartApp()
        }
    }
}

// Véus da barra de navegação (os mesmos tons padrão do Android)
private val LightNavScrim = Color.argb(0xE6, 0xFF, 0xFF, 0xFF)
private val DarkNavScrim = Color.argb(0x80, 0x1B, 0x1B, 0x1B)

@Composable
fun SupermercadoSmartApp() {
    val context = LocalContext.current
    var themeMode by remember { mutableStateOf(ThemePreference.load(context)) }
    val darkTheme = when (themeMode) {
        ThemeMode.SYSTEM -> isSystemInDarkTheme()
        ThemeMode.LIGHT -> false
        ThemeMode.DARK -> true
    }

    // Ícones claros/escuros nas barras do sistema conforme o tema escolhido (não só o do celular)
    LaunchedEffect(darkTheme) {
        (context as? ComponentActivity)?.enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.auto(Color.TRANSPARENT, Color.TRANSPARENT) { darkTheme },
            navigationBarStyle = SystemBarStyle.auto(LightNavScrim, DarkNavScrim) { darkTheme }
        )
    }

    SupermercadoSmartTheme(darkTheme = darkTheme) {
        Surface(modifier = Modifier.fillMaxSize()) {
            val viewModel: ShoppingViewModel = viewModel()
            MainScreen(
                viewModel = viewModel,
                themeMode = themeMode,
                onThemeModeChange = { mode ->
                    themeMode = mode
                    ThemePreference.save(context, mode)
                }
            )
        }
    }
}
