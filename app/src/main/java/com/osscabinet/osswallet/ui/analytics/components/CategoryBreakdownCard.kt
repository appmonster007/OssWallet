package com.osscabinet.ossWallet.ui.analytics.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.rounded.PieChart
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.osscabinet.ossWallet.domain.model.Category
import com.osscabinet.ossWallet.domain.model.Filter
import com.osscabinet.ossWallet.domain.model.Account
import com.osscabinet.ossWallet.domain.model.TransactionFilter
import com.osscabinet.ossWallet.domain.model.settings.CategoryBreakdownCardConfig
import kotlinx.serialization.Contextual
import kotlinx.serialization.Serializable
import com.osscabinet.ossWallet.ui.components.AeonFlowSectionHeader
import com.osscabinet.ossWallet.ui.components.AeonSettingsToggle
import com.osscabinet.ossWallet.ui.components.cards.AeonCardSettingsSheet
import com.osscabinet.ossWallet.ui.components.visualization.charts.AeonPieChart
import com.osscabinet.ossWallet.ui.components.visualization.charts.AeonPieChartSlice
import com.osscabinet.ossWallet.ui.theme.OssWalletTheme
import java.math.BigDecimal
import androidx.compose.foundation.horizontalScroll
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Home
import com.osscabinet.ossWallet.ui.components.cards.AeonExpressiveCard
import com.osscabinet.ossWallet.ui.util.formatAmount
import java.math.RoundingMode

