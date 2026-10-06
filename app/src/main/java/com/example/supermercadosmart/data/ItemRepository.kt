package com.example.supermercadosmart.data

import kotlinx.coroutines.flow.Flow

class ItemRepository(
    private val itemDao: ItemDao,
    private val budgetDao: BudgetDao
) {
    val allItems: Flow<List<Item>> = itemDao.getAllItems()
    val budget: Flow<BudgetSettings?> = budgetDao.getBudget()

    suspend fun insert(item: Item): Long = itemDao.insert(item)

    suspend fun update(item: Item) = itemDao.update(item)

    suspend fun delete(item: Item) = itemDao.delete(item)

    suspend fun clearAll() = itemDao.clearAll()

    suspend fun findByBarcode(barcode: String): Item? = itemDao.findByBarcode(barcode)

    suspend fun setBudget(maxBudget: Double) = budgetDao.setBudget(BudgetSettings(maxBudget = maxBudget))
}
