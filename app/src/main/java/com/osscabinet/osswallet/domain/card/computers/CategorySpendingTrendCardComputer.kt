package com.osscabinet.ossWallet.domain.card.computers

import com.osscabinet.ossWallet.domain.card.CardDataComputer
import com.osscabinet.ossWallet.domain.card.CardInput
import com.osscabinet.ossWallet.domain.card.CardRenderData
import com.osscabinet.ossWallet.domain.card.CardType
import com.osscabinet.ossWallet.domain.card.CashFlowComparisonGranularity
import com.osscabinet.ossWallet.domain.card.CategoryAmount
import com.osscabinet.ossWallet.domain.card.CategorySpendingBucket
import com.osscabinet.ossWallet.domain.card.CategorySummary
import com.osscabinet.ossWallet.domain.card.resolveEffectiveFilter
import com.osscabinet.ossWallet.domain.model.CategoryNature
import com.osscabinet.ossWallet.domain.model.settings.CategorySpendingTrendCardConfig
import com.osscabinet.ossWallet.domain.model.settings.ChartMetric
import com.osscabinet.ossWallet.domain.model.timeline.TimelineRange
import com.osscabinet.ossWallet.domain.service.currency.CurrencyService
import com.osscabinet.ossWallet.domain.service.filter.TransactionFilterService
import com.osscabinet.ossWallet.domain.service.timeline.TimelineService
import java.math.BigDecimal
import java.math.RoundingMode
import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
import java.time.format.DateTimeFormatter

