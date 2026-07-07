package com.osscabinet.ossWallet.ui.components.entry

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Repeat
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.VerticalDivider
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
import com.osscabinet.ossWallet.ui.theme.scaledDp

/**
 * Standardized row with an optional icon, label, and switch for entry screens and settings.
 * Supports a "double action" where the main area is clickable independently of the switch.
 */
@Composable
fun EntrySwitchRow(
    label: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    icon: ImageVector? = null,
    enabled: Boolean = true,
    subtitle: String? = null,
    isFlush: Boolean = false,
    iconContainerColor: Color = AppColors.secondary.copy(alpha = 0.1f),
    iconTint: Color = AppColors.secondary
) {
    val uiScale = AeonTheme.uiScale
    val horizontalPadding = if (isFlush) 0.dp else AeonTheme.dimensions.cardPadding
    val verticalPadding =
        if (isFlush) AeonTheme.dimensions.spacingSm else AeonTheme.dimensions.cardPadding

    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(IntrinsicSize.Min),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Main content area (Icon + Text)
        Row(
            modifier = Modifier
                .weight(1f)
                .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
                .padding(
                    start = horizontalPadding,
                    top = verticalPadding,
                    bottom = verticalPadding,
                    end = scaledDp(12f, uiScale)
                ),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(scaledDp(12f, uiScale))
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
                    color = if (enabled) AppColors.contentPrimary else AppColors.contentPrimary.copy(
                        alpha = 0.4f
                    )
                )
                if (subtitle != null) {
                    Text(
                        text = subtitle,
                        style = AppTextStyles.caption,
                        color = AppColors.contentSecondary.copy(alpha = if (enabled) 0.6f else 0.4f)
                    )
                }
            }
        }

        // Divider (only if double action)
        if (onClick != null) {
            VerticalDivider(
                modifier = Modifier
                    .fillMaxHeight()
                    .padding(vertical = 12.dp)
                    .width(0.5.dp),
                color = AppColors.divider.copy(alpha = 0.3f)
            )
        }

        // Switch area
        Box(
            modifier = Modifier
                .padding(start = scaledDp(12f, uiScale), end = horizontalPadding)
                .padding(vertical = verticalPadding),
            contentAlignment = Alignment.Center
        ) {
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
                    subtitle = "Automatically create this transaction every month",
                    onClick = {}
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
