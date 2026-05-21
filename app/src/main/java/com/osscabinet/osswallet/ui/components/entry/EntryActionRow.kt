package com.osscabinet.ossWallet.ui.components.entry

import androidx.compose.foundation.layout.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.osscabinet.ossWallet.ui.theme.AeonTheme
import com.osscabinet.ossWallet.ui.theme.OssWalletTheme
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.Category
import com.osscabinet.ossWallet.ui.components.AeonEntryActionCard
import com.osscabinet.ossWallet.ui.theme.ThemePreviews

/**
 * Standardized row for side-by-side action cards (e.g. Account & Category).
 * Part of the "Architect Slate" Context tier.
 */
@Composable
fun EntryActionRow(
    modifier: Modifier = Modifier,
    content: @Composable RowScope.() -> Unit
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(AeonTheme.dimensions.spacingMd),
        verticalAlignment = Alignment.CenterVertically,
        content = content
    )
}

@ThemePreviews
@Composable
fun EntryActionRowPreview() {
    OssWalletTheme {
        EntryActionRow(modifier = Modifier.padding(16.dp)) {
            AeonEntryActionCard(
                icon = Icons.Default.AccountBalance,
                label = "ACCOUNT",
                value = "Savings",
                onClick = {},
                modifier = Modifier.weight(1f)
            )
            AeonEntryActionCard(
                icon = Icons.Default.Category,
                label = "CATEGORY",
                value = "Food",
                onClick = {},
                modifier = Modifier.weight(1f)
            )
        }
    }
}
