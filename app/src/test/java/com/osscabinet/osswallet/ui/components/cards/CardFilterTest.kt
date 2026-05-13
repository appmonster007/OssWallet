package com.osscabinet.ossWallet.ui.components.cards

import com.osscabinet.ossWallet.domain.model.Account
import com.osscabinet.ossWallet.domain.model.Transaction
import com.osscabinet.ossWallet.domain.model.TransactionStatus
import com.osscabinet.ossWallet.domain.model.TransactionFilter
import com.osscabinet.ossWallet.domain.model.settings.*
import com.osscabinet.ossWallet.domain.model.timeline.TimelineRange
import org.junit.Test
import org.junit.Assert.*
import java.math.BigDecimal

/**
 * Unit tests for CardFilter extension functions.
 */
class CardFilterTest {

    private val account1 = Account(
        id = "acc1",
        name = "Account 1",
        currencyCode = "USD",
        structureType = "Bank",
        colorHex = "#FF0000",
        icon = "bank"
    )

    private val account2 = Account(
        id = "acc2",
        name = "Account 2",
        currencyCode = "EUR",
        structureType = "Cash",
        colorHex = "#00FF00",
        icon = "cash"
    )

    private val account3 = Account(
        id = "acc3",
        name = "Account 3",
        currencyCode = "GBP",
        structureType = "Credit Card",
        colorHex = "#0000FF",
        icon = "credit_card"
    )

    private val transaction1 = Transaction(
        id = 1,
        name = "Food",
        amount = BigDecimal("100"),
        categoryId = 1001L,
        timestamp = System.currentTimeMillis(),
        currency = "USD",
        transactionType = "expense",
        accountId = "acc1",
        status = TransactionStatus.CLEARED
    )

    private val transaction2 = Transaction(
        id = 2,
        name = "Transport",
        amount = BigDecimal("200"),
        categoryId = 1002L,
        timestamp = System.currentTimeMillis(),
        currency = "EUR",
        transactionType = "expense",
        accountId = "acc2",
        status = TransactionStatus.CLEARED
    )

    private val transaction3 = Transaction(
        id = 3,
        name = "Entertainment",
        amount = BigDecimal("300"),
        categoryId = 1003L,
        timestamp = System.currentTimeMillis(),
        currency = "GBP",
        transactionType = "expense",
        accountId = "acc3",
        status = TransactionStatus.CLEARED
    )

    private val allAccounts = listOf(account1, account2, account3)
    private val allTransactions = listOf(transaction1, transaction2, transaction3)

    // Helper extension functions (assuming these still exist or are being tested)
    private fun CardConfig.filterAccounts(accounts: List<Account>): List<Account> {
        val accountIds = when (this) {
            is NetWorthCardConfig -> filter.accountIds
            is CurrencyBalancesCardConfig -> filter.accountIds
            is CashFlowCardConfig -> filter.accountIds
            is AccountsCardConfig -> filter.accountIds
            is AccountHeroCardConfig -> filter.accountIds
            else -> emptyList()
        }
        return if (accountIds.isEmpty()) accounts else accounts.filter { accountIds.contains(it.id) }
    }

    private fun CardConfig.filterTransactions(transactions: List<Transaction>): List<Transaction> {
        val accountIds = when (this) {
            is NetWorthCardConfig -> filter.accountIds
            is CurrencyBalancesCardConfig -> filter.accountIds
            is CashFlowCardConfig -> filter.accountIds
            is AccountsCardConfig -> filter.accountIds
            is AccountHeroCardConfig -> filter.accountIds
            else -> emptyList()
        }
        return if (accountIds.isEmpty()) transactions else transactions.filter { accountIds.contains(it.accountId) }
    }

    @Test
    fun `filterAccounts with NetWorthCardConfig and accountIds returns filtered accounts`() {
        val config = NetWorthCardConfig(
            cardId = "networth1",
            order = 1,
            fontSize = "medium",
            filter = TransactionFilter(accountIds = listOf("acc1", "acc2"), timelineRange = TimelineRange.AllTime)
        )

        val filtered = config.filterAccounts(allAccounts)

        assertEquals(2, filtered.size)
        assertTrue(filtered.any { it.id == "acc1" })
        assertTrue(filtered.any { it.id == "acc2" })
        assertTrue(filtered.none { it.id == "acc3" })
    }

    @Test
    fun `filterAccounts with NetWorthCardConfig and no accountIds returns all accounts`() {
        val config = NetWorthCardConfig(
            cardId = "networth1",
            order = 1,
            fontSize = "medium",
            filter = TransactionFilter(accountIds = emptyList(), timelineRange = TimelineRange.AllTime)
        )

        val filtered = config.filterAccounts(allAccounts)

        assertEquals(3, filtered.size)
    }

    @Test
    fun `filterAccounts with CurrencyBalancesCardConfig and accountIds returns filtered accounts`() {
        val config = CurrencyBalancesCardConfig(
            cardId = "currency1",
            order = 1,
            layout = "grid",
            showBaseConversion = true,
            filter = TransactionFilter(accountIds = listOf("acc1"), timelineRange = TimelineRange.AllTime)
        )

        val filtered = config.filterAccounts(allAccounts)

        assertEquals(1, filtered.size)
        assertEquals("acc1", filtered[0].id)
    }

    @Test
    fun `filterTransactions with NetWorthCardConfig and accountIds returns filtered transactions`() {
        val config = NetWorthCardConfig(
            cardId = "networth1",
            order = 1,
            fontSize = "medium",
            filter = TransactionFilter(accountIds = listOf("acc1", "acc2"), timelineRange = TimelineRange.AllTime)
        )

        val filtered = config.filterTransactions(allTransactions)

        assertEquals(2, filtered.size)
        assertTrue(filtered.any { it.accountId == "acc1" })
        assertTrue(filtered.any { it.accountId == "acc2" })
        assertTrue(filtered.none { it.accountId == "acc3" })
    }

    @Test
    fun `filterTransactions with CurrencyBalancesCardConfig and accountIds returns filtered transactions`() {
        val config = CurrencyBalancesCardConfig(
            cardId = "currency1",
            order = 1,
            layout = "grid",
            showBaseConversion = true,
            filter = TransactionFilter(accountIds = listOf("acc1"), timelineRange = TimelineRange.AllTime)
        )

        val filtered = config.filterTransactions(allTransactions)

        assertEquals(1, filtered.size)
        assertEquals("acc1", filtered[0].accountId)
    }

    @Test
    fun `filterTransactions with CashFlowCardConfig and accountIds returns filtered transactions`() {
        val config = CashFlowCardConfig(
            cardId = "cashflow1",
            order = 1,
            showBaseConversion = true,
            filter = TransactionFilter(accountIds = listOf("acc2"), timelineRange = TimelineRange.AllTime)
        )

        val filtered = config.filterTransactions(allTransactions)

        assertEquals(1, filtered.size)
        assertEquals("acc2", filtered[0].accountId)
    }
}
