package com.osscabinet.ossWallet.ui.components.cards.types

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.rounded.PieChart
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.osscabinet.ossWallet.domain.card.CardInput
import com.osscabinet.ossWallet.domain.card.CardRenderData
import com.osscabinet.ossWallet.domain.card.CardUpdateHandler
import com.osscabinet.ossWallet.domain.model.TransactionFilter
import com.osscabinet.ossWallet.domain.model.settings.CategoryPieChartCardConfig
import com.osscabinet.ossWallet.domain.model.settings.ChartMetric
import com.osscabinet.ossWallet.domain.model.timeline.TimelineRange
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
import com.osscabinet.ossWallet.ui.components.visualization.charts.AeonPieChart
import com.osscabinet.ossWallet.ui.components.visualization.charts.AeonPieChartSlice
import com.osscabinet.ossWallet.ui.theme.AppColors
import com.osscabinet.ossWallet.ui.theme.AppTextStyles
import com.osscabinet.ossWallet.ui.theme.LocalThemeMode
import com.osscabinet.ossWallet.ui.theme.OssWalletTheme
import com.osscabinet.ossWallet.ui.util.formatCurrency
import com.osscabinet.ossWallet.ui.util.generateShade
import com.osscabinet.ossWallet.ui.util.toChartColor
import java.math.BigDecimal
import java.math.RoundingMode

