package com.example.supermercadosmart.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "budget_settings")
data class BudgetSettings(
    @PrimaryKey
    val id: Int = 1,
    val maxBudget: Double = 0.0
)
