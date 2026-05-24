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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
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
    revealedCardIds: List<Long> = emptyList(),
    allTemplates: List<com.osscabinet.ossWallet.domain.model.Template> = emptyList(),
    allCategories: List<Category> = emptyList(),
    onAddClick: (com.osscabinet.ossWallet.domain.model.TransactionType?, Long?) -> Unit = { _, _ -> },
    expandableFabEnabled: Boolean = true
) {
    var isFabExpanded by androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf(false) }

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
            },
            floatingActionButton = {
                if (isTopLevel) {
                    com.osscabinet.ossWallet.ui.components.navigation.TransactionFabMenu(
                        isExpanded = isFabExpanded,
                        templates = allTemplates,
                        categories = allCategories,
                        onAddClick = { type, templateId ->
                            isFabExpanded = false
                            onAddClick(type, templateId)
                        }
                    )
                }
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
