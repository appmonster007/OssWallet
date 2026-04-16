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
import java.math.BigDecimal

@OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)
@Composable
fun AeonTransactionList(
    transactions: List<TransactionWithBalance>,
    accountMap: Map<String, String>,
    categoryMap: Map<Long, CategoryEntity>,
    onTransactionClick: (Transaction) -> Unit,
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues(0.dp),
    showDailyHeader: Boolean = true,
    precision: Int = 2
) {
    val grouped = if (showDailyHeader) {
        transactions.groupBy {
            java.text.SimpleDateFormat("MMMM d, yyyy", java.util.Locale.getDefault()).format(java.util.Date(it.transaction.timestamp))
        }
    } else {
        mapOf("" to transactions)
    }

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = contentPadding,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        grouped.forEach { (date, dailyTransactions) ->
            if (date.isNotEmpty()) {
                stickyHeader {
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        color = MaterialTheme.colorScheme.surface
                    ) {
                        Text(
                            text = date.uppercase(),
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = FontWeight.ExtraBold,
                                color = MaterialTheme.colorScheme.outline,
                                letterSpacing = 1.sp
                            )
                        )
                    }
                }
            }

            items(dailyTransactions, key = { it.transaction.id }) { item ->
                val category = categoryMap[item.transaction.categoryId]
                val icon = categoryIcons[category?.iconName] ?: categoryIcons[category?.name] ?: Icons.Rounded.Category
                val iconBg = category?.colorHex?.let { Color(it) } ?: MaterialTheme.colorScheme.primaryContainer

                val balanceBefore = item.balanceAfter.subtract(item.transaction.amount)

                AeonTransactionItem(
                    category = category?.name ?: "Uncategorized",
                    account = accountMap[item.transaction.accountId] ?: "Unknown Account",
                    amount = formatCurrencyRight(item.transaction.amount.abs(), "", precision),
                    balanceBefore = formatCurrencyRight(balanceBefore, "", precision),
                    balanceAfter = formatCurrencyRight(item.balanceAfter, "", precision),
                    type = if (item.transaction.isIncome) CashFlowType.INCOME else CashFlowType.EXPENSE,
                    timestamp = java.text.SimpleDateFormat("HH:mm", java.util.Locale.getDefault()).format(java.util.Date(item.transaction.timestamp)),
                    note = item.transaction.note,
                    tags = item.transaction.tags,
                    icon = icon,
                    iconBg = iconBg,
                    precision = precision,
                    onClick = { onTransactionClick(item.transaction) }
                )
            }
        }
    }
}

@Composable
fun AeonTransactionItem(
    category: String,
    account: String,
    amount: String,
    balanceBefore: String,
    balanceAfter: String,
    type: CashFlowType,
    timestamp: String,
    modifier: Modifier = Modifier,
    note: String? = null,
    tags: List<String> = emptyList(),
    icon: ImageVector? = null,
    iconBg: Color? = null,
    precision: Int = 2,
    onClick: () -> Unit
) {
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(28.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f)
    ) {
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
                        category,
                        style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        (if (type == CashFlowType.INCOME) "+" else "-") + amount,
                        style = MaterialTheme.typography.bodyLarge.copy(
                            fontWeight = FontWeight.ExtraBold,
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
                        "$account • $timestamp",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.outline
                    )
                    Text(
                        "$balanceBefore → $balanceAfter",
                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                        color = MaterialTheme.colorScheme.outline.copy(alpha = 0.6f)
                    )
                }

                if (!note.isNullOrBlank()) {
                    Text(
                        note,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 4.dp),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}
