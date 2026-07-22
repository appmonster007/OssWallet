package com.osscabinet.ossWallet.ui.components.cards.types

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.osscabinet.ossWallet.domain.card.CardInput
import com.osscabinet.ossWallet.domain.card.CardRenderData
import com.osscabinet.ossWallet.domain.card.CardUpdateHandler
import com.osscabinet.ossWallet.domain.model.TransactionFilter
import com.osscabinet.ossWallet.domain.model.settings.IncomeExpenseBarCardConfig
import com.osscabinet.ossWallet.domain.model.settings.ChartMetric
import com.osscabinet.ossWallet.ui.components.AeonDropdownSelector
import com.osscabinet.ossWallet.ui.components.AeonSettingsToggle
import com.osscabinet.ossWallet.ui.components.cards.AeonExpressiveCard
import com.osscabinet.ossWallet.ui.components.cards.AeonInteractionCallbacks
import com.osscabinet.ossWallet.ui.components.cards.CardCallbacks
import com.osscabinet.ossWallet.ui.components.cards.CardLayoutProps
import com.osscabinet.ossWallet.ui.components.cards.CardPerspectiveSettings
import com.osscabinet.ossWallet.ui.components.cards.CardSettingsSection
import com.osscabinet.ossWallet.ui.components.cards.CardWithCallbacks
import com.osscabinet.ossWallet.ui.components.cards.ChartSummarySection
import com.osscabinet.ossWallet.ui.components.cards.resolveDrillDown
import com.osscabinet.ossWallet.ui.components.visualization.charts.AeonBarChartBar
import com.osscabinet.ossWallet.ui.components.visualization.charts.AeonBarChartGroup
import com.osscabinet.ossWallet.ui.components.visualization.charts.AeonBarLineChart
import com.osscabinet.ossWallet.ui.components.visualization.charts.AeonLegendItemData
import com.osscabinet.ossWallet.ui.theme.AccentOrange
import com.osscabinet.ossWallet.ui.theme.AppColors
import com.osscabinet.ossWallet.ui.theme.AppTextStyles
import com.osscabinet.ossWallet.ui.theme.ChartColorBlue
import com.osscabinet.ossWallet.ui.theme.ChartColorGreen
import com.osscabinet.ossWallet.ui.theme.ChartColorOrange
import com.osscabinet.ossWallet.ui.theme.ChartColorRed
import com.osscabinet.ossWallet.ui.theme.OssWalletTheme
import com.osscabinet.ossWallet.ui.util.CurrencyDisplaySpec
import com.osscabinet.ossWallet.ui.util.CurrencyReprUi
import java.math.BigDecimal

fun mapIncomeExpenseBar(
    config: IncomeExpenseBarCardConfig,
    renderData: CardRenderData.IncomeExpenseBar,
    input: CardInput,
    updateHandler: CardUpdateHandler,
    onDelete: ((String) -> Unit)?,
    onRecordsClick: (TransactionFilter) -> Unit,
    callbacks: AeonInteractionCallbacks,
    filterSheetEnabled: Boolean,
) = CardWithCallbacks.IncomeExpenseBar(
    renderData = renderData,
    callbacks = CardCallbacks.IncomeExpenseBar(
        onUpdate = { newConfig ->
            updateHandler.updateIncomeExpenseBarConfig(
                config.cardId,
                newConfig,
            )
        },
        onDelete = onDelete?.let { { it(config.cardId) } },
        onRecordsClick = { drillDown ->
            onRecordsClick(
                resolveDrillDown(
                    input,
                    config,
                    drillDown
                )
            )
        },
        onSaveFilterClick = callbacks.filterActions.onSaveClick
    ),
    filterRules = config.filterRules,
    baseFilter = input.filter,
    filterSheetEnabled = filterSheetEnabled
)

