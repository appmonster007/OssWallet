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
import com.osscabinet.ossWallet.domain.model.Account
import com.osscabinet.ossWallet.domain.model.CashFlowType
import com.osscabinet.ossWallet.domain.model.TransactionStatus
import com.osscabinet.ossWallet.domain.model.TransactionType
import com.osscabinet.ossWallet.domain.model.Category
import com.osscabinet.ossWallet.domain.model.TransactionListState
import com.osscabinet.ossWallet.domain.model.TransactionWithBalance
import com.osscabinet.ossWallet.domain.model.TransactionFilter
import com.osscabinet.ossWallet.domain.service.timeline.TimelineService
import com.osscabinet.ossWallet.domain.service.filter.TransactionFilterService
import com.osscabinet.ossWallet.ui.components.getIconByName
import com.osscabinet.ossWallet.domain.service.currency.CurrencyFormatter.formatCurrencyRight
import java.math.BigDecimal
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import com.osscabinet.ossWallet.ui.theme.AppColors
import com.osscabinet.ossWallet.ui.theme.AppTextStyles

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun RecentActivitiesScreen(
    listState: TransactionListState,
    onRecordsClick: (TransactionFilter) -> Unit,
    modifier: Modifier = Modifier,
    onTransactionEvent: (TransactionItemEvent) -> Unit = {},
    baseCurrency: String = "USD",
    contentPadding: PaddingValues = PaddingValues(bottom = 24.dp),
    revealedCardIds: List<Long> = emptyList(),
    precision: Int = 2,
    filter: TransactionFilter = TransactionFilter(),
    showHeader: Boolean = true,
    headerTitle: String = "Recent Activities",
    is24Hour: Boolean = false,
    showOpeningBalance: Boolean = false,
    showClosingBalance: Boolean = true
) {

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
                        tint = AppColors.primary
                    )
                    Text(
                        text = headerTitle,
                        style = AppTextStyles.screenSubTitle.copy(
                            fontWeight = FontWeight.Bold
                        )
                    )
                }
                TextButton(
                    onClick = { onRecordsClick(filter) },
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp),
                    modifier = Modifier.height(32.dp)
                ) {
                    Text(
                        "VIEW ALL",
                        style = AppTextStyles.transactionSwipeAction.copy(
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 0.5.sp,
                            fontSize = 12.sp
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
            if (listState.isEmpty) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 16.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            "No recent activities",
                            style = AppTextStyles.transactionTitle,
                            color = AppColors.contentSecondary,
                        )
                    }
                }
            } else {
                listState.groups.forEach { group ->
                    stickyHeader {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(AppColors.screenBackground)
                        ) {
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
                                showClosingBalance = showClosingBalance
                            )
                        }
                    }

                    items(group.transactions, key = { it.item.transaction.id }) { enriched ->
                        val transaction = enriched.item.transaction

                        SwipeableTransactionItem(
                            transaction = transaction,
                            isRevealed = revealedCardIds.contains(transaction.id),
                            onEvent = onTransactionEvent
                        ) {
                            AeonTransactionItem(
                                category = enriched.categoryName,
                                account = enriched.accountName,
                                amount = formatCurrencyRight(
                                    transaction.amount,
                                    transaction.currency,
                                    precision
                                ),
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
                                type = when (transaction.transactionType) {
                                    TransactionType.TRANSFER -> CashFlowType.TRANSFER
                                    TransactionType.INCOME -> CashFlowType.INCOME
                                    else -> CashFlowType.EXPENSE
                                },
                                timestamp = SimpleDateFormat(
                                    if (is24Hour) "HH:mm" else "hh:mm a",
                                    Locale.getDefault()
                                ).format(Date(transaction.timestamp)),
                                note = transaction.note,
                                tags = transaction.tags,
                                icon = getIconByName(enriched.categoryIcon),
                                iconBg = Color(enriched.categoryColor),
                                status = transaction.status
                            ) { onTransactionEvent(TransactionItemEvent.Click(transaction.id)) }
                        }
                    }
                }
            }
        }
    }
}
