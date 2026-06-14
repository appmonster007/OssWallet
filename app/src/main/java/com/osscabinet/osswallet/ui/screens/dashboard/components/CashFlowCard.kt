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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.osscabinet.ossWallet.domain.card.CardFilterRules
import com.osscabinet.ossWallet.domain.model.FlowDirection
import com.osscabinet.ossWallet.domain.model.TransactionFilter
import com.osscabinet.ossWallet.domain.model.TransactionType
import com.osscabinet.ossWallet.domain.model.analytics.CashFlowBreakdownItem
import com.osscabinet.ossWallet.domain.model.analytics.CashFlowResult
import com.osscabinet.ossWallet.domain.model.settings.CashFlowCardConfig
import com.osscabinet.ossWallet.domain.model.settings.CashFlowDisplayMode
import com.osscabinet.ossWallet.domain.service.currency.CurrencyFormatter.formatAuto
import com.osscabinet.ossWallet.ui.components.AeonBaseConversionToggle
import com.osscabinet.ossWallet.ui.components.aeonDashedTopRule
import com.osscabinet.ossWallet.ui.components.aeonHairlineBorder
import com.osscabinet.ossWallet.ui.components.cards.AeonExpressiveCard
import com.osscabinet.ossWallet.ui.components.cards.CardCallbacks
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
        val types = config.filter.transactionTypes
        if (types.isEmpty()) {
            EmptyTypesState()
        } else {
            CashFlowTable(
                config = config,
                result = cashFlowResult ?: CashFlowResult.empty(),
                baseCurrency = baseCurrency,
                callbacks = callbacks,
            )
        }
    }
}

