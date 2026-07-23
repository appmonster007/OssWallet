package com.osscabinet.ossWallet.domain.card.computers

import com.osscabinet.ossWallet.domain.card.CardComputerUtils
import com.osscabinet.ossWallet.domain.card.CardDataComputer
import com.osscabinet.ossWallet.domain.card.CardInput
import com.osscabinet.ossWallet.domain.card.CardRenderData
import com.osscabinet.ossWallet.domain.card.CardType
import com.osscabinet.ossWallet.domain.card.CategoryAmount
import com.osscabinet.ossWallet.domain.card.CategorySpendingBucket
import com.osscabinet.ossWallet.domain.card.CategorySummary
import com.osscabinet.ossWallet.domain.card.resolveEffectiveFilter
import com.osscabinet.ossWallet.domain.model.CategoryNature
import com.osscabinet.ossWallet.domain.model.settings.AccountingPerspective
import com.osscabinet.ossWallet.domain.model.settings.CategorySpendingTrendCardConfig
import com.osscabinet.ossWallet.domain.service.financial.extractFrom
import com.osscabinet.ossWallet.domain.model.timeline.TimelineRange
import com.osscabinet.ossWallet.domain.service.aggregation.TransactionAggregationService
import com.osscabinet.ossWallet.domain.service.financial.FinancialSummary
import com.osscabinet.ossWallet.domain.service.currency.CurrencyService
import com.osscabinet.ossWallet.domain.service.query.DatabaseQueryService
import com.osscabinet.ossWallet.domain.service.timeline.TimelineService
import java.math.BigDecimal
import java.math.RoundingMode
import java.time.Instant
import java.time.LocalDate
import java.time.format.DateTimeFormatter

