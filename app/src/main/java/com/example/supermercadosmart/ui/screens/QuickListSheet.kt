package com.example.supermercadosmart.ui.screens

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.PlaylistAdd
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.supermercadosmart.data.Category
import com.example.supermercadosmart.data.QuickEntry
import com.example.supermercadosmart.data.QuickListParser
import kotlinx.coroutines.launch

/** Item ainda não salvo na "Monte sua lista antecipado". */
private data class Draft(
    val id: Long,
    val name: String,
    val quantity: Int,
    val category: Category
)

/**
 * "Monte sua lista antecipado": tela cheia para digitar vários itens de uma vez, só com o nome.
 * Enter (ou o "+") passa o item para a lista de baixo; colar várias linhas cria vários itens.
 * Os itens entram sem preço; o preço é informado no mercado.
 *
 * @param existingNames chaves ([QuickListParser.key]) dos itens que já estão na lista aberta
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun QuickListSheet(
    existingNames: Set<String>,
    onConfirm: (List<Pair<QuickEntry, Category>>) -> Unit,
    onDismiss: () -> Unit
) {
    val drafts = remember { mutableStateListOf<Draft>() }
    var nextId by remember { mutableLongStateOf(1L) }
    var input by remember { mutableStateOf(TextFieldValue("")) }
    var confirmDiscard by remember { mutableStateOf(false) }
    var categoryEdit by remember { mutableStateOf<Draft?>(null) }
    val focusRequester = remember { FocusRequester() }
    val listState = rememberLazyListState()
    val scope = rememberCoroutineScope()

    LaunchedEffect(Unit) { focusRequester.requestFocus() }

    /** Passa o texto para a lista; nome repetido soma na quantidade do que já foi digitado. */
    fun commit(text: String) {
        val parsed = QuickListParser.parse(text)
        if (parsed.isEmpty()) return
        parsed.forEach { entry ->
            val key = QuickListParser.key(entry.name)
            val index = drafts.indexOfFirst { QuickListParser.key(it.name) == key }
            if (index >= 0) {
                val old = drafts[index]
                drafts[index] = old.copy(quantity = (old.quantity + entry.quantity).coerceAtMost(99))
            } else {
                drafts.add(Draft(nextId++, entry.name, entry.quantity, Category.guess(entry.name)))
            }
        }
        // o mais novo aparece no topo
        scope.launch { listState.animateScrollToItem(0) }
    }

    fun close() {
        if (drafts.isEmpty()) onDismiss() else confirmDiscard = true
    }

    fun save() {
        // o que ficou no campo também entra
        commit(input.text)
        input = TextFieldValue("")
        if (drafts.isNotEmpty()) {
            onConfirm(drafts.map { QuickEntry(it.name, it.quantity) to it.category })
        }
    }

    Dialog(
        onDismissRequest = { close() },
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.background
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                // Topo: fechar, título e "Adicionar (N)" (fica no alto para o teclado não cobrir)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 4.dp, end = 12.dp, top = 8.dp, bottom = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = { close() }) {
                        Icon(Icons.Default.Close, contentDescription = "Fechar")
                    }
                    Text(
                        "Monte sua lista antecipado",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                        maxLines = 2,
                        modifier = Modifier.weight(1f)
                    )
                    val canSave = drafts.isNotEmpty() || QuickListParser.parse(input.text).isNotEmpty()
                    Button(
                        onClick = { save() },
                        enabled = canSave,
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp)
                    ) {
                        Text(if (drafts.isEmpty()) "Adicionar" else "Adicionar (${drafts.size})")
                    }
                }

                Text(
                    "Digite um produto e toque em Enter. O preço você informa no mercado.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 16.dp)
                )

                OutlinedTextField(
                    value = input,
                    onValueChange = { value ->
                        val text = value.text
                        if (text.contains('\n') || text.contains('\r')) {
                            // Enter ou texto colado: as linhas completas viram itens,
                            // o que vem depois da última quebra continua no campo
                            val cut = maxOf(text.lastIndexOf('\n'), text.lastIndexOf('\r'))
                            commit(text.substring(0, cut))
                            val rest = text.substring(cut + 1)
                            input = TextFieldValue(rest, selection = TextRange(rest.length))
                        } else {
                            input = value
                        }
                    },
                    placeholder = { Text("Ex.: arroz, 2 leite, pão de forma") },
                    trailingIcon = {
                        IconButton(
                            onClick = {
                                commit(input.text)
                                input = TextFieldValue("")
                            },
                            enabled = input.text.isNotBlank()
                        ) {
                            Icon(Icons.Default.Add, contentDescription = "Incluir item")
                        }
                    },
                    maxLines = 3,
                    keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp)
                        .focusRequester(focusRequester)
                )

                if (drafts.isEmpty()) {
                    QuickListTips(modifier = Modifier.weight(1f))
                } else {
                    Text(
                        "${drafts.size} ${if (drafts.size == 1) "item" else "itens"} · toque no ícone para trocar a categoria",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 20.dp, vertical = 4.dp)
                    )
                    LazyColumn(
                        state = listState,
                        modifier = Modifier.weight(1f),
                        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 24.dp)
                    ) {
                        // o mais novo primeiro (salva na ordem digitada)
                        items(drafts.asReversed(), key = { it.id }) { draft ->
                            DraftRow(
                                draft = draft,
                                alreadyInList = QuickListParser.key(draft.name) in existingNames,
                                onCategoryClick = { categoryEdit = draft },
                                onQuantityChange = { quantity ->
                                    val index = drafts.indexOfFirst { it.id == draft.id }
                                    if (index >= 0) drafts[index] = drafts[index].copy(quantity = quantity)
                                },
                                onRemove = { drafts.removeAll { it.id == draft.id } },
                                modifier = Modifier.animateItemPlacement()
                            )
                            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                        }
                    }
                }
            }
        }

        categoryEdit?.let { editing ->
            CategoryPickerDialog(
                itemName = editing.name,
                current = editing.category,
                onSelect = { category ->
                    val index = drafts.indexOfFirst { it.id == editing.id }
                    if (index >= 0) drafts[index] = drafts[index].copy(category = category)
                    categoryEdit = null
                },
                onDismiss = { categoryEdit = null }
            )
        }

        if (confirmDiscard) {
            AlertDialog(
                onDismissRequest = { confirmDiscard = false },
                title = { Text("Descartar itens?") },
                text = {
                    Text(
                        "${drafts.size} ${if (drafts.size == 1) "item ainda não foi adicionado" else "itens ainda não foram adicionados"} à lista."
                    )
                },
                confirmButton = {
                    TextButton(onClick = {
                        confirmDiscard = false
                        onDismiss()
                    }) { Text("Descartar") }
                },
                dismissButton = {
                    TextButton(onClick = { confirmDiscard = false }) { Text("Continuar") }
                }
            )
        }
    }
}