// TODO(card-upgrade): make the 2% 'More Categories' threshold a config setting (Off / 1% / 2% / 5%), default 2%.
// TODO(card-upgrade): legend rows (5 / 10 / All) and sort (Amount / Biggest change / A-Z); replace the hardcoded take(5).
// TODO(card-upgrade): show per-category change vs previous period on legend rows (previousAmount is already loaded).
// TODO(card-upgrade): extract the grouping logic shared with CategoryTreemapCard into one tested function first.
// See docs/guides/card-upgrade-roadmap.md
@Composable
fun CategoryPieChartCard(
    config: CategoryPieChartCardConfig,
    data: List<CategoryChartData>,
    currency: String,
    callbacks: CardCallbacks.Analytics = CardCallbacks.Analytics(),
    layoutProps: CardLayoutProps = CardLayoutProps(),
) {
    val displayFilter = layoutProps.computeDisplayFilter(config.filter)

    val mode = LocalThemeMode.current
    val isDarkTheme = mode.isDarkTheme
    val isAmoled = mode.isAmoled

    val filteredData = if (config.showOnlyActiveCategories) {
        remember(data) { data.filter { it.amount.signum() != 0 } }
    } else data

    var navigationStack by remember(filteredData) {
        mutableStateOf(listOf(filteredData))
    }
    val currentData = navigationStack.last()

    val currentOnUpdate = rememberUpdatedState(callbacks.onUpdate)

    AeonExpressiveCard(
        title = config.title,
        icon = Icons.Rounded.PieChart,
        modifier = layoutProps.modifier,
        headerModifier = layoutProps.headerModifier,
        filterEnabled = layoutProps.showFilterButton,
        filter = displayFilter,
        baseFilter = layoutProps.baseFilter,
        filterRules = layoutProps.filterRules ?: config.filterRules,
        filterContext = layoutProps.filterContext,
        onFilterChange = { callbacks.onUpdate(config.copy(filter = it)) },
        onSaveFilterClick = callbacks.onSaveFilterClick,
        onDelete = callbacks.onDelete,
        settingsContent = {
            CategoryPieChartSettingsContent(
                config = config,
                onUpdate = { currentOnUpdate.value(it) },
            )
        }
    ) {
        // --- Gated Heavy Computations ---
        // These only run when the front side is actually visible.
        val groupedData = remember(currentData, navigationStack.size) {
            val sorted = currentData.sortedByDescending { it.amount.abs() }

            val isMoreCategoriesDrillDown = if (navigationStack.size > 1) {
                val prevLevelRawData = navigationStack[navigationStack.size - 2]
                prevLevelRawData.none { it.subcategoryData === currentData }
            } else false

            if (!isMoreCategoriesDrillDown) {
                // TODO(card-upgrade): hardcoded 2% threshold; read it from config (see roadmap).
                val mainItems = sorted.filter { it.percentage >= 0.02f }
                val others =
                    sorted.filter { (it.percentage < 0.02f) && (it.amount.abs() > BigDecimal.ZERO) }

                if (others.size > 1) {
                    val otherSum = others.sumOf { it.amount }
                    val otherPercentage = others.sumOf { it.percentage.toDouble() }.toFloat()
                    val otherPrevSum = if (others.any { it.previousAmount != null }) {
                        others.sumOf { it.previousAmount ?: BigDecimal.ZERO }
                    } else null

                    mainItems + CategoryChartData(
                        category = "More Categories",
                        categoryId = -1L,
                        amount = otherSum,
                        colorHex = 0xFF9E9E9E.toInt(), // Grey
                        percentage = otherPercentage,
                        hasSubcategories = true,
                        subcategoryCount = others.size,
                        subcategoryData = others,
                        previousAmount = otherPrevSum
                    )
                } else {
                    sorted
                }
            } else {
                sorted
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        if (navigationStack.size > 1) {
            Row(
                modifier = Modifier
                    .padding(bottom = 12.dp)
                    .horizontalScroll(rememberScrollState()),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = { navigationStack = navigationStack.take(1) },
                    modifier = Modifier.size(24.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Home,
                        contentDescription = "Home",
                        modifier = Modifier.size(16.dp),
                        tint = AppColors.primary
                    )
                }

                navigationStack.drop(1).forEachIndexed { index, stack ->
                    Icon(
                        imageVector = Icons.Default.ChevronRight,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                        tint = AppColors.border
                    )

                    val prevStack = navigationStack[index]
                    val name = if (prevStack.any { it.subcategoryData === stack }) {
                        prevStack.find { it.subcategoryData === stack }?.category ?: "Sub"
                    } else {
                        "More Categories"
                    }

                    TextButton(
                        onClick = { navigationStack = navigationStack.take(index + 2) },
                        contentPadding = PaddingValues(horizontal = 4.dp, vertical = 0.dp),
                        modifier = Modifier.height(24.dp)
                    ) {
                        Text(
                            name.uppercase(),
                            style = AppTextStyles.transactionSwipeAction.copy(
                                fontWeight = FontWeight.Bold,
                                fontSize = 10.sp
                            )
                        )
                    }
                }
            }
        }

        val total = remember(currentData) { currentData.sumOf { it.amount } }
        val previousTotalValue = remember(currentData) {
            if (currentData.any { it.previousAmount != null }) {
                currentData.sumOf { it.previousAmount ?: BigDecimal.ZERO }
            } else null
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(280.dp)
        ) {
            if (groupedData.isNotEmpty()) {
                val slices = groupedData.mapIndexed { index, item ->
                    val baseColor = if (navigationStack.size > 1) {
                        val prevStack = navigationStack[navigationStack.size - 2]
                        val parentItem = prevStack.find { it.subcategoryData === currentData }
                        parentItem?.colorHex?.let { Color(it) } ?: Color(item.colorHex)
                    } else {
                        Color(item.colorHex)
                    }

                    val displayColor = if (navigationStack.size > 1) {
                        baseColor.generateShade(index, groupedData.size, isDarkTheme, isAmoled)
                    } else {
                        baseColor.toChartColor(isDarkTheme, isAmoled)
                    }

                    val sliceValue = item.amount
                    val globalMagnitude = data.sumOf { it.amount.abs() }

                    AeonPieChartSlice(
                        value = sliceValue,
                        label = item.category,
                        color = displayColor,
                        hasSubcategories = item.hasSubcategories,
                        subcategoryCount = item.subcategoryCount,
                        percentageOfTotal = if (globalMagnitude.signum() != 0)
                            sliceValue.abs().divide(globalMagnitude, 4, RoundingMode.HALF_UP)
                                .toFloat()
                        else 0f,
                        id = item.categoryId
                    )
                }

                AeonPieChart(
                    slices = slices,
                    modifier = Modifier.fillMaxSize(),
                    rounded = config.rounded,
                    currency = currency,
                    animate = layoutProps.animate,
                    showCenterTotal = false,
                    previousTotal = previousTotalValue,
                    showComparison = config.showComparison,
                    isAllTime = config.filter.timelineRange is TimelineRange.AllTime,
                    positiveDeltaIsGood = false,
                    onSliceClick = {},
                    onDrillInClick = { slice ->
                        val clickedItem = groupedData.find { it.category == slice.label }
                        if (clickedItem?.hasSubcategories == true && !clickedItem.subcategoryData.isNullOrEmpty()) {
                            navigationStack += listOf(clickedItem.subcategoryData)
                        }
                    },
                    onNavigateToRecords = { slice ->
                        val clickedItem = groupedData.find { it.category == slice.label }
                        val categoryIds =
                            clickedItem?.allCategoryIds() ?: listOfNotNull(slice.id?.toString())

                        callbacks.onRecordsClick(
                            config.filter.copy(
                                categoryIds = categoryIds.asSequence().map { it.toLong() }.toSet()
                            )
                        )
                    }
                )
            } else {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(
                        "No category data",
                        style = AppTextStyles.caption,
                        color = AppColors.contentSecondary
                    )
                }
            }
        }

        if (groupedData.isNotEmpty()) {
            Spacer(modifier = Modifier.height(16.dp))

            ChartSummarySection(
                total = total,
                previousTotal = previousTotalValue,
                currency = currency,
                showComparison = config.showComparison,
                isAllTime = config.filter.timelineRange is TimelineRange.AllTime,
                perspective = config.perspective,
                selectedMetric = config.selectedMetric,
                onMetricChange = { callbacks.onUpdate(config.copy(selectedMetric = it)) },
                splitTransfers = config.splitTransfers
            )

            if (config.showLegend) {
                Spacer(modifier = Modifier.height(16.dp))
                PieLegend(
                    data = groupedData,
                    currency = currency,
                    isDarkTheme = isDarkTheme,
                    isAmoled = isAmoled,
                    showFullBreakdown = config.showFullBreakdown,
                    onToggleBreakdown = { callbacks.onUpdate(config.copy(showFullBreakdown = it)) },
                    onItemClick = { item ->
                        if (item.hasSubcategories && !item.subcategoryData.isNullOrEmpty()) {
                            navigationStack += listOf(item.subcategoryData)
                        } else {
                            val categoryIds = item.allCategoryIds()
                            callbacks.onRecordsClick(
                                config.filter.copy(
                                    categoryIds = categoryIds.asSequence().map { it.toLong() }.toSet()
                                )
                            )
                        }
                    }
                )
            }
        }
    }
}


