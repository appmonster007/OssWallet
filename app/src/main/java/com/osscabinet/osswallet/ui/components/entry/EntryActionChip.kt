package com.osscabinet.ossWallet.ui.components.entry

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Label
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.osscabinet.ossWallet.ui.theme.AeonTheme
import com.osscabinet.ossWallet.ui.theme.AppColors
import com.osscabinet.ossWallet.ui.theme.AppTextStyles
import com.osscabinet.ossWallet.ui.theme.OssWalletTheme
import com.osscabinet.ossWallet.ui.theme.ThemePreviews
import com.osscabinet.ossWallet.ui.theme.scaledDp

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
) {
    val haptic = LocalHapticFeedback.current
    val uiScale = AeonTheme.uiScale
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(if (isPressed) 0.96f else 1f, label = "chipScale")

    val backgroundColor by animateColorAsState(
        if (isSelected) AppColors.primaryContainer
        else AppColors.surfaceContainerLow,
        label = "bgColor"
    )

    val borderColor by animateColorAsState(
        if (isSelected) AppColors.primary
        else AppColors.outlineVariant,
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
            .height(scaledDp(48f, uiScale))
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
                color = iconContainerColor ?: AppColors.transparent,
                modifier = Modifier.size(AeonTheme.dimensions.iconSizeLg)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        modifier = Modifier.size(AeonTheme.dimensions.iconSizeSm),
                        tint = iconTint ?: (if (isSelected) AppColors.onPrimaryContainer else AppColors.primary)
                    )
                }
            }
            Text(
                text = label,
                style = AppTextStyles.chipLabel,
                color = if (isSelected) AppColors.onPrimaryContainer else AppColors.contentPrimary,
                maxLines = 1,
                overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
            )
        }
    }
}

@ThemePreviews
@Composable
fun EntryActionChipPreview() {
    OssWalletTheme {
        Row(
            modifier = Modifier.padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            EntryActionChip(
                icon = Icons.Default.Label,
                label = "Unselected",
                onClick = {},
                isSelected = false
            )
            EntryActionChip(
                icon = Icons.Default.Add,
                label = "Selected",
                onClick = {},
                isSelected = true
            )
        }
    }
}
