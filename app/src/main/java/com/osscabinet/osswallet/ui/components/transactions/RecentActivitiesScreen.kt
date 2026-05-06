package com.osscabinet.ossWallet.ui.components.transactions

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Category
import androidx.compose.material.icons.rounded.History
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.osscabinet.ossWallet.data.local.TransactionGroupingType
import com.osscabinet.ossWallet.domain.model.CashFlowType
import com.osscabinet.ossWallet.domain.model.Category
import com.osscabinet.ossWallet.domain.model.TransactionWithBalance
import com.osscabinet.ossWallet.domain.service.timeline.TimelineService
import com.osscabinet.ossWallet.ui.components.categoryIcons
import com.osscabinet.ossWallet.ui.navigation.RecordsFilterContext
import com.osscabinet.ossWallet.ui.util.formatCurrencyRight
import java.math.BigDecimal
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Helper function to filter transactions to recent ones (last N or last X days, whichever is more)
 */
private fun filterToRecentTransactions(
    allTransactions: List<TransactionWithBalance>,
    limit: Int = 20,
    days: Int = 7,
    excludeFuture: Boolean = true
): List<TransactionWithBalance> {
    val now = System.currentTimeMillis()
    val base =
        if (excludeFuture) allTransactions.filter { it.transaction.timestamp <= now } else allTransactions

    // Get transactions from last 7 days
    val cutoffTime = now - (days * 24 * 60 * 60 * 1000L)
    val transactionsFromLastDays = base.filter { it.transaction.timestamp >= cutoffTime }

    // Get last 20 transactions
    val lastLimitTransactions = base.take(limit)

    // Return whichever has more transactions
    return if (transactionsFromLastDays.size > lastLimitTransactions.size) {
        transactionsFromLastDays
    } else {
        lastLimitTransactions
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun RecentActivitiesScreen(
    allTransactions: List<TransactionWithBalance>,
    accountNamesMap: Map<String, String>,
    categoryMap: Map<Long, Category>,
    onRecordsClick: (RecordsFilterContext) -> Unit,
    onTransactionClick: (Long) -> Unit = {},
    baseCurrency: String = "USD",
    exchangeRates: Map<String, BigDecimal> = emptyMap(),
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
    applyFilter: Boolean = true,
    excludeFuture: Boolean = true,
    precision: Int = 2,
    filterContext: RecordsFilterContext = RecordsFilterContext.None,
    showHeader: Boolean = true,
    headerTitle: String = "Recent Activities",
    is24Hour: Boolean = false,
    groupBy: TransactionGroupingType = TransactionGroupingType.BY_DAY
) {
    // Apply recent transactions filter if enabled
    val recentTransactions = if (applyFilter) {
        filterToRecentTransactions(allTransactions, limit, days, excludeFuture)
    } else {
        allTransactions
    }

    val groupedTransactions =
        TimelineService.groupTransactions(recentTransactions, groupBy)

    Column(
        modifier = modifier.fillMaxSize()
    ) {
        if (showHeader) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.History,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = headerTitle,
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold
                        )
                    )
                }
                TextButton(
                    onClick = { onRecordsClick(filterContext) },
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp),
                    modifier = Modifier.height(32.dp)
                ) {
                    Text(
                        "VIEW ALL",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 0.5.sp,
                            fontSize = 10.sp
                        )
                    )
                }
            }
        }

        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            contentPadding = contentPadding,
            verticalArrangement = Arrangement.spacedBy(0.dp)
        ) {
            if (recentTransactions.isEmpty()) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 16.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            "No recent activities",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                        )
                    }
                }
            } else {
                groupedTransactions.forEach { (key, groupTransactions) ->
                    val headerData = TimelineService.getHeaderData(
                        groupKey = key,
                        groupTransactions = groupTransactions,
                        allTransactions = allTransactions,
                        groupingType = groupBy,
                        baseCurrency = baseCurrency,
                        exchangeRates = exchangeRates,
                        precision = precision
                    )

                    stickyHeader {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(MaterialTheme.colorScheme.background)
                        ) {
                            TransactionHeader(
                                title = headerData.title,
                                subtitle = headerData.subtitle,
                                incomeByCurrency = headerData.incomeByCurrency,
                                expenseByCurrency = headerData.expenseByCurrency,
                                closingBalancesByCurrency = headerData.closingBalancesByCurrency,
                                allCurrencies = headerData.allCurrencies,
                                incomeBase = headerData.incomeBase,
                                expenseBase = headerData.expenseBase,
                                netBase = headerData.netBase,
                                totalClosingBalanceBase = headerData.totalClosingBalanceBase,
                                baseCurrency = baseCurrency,
                                precision = precision
                            )
                        }
                    }

                    items(groupTransactions, key = { it.transaction.id }) { item ->
                        val transaction = item.transaction
                        val category = transaction.categoryId?.let { categoryMap[it] }
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
                                amount = formatCurrencyRight(
                                    transaction.amount,
                                    transaction.currency,
                                    precision
                                ),
                                balanceBefore = formatCurrencyRight(
                                    balanceBeforeValue,
                                    transaction.currency,
                                    precision
                                ),
                                balanceAfter = formatCurrencyRight(
                                    item.balanceAfter,
                                    transaction.currency,
                                    precision
                                ),
                                type = if (transaction.amount.signum() >= 0) CashFlowType.INCOME else CashFlowType.EXPENSE,
                                timestamp = SimpleDateFormat(
                                    if (is24Hour) "HH:mm" else "hh:mm a",
                                    Locale.getDefault()
                                ).format(Date(transaction.timestamp)),
                                note = transaction.note,
                                tags = transaction.tags,
                                icon = category?.let { categoryIcons[it.iconName] }
                                    ?: categoryIcons[category?.name] ?: Icons.Rounded.Category,
                                iconBg = category?.let { Color(it.colorHex) },
                                precision = precision,
                                status = transaction.status,
                                onClick = { onTransactionClick(transaction.id) }
                            )
                        }
                    }
                }
            }
        }
    }
}
