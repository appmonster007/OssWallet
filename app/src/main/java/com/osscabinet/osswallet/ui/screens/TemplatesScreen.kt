package com.osscabinet.ossWallet.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.osscabinet.ossWallet.data.local.entity.TemplateEntity
import com.osscabinet.ossWallet.ui.components.*
import com.osscabinet.ossWallet.ui.viewmodel.TemplateViewModel
import com.osscabinet.ossWallet.ui.viewmodel.AccountViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TemplatesScreen(
    viewModel: TemplateViewModel,
    accountViewModel: AccountViewModel,
    onAddTemplateClick: (Long?) -> Unit,
    onBackClick: () -> Unit
) {
    val templates by viewModel.templates.collectAsStateWithLifecycle()
    val starredTemplates by viewModel.starredTemplates.collectAsStateWithLifecycle()

    var editList by remember { mutableStateOf(emptyList<TemplateEntity>()) }
    var templateToDelete by remember { mutableStateOf<TemplateEntity?>(null) }

    AeonConfirmDeleteDialog(
        show = templateToDelete != null,
        title = "Delete Template",
        message = "Are you sure you want to delete the template \"${templateToDelete?.name}\"?",
        onConfirm = {
            templateToDelete?.let { viewModel.deleteTemplate(it) }
            templateToDelete = null
        },
        onDismiss = { templateToDelete = null }
    )

    LaunchedEffect(starredTemplates) {
        if (editList.isEmpty() || editList.size != starredTemplates.size || editList.map { it.id }.toSet() != starredTemplates.map { it.id }.toSet()) {
            editList = starredTemplates
        } else {
            // Update properties but keep order
            val orderMap = editList.withIndex().associate { it.value.id to it.index }
            editList = starredTemplates.sortedBy { orderMap[it.id] ?: Int.MAX_VALUE }
        }
    }

    var isReorderMode by remember { mutableStateOf(false) }
    var navTriggered by remember { mutableStateOf(false) }

    val navigate: (() -> Unit) -> Unit = { action ->
        if (!navTriggered) {
            navTriggered = true
            action()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (isReorderMode) "Reorder Starred" else "Templates") },
                navigationIcon = {
                    IconButton(onClick = if (isReorderMode) { { isReorderMode = false } } else { { navigate(onBackClick) } }) {
                        Icon(
                            if (isReorderMode) Icons.Rounded.Close else Icons.AutoMirrored.Rounded.ArrowBack,
                            contentDescription = if (isReorderMode) "Cancel" else "Back"
                        )
                    }
                },
                actions = {
                    if (starredTemplates.isNotEmpty()) {
                        IconButton(onClick = { isReorderMode = !isReorderMode }) {
                            Icon(
                                if (isReorderMode) Icons.Rounded.Check else Icons.Rounded.SwapVert,
                                contentDescription = if (isReorderMode) "Done" else "Reorder Starred"
                            )
                        }
                    }
                }
            )
        },
        floatingActionButton = {
            if (!isReorderMode) {
                AeonFloatingActionButton(
                    onClick = { navigate { onAddTemplateClick(null) } },
                    icon = Icons.Rounded.Add,
                    contentDescription = "Add Template"
                )
            }
        }
    ) { padding ->
        if (isReorderMode) {
            Box(modifier = Modifier.padding(padding)) {
                ReorderableList(
                    items = editList,
                    key = { it.id },
                    onReorder = { newList -> editList = newList },
                    onDragEnd = { newList -> viewModel.updateTemplatesOrder(newList) },
                    modifier = Modifier
                        .fillMaxSize()
                        .background(MaterialTheme.colorScheme.background),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) { template, isDragging ->
                    ReorderTemplateItem(
                        template = template,
                        isDragging = isDragging
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .background(MaterialTheme.colorScheme.background),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(templates, key = { it.id }) { template ->
                    TemplateItem(
                        template = template,
                        onEdit = { navigate { onAddTemplateClick(template.id) } },
                        onDelete = { templateToDelete = template },
                        onToggleStar = { viewModel.toggleStar(template) }
                    )
                }
            }
        }
    }
}

@Composable
fun ReorderTemplateItem(
    template: TemplateEntity,
    isDragging: Boolean = false
) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = if (isDragging) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.9f)
        else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
        border = if (isDragging) BorderStroke(1.dp, MaterialTheme.colorScheme.primary) else null,
        shadowElevation = if (isDragging) 8.dp else 0.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                Icons.Rounded.DragHandle,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.outline,
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.width(16.dp))
            Icon(Icons.Rounded.Star, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
            Spacer(modifier = Modifier.width(16.dp))
            Text(template.name, modifier = Modifier.weight(1f), style = MaterialTheme.typography.titleMedium)
        }
    }
}

