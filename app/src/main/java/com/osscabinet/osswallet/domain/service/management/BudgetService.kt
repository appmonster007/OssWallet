package com.osscabinet.ossWallet.domain.service.management

import com.osscabinet.ossWallet.domain.model.Budget
import com.osscabinet.ossWallet.domain.model.BudgetAlertLevel
import com.osscabinet.ossWallet.domain.model.BudgetPaceStatus
import com.osscabinet.ossWallet.domain.model.BudgetProgress
import com.osscabinet.ossWallet.domain.model.Category
import com.osscabinet.ossWallet.domain.model.Transaction
import com.osscabinet.ossWallet.domain.model.TransactionFilter
import com.osscabinet.ossWallet.domain.model.TransactionType
import com.osscabinet.ossWallet.domain.model.timeline.TimelineRange
import com.osscabinet.ossWallet.domain.service.currency.CurrencyService
import com.osscabinet.ossWallet.domain.service.currency.ExchangeRateSnapshot
import com.osscabinet.ossWallet.domain.service.filter.TransactionFilterService
import com.osscabinet.ossWallet.domain.service.timeline.TimelineService
import java.math.BigDecimal
import java.math.RoundingMode

object BudgetService {

    fun getCurrentPeriodRange(budget: Budget, now: Long): Pair<Long, Long> {
        val (start, end) = TimelineService.getDateRange(budget.timelineRange, referenceTime = now)
        return Pair(start ?: now, end ?: now)
    }

    /**
     * Start of the budget's current period for use as an identity (e.g. a notification dedupe
     * key). Unlike [getCurrentPeriodRange] it never falls back to [now] for open-ended ranges,
     * so it only changes when the period itself rolls over.
     */
    fun periodIdentity(budget: Budget, now: Long): Long =
        TimelineService.getDateRange(budget.timelineRange, referenceTime = now).first ?: 0L

    /**
     * Share (0..1) of the budget's current period that has passed at [now], or null when the budget
     * has no fixed period (All Time) or the period has no length.
     */
    fun elapsedFraction(budget: Budget, now: Long): Float? {
        if (budget.timelineRange is TimelineRange.AllTime) return null
        val (start, end) = getCurrentPeriodRange(budget, now)
        if (end <= start) return null
        return ((now - start).toFloat() / (end - start).toFloat()).coerceIn(0f, 1f)
    }

    /**
     * Over the limit wins, then spending clearly faster than the period (more than [PACE_TOLERANCE]
     * ahead of the elapsed share), then reaching the budget's own alert threshold.
     */
    fun paceStatus(progress: BudgetProgress): BudgetPaceStatus {
        val elapsed = progress.elapsedFraction
        return when {
            progress.alertLevel == BudgetAlertLevel.EXCEEDED -> BudgetPaceStatus.OVER
            elapsed != null && progress.percentage > elapsed + PACE_TOLERANCE -> BudgetPaceStatus.AHEAD_OF_PACE
            progress.alertLevel == BudgetAlertLevel.WARNING -> BudgetPaceStatus.NEAR_LIMIT
            else -> BudgetPaceStatus.ON_TRACK
        }
    }

    const val PACE_TOLERANCE = 0.10f

    fun calculateSpent(
        budget: Budget,
        transactions: List<Transaction>,
        exchangeRates: ExchangeRateSnapshot,
        baseCurrency: String,
        now: Long,
        allCategories: List<Category> = emptyList(),
        precision: Int = CurrencyService.precision,
        excludeFuture: Boolean = false
    ): BigDecimal {
        val filteredTransactions =
            filterTransactionsForBudget(budget, transactions, now, allCategories, excludeFuture)

        // Aggregation logic using currency conversion
        val netAmount = filteredTransactions.fold(BigDecimal.ZERO) { acc, tx ->
            val converted = CurrencyService.convertAmount(
                amount = tx.amount, // Use signed amount to correctly handle income vs expense
                fromCurrency = tx.currency,
                toCurrency = budget.currency,
                exchangeRates = exchangeRates,
                precision = 10 // Use high precision for intermediate sum
            )
            acc.add(converted)
        }

        // If it's an income-only budget, we track progress towards positive target.
        // Otherwise (expense or mixed), we track net outflow.
        val isIncomeOnly = budget.trackTypes.isNotEmpty() && budget.trackTypes.all { it == TransactionType.INCOME }
        val spent = if (isIncomeOnly) netAmount else netAmount.negate()

        return spent.setScale(precision, RoundingMode.HALF_UP)
    }

    fun buildTransactionFilter(
        budget: Budget,
        now: Long,
        allCategories: List<Category> = emptyList(),
        excludeFuture: Boolean = false
    ): TransactionFilter {
        // AllTime has no defined start/end — keep it as AllTime so the filter engine
        // skips the date-range check entirely. Resolving it via getCurrentPeriodRange
        // collapses AllTime to (now, now), which matches nothing.
        val resolvedRange: TimelineRange = if (budget.timelineRange is TimelineRange.AllTime) {
            budget.timelineRange.withExcludeFuture(excludeFuture)
        } else {
            val (start, end) = getCurrentPeriodRange(budget, now)
            TimelineRange.CustomRange(start, end, excludeFuture = excludeFuture)
        }

        val expandedCategoryIds = if (allCategories.isNotEmpty() && budget.categoryIds.isNotEmpty()) {
            TransactionFilterService.expandCategoryIds(budget.categoryIds, allCategories)
        } else budget.categoryIds

        return TransactionFilter(
            categoryIds = expandedCategoryIds,
            accountIds = budget.accountIds,
            tags = budget.tags,
            paymentMethods = budget.paymentMethods,
            statuses = budget.statuses,
            currencies = budget.currencies,
            linkedTransactionIds = budget.linkedTransactionIds,
            transactionTypes = budget.trackTypes,
            timelineRange = resolvedRange
        ).let { budget.advanced.applyTo(it).copy(matchAllTags = budget.matchAllTags) }
    }

