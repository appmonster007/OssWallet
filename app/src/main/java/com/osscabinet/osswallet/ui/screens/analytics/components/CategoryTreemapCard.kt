package com.osscabinet.ossWallet.ui.screens.analytics.components

import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.rounded.Dashboard
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.osscabinet.ossWallet.domain.card.CardFilterRules
import com.osscabinet.ossWallet.domain.model.Account
import com.osscabinet.ossWallet.domain.model.Category
import com.osscabinet.ossWallet.domain.model.Filter
import com.osscabinet.ossWallet.domain.model.TransactionFilter
import com.osscabinet.ossWallet.domain.model.settings.CategoryTreemapCardConfig
import com.osscabinet.ossWallet.ui.components.AeonSettingsToggle
import com.osscabinet.ossWallet.ui.components.cards.AeonCardFilterButton
import com.osscabinet.ossWallet.ui.components.cards.AeonCardSettingsSheet
import com.osscabinet.ossWallet.ui.components.cards.AeonExpressiveCard
import com.osscabinet.ossWallet.ui.components.visualization.charts.AeonTreemap
import com.osscabinet.ossWallet.ui.components.visualization.charts.AeonTreemapNode
import com.osscabinet.ossWallet.ui.theme.OssWalletTheme
import com.osscabinet.ossWallet.domain.service.currency.CurrencyFormatter.formatAmount
import com.osscabinet.ossWallet.ui.util.generateShade
import com.osscabinet.ossWallet.ui.util.toChartColor
import kotlinx.serialization.Contextual
import kotlinx.serialization.Serializable
import java.math.BigDecimal
import com.osscabinet.ossWallet.domain.service.timeline.TimelineService
import com.osscabinet.ossWallet.ui.theme.AppColors

