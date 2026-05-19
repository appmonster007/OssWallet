package com.osscabinet.ossWallet.ui.screens.dashboard.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.rounded.Analytics
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.osscabinet.ossWallet.domain.model.CashFlowType
import com.osscabinet.ossWallet.domain.model.TransactionFilter
import com.osscabinet.ossWallet.domain.model.TransactionType
import com.osscabinet.ossWallet.domain.model.analytics.CashFlowBreakdownItem
import com.osscabinet.ossWallet.domain.model.analytics.CashFlowResult
import com.osscabinet.ossWallet.domain.model.settings.CashFlowCardConfig
import com.osscabinet.ossWallet.ui.components.*
import com.osscabinet.ossWallet.ui.components.cards.AeonExpressiveCard
import com.osscabinet.ossWallet.ui.components.cards.CardCallbacks
import com.osscabinet.ossWallet.ui.components.cards.FilterSelectionContext
import com.osscabinet.ossWallet.domain.service.currency.CurrencyFormatter.formatCurrencyRight
import java.math.BigDecimal
import com.osscabinet.ossWallet.ui.theme.AppColors
import com.osscabinet.ossWallet.ui.theme.NegativeAmountColor
import com.osscabinet.ossWallet.ui.theme.OssWalletTheme
import com.osscabinet.ossWallet.ui.theme.PositiveAmountColor
import com.osscabinet.ossWallet.ui.theme.AppTextStyles

@Composable
fun CashFlowCard(
    cashFlowResult: CashFlowResult?,
    baseCurrency: String,
    config: CashFlowCardConfig,
    callbacks: CardCallbacks.CashFlow = CardCallbacks.CashFlow(),
    filterContext: FilterSelectionContext = FilterSelectionContext(),
    baseFilter: TransactionFilter = TransactionFilter(),
    inheritedFilter: TransactionFilter = TransactionFilter(),
    screenOwnsTimeline: Boolean = false,
    showFilterButton: Boolean = true,
    modifier: Modifier = Modifier,
    headerModifier: Modifier = Modifier,
) {
    val totalIncomeInBase = cashFlowResult?.totalIncomeInBase ?: BigDecimal.ZERO
    val totalExpenseInBase = cashFlowResult?.totalExpensesInBase ?: BigDecimal.ZERO

    val displayFilter = if (screenOwnsTimeline || inheritedFilter.timelineRange !is com.osscabinet.ossWallet.domain.model.timeline.TimelineRange.AllTime)
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
        filterRules = config.filterRules,
        filterContext = filterContext,
        onFilterChange = { callbacks.onFilterChange(config.cardId, it) },
        onSaveFilterClick = callbacks.onSaveFilterClick,
        displaySettings = { onDismiss ->
            CashFlowDisplaySettingsDialog(config = config, baseCurrency = baseCurrency, callbacks = callbacks, onDismiss = onDismiss)
        }
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            AeonFlowCard(
                label = "Income",
                totalInBase = totalIncomeInBase,
                baseCurrency = baseCurrency,
                type = CashFlowType.INCOME,
                incomeColor = PositiveAmountColor,
                modifier = Modifier.weight(1f),
                onClick = { callbacks.onClickIncome?.invoke(config.filter.copy(transactionTypes = setOf(TransactionType.INCOME))) }
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    cashFlowResult?.breakdown?.forEach { item ->
                        AeonFlowBreakdownItem(
                            curr = item.currency,
                            amount = item.income,
                            inBase = item.incomeInBase,
                            baseCurrency = baseCurrency,
                            showBaseConversion = config.showBaseConversion,
                            onClick = {
                                callbacks.onClickIncome?.invoke(
                                    config.filter.copy(
                                        transactionTypes = setOf(TransactionType.INCOME),
                                        currencies = setOf(item.currency)
                                    )
                                )
                            }
                        )
                    }
                }
            }

            AeonFlowCard(
                label = "Expense",
                totalInBase = totalExpenseInBase,
                baseCurrency = baseCurrency,
                type = CashFlowType.EXPENSE,
                expenseColor = NegativeAmountColor,
                modifier = Modifier.weight(1f),
                onClick = { callbacks.onClickExpense?.invoke(config.filter.copy(transactionTypes = setOf(TransactionType.EXPENSE))) }
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    cashFlowResult?.breakdown?.forEach { item ->
                        AeonFlowBreakdownItem(
                            curr = item.currency,
                            amount = item.expense,
                            inBase = item.expenseInBase,
                            baseCurrency = baseCurrency,
                            showBaseConversion = config.showBaseConversion,
                            onClick = {
                                callbacks.onClickExpense?.invoke(
                                    config.filter.copy(
                                        transactionTypes = setOf(TransactionType.EXPENSE),
                                        currencies = setOf(item.currency)
                                    )
                                )
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun CashFlowDisplaySettingsDialog(
    config: CashFlowCardConfig,
    baseCurrency: String,
    callbacks: CardCallbacks.CashFlow,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        dismissButton = {
            TextButton(
                onClick = {
                    callbacks.onDelete()
                    onDismiss()
                },
                colors = ButtonDefaults.textButtonColors(contentColor = AppColors.error)
            ) {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = "Delete card",
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text("Delete")
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("Done") }
        },
        title = {
            Text("Cash Flow Display", style = AppTextStyles.cardTitle)
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                AeonBaseConversionToggle(
                    baseCurrency = baseCurrency,
                    showBaseConversion = config.showBaseConversion,
                    onShowBaseConversionChange = { show -> callbacks.onShowBaseConversionChange(config.cardId, show) }
                )
            }
        },
        shape = RoundedCornerShape(32.dp),
        containerColor = AppColors.sheetSurface
    )
}

@Composable
internal fun AeonFlowBreakdownItem(
    curr: String,
    amount: BigDecimal,
    inBase: BigDecimal,
    baseCurrency: String,
    showBaseConversion: Boolean = true,
    onClick: () -> Unit = {}
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            curr,
            style = AppTextStyles.transactionSwipeAction.copy(
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold
            ),
            color = AppColors.contentSecondary
        )
        Column(horizontalAlignment = Alignment.End) {
            Text(
                formatCurrencyRight(amount, curr, shorten = true),
                style = AppTextStyles.transactionTitle.copy(
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp
                )
            )
            if (showBaseConversion && curr != baseCurrency) {
                Text(
                    "≈ ${formatCurrencyRight(inBase, baseCurrency, shorten = true)}",
                    style = AppTextStyles.transactionSwipeAction.copy(
                        fontSize = 8.sp,
                        fontWeight = FontWeight.Medium
                    ),
                    color = AppColors.contentSecondary
                )
            }
        }
    }
}

