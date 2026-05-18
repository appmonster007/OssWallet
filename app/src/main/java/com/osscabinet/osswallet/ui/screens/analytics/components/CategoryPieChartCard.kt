package com.osscabinet.ossWallet.ui.screens.analytics.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.rounded.PieChart
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.osscabinet.ossWallet.domain.model.TransactionFilter
import com.osscabinet.ossWallet.domain.model.timeline.TimelineRange
import com.osscabinet.ossWallet.domain.model.settings.CategoryPieChartCardConfig
import com.osscabinet.ossWallet.ui.components.cards.FilterSelectionContext
import kotlinx.serialization.Contextual
import kotlinx.serialization.Serializable
import com.osscabinet.ossWallet.ui.components.AeonSettingsToggle
import com.osscabinet.ossWallet.ui.components.visualization.charts.AeonPieChart
import com.osscabinet.ossWallet.ui.components.visualization.charts.AeonPieChartSlice
import com.osscabinet.ossWallet.ui.theme.OssWalletTheme
import com.osscabinet.ossWallet.ui.util.toChartColor
import com.osscabinet.ossWallet.ui.util.generateShade
import com.osscabinet.ossWallet.domain.service.currency.CurrencyFormatter.formatAmount
import com.osscabinet.ossWallet.ui.components.cards.AeonExpressiveCard
import com.osscabinet.ossWallet.ui.components.cards.CardCallbacks
import java.math.BigDecimal
import java.math.RoundingMode
import com.osscabinet.ossWallet.domain.service.timeline.TimelineService
import com.osscabinet.ossWallet.ui.theme.AppColors
import com.osscabinet.ossWallet.ui.theme.AppTextStyles

@Composable
fun CategoryPieChartCard(
    config: CategoryPieChartCardConfig,
    data: List<CategoryPieChartData>,
    modifier: Modifier = Modifier,
    headerModifier: Modifier = Modifier,
    callbacks: CardCallbacks.Analytics = CardCallbacks.Analytics(),
    filterContext: FilterSelectionContext = FilterSelectionContext(),
    baseFilter: TransactionFilter = TransactionFilter(),
    currentTheme: String = "System",
    previousTotal: BigDecimal? = null
) {
    val isDarkTheme = when (currentTheme) {
        "Light" -> false
        "Dark", "Amoled" -> true
        else -> isSystemInDarkTheme()
    }
    val isAmoled = currentTheme == "Amoled"
    
    // Drill-down state
    var navigationStack by remember { mutableStateOf(listOf<List<CategoryPieChartData>>(data)) }
    
    val filteredData = remember(data, config.showOnlyActiveCategories) {
        if (config.showOnlyActiveCategories) {
            data.filter { it.amount.signum() != 0 }
        } else {
            data
        }
    }
    
    val currentData = navigationStack.last()
    
    // Sync with external data changes
    LaunchedEffect(filteredData) {
        navigationStack = listOf(filteredData)
    }

    AeonExpressiveCard(
        title = config.title,
        icon = Icons.Rounded.PieChart,
        modifier = modifier,
        headerModifier = headerModifier,
        filter = config.filter,
        baseFilter = baseFilter,
        filterRules = config.filterRules,
        filterContext = filterContext,
        onFilterChange = { callbacks.onUpdate(config.copy(filter = it)) },
        onSaveFilterClick = callbacks.onSaveFilterClick,
        displaySettings = { onDismiss ->
            CategoryPieChartDisplaySettingsDialog(config = config, onUpdate = { callbacks.onUpdate(it) }, onDelete = callbacks.onDelete, onDismiss = onDismiss)
        }
    ) {
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
                    val name = prevStack.find { it.subcategoryData === stack }?.category ?: "Sub"
                    
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

        remember(data) { data.sumOf { it.amount.abs() } }

        val groupedData = remember(currentData) {
            val sorted = currentData.sortedByDescending { it.amount.abs() }
            // Group everything after top 10 into "Other"
            if (sorted.size > 12) {
                val top = sorted.take(10)
                val others = sorted.drop(10).filter { it.amount.abs() > BigDecimal.ZERO }
                if (others.isEmpty()) {
                    sorted
                } else {
                    val otherSum = others.sumOf { it.amount }
                    val otherPercentage = others.sumOf { it.percentage.toDouble() }.toFloat()
                    
                    top + CategoryPieChartData(
                        category = "Other",
                        categoryId = -1L, // Special ID for "Other"
                        amount = otherSum,
                        colorHex = 0xFF9E9E9E.toInt(), // Grey
                        percentage = otherPercentage,
                        hasSubcategories = true,
                        subcategoryCount = others.size,
                        subcategoryData = others
                    )
                }
            } else {
                sorted
            }
        }

        // Refined Visualization
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
                    val globalValue = data.sumOf { it.amount }

                    AeonPieChartSlice(
                        value = sliceValue,
                        label = item.category,
                        color = displayColor,
                        hasSubcategories = item.hasSubcategories,
                        subcategoryCount = item.subcategoryCount,
                        percentageOfTotal = if (globalValue.signum() != 0) 
                            sliceValue.divide(globalValue, 4, RoundingMode.HALF_UP).toFloat() 
                            else 0f,
                        id = item.categoryId
                    )
                }
                
                AeonPieChart(
                    slices = slices,
                    modifier = Modifier.fillMaxSize(),
                    rounded = config.rounded,
                    currency = baseFilter.currencies.firstOrNull() ?: "USD",
                    previousTotal = previousTotal,
                    showComparison = config.showComparison,
                    isAllTime = config.filter.timelineRange is TimelineRange.AllTime,
                    onSliceClick = { slice ->
                        // Interaction feedback
                    },
                    onDrillInClick = { slice ->
                        val clickedItem = groupedData.find { it.category == slice.label }
                        if (clickedItem?.hasSubcategories == true && !clickedItem.subcategoryData.isNullOrEmpty()) {
                            navigationStack = navigationStack + listOf(clickedItem.subcategoryData)
                        }
                    },
                    onNavigateToRecords = { slice ->
                        val clickedItem = groupedData.find { it.category == slice.label }
                        val categoryIds = clickedItem?.allCategoryIds() ?: listOfNotNull(slice.id?.toString())
                        
                        callbacks.onRecordsClick(
                            config.filter.copy(
                                categoryIds = categoryIds.map { it.toLong() }.toSet()
                            )
                        )
                    }
                )
            } else {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("No category data", style = AppTextStyles.caption, color = AppColors.contentSecondary)
                }
            }
        }
        
        // Optimized Legend
        if (config.showLegend && groupedData.isNotEmpty()) {
            Spacer(modifier = Modifier.height(16.dp))
            CategoryLegend(
                data = groupedData, 
                navigationStack = navigationStack,
                isDarkTheme = isDarkTheme, 
                isAmoled = isAmoled,
                showFullBreakdown = config.showFullBreakdown,
                onToggleBreakdown = { callbacks.onUpdate(config.copy(showFullBreakdown = it)) }
            )
        }
    }

}