@Composable
fun CategoryTreemapCard(
    config: CategoryTreemapCardConfig,
    data: List<CategoryTreemapData>,
    modifier: Modifier = Modifier,
    headerModifier: Modifier = Modifier,
    onUpdate: (CategoryTreemapCardConfig) -> Unit = {},
    onDelete: () -> Unit = {},
    onRecordsClick: (com.osscabinet.ossWallet.domain.model.TransactionFilter) -> Unit = {},
    accounts: List<Account> = emptyList(),
    allCategories: List<Category> = emptyList(),
    savedFilters: List<Filter> = emptyList(),
    filterRules: CardFilterRules = CardFilterRules.INCOME_OR_EXPENSE_ONLY,
    baseFilter: TransactionFilter = TransactionFilter(),
    currentTheme: String = "System",
    precision: Int = 2
) {
    var showDisplaySettings by remember { mutableStateOf(false) }
    var showFilterSettings by remember { mutableStateOf(false) }
    
    val isDarkTheme = when (currentTheme) {
        "Light" -> false
        "Dark", "Amoled" -> true
        else -> isSystemInDarkTheme()
    }
    val isAmoled = currentTheme == "Amoled"

    // Drill-down state
    var navigationStack by remember { mutableStateOf(listOf<List<CategoryTreemapData>>(data)) }
    val currentData = navigationStack.last()
    
    // Grouping logic for "Other" category
    val groupedData = remember(currentData) {
        val sorted = currentData.sortedByDescending { it.amount.abs() }
        if (sorted.size > 12) {
            val top = sorted.take(10)
            val others = sorted.drop(10).filter { it.amount.abs() > BigDecimal.ZERO }
            if (others.isEmpty()) {
                sorted
            } else {
                val otherSum = others.sumOf { it.amount }
                val otherPercentage = others.sumOf { it.percentage.toDouble() }.toFloat()
                
                top + CategoryTreemapData(
                    category = "Other",
                    categoryId = -1L,
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
    
    // Sync with external data changes
    LaunchedEffect(data) {
        navigationStack = listOf(data)
    }

    AeonExpressiveCard(
        title = config.title,
        icon = Icons.Rounded.Dashboard,
        modifier = modifier,
        headerModifier = headerModifier,
        filter = config.filter,
        baseFilter = baseFilter,
        filterRules = filterRules,
        headerTrailing = { activeCount ->
            AeonCardFilterButton(
                onClick = { showFilterSettings = true },
                activeFilterCount = activeCount
            )
            IconButton(onClick = { showDisplaySettings = true }, modifier = Modifier.size(32.dp)) {
                Icon(Icons.Rounded.Tune, contentDescription = "Display Settings", modifier = Modifier.size(20.dp), tint = AppColors.primary)
            }
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
                            style = MaterialTheme.typography.labelSmall.copy(
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
                        val categoryIds = clickedItem?.allCategoryIds() ?: listOfNotNull(node.id?.toString())

                        onRecordsClick(
                            config.filter.copy(
                                categoryIds = categoryIds.map { it.toLong() }.toSet()
                            )
                        )
                    }
                )
            } else {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("No expense data", style = MaterialTheme.typography.bodySmall, color = AppColors.contentSecondary)
                }
            }
        }

        // Consistent Legend for Treemap
        if (config.showLegend && groupedData.isNotEmpty()) {
            Spacer(modifier = Modifier.height(16.dp))
            TreemapLegend(data = groupedData, isDarkTheme = isDarkTheme, isAmoled = isAmoled, precision = precision)
        }
    }

    if (showDisplaySettings) {
        CategoryTreemapDisplaySettingsDialog(
            config = config,
            onUpdate = onUpdate,
            onDismiss = { showDisplaySettings = false },
            onDelete = onDelete
        )
    }
    
    if (showFilterSettings) {
        AeonCardSettingsSheet(
            title = "Treemap Filter",
            filter = config.filter,
            onFilterChange = { newFilter -> onUpdate(config.copy(filter = newFilter)) },
            onDismiss = { showFilterSettings = false },
            onDelete = onDelete,
            accounts = accounts,
            allCategories = allCategories,
            savedFilters = savedFilters,
            filterRules = config.filterRules,
            baseFilter = baseFilter
        )
    }
}

@Composable
private fun CategoryTreemapDisplaySettingsDialog(
    config: CategoryTreemapCardConfig,
    onUpdate: (CategoryTreemapCardConfig) -> Unit,
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
                "Treemap Display",
                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                AeonSettingsToggle(
                    label = "SHOW LABELS",
                    description = "Display category names on treemap",
                    checked = config.showLabels,
                    onCheckedChange = { onUpdate(config.copy(showLabels = it)) }
                )
                AeonSettingsToggle(
                    label = "SHOW LEGEND",
                    description = "Display list of categories beneath the map",
                    checked = config.showLegend,
                    onCheckedChange = { onUpdate(config.copy(showLegend = it)) }
                )
                AeonSettingsToggle(
                    label = "ROUNDED VISUALS",
                    description = "Apply rounded corners and spacing to segments",
                    checked = config.rounded,
                    onCheckedChange = { onUpdate(config.copy(rounded = it)) }
                )
            }
        },
        shape = RoundedCornerShape(32.dp),
        containerColor = AppColors.sheetSurface
    )
}

@Composable
fun TreemapLegend(data: List<CategoryTreemapData>, isDarkTheme: Boolean, isAmoled: Boolean, precision: Int = 2) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        data.take(5).forEach { item ->
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Surface(
                    modifier = Modifier.size(10.dp),
                    shape = RoundedCornerShape(2.dp),
                    color = Color(item.colorHex).toChartColor(isDarkTheme, isAmoled),
                    border = BorderStroke(0.5.dp, AppColors.divider.copy(alpha = 0.3f))
                ) {}
                
                Text(
                    text = item.category,
                    style = MaterialTheme.typography.labelMedium.copy(
                        fontWeight = FontWeight.Medium,
                        fontSize = 12.sp
                    ),
                    modifier = Modifier.weight(1f)
                )
                
                Text(
                    text = formatAmount(item.amount, precision),
                    style = MaterialTheme.typography.labelMedium.copy(
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp
                    ),
                    color = AppColors.contentPrimary
                )
            }
        }
        
        if (data.size > 5) {
            Text(
                text = "+${data.size - 5} more categories",
                style = MaterialTheme.typography.labelSmall.copy(
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Medium
                ),
                color = AppColors.contentSecondary.copy(alpha = 0.5f),
                modifier = Modifier.padding(start = 22.dp)
            )
        }
    }
}

