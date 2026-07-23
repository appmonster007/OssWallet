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
import com.osscabinet.ossWallet.domain.service.financial.FinancialSummary
import com.osscabinet.ossWallet.domain.model.TransactionFilter
import com.osscabinet.ossWallet.domain.model.settings.AccountingPerspective
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
import com.osscabinet.ossWallet.ui.components.visualization.charts.AeonChartLegend
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

            // 1. Compute per-bucket visible net based on legend toggles
            val visibleNetData = remember(renderData.buckets, config.perspective, config.visibleMetrics, config.splitTransfers) {
                val isCashFlow = config.perspective is AccountingPerspective.CashFlow
                val showInc = config.visibleMetrics.contains(ChartMetric.INCOME)
                val showExp = config.visibleMetrics.contains(ChartMetric.EXPENSE)
                val showInflow = config.visibleMetrics.contains(ChartMetric.INFLOW)
                val showOutflow = config.visibleMetrics.contains(ChartMetric.OUTFLOW)
                val showTrf = config.visibleMetrics.contains(ChartMetric.TRANSFER)
                val showTrfIn = config.visibleMetrics.contains(ChartMetric.TRANSFER_IN)
                val showTrfOut = config.visibleMetrics.contains(ChartMetric.TRANSFER_OUT)

                renderData.buckets.map { p ->
                    var bucketNet = BigDecimal.ZERO
                    if (isCashFlow) {
                        if (showInflow) bucketNet += p.metrics.inflow
                        if (showOutflow) bucketNet += p.metrics.outflow
                    } else {
                        // In Accounting mode, we look at individual components.
                        if (showInc) bucketNet += p.metrics.income
                        if (showExp) bucketNet += p.metrics.expense
                        if (config.splitTransfers) {
                            if (showTrfIn) bucketNet += p.metrics.transferIn
                            if (showTrfOut) bucketNet += p.metrics.transferOut
                        } else {
                            if (showTrf) bucketNet += p.metrics.transfer
                        }
                    }
                    bucketNet
                }
            }

            // Total for the summary section (sum of all visible components)
            val summaryTotal = remember(visibleNetData) {
                visibleNetData.fold(BigDecimal.ZERO) { acc, v -> acc + v }
            }

            ChartSummarySection(
                total = summaryTotal,
                previousTotal = null,
                currency = renderData.currency,
                showComparison = false,
                isAllTime = config.filter.timelineRange is com.osscabinet.ossWallet.domain.model.timeline.TimelineRange.AllTime,
                perspective = config.perspective,
                selectedMetric = ChartMetric.NET, // Summary shows the net of visible components
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
                val showInflow = config.visibleMetrics.contains(ChartMetric.INFLOW)
                val showOutflow = config.visibleMetrics.contains(ChartMetric.OUTFLOW)
                val showTrf = config.visibleMetrics.contains(ChartMetric.TRANSFER)
                val showTrfIn = config.visibleMetrics.contains(ChartMetric.TRANSFER_IN)
                val showTrfOut = config.visibleMetrics.contains(ChartMetric.TRANSFER_OUT)

                if (isCumulative) {
                    var runInc = BigDecimal.ZERO
                    var runExp = BigDecimal.ZERO
                    var runTrf = BigDecimal.ZERO
                    var runTrfIn = BigDecimal.ZERO
                    var runTrfOut = BigDecimal.ZERO
                    var runInflow = BigDecimal.ZERO
                    var runOutflow = BigDecimal.ZERO
                    
                    listOf(AeonBarChartGroup("", emptyList())) + renderData.buckets.map { p ->
                        runInc += p.metrics.income
                        runExp += p.metrics.expense
                        runTrf += p.metrics.transfer
                        runTrfIn += p.metrics.transferIn
                        runTrfOut += p.metrics.transferOut
                        runInflow += p.metrics.inflow
                        runOutflow += p.metrics.outflow
                        
                        val bars = mutableListOf<AeonBarChartBar>()
                        if (isCashFlow) {
                            // Directional View: Inflow and Outflow
                            if (showInflow && runInflow.signum() != 0) bars.add(AeonBarChartBar(runInflow, "Inflow", ChartColorGreen))
                            if (showOutflow && runOutflow.signum() != 0) bars.add(AeonBarChartBar(runOutflow, "Outflow", ChartColorRed))
                        } else {
                            // Detailed Nature View
                            if (showInc && runInc.signum() != 0) bars.add(AeonBarChartBar(runInc, "Income", ChartColorGreen))
                            if (showExp && runExp.signum() != 0) bars.add(AeonBarChartBar(runExp, "Expense", ChartColorRed))
                            if (config.splitTransfers) {
                                if (showTrfIn && runTrfIn.signum() != 0) bars.add(AeonBarChartBar(runTrfIn, "Transfer In", ChartColorOrange))
                                if (showTrfOut && runTrfOut.signum() != 0) bars.add(AeonBarChartBar(runTrfOut, "Transfer Out", AccentOrange))
                            } else {
                                if (showTrf && runTrf.signum() != 0) bars.add(AeonBarChartBar(runTrf, "Transfer", ChartColorOrange))
                            }
                        }
                        AeonBarChartGroup(p.label, bars)
                    }
                } else {
                    renderData.buckets.map { p ->
                        val bars = mutableListOf<AeonBarChartBar>()
                        if (isCashFlow) {
                            // Directional View: Inflow and Outflow
                            if (showInflow && p.metrics.inflow.signum() != 0) bars.add(AeonBarChartBar(p.metrics.inflow, "Inflow", ChartColorGreen))
                            if (showOutflow && p.metrics.outflow.signum() != 0) bars.add(AeonBarChartBar(p.metrics.outflow, "Outflow", ChartColorRed))
                        } else {
                            if (showInc && p.metrics.income.signum() != 0) bars.add(AeonBarChartBar(p.metrics.income, "Income", ChartColorGreen))
                            if (showExp && p.metrics.expense.signum() != 0) bars.add(AeonBarChartBar(p.metrics.expense, "Expense", ChartColorRed))
                            if (config.splitTransfers) {
                                if (showTrfIn && p.metrics.transferIn.signum() != 0) bars.add(AeonBarChartBar(p.metrics.transferIn, "Transfer In", ChartColorOrange))
                                if (showTrfOut && p.metrics.transferOut.signum() != 0) bars.add(AeonBarChartBar(p.metrics.transferOut, "Transfer Out", AccentOrange))
                            } else {
                                if (showTrf && p.metrics.transfer.signum() != 0) bars.add(AeonBarChartBar(p.metrics.transfer, "Transfer", ChartColorOrange))
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

            val netLineData: List<BigDecimal> = remember(visibleNetData, renderData.todayBucketIndex, isCumulative, config.visibleMetrics) {
                if (!config.visibleMetrics.contains(ChartMetric.NET)) return@remember emptyList()

                val realValues = if (renderData.todayBucketIndex >= 0) {
                    visibleNetData.take(renderData.todayBucketIndex + 1)
                } else if (renderData.todayBucketIndex == -1) {
                    visibleNetData
                } else {
                    emptyList()
                }

                if (isCumulative) {
                    val result = mutableListOf(BigDecimal.ZERO)
                    var running = BigDecimal.ZERO
                    realValues.forEach { v ->
                        running += v
                        result.add(running)
                    }
                    result
                } else {
                    realValues
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
    val isCashFlow = perspective is com.osscabinet.ossWallet.domain.model.settings.AccountingPerspective.CashFlow

    // Resolve colors in composable scope
    val incColor = ChartColorGreen
    val expColor = ChartColorRed
    val trfInColor = ChartColorOrange
    val trfOutColor = AccentOrange
    val trfColor = ChartColorOrange
    val netColor = ChartColorBlue

    val legendItems = remember(labels, visibleMetrics, perspective, splitTransfers) {
        buildList {
            if (isCashFlow) {
                // 1. Inflow
                add(
                    AeonLegendItemData(
                        color = incColor,
                        label = labels[ChartMetric.INFLOW] ?: "Inflow",
                        isEnabled = visibleMetrics.contains(ChartMetric.INFLOW),
                        onClick = { onToggleMetrics(listOf(ChartMetric.INFLOW)) }
                    )
                )
                // 2. Outflow
                add(
                    AeonLegendItemData(
                        color = expColor,
                        label = labels[ChartMetric.OUTFLOW] ?: "Outflow",
                        isEnabled = visibleMetrics.contains(ChartMetric.OUTFLOW),
                        onClick = { onToggleMetrics(listOf(ChartMetric.OUTFLOW)) }
                    )
                )
            } else {
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
                val showTransferLegend = (labels.containsKey(ChartMetric.TRANSFER)) ||
                                         (labels.containsKey(ChartMetric.TRANSFER_IN))

                if (showTransferLegend) {
                    if (splitTransfers) {
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
                                label = labels[ChartMetric.TRANSFER] ?: "Transfers",
                                isEnabled = visibleMetrics.contains(ChartMetric.TRANSFER),
                                onClick = { onToggleMetrics(listOf(ChartMetric.TRANSFER)) }
                            )
                        )
                    }
                }
            }
            
            // 4. Net
            add(
                AeonLegendItemData(
                    color = netColor,
                    label = labels[ChartMetric.NET] ?: "Net",
                    isEnabled = visibleMetrics.contains(ChartMetric.NET),
                    onClick = { onToggleMetrics(listOf(ChartMetric.NET)) },
                    id = "net"
                )
            )
        }.sortedBy { it.label }
    }

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        AeonChartLegend(
            items = legendItems,
            modifier = Modifier.weight(1f)
        )

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

@androidx.compose.ui.tooling.preview.Preview(showBackground = true)
@Composable
fun IncomeExpenseBarCardPreview() {
    val buckets = listOf(
        com.osscabinet.ossWallet.domain.card.CashFlowTimePoint(
            "Jan",
            0,
            FinancialSummary(
                income = BigDecimal("5000"),
                expense = BigDecimal("-3500"),
                transferIn = BigDecimal.ZERO,
                transferOut = BigDecimal.ZERO
            )
        ),
        com.osscabinet.ossWallet.domain.card.CashFlowTimePoint(
            "Feb",
            0,
            FinancialSummary(
                income = BigDecimal("5200"),
                expense = BigDecimal("-4000"),
                transferIn = BigDecimal.ZERO,
                transferOut = BigDecimal.ZERO
            )
        ),
        com.osscabinet.ossWallet.domain.card.CashFlowTimePoint(
            "Mar",
            0,
            FinancialSummary(
                income = BigDecimal("4800"),
                expense = BigDecimal("-3800"),
                transferIn = BigDecimal.ZERO,
                transferOut = BigDecimal.ZERO
            )
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
