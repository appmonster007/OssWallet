package com.osscabinet.ossWallet.ui.screens.dashboard

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.pager.rememberPagerState
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
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.osscabinet.ossWallet.domain.card.CardComputerRegistry
import com.osscabinet.ossWallet.domain.card.CardInput
import com.osscabinet.ossWallet.domain.card.CardUpdateHandler
import com.osscabinet.ossWallet.domain.model.Filter
import com.osscabinet.ossWallet.domain.model.Transaction
import com.osscabinet.ossWallet.domain.model.TransactionFilter
import com.osscabinet.ossWallet.domain.model.timeline.TimelineRange
import com.osscabinet.ossWallet.domain.model.TransactionListState
import com.osscabinet.ossWallet.domain.model.TransactionType
import com.osscabinet.ossWallet.domain.model.settings.CardConfig
import com.osscabinet.ossWallet.domain.model.settings.DashboardCardsConfig
import com.osscabinet.ossWallet.domain.model.settings.ScreenContext
import com.osscabinet.ossWallet.ui.components.AeonHazeProvider
import com.osscabinet.ossWallet.ui.components.aeonHazeSource
import com.osscabinet.ossWallet.ui.components.AeonTopBarActionPill
import com.osscabinet.ossWallet.ui.components.AeonTopBarNavTitlePill
import com.osscabinet.ossWallet.ui.components.AeonTopBarSection
import com.osscabinet.ossWallet.ui.components.AeonTopAppBar
import com.osscabinet.ossWallet.ui.components.AppBarIconButton
import com.osscabinet.ossWallet.ui.theme.AeonSize
import com.osscabinet.ossWallet.ui.components.AeonShimmer
import com.osscabinet.ossWallet.ui.components.CardTypeSelectionDialog
import com.osscabinet.ossWallet.ui.components.ContourBackground
import com.osscabinet.ossWallet.ui.components.cards.AccountActions
import com.osscabinet.ossWallet.ui.components.cards.AeonCardList
import com.osscabinet.ossWallet.ui.components.cards.AeonInteractionCallbacks
import com.osscabinet.ossWallet.ui.components.cards.CardWithCallbacks
import com.osscabinet.ossWallet.ui.components.cards.FilterPillController
import com.osscabinet.ossWallet.ui.components.cards.LocalFilterPillController
import com.osscabinet.ossWallet.ui.components.cards.UnifiedCardRenderer
import com.osscabinet.ossWallet.ui.components.cards.rememberCardWithCallbacks
import com.osscabinet.ossWallet.ui.components.cards.BudgetActions
import com.osscabinet.ossWallet.ui.components.cards.FilterActions
import com.osscabinet.ossWallet.ui.components.cards.FilterSelectionContext
import com.osscabinet.ossWallet.ui.components.cards.LoanActions
import com.osscabinet.ossWallet.ui.components.cards.PaymentActions
import com.osscabinet.ossWallet.ui.components.cards.TagActions
import com.osscabinet.ossWallet.ui.components.cards.TransactionActions
import com.osscabinet.ossWallet.ui.components.transactions.TransactionFilterPill
import com.osscabinet.ossWallet.ui.components.navigation.NavigationTab
import com.osscabinet.ossWallet.ui.components.navigation.TransactionFabMenu
import com.osscabinet.ossWallet.ui.components.navigation.UnifiedPillNavigation
import com.osscabinet.ossWallet.ui.components.scrollBottomPadding
import com.osscabinet.ossWallet.ui.components.transactions.RecentActivitiesScreen
import com.osscabinet.ossWallet.ui.components.transactions.TransactionItemEvent
import com.osscabinet.ossWallet.ui.navigation.DashboardViewContext
import com.osscabinet.ossWallet.ui.screens.accounts.AccountViewModel
import com.osscabinet.ossWallet.ui.screens.analytics.AnalyticsViewModel
import com.osscabinet.ossWallet.ui.screens.settings.SettingsViewModel
import com.osscabinet.ossWallet.ui.screens.templates.TemplateViewModel
import com.osscabinet.ossWallet.ui.screens.transactions.TransactionViewModel
import com.osscabinet.ossWallet.ui.navigation.SharedNavViewModels
import com.osscabinet.ossWallet.ui.theme.AeonTheme
import com.osscabinet.ossWallet.ui.theme.AppColors
import com.osscabinet.ossWallet.ui.theme.AppTextStyles
import com.osscabinet.ossWallet.ui.theme.HazeLayer
import dev.chrisbanes.haze.hazeSource
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import java.net.URLDecoder
import java.nio.charset.StandardCharsets


