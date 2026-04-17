package com.osscabinet.ossWallet.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.osscabinet.ossWallet.ui.components.AeonExpandableFab
import com.osscabinet.ossWallet.data.local.entity.TemplateEntity
import com.osscabinet.ossWallet.ui.components.transactions.AeonTransactionList
import com.osscabinet.ossWallet.ui.components.transactions.TransactionFilterSheet
import com.osscabinet.ossWallet.ui.components.transactions.TransactionListTopAppBar
import com.osscabinet.ossWallet.ui.components.transactions.TransactionListEmptyState
import com.osscabinet.ossWallet.domain.model.TransactionFilter
import com.osscabinet.ossWallet.domain.model.TransactionWithBalance
import com.osscabinet.ossWallet.domain.model.Account
import com.osscabinet.ossWallet.data.local.entity.CategoryEntity
import com.osscabinet.ossWallet.data.local.entity.FilterEntity
import com.osscabinet.ossWallet.data.local.TransactionSectionSettings
import com.osscabinet.ossWallet.ui.viewmodel.AnalyticsUiState
import androidx.compose.ui.tooling.preview.Preview
import com.osscabinet.ossWallet.domain.model.Transaction
import com.osscabinet.ossWallet.ui.theme.OssWalletTheme
import java.math.BigDecimal
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TransactionListContent(
    transactionsWithBalance: List<TransactionWithBalance>,
    accounts: List<Account>,
    accountMap: Map<String, String>,
    accountCurrencyMap: Map<String, String>,
    categories: List<CategoryEntity>,
    categoryMap: Map<Long, CategoryEntity>,
    decimalPrecision: Int,
    analyticsState: AnalyticsUiState,
    filter: TransactionFilter,
    recordsSettings: TransactionSectionSettings,
    savedFilters: List<FilterEntity> = emptyList(),
    templates: List<TemplateEntity> = emptyList(),
    onAddTransactionClick: (Int?, String?) -> Unit,
    onEditTransactionClick: (Long) -> Unit,
    onBackClick: () -> Unit,
    onAccountDetailClick: (String) -> Unit,
    onUpdateFilter: (TransactionFilter) -> Unit,
    onSetDateRange: (Long?, Long?) -> Unit,
    onUpdateRecordsSettings: (TransactionSectionSettings) -> Unit
) {
    val scaffoldState = rememberBottomSheetScaffoldState(
        bottomSheetState = rememberStandardBottomSheetState(
            initialValue = SheetValue.PartiallyExpanded,
            skipHiddenState = true
        )
    )
    val scope = rememberCoroutineScope()

    Box(modifier = Modifier.fillMaxSize()) {
        BottomSheetScaffold(
            scaffoldState = scaffoldState,
            modifier = Modifier.background(Color.Transparent),
            sheetPeekHeight = 90.dp,
            sheetContainerColor = Color.Transparent,
            sheetContentColor = MaterialTheme.colorScheme.onSurface,
            sheetTonalElevation = 0.dp,
            sheetShadowElevation = 0.dp,
            sheetShape = RectangleShape,
            sheetDragHandle = null,
            topBar = {
                TransactionListTopAppBar(
                    filter = filter,
                    recordsSettings = recordsSettings,
                    onBackClick = onBackClick,
                    onUpdateRecordsSettings = onUpdateRecordsSettings
                )
            },
            sheetContent = {
                TransactionFilterSheet(
                    filter = filter,
                    accounts = accounts,
                    categories = categories,
                    savedFilters = savedFilters,
                    onSetDateRange = onSetDateRange,
                    onUpdateFilter = onUpdateFilter,
                    onToggleExpand = {
                        scope.launch {
                            if (scaffoldState.bottomSheetState.currentValue == SheetValue.PartiallyExpanded) {
                                scaffoldState.bottomSheetState.expand()
                            } else {
                                scaffoldState.bottomSheetState.partialExpand()
                            }
                        }
                    },
                    onClose = {
                        scope.launch { scaffoldState.bottomSheetState.partialExpand() }
                    },
                    isExpanded = scaffoldState.bottomSheetState.targetValue == SheetValue.Expanded,
//                backgroundColor = MaterialTheme.colorScheme.surfaceContainer
//                backgroundColor = MaterialTheme.colorScheme.primaryContainer
                    backgroundColor = MaterialTheme.colorScheme.surfaceContainerHigh
                )
            }
        ) { innerPadding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(top = innerPadding.calculateTopPadding())
                    .background(MaterialTheme.colorScheme.background)
            ) {
                if (transactionsWithBalance.isEmpty()) {
                    TransactionListEmptyState()
                } else {
                    AeonTransactionList(
                        transactions = transactionsWithBalance,
                        accountMap = accountMap,
                        categoryMap = categoryMap,
                        onTransactionClick = { onEditTransactionClick(it.id) },
                        contentPadding = PaddingValues(
                            bottom = innerPadding.calculateBottomPadding() + 16.dp
                        ),
                        precision = decimalPrecision,
                        useNoteAsHeader = recordsSettings.useNoteAsHeader,
                        showOpeningBalance = recordsSettings.showOpeningBalance,
                        showClosingBalance = recordsSettings.showClosingBalance,
                        baseCurrency = analyticsState.selectedCurrency,
                        exchangeRates = analyticsState.exchangeRates,
                        accountCurrencyMap = accountCurrencyMap,
                        filterAccountId = if (filter.accountIds.size == 1) filter.accountIds.first() else null
                    )
                }
            }
        }

        AeonExpandableFab(
            onAddClick = onAddTransactionClick,
            templates = templates,
            fabPaddingBottom = 100.dp
        )
    }
}

