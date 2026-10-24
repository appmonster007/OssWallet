package com.osscabinet.ossWallet.domain.service

import com.osscabinet.ossWallet.domain.model.Budget
import com.osscabinet.ossWallet.domain.model.BudgetPaceStatus
import com.osscabinet.ossWallet.domain.model.BudgetProgress
import com.osscabinet.ossWallet.domain.model.timeline.TimelineRange
import com.osscabinet.ossWallet.domain.service.management.BudgetService
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.math.BigDecimal

class BudgetPaceTest {

    private val day = 24L * 60 * 60 * 1000
    private val start = 1_800_000_000_000L
    private val end = start + 10 * day

    private fun budget(range: TimelineRange = TimelineRange.CustomRange(start, end), alert: Float = 0.8f) = Budget(
        name = "Groceries",
        currency = "USD",
        limitAmount = BigDecimal("100"),
        timelineRange = range,
        alertThreshold = alert,
        profileId = 1
    )

    private fun progress(spent: String, elapsed: Float?, alert: Float = 0.8f, planned: String = "0") =
        BudgetService.buildProgress(
            budget(alert = alert), BigDecimal(spent), elapsedFraction = elapsed, plannedAmount = BigDecimal(planned)
        )

    @Test
    fun `elapsed fraction is the share of the period that has passed`() {
        assertEquals(0.5f, BudgetService.elapsedFraction(budget(), start + 5 * day)!!, 0.001f)
        assertEquals(0.2f, BudgetService.elapsedFraction(budget(), start + 2 * day)!!, 0.001f)
    }

    @Test
    fun `elapsed fraction is clamped outside the period`() {
        assertEquals(0f, BudgetService.elapsedFraction(budget(), start - 3 * day)!!, 0.001f)
        assertEquals(1f, BudgetService.elapsedFraction(budget(), end + 3 * day)!!, 0.001f)
    }

    @Test
    fun `an all time budget has no pace`() {
        assertNull(BudgetService.elapsedFraction(budget(TimelineRange.AllTime()), start))
    }

    @Test
    fun `spending within tolerance of the elapsed share is on track`() {
        assertEquals(BudgetPaceStatus.ON_TRACK, BudgetService.paceStatus(progress("55", 0.5f)))
        assertEquals(BudgetPaceStatus.ON_TRACK, BudgetService.paceStatus(progress("60", 0.5f)))
    }

    @Test
    fun `spending clearly faster than the period is ahead of pace`() {
        assertEquals(BudgetPaceStatus.AHEAD_OF_PACE, BudgetService.paceStatus(progress("70", 0.5f)))
    }

    @Test
    fun `reaching the alert threshold late in the period is near limit not ahead`() {
        // 85% spent with 85% of the period gone: on pace, but past the 80% alert threshold.
        assertEquals(BudgetPaceStatus.NEAR_LIMIT, BudgetService.paceStatus(progress("85", 0.85f)))
    }

    @Test
    fun `over the limit wins over everything`() {
        assertEquals(BudgetPaceStatus.OVER, BudgetService.paceStatus(progress("120", 0.9f)))
        assertEquals(BudgetPaceStatus.OVER, BudgetService.paceStatus(progress("100", 1f)))
    }

    @Test
    fun `without a period only the alert level applies`() {
        assertEquals(BudgetPaceStatus.ON_TRACK, BudgetService.paceStatus(progress("50", null)))
        assertEquals(BudgetPaceStatus.NEAR_LIMIT, BudgetService.paceStatus(progress("85", null)))
    }

    @Test
    fun `progress carries the elapsed fraction`() {
        val p: BudgetProgress = progress("10", 0.4f)
        assertEquals(0.4f, p.elapsedFraction!!, 0.0001f)
    }

    @Test
    fun `period identity is stable across runs for an open-ended range`() {
        val b = budget(TimelineRange.AllTime())
        assertEquals(
            BudgetService.periodIdentity(b, start),
            BudgetService.periodIdentity(b, start + 3 * day)
        )
    }

    @Test
    fun `period identity is the period start for a fixed range`() {
        val b = budget()
        assertEquals(start, BudgetService.periodIdentity(b, start + day))
        assertEquals(start, BudgetService.periodIdentity(b, start + 4 * day))
    }

    // ---- paceTargetFraction / planned-aware pace ----

    @Test
    fun `pace target equals elapsed when nothing is planned`() {
        assertEquals(0.5f, BudgetService.paceTargetFraction(progress("40", 0.5f))!!, 0.001f)
    }

    @Test
    fun `pace target shrinks proportionally to planned's share of the limit`() {
        // limit=100, planned=20 -> discretionary 80% of the limit, half the period elapsed.
        assertEquals(0.4f, BudgetService.paceTargetFraction(progress("40", 0.5f, planned = "20"))!!, 0.001f)
    }

    @Test
    fun `pace target is null once planned alone would exceed the limit`() {
        // Already over before any time has passed — pace is moot, hide the marker rather than
        // pin it somewhere that no longer means anything.
        assertNull(BudgetService.paceTargetFraction(progress("0", 0.1f, planned = "150")))
    }

    @Test
    fun `pace target is null once spent plus planned exceeds the limit`() {
        // Same shape as the real-world case this was reported from: limit 500, spent 470,
        // planned 60, 90% elapsed — scaled to this file's limit of 100.
        assertNull(BudgetService.paceTargetFraction(progress("94", 0.9f, planned = "12")))
    }

    @Test
    fun `pace target is null without a bounded period`() {
        assertNull(BudgetService.paceTargetFraction(progress("40", null)))
    }

    @Test
    fun `over including planned flips pace status even when spent alone is under the limit`() {
        // spent=80 is under the 100 limit and under the 80% alert threshold's own boundary isn't
        // crossed by spent alone, but spent+planned=110 exceeds the limit outright.
        assertEquals(BudgetPaceStatus.OVER, BudgetService.paceStatus(progress("80", 0.5f, planned = "30")))
    }

    @Test
    fun `planned shrinking the target can turn ON_TRACK into AHEAD_OF_PACE`() {
        // Without planned: elapsed=0.5, spent=55 -> 0.55 is within tolerance (0.10) of 0.5 -> ON_TRACK.
        assertEquals(BudgetPaceStatus.ON_TRACK, BudgetService.paceStatus(progress("55", 0.5f)))
        // With a planned=20 commitment: target shrinks to 0.4, and 0.55 > 0.4+0.10 -> AHEAD_OF_PACE.
        assertEquals(BudgetPaceStatus.AHEAD_OF_PACE, BudgetService.paceStatus(progress("55", 0.5f, planned = "20")))
    }
}
