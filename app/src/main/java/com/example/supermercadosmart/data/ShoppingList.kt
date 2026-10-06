package com.example.supermercadosmart.data

import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.PrimaryKey

/** Uma lista de compras, com o seu próprio orçamento. Os itens apontam para ela por [Item.listId]. */
@Entity(tableName = "shopping_lists")
data class ShoppingList(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    /** Orçamento máximo da lista (0 = não definido). */
    val maxBudget: Double = 0.0,
    val createdAt: Long = System.currentTimeMillis()
)

/** Lista com os números que aparecem na tela "Minhas listas". */
data class ListSummary(
    @Embedded val list: ShoppingList,
    val itemCount: Int,
    val inCartCount: Int,
    val total: Double
)

/** Itens da lista aberta, junto com a própria lista (o [listId] diz de qual lista eles são). */
data class ListContent(
    val listId: Long,
    val list: ShoppingList?,
    val items: List<Item>
)

/** O que foi apagado ao excluir uma lista, para o "Desfazer" pôr tudo de volta. */
data class DeletedList(
    val list: ShoppingList,
    val items: List<Item>
)
