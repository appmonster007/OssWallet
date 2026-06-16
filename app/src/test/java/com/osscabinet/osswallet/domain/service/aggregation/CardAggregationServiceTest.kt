package com.osscabinet.ossWallet.domain.service.aggregation

import com.osscabinet.ossWallet.domain.card.CardInput
import com.osscabinet.ossWallet.domain.model.Account
import com.osscabinet.ossWallet.domain.model.AccountStructureType
import com.osscabinet.ossWallet.domain.model.TransactionFilter
import com.osscabinet.ossWallet.domain.model.settings.AccountsCardConfig
import com.osscabinet.ossWallet.domain.model.timeline.ThisPeriodType
import com.osscabinet.ossWallet.domain.model.timeline.TimelineRange
import com.osscabinet.ossWallet.domain.service.currency.ExchangeRateSnapshot
import com.osscabinet.ossWallet.domain.service.financial.FinancialSummary
import com.osscabinet.ossWallet.domain.service.query.DatabaseQueryService
import com.osscabinet.ossWallet.domain.service.timeline.TimelineService
import io.mockk.*
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Before
import org.junit.Test
import java.math.BigDecimal

class CardAggregationServiceTest {

    private val queryService: DatabaseQueryService = mockk()
    private val profileId = 1
    private val baseCurrency = "USD"
    private val exchangeRates = ExchangeRateSnapshot(
        anchor = baseCurrency,
        rates = mapOf(baseCurrency to BigDecimal.ONE)
    )

    private val sampleAccount = Account(
        id = "acc1",
        name = "Checking",
        currencyCode = "USD",
        structureType = AccountStructureType.BANK,
        colorHex = "#000000",
        icon = "bank"
    )

    @Before
    fun setup() {
        mockkObject(TransactionAggregationService)
        mockkObject(TimelineService)
    }

    @After
    fun tearDown() {
        unmockkAll()
    }

    @Test
    fun `computeAccountsCard populates current and previous flow metrics correctly`() = runBlocking {
        val config = AccountsCardConfig(
            cardId = "test",
            showComparison = true,
            filter = TransactionFilter(timelineRange = TimelineRange.ThisPeriod(ThisPeriodType.MONTH))
        )
        
        val input = CardInput(
            profileId = profileId,
            targetCurrency = baseCurrency,
            exchangeRates = exchangeRates,
            filter = TransactionFilter(timelineRange = TimelineRange.ThisPeriod(ThisPeriodType.MONTH)),
            accounts = listOf(sampleAccount),
            categories = emptyList()
        )

        // Mock current period summary
        val currentSummary = mapOf(
            "acc1" to mapOf(
                "USD" to FinancialSummary(
                    income = BigDecimal("1000.00"),
                    expense = BigDecimal("-200.00"),
                    totalAmount = BigDecimal("5000.00"),
                    transfer = BigDecimal.ZERO,
                    transactionCount = 5
                )
            )
        )
        
        // Mock previous period summary
        val previousSummary = mapOf(
            "acc1" to mapOf(
                "USD" to FinancialSummary(
                    income = BigDecimal("800.00"),
                    expense = BigDecimal("-150.00"),
                    totalAmount = BigDecimal("4200.00"),
                    transfer = BigDecimal.ZERO,
                    transactionCount = 4
                )
            )
        )

        coEvery { 
            TransactionAggregationService.getSummaryByAccountAndCurrency(queryService, profileId, any()) 
        } returnsMany listOf(
            Result.success(currentSummary), // Balance filter call
            Result.success(currentSummary), // Current period call
            Result.success(previousSummary), // Balance filter call
            Result.success(previousSummary)  // Previous period call
        )

        every { TimelineService.getPreviousRange(any()) } returns Pair(1000L, 2000L)

        val result = CardAggregationService.computeAccountsCard(queryService, profileId, input, config)

        val summary = result.accountSummaries["acc1"]
        assertNotNull(summary)
        
        // Current Period Flow
        assertEquals(0, BigDecimal("1000.00").compareTo(summary!!.currentPeriodFlow.income))
        assertEquals(0, BigDecimal("-200.00").compareTo(summary.currentPeriodFlow.expense))
        
        // Previous Period Flow
        assertNotNull(summary.previousPeriodFlow)
        assertEquals(0, BigDecimal("800.00").compareTo(summary.previousPeriodFlow?.income))
        assertEquals(0, BigDecimal("-150.00").compareTo(summary.previousPeriodFlow?.expense))
        
        // Balance (Cumulative)
        assertEquals(0, BigDecimal("5000.00").compareTo(summary.balance))
    }
}
