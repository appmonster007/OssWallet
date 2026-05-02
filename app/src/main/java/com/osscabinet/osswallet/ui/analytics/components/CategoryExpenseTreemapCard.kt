package com.osscabinet.ossWallet.ui.analytics.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.rounded.Dashboard
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.osscabinet.ossWallet.domain.model.Category
import com.osscabinet.ossWallet.domain.model.Filter
import com.osscabinet.ossWallet.domain.model.Account
import com.osscabinet.ossWallet.domain.model.TransactionFilter
import com.osscabinet.ossWallet.domain.model.settings.CategoryExpenseTreemapCardConfig
import kotlinx.serialization.Contextual
import kotlinx.serialization.Serializable
import com.osscabinet.ossWallet.ui.components.AeonFlowSectionHeader
import com.osscabinet.ossWallet.ui.components.AeonSettingsToggle
import com.osscabinet.ossWallet.ui.components.cards.AeonCardSettingsSheet
import com.osscabinet.ossWallet.ui.components.visualization.charts.AeonTreemap
import com.osscabinet.ossWallet.ui.components.visualization.charts.AeonTreemapNode
import com.osscabinet.ossWallet.ui.theme.OssWalletTheme
import com.osscabinet.ossWallet.ui.util.generateShade
import java.math.BigDecimal
import com.osscabinet.ossWallet.ui.components.cards.AeonExpressiveCard
import androidx.compose.foundation.horizontalScroll
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Home
import androidx.compose.ui.unit.sp
import com.osscabinet.ossWallet.ui.util.formatAmount
import java.math.RoundingMode

@Composable
fun CategoryExpenseTreemapCard(
    config: CategoryExpenseTreemapCardConfig,
    data: List<CategoryExpenseTreemapData>,
    modifier: Modifier = Modifier,
    @Suppress("UNUSED_PARAMETER") headerModifier: Modifier = Modifier,
    onUpdate: (CategoryExpenseTreemapCardConfig) -> Unit = {},
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
    var navigationStack by remember { mutableStateOf(listOf<List<CategoryExpenseTreemapData>>(data)) }
    val currentData = navigationStack.last()
    
    // Grouping logic for "Other" category
    val groupedData = remember(currentData) {
        val sorted = currentData.sortedByDescending { it.expense.abs() }
        if (sorted.size > 12) {
            val top = sorted.take(10)
            val others = sorted.drop(10)
            val otherSum = others.sumOf { it.expense }
            val otherPercentage = others.sumOf { it.percentage.toDouble() }.toFloat()
            
            top + CategoryExpenseTreemapData(
                category = "Other",
                categoryId = -1L,
                expense = otherSum,
                colorHex = 0xFF9E9E9E.toInt(), // Grey
                percentage = otherPercentage,
                hasSubcategories = true,
                subcategoryData = others
            )
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
                        baseColor.generateShade(index, groupedData.size)
                    } else {
                        baseColor
                    }

                    AeonTreemapNode(
                        name = item.category,
                        value = item.expense,
                        color = displayColor,
                        percentageOfTotal = item.percentage,
                        id = item.categoryId,
                        hasSubcategories = item.hasSubcategories && !item.subcategoryData.isNullOrEmpty(),
                        subcategoryCount = item.subcategoryData?.size ?: 0
                    )
                }
                
                AeonTreemap(
                    nodes = rootNodes,
                    modifier = Modifier.fillMaxSize(),
                    showLabels = config.showLabels,
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

                        onRecordsClick(com.osscabinet.ossWallet.ui.navigation.RecordsFilterContext.Custom(
                            categoryIds = categoryIds,
                            accountIds = config.filter.accountIds,
                            startDate = config.filter.startDate,
                            endDate = config.filter.endDate,
                            isIncome = false // All expenses
                        ))
                    }
                )
            } else {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("No expense data", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }

        // Consistent Legend for Treemap
        if (config.showLegend && groupedData.isNotEmpty()) {
            Spacer(modifier = Modifier.height(16.dp))
            TreemapLegend(data = groupedData, precision = precision)
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
    config: CategoryExpenseTreemapCardConfig,
    onUpdate: (CategoryExpenseTreemapCardConfig) -> Unit,
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
            }
        },
        shape = RoundedCornerShape(32.dp),
        containerColor = MaterialTheme.colorScheme.surfaceContainer
    )
}

@Composable
fun TreemapLegend(data: List<CategoryExpenseTreemapData>, precision: Int = 2) {
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
                    text = formatAmount(item.expense, precision),
                    style = MaterialTheme.typography.labelMedium.copy(
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp
                    ),
                    color = MaterialTheme.colorScheme.onSurface
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

@androidx.compose.ui.tooling.preview.Preview(showBackground = true)
@Composable
fun CategoryExpenseTreemapCardPreview() {
    val sampleData = listOf(
        CategoryExpenseTreemapData("Housing", null, BigDecimal("1200.00"), 0xFF2196F3.toInt(), 0.6f),
        CategoryExpenseTreemapData("Food", null, BigDecimal("450.00"), 0xFFFF5722.toInt(), 0.2f),
        CategoryExpenseTreemapData("Transport", null, BigDecimal("150.00"), 0xFF4CAF50.toInt(), 0.1f),
        CategoryExpenseTreemapData("Entertainment", null, BigDecimal("200.00"), 0xFF9C27B0.toInt(), 0.1f),
        CategoryExpenseTreemapData("Misc 1", null, BigDecimal("10.00"), 0xFF607D8B.toInt(), 0.01f),
        CategoryExpenseTreemapData("Misc 2", null, BigDecimal("9.00"), 0xFF607D8B.toInt(), 0.01f),
        CategoryExpenseTreemapData("Misc 3", null, BigDecimal("8.00"), 0xFF607D8B.toInt(), 0.01f),
        CategoryExpenseTreemapData("Misc 4", null, BigDecimal("7.00"), 0xFF607D8B.toInt(), 0.01f),
        CategoryExpenseTreemapData("Misc 5", null, BigDecimal("6.00"), 0xFF607D8B.toInt(), 0.01f),
        CategoryExpenseTreemapData("Misc 6", null, BigDecimal("5.00"), 0xFF607D8B.toInt(), 0.01f),
        CategoryExpenseTreemapData("Misc 7", null, BigDecimal("4.00"), 0xFF607D8B.toInt(), 0.01f),
        CategoryExpenseTreemapData("Misc 8", null, BigDecimal("3.00"), 0xFF607D8B.toInt(), 0.01f),
        CategoryExpenseTreemapData("Misc 9", null, BigDecimal("2.00"), 0xFF607D8B.toInt(), 0.01f),
        CategoryExpenseTreemapData("Misc 10", null, BigDecimal("1.00"), 0xFF607D8B.toInt(), 0.01f)
    )

     OssWalletTheme {
        CategoryExpenseTreemapCard(
            config = CategoryExpenseTreemapCardConfig(
                cardId = "preview",
                title = "Category Expenses",
                showLabels = true,
                showLegend = true
            ),
            data = sampleData
        )
    }
}

/**
 * Category expense data for treemap card.
 * Represents expense distribution across categories for visualization.
 */
@Serializable
data class CategoryExpenseTreemapData(
    val category: String,
    val categoryId: Long?,
    @Contextual val expense: BigDecimal,
    val colorHex: Int,
    val percentage: Float,
    val hasSubcategories: Boolean = false,
    val subcategoryData: List<CategoryExpenseTreemapData>? = null
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