@Composable
private fun CategoryPieChartDisplaySettingsDialog(
    config: CategoryPieChartCardConfig,
    onUpdate: (CategoryPieChartCardConfig) -> Unit,
    onDismiss: () -> Unit,
    onDelete: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        dismissButton = {
            TextButton(
                onClick = {
                    onDelete()
                    onDismiss()
                },
                colors = ButtonDefaults.textButtonColors(
                    contentColor = AppColors.error
                )
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
            TextButton(onClick = onDismiss) {
                Text("Done")
            }
        },
        title = {
            Text(
                "Breakdown Display",
                style = AppTextStyles.cardTitle
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                AeonSettingsToggle(
                    label = "SHOW LABELS",
                    checked = config.showLabels,
                    onCheckedChange = { onUpdate(config.copy(showLabels = it)) }
                )
                AeonSettingsToggle(
                    label = "SHOW LEGEND",
                    checked = config.showLegend,
                    onCheckedChange = { onUpdate(config.copy(showLegend = it)) }
                )
                AeonSettingsToggle(
                    label = "ROUNDED VISUALS",
                    description = "Apply spacing and modern visuals to segments",
                    checked = config.rounded,
                    onCheckedChange = { onUpdate(config.copy(rounded = it)) }
                )
                AeonSettingsToggle(
                    label = "FULL BREAKDOWN",
                    description = "Show all categories in the legend instead of top 5",
                    checked = config.showFullBreakdown,
                    onCheckedChange = { onUpdate(config.copy(showFullBreakdown = it)) }
                )
                AeonSettingsToggle(
                    label = "ACTIVE ONLY",
                    description = "Only show categories with non-zero values",
                    checked = config.showOnlyActiveCategories,
                    onCheckedChange = { onUpdate(config.copy(showOnlyActiveCategories = it)) }
                )
                AeonSettingsToggle(
                    label = "SHOW COMPARISON",
                    description = "Display comparison to previous period in chart center",
                    checked = config.showComparison,
                    onCheckedChange = { onUpdate(config.copy(showComparison = it)) }
                )
            }
        },
        shape = RoundedCornerShape(32.dp),
        containerColor = AppColors.sheetSurface
    )
}

