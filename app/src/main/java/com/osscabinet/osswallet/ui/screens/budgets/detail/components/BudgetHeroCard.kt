package com.osscabinet.ossWallet.ui.screens.budgets.detail.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
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
import androidx.compose.material.icons.rounded.DonutLarge
import androidx.compose.material.icons.rounded.Today
import androidx.compose.material.icons.rounded.Warning
import androidx.compose.material3.Icon
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
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.osscabinet.ossWallet.domain.model.Budget
import com.osscabinet.ossWallet.domain.model.BudgetAlertLevel
import com.osscabinet.ossWallet.domain.model.BudgetPaceStatus
import com.osscabinet.ossWallet.domain.model.BudgetProgress
import com.osscabinet.ossWallet.domain.model.Category
import com.osscabinet.ossWallet.domain.model.Transaction
import com.osscabinet.ossWallet.domain.model.TransactionType
import com.osscabinet.ossWallet.domain.model.settings.BarChartGranularity
import com.osscabinet.ossWallet.domain.model.timeline.ThisPeriodType
import com.osscabinet.ossWallet.domain.model.timeline.TimelineRange
import com.osscabinet.ossWallet.domain.service.currency.ExchangeRateSnapshot
import com.osscabinet.ossWallet.domain.service.management.BudgetService
import com.osscabinet.ossWallet.domain.service.timeline.TimelineService
import com.osscabinet.ossWallet.ui.components.cards.AeonExpressiveCard
import com.osscabinet.ossWallet.ui.components.cards.CardSettingsChipGrid
import com.osscabinet.ossWallet.ui.components.cards.CardSettingsSection
import com.osscabinet.ossWallet.ui.components.cards.CardSettingsSegmentedRow
import com.osscabinet.ossWallet.ui.components.visualization.AeonPeriodComparisonSparkline
import com.osscabinet.ossWallet.ui.components.visualization.AeonSparkline
import com.osscabinet.ossWallet.ui.theme.AppColors
import com.osscabinet.ossWallet.ui.theme.AppTextStyles
import com.osscabinet.ossWallet.ui.theme.OssWalletTheme
import com.osscabinet.ossWallet.ui.util.CurrencyDisplaySpec
import com.osscabinet.ossWallet.ui.util.CurrencyPartStyles
import com.osscabinet.ossWallet.ui.util.CurrencyReprUi
import java.math.BigDecimal
import java.math.RoundingMode

/** Which shape the Budget Hero Card's TREND chart takes. Session-only state, not persisted. */
enum class BudgetTrendViewMode { PERIODIC, THIS_PERIOD }

