package com.osscabinet.ossWallet.domain.calculator

import com.osscabinet.ossWallet.domain.model.Account
import com.osscabinet.ossWallet.domain.model.Transaction
import com.osscabinet.ossWallet.domain.model.TransactionStatus
import com.osscabinet.ossWallet.domain.model.analytics.ComputationContext
import com.osscabinet.ossWallet.domain.card.CardComputerRegistry
import com.osscabinet.ossWallet.domain.card.CardInput
import com.osscabinet.ossWallet.domain.card.CardRenderData
import com.osscabinet.ossWallet.domain.card.CardType
import com.osscabinet.ossWallet.domain.model.settings.NetWorthCardConfig
import com.osscabinet.ossWallet.domain.model.settings.CashFlowCardConfig
import com.osscabinet.ossWallet.domain.model.settings.CurrencyBalancesCardConfig
import com.osscabinet.ossWallet.domain.model.TransactionFilter
import com.osscabinet.ossWallet.domain.model.timeline.TimelineRange
import org.junit.Assert.assertEquals
import org.junit.Test
import java.math.BigDecimal

class CardComputationTest {

    private val baseCurrency = "USD"
    private val exchangeRates = mapOf("USD" to BigDecimal.ONE, "EUR" to BigDecimal("0.85"))
    private val registry = CardComputerRegistry.DEFAULT
    
    private val accounts = listOf(
        Account(id = "1", name = "Cash", currencyCode = "USD", structureType = "wallet", colorHex = "#FFFFFF", icon = "wallet"),
        Account(id = "2", name = "Bank", currencyCode = "EUR", structureType = "bank", colorHex = "#000000", icon = "bank")
    )

    private val transactions = listOf(
        Transaction(id = 1L, title = "Salary", amount = BigDecimal("1000"), category = "salary", timestamp = System.currentTimeMillis(), currency = "USD", transactionType = "income", accountId = "1"),
        Transaction(id = 2L, title = "Lunch", amount = BigDecimal("-50"), category = "food", timestamp = System.currentTimeMillis(), currency = "EUR", transactionType = "expense", accountId = "2")
    )

    private val input = CardInput(
        transactions = transactions,
        accounts = accounts,
        categories = emptyList(),
        exchangeRates = exchangeRates,
        baseCurrency = baseCurrency,
        precision = 2
    )

    @Test
    fun `NetWorthCalculator computes correctly`() {
        val config = NetWorthCardConfig(cardId = "test", filter = TransactionFilter(timelineRange = TimelineRange.AllTime))
        val result = registry.compute(CardType.NET_WORTH, input, config) as CardRenderData.NetWorth
        
        // 1000 USD + (-50 EUR converted to USD)
        // EUR to USD: -50 / 0.85 = -58.8235...
        // 1000 - 58.8235... = 941.1764...
        assertEquals(BigDecimal("941.18"), result.value.setScale(2, java.math.RoundingMode.HALF_UP))
    }

    @Test
    fun `CashFlowCalculator computes correctly`() {
        val config = CashFlowCardConfig(cardId = "test", filter = TransactionFilter(timelineRange = TimelineRange.AllTime))
        val result = registry.compute(CardType.CASH_FLOW, input, config) as CardRenderData.CashFlow
        
        assertEquals(BigDecimal("1000.00"), result.result.totalIncomeInBase)
        assertEquals(BigDecimal("-58.82"), result.result.totalExpensesInBase)
        assertEquals(BigDecimal("941.18"), result.result.netInBase)
    }

    @Test
    fun `CurrencyBalancesCalculator computes correctly`() {
        val config = CurrencyBalancesCardConfig(cardId = "test", filter = TransactionFilter(timelineRange = TimelineRange.AllTime))
        val result = registry.compute(CardType.CURRENCY_BALANCES, input, config) as CardRenderData.CurrencyBalances
        
        assertEquals(2, result.items.size)
        val usd = result.items.find { it.currency == "USD" }!!
        val eur = result.items.find { it.currency == "EUR" }!!
        
        assertEquals(0, BigDecimal("1000").compareTo(usd.amount))
        assertEquals(0, BigDecimal("-50").compareTo(eur.amount))
    }
}