    fun filterTransactionsForBudget(
        budget: Budget,
        transactions: List<Transaction>,
        now: Long,
        allCategories: List<Category> = emptyList(),
        excludeFuture: Boolean = false
    ): List<Transaction> {
        return TransactionFilterService.filterTransactions(
            transactions,
            buildTransactionFilter(budget, now, allCategories, excludeFuture)
        )
    }

    fun buildProgress(
        budget: Budget,
        spent: BigDecimal,
        rolloverAmount: BigDecimal = BigDecimal.ZERO,
        elapsedFraction: Float? = null
    ): BudgetProgress {
        val effective = budget.limitAmount.add(rolloverAmount)
        // Remaining balance = budget amount (effective) - total transactions amount after converting to base currency
        val remaining = effective.subtract(spent).max(BigDecimal.ZERO)
        val percentage = if (effective.compareTo(BigDecimal.ZERO) == 0) 0f
        else spent.divide(effective, 4, RoundingMode.HALF_UP).toFloat()

        val alertLevel = when {
            percentage >= 1.0f -> BudgetAlertLevel.EXCEEDED
            percentage >= budget.alertThreshold -> BudgetAlertLevel.WARNING
            else -> BudgetAlertLevel.NONE
        }
        return BudgetProgress(
            budget = budget,
            spent = spent,
            limit = effective,
            remaining = remaining,
            percentage = percentage,
            alertLevel = alertLevel,
            rolloverAmount = rolloverAmount,
            elapsedFraction = elapsedFraction
        )
    }

    fun calculateRollover(
        budget: Budget,
        transactions: List<Transaction>,
        exchangeRates: ExchangeRateSnapshot,
        baseCurrency: String,
        now: Long,
        allCategories: List<Category> = emptyList(),
        depth: Int = 0,
        precision: Int = CurrencyService.precision,
        excludeFuture: Boolean = false
    ): BigDecimal {
        if (!budget.rollover || depth >= 12) return BigDecimal.ZERO
        
        val previousRange = TimelineService.getPreviousRange(budget.timelineRange, referenceTime = now)
        val (prevStart, prevEnd) = previousRange
        
        // Midpoint of previous period to calculate spent
        val previousNow = if (prevStart != null && prevEnd != null) (prevStart + prevEnd) / 2 else now
        
        // Temporarily override budget's timelineRange to previous for spent calculation
        val previousBudget = budget.copy(timelineRange = TimelineRange.CustomRange(prevStart, prevEnd))
        val previousSpent = calculateSpent(previousBudget, transactions, exchangeRates, baseCurrency, previousNow, allCategories, precision, excludeFuture)
        
        // Cumulative: Previous period's rollover affects its effective limit
        val previousRollover = calculateRollover(previousBudget, transactions, exchangeRates, baseCurrency, previousNow, allCategories, depth + 1, precision, excludeFuture)
        val previousEffectiveLimit = budget.limitAmount.add(previousRollover)
        
        return previousEffectiveLimit.subtract(previousSpent).max(BigDecimal.ZERO)
    }

    fun remainingDailyAllowance(progress: BudgetProgress, now: Long): BigDecimal? {
        val budget = progress.budget
        if (budget.timelineRange is TimelineRange.AllTime) return null
        val (start, end) = getCurrentPeriodRange(budget, now)
        if (end <= now || end <= start) return null

        val remainingMs = end - now
        val remainingDays = (remainingMs / (1000L * 60 * 60 * 24)).coerceAtLeast(1)
        return progress.remaining.divide(BigDecimal(remainingDays), 2, RoundingMode.HALF_UP)
    }

    fun getPeriodHistory(
        budget: Budget,
        transactions: List<Transaction>,
        exchangeRates: ExchangeRateSnapshot,
        baseCurrency: String,
        now: Long,
        allCategories: List<Category> = emptyList(),
        count: Int = 5,
        precision: Int = CurrencyService.precision
    ): List<BudgetProgress> {
        if (budget.timelineRange is TimelineRange.AllTime) return emptyList()

        return (0 downTo -count).map { offset ->
            val shiftedRange = TimelineService.shiftRange(budget.timelineRange, offset)
            val (start, end) = TimelineService.getDateRange(shiftedRange, referenceTime = now)
            val evalBudget = if (start != null && end != null) {
                budget.copy(timelineRange = TimelineRange.CustomRange(start, end))
            } else budget.copy(timelineRange = shiftedRange)

            val rollover = if (budget.rollover) {
                calculateRollover(evalBudget, transactions, exchangeRates, baseCurrency, now, allCategories, precision = precision)
            } else BigDecimal.ZERO

            val spent = calculateSpent(evalBudget, transactions, exchangeRates, baseCurrency, now, allCategories, precision = precision)
            val elapsed = elapsedFraction(evalBudget, now)
            buildProgress(evalBudget, spent, rollover, elapsed)
        }
    }
}
