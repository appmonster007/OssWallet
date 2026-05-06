package com.osscabinet.ossWallet.ui.components.transactions

import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.osscabinet.ossWallet.data.local.TransactionGroupingType
import com.osscabinet.ossWallet.domain.service.TransactionGroupingService
import com.osscabinet.ossWallet.domain.model.CashFlowType
import com.osscabinet.ossWallet.domain.model.Transaction
import com.osscabinet.ossWallet.domain.model.TransactionWithBalance
import com.osscabinet.ossWallet.domain.model.Category
import com.osscabinet.ossWallet.ui.components.categoryIcons
import com.osscabinet.ossWallet.ui.components.transactions.TransactionHeader
import com.osscabinet.ossWallet.ui.util.formatCurrencyRight
import com.osscabinet.ossWallet.domain.service.CurrencyService
import java.math.BigDecimal

import java.text.SimpleDateFormat
import java.util.*

@OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class, ExperimentalLayoutApi::class)
@Composable
fun AeonTransactionList(
    transactions: List<TransactionWithBalance>,
    accountMap: Map<String, String>,
    categoryMap: Map<Long, Category>,
    onTransactionClick: (Transaction) -> Unit,
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues(0.dp),
    showDailyHeader: Boolean = true,
    precision: Int = 2,
    useNoteAsHeader: Boolean = false,
    showOpeningBalance: Boolean = false,
    showClosingBalance: Boolean = true,
    groupBy: TransactionGroupingType = TransactionGroupingType.BY_WEEK,
    baseCurrency: String,
    exchangeRates: Map<String, BigDecimal>,
    accountCurrencyMap: Map<String, String> = emptyMap(),
    filterAccountId: String? = null,
    revealedCardIds: List<Long> = emptyList(),
    onItemExpanded: (Long) -> Unit = {},
    onItemCollapsed: (Long) -> Unit = {},
    onStatusChange: (Long, com.osscabinet.ossWallet.domain.model.TransactionStatus) -> Unit = { _, _ -> },
    onClone: (Long) -> Unit = {},
    onDelete: (Long) -> Unit = {},
    onSplit: (Long) -> Unit = {},
    onFullSwipeRight: (Long) -> Unit = {},
    is24Hour: Boolean = false
) {
    val groupedTransactions = TransactionGroupingService.groupTransactions(transactions, groupBy)

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = contentPadding,
        verticalArrangement = Arrangement.spacedBy(0.dp)
    ) {
        groupedTransactions.forEach { (key, groupTransactions) ->
            val headerData = TransactionGroupingService.getHeaderData(
                groupKey = key,
                groupTransactions = groupTransactions,
                allTransactions = transactions,
                groupingType = groupBy,
                baseCurrency = baseCurrency,
                exchangeRates = exchangeRates,
                precision = precision
            )

            stickyHeader {
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

            items(groupTransactions, key = { it.transaction.id }) { item ->
                val category = categoryMap[item.transaction.categoryId]
                val icon = categoryIcons[category?.iconName] ?: categoryIcons[category?.name] ?: Icons.Rounded.Category
                val iconBg = category?.colorHex?.let { Color(it) } ?: MaterialTheme.colorScheme.primaryContainer

                val amountInBase = CurrencyService.convertAmount(item.transaction.amount, item.transaction.currency, baseCurrency, exchangeRates, precision)

                SwipeableTransactionItem(
                    transaction = item.transaction,
                    isRevealed = revealedCardIds.contains(item.transaction.id),
                    onExpandLeft = { onItemExpanded(item.transaction.id) },
                    onExpandRight = { onItemExpanded(item.transaction.id) },
                    onCollapse = { onItemCollapsed(item.transaction.id) },
                    onFullSwipeRight = { onFullSwipeRight(item.transaction.id) },
                    onStatusChange = { onStatusChange(item.transaction.id, it) },
                    onClone = { onClone(item.transaction.id) },
                    onDelete = { onDelete(item.transaction.id) },
                    onSplit = { onSplit(item.transaction.id) }
                ) {
                    AeonTransactionItem(
                        category = category?.name ?: "Uncategorized",
                        account = accountMap[item.transaction.accountId] ?: "Unknown Account",
                        amount = formatCurrencyRight(item.transaction.amount, item.transaction.currency, precision),
                        convertedAmount = if (item.transaction.currency != baseCurrency) {
                            formatCurrencyRight(amountInBase, baseCurrency, precision)
                        } else null,
                        balanceBefore = formatCurrencyRight(item.balanceBefore, item.transaction.currency, precision),
                        balanceAfter = formatCurrencyRight(item.balanceAfter, item.transaction.currency, precision),
                        type = if (item.transaction.amount.signum() >= 0) CashFlowType.INCOME else CashFlowType.EXPENSE,
                        timestamp = SimpleDateFormat(if (is24Hour) "EEE, MMM d • HH:mm" else "EEE, MMM d • hh:mm a", Locale.getDefault()).format(Date(item.transaction.timestamp)),
                        note = item.transaction.note,
                        tags = item.transaction.tags,
                        icon = icon,
                        iconBg = iconBg,
                        precision = precision,
                        useNoteAsHeader = useNoteAsHeader,
                        showOpeningBalance = showOpeningBalance,
                        showClosingBalance = showClosingBalance,
                        status = item.transaction.status,
                        onClick = { onTransactionClick(item.transaction) }
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
    showOpeningBalance: Boolean = false,
    showClosingBalance: Boolean = true,
    status: com.osscabinet.ossWallet.domain.model.TransactionStatus = com.osscabinet.ossWallet.domain.model.TransactionStatus.CLEARED,
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
        color = MaterialTheme.colorScheme.surface,
        shape = RectangleShape
    ) {
        Column {
            HorizontalDivider(
                modifier = Modifier.fillMaxWidth(),
                thickness = 0.5.dp,
                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
            )
            
            Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(modifier = Modifier.size(40.dp)) {
                        Surface(
                            modifier = Modifier.fillMaxSize(),
                            shape = CircleShape,
                            color = (iconBg ?: MaterialTheme.colorScheme.primaryContainer).copy(alpha = 0.2f)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    icon ?: Icons.Rounded.Category,
                                    contentDescription = null,
                                    tint = iconBg ?: MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }

                        // Status indicator
                        if (status != com.osscabinet.ossWallet.domain.model.TransactionStatus.UNCLEARED) {
                            Surface(
                                modifier = Modifier
                                    .size(14.dp)
                                    .align(Alignment.BottomStart),
                                shape = CircleShape,
                                color = when (status) {
                                    com.osscabinet.ossWallet.domain.model.TransactionStatus.CLEARED -> Color(0xFF4CAF50)
                                    com.osscabinet.ossWallet.domain.model.TransactionStatus.RECONCILE -> Color(0xFF2196F3)
                                    com.osscabinet.ossWallet.domain.model.TransactionStatus.UNCLEARED -> Color.Transparent
                                }
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = when (status) {
                                            com.osscabinet.ossWallet.domain.model.TransactionStatus.CLEARED -> Icons.Rounded.Check
                                            com.osscabinet.ossWallet.domain.model.TransactionStatus.RECONCILE -> Icons.Rounded.Sync
                                            com.osscabinet.ossWallet.domain.model.TransactionStatus.UNCLEARED -> Icons.Rounded.Check
                                        },
                                        contentDescription = null,
                                        tint = Color.White,
                                        modifier = Modifier.size(8.dp)
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    Row(
                        modifier = Modifier.weight(1f),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                headerText,
                                style = MaterialTheme.typography.bodyMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp
                                ),
                                maxLines = if (isNoteHeader) Int.MAX_VALUE else 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                subHeaderText,
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontSize = 11.sp
                                ),
                                color = MaterialTheme.colorScheme.outline,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            if (!bottomText.isNullOrBlank()) {
                                Text(
                                    bottomText,
                                    style = MaterialTheme.typography.bodySmall.copy(
                                        fontSize = 12.sp
                                    ),
                                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
                                    modifier = Modifier.padding(top = 2.dp),
                                    maxLines = if (!isNoteHeader) Int.MAX_VALUE else 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }

                        Column(horizontalAlignment = Alignment.End) {
                            Text(
                                text = amount,
                                style = MaterialTheme.typography.bodyLarge.copy(
                                    fontWeight = FontWeight.Black,
                                    fontSize = 15.sp,
                                    color = if (type == CashFlowType.INCOME) Color(0xFF4CAF50) else MaterialTheme.colorScheme.error
                                )
                            )
                            
                            if (convertedAmount != null) {
                                Text(
                                    text = convertedAmount,
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 10.sp
                                    ),
                                    color = (if (type == CashFlowType.INCOME) Color(0xFF4CAF50) else MaterialTheme.colorScheme.error).copy(alpha = 0.7f)
                                )
                            }

                            if (showOpeningBalance || showClosingBalance) {
                                val balanceText = when {
                                    showOpeningBalance && showClosingBalance -> "$balanceBefore → $balanceAfter"
                                    showOpeningBalance -> balanceBefore
                                    else -> balanceAfter
                                }
                                Text(
                                    balanceText,
                                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp),
                                    color = MaterialTheme.colorScheme.outline.copy(alpha = 0.6f)
                                )
                            }

                            Text(
                                timestamp,
                                style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp),
                                color = MaterialTheme.colorScheme.outline.copy(alpha = 0.4f),
                                modifier = Modifier.padding(top = 2.dp)
                            )
                        }
                    }
                }

                if (tags.isNotEmpty()) {
                    FlowRow(
                        modifier = Modifier.padding(top = 8.dp),
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        tags.forEach { tag ->
                            Surface(
                                shape = RoundedCornerShape(4.dp),
                                color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.3f),
                            ) {
                                Text(
                                    text = tag,
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSecondaryContainer
                                    ),
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }
                    }
                }
            }

            HorizontalDivider(
                modifier = Modifier.fillMaxWidth(),
                thickness = 0.5.dp,
                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
            )
        }
    }
}
