package com.osscabinet.ossWallet.ui.screens.transactions.entry.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Notes
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import com.osscabinet.ossWallet.ui.components.categoryIcons
import com.osscabinet.ossWallet.ui.screens.transactions.entry.TransactionEntryEvent
import com.osscabinet.ossWallet.ui.screens.transactions.entry.TransactionEntryState
import com.osscabinet.ossWallet.ui.util.formatCurrencyRight

/**
 * Metadata section containing note, payee, linked transactions, tags, and other details.
 */
@Composable
fun TransactionMetadataSection(
    state: TransactionEntryState,
    onEvent: (TransactionEntryEvent) -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(28.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Note input
            EditorialInput(
                value = state.note,
                onValueChange = { onEvent(TransactionEntryEvent.SetNote(it)) },
                label = "NOTE",
                icon = Icons.AutoMirrored.Rounded.Notes
            )

            // Payee input
            EditorialInput(
                value = state.payee,
                onValueChange = { onEvent(TransactionEntryEvent.SetPayee(it)) },
                label = "PAYEE",
                icon = Icons.Rounded.Person
            )

            // Linked Transactions
            LinkedTransactionsSection(
                linkedTransactions = state.displayLinkedTxs,
                accounts = state.accounts,
                categories = state.categories,
                onShowSheet = { onEvent(TransactionEntryEvent.ShowLinkedTransactionSheet(true)) },
                onRemoveLink = { txId ->
                    state.transactionId?.let { onEvent(TransactionEntryEvent.RemoveLinkedTransaction(it, txId)) }
                }
            )

            // Tags
            TagsSection(
                tags = state.selectedTags,
                allTags = state.allTags,
                onShowSheet = { onEvent(TransactionEntryEvent.ShowLabelSheet(true)) },
                onRemoveTag = { tag -> onEvent(TransactionEntryEvent.RemoveTag(tag)) }
            )

            // Payment Method and Status row with icons like original design
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                // Payment Method with exact same icon logic as original
                val methods = listOf(
                    "Cash" to Icons.Rounded.Payments,
                    "Credit Card" to Icons.Rounded.CreditCard,
                    "Debit Card" to Icons.Rounded.CreditCard,
                    "Bank Transfer" to Icons.Rounded.AccountBalance,
                    "Digital Wallet" to Icons.Rounded.Wallet,
                    "Mobile Payment" to Icons.Rounded.Contactless,
                    "Check" to Icons.Rounded.Description,
                    "Cryptocurrency" to Icons.Rounded.CurrencyBitcoin,
                    "Other" to Icons.Rounded.MoreHoriz
                )
                val paymentIcon = methods.find { it.first == state.paymentMethod }?.second ?: Icons.Rounded.Contactless

                ActionChip(
                    icon = paymentIcon,
                    label = state.paymentMethod,
                    onClick = { onEvent(TransactionEntryEvent.ShowPaymentMethodSheet(true)) },
                    modifier = Modifier.weight(1f)
                )

                // Status with icon and color from TransactionStatus enum
                val statusColor = Color(android.graphics.Color.parseColor(state.status.colorHex))
                val statusIcon = com.osscabinet.ossWallet.ui.components.AccountIconMap[state.status.iconName]
                    ?: Icons.Rounded.CheckCircle

                ActionChip(
                    icon = statusIcon,
                    label = state.status.displayName,
                    onClick = { onEvent(TransactionEntryEvent.ShowStatusSheet(true)) },
                    modifier = Modifier.weight(1f),
                    iconContainerColor = statusColor.copy(alpha = 0.2f),
                    iconTint = statusColor
                )
            }

            // Warranty
            WarrantyRow(
                isEnabled = state.isWarrantyEnabled,
                expirationMillis = state.warrantyExpirationMillis,
                onToggle = { onEvent(TransactionEntryEvent.SetWarrantyEnabled(it)) },
                onShowDatePicker = { onEvent(TransactionEntryEvent.ShowWarrantyDatePicker(true)) }
            )

            // Location
            LocationRow(
                coords = state.locationCoords,
                onShowLocationSheet = { onEvent(TransactionEntryEvent.ShowLocationSheet(true)) }
            )
        }
    }
}

