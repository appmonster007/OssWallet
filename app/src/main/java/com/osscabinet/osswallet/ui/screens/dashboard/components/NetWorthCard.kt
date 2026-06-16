package com.osscabinet.ossWallet.ui.screens.dashboard.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AccountBalance
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.osscabinet.ossWallet.domain.card.CardFilterRules
import com.osscabinet.ossWallet.domain.card.NetWorthMetrics
import com.osscabinet.ossWallet.domain.model.TransactionFilter
import com.osscabinet.ossWallet.domain.model.TransactionType
import com.osscabinet.ossWallet.domain.model.isTransfer
import com.osscabinet.ossWallet.domain.model.analytics.CashFlowResult
import com.osscabinet.ossWallet.domain.model.settings.CashFlowDisplayMode
import com.osscabinet.ossWallet.domain.model.settings.NetWorthCardConfig
import com.osscabinet.ossWallet.domain.model.timeline.TimelineRange
import com.osscabinet.ossWallet.domain.service.currency.CurrencyService
import com.osscabinet.ossWallet.ui.util.CurrencyDisplaySpec
import com.osscabinet.ossWallet.ui.util.CurrencyPartStyles
import com.osscabinet.ossWallet.ui.util.CurrencyReprUi
import com.osscabinet.ossWallet.ui.components.cards.AeonExpressiveCard
import com.osscabinet.ossWallet.ui.components.cards.CardCallbacks
import com.osscabinet.ossWallet.ui.components.entry.EntrySwitchRow
import com.osscabinet.ossWallet.ui.components.cards.CardSettingsSection
import com.osscabinet.ossWallet.ui.components.cards.CardSettingsSheet
import com.osscabinet.ossWallet.ui.components.cards.FilterSelectionContext
import com.osscabinet.ossWallet.ui.components.AeonHazeProvider
import com.osscabinet.ossWallet.ui.components.aeonDashedTopRule
import com.osscabinet.ossWallet.ui.theme.AeonTheme
import com.osscabinet.ossWallet.ui.theme.AppColors
import com.osscabinet.ossWallet.ui.theme.AppTextStyles
import com.osscabinet.ossWallet.ui.theme.OssWalletTheme
import com.osscabinet.ossWallet.ui.theme.ThemePreviews
import java.math.BigDecimal
import java.math.RoundingMode

