package com.osscabinet.ossWallet.ui.components.visualization

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.osscabinet.ossWallet.ui.theme.AppColors
import com.osscabinet.ossWallet.ui.theme.OssWalletTheme

/**
 * Data class representing a segment in [AeonSegmentedProgressBar].
 * @param weight The proportional width of this segment (0.0 to 1.0).
 * @param color The color of this segment.
 */
data class ProgressBarSegment(
    val weight: Float,
    val color: Color
)

@Composable
fun AeonProgressBar(
    progress: Float,
    modifier: Modifier = Modifier,
    color: Color = AppColors.primary,
    trackColor: Color = AppColors.insetSurface,
    height: Dp = 12.dp,
    animate: Boolean = false
) {
    var targetProgress by remember { mutableStateOf(if (animate) 0f else progress) }

    val animatedProgress by animateFloatAsState(
        targetValue = targetProgress,
        animationSpec = tween(durationMillis = 1000),
        label = "AeonProgressBarProgress"
    )

    LaunchedEffect(progress) {
        targetProgress = progress
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(height)
            .clip(CircleShape)
            .background(trackColor)
    ) {
        Box(
            modifier = Modifier
                .fillMaxHeight()
                .fillMaxWidth(animatedProgress.coerceIn(0f, 1f))
                .clip(CircleShape)
                .background(color)
        )
    }
}

@Preview(showBackground = true)
@Composable
fun AeonProgressBarPreview() {
    OssWalletTheme {
        Surface(modifier = Modifier.padding(16.dp)) {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                AeonProgressBar(progress = 0.3f)
                AeonProgressBar(progress = 0.7f, color = Color.Red)
                AeonProgressBar(progress = 1.0f, color = Color.Green)
                
                AeonSegmentedProgressBar(
                    segments = listOf(
                        ProgressBarSegment(0.3f, Color.Red),
                        ProgressBarSegment(0.5f, Color.Green),
                        ProgressBarSegment(0.2f, Color.Blue)
                    )
                )
            }
        }
    }
}

/**
 * A segmented progress bar that can show multiple portions with different colors.
 * Reusable implementation for transaction splitting, budget allocations, etc.
 */
@Composable
fun AeonSegmentedProgressBar(
    segments: List<ProgressBarSegment>,
    modifier: Modifier = Modifier,
    height: Dp = 4.dp,
    trackColor: Color = AppColors.insetSurface.copy(alpha = 0.2f)
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(height)
            .clip(CircleShape)
            .background(trackColor)
    ) {
        segments.forEach { segment ->
            if (segment.weight > 0f) {
                Box(
                    modifier = Modifier
                        .fillMaxHeight()
                        .weight(segment.weight)
                        .background(segment.color)
                )
            }
        }
    }
}
