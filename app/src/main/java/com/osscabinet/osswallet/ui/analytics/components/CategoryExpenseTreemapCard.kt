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
import com.osscabinet.ossWallet.domain.model.analytics.CategoryExpenseTreemapData
import com.osscabinet.ossWallet.domain.model.settings.CategoryExpenseTreemapCardConfig
import com.osscabinet.ossWallet.ui.components.AeonFlowSectionHeader
import com.osscabinet.ossWallet.ui.components.TimelineFilterBar
import com.osscabinet.ossWallet.ui.components.visualization.charts.AeonTreemap
import com.osscabinet.ossWallet.ui.components.visualization.charts.AeonTreemapNode
import com.osscabinet.ossWallet.ui.theme.OssWalletTheme
import java.math.BigDecimal

@Composable
fun CategoryExpenseTreemapCard(
    config: CategoryExpenseTreemapCardConfig,
    data: List<CategoryExpenseTreemapData>,
    modifier: Modifier = Modifier,
    headerModifier: Modifier = Modifier,
    onUpdate: (CategoryExpenseTreemapCardConfig) -> Unit = {},
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
            
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(350.dp)
            ) {
                if (data.isNotEmpty()) {
                    val nodes = data.map { item ->
                        AeonTreemapNode(
                            name = item.category,
                            value = item.expense.abs(),
                            color = Color(item.colorHex)
                        )
                    }
                    
                    AeonTreemap(
                        nodes = nodes,
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = androidx.compose.ui.Alignment.Center) {
                        Text("No expense data", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        }
    }
    
    if (showSettings) {
        CategoryExpenseTreemapSettingsDialog(
            config = config,
            onUpdate = onUpdate,
            onDismiss = { showSettings = false },
            onDelete = onDelete
        )
    }
}

@Composable
private fun CategoryExpenseTreemapSettingsDialog(
    config: CategoryExpenseTreemapCardConfig,
    onUpdate: (CategoryExpenseTreemapCardConfig) -> Unit,
    onDismiss: () -> Unit,
    onDelete: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = { TextButton(onClick = onDismiss) { Text("Done") } },
        dismissButton = {
            TextButton(onClick = onDelete, colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)) {
                Icon(Icons.Default.Delete, null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("Delete")
            }
        },
        title = { Text(config.title, style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Surface(
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                    shape = RoundedCornerShape(20.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text("TIMELINE", style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, letterSpacing = 1.sp), color = MaterialTheme.colorScheme.primary)
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
fun CategoryExpenseTreemapCardPreview() {
    OssWalletTheme {
        CategoryExpenseTreemapCard(
            config = CategoryExpenseTreemapCardConfig(
                cardId = "preview",
                title = "CATEGORY EXPENSES"
            ),
            data = listOf(
                CategoryExpenseTreemapData("Rent", null, BigDecimal("1200.00"), 0xFF2196F3.toInt(), 0.6f),
                CategoryExpenseTreemapData("Food", null, BigDecimal("450.00"), 0xFFFF5722.toInt(), 0.225f),
                CategoryExpenseTreemapData("Transport", null, BigDecimal("150.00"), 0xFF4CAF50.toInt(), 0.075f),
                CategoryExpenseTreemapData("Entertainment", null, BigDecimal("200.00"), 0xFF9C27B0.toInt(), 0.1f)
            ),
            modifier = Modifier.padding(16.dp)
        )
    }
}
