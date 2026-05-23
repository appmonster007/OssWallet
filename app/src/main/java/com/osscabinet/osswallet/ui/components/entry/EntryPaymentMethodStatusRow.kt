package com.osscabinet.ossWallet.ui.components.entry

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Contactless
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.osscabinet.ossWallet.ui.theme.OssWalletTheme

/**
 * A shared row for Payment Method and Status selection.
 */
@Composable
fun EntryPaymentMethodStatusRow(
    paymentMethod: String,
    onPaymentMethodClick: () -> Unit,
    statusLabel: String,
    onStatusClick: () -> Unit,
    paymentMethodIcon: ImageVector = Icons.Rounded.Contactless,
    statusIcon: ImageVector = Icons.Rounded.CheckCircle,
    statusIconTint: androidx.compose.ui.graphics.Color? = null,
    statusContainerColor: androidx.compose.ui.graphics.Color? = null
) {
    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        EntryActionChip(
            icon = paymentMethodIcon,
            label = paymentMethod,
            onClick = onPaymentMethodClick,
            modifier = Modifier.weight(1f)
        )
        EntryActionChip(
            icon = statusIcon,
            label = statusLabel,
            onClick = onStatusClick,
            modifier = Modifier.weight(1f),
            iconTint = statusIconTint,
            iconContainerColor = statusContainerColor
        )
    }
}

@Preview(showBackground = true)
@Composable
fun EntryPaymentMethodStatusRowPreview() {
    OssWalletTheme {
        Surface(modifier = Modifier.padding(16.dp)) {
            EntryPaymentMethodStatusRow(
                paymentMethod = "Cash",
                onPaymentMethodClick = {},
                statusLabel = "Cleared",
                onStatusClick = {}
            )
        }
    }
}

