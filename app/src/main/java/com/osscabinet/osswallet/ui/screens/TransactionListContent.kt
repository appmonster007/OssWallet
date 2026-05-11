package com.osscabinet.ossWallet.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.osscabinet.ossWallet.ui.components.transactions.AeonTransactionList
import com.osscabinet.ossWallet.ui.components.transactions.FilterSheetContent
import com.osscabinet.ossWallet.ui.components.transactions.FilterSheetDragHandle
import com.osscabinet.ossWallet.ui.components.transactions.TransactionListTopAppBar
import com.osscabinet.ossWallet.ui.components.transactions.TransactionListEmptyState
import com.osscabinet.ossWallet.domain.model.TransactionFilter
import com.osscabinet.ossWallet.domain.model.TransactionListState
import com.osscabinet.ossWallet.domain.model.TransactionWithBalance
import com.osscabinet.ossWallet.domain.model.Account
import com.osscabinet.ossWallet.domain.model.Category
import com.osscabinet.ossWallet.domain.model.Filter
import com.osscabinet.ossWallet.domain.model.timeline.TimelineRange
import com.osscabinet.ossWallet.data.local.TransactionSectionSettings
import com.osscabinet.ossWallet.ui.viewmodel.AnalyticsUiState
import androidx.compose.ui.tooling.preview.Preview
import com.osscabinet.ossWallet.domain.model.Transaction
import com.osscabinet.ossWallet.ui.theme.OssWalletTheme
import java.math.BigDecimal
import kotlinx.coroutines.launch

/**
 * Helper function to calculate 30% of screen height as dp padding.
 * This ensures the scroll ends when the last item is 30% above the bottom.
 */
@Composable
private fun calculateScrollBottomPadding(): Dp {
    val configuration = LocalConfiguration.current
    val density = LocalDensity.current
    val screenHeightPx = configuration.screenHeightDp * density.density
    val thirtyPercentPx = screenHeightPx * 0.3f
    return with(density) { thirtyPercentPx.toDp() }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TransactionListContent(
    listState: TransactionListState,
    accounts: List<Account>,
    categories: List<Category>,
    decimalPrecision: Int,
    analyticsState: AnalyticsUiState,
    filter: TransactionFilter,
    recordsSettings: TransactionSectionSettings,
    savedFilters: List<Filter> = emptyList(),
    onEditTransactionClick: (Long) -> Unit,
    onBackClick: () -> Unit,
    onMenuClick: () -> Unit = {},
    isTopLevel: Boolean = true,
    onUpdateFilter: (TransactionFilter) -> Unit,
    onUpdateTimelineRange: (TimelineRange) -> Unit,
    onUpdateRecordsSettings: (TransactionSectionSettings) -> Unit,
    onResetToInitial: (() -> Unit)? = null,
    revealedCardIds: List<Long> = emptyList(),
    onItemExpanded: (Long) -> Unit = {},
    onItemCollapsed: (Long) -> Unit = {},
    onStatusChange: (Long, com.osscabinet.ossWallet.domain.model.TransactionStatus) -> Unit = { _, _ -> },
    onClone: (Long) -> Unit = {},
    onDelete: (Long) -> Unit = {},
    onSplit: (Long) -> Unit = {},
    onFullSwipeRight: (Long) -> Unit = {},
    is24Hour: Boolean = false
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
        modifier = Modifier.background(Color.Transparent),
        sheetPeekHeight = 90.dp,
        sheetContainerColor = Color.Transparent,
        sheetContentColor = MaterialTheme.colorScheme.onSurface,
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
                backgroundColor = MaterialTheme.colorScheme.surfaceContainerHigh
            )
        },
        topBar = {
            TransactionListTopAppBar(
                filter = filter,
                recordsSettings = recordsSettings,
                onBackClick = onBackClick,
                onMenuClick = onMenuClick,
                isTopLevel = isTopLevel,
                onUpdateRecordsSettings = onUpdateRecordsSettings
            )
        },
        sheetContent = {
            FilterSheetContent(
                filter = filter,
                accounts = accounts,
                allCategories = categories,
                savedFilters = savedFilters,
                onUpdateTimelineRange = onUpdateTimelineRange,
                onApplyFilter = onUpdateFilter,
                onToggleExpand = {
                    scope.launch {
                        if (scaffoldState.bottomSheetState.currentValue == SheetValue.PartiallyExpanded) {
                            scaffoldState.bottomSheetState.expand()
                        } else {
                            scaffoldState.bottomSheetState.partialExpand()
                        }
                    }
                },
                onClose = { scope.launch { scaffoldState.bottomSheetState.partialExpand() } },
                backgroundColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                accountNames = accountNames,
                onResetToInitial = onResetToInitial
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = innerPadding.calculateTopPadding())
                .background(MaterialTheme.colorScheme.background)
        ) {
            if (listState.isEmpty) {
                TransactionListEmptyState()
            } else {
                AeonTransactionList(
                    listState = listState,
                    onTransactionClick = { onEditTransactionClick(it.id) },
                    contentPadding = PaddingValues(bottom = calculateScrollBottomPadding()),
                    precision = decimalPrecision,
                    useNoteAsHeader = recordsSettings.useNoteAsHeader,
                    showOpeningBalance = recordsSettings.showOpeningBalance,
                    showClosingBalance = recordsSettings.showClosingBalance,
                    showOnlyActiveCurrencies = recordsSettings.showOnlyActiveCurrencies,
                    baseCurrency = (analyticsState as? AnalyticsUiState.Success)?.selectedCurrency ?: "USD",
                    revealedCardIds = revealedCardIds,
                    onItemExpanded = onItemExpanded,
                    onItemCollapsed = onItemCollapsed,
                    onStatusChange = onStatusChange,
                    onClone = onClone,
                    onDelete = onDelete,
                    onSplit = onSplit,
                    onFullSwipeRight = onFullSwipeRight,
                    is24Hour = is24Hour
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
        structureType = "Personal",
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
            modifier = Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.surfaceVariant).padding(8.dp),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            Button(onClick = { theme = "Light" }) { Text("Light") }
            Button(onClick = { theme = "Dark" }) { Text("Dark") }
            Button(onClick = { theme = "Amoled" }) { Text("Amoled") }
        }
        OssWalletTheme(theme = theme) {
            TransactionListContent(
                listState = TransactionListState(isEmpty = false),
                accounts = listOf(sampleAccount),
                categories = listOf(sampleCategory),
                decimalPrecision = 2,
                analyticsState = AnalyticsUiState.Loading,
                filter = TransactionFilter(),
                recordsSettings = TransactionSectionSettings(),
                onEditTransactionClick = {},
                onBackClick = {},
                onUpdateFilter = {},
                onUpdateTimelineRange = { },
                onUpdateRecordsSettings = {}
            )
        }
    }
}
