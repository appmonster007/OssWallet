package com.osscabinet.ossWallet.ui.components.cards.types

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ShowChart
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.osscabinet.ossWallet.domain.card.CardInput
import com.osscabinet.ossWallet.domain.card.CardRenderData
import com.osscabinet.ossWallet.domain.card.CardUpdateHandler
import com.osscabinet.ossWallet.domain.card.CashFlowComparisonSeries
import com.osscabinet.ossWallet.domain.model.TransactionFilter
import com.osscabinet.ossWallet.domain.model.settings.CashFlowComparisonCardConfig
import com.osscabinet.ossWallet.domain.model.settings.ChartMetric
import com.osscabinet.ossWallet.domain.service.currency.CurrencyService
import com.osscabinet.ossWallet.ui.components.AeonSettingsToggle
import com.osscabinet.ossWallet.ui.components.cards.AeonCardSettingsScaffold
import com.osscabinet.ossWallet.ui.components.cards.AeonExpressiveCard
import com.osscabinet.ossWallet.ui.components.cards.AeonInteractionCallbacks
import com.osscabinet.ossWallet.ui.components.cards.AeonMetricToggle
import com.osscabinet.ossWallet.ui.components.cards.CardCallbacks
import com.osscabinet.ossWallet.ui.components.cards.CardLayoutProps
import com.osscabinet.ossWallet.ui.components.cards.CardPerspectiveSettings
import com.osscabinet.ossWallet.ui.components.cards.CardSettingsSection
import com.osscabinet.ossWallet.ui.components.cards.CardWithCallbacks
import com.osscabinet.ossWallet.ui.components.cards.resolveDrillDown
import com.osscabinet.ossWallet.ui.components.visualization.charts.AeonChartMarker
import com.osscabinet.ossWallet.ui.components.visualization.charts.AeonLineChart
import com.osscabinet.ossWallet.ui.components.visualization.charts.AeonLineChartPoint
import com.osscabinet.ossWallet.ui.components.visualization.charts.AeonLineChartSeries
import com.osscabinet.ossWallet.ui.theme.AppColors
import com.osscabinet.ossWallet.ui.theme.AppTextStyles
import com.osscabinet.ossWallet.ui.theme.OssWalletTheme
import com.osscabinet.ossWallet.ui.theme.ChartColorBlue
import com.osscabinet.ossWallet.ui.theme.ChartColorGreen
import com.osscabinet.ossWallet.ui.theme.ChartColorOrange
import com.osscabinet.ossWallet.ui.util.CurrencyDisplaySpec
import com.osscabinet.ossWallet.ui.util.formatCurrency
import java.math.BigDecimal

private val CurrentPeriodColor = ChartColorBlue
private val PreviousPeriodColor = ChartColorOrange
private val LastYearColor = ChartColorGreen

