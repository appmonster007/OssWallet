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
        balance = BigDecimal("1000"),
        currencyCode = "USD",
        structureType = "Bank",
        colorHex = "#FF0000",
        icon = "bank"
    )

    private val account2 = Account(
        id = "acc2",
        name = "Account 2",
        balance = BigDecimal("2000"),
        currencyCode = "EUR",
        structureType = "Cash",
        colorHex = "#00FF00",
        icon = "cash"
    )

    private val account3 = Account(
        id = "acc3",
        name = "Account 3",
        balance = BigDecimal("3000"),
        currencyCode = "GBP",
        structureType = "Credit Card",
        colorHex = "#0000FF",
        icon = "credit_card"
    )

    private val transaction1 = Transaction(
        id = 1,
        title = "Food",
        amount = BigDecimal("100"),
        category = "Food",
        timestamp = System.currentTimeMillis(),
        currency = "USD",
        transactionType = "expense",
        accountId = "acc1",
        status = TransactionStatus.CLEARED
    )

    private val transaction2 = Transaction(
        id = 2,
        title = "Transport",
        amount = BigDecimal("200"),
        category = "Transport",
        timestamp = System.currentTimeMillis(),
        currency = "EUR",
        transactionType = "expense",
        accountId = "acc2",
        status = TransactionStatus.CLEARED
    )

    private val transaction3 = Transaction(
        id = 3,
        title = "Entertainment",
        amount = BigDecimal("300"),
        category = "Entertainment",
        timestamp = System.currentTimeMillis(),
        currency = "GBP",
        transactionType = "expense",
        accountId = "acc3",
        status = TransactionStatus.CLEARED
    )

    private val allAccounts = listOf(account1, account2, account3)
    private val allTransactions = listOf(transaction1, transaction2, transaction3)

    @Test
    fun `filterAccounts with NetWorthCardConfig and selectedAccountIds returns filtered accounts`() {
        val config = NetWorthCardConfig(
            cardId = "networth1",
            order = 1,
            fontSize = "medium",
            timelineRange = TimelineRange.AllTime,
            selectedAccountIds = listOf("acc1", "acc2")
        )

        val filtered = config.filterAccounts(allAccounts)

        assertEquals(2, filtered.size)
        assertTrue(filtered.any { it.id == "acc1" })
        assertTrue(filtered.any { it.id == "acc2" })
        assertTrue(filtered.none { it.id == "acc3" })
    }

    @Test
    fun `filterAccounts with NetWorthCardConfig and no selectedAccountIds returns all accounts`() {
        val config = NetWorthCardConfig(
            cardId = "networth1",
            order = 1,
            fontSize = "medium",
            timelineRange = TimelineRange.AllTime,
            selectedAccountIds = null
        )

        val filtered = config.filterAccounts(allAccounts)

        assertEquals(3, filtered.size)
    }

    @Test
    fun `filterAccounts with NetWorthCardConfig and empty selectedAccountIds returns all accounts`() {
        val config = NetWorthCardConfig(
            cardId = "networth1",
            order = 1,
            fontSize = "medium",
            timelineRange = TimelineRange.AllTime,
            selectedAccountIds = emptyList()
        )

        val filtered = config.filterAccounts(allAccounts)

        assertEquals(3, filtered.size)
    }

    @Test
    fun `filterAccounts with CurrencyBalancesCardConfig and accountId returns filtered accounts`() {
        val config = CurrencyBalancesCardConfig(
            cardId = "currency1",
            order = 1,
            layout = "grid",
            showBaseConversion = true,
            timelineRange = TimelineRange.AllTime,
            filter = TransactionFilter(accountIds = listOf("acc1"))
        )

        val filtered = config.filterAccounts(allAccounts)

        assertEquals(1, filtered.size)
        assertEquals("acc1", filtered[0].id)
    }

    @Test
    fun `filterAccounts with CurrencyBalancesCardConfig and no accountId returns all accounts`() {
        val config = CurrencyBalancesCardConfig(
            cardId = "currency1",
            order = 1,
            layout = "grid",
            showBaseConversion = true,
            timelineRange = TimelineRange.AllTime,
            filter = TransactionFilter(accountIds = emptyList())
        )

        val filtered = config.filterAccounts(allAccounts)

        assertEquals(3, filtered.size)
    }

    @Test
    fun `filterAccounts with CashFlowCardConfig returns all accounts`() {
        val config = CashFlowCardConfig(
            cardId = "cashflow1",
            order = 1,
            showBaseConversion = true,
            timelineRange = TimelineRange.AllTime,
            filter = TransactionFilter(accountIds = listOf("acc1"))
        )

        val filtered = config.filterAccounts(allAccounts)

        assertEquals(3, filtered.size)
    }

    @Test
    fun `filterAccounts with AccountHeroCardConfig returns all accounts`() {
        val config = AccountHeroCardConfig(
            cardId = "hero1",
            order = 1,
            showCurrencyBadges = true
        )

        val filtered = config.filterAccounts(allAccounts)

        assertEquals(3, filtered.size)
    }

    @Test
    fun `filterTransactions with NetWorthCardConfig and selectedAccountIds returns filtered transactions`() {
        val config = NetWorthCardConfig(
            cardId = "networth1",
            order = 1,
            fontSize = "medium",
            timelineRange = TimelineRange.AllTime,
            selectedAccountIds = listOf("acc1", "acc2")
        )

        val filtered = config.filterTransactions(allTransactions)

        assertEquals(2, filtered.size)
        assertTrue(filtered.any { it.accountId == "acc1" })
        assertTrue(filtered.any { it.accountId == "acc2" })
        assertTrue(filtered.none { it.accountId == "acc3" })
    }

    @Test
    fun `filterTransactions with NetWorthCardConfig and no selectedAccountIds returns all transactions`() {
        val config = NetWorthCardConfig(
            cardId = "networth1",
            order = 1,
            fontSize = "medium",
            timelineRange = TimelineRange.AllTime,
            selectedAccountIds = null
        )

        val filtered = config.filterTransactions(allTransactions)

        assertEquals(3, filtered.size)
    }

    @Test
    fun `filterTransactions with CurrencyBalancesCardConfig and accountId returns filtered transactions`() {
        val config = CurrencyBalancesCardConfig(
            cardId = "currency1",
            order = 1,
            layout = "grid",
            showBaseConversion = true,
            timelineRange = TimelineRange.AllTime,
            filter = TransactionFilter(accountIds = listOf("acc1"))
        )

        val filtered = config.filterTransactions(allTransactions)

        assertEquals(1, filtered.size)
        assertEquals("acc1", filtered[0].accountId)
    }

    @Test
    fun `filterTransactions with CurrencyBalancesCardConfig and no accountId returns all transactions`() {
        val config = CurrencyBalancesCardConfig(
            cardId = "currency1",
            order = 1,
            layout = "grid",
            showBaseConversion = true,
            timelineRange = TimelineRange.AllTime,
            filter = TransactionFilter(accountIds = emptyList())
        )

        val filtered = config.filterTransactions(allTransactions)

        assertEquals(3, filtered.size)
    }

    @Test
    fun `filterTransactions with CashFlowCardConfig and accountId returns filtered transactions`() {
        val config = CashFlowCardConfig(
            cardId = "cashflow1",
            order = 1,
            showBaseConversion = true,
            timelineRange = TimelineRange.AllTime,
            filter = TransactionFilter(accountIds = listOf("acc2"))
        )

        val filtered = config.filterTransactions(allTransactions)

        assertEquals(1, filtered.size)
        assertEquals("acc2", filtered[0].accountId)
    }

    @Test
    fun `filterTransactions with CashFlowCardConfig and no accountId returns all transactions`() {
        val config = CashFlowCardConfig(
            cardId = "cashflow1",
            order = 1,
            showBaseConversion = true,
            timelineRange = TimelineRange.AllTime,
            filter = TransactionFilter(accountIds = emptyList())
        )

        val filtered = config.filterTransactions(allTransactions)

        assertEquals(3, filtered.size)
    }

    @Test
    fun `filterTransactions with AccountHeroCardConfig returns all transactions`() {
        val config = AccountHeroCardConfig(
            cardId = "hero1",
            order = 1,
            showCurrencyBadges = true
        )

        val filtered = config.filterTransactions(allTransactions)

        assertEquals(3, filtered.size)
    }
}
