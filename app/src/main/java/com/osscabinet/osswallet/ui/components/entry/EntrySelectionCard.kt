package com.osscabinet.ossWallet.ui.components.entry

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.osscabinet.ossWallet.ui.theme.AppColors

enum class EntrySelectionMode {
    SINGLE, MULTI, MANAGEMENT
}

/**
 * Standardized selection card for lists in entry screens.
 * Supports single, multi-selection, and management modes with consistent "Architect Slate" styling.
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
        if (isSelected && selectionMode != EntrySelectionMode.MANAGEMENT) AppColors.primary.copy(alpha = 0.08f)
        else AppColors.recessedSurface,
        label = "bgColor"
    )
    
    val borderColor by animateColorAsState(
        if (isSelected && selectionMode != EntrySelectionMode.MANAGEMENT) AppColors.primary
        else AppColors.divider.copy(alpha = 0.2f),
        label = "borderColor"
    )
    
    val borderWidth by animateDpAsState(
        if (isSelected && selectionMode != EntrySelectionMode.MANAGEMENT) 1.5.dp else 0.5.dp,
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
                color = iconColor.copy(alpha = 0.15f),
                modifier = Modifier.size(36.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp),
                        tint = iconColor
                    )
                }
            }
            
            Spacer(modifier = Modifier.width(16.dp))
            
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = label,
                    style = MaterialTheme.typography.bodyLarge.copy(
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.SemiBold
                    ),
                    color = if (isSelected && selectionMode != EntrySelectionMode.MANAGEMENT) AppColors.primary else AppColors.contentPrimary
                )
                if (detail != null) {
                    Text(
                        text = detail,
                        style = MaterialTheme.typography.labelSmall.copy(
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
