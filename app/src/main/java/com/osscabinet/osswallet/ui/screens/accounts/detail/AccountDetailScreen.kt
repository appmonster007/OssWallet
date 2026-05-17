package com.osscabinet.ossWallet.ui.screens.accounts.detail

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
import com.osscabinet.ossWallet.domain.model.TransactionType
import com.osscabinet.ossWallet.domain.model.TransactionListState
import com.osscabinet.ossWallet.domain.model.merge
import com.osscabinet.ossWallet.domain.model.timeline.TimelineRange
import com.osscabinet.ossWallet.domain.model.TransactionWithBalance
import com.osscabinet.ossWallet.ui.screens.accounts.AccountViewModel
import com.osscabinet.ossWallet.ui.screens.transactions.TransactionViewModel
import com.osscabinet.ossWallet.ui.screens.settings.SettingsViewModel
import com.osscabinet.ossWallet.ui.screens.analytics.AnalyticsViewModel
import com.osscabinet.ossWallet.ui.screens.accounts.detail.AccountDetailViewModel
import com.osscabinet.ossWallet.ui.components.*
import com.osscabinet.ossWallet.ui.components.navigation.UnifiedPillNavigation
import com.osscabinet.ossWallet.ui.components.navigation.NavigationTab
import com.osscabinet.ossWallet.ui.components.navigation.TransactionFabMenu
import com.osscabinet.ossWallet.ui.screens.accounts.detail.components.AccountHeroCard
import com.osscabinet.ossWallet.ui.components.cards.UnifiedCardRenderer
import com.osscabinet.ossWallet.ui.components.cards.rememberCardWithCallbacks
import com.osscabinet.ossWallet.ui.components.cards.AeonCardList
import com.osscabinet.ossWallet.ui.components.cards.CardListCallbacks
import com.osscabinet.ossWallet.ui.components.cards.FilterSelectionContext
import com.osscabinet.ossWallet.domain.card.*
import com.osscabinet.ossWallet.domain.model.settings.*
import com.osscabinet.ossWallet.ui.components.cards.CardCallbacks
import com.osscabinet.ossWallet.ui.components.cards.CardWithCallbacks
import com.osscabinet.ossWallet.ui.components.transactions.AeonTransactionList
import com.osscabinet.ossWallet.ui.components.transactions.TransactionListEmptyState
import com.osscabinet.ossWallet.ui.components.transactions.FilterEvent
import com.osscabinet.ossWallet.ui.components.transactions.TransactionFilterSheet
import com.osscabinet.ossWallet.ui.components.transactions.TransactionItemEvent
import com.osscabinet.ossWallet.ui.components.transactions.RecentActivitiesScreen
import com.osscabinet.ossWallet.ui.screens.templates.TemplateViewModel
import com.osscabinet.ossWallet.ui.screens.accounts.detail.AccountDetailUiState
import com.osscabinet.ossWallet.ui.theme.AeonTheme
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import java.net.URLDecoder
import java.nio.charset.StandardCharsets
import com.osscabinet.ossWallet.ui.theme.AppColors
import com.osscabinet.ossWallet.ui.theme.AppTextStyles

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
    filterJson: String? = null,
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
    onSplitTransactionClick: (Long) -> Unit = {},
    onAddTransactionClick: (String, TransactionType?, Long?) -> Unit,
    onRecordsClick: (TransactionFilter) -> Unit
) {
    // Initialize filter from navigation parameter when screen opens
    LaunchedEffect(filterJson) {
        if (filterJson != null) {
            try {
                val decoded = URLDecoder.decode(filterJson, StandardCharsets.UTF_8.toString())
                val filter = Json.decodeFromString<TransactionFilter>(decoded)
                accountDetailViewModel.updateFilter(filter)
            } catch (e: Exception) {
                // Handle error silently
            }
        }
    }

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
    var showFilterSheet by remember { mutableStateOf(false) }
    var isFabExpanded by remember { mutableStateOf(false) }

    val accountDetailCardsConfig by accountDetailViewModel.cardsConfig.collectAsStateWithLifecycle()
    val accounts by accountViewModel.accounts.collectAsStateWithLifecycle()

    if (showFilterSheet && uiState is AccountDetailUiState.Success) {
        val state = uiState as AccountDetailUiState.Success
        TransactionFilterSheet(
            filter = state.currentFilter,
            accounts = accounts,
            categories = categories,
            savedFilters = savedFilters,
            onFilterEvent = { event ->
                when (event) {
                    is FilterEvent.Apply -> accountDetailViewModel.updateFilter(event.filter)
                    is FilterEvent.UpdateTimeline -> accountDetailViewModel.updateTimelineRange(event.range)
                    is FilterEvent.Reset -> accountDetailViewModel.clearFilters()
                    is FilterEvent.UpdateSettings -> {}
                }
            },
            onToggleExpand = { /* Handle if needed */ },
            onClose = { showFilterSheet = false },
            showAccountFilter = false,
            accountNames = remember(accounts) { accounts.associate { it.id to it.name } },
        )
    }

    if (showSettingsDialog && uiState is AccountDetailUiState.Success) {
        val state = uiState as AccountDetailUiState.Success
        AlertDialog(
            onDismissRequest = { showSettingsDialog = false },
            title = { Text("Transaction Settings") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(AeonTheme.dimensions.spacingLg)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Show Planned Payments", style = AppTextStyles.listItemTitle)
                            Text("Include future transactions", style = AppTextStyles.caption, color = AppColors.border)
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
                            Text("Use Note as Header", style = AppTextStyles.listItemTitle)
                            Text("Primary header will show transaction note", style = AppTextStyles.caption, color = AppColors.border)
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
                    Text("Account Details", style = AppTextStyles.cardSectionTitle)
                },
                navigationIcon = {
                    IconButton(onClick = { navigate { onBackClick() } }) {
                        Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = { showFilterSheet = true }) {
                        Icon(Icons.Rounded.FilterList, contentDescription = "Filter")
                    }
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
                                onSplitTransactionClick = onSplitTransactionClick,
                                onAccountClick = onAccountClick,
                                navigate = navigate,
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(horizontal = AeonTheme.dimensions.screenPadding),
                                screenPadding = PaddingValues(),
                                showAddCardDialog = showAddCardDialog,
                                onShowAddCardDialogChange = { showAddCardDialog = it },
                                cardAnimationEnabled = cardAnimationEnabled
                            )
                        }
                        1 -> {
                            // Transactions tab content using RecentActivitiesCard
                            val revealedCardIds by transactionViewModel.revealedCardIdsList.collectAsStateWithLifecycle()

                            if (state.listState.isEmpty) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxSize(),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        Icon(
                                            Icons.Rounded.History,
                                            contentDescription = null,
                                            modifier = Modifier.size(AeonTheme.dimensions.iconSize3xl),
                                            tint = AppColors.contentSecondary.copy(alpha = 0.2f)
                                        )
                                        Spacer(modifier = Modifier.height(AeonTheme.dimensions.spacingLg))
                                        Text(
                                            "No transactions yet",
                                            style = AppTextStyles.listItemTitle,
                                            color = AppColors.border
                                        )
                                    }
                                }
                            } else {
                                RecentActivitiesScreen(
                                    listState = state.listState,
                                    onRecordsClick = { drillDown ->
                                        onRecordsClick(state.currentFilter.merge(drillDown))
                                    },
                                    baseCurrency = state.baseCurrency,
                                    contentPadding = PaddingValues(bottom = calculateScrollBottomPadding()),
                                    modifier = Modifier.fillMaxSize(),
                                    revealedCardIds = revealedCardIds,
                                    onTransactionEvent = { event ->
                                        when (event) {
                                            is TransactionItemEvent.Click -> navigate { onEditTransactionClick(event.id) }
                                            is TransactionItemEvent.Expand -> transactionViewModel.onItemExpanded(event.id)
                                            is TransactionItemEvent.Collapse -> transactionViewModel.onItemCollapsed(event.id)
                                            is TransactionItemEvent.StatusChange -> transactionViewModel.quickUpdateStatus(event.id, event.status)
                                            is TransactionItemEvent.Clone -> onCloneTransactionClick(event.id)
                                            is TransactionItemEvent.Delete -> transactionViewModel.quickDeleteTransaction(event.id)
                                            is TransactionItemEvent.Split -> onSplitTransactionClick(event.id)
                                            is TransactionItemEvent.FullSwipeRight -> transactionViewModel.toggleStatusClearedUncleared(event.id)
                                        }
                                    },
                                    precision = state.decimalPrecision,
                                    is24Hour = state.use24HourFormat,
                                    filter = state.currentFilter
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
                    navigate { onAddTransactionClick(accountId, TransactionType.EXPENSE, null) }
                }
            },
            fabMenu = if (expandableFabEnabled) {
                {
                    TransactionFabMenu(
                        isExpanded = isFabExpanded,
                        templates = allTemplates,
                        onAddClick = { type, templateId ->
                            isFabExpanded = false
                            navigate { onAddTransactionClick(accountId, type, templateId) }
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
    onRecordsClick: (TransactionFilter) -> Unit,
    accountId: String,
    currentTheme: String,
    savedFilters: List<com.osscabinet.ossWallet.domain.model.Filter>,
    transactionViewModel: TransactionViewModel,
    onCloneTransactionClick: (Long) -> Unit,
    onEditTransactionClick: (Long) -> Unit,
    onSplitTransactionClick: (Long) -> Unit = {},
    onAccountClick: (String) -> Unit,
    navigate: (() -> Unit) -> Unit,
    modifier: Modifier = Modifier,
    screenPadding: PaddingValues = PaddingValues(),
    showAddCardDialog: Boolean,
    onShowAddCardDialogChange: (Boolean) -> Unit,
    cardAnimationEnabled: Boolean
) {
    var showDeleteCardsDialog by remember { mutableStateOf(false) }

    val accountScopeFilter = state.currentFilter
    val sharedCardInput = remember(state) {
        CardInput(
            profileId = state.profileId,
            transactions = state.allAccountTransactions,
            accounts = listOf(state.account),
            categories = state.allCategories,
            exchangeRates = state.exchangeRates,
            baseCurrency = state.baseCurrency,
            precision = state.decimalPrecision,
            filter = accountScopeFilter
        )
    }

    // Always-present hero card — no filters (All Time)
    val heroCardInput = remember(state) {
        CardInput(
            profileId = state.profileId,
            transactions = state.allAccountTransactions,
            accounts = listOf(state.account),
            categories = state.allCategories,
            exchangeRates = state.exchangeRates,
            baseCurrency = state.baseCurrency,
            precision = state.decimalPrecision,
            filter = TransactionFilter(accountIds = setOf(accountId), timelineRange = TimelineRange.AllTime)
        )
    }

    val heroConfig = remember(accountId) {
        AccountHeroCardConfig(
            cardId = "hero_$accountId",
            filter = TransactionFilter(accountIds = setOf(accountId), timelineRange = TimelineRange.AllTime)
        )
    }
    val heroCard = rememberCardWithCallbacks(
        config = heroConfig,
        input = heroCardInput,
        registry = cardComputerRegistry,
        updateHandler = accountDetailViewModel,
        onDelete = {},
        onRecordsClick = onRecordsClick,
        callbacks = CardListCallbacks(
            onItemExpanded = { transactionViewModel.onItemExpanded(it) },
            onItemCollapsed = { transactionViewModel.onItemCollapsed(it) },
            onStatusChange = { id, status -> transactionViewModel.quickUpdateStatus(id, status) },
            onClone = { onCloneTransactionClick(it) },
            onDeleteTransaction = { transactionViewModel.quickDeleteTransaction(it) },
            onSplit = { onSplitTransactionClick(it) },
            onFullSwipeRight = { transactionViewModel.toggleStatusClearedUncleared(it) },
        ),
        currentTheme = currentTheme
    )

    Column(
        modifier = modifier
            .background(AppColors.screenBackground)
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
                top = AeonTheme.dimensions.spacingMd,
                bottom = calculateScrollBottomPadding()
            ),
            animateCards = cardAnimationEnabled,
            currentTheme = currentTheme,
            filterContext = FilterSelectionContext(
                accounts = listOf(state.account),
                allCategories = state.allCategories,
                savedFilters = savedFilters
            ),
            callbacks = CardListCallbacks(
                onAccountClick = { id -> navigate { onAccountClick(id) } },
                onTransactionClick = onEditTransactionClick,
                onItemExpanded = { transactionViewModel.onItemExpanded(it) },
                onItemCollapsed = { transactionViewModel.onItemCollapsed(it) },
                onStatusChange = { id, status -> transactionViewModel.quickUpdateStatus(id, status) },
                onClone = { onCloneTransactionClick(it) },
                onDeleteTransaction = { transactionViewModel.quickDeleteTransaction(it) },
                onSplit = { onSplitTransactionClick(it) },
                onFullSwipeRight = { transactionViewModel.toggleStatusClearedUncleared(it) },
            ),
            baseFilter = accountScopeFilter,
            forceDisableAccountSelection = true,
            header = {
                if (heroCard != null && heroCard !is CardWithCallbacks.ComputeError) {
                    UnifiedCardRenderer(
                        cardWithCallbacks = heroCard,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = screenPadding.calculateTopPadding() + AeonTheme.dimensions.spacingLg, bottom = AeonTheme.dimensions.spacingXs),
                        currentTheme = currentTheme,
                        filterContext = FilterSelectionContext(
                            accounts = listOf(state.account),
                            allCategories = state.allCategories,
                            savedFilters = savedFilters
                        ),
                    )
                }
            }
        )
    }

    if (showAddCardDialog) {
        CardTypeSelectionDialog(
            screenContext = ScreenContext.ACCOUNT_DETAIL,
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
                verticalArrangement = Arrangement.spacedBy(AeonTheme.dimensions.spacingSm)
            ) {
                Text("Select cards to delete:")
                
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(AeonTheme.dimensions.spacingXs)
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
                            Spacer(modifier = Modifier.width(AeonTheme.dimensions.spacingSm))
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
