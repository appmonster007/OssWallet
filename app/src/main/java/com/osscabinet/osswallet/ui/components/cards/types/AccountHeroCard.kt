package com.osscabinet.ossWallet.ui.components.cards.types

import com.osscabinet.ossWallet.ui.components.visualization.AeonSparkline
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.TrendingDown
import androidx.compose.material.icons.automirrored.rounded.TrendingUp
import androidx.compose.material.icons.rounded.AccountBalance
import androidx.compose.material.icons.rounded.SwapHoriz
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.graphics.toColorInt
import com.osscabinet.ossWallet.domain.card.CardInput
import com.osscabinet.ossWallet.domain.card.CardRenderData
import com.osscabinet.ossWallet.domain.model.CreditCardDetails
import com.osscabinet.ossWallet.domain.card.CardUpdateHandler
import com.osscabinet.ossWallet.domain.model.Account
import com.osscabinet.ossWallet.domain.model.AccountStructureType
import com.osscabinet.ossWallet.domain.model.TransactionFilter
import com.osscabinet.ossWallet.domain.model.TransactionType
import com.osscabinet.ossWallet.domain.service.financial.FinancialSummary
import com.osscabinet.ossWallet.domain.model.analytics.AccountSummaryItem
import com.osscabinet.ossWallet.domain.model.settings.AccountHeroCardConfig
import com.osscabinet.ossWallet.domain.model.settings.AccountingPerspective
import com.osscabinet.ossWallet.ui.components.AeonSettingsToggle
import com.osscabinet.ossWallet.ui.components.AppIconMap
import com.osscabinet.ossWallet.ui.components.aeonDashedTopRule
import com.osscabinet.ossWallet.ui.components.cards.AeonExpressiveCard
import com.osscabinet.ossWallet.ui.components.cards.AeonInteractionCallbacks
import com.osscabinet.ossWallet.ui.components.cards.CardCallbacks
import com.osscabinet.ossWallet.ui.components.cards.CardLayoutProps
import com.osscabinet.ossWallet.ui.components.cards.CardPerspectiveSettings
import com.osscabinet.ossWallet.ui.components.cards.CardSettingsSection
import com.osscabinet.ossWallet.ui.components.cards.CardWithCallbacks
import com.osscabinet.ossWallet.ui.components.cards.resolveDrillDown
import com.osscabinet.ossWallet.ui.components.cards.resolveSeeAll
import com.osscabinet.ossWallet.ui.theme.AppColors
import com.osscabinet.ossWallet.ui.theme.AppTextStyles
import com.osscabinet.ossWallet.ui.theme.OssWalletTheme
import com.osscabinet.ossWallet.ui.theme.color
import com.osscabinet.ossWallet.ui.util.CurrencyDisplaySpec
import com.osscabinet.ossWallet.ui.util.CurrencyPartStyles
import com.osscabinet.ossWallet.ui.util.CurrencyReprUi
import com.osscabinet.ossWallet.ui.util.formatCurrency
import java.math.BigDecimal
import java.math.RoundingMode

/**
 * Account hero card — vertical "ledger flow" layout.
 *
 * Structure (all inside the neutral [AeonExpressiveCard] shell):
 *  1. Balance hero (semantic color) + approx base
 *  2. Balance-history sparkline — gated: only rendered when [balanceHistory] has ≥2 points
 *  3. Vertical ledger of flows (income / expense / transfer, or inflow / outflow)
 *  4. Currency distribution — 3-column grid with ghost fillers, hidden when single-currency
 *
 * The account color is used only as an *accent* (card border, currency codes, watermark),
 * keeping the card consistent with [NetWorthCard] and the rest of the dashboard.
 */
