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
import com.osscabinet.ossWallet.ui.components.*
import com.osscabinet.ossWallet.ui.components.navigation.UnifiedPillNavigation
import com.osscabinet.ossWallet.ui.components.navigation.NavigationTab
import com.osscabinet.ossWallet.ui.components.navigation.TransactionFabMenu
import com.osscabinet.ossWallet.ui.accountdetail.components.AccountHeroCard
import com.osscabinet.ossWallet.ui.components.cards.UnifiedCardRenderer
import com.osscabinet.ossWallet.ui.components.cards.rememberCardWithCallbacks
import com.osscabinet.ossWallet.ui.components.cards.AeonCardList
import com.osscabinet.ossWallet.domain.card.*
import com.osscabinet.ossWallet.domain.model.settings.*
import com.osscabinet.ossWallet.ui.components.cards.CardCallbacks
import com.osscabinet.ossWallet.ui.components.cards.CardWithCallbacks
import com.osscabinet.ossWallet.ui.components.transactions.AeonTransactionList
import com.osscabinet.ossWallet.ui.components.transactions.TransactionListEmptyState
import com.osscabinet.ossWallet.ui.components.transactions.TransactionFilterSheet
import com.osscabinet.ossWallet.ui.components.transactions.RecentActivitiesScreen
import com.osscabinet.ossWallet.ui.navigation.RecordsFilterContext
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
    cardComputerRegistry: CardComputerRegistry,
    templateViewModel: TemplateViewModel,
    onBackClick: () -> Unit,
    onEditAccountClick: (String) -> Unit,
    onAccountClick: (String) -> Unit,
    onEditTransactionClick: (Long) -> Unit,
    onCloneTransactionClick: (Long) -> Unit = {},
    onAddTransactionClick: (String, Int?, String?) -> Unit,
    onRecordsClick: (RecordsFilterContext) -> Unit
) {
    val currentTheme by settingsViewModel.theme.collectAsStateWithLifecycle(initialValue = "System")
    val cardAnimationEnabled by settingsViewModel.cardAnimationEnabled.collectAsStateWithLifecycle(initialValue = true)
    val expandableFabEnabled by settingsViewModel.expandableFabEnabled.collectAsStateWithLifecycle(initialValue = true)

    val uiState by accountDetailViewModel.uiState.collectAsStateWithLifecycle()
    val scope = rememberCoroutineScope()

    val categories by transactionViewModel.categories.collectAsStateWithLifecycle()
    val savedFilters by transactionViewModel.savedFilters.collectAsStateWithLifecycle()
    remember(categories) { categories.associateBy { it.id } }
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
    var isFabExpanded by remember { mutableStateOf(false) }

    val accountDetailCardsConfig by accountDetailViewModel.cardsConfig.collectAsStateWithLifecycle()

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
                AeonShimmer(
                    innerPadding = screenPadding,
                    showCashflow = false,
                    showAccounts = false
                )
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
                            DynamicAccountDetailCards(
                                state = state,
                                cardsConfig = accountDetailCardsConfig,
                                accountDetailViewModel = accountDetailViewModel,
                                cardComputerRegistry = cardComputerRegistry,
                                onRecordsClick = onRecordsClick,
                                accountId = accountId,
                                currentTheme = currentTheme,
                                savedFilters = savedFilters,
                                transactionViewModel = transactionViewModel,
                                onCloneTransactionClick = onCloneTransactionClick,
                                onEditTransactionClick = onEditTransactionClick,
                                onAccountClick = onAccountClick,
                                navigate = navigate,
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(horizontal = 16.dp),
                                screenPadding = PaddingValues(),
                                showAddCardDialog = showAddCardDialog,
                                onShowAddCardDialogChange = { showAddCardDialog = it },
                                cardAnimationEnabled = cardAnimationEnabled
                            )
                        }
                        1 -> {
                            // Transactions tab content using RecentActivitiesCard
                            val accounts by accountViewModel.accounts.collectAsStateWithLifecycle()
                            val accountNamesMap = remember(accounts) { accounts.associate { it.id to it.name } }
                            remember(accounts) { accounts.associate { it.id to it.currencyCode } }
                            val categoriesList by transactionViewModel.categories.collectAsStateWithLifecycle()
                            val categoryMap = remember(categoriesList) { categoriesList.associateBy { it.id } }
                            val revealedCardIds by transactionViewModel.revealedCardIdsList.collectAsStateWithLifecycle()

                            val transactionsWithBalanceList = state.transactionsWithBalance

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
                                RecentActivitiesScreen(
                                    allTransactions = transactionsWithBalanceList,
                                    accountNamesMap = accountNamesMap,
                                    categoryMap = categoryMap,
                                    onRecordsClick = { _ -> navigate { onRecordsClick(RecordsFilterContext.ByAccount(accountId)) } },
                                    onTransactionClick = { txId -> navigate { onEditTransactionClick(txId) } },
                                    baseCurrency = state.baseCurrency,
                                    exchangeRates = state.exchangeRates,
                                    contentPadding = PaddingValues(bottom = calculateScrollBottomPadding()),
                                    modifier = Modifier.fillMaxSize(),
                                    revealedCardIds = revealedCardIds,
                                    onItemExpanded = { transactionViewModel.onItemExpanded(it) },
                                    onItemCollapsed = { transactionViewModel.onItemCollapsed(it) },
                                    onStatusChange = { transactionId, status -> transactionViewModel.quickUpdateStatus(transactionId, status) },
                                    onClone = { onCloneTransactionClick(it) },
                                    onDelete = { transactionViewModel.quickDeleteTransaction(it) },
                                    onSplit = { transactionViewModel.quickSplitTransaction(it) },
                                    onFullSwipeRight = { transactionViewModel.toggleStatusClearedUncleared(it) },
                                    applyFilter = false,
                                    excludeFuture = !state.showPlannedPayments,
                                    precision = state.decimalPrecision,
                                    is24Hour = state.use24HourFormat,
                                    filterContext = RecordsFilterContext.ByAccount(accountId)
                                )
                            }
                        }
                    }
                }
            }
        }

        UnifiedPillNavigation(
            selectedTab = pagerState.currentPage,
            onTabSelected = { index ->
                scope.launch { pagerState.animateScrollToPage(index) }
            },
            tabs = tabs,
            isFabExpanded = isFabExpanded,
            onFabToggle = { if (expandableFabEnabled) isFabExpanded = it },
            onFabClick = {
                if (!expandableFabEnabled) {
                    navigate { onAddTransactionClick(accountId, 0, null) }
                }
            },
            fabMenu = if (expandableFabEnabled) {
                {
                    TransactionFabMenu(
                        isExpanded = isFabExpanded,
                        templates = allTemplates,
                        onAddClick = { type, name ->
                            isFabExpanded = false
                            navigate { onAddTransactionClick(accountId, type, name) }
                        }
                    )
                }
            } else null
        )
        }

    }
}

