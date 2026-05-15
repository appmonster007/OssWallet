package com.osscabinet.ossWallet.ui.screens.budgets.detail.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
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
import com.osscabinet.ossWallet.ui.components.cards.AeonExpressiveCard
import com.osscabinet.ossWallet.domain.service.currency.CurrencyFormatter.formatAmount
import com.osscabinet.ossWallet.domain.service.currency.CurrencyFormatter.formatCurrencyRight
import java.math.BigDecimal

@Composable
fun BudgetHeroCard(
    budget: Budget,
    progress: BudgetProgress,
    modifier: Modifier = Modifier,
    precision: Int = 2
) {
    val healthyColor = Color(0xFF00695C)
    val warningColor = Color(0xFFC62828)
    
    val accentColor = when (progress.alertLevel) {
        BudgetAlertLevel.EXCEEDED, BudgetAlertLevel.WARNING -> warningColor
        BudgetAlertLevel.NONE -> healthyColor
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
                            style = MaterialTheme.typography.labelMedium.copy(
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

                        val amount = formatCurrencyRight(displayAmount, budget.currency, precision = precision, shorten = true)
                        val amountParts = amount.split(" ")
                        val amountText = amountParts.dropLast(1).joinToString(" ")
                        val symbolText = amountParts.lastOrNull() ?: ""

                        Text(
                            text = buildAnnotatedString {
                                append(amount)
//                                append(" ")
//                                withStyle(SpanStyle(
//                                    fontWeight = FontWeight.Light,
//                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
//                                    fontSize = 18.sp
//                                )) {
//                                    append(symbolText)
//                                }
                            },
                            style = MaterialTheme.typography.headlineLarge.copy(
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 32.sp,
                                letterSpacing = (-1).sp
                            ),
                            color = if (progress.alertLevel == BudgetAlertLevel.EXCEEDED) warningColor else MaterialTheme.colorScheme.onSurface
                        )
                    }
                    
                    Text(
                        text = "${(progress.percentage * 100).toInt()}%",
                        style = MaterialTheme.typography.displaySmall.copy(
                            fontWeight = FontWeight.Black,
                            fontFeatureSettings = "tnum"
                        ),
                        color = accentColor
                    )
                }

                LinearProgressIndicator(
                    progress = { progress.percentage.coerceIn(0f, 1f) },
                    modifier = Modifier.fillMaxWidth().height(8.dp),
                    color = accentColor,
                    trackColor = accentColor.copy(alpha = 0.1f),
                    strokeCap = StrokeCap.Round
                )
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "${formatAmount(progress.spent, precision)} spent",
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                    )
                    Text(
                        text = "Limit: ${formatCurrencyRight(budget.limitAmount, budget.currency, precision, shorten = true)}",
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                    )
                }
            }

            if (progress.alertLevel != BudgetAlertLevel.NONE) {
                Surface(
                    color = warningColor.copy(alpha = 0.05f),
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, warningColor.copy(alpha = 0.1f))
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp).fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            Icons.Rounded.Warning,
                            contentDescription = null,
                            tint = warningColor,
                            modifier = Modifier.size(18.dp)
                        )
                        Column {
                            Text(
                                text = if (progress.alertLevel == BudgetAlertLevel.EXCEEDED) "Budget Exceeded" else "Approaching Limit",
                                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                color = warningColor
                            )
                            Text(
                                text = "Alert threshold set at ${(budget.alertThreshold * 100).toInt()}%",
                                style = MaterialTheme.typography.bodySmall,
                                color = warningColor.copy(alpha = 0.7f)
                            )
                        }
                    }
                }
            }
        }
    }
}
