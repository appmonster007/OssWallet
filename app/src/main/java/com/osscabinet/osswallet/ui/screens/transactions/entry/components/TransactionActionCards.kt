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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.osscabinet.ossWallet.ui.components.categoryIcons
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
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        // Template selector (only for new transactions)
        if (!state.isEditMode) {
            ActionCard(
                icon = Icons.Rounded.Bookmark,
                label = "TEMPLATE",
                value = state.selectedTemplateName ?: "None",
                onClick = { onEvent(TransactionEntryEvent.ShowTemplateSheet(true)) },
                iconContainerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
                iconTint = MaterialTheme.colorScheme.primary
            )
        }

        // Date selector
        ActionCard(
            icon = Icons.Rounded.CalendarToday,
            label = "DATE",
            value = formatDate(state.selectedDateMillis),
            onClick = { onEvent(TransactionEntryEvent.ShowDatePicker(true)) },
            iconContainerColor = MaterialTheme.colorScheme.secondary.copy(alpha = 0.1f),
            iconTint = MaterialTheme.colorScheme.secondary
        )

        // Account and Category/Target Account in same row
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            // Account selector
            ActionCard(
                icon = Icons.Rounded.AccountBalance,
                label = if (state.isTransfer) "FROM" else "ACCOUNT",
                value = state.selectedAccount?.name ?: "Select",
                onClick = { onEvent(TransactionEntryEvent.ShowAccountSheet(true)) },
                modifier = Modifier.weight(1f),
                iconContainerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
                iconTint = MaterialTheme.colorScheme.primary
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
                    iconContainerColor = MaterialTheme.colorScheme.tertiary.copy(alpha = 0.1f),
                    iconTint = MaterialTheme.colorScheme.tertiary
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
                    iconContainerColor = MaterialTheme.colorScheme.tertiary.copy(alpha = 0.1f),
                    iconTint = MaterialTheme.colorScheme.tertiary
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
    iconContainerColor: Color = MaterialTheme.colorScheme.surface,
    iconTint: Color = MaterialTheme.colorScheme.primary
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(32.dp),
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        border = BorderStroke(0.5.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.15f)),
        shadowElevation = 0.dp,
        modifier = modifier.height(72.dp)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                shape = CircleShape,
                color = iconContainerColor,
                modifier = Modifier.size(40.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(icon, contentDescription = null, modifier = Modifier.size(20.dp), tint = iconTint)
                }
            }
            Spacer(modifier = Modifier.width(16.dp))
            Column(verticalArrangement = Arrangement.Center) {
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.5.sp,
                        fontSize = 10.sp
                    ),
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                )
                Text(
                    text = value,
                    style = MaterialTheme.typography.bodyLarge.copy(
                        fontWeight = FontWeight.SemiBold
                    ),
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
