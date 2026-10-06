@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.example.supermercadosmart.ui.screens

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.ShoppingBasket
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.supermercadosmart.data.BudgetBand
import com.example.supermercadosmart.data.BudgetStatus
import com.example.supermercadosmart.data.ListSummary
import com.example.supermercadosmart.data.ThemeMode
import com.example.supermercadosmart.viewmodel.ShoppingViewModel
import kotlinx.coroutines.launch
import java.text.NumberFormat
import java.util.Locale

private val listsCurrency = NumberFormat.getCurrencyInstance(Locale("pt", "BR"))
private val ListCardShape = RoundedCornerShape(16.dp)

/** Nome padrão quando a lista é criada sem nome. */
private const val NEW_LIST_FALLBACK_NAME = "Nova lista"

/**
 * Tela inicial: todas as listas de compras, cada uma com o seu orçamento.
 * Tocar numa lista abre ela; o ⋮ de cada card tem Renomear, Duplicar e Excluir (com Desfazer).
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ListsScreen(
    viewModel: ShoppingViewModel,
    themeMode: ThemeMode = ThemeMode.SYSTEM,
    onThemeModeChange: (ThemeMode) -> Unit = {}
) {
    val lists by viewModel.lists.collectAsState()

    var showMenu by remember { mutableStateOf(false) }
    var showNewListDialog by remember { mutableStateOf(false) }
    var renaming by remember { mutableStateOf<ListSummary?>(null) }

    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    fun showMessage(message: String, actionLabel: String? = null, long: Boolean = false, onAction: () -> Unit = {}) {
        scope.launch {
            snackbarHostState.currentSnackbarData?.dismiss()
            val result = snackbarHostState.showSnackbar(
                message = message,
                actionLabel = actionLabel,
                duration = if (long) SnackbarDuration.Long else SnackbarDuration.Short
            )
            if (result == SnackbarResult.ActionPerformed) onAction()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Minhas listas") },
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
                            ThemeMenuSection(
                                themeMode = themeMode,
                                onSelect = { mode ->
                                    showMenu = false
                                    onThemeModeChange(mode)
                                },
                                showDivider = false
                            )
                        }
                    }
                }
            )
        },
        floatingActionButton = {
            if (!lists.isNullOrEmpty()) {
                ExtendedFloatingActionButton(
                    onClick = { showNewListDialog = true },
                    icon = { Icon(Icons.Default.Add, contentDescription = null) },
                    text = { Text("Nova lista") }
                )
            }
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        containerColor = MaterialTheme.colorScheme.background
    ) { padding ->
        val current = lists
        when {
            current == null -> Unit // carregando (é rapidinho)
            current.isEmpty() -> NoListsState(
                onCreate = { showNewListDialog = true },
                modifier = Modifier.padding(padding)
            )
            else -> LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 96.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(current, key = { it.list.id }) { summary ->
                    ListCard(
                        summary = summary,
                        onOpen = { viewModel.openList(summary.list.id) },
                        onRename = { renaming = summary },
                        onDuplicate = {
                            viewModel.duplicateList(summary.list.id, "${summary.list.name} (cópia)")
                            showMessage("Lista \"${summary.list.name}\" duplicada")
                        },
                        onDelete = {
                            scope.launch {
                                val deleted = viewModel.deleteList(summary.list.id) ?: return@launch
                                showMessage(
                                    message = "\"${deleted.list.name}\" excluída",
                                    actionLabel = "Desfazer",
                                    long = true
                                ) { viewModel.restoreList(deleted) }
                            }
                        },
                        modifier = Modifier.animateItemPlacement()
                    )
                }
            }
        }
    }

    if (showNewListDialog) {
        ListFormDialog(
            title = "Nova lista",
            confirmLabel = "Criar",
            initialName = "",
            showBudget = true,
            onConfirm = { name, budget ->
                showNewListDialog = false
                viewModel.createList(name.ifBlank { NEW_LIST_FALLBACK_NAME }, budget)
            },
            onDismiss = { showNewListDialog = false }
        )
    }

    renaming?.let { summary ->
        ListFormDialog(
            title = "Renomear lista",
            confirmLabel = "Salvar",
            initialName = summary.list.name,
            showBudget = false,
            onConfirm = { name, _ ->
                if (name.isNotBlank()) viewModel.renameList(summary.list.id, name)
                renaming = null
            },
            onDismiss = { renaming = null }
        )
    }
}

/** Card de uma lista: nome, nº de itens, total, orçamento e barrinha de uso. */
@Composable
private fun ListCard(
    summary: ListSummary,
    onOpen: () -> Unit,
    onRename: () -> Unit,
    onDuplicate: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = MaterialTheme.colorScheme
    val list = summary.list
    val status = BudgetStatus(total = summary.total, maxBudget = list.maxBudget)
    var showMenu by remember { mutableStateOf(false) }

    Card(
        onClick = onOpen,
        modifier = modifier.fillMaxWidth(),
        shape = ListCardShape,
        colors = CardDefaults.cardColors(containerColor = colors.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 14.dp, top = 12.dp, bottom = 14.dp, end = 4.dp),
            verticalAlignment = Alignment.Top
        ) {
            Box(
                modifier = Modifier
                    .padding(top = 2.dp)
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(colors.primaryContainer),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Default.ShoppingBasket,
                    contentDescription = null,
                    tint = colors.onPrimaryContainer
                )
            }

            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(start = 12.dp)
            ) {
                Text(
                    list.name,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = colors.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    itemsLabel(summary),
                    style = MaterialTheme.typography.bodySmall,
                    color = colors.onSurfaceVariant
                )

                Spacer(Modifier.height(8.dp))

                Row(verticalAlignment = Alignment.Bottom) {
                    Text(
                        listsCurrency.format(summary.total),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = colors.primary
                    )
                    if (status.hasBudget) {
                        Text(
                            " de ${listsCurrency.format(list.maxBudget)}",
                            style = MaterialTheme.typography.bodySmall,
                            color = colors.onSurfaceVariant,
                            modifier = Modifier
                                .weight(1f)
                                .padding(bottom = 2.dp),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            if (status.isOver) "Passou ${listsCurrency.format(-status.remaining)}"
                            else "Restam ${listsCurrency.format(status.remaining)}",
                            style = MaterialTheme.typography.labelMedium,
                            color = if (status.isOver) colors.error else colors.onSurfaceVariant,
                            modifier = Modifier.padding(bottom = 2.dp, end = 8.dp)
                        )
                    } else {
                        Text(
                            " · sem orçamento",
                            style = MaterialTheme.typography.bodySmall,
                            color = colors.onSurfaceVariant,
                            modifier = Modifier.padding(bottom = 2.dp)
                        )
                    }
                }

                if (status.hasBudget) {
                    BudgetBar(
                        fraction = status.usedFraction.toFloat(),
                        color = when (status.band) {
                            BudgetBand.SAFE -> colors.primary
                            BudgetBand.WARNING -> colors.tertiary
                            BudgetBand.OVER -> colors.error
                        },
                        trackColor = colors.surfaceVariant,
                        modifier = Modifier.padding(top = 6.dp, end = 8.dp)
                    )
                }
            }

            Box {
                IconButton(onClick = { showMenu = true }) {
                    Icon(
                        Icons.Default.MoreVert,
                        contentDescription = "Opções da lista ${list.name}",
                        tint = colors.onSurfaceVariant
                    )
                }
                DropdownMenu(expanded = showMenu, onDismissRequest = { showMenu = false }) {
                    DropdownMenuItem(
                        text = { Text("Renomear") },
                        leadingIcon = { Icon(Icons.Default.Edit, contentDescription = null) },
                        onClick = {
                            showMenu = false
                            onRename()
                        }
                    )
                    DropdownMenuItem(
                        text = { Text("Duplicar") },
                        leadingIcon = { Icon(Icons.Default.ContentCopy, contentDescription = null) },
                        onClick = {
                            showMenu = false
                            onDuplicate()
                        }
                    )
                    DropdownMenuItem(
                        text = { Text("Excluir", color = colors.error) },
                        leadingIcon = { Icon(Icons.Default.Delete, contentDescription = null, tint = colors.error) },
                        onClick = {
                            showMenu = false
                            onDelete()
                        }
                    )
                }
            }
        }
    }
}

