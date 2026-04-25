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
import com.osscabinet.ossWallet.domain.model.settings.CashFlowTrendCardConfig
import com.osscabinet.ossWallet.ui.components.AeonFlowSectionHeader
import com.osscabinet.ossWallet.ui.components.TimelineFilterBar
import com.osscabinet.ossWallet.ui.theme.PositiveAmountColor
import com.osscabinet.ossWallet.ui.theme.NegativeAmountColor
import com.osscabinet.ossWallet.ui.theme.AccentBlue

@Composable
fun CashFlowTrendCard(
    config: CashFlowTrendCardConfig,
    data: CashFlowTrendData,
    modifier: Modifier = Modifier,
    onUpdate: (CashFlowTrendCardConfig) -> Unit = {},
    onDelete: () -> Unit = {}
) {
    var showSettings by remember { mutableStateOf(false) }
    var selectedBarIndex by remember { mutableStateOf<Int?>(null) }
    
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
            // Header
            AeonFlowSectionHeader(
                title = config.title,
                trailing = {
                    IconButton(onClick = { showSettings = true }) {
                        Icon(Icons.Default.Settings, contentDescription = "Settings")
                    }
                }
            )
            
            // Legend
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(12.dp)
                            .background(PositiveAmountColor, RoundedCornerShape(2.dp))
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Income", style = MaterialTheme.typography.labelSmall)
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(12.dp)
                            .background(NegativeAmountColor, RoundedCornerShape(2.dp))
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Expenses", style = MaterialTheme.typography.labelSmall)
                }
            }
            
            // Interactive bar + line chart with proper scales
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(220.dp)
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f), RoundedCornerShape(8.dp))
                    .clip(RoundedCornerShape(8.dp))
                    .clickable { selectedBarIndex = null }
            ) {
                if (data.barData.isNotEmpty() && data.lineData.isNotEmpty()) {
                    Canvas(modifier = Modifier.fillMaxSize().padding(16.dp)) {
                        val width = size.width
                        val height = size.height
                        val padding = 40f
                        
                        // Calculate min and max for scaling
                        val allValues = data.barData + data.lineData
                        val maxValue = allValues.maxOrNull() ?: 1.0
                        val minValue = allValues.minOrNull() ?: 0.0
                        val valueRange = (maxValue - minValue).takeIf { it > 0 } ?: 1.0
                        
                        // Helper functions for mapping
                        fun valueToY(value: Double): Float {
                            val normalized = ((value - minValue) / valueRange).toFloat()
                            return height - padding - (normalized * (height - 2 * padding))
                        }
                        
                        fun indexToX(index: Int): Float {
                            return padding + (index.toFloat() / (data.barData.size - 1).coerceAtLeast(1)) * (width - 2 * padding)
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
                        
                        // Draw zero line (if range includes both positive and negative)
                        if (minValue < 0 && maxValue > 0) {
                            val zeroY = valueToY(0.0)
                            drawLine(
                                start = Offset(padding, zeroY),
                                end = Offset(width - padding, zeroY),
                                color = Color.Gray,
                                strokeWidth = 1f
                            )
                        }
                        
                        // Draw bar chart for cash flow
                        val barWidth = (width - 2 * padding) / data.barData.size * 0.6f
                        data.barData.forEachIndexed { index, value ->
                            val x = indexToX(index)
                            val y = valueToY(value)
                            val barColor = if (value >= 0) PositiveAmountColor else NegativeAmountColor
                            
                            // Draw bar from zero line or bottom
                            val barTop = if (value >= 0) y else valueToY(0.0)
                            val barBottom = if (value >= 0) valueToY(0.0) else y
                            
                            drawRoundRect(
                                color = barColor,
                                topLeft = Offset(x - barWidth / 2, barTop),
                                size = androidx.compose.ui.geometry.Size(barWidth, (barBottom - barTop).coerceAtLeast(1f)),
                                cornerRadius = androidx.compose.ui.geometry.CornerRadius(4f)
                            )
                        }
                        
                        // Draw line chart for cumulative trend
                        val path = Path()
                        data.lineData.forEachIndexed { index, value ->
                            val x = indexToX(index)
                            val y = valueToY(value)
                            if (index == 0) path.moveTo(x, y) else path.lineTo(x, y)
                        }
                        
                        drawPath(
                            path = path,
                            color = AccentBlue,
                            style = Stroke(width = 3f)
                        )
                        
                        // Draw points on line
                        data.lineData.forEachIndexed { index, value ->
                            val x = indexToX(index)
                            val y = valueToY(value)
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
                            val value = minValue + (i * valueRange / 4)
                            val y = height - padding - (i * (height - 2 * padding) / 4)
                            drawContext.canvas.nativeCanvas.drawText(
                                String.format("%.0f", value),
                                padding - 35f,
                                y + 8f,
                                labelPaint
                            )
                        }
                        
                        // Draw X-axis labels (period numbers)
                        for (i in 0 until data.barData.size.coerceAtMost(6)) {
                            val index = (i * (data.barData.size - 1) / 5).coerceAtMost(data.barData.size - 1)
                            val x = indexToX(index)
                            drawContext.canvas.nativeCanvas.drawText(
                                "P${index + 1}",
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
    }
    
    if (showSettings) {
        CashFlowTrendSettingsDialog(
            config = config,
            onUpdate = onUpdate,
            onDismiss = { showSettings = false },
            onDelete = onDelete
        )
    }
}

@Composable
private fun CashFlowTrendSettingsDialog(
    config: CashFlowTrendCardConfig,
    onUpdate: (CashFlowTrendCardConfig) -> Unit,
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

data class CashFlowTrendData(
    val barData: List<Double>,
    val lineData: List<Double>
)
