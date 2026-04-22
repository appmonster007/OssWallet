package com.osscabinet.ossWallet.ui.accountdetail

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.*
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.produceState
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import java.math.BigDecimal
import com.osscabinet.ossWallet.domain.model.TransactionFilter
import com.osscabinet.ossWallet.domain.model.TransactionWithBalance
import com.osscabinet.ossWallet.ui.viewmodel.AccountViewModel
import com.osscabinet.ossWallet.ui.viewmodel.TransactionViewModel
import com.osscabinet.ossWallet.ui.viewmodel.SettingsViewModel
import com.osscabinet.ossWallet.ui.viewmodel.AnalyticsViewModel
import com.osscabinet.ossWallet.ui.viewmodel.AccountDetailViewModel
import com.osscabinet.ossWallet.ui.viewmodel.CardManagerViewModel
import com.osscabinet.ossWallet.ui.components.*
import com.osscabinet.ossWallet.ui.components.AeonExpandableFab
import com.osscabinet.ossWallet.ui.components.NavigationPill
import com.osscabinet.ossWallet.ui.accountdetail.components.AccountHeroCard
import com.osscabinet.ossWallet.ui.components.transactions.AeonTransactionList
import com.osscabinet.ossWallet.ui.components.transactions.TransactionListEmptyState
import com.osscabinet.ossWallet.ui.components.transactions.TransactionFilterSheet
import com.osscabinet.ossWallet.ui.viewmodel.TemplateViewModel
import com.osscabinet.ossWallet.ui.viewmodel.AccountDetailUiState
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
fun AccountDetailScreen(
    accountId: String,
    accountViewModel: AccountViewModel,
    transactionViewModel: TransactionViewModel,
    settingsViewModel: SettingsViewModel,
    analyticsViewModel: AnalyticsViewModel,
    accountDetailViewModel: AccountDetailViewModel,
    cardManagerViewModel: CardManagerViewModel,
    templateViewModel: TemplateViewModel,
    onBackClick: () -> Unit,
    onEditAccountClick: (String) -> Unit,
    onEditTransactionClick: (Long) -> Unit,
    onAddTransactionClick: (String, Int?, String?) -> Unit,
    onRecordsClick: (com.osscabinet.ossWallet.ui.navigation.RecordsFilterContext) -> Unit,
    onRecordsClickWithFilterStack: (com.osscabinet.ossWallet.domain.model.filter.FilterStack) -> Unit = {}
) {
    val currentTheme by settingsViewModel.theme.collectAsStateWithLifecycle(initialValue = "System")
    val cardAnimationEnabled by settingsViewModel.cardAnimationEnabled.collectAsStateWithLifecycle(initialValue = true)

    val uiState by accountDetailViewModel.uiState.collectAsStateWithLifecycle()

    val categories by transactionViewModel.categories.collectAsStateWithLifecycle()
    val categoryMap = remember(categories) { categories.associateBy { it.id } }
    val allTemplates by templateViewModel.templates.collectAsStateWithLifecycle()

    var navTriggered by remember { mutableStateOf(false) }
    
    // Reset navTriggered when returning to this screen
    androidx.lifecycle.compose.LifecycleEventEffect(androidx.lifecycle.Lifecycle.Event.ON_RESUME) {
        navTriggered = false
    }

    val navigate: (() -> Unit) -> Unit = { action ->
        if (!navTriggered) {
            navTriggered = true
            action()
        }
    }

    val pageCount = 2
    val pagerState = rememberPagerState(initialPage = 0) { pageCount }
    val tabs: List<Pair<ImageVector, String>> = listOf(
        Icons.Rounded.Dashboard to "Overview",
        Icons.Rounded.Receipt to "Transactions"
    )

    var showSettingsDialog by remember { mutableStateOf(false) }
    var showAddCardDialog by remember { mutableStateOf(false) }

    val accountDetailCardsConfig by cardManagerViewModel.accountDetailCardsConfig.collectAsStateWithLifecycle()

    if (showSettingsDialog && uiState is AccountDetailUiState.Success) {
        val state = uiState as AccountDetailUiState.Success
        AlertDialog(
            onDismissRequest = { showSettingsDialog = false },
            title = { Text("Transaction Settings") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Show Planned Payments", style = MaterialTheme.typography.bodyLarge)
                            Text("Include future transactions", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.outline)
                        }
                        Switch(
                            checked = state.showPlannedPayments,
                            onCheckedChange = { accountDetailViewModel.updateShowPlannedPayments(it) }
                        )
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Use Note as Header", style = MaterialTheme.typography.bodyLarge)
                            Text("Primary header will show transaction note", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.outline)
                        }
                        Switch(
                            checked = state.useNoteAsHeader,
                            onCheckedChange = { accountDetailViewModel.updateUseNoteAsHeader(it) }
                        )
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showSettingsDialog = false }) {
                    Text("Close")
                }
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text("Account Details", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold))
                },
                navigationIcon = {
                    IconButton(onClick = { navigate { onBackClick() } }) {
                        Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = { navigate { onEditAccountClick(accountId) } }) {
                        Icon(Icons.Rounded.Edit, contentDescription = "Edit")
                    }
                    IconButton(onClick = { showSettingsDialog = true }) {
                        Icon(Icons.Rounded.Settings, contentDescription = "Settings")
                    }
                    IconButton(onClick = { showAddCardDialog = true }) {
                        Icon(Icons.Rounded.Add, contentDescription = "Add Card")
                    }
                }
            )
        }
    ) { screenPadding ->
        Box(modifier = Modifier.fillMaxSize()) {
            when (val state = uiState) {
                is AccountDetailUiState.Loading -> {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
                    }
                }
                is AccountDetailUiState.Error -> {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text(state.message)
                    }
                }
                is AccountDetailUiState.Success -> {
                    HorizontalPager(
                        state = pagerState,
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(screenPadding),
                        verticalAlignment = Alignment.Top
                    ) { page ->
                        when (page) {
                            0 -> {
                                Box(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .background(MaterialTheme.colorScheme.background)
                                ) {
                                    DynamicAccountDetailCards(
                                        state = state,
                                        cardsConfig = accountDetailCardsConfig,
                                        cardManagerViewModel = cardManagerViewModel,
                                        accountDetailViewModel = accountDetailViewModel,
                                        onRecordsClick = onRecordsClick,
                                        accountId = accountId,
                                        currentTheme = currentTheme,
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .padding(horizontal = 16.dp),
                                        screenPadding = PaddingValues(),
                                        showAddCardDialog = showAddCardDialog,
                                        onShowAddCardDialogChange = { showAddCardDialog = it },
                                        cardAnimationEnabled = cardAnimationEnabled
                                    )
                                }
                            }
                            1 -> {
                        // Transactions tab content (same as original)
                        val transactionsWithBalanceList = remember(state.cashFlowTransactions, state.account.balance) {
                            // Calculate running balance from transactions
                            // Start from zero and apply transactions in reverse order
                            var currentBalance = BigDecimal.ZERO
                            state.cashFlowTransactions.sortedByDescending { it.timestamp }.map { tx ->
                                val balanceAfter = currentBalance
                                val balanceBefore = if (tx.isIncome) currentBalance.subtract(tx.amount) else currentBalance.add(tx.amount)
                                currentBalance = balanceBefore
                                TransactionWithBalance(tx, balanceBefore, balanceAfter)
                            }.reversed()
                        }.sortedByDescending { it.transaction.timestamp }

                        if (transactionsWithBalanceList.isEmpty()) {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize(),
                                contentAlignment = Alignment.Center
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Icon(
                                        Icons.Rounded.History,
                                        contentDescription = null,
                                        modifier = Modifier.size(64.dp),
                                        tint = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)
                                    )
                                    Spacer(modifier = Modifier.height(16.dp))
                                    Text(
                                        "No transactions yet",
                                        style = MaterialTheme.typography.bodyLarge,
                                        color = MaterialTheme.colorScheme.outline
                                    )
                                }
                            }
                        } else {
                            val filter by transactionViewModel.accountDetailFilter.collectAsStateWithLifecycle()
                            val savedFilters by transactionViewModel.savedFilters.collectAsStateWithLifecycle()
                            val accounts by accountViewModel.accounts.collectAsStateWithLifecycle()
                            val accountNamesMap = remember(accounts) { accounts.associate { it.id to it.name } }
                            val accountCurrencyMap = remember(accounts) { accounts.associate { it.id to it.currencyCode } }
                            val categoriesList by transactionViewModel.categories.collectAsStateWithLifecycle()
                            val analyticsState by analyticsViewModel.uiState.collectAsStateWithLifecycle()
                            val scope = rememberCoroutineScope()
                            val scaffoldState = rememberBottomSheetScaffoldState(
                                bottomSheetState = rememberStandardBottomSheetState(
                                    initialValue = SheetValue.PartiallyExpanded,
                                    skipHiddenState = true
                                )
                            )

                            // Apply account filter automatically if not already set specifically for this account
                            LaunchedEffect(accountId) {
                                if (!filter.accountIds.contains(accountId)) {
                                    transactionViewModel.updateAccountDetailFilter(filter.copy(accountIds = listOf(accountId)))
                                }
                            }

                            // Reset filters when leaving the transactions tab or the screen
                            DisposableEffect(Unit) {
                                onDispose {
                                    transactionViewModel.clearAccountDetailFilters()
                                }
                            }

                            BottomSheetScaffold(
                                scaffoldState = scaffoldState,
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(top = screenPadding.calculateTopPadding())
                                    .background(Color.Transparent),
                                sheetPeekHeight = 90.dp,
                                sheetContainerColor = Color.Transparent,
                                sheetContentColor = MaterialTheme.colorScheme.onSurface,
                                sheetTonalElevation = 0.dp,
                                sheetShadowElevation = 0.dp,
                                sheetShape = RectangleShape,
                                sheetDragHandle = null,
                                sheetSwipeEnabled = true,
                                sheetContent = {
                                    TransactionFilterSheet(
                                        filter = filter,
                                        accounts = accounts,
                                        categories = categoriesList,
                                        savedFilters = savedFilters,
                                        onSetDateRange = { start, end -> transactionViewModel.setAccountDetailDateRangeFilter(start, end) },
                                        onUpdateFilter = { updatedFilter ->
                                            // Ensure we always stay filtered by this account
                                            transactionViewModel.updateAccountDetailFilter(updatedFilter.copy(accountIds = listOf(accountId)))
                                        },
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
                                        backgroundColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                                        showAccountFilter = false
                                    )
                                }
                            ) { sheetPadding ->
                                Column(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .padding(top = sheetPadding.calculateTopPadding())
                                ) {
                                    val filteredTransactions = remember(transactionsWithBalanceList, filter) {
                                        transactionsWithBalanceList.filter { item ->
                                            val tx = item.transaction
                                            
                                            // Manual in-memory filtering for account detail screen
                                            val queryNote = filter.queryNote
                                            val matchesNote = queryNote == null || 
                                                (if (filter.isNoteRegex) {
                                                    try { Regex(queryNote, RegexOption.IGNORE_CASE).containsMatchIn(tx.note) } catch(e: Exception) { false }
                                                } else tx.note.contains(queryNote, ignoreCase = true))
                                            
                                            val queryPayee = filter.queryPayee
                                            val matchesPayee = queryPayee == null || 
                                                (if (filter.isPayeeRegex) {
                                                    try { Regex(queryPayee, RegexOption.IGNORE_CASE).containsMatchIn(tx.payee) } catch(e: Exception) { false }
                                                } else tx.payee.contains(queryPayee, ignoreCase = true))
                                            
                                            val categoryIds = filter.categoryIds
                                            val matchesCategory = categoryIds.isEmpty() || tx.categoryId in categoryIds
                                            
                                            val startDate = filter.startDate
                                            val endDate = filter.endDate
                                            val matchesDate = (startDate == null || tx.timestamp >= startDate) &&
                                                              (endDate == null || tx.timestamp <= endDate)
                                            
                                            val currencies = filter.currencies
                                            val matchesCurrency = currencies.isEmpty() || tx.currency in currencies
                                            
                                            val isIncome = filter.isIncome
                                            val matchesIncome = isIncome == null || tx.isIncome == isIncome
                                            
                                            val paymentMethods = filter.paymentMethods
                                            val matchesPaymentMethod = paymentMethods.isEmpty() || tx.paymentMethod in paymentMethods
                                            
                                            matchesNote && matchesPayee && matchesCategory && matchesDate && matchesCurrency && matchesIncome && matchesPaymentMethod
                                        }
                                    }

                                if (filteredTransactions.isEmpty()) {
                                    TransactionListEmptyState()
                                } else {
                                    AeonTransactionList(
                                        transactions = filteredTransactions,
                                        accountMap = accountNamesMap,
                                        categoryMap = categoryMap,
                                        onTransactionClick = { tx -> navigate { onEditTransactionClick(tx.id) } },
                                        contentPadding = PaddingValues(
                                            bottom = calculateScrollBottomPadding()
                                        ),
                                        precision = state.decimalPrecision,
                                        useNoteAsHeader = state.useNoteAsHeader,
                                        showOpeningBalance = true, // Force show balances for account details
                                        showClosingBalance = true,
                                        baseCurrency = analyticsState.selectedCurrency,
                                        exchangeRates = state.exchangeRates,
                                        accountCurrencyMap = accountCurrencyMap,
                                        filterAccountId = accountId
                                    )
                                }
                                }
                            }
                        }
                                    }
                    }
                }
            }
        }

        NavigationPill(
            pagerState = pagerState,
            tabs = tabs,
            modifier = Modifier.align(Alignment.BottomCenter)
        )

        AeonExpandableFab(
            onAddClick = { type, name -> navigate { onAddTransactionClick(accountId, type, name) } },
            templates = allTemplates,
            fabPaddingBottom = if (pagerState.currentPage == 1) 100.dp else 16.dp
        )
        }

        if (showAddCardDialog && uiState is AccountDetailUiState.Success) {
            AccountDetailCardTypeSelectionDialog(
                onDismiss = { showAddCardDialog = false },
                onCardTypeSelected = { cardType ->
                    cardManagerViewModel.addAccountDetailCard(cardType)
                    showAddCardDialog = false
                }
            )
        }
    }
}

