package com.osscabinet.ossWallet.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
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
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.osscabinet.ossWallet.domain.model.Transaction
import com.osscabinet.ossWallet.domain.model.CashFlowType
import com.osscabinet.ossWallet.ui.util.formatAmount
import com.osscabinet.ossWallet.ui.util.formatCurrencyRight
import java.math.BigDecimal
import java.math.RoundingMode

@Composable
fun AeonFlowBreakdownItem(
    curr: String,
    amount: BigDecimal,
    inBase: BigDecimal,
    baseCurrency: String,
    precision: Int = 2
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
        Column {
            Text(
                formatCurrencyRight(amount, curr, precision),
                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Black)
            )
            if (curr != baseCurrency) {
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
fun AeonFlowBadge(
    amount: BigDecimal,
    type: CashFlowType,
    modifier: Modifier = Modifier,
    incomeColor: Color = Color(0xFF2E7D32),
    expenseColor: Color = Color(0xFFC62828),
    precision: Int = 2
) {
    val isIncome = type == CashFlowType.INCOME
    val flowColor = if (isIncome) incomeColor else expenseColor
    val icon = if (isIncome) Icons.Rounded.ArrowDownward else Icons.Rounded.ArrowUpward

    // Following Material 3 tonal container principles
    val containerColor = flowColor.copy(alpha = 0.12f)
    val contentColor = flowColor // High contrast against the pale background

    Surface(
        modifier = modifier,
        color = containerColor,
        shape = RoundedCornerShape(8.dp),
        border = BorderStroke(1.dp, flowColor.copy(alpha = 0.2f))
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = contentColor,
                modifier = Modifier.size(14.dp)
            )
            Text(
                text = formatAmount(amount, precision),
                style = MaterialTheme.typography.labelSmall.copy(
                    fontWeight = FontWeight.ExtraBold,
                    letterSpacing = 0.5.sp
                ),
                color = contentColor
            )
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
    incomeColor: Color = Color(0xFF2E7D32),
    expenseColor: Color = Color(0xFFC62828),
    precision: Int = 2,
    content: @Composable ColumnScope.() -> Unit = {}
) {
    val isAmoled = currentTheme == "Amoled"

    val isIncome = type == CashFlowType.INCOME
    val flowColor = if (isIncome) incomeColor else expenseColor

    // M3 Tonal Palette approach: 
    // Container is very light/desaturated, content is the bold brand/semantic color
    val containerColor = if (isAmoled) Color.Black else flowColor.copy(alpha = 0.08f)
    val borderColor =
        if (isAmoled) BorderStroke(1.dp, flowColor.copy(alpha = 0.3f)) else BorderStroke(
            1.dp,
            flowColor.copy(alpha = 0.15f)
        )
    val icon = if (isIncome) Icons.Rounded.ArrowDownward else Icons.Rounded.ArrowUpward

    var cardSize by remember { mutableStateOf(IntSize.Zero) }
    val density = LocalDensity.current

    Surface(
        modifier = modifier.onSizeChanged { cardSize = it },
        shape = RoundedCornerShape(24.dp),
        color = containerColor,
        border = borderColor
    ) {
        Box {
            val iconHeight = with(density) { (cardSize.height * 0.75f).toDp() }
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier
                    .align(Alignment.CenterEnd)
//                    .offset { IntOffset(x = (cardSize.width * 0.25f).toInt(), y = 0) }
                    .size(iconHeight)
                    .alpha(0.2f),
                tint = flowColor
            )
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelMedium,
                    color = flowColor,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = formatCurrencyRight(totalInBase, baseCurrency, precision),
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Black)
                )
                Spacer(modifier = Modifier.height(12.dp))
                content()
            }
        }
    }
}

@Composable
fun AeonCashFlowSummary(
    transactions: List<Transaction>,
    baseCurrency: String,
    exchangeRates: Map<String, BigDecimal>,
    currentTheme: String,
    modifier: Modifier = Modifier,
    showTitle: Boolean = true,
    precision: Int = 2
) {
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
                    val rate = if (curr == baseCurrency) BigDecimal.ONE else (exchangeRates[curr]
                        ?: BigDecimal.ONE)
                    val incomeInBase = if (rate.compareTo(BigDecimal.ZERO) != 0) income.divide(
                        rate,
                        precision,
                        RoundingMode.HALF_UP
                    ) else income
                    val expenseInBase = if (rate.compareTo(BigDecimal.ZERO) != 0) expense.divide(
                        rate,
                        precision,
                        RoundingMode.HALF_UP
                    ) else expense
                    Triple(curr, Pair(income, incomeInBase), Pair(expense, expenseInBase))
                }
            }
        }
    }

    val totalIncomeInBase =
        flowData.fold(BigDecimal.ZERO) { acc, data -> acc.add(data.second.second) }
    val totalExpenseInBase =
        flowData.fold(BigDecimal.ZERO) { acc, data -> acc.add(data.third.second) }

    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(16.dp)) {
        if (showTitle) {
            Text(
                "CASH FLOW SUMMARY",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.outline
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
                            curr,
                            income.first,
                            income.second,
                            baseCurrency,
                            precision
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
                            curr,
                            expense.first,
                            expense.second,
                            baseCurrency,
                            precision
                        )
                    }
                }
            }
        }
    }
}