@Composable
fun BudgetHeroCard(
    budget: Budget,
    progress: BudgetProgress,
    modifier: Modifier = Modifier,
    offset: Int = 0,
    onOffsetChange: ((Int) -> Unit)? = null,
    history: List<BudgetProgress> = emptyList(),
    transactions: List<Transaction> = emptyList(),
    exchangeRates: ExchangeRateSnapshot? = null,
    categories: List<Category> = emptyList(),
    // Only for previews/tests — real callers always start on Periodic; the user picks This
    // Period via the settings sheet, which has no other way to be pre-seeded for a screenshot.
    initialTrendViewMode: BudgetTrendViewMode = BudgetTrendViewMode.PERIODIC
) {
    val accentColor = when (progress.alertLevel) {
        BudgetAlertLevel.EXCEEDED, BudgetAlertLevel.WARNING -> AppColors.error
        BudgetAlertLevel.NONE -> AppColors.primary
    }

    var trendViewMode by remember { mutableStateOf(initialTrendViewMode) }
    var trendGranularity by remember { mutableStateOf(BarChartGranularity.DAY) }
    val supportsThisPeriod = budget.timelineRange !is TimelineRange.AllTime

    val shiftedRange = if (offset != 0) TimelineService.shiftRange(budget.timelineRange, offset) else budget.timelineRange
    val (start, end) = TimelineService.getDateRange(shiftedRange)
    val sdf = remember { java.text.SimpleDateFormat("MMM d, yyyy", java.util.Locale.getDefault()) }
    val periodLabel = if (start != null && end != null) {
        "${sdf.format(java.util.Date(start))} - ${sdf.format(java.util.Date(end))}"
    } else shiftedRange.toDisplayString()

    val paceStatus = BudgetService.paceStatus(progress)
    val dailyAllowance = BudgetService.remainingDailyAllowance(progress, System.currentTimeMillis())

    val displayAmount = if (progress.alertLevel == BudgetAlertLevel.EXCEEDED) {
        progress.spent.subtract(progress.limit)
    } else {
        progress.remaining
    }

    AeonExpressiveCard(
        title = budget.name,
        subtitle = "Budget: ${shiftedRange.toDisplayString()}",
        icon = Icons.Rounded.DonutLarge,
        accentColor = accentColor,
        modifier = modifier,
        settingsContent = if (supportsThisPeriod) {
            {
                CardSettingsSection("Trend") {
                    CardSettingsSegmentedRow(
                        options = BudgetTrendViewMode.entries,
                        selected = trendViewMode,
                        onSelected = { trendViewMode = it },
                        labelProvider = {
                            when (it) {
                                BudgetTrendViewMode.PERIODIC -> "Periodic"
                                BudgetTrendViewMode.THIS_PERIOD -> "This Period"
                            }
                        }
                    )
                    Text(
                        text = "GRANULARITY",
                        style = AppTextStyles.overlineLabel.copy(
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.2.sp,
                            fontSize = 10.sp
                        ),
                        color = AppColors.primary.copy(alpha = if (trendViewMode == BudgetTrendViewMode.THIS_PERIOD) 1f else 0.4f),
                        modifier = Modifier.padding(top = 4.dp)
                    )
                    CardSettingsChipGrid(
                        options = BarChartGranularity.entries,
                        selected = trendGranularity,
                        onSelected = { trendGranularity = it },
                        labelProvider = { it.displayName },
                        columns = 3,
                        enabled = trendViewMode == BudgetTrendViewMode.THIS_PERIOD
                    )
                }
            }
        } else null
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 8.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Period Stepper (Hidden for AllTime open-ended budgets)
            if (onOffsetChange != null && budget.timelineRange !is TimelineRange.AllTime) {
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

                    // Reset Button centered below and outside the timeline stepper pill
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

            // 6-month spending trendline from history
            val trendHistory = remember(history, progress) {
                val items = if (history.isNotEmpty()) history else listOf(progress)
                val missing = 6 - items.size
                if (missing > 0) {
                    val firstItem = items.first()
                    val padded = (missing downTo 1).map { step ->
                        val paddedRange = TimelineService.shiftRange(firstItem.budget.timelineRange, -step)
                        val paddedBudget = firstItem.budget.copy(timelineRange = paddedRange)
                        BudgetProgress(
                            budget = paddedBudget,
                            spent = BigDecimal.ZERO,
                            limit = paddedBudget.limitAmount,
                            remaining = paddedBudget.limitAmount,
                            percentage = 0f,
                            alertLevel = BudgetAlertLevel.NONE
                        )
                    }
                    padded + items
                } else items
            }
            val sparklinePoints = remember(trendHistory) {
                trendHistory.map { it.spent }
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
                                val (hStart, hEnd) = TimelineService.getDateRange(activeHistoryItem.budget.timelineRange)
                                if (hStart != null && hEnd != null) {
                                    sdfMonth.format(java.util.Date(hStart)).uppercase()
                                } else activeHistoryItem.budget.timelineRange.toDisplayString().uppercase()
                            } else "TREND",
                            style = AppTextStyles.caption.copy(
                                fontWeight = FontWeight.Bold,
                                fontSize = 10.sp,
                                letterSpacing = 1.sp
                            ),
                            color = if (activeHistoryItem != null) AppColors.primary else AppColors.contentSecondary.copy(alpha = 0.6f)
                        )

                        if (activeHistoryItem != null) {
                            Text(
                                text = "${CurrencyReprUi.formatCurrencyAnnotated(activeHistoryItem.spent, budget.currency, spec = CurrencyDisplaySpec(useSemanticColor = false))} spent",
                                style = AppTextStyles.caption.copy(fontWeight = FontWeight.Bold, fontSize = 11.sp),
                                color = AppColors.contentPrimary
                            )
                        } else if (supportsThisPeriod) {
                            Row(
                                modifier = Modifier
                                    .background(AppColors.surfaceContainerHigh.copy(alpha = 0.5f), RoundedCornerShape(10.dp))
                                    .padding(2.dp),
                                horizontalArrangement = Arrangement.spacedBy(2.dp)
                            ) {
                                listOf(
                                    BudgetTrendViewMode.PERIODIC to "Periodic",
                                    BudgetTrendViewMode.THIS_PERIOD to "This Period"
                                ).forEach { (mode, label) ->
                                    val selected = trendViewMode == mode
                                    Text(
                                        text = label,
                                        style = AppTextStyles.caption.copy(
                                            fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                                            fontSize = 10.sp
                                        ),
                                        color = if (selected) AppColors.onPrimary else AppColors.contentSecondary,
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(if (selected) accentColor else androidx.compose.ui.graphics.Color.Transparent)
                                            .clickable { trendViewMode = mode }
                                            .padding(horizontal = 8.dp, vertical = 4.dp)
                                    )
                                }
                            }
                        }
                    }

                    if (trendViewMode == BudgetTrendViewMode.THIS_PERIOD && supportsThisPeriod && exchangeRates != null) {
                        val periodTrend = remember(shiftedRange, transactions, trendGranularity, exchangeRates, categories, progress.spent, progress.plannedAmount) {
                            BudgetService.getThisPeriodTrend(
                                // The stepper's selected period, not the budget's own (unshifted)
                                // timelineRange — otherwise this always shows the real current period.
                                budget = budget.copy(timelineRange = shiftedRange),
                                transactions = transactions,
                                spent = progress.spent,
                                plannedAmount = progress.plannedAmount,
                                exchangeRates = exchangeRates,
                                baseCurrency = budget.currency,
                                now = System.currentTimeMillis(),
                                allCategories = categories,
                                granularity = trendGranularity
                            )
                        }
                        if (periodTrend != null && periodTrend.currentPoints.size >= 2) {
                            AeonPeriodComparisonSparkline(
                                currentPoints = periodTrend.currentPoints,
                                previousPoints = periodTrend.previousPoints,
                                totalBuckets = periodTrend.totalCurrentBuckets,
                                lineColor = accentColor,
                                plannedTailEnd = periodTrend.plannedTailEnd,
                                limit = progress.limit,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(60.dp)
                            )
                        }
                    } else {
                        AeonSparkline(
                            points = sparklinePoints,
                            lineColor = accentColor,
                            selectedIndex = selectedHistoryIndex,
                            onSelectedIndexChange = { selectedHistoryIndex = it },
                            limit = progress.limit,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(60.dp)
                        )
                    }
                }
            }

            // Hero Section: Label & Percentage Badge + Auto-Scaling Hero Amount
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (progress.alertLevel == BudgetAlertLevel.EXCEEDED) "EXCEEDED BY" else "REMAINING BUDGET",
                        style = AppTextStyles.caption.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 11.sp,
                            letterSpacing = 1.2.sp
                        ),
                        color = if (progress.alertLevel == BudgetAlertLevel.EXCEEDED) AppColors.error else AppColors.contentSecondary.copy(alpha = 0.7f)
                    )

                    Surface(
                        color = accentColor.copy(alpha = 0.12f),
                        shape = RoundedCornerShape(20.dp),
                        border = BorderStroke(1.dp, accentColor.copy(alpha = 0.25f))
                    ) {
                        Text(
                            text = "${(progress.percentage * 100).toInt()}% used",
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
                var multiplier by remember(displayAmount, budget.currency) { mutableFloatStateOf(1f) }
                val symbolStyle = SpanStyle(
                    fontWeight = FontWeight.Light,
                    color = AppColors.contentSecondary.copy(alpha = 0.5f),
                    fontSize = (22f * multiplier).sp
                )

                Text(
                    text = CurrencyReprUi.formatCurrencyAnnotated(
                        amount = displayAmount,
                        currencyCode = budget.currency,
                        spec = CurrencyDisplaySpec(useSemanticColor = false, shortenThreshold = -1),
                        partStyles = CurrencyPartStyles(symbol = symbolStyle)
                    ),
                    style = AppTextStyles.heroAmountLarge.copy(
                        fontSize = (34f * multiplier).sp,
                        letterSpacing = (-1).sp,
                        lineHeight = (40f * multiplier).sp
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

            // Pace Status & Daily Allowance Velocity Container
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
                color = AppColors.surfaceContainerHigh.copy(alpha = 0.4f),
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
                        color = paceColor.copy(alpha = 0.12f),
                        shape = RoundedCornerShape(8.dp),
                        border = BorderStroke(0.5.dp, paceColor.copy(alpha = 0.25f))
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(5.dp),
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(6.dp)
                                    .background(paceColor, CircleShape)
                            )
                            Text(
                                text = paceLabel,
                                style = AppTextStyles.caption.copy(fontWeight = FontWeight.Bold, fontSize = 10.sp, letterSpacing = 0.5.sp),
                                color = paceColor
                            )
                        }
                    }

                    if (dailyAllowance != null) {
                        Text(
                            text = "${CurrencyReprUi.formatCurrencyAnnotated(dailyAllowance, budget.currency, spec = CurrencyDisplaySpec(useSemanticColor = false))} / day",
                            style = AppTextStyles.caption.copy(fontWeight = FontWeight.SemiBold, fontSize = 11.sp),
                            color = AppColors.contentPrimary
                        )
                    }
                }
            }

            // Gauge & Dual Readout
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                // Planned extends the solid "spent" fill with a tinted segment up to spent+planned,
                // so the gauge shows the committed-but-not-yet-spent amount without affecting
                // percentage/alertLevel (which are based on spent alone).
                val plannedPercentage = remember(progress, budget.limitAmount) {
                    if (budget.limitAmount.signum() > 0) {
                        progress.spent.add(progress.plannedAmount)
                            .divide(budget.limitAmount, 4, RoundingMode.HALF_UP)
                            .toFloat()
                            .coerceIn(0f, 1f)
                    } else 0f
                }
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(8.dp)
                        .clip(RoundedCornerShape(50))
                        .background(accentColor.copy(alpha = 0.08f))
                ) {
                    if (progress.plannedAmount.signum() > 0) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth(plannedPercentage)
                                .fillMaxHeight()
                                .background(accentColor.copy(alpha = 0.30f))
                        )
                    }
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(progress.percentage.coerceIn(0f, 1f))
                            .fillMaxHeight()
                            .background(accentColor)
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

                // Planned: future-dated transactions + due planned payments in this period,
                // matching the budget's scope. Shown separately — not yet actually spent.
                if (progress.plannedAmount.signum() > 0) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(6.dp)
                                .background(AppColors.contentSecondary.copy(alpha = 0.4f), CircleShape)
                        )
                        Text(
                            text = buildAnnotatedString {
                                append(
                                    CurrencyReprUi.formatCurrencyAnnotated(
                                        amount = progress.plannedAmount,
                                        currencyCode = null,
                                        spec = CurrencyDisplaySpec(useSemanticColor = false)
                                    )
                                )
                                append(" planned")
                            },
                            style = AppTextStyles.caption.copy(fontWeight = FontWeight.Medium),
                            color = AppColors.contentSecondary.copy(alpha = 0.6f)
                        )
                    }
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

