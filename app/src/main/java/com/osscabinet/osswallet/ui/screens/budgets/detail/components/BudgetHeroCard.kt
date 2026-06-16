package com.osscabinet.ossWallet.ui.screens.budgets.detail.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.DonutLarge
import androidx.compose.material.icons.rounded.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.osscabinet.ossWallet.domain.model.Budget
import com.osscabinet.ossWallet.domain.model.BudgetAlertLevel
import com.osscabinet.ossWallet.domain.model.BudgetProgress
import com.osscabinet.ossWallet.ui.util.CurrencyDisplaySpec
import com.osscabinet.ossWallet.ui.util.CurrencyPartStyles
import com.osscabinet.ossWallet.ui.util.CurrencyReprUi
import com.osscabinet.ossWallet.ui.components.cards.AeonExpressiveCard
import com.osscabinet.ossWallet.ui.theme.AppColors
import com.osscabinet.ossWallet.ui.theme.AppTextStyles

@Composable
fun BudgetHeroCard(
    budget: Budget,
    progress: BudgetProgress,
    modifier: Modifier = Modifier
) {
    val accentColor = when (progress.alertLevel) {
        BudgetAlertLevel.EXCEEDED, BudgetAlertLevel.WARNING -> AppColors.error
        BudgetAlertLevel.NONE -> AppColors.primary
    }

    AeonExpressiveCard(
        title = budget.name,
        subtitle = "Budget: ${budget.timelineRange.toDisplayString()}",
        icon = Icons.Rounded.DonutLarge,
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
                            text = if (progress.alertLevel == BudgetAlertLevel.EXCEEDED) "OVER BUDGET" else "REMAINING",
                            style = AppTextStyles.caption.copy(
                                fontWeight = FontWeight.SemiBold,
                                letterSpacing = 1.5.sp
                            ),
                            color = AppColors.contentSecondary.copy(alpha = 0.6f)
                        )

                        val displayAmount = if (progress.alertLevel == BudgetAlertLevel.EXCEEDED) {
                            progress.spent.subtract(progress.limit)
                        } else {
                            progress.remaining
                        }

                        Text(
                            text = CurrencyReprUi.formatCurrencyAnnotated(
                                amount = displayAmount,
                                currencyCode = budget.currency,
                                spec = CurrencyDisplaySpec(useSemanticColor = false),
                                partStyles = CurrencyPartStyles(
                                    symbol = SpanStyle(
                                        fontWeight = FontWeight.Normal,
                                        color = AppColors.contentSecondary.copy(alpha = 0.4f),
                                        fontSize = 16.sp
                                    )
                                )
                            ),
                            style = AppTextStyles.heroAmount.copy(
                                fontWeight = FontWeight.Bold,
                                fontSize = 36.sp,
                                letterSpacing = (-1.5).sp
                            ),
                            color = if (progress.alertLevel == BudgetAlertLevel.EXCEEDED) AppColors.error else AppColors.contentPrimary
                        )
                    }

                    Surface(
                        color = accentColor.copy(alpha = 0.1f),
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.dp, accentColor.copy(alpha = 0.2f))
                    ) {
                        Text(
                            text = "${(progress.percentage * 100).toInt()}%",
                            style = AppTextStyles.screenSubTitle.copy(
                                fontWeight = FontWeight.Black,
                                fontSize = 18.sp
                            ),
                            color = accentColor,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                        )
                    }
                }

                Box(modifier = Modifier.fillMaxWidth()) {
                    LinearProgressIndicator(
                        progress = { progress.percentage.coerceIn(0f, 1f) },
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
                                        amount = progress.spent,
                                        currencyCode = null,
                                        spec = CurrencyDisplaySpec(useSemanticColor = false)
                                    )
                                )
                                append(" spent")
                            },
                            style = AppTextStyles.caption.copy(fontWeight = FontWeight.Medium),
                            color = AppColors.contentSecondary.copy(alpha = 0.8f)
                        )
                    }
                    Text(
                        text = buildAnnotatedString {
                            append("Limit: ")
                            append(
                                CurrencyReprUi.formatCurrencyAnnotated(
                                    amount = budget.limitAmount,
                                    currencyCode = budget.currency,
                                    spec = CurrencyDisplaySpec(useSemanticColor = false)
                                )
                            )
                        },
                        style = AppTextStyles.caption.copy(fontWeight = FontWeight.Medium),
                        color = AppColors.contentSecondary.copy(alpha = 0.6f)
                    )
                }
            }

            if (progress.alertLevel != BudgetAlertLevel.NONE) {
                Surface(
                    color = AppColors.cardSurfaceProminent.copy(alpha = 0.4f),
                    shape = RoundedCornerShape(16.dp),
                    border = BorderStroke(0.5.dp, AppColors.divider.copy(alpha = 0.1f))
                ) {
                    Row(
                        modifier = Modifier
                            .padding(12.dp)
                            .fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .background(AppColors.error.copy(alpha = 0.1f), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                Icons.Rounded.Warning,
                                contentDescription = null,
                                tint = AppColors.error,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                        Column {
                            Text(
                                text = if (progress.alertLevel == BudgetAlertLevel.EXCEEDED) "BUDGET EXCEEDED" else "APPROACHING LIMIT",
                                style = AppTextStyles.caption.copy(
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 10.sp,
                                    letterSpacing = 0.8.sp
                                ),
                                color = AppColors.error
                            )
                            Text(
                                text = "Alert threshold set at ${(budget.alertThreshold * 100).toInt()}%",
                                style = AppTextStyles.caption,
                                color = AppColors.contentSecondary.copy(alpha = 0.7f)
                            )
                        }
                    }
                }
            }
        }
    }
}
