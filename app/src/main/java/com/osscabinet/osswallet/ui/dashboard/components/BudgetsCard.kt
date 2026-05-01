package com.osscabinet.ossWallet.ui.dashboard.components

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.osscabinet.ossWallet.domain.model.BudgetAlertLevel
import com.osscabinet.ossWallet.domain.model.BudgetProgress
import com.osscabinet.ossWallet.domain.model.settings.BudgetsCardConfig

@Composable
fun BudgetsCard(
    items: List<BudgetProgress>,
    config: BudgetsCardConfig,
    onUpdate: (BudgetsCardConfig) -> Unit,
    onBudgetClick: (Long) -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier
) {
    var showMenu by remember { mutableStateOf(false) }

    ElevatedCard(modifier = modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = config.title,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold
                )
                Box {
                    IconButton(onClick = { showMenu = true }) {
                        Icon(Icons.Default.MoreVert, contentDescription = null)
                    }
                    DropdownMenu(expanded = showMenu, onDismissRequest = { showMenu = false }) {
                        DropdownMenuItem(
                            text = { Text("Show archived") },
                            trailingIcon = {
                                Checkbox(checked = config.showArchived, onCheckedChange = null)
                            },
                            onClick = {
                                onUpdate(config.copy(showArchived = !config.showArchived))
                                showMenu = false
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Delete") },
                            leadingIcon = { Icon(Icons.Default.Delete, contentDescription = null) },
                            onClick = { showMenu = false; onDelete() }
                        )
                    }
                }
            }

            if (items.isEmpty()) {
                Text(
                    text = "No budgets yet",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(vertical = 8.dp)
                )
            } else {
                Spacer(modifier = Modifier.height(8.dp))
                items.forEach { progress ->
                    BudgetProgressRow(
                        progress = progress,
                        onClick = { onBudgetClick(progress.budget.id) }
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                }
            }
        }
    }
}

@Composable
private fun BudgetProgressRow(
    progress: BudgetProgress,
    onClick: () -> Unit
) {
    val trackColor = when (progress.alertLevel) {
        BudgetAlertLevel.EXCEEDED -> MaterialTheme.colorScheme.error
        BudgetAlertLevel.WARNING -> Color(0xFFF57C00)
        BudgetAlertLevel.NONE -> MaterialTheme.colorScheme.primary
    }

    Surface(onClick = onClick, color = MaterialTheme.colorScheme.surface) {
        Column {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = progress.budget.name,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    text = "${progress.spent.toPlainString()} / ${progress.limit.toPlainString()} ${progress.budget.currency}",
                    style = MaterialTheme.typography.bodySmall,
                    color = trackColor
                )
            }
            Spacer(modifier = Modifier.height(4.dp))
            LinearProgressIndicator(
                progress = { progress.percentage.coerceIn(0f, 1f) },
                modifier = Modifier.fillMaxWidth(),
                color = trackColor,
                trackColor = MaterialTheme.colorScheme.surfaceVariant
            )
            if (progress.alertLevel == BudgetAlertLevel.EXCEEDED) {
                Text(
                    text = "Over budget by ${progress.spent.subtract(progress.limit).toPlainString()} ${progress.budget.currency}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.error
                )
            } else {
                Text(
                    text = "${progress.remaining.toPlainString()} ${progress.budget.currency} remaining",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