@Composable
private fun DraftRow(
    draft: Draft,
    alreadyInList: Boolean,
    onCategoryClick: () -> Unit,
    onQuantityChange: (Int) -> Unit,
    onRemove: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        CategoryBadge(
            category = draft.category,
            size = 40,
            modifier = Modifier
                .clip(CircleShape)
                .clickable(onClick = onCategoryClick)
        )
        Column(
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = 12.dp)
        ) {
            Text(
                draft.name,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.SemiBold,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                if (alreadyInList) "${draft.category.label} · já está na lista" else draft.category.label,
                style = MaterialTheme.typography.bodySmall,
                color = if (alreadyInList) MaterialTheme.colorScheme.tertiary
                else MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        // Quantidade [− n +]
        Row(
            modifier = Modifier
                .clip(RoundedCornerShape(50))
                .background(MaterialTheme.colorScheme.surface),
            verticalAlignment = Alignment.CenterVertically
        ) {
            SmallRoundButton(
                onClick = { if (draft.quantity > 1) onQuantityChange(draft.quantity - 1) },
                enabled = draft.quantity > 1
            ) {
                Icon(Icons.Default.Remove, contentDescription = "Diminuir", modifier = Modifier.size(18.dp))
            }
            Text(
                draft.quantity.toString(),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(horizontal = 4.dp)
            )
            SmallRoundButton(
                onClick = { if (draft.quantity < 99) onQuantityChange(draft.quantity + 1) },
                enabled = draft.quantity < 99
            ) {
                Icon(Icons.Default.Add, contentDescription = "Aumentar", modifier = Modifier.size(18.dp))
            }
        }
        Spacer(Modifier.width(4.dp))
        IconButton(onClick = onRemove) {
            Icon(
                Icons.Default.Close,
                contentDescription = "Remover ${draft.name}",
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun SmallRoundButton(onClick: () -> Unit, enabled: Boolean, content: @Composable () -> Unit) {
    Box(
        modifier = Modifier
            .size(32.dp)
            .clip(CircleShape)
            .clickable(enabled = enabled, onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Box(modifier = Modifier.alpha(if (enabled) 1f else 0.35f)) {
            content()
        }
    }
}

/** Dicas enquanto ainda não há itens. */
@Composable
private fun QuickListTips(modifier: Modifier = Modifier) {
    Box(modifier = modifier.fillMaxWidth(), contentAlignment = Alignment.TopCenter) {
        Column(
            modifier = Modifier.padding(horizontal = 32.dp, vertical = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .size(72.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primaryContainer),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.AutoMirrored.Filled.PlaylistAdd,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(36.dp)
                )
            }
            Spacer(Modifier.height(16.dp))
            listOf(
                "\"2 leite\" ou \"leite x2\" já entra com quantidade 2.",
                "Cole uma lista do WhatsApp: cada linha vira um item.",
                "A categoria é escolhida sozinha; toque no ícone para trocar."
            ).forEach { tip ->
                Text(
                    tip,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(vertical = 4.dp)
                )
            }
        }
    }
}
