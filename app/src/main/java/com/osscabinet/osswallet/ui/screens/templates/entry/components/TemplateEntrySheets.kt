package com.osscabinet.ossWallet.ui.screens.templates.entry.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.osscabinet.ossWallet.R
import com.osscabinet.ossWallet.domain.model.TransactionType
import com.osscabinet.ossWallet.ui.components.AeonDatePickerDialog
import com.osscabinet.ossWallet.ui.components.ExchangeRateDialog
import com.osscabinet.ossWallet.ui.components.entry.AeonCurrencySelection
import com.osscabinet.ossWallet.ui.components.entry.AeonTagSelectionDialog
import com.osscabinet.ossWallet.ui.components.entry.*
import com.osscabinet.ossWallet.ui.screens.templates.entry.TemplateEntryEvent
import com.osscabinet.ossWallet.ui.screens.templates.entry.TemplateEntryState

@Composable
fun TemplateEntrySheets(
    state: TemplateEntryState,
    onSearch: (String) -> kotlinx.coroutines.flow.Flow<List<com.osscabinet.ossWallet.domain.model.Transaction>>,
    onEvent: (TemplateEntryEvent) -> Unit
) {
    if (state.isNumpadVisible) {
        AeonCalculatorSheet(
            currentAmount = state.amount,
            transactionType = state.transactionType,
            onAmountChanged = { onEvent(TemplateEntryEvent.SetAmount(it)) },
            onTypeChanged = { newType ->
                onEvent(TemplateEntryEvent.SetTransactionType(newType))
            },
            onDismiss = { onEvent(TemplateEntryEvent.ShowNumpad(false)) }
        )
    }

    if (state.showAccountSheet) {
        AeonAccountSelection(
            accounts = state.accounts,
            selectedAccountIds = state.selectedAccount?.id?.let { setOf(it) } ?: emptySet(),
            onSelectionChange = { ids -> onEvent(TemplateEntryEvent.SetAccount(ids.firstOrNull())) },
            onDismiss = { onEvent(TemplateEntryEvent.ShowAccountSheet(false)) },
            title = "Select Account",
            allowOutsideWallet = true
        )
    }

    if (state.showTargetAccountSheet) {
        AeonAccountSelection(
            accounts = state.accounts,
            selectedAccountIds = state.selectedTargetAccount?.id?.let { setOf(it) } ?: emptySet(),
            onSelectionChange = { ids -> onEvent(TemplateEntryEvent.SetTargetAccount(ids.firstOrNull())) },
            onDismiss = { onEvent(TemplateEntryEvent.ShowTargetAccountSheet(false)) },
            title = stringResource(R.string.target_account_selection_title),
            allowOutsideWallet = true
        )
    }

    if (state.showCategorySheet) {
        AeonCategorySelection(
            categories = state.categories,
            selectedCategoryIds = setOf(state.categoryId),
            onSelectionChange = { ids ->
                onEvent(TemplateEntryEvent.SetCategory(ids.firstOrNull() ?: -1L))
            },
            onDismiss = { onEvent(TemplateEntryEvent.ShowCategorySheet(false)) }
        )
    }

    if (state.showPaymentMethodSheet) {
        AeonPaymentMethodSelection(
            selectedMethods = setOf(state.paymentMethod),
            onSelectionChange = { methods ->
                methods.firstOrNull()?.let { method ->
                    onEvent(TemplateEntryEvent.SetPaymentMethod(method))
                }
            },
            onDismiss = { onEvent(TemplateEntryEvent.ShowPaymentMethodSheet(false)) }
        )
    }

    if (state.showStatusSheet) {
        AeonStatusSelection(
            selectedStatuses = setOf(state.transactionStatus),
            onSelectionChange = { statuses ->
                statuses.firstOrNull()?.let { status ->
                    onEvent(TemplateEntryEvent.SetTransactionStatus(status))
                }
            },
            onDismiss = { onEvent(TemplateEntryEvent.ShowStatusSheet(false)) }
        )
    }

    if (state.showTagSheet) {
        AeonTagSelectionDialog(
            tags = state.allTags,
            selectedTags = state.tags,
            onTagsSelected = { onEvent(TemplateEntryEvent.SetTags(it)) },
            onAddTag = { name, color -> onEvent(TemplateEntryEvent.AddTag(name, color)) },
            onDismiss = { onEvent(TemplateEntryEvent.ShowTagSheet(false)) }
        )
    }

    if (state.showCurrencySheet) {
        AeonCurrencySelection(
            selectedCurrencies = setOf(state.currency),
            onSelectionChange = { ids ->
                ids.firstOrNull()?.let { onEvent(TemplateEntryEvent.SetCurrency(it)) }
            },
            onDismiss = { onEvent(TemplateEntryEvent.ShowCurrencySheet(false)) },
            currencies = state.shortlistedCurrencies.ifEmpty { null }
        )
    }

    if (state.showTargetCurrencySheet) {
        AeonCurrencySelection(
            selectedCurrencies = setOf(state.targetCurrency ?: "USD"),
            onSelectionChange = { ids ->
                ids.firstOrNull()?.let { onEvent(TemplateEntryEvent.SetTargetCurrency(it)) }
            },
            onDismiss = { onEvent(TemplateEntryEvent.ShowTargetCurrencySheet(false)) },
            title = "Select Target Currency",
            currencies = state.selectedTargetAccount?.allowedCurrencies?.toList()?.ifEmpty { null }
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
        AeonLocationSelection(
            gpsLocation = state.gpsLocation,
            onLocationSelected = { gpsStr ->
                onEvent(TemplateEntryEvent.SetLocation(gpsStr))
                onEvent(TemplateEntryEvent.ShowLocationSheet(false))
            },
            onDismiss = { onEvent(TemplateEntryEvent.ShowLocationSheet(false)) }
        )
    }

    if (state.showLinkedTransactionSheet) {
        AeonLinkedTransactionSelection(
            onSearch = onSearch,
            currentTransactionId = null,
            linkedIds = state.displayLinkedTxs.map { it.id }.toSet(),
            accounts = state.accounts,
            categories = state.categories,
            allTags = state.allTags,
            onLinkedTransactionsChanged = { ids ->
                onEvent(TemplateEntryEvent.SetLinkedTransactionIds(ids.toList()))
            },
            onDismiss = { onEvent(TemplateEntryEvent.ShowLinkedTransactionSheet(false)) }
        )
    }
}
