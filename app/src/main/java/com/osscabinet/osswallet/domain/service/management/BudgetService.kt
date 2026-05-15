package com.osscabinet.ossWallet.domain.service.management

import com.osscabinet.ossWallet.domain.model.Budget
import com.osscabinet.ossWallet.domain.model.BudgetAlertLevel
import com.osscabinet.ossWallet.domain.model.BudgetPeriod
import com.osscabinet.ossWallet.domain.model.BudgetProgress
import com.osscabinet.ossWallet.domain.model.Category
import com.osscabinet.ossWallet.domain.model.Transaction
import com.osscabinet.ossWallet.domain.model.TransactionFilter
import com.osscabinet.ossWallet.domain.model.TransactionType
import com.osscabinet.ossWallet.domain.model.timeline.TimelineRange
import com.osscabinet.ossWallet.domain.service.currency.CurrencyService
import com.osscabinet.ossWallet.domain.service.filter.TransactionFilterService
import java.math.BigDecimal
import java.math.RoundingMode
import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalTime
import java.time.ZoneId
import java.time.temporal.TemporalAdjusters

object BudgetService {

    fun getCurrentPeriodRange(budget: Budget, now: Long): Pair<Long, Long> {
        val zone = ZoneId.systemDefault()
        val today = Instant.ofEpochMilli(now).atZone(zone).toLocalDate()
        return when (budget.period) {
            BudgetPeriod.DAILY -> {
                val start = today.atStartOfDay(zone).toInstant().toEpochMilli()
                val end = today.atTime(LocalTime.MAX).atZone(zone).toInstant().toEpochMilli()
                Pair(start, end)
            }

            BudgetPeriod.WEEKLY -> {
                val start = today.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
                val end = start.plusDays(6)
                Pair(
                    start.atStartOfDay(zone).toInstant().toEpochMilli(),
                    end.atTime(LocalTime.MAX).atZone(zone).toInstant().toEpochMilli()
                )
            }

            BudgetPeriod.MONTHLY -> {
                val start = today.with(TemporalAdjusters.firstDayOfMonth())
                val end = today.with(TemporalAdjusters.lastDayOfMonth())
                Pair(
                    start.atStartOfDay(zone).toInstant().toEpochMilli(),
                    end.atTime(LocalTime.MAX).atZone(zone).toInstant().toEpochMilli()
                )
            }

            BudgetPeriod.QUARTERLY -> {
                val firstMonthOfQuarter = ((today.monthValue - 1) / 3) * 3 + 1
                val start =
                    today.withMonth(firstMonthOfQuarter).with(TemporalAdjusters.firstDayOfMonth())
                val lastMonthOfQuarter = firstMonthOfQuarter + 2
                val end =
                    today.withMonth(lastMonthOfQuarter).with(TemporalAdjusters.lastDayOfMonth())
                Pair(
                    start.atStartOfDay(zone).toInstant().toEpochMilli(),
                    end.atTime(LocalTime.MAX).atZone(zone).toInstant().toEpochMilli()
                )
            }

            BudgetPeriod.YEARLY -> {
                val start = today.with(TemporalAdjusters.firstDayOfYear())
                val end = today.with(TemporalAdjusters.lastDayOfYear())
                Pair(
                    start.atStartOfDay(zone).toInstant().toEpochMilli(),
                    end.atTime(LocalTime.MAX).atZone(zone).toInstant().toEpochMilli()
                )
            }

            BudgetPeriod.CUSTOM -> {
                val start = budget.customStartDate ?: now
                val end = budget.customEndDate ?: now
                Pair(start, end)
            }
        }
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

    fun filterTransactionsForBudget(
        budget: Budget,
        transactions: List<Transaction>,
        now: Long,
        allCategories: List<Category> = emptyList()
    ): List<Transaction> {
        val (start, end) = getCurrentPeriodRange(budget, now)

        val expandedCategoryIds = if (allCategories.isNotEmpty() && budget.categoryIds.isNotEmpty()) {
            TransactionFilterService.expandCategoryIds(budget.categoryIds, allCategories)
        } else budget.categoryIds

        val budgetFilter = TransactionFilter(
            categoryIds = expandedCategoryIds,
            accountIds = budget.accountIds,
            tags = budget.tags,
            paymentMethods = budget.paymentMethods,
            statuses = budget.statuses,
            currencies = budget.currencies,
            linkedTransactionIds = budget.linkedTransactionIds,
            transactionTypes = budget.trackTypes,
            timelineRange = TimelineRange.CustomRange(start, end),
            excludeFuture = false
        )

        return TransactionFilterService.filterTransactions(transactions, budgetFilter)
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
        
        val previousNow = getPreviousPeriodMidpoint(budget, now)
        val previousSpent = calculateSpent(budget, transactions, exchangeRates, baseCurrency, precision, previousNow, allCategories)
        
        // Cumulative: Previous period's rollover affects its effective limit
        val previousRollover = calculateRollover(budget, transactions, exchangeRates, baseCurrency, precision, previousNow, allCategories, depth + 1)
        val previousEffectiveLimit = budget.limitAmount.add(previousRollover)
        
        return previousEffectiveLimit.subtract(previousSpent).max(BigDecimal.ZERO)
    }

    private fun getPreviousPeriodMidpoint(budget: Budget, now: Long): Long {
        val zone = ZoneId.systemDefault()
        val today = Instant.ofEpochMilli(now).atZone(zone).toLocalDate()
        val previousDate = when (budget.period) {
            BudgetPeriod.DAILY -> today.minusDays(1)
            BudgetPeriod.WEEKLY -> today.minusWeeks(1)
            BudgetPeriod.MONTHLY -> today.minusMonths(1)
            BudgetPeriod.QUARTERLY -> today.minusMonths(3)
            BudgetPeriod.YEARLY -> today.minusYears(1)
            BudgetPeriod.CUSTOM -> today.minusMonths(1)
        }
        return previousDate.atStartOfDay(zone).toInstant().toEpochMilli()
    }
}
