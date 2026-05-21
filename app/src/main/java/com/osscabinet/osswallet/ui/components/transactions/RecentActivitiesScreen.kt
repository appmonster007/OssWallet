package com.osscabinet.ossWallet.ui.components.transactions

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.History
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.osscabinet.ossWallet.domain.model.CashFlowType
import com.osscabinet.ossWallet.domain.model.Transaction
import com.osscabinet.ossWallet.domain.model.TransactionFilter
import com.osscabinet.ossWallet.domain.model.TransactionGroup
import com.osscabinet.ossWallet.domain.model.TransactionListState
import com.osscabinet.ossWallet.domain.model.TransactionType
import com.osscabinet.ossWallet.domain.model.TransactionUiModel
import com.osscabinet.ossWallet.domain.model.TransactionWithBalance
import com.osscabinet.ossWallet.domain.service.timeline.TimelineService
import com.osscabinet.ossWallet.ui.components.getIconByName
import com.osscabinet.ossWallet.domain.service.currency.CurrencyFormatter.formatCurrencyRight
import com.osscabinet.ossWallet.ui.theme.AppColors
import com.osscabinet.ossWallet.ui.theme.AppTextStyles
import com.osscabinet.ossWallet.ui.theme.OssWalletTheme
import java.math.BigDecimal

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
    filter: TransactionFilter = TransactionFilter(),
    showHeader: Boolean = true,
    headerTitle: String = "Recent Activities",
    showOpeningBalance: Boolean = false,
    showClosingBalance: Boolean = true,
    showCurrencyBreakdown: Boolean = true,
    tagColors: Map<String, Int> = emptyMap()
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
                        tint = AppColors.primary,
                        modifier = Modifier.size(20.dp)
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
                        text = "VIEW ALL",
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
                    TransactionListEmptyState(
                        modifier = Modifier.padding(vertical = 32.dp)
                    )
                }
            } else {
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
                            showOpeningBalance = showOpeningBalance,
                            showClosingBalance = showClosingBalance,
                            showCurrencyBreakdown = showCurrencyBreakdown
                        )
                    }

                    itemsIndexed(group.transactions, key = { _, enriched -> enriched.item.transaction.id }) { index, enriched ->
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
                                    transaction.currency
                                ),
                                convertedAmount = if ((transaction.currency != baseCurrency) && (enriched.amountInBase != null)) {
                                    formatCurrencyRight(enriched.amountInBase, baseCurrency)
                                } else null,
                                balanceBefore = formatCurrencyRight(
                                    enriched.item.balanceBefore,
                                    enriched.accountCurrency
                                ),
                                balanceAfter = formatCurrencyRight(
                                    enriched.item.balanceAfter,
                                    enriched.accountCurrency
                                ),
                                type = when (transaction.transactionType) {
                                    TransactionType.TRANSFER -> CashFlowType.TRANSFER
                                    TransactionType.INCOME -> CashFlowType.INCOME
                                    else -> CashFlowType.EXPENSE
                                },
                                timestamp = com.osscabinet.ossWallet.ui.util.TimeUtils.formatDateTime(transaction.timestamp),
                                note = transaction.note,
                                tags = enriched.tags,
                                icon = getIconByName(enriched.categoryIcon),
                                iconBg = Color(enriched.categoryColor),
                                status = transaction.status,
                                showDividers = index < group.transactions.size - 1,
                            ) { onTransactionEvent(TransactionItemEvent.Click(transaction.id)) }
                        }
                    }
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun RecentActivitiesScreenPreview() {
    val mockTransaction = Transaction(
        id = 1L,
        name = "Starbucks Coffee",
        amount = BigDecimal("-5.50"),
        timestamp = System.currentTimeMillis(),
        currency = "USD",
        transactionType = TransactionType.EXPENSE,
        accountId = "acc1",
        categoryId = 1001L
    )
    val mockUiModel = TransactionUiModel(
        item = TransactionWithBalance(mockTransaction, BigDecimal("100.00"), BigDecimal("94.50")),
        accountName = "Primary Account",
        accountCurrency = "USD",
        categoryName = "Food & Drinks",
        categoryIcon = "Restaurant",
        categoryColor = 0xFFFF9800.toInt()
    )
    val mockGroup = TransactionGroup(
        key = "Today",
        headerData = TimelineService.GroupHeaderData(
            title = "Today",
            subtitle = "July 15",
            incomeByCurrency = emptyMap(),
            expenseByCurrency = mapOf("USD" to BigDecimal("5.50")),
            openingBalancesByCurrency = emptyMap(),
            closingBalancesByCurrency = emptyMap(),
            allCurrencies = listOf("USD"),
            incomeBase = BigDecimal.ZERO,
            expenseBase = BigDecimal("5.50"),
            netBase = BigDecimal("-5.50"),
            totalOpeningBalanceBase = BigDecimal.ZERO,
            totalClosingBalanceBase = BigDecimal.ZERO
        ),
        transactions = listOf(mockUiModel)
    )

    OssWalletTheme {
        RecentActivitiesScreen(
            listState = TransactionListState(
                groups = listOf(mockGroup),
                isEmpty = false
            ),
            onRecordsClick = {}
        )
    }
}