@Composable
fun CategoryLegend(
    data: List<CategoryPieChartData>, 
    navigationStack: List<List<CategoryPieChartData>>,
    isDarkTheme: Boolean, 
    isAmoled: Boolean,
    showFullBreakdown: Boolean,
    onToggleBreakdown: (Boolean) -> Unit
) {
    val displayData = if (showFullBreakdown) data else data.take(5)

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        displayData.forEachIndexed { index, item ->
            val baseColor = if (navigationStack.size > 1) {
                val currentData = navigationStack.last()
                val prevStack = navigationStack[navigationStack.size - 2]
                val parentItem = prevStack.find { it.subcategoryData === currentData }
                parentItem?.colorHex?.let { Color(it) } ?: Color(item.colorHex)
            } else {
                Color(item.colorHex)
            }
            
            val displayColor = if (navigationStack.size > 1) {
                baseColor.generateShade(index, data.size, isDarkTheme, isAmoled)
            } else {
                baseColor.toChartColor(isDarkTheme, isAmoled)
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Surface(
                    modifier = Modifier.size(10.dp),
                    shape = RoundedCornerShape(2.dp),
                    color = displayColor,
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
                
                Text(
                    text = formatAmount(item.amount),
                    style = AppTextStyles.drawerItemLabel.copy(
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp
                    ),
                    color = AppColors.contentPrimary
                )
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

private fun formatCurrency(amount: BigDecimal): String {
    val formatted = formatAmount(amount)
    return if (amount >= BigDecimal.ZERO) {
        "+$formatted"
    } else {
        formatted
    }
}

@androidx.compose.ui.tooling.preview.Preview(showBackground = true)
@Composable
fun CategoryPieChartCardPreview() {
    val subData = listOf(
        CategoryPieChartData(category = "Coffee", categoryId = null, amount = BigDecimal("50.00"), colorHex = 0xFF795548.toInt(), percentage = 0.1f),
        CategoryPieChartData(category = "Dinner", categoryId = null, amount = BigDecimal("150.00"), colorHex = 0xFFFF9800.toInt(), percentage = 0.3f)
    )
    
    val richData = listOf(
        CategoryPieChartData(category = "Housing", categoryId = null, amount = BigDecimal("1200.00"), colorHex = 0xFF2196F3.toInt(), percentage = 0.6f),
        CategoryPieChartData(
            category = "Food & Dining", categoryId = null, amount = BigDecimal("450.00"), colorHex = 0xFFFF5722.toInt(), percentage = 0.225f,
            hasSubcategories = true,
            subcategoryData = subData
        ),
        CategoryPieChartData(category = "Transport", categoryId = null, amount = BigDecimal("150.00"), colorHex = 0xFF4CAF50.toInt(), percentage = 0.075f),
        CategoryPieChartData(category = "Entertainment", categoryId = null, amount = BigDecimal("200.00"), colorHex = 0xFF9C27B0.toInt(), percentage = 0.1f),
        CategoryPieChartData(category = "Misc 1", categoryId = null, amount = BigDecimal("10.00"), colorHex = 0xFF607D8B.toInt(), percentage = 0.01f),
        CategoryPieChartData(category = "Misc 2", categoryId = null, amount = BigDecimal("9.00"), colorHex = 0xFF607D8B.toInt(), percentage = 0.01f),
        CategoryPieChartData(category = "Misc 3", categoryId = null, amount = BigDecimal("8.00"), colorHex = 0xFF607D8B.toInt(), percentage = 0.01f),
        CategoryPieChartData(category = "Misc 4", categoryId = null, amount = BigDecimal("7.00"), colorHex = 0xFF607D8B.toInt(), percentage = 0.01f),
        CategoryPieChartData(category = "Misc 5", categoryId = null, amount = BigDecimal("6.00"), colorHex = 0xFF607D8B.toInt(), percentage = 0.01f),
        CategoryPieChartData(category = "Misc 6", categoryId = null, amount = BigDecimal("5.00"), colorHex = 0xFF607D8B.toInt(), percentage = 0.01f),
        CategoryPieChartData(category = "Misc 7", categoryId = null, amount = BigDecimal("4.00"), colorHex = 0xFF607D8B.toInt(), percentage = 0.01f),
        CategoryPieChartData(category = "Misc 8", categoryId = null, amount = BigDecimal("3.00"), colorHex = 0xFF607D8B.toInt(), percentage = 0.01f),
        CategoryPieChartData(category = "Misc 9", categoryId = null, amount = BigDecimal("2.00"), colorHex = 0xFF607D8B.toInt(), percentage = 0.01f),
        CategoryPieChartData(category = "Misc 10", categoryId = null, amount = BigDecimal("1.00"), colorHex = 0xFF607D8B.toInt(), percentage = 0.01f)
    )

    OssWalletTheme {
        Column(
            modifier = Modifier.padding(16.dp).verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            CategoryPieChartCard(
                config = CategoryPieChartCardConfig(cardId = "rich", title = "Category Pie Chart"),
                data = richData
            )
        }
    }
}

/**
 * Category data for pie chart card.
 * Represents expense or income distribution for each category.
 */
@Serializable
data class CategoryPieChartData(
    val category: String,
    val categoryId: Long?,
    @Contextual val amount: BigDecimal,
    val colorHex: Int,
    val percentage: Float,
    val hasSubcategories: Boolean = false,
    val subcategoryCount: Int = 0,
    val subcategoryData: List<CategoryPieChartData>? = null
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
