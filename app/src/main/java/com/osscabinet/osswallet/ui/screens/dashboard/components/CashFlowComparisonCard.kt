package com.osscabinet.ossWallet.ui.screens.dashboard.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ShowChart
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.osscabinet.ossWallet.domain.card.CardRenderData
import com.osscabinet.ossWallet.domain.card.CashFlowComparisonGranularity
import com.osscabinet.ossWallet.domain.card.CashFlowComparisonSeries
import com.osscabinet.ossWallet.domain.model.TransactionFilter
import com.osscabinet.ossWallet.domain.model.settings.CashFlowComparisonCardConfig
import com.osscabinet.ossWallet.domain.model.settings.CashFlowComparisonMetric
import com.osscabinet.ossWallet.ui.components.cards.AeonExpressiveCard
import com.osscabinet.ossWallet.ui.components.cards.CardCallbacks
import com.osscabinet.ossWallet.ui.components.cards.FilterSelectionContext
import com.osscabinet.ossWallet.ui.components.visualization.charts.AeonLineChart
import com.osscabinet.ossWallet.ui.components.visualization.charts.AeonLineChartPoint
import com.osscabinet.ossWallet.ui.components.visualization.charts.AeonLineChartSeries
import com.osscabinet.ossWallet.ui.theme.AppColors
import com.osscabinet.ossWallet.ui.theme.AppTextStyles
import com.osscabinet.ossWallet.ui.theme.ChartColorBlue
import com.osscabinet.ossWallet.ui.theme.ChartColorGreen
import com.osscabinet.ossWallet.ui.theme.ChartColorOrange
import com.osscabinet.ossWallet.domain.service.currency.CurrencyFormatter
import com.osscabinet.ossWallet.domain.service.currency.CurrencyService
import java.math.BigDecimal

private val CurrentPeriodColor = ChartColorBlue
private val PreviousPeriodColor = ChartColorOrange
private val LastYearColor = ChartColorGreen

