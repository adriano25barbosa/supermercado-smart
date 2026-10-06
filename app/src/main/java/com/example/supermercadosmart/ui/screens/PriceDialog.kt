package com.example.supermercadosmart.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.util.Locale

private val priceInputFormat = DecimalFormat("0.00", DecimalFormatSymbols(Locale("pt", "BR")))

/**
 * Informar o preço de um item. Abre ao tocar no card e, para itens sem preço,
 * sozinho ao marcar no carrinho ([fromCart] = true: botão "Pular").
 */
@Composable
fun PriceDialog(
    itemName: String,
    currentPrice: Double,
    quantity: Int,
    fromCart: Boolean,
    onConfirm: (Double) -> Unit,
    onDismiss: () -> Unit
) {
    var field by remember {
        val start = if (currentPrice > 0) priceInputFormat.format(currentPrice) else ""
        mutableStateOf(TextFieldValue(start, selection = TextRange(0, start.length)))
    }
    val price = field.text.replace(',', '.').toDoubleOrNull()
    val isValid = price != null && price >= 0.0
    val focusRequester = remember { FocusRequester() }

    LaunchedEffect(Unit) { focusRequester.requestFocus() }

    fun confirm() {
        if (isValid) onConfirm(price ?: 0.0)
    }

    Dialog(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(20.dp))
                .padding(20.dp)
        ) {
            Text(
                if (fromCart) "Quanto custou?" else "Preço do item",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
            Text(
                itemName,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(top = 4.dp, bottom = 16.dp)
            )

            OutlinedTextField(
                value = field,
                onValueChange = { input ->
                    // Só números e uma vírgula
                    val cleaned = input.text
                        .filter { c -> c.isDigit() || c == '.' || c == ',' }
                        .replace('.', ',')
                    if (cleaned.count { it == ',' } <= 1) field = input.copy(text = cleaned)
                },
                label = { Text("Preço por unidade (R$)") },
                supportingText = if (quantity > 1) {
                    { Text("Quantidade: $quantity — o total é calculado sozinho") }
                } else null,
                modifier = Modifier
                    .fillMaxWidth()
                    .focusRequester(focusRequester),
                singleLine = true,
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Decimal,
                    imeAction = ImeAction.Done
                ),
                keyboardActions = KeyboardActions(onDone = { confirm() })
            )

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 20.dp),
                horizontalArrangement = Arrangement.End
            ) {
                TextButton(onClick = onDismiss) {
                    Text(if (fromCart) "Pular" else "Cancelar")
                }
                Button(
                    onClick = { confirm() },
                    enabled = isValid,
                    modifier = Modifier.padding(start = 8.dp)
                ) {
                    Text("Salvar")
                }
            }
        }
    }
}
