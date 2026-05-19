package com.osscabinet.ossWallet.ui.components.visualization.charts

import androidx.compose.foundation.*
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.scrollable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.osscabinet.ossWallet.ui.theme.AeonTheme
import com.osscabinet.ossWallet.ui.theme.AppColors
import com.osscabinet.ossWallet.ui.theme.AppTextStyles
import java.math.BigDecimal
import java.math.RoundingMode

@Composable
fun AeonUpSetPlot(
    data: AeonUpSetData,
    currency: String,
    modifier: Modifier = Modifier,
    dotColorActive: Color = AppColors.primary,
    dotColorInactive: Color = AppColors.border.copy(alpha = 0.2f),
    lineColor: Color = AppColors.primary,
    onIntersectionClick: (AeonUpSetIntersection) -> Unit = {},
    onSetClick: (Int) -> Unit = {}
) {
    if (data.sets.isEmpty()) return

    val setNames = data.sets.map { it.name }
    val maxSetValue = data.sets.maxOfOrNull { it.total.abs() } ?: BigDecimal.ONE
    val maxIntersectionValue = data.intersections.maxOfOrNull { it.value.abs() } ?: BigDecimal.ONE

    val rowHeight = 24.dp
    val colWidth = 24.dp
    val leftPanelWidth = 80.dp
    val rightPanelWidth = 80.dp

    val verticalScrollState = rememberScrollState()
    val horizontalScrollState = rememberScrollState()

    Column(modifier = modifier) {
        // Top row: Empty | Intersection Bars | Empty
        Row(modifier = Modifier.fillMaxWidth()) {
            Box(modifier = Modifier.width(leftPanelWidth))
            
            // Intersection Size Bars (Horizontal Scroll)
            Box(modifier = Modifier.weight(1f).horizontalScroll(horizontalScrollState)) {
                Row(
                    modifier = Modifier.height(60.dp).padding(bottom = 2.dp),
                    horizontalArrangement = Arrangement.Start,
                    verticalAlignment = Alignment.Bottom
                ) {
                    data.intersections.forEach { intersection ->
                        val ratio = if (maxIntersectionValue > BigDecimal.ZERO) {
                            intersection.value.abs().divide(maxIntersectionValue, 4, RoundingMode.HALF_UP).toFloat()
                        } else 0f
                        
                        Box(
                            modifier = Modifier
                                .width(colWidth)
                                .fillMaxHeight(ratio.coerceAtLeast(0.05f))
                                .padding(horizontal = 6.dp)
                                .background(AppColors.primary.copy(alpha = 0.8f), RoundedCornerShape(topStart = 2.dp, topEnd = 2.dp))
                                .clickable { onIntersectionClick(intersection) }
                        )
                    }
                }
            }
            
            Box(modifier = Modifier.width(rightPanelWidth))
        }

        // Main content: Set names | Dot Matrix | Set Size Bars
        Row(modifier = Modifier.fillMaxWidth().heightIn(max = 300.dp)) {
            // Left Panel: Set names (Vertical Scroll)
            Column(
                modifier = Modifier
                    .width(leftPanelWidth)
                    .verticalScroll(verticalScrollState)
            ) {
                setNames.forEachIndexed { index, name ->
                    Box(
                        modifier = Modifier
                            .height(rowHeight)
                            .fillMaxWidth()
                            .clickable { onSetClick(index) },
                        contentAlignment = Alignment.CenterStart
                    ) {
                        Text(
                            text = name,
                            style = AppTextStyles.caption.copy(fontSize = 9.sp, fontWeight = FontWeight.SemiBold),
                            color = AppColors.contentPrimary,
                            maxLines = 1,
                            modifier = Modifier.padding(horizontal = 4.dp)
                        )
                    }
                }
            }

            // Center: Dot Matrix (Dual Scroll)
            Box(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(verticalScrollState)
                    .horizontalScroll(horizontalScrollState)
            ) {
                Canvas(
                    modifier = Modifier
                        .width(colWidth * data.intersections.size)
                        .height(rowHeight * setNames.size)
                ) {
                    val dx = colWidth.toPx()
                    val dy = rowHeight.toPx()
                    val radius = 5.dp.toPx()

                    data.intersections.forEachIndexed { colIdx, intersection ->
                        val x = dx * (colIdx + 0.5f)
                        
                        if (intersection.setIndices.size > 1) {
                            val activeIndices = intersection.setIndices.sorted()
                            val minY = dy * (activeIndices.first() + 0.5f)
                            val maxY = dy * (activeIndices.last() + 0.5f)
                            drawLine(
                                color = lineColor,
                                start = Offset(x, minY),
                                end = Offset(x, maxY),
                                strokeWidth = 2.dp.toPx()
                            )
                        }

                        for (rowIdx in 0 until setNames.size) {
                            val y = dy * (rowIdx + 0.5f)
                            val isActive = intersection.setIndices.contains(rowIdx)
                            drawCircle(
                                color = if (isActive) dotColorActive else dotColorInactive,
                                radius = radius,
                                center = Offset(x, y),
                                style = Fill
                            )
                        }
                    }
                }
                
                // Overlay invisible clickable boxes for intersection interactivity
                Row {
                    data.intersections.forEach { intersection ->
                        Box(
                            modifier = Modifier
                                .width(colWidth)
                                .height(rowHeight * setNames.size)
                                .clickable { onIntersectionClick(intersection) }
                        )
                    }
                }
            }

            // Right Panel: Set size bars (Vertical Scroll)
            Column(
                modifier = Modifier
                    .width(rightPanelWidth)
                    .verticalScroll(verticalScrollState)
            ) {
                data.sets.forEachIndexed { index, set ->
                    val ratio = if (maxSetValue > BigDecimal.ZERO) {
                        set.total.abs().divide(maxSetValue, 4, RoundingMode.HALF_UP).toFloat()
                    } else 0f
                    
                    Box(
                        modifier = Modifier
                            .height(rowHeight)
                            .fillMaxWidth()
                            .clickable { onSetClick(index) }
                            .padding(vertical = 8.dp, horizontal = 4.dp),
                        contentAlignment = Alignment.CenterStart
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth(ratio.coerceAtLeast(0.05f))
                                .fillMaxHeight()
                                .background(AppColors.accent.copy(alpha = 0.6f), RoundedCornerShape(1.dp))
                        )
                    }
                }
            }
        }
    }
}

