package com.osscabinet.ossWallet.ui.screens.loans.detail.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CalendarMonth
import androidx.compose.material.icons.rounded.ExpandLess
import androidx.compose.material.icons.rounded.ExpandMore
import androidx.compose.material.icons.rounded.PieChart
import androidx.compose.material.icons.rounded.Schedule
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.osscabinet.ossWallet.domain.model.AmortizationEntry
import com.osscabinet.ossWallet.domain.model.AmortizationScheduleSummary
import com.osscabinet.ossWallet.domain.model.AmortizationYearlySummary
import com.osscabinet.ossWallet.domain.model.Loan
import com.osscabinet.ossWallet.domain.service.management.LoanService
import com.osscabinet.ossWallet.ui.theme.AppColors
import com.osscabinet.ossWallet.ui.theme.AppTextStyles
import java.math.BigDecimal
import java.math.RoundingMode
import java.text.SimpleDateFormat
import java.time.Instant
import java.time.ZoneId
import java.util.Date
import java.util.Locale

enum class AmortizationViewMode {
    MONTHLY,
    YEARLY
}

@Composable
fun LoanAmortizationScheduleSection(
    loan: Loan,
    monthlyPayment: BigDecimal,
    modifier: Modifier = Modifier
) {
    var viewMode by remember { mutableStateOf(AmortizationViewMode.MONTHLY) }
    var showAllPeriods by remember { mutableStateOf(false) }

    val monthlyEntries = remember(loan, monthlyPayment) {
        LoanService.getAmortizationSchedule(loan, monthlyPayment, loan.startDate)
    }

    if (monthlyEntries.isEmpty()) return

    val scheduleSummary = remember(loan, monthlyEntries, monthlyPayment) {
        LoanService.calculateScheduleSummary(loan, monthlyEntries, monthlyPayment)
    }

    val yearlySummaries = remember(monthlyEntries) {
        LoanService.getAmortizationYearlySummaries(monthlyEntries)
    }

    val currentYearMonth = remember {
        val zone = ZoneId.systemDefault()
        val now = Instant.now().atZone(zone)
        now.year * 100 + now.monthValue
    }

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // --- Executive Summary Card ---
        AmortizationSummaryCard(
            summary = scheduleSummary,
            currency = loan.currency
        )

        // --- View Mode Selector (Monthly vs Yearly) ---
        AmortizationViewModeSelector(
            selectedMode = viewMode,
            onModeSelected = { viewMode = it },
            hasMultipleYears = yearlySummaries.size > 1
        )

        // --- Schedule Rows ---
        when (viewMode) {
            AmortizationViewMode.MONTHLY -> {
                val displayEntries = if (showAllPeriods) monthlyEntries else monthlyEntries.take(12)
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    displayEntries.forEach { entry ->
                        val entryYearMonth = remember(entry.date) {
                            val zone = ZoneId.systemDefault()
                            val dt = Instant.ofEpochMilli(entry.date).atZone(zone)
                            dt.year * 100 + dt.monthValue
                        }
                        val isCurrentPeriod = (entryYearMonth == currentYearMonth)

                        AmortizationMonthlyRowCard(
                            entry = entry,
                            currency = loan.currency,
                            isCurrentPeriod = isCurrentPeriod,
                            totalLoanPrincipal = loan.principalAmount
                        )
                    }

                    if (monthlyEntries.size > 12) {
                        TextButton(
                            onClick = { showAllPeriods = !showAllPeriods },
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 4.dp)
                        ) {
                            Text(
                                if (showAllPeriods) "Show fewer periods"
                                else "Show all ${monthlyEntries.size} monthly periods",
                                style = AppTextStyles.caption.copy(fontWeight = FontWeight.SemiBold),
                                color = AppColors.primary
                            )
                        }
                    }
                }
            }

            AmortizationViewMode.YEARLY -> {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    yearlySummaries.forEach { yearly ->
                        AmortizationYearlyRowCard(
                            summary = yearly,
                            currency = loan.currency
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun AmortizationSummaryCard(
    summary: AmortizationScheduleSummary,
    currency: String,
    modifier: Modifier = Modifier
) {
    val principalRatio = remember(summary) {
        if (summary.totalCost > BigDecimal.ZERO) {
            summary.totalPrincipal.divide(summary.totalCost, 4, RoundingMode.HALF_UP).toFloat()
        } else 1f
    }
    val interestRatio = (1f - principalRatio).coerceAtLeast(0f)

    Surface(
        color = AppColors.cardSurfaceProminent,
        shape = RoundedCornerShape(20.dp),
        border = BorderStroke(0.5.dp, AppColors.divider.copy(alpha = 0.15f)),
        modifier = modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Rounded.PieChart,
                        contentDescription = null,
                        tint = AppColors.primary,
                        modifier = Modifier.size(18.dp)
                    )
                    Text(
                        "PAYMENT SUMMARY",
                        style = AppTextStyles.caption.copy(
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.sp
                        ),
                        color = AppColors.contentSecondary
                    )
                }

                Surface(
                    color = AppColors.primary.copy(alpha = 0.12f),
                    shape = CircleShape
                ) {
                    Text(
                        "${summary.totalPeriods} payments",
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                        style = AppTextStyles.caption.copy(
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 11.sp
                        ),
                        color = AppColors.primary
                    )
                }
            }

            // --- Key Numbers Row ---
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                SummaryMetricItem(
                    label = "TOTAL COST",
                    value = "${summary.totalCost.setScale(2, RoundingMode.HALF_UP).toPlainString()} $currency",
                    color = AppColors.contentPrimary
                )
                SummaryMetricItem(
                    label = "TOTAL INTEREST",
                    value = "${summary.totalInterest.setScale(2, RoundingMode.HALF_UP).toPlainString()} $currency",
                    color = AppColors.error
                )
                SummaryMetricItem(
                    label = "EST. MONTHLY",
                    value = "${summary.monthlyPayment.setScale(2, RoundingMode.HALF_UP).toPlainString()} $currency",
                    color = AppColors.primary
                )
            }

            // --- Proportional Principal vs Interest Bar ---
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(8.dp)
                        .clip(CircleShape)
                        .background(AppColors.divider.copy(alpha = 0.15f))
                ) {
                    Box(
                        modifier = Modifier
                            .weight(principalRatio.coerceAtLeast(0.02f))
                            .height(8.dp)
                            .background(AppColors.primary)
                    )
                    if (interestRatio > 0.001f) {
                        Box(
                            modifier = Modifier
                                .weight(interestRatio)
                                .height(8.dp)
                                .background(AppColors.error)
                        )
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .clip(CircleShape)
                                .background(AppColors.primary)
                        )
                        Text(
                            "Principal ${(principalRatio * 100).toInt()}%",
                            style = AppTextStyles.caption.copy(fontSize = 11.sp),
                            color = AppColors.contentSecondary
                        )
                    }

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .clip(CircleShape)
                                .background(AppColors.error)
                        )
                        Text(
                            "Interest ${(interestRatio * 100).toInt()}%",
                            style = AppTextStyles.caption.copy(fontSize = 11.sp),
                            color = AppColors.contentSecondary
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SummaryMetricItem(
    label: String,
    value: String,
    color: Color
) {
    Column {
        Text(
            label,
            style = AppTextStyles.caption.copy(
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 0.5.sp
            ),
            color = AppColors.contentSecondary
        )
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            value,
            style = AppTextStyles.managementItemTitle.copy(
                fontWeight = FontWeight.Bold,
                fontSize = 13.sp
            ),
            color = color
        )
    }
}

