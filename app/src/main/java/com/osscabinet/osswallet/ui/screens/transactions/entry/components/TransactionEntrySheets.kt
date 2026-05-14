package com.osscabinet.ossWallet.ui.screens.transactions.entry.components

import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import com.osscabinet.ossWallet.ui.components.AeonDateTimePicker
import com.osscabinet.ossWallet.ui.components.ExchangeRateDialog
import com.osscabinet.ossWallet.ui.components.entry.AeonCurrencySelection
import com.osscabinet.ossWallet.ui.components.entry.AeonTemplateSelection
import com.osscabinet.ossWallet.ui.screens.transactions.entry.components.TransactionAccountSelection
import com.osscabinet.ossWallet.ui.screens.transactions.entry.components.TransactionCategorySelection
import com.osscabinet.ossWallet.ui.screens.transactions.entry.TransactionEntryEvent
import com.osscabinet.ossWallet.ui.screens.transactions.entry.TransactionEntryState
import com.osscabinet.ossWallet.ui.screens.transactions.entry.sheets.*

/**
 * Container for all bottom sheets used in transaction entry.
 * Uses shared components to ensure consistency.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TransactionEntrySheets(
    state: TransactionEntryState,
    onEvent: (TransactionEntryEvent) -> Unit
) {
    // Date/Time Picker
    if (state.showDatePicker) {
        AeonDateTimePicker(
            initialDateTime = state.selectedDateMillis,
            onDateTimeSelected = { millis ->
                onEvent(TransactionEntryEvent.SetDate(millis))
                onEvent(TransactionEntryEvent.ShowDatePicker(false))
            },
            onDismiss = { onEvent(TransactionEntryEvent.ShowDatePicker(false)) },
            is24Hour = state.use24HourFormat
        )
    }

    // Category Selection
    if (state.showCategorySheet) {
        TransactionCategorySelection(
            categories = state.categories,
            selectedCategoryId = state.selectedCategoryId,
            onCategorySelected = { _, id ->
                onEvent(TransactionEntryEvent.SetCategory(id))
                onEvent(TransactionEntryEvent.ShowCategorySheet(false))
            },
            onDismiss = { onEvent(TransactionEntryEvent.ShowCategorySheet(false)) }
        )
    }

    // Account Selection
    if (state.showAccountSheet) {
        TransactionAccountSelection(
            accounts = state.accounts,
            selectedAccountId = state.selectedAccount?.id,
            onAccountSelected = { account ->
                onEvent(TransactionEntryEvent.SetAccount(account))
                onEvent(TransactionEntryEvent.ShowAccountSheet(false))
            },
            onDismiss = { onEvent(TransactionEntryEvent.ShowAccountSheet(false)) }
        )
    }

    // Target Account Selection (for transfers)
    if (state.showTargetAccountSheet) {
        TransactionAccountSelection(
            accounts = state.accounts.filter { it.id != state.selectedAccount?.id },
            selectedAccountId = state.targetAccount?.id,
            onAccountSelected = { account ->
                onEvent(TransactionEntryEvent.SetTargetAccount(account))
                onEvent(TransactionEntryEvent.ShowTargetAccountSheet(false))
            },
            onDismiss = { onEvent(TransactionEntryEvent.ShowTargetAccountSheet(false)) },
            title = "Select Target Account",
            allowOutsideWallet = true
        )
    }

    // Template Selection
    if (state.showTemplateSheet) {
        AeonTemplateSelection(
            templates = state.templates,
            categories = state.categories,
            onTemplateSelected = { template ->
                if (template != null) {
                    onEvent(TransactionEntryEvent.ApplyTemplate(template))
                }
                onEvent(TransactionEntryEvent.ShowTemplateSheet(false))
            },
            onDismiss = { onEvent(TransactionEntryEvent.ShowTemplateSheet(false)) },
            showCreateNew = false
        )
    }

    // Linked Transaction Selection
    if (state.showLinkedTransactionSheet) {
        LinkedTransactionSelection(
            transactions = state.transactions,
            currentTransactionId = state.transactionId,
            linkedIds = state.displayLinkedTxs.map { it.id }.toSet(),
            accounts = state.accounts,
            categories = state.categories,
            is24Hour = state.use24HourFormat,
            onLinkedTransactionsChanged = { ids ->
                onEvent(TransactionEntryEvent.SetLinkedTransactionIds(ids.toList()))
            },
            onDismiss = { onEvent(TransactionEntryEvent.ShowLinkedTransactionSheet(false)) }
        )
    }

    // Tag Selection
    if (state.showTagSheet) {
        TagSelection(
            tags = state.allTags,
            selectedTags = state.selectedTags,
            onTagsSelected = { tags ->
                onEvent(TransactionEntryEvent.SetTags(tags))
                onEvent(TransactionEntryEvent.ShowTagSheet(false))
            },
            onAddTag = { tag, color -> onEvent(TransactionEntryEvent.AddTag(tag, color)) },
            onEditTag = { _ -> /* Support edit if needed */ },
            onDeleteTag = { tag -> onEvent(TransactionEntryEvent.DeleteTag(tag)) },
            onDismiss = { onEvent(TransactionEntryEvent.ShowTagSheet(false)) }
        )
    }

    // Payment Method Selection
    if (state.showPaymentMethodSheet) {
        PaymentMethodSelection(
            selectedMethod = state.paymentMethod,
            onMethodSelected = { method ->
                onEvent(TransactionEntryEvent.SetPaymentMethod(method))
                onEvent(TransactionEntryEvent.ShowPaymentMethodSheet(false))
            },
            onDismiss = { onEvent(TransactionEntryEvent.ShowPaymentMethodSheet(false)) }
        )
    }

    // Status Selection
    if (state.showStatusSheet) {
        StatusSelection(
            selectedStatus = state.status,
            onStatusSelected = { status ->
                onEvent(TransactionEntryEvent.SetStatus(status))
                onEvent(TransactionEntryEvent.ShowStatusSheet(false))
            },
            onDismiss = { onEvent(TransactionEntryEvent.ShowStatusSheet(false)) }
        )
    }

    // Warranty Date Picker
    if (state.showWarrantyDatePicker) {
        AeonDateTimePicker(
            initialDateTime = state.warrantyExpirationMillis,
            onDateTimeSelected = { millis ->
                onEvent(TransactionEntryEvent.SetWarrantyExpiration(millis))
                onEvent(TransactionEntryEvent.ShowWarrantyDatePicker(false))
            },
            onDismiss = { onEvent(TransactionEntryEvent.ShowWarrantyDatePicker(false)) },
            is24Hour = state.use24HourFormat
        )
    }

    // Location Selection
    if (state.showLocationSheet) {
        LocationSelection(
            currentCoords = state.locationCoords,
            onLocationSelected = { coords ->
                onEvent(TransactionEntryEvent.SetLocation(coords))
                onEvent(TransactionEntryEvent.ShowLocationSheet(false))
            },
            onDismiss = { onEvent(TransactionEntryEvent.ShowLocationSheet(false)) }
        )
    }

    // Split Transaction Screen
    if (state.showSplitSheet) {
        TransactionSplitSheet(
            state = state,
            onEvent = onEvent,
            onDismiss = { onEvent(TransactionEntryEvent.ShowSplitSheet(false)) }
        )
    }

    // Split Category Selection
    state.showSplitCategorySheet?.let { index ->
        TransactionCategorySelection(
            categories = state.categories,
            selectedCategoryId = state.splits.getOrNull(index)?.categoryId ?: state.selectedCategoryId,
            onCategorySelected = { _, id ->
                onEvent(TransactionEntryEvent.UpdateSplit(index, categoryId = id))
                onEvent(TransactionEntryEvent.ShowSplitCategorySheet(null))
            },
            onDismiss = { onEvent(TransactionEntryEvent.ShowSplitCategorySheet(null)) }
        )
    }

    // Numpad (Calculator)
    if (state.isNumpadVisible) {
        CalculatorSheet(
            currentAmount = state.amount,
            transactionType = state.transactionType,
            decimalPrecision = state.decimalPrecision,
            onAmountChanged = { amount ->
                onEvent(TransactionEntryEvent.SetAmount(amount))
            },
            onDismiss = { onEvent(TransactionEntryEvent.ShowNumpad(false)) }
        )
    }

    // Exchange Rate Dialog (for transfers)
    if (state.showExchangeRateDialog) {
        ExchangeRateDialog(
            initialRate = state.exchangeRate,
            onDismiss = { onEvent(TransactionEntryEvent.ShowExchangeRateDialog(false)) },
            onConfirm = { rate ->
                onEvent(TransactionEntryEvent.SetExchangeRate(rate))
                onEvent(TransactionEntryEvent.ShowExchangeRateDialog(false))
            }
        )
    }

    // Currency Selection
    if (state.showCurrencySheet) {
        AeonCurrencySelection(
            selectedCurrency = state.selectedCurrency,
            onCurrencySelected = { onEvent(TransactionEntryEvent.SetCurrency(it)) },
            onDismiss = { onEvent(TransactionEntryEvent.ShowCurrencySheet(false)) }
        )
    }

    if (state.showTargetCurrencySheet) {
        AeonCurrencySelection(
            selectedCurrency = state.targetCurrency,
            onCurrencySelected = { onEvent(TransactionEntryEvent.SetTargetCurrency(it)) },
            onDismiss = { onEvent(TransactionEntryEvent.ShowTargetCurrencySheet(false)) },
            title = "Select Target Currency"
        )
    }
}
