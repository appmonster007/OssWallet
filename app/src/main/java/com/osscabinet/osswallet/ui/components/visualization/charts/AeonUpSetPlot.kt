package com.osscabinet.ossWallet.ui.components.visualization.charts

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.layout
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.osscabinet.ossWallet.domain.model.settings.UpSetDynamicFilteringMode
import com.osscabinet.ossWallet.domain.service.currency.CurrencyFormatter
import com.osscabinet.ossWallet.ui.theme.AppColors
import com.osscabinet.ossWallet.ui.theme.AppTextStyles
import com.osscabinet.ossWallet.ui.theme.ChartColorGreen
import com.osscabinet.ossWallet.ui.theme.OssWalletTheme
import java.math.BigDecimal
import java.math.RoundingMode

@Composable
private fun UpSetHorizontalBar(
    name: String,
    amount: String,
    magnitudeRatio: Float,
    isPositive: Boolean,
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
        val barColor = if (isPositive) ChartColorGreen else AppColors.error

        // Horizontal Bar
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight()
        ) {
            // Background track for the whole row
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .clip(RoundedCornerShape(4.dp))
                    .background(AppColors.primary.copy(alpha = 0.03f))
            )

            // Draw bar segment - grows Left -> Right for +ve, Right -> Left for -ve
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .fillMaxWidth(magnitudeRatio.coerceAtLeast(0.01f))
                    .align(if (isPositive) Alignment.CenterStart else Alignment.CenterEnd)
                    .clip(RoundedCornerShape(4.dp))
                    .background(barColor.copy(alpha = 0.25f))
            )
        }

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
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = amount,
                style = AppTextStyles.caption.copy(
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Bold,
                    fontFeatureSettings = "tnum"
                ),
                color = AppColors.contentPrimary,
                maxLines = 1
            )
        }
    }
}

