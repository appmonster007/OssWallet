package com.osscabinet.ossWallet.ui.screens.dashboard.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.TrendingDown
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.rounded.AccountBalance
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.osscabinet.ossWallet.ui.components.cards.CardSettingsSection
import com.osscabinet.ossWallet.ui.components.cards.CardSettingsSheet
import com.osscabinet.ossWallet.domain.card.NetWorthMetrics
import com.osscabinet.ossWallet.domain.model.TransactionFilter
import com.osscabinet.ossWallet.domain.model.TransactionType
import com.osscabinet.ossWallet.domain.model.analytics.CashFlowResult
import com.osscabinet.ossWallet.domain.model.settings.NetWorthCardConfig
import com.osscabinet.ossWallet.domain.model.timeline.ThisPeriodType
import com.osscabinet.ossWallet.domain.model.timeline.TimelineRange
import com.osscabinet.ossWallet.ui.components.cards.AeonExpressiveCard
import com.osscabinet.ossWallet.ui.components.cards.CardCallbacks
import com.osscabinet.ossWallet.ui.components.cards.FilterSelectionContext
import com.osscabinet.ossWallet.ui.theme.AeonTheme
import com.osscabinet.ossWallet.ui.theme.OssWalletTheme
import com.osscabinet.ossWallet.ui.theme.PositiveAmountColor
import com.osscabinet.ossWallet.domain.service.currency.CurrencyFormatter.formatCurrencyRight
import java.math.BigDecimal
import java.math.RoundingMode
import com.osscabinet.ossWallet.ui.theme.AppColors
import com.osscabinet.ossWallet.ui.theme.AppTextStyles

@Composable
fun NetWorthCard(
    currentMetrics: NetWorthMetrics,
    baseCurrency: String,
    displayCurrency: String = baseCurrency,
    config: NetWorthCardConfig,
    previousMetrics: NetWorthMetrics? = null,
    callbacks: CardCallbacks.NetWorth = CardCallbacks.NetWorth(),
    filterContext: FilterSelectionContext = FilterSelectionContext(),
    baseFilter: TransactionFilter = TransactionFilter(),
    inheritedFilter: TransactionFilter = TransactionFilter(),
    screenOwnsTimeline: Boolean = false,
    showFilterButton: Boolean = true,
    modifier: Modifier = Modifier,
    headerModifier: Modifier = Modifier,
) {
    val displayFilter = if (screenOwnsTimeline || inheritedFilter.timelineRange !is TimelineRange.AllTime)
        config.filter.copy(timelineRange = inheritedFilter.timelineRange)
    else config.filter

    AeonExpressiveCard(
        title = config.title,
        icon = Icons.Rounded.AccountBalance,
        modifier = modifier,
        headerModifier = headerModifier,
        onClick = callbacks.onSeeAll,
        filterEnabled = showFilterButton,
        filter = displayFilter,
        baseFilter = baseFilter,
        filterRules = config.filterRules,
        filterContext = filterContext,
        onFilterChange = { callbacks.onFilterChange(config.cardId, it) },
        onSaveFilterClick = callbacks.onSaveFilterClick,
        displaySettings = { onDismiss ->
            NetWorthSettingsSheet(config = config, callbacks = callbacks, onDismiss = onDismiss)
        }
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(AeonTheme.dimensions.spacingMd)
        ) {
            val baseFontSize = when (config.fontSize) {
                "Small" -> 18.sp
                "Large" -> 24.sp
                "X-Large" -> 32.sp
                else -> 20.sp
            }

            Column {
                Text(
                    text = formatCurrencyRight(currentMetrics.netWorth, displayCurrency, shorten = true),
                    style = AppTextStyles.screenTitle.copy(
                        fontWeight = FontWeight.Bold,
                        fontSize = baseFontSize,
                        letterSpacing = (-0.5).sp
                    ),
                    color = AppColors.contentPrimary
                )

                currentMetrics.netWorthInBase?.let { inBase ->
                    Text(
                        text = "≈ ${formatCurrencyRight(inBase, baseCurrency, shorten = true)}",
                        style = AppTextStyles.metaLabel,
                        color = AppColors.contentPrimary.copy(alpha = 0.6f)
                    )
                }

                if (config.showComparison && previousMetrics != null) {
                    val change = currentMetrics.netWorth.subtract(previousMetrics.netWorth)
                    if (change.signum() != 0 && previousMetrics.netWorth.signum() != 0) {
                        val percentage = change.multiply(BigDecimal("100"))
                            .divide(previousMetrics.netWorth.abs(), 1, RoundingMode.HALF_UP)

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(
                                if (change.signum() > 0) Icons.AutoMirrored.Filled.TrendingUp else Icons.AutoMirrored.Filled.TrendingDown,
                                contentDescription = null,
                                modifier = Modifier.size(12.dp),
                                tint = AppColors.contentSecondary
                            )
                            Text(
                                text = "${if (change.signum() > 0) "+" else ""}$percentage% from prev. period",
                                style = AppTextStyles.transactionSwipeAction.copy(
                                    fontWeight = FontWeight.Medium,
                                    fontSize = 12.sp
                                ),
                                color = AppColors.contentSecondary
                            )
                        }
                    }
                }
            }

            Row(
                horizontalArrangement = Arrangement.spacedBy(AeonTheme.dimensions.spacingSm),
                modifier = Modifier.fillMaxWidth()
            ) {
                FlowBadge(
                    label = "INCOME",
                    amount = currentMetrics.cashFlow.totalIncomeInBase,
                    currency = displayCurrency,
                    color = PositiveAmountColor,
                    modifier = Modifier.weight(1f),
                    onClick = { callbacks.onClickIncome?.invoke(config.filter.copy(transactionTypes = setOf(TransactionType.INCOME))) },
                    previousAmount = if (config.showComparison) previousMetrics?.cashFlow?.totalIncomeInBase else null
                )
                FlowBadge(
                    label = "EXPENSE",
                    amount = currentMetrics.cashFlow.totalExpensesInBase,
                    currency = displayCurrency,
                    color = AppColors.error,
                    modifier = Modifier.weight(1f),
                    onClick = { callbacks.onClickExpense?.invoke(config.filter.copy(transactionTypes = setOf(TransactionType.EXPENSE))) },
                    previousAmount = if (config.showComparison) previousMetrics?.cashFlow?.totalExpensesInBase else null
                )
            }
        }
    }
}