@Composable
fun AccountHeroCard(
    summary: AccountSummaryItem,
    baseCurrency: String,
    config: AccountHeroCardConfig,
    callbacks: CardCallbacks.AccountHero = CardCallbacks.AccountHero(),
    layoutProps: CardLayoutProps = CardLayoutProps(),
    balanceHistory: List<BigDecimal> = emptyList(),
) {
    val account = summary.account
    val accentColor = try {
        Color(account.colorHex.toColorInt())
    } catch (_: Exception) {
        AppColors.primary
    }
    val icon = AppIconMap[account.icon] ?: Icons.Rounded.AccountBalance

    val isAccounting = config.perspective is AccountingPerspective.Accounting
    val displayFilter = layoutProps.computeDisplayFilter(config.filter)
    val currentOnUpdate = rememberUpdatedState(callbacks.onFilterChange)

    AeonExpressiveCard(
        title = account.name,
        icon = icon,
        accentColor = accentColor,
        backgroundIcon = icon,
        modifier = layoutProps.modifier,
        headerModifier = layoutProps.headerModifier,
        filterEnabled = layoutProps.showFilterButton,
        filter = displayFilter,
        baseFilter = layoutProps.baseFilter,
        filterRules = layoutProps.filterRules ?: config.filterRules,
        filterContext = layoutProps.filterContext,
        onFilterChange = { currentOnUpdate.value.invoke(config.cardId, it) },
        onSaveFilterClick = callbacks.onSaveFilterClick,
        onDelete = callbacks.onDelete,
        settingsContent = {
            AccountHeroSettingsContent(config = config, callbacks = callbacks)
        }
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // ── 1. Balance hero ─────────────────────────────────────────────
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (isAccounting) "TOTAL BALANCE" else "NET CASH FLOW",
                        style = AppTextStyles.overlineLabel.copy(
                            letterSpacing = 3.sp,
                            fontSize = 9.sp
                        ),
                        color = AppColors.contentSecondary.copy(alpha = 0.7f)
                    )

                    Surface(
                        color = account.structureType.color.copy(alpha = 0.12f),
                        shape = RoundedCornerShape(4.dp)
                    ) {
                        Text(
                            text = account.structureType.displayName.uppercase(),
                            style = AppTextStyles.tagLabel.copy(
                                fontSize = 8.sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 0.5.sp
                            ),
                            color = account.structureType.color,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }

                var multiplier by remember(summary.balance, account.currencyCode) { mutableFloatStateOf(1f) }
                val symbolStyle = SpanStyle(fontWeight = FontWeight.Light, fontSize = (21f * multiplier).sp)
                Text(
                    text = CurrencyReprUi.formatCurrencyAnnotated(
                        amount = summary.balance,
                        currencyCode = account.currencyCode,
                        spec = CurrencyDisplaySpec(useSemanticColor = true, shortenThreshold = -1),
                        partStyles = CurrencyPartStyles(symbol = symbolStyle)
                    ),
                    style = AppTextStyles.heroAmountLarge.copy(
                        fontSize = (30f * multiplier).sp,
                        letterSpacing = (-1).sp
                    ),
                    color = AppColors.contentPrimary,
                    maxLines = 1,
                    softWrap = false,
                    overflow = TextOverflow.Clip,
                    onTextLayout = { textLayoutResult ->
                        if (textLayoutResult.didOverflowWidth && multiplier > 0.4f) {
                            multiplier *= 0.85f
                        }
                    },
                    modifier = remember(config.perspective, config.filter, callbacks) {
                        Modifier.clickable {
                            if (isAccounting) callbacks.onClickNet?.invoke(config.filter)
                            else callbacks.onClickNetFlow?.invoke(config.filter)
                        }
                    }
                )

                if (account.currencyCode != baseCurrency) {
                    Text(
                        text = CurrencyReprUi.formatCurrencyAnnotated(
                            amount = summary.balanceInBase,
                            currencyCode = baseCurrency,
                            spec = CurrencyDisplaySpec(
                                isApproximate = true,
                                useSemanticColor = false
                            )
                        ),
                        style = AppTextStyles.transactionTitle.copy(
                            fontWeight = FontWeight.Medium,
                            color = AppColors.contentSecondary.copy(alpha = 0.7f)
                        )
                    )
                }
            }

            // ── 2. Sparkline (gated on data availability) ───────────────────
            if (balanceHistory.size >= 2) {
                val isNeg = summary.balance.signum() < 0
                AeonSparkline(
                    points = balanceHistory,
                    lineColor = if (isNeg) AppColors.negative else AppColors.positive,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp)
                )
            }

            // ── 2.1 Credit Details & Utilization Gauge ────────────────────
            val creditDetails = account.creditDetails
            if (account.structureType == AccountStructureType.CREDIT && creditDetails != null) {
                val limit = creditDetails.creditLimit
                val owed = if (summary.balance < BigDecimal.ZERO) summary.balance.abs() else BigDecimal.ZERO
                val available = if (summary.balance > BigDecimal.ZERO) limit + summary.balance else (limit - owed).coerceAtLeast(BigDecimal.ZERO)
                val ratio = if (limit.signum() > 0) (owed.toDouble() / limit.toDouble()).coerceIn(0.0, 1.0) else 0.0
                val percentage = (ratio * 100).toInt()

                val utilColor = when {
                    ratio < 0.30 -> AppColors.positive
                    ratio < 0.50 -> AppColors.accent
                    else -> AppColors.negative
                }

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .aeonDashedTopRule()
                        .padding(top = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // Header row with title & status pill
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "CREDIT UTILIZATION",
                            style = AppTextStyles.overlineLabel.copy(
                                letterSpacing = 2.sp,
                                fontSize = 9.sp
                            ),
                            color = AppColors.contentSecondary.copy(alpha = 0.7f)
                        )

                        Surface(
                            color = utilColor.copy(alpha = 0.12f),
                            shape = RoundedCornerShape(4.dp)
                        ) {
                            Text(
                                text = "$percentage% UTILIZED",
                                style = AppTextStyles.tagLabel.copy(
                                    fontSize = 8.sp,
                                    fontWeight = FontWeight.Bold,
                                    letterSpacing = 0.5.sp
                                ),
                                color = utilColor,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }

                    // Progress bar track
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(6.dp)
                            .clip(RoundedCornerShape(3.dp))
                            .background(AppColors.contentPrimary.copy(alpha = 0.08f))
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxHeight()
                                .fillMaxWidth(ratio.toFloat())
                                .clip(RoundedCornerShape(3.dp))
                                .background(utilColor)
                        )
                    }

                    // Dual readout columns (Available Credit vs Credit Limit)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "AVAILABLE CREDIT",
                                style = AppTextStyles.overlineLabel.copy(
                                    fontSize = 8.sp,
                                    letterSpacing = 1.sp
                                ),
                                color = AppColors.contentSecondary.copy(alpha = 0.6f)
                            )
                            Text(
                                text = formatCurrency(available, account.currencyCode),
                                style = AppTextStyles.listItemTitle.copy(
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 15.sp
                                ),
                                color = AppColors.contentPrimary
                            )
                        }

                        Column(horizontalAlignment = Alignment.End) {
                            Text(
                                text = "CREDIT LIMIT",
                                style = AppTextStyles.overlineLabel.copy(
                                    fontSize = 8.sp,
                                    letterSpacing = 1.sp
                                ),
                                color = AppColors.contentSecondary.copy(alpha = 0.6f)
                            )
                            Text(
                                text = formatCurrency(limit, account.currencyCode),
                                style = AppTextStyles.listItemTitle.copy(
                                    fontWeight = FontWeight.Medium,
                                    fontSize = 14.sp
                                ),
                                color = AppColors.contentSecondary
                            )
                        }
                    }

                    // Metadata chips row (Due Day, Closing Day, APR)
                    if (creditDetails.paymentDueDay != null || creditDetails.statementClosingDay != null || creditDetails.interestRateApr != null) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            if (creditDetails.paymentDueDay != null) {
                                Surface(
                                    color = AppColors.contentPrimary.copy(alpha = 0.05f),
                                    shape = RoundedCornerShape(6.dp)
                                ) {
                                    Text(
                                        text = "DUE: ${creditDetails.paymentDueDay}TH",
                                        style = AppTextStyles.metaLabel.copy(
                                            fontSize = 9.sp,
                                            fontWeight = FontWeight.Bold
                                        ),
                                        color = AppColors.contentSecondary.copy(alpha = 0.8f),
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }
                            }

                            if (creditDetails.statementClosingDay != null) {
                                Surface(
                                    color = AppColors.contentPrimary.copy(alpha = 0.05f),
                                    shape = RoundedCornerShape(6.dp)
                                ) {
                                    Text(
                                        text = "CLOSING: ${creditDetails.statementClosingDay}TH",
                                        style = AppTextStyles.metaLabel.copy(
                                            fontSize = 9.sp,
                                            fontWeight = FontWeight.Bold
                                        ),
                                        color = AppColors.contentSecondary.copy(alpha = 0.8f),
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }
                            }

                            if (creditDetails.interestRateApr != null) {
                                Surface(
                                    color = AppColors.contentPrimary.copy(alpha = 0.05f),
                                    shape = RoundedCornerShape(6.dp)
                                ) {
                                    Text(
                                        text = "APR: ${creditDetails.interestRateApr}%",
                                        style = AppTextStyles.metaLabel.copy(
                                            fontSize = 9.sp,
                                            fontWeight = FontWeight.Bold
                                        ),
                                        color = AppColors.accent,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // ── 3. Vertical ledger of flows ─────────────────────────────────
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .aeonDashedTopRule()
                    .padding(top = 12.dp)
            ) {
            val flow = summary.currentPeriodMetrics
                val prevFlow = summary.previousPeriodMetrics.takeIf { config.showComparison }

                if (isAccounting) {
                    LedgerRow(
                        label = "Income",
                        amount = flow.income,
                        currency = account.currencyCode,
                        badgeIcon = Icons.AutoMirrored.Rounded.TrendingUp,
                        color = AppColors.positive,
                        previousAmount = prevFlow?.income,
                        forceShowPlus = true,
                        onClick = {
                            callbacks.onClickIncome?.invoke(
                                config.filter.copy(transactionTypes = setOf(TransactionType.INCOME))
                            )
                        }
                    )
                    LedgerDivider()
                    LedgerRow(
                        label = "Expense",
                        amount = flow.expense,
                        currency = account.currencyCode,
                        badgeIcon = Icons.AutoMirrored.Rounded.TrendingDown,
                        color = AppColors.negative,
                        previousAmount = prevFlow?.expense,
                        onClick = {
                            callbacks.onClickExpense?.invoke(
                                config.filter.copy(transactionTypes = setOf(TransactionType.EXPENSE))
                            )
                        }
                    )
                    LedgerDivider()
                    if (config.splitTransfers) {
                        LedgerRow(
                            label = "Transfer In",
                            amount = flow.transferIn,
                            currency = account.currencyCode,
                            badgeIcon = Icons.Rounded.SwapHoriz,
                            color = AppColors.contentPrimary,
                            previousAmount = prevFlow?.transferIn,
                            forceShowPlus = true,
                            onClick = {
                                callbacks.onClickTransfer?.invoke(
                                    config.filter.copy(transactionTypes = setOf(TransactionType.TRANSFER_INFLOW))
                                )
                            }
                        )
                        LedgerDivider()
                        LedgerRow(
                            label = "Transfer Out",
                            amount = flow.transferOut,
                            currency = account.currencyCode,
                            badgeIcon = Icons.Rounded.SwapHoriz,
                            color = AppColors.contentPrimary,
                            previousAmount = prevFlow?.transferOut,
                            onClick = {
                                callbacks.onClickTransfer?.invoke(
                                    config.filter.copy(transactionTypes = setOf(TransactionType.TRANSFER_OUTFLOW))
                                )
                            }
                        )
                    } else {
                        LedgerRow(
                            label = "Transfer",
                            amount = flow.transfer,
                            currency = account.currencyCode,
                            badgeIcon = Icons.Rounded.SwapHoriz,
                            color = AppColors.contentPrimary,
                            previousAmount = prevFlow?.transfer,
                            forceShowPlus = true,
                            onClick = {
                                callbacks.onClickTransfer?.invoke(
                                    config.filter.copy(transactionTypes = TransactionType.TRANSFER_TYPES)
                                )
                            }
                        )
                    }
                } else {
                    LedgerRow(
                        label = "Inflow",
                        amount = flow.inflow,
                        currency = account.currencyCode,
                        badgeIcon = Icons.AutoMirrored.Rounded.TrendingUp,
                        color = AppColors.positive,
                        previousAmount = prevFlow?.inflow,
                        forceShowPlus = true,
                        onClick = {
                            callbacks.onClickInflow?.invoke(
                                config.filter.copy(
                                    transactionTypes = setOf(
                                        TransactionType.INCOME,
                                        TransactionType.TRANSFER_INFLOW
                                    )
                                )
                            )
                        }
                    )
                    LedgerDivider()
                    LedgerRow(
                        label = "Outflow",
                        amount = flow.outflow,
                        currency = account.currencyCode,
                        badgeIcon = Icons.AutoMirrored.Rounded.TrendingDown,
                        color = AppColors.negative,
                        previousAmount = prevFlow?.outflow,
                        onClick = {
                            callbacks.onClickOutflow?.invoke(
                                config.filter.copy(
                                    transactionTypes = setOf(
                                        TransactionType.EXPENSE,
                                        TransactionType.TRANSFER_OUTFLOW
                                    )
                                )
                            )
                        }
                    )
                }
            }

            // ── 4. Currency distribution grid ───────────────────────────────
            val currencyBalances = summary.currencyBalances
            if (config.showCurrencyBadges && currencyBalances.size > 1) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .aeonDashedTopRule()
                        .padding(top = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        text = "DISTRIBUTION",
                        style = AppTextStyles.overlineLabel.copy(
                            letterSpacing = 2.sp,
                            fontSize = 9.sp
                        ),
                        color = AppColors.contentSecondary.copy(alpha = 0.7f)
                    )
                    CurrencyGhostGrid(
                        balances = currencyBalances,
                        accentColor = accentColor,
                        onCurrencyClick = { callbacks.onClickCurrency?.invoke(it) }
                    )
                }
            }
        }
    }
}

