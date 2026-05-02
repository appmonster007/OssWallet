package com.osscabinet.ossWallet.ui.components.transactions

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Category
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.osscabinet.ossWallet.domain.model.TransactionWithBalance
import com.osscabinet.ossWallet.domain.model.CashFlowType
import com.osscabinet.ossWallet.domain.model.Category
import com.osscabinet.ossWallet.ui.components.categoryIcons
import com.osscabinet.ossWallet.ui.util.formatCurrencyRight
import com.osscabinet.ossWallet.ui.components.cards.AeonExpressiveCard
import java.math.BigDecimal
import java.text.SimpleDateFormat
import java.util.*

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
    val transactions = if (excludeFuture) allTransactions.filter { it.transaction.timestamp <= now } else allTransactions
    val cutoffTime = now - (days * 24 * 60 * 60 * 1000L)
    val transactionsFromLastDays = transactions.filter { it.transaction.timestamp >= cutoffTime }
    val lastLimitTransactions = transactions.take(limit)
    return if (transactionsFromLastDays.size > lastLimitTransactions.size) transactionsFromLastDays else lastLimitTransactions
}

@Composable
fun RecentActivitiesCard(
    allTransactions: List<TransactionWithBalance>,
    accountNamesMap: Map<String, String>,
    categoryMap: Map<Long, Category>,
    @Suppress("UNUSED_PARAMETER") decimalPrecision: Int,
    onRecordsClick: (com.osscabinet.ossWallet.ui.navigation.RecordsFilterContext) -> Unit,
    onTransactionClick: (Long) -> Unit = {},
    @Suppress("UNUSED_PARAMETER") baseCurrency: String = "USD",
    @Suppress("UNUSED_PARAMETER") exchangeRates: Map<String, BigDecimal> = emptyMap(),
    @Suppress("UNUSED_PARAMETER") accountCurrencyMap: Map<String, String> = emptyMap(),
    @Suppress("UNUSED_PARAMETER") contentPadding: PaddingValues = PaddingValues(bottom = 24.dp),
    modifier: Modifier = Modifier,
    @Suppress("UNUSED_PARAMETER") revealedCardIds: List<Long> = emptyList(),
    @Suppress("UNUSED_PARAMETER") onItemExpanded: (Long) -> Unit = {},
    @Suppress("UNUSED_PARAMETER") onItemCollapsed: (Long) -> Unit = {},
    @Suppress("UNUSED_PARAMETER") onStatusChange: (Long, com.osscabinet.ossWallet.domain.model.TransactionStatus) -> Unit = { _, _ -> },
    @Suppress("UNUSED_PARAMETER") onClone: (Long) -> Unit = {},
    @Suppress("UNUSED_PARAMETER") onDelete: (Long) -> Unit = {},
    @Suppress("UNUSED_PARAMETER") onSplit: (Long) -> Unit = {},
    @Suppress("UNUSED_PARAMETER") onFullSwipeRight: (Long) -> Unit = {},
    limit: Int = 20,
    days: Int = 7,
    applyFilter: Boolean = true,
    excludeFuture: Boolean = true,
    precision: Int = 2
) {
    val now = System.currentTimeMillis()
    val base = if (excludeFuture) allTransactions.filter { it.transaction.timestamp <= now } else allTransactions
    val recentTransactions = if (applyFilter) filterToRecentTransactions(base, limit, days, excludeFuture = false) else base

    val calendar = Calendar.getInstance()
    val groupedByDay = recentTransactions.groupBy {
        calendar.timeInMillis = it.transaction.timestamp
        val day = calendar.get(Calendar.DAY_OF_MONTH)
        val month = calendar.get(Calendar.MONTH)
        val year = calendar.get(Calendar.YEAR)
        "$year-$month-$day"
    }

    AeonExpressiveCard(
        title = "RECENT ACTIVITIES",
        modifier = modifier,
        headerTrailing = {
            TextButton(
                onClick = { onRecordsClick(com.osscabinet.ossWallet.ui.navigation.RecordsFilterContext.None) },
                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp),
                modifier = Modifier.height(28.dp)
            ) {
                Text(
                    "VIEW ALL",
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontWeight = FontWeight.ExtraBold,
                        letterSpacing = 0.5.sp,
                        fontSize = 10.sp
                    )
                )
            }
        }
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            if (recentTransactions.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        "No recent activities",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                groupedByDay.forEach { (_, dayTransactions) ->
                    val date = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault()).format(Date(dayTransactions.first().transaction.timestamp))
                    val isToday = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault()).format(Date(System.currentTimeMillis())) == date
                    val displayDate = if (isToday) "Today" else date

                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                displayDate.uppercase(),
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.outline,
                                    letterSpacing = 0.5.sp
                                )
                            )
                            Text(
                                "${dayTransactions.size} txs",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    color = MaterialTheme.colorScheme.outline.copy(alpha = 0.7f)
                                )
                            )
                        }

                        dayTransactions.forEach { item ->
                            val transaction = item.transaction
                            val category = transaction.categoryId?.let { categoryMap[it] }
                            val balanceBeforeValue = item.balanceAfter.subtract(transaction.amount)

                            AeonTransactionItem(
                                category = transaction.category,
                                account = accountNamesMap[transaction.accountId] ?: "Unknown",
                                amount = formatCurrencyRight(transaction.amount, transaction.currency, precision),
                                balanceBefore = formatCurrencyRight(balanceBeforeValue, transaction.currency, precision),
                                balanceAfter = formatCurrencyRight(item.balanceAfter, transaction.currency, precision),
                                type = if (transaction.amount.signum() >= 0) CashFlowType.INCOME else CashFlowType.EXPENSE,
                                timestamp = SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(transaction.timestamp)),
                                note = transaction.note,
                                tags = transaction.tags,
                                icon = category?.let { categoryIcons[it.iconName] } ?: categoryIcons[category?.name] ?: Icons.Rounded.Category,
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