@Composable
fun IncomeExpenseBarCard(
    renderData: CardRenderData.IncomeExpenseBar,
    callbacks: CardCallbacks.IncomeExpenseBar = CardCallbacks.IncomeExpenseBar(),
    layoutProps: CardLayoutProps = CardLayoutProps(),
) {
    val config = renderData.config
    val isCumulative = config.isCumulative
    val currentOnUpdate = rememberUpdatedState(callbacks.onUpdate)

    val displayFilter = layoutProps.computeDisplayFilter(config.filter)

    AeonExpressiveCard(
        title = config.title,
        icon = Icons.Default.BarChart,
        modifier = layoutProps.modifier,
        headerModifier = layoutProps.headerModifier,
        filterEnabled = layoutProps.showFilterButton,
        filter = displayFilter,
        baseFilter = layoutProps.baseFilter,
        filterRules = layoutProps.filterRules ?: config.filterRules,
        filterContext = layoutProps.filterContext,
        onFilterChange = { newFilter ->
            callbacks.onUpdate(config.copy(filter = newFilter))
        },
        onSaveFilterClick = callbacks.onSaveFilterClick,
        onDelete = callbacks.onDelete,
        settingsContent = {
            IncomeExpenseBarSettingsContent(
                config = config,
                onUpdate = { currentOnUpdate.value(it) },
            )
        }
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            // --- Gated Heavy Computations ---
            // Net total for the period (sum of all bucket net values)
            val netTotal = remember(renderData.buckets) {
                renderData.buckets.fold(BigDecimal.ZERO) { acc, p -> acc + p.net }
            }

            ChartSummarySection(
                total = netTotal,
                previousTotal = null,
                currency = renderData.currency,
                showComparison = false,
                isAllTime = config.filter.timelineRange is com.osscabinet.ossWallet.domain.model.timeline.TimelineRange.AllTime,
                perspective = config.perspective,
                selectedMetric = ChartMetric.NET, // This card is a combination, but summary shows NET
                onMetricChange = { /* Metric change not applicable to this combined card */ },
                splitTransfers = config.splitTransfers,
                dropdownEnabled = false
            )

            // Legend row with Trend/Cumulative dropdown
            IncomeExpenseLegendRow(
                perspective = config.perspective,
                isCumulative = isCumulative,
                splitTransfers = config.splitTransfers,
                visibleMetrics = config.visibleMetrics,
                onToggleMetrics = { metrics: List<ChartMetric> ->
                    var newMetrics = config.visibleMetrics
                    metrics.forEach { metric: ChartMetric ->
                        newMetrics = if (newMetrics.contains(metric)) {
                            newMetrics - metric
                        } else {
                            newMetrics + metric
                        }
                    }
                    callbacks.onUpdate(config.copy(visibleMetrics = newMetrics))
                },
                onModeChange = { callbacks.onUpdate(config.copy(isCumulative = it)) }
            )

            // Build chart data.
            val barGroups = remember(renderData.buckets, config.perspective, config.splitTransfers, isCumulative, config.visibleMetrics) {
                val isCashFlow = config.perspective is com.osscabinet.ossWallet.domain.model.settings.AccountingPerspective.CashFlow
                val showInc = config.visibleMetrics.contains(ChartMetric.INCOME)
                val showExp = config.visibleMetrics.contains(ChartMetric.EXPENSE)
                val showTrf = config.visibleMetrics.contains(ChartMetric.TRANSFER)
                val showTrfIn = config.visibleMetrics.contains(ChartMetric.TRANSFER_IN)
                val showTrfOut = config.visibleMetrics.contains(ChartMetric.TRANSFER_OUT)

                if (isCumulative) {
                    var runInc = BigDecimal.ZERO
                    var runExp = BigDecimal.ZERO
                    var runTrf = BigDecimal.ZERO
                    var runTrfIn = BigDecimal.ZERO
                    var runTrfOut = BigDecimal.ZERO
                    
                    listOf(AeonBarChartGroup("", emptyList())) + renderData.buckets.map { p ->
                        runInc += p.income
                        runExp += p.expense
                        runTrf += p.transfer
                        runTrfIn += p.transferIn
                        runTrfOut += p.transferOut
                        
                        val bars = mutableListOf<AeonBarChartBar>()
                        if (isCashFlow) {
                            val rawInc = runInc - runTrfIn
                            if (showInc) bars.add(AeonBarChartBar(rawInc, "Income", ChartColorGreen))
                            if (showTrfIn) bars.add(AeonBarChartBar(runTrfIn, "Transfer In", ChartColorOrange))
                            
                            val rawExp = runExp - runTrfOut
                            if (showExp) bars.add(AeonBarChartBar(rawExp, "Expense", ChartColorRed))
                            if (showTrfOut) bars.add(AeonBarChartBar(runTrfOut, "Transfer Out", AccentOrange))
                        } else {
                            if (showInc) bars.add(AeonBarChartBar(runInc, "Income", ChartColorGreen))
                            if (showExp) bars.add(AeonBarChartBar(runExp, "Expense", ChartColorRed))
                            if (config.splitTransfers) {
                                if (showTrfIn) bars.add(AeonBarChartBar(runTrfIn, "Transfer In", ChartColorOrange))
                                if (showTrfOut) bars.add(AeonBarChartBar(runTrfOut, "Transfer Out", AccentOrange))
                            } else {
                                if (showTrf) bars.add(AeonBarChartBar(runTrf, "Transfer", ChartColorOrange))
                            }
                        }
                        AeonBarChartGroup(p.label, bars)
                    }
                } else {
                    renderData.buckets.map { p ->
                        val bars = mutableListOf<AeonBarChartBar>()
                        if (isCashFlow) {
                            val rawInc = p.income - p.transferIn
                            if (showInc) bars.add(AeonBarChartBar(rawInc, "Income", ChartColorGreen))
                            if (showTrfIn) bars.add(AeonBarChartBar(p.transferIn, "Transfer In", ChartColorOrange))
                            
                            val rawExp = p.expense - p.transferOut
                            if (showExp) bars.add(AeonBarChartBar(rawExp, "Expense", ChartColorRed))
                            if (showTrfOut) bars.add(AeonBarChartBar(p.transferOut, "Transfer Out", AccentOrange))
                        } else {
                            if (showInc) bars.add(AeonBarChartBar(p.income, "Income", ChartColorGreen))
                            if (showExp) bars.add(AeonBarChartBar(p.expense, "Expense", ChartColorRed))
                            if (config.splitTransfers) {
                                if (showTrfIn) bars.add(AeonBarChartBar(p.transferIn, "Transfer In", ChartColorOrange))
                                if (showTrfOut) bars.add(AeonBarChartBar(p.transferOut, "Transfer Out", AccentOrange))
                            } else {
                                if (showTrf) bars.add(AeonBarChartBar(p.transfer, "Transfer", ChartColorOrange))
                            }
                        }
                        AeonBarChartGroup(p.label, bars)
                    }
                }
            }

            // X-Labels: For cumulative mode, we prepend null for the origin point.
            val xLabelData: List<String?> = remember(renderData.buckets, isCumulative) {
                if (isCumulative) {
                    listOf(null) + renderData.buckets.map { it.label }
                } else {
                    renderData.buckets.map { it.label }
                }
            }

            val netLineData: List<BigDecimal> = remember(renderData.buckets, renderData.todayBucketIndex, isCumulative, config.visibleMetrics) {
                if (!config.visibleMetrics.contains(ChartMetric.NET)) return@remember emptyList()

                val realBuckets = if (renderData.todayBucketIndex >= 0) {
                    renderData.buckets.take(renderData.todayBucketIndex + 1)
                } else if (renderData.todayBucketIndex == -1) {
                    renderData.buckets
                } else {
                    emptyList()
                }
                if (isCumulative) {
                    buildCumulativeNet(realBuckets)
                } else {
                    realBuckets.map { it.net }
                }
            }

            if (renderData.buckets.isNotEmpty()) {
                AeonBarLineChart(
                    groups = barGroups,
                    lineData = netLineData,
                    xLabels = xLabelData,
                    showXAxisLabels = true,
                    lineThicknessDp = 1.5f,
                    lineColor = ChartColorBlue,
                    currency = renderData.currency,
                    showGrid = config.showGridLines,
                    animate = layoutProps.animate,
                    onNavigateToRecords = { index ->
                        val bucket = if (isCumulative) {
                            if (index > 0) renderData.buckets.getOrNull(index - 1) else null
                        } else {
                            renderData.buckets.getOrNull(index)
                        }

                        bucket?.let {
                            val drillDownFilter = TransactionFilter(
                                timelineRange = com.osscabinet.ossWallet.domain.model.timeline.TimelineRange.CustomRange(
                                    start = it.epochMillis,
                                    end = it.bucketEnd
                                )
                            )
                            callbacks.onRecordsClick(drillDownFilter)
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(220.dp)
                )
            } else {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(100.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "No data for selected period",
                        style = AppTextStyles.caption,
                        color = AppColors.contentSecondary
                    )
                }
            }
        }
    }
}

