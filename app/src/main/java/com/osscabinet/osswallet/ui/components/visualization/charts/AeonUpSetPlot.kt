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
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.layout
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.osscabinet.ossWallet.domain.model.settings.UpSetDynamicFilteringMode
import com.osscabinet.ossWallet.domain.service.currency.CurrencyFormatter
import com.osscabinet.ossWallet.ui.theme.AeonTheme
import com.osscabinet.ossWallet.ui.theme.AppColors
import com.osscabinet.ossWallet.ui.theme.AppTextStyles
import java.math.BigDecimal
import java.math.RoundingMode

@Composable
private fun UpSetHorizontalBar(
    name: String,
    amount: String,
    ratio: Float,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 4.dp, vertical = 2.dp),
        contentAlignment = Alignment.CenterStart
    ) {
        // Horizontal Bar as background
        Box(
            modifier = Modifier
                .fillMaxWidth(ratio.coerceAtLeast(0.01f))
                .fillMaxHeight()
                .clip(RoundedCornerShape(6.dp))
                .background(AppColors.accent.copy(alpha = 0.2f))
        )
        
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = name,
                style = AppTextStyles.caption.copy(fontSize = 10.sp, fontWeight = FontWeight.Bold),
                color = AppColors.contentPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = amount,
                style = AppTextStyles.caption.copy(fontSize = 9.sp, fontFeatureSettings = "tnum"),
                color = AppColors.contentSecondary,
                maxLines = 1
            )
        }
    }
}

