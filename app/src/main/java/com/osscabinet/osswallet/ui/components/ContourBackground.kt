package com.osscabinet.ossWallet.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import com.osscabinet.ossWallet.ui.theme.AppColors
import com.osscabinet.ossWallet.ui.theme.LocalGlassmorphismEnabled
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

/**
 * A decorative background that draws subtle topographic contour lines.
 * Only visible when Glassmorphism is enabled to enhance the "frosted glass" depth effect.
 */
@Composable
fun ContourBackground(
    modifier: Modifier = Modifier,
) {
    val glassEnabled = LocalGlassmorphismEnabled.current
    if (!glassEnabled) return

    // Use a color that contrasts clearly with the screen background
    // onScreenBackground is dark on light theme and light on dark theme
    val lineColor = AppColors.onScreenBackground.copy(alpha = 0.5f)

    Canvas(modifier = modifier.fillMaxSize()) {
        val width = size.width
        val height = size.height

        // Top-left organic shape
        drawContourCluster(
            centerX = width * 0.05f,
            centerY = height * 0.15f,
            baseRadius = 450f,
            levels = 6,
            color = lineColor
        )

        // Bottom-right organic shape
        drawContourCluster(
            centerX = width * 0.95f,
            centerY = height * 0.85f,
            baseRadius = 600f,
            levels = 8,
            color = lineColor
        )

        // Mid-left organic shape
        drawContourCluster(
            centerX = width * -0.1f,
            centerY = height * 0.6f,
            baseRadius = 500f,
            levels = 5,
            color = lineColor
        )
    }
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawContourCluster(
    centerX: Float,
    centerY: Float,
    baseRadius: Float,
    levels: Int,
    color: androidx.compose.ui.graphics.Color
) {
    for (i in 1..levels) {
        val radius = baseRadius * (i.toFloat() / levels)
        val path = Path().apply {
            val points = 30
            for (p in 0..points) {
                val angle = (p.toFloat() / points) * 2 * PI
                
                // Add some organic wobbliness using multiple sine waves
                val wobble = (
                    sin(angle * 2 + i) * 15f + 
                    sin(angle * 5 - i) * 10f +
                    cos(angle * 3 + (i * 0.5f)) * 12f
                ).toFloat()
                
                val x = centerX + (radius + wobble) * cos(angle).toFloat()
                val y = centerY + (radius + wobble) * sin(angle).toFloat()
                
                if (p == 0) moveTo(x, y) else lineTo(x, y)
            }
            close()
        }
        drawPath(
            path = path,
            color = color,
            style = Stroke(width = 5f)
        )
    }
}
