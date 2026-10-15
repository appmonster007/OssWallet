package com.osscabinet.ossWallet.domain.service.goal

import com.osscabinet.ossWallet.domain.model.Goal
import com.osscabinet.ossWallet.domain.model.GoalProgress
import com.osscabinet.ossWallet.domain.model.GoalStatus
import java.math.BigDecimal
import java.math.RoundingMode
import java.time.Instant
import java.time.ZoneId
import java.time.temporal.ChronoUnit

object GoalService {

    /** Share of the goal-to-deadline window that must be funded before it counts as behind. */
    private const val PACE_TOLERANCE = 0.10f
    private const val DAYS_PER_MONTH = 30

    fun progress(goal: Goal, now: Long, zone: ZoneId = ZoneId.systemDefault()): GoalProgress {
        val target = goal.targetAmount
        val saved = goal.savedAmount
        val remaining = target.subtract(saved).max(BigDecimal.ZERO)
        val complete = target.signum() > 0 && saved >= target
        val fraction = when {
            target.signum() <= 0 -> 0f
            complete -> 1f
            else -> saved.max(BigDecimal.ZERO).divide(target, 4, RoundingMode.HALF_UP).toFloat()
        }

        val daysLeft = goal.deadline?.let {
            ChronoUnit.DAYS.between(
                Instant.ofEpochMilli(now).atZone(zone).toLocalDate(),
                Instant.ofEpochMilli(it).atZone(zone).toLocalDate()
            )
        }

        val requiredPerMonth = if (complete || daysLeft == null) null else {
            // Inside the final month the whole remainder is due, so never divide by < 1 month.
            val months = BigDecimal(daysLeft.coerceAtLeast(DAYS_PER_MONTH.toLong()))
                .divide(BigDecimal(DAYS_PER_MONTH), 4, RoundingMode.HALF_UP)
            remaining.divide(months, 2, RoundingMode.HALF_UP)
        }

        val status = when {
            complete -> GoalStatus.COMPLETED
            daysLeft == null -> GoalStatus.NO_DEADLINE
            daysLeft < 0 -> GoalStatus.OVERDUE
            elapsedFraction(goal, now)?.let { fraction < it - PACE_TOLERANCE } == true -> GoalStatus.BEHIND
            else -> GoalStatus.ON_TRACK
        }
        return GoalProgress(goal, fraction, remaining, daysLeft, requiredPerMonth, status)
    }

    /** Applies a contribution (negative = withdrawal), clamping at zero and stamping completion. */
    fun applyContribution(goal: Goal, amount: BigDecimal, now: Long): Goal {
        val saved = goal.savedAmount.add(amount).max(BigDecimal.ZERO)
        val complete = goal.targetAmount.signum() > 0 && saved >= goal.targetAmount
        return goal.copy(
            savedAmount = saved,
            completedAt = if (complete) goal.completedAt ?: now else null
        )
    }

    private fun elapsedFraction(goal: Goal, now: Long): Float? {
        val deadline = goal.deadline ?: return null
        val window = deadline - goal.createdAt
        if (window <= 0) return null
        return ((now - goal.createdAt).toFloat() / window.toFloat()).coerceIn(0f, 1f)
    }
}