@Composable
fun CategoryBreakdownCard(
    config: CategoryBreakdownCardConfig,
    data: List<CategoryBreakdownData>,
    modifier: Modifier = Modifier,
    @Suppress("UNUSED_PARAMETER") headerModifier: Modifier = Modifier,
    onUpdate: (CategoryBreakdownCardConfig) -> Unit = {},
    onDelete: () -> Unit = {},
    onRecordsClick: (com.osscabinet.ossWallet.ui.navigation.RecordsFilterContext) -> Unit = {},
    accounts: List<Account> = emptyList(),
    allCategories: List<Category> = emptyList(),
    savedFilters: List<Filter> = emptyList(),
    baseFilter: TransactionFilter = TransactionFilter(),
    precision: Int = 2
) {
    var showDisplaySettings by remember { mutableStateOf(false) }
    var showFilterSettings by remember { mutableStateOf(false) }
    
    // Drill-down state
    var navigationStack by remember { mutableStateOf(listOf<List<CategoryBreakdownData>>(data)) }
    val currentData = navigationStack.last()
    
    // Sync with external data changes
    LaunchedEffect(data) {
        navigationStack = listOf(data)
    }

    AeonExpressiveCard(
        title = config.title,
        icon = Icons.Rounded.PieChart,
        modifier = modifier,
        filter = config.filter,
        headerTrailing = {
            IconButton(onClick = { showFilterSettings = true }, modifier = Modifier.size(32.dp)) {
                Icon(Icons.Default.FilterList, contentDescription = "Filter", modifier = Modifier.size(20.dp), tint = MaterialTheme.colorScheme.primary)
            }
            IconButton(onClick = { showDisplaySettings = true }, modifier = Modifier.size(32.dp)) {
                Icon(Icons.Default.Tune, contentDescription = "Display Settings", modifier = Modifier.size(20.dp), tint = MaterialTheme.colorScheme.primary)
            }
        }
    ) {
        if (navigationStack.size > 1) {
            Row(
                modifier = Modifier
                    .padding(bottom = 12.dp)
                    .horizontalScroll(rememberScrollState()),
                verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = { navigationStack = navigationStack.take(1) },
                    modifier = Modifier.size(24.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Home,
                        contentDescription = "Home",
                        modifier = Modifier.size(16.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
                
                navigationStack.drop(1).forEachIndexed { index, stack ->
                    Icon(
                        imageVector = Icons.Default.ChevronRight,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                        tint = MaterialTheme.colorScheme.outline
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

        val totalGlobal = remember(data) { data.sumOf { it.netBalance }.abs() }

        val groupedData = remember(currentData) {
            val sorted = currentData.sortedByDescending { it.netBalance.abs() }
            // Group everything after top 10 into "Other"
            if (sorted.size > 12) {
                val top = sorted.take(10)
                val others = sorted.drop(10)
                val otherSum = others.sumOf { it.netBalance }
                val otherPercentage = others.sumOf { it.percentage.toDouble() }.toFloat()
                
                top + CategoryBreakdownData(
                    category = "Other",
                    categoryId = -1L, // Special ID for "Other"
                    netBalance = otherSum,
                    income = others.sumOf { it.income },
                    expense = others.sumOf { it.expense },
                    colorHex = 0xFF9E9E9E.toInt(), // Grey
                    percentage = otherPercentage,
                    hasSubcategories = true,
                    subcategoryData = others
                )
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
                val slices = groupedData.map { item ->
                    AeonPieChartSlice(
                        value = item.netBalance.abs(),
                        label = item.category,
                        color = Color(item.colorHex),
                        hasSubcategories = item.hasSubcategories && !item.subcategoryData.isNullOrEmpty(),
                        subcategoryCount = item.subcategoryData?.size ?: 0,
                        percentageOfTotal = if (totalGlobal.signum() != 0) 
                            item.netBalance.abs().divide(totalGlobal, 4, RoundingMode.HALF_UP).toFloat() 
                            else 0f,
                        id = item.categoryId
                    )
                }
                
                AeonPieChart(
                    slices = slices,
                    modifier = Modifier.fillMaxSize(),
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
                        
                        onRecordsClick(com.osscabinet.ossWallet.ui.navigation.RecordsFilterContext.Custom(
                            categoryIds = categoryIds,
                            accountIds = config.filter.accountIds,
                            startDate = config.filter.startDate,
                            endDate = config.filter.endDate
                        ))
                    }
                )
            } else {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = androidx.compose.ui.Alignment.Center) {
                    Text("No category data", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
        
        // Optimized Legend
        if (config.showLegend && groupedData.isNotEmpty()) {
            Spacer(modifier = Modifier.height(16.dp))
            CategoryLegend(data = groupedData, precision = precision)
        }
    }

    if (showDisplaySettings) {
        CategoryBreakdownDisplaySettingsDialog(
            config = config,
            onUpdate = onUpdate,
            onDismiss = { showDisplaySettings = false },
            onDelete = onDelete
        )
    }
    
    if (showFilterSettings) {
        AeonCardSettingsSheet(
            title = "Breakdown Filter",
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
private fun CategoryBreakdownDisplaySettingsDialog(
    config: CategoryBreakdownCardConfig,
    onUpdate: (CategoryBreakdownCardConfig) -> Unit,
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
                    contentColor = MaterialTheme.colorScheme.error
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
                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
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
            }
        },
        shape = RoundedCornerShape(32.dp),
        containerColor = MaterialTheme.colorScheme.surfaceContainer
    )
}

@Composable
fun CategoryLegend(data: List<CategoryBreakdownData>, precision: Int = 2) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        data.take(5).forEach { item ->
            Row(
                verticalAlignment = androidx.compose.ui.Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Surface(
                    modifier = Modifier.size(10.dp),
                    shape = RoundedCornerShape(2.dp),
                    color = Color(item.colorHex),
                    border = BorderStroke(0.5.dp, Color.Black.copy(alpha = 0.05f))
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
                    text = formatCurrency(item.netBalance, precision),
                    style = MaterialTheme.typography.labelMedium.copy(
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp
                    ),
                    color = if (item.netBalance < BigDecimal.ZERO) Color(0xFFB3261E) else Color(0xFF004D40)
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
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                modifier = Modifier.padding(start = 22.dp)
            )
        }
    }
}

private fun formatCurrency(amount: BigDecimal, precision: Int = 2): String {
    val formatted = formatAmount(amount, precision)
    return if (amount >= BigDecimal.ZERO) {
        "+$formatted"
    } else {
        formatted
    }
}

@androidx.compose.ui.tooling.preview.Preview(showBackground = true)
@Composable
fun CategoryBreakdownCardPreview() {
    val subData = listOf(
        CategoryBreakdownData("Coffee", null, BigDecimal("50.00"), BigDecimal.ZERO, BigDecimal("50.00"), 0xFF795548.toInt(), 0.1f),
        CategoryBreakdownData("Dinner", null, BigDecimal("150.00"), BigDecimal.ZERO, BigDecimal("150.00"), 0xFFFF9800.toInt(), 0.3f)
    )
    
    val richData = listOf(
        CategoryBreakdownData("Housing", null, BigDecimal("1200.00"), BigDecimal.ZERO, BigDecimal("1200.00"), 0xFF2196F3.toInt(), 0.6f),
        CategoryBreakdownData(
            "Food & Dining", null, BigDecimal("450.00"), BigDecimal.ZERO, BigDecimal("450.00"), 0xFFFF5722.toInt(), 0.225f,
            hasSubcategories = true,
            subcategoryData = subData
        ),
        CategoryBreakdownData("Transport", null, BigDecimal("150.00"), BigDecimal.ZERO, BigDecimal("150.00"), 0xFF4CAF50.toInt(), 0.075f),
        CategoryBreakdownData("Entertainment", null, BigDecimal("200.00"), BigDecimal.ZERO, BigDecimal("200.00"), 0xFF9C27B0.toInt(), 0.1f),
        CategoryBreakdownData("Misc 1", null, BigDecimal("10.00"), BigDecimal.ZERO, BigDecimal("10.00"), 0xFF607D8B.toInt(), 0.01f),
        CategoryBreakdownData("Misc 2", null, BigDecimal("9.00"), BigDecimal.ZERO, BigDecimal("9.00"), 0xFF607D8B.toInt(), 0.01f),
        CategoryBreakdownData("Misc 3", null, BigDecimal("8.00"), BigDecimal.ZERO, BigDecimal("8.00"), 0xFF607D8B.toInt(), 0.01f),
        CategoryBreakdownData("Misc 4", null, BigDecimal("7.00"), BigDecimal.ZERO, BigDecimal("7.00"), 0xFF607D8B.toInt(), 0.01f),
        CategoryBreakdownData("Misc 5", null, BigDecimal("6.00"), BigDecimal.ZERO, BigDecimal("6.00"), 0xFF607D8B.toInt(), 0.01f),
        CategoryBreakdownData("Misc 6", null, BigDecimal("5.00"), BigDecimal.ZERO, BigDecimal("5.00"), 0xFF607D8B.toInt(), 0.01f),
        CategoryBreakdownData("Misc 7", null, BigDecimal("4.00"), BigDecimal.ZERO, BigDecimal("4.00"), 0xFF607D8B.toInt(), 0.01f),
        CategoryBreakdownData("Misc 8", null, BigDecimal("3.00"), BigDecimal.ZERO, BigDecimal("3.00"), 0xFF607D8B.toInt(), 0.01f),
        CategoryBreakdownData("Misc 9", null, BigDecimal("2.00"), BigDecimal.ZERO, BigDecimal("2.00"), 0xFF607D8B.toInt(), 0.01f),
        CategoryBreakdownData("Misc 10", null, BigDecimal("1.00"), BigDecimal.ZERO, BigDecimal("1.00"), 0xFF607D8B.toInt(), 0.01f)
    )

    OssWalletTheme {
        Column(
            modifier = Modifier.padding(16.dp).verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            CategoryBreakdownCard(
                config = CategoryBreakdownCardConfig(cardId = "rich", title = "Category Breakdown"),
                data = richData
            )
        }
    }
}

/**
 * Category breakdown data for pie chart card.
 * Represents the net balance (income - expense) for each category.
 */
@Serializable
data class CategoryBreakdownData(
    val category: String,
    val categoryId: Long?,
    @Contextual val netBalance: BigDecimal,
    @Contextual val income: BigDecimal,
    @Contextual val expense: BigDecimal,
    val colorHex: Int,
    val percentage: Float,
    val hasSubcategories: Boolean = false,
    val subcategoryData: List<CategoryBreakdownData>? = null
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
