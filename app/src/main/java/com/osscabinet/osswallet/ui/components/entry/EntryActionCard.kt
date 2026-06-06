package com.osscabinet.ossWallet.ui.components.entry

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
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
import com.osscabinet.ossWallet.ui.theme.LocalGlassmorphismEnabled
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.osscabinet.ossWallet.ui.components.GlassSurface
import com.osscabinet.ossWallet.ui.theme.AeonTheme
import com.osscabinet.ossWallet.ui.theme.AppColors
import com.osscabinet.ossWallet.ui.theme.AppTextStyles
import com.osscabinet.ossWallet.ui.theme.GlassTokens
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

    val glassEnabled = LocalGlassmorphismEnabled.current

    // When glassmorphism is enabled we avoid painting large solid fills with the
    // selected color. Instead we surface the accent via borders and icon containers
    // while keeping the card's glass background. When glass is disabled we keep the
    // classic filled selected style.
    val isFilledSelected = isSelected && !glassEnabled

    // Accent foreground when painting a filled background (white/dark based on luminance)
    val filledAccentContent = remember(selectedColor) {
        if (selectedColor.luminance() > 0.4f) Color(0xFF1A1A1A) else Color.White
    }

    val backgroundColor by animateColorAsState(
        targetValue = when {
            isFilledSelected -> selectedColor
            glassEnabled -> AppColors.insetSurface.copy(alpha = 0.4f)
            else -> AppColors.surfaceContainerLow
        },
        label = "bgColor"
    )

    val borderColor by animateColorAsState(
        targetValue = when {
            isSelected && glassEnabled -> selectedColor.copy(alpha = 0.5f)
            isSelected -> selectedColor
            glassEnabled -> Color.White.copy(alpha = 0.1f)
            else -> AppColors.outlineVariant.copy(alpha = 0.5f)
        },
        label = "borderColor"
    )

    val borderWidth by animateDpAsState(
        targetValue = if (isSelected) 1.5.dp else 1.dp,
        label = "borderWidth"
    )

    GlassSurface(
        modifier = modifier
            .height(72.dp)
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .clickable(
                interactionSource = interactionSource,
                indication = LocalIndication.current,
                enabled = enabled
            ) {
                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                onClick()
            },
        shape = RoundedCornerShape(AeonTheme.dimensions.chipCornerRadius),
        hazeStyle = GlassTokens.cardStyle(),
        fallbackColor = backgroundColor,
        borderColor = borderColor,
        borderAlpha = if (isSelected) 0.5f else 0.1f,
        borderWidth = borderWidth,
        shadowElevation = 0.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = AeonTheme.dimensions.cardPadding),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                shape = CircleShape,
                // If selected and glass is enabled, show a subtle tinted container. If
                // selected and glass is disabled, the card itself is filled so make the
                // icon container transparent to avoid a double chip.
                color = when {
                    isSelected && glassEnabled -> selectedColor.copy(alpha = 0.12f)
                    isSelected && !glassEnabled -> AppColors.transparent
                    else -> iconContainerColor
                },
                modifier = Modifier.size(AeonTheme.dimensions.iconSizeLg)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        icon,
                        contentDescription = null,
                        modifier = Modifier.size(AeonTheme.dimensions.iconSizeMd),
                        // Icon tint uses filledAccentContent when the card is a solid
                        // fill (for contrast), otherwise we tint with the accent color so
                        // it reads against the glass background.
                        tint = when {
                            isSelected && isFilledSelected -> filledAccentContent
                            isSelected -> selectedColor
                            else -> iconTint
                        }
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
                            isError -> AppColors.error
                            isSelected && isFilledSelected -> filledAccentContent
                            isSelected -> selectedColor
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
                    color = when {
                        isSelected && isFilledSelected -> filledAccentContent
                        isSelected -> selectedColor
                        else -> AppColors.contentPrimary
                    },
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
                        tint = if (isSelected && isFilledSelected) filledAccentContent
                        else if (isSelected) selectedColor
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
