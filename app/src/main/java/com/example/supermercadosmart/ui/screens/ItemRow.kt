@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.example.supermercadosmart.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.RemoveShoppingCart
import androidx.compose.material.icons.filled.ShoppingBasket
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material.icons.outlined.Circle
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Text
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.onLongClick
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.rememberAsyncImagePainter
import com.example.supermercadosmart.data.Item
import com.example.supermercadosmart.ui.theme.AlertRed
import com.example.supermercadosmart.ui.theme.CardWhite
import com.example.supermercadosmart.ui.theme.SuccessGreen
import java.text.NumberFormat
import java.util.Locale

private val currencyFormat = NumberFormat.getCurrencyInstance(Locale("pt", "BR"))
private val CardShape = RoundedCornerShape(16.dp)

/**
 * Card de um item da lista, com deslizar:
 *  - para a direita → marca/desmarca no carrinho
 *  - para a esquerda → exclui
 * Toque longo no card → [onLongPress] (trocar a categoria).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ItemRow(
    item: Item,
    onToggleInCart: () -> Unit,
    onIncrement: () -> Unit,
    onDecrement: () -> Unit,
    onSwipeToggleInCart: () -> Unit,
    onSwipeDelete: () -> Unit,
    modifier: Modifier = Modifier,
    onLongPress: () -> Unit = {}
) {
    // As ações mudam a cada recomposição (item atualizado); o estado do deslize é lembrado uma vez só
    val currentSwipeToggle by rememberUpdatedState(onSwipeToggleInCart)
    val currentSwipeDelete by rememberUpdatedState(onSwipeDelete)
    // Evita disparar a ação duas vezes no mesmo gesto
    val lastSwipe = remember { longArrayOf(0L) }

    val dismissState = rememberSwipeToDismissBoxState(
        confirmValueChange = { value ->
            val now = System.currentTimeMillis()
            val repeated = now - lastSwipe[0] < 600
            when (value) {
                SwipeToDismissBoxValue.StartToEnd -> {
                    if (!repeated) {
                        lastSwipe[0] = now
                        currentSwipeToggle()
                    }
                    false // volta para o lugar; o item só muda de seção
                }
                SwipeToDismissBoxValue.EndToStart -> {
                    if (!repeated) {
                        lastSwipe[0] = now
                        currentSwipeDelete()
                    }
                    true // sai da tela
                }
                SwipeToDismissBoxValue.Settled -> true
            }
        }
    )

    SwipeToDismissBox(
        state = dismissState,
        modifier = modifier.padding(vertical = 5.dp),
        backgroundContent = {
            SwipeBackground(
                direction = dismissState.dismissDirection,
                inCart = item.inCart
            )
        }
    ) {
        ItemCard(
            item = item,
            onToggleInCart = onToggleInCart,
            onIncrement = onIncrement,
            onDecrement = onDecrement,
            onLongPress = onLongPress
        )
    }
}

@Composable
private fun SwipeBackground(direction: SwipeToDismissBoxValue, inCart: Boolean) {
    val color by animateColorAsState(
        targetValue = when (direction) {
            SwipeToDismissBoxValue.StartToEnd -> SuccessGreen
            SwipeToDismissBoxValue.EndToStart -> AlertRed
            SwipeToDismissBoxValue.Settled -> Color.Transparent
        },
        label = "swipeBackground"
    )
    Box(
        modifier = Modifier
            .fillMaxSize()
            .clip(CardShape)
            .background(color)
            .padding(horizontal = 24.dp),
        contentAlignment = if (direction == SwipeToDismissBoxValue.EndToStart) {
            Alignment.CenterEnd
        } else {
            Alignment.CenterStart
        }
    ) {
        when (direction) {
            SwipeToDismissBoxValue.StartToEnd -> SwipeLabel(
                icon = if (inCart) Icons.Default.RemoveShoppingCart else Icons.Default.ShoppingCart,
                text = if (inCart) "Tirar do carrinho" else "No carrinho"
            )
            SwipeToDismissBoxValue.EndToStart -> SwipeLabel(
                icon = Icons.Default.Delete,
                text = "Excluir"
            )
            SwipeToDismissBoxValue.Settled -> Unit
        }
    }
}

@Composable
private fun SwipeLabel(icon: androidx.compose.ui.graphics.vector.ImageVector, text: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(icon, contentDescription = null, tint = CardWhite)
        Spacer(Modifier.width(8.dp))
        Text(text, color = CardWhite, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
private fun ItemCard(
    item: Item,
    onToggleInCart: () -> Unit,
    onIncrement: () -> Unit,
    onDecrement: () -> Unit,
    onLongPress: () -> Unit
) {
    val colors = MaterialTheme.colorScheme
    val haptic = LocalHapticFeedback.current
    val currentLongPress by rememberUpdatedState(onLongPress)
    val containerColor by animateColorAsState(
        targetValue = if (item.inCart) colors.primaryContainer else colors.surface,
        label = "cardColor"
    )
    val contentAlpha by animateFloatAsState(
        targetValue = if (item.inCart) 0.6f else 1f,
        label = "contentAlpha"
    )

    Card(
        modifier = Modifier
            .fillMaxWidth()
            // Toque longo em qualquer parte do card (fora dos botões) troca a categoria
            .pointerInput(Unit) {
                detectTapGestures(
                    onLongPress = {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        currentLongPress()
                    }
                )
            }
            .semantics {
                onLongClick(label = "Mudar categoria") {
                    currentLongPress()
                    true
                }
            },
        shape = CardShape,
        colors = CardDefaults.cardColors(containerColor = containerColor),
        elevation = CardDefaults.cardElevation(defaultElevation = if (item.inCart) 0.dp else 1.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 4.dp, end = 12.dp, top = 10.dp, bottom = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onToggleInCart) {
                Icon(
                    imageVector = if (item.inCart) Icons.Default.CheckCircle else Icons.Outlined.Circle,
                    contentDescription = if (item.inCart) "Tirar do carrinho" else "Marcar no carrinho",
                    tint = if (item.inCart) SuccessGreen else colors.outline
                )
            }

            ItemPhoto(item = item, modifier = Modifier.alpha(contentAlpha))

            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(start = 12.dp, end = 8.dp)
                    .alpha(contentAlpha)
            ) {
                Text(
                    item.name,
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    textDecoration = if (item.inCart) TextDecoration.LineThrough else TextDecoration.None
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    "${currencyFormat.format(item.unitPrice)} un.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = colors.onSurfaceVariant
                )
            }

            Column(horizontalAlignment = Alignment.End) {
                Text(
                    currencyFormat.format(item.totalPrice),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = colors.onSurface.copy(alpha = contentAlpha)
                )
                Spacer(Modifier.height(6.dp))
                QuantityPill(
                    quantity = item.quantity,
                    onIncrement = onIncrement,
                    onDecrement = onDecrement
                )
            }
        }
    }
}

@Composable
private fun ItemPhoto(item: Item, modifier: Modifier = Modifier) {
    val photoShape = RoundedCornerShape(12.dp)
    if (item.imageUri != null) {
        Image(
            painter = rememberAsyncImagePainter(item.imageUri),
            contentDescription = item.name,
            modifier = modifier
                .size(56.dp)
                .clip(photoShape)
                .background(MaterialTheme.colorScheme.primaryContainer),
            contentScale = ContentScale.Crop
        )
    } else {
        Box(
            modifier = modifier
                .size(56.dp)
                .clip(photoShape)
                .background(MaterialTheme.colorScheme.primaryContainer),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                Icons.Default.ShoppingBasket,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(26.dp)
            )
        }
    }
}

/** Quantidade em pílula: [ − 2 + ]. O "−" some quando a quantidade é 1. */
@Composable
private fun QuantityPill(
    quantity: Int,
    onIncrement: () -> Unit,
    onDecrement: () -> Unit
) {
    Row(
        modifier = Modifier
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.background)
            .padding(horizontal = 2.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center
    ) {
        AnimatedVisibility(visible = quantity > 1, enter = fadeIn(), exit = fadeOut()) {
            PillButton(onClick = onDecrement) {
                Icon(Icons.Default.Remove, contentDescription = "Diminuir", modifier = Modifier.size(18.dp))
            }
        }
        Text(
            quantity.toString(),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(horizontal = 6.dp)
        )
        PillButton(onClick = onIncrement) {
            Icon(Icons.Default.Add, contentDescription = "Aumentar", modifier = Modifier.size(18.dp))
        }
    }
}

@Composable
private fun PillButton(onClick: () -> Unit, content: @Composable () -> Unit) {
    Box(
        modifier = Modifier
            .size(32.dp)
            .clip(CircleShape)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        content()
    }
}
