package com.osscabinet.ossWallet.ui.screens.transactions.entry.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import com.osscabinet.ossWallet.ui.screens.transactions.entry.TransactionEntryState
import com.osscabinet.ossWallet.ui.theme.OssWalletTheme

@Preview(showBackground = true, heightDp = 1000)
@Composable
fun TransactionFormBodyPreview() {
    OssWalletTheme {
        TransactionFormBody(
            state = TransactionEntryState(
                amount = "42.50",
                transactionType = 0,
                selectedCurrency = "USD",
                note = "Dinner with friends"
            ),
            onEvent = {}
        )
    }
}

@Preview(showBackground = true, heightDp = 1000)
@Composable
fun TransactionFormBodyTransferPreview() {
    OssWalletTheme {
        TransactionFormBody(
            state = TransactionEntryState(
                amount = "500.00",
                transactionType = 2,
                selectedCurrency = "USD",
                targetCurrency = "EUR",
                note = "Monthly Savings Transfer"
            ),
            onEvent = {}
        )
    }
}
