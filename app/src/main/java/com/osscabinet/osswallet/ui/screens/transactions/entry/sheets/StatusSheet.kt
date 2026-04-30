package com.osscabinet.ossWallet.ui.screens.transactions.entry.sheets

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.osscabinet.ossWallet.domain.model.TransactionStatus

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StatusSheet(
    selectedStatus: TransactionStatus,
    onStatusSelected: (TransactionStatus) -> Unit,
    onDismiss: () -> Unit
) {
    data class StatusOption(
        val status: TransactionStatus,
        val icon: ImageVector,
        val label: String
    )

    val statuses = listOf(
        StatusOption(TransactionStatus.CLEARED, Icons.Rounded.CheckCircle, "Cleared"),
        StatusOption(TransactionStatus.UNCLEARED, Icons.Rounded.Schedule, "Uncleared"),
        StatusOption(TransactionStatus.RECONCILE, Icons.Rounded.Book, "Reconcile")
    )

    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "Transaction Status",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(16.dp))

            statuses.forEach { option ->
                val isSelected = option.status == selectedStatus

                Surface(
                    onClick = { onStatusSelected(option.status) },
                    shape = MaterialTheme.shapes.medium,
                    color = if (isSelected) {
                        MaterialTheme.colorScheme.primaryContainer
                    } else {
                        MaterialTheme.colorScheme.surface
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(option.icon, contentDescription = null)
                        Spacer(modifier = Modifier.width(16.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = option.label,
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = FontWeight.Medium
                            )
                            Text(
                                text = getStatusDescription(option.status),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        if (isSelected) {
                            Icon(
                                Icons.Rounded.Check,
                                contentDescription = "Selected",
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))
            }
        }
    }
}

private fun getStatusDescription(status: TransactionStatus): String {
    return when (status) {
        TransactionStatus.CLEARED -> "Transaction has been confirmed"
        TransactionStatus.UNCLEARED -> "Awaiting confirmation"
        TransactionStatus.RECONCILE -> "Needs to be reconciled with bank statement"
    }
}
