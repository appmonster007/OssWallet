package com.osscabinet.ossWallet.ui.screens.dashboard.components

import androidx.compose.foundation.BorderStroke
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
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.rounded.AccountBalance
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.osscabinet.ossWallet.domain.card.CardFilterRules
import com.osscabinet.ossWallet.domain.card.NetWorthMetrics
import com.osscabinet.ossWallet.domain.model.Account
import com.osscabinet.ossWallet.domain.model.Category
import com.osscabinet.ossWallet.domain.model.Filter
import com.osscabinet.ossWallet.domain.model.TransactionFilter
import com.osscabinet.ossWallet.domain.model.TransactionType
import com.osscabinet.ossWallet.domain.model.timeline.ThisPeriodType
import com.osscabinet.ossWallet.domain.model.timeline.TimelineRange
import com.osscabinet.ossWallet.ui.components.cards.AeonCardFilterButton
import com.osscabinet.ossWallet.ui.components.cards.AeonCardSettingsSheet
import com.osscabinet.ossWallet.ui.components.cards.AeonExpressiveCard
import com.osscabinet.ossWallet.ui.theme.NegativeAmountColor
import com.osscabinet.ossWallet.ui.theme.OssWalletTheme
import com.osscabinet.ossWallet.ui.theme.PositiveAmountColor
import com.osscabinet.ossWallet.domain.service.currency.CurrencyFormatter.formatCurrencyRight
import java.math.BigDecimal
import java.math.RoundingMode
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

@Composable
fun NetWorthCard(
    currentMetrics: NetWorthMetrics,
    baseCurrency: String,
    precision: Int,
    cardId: String,
    fontSize: String,
    showComparison: Boolean,
    filter: TransactionFilter,
    onFontSizeChange: (String, String) -> Unit,
    onShowComparisonChange: (String, Boolean) -> Unit,
    onFilterChange: (String, TransactionFilter) -> Unit,
    onDelete: () -> Unit = {},
    onClickIncome: (TransactionFilter) -> Unit = {},
    onClickExpense: (TransactionFilter) -> Unit = {},
    modifier: Modifier = Modifier,
    @Suppress("UNUSED_PARAMETER") headerModifier: Modifier = Modifier,
    filterRules: CardFilterRules = CardFilterRules.DEFAULT,
    baseFilter: TransactionFilter = TransactionFilter(),
    accounts: List<Account> = emptyList(),
    allCategories: List<Category> = emptyList(),
    savedFilters: List<Filter> = emptyList(),
    @Suppress("UNUSED_PARAMETER") currentTheme: String = "System",
    previousMetrics: NetWorthMetrics? = null
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

    AeonExpressiveCard(
        title = "Net Worth",
        subtitle = timelineLabel,
        icon = Icons.Rounded.AccountBalance,
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
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Primary Figure: Manrope, Bold, 20px (or larger as per font setting)
            val baseFontSize = when (fontSize) {
                "Small" -> 18.sp
                "Large" -> 24.sp
                "X-Large" -> 32.sp
                else -> 20.sp
            }
            
            Column {
                Text(
                    text = formatCurrencyRight(currentMetrics.netWorth, baseCurrency, precision, shorten = true),
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontWeight = FontWeight.Bold,
                        fontSize = baseFontSize,
                        letterSpacing = (-0.5).sp
                    ),
                    color = MaterialTheme.colorScheme.onSurface
                )
                
                if (showComparison && previousMetrics != null) {
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
                                tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                            )
                            Text(
                                text = "${if (change.signum() > 0) "+" else ""}$percentage% from prev. period",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontWeight = FontWeight.Medium,
                                    fontSize = 11.sp
                                ),
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                            )
                        }
                    }
                }
            }

            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                FlowBadge(
                    label = "INCOME",
                    amount = currentMetrics.cashFlow.totalIncomeInBase,
                    currency = baseCurrency,
                    precision = precision,
                    color = PositiveAmountColor,
                    isIncome = true,
                    modifier = Modifier.weight(1f),
                    onClick = { onClickIncome(filter.copy(transactionType = TransactionType.INCOME)) },
                    previousAmount = if (showComparison) previousMetrics?.cashFlow?.totalIncomeInBase else null
                )
                FlowBadge(
                    label = "EXPENSE",
                    amount = currentMetrics.cashFlow.totalExpensesInBase,
                    currency = baseCurrency,
                    precision = precision,
                    color = NegativeAmountColor,
                    isIncome = false,
                    modifier = Modifier.weight(1f),
                    onClick = { onClickExpense(filter.copy(transactionType = TransactionType.EXPENSE)) },
                    previousAmount = if (showComparison) previousMetrics?.cashFlow?.totalExpensesInBase else null
                )
            }
        }
    }

    if (showDisplaySettings) {
        NetWorthDisplaySettingsDialog(
            fontSize = fontSize,
            showComparison = showComparison,
            cardId = cardId,
            onFontSizeChange = onFontSizeChange,
            onShowComparisonChange = onShowComparisonChange,
            onDismiss = { showDisplaySettings = false },
            onDelete = onDelete
        )
    }

    if (showFilterSettings) {
        AeonCardSettingsSheet(
            title = "Net Worth Filter",
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
private fun FlowBadge(
    label: String,
    amount: BigDecimal,
    currency: String,
    precision: Int,
    color: Color,
    isIncome: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit = {},
    previousAmount: BigDecimal? = null
) {
    Surface(
        modifier = modifier.clickable(onClick = onClick),
        color = color.copy(alpha = 0.08f),
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(0.5.dp, color.copy(alpha = 0.1f))
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
//                Icon(
//                    if (isIncome) Icons.AutoMirrored.Filled.TrendingUp else Icons.AutoMirrored.Filled.TrendingDown,
//                    contentDescription = null,
//                    modifier = Modifier.size(14.dp),
//                    tint = color
//                )
                    Text(
                        text = label,
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 10.sp,
                            letterSpacing = 0.5.sp
                        ),
                        color = color.copy(alpha = 0.8f)
                    )
                }
                if (previousAmount != null) {
                    val change = amount.subtract(previousAmount)
                    if (change.signum() != 0 && previousAmount.signum() != 0) {
                        val percentage = change.multiply(BigDecimal("100"))
                            .divide(previousAmount.abs(), 1, RoundingMode.HALF_UP)

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(
                                if (change.signum() > 0) Icons.AutoMirrored.Filled.TrendingUp else Icons.AutoMirrored.Filled.TrendingDown,
                                contentDescription = null,
                                modifier = Modifier.size(10.dp),
                                tint = color.copy(alpha = 0.7f)
                            )
                            Text(
                                text = "${if (change.signum() > 0) "+" else ""}$percentage%",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontWeight = FontWeight.Medium,
                                    fontSize = 10.sp
                                ),
                                color = color.copy(alpha = 0.7f)
                            )
                        }
                    }
                }
            }
            Text(
                text = formatCurrencyRight(amount, currency, precision, shorten = true),
                style = MaterialTheme.typography.bodyMedium.copy(
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp
                ),
                color = color
            )
        }
    }
}

