package com.osscabinet.ossWallet.ui.screens.analytics.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.isSystemInDarkTheme
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.rounded.Dashboard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.osscabinet.ossWallet.domain.card.CardFilterRules
import com.osscabinet.ossWallet.domain.model.TransactionFilter
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import com.osscabinet.ossWallet.ui.components.entry.EntrySwitchRow
import com.osscabinet.ossWallet.domain.model.settings.CategoryTreemapCardConfig
import com.osscabinet.ossWallet.domain.model.settings.ChartMetric
import com.osscabinet.ossWallet.domain.model.timeline.TimelineRange
import com.osscabinet.ossWallet.domain.service.currency.CurrencyFormatter
import com.osscabinet.ossWallet.ui.components.cards.AeonExpressiveCard
import com.osscabinet.ossWallet.ui.components.cards.CardCallbacks
import com.osscabinet.ossWallet.ui.components.cards.CardSettingsSection
import com.osscabinet.ossWallet.ui.components.cards.CardSettingsSheet
import com.osscabinet.ossWallet.ui.components.cards.FilterSelectionContext
import com.osscabinet.ossWallet.ui.components.visualization.charts.AeonTreemap
import com.osscabinet.ossWallet.ui.components.visualization.charts.AeonTreemapNode
import com.osscabinet.ossWallet.ui.theme.AppColors
import com.osscabinet.ossWallet.ui.theme.AppTextStyles
import com.osscabinet.ossWallet.ui.theme.LocalThemeMode
import com.osscabinet.ossWallet.ui.theme.OssWalletTheme
import com.osscabinet.ossWallet.ui.theme.PositiveAmountColor
import com.osscabinet.ossWallet.ui.util.generateShade
import com.osscabinet.ossWallet.ui.util.toChartColor
import kotlinx.serialization.Contextual
import kotlinx.serialization.Serializable
import java.math.BigDecimal
import java.math.RoundingMode

