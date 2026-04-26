package com.osscabinet.ossWallet.ui.analytics.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.osscabinet.ossWallet.data.local.entity.CategoryEntity
import com.osscabinet.ossWallet.data.local.entity.FilterEntity
import com.osscabinet.ossWallet.domain.model.Account
import com.osscabinet.ossWallet.domain.model.analytics.CategoryBreakdownData
import com.osscabinet.ossWallet.domain.model.settings.CategoryBreakdownCardConfig
import com.osscabinet.ossWallet.ui.components.AeonFlowSectionHeader
import com.osscabinet.ossWallet.ui.components.cards.AeonCardSettingsSheet
import com.osscabinet.ossWallet.ui.components.visualization.charts.AeonPieChart
import com.osscabinet.ossWallet.ui.components.visualization.charts.AeonPieChartSlice
import com.osscabinet.ossWallet.ui.theme.OssWalletTheme
import java.math.BigDecimal

@Composable
fun CategoryBreakdownCard(
    config: CategoryBreakdownCardConfig,
    data: List<CategoryBreakdownData>,
    modifier: Modifier = Modifier,
    headerModifier: Modifier = Modifier,
    onUpdate: (CategoryBreakdownCardConfig) -> Unit = {},
    onDelete: () -> Unit = {},
    onRecordsClick: (com.osscabinet.ossWallet.ui.navigation.RecordsFilterContext) -> Unit = {},
    accounts: List<Account> = emptyList(),
    allCategories: List<CategoryEntity> = emptyList(),
    savedFilters: List<FilterEntity> = emptyList()
) {
    var showSettings by remember { mutableStateOf(false) }
    
    // Drill-down state
    var navigationStack by remember { mutableStateOf(listOf<List<CategoryBreakdownData>>(data)) }
    val currentData = navigationStack.last()
    
    // Sync with external data changes
    LaunchedEffect(data) {
        navigationStack = listOf(data)
    }

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            AeonFlowSectionHeader(
                title = config.title,
                modifier = headerModifier.fillMaxWidth(),
                filter = config.filter,
                breadcrumbs = if (navigationStack.size > 1) {
                    navigationStack.drop(1).map { stack ->
                        // Find the parent's name by looking at where this stack came from
                        val prevIndex = navigationStack.indexOf(stack) - 1
                        val prevStack = navigationStack[prevIndex]
                        prevStack.find { it.subcategoryData === stack }?.category ?: "Sub"
                    }
                } else emptyList(),
                onBreadcrumbClick = { index ->
                    // index is -1 for Home, 0 for first sub, etc.
                    navigationStack = navigationStack.take(index + 2)
                },
                trailing = {
                    Row {
                        IconButton(onClick = { showSettings = true }) {
                            Icon(Icons.Default.Settings, contentDescription = "Settings", modifier = Modifier.size(20.dp))
                        }
                        IconButton(onClick = onDelete) {
                            Icon(Icons.Default.Delete, contentDescription = "Delete", modifier = Modifier.size(20.dp))
                        }
                    }
                }
            )
            
            val totalGlobal = remember(data) { data.sumOf { it.netBalance }.abs() }

            // Refined Visualization
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(300.dp)
            ) {
                if (currentData.isNotEmpty()) {
                    val slices = currentData.map { item ->
                        AeonPieChartSlice(
                            value = item.netBalance.abs(),
                            label = item.category,
                            color = Color(item.colorHex),
                            hasSubcategories = item.hasSubcategories && !item.subcategoryData.isNullOrEmpty(),
                            subcategoryCount = item.subcategoryData?.size ?: 0,
                            percentageOfTotal = if (totalGlobal.signum() != 0) 
                                item.netBalance.abs().divide(totalGlobal, 4, java.math.RoundingMode.HALF_UP).toFloat() 
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
                            val clickedItem = currentData.find { it.category == slice.label }
                            if (clickedItem?.hasSubcategories == true && !clickedItem.subcategoryData.isNullOrEmpty()) {
                                navigationStack = navigationStack + listOf(clickedItem.subcategoryData)
                            }
                        },
                        onNavigateToRecords = { slice ->
                            val clickedItem = currentData.find { it.categoryId == slice.id }
                            val categoryIds = clickedItem?.allCategoryIds() ?: listOfNotNull(slice.id?.toString())
                            
                            onRecordsClick(com.osscabinet.ossWallet.ui.navigation.RecordsFilterContext.Custom(
                                categoryIds = categoryIds,
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
            if (config.showLegend && currentData.isNotEmpty()) {
                CategoryLegend(data = currentData)
            }
        }
    }
    
    if (showSettings) {
        AeonCardSettingsSheet(
            title = config.title,
            filter = config.filter,
            onFilterChange = { newFilter -> onUpdate(config.copy(filter = newFilter)) },
            onDismiss = { showSettings = false },
            onDelete = onDelete,
            accounts = accounts,
            allCategories = allCategories,
            savedFilters = savedFilters
        )
    }
}

@Composable
fun CategoryLegend(data: List<CategoryBreakdownData>) {
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
                    modifier = Modifier.size(12.dp),
                    shape = RoundedCornerShape(2.dp),
                    color = Color(item.colorHex)
                ) {}
                
                Text(
                    text = item.category,
                    style = MaterialTheme.typography.labelMedium,
                    modifier = Modifier.weight(1f)
                )
                
                Text(
                    text = formatCurrency(item.netBalance),
                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                    color = if (item.netBalance < BigDecimal.ZERO) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
                )
            }
        }
        
        if (data.size > 5) {
            Text(
                text = "+${data.size - 5} more categories",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.outline,
                modifier = Modifier.padding(start = 24.dp)
            )
        }
    }
}

private fun formatCurrency(amount: BigDecimal): String {
    return if (amount >= BigDecimal.ZERO) {
        "+$amount"
    } else {
        amount.toString()
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
        CategoryBreakdownData("Entertainment", null, BigDecimal("200.00"), BigDecimal.ZERO, BigDecimal("200.00"), 0xFF9C27B0.toInt(), 0.1f)
    )

    OssWalletTheme {
        Column(
            modifier = Modifier.padding(16.dp).verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text("Rich Data (Drill-down enabled)", style = MaterialTheme.typography.titleSmall)
            CategoryBreakdownCard(
                config = CategoryBreakdownCardConfig(cardId = "rich", title = "CATEGORY BREAKDOWN"),
                data = richData
            )
            
            Text("Empty State", style = MaterialTheme.typography.titleSmall)
            CategoryBreakdownCard(
                config = CategoryBreakdownCardConfig(cardId = "empty", title = "CATEGORY BREAKDOWN"),
                data = emptyList()
            )
        }
    }
}
