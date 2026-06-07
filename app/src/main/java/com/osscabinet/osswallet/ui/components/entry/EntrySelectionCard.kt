package com.osscabinet.ossWallet.ui.components.entry

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Category
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
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
import com.osscabinet.ossWallet.ui.util.isDark

enum class EntrySelectionMode {
    SINGLE, MULTI, MANAGEMENT
}

/**
 * Standardized selection card for lists in entry screens.
 * Supports single, multi-selection, and management modes with consistent styling.
 */
@Composable
fun EntrySelectionCard(
    icon: ImageVector,
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    detail: String? = null,
    iconColor: Color = AppColors.primary,
    selectionMode: EntrySelectionMode = EntrySelectionMode.SINGLE,
    trailingContent: (@Composable RowScope.() -> Unit)? = null
) {
    val haptic = LocalHapticFeedback.current

    val backgroundColor by animateColorAsState(
        targetValue = if (isSelected && (selectionMode != EntrySelectionMode.MANAGEMENT)) AppColors.primaryContainer
        else AppColors.surfaceContainerLow,
        label = "bgColor"
    )

    val borderColor by animateColorAsState(
        targetValue = if (isSelected && (selectionMode != EntrySelectionMode.MANAGEMENT)) AppColors.primary
        else AppColors.outlineVariant,
        label = "borderColor"
    )

    val config = GlassTokens.configForVariant(com.osscabinet.ossWallet.ui.theme.GlassVariant.Card)
    val isActiveSelection = isSelected && selectionMode != EntrySelectionMode.MANAGEMENT
    val borderWidth by animateDpAsState(
        targetValue = if (isActiveSelection) config.borderWidth.dp else (config.borderWidth * 0.5f).dp,
        label = "borderWidth"
    )

    val interactionSource = remember { MutableInteractionSource() }

    GlassSurface(
        modifier = modifier
            .fillMaxWidth()
            .clickable(
                interactionSource = interactionSource,
                indication = LocalIndication.current
            ) {
                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                onClick()
            },
        shape = RoundedCornerShape(AeonTheme.dimensions.componentCornerRadius),
        hazeStyle = GlassTokens.cardStyle(),
        glassVariant = com.osscabinet.ossWallet.ui.theme.GlassVariant.Card,
        fallbackColor = backgroundColor,
        borderColor = borderColor,
        borderAlpha = if (isActiveSelection) config.borderAlpha else config.borderAlpha * 0.2f,
        borderWidth = borderWidth,
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(AeonTheme.dimensions.cardPadding),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                shape = CircleShape,
                color = if (isSelected && selectionMode != EntrySelectionMode.MANAGEMENT) AppColors.primary.copy(
                    alpha = 0.2f
                ) else iconColor.copy(alpha = 0.12f),
                modifier = Modifier.size(AeonTheme.dimensions.iconSizeXl)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        modifier = Modifier.size(AeonTheme.dimensions.iconSizeSm),
                        tint = if (isSelected && selectionMode != EntrySelectionMode.MANAGEMENT) AppColors.primary else iconColor
                    )
                }
            }

            Spacer(modifier = Modifier.width(AeonTheme.dimensions.spacingLg))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = label,
                    style = if (isSelected) AppTextStyles.managementItemTitle else AppTextStyles.listItemTitle,
                    color = if (isSelected && selectionMode != EntrySelectionMode.MANAGEMENT) AppColors.primary else AppColors.contentPrimary
                )
                if (detail != null) {
                    Text(
                        text = detail,
                        style = AppTextStyles.caption,
                        color = AppColors.contentSecondary
                    )
                }
            }

            if (trailingContent != null) {
                trailingContent()
            } else {
                when (selectionMode) {
                    EntrySelectionMode.SINGLE -> {
                        if (isSelected) {
                            Icon(
                                imageVector = Icons.Rounded.CheckCircle,
                                contentDescription = "Selected",
                                tint = AppColors.primary,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                    }

                    EntrySelectionMode.MULTI -> {
                        Switch(
                            checked = isSelected,
                            onCheckedChange = {
                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                onClick()
                            },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = AppColors.primary,
                                checkedTrackColor = AppColors.switchTrackOn,
                                checkedBorderColor = AppColors.primary,
                                uncheckedThumbColor = AppColors.switchThumbOff,
                                uncheckedTrackColor = AppColors.switchTrackOff,
                                uncheckedBorderColor = AppColors.outline
                            )
                        )
                    }

                    EntrySelectionMode.MANAGEMENT -> {
                        Icon(
                            imageVector = Icons.Rounded.Edit,
                            contentDescription = "Edit",
                            tint = AppColors.contentSecondary,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun EntrySelectionCardPreview() {
    OssWalletTheme {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            EntrySelectionCard(
                icon = Icons.Rounded.Category,
                label = "Single Selection",
                detail = "This is a detail text",
                isSelected = true,
                onClick = {}
            )
            EntrySelectionCard(
                icon = Icons.Rounded.Category,
                label = "Multi Selection",
                isSelected = false,
                selectionMode = EntrySelectionMode.MULTI,
                onClick = {}
            )
            EntrySelectionCard(
                icon = Icons.Rounded.Category,
                label = "Management Mode",
                isSelected = false,
                selectionMode = EntrySelectionMode.MANAGEMENT,
                onClick = {}
            )
        }
    }
}
