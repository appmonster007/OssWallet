package com.osscabinet.ossWallet.domain.service.management

import com.osscabinet.ossWallet.domain.model.Budget
import com.osscabinet.ossWallet.domain.model.BudgetAlertLevel
import com.osscabinet.ossWallet.domain.model.BudgetPaceStatus
import com.osscabinet.ossWallet.domain.model.BudgetProgress
import com.osscabinet.ossWallet.domain.model.Category
import com.osscabinet.ossWallet.domain.model.EntryMode
import com.osscabinet.ossWallet.domain.model.PlannedPayment
import com.osscabinet.ossWallet.domain.model.PlannedPaymentStatus
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
        val wallNow = System.currentTimeMillis()
        // A transaction dated after today hasn't actually happened yet — it's a commitment,
        // not spend, so it's excluded here and surfaced separately via calculatePlannedAmount.
        val filteredTransactions =
            filterTransactionsForBudget(budget, transactions, now, allCategories, excludeFuture)
                .filter { it.timestamp <= wallNow }

        val netAmount = convertAndSum(filteredTransactions.map { it.amount to it.currency }, budget, exchangeRates)
        return applySignConvention(budget, netAmount).setScale(precision, RoundingMode.HALF_UP)
    }

    /**
     * Sum of this budget's future-dated transactions (timestamp after today) within its current
     * period — money already entered as a transaction but not yet actually spent.
     */
    fun calculateFutureTransactionsAmount(
        budget: Budget,
        transactions: List<Transaction>,
        exchangeRates: ExchangeRateSnapshot,
        baseCurrency: String,
        now: Long,
        allCategories: List<Category> = emptyList(),
        precision: Int = CurrencyService.precision
    ): BigDecimal {
        val wallNow = System.currentTimeMillis()
        val futureTransactions =
            filterTransactionsForBudget(budget, transactions, now, allCategories, excludeFuture = false)
                .filter { it.timestamp > wallNow }

        val netAmount = convertAndSum(futureTransactions.map { it.amount to it.currency }, budget, exchangeRates)
        return applySignConvention(budget, netAmount).setScale(precision, RoundingMode.HALF_UP)
    }

    /**
     * Active planned payments matching this budget's own scope (account/category/currency/tags/
     * track types) — the same scoping a real transaction would need to count toward [calculateSpent].
     */
    fun filterPlannedPaymentsForBudget(
        budget: Budget,
        plannedPayments: List<PlannedPayment>,
        allCategories: List<Category> = emptyList()
    ): List<PlannedPayment> {
        val categoryIdSet = if (allCategories.isNotEmpty() && budget.categoryIds.isNotEmpty()) {
            TransactionFilterService.expandCategoryIds(budget.categoryIds, allCategories)
        } else budget.categoryIds

        return plannedPayments.filter { payment ->
            payment.status == PlannedPaymentStatus.ACTIVE &&
                (budget.accountIds.isEmpty() || payment.accountId in budget.accountIds) &&
                (categoryIdSet.isEmpty() || payment.categoryId in categoryIdSet) &&
                (budget.currencies.isEmpty() || payment.currency in budget.currencies) &&
                matchesTags(budget, payment.tags) &&
                matchesTrackType(budget, payment.entryMode)
        }
    }

    /**
     * Sum of occurrences (of planned payments matching this budget's scope) due within the
     * budget's current period. Null for All Time budgets, which have no period to bound this to.
     */
    fun calculatePlannedPaymentsAmount(
        budget: Budget,
        plannedPayments: List<PlannedPayment>,
        exchangeRates: ExchangeRateSnapshot,
        baseCurrency: String,
        now: Long,
        allCategories: List<Category> = emptyList(),
        precision: Int = CurrencyService.precision
    ): BigDecimal {
        if (budget.timelineRange is TimelineRange.AllTime) {
            return BigDecimal.ZERO.setScale(precision, RoundingMode.HALF_UP)
        }

        val (periodStart, periodEnd) = getCurrentPeriodRange(budget, now)
        val matching = filterPlannedPaymentsForBudget(budget, plannedPayments, allCategories)
        val occurrences = PlannedPaymentService.generateUpcoming(matching, periodStart, periodEnd)
            .filter { it.scheduledDate in periodStart..periodEnd }

        val signedAmounts = occurrences.map { item ->
            val payment = item.plannedPayment
            val signedMagnitude =
                if (payment.entryMode == EntryMode.INCOME) payment.amount else payment.amount.negate()
            signedMagnitude to payment.currency
        }

        val netAmount = convertAndSum(signedAmounts, budget, exchangeRates)
        return applySignConvention(budget, netAmount).setScale(precision, RoundingMode.HALF_UP)
    }

    /** [calculateFutureTransactionsAmount] + [calculatePlannedPaymentsAmount] — the budget's full "committed but not yet spent" figure. */
    fun calculatePlannedAmount(
        budget: Budget,
        transactions: List<Transaction>,
        plannedPayments: List<PlannedPayment>,
        exchangeRates: ExchangeRateSnapshot,
        baseCurrency: String,
        now: Long,
        allCategories: List<Category> = emptyList(),
        precision: Int = CurrencyService.precision
    ): BigDecimal {
        val future = calculateFutureTransactionsAmount(
            budget, transactions, exchangeRates, baseCurrency, now, allCategories, precision
        )
        val planned = calculatePlannedPaymentsAmount(
            budget, plannedPayments, exchangeRates, baseCurrency, now, allCategories, precision
        )
        return future.add(planned).setScale(precision, RoundingMode.HALF_UP)
    }

    private fun convertAndSum(
        amounts: List<Pair<BigDecimal, String>>,
        budget: Budget,
        exchangeRates: ExchangeRateSnapshot
    ): BigDecimal = amounts.fold(BigDecimal.ZERO) { acc, (amount, currency) ->
        acc.add(
            CurrencyService.convertAmount(
                amount = amount,
                fromCurrency = currency,
                toCurrency = budget.currency,
                exchangeRates = exchangeRates,
                precision = 10 // Use high precision for intermediate sum
            )
        )
    }

    // If it's an income-only budget, we track progress towards a positive target.
    // Otherwise (expense or mixed), we track net outflow.
    private fun applySignConvention(budget: Budget, netAmount: BigDecimal): BigDecimal {
        val isIncomeOnly = budget.trackTypes.isNotEmpty() && budget.trackTypes.all { it == TransactionType.INCOME }
        return if (isIncomeOnly) netAmount else netAmount.negate()
    }

    private fun matchesTags(budget: Budget, tags: Set<String>): Boolean =
        if (budget.matchAllTags) tags.containsAll(budget.tags) else budget.tags.isEmpty() || tags.any { it in budget.tags }

    private fun matchesTrackType(budget: Budget, entryMode: EntryMode): Boolean {
        if (budget.trackTypes.isEmpty()) return true
        return when (entryMode) {
            EntryMode.EXPENSE -> TransactionType.EXPENSE in budget.trackTypes
            EntryMode.INCOME -> TransactionType.INCOME in budget.trackTypes
            // A transfer's direction isn't known until it's confirmed into real inflow/outflow
            // legs, so it can't be matched against trackTypes ahead of time.
            EntryMode.TRANSFER -> false
        }
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
        elapsedFraction: Float? = null,
        plannedAmount: BigDecimal = BigDecimal.ZERO
    ): BudgetProgress {
        val effective = budget.limitAmount.add(rolloverAmount)
        // Remaining balance = budget amount (effective) - total transactions amount after converting to base currency
        // Planned/future amounts are deliberately excluded from both — they're shown
        // separately so a committed-but-not-yet-spent payment doesn't trigger a false alert.
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
            elapsedFraction = elapsedFraction,
            plannedAmount = plannedAmount
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

    fun getPeriodProgressForOffset(
        budget: Budget,
        transactions: List<Transaction>,
        exchangeRates: ExchangeRateSnapshot,
        baseCurrency: String,
        now: Long,
        offset: Int,
        allCategories: List<Category> = emptyList(),
        precision: Int = CurrencyService.precision,
        plannedPayments: List<PlannedPayment> = emptyList()
    ): BudgetProgress {
        if (budget.timelineRange is TimelineRange.AllTime) {
            val spent = calculateSpent(budget, transactions, exchangeRates, baseCurrency, now, allCategories, precision = precision)
            return buildProgress(budget, spent)
        }

        val shiftedRange = TimelineService.shiftRange(budget.timelineRange, offset)
        val (start, end) = TimelineService.getDateRange(shiftedRange, referenceTime = now)
        val evalBudget = if (start != null && end != null) {
            budget.copy(timelineRange = TimelineRange.CustomRange(start, end))
        } else budget.copy(timelineRange = shiftedRange)

        val rollover = if (budget.rollover) {
            calculateRollover(evalBudget, transactions, exchangeRates, baseCurrency, now, allCategories, precision = precision)
        } else BigDecimal.ZERO

        val spent = calculateSpent(evalBudget, transactions, exchangeRates, baseCurrency, now, allCategories, precision = precision)
        val planned = calculatePlannedAmount(
            evalBudget, transactions, plannedPayments, exchangeRates, baseCurrency, now, allCategories, precision
        )
        val elapsed = elapsedFraction(evalBudget, now)
        return buildProgress(evalBudget, spent, rollover, elapsed, planned)
    }

    fun getPeriodHistory(
        budget: Budget,
        transactions: List<Transaction>,
        exchangeRates: ExchangeRateSnapshot,
        baseCurrency: String,
        now: Long,
        allCategories: List<Category> = emptyList(),
        offset: Int = 0,
        count: Int = 5,
        precision: Int = CurrencyService.precision,
        plannedPayments: List<PlannedPayment> = emptyList()
    ): List<BudgetProgress> {
        if (budget.timelineRange is TimelineRange.AllTime) return emptyList()

        val endOffset = offset
        val startOffset = offset - count
        return (startOffset..endOffset).map { targetOffset ->
            getPeriodProgressForOffset(
                budget = budget,
                transactions = transactions,
                exchangeRates = exchangeRates,
                baseCurrency = baseCurrency,
                now = now,
                offset = targetOffset,
                allCategories = allCategories,
                precision = precision,
                plannedPayments = plannedPayments
            )
        }
    }
}
