package com.osscabinet.ossWallet.ui.screens.loans.detail.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.rounded.AccountBalance
import androidx.compose.material.icons.rounded.Event
import androidx.compose.material.icons.rounded.NorthEast
import androidx.compose.material.icons.rounded.Percent
import androidx.compose.material.icons.rounded.SouthWest
import androidx.compose.material.icons.rounded.Today
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.osscabinet.ossWallet.domain.model.Loan
import com.osscabinet.ossWallet.domain.model.LoanStatus
import com.osscabinet.ossWallet.domain.model.LoanSummary
import com.osscabinet.ossWallet.domain.model.LoanType
import com.osscabinet.ossWallet.domain.model.timeline.TimelineRange
import com.osscabinet.ossWallet.domain.service.timeline.TimelineService
import com.osscabinet.ossWallet.ui.components.cards.AeonExpressiveCard
import com.osscabinet.ossWallet.ui.components.visualization.AeonSparkline
import com.osscabinet.ossWallet.ui.theme.AeonTheme
import com.osscabinet.ossWallet.ui.theme.AppColors
import com.osscabinet.ossWallet.ui.theme.AppTextStyles
import com.osscabinet.ossWallet.ui.theme.OssWalletTheme
import com.osscabinet.ossWallet.ui.theme.WarningColor
import com.osscabinet.ossWallet.ui.util.CurrencyDisplaySpec
import com.osscabinet.ossWallet.ui.util.CurrencyPartStyles
import com.osscabinet.ossWallet.ui.util.CurrencyReprUi
import java.math.BigDecimal

