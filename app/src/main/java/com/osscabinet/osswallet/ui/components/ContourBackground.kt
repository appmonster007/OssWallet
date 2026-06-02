package com.osscabinet.ossWallet.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.withTransform
import com.osscabinet.ossWallet.ui.theme.AppColors
import com.osscabinet.ossWallet.ui.theme.LocalContourBgVersion
import com.osscabinet.ossWallet.ui.theme.LocalGlassmorphismEnabled
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.exp
import kotlin.math.sin
import kotlin.random.Random

/**
 * A decorative background that draws subtle topographic contour lines.
 * Only visible when Glassmorphism is enabled to enhance the "frosted glass" depth effect.
 */
@Composable
fun ContourBackground(
    modifier: Modifier = Modifier,
    version: Int = LocalContourBgVersion.current,
) {
    val glassEnabled = LocalGlassmorphismEnabled.current
    if (!glassEnabled) return

    when (version) {
        1 -> CleanContours(modifier)
        2 -> BoldContours(modifier)
        3 -> OrganicContours(modifier)
        else -> GeographicalContours(modifier)
    }
}

// region Geographical Elevation (Default)
@Composable
private fun GeographicalContours(modifier: Modifier) {
    val baseColor = AppColors.onScreenBackground

    // Pre-calculate paths for a high-fidelity topographic map
    val contourPaths = remember {
        val gridX = 50
        val gridY = 50
        val cellW = 1000f / gridX
        val cellH = 1000f / gridY

        // 1. Generate multi-scale peaks for organic "terrain"
        val peaks = mutableListOf<PeakV4>()
        
        // Large "mountain ranges"
        repeat(Random.nextInt(3, 5)) {
            peaks.add(PeakV4(
                x = Random.nextFloat(),
                y = Random.nextFloat(),
                strength = Random.nextFloat() * 0.6f + 0.4f,
                radiusSq = (Random.nextFloat() * 0.4f + 0.2f).let { it * it }
            ))
        }
        
        // Smaller "hills/ridges" for detail
        repeat(Random.nextInt(8, 15)) {
            peaks.add(PeakV4(
                x = Random.nextFloat(),
                y = Random.nextFloat(),
                strength = Random.nextFloat() * 0.3f + 0.1f,
                radiusSq = (Random.nextFloat() * 0.15f + 0.05f).let { it * it }
            ))
        }

        // 2. Build elevation grid with a potential field
        val grid = Array(gridX + 1) { ix ->
            FloatArray(gridY + 1) { iy ->
                val x = ix.toFloat() / gridX
                val y = iy.toFloat() / gridY
                var total = 0f
                for (p in peaks) {
                    val dx = x - p.x
                    val dy = y - p.y
                    // Gaussian influence
                    total += p.strength * exp(-(dx * dx + dy * dy) / p.radiusSq)
                }
                total
            }
        }

        // 3. Marching Squares to generate non-intersecting lines
        val thresholdCount = 20 // More lines for better density
        List(thresholdCount) { i ->
            val t = (i + 1) * 0.06f // Threshold elevation
            val path = Path()
            for (ix in 0 until gridX) {
                for (iy in 0 until gridY) {
                    val v0 = grid[ix][iy]
                    val v1 = grid[ix + 1][iy]
                    val v2 = grid[ix + 1][iy + 1]
                    val v3 = grid[ix][iy + 1]

                    var cellIndex = 0
                    if (v0 >= t) cellIndex += 1
                    if (v1 >= t) cellIndex += 2
                    if (v2 >= t) cellIndex += 4
                    if (v3 >= t) cellIndex += 8

                    if (cellIndex == 0 || cellIndex == 15) continue

                    val segments = getSegmentsForGeographical(cellIndex, v0, v1, v2, v3, t, ix, iy, cellW, cellH)
                    for (s in segments.indices step 2) {
                        path.moveTo(segments[s].x, segments[s].y)
                        path.lineTo(segments[s + 1].x, segments[s + 1].y)
                    }
                }
            }
            path
        }
    }

    Canvas(modifier = modifier.fillMaxSize()) {
        val scaleX = size.width / 1000f
        val scaleY = size.height / 1000f

        contourPaths.forEachIndexed { index, path ->
            // Elevation depth: Higher lines are slightly more "visible"
            val elevationFactor = (index.toFloat() / contourPaths.size)
            
            // Standard map conventions: index contours every 5 lines
            val isIndexContour = (index + 1) % 5 == 0
            
            // Refined alpha logic: base + elevation-based boost (increased for visibility)
            val alpha = if (isIndexContour) {
                0.25f + (elevationFactor * 0.2f)
            } else {
                0.08f + (elevationFactor * 0.12f)
            }
            
            val strokeWidth = if (isIndexContour) 3.2f else 1.2f

            withTransform({
                scale(scaleX, scaleY, pivot = Offset.Zero)
            }) {
                drawPath(
                    path = path,
                    color = baseColor.copy(alpha = alpha),
                    style = Stroke(
                        width = strokeWidth / scaleX,
                        miter = 10f
                    )
                )
            }
        }
    }
}

