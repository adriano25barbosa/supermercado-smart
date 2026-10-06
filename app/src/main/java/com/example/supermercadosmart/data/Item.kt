package com.example.supermercadosmart.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "items")
data class Item(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    val unitPrice: Double,
    val quantity: Int = 1,
    val inCart: Boolean = false,
    val imageUri: String? = null,
    val barcode: String? = null,
    /** Chave da categoria (ex.: "LIMPEZA"); veja [Category]. */
    val category: String = Category.OUTROS.name,
    val timestamp: Long = System.currentTimeMillis()
) {
    val totalPrice: Double
        get() = unitPrice * quantity
}
