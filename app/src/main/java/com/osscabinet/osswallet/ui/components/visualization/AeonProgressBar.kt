package com.osscabinet.ossWallet.ui.components.visualization

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.osscabinet.ossWallet.ui.theme.AppColors

@Composable
fun AeonProgressBar(
    progress: Float,
    modifier: Modifier = Modifier,
    color: Color = AppColors.primary,
    trackColor: Color = AppColors.insetSurface,
    height: Dp = 12.dp,
    animate: Boolean = true
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
