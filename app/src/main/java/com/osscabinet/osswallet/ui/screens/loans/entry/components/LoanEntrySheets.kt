package com.osscabinet.ossWallet.ui.screens.loans.entry.components

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
import com.osscabinet.ossWallet.ui.screens.loans.entry.LoanEntryEvent
import com.osscabinet.ossWallet.ui.screens.loans.entry.LoanEntryState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LoanEntrySheets(
    state: LoanEntryState,
    onSearch: (String) -> kotlinx.coroutines.flow.Flow<List<com.osscabinet.ossWallet.domain.model.Transaction>>,
    onEvent: (LoanEntryEvent) -> Unit
) {
    if (state.showNumpad) {
        AeonCalculatorSheet(
            currentAmount = state.principalAmount,
            onAmountChanged = { onEvent(LoanEntryEvent.SetPrincipalAmount(it)) },
            onDismiss = { onEvent(LoanEntryEvent.ShowNumpad(false)) },
            title = "Principal Amount"
        )
    }

    if (state.showInterestNumpad) {
        AeonCalculatorSheet(
            currentAmount = state.interestRate,
            onAmountChanged = { onEvent(LoanEntryEvent.SetInterestRate(it)) },
            onDismiss = { onEvent(LoanEntryEvent.ShowInterestNumpad(false)) },
            title = "Interest Rate (%)"
        )
    }

    if (state.showTimelineSheet) {
        AeonTimelineSheet(
            selectedRange = state.timelineRange,
            onRangeSelected = { onEvent(LoanEntryEvent.SetTimelineRange(it)) },
            onDismiss = { onEvent(LoanEntryEvent.ShowTimelineSheet(false)) }
        )
    }

    if (state.showStartDatePicker) {
        AeonDatePickerDialog(
            selectedDateMillis = state.startDate,
            onDateSelected = {
                onEvent(LoanEntryEvent.SetStartDate(it))
                onEvent(LoanEntryEvent.ShowStartDatePicker(false))
            },
            onDismiss = { onEvent(LoanEntryEvent.ShowStartDatePicker(false)) },
            label = "Start Date"
        )
    }

    if (state.showSettlementDatePicker) {
        AeonDatePickerDialog(
            selectedDateMillis = state.settlementDate,
            onDateSelected = {
                onEvent(LoanEntryEvent.SetSettlementDate(it))
                onEvent(LoanEntryEvent.ShowSettlementDatePicker(false))
            },
            onDismiss = { onEvent(LoanEntryEvent.ShowSettlementDatePicker(false)) },
            label = "Settlement Date"
        )
    }

    if (state.showAccountSheet) {
        AeonAccountSelection(
            accounts = state.accounts,
            selectedAccountIds = if (state.linkedAccountId.isEmpty()) emptySet() else setOf(state.linkedAccountId),
            onSelectionChange = { ids ->
                ids.firstOrNull()?.let { onEvent(LoanEntryEvent.SetLinkedAccountId(it)) }
                onEvent(LoanEntryEvent.ShowAccountSheet(false))
            },
            onDismiss = { onEvent(LoanEntryEvent.ShowAccountSheet(false)) },
            mode = AeonSelectMode.SINGLE,
            title = "Select Account"
        )
    }

    if (state.showTrackTypeSheet) {
        AeonSelectionSheet(
            title = "Track Transaction Types",
            onDismiss = { onEvent(LoanEntryEvent.ShowTrackTypeSheet(false)) },
            items = TransactionType.entries,
            selectedIds = state.selectedTrackTypes,
            onSelectionChange = { ids ->
                onEvent(LoanEntryEvent.SetTrackTypes(ids))
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
                onEvent(LoanEntryEvent.SetCategoryIds(ids))
            },
            onDismiss = { onEvent(LoanEntryEvent.ShowCategorySheet(false)) },
            mode = AeonSelectMode.MULTI,
            title = "Filter Categories",
            startExpanded = false
        )
    }

    if (state.showTagSheet) {
        AeonTagSelectionDialog(
            tags = state.availableTags,
            selectedTags = state.selectedTags,
            onTagsSelected = {
                onEvent(LoanEntryEvent.SetTags(it))
            },
            onAddTag = { name, _ ->
                onEvent(LoanEntryEvent.SetTags(state.selectedTags + name))
            },
            onDismiss = { onEvent(LoanEntryEvent.ShowTagSheet(false)) }
        )
    }

    if (state.showCurrencyFilterSheet) {
        AeonCurrencySelection(
            selectedCurrencies = state.selectedCurrencies,
            onSelectionChange = { ids ->
                onEvent(LoanEntryEvent.SetCurrencies(ids))
            },
            onDismiss = { onEvent(LoanEntryEvent.ShowCurrencyFilterSheet(false)) },
            mode = AeonSelectMode.MULTI,
            title = "Filter Currencies",
            currencies = state.shortlistedCurrencies.ifEmpty { null }
        )
    }

    if (state.showFilterSheet) {
        AeonFilterSelection(
            filters = state.availableFilters,
            selectedFilterId = state.filterId,
            onFilterSelected = { onEvent(LoanEntryEvent.SetFilterId(it)) },
            onDismiss = { onEvent(LoanEntryEvent.ShowFilterSheet(false)) },
            title = "Select Filter",
            showCreateNew = true
        )
    }

    if (state.showPaymentMethodSheet) {
        AeonPaymentMethodSelection(
            selectedMethods = state.selectedPaymentMethods,
            onSelectionChange = { ids ->
                onEvent(LoanEntryEvent.SetPaymentMethods(ids))
            },
            onDismiss = { onEvent(LoanEntryEvent.ShowPaymentMethodSheet(false)) },
            mode = AeonSelectMode.MULTI,
            title = "Filter Payment Methods"
        )
    }

    if (state.showStatusSheet) {
        AeonStatusSelection(
            selectedStatuses = state.selectedStatuses,
            onSelectionChange = { ids ->
                onEvent(LoanEntryEvent.SetStatuses(ids))
            },
            onDismiss = { onEvent(LoanEntryEvent.ShowStatusSheet(false)) },
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
            onLinkedTransactionsChanged = { onEvent(LoanEntryEvent.SetLinkedTransactions(it)) },
            onDismiss = { onEvent(LoanEntryEvent.ShowLinkSheet(false)) }
        )
    }

    if (state.showMinAmountSheet) {
        AeonCalculatorSheet(
            currentAmount = state.minAmount,
            onAmountChanged = { onEvent(LoanEntryEvent.SetMinAmount(it)) },
            onDismiss = { onEvent(LoanEntryEvent.ShowMinAmountSheet(false)) },
            title = "Minimum Amount"
        )
    }

    if (state.showMaxAmountSheet) {
        AeonCalculatorSheet(
            currentAmount = state.maxAmount,
            onAmountChanged = { onEvent(LoanEntryEvent.SetMaxAmount(it)) },
            onDismiss = { onEvent(LoanEntryEvent.ShowMaxAmountSheet(false)) },
            title = "Maximum Amount"
        )
    }

    if (state.showWarrantyStartPicker) {
        AeonDatePickerDialog(
            selectedDateMillis = state.warrantyExpirationStart ?: System.currentTimeMillis(),
            onDateSelected = {
                onEvent(LoanEntryEvent.SetWarrantyExpirationStart(it))
                onEvent(LoanEntryEvent.ShowWarrantyStartPicker(false))
            },
            onDismiss = { onEvent(LoanEntryEvent.ShowWarrantyStartPicker(false)) },
            label = "Warranty Start"
        )
    }

    if (state.showWarrantyEndPicker) {
        AeonDatePickerDialog(
            selectedDateMillis = state.warrantyExpirationEnd ?: System.currentTimeMillis(),
            onDateSelected = {
                onEvent(LoanEntryEvent.SetWarrantyExpirationEnd(it))
                onEvent(LoanEntryEvent.ShowWarrantyEndPicker(false))
            },
            onDismiss = { onEvent(LoanEntryEvent.ShowWarrantyEndPicker(false)) },
            label = "Warranty End"
        )
    }
}
