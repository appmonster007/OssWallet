package com.osscabinet.ossWallet.ui.dashboard.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.rounded.AccountBalance
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.osscabinet.ossWallet.domain.model.LoanSummary
import com.osscabinet.ossWallet.domain.model.LoanType
import com.osscabinet.ossWallet.domain.model.settings.LoansCardConfig

import androidx.compose.foundation.shape.RoundedCornerShape
import com.osscabinet.ossWallet.ui.components.cards.AeonExpressiveCard
import androidx.compose.ui.unit.sp

import com.osscabinet.ossWallet.ui.util.formatAmount

@Composable
fun LoansCard(
    items: List<LoanSummary>,
    config: LoansCardConfig,
    onUpdate: (LoansCardConfig) -> Unit,
    onLoanClick: (Long) -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier,
    headerModifier: Modifier = Modifier,
    precision: Int = 2
) {
    var showMenu by remember { mutableStateOf(false) }

    AeonExpressiveCard(
        title = config.title,
        icon = Icons.Rounded.AccountBalance,
        modifier = modifier,
        headerModifier = headerModifier,
        headerTrailing = { _ ->
            Box {
                IconButton(
                    onClick = { showMenu = true },
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        Icons.Default.MoreVert,
                        contentDescription = "Options",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp)
                    )
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
                        text = { Text("Delete card") },
                        leadingIcon = { Icon(Icons.Default.Delete, contentDescription = null) },
                        onClick = { showMenu = false; onDelete() }
                    )
                }
            }
        }
    ) {
        if (items.isEmpty()) {
            Text(
                text = "No loans yet",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(vertical = 8.dp)
            )
        } else {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                items.forEach { summary ->
                    LoanSummaryRow(
                        summary = summary,
                        onClick = { onLoanClick(summary.loan.id) },
                        precision = precision
                    )
                }
            }
        }
    }
}

@Composable
private fun LoanSummaryRow(
    summary: LoanSummary,
    onClick: () -> Unit,
    precision: Int = 2
) {
    val typeLabel = if (summary.loan.loanType == LoanType.LENT) "Lent to" else "Borrowed from"
    val accentColor = if (summary.loan.loanType == LoanType.LENT)
        MaterialTheme.colorScheme.primary
    else
        MaterialTheme.colorScheme.tertiary

    Surface(
        onClick = onClick,
        color = MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.3f),
        shape = RoundedCornerShape(20.dp),
        border = BorderStroke(0.5.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.1f))
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 14.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = summary.loan.name,
                    style = MaterialTheme.typography.titleSmall.copy(
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 14.sp
                    )
                )
                Text(
                    text = "$typeLabel ${summary.loan.lenderOrBorrowerName}".uppercase(),
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontWeight = FontWeight.Bold,
                        fontSize = 9.sp,
                        letterSpacing = 0.5.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                    )
                )
            }
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = "${formatAmount(summary.remainingBalance, precision)} ${summary.loan.currency}",
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp,
                        color = accentColor
                    )
                )
                Text(
                    text = "REMAINING",
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.5.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                    )
                )
            }
        }
    }
}

@androidx.compose.ui.tooling.preview.Preview(showBackground = true)
@Composable
fun LoansCardPreview() {
    val loan = com.osscabinet.ossWallet.domain.model.Loan(
        id = 1L, name = "Car Loan", principalAmount = java.math.BigDecimal("15000.00"), currency = "USD",
        startDate = System.currentTimeMillis(), lenderOrBorrowerName = "Bank",
        loanType = com.osscabinet.ossWallet.domain.model.LoanType.BORROWED, linkedAccountId = "1", profileId = 1
    )
    val loans = listOf(
        com.osscabinet.ossWallet.domain.model.LoanSummary(
            loan = loan,
            remainingBalance = java.math.BigDecimal("12000.00"),
            totalPaid = java.math.BigDecimal("3000.00"),
            interestAccrued = java.math.BigDecimal("150.00")
        )
    )

    com.osscabinet.ossWallet.ui.theme.OssWalletTheme {
        Box(modifier = Modifier.padding(16.dp)) {
            LoansCard(
                items = loans,
                config = com.osscabinet.ossWallet.domain.model.settings.LoansCardConfig(cardId = "preview", title = "Loans"),
                onUpdate = {},
                onLoanClick = {},
                onDelete = {}
            )
        }
    }
}
