package com.osscabinet.ossWallet.ui.screens.plannedpayments.entry.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.osscabinet.ossWallet.R
import com.osscabinet.ossWallet.domain.model.AmountEditingSide
import com.osscabinet.ossWallet.domain.model.Category
import com.osscabinet.ossWallet.domain.model.Transaction
import com.osscabinet.ossWallet.ui.components.AeonDatePickerDialog
import com.osscabinet.ossWallet.ui.components.ExchangeRateDialog
import com.osscabinet.ossWallet.domain.model.Account
import com.osscabinet.ossWallet.ui.components.entry.AeonAccountSelection
import com.osscabinet.ossWallet.ui.components.entry.AeonCalculatorSheet
import com.osscabinet.ossWallet.ui.components.entry.AeonCategorySelection
import com.osscabinet.ossWallet.ui.components.entry.AeonCurrencySelection
import com.osscabinet.ossWallet.ui.components.entry.AeonLinkedTransactionSelection
import com.osscabinet.ossWallet.ui.components.entry.AeonLocationSelection
import com.osscabinet.ossWallet.ui.components.entry.AeonPaymentMethodSelection
import com.osscabinet.ossWallet.ui.components.entry.AeonPlannedPaymentStatusSelection
import com.osscabinet.ossWallet.ui.components.entry.AeonScheduleBuilder
import com.osscabinet.ossWallet.ui.components.entry.AeonStatusSelection
import com.osscabinet.ossWallet.ui.components.entry.AeonTagSelectionDialog
import com.osscabinet.ossWallet.ui.components.entry.AeonTemplateSelection
import com.osscabinet.ossWallet.ui.screens.plannedpayments.entry.PlannedPaymentEntryEvent
import com.osscabinet.ossWallet.ui.screens.plannedpayments.entry.PlannedPaymentEntryState
import com.osscabinet.ossWallet.domain.service.currency.CurrencyService
import com.osscabinet.ossWallet.ui.components.AeonSelectMode