@Composable
private fun NetWorthDisplaySettingsDialog(
    fontSize: String,
    showComparison: Boolean,
    cardId: String,
    onFontSizeChange: (String, String) -> Unit,
    onShowComparisonChange: (String, Boolean) -> Unit,
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
                    contentColor = MaterialTheme.colorScheme.error
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
                "Net Worth Display",
                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                // Font Size Selection
                Surface(
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                    shape = RoundedCornerShape(20.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text(
                            "FONT SIZE",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 1.sp
                            ),
                            color = MaterialTheme.colorScheme.primary
                        )

                        SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                            listOf("Small", "Medium", "Large", "X-Large").forEachIndexed { index, size ->
                                SegmentedButton(
                                    selected = fontSize == size,
                                    onClick = { onFontSizeChange(cardId, size) },
                                    shape = SegmentedButtonDefaults.itemShape(
                                        index = index,
                                        count = 4
                                    ),
                                    label = { 
                                        Text(
                                            when(size) {
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
                }

                // Show Comparison Toggle
                Surface(
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                    shape = RoundedCornerShape(20.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                "SHOW COMPARISON",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontWeight = FontWeight.Bold,
                                    letterSpacing = 1.sp
                                ),
                                color = MaterialTheme.colorScheme.primary
                            )
                            Text(
                                "Show change from previous period",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Switch(
                            checked = showComparison,
                            onCheckedChange = { onShowComparisonChange(cardId, it) }
                        )
                    }
                }
            }
        },
        shape = RoundedCornerShape(32.dp),
        containerColor = MaterialTheme.colorScheme.surfaceContainer
    )
}

@Preview(showBackground = true, name = "Net Worth Card - Normal")
@Composable
fun NetWorthCardPreview() {
    OssWalletTheme {
        Box(modifier = Modifier.padding(16.dp)) {
            NetWorthCard(
                currentMetrics = NetWorthMetrics(
                    netWorth = BigDecimal("125000.50"),
                    cashFlow = com.osscabinet.ossWallet.domain.model.analytics.CashFlowResult.empty().copy(
                        totalIncomeInBase = BigDecimal("5000.00"),
                        totalExpensesInBase = BigDecimal("3200.00")
                    )
                ),
                baseCurrency = "USD",
                precision = 2,
                cardId = "preview",
                fontSize = "Medium",
                showComparison = true,
                filter = TransactionFilter(),
                onFontSizeChange = { _, _ -> },
                onShowComparisonChange = { _, _ -> },
                onFilterChange = { _, _ -> }
            )
        }
    }
}

@Preview(showBackground = true, name = "Net Worth Card - With Comparison")
@Composable
fun NetWorthCardComparisonPreview() {
    OssWalletTheme {
        Box(modifier = Modifier.padding(16.dp)) {
            NetWorthCard(
                currentMetrics = NetWorthMetrics(
                    netWorth = BigDecimal("125000.50"),
                    cashFlow = com.osscabinet.ossWallet.domain.model.analytics.CashFlowResult.empty().copy(
                        totalIncomeInBase = BigDecimal("5500.00"),
                        totalExpensesInBase = BigDecimal("2800.00")
                    )
                ),
                baseCurrency = "USD",
                precision = 2,
                cardId = "preview_comparison",
                fontSize = "Medium",
                showComparison = true,
                filter = TransactionFilter(timelineRange = TimelineRange.ThisPeriod(ThisPeriodType.MONTH)),
                onFontSizeChange = { _, _ -> },
                onShowComparisonChange = { _, _ -> },
                onFilterChange = { _, _ -> },
                previousMetrics = NetWorthMetrics(
                    netWorth = BigDecimal("122300.50"),
                    cashFlow = com.osscabinet.ossWallet.domain.model.analytics.CashFlowResult.empty().copy(
                        totalIncomeInBase = BigDecimal("4800.00"),
                        totalExpensesInBase = BigDecimal("3000.00")
                    )
                )
            )
        }
    }
}
