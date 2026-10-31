package com.osscabinet.osswallet.domain.service.goal

import com.osscabinet.ossWallet.domain.model.Goal
import com.osscabinet.ossWallet.domain.model.GoalStatus
import com.osscabinet.ossWallet.domain.service.goal.GoalService
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.math.BigDecimal
import java.time.ZoneOffset
import java.time.ZonedDateTime

class GoalServiceTest {

    private val zone = ZoneOffset.UTC
    private val day = 24L * 60 * 60 * 1000
    private val now = ZonedDateTime.of(2026, 10, 15, 12, 0, 0, 0, zone).toInstant().toEpochMilli()

    private fun goal(
        saved: String = "0", target: String = "1000", deadline: Long? = null,
        createdAt: Long = now - 30 * day, completedAt: Long? = null
    ) = Goal(
        id = 1, name = "Laptop", targetAmount = BigDecimal(target), savedAmount = BigDecimal(saved),
        currency = "USD", deadline = deadline, profileId = 1, createdAt = createdAt, completedAt = completedAt
    )

    @Test fun `fraction and remaining reflect saved amount`() {
        val p = GoalService.progress(goal(saved = "250"), now, zone)
        assertEquals(0.25f, p.fraction, 0.0001f)
        assertEquals(0, BigDecimal("750").compareTo(p.remaining))
    }

    @Test fun `reaching the target completes the goal and caps the fraction`() {
        val p = GoalService.progress(goal(saved = "1200"), now, zone)
        assertEquals(GoalStatus.COMPLETED, p.status)
        assertEquals(1f, p.fraction, 0f)
        assertEquals(0, BigDecimal.ZERO.compareTo(p.remaining))
        assertNull(p.requiredPerMonth)
    }

    @Test fun `no deadline means no pace or monthly requirement`() {
        val p = GoalService.progress(goal(saved = "100"), now, zone)
        assertEquals(GoalStatus.NO_DEADLINE, p.status)
        assertNull(p.daysLeft)
        assertNull(p.requiredPerMonth)
    }

    @Test fun `past deadline is overdue`() {
        val p = GoalService.progress(goal(saved = "100", deadline = now - 2 * day), now, zone)
        assertEquals(GoalStatus.OVERDUE, p.status)
        assertEquals(-2L, p.daysLeft)
    }

    @Test fun `required per month spreads the remainder over the months left`() {
        // 300 days left = 10 x 30-day months; 1000 target, 0 saved -> 100 / month.
        val p = GoalService.progress(goal(deadline = now + 300 * day), now, zone)
        assertEquals(0, BigDecimal("100.00").compareTo(p.requiredPerMonth))
    }

    @Test fun `inside the last month the whole remainder is due`() {
        val p = GoalService.progress(goal(saved = "400", deadline = now + 5 * day), now, zone)
        assertEquals(0, BigDecimal("600.00").compareTo(p.requiredPerMonth))
    }

    @Test fun `behind pace is detected against elapsed time`() {
        // Created 30d ago, deadline in 10d -> 75% of the window elapsed, only 10% saved.
        val p = GoalService.progress(goal(saved = "100", deadline = now + 10 * day), now, zone)
        assertEquals(GoalStatus.BEHIND, p.status)
    }

    @Test fun `on pace is on track`() {
        val p = GoalService.progress(goal(saved = "800", deadline = now + 10 * day), now, zone)
        assertEquals(GoalStatus.ON_TRACK, p.status)
    }

    @Test fun `contribution stamps completion once and withdrawal clears it`() {
        val done = GoalService.applyContribution(goal(saved = "900"), BigDecimal("100"), now)
        assertEquals(now, done.completedAt)
        // Further money does not move the original completion time.
        assertEquals(now, GoalService.applyContribution(done, BigDecimal("5"), now + day).completedAt)
        val reopened = GoalService.applyContribution(done, BigDecimal("-500"), now + day)
        assertNull(reopened.completedAt)
        assertEquals(0, BigDecimal("500").compareTo(reopened.savedAmount))
    }

    @Test fun `withdrawal never drops below zero`() {
        val g = GoalService.applyContribution(goal(saved = "50"), BigDecimal("-500"), now)
        assertEquals(0, BigDecimal.ZERO.compareTo(g.savedAmount))
        assertTrue(g.completedAt == null)
        assertNotNull(g)
    }
}