private data class PeakV4(val x: Float, val y: Float, val strength: Float, val radiusSq: Float)

private fun getSegmentsForGeographical(
    cellIndex: Int, v0: Float, v1: Float, v2: Float, v3: Float, t: Float,
    ix: Int, iy: Int, cw: Float, ch: Float
): List<Offset> {
    fun lerpX(x1: Int, x2: Int, y: Int, val1: Float, val2: Float) = Offset(
        (x1 + ((t - val1) / (val2 - val1)) * (x2 - x1)) * cw,
        y * ch
    )
    fun lerpY(y1: Int, y2: Int, x: Int, val1: Float, val2: Float) = Offset(
        x * cw,
        (y1 + ((t - val1) / (val2 - val1)) * (y2 - y1)) * ch
    )

    val e0 = lerpX(ix, ix + 1, iy, v0, v1)
    val e1 = lerpY(iy, iy + 1, ix + 1, v1, v2)
    val e2 = lerpX(ix, ix + 1, iy + 1, v3, v2)
    val e3 = lerpY(iy, iy + 1, ix, v0, v3)

    return when (cellIndex) {
        1, 14 -> listOf(e0, e3)
        2, 13 -> listOf(e0, e1)
        3, 12 -> listOf(e1, e3)
        4, 11 -> listOf(e1, e2)
        5 -> listOf(e0, e1, e2, e3)
        6, 9 -> listOf(e0, e2)
        7, 8 -> listOf(e3, e2)
        10 -> listOf(e0, e3, e1, e2)
        else -> emptyList()
    }
}
// endregion

// region Premium Organic
@Composable
private fun OrganicContours(modifier: Modifier) {
    val clusters = remember {
        val count = Random.nextInt(4, 7)
        List(count) {
            ContourCluster(
                xFrac = Random.nextFloat() * 1.4f - 0.2f,
                yFrac = Random.nextFloat() * 1.4f - 0.2f,
                baseRadius = Random.nextFloat() * 400f + 250f,
                levels = Random.nextInt(6, 12),
                seed = Random.nextFloat() * 20f
            )
        }
    }

    val baseColor = AppColors.onScreenBackground

    Canvas(modifier = modifier.fillMaxSize()) {
        clusters.forEach { cluster ->
            drawContourClusterOrganic(
                centerX = size.width * cluster.xFrac,
                centerY = size.height * cluster.yFrac,
                cluster = cluster,
                color = baseColor
            )
        }
    }
}

