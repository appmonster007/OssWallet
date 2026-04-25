package com.osscabinet.ossWallet.ui.analytics.components

import androidx.compose.foundation.layout.*
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
import com.osscabinet.ossWallet.domain.model.analytics.CategoryBreakdownData
import com.osscabinet.ossWallet.domain.model.settings.CategoryBreakdownCardConfig
import com.osscabinet.ossWallet.ui.components.AeonFlowSectionHeader
import com.osscabinet.ossWallet.ui.components.TimelineFilterBar
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
    onDelete: () -> Unit = {}
) {
    var showSettings by remember { mutableStateOf(false) }
    
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
            
            // Refined Visualization
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(300.dp)
            ) {
                if (data.isNotEmpty()) {
                    val slices = data.map { item ->
                        AeonPieChartSlice(
                            value = item.netBalance.abs(),
                            label = item.category,
                            color = Color(item.colorHex)
                        )
                    }
                    
                    AeonPieChart(
                        slices = slices,
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = androidx.compose.ui.Alignment.Center) {
                        Text("No category data", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
            
            // Optimized Legend
            if (config.showLegend && data.isNotEmpty()) {
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
                colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
            ) {
                Icon(Icons.Default.Delete, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("Delete")
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("Done") }
        },
        title = {
            Text(config.title, style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold))
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
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
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, letterSpacing = 1.sp),
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

@androidx.compose.ui.tooling.preview.Preview(showBackground = true)
@Composable
fun CategoryBreakdownCardPreview() {
    OssWalletTheme {
        CategoryBreakdownCard(
            config = CategoryBreakdownCardConfig(
                cardId = "preview",
                title = "CATEGORY BREAKDOWN"
            ),
            data = listOf(
                CategoryBreakdownData("Food", null, BigDecimal("450.00"), BigDecimal.ZERO, BigDecimal("450.00"), 0xFFFF5722.toInt(), 0.3f),
                CategoryBreakdownData("Rent", null, BigDecimal("1200.00"), BigDecimal.ZERO, BigDecimal("1200.00"), 0xFF2196F3.toInt(), 0.6f),
                CategoryBreakdownData("Transport", null, BigDecimal("150.00"), BigDecimal.ZERO, BigDecimal("150.00"), 0xFF4CAF50.toInt(), 0.1f)
            ),
            modifier = Modifier.padding(16.dp)
        )
    }
}
