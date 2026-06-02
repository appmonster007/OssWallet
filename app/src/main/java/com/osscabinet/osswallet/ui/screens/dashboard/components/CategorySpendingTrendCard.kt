package com.osscabinet.ossWallet.ui.screens.dashboard.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.osscabinet.ossWallet.domain.card.CardFilterRules
import com.osscabinet.ossWallet.domain.card.CardRenderData
import com.osscabinet.ossWallet.domain.card.CategorySummary
import com.osscabinet.ossWallet.domain.model.CategoryNature
import com.osscabinet.ossWallet.domain.model.TransactionFilter
import com.osscabinet.ossWallet.domain.model.forCategory
import com.osscabinet.ossWallet.domain.model.settings.CategorySpendingTrendCardConfig
import com.osscabinet.ossWallet.domain.model.settings.ChartMetric
import com.osscabinet.ossWallet.domain.service.currency.CurrencyFormatter
import com.osscabinet.ossWallet.domain.service.currency.CurrencyService
import com.osscabinet.ossWallet.ui.components.cards.AeonExpressiveCard
import com.osscabinet.ossWallet.ui.components.cards.CardCallbacks
import com.osscabinet.ossWallet.ui.components.entry.EntrySwitchRow
import com.osscabinet.ossWallet.ui.components.cards.CardSettingsSection
import com.osscabinet.ossWallet.ui.components.cards.CardSettingsSheet
import com.osscabinet.ossWallet.ui.components.cards.FilterSelectionContext
import com.osscabinet.ossWallet.ui.components.visualization.charts.AeonBarChart
import com.osscabinet.ossWallet.ui.components.visualization.charts.AeonBarChartBar
import com.osscabinet.ossWallet.ui.components.visualization.charts.AeonBarChartGroup
import com.osscabinet.ossWallet.ui.theme.AppColors
import com.osscabinet.ossWallet.ui.theme.AppTextStyles
import com.osscabinet.ossWallet.ui.theme.InfoColor
import com.osscabinet.ossWallet.ui.theme.WarningColor
import com.osscabinet.ossWallet.ui.theme.OssWalletTheme
import java.math.BigDecimal

