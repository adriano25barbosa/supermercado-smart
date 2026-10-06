package com.example.supermercadosmart.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.supermercadosmart.data.Item
import com.example.supermercadosmart.ui.theme.AlertRed
import com.example.supermercadosmart.ui.theme.SuccessGreen
import java.text.NumberFormat
import java.util.Locale

private val currencyFormat = NumberFormat.getCurrencyInstance(Locale("pt", "BR"))

@Composable
fun SubtotalCard(
    items: List<Item>,
    maxBudget: Double,
    onBudgetClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val total = items.sumOf { it.totalPrice }
    val itemCount = items.sumOf { it.quantity }

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primary),
        elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                "No carrinho ($itemCount ${if (itemCount == 1) "item" else "itens"})",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.85f)
            )
            Text(
                currencyFormat.format(total),
                style = MaterialTheme.typography.headlineLarge,
                color = MaterialTheme.colorScheme.onPrimary,
                fontWeight = FontWeight.Bold
            )

            Row(
                modifier = Modifier
                    .padding(top = 12.dp)
                    .clickable { onBudgetClick() },
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "Orçamento máximo: ",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.85f)
                )
                Text(
                    if (maxBudget > 0) currencyFormat.format(maxBudget) else "definir",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onPrimary,
                    fontWeight = FontWeight.Bold
                )
            }

            if (maxBudget > 0) {
                val remaining = maxBudget - total
                val isOverBudget = remaining < 0

                Row(
                    modifier = Modifier
                        .padding(top = 10.dp)
                        .background(
                            if (isOverBudget) AlertRed.copy(alpha = 0.15f) else SuccessGreen.copy(alpha = 0.15f),
                            RoundedCornerShape(12.dp)
                        )
                        .padding(horizontal = 14.dp, vertical = 8.dp)
                ) {
                    val message = when {
                        remaining > 0 -> "Faltam ${currencyFormat.format(remaining)} para o orçamento"
                        remaining == 0.0 -> "Você atingiu exatamente o orçamento"
                        else -> "Orçamento ultrapassado em ${currencyFormat.format(-remaining)}"
                    }
                    Text(
                        message,
                        style = MaterialTheme.typography.bodyMedium,
                        color = if (isOverBudget) AlertRed else SuccessGreen,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }
    }
}
