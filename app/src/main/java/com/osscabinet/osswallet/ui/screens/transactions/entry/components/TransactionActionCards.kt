package com.osscabinet.ossWallet.ui.screens.transactions.entry.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.osscabinet.ossWallet.ui.components.entry.EntryActionCard
import com.osscabinet.ossWallet.ui.screens.transactions.entry.TransactionEntryEvent
import com.osscabinet.ossWallet.ui.screens.transactions.entry.TransactionEntryState

/**
 * Row of slim action cards for account, category, target account selection.
 * Following the "Architect Slate" design specification.
 */
@Composable
fun TransactionActionCards(
    state: TransactionEntryState,
    onEvent: (TransactionEntryEvent) -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // First Row: Template and Date (Side by side)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (!state.isEditMode) {
                EntryActionCard(
                    icon = Icons.Rounded.Bookmark,
                    label = "TEMPLATE",
                    value = state.selectedTemplateName ?: "None",
                    onClick = { onEvent(TransactionEntryEvent.ShowTemplateSheet(true)) },
                    modifier = Modifier.weight(1f),
                    iconContainerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
                    iconTint = MaterialTheme.colorScheme.primary
                )
            }

            EntryActionCard(
                icon = Icons.Rounded.CalendarToday,
                label = "DATE",
                value = formatDate(state.selectedDateMillis),
                onClick = { onEvent(TransactionEntryEvent.ShowDatePicker(true)) },
                modifier = Modifier.weight(1f),
                iconContainerColor = MaterialTheme.colorScheme.secondary.copy(alpha = 0.1f),
                iconTint = MaterialTheme.colorScheme.secondary
            )
        }

        // Second Row: Account and Category/Target Account (Side by side)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            EntryActionCard(
                icon = Icons.Rounded.AccountBalance,
                label = if (state.isTransfer) "FROM" else "ACCOUNT",
                value = state.selectedAccount?.name ?: "Select",
                onClick = { onEvent(TransactionEntryEvent.ShowAccountSheet(true)) },
                modifier = Modifier.weight(1f),
                iconContainerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
                iconTint = MaterialTheme.colorScheme.primary
            )

            if (state.isTransfer) {
                val targetDisplay = when {
                    state.targetAccount != null -> state.targetAccount.name
                    else -> "Outside Wallet"
                }
                EntryActionCard(
                    icon = Icons.Rounded.AccountBalance,
                    label = "TO",
                    value = targetDisplay,
                    onClick = { onEvent(TransactionEntryEvent.ShowTargetAccountSheet(true)) },
                    modifier = Modifier.weight(1f),
                    iconContainerColor = MaterialTheme.colorScheme.tertiary.copy(alpha = 0.1f),
                    iconTint = MaterialTheme.colorScheme.tertiary
                )
            } else {
                val categoryObj = state.categories.find { it.id == state.selectedCategoryId }
                val displayCategoryName = categoryObj?.name ?: "Unknown"

                val categoryIcon = com.osscabinet.ossWallet.ui.components.categoryIcons[displayCategoryName] ?: Icons.Rounded.Category

                EntryActionCard(
                    icon = categoryIcon,
                    label = "CATEGORY",
                    value = displayCategoryName,
                    onClick = { onEvent(TransactionEntryEvent.ShowCategorySheet(true)) },
                    modifier = Modifier.weight(1f),
                    iconContainerColor = MaterialTheme.colorScheme.tertiary.copy(alpha = 0.1f),
                    iconTint = MaterialTheme.colorScheme.tertiary
                )
            }
        }
    }
}


private fun formatDate(millis: Long): String {
    val formatter = java.text.SimpleDateFormat("MMM dd, yyyy", java.util.Locale.getDefault())
    return formatter.format(java.util.Date(millis))
}