private fun itemsLabel(summary: ListSummary): String {
    val count = summary.itemCount
    if (count == 0) return "Lista vazia"
    val items = if (count == 1) "1 item" else "$count itens"
    return when {
        summary.inCartCount == count -> "$items · tudo no carrinho"
        summary.inCartCount > 0 -> "$items · ${summary.inCartCount} no carrinho"
        else -> items
    }
}

/** Barrinha de uso do orçamento (cheia em 100%; acima disso fica cheia e vermelha). */
@Composable
private fun BudgetBar(
    fraction: Float,
    color: Color,
    trackColor: Color,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(6.dp)
            .clip(CircleShape)
            .background(trackColor)
    ) {
        Box(
            modifier = Modifier
                .fillMaxHeight()
                .fillMaxWidth(fraction.coerceIn(0f, 1f))
                .clip(CircleShape)
                .background(color)
        )
    }
}

/** Sem nenhuma lista (todas excluídas). */
@Composable
private fun NoListsState(onCreate: () -> Unit, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier
                .size(96.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.primaryContainer),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                Icons.Default.ShoppingBasket,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onPrimaryContainer,
                modifier = Modifier.size(48.dp)
            )
        }
        Text(
            "Nenhuma lista ainda",
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.onBackground,
            modifier = Modifier.padding(top = 20.dp)
        )
        Text(
            "Crie uma lista para cada compra: a do mês, a da semana, a do churrasco…",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(top = 6.dp, bottom = 20.dp)
        )
        Button(onClick = onCreate) {
            Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
            Text("Criar lista", modifier = Modifier.padding(start = 6.dp))
        }
    }
}

