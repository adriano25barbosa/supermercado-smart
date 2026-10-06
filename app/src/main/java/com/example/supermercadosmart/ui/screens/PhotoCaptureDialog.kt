package com.example.supermercadosmart.ui.screens

import android.net.Uri
import androidx.camera.core.ImageCapture
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Close
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.example.supermercadosmart.camera.CameraPreview
import com.example.supermercadosmart.camera.takePhoto

/**
 * Diálogo com preview de câmera para capturar a foto de um produto.
 * Protegido: se a câmera não estiver disponível, mostra mensagem em vez de travar.
 */
@Composable
fun PhotoCaptureDialog(
    onPhotoCaptured: (Uri) -> Unit,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    var imageCapture by remember { mutableStateOf<ImageCapture?>(null) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

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
                    "Capturar foto do produto",
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
                    onImageCaptureReady = { capture -> imageCapture = capture },
                    onError = { errorMessage = "Câmera indisponível neste dispositivo." }
                )

                if (errorMessage != null) {
                    Text(
                        errorMessage ?: "",
                        color = Color.White,
                        modifier = Modifier.padding(16.dp)
                    )
                }
            }

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 16.dp),
                contentAlignment = Alignment.Center
            ) {
                IconButton(
                    onClick = {
                        val capture = imageCapture
                        if (capture != null) {
                            takePhoto(
                                context = context,
                                imageCapture = capture,
                                onSaved = { uri ->
                                    onPhotoCaptured(uri)
                                    onDismiss()
                                },
                                onError = { msg -> errorMessage = msg }
                            )
                        } else {
                            errorMessage = "Câmera ainda não está pronta. Tente novamente."
                        }
                    },
                    modifier = Modifier
                        .background(MaterialTheme.colorScheme.primary, CircleShape)
                        .padding(4.dp)
                ) {
                    Icon(
                        Icons.Default.CameraAlt,
                        contentDescription = "Capturar",
                        tint = MaterialTheme.colorScheme.onPrimary,
                        modifier = Modifier.padding(12.dp)
                    )
                }
            }

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
