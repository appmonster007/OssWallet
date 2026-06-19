package com.osscabinet.ossWallet.ui.screens.dashboard.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Analytics
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.osscabinet.ossWallet.domain.card.CardFilterRules
import com.osscabinet.ossWallet.domain.model.TransactionFilter
import com.osscabinet.ossWallet.domain.model.TransactionType
import com.osscabinet.ossWallet.domain.model.isTransfer


import com.osscabinet.ossWallet.domain.model.analytics.CashFlowBreakdownItem
import com.osscabinet.ossWallet.domain.model.analytics.CashFlowResult
import com.osscabinet.ossWallet.domain.model.settings.CashFlowCardConfig
import com.osscabinet.ossWallet.domain.model.settings.AccountingPerspective
import com.osscabinet.ossWallet.ui.util.CurrencyDisplaySpec
import com.osscabinet.ossWallet.ui.util.CurrencyReprUi
import com.osscabinet.ossWallet.ui.components.AeonBaseConversionToggle
import com.osscabinet.ossWallet.ui.components.AeonSettingsToggle
import com.osscabinet.ossWallet.ui.components.aeonDashedTopRule
import com.osscabinet.ossWallet.ui.components.aeonHairlineBorder
import com.osscabinet.ossWallet.ui.components.cards.AeonExpressiveCard
import com.osscabinet.ossWallet.ui.components.cards.CardCallbacks
import com.osscabinet.ossWallet.ui.components.cards.CardPerspectiveSettings
import com.osscabinet.ossWallet.ui.components.cards.CardSettingsSection
import com.osscabinet.ossWallet.ui.components.cards.CardSettingsSheet
import com.osscabinet.ossWallet.ui.components.cards.FilterSelectionContext
import com.osscabinet.ossWallet.ui.components.entry.EntrySwitchRow
import com.osscabinet.ossWallet.ui.theme.AppColors
import com.osscabinet.ossWallet.ui.theme.AppTextStyles
import com.osscabinet.ossWallet.ui.theme.OssWalletTheme
import java.math.BigDecimal

@Composable
fun CashFlowCard(
    cashFlowResult: CashFlowResult?,
    baseCurrency: String,
    config: CashFlowCardConfig,
    callbacks: CardCallbacks.CashFlow = CardCallbacks.CashFlow(),
    filterContext: FilterSelectionContext = FilterSelectionContext(),
    baseFilter: TransactionFilter = TransactionFilter(),
    inheritedFilter: TransactionFilter = TransactionFilter(),
    filterRules: CardFilterRules? = null,
    screenOwnsTimeline: Boolean = false,
    showFilterButton: Boolean = true,
    modifier: Modifier = Modifier,
    headerModifier: Modifier = Modifier,
) {
    val totalIncomeInBase = cashFlowResult?.totalIncomeInBase ?: BigDecimal.ZERO
    val totalExpenseInBase = cashFlowResult?.totalExpensesInBase ?: BigDecimal.ZERO

    val displayFilter =
        if (screenOwnsTimeline || inheritedFilter.timelineRange !is com.osscabinet.ossWallet.domain.model.timeline.TimelineRange.AllTime)
            config.filter.copy(timelineRange = inheritedFilter.timelineRange)
        else config.filter

    AeonExpressiveCard(
        title = config.title,
        icon = Icons.Rounded.Analytics,
        modifier = modifier,
        headerModifier = headerModifier,
        filterEnabled = showFilterButton,
        filter = displayFilter,
        baseFilter = baseFilter,
        filterRules = filterRules ?: config.filterRules,
        filterContext = filterContext,
        onFilterChange = { callbacks.onFilterChange(config.cardId, it) },
        onSaveFilterClick = callbacks.onSaveFilterClick,
        displaySettings = { onDismiss ->
            CashFlowSettingsSheet(
                config = config,
                baseCurrency = baseCurrency,
                callbacks = callbacks,
                onDismiss = onDismiss
            )
        }
    ) {
        CashFlowTable(
            config = config,
            result = cashFlowResult ?: CashFlowResult.empty(),
            baseCurrency = baseCurrency,
            callbacks = callbacks,
        )
    }
}

