package com.osscabinet.ossWallet.ui.components.entry

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

/**
 * Standardized 2-column grid for Account and Category selection.
 * Part of the "Architect Slate" Context tier.
 */
@Composable
fun AeonContextGrid(
    modifier: Modifier = Modifier,
    content: @Composable RowScope.() -> Unit
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        EntryActionRow {
            content()
        }
    }
}
