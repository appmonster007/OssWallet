package com.osscabinet.ossWallet.ui.screens.budgets.detail.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.DonutLarge
import androidx.compose.material.icons.rounded.Warning
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.osscabinet.ossWallet.domain.model.Budget
import com.osscabinet.ossWallet.domain.model.BudgetAlertLevel
import com.osscabinet.ossWallet.domain.model.BudgetProgress
import com.osscabinet.ossWallet.domain.service.currency.CurrencyFormatter.formatAmount
import com.osscabinet.ossWallet.domain.service.currency.CurrencyFormatter.formatCurrencyRight
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
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            // Main Progress Section
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Bottom
                ) {
                    Column {
                        Text(
                            text = if (progress.alertLevel == BudgetAlertLevel.EXCEEDED) "OVER BUDGET" else "REMAINING",
                            style = AppTextStyles.drawerItemLabel.copy(
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 1.2.sp
                            ),
                            color = accentColor.copy(alpha = 0.6f)
                        )

                        val displayAmount = if (progress.alertLevel == BudgetAlertLevel.EXCEEDED) {
                            progress.spent.subtract(progress.limit)
                        } else {
                            progress.remaining
                        }

                        val amount =
                            formatCurrencyRight(displayAmount, budget.currency, shorten = true)
                        val amountParts = amount.split(" ")
                        amountParts.dropLast(1).joinToString(" ")
                        amountParts.lastOrNull() ?: ""

                        Text(
                            text = buildAnnotatedString {
                                append(amount)
//                                append(" ")
//                                withStyle(SpanStyle(
//                                    fontWeight = FontWeight.Light,
//                                    color = AppColors.contentSecondary,
//                                    fontSize = 18.sp
//                                )) {
//                                    append(symbolText)
//                                }
                            },
                            style = AppTextStyles.heroAmount.copy(
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 32.sp,
                                letterSpacing = (-1).sp
                            ),
                            color = if (progress.alertLevel == BudgetAlertLevel.EXCEEDED) AppColors.error else AppColors.contentPrimary
                        )
                    }

                    Text(
                        text = "${(progress.percentage * 100).toInt()}%",
                        style = AppTextStyles.heroAmountDisplay.copy(
                            fontWeight = FontWeight.Black,
                            fontFeatureSettings = "tnum"
                        ),
                        color = accentColor
                    )
                }

                LinearProgressIndicator(
                    progress = { progress.percentage.coerceIn(0f, 1f) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(8.dp),
                    color = accentColor,
                    trackColor = accentColor.copy(alpha = 0.1f),
                    strokeCap = StrokeCap.Round
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "${formatAmount(progress.spent)} spent",
                        style = AppTextStyles.cardDelta,
                        color = AppColors.contentSecondary.copy(alpha = 0.7f)
                    )
                    Text(
                        text = "Limit: ${
                            formatCurrencyRight(
                                budget.limitAmount,
                                budget.currency,
                                shorten = true
                            )
                        }",
                        style = AppTextStyles.cardDelta,
                        color = AppColors.contentSecondary.copy(alpha = 0.7f)
                    )
                }
            }

            if (progress.alertLevel != BudgetAlertLevel.NONE) {
                Surface(
                    color = AppColors.error.copy(alpha = 0.05f),
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, AppColors.error.copy(alpha = 0.1f))
                ) {
                    Row(
                        modifier = Modifier
                            .padding(12.dp)
                            .fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            Icons.Rounded.Warning,
                            contentDescription = null,
                            tint = AppColors.error,
                            modifier = Modifier.size(18.dp)
                        )
                        Column {
                            Text(
                                text = if (progress.alertLevel == BudgetAlertLevel.EXCEEDED) "Budget Exceeded" else "Approaching Limit",
                                style = AppTextStyles.managementItemTitle,
                                color = AppColors.error
                            )
                            Text(
                                text = "Alert threshold set at ${(budget.alertThreshold * 100).toInt()}%",
                                style = AppTextStyles.caption,
                                color = AppColors.error.copy(alpha = 0.7f)
                            )
                        }
                    }
                }
            }
        }
    }
}