private data class TableColumn(
    val label: String,
    val key: String,
    val color: Color,
    val totalInBase: BigDecimal,
    val nativeOf: (CashFlowBreakdownItem) -> BigDecimal,
    val baseOf: (CashFlowBreakdownItem) -> BigDecimal,
    val onCellClick: ((String) -> Unit)?,
    val isSigned: Boolean = false,
)

@Composable
private fun CashFlowTable(
    config: CashFlowCardConfig,
    result: CashFlowResult,
    baseCurrency: String,
    callbacks: CardCallbacks.CashFlow,
) {
    val types = config.filter.transactionTypes
    val showIncome = types.isEmpty() || TransactionType.INCOME in types
    val showExpense = types.isEmpty() || TransactionType.EXPENSE in types
    val showTransfer = types.isEmpty() || types.any { it.isTransfer }
    val baseFilterForDrill = config.filter
    val positiveColor = AppColors.positive
    val negativeColor = AppColors.negative
    val accentColor = AppColors.accent

    val columns: List<TableColumn> = remember(config, types, result, positiveColor, negativeColor, accentColor) {
        when (config.perspective) {
            is AccountingPerspective.Accounting -> buildList {
                if (showIncome) add(
                    TableColumn(
                        label = "INCOME", key = "inc", color = positiveColor,
                        totalInBase = result.totalIncomeInBase,
                        nativeOf = { it.income }, baseOf = { it.incomeInBase },
                        onCellClick = { ccy ->
                            callbacks.onClickIncome?.invoke(
                                baseFilterForDrill.copy(
                                    transactionTypes = setOf(TransactionType.INCOME),
                                    currencies = setOf(ccy),
                                )
                            )
                        }
                    )
                )
                if (showExpense) add(
                    TableColumn(
                        label = "EXPENSE", key = "exp", color = negativeColor,
                        totalInBase = result.totalExpensesInBase,
                        nativeOf = { it.expense }, baseOf = { it.expenseInBase },
                        onCellClick = { ccy ->
                            callbacks.onClickExpense?.invoke(
                                baseFilterForDrill.copy(
                                    transactionTypes = setOf(TransactionType.EXPENSE),
                                    currencies = setOf(ccy),
                                )
                            )
                        }
                    )
                )
                if (showTransfer) add(
                    TableColumn(
                        label = "TRANSFER", key = "trf", color = accentColor,
                        totalInBase = result.totalNetTransferInBase,
                        nativeOf = { it.netTransfer },
                        baseOf = { it.netTransferInBase },
                        onCellClick = { ccy ->
                            callbacks.onClickTransfer?.invoke(
                                baseFilterForDrill.copy(
                                    transactionTypes = TransactionType.TRANSFER_TYPES,
                                    currencies = setOf(ccy),
                                )
                            )
                        },
                        isSigned = true
                    )
                )
            }
            is AccountingPerspective.CashFlow -> {
                val inflowTypes = buildSet {
                    if (showIncome) add(TransactionType.INCOME)
                    if (showTransfer) add(TransactionType.TRANSFER_INFLOW)
                }
                val outflowTypes = buildSet {
                    if (showExpense) add(TransactionType.EXPENSE)
                    if (showTransfer) add(TransactionType.TRANSFER_OUTFLOW)
                }
                listOf(
                    TableColumn(
                        label = "INFLOW", key = "in", color = positiveColor,
                        totalInBase = (if (showIncome) result.totalIncomeInBase else BigDecimal.ZERO) +
                                (if (showTransfer) result.totalTransferInInBase else BigDecimal.ZERO),
                        nativeOf = {
                            (if (showIncome) it.income else BigDecimal.ZERO) +
                                    (if (showTransfer) it.transferIn else BigDecimal.ZERO)
                        },
                        baseOf = {
                            (if (showIncome) it.incomeInBase else BigDecimal.ZERO) +
                                    (if (showTransfer) it.transferInInBase else BigDecimal.ZERO)
                        },
                        onCellClick = { ccy ->
                            callbacks.onClickInflow?.invoke(
                                baseFilterForDrill.copy(
                                    transactionTypes = inflowTypes,
                                    currencies = setOf(ccy),
                                )
                            )
                        }
                    ),
                    TableColumn(
                        label = "OUTFLOW", key = "out", color = negativeColor,
                        totalInBase = (if (showExpense) result.totalExpensesInBase else BigDecimal.ZERO) +
                                (if (showTransfer) result.totalTransferOutInBase else BigDecimal.ZERO),
                        nativeOf = {
                            (if (showExpense) it.expense else BigDecimal.ZERO) +
                                    (if (showTransfer) it.transferOut else BigDecimal.ZERO)
                        },
                        baseOf = {
                            (if (showExpense) it.expenseInBase else BigDecimal.ZERO) +
                                    (if (showTransfer) it.transferOutInBase else BigDecimal.ZERO)
                        },
                        onCellClick = { ccy ->
                            callbacks.onClickOutflow?.invoke(
                                baseFilterForDrill.copy(
                                    transactionTypes = outflowTypes,
                                    currencies = setOf(ccy),
                                )
                            )
                        }
                    ),
                )
            }
        }
    }

    val netColumn: TableColumn? = if (config.showNetColumn) {
        val netTotalInBase = columns.fold(BigDecimal.ZERO) { acc, c -> acc + c.totalInBase }
        TableColumn(
            label = if (config.perspective is AccountingPerspective.Accounting) "NET" else "NET FLOW",
            key = "net",
            color = accentColor,
            totalInBase = netTotalInBase,
            nativeOf = { item ->
                columns.fold(BigDecimal.ZERO) { acc, c ->
                    val v = c.nativeOf(item)
                    acc + v
                }
            },
            baseOf = { item ->
                columns.fold(BigDecimal.ZERO) { acc, c ->
                    val v = c.baseOf(item)
                    acc + v
                }
            },
            isSigned = true,
            onCellClick = { ccy ->
                if (config.perspective is AccountingPerspective.Accounting) {
                    callbacks.onClickNet?.invoke(config.filter.copy(currencies = setOf(ccy)))
                } else {
                    callbacks.onClickNetFlow?.invoke(config.filter.copy(currencies = setOf(ccy)))
                }
            },
        )
    } else null

    val activeColumns = columns + listOfNotNull(netColumn)
    val cellWidth: Dp = when (activeColumns.size) {
        in 0..2 -> 130.dp
        3 -> 120.dp
        else -> 110.dp
    }
    val ccyColWidth: Dp = 64.dp

    val scrollState = rememberScrollState()
    val rows = result.breakdown

    Column(modifier = Modifier.fillMaxWidth()) {
        // Header row
        Row(modifier = Modifier.fillMaxWidth()) {
            HeaderCell(text = "CCY", width = ccyColWidth)
            Row(modifier = Modifier.horizontalScroll(scrollState)) {
                activeColumns.forEach { col ->
                    HeaderCell(text = col.label, width = cellWidth, align = Alignment.End)
                }
            }
        }

        // Σ totals row at TOP
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 6.dp)
                .clip(RoundedCornerShape(6.dp))
                .background(AppColors.cardSurfaceProminent.copy(alpha = 0.5f))
                .padding(vertical = 8.dp)
        ) {
            TotalsCcyCell(width = ccyColWidth)
            Row(modifier = Modifier.horizontalScroll(scrollState)) {
                activeColumns.forEach { col ->
                    TotalsCell(
                        amountInBase = col.totalInBase,
                        baseCurrency = baseCurrency,
                        color = col.color,
                        signed = col.isSigned,
                        width = cellWidth,
                    )
                }
            }
        }

        // Per-currency body rows
        rows.forEach { item ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 10.dp)
            ) {
                CcyCell(text = item.currency, width = ccyColWidth)
                Row(modifier = Modifier.horizontalScroll(scrollState)) {
                    activeColumns.forEach { col ->
                        BreakdownCell(
                            native = col.nativeOf(item),
                            base = col.baseOf(item),
                            nativeCcy = item.currency,
                            baseCcy = baseCurrency,
                            color = col.color,
                            signed = col.isSigned,
                            showBaseConversion = config.showBaseConversion,
                            width = cellWidth,
                            onClick = col.onCellClick?.let { cb -> { cb(item.currency) } },
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun HeaderCell(text: String, width: Dp, align: Alignment.Horizontal = Alignment.Start) {
    Box(
        modifier = Modifier.width(width),
        contentAlignment = if (align == Alignment.End) Alignment.CenterEnd else Alignment.CenterStart,
    ) {
        Text(
            text = text,
            style = AppTextStyles.sectionOverline,
            color = AppColors.contentSecondary.copy(alpha = 0.7f),
            maxLines = 1,
        )
    }
}

@Composable
private fun CcyCell(text: String, width: Dp) {
    Box(
        modifier = Modifier.width(width),
        contentAlignment = Alignment.CenterStart,
    ) {
        Text(
            text = text,
            style = AppTextStyles.metaLabel.copy(fontWeight = FontWeight.Medium),
            color = AppColors.contentPrimary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
private fun TotalsCcyCell(width: Dp) {
    Box(
        modifier = Modifier
            .width(width)
            .padding(start = 4.dp),
        contentAlignment = Alignment.CenterStart,
    ) {
        Text(
            text = "Σ",
            style = AppTextStyles.metaLabel.copy(fontWeight = FontWeight.Bold),
            color = AppColors.contentSecondary,
        )
    }
}

@Composable
private fun TotalsCell(
    amountInBase: BigDecimal,
    baseCurrency: String,
    color: Color,
    signed: Boolean,
    width: Dp,
) {
    val text = if (amountInBase.signum() == 0 && signed) {
        buildAnnotatedString { append("—") }
    } else {
        CurrencyReprUi.formatCurrencyAnnotated(
            amount = amountInBase,
            currencyCode = baseCurrency,
            spec = CurrencyDisplaySpec(forceShowPlus = signed, useSemanticColor = false)
        )
    }
    Box(
        modifier = Modifier
            .width(width)
            .padding(end = 4.dp),
        contentAlignment = Alignment.CenterEnd,
    ) {
        Text(
            text = text,
            style = AppTextStyles.metaLabel.copy(fontWeight = FontWeight.SemiBold),
            color = color,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
private fun BreakdownCell(
    native: BigDecimal,
    base: BigDecimal,
    nativeCcy: String,
    baseCcy: String,
    color: Color,
    signed: Boolean,
    showBaseConversion: Boolean,
    width: Dp,
    onClick: (() -> Unit)?,
) {
    val isZero = native.signum() == 0
    val nativeText = if (isZero && signed) {
        buildAnnotatedString { append("—") }
    } else {
        CurrencyReprUi.formatCurrencyAnnotated(
            amount = native,
            currencyCode = nativeCcy,
            spec = CurrencyDisplaySpec(forceShowPlus = signed, useSemanticColor = false)
        )
    }
    val clickModifier = if (onClick != null && !isZero) Modifier.clickable { onClick() } else Modifier
    Column(
        modifier = Modifier
            .width(width)
            .then(clickModifier)
            .padding(end = 4.dp),
        horizontalAlignment = Alignment.End,
    ) {
        Text(
            text = nativeText,
            style = AppTextStyles.transactionAmount.copy(fontWeight = FontWeight.Normal),
            color = if (isZero) AppColors.contentSecondary.copy(alpha = 0.5f) else color,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        if (!isZero && showBaseConversion && nativeCcy != baseCcy) {
            Text(
                text = CurrencyReprUi.formatCurrencyAnnotated(
                    amount = base,
                    currencyCode = baseCcy,
                    spec = CurrencyDisplaySpec(forceShowPlus = signed, useSemanticColor = false, isApproximate = true)
                ),
                style = AppTextStyles.metaLabel,
                color = AppColors.contentSecondary.copy(alpha = 0.6f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CashFlowSettingsSheet(
    config: CashFlowCardConfig,
    baseCurrency: String,
    callbacks: CardCallbacks.CashFlow,
    onDismiss: () -> Unit
) {
    CardSettingsSheet(title = "Cash Flow", onDismiss = onDismiss, onDelete = callbacks.onDelete) {
        CardSettingsSection("Display") {
            AeonBaseConversionToggle(
                baseCurrency = baseCurrency,
                showBaseConversion = config.showBaseConversion,
                onShowBaseConversionChange = {
                    callbacks.onShowBaseConversionChange(config.cardId, it)
                }
            )
            AeonSettingsToggle(
                label = "Show NET column",
                checked = config.showNetColumn,
                onCheckedChange = {
                    callbacks.onShowNetColumnChange(config.cardId, it)
                }
            )
        }
        CardPerspectiveSettings(
            perspective = config.perspective,
            onPerspectiveChange = { callbacks.onPerspectiveChange(config.cardId, it) }
        )
    }
}

@androidx.compose.ui.tooling.preview.Preview(name = "Light Mode", showBackground = true)
@Composable
fun CashFlowCardLightPreview() {
    OssWalletTheme(theme = "Light") {
        CashFlowCardPreviewContent()
    }
}

@androidx.compose.ui.tooling.preview.Preview(
    name = "Dark Mode",
    showBackground = true,
    uiMode = android.content.res.Configuration.UI_MODE_NIGHT_YES
)
@Composable
fun CashFlowCardDarkPreview() {
    OssWalletTheme(theme = "Dark") {
        CashFlowCardPreviewContent()
    }
}

@androidx.compose.ui.tooling.preview.Preview(
    name = "Amoled Mode",
    showBackground = true,
    uiMode = android.content.res.Configuration.UI_MODE_NIGHT_YES
)
@Composable
fun CashFlowCardAmoledPreview() {
    OssWalletTheme(theme = "Amoled") {
        CashFlowCardPreviewContent()
    }
}

@Composable
private fun CashFlowCardPreviewContent() {
    val sampleBreakdown = listOf(
        CashFlowBreakdownItem(
            currency = "USD",
            income = BigDecimal("5000.00"),
            incomeInBase = BigDecimal("5000.00"),
            expense = BigDecimal("-3500.00"),
            expenseInBase = BigDecimal("-3500.00"),
            transfer = BigDecimal("200.00"),
            transferInBase = BigDecimal("200.00"),
            transferIn = BigDecimal("400.00"),
            transferInInBase = BigDecimal("400.00"),
            transferOut = BigDecimal("-200.00"),
            transferOutInBase = BigDecimal("-200.00")
        ),
        CashFlowBreakdownItem(
            currency = "EUR",
            income = BigDecimal("1000.00"),
            incomeInBase = BigDecimal("1100.00"),
            expense = BigDecimal("-800.00"),
            expenseInBase = BigDecimal("-880.00"),
            transfer = BigDecimal("100.00"),
            transferInBase = BigDecimal("110.00"),
            transferIn = BigDecimal("200.00"),
            transferInInBase = BigDecimal("220.00"),
            transferOut = BigDecimal("-100.00"),
            transferOutInBase = BigDecimal("-110.00")
        )
    )
    val result = CashFlowResult(
        totalIncomeInBase = BigDecimal("6100.00"),
        totalExpensesInBase = BigDecimal("-4380.00"),
        totalTransfersInBase = BigDecimal("310.00"),
        totalTransferInInBase = BigDecimal("620.00"),
        totalTransferOutInBase = BigDecimal("-310.00"),
        netInBase = BigDecimal("1720.00"),
        breakdown = sampleBreakdown,
        filteredTransactions = emptyList()
    )

    Box(modifier = Modifier.padding(16.dp)) {
        CashFlowCard(
            cashFlowResult = result,
            baseCurrency = "USD",
            config = CashFlowCardConfig(cardId = "preview")
        )
    }
}
