package com.example.supermercadosmart.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.example.supermercadosmart.camera.BarcodeAnalyzer
import com.example.supermercadosmart.camera.CameraPreview

/**
 * Diálogo com leitor de código de barras em tempo real (ML Kit + CameraX).
 * Se a câmera não estiver disponível, permite digitar o código manualmente.
 */
@Composable
fun BarcodeScannerDialog(
    onBarcodeScanned: (String) -> Unit,
    onDismiss: () -> Unit
) {
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var manualCode by remember { mutableStateOf("") }

    val analyzer = remember {
        BarcodeAnalyzer { code ->
            onBarcodeScanned(code)
        }
    }

    Dialog(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(20.dp))
                .padding(16.dp)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 12.dp),
                contentAlignment = Alignment.CenterEnd
            ) {
                Text(
                    "Ler código de barras",
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.align(Alignment.CenterStart)
                )
                IconButton(onClick = onDismiss) {
                    Icon(Icons.Default.Close, contentDescription = "Fechar")
                }
            }

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(3f / 4f)
                    .background(Color.Black, RoundedCornerShape(16.dp)),
                contentAlignment = Alignment.Center
            ) {
                // Erros da câmera chegam pelo onError (o Compose não permite try/catch aqui)
                CameraPreview(
                    modifier = Modifier.fillMaxWidth(),
                    barcodeAnalyzer = analyzer,
                    onError = { errorMessage = "Câmera indisponível neste dispositivo." }
                )
                Icon(
                    Icons.Default.QrCodeScanner,
                    contentDescription = null,
                    tint = Color.White.copy(alpha = 0.5f),
                    modifier = Modifier.padding(48.dp)
                )
            }

            Text(
                "Aponte a câmera para o código de barras do produto.",
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(top = 12.dp, bottom = 8.dp)
            )

            Text(
                "Ou digite o código manualmente:",
                style = MaterialTheme.typography.labelLarge,
                modifier = Modifier.padding(top = 8.dp)
            )

            androidx.compose.material3.OutlinedTextField(
                value = manualCode,
                onValueChange = { manualCode = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 4.dp),
                placeholder = { Text("Ex: 7891000100103") },
                singleLine = true,
                trailingIcon = {
                    IconButton(onClick = {
                        if (manualCode.isNotBlank()) {
                            onBarcodeScanned(manualCode.trim())
                        }
                    }) {
                        Icon(Icons.Default.QrCodeScanner, contentDescription = "Buscar")
                    }
                }
            )

            if (errorMessage != null) {
                Text(
                    errorMessage ?: "",
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(top = 8.dp)
                )
            }
        }
    }
}
