package com.osscabinet.ossWallet.ui.components.transactions

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Category
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
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
import com.osscabinet.ossWallet.ui.components.transactions.TransactionHeader
import com.osscabinet.ossWallet.ui.util.formatCurrencyRight
import com.osscabinet.ossWallet.domain.service.CurrencyService
import java.math.BigDecimal
import java.text.SimpleDateFormat
import java.util.*
import androidx.compose.foundation.ExperimentalFoundationApi

/**
 * Helper function to filter transactions to recent ones (last N or last X days, whichever is more)
 * This is the same logic used in TransactionUseCases.getRecentTransactions
 */
private fun filterToRecentTransactions(
    allTransactions: List<TransactionWithBalance>,
    limit: Int = 20,
    days: Int = 7
): List<TransactionWithBalance> {
    // Get transactions from last 7 days
    val cutoffTime = System.currentTimeMillis() - (days * 24 * 60 * 60 * 1000L)
    val transactionsFromLastDays = allTransactions.filter { it.transaction.timestamp >= cutoffTime }

    // Get last 20 transactions
    val lastLimitTransactions = allTransactions.take(limit)

    // Return whichever has more transactions
    return if (transactionsFromLastDays.size > lastLimitTransactions.size) {
        transactionsFromLastDays
    } else {
        lastLimitTransactions
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun RecentActivitiesCard(
    allTransactions: List<TransactionWithBalance>,
    accountNamesMap: Map<String, String>,
    categoryMap: Map<Long, CategoryEntity>,
    decimalPrecision: Int,
    onRecordsClick: (com.osscabinet.ossWallet.ui.navigation.RecordsFilterContext) -> Unit,
    onTransactionClick: (Long) -> Unit = {},
    baseCurrency: String = "USD",
    exchangeRates: Map<String, BigDecimal> = emptyMap(),
    accountCurrencyMap: Map<String, String> = emptyMap(),
    contentPadding: PaddingValues = PaddingValues(bottom = 24.dp),
    modifier: Modifier = Modifier,
    revealedCardIds: List<Long> = emptyList(),
    onItemExpanded: (Long) -> Unit = {},
    onItemCollapsed: (Long) -> Unit = {},
    onStatusChange: (Long, com.osscabinet.ossWallet.domain.model.TransactionStatus) -> Unit = { _, _ -> },
    onClone: (Long) -> Unit = {},
    onDelete: (Long) -> Unit = {},
    onSplit: (Long) -> Unit = {},
    onFullSwipeRight: (Long) -> Unit = {},
    limit: Int = 20,
    days: Int = 7,
    applyFilter: Boolean = true
) {
    // Apply recent transactions filter if enabled
    val recentTransactions = if (applyFilter) {
        filterToRecentTransactions(allTransactions, limit, days)
    } else {
        allTransactions
    }

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

            // Calculate currency breakdown for the day (using signed amounts)
            val incomeByCurrency = dayTransactions.filter { it.transaction.amount.signum() >= 0 }
                .groupBy { it.transaction.currency }
                .mapValues { entry -> entry.value.fold(BigDecimal.ZERO) { acc, tx -> acc.add(tx.transaction.amount) } }
            val dayIncomeBase = CurrencyService.calculateTotalInCurrency(incomeByCurrency, baseCurrency, exchangeRates, decimalPrecision)

            val expenseByCurrency = dayTransactions.filter { it.transaction.amount.signum() < 0 }
                .groupBy { it.transaction.currency }
                .mapValues { entry -> entry.value.fold(BigDecimal.ZERO) { acc, tx -> acc.add(tx.transaction.amount) } }
            val dayExpenseBase = CurrencyService.calculateTotalInCurrency(expenseByCurrency, baseCurrency, exchangeRates, decimalPrecision)

            val dayNetBase = dayIncomeBase.add(dayExpenseBase)

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
                TransactionHeader(
                    title = displayDate,
                    subtitle = "${dayTransactions.size} transaction${if (dayTransactions.size > 1) "s" else ""}",
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

                SwipeableTransactionItem(
                    transaction = transaction,
                    isRevealed = revealedCardIds.contains(transaction.id),
                    onExpandLeft = { onItemExpanded(transaction.id) },
                    onExpandRight = { onItemExpanded(transaction.id) },
                    onCollapse = { onItemCollapsed(transaction.id) },
                    onFullSwipeRight = { onFullSwipeRight(transaction.id) },
                    onStatusChange = { onStatusChange(transaction.id, it) },
                    onClone = { onClone(transaction.id) },
                    onDelete = { onDelete(transaction.id) },
                    onSplit = { onSplit(transaction.id) }
                ) {
                    AeonTransactionItem(
                        category = transaction.category,
                        account = accountNamesMap[transaction.accountId] ?: "Unknown",
                        amount = formatCurrencyRight(transaction.amount, transaction.currency, decimalPrecision),
                        balanceBefore = formatCurrencyRight(balanceBeforeValue, transaction.currency, decimalPrecision),
                        balanceAfter = formatCurrencyRight(item.balanceAfter, transaction.currency, decimalPrecision),
                        type = if (transaction.amount.signum() >= 0) CashFlowType.INCOME else CashFlowType.EXPENSE,
                        timestamp = SimpleDateFormat("MMM dd, HH:mm", Locale.getDefault()).format(Date(transaction.timestamp)),
                        note = transaction.note,
                        tags = transaction.tags,
                        icon = categoryEntity?.let { categoryIcons[it.iconName] } ?: categoryIcons[categoryEntity?.name] ?: Icons.Rounded.Category,
                        iconBg = categoryEntity?.let { Color(it.colorHex) },
                        precision = decimalPrecision,
                        status = transaction.status,
                        onClick = { onTransactionClick(transaction.id) }
                    )
                }
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
