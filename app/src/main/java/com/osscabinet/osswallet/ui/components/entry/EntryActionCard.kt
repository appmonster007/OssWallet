package com.osscabinet.ossWallet.ui.components.entry

import com.osscabinet.ossWallet.ui.theme.GlassElement
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.background
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
import androidx.compose.material.icons.rounded.AccountBalanceWallet
import androidx.compose.material.icons.rounded.Error
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
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.osscabinet.ossWallet.ui.components.GlassSurface
import com.osscabinet.ossWallet.ui.theme.AeonTheme
import com.osscabinet.ossWallet.ui.theme.AppColors
import com.osscabinet.ossWallet.ui.theme.AppTextStyles
import com.osscabinet.ossWallet.ui.theme.ColorOnDark
import com.osscabinet.ossWallet.ui.theme.ColorOnLight
import com.osscabinet.ossWallet.ui.theme.GlassTokens
import com.osscabinet.ossWallet.ui.theme.GlassVariant
import com.osscabinet.ossWallet.ui.theme.LocalGlassmorphismEnabled
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
    val uiScale = AeonTheme.uiScale
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(if (isPressed && enabled) 0.98f else 1f, label = "cardScale")

    val glassEnabled = LocalGlassmorphismEnabled.current

    // Non-glass: solid selectedColor fill. Glass: blur + colored overlay — both use filled content colors.
    val isFilledSelected = isSelected && !glassEnabled
    val isGlassFilledSelected = isSelected && glassEnabled

    // Foreground color that reads against a filled/colored background (white or near-black)
    val filledAccentContent = remember(selectedColor) {
        if (selectedColor.luminance() > 0.4f) ColorOnLight else ColorOnDark
    }

    val backgroundColor by animateColorAsState(
        targetValue = when {
            isFilledSelected -> selectedColor
            glassEnabled -> AppColors.surfaceContainerLow.copy(alpha = 0.6f)
            else -> AppColors.surfaceContainerLow
        },
        label = "bgColor"
    )

    val borderColor by animateColorAsState(
        targetValue = when {
            isSelected -> selectedColor
            glassEnabled -> AppColors.outlineVariant.copy(alpha = 0.2f)
            else -> AppColors.outlineVariant
        },
        label = "borderColor"
    )

    val config = GlassTokens.configForVariant(GlassVariant.Card)
    val borderWidth by animateDpAsState(
        targetValue = config.surface.borderWidth.dp,
        label = "borderWidth"
    )

    GlassSurface(
        element = GlassElement.ENTRY,
        modifier = modifier
            .height(64.dp)
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
        shape = RoundedCornerShape(AeonTheme.dimensions.cardCornerRadius),
        hazeStyle = GlassTokens.cardStyle(),
        glassVariant = GlassVariant.Card,
        fallbackColor = backgroundColor,
        borderColor = borderColor,
        borderWidth = borderWidth,
    ) {
        // Glass-mode color wash: renders on top of the haze blur, behind all content.
        // Gives the same full-color appearance as the solid fill in non-glass mode.
        if (isGlassFilledSelected) {
            Box(modifier = Modifier
                .fillMaxSize()
                .background(selectedColor.copy(alpha = 0.32f)))
        }

        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = AeonTheme.dimensions.cardPadding),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                shape = CircleShape,
                // Card itself carries the color in both selected modes — icon container stays transparent.
                color = if (isSelected) AppColors.transparent else iconContainerColor,
                modifier = Modifier.size(AeonTheme.dimensions.iconSizeLg)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        icon,
                        contentDescription = null,
                        modifier = Modifier.size(AeonTheme.dimensions.iconSizeMd),
                        tint = if (isSelected) filledAccentContent else iconTint
                    )
                }
            }
            Spacer(modifier = Modifier.width(AeonTheme.dimensions.spacingSm))
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
                            isSelected -> filledAccentContent
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
                    color = if (isSelected) filledAccentContent else AppColors.contentPrimary,
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
                        tint = if (isSelected) filledAccentContent else AppColors.contentSecondary
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
