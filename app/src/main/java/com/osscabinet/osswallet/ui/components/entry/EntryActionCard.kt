package com.osscabinet.ossWallet.ui.components.entry

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Error
import androidx.compose.material.icons.rounded.AccountBalanceWallet
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.osscabinet.ossWallet.ui.theme.AeonTheme
import com.osscabinet.ossWallet.ui.theme.AppColors
import com.osscabinet.ossWallet.ui.theme.AppTextStyles
import com.osscabinet.ossWallet.ui.theme.OssWalletTheme

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
    enabled: Boolean = true,
    isError: Boolean = false,
    iconContainerColor: Color = AppColors.primary.copy(alpha = 0.1f),
    iconTint: Color = AppColors.primary,
    selectedColor: Color = AppColors.primary,
    trailingIcon: ImageVector? = null,
    onTrailingIconClick: (() -> Unit)? = null
) {
    val haptic = LocalHapticFeedback.current
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(if (isPressed && enabled) 0.98f else 1f, label = "cardScale")

    // Luminance-based contrast: dark text on light backgrounds, white on dark
    val contentColor = remember(isSelected, selectedColor) {
        if (isSelected) {
            if (selectedColor.luminance() > 0.4f) Color(0xFF1A1A1A) else Color.White
        } else Color.Unspecified
    }

    val backgroundColor by animateColorAsState(
        if (isSelected) selectedColor 
        else if (com.osscabinet.ossWallet.ui.theme.LocalGlassmorphismEnabled.current) AppColors.insetSurface.copy(alpha = 0.4f)
        else AppColors.surfaceContainerLow,
        label = "bgColor"
    )

    val borderColor by animateColorAsState(
        if (isSelected) selectedColor 
        else if (com.osscabinet.ossWallet.ui.theme.LocalGlassmorphismEnabled.current) Color.White.copy(alpha = 0.1f)
        else AppColors.outlineVariant.copy(alpha = 0.5f),
        label = "borderColor"
    )

    Surface(
        onClick = {
            if (enabled) {
                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                onClick()
            }
        },
        interactionSource = interactionSource,
        enabled = enabled,
        shape = RoundedCornerShape(AeonTheme.dimensions.chipCornerRadius),
        color = backgroundColor,
        border = BorderStroke(if (isSelected) 1.5.dp else 1.dp, borderColor),
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
                color = if (isSelected) AppColors.transparent else iconContainerColor,
                modifier = Modifier.size(AeonTheme.dimensions.iconSizeLg)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        icon,
                        contentDescription = null,
                        modifier = Modifier.size(AeonTheme.dimensions.iconSizeMd),
                        tint = if (isSelected) contentColor else iconTint
                    )
                }
            }
            Spacer(modifier = Modifier.width(AeonTheme.dimensions.spacingLg))
            Column(
                verticalArrangement = Arrangement.Center,
                modifier = Modifier.weight(1f)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = label,
                        style = AppTextStyles.overlineLabel,
                        color = when {
                            isSelected -> contentColor
                            isError -> AppColors.error
                            else -> AppColors.contentSecondary
                        }
                    )
                    if (isError) {
                        Spacer(modifier = Modifier.width(4.dp))
                        Icon(
                            imageVector = Icons.Rounded.Error,
                            contentDescription = null,
                            tint = AppColors.error,
                            modifier = Modifier.size(10.dp)
                        )
                    }
                }
                Text(
                    text = value,
                    style = AppTextStyles.managementItemTitle,
                    color = if (isSelected) contentColor else AppColors.contentPrimary,
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
                        tint = if (isSelected) contentColor
                        else AppColors.contentSecondary
                    )
                }
            }
        }
    }
}


@Preview(showBackground = true)
@Composable
fun EntryActionCardPreview() {
    OssWalletTheme {
        Surface(modifier = Modifier.padding(16.dp)) {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                EntryActionCard(
                    icon = Icons.Rounded.AccountBalanceWallet,
                    label = "ACCOUNT",
                    value = "Cash",
                    onClick = {},
                    modifier = Modifier.weight(1f)
                )
                EntryActionCard(
                    icon = Icons.Rounded.AccountBalanceWallet,
                    label = "ACCOUNT",
                    value = "Savings",
                    onClick = {},
                    isSelected = true,
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}
