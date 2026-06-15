package com.osscabinet.ossWallet.ui.screens.filters.entry.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Remove
import androidx.compose.material.icons.rounded.SwapHoriz
import androidx.compose.runtime.Composable
import com.osscabinet.ossWallet.domain.model.PaymentMethod
import com.osscabinet.ossWallet.domain.model.TransactionStatus
import com.osscabinet.ossWallet.domain.model.TransactionType
import com.osscabinet.ossWallet.ui.components.AeonDatePickerDialog
import com.osscabinet.ossWallet.ui.components.AeonSelectMode
import com.osscabinet.ossWallet.ui.components.entry.AeonAccountSelection
import com.osscabinet.ossWallet.ui.components.entry.AeonCalculatorSheet
import com.osscabinet.ossWallet.ui.components.entry.AeonCategorySelection
import com.osscabinet.ossWallet.ui.components.entry.AeonCurrencySelection
import com.osscabinet.ossWallet.ui.components.entry.AeonLinkedTransactionSelection
import com.osscabinet.ossWallet.ui.components.entry.AeonPaymentMethodSelection
import com.osscabinet.ossWallet.ui.components.entry.AeonSelectionSheet
import com.osscabinet.ossWallet.ui.components.entry.AeonStatusSelection
import com.osscabinet.ossWallet.ui.components.entry.AeonTagSelectionDialog
import com.osscabinet.ossWallet.ui.components.entry.AeonTimelineSheet
import com.osscabinet.ossWallet.ui.screens.filters.entry.FilterEntryEvent
import com.osscabinet.ossWallet.ui.screens.filters.entry.FilterEntryState

@Composable
fun FilterEntrySheets(
    state: FilterEntryState,
    onSearch: (String) -> kotlinx.coroutines.flow.Flow<List<com.osscabinet.ossWallet.domain.model.Transaction>>,
    onEvent: (FilterEntryEvent) -> Unit
) {
    if (state.showTypeSheet) {
        val allTypes = TransactionType.entries.toSet()
        AeonSelectionSheet(
            title = "Transaction Types",
            onDismiss = { onEvent(FilterEntryEvent.ShowTypeSheet(false)) },
            items = TransactionType.entries,
            // Empty selection means "all types" — resolve to full set so all appear checked
            selectedIds = state.selectedTransactionTypes.ifEmpty { allTypes },
            onSelectionChange = { ids ->
                // Collapse "all selected" or "nothing" back to empty (= no type filter)
                onEvent(FilterEntryEvent.SetTransactionTypes(if (ids == allTypes || ids.isEmpty()) emptySet() else ids))
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

    if (state.showAccountSheet) {
        // Show ALL accounts (active + archived). Empty selection resolves to active accounts only,
        // so archived appear unchecked by default — user can opt in by checking them.
        val activeAccountIds = state.accounts.filter { !it.isArchived }.map { it.id }.toSet()
        AeonAccountSelection(
            accounts = state.accounts,
            selectedAccountIds = state.selectedAccountIds.ifEmpty { activeAccountIds },
            onSelectionChange = { ids ->
                // Store empty to mean "all active accounts"; only persist an explicit set when the
                // user picks a non-default set (subset of active, or includes archived accounts).
                val effectiveIds = if (ids == activeAccountIds) emptySet() else ids
                onEvent(FilterEntryEvent.SetAccountIds(effectiveIds))
            },
            onDismiss = { onEvent(FilterEntryEvent.ShowAccountSheet(false)) },
            mode = AeonSelectMode.MULTI,
            title = "Filter Accounts"
        )
    }

    if (state.showCategorySheet) {
        val allCategoryIds = state.categories.map { it.id }.toSet()
        AeonCategorySelection(
            categories = state.categories,
            // Empty selection means "all categories" — resolve so all appear checked
            selectedCategoryIds = state.selectedCategoryIds.ifEmpty { allCategoryIds },
            onSelectionChange = { ids ->
                onEvent(FilterEntryEvent.SetCategoryIds(if (ids == allCategoryIds || ids.isEmpty()) emptySet() else ids))
            },
            onDismiss = { onEvent(FilterEntryEvent.ShowCategorySheet(false)) },
            mode = AeonSelectMode.MULTI,
            title = "Filter Categories",
            startExpanded = false
        )
    }

    if (state.showPaymentMethodSheet) {
        val allMethods = PaymentMethod.entries.toSet()
        AeonPaymentMethodSelection(
            // Empty selection means "any method" — resolve so all appear checked
            selectedMethods = state.selectedPaymentMethods.ifEmpty { allMethods },
            onSelectionChange = { ids ->
                onEvent(FilterEntryEvent.SetPaymentMethods(if (ids == allMethods || ids.isEmpty()) emptySet() else ids))
            },
            onDismiss = { onEvent(FilterEntryEvent.ShowPaymentMethodSheet(false)) },
            mode = AeonSelectMode.MULTI,
            title = "Filter Payment Methods"
        )
    }

    if (state.showStatusSheet) {
        val allStatuses = TransactionStatus.entries.toSet()
        AeonStatusSelection(
            // Empty selection means "all statuses" — resolve so all appear checked
            selectedStatuses = state.selectedStatuses.ifEmpty { allStatuses },
            onSelectionChange = { ids ->
                onEvent(FilterEntryEvent.SetStatuses(if (ids == allStatuses || ids.isEmpty()) emptySet() else ids))
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
        // The shortlisted currencies are the currencies relevant to the user's accounts/transactions.
        // When selection is empty (= all), pre-select the shortlist so the user sees what "all" means.
        val shortlisted = state.shortlistedCurrencies
        AeonCurrencySelection(
            selectedCurrencies = state.selectedCurrencies.ifEmpty { shortlisted.toSet() },
            onSelectionChange = { ids ->
                // Collapse back to empty when the user selects all shortlisted currencies (or nothing)
                val effectiveIds = if (ids == shortlisted.toSet() || ids.isEmpty()) emptySet() else ids
                onEvent(FilterEntryEvent.SetCurrencies(effectiveIds))
            },
            onDismiss = { onEvent(FilterEntryEvent.ShowCurrencySheet(false)) },
            mode = AeonSelectMode.MULTI,
            title = "Filter Currencies",
            currencies = shortlisted.ifEmpty { null }
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