@Composable
fun DynamicAccountDetailCards(
    state: AccountDetailUiState.Success,
    cardsConfig: DashboardCardsConfig,
    accountDetailViewModel: AccountDetailViewModel,
    cardComputerRegistry: CardComputerRegistry,
    onRecordsClick: (RecordsFilterContext) -> Unit,
    accountId: String,
    currentTheme: String,
    savedFilters: List<com.osscabinet.ossWallet.domain.model.Filter>,
    transactionViewModel: TransactionViewModel,
    onCloneTransactionClick: (Long) -> Unit,
    onEditTransactionClick: (Long) -> Unit,
    onAccountClick: (String) -> Unit,
    navigate: (() -> Unit) -> Unit,
    modifier: Modifier = Modifier,
    screenPadding: PaddingValues = PaddingValues(),
    showAddCardDialog: Boolean,
    onShowAddCardDialogChange: (Boolean) -> Unit,
    cardAnimationEnabled: Boolean
) {
    var showDeleteCardsDialog by remember { mutableStateOf(false) }

    val accountScopeFilter = remember(accountId) { TransactionFilter(accountIds = listOf(accountId), excludeFuture = false) }
    val sharedCardInput = remember(state) {
        CardInput(
            transactions = state.allAccountTransactions,
            accounts = listOf(state.account),
            categories = state.allCategories,
            exchangeRates = state.exchangeRates,
            baseCurrency = state.baseCurrency,
            precision = state.decimalPrecision,
            filter = accountScopeFilter
        )
    }

    // Always-present hero card — not in the reorderable list, cannot be deleted.
    val heroConfig = remember(accountId) {
        AccountHeroCardConfig(
            cardId = "hero_$accountId",
            filter = accountScopeFilter
        )
    }
    val heroCard = rememberCardWithCallbacks(
        config = heroConfig,
        input = sharedCardInput,
        registry = cardComputerRegistry,
        updateHandler = accountDetailViewModel,
        onDelete = {},
        onRecordsClick = onRecordsClick,
        onItemExpanded = { transactionViewModel.onItemExpanded(it) },
        onItemCollapsed = { transactionViewModel.onItemCollapsed(it) },
        onStatusChange = { transactionId, status ->
            transactionViewModel.quickUpdateStatus(
                transactionId,
                status
            )
        },
        onClone = { onCloneTransactionClick(it) },
        onDeleteTransaction = { transactionViewModel.quickDeleteTransaction(it) },
        onSplit = { transactionViewModel.quickSplitTransaction(it) },
        onFullSwipeRight = { transactionViewModel.toggleStatusClearedUncleared(it) },
        currentTheme = currentTheme
    )

    Column(
        modifier = modifier
            .background(MaterialTheme.colorScheme.background)
    ) {
        AeonCardList(
            cards = cardsConfig.cards.filter { it !is AccountHeroCardConfig },
            cardInput = sharedCardInput,
            cardComputerRegistry = cardComputerRegistry,
            cardUpdateHandler = accountDetailViewModel,
            onReorder = { accountDetailViewModel.reorderCards(it) },
            onDelete = { accountDetailViewModel.removeCard(it) },
            onRecordsClick = onRecordsClick,
            modifier = Modifier.weight(1f),
            contentPadding = PaddingValues(
                start = 0.dp,
                end = 0.dp,
                top = 8.dp,
                bottom = calculateScrollBottomPadding()
            ),
            animateCards = cardAnimationEnabled,
            currentTheme = currentTheme,
            accounts = listOf(state.account),
            allCategories = state.allCategories,
            savedFilters = savedFilters,
            onAccountClick = { id -> navigate { onAccountClick(id) } },
            onTransactionClick = onEditTransactionClick,
            onItemExpanded = { transactionViewModel.onItemExpanded(it) },
            onItemCollapsed = { transactionViewModel.onItemCollapsed(it) },
            onStatusChange = { transactionId, status ->
                transactionViewModel.quickUpdateStatus(
                    transactionId,
                    status
                )
            },
            onClone = { onCloneTransactionClick(it) },
            onDeleteTransaction = { transactionViewModel.quickDeleteTransaction(it) },
            onSplit = { transactionViewModel.quickSplitTransaction(it) },
            onFullSwipeRight = { transactionViewModel.toggleStatusClearedUncleared(it) },
            baseFilter = accountScopeFilter,
            forceDisableAccountSelection = true,
            header = {
                if (heroCard != null && heroCard !is CardWithCallbacks.ComputeError) {
                    UnifiedCardRenderer(
                        cardWithCallbacks = heroCard,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = screenPadding.calculateTopPadding() + 16.dp, bottom = 4.dp),
                        showTitle = false,
                        currentTheme = currentTheme,
                        accounts = listOf(state.account),
                        allCategories = state.allCategories,
                        savedFilters = savedFilters
                    )
                }
            }
        )
    }

    if (showAddCardDialog) {
        AccountDetailCardTypeSelectionDialog(
            onDismiss = { onShowAddCardDialogChange(false) },
            onCardTypeSelected = { cardType ->
                accountDetailViewModel.addCard(
                    CardConfig.createDefault(cardType, ScreenContext.ACCOUNT_DETAIL)
                )
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
                    accountDetailViewModel.removeCard(cardId)
                }
                showDeleteCardsDialog = false
            }
        )
    }
}

@Composable
fun AccountDetailCardTypeSelectionDialog(
    onDismiss: () -> Unit,
    onCardTypeSelected: (CardType) -> Unit
) {
    GenericAddCardDialog(
        onDismiss = onDismiss,
        cardOptions = CardType.entries
            .filter { it.isUserAddableOn(ScreenContext.ACCOUNT_DETAIL) }
            .map { type ->
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
    cardsConfig: List<CardConfig>,
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