/** A single ledger line item: icon badge + label (+ optional trend) on the left, amount on the right. */
@Composable
private fun LedgerRow(
    label: String,
    amount: BigDecimal,
    currency: String,
    badgeIcon: ImageVector,
    color: Color,
    modifier: Modifier = Modifier,
    previousAmount: BigDecimal? = null,
    forceShowPlus: Boolean = false,
    onClick: () -> Unit = {}
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(26.dp)
                    .background(color.copy(alpha = 0.14f), RoundedCornerShape(8.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = badgeIcon,
                    contentDescription = null,
                    tint = color,
                    modifier = Modifier.size(15.dp)
                )
            }
            Column {
                Text(
                    text = label,
                    style = AppTextStyles.transactionTitle.copy(fontWeight = FontWeight.SemiBold),
                    color = AppColors.contentSecondary
                )
                val sub = trendSublabel(amount, previousAmount)
                if (sub != null) {
                    Text(
                        text = sub,
                        style = AppTextStyles.metaLabel.copy(fontSize = 9.sp),
                        color = AppColors.contentSecondary.copy(alpha = 0.5f)
                    )
                }
            }
        }
        Text(
            text = formatCurrency(
                amount = amount,
                currencyCode = currency,
                spec = CurrencyDisplaySpec(
                    useSemanticColor = false,
                    forceShowPlus = forceShowPlus,
                    shortenThreshold = 12
                )
            ),
            style = AppTextStyles.screenSubTitle.copy(
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp
            ),
            color = color
        )
    }
}

