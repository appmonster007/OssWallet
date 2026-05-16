package com.osscabinet.ossWallet.ui.components.visualization.charts

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.osscabinet.ossWallet.ui.screens.analytics.components.CategoryPieChartData
import com.osscabinet.ossWallet.domain.service.currency.CurrencyFormatter.formatAmount
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin
import kotlin.math.sqrt
import com.osscabinet.ossWallet.ui.theme.AppColors
import com.osscabinet.ossWallet.ui.theme.AppTextStyles

/**
 * Interactive pie wheel chart for category breakdown.
 * Displays net balance distribution across categories with tap interactions.
 */
@Composable
fun CategoryBreakdownPieChart(
    data: List<CategoryPieChartData>,
    modifier: Modifier = Modifier,
    onSegmentClick: (CategoryPieChartData) -> Unit = {},
    onSegmentDoubleClick: (CategoryPieChartData) -> Unit = {},
    showLabels: Boolean = true
) {
    var selectedSegment by remember { mutableStateOf<CategoryPieChartData?>(null) }
    var showSubcategories by remember { mutableStateOf(false) }
    var currentData by remember { mutableStateOf(data) }
    val backgroundColor = AppColors.screenBackground
    val surfaceColor = AppColors.panelSurface
    
    // When data changes, reset to top-level view
    if (data != currentData && !showSubcategories) {
        currentData = data
    }
    
    Box(modifier = modifier) {
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(Unit) {
                    var lastTapTime = 0L
                    detectTapGestures { offset ->
                        val currentTime = System.currentTimeMillis()
                        val isDoubleClick = currentTime - lastTapTime < 300L
                        lastTapTime = currentTime
                        
                        val centerX = size.width / 2
                        val centerY = size.height / 2
                        val dx = offset.x - centerX
                        val dy = offset.y - centerY
                        val distance = sqrt(dx * dx + dy * dy)
                        val radius = min(size.width, size.height) / 2 * 0.8f
                        
                        if (distance <= radius) {
                            val angle = atan2(dy, dx).let {
                                if (it < 0) it + 2 * Math.PI else it
                            }.toFloat()
                            
                            var currentAngle = 0f
                            for (segment in currentData) {
                                val sweepAngle = segment.percentage * 360f
                                if (angle in currentAngle..(currentAngle + sweepAngle)) {
                                    if (isDoubleClick && segment.hasSubcategories) {
                                        onSegmentDoubleClick(segment)
                                        showSubcategories = true
                                        currentData = segment.subcategoryData ?: emptyList()
                                    } else {
                                        selectedSegment = segment
                                        onSegmentClick(segment)
                                        if (!segment.hasSubcategories) {
                                            showSubcategories = false
                                            currentData = data
                                        }
                                    }
                                    return@detectTapGestures
                                }
                                currentAngle += sweepAngle
                            }
                        }
                    }
                }
        ) {
            val centerX = size.width / 2
            val centerY = size.height / 2
            val radius = min(size.width, size.height) / 2 * 0.8f
            var startAngle = -90f
            
            for (segment in currentData) {
                val sweepAngle = segment.percentage * 360f
                val isSelected = selectedSegment?.category == segment.category
                val segmentRadius = if (isSelected) radius * 1.05f else radius
                
                drawArc(
                    color = Color(segment.colorHex),
                    startAngle = startAngle,
                    sweepAngle = sweepAngle,
                    useCenter = true,
                    size = Size(segmentRadius * 2, segmentRadius * 2),
                    topLeft = Offset(
                        centerX - segmentRadius,
                        centerY - segmentRadius
                    )
                )
                
                // Draw segment separator
                drawArc(
                    color = backgroundColor,
                    startAngle = startAngle,
                    sweepAngle = 2f,
                    useCenter = true,
                    size = Size(segmentRadius * 2, segmentRadius * 2),
                    topLeft = Offset(
                        centerX - segmentRadius,
                        centerY - segmentRadius
                    )
                )
                
                startAngle += sweepAngle
            }
            
            // Draw center circle for donut chart effect
            drawArc(
                color = surfaceColor,
                startAngle = 0f,
                sweepAngle = 360f,
                useCenter = true,
                size = Size(radius * 0.6f, radius * 0.6f),
                topLeft = Offset(
                    centerX - radius * 0.3f,
                    centerY - radius * 0.3f
                )
            )
        }
        
        // Center text overlay
        if (showLabels) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                val displaySegment = selectedSegment ?: currentData.firstOrNull()
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = displaySegment?.category ?: "Total",
                        style = AppTextStyles.screenSubTitle,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center
                    )
                    if (displaySegment != null) {
                        Text(
                            text = formatAmount(displaySegment.amount.abs()),
                            style = AppTextStyles.transactionTitle,
                            textAlign = TextAlign.Center,
                            color = AppColors.contentPrimary
                        )
                        if (displaySegment.hasSubcategories && !showSubcategories) {
                            Text(
                                text = "Double tap for subcategories",
                                style = AppTextStyles.transactionSwipeAction,
                                textAlign = TextAlign.Center,
                                color = AppColors.contentSecondary
                            )
                        }
                    }
                }
            }
        }
    }
}

private fun formatCurrency(amount: java.math.BigDecimal): String {
    return if (amount >= java.math.BigDecimal.ZERO) {
        "+$amount"
    } else {
        amount.toString()
    }
}
