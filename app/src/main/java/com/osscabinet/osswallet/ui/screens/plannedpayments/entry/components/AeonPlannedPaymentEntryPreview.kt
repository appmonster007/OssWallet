package com.osscabinet.ossWallet.ui.screens.plannedpayments.entry.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import com.osscabinet.ossWallet.domain.model.TransactionType
import com.osscabinet.ossWallet.ui.screens.plannedpayments.entry.PlannedPaymentEntryState
import com.osscabinet.ossWallet.ui.theme.OssWalletTheme

@Preview(showBackground = true, heightDp = 1000)
@Composable
fun AeonPlannedPaymentFormBodyPreview() {
    OssWalletTheme {
        PlannedPaymentFormBody(
            state = PlannedPaymentEntryState(
                name = "Netflix Subscription",
                amount = "15.99",
                transactionType = TransactionType.EXPENSE,
                currency = "USD"
            ),
            onEvent = {}
        )
    }
}
