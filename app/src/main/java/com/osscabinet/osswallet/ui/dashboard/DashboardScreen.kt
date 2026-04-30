package com.osscabinet.ossWallet.ui.dashboard

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items as gridItems
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.AccountBox
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AttachMoney
import androidx.compose.material.icons.filled.PieChart
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.runtime.*
import androidx.compose.runtime.produceState
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.flow.map
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import com.osscabinet.ossWallet.ui.navigation.DashboardViewContext
import kotlinx.serialization.json.Json
import java.net.URLDecoder
import java.nio.charset.StandardCharsets
import com.osscabinet.ossWallet.domain.model.Account
import com.osscabinet.ossWallet.domain.model.Transaction
import com.osscabinet.ossWallet.domain.model.analytics.CashFlowResult
import com.osscabinet.ossWallet.domain.model.analytics.CurrencyAggregationItem
import com.osscabinet.ossWallet.domain.model.analytics.AccountSummaryItem
import androidx.lifecycle.viewmodel.compose.viewModel
import com.osscabinet.ossWallet.domain.model.analytics.ComputationContext
import com.osscabinet.ossWallet.domain.model.settings.*
import com.osscabinet.ossWallet.domain.model.TransactionWithBalance
import com.osscabinet.ossWallet.domain.model.CashFlowType
import com.osscabinet.ossWallet.ui.viewmodel.AccountViewModel
import com.osscabinet.ossWallet.ui.viewmodel.TransactionViewModel
import com.osscabinet.ossWallet.ui.viewmodel.SettingsViewModel
import com.osscabinet.ossWallet.ui.viewmodel.AnalyticsViewModel
import com.osscabinet.ossWallet.ui.util.formatCurrencyRight
import com.osscabinet.ossWallet.ui.util.toContainerColor
import com.osscabinet.ossWallet.ui.util.toOnContainerColor
import com.osscabinet.ossWallet.ui.viewmodel.TemplateViewModel
import com.osscabinet.ossWallet.ui.viewmodel.CardManagerViewModel
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.AttachMoney
import androidx.compose.material.icons.filled.AccountBox
import androidx.compose.material.icons.filled.PieChart
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import com.osscabinet.ossWallet.domain.card.*
import com.osscabinet.ossWallet.ui.components.cards.*
import com.osscabinet.ossWallet.domain.model.settings.*
import com.osscabinet.ossWallet.domain.model.settings.DashboardCardsConfig
import com.osscabinet.ossWallet.domain.model.Template
import com.osscabinet.ossWallet.ui.components.*
import com.osscabinet.ossWallet.ui.components.CardTypeOptionData
import com.osscabinet.ossWallet.ui.components.GenericAddCardDialog
import com.osscabinet.ossWallet.ui.components.navigation.ExpressiveNavigationBar
import com.osscabinet.ossWallet.ui.components.navigation.NavigationTab
import com.osscabinet.ossWallet.ui.components.AeonExpandableFab
import com.osscabinet.ossWallet.domain.model.timeline.TimelineRange
import com.osscabinet.ossWallet.domain.service.TimelineService
import com.osscabinet.ossWallet.ui.dashboard.components.NetWorthCard
import com.osscabinet.ossWallet.ui.components.transactions.RecentActivitiesCard
import com.osscabinet.ossWallet.ui.dashboard.components.AccountsCard
import com.osscabinet.ossWallet.ui.dashboard.components.CashFlowCard
import com.osscabinet.ossWallet.ui.dashboard.components.CurrencyBalancesCard
import com.osscabinet.ossWallet.ui.components.cards.UnifiedCardRenderer
import com.osscabinet.ossWallet.domain.card.*
import com.osscabinet.ossWallet.ui.components.cards.CardCallbacks
import com.osscabinet.ossWallet.ui.analytics.components.CategoryBreakdownData
import com.osscabinet.ossWallet.ui.analytics.components.CategoryExpenseTreemapData
import com.osscabinet.ossWallet.ui.components.cards.CardWithCallbacks
import com.osscabinet.ossWallet.ui.components.transactions.AeonTransactionItem
import com.osscabinet.ossWallet.ui.viewmodel.NetWorthUiState
import java.math.BigDecimal
import java.math.RoundingMode
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

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

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class, ExperimentalFoundationApi::class)
@Composable
fun DashboardScreen(
    onAnalyticsClick: () -> Unit,
    onRecordsClick: (com.osscabinet.ossWallet.ui.navigation.RecordsFilterContext) -> Unit,
    onAddClick: (Int?, String?) -> Unit,
    onManageAccountsClick: () -> Unit,
    onAccountClick: (String) -> Unit,
    onAccountsViewClick: () -> Unit,
    onExchangeRatesClick: () -> Unit,
    onTemplatesClick: () -> Unit,
    onSettingsClick: () -> Unit,
    onEditTransactionClick: (Long) -> Unit,
    onCloneTransactionClick: (Long) -> Unit = {},
    accountViewModel: AccountViewModel,
    transactionViewModel: TransactionViewModel,
    settingsViewModel: SettingsViewModel,
    analyticsViewModel: AnalyticsViewModel,
    templateViewModel: TemplateViewModel,
    dashboardViewModel: DashboardViewModel,
    cardManagerViewModel: CardManagerViewModel,
    cardComputerRegistry: CardComputerRegistry,
    viewContext: String? = null
) {
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()

    var navTriggered by remember { mutableStateOf(false) }
    
    // Reset navTriggered when returning to this screen
    androidx.lifecycle.compose.LifecycleEventEffect(androidx.lifecycle.Lifecycle.Event.ON_RESUME) {
        navTriggered = false
    }

    // Apply initial view focus when screen opens with context
    LaunchedEffect(viewContext) {
        viewContext?.let { contextString ->
            try {
                val decodedContext = URLDecoder.decode(contextString, StandardCharsets.UTF_8.toString())
                val context = Json.decodeFromString<DashboardViewContext>(decodedContext)
                dashboardViewModel.initializeDashboardView(context)
            } catch (e: Exception) {
                // If context is invalid, use default
                dashboardViewModel.initializeDashboardView(DashboardViewContext.None)
            }
        } ?: run {
            // If no context provided, ensure view focus is cleared
            dashboardViewModel.clearViewFocus()
        }
    }

    // Clear view focus when leaving the screen
    DisposableEffect(Unit) {
        onDispose {
            dashboardViewModel.clearViewFocus()
        }
    }

    val navigate: (() -> Unit) -> Unit = { action ->
        if (!navTriggered) {
            navTriggered = true
            action()
        }
    }

    val dashboardUiState by dashboardViewModel.uiState.collectAsStateWithLifecycle()
    val viewFocus by dashboardViewModel.viewFocus.collectAsStateWithLifecycle()
    val initialViewContext by dashboardViewModel.initialViewContext.collectAsStateWithLifecycle()
    val accounts by accountViewModel.accounts.collectAsStateWithLifecycle()
    val nonArchivedAccounts = remember(accounts) { accounts.filter { !it.isArchived } }
    val cardsConfig by cardManagerViewModel.cardsConfig.collectAsStateWithLifecycle()

    val currentProfile by settingsViewModel.currentProfile.collectAsStateWithLifecycle()
    val currentTheme by settingsViewModel.theme.collectAsStateWithLifecycle(initialValue = "System")
    val cardAnimationEnabled by settingsViewModel.cardAnimationEnabled.collectAsStateWithLifecycle(initialValue = true)
    
    var showAddCardDialog by remember { mutableStateOf(false) }

    val pageCount = 2
    val pagerState = remember {
        object : PagerState() {
            override val pageCount: Int get() = pageCount
        }
    }
    val tabs = listOf(
        NavigationTab(
            icon = Icons.Rounded.Dashboard,
            selectedIcon = Icons.Rounded.Dashboard,
            label = "Glance",
            contentDescription = "Glance tab"
        ),
        NavigationTab(
            icon = Icons.Rounded.History,
            selectedIcon = Icons.Rounded.History,
            label = "Recent Activities",
            contentDescription = "Recent Activities tab"
        )
    )

    val starredTemplates by templateViewModel.starredTemplates.collectAsStateWithLifecycle()

    val allTemplates by templateViewModel.templates.collectAsStateWithLifecycle()

    // We no longer need this LaunchedEffect because AnalyticsViewModel
    // now observes settingsDataStore.currency directly in its init block.
    // However, we might still want to ensure it's synced if it was overridden by other screens.
    val settingsCurrency by settingsViewModel.currency.collectAsStateWithLifecycle()
    LaunchedEffect(settingsCurrency) {
        analyticsViewModel.onCurrencySelected(settingsCurrency)
    }

    Box(modifier = Modifier.fillMaxSize()) {
        ModalNavigationDrawer(
            drawerState = drawerState,
            modifier = Modifier.fillMaxSize(),
            drawerContent = {
                AppDrawerContent(
                    currentProfile = currentProfile,
                    nonArchivedAccounts = nonArchivedAccounts,
                    starredTemplates = starredTemplates,
                    selectedDestination = "dashboard",
                    onDashboardClick = { scope.launch { drawerState.close() } },
                    onAccountsViewClick = { navigate(onAccountsViewClick) },
                    onRecordsClick = { navigate { onRecordsClick(com.osscabinet.ossWallet.ui.navigation.RecordsFilterContext.None) } },
                    onAnalyticsClick = { navigate(onAnalyticsClick) },
                    onExchangeRatesClick = { navigate(onExchangeRatesClick) },
                    onTemplatesClick = { navigate(onTemplatesClick) },
                    onManageAccountsClick = { navigate(onManageAccountsClick) },
                    onSettingsClick = { navigate(onSettingsClick) },
                    onAccountClick = { id -> navigate { onAccountClick(id) } },
                    onTemplateClick = { template -> navigate { onAddClick(template.transactionType, template.name) } },
                    onCloseDrawer = { scope.launch { drawerState.close() } }
                )
            }
        ) {
            Scaffold(
                topBar = {
                    TopAppBar(
                        navigationIcon = {
                            IconButton(onClick = { scope.launch { drawerState.open() } }) {
                                Icon(Icons.Default.Menu, contentDescription = "Menu")
                            }
                        },
                        title = {
                            Column(modifier = Modifier.clickable { navigate(onSettingsClick) }) {
                                Text(currentProfile?.name ?: "Intelligence", style = MaterialTheme.typography.titleLarge)
                                Text("Personal Account", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.outline)
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
            ) { innerPadding ->
                // Context indicator banner (if view focus is applied)
                if (initialViewContext != DashboardViewContext.None) {
                    DashboardViewIndicator(
                        context = initialViewContext,
                        onReset = { dashboardViewModel.resetToInitialView() }
                    )
                }

                when (val state = dashboardUiState) {
                    is DashboardUiState.Loading -> {
                        AeonShimmer(innerPadding = innerPadding)
                    }
                    is DashboardUiState.Success -> {
                            DashboardContent(
                                innerPadding = innerPadding,
                                pagerState = pagerState,
                                tabs = tabs,
                                state = state,
                                currentTheme = currentTheme,
                                gridColumns = state.gridColumns,
                                decimalPrecision = state.decimalPrecision,
                                onManageAccountsClick = { navigate(onManageAccountsClick) },
                                onAccountsViewClick = { navigate(onAccountsViewClick) },
                                onAccountClick = { id -> navigate { onAccountClick(id) } },
                                onRecordsClick = { context -> navigate { onRecordsClick(context) } },
                                onEditTransactionClick = { id -> navigate { onEditTransactionClick(id) } },
                                onCloneTransactionClick = { id -> navigate { onCloneTransactionClick(id) } },
                                transactionViewModel = transactionViewModel,
                                dashboardViewModel = dashboardViewModel,
                                cardManagerViewModel = cardManagerViewModel,
                                cardComputerRegistry = cardComputerRegistry,
                                cardsConfig = cardsConfig,
                                showAddCardDialog = showAddCardDialog,
                                onShowAddCardDialogChange = { showAddCardDialog = it },
                                cardAnimationEnabled = cardAnimationEnabled
                            )
                    }
                    is DashboardUiState.Error -> {
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Text(state.message)
                        }
                    }
                }
            }

            AeonExpandableFab(
                onAddClick = { type, name -> navigate { onAddClick(type, name) } },
                templates = allTemplates,
                fabPaddingBottom = 96.dp
            )
        }
    }
}


@OptIn(ExperimentalFoundationApi::class)
@Composable
fun DashboardContent(
    innerPadding: PaddingValues,
    pagerState: androidx.compose.foundation.pager.PagerState,
    tabs: List<NavigationTab>,
    state: DashboardUiState.Success,
    currentTheme: String,
    gridColumns: Int,
    decimalPrecision: Int,
    onManageAccountsClick: () -> Unit,
    onAccountsViewClick: () -> Unit,
    onAccountClick: (String) -> Unit,
    onRecordsClick: (com.osscabinet.ossWallet.ui.navigation.RecordsFilterContext) -> Unit,
    onEditTransactionClick: (Long) -> Unit,
    onCloneTransactionClick: (Long) -> Unit = {},
    transactionViewModel: TransactionViewModel,
    dashboardViewModel: DashboardViewModel,
    cardManagerViewModel: CardManagerViewModel,
    cardComputerRegistry: CardComputerRegistry,
    cardsConfig: DashboardCardsConfig,
    showAddCardDialog: Boolean,
    onShowAddCardDialogChange: (Boolean) -> Unit,
    cardAnimationEnabled: Boolean
) {
    HorizontalPager(
        state = pagerState,
        modifier = Modifier
            .fillMaxSize()
            .padding(innerPadding),
        verticalAlignment = Alignment.Top
    ) { page ->
        when (page) {
            0 -> {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(MaterialTheme.colorScheme.background)
                ) {
                    // Header row with Add Card button
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Glance",
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold
                        )
                        IconButton(
                            onClick = { onShowAddCardDialogChange(true) }
                        ) {
                            Icon(
                                imageVector = Icons.Default.Add,
                                contentDescription = "Add card",
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                    
                    CardReorderableList(
                        items = cardsConfig.cards.sortedBy { it.order },
                        key = { it.cardId },
                        onReorder = { reorderedCards ->
                            cardManagerViewModel.reorderDashboardCards(reorderedCards)
                        },
                        modifier = Modifier.weight(1f),
                        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = calculateScrollBottomPadding()),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                        animateCards = cardAnimationEnabled
                    ) { cardConfig, isDragging, dragModifier ->
                        val input = CardInput(
                            transactions = state.allTransactions,
                            accounts = state.accounts,
                            categories = emptyList(),
                            exchangeRates = state.exchangeRates,
                            baseCurrency = state.baseCurrency,
                            precision = state.decimalPrecision
                        )

                        val cardWithCallbacks = rememberCardWithCallbacks(
                            config = cardConfig,
                            input = input,
                            registry = cardComputerRegistry,
                            updateHandler = dashboardViewModel,
                            onDelete = { cardManagerViewModel.removeCard(it) },
                            onRecordsClick = onRecordsClick,
                            onManageAccountsClick = onManageAccountsClick,
                            onAccountsViewClick = onAccountsViewClick,
                            onAccountClick = onAccountClick,
                            currentTheme = currentTheme
                        )

                        cardWithCallbacks?.let {
                            UnifiedCardRenderer(
                                renderData = it.renderData,
                                callbacks = it.callbacks,
                                modifier = Modifier.fillMaxWidth(),
                                headerModifier = dragModifier,
                                showTitle = true,
                                currentTheme = currentTheme,
                                accounts = state.accounts,
                                allCategories = emptyList(),
                                savedFilters = emptyList()
                            )
                        } ?: Box(modifier = Modifier.size(0.dp))
                    }
                }
            }
            1 -> {
                val accountMap = remember(state.accounts) { state.accounts.associate { it.id to it.name } }
                val accountCurrencyMap = remember(state.accounts) { state.accounts.associate { it.id to it.currencyCode } }
                val categories by transactionViewModel.categories.collectAsStateWithLifecycle()
                val categoryMap = remember(categories) { categories.associateBy { it.id } }
                val revealedCardIds by transactionViewModel.revealedCardIdsList.collectAsStateWithLifecycle()

                RecentActivitiesCard(
                    allTransactions = state.allTransactionsWithBalance,
                    accountNamesMap = accountMap,
                    categoryMap = categoryMap,
                    decimalPrecision = state.decimalPrecision,
                    onRecordsClick = onRecordsClick,
                    onTransactionClick = onEditTransactionClick,
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
    
    // Add Card Dialog
    if (showAddCardDialog) {
        AddCardDialog(
            onDismiss = { onShowAddCardDialogChange(false) },
            onAddCard = { cardType ->
                val unifiedType = CardType.entries.find { it.serialName == cardType }
                if (unifiedType != null) {
                    cardManagerViewModel.addDashboardCard(unifiedType)
                }
                onShowAddCardDialogChange(false)
            }
        )
    }
}

@Composable
fun AddCardDialog(
    onDismiss: () -> Unit,
    onAddCard: (String) -> Unit
) {
    GenericAddCardDialog(
        onDismiss = onDismiss,
        cardOptions = CardType.entries.filter { it != CardType.ACCOUNT_HERO && it != CardType.BUDGETS }.map { type ->
            CardTypeOptionData(
                title = type.displayName,
                description = "", // TODO: Add descriptions to CardType
                icon = when (type) {
                    CardType.NET_WORTH -> Icons.Default.AccountBalance
                    CardType.CURRENCY_BALANCES -> Icons.Default.AttachMoney
                    CardType.CASH_FLOW -> Icons.AutoMirrored.Filled.TrendingUp
                    CardType.ACCOUNTS -> Icons.Default.AccountBox
                    CardType.CATEGORY_BREAKDOWN -> Icons.Default.PieChart
                    CardType.CATEGORY_EXPENSE_TREEMAP -> Icons.Default.GridView
                    else -> null
                },
                onClick = { onAddCard(type.serialName) }
            )
        }
    )
}

@Composable
fun DashboardViewIndicator(
    context: DashboardViewContext,
    onReset: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.primaryContainer)
            .padding(16.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.Visibility,
                contentDescription = "View focus",
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(16.dp)
            )
            Text(
                text = getDashboardContextDisplayName(context),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.primary
            )
        }
        TextButton(onClick = onReset) {
            Text("Reset")
        }
    }
}

private fun getDashboardContextDisplayName(context: DashboardViewContext): String {
    return when (context) {
        is DashboardViewContext.None -> "Default"
        is DashboardViewContext.FocusCard -> "Card: ${context.cardId}"
        is DashboardViewContext.FocusSection -> "Section: ${context.section.name}"
    }
}