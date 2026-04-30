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
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
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
import com.osscabinet.ossWallet.ui.components.navigation.ExpressiveNavigationBar
import com.osscabinet.ossWallet.ui.components.navigation.NavigationTab
import com.osscabinet.ossWallet.ui.components.AeonExpandableFab
import com.osscabinet.ossWallet.ui.accountdetail.components.AccountHeroCard
import com.osscabinet.ossWallet.ui.components.cards.UnifiedCardRenderer
import com.osscabinet.ossWallet.ui.components.cards.CardRenderData
import com.osscabinet.ossWallet.ui.components.cards.CardCallbacks
import com.osscabinet.ossWallet.ui.components.cards.CardWithCallbacks
import com.osscabinet.ossWallet.ui.components.transactions.AeonTransactionList
import com.osscabinet.ossWallet.ui.components.transactions.TransactionListEmptyState
import com.osscabinet.ossWallet.ui.components.transactions.TransactionFilterSheet
import com.osscabinet.ossWallet.ui.components.transactions.RecentActivitiesCard
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
    onCloneTransactionClick: (Long) -> Unit = {},
    onAddTransactionClick: (String, Int?, String?) -> Unit,
    onRecordsClick: (com.osscabinet.ossWallet.ui.navigation.RecordsFilterContext) -> Unit
) {
    val currentTheme by settingsViewModel.theme.collectAsStateWithLifecycle(initialValue = "System")
    val cardAnimationEnabled by settingsViewModel.cardAnimationEnabled.collectAsStateWithLifecycle(initialValue = true)

    val uiState by accountDetailViewModel.uiState.collectAsStateWithLifecycle()
    val scope = rememberCoroutineScope()

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
    val tabs = listOf(
        NavigationTab(
            icon = Icons.Rounded.Dashboard,
            selectedIcon = Icons.Rounded.Dashboard,
            label = "Overview",
            contentDescription = "Overview tab"
        ),
        NavigationTab(
            icon = Icons.Rounded.Receipt,
            selectedIcon = Icons.Rounded.Receipt,
            label = "Transactions",
            contentDescription = "Transactions tab"
        )
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
        },
        bottomBar = {
            ExpressiveNavigationBar(
                selectedTab = pagerState.currentPage,
                onTabSelected = { index ->
                    scope.launch { pagerState.animateScrollToPage(index) }
                },
                tabs = tabs
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
                            // Transactions tab content using RecentActivitiesCard
                            val accounts by accountViewModel.accounts.collectAsStateWithLifecycle()
                            val accountNamesMap = remember(accounts) { accounts.associate { it.id to it.name } }
                            val accountCurrencyMap = remember(accounts) { accounts.associate { it.id to it.currencyCode } }
                            val categoriesList by transactionViewModel.categories.collectAsStateWithLifecycle()
                            val categoryMap = remember(categoriesList) { categoriesList.associateBy { it.id } }
                            val revealedCardIds by transactionViewModel.revealedCardIdsList.collectAsStateWithLifecycle()

                            // Calculate running balance for all account transactions
                            val transactionsWithBalanceList = remember(state.allAccountTransactions, state.account.balance) {
                                // Calculate running balance from transactions
                                // Start from zero and apply transactions in reverse order
                                var currentBalance = BigDecimal.ZERO
                                state.allAccountTransactions.sortedByDescending { it.timestamp }.map { tx ->
                                    val balanceAfter = currentBalance
                                    val balanceBefore = currentBalance.subtract(tx.amount) // tx.amount is already signed
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
                                RecentActivitiesCard(
                                    allTransactions = transactionsWithBalanceList,
                                    accountNamesMap = accountNamesMap,
                                    categoryMap = categoryMap,
                                    decimalPrecision = state.decimalPrecision,
                                    onRecordsClick = { context -> navigate { onRecordsClick(context) } },
                                    onTransactionClick = { txId -> navigate { onEditTransactionClick(txId) } },
                                    baseCurrency = state.baseCurrency,
                                    exchangeRates = state.exchangeRates,
                                    accountCurrencyMap = accountCurrencyMap,
                                    contentPadding = PaddingValues(bottom = calculateScrollBottomPadding()),
                                    modifier = Modifier.fillMaxSize(),
                                    revealedCardIds = revealedCardIds,
                                    onItemExpanded = { transactionViewModel.onItemExpanded(it) },
                                    onItemCollapsed = { transactionViewModel.onItemCollapsed(it) },
                                    onStatusChange = { transactionId, status -> transactionViewModel.quickUpdateStatus(transactionId, status) },
                                    onClone = { onCloneTransactionClick(it) },
                                    onDelete = { transactionViewModel.quickDeleteTransaction(it) },
                                    onSplit = { transactionViewModel.quickSplitTransaction(it) },
                                    onFullSwipeRight = { transactionViewModel.toggleStatusClearedUncleared(it) }
                                )
                            }
                        }
                    }
                }
            }
        }

        AeonExpandableFab(
            onAddClick = { type, name -> navigate { onAddTransactionClick(accountId, type, name) } },
            templates = allTemplates,
            fabPaddingBottom = 96.dp
        )
        }

        if (showAddCardDialog && uiState is AccountDetailUiState.Success) {
            AccountDetailCardTypeSelectionDialog(
                onDismiss = { showAddCardDialog = false },
                onCardTypeSelected = { cardType ->
                    cardManagerViewModel.addAccountDetailCard(cardType, accountId)
                    showAddCardDialog = false
                }
            )
        }
    }
}