@Composable
fun DynamicAccountDetailCards(
    state: AccountDetailUiState.Success,
    cardsConfig: com.osscabinet.ossWallet.domain.model.settings.AccountDetailCardsConfig,
    cardManagerViewModel: CardManagerViewModel,
    accountDetailViewModel: AccountDetailViewModel,
    onRecordsClick: (com.osscabinet.ossWallet.ui.navigation.RecordsFilterContext) -> Unit,
    accountId: String,
    currentTheme: String,
    modifier: Modifier = Modifier,
    screenPadding: PaddingValues = PaddingValues(),
    showAddCardDialog: Boolean,
    onShowAddCardDialogChange: (Boolean) -> Unit,
    cardAnimationEnabled: Boolean
) {
    var showDeleteCardsDialog by remember { mutableStateOf(false) }
    
    Column(
        modifier = modifier
    ) {
        CardReorderableList(
            items = cardsConfig.cards.sortedBy { it.order },
            key = { it.cardId },
            onReorder = { reorderedCards ->
                // Update order property based on new position and save atomically
                cardManagerViewModel.reorderAccountDetailCards(reorderedCards)
            },
            modifier = Modifier.weight(1f),
            contentPadding = PaddingValues(
                start = 0.dp,
                end = 0.dp,
                top = screenPadding.calculateTopPadding() + 24.dp,
                bottom = calculateScrollBottomPadding()
            ),
            verticalArrangement = Arrangement.spacedBy(24.dp),
            animateCards = cardAnimationEnabled
        ) { cardConfig, isDragging, dragModifier ->
            when (cardConfig) {
                is com.osscabinet.ossWallet.domain.model.settings.AccountHeroCardConfig -> {
                    AccountHeroCard(
                        account = state.account,
                        balance = state.account.balance,
                        netBalance = state.account.balance,
                        baseCurrency = state.baseCurrency,
                        exchangeRates = state.exchangeRates,
                        precision = state.decimalPrecision,
                        modifier = Modifier.fillMaxWidth(),
                        showCurrencyBadges = cardConfig.showCurrencyBadges,
                        onShowCurrencyBadgesChange = { show ->
                            accountDetailViewModel.updateCardHeroShowCurrencyBadges(cardConfig.cardId, show)
                        },
                        onClick = { onRecordsClickWithFilterStack(com.osscabinet.ossWallet.domain.model.filter.FilterStack.forAccount(accountId)) },
                        onDelete = { accountDetailViewModel.removeAccountDetailCard(cardConfig.cardId) }
                    )
                }
                is com.osscabinet.ossWallet.domain.model.settings.AccountCashFlowCardConfig -> {
                    val cashFlowResult by produceState<com.osscabinet.ossWallet.domain.usecase.CashFlowResult?>(initialValue = null, key1 = cardConfig, key2 = state) {
                        value = accountDetailViewModel.computeCashFlowForCard(
                            cardConfig = cardConfig,
                            accountTransactions = state.cashFlowTransactions,
                            baseCurrency = state.account.currencyCode,
                            exchangeRates = state.exchangeRates,
                            precision = state.decimalPrecision
                        )
                    }

                    cashFlowResult?.let { result ->
                        com.osscabinet.ossWallet.ui.dashboard.components.CashFlowCard(
                            cashFlowResult = result,
                            baseCurrency = state.account.currencyCode,
                            currentTheme = currentTheme,
                            cardId = cardConfig.cardId,
                            modifier = Modifier.fillMaxWidth(),
                            headerModifier = dragModifier,
                            showTitle = true,
                            precision = state.decimalPrecision,
                            showBaseConversion = cardConfig.showBaseConversion,
                            timelineRange = cardConfig.timelineRange,
                            onShowBaseConversionChange = { _, it ->
                                accountDetailViewModel.updateCardCashFlowShowBaseConversion(cardConfig.cardId, it)
                            },
                            onTimelineRangeChange = { _, it ->
                                accountDetailViewModel.updateCardCashFlowTimelineRange(cardConfig.cardId, it)
                            },
                            onDelete = { accountDetailViewModel.removeAccountDetailCard(cardConfig.cardId) },
                            onClickIncome = { onRecordsClickWithFilterStack(com.osscabinet.ossWallet.domain.model.filter.FilterStack.forAccountAndType(accountId, isIncome = true)) },
                            onClickExpense = { onRecordsClickWithFilterStack(com.osscabinet.ossWallet.domain.model.filter.FilterStack.forAccountAndType(accountId, isIncome = false)) }
                        )
                    } ?: Box(modifier = Modifier.size(0.dp))
                }
                is com.osscabinet.ossWallet.domain.model.settings.AccountCurrencyBalancesCardConfig -> {
                    val currencyTotals by produceState<List<com.osscabinet.ossWallet.domain.usecase.CurrencyAggregationItem>?>(initialValue = null, key1 = cardConfig, key2 = state) {
                        value = accountDetailViewModel.computeCurrencyTotalsForCard(
                            cardConfig = cardConfig,
                            account = state.account,
                            accountTransactions = state.cashFlowTransactions,
                            baseCurrency = state.baseCurrency,
                            exchangeRates = state.exchangeRates,
                            precision = state.decimalPrecision
                        )
                    }

                    currencyTotals?.let { totals ->
                        com.osscabinet.ossWallet.ui.dashboard.components.CurrencyBalancesCard(
                            currencyTotals = totals,
                            baseCurrency = state.baseCurrency,
                            cardId = cardConfig.cardId,
                            modifier = Modifier.fillMaxWidth(),
                            headerModifier = dragModifier,
                            precision = state.decimalPrecision,
                            layout = cardConfig.layout,
                            showBaseConversion = cardConfig.showBaseConversion,
                            timelineRange = cardConfig.timelineRange,
                            onLayoutChange = { _, it ->
                                accountDetailViewModel.updateCardCurrencyBalancesLayout(cardConfig.cardId, it)
                            },
                            onShowBaseConversionChange = { _, it ->
                                accountDetailViewModel.updateCardCurrencyBalancesShowBaseConversion(cardConfig.cardId, it)
                            },
                            onTimelineRangeChange = { _, it ->
                                accountDetailViewModel.updateCardCurrencyBalancesTimelineRange(cardConfig.cardId, it)
                            },
                            onDelete = { accountDetailViewModel.removeAccountDetailCard(cardConfig.cardId) },
                            onCurrencyClick = { currency ->
                                onRecordsClickWithFilterStack(com.osscabinet.ossWallet.domain.model.filter.FilterStack.forAccountAndCurrency(accountId, currency))
                            }
                        )
                    } ?: Box(modifier = Modifier.size(0.dp))
                }
            }
        }
    }

    if (showAddCardDialog) {
        AccountDetailCardTypeSelectionDialog(
            onDismiss = { onShowAddCardDialogChange(false) },
            onCardTypeSelected = { cardType ->
                cardManagerViewModel.addAccountDetailCard(cardType)
                onShowAddCardDialogChange(false)
            }
        )
    }
    
    if (showDeleteCardsDialog) {
        DeleteAccountDetailCardsDialog(
            cardsConfig = cardsConfig.cards,
            onDismiss = { showDeleteCardsDialog = false },
            onDeleteCards = { cardIds ->
                cardIds.forEach { cardId ->
                    accountDetailViewModel.removeAccountDetailCard(cardId)
                }
                showDeleteCardsDialog = false
            }
        )
    }
}

