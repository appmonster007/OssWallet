package com.osscabinet.ossWallet.ui.screens.transactions.entry.sheets

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.osscabinet.ossWallet.domain.model.Account
import com.osscabinet.ossWallet.domain.model.CashFlowType
import com.osscabinet.ossWallet.domain.model.Category
import com.osscabinet.ossWallet.domain.model.Transaction
import com.osscabinet.ossWallet.ui.components.categoryIcons
import com.osscabinet.ossWallet.ui.components.transactions.AeonTransactionItem
import com.osscabinet.ossWallet.ui.util.formatCurrencyRight
import java.math.BigDecimal
import java.text.SimpleDateFormat
import java.util.*

private fun formatDateTime(millis: Long): String {
    val sdf = SimpleDateFormat("MMM dd, HH:mm", Locale.getDefault())
    return sdf.format(Date(millis))
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LinkedTransactionSheet(
    transactions: List<Transaction>,
    currentTransactionId: Long?,
    linkedIds: Set<Long>,
    accounts: List<Account>,
    categories: List<Category>,
    onLinkedTransactionsChanged: (Set<Long>) -> Unit,
    onDismiss: () -> Unit
) {
    var selectedIds by remember { mutableStateOf(linkedIds) }

    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Link Transactions",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "${selectedIds.size} selected",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Spacer(modifier = Modifier.height(16.dp))

            LazyColumn(
                modifier = Modifier.heightIn(max = 500.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(
                    transactions
                        .filter { it.id != currentTransactionId }
                        .sortedByDescending { it.timestamp }
                ) { tx ->
                    val isSelected = tx.id in selectedIds
                    val account = accounts.find { it.id == tx.accountId }
                    val category = categories.find { it.name == tx.category }
                    val icon = categoryIcons[category?.iconName] ?: categoryIcons[tx.category] ?: Icons.Rounded.Category
                    val iconBg = category?.colorHex?.let { Color(it) }

                    Box {
                        AeonTransactionItem(
                            category = tx.category,
                            account = account?.name ?: "Unknown",
                            amount = formatCurrencyRight(tx.amount, tx.currency, precision = 2),
                            balanceBefore = formatCurrencyRight(BigDecimal.ZERO, tx.currency, precision = 2),
                            balanceAfter = formatCurrencyRight(tx.amount, tx.currency, precision = 2),
                            type = if (tx.amount.signum() >= 0) CashFlowType.INCOME else CashFlowType.EXPENSE,
                            timestamp = formatDateTime(tx.timestamp),
                            note = tx.note,
                            tags = tx.tags,
                            icon = icon,
                            iconBg = iconBg,
                            precision = 2,
                            showOpeningBalance = false,
                            showClosingBalance = false,
                            status = tx.status,
                            onClick = {
                                selectedIds = if (isSelected) {
                                    selectedIds - tx.id
                                } else {
                                    selectedIds + tx.id
                                }
                            }
                        )

                        // Checkbox overlay with animation
                        Surface(
                            modifier = Modifier
                                .align(Alignment.TopStart)
                                .padding(horizontal = 8.dp, vertical = 8.dp),
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.surface.copy(alpha = 0.9f),
                            border = BorderStroke(
                                2.dp,
                                if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant
                            )
                        ) {
                            Box(
                                modifier = Modifier.size(24.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                if (isSelected) {
                                    Icon(
                                        Icons.Rounded.Check,
                                        contentDescription = "Selected",
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Button(
                onClick = {
                    onLinkedTransactionsChanged(selectedIds)
                    onDismiss()
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Link ${selectedIds.size} Transactions")
            }
        }
    }
}
