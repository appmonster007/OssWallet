package com.osscabinet.ossWallet.ui.screens.dashboard


import androidx.activity.compose.BackHandler
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Restore
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.rounded.Dashboard
import androidx.compose.material.icons.rounded.History
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Surface
import com.osscabinet.ossWallet.ui.components.AeonConfirmDeleteDialog
import com.osscabinet.ossWallet.ui.components.AeonSnackbarHost
import com.osscabinet.ossWallet.ui.components.AeonSnackbarType
import com.osscabinet.ossWallet.ui.components.showAeonSnackbar
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.osscabinet.ossWallet.domain.card.CardComputeState
import com.osscabinet.ossWallet.domain.card.CardInput
import com.osscabinet.ossWallet.domain.card.CardRenderData
import com.osscabinet.ossWallet.domain.card.CardType
import com.osscabinet.ossWallet.domain.card.CardUpdateHandler
import com.osscabinet.ossWallet.domain.card.NetWorthMetrics
import com.osscabinet.ossWallet.domain.service.financial.FinancialSummary
import com.osscabinet.ossWallet.domain.model.Account
import com.osscabinet.ossWallet.domain.model.AccountStructureType
import com.osscabinet.ossWallet.domain.model.Category
import com.osscabinet.ossWallet.domain.model.EntryMode
import com.osscabinet.ossWallet.domain.model.Filter
import com.osscabinet.ossWallet.domain.model.Template
import com.osscabinet.ossWallet.domain.model.Transaction
import com.osscabinet.ossWallet.domain.model.TransactionFilter
import com.osscabinet.ossWallet.domain.model.TransactionStatus
import com.osscabinet.ossWallet.domain.model.analytics.AccountSummaryItem
import com.osscabinet.ossWallet.domain.model.analytics.CashFlowResult
import com.osscabinet.ossWallet.domain.model.settings.AccountsCardConfig
import com.osscabinet.ossWallet.domain.model.settings.CardConfig
import com.osscabinet.ossWallet.domain.model.settings.DashboardCardsConfig
import com.osscabinet.ossWallet.domain.model.settings.FastScrollSide
import com.osscabinet.ossWallet.domain.model.settings.NetWorthCardConfig
import com.osscabinet.ossWallet.domain.model.settings.ScreenContext
import com.osscabinet.ossWallet.domain.model.timeline.TimelineRange
import com.osscabinet.ossWallet.domain.repository.CardRepository
import com.osscabinet.ossWallet.ui.components.AeonHazeProvider
import com.osscabinet.ossWallet.ui.components.AeonShimmer
import com.osscabinet.ossWallet.ui.components.AeonTopAppBar
import com.osscabinet.ossWallet.ui.components.AeonTopBarActionPill
import com.osscabinet.ossWallet.ui.components.AeonTopBarNavTitlePill
import com.osscabinet.ossWallet.ui.components.AeonTopBarSection
import com.osscabinet.ossWallet.ui.components.AppBarIconButton
import com.osscabinet.ossWallet.ui.components.aeonHazeSource
import com.osscabinet.ossWallet.ui.components.cards.AccountActions
import com.osscabinet.ossWallet.ui.components.cards.AeonCardAtelier
import com.osscabinet.ossWallet.ui.components.cards.AeonCardList
import com.osscabinet.ossWallet.ui.components.cards.AeonInteractionCallbacks
import com.osscabinet.ossWallet.ui.components.cards.LocalCardReorderState
import com.osscabinet.ossWallet.ui.components.cards.rememberAeonCardReorderState
import com.osscabinet.ossWallet.ui.components.cards.BudgetActions
import com.osscabinet.ossWallet.ui.components.cards.FilterActions
import com.osscabinet.ossWallet.ui.components.cards.FilterPillController
import com.osscabinet.ossWallet.ui.components.cards.FilterSelectionContext
import com.osscabinet.ossWallet.ui.components.cards.LoanActions
import com.osscabinet.ossWallet.ui.components.cards.LocalEditingCardId
import com.osscabinet.ossWallet.ui.components.cards.LocalFilterPillController
import com.osscabinet.ossWallet.ui.components.cards.LocalOnEditCard
import com.osscabinet.ossWallet.ui.components.cards.PaymentActions
import com.osscabinet.ossWallet.ui.components.cards.TagActions
import com.osscabinet.ossWallet.ui.components.cards.TransactionActions
import com.osscabinet.ossWallet.ui.components.navigation.NavigationTab
import com.osscabinet.ossWallet.ui.components.navigation.TransactionFabMenu
import com.osscabinet.ossWallet.ui.components.navigation.UnifiedPillNavigation
import com.osscabinet.ossWallet.ui.components.scrollBottomPadding
import com.osscabinet.ossWallet.ui.components.transactions.LinkedTransactionJumpSheet
import com.osscabinet.ossWallet.ui.components.transactions.RecentActivitiesScreen
import com.osscabinet.ossWallet.ui.components.transactions.TransactionFilterPill
import com.osscabinet.ossWallet.ui.components.transactions.TransactionItemEvent
import com.osscabinet.ossWallet.ui.navigation.DashboardViewContext
import com.osscabinet.ossWallet.ui.navigation.SharedNavViewModels
import com.osscabinet.ossWallet.ui.screens.dashboard.quickadd.QuickAddSheet
import com.osscabinet.ossWallet.ui.screens.plannedpayments.UndoAction
import com.osscabinet.ossWallet.ui.screens.transactions.model.TransactionListItem
import com.osscabinet.ossWallet.ui.screens.transactions.model.TransactionListUiState
import com.osscabinet.ossWallet.ui.theme.AeonSize
import com.osscabinet.ossWallet.ui.theme.AeonTheme
import com.osscabinet.ossWallet.ui.theme.AppColors
import com.osscabinet.ossWallet.ui.theme.AppTextStyles
import com.osscabinet.ossWallet.ui.theme.HazeLayer
import com.osscabinet.ossWallet.ui.theme.OssWalletTheme
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.persistentMapOf
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import java.math.BigDecimal
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
    val onAddClick: (EntryMode?, Long?) -> Unit,
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
    val onSplitTransactionClick: (Long) -> Unit = {}
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

    // Apply initial view focus when screen opens with context
    LaunchedEffect(config.viewContextJson) {
        config.viewContextJson?.let { contextString ->
            try {
                val decodedContext =
                    URLDecoder.decode(contextString, StandardCharsets.UTF_8.toString())
                val context = Json.decodeFromString<DashboardViewContext>(decodedContext)
                dashboardViewModel.onIntent(DashboardIntent.InitializeView(context))
            } catch (_: Exception) {
                dashboardViewModel.onIntent(DashboardIntent.InitializeView(DashboardViewContext.None))
            }
        } ?: run {
            dashboardViewModel.onIntent(DashboardIntent.ClearViewFocus)
        }
    }

    // Clear view focus when leaving the screen
    DisposableEffect(Unit) {
        onDispose {
            dashboardViewModel.onIntent(DashboardIntent.ClearViewFocus)
        }
    }

    val dashboardUiState by dashboardViewModel.uiState.collectAsStateWithLifecycle()
    val cardsConfig by dashboardViewModel.cardsConfig.collectAsStateWithLifecycle()

    val successState = dashboardUiState as? DashboardUiState.Success

    val isTopLevel = remember(successState?.initialViewContext, config.isTopLevel) {
        val initialViewContext = successState?.initialViewContext ?: DashboardViewContext.None
        config.isTopLevel || (initialViewContext == DashboardViewContext.None)
    }

    DashboardScreenLayout(
        isTopLevel = isTopLevel,
        initialViewContext = successState?.initialViewContext ?: DashboardViewContext.None,
        uiState = dashboardUiState,
        cardsConfig = cardsConfig,
        currentProfileName = successState?.displayPrefs?.currentProfileName,
        cardAnimationEnabled = successState?.displayPrefs?.cardAnimationEnabled ?: true,
        expandableFabEnabled = successState?.displayPrefs?.expandableFabEnabled ?: true,
        allTemplates = successState?.allTemplates ?: emptyList(),
        allCategories = successState?.allCategories ?: emptyList(),
        lastUndoAction = successState?.lastUndoAction,
        savedFilters = successState?.savedFilters ?: emptyList(),
        revealedCardIds = successState?.revealedCardIds?.toImmutableList() ?: persistentListOf(),
        transactionSnackbarMessage = successState?.transactionSnackbarMessage,
        scrollToId = successState?.scrollToId,
        highlightedId = successState?.highlightedId,
        recentActivitiesBreakdown = successState?.displayPrefs?.recentActivitiesBreakdown ?: true,
        actions = actions,
        onResetView = { dashboardViewModel.onIntent(DashboardIntent.ResetToInitialView) },
        onUndoAction = { dashboardViewModel.onIntent(DashboardIntent.UndoLastPlannedPaymentAction) },
        onClearUndo = { dashboardViewModel.onIntent(DashboardIntent.ClearUndoAction) },
        onClearTransactionSnackbar = { dashboardViewModel.onIntent(DashboardIntent.ClearSnackbar) },
        onClearScrollTo = { dashboardViewModel.onIntent(DashboardIntent.ClearScrollTo) },
        onAddCard = dashboardViewModel::addCard,
        onReorderCards = dashboardViewModel::reorderCards,
        onRemoveCard = dashboardViewModel::removeCard,
        onQuickConfirm = { id, date ->
            dashboardViewModel.onIntent(
                DashboardIntent.QuickConfirmPlannedPayment(
                    id,
                    date
                )
            )
        },
        onQuickSkip = { id, date ->
            dashboardViewModel.onIntent(
                DashboardIntent.QuickSkipPlannedPayment(
                    id,
                    date
                )
            )
        },
        onSearchTransactions = { query -> dashboardViewModel.searchTransactions(query) },
        onItemExpanded = { dashboardViewModel.onIntent(DashboardIntent.OnCardExpanded(it)) },
        onItemCollapsed = { dashboardViewModel.onIntent(DashboardIntent.OnCardCollapsed(it)) },
        onStatusChange = { id, status ->
            dashboardViewModel.onIntent(
                DashboardIntent.ChangeTransactionStatus(
                    id,
                    status
                )
            )
        },
        onDeleteTransaction = { id ->
            dashboardViewModel.onIntent(
                DashboardIntent.QuickDeleteTransaction(
                    id
                )
            )
        },
        onCloneTransactionClick = { id -> actions.onCloneTransactionClick(id) },
        onFullSwipeRight = { id ->
            dashboardViewModel.onIntent(
                DashboardIntent.ToggleStatusClearedUncleared(
                    id
                )
            )
        },
        onJumpTransaction = { id, availableIds ->
            dashboardViewModel.onIntent(
                DashboardIntent.HandleJumpRequest(
                    id,
                    availableIds
                )
            )
        },
        onPerformJump = { id, _ -> dashboardViewModel.onIntent(DashboardIntent.PerformJump(id)) },
        onCloseLinkedPopup = { dashboardViewModel.onIntent(DashboardIntent.CloseLinkedPopup) },
        linkedPopupTxs = successState?.linkedPopupTxs,
        onAddTag = { name, color ->
            dashboardViewModel.onIntent(
                DashboardIntent.AddTag(
                    name,
                    color
                )
            )
        },
        onConfirmPlanned = { id, date ->
            dashboardViewModel.onIntent(
                DashboardIntent.QuickConfirmPlannedPayment(
                    id,
                    date
                )
            )
        },
        onCancelPlanned = { id, date ->
            dashboardViewModel.onIntent(
                DashboardIntent.QuickSkipPlannedPayment(
                    id,
                    date
                )
            )
        },
        cardUpdateHandler = dashboardViewModel
    )
}