/**
 * Diálogo para criar ([showBudget] = true: nome + orçamento opcional) ou renomear uma lista.
 * O orçamento vazio fica 0 (sem orçamento).
 */
@Composable
fun ListFormDialog(
    title: String,
    confirmLabel: String,
    initialName: String,
    showBudget: Boolean,
    onConfirm: (name: String, budget: Double) -> Unit,
    onDismiss: () -> Unit
) {
    var name by remember { mutableStateOf(initialName) }
    var budgetText by remember { mutableStateOf("") }
    val focusRequester = remember { FocusRequester() }

    fun confirm() = onConfirm(name.trim(), budgetText.toDoubleOrNull() ?: 0.0)

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Column {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it.take(40) },
                    label = { Text("Nome da lista") },
                    placeholder = { Text("Ex.: Compra do mês") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(
                        capitalization = KeyboardCapitalization.Sentences,
                        imeAction = if (showBudget) ImeAction.Next else ImeAction.Done
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .focusRequester(focusRequester)
                )
                if (showBudget) {
                    OutlinedTextField(
                        value = budgetText,
                        onValueChange = { text ->
                            budgetText = text.filter { c -> c.isDigit() || c == '.' || c == ',' }.replace(',', '.')
                        },
                        label = { Text("Orçamento (R$, opcional)") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 12.dp)
                    )
                }
            }
            // abre o teclado já no nome (se o campo ainda não estiver pronto, só não foca)
            LaunchedEffect(Unit) {
                try {
                    focusRequester.requestFocus()
                } catch (e: IllegalStateException) {
                    // ignora
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { confirm() },
                // renomear exige um nome; criar pode ficar sem (vira "Nova lista")
                enabled = showBudget || name.isNotBlank()
            ) {
                Text(confirmLabel)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancelar") }
        }
    )
}
