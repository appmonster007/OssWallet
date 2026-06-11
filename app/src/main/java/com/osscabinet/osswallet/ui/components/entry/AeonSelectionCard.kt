package com.osscabinet.ossWallet.ui.components.entry

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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Category
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material3.Icon
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.osscabinet.ossWallet.ui.components.AeonSelectMode
import com.osscabinet.ossWallet.ui.components.management.AeonListItem
import com.osscabinet.ossWallet.ui.theme.AeonTheme
import com.osscabinet.ossWallet.ui.theme.AppColors
import com.osscabinet.ossWallet.ui.theme.AppTextStyles
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

    AeonListItem(
        title = label,
        icon = icon,
        iconTint = iconColor,
        subtitle = detail,
        isSelected = isSelected && selectionMode != AeonSelectMode.MANAGEMENT,
        onClick = onClick,
        modifier = modifier,
        trailingContent = {
            if (trailingContent != null) {
                Row { trailingContent() }
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
                                modifier = Modifier.size(dims.iconSizeMd)
                            )
                        }
                    }
                }
            }
        }
    )
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
