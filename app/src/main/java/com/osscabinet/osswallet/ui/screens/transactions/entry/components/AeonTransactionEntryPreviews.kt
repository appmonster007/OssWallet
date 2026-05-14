package com.osscabinet.ossWallet.ui.screens.transactions.entry.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import com.osscabinet.ossWallet.domain.model.TransactionType
import com.osscabinet.ossWallet.ui.screens.transactions.entry.TransactionEntryState
import com.osscabinet.ossWallet.ui.theme.OssWalletTheme

@Preview(showBackground = true, heightDp = 1000)
@Composable
fun AeonTransactionFormBodyPreview() {
    OssWalletTheme {
        TransactionFormBody(
            state = TransactionEntryState(
                amount = "42.50",
                transactionType = TransactionType.EXPENSE,
                selectedCurrency = "USD",
                note = "Dinner with friends"
            ),
            onEvent = {}
        )
    }
}

@Preview(showBackground = true, heightDp = 1000)
@Composable
fun AeonTransactionFormBodyTransferPreview() {
    OssWalletTheme {
        TransactionFormBody(
            state = TransactionEntryState(
                amount = "500.00",
                transactionType = TransactionType.TRANSFER,
                selectedCurrency = "USD",
                targetCurrency = "EUR",
                note = "Monthly Savings Transfer"
            ),
            onEvent = {}
        )
    }
}
