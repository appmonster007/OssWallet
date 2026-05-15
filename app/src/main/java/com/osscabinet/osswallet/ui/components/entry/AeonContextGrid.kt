package com.osscabinet.ossWallet.ui.components.entry

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.osscabinet.ossWallet.ui.theme.AeonTheme

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
