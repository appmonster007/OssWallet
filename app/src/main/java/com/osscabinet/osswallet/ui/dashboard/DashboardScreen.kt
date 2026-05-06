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
import com.osscabinet.ossWallet.domain.model.TransactionFilter
import com.osscabinet.ossWallet.ui.viewmodel.AccountViewModel
import com.osscabinet.ossWallet.ui.viewmodel.TransactionViewModel
import com.osscabinet.ossWallet.ui.viewmodel.SettingsViewModel
import com.osscabinet.ossWallet.ui.viewmodel.AnalyticsViewModel
import com.osscabinet.ossWallet.ui.util.formatCurrencyRight
import com.osscabinet.ossWallet.ui.util.toContainerColor
import com.osscabinet.ossWallet.ui.util.toOnContainerColor
import com.osscabinet.ossWallet.ui.viewmodel.TemplateViewModel
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
import com.osscabinet.ossWallet.ui.components.navigation.UnifiedPillNavigation
import com.osscabinet.ossWallet.ui.components.navigation.NavigationTab
import com.osscabinet.ossWallet.ui.components.navigation.TransactionFabMenu
import com.osscabinet.ossWallet.domain.model.timeline.TimelineRange
import com.osscabinet.ossWallet.domain.service.TimelineService
import com.osscabinet.ossWallet.ui.dashboard.components.NetWorthCard
import com.osscabinet.ossWallet.ui.components.transactions.RecentActivitiesScreen
import com.osscabinet.ossWallet.ui.dashboard.components.AccountsCard
import com.osscabinet.ossWallet.ui.dashboard.components.CashFlowCard
import com.osscabinet.ossWallet.ui.dashboard.components.CurrencyBalancesCard
import com.osscabinet.ossWallet.ui.components.cards.UnifiedCardRenderer
import com.osscabinet.ossWallet.domain.card.*
import com.osscabinet.ossWallet.ui.components.cards.CardCallbacks
import com.osscabinet.ossWallet.ui.analytics.components.CategoryPieChartData
import com.osscabinet.ossWallet.ui.analytics.components.CategoryTreemapData
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
    onMenuClick: () -> Unit,
    onRecordsClick: (com.osscabinet.ossWallet.ui.navigation.RecordsFilterContext) -> Unit,
    onAddClick: (Int?, String?) -> Unit,
    onManageAccountsClick: () -> Unit,
    onAccountClick: (String) -> Unit,
    onSettingsClick: () -> Unit,
    onEditTransactionClick: (Long) -> Unit,
    onCloneTransactionClick: (Long) -> Unit = {},
    accountViewModel: AccountViewModel,
    transactionViewModel: TransactionViewModel,
    settingsViewModel: SettingsViewModel,
    analyticsViewModel: AnalyticsViewModel,
    templateViewModel: TemplateViewModel,
    dashboardViewModel: DashboardViewModel,
    cardComputerRegistry: CardComputerRegistry,
    viewContext: String? = null
) {
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
    val initialViewContext by dashboardViewModel.initialViewContext.collectAsStateWithLifecycle()
    val accounts by accountViewModel.accounts.collectAsStateWithLifecycle()
    val cardsConfig by dashboardViewModel.cardsConfig.collectAsStateWithLifecycle()

    val currentProfile by settingsViewModel.currentProfile.collectAsStateWithLifecycle()
    val currentTheme by settingsViewModel.theme.collectAsStateWithLifecycle(initialValue = "System")
    val cardAnimationEnabled by settingsViewModel.cardAnimationEnabled.collectAsStateWithLifecycle(initialValue = true)
    val expandableFabEnabled by settingsViewModel.expandableFabEnabled.collectAsStateWithLifecycle(initialValue = true)
    
    var showAddCardDialog by remember { mutableStateOf(false) }
    var isFabExpanded by remember { mutableStateOf(false) }

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

    val allTemplates by templateViewModel.templates.collectAsStateWithLifecycle()
    val scope = rememberCoroutineScope()

    Box(modifier = Modifier.fillMaxSize()) {
        Scaffold(
            topBar = {
                TopAppBar(
                    navigationIcon = {
                        IconButton(onClick = onMenuClick) {
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
                        onManageAccountsClick = { navigate(onManageAccountsClick) },
                        onAccountClick = { id -> navigate { onAccountClick(id) } },
                        onRecordsClick = { context -> navigate { onRecordsClick(context) } },
                        onEditTransactionClick = { id -> navigate { onEditTransactionClick(id) } },
                        onCloneTransactionClick = { id -> navigate { onCloneTransactionClick(id) } },
                        transactionViewModel = transactionViewModel,
                        dashboardViewModel = dashboardViewModel,
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
                    navigate { onAddClick(0, null) }
                }
            },
            fabMenu = if (expandableFabEnabled) {
                {
                    TransactionFabMenu(
                        isExpanded = isFabExpanded,
                        templates = allTemplates,
                        onAddClick = { type, name ->
                            isFabExpanded = false
                            navigate { onAddClick(type, name) }
                        }
                    )
                }
            } else null
        )
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
    onManageAccountsClick: () -> Unit,
    onAccountClick: (String) -> Unit,
    onRecordsClick: (com.osscabinet.ossWallet.ui.navigation.RecordsFilterContext) -> Unit,
    onEditTransactionClick: (Long) -> Unit,
    onCloneTransactionClick: (Long) -> Unit = {},
    transactionViewModel: TransactionViewModel,
    dashboardViewModel: DashboardViewModel,
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
                    
                    val savedFilters by transactionViewModel.savedFilters.collectAsStateWithLifecycle()
                    
                    AeonCardList(
                        cards = cardsConfig.cards,
                        cardInput = CardInput(
                            transactions = state.allTransactions,
                            accounts = state.accounts,
                            categories = state.allCategories,
                            exchangeRates = state.exchangeRates,
                            baseCurrency = state.baseCurrency,
                            precision = state.decimalPrecision,
                            plannedPayments = state.plannedPayments,
                            budgets = state.budgets,
                            loans = state.loans,
                            filter = TransactionFilter(excludeFuture = false)
                        ),
                        cardComputerRegistry = cardComputerRegistry,
                        cardUpdateHandler = dashboardViewModel,
                        onReorder = { dashboardViewModel.reorderCards(it) },
                        onDelete = { dashboardViewModel.removeCard(it) },
                        onRecordsClick = onRecordsClick,
                        modifier = Modifier.weight(1f),
                        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = calculateScrollBottomPadding()),
                        animateCards = cardAnimationEnabled,
                        currentTheme = currentTheme,
                        accounts = state.accounts,
                        allCategories = state.allCategories,
                        savedFilters = savedFilters,
                        onManageAccountsClick = onManageAccountsClick,
                        onAccountClick = onAccountClick,
                        onPlannedPaymentConfirm = { id -> dashboardViewModel.quickConfirmById(id) },
                        onPlannedPaymentSkip = { id -> dashboardViewModel.quickSkipById(id) }
                    )
                }
            }
            1 -> {
                val accountMap = remember(state.accounts) { state.accounts.associate { it.id to it.name } }
                val categories by transactionViewModel.categories.collectAsStateWithLifecycle()
                val categoryMap = remember(categories) { categories.associateBy { it.id } }
                val revealedCardIds by transactionViewModel.revealedCardIdsList.collectAsStateWithLifecycle()

                RecentActivitiesScreen(
                    allTransactions = state.allTransactionsWithBalance,
                    accountNamesMap = accountMap,
                    categoryMap = categoryMap,
                    onRecordsClick = onRecordsClick,
                    onTransactionClick = onEditTransactionClick,
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
                    precision = state.decimalPrecision,
                    is24Hour = state.use24HourFormat
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
                    dashboardViewModel.addCard(unifiedType)
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
        cardOptions = CardType.entries.filter { it.isUserAddableOn(ScreenContext.DASHBOARD) && it != CardType.BUDGETS }.map { type ->
            CardTypeOptionData(
                title = type.displayName,
                description = "", // TODO: Add descriptions to CardType
                icon = when (type) {
                    CardType.NET_WORTH -> Icons.Default.AccountBalance
                    CardType.CURRENCY_BALANCES -> Icons.Default.AttachMoney
                    CardType.CASH_FLOW -> Icons.AutoMirrored.Filled.TrendingUp
                    CardType.ACCOUNTS -> Icons.Default.AccountBox
                    CardType.CATEGORY_PIE_CHART -> Icons.Default.PieChart
                    CardType.CATEGORY_TREEMAP -> Icons.Default.GridView
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