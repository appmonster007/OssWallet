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
 * Standardized row with an icon, label, and switch for entry screens.
 * Following the "Architect Slate" design specification.
 */
@Composable
fun EntrySwitchRow(
    label: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    icon: ImageVector,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    iconContainerColor: Color = AppColors.primary.copy(alpha = 0.12f),
    iconTint: Color = AppColors.primary
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(AeonTheme.dimensions.spacingMd)
    ) {
        Surface(
            shape = CircleShape,
            color = iconContainerColor,
            modifier = Modifier.size(AeonTheme.dimensions.iconSizeXl)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    icon,
                    contentDescription = null,
                    modifier = Modifier.size(AeonTheme.dimensions.iconSizeSm),
                    tint = iconTint
                )
            }
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = label,
                style = AppTextStyles.formSwitchLabel,
                color = AppColors.contentPrimary
            )
            if (subtitle != null) {
                Text(
                    text = subtitle,
                    style = AppTextStyles.caption,
                    color = AppColors.contentSecondary
                )
            }
        }
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedTrackColor = AppColors.primary,
                checkedThumbColor = AppColors.switchThumbOn
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
            }
        }
    }
}
