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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddAPhoto
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import coil.compose.rememberAsyncImagePainter
import com.example.supermercadosmart.data.Category
import com.example.supermercadosmart.data.LookupResult
import com.example.supermercadosmart.data.LookupSource

data class NewItemResult(
    val name: String,
    val unitPrice: Double,
    val quantity: Int,
    val imageUri: String?,
    val barcode: String?,
    val category: String
)

@Composable
fun AddItemDialog(
    onConfirm: (NewItemResult) -> Unit,
    onDismiss: () -> Unit,
    // Código já lido pelo "Escanear código" do botão + (vem preenchido no diálogo)
    initialBarcode: String? = null,
    // Busca nome/foto/categoria pelo código (listas do app e Open Food Facts); null = não busca
    onLookupBarcode: (suspend (String) -> LookupResult)? = null
) {
    var name by remember { mutableStateOf("") }
    var priceText by remember { mutableStateOf("") }
    var quantityText by remember { mutableStateOf("1") }
    var imageUri by remember { mutableStateOf<Uri?>(null) }
    var barcode by remember { mutableStateOf(initialBarcode) }

    // Categoria: o app sugere pelo nome até a pessoa escolher um chip por conta própria
    var category by remember { mutableStateOf(Category.OUTROS) }
    var categoryChosenByUser by remember { mutableStateOf(false) }

    // Busca pelo código: o que o app preencheu sozinho pode ser trocado por uma nova leitura;
    // o que a pessoa digitou/fotografou nunca é sobrescrito
    var lookupState by remember { mutableStateOf<LookupUiState>(LookupUiState.Idle) }
    var lookupAttempt by remember { mutableStateOf(0) }
    var autoFilledName by remember { mutableStateOf<String?>(null) }
    var autoFilledImage by remember { mutableStateOf<Uri?>(null) }

    LaunchedEffect(barcode, lookupAttempt) {
        val code = barcode
        if (code == null || onLookupBarcode == null) return@LaunchedEffect
        lookupState = LookupUiState.Loading
        val result = onLookupBarcode(code)
        lookupState = when (result) {
            is LookupResult.Found -> {
                val foundName = result.name
                if (!foundName.isNullOrBlank() && (name.isBlank() || name == autoFilledName)) {
                    name = foundName
                    autoFilledName = foundName
                    if (!categoryChosenByUser) category = result.category ?: Category.guess(foundName)
                } else if (result.category != null && !categoryChosenByUser && name.isBlank()) {
                    category = result.category
                }
                val foundImage = result.imageUri
                if (foundImage != null && (imageUri == null || imageUri == autoFilledImage)) {
                    val uri = Uri.parse(foundImage)
                    imageUri = uri
                    autoFilledImage = uri
                }
                LookupUiState.Found(result.source)
            }
            LookupResult.NotFound -> LookupUiState.NotFound
            LookupResult.Failed -> LookupUiState.Failed
        }
    }

    var showPhotoDialog by remember { mutableStateOf(false) }
    var showBarcodeDialog by remember { mutableStateOf(false) }

    // O preço aparece com vírgula (ex: 5,99) e é convertido para número na hora de usar
    val price = priceText.replace(',', '.').toDoubleOrNull()
    val isValid = name.isNotBlank() && (price ?: -1.0) >= 0.0

    Dialog(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(20.dp))
                .verticalScroll(rememberScrollState())
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
                onValueChange = {
                    name = it
                    if (!categoryChosenByUser) category = Category.guess(it)
                },
                label = { Text("Nome do produto") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            Text(
                "Categoria",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 12.dp, bottom = 4.dp)
            )
            CategoryChipsRow(
                selected = category,
                onSelect = {
                    category = it
                    categoryChosenByUser = true
                }
            )

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedTextField(
                    value = priceText,
                    onValueChange = { input ->
                        // Só números e uma vírgula
                        val cleaned = input
                            .filter { c -> c.isDigit() || c == '.' || c == ',' }
                            .replace('.', ',')
                        if (cleaned.count { it == ',' } <= 1) priceText = cleaned
                    },
                    label = { Text("Preço (R$)") },
                    modifier = Modifier.weight(1f),
                    singleLine = true,
                    // Teclado numérico com vírgula
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Decimal,
                        imeAction = ImeAction.Next
                    )
                )
                OutlinedTextField(
                    value = quantityText,
                    onValueChange = { quantityText = it.filter { c -> c.isDigit() } },
                    label = { Text("Qtd.") },
                    modifier = Modifier.weight(0.6f),
                    singleLine = true,
                    // Teclado só de números
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Number,
                        imeAction = ImeAction.Done
                    )
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

            LookupStatus(
                state = lookupState,
                onRetry = { lookupAttempt++ }
            )

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
                        val qty = quantityText.toIntOrNull()?.coerceAtLeast(1) ?: 1
                        onConfirm(
                            NewItemResult(
                                name = name.trim(),
                                unitPrice = price ?: 0.0,
                                quantity = qty,
                                imageUri = imageUri?.toString(),
                                barcode = barcode,
                                category = category.name
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

/** Situação da busca pelo código de barras, mostrada logo abaixo do botão do código. */
private sealed class LookupUiState {
    object Idle : LookupUiState()
    object Loading : LookupUiState()
    data class Found(val source: LookupSource) : LookupUiState()
    object NotFound : LookupUiState()
    object Failed : LookupUiState()
}

@Composable
private fun LookupStatus(state: LookupUiState, onRetry: () -> Unit) {
    when (state) {
        LookupUiState.Idle -> Unit
        LookupUiState.Loading -> Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            CircularProgressIndicator(
                modifier = Modifier.size(16.dp),
                strokeWidth = 2.dp,
                color = MaterialTheme.colorScheme.primary
            )
            Text(
                "Buscando produto…",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(start = 8.dp)
            )
        }
        is LookupUiState.Found -> LookupMessage(
            icon = Icons.Default.CheckCircle,
            text = when (state.source) {
                LookupSource.MY_LISTS -> "Encontrado nas suas listas"
                LookupSource.OPEN_FOOD_FACTS -> "Encontrado no Open Food Facts"
            },
            tint = MaterialTheme.colorScheme.primary
        )
        LookupUiState.NotFound -> LookupMessage(
            icon = Icons.Outlined.Info,
            text = "Produto não encontrado, digite o nome",
            tint = MaterialTheme.colorScheme.onSurfaceVariant
        )
        LookupUiState.Failed -> Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(modifier = Modifier.weight(1f)) {
                LookupMessage(
                    icon = Icons.Default.CloudOff,
                    text = "Sem internet",
                    tint = MaterialTheme.colorScheme.error
                )
            }
            TextButton(onClick = onRetry, modifier = Modifier.padding(top = 4.dp)) {
                Text("Tentar de novo")
            }
        }
    }
}

@Composable
private fun LookupMessage(
    icon: ImageVector,
    text: String,
    tint: androidx.compose.ui.graphics.Color
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(16.dp))
        Text(
            text,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(start = 8.dp)
        )
    }
}