@Preview(showBackground = true, widthDp = 400, heightDp = 600)
@Composable
fun PreviewUpSetPlotLarge() {
    val sets = (1..15).map { AeonUpSetSet("Tag $it", BigDecimal(it * 100)) }
    val intersections = listOf(
        AeonUpSetIntersection(setOf(0, 1, 2), BigDecimal(500), BigDecimal(600), BigDecimal(100), emptyMap(), emptyMap(), 10),
        AeonUpSetIntersection(setOf(0, 1), BigDecimal(300), BigDecimal(400), BigDecimal(100), emptyMap(), emptyMap(), 5),
        AeonUpSetIntersection(setOf(2, 3, 4, 5), BigDecimal(800), BigDecimal(1000), BigDecimal(200), emptyMap(), emptyMap(), 15),
        AeonUpSetIntersection(setOf(6, 7), BigDecimal(200), BigDecimal(300), BigDecimal(100), emptyMap(), emptyMap(), 3),
        AeonUpSetIntersection(setOf(0, 5, 10), BigDecimal(450), BigDecimal(600), BigDecimal(150), emptyMap(), emptyMap(), 8),
        AeonUpSetIntersection(setOf(12, 13, 14), BigDecimal(1200), BigDecimal(1500), BigDecimal(300), emptyMap(), emptyMap(), 20),
        AeonUpSetIntersection(setOf(1, 4, 7, 10), BigDecimal(150), BigDecimal(200), BigDecimal(50), emptyMap(), emptyMap(), 2),
        AeonUpSetIntersection(setOf(8), BigDecimal(100), BigDecimal(150), BigDecimal(50), emptyMap(), emptyMap(), 1)
    )
    
    com.osscabinet.ossWallet.ui.theme.OssWalletTheme {
        androidx.compose.material3.Surface(color = Color.White, modifier = Modifier.padding(16.dp)) {
            AeonUpSetPlot(
                data = AeonUpSetData(sets, intersections),
                currency = "USD",
                modifier = Modifier.fillMaxSize()
            )
        }
    }
}

@Preview(showBackground = true, widthDp = 300, heightDp = 300)
@Composable
fun PreviewUpSetPlotSmall() {
    val sets = listOf(
        AeonUpSetSet("Work", BigDecimal(1000)),
        AeonUpSetSet("Travel", BigDecimal(500)),
        AeonUpSetSet("Food", BigDecimal(300))
    )
    val intersections = listOf(
        AeonUpSetIntersection(setOf(0, 1), BigDecimal(200), BigDecimal(300), BigDecimal(100), emptyMap(), emptyMap(), 2),
        AeonUpSetIntersection(setOf(0, 2), BigDecimal(150), BigDecimal(200), BigDecimal(50), emptyMap(), emptyMap(), 3),
        AeonUpSetIntersection(setOf(0, 1, 2), BigDecimal(50), BigDecimal(100), BigDecimal(50), emptyMap(), emptyMap(), 1)
    )
    
    com.osscabinet.ossWallet.ui.theme.OssWalletTheme {
        androidx.compose.material3.Surface(color = Color.White, modifier = Modifier.padding(16.dp)) {
            AeonUpSetPlot(
                data = AeonUpSetData(sets, intersections),
                currency = "USD"
            )
        }
    }
}
