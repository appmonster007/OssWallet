package com.osscabinet.ossWallet.ui.screens.templates.entry.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import com.osscabinet.ossWallet.domain.model.TransactionType
import com.osscabinet.ossWallet.ui.screens.templates.entry.TemplateEntryState
import com.osscabinet.ossWallet.ui.theme.OssWalletTheme

@Preview(showBackground = true, heightDp = 1000)
@Composable
fun AeonTemplateFormBodyPreview() {
    OssWalletTheme {
        TemplateFormBody(
            state = TemplateEntryState(
                name = "Weekly Grocery",
                amount = "0.00",
                transactionType = TransactionType.EXPENSE,
                currency = "USD"
            ),
            onSearch = { kotlinx.coroutines.flow.flowOf(emptyList()) },
            onEvent = {}
        )
    }
}
