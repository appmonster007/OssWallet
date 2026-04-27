package com.osscabinet.ossWallet.domain.model

import java.math.BigDecimal
import java.math.RoundingMode

/**
 * Advanced Budget model for financial planning.
 * Supports multi-filter scope, temporal logic, and intelligence metrics.
 */
data class Budget(
    val id: String,
    val name: String,
    val limit: BigDecimal,
    val spent: BigDecimal,
    val currency: String = "USD",
    val period: BudgetPeriod = BudgetPeriod.MONTHLY,
    val filter: TransactionFilter = TransactionFilter(),
    val alertThresholds: Set<Float> = setOf(0.5f, 0.8f, 1.0f),
    val isPredictiveAlertEnabled: Boolean = true,
    val startDate: Long? = null, // For CUSTOM period
    val endDate: Long? = null,   // For CUSTOM period
    val lastEvaluated: Long = System.currentTimeMillis()
) {
    val progress: Float
        get() = if (limit.signum() > 0) (spent.divide(limit, 4, RoundingMode.HALF_UP)).toFloat() else 0f

    val remaining: BigDecimal
        get() = (limit.subtract(spent)).max(BigDecimal.ZERO)

    val isOverBudget: Boolean
        get() = spent.compareTo(limit) > 0

    /**
     * Burn Rate: Daily safe spend based on remaining days in period.
     */
    fun calculateDailySafeSpend(daysRemaining: Int): BigDecimal {
        if (daysRemaining <= 0) return BigDecimal.ZERO
        return remaining.divide(BigDecimal(daysRemaining), 2, RoundingMode.HALF_UP)
    }

    /**
     * Predictive Risk: Flag high risk if spending velocity exceeds historical/linear expectation.
     */
    fun isHighRisk(daysElapsed: Int, totalDays: Int): Boolean {
        if (totalDays <= 0 || daysElapsed <= 0) return false
        val expectedProgress = daysElapsed.toFloat() / totalDays.toFloat()
        // If current progress exceeds expected progress by more than 15%, flag as high risk
        return progress > (expectedProgress + 0.15f)
    }
}

enum class BudgetPeriod {
    WEEKLY, MONTHLY, CUSTOM
}
