package com.example.supermercadosmart.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.IconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.snapshotFlow
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.supermercadosmart.data.Category
import com.example.supermercadosmart.data.Item
import com.example.supermercadosmart.data.ThemeMode
import com.example.supermercadosmart.data.categoryEnum
import com.example.supermercadosmart.pdf.PdfExporter
import com.example.supermercadosmart.viewmodel.ShoppingViewModel
import kotlinx.coroutines.launch
import java.text.NumberFormat
import java.util.Locale

private val headerCurrency = NumberFormat.getCurrencyInstance(Locale("pt", "BR"))

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun MainScreen(
    viewModel: ShoppingViewModel,
    listId: Long,
    onBack: () -> Unit,
    themeMode: ThemeMode = ThemeMode.SYSTEM,
    onThemeModeChange: (ThemeMode) -> Unit = {}
) {
    val context = LocalContext.current
    // Conteúdo da lista aberta. Só vale se for desta lista ([listId]): ao trocar de lista,
    // a tela fica vazia por um instante em vez de mostrar os itens da lista anterior.
    val content by viewModel.listContent.collectAsState()
    val ready = content?.listId == listId
    val shoppingItems = if (ready) content?.items.orEmpty() else emptyList()
    val listName = if (ready) content?.list?.name.orEmpty() else ""
    val maxBudget = if (ready) content?.list?.maxBudget ?: 0.0 else 0.0

    // Voltar (botão do celular ou seta do topo) leva para "Minhas listas"
    BackHandler(onBack = onBack)

    var showAddDialog by remember { mutableStateOf(false) }
    var showBudgetDialog by remember { mutableStateOf(false) }

    // Botão "+ Adicionar": painel com "Digitar" ou "Escanear"
    var showAddSheet by remember { mutableStateOf(false) }
    var showScanner by remember { mutableStateOf(false) }
    // Código lido pelo "Escanear código", que vai preenchido para o diálogo de cadastro
    var scannedBarcode by remember { mutableStateOf<String?>(null) }

    // Menu ⋮ do topo
    var showMenu by remember { mutableStateOf(false) }
    var showRenameDialog by remember { mutableStateOf(false) }

    fun openTypeDialog() {
        scannedBarcode = null
        showAddDialog = true
    }

    // Duas seções: "A comprar" e "No carrinho" (recolhível, começa aberta)
    val toBuyItems = shoppingItems.filter { !it.inCart }
    val inCartItems = shoppingItems.filter { it.inCart }
    var cartExpanded by rememberSaveable { mutableStateOf(true) }

    // "A comprar" agrupado por categoria, na ordem dos corredores; categorias vazias somem
    val toBuyByCategory = Category.values().mapNotNull { category ->
        val itemsInCategory = toBuyItems.filter { it.categoryEnum == category }
        if (itemsInCategory.isEmpty()) null else category to itemsInCategory
    }

    // Item cuja categoria está sendo trocada (toque longo)
    var categoryEditItem by remember { mutableStateOf<Item?>(null) }

    // Barra "Desfazer" no rodapé
    val snackbarHostState = remember { SnackbarHostState() }

    // Topo que encolhe: o card vira uma faixa compacta quando a lista é rolada
    val listState = rememberLazyListState()
    val scope = rememberCoroutineScope()
    // (encolhe ao passar de um pequeno limite e só volta a abrir no topo, evitando "piscar")
    var budgetCollapsed by remember { mutableStateOf(false) }
    LaunchedEffect(listState) {
        snapshotFlow { listState.firstVisibleItemIndex to listState.firstVisibleItemScrollOffset }
            .collect { (index, offset) ->
                if (index > 0 || offset > 24) budgetCollapsed = true
                else if (offset == 0) budgetCollapsed = false
            }
    }

    fun showUndo(message: String, onUndo: () -> Unit) {
        scope.launch {
            snackbarHostState.currentSnackbarData?.dismiss()
            val result = snackbarHostState.showSnackbar(
                message = message,
                actionLabel = "Desfazer",
                duration = SnackbarDuration.Short
            )
            if (result == SnackbarResult.ActionPerformed) onUndo()
        }
    }

    val ShoppingItem: @Composable (Item, Modifier) -> Unit = { item, itemModifier ->
        ItemRow(
            item = item,
            onToggleInCart = { viewModel.toggleInCart(item) },
            onIncrement = {
                viewModel.updateItem(item.copy(quantity = item.quantity + 1))
            },
            onDecrement = {
                if (item.quantity > 1) {
                    viewModel.updateItem(item.copy(quantity = item.quantity - 1))
                }
            },
            onSwipeToggleInCart = {
                val wasInCart = item.inCart
                viewModel.toggleInCart(item)
                showUndo(
                    if (wasInCart) "\"${item.name}\" voltou para a lista"
                    else "\"${item.name}\" no carrinho"
                ) {
                    // volta só o "no carrinho", mantendo o que mais tiver mudado no item
                    val current = viewModel.allItems.value.find { it.id == item.id } ?: item
                    viewModel.updateItem(current.copy(inCart = wasInCart))
                }
            },
            onSwipeDelete = {
                viewModel.deleteItem(item)
                showUndo("\"${item.name}\" removido") { viewModel.restoreItem(item) }
            },
            modifier = itemModifier,
            onLongPress = { categoryEditItem = item }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        listName,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Minhas listas")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                ),
                actions = {
                    Box {
                        IconButton(onClick = { showMenu = true }) {
                            Icon(Icons.Default.MoreVert, contentDescription = "Mais opções")
                        }
                        DropdownMenu(
                            expanded = showMenu,
                            onDismissRequest = { showMenu = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text("Ver PDF") },
                                leadingIcon = { Icon(Icons.Default.PictureAsPdf, contentDescription = null) },
                                enabled = shoppingItems.isNotEmpty(),
                                onClick = {
                                    showMenu = false
                                    PdfExporter.open(context, shoppingItems, listName)
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Compartilhar PDF") },
                                leadingIcon = { Icon(Icons.Default.Share, contentDescription = null) },
                                enabled = shoppingItems.isNotEmpty(),
                                onClick = {
                                    showMenu = false
                                    PdfExporter.exportAndShare(context, shoppingItems, listName)
                                }
                            )

                            HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
                            DropdownMenuItem(
                                text = { Text("Renomear lista") },
                                leadingIcon = { Icon(Icons.Default.Edit, contentDescription = null) },
                                enabled = ready,
                                onClick = {
                                    showMenu = false
                                    showRenameDialog = true
                                }
                            )

                            // Tema: Sistema / Claro / Escuro (o escolhido fica com ✓)
                            ThemeMenuSection(
                                themeMode = themeMode,
                                onSelect = { mode ->
                                    showMenu = false
                                    onThemeModeChange(mode)
                                }
                            )
                        }
                    }
                }
            )
        },
        floatingActionButton = {
            // Com a lista vazia, os botões ficam na própria tela vazia
            if (shoppingItems.isNotEmpty()) {
                ExtendedFloatingActionButton(
                    onClick = { showAddSheet = true },
                    icon = { Icon(Icons.Default.Add, contentDescription = null) },
                    text = { Text("Adicionar") },
                    // encolhe para só o "+" enquanto a lista está rolada
                    expanded = !budgetCollapsed
                )
            }
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        containerColor = MaterialTheme.colorScheme.background
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp)
        ) {
            SubtotalCard(
                items = shoppingItems,
                maxBudget = maxBudget,
                onBudgetClick = { showBudgetDialog = true },
                collapsed = budgetCollapsed && shoppingItems.isNotEmpty(),
                onCollapsedClick = { scope.launch { listState.animateScrollToItem(0) } },
                modifier = Modifier.padding(top = 8.dp, bottom = 12.dp)
            )

            if (!ready) {
                // carregando a lista (é rapidinho)
            } else if (shoppingItems.isEmpty()) {
                EmptyState(
                    onType = { openTypeDialog() },
                    onScan = { showScanner = true }
                )
            } else {
                LazyColumn(
                    state = listState,
                    contentPadding = PaddingValues(bottom = 96.dp)
                ) {
                    if (toBuyItems.isNotEmpty()) {
                        item(key = "header_to_buy") {
                            SectionHeader(
                                title = "A comprar",
                                count = toBuyItems.size,
                                total = toBuyItems.sumOf { it.totalPrice },
                                modifier = Modifier.animateItemPlacement()
                            )
                        }
                    } else {
                        item(key = "all_in_cart") {
                            Text(
                                "Tudo no carrinho!",
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.SemiBold,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 12.dp)
                                    .animateItemPlacement(),
                                textAlign = TextAlign.Center
                            )
                        }
                    }

                    toBuyByCategory.forEach { (category, itemsInCategory) ->
                        // Cabeçalho fixo: fica no topo enquanto os itens da categoria rolam
                        stickyHeader(key = "category_${category.name}") {
                            CategoryHeader(
                                category = category,
                                count = itemsInCategory.size,
                                total = itemsInCategory.sumOf { it.totalPrice }
                            )
                        }
                        items(itemsInCategory, key = { it.id }) { item ->
                            ShoppingItem(item, Modifier.animateItemPlacement())
                        }
                    }

                    if (inCartItems.isNotEmpty()) {
                        // Também fixo, para o último cabeçalho de categoria não ficar por cima do carrinho
                        stickyHeader(key = "header_in_cart") {
                            SectionHeader(
                                title = "No carrinho",
                                count = inCartItems.size,
                                total = inCartItems.sumOf { it.totalPrice },
                                expanded = cartExpanded,
                                onClick = { cartExpanded = !cartExpanded },
                                modifier = Modifier
                                    .background(MaterialTheme.colorScheme.background)
                                    .padding(top = 8.dp)
                            )
                        }
                        if (cartExpanded) {
                            items(inCartItems, key = { it.id }) { item ->
                                ShoppingItem(item, Modifier.animateItemPlacement())
                            }
                        }
                    }
                }
            }
        }
    }

    if (showAddDialog) {
        AddItemDialog(
            onConfirm = { result ->
                viewModel.addItem(
                    name = result.name,
                    unitPrice = result.unitPrice,
                    quantity = result.quantity,
                    imageUri = result.imageUri,
                    barcode = result.barcode,
                    category = result.category
                )
                showAddDialog = false
                scannedBarcode = null
            },
            onDismiss = {
                showAddDialog = false
                scannedBarcode = null
            },
            initialBarcode = scannedBarcode
        )
    }

    if (showAddSheet) {
        AddActionSheet(
            onType = { openTypeDialog() },
            onScan = { showScanner = true },
            onDismiss = { showAddSheet = false }
        )
    }

    if (showScanner) {
        BarcodeScannerDialog(
            onBarcodeScanned = { code ->
                showScanner = false
                scannedBarcode = code
                showAddDialog = true
            },
            onDismiss = { showScanner = false }
        )
    }

    categoryEditItem?.let { editing ->
        CategoryPickerDialog(
            itemName = editing.name,
            current = editing.categoryEnum,
            onSelect = { newCategory ->
                // usa a versão mais recente do item (pode ter mudado quantidade etc.)
                val current = viewModel.allItems.value.find { it.id == editing.id } ?: editing
                if (newCategory != current.categoryEnum) {
                    viewModel.changeCategory(current, newCategory)
                }
                categoryEditItem = null
            },
            onDismiss = { categoryEditItem = null }
        )
    }

    if (showRenameDialog) {
        ListFormDialog(
            title = "Renomear lista",
            confirmLabel = "Salvar",
            initialName = listName,
            showBudget = false,
            onConfirm = { name, _ ->
                if (name.isNotBlank()) viewModel.renameList(listId, name)
                showRenameDialog = false
            },
            onDismiss = { showRenameDialog = false }
        )
    }

    if (showBudgetDialog) {
        BudgetDialog(
            currentBudget = maxBudget,
            onConfirm = { value ->
                viewModel.setBudget(value)
                showBudgetDialog = false
            },
            onDismiss = { showBudgetDialog = false }
        )
    }
}


/** Cabeçalho de seção: "A comprar · 3 itens · R$ 25,90". Com [onClick], vira recolhível. */
@Composable
private fun SectionHeader(
    title: String,
    count: Int,
    total: Double,
    modifier: Modifier = Modifier,
    expanded: Boolean? = null,
    onClick: (() -> Unit)? = null
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
            .padding(vertical = 8.dp, horizontal = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            "$title ($count)",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.primary,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.weight(1f),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        Text(
            headerCurrency.format(total),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontWeight = FontWeight.SemiBold
        )
        if (expanded != null) {
            Icon(
                imageVector = if (expanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                contentDescription = if (expanded) "Recolher" else "Expandir",
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(start = 4.dp)
            )
        }
    }
}
