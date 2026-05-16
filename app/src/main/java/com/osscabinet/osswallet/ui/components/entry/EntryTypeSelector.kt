package com.osscabinet.ossWallet.ui.components.entry

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.osscabinet.ossWallet.ui.theme.*

/**
 * Animated type selector for entry screens.
 * Following the "Architect Slate" design specification.
 */
@Composable
fun <T> EntryTypeSelector(
    selectedType: T,
    onTypeSelected: (T) -> Unit,
    types: List<Triple<ImageVector, String, T>>,
    selectedColor: Color = AppColors.primary,
    itemWidth: androidx.compose.ui.unit.Dp = 80.dp
) {
    val selectedIndex = types.indexOfFirst { it.third == selectedType }.coerceAtLeast(0)

    val indicatorColor by animateColorAsState(
        targetValue = selectedColor,
        animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
        label = "indicatorColor"
    )

    val indicatorOffset by animateDpAsState(
        targetValue = itemWidth * selectedIndex,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioLowBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "indicatorOffset"
    )

    Box(
        modifier = Modifier
            .height(44.dp)
            .clip(RoundedCornerShape(32.dp))
            .background(AppColors.recessedSurface)
            .border(0.5.dp, AppColors.divider.copy(alpha = 0.15f), RoundedCornerShape(32.dp))
            .padding(4.dp)
    ) {
        Box(
            modifier = Modifier
                .offset { androidx.compose.ui.unit.IntOffset(indicatorOffset.roundToPx(), 0) }
                .width(itemWidth)
                .fillMaxHeight()
                .clip(CircleShape)
                .background(indicatorColor)
        )

        Row(
            modifier = Modifier.fillMaxHeight(),
            horizontalArrangement = Arrangement.spacedBy(0.dp)
        ) {
            types.forEach { (icon, label, type) ->
                val isSelected = selectedType == type
                val scale by animateFloatAsState(
                    targetValue = if (isSelected) 1.1f else 1.0f,
                    animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
                    label = "iconScale"
                )

                Box(
                    modifier = Modifier
                        .width(itemWidth)
                        .fillMaxHeight()
                        .clip(CircleShape)
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null
                        ) { onTypeSelected(type) },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = label,
                        tint = if (isSelected) ColorOnDark else AppColors.contentSecondary.copy(alpha = 0.6f),
                        modifier = Modifier
                            .size(26.dp)
                            .graphicsLayer(scaleX = scale, scaleY = scale)
                    )
                }
            }
        }
    }
}