@Composable
fun PlannedPaymentEntrySheets(
    state: PlannedPaymentEntryState,
    onSearch: (String) -> kotlinx.coroutines.flow.Flow<List<Transaction>>,
    onEvent: (PlannedPaymentEntryEvent) -> Unit
) {
    if (state.showScheduleBuilder) {
        AeonScheduleBuilder(
            initialStartDate = state.startDateMillis,
            initialRecurrenceType = state.recurrenceType,
            initialInterval = state.recurrenceInterval,
            initialDayOfWeek = state.dayOfWeek,
            initialDayOfMonth = state.dayOfMonth,
            initialCronExpression = state.cronExpression,
            onConfirm = { start, type, interval, dow, dom, cron ->
                onEvent(
                    PlannedPaymentEntryEvent.SetRecurrence(
                        start,
                        type,
                        interval,
                        dow,
                        dom,
                        cron
                    )
                )
                onEvent(PlannedPaymentEntryEvent.ShowScheduleBuilder(false))
            },
            onDismiss = { onEvent(PlannedPaymentEntryEvent.ShowScheduleBuilder(false)) }
        )
    }

    if (state.showEndDatePicker) {
        AeonDatePickerDialog(
            selectedDateMillis = state.endDateMillis,
            onDateSelected = {
                onEvent(PlannedPaymentEntryEvent.SetEndDate(it))
                onEvent(PlannedPaymentEntryEvent.ShowEndDatePicker(false))
            },
            onDismiss = { onEvent(PlannedPaymentEntryEvent.ShowEndDatePicker(false)) }
        )
    }

    if (state.showCurrencySheet) {
        AeonCurrencySelection(
            selectedCurrencies = setOf(state.currency),
            onSelectionChange = { ids -> ids.firstOrNull()?.let { onEvent(PlannedPaymentEntryEvent.SetCurrency(it)) } },
            onDismiss = { onEvent(PlannedPaymentEntryEvent.ShowCurrencySheet(false)) },
            currencies = state.fullProviderCurrencies.ifEmpty { state.shortlistedCurrencies }
        )
    }

    if (state.showTargetCurrencySheet) {
        AeonCurrencySelection(
            selectedCurrencies = setOf(state.targetCurrency),
            onSelectionChange = { ids -> ids.firstOrNull()?.let { onEvent(PlannedPaymentEntryEvent.SetTargetCurrency(it)) } },
            onDismiss = { onEvent(PlannedPaymentEntryEvent.ShowTargetCurrencySheet(false)) },
            currencies = state.fullProviderCurrencies.ifEmpty { state.shortlistedCurrencies }
        )
    }

    if (state.showCategorySheet) {
        AeonCategorySelection(
            categories = state.categories.filter { !Category.isInitialBalance(it.id) },
            selectedCategoryIds = setOf(state.selectedCategoryId),
            onSelectionChange = { ids ->
                ids.firstOrNull()?.let { onEvent(PlannedPaymentEntryEvent.SetCategoryId(it)) }
            },
            onDismiss = { onEvent(PlannedPaymentEntryEvent.ShowCategorySheet(false)) }
        )
    }

    if (state.showAccountSheet) {
        val filteredAccounts = if (state.isTransfer) listOf(Account.OUTSIDE_WALLET) + state.accounts
                       else state.accounts

        AeonAccountSelection(
            accounts = filteredAccounts,
            selectedAccountIds = state.selectedAccountId?.let { setOf(it) } ?: emptySet(),
            onSelectionChange = { ids -> onEvent(PlannedPaymentEntryEvent.SetAccountId(ids.firstOrNull())) },
            onDismiss = { onEvent(PlannedPaymentEntryEvent.ShowAccountSheet(false)) }
        )
    }

    if (state.showTargetAccountSheet) {
        val filteredAccounts = listOf(Account.OUTSIDE_WALLET) + state.accounts

        AeonAccountSelection(
            accounts = filteredAccounts,
            selectedAccountIds = state.targetAccountId?.let { setOf(it) } ?: emptySet(),
            onSelectionChange = { ids -> onEvent(PlannedPaymentEntryEvent.SetTargetAccountId(ids.firstOrNull())) },
            onDismiss = { onEvent(PlannedPaymentEntryEvent.ShowTargetAccountSheet(false)) },
            title = stringResource(R.string.target_account_selection_title)
        )
    }

    if (state.showPaymentMethodSheet) {
        AeonPaymentMethodSelection(
            selectedMethods = setOf(state.paymentMethod),
            onSelectionChange = { methods ->
                methods.firstOrNull()?.let { method ->
                    onEvent(PlannedPaymentEntryEvent.SetPaymentMethod(method))
                }
            },
            onDismiss = { onEvent(PlannedPaymentEntryEvent.ShowPaymentMethodSheet(false)) }
        )
    }

    if (state.showTagSheet) {
        AeonTagSelectionDialog(
            tags = state.availableTags,
            selectedTags = state.selectedTagNames,
            onTagsSelected = { tags ->
                onEvent(PlannedPaymentEntryEvent.SetTags(tags))
            },
            onAddTag = { _, _ -> },
            onDismiss = { onEvent(PlannedPaymentEntryEvent.ShowTagSheet(false)) }
        )
    }

    if (state.showStatusSheet) {
        AeonPlannedPaymentStatusSelection(
            selectedStatus = state.status,
            onStatusSelected = { status ->
                onEvent(PlannedPaymentEntryEvent.SetStatus(status))
                onEvent(PlannedPaymentEntryEvent.ShowStatusSheet(false))
            },
            onDismiss = { onEvent(PlannedPaymentEntryEvent.ShowStatusSheet(false)) }
        )
    }

    if (state.showTransactionStatusSheet) {
        AeonStatusSelection(
            selectedStatuses = setOf(state.transactionStatus),
            onSelectionChange = { statuses ->
                statuses.firstOrNull()?.let { status ->
                    onEvent(PlannedPaymentEntryEvent.SetTransactionStatus(status))
                }
            },
            onDismiss = { onEvent(PlannedPaymentEntryEvent.ShowTransactionStatusSheet(false)) }
        )
    }

    if (state.showNumpad) {
        AeonCalculatorSheet(
            currentAmount = if (state.activeAmountSide == AmountEditingSide.SOURCE) state.amount else state.targetAmount,
            onAmountChanged = { amount ->
                if (state.activeAmountSide == AmountEditingSide.SOURCE) {
                    onEvent(PlannedPaymentEntryEvent.SetAmount(amount))
                } else {
                    onEvent(PlannedPaymentEntryEvent.SetTargetAmount(amount))
                }
            },
            onDismiss = { onEvent(PlannedPaymentEntryEvent.ShowNumpad(false)) }
        )
    }

    if (state.showTemplateSheet) {
        AeonTemplateSelection(
            templates = state.templates,
            categories = state.categories,
            onTemplateSelected = { template ->
                onEvent(PlannedPaymentEntryEvent.SetLinkedTemplate(template))
                onEvent(PlannedPaymentEntryEvent.ShowTemplateSheet(false))
            },
            onDismiss = { onEvent(PlannedPaymentEntryEvent.ShowTemplateSheet(false)) }
        )
    }

    if (state.showExchangeRateDialog) {
        ExchangeRateDialog(
            initialRate = state.exchangeRate,
            onConfirm = { rate ->
                onEvent(PlannedPaymentEntryEvent.SetExchangeRate(rate))
                onEvent(PlannedPaymentEntryEvent.ShowExchangeRateDialog(false))
            },
            onDismiss = { onEvent(PlannedPaymentEntryEvent.ShowExchangeRateDialog(false)) }
        )
    }

    if (state.showWarrantyDatePicker) {
        AeonDatePickerDialog(
            selectedDateMillis = state.warrantyExpirationMillis,
            onDateSelected = { millis ->
                onEvent(PlannedPaymentEntryEvent.SetWarrantyExpiration(millis))
                onEvent(PlannedPaymentEntryEvent.ShowWarrantyDatePicker(false))
            },
            onDismiss = { onEvent(PlannedPaymentEntryEvent.ShowWarrantyDatePicker(false)) }
        )
    }

    if (state.showLocationSheet) {
        AeonLocationSelection(
            gpsLocation = state.gpsLocation,
            onLocationSelected = { gpsStr, locationName ->
                onEvent(PlannedPaymentEntryEvent.SetLocation(gpsStr, locationName))
                onEvent(PlannedPaymentEntryEvent.ShowLocationSheet(false))
            },
            onDismiss = { onEvent(PlannedPaymentEntryEvent.ShowLocationSheet(false)) }
        )
    }

    if (state.showLinkedTransactionSheet) {
        AeonLinkedTransactionSelection(
            onSearch = onSearch,
            currentTransactionId = null, // Planned payments don't have a transaction ID
            linkedIds = state.displayLinkedTxs.map { it.id }.toSet(),
            accounts = state.accounts,
            categories = state.categories,
            allTags = state.availableTags,
            onLinkedTransactionsChanged = { ids ->
                onEvent(PlannedPaymentEntryEvent.SetLinkedTransactionIds(ids.toList()))
            },
            onDismiss = { onEvent(PlannedPaymentEntryEvent.ShowLinkedTransactionSheet(false)) }
        )
    }
}
