package com.osscabinet.ossWallet.ui.screens.transactions.list

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import com.osscabinet.ossWallet.ui.components.scrollBottomPadding
import androidx.compose.ui.unit.dp
import com.osscabinet.ossWallet.ui.components.transactions.AeonTransactionList
import com.osscabinet.ossWallet.ui.components.transactions.FilterEvent
import com.osscabinet.ossWallet.ui.components.transactions.TransactionFilterPill
import com.osscabinet.ossWallet.ui.components.transactions.TransactionListTopAppBar
import com.osscabinet.ossWallet.ui.components.transactions.TransactionListEmptyState
import com.osscabinet.ossWallet.domain.model.TransactionFilter
import com.osscabinet.ossWallet.domain.model.TransactionListState
import com.osscabinet.ossWallet.domain.model.Account
import com.osscabinet.ossWallet.domain.model.AccountStructureType
import com.osscabinet.ossWallet.domain.model.Category
import com.osscabinet.ossWallet.domain.model.Filter
import com.osscabinet.ossWallet.data.local.TransactionSectionSettings
import com.osscabinet.ossWallet.ui.components.transactions.TransactionItemEvent
import com.osscabinet.ossWallet.ui.screens.analytics.AnalyticsUiState
import androidx.compose.ui.tooling.preview.Preview
import com.osscabinet.ossWallet.domain.model.Transaction
import com.osscabinet.ossWallet.ui.theme.OssWalletTheme
import com.osscabinet.ossWallet.ui.theme.AppColors

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TransactionListContent(
    listState: TransactionListState,
    allTransactions: List<Transaction>,
    accounts: List<Account>,
    categories: List<Category>,
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
                        revealedCardIds = revealedCardIds
                    )
                }
            }
        }

        TransactionFilterPill(
            filter = filter,
            accounts = accounts,
            categories = categories,
            savedFilters = savedFilters,
            onFilterEvent = onFilterEvent,
            onSaveFilterClick = onSaveFilterClick,
            modifier = Modifier.fillMaxSize()
        )
    }
}

@Preview(showBackground = true)
@Composable
fun TransactionListContentPreview() {
    val sampleAccount = Account(
        id = "1",
        name = "Checking",
        currencyCode = "USD",
        structureType = AccountStructureType.CHECKING,
        colorHex = "#FF0000",
        icon = "account_balance",
        allowedCurrencies = listOf("USD")
    )

    val sampleCategory = Category(
        id = 1L,
        name = "Groceries",
        iconName = "shopping_cart",
        colorHex = 0xFFFF0000.toInt(),
        isDefault = true,
        parentId = null
    )

    var theme by remember { mutableStateOf("System") }
    OssWalletTheme(theme = theme) {
        Column {
            Row(
                modifier = Modifier.fillMaxWidth().background(AppColors.insetSurface).padding(8.dp),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                Button(onClick = { theme = "Light" }) { Text("Light") }
                Button(onClick = { theme = "Dark" }) { Text("Dark") }
                Button(onClick = { theme = "Amoled" }) { Text("Amoled") }
            }
            TransactionListContent(
                listState = TransactionListState(isEmpty = false),
                allTransactions = emptyList(),
                accounts = listOf(sampleAccount),
                categories = listOf(sampleCategory),
                analyticsState = AnalyticsUiState.Loading,
                filter = TransactionFilter(),
                recordsSettings = TransactionSectionSettings(),
                onTransactionEvent = {},
                onBackClick = {},
                onFilterEvent = {}
            )
        }
    }
}
