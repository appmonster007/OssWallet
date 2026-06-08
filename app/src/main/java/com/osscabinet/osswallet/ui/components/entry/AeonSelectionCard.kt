package com.osscabinet.ossWallet.ui.components.entry

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
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
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.osscabinet.ossWallet.ui.components.AeonSelectMode
import com.osscabinet.ossWallet.ui.components.GlassSurface
import com.osscabinet.ossWallet.ui.theme.AeonTheme
import com.osscabinet.ossWallet.ui.theme.AppColors
import com.osscabinet.ossWallet.ui.theme.AppTextStyles
import com.osscabinet.ossWallet.ui.theme.GlassTokens
import com.osscabinet.ossWallet.ui.theme.GlassVariant
import com.osscabinet.ossWallet.ui.theme.OssWalletTheme

/**
 * Standardized selection card for lists in Aeon design system.
 * Supports single, multi-selection, and management modes with glassmorphism styling.
 */
@Composable
fun AeonSelectionCard(
    icon: ImageVector,
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    detail: String? = null,
    iconColor: Color = AppColors.primary,
    selectionMode: AeonSelectMode = AeonSelectMode.SINGLE,
    trailingContent: (@Composable RowScope.() -> Unit)? = null,
) {
    val haptic = LocalHapticFeedback.current
    val dims = AeonTheme.dimensions
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.98f else 1f,
        label = "scale"
    )

    val isActiveSelection = (isSelected && selectionMode != AeonSelectMode.MANAGEMENT)

    val backgroundColor by animateColorAsState(
        targetValue = if (isActiveSelection) iconColor.copy(alpha = 0.15f)
        else AppColors.surfaceContainerLow,
        label = "bgColor"
    )

    val borderColor by animateColorAsState(
        targetValue = if (isActiveSelection) iconColor
        else AppColors.outlineVariant.copy(alpha = 0.6f),
        label = "borderColor"
    )

    val borderWidth by animateDpAsState(
        targetValue = if (isActiveSelection) 1.5.dp else 1.dp,
        label = "borderWidth"
    )

    val contentAlpha = if (isActiveSelection) 1f else 0.9f

    GlassSurface(
        modifier = modifier
            .fillMaxWidth()
            .scale(scale)
            .clickable(
                interactionSource = interactionSource,
                indication = LocalIndication.current,
            ) {
                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                onClick()
            },
        shape = RoundedCornerShape(dims.componentCornerRadius),
        hazeStyle = GlassTokens.itemStyle(),
        glassVariant = GlassVariant.Item,
        fallbackColor = backgroundColor,
        borderColor = borderColor,
        borderWidth = borderWidth,

        shadowElevation = if (isActiveSelection) 2.dp else 0.dp,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
//                .defaultMinSize(minHeight = 72.dp)
                .padding(horizontal = dims.cardPadding, vertical = dims.spacingMd),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            // Icon Container
            Surface(
                shape = CircleShape,
                color = iconColor.copy(alpha = if (isActiveSelection) 0.25f else 0.12f),
                modifier = Modifier.size(dims.iconSizeLg)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        modifier = Modifier.size(dims.iconSizeMd),
                        tint = iconColor
                    )
                }
            }

            Spacer(modifier = Modifier.width(dims.spacingLg))

            // Label & Detail
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                Text(
                    text = label,
                    style = AppTextStyles.listItemTitle,
                    color = if (isActiveSelection) iconColor else AppColors.contentPrimary,
                    modifier = Modifier.alpha(contentAlpha),
                    maxLines = 1,
                    overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                )
                detail?.let {
                    Text(
                        text = it,
                        style = AppTextStyles.listItemSubtitle,
                        color = AppColors.contentSecondary,
                        modifier = Modifier.alpha(if (isActiveSelection) 0.95f else 0.75f),
                        maxLines = 2,
                        overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                    )
                }
            }

            Spacer(modifier = Modifier.width(dims.spacingMd))

            // Trailing Content
            if (trailingContent != null) {
                trailingContent()
            } else {
                Box(
                    modifier = Modifier.defaultMinSize(minWidth = dims.iconSizeMd),
                    contentAlignment = Alignment.Center
                ) {
                    when (selectionMode) {
                        AeonSelectMode.SINGLE, AeonSelectMode.PARENT -> {
                            if (isSelected) {
                                Icon(
                                    imageVector = Icons.Rounded.CheckCircle,
                                    contentDescription = "Selected",
                                    tint = iconColor,
                                    modifier = Modifier.size(dims.iconSizeMd)
                                )
                            }
                        }

                        AeonSelectMode.MULTI -> {
                            Switch(
                                checked = isSelected,
                                onCheckedChange = {
                                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                    onClick()
                                },
                                colors = SwitchDefaults.colors(
                                    checkedThumbColor = iconColor,
                                    checkedTrackColor = iconColor.copy(alpha = 0.3f),
                                    checkedBorderColor = iconColor,
                                    uncheckedThumbColor = AppColors.switchThumbOff,
                                    uncheckedTrackColor = AppColors.switchTrackOff,
                                    uncheckedBorderColor = AppColors.outline
                                )
                            )
                        }

                        AeonSelectMode.MANAGEMENT -> {
                            Icon(
                                imageVector = Icons.Rounded.Edit,
                                contentDescription = "Edit $label",
                                tint = AppColors.contentSecondary,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun AeonSelectionListPreview() {
    OssWalletTheme {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                "TAG SELECTION LIST",
                style = AppTextStyles.sectionOverline,
                color = AppColors.primary,
                modifier = Modifier.padding(bottom = 4.dp)
            )
            
            val tags = listOf(
                Triple("Work", "Professional tasks and business meetings", Color(0xFF2196F3)),
                Triple("Personal", "Private matters and home life", Color(0xFF4CAF50)),
                Triple("Urgent", "High priority - needs immediate attention", Color(0xFFF44336)),
                Triple("Later", "Low priority tasks for next week", Color(0xFFFF9800))
            )

            tags.forEachIndexed { index, (label, detail, color) ->
                AeonSelectionCard(
                    icon = Icons.Rounded.Category,
                    label = label,
                    detail = detail,
                    isSelected = index == 0,
                    iconColor = color,
                    onClick = {}
                )
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun AeonSelectionCardPreview() {
    OssWalletTheme {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text("SINGLE SELECTION", style = AppTextStyles.sectionOverline, color = AppColors.primary)
            AeonSelectionCard(
                icon = Icons.Rounded.Category,
                label = "Active Item",
                detail = "Currently selected item state",
                isSelected = true,
                onClick = {}
            )
            AeonSelectionCard(
                icon = Icons.Rounded.Category,
                label = "Inactive Item",
                detail = "Standard unselected item state",
                isSelected = false,
                onClick = {}
            )

            Spacer(modifier = Modifier.size(16.dp))
            Text("MULTI SELECTION", style = AppTextStyles.sectionOverline, color = AppColors.primary)
            AeonSelectionCard(
                icon = Icons.Rounded.Category,
                label = "Multi-Select Item",
                detail = "Uses a switch for clarity in long lists",
                isSelected = true,
                selectionMode = AeonSelectMode.MULTI,
                onClick = {}
            )

            Spacer(modifier = Modifier.size(16.dp))
            Text("PARENT SELECTION", style = AppTextStyles.sectionOverline, color = AppColors.primary)
            AeonSelectionCard(
                icon = Icons.Rounded.Category,
                label = "Parent Category",
                detail = "Selection in hierarchical lists",
                isSelected = true,
                selectionMode = AeonSelectMode.PARENT,
                onClick = {}
            )

            Spacer(modifier = Modifier.size(16.dp))
            Text("MANAGEMENT MODE", style = AppTextStyles.sectionOverline, color = AppColors.primary)
            AeonSelectionCard(
                icon = Icons.Rounded.Category,
                label = "Editable Category",
                detail = "Click to open editor for this item",
                isSelected = false,
                selectionMode = AeonSelectMode.MANAGEMENT,
                onClick = {}
            )
        }
    }
}
