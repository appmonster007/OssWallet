package com.osscabinet.ossWallet.ui.components.entry

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
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
import com.osscabinet.ossWallet.ui.theme.AppColors
import com.osscabinet.ossWallet.ui.theme.AppTextStyles
import com.osscabinet.ossWallet.ui.theme.OssWalletTheme

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
        targetValue = if (isSelected && (selectionMode != EntrySelectionMode.MANAGEMENT)) AppColors.primary.copy(alpha = 0.08f)
        else AppColors.recessedSurface,
        label = "bgColor"
    )
    
    val borderColor by animateColorAsState(
        targetValue = if (isSelected && (selectionMode != EntrySelectionMode.MANAGEMENT)) AppColors.primary
        else AppColors.divider.copy(alpha = 0.2f),
        label = "borderColor"
    )
    
    val borderWidth by animateDpAsState(
        targetValue = if (isSelected && (selectionMode != EntrySelectionMode.MANAGEMENT)) 1.5.dp else 0.5.dp,
        label = "borderWidth"
    )

    Surface(
        onClick = {
            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
            onClick()
        },
        shape = RoundedCornerShape(24.dp),
        color = backgroundColor,
        border = BorderStroke(borderWidth, borderColor),
        modifier = modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                shape = CircleShape,
                color = if (isSelected && selectionMode != EntrySelectionMode.MANAGEMENT) AppColors.primary.copy(alpha = 0.15f) else iconColor.copy(alpha = 0.1f),
                modifier = Modifier.size(36.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp),
                        tint = if (isSelected && selectionMode != EntrySelectionMode.MANAGEMENT) AppColors.primary else iconColor
                    )
                }
            }
            
            Spacer(modifier = Modifier.width(16.dp))
            
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = label,
                    style = AppTextStyles.listItemTitle.copy(
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.SemiBold
                    ),
                    color = if (isSelected && selectionMode != EntrySelectionMode.MANAGEMENT) AppColors.primary else AppColors.contentPrimary
                )
                if (detail != null) {
                    Text(
                        text = detail,
                        style = AppTextStyles.transactionSwipeAction.copy(
                            fontSize = 11.sp,
                            color = AppColors.contentSecondary.copy(alpha = 0.7f)
                        )
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
                                checkedThumbColor = AppColors.switchThumbOn,
                                checkedTrackColor = AppColors.primary
                            )
                        )
                    }
                    EntrySelectionMode.MANAGEMENT -> {
                        Icon(
                            imageVector = Icons.Rounded.Edit,
                            contentDescription = "Edit",
                            tint = AppColors.border,
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