@Composable
fun LoanHeroCard(
    loan: Loan,
    summary: LoanSummary,
    modifier: Modifier = Modifier,
    offset: Int = 0,
    onOffsetChange: ((Int) -> Unit)? = null,
    history: List<LoanSummary> = emptyList()
) {
    val accentColor = if (loan.loanType == LoanType.LENT) AppColors.primary else WarningColor

    val shiftedRange = if (offset != 0) TimelineService.shiftRange(loan.timelineRange, offset) else loan.timelineRange
    val (start, end) = TimelineService.getDateRange(shiftedRange)
    val sdf = remember { java.text.SimpleDateFormat("MMM d, yyyy", java.util.Locale.getDefault()) }
    val periodLabel = if (start != null && end != null) {
        "${sdf.format(java.util.Date(start))} - ${sdf.format(java.util.Date(end))}"
    } else shiftedRange.toDisplayString()

    val progress = if (loan.principalAmount > BigDecimal.ZERO)
        (summary.totalPaid.divide(
            loan.principalAmount,
            4,
            java.math.RoundingMode.HALF_UP
        )).toFloat().coerceIn(0f, 1f)
    else 0f

    val settlementDate = loan.expectedSettlementDate
    val isSettled = summary.remainingBalance == BigDecimal.ZERO || loan.status == LoanStatus.SETTLED
    val now = System.currentTimeMillis()
    val isOverdue = !isSettled && settlementDate != null && settlementDate < now

    val healthColor = when {
        isSettled -> AppColors.accent
        isOverdue -> AppColors.error
        else -> AppColors.primary
    }

    val healthLabel = when {
        isSettled -> "FULLY SETTLED"
        isOverdue -> "SETTLEMENT OVERDUE"
        else -> "ON TRACK"
    }

    AeonExpressiveCard(
        title = loan.name,
        subtitle = "${loan.loanType.name} — ${shiftedRange.toDisplayString()}",
        icon = Icons.Rounded.AccountBalance,
        accentColor = accentColor,
        modifier = modifier
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 8.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // 1. Period Stepper (Hidden for AllTime open-ended loans)
            if (onOffsetChange != null && loan.timelineRange !is TimelineRange.AllTime) {
                val isOffsetActive = offset != 0
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Surface(
                        color = AppColors.surfaceContainerHigh.copy(alpha = if (isOffsetActive) 0.5f else 0.3f),
                        shape = RoundedCornerShape(16.dp),
                        border = BorderStroke(
                            width = 0.5.dp,
                            color = if (isOffsetActive) AppColors.primary.copy(alpha = 0.35f) else AppColors.divider.copy(alpha = 0.12f)
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(42.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(horizontal = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Surface(
                                onClick = { onOffsetChange(offset - 1) },
                                shape = CircleShape,
                                color = AppColors.cardSurface.copy(alpha = 0.7f),
                                modifier = Modifier.size(28.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        Icons.AutoMirrored.Rounded.KeyboardArrowLeft,
                                        contentDescription = "Previous Period",
                                        tint = AppColors.contentPrimary,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }

                            Text(
                                text = periodLabel,
                                style = AppTextStyles.caption.copy(
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 12.sp
                                ),
                                color = AppColors.contentPrimary
                            )

                            Surface(
                                onClick = { onOffsetChange(offset + 1) },
                                shape = CircleShape,
                                color = AppColors.cardSurface.copy(alpha = 0.7f),
                                modifier = Modifier.size(28.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        Icons.AutoMirrored.Rounded.KeyboardArrowRight,
                                        contentDescription = "Next Period",
                                        tint = AppColors.contentPrimary,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }
                        }
                    }

                    // Reset Button centered below the timeline stepper pill
                    if (isOffsetActive) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp),
                                modifier = Modifier
                                    .clip(RoundedCornerShape(16.dp))
                                    .background(AppColors.primary.copy(alpha = 0.12f))
                                    .border(0.5.dp, AppColors.primary.copy(alpha = 0.25f), RoundedCornerShape(16.dp))
                                    .clickable { onOffsetChange(0) }
                                    .padding(horizontal = 10.dp, vertical = 3.dp)
                            ) {
                                Icon(
                                    Icons.Rounded.Today,
                                    contentDescription = "Reset to Current Period",
                                    tint = AppColors.primary,
                                    modifier = Modifier.size(11.dp)
                                )
                                Text(
                                    text = "Reset to Current",
                                    style = AppTextStyles.caption.copy(
                                        fontSize = 10.5.sp,
                                        fontWeight = FontWeight.Bold,
                                        letterSpacing = 0.2.sp
                                    ),
                                    color = AppColors.primary
                                )
                            }
                        }
                    }
                }
            }

            // 2. 6-period Repayments Sparkline Trendline
            val trendHistory = remember(history, summary) {
                val items = if (history.isNotEmpty()) history else listOf(summary)
                val missing = 6 - items.size
                if (missing > 0) {
                    val firstItem = items.first()
                    val padded = (missing downTo 1).map { step ->
                        val paddedRange = TimelineService.shiftRange(firstItem.loan.timelineRange, -step)
                        val paddedLoan = firstItem.loan.copy(timelineRange = paddedRange)
                        LoanSummary(
                            loan = paddedLoan,
                            totalPaid = BigDecimal.ZERO,
                            remainingBalance = paddedLoan.principalAmount,
                            interestAccrued = BigDecimal.ZERO
                        )
                    }
                    padded + items
                } else items
            }
            val sparklinePoints = remember(trendHistory) {
                trendHistory.map { it.totalPaid }
            }

            val sdfMonth = remember { java.text.SimpleDateFormat("MMM yyyy", java.util.Locale.getDefault()) }
            if (sparklinePoints.size >= 2) {
                var selectedHistoryIndex by remember { mutableStateOf<Int?>(null) }
                val activeHistoryItem = selectedHistoryIndex?.let { trendHistory.getOrNull(it) }

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 2.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = if (activeHistoryItem != null) {
                                val (hStart, hEnd) = TimelineService.getDateRange(activeHistoryItem.loan.timelineRange)
                                if (hStart != null && hEnd != null) {
                                    sdfMonth.format(java.util.Date(hStart)).uppercase()
                                } else activeHistoryItem.loan.timelineRange.toDisplayString().uppercase()
                            } else "REPAYMENT TREND",
                            style = AppTextStyles.caption.copy(
                                fontWeight = FontWeight.Bold,
                                fontSize = 10.sp,
                                letterSpacing = 1.sp
                            ),
                            color = if (activeHistoryItem != null) AppColors.primary else AppColors.contentSecondary.copy(alpha = 0.6f)
                        )

                        if (activeHistoryItem != null) {
                            Text(
                                text = "${CurrencyReprUi.formatCurrencyAnnotated(activeHistoryItem.totalPaid, loan.currency, spec = CurrencyDisplaySpec(useSemanticColor = false))} settled",
                                style = AppTextStyles.caption.copy(fontWeight = FontWeight.Bold, fontSize = 11.sp),
                                color = AppColors.contentPrimary
                            )
                        }
                    }

                    AeonSparkline(
                        points = sparklinePoints,
                        lineColor = accentColor,
                        selectedIndex = selectedHistoryIndex,
                        onSelectedIndexChange = { selectedHistoryIndex = it },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                    )
                }
            }

            // 3. Health & Velocity Banner
            Surface(
                color = AppColors.surfaceContainerHigh.copy(alpha = 0.45f),
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(0.5.dp, AppColors.divider.copy(alpha = 0.12f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 10.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        color = healthColor.copy(alpha = 0.12f),
                        shape = RoundedCornerShape(8.dp),
                        border = BorderStroke(0.5.dp, healthColor.copy(alpha = 0.25f))
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(5.dp),
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(6.dp)
                                    .background(healthColor, CircleShape)
                            )
                            Text(
                                text = healthLabel,
                                style = AppTextStyles.caption.copy(fontWeight = FontWeight.Bold, fontSize = 10.sp, letterSpacing = 0.5.sp),
                                color = healthColor
                            )
                        }
                    }

                    if (settlementDate != null && !isSettled) {
                        val daysDiff = ((settlementDate - now) / (1000L * 60 * 60 * 24)).toInt()
                        val subtext = when {
                            daysDiff < 0 -> "${-daysDiff}d overdue"
                            daysDiff == 0 -> "Due today"
                            else -> "${daysDiff}d remaining"
                        }
                        Text(
                            text = subtext,
                            style = AppTextStyles.caption.copy(
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 11.sp
                            ),
                            color = if (daysDiff <= 0) AppColors.error else AppColors.contentPrimary
                        )
                    } else if (summary.interestAccrued > BigDecimal.ZERO) {
                        Text(
                            text = "+${CurrencyReprUi.formatCurrencyAnnotated(summary.interestAccrued, loan.currency, spec = CurrencyDisplaySpec(useSemanticColor = false))} interest",
                            style = AppTextStyles.caption.copy(fontWeight = FontWeight.SemiBold, fontSize = 11.sp),
                            color = AppColors.contentSecondary
                        )
                    }
                }
            }

            // 4. Hero Section: Label & Percentage Badge + Auto-Scaling Hero Amount
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "REMAINING BALANCE",
                        style = AppTextStyles.caption.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 11.sp,
                            letterSpacing = 1.2.sp
                        ),
                        color = AppColors.contentSecondary.copy(alpha = 0.7f)
                    )

                    Surface(
                        color = accentColor.copy(alpha = 0.12f),
                        shape = RoundedCornerShape(20.dp),
                        border = BorderStroke(1.dp, accentColor.copy(alpha = 0.25f))
                    ) {
                        Text(
                            text = "${(progress * 100).toInt()}% settled",
                            style = AppTextStyles.caption.copy(
                                fontWeight = FontWeight.Bold,
                                fontSize = 11.sp
                            ),
                            color = accentColor,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                        )
                    }
                }

                // Auto-scaling Hero Amount
                var multiplier by remember(summary.remainingBalance, loan.currency) { mutableFloatStateOf(1f) }
                val symbolStyle = SpanStyle(
                    fontWeight = FontWeight.Light,
                    color = AppColors.contentSecondary.copy(alpha = 0.5f),
                    fontSize = (22f * multiplier).sp
                )

                Text(
                    text = CurrencyReprUi.formatCurrencyAnnotated(
                        amount = summary.remainingBalance,
                        currencyCode = loan.currency,
                        spec = CurrencyDisplaySpec(useSemanticColor = false, shortenThreshold = -1),
                        partStyles = CurrencyPartStyles(symbol = symbolStyle)
                    ),
                    style = AppTextStyles.heroAmountLarge.copy(
                        fontSize = (34f * multiplier).sp,
                        letterSpacing = (-1).sp,
                        lineHeight = (40f * multiplier).sp
                    ),
                    color = AppColors.contentPrimary,
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

            // 5. Progress Gauge Bar & Dual Readout
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Box(modifier = Modifier.fillMaxWidth()) {
                    LinearProgressIndicator(
                        progress = { progress },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(8.dp),
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
                            style = AppTextStyles.caption.copy(fontWeight = FontWeight.Medium),
                            color = AppColors.contentSecondary.copy(alpha = 0.8f)
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
                        style = AppTextStyles.caption.copy(fontWeight = FontWeight.Medium),
                        color = AppColors.contentSecondary.copy(alpha = 0.6f)
                    )
                }
            }

            // 6. Adaptive Multi-Metric Grid
            val dateFmt = remember { java.text.SimpleDateFormat("MMM d, yyyy", java.util.Locale.getDefault()) }

            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    LoanMetricItem(
                        label = if (loan.loanType == LoanType.LENT) "LENT TO" else "BORROWED FROM",
                        value = loan.lenderOrBorrowerName,
                        icon = if (loan.loanType == LoanType.LENT) Icons.Rounded.NorthEast else Icons.Rounded.SouthWest,
                        color = AppColors.primary,
                        modifier = Modifier.weight(1f)
                    )
                    if (loan.interestRate != null) {
                        LoanMetricItem(
                            label = "INTEREST RATE",
                            value = "${loan.interestRate}%",
                            icon = Icons.Rounded.Percent,
                            color = AppColors.error,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }

                if (summary.interestAccrued > BigDecimal.ZERO || settlementDate != null) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        if (summary.interestAccrued > BigDecimal.ZERO) {
                            LoanMetricItem(
                                label = "INTEREST ACCRUED",
                                value = CurrencyReprUi.formatCurrencyAnnotated(
                                    summary.interestAccrued,
                                    loan.currency,
                                    spec = CurrencyDisplaySpec(useSemanticColor = false)
                                ).text,
                                icon = Icons.Rounded.Percent,
                                color = WarningColor,
                                modifier = Modifier.weight(1f)
                            )
                        }
                        if (settlementDate != null) {
                            LoanMetricItem(
                                label = "DUE DATE",
                                value = dateFmt.format(java.util.Date(settlementDate)),
                                icon = Icons.Rounded.Event,
                                color = if (isOverdue) AppColors.error else AppColors.primary,
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
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
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Preview(showBackground = true, name = "Loan Hero Card - Borrowed with Interest")
@Composable
private fun LoanHeroCardBorrowedPreview() {
    val sampleLoan = Loan(
        id = 1L,
        name = "Home Mortgage",
        principalAmount = BigDecimal("250000.00"),
        currency = "USD",
        interestRate = 3.5f,
        startDate = System.currentTimeMillis() - 1000000000L,
        lenderOrBorrowerName = "Chase Bank",
        loanType = LoanType.BORROWED,
        linkedAccountId = "acc1",
        expectedSettlementDate = System.currentTimeMillis() + 86400000L * 45,
        status = LoanStatus.ACTIVE,
        profileId = 1
    )
    val sampleSummary = LoanSummary(
        loan = sampleLoan,
        totalPaid = BigDecimal("35000.00"),
        remainingBalance = BigDecimal("215000.00"),
        interestAccrued = BigDecimal("1245.50")
    )
    val sampleHistory = listOf(
        BigDecimal("5000.00"),
        BigDecimal("6000.00"),
        BigDecimal("5500.00"),
        BigDecimal("6200.00"),
        BigDecimal("6100.00"),
        BigDecimal("6200.00")
    ).map { paid ->
        LoanSummary(
            loan = sampleLoan,
            totalPaid = paid,
            remainingBalance = sampleLoan.principalAmount - paid,
            interestAccrued = BigDecimal("120.00")
        )
    }

    OssWalletTheme {
        LoanHeroCard(
            loan = sampleLoan,
            summary = sampleSummary,
            offset = 0,
            onOffsetChange = {},
            history = sampleHistory
        )
    }
}

@Preview(showBackground = true, name = "Loan Hero Card - Lent On Track")
@Composable
private fun LoanHeroCardLentPreview() {
    val sampleLoan = Loan(
        id = 2L,
        name = "Car Loan to Alex",
        principalAmount = BigDecimal("12000.00"),
        currency = "USD",
        interestRate = null,
        startDate = System.currentTimeMillis() - 500000000L,
        lenderOrBorrowerName = "Alex Miller",
        loanType = LoanType.LENT,
        linkedAccountId = "acc2",
        expectedSettlementDate = System.currentTimeMillis() + 86400000L * 120,
        status = LoanStatus.ACTIVE,
        profileId = 1
    )
    val sampleSummary = LoanSummary(
        loan = sampleLoan,
        totalPaid = BigDecimal("4800.00"),
        remainingBalance = BigDecimal("7200.00"),
        interestAccrued = BigDecimal.ZERO
    )

    OssWalletTheme {
        LoanHeroCard(
            loan = sampleLoan,
            summary = sampleSummary,
            offset = 0,
            onOffsetChange = {}
        )
    }
}

@Preview(showBackground = true, name = "Loan Hero Card - Fully Settled")
@Composable
private fun LoanHeroCardSettledPreview() {
    val sampleLoan = Loan(
        id = 3L,
        name = "Equipment Purchase",
        principalAmount = BigDecimal("5000.00"),
        currency = "USD",
        interestRate = 2.0f,
        startDate = System.currentTimeMillis() - 2000000000L,
        lenderOrBorrowerName = "Tech Equipment Corp",
        loanType = LoanType.BORROWED,
        linkedAccountId = "acc1",
        expectedSettlementDate = System.currentTimeMillis() - 86400000L * 10,
        status = LoanStatus.SETTLED,
        profileId = 1
    )
    val sampleSummary = LoanSummary(
        loan = sampleLoan,
        totalPaid = BigDecimal("5000.00"),
        remainingBalance = BigDecimal.ZERO,
        interestAccrued = BigDecimal("100.00")
    )

    OssWalletTheme {
        LoanHeroCard(
            loan = sampleLoan,
            summary = sampleSummary,
            offset = 0,
            onOffsetChange = {}
        )
    }
}
