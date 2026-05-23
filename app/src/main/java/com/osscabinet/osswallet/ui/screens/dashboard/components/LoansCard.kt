package com.osscabinet.ossWallet.ui.screens.dashboard.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Payments
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
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
import com.osscabinet.ossWallet.ui.components.cards.CardCallbacks
import com.osscabinet.ossWallet.ui.components.cards.CardSettingsSheet
import com.osscabinet.ossWallet.ui.components.cards.CardSettingsSection
import androidx.compose.ui.unit.sp

import com.osscabinet.ossWallet.domain.service.currency.CurrencyFormatter.formatAmount
import com.osscabinet.ossWallet.domain.service.currency.CurrencyFormatter.formatCurrencyRight
import com.osscabinet.ossWallet.ui.theme.WarningColor
import java.math.BigDecimal
import java.math.RoundingMode
import com.osscabinet.ossWallet.ui.theme.AppColors
import com.osscabinet.ossWallet.ui.theme.AppTextStyles

@Composable
fun LoansCard(
    items: List<LoanSummary>,
    config: LoansCardConfig,
    callbacks: CardCallbacks.Loans,
    modifier: Modifier = Modifier,
    headerModifier: Modifier = Modifier
) {
    AeonExpressiveCard(
        title = config.title,
        icon = Icons.Rounded.Payments,
        modifier = modifier,
        headerModifier = headerModifier,
        onClick = callbacks.onSeeAll,
        filterEnabled = false,
        displaySettings = { onDismiss ->
            LoansSettingsSheet(config = config, callbacks = callbacks, onDismiss = onDismiss)
        }
    ) {
        if (items.isEmpty()) {
            Text(
                text = "No loans yet",
                style = AppTextStyles.caption,
                color = AppColors.contentSecondary,
                modifier = Modifier.padding(vertical = 8.dp)
            )
        } else {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                items.forEach { summary ->
                    LoanSummaryRow(
                        summary = summary,
                        onClick = { callbacks.onLoanClick(summary.loan.id) }
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun LoansSettingsSheet(
    config: LoansCardConfig,
    callbacks: CardCallbacks.Loans,
    onDismiss: () -> Unit
) {
    CardSettingsSheet(title = "Loans", onDismiss = onDismiss, onDelete = callbacks.onDelete) {
        CardSettingsSection("Show Type") {
            listOf("ALL", "LENT", "BORROWED").forEach { type ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(type.lowercase().replaceFirstChar { it.uppercase() }, style = AppTextStyles.transactionTitle)
                    RadioButton(
                        selected = config.showType == type,
                        onClick = { callbacks.onUpdate(config.copy(showType = type)) }
                    )
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
                        style = AppTextStyles.screenSubTitle.copy(
                            fontWeight = FontWeight.Bold,
                            letterSpacing = (-0.2).sp
                        ),
                        color = AppColors.contentPrimary
                    )
                    Text(
                        text = if (summary.loan.loanType == LoanType.LENT) "LENT TO ${summary.loan.lenderOrBorrowerName}" 
                               else "BORROWED FROM ${summary.loan.lenderOrBorrowerName}",
                        style = AppTextStyles.transactionSwipeAction.copy(
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 0.8.sp
                        ),
                        color = accentColor.copy(alpha = 0.7f)
                    )
                }
                
                Text(
                    text = "${(progress * 100).toInt()}%",
                    style = AppTextStyles.drawerItemLabel.copy(
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
                        style = AppTextStyles.transactionSwipeAction.copy(
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.sp
                        ),
                        color = AppColors.contentSecondary.copy(alpha = 0.6f)
                    )
                    Text(
                        text = formatCurrencyRight(summary.remainingBalance, summary.loan.currency, shorten = true),
                        style = AppTextStyles.listItemTitle.copy(
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
                        text = "Principal: ${formatAmount(summary.loan.principalAmount)}",
                        style = AppTextStyles.transactionSwipeAction.copy(
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
        id = 1L, name = "Car Loan", principalAmount = BigDecimal("15000.00"), currency = "USD",
        startDate = System.currentTimeMillis(), lenderOrBorrowerName = "Bank",
        loanType = LoanType.BORROWED, linkedAccountId = "1", profileId = 1
    )
    val loans = listOf(
        LoanSummary(
            loan = loan,
            remainingBalance = BigDecimal("12000.00"),
            totalPaid = BigDecimal("3000.00"),
            interestAccrued = BigDecimal("150.00")
        )
    )

    com.osscabinet.ossWallet.ui.theme.OssWalletTheme {
        Box(modifier = Modifier.padding(16.dp)) {
            LoansCard(
                items = loans,
                config = LoansCardConfig(cardId = "preview", title = "Loans"),
                callbacks = CardCallbacks.Loans(onUpdate = {}, onLoanClick = {}, onDelete = {})
            )
        }
    }
}
