package com.osscabinet.ossWallet.ui.screens.filters.entry.components

import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.osscabinet.ossWallet.domain.model.TransactionType
import com.osscabinet.ossWallet.ui.screens.filters.entry.FilterEntryState
import com.osscabinet.ossWallet.ui.theme.OssWalletTheme

@Preview(showBackground = true)
@Composable
fun AeonFilterEntryPreview() {
    OssWalletTheme {
        FilterFormBody(
            state = FilterEntryState(
                name = "High Expenses",
                selectedTransactionTypes = setOf(TransactionType.EXPENSE),
                minAmount = "100",
                isNoteRegex = true
            ),
            onEvent = {},
            modifier = Modifier.padding(16.dp)
        )
    }
}
