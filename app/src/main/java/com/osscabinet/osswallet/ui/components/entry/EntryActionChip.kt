package com.osscabinet.ossWallet.ui.components.entry

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

import com.osscabinet.ossWallet.ui.theme.AeonTheme

/**
 * Compact action chip for entry screens.
 * Following the "Architect Slate" design specification with haptics and animations.
 * Supports an optional selected state for single or multi-selection scenarios.
 */
@Composable
fun EntryActionChip(
    icon: ImageVector,
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    isSelected: Boolean = false,
    iconTint: Color? = null,
    iconContainerColor: Color? = null,
    selectedColor: Color = MaterialTheme.colorScheme.primary
) {
    val haptic = LocalHapticFeedback.current
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(if (isPressed) 0.96f else 1f, label = "chipScale")
    
    val backgroundColor by animateColorAsState(
        if (isSelected) selectedColor.copy(alpha = 0.08f)
        else MaterialTheme.colorScheme.surfaceContainerLowest,
        label = "bgColor"
    )
    
    val borderColor by animateColorAsState(
        if (isSelected) selectedColor
        else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.2f),
        label = "borderColor"
    )

    Surface(
        onClick = {
            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
            onClick()
        },
        interactionSource = interactionSource,
        shape = RoundedCornerShape(AeonTheme.dimensions.chipCornerRadius),
        color = backgroundColor,
        border = BorderStroke(if (isSelected) 1.5.dp else 0.5.dp, borderColor),
        modifier = modifier
            .height(48.dp)
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
    ) {
        Row(
            modifier = Modifier.padding(horizontal = AeonTheme.dimensions.chipPadding),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(AeonTheme.dimensions.spacingSm)
        ) {
            Surface(
                shape = CircleShape,
                color = iconContainerColor ?: (if (isSelected) selectedColor.copy(alpha = 0.12f) else MaterialTheme.colorScheme.primary.copy(alpha = 0.08f)),
                modifier = Modifier.size(AeonTheme.dimensions.iconSizeLg)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        modifier = Modifier.size(AeonTheme.dimensions.iconSizeSm),
                        tint = iconTint ?: (if (isSelected) selectedColor else MaterialTheme.colorScheme.primary)
                    )
                }
            }
            Text(
                text = label,
                style = MaterialTheme.typography.labelLarge.copy(
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.SemiBold,
                    letterSpacing = 0.2.sp
                ),
                color = if (isSelected) selectedColor else MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
            )
        }
    }
}
