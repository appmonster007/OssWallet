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
import com.osscabinet.ossWallet.domain.model.TransactionType
import com.osscabinet.ossWallet.domain.model.isTransfer
import com.osscabinet.ossWallet.domain.model.settings.AccountingPerspective
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

    override suspend fun compute(
        input: CardInput,
        config: CategorySpendingTrendCardConfig
    ): CardRenderData.CategorySpendingTrend {
        val rawFilter = resolveEffectiveFilter(input.filter, config.filter, config.filterRules, input.screenOwnsTimeline)
        val excludeInternal = (config.perspective as? AccountingPerspective.CashFlow)?.excludeInternalTransfers ?: false
        val mergedFilter = (if (rawFilter.categoryIds.isEmpty()) rawFilter
            else rawFilter.copy(categoryIds = input.categoryHierarchy.expand(rawFilter.categoryIds))).copy(
            accountIds = rawFilter.accountIds.ifEmpty { input.effectiveAccountIds }
        )
        val (currentStart, currentEnd) = TimelineService.resolveBoundRange(mergedFilter.timelineRange, input.transactions)

        // Build category nature lookup from full categories list
        val categoryNatureById = input.categories.associate { it.id to it.nature }

        // Single-pass filter against the correct transaction source (includes archived if explicitly selected)
        val txSource = input.transactionsFor(mergedFilter)
        val filteredTx = TransactionFilterService.filter(txSource, mergedFilter, excludeInternal)

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

        val granularity = CardComputerUtils.resolveGranularity(currentStart, currentEnd)

        val rawBuckets = CardComputerUtils.generateBuckets(currentStart, currentEnd, granularity)

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

                val shouldInclude = when (config.perspective) {
                    is AccountingPerspective.Accounting -> when (config.selectedMetric) {
                        ChartMetric.NET -> true
                        ChartMetric.INCOME -> tx.transactionType == TransactionType.INCOME
                        ChartMetric.EXPENSE -> tx.transactionType == TransactionType.EXPENSE
                        ChartMetric.TRANSFER -> tx.transactionType.isTransfer
                    }
                    is AccountingPerspective.CashFlow -> when (config.selectedMetric) {
                        ChartMetric.NET -> true
                        ChartMetric.INCOME -> converted.signum() > 0
                        ChartMetric.EXPENSE -> converted.signum() < 0
                        ChartMetric.TRANSFER -> false
                    }
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
                    amount = amount.setScale(CurrencyService.precision, RoundingMode.HALF_UP)
                )
            }.sortedBy { it.amount }

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
}