@OptIn(
    ExperimentalMaterial3Api::class,
    ExperimentalLayoutApi::class,
    ExperimentalFoundationApi::class
)
@Composable
fun DashboardScreenLayout(
    isTopLevel: Boolean,
    initialViewContext: DashboardViewContext,
    uiState: DashboardUiState,
    cardsConfig: DashboardCardsConfig,
    currentProfileName: String?,
    cardAnimationEnabled: Boolean,
    expandableFabEnabled: Boolean,
    allTemplates: List<Template>,
    allCategories: List<Category>,
    lastUndoAction: UndoAction?,
    savedFilters: List<Filter>,
    revealedCardIds: ImmutableList<String>,
    transactionSnackbarMessage: String?,
    scrollToId: String?,
    highlightedId: String?,
    recentActivitiesBreakdown: Boolean,
    actions: DashboardActions,
    onResetView: () -> Unit,
    onUndoAction: () -> Unit,
    onClearUndo: () -> Unit,
    onClearTransactionSnackbar: () -> Unit,
    onClearScrollTo: () -> Unit,
    onAddCard: (CardType) -> Unit,
    onReorderCards: (List<CardConfig>) -> Unit,
    onRemoveCard: (String) -> Unit,
    onQuickConfirm: (Long, Long) -> Unit,
    onQuickSkip: (Long, Long) -> Unit,
    onSearchTransactions: (String) -> Flow<List<Transaction>>,
    onItemExpanded: (String) -> Unit,
    onItemCollapsed: (String) -> Unit,
    onStatusChange: (Long, TransactionStatus) -> Unit,
    onDeleteTransaction: (Long) -> Unit,
    onCloneTransactionClick: (Long) -> Unit,
    onFullSwipeRight: (Long) -> Unit,
    onJumpTransaction: (Long, Set<Long>?) -> Unit,
    onPerformJump: (Long, Set<Long>?) -> Unit,
    onCloseLinkedPopup: () -> Unit,
    linkedPopupTxs: List<TransactionListItem>?,
    onAddTag: (String, Int) -> Unit,
    onConfirmPlanned: (Long, Long) -> Unit,
    onCancelPlanned: (Long, Long) -> Unit,
    cardUpdateHandler: CardUpdateHandler,
) {
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

    var showAddCardDialog by remember { mutableStateOf(false) }
    var showSettingsDialog by remember { mutableStateOf(false) }
    var showResetConfirmDialog by remember { mutableStateOf(false) }
    var isFabExpanded by remember { mutableStateOf(false) }
    var showQuickAddSheet by remember { mutableStateOf(false) }
    var editingCardId by remember { mutableStateOf<String?>(null) }
    val cardReorderState = rememberAeonCardReorderState()

    BackHandler(enabled = showAddCardDialog) { showAddCardDialog = false }
    BackHandler(enabled = showSettingsDialog) { showSettingsDialog = false }
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

    val navigationBarHeight = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
    val scope = rememberCoroutineScope()
    val glanceListState = rememberLazyListState()
    val recentActivitiesListState = rememberLazyListState()

    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(transactionSnackbarMessage) {
        transactionSnackbarMessage?.let {
            snackbarHostState.showAeonSnackbar(it)
            onClearTransactionSnackbar()
        }
    }

    LaunchedEffect(lastUndoAction) {
        val action = lastUndoAction ?: return@LaunchedEffect
        val message =
            if (action is UndoAction.ConfirmUndo) "Payment confirmed" else "Occurrence skipped"
        val result = snackbarHostState.showAeonSnackbar(
            message = message,
            actionLabel = "Undo",
            duration = SnackbarDuration.Short,
            type = AeonSnackbarType.SUCCESS
        )
        if (result == SnackbarResult.ActionPerformed) {
            onUndoAction()
        } else {
            onClearUndo()
        }
    }

    // Scroll reset: only when the screen/app is resumed while the Glance tab is active.
    androidx.lifecycle.compose.LifecycleEventEffect(androidx.lifecycle.Lifecycle.Event.ON_RESUME) {
        if (pagerState.currentPage == 0) {
            scope.launch { glanceListState.scrollToItem(0) }
        }
    }

    // Intercept back on non-first tab
    BackHandler(enabled = pagerState.currentPage > 0 && !showAddCardDialog && !isFabExpanded) {
        scope.launch { pagerState.animateScrollToPage(0) }
    }
    BackHandler(enabled = editingCardId != null) { editingCardId = null }

    val filterPillController = remember { FilterPillController() }

    if (showSettingsDialog) {
        Dialog(
            onDismissRequest = { showSettingsDialog = false },
            properties = DialogProperties(usePlatformDefaultWidth = false)
        ) {
            val configuration = LocalConfiguration.current
            val screenHeight = configuration.screenHeightDp.dp
            Surface(
                modifier = Modifier
                    .fillMaxWidth(0.92f)
                    .heightIn(max = screenHeight * 0.85f),
                shape = RoundedCornerShape(AeonSize.D32.value),
                color = AppColors.sheetSurface,
                tonalElevation = AeonSize.D6.value
            ) {
                Column(modifier = Modifier.padding(24.dp)) {
                    Text(
                        text = "Dashboard Settings",
                        style = AppTextStyles.heroAmount.copy(
                            fontWeight = FontWeight.Light,
                            fontSize = 28.sp,
                            color = AppColors.contentPrimary.copy(alpha = 0.8f)
                        ),
                        modifier = Modifier.padding(bottom = 24.dp)
                    )
                    Column(
                        modifier = Modifier
                            .weight(1f, fill = false)
                            .verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(AeonTheme.dimensions.spacingLg)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    showSettingsDialog = false
                                    showResetConfirmDialog = true
                                }
                                .padding(vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Restore,
                                contentDescription = null,
                                tint = AppColors.error
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(
                                text = "Reset to Default",
                                style = AppTextStyles.transactionTitle,
                                color = AppColors.error
                            )
                        }
                    }
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 16.dp),
                        contentAlignment = Alignment.CenterEnd
                    ) {
                        TextButton(onClick = { showSettingsDialog = false }) {
                            Text(
                                "Close",
                                style = AppTextStyles.listItemTitle.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = AppColors.primary
                                )
                            )
                        }
                    }
                }
            }
        }
    }

    AeonConfirmDeleteDialog(
        show = showResetConfirmDialog,
        title = "Reset to Default?",
        message = "This restores the default card layout for this screen. Your customizations here will be lost.",
        onConfirm = {
            showResetConfirmDialog = false
            cardUpdateHandler.resetCardsToDefault()
        },
        onDismiss = { showResetConfirmDialog = false },
        confirmButtonText = "Reset"
    )

    AeonHazeProvider { hazeLayers ->
        CompositionLocalProvider(
            LocalFilterPillController provides filterPillController,
            LocalEditingCardId provides editingCardId,
            LocalOnEditCard provides { editingCardId = it },
            LocalCardReorderState provides cardReorderState
        ) {
            Box(modifier = Modifier.fillMaxSize()) {
                // 1. Content Layer (Layer 1)
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .statusBarsPadding()
                        .aeonHazeSource(hazeLayers.states[HazeLayer.Overlay])
                        .padding(bottom = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding())
                ) {
                    // Context indicator banner (if view focus is applied)
                    if (initialViewContext != DashboardViewContext.None) {
                        DashboardViewIndicator(
                            context = initialViewContext,
                            onReset = onResetView
                        )
                    }

                    // Crossfade keyed on the STATE'S TYPE only (not the state object itself) —
                    // DashboardUiState.Success is a large data class that gets a new instance on
                    // every minor update (scroll, snackbar, filter, highlight), so keying on the
                    // object directly made Crossfade re-trigger its fade transition on nearly
                    // every recomposition, briefly running two DashboardContent subtrees (two
                    // HorizontalPagers/LazyListStates/nav-triggering LaunchedEffects) at once —
                    // that's what broke navigation/scroll. Keying on just the phase means the
                    // fade only plays on an actual Loading→Success→Error transition; the content
                    // inside each branch still reads the live `uiState` so in-place updates are
                    // ordinary recomposition, not a new crossfade.
                    val uiPhase = when (uiState) {
                        is DashboardUiState.Loading -> 0
                        is DashboardUiState.Success -> 1
                        is DashboardUiState.Error -> 2
                    }
                    Crossfade(
                        targetState = uiPhase,
                        modifier = Modifier.weight(1f),
                        animationSpec = tween(durationMillis = 220),
                        label = "dashboard_ui_state"
                    ) { phase ->
                    when (phase) {
                        0 -> {
                            AeonShimmer(
                                innerPadding = PaddingValues(
                                    top = AeonSize.D64.value + AeonTheme.dimensions.spacingMd,
                                    bottom = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
                                ),
                                cards = cardsConfig.cards.map { it.cardType }
                            )
                        }

                        1 -> {
                            val state = uiState as DashboardUiState.Success
                            DashboardContent(
                                modifier = Modifier,
                                innerPadding = PaddingValues(bottom = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()),
                                pagerState = pagerState,
                                state = state,
                                onMenuClick = actions.onMenuClick,
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
                                onNavigateToBudgetDetail = { id ->
                                    navigate {
                                        actions.onNavigateToBudgetDetail(
                                            id
                                        )
                                    }
                                },
                                onNavigateToLoanDetail = { id ->
                                    navigate {
                                        actions.onNavigateToLoanDetail(
                                            id
                                        )
                                    }
                                },
                                onRecordsClick = { context ->
                                    navigate {
                                        actions.onRecordsClick(
                                            context
                                        )
                                    }
                                },
                                onSaveFilterClick = actions.onSaveFilterClick,
                                onEditTransactionClick = { id ->
                                    navigate {
                                        actions.onEditTransactionClick(
                                            id
                                        )
                                    }
                                },
                                onCloneTransactionClick = onCloneTransactionClick,
                                onSplitTransactionClick = { id ->
                                    navigate {
                                        actions.onSplitTransactionClick(
                                            id
                                        )
                                    }
                                },
                                onJumpTransaction = onJumpTransaction,
                                onPerformJump = onPerformJump,
                                scrollToId = scrollToId,
                                highlightedId = highlightedId,
                                onClearScrollTo = onClearScrollTo,
                                savedFilters = savedFilters,
                                revealedCardIds = revealedCardIds,
                                recentActivitiesBreakdown = recentActivitiesBreakdown,
                                onSearchTransactions = onSearchTransactions,
                                onItemExpanded = onItemExpanded,
                                onItemCollapsed = onItemCollapsed,
                                onStatusChange = onStatusChange,
                                onDeleteTransaction = onDeleteTransaction,
                                onFullSwipeRight = onFullSwipeRight,
                                onQuickConfirm = onQuickConfirm,
                                onQuickSkip = onQuickSkip,
                                onAddTag = onAddTag,
                                onReorderCards = onReorderCards,
                                onRemoveCard = onRemoveCard,
                                onConfirmPlanned = onConfirmPlanned,
                                onCancelPlanned = onCancelPlanned,
                                onAddCard = onAddCard,
                                cardUpdateHandler = cardUpdateHandler,
                                cardsConfig = cardsConfig,
                                glanceListState = glanceListState,
                                recentActivitiesListState = recentActivitiesListState,
                                showAddCardDialog = showAddCardDialog,
                                onShowAddCardDialogChange = { showAddCardDialog = it },
                                cardAnimationEnabled = cardAnimationEnabled
                            )
                        }

                        else -> {
                            val state = uiState as DashboardUiState.Error
                            Box(
                                modifier = Modifier.fillMaxSize(),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(state.message)
                            }
                        }
                    }
                    } // Crossfade
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
                                            currentProfileName ?: "Intelligence",
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
                                    0 -> {
                                        if (cardReorderState.isReordering) {
                                            AppBarIconButton(
                                                icon = Icons.Default.Check,
                                                onClick = { cardReorderState.confirm() }
                                            )
                                        }
                                        AppBarIconButton(
                                            icon = Icons.Default.Settings,
                                            onClick = { showSettingsDialog = true }
                                        )
                                        AppBarIconButton(
                                            icon = Icons.Default.Add,
                                            onClick = { showAddCardDialog = true }
                                        )
                                    }

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
                            showQuickAddSheet = true
                        }
                    },
                    onFabLongPress = if (!expandableFabEnabled) ({
                        navigate { actions.onAddClick(EntryMode.EXPENSE, null) }
                    }) else null,
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

                // 4. Snackbar Layer (above FAB)
                AeonSnackbarHost(hostState = snackbarHostState)

                // 5. Quick Add Sheet (when FAB menu is disabled)
                if (showQuickAddSheet) {
                    QuickAddSheet(onDismiss = { showQuickAddSheet = false })
                }

                // 6. Filter Pill Layer (Overlay)
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

                // 7. Linked Transactions Jump Sheet
                linkedPopupTxs?.let { txs ->
                    val availableIds = (uiState as? DashboardUiState.Success)
                        ?.recentActivitiesState?.getAllTransactionIds()
                    LinkedTransactionJumpSheet(
                        transactions = txs,
                        onJump = { id -> onPerformJump(id, availableIds) },
                        onDismiss = onCloseLinkedPopup
                    )
                }
            } // outer Box
        } // AeonHazeProvider
    } // CompositionLocalProvider
}


