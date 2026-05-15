package com.osscabinet.ossWallet.ui.screens.filters.entry.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.runtime.Composable
import com.osscabinet.ossWallet.domain.model.TransactionType
import com.osscabinet.ossWallet.ui.components.AeonDatePickerDialog
import com.osscabinet.ossWallet.ui.components.AeonSelectMode
import com.osscabinet.ossWallet.ui.components.entry.*
import com.osscabinet.ossWallet.ui.screens.filters.entry.FilterEntryEvent
import com.osscabinet.ossWallet.ui.screens.filters.entry.FilterEntryState

@Composable
fun FilterEntrySheets(
    state: FilterEntryState,
    onEvent: (FilterEntryEvent) -> Unit
) {
    if (state.showTypeSheet) {
        AeonSelectionSheet(
            title = "Transaction Types",
            onDismiss = { onEvent(FilterEntryEvent.ShowTypeSheet(false)) },
            items = TransactionType.entries.filter { it != TransactionType.TRANSFER },
            selectedIds = state.selectedTransactionTypes,
            onSelectionChange = { ids ->
                onEvent(FilterEntryEvent.SetTransactionTypes(ids))
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
            onSelectionChange = { ids -> onEvent(FilterEntryEvent.SetAccountIds(ids)) },
            onDismiss = { onEvent(FilterEntryEvent.ShowAccountSheet(false)) },
            mode = AeonSelectMode.MULTI,
            title = "Filter Accounts"
        )
    }

    if (state.showCategorySheet) {
        AeonCategorySelection(
            categories = state.categories,
            selectedCategoryIds = state.selectedCategoryIds,
            onSelectionChange = { ids -> onEvent(FilterEntryEvent.SetCategoryIds(ids)) },
            onDismiss = { onEvent(FilterEntryEvent.ShowCategorySheet(false)) },
            mode = AeonSelectMode.MULTI,
            title = "Filter Categories",
            startExpanded = false
        )
    }

    if (state.showPaymentMethodSheet) {
        AeonPaymentMethodSelection(
            selectedMethods = state.selectedPaymentMethods,
            onSelectionChange = { onEvent(FilterEntryEvent.SetPaymentMethods(it)) },
            onDismiss = { onEvent(FilterEntryEvent.ShowPaymentMethodSheet(false)) },
            mode = AeonSelectMode.MULTI,
            title = "Filter Payment Methods"
        )
    }

    if (state.showStatusSheet) {
        AeonStatusSelection(
            selectedStatuses = state.selectedStatuses,
            onSelectionChange = { onEvent(FilterEntryEvent.SetStatuses(it)) },
            onDismiss = { onEvent(FilterEntryEvent.ShowStatusSheet(false)) },
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
            onLinkedTransactionsChanged = { onEvent(FilterEntryEvent.SetLinkedTransactions(it)) },
            onDismiss = { onEvent(FilterEntryEvent.ShowLinkSheet(false)) }
        )
    }

    if (state.showTagSheet) {
        AeonTagSelection(
            tags = state.availableTags,
            selectedTags = state.selectedTags,
            onTagsSelected = {
                onEvent(FilterEntryEvent.SetTags(it))
                onEvent(FilterEntryEvent.ShowTagSheet(false))
            },
            onAddTag = { name, _ -> 
                onEvent(FilterEntryEvent.SetTags(state.selectedTags + name))
            },
            onEditTag = {},
            onDeleteTag = {},
            onDismiss = { onEvent(FilterEntryEvent.ShowTagSheet(false)) }
        )
    }

    if (state.showCurrencySheet) {
        val allCurrencies = listOf("USD", "EUR", "GBP", "JPY", "INR", "CAD", "AUD", "CHF", "CNY")
        AeonSelectionSheet(
            title = "Filter Currencies",
            onDismiss = { onEvent(FilterEntryEvent.ShowCurrencySheet(false)) },
            items = allCurrencies,
            selectedIds = state.selectedCurrencies,
            onSelectionChange = { ids -> onEvent(FilterEntryEvent.SetCurrencies(ids)) },
            idMapper = { it },
            labelMapper = { it },
            mode = AeonSelectMode.MULTI,
            iconMapper = { Icons.Rounded.AttachMoney }
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
