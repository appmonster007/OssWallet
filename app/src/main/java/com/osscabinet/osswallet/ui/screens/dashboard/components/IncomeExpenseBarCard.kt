package com.osscabinet.ossWallet.ui.screens.dashboard.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material3.*
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.osscabinet.ossWallet.domain.card.CardFilterRules
import com.osscabinet.ossWallet.domain.card.CardRenderData
import com.osscabinet.ossWallet.domain.model.TransactionFilter
import com.osscabinet.ossWallet.domain.model.settings.IncomeExpenseBarCardConfig
import com.osscabinet.ossWallet.ui.components.cards.AeonExpressiveCard
import com.osscabinet.ossWallet.ui.components.cards.CardCallbacks
import com.osscabinet.ossWallet.ui.components.cards.CardSettingsSheet
import com.osscabinet.ossWallet.ui.components.cards.CardSettingsSection
import com.osscabinet.ossWallet.ui.components.cards.FilterSelectionContext
import com.osscabinet.ossWallet.ui.components.visualization.charts.AeonBarLineChart
import com.osscabinet.ossWallet.ui.theme.AppColors
import com.osscabinet.ossWallet.ui.theme.AppTextStyles
import com.osscabinet.ossWallet.ui.theme.ChartColorBlue
import com.osscabinet.ossWallet.ui.theme.ChartColorGreen
import com.osscabinet.ossWallet.ui.theme.ChartColorRed
import com.osscabinet.ossWallet.domain.service.currency.CurrencyFormatter
import com.osscabinet.ossWallet.domain.service.currency.CurrencyService
import java.math.BigDecimal

@Composable
fun IncomeExpenseBarCard(
    renderData: CardRenderData.IncomeExpenseBar,
    callbacks: CardCallbacks.IncomeExpenseBar = CardCallbacks.IncomeExpenseBar(),
    filterContext: FilterSelectionContext = FilterSelectionContext(),
    baseFilter: TransactionFilter = TransactionFilter(),
    inheritedFilter: TransactionFilter = TransactionFilter(),
    filterRules: CardFilterRules? = null,
    screenOwnsTimeline: Boolean = false,
    showFilterButton: Boolean = true,
    modifier: Modifier = Modifier,
    headerModifier: Modifier = Modifier,
) {
    val config = renderData.config

    var isCumulative by rememberSaveable { mutableStateOf(config.isCumulative) }

    val displayFilter = if (screenOwnsTimeline || inheritedFilter.timelineRange !is com.osscabinet.ossWallet.domain.model.timeline.TimelineRange.AllTime)
        config.filter.copy(timelineRange = inheritedFilter.timelineRange)
    else config.filter

    AeonExpressiveCard(
        title = config.title,
        icon = Icons.Default.BarChart,
        modifier = modifier,
        headerModifier = headerModifier,
        filterEnabled = showFilterButton,
        filter = displayFilter,
        baseFilter = baseFilter,
        filterRules = filterRules ?: config.filterRules,
        filterContext = filterContext,
        onFilterChange = { newFilter ->
            callbacks.onUpdate(config.copy(filter = newFilter))
        },
        onSaveFilterClick = callbacks.onSaveFilterClick,
        displaySettings = { onDismiss ->
            IncomeExpenseBarSettingsSheet(
                config = config,
                callbacks = callbacks,
                onDismiss = onDismiss
            )
        }
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            // Segmented button: Trend | Cumulative
            val modeOptions = listOf("Trend", "Cumulative")
            SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                modeOptions.forEachIndexed { index, label ->
                    val isSelected = (isCumulative && label == "Cumulative") || (!isCumulative && label == "Trend")
                    SegmentedButton(
                        selected = isSelected,
                        onClick = {
                            val newCumulative = label == "Cumulative"
                            isCumulative = newCumulative
                            callbacks.onUpdate(config.copy(isCumulative = newCumulative))
                        },
                        shape = SegmentedButtonDefaults.itemShape(index = index, count = modeOptions.size),
                        label = {
                            Text(
                                text = label,
                                style = AppTextStyles.caption.copy(fontSize = 11.sp)
                            )
                        }
                    )
                }
            }

            // Net total for the period (sum of all bucket net values)
            val netTotal = renderData.buckets.fold(BigDecimal.ZERO) { acc, p -> acc + p.net }
            Text(
                text = CurrencyFormatter.formatCurrencyRight(netTotal, renderData.baseCurrency, CurrencyService.precision),
                style = AppTextStyles.cardSecondaryAmount,
                color = AppColors.contentPrimary,
                modifier = Modifier.padding(horizontal = 2.dp)
            )

            // Legend row
            IncomeExpenseLegendRow()

            // Build chart data — prepend zero sentinel so line starts at origin.
            // Bars cover the full period; net line is capped at today (todayBucketIndex).
            val incomeChartData = listOf(BigDecimal.ZERO) + renderData.buckets.map { it.income }
            val expenseChartData = listOf(BigDecimal.ZERO) + renderData.buckets.map { it.expense }
            val xLabelData: List<String?> = listOf(null) + renderData.buckets.map { it.label }

            val realBuckets = if (renderData.todayBucketIndex >= 0) {
                renderData.buckets.take(renderData.todayBucketIndex + 1)
            } else {
                emptyList()
            }
            val netLineData: List<BigDecimal> = if (isCumulative) {
                buildCumulativeNet(realBuckets)
            } else {
                listOf(BigDecimal.ZERO) + realBuckets.map { it.net }
            }

            if (renderData.buckets.isNotEmpty()) {
                AeonBarLineChart(
                    incomeData = incomeChartData,
                    expenseData = expenseChartData,
                    lineData = netLineData,
                    xLabels = xLabelData,
                    showXAxisLabels = true,
                    lineThicknessDp = 1.5f,
                    barColorPositive = ChartColorGreen,
                    barColorNegative = ChartColorRed,
                    lineColor = ChartColorBlue,
                    currency = renderData.baseCurrency,
                    showGrid = config.showGridLines,
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
private fun IncomeExpenseLegendRow() {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        IncomeExpenseLegendItem(color = ChartColorGreen, label = "Income")
        IncomeExpenseLegendItem(color = ChartColorRed, label = "Expense")
        IncomeExpenseLegendItem(color = ChartColorBlue, label = "Net")
    }
}

@Composable
private fun IncomeExpenseLegendItem(color: Color, label: String) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Box(
            modifier = Modifier
                .size(width = 16.dp, height = 3.dp)
                .background(
                    color = color,
                    shape = RoundedCornerShape(2.dp)
                )
        )
        Text(
            text = label,
            style = AppTextStyles.caption.copy(fontSize = 10.sp),
            color = AppColors.contentSecondary
        )
    }
}

