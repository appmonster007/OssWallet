package com.osscabinet.ossWallet.ui.components.transactions

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.HorizontalDivider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.osscabinet.ossWallet.domain.model.CashFlowType
import com.osscabinet.ossWallet.domain.model.ConfirmedTransactionUiModel
import com.osscabinet.ossWallet.domain.model.PlannedPaymentUiModel
import com.osscabinet.ossWallet.domain.model.TransactionListState
import com.osscabinet.ossWallet.ui.util.formatCurrency
import com.osscabinet.ossWallet.ui.components.getIconByName
import com.osscabinet.ossWallet.ui.components.nativeWithConvertedStrings
import com.osscabinet.ossWallet.ui.theme.AppColors

@OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class, ExperimentalLayoutApi::class)
@Composable
fun AeonTransactionList(
    listState: TransactionListState,
    onTransactionEvent: (TransactionItemEvent) -> Unit,
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues(0.dp),
    useNoteAsHeader: Boolean = false,
    showHeaderOpeningBalance: Boolean = false,
    showHeaderClosingBalance: Boolean = true,
    showItemOpeningBalance: Boolean = false,
    showItemClosingBalance: Boolean = true,
    showOnlyActiveCurrencies: Boolean = false,
    showCurrencyBreakdown: Boolean = true,
    allTxnBreakdown: Boolean = true,
    showAccount: Boolean = true,
    showCategory: Boolean = true,
    showNote: Boolean = true,
    showTags: Boolean = true,
    showTimestamp: Boolean = true,
    showIcon: Boolean = true,
    baseCurrency: String,
    revealedCardIds: List<String> = emptyList(),
    hasStickyHeader: Boolean = true
) {
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = contentPadding,
        verticalArrangement = Arrangement.spacedBy(0.dp)
    ) {
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
                    showOpeningBalance = if (showCurrencyBreakdown && !allTxnBreakdown) false else showHeaderOpeningBalance,
                    showClosingBalance = if (showCurrencyBreakdown && !allTxnBreakdown) false else showHeaderClosingBalance,
                    showOnlyActiveCurrencies = !allTxnBreakdown || showOnlyActiveCurrencies,
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

                Box(modifier = Modifier.fillMaxWidth()) {
                    SwipeableTransactionItem(
                        record = record,
                        isRevealed = revealedCardIds.contains(record.id),
                        onEvent = onTransactionEvent
                    ) {
                        val (amountText, convertedText) = nativeWithConvertedStrings(
                            amount = record.amount,
                            nativeCurrency = record.currency,
                            targetCurrency = baseCurrency,
                            convertedAmount = record.amountInBase,
                        )
                        AeonTransactionItem(
                            category = record.categoryName,
                            account = record.accountName,
                            amount = amountText,
                            convertedAmount = convertedText,
                            balanceBefore = if (record is ConfirmedTransactionUiModel) formatCurrency(
                                record.item.balanceBefore,
                                record.accountCurrency
                            ) else "",
                            balanceAfter = if (record is ConfirmedTransactionUiModel) formatCurrency(
                                record.item.balanceAfter,
                                record.accountCurrency
                            ) else "",
                            type = if (record.amount.signum() >= 0) CashFlowType.INCOME else CashFlowType.EXPENSE,
                            timestamp = if (record is ConfirmedTransactionUiModel) com.osscabinet.ossWallet.ui.util.TimeUtils.formatDateTime(
                                record.timestamp
                            )
                            else com.osscabinet.ossWallet.ui.util.TimeUtils.formatDate(record.timestamp),
                            note = record.note,
                            tags = record.tags,
                            icon = icon,
                            iconBg = iconBg,
                            useNoteAsHeader = useNoteAsHeader,
                            showOpeningBalance = showItemOpeningBalance && record is ConfirmedTransactionUiModel,
                            showClosingBalance = showItemClosingBalance && record is ConfirmedTransactionUiModel,
                            status = (record as? ConfirmedTransactionUiModel)?.status
                                ?: com.osscabinet.ossWallet.domain.model.TransactionStatus.UNCLEARED,
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
        item {
            HorizontalDivider(
                modifier = Modifier.padding(horizontal = 16.dp),
                thickness = 0.5.dp,
                color = AppColors.divider.copy(alpha = 0.5f)
            )
        }
    }
}