@Preview(showBackground = true, name = "Budget Hero Card - On Track")
@Composable
private fun BudgetHeroCardOnTrackPreview() {
    val sampleBudget = Budget(
        id = 1L,
        name = "Monthly Groceries",
        limitAmount = BigDecimal("500.00"),
        currency = "USD",
        timelineRange = TimelineRange.ThisPeriod(ThisPeriodType.MONTH),
        categoryIds = setOf(1L),
        accountIds = emptySet(),
        tags = emptySet(),
        profileId = 1
    )
    val sampleProgress = BudgetProgress(
        budget = sampleBudget,
        spent = BigDecimal("320.00"),
        limit = BigDecimal("500.00"),
        remaining = BigDecimal("180.00"),
        percentage = 0.64f,
        alertLevel = BudgetAlertLevel.NONE,
        plannedAmount = BigDecimal("45.00")
    )
    val sampleHistory = listOf(
        BigDecimal("210.00"),
        BigDecimal("380.00"),
        BigDecimal("290.00"),
        // Exceeds the $500 limit, exercising the limit line + red crossing segment.
        BigDecimal("540.00"),
        BigDecimal("350.00"),
        BigDecimal("320.00")
    ).map { spent ->
        BudgetProgress(
            budget = sampleBudget,
            spent = spent,
            limit = BigDecimal("500.00"),
            remaining = (BigDecimal("500.00") - spent).max(BigDecimal.ZERO),
            percentage = (spent.toFloat() / 500f),
            alertLevel = BudgetAlertLevel.NONE
        )
    }
    OssWalletTheme {
        BudgetHeroCard(
            budget = sampleBudget,
            progress = sampleProgress,
            offset = 0,
            onOffsetChange = {},
            history = sampleHistory
        )
    }
}

