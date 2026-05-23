package com.osscabinet.ossWallet.ui.screens.plannedpayments.entry.components

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.osscabinet.ossWallet.R
import com.osscabinet.ossWallet.domain.model.*
import com.osscabinet.ossWallet.ui.components.*
import com.osscabinet.ossWallet.ui.components.entry.AeonTagSelectionDialog
import com.osscabinet.ossWallet.ui.components.entry.*
import com.osscabinet.ossWallet.ui.screens.plannedpayments.entry.PlannedPaymentEntryEvent
import com.osscabinet.ossWallet.ui.screens.plannedpayments.entry.PlannedPaymentEntryState

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
                onEvent(PlannedPaymentEntryEvent.SetRecurrence(start, type, interval, dow, dom, cron))
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

    if (state.showCategorySheet) {
        AeonCategorySelection(
            categories = state.categories,
            selectedCategoryIds = setOf(state.selectedCategoryId),
            onSelectionChange = { ids ->
                ids.firstOrNull()?.let { onEvent(PlannedPaymentEntryEvent.SetCategoryId(it)) }
            },
            onDismiss = { onEvent(PlannedPaymentEntryEvent.ShowCategorySheet(false)) }
        )
    }

    if (state.showAccountSheet) {
        AeonAccountSelection(
            accounts = state.accounts,
            selectedAccountIds = state.selectedAccountId?.let { setOf(it) } ?: emptySet(),
            onSelectionChange = { ids -> onEvent(PlannedPaymentEntryEvent.SetAccountId(ids.firstOrNull())) },
            onDismiss = { onEvent(PlannedPaymentEntryEvent.ShowAccountSheet(false)) },
            allowOutsideWallet = true
        )
    }

    if (state.showTargetAccountSheet) {
        AeonAccountSelection(
            accounts = state.accounts,
            selectedAccountIds = state.targetAccountId?.let { setOf(it) } ?: emptySet(),
            onSelectionChange = { ids -> onEvent(PlannedPaymentEntryEvent.SetTargetAccountId(ids.firstOrNull())) },
            onDismiss = { onEvent(PlannedPaymentEntryEvent.ShowTargetAccountSheet(false)) },
            title = stringResource(R.string.target_account_selection_title),
            allowOutsideWallet = true
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
            currentAmount = state.amount,
            transactionType = state.transactionType,
            onAmountChanged = { amount ->
                onEvent(PlannedPaymentEntryEvent.SetAmountText(amount))
            },
            onTypeChanged = { newType ->
                onEvent(PlannedPaymentEntryEvent.SetTransactionType(newType))
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
            initialRate = state.transferRate,
            onConfirm = { rate ->
                onEvent(PlannedPaymentEntryEvent.SetTransferRate(rate))
                onEvent(PlannedPaymentEntryEvent.ShowExchangeRateDialog(false))
            },
            onDismiss = { onEvent(PlannedPaymentEntryEvent.ShowExchangeRateDialog(false)) }
        )
    }

    if (state.showCurrencySheet) {
        AeonCurrencySelection(
            selectedCurrencies = setOf(state.currency),
            onSelectionChange = { ids ->
                ids.firstOrNull()?.let { onEvent(PlannedPaymentEntryEvent.SetCurrency(it)) }
            },
            onDismiss = { onEvent(PlannedPaymentEntryEvent.ShowCurrencySheet(false)) },
            currencies = state.shortlistedCurrencies.ifEmpty { null }
        )
    }

    if (state.showTargetCurrencySheet) {
        val targetAccount = state.accounts.find { it.id == state.targetAccountId }
        AeonCurrencySelection(
            selectedCurrencies = setOf(state.targetCurrency ?: "USD"),
            onSelectionChange = { ids ->
                ids.firstOrNull()?.let { onEvent(PlannedPaymentEntryEvent.SetTargetCurrency(it)) }
            },
            onDismiss = { onEvent(PlannedPaymentEntryEvent.ShowTargetCurrencySheet(false)) },
            title = "Select Target Currency",
            currencies = if (targetAccount == null) null
                         else targetAccount.allowedCurrencies.toList().ifEmpty { null }
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
            onLocationSelected = { gpsStr ->
                onEvent(PlannedPaymentEntryEvent.SetLocation(gpsStr))
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
