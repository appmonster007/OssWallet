package com.osscabinet.ossWallet.ui.components.management

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AccountBalance
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.osscabinet.ossWallet.ui.theme.AppColors
import com.osscabinet.ossWallet.ui.theme.AppTextStyles
import androidx.compose.material3.Text
import com.osscabinet.ossWallet.ui.theme.OssWalletTheme

/**
 * Thin wrapper over [AeonListItem] preserving the existing ManagementListItem API.
 * All callers (BudgetListItem, LoanItem, PlannedPaymentListItem, etc.) continue to work
 * without changes and automatically gain GlassSurface + AeonTheme.dimensions + animations.
 */
@Composable
fun ManagementListItem(
    onClick: () -> Unit,
    icon: ImageVector,
    iconTint: Color,
    title: String,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    trailingContent: @Composable (ColumnScope.() -> Unit)? = null,
    menuItems: @Composable (ColumnScope.(onDismiss: () -> Unit) -> Unit)? = null,
    isWarning: Boolean = false,
    additionalContent: @Composable (ColumnScope.() -> Unit)? = null
) {
    AeonListItem(
        title = title,
        icon = icon,
        iconTint = iconTint,
        modifier = modifier,
        subtitle = subtitle,
        onClick = onClick,
        isWarning = isWarning,
        trailingContent = trailingContent,
        menuItems = menuItems,
        additionalContent = additionalContent,
    )
}

@Preview(showBackground = true)
@Composable
private fun ManagementListItemPreview() {
    OssWalletTheme {
        Box(modifier = Modifier.padding(16.dp)) {
            ManagementListItem(
                onClick = {},
                icon = Icons.Rounded.AccountBalance,
                iconTint = AppColors.primary,
                title = "Checking Account",
                subtitle = "Bank of America · **** 1234",
                trailingContent = {
                    Text(
                        "\$2,500.00",
                        style = AppTextStyles.managementItemTitle,
                        color = AppColors.primary
                    )
                }
            )
        }
    }
}
