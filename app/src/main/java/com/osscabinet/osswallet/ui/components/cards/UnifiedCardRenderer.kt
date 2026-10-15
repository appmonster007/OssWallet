package com.osscabinet.ossWallet.ui.components.cards

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import com.osscabinet.ossWallet.domain.model.settings.ChartMetric
import com.osscabinet.ossWallet.domain.service.financial.CategoryTreeMetrics
import com.osscabinet.ossWallet.ui.components.cards.types.AccountHeroCard
import com.osscabinet.ossWallet.ui.components.cards.types.AccountsCard
import com.osscabinet.ossWallet.ui.components.cards.types.BudgetsCard
import com.osscabinet.ossWallet.ui.components.cards.types.CashFlowCard
import com.osscabinet.ossWallet.ui.components.cards.types.CashFlowComparisonCard
import com.osscabinet.ossWallet.ui.components.cards.types.CategoryChartData
import com.osscabinet.ossWallet.ui.components.cards.types.CategoryPieChartCard
import com.osscabinet.ossWallet.ui.components.cards.types.CategorySpendingTrendCard
import com.osscabinet.ossWallet.ui.components.cards.types.CategoryTreemapCard
import com.osscabinet.ossWallet.ui.components.cards.types.CurrencyBalancesCard
import com.osscabinet.ossWallet.ui.components.cards.types.IncomeExpenseBarCard
import com.osscabinet.ossWallet.ui.components.cards.types.GoalsCard
import com.osscabinet.ossWallet.ui.components.cards.types.LoansCard
import com.osscabinet.ossWallet.ui.components.cards.types.NetWorthCard
import com.osscabinet.ossWallet.ui.components.cards.types.PlannedPaymentsCard
import com.osscabinet.ossWallet.ui.components.cards.types.RecentActivitiesCard
import com.osscabinet.ossWallet.ui.components.cards.types.TagUpSetPlotCard
import com.osscabinet.ossWallet.ui.components.ShimmerByType
import com.osscabinet.ossWallet.ui.components.rememberShimmerBrush
import java.math.BigDecimal
import java.math.RoundingMode

