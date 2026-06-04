package com.osscabinet.ossWallet.ui.screens.transactions.list

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.osscabinet.ossWallet.domain.model.Account
import com.osscabinet.ossWallet.domain.model.Category
import com.osscabinet.ossWallet.domain.model.ConfirmedTransactionUiModel
import com.osscabinet.ossWallet.domain.model.Filter
import com.osscabinet.ossWallet.domain.model.Transaction
import com.osscabinet.ossWallet.domain.model.TransactionFilter
import com.osscabinet.ossWallet.domain.model.TransactionGroup
import com.osscabinet.ossWallet.domain.model.TransactionListState
import com.osscabinet.ossWallet.domain.model.TransactionType
import com.osscabinet.ossWallet.domain.model.TransactionWithBalance
import com.osscabinet.ossWallet.domain.model.settings.TransactionSectionSettings
import com.osscabinet.ossWallet.domain.service.timeline.TimelineService
import com.osscabinet.ossWallet.ui.components.AeonHazeProvider
import com.osscabinet.ossWallet.ui.components.scrollBottomPadding
import com.osscabinet.ossWallet.ui.components.transactions.AeonTransactionList
import com.osscabinet.ossWallet.ui.components.transactions.FilterEvent
import com.osscabinet.ossWallet.ui.components.transactions.TransactionFilterPill
import com.osscabinet.ossWallet.ui.components.transactions.TransactionItemEvent
import com.osscabinet.ossWallet.ui.components.transactions.TransactionListEmptyState
import com.osscabinet.ossWallet.ui.components.transactions.TransactionListTopAppBar
import com.osscabinet.ossWallet.ui.screens.analytics.AnalyticsUiState
import com.osscabinet.ossWallet.ui.theme.AeonSize
import com.osscabinet.ossWallet.ui.theme.OssWalletTheme
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import java.math.BigDecimal

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TransactionListContent(
    listState: TransactionListState,
    onSearchTransactions: (String) -> Flow<List<Transaction>>,
    accounts: List<Account>,
    categories: List<Category>,
    allTags: List<com.osscabinet.ossWallet.domain.model.Tag> = emptyList(),
    analyticsState: AnalyticsUiState,
    filter: TransactionFilter,
    recordsSettings: TransactionSectionSettings,
    savedFilters: List<Filter> = emptyList(),
    onTransactionEvent: (TransactionItemEvent) -> Unit,
    onBackClick: () -> Unit,
    onMenuClick: () -> Unit = {},
    onSaveFilterClick: (TransactionFilter) -> Unit = {},
    isTopLevel: Boolean = true,
    onFilterEvent: (FilterEvent) -> Unit,
    onAddTag: (String, Int) -> Unit = { _, _ -> },
    revealedCardIds: List<String> = emptyList()
) {
    val pillHeight = AeonSize.D64.value

    AeonHazeProvider { _ ->
        Box(modifier = Modifier.fillMaxSize()) {
            // 1. Transaction List (Layer 1)
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .statusBarsPadding() // Avoid bleeding into status bar icons
                    .then(
                        if (recordsSettings.stickyHeaders) {
                            Modifier.padding(top = pillHeight)
                        } else {
                            Modifier
                        }
                    )
            ) {
                if (listState.isEmpty) {
                    TransactionListEmptyState(
                        isFutureExcluded = filter.timelineRange.excludeFuture,
                        onIncludeFutureClick = {
                            onFilterEvent(
                                FilterEvent.Apply(
                                    filter.copy(
                                        timelineRange = filter.timelineRange.withExcludeFuture(false)
                                    )
                                )
                            )
                        },
                        modifier = if (!recordsSettings.stickyHeaders) {
                            Modifier.padding(top = pillHeight)
                        } else Modifier
                    )
                } else {
                    AeonTransactionList(
                        listState = listState,
                        onTransactionEvent = onTransactionEvent,
                        contentPadding = PaddingValues(
                            top = if (recordsSettings.stickyHeaders) 0.dp else pillHeight,
                            bottom = scrollBottomPadding()
                        ),
                        useNoteAsHeader = recordsSettings.useNoteAsHeader,
                        showHeaderOpeningBalance = recordsSettings.showHeaderOpeningBalance,
                        showHeaderClosingBalance = recordsSettings.showHeaderClosingBalance,
                        showItemOpeningBalance = recordsSettings.showItemOpeningBalance,
                        showItemClosingBalance = recordsSettings.showItemClosingBalance,
                        showOnlyActiveCurrencies = recordsSettings.showOnlyActiveCurrencies,
                        showCurrencyBreakdown = recordsSettings.showCurrencyBreakdown,
                        showAccount = recordsSettings.showAccount,
                        showCategory = recordsSettings.showCategory,
                        showNote = recordsSettings.showNote,
                        showTags = recordsSettings.showTags,
                        showTimestamp = recordsSettings.showTimestamp,
                        showIcon = recordsSettings.showIcon,
                        baseCurrency = (analyticsState as? AnalyticsUiState.Success)?.selectedCurrency
                            ?: "USD",
                        revealedCardIds = revealedCardIds,
                        hasStickyHeader = recordsSettings.stickyHeaders,
                    )
                }
            }

            // 2. Top Bar (Layer 3 - Floating Pills)
            TransactionListTopAppBar(
                filter = filter,
                recordsSettings = recordsSettings,
                onBackClick = onBackClick,
                onMenuClick = onMenuClick,
                isTopLevel = isTopLevel,
                onUpdateRecordsSettings = { onFilterEvent(FilterEvent.UpdateSettings(it)) }
            )
// ...

            // 3. Filter Pill (Layer 4 - if applicable)
            TransactionFilterPill(
                filter = filter,
                accounts = accounts,
                categories = categories,
                allTags = allTags,
                onSearchTransactions = onSearchTransactions,
                savedFilters = savedFilters,
                onFilterEvent = onFilterEvent,
                onSaveFilterClick = onSaveFilterClick,
                onAddTag = onAddTag,
                modifier = Modifier.fillMaxSize()
            )
        }
    }
}