@OptIn(ExperimentalFoundationApi::class)
@Composable
fun DashboardContent(
    modifier: Modifier = Modifier,
    innerPadding: PaddingValues,
    pagerState: PagerState,
    state: DashboardUiState.Success,
    onMenuClick: () -> Unit,
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
    onJumpTransaction: (Long, Set<Long>?) -> Unit,
    onPerformJump: (Long, Set<Long>?) -> Unit,
    scrollToId: String? = null,
    highlightedId: String? = null,
    onClearScrollTo: () -> Unit = {},
    savedFilters: List<Filter>,
    revealedCardIds: ImmutableList<String>,
    recentActivitiesBreakdown: Boolean,
    onSearchTransactions: (String) -> Flow<List<Transaction>>,
    onItemExpanded: (String) -> Unit,
    onItemCollapsed: (String) -> Unit,
    onStatusChange: (Long, TransactionStatus) -> Unit,
    onDeleteTransaction: (Long) -> Unit,
    onFullSwipeRight: (Long) -> Unit,
    onQuickConfirm: (Long, Long) -> Unit,
    onQuickSkip: (Long, Long) -> Unit,
    onAddTag: (String, Int) -> Unit,
    onReorderCards: (List<CardConfig>) -> Unit,
    onRemoveCard: (String) -> Unit,
    onConfirmPlanned: (Long, Long) -> Unit,
    onCancelPlanned: (Long, Long) -> Unit,
    onAddCard: (CardType) -> Unit,
    cardUpdateHandler: CardUpdateHandler,
    cardsConfig: DashboardCardsConfig,
    glanceListState: LazyListState,
    recentActivitiesListState: LazyListState,
    showAddCardDialog: Boolean,
    onShowAddCardDialogChange: (Boolean) -> Unit,
    cardAnimationEnabled: Boolean
) {
    val pillHeight = AeonSize.D64.value

    HorizontalPager(
        state = pagerState,
        modifier = modifier.fillMaxSize(),
        beyondViewportPageCount = 1,
        verticalAlignment = Alignment.Top
    ) { page ->
            when (page) {
                0 -> {
                    DashboardGlanceTab(
                        state = state,
                        cardsConfig = cardsConfig,
                        listState = glanceListState,
                        cardUpdateHandler = cardUpdateHandler,
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
                        onJumpTransaction = onJumpTransaction,
                        onPerformJump = onPerformJump,
                        scrollToId = scrollToId,
                        highlightedId = highlightedId,
                        onClearScrollTo = onClearScrollTo,
                        onSearchTransactions = onSearchTransactions,
                        onItemExpanded = onItemExpanded,
                        onItemCollapsed = onItemCollapsed,
                        onStatusChange = onStatusChange,
                        onDeleteTransaction = onDeleteTransaction,
                        onFullSwipeRight = onFullSwipeRight,
                        onQuickConfirm = onQuickConfirm,
                        onQuickSkip = onQuickSkip,
                        onAddTag = onAddTag,
                        onReorderCards = onReorderCards,
                        onRemoveCard = onRemoveCard,
                        topPadding = pillHeight,
                        bottomPadding = innerPadding.calculateBottomPadding()
                    )
                }

                1 -> {
                    DashboardRecentActivitiesTab(
                        recentActivitiesState = state.recentActivitiesState,
                        revealedCardIds = revealedCardIds,
                        recentActivitiesBreakdown = recentActivitiesBreakdown,
                        baseCurrency = state.baseCurrency,
                        scrollToId = scrollToId,
                        highlightedId = highlightedId,
                        onScrollComplete = onClearScrollTo,
                        onRecordsClick = onRecordsClick,
                        onTransactionEvent = { event ->
                            when (event) {
                                is TransactionItemEvent.Click -> {
                                    if (event.id.startsWith("tx_")) {
                                        onEditTransactionClick(
                                            event.id.removePrefix("tx_").toLong()
                                        )
                                    }
                                }

                                is TransactionItemEvent.Expand -> onItemExpanded(event.id)
                                is TransactionItemEvent.Collapse -> onItemCollapsed(event.id)
                                is TransactionItemEvent.StatusChange -> onStatusChange(
                                    event.id,
                                    event.status
                                )

                                is TransactionItemEvent.Clone -> onCloneTransactionClick(event.id)
                                is TransactionItemEvent.Delete -> onDeleteTransaction(event.id)
                                is TransactionItemEvent.Split -> onSplitTransactionClick(event.id)
                                is TransactionItemEvent.Jump -> onJumpTransaction(
                                    event.id,
                                    state.recentActivitiesState.getAllTransactionIds()
                                )

                                is TransactionItemEvent.FullSwipeRight -> onFullSwipeRight(event.id)
                                is TransactionItemEvent.ConfirmPlanned -> onConfirmPlanned(
                                    event.paymentId,
                                    event.scheduledDate
                                )

                                is TransactionItemEvent.CancelPlanned -> onCancelPlanned(
                                    event.paymentId,
                                    event.scheduledDate
                                )
                            }
                        },
                        topPadding = pillHeight,
                        bottomPadding = innerPadding.calculateBottomPadding(),
                        hasStickyHeader = false,
                        lazyListState = recentActivitiesListState,
                        fastScrollSide = state.displayPrefs?.fastScrollSide ?: FastScrollSide.RIGHT
                    )
                }
            }
        }

        // Add Card Studio (Atelier)
        if (showAddCardDialog) {
            AeonCardAtelier(
                screenContext = ScreenContext.DASHBOARD,
                onDismiss = { onShowAddCardDialogChange(false) },
                onCardTypeSelected = { cardType ->
                    onAddCard(cardType)
                }
            )
        }
}

