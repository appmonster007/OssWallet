package com.osscabinet.ossWallet.ui.components.visualization.charts

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.osscabinet.ossWallet.domain.model.analytics.TimelineTrajectoryData
import java.math.BigDecimal
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Multi-line chart for timeline trajectory visualization.
 * Displays balance, expense, and income trajectories over time.
 */
@Composable
fun TimelineTrajectoryLineChart(
    data: List<TimelineTrajectoryData>,
    modifier: Modifier = Modifier,
    showBalance: Boolean = true,
    showExpense: Boolean = true,
    showIncome: Boolean = true
) {
    Canvas(modifier = modifier.fillMaxSize().padding(16.dp)) {
        if (data.isEmpty()) return@Canvas
        
        val width = size.width
        val height = size.height
        val padding = 40f
        
        // Find min and max values for scaling
        val allValues = mutableListOf<BigDecimal>()
        if (showBalance) allValues.addAll(data.map { it.balance })
        if (showExpense) allValues.addAll(data.map { it.expense })
        if (showIncome) allValues.addAll(data.map { it.income })
        
        val minValue = allValues.minOrNull() ?: BigDecimal.ZERO
        val maxValue = allValues.maxOrNull() ?: BigDecimal.ONE
        val valueRange = (maxValue - minValue).takeIf { it > BigDecimal.ZERO } ?: BigDecimal.ONE
        
        // Helper function to map values to canvas coordinates
        fun valueToY(value: BigDecimal): Float {
            val normalized = (value - minValue).toFloat() / valueRange.toFloat()
            return height - padding - (normalized * (height - 2 * padding))
        }
        
        fun indexToX(index: Int): Float {
            return padding + (index.toFloat() / (data.size - 1).coerceAtLeast(1)) * (width - 2 * padding)
        }
        
        // Draw axes
        val axisPaint = android.graphics.Paint().apply {
            color = android.graphics.Color.GRAY
            strokeWidth = 2f
            isAntiAlias = true
        }
        
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
        
        // Draw balance line
        if (showBalance) {
            val balancePath = Path()
            data.forEachIndexed { index, point ->
                val x = indexToX(index)
                val y = valueToY(point.balance)
                if (index == 0) balancePath.moveTo(x, y) else balancePath.lineTo(x, y)
            }
            drawPath(
                path = balancePath,
                color = Color(0xFF4CAF50), // Green for balance
                style = Stroke(width = 3f, pathEffect = null)
            )
        }
        
        // Draw expense line
        if (showExpense) {
            val expensePath = Path()
            data.forEachIndexed { index, point ->
                val x = indexToX(index)
                val y = valueToY(point.expense)
                if (index == 0) expensePath.moveTo(x, y) else expensePath.lineTo(x, y)
            }
            drawPath(
                path = expensePath,
                color = Color(0xFFF44336), // Red for expense
                style = Stroke(width = 3f, pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 5f)))
            )
        }
        
        // Draw income line
        if (showIncome) {
            val incomePath = Path()
            data.forEachIndexed { index, point ->
                val x = indexToX(index)
                val y = valueToY(point.income)
                if (index == 0) incomePath.moveTo(x, y) else incomePath.lineTo(x, y)
            }
            drawPath(
                path = incomePath,
                color = Color(0xFF2196F3), // Blue for income
                style = Stroke(width = 3f, pathEffect = null)
            )
        }
        
        // Draw date labels (simplified - show first and last)
        val labelPaint = android.graphics.Paint().apply {
            color = android.graphics.Color.DKGRAY
            textSize = 24f
            isAntiAlias = true
        }
        
        val dateFormat = SimpleDateFormat("MMM dd", Locale.getDefault())
        
        if (data.isNotEmpty()) {
            drawContext.canvas.nativeCanvas.apply {
                // First date
                drawText(
                    dateFormat.format(Date(data.first().timestamp)),
                    padding,
                    height - padding + 30f,
                    labelPaint
                )
                
                // Last date
                drawText(
                    dateFormat.format(Date(data.last().timestamp)),
                    width - padding - 60f,
                    height - padding + 30f,
                    labelPaint
                )
            }
        }
    }
}

/**
 * Legend for timeline trajectory chart.
 */
@Composable
fun TimelineLegend(
    showBalance: Boolean,
    showExpense: Boolean,
    showIncome: Boolean
) {
    // Legend implementation would go here
    // For now, this is a placeholder
}
