package com.osscabinet.ossWallet.domain.model.settings

import com.osscabinet.ossWallet.domain.model.TransactionFilter
import com.osscabinet.ossWallet.domain.model.timeline.LastPeriodType
import com.osscabinet.ossWallet.domain.model.timeline.TimelineRange
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Unit tests for analytics card config serialization.
 * Tests ensure that analytics card configs can be serialized and deserialized correctly.
 */
class AnalyticsCardConfigSerializationTest {

    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
    }

    @Test
    fun `CategoryBreakdownCardConfig serialization works`() {
        val config = CategoryBreakdownCardConfig(
            cardId = "test_category_breakdown",
            order = 0,
            title = "CATEGORY BREAKDOWN",
            showLabels = true,
            showLegend = true,
            filter = TransactionFilter(
                timelineRange = TimelineRange.LastPeriod(LastPeriodType.LAST_30_DAYS),
                accountIds = listOf("test_account")
            )
        )
        val serialized = json.encodeToString(config)
        val deserialized = json.decodeFromString<CategoryBreakdownCardConfig>(serialized)
        assertEquals(config.cardId, deserialized.cardId)
        assertEquals(config.order, deserialized.order)
        assertEquals(config.title, deserialized.title)
        assertEquals(config.showLabels, deserialized.showLabels)
        assertEquals(config.showLegend, deserialized.showLegend)
        assertEquals(config.filter.accountIds[0], deserialized.filter.accountIds[0])
    }

    @Test
    fun `TimelineTrajectoryCardConfig serialization works`() {
        val config = TimelineTrajectoryCardConfig(
            cardId = "test_timeline_trajectory",
            order = 1,
            title = "TIMELINE TRAJECTORY",
            showBalance = true,
            showExpense = true,
            showIncome = true,
            filter = TransactionFilter(
                timelineRange = TimelineRange.LastPeriod(LastPeriodType.LAST_30_DAYS),
                accountIds = listOf("test_account")
            )
        )
        val serialized = json.encodeToString(config)
        val deserialized = json.decodeFromString<TimelineTrajectoryCardConfig>(serialized)
        assertEquals(config.cardId, deserialized.cardId)
        assertEquals(config.order, deserialized.order)
        assertEquals(config.title, deserialized.title)
        assertEquals(config.showBalance, deserialized.showBalance)
        assertEquals(config.showExpense, deserialized.showExpense)
        assertEquals(config.showIncome, deserialized.showIncome)
        assertEquals(config.filter.accountIds[0], deserialized.filter.accountIds[0])
    }

    @Test
    fun `CategoryExpenseTreemapCardConfig serialization works`() {
        val config = CategoryExpenseTreemapCardConfig(
            cardId = "test_category_expense_treemap",
            order = 2,
            title = "CATEGORY EXPENSES",
            showLabels = true,
            filter = TransactionFilter(
                timelineRange = TimelineRange.LastPeriod(LastPeriodType.LAST_30_DAYS),
                accountIds = listOf("test_account")
            )
        )
        val serialized = json.encodeToString(config)
        val deserialized = json.decodeFromString<CategoryExpenseTreemapCardConfig>(serialized)
        assertEquals(config.cardId, deserialized.cardId)
        assertEquals(config.order, deserialized.order)
        assertEquals(config.title, deserialized.title)
        assertEquals(config.showLabels, deserialized.showLabels)
        assertEquals(config.filter.accountIds[0], deserialized.filter.accountIds[0])
    }

    @Test
    fun `AnalyticsCardsConfig serialization works`() {
        val config = AnalyticsCardsConfig(
            cards = listOf(
                CategoryBreakdownCardConfig(
                    cardId = "test_category_breakdown",
                    order = 0,
                    title = "CATEGORY BREAKDOWN"
                ),
                TimelineTrajectoryCardConfig(
                    cardId = "test_timeline_trajectory",
                    order = 1,
                    title = "TIMELINE TRAJECTORY"
                )
            )
        )
        val serialized = json.encodeToString(config)
        val deserialized = json.decodeFromString<AnalyticsCardsConfig>(serialized)
        assertEquals(config.cards.size, deserialized.cards.size)
        assertTrue(deserialized.cards[0] is CategoryBreakdownCardConfig)
        assertTrue(deserialized.cards[1] is TimelineTrajectoryCardConfig)
    }

    @Test
    fun `AnalyticsCardsConfig empty serialization works`() {
        val config = AnalyticsCardsConfig()
        val serialized = json.encodeToString(config)
        val deserialized = json.decodeFromString<AnalyticsCardsConfig>(serialized)
        assertTrue(deserialized.cards.isEmpty())
    }

    @Test
    fun `CardConfig sealed class serialization works with CategoryBreakdownCardConfig`() {
        val config: CardConfig = CategoryBreakdownCardConfig(
            cardId = "test_category_breakdown",
            order = 0,
            title = "CATEGORY BREAKDOWN"
        )
        val serialized = json.encodeToString(config)
        val deserialized = json.decodeFromString<CardConfig>(serialized)
        assertTrue(deserialized is CategoryBreakdownCardConfig)
        assertEquals(config.cardId, deserialized.cardId)
    }

    @Test
    fun `CardConfig sealed class serialization works with TimelineTrajectoryCardConfig`() {
        val config: CardConfig = TimelineTrajectoryCardConfig(
            cardId = "test_timeline_trajectory",
            order = 1,
            title = "TIMELINE TRAJECTORY"
        )
        val serialized = json.encodeToString(config)
        val deserialized = json.decodeFromString<CardConfig>(serialized)
        assertTrue(deserialized is TimelineTrajectoryCardConfig)
        assertEquals(config.cardId, deserialized.cardId)
    }

    @Test
    fun `CardConfig sealed class serialization works with CategoryExpenseTreemapCardConfig`() {
        val config: CardConfig = CategoryExpenseTreemapCardConfig(
            cardId = "test_category_expense_treemap",
            order = 2,
            title = "CATEGORY EXPENSES"
        )
        val serialized = json.encodeToString(config)
        val deserialized = json.decodeFromString<CardConfig>(serialized)
        assertTrue(deserialized is CategoryExpenseTreemapCardConfig)
        assertEquals(config.cardId, deserialized.cardId)
    }
}
