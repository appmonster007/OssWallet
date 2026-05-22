package com.osscabinet.ossWallet.ui.screens.filters.entry.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.runtime.Composable
import com.osscabinet.ossWallet.domain.model.TransactionType
import com.osscabinet.ossWallet.ui.components.AeonDatePickerDialog
import com.osscabinet.ossWallet.ui.components.AeonSelectMode
import com.osscabinet.ossWallet.ui.components.entry.AeonTagSelectionDialog
import com.osscabinet.ossWallet.ui.components.entry.*
import com.osscabinet.ossWallet.ui.screens.filters.entry.FilterEntryEvent
import com.osscabinet.ossWallet.ui.screens.filters.entry.FilterEntryState

@Composable
fun FilterEntrySheets(
    state: FilterEntryState,
    onSearch: (String) -> kotlinx.coroutines.flow.Flow<List<com.osscabinet.ossWallet.domain.model.Transaction>>,
    onEvent: (FilterEntryEvent) -> Unit
) {
    if (state.showTypeSheet) {
        AeonSelectionSheet(
            title = "Transaction Types",
            onDismiss = { onEvent(FilterEntryEvent.ShowTypeSheet(false)) },
            items = TransactionType.entries,
            selectedIds = state.selectedTransactionTypes,
            onSelectionChange = { ids ->
                if (ids.isNotEmpty()) onEvent(FilterEntryEvent.SetTransactionTypes(ids))
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

    if (state.showAccountSheet) {
        AeonAccountSelection(
            accounts = state.accounts,
            selectedAccountIds = state.selectedAccountIds,
            onSelectionChange = { ids -> 
                if (ids.isNotEmpty()) onEvent(FilterEntryEvent.SetAccountIds(ids)) 
            },
            onDismiss = { onEvent(FilterEntryEvent.ShowAccountSheet(false)) },
            mode = AeonSelectMode.MULTI,
            title = "Filter Accounts"
        )
    }

    if (state.showCategorySheet) {
        AeonCategorySelection(
            categories = state.categories,
            selectedCategoryIds = state.selectedCategoryIds,
            onSelectionChange = { ids -> 
                if (ids.isNotEmpty()) onEvent(FilterEntryEvent.SetCategoryIds(ids)) 
            },
            onDismiss = { onEvent(FilterEntryEvent.ShowCategorySheet(false)) },
            mode = AeonSelectMode.MULTI,
            title = "Filter Categories",
            startExpanded = false
        )
    }

    if (state.showPaymentMethodSheet) {
        AeonPaymentMethodSelection(
            selectedMethods = state.selectedPaymentMethods,
            onSelectionChange = { ids -> 
                if (ids.isNotEmpty()) onEvent(FilterEntryEvent.SetPaymentMethods(ids)) 
            },
            onDismiss = { onEvent(FilterEntryEvent.ShowPaymentMethodSheet(false)) },
            mode = AeonSelectMode.MULTI,
            title = "Filter Payment Methods"
        )
    }

    if (state.showStatusSheet) {
        AeonStatusSelection(
            selectedStatuses = state.selectedStatuses,
            onSelectionChange = { ids -> 
                if (ids.isNotEmpty()) onEvent(FilterEntryEvent.SetStatuses(ids)) 
            },
            onDismiss = { onEvent(FilterEntryEvent.ShowStatusSheet(false)) },
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
            onLinkedTransactionsChanged = { onEvent(FilterEntryEvent.SetLinkedTransactions(it)) },
            onDismiss = { onEvent(FilterEntryEvent.ShowLinkSheet(false)) }
        )
    }

    if (state.showTagSheet) {
        AeonTagSelectionDialog(
            tags = state.availableTags,
            selectedTags = state.selectedTags,
            onTagsSelected = {
                onEvent(FilterEntryEvent.SetTags(it))
            },
            onAddTag = { name, _ -> 
                onEvent(FilterEntryEvent.SetTags(state.selectedTags + name))
            },
            onDismiss = { onEvent(FilterEntryEvent.ShowTagSheet(false)) }
        )
    }

    if (state.showCurrencySheet) {
        AeonCurrencySelection(
            selectedCurrencies = state.selectedCurrencies,
            onSelectionChange = { ids ->
                onEvent(FilterEntryEvent.SetCurrencies(ids))
            },
            onDismiss = { onEvent(FilterEntryEvent.ShowCurrencySheet(false)) },
            mode = AeonSelectMode.MULTI,
            title = "Filter Currencies",
            currencies = state.shortlistedCurrencies.ifEmpty { null }
        )
    }

    if (state.showMinAmountSheet) {
        AeonCalculatorSheet(
            currentAmount = state.minAmount,
            onAmountChanged = { onEvent(FilterEntryEvent.SetMinAmount(it)) },
            onDismiss = { onEvent(FilterEntryEvent.ShowMinAmountSheet(false)) },
            title = "Minimum Amount"
        )
    }

    if (state.showMaxAmountSheet) {
        AeonCalculatorSheet(
            currentAmount = state.maxAmount,
            onAmountChanged = { onEvent(FilterEntryEvent.SetMaxAmount(it)) },
            onDismiss = { onEvent(FilterEntryEvent.ShowMaxAmountSheet(false)) },
            title = "Maximum Amount"
        )
    }

    if (state.showTimelineSheet) {
        AeonTimelineSheet(
            selectedRange = state.timelineRange,
            onRangeSelected = { onEvent(FilterEntryEvent.SetTimelineRange(it)) },
            onDismiss = { onEvent(FilterEntryEvent.ShowTimelineSheet(false)) }
        )
    }

    if (state.showWarrantyStartPicker) {
        AeonDatePickerDialog(
            selectedDateMillis = state.warrantyExpirationStart ?: System.currentTimeMillis(),
            onDateSelected = { 
                onEvent(FilterEntryEvent.SetWarrantyExpirationStart(it))
                onEvent(FilterEntryEvent.ShowWarrantyStartPicker(false))
            },
            onDismiss = { onEvent(FilterEntryEvent.ShowWarrantyStartPicker(false)) },
            label = "Warranty Start"
        )
    }

    if (state.showWarrantyEndPicker) {
        AeonDatePickerDialog(
            selectedDateMillis = state.warrantyExpirationEnd ?: System.currentTimeMillis(),
            onDateSelected = { 
                onEvent(FilterEntryEvent.SetWarrantyExpirationEnd(it))
                onEvent(FilterEntryEvent.ShowWarrantyEndPicker(false))
            },
            onDismiss = { onEvent(FilterEntryEvent.ShowWarrantyEndPicker(false)) },
            label = "Warranty End"
        )
    }
}
