package com.osscabinet.ossWallet.ui.screens.transactions.entry.components

import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import com.osscabinet.ossWallet.ui.components.AeonDateTimePicker
import com.osscabinet.ossWallet.ui.components.ExchangeRateDialog
import com.osscabinet.ossWallet.ui.screens.transactions.entry.components.TransactionAccountSelection
import com.osscabinet.ossWallet.ui.screens.transactions.entry.components.TransactionCategorySelection
import com.osscabinet.ossWallet.ui.screens.transactions.entry.TransactionEntryEvent
import com.osscabinet.ossWallet.ui.screens.transactions.entry.TransactionEntryState
import com.osscabinet.ossWallet.ui.screens.transactions.entry.sheets.*

/**
 * Container for all bottom sheets used in transaction entry.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TransactionEntrySheets(
    state: TransactionEntryState,
    onEvent: (TransactionEntryEvent) -> Unit
) {
    val scope = rememberCoroutineScope()

    // Date/Time Picker (AeonDateTimePicker with custom UI)
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

    // Category Selection Screen
    if (state.showCategorySheet) {
        TransactionCategorySelection(
            categories = state.categories,
            selectedCategoryId = state.selectedCategoryId,
            onCategorySelected = { category, id ->
                onEvent(TransactionEntryEvent.SetCategory(category, id))
                onEvent(TransactionEntryEvent.ShowCategorySheet(false))
            },
            onDismiss = { onEvent(TransactionEntryEvent.ShowCategorySheet(false)) }
        )
    }

    // Account Selection Screen
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

    // Target Account Selection Screen (for transfers)
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

    // Template Selection Screen
    if (state.showTemplateSheet) {
        TemplateSelection(
            templates = state.templates,
            onTemplateSelected = { template ->
                onEvent(TransactionEntryEvent.ApplyTemplate(template))
                onEvent(TransactionEntryEvent.ShowTemplateSheet(false))
            },
            onDismiss = { onEvent(TransactionEntryEvent.ShowTemplateSheet(false)) }
        )
    }

    // Linked Transaction Selection Screen
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

    // Tag Selection Screen
    if (state.showTagSheet) {
        TagSelection(
            tags = state.allTags,
            selectedTags = state.selectedTags,
            onTagsSelected = { tags ->
                onEvent(TransactionEntryEvent.SetTags(tags))
                onEvent(TransactionEntryEvent.ShowTagSheet(false))
            },
            onAddTag = { tag, color -> onEvent(TransactionEntryEvent.AddTag(tag, color)) },
            onEditTag = { tag -> /* We could support edit from here too if desired */ },
            onDeleteTag = { tag -> onEvent(TransactionEntryEvent.DeleteTag(tag)) },
            onDismiss = { onEvent(TransactionEntryEvent.ShowTagSheet(false)) }
        )
    }

    // Payment Method Selection Screen
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

    // Status Selection Screen
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

    // Warranty Date/Time Picker (AeonDateTimePicker with custom UI)
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

    // Location Selection Screen
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
}
