package com.osscabinet.ossWallet.ui.components.transactions

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.History
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.osscabinet.ossWallet.domain.model.CashFlowType
import com.osscabinet.ossWallet.domain.model.TransactionListState
import com.osscabinet.ossWallet.domain.model.TransactionWithBalance
import com.osscabinet.ossWallet.domain.model.TransactionFilter
import com.osscabinet.ossWallet.domain.model.TransactionType
import com.osscabinet.ossWallet.domain.service.timeline.TimelineService
import com.osscabinet.ossWallet.ui.components.getIconByName
import com.osscabinet.ossWallet.domain.service.currency.CurrencyFormatter.formatCurrencyRight
import java.math.BigDecimal
import com.osscabinet.ossWallet.ui.theme.AppColors
import com.osscabinet.ossWallet.ui.theme.AppTextStyles
import androidx.compose.ui.tooling.preview.Preview
import com.osscabinet.ossWallet.domain.model.Transaction
import com.osscabinet.ossWallet.domain.model.TransactionGroup
import com.osscabinet.ossWallet.domain.model.TransactionUiModel
import com.osscabinet.ossWallet.ui.theme.OssWalletTheme

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
    showCurrencyBreakdown: Boolean = true
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
                        Icons.Rounded.History,
                        contentDescription = null,
                        tint = AppColors.primary,
                        modifier = Modifier.size(20.dp)
                    )
                    Text(
                        headerTitle,
                        style = AppTextStyles.screenSubTitle.copy(
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 0.5.sp
                        ),
                        color = AppColors.primary
                    )
                }

                TextButton(onClick = { onRecordsClick(filter) }) {
                    Text(
                        "VIEW ALL",
                        style = AppTextStyles.transactionSwipeAction.copy(
                            fontWeight = FontWeight.ExtraBold,
                            letterSpacing = 1.sp
                        ),
                        color = AppColors.primary
                    )
                }
            }
        }

        if (listState.isEmpty) {
            TransactionListEmptyState(
                modifier = Modifier.weight(1f)
            )
        } else {
            LazyColumn(
                modifier = Modifier.weight(1f),
                contentPadding = contentPadding
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
                            showOpeningBalance = showOpeningBalance,
                            showClosingBalance = showClosingBalance,
                            showCurrencyBreakdown = showCurrencyBreakdown
                        )
                    }

                    items(group.transactions, key = { it.item.transaction.id }) { enriched ->
                        val tx = enriched.item.transaction
                        val icon = getIconByName(enriched.categoryIcon)
                        val iconBg = Color(enriched.categoryColor)

                        SwipeableTransactionItem(
                            transaction = tx,
                            isRevealed = revealedCardIds.contains(tx.id),
                            onEvent = onTransactionEvent
                        ) {
                            AeonTransactionItem(
                                category = enriched.categoryName,
                                account = enriched.accountName,
                                amount = formatCurrencyRight(
                                    tx.amount,
                                    tx.currency
                                ),
                                convertedAmount = if ((tx.currency != baseCurrency) && (enriched.amountInBase != null)) {
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
                                type = if (tx.amount.signum() >= 0) CashFlowType.INCOME else CashFlowType.EXPENSE,
                                timestamp = com.osscabinet.ossWallet.ui.util.TimeUtils.formatDateTime(tx.timestamp),
                                note = tx.note,
                                tags = tx.tags,
                                icon = icon,
                                iconBg = iconBg,
                                status = tx.status
                            ) { onTransactionEvent(TransactionItemEvent.Click(tx.id)) }
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
        name = "Starbucks",
        amount = BigDecimal("5.50"),
        timestamp = System.currentTimeMillis(),
        currency = "USD",
        transactionType = TransactionType.EXPENSE,
        accountId = "acc1",
        categoryId = 1001L
    )
    val mockUiModel = TransactionUiModel(
        item = TransactionWithBalance(mockTransaction, BigDecimal("100.00"), BigDecimal("94.50")),
        accountName = "Cash",
        accountCurrency = "USD",
        categoryName = "Food",
        categoryIcon = "Restaurant",
        categoryColor = 0xFF00FF00.toInt()
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
