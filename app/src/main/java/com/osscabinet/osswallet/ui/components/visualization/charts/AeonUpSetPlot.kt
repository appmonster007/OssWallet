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
import com.osscabinet.ossWallet.domain.model.settings.UpSetDynamicFilteringMode
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
    maxVisibleRows: Int = 10,
    maxVisibleCols: Int = 15,
    dynamicFilteringMode: UpSetDynamicFilteringMode = UpSetDynamicFilteringMode.OFF,
    dotColorActive: Color = AppColors.primary,
    dotColorInactive: Color = AppColors.border.copy(alpha = 0.2f),
    lineColor: Color = AppColors.primary,
    onIntersectionClick: (AeonUpSetIntersection) -> Unit = {},
    onSetClick: (Int) -> Unit = {}
) {
    if (data.sets.isEmpty()) return

    val rowHeight = 24.dp
    val colWidth = 24.dp
    val leftPanelWidth = 80.dp
    val rightPanelWidth = 80.dp

    val horizontalScrollState = rememberScrollState()
    val verticalScrollState = rememberScrollState()

    val horizontalScrollEnabled = dynamicFilteringMode == UpSetDynamicFilteringMode.OFF || 
                                 dynamicFilteringMode == UpSetDynamicFilteringMode.HORIZONTAL
    val verticalScrollEnabled = dynamicFilteringMode == UpSetDynamicFilteringMode.OFF || 
                               dynamicFilteringMode == UpSetDynamicFilteringMode.VERTICAL

    val density = androidx.compose.ui.platform.LocalDensity.current
    val colWidthPx = with(density) { colWidth.toPx() }
    val rowHeightPx = with(density) { rowHeight.toPx() }
    val viewportWidthPx = with(density) { (colWidth * maxVisibleCols).toPx() }
    val viewportHeightPx = with(density) { (rowHeight * maxVisibleRows).toPx() }

    val activeRowIndices = remember(horizontalScrollState.value, data.intersections, dynamicFilteringMode) {
        if (dynamicFilteringMode != UpSetDynamicFilteringMode.HORIZONTAL) {
            data.sets.indices.toSet()
        } else {
            val firstVisible = (horizontalScrollState.value / colWidthPx).toInt()
            val lastVisible = ((horizontalScrollState.value + viewportWidthPx) / colWidthPx).toInt()
            
            val visibleIntersections = data.intersections.subList(
                firstVisible.coerceIn(0, data.intersections.size),
                (lastVisible + 1).coerceIn(0, data.intersections.size)
            )
            
            visibleIntersections.flatMap { it.setIndices }.toSet()
        }
    }

    val activeColIndices = remember(verticalScrollState.value, data.sets, dynamicFilteringMode) {
        if (dynamicFilteringMode != UpSetDynamicFilteringMode.VERTICAL) {
            data.intersections.indices.toSet()
        } else {
            val firstVisible = (verticalScrollState.value / rowHeightPx).toInt()
            val lastVisible = ((verticalScrollState.value + viewportHeightPx) / rowHeightPx).toInt()
            
            val visibleTagIndices = data.sets.indices.toList().subList(
                firstVisible.coerceIn(0, data.sets.size),
                (lastVisible + 1).coerceIn(0, data.sets.size)
            ).toSet()

            data.intersections.withIndex().filter { (_, intersection) ->
                intersection.setIndices.any { visibleTagIndices.contains(it) }
            }.map { it.index }.toSet()
        }
    }

    val renderedSets = remember(activeRowIndices, data.sets) {
        data.sets.filterIndexed { index, _ -> activeRowIndices.contains(index) }
    }

    val renderedIntersections = remember(activeColIndices, data.intersections) {
        data.intersections.filterIndexed { index, _ -> activeColIndices.contains(index) }
    }

    val setNames = renderedSets.map { it.name }
    val maxSetValue = data.sets.maxOfOrNull { it.total.abs() } ?: BigDecimal.ONE
    val maxIntersectionValue = data.intersections.maxOfOrNull { it.value.abs() } ?: BigDecimal.ONE

    val viewportHeight = rowHeight * minOf(setNames.size, maxVisibleRows)
    val viewportWidth = colWidth * minOf(renderedIntersections.size, maxVisibleCols)

    // Map original indices to rendered indices
    val originalToRenderedIndex = remember(activeRowIndices) {
        activeRowIndices.sorted().withIndex().associate { it.value to it.index }
    }
    val originalToRenderedColIndex = remember(activeColIndices) {
        activeColIndices.sorted().withIndex().associate { it.value to it.index }
    }

    Column(modifier = modifier) {
        // Top row: Empty | Intersection Bars | Empty
        Row(modifier = Modifier.fillMaxWidth()) {
            Box(modifier = Modifier.width(leftPanelWidth))
            
            // Intersection Size Bars (Horizontal Scroll)
            Box(modifier = Modifier.width(viewportWidth).horizontalScroll(horizontalScrollState, enabled = horizontalScrollEnabled)) {
                Row(
                    modifier = Modifier.width(colWidth * renderedIntersections.size).height(60.dp).padding(bottom = 2.dp),
                    horizontalArrangement = Arrangement.Start,
                    verticalAlignment = Alignment.Bottom
                ) {
                    renderedIntersections.forEach { intersection ->
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
        Row(modifier = Modifier.fillMaxWidth().height(viewportHeight)) {
            // Left Panel: Set names (Vertical Scroll)
            Column(
                modifier = Modifier
                    .width(leftPanelWidth)
                    .verticalScroll(verticalScrollState, enabled = verticalScrollEnabled)
            ) {
                activeRowIndices.sorted().forEach { originalIdx ->
                    val name = data.sets[originalIdx].name
                    Box(
                        modifier = Modifier
                            .height(rowHeight)
                            .fillMaxWidth()
                            .clickable { onSetClick(originalIdx) },
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
                    .width(viewportWidth)
                    .verticalScroll(verticalScrollState, enabled = verticalScrollEnabled)
                    .horizontalScroll(horizontalScrollState, enabled = horizontalScrollEnabled)
            ) {
                Canvas(
                    modifier = Modifier
                        .width(colWidth * renderedIntersections.size)
                        .height(rowHeight * setNames.size)
                ) {
                    val dx = colWidth.toPx()
                    val dy = rowHeight.toPx()
                    val radius = 5.dp.toPx()

                    renderedIntersections.forEachIndexed { colIdx, intersection ->
                        val x = dx * (colIdx + 0.5f)
                        
                        if (intersection.setIndices.size > 1) {
                            val renderedIndices = intersection.setIndices.mapNotNull { originalToRenderedIndex[it] }.sorted()
                            if (renderedIndices.size > 1) {
                                val minY = dy * (renderedIndices.first() + 0.5f)
                                val maxY = dy * (renderedIndices.last() + 0.5f)
                                drawLine(
                                    color = lineColor,
                                    start = Offset(x, minY),
                                    end = Offset(x, maxY),
                                    strokeWidth = 2.dp.toPx()
                                )
                            }
                        }

                        for (originalRowIdx in activeRowIndices.sorted()) {
                            val renderedRowIdx = originalToRenderedIndex[originalRowIdx] ?: continue
                            val y = dy * (renderedRowIdx + 0.5f)
                            val isActive = intersection.setIndices.contains(originalRowIdx)
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
                    renderedIntersections.forEach { intersection ->
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
                    .verticalScroll(verticalScrollState, enabled = verticalScrollEnabled)
            ) {
                activeRowIndices.sorted().forEach { originalRowIdx ->
                    val set = data.sets[originalRowIdx]
                    val ratio = if (maxSetValue > BigDecimal.ZERO) {
                        set.total.abs().divide(maxSetValue, 4, RoundingMode.HALF_UP).toFloat()
                    } else 0f
                    
                    Box(
                        modifier = Modifier
                            .height(rowHeight)
                            .fillMaxWidth()
                            .clickable { onSetClick(originalRowIdx) }
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