@Composable
private fun LedgerDivider() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(1.dp)
            .background(AppColors.outlineVariant.copy(alpha = 0.5f))
    )
}

/** "▲ +12.4% vs last period" style trend, or null when there's nothing meaningful to show. */
private fun trendSublabel(amount: BigDecimal, previousAmount: BigDecimal?): String? {
    if (previousAmount == null) return null
    val change = amount.subtract(previousAmount)
    if (change.signum() == 0 || previousAmount.signum() == 0) return null
    val percentage = change.multiply(BigDecimal("100"))
        .divide(previousAmount.abs(), 1, RoundingMode.HALF_UP)
    val up = change.signum() > 0
    val pct = if (percentage.abs() > BigDecimal("999")) {
        if (percentage.signum() > 0) ">999%" else "<-999%"
    } else {
        "${if (up) "+" else ""}$percentage%"
    }
    return "${if (up) "▲" else "▼"} $pct"
}

/**
 * Currency chips in a fixed 3-column grid. Ghost cells fill the last row so chips keep a
 * uniform width and the grid never looks ragged.
 */
@Composable
private fun CurrencyGhostGrid(
    balances: Map<String, BigDecimal>,
    accentColor: Color,
    onCurrencyClick: (String) -> Unit,
) {
    val columns = 3
    val entries = remember(balances) {
        balances.entries.sortedByDescending { it.value.abs() }.map { it.key to it.value }
    }
    val ghostCount = (columns - entries.size % columns) % columns

    Column(verticalArrangement = Arrangement.spacedBy(7.dp)) {
        entries.chunked(columns).forEachIndexed { rowIndex, rowItems ->
            val isLastRow = rowIndex == (entries.size - 1) / columns
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(IntrinsicSize.Min),
                horizontalArrangement = Arrangement.spacedBy(7.dp)
            ) {
                rowItems.forEach { (curr, bal) ->
                    CurrencyChip(
                        currency = curr,
                        amount = bal,
                        accentColor = accentColor,
                        onClick = { onCurrencyClick(curr) },
                        modifier = Modifier.weight(1f)
                    )
                }
                if (isLastRow) {
                    repeat(ghostCount) {
                        GhostChip(modifier = Modifier.weight(1f))
                    }
                }
            }
        }
    }
}

