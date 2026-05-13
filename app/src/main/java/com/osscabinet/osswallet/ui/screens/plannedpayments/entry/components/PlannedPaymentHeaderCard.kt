package com.osscabinet.ossWallet.ui.screens.plannedpayments.entry.components

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import com.osscabinet.ossWallet.domain.service.currency.CurrencyService
import com.osscabinet.ossWallet.ui.components.entry.AeonImpactHeaderCard
import com.osscabinet.ossWallet.ui.screens.plannedpayments.entry.PlannedPaymentEntryEvent
import com.osscabinet.ossWallet.ui.screens.plannedpayments.entry.PlannedPaymentEntryState

@Composable
fun PlannedPaymentHeaderCard(
    state: PlannedPaymentEntryState,
    onEvent: (PlannedPaymentEntryEvent) -> Unit
) {
    val allCurrencies = remember { CurrencyService.getAllCurrencies() }

    AeonImpactHeaderCard(
        transactionType = state.transactionType,
        onTypeSelected = { onEvent(PlannedPaymentEntryEvent.SetTransactionType(it)) },
        amount = state.amount,
        currency = state.currency,
        onCurrencyClick = { onEvent(PlannedPaymentEntryEvent.ShowCurrencySheet(true)) },
        onAmountClick = { onEvent(PlannedPaymentEntryEvent.ShowNumpad(true)) },
        name = state.name,
        onNameChange = { onEvent(PlannedPaymentEntryEvent.SetName(it)) },
        nameLabel = "PAYMENT NAME",
        isTransfer = state.transactionType == 2,
        targetAccountId = state.targetAccountId,
        targetCurrency = state.targetCurrency,
        onTargetCurrencyClick = { onEvent(PlannedPaymentEntryEvent.ShowTargetCurrencySheet(true)) },
        transferRate = state.transferRate,
        onExchangeRateClick = { onEvent(PlannedPaymentEntryEvent.ShowExchangeRateDialog(true)) },
        supportedCurrencies = allCurrencies
    )
}
