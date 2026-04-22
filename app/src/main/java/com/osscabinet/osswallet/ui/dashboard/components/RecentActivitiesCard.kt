package com.osscabinet.ossWallet.ui.dashboard.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Category
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material.icons.rounded.KeyboardArrowUp
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.osscabinet.ossWallet.domain.model.TransactionWithBalance
import com.osscabinet.ossWallet.domain.model.CashFlowType
import com.osscabinet.ossWallet.data.local.entity.CategoryEntity
import com.osscabinet.ossWallet.ui.components.AeonFlowSectionHeader
import com.osscabinet.ossWallet.ui.components.categoryIcons
import com.osscabinet.ossWallet.ui.components.transactions.AeonTransactionItem
import com.osscabinet.ossWallet.ui.util.formatCurrencyRight
import com.osscabinet.ossWallet.domain.service.CurrencyService
import java.math.BigDecimal
import java.text.SimpleDateFormat
import java.util.*
import androidx.compose.foundation.ExperimentalFoundationApi

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun RecentActivitiesCard(
    recentTransactions: List<TransactionWithBalance>,
    accountNamesMap: Map<String, String>,
    categoryMap: Map<Long, CategoryEntity>,
    decimalPrecision: Int,
    onRecordsClick: (com.osscabinet.ossWallet.ui.navigation.RecordsFilterContext) -> Unit,
    onTransactionClick: (Long) -> Unit = {},
    baseCurrency: String = "USD",
    exchangeRates: Map<String, BigDecimal> = emptyMap(),
    accountCurrencyMap: Map<String, String> = emptyMap(),
    contentPadding: PaddingValues = PaddingValues(bottom = 24.dp),
    modifier: Modifier = Modifier
) {
    val calendar = Calendar.getInstance()
    val groupedByDay = recentTransactions.groupBy {
        calendar.timeInMillis = it.transaction.timestamp
        val day = calendar.get(Calendar.DAY_OF_MONTH)
        val month = calendar.get(Calendar.MONTH)
        val year = calendar.get(Calendar.YEAR)
        "$year-$month-$day"
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        verticalArrangement = Arrangement.spacedBy(0.dp),
        contentPadding = contentPadding
    ) {
        item {
            AeonFlowSectionHeader(
                title = "RECENT ACTIVITIES",
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                trailing = {
                    TextButton(onClick = { onRecordsClick(com.osscabinet.ossWallet.ui.navigation.RecordsFilterContext.None) }) {
                        Text(
                            "View all",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        )
                    }
                }
            )
        }

        groupedByDay.forEach { (dayKey, dayTransactions) ->
            val date = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault()).format(Date(dayTransactions.first().transaction.timestamp))
            val isToday = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault()).format(Date(System.currentTimeMillis())) == date
            val displayDate = if (isToday) "Today" else date

            // Calculate currency breakdown for the day
            val incomeByCurrency = dayTransactions.filter { it.transaction.transactionType == "income" }
                .groupBy { it.transaction.currency }
                .mapValues { entry -> entry.value.fold(BigDecimal.ZERO) { acc, tx -> acc.add(tx.transaction.amount) } }
            val dayIncomeBase = CurrencyService.calculateTotalInCurrency(incomeByCurrency, baseCurrency, exchangeRates, decimalPrecision)

            val expenseByCurrency = dayTransactions.filter { it.transaction.transactionType == "expense" }
                .groupBy { it.transaction.currency }
                .mapValues { entry -> entry.value.fold(BigDecimal.ZERO) { acc, tx -> acc.add(tx.transaction.amount) } }
            val dayExpenseBase = CurrencyService.calculateTotalInCurrency(expenseByCurrency, baseCurrency, exchangeRates, decimalPrecision)

            val dayNetBase = dayIncomeBase.subtract(dayExpenseBase)

            // Calculate closing balance at end of day
            val dayEndTimestamp = dayTransactions.maxOf { it.transaction.timestamp }
            val closingBalancesByCurrency = recentTransactions
                .filter { it.transaction.timestamp <= dayEndTimestamp }
                .groupBy { it.transaction.accountId }
                .mapValues { (_, accountTxs) -> accountTxs.maxBy { it.transaction.timestamp } }
                .values
                .groupBy { it.transaction.currency }
                .mapValues { (_, latestTxs) ->
                    latestTxs.fold(BigDecimal.ZERO) { acc, tx -> acc.add(tx.balanceAfter) }
                }

            val totalClosingBalanceBase = CurrencyService.calculateTotalInCurrency(
                closingBalancesByCurrency,
                baseCurrency,
                exchangeRates,
                decimalPrecision
            )

            val allDayCurrencies = (incomeByCurrency.keys + expenseByCurrency.keys)
                .distinct()
                .sorted()

            stickyHeader {
                DailyHeader(
                    date = displayDate,
                    transactionCount = dayTransactions.size,
                    incomeByCurrency = incomeByCurrency,
                    expenseByCurrency = expenseByCurrency,
                    closingBalancesByCurrency = closingBalancesByCurrency,
                    allCurrencies = allDayCurrencies,
                    incomeBase = dayIncomeBase,
                    expenseBase = dayExpenseBase,
                    netBase = dayNetBase,
                    totalClosingBalanceBase = totalClosingBalanceBase,
                    baseCurrency = baseCurrency,
                    precision = decimalPrecision
                )
            }

            items(dayTransactions, key = { it.transaction.id }) { item ->
                val transaction = item.transaction
                val categoryEntity = transaction.categoryId?.let { categoryMap[it] }
                val balanceBeforeValue = item.balanceAfter.subtract(transaction.amount)

                AeonTransactionItem(
                    category = transaction.category,
                    account = accountNamesMap[transaction.accountId] ?: "Unknown",
                    amount = formatCurrencyRight(transaction.amount.abs(), transaction.currency, decimalPrecision),
                    balanceBefore = formatCurrencyRight(balanceBeforeValue, transaction.currency, decimalPrecision),
                    balanceAfter = formatCurrencyRight(item.balanceAfter, transaction.currency, decimalPrecision),
                    type = if (transaction.transactionType == "income") CashFlowType.INCOME else CashFlowType.EXPENSE,
                    timestamp = SimpleDateFormat("MMM dd, HH:mm", Locale.getDefault()).format(Date(transaction.timestamp)),
                    note = transaction.note,
                    tags = transaction.tags,
                    icon = categoryEntity?.let { categoryIcons[it.iconName] } ?: categoryIcons[categoryEntity?.name] ?: Icons.Rounded.Category,
                    iconBg = categoryEntity?.let { Color(it.colorHex) },
                    precision = decimalPrecision,
                    onClick = { onTransactionClick(transaction.id) }
                )
            }
        }

        item {
            HorizontalDivider(
                thickness = 0.5.dp,
                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
            )
        }
    }
}