@Preview(showBackground = true)
@Composable
fun TransactionListContentPreview() {
    val sampleAccount = Account(
        id = "1",
        name = "Checking",
        balance = BigDecimal("1500.00"),
        currencyCode = "USD",
        structureType = "Personal",
        colorHex = "#FF0000",
        icon = "account_balance"
    )

    val sampleCategory = CategoryEntity(
        id = 1L,
        name = "Groceries",
        iconName = "shopping_cart",
        colorHex = 0xFFFF0000.toInt()
    )

    val sampleTransaction = Transaction(
        id = 1L,
        title = "Weekly Groceries",
        note = "Weekly groceries from supermarket",
        amount = BigDecimal("85.50"),
        category = "Groceries",
        timestamp = System.currentTimeMillis(),
        currency = "USD",
        isIncome = false,
        accountId = "1",
        categoryId = 1L
    )

    val transactions = List(20) { i ->
        TransactionWithBalance(
            transaction = sampleTransaction.copy(
                id = i.toLong(),
                title = "Transaction $i",
                note = "Weekly expense \nNote $i",
                timestamp = System.currentTimeMillis() - (i * 86400000L) // One day apart
            ),
            balanceBefore = BigDecimal("1500.00"),
            balanceAfter = BigDecimal("1414.50")
        )
    }

    Column {
        var theme by remember { mutableStateOf("System") }
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
                transactionsWithBalance = transactions,
                accounts = listOf(sampleAccount),
                accountMap = mapOf("1" to "Checking"),
                accountCurrencyMap = mapOf("1" to "USD"),
                categories = listOf(sampleCategory),
                categoryMap = mapOf(1L to sampleCategory),
                decimalPrecision = 2,
                analyticsState = AnalyticsUiState(),
                filter = TransactionFilter(),
                recordsSettings = TransactionSectionSettings(),
                onAddTransactionClick = { _, _ -> },
                onEditTransactionClick = {},
                onBackClick = {},
                onAccountDetailClick = {},
                onUpdateFilter = {},
                onSetDateRange = { _, _ -> },
                onUpdateRecordsSettings = {}
            )
        }
    }
}