@Composable
private fun FlowBadge(
    label: String,
    amount: BigDecimal,
    currency: String,
    color: Color,
    modifier: Modifier = Modifier,
    onClick: () -> Unit = {},
    previousAmount: BigDecimal? = null
) {
    Surface(
        modifier = modifier.clickable(onClick = onClick),
        color = AppColors.insetSurface,
        shape = RoundedCornerShape(AeonTheme.dimensions.containerCornerRadius),
    ) {
        Column(
            modifier = Modifier.padding(horizontal = AeonTheme.dimensions.cardPadding, vertical = AeonTheme.dimensions.spacingMd),
            verticalArrangement = Arrangement.spacedBy(AeonTheme.dimensions.spacingXs)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(AeonTheme.dimensions.spacingXs)
                ) {
                    Text(
                        text = label,
                        style = AppTextStyles.transactionSwipeAction.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp,
                            letterSpacing = 0.5.sp
                        ),
                        color = AppColors.contentSecondary
                    )
                }
                if (previousAmount != null) {
                    val change = amount.subtract(previousAmount)
                    if (change.signum() != 0 && previousAmount.signum() != 0) {
                        val percentage = change.multiply(BigDecimal("100"))
                            .divide(previousAmount.abs(), 1, RoundingMode.HALF_UP)

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(AeonTheme.dimensions.spacingXs)
                        ) {
                            Icon(
                                if (change.signum() > 0) Icons.AutoMirrored.Filled.TrendingUp else Icons.AutoMirrored.Filled.TrendingDown,
                                contentDescription = null,
                                modifier = Modifier.size(AeonTheme.dimensions.iconSizeXs),
                                tint = color
                            )
                            Text(
                                text = "${if (change.signum() > 0) "+" else ""}$percentage%",
                                style = AppTextStyles.transactionSwipeAction.copy(
                                    fontWeight = FontWeight.Medium,
                                    fontSize = 12.sp
                                ),
                                color = color
                            )
                        }
                    }
                }
            }
            Text(
                text = formatCurrencyRight(amount, currency, shorten = true),
                style = AppTextStyles.managementItemTitle.copy(
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp
                ),
                color = color
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun NetWorthSettingsSheet(
    config: NetWorthCardConfig,
    callbacks: CardCallbacks.NetWorth,
    onDismiss: () -> Unit
) {
    CardSettingsSheet(title = "Net Worth", onDismiss = onDismiss, onDelete = callbacks.onDelete) {
        CardSettingsSection("Font Size") {
            SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                listOf("Small", "Medium", "Large", "X-Large").forEachIndexed { index, size ->
                    SegmentedButton(
                        selected = config.fontSize == size,
                        onClick = { callbacks.onFontSizeChange(config.cardId, size) },
                        shape = SegmentedButtonDefaults.itemShape(index = index, count = 4),
                        label = {
                            Text(when (size) {
                                "Small" -> "S"
                                "Medium" -> "M"
                                "Large" -> "L"
                                else -> "XL"
                            })
                        }
                    )
                }
            }
        }
        CardSettingsSection("Display") {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("Show Comparison", style = AppTextStyles.transactionTitle)
                    Text(
                        "Show change from previous period",
                        style = AppTextStyles.caption,
                        color = AppColors.contentSecondary
                    )
                }
                Switch(
                    checked = config.showComparison,
                    onCheckedChange = { callbacks.onShowComparisonChange(config.cardId, it) }
                )
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun NetWorthCardPreview() {
    val currentMetrics = NetWorthMetrics(
        netWorth = BigDecimal("15250.75"),
        cashFlow = CashFlowResult(
            totalIncomeInBase = BigDecimal("4500.00"),
            totalExpensesInBase = BigDecimal("2350.50"),
            totalTransfersInBase = BigDecimal.ZERO,
            netInBase = BigDecimal("2149.50"),
            breakdown = emptyList(),
            filteredTransactions = emptyList()
        )
    )
    val previousMetrics = NetWorthMetrics(
        netWorth = BigDecimal("14100.25"),
        cashFlow = CashFlowResult(
            totalIncomeInBase = BigDecimal("4200.00"),
            totalExpensesInBase = BigDecimal("2100.00"),
            totalTransfersInBase = BigDecimal.ZERO,
            netInBase = BigDecimal("2100.00"),
            breakdown = emptyList(),
            filteredTransactions = emptyList()
        )
    )
    OssWalletTheme {
        Surface(modifier = Modifier.padding(16.dp)) {
            NetWorthCard(
                currentMetrics = currentMetrics,
                previousMetrics = previousMetrics,
                baseCurrency = "USD",
                config = NetWorthCardConfig(cardId = "net_worth", showComparison = true)
            )
        }
    }
}
