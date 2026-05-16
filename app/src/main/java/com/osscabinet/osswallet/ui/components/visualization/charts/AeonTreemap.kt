package com.osscabinet.ossWallet.ui.components.visualization.charts

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.KeyboardDoubleArrowRight
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.foundation.Image
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.filled.AddCircle
import androidx.compose.material.icons.filled.ZoomIn
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.unit.sp
import com.osscabinet.ossWallet.ui.theme.*
import com.osscabinet.ossWallet.domain.service.currency.CurrencyFormatter.formatCurrencyRight
import com.osscabinet.ossWallet.ui.util.generateShade
import java.math.BigDecimal
import java.math.RoundingMode

/**
 * High-fidelity treemap visualization for categorical distribution.
 */
@Composable
fun AeonTreemap(
    nodes: List<AeonTreemapNode>,
    modifier: Modifier = Modifier,
    currency: String = "USD",
    precision: Int = 2,
    showLabels: Boolean = true,
    rounded: Boolean = true,
    onNodeClick: (AeonTreemapNode) -> Unit = {},
    onDrillInClick: (AeonTreemapNode) -> Unit = {},
    onNavigateToRecords: (AeonTreemapNode) -> Unit = {}
) {
    if (nodes.isEmpty()) {
        Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("No data available", style = MaterialTheme.typography.bodySmall)
        }
        return
    }

    var selectedNode by remember { mutableStateOf<AeonTreemapNode?>(null) }
    var touchPoint by remember { mutableStateOf<Offset?>(null) }
    val density = LocalDensity.current
    val haptic = LocalHapticFeedback.current
    val disclosurePainter = rememberVectorPainter(Icons.Default.AddCircle)

    val totalValue = nodes.sumOf { it.value.abs() }
    val dims = AeonTheme.dimensions

    BoxWithConstraints(modifier = modifier) {
        val widthPx = with(density) { maxWidth.toPx() }
        val heightPx = with(density) { maxHeight.toPx() }
        
        val layouts = remember(nodes, widthPx, heightPx) {
            computeTreemapLayout(nodes, Rect(0f, 0f, widthPx, heightPx), totalValue)
        }

        fun findNodeAt(offset: Offset): AeonTreemapNode? {
            return layouts.find { it.second.contains(offset) }?.first
        }

        Box(modifier = Modifier.fillMaxSize()) {
            Canvas(
                modifier = Modifier
                    .fillMaxSize()
                    .semantics {
                        contentDescription = "Treemap showing distribution. Drag or tap boxes for details."
                    }
                    .pointerInput(nodes) {
                        detectDragGestures(
                            onDragStart = { offset ->
                                touchPoint = offset
                                selectedNode = findNodeAt(offset)
                            },
                            onDrag = { change, _ ->
                                touchPoint = change.position
                                selectedNode = findNodeAt(change.position)
                            },
                            onDragEnd = {
                                // Keep selection for tooltip
                            },
                            onDragCancel = {
                                selectedNode = null
                                touchPoint = null
                            }
                        )
                    }
                    .pointerInput(nodes) {
                        detectTapGestures(
                            onTap = { offset ->
                                val node = findNodeAt(offset)
                                if (node != null) {
                                    if (node.hasSubcategories) {
                                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                    } else {
                                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                    }
                                    onNodeClick(node)
                                }
                                selectedNode = node
                                touchPoint = offset
                            },
                            onLongPress = {
                                // Consumes to prevent reordering during interaction
                            }
                        )
                    }
            ) {
                // 1. Draw Ambient Shadow (Background depth for the whole treemap)
                drawRoundRect(
                    color = ChartOverlayDark,
                    topLeft = Offset(-(dims.spacingXs / 2).toPx(), -(dims.spacingXs / 2).toPx()),
                    size = Size(widthPx + dims.spacingXs.toPx(), heightPx + dims.spacingXs.toPx()),
                    cornerRadius = CornerRadius(dims.spacingSm.toPx(), dims.spacingSm.toPx())
                )

                layouts.forEach { (node, rect) ->
                    val isSelected = selectedNode == node
                    val radius = if (rounded) (dims.spacingXs * 1.5f).toPx() else 0f
                    val cornerRadius = CornerRadius(radius, radius)
                    
                    // Modern Look: Precise radial gaps (spacing between rectangles)
                    val gap = if (rounded) (dims.spacingXs / 2).toPx() else 0f
                    val drawRect = Rect(
                        rect.left + gap / 2,
                        rect.top + gap / 2,
                        rect.right - gap / 2,
                        rect.bottom - gap / 2
                    )
                    
                    if (drawRect.width <= 0 || drawRect.height <= 0) return@forEach

                    // 2. Base Segment Color
                    drawRoundRect(
                        color = node.color,
                        topLeft = Offset(drawRect.left, drawRect.top),
                        size = Size(drawRect.width, drawRect.height),
                        cornerRadius = cornerRadius
                    )
                    
                    // 3. High-Fidelity Layer: Inner Bevel/Glass Highlight
                    if (drawRect.width > dims.containerCornerRadius.toPx() && drawRect.height > dims.containerCornerRadius.toPx()) {
                        val innerRadius = if (rounded) (dims.spacingXs * 1.25f).toPx() else 0f
                        drawRoundRect(
                            color = ChartGlossHighlight,
                            topLeft = Offset(drawRect.left + (dims.spacingXs / 4).toPx(), drawRect.top + (dims.spacingXs / 4).toPx()),
                            size = Size(drawRect.width - (dims.spacingXs / 2).toPx(), drawRect.height - (dims.spacingXs / 2).toPx()),
                            cornerRadius = CornerRadius(innerRadius, innerRadius),
                            style = androidx.compose.ui.graphics.drawscope.Stroke(width = (dims.spacingXs / 4).toPx())
                        )
                        
                        // Subtle Top Glow
                        drawRoundRect(
                            brush = Brush.verticalGradient(
                                colors = listOf(ChartGlossGradEnd, ColorTransparent),
                                startY = drawRect.top,
                                endY = drawRect.top + drawRect.height * 0.4f
                            ),
                            topLeft = Offset(drawRect.left, drawRect.top),
                            size = Size(drawRect.width, drawRect.height),
                            cornerRadius = cornerRadius
                        )
                    }
                    
                    // 4. Selection Highlight
                    if (isSelected) {
                        drawRoundRect(
                            color = ChartRimHighlight,
                            topLeft = Offset(drawRect.left, drawRect.top),
                            size = Size(drawRect.width, drawRect.height),
                            cornerRadius = cornerRadius,
                            style = androidx.compose.ui.graphics.drawscope.Stroke(width = (dims.spacingXs * 0.75f).toPx())
                        )
                    }
                }
            }

            // Dismissal layer for Tooltip
            if (selectedNode != null) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .pointerInput(Unit) {
                            detectTapGestures {
                                selectedNode = null
                                touchPoint = null
                            }
                        }
                )
            }

            // Overlay labels for larger segments
            if (showLabels) {
                layouts.forEach { (node, rect) ->
                    val gap = with(density) { if (rounded) (dims.spacingXs / 2).toPx() else 0f }
                    val drawRect = Rect(rect.left + gap/2, rect.top + gap/2, rect.right - gap/2, rect.bottom - gap/2)
                    val rectWidthDp = with(density) { drawRect.width.toDp() }
                    val rectHeightDp = with(density) { drawRect.height.toDp() }
                    
                    // Show labels if box is reasonably large
                    val showText = rectWidthDp > dims.spacing3xl && rectHeightDp > dims.buttonCornerRadius
                    // Show only subcategory indicator if box is too small for text but large enough for icon
                    val showOnlyIcon = !showText && node.hasSubcategories && rectWidthDp > dims.spacingLg && rectHeightDp > dims.spacingLg

                    if (showText || showOnlyIcon) {
                        val iconTint = if (node.color.luminance() > 0.5f) ColorOnLight.copy(alpha = 0.6f) else ColorOnDark.copy(alpha = 0.7f)
                        val labelColor = if (node.color.luminance() > 0.5f) ColorOnLight else ColorOnDark

                        Box(
                            modifier = Modifier
                                .offset(x = with(density) { drawRect.left.toDp() }, y = with(density) { drawRect.top.toDp() })
                                .size(rectWidthDp, rectHeightDp)
                                .padding(horizontal = dims.spacingSm, vertical = dims.spacingXs)
                                .clipToBounds(),
                            contentAlignment = if (showText) Alignment.TopStart else Alignment.Center
                        ) {
                            if (showText) {
                                Column(horizontalAlignment = Alignment.Start) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(
                                            node.name,
                                            style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp, fontWeight = FontWeight.Bold),
                                            color = labelColor,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis,
                                            modifier = Modifier.weight(1f, fill = false)
                                        )
                                        if (node.hasSubcategories) {
                                            Image(
                                                painter = disclosurePainter,
                                                contentDescription = null,
                                                modifier = Modifier.size(dims.iconSizeXs).padding(start = (dims.spacingXs / 2)),
                                                colorFilter = ColorFilter.tint(iconTint)
                                            )
                                        }
                                    }
                                    Text(
                                        formatCurrencyRight(node.value, currency, precision),
                                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Medium, fontSize = 9.sp),
                                        color = labelColor.copy(alpha = 0.85f),
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            } else {
                                Image(
                                    painter = disclosurePainter,
                                    contentDescription = null,
                                    modifier = Modifier.size(dims.iconSizeXs),
                                    colorFilter = ColorFilter.tint(iconTint)
                                )
                            }
                        }
                    }
                }
            }

            // Tooltip Overlay
            selectedNode?.let { node ->
                touchPoint?.let { point ->
                    AeonChartInteractionPopup(
                        name = node.name,
                        value = node.value,
                        color = node.color,
                        percentageOfTotal = node.percentageOfTotal,
                        hasSubcategories = node.hasSubcategories,
                        subcategoryCount = node.subcategoryCount,
                        touchPoint = point,
                        chartWidthPx = widthPx,
                        chartHeightPx = heightPx,
                        density = density.density,
                        currency = currency,
                        precision = precision,
                        onDrillInClick = { 
                            selectedNode = null
                            onDrillInClick(node) 
                        },
                        onNavigateToRecords = { 
                            selectedNode = null
                            onNavigateToRecords(node) 
                        }
                    )
                }
            }
        }
    }
}

