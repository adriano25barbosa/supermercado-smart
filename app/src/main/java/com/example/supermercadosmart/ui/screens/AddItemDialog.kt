package com.example.supermercadosmart.ui.screens

import android.net.Uri
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddAPhoto
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import coil.compose.rememberAsyncImagePainter

data class NewItemResult(
    val name: String,
    val unitPrice: Double,
    val quantity: Int,
    val imageUri: String?,
    val barcode: String?
)

@Composable
fun AddItemDialog(
    onConfirm: (NewItemResult) -> Unit,
    onDismiss: () -> Unit
) {
    var name by remember { mutableStateOf("") }
    var priceText by remember { mutableStateOf("") }
    var quantityText by remember { mutableStateOf("1") }
    var imageUri by remember { mutableStateOf<Uri?>(null) }
    var barcode by remember { mutableStateOf<String?>(null) }

    var showPhotoDialog by remember { mutableStateOf(false) }
    var showBarcodeDialog by remember { mutableStateOf(false) }

    val isValid = name.isNotBlank() && (priceText.toDoubleOrNull() ?: -1.0) >= 0.0

    Dialog(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(20.dp))
                .padding(20.dp)
        ) {
            Text("Adicionar produto", style = MaterialTheme.typography.titleLarge)

            Box(
                modifier = Modifier
                    .padding(vertical = 12.dp)
                    .size(90.dp)
                    .align(Alignment.CenterHorizontally)
                    .background(MaterialTheme.colorScheme.primaryContainer, CircleShape)
                    .border(1.dp, MaterialTheme.colorScheme.primary, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                if (imageUri != null) {
                    Image(
                        painter = rememberAsyncImagePainter(imageUri),
                        contentDescription = "Foto do produto",
                        modifier = Modifier
                            .size(90.dp)
                            .background(MaterialTheme.colorScheme.primaryContainer, CircleShape),
                        contentScale = ContentScale.Crop
                    )
                }
                IconButton(
                    onClick = { showPhotoDialog = true },
                    modifier = Modifier.align(Alignment.BottomEnd)
                ) {
                    Icon(
                        Icons.Default.AddAPhoto,
                        contentDescription = "Adicionar foto",
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            }

            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text("Nome do produto") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedTextField(
                    value = priceText,
                    onValueChange = { priceText = it.filter { c -> c.isDigit() || c == '.' || c == ',' }.replace(',', '.') },
                    label = { Text("Preço (R$)") },
                    modifier = Modifier.weight(1f),
                    singleLine = true
                )
                OutlinedTextField(
                    value = quantityText,
                    onValueChange = { quantityText = it.filter { c -> c.isDigit() } },
                    label = { Text("Qtd.") },
                    modifier = Modifier.weight(0.6f),
                    singleLine = true
                )
            }

            OutlinedButton(
                onClick = { showBarcodeDialog = true },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 12.dp)
            ) {
                Icon(Icons.Default.QrCodeScanner, contentDescription = null)
                Text(
                    text = if (barcode == null) "  Ler código de barras" else "  Código: $barcode",
                    modifier = Modifier.padding(start = 4.dp)
                )
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 20.dp),
                horizontalArrangement = Arrangement.End
            ) {
                TextButton(onClick = onDismiss) {
                    Text("Cancelar")
                }
                Button(
                    onClick = {
                        val price = priceText.toDoubleOrNull() ?: 0.0
                        val qty = quantityText.toIntOrNull()?.coerceAtLeast(1) ?: 1
                        onConfirm(
                            NewItemResult(
                                name = name.trim(),
                                unitPrice = price,
                                quantity = qty,
                                imageUri = imageUri?.toString(),
                                barcode = barcode
                            )
                        )
                    },
                    enabled = isValid,
                    modifier = Modifier.padding(start = 8.dp)
                ) {
                    Text("Salvar")
                }
            }
        }
    }

    if (showPhotoDialog) {
        PhotoCaptureDialog(
            onPhotoCaptured = { uri -> imageUri = uri },
            onDismiss = { showPhotoDialog = false }
        )
    }

    if (showBarcodeDialog) {
        BarcodeScannerDialog(
            onBarcodeScanned = { code ->
                barcode = code
                showBarcodeDialog = false
            },
            onDismiss = { showBarcodeDialog = false }
        )
    }
}
