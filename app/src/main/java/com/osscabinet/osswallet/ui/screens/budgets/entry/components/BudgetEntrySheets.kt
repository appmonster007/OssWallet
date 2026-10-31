package com.osscabinet.ossWallet.ui.screens.budgets.entry.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Remove
import androidx.compose.material.icons.rounded.SwapHoriz
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import com.osscabinet.ossWallet.domain.model.TransactionType
import com.osscabinet.ossWallet.ui.components.AeonDatePickerDialog
import com.osscabinet.ossWallet.ui.components.AeonSelectMode
import com.osscabinet.ossWallet.ui.components.entry.AeonAccountSelection
import com.osscabinet.ossWallet.ui.components.entry.AeonCalculatorSheet
import com.osscabinet.ossWallet.ui.components.entry.AeonCategorySelection
import com.osscabinet.ossWallet.ui.components.entry.AeonCurrencySelection
import com.osscabinet.ossWallet.ui.components.entry.AeonFilterSelection
import com.osscabinet.ossWallet.ui.components.entry.AeonLinkedTransactionSelection
import com.osscabinet.ossWallet.ui.components.entry.AeonPaymentMethodSelection
import com.osscabinet.ossWallet.ui.components.entry.AeonSelectionSheet
import com.osscabinet.ossWallet.ui.components.entry.AeonStatusSelection
import com.osscabinet.ossWallet.ui.components.entry.AeonTagSelectionDialog
import com.osscabinet.ossWallet.ui.components.entry.AeonTimelineSheet
import com.osscabinet.ossWallet.ui.screens.budgets.entry.BudgetEntryEvent
import com.osscabinet.ossWallet.ui.screens.budgets.entry.BudgetEntryState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BudgetEntrySheets(
    state: BudgetEntryState,
    onSearch: (String) -> kotlinx.coroutines.flow.Flow<List<com.osscabinet.ossWallet.domain.model.Transaction>>,
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

    if (state.showBaseCurrencySheet) {
        AeonCurrencySelection(
            selectedCurrencies = setOf(state.currency),
            onSelectionChange = { ids ->
                ids.firstOrNull()?.let { onEvent(BudgetEntryEvent.SetCurrency(it)) }
            },
            onDismiss = { onEvent(BudgetEntryEvent.ShowBaseCurrencySheet(false)) },
            mode = AeonSelectMode.SINGLE,
            title = "Select Budget Base Currency",
            currencies = state.shortlistedCurrencies.ifEmpty { com.osscabinet.ossWallet.domain.service.currency.CurrencyService.getAllCurrencies() }
        )
    }

    if (state.showTimelineSheet) {
        AeonTimelineSheet(
            selectedRange = state.timelineRange,
            onRangeSelected = { onEvent(BudgetEntryEvent.SetTimelineRange(it)) },
            onDismiss = { onEvent(BudgetEntryEvent.ShowTimelineSheet(false)) }
        )
    }

    if (state.showScheduleBuilder) {
        val periodic = state.timelineRange as? com.osscabinet.ossWallet.domain.model.timeline.TimelineRange.Periodic
        com.osscabinet.ossWallet.ui.components.entry.AeonScheduleBuilder(
            initialStartDate = periodic?.startDate ?: System.currentTimeMillis(),
            initialRecurrenceType = periodic?.recurrenceRule?.type ?: com.osscabinet.ossWallet.domain.model.RecurrenceType.MONTHLY,
            initialInterval = periodic?.recurrenceRule?.interval ?: 1,
            initialDayOfWeek = periodic?.recurrenceRule?.dayOfWeek,
            initialDayOfMonth = periodic?.recurrenceRule?.dayOfMonth,
            initialCronExpression = periodic?.recurrenceRule?.cronExpression,
            onConfirm = { start, type, interval, dow, dom, cron ->
                val rule = com.osscabinet.ossWallet.domain.model.RecurrenceRule(type, interval, dow, dom, cron)
                onEvent(BudgetEntryEvent.SetTimelineRange(com.osscabinet.ossWallet.domain.model.timeline.TimelineRange.Periodic(rule, start)))
                onEvent(BudgetEntryEvent.ShowScheduleBuilder(false))
            },
            onDismiss = { onEvent(BudgetEntryEvent.ShowScheduleBuilder(false)) }
        )
    }

    if (state.showAccountSheet) {
        AeonAccountSelection(
            accounts = state.accounts,
            selectedAccountIds = state.selectedAccountIds,
            onSelectionChange = { ids ->
                onEvent(BudgetEntryEvent.SetAccountIds(ids))
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
            items = TransactionType.entries,
            selectedIds = state.selectedTrackTypes,
            onSelectionChange = { ids ->
                onEvent(BudgetEntryEvent.SetTrackTypes(ids))
            },
            idMapper = { it },
            labelMapper = { it.displayName },
            mode = AeonSelectMode.MULTI,
            iconMapper = {
                when (it) {
                    TransactionType.EXPENSE -> Icons.Rounded.Remove
                    TransactionType.INCOME -> Icons.Rounded.Add
                    TransactionType.TRANSFER_INFLOW, TransactionType.TRANSFER_OUTFLOW -> Icons.Rounded.SwapHoriz
                }
            }
        )
    }

    if (state.showCategorySheet) {
        AeonCategorySelection(
            categories = state.categories,
            selectedCategoryIds = state.selectedCategoryIds,
            onSelectionChange = { ids ->
                onEvent(BudgetEntryEvent.SetCategoryIds(ids))
            },
            onDismiss = { onEvent(BudgetEntryEvent.ShowCategorySheet(false)) },
            mode = AeonSelectMode.MULTI,
            title = "Budget Categories",
            startExpanded = false
        )
    }

    if (state.showTagSheet) {
        AeonTagSelectionDialog(
            tags = state.availableTags,
            selectedTags = state.selectedTags,
            onTagsSelected = {
                onEvent(BudgetEntryEvent.SetTags(it))
            },
            onAddTag = { name, _ ->
                onEvent(BudgetEntryEvent.SetTags(state.selectedTags + name))
            },
            onDismiss = { onEvent(BudgetEntryEvent.ShowTagSheet(false)) }
        )
    }

    if (state.showCurrencyFilterSheet) {
        AeonCurrencySelection(
            selectedCurrencies = state.selectedCurrencies,
            onSelectionChange = { ids ->
                onEvent(BudgetEntryEvent.SetCurrencies(ids))
            },
            onDismiss = { onEvent(BudgetEntryEvent.ShowCurrencyFilterSheet(false)) },
            mode = AeonSelectMode.MULTI,
            title = "Filter Currencies",
            currencies = state.shortlistedCurrencies.ifEmpty { null }
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
                onEvent(BudgetEntryEvent.SetPaymentMethods(ids))
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
                onEvent(BudgetEntryEvent.SetStatuses(ids))
            },
            onDismiss = { onEvent(BudgetEntryEvent.ShowStatusSheet(false)) },
            mode = AeonSelectMode.MULTI,
            title = "Filter Statuses"
        )
    }

    if (state.showLinkSheet) {
        AeonLinkedTransactionSelection(
            onSearch = onSearch,
            currentTransactionId = null,
            linkedIds = state.linkedTransactionIds,
            accounts = state.accounts,
            categories = state.categories,
            allTags = state.availableTags,
            onLinkedTransactionsChanged = { onEvent(BudgetEntryEvent.SetLinkedTransactions(it)) },
            onDismiss = { onEvent(BudgetEntryEvent.ShowLinkSheet(false)) }
        )
    }

    if (state.showMinAmountSheet) {
        AeonCalculatorSheet(
            currentAmount = state.minAmount,
            onAmountChanged = { onEvent(BudgetEntryEvent.SetMinAmount(it)) },
            onDismiss = { onEvent(BudgetEntryEvent.ShowMinAmountSheet(false)) },
            title = "Minimum Amount"
        )
    }

    if (state.showMaxAmountSheet) {
        AeonCalculatorSheet(
            currentAmount = state.maxAmount,
            onAmountChanged = { onEvent(BudgetEntryEvent.SetMaxAmount(it)) },
            onDismiss = { onEvent(BudgetEntryEvent.ShowMaxAmountSheet(false)) },
            title = "Maximum Amount"
        )
    }

    if (state.showWarrantyStartPicker) {
        AeonDatePickerDialog(
            selectedDateMillis = state.warrantyExpirationStart ?: System.currentTimeMillis(),
            onDateSelected = {
                onEvent(BudgetEntryEvent.SetWarrantyExpirationStart(it))
                onEvent(BudgetEntryEvent.ShowWarrantyStartPicker(false))
            },
            onDismiss = { onEvent(BudgetEntryEvent.ShowWarrantyStartPicker(false)) },
            label = "Warranty Start"
        )
    }

    if (state.showWarrantyEndPicker) {
        AeonDatePickerDialog(
            selectedDateMillis = state.warrantyExpirationEnd ?: System.currentTimeMillis(),
            onDateSelected = {
                onEvent(BudgetEntryEvent.SetWarrantyExpirationEnd(it))
                onEvent(BudgetEntryEvent.ShowWarrantyEndPicker(false))
            },
            onDismiss = { onEvent(BudgetEntryEvent.ShowWarrantyEndPicker(false)) },
            label = "Warranty End"
        )
    }
}
