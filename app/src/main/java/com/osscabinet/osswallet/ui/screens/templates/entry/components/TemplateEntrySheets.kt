package com.osscabinet.ossWallet.ui.screens.templates.entry.components

import androidx.compose.runtime.Composable
import com.osscabinet.ossWallet.ui.components.AeonDatePickerDialog
import com.osscabinet.ossWallet.ui.components.ExchangeRateDialog
import com.osscabinet.ossWallet.ui.components.entry.AeonCurrencySelection
import com.osscabinet.ossWallet.ui.screens.templates.entry.TemplateEntryEvent
import com.osscabinet.ossWallet.ui.screens.templates.entry.TemplateEntryState
import com.osscabinet.ossWallet.ui.screens.transactions.entry.components.TransactionAccountSelection
import com.osscabinet.ossWallet.ui.screens.transactions.entry.components.TransactionCategorySelection
import com.osscabinet.ossWallet.ui.screens.transactions.entry.sheets.*

@Composable
fun TemplateEntrySheets(
    state: TemplateEntryState,
    onEvent: (TemplateEntryEvent) -> Unit
) {
    if (state.isNumpadVisible) {
        CalculatorSheet(
            currentAmount = state.amount,
            transactionType = state.transactionType,
            decimalPrecision = state.decimalPrecision,
            onAmountChanged = { onEvent(TemplateEntryEvent.SetAmount(it)) },
            onTypeChanged = { newType ->
                val type = when (newType) {
                    "income" -> 1
                    "transfer" -> 2
                    else -> 0
                }
                onEvent(TemplateEntryEvent.SetTransactionType(type))
            },
            onDismiss = { onEvent(TemplateEntryEvent.ShowNumpad(false)) }
        )
    }

    if (state.showAccountSheet) {
        TransactionAccountSelection(
            accounts = state.accounts,
            selectedAccountId = state.selectedAccount?.id,
            onAccountSelected = { onEvent(TemplateEntryEvent.SetAccount(it?.id)) },
            onDismiss = { onEvent(TemplateEntryEvent.ShowAccountSheet(false)) },
            title = "Select Account",
            allowOutsideWallet = true
        )
    }

    if (state.showTargetAccountSheet) {
        TransactionAccountSelection(
            accounts = state.accounts,
            selectedAccountId = state.selectedTargetAccount?.id,
            onAccountSelected = { onEvent(TemplateEntryEvent.SetTargetAccount(it?.id)) },
            onDismiss = { onEvent(TemplateEntryEvent.ShowTargetAccountSheet(false)) },
            title = "Select Target Account",
            allowOutsideWallet = true
        )
    }

    if (state.showCategorySheet) {
        TransactionCategorySelection(
            categories = state.categories,
            selectedCategoryId = state.categoryId,
            onCategorySelected = { _, id ->
                onEvent(TemplateEntryEvent.SetCategory(id))
                onEvent(TemplateEntryEvent.ShowCategorySheet(false))
            },
            onDismiss = { onEvent(TemplateEntryEvent.ShowCategorySheet(false)) }
        )
    }

    if (state.showPaymentMethodSheet) {
        PaymentMethodSelection(
            selectedMethod = state.paymentMethod,
            onMethodSelected = { 
                onEvent(TemplateEntryEvent.SetPaymentMethod(it))
                onEvent(TemplateEntryEvent.ShowPaymentMethodSheet(false))
            },
            onDismiss = { onEvent(TemplateEntryEvent.ShowPaymentMethodSheet(false)) }
        )
    }

    if (state.showStatusSheet) {
        StatusSelection(
            selectedStatus = state.transactionStatus,
            onStatusSelected = {
                onEvent(TemplateEntryEvent.SetTransactionStatus(it))
                onEvent(TemplateEntryEvent.ShowStatusSheet(false))
            },
            onDismiss = { onEvent(TemplateEntryEvent.ShowStatusSheet(false)) }
        )
    }

    if (state.showTagSheet) {
        TagSelection(
            tags = state.allTags,
            selectedTags = state.tags,
            onTagsSelected = { onEvent(TemplateEntryEvent.SetTags(it)) },
            onAddTag = { name, color -> onEvent(TemplateEntryEvent.AddTag(name, color)) },
            onEditTag = { onEvent(TemplateEntryEvent.UpdateTag(it)) },
            onDeleteTag = { onEvent(TemplateEntryEvent.DeleteTag(it)) },
            onDismiss = { onEvent(TemplateEntryEvent.ShowTagSheet(false)) }
        )
    }

    if (state.showCurrencySheet) {
        AeonCurrencySelection(
            selectedCurrency = state.currency,
            onCurrencySelected = { 
                onEvent(TemplateEntryEvent.SetCurrency(it))
                onEvent(TemplateEntryEvent.ShowCurrencySheet(false))
            },
            onDismiss = { onEvent(TemplateEntryEvent.ShowCurrencySheet(false)) }
        )
    }

    if (state.showTargetCurrencySheet) {
        AeonCurrencySelection(
            selectedCurrency = state.targetCurrency ?: "USD",
            onCurrencySelected = { 
                onEvent(TemplateEntryEvent.SetTargetCurrency(it))
                onEvent(TemplateEntryEvent.ShowTargetCurrencySheet(false))
            },
            onDismiss = { onEvent(TemplateEntryEvent.ShowTargetCurrencySheet(false)) },
            title = "Select Target Currency"
        )
    }

    if (state.showExchangeRateDialog) {
        ExchangeRateDialog(
            initialRate = state.transferRate,
            onConfirm = { 
                onEvent(TemplateEntryEvent.SetTransferRate(it))
                onEvent(TemplateEntryEvent.ShowExchangeRateDialog(false))
            },
            onDismiss = { onEvent(TemplateEntryEvent.ShowExchangeRateDialog(false)) }
        )
    }

    if (state.showWarrantyDatePicker) {
        AeonDatePickerDialog(
            selectedDateMillis = state.warrantyExpirationMillis,
            onDateSelected = { 
                onEvent(TemplateEntryEvent.SetWarrantyExpiration(it))
                onEvent(TemplateEntryEvent.ShowWarrantyDatePicker(false))
            },
            onDismiss = { onEvent(TemplateEntryEvent.ShowWarrantyDatePicker(false)) }
        )
    }

    if (state.showLocationSheet) {
        LocationSelection(
            currentCoords = if (state.latitude != null && state.longitude != null) {
                state.latitude to state.longitude
            } else null,
            onLocationSelected = { coords ->
                onEvent(TemplateEntryEvent.SetLocation("", coords?.first, coords?.second))
                onEvent(TemplateEntryEvent.ShowLocationSheet(false))
            },
            onDismiss = { onEvent(TemplateEntryEvent.ShowLocationSheet(false)) }
        )
    }

    if (state.showLinkedTransactionSheet) {
        LinkedTransactionSelection(
            transactions = state.allTransactions,
            currentTransactionId = null,
            linkedIds = state.displayLinkedTxs.map { it.id }.toSet(),
            accounts = state.accounts,
            categories = state.categories,
            is24Hour = state.use24HourFormat,
            onLinkedTransactionsChanged = { ids ->
                onEvent(TemplateEntryEvent.SetLinkedTransactionIds(ids.toList()))
            },
            onDismiss = { onEvent(TemplateEntryEvent.ShowLinkedTransactionSheet(false)) }
        )
    }
}
