package com.osscabinet.ossWallet.ui.components.transactions

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.TrendingUp
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.osscabinet.ossWallet.domain.model.CashFlowType
import com.osscabinet.ossWallet.domain.model.Transaction
import com.osscabinet.ossWallet.domain.model.TransactionWithBalance
import com.osscabinet.ossWallet.data.local.entity.CategoryEntity
import com.osscabinet.ossWallet.ui.components.categoryIcons
import com.osscabinet.ossWallet.ui.util.formatCurrencyRight
import com.osscabinet.ossWallet.ui.util.convertAmount
import com.osscabinet.ossWallet.ui.util.calculateTotalInCurrency
import java.math.BigDecimal

import java.text.SimpleDateFormat
import java.util.*

@OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class, ExperimentalLayoutApi::class)
@Composable
fun AeonTransactionList(
    transactions: List<TransactionWithBalance>,
    accountMap: Map<String, String>,
    categoryMap: Map<Long, CategoryEntity>,
    onTransactionClick: (Transaction) -> Unit,
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues(0.dp),
    showDailyHeader: Boolean = true,
    precision: Int = 2,
    useNoteAsHeader: Boolean = false,
    baseCurrency: String,
    exchangeRates: Map<String, BigDecimal>,
    accountCurrencyMap: Map<String, String> = emptyMap(),
    filterAccountId: String? = null
) {
    val calendar = Calendar.getInstance()
    val groupedByWeek = transactions.groupBy {
        calendar.timeInMillis = it.transaction.timestamp
        val weekOfYear = calendar.get(Calendar.WEEK_OF_YEAR)
        val year = calendar.get(Calendar.YEAR)
        "$year-W$weekOfYear"
    }

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = contentPadding,
        verticalArrangement = Arrangement.spacedBy(0.dp)
    ) {
        groupedByWeek.forEach { (weekKey, weekTransactions) ->
            val (year, week) = weekKey.split("-W").map { it.toInt() }
            calendar.set(Calendar.YEAR, year)
            calendar.set(Calendar.WEEK_OF_YEAR, week)
            calendar.set(Calendar.DAY_OF_WEEK, Calendar.MONDAY)
            val weekStart = calendar.time
            calendar.add(Calendar.DAY_OF_WEEK, 6)
            val weekEnd = calendar.time
            val weekRange = "${SimpleDateFormat("MMM d", Locale.getDefault()).format(weekStart)} - ${SimpleDateFormat("MMM d, yyyy", Locale.getDefault()).format(weekEnd)}"

            // Aggregation using CurrencyUtils (CurrencyUtilkit)
            val incomeTotals = weekTransactions.filter { it.transaction.isIncome }
                .groupBy { it.transaction.currency }
                .mapValues { entry -> entry.value.fold(BigDecimal.ZERO) { acc, tx -> acc.add(tx.transaction.amount) } }
            val weekIncome = calculateTotalInCurrency(incomeTotals, baseCurrency, exchangeRates, precision)

            val expenseTotals = weekTransactions.filter { !it.transaction.isIncome }
                .groupBy { it.transaction.currency }
                .mapValues { entry -> entry.value.fold(BigDecimal.ZERO) { acc, tx -> acc.add(tx.transaction.amount) } }
            val weekExpense = calculateTotalInCurrency(expenseTotals, baseCurrency, exchangeRates, precision)

            val weekNet = weekIncome.subtract(weekExpense)
            
            // Determine if we should show closing balance: 
            // 1. If we are filtering by a specific account
            // 2. Or if all transactions in this week belong to the same account
            val uniqueAccountIds = weekTransactions.map { it.transaction.accountId }.distinct()
            val showBalanceColumn = filterAccountId != null || uniqueAccountIds.size == 1
            
            val closingBalance = if (showBalanceColumn) {
                val targetAccId = filterAccountId ?: uniqueAccountIds.firstOrNull()
                weekTransactions.filter { it.transaction.accountId == targetAccId }
                    .maxByOrNull { it.transaction.timestamp }?.let { lastItem ->
                        val accountCurrency = accountCurrencyMap[lastItem.transaction.accountId] ?: baseCurrency
                        convertAmount(lastItem.balanceAfter, accountCurrency, baseCurrency, exchangeRates, precision)
                    } ?: BigDecimal.ZERO
            } else {
                BigDecimal.ZERO
            }

            stickyHeader {
                val positiveColor = Color(0xFF4CAF50)
                val negativeColor = MaterialTheme.colorScheme.error
                val netColor = if (weekNet >= BigDecimal.ZERO) positiveColor else negativeColor
                val surfaceColor = MaterialTheme.colorScheme.surface
                val outlineColor = MaterialTheme.colorScheme.outline
                val dividerColor = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)

                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    color = surfaceColor
                ) {
                    Column {
                        HorizontalDivider(
                            thickness = 0.5.dp,
                            color = dividerColor
                        )
                        Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)) {
                            Text(
                                text = "WEEK $week ($weekRange)".uppercase(),
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontWeight = FontWeight.ExtraBold,
                                    color = outlineColor,
                                    letterSpacing = 1.sp
                                )
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                    Column {
                                        Text("INCOME", style = MaterialTheme.typography.labelSmall, color = outlineColor.copy(alpha = 0.6f))
                                        Text(formatCurrencyRight(weekIncome, baseCurrency, precision), style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold, color = Color(0xFF4CAF50)))
                                    }
                                    Column {
                                        Text("EXPENSE", style = MaterialTheme.typography.labelSmall, color = outlineColor.copy(alpha = 0.6f))
                                        Text(formatCurrencyRight(weekExpense, baseCurrency, precision), style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold, color = negativeColor))
                                    }
                                    Column {
                                        Text("TOTAL", style = MaterialTheme.typography.labelSmall, color = outlineColor.copy(alpha = 0.6f))
                                        Text(
                                            (if (weekNet > BigDecimal.ZERO) "+ " else "") + formatCurrencyRight(weekNet, baseCurrency, precision),
                                            style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold, color = netColor)
                                        )
                                    }
                                }
                                if (showBalanceColumn) {
                                    Column(horizontalAlignment = Alignment.End) {
                                        Text("Balance", style = MaterialTheme.typography.labelSmall, color = outlineColor.copy(alpha = 0.6f))
                                        Text(formatCurrencyRight(closingBalance, baseCurrency, precision), style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold))
                                    }
                                }
                            }
                        }
                    }
                }
            }

            items(weekTransactions, key = { it.transaction.id }) { item ->
                val category = categoryMap[item.transaction.categoryId]
                val icon = categoryIcons[category?.iconName] ?: categoryIcons[category?.name] ?: Icons.Rounded.Category
                val iconBg = category?.colorHex?.let { Color(it) } ?: MaterialTheme.colorScheme.primaryContainer

                val accountCurrency = accountCurrencyMap[item.transaction.accountId] ?: baseCurrency
                val isFilteredToAccount = filterAccountId != null
                
                // Determine the display currency for balances
                val balanceCurrency = if (isFilteredToAccount) accountCurrency else baseCurrency
                
                val amountInBase = convertAmount(item.transaction.amount, item.transaction.currency, baseCurrency, exchangeRates, precision)

                AeonTransactionItem(
                    category = category?.name ?: "Uncategorized",
                    account = accountMap[item.transaction.accountId] ?: "Unknown Account",
                    amount = formatCurrencyRight(item.transaction.amount.abs(), item.transaction.currency, precision),
                    convertedAmount = if (item.transaction.currency != baseCurrency) {
                        formatCurrencyRight(amountInBase.abs(), baseCurrency, precision)
                    } else null,
                    balanceBefore = formatCurrencyRight(item.balanceBefore, balanceCurrency, precision),
                    balanceAfter = formatCurrencyRight(item.balanceAfter, balanceCurrency, precision),
                    type = if (item.transaction.isIncome) CashFlowType.INCOME else CashFlowType.EXPENSE,
                    timestamp = SimpleDateFormat("EEE, MMM d • HH:mm", Locale.getDefault()).format(Date(item.transaction.timestamp)),
                    note = item.transaction.note,
                    tags = item.transaction.tags,
                    icon = icon,
                    iconBg = iconBg,
                    precision = precision,
                    useNoteAsHeader = useNoteAsHeader,
                    onClick = { onTransactionClick(item.transaction) }
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

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun AeonTransactionItem(
    category: String,
    account: String,
    amount: String,
    modifier: Modifier = Modifier,
    convertedAmount: String? = null,
    balanceBefore: String,
    balanceAfter: String,
    type: CashFlowType,
    timestamp: String,
    note: String? = null,
    tags: List<String> = emptyList(),
    icon: ImageVector? = null,
    iconBg: Color? = null,
    precision: Int = 2,
    useNoteAsHeader: Boolean = false,
    onClick: () -> Unit
) {
    val isNoteHeader = useNoteAsHeader && !note.isNullOrBlank()
    val headerText = if (isNoteHeader) note else category
    val subHeaderText = account
    val bottomText = if (isNoteHeader) category else note

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        color = MaterialTheme.colorScheme.surface
    ) {
        Column {
            HorizontalDivider(
                modifier = Modifier.fillMaxWidth(),
                thickness = 0.5.dp,
                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
            )
            Row(
                modifier = Modifier.padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    modifier = Modifier.size(48.dp),
                    shape = CircleShape,
                    color = (iconBg ?: MaterialTheme.colorScheme.primaryContainer).copy(alpha = 0.2f)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            icon ?: Icons.Rounded.Category,
                            contentDescription = null,
                            tint = iconBg ?: MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.width(16.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            headerText,
                            style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f)
                        )
                        Text(
                            (if (type == CashFlowType.INCOME) "+" else "-") + amount,
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Black,
                                color = if (type == CashFlowType.INCOME) Color(0xFF4CAF50) else MaterialTheme.colorScheme.error
                            )
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            subHeaderText,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.outline,
                            modifier = Modifier.weight(1f),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        if (convertedAmount != null) {
                            Text(
                                (if (type == CashFlowType.INCOME) "+" else "-") + convertedAmount,
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                color = (if (type == CashFlowType.INCOME) Color(0xFF4CAF50) else MaterialTheme.colorScheme.error).copy(alpha = 0.7f)
                            )
                        } else {
                            Text(
                                "$balanceBefore → $balanceAfter",
                                style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                                color = MaterialTheme.colorScheme.outline.copy(alpha = 0.6f)
                            )
                        }
                    }

                    if (convertedAmount != null) {
                        Text(
                            "$balanceBefore → $balanceAfter",
                            style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                            color = MaterialTheme.colorScheme.outline.copy(alpha = 0.6f),
                            modifier = Modifier.align(Alignment.End)
                        )
                    }

                    if (!bottomText.isNullOrBlank()) {
                        Text(
                            bottomText,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(top = 4.dp),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    if (tags.isNotEmpty()) {
                        FlowRow(
                            modifier = Modifier.padding(top = 8.dp),
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                            verticalArrangement = Arrangement.spacedBy(2.dp)
                        ) {
                            tags.forEach { tag ->
                                Surface(
                                    shape = RoundedCornerShape(6.dp),
                                    color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.4f),
                                ) {
                                    Text(
                                        text = tag,
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.onSecondaryContainer
                                        ),
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                                    )
                                }
                            }
                        }
                    }

                    Text(
                        timestamp,
                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp),
                        color = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f),
                        modifier = Modifier
                            .align(Alignment.End)
                            .padding(top = 4.dp)
                    )
                }
            }
        }
    }
}
