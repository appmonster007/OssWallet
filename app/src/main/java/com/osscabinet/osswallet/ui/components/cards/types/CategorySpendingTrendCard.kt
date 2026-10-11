package com.osscabinet.ossWallet.ui.components.cards.types

import com.osscabinet.ossWallet.domain.model.settings.BarChartViewMode
import com.osscabinet.ossWallet.ui.components.cards.BarChartModeSettings
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.osscabinet.ossWallet.domain.card.CardInput
import com.osscabinet.ossWallet.domain.card.CardRenderData
import com.osscabinet.ossWallet.domain.card.CardUpdateHandler
import com.osscabinet.ossWallet.domain.card.CategorySummary
import com.osscabinet.ossWallet.domain.model.CategoryNature
import com.osscabinet.ossWallet.domain.model.TransactionFilter
import com.osscabinet.ossWallet.domain.model.forCategory
import com.osscabinet.ossWallet.domain.model.settings.CategorySpendingTrendCardConfig
import com.osscabinet.ossWallet.domain.service.currency.CurrencyService
import com.osscabinet.ossWallet.ui.components.AeonDropdownSelector
import com.osscabinet.ossWallet.ui.components.AeonSettingsToggle
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
import com.osscabinet.ossWallet.ui.components.visualization.charts.AeonBarChart
import com.osscabinet.ossWallet.ui.components.visualization.charts.visibleBars
import com.osscabinet.ossWallet.ui.components.visualization.charts.AeonBarChartBar
import com.osscabinet.ossWallet.ui.components.visualization.charts.AeonBarChartGroup
import com.osscabinet.ossWallet.ui.components.visualization.charts.AeonChartLegend
import com.osscabinet.ossWallet.ui.components.visualization.charts.AeonLegendItemData
import com.osscabinet.ossWallet.ui.theme.AppColors
import com.osscabinet.ossWallet.ui.theme.OssWalletTheme
import com.osscabinet.ossWallet.ui.theme.AppTextStyles
import com.osscabinet.ossWallet.ui.theme.InfoColor
import com.osscabinet.ossWallet.ui.theme.WarningColor
import java.math.BigDecimal
import com.osscabinet.ossWallet.ui.util.formatCurrency as formatCurrencyUtil

