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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.osscabinet.ossWallet.domain.model.settings.PeriodComparisonCardConfig
import com.osscabinet.ossWallet.ui.components.AeonFlowSectionHeader
import com.osscabinet.ossWallet.ui.components.TimelineFilterBar
import com.osscabinet.ossWallet.ui.theme.AccentBlue
import com.osscabinet.ossWallet.ui.util.formatCurrencyRight
import java.math.BigDecimal

@Composable
fun PeriodComparisonCard(
    config: PeriodComparisonCardConfig,
    data: PeriodComparisonData,
    modifier: Modifier = Modifier,
    onUpdate: (PeriodComparisonCardConfig) -> Unit = {},
    onDelete: () -> Unit = {}
) {
    var showSettings by remember { mutableStateOf(false) }
    var selectedTab by remember { mutableStateOf(config.selectedTab) }
    
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
            
            // Current value
            Text(
                formatCurrencyRight(data.currentValue, data.currency, data.precision),
                fontSize = 24.sp,
                color = MaterialTheme.colorScheme.onSurface,
                fontWeight = FontWeight.Bold
            )
            
            // Interactive Tabs
            Row(
                modifier = Modifier
                    .padding(vertical = 12.dp)
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant)
            ) {
                listOf("Cash flow", "Expenses", "Income").forEach { tab ->
                    val isSelected = tab == selectedTab
                    Text(
                        tab,
                        modifier = Modifier
                            .weight(1f)
                            .background(
                                if (isSelected) AccentBlue else Color.Transparent,
                                RoundedCornerShape(8.dp)
                            )
                            .clickable { selectedTab = tab }
                            .padding(8.dp),
                        textAlign = TextAlign.Center,
                        color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 12.sp,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                    )
                }
            }
            
            // Interactive line chart with proper scales
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp)
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f), RoundedCornerShape(8.dp))
                    .clip(RoundedCornerShape(8.dp))
            ) {
                if (data.chartData.isNotEmpty()) {
                    Canvas(modifier = Modifier.fillMaxSize().padding(16.dp)) {
                        val width = size.width
                        val height = size.height
                        val padding = 40f
                        
                        // Calculate min and max for scaling
                        val maxValue = data.chartData.maxOrNull() ?: 1.0
                        val minValue = data.chartData.minOrNull() ?: 0.0
                        val valueRange = (maxValue - minValue).takeIf { it > 0 } ?: 1.0
                        
                        // Helper functions for mapping
                        fun valueToY(value: Double): Float {
                            val normalized = ((value - minValue) / valueRange).toFloat()
                            return height - padding - (normalized * (height - 2 * padding))
                        }
                        
                        fun indexToX(index: Int): Float {
                            return padding + (index.toFloat() / (data.chartData.size - 1).coerceAtLeast(1)) * (width - 2 * padding)
                        }
                        
                        // Draw grid lines (horizontal)
                        val gridPaint = android.graphics.Paint().apply {
                            color = android.graphics.Color.parseColor("#E0E0E0")
                            strokeWidth = 1f
                            isAntiAlias = true
                        }
                        
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
                        
                        // Draw line chart
                        val path = Path()
                        data.chartData.forEachIndexed { index, value ->
                            val x = indexToX(index)
                            val y = valueToY(value)
                            if (index == 0) path.moveTo(x, y) else path.lineTo(x, y)
                        }
                        
                        drawPath(
                            path = path,
                            color = AccentBlue,
                            style = Stroke(width = 3f)
                        )
                        
                        // Draw points
                        data.chartData.forEachIndexed { index, value ->
                            val x = indexToX(index)
                            val y = valueToY(value)
                            drawCircle(
                                color = AccentBlue,
                                radius = 6f,
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
                        for (i in 0 until data.chartData.size.coerceAtMost(6)) {
                            val index = (i * (data.chartData.size - 1) / 5).coerceAtMost(data.chartData.size - 1)
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
        PeriodComparisonSettingsDialog(
            config = config,
            onUpdate = onUpdate,
            onDismiss = { showSettings = false },
            onDelete = onDelete
        )
    }
}

@Composable
private fun PeriodComparisonSettingsDialog(
    config: PeriodComparisonCardConfig,
    onUpdate: (PeriodComparisonCardConfig) -> Unit,
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

data class PeriodComparisonData(
    val currentValue: BigDecimal,
    val currency: String,
    val precision: Int,
    val chartData: List<Double>
)