@androidx.compose.ui.tooling.preview.Preview(showBackground = true)
@Composable
fun CategoryTreemapCardPreview() {
    val sampleData = listOf(
        CategoryTreemapData(category = "Housing", categoryId = null, amount = BigDecimal("1200.00"), colorHex = 0xFF2196F3.toInt(), percentage = 0.6f),
        CategoryTreemapData(category = "Food", categoryId = null, amount = BigDecimal("450.00"), colorHex = 0xFFFF5722.toInt(), percentage = 0.2f),
        CategoryTreemapData(category = "Transport", categoryId = null, amount = BigDecimal("150.00"), colorHex = 0xFF4CAF50.toInt(), percentage = 0.1f),
        CategoryTreemapData(category = "Entertainment", categoryId = null, amount = BigDecimal("200.00"), colorHex = 0xFF9C27B0.toInt(), percentage = 0.1f),
        CategoryTreemapData(category = "Misc 1", categoryId = null, amount = BigDecimal("10.00"), colorHex = 0xFF607D8B.toInt(), percentage = 0.01f),
        CategoryTreemapData(category = "Misc 2", categoryId = null, amount = BigDecimal("9.00"), colorHex = 0xFF607D8B.toInt(), percentage = 0.01f),
        CategoryTreemapData(category = "Misc 3", categoryId = null, amount = BigDecimal("8.00"), colorHex = 0xFF607D8B.toInt(), percentage = 0.01f),
        CategoryTreemapData(category = "Misc 4", categoryId = null, amount = BigDecimal("7.00"), colorHex = 0xFF607D8B.toInt(), percentage = 0.01f),
        CategoryTreemapData(category = "Misc 5", categoryId = null, amount = BigDecimal("6.00"), colorHex = 0xFF607D8B.toInt(), percentage = 0.01f),
        CategoryTreemapData(category = "Misc 6", categoryId = null, amount = BigDecimal("5.00"), colorHex = 0xFF607D8B.toInt(), percentage = 0.01f),
        CategoryTreemapData(category = "Misc 7", categoryId = null, amount = BigDecimal("4.00"), colorHex = 0xFF607D8B.toInt(), percentage = 0.01f),
        CategoryTreemapData(category = "Misc 8", categoryId = null, amount = BigDecimal("3.00"), colorHex = 0xFF607D8B.toInt(), percentage = 0.01f),
        CategoryTreemapData(category = "Misc 9", categoryId = null, amount = BigDecimal("2.00"), colorHex = 0xFF607D8B.toInt(), percentage = 0.01f),
        CategoryTreemapData(category = "Misc 10", categoryId = null, amount = BigDecimal("1.00"), colorHex = 0xFF607D8B.toInt(), percentage = 0.01f)
    )

     OssWalletTheme {
        CategoryTreemapCard(
            config = CategoryTreemapCardConfig(
                cardId = "preview",
                title = "Category Treemap",
                showLabels = true,
                showLegend = true
            ),
            data = sampleData
        )
    }
}

/**
 * Category data for treemap card.
 * Represents expense or income distribution across categories for visualization.
 */
@Serializable
data class CategoryTreemapData(
    val category: String,
    val categoryId: Long?,
    @Contextual val amount: BigDecimal,
    val colorHex: Int,
    val percentage: Float,
    val hasSubcategories: Boolean = false,
    val subcategoryCount: Int = 0,
    val subcategoryData: List<CategoryTreemapData>? = null
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

