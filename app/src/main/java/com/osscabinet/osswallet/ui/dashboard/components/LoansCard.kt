package com.osscabinet.ossWallet.ui.dashboard.components

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.osscabinet.ossWallet.domain.model.LoanSummary
import com.osscabinet.ossWallet.domain.model.LoanType
import com.osscabinet.ossWallet.domain.model.settings.LoansCardConfig

@Composable
fun LoansCard(
    items: List<LoanSummary>,
    config: LoansCardConfig,
    onUpdate: (LoansCardConfig) -> Unit,
    onLoanClick: (Long) -> Unit,
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
                        listOf("ALL", "LENT", "BORROWED").forEach { type ->
                            DropdownMenuItem(
                                text = { Text(type.lowercase().replaceFirstChar { it.uppercase() }) },
                                trailingIcon = {
                                    if (config.showType == type) {
                                        RadioButton(selected = true, onClick = null)
                                    }
                                },
                                onClick = {
                                    onUpdate(config.copy(showType = type))
                                    showMenu = false
                                }
                            )
                        }
                        HorizontalDivider()
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
                    text = "No loans yet",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(vertical = 8.dp)
                )
            } else {
                Spacer(modifier = Modifier.height(8.dp))
                items.forEach { summary ->
                    LoanSummaryRow(
                        summary = summary,
                        onClick = { onLoanClick(summary.loan.id) }
                    )
                    if (summary != items.last()) HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                }
            }
        }
    }
}

@Composable
private fun LoanSummaryRow(
    summary: LoanSummary,
    onClick: () -> Unit
) {
    val typeLabel = if (summary.loan.loanType == LoanType.LENT) "Lent to" else "Borrowed from"
    val accentColor = if (summary.loan.loanType == LoanType.LENT)
        MaterialTheme.colorScheme.primary
    else
        MaterialTheme.colorScheme.tertiary

    Surface(onClick = onClick, color = MaterialTheme.colorScheme.surface) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = summary.loan.name,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    text = "$typeLabel ${summary.loan.lenderOrBorrowerName}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = "${summary.remainingBalance.toPlainString()} ${summary.loan.currency}",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = accentColor
                )
                Text(
                    text = "remaining",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