@Composable
fun NetWorthCard(
    currentPeriodMetrics: NetWorthMetrics,
    baseCurrency: String,
    displayCurrency: String = baseCurrency,
    config: NetWorthCardConfig,
    previousPeriodMetrics: NetWorthMetrics? = null,
    callbacks: CardCallbacks.NetWorth = CardCallbacks.NetWorth(),
    filterContext: FilterSelectionContext = FilterSelectionContext(),
    baseFilter: TransactionFilter = TransactionFilter(),
    inheritedFilter: TransactionFilter = TransactionFilter(),
    filterRules: CardFilterRules? = null,
    screenOwnsTimeline: Boolean = false,
    showFilterButton: Boolean = true,
    modifier: Modifier = Modifier,
    headerModifier: Modifier = Modifier,
) {
    val displayFilter =
        if (screenOwnsTimeline || inheritedFilter.timelineRange !is TimelineRange.AllTime)
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
        filterRules = filterRules ?: config.filterRules,
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
                "Small" -> 24.sp
                "Large" -> 32.sp
                "X-Large" -> 42.sp
                else -> 28.sp
            }

            Column(verticalArrangement = Arrangement.spacedBy(AeonTheme.dimensions.spacingXs)) {
                val contentColor = AppColors.contentPrimary

                val symbolStyle = SpanStyle(
                    fontWeight = FontWeight.Light,
                    color = contentColor.copy(alpha = 0.6f),
                    fontSize = (baseFontSize.value * 0.7f).sp
                )

                Text(
                    text = CurrencyReprUi.formatCurrencyAnnotated(
                        amount = currentPeriodMetrics.netWorth,
                        currencyCode = displayCurrency,
                        spec = CurrencyDisplaySpec(useSemanticColor = true),
                        partStyles = CurrencyPartStyles(symbol = symbolStyle)
                    ),
                    style = AppTextStyles.heroAmountLarge.copy(
                        fontSize = baseFontSize,
                        letterSpacing = (-1).sp
                    ),
                    color = contentColor
                )

                // Mono trend readout — "▲ +8.2%  VS PRIOR PERIOD"
                if (config.showComparison && previousPeriodMetrics != null) {
                    val change = currentPeriodMetrics.netWorth.subtract(previousPeriodMetrics.netWorth)
                    if (change.signum() != 0 && previousPeriodMetrics.netWorth.signum() != 0) {
                        val percentage = change.multiply(BigDecimal("100"))
                            .divide(previousPeriodMetrics.netWorth.abs(), 1, RoundingMode.HALF_UP)
                        val up = change.signum() > 0
                        val trendColor = if (up) AppColors.positive else AppColors.negative
                        Text(
                            text = buildAnnotatedString {
                                withStyle(SpanStyle(color = trendColor)) {
                                    append("${if (up) "▲" else "▼"} ${if (up) "+" else ""}$percentage%")
                                }
                                append("   ")
                                withStyle(SpanStyle(color = AppColors.contentSecondary.copy(alpha = 0.7f))) {
                                    append("VS PRIOR PERIOD")
                                }
                            },
                            style = AppTextStyles.metaLabel.copy(fontWeight = FontWeight.Medium)
                        )
                    }
                }
            }

            // Three-column row or two-column row depending on mode.
            Row(
                horizontalArrangement = Arrangement.spacedBy(AeonTheme.dimensions.spacingLg),
                modifier = Modifier
                    .fillMaxWidth()
                    .aeonDashedTopRule()
                    .padding(top = 14.dp)
                    .height(IntrinsicSize.Min)
            ) {
                val cf = currentPeriodMetrics.cashFlow
                val prevCf = previousPeriodMetrics?.cashFlow
                val excludeSameCcy = config.excludeSameCurrencyTransfers
                val types = config.filter.transactionTypes
                val showIncome = types.isEmpty() || TransactionType.INCOME in types
                val showExpense = types.isEmpty() || TransactionType.EXPENSE in types
                val showTransfer = types.isEmpty() || types.any { it.isTransfer }

                when (config.displayMode) {
                    CashFlowDisplayMode.TYPE_DISTINCTION -> {
                        if (showIncome) {
                            NetWorthFlowColumn(
                                label = "INC",
                                amount = cf.totalIncomeInBase,
                                currency = displayCurrency,
                                color = AppColors.positive,
                                modifier = Modifier
                                    .weight(1f)
                                    .clickable {
                                        callbacks.onClickIncome?.invoke(
                                            config.filter.copy(transactionTypes = setOf(TransactionType.INCOME))
                                        )
                                    },
                                previousAmount = if (config.showComparison) prevCf?.totalIncomeInBase else null
                            )
                        }
                        if (showExpense) {
                            if (showIncome) NetWorthSeparator()
                            NetWorthFlowColumn(
                                label = "EXP",
                                amount = cf.totalExpensesInBase,
                                currency = displayCurrency,
                                color = AppColors.negative,
                                modifier = Modifier
                                    .weight(1f)
                                    .clickable {
                                        callbacks.onClickExpense?.invoke(
                                            config.filter.copy(transactionTypes = setOf(TransactionType.EXPENSE))
                                        )
                                    },
                                previousAmount = if (config.showComparison) prevCf?.totalExpensesInBase else null
                            )
                        }
                        if (showTransfer) {
                            if (showIncome || showExpense) NetWorthSeparator()
                            val tx = if (excludeSameCcy) cf.totalCrossCcyTransferInInBase + cf.totalCrossCcyTransferOutInBase
                            else cf.totalTransferInInBase + cf.totalTransferOutInBase
                            val prevTx = if (prevCf != null) {
                                if (excludeSameCcy) prevCf.totalCrossCcyTransferInInBase + prevCf.totalCrossCcyTransferOutInBase
                                else prevCf.totalTransferInInBase + prevCf.totalTransferOutInBase
                            } else null

                            NetWorthFlowColumn(
                                label = "TRF",
                                amount = tx,
                                currency = displayCurrency,
                                color = AppColors.accent,
                                modifier = Modifier
                                    .weight(1f)
                                    .clickable {
                                        callbacks.onClickTransfer?.invoke(
                                            config.filter.copy(transactionTypes = TransactionType.TRANSFER_TYPES)
                                        )
                                    },
                                previousAmount = if (config.showComparison) prevTx else null,
                                forceShowPlus = true
                            )
                        }
                    }

                    CashFlowDisplayMode.INFLOW_OUTFLOW -> {
                        val txIn = if (excludeSameCcy) cf.totalCrossCcyTransferInInBase else cf.totalTransferInInBase
                        val txOut = if (excludeSameCcy) cf.totalCrossCcyTransferOutInBase else cf.totalTransferOutInBase
                        val prevTxIn = if (excludeSameCcy) prevCf?.totalCrossCcyTransferInInBase else prevCf?.totalTransferInInBase
                        val prevTxOut = if (excludeSameCcy) prevCf?.totalCrossCcyTransferOutInBase else prevCf?.totalTransferOutInBase

                        val inflow = (if (showIncome) cf.totalIncomeInBase else BigDecimal.ZERO) + (if (showTransfer) txIn else BigDecimal.ZERO)
                        val outflow = (if (showExpense) cf.totalExpensesInBase else BigDecimal.ZERO) + (if (showTransfer) txOut else BigDecimal.ZERO)
                        
                        val prevInflow = if (prevCf != null) {
                            (if (showIncome) prevCf.totalIncomeInBase else BigDecimal.ZERO) + (if (showTransfer) (prevTxIn ?: BigDecimal.ZERO) else BigDecimal.ZERO)
                        } else null
                        val prevOutflow = if (prevCf != null) {
                            (if (showExpense) prevCf.totalExpensesInBase else BigDecimal.ZERO) + (if (showTransfer) (prevTxOut ?: BigDecimal.ZERO) else BigDecimal.ZERO)
                        } else null

                        NetWorthFlowColumn(
                            label = "IN",
                            amount = inflow,
                            currency = displayCurrency,
                            color = AppColors.positive,
                            modifier = Modifier
                                .weight(1f)
                                .clickable {
                                    callbacks.onClickInflow?.invoke(
                                        config.filter.copy(
                                            transactionTypes = buildSet {
                                                if (showIncome) add(TransactionType.INCOME)
                                                if (showTransfer) add(TransactionType.TRANSFER_INFLOW)
                                            }
                                        )
                                    )
                                },
                            previousAmount = if (config.showComparison) prevInflow else null
                        )
                        NetWorthSeparator()
                        NetWorthFlowColumn(
                            label = "OUT",
                            amount = outflow,
                            currency = displayCurrency,
                            color = AppColors.negative,
                            modifier = Modifier
                                .weight(1f)
                                .clickable {
                                    callbacks.onClickOutflow?.invoke(
                                        config.filter.copy(
                                            transactionTypes = buildSet {
                                                if (showExpense) add(TransactionType.EXPENSE)
                                                if (showTransfer) add(TransactionType.TRANSFER_OUTFLOW)
                                            }
                                        )
                                    )
                                },
                            previousAmount = if (config.showComparison) prevOutflow else null
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun NetWorthSeparator() {
    Box(
        modifier = Modifier
            .width(1.dp)
            .fillMaxHeight()
            .background(AppColors.outlineVariant)
    )
}

@Composable
private fun NetWorthFlowColumn(
    label: String,
    amount: BigDecimal,
    currency: String,
    color: Color,
    modifier: Modifier = Modifier,
    previousAmount: BigDecimal? = null,
    forceShowPlus: Boolean = false
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                text = label,
                style = AppTextStyles.sectionOverline.copy(fontSize = 10.sp),
                color = AppColors.contentSecondary.copy(alpha = 0.6f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f, fill = false)
            )
            if (previousAmount != null) {
                val change = amount.subtract(previousAmount)
                if (change.signum() != 0 && previousAmount.signum() != 0) {
                    val percentage = change.multiply(BigDecimal("100"))
                        .divide(previousAmount.abs(), 1, RoundingMode.HALF_UP)
                    val up = change.signum() > 0
                    
                    // Compact trend for huge percentages
                    val displayPercentage = if (percentage.abs() > BigDecimal("999")) {
                        "${if (percentage.signum() > 0) ">999" else "<-999"}%"
                    } else {
                        "${if (up) "+" else ""}$percentage%"
                    }

                    Text(
                        text = "${if (up) "▲" else "▼"} $displayPercentage",
                        style = AppTextStyles.metaLabel.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 9.sp
                        ),
                        color = color,
                        maxLines = 1
                    )
                }
            }
        }
        Text(
            text = CurrencyReprUi.formatCurrencyAnnotated(
                amount = amount,
                currencyCode = currency,
                spec = CurrencyDisplaySpec(
                    useSemanticColor = false, 
                    forceShowPlus = forceShowPlus,
                    shortenThreshold = 12
                )
            ),
            style = AppTextStyles.transactionAmount.copy(
                fontWeight = FontWeight.Medium,
                fontSize = 17.sp,
                letterSpacing = (-0.5).sp
            ),
            color = color,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
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
            SingleChoiceSegmentedButtonRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            ) {
                listOf("Small", "Medium", "Large", "X-Large").forEachIndexed { index, size ->
                    SegmentedButton(
                        selected = config.fontSize == size,
                        onClick = { callbacks.onFontSizeChange(config.cardId, size) },
                        shape = SegmentedButtonDefaults.itemShape(index = index, count = 4),
                        label = {
                            Text(
                                when (size) {
                                    "Small" -> "S"
                                    "Medium" -> "M"
                                    "Large" -> "L"
                                    else -> "XL"
                                }
                            )
                        }
                    )
                }
            }
        }
        CardSettingsSection("Display") {
            EntrySwitchRow(
                label = "Show Comparison",
                subtitle = "Show change from previous period",
                checked = config.showComparison,
                onCheckedChange = { callbacks.onShowComparisonChange(config.cardId, it) },
                isFlush = true
            )
        }
        CardSettingsSection("Layout") {
            SingleChoiceSegmentedButtonRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp)
            ) {
                CashFlowDisplayMode.entries.forEachIndexed { index, mode ->
                    SegmentedButton(
                        selected = config.displayMode == mode,
                        onClick = { callbacks.onDisplayModeChange(config.cardId, mode) },
                        shape = SegmentedButtonDefaults.itemShape(
                            index = index,
                            count = CashFlowDisplayMode.entries.size
                        ),
                        label = {
                            Text(
                                when (mode) {
                                    CashFlowDisplayMode.TYPE_DISTINCTION -> "Type"
                                    CashFlowDisplayMode.INFLOW_OUTFLOW -> "In / Out"
                                }
                            )
                        }
                    )
                }
            }
            if (config.displayMode == CashFlowDisplayMode.INFLOW_OUTFLOW) {
                com.osscabinet.ossWallet.ui.components.AeonSettingsToggle(
                    label = "Exclude same-currency transfers",
                    description = "Only used in Inflow / Outflow mode",
                    checked = config.excludeSameCurrencyTransfers,
                    onCheckedChange = {
                        callbacks.onExcludeSameCurrencyTransfersChange(config.cardId, it)
                    }
                )
            }
        }
    }
}

