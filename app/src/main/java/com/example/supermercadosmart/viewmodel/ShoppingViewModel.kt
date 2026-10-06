package com.example.supermercadosmart.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.supermercadosmart.data.AppDatabase
import com.example.supermercadosmart.data.Item
import com.example.supermercadosmart.data.ItemRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class ShoppingViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: ItemRepository

    val allItems: StateFlow<List<Item>>
    val maxBudget: StateFlow<Double>

    init {
        val db = AppDatabase.getInstance(application)
        repository = ItemRepository(db.itemDao(), db.budgetDao())

        allItems = repository.allItems.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

        maxBudget = repository.budget
            .map { settings -> settings?.maxBudget ?: 0.0 }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5000),
                initialValue = 0.0
            )
    }

    fun addItem(
        name: String,
        unitPrice: Double,
        quantity: Int,
        imageUri: String? = null,
        barcode: String? = null
    ) {
        viewModelScope.launch {
            repository.insert(
                Item(
                    name = name,
                    unitPrice = unitPrice,
                    quantity = quantity,
                    imageUri = imageUri,
                    barcode = barcode
                )
            )
        }
    }

    fun updateItem(item: Item) {
        viewModelScope.launch {
            repository.update(item)
        }
    }

    fun toggleInCart(item: Item) {
        viewModelScope.launch {
            repository.update(item.copy(inCart = !item.inCart))
        }
    }

    fun deleteItem(item: Item) {
        viewModelScope.launch {
            repository.delete(item)
        }
    }

    /** Desfazer exclusão: reinsere o item com o mesmo id e os mesmos dados. */
    fun restoreItem(item: Item) {
        viewModelScope.launch {
            repository.insert(item)
        }
    }

    fun clearAllItems() {
        viewModelScope.launch {
            repository.clearAll()
        }
    }

    fun setBudget(value: Double) {
        viewModelScope.launch {
            repository.setBudget(value)
        }
    }

    suspend fun findItemByBarcode(barcode: String): Item? {
        return repository.findByBarcode(barcode)
    }
}
