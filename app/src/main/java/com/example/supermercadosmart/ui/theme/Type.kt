package com.example.supermercadosmart.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.example.supermercadosmart.R

/**
 * Nunito (arredondada) nos títulos, valores e botões; o texto corrido fica com a fonte do
 * sistema, que lê melhor em tamanho pequeno. Os arquivos estão em res/font (funciona offline).
 */
val Nunito = FontFamily(
    Font(R.font.nunito_semibold, FontWeight.SemiBold),
    Font(R.font.nunito_bold, FontWeight.Bold),
    Font(R.font.nunito_extrabold, FontWeight.ExtraBold)
)

private val Base = Typography()

val AppTypography = Typography(
    displaySmall = Base.displaySmall.copy(fontFamily = Nunito, fontWeight = FontWeight.ExtraBold),
    headlineLarge = Base.headlineLarge.copy(
        fontFamily = Nunito, fontWeight = FontWeight.ExtraBold, fontSize = 28.sp, lineHeight = 34.sp
    ),
    headlineMedium = Base.headlineMedium.copy(
        fontFamily = Nunito, fontWeight = FontWeight.Bold, fontSize = 22.sp, lineHeight = 28.sp
    ),
    headlineSmall = Base.headlineSmall.copy(fontFamily = Nunito, fontWeight = FontWeight.Bold),
    titleLarge = Base.titleLarge.copy(
        fontFamily = Nunito, fontWeight = FontWeight.ExtraBold, fontSize = 21.sp, lineHeight = 28.sp
    ),
    titleMedium = Base.titleMedium.copy(
        fontFamily = Nunito, fontWeight = FontWeight.Bold, fontSize = 16.sp, lineHeight = 22.sp
    ),
    titleSmall = Base.titleSmall.copy(fontFamily = Nunito, fontWeight = FontWeight.Bold),
    bodyLarge = Base.bodyLarge.copy(fontSize = 16.sp),
    bodyMedium = Base.bodyMedium.copy(fontSize = 14.sp),
    bodySmall = Base.bodySmall,
    labelLarge = Base.labelLarge.copy(fontFamily = Nunito, fontWeight = FontWeight.Bold, fontSize = 14.sp),
    labelMedium = Base.labelMedium.copy(fontFamily = Nunito, fontWeight = FontWeight.Bold),
    labelSmall = Base.labelSmall.copy(fontFamily = Nunito, fontWeight = FontWeight.Bold)
)