@Composable
fun AeonFlowCard(
    label: String,
    totalInBase: BigDecimal,
    baseCurrency: String,
    type: CashFlowType,
    modifier: Modifier = Modifier,
    @Suppress("UNUSED_PARAMETER") currentTheme: String = "System",
    incomeColor: Color = PositiveAmountColor,
    expenseColor: Color = NegativeAmountColor,
    onClick: () -> Unit = {},
    content: @Composable ColumnScope.() -> Unit
) {
    val color = if (type == CashFlowType.INCOME) incomeColor else expenseColor

    Surface(
        color = AppColors.insetSurface,
        shape = RoundedCornerShape(24.dp),
        modifier = modifier.clickable { onClick() }
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    label.uppercase(),
                    style = AppTextStyles.transactionSwipeAction.copy(
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.5.sp,
                        fontSize = 10.sp
                    ),
                    color = color
                )
                AeonFlowBadge(totalInBase, type, baseCurrency, incomeColor = color, expenseColor = color)
            }

            content()
        }
    }
}

@androidx.compose.ui.tooling.preview.Preview(showBackground = true)
@Composable
fun CashFlowCardPreview() {
    val sampleBreakdown = listOf(
        CashFlowBreakdownItem("USD", BigDecimal("5000.00"), BigDecimal("5000.00"), BigDecimal("3500.00"), BigDecimal("3500.00"), BigDecimal.ZERO, BigDecimal.ZERO),
        CashFlowBreakdownItem("EUR", BigDecimal("1000.00"), BigDecimal("1100.00"), BigDecimal("800.00"), BigDecimal("880.00"), BigDecimal.ZERO, BigDecimal.ZERO)
    )
    val result = CashFlowResult(
        totalIncomeInBase = BigDecimal("6100.00"),
        totalExpensesInBase = BigDecimal("4380.00"),
        totalTransfersInBase = BigDecimal.ZERO,
        netInBase = BigDecimal("1720.00"),
        breakdown = sampleBreakdown,
        filteredTransactions = emptyList()
    )

    OssWalletTheme {
        Box(modifier = Modifier.padding(16.dp)) {
            CashFlowCard(
                cashFlowResult = result,
                baseCurrency = "USD",
                config = CashFlowCardConfig(cardId = "preview")
            )
        }
    }
}