/**
 * Squarified Treemap algorithm implementation.
 * Maintains aspect ratios as close to 1:1 as possible.
 */
private fun computeTreemapLayout(
    nodes: List<AeonTreemapNode>,
    bounds: Rect,
    totalValue: BigDecimal
): List<Pair<AeonTreemapNode, Rect>> {
    if (nodes.isEmpty() || totalValue.signum() == 0) return emptyList()
    
    val result = mutableListOf<Pair<AeonTreemapNode, Rect>>()
    val sortedNodes = nodes.sortedByDescending { it.value.abs() }
    
    squarify(sortedNodes, emptyList(), bounds, totalValue, result)
    
    return result
}

private fun squarify(
    remaining: List<AeonTreemapNode>,
    currentRow: List<AeonTreemapNode>,
    bounds: Rect,
    total: BigDecimal,
    result: MutableList<Pair<AeonTreemapNode, Rect>>
) {
    if (remaining.isEmpty()) {
        if (currentRow.isNotEmpty()) {
            layoutRow(currentRow, bounds, total, result)
        }
        return
    }

    val nextNode = remaining.first()
    val newRow = currentRow + nextNode
    
    if (currentRow.isEmpty() || worstAspectRatio(currentRow, bounds, total) >= worstAspectRatio(newRow, bounds, total)) {
        squarify(remaining.drop(1), newRow, bounds, total, result)
    } else {
        val newBounds = layoutRow(currentRow, bounds, total, result)
        val remainingTotal = total - currentRow.sumOf { it.value.abs() }
        squarify(remaining, emptyList(), newBounds, remainingTotal, result)
    }
}