@Composable
fun CategorySpendingTrendCard(
    renderData: CardRenderData.CategorySpendingTrend,
    callbacks: CardCallbacks.CategorySpendingTrend = CardCallbacks.CategorySpendingTrend(),
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
    var categoriesExpanded by rememberSaveable { mutableStateOf(false) }

    val displayFilter =
        if (screenOwnsTimeline || inheritedFilter.timelineRange !is com.osscabinet.ossWallet.domain.model.timeline.TimelineRange.AllTime)
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
            CategorySpendingSettingsSheet(
                config = config,
                callbacks = callbacks,
                onDismiss = onDismiss
            )
        }
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            MetricToggleRow(
                selectedMetric = config.selectedMetric,
                onMetricChange = { callbacks.onUpdate(config.copy(selectedMetric = it)) }
            )

            // Resolve nature colors in composable scope
            val mustColor = AppColors.error
            val needColor = InfoColor
            val wantColor = WarningColor

            // Stacked bar chart
            val chartGroups = buildChartGroups(renderData, mustColor, needColor, wantColor)
            if (chartGroups.isNotEmpty()) {
                AeonBarChart(
                    groups = chartGroups,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(220.dp),
                    currency = renderData.baseCurrency,
                    showGrid = config.showGridLines,
                    showXAxisLabels = true,
                    cornerRadius = 0.dp
                )
            }

            // Nature filters (Must, Need, Want) - Placed below graph, equally spaced
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                CategoryNature.entries.forEach { nature ->
                    val isSelected = nature in config.selectedNatures
                    val natureColor = when (nature) {
                        CategoryNature.MUST -> AppColors.error
                        CategoryNature.NEED -> InfoColor
                        CategoryNature.WANT -> WarningColor
                    }

                    FilterChip(
                        selected = isSelected,
                        onClick = {
                            val newNatures = if (isSelected) {
                                config.selectedNatures - nature
                            } else {
                                config.selectedNatures + nature
                            }
                            callbacks.onUpdate(config.copy(selectedNatures = newNatures))
                        },
                        label = {
                            Box(
                                modifier = Modifier.fillMaxWidth(),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = nature.name.lowercase().replaceFirstChar { it.uppercase() },
                                    style = AppTextStyles.caption.copy(
                                        fontSize = 11.sp,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                    )
                                )
                            }
                        },
                        modifier = Modifier.weight(1f),
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = natureColor.copy(alpha = 0.12f),
                            selectedLabelColor = natureColor,
                            containerColor = Color.Transparent,
                            labelColor = AppColors.contentSecondary.copy(alpha = 0.6f)
                        ),
                        border = FilterChipDefaults.filterChipBorder(
                            borderColor = AppColors.contentSecondary.copy(alpha = 0.2f),
                            selectedBorderColor = natureColor,
                            borderWidth = 1.dp,
                            selectedBorderWidth = 1.dp,
                            enabled = true,
                            selected = isSelected
                        )
                    )
                }
            }

            // Expandable category list header
            if (renderData.activeCategories.isNotEmpty()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .clickable { categoriesExpanded = !categoriesExpanded }
                        .padding(vertical = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Categories (${renderData.activeCategories.size})",
                        style = AppTextStyles.formSectionLabel,
                        color = AppColors.contentPrimary
                    )
                    Icon(
                        imageVector = if (categoriesExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                        contentDescription = if (categoriesExpanded) "Collapse" else "Expand",
                        tint = AppColors.contentSecondary,
                        modifier = Modifier.size(20.dp)
                    )
                }

                AnimatedVisibility(visible = categoriesExpanded) {
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        renderData.activeCategories.forEach { summary ->
                            CategorySummaryRow(
                                summary = summary,
                                currency = renderData.baseCurrency,
                                onClick = {
                                    callbacks.onRecordsClick(displayFilter.forCategory(summary.categoryId))
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun CategorySummaryRow(
    summary: CategorySummary,
    currency: String,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(4.dp))
            .clickable(onClick = onClick)
            .padding(vertical = 4.dp, horizontal = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.weight(1f)
        ) {
            Box(
                modifier = Modifier
                    .size(10.dp)
                    .background(Color(summary.colorHex or 0xFF000000.toInt()), CircleShape)
            )
            Text(
                text = summary.name,
                style = AppTextStyles.transactionSwipeAction,
                color = AppColors.contentPrimary,
                maxLines = 1,
                modifier = Modifier.weight(1f)
            )
            NatureChip(nature = summary.nature)
        }
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = formatCurrency(summary.totalAmount, currency),
            style = AppTextStyles.formSectionLabel,
            color = AppColors.contentPrimary
        )
    }
}

@Composable
private fun formatCurrency(amount: BigDecimal, currency: String): String {
    val formatted = CurrencyFormatter.formatAuto(
        amount = amount,
        currencyCode = currency,
        precision = CurrencyService.precision
    )
    return if (amount.signum() > 0) "+$formatted" else formatted
}

@Composable
private fun MetricToggleRow(
    selectedMetric: ChartMetric,
    onMetricChange: (ChartMetric) -> Unit
) {
    val options = ChartMetric.entries
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
private fun NatureChip(nature: CategoryNature) {
    val (label, color) = when (nature) {
        CategoryNature.MUST -> "Must" to AppColors.error
        CategoryNature.NEED -> "Need" to InfoColor
        CategoryNature.WANT -> "Want" to WarningColor
    }
    Surface(
        shape = RoundedCornerShape(4.dp),
        color = color.copy(alpha = 0.15f)
    ) {
        Text(
            text = label,
            style = AppTextStyles.caption.copy(fontSize = 9.sp),
            color = color,
            modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CategorySpendingSettingsSheet(
    config: CategorySpendingTrendCardConfig,
    callbacks: CardCallbacks.CategorySpendingTrend,
    onDismiss: () -> Unit
) {
    CardSettingsSheet(
        title = "Category Spending",
        onDismiss = onDismiss,
        onDelete = callbacks.onDelete
    ) {
        CardSettingsSection("Display") {
            EntrySwitchRow(
                label = "Show Grid Lines",
                checked = config.showGridLines,
                onCheckedChange = { callbacks.onUpdate(config.copy(showGridLines = it)) },
                isFlush = true
            )
        }
    }
}

@androidx.compose.ui.tooling.preview.Preview(showBackground = true)
@Composable
fun CategorySpendingTrendCardPreview() {
    val renderData = CardRenderData.CategorySpendingTrend(
        config = CategorySpendingTrendCardConfig(cardId = "preview"),
        buckets = listOf(
            com.osscabinet.ossWallet.domain.card.CategorySpendingBucket(
                label = "Jan",
                epochMillis = 1704067200000L,
                categoryAmounts = listOf(
                    com.osscabinet.ossWallet.domain.card.CategoryAmount(
                        1L,
                        "Rent",
                        0xFFE57373.toInt(),
                        CategoryNature.MUST,
                        BigDecimal("-1200.00")
                    ),
                    com.osscabinet.ossWallet.domain.card.CategoryAmount(
                        2L,
                        "Groceries",
                        0xFFFFB74D.toInt(),
                        CategoryNature.NEED,
                        BigDecimal("-400.00")
                    ),
                    com.osscabinet.ossWallet.domain.card.CategoryAmount(
                        3L,
                        "Refund",
                        0xFF81C784.toInt(),
                        CategoryNature.WANT,
                        BigDecimal("50.00")
                    )
                )
            ),
            com.osscabinet.ossWallet.domain.card.CategorySpendingBucket(
                label = "Feb",
                epochMillis = 1706745600000L,
                categoryAmounts = listOf(
                    com.osscabinet.ossWallet.domain.card.CategoryAmount(
                        1L,
                        "Rent",
                        0xFFE57373.toInt(),
                        CategoryNature.MUST,
                        BigDecimal("-1200.00")
                    ),
                    com.osscabinet.ossWallet.domain.card.CategoryAmount(
                        2L,
                        "Groceries",
                        0xFFFFB74D.toInt(),
                        CategoryNature.NEED,
                        BigDecimal("-350.00")
                    ),
                    com.osscabinet.ossWallet.domain.card.CategoryAmount(
                        3L,
                        "Gift",
                        0xFF81C784.toInt(),
                        CategoryNature.WANT,
                        BigDecimal("150.00")
                    )
                )
            )
        ),
        granularity = com.osscabinet.ossWallet.domain.card.CashFlowComparisonGranularity.MONTH,
        activeCategories = listOf(
            CategorySummary(
                1L,
                "Rent",
                0xFFE57373.toInt(),
                CategoryNature.MUST,
                BigDecimal("-2400.00")
            ),
            CategorySummary(
                2L,
                "Groceries",
                0xFFFFB74D.toInt(),
                CategoryNature.NEED,
                BigDecimal("-750.00")
            ),
            CategorySummary(
                3L,
                "Wants (Net)",
                0xFF81C784.toInt(),
                CategoryNature.WANT,
                BigDecimal("200.00")
            )
        ),
        baseCurrency = "USD"
    )

    OssWalletTheme {
        Box(modifier = Modifier.padding(16.dp)) {
            CategorySpendingTrendCard(renderData = renderData)
        }
    }
}

private fun buildChartGroups(
    renderData: CardRenderData.CategorySpendingTrend,
    mustColor: Color,
    needColor: Color,
    wantColor: Color
): List<AeonBarChartGroup> {
    return renderData.buckets.map { bucket ->
        // Aggregate amounts by nature for a cleaner stacked view
        val natureTotals = bucket.categoryAmounts
            .filter { it.amount.signum() != 0 }
            .groupBy { it.nature }
            .map { (nature, amounts) ->
                val total = amounts.map { it.amount }.fold(BigDecimal.ZERO, BigDecimal::add)
                val color = when (nature) {
                    CategoryNature.MUST -> mustColor
                    CategoryNature.NEED -> needColor
                    CategoryNature.WANT -> wantColor
                }
                val label = nature.name.lowercase().replaceFirstChar { it.uppercase() }
                AeonBarChartBar(value = total, label = label, color = color)
            }
            .sortedBy { it.value } // Consistent stacking order

        // Empty buckets get a zero placeholder so the chart always renders axes and labels
        AeonBarChartGroup(
            label = bucket.label,
            bars = natureTotals.ifEmpty { listOf(AeonBarChartBar(BigDecimal.ZERO, "", Color.Transparent)) }
        )
    }
}
