package com.osscabinet.ossWallet.ui.screens.loans.entry.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.runtime.Composable
import com.osscabinet.ossWallet.domain.model.TransactionType
import com.osscabinet.ossWallet.ui.components.AeonDatePickerDialog
import com.osscabinet.ossWallet.ui.components.AeonSelectMode
import com.osscabinet.ossWallet.ui.components.entry.*
import com.osscabinet.ossWallet.ui.screens.loans.entry.LoanEntryEvent
import com.osscabinet.ossWallet.ui.screens.loans.entry.LoanEntryState

@Composable
fun LoanEntrySheets(
    state: LoanEntryState,
    onEvent: (LoanEntryEvent) -> Unit
) {
    if (state.showStartDatePicker) {
        AeonDatePickerDialog(
            selectedDateMillis = state.startDate,
            onDateSelected = { 
                onEvent(LoanEntryEvent.SetStartDate(it))
                onEvent(LoanEntryEvent.ShowStartDatePicker(false))
            },
            onDismiss = { onEvent(LoanEntryEvent.ShowStartDatePicker(false)) }
        )
    }

    if (state.showSettlementDatePicker) {
        AeonDatePickerDialog(
            selectedDateMillis = state.settlementDate,
            onDateSelected = { 
                onEvent(LoanEntryEvent.SetSettlementDate(it))
                onEvent(LoanEntryEvent.ShowSettlementDatePicker(false))
            },
            onDismiss = { onEvent(LoanEntryEvent.ShowSettlementDatePicker(false)) }
        )
    }

    if (state.showPaymentMethodSheet) {
        AeonPaymentMethodSelection(
            selectedMethods = state.selectedPaymentMethods,
            onSelectionChange = { ids ->
                if (ids.isNotEmpty()) onEvent(LoanEntryEvent.SetPaymentMethods(ids))
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
                if (ids.isNotEmpty()) onEvent(LoanEntryEvent.SetStatuses(ids))
            },
            onDismiss = { onEvent(LoanEntryEvent.ShowStatusSheet(false)) },
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
            onLinkedTransactionsChanged = { onEvent(LoanEntryEvent.SetLinkedTransactions(it)) },
            onDismiss = { onEvent(LoanEntryEvent.ShowLinkSheet(false)) }
        )
    }

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

    if (state.showAccountSheet) {
        AeonAccountSelection(
            accounts = state.accounts,
            selectedAccountIds = if (state.linkedAccountId.isEmpty()) emptySet() else setOf(state.linkedAccountId),
            onSelectionChange = { ids ->
                ids.firstOrNull()?.let { onEvent(LoanEntryEvent.SetLinkedAccountId(it)) }
            },
            onDismiss = { onEvent(LoanEntryEvent.ShowAccountSheet(false)) },
            mode = AeonSelectMode.SINGLE,
            title = "Loan Account"
        )
    }

    if (state.showTrackTypeSheet) {
        AeonSelectionSheet(
            title = "Track Transaction Types",
            onDismiss = { onEvent(LoanEntryEvent.ShowTrackTypeSheet(false)) },
            items = TransactionType.entries.filter { it != TransactionType.TRANSFER },
            selectedIds = state.selectedTrackTypes,
            onSelectionChange = { ids ->
                if (ids.isNotEmpty()) onEvent(LoanEntryEvent.SetTrackTypes(ids))
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
                if (ids.isNotEmpty()) onEvent(LoanEntryEvent.SetCategoryIds(ids))
            },
            onDismiss = { onEvent(LoanEntryEvent.ShowCategorySheet(false)) },
            mode = AeonSelectMode.MULTI,
            title = "Loan Categories",
            startExpanded = false
        )
    }

    if (state.showTagSheet) {
        AeonTagSelection(
            tags = state.availableTags.map { com.osscabinet.ossWallet.domain.model.Tag(name = it, colorHex = -1) },
            selectedTags = state.selectedTags,
            onTagsSelected = {
                onEvent(LoanEntryEvent.SetTags(it))
                onEvent(LoanEntryEvent.ShowTagSheet(false))
            },
            onAddTag = { name, _ -> 
                onEvent(LoanEntryEvent.SetTags(state.selectedTags + name))
            },
            onEditTag = {},
            onDeleteTag = {},
            onDismiss = { onEvent(LoanEntryEvent.ShowTagSheet(false)) }
        )
    }

    if (state.showCurrencyFilterSheet) {
        AeonCurrencySelection(
            selectedCurrencies = state.selectedCurrencies,
            onSelectionChange = { ids ->
                if (ids.isNotEmpty()) onEvent(LoanEntryEvent.SetCurrencies(ids))
            },
            onDismiss = { onEvent(LoanEntryEvent.ShowCurrencyFilterSheet(false)) },
            mode = AeonSelectMode.MULTI,
            title = "Filter Currencies"
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
}