@Composable
private fun CategoryPieChartSettingsContent(
    config: CategoryPieChartCardConfig,
    onUpdate: (CategoryPieChartCardConfig) -> Unit,
) {
    CardSettingsSection("Visuals") {
        AeonSettingsToggle(
            "Show Labels",
            config.showLabels,
            description = "Display category names on segments",
            onCheckedChange = { onUpdate(config.copy(showLabels = it)) },
            showBackground = false
        )
        AeonSettingsToggle(
            "Show Legend",
            config.showLegend,
            description = "Display list of categories beneath the map",
            onCheckedChange = { onUpdate(config.copy(showLegend = it)) },
            showBackground = false
        )
        AeonSettingsToggle(
            "Rounded Visuals",
            config.rounded,
            description = "Apply rounded corners and spacing to segments",
            onCheckedChange = { onUpdate(config.copy(rounded = it)) },
            showBackground = false
        )
    }
    CardSettingsSection("Data") {
        AeonSettingsToggle(
            "Full Breakdown",
            config.showFullBreakdown,
            description = "Show all categories instead of top 5",
            onCheckedChange = { onUpdate(config.copy(showFullBreakdown = it)) },
            showBackground = false
        )
        AeonSettingsToggle(
            "Active Only",
            config.showOnlyActiveCategories,
            description = "Only show categories with non-zero values",
            onCheckedChange = { onUpdate(config.copy(showOnlyActiveCategories = it)) },
            showBackground = false
        )
    }
    CardPerspectiveSettings(
        perspective = config.perspective,
        onPerspectiveChange = { onUpdate(config.copy(perspective = it)) },
        excludeInternalTransfers = config.excludeInternalTransfers,
        onExcludeInternalTransfersChange = { onUpdate(config.copy(excludeInternalTransfers = it)) },
        splitTransfers = config.splitTransfers,
        onSplitTransfersChange = { onUpdate(config.copy(splitTransfers = it)) }
    )
}


