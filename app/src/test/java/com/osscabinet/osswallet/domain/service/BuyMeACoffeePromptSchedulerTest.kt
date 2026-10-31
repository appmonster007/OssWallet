package com.osscabinet.ossWallet.domain.service

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class BuyMeACoffeePromptSchedulerTest {

    // Anchor Monday is 2024-01-01, so 2024-01-01 and 2024-01-15 are "alternate" weeks;
    // 2024-01-08 and 2024-01-22 are the skipped weeks in between.
    private val alternateWeekMonday = LocalDate.of(2024, 1, 15)
    private val skippedWeekMonday = LocalDate.of(2024, 1, 8)

    @Test
    fun `shows prompt on an alternate week when never shown before`() {
        val today = alternateWeekMonday.plusDays(3) // Thursday within the window
        assertTrue(
            BuyMeACoffeePromptScheduler.shouldShowPrompt(today, BuyMeACoffeePromptScheduler.NEVER_SHOWN_EPOCH_DAY)
        )
    }

    @Test
    fun `does not show prompt on a skipped week`() {
        val today = skippedWeekMonday.plusDays(2)
        assertFalse(
            BuyMeACoffeePromptScheduler.shouldShowPrompt(today, BuyMeACoffeePromptScheduler.NEVER_SHOWN_EPOCH_DAY)
        )
    }

    @Test
    fun `does not show prompt again within the same alternate window once recorded`() {
        val today = alternateWeekMonday.plusDays(5)
        val lastShown = BuyMeACoffeePromptScheduler.currentWindowEpochDay(alternateWeekMonday)
        assertFalse(BuyMeACoffeePromptScheduler.shouldShowPrompt(today, lastShown))
    }

    @Test
    fun `shows prompt again once a new alternate window starts`() {
        val nextAlternateMonday = alternateWeekMonday.plusWeeks(2)
        val lastShown = BuyMeACoffeePromptScheduler.currentWindowEpochDay(alternateWeekMonday)
        assertTrue(
            BuyMeACoffeePromptScheduler.shouldShowPrompt(nextAlternateMonday.plusDays(1), lastShown)
        )
    }
}
