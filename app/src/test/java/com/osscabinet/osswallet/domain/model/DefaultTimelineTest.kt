package com.osscabinet.ossWallet.domain.model

import com.osscabinet.ossWallet.domain.model.settings.AccountHeroCardConfig
import com.osscabinet.ossWallet.domain.model.settings.withClearedFilter
import com.osscabinet.ossWallet.domain.model.timeline.DefaultTimeline
import com.osscabinet.ossWallet.domain.model.timeline.ThisPeriodType
import com.osscabinet.ossWallet.domain.model.timeline.TimelineRange
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class DefaultTimelineTest {

    @Test
    fun `initial timeline is this year up to now`() {
        assertEquals(
            TimelineRange.ThisPeriod(ThisPeriodType.YEAR, excludeFuture = true),
            DefaultTimeline.INITIAL
        )
    }

    @Test
    fun `account hero card starts on the initial timeline and keeps it when cleared`() {
        val config = AccountHeroCardConfig(cardId = "hero")
        assertEquals(DefaultTimeline.INITIAL, config.filter.timelineRange)
        assertEquals(
            DefaultTimeline.INITIAL,
            (config.withClearedFilter() as AccountHeroCardConfig).filter.timelineRange
        )
    }

    @Test
    fun `plain transaction filter still defaults to all time so merge treats it as unset`() {
        assertTrue(TransactionFilter().timelineRange is TimelineRange.AllTime)
    }
}
