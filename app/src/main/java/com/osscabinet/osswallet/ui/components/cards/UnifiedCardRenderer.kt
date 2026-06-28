package com.osscabinet.ossWallet.ui.components.cards

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import com.osscabinet.ossWallet.domain.model.TransactionFilter
import com.osscabinet.ossWallet.domain.model.settings.ChartMetric
import com.osscabinet.ossWallet.domain.service.financial.CategoryTreeMetrics
import java.math.BigDecimal
import java.math.RoundingMode
import com.osscabinet.ossWallet.ui.components.toIcon
import com.osscabinet.ossWallet.ui.components.cards.types.*

@Composable
fun UnifiedCardRenderer(
    cardWithCallbacks: CardWithCallbacks,
    modifier: Modifier = Modifier,
    headerModifier: Modifier = Modifier,
    filterContext: FilterSelectionContext = FilterSelectionContext(),
    inheritedFilter: TransactionFilter = TransactionFilter(),
    screenOwnsTimeline: Boolean = false,
    animate: Boolean = false,
    scrollToId: String? = null,
    highlightedId: String? = null,
    onScrollComplete: () -> Unit = {}
) {
    when (cardWithCallbacks) {
        is CardWithCallbacks.NetWorth -> {
            val renderData = cardWithCallbacks.renderData
            NetWorthCard(
                currentPeriodMetrics = renderData.currentPeriodMetrics,
                baseCurrency = renderData.currency,
                displayCurrency = renderData.currency,
                config = renderData.config,
                previousPeriodMetrics = renderData.previousPeriodMetrics,
                callbacks = cardWithCallbacks.callbacks,
                filterContext = filterContext,
                baseFilter = cardWithCallbacks.baseFilter,
                inheritedFilter = inheritedFilter,
                filterRules = cardWithCallbacks.filterRules,
                screenOwnsTimeline = screenOwnsTimeline,
                showFilterButton = cardWithCallbacks.filterSheetEnabled,
                modifier = modifier.fillMaxWidth(),
                headerModifier = headerModifier,
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
                filterContext = filterContext,
                baseFilter = cardWithCallbacks.baseFilter,
                inheritedFilter = inheritedFilter,
                filterRules = cardWithCallbacks.filterRules,
                screenOwnsTimeline = screenOwnsTimeline,
                showFilterButton = cardWithCallbacks.filterSheetEnabled,
                modifier = modifier.fillMaxWidth(),
                headerModifier = headerModifier
            )
        }

        is CardWithCallbacks.CashFlow -> {
            val renderData = cardWithCallbacks.renderData
            CashFlowCard(
                cashFlowResult = renderData.result,
                baseCurrency = renderData.currency,
                config = renderData.config,
                callbacks = cardWithCallbacks.callbacks,
                filterContext = filterContext,
                baseFilter = cardWithCallbacks.baseFilter,
                inheritedFilter = inheritedFilter,
                filterRules = cardWithCallbacks.filterRules,
                screenOwnsTimeline = screenOwnsTimeline,
                showFilterButton = cardWithCallbacks.filterSheetEnabled,
                modifier = modifier.fillMaxWidth(),
                headerModifier = headerModifier
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
                animate = animate,
                callbacks = cardWithCallbacks.callbacks,
                filterContext = filterContext,
                baseFilter = cardWithCallbacks.baseFilter,
                inheritedFilter = inheritedFilter,
                filterRules = cardWithCallbacks.filterRules,
                screenOwnsTimeline = screenOwnsTimeline,
                modifier = modifier.fillMaxWidth(),
                headerModifier = headerModifier,
                showFilterButton = cardWithCallbacks.filterSheetEnabled
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
                animate = animate,
                callbacks = cardWithCallbacks.callbacks,
                filterContext = filterContext,
                baseFilter = cardWithCallbacks.baseFilter,
                inheritedFilter = inheritedFilter,
                filterRules = cardWithCallbacks.filterRules,
                screenOwnsTimeline = screenOwnsTimeline,
                modifier = modifier.fillMaxWidth(),
                headerModifier = headerModifier,
                showFilterButton = cardWithCallbacks.filterSheetEnabled
            )
        }

        is CardWithCallbacks.AccountHero -> {
            val renderData = cardWithCallbacks.renderData
            val cb = cardWithCallbacks.callbacks
            AccountHeroCard(
                summary = renderData.summary,
                baseCurrency = renderData.currency,
                config = renderData.config,
                callbacks = cb,
                filterRules = cardWithCallbacks.filterRules,
                baseFilter = cardWithCallbacks.baseFilter,
                onFilterChange = { cb.onFilterChange.invoke(renderData.config.cardId, it) },
                onSaveFilterClick = cb.onSaveFilterClick,
                filterContext = filterContext,
                inheritedFilter = inheritedFilter,
                screenOwnsTimeline = screenOwnsTimeline,
                showFilterButton = cardWithCallbacks.filterSheetEnabled,
                modifier = modifier.fillMaxWidth(),
                headerModifier = headerModifier
            )
        }

        is CardWithCallbacks.Accounts -> {
            val renderData = cardWithCallbacks.renderData
            AccountsCard(
                accounts = renderData.updatedAccounts,
                accountSummaries = renderData.accountSummaries,
                baseCurrency = renderData.currency,
                config = renderData.config,
                callbacks = cardWithCallbacks.callbacks,
                filterContext = filterContext,
                baseFilter = cardWithCallbacks.baseFilter,
                inheritedFilter = inheritedFilter,
                filterRules = cardWithCallbacks.filterRules,
                screenOwnsTimeline = screenOwnsTimeline,
                showFilterButton = cardWithCallbacks.filterSheetEnabled,
                modifier = modifier.fillMaxWidth(),
                headerModifier = headerModifier.fillMaxWidth(),
            )
        }

        is CardWithCallbacks.PlannedPayments -> {
            val renderData = cardWithCallbacks.renderData
            PlannedPaymentsCard(
                upcoming = renderData.upcoming,
                overdue = renderData.overdue,
                targetCurrency = renderData.currency,
                exchangeRates = renderData.exchangeRates,
                config = renderData.config,
                callbacks = cardWithCallbacks.callbacks,
                filterContext = filterContext,
                filterRules = cardWithCallbacks.filterRules,
                baseFilter = cardWithCallbacks.baseFilter,
                inheritedFilter = inheritedFilter,
                screenOwnsTimeline = screenOwnsTimeline,
                showFilterButton = cardWithCallbacks.filterSheetEnabled,
                modifier = modifier.fillMaxWidth(),
                headerModifier = headerModifier
            )
        }

        is CardWithCallbacks.Budgets -> {
            val renderData = cardWithCallbacks.renderData
            BudgetsCard(
                items = renderData.items,
                config = renderData.config,
                callbacks = cardWithCallbacks.callbacks,
                filterContext = filterContext,
                filterRules = cardWithCallbacks.filterRules,
                baseFilter = cardWithCallbacks.baseFilter,
                inheritedFilter = inheritedFilter,
                screenOwnsTimeline = screenOwnsTimeline,
                showFilterButton = cardWithCallbacks.filterSheetEnabled,
                modifier = modifier.fillMaxWidth(),
                headerModifier = headerModifier
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
                filterContext = filterContext,
                filterRules = cardWithCallbacks.filterRules,
                baseFilter = cardWithCallbacks.baseFilter,
                inheritedFilter = inheritedFilter,
                screenOwnsTimeline = screenOwnsTimeline,
                showFilterButton = cardWithCallbacks.filterSheetEnabled,
                modifier = modifier.fillMaxWidth(),
                headerModifier = headerModifier
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
                filterContext = filterContext,
                baseFilter = cardWithCallbacks.baseFilter,
                inheritedFilter = inheritedFilter,
                filterRules = cardWithCallbacks.filterRules,
                screenOwnsTimeline = screenOwnsTimeline,
                showFilterButton = cardWithCallbacks.filterSheetEnabled,
                modifier = modifier.fillMaxWidth(),
                headerModifier = headerModifier,
                scrollToId = scrollToId,
                highlightedId = highlightedId,
                onScrollComplete = onScrollComplete
            )
        }

        is CardWithCallbacks.CashFlowComparison -> {
            CashFlowComparisonCard(
                renderData = cardWithCallbacks.renderData,
                callbacks = cardWithCallbacks.callbacks,
                filterContext = filterContext,
                baseFilter = cardWithCallbacks.baseFilter,
                inheritedFilter = inheritedFilter,
                filterRules = cardWithCallbacks.filterRules,
                screenOwnsTimeline = screenOwnsTimeline,
                showFilterButton = cardWithCallbacks.filterSheetEnabled,
                animate = animate,
                modifier = modifier.fillMaxWidth(),
                headerModifier = headerModifier
            )
        }

        is CardWithCallbacks.IncomeExpenseBar -> {
            IncomeExpenseBarCard(
                renderData = cardWithCallbacks.renderData,
                callbacks = cardWithCallbacks.callbacks,
                filterContext = filterContext,
                baseFilter = cardWithCallbacks.baseFilter,
                inheritedFilter = inheritedFilter,
                filterRules = cardWithCallbacks.filterRules,
                screenOwnsTimeline = screenOwnsTimeline,
                showFilterButton = cardWithCallbacks.filterSheetEnabled,
                animate = animate,
                modifier = modifier.fillMaxWidth(),
                headerModifier = headerModifier
            )
        }

        is CardWithCallbacks.CategorySpendingTrend -> {
            CategorySpendingTrendCard(
                renderData = cardWithCallbacks.renderData,
                callbacks = cardWithCallbacks.callbacks,
                filterContext = filterContext,
                baseFilter = cardWithCallbacks.baseFilter,
                inheritedFilter = inheritedFilter,
                filterRules = cardWithCallbacks.filterRules,
                screenOwnsTimeline = screenOwnsTimeline,
                showFilterButton = cardWithCallbacks.filterSheetEnabled,
                animate = animate,
                modifier = modifier.fillMaxWidth(),
                headerModifier = headerModifier
            )
        }

        is CardWithCallbacks.TagUpSetPlot -> {
            TagUpSetPlotCard(
                renderData = cardWithCallbacks.renderData,
                callbacks = cardWithCallbacks.callbacks,
                onRecordsClick = cardWithCallbacks.onRecordsClick,
                filterContext = filterContext,
                baseFilter = cardWithCallbacks.baseFilter,
                inheritedFilter = inheritedFilter,
                filterRules = cardWithCallbacks.filterRules,
                screenOwnsTimeline = screenOwnsTimeline,
                showFilterButton = cardWithCallbacks.filterSheetEnabled,
                modifier = modifier.fillMaxWidth(),
                headerModifier = headerModifier
            )
        }

        is CardWithCallbacks.Loading -> {
            AeonExpressiveCard(
                title = cardWithCallbacks.cardType.displayName,
                icon = cardWithCallbacks.cardType.toIcon(),
                headerModifier = headerModifier,
            ) {
                CardContentShimmer()
            }
        }

        is CardWithCallbacks.ComputeError -> {
            AeonExpressiveCard(
                title = cardWithCallbacks.cardType.displayName,
                icon = cardWithCallbacks.cardType.toIcon(),
                headerModifier = headerModifier,
                isWarning = true,
            ) {
                CardErrorContent(cardWithCallbacks.message)
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
        ChartMetric.NET -> items.sumOf { it.netBalance.abs() }
        ChartMetric.TRANSFER -> transferTotal.abs()
    }

    fun mapOne(metric: CategoryTreeMetrics): CategoryChartData? {
        val amount = when (selectedMetric) {
            ChartMetric.INCOME -> metric.income
            ChartMetric.EXPENSE -> metric.expense
            ChartMetric.NET -> metric.netBalance
            ChartMetric.TRANSFER -> metric.transfer
        }
        val prevAmount = when (selectedMetric) {
            ChartMetric.INCOME -> metric.previousIncome
            ChartMetric.EXPENSE -> metric.previousExpense
            ChartMetric.NET -> metric.previousNetBalance
            ChartMetric.TRANSFER -> metric.previousTransfer
        }
        val subData = metric.subcategoryData.mapNotNull { mapOne(it) }
            .sortedByDescending { it.amount.abs() }
        if (amount.signum() == 0 && subData.isEmpty() && (prevAmount == null || prevAmount.signum() == 0)) {
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
