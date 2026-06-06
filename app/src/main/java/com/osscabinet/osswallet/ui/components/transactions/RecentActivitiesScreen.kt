package com.osscabinet.ossWallet.ui.components.transactions

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.runtime.remember
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
import com.osscabinet.ossWallet.domain.model.Category
import com.osscabinet.ossWallet.domain.model.CashFlowType
import com.osscabinet.ossWallet.domain.model.ConfirmedTransactionUiModel
import com.osscabinet.ossWallet.domain.model.PlannedPaymentUiModel
import com.osscabinet.ossWallet.domain.model.RecordUiModel
import com.osscabinet.ossWallet.domain.model.Transaction
import com.osscabinet.ossWallet.domain.model.TransactionFilter
import com.osscabinet.ossWallet.domain.model.TransactionGroup
import com.osscabinet.ossWallet.domain.model.TransactionListState
import com.osscabinet.ossWallet.domain.model.TransactionType
import com.osscabinet.ossWallet.domain.model.TransactionWithBalance
import com.osscabinet.ossWallet.ui.components.aeonHazeSource
import com.osscabinet.ossWallet.domain.service.currency.CurrencyFormatter.formatAuto
import com.osscabinet.ossWallet.domain.service.timeline.TimelineService
import com.osscabinet.ossWallet.ui.components.getIconByName
import com.osscabinet.ossWallet.ui.theme.AppColors
import com.osscabinet.ossWallet.ui.theme.AppTextStyles
import com.osscabinet.ossWallet.ui.theme.LocalAeonHaze
import com.osscabinet.ossWallet.ui.theme.AeonSize
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
    contentPadding: PaddingValues = PaddingValues(bottom = AeonSize.D24.value),
    revealedCardIds: List<String> = emptyList(),
    filter: TransactionFilter = TransactionFilter(),
    showHeader: Boolean = true,
    headerTitle: String = "Recent Activities",
    showHeaderOpeningBalance: Boolean = false,
    showHeaderClosingBalance: Boolean = true,
    showItemOpeningBalance: Boolean = false,
    showItemClosingBalance: Boolean = true,
    showCurrencyBreakdown: Boolean = true,
    useNoteAsHeader: Boolean = false,
    showAccount: Boolean = true,
    showCategory: Boolean = true,
    showNote: Boolean = true,
    showTags: Boolean = true,
    showTimestamp: Boolean = true,
    showIcon: Boolean = true,
    tagColors: Map<String, Int> = emptyMap(),
    hasStickyHeader: Boolean = true
) {
    Column(
        modifier = modifier.fillMaxSize()
    ) {
// ... (omitting intermediate for brevity, will use full block in tool call)
        if (showHeader) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = AeonSize.D16.value, vertical = AeonSize.D12.value),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(AeonSize.D8.value)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.History,
                        contentDescription = null,
                        tint = AppColors.primary,
                        modifier = Modifier.size(AeonSize.D20.value)
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
                    contentPadding = PaddingValues(horizontal = AeonSize.D8.value, vertical = 0.dp),
                    modifier = Modifier.height(AeonSize.D32.value)
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

        val layers = LocalAeonHaze.current
        val itemHazeModifier = remember(layers, hasStickyHeader) {
            if (layers != null) {
                layers.states.take(3).foldIndexed(Modifier as Modifier) { index, mod, state ->
                    // Skip Overlay layer (index 1) if sticky headers are enabled,
                    // as content does not flow under the top app bar pills in that mode.
                    if (hasStickyHeader && index == 1) mod
                    else mod.aeonHazeSource(state)
                }
            } else Modifier
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
                    val headerContent: @Composable () -> Unit = {
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
                            showOpeningBalance = showHeaderOpeningBalance,
                            showClosingBalance = showHeaderClosingBalance,
                            showCurrencyBreakdown = showCurrencyBreakdown
                        )
                    }

                    if (hasStickyHeader) {
                        stickyHeader { headerContent() }
                    } else {
                        item { headerContent() }
                    }

                    itemsIndexed(
                        group.transactions,
                        key = { _, record -> record.id }) { index, record ->
                        val icon = getIconByName(record.categoryIcon)
                        val iconBg = Color(record.categoryColor)

                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .then(itemHazeModifier)
                        ) {
                        SwipeableTransactionItem(
                            record = record,
                            isRevealed = revealedCardIds.contains(record.id),
                            onEvent = onTransactionEvent
                        ) {
                            AeonTransactionItem(
                                category = record.categoryName,
                                account = record.accountName,
                                amount = formatAuto(
                                    record.amount,
                                    record.currency
                                ),
                                convertedAmount = if ((record.currency != baseCurrency) && (record.amountInBase != null)) {
                                    formatAuto(record.amountInBase, baseCurrency)
                                } else null,
                                balanceBefore = if (record is ConfirmedTransactionUiModel) formatAuto(
                                    record.item.balanceBefore,
                                    record.accountCurrency
                                ) else "",
                                balanceAfter = if (record is ConfirmedTransactionUiModel) formatAuto(
                                    record.item.balanceAfter,
                                    record.accountCurrency
                                ) else "",
                                type = if (record.amount.signum() >= 0) CashFlowType.INCOME else CashFlowType.EXPENSE,
                                timestamp = if (record is ConfirmedTransactionUiModel) com.osscabinet.ossWallet.ui.util.TimeUtils.formatDateTime(record.timestamp)
                                            else com.osscabinet.ossWallet.ui.util.TimeUtils.formatDate(record.timestamp),
                                note = record.note,
                                tags = record.tags,
                                icon = icon,
                                iconBg = iconBg,
                                useNoteAsHeader = useNoteAsHeader,
                                showOpeningBalance = showItemOpeningBalance && record is ConfirmedTransactionUiModel,
                                showClosingBalance = showItemClosingBalance && record is ConfirmedTransactionUiModel,
                                status = (record as? ConfirmedTransactionUiModel)?.status ?: com.osscabinet.ossWallet.domain.model.TransactionStatus.UNCLEARED,
                                showAccount = showAccount,
                                showCategory = showCategory,
                                showNote = showNote,
                                showTags = showTags,
                                showTimestamp = showTimestamp,
                                showIcon = showIcon,
                                showDividers = index < group.transactions.size - 1,
                                isPlanned = record is PlannedPaymentUiModel
                            ) { onTransactionEvent(TransactionItemEvent.Click(record.id)) }
                        }
                        } // hazeSource Box
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
        categoryId = 1L
    )
    val mockUiModel = ConfirmedTransactionUiModel(
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