@Composable
fun UnifiedCardRenderer(
    cardWithCallbacks: CardWithCallbacks,
    props: CardLayoutProps,
) {
    CompositionLocalProvider(LocalCardId provides cardWithCallbacks.cardId) {
        val effectiveProps = remember(props, cardWithCallbacks.baseFilter, cardWithCallbacks.filterRules, cardWithCallbacks.filterSheetEnabled) {
            props.applyCardOverrides(cardWithCallbacks)
        }

        when (cardWithCallbacks) {
            is CardWithCallbacks.NetWorth -> {
                val renderData = cardWithCallbacks.renderData
                NetWorthCard(
                    currentPeriodMetrics = renderData.currentPeriodMetrics,
                    baseCurrency = renderData.currency,
                    displayCurrency = renderData.currency,
                    config = renderData.config,
                    previousPeriodMetrics = renderData.previousPeriodMetrics,
                    trend = renderData.trend,
                    callbacks = cardWithCallbacks.callbacks,
                    layoutProps = effectiveProps
                )
            }

            is CardWithCallbacks.CurrencyBalances -> {
                val renderData = cardWithCallbacks.renderData
                CurrencyBalancesCard(
                    currencyTotals = renderData.items,
                    baseCurrency = renderData.currency,
                    exchangeRates = renderData.exchangeRates,
                    config = renderData.config,
                    callbacks = cardWithCallbacks.callbacks,
                    layoutProps = effectiveProps
                )
            }

            is CardWithCallbacks.CashFlow -> {
                val renderData = cardWithCallbacks.renderData
                CashFlowCard(
                    cashFlowResult = renderData.result,
                    baseCurrency = renderData.currency,
                    config = renderData.config,
                    previousResult = renderData.previousResult,
                    callbacks = cardWithCallbacks.callbacks,
                    layoutProps = effectiveProps
                )
            }

            is CardWithCallbacks.CategoryPieChart -> {
                val renderData = cardWithCallbacks.renderData
                val uiData = remember(renderData, renderData.config.selectedMetric) {
                    mapCategoryMetrics(
                        items = renderData.items,
                        selectedMetric = renderData.config.selectedMetric,
                        incomeTotal = renderData.incomeTotal,
                        expenseTotal = renderData.expenseTotal,
                        transferTotal = renderData.transferTotal,
                    )
                }
                CategoryPieChartCard(
                    config = renderData.config,
                    data = uiData,
                    currency = renderData.currency,
                    callbacks = cardWithCallbacks.callbacks,
                    layoutProps = effectiveProps
                )
            }

            is CardWithCallbacks.CategoryTreemap -> {
                val renderData = cardWithCallbacks.renderData
                val uiData = remember(renderData, renderData.config.selectedMetric) {
                    mapCategoryMetrics(
                        items = renderData.items,
                        selectedMetric = renderData.config.selectedMetric,
                        incomeTotal = renderData.incomeTotal,
                        expenseTotal = renderData.expenseTotal,
                        transferTotal = renderData.transferTotal,
                    )
                }
                CategoryTreemapCard(
                    config = renderData.config,
                    data = uiData,
                    currency = renderData.currency,
                    callbacks = cardWithCallbacks.callbacks,
                    layoutProps = effectiveProps
                )
            }

            is CardWithCallbacks.AccountHero -> {
                val renderData = cardWithCallbacks.renderData
                AccountHeroCard(
                    summary = renderData.summary,
                    baseCurrency = renderData.currency,
                    config = renderData.config,
                    callbacks = cardWithCallbacks.callbacks,
                    layoutProps = effectiveProps,
                    balanceHistory = renderData.summary.balanceHistory,
                )
            }

            is CardWithCallbacks.Accounts -> {
                val renderData = cardWithCallbacks.renderData
                AccountsCard(
                    accounts = renderData.updatedAccounts,
                    accountSummaries = renderData.accountSummaries,
                    previousSummaries = renderData.previousSummaries,
                    baseCurrency = renderData.currency,
                    config = renderData.config,
                    callbacks = cardWithCallbacks.callbacks,
                    layoutProps = effectiveProps
                )
            }

            is CardWithCallbacks.PlannedPayments -> {
                val renderData = cardWithCallbacks.renderData
                PlannedPaymentsCard(
                    items = renderData.items,
                    targetCurrency = renderData.currency,
                    exchangeRates = renderData.exchangeRates,
                    config = renderData.config,
                    previousItems = renderData.previousItems,
                    summary = renderData.summary,
                    callbacks = cardWithCallbacks.callbacks,
                    layoutProps = effectiveProps
                )
            }

            is CardWithCallbacks.Budgets -> {
                val renderData = cardWithCallbacks.renderData
                BudgetsCard(
                    items = renderData.items,
                    config = renderData.config,
                    previousItems = renderData.previousItems,
                    callbacks = cardWithCallbacks.callbacks,
                    layoutProps = effectiveProps
                )
            }

            is CardWithCallbacks.Goals -> {
                val renderData = cardWithCallbacks.renderData
                GoalsCard(
                    items = renderData.items,
                    config = renderData.config,
                    callbacks = cardWithCallbacks.callbacks,
                    layoutProps = effectiveProps
                )
            }

            is CardWithCallbacks.Loans -> {
                val renderData = cardWithCallbacks.renderData
                LoansCard(
                    items = renderData.items,
                    targetCurrency = renderData.currency,
                    exchangeRates = renderData.exchangeRates,
                    config = renderData.config,
                    callbacks = cardWithCallbacks.callbacks,
                    layoutProps = effectiveProps
                )
            }

            is CardWithCallbacks.RecentActivities -> {
                val renderData = cardWithCallbacks.renderData
                RecentActivitiesCard(
                    listState = com.osscabinet.ossWallet.ui.mapper.TransactionUiMapper.mapToUiState(
                        domainState = renderData.listState,
                        baseCurrency = renderData.currency
                    ),
                    config = renderData.config,
                    callbacks = cardWithCallbacks.callbacks,
                    baseCurrency = renderData.currency,
                    layoutProps = effectiveProps,
                )
            }

            is CardWithCallbacks.CashFlowComparison -> {
                CashFlowComparisonCard(
                    renderData = cardWithCallbacks.renderData,
                    callbacks = cardWithCallbacks.callbacks,
                    layoutProps = effectiveProps
                )
            }

            is CardWithCallbacks.IncomeExpenseBar -> {
                IncomeExpenseBarCard(
                    renderData = cardWithCallbacks.renderData,
                    callbacks = cardWithCallbacks.callbacks,
                    layoutProps = effectiveProps
                )
            }

            is CardWithCallbacks.CategorySpendingTrend -> {
                CategorySpendingTrendCard(
                    renderData = cardWithCallbacks.renderData,
                    callbacks = cardWithCallbacks.callbacks,
                    layoutProps = effectiveProps
                )
            }

            is CardWithCallbacks.TagUpSetPlot -> {
                TagUpSetPlotCard(
                    renderData = cardWithCallbacks.renderData,
                    callbacks = cardWithCallbacks.callbacks,
                    onRecordsClick = cardWithCallbacks.onRecordsClick,
                    layoutProps = effectiveProps
                )
            }

            is CardWithCallbacks.Loading -> {
                val brush = rememberShimmerBrush(label = "card_loading_${cardWithCallbacks.cardId}")
                ShimmerByType(type = cardWithCallbacks.cardType, brush = brush)
            }

            is CardWithCallbacks.ComputeError -> {
                AeonExpressiveCard(
                    title = cardWithCallbacks.cardType.displayName,
                    icon = cardWithCallbacks.cardType.toIcon(),
                    headerModifier = effectiveProps.headerModifier,
                    isWarning = true,
                ) {
                    CardErrorContent(cardWithCallbacks.message)
                }
            }
        }
    }
}

