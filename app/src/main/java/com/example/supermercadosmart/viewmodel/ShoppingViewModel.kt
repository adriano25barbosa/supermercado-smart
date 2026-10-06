package com.example.supermercadosmart.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.supermercadosmart.data.AppDatabase
import com.example.supermercadosmart.data.Category
import com.example.supermercadosmart.data.DeletedList
import com.example.supermercadosmart.data.Item
import com.example.supermercadosmart.data.ItemRepository
import com.example.supermercadosmart.data.ListContent
import com.example.supermercadosmart.data.ListSummary
import com.example.supermercadosmart.data.LookupResult
import com.example.supermercadosmart.data.ProductLookup
import com.example.supermercadosmart.data.QuickEntry
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

@OptIn(ExperimentalCoroutinesApi::class)
class ShoppingViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = ItemRepository(AppDatabase.getInstance(application))
    private val productLookup = ProductLookup(application, repository)

    // ---- Tela "Minhas listas" ----

    /** Todas as listas com nº de itens e total. null = ainda carregando. */
    val lists: StateFlow<List<ListSummary>?> = repository.listSummaries
        .stateIn(viewModelScope, SharingStarted.Eagerly, null)

    // ---- Lista aberta ----

    private val _currentListId = MutableStateFlow<Long?>(null)

    /** Lista aberta no momento (null = tela "Minhas listas"). */
    val currentListId: StateFlow<Long?> = _currentListId.asStateFlow()

    /**
     * Itens da última lista aberta, junto com o id dela. Ao voltar para "Minhas listas" o
     * conteúdo continua aqui (a tela da lista sai com animação sem ficar em branco); ao abrir
     * outra lista, a tela compara o [ListContent.listId] para não mostrar itens da anterior.
     */
    val listContent: StateFlow<ListContent?> = _currentListId
        .filterNotNull()
        .flatMapLatest { id ->
            combine(repository.observeList(id), repository.itemsForList(id)) { list, items ->
                ListContent(listId = id, list = list, items = items)
            }
        }
        .stateIn(viewModelScope, SharingStarted.Eagerly, null)

    /** Itens da lista aberta (usado pelos "Desfazer" para pegar a versão mais recente do item). */
    val allItems: StateFlow<List<Item>> = listContent
        .map { it?.items ?: emptyList() }
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    fun openList(listId: Long) {
        _currentListId.value = listId
    }

    fun closeList() {
        _currentListId.value = null
    }

    // ---- Ações nas listas ----

    /** Cria uma lista e já abre ela. */
    fun createList(name: String, maxBudget: Double) {
        viewModelScope.launch {
            val id = repository.createList(name, maxBudget)
            openList(id)
        }
    }

    fun renameList(listId: Long, name: String) {
        viewModelScope.launch {
            repository.renameList(listId, name)
        }
    }

    fun duplicateList(listId: Long, newName: String) {
        viewModelScope.launch {
            repository.duplicateList(listId, newName)
        }
    }

    /** Exclui a lista e os itens; devolve o que foi apagado para o "Desfazer". */
    suspend fun deleteList(listId: Long): DeletedList? = repository.deleteList(listId)

    fun restoreList(deleted: DeletedList) {
        viewModelScope.launch {
            repository.restoreList(deleted)
        }
    }

    // ---- Itens da lista aberta ----

    fun addItem(
        name: String,
        unitPrice: Double,
        quantity: Int,
        imageUri: String? = null,
        barcode: String? = null,
        category: String = Category.OUTROS.name
    ) {
        val listId = _currentListId.value ?: return
        viewModelScope.launch {
            repository.insert(
                Item(
                    name = name,
                    unitPrice = unitPrice,
                    quantity = quantity,
                    imageUri = imageUri,
                    barcode = barcode,
                    category = category,
                    listId = listId
                )
            )
        }
    }

    /**
     * "Monte sua lista antecipado": vários itens de uma vez, só nome, quantidade e categoria,
     * sem preço (preço 0, informado depois no mercado). Ficam na lista na ordem digitada.
     */
    fun addItems(entries: List<Pair<QuickEntry, Category>>) {
        val listId = _currentListId.value ?: return
        if (entries.isEmpty()) return
        // A lista mostra o mais novo primeiro: o primeiro digitado ganha o horário maior
        val now = System.currentTimeMillis()
        val items = entries.mapIndexed { index, (entry, category) ->
            Item(
                name = entry.name,
                unitPrice = 0.0,
                quantity = entry.quantity,
                category = category.name,
                listId = listId,
                timestamp = now + (entries.size - 1 - index)
            )
        }
        viewModelScope.launch {
            repository.insertAll(items)
        }
    }

    /** Informa (ou corrige) o preço de um item. Só o preço muda no banco. */
    fun setPrice(item: Item, unitPrice: Double) {
        viewModelScope.launch {
            repository.setPrice(item.id, unitPrice.coerceAtLeast(0.0))
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

    /** Troca a categoria de um item (toque longo no card). */
    fun changeCategory(item: Item, category: Category) {
        viewModelScope.launch {
            repository.update(item.copy(category = category.name))
        }
    }

    fun deleteItem(item: Item) {
        viewModelScope.launch {
            repository.delete(item)
        }
    }

    /** Desfazer exclusão: reinsere o item com o mesmo id e os mesmos dados (inclusive a lista). */
    fun restoreItem(item: Item) {
        viewModelScope.launch {
            repository.insert(item)
        }
    }

    /** Esvazia a lista aberta. */
    fun clearAllItems() {
        val listId = _currentListId.value ?: return
        viewModelScope.launch {
            repository.clearList(listId)
        }
    }

    /** Orçamento da lista aberta. */
    fun setBudget(value: Double) {
        val listId = _currentListId.value ?: return
        viewModelScope.launch {
            repository.setBudget(listId, value)
        }
    }

    suspend fun findItemByBarcode(barcode: String): Item? {
        return repository.findByBarcode(barcode)
    }

    /** Nome, foto e categoria pelo código de barras: primeiro nas listas, depois no Open Food Facts. */
    suspend fun lookupBarcode(barcode: String): LookupResult = productLookup.lookup(barcode)
}
