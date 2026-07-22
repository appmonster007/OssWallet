package com.osscabinet.ossWallet.ui.components.cards.types

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
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AccountBalance
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.osscabinet.ossWallet.domain.card.CardInput
import com.osscabinet.ossWallet.domain.card.CardRenderData
import com.osscabinet.ossWallet.domain.card.CardUpdateHandler
import com.osscabinet.ossWallet.domain.card.NetWorthMetrics
import com.osscabinet.ossWallet.domain.model.TransactionFilter
import com.osscabinet.ossWallet.domain.model.TransactionType
import com.osscabinet.ossWallet.domain.model.analytics.CashFlowResult
import com.osscabinet.ossWallet.domain.model.isTransfer
import com.osscabinet.ossWallet.domain.model.settings.AccountingPerspective
import com.osscabinet.ossWallet.domain.model.settings.NetWorthCardConfig
import com.osscabinet.ossWallet.ui.components.AeonHazeProvider
import com.osscabinet.ossWallet.ui.components.AeonSettingsToggle
import com.osscabinet.ossWallet.ui.components.aeonDashedTopRule
import com.osscabinet.ossWallet.ui.components.cards.AeonExpressiveCard
import com.osscabinet.ossWallet.ui.components.cards.AeonInteractionCallbacks
import com.osscabinet.ossWallet.ui.components.cards.CardCallbacks
import com.osscabinet.ossWallet.ui.components.cards.CardPerspectiveSettings
import com.osscabinet.ossWallet.ui.components.cards.CardSettingsSection
import com.osscabinet.ossWallet.ui.components.cards.CardLayoutProps
import com.osscabinet.ossWallet.ui.components.cards.CardWithCallbacks
import com.osscabinet.ossWallet.ui.components.cards.resolveDrillDown
import com.osscabinet.ossWallet.ui.components.cards.resolveSeeAll
import com.osscabinet.ossWallet.ui.theme.AeonTheme
import com.osscabinet.ossWallet.ui.theme.AppColors
import com.osscabinet.ossWallet.ui.theme.AppTextStyles
import com.osscabinet.ossWallet.ui.theme.OssWalletTheme
import com.osscabinet.ossWallet.ui.theme.ThemePreviews
import com.osscabinet.ossWallet.ui.util.CurrencyDisplaySpec
import com.osscabinet.ossWallet.ui.util.CurrencyPartStyles
import com.osscabinet.ossWallet.ui.util.CurrencyReprUi
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
    layoutProps: CardLayoutProps = CardLayoutProps(),
) {
    val displayFilter = layoutProps.computeDisplayFilter(config.filter)
    val currentOnUpdate = rememberUpdatedState(callbacks.onFilterChange)

    AeonExpressiveCard(
        title = config.title,
        icon = Icons.Rounded.AccountBalance,
        modifier = layoutProps.modifier,
        headerModifier = layoutProps.headerModifier,
        onClick = callbacks.onSeeAll,
        filterEnabled = layoutProps.showFilterButton,
        filter = displayFilter,
        baseFilter = layoutProps.baseFilter,
        filterRules = layoutProps.filterRules ?: config.filterRules,
        filterContext = layoutProps.filterContext,
        onFilterChange = { currentOnUpdate.value?.invoke(config.cardId, it) },
        onSaveFilterClick = callbacks.onSaveFilterClick,
        onDelete = callbacks.onDelete,
        settingsContent = {
            NetWorthSettingsContent(config = config, callbacks = callbacks)
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
                    color = contentColor.copy(alpha = 0.9f),
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
                    color = contentColor,
                    modifier = remember(config.perspective, config.filter, callbacks) {
                        Modifier.clickable {
                            if (config.perspective is AccountingPerspective.Accounting) {
                                callbacks.onClickNet?.invoke(config.filter)
                            } else {
                                callbacks.onClickNetFlow?.invoke(config.filter)
                            }
                        }
                    }
                )

                // Mono trend readout — "▲ +8.2%  VS PRIOR PERIOD"
                if (config.showComparison && (previousPeriodMetrics != null)) {
                    val change =
                        currentPeriodMetrics.netWorth.subtract(previousPeriodMetrics.netWorth)
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
                val types = config.filter.transactionTypes
                val showIncome = types.isEmpty() || TransactionType.INCOME in types
                val showExpense = types.isEmpty() || TransactionType.EXPENSE in types
                val showTransfer = types.isEmpty() || types.any { it.isTransfer }

                when (config.perspective) {
                    is AccountingPerspective.Accounting -> {
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
                                            config.filter.copy(
                                                transactionTypes = setOf(
                                                    TransactionType.INCOME
                                                )
                                            )
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
                                            config.filter.copy(
                                                transactionTypes = setOf(
                                                    TransactionType.EXPENSE
                                                )
                                            )
                                        )
                                    },
                                previousAmount = if (config.showComparison) prevCf?.totalExpensesInBase else null
                            )
                        }
                        if (showTransfer) {
                            if (showIncome || showExpense) NetWorthSeparator()
                            if (config.splitTransfers) {
                                NetWorthFlowColumn(
                                    label = "TRF IN",
                                    amount = cf.totalTransferInInBase,
                                    currency = displayCurrency,
                                    color = AppColors.accent,
                                    modifier = Modifier
                                        .weight(1f)
                                        .clickable {
                                            callbacks.onClickTransfer?.invoke(
                                                config.filter.copy(transactionTypes = setOf(TransactionType.TRANSFER_INFLOW))
                                            )
                                        },
                                    previousAmount = if (config.showComparison) prevCf?.totalTransferInInBase else null,
                                    forceShowPlus = true
                                )
                                NetWorthSeparator()
                                NetWorthFlowColumn(
                                    label = "TRF OUT",
                                    amount = cf.totalTransferOutInBase,
                                    currency = displayCurrency,
                                    color = AppColors.accent,
                                    modifier = Modifier
                                        .weight(1f)
                                        .clickable {
                                            callbacks.onClickTransfer?.invoke(
                                                config.filter.copy(transactionTypes = setOf(TransactionType.TRANSFER_OUTFLOW))
                                            )
                                        },
                                    previousAmount = if (config.showComparison) prevCf?.totalTransferOutInBase else null
                                )
                            } else {
                                NetWorthFlowColumn(
                                    label = "TRF",
                                    amount = cf.totalNetTransferInBase,
                                    currency = displayCurrency,
                                    color = AppColors.accent,
                                    modifier = Modifier
                                        .weight(1f)
                                        .clickable {
                                            callbacks.onClickTransfer?.invoke(
                                                config.filter.copy(transactionTypes = TransactionType.TRANSFER_TYPES)
                                            )
                                        },
                                    previousAmount = if (config.showComparison) prevCf?.totalNetTransferInBase else null,
                                    forceShowPlus = true
                                )
                            }
                        }
                    }

                    is AccountingPerspective.CashFlow -> {
                        // toPerspective() absorbs transferIn into income and zeroes
                        // totalTransferInInBase, so treat showTransfer as showInflow.
                        val showInflow  = showIncome || showTransfer
                        val showOutflow = showExpense || showTransfer
                        val inflow      = if (showInflow)  cf.totalInflowInBase  else BigDecimal.ZERO
                        val outflow     = if (showOutflow) cf.totalOutflowInBase else BigDecimal.ZERO
                        val prevInflow  = if (showInflow)  prevCf?.totalInflowInBase  else null
                        val prevOutflow = if (showOutflow) prevCf?.totalOutflowInBase else null

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
                    shortenThreshold = 11
                )
            ),
            style = AppTextStyles.transactionAmount.copy(
                fontWeight = FontWeight.Medium,
                fontSize = 14.sp,
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
private fun NetWorthSettingsContent(
    config: NetWorthCardConfig,
    callbacks: CardCallbacks.NetWorth
) {
    val currentOnFontSizeChange = rememberUpdatedState(callbacks.onFontSizeChange)
    val currentOnShowComparisonChange = rememberUpdatedState(callbacks.onShowComparisonChange)
    val currentOnPerspectiveChange = rememberUpdatedState(callbacks.onPerspectiveChange)
    val currentOnExcludeInternalTransfersChange = rememberUpdatedState(callbacks.onExcludeInternalTransfersChange)
    val currentOnSplitTransfersChange = rememberUpdatedState(callbacks.onSplitTransfersChange)

    CardSettingsSection("Font Size") {
        SingleChoiceSegmentedButtonRow(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp)
        ) {
            listOf("Small", "Medium", "Large", "X-Large").forEachIndexed { index, size ->
                SegmentedButton(
                    selected = config.fontSize == size,
                    onClick = { currentOnFontSizeChange.value(config.cardId, size) },
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
            currentOnExcludeInternalTransfersChange.value(
                config.cardId,
                it
            )
        },
        splitTransfers = config.splitTransfers,
        onSplitTransfersChange = { currentOnSplitTransfersChange.value(config.cardId, it) }
    )
}

@ThemePreviews
@Composable
@Preview(heightDp = 360)
fun NetWorthCardGlassPreview() {
    val currentPeriodMetrics = NetWorthMetrics(
        netWorth = BigDecimal("152500.75"),
        cashFlow = CashFlowResult(
            totalIncomeInBase = BigDecimal("4500.00"),
            totalExpensesInBase = BigDecimal("-2350.50"),
            totalTransfersInBase = BigDecimal("500.00"),
            totalTransferInInBase = BigDecimal("1000.00"),
            totalTransferOutInBase = BigDecimal("-500.00"),
            netInBase = BigDecimal("2649.50"),
            breakdown = emptyList(),
            filteredTransactions = emptyList()
        )
    )
    val previousPeriodMetrics = NetWorthMetrics(
        netWorth = BigDecimal("14100.25"),
        cashFlow = CashFlowResult(
            totalIncomeInBase = BigDecimal("4200.00"),
            totalExpensesInBase = BigDecimal("-2100.00"),
            totalTransfersInBase = BigDecimal("400.00"),
            totalTransferInInBase = BigDecimal("800.00"),
            totalTransferOutInBase = BigDecimal("-400.00"),
            netInBase = BigDecimal("2500.00"),
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

@Preview(name = "Light Mode", showBackground = true)
@Composable
fun NetWorthCardLightPreview() {
    OssWalletTheme(theme = "Light") {
        NetWorthCardPreviewContent()
    }
}

@Preview(
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

@Preview(
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
            totalExpensesInBase = BigDecimal("-2350.50"),
            totalTransfersInBase = BigDecimal("500.00"),
            totalTransferInInBase = BigDecimal("1000.00"),
            totalTransferOutInBase = BigDecimal("-500.00"),
            netInBase = BigDecimal("2649.50"),
            breakdown = emptyList(),
            filteredTransactions = emptyList()
        )
    )
    val previousPeriodMetrics = NetWorthMetrics(
        netWorth = BigDecimal("14100.25"),
        cashFlow = CashFlowResult(
            totalIncomeInBase = BigDecimal("4200.00"),
            totalExpensesInBase = BigDecimal("-2100.00"),
            totalTransfersInBase = BigDecimal("400.00"),
            totalTransferInInBase = BigDecimal("800.00"),
            totalTransferOutInBase = BigDecimal("-400.00"),
            netInBase = BigDecimal("2500.00"),
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

/**
 * Maps [NetWorthCardConfig] + [CardRenderData.NetWorth] to [CardWithCallbacks.NetWorth].
 */
fun mapNetWorth(
    config: NetWorthCardConfig,
    renderData: CardRenderData.NetWorth,
    input: CardInput,
    updateHandler: CardUpdateHandler,
    onDelete: ((String) -> Unit)?,
    onRecordsClick: (TransactionFilter) -> Unit,
    callbacks: AeonInteractionCallbacks,
    filterSheetEnabled: Boolean
) = CardWithCallbacks.NetWorth(
    renderData = renderData,
    callbacks = CardCallbacks.NetWorth(
        onFontSizeChange = updateHandler::updateCardNetWorthFontSize,
        onShowComparisonChange = updateHandler::updateCardShowComparison,
        onPerspectiveChange = updateHandler::updateCardPerspective,
        onExcludeInternalTransfersChange = updateHandler::updateCardExcludeInternalTransfers,
        onSplitTransfersChange = updateHandler::updateCardSplitTransfers,
        onFilterChange = updateHandler::updateCardNetWorthFilter,
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
        onSeeAll = { onRecordsClick(resolveSeeAll(input, config)) },
        onDelete = onDelete?.let { { it(config.cardId) } },
        onSaveFilterClick = callbacks.filterActions.onSaveClick
    ),
    filterRules = config.filterRules,
    baseFilter = input.filter,
    filterSheetEnabled = filterSheetEnabled
)