@Composable
private fun CurrencyChip(
    currency: String,
    amount: BigDecimal,
    accentColor: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier.clickable { onClick() },
        color = AppColors.contentPrimary.copy(alpha = 0.05f),
        shape = RoundedCornerShape(18.dp),
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 9.dp, vertical = 7.dp),
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            Text(
                text = currency,
                style = AppTextStyles.overlineLabel.copy(
                    fontSize = 8.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp
                ),
                color = accentColor
            )
            Text(
                text = formatCurrency(
                    amount = amount,
                    currencyCode = currency,
                    spec = CurrencyDisplaySpec(useSemanticColor = false, shortenThreshold = 9)
                ),
                style = AppTextStyles.transactionTitle.copy(
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp
                ),
                color = AppColors.contentPrimary,
                maxLines = 1
            )
        }
    }
}

@Composable
private fun GhostChip(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .fillMaxHeight()
            .background(AppColors.contentPrimary.copy(alpha = 0.015f), RoundedCornerShape(18.dp))
    )
}

@Composable
private fun AccountHeroSettingsContent(
    config: AccountHeroCardConfig,
    callbacks: CardCallbacks.AccountHero,
) {
    val currentOnShowCurrencyBadgesChange = rememberUpdatedState(callbacks.onShowCurrencyBadgesChange)
    val currentOnShowComparisonChange = rememberUpdatedState(callbacks.onShowComparisonChange)
    val currentOnPerspectiveChange = rememberUpdatedState(callbacks.onPerspectiveChange)
    val currentOnExcludeInternalTransfersChange = rememberUpdatedState(callbacks.onExcludeInternalTransfersChange)
    val currentOnSplitTransfersChange = rememberUpdatedState(callbacks.onSplitTransfersChange)

    CardSettingsSection("Display") {
        AeonSettingsToggle(
            label = "Show Currency Distribution",
            description = "Show breakdown of multi-currency balances",
            checked = config.showCurrencyBadges,
            onCheckedChange = { currentOnShowCurrencyBadgesChange.value(config.cardId, it) },
            showBackground = false
        )
        AeonSettingsToggle(
            label = "Show Comparison",
            description = "Show change from previous period",
            checked = config.showComparison,
            onCheckedChange = { currentOnShowComparisonChange.value(config.cardId, it) },
            showBackground = false
        )
    }
    CardPerspectiveSettings(
        perspective = config.perspective,
        onPerspectiveChange = { currentOnPerspectiveChange.value(config.cardId, it) },
        excludeInternalTransfers = config.excludeInternalTransfers,
        onExcludeInternalTransfersChange = {
            currentOnExcludeInternalTransfersChange.value(config.cardId, it)
        },
        splitTransfers = config.splitTransfers,
        onSplitTransfersChange = { currentOnSplitTransfersChange.value(config.cardId, it) }
    )
}

