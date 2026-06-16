package com.osscabinet.ossWallet.ui.screens.loans.detail.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.osscabinet.ossWallet.domain.model.Loan
import com.osscabinet.ossWallet.domain.model.LoanSummary
import com.osscabinet.ossWallet.domain.model.LoanType
import com.osscabinet.ossWallet.ui.util.CurrencyDisplaySpec
import com.osscabinet.ossWallet.ui.util.CurrencyPartStyles
import com.osscabinet.ossWallet.ui.util.CurrencyReprUi
import com.osscabinet.ossWallet.ui.components.cards.AeonExpressiveCard
import com.osscabinet.ossWallet.ui.theme.AeonTheme
import com.osscabinet.ossWallet.ui.theme.AppColors
import com.osscabinet.ossWallet.ui.theme.AppTextStyles
import com.osscabinet.ossWallet.ui.theme.WarningColor
import java.math.BigDecimal

@Composable
fun LoanHeroCard(
    loan: Loan,
    summary: LoanSummary,
    modifier: Modifier = Modifier
) {
    val accentColor = if (loan.loanType == LoanType.LENT) AppColors.primary else WarningColor

    val progress = if (loan.principalAmount > BigDecimal.ZERO)
        (summary.totalPaid.divide(
            loan.principalAmount,
            4,
            java.math.RoundingMode.HALF_UP
        )).toFloat().coerceIn(0f, 1f)
    else 0f

    AeonExpressiveCard(
        title = loan.name,
        subtitle = "${loan.loanType.name} — ${loan.currency}",
        icon = Icons.Rounded.AccountBalance,
        accentColor = accentColor,
        modifier = modifier
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            // Main Progress Section
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Bottom
                ) {
                    Column {
                        Text(
                            text = "REMAINING BALANCE",
                            style = AppTextStyles.sectionOverline,
                            color = AppColors.contentSecondary
                        )

                        Text(
                            text = CurrencyReprUi.formatCurrencyAnnotated(
                                amount = summary.remainingBalance,
                                currencyCode = loan.currency,
                                spec = CurrencyDisplaySpec(useSemanticColor = false),
                                partStyles = CurrencyPartStyles(
                                    symbol = SpanStyle(
                                        fontWeight = FontWeight.Normal,
                                        color = AppColors.contentSecondary,
                                        fontSize = 16.sp
                                    )
                                )
                            ),
                            style = AppTextStyles.heroAmountDisplay,
                            color = AppColors.contentPrimary
                        )
                    }

                    Surface(
                        color = accentColor.copy(alpha = 0.1f),
                        shape = RoundedCornerShape(AeonTheme.dimensions.componentCornerRadius),
                        border = BorderStroke(1.dp, accentColor)
                    ) {
                        Text(
                            text = "${(progress * 100).toInt()}%",
                            style = AppTextStyles.cardDelta,
                            color = accentColor,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                        )
                    }
                }

                Box(modifier = Modifier.fillMaxWidth()) {
                    LinearProgressIndicator(
                        progress = { progress },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(12.dp),
                        color = accentColor,
                        trackColor = accentColor.copy(alpha = 0.08f),
                        strokeCap = StrokeCap.Round
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(6.dp)
                                .background(accentColor, CircleShape)
                        )
                        Text(
                            text = buildAnnotatedString {
                                append(
                                    CurrencyReprUi.formatCurrencyAnnotated(
                                        amount = summary.totalPaid,
                                        currencyCode = null,
                                        spec = CurrencyDisplaySpec(useSemanticColor = false)
                                    )
                                )
                                append(" settled")
                            },
                            style = AppTextStyles.caption,
                            color = AppColors.contentSecondary
                        )
                    }
                    Text(
                        text = buildAnnotatedString {
                            append("Goal: ")
                            append(
                                CurrencyReprUi.formatCurrencyAnnotated(
                                    amount = loan.principalAmount,
                                    currencyCode = loan.currency,
                                    spec = CurrencyDisplaySpec(useSemanticColor = false)
                                )
                            )
                        },
                        style = AppTextStyles.caption,
                        color = AppColors.contentSecondary
                    )
                }
            }

            // Loan Details Grid
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                LoanMetricItem(
                    label = if (loan.loanType == LoanType.LENT) "LENT TO" else "BORROWED FROM",
                    value = loan.lenderOrBorrowerName,
                    icon = if (loan.loanType == LoanType.LENT) Icons.Rounded.NorthEast else Icons.Rounded.SouthWest,
                    color = AppColors.primary,
                    modifier = Modifier.weight(1.2f)
                )
                if (loan.interestRate != null) {
                    LoanMetricItem(
                        label = "INTEREST",
                        value = "${loan.interestRate}%",
                        icon = Icons.Rounded.Percent,
                        color = AppColors.error,
                        modifier = Modifier.weight(0.8f)
                    )
                }
            }
        }
    }
}

@Composable
private fun LoanMetricItem(
    label: String,
    value: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    color: Color,
    modifier: Modifier = Modifier
) {
    Surface(
        color = AppColors.surfaceContainerLow,
        shape = RoundedCornerShape(AeonTheme.dimensions.componentCornerRadius),
        border = BorderStroke(1.dp, AppColors.outlineVariant),
        modifier = modifier
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = color,
                    modifier = Modifier.size(14.dp)
                )
                Text(
                    text = label,
                    style = AppTextStyles.overlineLabel,
                    color = AppColors.contentSecondary
                )
            }
            Text(
                text = value,
                style = AppTextStyles.managementItemTitle,
                color = AppColors.contentPrimary,
                maxLines = 1,
                overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
            )
        }
    }
}

