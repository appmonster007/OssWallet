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
import com.osscabinet.ossWallet.domain.service.currency.CurrencyFormatter.formatAuto
import com.osscabinet.ossWallet.ui.components.getIconByName
import com.osscabinet.ossWallet.ui.theme.AppColors
import com.osscabinet.ossWallet.ui.theme.LocalAeonHaze
import dev.chrisbanes.haze.hazeSource

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
    showAccount: Boolean = true,
    showCategory: Boolean = true,
    showNote: Boolean = true,
    showTags: Boolean = true,
    showTimestamp: Boolean = true,
    showIcon: Boolean = true,
    baseCurrency: String,
    revealedCardIds: List<String> = emptyList()
) {
    val layers = LocalAeonHaze.current
    val itemHazeModifier = remember(layers) {
        if (layers != null) {
            // Register as source for standard depths: Surface(0), Overlay(1), Drawer(2)
            layers.states.take(3).fold(Modifier as Modifier) { mod, state ->
                mod.hazeSource(state)
            }
        } else Modifier
    }

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
                    showOpeningBalance = showHeaderOpeningBalance,
                    showClosingBalance = showHeaderClosingBalance,
                    showOnlyActiveCurrencies = showOnlyActiveCurrencies,
                    showCurrencyBreakdown = showCurrencyBreakdown
                )
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