/**
 * Maps [AccountHeroCardConfig] + [CardRenderData.AccountHero] to [CardWithCallbacks.AccountHero].
 */
fun mapAccountHero(
    config: AccountHeroCardConfig,
    renderData: CardRenderData.AccountHero,
    input: CardInput,
    updateHandler: CardUpdateHandler,
    onDelete: ((String) -> Unit)?,
    onRecordsClick: (TransactionFilter) -> Unit,
    callbacks: AeonInteractionCallbacks,
    filterSheetEnabled: Boolean
) = CardWithCallbacks.AccountHero(
    renderData = renderData,
    callbacks = CardCallbacks.AccountHero(
        onShowCurrencyBadgesChange = updateHandler::updateCardHeroShowCurrencyBadges,
        onShowComparisonChange = updateHandler::updateCardShowComparison,
        onPerspectiveChange = updateHandler::updateCardPerspective,
        onExcludeInternalTransfersChange = updateHandler::updateCardExcludeInternalTransfers,
        onSplitTransfersChange = updateHandler::updateCardSplitTransfers,
        onFilterChange = updateHandler::updateCardHeroFilter,
        onClickIncome = { drillDown -> onRecordsClick(resolveDrillDown(input, config, drillDown)) },
        onClickExpense = { drillDown ->
            onRecordsClick(
                resolveDrillDown(
                    input,
                    config,
                    drillDown
                )
            )
        },
        onClickTransfer = { drillDown ->
            onRecordsClick(
                resolveDrillDown(
                    input,
                    config,
                    drillDown
                )
            )
        },
        onClickInflow = { drillDown -> onRecordsClick(resolveDrillDown(input, config, drillDown)) },
        onClickOutflow = { drillDown ->
            onRecordsClick(
                resolveDrillDown(
                    input,
                    config,
                    drillDown
                )
            )
        },
        onClickNet = { drillDown -> onRecordsClick(resolveDrillDown(input, config, drillDown)) },
        onClickNetFlow = { drillDown ->
            onRecordsClick(
                resolveDrillDown(
                    input,
                    config,
                    drillDown
                )
            )
        },
        onClickCurrency = { currency ->
            val drillDown = TransactionFilter(currencies = setOf(currency))
            onRecordsClick(resolveDrillDown(input, config, drillDown))
        },
        onSeeAll = { onRecordsClick(resolveSeeAll(input, config)) },
        onClick = { callbacks.accountActions.onClick(renderData.summary.account.id) },
        onDelete = onDelete?.let { { it(config.cardId) } },
        onSaveFilterClick = callbacks.filterActions.onSaveClick
    ),
    filterRules = config.filterRules,
    baseFilter = input.filter,
    filterSheetEnabled = filterSheetEnabled
)