private fun worstAspectRatio(row: List<AeonTreemapNode>, bounds: Rect, total: BigDecimal): Float {
    if (row.isEmpty()) return Float.MAX_VALUE
    
    val rowSum = row.sumOf { it.value.abs() }.toFloat()
    val totalFloat = total.abs().toFloat()
    val width = bounds.width
    val height = bounds.height
    val side = if (width > height) height else width
    
    val s = rowSum / totalFloat * (width * height)
    val minV = row.minOf { it.value.abs() }.toFloat() / totalFloat * (width * height)
    val maxV = row.maxOf { it.value.abs() }.toFloat() / totalFloat * (width * height)
    
    return maxOf(
        (side * side * maxV) / (s * s),
        (s * s) / (side * side * minV)
    )
}

private fun layoutRow(
    row: List<AeonTreemapNode>,
    bounds: Rect,
    total: BigDecimal,
    result: MutableList<Pair<AeonTreemapNode, Rect>>
): Rect {
    val rowSum = row.sumOf { it.value.abs() }.toFloat()
    val totalFloat = total.abs().toFloat()
    val rowAreaRatio = rowSum / totalFloat
    
    if (bounds.width >= bounds.height) {
        // Vertical strip
        val rowWidth = bounds.width * rowAreaRatio
        // IMPORTANT: Clip last row to fill bounds exactly
        val actualRowWidth = if (rowSum >= totalFloat * 0.99f) bounds.width else rowWidth
        
        var currentY = bounds.top
        row.forEachIndexed { index, node ->
            val nodeHeight = (node.value.abs().toFloat() / rowSum) * bounds.height
            val finalHeight = if (index == row.size - 1) bounds.bottom - currentY else nodeHeight
            result.add(node to Rect(bounds.left, currentY, bounds.left + actualRowWidth, currentY + finalHeight))
            currentY += finalHeight
        }
        return Rect(bounds.left + actualRowWidth, bounds.top, bounds.right, bounds.bottom)
    } else {
        // Horizontal strip
        val rowHeight = bounds.height * rowAreaRatio
        // IMPORTANT: Clip last row to fill bounds exactly
        val actualRowHeight = if (rowSum >= totalFloat * 0.99f) bounds.height else rowHeight
        
        var currentX = bounds.left
        row.forEachIndexed { index, node ->
            val nodeWidth = (node.value.abs().toFloat() / rowSum) * bounds.width
            val finalWidth = if (index == row.size - 1) bounds.right - currentX else nodeWidth
            result.add(node to Rect(currentX, bounds.top, currentX + finalWidth, bounds.top + actualRowHeight))
            currentX += finalWidth
        }
        return Rect(bounds.left, bounds.top + actualRowHeight, bounds.right, bounds.bottom)
    }
}

