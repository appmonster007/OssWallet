package com.osscabinet.ossWallet.ui.components.entry

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CalendarMonth
import androidx.compose.material.icons.rounded.Security
import androidx.compose.runtime.Composable

/**
 * A shared section for Warranty protection settings.
 */
@Composable
fun EntryWarrantySection(
    enabled: Boolean,
    onToggle: (Boolean) -> Unit,
    expirationMillis: Long,
    onDateClick: () -> Unit
) {
    EntrySwitchRow(
        label = "Warranty Protection",
        checked = enabled,
        onCheckedChange = onToggle,
        icon = Icons.Rounded.Security
    )
    if (enabled) {
        EntryDateCard(
            icon = Icons.Rounded.CalendarMonth,
            label = "EXPIRATION DATE",
            dateMillis = expirationMillis,
            onClick = onDateClick
        )
    }
}
