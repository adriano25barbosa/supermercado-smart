package com.example.supermercadosmart.ui.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.supermercadosmart.ui.theme.AlertRed
import com.example.supermercadosmart.ui.theme.ForestGreen
import com.example.supermercadosmart.ui.theme.ForestGreenLight
import com.example.supermercadosmart.ui.theme.LocalDarkTheme
import com.example.supermercadosmart.ui.theme.NightCartContainer
import com.example.supermercadosmart.ui.theme.Sage
import com.example.supermercadosmart.ui.theme.SageLight
import com.example.supermercadosmart.ui.theme.SandAccent

/** Tela de lista vazia: ilustração de carrinho, texto curto e os dois jeitos de adicionar. */
@Composable
fun EmptyState(
    onType: () -> Unit,
    onScan: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(
            modifier = Modifier
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            val colors = illustrationColors(
                dark = LocalDarkTheme.current,
                basketFill = MaterialTheme.colorScheme.surface
            )
            Canvas(modifier = Modifier.size(170.dp)) { drawEmptyCart(colors) }

            Spacer(Modifier.height(16.dp))
            Text(
                "Sua lista está vazia",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
                textAlign = TextAlign.Center
            )
            Spacer(Modifier.height(6.dp))
            Text(
                "Adicione o primeiro produto digitando o nome\nou escaneando o código de barras.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )

            Spacer(Modifier.height(24.dp))
            Button(onClick = onType, modifier = Modifier.width(240.dp)) {
                Icon(Icons.Default.Edit, contentDescription = null)
                Text("Digitar produto", modifier = Modifier.padding(start = 8.dp))
            }
            Spacer(Modifier.height(8.dp))
            OutlinedButton(onClick = onScan, modifier = Modifier.width(240.dp)) {
                Icon(Icons.Default.QrCodeScanner, contentDescription = null)
                Text("Escanear código", modifier = Modifier.padding(start = 8.dp))
            }
        }
    }
}

/** Cores da ilustração: no modo escuro o contorno vira sálvia e o fundo, verde escuro. */
private class IllustrationColors(
    val circle: Color,
    val basketFill: Color,
    val outline: Color,
    val ribs: Color,
    val leafLight: Color,
    val leafDark: Color
)

private fun illustrationColors(dark: Boolean, basketFill: Color) = if (dark) {
    IllustrationColors(
        circle = NightCartContainer,
        basketFill = basketFill,
        outline = Sage,
        ribs = ForestGreenLight,
        leafLight = SageLight,
        leafDark = Sage
    )
} else {
    IllustrationColors(
        circle = SageLight,
        basketFill = basketFill,
        outline = ForestGreen,
        ribs = Sage,
        leafLight = Sage,
        leafDark = ForestGreenLight
    )
}

/** Carrinho com folhas, pão e tomate, nas cores da paleta "Gourmet Fresh". */
private fun DrawScope.drawEmptyCart(c: IllustrationColors) {
    val w = size.width
    fun p(x: Float, y: Float) = Offset(x * w, y * w)

    // Círculo de fundo sálvia claro
    drawCircle(c.circle, radius = w * 0.48f, center = center)

    // Brilhos em areia
    drawCircle(SandAccent, radius = w * 0.022f, center = p(0.20f, 0.22f))
    drawCircle(SandAccent, radius = w * 0.016f, center = p(0.80f, 0.20f))
    drawCircle(SandAccent, radius = w * 0.014f, center = p(0.86f, 0.58f))

    // Produtos saindo do carrinho (desenhados antes para o cesto ficar por cima)
    rotate(degrees = 30f, pivot = p(0.62f, 0.40f)) { // pão
        drawRoundRect(
            color = SandAccent,
            topLeft = p(0.58f, 0.26f),
            size = Size(0.09f * w, 0.28f * w),
            cornerRadius = CornerRadius(0.045f * w)
        )
    }
    rotate(degrees = -28f, pivot = p(0.40f, 0.38f)) { // folha clara
        drawOval(c.leafLight, topLeft = p(0.35f, 0.24f), size = Size(0.10f * w, 0.24f * w))
    }
    rotate(degrees = 8f, pivot = p(0.50f, 0.34f)) { // folha escura
        drawOval(c.leafDark, topLeft = p(0.45f, 0.20f), size = Size(0.10f * w, 0.26f * w))
    }
    drawCircle(AlertRed, radius = w * 0.06f, center = p(0.54f, 0.45f)) // tomate

    val stroke = Stroke(width = w * 0.032f, cap = StrokeCap.Round, join = StrokeJoin.Round)

    // Cesto (trapézio)
    val basket = Path().apply {
        moveTo(0.28f * w, 0.46f * w)
        lineTo(0.76f * w, 0.46f * w)
        lineTo(0.70f * w, 0.66f * w)
        lineTo(0.34f * w, 0.66f * w)
        close()
    }
    drawPath(basket, c.basketFill)
    drawPath(basket, c.outline, style = stroke)

    // Linhas do cesto
    val rib = Stroke(width = w * 0.016f, cap = StrokeCap.Round)
    drawLine(c.ribs, p(0.33f, 0.53f), p(0.71f, 0.53f), strokeWidth = rib.width, cap = StrokeCap.Round)
    drawLine(c.ribs, p(0.35f, 0.60f), p(0.69f, 0.60f), strokeWidth = rib.width, cap = StrokeCap.Round)

    // Alça
    val handle = Path().apply {
        moveTo(0.28f * w, 0.46f * w)
        lineTo(0.23f * w, 0.33f * w)
        lineTo(0.14f * w, 0.33f * w)
    }
    drawPath(handle, c.outline, style = stroke)

    // Base e rodas
    val base = Path().apply {
        moveTo(0.34f * w, 0.66f * w)
        lineTo(0.37f * w, 0.73f * w)
        lineTo(0.69f * w, 0.73f * w)
    }
    drawPath(base, c.outline, style = stroke)
    drawCircle(c.outline, radius = w * 0.04f, center = p(0.41f, 0.81f))
    drawCircle(c.outline, radius = w * 0.04f, center = p(0.65f, 0.81f))
}
