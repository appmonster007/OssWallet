package com.osscabinet.ossWallet.ui.screens.transactions.list

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import com.osscabinet.ossWallet.ui.components.scrollBottomPadding
import com.osscabinet.ossWallet.domain.model.TransactionFilter
import com.osscabinet.ossWallet.domain.model.TransactionListState
import com.osscabinet.ossWallet.domain.model.Account
import com.osscabinet.ossWallet.domain.model.Category
import com.osscabinet.ossWallet.domain.model.Filter
import com.osscabinet.ossWallet.domain.model.settings.TransactionSectionSettings
import com.osscabinet.ossWallet.ui.components.transactions.*
import com.osscabinet.ossWallet.ui.screens.analytics.AnalyticsUiState
import com.osscabinet.ossWallet.domain.model.Transaction
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
                        showOpeningBalance = recordsSettings.showOpeningBalance,
                        showClosingBalance = recordsSettings.showClosingBalance,
                        showOnlyActiveCurrencies = recordsSettings.showOnlyActiveCurrencies,
                        showCurrencyBreakdown = recordsSettings.showCurrencyBreakdown,
                        baseCurrency = (analyticsState as? AnalyticsUiState.Success)?.selectedCurrency ?: "USD",
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
