package com.osscabinet.ossWallet.ui.screens.dashboard.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.rounded.Analytics
import androidx.compose.material.icons.rounded.ArrowDownward
import androidx.compose.material.icons.rounded.ArrowUpward
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.osscabinet.ossWallet.domain.model.Account
import com.osscabinet.ossWallet.domain.model.CashFlowType
import com.osscabinet.ossWallet.domain.model.Category
import com.osscabinet.ossWallet.domain.model.Filter
import com.osscabinet.ossWallet.domain.model.TransactionFilter
import com.osscabinet.ossWallet.domain.model.TransactionType
import com.osscabinet.ossWallet.domain.model.timeline.TimelineRange
import com.osscabinet.ossWallet.domain.model.analytics.CashFlowBreakdownItem
import com.osscabinet.ossWallet.domain.model.analytics.CashFlowResult
import com.osscabinet.ossWallet.ui.components.*
import com.osscabinet.ossWallet.ui.components.cards.AeonCardSettingsSheet
import com.osscabinet.ossWallet.domain.service.currency.CurrencyFormatter.formatCurrencyRight
import java.math.BigDecimal
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

import com.osscabinet.ossWallet.domain.card.CardFilterRules

import com.osscabinet.ossWallet.ui.components.cards.AeonCardFilterButton
import com.osscabinet.ossWallet.ui.components.cards.AeonExpressiveCard

import com.osscabinet.ossWallet.ui.theme.AppColors
import com.osscabinet.ossWallet.ui.theme.NegativeAmountColor
import com.osscabinet.ossWallet.ui.theme.OssWalletTheme
import com.osscabinet.ossWallet.ui.theme.PositiveAmountColor
import com.osscabinet.ossWallet.ui.theme.AppTextStyles

