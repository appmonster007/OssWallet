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
        now: Long
    ): BigDecimal {
        val (start, end) = getCurrentPeriodRange(budget, now)
        val targetTypes = budget.trackTypes.map { if (it == BudgetTrackType.EXPENSE) "expense" else "income" }.toSet()

        return transactions
            .filter { tx ->
                tx.timestamp in start..end &&
                tx.transactionType in targetTypes &&
                (budget.categoryIds.isEmpty() || (tx.categoryId != null && tx.categoryId in budget.categoryIds)) &&
                (budget.accountIds.isEmpty() || tx.accountId in budget.accountIds) &&
                (budget.tags.isEmpty() || budget.tags.all { tag -> tag in tx.tags })
            }
            .fold(BigDecimal.ZERO) { acc, tx ->
                val converted = CurrencyService.convertAmount(
                    amount = tx.amount.abs(),
                    fromCurrency = tx.currency,
                    toCurrency = budget.currency,
                    exchangeRates = exchangeRates,
                    precision = precision + 4
                )
                acc.add(converted)
            }
            .setScale(precision, RoundingMode.HALF_UP)
    }

    fun buildProgress(budget: Budget, spent: BigDecimal, rolloverAmount: BigDecimal = BigDecimal.ZERO): BudgetProgress {
        val effective = budget.limitAmount.add(rolloverAmount)
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
        now: Long
    ): BigDecimal {
        if (!budget.rollover) return BigDecimal.ZERO
        val previousNow = getPreviousPeriodMidpoint(budget, now)
        val previousSpent = calculateSpent(budget, transactions, exchangeRates, baseCurrency, precision, previousNow)
        return budget.limitAmount.subtract(previousSpent).max(BigDecimal.ZERO)
    }

    private fun getPreviousPeriodMidpoint(budget: Budget, now: Long): Long {
        val zone = ZoneId.systemDefault()
        val today = Instant.ofEpochMilli(now).atZone(zone).toLocalDate()
        val previousDate = when (budget.period) {
            BudgetPeriod.WEEKLY -> today.minusWeeks(1)
            BudgetPeriod.MONTHLY -> today.minusMonths(1)
            BudgetPeriod.QUARTERLY -> today.minusMonths(3)
            BudgetPeriod.YEARLY -> today.minusYears(1)
            BudgetPeriod.CUSTOM -> today.minusMonths(1)
        }
        return previousDate.atStartOfDay(zone).toInstant().toEpochMilli()
    }
}
