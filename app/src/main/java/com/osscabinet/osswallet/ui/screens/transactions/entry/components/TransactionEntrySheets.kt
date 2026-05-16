package com.osscabinet.ossWallet.ui.screens.transactions.entry.components

import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import com.osscabinet.ossWallet.ui.components.AeonDateTimePicker
import com.osscabinet.ossWallet.ui.components.ExchangeRateDialog
import com.osscabinet.ossWallet.domain.model.TransactionType
import com.osscabinet.ossWallet.ui.components.entry.AeonCurrencySelection
import com.osscabinet.ossWallet.ui.components.entry.AeonTemplateSelection
import com.osscabinet.ossWallet.ui.components.entry.*
import com.osscabinet.ossWallet.ui.screens.transactions.entry.TransactionEntryEvent
import com.osscabinet.ossWallet.ui.screens.transactions.entry.TransactionEntryState

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
        AeonCategorySelection(
            categories = state.categories,
            selectedCategoryIds = setOf(state.selectedCategoryId),
            onSelectionChange = { ids ->
                ids.firstOrNull()?.let { id ->
                    onEvent(TransactionEntryEvent.SetCategory(id))
                }
            },
            onDismiss = { onEvent(TransactionEntryEvent.ShowCategorySheet(false)) }
        )
    }

    // Account Selection
    if (state.showAccountSheet) {
        AeonAccountSelection(
            accounts = state.accounts,
            selectedAccountIds = state.selectedAccount?.id?.let { setOf(it) } ?: emptySet(),
            onSelectionChange = { ids ->
                ids.firstOrNull()?.let { id ->
                    onEvent(TransactionEntryEvent.SetAccount(state.accounts.find { it.id == id }))
                }
            },
            onDismiss = { onEvent(TransactionEntryEvent.ShowAccountSheet(false)) }
        )
    }

    // Target Account Selection (for transfers)
    if (state.showTargetAccountSheet) {
        AeonAccountSelection(
            accounts = state.accounts.filter { it.id != state.selectedAccount?.id },
            selectedAccountIds = state.targetAccount?.id?.let { setOf(it) } ?: emptySet(),
            onSelectionChange = { ids ->
                ids.firstOrNull()?.let { id ->
                    onEvent(TransactionEntryEvent.SetTargetAccount(state.accounts.find { it.id == id }))
                }
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
        AeonLinkedTransactionSelection(
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
        AeonTagSelectionDialog(
            tags = state.allTags,
            selectedTags = state.selectedTags,
            onTagsSelected = { tags ->
                onEvent(TransactionEntryEvent.SetTags(tags))
            },
            onAddTag = { tag, color -> onEvent(TransactionEntryEvent.AddTag(tag, color)) },
            onDismiss = { onEvent(TransactionEntryEvent.ShowTagSheet(false)) }
        )
    }

    // Payment Method Selection
    if (state.showPaymentMethodSheet) {
        AeonPaymentMethodSelection(
            selectedMethods = setOf(state.paymentMethod),
            onSelectionChange = { methods ->
                methods.firstOrNull()?.let { method ->
                    onEvent(TransactionEntryEvent.SetPaymentMethod(method))
                }
            },
            onDismiss = { onEvent(TransactionEntryEvent.ShowPaymentMethodSheet(false)) }
        )
    }

    // Status Selection
    if (state.showStatusSheet) {
        AeonStatusSelection(
            selectedStatuses = setOf(state.status),
            onSelectionChange = { statuses ->
                statuses.firstOrNull()?.let { status ->
                    onEvent(TransactionEntryEvent.SetStatus(status))
                }
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
        AeonLocationSelection(
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
        AeonTransactionSplitSheet(
            totalAmount = state.amountValue,
            currency = state.selectedCurrency,
            splits = state.splits,
            categories = state.categories,
            onAddSplit = { onEvent(TransactionEntryEvent.AddSplit) },
            onRemoveSplit = { index -> onEvent(TransactionEntryEvent.RemoveSplit(index)) },
            onUpdateSplit = { index, amount, note, categoryId ->
                onEvent(TransactionEntryEvent.UpdateSplit(index, amount = amount, note = note, categoryId = categoryId))
            },
            onShowCategorySheet = { index -> onEvent(TransactionEntryEvent.ShowSplitCategorySheet(index)) },
            onToggleSplitMode = { onEvent(TransactionEntryEvent.ToggleSplitMode(it)) },
            onDismiss = { onEvent(TransactionEntryEvent.ShowSplitSheet(false)) }
        )
    }

    // Split Category Selection
    state.showSplitCategorySheet?.let { index ->
        AeonCategorySelection(
            categories = state.categories,
            selectedCategoryIds = setOf(state.splits.getOrNull(index)?.categoryId ?: state.selectedCategoryId),
            onSelectionChange = { ids ->
                ids.firstOrNull()?.let { id ->
                    onEvent(TransactionEntryEvent.UpdateSplit(index, categoryId = id))
                }
            },
            onDismiss = { onEvent(TransactionEntryEvent.ShowSplitCategorySheet(null)) }
        )
    }

    // Numpad (Calculator)
    if (state.isNumpadVisible) {
        AeonCalculatorSheet(
            currentAmount = state.amount,
            transactionType = state.transactionType,
            decimalPrecision = state.decimalPrecision,
            onAmountChanged = { amount ->
                onEvent(TransactionEntryEvent.SetAmount(amount))
            },
            onTypeChanged = { newType ->
                onEvent(TransactionEntryEvent.SetTransactionType(newType))
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
            selectedCurrencies = setOf(state.selectedCurrency),
            onSelectionChange = { ids ->
                ids.firstOrNull()?.let { onEvent(TransactionEntryEvent.SetCurrency(it)) }
            },
            onDismiss = { onEvent(TransactionEntryEvent.ShowCurrencySheet(false)) }
        )
    }

    if (state.showTargetCurrencySheet) {
        AeonCurrencySelection(
            selectedCurrencies = setOf(state.targetCurrency),
            onSelectionChange = { ids ->
                ids.firstOrNull()?.let { onEvent(TransactionEntryEvent.SetTargetCurrency(it)) }
            },
            onDismiss = { onEvent(TransactionEntryEvent.ShowTargetCurrencySheet(false)) },
            title = "Select Target Currency"
        )
    }
}
