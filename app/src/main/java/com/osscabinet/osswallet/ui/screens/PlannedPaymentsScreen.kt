package com.osscabinet.ossWallet.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.osscabinet.ossWallet.domain.model.PlannedPayment
import com.osscabinet.ossWallet.domain.model.PlannedPaymentStatus
import com.osscabinet.ossWallet.ui.viewmodel.PlannedPaymentViewModel
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlannedPaymentsScreen(
    viewModel: PlannedPaymentViewModel,
    onAddClick: () -> Unit,
    onEditClick: (Long) -> Unit,
    onBackClick: () -> Unit
) {
    val payments by viewModel.plannedPayments.collectAsStateWithLifecycle()

    val active = payments.filter { it.status == PlannedPaymentStatus.ACTIVE }
    val paused = payments.filter { it.status == PlannedPaymentStatus.PAUSED }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Planned Payments") },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = onAddClick) {
                Icon(Icons.Default.Add, contentDescription = "Add")
            }
        }
    ) { padding ->
        if (payments.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "No planned payments yet.\nTap + to add one.",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (active.isNotEmpty()) {
                    item {
                        Text(
                            "Active",
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(vertical = 4.dp)
                        )
                    }
                    items(active, key = { it.id }) { payment ->
                        PlannedPaymentItem(
                            payment = payment,
                            onEdit = { onEditClick(payment.id) },
                            onDelete = { viewModel.delete(payment.id) },
                            onPause = { viewModel.pause(payment) },
                            onResume = { viewModel.resume(payment) }
                        )
                    }
                }
                if (paused.isNotEmpty()) {
                    item {
                        Text(
                            "Paused",
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(vertical = 4.dp)
                        )
                    }
                    items(paused, key = { it.id }) { payment ->
                        PlannedPaymentItem(
                            payment = payment,
                            onEdit = { onEditClick(payment.id) },
                            onDelete = { viewModel.delete(payment.id) },
                            onPause = { viewModel.pause(payment) },
                            onResume = { viewModel.resume(payment) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun PlannedPaymentItem(
    payment: PlannedPayment,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onPause: () -> Unit,
    onResume: () -> Unit
) {
    var showMenu by remember { mutableStateOf(false) }
    val dateFormat = remember { SimpleDateFormat("MMM d, yyyy", Locale.getDefault()) }

    ElevatedCard(modifier = Modifier.fillMaxWidth(), onClick = onEdit) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(payment.title, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium)
                Text(
                    "${payment.amount.toPlainString()} ${payment.currency}  •  ${payment.recurrenceRule.type.name.lowercase().replace('_', ' ')}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    "Next: ${dateFormat.format(Date(payment.nextDueDate))}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Box {
                IconButton(onClick = { showMenu = true }) {
                    Icon(Icons.Default.MoreVert, contentDescription = null)
                }
                DropdownMenu(expanded = showMenu, onDismissRequest = { showMenu = false }) {
                    if (payment.status == PlannedPaymentStatus.ACTIVE) {
                        DropdownMenuItem(text = { Text("Pause") }, onClick = { showMenu = false; onPause() })
                    } else {
                        DropdownMenuItem(text = { Text("Resume") }, onClick = { showMenu = false; onResume() })
                    }
                    DropdownMenuItem(text = { Text("Delete", color = MaterialTheme.colorScheme.error) }, onClick = { showMenu = false; onDelete() })
                }
            }
        }
    }
}