private fun mapCategoryMetrics(
    items: List<CategoryTreeMetrics>,
    selectedMetric: ChartMetric,
    incomeTotal: BigDecimal,
    expenseTotal: BigDecimal,
    transferTotal: BigDecimal,
): List<CategoryChartData> {
    val globalTotalAbs = when (selectedMetric) {
        ChartMetric.INCOME -> incomeTotal.abs()
        ChartMetric.EXPENSE -> expenseTotal.abs()
        ChartMetric.NET -> items.sumOf { it.metrics.netFlow.abs() }
        ChartMetric.TRANSFER -> transferTotal.abs()
        ChartMetric.TRANSFER_IN -> items.sumOf { it.metrics.transferIn.abs() }
        ChartMetric.TRANSFER_OUT -> items.sumOf { it.metrics.transferOut.abs() }
        ChartMetric.INFLOW -> items.sumOf { it.metrics.inflow.abs() }
        ChartMetric.OUTFLOW -> items.sumOf { it.metrics.outflow.abs() }
    }

    fun mapOne(metric: CategoryTreeMetrics): CategoryChartData? {
        val amount = when (selectedMetric) {
            ChartMetric.INCOME -> metric.metrics.income
            ChartMetric.EXPENSE -> metric.metrics.expense
            ChartMetric.NET -> metric.metrics.netFlow
            ChartMetric.TRANSFER -> metric.metrics.transfer
            ChartMetric.TRANSFER_IN -> metric.metrics.transferIn
            ChartMetric.TRANSFER_OUT -> metric.metrics.transferOut
            ChartMetric.INFLOW -> metric.metrics.inflow
            ChartMetric.OUTFLOW -> metric.metrics.outflow
        }
        val prevAmount = when (selectedMetric) {
            ChartMetric.INCOME -> metric.previousMetrics?.income
            ChartMetric.EXPENSE -> metric.previousMetrics?.expense
            ChartMetric.NET -> metric.previousMetrics?.netFlow
            ChartMetric.TRANSFER -> metric.previousMetrics?.transfer
            ChartMetric.TRANSFER_IN -> metric.previousMetrics?.transferIn
            ChartMetric.TRANSFER_OUT -> metric.previousMetrics?.transferOut
            ChartMetric.INFLOW -> metric.previousMetrics?.inflow
            ChartMetric.OUTFLOW -> metric.previousMetrics?.outflow
        }
        val subData = metric.subcategoryData.asSequence().mapNotNull { mapOne(it) }
            .sortedByDescending { it.amount.abs() }.toList()
        if (amount.signum() == 0 && subData.isEmpty() && (prevAmount == null || (prevAmount.signum() == 0))) {
            return null
        }
        val percentage = if (globalTotalAbs.signum() != 0) {
            amount.abs().divide(globalTotalAbs, 4, RoundingMode.HALF_UP).toFloat()
        } else 0f
        return CategoryChartData(
            category = metric.category,
            categoryId = metric.categoryId,
            amount = amount,
            colorHex = metric.colorHex,
            percentage = percentage,
            hasSubcategories = subData.isNotEmpty(),
            subcategoryCount = subData.size,
            subcategoryData = subData,
            previousAmount = prevAmount,
        )
    }

    return items.mapNotNull { mapOne(it) }.sortedByDescending { it.amount.abs() }
}
