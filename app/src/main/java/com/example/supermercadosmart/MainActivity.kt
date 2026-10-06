package com.example.supermercadosmart

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
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

@Composable
fun SupermercadoSmartApp() {
    SupermercadoSmartTheme {
        Surface(modifier = Modifier.fillMaxSize()) {
            val viewModel: ShoppingViewModel = viewModel()
            MainScreen(viewModel = viewModel)
        }
    }
}
