package com.osscabinet.ossWallet.ui.screens.transactions.entry.sheets

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
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
    // Get all statuses from enum - each status has iconName, colorHex, displayName, description
    val statuses = TransactionStatus.values()

    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "Transaction Status",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(16.dp))

            statuses.forEach { status ->
                val isSelected = status == selectedStatus
                val statusColor = Color(android.graphics.Color.parseColor(status.colorHex))
                val statusIcon = com.osscabinet.ossWallet.ui.components.AccountIconMap[status.iconName]
                    ?: Icons.Rounded.CheckCircle

                Surface(
                    onClick = { onStatusSelected(status) },
                    shape = MaterialTheme.shapes.medium,
                    color = if (isSelected) {
                        statusColor.copy(alpha = 0.15f)
                    } else {
                        MaterialTheme.colorScheme.surface
                    },
                    border = if (isSelected) {
                        androidx.compose.foundation.BorderStroke(2.dp, statusColor)
                    } else null,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Status icon with colored background
                        Surface(
                            shape = androidx.compose.foundation.shape.CircleShape,
                            color = statusColor.copy(alpha = 0.2f),
                            modifier = Modifier.size(40.dp)
                        ) {
                            Icon(
                                statusIcon,
                                contentDescription = null,
                                tint = statusColor,
                                modifier = Modifier.padding(8.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(16.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = status.displayName,
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = FontWeight.Medium
                            )
                            Text(
                                text = status.description,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        if (isSelected) {
                            Icon(
                                Icons.Rounded.Check,
                                contentDescription = "Selected",
                                tint = statusColor
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))
            }
        }
    }
}
