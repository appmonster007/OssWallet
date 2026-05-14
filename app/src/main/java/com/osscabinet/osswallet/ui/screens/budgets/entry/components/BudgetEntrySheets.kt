package com.osscabinet.ossWallet.ui.screens.budgets.entry.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.TrendingDown
import androidx.compose.material.icons.automirrored.rounded.TrendingUp
import androidx.compose.material.icons.rounded.Update
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.osscabinet.ossWallet.domain.model.*
import com.osscabinet.ossWallet.ui.components.*
import com.osscabinet.ossWallet.ui.screens.transactions.entry.sheets.CalculatorSheet

@Composable
fun BudgetEntrySheets(
    showCustomStartPicker: Boolean,
    onDismissCustomStartPicker: () -> Unit,
    customStartDateMillis: Long,
    onCustomStartDateSelected: (Long) -> Unit,
    showCustomEndPicker: Boolean,
    onDismissCustomEndPicker: () -> Unit,
    customEndDateMillis: Long,
    onCustomEndDateSelected: (Long) -> Unit,
    showNumpad: Boolean,
    onDismissNumpad: () -> Unit,
    limitText: String,
    onLimitChanged: (String) -> Unit,
    showAccountSheet: Boolean,
    onDismissAccountSheet: () -> Unit,
    accountQuery: String,
    onAccountQueryChange: (String) -> Unit,
    accounts: List<Account>,
    selectedAccountIds: Set<String>,
    onAccountSelectionChange: (Set<String>) -> Unit,
    showPeriodSheet: Boolean,
    onDismissPeriodSheet: () -> Unit,
    period: BudgetPeriod,
    onPeriodSelected: (BudgetPeriod) -> Unit,
    showTrackTypeSheet: Boolean,
    onDismissTrackTypeSheet: () -> Unit,
    selectedTrackTypes: Set<TransactionType>,
    onTrackTypeSelectionChange: (Set<TransactionType>) -> Unit,
    showCategorySheet: Boolean,
    onDismissCategorySheet: () -> Unit,
    categoryQuery: String,
    onCategoryQueryChange: (String) -> Unit,
    categories: List<Category>,
    selectedCategoryIds: Set<Long>,
    onCategorySelectionChange: (Set<Long>) -> Unit,
    showTagSheet: Boolean,
    onDismissTagSheet: () -> Unit,
    availableTags: List<String>,
    selectedTags: Set<String>,
    onTagSelectionChange: (Set<String>) -> Unit,
    onAddTag: (String) -> Unit
) {
    if (showCustomStartPicker) {
        AeonDatePickerDialog(
            selectedDateMillis = customStartDateMillis,
            onDateSelected = onCustomStartDateSelected,
            onDismiss = onDismissCustomStartPicker
        )
    }
    if (showCustomEndPicker) {
        AeonDatePickerDialog(
            selectedDateMillis = customEndDateMillis,
            onDateSelected = onCustomEndDateSelected,
            onDismiss = onDismissCustomEndPicker
        )
    }
    if (showNumpad) {
        CalculatorSheet(
            currentAmount = limitText,
            transactionType = TransactionType.INCOME,
            decimalPrecision = 2,
            onAmountChanged = onLimitChanged,
            onDismiss = onDismissNumpad
        )
    }

    if (showAccountSheet) {
        val filteredAccounts = remember(accountQuery, accounts) {
            if (accountQuery.isBlank()) accounts
            else accounts.filter { it.name.contains(accountQuery, ignoreCase = true) }
        }

        AeonFullScreenDialog(onDismissRequest = onDismissAccountSheet) {
            AeonSelectionScreen(
                title = "Select Accounts",
                onBack = onDismissAccountSheet
            ) { padding ->
                Column(modifier = Modifier
                    .padding(padding)
                    .padding(horizontal = 24.dp)) {
                    Text(
                        text = "Choose which accounts to include in this budget",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    AeonSearchField(
                        query = accountQuery,
                        onQueryChange = onAccountQueryChange,
                        placeholder = "Search accounts..."
                    )

                    Spacer(modifier = Modifier.height(24.dp))
                    Box(modifier = Modifier.weight(1f, fill = false)) {
                        Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                            AeonHybridSelector(
                                items = filteredAccounts,
                                mode = AeonSelectMode.MULTI,
                                selectedIds = selectedAccountIds,
                                onSelectionChange = onAccountSelectionChange,
                                idMapper = { it.id },
                                labelMapper = { it.name },
                                detailMapper = { it.currencyCode },
                                iconMapper = { getAccountIconByName(it.icon) },
                                colorMapper = { Color(android.graphics.Color.parseColor(it.colorHex)) }
                            )
                        }
                    }
                }
            }
        }
    }

    if (showPeriodSheet) {
        AeonFullScreenDialog(onDismissRequest = onDismissPeriodSheet) {
            AeonSelectionScreen(
                title = "Reset Period",
                onBack = onDismissPeriodSheet
            ) { padding ->
                Column(modifier = Modifier
                    .padding(padding)
                    .padding(horizontal = 24.dp)) {
                    Text(
                        text = "How often should this budget reset?",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                    )
                    Spacer(modifier = Modifier.height(24.dp))
                    Box(modifier = Modifier.weight(1f, fill = false)) {
                        Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                            AeonHybridSelector(
                                items = BudgetPeriod.entries,
                                mode = AeonSelectMode.SINGLE,
                                selectedIds = setOf(period.ordinal.toLong()),
                                onSelectionChange = { ids ->
                                    ids.firstOrNull()?.let { ordinal ->
                                        BudgetPeriod.entries.getOrNull(ordinal.toInt())?.let { onPeriodSelected(it) }
                                    }
                                },
                                idMapper = { it.ordinal.toLong() },
                                labelMapper = {
                                    it.name.lowercase().replaceFirstChar { c -> c.uppercase() }
                                },
                                iconMapper = { Icons.Rounded.Update }
                            )
                        }
                    }
                }
            }
        }
    }

    if (showTrackTypeSheet) {
        AeonFullScreenDialog(onDismissRequest = onDismissTrackTypeSheet) {
            AeonSelectionScreen(
                title = "Track Types",
                onBack = onDismissTrackTypeSheet
            ) { padding ->
                Column(modifier = Modifier
                    .padding(padding)
                    .padding(horizontal = 24.dp)) {
                    Text(
                        text = "Should this budget track expenses, income, or both?",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                    )
                    Spacer(modifier = Modifier.height(24.dp))
                    Box(modifier = Modifier.weight(1f, fill = false)) {
                        Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                            AeonHybridSelector(
                                items = listOf(TransactionType.EXPENSE, TransactionType.INCOME),
                                mode = AeonSelectMode.MULTI,
                                selectedIds = selectedTrackTypes.map { it.ordinal.toLong() }
                                    .toSet(),
                                onSelectionChange = { ids ->
                                    if (ids.isNotEmpty()) {
                                        onTrackTypeSelectionChange(ids.mapNotNull { ordinal ->
                                            listOf(TransactionType.EXPENSE, TransactionType.INCOME).getOrNull(ordinal.toInt())
                                        }.toSet())
                                    }
                                },
                                idMapper = { it.ordinal.toLong() },
                                labelMapper = {
                                    it.displayName
                                },
                                iconMapper = {
                                    when (it) {
                                        TransactionType.EXPENSE -> Icons.AutoMirrored.Rounded.TrendingDown
                                        TransactionType.INCOME -> Icons.AutoMirrored.Rounded.TrendingUp
                                        else -> Icons.Rounded.Update
                                    }
                                }
                            )
                        }
                    }
                }
            }
        }
    }

    if (showCategorySheet) {
        val filteredCategories = remember(categoryQuery, categories) {
            if (categoryQuery.isBlank()) categories
            else categories.filter { it.name.contains(categoryQuery, ignoreCase = true) }
        }

        AeonFullScreenDialog(onDismissRequest = onDismissCategorySheet) {
            AeonSelectionScreen(
                title = "Categories",
                onBack = onDismissCategorySheet
            ) { padding ->
                Column(modifier = Modifier
                    .padding(padding)
                    .padding(24.dp)) {
                    
                    AeonSearchField(
                        query = categoryQuery,
                        onQueryChange = onCategoryQueryChange,
                        placeholder = "Search categories..."
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    Box(modifier = Modifier.weight(1f, fill = false)) {
                        Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                            AeonHierarchicalHybridSelector(
                                items = filteredCategories,
                                mode = AeonSelectMode.MULTI,
                                selectedIds = selectedCategoryIds,
                                onSelectionChange = onCategorySelectionChange,
                                idMapper = { it.id },
                                labelMapper = { it.name },
                                parentIdMapper = { it.parentId },
                                iconMapper = { getIconByName(it.iconName) },
                                colorMapper = {
                                    if (it.colorHex == -1) Color(0xFF9E9E9E) else Color(
                                        it.colorHex
                                    )
                                }
                            )
                        }
                    }
                }
            }
        }
    }

    if (showTagSheet) {
        BudgetTagSelection(
            availableTags = availableTags,
            selectedTags = selectedTags,
            onSelectionChanged = onTagSelectionChange,
            onAddTag = onAddTag,
            onDismiss = onDismissTagSheet
        )
    }
}
