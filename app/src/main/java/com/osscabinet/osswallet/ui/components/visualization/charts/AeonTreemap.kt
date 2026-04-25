package com.osscabinet.ossWallet.ui.components.visualization.charts

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.osscabinet.ossWallet.ui.theme.OssWalletTheme
import com.osscabinet.ossWallet.ui.util.formatCurrencyRight
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
    precision: Int = 2
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

    val totalValue = nodes.sumOf { it.value }

    BoxWithConstraints(modifier = modifier) {
        val widthPx = with(LocalDensity.current) { maxWidth.toPx() }
        val heightPx = with(LocalDensity.current) { maxHeight.toPx() }
        
        val layouts = remember(nodes, widthPx, heightPx) {
            computeTreemapLayout(nodes, Rect(0f, 0f, widthPx, heightPx), totalValue)
        }

        fun findNodeAt(offset: Offset): AeonTreemapNode? {
            return layouts.find { it.second.contains(offset) }?.first
        }

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
                            selectedNode = null
                            touchPoint = null
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
                            selectedNode = findNodeAt(offset)
                            touchPoint = offset
                        },
                        onLongPress = {
                            // Consumes to prevent reordering during interaction
                        }
                    )
                }
        ) {
            layouts.forEach { (node, rect) ->
                drawRect(
                    color = node.color,
                    topLeft = Offset(rect.left, rect.top),
                    size = androidx.compose.ui.geometry.Size(rect.width, rect.height)
                )
                
                // Draw border for segments
                drawRect(
                    color = Color.White.copy(alpha = 0.5f),
                    topLeft = Offset(rect.left, rect.top),
                    size = androidx.compose.ui.geometry.Size(rect.width, rect.height),
                    style = androidx.compose.ui.graphics.drawscope.Stroke(width = 1.dp.toPx())
                )
            }
        }

        // Overlay labels for larger segments
        layouts.forEach { (node, rect) ->
            if (rect.width > 60f && rect.height > 40f) {
                Box(
                    modifier = Modifier
                        .offset(x = (rect.left / density.density).dp, y = (rect.top / density.density).dp)
                        .size((rect.width / density.density).dp, (rect.height / density.density).dp)
                        .padding(4.dp),
                    contentAlignment = Alignment.TopStart
                ) {
                    Column {
                        Text(
                            node.name,
                            style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                            color = Color.White,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            formatCurrencyRight(node.value, currency, precision),
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, fontSize = 9.sp),
                            color = Color.White.copy(alpha = 0.8f),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
        }

        // Tooltip Overlay
        selectedNode?.let { node ->
            touchPoint?.let { point ->
                Surface(
                    modifier = Modifier
                        .offset(
                            x = (point.x / density.density).dp - 60.dp,
                            y = (point.y / density.density).dp - 80.dp
                        )
                        .width(140.dp)
                        .clip(RoundedCornerShape(8.dp)),
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    tonalElevation = 4.dp,
                    shadowElevation = 4.dp
                ) {
                    Column(
                        modifier = Modifier.padding(8.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            node.name,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            formatCurrencyRight(node.value, currency, precision),
                            style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                            color = node.color
                        )
                    }
                }
            }
        }
    }
}

/**
 * Simple Squarified Treemap algorithm implementation.
 */
private fun computeTreemapLayout(
    nodes: List<AeonTreemapNode>,
    bounds: Rect,
    totalValue: BigDecimal
): List<Pair<AeonTreemapNode, Rect>> {
    if (nodes.isEmpty() || totalValue.signum() == 0) return emptyList()
    
    val result = mutableListOf<Pair<AeonTreemapNode, Rect>>()
    val sortedNodes = nodes.sortedByDescending { it.value }
    
    divide(sortedNodes, bounds, totalValue, result)
    
    return result
}

private fun divide(
    nodes: List<AeonTreemapNode>,
    bounds: Rect,
    total: BigDecimal,
    result: MutableList<Pair<AeonTreemapNode, Rect>>
) {
    if (nodes.isEmpty()) return
    if (nodes.size == 1) {
        result.add(nodes[0] to bounds)
        return
    }

    // Split nodes into two groups
    var halfValue = BigDecimal.ZERO
    val targetHalf = total.divide(BigDecimal("2"), 10, RoundingMode.HALF_UP)
    var splitIndex = 0
    
    for (i in nodes.indices) {
        halfValue = halfValue.add(nodes[i].value)
        splitIndex = i
        if (halfValue >= targetHalf) break
    }

    val firstPart = nodes.subList(0, splitIndex + 1)
    val secondPart = nodes.subList(splitIndex + 1, nodes.size)
    val firstTotal = halfValue
    val secondTotal = total - halfValue

    if (bounds.width > bounds.height) {
        // Vertical split
        val splitX = bounds.left + bounds.width * (firstTotal.divide(total, 10, RoundingMode.HALF_UP).toFloat())
        divide(firstPart, Rect(bounds.left, bounds.top, splitX, bounds.bottom), firstTotal, result)
        divide(secondPart, Rect(splitX, bounds.top, bounds.right, bounds.bottom), secondTotal, result)
    } else {
        // Horizontal split
        val splitY = bounds.top + bounds.height * (firstTotal.divide(total, 10, RoundingMode.HALF_UP).toFloat())
        divide(firstPart, Rect(bounds.left, bounds.top, bounds.right, splitY), firstTotal, result)
        divide(secondPart, Rect(bounds.left, splitY, bounds.right, bounds.bottom), secondTotal, result)
    }
}

@androidx.compose.ui.tooling.preview.Preview(showBackground = true)
@Composable
fun AeonTreemapPreview() {
    OssWalletTheme {
        Box(modifier = Modifier.size(400.dp, 300.dp).padding(16.dp)) {
            AeonTreemap(
                nodes = listOf(
                    AeonTreemapNode("Food", BigDecimal("500.00"), Color(0xFFFF5722)),
                    AeonTreemapNode("Rent", BigDecimal("1200.00"), Color(0xFF2196F3)),
                    AeonTreemapNode("Transport", BigDecimal("300.00"), Color(0xFF4CAF50)),
                    AeonTreemapNode("Entertainment", BigDecimal("200.00"), Color(0xFF9C27B0)),
                    AeonTreemapNode("Utilities", BigDecimal("150.00"), Color(0xFFFFC107)),
                    AeonTreemapNode("Health", BigDecimal("100.00"), Color(0xFFE91E63))
                )
            )
        }
    }
}