@Composable
private fun UpSetVerticalBar(
    amount: String,
    ratio: Float,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxHeight()
            .clickable(onClick = onClick),
        contentAlignment = Alignment.BottomCenter
    ) {
        // Track (Background)
        Box(
            modifier = Modifier
                .fillMaxHeight()
                .width(16.dp)
                .clip(RoundedCornerShape(topStart = 4.dp, topEnd = 4.dp))
                .background(AppColors.primary.copy(alpha = 0.05f))
        )

        // Bar (Value)
        Box(
            modifier = Modifier
                .width(16.dp)
                .fillMaxHeight(ratio.coerceAtLeast(0.01f))
                .clip(RoundedCornerShape(topStart = 4.dp, topEnd = 4.dp))
                .background(AppColors.primary.copy(alpha = 0.2f))
        )
        
        // Amount (Overlay) - Correctly aligned to baseline
        Box(
            modifier = Modifier
                .padding(bottom = 12.dp)
                .width(24.dp) // Match column width
                .wrapContentHeight(Alignment.Bottom)
        ) {
            Text(
                text = amount,
                style = AppTextStyles.caption.copy(
                    fontSize = 8.sp, 
                    fontWeight = FontWeight.Bold,
                    fontFeatureSettings = "tnum"
                ),
                color = AppColors.contentSecondary,
                maxLines = 1,
                overflow = TextOverflow.Visible,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .graphicsLayer(rotationZ = -90f)
                    .layout { measurable, constraints ->
                        val placeable = measurable.measure(constraints.copy(maxWidth = Constraints.Infinity))
                        // Swap dimensions for the layout engine
                        layout(placeable.height, placeable.width) {
                            placeable.place(
                                x = -(placeable.width / 2 - placeable.height / 2),
                                y = -(placeable.height / 2 - placeable.width / 2)
                            )
                        }
                    }
            )
        }
    }
}

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
    val leftPanelWidth = 120.dp
    val gutterWidth = 8.dp // Space between left panel and plot content

    val horizontalScrollState = rememberScrollState()
    val verticalScrollState = rememberScrollState()

    LaunchedEffect(dynamicFilteringMode) {
        if (dynamicFilteringMode == UpSetDynamicFilteringMode.HORIZONTAL) {
            verticalScrollState.scrollTo(0)
        } else if (dynamicFilteringMode == UpSetDynamicFilteringMode.VERTICAL) {
            horizontalScrollState.scrollTo(0)
        }
    }

    val horizontalScrollEnabled = dynamicFilteringMode == UpSetDynamicFilteringMode.OFF || 
                                 dynamicFilteringMode == UpSetDynamicFilteringMode.HORIZONTAL
    val verticalScrollEnabled = dynamicFilteringMode == UpSetDynamicFilteringMode.OFF || 
                               dynamicFilteringMode == UpSetDynamicFilteringMode.VERTICAL

    val density = androidx.compose.ui.platform.LocalDensity.current
    val colWidthPx = with(density) { colWidth.toPx() }
    val rowHeightPx = with(density) { rowHeight.toPx() }

    val viewportWidth = colWidth * maxVisibleCols
    val viewportHeight = rowHeight * maxVisibleRows
    val viewportWidthPx = with(density) { viewportWidth.toPx() }
    val viewportHeightPx = with(density) { viewportHeight.toPx() }

    val activeRowIndices = remember(horizontalScrollState.value, data.intersections, dynamicFilteringMode) {
        if (dynamicFilteringMode != UpSetDynamicFilteringMode.HORIZONTAL) {
            data.sets.indices.toSet()
        } else {
            val hValue = horizontalScrollState.value
            val firstVisible = (hValue / colWidthPx).toInt()
            val lastVisible = ((hValue + viewportWidthPx) / colWidthPx).toInt()
            
            val visibleIntersections = data.intersections.subList(
                firstVisible.coerceIn(0, data.intersections.size),
                (lastVisible + 1).coerceIn(0, data.intersections.size)
            )
            
            // Maintain original index order for smooth scrolling
            visibleIntersections.flatMap { it.setIndices }.toSet().sorted().toSet()
        }
    }

    val activeColIndices = remember(verticalScrollState.value, data.sets, dynamicFilteringMode) {
        if (dynamicFilteringMode != UpSetDynamicFilteringMode.VERTICAL) {
            data.intersections.indices.toSet()
        } else {
            val vValue = verticalScrollState.value
            val firstVisible = (vValue / rowHeightPx).toInt()
            val lastVisible = ((vValue + viewportHeightPx) / rowHeightPx).toInt()
            
            val visibleTagIndices = data.sets.indices.toList().subList(
                firstVisible.coerceIn(0, data.sets.size),
                (lastVisible + 1).coerceIn(0, data.sets.size)
            ).toSet()

            // Maintain original order (by value) for smooth scrolling
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

    val finalViewportHeight = rowHeight * minOf(setNames.size, maxVisibleRows)
    val finalViewportWidth = colWidth * minOf(renderedIntersections.size, maxVisibleCols)

    // Map original indices to rendered indices
    val originalToRenderedIndex = remember(activeRowIndices) {
        activeRowIndices.sorted().withIndex().associate { it.value to it.index }
    }

    Column(modifier = modifier) {
        // Top row: Empty | Intersection Bars
        Row(modifier = Modifier.fillMaxWidth()) {
            Box(modifier = Modifier.width(leftPanelWidth))
            
            // Intersection Size Bars (Horizontal Scroll)
            Box(
                modifier = Modifier.width(finalViewportWidth)
                    .horizontalScroll(horizontalScrollState, enabled = horizontalScrollEnabled)
            ) {
                Row(
                    modifier = Modifier
                        .width(colWidth * renderedIntersections.size + gutterWidth)
                        .height(120.dp),
                    horizontalArrangement = Arrangement.Start,
                    verticalAlignment = Alignment.Bottom
                ) {
                    Spacer(modifier = Modifier.width(gutterWidth))
                    renderedIntersections.forEach { intersection ->
                        val ratio = if (maxIntersectionValue > BigDecimal.ZERO) {
                            intersection.value.abs().divide(maxIntersectionValue, 4, RoundingMode.HALF_UP).toFloat()
                        } else 0f
                        
                        UpSetVerticalBar(
                            amount = CurrencyFormatter.formatShortCurrency(intersection.value, currency),
                            ratio = ratio,
                            onClick = { onIntersectionClick(intersection) },
                            modifier = Modifier.width(colWidth)
                        )
                    }
                }
            }
        }

        // Main content: Set names + Bars | Dot Matrix
        Row(modifier = Modifier.fillMaxWidth().height(finalViewportHeight)) {
            // Left Panel: Set names, Amounts + Bars (Vertical Scroll)
            Column(
                modifier = Modifier
                    .width(leftPanelWidth)
                    .verticalScroll(verticalScrollState, enabled = verticalScrollEnabled)
            ) {
                activeRowIndices.sorted().forEach { originalIdx ->
                    val set = data.sets[originalIdx]
                    val ratio = if (maxSetValue > BigDecimal.ZERO) {
                        set.total.abs().divide(maxSetValue, 4, RoundingMode.HALF_UP).toFloat()
                    } else 0f
                    
                    UpSetHorizontalBar(
                        name = set.name,
                        amount = CurrencyFormatter.formatShortCurrency(set.total, currency),
                        ratio = ratio,
                        onClick = { onSetClick(originalIdx) },
                        modifier = Modifier.height(rowHeight)
                    )
                }
            }

            // Center: Dot Matrix (Dual Scroll)
            Box(
                modifier = Modifier
                    .width(finalViewportWidth)
                    .verticalScroll(verticalScrollState, enabled = verticalScrollEnabled)
                    .horizontalScroll(horizontalScrollState, enabled = horizontalScrollEnabled)
            ) {
                Canvas(
                    modifier = Modifier
                        .width(colWidth * renderedIntersections.size + gutterWidth)
                        .height(rowHeight * setNames.size)
                ) {
                    val dx = colWidth.toPx()
                    val dy = rowHeight.toPx()
                    val radius = 5.dp.toPx()
                    val offsetX = gutterWidth.toPx()

                    renderedIntersections.forEachIndexed { colIdx, intersection ->
                        val x = offsetX + dx * (colIdx + 0.5f)
                        
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
                    Spacer(modifier = Modifier.width(gutterWidth))
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
        }
    }
}


