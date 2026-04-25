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
import androidx.compose.material3.Slider
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
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.osscabinet.ossWallet.domain.model.settings.BalanceForecastCardConfig
import com.osscabinet.ossWallet.ui.components.AeonFlowSectionHeader
import com.osscabinet.ossWallet.ui.components.TimelineFilterBar
import com.osscabinet.ossWallet.ui.theme.PositiveAmountColor
import com.osscabinet.ossWallet.ui.theme.NegativeAmountColor
import com.osscabinet.ossWallet.ui.theme.AccentBlue
import com.osscabinet.ossWallet.ui.util.formatCurrencyRight
import java.math.BigDecimal

@Composable
fun BalanceForecastCard(
    config: BalanceForecastCardConfig,
    data: BalanceForecastData,
    modifier: Modifier = Modifier,
    onUpdate: (BalanceForecastCardConfig) -> Unit = {},
    onDelete: () -> Unit = {}
) {
    var showSettings by remember { mutableStateOf(false) }
    var forecastDays by remember { mutableStateOf(config.forecastDays) }
    var selectedPointIndex by remember { mutableStateOf<Int?>(null) }
    
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
            Column {
                AeonFlowSectionHeader(
                    title = config.title,
                    trailing = {
                        IconButton(onClick = { showSettings = true }) {
                            Icon(Icons.Default.Settings, contentDescription = "Settings")
                        }
                    }
                )
                Text(
                    config.subtitle,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            
            // Forecast value with trend
            Row(verticalAlignment = Alignment.Bottom) {
                Text(
                    formatCurrencyRight(data.forecastBalance, data.currency, data.precision),
                    fontSize = 24.sp,
                    color = if (data.forecastBalance >= java.math.BigDecimal.ZERO) 
                        PositiveAmountColor 
                    else 
                        NegativeAmountColor,
                    fontWeight = FontWeight.Bold
                )
                Spacer(Modifier.weight(1f))
                Text(
                    "in $forecastDays days",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            
            // Forecast days slider
            Column {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        "Forecast Days: $forecastDays",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Medium
                    )
                }
                Slider(
                    value = forecastDays.toFloat(),
                    onValueChange = { 
                        forecastDays = it.toInt()
                        onUpdate(config.copy(forecastDays = it.toInt()))
                    },
                    valueRange = 7f..90f,
                    steps = 83,
                    modifier = Modifier.fillMaxWidth()
                )
            }
            
            // Interactive waterfall chart with proper scales
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(220.dp)
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f), RoundedCornerShape(8.dp))
                    .clip(RoundedCornerShape(8.dp))
                    .clickable { selectedPointIndex = null }
            ) {
                if (data.chartData.isNotEmpty()) {
                    Canvas(modifier = Modifier.fillMaxSize().padding(16.dp)) {
                        val width = size.width
                        val height = size.height
                        val padding = 40f
                        
                        // Calculate min and max for scaling from all three series
                        val allValues = data.chartData.flatMap { listOf(it.first, it.second, it.third) }
                        val maxValue = allValues.maxOrNull() ?: 1.0
                        val minValue = allValues.minOrNull() ?: 0.0
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
                        
                        // Draw optimistic line (green)
                        val optimisticPath = Path()
                        data.chartData.forEachIndexed { index, (optimistic, _, _) ->
                            val x = indexToX(index)
                            val y = valueToY(optimistic)
                            if (index == 0) optimisticPath.moveTo(x, y) else optimisticPath.lineTo(x, y)
                        }
                        drawPath(
                            path = optimisticPath,
                            color = PositiveAmountColor.copy(alpha = 0.5f),
                            style = Stroke(width = 2f, pathEffect = PathEffect.dashPathEffect(floatArrayOf(5f, 5f)))
                        )
                        
                        // Draw realistic line (blue)
                        val realisticPath = Path()
                        data.chartData.forEachIndexed { index, (_, realistic, _) ->
                            val x = indexToX(index)
                            val y = valueToY(realistic)
                            if (index == 0) realisticPath.moveTo(x, y) else realisticPath.lineTo(x, y)
                        }
                        drawPath(
                            path = realisticPath,
                            color = AccentBlue,
                            style = Stroke(width = 3f)
                        )
                        
                        // Draw pessimistic line (red)
                        val pessimisticPath = Path()
                        data.chartData.forEachIndexed { index, (_, _, pessimistic) ->
                            val x = indexToX(index)
                            val y = valueToY(pessimistic)
                            if (index == 0) pessimisticPath.moveTo(x, y) else pessimisticPath.lineTo(x, y)
                        }
                        drawPath(
                            path = pessimisticPath,
                            color = NegativeAmountColor.copy(alpha = 0.5f),
                            style = Stroke(width = 2f, pathEffect = PathEffect.dashPathEffect(floatArrayOf(5f, 5f)))
                        )
                        
                        // Draw points on realistic line
                        data.chartData.forEachIndexed { index, (_, realistic, _) ->
                            val x = indexToX(index)
                            val y = valueToY(realistic)
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
                        
                        // Draw X-axis labels (forecast periods)
                        for (i in 0 until data.chartData.size.coerceAtMost(6)) {
                            val index = (i * (data.chartData.size - 1) / 5).coerceAtMost(data.chartData.size - 1)
                            val x = indexToX(index)
                            val daysLabel = (index * forecastDays / (data.chartData.size - 1)).toInt()
                            drawContext.canvas.nativeCanvas.drawText(
                                "+${daysLabel}d",
                                x - 15f,
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
        BalanceForecastSettingsDialog(
            config = config,
            onUpdate = onUpdate,
            onDismiss = { showSettings = false },
            onDelete = onDelete
        )
    }
}

@Composable
private fun BalanceForecastSettingsDialog(
    config: BalanceForecastCardConfig,
    onUpdate: (BalanceForecastCardConfig) -> Unit,
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

data class BalanceForecastData(
    val forecastBalance: BigDecimal,
    val currency: String,
    val precision: Int,
    val chartData: List<Triple<Double, Double, Double>>
)
