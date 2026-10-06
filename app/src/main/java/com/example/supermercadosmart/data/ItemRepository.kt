package com.example.supermercadosmart.data

import androidx.room.withTransaction
import kotlinx.coroutines.flow.Flow

class ItemRepository(private val db: AppDatabase) {

    private val itemDao = db.itemDao()
    private val listDao = db.shoppingListDao()

    // ---- Listas ----

    val listSummaries: Flow<List<ListSummary>> = listDao.getSummaries()

    fun observeList(listId: Long): Flow<ShoppingList?> = listDao.observe(listId)

    suspend fun createList(name: String, maxBudget: Double): Long =
        listDao.insert(ShoppingList(name = name, maxBudget = maxBudget))

    suspend fun renameList(listId: Long, name: String) = listDao.rename(listId, name)

    suspend fun setBudget(listId: Long, maxBudget: Double) = listDao.setBudget(listId, maxBudget)

    /** Copia a lista e os itens (todos fora do carrinho). Devolve o id da cópia, ou null se a lista não existir. */
    suspend fun duplicateList(listId: Long, newName: String): Long? = db.withTransaction {
        val source = listDao.getById(listId) ?: return@withTransaction null
        val newId = listDao.insert(
            source.copy(id = 0, name = newName, createdAt = System.currentTimeMillis())
        )
        val items = itemDao.getItemsForListOnce(listId)
        itemDao.insertAll(items.map { it.copy(id = 0, listId = newId, inCart = false) })
        newId
    }

    /** Apaga a lista e os itens dela; devolve o que foi apagado para o "Desfazer". */
    suspend fun deleteList(listId: Long): DeletedList? = db.withTransaction {
        val list = listDao.getById(listId) ?: return@withTransaction null
        val items = itemDao.getItemsForListOnce(listId)
        itemDao.clearList(listId)
        listDao.delete(list)
        DeletedList(list, items)
    }

    /** Desfazer: reinsere a lista e os itens com os mesmos ids. */
    suspend fun restoreList(deleted: DeletedList) = db.withTransaction {
        listDao.insert(deleted.list)
        itemDao.insertAll(deleted.items)
    }

    // ---- Itens ----

    fun itemsForList(listId: Long): Flow<List<Item>> = itemDao.getItemsForList(listId)

    suspend fun insert(item: Item): Long = itemDao.insert(item)

    suspend fun insertAll(items: List<Item>) = itemDao.insertAll(items)

    suspend fun setPrice(itemId: Long, unitPrice: Double) = itemDao.setPrice(itemId, unitPrice)

    suspend fun update(item: Item) = itemDao.update(item)

    suspend fun delete(item: Item) = itemDao.delete(item)

    suspend fun clearList(listId: Long) = itemDao.clearList(listId)

    suspend fun findByBarcode(barcode: String): Item? = itemDao.findByBarcode(barcode)
}