@androidx.compose.ui.tooling.preview.Preview(showBackground = true)
@Composable
fun AeonTreemapPreview() {
    OssWalletTheme {
        Box(modifier = Modifier.size(400.dp, 300.dp).padding(AeonTheme.dimensions.screenPadding)) {
            AeonTreemap(
                nodes = listOf(
                    AeonTreemapNode("Food", BigDecimal("500.00"), ChartColorOrange, emptyList(), emptyMap(), false, 0, 0f, null),
                    AeonTreemapNode("Rent", BigDecimal("1200.00"), TagPresetColors[5], emptyList(), emptyMap(), false, 0, 0f, null),
                    AeonTreemapNode("Transport", BigDecimal("300.00"), TagPresetColors[10], emptyList(), emptyMap(), false, 0, 0f, null),
                    AeonTreemapNode("Entertainment", BigDecimal("200.00"), TagPresetColors[2], emptyList(), emptyMap(), false, 0, 0f, null),
                    AeonTreemapNode("Utilities", BigDecimal("150.00"), TagPresetColors[13], emptyList(), emptyMap(), false, 0, 0f, null),
                    AeonTreemapNode("Health", BigDecimal("100.00"), TagPresetColors[1], emptyList(), emptyMap(), false, 0, 0f, null)
                )
            )
        }
    }
}
