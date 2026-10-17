package com.osscabinet.ossWallet.appfunctions

import com.osscabinet.ossWallet.domain.service.financial.FinancialSummary
import java.math.BigDecimal

/**
 * Shapes the app's own aggregates ([FinancialSummary], from TransactionAggregationService) into what an assistant
 * receives. Spending is negative in the database and shown positive here; money of different currencies is never added.
 */
object AssistantStatistics {
    const val TOP_ITEMS_PER_CURRENCY = 8
    const val MAX_TREND_POINTS = 60

    /** One slice of spending: a (resolved) label in one currency. */
    data class Slice(val currency: String, val label: String, val spent: BigDecimal)

    enum class Granularity { DAY, MONTH, YEAR }

    /**
     * Per-currency totals, with the comparison when [previous] is given. A currency with no income or spending
     * records is left out. The change is how much more (positive) or less (negative) was spent.
     */
    fun summaries(current: Map<String, FinancialSummary>, previous: Map<String, FinancialSummary>?): List<CurrencySummary> =
        current.mapNotNull { (currency, now) ->
            if (now.transactionCount == 0 && now.income.signum() == 0 && now.expense.signum() == 0) return@mapNotNull null
            val spent = now.expense.abs()
            val prior = previous?.get(currency)
            val priorSpent = previous?.let { prior?.expense?.abs() ?: BigDecimal.ZERO }
            CurrencySummary(
                currency = currency,
                income = AssistantSupport.money(now.income, currency),
                expense = AssistantSupport.money(spent, currency),
                net = AssistantSupport.money(now.income - spent, currency),
                transactionCount = now.transactionCount,
                previousIncome = previous?.let { AssistantSupport.money(prior?.income ?: BigDecimal.ZERO, currency) },
                previousExpense = priorSpent?.let { AssistantSupport.money(it, currency) },
                expenseChangePercent = priorSpent?.takeIf { it.signum() != 0 }?.let { AssistantSupport.percent(spent - it, it) }
            )
        }.sortedBy { it.currency }

    /**
     * Spending split into slices; slices with the same label merge. Each share is of its own currency's total spending,
     * and only the largest [TOP_ITEMS_PER_CURRENCY] per currency are kept.
     */
    fun breakdown(slices: List<Slice>): List<BreakdownItem> =
        slices.filter { it.spent.signum() != 0 }.groupBy { it.currency }.toSortedMap().flatMap { (currency, items) ->
            val total = items.fold(BigDecimal.ZERO) { acc, i -> acc + i.spent.abs() }
            items.groupBy { it.label }
                .map { (label, group) -> label to group.fold(BigDecimal.ZERO) { acc, i -> acc + i.spent.abs() } }
                .sortedByDescending { it.second }
                .take(TOP_ITEMS_PER_CURRENCY)
                .map { (label, amount) ->
                    BreakdownItem(currency, label, AssistantSupport.money(amount, currency), AssistantSupport.percent(amount, total))
                }
        }

    /** Slices from per-category-or-account summaries (currency -> summary), labelled by [label]. */
    fun slices(byKey: Map<String, Map<String, FinancialSummary>>, label: (String) -> String): List<Slice> =
        byKey.flatMap { (key, perCurrency) ->
            perCurrency.map { (currency, summary) -> Slice(currency, label(key), summary.expense.abs()) }
        }

    /**
     * Income and spending over time from per-day summaries (day "YYYY-MM-DD" -> currency -> summary), rolled up to
     * [granularity] and oldest first; only the most recent [MAX_TREND_POINTS] points are kept.
     */
    fun trend(byDay: Map<String, Map<String, FinancialSummary>>, granularity: Granularity): List<TrendPoint> {
        val rolled = linkedMapOf<Pair<String, String>, FinancialSummary>()
        byDay.forEach { (day, perCurrency) ->
            val bucket = bucketOf(day, granularity)
            perCurrency.forEach { (currency, summary) ->
                rolled.merge(bucket to currency, summary) { a, b -> a + b }
            }
        }
        return rolled.map { (key, summary) ->
            TrendPoint(
                label = key.first,
                currency = key.second,
                income = AssistantSupport.money(summary.income, key.second),
                expense = AssistantSupport.money(summary.expense.abs(), key.second)
            )
        }.sortedWith(compareBy({ it.label }, { it.currency })).takeLast(MAX_TREND_POINTS)
    }

    private fun bucketOf(day: String, granularity: Granularity): String = when (granularity) {
        Granularity.DAY -> day
        Granularity.MONTH -> day.take(7)
        Granularity.YEAR -> day.take(4)
    }

    /** Buckets that keep the trend readable: days for a few weeks, months for a few years, then years. */
    fun granularityFor(rangeMillis: LongRange?): Granularity {
        if (rangeMillis == null) return Granularity.MONTH
        val days = (rangeMillis.last - rangeMillis.first) / 86_400_000L + 1
        return when {
            days <= 45 -> Granularity.DAY
            days <= 1100 -> Granularity.MONTH
            else -> Granularity.YEAR
        }
    }
}
