package com.osscabinet.ossWallet.domain.service.management

import com.osscabinet.ossWallet.domain.model.Budget
import com.osscabinet.ossWallet.domain.model.BudgetAlertLevel
import com.osscabinet.ossWallet.domain.model.BudgetProgress
import com.osscabinet.ossWallet.domain.model.Category
import com.osscabinet.ossWallet.domain.model.Transaction
import com.osscabinet.ossWallet.domain.model.TransactionFilter
import com.osscabinet.ossWallet.domain.model.TransactionType
import com.osscabinet.ossWallet.domain.model.timeline.TimelineRange
import com.osscabinet.ossWallet.domain.service.currency.CurrencyService
import com.osscabinet.ossWallet.domain.service.filter.TransactionFilterService
import com.osscabinet.ossWallet.domain.service.timeline.TimelineService
import java.math.BigDecimal
import java.math.RoundingMode

object BudgetService {

    fun getCurrentPeriodRange(budget: Budget, now: Long): Pair<Long, Long> {
        val (start, end) = TimelineService.getDateRange(budget.timelineRange, referenceTime = now)
        return Pair(start ?: now, end ?: now)
    }

    fun calculateSpent(
        budget: Budget,
        transactions: List<Transaction>,
        exchangeRates: Map<String, BigDecimal>,
        baseCurrency: String,
        precision: Int,
        now: Long,
        allCategories: List<Category> = emptyList()
    ): BigDecimal {
        val filteredTransactions =
            filterTransactionsForBudget(budget, transactions, now, allCategories)

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
        // Otherwise (expense or mixed), we track net outflow (negated sum).
        val onlyIncome =
            budget.trackTypes.size == 1 && budget.trackTypes.first() == TransactionType.INCOME
        val spent = if (onlyIncome) netAmount else netAmount.negate()

        return spent.setScale(precision, RoundingMode.HALF_UP)
    }

    fun buildTransactionFilter(
        budget: Budget,
        now: Long,
        allCategories: List<Category> = emptyList()
    ): TransactionFilter {
        // AllTime has no defined start/end — keep it as AllTime so the filter engine
        // skips the date-range check entirely. Resolving it via getCurrentPeriodRange
        // collapses AllTime to (now, now), which matches nothing.
        val resolvedRange: TimelineRange = if (budget.timelineRange is TimelineRange.AllTime) {
            TimelineRange.AllTime
        } else {
            val (start, end) = getCurrentPeriodRange(budget, now)
            TimelineRange.CustomRange(start, end)
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
            timelineRange = resolvedRange,
            excludeFuture = false
        )
    }

    fun filterTransactionsForBudget(
        budget: Budget,
        transactions: List<Transaction>,
        now: Long,
        allCategories: List<Category> = emptyList()
    ): List<Transaction> {
        return TransactionFilterService.filterTransactions(
            transactions,
            buildTransactionFilter(budget, now, allCategories)
        )
    }

    fun buildProgress(
        budget: Budget,
        spent: BigDecimal,
        rolloverAmount: BigDecimal = BigDecimal.ZERO
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
            rolloverAmount = rolloverAmount
        )
    }

    fun calculateRollover(
        budget: Budget,
        transactions: List<Transaction>,
        exchangeRates: Map<String, BigDecimal>,
        baseCurrency: String,
        precision: Int,
        now: Long,
        allCategories: List<Category> = emptyList(),
        depth: Int = 0
    ): BigDecimal {
        if (!budget.rollover || depth >= 12) return BigDecimal.ZERO
        
        val previousRange = TimelineService.getPreviousRange(budget.timelineRange, referenceTime = now)
        val (prevStart, prevEnd) = previousRange
        
        // Midpoint of previous period to calculate spent
        val previousNow = if (prevStart != null && prevEnd != null) (prevStart + prevEnd) / 2 else now
        
        // Temporarily override budget's timelineRange to previous for spent calculation
        val previousBudget = budget.copy(timelineRange = TimelineRange.CustomRange(prevStart, prevEnd))
        val previousSpent = calculateSpent(previousBudget, transactions, exchangeRates, baseCurrency, precision, previousNow, allCategories)
        
        // Cumulative: Previous period's rollover affects its effective limit
        val previousRollover = calculateRollover(previousBudget, transactions, exchangeRates, baseCurrency, precision, previousNow, allCategories, depth + 1)
        val previousEffectiveLimit = budget.limitAmount.add(previousRollover)
        
        return previousEffectiveLimit.subtract(previousSpent).max(BigDecimal.ZERO)
    }
}
