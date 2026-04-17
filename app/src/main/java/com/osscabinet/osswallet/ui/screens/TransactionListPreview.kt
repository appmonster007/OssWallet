package com.osscabinet.ossWallet.ui.screens

import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import com.osscabinet.ossWallet.domain.model.Account
import com.osscabinet.ossWallet.domain.model.Transaction
import com.osscabinet.ossWallet.domain.model.TransactionFilter
import com.osscabinet.ossWallet.domain.model.TransactionWithBalance
import com.osscabinet.ossWallet.data.local.entity.CategoryEntity
import com.osscabinet.ossWallet.ui.theme.OssWalletTheme
import com.osscabinet.ossWallet.ui.viewmodel.AnalyticsUiState
import java.math.BigDecimal
import java.util.*

@Preview(showBackground = true, widthDp = 360, heightDp = 800, name = "Filtered Transactions")
@Composable
fun TransactionListFilteredPreview() {
    val mockAccounts = listOf(
        Account("1", "Main Bank", BigDecimal("1000.00"), BigDecimal("1000.00"), "USD", "REGULAR", "#2196F3", "AccountBalance"),
        Account("2", "Cash", BigDecimal("500.00"), BigDecimal("500.00"), "USD", "REGULAR", "#4CAF50", "AccountBalanceWallet")
    )
    
    val mockCategories = listOf(
        CategoryEntity(1, "Food", "Restaurant", -12627531, true),
        CategoryEntity(2, "Transport", "DirectionsCar", -14575885, true)
    )
    
    val mockTransactions = listOf(
        TransactionWithBalance(
            transaction = Transaction(
                id = 1,
                title = "Lunch at Burger King",
                amount = BigDecimal("15.50"),
                category = "Food",
                categoryId = 1,
                timestamp = System.currentTimeMillis(),
                currency = "USD",
                isIncome = false,
                accountId = "1",
                paymentMethod = "Credit Card",
                payee = "Burger King",
                note = "Quick lunch"
            ),
            balanceBefore = BigDecimal("1000.00"),
            balanceAfter = BigDecimal("984.50")
        )
    )

    OssWalletTheme {
        TransactionListContent(
            transactionsWithBalance = mockTransactions,
            accounts = mockAccounts,
            accountMap = mockAccounts.associate { it.id to it.name },
            accountCurrencyMap = mockAccounts.associate { it.id to it.currencyCode },
            categories = mockCategories,
            categoryMap = mockCategories.associateBy { it.id },
            decimalPrecision = 2,
            analyticsState = AnalyticsUiState(selectedCurrency = "USD"),
            filter = TransactionFilter(queryPayee = "Burger"),
            recordsSettings = com.osscabinet.ossWallet.data.local.TransactionSectionSettings(),
            onAddTransactionClick = { _, _ -> },
            onEditTransactionClick = {},
            onBackClick = {},
            onAccountDetailClick = {},
            onUpdateFilter = {},
            onSetDateRange = { _, _ -> },
            onUpdateRecordsSettings = {}
        )
    }
}