@androidx.compose.ui.tooling.preview.Preview(showBackground = true)
@Composable
fun IncomeExpenseBarCardPreview() {
    val buckets = listOf(
        com.osscabinet.ossWallet.domain.card.CashFlowTimePoint("Jan", 0, BigDecimal("5000"), BigDecimal("3500"), BigDecimal("1500")),
        com.osscabinet.ossWallet.domain.card.CashFlowTimePoint("Feb", 0, BigDecimal("5200"), BigDecimal("4000"), BigDecimal("1200")),
        com.osscabinet.ossWallet.domain.card.CashFlowTimePoint("Mar", 0, BigDecimal("4800"), BigDecimal("3800"), BigDecimal("1000"))
    )
    val renderData = CardRenderData.IncomeExpenseBar(
        config = IncomeExpenseBarCardConfig(cardId = "preview"),
        buckets = buckets,
        granularity = com.osscabinet.ossWallet.domain.card.CashFlowComparisonGranularity.MONTH,
        baseCurrency = "USD",
        todayBucketIndex = 2
    )

    com.osscabinet.ossWallet.ui.theme.OssWalletTheme {
        Box(modifier = Modifier.padding(16.dp)) {
            IncomeExpenseBarCard(renderData = renderData)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun IncomeExpenseBarSettingsSheet(
    config: IncomeExpenseBarCardConfig,
    callbacks: CardCallbacks.IncomeExpenseBar,
    onDismiss: () -> Unit
) {
    CardSettingsSheet(title = "Income & Expense", onDismiss = onDismiss, onDelete = callbacks.onDelete) {
        CardSettingsSection("Display") {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Show Grid Lines", style = AppTextStyles.caption)
                Switch(
                    checked = config.showGridLines,
                    onCheckedChange = { callbacks.onUpdate(config.copy(showGridLines = it)) }
                )
            }
        }
    }
}
