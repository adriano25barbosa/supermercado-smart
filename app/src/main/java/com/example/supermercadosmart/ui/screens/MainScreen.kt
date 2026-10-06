package com.example.supermercadosmart.ui.screens

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
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
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.example.supermercadosmart.pdf.PdfExporter
import com.example.supermercadosmart.viewmodel.ShoppingViewModel
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(viewModel: ShoppingViewModel) {
    val context = LocalContext.current
    val shoppingItems by viewModel.allItems.collectAsState()
    val maxBudget by viewModel.maxBudget.collectAsState()

    var showAddDialog by remember { mutableStateOf(false) }
    var showBudgetDialog by remember { mutableStateOf(false) }

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

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Supermercado Smart") },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                ),
                actions = {
                    androidx.compose.material3.IconButton(
                        onClick = {
                            if (shoppingItems.isNotEmpty()) {
                                PdfExporter.exportAndShare(context, shoppingItems)
                            }
                        }
                    ) {
                        Icon(Icons.Default.PictureAsPdf, contentDescription = "Exportar PDF")
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { showAddDialog = true }) {
                Icon(Icons.Default.Add, contentDescription = "Adicionar produto")
            }
        },
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

            if (shoppingItems.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        "Sua lista está vazia.\nToque no botão + para adicionar produtos.",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )
                }
            } else {
                LazyColumn(
                    state = listState,
                    contentPadding = PaddingValues(bottom = 96.dp)
                ) {
                    items(shoppingItems, key = { it.id }) { item ->
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
                            onDelete = { viewModel.deleteItem(item) }
                        )
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
                    barcode = result.barcode
                )
                showAddDialog = false
            },
            onDismiss = { showAddDialog = false }
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

