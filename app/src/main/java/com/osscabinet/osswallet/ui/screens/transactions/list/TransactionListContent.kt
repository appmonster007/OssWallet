package com.osscabinet.ossWallet.ui.screens.transactions.list

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.osscabinet.ossWallet.domain.model.Account
import com.osscabinet.ossWallet.domain.model.Category
import com.osscabinet.ossWallet.domain.model.Filter
import com.osscabinet.ossWallet.domain.model.Transaction
import com.osscabinet.ossWallet.domain.model.TransactionFilter
import com.osscabinet.ossWallet.domain.model.TransactionListState
import com.osscabinet.ossWallet.domain.model.settings.TransactionSectionSettings
import com.osscabinet.ossWallet.ui.components.scrollBottomPadding
import com.osscabinet.ossWallet.ui.components.transactions.AeonTransactionList
import com.osscabinet.ossWallet.ui.components.transactions.FilterEvent
import com.osscabinet.ossWallet.ui.components.transactions.TransactionFilterPill
import com.osscabinet.ossWallet.ui.components.transactions.TransactionItemEvent
import com.osscabinet.ossWallet.ui.components.transactions.TransactionListEmptyState
import com.osscabinet.ossWallet.ui.components.transactions.TransactionListTopAppBar
import com.osscabinet.ossWallet.ui.screens.analytics.AnalyticsUiState
import com.osscabinet.ossWallet.ui.theme.AppColors
import kotlinx.coroutines.flow.Flow

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
    revealedCardIds: List<Long> = emptyList()
) {
    Box(modifier = Modifier.fillMaxSize()) {
        Scaffold(
            modifier = Modifier.background(AppColors.transparent),
            topBar = {
                TransactionListTopAppBar(
                    filter = filter,
                    recordsSettings = recordsSettings,
                    onBackClick = onBackClick,
                    onMenuClick = onMenuClick,
                    isTopLevel = isTopLevel,
                    onUpdateRecordsSettings = { onFilterEvent(FilterEvent.UpdateSettings(it)) }
                )
            }
        ) { innerPadding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .background(AppColors.screenBackground)
            ) {
                if (listState.isEmpty) {
                    TransactionListEmptyState()
                } else {
                    AeonTransactionList(
                        listState = listState,
                        onTransactionEvent = onTransactionEvent,
                        contentPadding = PaddingValues(bottom = scrollBottomPadding()),
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
                    )
                }
            }
        }

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