@Preview(showBackground = true, name = "Budget Hero Card - Exceeded")
@Composable
private fun BudgetHeroCardExceededPreview() {
    val sampleBudget = Budget(
        id = 2L,
        name = "Dining Out",
        limitAmount = BigDecimal("200.00"),
        currency = "USD",
        timelineRange = TimelineRange.ThisPeriod(ThisPeriodType.MONTH),
        categoryIds = setOf(2L),
        accountIds = emptySet(),
        tags = emptySet(),
        profileId = 1
    )
    val sampleProgress = BudgetProgress(
        budget = sampleBudget,
        spent = BigDecimal("245.50"),
        limit = BigDecimal("200.00"),
        remaining = BigDecimal("-45.50"),
        percentage = 1.2275f,
        alertLevel = BudgetAlertLevel.EXCEEDED,
        plannedAmount = BigDecimal("30.00")
    )
    OssWalletTheme {
        BudgetHeroCard(
            budget = sampleBudget,
            progress = sampleProgress,
            offset = 0,
            onOffsetChange = {}
        )
    }
}

@Preview(showBackground = true, name = "Budget Hero Card - Timeline Offset")
@Composable
private fun BudgetHeroCardTimelineOffsetPreview() {
    val sampleBudget = Budget(
        id = 3L,
        name = "Travel & Leisure",
        limitAmount = BigDecimal("800.00"),
        currency = "USD",
        timelineRange = TimelineRange.ThisPeriod(ThisPeriodType.MONTH),
        categoryIds = setOf(3L),
        accountIds = emptySet(),
        tags = emptySet(),
        profileId = 1
    )
    val sampleProgress = BudgetProgress(
        budget = sampleBudget,
        spent = BigDecimal("150.00"),
        limit = BigDecimal("800.00"),
        remaining = BigDecimal("650.00"),
        percentage = 0.1875f,
        alertLevel = BudgetAlertLevel.NONE
    )
    val sampleHistory = listOf(
        BigDecimal("620.00"),
        BigDecimal("450.00"),
        BigDecimal("710.00"),
        BigDecimal("500.00"),
        BigDecimal("300.00"),
        BigDecimal("150.00")
    ).map { spent ->
        BudgetProgress(
            budget = sampleBudget,
            spent = spent,
            limit = BigDecimal("800.00"),
            remaining = BigDecimal("800.00") - spent,
            percentage = (spent.toFloat() / 800f),
            alertLevel = BudgetAlertLevel.NONE
        )
    }
    OssWalletTheme {
        BudgetHeroCard(
            budget = sampleBudget,
            progress = sampleProgress,
            offset = -1,
            onOffsetChange = {},
            history = sampleHistory
        )
    }
}

