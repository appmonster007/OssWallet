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
import com.osscabinet.ossWallet.ui.theme.AppTextStyles
import androidx.compose.ui.tooling.preview.Preview
import com.osscabinet.ossWallet.ui.theme.OssWalletTheme
import androidx.compose.material.icons.rounded.Category

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
        targetValue = if (isSelected) AppColors.primary.copy(alpha = 0.08f) else AppColors.recessedSurface,
        label = "bg"
    )
    
    val borderColor by animateColorAsState(
        targetValue = if (isSelected) AppColors.primary.copy(alpha = 0.3f) else AppColors.divider.copy(alpha = 0.1f),
        label = "border"
    )

    val elevation by animateDpAsState(
        targetValue = if (isSelected) 2.dp else 0.dp,
        label = "elev"
    )

    Surface(
        onClick = {
            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
            onClick()
        },
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        color = backgroundColor,
        border = BorderStroke(0.5.dp, borderColor),
        tonalElevation = elevation
    ) {
        Row(
            modifier = Modifier
                .padding(12.dp)
                .heightIn(min = 40.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Surface(
                shape = CircleShape,
                color = if (isSelected) AppColors.primary.copy(alpha = 0.15f) else iconColor.copy(alpha = 0.1f),
                modifier = Modifier.size(44.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = if (isSelected && selectionMode != EntrySelectionMode.MANAGEMENT) Icons.Rounded.CheckCircle else icon,
                        contentDescription = null,
                        modifier = Modifier.size(22.dp),
                        tint = if (isSelected) AppColors.primary else iconColor
                    )
                }
            }
            
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = label,
                    style = AppTextStyles.listItemTitle.copy(
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                    ),
                    color = if (isSelected) AppColors.primary else AppColors.contentPrimary
                )
                if (detail != null) {
                    Text(
                        text = detail,
                        style = AppTextStyles.metaLabel,
                        color = AppColors.contentSecondary.copy(alpha = 0.6f)
                    )
                }
            }

            if (trailingContent != null) {
                trailingContent()
            } else if (selectionMode == EntrySelectionMode.MANAGEMENT) {
                Icon(
                    imageVector = Icons.Rounded.Edit,
                    contentDescription = "Edit",
                    modifier = Modifier.size(18.dp),
                    tint = AppColors.contentSecondary.copy(alpha = 0.4f)
                )
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun EntrySelectionCardPreview() {
    OssWalletTheme {
        Surface(modifier = Modifier.padding(16.dp)) {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                EntrySelectionCard(
                    icon = Icons.Rounded.Category,
                    label = "Unselected Card",
                    isSelected = false,
                    onClick = {}
                )
                EntrySelectionCard(
                    icon = Icons.Rounded.Category,
                    label = "Selected Card",
                    isSelected = true,
                    onClick = {}
                )
                EntrySelectionCard(
                    icon = Icons.Rounded.Category,
                    label = "Management Mode",
                    isSelected = false,
                    onClick = {},
                    selectionMode = EntrySelectionMode.MANAGEMENT
                )
            }
        }
    }
}
