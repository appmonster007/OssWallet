package com.osscabinet.ossWallet.ui.screens.transactions.list

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.osscabinet.ossWallet.ui.components.scrollBottomPadding
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.osscabinet.ossWallet.ui.components.transactions.AeonTransactionList
import com.osscabinet.ossWallet.ui.components.transactions.FilterEvent
import com.osscabinet.ossWallet.ui.components.transactions.FilterSheetContent
import com.osscabinet.ossWallet.ui.components.transactions.FilterSheetDragHandle
import com.osscabinet.ossWallet.ui.components.transactions.TransactionListTopAppBar
import com.osscabinet.ossWallet.ui.components.transactions.TransactionListEmptyState
import com.osscabinet.ossWallet.domain.model.TransactionFilter
import com.osscabinet.ossWallet.domain.model.TransactionListState
import com.osscabinet.ossWallet.domain.model.TransactionWithBalance
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
import java.math.BigDecimal
import kotlinx.coroutines.launch
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
    val accountNames = remember(accounts) { accounts.associate { it.id to it.name } }
    val scaffoldState = rememberBottomSheetScaffoldState(
        bottomSheetState = rememberStandardBottomSheetState(
            initialValue = SheetValue.PartiallyExpanded,
            skipHiddenState = true
        )
    )
    val scope = rememberCoroutineScope()

    BottomSheetScaffold(
        scaffoldState = scaffoldState,
        modifier = Modifier.background(AppColors.transparent),
        sheetPeekHeight = 110.dp,
        sheetContainerColor = AppColors.transparent,
        sheetContentColor = AppColors.contentPrimary,
        sheetTonalElevation = 0.dp,
        sheetShadowElevation = 0.dp,
        sheetShape = RectangleShape,
        sheetDragHandle = {
            FilterSheetDragHandle(
                isExpanded = scaffoldState.bottomSheetState.targetValue == SheetValue.Expanded,
                onClick = {
                    scope.launch {
                        if (scaffoldState.bottomSheetState.currentValue == SheetValue.PartiallyExpanded) {
                            scaffoldState.bottomSheetState.expand()
                        } else {
                            scaffoldState.bottomSheetState.partialExpand()
                        }
                    }
                },
                backgroundColor = AppColors.insetSurface
            )
        },
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
        sheetContent = {
            FilterSheetContent(
                filter = filter,
                accounts = accounts,
                allCategories = categories,
                allTransactions = allTransactions,
                savedFilters = savedFilters,
                onFilterEvent = onFilterEvent,
                onSaveFilterClick = onSaveFilterClick,
                onClose = { scope.launch { scaffoldState.bottomSheetState.partialExpand() } },
                backgroundColor = AppColors.insetSurface,
                accountNames = accountNames,
            )
        }
    ) { innerPadding ->
        Box(modifier = Modifier.fillMaxSize()) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(top = innerPadding.calculateTopPadding())
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
                        baseCurrency = (analyticsState as? AnalyticsUiState.Success)?.selectedCurrency ?: "USD",
                        revealedCardIds = revealedCardIds
                    )
                }
            }

            // Scrim Overlay: Blocks interaction and dims background when filter sheet is open
            val targetValue = scaffoldState.bottomSheetState.targetValue
            val currentValue = scaffoldState.bottomSheetState.currentValue
            val isExpanded = targetValue == SheetValue.Expanded || currentValue == SheetValue.Expanded
            
            val scrimAlpha by animateFloatAsState(
                targetValue = if (targetValue == SheetValue.Expanded) 0.5f else 0f,
                label = "filter_scrim_alpha"
            )

            if (scrimAlpha > 0f || isExpanded) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = scrimAlpha))
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null
                        ) {
                            scope.launch { scaffoldState.bottomSheetState.partialExpand() }
                        }
                )
            }
        }
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
    Column {
        Row(
            modifier = Modifier.fillMaxWidth().background(AppColors.insetSurface).padding(8.dp),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            Button(onClick = { theme = "Light" }) { Text("Light") }
            Button(onClick = { theme = "Dark" }) { Text("Dark") }
            Button(onClick = { theme = "Amoled" }) { Text("Amoled") }
        }
        OssWalletTheme(theme = theme) {
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
                onFilterEvent = {},
            )
        }
    }
}