@Composable
fun CashFlowComparisonCard(
    renderData: CardRenderData.CashFlowComparison,
    callbacks: CardCallbacks.CashFlowComparison = CardCallbacks.CashFlowComparison(),
    layoutProps: CardLayoutProps = CardLayoutProps(),
) {
    val config = renderData.config

    var selectedMetric by rememberSaveable { mutableStateOf(config.selectedMetric) }
    LaunchedEffect(config.selectedMetric) { selectedMetric = config.selectedMetric }

    val displayFilter = layoutProps.computeDisplayFilter(config.filter)

    AeonExpressiveCard(
        title = config.title,
        icon = Icons.AutoMirrored.Filled.ShowChart,
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
            CashFlowComparisonSettingsContent(
                config = config,
                callbacks = callbacks,
            )
        }
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            // Keep the selected metric coherent with the split-transfers setting so a stale
            // TRF / TRF IN selection doesn't leave the toggle with nothing highlighted.
            val effectiveMetric = coerceTransferMetric(selectedMetric, config.splitTransfers)

            // Metric toggle row — purely display-local; no config persist needed
            AeonMetricToggle(
                perspective = config.perspective,
                selectedMetric = effectiveMetric,
                onMetricChange = { metric -> selectedMetric = metric },
                splitTransfers = config.splitTransfers
            )

            // Aggregate sum for current period
            val aggregateTotal = renderData.currentSeries.points.fold(BigDecimal.ZERO) { acc, p ->
                acc + when (effectiveMetric) {
                    ChartMetric.NET -> p.net
                    ChartMetric.INCOME -> p.income
                    ChartMetric.EXPENSE -> p.expense
                    ChartMetric.TRANSFER -> p.transfer
                    ChartMetric.TRANSFER_IN -> p.transferIn
                    ChartMetric.TRANSFER_OUT -> p.transferOut
                }
            }
            Text(
                text = formatCurrency(
                    amount = aggregateTotal,
                    currencyCode = renderData.currency,
                    spec = CurrencyDisplaySpec(precision = CurrencyService.precision)
                ),
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
                selectedMetric = effectiveMetric,
                isCumulative = config.isCumulative,
                renderData = renderData
            )

            if (series.isNotEmpty()) {
                val maxXBuckets = maxOf(
                    renderData.currentTotalBuckets,
                    renderData.previousTotalBuckets,
                    renderData.lastYearTotalBuckets
                )
                // Use maxXBuckets + 1 for cumulative view to account for zero origin point.
                val totalXSlots = if (config.isCumulative) maxXBuckets + 1 else maxXBuckets

                // Labels: Prepend null for origin point in cumulative mode to avoid label repetition.
                val xAxisLabels: List<String?> = if (config.isCumulative) {
                    listOf(null) + renderData.periodLabels
                } else {
                    renderData.periodLabels
                }

                val markers = buildMarkersList(renderData, config)

                AeonLineChart(
                    series = series,
                    currency = renderData.currency,
                    lineThicknessDp = 1.5f,
                    showGrid = config.showGridLines,
                    totalXSlots = totalXSlots,
                    xAxisLabels = xAxisLabels,
                    markers = markers,
                    animate = layoutProps.animate,
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
 * Keeps a saved metric selection valid when the split-transfers setting changes:
 * TRF ↔ TRF IN so the toggle always has a highlighted option.
 */
private fun coerceTransferMetric(metric: ChartMetric, splitTransfers: Boolean): ChartMetric = when {
    splitTransfers && metric == ChartMetric.TRANSFER -> ChartMetric.TRANSFER_IN
    !splitTransfers && (metric == ChartMetric.TRANSFER_IN || metric == ChartMetric.TRANSFER_OUT) -> ChartMetric.TRANSFER
    else -> metric
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
        androidx.compose.foundation.Canvas(modifier = Modifier.size(width = 16.dp, height = 3.dp)) {
            drawLine(
                color = color.copy(alpha = alpha),
                start = androidx.compose.ui.geometry.Offset(0f, size.height / 2),
                end = androidx.compose.ui.geometry.Offset(size.width, size.height / 2),
                strokeWidth = size.height,
                pathEffect = if (isDashed) androidx.compose.ui.graphics.PathEffect.dashPathEffect(
                    floatArrayOf(4f, 3f)
                ) else null,
                cap = androidx.compose.ui.graphics.StrokeCap.Round
            )
        }
        Text(
            text = label,
            style = AppTextStyles.caption.copy(fontSize = 10.sp),
            color = AppColors.contentSecondary.copy(alpha = alpha)
        )
    }
}

private fun buildSeriesList(
    config: CashFlowComparisonCardConfig,
    selectedMetric: ChartMetric,
    isCumulative: Boolean,
    renderData: CardRenderData.CashFlowComparison
): List<AeonLineChartSeries> {
    val series = mutableListOf<AeonLineChartSeries>()

    fun seriesPoints(s: CashFlowComparisonSeries): List<AeonLineChartPoint> {
        var running = BigDecimal.ZERO
        val dataPoints = s.points.mapIndexed { index, point ->
            val value = when (selectedMetric) {
                ChartMetric.NET -> point.net
                ChartMetric.INCOME -> point.income
                ChartMetric.EXPENSE -> point.expense
                ChartMetric.TRANSFER -> point.transfer
                ChartMetric.TRANSFER_IN -> point.transferIn
                ChartMetric.TRANSFER_OUT -> point.transferOut
            }
            running += value
            AeonLineChartPoint(
                x = if (isCumulative) (index + 1).toFloat() else index.toFloat(),
                y = if (isCumulative) running else value,
                label = point.label
            )
        }
        return if (isCumulative) {
            listOf(AeonLineChartPoint(x = 0f, y = BigDecimal.ZERO, label = null)) + dataPoints
        } else {
            dataPoints
        }
    }

    if (config.showCurrentPeriod && renderData.currentSeries.points.isNotEmpty()) {
        series.add(
            AeonLineChartSeries(
                name = "Current",
                color = CurrentPeriodColor,
                dataPoints = seriesPoints(renderData.currentSeries),
                fillGradient = true,
                showEndDot = true
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
                isDashed = true
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
                isDashed = true
            )
        )
    }

    return series
}

private fun buildMarkersList(
    renderData: CardRenderData.CashFlowComparison,
    config: CashFlowComparisonCardConfig
): List<AeonChartMarker> {
    val markers = mutableListOf<AeonChartMarker>()

    // Show a period-end marker for each enabled series.
    // For the current period, we mark 'today' (where the line terminates).
    // For previous/last-year periods, we mark the end of their respective periods.
    fun addMarker(show: Boolean, position: Int, label: String, color: Color) {
        if (!show || (position <= 0)) return
        markers.add(AeonChartMarker(position.toFloat(), label, color, isDashed = true))
    }

    val currentMarkerPos = if (renderData.currentSeries.points.isNotEmpty()) {
        renderData.currentSeries.points.size
    } else {
        // If the period is entirely in the future, we have no 'today' point to mark yet.
        0
    }

    addMarker(config.showCurrentPeriod, currentMarkerPos, "Current", CurrentPeriodColor)

    // Only show Prev/LY markers if those periods have actually started/occurred.
    if (renderData.previousSeries.points.isNotEmpty()) {
        addMarker(
            config.showPreviousPeriod,
            renderData.previousSeries.points.size,
            "Prev",
            PreviousPeriodColor
        )
    }
    if (renderData.lastYearSeries.points.isNotEmpty()) {
        addMarker(
            config.showSamePeriodLastYear,
            renderData.lastYearSeries.points.size,
            "LY",
            LastYearColor
        )
    }

    return markers
}

@Composable
private fun CashFlowComparisonSettingsContent(
    config: CashFlowComparisonCardConfig,
    callbacks: CardCallbacks.CashFlowComparison,
) {
    CardSettingsSection("Display") {
        AeonSettingsToggle(
            label = "Cumulative",
            checked = config.isCumulative,
            onCheckedChange = { callbacks.onUpdate(config.copy(isCumulative = it)) },
            showBackground = false
        )
        AeonSettingsToggle(
            label = "Show Grid Lines",
            checked = config.showGridLines,
            onCheckedChange = { callbacks.onUpdate(config.copy(showGridLines = it)) },
            showBackground = false
        )
    }
    CardSettingsSection("Series") {
        AeonSettingsToggle(
            label = "Current Period",
            checked = config.showCurrentPeriod,
            onCheckedChange = { callbacks.onUpdate(config.copy(showCurrentPeriod = it)) },
            showBackground = false
        )
        AeonSettingsToggle(
            label = "Previous Period",
            checked = config.showPreviousPeriod,
            onCheckedChange = { callbacks.onUpdate(config.copy(showPreviousPeriod = it)) },
            showBackground = false
        )
        AeonSettingsToggle(
            label = "Same Period Last Year",
            checked = config.showSamePeriodLastYear,
            onCheckedChange = { callbacks.onUpdate(config.copy(showSamePeriodLastYear = it)) },
            showBackground = false
        )
    }
    CardPerspectiveSettings(
        perspective = config.perspective,
        onPerspectiveChange = { callbacks.onUpdate(config.copy(perspective = it)) },
        excludeInternalTransfers = config.excludeInternalTransfers,
        onExcludeInternalTransfersChange = {
            callbacks.onUpdate(
                config.copy(
                    excludeInternalTransfers = it,
                ),
            )
        },
        splitTransfers = config.splitTransfers,
        onSplitTransfersChange = { callbacks.onUpdate(config.copy(splitTransfers = it)) },
    )
}

/**
 * Maps [CashFlowComparisonCardConfig] + [CardRenderData.CashFlowComparison] to [CardWithCallbacks.CashFlowComparison].
 */
fun mapCashFlowComparison(
    config: CashFlowComparisonCardConfig,
    renderData: CardRenderData.CashFlowComparison,
    input: CardInput,
    updateHandler: CardUpdateHandler,
    onDelete: ((String) -> Unit)?,
    onRecordsClick: (TransactionFilter) -> Unit,
    callbacks: AeonInteractionCallbacks,
    filterSheetEnabled: Boolean
) = CardWithCallbacks.CashFlowComparison(
    renderData = renderData,
    callbacks = CardCallbacks.CashFlowComparison(
        onUpdate = { newConfig ->
            updateHandler.updateCashFlowComparisonConfig(
                config.cardId,
                newConfig
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

@androidx.compose.ui.tooling.preview.Preview(
    showBackground = true,
    name = "Shorter Previous Period"
)
@Composable
fun CashFlowComparisonShorterPrevPreview() {
    val renderData = CardRenderData.CashFlowComparison(
        config = CashFlowComparisonCardConfig(
            cardId = "preview_shorter",
            title = "Shorter Previous Period",
            showCurrentPeriod = true,
            showPreviousPeriod = true,
            showSamePeriodLastYear = false
        ),
        currentSeries = CashFlowComparisonSeries(
            listOf(
                com.osscabinet.ossWallet.domain.card.CashFlowTimePoint(
                    "01/02",
                    0,
                    BigDecimal("100"),
                    BigDecimal("50"),
                    BigDecimal.ZERO,
                    BigDecimal("50")
                ),
                com.osscabinet.ossWallet.domain.card.CashFlowTimePoint(
                    "02/02",
                    0,
                    BigDecimal("120"),
                    BigDecimal("60"),
                    BigDecimal.ZERO,
                    BigDecimal("60")
                ),
                com.osscabinet.ossWallet.domain.card.CashFlowTimePoint(
                    "03/02",
                    0,
                    BigDecimal("150"),
                    BigDecimal("70"),
                    BigDecimal.ZERO,
                    BigDecimal("80")
                )
            )
        ),
        previousSeries = CashFlowComparisonSeries(
            listOf(
                com.osscabinet.ossWallet.domain.card.CashFlowTimePoint(
                    "01/01",
                    0,
                    BigDecimal("80"),
                    BigDecimal("40"),
                    BigDecimal.ZERO,
                    BigDecimal("40")
                ),
                com.osscabinet.ossWallet.domain.card.CashFlowTimePoint(
                    "02/01",
                    0,
                    BigDecimal("90"),
                    BigDecimal("45"),
                    BigDecimal.ZERO,
                    BigDecimal("45")
                )
            )
        ),
        lastYearSeries = CashFlowComparisonSeries(emptyList()),
        currency = "USD",
        granularity = com.osscabinet.ossWallet.domain.card.CashFlowComparisonGranularity.DAY,
        periodLabels = listOf("01/02", "02/02", "03/02", "04/02", "05/02"),
        currentTotalBuckets = 5,
        previousTotalBuckets = 2,
        lastYearTotalBuckets = 0
    )

    OssWalletTheme {
        Box(modifier = Modifier.padding(16.dp)) {
            CashFlowComparisonCard(renderData = renderData)
        }
    }
}

@androidx.compose.ui.tooling.preview.Preview(showBackground = true, name = "Longer Previous Period")
@Composable
fun CashFlowComparisonLongerPrevPreview() {
    val renderData = CardRenderData.CashFlowComparison(
        config = CashFlowComparisonCardConfig(
            cardId = "preview_longer",
            title = "Longer Previous Period",
            showCurrentPeriod = true,
            showPreviousPeriod = true,
            showSamePeriodLastYear = false
        ),
        currentSeries = CashFlowComparisonSeries(
            listOf(
                com.osscabinet.ossWallet.domain.card.CashFlowTimePoint(
                    "01/02",
                    0,
                    BigDecimal("100"),
                    BigDecimal("50"),
                    BigDecimal.ZERO,
                    BigDecimal("50")
                ),
                com.osscabinet.ossWallet.domain.card.CashFlowTimePoint(
                    "02/02",
                    0,
                    BigDecimal("120"),
                    BigDecimal("60"),
                    BigDecimal.ZERO,
                    BigDecimal("60")
                )
            )
        ),
        previousSeries = CashFlowComparisonSeries(
            listOf(
                com.osscabinet.ossWallet.domain.card.CashFlowTimePoint(
                    "01/01",
                    0,
                    BigDecimal("80"),
                    BigDecimal("40"),
                    BigDecimal.ZERO,
                    BigDecimal("40")
                ),
                com.osscabinet.ossWallet.domain.card.CashFlowTimePoint(
                    "02/01",
                    0,
                    BigDecimal("90"),
                    BigDecimal("45"),
                    BigDecimal.ZERO,
                    BigDecimal("45")
                ),
                com.osscabinet.ossWallet.domain.card.CashFlowTimePoint(
                    "03/01",
                    0,
                    BigDecimal("100"),
                    BigDecimal("50"),
                    BigDecimal.ZERO,
                    BigDecimal("50")
                ),
                com.osscabinet.ossWallet.domain.card.CashFlowTimePoint(
                    "04/01",
                    0,
                    BigDecimal("110"),
                    BigDecimal("55"),
                    BigDecimal.ZERO,
                    BigDecimal("55")
                ),
                com.osscabinet.ossWallet.domain.card.CashFlowTimePoint(
                    "05/01",
                    0,
                    BigDecimal("120"),
                    BigDecimal("60"),
                    BigDecimal.ZERO,
                    BigDecimal("60")
                )
            )
        ),
        lastYearSeries = CashFlowComparisonSeries(emptyList()),
        currency = "USD",
        granularity = com.osscabinet.ossWallet.domain.card.CashFlowComparisonGranularity.DAY,
        periodLabels = listOf("01/02", "02/02", "03/02", "04/02", "05/02"),
        currentTotalBuckets = 5,
        previousTotalBuckets = 5,
        lastYearTotalBuckets = 0
    )

    OssWalletTheme {
        Box(modifier = Modifier.padding(16.dp)) {
            CashFlowComparisonCard(renderData = renderData)
        }
    }
}

@androidx.compose.ui.tooling.preview.Preview(
    showBackground = true,
    name = "Overlapping Markers (Tight)"
)
@Composable
fun CashFlowComparisonTightMarkersPreview() {
    val renderData = CardRenderData.CashFlowComparison(
        config = CashFlowComparisonCardConfig(
            cardId = "preview_tight",
            title = "Tight Markers",
            showCurrentPeriod = true,
            showPreviousPeriod = true,
            showSamePeriodLastYear = true
        ),
        currentSeries = CashFlowComparisonSeries(
            listOf(
                com.osscabinet.ossWallet.domain.card.CashFlowTimePoint(
                    "28/02",
                    0,
                    BigDecimal("120"),
                    BigDecimal("60"),
                    BigDecimal.ZERO,
                    BigDecimal("60")
                )
            )
        ),
        previousSeries = CashFlowComparisonSeries(
            listOf(
                com.osscabinet.ossWallet.domain.card.CashFlowTimePoint(
                    "29/01",
                    0,
                    BigDecimal("100"),
                    BigDecimal("50"),
                    BigDecimal.ZERO,
                    BigDecimal("50")
                )
            )
        ),
        lastYearSeries = CashFlowComparisonSeries(
            listOf(
                com.osscabinet.ossWallet.domain.card.CashFlowTimePoint(
                    "01/03",
                    0,
                    BigDecimal("100"),
                    BigDecimal("50"),
                    BigDecimal.ZERO,
                    BigDecimal("50")
                )
            )
        ),
        currency = "USD",
        granularity = com.osscabinet.ossWallet.domain.card.CashFlowComparisonGranularity.DAY,
        periodLabels = (1..30).map { "$it/02" },
        currentTotalBuckets = 28,
        previousTotalBuckets = 29,
        lastYearTotalBuckets = 30
    )

    OssWalletTheme {
        Box(modifier = Modifier.padding(16.dp)) {
            CashFlowComparisonCard(renderData = renderData)
        }
    }
}