/**
 * A Value Object representing a valid configuration for the Dashboard.
 */
data class DashboardConfig(
    val isTopLevel: Boolean,
    val viewContextJson: String? = null
)

/**
 * Grouped actions for the Dashboard screen.
 */
data class DashboardActions(
    val onMenuClick: () -> Unit,
    val onBackClick: () -> Unit = {},
    val onRecordsClick: (TransactionFilter) -> Unit,
    val onAddClick: (TransactionType?, Long?) -> Unit,
    val onManageAccountsClick: () -> Unit,
    val onAccountClick: (String) -> Unit,
    val onNavigateToPlannedPaymentList: () -> Unit = {},
    val onNavigateToBudgetList: () -> Unit = {},
    val onNavigateToLoanList: () -> Unit = {},
    val onNavigateToPlannedPaymentDetail: (Long) -> Unit = {},
    val onNavigateToBudgetDetail: (Long) -> Unit = {},
    val onNavigateToLoanDetail: (Long) -> Unit = {},
    val onEditTransactionClick: (Long) -> Unit,
    val onSaveFilterClick: (TransactionFilter) -> Unit = {},
    val onCloneTransactionClick: (Long) -> Unit = {},
    val onSplitTransactionClick: (Long) -> Unit = {},
)

@OptIn(
    ExperimentalMaterial3Api::class,
    ExperimentalLayoutApi::class,
    ExperimentalFoundationApi::class
)
@Composable
fun DashboardScreen(
    config: DashboardConfig,
    actions: DashboardActions,
    vms: SharedNavViewModels
) {
    val dashboardViewModel = vms.dashboardViewModel
    val transactionViewModel = vms.transactionViewModel
    val settingsViewModel = vms.settingsViewModel
    val templateViewModel = vms.templateViewModel
    val cardComputerRegistry = vms.dashboardViewModel.cardComputerRegistry

    var navTriggered by remember { mutableStateOf(false) }

    // Reset navTriggered when returning to this screen
    androidx.lifecycle.compose.LifecycleEventEffect(androidx.lifecycle.Lifecycle.Event.ON_RESUME) {
        navTriggered = false
    }

    // Apply initial view focus when screen opens with context
    LaunchedEffect(config.viewContextJson) {
        config.viewContextJson?.let { contextString ->
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
    val cardAnimationEnabled by settingsViewModel.cardAnimationEnabled.collectAsStateWithLifecycle(
        initialValue = true
    )
    val expandableFabEnabled by settingsViewModel.expandableFabEnabled.collectAsStateWithLifecycle(
        initialValue = true
    )

    var showAddCardDialog by remember { mutableStateOf(false) }
    var isFabExpanded by remember { mutableStateOf(false) }

    BackHandler(enabled = showAddCardDialog) { showAddCardDialog = false }
    BackHandler(enabled = isFabExpanded) { isFabExpanded = false }

    val pageCount = 2
    val pagerState = rememberPagerState(pageCount = { pageCount })
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
    val navigationBarHeight = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
    val scope = rememberCoroutineScope()
    val glanceListState = rememberLazyListState()

    // Robust scroll reset: triggers when the Glance tab is selected or the screen is resumed.
    // This addresses the "swipe back" scenario where LifecycleEventEffect doesn't fire.
    LaunchedEffect(pagerState.currentPage) {
        if (pagerState.currentPage == 0) {
            glanceListState.scrollToItem(0)
        }
    }

    androidx.lifecycle.compose.LifecycleEventEffect(androidx.lifecycle.Lifecycle.Event.ON_RESUME) {
        if (pagerState.currentPage == 0) {
            scope.launch { glanceListState.scrollToItem(0) }
        }
    }

    // Intercept back on non-first tab: scroll to tab 0 before allowing normal back navigation.
    // Compound condition ensures dialog/FAB BackHandlers (registered earlier = lower LIFO priority)
    // still fire first when those overlays are active.
    BackHandler(enabled = pagerState.currentPage > 0 && !showAddCardDialog && !isFabExpanded) {
        scope.launch { pagerState.animateScrollToPage(0) }
    }

    val isTopLevel = remember(initialViewContext, config.isTopLevel) {
        config.isTopLevel || initialViewContext == DashboardViewContext.None
    }

    val filterPillController = remember { FilterPillController() }

    val pillHeight = AeonSize.D64.value

    AeonHazeProvider { hazeLayers ->
        CompositionLocalProvider(LocalFilterPillController provides filterPillController) {
            Box(modifier = Modifier.fillMaxSize()) {
                // 1. Content Layer (Layer 1)
                Scaffold(
                    containerColor = Color.Transparent
                ) { innerPadding ->
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .statusBarsPadding()
                            .aeonHazeSource(hazeLayers.states[HazeLayer.Overlay])
                            .padding(bottom = innerPadding.calculateBottomPadding())
                    ) {
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
                                    onManageAccountsClick = { navigate(actions.onManageAccountsClick) },
                                    onAccountClick = { id -> navigate { actions.onAccountClick(id) } },
                                    onNavigateToPlannedPaymentList = { navigate(actions.onNavigateToPlannedPaymentList) },
                                    onNavigateToBudgetList = { navigate(actions.onNavigateToBudgetList) },
                                    onNavigateToLoanList = { navigate(actions.onNavigateToLoanList) },
                                    onNavigateToPlannedPaymentDetail = { id ->
                                        navigate {
                                            actions.onNavigateToPlannedPaymentDetail(
                                                id
                                            )
                                        }
                                    },
                                    onNavigateToBudgetDetail = { id -> navigate { actions.onNavigateToBudgetDetail(id) } },
                                    onNavigateToLoanDetail = { id -> navigate { actions.onNavigateToLoanDetail(id) } },
                                    onRecordsClick = { context -> navigate { actions.onRecordsClick(context) } },
                                    onSaveFilterClick = actions.onSaveFilterClick,
                                    onEditTransactionClick = { id -> navigate { actions.onEditTransactionClick(id) } },
                                    onCloneTransactionClick = { id -> navigate { actions.onCloneTransactionClick(id) } },
                                    onSplitTransactionClick = { id -> navigate { actions.onSplitTransactionClick(id) } },
                                    transactionViewModel = transactionViewModel,
                                    dashboardViewModel = dashboardViewModel,
                                    cardComputerRegistry = cardComputerRegistry,
                                    cardsConfig = cardsConfig,
                                    glanceListState = glanceListState,
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
                }

                // 2. Top Bar Layer (Layer 3 - Floating Pills)
                AeonTopAppBar(
                    left = AeonTopBarSection(
                        content = {
                            AeonTopBarNavTitlePill(
                                title = "", // Using titleContent instead
                                onMenuClick = if (isTopLevel) actions.onMenuClick else null,
                                onBackClick = if (!isTopLevel) actions.onBackClick else null,
                                isTopLevel = isTopLevel,
                                tabs = tabs.map { it.label },
                                selectedTabIndex = pagerState.currentPage,
                                onTabSelected = { index ->
                                    scope.launch { pagerState.animateScrollToPage(index) }
                                },
                                titleContent = {
                                    Column(
                                        verticalArrangement = Arrangement.Center
                                    ) {
                                        Text(
                                            currentProfile?.name ?: "Intelligence",
                                            style = AppTextStyles.screenTitle.copy(
                                                fontSize = 16.sp,
                                                lineHeight = 20.sp,
                                                fontWeight = FontWeight.ExtraBold,
                                                letterSpacing = (-0.5).sp
                                            )
                                        )
                                    }
                                }
                            )
                        }
                    ),
                    right = AeonTopBarSection(
                        content = {
                            AeonTopBarActionPill {
                                when (pagerState.currentPage) {
                                    0 -> AppBarIconButton(
                                        icon = Icons.Default.Add,
                                        onClick = { showAddCardDialog = true }
                                    )
                                    1 -> AppBarIconButton(
                                        icon = Icons.Rounded.History,
                                        onClick = { actions.onRecordsClick(TransactionFilter()) }
                                    )
                                }
                            }
                        }
                    )
                )

                // 3. Navigation Layer (Overlay)
                UnifiedPillNavigation(
                    selectedTab = pagerState.currentPage,
                    onTabSelected = { index ->
                        scope.launch { pagerState.animateScrollToPage(index) }
                    },
                    tabs = tabs,
                    fabPaddingBottom = AeonSize.D16.value + navigationBarHeight,
                    isFabExpanded = isFabExpanded,
                    onFabToggle = { if (expandableFabEnabled) isFabExpanded = it },
                    onFabClick = {
                        if (!expandableFabEnabled) {
                            navigate { actions.onAddClick(TransactionType.EXPENSE, null) }
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
                                    navigate { actions.onAddClick(type, templateId) }
                                }
                            )
                        }
                    } else null
                )

                // 4. Filter Pill Layer (Overlay)
                val pillRequest = filterPillController.activeRequest
                if (pillRequest != null) {
                    TransactionFilterPill(
                        filter = pillRequest.filter,
                        accounts = pillRequest.accounts,
                        categories = pillRequest.categories,
                        allTags = pillRequest.allTags,
                        savedFilters = pillRequest.savedFilters,
                        onSearchTransactions = pillRequest.onSearchTransactions,
                        onFilterEvent = pillRequest.onFilterEvent,
                        onSaveFilterClick = pillRequest.onSaveFilterClick,
                        onDismiss = {
                            pillRequest.onDismiss()
                            filterPillController.dismiss()
                        },
                        modifier = Modifier.fillMaxSize(),
                        filterRules = pillRequest.filterRules,
                        baseFilter = pillRequest.baseFilter,
                    )
                }
            } // outer Box
        } // AeonHazeProvider
    } // CompositionLocalProvider
}


