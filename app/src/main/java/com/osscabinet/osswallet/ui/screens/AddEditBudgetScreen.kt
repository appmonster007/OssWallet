package com.osscabinet.ossWallet.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.ui.unit.sp
import com.osscabinet.ossWallet.domain.model.*
import com.osscabinet.ossWallet.ui.components.*
import com.osscabinet.ossWallet.ui.viewmodel.BudgetViewModel
import com.osscabinet.ossWallet.ui.viewmodel.BudgetIntent
import com.osscabinet.ossWallet.ui.viewmodel.AccountViewModel
import com.osscabinet.ossWallet.ui.viewmodel.TransactionViewModel
import com.osscabinet.ossWallet.ui.viewmodel.TagViewModel
import java.math.BigDecimal
import java.util.UUID

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddEditBudgetScreen(
    budgetId: String? = null,
    budgetViewModel: BudgetViewModel,
    accountViewModel: AccountViewModel,
    transactionViewModel: TransactionViewModel,
    tagViewModel: TagViewModel,
    onBackClick: () -> Unit
) {
    val state by budgetViewModel.state.collectAsStateWithLifecycle()
    val accounts by accountViewModel.accounts.collectAsStateWithLifecycle()
    val categories by transactionViewModel.categories.collectAsStateWithLifecycle()
    val tags by tagViewModel.tags.collectAsStateWithLifecycle()
    
    val editingBudget = remember(budgetId, state.budgets) {
        state.budgets.find { it.budget.id == budgetId }?.budget
    }

    var name by remember { mutableStateOf(editingBudget?.name ?: "") }
    var limit by remember { mutableStateOf(editingBudget?.limit?.toString() ?: "") }
    var period by remember { mutableStateOf(editingBudget?.period ?: BudgetPeriod.MONTHLY) }
    
    // Multi-select state
    var selectedAccountIds by remember { mutableStateOf(editingBudget?.filter?.accountIds ?: emptyList<String>()) }
    var selectedCategoryIds by remember { mutableStateOf(editingBudget?.filter?.categoryIds ?: emptyList<Long>()) }
    var selectedTags by remember { mutableStateOf(editingBudget?.filter?.tags ?: emptyList<String>()) }
    
    var showAccountPicker by remember { mutableStateOf(false) }
    var showCategoryPicker by remember { mutableStateOf(false) }
    var showTagPicker by remember { mutableStateOf(false) }
    var isNumpadVisible by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text(if (budgetId == null) "New Budget" else "Edit Budget", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    if (budgetId != null) {
                        IconButton(onClick = { 
                            budgetViewModel.handleIntent(BudgetIntent.DeleteBudget(budgetId))
                            onBackClick()
                        }) {
                            Icon(Icons.Rounded.Delete, contentDescription = "Delete", tint = MaterialTheme.colorScheme.error)
                        }
                    }
                    Button(
                        onClick = {
                            val budget = Budget(
                                id = budgetId ?: UUID.randomUUID().toString(),
                                name = name,
                                limit = limit.toBigDecimalOrNull() ?: BigDecimal.ZERO,
                                spent = editingBudget?.spent ?: BigDecimal.ZERO,
                                period = period,
                                filter = TransactionFilter(
                                    accountIds = selectedAccountIds,
                                    categoryIds = selectedCategoryIds,
                                    tags = selectedTags,
                                    transactionType = "expense"
                                )
                            )
                            budgetViewModel.handleIntent(BudgetIntent.UpsertBudget(budget))
                            onBackClick()
                        },
                        enabled = name.isNotBlank() && limit.toBigDecimalOrNull() != null,
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("Save")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            // Amount Card
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(140.dp)
                    .clickable { isNumpadVisible = true },
                shape = RoundedCornerShape(32.dp),
                color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
            ) {
                Column(
                    modifier = Modifier.fillMaxSize(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Text("BUDGET LIMIT", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
                    Text(
                        text = if (limit.isEmpty()) "0.00" else limit,
                        style = MaterialTheme.typography.displayMedium,
                        fontWeight = FontWeight.Black,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }

            EditorialInput(
                value = name,
                onValueChange = { name = it },
                label = "BUDGET NAME",
                icon = Icons.Rounded.Label
            )

            // Period Selection
            AeonFlowSectionHeader(title = "TEMPORAL CYCLE")
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                BudgetPeriod.entries.forEach { p ->
                    FilterChip(
                        selected = period == p,
                        onClick = { period = p },
                        label = { Text(p.name) },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp)
                    )
                }
            }

            // Scope Filtering
            AeonFlowSectionHeader(title = "SCOPE FILTERING")
            
            RecordFieldCard(
                icon = Icons.Rounded.AccountBalance,
                label = "ACCOUNTS",
                value = if (selectedAccountIds.isEmpty()) "All Accounts" else "${selectedAccountIds.size} Selected",
                onClick = { showAccountPicker = true },
                isSlim = true,
                iconContainerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.5f)
            )

            RecordFieldCard(
                icon = Icons.Rounded.Category,
                label = "CATEGORIES",
                value = if (selectedCategoryIds.isEmpty()) "All Categories" else "${selectedCategoryIds.size} Selected",
                onClick = { showCategoryPicker = true },
                isSlim = true,
                iconContainerColor = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.5f)
            )

            RecordFieldCard(
                icon = Icons.Rounded.Label,
                label = "LABELS",
                value = if (selectedTags.isEmpty()) "All Labels" else "${selectedTags.size} Selected",
                onClick = { showTagPicker = true },
                isSlim = true,
                iconContainerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
            )
        }

        if (isNumpadVisible) {
            ModalBottomSheet(onDismissRequest = { isNumpadVisible = false }) {
                CalculatorNumpad(
                    amount = limit,
                    onAmountChange = { newAmount, _ -> limit = newAmount },
                    onDone = { isNumpadVisible = false },
                    modifier = Modifier.padding(16.dp)
                )
            }
        }

        if (showAccountPicker) {
            MultiSelectPickerSheet(
                title = "Select Accounts",
                items = accounts.map { it.id to it.name },
                selectedIds = selectedAccountIds,
                onSelectionChanged = { selectedAccountIds = it },
                onDismiss = { showAccountPicker = false }
            )
        }

        if (showCategoryPicker) {
            MultiSelectPickerSheet(
                title = "Select Categories",
                items = categories.map { it.id.toString() to it.name },
                selectedIds = selectedCategoryIds.map { it.toString() },
                onSelectionChanged = { selectedCategoryIds = it.map { id -> id.toLong() } },
                onDismiss = { showCategoryPicker = false }
            )
        }

        if (showTagPicker) {
            MultiSelectPickerSheet(
                title = "Select Labels",
                items = tags.map { it.name to it.name },
                selectedIds = selectedTags,
                onSelectionChanged = { selectedTags = it },
                onDismiss = { showTagPicker = false }
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MultiSelectPickerSheet(
    title: String,
    items: List<Pair<String, String>>, // id to label
    selectedIds: List<String>,
    onSelectionChanged: (List<String>) -> Unit,
    onDismiss: () -> Unit
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp)
    ) {
        Column(modifier = Modifier.padding(24.dp).padding(bottom = 32.dp).fillMaxHeight(0.6f)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(title, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                TextButton(onClick = { onSelectionChanged(emptyList()) }) {
                    Text("Clear All")
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
            
            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(items) { (id, label) ->
                    val isSelected = id in selectedIds
                    Surface(
                        onClick = {
                            if (isSelected) onSelectionChanged(selectedIds - id)
                            else onSelectionChanged(selectedIds + id)
                        },
                        shape = RoundedCornerShape(16.dp),
                        color = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                    ) {
                        Row(
                            modifier = Modifier.padding(16.dp).fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(label, modifier = Modifier.weight(1f))
                            Checkbox(checked = isSelected, onCheckedChange = null)
                        }
                    }
                }
            }
        }
    }
}
