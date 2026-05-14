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
import androidx.compose.ui.unit.dp
import com.osscabinet.ossWallet.domain.model.*
import com.osscabinet.ossWallet.ui.components.*
import com.osscabinet.ossWallet.ui.components.entry.*
import com.osscabinet.ossWallet.ui.components.entry.AeonAccountSelection
import com.osscabinet.ossWallet.ui.components.entry.AeonCategorySelection
import com.osscabinet.ossWallet.ui.components.entry.AeonCalculatorSheet

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
        AeonCalculatorSheet(
            currentAmount = limitText,
            transactionType = TransactionType.INCOME,
            decimalPrecision = 2,
            onAmountChanged = onLimitChanged,
            onDismiss = onDismissNumpad
        )
    }

    if (showAccountSheet) {
        AeonAccountSelection(
            accounts = accounts,
            selectedAccountIds = selectedAccountIds,
            onSelectionChange = onAccountSelectionChange,
            onDismiss = onDismissAccountSheet,
            mode = AeonSelectMode.MULTI,
            title = "Select Accounts",
            description = "Choose which accounts to include in this budget"
        )
    }

    if (showPeriodSheet) {
        AeonSelectionSheet(
            title = "Reset Period",
            onDismiss = onDismissPeriodSheet,
            items = BudgetPeriod.entries,
            selectedIds = setOf(period),
            onSelectionChange = { ids: Set<BudgetPeriod> ->
                ids.firstOrNull()?.let { onPeriodSelected(it) }
            },
            idMapper = { it: BudgetPeriod -> it },
            labelMapper = { it.name.lowercase().replaceFirstChar { c -> c.uppercase() } },
            iconMapper = { Icons.Rounded.Update },
            headerContent = {
                Text(
                    text = "How often should this budget reset?",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                    modifier = Modifier.padding(bottom = 16.dp)
                )
            }
        )
    }

    if (showTrackTypeSheet) {
        AeonSelectionSheet(
            title = "Track Types",
            onDismiss = onDismissTrackTypeSheet,
            items = listOf(TransactionType.EXPENSE, TransactionType.INCOME),
            selectedIds = selectedTrackTypes,
            onSelectionChange = onTrackTypeSelectionChange,
            idMapper = { it: TransactionType -> it },
            labelMapper = { it.displayName },
            mode = AeonSelectMode.MULTI,
            iconMapper = {
                when (it) {
                    TransactionType.EXPENSE -> Icons.AutoMirrored.Rounded.TrendingDown
                    TransactionType.INCOME -> Icons.AutoMirrored.Rounded.TrendingUp
                    else -> Icons.Rounded.Update
                }
            },
            headerContent = {
                Text(
                    text = "Should this budget track expenses, income, or both?",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                    modifier = Modifier.padding(bottom = 16.dp)
                )
            }
        )
    }

    if (showCategorySheet) {
        AeonCategorySelection(
            categories = categories,
            selectedCategoryIds = selectedCategoryIds,
            onSelectionChange = onCategorySelectionChange,
            onDismiss = onDismissCategorySheet,
            mode = AeonSelectMode.MULTI,
            title = "Categories"
        )
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