class CategorySpendingTrendCardComputer :
    CardDataComputer<CategorySpendingTrendCardConfig, CardRenderData.CategorySpendingTrend> {

    override val supportedType = CardType.CATEGORY_SPENDING_TREND

    private val dayFormatter = DateTimeFormatter.ofPattern("dd/MM")
    private val monthFormatter = DateTimeFormatter.ofPattern("MM/yyyy")

    override suspend fun compute(
        input: CardInput,
        config: CategorySpendingTrendCardConfig
    ): CardRenderData.CategorySpendingTrend {
        val rawFilter = resolveEffectiveFilter(input.filter, config.filter, config.filterRules, input.screenOwnsTimeline)
        val mergedFilter = if (rawFilter.categoryIds.isEmpty()) rawFilter
            else rawFilter.copy(categoryIds = input.categoryHierarchy.expand(rawFilter.categoryIds))
        val (currentStart, currentEnd) = TimelineService.resolveBoundRange(mergedFilter.timelineRange, input.transactions)

        // Build category nature lookup from full categories list
        val categoryNatureById = input.categories.associate { it.id to it.nature }

        // Single-pass filter against the correct transaction source (includes archived if explicitly selected)
        val txSource = input.transactionsFor(mergedFilter)
        val filteredTx = TransactionFilterService.filter(txSource, mergedFilter)

        // Cap at today — future buckets always show 0
        val now = System.currentTimeMillis()
        val pastFilteredTx = filteredTx.filter { it.timestamp <= now }

        // Further filter by selected nature
        val natureTx = if (config.selectedNatures.size == CategoryNature.entries.size) {
            pastFilteredTx
        } else {
            pastFilteredTx.filter { tx ->
                val nature = categoryNatureById[tx.categoryId]
                nature != null && nature in config.selectedNatures
            }
        }

        val spanDays = (currentEnd - currentStart) / 86_400_000L
        val granularity = when {
            spanDays <= 90L  -> CashFlowComparisonGranularity.DAY
            spanDays <= 365L -> CashFlowComparisonGranularity.WEEK
            else             -> CashFlowComparisonGranularity.MONTH
        }

        val rawBuckets = generateBuckets(currentStart, currentEnd, granularity)

        // Track total per category across all buckets (for active categories list)
        val categoryTotalsMap = mutableMapOf<Long, BigDecimal>()

        val buckets = rawBuckets.map { (bucketStart, bucketEnd, label) ->
            val bucketTx = natureTx.filter { tx ->
                tx.timestamp in bucketStart..bucketEnd
            }

            // Group by categoryId, sum converted amounts (properly signed)
            val amountsByCategory = mutableMapOf<Long, BigDecimal>()
            bucketTx.forEach { tx ->
                val catId = tx.categoryId
                val converted = CurrencyService.convertAmount(
                    amount = tx.amount, // Respect sign: expenses are negative, income is positive
                    fromCurrency = tx.currency,
                    toCurrency = input.targetCurrency,
                    exchangeRates = input.exchangeRates,
                    precision = 10
                )

                val shouldInclude = when (config.selectedMetric) {
                    ChartMetric.NET -> true
                    ChartMetric.INCOME -> converted.signum() > 0
                    ChartMetric.EXPENSE -> converted.signum() < 0
                }

                if (shouldInclude) {
                    amountsByCategory[catId] = (amountsByCategory[catId] ?: BigDecimal.ZERO) + converted
                    categoryTotalsMap[catId] = (categoryTotalsMap[catId] ?: BigDecimal.ZERO) + converted
                }
            }

            val categoryAmounts = amountsByCategory.mapNotNull { (catId, amount) ->
                val cat = input.categories.find { it.id == catId } ?: return@mapNotNull null
                
                CategoryAmount(
                    categoryId = catId,
                    categoryName = cat.name,
                    colorHex = cat.colorHex,
                    nature = cat.nature,
                    // Keep signed amount: negative for expenses (stack down), positive for income (stack up)
                    amount = amount.setScale(CurrencyService.precision, RoundingMode.HALF_UP)
                )
            }.sortedBy { it.amount } // Sort such that most negative (largest expenses) are prominent

            CategorySpendingBucket(
                label = label,
                epochMillis = bucketStart,
                categoryAmounts = categoryAmounts
            )
        }

        // Build active categories (those with any transaction in the period)
        val activeCategories = categoryTotalsMap.mapNotNull { (catId, total) ->
            val cat = input.categories.find { it.id == catId } ?: return@mapNotNull null
            
            CategorySummary(
                categoryId = catId,
                name = cat.name,
                colorHex = cat.colorHex,
                nature = cat.nature,
                // Keep signed total: negative shows as expense, positive as income
                totalAmount = total.setScale(CurrencyService.precision, RoundingMode.HALF_UP)
            )
        }.sortedBy { it.totalAmount }

        return CardRenderData.CategorySpendingTrend(
            config = config,
            buckets = buckets,
            granularity = granularity,
            activeCategories = activeCategories,
            currency = input.targetCurrency
        )
    }

    private fun generateBuckets(
        rangeStart: Long,
        rangeEnd: Long,
        granularity: CashFlowComparisonGranularity
    ): List<Triple<Long, Long, String>> {
        val zone = TimelineService.systemZoneId
        val buckets = mutableListOf<Triple<Long, Long, String>>()

        when (granularity) {
            CashFlowComparisonGranularity.DAY -> {
                var day = Instant.ofEpochMilli(rangeStart).atZone(zone).toLocalDate()
                val lastDay = Instant.ofEpochMilli(rangeEnd).atZone(zone).toLocalDate()
                while (!day.isAfter(lastDay)) {
                    val start = day.atStartOfDay(zone).toInstant().toEpochMilli()
                    val end = day.atTime(23, 59, 59, 999_000_000).atZone(zone).toInstant().toEpochMilli()
                    buckets.add(Triple(start, end, day.format(dayFormatter)))
                    day = day.plusDays(1)
                }
            }
            CashFlowComparisonGranularity.WEEK -> {
                var weekStart = Instant.ofEpochMilli(rangeStart).atZone(zone).toLocalDate()
                    .with(DayOfWeek.MONDAY)
                    .let { if (it.atStartOfDay(zone).toInstant().toEpochMilli() > rangeStart) it.minusWeeks(1) else it }
                val rangeEndDate = Instant.ofEpochMilli(rangeEnd).atZone(zone).toLocalDate()
                while (!weekStart.isAfter(rangeEndDate)) {
                    val weekEnd = weekStart.plusDays(6)
                    val start = weekStart.atStartOfDay(zone).toInstant().toEpochMilli()
                    val end = weekEnd.atTime(23, 59, 59, 999_000_000).atZone(zone).toInstant().toEpochMilli()
                    buckets.add(Triple(start, end, weekStart.format(dayFormatter)))
                    weekStart = weekStart.plusWeeks(1)
                }
            }
            CashFlowComparisonGranularity.MONTH -> {
                var month = Instant.ofEpochMilli(rangeStart).atZone(zone).toLocalDate().withDayOfMonth(1)
                val lastMonth = Instant.ofEpochMilli(rangeEnd).atZone(zone).toLocalDate().withDayOfMonth(1)
                while (!month.isAfter(lastMonth)) {
                    val start = month.atStartOfDay(zone).toInstant().toEpochMilli()
                    val end = month.plusMonths(1).atStartOfDay(zone).toInstant().toEpochMilli() - 1
                    buckets.add(Triple(start, end, month.format(monthFormatter)))
                    month = month.plusMonths(1)
                }
            }
        }

        return buckets
    }
}
