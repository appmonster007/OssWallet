package com.osscabinet.ossWallet.domain.calculator

import com.osscabinet.ossWallet.domain.model.Account
import com.osscabinet.ossWallet.domain.model.AccountStructureType
import com.osscabinet.ossWallet.domain.model.Category
import com.osscabinet.ossWallet.domain.model.Transaction
import com.osscabinet.ossWallet.domain.model.TransactionType
import com.osscabinet.ossWallet.domain.card.CardComputerRegistry
import com.osscabinet.ossWallet.domain.card.CardInput
import com.osscabinet.ossWallet.domain.card.CardRenderData
import com.osscabinet.ossWallet.domain.card.CardType
import com.osscabinet.ossWallet.domain.card.CardDataComputer
import com.osscabinet.ossWallet.domain.card.NetWorthMetrics
import com.osscabinet.ossWallet.domain.model.settings.NetWorthCardConfig
import com.osscabinet.ossWallet.domain.model.settings.CashFlowCardConfig
import com.osscabinet.ossWallet.domain.model.settings.CurrencyBalancesCardConfig
import com.osscabinet.ossWallet.domain.model.TransactionFilter
import com.osscabinet.ossWallet.domain.model.timeline.TimelineRange
import com.osscabinet.ossWallet.domain.service.aggregation.AggregationService
import com.osscabinet.ossWallet.domain.service.currency.CurrencyService
import com.osscabinet.ossWallet.domain.service.currency.ExchangeRateSnapshot
import org.junit.Assert.assertEquals
import org.junit.Test
import java.math.BigDecimal

class CardComputationTest {

    private val baseCurrency = "USD"
    private val exchangeRates = ExchangeRateSnapshot(
        anchor = "USD",
        rates = mapOf("USD" to BigDecimal.ONE, "EUR" to BigDecimal("0.85"))
    )

    private val registry = CardComputerRegistry(listOf(
        object : CardDataComputer<NetWorthCardConfig, CardRenderData.NetWorth> {
            override val supportedType = CardType.NET_WORTH
            override suspend fun compute(input: CardInput, config: NetWorthCardConfig): CardRenderData.NetWorth {
                val totals = AggregationService.calculateTotalByCurrency(input.transactions)
                val netWorth = CurrencyService.calculateTotalInCurrency(
                    totals, input.targetCurrency, input.exchangeRates
                )
                val cashFlow = AggregationService.calculateCashFlowResult(input.transactions, input.targetCurrency, input.exchangeRates)
                return CardRenderData.NetWorth(config, NetWorthMetrics(netWorth, cashFlow), input.targetCurrency)
            }
        },
        object : CardDataComputer<CashFlowCardConfig, CardRenderData.CashFlow> {
            override val supportedType = CardType.CASH_FLOW
            override suspend fun compute(input: CardInput, config: CashFlowCardConfig): CardRenderData.CashFlow {
                val result = AggregationService.calculateCashFlowResult(input.transactions, input.targetCurrency, input.exchangeRates)
                return CardRenderData.CashFlow(config, result, input.targetCurrency)
            }
        },
        object : CardDataComputer<CurrencyBalancesCardConfig, CardRenderData.CurrencyBalances> {
            override val supportedType = CardType.CURRENCY_BALANCES
            override suspend fun compute(input: CardInput, config: CurrencyBalancesCardConfig): CardRenderData.CurrencyBalances {
                val items = AggregationService.calculateCurrencyBalances(input.transactions, input.targetCurrency, input.exchangeRates)
                return CardRenderData.CurrencyBalances(config, items, input.targetCurrency)
            }
        }
    ))
    
    private val accounts = listOf(
        Account(id = "1", name = "Cash", currencyCode = "USD", structureType = AccountStructureType.CASH, colorHex = "#FFFFFF", icon = "wallet"),
        Account(id = "2", name = "Bank", currencyCode = "EUR", structureType = AccountStructureType.BANK, colorHex = "#000000", icon = "bank")
    )

    private val transactions = listOf(
        Transaction(id = 1L, name = "Salary", amount = BigDecimal("1000"), categoryId = 1001L, timestamp = System.currentTimeMillis(), currency = "USD", transactionType = TransactionType.INCOME, accountId = "1"),
        Transaction(id = 2L, name = "Lunch", amount = BigDecimal("-50"), categoryId = Category.SYSTEM_UNKNOWN, timestamp = System.currentTimeMillis(), currency = "EUR", transactionType = TransactionType.EXPENSE, accountId = "2")
    )

    private val input = CardInput(
        transactions = transactions,
        accounts = accounts,
        categories = emptyList(),
        exchangeRates = exchangeRates,
        targetCurrency = baseCurrency
    )

    @Test
    fun `NetWorthCalculator computes correctly`() {
        val config = NetWorthCardConfig(cardId = "test", filter = TransactionFilter(timelineRange = TimelineRange.AllTime()))
        val result = runBlocking { registry.compute(CardType.NET_WORTH, input, config) as CardRenderData.NetWorth }
        
        // 1000 USD + (-50 EUR converted to USD)
        // EUR to USD: -50 / 0.85 = -58.8235...
        // 1000 - 58.8235... = 941.1764...
        assertEquals(BigDecimal("941.18"), result.currentPeriodMetrics.netWorth.setScale(2, java.math.RoundingMode.HALF_UP))
    }

    @Test
    fun `CashFlowCalculator computes correctly`() {
        val config = CashFlowCardConfig(cardId = "test", filter = TransactionFilter(timelineRange = TimelineRange.AllTime()))
        val result = runBlocking { registry.compute(CardType.CASH_FLOW, input, config) as CardRenderData.CashFlow }
        
        assertEquals(BigDecimal("1000.00"), result.result.totalIncomeInBase)
        assertEquals(BigDecimal("-58.82"), result.result.totalExpensesInBase)
        assertEquals(BigDecimal("941.18"), result.result.netInBase)
    }

    @Test
    fun `CurrencyBalancesCalculator computes correctly`() {
        val config = CurrencyBalancesCardConfig(cardId = "test", filter = TransactionFilter(timelineRange = TimelineRange.AllTime()))
        val result = runBlocking { registry.compute(CardType.CURRENCY_BALANCES, input, config) as CardRenderData.CurrencyBalances }
        
        assertEquals(2, result.items.size)
        val usd = result.items.find { it.currency == "USD" }!!
        val eur = result.items.find { it.currency == "EUR" }!!
        
        assertEquals(0, BigDecimal("1000").compareTo(usd.amount))
        assertEquals(0, BigDecimal("-50").compareTo(eur.amount))
    }

    private fun <T> runBlocking(block: suspend () -> T): T = kotlinx.coroutines.runBlocking { block() }
}
