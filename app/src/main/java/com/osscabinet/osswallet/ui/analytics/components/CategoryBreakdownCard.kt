package com.osscabinet.ossWallet.ui.analytics.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
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
import com.osscabinet.ossWallet.domain.model.analytics.CategoryBreakdownData
import com.osscabinet.ossWallet.domain.model.settings.CategoryBreakdownCardConfig
import com.osscabinet.ossWallet.ui.components.AeonFlowSectionHeader
import com.osscabinet.ossWallet.ui.components.TimelineFilterBar
import com.osscabinet.ossWallet.ui.components.visualization.charts.CategoryBreakdownPieChart

/**
 * Category Breakdown Card with pie chart visualization.
 * Integrates TimelineFilterBar for timeline range selection.
 */
@Composable
fun CategoryBreakdownCard(
    config: CategoryBreakdownCardConfig,
    data: List<CategoryBreakdownData>,
    modifier: Modifier = Modifier,
    headerModifier: Modifier = Modifier,
    onUpdate: (CategoryBreakdownCardConfig) -> Unit = {},
    onDelete: () -> Unit = {}
) {
    var showSettings by remember { mutableStateOf(false) }
    
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        )
    ) {
        Column(
            modifier = Modifier.padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Header with settings button only
            AeonFlowSectionHeader(
                title = config.title,
                modifier = headerModifier.fillMaxWidth(),
                trailing = {
                    IconButton(onClick = { showSettings = true }) {
                        Icon(Icons.Default.Settings, contentDescription = "Settings")
                    }
                }
            )
            
            // Pie Chart
            CategoryBreakdownPieChart(
                data = data,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(300.dp),
                showLabels = config.showLabels
            )
            
            // Legend
            if (config.showLegend) {
                CategoryLegend(data = data)
            }
        }
    }
    
    if (showSettings) {
        CategoryBreakdownSettingsDialog(
            config = config,
            onUpdate = onUpdate,
            onDismiss = { showSettings = false },
            onDelete = onDelete
        )
    }
}

/**
 * Legend for category breakdown pie chart.
 * Shows total amount instead of percentage breakdown.
 */
@Composable
fun CategoryLegend(data: List<CategoryBreakdownData>) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        data.take(5).forEach { item ->
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Color indicator
                Box(
                    modifier = Modifier
                        .width(16.dp)
                        .height(16.dp)
                        .background(Color(item.colorHex))
                )
                
                // Category name and amount
                Text(
                    text = "${item.category}: ${formatCurrency(item.netBalance)}",
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.Medium
                )
                
                // Subcategory indicator
                if (item.hasSubcategories) {
                    Icon(
                        imageVector = Icons.Default.ArrowDropDown,
                        contentDescription = "Has subcategories",
                        modifier = Modifier.size(16.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
        
        if (data.size > 5) {
            Text(
                text = "+${data.size - 5} more categories",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

private fun formatCurrency(amount: java.math.BigDecimal): String {
    return if (amount >= java.math.BigDecimal.ZERO) {
        "+$amount"
    } else {
        "-$amount"
    }
}

@Composable
private fun CategoryBreakdownSettingsDialog(
    config: CategoryBreakdownCardConfig,
    onUpdate: (CategoryBreakdownCardConfig) -> Unit,
    onDismiss: () -> Unit,
    onDelete: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        dismissButton = {
            TextButton(
                onClick = onDelete,
                colors = androidx.compose.material3.ButtonDefaults.textButtonColors(
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
                config.title,
                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                // Timeline Range Selection
                Surface(
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                    shape = RoundedCornerShape(20.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text(
                            "TIMELINE",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 1.sp
                            ),
                            color = MaterialTheme.colorScheme.primary
                        )

                        TimelineFilterBar(
                            currentRange = config.timelineRange,
                            onRangeSelected = { range -> onUpdate(config.copy(timelineRange = range)) },
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }
        }
    )
}
