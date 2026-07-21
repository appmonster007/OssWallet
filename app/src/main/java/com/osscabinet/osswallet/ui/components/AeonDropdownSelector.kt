package com.osscabinet.ossWallet.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material.icons.rounded.KeyboardArrowUp
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.MenuItemColors
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.osscabinet.ossWallet.ui.theme.AeonTheme
import com.osscabinet.ossWallet.ui.theme.AppColors
import com.osscabinet.ossWallet.ui.theme.AppTextStyles

@Composable
fun <T> AeonDropdownSelector(
    value: T,
    options: List<T>,
    labelProvider: (T) -> String,
    onValueChange: (T) -> Unit,
    modifier: Modifier = Modifier
) {
    var expanded by remember { mutableStateOf(false) }
    val haptic = LocalHapticFeedback.current

    Box(modifier = modifier) {
        Surface(
            modifier = Modifier.clickable {
                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                expanded = true
            },
            shape = RoundedCornerShape(8.dp),
            color = AppColors.contentPrimary.copy(alpha = 0.05f)
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = labelProvider(value),
                    style = AppTextStyles.chipLabelBold.copy(
                        fontSize = 10.sp,
                        letterSpacing = 0.5.sp
                    ),
                    color = AppColors.primary
                )
                Icon(
                    imageVector = if (expanded) Icons.Rounded.KeyboardArrowUp else Icons.Rounded.KeyboardArrowDown,
                    contentDescription = null,
                    modifier = Modifier.size(14.dp),
                    tint = AppColors.primary
                )
            }
        }

        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            containerColor = AppColors.surfaceContainerHigh,
            shape = RoundedCornerShape(AeonTheme.dimensions.componentCornerRadius),
            modifier = Modifier.widthIn(min = 100.dp)
        ) {
            options.forEach { option ->
                val isSelected = option == value
                DropdownMenuItem(
                    modifier = Modifier.fillMaxWidth(),
                    text = {
                        Text(
                            text = labelProvider(option),
                            style = if (isSelected) AppTextStyles.chipLabelBold else AppTextStyles.chipLabel,
                            color = if (isSelected) AppColors.primary else AppColors.contentPrimary
                        )
                    },
                    onClick = {
                        onValueChange(option)
                        expanded = false
                    },
                    colors = MenuItemColors(
                        textColor = if (isSelected) AppColors.primary else AppColors.contentPrimary,
                        leadingIconColor = AppColors.contentSecondary,
                        trailingIconColor = AppColors.primary,
                        disabledTextColor = AppColors.contentSecondary.copy(alpha = 0.38f),
                        disabledLeadingIconColor = AppColors.contentSecondary.copy(alpha = 0.38f),
                        disabledTrailingIconColor = AppColors.contentSecondary.copy(alpha = 0.38f)
                    ),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp)
                )
            }
        }
    }
}