@OptIn(ExperimentalFoundationApi::class)
@Composable
fun DashboardContent(
    innerPadding: PaddingValues,
    pagerState: PagerState,
    tabs: List<NavigationTab>,
    state: DashboardUiState.Success,
    onManageAccountsClick: () -> Unit,
    onAccountClick: (String) -> Unit,
    onNavigateToPlannedPaymentList: () -> Unit = {},
    onNavigateToBudgetList: () -> Unit = {},
    onNavigateToLoanList: () -> Unit = {},
    onNavigateToPlannedPaymentDetail: (Long) -> Unit = {},
    onNavigateToBudgetDetail: (Long) -> Unit = {},
    onNavigateToLoanDetail: (Long) -> Unit = {},
    onRecordsClick: (TransactionFilter) -> Unit,
    onSaveFilterClick: (TransactionFilter) -> Unit = {},
    onEditTransactionClick: (Long) -> Unit,
    onCloneTransactionClick: (Long) -> Unit = {},
    onSplitTransactionClick: (Long) -> Unit = {},
    transactionViewModel: TransactionViewModel,
    dashboardViewModel: DashboardViewModel,
    cardComputerRegistry: CardComputerRegistry,
    cardsConfig: DashboardCardsConfig,
    glanceListState: LazyListState,
    showAddCardDialog: Boolean,
    onShowAddCardDialogChange: (Boolean) -> Unit,
    cardAnimationEnabled: Boolean
) {
    val pillHeight = AeonSize.D64.value

    Box(
        modifier = Modifier.fillMaxSize()
    ) {
        HorizontalPager(
            state = pagerState,
            modifier = Modifier.fillMaxSize(),
            verticalAlignment = Alignment.Top
        ) { page ->
            when (page) {
                0 -> {
                    val savedFilters by transactionViewModel.savedFilters.collectAsStateWithLifecycle()
                    DashboardGlanceTab(
                        state = state,
                        cardsConfig = cardsConfig,
                        listState = glanceListState,
                        cardComputerRegistry = cardComputerRegistry,
                        cardUpdateHandler = dashboardViewModel,
                        savedFilters = savedFilters,
                        cardAnimationEnabled = cardAnimationEnabled,
                        onShowAddCardDialogChange = { onShowAddCardDialogChange(it) },
                        onRecordsClick = onRecordsClick,
                        onManageAccountsClick = onManageAccountsClick,
                        onAccountClick = onAccountClick,
                        onNavigateToPlannedPaymentList = onNavigateToPlannedPaymentList,
                        onNavigateToBudgetList = onNavigateToBudgetList,
                        onNavigateToLoanList = onNavigateToLoanList,
                        onNavigateToPlannedPaymentDetail = onNavigateToPlannedPaymentDetail,
                        onNavigateToBudgetDetail = onNavigateToBudgetDetail,
                        onNavigateToLoanDetail = onNavigateToLoanDetail,
                        onSaveFilterClick = onSaveFilterClick,
                        onEditTransactionClick = onEditTransactionClick,
                        onCloneTransactionClick = onCloneTransactionClick,
                        onSplitTransactionClick = onSplitTransactionClick,
                        onSearchTransactions = transactionViewModel::searchTransactions,
                        onItemExpanded = transactionViewModel::onItemExpanded,
                        onItemCollapsed = transactionViewModel::onItemCollapsed,
                        onStatusChange = transactionViewModel::quickUpdateStatus,
                        onDeleteTransaction = transactionViewModel::quickDeleteTransaction,
                        onFullSwipeRight = transactionViewModel::toggleStatusClearedUncleared,
                        onQuickConfirm = dashboardViewModel::quickConfirmById,
                        onQuickSkip = dashboardViewModel::quickSkipById,
                        onAddTag = transactionViewModel::addTag,
                        onReorderCards = dashboardViewModel::reorderCards,
                        onRemoveCard = dashboardViewModel::removeCard,
                        topPadding = pillHeight,
                        bottomPadding = innerPadding.calculateBottomPadding()
                    )
                }

                1 -> {
                    val revealedCardIds by transactionViewModel.revealedCardIdsList.collectAsStateWithLifecycle()
                    val recentActivitiesBreakdown by dashboardViewModel.recentActivitiesBreakdown.collectAsStateWithLifecycle()

                    DashboardRecentActivitiesTab(
                        recentActivitiesState = state.recentActivitiesState,
                        revealedCardIds = revealedCardIds,
                        recentActivitiesBreakdown = recentActivitiesBreakdown,
                        baseCurrency = state.baseCurrency,
                        onRecordsClick = onRecordsClick,
                        onTransactionEvent = { event ->
                            when (event) {
                                is TransactionItemEvent.Click -> {
                                    if (event.id.startsWith("tx_")) {
                                        onEditTransactionClick(event.id.removePrefix("tx_").toLong())
                                    }
                                }

                                is TransactionItemEvent.Expand -> transactionViewModel.onItemExpanded(
                                    event.id
                                )

                                is TransactionItemEvent.Collapse -> transactionViewModel.onItemCollapsed(
                                    event.id
                                )

                                is TransactionItemEvent.StatusChange -> transactionViewModel.quickUpdateStatus(
                                    event.id,
                                    event.status
                                )

                                is TransactionItemEvent.Clone -> onCloneTransactionClick(event.id)
                                is TransactionItemEvent.Delete -> transactionViewModel.quickDeleteTransaction(
                                    event.id
                                )

                                is TransactionItemEvent.Split -> onSplitTransactionClick(event.id)
                                is TransactionItemEvent.FullSwipeRight -> transactionViewModel.toggleStatusClearedUncleared(
                                    event.id
                                )

                                is TransactionItemEvent.ConfirmPlanned -> transactionViewModel.confirmPlannedPayment(
                                    event.paymentId,
                                    event.scheduledDate
                                )

                                is TransactionItemEvent.CancelPlanned -> transactionViewModel.cancelPlannedPayment(
                                    event.paymentId,
                                    event.scheduledDate
                                )
                            }
                        },
                        topPadding = pillHeight,
                        bottomPadding = innerPadding.calculateBottomPadding(),
                        hasStickyHeader = false
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
                }
            )
        }
    }
}

@Composable
fun DashboardGlanceTab(
    state: DashboardUiState.Success,
    cardsConfig: DashboardCardsConfig,
    listState: LazyListState = rememberLazyListState(),
    cardComputerRegistry: CardComputerRegistry,
    cardUpdateHandler: CardUpdateHandler,
    savedFilters: List<Filter>,
    cardAnimationEnabled: Boolean,
    onShowAddCardDialogChange: (Boolean) -> Unit,
    onRecordsClick: (TransactionFilter) -> Unit,
    onManageAccountsClick: () -> Unit,
    onAccountClick: (String) -> Unit,
    onNavigateToPlannedPaymentList: () -> Unit = {},
    onNavigateToBudgetList: () -> Unit = {},
    onNavigateToLoanList: () -> Unit = {},
    onNavigateToPlannedPaymentDetail: (Long) -> Unit = {},
    onNavigateToBudgetDetail: (Long) -> Unit = {},
    onNavigateToLoanDetail: (Long) -> Unit = {},
    onSaveFilterClick: (TransactionFilter) -> Unit,
    onEditTransactionClick: (Long) -> Unit,
    onCloneTransactionClick: (Long) -> Unit,
    onSplitTransactionClick: (Long) -> Unit,
    onSearchTransactions: (String) -> Flow<List<Transaction>>,
    onItemExpanded: (String) -> Unit,
    onItemCollapsed: (String) -> Unit,
    onStatusChange: (Long, com.osscabinet.ossWallet.domain.model.TransactionStatus) -> Unit,
    onDeleteTransaction: (Long) -> Unit,
    onFullSwipeRight: (Long) -> Unit,
    onQuickConfirm: (Long) -> Unit,
    onQuickSkip: (Long) -> Unit,
    onAddTag: (String, Int) -> Unit,
    onReorderCards: (List<CardConfig>) -> Unit,
    onRemoveCard: (String) -> Unit,
    topPadding: androidx.compose.ui.unit.Dp = 0.dp,
    bottomPadding: androidx.compose.ui.unit.Dp = 0.dp
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
    ) {
    val sharedCallbacks = remember(
        onManageAccountsClick,
        onAccountClick,
        onNavigateToBudgetDetail,
        onNavigateToBudgetList,
        onNavigateToLoanDetail,
        onNavigateToLoanList,
        onEditTransactionClick,
        onItemExpanded,
        onItemCollapsed,
        onStatusChange,
        onCloneTransactionClick,
        onDeleteTransaction,
        onSplitTransactionClick,
        onFullSwipeRight,
        onNavigateToPlannedPaymentDetail,
        onQuickConfirm,
        onQuickSkip,
        onNavigateToPlannedPaymentList,
        onSaveFilterClick,
        onAddTag
    ) {
        AeonInteractionCallbacks(
            accountActions = AccountActions(
                onManageClick = onManageAccountsClick,
                onClick = onAccountClick,
                onSeeAll = onManageAccountsClick
            ),
            budgetActions = BudgetActions(
                onClick = onNavigateToBudgetDetail,
                onSeeAll = onNavigateToBudgetList
            ),
            loanActions = LoanActions(
                onClick = onNavigateToLoanDetail,
                onSeeAll = onNavigateToLoanList
            ),
            transactionActions = TransactionActions(
                onClick = onEditTransactionClick,
                onItemExpanded = onItemExpanded,
                onItemCollapsed = onItemCollapsed,
                onStatusChange = onStatusChange,
                onClone = onCloneTransactionClick,
                onDelete = onDeleteTransaction,
                onSplit = onSplitTransactionClick,
                onFullSwipeRight = onFullSwipeRight
            ),
            paymentActions = PaymentActions(
                onClick = onNavigateToPlannedPaymentDetail,
                onConfirm = onQuickConfirm,
                onSkip = onQuickSkip,
                onSeeAll = onNavigateToPlannedPaymentList
            ),
            filterActions = FilterActions(
                onSaveClick = onSaveFilterClick
            ),
            tagActions = TagActions(
                onAdd = onAddTag
            )
        )
    }

    val sharedCardInput = remember(state) {
        CardInput(
            profileId = state.profileId,
            transactions = state.allTransactions,
            accounts = state.accounts,
            categories = state.allCategories,
            tags = state.allTags,
            exchangeRates = state.exchangeRates,
            baseCurrency = state.baseCurrency,
            plannedPayments = state.plannedPayments,
            budgets = state.budgets,
            loans = state.loans,
            loanPayments = state.loanPayments,
            filter = TransactionFilter(timelineRange = TimelineRange.AllTime(excludeFuture = true))
        )
    }

    AeonCardList(
        cards = cardsConfig.cards,
        state = listState,
        cardDataMap = state.cardDataMap,
        cardInput = sharedCardInput,
        cardComputerRegistry = cardComputerRegistry,
        cardUpdateHandler = cardUpdateHandler,
        onReorder = onReorderCards,
        onDelete = onRemoveCard,
        onRecordsClick = onRecordsClick,
        modifier = Modifier.weight(1f),
        contentPadding = PaddingValues(
            start = AeonTheme.dimensions.screenPadding,
            end = AeonTheme.dimensions.screenPadding,
            top = topPadding + AeonTheme.dimensions.spacingMd,
            bottom = scrollBottomPadding() + bottomPadding
        ),
        animateCards = cardAnimationEnabled,
        filterContext = FilterSelectionContext(
            accounts = state.accounts,
            allCategories = state.allCategories,
            allTags = state.allTags,
            savedFilters = savedFilters,
            onSearchTransactions = onSearchTransactions
        ),
        callbacks = sharedCallbacks
    )
    }
}

