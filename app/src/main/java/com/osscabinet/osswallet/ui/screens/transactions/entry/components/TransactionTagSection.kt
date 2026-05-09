package com.osscabinet.ossWallet.ui.screens.transactions.entry.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Notes
import androidx.compose.material.icons.automirrored.rounded.Label
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.osscabinet.ossWallet.domain.service.currency.CurrencyFormatter.formatCurrencyRight
import com.osscabinet.ossWallet.ui.components.categoryIcons
import com.osscabinet.ossWallet.ui.components.entry.*
import com.osscabinet.ossWallet.ui.screens.transactions.entry.TransactionEntryEvent
import com.osscabinet.ossWallet.ui.screens.transactions.entry.TransactionEntryState

/**
 * Metadata section containing note, payee, linked transactions, tags, and other details.
 * Following the "Architect Slate" design specification.
 */
@Composable
fun TransactionTagSection(
    state: TransactionEntryState,
    onEvent: (TransactionEntryEvent) -> Unit
) {
    EntrySection(
        title = "ADDITIONAL DETAILS",
        icon = Icons.Rounded.MoreHoriz
    ) {
        // Note input
        EntryEditorialInput(
            value = state.note,
            onValueChange = { onEvent(TransactionEntryEvent.SetNote(it)) },
            label = "NOTE",
            icon = Icons.AutoMirrored.Rounded.Notes
        )

        // Payee input
        EntryEditorialInput(
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
            is24Hour = state.use24HourFormat,
            onShowSheet = { onEvent(TransactionEntryEvent.ShowLinkedTransactionSheet(true)) },
            onRemoveLink = { txId ->
                state.transactionId?.let { onEvent(TransactionEntryEvent.RemoveLinkedTransaction(it, txId)) }
            }
        )

        // Tags
        EntryTagSection(
            selectedTags = state.selectedTags,
            onShowTagSheet = { onEvent(TransactionEntryEvent.ShowTagSheet(true)) },
            onRemoveTag = { tag -> onEvent(TransactionEntryEvent.RemoveTag(tag)) }
        )

        // Payment Method and Status row
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
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

            EntryActionChip(
                icon = paymentIcon,
                label = state.paymentMethod,
                onClick = { onEvent(TransactionEntryEvent.ShowPaymentMethodSheet(true)) },
                modifier = Modifier.weight(1f)
            )

            val statusColor = Color(android.graphics.Color.parseColor(state.status.colorHex))
            val statusIcon = com.osscabinet.ossWallet.ui.components.AccountIconMap[state.status.iconName]
                ?: Icons.Rounded.CheckCircle

            EntryActionChip(
                icon = statusIcon,
                label = state.status.displayName,
                onClick = { onEvent(TransactionEntryEvent.ShowStatusSheet(true)) },
                modifier = Modifier.weight(1f),
                iconContainerColor = statusColor.copy(alpha = 0.1f),
                iconTint = statusColor
            )
        }

        HorizontalDivider(
            modifier = Modifier.padding(vertical = 4.dp),
            thickness = 0.5.dp,
            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.2f)
        )

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