@Composable
fun DailyHeader(
    date: String,
    transactionCount: Int,
    incomeByCurrency: Map<String, BigDecimal>,
    expenseByCurrency: Map<String, BigDecimal>,
    closingBalancesByCurrency: Map<String, BigDecimal>,
    allCurrencies: List<String>,
    incomeBase: BigDecimal,
    expenseBase: BigDecimal,
    netBase: BigDecimal,
    totalClosingBalanceBase: BigDecimal,
    baseCurrency: String,
    precision: Int
) {
    var isExpanded by remember { mutableStateOf(false) }
    val positiveColor = Color(0xFF4CAF50)
    val negativeColor = MaterialTheme.colorScheme.error
    val surfaceColor = MaterialTheme.colorScheme.surfaceContainer
    val outlineColor = MaterialTheme.colorScheme.outline
    val dividerColor = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)

    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = surfaceColor
    ) {
        Column {
            HorizontalDivider(thickness = 0.5.dp, color = dividerColor)
            Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)) {
                // Row 1: Date Info
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = date.uppercase(),
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.ExtraBold,
                            color = outlineColor,
                            letterSpacing = 1.sp,
                            fontSize = 9.sp
                        )
                    )
                    Text(
                        text = "$transactionCount transaction${if (transactionCount > 1) "s" else ""}",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.Medium,
                            color = outlineColor.copy(alpha = 0.6f),
                            letterSpacing = 0.5.sp,
                            fontSize = 8.sp
                        )
                    )
                }

                Spacer(modifier = Modifier.height(6.dp))

                // Row 2: Headers
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Spacer(modifier = Modifier.width(44.dp))
                    Row(
                        modifier = Modifier.weight(1f),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        listOf("INCOME", "EXPENSE", "NET").forEach { label ->
                            Text(
                                label,
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontSize = 7.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    letterSpacing = 0.5.sp
                                ),
                                color = outlineColor.copy(alpha = 0.5f),
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                    Text(
                        "BALANCE",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontSize = 7.sp,
                            fontWeight = FontWeight.ExtraBold,
                            letterSpacing = 0.5.sp
                        ),
                        color = outlineColor.copy(alpha = 0.5f),
                        modifier = Modifier.width(80.dp),
                        textAlign = TextAlign.End
                    )
                }

                // Row 3: Aggregate (Clickable)
                val canExpand = allCurrencies.isNotEmpty()
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .then(if (canExpand) Modifier.clickable { isExpanded = !isExpanded } else Modifier)
                        .padding(vertical = 1.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        modifier = Modifier.width(44.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "TOTAL",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = FontWeight.Black,
                                fontSize = 8.5.sp
                            ),
                            color = MaterialTheme.colorScheme.primary
                        )
                        if (canExpand) {
                            Icon(
                                if (isExpanded) Icons.Rounded.KeyboardArrowUp else Icons.Rounded.KeyboardArrowDown,
                                contentDescription = null,
                                modifier = Modifier.size(10.dp),
                                tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.7f)
                            )
                        }
                    }
                    Row(
                        modifier = Modifier.weight(1f),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            formatCurrencyRight(incomeBase, baseCurrency, precision),
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, fontSize = 9.5.sp),
                            color = positiveColor,
                            modifier = Modifier.weight(1f)
                        )
                        Text(
                            formatCurrencyRight(expenseBase, baseCurrency, precision),
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, fontSize = 9.5.sp),
                            color = negativeColor,
                            modifier = Modifier.weight(1f)
                        )
                        val netSign = if (netBase >= BigDecimal.ZERO) "+ " else "- "
                        Text(
                            netSign + formatCurrencyRight(netBase.abs(), baseCurrency, precision),
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, fontSize = 9.5.sp),
                            color = if (netBase >= BigDecimal.ZERO) positiveColor else negativeColor,
                            modifier = Modifier.weight(1f)
                        )
                    }
                    Text(
                        formatCurrencyRight(totalClosingBalanceBase, baseCurrency, precision),
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, fontSize = 9.5.sp),
                        modifier = Modifier.width(80.dp),
                        textAlign = TextAlign.End,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                // Row 4+: Breakdown (Expandable)
                AnimatedVisibility(visible = isExpanded) {
                    Column(modifier = Modifier.padding(top = 4.dp)) {
                        allCurrencies.forEach { curr ->
                            val inc = incomeByCurrency[curr] ?: BigDecimal.ZERO
                            val exp = expenseByCurrency[curr] ?: BigDecimal.ZERO
                            val net = inc.subtract(exp)
                            val bal = closingBalancesByCurrency[curr]

                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 1.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = curr,
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 10.sp
                                    ),
                                    modifier = Modifier.width(44.dp),
                                    color = outlineColor.copy(alpha = 0.8f)
                                )

                                Row(
                                    modifier = Modifier.weight(1f),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Text(
                                        formatCurrencyRight(inc, curr, precision),
                                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                                        color = positiveColor.copy(alpha = 0.7f),
                                        modifier = Modifier.weight(1f)
                                    )
                                    Text(
                                        formatCurrencyRight(exp, curr, precision),
                                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                                        color = negativeColor.copy(alpha = 0.7f),
                                        modifier = Modifier.weight(1f)
                                    )
                                    val netSign = if (net >= BigDecimal.ZERO) "+ " else "- "
                                    Text(
                                        netSign + formatCurrencyRight(net.abs(), curr, precision),
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 10.sp
                                        ),
                                        color = (if (net >= BigDecimal.ZERO) positiveColor else negativeColor).copy(alpha = 0.7f),
                                        modifier = Modifier.weight(1f)
                                    )
                                }

                                Text(
                                    if (bal != null) formatCurrencyRight(bal, curr, precision) else "0.00 $curr",
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 10.sp
                                    ),
                                    modifier = Modifier.width(80.dp),
                                    textAlign = TextAlign.End,
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

