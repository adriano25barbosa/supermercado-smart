package com.example.supermercadosmart.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Savings
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.supermercadosmart.data.BudgetBand
import com.example.supermercadosmart.data.BudgetStatus
import com.example.supermercadosmart.data.Item
import com.example.supermercadosmart.ui.theme.AlertRedBright
import com.example.supermercadosmart.ui.theme.CardWhite
import com.example.supermercadosmart.ui.theme.ForestGreen
import com.example.supermercadosmart.ui.theme.Sage
import com.example.supermercadosmart.ui.theme.SandAccent
import java.text.NumberFormat
import java.util.Locale

private val currencyFormat = NumberFormat.getCurrencyInstance(Locale("pt", "BR"))

// O card usa sempre o verde floresta, para as cores da barra terem contraste
private val CardBackground = ForestGreen
private val OnCard = CardWhite
private val OnCardMuted = CardWhite.copy(alpha = 0.75f)
private val TrackColor = CardWhite.copy(alpha = 0.18f)

private fun bandColor(band: BudgetBand): Color = when (band) {
    BudgetBand.SAFE -> Sage
    BudgetBand.WARNING -> SandAccent
    BudgetBand.OVER -> AlertRedBright
}

/**
 * Card de orçamento no topo da lista.
 *
 * @param collapsed quando true (lista rolada), mostra só uma faixa compacta
 * @param onCollapsedClick toque na faixa compacta (ex.: voltar ao topo)
 */
@Composable
fun SubtotalCard(
    items: List<Item>,
    maxBudget: Double,
    onBudgetClick: () -> Unit,
    modifier: Modifier = Modifier,
    collapsed: Boolean = false,
    onCollapsedClick: () -> Unit = {}
) {
    val total = items.sumOf { it.totalPrice }
    val itemCount = items.sumOf { it.quantity }
    val status = BudgetStatus(total = total, maxBudget = maxBudget)
    val itemsLabel = "$itemCount ${if (itemCount == 1) "item" else "itens"}"

    val accent by animateColorAsState(
        targetValue = bandColor(status.band),
        animationSpec = tween(400),
        label = "corBarra"
    )
    val padding by animateDpAsState(if (collapsed) 12.dp else 20.dp, label = "padding")
    val corner by animateDpAsState(if (collapsed) 18.dp else 24.dp, label = "canto")

    Card(
        modifier = modifier
            .fillMaxWidth()
            .animateContentSize(),
        shape = RoundedCornerShape(corner),
        colors = CardDefaults.cardColors(containerColor = CardBackground),
        elevation = CardDefaults.cardElevation(defaultElevation = if (collapsed) 4.dp else 6.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .then(if (collapsed) Modifier.clickable { onCollapsedClick() } else Modifier)
                .padding(horizontal = padding + 4.dp, vertical = padding)
        ) {
            if (collapsed) {
                CompactHeader(status, total, accent, onBudgetClick)
            } else {
                ExpandedHeader(status, total, itemsLabel, accent)
            }

            if (status.hasBudget) {
                Spacer(Modifier.height(if (collapsed) 8.dp else 14.dp))
                BudgetProgressBar(
                    fraction = status.usedFraction.toFloat(),
                    color = accent,
                    height = if (collapsed) 6.dp else 10.dp
                )
            }

            AnimatedVisibility(
                visible = !collapsed,
                enter = fadeIn() + expandVertically(),
                exit = fadeOut() + shrinkVertically()
            ) {
                Footer(status, total, itemsLabel, maxBudget, onBudgetClick)
            }
        }
    }
}