@Composable
private fun LinkedTransactionsSection(
    linkedTransactions: List<com.osscabinet.ossWallet.domain.model.Transaction>,
    accounts: List<com.osscabinet.ossWallet.domain.model.Account>,
    categories: List<com.osscabinet.ossWallet.domain.model.Category>,
    onShowSheet: () -> Unit,
    onRemoveLink: (Long) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Surface(
            shape = RoundedCornerShape(28.dp),
            color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)),
            modifier = Modifier.fillMaxWidth(),
            onClick = onShowSheet
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.2f),
                    modifier = Modifier.size(48.dp)
                ) {
                    Icon(
                        Icons.Rounded.Link,
                        contentDescription = null,
                        modifier = Modifier.padding(12.dp).size(24.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
                Spacer(modifier = Modifier.width(16.dp))
                Text(
                    text = "Link Transaction",
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
                Spacer(modifier = Modifier.weight(1f))
                Icon(
                    Icons.Rounded.KeyboardArrowDown,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
            }
        }

        // Display linked transactions with AeonTransactionItem
        linkedTransactions.forEach { tx ->
            LinkedTransactionItem(
                tx = tx,
                account = accounts.find { it.id == tx.accountId },
                category = categories.find { it.name == tx.category },
                onRemove = { onRemoveLink(tx.id) }
            )
        }
    }
}

@Composable
private fun LinkedTransactionItem(
    tx: com.osscabinet.ossWallet.domain.model.Transaction,
    account: com.osscabinet.ossWallet.domain.model.Account?,
    category: com.osscabinet.ossWallet.domain.model.Category?,
    onRemove: () -> Unit
) {
    val icon: ImageVector = category?.iconName?.let { categoryIcons[it] }
        ?: categoryIcons[tx.category]
        ?: Icons.Rounded.Category
    val iconBg = category?.colorHex?.let { Color(it) }

    Box {
        com.osscabinet.ossWallet.ui.components.transactions.AeonTransactionItem(
            category = tx.category,
            account = account?.name ?: "Unknown",
            amount = formatCurrencyRight(tx.amount, tx.currency, precision = 2),
            balanceBefore = java.math.BigDecimal.ZERO.toPlainString(),
            balanceAfter = tx.amount.toPlainString(),
            type = if (tx.amount.signum() >= 0) com.osscabinet.ossWallet.domain.model.CashFlowType.INCOME else com.osscabinet.ossWallet.domain.model.CashFlowType.EXPENSE,
            timestamp = formatDateTime(tx.timestamp),
            note = tx.note,
            tags = tx.tags,
            icon = icon,
            iconBg = iconBg,
            precision = 2,
            showOpeningBalance = false,
            showClosingBalance = false,
            status = tx.status,
            onClick = { /* Linked transactions are read-only here */ }
        )

        // Remove button overlay
        IconButton(
            onClick = onRemove,
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(4.dp)
                .size(28.dp)
        ) {
            Icon(
                Icons.Rounded.Close,
                contentDescription = "Remove link",
                tint = MaterialTheme.colorScheme.error.copy(alpha = 0.7f),
                modifier = Modifier.size(16.dp)
            )
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun TagsSection(
    tags: List<String>,
    allTags: List<com.osscabinet.ossWallet.domain.model.Tag>,
    onShowSheet: () -> Unit,
    onRemoveTag: (String) -> Unit
) {
    FlowRow(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // Add tag button
        Surface(
            onClick = onShowSheet,
            shape = RoundedCornerShape(16.dp),
            color = MaterialTheme.colorScheme.primaryContainer,
            modifier = Modifier.height(32.dp)
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Rounded.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("Add Tag", style = MaterialTheme.typography.labelMedium)
            }
        }

        // Selected tags with remove button
        tags.forEach { tag ->
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.secondaryContainer,
                modifier = Modifier.height(32.dp)
            ) {
                Row(
                    modifier = Modifier.padding(start = 12.dp, end = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(tag, style = MaterialTheme.typography.labelMedium)
                    Spacer(modifier = Modifier.width(4.dp))
                    // Remove button
                    IconButton(
                        onClick = { onRemoveTag(tag) },
                        modifier = Modifier.size(20.dp)
                    ) {
                        Icon(
                            Icons.Rounded.Close,
                            contentDescription = "Remove tag",
                            modifier = Modifier.size(14.dp),
                            tint = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.7f)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun WarrantyRow(
    isEnabled: Boolean,
    expirationMillis: Long,
    onToggle: (Boolean) -> Unit,
    onShowDatePicker: () -> Unit
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Icon(Icons.Rounded.Security, contentDescription = null)
        Text("Warranty", modifier = Modifier.weight(1f))
        Switch(checked = isEnabled, onCheckedChange = onToggle)
    }

    if (isEnabled) {
        Surface(
            onClick = onShowDatePicker,
            shape = RoundedCornerShape(12.dp),
            color = MaterialTheme.colorScheme.surface,
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Rounded.CalendarMonth, contentDescription = null)
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = "Expires: ${formatDate(expirationMillis)}",
                    modifier = Modifier.weight(1f)
                )
                Icon(Icons.Rounded.Edit, contentDescription = null)
            }
        }
    }
}

@Composable
private fun LocationRow(
    coords: Pair<Double, Double>?,
    onShowLocationSheet: () -> Unit
) {
    Surface(
        onClick = onShowLocationSheet,
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(Icons.Rounded.LocationOn, contentDescription = null)
            Spacer(modifier = Modifier.width(12.dp))
            Text(
                text = coords?.let { "Location set" } ?: "Add Location",
                modifier = Modifier.weight(1f)
            )
            Icon(Icons.Rounded.Edit, contentDescription = null)
        }
    }
}


private fun formatDate(millis: Long): String {
    val formatter = java.text.SimpleDateFormat("MMM dd, yyyy", java.util.Locale.getDefault())
    return formatter.format(java.util.Date(millis))
}

private fun formatDateTime(millis: Long): String {
    val formatter = java.text.SimpleDateFormat("MMM dd, HH:mm", java.util.Locale.getDefault())
    return formatter.format(java.util.Date(millis))
}
