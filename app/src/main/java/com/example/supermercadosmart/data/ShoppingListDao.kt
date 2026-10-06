package com.example.supermercadosmart.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface ShoppingListDao {

    /** Todas as listas (mais novas primeiro) com nº de itens, nº no carrinho e total. */
    @Query(
        """
        SELECT l.*,
               COUNT(i.id) AS itemCount,
               COALESCE(SUM(CASE WHEN i.inCart THEN 1 ELSE 0 END), 0) AS inCartCount,
               COALESCE(SUM(i.unitPrice * i.quantity), 0.0) AS total
        FROM shopping_lists l
        LEFT JOIN items i ON i.listId = l.id
        GROUP BY l.id
        ORDER BY l.createdAt DESC
        """
    )
    fun getSummaries(): Flow<List<ListSummary>>

    @Query("SELECT * FROM shopping_lists WHERE id = :id LIMIT 1")
    fun observe(id: Long): Flow<ShoppingList?>

    @Query("SELECT * FROM shopping_lists WHERE id = :id LIMIT 1")
    suspend fun getById(id: Long): ShoppingList?

    /** REPLACE: o "Desfazer" reinsere a lista com o mesmo id. */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(list: ShoppingList): Long

    @Delete
    suspend fun delete(list: ShoppingList)

    @Query("UPDATE shopping_lists SET name = :name WHERE id = :id")
    suspend fun rename(id: Long, name: String)

    @Query("UPDATE shopping_lists SET maxBudget = :maxBudget WHERE id = :id")
    suspend fun setBudget(id: Long, maxBudget: Double)
}
