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
    useNoteAsHeader: Boolean = false
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

            val weekIncome = weekTransactions.filter { it.transaction.isIncome }.fold(BigDecimal.ZERO) { acc, tx -> acc.add(tx.transaction.amount) }
            val weekExpense = weekTransactions.filter { !it.transaction.isIncome }.fold(BigDecimal.ZERO) { acc, tx -> acc.add(tx.transaction.amount) }
            val closingBalance = weekTransactions.maxByOrNull { it.transaction.timestamp }?.balanceAfter ?: BigDecimal.ZERO

            stickyHeader {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    color = MaterialTheme.colorScheme.surface
                ) {
                    Column {
                        HorizontalDivider(
                            thickness = 0.5.dp,
                            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                        )
                        Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)) {
                            Text(
                                text = "WEEK $week ($weekRange)".uppercase(),
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontWeight = FontWeight.ExtraBold,
                                    color = MaterialTheme.colorScheme.outline,
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
                                        Text("INCOME", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.outline.copy(alpha = 0.6f))
                                        Text(formatCurrencyRight(weekIncome, "", precision), style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold, color = Color(0xFF4CAF50)))
                                    }
                                    Column {
                                        Text("EXPENSE", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.outline.copy(alpha = 0.6f))
                                        Text(formatCurrencyRight(weekExpense, "", precision), style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.error))
                                    }
                                    Column {
                                        Text("TOTAL", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.outline.copy(alpha = 0.6f))
                                        Text("∑ " + formatCurrencyRight(weekIncome-weekExpense, "", precision), style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.error))
                                    }
                                }
                                Column(horizontalAlignment = Alignment.End) {
                                    Text("Balance", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.outline.copy(alpha = 0.6f))
                                    Text(formatCurrencyRight(closingBalance, "", precision), style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold))
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

                val balanceBefore = item.balanceAfter.subtract(item.transaction.amount)

                AeonTransactionItem(
                    category = category?.name ?: "Uncategorized",
                    account = accountMap[item.transaction.accountId] ?: "Unknown Account",
                    amount = formatCurrencyRight(item.transaction.amount.abs(), "", precision),
                    balanceBefore = formatCurrencyRight(balanceBefore, "", precision),
                    balanceAfter = formatCurrencyRight(item.balanceAfter, "", precision),
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
    useNoteAsHeader: Boolean = false,
    onClick: () -> Unit
) {
    val headerText = if (useNoteAsHeader && !note.isNullOrBlank()) note else category
    val subHeaderText = if (useNoteAsHeader && !note.isNullOrBlank()) category else account

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
                            overflow = TextOverflow.Ellipsis
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
                            if (useNoteAsHeader && !note.isNullOrBlank()) "$subHeaderText • $account" else subHeaderText,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.outline,
                            modifier = Modifier.weight(1f),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            "$balanceBefore → $balanceAfter",
                            style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                            color = MaterialTheme.colorScheme.outline.copy(alpha = 0.6f)
                        )
                    }
                    
                    Text(
                        timestamp,
                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp),
                        color = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f),
                        modifier = Modifier.align(Alignment.End)
                    )

                    if (!useNoteAsHeader && !note.isNullOrBlank()) {
                        Text(
                            note,
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
                }
            }
        }
    }
}
