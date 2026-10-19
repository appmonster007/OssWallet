package com.osscabinet.ossWallet.ui.screens.budgets.detail.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.rounded.DonutLarge
import androidx.compose.material.icons.rounded.Warning
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.osscabinet.ossWallet.domain.model.Budget
import com.osscabinet.ossWallet.domain.model.BudgetAlertLevel
import com.osscabinet.ossWallet.domain.model.BudgetPaceStatus
import com.osscabinet.ossWallet.domain.model.BudgetProgress
import com.osscabinet.ossWallet.domain.service.management.BudgetService
import com.osscabinet.ossWallet.domain.service.timeline.TimelineService
import com.osscabinet.ossWallet.ui.components.cards.AeonExpressiveCard
import com.osscabinet.ossWallet.ui.theme.AppColors
import com.osscabinet.ossWallet.ui.theme.AppTextStyles
import com.osscabinet.ossWallet.ui.util.CurrencyDisplaySpec
import com.osscabinet.ossWallet.ui.util.CurrencyPartStyles
import com.osscabinet.ossWallet.ui.util.CurrencyReprUi

@Composable
fun BudgetHeroCard(
    budget: Budget,
    progress: BudgetProgress,
    modifier: Modifier = Modifier,
    offset: Int = 0,
    onOffsetChange: ((Int) -> Unit)? = null
) {
    val accentColor = when (progress.alertLevel) {
        BudgetAlertLevel.EXCEEDED, BudgetAlertLevel.WARNING -> AppColors.error
        BudgetAlertLevel.NONE -> AppColors.primary
    }

    val (start, end) = TimelineService.getDateRange(budget.timelineRange)
    val sdf = remember { java.text.SimpleDateFormat("MMM d, yyyy", java.util.Locale.getDefault()) }
    val periodLabel = if (start != null && end != null) {
        "${sdf.format(java.util.Date(start))} - ${sdf.format(java.util.Date(end))}"
    } else budget.timelineRange.toDisplayString()

    val paceStatus = BudgetService.paceStatus(progress)
    val dailyAllowance = BudgetService.remainingDailyAllowance(progress, System.currentTimeMillis())

    val displayAmount = if (progress.alertLevel == BudgetAlertLevel.EXCEEDED) {
        progress.spent.subtract(progress.limit)
    } else {
        progress.remaining
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
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Period Stepper
            if (onOffsetChange != null) {
                Surface(
                    color = AppColors.surfaceContainerHigh.copy(alpha = 0.5f),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 4.dp, vertical = 2.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        IconButton(
                            onClick = { onOffsetChange(offset - 1) },
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(
                                Icons.AutoMirrored.Rounded.KeyboardArrowLeft,
                                contentDescription = "Previous Period",
                                tint = AppColors.contentPrimary,
                                modifier = Modifier.size(20.dp)
                            )
                        }

                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = periodLabel,
                                style = AppTextStyles.caption.copy(fontWeight = FontWeight.Bold),
                                color = AppColors.contentPrimary
                            )
                            if (offset != 0) {
                                TextButton(
                                    onClick = { onOffsetChange(0) },
                                    modifier = Modifier.height(24.dp)
                                ) {
                                    Text(
                                        "Back to Current",
                                        style = AppTextStyles.caption.copy(fontSize = 10.sp, fontWeight = FontWeight.SemiBold),
                                        color = AppColors.primary
                                    )
                                }
                            }
                        }

                        IconButton(
                            onClick = { onOffsetChange(offset + 1) },
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(
                                Icons.AutoMirrored.Rounded.KeyboardArrowRight,
                                contentDescription = "Next Period",
                                tint = AppColors.contentPrimary,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }
            }

            // Hero Section: Label & Percentage Badge + Auto-Scaling Hero Amount
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (progress.alertLevel == BudgetAlertLevel.EXCEEDED) "OVER BUDGET" else "REMAINING",
                        style = AppTextStyles.caption.copy(
                            fontWeight = FontWeight.SemiBold,
                            letterSpacing = 1.5.sp
                        ),
                        color = AppColors.contentSecondary.copy(alpha = 0.6f)
                    )

                    Surface(
                        color = accentColor.copy(alpha = 0.1f),
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.dp, accentColor.copy(alpha = 0.2f))
                    ) {
                        Text(
                            text = "${(progress.percentage * 100).toInt()}%",
                            style = AppTextStyles.screenSubTitle.copy(
                                fontWeight = FontWeight.Black,
                                fontSize = 16.sp
                            ),
                            color = accentColor,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                        )
                    }
                }

                // Auto-scaling Hero Amount (mirroring AccountHeroCard pattern)
                var multiplier by remember(displayAmount, budget.currency) { mutableFloatStateOf(1f) }
                val symbolStyle = SpanStyle(
                    fontWeight = FontWeight.Normal,
                    color = AppColors.contentSecondary.copy(alpha = 0.4f),
                    fontSize = (18f * multiplier).sp
                )

                Text(
                    text = CurrencyReprUi.formatCurrencyAnnotated(
                        amount = displayAmount,
                        currencyCode = budget.currency,
                        spec = CurrencyDisplaySpec(useSemanticColor = false, shortenThreshold = -1),
                        partStyles = CurrencyPartStyles(symbol = symbolStyle)
                    ),
                    style = AppTextStyles.heroAmount.copy(
                        fontWeight = FontWeight.Bold,
                        fontSize = (36f * multiplier).sp,
                        letterSpacing = (-1.5).sp
                    ),
                    color = if (progress.alertLevel == BudgetAlertLevel.EXCEEDED) AppColors.error else AppColors.contentPrimary,
                    maxLines = 1,
                    softWrap = false,
                    overflow = TextOverflow.Clip,
                    onTextLayout = { textLayoutResult ->
                        if (textLayoutResult.didOverflowWidth && multiplier > 0.4f) {
                            multiplier *= 0.85f
                        }
                    }
                )
            }

            // Pace Status & Daily Allowance
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                val paceColor = when (paceStatus) {
                    BudgetPaceStatus.OVER -> AppColors.error
                    BudgetPaceStatus.NEAR_LIMIT -> AppColors.error
                    BudgetPaceStatus.AHEAD_OF_PACE -> AppColors.accent
                    BudgetPaceStatus.ON_TRACK -> AppColors.primary
                }
                val paceLabel = when (paceStatus) {
                    BudgetPaceStatus.OVER -> "OVER BUDGET"
                    BudgetPaceStatus.NEAR_LIMIT -> "NEAR LIMIT"
                    BudgetPaceStatus.AHEAD_OF_PACE -> "AHEAD OF PACE"
                    BudgetPaceStatus.ON_TRACK -> "ON TRACK"
                }

                Surface(
                    color = paceColor.copy(alpha = 0.12f),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(
                        text = paceLabel,
                        style = AppTextStyles.caption.copy(fontWeight = FontWeight.Bold, fontSize = 10.sp),
                        color = paceColor,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }

                if (dailyAllowance != null) {
                    Text(
                        text = "${CurrencyReprUi.formatCurrencyAnnotated(dailyAllowance, budget.currency, spec = CurrencyDisplaySpec(useSemanticColor = false))} / day left",
                        style = AppTextStyles.caption.copy(fontWeight = FontWeight.Medium),
                        color = AppColors.contentSecondary.copy(alpha = 0.8f)
                    )
                }
            }

            // Gauge & Dual Readout
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Box(modifier = Modifier.fillMaxWidth()) {
                    LinearProgressIndicator(
                        progress = { progress.percentage.coerceIn(0f, 1f) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(10.dp),
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
