package com.osscabinet.ossWallet.ui.components.transactions

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Category
import androidx.compose.material.icons.rounded.History
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
        title = "Recent Activities",
        icon = Icons.Rounded.History,
        modifier = modifier,
        headerTrailing = {
            TextButton(
                onClick = { onRecordsClick(com.osscabinet.ossWallet.ui.navigation.RecordsFilterContext.None) },
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
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            if (recentTransactions.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        "No recent activities",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                    )
                }
            } else {
                groupedByDay.forEach { (_, dayTransactions) ->
                    val date = SimpleDateFormat("MMM dd", Locale.getDefault()).format(Date(dayTransactions.first().transaction.timestamp))
                    val isToday = SimpleDateFormat("MMM dd", Locale.getDefault()).format(Date(System.currentTimeMillis())) == date
                    val displayDate = if (isToday) "Today" else date

                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text(
                            text = displayDate.uppercase(),
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                                letterSpacing = 0.5.sp,
                                fontSize = 9.sp
                            )
                        )

                        dayTransactions.forEach { item ->
                            val transaction = item.transaction
                            val category = transaction.categoryId?.let { categoryMap[it] }
                            AeonCardTransactionItem(
                                category = transaction.category,
                                account = accountNamesMap[transaction.accountId] ?: "Unknown",
                                amount = formatCurrencyRight(transaction.amount, transaction.currency, precision),
                                type = if (transaction.amount.signum() >= 0) CashFlowType.INCOME else CashFlowType.EXPENSE,
                                timestamp = SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(transaction.timestamp)),
                                icon = category?.let { categoryIcons[it.iconName] } ?: categoryIcons[category?.name] ?: Icons.Rounded.Category,
                                iconBg = category?.let { Color(it.colorHex) } ?: MaterialTheme.colorScheme.primary,
                                onClick = { onTransactionClick(transaction.id) }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun AeonCardTransactionItem(
    category: String,
    account: String,
    amount: String,
    type: CashFlowType,
    timestamp: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    iconBg: Color,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        color = Color.Transparent,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Surface(
                modifier = Modifier.size(36.dp),
                shape = androidx.compose.foundation.shape.CircleShape,
                color = iconBg.copy(alpha = 0.15f)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp),
                        tint = iconBg
                    )
                }
            }
            
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = category,
                    style = MaterialTheme.typography.titleSmall.copy(
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 14.sp
                    ),
                    maxLines = 1,
                    overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                )
                Text(
                    text = "$account • $timestamp",
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Medium
                    ),
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                    maxLines = 1,
                    overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                )
            }
            
            Text(
                text = amount,
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp,
                    color = if (type == CashFlowType.INCOME) Color(0xFF004D40) else Color(0xFFB3261E)
                )
            )
        }
    }
}

@androidx.compose.ui.tooling.preview.Preview(showBackground = true)
@Composable
fun RecentActivitiesCardPreview() {
    val tx1 = com.osscabinet.ossWallet.domain.model.Transaction(
        id = 1L, title = "Lunch", amount = java.math.BigDecimal("-50.00"), currency = "USD",
        category = "Food", timestamp = System.currentTimeMillis(), accountId = "1", transactionType = "expense"
    )
    val tx2 = com.osscabinet.ossWallet.domain.model.Transaction(
        id = 2L, title = "Salary", amount = java.math.BigDecimal("2000.00"), currency = "USD",
        category = "Salary", timestamp = System.currentTimeMillis() - 86400000L, accountId = "1", transactionType = "income"
    )
    val transactions = listOf(
        TransactionWithBalance(
            transaction = tx1,
            balanceBefore = java.math.BigDecimal("5000.00"),
            balanceAfter = java.math.BigDecimal("4950.00")
        ),
        TransactionWithBalance(
            transaction = tx2,
            balanceBefore = java.math.BigDecimal("3000.00"),
            balanceAfter = java.math.BigDecimal("5000.00")
        )
    )

    com.osscabinet.ossWallet.ui.theme.OssWalletTheme {
        Box(modifier = Modifier.padding(16.dp)) {
            RecentActivitiesCard(
                allTransactions = transactions,
                accountNamesMap = mapOf("1" to "Main Account"),
                categoryMap = emptyMap(),
                decimalPrecision = 2,
                onRecordsClick = {}
            )
        }
    }
}