/**
 * Builds a cumulative net list prepended with a zero origin point.
 */
private fun buildCumulativeNet(buckets: List<com.osscabinet.ossWallet.domain.card.CashFlowTimePoint>): List<BigDecimal> {
    val result = mutableListOf(BigDecimal.ZERO)
    var running = BigDecimal.ZERO
    buckets.forEach { point ->
        running += point.net
        result.add(running)
    }
    return result
}

@Composable
private fun IncomeExpenseLegendRow(
    perspective: com.osscabinet.ossWallet.domain.model.settings.AccountingPerspective,
    isCumulative: Boolean,
    splitTransfers: Boolean,
    visibleMetrics: Set<ChartMetric>,
    onToggleMetrics: (List<ChartMetric>) -> Unit,
    onModeChange: (Boolean) -> Unit
) {
    val labels = com.osscabinet.ossWallet.domain.model.settings.AccountingPerspective.labelsFor(perspective, splitTransfers)
    
    // Resolve colors in composable scope
    val incColor = ChartColorGreen
    val expColor = ChartColorRed
    val trfInColor = ChartColorOrange
    val trfOutColor = AccentOrange
    val trfColor = ChartColorOrange
    val netColor = ChartColorBlue

    val legendItems = remember(labels, visibleMetrics, perspective, splitTransfers) {
        buildList {
            // 1. Income
            add(
                AeonLegendItemData(
                    color = incColor,
                    label = labels[ChartMetric.INCOME] ?: "Income",
                    isEnabled = visibleMetrics.contains(ChartMetric.INCOME),
                    onClick = { onToggleMetrics(listOf(ChartMetric.INCOME)) }
                )
            )
            // 2. Expense
            add(
                AeonLegendItemData(
                    color = expColor,
                    label = labels[ChartMetric.EXPENSE] ?: "Expense",
                    isEnabled = visibleMetrics.contains(ChartMetric.EXPENSE),
                    onClick = { onToggleMetrics(listOf(ChartMetric.EXPENSE)) }
                )
            )
            
            // 3. Transfers
            val isCashFlow = perspective is com.osscabinet.ossWallet.domain.model.settings.AccountingPerspective.CashFlow
            val showTransferLegend = isCashFlow || 
                                     (labels.containsKey(ChartMetric.TRANSFER)) ||
                                     (labels.containsKey(ChartMetric.TRANSFER_IN))

            if (showTransferLegend) {
                if (isCashFlow || splitTransfers) {
                    add(
                        AeonLegendItemData(
                            color = trfInColor,
                            label = labels[ChartMetric.TRANSFER_IN] ?: "In",
                            isEnabled = visibleMetrics.contains(ChartMetric.TRANSFER_IN),
                            onClick = { onToggleMetrics(listOf(ChartMetric.TRANSFER_IN)) }
                        )
                    )
                    add(
                        AeonLegendItemData(
                            color = trfOutColor,
                            label = labels[ChartMetric.TRANSFER_OUT] ?: "Out",
                            isEnabled = visibleMetrics.contains(ChartMetric.TRANSFER_OUT),
                            onClick = { onToggleMetrics(listOf(ChartMetric.TRANSFER_OUT)) }
                        )
                    )
                } else {
                    add(
                        AeonLegendItemData(
                            color = trfColor,
                            label = "Transfers",
                            isEnabled = visibleMetrics.contains(ChartMetric.TRANSFER),
                            onClick = { onToggleMetrics(listOf(ChartMetric.TRANSFER)) }
                        )
                    )
                }
            }
            
            // 4. Net
            add(
                AeonLegendItemData(
                    color = netColor,
                    label = labels[ChartMetric.NET] ?: "Net",
                    isEnabled = visibleMetrics.contains(ChartMetric.NET),
                    onClick = { onToggleMetrics(listOf(ChartMetric.NET)) }
                )
            )
        }.sortedBy { it.label }
    }

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        FlowRow(
            modifier = Modifier.weight(1f),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            legendItems.forEach { item ->
                IncomeExpenseLegendItem(
                    color = item.color,
                    label = item.label,
                    isEnabled = item.isEnabled,
                    onClick = item.onClick
                )
            }
        }

        Box(modifier = Modifier.padding(start = 8.dp)) {
            AeonDropdownSelector(
                value = if (isCumulative) "Cumulative" else "Trend",
                options = listOf("Trend", "Cumulative"),
                labelProvider = { it },
                onValueChange = { onModeChange(it == "Cumulative") }
            )
        }
    }
}