@Composable
fun TemplateItem(
    template: TemplateEntity,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onToggleStar: () -> Unit
) {
    Card(
        onClick = onEdit,
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                modifier = Modifier.size(40.dp),
                shape = CircleShape,
                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        when (template.transactionType) {
                            1 -> Icons.Rounded.ArrowUpward
                            2 -> Icons.Rounded.SwapHoriz
                            else -> Icons.Rounded.ArrowDownward
                        },
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(template.name, style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold))
                Text(template.category, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.outline)
            }
            IconButton(onClick = onToggleStar) {
                Icon(
                    imageVector = if (template.isStarred) Icons.Rounded.Star else Icons.Rounded.StarBorder,
                    contentDescription = "Star",
                    tint = if (template.isStarred) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline
                )
            }
            IconButton(onClick = onDelete) {
                Icon(Icons.Rounded.DeleteOutline, contentDescription = "Delete", tint = MaterialTheme.colorScheme.error)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TemplateDialog(
    template: TemplateEntity?,
    accounts: List<com.osscabinet.ossWallet.domain.model.Account>,
    onDismiss: () -> Unit,
    onConfirm: (String, String, Double, Int, String?, String?) -> Unit
) {
    var name by remember { mutableStateOf(template?.name ?: "") }
    var category by remember { mutableStateOf(template?.category ?: "Shopping") }
    var amount by remember { mutableStateOf(template?.amount?.toString() ?: "") }
    var type by remember { mutableIntStateOf(template?.transactionType ?: 0) }
    var note by remember { mutableStateOf(template?.note ?: "") }
    var accountId by remember { mutableStateOf(template?.accountId) }
    
    var showAccountPicker by remember { mutableStateOf(false) }
    val selectedAccount = remember(accountId, accounts) { accounts.find { it.id == accountId } }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (template == null) "New Template" else "Edit Template") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Name") },
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = category,
                    onValueChange = { category = it },
                    label = { Text("Category") },
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = amount,
                    onValueChange = { amount = it },
                    label = { Text("Default Amount") },
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = note,
                    onValueChange = { note = it },
                    label = { Text("Default Note (Optional)") },
                    modifier = Modifier.fillMaxWidth()
                )
                
                Surface(
                    onClick = { showAccountPicker = true },
                    shape = RoundedCornerShape(8.dp),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(selectedAccount?.name ?: "Default Account (Optional)", color = if (selectedAccount != null) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.outline)
                        Icon(Icons.Rounded.KeyboardArrowDown, contentDescription = null)
                    }
                }
                
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(
                        selected = type == 0,
                        onClick = { type = 0 },
                        label = { Text("Expense") }
                    )
                    FilterChip(
                        selected = type == 1,
                        onClick = { type = 1 },
                        label = { Text("Income") }
                    )
                    FilterChip(
                        selected = type == 2,
                        onClick = { type = 2 },
                        label = { Text("Transfer") }
                    )
                }
            }
        },
        confirmButton = {
            Button(onClick = { onConfirm(name, category, amount.toDoubleOrNull() ?: 0.0, type, note.ifBlank { null }, accountId) }) {
                Text("Save")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )

    if (showAccountPicker) {
        AccountPickerSheet(
            accounts = accounts,
            selectedAccount = selectedAccount,
            onAccountSelected = {
                accountId = it.id
                showAccountPicker = false
            },
            onDismiss = { showAccountPicker = false }
        )
    }
}