@Composable
private fun LinkedTransactionsSection(
    linkedTransactions: List<com.osscabinet.ossWallet.domain.model.Transaction>,
    accounts: List<com.osscabinet.ossWallet.domain.model.Account>,
    categories: List<com.osscabinet.ossWallet.domain.model.Category>,
    is24Hour: Boolean,
    onShowSheet: () -> Unit,
    onRemoveLink: (Long) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.05f),
            border = BorderStroke(0.5.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)),
            modifier = Modifier.fillMaxWidth(),
            onClick = onShowSheet
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        Icons.Rounded.Link,
                        contentDescription = null,
                        modifier = Modifier.padding(8.dp).size(16.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = "Link Transaction",
                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                    color = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.weight(1f))
                Icon(
                    Icons.Rounded.Add,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
            }
        }

        linkedTransactions.forEach { tx ->
            LinkedTransactionItem(
                tx = tx,
                account = accounts.find { it.id == tx.accountId },
                category = categories.find { it.id == tx.categoryId },
                is24Hour = is24Hour,
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
    is24Hour: Boolean,
    onRemove: () -> Unit
) {
    val categoryName = category?.name ?: "Unknown"
    val icon: ImageVector = category?.iconName?.let { categoryIcons[it] }
        ?: categoryIcons[categoryName]
        ?: Icons.Rounded.Category
    val iconBg = category?.colorHex?.let { Color(it) }

    Box {
        com.osscabinet.ossWallet.ui.components.transactions.AeonTransactionItem(
            category = categoryName,
            account = account?.name ?: "Unknown",
            amount = formatCurrencyRight(tx.amount, tx.currency, precision = 2),
            balanceBefore = java.math.BigDecimal.ZERO.toPlainString(),
            balanceAfter = tx.amount.toPlainString(),
            type = if (tx.amount.signum() >= 0) com.osscabinet.ossWallet.domain.model.CashFlowType.INCOME else com.osscabinet.ossWallet.domain.model.CashFlowType.EXPENSE,
            timestamp = formatDateTime(tx.timestamp, is24Hour),
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

        IconButton(
            onClick = onRemove,
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(8.dp)
                .size(24.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.error.copy(alpha = 0.1f))
        ) {
            Icon(
                Icons.Rounded.Close,
                contentDescription = "Remove link",
                tint = MaterialTheme.colorScheme.error,
                modifier = Modifier.size(12.dp)
            )
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
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Surface(
                shape = CircleShape,
                color = MaterialTheme.colorScheme.secondary.copy(alpha = 0.1f),
                modifier = Modifier.size(32.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(Icons.Rounded.Security, contentDescription = null, modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.secondary)
                }
            }
            Text(
                "Warranty Protection",
                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
                modifier = Modifier.weight(1f)
            )
            Switch(
                checked = isEnabled,
                onCheckedChange = onToggle,
                colors = SwitchDefaults.colors(
                    checkedThumbColor = Color.White,
                    checkedTrackColor = Color(0xFF004D40)
                )
            )
        }

        if (isEnabled) {
            Surface(
                onClick = onShowDatePicker,
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.surfaceContainerLowest,
                border = BorderStroke(0.5.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.2f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Rounded.CalendarMonth, contentDescription = null, modifier = Modifier.size(20.dp), tint = MaterialTheme.colorScheme.primary)
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text("Expiration Date", style = MaterialTheme.typography.labelSmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)))
                        Text(
                            text = formatDate(expirationMillis),
                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold)
                        )
                    }
                    Spacer(modifier = Modifier.weight(1f))
                    Icon(Icons.Rounded.ChevronRight, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f))
                }
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
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surfaceContainerLowest,
        border = BorderStroke(0.5.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.2f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                shape = CircleShape,
                color = MaterialTheme.colorScheme.tertiary.copy(alpha = 0.1f),
                modifier = Modifier.size(32.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(Icons.Rounded.LocationOn, contentDescription = null, modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.tertiary)
                }
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column {
                Text("Transaction Location", style = MaterialTheme.typography.labelSmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)))
                Text(
                    text = coords?.let { "Coordinates: ${it.first.toString().take(6)}, ${it.second.toString().take(6)}" } ?: "Add Location",
                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold)
                )
            }
            Spacer(modifier = Modifier.weight(1f))
            Icon(Icons.Rounded.ChevronRight, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f))
        }
    }
}


private fun formatDate(millis: Long): String {
    val formatter = java.text.SimpleDateFormat("MMM dd, yyyy", java.util.Locale.getDefault())
    return formatter.format(java.util.Date(millis))
}

private fun formatDateTime(millis: Long, is24Hour: Boolean = false): String {
    val pattern = if (is24Hour) "MMM dd, HH:mm" else "MMM dd, hh:mm a"
    val formatter = java.text.SimpleDateFormat(pattern, java.util.Locale.getDefault())
    return formatter.format(java.util.Date(millis))
}
