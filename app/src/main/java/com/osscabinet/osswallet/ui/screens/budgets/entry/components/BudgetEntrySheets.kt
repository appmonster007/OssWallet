package com.osscabinet.ossWallet.ui.screens.budgets.entry.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.runtime.Composable
import com.osscabinet.ossWallet.domain.model.BudgetPeriod
import com.osscabinet.ossWallet.domain.model.TransactionType
import com.osscabinet.ossWallet.ui.components.AeonDatePickerDialog
import com.osscabinet.ossWallet.ui.components.AeonSelectMode
import com.osscabinet.ossWallet.ui.components.entry.*
import com.osscabinet.ossWallet.ui.screens.budgets.entry.BudgetEntryEvent
import com.osscabinet.ossWallet.ui.screens.budgets.entry.BudgetEntryState

@Composable
fun BudgetEntrySheets(
    state: BudgetEntryState,
    onEvent: (BudgetEntryEvent) -> Unit
) {
    if (state.showNumpad) {
        AeonCalculatorSheet(
            currentAmount = state.limitAmount,
            onAmountChanged = { onEvent(BudgetEntryEvent.SetLimitAmount(it)) },
            onDismiss = { onEvent(BudgetEntryEvent.ShowNumpad(false)) },
            title = "Budget Limit"
        )
    }

    if (state.showPeriodSheet) {
        AeonSelectionSheet(
            title = "Budget Period",
            onDismiss = { onEvent(BudgetEntryEvent.ShowPeriodSheet(false)) },
            items = BudgetPeriod.entries,
            selectedIds = setOf(state.period),
            onSelectionChange = { ids ->
                ids.firstOrNull()?.let { 
                    onEvent(BudgetEntryEvent.SetPeriod(it))
                    if (it == BudgetPeriod.CUSTOM) {
                        onEvent(BudgetEntryEvent.ShowCustomStartPicker(true))
                    }
                }
            },
            idMapper = { it },
            labelMapper = { it.name.lowercase().replaceFirstChar { it.uppercase() } },
            mode = AeonSelectMode.SINGLE,
            iconMapper = {
                when (it) {
                    BudgetPeriod.DAILY -> Icons.Rounded.Today
                    BudgetPeriod.WEEKLY -> Icons.Rounded.ViewWeek
                    BudgetPeriod.MONTHLY -> Icons.Rounded.CalendarMonth
                    BudgetPeriod.QUARTERLY -> Icons.Rounded.PieChart
                    BudgetPeriod.YEARLY -> Icons.Rounded.CalendarToday
                    BudgetPeriod.CUSTOM -> Icons.Rounded.EditCalendar
                }
            }
        )
    }

    if (state.showCustomStartPicker) {
        AeonDatePickerDialog(
            selectedDateMillis = state.customStartDate,
            onDateSelected = { 
                onEvent(BudgetEntryEvent.SetCustomStartDate(it))
                onEvent(BudgetEntryEvent.ShowCustomStartPicker(false))
                onEvent(BudgetEntryEvent.ShowCustomEndPicker(true))
            },
            onDismiss = { onEvent(BudgetEntryEvent.ShowCustomStartPicker(false)) },
            label = "Custom Start Date"
        )
    }

    if (state.showCustomEndPicker) {
        AeonDatePickerDialog(
            selectedDateMillis = state.customEndDate,
            onDateSelected = { 
                onEvent(BudgetEntryEvent.SetCustomEndDate(it))
                onEvent(BudgetEntryEvent.ShowCustomEndPicker(false))
            },
            onDismiss = { onEvent(BudgetEntryEvent.ShowCustomEndPicker(false)) },
            label = "Custom End Date"
        )
    }

    if (state.showAccountSheet) {
        AeonAccountSelection(
            accounts = state.accounts,
            selectedAccountIds = state.selectedAccountIds,
            onSelectionChange = { ids ->
                if (ids.isNotEmpty()) onEvent(BudgetEntryEvent.SetAccountIds(ids))
            },
            onDismiss = { onEvent(BudgetEntryEvent.ShowAccountSheet(false)) },
            mode = AeonSelectMode.MULTI,
            title = "Budget Accounts"
        )
    }

    if (state.showTrackTypeSheet) {
        AeonSelectionSheet(
            title = "Track Transaction Types",
            onDismiss = { onEvent(BudgetEntryEvent.ShowTrackTypeSheet(false)) },
            items = TransactionType.entries.filter { it != TransactionType.TRANSFER },
            selectedIds = state.selectedTrackTypes,
            onSelectionChange = { ids ->
                if (ids.isNotEmpty()) onEvent(BudgetEntryEvent.SetTrackTypes(ids))
            },
            idMapper = { it },
            labelMapper = { it.displayName },
            mode = AeonSelectMode.MULTI,
            iconMapper = {
                when (it) {
                    TransactionType.EXPENSE -> Icons.Rounded.Remove
                    TransactionType.INCOME -> Icons.Rounded.Add
                    TransactionType.TRANSFER -> Icons.Rounded.SwapHoriz
                }
            }
        )
    }

    if (state.showCategorySheet) {
        AeonCategorySelection(
            categories = state.categories,
            selectedCategoryIds = state.selectedCategoryIds,
            onSelectionChange = { ids ->
                if (ids.isNotEmpty()) onEvent(BudgetEntryEvent.SetCategoryIds(ids))
            },
            onDismiss = { onEvent(BudgetEntryEvent.ShowCategorySheet(false)) },
            mode = AeonSelectMode.MULTI,
            title = "Budget Categories",
            startExpanded = false
        )
    }

    if (state.showTagSheet) {
        AeonTagSelection(
            tags = state.availableTags,
            selectedTags = state.selectedTags,
            onTagsSelected = {
                onEvent(BudgetEntryEvent.SetTags(it))
                onEvent(BudgetEntryEvent.ShowTagSheet(false))
            },
            onAddTag = { name, _ -> 
                onEvent(BudgetEntryEvent.SetTags(state.selectedTags + name))
            },
            onEditTag = {},
            onDeleteTag = {},
            onDismiss = { onEvent(BudgetEntryEvent.ShowTagSheet(false)) }
        )
    }

    if (state.showCurrencyFilterSheet) {
        AeonCurrencySelection(
            selectedCurrencies = state.selectedCurrencies,
            onSelectionChange = { ids ->
                if (ids.isNotEmpty()) onEvent(BudgetEntryEvent.SetCurrencies(ids))
            },
            onDismiss = { onEvent(BudgetEntryEvent.ShowCurrencyFilterSheet(false)) },
            mode = AeonSelectMode.MULTI,
            title = "Filter Currencies"
        )
    }

    if (state.showFilterSheet) {
        AeonFilterSelection(
            filters = state.availableFilters,
            selectedFilterId = state.filterId,
            onFilterSelected = { onEvent(BudgetEntryEvent.SetFilterId(it)) },
            onDismiss = { onEvent(BudgetEntryEvent.ShowFilterSheet(false)) },
            title = "Select Filter",
            showCreateNew = true
        )
    }

    if (state.showPaymentMethodSheet) {
        AeonPaymentMethodSelection(
            selectedMethods = state.selectedPaymentMethods,
            onSelectionChange = { ids ->
                if (ids.isNotEmpty()) onEvent(BudgetEntryEvent.SetPaymentMethods(ids))
            },
            onDismiss = { onEvent(BudgetEntryEvent.ShowPaymentMethodSheet(false)) },
            mode = AeonSelectMode.MULTI,
            title = "Filter Payment Methods"
        )
    }

    if (state.showStatusSheet) {
        AeonStatusSelection(
            selectedStatuses = state.selectedStatuses,
            onSelectionChange = { ids ->
                if (ids.isNotEmpty()) onEvent(BudgetEntryEvent.SetStatuses(ids))
            },
            onDismiss = { onEvent(BudgetEntryEvent.ShowStatusSheet(false)) },
            mode = AeonSelectMode.MULTI,
            title = "Filter Statuses"
        )
    }

    if (state.showLinkSheet) {
        AeonLinkedTransactionSelection(
            transactions = state.allTransactions,
            currentTransactionId = null,
            linkedIds = state.linkedTransactionIds,
            accounts = state.accounts,
            categories = state.categories,
            onLinkedTransactionsChanged = { onEvent(BudgetEntryEvent.SetLinkedTransactions(it)) },
            onDismiss = { onEvent(BudgetEntryEvent.ShowLinkSheet(false)) }
        )
    }
}
