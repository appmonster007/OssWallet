package com.osscabinet.ossWallet.ui.screens.dashboard

import com.osscabinet.ossWallet.ui.screens.accounts.*
import com.osscabinet.ossWallet.ui.screens.transactions.*
import com.osscabinet.ossWallet.ui.screens.templates.*
import com.osscabinet.ossWallet.ui.screens.settings.*
import com.osscabinet.ossWallet.ui.screens.analytics.*
import com.osscabinet.ossWallet.ui.util.BaseViewModel
import com.osscabinet.ossWallet.ui.util.GenericViewModelFactory

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PagerState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.rounded.Dashboard
import androidx.compose.material.icons.rounded.History
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.osscabinet.ossWallet.domain.card.CardComputerRegistry
import com.osscabinet.ossWallet.domain.card.CardInput
import com.osscabinet.ossWallet.domain.card.CardType
import com.osscabinet.ossWallet.domain.model.TransactionFilter
import com.osscabinet.ossWallet.domain.model.TransactionType
import com.osscabinet.ossWallet.domain.model.TransactionListState
import com.osscabinet.ossWallet.domain.model.settings.DashboardCardsConfig
import com.osscabinet.ossWallet.domain.model.settings.ScreenContext
import com.osscabinet.ossWallet.ui.components.AeonShimmer
import com.osscabinet.ossWallet.ui.components.CardTypeSelectionDialog
import com.osscabinet.ossWallet.ui.components.cards.AeonCardList
import com.osscabinet.ossWallet.ui.components.cards.CardListCallbacks
import com.osscabinet.ossWallet.ui.components.cards.FilterSelectionContext
import com.osscabinet.ossWallet.ui.components.navigation.NavigationTab
import com.osscabinet.ossWallet.ui.components.navigation.TransactionFabMenu
import com.osscabinet.ossWallet.ui.components.navigation.UnifiedPillNavigation
import com.osscabinet.ossWallet.ui.components.transactions.RecentActivitiesScreen
import com.osscabinet.ossWallet.ui.components.transactions.TransactionItemEvent
import com.osscabinet.ossWallet.ui.navigation.DashboardViewContext
import com.osscabinet.ossWallet.ui.screens.accounts.AccountViewModel
import com.osscabinet.ossWallet.ui.screens.analytics.AnalyticsViewModel
import com.osscabinet.ossWallet.ui.screens.settings.SettingsViewModel
import com.osscabinet.ossWallet.ui.screens.templates.TemplateViewModel
import com.osscabinet.ossWallet.ui.screens.transactions.TransactionViewModel
import com.osscabinet.ossWallet.ui.screens.dashboard.DashboardViewModel
import com.osscabinet.ossWallet.ui.theme.AeonTheme
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import java.net.URLDecoder
import java.nio.charset.StandardCharsets
import com.osscabinet.ossWallet.domain.card.NetWorthMetrics
import com.osscabinet.ossWallet.domain.model.Budget
import com.osscabinet.ossWallet.domain.model.BudgetAlertLevel
import com.osscabinet.ossWallet.domain.model.BudgetProgress
import com.osscabinet.ossWallet.domain.model.analytics.CashFlowResult
import com.osscabinet.ossWallet.domain.model.settings.BudgetsCardConfig
import com.osscabinet.ossWallet.domain.model.settings.NetWorthCardConfig
import com.osscabinet.ossWallet.domain.model.timeline.ThisPeriodType
import com.osscabinet.ossWallet.domain.model.timeline.TimelineRange
import com.osscabinet.ossWallet.ui.screens.dashboard.components.BudgetsCard
import com.osscabinet.ossWallet.ui.screens.dashboard.components.NetWorthCard
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Surface
import java.math.BigDecimal
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