@Composable
private fun EmptyTypesState() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 24.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = "No transaction types selected. Open settings to choose what to show.",
            style = AppTextStyles.metaLabel,
            color = AppColors.contentSecondary,
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
    val showIncome = TransactionType.INCOME in types
    val showExpense = TransactionType.EXPENSE in types
    val showTransfer = TransactionType.TRANSFER in types
    val excludeSameCcy = config.excludeSameCurrencyTransfers

    // Pick transfer source: cross-ccy-only when toggle is on, gross otherwise.
    fun txIn(item: CashFlowBreakdownItem) = if (excludeSameCcy) item.crossCcyTransferIn else item.transferIn
    fun txInBase(item: CashFlowBreakdownItem) = if (excludeSameCcy) item.crossCcyTransferInInBase else item.transferInInBase
    fun txOut(item: CashFlowBreakdownItem) = if (excludeSameCcy) item.crossCcyTransferOut else item.transferOut
    fun txOutBase(item: CashFlowBreakdownItem) = if (excludeSameCcy) item.crossCcyTransferOutInBase else item.transferOutInBase

    val totalTxInBase = if (excludeSameCcy) result.totalCrossCcyTransferInInBase else result.totalTransferInInBase
    val totalTxOutBase = if (excludeSameCcy) result.totalCrossCcyTransferOutInBase else result.totalTransferOutInBase

    val baseFilterForDrill = config.filter
    val positiveColor = AppColors.positive
    val negativeColor = AppColors.negative
    val accentColor = AppColors.accent

    val columns: List<TableColumn> = remember(config, types, excludeSameCcy, result, positiveColor, negativeColor, accentColor) {
        when (config.displayMode) {
            CashFlowDisplayMode.TYPE_DISTINCTION -> buildList {
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
                                    flowDirection = FlowDirection.ANY,
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
                                    flowDirection = FlowDirection.ANY,
                                )
                            )
                        }
                    )
                )
                if (showTransfer) add(
                    TableColumn(
                        label = "TRANSFER", key = "tx", color = accentColor,
                        totalInBase = totalTxInBase - totalTxOutBase,
                        nativeOf = { txIn(it) - txOut(it) },
                        baseOf = { txInBase(it) - txOutBase(it) },
                        isSigned = true,
                        onCellClick = { ccy ->
                            callbacks.onClickTransfer?.invoke(
                                baseFilterForDrill.copy(
                                    transactionTypes = setOf(TransactionType.TRANSFER),
                                    currencies = setOf(ccy),
                                    flowDirection = FlowDirection.ANY,
                                )
                            )
                        }
                    )
                )
            }
            CashFlowDisplayMode.INFLOW_OUTFLOW -> {
                val inflowTypes = buildSet {
                    if (showIncome) add(TransactionType.INCOME)
                    if (showTransfer) add(TransactionType.TRANSFER)
                }
                val outflowTypes = buildSet {
                    if (showExpense) add(TransactionType.EXPENSE)
                    if (showTransfer) add(TransactionType.TRANSFER)
                }
                listOf(
                    TableColumn(
                        label = "INFLOW", key = "in", color = positiveColor,
                        totalInBase = (if (showIncome) result.totalIncomeInBase else BigDecimal.ZERO) +
                                (if (showTransfer) totalTxInBase else BigDecimal.ZERO),
                        nativeOf = {
                            (if (showIncome) it.income else BigDecimal.ZERO) +
                                    (if (showTransfer) txIn(it) else BigDecimal.ZERO)
                        },
                        baseOf = {
                            (if (showIncome) it.incomeInBase else BigDecimal.ZERO) +
                                    (if (showTransfer) txInBase(it) else BigDecimal.ZERO)
                        },
                        onCellClick = { ccy ->
                            callbacks.onClickInflow?.invoke(
                                baseFilterForDrill.copy(
                                    transactionTypes = inflowTypes,
                                    currencies = setOf(ccy),
                                    flowDirection = FlowDirection.IN,
                                )
                            )
                        }
                    ),
                    TableColumn(
                        label = "OUTFLOW", key = "out", color = negativeColor,
                        totalInBase = (if (showExpense) result.totalExpensesInBase else BigDecimal.ZERO) +
                                (if (showTransfer) totalTxOutBase else BigDecimal.ZERO),
                        nativeOf = {
                            (if (showExpense) it.expense else BigDecimal.ZERO) +
                                    (if (showTransfer) txOut(it) else BigDecimal.ZERO)
                        },
                        baseOf = {
                            (if (showExpense) it.expenseInBase else BigDecimal.ZERO) +
                                    (if (showTransfer) txOutBase(it) else BigDecimal.ZERO)
                        },
                        onCellClick = { ccy ->
                            callbacks.onClickOutflow?.invoke(
                                baseFilterForDrill.copy(
                                    transactionTypes = outflowTypes,
                                    currencies = setOf(ccy),
                                    flowDirection = FlowDirection.OUT,
                                )
                            )
                        }
                    ),
                )
            }
        }
    }

    val netColumn: TableColumn? = if (config.showNetColumn) {
        val netTotalInBase = columns.fold(BigDecimal.ZERO) { acc, c ->
            if (c.key == "exp" || c.key == "out") acc - c.totalInBase else acc + c.totalInBase
        }
        TableColumn(
            label = "NET", key = "net", color = accentColor,
            totalInBase = netTotalInBase,
            nativeOf = { item ->
                columns.fold(BigDecimal.ZERO) { acc, c ->
                    val v = c.nativeOf(item)
                    if (c.key == "exp" || c.key == "out") acc - v else acc + v
                }
            },
            baseOf = { item ->
                columns.fold(BigDecimal.ZERO) { acc, c ->
                    val v = c.baseOf(item)
                    if (c.key == "exp" || c.key == "out") acc - v else acc + v
                }
            },
            isSigned = true,
            onCellClick = null,
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
    val sign = if (signed && amountInBase.signum() > 0) "+" else ""
    val text = if (amountInBase.signum() == 0 && signed) "—" else sign + formatAuto(amountInBase, baseCurrency)
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
    val sign = if (signed && native.signum() > 0) "+" else ""
    val nativeText = if (isZero && signed) "—" else sign + formatAuto(native, nativeCcy)
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
            val baseSign = if (signed && base.signum() > 0) "+" else ""
            Text(
                text = "≈ $baseSign${formatAuto(base, baseCcy)}",
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
        }
        CardSettingsSection("Layout") {
            SingleChoiceSegmentedButtonRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
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
            EntrySwitchRow(
                label = "Show NET column",
                checked = config.showNetColumn,
                onCheckedChange = {
                    callbacks.onShowNetColumnChange(config.cardId, it)
                },
                isFlush = true,
            )
            EntrySwitchRow(
                label = "Exclude same-currency transfers",
                subtitle = "Only used in Inflow / Outflow mode",
                checked = config.excludeSameCurrencyTransfers,
                enabled = config.displayMode == CashFlowDisplayMode.INFLOW_OUTFLOW,
                onCheckedChange = {
                    callbacks.onExcludeSameCurrencyTransfersChange(config.cardId, it)
                },
                isFlush = true,
            )
        }
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
            "USD",
            BigDecimal("5000.00"),
            BigDecimal("5000.00"),
            BigDecimal("3500.00"),
            BigDecimal("3500.00"),
            BigDecimal.ZERO,
            BigDecimal.ZERO
        ),
        CashFlowBreakdownItem(
            "EUR",
            BigDecimal("1000.00"),
            BigDecimal("1100.00"),
            BigDecimal("800.00"),
            BigDecimal("880.00"),
            BigDecimal.ZERO,
            BigDecimal.ZERO
        )
    )
    val result = CashFlowResult(
        totalIncomeInBase = BigDecimal("6100.00"),
        totalExpensesInBase = BigDecimal("4380.00"),
        totalTransfersInBase = BigDecimal.ZERO,
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
