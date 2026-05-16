package com.osscabinet.ossWallet.ui.screens.dashboard.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.rounded.Payments
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.osscabinet.ossWallet.domain.model.LoanSummary
import com.osscabinet.ossWallet.domain.model.LoanType
import com.osscabinet.ossWallet.domain.model.settings.LoansCardConfig

import androidx.compose.foundation.shape.RoundedCornerShape
import com.osscabinet.ossWallet.ui.components.cards.AeonExpressiveCard
import androidx.compose.ui.unit.sp

import com.osscabinet.ossWallet.domain.service.currency.CurrencyFormatter.formatAmount
import com.osscabinet.ossWallet.domain.service.currency.CurrencyFormatter.formatCurrencyRight
import com.osscabinet.ossWallet.ui.theme.WarningColor
import java.math.BigDecimal
import java.math.RoundingMode
import com.osscabinet.ossWallet.ui.theme.AppColors

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
        icon = Icons.Rounded.Payments,
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
                        tint = AppColors.primary,
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
                color = AppColors.contentSecondary,
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
    val accentColor = if (summary.loan.loanType == LoanType.LENT) AppColors.primary else WarningColor
    
    val progress = if (summary.loan.principalAmount > BigDecimal.ZERO)
        (summary.totalPaid.divide(summary.loan.principalAmount, 4, RoundingMode.HALF_UP)).toFloat().coerceIn(0f, 1f)
    else 0f

    Surface(
        onClick = onClick,
        color = AppColors.sheetSurface,
        shape = RoundedCornerShape(24.dp),
        border = BorderStroke(1.dp, AppColors.divider.copy(alpha = 0.1f))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = summary.loan.name,
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            letterSpacing = (-0.2).sp
                        ),
                        color = AppColors.contentPrimary
                    )
                    Text(
                        text = if (summary.loan.loanType == LoanType.LENT) "LENT TO ${summary.loan.lenderOrBorrowerName}" 
                               else "BORROWED FROM ${summary.loan.lenderOrBorrowerName}",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 0.8.sp
                        ),
                        color = accentColor.copy(alpha = 0.7f)
                    )
                }
                
                Text(
                    text = "${(progress * 100).toInt()}%",
                    style = MaterialTheme.typography.labelLarge.copy(
                        fontWeight = FontWeight.Black,
                        fontFeatureSettings = "tnum"
                    ),
                    color = accentColor
                )
            }
            
            Spacer(modifier = Modifier.height(12.dp))
            
            LinearProgressIndicator(
                progress = { progress },
                modifier = Modifier.fillMaxWidth().height(10.dp),
                color = accentColor,
                trackColor = accentColor.copy(alpha = 0.1f),
                strokeCap = StrokeCap.Round
            )
            
            Spacer(modifier = Modifier.height(14.dp))
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "REMAINING",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.sp
                        ),
                        color = AppColors.contentSecondary.copy(alpha = 0.6f)
                    )
                    Text(
                        text = formatCurrencyRight(summary.remainingBalance, summary.loan.currency, precision, shorten = true),
                        style = MaterialTheme.typography.bodyLarge.copy(
                            fontWeight = FontWeight.ExtraBold
                        ),
                        color = AppColors.contentPrimary
                    )
                }
                
                Surface(
                    color = AppColors.componentOnCard.copy(alpha = 0.5f),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(
                        text = "Principal: ${formatAmount(summary.loan.principalAmount, precision)}",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.Bold,
                            fontFeatureSettings = "tnum"
                        ),
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        color = AppColors.contentSecondary.copy(alpha = 0.7f)
                    )
                }
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
