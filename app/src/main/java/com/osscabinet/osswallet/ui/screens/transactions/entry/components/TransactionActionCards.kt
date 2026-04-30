package com.osscabinet.ossWallet.ui.screens.transactions.entry.components

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.osscabinet.ossWallet.ui.components.categoryIcons
import com.osscabinet.ossWallet.ui.screens.transactions.entry.TransactionEntryEvent
import com.osscabinet.ossWallet.ui.screens.transactions.entry.TransactionEntryState

/**
 * Row of slim action cards for account, category, target account selection.
 */
@Composable
fun TransactionActionCards(
    state: TransactionEntryState,
    onEvent: (TransactionEntryEvent) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        // Template selector (only for new transactions)
        if (!state.isEditMode) {
            ActionCard(
                icon = Icons.Rounded.Bookmark,
                label = "TEMPLATE",
                value = state.selectedTemplateName ?: "None",
                onClick = { onEvent(TransactionEntryEvent.ShowTemplateSheet(true)) },
                iconContainerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
            )
        }

        // Date selector
        ActionCard(
            icon = Icons.Rounded.CalendarToday,
            label = "DATE",
            value = formatDate(state.selectedDateMillis),
            onClick = { onEvent(TransactionEntryEvent.ShowDatePicker(true)) },
            iconContainerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.5f)
        )

        // Account and Category/Target Account in same row
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            // Account selector
            ActionCard(
                icon = Icons.Rounded.AccountBalance,
                label = if (state.isTransfer) "FROM" else "ACCOUNT",
                value = state.selectedAccount?.name ?: "Select",
                onClick = { onEvent(TransactionEntryEvent.ShowAccountSheet(true)) },
                modifier = Modifier.weight(1f),
                iconContainerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
            )

            // Target account (transfer) or Category (expense/income)
            if (state.isTransfer) {
                val targetDisplay = when {
                    state.targetAccount != null -> state.targetAccount.name
                    else -> "Outside Wallet"
                }
                ActionCard(
                    icon = Icons.Rounded.AccountBalance,
                    label = "TO",
                    value = targetDisplay,
                    onClick = { onEvent(TransactionEntryEvent.ShowTargetAccountSheet(true)) },
                    modifier = Modifier.weight(1f),
                    iconContainerColor = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.5f)
                )
            } else {
                val displayCategory = if (state.isEditMode) {
                    state.categories.find { it.id == state.selectedCategoryId }?.name ?: state.category
                } else state.category

                // Use categoryIcons map like original, fallback to Category icon
                val categoryIcon = categoryIcons[displayCategory] ?: Icons.Rounded.Category

                ActionCard(
                    icon = categoryIcon,
                    label = "CATEGORY",
                    value = displayCategory,
                    onClick = { onEvent(TransactionEntryEvent.ShowCategorySheet(true)) },
                    modifier = Modifier.weight(1f),
                    iconContainerColor = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.5f)
                )
            }
        }
    }
}

@Composable
private fun ActionCard(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    value: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    iconContainerColor: androidx.compose.ui.graphics.Color = MaterialTheme.colorScheme.surface
) {
    Surface(
        onClick = onClick,
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.surface,
        modifier = modifier.height(56.dp)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp),
            verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
        ) {
            Surface(
                shape = androidx.compose.foundation.shape.CircleShape,
                color = iconContainerColor,
                modifier = Modifier.size(32.dp)
            ) {
                Box(contentAlignment = androidx.compose.ui.Alignment.Center) {
                    Icon(icon, contentDescription = null, modifier = Modifier.size(16.dp))
                }
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column {
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = value,
                    style = MaterialTheme.typography.bodyMedium,
                    maxLines = 1
                )
            }
        }
    }
}

private fun formatDate(millis: Long): String {
    val formatter = java.text.SimpleDateFormat("MMM dd, yyyy", java.util.Locale.getDefault())
    return formatter.format(java.util.Date(millis))
}