@Composable
fun DashboardRecentActivitiesTab(
    recentActivitiesState: TransactionListState,
    revealedCardIds: List<String>,
    recentActivitiesBreakdown: Boolean,
    baseCurrency: String,
    onRecordsClick: (TransactionFilter) -> Unit,
    onTransactionEvent: (TransactionItemEvent) -> Unit,
    topPadding: androidx.compose.ui.unit.Dp = 0.dp,
    bottomPadding: androidx.compose.ui.unit.Dp = 0.dp,
    hasStickyHeader: Boolean = true
) {
    RecentActivitiesScreen(
        listState = recentActivitiesState,
        onRecordsClick = onRecordsClick,
        baseCurrency = baseCurrency,
        contentPadding = PaddingValues(
            top = topPadding + AeonTheme.dimensions.spacingMd,
            bottom = scrollBottomPadding() + bottomPadding
        ),
        modifier = Modifier.fillMaxSize(),
        revealedCardIds = revealedCardIds,
        showCurrencyBreakdown = recentActivitiesBreakdown,
        onTransactionEvent = onTransactionEvent,
        hasStickyHeader = hasStickyHeader,
        showHeader = false
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
            .background(AppColors.accentContainer)
            .padding(
                horizontal = AeonTheme.dimensions.screenPadding,
                vertical = AeonTheme.dimensions.spacingMd
            ),
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