@Composable
fun PieLegend(
    data: List<CategoryChartData>,
    currency: String,
    isDarkTheme: Boolean,
    isAmoled: Boolean,
    showFullBreakdown: Boolean,
    onToggleBreakdown: (Boolean) -> Unit,
    onItemClick: (CategoryChartData) -> Unit
) {
    // TODO(card-upgrade): hardcoded 5 rows and amount order; add row count and sort settings (see roadmap).
    val displayData = if (showFullBreakdown) data else data.take(5)

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        displayData.forEach { item ->
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onItemClick(item) }
            ) {
                Surface(
                    modifier = Modifier.size(10.dp),
                    shape = RoundedCornerShape(2.dp),
                    color = Color(item.colorHex).toChartColor(isDarkTheme, isAmoled),
                    border = BorderStroke(0.5.dp, AppColors.divider.copy(alpha = 0.3f))
                ) {}

                Text(
                    text = item.category,
                    style = AppTextStyles.drawerItemLabel.copy(
                        fontWeight = FontWeight.Medium,
                        fontSize = 12.sp
                    ),
                    modifier = Modifier.weight(1f)
                )

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = formatCurrency(item.amount, currency),
                        style = AppTextStyles.drawerItemLabel.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp
                        ),
                        color = AppColors.contentPrimary
                    )
                    Text(
                        text = "(%.2f%%)".format(item.percentage * 100),
                        style = AppTextStyles.caption.copy(
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Normal
                        ),
                        color = AppColors.contentSecondary
                    )
                }
            }
        }

        if (!showFullBreakdown && data.size > 5) {
            Text(
                text = "+${data.size - 5} more categories",
                style = AppTextStyles.transactionSwipeAction.copy(
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Medium
                ),
                color = AppColors.primary,
                modifier = Modifier
                    .padding(start = 22.dp)
                    .clickable { onToggleBreakdown(true) }
            )
        } else if (showFullBreakdown && data.size > 5) {
            Text(
                text = "Show less",
                style = AppTextStyles.transactionSwipeAction.copy(
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Medium
                ),
                color = AppColors.primary,
                modifier = Modifier
                    .padding(start = 22.dp)
                    .clickable { onToggleBreakdown(false) }
            )
        }
    }
}

/**
 * Maps [CategoryPieChartCardConfig] + [CardRenderData.CategoryPieChart] to [CardWithCallbacks.CategoryPieChart].
 */
fun mapCategoryPieChart(
    config: CategoryPieChartCardConfig,
    renderData: CardRenderData.CategoryPieChart,
    input: CardInput,
    updateHandler: CardUpdateHandler,
    onDelete: ((String) -> Unit)?,
    onRecordsClick: (TransactionFilter) -> Unit,
    callbacks: AeonInteractionCallbacks,
    filterSheetEnabled: Boolean
) = CardWithCallbacks.CategoryPieChart(
    renderData = renderData,
    callbacks = CardCallbacks.Analytics(
        onUpdate = { newConfig ->
            if (newConfig is CategoryPieChartCardConfig) {
                updateHandler.updateCardCategoryPieChartConfig(config.cardId, newConfig)
            } else {
                if (com.osscabinet.ossWallet.BuildConfig.DEBUG)
                    android.util.Log.w(
                        "CardBridge",
                        "CategoryPieChart.onUpdate: unexpected ${newConfig::class.simpleName}"
                    )
            }
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
fun CategoryPieChartCardPreview() {
    val sampleData = listOf(
        CategoryChartData(
            category = "Housing",
            categoryId = null,
            amount = BigDecimal("1200.00"),
            colorHex = 0xFF2196F3.toInt(),
            percentage = 0.6f
        ),
        CategoryChartData(
            category = "Food",
            categoryId = null,
            amount = BigDecimal("450.00"),
            colorHex = 0xFFFF5722.toInt(),
            percentage = 0.2f
        ),
        CategoryChartData(
            category = "Transport",
            categoryId = null,
            amount = BigDecimal("150.00"),
            colorHex = 0xFF4CAF50.toInt(),
            percentage = 0.1f
        ),
        CategoryChartData(
            category = "Entertainment",
            categoryId = null,
            amount = BigDecimal("200.00"),
            colorHex = 0xFF9C27B0.toInt(),
            percentage = 0.1f
        )
    )

    OssWalletTheme {
        Box(
            modifier = Modifier
                .background(AppColors.background)
                .padding(16.dp)
        ) {
            CategoryPieChartCard(
                config = CategoryPieChartCardConfig(
                    cardId = "preview",
                    title = "Category Breakdown",
                    showLabels = true,
                    showLegend = true,
                    showComparison = true,
                    filter = TransactionFilter(timelineRange = TimelineRange.ThisPeriod(com.osscabinet.ossWallet.domain.model.timeline.ThisPeriodType.MONTH))
                ),
                data = sampleData.map { it.copy(previousAmount = it.amount.multiply(BigDecimal("0.9"))) },
                currency = "USD"
            )
        }
    }
}
