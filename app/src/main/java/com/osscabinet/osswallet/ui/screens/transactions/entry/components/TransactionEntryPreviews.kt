package com.osscabinet.ossWallet.ui.screens.transactions.entry.components

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.osscabinet.ossWallet.ui.screens.transactions.entry.TransactionEntryState
import com.osscabinet.ossWallet.ui.theme.OssWalletTheme

@Preview(showBackground = true)
@Composable
fun TransactionHeaderCardLongAmountPreview() {
    OssWalletTheme {
        Surface(modifier = Modifier.padding(16.dp), color = MaterialTheme.colorScheme.background) {
            TransactionHeaderCard(
                state = TransactionEntryState(
                    amount = "1,250,000,000.00",
                    transactionType = 0,
                    selectedCurrency = "USD"
                ),
                onEvent = {}
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
fun TransactionActionCardsSideBySidePreview() {
    OssWalletTheme {
        Surface(modifier = Modifier.padding(16.dp), color = MaterialTheme.colorScheme.background) {
            TransactionActionCards(
                state = TransactionEntryState(
                    isEditMode = false,
                    selectedTemplateName = "Monthly Rent"
                ),
                onEvent = {}
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
fun TransactionActionCardsSingleRowPreview() {
    OssWalletTheme {
        Surface(modifier = Modifier.padding(16.dp), color = MaterialTheme.colorScheme.background) {
            TransactionActionCards(
                state = TransactionEntryState(
                    isEditMode = true
                ),
                onEvent = {}
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
fun TransactionTagSectionPreview() {
    OssWalletTheme {
        Surface(modifier = Modifier.padding(16.dp), color = MaterialTheme.colorScheme.background) {
            TransactionTagSection(
                state = TransactionEntryState(
                    note = "Weekly grocery shopping at Whole Foods",
                    payee = "Whole Foods Market",
                    selectedTags = listOf("Food", "Essential")
                ),
                onEvent = {}
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
fun TransactionFormBodyPreview() {
    OssWalletTheme {
        TransactionFormBody(
            state = TransactionEntryState(
                amount = "42.50",
                transactionType = 0,
                selectedCategoryId = 1L,
                note = "Dinner with friends"
            ),
            onEvent = {}
        )
    }
}