@androidx.compose.ui.tooling.preview.Preview(showBackground = true, name = "Account Hero - Light")
@Composable
fun AccountHeroCardLightPreview() {
    AccountHeroCardPreviewContent("Light")
}

@androidx.compose.ui.tooling.preview.Preview(showBackground = true, name = "Account Hero - Dark")
@Composable
fun AccountHeroCardDarkPreview() {
    AccountHeroCardPreviewContent("Dark")
}

@Preview(showBackground = true, name = "Account Hero - Amoled")
@Composable
fun AccountHeroCardAmoledPreview() {
    AccountHeroCardPreviewContent("Amoled")
}

@Preview(showBackground = true, name = "Account Hero - Credit Card")
@Composable
fun AccountHeroCardCreditPreview() {
    OssWalletTheme {
        val creditAccount = Account(
            id = "credit-1",
            name = "Chase Sapphire Reserve",
            currencyCode = "USD",
            structureType = AccountStructureType.CREDIT,
            colorHex = "#F44336",
            icon = "CreditCard",
            creditDetails = CreditCardDetails(
                accountId = "credit-1",
                creditLimit = BigDecimal("10000.00"),
                statementClosingDay = 15,
                paymentDueDay = 25,
                interestRateApr = 21.99f
            )
        )
        val summary = AccountSummaryItem(
            account = creditAccount,
            balance = BigDecimal("2450.00"),
            balanceInBase = BigDecimal("2450.00"),
            currentPeriodMetrics = FinancialSummary(
                income = BigDecimal("0.00"),
                expense = BigDecimal("1250.00")
            )
        )
        AccountHeroCard(
            summary = summary,
            baseCurrency = "USD",
            config = AccountHeroCardConfig(cardId = "hero-credit"),
            balanceHistory = listOf(
                BigDecimal("1200.00"),
                BigDecimal("1800.00"),
                BigDecimal("2450.00")
            )
        )
    }
}

