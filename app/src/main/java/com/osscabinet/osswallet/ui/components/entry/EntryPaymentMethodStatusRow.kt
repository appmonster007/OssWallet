package com.osscabinet.ossWallet.ui.components.entry

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Contactless
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp

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
