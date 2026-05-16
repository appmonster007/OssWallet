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
import com.osscabinet.ossWallet.ui.theme.AppColors

/**
 * Shared action card for entry screens.
 * Displays a circle icon, a small uppercase label, and a value below it.
 * Following the "Architect Slate" design specification with haptics and animations.
 * Supports an optional selected state for scenarios requiring highlight.
 */
@Composable
fun EntryActionCard(
    icon: ImageVector,
    label: String,
    value: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    isSelected: Boolean = false,
    iconContainerColor: Color = AppColors.primary.copy(alpha = 0.1f),
    iconTint: Color = AppColors.primary,
    selectedColor: Color = AppColors.primary,
    trailingIcon: ImageVector? = null,
    onTrailingIconClick: (() -> Unit)? = null
) {
    val haptic = LocalHapticFeedback.current
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(if (isPressed) 0.98f else 1f, label = "cardScale")

    val backgroundColor by animateColorAsState(
        if (isSelected) selectedColor.copy(alpha = 0.08f)
        else AppColors.cardSurface,
        label = "bgColor"
    )
    
    val borderColor by animateColorAsState(
        if (isSelected) selectedColor
        else AppColors.divider.copy(alpha = 0.15f),
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
        shadowElevation = 0.dp,
        modifier = modifier
            .height(72.dp)
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
    ) {
        Row(
            modifier = Modifier.padding(horizontal = AeonTheme.dimensions.cardPadding),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                shape = CircleShape,
                color = if (isSelected) selectedColor.copy(alpha = 0.12f) else iconContainerColor,
                modifier = Modifier.size(AeonTheme.dimensions.iconSizeLg)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        icon,
                        contentDescription = null,
                        modifier = Modifier.size(AeonTheme.dimensions.iconSizeSm),
                        tint = if (isSelected) selectedColor else iconTint
                    )
                }
            }
            Spacer(modifier = Modifier.width(AeonTheme.dimensions.spacingLg))
            Column(
                verticalArrangement = Arrangement.Center,
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.5.sp,
                        fontSize = 10.sp
                    ),
                    color = if (isSelected) selectedColor.copy(alpha = 0.7f) else AppColors.contentSecondary.copy(alpha = 0.6f)
                )
                Text(
                    text = value,
                    style = MaterialTheme.typography.bodyLarge.copy(
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.SemiBold
                    ),
                    color = if (isSelected) selectedColor else AppColors.contentPrimary,
                    maxLines = 1,
                    overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                )
            }

            if (trailingIcon != null) {
                IconButton(
                    onClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        onTrailingIconClick?.invoke()
                    },
                    modifier = Modifier.size(AeonTheme.dimensions.iconSizeMd)
                ) {
                    Icon(
                        trailingIcon,
                        contentDescription = null,
                        modifier = Modifier.size(AeonTheme.dimensions.iconSizeSm),
                        tint = if (isSelected) selectedColor.copy(alpha = 0.5f) else AppColors.contentSecondary.copy(alpha = 0.4f)
                    )
                }
            }
        }
    }
}
