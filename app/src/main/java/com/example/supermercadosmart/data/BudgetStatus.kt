package com.example.supermercadosmart.data

/** Faixa de uso do orçamento, usada para escolher a cor da barra. */
enum class BudgetBand { SAFE, WARNING, OVER }

/**
 * Cálculos do card de orçamento, separados da tela para facilitar testes.
 *
 * @param total soma dos itens da lista
 * @param maxBudget orçamento máximo (0 ou negativo = não definido)
 */
data class BudgetStatus(val total: Double, val maxBudget: Double) {

    val hasBudget: Boolean get() = maxBudget > 0.0

    /** Quanto ainda pode gastar (negativo quando passou do orçamento). */
    val remaining: Double get() = maxBudget - total

    val isOver: Boolean get() = hasBudget && total > maxBudget + EPSILON

    /** Fração usada do orçamento (0.0 = nada, 1.0 = 100%). Pode passar de 1.0. */
    val usedFraction: Double
        get() = if (hasBudget) (total / maxBudget).coerceAtLeast(0.0) else 0.0

    /** Percentual inteiro para exibir (ex.: 78). */
    val usedPercent: Int get() = Math.round(usedFraction * 100).toInt()

    /** Verde até 70%, areia de 70% a 100%, vermelho acima de 100%. */
    val band: BudgetBand
        get() = when {
            isOver -> BudgetBand.OVER
            usedFraction > WARNING_THRESHOLD -> BudgetBand.WARNING
            else -> BudgetBand.SAFE
        }

    companion object {
        const val WARNING_THRESHOLD = 0.70
        private const val EPSILON = 0.005 // meio centavo, evita erro de arredondamento
    }
}