private fun DrawScope.drawContourClusterOrganic(
    centerX: Float,
    centerY: Float,
    cluster: ContourCluster,
    color: Color
) {
    for (i in 1..cluster.levels) {
        val alpha = (i.toFloat() / cluster.levels) * 0.12f + 0.03f
        val radius = cluster.baseRadius * (i.toFloat() / cluster.levels)
        
        val path = Path().apply {
            val points = 60
            for (p in 0..points) {
                val angle = (p.toFloat() / points) * 2 * PI
                val wobble = (
                    sin(angle * 2 + i + cluster.seed) * (18f + i * 2) + 
                    sin(angle * 4 - i * 0.5 + cluster.seed) * 12f +
                    cos(angle * 3 + (i * 0.8f) + cluster.seed) * 10f +
                    sin(angle * 7 + cluster.seed) * 5f
                ).toFloat()
                
                val x = centerX + (radius + wobble) * cos(angle).toFloat()
                val y = centerY + (radius + wobble) * sin(angle).toFloat()
                if (p == 0) moveTo(x, y) else lineTo(x, y)
            }
            close()
        }
        drawPath(path = path, color = color.copy(alpha = alpha), style = Stroke(width = 1.5f))
    }
}
// endregion

// region Bold & Simple
@Composable
private fun BoldContours(modifier: Modifier) {
    val clusters = remember {
        val count = Random.nextInt(3, 6)
        List(count) {
            ContourCluster(
                xFrac = Random.nextFloat() * 1.2f - 0.1f,
                yFrac = Random.nextFloat() * 1.2f - 0.1f,
                baseRadius = Random.nextFloat() * 300f + 300f,
                levels = Random.nextInt(4, 9),
                seed = Random.nextFloat() * 10f
            )
        }
    }

    val lineColor = AppColors.onScreenBackground.copy(alpha = 0.2f)

    Canvas(modifier = modifier.fillMaxSize()) {
        clusters.forEach { cluster ->
            for (i in 1..cluster.levels) {
                val radius = cluster.baseRadius * (i.toFloat() / cluster.levels)
                val path = Path().apply {
                    val points = 30
                    for (p in 0..points) {
                        val angle = (p.toFloat() / points) * 2 * PI
                        val wobble = (
                            sin(angle * 2 + i + cluster.seed) * 15f + 
                            sin(angle * 5 - i + cluster.seed) * 10f +
                            cos(angle * 3 + (i * 0.5f) + cluster.seed) * 12f
                        ).toFloat()
                        
                        val x = (size.width * cluster.xFrac) + (radius + wobble) * cos(angle).toFloat()
                        val y = (size.height * cluster.yFrac) + (radius + wobble) * sin(angle).toFloat()
                        if (p == 0) moveTo(x, y) else lineTo(x, y)
                    }
                    close()
                }
                drawPath(path = path, color = lineColor, style = Stroke(width = 5f))
            }
        }
    }
}
// endregion

// region Clean Original
@Composable
private fun CleanContours(modifier: Modifier) {
    val clusters = remember {
        val count = Random.nextInt(4, 7)
        List(count) {
            ContourCluster(
                xFrac = Random.nextFloat() * 1.4f - 0.2f,
                yFrac = Random.nextFloat() * 1.4f - 0.2f,
                baseRadius = Random.nextFloat() * 400f + 250f,
                levels = Random.nextInt(6, 12),
                seed = Random.nextFloat() * 20f
            )
        }
    }

    val baseColor = AppColors.onScreenBackground

    Canvas(modifier = modifier.fillMaxSize()) {
        clusters.forEach { cluster ->
            for (i in 1..cluster.levels) {
                val alpha = (i.toFloat() / cluster.levels) * 0.12f + 0.03f
                val radius = cluster.baseRadius * (i.toFloat() / cluster.levels)
                val path = Path().apply {
                    val points = 60
                    for (p in 0..points) {
                        val angle = (p.toFloat() / points) * 2 * PI
                        val wobble = (sin(angle * 2 + i + cluster.seed) * (18f + i * 2)).toFloat()
                        val x = (size.width * cluster.xFrac) + (radius + wobble) * cos(angle).toFloat()
                        val y = (size.height * cluster.yFrac) + (radius + wobble) * sin(angle).toFloat()
                        if (p == 0) moveTo(x, y) else lineTo(x, y)
                    }
                    close()
                }
                drawPath(path = path, color = baseColor.copy(alpha = alpha), style = Stroke(width = 1.5f))
            }
        }
    }
}
// endregion

private data class ContourCluster(
    val xFrac: Float,
    val yFrac: Float,
    val baseRadius: Float,
    val levels: Int,
    val seed: Float
)
