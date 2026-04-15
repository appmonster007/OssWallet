package com.osscabinet.ossWallet.ui.components.dashboard

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.material.icons.rounded.ArrowDownward
import androidx.compose.material.icons.rounded.ArrowUpward
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.osscabinet.ossWallet.domain.model.CashFlowType
import com.osscabinet.ossWallet.domain.model.Transaction
import com.osscabinet.ossWallet.ui.components.AeonBaseConversionToggle
import com.osscabinet.ossWallet.ui.components.AeonFlowBadge
import com.osscabinet.ossWallet.ui.components.AeonFlowSectionHeader
import com.osscabinet.ossWallet.ui.util.convertAmount
import com.osscabinet.ossWallet.ui.util.formatCurrencyRight
import kotlinx.serialization.Serializable
import java.math.BigDecimal
import java.math.RoundingMode

@Serializable
data class CashFlowSettings(
    val showBaseConversion: Boolean = true
)

@Composable
fun AeonCashFlowSummary(
    transactions: List<Transaction>,
    baseCurrency: String,
    exchangeRates: Map<String, BigDecimal>,
    currentTheme: String,
    modifier: Modifier = Modifier,
    headerModifier: Modifier = Modifier,
    showTitle: Boolean = true,
    precision: Int = 2,
    showBaseConversion: Boolean = true,
    onShowBaseConversionChange: (Boolean) -> Unit = {}
) {
    var showSettings by remember { mutableStateOf(false) }

    val allSupportedCurrencies = remember(transactions) {
        transactions.map { it.currency }.distinct().sorted()
    }

    val flowData by remember(
        allSupportedCurrencies,
        transactions,
        exchangeRates,
        baseCurrency,
        precision
    ) {
        derivedStateOf {
            allSupportedCurrencies.sorted().mapNotNull { curr ->
                val txsForCurr = transactions.filter { it.currency == curr }
                val income = txsForCurr.filter { it.isIncome }
                    .fold(BigDecimal.ZERO) { acc, t -> acc.add(t.amount) }
                val expense = txsForCurr.filter { !it.isIncome }
                    .fold(BigDecimal.ZERO) { acc, t -> acc.add(t.amount) }

                if (income.signum() == 0 && expense.signum() == 0) null
                else {
                    val incomeInBase = convertAmount(income, curr, baseCurrency, exchangeRates, precision + 4)
                    val expenseInBase = convertAmount(expense, curr, baseCurrency, exchangeRates, precision + 4)
                    Triple(curr, Pair(income, incomeInBase), Pair(expense, expenseInBase))
                }
            }
        }
    }

    val totalIncomeInBase =
        flowData.fold(BigDecimal.ZERO) { acc, data -> acc.add(data.second.second) }.setScale(precision, RoundingMode.HALF_UP)
    val totalExpenseInBase =
        flowData.fold(BigDecimal.ZERO) { acc, data -> acc.add(data.third.second) }.setScale(precision, RoundingMode.HALF_UP)

    Card(
        modifier = modifier,
        shape = RoundedCornerShape(24.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            if (showTitle) {
                AeonFlowSectionHeader(
                    title = "CASH FLOW SUMMARY",
                    modifier = headerModifier,
                    trailing = {
                        IconButton(
                            onClick = { showSettings = true },
                            modifier = Modifier.size(24.dp)
                        ) {
                            Icon(
                                Icons.Default.MoreHoriz,
                                contentDescription = "Settings",
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                )
            }

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
                    precision = precision,
                    modifier = Modifier.weight(1f)
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        flowData.forEach { (curr, income, _) ->
                            AeonFlowBreakdownItem(
                                curr = curr,
                                amount = income.first,
                                inBase = income.second,
                                baseCurrency = baseCurrency,
                                precision = precision,
                                showBaseConversion = showBaseConversion
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
                    precision = precision,
                    modifier = Modifier.weight(1f)
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        flowData.forEach { (curr, _, expense) ->
                            AeonFlowBreakdownItem(
                                curr = curr,
                                amount = expense.first,
                                inBase = expense.second,
                                baseCurrency = baseCurrency,
                                precision = precision,
                                showBaseConversion = showBaseConversion
                            )
                        }
                    }
                }
            }
        }
    }

    if (showSettings) {
        AlertDialog(
            onDismissRequest = { showSettings = false },
            confirmButton = {
                TextButton(onClick = { showSettings = false }) {
                    Text("Done")
                }
            },
            title = {
                Text(
                    "Cash Flow Settings",
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    AeonBaseConversionToggle(
                        baseCurrency = baseCurrency,
                        showBaseConversion = showBaseConversion,
                        onShowBaseConversionChange = onShowBaseConversionChange
                    )
                }
            },
            shape = RoundedCornerShape(32.dp),
            containerColor = MaterialTheme.colorScheme.surface
        )
    }
}

@Composable
internal fun AeonFlowBreakdownItem(
    curr: String,
    amount: BigDecimal,
    inBase: BigDecimal,
    baseCurrency: String,
    precision: Int = 2,
    showBaseConversion: Boolean = true
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            curr,
            style = MaterialTheme.typography.labelSmall.copy(
                fontSize = 10.sp,
                fontWeight = FontWeight.Black
            ),
            color = MaterialTheme.colorScheme.outline
        )
        Column(horizontalAlignment = Alignment.End) {
            Text(
                formatCurrencyRight(amount, curr, precision),
                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Black)
            )
            if (showBaseConversion && curr != baseCurrency) {
                Text(
                    "≈ ${formatCurrencyRight(inBase, baseCurrency, precision)}",
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold
                    ),
                    color = MaterialTheme.colorScheme.outline
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
    currentTheme: String = "System",
    incomeColor: Color = Color(0xFF4CAF50),
    expenseColor: Color = Color(0xFFF44336),
    precision: Int = 2,
    content: @Composable ColumnScope.() -> Unit
) {
    val color = if (type == CashFlowType.INCOME) incomeColor else expenseColor
    val icon = if (type == CashFlowType.INCOME) Icons.Rounded.ArrowUpward else Icons.Rounded.ArrowDownward
    
    var cardSize by remember { mutableStateOf(IntSize.Zero) }
    val density = LocalDensity.current

    Surface(
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
        shape = RoundedCornerShape(20.dp),
        border = BorderStroke(1.dp, color.copy(alpha = 0.1f)),
        modifier = modifier.onSizeChanged { cardSize = it }
    ) {
        Box(modifier = Modifier.fillMaxWidth()) {
            val iconHeight = with(density) { (cardSize.height * 0.6f).toDp() }
            
            if (cardSize.height > 0) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .size(iconHeight)
                        .alpha(0.1f),
                    tint = color
                )
            }

            Column(
                modifier = Modifier.padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        label.uppercase(),
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.sp
                        ),
                        color = color
                    )
                    AeonFlowBadge(totalInBase, type, precision = precision)
                }

                content()
            }
        }
    }
}