@OptIn(
    ExperimentalMaterial3Api::class,
    ExperimentalLayoutApi::class,
    ExperimentalFoundationApi::class
)
@Composable
fun DashboardScreen(
    onMenuClick: () -> Unit,
    onBackClick: () -> Unit = {},
    isTopLevelParam: Boolean = false,
    onRecordsClick: (com.osscabinet.ossWallet.domain.model.TransactionFilter) -> Unit,
    onAddClick: (TransactionType?, Long?) -> Unit,
    onManageAccountsClick: () -> Unit,
    onAccountClick: (String) -> Unit,
    onSettingsClick: () -> Unit,
    onEditTransactionClick: (Long) -> Unit,
    onCloneTransactionClick: (Long) -> Unit = {},
    onSplitTransactionClick: (Long) -> Unit = {},
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
                val decodedContext =
                    URLDecoder.decode(contextString, StandardCharsets.UTF_8.toString())
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
    val cardsConfig by dashboardViewModel.cardsConfig.collectAsStateWithLifecycle()

    val currentProfile by settingsViewModel.currentProfile.collectAsStateWithLifecycle()
    val currentTheme by settingsViewModel.theme.collectAsStateWithLifecycle(initialValue = "System")
    val cardAnimationEnabled by settingsViewModel.cardAnimationEnabled.collectAsStateWithLifecycle(
        initialValue = true
    )
    val expandableFabEnabled by settingsViewModel.expandableFabEnabled.collectAsStateWithLifecycle(
        initialValue = true
    )

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
    val allCategories by transactionViewModel.categories.collectAsStateWithLifecycle()
    val scope = rememberCoroutineScope()

    val isTopLevel = remember(initialViewContext, isTopLevelParam) {
        isTopLevelParam || initialViewContext == DashboardViewContext.None
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Scaffold(
            topBar = {
                TopAppBar(
                    navigationIcon = {
                        if (isTopLevel) {
                            IconButton(onClick = onMenuClick) {
                                Icon(Icons.Default.Menu, contentDescription = "Menu")
                            }
                        } else {
                            IconButton(onClick = onBackClick) {
                                Icon(
                                    Icons.AutoMirrored.Filled.ArrowBack,
                                    contentDescription = "Back"
                                )
                            }
                        }
                    },
                    title = {
                        Column(
                            modifier = Modifier.clickable { navigate(onSettingsClick) },
                            verticalArrangement = Arrangement.spacedBy(AeonTheme.dimensions.spacingXs)
                        ) {
                            Text(
                                currentProfile?.name ?: "Intelligence",
                                style = AppTextStyles.screenTitle
                            )
                            Text(
                                "Personal Account",
                                style = AppTextStyles.transactionSwipeAction,
                                color = AppColors.border
                            )
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
                        onSplitTransactionClick = { id -> navigate { onSplitTransactionClick(id) } },
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
                    navigate { onAddClick(TransactionType.EXPENSE, null) }
                }
            },
            fabMenu = if (expandableFabEnabled) {
                {
                    TransactionFabMenu(
                        isExpanded = isFabExpanded,
                        templates = allTemplates,
                        categories = allCategories,
                        onAddClick = { type, templateId ->
                            isFabExpanded = false
                            navigate { onAddClick(type, templateId) }
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
    pagerState: PagerState,
    tabs: List<NavigationTab>,
    state: DashboardUiState.Success,
    currentTheme: String,
    onManageAccountsClick: () -> Unit,
    onAccountClick: (String) -> Unit,
    onRecordsClick: (com.osscabinet.ossWallet.domain.model.TransactionFilter) -> Unit,
    onEditTransactionClick: (Long) -> Unit,
    onCloneTransactionClick: (Long) -> Unit = {},
    onSplitTransactionClick: (Long) -> Unit = {},
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
                        .background(AppColors.screenBackground)
                ) {
                // Header row with Add Card button
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = AeonTheme.dimensions.screenPadding, vertical = AeonTheme.dimensions.spacingMd),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Glance",
                        style = AppTextStyles.screenTitle,
                        fontWeight = FontWeight.Bold
                    )
                    IconButton(
                        onClick = { onShowAddCardDialogChange(true) },
                        modifier = Modifier.size(AeonTheme.dimensions.iconSizeLg)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = "Add card",
                            tint = AppColors.primary,
                            modifier = Modifier.size(AeonTheme.dimensions.iconSizeSm)
                        )
                    }
                }

                    val savedFilters by transactionViewModel.savedFilters.collectAsStateWithLifecycle()

                    AeonCardList(
                        cards = cardsConfig.cards,
                        cardDataMap = state.cardDataMap,
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
                        contentPadding = PaddingValues(
                            start = AeonTheme.dimensions.screenPadding,
                            end = AeonTheme.dimensions.screenPadding,
                            top = AeonTheme.dimensions.spacingMd,
                            bottom = calculateScrollBottomPadding()
                        ),
                        animateCards = cardAnimationEnabled,
                        currentTheme = currentTheme,
                        filterContext = FilterSelectionContext(
                            accounts = state.accounts,
                            allCategories = state.allCategories,
                            savedFilters = savedFilters
                        ),
                        callbacks = CardListCallbacks(
                            onManageAccountsClick = onManageAccountsClick,
                            onAccountClick = onAccountClick,
                            onTransactionClick = onEditTransactionClick,
                            onPlannedPaymentConfirm = { id -> dashboardViewModel.quickConfirmById(id) },
                            onPlannedPaymentSkip = { id -> dashboardViewModel.quickSkipById(id) },
                            onItemExpanded = { transactionViewModel.onItemExpanded(it) },
                            onItemCollapsed = { transactionViewModel.onItemCollapsed(it) },
                            onStatusChange = { id, status -> transactionViewModel.quickUpdateStatus(id, status) },
                            onClone = { onCloneTransactionClick(it) },
                            onDeleteTransaction = { transactionViewModel.quickDeleteTransaction(it) },
                            onSplit = { onSplitTransactionClick(it) },
                            onFullSwipeRight = { transactionViewModel.toggleStatusClearedUncleared(it) },
                        ),
                    )
                }
            }

            1 -> {
                val revealedCardIds by transactionViewModel.revealedCardIdsList.collectAsStateWithLifecycle()

                RecentActivitiesScreen(
                    listState = state.recentActivitiesState,
                    onRecordsClick = onRecordsClick,
                    baseCurrency = state.baseCurrency,
                    contentPadding = PaddingValues(bottom = calculateScrollBottomPadding()),
                    modifier = Modifier.fillMaxSize(),
                    revealedCardIds = revealedCardIds,
                    onTransactionEvent = { event ->
                        when (event) {
                            is TransactionItemEvent.Click -> onEditTransactionClick(event.id)
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
                    is24Hour = state.use24HourFormat
                )
            }
        }
    }

    // Add Card Dialog
    if (showAddCardDialog) {
        CardTypeSelectionDialog(
            screenContext = ScreenContext.DASHBOARD,
            onDismiss = { onShowAddCardDialogChange(false) },
            onCardTypeSelected = { cardType ->
                dashboardViewModel.addCard(cardType)
                onShowAddCardDialogChange(false)
            },
            additionalFilter = { it != CardType.BUDGETS }
        )
    }
}

@Composable
fun DashboardViewIndicator(
    context: DashboardViewContext,
    onReset: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(AppColors.accentContainer)
            .padding(horizontal = AeonTheme.dimensions.screenPadding, vertical = AeonTheme.dimensions.spacingMd),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(AeonTheme.dimensions.spacingSm),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.Visibility,
                contentDescription = "View focus",
                tint = AppColors.primary,
                modifier = Modifier.size(AeonTheme.dimensions.iconSizeXs)
            )
            Text(
                text = getDashboardContextDisplayName(context),
                style = AppTextStyles.transactionTitle,
                color = AppColors.primary
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

// --- PREVIEWS ---

@Composable
private fun DashboardReviewContent() {
    val netWorthMetrics = NetWorthMetrics(
        netWorth = BigDecimal("125000.50"),
        cashFlow = CashFlowResult.empty().copy(
            totalIncomeInBase = BigDecimal("5000.00"),
            totalExpensesInBase = BigDecimal("3200.00")
        )
    )

    val budget1 = Budget(
        id = 1L, name = "Food", currency = "USD", limitAmount = BigDecimal("1000.00"),
        timelineRange = TimelineRange.AllTime, profileId = 1
    )
    val budgets = listOf(
        BudgetProgress(
            budget = budget1,
            spent = BigDecimal("450.00"),
            limit = BigDecimal("1000.00"),
            remaining = BigDecimal("550.00"),
            percentage = 0.45f,
            alertLevel = BudgetAlertLevel.NONE
        )
    )

    Column(
        modifier = Modifier
            .padding(16.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        NetWorthCard(
            currentMetrics = netWorthMetrics,
            baseCurrency = "USD",
            precision = 2,
            config = NetWorthCardConfig(cardId = "networth")
        )

        BudgetsCard(
            items = budgets,
            config = BudgetsCardConfig(cardId = "budgets", title = "Budgets")
        )
    }
}

@Preview(name = "Dashboard - Light Mode", showBackground = true)
@Composable
fun DashboardLightPreview() {
    com.osscabinet.ossWallet.ui.theme.OssWalletTheme(theme = "Light") {
        Surface(color = AppColors.screenBackground) {
            DashboardReviewContent()
        }
    }
}

@Preview(name = "Dashboard - Dark Mode", showBackground = true)
@Composable
fun DashboardDarkPreview() {
    com.osscabinet.ossWallet.ui.theme.OssWalletTheme(theme = "Dark") {
        Surface(color = AppColors.screenBackground) {
            DashboardReviewContent()
        }
    }
}

@Preview(name = "Dashboard - Amoled Mode", showBackground = true)
@Composable
fun DashboardAmoledPreview() {
    com.osscabinet.ossWallet.ui.theme.OssWalletTheme(theme = "Amoled") {
        Surface(color = AppColors.screenBackground) {
            DashboardReviewContent()
        }
    }
}
