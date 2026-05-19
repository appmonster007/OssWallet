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
import com.osscabinet.ossWallet.domain.model.settings.CategoryNatureFilter
import com.osscabinet.ossWallet.domain.model.settings.CategorySpendingTrendCardConfig
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
        val mergedFilter = resolveEffectiveFilter(input.filter, config.filter, config.filterRules, input.screenOwnsTimeline)
        val (currentStart, currentEnd) = TimelineService.getDateRange(mergedFilter.timelineRange)

        if (currentStart == null || currentEnd == null) {
            return CardRenderData.CategorySpendingTrend(
                config = config,
                buckets = emptyList(),
                granularity = CashFlowComparisonGranularity.DAY,
                activeCategories = emptyList(),
                baseCurrency = input.baseCurrency
            )
        }

        // Build category nature lookup from full categories list
        val categoryNatureById = input.categories.associate { it.id to it.nature }

        // Single-pass filter
        val filteredTx = TransactionFilterService.filter(input.transactions, mergedFilter)

        // Cap at today — future buckets always show 0
        val now = System.currentTimeMillis()
        val pastFilteredTx = filteredTx.filter { it.timestamp <= now }

        // Further filter by selected nature
        val natureTx = if (config.selectedNature == CategoryNatureFilter.ALL) {
            pastFilteredTx
        } else {
            val targetNature = when (config.selectedNature) {
                CategoryNatureFilter.MUST -> CategoryNature.MUST
                CategoryNatureFilter.NEED -> CategoryNature.NEED
                CategoryNatureFilter.WANT -> CategoryNature.WANT
                CategoryNatureFilter.ALL -> null
            }
            if (targetNature == null) pastFilteredTx
            else pastFilteredTx.filter { tx ->
                categoryNatureById[tx.categoryId] == targetNature
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

            // Group by categoryId, sum converted amounts
            val amountsByCategory = mutableMapOf<Long, BigDecimal>()
            bucketTx.forEach { tx ->
                val catId = tx.categoryId
                val converted = CurrencyService.convertAmount(
                    amount = tx.amount.abs(),
                    fromCurrency = tx.currency,
                    toCurrency = input.baseCurrency,
                    exchangeRates = input.exchangeRates,
                    precision = 10
                )
                amountsByCategory[catId] = (amountsByCategory[catId] ?: BigDecimal.ZERO) + converted
                categoryTotalsMap[catId] = (categoryTotalsMap[catId] ?: BigDecimal.ZERO) + converted
            }

            val categoryAmounts = amountsByCategory.mapNotNull { (catId, amount) ->
                val cat = input.categories.find { it.id == catId } ?: return@mapNotNull null
                CategoryAmount(
                    categoryId = catId,
                    categoryName = cat.name,
                    colorHex = cat.colorHex,
                    nature = cat.nature,
                    amount = amount.setScale(CurrencyService.precision, RoundingMode.HALF_UP)
                )
            }.sortedByDescending { it.amount }

            CategorySpendingBucket(
                label = label,
                epochMillis = bucketStart,
                categoryAmounts = categoryAmounts
            )
        }

        // Build active categories (those with any spending in the period)
        val activeCategories = categoryTotalsMap.mapNotNull { (catId, total) ->
            val cat = input.categories.find { it.id == catId } ?: return@mapNotNull null
            CategorySummary(
                categoryId = catId,
                name = cat.name,
                colorHex = cat.colorHex,
                nature = cat.nature,
                totalAmount = total.setScale(CurrencyService.precision, RoundingMode.HALF_UP)
            )
        }.sortedByDescending { it.totalAmount }

        return CardRenderData.CategorySpendingTrend(
            config = config,
            buckets = buckets,
            granularity = granularity,
            activeCategories = activeCategories,
            baseCurrency = input.baseCurrency
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
                var day = LocalDate.ofInstant(Instant.ofEpochMilli(rangeStart), zone)
                val lastDay = LocalDate.ofInstant(Instant.ofEpochMilli(rangeEnd), zone)
                while (!day.isAfter(lastDay)) {
                    val start = day.atStartOfDay(zone).toInstant().toEpochMilli()
                    val end = day.atTime(23, 59, 59, 999_000_000).atZone(zone).toInstant().toEpochMilli()
                    buckets.add(Triple(start, end, day.format(dayFormatter)))
                    day = day.plusDays(1)
                }
            }
            CashFlowComparisonGranularity.WEEK -> {
                var weekStart = LocalDate.ofInstant(Instant.ofEpochMilli(rangeStart), zone)
                    .with(DayOfWeek.MONDAY)
                    .let { if (it.atStartOfDay(zone).toInstant().toEpochMilli() > rangeStart) it.minusWeeks(1) else it }
                val rangeEndDate = LocalDate.ofInstant(Instant.ofEpochMilli(rangeEnd), zone)
                while (!weekStart.isAfter(rangeEndDate)) {
                    val weekEnd = weekStart.plusDays(6)
                    val start = weekStart.atStartOfDay(zone).toInstant().toEpochMilli()
                    val end = weekEnd.atTime(23, 59, 59, 999_000_000).atZone(zone).toInstant().toEpochMilli()
                    buckets.add(Triple(start, end, weekStart.format(dayFormatter)))
                    weekStart = weekStart.plusWeeks(1)
                }
            }
            CashFlowComparisonGranularity.MONTH -> {
                var month = LocalDate.ofInstant(Instant.ofEpochMilli(rangeStart), zone).withDayOfMonth(1)
                val lastMonth = LocalDate.ofInstant(Instant.ofEpochMilli(rangeEnd), zone).withDayOfMonth(1)
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
