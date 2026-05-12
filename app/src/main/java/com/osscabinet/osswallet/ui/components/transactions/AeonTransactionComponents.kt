package com.osscabinet.ossWallet.ui.components.transactions

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Category
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Sync
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.osscabinet.ossWallet.data.local.TransactionGroupingType
import com.osscabinet.ossWallet.domain.model.Account
import com.osscabinet.ossWallet.domain.model.CashFlowType
import com.osscabinet.ossWallet.domain.model.Category
import com.osscabinet.ossWallet.domain.model.Transaction
import com.osscabinet.ossWallet.domain.model.TransactionListState
import com.osscabinet.ossWallet.domain.model.TransactionWithBalance
import com.osscabinet.ossWallet.domain.service.currency.CurrencyService
import com.osscabinet.ossWallet.domain.service.timeline.TimelineService
import com.osscabinet.ossWallet.ui.components.getIconByName
import com.osscabinet.ossWallet.domain.service.currency.CurrencyFormatter.formatCurrencyRight
import java.math.BigDecimal
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class, ExperimentalLayoutApi::class)
@Composable
fun AeonTransactionList(
    listState: TransactionListState,
    onTransactionClick: (Transaction) -> Unit,
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues(0.dp),
    precision: Int = 2,
    useNoteAsHeader: Boolean = false,
    showOpeningBalance: Boolean = false,
    showClosingBalance: Boolean = true,
    showOnlyActiveCurrencies: Boolean = false,
    baseCurrency: String,
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
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = contentPadding,
        verticalArrangement = Arrangement.spacedBy(0.dp)
    ) {
        listState.groups.forEach { group ->
            stickyHeader {
                TransactionHeader(
                    title = group.headerData.title,
                    subtitle = group.headerData.subtitle,
                    incomeByCurrency = group.headerData.incomeByCurrency,
                    expenseByCurrency = group.headerData.expenseByCurrency,
                    openingBalancesByCurrency = group.headerData.openingBalancesByCurrency,
                    closingBalancesByCurrency = group.headerData.closingBalancesByCurrency,
                    allCurrencies = group.headerData.allCurrencies,
                    incomeBase = group.headerData.incomeBase,
                    expenseBase = group.headerData.expenseBase,
                    netBase = group.headerData.netBase,
                    totalOpeningBalanceBase = group.headerData.totalOpeningBalanceBase,
                    totalClosingBalanceBase = group.headerData.totalClosingBalanceBase,
                    baseCurrency = baseCurrency,
                    precision = precision,
                    showOpeningBalance = showOpeningBalance,
                    showClosingBalance = showClosingBalance,
                    showOnlyActiveCurrencies = showOnlyActiveCurrencies
                )
            }

            items(group.transactions, key = { it.item.transaction.id }) { enriched ->
                val tx = enriched.item.transaction
                val icon = getIconByName(enriched.categoryIcon)
                val iconBg = Color(enriched.categoryColor)

                SwipeableTransactionItem(
                    transaction = tx,
                    isRevealed = revealedCardIds.contains(tx.id),
                    onExpandLeft = { onItemExpanded(tx.id) },
                    onExpandRight = { onItemExpanded(tx.id) },
                    onCollapse = { onItemCollapsed(tx.id) },
                    onFullSwipeRight = { onFullSwipeRight(tx.id) },
                    onStatusChange = { onStatusChange(tx.id, it) },
                    onClone = { onClone(tx.id) },
                    onDelete = { onDelete(tx.id) },
                    onSplit = { onSplit(tx.id) }
                ) {
                    AeonTransactionItem(
                        category = enriched.categoryName,
                        account = enriched.accountName,
                        amount = formatCurrencyRight(
                            tx.amount,
                            tx.currency,
                            precision
                        ),
                        convertedAmount = if ((tx.currency != baseCurrency) && (enriched.amountInBase != null)) {
                            formatCurrencyRight(enriched.amountInBase, baseCurrency, precision)
                        } else null,
                        balanceBefore = formatCurrencyRight(
                            enriched.item.balanceBefore,
                            enriched.accountCurrency,
                            precision
                        ),
                        balanceAfter = formatCurrencyRight(
                            enriched.item.balanceAfter,
                            enriched.accountCurrency,
                            precision
                        ),
                        type = if (tx.amount.signum() >= 0) CashFlowType.INCOME else CashFlowType.EXPENSE,
                        timestamp = SimpleDateFormat(
                            if (is24Hour) "EEE, MMM d • HH:mm" else "EEE, MMM d • hh:mm a",
                            Locale.getDefault()
                        ).format(Date(tx.timestamp)),
                        note = tx.note,
                        tags = tx.tags,
                        icon = icon,
                        iconBg = iconBg,
                        useNoteAsHeader = useNoteAsHeader,
                        showOpeningBalance = showOpeningBalance,
                        showClosingBalance = showClosingBalance,
                        status = tx.status
                    ) { onTransactionClick(tx) }
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
    useNoteAsHeader: Boolean = false,
    showOpeningBalance: Boolean = false,
    showClosingBalance: Boolean = true,
    showDividers: Boolean = true,
    status: com.osscabinet.ossWallet.domain.model.TransactionStatus = com.osscabinet.ossWallet.domain.model.TransactionStatus.CLEARED,
    containerColor: Color = MaterialTheme.colorScheme.surface,
    onClick: () -> Unit
) {
    val isNoteHeader = useNoteAsHeader && !note.isNullOrBlank()
    val headerText = if (isNoteHeader) note else category
    val bottomText = if (isNoteHeader) category else note

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        color = containerColor,
        shape = RectangleShape
    ) {
        Column {
            if (showDividers) {
                HorizontalDivider(
                    modifier = Modifier.fillMaxWidth(),
                    thickness = 0.5.dp,
                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                )
            }

            Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(modifier = Modifier.size(32.dp)) {
                        Surface(
                            modifier = Modifier.fillMaxSize(),
                            shape = CircleShape,
                            color = (iconBg ?: MaterialTheme.colorScheme.primaryContainer).copy(
                                alpha = 0.2f
                            )
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    icon ?: Icons.Rounded.Category,
                                    contentDescription = null,
                                    tint = iconBg ?: MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }

                        // Status indicator
                        if (status != com.osscabinet.ossWallet.domain.model.TransactionStatus.UNCLEARED) {
                            Surface(
                                modifier = Modifier
                                    .size(12.dp)
                                    .align(Alignment.BottomStart),
                                shape = CircleShape,
                                color = when (status) {
                                    com.osscabinet.ossWallet.domain.model.TransactionStatus.CLEARED -> Color(
                                        0xFF4CAF50
                                    )

                                    com.osscabinet.ossWallet.domain.model.TransactionStatus.RECONCILE -> Color(
                                        0xFF2196F3
                                    )

                                    com.osscabinet.ossWallet.domain.model.TransactionStatus.UNCLEARED -> Color.Transparent
                                }
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = when (status) {
                                            com.osscabinet.ossWallet.domain.model.TransactionStatus.CLEARED -> Icons.Rounded.Check
                                            else -> Icons.Rounded.Sync
                                        },
                                        contentDescription = null,
                                        tint = Color.White,
                                        modifier = Modifier.size(6.dp)
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    Row(
                        modifier = Modifier.weight(1f),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                headerText,
                                style = MaterialTheme.typography.bodyMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.sp
                                ),
                                maxLines = if (isNoteHeader) Int.MAX_VALUE else 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                account,
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontSize = 10.sp
                                ),
                                color = MaterialTheme.colorScheme.outline,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            if (!bottomText.isNullOrBlank()) {
                                Text(
                                    bottomText,
                                    style = MaterialTheme.typography.bodySmall.copy(
                                        fontSize = 11.sp
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
                                    fontSize = 13.sp,
                                    color = if (type == CashFlowType.INCOME) Color(0xFF4CAF50) else MaterialTheme.colorScheme.error
                                )
                            )

                            if (convertedAmount != null) {
                                Text(
                                    text = convertedAmount,
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 9.sp
                                    ),
                                    color = (if (type == CashFlowType.INCOME) Color(0xFF4CAF50) else MaterialTheme.colorScheme.error).copy(
                                        alpha = 0.7f
                                    )
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
                                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 8.sp),
                                    color = MaterialTheme.colorScheme.outline.copy(alpha = 0.6f)
                                )
                            }

                            Text(
                                timestamp,
                                style = MaterialTheme.typography.labelSmall.copy(fontSize = 8.sp),
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
                                        fontSize = 8.sp,
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

            if (showDividers) {
                HorizontalDivider(
                    modifier = Modifier.fillMaxWidth(),
                    thickness = 0.5.dp,
                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                )
            }
        }
    }
}
