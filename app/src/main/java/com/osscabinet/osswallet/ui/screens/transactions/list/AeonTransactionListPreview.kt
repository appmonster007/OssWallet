package com.osscabinet.ossWallet.ui.screens.transactions.list

import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import com.osscabinet.ossWallet.domain.model.Account
import com.osscabinet.ossWallet.domain.model.Transaction
import com.osscabinet.ossWallet.domain.model.TransactionFilter
import com.osscabinet.ossWallet.domain.model.TransactionWithBalance
import com.osscabinet.ossWallet.domain.model.Category
import com.osscabinet.ossWallet.ui.theme.OssWalletTheme
import com.osscabinet.ossWallet.ui.screens.analytics.AnalyticsUiState
import java.math.BigDecimal
import java.util.*

@Preview(showBackground = true, widthDp = 360, heightDp = 800, name = "Filtered Transactions")
@Composable
fun AeonTransactionListFilteredPreview() {
    val mockAccounts = listOf(
        Account("1", "Main Bank", "USD", "REGULAR", "#2196F3", "AccountBalance", listOf("USD")),
        Account("2", "Cash", "USD", "REGULAR", "#4CAF50", "AccountBalanceWallet", listOf("USD"))
    )
    
    val mockCategories = listOf(
        Category(1, "Food", "Restaurant", -12627531, true, null),
        Category(2, "Transport", "DirectionsCar", -14575885, true, null)
    )

    OssWalletTheme {
        TransactionListContent(
            listState = com.osscabinet.ossWallet.domain.model.TransactionListState(isEmpty = false),
            accounts = mockAccounts,
            categories = mockCategories,
            decimalPrecision = 2,
            analyticsState = AnalyticsUiState.Success(selectedCurrency = "USD"),
            filter = TransactionFilter(queryPayee = "Burger"),
            recordsSettings = com.osscabinet.ossWallet.data.local.TransactionSectionSettings(),
            onEditTransactionClick = {},
            onBackClick = {},
            onUpdateFilter = {},
            onUpdateTimelineRange = {},
            onUpdateRecordsSettings = {}
        )
    }
}
