package com.osscabinet.ossWallet.ui.screens.templates.entry.components

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import com.osscabinet.ossWallet.domain.service.currency.CurrencyService
import com.osscabinet.ossWallet.ui.components.entry.AeonImpactHeaderCard
import com.osscabinet.ossWallet.ui.screens.templates.entry.TemplateEntryEvent
import com.osscabinet.ossWallet.ui.screens.templates.entry.TemplateEntryState

@Composable
fun TemplateHeaderCard(
    state: TemplateEntryState,
    onEvent: (TemplateEntryEvent) -> Unit
) {
    val allCurrencies = remember { CurrencyService.getAllCurrencies() }

    AeonImpactHeaderCard(
        transactionType = state.transactionType,
        onTypeSelected = { onEvent(TemplateEntryEvent.SetTransactionType(it)) },
        amount = state.amount,
        currency = state.currency,
        onCurrencyClick = { onEvent(TemplateEntryEvent.ShowCurrencySheet(true)) },
        onAmountClick = { onEvent(TemplateEntryEvent.ShowNumpad(true)) },
        name = state.name,
        onNameChange = { onEvent(TemplateEntryEvent.SetName(it)) },
        nameLabel = "TEMPLATE NAME",
        isTransfer = state.transactionType == 2,
        targetAccountId = state.targetAccountId,
        targetCurrency = state.targetCurrency,
        onTargetCurrencyClick = { onEvent(TemplateEntryEvent.ShowTargetCurrencySheet(true)) },
        transferRate = state.transferRate,
        onExchangeRateClick = { onEvent(TemplateEntryEvent.ShowExchangeRateDialog(true)) },
        supportedCurrencies = allCurrencies
    )
}