@Composable
private fun UpSetVerticalBar(
    amount: String,
    magnitudeRatio: Float,
    isPositive: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxHeight()
            .clickable(onClick = onClick),
        contentAlignment = Alignment.TopCenter
    ) {
        val barColor = if (isPositive) ChartColorGreen else AppColors.error

        // Track & Bar
        Box(modifier = Modifier
            .fillMaxHeight()
            .width(18.dp)) {
            // Background track
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .clip(RoundedCornerShape(4.dp))
                    .background(AppColors.primary.copy(alpha = 0.05f))
            )

            // Bar segment - grows Bottom -> Top for +ve, Top -> Bottom for -ve
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .fillMaxHeight(magnitudeRatio.coerceAtLeast(0.01f))
                    .align(if (isPositive) Alignment.BottomCenter else Alignment.TopCenter)
                    .clip(RoundedCornerShape(2.dp))
                    .background(barColor.copy(alpha = 0.5f))
            )
        }

        // Amount label - always at the bottom as per original design
        Box(
            modifier = Modifier
                .padding(bottom = 12.dp)
                .width(24.dp)
                .fillMaxHeight()
                .wrapContentHeight(Alignment.Bottom)
        ) {
            Text(
                text = amount,
                style = AppTextStyles.caption.copy(
                    fontSize = 8.5.sp,
                    fontWeight = FontWeight.ExtraBold,
                    fontFeatureSettings = "tnum"
                ),
                color = AppColors.contentPrimary,
                maxLines = 1,
                overflow = TextOverflow.Visible,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .graphicsLayer(rotationZ = -90f)
                    .layout { measurable, constraints ->
                        val placeable =
                            measurable.measure(constraints.copy(maxWidth = Constraints.Infinity))
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

    val density = androidx.compose.ui.platform.LocalDensity.current
    val colWidthPx = with(density) { colWidth.toPx() }
    val rowHeightPx = with(density) { rowHeight.toPx() }

    val viewportWidth = colWidth * maxVisibleCols
    val viewportHeight = rowHeight * maxVisibleRows
    val viewportWidthPx = with(density) { viewportWidth.toPx() }
    val viewportHeightPx = with(density) { viewportHeight.toPx() }

    val activeRowIndices =
        remember(horizontalScrollState.value, data.intersections, dynamicFilteringMode) {
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

    // Magnitude scaling logic for bars (simplified, edge-based baselines)
    val maxIntMagnitude = data.intersections.maxOfOrNull { it.value.abs() } ?: BigDecimal.ONE
    val maxSetMagnitude = data.sets.maxOfOrNull { it.total.abs() } ?: BigDecimal.ONE

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
                modifier = Modifier
                    .width(finalViewportWidth)
                    .horizontalScroll(horizontalScrollState, enabled = true)
            ) {
                Row(
                    modifier = Modifier
                        .width(colWidth * renderedIntersections.size + gutterWidth)
                        .height(170.dp),
                    horizontalArrangement = Arrangement.Start,
                    verticalAlignment = Alignment.Bottom
                ) {
                    Spacer(modifier = Modifier.width(gutterWidth))
                    renderedIntersections.forEach { intersection ->
                        val magnitudeRatio = intersection.value.abs().divide(
                            maxIntMagnitude.coerceAtLeast(BigDecimal.ONE),
                            4,
                            RoundingMode.HALF_UP
                        ).toFloat()
                        val isPositive = intersection.value >= BigDecimal.ZERO

                        UpSetVerticalBar(
                            amount = CurrencyFormatter.formatAuto(
                                intersection.value,
                                currency,
                                shorten = true
                            ),
                            magnitudeRatio = magnitudeRatio,
                            isPositive = isPositive,
                            onClick = { onIntersectionClick(intersection) },
                            modifier = Modifier.width(colWidth)
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Main content: Set names + Bars | Dot Matrix
        Row(modifier = Modifier
            .fillMaxWidth()
            .height(finalViewportHeight)) {
            // Left Panel: Set names, Amounts + Bars (Vertical Scroll)
            Column(
                modifier = Modifier
                    .width(leftPanelWidth)
                    .verticalScroll(verticalScrollState, enabled = true)
            ) {
                activeRowIndices.sorted().forEach { originalIdx ->
                    val set = data.sets[originalIdx]
                    val magnitudeRatio = set.total.abs().divide(
                        maxSetMagnitude.coerceAtLeast(BigDecimal.ONE),
                        4,
                        RoundingMode.HALF_UP
                    ).toFloat()
                    val isPositive = set.total >= BigDecimal.ZERO

                    UpSetHorizontalBar(
                        name = set.name,
                        amount = CurrencyFormatter.formatAuto(set.total, currency, shorten = true),
                        magnitudeRatio = magnitudeRatio,
                        isPositive = isPositive,
                        onClick = { onSetClick(originalIdx) },
                        modifier = Modifier.height(rowHeight)
                    )
                }
            }

            // Center: Dot Matrix (Dual Scroll)
            Box(
                modifier = Modifier
                    .width(finalViewportWidth)
                    .verticalScroll(verticalScrollState, enabled = true)
                    .horizontalScroll(horizontalScrollState, enabled = true)
            ) {
                Canvas(
                    modifier = Modifier
                        .width(colWidth * renderedIntersections.size + gutterWidth)
                        .height(rowHeight * setNames.size)
                ) {
                    val dx = colWidth.toPx()
                    val dy = rowHeight.toPx()
                    val radius = 4.dp.toPx()
                    val offsetX = gutterWidth.toPx()

                    renderedIntersections.forEachIndexed { colIdx, intersection ->
                        val x = offsetX + dx * (colIdx + 0.5f)

                        if (intersection.setIndices.size > 1) {
                            val renderedIndices =
                                intersection.setIndices.mapNotNull { originalToRenderedIndex[it] }
                                    .sorted()
                            if (renderedIndices.size > 1) {
                                val minY = dy * (renderedIndices.first() + 0.5f)
                                val maxY = dy * (renderedIndices.last() + 0.5f)
                                drawLine(
                                    color = lineColor.copy(alpha = 0.8f),
                                    start = Offset(x, minY),
                                    end = Offset(x, maxY),
                                    strokeWidth = 3.5.dp.toPx()
                                )
                            }
                        }

                        for (originalRowIdx in activeRowIndices.sorted()) {
                            val renderedRowIdx = originalToRenderedIndex[originalRowIdx] ?: continue
                            val y = dy * (renderedRowIdx + 0.5f)
                            val isActive = intersection.setIndices.contains(originalRowIdx)

                            if (isActive) {
                                drawCircle(
                                    color = dotColorActive,
                                    radius = radius * 1.2f,
                                    center = Offset(x, y),
                                    style = Fill
                                )
                            } else {
                                drawCircle(
                                    color = dotColorInactive,
                                    radius = radius * 0.8f,
                                    center = Offset(x, y),
                                    style = Fill
                                )
                            }
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

@Preview(showBackground = true, name = "Mixed Data UpSet", widthDp = 300)
@Composable
fun AeonUpSetPlotPreview() {
    val sets = listOf(
        AeonUpSetSet("Salary", BigDecimal("3500")),
        AeonUpSetSet("Groceries", BigDecimal("-800")),
        AeonUpSetSet("Business", BigDecimal("1200")),
        AeonUpSetSet("Leisure", BigDecimal("-400")),
        AeonUpSetSet("Tax", BigDecimal("-900"))
    )
    val intersections = listOf(
        AeonUpSetIntersection(setOf(0), BigDecimal("3000"), count = 1),
        AeonUpSetIntersection(setOf(1, 3), BigDecimal("-250"), count = 4),
        AeonUpSetIntersection(setOf(2), BigDecimal("1500"), count = 2),
        AeonUpSetIntersection(setOf(0, 2), BigDecimal("500"), count = 1),
        AeonUpSetIntersection(setOf(1, 4), BigDecimal("-1000"), count = 3),
        AeonUpSetIntersection(setOf(3), BigDecimal("-150"), count = 2)
    )
    OssWalletTheme {
        Surface(modifier = Modifier.padding(16.dp)) {
            AeonUpSetPlot(
                data = AeonUpSetData(sets, intersections),
                currency = "USD"
            )
        }
    }
}