@Composable
fun AccountDetailCardTypeSelectionDialog(
    onDismiss: () -> Unit,
    onCardTypeSelected: (com.osscabinet.ossWallet.ui.viewmodel.AccountDetailCardType) -> Unit
) {
    GenericAddCardDialog(
        onDismiss = onDismiss,
        cardOptions = listOf(
            CardTypeOptionData(
                title = "Account Summary",
                description = "Display account balance and details",
                onClick = { onCardTypeSelected(com.osscabinet.ossWallet.ui.viewmodel.AccountDetailCardType.ACCOUNT_HERO) }
            ),
            CardTypeOptionData(
                title = "Cash Flow",
                description = "Track income and expenses for this account",
                onClick = { onCardTypeSelected(com.osscabinet.ossWallet.ui.viewmodel.AccountDetailCardType.ACCOUNT_CASH_FLOW) }
            ),
            CardTypeOptionData(
                title = "Currency Balances",
                description = "Show currency breakdown for this account",
                onClick = { onCardTypeSelected(com.osscabinet.ossWallet.ui.viewmodel.AccountDetailCardType.ACCOUNT_CURRENCY_BALANCES) }
            )
        )
    )
}

@Composable
fun DeleteAccountDetailCardsDialog(
    cardsConfig: List<com.osscabinet.ossWallet.domain.model.settings.AccountDetailCardConfig>,
    onDismiss: () -> Unit,
    onDeleteCards: (List<String>) -> Unit
) {
    var selectedCards by remember { mutableStateOf(setOf<String>()) }
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Delete Cards") },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 300.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text("Select cards to delete:")
                
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    items(cardsConfig.sortedBy { it.order }) { card ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    selectedCards = if (selectedCards.contains(card.cardId)) {
                                        selectedCards - card.cardId
                                    } else {
                                        selectedCards + card.cardId
                                    }
                                },
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Checkbox(
                                checked = selectedCards.contains(card.cardId),
                                onCheckedChange = { checked ->
                                    selectedCards = if (checked) {
                                        selectedCards + card.cardId
                                    } else {
                                        selectedCards - card.cardId
                                    }
                                }
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(card.title)
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    onDeleteCards(selectedCards.toList())
                },
                enabled = selectedCards.isNotEmpty()
            ) {
                Text("Delete")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
