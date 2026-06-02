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
import androidx.compose.ui.graphics.graphicsLayer
import com.osscabinet.ossWallet.ui.theme.AppColors
import com.osscabinet.ossWallet.ui.theme.LocalContourBgVersion
import com.osscabinet.ossWallet.ui.theme.LocalContourLinesEnabled
import com.osscabinet.ossWallet.ui.theme.LocalGlassmorphismEnabled
import com.osscabinet.ossWallet.ui.theme.LocalRandomizeContourEachLoad
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
    val contoursEnabled = LocalContourLinesEnabled.current
    if (!glassEnabled || !contoursEnabled) return

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
    val randomize = LocalRandomizeContourEachLoad.current

    // Pre-calculate combined paths to minimize draw calls and object allocation
    val pathData = remember(randomize) {
        val random = if (randomize) Random.Default else Random(42)
        val gridX = 40 // Optimized from 50
        val gridY = 40
        val cellW = 1000f / gridX
        val cellH = 1000f / gridY

        val peaks = List(random.nextInt(10, 15)) {
            PeakV4(
                x = random.nextFloat(),
                y = random.nextFloat(),
                strength = random.nextFloat() * 0.5f + 0.1f,
                radiusSq = (random.nextFloat() * 0.3f + 0.05f).let { it * it }
            )
        }

        val grid = Array(gridX + 1) { ix ->
            FloatArray(gridY + 1) { iy ->
                val x = ix.toFloat() / gridX
                val y = iy.toFloat() / gridY
                var total = 0f
                for (p in peaks) {
                    val dx = x - p.x
                    val dy = y - p.y
                    total += p.strength * exp(-(dx * dx + dy * dy) / p.radiusSq)
                }
                total
            }
        }

        val normalPath = Path()
        val indexPath = Path()
        val thresholdCount = 12 // Optimized from 20

        for (i in 0 until thresholdCount) {
            val t = (i + 1) * 0.08f
            val isIndex = (i + 1) % 4 == 0
            val targetPath = if (isIndex) indexPath else normalPath

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
                        targetPath.moveTo(segments[s].x, segments[s].y)
                        targetPath.lineTo(segments[s + 1].x, segments[s + 1].y)
                    }
                }
            }
        }
        Pair(normalPath, indexPath)
    }

    Canvas(
        modifier = modifier
            .fillMaxSize()
            .graphicsLayer { // Enables hardware layer for the entire background
                clip = true
            }
    ) {
        val scaleX = size.width / 1000f
        val scaleY = size.height / 1000f

        withTransform({
            scale(scaleX, scaleY, pivot = Offset.Zero)
        }) {
            // Only 2 draw calls instead of 20+
            drawPath(
                path = pathData.first,
                color = baseColor.copy(alpha = 0.12f),
                style = Stroke(width = 1.2f / scaleX)
            )
            drawPath(
                path = pathData.second,
                color = baseColor.copy(alpha = 0.35f),
                style = Stroke(width = 3.2f / scaleX)
            )
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
    val randomize = LocalRandomizeContourEachLoad.current
    val baseColor = AppColors.onScreenBackground

    // Pre-calculate paths to avoid allocation in the draw loop
    val combinedPath = remember(randomize) {
        val random = if (randomize) Random.Default else Random(42)
        val count = random.nextInt(4, 7)
        val path = Path()
        
        repeat(count) {
            val centerX = random.nextFloat() * 1400f - 200f
            val centerY = random.nextFloat() * 1400f - 200f
            val baseRadius = random.nextFloat() * 400f + 250f
            val levels = random.nextInt(6, 12)
            val seed = random.nextFloat() * 20f

            for (i in 1..levels) {
                val radius = baseRadius * (i.toFloat() / levels)
                val points = 40
                for (p in 0..points) {
                    val angle = (p.toFloat() / points) * 2 * PI
                    val wobble = (
                        sin(angle * 2 + i + seed) * (18f + i * 2) + 
                        sin(angle * 4 - i * 0.5 + seed) * 12f
                    ).toFloat()
                    
                    val x = centerX + (radius + wobble) * cos(angle).toFloat()
                    val y = centerY + (radius + wobble) * sin(angle).toFloat()
                    if (p == 0) path.moveTo(x, y) else path.lineTo(x, y)
                }
                path.close()
            }
        }
        path
    }

    Canvas(modifier = modifier.fillMaxSize().graphicsLayer { clip = true }) {
        val scaleX = size.width / 1000f
        val scaleY = size.height / 1000f
        withTransform({
            scale(scaleX, scaleY, pivot = Offset.Zero)
        }) {
            drawPath(path = combinedPath, color = baseColor.copy(alpha = 0.08f), style = Stroke(width = 1.5f / scaleX))
        }
    }
}
// endregion

// region Bold & Simple
@Composable
private fun BoldContours(modifier: Modifier) {
    val randomize = LocalRandomizeContourEachLoad.current
    val baseColor = AppColors.onScreenBackground

    val combinedPath = remember(randomize) {
        val random = if (randomize) Random.Default else Random(42)
        val count = random.nextInt(3, 6)
        val path = Path()

        repeat(count) {
            val centerX = random.nextFloat() * 1200f - 100f
            val centerY = random.nextFloat() * 1200f - 100f
            val baseRadius = random.nextFloat() * 300f + 300f
            val levels = random.nextInt(4, 9)
            val seed = random.nextFloat() * 10f

            for (i in 1..levels) {
                val radius = baseRadius * (i.toFloat() / levels)
                val points = 30
                for (p in 0..points) {
                    val angle = (p.toFloat() / points) * 2 * PI
                    val wobble = (
                        sin(angle * 2 + i + seed) * 15f + 
                        sin(angle * 5 - i + seed) * 10f
                    ).toFloat()
                    
                    val x = centerX + (radius + wobble) * cos(angle).toFloat()
                    val y = centerY + (radius + wobble) * sin(angle).toFloat()
                    if (p == 0) path.moveTo(x, y) else path.lineTo(x, y)
                }
                path.close()
            }
        }
        path
    }

    Canvas(modifier = modifier.fillMaxSize().graphicsLayer { clip = true }) {
        val scaleX = size.width / 1000f
        val scaleY = size.height / 1000f
        withTransform({
            scale(scaleX, scaleY, pivot = Offset.Zero)
        }) {
            drawPath(path = combinedPath, color = baseColor.copy(alpha = 0.2f), style = Stroke(width = 5f / scaleX))
        }
    }
}
// endregion

// region Clean Original
@Composable
private fun CleanContours(modifier: Modifier) {
    val randomize = LocalRandomizeContourEachLoad.current
    val baseColor = AppColors.onScreenBackground

    val combinedPath = remember(randomize) {
        val random = if (randomize) Random.Default else Random(42)
        val count = random.nextInt(4, 7)
        val path = Path()

        repeat(count) {
            val centerX = random.nextFloat() * 1400f - 200f
            val centerY = random.nextFloat() * 1400f - 200f
            val baseRadius = random.nextFloat() * 400f + 250f
            val levels = random.nextInt(6, 12)
            val seed = random.nextFloat() * 20f

            for (i in 1..levels) {
                val radius = baseRadius * (i.toFloat() / levels)
                val points = 40
                for (p in 0..points) {
                    val angle = (p.toFloat() / points) * 2 * PI
                    val wobble = (sin(angle * 2 + i + seed) * (18f + i * 2)).toFloat()
                    val x = centerX + (radius + wobble) * cos(angle).toFloat()
                    val y = centerY + (radius + wobble) * sin(angle).toFloat()
                    if (p == 0) path.moveTo(x, y) else path.lineTo(x, y)
                }
                path.close()
            }
        }
        path
    }

    Canvas(modifier = modifier.fillMaxSize().graphicsLayer { clip = true }) {
        val scaleX = size.width / 1000f
        val scaleY = size.height / 1000f
        withTransform({
            scale(scaleX, scaleY, pivot = Offset.Zero)
        }) {
            drawPath(path = combinedPath, color = baseColor.copy(alpha = 0.08f), style = Stroke(width = 1.5f / scaleX))
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