@Composable
private fun IncomeExpenseLegendItem(
    color: Color,
    label: String,
    isEnabled: Boolean,
    onClick: () -> Unit
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        modifier = Modifier
            .clickable(onClick = onClick)
            .alpha(if (isEnabled) 1f else 0.4f)
    ) {
        Box(
            modifier = Modifier
                .size(width = 16.dp, height = 3.dp)
                .background(
                    color = if (isEnabled) color else color.copy(alpha = 0.4f),
                    shape = RoundedCornerShape(2.dp)
                )
        )
        Text(
            text = label,
            style = AppTextStyles.caption.copy(fontSize = 10.sp),
            color = if (isEnabled) AppColors.contentPrimary else AppColors.contentSecondary
        )
    }
}

@androidx.compose.ui.tooling.preview.Preview(showBackground = true)
@Composable
fun IncomeExpenseBarCardPreview() {
    val buckets = listOf(
        com.osscabinet.ossWallet.domain.card.CashFlowTimePoint(
            "Jan",
            0,
            BigDecimal("5000"),
            BigDecimal("-3500"),
            BigDecimal.ZERO,
            BigDecimal("1500")
        ),
        com.osscabinet.ossWallet.domain.card.CashFlowTimePoint(
            "Feb",
            0,
            BigDecimal("5200"),
            BigDecimal("-4000"),
            BigDecimal.ZERO,
            BigDecimal("1200")
        ),
        com.osscabinet.ossWallet.domain.card.CashFlowTimePoint(
            "Mar",
            0,
            BigDecimal("4800"),
            BigDecimal("-3800"),
            BigDecimal.ZERO,
            BigDecimal("1000")
        )
    )
    val renderData = CardRenderData.IncomeExpenseBar(
        config = IncomeExpenseBarCardConfig(cardId = "preview"),
        buckets = buckets,
        granularity = com.osscabinet.ossWallet.domain.card.CashFlowComparisonGranularity.MONTH,
        currency = "USD",
        todayBucketIndex = 2
    )

    OssWalletTheme {
        Box(modifier = Modifier.padding(16.dp)) {
            IncomeExpenseBarCard(renderData = renderData)
        }
    }
}

@Composable
private fun IncomeExpenseBarSettingsContent(
    config: IncomeExpenseBarCardConfig,
    onUpdate: (IncomeExpenseBarCardConfig) -> Unit,
) {
    CardSettingsSection("Display") {
        AeonSettingsToggle(
            label = "Show Grid Lines",
            checked = config.showGridLines,
            onCheckedChange = { onUpdate(config.copy(showGridLines = it)) },
            showBackground = false
        )
    }
    CardPerspectiveSettings(
        perspective = config.perspective,
        onPerspectiveChange = { onUpdate(config.copy(perspective = it)) },
        excludeInternalTransfers = config.excludeInternalTransfers,
        onExcludeInternalTransfersChange = {
            onUpdate(config.copy(excludeInternalTransfers = it))
        },
        splitTransfers = config.splitTransfers,
        onSplitTransfersChange = {
            onUpdate(config.copy(splitTransfers = it))
        }
    )
}
