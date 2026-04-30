package com.osscabinet.ossWallet.ui.screens.transactions.entry.components

import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
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

    // Date Picker
    if (state.showDatePicker) {
        DatePickerSheet(
            initialMillis = state.selectedDateMillis,
            onDateSelected = { millis ->
                millis?.let { onEvent(TransactionEntryEvent.SetDate(it)) }
                onEvent(TransactionEntryEvent.ShowDatePicker(false))
            },
            onDismiss = { onEvent(TransactionEntryEvent.ShowDatePicker(false)) }
        )
    }

    // Category Selection Sheet
    if (state.showCategorySheet) {
        CategorySelectionSheet(
            categories = state.categories,
            selectedCategoryId = state.selectedCategoryId,
            onCategorySelected = { category, id ->
                onEvent(TransactionEntryEvent.SetCategory(category, id))
                onEvent(TransactionEntryEvent.ShowCategorySheet(false))
            },
            onDismiss = { onEvent(TransactionEntryEvent.ShowCategorySheet(false)) }
        )
    }

    // Account Selection Sheet
    if (state.showAccountSheet) {
        AccountSelectionSheet(
            accounts = state.accounts,
            selectedAccountId = state.selectedAccount?.id,
            onAccountSelected = { account ->
                onEvent(TransactionEntryEvent.SetAccount(account))
                onEvent(TransactionEntryEvent.ShowAccountSheet(false))
            },
            onDismiss = { onEvent(TransactionEntryEvent.ShowAccountSheet(false)) }
        )
    }

    // Target Account Selection Sheet (for transfers)
    if (state.showTargetAccountSheet) {
        AccountSelectionSheet(
            accounts = state.accounts.filter { it.id != state.selectedAccount?.id },
            selectedAccountId = state.targetAccount?.id,
            onAccountSelected = { account ->
                onEvent(TransactionEntryEvent.SetTargetAccount(account))
                onEvent(TransactionEntryEvent.ShowTargetAccountSheet(false))
            },
            onDismiss = { onEvent(TransactionEntryEvent.ShowTargetAccountSheet(false)) },
            title = "Select Target Account"
        )
    }

    // Template Selection Sheet
    if (state.showTemplateSheet) {
        TemplateSelectionSheet(
            templates = state.templates,
            onTemplateSelected = { template ->
                onEvent(TransactionEntryEvent.ApplyTemplate(template))
                onEvent(TransactionEntryEvent.ShowTemplateSheet(false))
            },
            onDismiss = { onEvent(TransactionEntryEvent.ShowTemplateSheet(false)) }
        )
    }

    // Linked Transaction Sheet
    if (state.showLinkedTransactionSheet) {
        LinkedTransactionSheet(
            transactions = state.transactions,
            currentTransactionId = state.transactionId,
            linkedIds = state.displayLinkedTxs.map { it.id }.toSet(),
            accounts = state.accounts,
            categories = state.categories,
            onLinkedTransactionsChanged = { ids ->
                onEvent(TransactionEntryEvent.SetLinkedTransactionIds(ids.toList()))
            },
            onDismiss = { onEvent(TransactionEntryEvent.ShowLinkedTransactionSheet(false)) }
        )
    }

    // Tag Selection Sheet
    if (state.showLabelSheet) {
        TagSelectionSheet(
            tags = state.allTags,
            selectedTags = state.selectedTags,
            onTagsSelected = { tags ->
                onEvent(TransactionEntryEvent.SetTags(tags))
                onEvent(TransactionEntryEvent.ShowLabelSheet(false))
            },
            onAddTag = { tag -> onEvent(TransactionEntryEvent.AddTag(tag)) },
            onDismiss = { onEvent(TransactionEntryEvent.ShowLabelSheet(false)) }
        )
    }

    // Payment Method Sheet
    if (state.showPaymentMethodSheet) {
        PaymentMethodSheet(
            selectedMethod = state.paymentMethod,
            onMethodSelected = { method ->
                onEvent(TransactionEntryEvent.SetPaymentMethod(method))
                onEvent(TransactionEntryEvent.ShowPaymentMethodSheet(false))
            },
            onDismiss = { onEvent(TransactionEntryEvent.ShowPaymentMethodSheet(false)) }
        )
    }

    // Status Sheet
    if (state.showStatusSheet) {
        StatusSheet(
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
        DatePickerSheet(
            initialMillis = state.warrantyExpirationMillis,
            onDateSelected = { millis ->
                millis?.let { onEvent(TransactionEntryEvent.SetWarrantyExpiration(it)) }
                onEvent(TransactionEntryEvent.ShowWarrantyDatePicker(false))
            },
            onDismiss = { onEvent(TransactionEntryEvent.ShowWarrantyDatePicker(false)) },
            title = "Warranty Expiration"
        )
    }

    // Location Sheet
    if (state.showLocationSheet) {
        LocationSheet(
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
            onAmountChanged = { amount ->
                onEvent(TransactionEntryEvent.SetAmount(amount))
            },
            onDismiss = { onEvent(TransactionEntryEvent.ShowNumpad(false)) }
        )
    }
}
