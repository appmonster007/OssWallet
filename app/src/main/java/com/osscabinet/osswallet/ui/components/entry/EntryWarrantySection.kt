package com.osscabinet.ossWallet.ui.components.entry

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CalendarMonth
import androidx.compose.material.icons.rounded.Security
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.osscabinet.ossWallet.ui.theme.OssWalletTheme
import com.osscabinet.ossWallet.ui.theme.ThemePreviews

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

@ThemePreviews
@Composable
fun EntryWarrantySectionPreview() {
    OssWalletTheme {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            EntryWarrantySection(
                enabled = false,
                onToggle = {},
                expirationMillis = System.currentTimeMillis(),
                onDateClick = {}
            )
            EntryWarrantySection(
                enabled = true,
                onToggle = {},
                expirationMillis = System.currentTimeMillis() + 31536000000L,
                onDateClick = {}
            )
        }
    }
}