class CategorySpendingTrendCardComputer(
    private val queryService: DatabaseQueryService
) : CardDataComputer<CategorySpendingTrendCardConfig, CardRenderData.CategorySpendingTrend> {

    override val supportedType = CardType.CATEGORY_SPENDING_TREND

    override suspend fun compute(
        input: CardInput,
        config: CategorySpendingTrendCardConfig
    ): CardRenderData.CategorySpendingTrend {
        val rawFilter = resolveEffectiveFilter(input.filter, config.filter, config.filterRules, input.screenOwnsTimeline)
        val excludeInternal = config.excludeInternalTransfers && config.perspective is AccountingPerspective.CashFlow
        val mergedFilter = (if (rawFilter.categoryIds.isEmpty()) rawFilter
            else rawFilter.copy(categoryIds = input.categoryHierarchy.expand(rawFilter.categoryIds))).copy(
            accountIds = rawFilter.accountIds.ifEmpty { input.effectiveAccountIds }
        )

        val (currentStart, currentEnd) = resolveTimeBounds(mergedFilter.timelineRange, input.profileId)
        val categoryNatureById = input.categories.associate { it.id to it.nature }
        val categoryById = input.categories.associateBy { it.id }
        val now = System.currentTimeMillis()
        val granularity = CardComputerUtils.resolveGranularity(currentStart, currentEnd)
        // Cap chart at today — future buckets always show 0
        val rawBuckets = CardComputerUtils.generateBuckets(currentStart, minOf(currentEnd, now), granularity)

        val categoryTotalsMap = mutableMapOf<Long, BigDecimal>()

        // Single DB call for the full range, partitioned by day — replaces one call per bucket.
        val fullRangeFilter = mergedFilter.copy(
            timelineRange = TimelineRange.CustomRange(currentStart, minOf(currentEnd, now))
        )
        val dailyData = TransactionAggregationService.getSummaryByCategoryAndCurrencyByDay(
            queryService, input.profileId, fullRangeFilter, excludeInternal
        ).getOrDefault(emptyMap())

        // Pre-assign each "YYYY-MM-DD" day key to a bucket index using sorted bucket starts.
        val zone = TimelineService.systemZoneId
        val dayFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")
        val bucketStartMillis = rawBuckets.map { it.first }

        val dayToBucketIdx: Map<String, Int> = dailyData.keys.mapNotNull { dayKey ->
            val dayStart = try {
                LocalDate.parse(dayKey, dayFormatter).atStartOfDay(zone).toInstant().toEpochMilli()
            } catch (_: Exception) { return@mapNotNull null }
            val idx = bucketStartMillis.indexOfLast { it <= dayStart }
            if (idx < 0) return@mapNotNull null
            val (_, bucketEnd, _) = rawBuckets[idx]
            if (dayStart > bucketEnd) return@mapNotNull null
            dayKey to idx
        }.toMap()

        // Group daily rows by bucket index.
        val byBucket: Map<Int, Map<Long, Map<String, FinancialSummary>>> =
            buildMap {
                dayToBucketIdx.forEach { (dayKey, bucketIdx) ->
                    val daySummaries = dailyData[dayKey] ?: return@forEach
                    val existing = getOrDefault(bucketIdx, emptyMap()).toMutableMap()
                    daySummaries.forEach { (catId, byCurrency) ->
                        val merged = existing.getOrDefault(catId, emptyMap()).toMutableMap()
                        byCurrency.forEach { (currency, summary) ->
                            val prev = merged[currency]
                            merged[currency] = if (prev == null) summary else prev + summary
                        }
                        existing[catId] = merged
                    }
                    put(bucketIdx, existing)
                }
            }

        val buckets = rawBuckets.mapIndexed { idx, (bucketStart, _, label) ->
            val summariesByCategory = byBucket[idx] ?: emptyMap()
            val amountsByCategory = mutableMapOf<Long, BigDecimal>()

            summariesByCategory.forEach { (catId, byCurrency) ->
                val nature = categoryNatureById[catId] ?: return@forEach
                if (config.selectedNatures.size < CategoryNature.entries.size && nature !in config.selectedNatures) return@forEach

                val summary = CurrencyService.calculateTotalFinancialSummary(byCurrency, input.targetCurrency, input.exchangeRates)
                val amount = config.selectedMetric.extractFrom(summary)
                if (amount.signum() != 0) {
                    amountsByCategory[catId] = (amountsByCategory[catId] ?: BigDecimal.ZERO).add(amount)
                    categoryTotalsMap[catId] = (categoryTotalsMap[catId] ?: BigDecimal.ZERO).add(amount)
                }
            }

            val categoryAmounts = amountsByCategory.mapNotNull { (catId, amount) ->
                val cat = categoryById[catId] ?: return@mapNotNull null
                CategoryAmount(
                    categoryId = catId,
                    categoryName = cat.name,
                    colorHex = cat.colorHex,
                    nature = cat.nature,
                    amount = amount.setScale(CurrencyService.precision, RoundingMode.HALF_UP)
                )
            }.sortedBy { it.amount }

            CategorySpendingBucket(label = label, epochMillis = bucketStart, categoryAmounts = categoryAmounts)
        }

        val activeCategories = categoryTotalsMap.mapNotNull { (catId, total) ->
            val cat = input.categories.find { it.id == catId } ?: return@mapNotNull null
            CategorySummary(
                categoryId = catId,
                name = cat.name,
                colorHex = cat.colorHex,
                nature = cat.nature,
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

    private suspend fun resolveTimeBounds(timelineRange: TimelineRange, profileId: Int): Pair<Long, Long> {
        // For explicit ranges (month, year, custom), derive bounds without hitting the DB.
        val (start, end) = TimelineService.getDateRange(timelineRange)
        if (start != null && end != null) return start to end

        // AllTime: query the DB for the earliest transaction so the chart spans the full history.
        val now = System.currentTimeMillis()
        val minTs = queryService.getMinTransactionTimestamp(profileId) ?: return now to now
        val zone = TimelineService.systemZoneId
        val resolvedStart = Instant.ofEpochMilli(minTs).atZone(zone).toLocalDate()
            .atStartOfDay(zone).toInstant().toEpochMilli()
        return resolvedStart to now
    }
}
