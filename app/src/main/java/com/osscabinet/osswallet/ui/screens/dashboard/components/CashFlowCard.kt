package com.osscabinet.ossWallet.ui.screens.dashboard.components

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
import androidx.compose.material.icons.rounded.Analytics
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.osscabinet.ossWallet.domain.card.CardFilterRules
import com.osscabinet.ossWallet.domain.model.TransactionFilter
import com.osscabinet.ossWallet.domain.model.TransactionType
import com.osscabinet.ossWallet.domain.model.analytics.CashFlowBreakdownItem
import com.osscabinet.ossWallet.domain.model.analytics.CashFlowResult
import com.osscabinet.ossWallet.domain.model.settings.CashFlowCardConfig
import com.osscabinet.ossWallet.domain.service.currency.CurrencyFormatter.formatAuto
import com.osscabinet.ossWallet.ui.components.AeonBaseConversionToggle
import com.osscabinet.ossWallet.ui.components.aeonDashedTopRule
import com.osscabinet.ossWallet.ui.components.cards.AeonExpressiveCard
import com.osscabinet.ossWallet.ui.components.cards.CardCallbacks
import com.osscabinet.ossWallet.ui.components.cards.CardSettingsSection
import com.osscabinet.ossWallet.ui.components.cards.CardSettingsSheet
import com.osscabinet.ossWallet.ui.components.cards.FilterSelectionContext
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
        val breakdown = cashFlowResult?.breakdown.orEmpty()
        val net = totalIncomeInBase.subtract(totalExpenseInBase)

        Column(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(IntrinsicSize.Min),
                horizontalArrangement = Arrangement.spacedBy(20.dp)
            ) {
                CashFlowColumn(
                    label = "INCOME",
                    total = totalIncomeInBase,
                    baseCurrency = baseCurrency,
                    color = AppColors.positive,
                    breakdown = breakdown,
                    amountOf = { it.income },
                    inBaseOf = { it.incomeInBase },
                    showBaseConversion = config.showBaseConversion,
                    onHeaderClick = {
                        callbacks.onClickIncome?.invoke(
                            config.filter.copy(transactionTypes = setOf(TransactionType.INCOME))
                        )
                    },
                    onRowClick = { cur ->
                        callbacks.onClickIncome?.invoke(
                            config.filter.copy(
                                transactionTypes = setOf(TransactionType.INCOME),
                                currencies = setOf(cur)
                            )
                        )
                    },
                    modifier = Modifier.weight(1f)
                )
                Box(
                    modifier = Modifier
                        .width(1.dp)
                        .fillMaxHeight()
                        .background(AppColors.outlineVariant)
                )
                CashFlowColumn(
                    label = "EXPENSE",
                    total = totalExpenseInBase,
                    baseCurrency = baseCurrency,
                    color = AppColors.negative,
                    breakdown = breakdown,
                    amountOf = { it.expense },
                    inBaseOf = { it.expenseInBase },
                    showBaseConversion = config.showBaseConversion,
                    onHeaderClick = {
                        callbacks.onClickExpense?.invoke(
                            config.filter.copy(transactionTypes = setOf(TransactionType.EXPENSE))
                        )
                    },
                    onRowClick = { cur ->
                        callbacks.onClickExpense?.invoke(
                            config.filter.copy(
                                transactionTypes = setOf(TransactionType.EXPENSE),
                                currencies = setOf(cur)
                            )
                        )
                    },
                    modifier = Modifier.weight(1f)
                )
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 16.dp)
                    .aeonDashedTopRule()
                    .padding(top = 14.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Bottom
            ) {
                Text(
                    text = "NET",
                    style = AppTextStyles.sectionOverline,
                    color = AppColors.contentSecondary.copy(alpha = 0.7f)
                )
                Text(
                    text = (if (net.signum() >= 0) "+" else "") + formatAuto(net, baseCurrency),
                    style = AppTextStyles.transactionAmount.copy(
                        fontWeight = FontWeight.Medium,
                        fontSize = 17.sp
                    ),
                    color = AppColors.accent
                )
            }
        }
    }
}

@Composable
private fun CashFlowColumn(
    label: String,
    total: BigDecimal,
    baseCurrency: String,
    color: Color,
    breakdown: List<CashFlowBreakdownItem>,
    amountOf: (CashFlowBreakdownItem) -> BigDecimal,
    inBaseOf: (CashFlowBreakdownItem) -> BigDecimal,
    showBaseConversion: Boolean,
    onHeaderClick: () -> Unit,
    onRowClick: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onHeaderClick() },
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Bottom
        ) {
            Text(
                text = label,
                style = AppTextStyles.sectionOverline,
                color = AppColors.contentSecondary.copy(alpha = 0.7f)
            )
            Text(
                text = "Σ ${formatAuto(total, baseCurrency)}",
                style = AppTextStyles.metaLabel.copy(fontWeight = FontWeight.Medium),
                color = AppColors.contentSecondary
            )
        }
        breakdown.forEach { item ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 13.dp)
                    .clickable { onRowClick(item.currency) },
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Text(
                    text = item.currency,
                    style = AppTextStyles.metaLabel,
                    color = AppColors.contentSecondary.copy(alpha = 0.7f)
                )
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = formatAuto(amountOf(item), item.currency),
                        style = AppTextStyles.transactionAmount.copy(
                            fontWeight = FontWeight.Normal,
                            fontSize = 19.sp
                        ),
                        color = color
                    )
                    if (showBaseConversion && item.currency != baseCurrency) {
                        Text(
                            text = "≈ ${formatAuto(inBaseOf(item), baseCurrency)}",
                            style = AppTextStyles.metaLabel.copy(fontWeight = FontWeight.Medium),
                            color = AppColors.contentSecondary.copy(alpha = 0.6f)
                        )
                    }
                }
            }
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
                    callbacks.onShowBaseConversionChange(
                        config.cardId,
                        it
                    )
                }
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
