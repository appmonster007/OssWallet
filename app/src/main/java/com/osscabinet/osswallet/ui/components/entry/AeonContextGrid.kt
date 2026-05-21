package com.osscabinet.ossWallet.ui.components.entry

import androidx.compose.foundation.layout.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.osscabinet.ossWallet.ui.theme.AeonTheme
import com.osscabinet.ossWallet.ui.theme.OssWalletTheme
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.Category
import com.osscabinet.ossWallet.ui.components.AeonEntryActionCard
import com.osscabinet.ossWallet.ui.theme.ThemePreviews

/**
 * Standardized grid for Account, Category, Date, etc selection.
 * Part of the "Architect Slate" Context tier.
 * Contains rows of EntryActionCard.
 */
@Composable
fun AeonContextGrid(
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(AeonTheme.dimensions.spacingMd),
        content = content
    )
}

@ThemePreviews
@Composable
fun AeonContextGridPreview() {
    OssWalletTheme {
        AeonContextGrid(modifier = Modifier.padding(16.dp)) {
            EntryActionRow {
                AeonEntryActionCard(
                    icon = Icons.Default.AccountBalance,
                    label = "ACCOUNT",
                    value = "Main",
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
}
