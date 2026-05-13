package com.osscabinet.ossWallet.ui.screens.transactions.entry.components

import androidx.compose.runtime.Composable
import com.osscabinet.ossWallet.ui.components.entry.AeonImpactHeaderCard
import com.osscabinet.ossWallet.ui.screens.transactions.entry.TransactionEntryEvent
import com.osscabinet.ossWallet.ui.screens.transactions.entry.TransactionEntryState

/**
 * Header card containing transaction type selector and amount input.
 * Uses the standardized AeonImpactHeaderCard.
 */
@Composable
fun TransactionHeaderCard(
    state: TransactionEntryState,
    onEvent: (TransactionEntryEvent) -> Unit
) {
    AeonImpactHeaderCard(
        transactionType = state.transactionType,
        onTypeSelected = { onEvent(TransactionEntryEvent.SetTransactionType(it)) },
        amount = state.amount,
        currency = state.selectedCurrency,
        onCurrencyClick = { onEvent(TransactionEntryEvent.ShowNumpad(true)) }, // Usually opens numpad or currency selector
        onAmountClick = { onEvent(TransactionEntryEvent.ShowNumpad(true)) },
        name = state.name,
        onNameChange = { onEvent(TransactionEntryEvent.SetName(it)) },
        nameLabel = "TRANSACTION NAME",
        isTransfer = state.isTransfer,
        targetAccountId = state.targetAccount?.id,
        targetCurrency = state.targetCurrency,
        onTargetCurrencyClick = { onEvent(TransactionEntryEvent.ShowTargetCurrencySheet(true)) },
        transferRate = state.exchangeRate,
        onExchangeRateClick = { onEvent(TransactionEntryEvent.ShowExchangeRateDialog(true)) },
        isSplitMode = state.isSplitMode,
        onSplitClick = {
            if (!state.isSplitMode) {
                onEvent(TransactionEntryEvent.ToggleSplitMode(true))
            }
            onEvent(TransactionEntryEvent.ShowSplitSheet(true))
        },
        supportedCurrencies = state.selectedAccount?.allowedCurrencies ?: listOf("USD")
    )
}
