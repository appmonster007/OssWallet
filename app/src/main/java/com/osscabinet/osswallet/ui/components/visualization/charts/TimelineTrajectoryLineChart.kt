package com.osscabinet.ossWallet.ui.components.visualization.charts

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.osscabinet.ossWallet.domain.model.analytics.TimelineTrajectoryData
import com.osscabinet.ossWallet.ui.util.formatShortNumber
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
    Canvas(modifier = modifier.fillMaxSize().padding(horizontal = 8.dp, vertical = 24.dp)) {
        if (data.isEmpty()) return@Canvas
        
        val width = size.width
        val height = size.height
        val leftPadding = 50.dp.toPx()
        val bottomPadding = 30.dp.toPx()
        val topPadding = 10.dp.toPx()
        val rightPadding = 10.dp.toPx()
        
        val chartWidth = width - leftPadding - rightPadding
        val chartHeight = height - bottomPadding - topPadding
        
        // Find min and max values for scaling
        val allValues = mutableListOf<BigDecimal>()
        if (showBalance) allValues.addAll(data.map { it.balance })
        if (showExpense) allValues.addAll(data.map { it.expense })
        if (showIncome) allValues.addAll(data.map { it.income })
        
        val minValueRaw = allValues.minOrNull() ?: BigDecimal.ZERO
        val maxValueRaw = allValues.maxOrNull() ?: BigDecimal.ONE
        
        // Add some headroom
        val buffer = (maxValueRaw - minValueRaw).multiply(BigDecimal("0.1"))
        val minValue = (minValueRaw - buffer).coerceAtMost(BigDecimal.ZERO)
        val maxValue = (maxValueRaw + buffer).coerceAtLeast(BigDecimal.ONE)
        val valueRange = (maxValue - minValue).takeIf { it > BigDecimal.ZERO } ?: BigDecimal.ONE
        
        // Helper function to map values to canvas coordinates
        fun valueToY(value: BigDecimal): Float {
            val normalized = (value - minValue).toFloat() / valueRange.toFloat()
            return topPadding + chartHeight - (normalized * chartHeight)
        }
        
        fun indexToX(index: Int): Float {
            return leftPadding + (index.toFloat() / (data.size - 1).coerceAtLeast(1)) * chartWidth
        }

        // Draw horizontal grid lines and Y-axis labels
        val gridLines = 4
        val labelPaint = android.graphics.Paint().apply {
            color = android.graphics.Color.GRAY
            textSize = 10.sp.toPx()
            textAlign = android.graphics.Paint.Align.RIGHT
            isAntiAlias = true
        }

        for (i in 0..gridLines) {
            val ratio = i.toFloat() / gridLines
            val yValue = minValue + valueRange.multiply(BigDecimal(ratio.toString()))
            val yPos = valueToY(yValue)
            
            // Grid line
            drawLine(
                color = Color.LightGray.copy(alpha = 0.4f),
                start = Offset(leftPadding, yPos),
                end = Offset(width - rightPadding, yPos),
                strokeWidth = 1.dp.toPx()
            )
            
            // Label
            drawContext.canvas.nativeCanvas.drawText(
                formatShortNumber(yValue),
                leftPadding - 8.dp.toPx(),
                yPos + 4.dp.toPx(),
                labelPaint
            )
        }
        
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
                color = Color(0xFF4CAF50), // Green
                style = Stroke(width = 2.5.dp.toPx(), cap = StrokeCap.Round)
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
                color = Color(0xFFF44336), // Red
                style = Stroke(
                    width = 2.dp.toPx(), 
                    pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 10f)),
                    cap = StrokeCap.Round
                )
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
                color = Color(0xFF2196F3), // Blue
                style = Stroke(width = 2.dp.toPx(), cap = StrokeCap.Round)
            )
        }
        
        // Draw date labels (Start, Middle, End)
        val dateLabelPaint = android.graphics.Paint().apply {
            color = android.graphics.Color.GRAY
            textSize = 10.sp.toPx()
            textAlign = android.graphics.Paint.Align.CENTER
            isAntiAlias = true
        }
        val dateFormat = SimpleDateFormat("MMM dd", Locale.getDefault())
        
        if (data.size >= 2) {
            val indicesToShow = if (data.size > 2) listOf(0, data.size / 2, data.size - 1) else listOf(0, data.size - 1)
            indicesToShow.forEach { index ->
                val x = indexToX(index)
                val dateStr = dateFormat.format(Date(data[index].timestamp))
                drawContext.canvas.nativeCanvas.drawText(
                    dateStr,
                    x,
                    height - 5.dp.toPx(),
                    dateLabelPaint
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
    Row(
        modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (showBalance) {
            LegendItem(color = Color(0xFF4CAF50), label = "Balance")
            Spacer(modifier = Modifier.width(16.dp))
        }
        if (showIncome) {
            LegendItem(color = Color(0xFF2196F3), label = "Income")
            Spacer(modifier = Modifier.width(16.dp))
        }
        if (showExpense) {
            LegendItem(color = Color(0xFFF44336), label = "Expense", isDashed = true)
        }
    }
}

@Composable
private fun LegendItem(color: Color, label: String, isDashed: Boolean = false) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        if (isDashed) {
            Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                repeat(3) {
                    Box(
                        modifier = Modifier
                            .size(width = 4.dp, height = 2.dp)
                            .background(color, shape = RoundedCornerShape(1.dp))
                    )
                }
            }
        } else {
            Box(
                modifier = Modifier
                    .size(width = 12.dp, height = 3.dp)
                    .background(color, shape = RoundedCornerShape(2.dp))
            )
        }
        Spacer(modifier = Modifier.width(4.dp))
        Text(text = label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@androidx.compose.ui.tooling.preview.Preview(showBackground = true)
@Composable
fun TimelineTrajectoryLineChartPreview() {
    val sampleData = (0..30).map { i ->
        TimelineTrajectoryData(
            timestamp = System.currentTimeMillis() - (30 - i) * 24 * 60 * 60 * 1000L,
            balance = BigDecimal(10000 + i * 500),
            income = if (i % 7 == 0) BigDecimal(3000) else BigDecimal.ZERO,
            expense = if (i % 3 == 0) BigDecimal(1200) else BigDecimal.ZERO
        )
    }
    
    MaterialTheme {
        Column(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
            Box(modifier = Modifier.height(250.dp)) {
                TimelineTrajectoryLineChart(data = sampleData)
            }
            TimelineLegend(showBalance = true, showExpense = true, showIncome = true)
        }
    }
}