@Composable
fun DashboardGlanceTab(
    state: DashboardUiState.Success,
    cardsConfig: DashboardCardsConfig,
    listState: LazyListState = rememberLazyListState(),
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
    onJumpTransaction: (Long, Set<Long>?) -> Unit,
    onPerformJump: (Long, Set<Long>?) -> Unit,
    scrollToId: String? = null,
    highlightedId: String? = null,
    onClearScrollTo: () -> Unit = {},
    onSearchTransactions: (String) -> Flow<List<Transaction>>,
    onItemExpanded: (String) -> Unit,
    onItemCollapsed: (String) -> Unit,
    onStatusChange: (Long, TransactionStatus) -> Unit,
    onDeleteTransaction: (Long) -> Unit,
    onFullSwipeRight: (Long) -> Unit,
    onQuickConfirm: (Long, Long) -> Unit,
    onQuickSkip: (Long, Long) -> Unit,
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
        // Capture each callback in rememberUpdatedState so the AeonInteractionCallbacks object is
        // created once. Each lambda dereferences the State at invocation time, so callers always
        // get the latest value without forcing a rebuild of the entire callbacks tree.
        val onManageAccountsRef by rememberUpdatedState(onManageAccountsClick)
        val onAccountClickRef by rememberUpdatedState(onAccountClick)
        val onBudgetDetailRef by rememberUpdatedState(onNavigateToBudgetDetail)
        val onBudgetListRef by rememberUpdatedState(onNavigateToBudgetList)
        val onLoanDetailRef by rememberUpdatedState(onNavigateToLoanDetail)
        val onLoanListRef by rememberUpdatedState(onNavigateToLoanList)
        val onEditTxRef by rememberUpdatedState(onEditTransactionClick)
        val onJumpTxRef by rememberUpdatedState(onJumpTransaction)
        val onPerformJumpRef by rememberUpdatedState(onPerformJump)
        val onItemExpandedRef by rememberUpdatedState(onItemExpanded)
        val onItemCollapsedRef by rememberUpdatedState(onItemCollapsed)
        val onStatusChangeRef by rememberUpdatedState(onStatusChange)
        val onCloneTxRef by rememberUpdatedState(onCloneTransactionClick)
        val onDeleteTxRef by rememberUpdatedState(onDeleteTransaction)
        val onSplitTxRef by rememberUpdatedState(onSplitTransactionClick)
        val onFullSwipeRightRef by rememberUpdatedState(onFullSwipeRight)
        val onPaymentDetailRef by rememberUpdatedState(onNavigateToPlannedPaymentDetail)
        val onQuickConfirmRef by rememberUpdatedState(onQuickConfirm)
        val onQuickSkipRef by rememberUpdatedState(onQuickSkip)
        val onPaymentListRef by rememberUpdatedState(onNavigateToPlannedPaymentList)
        val onSaveFilterRef by rememberUpdatedState(onSaveFilterClick)
        val onAddTagRef by rememberUpdatedState(onAddTag)

        val sharedCallbacks = remember {
            AeonInteractionCallbacks(
                accountActions = AccountActions(
                    onManageClick = { onManageAccountsRef() },
                    onClick = { id -> onAccountClickRef(id) },
                    onSeeAll = { onManageAccountsRef() }
                ),
                budgetActions = BudgetActions(
                    onClick = { id -> onBudgetDetailRef(id) },
                    onSeeAll = { onBudgetListRef() }
                ),
                loanActions = LoanActions(
                    onClick = { id -> onLoanDetailRef(id) },
                    onSeeAll = { onLoanListRef() }
                ),
                transactionActions = TransactionActions(
                    onClick = { id -> onEditTxRef(id) },
                    onItemExpanded = { id -> onItemExpandedRef(id) },
                    onItemCollapsed = { id -> onItemCollapsedRef(id) },
                    onStatusChange = { id, status -> onStatusChangeRef(id, status) },
                    onClone = { id -> onCloneTxRef(id) },
                    onDelete = { id -> onDeleteTxRef(id) },
                    onSplit = { id -> onSplitTxRef(id) },
                    onJump = { id, availableIds -> onJumpTxRef(id, availableIds) },
                    onPerformJump = { id, availableIds -> onPerformJumpRef(id, availableIds) },
                    onFullSwipeRight = { id -> onFullSwipeRightRef(id) }
                ),
                paymentActions = PaymentActions(
                    onClick = { id -> onPaymentDetailRef(id) },
                    onConfirm = { id, date -> onQuickConfirmRef(id, date) },
                    onSkip = { id, date -> onQuickSkipRef(id, date) },
                    onSeeAll = { onPaymentListRef() }
                ),
                filterActions = FilterActions(
                    onSaveClick = { filter -> onSaveFilterRef(filter) }
                ),
                tagActions = TagActions(
                    onAdd = { name, profile -> onAddTagRef(name, profile) }
                )
            )
        }

        val sharedCardInput = remember(
            state.profileId,
            state.allAccounts,
            state.allCategories,
            state.allTags,
            state.exchangeRates,
            state.baseCurrency,
            state.plannedPayments,
            state.budgets,
            state.loans,
            state.loanPayments
        ) {
            CardInput(
                profileId = state.profileId,
                transactionsVersion = state.cardInput?.transactionsVersion ?: 0L,
                accounts = state.allAccounts.ifEmpty { state.accounts },
                categories = state.allCategories,
                tags = state.allTags,
                exchangeRates = state.exchangeRates,
                targetCurrency = state.baseCurrency,
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
            cardUpdateHandler = cardUpdateHandler,
            onReorder = onReorderCards,
            onDelete = onRemoveCard,
            onRecordsClick = onRecordsClick,
            modifier = Modifier.weight(1f),
            scrollToId = scrollToId,
            highlightedId = highlightedId,
            onScrollComplete = onClearScrollTo,
            contentPadding = PaddingValues(
                start = AeonTheme.dimensions.screenPadding,
                end = AeonTheme.dimensions.screenPadding,
                top = topPadding + AeonTheme.dimensions.spacingMd,
                bottom = scrollBottomPadding() + bottomPadding
            ),
            animateCards = cardAnimationEnabled,
            filterContext = FilterSelectionContext(
                accounts = state.allAccounts.ifEmpty { state.accounts },
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
    recentActivitiesState: TransactionListUiState,
    revealedCardIds: ImmutableList<String>,
    recentActivitiesBreakdown: Boolean,
    baseCurrency: String,
    scrollToId: String? = null,
    highlightedId: String? = null,
    onScrollComplete: () -> Unit = {},
    onRecordsClick: (TransactionFilter) -> Unit,
    onTransactionEvent: (TransactionItemEvent) -> Unit,
    topPadding: androidx.compose.ui.unit.Dp = 0.dp,
    bottomPadding: androidx.compose.ui.unit.Dp = 0.dp,
    hasStickyHeader: Boolean = true,
    lazyListState: LazyListState = rememberLazyListState(),
    fastScrollSide: FastScrollSide = FastScrollSide.RIGHT
) {
    RecentActivitiesScreen(
        listState = recentActivitiesState,
        lazyListState = lazyListState,
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
        showHeader = false,
        scrollToId = scrollToId,
        highlightedId = highlightedId,
        onScrollComplete = onScrollComplete,
        fastScrollSide = fastScrollSide
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

// ------------------------------------------------------------------------------------------------
// PREVIEWS
// ------------------------------------------------------------------------------------------------

@Preview(showBackground = true)
@Composable
fun DashboardScreenPreview() {
    val sampleAccount = Account(
        id = "1",
        name = "Main Savings",
        currencyCode = "USD",
        structureType = AccountStructureType.SAVINGS,
        colorHex = AccountStructureType.SAVINGS.colorHex,
        icon = AccountStructureType.SAVINGS.iconKey,
    )

    val netWorthConfig = NetWorthCardConfig(cardId = "net_worth_1", order = 0)
    val accountsConfig = AccountsCardConfig(cardId = "accounts_1", order = 1)

    val mockState = DashboardUiState.Success(
        profileId = 1,
        accounts = persistentListOf(sampleAccount),
        allAccounts = persistentListOf(sampleAccount),
        cardDataMap = persistentMapOf(
            "net_worth_1" to CardComputeState.Success(
                CardRenderData.NetWorth(
                    config = netWorthConfig,
                    currentPeriodMetrics = NetWorthMetrics(
                        netWorth = BigDecimal("12500.00"),
                        cashFlow = CashFlowResult.empty().copy(
                            metricsInBase = FinancialSummary(
                                income = BigDecimal("5000.00"),
                                expense = BigDecimal("-3000.00"), // Expenses should be negative
                                transactionCount = 10
                            )
                        )
                    ),
                    currency = "USD"
                )
            ),
            "accounts_1" to CardComputeState.Success(
                CardRenderData.Accounts(
                    config = accountsConfig,
                    updatedAccounts = listOf(sampleAccount),
                    accountSummaries = mapOf(
                        "1" to AccountSummaryItem(
                            account = sampleAccount,
                            currencyBalances = mapOf("USD" to BigDecimal("12500.00")),
                            balance = BigDecimal("12500.00"),
                            balanceInBase = BigDecimal("12500.00"),
                            currentPeriodMetrics = FinancialSummary(
                                income = BigDecimal("5000.00"),
                                expense = BigDecimal("-3000.00"),
                                transactionCount = 10
                            )
                        )
                    ),
                    currency = "USD",
                    currentTheme = "Glass"
                )
            )
        ),
        allTransactions = persistentListOf(),
        recentActivitiesState = TransactionListUiState(groups = emptyList(), isEmpty = true),
        allCategories = persistentListOf(),
        allTags = persistentListOf(),
        baseCurrency = "USD",
        exchangeRates = com.osscabinet.ossWallet.domain.service.currency.ExchangeRateSnapshot.EMPTY,
        plannedPayments = persistentListOf(),
        budgets = persistentListOf(),
        loans = persistentListOf(),
        loanPayments = persistentMapOf()
    )

    val sampleCards = listOf(netWorthConfig, accountsConfig)

    OssWalletTheme {
        DashboardScreenLayout(
            isTopLevel = true,
            initialViewContext = DashboardViewContext.None,
            uiState = mockState,
            cardsConfig = DashboardCardsConfig(cards = sampleCards),
            currentProfileName = "Intelligence",
            cardAnimationEnabled = true,
            expandableFabEnabled = true,
            allTemplates = emptyList(),
            allCategories = emptyList(),
            lastUndoAction = null,
            savedFilters = emptyList(),
            revealedCardIds = persistentListOf(),
            transactionSnackbarMessage = null,
            scrollToId = null,
            highlightedId = null,
            recentActivitiesBreakdown = true,
            actions = DashboardActions(
                onMenuClick = {},
                onRecordsClick = {},
                onAddClick = { _, _ -> },
                onManageAccountsClick = {},
                onAccountClick = {},
                onEditTransactionClick = {}
            ),
            onResetView = {},
            onUndoAction = {},
            onClearUndo = {},
            onClearTransactionSnackbar = {},
            onClearScrollTo = {},
            onAddCard = {},
            onReorderCards = {},
            onRemoveCard = {},
            onQuickConfirm = { _, _ -> },
            onQuickSkip = { _, _ -> },
            onSearchTransactions = { flowOf(emptyList()) },
            onItemExpanded = {},
            onItemCollapsed = {},
            onStatusChange = { _, _ -> },
            onDeleteTransaction = {},
            onCloneTransactionClick = {},
            onFullSwipeRight = {},
            onJumpTransaction = { _, _ -> },
            onPerformJump = { _, _ -> },
            onCloseLinkedPopup = {},
            linkedPopupTxs = null,
            onAddTag = { _, _ -> },
            onConfirmPlanned = { _, _ -> },
            onCancelPlanned = { _, _ -> },
            cardUpdateHandler = object : CardUpdateHandler {
                override val cardUpdateScope: CoroutineScope get() = MainScope()
                override val cardRepository: CardRepository get() = error("Mock")
                override val cardScreenContext: ScreenContext get() = ScreenContext.DASHBOARD
            }
        )
    }
}

@Preview(showBackground = true)
@Composable
fun DashboardGlanceTabPreview() {
    val mockState = DashboardUiState.Success(
        profileId = 1,
        accounts = persistentListOf(
            Account(
                id = "1",
                name = "Main Account",
                currencyCode = "USD",
                structureType = AccountStructureType.SAVINGS,
                colorHex = AccountStructureType.SAVINGS.colorHex,
                icon = AccountStructureType.SAVINGS.iconKey,
            )
        ),
        allAccounts = persistentListOf(
            Account(
                id = "1",
                name = "Main Account",
                currencyCode = "USD",
                structureType = AccountStructureType.SAVINGS,
                colorHex = AccountStructureType.SAVINGS.colorHex,
                icon = AccountStructureType.SAVINGS.iconKey,
            )
        ),
        cardDataMap = persistentMapOf(),
        allTransactions = persistentListOf(),
        recentActivitiesState = TransactionListUiState(groups = emptyList(), isEmpty = true),
        allCategories = persistentListOf(),
        allTags = persistentListOf(),
        baseCurrency = "USD",
        exchangeRates = com.osscabinet.ossWallet.domain.service.currency.ExchangeRateSnapshot.EMPTY,
        plannedPayments = persistentListOf(),
        budgets = persistentListOf(),
        loans = persistentListOf(),
        loanPayments = persistentMapOf()
    )

    val sampleCards = listOf(
        NetWorthCardConfig(cardId = "net_worth_1", order = 0)
    )

    OssWalletTheme {
        DashboardGlanceTab(
            state = mockState,
            cardsConfig = DashboardCardsConfig(cards = sampleCards),
            cardUpdateHandler = object : CardUpdateHandler {
                override val cardUpdateScope: CoroutineScope get() = MainScope()
                override val cardRepository: CardRepository get() = error("Mock")
                override val cardScreenContext: ScreenContext get() = ScreenContext.DASHBOARD
            },
            savedFilters = emptyList(),
            cardAnimationEnabled = true,
            onShowAddCardDialogChange = {},
            onRecordsClick = {},
            onManageAccountsClick = {},
            onAccountClick = {},
            onSaveFilterClick = {},
            onEditTransactionClick = {},
            onCloneTransactionClick = {},
            onSplitTransactionClick = {},
            onJumpTransaction = { _, _ -> },
            scrollToId = null,
            highlightedId = null,
            onClearScrollTo = {},
            onSearchTransactions = { flowOf(emptyList()) },
            onItemExpanded = {},
            onItemCollapsed = {},
            onStatusChange = { _, _ -> },
            onDeleteTransaction = {},
            onFullSwipeRight = {},
            onQuickConfirm = { _, _ -> },
            onQuickSkip = { _, _ -> },
            onAddTag = { _, _ -> },
            onReorderCards = {},
            onRemoveCard = {},
            onPerformJump = { _, _ -> },
        )
    }
}

@Preview(showBackground = true)
@Composable
fun DashboardRecentActivitiesTabPreview() {
    OssWalletTheme {
        DashboardRecentActivitiesTab(
            recentActivitiesState = TransactionListUiState(groups = emptyList(), isEmpty = true),
            revealedCardIds = persistentListOf(),
            recentActivitiesBreakdown = true,
            baseCurrency = "USD",
            onRecordsClick = {},
            onTransactionEvent = {}
        )
    }
}
