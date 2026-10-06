package com.example.supermercadosmart.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface ItemDao {

    @Query("SELECT * FROM items WHERE listId = :listId ORDER BY timestamp DESC")
    fun getItemsForList(listId: Long): Flow<List<Item>>

    @Query("SELECT * FROM items WHERE listId = :listId ORDER BY timestamp DESC")
    suspend fun getItemsForListOnce(listId: Long): List<Item>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(item: Item): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(items: List<Item>)

    @Update
    suspend fun update(item: Item)

    @Delete
    suspend fun delete(item: Item)

    /** Só o preço (não mexe em "no carrinho", quantidade etc.). */
    @Query("UPDATE items SET unitPrice = :unitPrice WHERE id = :id")
    suspend fun setPrice(id: Long, unitPrice: Double)

    @Query("DELETE FROM items WHERE listId = :listId")
    suspend fun clearList(listId: Long)

    @Query("SELECT * FROM items WHERE barcode = :barcode ORDER BY timestamp DESC LIMIT 1")
    suspend fun findByBarcode(barcode: String): Item?
}
