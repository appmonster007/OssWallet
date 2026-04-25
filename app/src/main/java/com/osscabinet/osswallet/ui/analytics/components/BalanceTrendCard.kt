package com.osscabinet.ossWallet.ui.analytics.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.osscabinet.ossWallet.domain.model.settings.BalanceTrendCardConfig
import com.osscabinet.ossWallet.ui.components.AeonFlowSectionHeader
import com.osscabinet.ossWallet.ui.components.TimelineFilterBar
import com.osscabinet.ossWallet.ui.theme.AccentBlue
import com.osscabinet.ossWallet.ui.util.formatCurrencyRight
import java.math.BigDecimal

@Composable
fun BalanceTrendCard(
    config: BalanceTrendCardConfig,
    data: BalanceTrendData,
    modifier: Modifier = Modifier,
    onUpdate: (BalanceTrendCardConfig) -> Unit = {},
    onDelete: () -> Unit = {}
) {
    var showSettings by remember { mutableStateOf(false) }
    
    Card(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp)),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            AeonFlowSectionHeader(
                title = config.title,
                trailing = {
                    Row {
                        IconButton(onClick = { showSettings = true }) {
                            Icon(Icons.Default.Settings, contentDescription = "Settings")
                        }
                        IconButton(onClick = onDelete) {
                            Icon(Icons.Default.Delete, contentDescription = "Delete")
                        }
                    }
                }
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Balance trend visualization
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        "Current Balance",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        formatCurrencyRight(data.currentBalance, data.currency, data.precision),
                        style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
                
                Surface(
                    color = if (data.percentageChange.startsWith("-")) {
                        MaterialTheme.colorScheme.errorContainer
                    } else {
                        MaterialTheme.colorScheme.primaryContainer
                    },
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.padding(8.dp)
                ) {
                    Text(
                        data.percentageChange,
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Medium),
                        color = if (data.percentageChange.startsWith("-")) {
                            MaterialTheme.colorScheme.onErrorContainer
                        } else {
                            MaterialTheme.colorScheme.onPrimaryContainer
                        },
                        modifier = Modifier.padding(8.dp, 4.dp)
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Area chart with proper scales
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp)
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f), RoundedCornerShape(8.dp))
                    .clip(RoundedCornerShape(8.dp))
            ) {
                if (data.weeklyBalances.isNotEmpty()) {
                    Canvas(modifier = Modifier.fillMaxSize().padding(16.dp)) {
                        val width = size.width
                        val height = size.height
                        val padding = 40f
                        
                        // Calculate min and max for scaling
                        val maxBalance = data.weeklyBalances.maxOrNull() ?: 1.0
                        val minBalance = data.weeklyBalances.minOrNull() ?: 0.0
                        val valueRange = (maxBalance - minBalance).takeIf { it > 0 } ?: 1.0
                        
                        // Helper functions for mapping
                        fun valueToY(value: Double): Float {
                            val normalized = ((value - minBalance) / valueRange).toFloat()
                            return height - padding - (normalized * (height - 2 * padding))
                        }
                        
                        fun indexToX(index: Int): Float {
                            return padding + (index.toFloat() / (data.weeklyBalances.size - 1).coerceAtLeast(1)) * (width - 2 * padding)
                        }
                        
                        // Draw grid lines (horizontal)
                        for (i in 0..4) {
                            val y = height - padding - (i * (height - 2 * padding) / 4)
                            drawLine(
                                start = Offset(padding, y),
                                end = Offset(width - padding, y),
                                color = Color(android.graphics.Color.parseColor("#E0E0E0")),
                                strokeWidth = 1f
                            )
                        }
                        
                        // Draw axes
                        drawLine(
                            start = Offset(padding, padding),
                            end = Offset(padding, height - padding),
                            color = Color.Gray,
                            strokeWidth = 2f
                        )
                        
                        drawLine(
                            start = Offset(padding, height - padding),
                            end = Offset(width - padding, height - padding),
                            color = Color.Gray,
                            strokeWidth = 2f
                        )
                        
                        // Draw area chart
                        val areaPath = Path()
                        val linePath = Path()
                        
                        data.weeklyBalances.forEachIndexed { index, balance ->
                            val x = indexToX(index)
                            val y = valueToY(balance)
                            
                            if (index == 0) {
                                areaPath.moveTo(x, height - padding)
                                areaPath.lineTo(x, y)
                                linePath.moveTo(x, y)
                            } else {
                                areaPath.lineTo(x, y)
                                linePath.lineTo(x, y)
                            }
                        }
                        
                        // Close the area path
                        areaPath.lineTo(indexToX(data.weeklyBalances.size - 1), height - padding)
                        areaPath.close()
                        
                        // Draw filled area
                        drawPath(
                            path = areaPath,
                            color = AccentBlue.copy(alpha = 0.3f)
                        )
                        
                        // Draw line on top
                        drawPath(
                            path = linePath,
                            color = AccentBlue,
                            style = Stroke(width = 3f)
                        )
                        
                        // Draw points
                        data.weeklyBalances.forEachIndexed { index, balance ->
                            val x = indexToX(index)
                            val y = valueToY(balance)
                            drawCircle(
                                color = AccentBlue,
                                radius = 5f,
                                center = Offset(x, y)
                            )
                        }
                        
                        // Draw Y-axis labels
                        val labelPaint = android.graphics.Paint().apply {
                            color = android.graphics.Color.DKGRAY
                            textSize = 24f
                            isAntiAlias = true
                        }
                        
                        for (i in 0..4) {
                            val value = minBalance + (i * valueRange / 4)
                            val y = height - padding - (i * (height - 2 * padding) / 4)
                            drawContext.canvas.nativeCanvas.drawText(
                                String.format("%.0f", value),
                                padding - 35f,
                                y + 8f,
                                labelPaint
                            )
                        }
                        
                        // Draw X-axis labels (week numbers)
                        for (i in 0 until data.weeklyBalances.size.coerceAtMost(6)) {
                            val index = (i * (data.weeklyBalances.size - 1) / 5).coerceAtMost(data.weeklyBalances.size - 1)
                            val x = indexToX(index)
                            drawContext.canvas.nativeCanvas.drawText(
                                "W${index + 1}",
                                x - 10f,
                                height - padding + 30f,
                                labelPaint
                            )
                        }
                    }
                } else {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            "No data available",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
        
        if (showSettings) {
            BalanceTrendSettingsDialog(
                config = config,
                onUpdate = onUpdate,
                onDismiss = { showSettings = false },
                onDelete = onDelete
            )
        }
    }
}

@Composable
private fun BalanceTrendSettingsDialog(
    config: BalanceTrendCardConfig,
    onUpdate: (BalanceTrendCardConfig) -> Unit,
    onDismiss: () -> Unit,
    onDelete: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        dismissButton = {
            TextButton(
                onClick = onDelete,
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