@ThemePreviews
@Composable
@Preview(heightDp = 360)
fun NetWorthCardGlassPreview() {
    val currentPeriodMetrics = NetWorthMetrics(
        netWorth = BigDecimal("152500.75"),
        cashFlow = CashFlowResult(
            totalIncomeInBase = BigDecimal("4500000.00"),
            totalExpensesInBase = BigDecimal("2350.50"),
            totalTransfersInBase = BigDecimal.ZERO,
            netInBase = BigDecimal("2149.50"),
            breakdown = emptyList(),
            filteredTransactions = emptyList()
        )
    )
    val previousPeriodMetrics = NetWorthMetrics(
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
        AeonHazeProvider(drawContours = true) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
                    .height(400.dp)
            ) {
                NetWorthCard(
                    currentPeriodMetrics = currentPeriodMetrics,
                    previousPeriodMetrics = previousPeriodMetrics,
                    baseCurrency = "USD",
                    config = NetWorthCardConfig(cardId = "net_worth", showComparison = true)
                )
            }
        }
    }
}

@androidx.compose.ui.tooling.preview.Preview(name = "Light Mode", showBackground = true)
@Composable
fun NetWorthCardLightPreview() {
    OssWalletTheme(theme = "Light") {
        NetWorthCardPreviewContent()
    }
}

@androidx.compose.ui.tooling.preview.Preview(
    name = "Dark Mode",
    showBackground = true,
    uiMode = android.content.res.Configuration.UI_MODE_NIGHT_YES
)
@Composable
fun NetWorthCardDarkPreview() {
    OssWalletTheme(theme = "Dark") {
        NetWorthCardPreviewContent()
    }
}

@androidx.compose.ui.tooling.preview.Preview(
    name = "Amoled Mode",
    showBackground = true,
    uiMode = android.content.res.Configuration.UI_MODE_NIGHT_YES
)
@Composable
fun NetWorthCardAmoledPreview() {
    OssWalletTheme(theme = "Amoled") {
        NetWorthCardPreviewContent()
    }
}

@Composable
private fun NetWorthCardPreviewContent() {
    val currentPeriodMetrics = NetWorthMetrics(
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
    val previousPeriodMetrics = NetWorthMetrics(
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

    Box(modifier = Modifier.padding(16.dp)) {
        NetWorthCard(
            currentPeriodMetrics = currentPeriodMetrics,
            previousPeriodMetrics = previousPeriodMetrics,
            baseCurrency = "USD",
            config = NetWorthCardConfig(cardId = "net_worth", showComparison = true)
        )
    }
}