@Composable
fun DynamicAccountDetailCards(
    state: AccountDetailUiState.Success,
    cardsConfig: com.osscabinet.ossWallet.domain.model.settings.DashboardCardsConfig,
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
            val cardWithCallbacks = when (cardConfig) {
                is com.osscabinet.ossWallet.domain.model.settings.AccountHeroCardConfig -> {
                    CardWithCallbacks(
                        renderData = CardRenderData.AccountHero(
                            config = cardConfig,
                            account = state.account,
                            balance = state.account.balance,
                            netBalance = state.account.balance,
                            baseCurrency = state.baseCurrency,
                            exchangeRates = state.exchangeRates,
                            precision = state.decimalPrecision
                        ),
                        callbacks = CardCallbacks.AccountHero(
                            onShowCurrencyBadgesChange = { cardId, show ->
                                accountDetailViewModel.updateCardHeroShowCurrencyBadges(cardId, show)
                            },
                            onClick = { onRecordsClick(com.osscabinet.ossWallet.ui.navigation.RecordsFilterContext.ByAccount(accountId)) },
                            onDelete = { accountDetailViewModel.removeAccountDetailCard(cardConfig.cardId) }
                        )
                    )
                }
                is com.osscabinet.ossWallet.domain.model.settings.CashFlowCardConfig -> {
                    var cashFlowResult by remember { mutableStateOf<com.osscabinet.ossWallet.domain.model.analytics.CashFlowResult?>(null) }
                    LaunchedEffect(cardConfig.cardId, cardConfig.timelineRange, state.allAccountTransactions, state.account.currencyCode, state.exchangeRates, state.decimalPrecision) {
                        cashFlowResult = accountDetailViewModel.computeCashFlowCard(
                            cardConfig = cardConfig,
                            transactions = state.allAccountTransactions,
                            baseCurrency = state.account.currencyCode,
                            exchangeRates = state.exchangeRates,
                            precision = state.decimalPrecision
                        )
                    }

                    cashFlowResult?.let { result ->
                        CardWithCallbacks(
                            renderData = CardRenderData.CashFlow(
                                config = cardConfig,
                                cashFlowResult = result,
                                baseCurrency = state.account.currencyCode,
                                precision = state.decimalPrecision
                            ),
                            callbacks = CardCallbacks.CashFlow(
                                onShowBaseConversionChange = { cardId, show ->
                                    accountDetailViewModel.updateCardCashFlowShowBaseConversion(cardId, show)
                                },
                                onTimelineRangeChange = { cardId, range ->
                                    accountDetailViewModel.updateCardCashFlowTimelineRange(cardId, range)
                                },
                                onClickIncome = { onRecordsClick(com.osscabinet.ossWallet.ui.navigation.RecordsFilterContext.ByAccountAndType(accountId, isIncome = true)) },
                                onClickExpense = { onRecordsClick(com.osscabinet.ossWallet.ui.navigation.RecordsFilterContext.ByAccountAndType(accountId, isIncome = false)) },
                                onDelete = { accountDetailViewModel.removeAccountDetailCard(cardConfig.cardId) }
                            )
                        )
                    }
                }
                is com.osscabinet.ossWallet.domain.model.settings.CurrencyBalancesCardConfig -> {
                    var currencyTotals by remember { mutableStateOf<List<com.osscabinet.ossWallet.domain.model.analytics.CurrencyAggregationItem>?>(null) }
                    LaunchedEffect(cardConfig.cardId, cardConfig.timelineRange, state.allAccountTransactions, state.baseCurrency, state.exchangeRates, state.decimalPrecision) {
                        currencyTotals = accountDetailViewModel.computeCurrencyBalancesCard(
                            cardConfig = cardConfig,
                            accounts = listOf(state.account),
                            transactions = state.allAccountTransactions,
                            baseCurrency = state.baseCurrency,
                            exchangeRates = state.exchangeRates,
                            precision = state.decimalPrecision
                        )
                    }

                    currencyTotals?.let { totals ->
                        CardWithCallbacks(
                            renderData = CardRenderData.CurrencyBalances(
                                config = cardConfig,
                                currencyTotals = totals,
                                baseCurrency = state.baseCurrency,
                                precision = state.decimalPrecision
                            ),
                            callbacks = CardCallbacks.CurrencyBalances(
                                onLayoutChange = { cardId, layout ->
                                    accountDetailViewModel.updateCardCurrencyBalancesLayout(cardId, layout)
                                },
                                onShowBaseConversionChange = { cardId, show ->
                                    accountDetailViewModel.updateCardCurrencyBalancesShowBaseConversion(cardId, show)
                                },
                                onTimelineRangeChange = { cardId, range ->
                                    accountDetailViewModel.updateCardCurrencyBalancesTimelineRange(cardId, range)
                                },
                                onCurrencyClick = { currency, timelineRange ->
                                    val (start, end) = com.osscabinet.ossWallet.domain.service.TimelineService.getDateRange(timelineRange)
                                    onRecordsClick(com.osscabinet.ossWallet.ui.navigation.RecordsFilterContext.ByAccountAndCurrency(accountId, currency))
                                },
                                onDelete = { accountDetailViewModel.removeAccountDetailCard(cardConfig.cardId) }
                            )
                        )
                    }
                }
                else -> null
            }
            
            cardWithCallbacks?.let {
                UnifiedCardRenderer(
                    renderData = it.renderData,
                    callbacks = it.callbacks,
                    modifier = Modifier.fillMaxWidth(),
                    headerModifier = dragModifier,
                    showTitle = true,
                    currentTheme = currentTheme
                )
            } ?: Box(modifier = Modifier.size(0.dp))
        }
    }

    if (showAddCardDialog) {
        AccountDetailCardTypeSelectionDialog(
            onDismiss = { onShowAddCardDialogChange(false) },
            onCardTypeSelected = { cardType ->
                cardManagerViewModel.addAccountDetailCard(cardType, accountId)
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
    onCardTypeSelected: (com.osscabinet.ossWallet.ui.viewmodel.UnifiedCardType) -> Unit
) {
    GenericAddCardDialog(
        onDismiss = onDismiss,
        cardOptions = listOf(
            com.osscabinet.ossWallet.ui.viewmodel.UnifiedCardType.ACCOUNT_DETAIL_HERO,
            com.osscabinet.ossWallet.ui.viewmodel.UnifiedCardType.ACCOUNT_DETAIL_CASH_FLOW,
            com.osscabinet.ossWallet.ui.viewmodel.UnifiedCardType.ACCOUNT_DETAIL_CURRENCY_BALANCES
        ).map { type ->
            CardTypeOptionData(
                title = type.displayName,
                description = type.description,
                onClick = { onCardTypeSelected(type) }
            )
        }
    )
}

@Composable
fun DeleteAccountDetailCardsDialog(
    cardsConfig: List<com.osscabinet.ossWallet.domain.model.settings.CardConfig>,
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