@Composable
private fun AccountHeroCardPreviewContent(theme: String) {
    val account = Account(
        "1",
        "Wealth Fund",
        "USD",
        AccountStructureType.INVESTMENT,
        "#FF4D40",
        "AccountBalance",
        emptyList()
    )
    val summary = AccountSummaryItem(
        account = account,
        currencyBalances = mapOf(
            "USD" to BigDecimal("1800000.00"),
            "EUR" to BigDecimal("45000.00"),
            "GBP" to BigDecimal("12000.00"),
            "JPY" to BigDecimal("380000.00")
        ),
        balance = BigDecimal("-2458000000000.50"),
        balanceInBase = BigDecimal("245800.50"),
        currentPeriodMetrics = FinancialSummary(
            income = BigDecimal("15000.00"),
            expense = BigDecimal("4200.00"),
            transferIn = BigDecimal.ZERO,
            transferOut = BigDecimal.ZERO
        ),
        previousPeriodMetrics = FinancialSummary(
            income = BigDecimal("13350.00"),
            expense = BigDecimal("4340.00"),
            transferIn = BigDecimal.ZERO,
            transferOut = BigDecimal.ZERO
        )
    )
    OssWalletTheme(theme = theme) {
        Box(
            modifier = Modifier
                .background(AppColors.background)
                .padding(24.dp)
        ) {
            AccountHeroCard(
                summary = summary,
                baseCurrency = "USD",
                config = AccountHeroCardConfig(
                    cardId = "preview",
                    showCurrencyBadges = true,
                    showComparison = true
                ),
                balanceHistory = listOf(
                    BigDecimal("-180000"), BigDecimal("-195000"), BigDecimal("-188000"),
                    BigDecimal("-210000"), BigDecimal("-225000"), BigDecimal("-245800")
                )
            )
        }
    }
}