@Composable
fun CategorySpendingTrendCard(
    renderData: CardRenderData.CategorySpendingTrend,
    callbacks: CardCallbacks.CategorySpendingTrend = CardCallbacks.CategorySpendingTrend(),
    layoutProps: CardLayoutProps = CardLayoutProps(),
) {
    val config = renderData.config
    var categoriesExpanded by rememberSaveable { mutableStateOf(value = false) }
    val currentOnUpdate = rememberUpdatedState(callbacks.onUpdate)

    val selectedMetric = config.selectedMetric
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
            CategorySpendingSettingsContent(
                config = config,
                onUpdate = { currentOnUpdate.value(it) },
            )
        }
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            // --- Gated Heavy Computations ---
            val totalAmount = remember(renderData.activeCategories) {
                renderData.activeCategories.sumOf { it.totalAmount }
            }

            ChartSummarySection(
                total = totalAmount,
                previousTotal = null,
                currency = renderData.currency,
                showComparison = false,
                isAllTime = config.filter.timelineRange is com.osscabinet.ossWallet.domain.model.timeline.TimelineRange.AllTime,
                perspective = config.perspective,
                selectedMetric = selectedMetric,
                onMetricChange = { /* Disabled */ },
                splitTransfers = config.splitTransfers,
                dropdownEnabled = false
            )

            // Resolve nature colors in composable scope
            val mustColor = AppColors.error
            val needColor = InfoColor
            val wantColor = WarningColor

            // Interactive Legend Row (Must, Need, Want)
            CategoryNatureLegendRow(
                selectedNatures = config.selectedNatures,
                isCumulative = config.isCumulative,
                onToggleNature = { nature ->
                    val newNatures = if (config.selectedNatures.contains(nature)) {
                        config.selectedNatures - nature
                    } else {
                        config.selectedNatures + nature
                    }
                    callbacks.onUpdate(config.copy(selectedNatures = newNatures))
                },
                onModeChange = { callbacks.onUpdate(config.copy(isCumulative = it)) }
            )

            // Stacked bar chart
            val chartGroups = remember(renderData, mustColor, needColor, wantColor, config.isCumulative, config.selectedNatures) {
                buildChartGroups(renderData, mustColor, needColor, wantColor, config.isCumulative, config.selectedNatures)
            }
            if (chartGroups.isNotEmpty()) {
                AeonBarChart(
                    groups = chartGroups,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(280.dp),
                    currency = renderData.currency,
                    showGrid = config.showGridLines,
                    showXAxisLabels = true,
                    roundedBars = false,
                    animate = layoutProps.animate,
                    scrollable = config.viewMode == BarChartViewMode.SCROLLABLE,
                    visibleBars = renderData.chartGranularity.visibleBars()
                )
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
                                currency = renderData.currency,
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
private fun CategoryNatureLegendRow(
    selectedNatures: Set<CategoryNature>,
    isCumulative: Boolean,
    onToggleNature: (CategoryNature) -> Unit,
    onModeChange: (Boolean) -> Unit,
) {
    // Resolve colors in composable scope
    val mustColor = AppColors.error
    val needColor = InfoColor
    val wantColor = WarningColor

    val items = remember(selectedNatures, mustColor, needColor, wantColor) {
        CategoryNature.entries.map { nature ->
            val label = nature.name.lowercase().replaceFirstChar { it.uppercase() }
            val color = when (nature) {
                CategoryNature.MUST -> mustColor
                CategoryNature.NEED -> needColor
                CategoryNature.WANT -> wantColor
            }
            AeonLegendItemData(
                color = color,
                label = label,
                isEnabled = nature in selectedNatures,
                onClick = { onToggleNature(nature) },
                metadata = mapOf("nature" to nature)
            )
        }.sortedBy { it.label }
    }

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        AeonChartLegend(
            items = items,
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
    val formatted = formatCurrencyUtil(
        amount = amount,
        currencyCode = currency,
        spec = com.osscabinet.ossWallet.ui.util.CurrencyDisplaySpec(precision = CurrencyService.precision)
    )
    return if (amount.signum() > 0) "+$formatted" else formatted
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

@Composable
private fun CategorySpendingSettingsContent(
    config: CategorySpendingTrendCardConfig,
    onUpdate: (CategorySpendingTrendCardConfig) -> Unit,
) {
    CardSettingsSection("Display") {
        AeonSettingsToggle(
            label = "Show Grid Lines",
            checked = config.showGridLines,
            onCheckedChange = { onUpdate(config.copy(showGridLines = it)) },
            showBackground = false
        )
    }
    BarChartModeSettings(
        viewMode = config.viewMode,
        onViewModeChange = { onUpdate(config.copy(viewMode = it)) },
        granularity = config.scrollGranularity,
        onGranularityChange = { onUpdate(config.copy(scrollGranularity = it)) }
    )
    CardPerspectiveSettings(
        perspective = config.perspective,
        onPerspectiveChange = { onUpdate(config.copy(perspective = it)) },
        excludeInternalTransfers = config.excludeInternalTransfers,
        onExcludeInternalTransfersChange = {
            onUpdate(
                config.copy(
                    excludeInternalTransfers = it
                )
            )
        },
        splitTransfers = config.splitTransfers,
        onSplitTransfersChange = { onUpdate(config.copy(splitTransfers = it)) }
    )
}

/**
 * Maps [CategorySpendingTrendCardConfig] + [CardRenderData.CategorySpendingTrend] to [CardWithCallbacks.CategorySpendingTrend].
 */
fun mapCategorySpendingTrend(
    config: CategorySpendingTrendCardConfig,
    renderData: CardRenderData.CategorySpendingTrend,
    input: CardInput,
    updateHandler: CardUpdateHandler,
    onDelete: ((String) -> Unit)?,
    onRecordsClick: (TransactionFilter) -> Unit,
    callbacks: AeonInteractionCallbacks,
    filterSheetEnabled: Boolean
) = CardWithCallbacks.CategorySpendingTrend(
    renderData = renderData,
    callbacks = CardCallbacks.CategorySpendingTrend(
        onUpdate = { newConfig ->
            updateHandler.updateCategorySpendingTrendConfig(
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
            com.osscabinet.ossWallet.domain.card.CategorySummary(
                2L,
                "Groceries",
                0xFFFFB74D.toInt(),
                CategoryNature.NEED,
                BigDecimal("-750.00")
            ),
            com.osscabinet.ossWallet.domain.card.CategorySummary(
                3L,
                "Wants (Net)",
                0xFF81C784.toInt(),
                CategoryNature.WANT,
                BigDecimal("200.00")
            )
        ),
        currency = "USD"
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
    wantColor: Color,
    isCumulative: Boolean,
    selectedNatures: Set<CategoryNature>
): List<AeonBarChartGroup> {
    val runningTotals = mutableMapOf<CategoryNature, BigDecimal>()
    
    val baseGroups = renderData.buckets.map { bucket ->
        val finalBars = mutableListOf<AeonBarChartBar>()

        CategoryNature.entries.forEach { nature ->
            val natureAmount = bucket.categoryAmounts.asSequence()
                .filter { it.nature == nature }
                .map { it.amount }
                .fold(BigDecimal.ZERO, BigDecimal::add)

            if (isCumulative) {
                val prevRunning = runningTotals[nature] ?: BigDecimal.ZERO
                val newRunning = prevRunning.add(natureAmount)
                runningTotals[nature] = newRunning
            }

            val displayValue = if (isCumulative) {
                runningTotals[nature] ?: BigDecimal.ZERO
            } else {
                natureAmount
            }

            // Only add the bar if it's in the selected natures AND has a value
            if (nature in selectedNatures && displayValue.signum() != 0) {
                val color = when (nature) {
                    CategoryNature.MUST -> mustColor
                    CategoryNature.NEED -> needColor
                    CategoryNature.WANT -> wantColor
                }
                val label = nature.name.lowercase().replaceFirstChar { it.uppercase() }
                finalBars.add(AeonBarChartBar(value = displayValue, label = label, color = color))
            }
        }

        AeonBarChartGroup(
            label = bucket.label,
            bars = finalBars.sortedBy { it.value.abs() }
        )
    }

    return if (isCumulative) {
        listOf(AeonBarChartGroup("", emptyList())) + baseGroups
    } else {
        baseGroups
    }
}
