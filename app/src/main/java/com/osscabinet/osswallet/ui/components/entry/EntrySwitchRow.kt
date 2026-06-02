package com.osscabinet.ossWallet.ui.components.entry

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Repeat
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.osscabinet.ossWallet.ui.theme.AeonTheme
import com.osscabinet.ossWallet.ui.theme.AppColors
import com.osscabinet.ossWallet.ui.theme.AppTextStyles
import com.osscabinet.ossWallet.ui.theme.OssWalletTheme

/**
 * Standardized row with an optional icon, label, and switch for entry screens and settings.
 * Following the "Architect Slate" design specification.
 */
@Composable
fun EntrySwitchRow(
    label: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    enabled: Boolean = true,
    subtitle: String? = null,
    isFlush: Boolean = false,
    iconContainerColor: Color = AppColors.secondary.copy(alpha = 0.1f),
    iconTint: Color = AppColors.secondary
) {
    val horizontalPadding = if (isFlush) 0.dp else AeonTheme.dimensions.cardPadding
    val verticalPadding = if (isFlush) AeonTheme.dimensions.spacingSm else AeonTheme.dimensions.cardPadding

    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = horizontalPadding, vertical = verticalPadding),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        if (icon != null) {
            Surface(
                shape = CircleShape,
                color = if (enabled) iconContainerColor else iconContainerColor.copy(alpha = 0.05f),
                modifier = Modifier.size(32.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        icon,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                        tint = if (enabled) iconTint else iconTint.copy(alpha = 0.4f)
                    )
                }
            }
        }
        
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = label,
                style = AppTextStyles.transactionTitle,
                color = if (enabled) AppColors.contentPrimary else AppColors.contentPrimary.copy(alpha = 0.4f)
            )
            if (subtitle != null) {
                Text(
                    text = subtitle,
                    style = AppTextStyles.caption,
                    color = AppColors.contentSecondary.copy(alpha = if (enabled) 0.6f else 0.4f)
                )
            }
        }

        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            enabled = enabled,
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
}

@Preview(showBackground = true)
@Composable
fun EntrySwitchRowPreview() {
    OssWalletTheme {
        Surface(modifier = Modifier.padding(16.dp)) {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                EntrySwitchRow(
                    label = "Repeat Monthly",
                    checked = true,
                    onCheckedChange = {},
                    icon = Icons.Rounded.Repeat,
                    subtitle = "Automatically create this transaction every month"
                )
                EntrySwitchRow(
                    label = "Notify me",
                    checked = false,
                    onCheckedChange = {},
                    icon = Icons.Rounded.Repeat
                )
                EntrySwitchRow(
                    label = "No Icon Toggle",
                    checked = true,
                    onCheckedChange = {},
                    subtitle = "This toggle doesn't have an icon"
                )
            }
        }
    }
}
