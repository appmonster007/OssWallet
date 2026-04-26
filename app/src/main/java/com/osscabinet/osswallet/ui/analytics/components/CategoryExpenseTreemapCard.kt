package com.osscabinet.ossWallet.ui.analytics.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.osscabinet.ossWallet.data.local.entity.CategoryEntity
import com.osscabinet.ossWallet.data.local.entity.FilterEntity
import com.osscabinet.ossWallet.domain.model.Account
import com.osscabinet.ossWallet.domain.model.analytics.CategoryExpenseTreemapData
import com.osscabinet.ossWallet.domain.model.settings.CategoryExpenseTreemapCardConfig
import com.osscabinet.ossWallet.ui.components.AeonFlowSectionHeader
import com.osscabinet.ossWallet.ui.components.AeonSettingsToggle
import com.osscabinet.ossWallet.ui.components.cards.AeonCardSettingsSheet
import com.osscabinet.ossWallet.ui.components.visualization.charts.AeonTreemap
import com.osscabinet.ossWallet.ui.components.visualization.charts.AeonTreemapNode
import com.osscabinet.ossWallet.ui.theme.OssWalletTheme
import com.osscabinet.ossWallet.ui.util.generateShade
import java.math.BigDecimal

@Composable
fun CategoryExpenseTreemapCard(
    config: CategoryExpenseTreemapCardConfig,
    data: List<CategoryExpenseTreemapData>,
    modifier: Modifier = Modifier,
    headerModifier: Modifier = Modifier,
    onUpdate: (CategoryExpenseTreemapCardConfig) -> Unit = {},
    onDelete: () -> Unit = {},
    onRecordsClick: (com.osscabinet.ossWallet.ui.navigation.RecordsFilterContext) -> Unit = {},
    accounts: List<Account> = emptyList(),
    allCategories: List<CategoryEntity> = emptyList(),
    savedFilters: List<FilterEntity> = emptyList()
) {
    var showSettings by remember { mutableStateOf(false) }
    
    // Drill-down state
    var navigationStack by remember { mutableStateOf(listOf<List<CategoryExpenseTreemapData>>(data)) }
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
            
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(300.dp)
            ) {
                if (currentData.isNotEmpty()) {
                    val rootNodes = currentData.sortedByDescending { it.expense.abs() }.mapIndexed { index, item ->
                        val baseColor = if (navigationStack.size > 1) {
                            // Find the parent item in the previous level of navigation stack
                            val prevStack = navigationStack[navigationStack.size - 2]
                            val parentItem = prevStack.find { it.subcategoryData === currentData }
                            parentItem?.colorHex?.let { Color(it) } ?: Color(item.colorHex)
                        } else {
                            Color(item.colorHex)
                        }
                        
                        val displayColor = if (navigationStack.size > 1) {
                            baseColor.generateShade(index, currentData.size)
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
                            val clickedItem = currentData.find { it.category == node.name }
                            if (clickedItem?.hasSubcategories == true && !clickedItem.subcategoryData.isNullOrEmpty()) {
                                navigationStack = navigationStack + listOf(clickedItem.subcategoryData)
                            }
                        },
                        onNavigateToRecords = { node ->
                            val clickedItem = currentData.find { it.categoryId == node.id }
                            val categoryIds = clickedItem?.allCategoryIds() ?: listOfNotNull(node.id?.toString())

                            onRecordsClick(com.osscabinet.ossWallet.ui.navigation.RecordsFilterContext.Custom(
                                categoryIds = categoryIds,
                                startDate = config.filter.startDate,
                                endDate = config.filter.endDate
                            ))
                        }
                    )
                } else {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text("No expense data", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
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
        ) {
            AeonSettingsToggle(
                label = "SHOW LABELS",
                description = "Display category names on treemap",
                checked = config.showLabels,
                onCheckedChange = { onUpdate(config.copy(showLabels = it)) }
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
        CategoryExpenseTreemapData("Entertainment", null, BigDecimal("200.00"), 0xFF9C27B0.toInt(), 0.1f)
    )

     OssWalletTheme {
        CategoryExpenseTreemapCard(
            config = CategoryExpenseTreemapCardConfig(cardId = "preview", title = "CATEGORY EXPENSES"),
            data = sampleData
        )
    }
}