@Preview(showBackground = true, name = "Budget Hero Card - This Period (over limit)")
@Composable
private fun BudgetHeroCardThisPeriodPreview() {
    val sampleBudget = Budget(
        id = 4L,
        name = "Dining Out",
        limitAmount = BigDecimal("300.00"),
        currency = "USD",
        timelineRange = TimelineRange.ThisPeriod(ThisPeriodType.MONTH),
        categoryIds = emptySet(),
        accountIds = emptySet(),
        tags = emptySet(),
        profileId = 1
    )
    val day = 24L * 60 * 60 * 1000
    val now = System.currentTimeMillis()
    fun expense(id: Long, amount: String, daysAgo: Long) = Transaction(
        id = id,
        name = "Sample",
        amount = BigDecimal(amount),
        timestamp = now - daysAgo * day,
        currency = "USD",
        transactionType = TransactionType.EXPENSE,
        accountId = null,
        categoryId = 1L
    )
    val sampleTransactions = listOf(
        // Current period so far — sums to $220.
        expense(1, "60.00", 1),
        expense(2, "50.00", 3),
        expense(3, "70.00", 5),
        expense(4, "40.00", 8),
        // Previous period, for the dashed comparison line.
        expense(5, "55.00", 34),
        expense(6, "65.00", 38),
        expense(7, "45.00", 42)
    )
    val sampleProgress = BudgetProgress(
        budget = sampleBudget,
        spent = BigDecimal("220.00"),
        limit = BigDecimal("300.00"),
        remaining = BigDecimal("80.00"),
        percentage = 0.7333f,
        alertLevel = BudgetAlertLevel.NONE,
        // $110 planned pushes spent+planned to $330 — over the $300 limit, so the planned tail
        // crosses the limit line and turns red partway through.
        plannedAmount = BigDecimal("110.00")
    )
    OssWalletTheme {
        BudgetHeroCard(
            budget = sampleBudget,
            progress = sampleProgress,
            offset = 0,
            onOffsetChange = {},
            transactions = sampleTransactions,
            exchangeRates = ExchangeRateSnapshot.EMPTY,
            initialTrendViewMode = BudgetTrendViewMode.THIS_PERIOD
        )
    }
}

