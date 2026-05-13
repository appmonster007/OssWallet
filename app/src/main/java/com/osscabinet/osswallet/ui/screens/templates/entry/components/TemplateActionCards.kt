package com.osscabinet.ossWallet.ui.screens.templates.entry.components

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.osscabinet.ossWallet.ui.components.getAccountIconByName
import com.osscabinet.ossWallet.ui.components.getIconByName
import com.osscabinet.ossWallet.ui.components.entry.*
import com.osscabinet.ossWallet.ui.screens.templates.entry.TemplateEntryEvent
import com.osscabinet.ossWallet.ui.screens.templates.entry.TemplateEntryState

@Composable
fun TemplateActionCards(
    state: TemplateEntryState,
    onEvent: (TemplateEntryEvent) -> Unit
) {
    AeonContextGrid {
        val accountIcon = getAccountIconByName(state.selectedAccount?.icon)
        EntryActionCard(
            icon = accountIcon,
            label = if (state.transactionType == 2) "FROM" else "ACCOUNT",
            value = state.selectedAccount?.name ?: "Select",
            onClick = { onEvent(TemplateEntryEvent.ShowAccountSheet(true)) },
            modifier = Modifier.weight(1f),
            iconContainerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
            iconTint = MaterialTheme.colorScheme.primary
        )

        if (state.transactionType == 2) {
            val targetDisplay = state.selectedTargetAccount?.name ?: "Select"
            val targetIcon = getAccountIconByName(state.selectedTargetAccount?.icon)
            EntryActionCard(
                icon = targetIcon,
                label = "TO",
                value = targetDisplay,
                onClick = { onEvent(TemplateEntryEvent.ShowTargetAccountSheet(true)) },
                modifier = Modifier.weight(1f),
                iconContainerColor = MaterialTheme.colorScheme.tertiary.copy(alpha = 0.1f),
                iconTint = MaterialTheme.colorScheme.tertiary
            )
        } else {
            val categoryIcon = getIconByName(state.selectedCategory?.iconName)
            EntryActionCard(
                icon = categoryIcon,
                label = "CATEGORY",
                value = state.selectedCategory?.name ?: "Unknown",
                onClick = { onEvent(TemplateEntryEvent.ShowCategorySheet(true)) },
                modifier = Modifier.weight(1f),
                iconContainerColor = MaterialTheme.colorScheme.tertiary.copy(alpha = 0.1f),
                iconTint = MaterialTheme.colorScheme.tertiary
            )
        }
    }
}