@Composable
private fun AmortizationViewModeSelector(
    selectedMode: AmortizationViewMode,
    onModeSelected: (AmortizationViewMode) -> Unit,
    hasMultipleYears: Boolean
) {
    if (!hasMultipleYears) return

    Surface(
        color = AppColors.cardSurfaceProminent.copy(alpha = 0.5f),
        shape = CircleShape,
        border = BorderStroke(0.5.dp, AppColors.divider.copy(alpha = 0.12f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(4.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            ModeChip(
                text = "Monthly Breakdowns",
                icon = Icons.Rounded.CalendarMonth,
                isSelected = selectedMode == AmortizationViewMode.MONTHLY,
                onClick = { onModeSelected(AmortizationViewMode.MONTHLY) },
                modifier = Modifier.weight(1f)
            )
            ModeChip(
                text = "Yearly Rollup",
                icon = Icons.Rounded.Schedule,
                isSelected = selectedMode == AmortizationViewMode.YEARLY,
                onClick = { onModeSelected(AmortizationViewMode.YEARLY) },
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
private fun ModeChip(
    text: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        color = if (isSelected) AppColors.primary else Color.Transparent,
        shape = CircleShape,
        modifier = modifier.clickable { onClick() }
    ) {
        Row(
            modifier = Modifier.padding(vertical = 8.dp, horizontal = 12.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = if (isSelected) AppColors.onPrimary else AppColors.contentSecondary,
                modifier = Modifier.size(14.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = text,
                style = AppTextStyles.caption.copy(
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                    fontSize = 12.sp
                ),
                color = if (isSelected) AppColors.onPrimary else AppColors.contentSecondary
            )
        }
    }
}

@Composable
private fun AmortizationMonthlyRowCard(
    entry: AmortizationEntry,
    currency: String,
    isCurrentPeriod: Boolean,
    totalLoanPrincipal: BigDecimal
) {
    var expanded by remember { mutableStateOf(false) }

    val dateStr = remember(entry.date) {
        SimpleDateFormat("MMM yyyy", Locale.getDefault()).format(Date(entry.date))
    }

    val principalRatio = remember(entry) {
        if (entry.payment > BigDecimal.ZERO) {
            entry.principal.divide(entry.payment, 4, RoundingMode.HALF_UP).toFloat()
        } else 1f
    }
    val interestRatio = (1f - principalRatio).coerceAtLeast(0f)

    Surface(
        color = if (isCurrentPeriod) AppColors.primary.copy(alpha = 0.08f) else AppColors.cardSurfaceProminent,
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(
            if (isCurrentPeriod) 1.dp else 0.5.dp,
            if (isCurrentPeriod) AppColors.primary else AppColors.divider.copy(alpha = 0.12f)
        ),
        modifier = Modifier
            .fillMaxWidth()
            .clickable { expanded = !expanded }
            .animateContentSize()
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // --- Header Row ---
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Surface(
                        color = if (isCurrentPeriod) AppColors.primary else AppColors.primary.copy(alpha = 0.12f),
                        shape = CircleShape,
                        modifier = Modifier.size(28.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text(
                                "#${entry.period}",
                                style = AppTextStyles.caption.copy(
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 11.sp
                                ),
                                color = if (isCurrentPeriod) AppColors.onPrimary else AppColors.primary
                            )
                        }
                    }

                    Column {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(
                                dateStr,
                                style = AppTextStyles.managementItemTitle.copy(
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp
                                )
                            )
                            if (isCurrentPeriod) {
                                Surface(
                                    color = AppColors.primary,
                                    shape = CircleShape
                                ) {
                                    Text(
                                        "CURRENT",
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                        style = AppTextStyles.caption.copy(
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 9.sp
                                        ),
                                        color = AppColors.onPrimary
                                    )
                                }
                            }
                        }
                    }
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        "${entry.payment.setScale(2, RoundingMode.HALF_UP).toPlainString()} $currency",
                        style = AppTextStyles.managementItemTitle.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp
                        ),
                        color = AppColors.contentPrimary
                    )
                    Icon(
                        imageVector = if (expanded) Icons.Rounded.ExpandLess else Icons.Rounded.ExpandMore,
                        contentDescription = null,
                        tint = AppColors.contentSecondary,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            // --- Micro Proportional Bar ---
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(4.dp)
                    .clip(CircleShape)
                    .background(AppColors.divider.copy(alpha = 0.15f))
            ) {
                Box(
                    modifier = Modifier
                        .weight(principalRatio.coerceAtLeast(0.02f))
                        .height(4.dp)
                        .background(AppColors.primary)
                )
                if (interestRatio > 0.001f) {
                    Box(
                        modifier = Modifier
                            .weight(interestRatio)
                            .height(4.dp)
                            .background(AppColors.error)
                    )
                }
            }

            // --- Core Metrics Row ---
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                RowMetricItem(
                    label = "PRINCIPAL",
                    value = "${entry.principal.setScale(2, RoundingMode.HALF_UP).toPlainString()} $currency",
                    color = AppColors.primary
                )
                RowMetricItem(
                    label = "INTEREST",
                    value = "${entry.interest.setScale(2, RoundingMode.HALF_UP).toPlainString()} $currency",
                    color = AppColors.error
                )
                RowMetricItem(
                    label = "BALANCE",
                    value = "${entry.balance.setScale(2, RoundingMode.HALF_UP).toPlainString()} $currency",
                    color = AppColors.contentSecondary
                )
            }

            // --- Expandable Accordion ---
            AnimatedVisibility(
                visible = expanded,
                enter = fadeIn() + expandVertically(),
                exit = fadeOut() + shrinkVertically()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp)
                        .background(
                            AppColors.divider.copy(alpha = 0.06f),
                            shape = RoundedCornerShape(10.dp)
                        )
                        .padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    val remainingPct = if (totalLoanPrincipal > BigDecimal.ZERO) {
                        entry.balance.divide(totalLoanPrincipal, 4, RoundingMode.HALF_UP).multiply(BigDecimal(100)).toInt()
                    } else 0

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            "Remaining Principal",
                            style = AppTextStyles.caption,
                            color = AppColors.contentSecondary
                        )
                        Text(
                            "$remainingPct% of total",
                            style = AppTextStyles.caption.copy(fontWeight = FontWeight.Bold),
                            color = AppColors.contentPrimary
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun RowMetricItem(
    label: String,
    value: String,
    color: Color
) {
    Column {
        Text(
            label,
            style = AppTextStyles.caption.copy(
                fontSize = 9.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 0.5.sp
            ),
            color = AppColors.contentSecondary
        )
        Text(
            value,
            style = AppTextStyles.caption.copy(
                fontWeight = FontWeight.Bold,
                fontSize = 12.sp
            ),
            color = color
        )
    }
}

@Composable
private fun AmortizationYearlyRowCard(
    summary: AmortizationYearlySummary,
    currency: String
) {
    Surface(
        color = AppColors.cardSurfaceProminent,
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(0.5.dp, AppColors.divider.copy(alpha = 0.12f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.CalendarMonth,
                        contentDescription = null,
                        tint = AppColors.primary,
                        modifier = Modifier.size(18.dp)
                    )
                    Text(
                        "Year ${summary.year}",
                        style = AppTextStyles.managementItemTitle.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp
                        )
                    )
                }

                Text(
                    "Paid: ${summary.totalPayment.setScale(2, RoundingMode.HALF_UP).toPlainString()} $currency",
                    style = AppTextStyles.caption.copy(
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp
                    ),
                    color = AppColors.contentPrimary
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                RowMetricItem(
                    label = "PRINCIPAL PAID",
                    value = "${summary.totalPrincipal.setScale(2, RoundingMode.HALF_UP).toPlainString()} $currency",
                    color = AppColors.primary
                )
                RowMetricItem(
                    label = "INTEREST PAID",
                    value = "${summary.totalInterest.setScale(2, RoundingMode.HALF_UP).toPlainString()} $currency",
                    color = AppColors.error
                )
                RowMetricItem(
                    label = "YEAR-END BALANCE",
                    value = "${summary.endingBalance.setScale(2, RoundingMode.HALF_UP).toPlainString()} $currency",
                    color = AppColors.contentSecondary
                )
            }
        }
    }
}