@Composable
fun CashFlowComparisonCard(
    renderData: CardRenderData.CashFlowComparison,
    callbacks: CardCallbacks.CashFlowComparison = CardCallbacks.CashFlowComparison(),
    filterContext: FilterSelectionContext = FilterSelectionContext(),
    baseFilter: TransactionFilter = TransactionFilter(),
    modifier: Modifier = Modifier,
    headerModifier: Modifier = Modifier,
) {
    val config = renderData.config

    var selectedMetric by rememberSaveable { mutableStateOf(config.selectedMetric) }

    AeonExpressiveCard(
        title = config.title,
        icon = Icons.AutoMirrored.Filled.ShowChart,
        modifier = modifier,
        headerModifier = headerModifier,
        filter = config.filter,
        baseFilter = baseFilter,
        filterRules = config.filterRules,
        filterContext = filterContext,
        onFilterChange = { newFilter ->
            callbacks.onUpdate(config.copy(filter = newFilter))
        },
        displaySettings = { onDismiss ->
            CashFlowComparisonDisplaySettingsDialog(
                config = config,
                callbacks = callbacks,
                onDismiss = onDismiss
            )
        }
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            // Metric toggle row — purely display-local; no config persist needed
            MetricToggleRow(
                selectedMetric = selectedMetric,
                onMetricChange = { metric -> selectedMetric = metric }
            )

            // Aggregate sum for current period
            val aggregateTotal = renderData.currentSeries.points.fold(BigDecimal.ZERO) { acc, p ->
                acc + when (selectedMetric) {
                    CashFlowComparisonMetric.NET -> p.net
                    CashFlowComparisonMetric.INCOME -> p.income
                    CashFlowComparisonMetric.EXPENSE -> p.expense.negate()
                }
            }
            Text(
                text = CurrencyFormatter.formatCurrencyRight(aggregateTotal, renderData.baseCurrency, CurrencyService.precision),
                style = AppTextStyles.cardSecondaryAmount,
                color = AppColors.contentPrimary,
                modifier = Modifier.padding(horizontal = 2.dp)
            )

            // Legend row
            LegendRow(
                showCurrent = config.showCurrentPeriod,
                showPrevious = config.showPreviousPeriod,
                showLastYear = config.showSamePeriodLastYear
            )

            // Chart
            val series = buildSeriesList(
                config = config,
                selectedMetric = selectedMetric,
                isCumulative = config.isCumulative,
                renderData = renderData
            )

            if (series.isNotEmpty()) {
                // Zero-origin sentinel + one slot per full-period bucket.
                // Shared across all series so shorter (current) series doesn't stretch.
                val totalXSlots = renderData.periodLabels.size + 1
                val xAxisLabels: List<String?> = listOf(null) + renderData.periodLabels
                AeonLineChart(
                    series = series,
                    currency = renderData.baseCurrency,
                    lineThicknessDp = 1.5f,
                    showGrid = config.showGridLines,
                    totalXSlots = totalXSlots,
                    xAxisLabels = xAxisLabels,
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
private fun MetricToggleRow(
    selectedMetric: CashFlowComparisonMetric,
    onMetricChange: (CashFlowComparisonMetric) -> Unit
) {
    val options = CashFlowComparisonMetric.entries
    SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
        options.forEachIndexed { index, metric ->
            SegmentedButton(
                selected = selectedMetric == metric,
                onClick = { onMetricChange(metric) },
                shape = SegmentedButtonDefaults.itemShape(index = index, count = options.size),
                label = {
                    Text(
                        text = metric.name,
                        style = AppTextStyles.caption.copy(fontSize = 11.sp)
                    )
                }
            )
        }
    }
}

@Composable
private fun LegendRow(
    showCurrent: Boolean,
    showPrevious: Boolean,
    showLastYear: Boolean
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (showCurrent) {
            LegendItem(color = CurrentPeriodColor, label = "Current", isDashed = false, alpha = 1f)
        }
        if (showPrevious) {
            LegendItem(color = PreviousPeriodColor, label = "Previous", isDashed = true, alpha = 1f)
        }
        if (showLastYear) {
            LegendItem(color = LastYearColor, label = "Last Year", isDashed = true, alpha = 0.6f)
        }
    }
}

@Composable
private fun LegendItem(
    color: Color,
    label: String,
    isDashed: Boolean,
    alpha: Float
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Box(
            modifier = Modifier
                .size(width = 16.dp, height = 3.dp)
                .background(
                    color = color.copy(alpha = alpha),
                    shape = RoundedCornerShape(2.dp)
                )
        )
        Text(
            text = label,
            style = AppTextStyles.caption.copy(fontSize = 10.sp),
            color = AppColors.contentSecondary.copy(alpha = alpha)
        )
    }
}

private fun buildSeriesList(
    config: CashFlowComparisonCardConfig,
    selectedMetric: CashFlowComparisonMetric,
    isCumulative: Boolean,
    renderData: CardRenderData.CashFlowComparison
): List<AeonLineChartSeries> {
    val series = mutableListOf<AeonLineChartSeries>()

    fun seriesPoints(s: CashFlowComparisonSeries): List<AeonLineChartPoint> {
        val origin = AeonLineChartPoint(x = 0f, y = BigDecimal.ZERO, label = null)
        var running = BigDecimal.ZERO
        val dataPoints = s.points.mapIndexed { index, point ->
            val value = when (selectedMetric) {
                CashFlowComparisonMetric.NET -> point.net
                CashFlowComparisonMetric.INCOME -> point.income
                CashFlowComparisonMetric.EXPENSE -> point.expense.negate()
            }
            running += value
            AeonLineChartPoint(
                x = (index + 1).toFloat(),
                y = if (isCumulative) running else value,
                label = point.label
            )
        }
        return listOf(origin) + dataPoints
    }

    if (config.showCurrentPeriod && renderData.currentSeries.points.isNotEmpty()) {
        series.add(
            AeonLineChartSeries(
                name = "Current",
                color = CurrentPeriodColor,
                dataPoints = seriesPoints(renderData.currentSeries),
                fillGradient = true,
                isDashed = false
            )
        )
    }

    if (config.showPreviousPeriod && renderData.previousSeries.points.isNotEmpty()) {
        series.add(
            AeonLineChartSeries(
                name = "Previous",
                color = PreviousPeriodColor,
                dataPoints = seriesPoints(renderData.previousSeries),
                fillGradient = false,
                isDashed = false
            )
        )
    }

    if (config.showSamePeriodLastYear && renderData.lastYearSeries.points.isNotEmpty()) {
        series.add(
            AeonLineChartSeries(
                name = "Last Year",
                color = LastYearColor.copy(alpha = 0.6f),
                dataPoints = seriesPoints(renderData.lastYearSeries),
                fillGradient = false,
                isDashed = false
            )
        )
    }

    return series
}

@Composable
private fun CashFlowComparisonDisplaySettingsDialog(
    config: CashFlowComparisonCardConfig,
    callbacks: CardCallbacks.CashFlowComparison,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        dismissButton = {
            TextButton(
                onClick = {
                    callbacks.onDelete()
                    onDismiss()
                },
                colors = ButtonDefaults.textButtonColors(contentColor = AppColors.error)
            ) {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = "Delete card",
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text("Delete")
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("Done") }
        },
        title = {
            Text("Cash Flow Comparison", style = AppTextStyles.cardTitle)
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Cumulative", style = AppTextStyles.caption)
                    Switch(
                        checked = config.isCumulative,
                        onCheckedChange = { callbacks.onUpdate(config.copy(isCumulative = it)) }
                    )
                }

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

                HorizontalDivider(color = AppColors.border.copy(alpha = 0.3f))

                Text("Show series", style = AppTextStyles.formSectionLabel)

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Current Period", style = AppTextStyles.caption)
                    Switch(
                        checked = config.showCurrentPeriod,
                        onCheckedChange = { callbacks.onUpdate(config.copy(showCurrentPeriod = it)) }
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Previous Period", style = AppTextStyles.caption)
                    Switch(
                        checked = config.showPreviousPeriod,
                        onCheckedChange = { callbacks.onUpdate(config.copy(showPreviousPeriod = it)) }
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Same Period Last Year", style = AppTextStyles.caption)
                    Switch(
                        checked = config.showSamePeriodLastYear,
                        onCheckedChange = { callbacks.onUpdate(config.copy(showSamePeriodLastYear = it)) }
                    )
                }
            }
        },
        shape = RoundedCornerShape(32.dp),
        containerColor = AppColors.sheetSurface
    )
}
