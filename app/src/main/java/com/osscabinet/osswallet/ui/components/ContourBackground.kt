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
    val contoursEnabled = LocalContourLinesEnabled.current
    if (!contoursEnabled) return

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

    val pathData = remember(randomize) {
        val random = if (randomize) Random.Default else Random(42)
        val gridX = 30 // Reduced for efficiency
        val gridY = 30
        val cellW = 1000f / gridX
        val cellH = 1000f / gridY

        val peaks = List(random.nextInt(8, 12)) {
            PeakV4(
                x = random.nextFloat(),
                y = random.nextFloat(),
                strength = random.nextFloat() * 0.5f + 0.1f,
                radiusSq = (random.nextFloat() * 0.35f + 0.1f).let { it * it }
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
                    val d2 = dx * dx + dy * dy
                    if (d2 < p.radiusSq * 4) { // Optimization: early exit for distant peaks
                        total += p.strength * exp(-d2 / p.radiusSq)
                    }
                }
                total
            }
        }

        val normalPath = Path()
        val indexPath = Path()
        val thresholdCount = 10 // Reduced from 12

        for (i in 0 until thresholdCount) {
            val t = (i + 1) * 0.1f
            val isIndex = (i + 1) % 4 == 0
            val targetPath = if (isIndex) indexPath else normalPath

            for (ix in 0 until gridX) {
                val xPos = ix * cellW
                for (iy in 0 until gridY) {
                    val yPos = iy * cellH
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

                    // Inlined and optimized segment generation
                    drawSegmentsInPath(targetPath, cellIndex, v0, v1, v2, v3, t, xPos, yPos, cellW, cellH)
                }
            }
        }
        Pair(normalPath, indexPath)
    }

    Canvas(
        modifier = modifier
            .fillMaxSize()
            .graphicsLayer {
                // Hint for GPU to cache this layer
                renderEffect = null 
                clip = true
            }
    ) {
        val scaleX = size.width / 1000f
        val scaleY = size.height / 1000f

        withTransform({
            scale(scaleX, scaleY, pivot = Offset.Zero)
        }) {
            drawPath(
                path = pathData.first,
                color = baseColor.copy(alpha = 0.10f),
                style = Stroke(width = 1.0f / scaleX)
            )
            drawPath(
                path = pathData.second,
                color = baseColor.copy(alpha = 0.30f),
                style = Stroke(width = 2.8f / scaleX)
            )
        }
    }
}

private fun drawSegmentsInPath(
    path: Path, cellIndex: Int, v0: Float, v1: Float, v2: Float, v3: Float, t: Float,
    x: Float, y: Float, cw: Float, ch: Float
) {
    fun getOffset(side: Int): Offset {
        return when (side) {
            0 -> Offset(x + (t - v0) / (v1 - v0) * cw, y)
            1 -> Offset(x + cw, y + (t - v1) / (v2 - v1) * ch)
            2 -> Offset(x + (t - v3) / (v2 - v3) * cw, y + ch)
            else -> Offset(x, y + (t - v0) / (v3 - v0) * ch)
        }
    }

    when (cellIndex) {
        1, 14 -> { val e0 = getOffset(0); val e3 = getOffset(3); path.moveTo(e0.x, e0.y); path.lineTo(e3.x, e3.y) }
        2, 13 -> { val e0 = getOffset(0); val e1 = getOffset(1); path.moveTo(e0.x, e0.y); path.lineTo(e1.x, e1.y) }
        3, 12 -> { val e1 = getOffset(1); val e3 = getOffset(3); path.moveTo(e1.x, e1.y); path.lineTo(e3.x, e3.y) }
        4, 11 -> { val e1 = getOffset(1); val e2 = getOffset(2); path.moveTo(e1.x, e1.y); path.lineTo(e2.x, e2.y) }
        5 -> {
            val e0 = getOffset(0); val e1 = getOffset(1); path.moveTo(e0.x, e0.y); path.lineTo(e1.x, e1.y)
            val e2 = getOffset(2); val e3 = getOffset(3); path.moveTo(e2.x, e2.y); path.lineTo(e3.x, e3.y)
        }
        6, 9 -> { val e0 = getOffset(0); val e2 = getOffset(2); path.moveTo(e0.x, e0.y); path.lineTo(e2.x, e2.y) }
        7, 8 -> { val e3 = getOffset(3); val e2 = getOffset(2); path.moveTo(e3.x, e3.y); path.lineTo(e2.x, e2.y) }
        10 -> {
            val e0 = getOffset(0); val e3 = getOffset(3); path.moveTo(e0.x, e0.y); path.lineTo(e3.x, e3.y)
            val e1 = getOffset(1); val e2 = getOffset(2); path.moveTo(e1.x, e1.y); path.lineTo(e2.x, e2.y)
        }
    }
}

private data class PeakV4(val x: Float, val y: Float, val strength: Float, val radiusSq: Float)

// endregion
// endregion

@Composable
private fun OrganicContours(modifier: Modifier) {
    val randomize = LocalRandomizeContourEachLoad.current
    val baseColor = AppColors.onScreenBackground

    val combinedPath = remember(randomize) {
        val random = if (randomize) Random.Default else Random(42)
        val count = random.nextInt(3, 4) // Further reduced
        val path = Path()
        
        repeat(count) {
            val centerX = random.nextFloat() * 1400f - 200f
            val centerY = random.nextFloat() * 1400f - 200f
            val baseRadius = random.nextFloat() * 400f + 250f
            val levels = random.nextInt(4, 8) // Further reduced
            val seed = random.nextFloat() * 20f

            for (i in 1..levels) {
                val radius = baseRadius * (i.toFloat() / levels)
                val points = 16 // Optimized for GPU vertex limit
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
            drawPath(path = combinedPath, color = baseColor.copy(alpha = 0.08f), style = Stroke(width = 1.0f / scaleX))
        }
    }
}

// region Bold & Simple
@Composable
private fun BoldContours(modifier: Modifier) {
    val randomize = LocalRandomizeContourEachLoad.current
    val baseColor = AppColors.onScreenBackground

    val combinedPath = remember(randomize) {
        val random = if (randomize) Random.Default else Random(42)
        val count = random.nextInt(2, 3) // Further reduced
        val path = Path()

        repeat(count) {
            val centerX = random.nextFloat() * 1200f - 100f
            val centerY = random.nextFloat() * 1200f - 100f
            val baseRadius = random.nextFloat() * 300f + 300f
            val levels = random.nextInt(3, 6) // Further reduced
            val seed = random.nextFloat() * 10f

            for (i in 1..levels) {
                val radius = baseRadius * (i.toFloat() / levels)
                val points = 16 // Optimized
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
            drawPath(path = combinedPath, color = baseColor.copy(alpha = 0.2f), style = Stroke(width = 3.5f / scaleX))
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
        val count = random.nextInt(3, 4) // Further reduced
        val path = Path()

        repeat(count) {
            val centerX = random.nextFloat() * 1400f - 200f
            val centerY = random.nextFloat() * 1400f - 200f
            val baseRadius = random.nextFloat() * 400f + 250f
            val levels = random.nextInt(4, 8) // Further reduced
            val seed = random.nextFloat() * 20f

            for (i in 1..levels) {
                val radius = baseRadius * (i.toFloat() / levels)
                val points = 20 // Optimized
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
            drawPath(path = combinedPath, color = baseColor.copy(alpha = 0.08f), style = Stroke(width = 1.0f / scaleX))
        }
    }
}

private data class ContourCluster(
    val xFrac: Float,
    val yFrac: Float,
    val baseRadius: Float,
    val levels: Int,
    val seed: Float
)
