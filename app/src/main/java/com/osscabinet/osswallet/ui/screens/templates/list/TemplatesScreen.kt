package com.osscabinet.ossWallet.ui.screens.templates.list

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
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import com.osscabinet.ossWallet.domain.model.Template
import com.osscabinet.ossWallet.domain.model.Category
import com.osscabinet.ossWallet.ui.components.*
import com.osscabinet.ossWallet.ui.components.getIconByName
import com.osscabinet.ossWallet.ui.components.accounts.AccountSelection
import com.osscabinet.ossWallet.ui.viewmodel.TemplateViewModel
import com.osscabinet.ossWallet.ui.viewmodel.AccountViewModel
import java.math.BigDecimal

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TemplatesScreen(
    onBackClick: () -> Unit,
    viewModel: TemplateViewModel,
    accountViewModel: AccountViewModel,
    categoryMap: Map<Long, com.osscabinet.ossWallet.domain.model.Category>,
    onAddTemplateClick: (Long?) -> Unit,
    onCreatePlannedPayment: (Template) -> Unit
) {
    val templates by viewModel.templates.collectAsStateWithLifecycle()
    val starredTemplates by viewModel.starredTemplates.collectAsStateWithLifecycle()

    TemplatesScreenContent(
        templates = templates,
        starredTemplates = starredTemplates,
        categoryMap = categoryMap,
        onBackClick = onBackClick,
        onAddTemplateClick = onAddTemplateClick,
        onDeleteTemplate = { viewModel.deleteTemplate(it) },
        onUpdateOrder = { viewModel.updateTemplatesOrder(it) },
        onToggleStar = { viewModel.toggleStar(it) },
        onCreatePlannedPayment = onCreatePlannedPayment
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TemplatesScreenContent(
    templates: List<Template>,
    starredTemplates: List<Template>,
    categoryMap: Map<Long, Category>,
    onBackClick: () -> Unit,
    onAddTemplateClick: (Long?) -> Unit,
    onDeleteTemplate: (Template) -> Unit,
    onUpdateOrder: (List<Template>) -> Unit,
    onToggleStar: (Template) -> Unit,
    onCreatePlannedPayment: (Template) -> Unit
) {
    var editList by remember { mutableStateOf(emptyList<Template>()) }
    var templateToDelete by remember { mutableStateOf<Template?>(null) }

    AeonConfirmDeleteDialog(
        show = templateToDelete != null,
        title = "Delete Template",
        message = "Are you sure you want to delete the template \"${templateToDelete?.name}\"?",
        onConfirm = {
            templateToDelete?.let { onDeleteTemplate(it) }
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
                    IconButton(onClick = if (isReorderMode) { { isReorderMode = false } } else { onBackClick }) {
                        Icon(
                            if (isReorderMode) Icons.Rounded.Close else Icons.AutoMirrored.Filled.ArrowBack,
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
                    onDragEnd = { newList -> onUpdateOrder(newList) },
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
                        categoryName = categoryMap[template.categoryId]?.name ?: "Unknown",
                        categoryIconName = categoryMap[template.categoryId]?.iconName,
                        onEdit = { navigate { onAddTemplateClick(template.id) } },
                        onDelete = { templateToDelete = template },
                        onToggleStar = { onToggleStar(template) },
                        onCreatePlannedPayment = { onCreatePlannedPayment(template) }
                    )
                }
            }
        }
    }
}

@Composable
fun ReorderTemplateItem(
    template: Template,
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
    template: Template,
    categoryName: String,
    categoryIconName: String?,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onToggleStar: () -> Unit,
    onCreatePlannedPayment: () -> Unit
) {
    val (badgeColor, badgeIcon) = when (template.transactionType) {
        0 -> MaterialTheme.colorScheme.error to Icons.Rounded.ArrowDownward          // expense — red
        1 -> Color(0xFF2E7D32) to Icons.Rounded.ArrowUpward                         // income — green
        else -> MaterialTheme.colorScheme.secondary to Icons.Rounded.Sync           // transfer
    }

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
            Box(contentAlignment = Alignment.Center) {
                Surface(
                    modifier = Modifier.size(40.dp),
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = getIconByName(categoryIconName),
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
                Surface(
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .offset(x = (-4).dp, y = 4.dp),
                    shape = CircleShape,
                    color = badgeColor,
                    border = BorderStroke(2.dp, MaterialTheme.colorScheme.surface)
                ) {
                    Icon(
                        imageVector = badgeIcon,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier
                            .size(16.dp)
                            .padding(2.dp)
                    )
                }
            }
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(template.name, style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold))
                Text(categoryName, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.outline)
            }
            IconButton(onClick = onCreatePlannedPayment) {
                Icon(
                    imageVector = Icons.Rounded.Schedule,
                    contentDescription = "Create Planned Payment",
                    tint = MaterialTheme.colorScheme.primary
                )
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
    template: Template?,
    accounts: List<com.osscabinet.ossWallet.domain.model.Account>,
    onDismiss: () -> Unit,
    onConfirm: (String, Long, Double, Int, String?, String?) -> Unit
) {
    var name by remember { mutableStateOf(template?.name ?: "") }
    var categoryId by remember { mutableLongStateOf(template?.categoryId ?: 1002L) }
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
                // Note: Simplified for now, real implementation should use a category picker
                OutlinedTextField(
                    value = categoryId.toString(),
                    onValueChange = { categoryId = it.toLongOrNull() ?: 1002L },
                    label = { Text("Category ID") },
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
            Button(onClick = { onConfirm(name, categoryId, amount.toDoubleOrNull() ?: 0.0, type, note.ifBlank { null }, accountId) }) {
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
        AccountSelection(
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

@Preview(showBackground = true)
@Composable
fun TemplatesScreenPreview() {
    val sampleTemplates = listOf(
        Template(
            id = 1L,
            name = "Coffee Shop",
            categoryId = 1001L,
            amount = BigDecimal("5.50"),
            transactionType = 0,
            icon = null,
            colorHex = null,
            accountId = null,
            note = "Morning coffee",
            payee = null,
            location = null,
            paymentMethod = null,
            currency = null,
            tags = emptyList(),
            isStarred = true,
            displayOrder = 0
        ),
        Template(
            id = 2L,
            name = "Monthly Rent",
            categoryId = 1002L,
            amount = BigDecimal("1200.00"),
            transactionType = 0,
            icon = null,
            colorHex = null,
            accountId = null,
            note = "Apartment rent",
            payee = null,
            location = null,
            paymentMethod = null,
            currency = null,
            tags = emptyList(),
            isStarred = true,
            displayOrder = 1
        ),
        Template(
            id = 3L,
            name = "Salary",
            categoryId = 1003L,
            amount = BigDecimal("3500.00"),
            transactionType = 1,
            icon = null,
            colorHex = null,
            accountId = null,
            note = "Monthly salary",
            payee = null,
            location = null,
            paymentMethod = null,
            currency = null,
            tags = emptyList(),
            isStarred = false,
            displayOrder = 2
        ),
        Template(
            id = 4L,
            name = "Grocery Shopping",
            categoryId = 1004L,
            amount = BigDecimal("150.00"),
            transactionType = 0,
            icon = null,
            colorHex = null,
            accountId = null,
            note = "Weekly groceries",
            payee = null,
            location = null,
            paymentMethod = null,
            currency = null,
            tags = emptyList(),
            isStarred = false,
            displayOrder = 3
        ),
        Template(
            id = 5L,
            name = "Savings Transfer",
            categoryId = 1005L,
            amount = BigDecimal("500.00"),
            transactionType = 2,
            icon = null,
            colorHex = null,
            accountId = null,
            note = "Monthly savings",
            payee = null,
            location = null,
            paymentMethod = null,
            currency = null,
            tags = emptyList(),
            isStarred = false,
            displayOrder = 4
        )
    )
    
    val sampleCategories = mapOf(
        1001L to Category(1001L, "Food & Dining", "restaurant", -14513374, false, null),
        1002L to Category(1002L, "Housing", "home", -16726016, false, null),
        1003L to Category(1003L, "Income", "trending_up", -14513374, false, null),
        1004L to Category(1004L, "Groceries", "shopping_cart", -26513, false, null),
        1005L to Category(1005L, "Transfer", "sync_alt", -769226, false, null)
    )
    
    MaterialTheme {
        TemplatesScreenContent(
            templates = sampleTemplates,
            starredTemplates = sampleTemplates.filter { it.isStarred },
            categoryMap = sampleCategories,
            onBackClick = {},
            onAddTemplateClick = {},
            onDeleteTemplate = {},
            onUpdateOrder = {},
            onToggleStar = {},
            onCreatePlannedPayment = {}
        )
    }
}

@Preview(showBackground = true, widthDp = 360)
@Composable
fun TemplateItemPreview() {
    val sampleTemplate = Template(
        id = 1L,
        name = "Coffee Shop",
        categoryId = 1001L,
        amount = BigDecimal("5.50"),
        transactionType = 0,
        icon = null,
        colorHex = null,
        accountId = null,
        note = "Morning coffee",
        payee = null,
        location = null,
        paymentMethod = null,
        currency = null,
        tags = emptyList(),
        isStarred = true,
        displayOrder = 0
    )

    MaterialTheme {
        Box(modifier = Modifier.padding(16.dp)) {
            TemplateItem(
                template = sampleTemplate,
                categoryName = "Food & Dining",
                categoryIconName = "restaurant",
                onEdit = {},
                onDelete = {},
                onToggleStar = {},
                onCreatePlannedPayment = {}
            )
        }
    }
}
