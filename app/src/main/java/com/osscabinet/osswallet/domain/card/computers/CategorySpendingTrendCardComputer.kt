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
import com.osscabinet.ossWallet.domain.model.settings.ChartMetric
import com.osscabinet.ossWallet.domain.model.timeline.TimelineRange
import com.osscabinet.ossWallet.domain.service.aggregation.TransactionAggregationService
import com.osscabinet.ossWallet.domain.service.currency.CurrencyService
import com.osscabinet.ossWallet.domain.service.query.DatabaseQueryService
import com.osscabinet.ossWallet.domain.service.timeline.TimelineService
import java.math.BigDecimal
import java.math.RoundingMode
import java.time.Instant

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
        val now = System.currentTimeMillis()
        val granularity = CardComputerUtils.resolveGranularity(currentStart, currentEnd)
        // Cap chart at today — future buckets always show 0
        val rawBuckets = CardComputerUtils.generateBuckets(currentStart, minOf(currentEnd, now), granularity)

        val categoryTotalsMap = mutableMapOf<Long, BigDecimal>()

        val buckets = rawBuckets.map { (bucketStart, bucketEnd, label) ->
            val bucketFilter = mergedFilter.copy(
                timelineRange = TimelineRange.CustomRange(bucketStart, bucketEnd)
            )
            val summariesByCategory = TransactionAggregationService.getSummaryByCategoryAndCurrency(
                queryService, input.profileId, bucketFilter, excludeInternal
            ).getOrDefault(emptyMap())

            val amountsByCategory = mutableMapOf<Long, BigDecimal>()
            summariesByCategory.forEach { (catId, byCurrency) ->
                val nature = categoryNatureById[catId] ?: return@forEach
                if (config.selectedNatures.size < CategoryNature.entries.size && nature !in config.selectedNatures) return@forEach

                val summary = CurrencyService.calculateTotalFinancialSummary(byCurrency, input.targetCurrency, input.exchangeRates)
                val amount = when (config.perspective) {
                    is AccountingPerspective.Accounting -> when (config.selectedMetric) {
                        ChartMetric.NET -> summary.income.add(summary.expense).add(summary.transfer)
                        ChartMetric.INCOME -> summary.income
                        ChartMetric.EXPENSE -> summary.expense
                        ChartMetric.TRANSFER -> summary.transfer
                    }
                    is AccountingPerspective.CashFlow -> when (config.selectedMetric) {
                        ChartMetric.NET -> summary.income.add(summary.expense).add(summary.transferIn).add(summary.transferOut)
                        ChartMetric.INCOME -> summary.income.add(summary.transferIn)
                        ChartMetric.EXPENSE -> summary.expense.add(summary.transferOut)
                        ChartMetric.TRANSFER -> BigDecimal.ZERO
                    }
                }
                if (amount.signum() != 0) {
                    amountsByCategory[catId] = (amountsByCategory[catId] ?: BigDecimal.ZERO).add(amount)
                    categoryTotalsMap[catId] = (categoryTotalsMap[catId] ?: BigDecimal.ZERO).add(amount)
                }
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
