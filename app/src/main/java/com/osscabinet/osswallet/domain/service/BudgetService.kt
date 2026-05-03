package com.osscabinet.ossWallet.domain.service

import com.osscabinet.ossWallet.domain.model.*
import java.math.BigDecimal
import java.math.RoundingMode
import java.time.*
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
                Pair(start.atStartOfDay(zone).toInstant().toEpochMilli(), end.atTime(LocalTime.MAX).atZone(zone).toInstant().toEpochMilli())
            }
            BudgetPeriod.MONTHLY -> {
                val start = today.with(TemporalAdjusters.firstDayOfMonth())
                val end = today.with(TemporalAdjusters.lastDayOfMonth())
                Pair(start.atStartOfDay(zone).toInstant().toEpochMilli(), end.atTime(LocalTime.MAX).atZone(zone).toInstant().toEpochMilli())
            }
            BudgetPeriod.QUARTERLY -> {
                val firstMonthOfQuarter = ((today.monthValue - 1) / 3) * 3 + 1
                val start = today.withMonth(firstMonthOfQuarter).with(TemporalAdjusters.firstDayOfMonth())
                val lastMonthOfQuarter = firstMonthOfQuarter + 2
                val end = today.withMonth(lastMonthOfQuarter).with(TemporalAdjusters.lastDayOfMonth())
                Pair(start.atStartOfDay(zone).toInstant().toEpochMilli(), end.atTime(LocalTime.MAX).atZone(zone).toInstant().toEpochMilli())
            }
            BudgetPeriod.YEARLY -> {
                val start = today.with(TemporalAdjusters.firstDayOfYear())
                val end = today.with(TemporalAdjusters.lastDayOfYear())
                Pair(start.atStartOfDay(zone).toInstant().toEpochMilli(), end.atTime(LocalTime.MAX).atZone(zone).toInstant().toEpochMilli())
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
        val filteredTransactions = filterTransactionsForBudget(budget, transactions, now, allCategories)

        // Aggregation logic using currency conversion
        val netAmount = filteredTransactions.fold(BigDecimal.ZERO) { acc, tx ->
            val converted = CurrencyConversionService.convertToBaseCurrency(
                amount = tx.amount, // Use signed amount to correctly handle income vs expense
                fromCurrency = tx.currency,
                toCurrency = budget.currency,
                exchangeRates = exchangeRates
            )
            acc.add(converted)
        }

        // If it's an income-only budget, we track progress towards positive target.
        // Otherwise (expense or mixed), we track net outflow (negated sum).
        val onlyIncome = budget.trackTypes.size == 1 && budget.trackTypes.first() == BudgetTrackType.INCOME
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
        
        // Utilize TransactionFilter for standard-compliant filtering
        val filter = com.osscabinet.ossWallet.domain.model.TransactionFilter(
            startDate = start,
            endDate = end,
            accountIds = budget.accountIds,
            categoryIds = TransactionFilterService.expandCategoryIds(budget.categoryIds, allCategories),
            tags = budget.tags,
            transactionType = if (budget.trackTypes.size == 1) {
                budget.trackTypes.first().name.lowercase()
            } else null,
            excludeFuture = false // Budgets track the whole configured period
        )

        return TransactionFilterService.filter(transactions, filter)
    }

    fun buildProgress(budget: Budget, spent: BigDecimal, rolloverAmount: BigDecimal = BigDecimal.ZERO): BudgetProgress {
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
        allCategories: List<Category> = emptyList()
    ): BigDecimal {
        if (!budget.rollover) return BigDecimal.ZERO
        val previousNow = getPreviousPeriodMidpoint(budget, now)
        val previousSpent = calculateSpent(budget, transactions, exchangeRates, baseCurrency, precision, previousNow, allCategories)
        return budget.limitAmount.subtract(previousSpent).max(BigDecimal.ZERO)
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
