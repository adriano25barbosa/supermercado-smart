package com.example.supermercadosmart.data

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(tableName = "items", indices = [Index("listId")])
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
    /** Lista a que o item pertence; veja [ShoppingList]. */
    val listId: Long = 0,
    val timestamp: Long = System.currentTimeMillis()
) {
    val totalPrice: Double
        get() = unitPrice * quantity
}

/**
 * Item sem preço: criado pela "Monte sua lista antecipado" (o preço é informado no mercado).
 * No banco fica como preço 0, sem coluna nova.
 */
val Item.hasPrice: Boolean
    get() = unitPrice > 0.0