@Composable
fun CategoryTreemapCard(
    config: CategoryTreemapCardConfig,
    data: List<CategoryTreemapData>,
    modifier: Modifier = Modifier,
    headerModifier: Modifier = Modifier,
    callbacks: CardCallbacks.Analytics = CardCallbacks.Analytics(),
    filterContext: FilterSelectionContext = FilterSelectionContext(),
    baseFilter: TransactionFilter = TransactionFilter(),
    inheritedFilter: TransactionFilter = TransactionFilter(),
    filterRules: CardFilterRules? = null,
    screenOwnsTimeline: Boolean = false,
    showFilterButton: Boolean = true
) {
    val displayFilter =
        if (screenOwnsTimeline || inheritedFilter.timelineRange !is TimelineRange.AllTime)
            config.filter.copy(timelineRange = inheritedFilter.timelineRange)
        else config.filter

    val mode = LocalThemeMode.current
    val isDarkTheme = mode.isDarkTheme
    val isAmoled = mode.isAmoled

    // Drill-down state
    var navigationStack by remember { mutableStateOf(listOf<List<CategoryTreemapData>>(data)) }
    val currentData = navigationStack.last()

    // Grouping logic for "More Categories" grouping based on 2% threshold
    val groupedData = remember(currentData, config.showOnlyActiveCategories, navigationStack.size) {
        val filtered = if (config.showOnlyActiveCategories) {
            currentData.filter { it.amount.signum() != 0 }
        } else {
            currentData
        }
        val sorted = filtered.sortedByDescending { it.amount.abs() }

        // Detect if we are in a synthesized "More Categories" view
        val isMoreCategoriesDrillDown = if (navigationStack.size > 1) {
            val prevLevelRawData = navigationStack[navigationStack.size - 2]
            // If the current list isn't a subcategory of anything in the previous level, 
            // it's our synthesized "More Categories" list.
            prevLevelRawData.none { it.subcategoryData === currentData }
        } else false

        if (!isMoreCategoriesDrillDown) {
            val mainItems = sorted.filter { it.percentage >= 0.02f }
            val others = sorted.filter { it.percentage < 0.02f && it.amount.abs() > BigDecimal.ZERO }

            if (others.size > 1) {
                val otherSum = others.sumOf { it.amount }
                val otherPercentage = others.sumOf { it.percentage.toDouble() }.toFloat()
                val otherPrevSum = if (others.any { it.previousAmount != null }) {
                    others.sumOf { it.previousAmount ?: BigDecimal.ZERO }
                } else null

                mainItems + CategoryTreemapData(
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

    // Sync with external data changes
    LaunchedEffect(data) {
        navigationStack = listOf(data)
    }

    AeonExpressiveCard(
        title = config.title,
        icon = Icons.Rounded.Dashboard,
        modifier = modifier,
        headerModifier = headerModifier,
        filterEnabled = showFilterButton,
        filter = displayFilter,
        baseFilter = baseFilter,
        filterRules = filterRules ?: config.filterRules,
        filterContext = filterContext,
        onFilterChange = { callbacks.onUpdate(config.copy(filter = it)) },
        onSaveFilterClick = callbacks.onSaveFilterClick,
        displaySettings = { onDismiss ->
            CategoryTreemapSettingsSheet(
                config = config,
                onUpdate = { callbacks.onUpdate(it) },
                onDelete = callbacks.onDelete,
                onDismiss = onDismiss
            )
        }
    ) {
        MetricToggleRow(
            selectedMetric = config.selectedMetric,
            onMetricChange = { callbacks.onUpdate(config.copy(selectedMetric = it)) }
        )
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

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(280.dp)
        ) {
            if (groupedData.isNotEmpty()) {
                val rootNodes = groupedData.mapIndexed { index, item ->
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

                    AeonTreemapNode(
                        name = item.category,
                        value = item.amount,
                        color = displayColor,
                        percentageOfTotal = item.percentage,
                        id = item.categoryId,
                        hasSubcategories = item.hasSubcategories,
                        subcategoryCount = item.subcategoryCount
                    )
                }

                AeonTreemap(
                    nodes = rootNodes,
                    modifier = Modifier.fillMaxSize(),
                    showLabels = config.showLabels,
                    rounded = config.rounded,
                    onNodeClick = { _ ->
                        // Interaction feedback
                    },
                    onDrillInClick = { node ->
                        val clickedItem = groupedData.find { it.category == node.name }
                        if (clickedItem?.hasSubcategories == true && !clickedItem.subcategoryData.isNullOrEmpty()) {
                            navigationStack = navigationStack + listOf(clickedItem.subcategoryData)
                        }
                    },
                    onNavigateToRecords = { node ->
                        val clickedItem = groupedData.find { it.category == node.name }
                        val categoryIds =
                            clickedItem?.allCategoryIds() ?: listOfNotNull(node.id?.toString())

                        callbacks.onRecordsClick(
                            config.filter.copy(
                                categoryIds = categoryIds.map { it.toLong() }.toSet()
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

        // Consistent Legend for Treemap
        if (groupedData.isNotEmpty()) {
            Spacer(modifier = Modifier.height(16.dp))

            // Summary Section with Total and Comparison
            val total = remember(currentData) { currentData.sumOf { it.amount } }
            val previousTotalValue = remember(currentData) {
                if (currentData.any { it.previousAmount != null }) {
                    currentData.sumOf { it.previousAmount ?: BigDecimal.ZERO }
                } else null
            }
            val currency = baseFilter.currencies.firstOrNull() ?: "USD"

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Bottom
            ) {
                Column {
                    Text(
                        text = "TOTAL",
                        style = AppTextStyles.overlineLabel.copy(
                            letterSpacing = 1.25.sp
                        ),
                        color = AppColors.contentSecondary.copy(alpha = 0.6f)
                    )
                    
                    val amountText = CurrencyFormatter.formatAuto(total)
                    val symbolText = CurrencyFormatter.getCurrencySymbol(currency)
                    val isSymbolLeft = CurrencyFormatter.activeSystem.symbolPosition == CurrencyFormatter.SymbolPosition.LEFT
                    val contentColor = AppColors.contentPrimary
                    
                    Text(
                        text = buildAnnotatedString {
                            if (isSymbolLeft) {
                                withStyle(SpanStyle(fontWeight = FontWeight.Light, color = contentColor.copy(alpha = 0.6f), fontSize = 16.sp)) {
                                    append(symbolText)
                                }
                                append(" ")
                                append(amountText)
                            } else {
                                append(amountText)
                                append(" ")
                                withStyle(SpanStyle(fontWeight = FontWeight.Light, color = contentColor.copy(alpha = 0.6f), fontSize = 16.sp)) {
                                    append(symbolText)
                                }
                            }
                        },
                        style = AppTextStyles.heroAmount.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 24.sp,
                            letterSpacing = (-0.5).sp
                        ),
                        color = contentColor
                    )
                }

                if (config.showComparison && config.filter.timelineRange !is TimelineRange.AllTime && previousTotalValue != null && previousTotalValue.signum() != 0) {
                    val delta = total.abs().subtract(previousTotalValue.abs())
                        .divide(previousTotalValue.abs(), 8, RoundingMode.HALF_UP)
                        .multiply(BigDecimal("100"))

                    val isIncrease = delta.signum() > 0
                    val isDecrease = delta.signum() < 0

                    val positiveDeltaIsGood = config.selectedMetric == ChartMetric.INCOME || config.selectedMetric == ChartMetric.NET

                    val color = when {
                        isIncrease -> if (positiveDeltaIsGood) AppColors.positive else AppColors.negative
                        isDecrease -> if (positiveDeltaIsGood) AppColors.negative else AppColors.positive
                        else -> AppColors.contentSecondary
                    }

                    val icon = when {
                        isIncrease -> Icons.Default.ArrowUpward
                        isDecrease -> Icons.Default.ArrowDownward
                        else -> null
                    }

                    Surface(
                        color = color.copy(alpha = 0.12f),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.padding(bottom = 2.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            if (icon != null) {
                                Icon(
                                    imageVector = icon,
                                    contentDescription = null,
                                    modifier = Modifier.size(12.dp),
                                    tint = color
                                )
                            }
                            Text(
                                text = "${delta.abs().setScale(1, RoundingMode.HALF_UP)}%",
                                style = AppTextStyles.transactionBadge.copy(
                                    fontSize = 11.sp
                                ),
                                color = color
                            )
                        }
                    }
                }
            }

            if (config.showLegend) {
                Spacer(modifier = Modifier.height(16.dp))
                TreemapLegend(
                    data = groupedData,
                    isDarkTheme = isDarkTheme,
                    isAmoled = isAmoled,
                    showFullBreakdown = config.showFullBreakdown,
                    onToggleBreakdown = { callbacks.onUpdate(config.copy(showFullBreakdown = it)) },
                    onItemClick = { item ->
                        if (item.hasSubcategories && !item.subcategoryData.isNullOrEmpty()) {
                            navigationStack = navigationStack + listOf(item.subcategoryData)
                        } else {
                            val categoryIds = item.allCategoryIds()
                            callbacks.onRecordsClick(
                                config.filter.copy(
                                    categoryIds = categoryIds.map { it.toLong() }.toSet()
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CategoryTreemapSettingsSheet(
    config: CategoryTreemapCardConfig,
    onUpdate: (CategoryTreemapCardConfig) -> Unit,
    onDelete: () -> Unit,
    onDismiss: () -> Unit
) {
    CardSettingsSheet(title = "Treemap", onDismiss = onDismiss, onDelete = onDelete) {
        CardSettingsSection("Visuals") {
            EntrySwitchRow(
                "Show Labels",
                config.showLabels,
                subtitle = "Display category names on segments",
                onCheckedChange = { onUpdate(config.copy(showLabels = it)) }
            )
            EntrySwitchRow(
                "Show Legend",
                config.showLegend,
                subtitle = "Display list of categories beneath the map",
                onCheckedChange = { onUpdate(config.copy(showLegend = it)) }
            )
            EntrySwitchRow(
                "Rounded Visuals",
                config.rounded,
                subtitle = "Apply rounded corners and spacing to segments",
                onCheckedChange = { onUpdate(config.copy(rounded = it)) }
            )
        }
        CardSettingsSection("Data") {
            EntrySwitchRow(
                "Full Breakdown",
                config.showFullBreakdown,
                subtitle = "Show all categories instead of top 5",
                onCheckedChange = { onUpdate(config.copy(showFullBreakdown = it)) }
            )
            EntrySwitchRow(
                "Active Only",
                config.showOnlyActiveCategories,
                subtitle = "Only show categories with non-zero values",
                onCheckedChange = { onUpdate(config.copy(showOnlyActiveCategories = it)) }
            )
        }
    }
}


@Composable
fun TreemapLegend(
    data: List<CategoryTreemapData>,
    isDarkTheme: Boolean,
    isAmoled: Boolean,
    showFullBreakdown: Boolean,
    onToggleBreakdown: (Boolean) -> Unit,
    onItemClick: (CategoryTreemapData) -> Unit
) {
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
                        text = CurrencyFormatter.formatAuto(item.amount),
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

@androidx.compose.ui.tooling.preview.Preview(showBackground = true)
@Composable
fun CategoryTreemapCardPreview() {
    val sampleData = listOf(
        CategoryTreemapData(
            category = "Housing",
            categoryId = null,
            amount = BigDecimal("1200.00"),
            colorHex = 0xFF2196F3.toInt(),
            percentage = 0.6f
        ),
        CategoryTreemapData(
            category = "Food",
            categoryId = null,
            amount = BigDecimal("450.00"),
            colorHex = 0xFFFF5722.toInt(),
            percentage = 0.2f
        ),
        CategoryTreemapData(
            category = "Transport",
            categoryId = null,
            amount = BigDecimal("150.00"),
            colorHex = 0xFF4CAF50.toInt(),
            percentage = 0.1f
        ),
        CategoryTreemapData(
            category = "Entertainment",
            categoryId = null,
            amount = BigDecimal("200.00"),
            colorHex = 0xFF9C27B0.toInt(),
            percentage = 0.1f
        ),
        CategoryTreemapData(
            category = "Misc 1",
            categoryId = null,
            amount = BigDecimal("10.00"),
            colorHex = 0xFF607D8B.toInt(),
            percentage = 0.01f
        ),
        CategoryTreemapData(
            category = "Misc 2",
            categoryId = null,
            amount = BigDecimal("9.00"),
            colorHex = 0xFF607D8B.toInt(),
            percentage = 0.01f
        ),
        CategoryTreemapData(
            category = "Misc 3",
            categoryId = null,
            amount = BigDecimal("8.00"),
            colorHex = 0xFF607D8B.toInt(),
            percentage = 0.01f
        ),
        CategoryTreemapData(
            category = "Misc 4",
            categoryId = null,
            amount = BigDecimal("7.00"),
            colorHex = 0xFF607D8B.toInt(),
            percentage = 0.01f
        ),
        CategoryTreemapData(
            category = "Misc 5",
            categoryId = null,
            amount = BigDecimal("6.00"),
            colorHex = 0xFF607D8B.toInt(),
            percentage = 0.01f
        ),
        CategoryTreemapData(
            category = "Misc 6",
            categoryId = null,
            amount = BigDecimal("5.00"),
            colorHex = 0xFF607D8B.toInt(),
            percentage = 0.01f
        ),
        CategoryTreemapData(
            category = "Misc 7",
            categoryId = null,
            amount = BigDecimal("4.00"),
            colorHex = 0xFF607D8B.toInt(),
            percentage = 0.01f
        ),
        CategoryTreemapData(
            category = "Misc 8",
            categoryId = null,
            amount = BigDecimal("3.00"),
            colorHex = 0xFF607D8B.toInt(),
            percentage = 0.01f
        ),
        CategoryTreemapData(
            category = "Misc 9",
            categoryId = null,
            amount = BigDecimal("2.00"),
            colorHex = 0xFF607D8B.toInt(),
            percentage = 0.01f
        ),
        CategoryTreemapData(
            category = "Misc 10",
            categoryId = null,
            amount = BigDecimal("1.00"),
            colorHex = 0xFF607D8B.toInt(),
            percentage = 0.01f
        )
    )

        OssWalletTheme {
            CategoryTreemapCard(
                config = CategoryTreemapCardConfig(
                    cardId = "preview",
                    title = "Category Treemap",
                    showLabels = true,
                    showLegend = true,
                    showComparison = true,
                    filter = TransactionFilter(timelineRange = TimelineRange.ThisPeriod(com.osscabinet.ossWallet.domain.model.timeline.ThisPeriodType.MONTH))
                ),
                data = sampleData.map { it.copy(previousAmount = it.amount.multiply(BigDecimal("0.9"))) }
            )
        }
}

@Serializable
data class CategoryTreemapData(
    val category: String,
    val categoryId: Long?,
    @Contextual val amount: BigDecimal,
    val colorHex: Int,
    val percentage: Float,
    val hasSubcategories: Boolean = false,
    val subcategoryCount: Int = 0,
    val subcategoryData: List<CategoryTreemapData>? = null,
    @Contextual val previousAmount: BigDecimal? = null
) {
    fun allCategoryIds(): List<String> {
        val ids = mutableListOf<String>()
        categoryId?.let { ids.add(it.toString()) }
        subcategoryData?.forEach {
            ids.addAll(it.allCategoryIds())
        }
        return ids
    }
}