@Preview(showBackground = true, name = "Empty State")
@Composable
fun TransactionListScreenEmptyPreview() {
    OssWalletTheme {
        TransactionListContent(
            listState = TransactionListState(isEmpty = true),
            onSearchTransactions = { flowOf(emptyList()) },
            accounts = emptyList(),
            categories = emptyList(),
            analyticsState = AnalyticsUiState.Loading,
            filter = TransactionFilter(),
            recordsSettings = TransactionSectionSettings(),
            onTransactionEvent = {},
            onBackClick = {},
            onFilterEvent = {}
        )
    }
}

@Preview(showBackground = true, name = "Populated List")
@Composable
fun TransactionListScreenPopulatedPreview() {
    val dummyTransaction = Transaction(
        id = 1,
        name = "Groceries",
        amount = BigDecimal("-50.00"),
        timestamp = System.currentTimeMillis(),
        currency = "USD",
        transactionType = TransactionType.EXPENSE,
        accountId = "acc1",
        categoryId = 1
    )
    
    val dummyRecord = ConfirmedTransactionUiModel(
        item = TransactionWithBalance(dummyTransaction, BigDecimal("1000.00"), BigDecimal("950.00")),
        accountName = "Main Wallet",
        accountCurrency = "USD",
        categoryName = "Food",
        categoryIcon = "ShoppingCart",
        categoryColor = 0xFF4CAF50.toInt()
    )

    val headerData = TimelineService.GroupHeaderData(
        title = "Today",
        subtitle = "March 15, 2024",
        incomeByCurrency = emptyMap(),
        expenseByCurrency = mapOf("USD" to BigDecimal("50.00")),
        openingBalancesByCurrency = mapOf("USD" to BigDecimal("1000.00")),
        closingBalancesByCurrency = mapOf("USD" to BigDecimal("950.00")),
        allCurrencies = listOf("USD"),
        incomeBase = BigDecimal.ZERO,
        expenseBase = BigDecimal("50.00"),
        netBase = BigDecimal("-50.00"),
        totalOpeningBalanceBase = BigDecimal("1000.00"),
        totalClosingBalanceBase = BigDecimal("950.00")
    )

    val listState = TransactionListState(
        groups = listOf(
            TransactionGroup(
                key = "today",
                headerData = headerData,
                transactions = listOf(dummyRecord)
            )
        ),
        isEmpty = false
    )

    OssWalletTheme {
        TransactionListContent(
            listState = listState,
            onSearchTransactions = { flowOf(emptyList()) },
            accounts = emptyList(),
            categories = emptyList(),
            analyticsState = AnalyticsUiState.Loading,
            filter = TransactionFilter(),
            recordsSettings = TransactionSectionSettings(
                showHeaderOpeningBalance = true,
                showHeaderClosingBalance = true
            ),
            onTransactionEvent = {},
            onBackClick = {},
            onFilterEvent = {}
        )
    }
}