@Composable
private fun ExpandedHeader(status: BudgetStatus, total: Double, itemsLabel: String, accent: Color) {
    if (!status.hasBudget) {
        Text("Subtotal · $itemsLabel", style = MaterialTheme.typography.labelLarge, color = OnCardMuted)
        Text(
            currencyFormat.format(total),
            style = MaterialTheme.typography.headlineLarge,
            color = OnCard,
            fontWeight = FontWeight.Bold
        )
        return
    }

    Row(verticalAlignment = Alignment.Bottom) {
        Column(Modifier.weight(1f)) {
            Text(
                if (status.isOver) "Passou do orçamento" else "Restam",
                style = MaterialTheme.typography.labelLarge,
                color = if (status.isOver) AlertRedBright else OnCardMuted
            )
            Text(
                currencyFormat.format(kotlin.math.abs(status.remaining)),
                style = MaterialTheme.typography.headlineLarge.copy(fontSize = MaterialTheme.typography.headlineLarge.fontSize * 1.15f),
                color = if (status.isOver) AlertRedBright else OnCard,
                fontWeight = FontWeight.Bold
            )
        }
        PercentChip(status.usedPercent, accent)
    }
}

@Composable
private fun CompactHeader(status: BudgetStatus, total: Double, accent: Color, onBudgetClick: () -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        if (status.hasBudget) {
            Text(
                if (status.isOver) "Passou " else "Restam ",
                style = MaterialTheme.typography.bodyMedium,
                color = if (status.isOver) AlertRedBright else OnCardMuted
            )
            Text(
                currencyFormat.format(kotlin.math.abs(status.remaining)),
                style = MaterialTheme.typography.titleMedium,
                color = if (status.isOver) AlertRedBright else OnCard,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.weight(1f)
            )
            Text(
                "${status.usedPercent}%",
                style = MaterialTheme.typography.labelLarge,
                color = accent,
                fontWeight = FontWeight.Bold
            )
        } else {
            Text("Subtotal ", style = MaterialTheme.typography.bodyMedium, color = OnCardMuted)
            Text(
                currencyFormat.format(total),
                style = MaterialTheme.typography.titleMedium,
                color = OnCard,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.weight(1f)
            )
            Text(
                "Definir orçamento",
                style = MaterialTheme.typography.labelLarge,
                color = SandAccent,
                modifier = Modifier.clickable { onBudgetClick() }
            )
        }
    }
}

@Composable
private fun Footer(
    status: BudgetStatus,
    total: Double,
    itemsLabel: String,
    maxBudget: Double,
    onBudgetClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        if (status.hasBudget) {
            Text(
                "Subtotal ${currencyFormat.format(total)} · $itemsLabel",
                style = MaterialTheme.typography.bodyMedium,
                color = OnCardMuted
            )
            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(10.dp))
                    .clickable { onBudgetClick() }
                    .padding(horizontal = 6.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    currencyFormat.format(maxBudget),
                    style = MaterialTheme.typography.bodyMedium,
                    color = OnCard,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(Modifier.width(4.dp))
                Icon(
                    Icons.Default.Edit,
                    contentDescription = "Editar orçamento",
                    tint = OnCardMuted,
                    modifier = Modifier.size(16.dp)
                )
            }
        } else {
            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(50))
                    .background(SandAccent)
                    .clickable { onBudgetClick() }
                    .padding(horizontal = 14.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    Icons.Default.Savings,
                    contentDescription = null,
                    tint = ForestGreen,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(Modifier.width(6.dp))
                Text(
                    "Definir orçamento",
                    style = MaterialTheme.typography.labelLarge,
                    color = ForestGreen,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    }
}

@Composable
private fun PercentChip(percent: Int, accent: Color) {
    Box(
        modifier = Modifier
            .padding(bottom = 6.dp)
            .clip(RoundedCornerShape(50))
            .background(accent.copy(alpha = 0.22f))
            .padding(horizontal = 10.dp, vertical = 4.dp)
    ) {
        Text(
            "$percent%",
            style = MaterialTheme.typography.labelLarge,
            color = accent,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
private fun BudgetProgressBar(fraction: Float, color: Color, height: Dp) {
    val animated by animateFloatAsState(
        targetValue = fraction.coerceIn(0f, 1f),
        animationSpec = tween(500),
        label = "progresso"
    )
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(height)
            .clip(RoundedCornerShape(50))
            .background(TrackColor)
    ) {
        if (animated > 0f) {
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .fillMaxWidth(animated)
                    .clip(RoundedCornerShape(50))
                    .background(color)
            )
        }
    }
}
