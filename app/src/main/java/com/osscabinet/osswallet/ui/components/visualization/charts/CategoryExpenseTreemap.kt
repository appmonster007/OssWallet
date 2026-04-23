package com.osscabinet.ossWallet.ui.components.visualization.charts

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.osscabinet.ossWallet.domain.model.analytics.CategoryExpenseTreemapData
import java.math.BigDecimal

/**
 * Treemap visualization for category expense distribution.
 * Uses a simplified grid-based layout for the treemap effect.
 */
@Composable
fun CategoryExpenseTreemap(
    data: List<CategoryExpenseTreemapData>,
    modifier: Modifier = Modifier,
    onCategoryClick: (CategoryExpenseTreemapData) -> Unit = {},
    onCategoryDoubleClick: (CategoryExpenseTreemapData) -> Unit = {},
    showLabels: Boolean = true
) {
    var currentData by remember { mutableStateOf(data) }
    var showSubcategories by remember { mutableStateOf(false) }
    
    // When data changes, reset to top-level view
    if (data != currentData && !showSubcategories) {
        currentData = data
    }
    
    if (currentData.isEmpty()) {
        Box(
            modifier = modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Text("No expense data available")
        }
        return
    }
    
    // Calculate total for sizing
    val totalExpense = currentData.sumOf { it.expense }
    
    // Simplified treemap: split into two columns
    val leftColumnData = currentData.take((currentData.size + 1) / 2)
    val rightColumnData = currentData.drop((currentData.size + 1) / 2)
    
    Row(modifier = modifier.fillMaxWidth()) {
        val leftWeight = if (totalExpense > BigDecimal.ZERO) {
            (leftColumnData.sumOf { it.expense }.toFloat() / totalExpense.toFloat()).coerceAtLeast(0.01f)
        } else {
            0.5f
        }
        val rightWeight = if (totalExpense > BigDecimal.ZERO) {
            (rightColumnData.sumOf { it.expense }.toFloat() / totalExpense.toFloat()).coerceAtLeast(0.01f)
        } else {
            0.5f
        }
        
        TreemapColumn(
            data = leftColumnData,
            totalExpense = totalExpense,
            modifier = Modifier.weight(leftWeight),
            onCategoryClick = { item ->
                onCategoryClick(item)
                if (!item.hasSubcategories) {
                    showSubcategories = false
                    currentData = data
                }
            },
            onCategoryDoubleClick = { item ->
                if (item.hasSubcategories) {
                    onCategoryDoubleClick(item)
                    showSubcategories = true
                    currentData = item.subcategoryData ?: emptyList()
                }
            },
            showLabels = showLabels
        )
        
        TreemapColumn(
            data = rightColumnData,
            totalExpense = totalExpense,
            modifier = Modifier.weight(rightWeight),
            onCategoryClick = { item ->
                onCategoryClick(item)
                if (!item.hasSubcategories) {
                    showSubcategories = false
                    currentData = data
                }
            },
            onCategoryDoubleClick = { item ->
                if (item.hasSubcategories) {
                    onCategoryDoubleClick(item)
                    showSubcategories = true
                    currentData = item.subcategoryData ?: emptyList()
                }
            },
            showLabels = showLabels
        )
    }
}

@Composable
private fun TreemapColumn(
    data: List<CategoryExpenseTreemapData>,
    totalExpense: BigDecimal,
    modifier: Modifier = Modifier,
    onCategoryClick: (CategoryExpenseTreemapData) -> Unit,
    onCategoryDoubleClick: (CategoryExpenseTreemapData) -> Unit,
    showLabels: Boolean
) {
    var lastTapTime by remember { mutableStateOf(0L) }
    
    Column(modifier = modifier.fillMaxSize()) {
        data.forEach { item ->
            val weight = if (totalExpense > BigDecimal.ZERO) {
                item.expense.toFloat() / totalExpense.toFloat()
            } else {
                1f / data.size
            }
            
            Box(
                modifier = Modifier
                    .weight(weight)
                    .fillMaxWidth()
                    .background(Color(item.colorHex))
                    .clickable {
                        val currentTime = System.currentTimeMillis()
                        val isDoubleClick = currentTime - lastTapTime < 300L
                        lastTapTime = currentTime
                        
                        if (isDoubleClick) {
                            onCategoryDoubleClick(item)
                        } else {
                            onCategoryClick(item)
                        }
                    }
                    .padding(8.dp),
                contentAlignment = Alignment.Center
            ) {
                if (showLabels) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = item.category,
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                textAlign = TextAlign.Center,
                                fontSize = 12.sp
                            )
                            Text(
                                text = formatCurrency(item.expense),
                                color = Color.White,
                                textAlign = TextAlign.Center,
                                fontSize = 10.sp
                            )
                        }
                        if (item.hasSubcategories) {
                            Icon(
                                imageVector = Icons.Default.ArrowDropDown,
                                contentDescription = "Has subcategories",
                                modifier = Modifier.size(12.dp),
                                tint = Color.White
                            )
                        }
                    }
                }
            }
        }
    }
}

private fun formatCurrency(amount: BigDecimal): String {
    return amount.toString()
}