@Composable
fun CashFlowCard(
    cashFlowResult: CashFlowResult?,
    baseCurrency: String,
    currentTheme: String,
    cardId: String,
    modifier: Modifier = Modifier,
    @Suppress("UNUSED_PARAMETER") headerModifier: Modifier = Modifier,
    @Suppress("UNUSED_PARAMETER") showTitle: Boolean = true,
    precision: Int = 2,
    showBaseConversion: Boolean = true,
    filter: TransactionFilter = TransactionFilter(),
    onShowBaseConversionChange: (String, Boolean) -> Unit = { _, _ -> },
    onFilterChange: (String, TransactionFilter) -> Unit = { _, _ -> },
    onDelete: () -> Unit = {},
    onClickIncome: (TransactionFilter) -> Unit = {},
    onClickExpense: (TransactionFilter) -> Unit = {},
    filterRules: CardFilterRules = CardFilterRules.DEFAULT,
    baseFilter: TransactionFilter = TransactionFilter(),
    accounts: List<Account> = emptyList(),
    allCategories: List<Category> = emptyList(),
    savedFilters: List<Filter> = emptyList()
) {
    var showDisplaySettings by remember { mutableStateOf(false) }
    var showFilterSettings by remember { mutableStateOf(false) }

    val timelineLabel = when (val tr = filter.timelineRange) {
        is TimelineRange.AllTime -> "All Time"
        is TimelineRange.ThisPeriod -> tr.period.label
        is TimelineRange.LastPeriod -> tr.period.label
        is TimelineRange.CustomRange -> {
            val formatter = DateTimeFormatter.ofPattern("MMM d").withZone(ZoneId.systemDefault())
            val start = tr.start?.let { formatter.format(Instant.ofEpochMilli(it)) } ?: "..."
            val end = tr.end?.let { formatter.format(Instant.ofEpochMilli(it)) } ?: "..."
            "$start - $end"
        }
    }

    val totalIncomeInBase = cashFlowResult?.totalIncomeInBase ?: BigDecimal.ZERO
    val totalExpenseInBase = cashFlowResult?.totalExpensesInBase ?: BigDecimal.ZERO

    AeonExpressiveCard(
        title = "Cash Flow",
        subtitle = timelineLabel,
        icon = Icons.Rounded.Analytics,
        modifier = modifier,
        headerModifier = headerModifier,
        filter = filter,
        baseFilter = baseFilter,
        filterRules = filterRules,
        headerTrailing = { activeCount ->
            AeonCardFilterButton(
                onClick = { showFilterSettings = true },
                activeFilterCount = activeCount
            )
            IconButton(
                onClick = { showDisplaySettings = true },
                modifier = Modifier.size(32.dp)
            ) {
                Icon(
                    Icons.Rounded.Tune,
                    contentDescription = "Display Settings",
                    tint = AppColors.primary,
                    modifier = Modifier.size(20.dp)
                )
            }
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
                currentTheme = currentTheme,
                incomeColor = PositiveAmountColor,
                precision = precision,
                modifier = Modifier.weight(1f),
                onClick = { onClickIncome(filter.copy(transactionTypes = setOf(TransactionType.INCOME))) }
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    cashFlowResult?.breakdown?.forEach { item ->
                        AeonFlowBreakdownItem(
                            curr = item.currency,
                            amount = item.income,
                            inBase = item.incomeInBase,
                            baseCurrency = baseCurrency,
                            precision = precision,
                            showBaseConversion = showBaseConversion,
                            onClick = {
                                onClickIncome(
                                    filter.copy(
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
                currentTheme = currentTheme,
                expenseColor = NegativeAmountColor,
                precision = precision,
                modifier = Modifier.weight(1f),
                onClick = { onClickExpense(filter.copy(transactionTypes = setOf(TransactionType.EXPENSE))) }
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    cashFlowResult?.breakdown?.forEach { item ->
                        AeonFlowBreakdownItem(
                            curr = item.currency,
                            amount = item.expense,
                            inBase = item.expenseInBase,
                            baseCurrency = baseCurrency,
                            precision = precision,
                            showBaseConversion = showBaseConversion,
                            onClick = {
                                onClickExpense(
                                    filter.copy(
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

    if (showDisplaySettings) {
        CashFlowDisplaySettingsDialog(
            showBaseConversion = showBaseConversion,
            baseCurrency = baseCurrency,
            cardId = cardId,
            onShowBaseConversionChange = onShowBaseConversionChange,
            onDismiss = { showDisplaySettings = false },
            onDelete = onDelete
        )
    }

    if (showFilterSettings) {
        AeonCardSettingsSheet(
            title = "Cash Flow Filter",
            filter = filter,
            onFilterChange = { newFilter -> onFilterChange(cardId, newFilter) },
            onDismiss = { showFilterSettings = false },
            onDelete = onDelete,
            filterRules = filterRules,
            baseFilter = baseFilter,
            accounts = accounts,
            allCategories = allCategories,
            savedFilters = savedFilters
        )
    }
}

@Composable
private fun CashFlowDisplaySettingsDialog(
    showBaseConversion: Boolean,
    baseCurrency: String,
    cardId: String,
    onShowBaseConversionChange: (String, Boolean) -> Unit,
    onDismiss: () -> Unit,
    onDelete: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        dismissButton = {
            TextButton(
                onClick = {
                    onDelete()
                    onDismiss()
                },
                colors = ButtonDefaults.textButtonColors(
                    contentColor = AppColors.error
                )
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
            TextButton(onClick = onDismiss) {
                Text("Done")
            }
        },
        title = {
            Text(
                "Cash Flow Display",
                style = AppTextStyles.cardTitle
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                AeonBaseConversionToggle(
                    baseCurrency = baseCurrency,
                    showBaseConversion = showBaseConversion,
                    onShowBaseConversionChange = { show -> onShowBaseConversionChange(cardId, show) }
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
    precision: Int = 2,
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
            color = AppColors.contentSecondary // Level 3 token
        )
        Column(horizontalAlignment = Alignment.End) {
            Text(
                formatCurrencyRight(amount, curr, precision, shorten = true),
                style = AppTextStyles.transactionTitle.copy(
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp
                )
            )
            if (showBaseConversion && curr != baseCurrency) {
                Text(
                    "≈ ${formatCurrencyRight(inBase, baseCurrency, precision, shorten = true)}",
                    style = AppTextStyles.transactionSwipeAction.copy(
                        fontSize = 12.sp,
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
    precision: Int = 2,
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
                        fontSize = 12.sp
                    ),
                    color = color
                )
                AeonFlowBadge(totalInBase, type, baseCurrency, precision = precision, incomeColor = color, expenseColor = color)
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
                currentTheme = "System",
                cardId = "preview"
            )
        }
    }
}
