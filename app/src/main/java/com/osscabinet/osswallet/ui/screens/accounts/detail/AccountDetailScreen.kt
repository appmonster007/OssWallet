package com.osscabinet.ossWallet.ui.screens.accounts.detail

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Dashboard
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.History
import androidx.compose.material.icons.rounded.Receipt
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHostState
import com.osscabinet.ossWallet.ui.components.AeonSnackbarHost
import com.osscabinet.ossWallet.ui.components.AeonSnackbarType
import com.osscabinet.ossWallet.ui.components.showAeonSnackbar
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.osscabinet.ossWallet.R
import com.osscabinet.ossWallet.domain.card.CardFilterRules
import com.osscabinet.ossWallet.domain.card.CardInput
import com.osscabinet.ossWallet.domain.model.EntryMode
import com.osscabinet.ossWallet.domain.model.TransactionFilter
import com.osscabinet.ossWallet.domain.model.merge
import com.osscabinet.ossWallet.domain.model.settings.CardConfig
import com.osscabinet.ossWallet.domain.model.settings.DashboardCardsConfig
import com.osscabinet.ossWallet.domain.model.settings.ScreenContext
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
import com.osscabinet.ossWallet.ui.components.cards.BudgetActions
import com.osscabinet.ossWallet.ui.components.cards.FilterPillController
import com.osscabinet.ossWallet.ui.components.cards.FilterSelectionContext
import com.osscabinet.ossWallet.ui.components.cards.LoanActions
import com.osscabinet.ossWallet.ui.components.cards.LocalCardReorderState
import com.osscabinet.ossWallet.ui.components.cards.LocalEditingCardId
import com.osscabinet.ossWallet.ui.components.cards.LocalFilterPillController
import com.osscabinet.ossWallet.ui.components.cards.LocalOnEditCard
import com.osscabinet.ossWallet.ui.components.cards.rememberAeonCardReorderState
import com.osscabinet.ossWallet.ui.components.cards.PaymentActions
import com.osscabinet.ossWallet.ui.components.cards.TagActions
import com.osscabinet.ossWallet.ui.components.cards.TransactionActions
import com.osscabinet.ossWallet.ui.components.entry.EntrySwitchRow
import com.osscabinet.ossWallet.ui.components.navigation.NavigationTab
import com.osscabinet.ossWallet.ui.components.navigation.TransactionFabMenu
import com.osscabinet.ossWallet.ui.components.navigation.UnifiedPillNavigation
import com.osscabinet.ossWallet.ui.components.scrollBottomPadding
import com.osscabinet.ossWallet.ui.components.transactions.RecentActivitiesScreen
import com.osscabinet.ossWallet.ui.components.transactions.TransactionFilterPill
import com.osscabinet.ossWallet.ui.components.transactions.TransactionItemEvent
import com.osscabinet.ossWallet.ui.navigation.SharedNavViewModels
import com.osscabinet.ossWallet.ui.screens.transactions.TransactionViewModel
import com.osscabinet.ossWallet.ui.theme.AeonSize
import com.osscabinet.ossWallet.ui.theme.AeonTheme
import com.osscabinet.ossWallet.ui.theme.AppColors
import com.osscabinet.ossWallet.ui.theme.AppTextStyles
import com.osscabinet.ossWallet.ui.theme.HazeLayer
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import java.net.URLDecoder
import java.nio.charset.StandardCharsets

/**
 * A Value Object representing a valid configuration for the Account Detail screen.
 */
data class AccountDetailConfig(
    val accountId: String,
    val filterJson: String? = null
)

/**
 * Grouped actions for the Account Detail screen.
 */
data class AccountDetailActions(
    val onBackClick: () -> Unit,
    val onEditAccountClick: (String) -> Unit,
    val onAccountClick: (String) -> Unit,
    val onEditTransactionClick: (Long) -> Unit,
    val onNavigateToPlannedPaymentList: () -> Unit = {},
    val onNavigateToBudgetList: () -> Unit = {},
    val onNavigateToLoanList: () -> Unit = {},
    val onNavigateToPlannedPaymentDetail: (Long) -> Unit = {},
    val onNavigateToBudgetDetail: (Long) -> Unit = {},
    val onNavigateToLoanDetail: (Long) -> Unit = {},
    val onCloneTransactionClick: (Long) -> Unit = {},
    val onSplitTransactionClick: (Long) -> Unit = {},
    val onAddTransactionClick: (String, EntryMode?, Long?) -> Unit,
    val onRecordsClick: (TransactionFilter) -> Unit,
    val onSaveFilterClick: (TransactionFilter) -> Unit = {}
)

@OptIn(
    ExperimentalMaterial3Api::class,
    androidx.compose.foundation.ExperimentalFoundationApi::class
)
@Composable
fun AccountDetailScreen(
    config: AccountDetailConfig,
    actions: AccountDetailActions,
    vms: SharedNavViewModels,
    accountDetailViewModel: AccountDetailViewModel
) {
    val transactionViewModel = vms.transactionViewModel
    val settingsViewModel = vms.settingsViewModel
    val templateViewModel = vms.templateViewModel

    val snackbarHostState = remember { SnackbarHostState() }
    val loadFilterErrorMessage = stringResource(R.string.account_detail_load_filter_error)

    LaunchedEffect(config.filterJson) {
        if (config.filterJson != null) {
            try {
                val decoded =
                    URLDecoder.decode(config.filterJson, StandardCharsets.UTF_8.toString())
                val filter = Json.decodeFromString<TransactionFilter>(decoded)
                accountDetailViewModel.updateFilter(filter)
            } catch (e: Exception) {
                snackbarHostState.showAeonSnackbar(
                    message = loadFilterErrorMessage,
                    type = AeonSnackbarType.ERROR
                )
            }
        }
    }

    val cardAnimationEnabled by settingsViewModel.cardAnimationEnabled.collectAsStateWithLifecycle(
        initialValue = true
    )
    val expandableFabEnabled by settingsViewModel.expandableFabEnabled.collectAsStateWithLifecycle(
        initialValue = true
    )
    val uiState by accountDetailViewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(uiState) {
        if (uiState is AccountDetailUiState.Error) {
            actions.onBackClick()
        }
    }

    val scope = rememberCoroutineScope()
    val categories by transactionViewModel.categories.collectAsStateWithLifecycle()
    val savedFilters by transactionViewModel.savedFilters.collectAsStateWithLifecycle()
    val allTemplates by templateViewModel.templates.collectAsStateWithLifecycle()

    var navTriggered by remember { mutableStateOf(false) }
    androidx.lifecycle.compose.LifecycleEventEffect(androidx.lifecycle.Lifecycle.Event.ON_RESUME) {
        navTriggered = false
    }

    val navigate: (() -> Unit) -> Unit = { action ->
        if (!navTriggered) {
            navTriggered = true
            action()
        }
    }

    val navigationBarHeight = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
    val pagerState = rememberPagerState(initialPage = 0) { 2 }
    val tabs = listOf(
        NavigationTab(
            icon = Icons.Rounded.Dashboard,
            selectedIcon = Icons.Rounded.Dashboard,
            label = stringResource(R.string.nav_overview),
            contentDescription = stringResource(R.string.nav_overview_desc)
        ),
        NavigationTab(
            icon = Icons.Rounded.Receipt,
            selectedIcon = Icons.Rounded.Receipt,
            label = stringResource(R.string.nav_transactions),
            contentDescription = stringResource(R.string.nav_transactions_desc)
        )
    )

    var showSettingsDialog by rememberSaveable { mutableStateOf(false) }
    var showAddCardDialog by rememberSaveable { mutableStateOf(false) }
    var isFabExpanded by remember { mutableStateOf(false) }
    var editingCardId by remember { mutableStateOf<String?>(null) }
    val cardReorderState = rememberAeonCardReorderState()

    val filterPillController = remember { FilterPillController() }

    BackHandler(enabled = pagerState.currentPage > 0 && !showSettingsDialog && !showAddCardDialog && !isFabExpanded) {
        scope.launch { pagerState.animateScrollToPage(0) }
    }
    BackHandler(enabled = showSettingsDialog) { showSettingsDialog = false }
    BackHandler(enabled = showAddCardDialog) { showAddCardDialog = false }
    BackHandler(enabled = isFabExpanded) { isFabExpanded = false }
    BackHandler(enabled = editingCardId != null) { editingCardId = null }

    val accountDetailCardsConfig by accountDetailViewModel.cardsConfig.collectAsStateWithLifecycle()
    val accountDetailCardDataMap by accountDetailViewModel.cardDataMap.collectAsStateWithLifecycle()
    val pillHeight = AeonSize.D64.value

    if (showSettingsDialog && uiState is AccountDetailUiState.Success) {
        val state = uiState as AccountDetailUiState.Success
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
                        text = stringResource(R.string.transaction_settings_title),
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
                        EntrySwitchRow(
                            label = stringResource(R.string.setting_show_planned),
                            subtitle = stringResource(R.string.setting_show_planned_desc),
                            checked = state.showPlannedPayments,
                            onCheckedChange = { accountDetailViewModel.updateShowPlannedPayments(it) },
                            isFlush = true
                        )
                        EntrySwitchRow(
                            label = stringResource(R.string.setting_use_note_as_header),
                            subtitle = stringResource(R.string.setting_use_note_as_header_desc),
                            checked = state.useNoteAsHeader,
                            onCheckedChange = { accountDetailViewModel.updateUseNoteAsHeader(it) },
                            isFlush = true
                        )
                    }
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 16.dp),
                        contentAlignment = Alignment.CenterEnd
                    ) {
                        TextButton(onClick = { showSettingsDialog = false }) {
                            Text(
                                stringResource(R.string.action_close),
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

    AeonHazeProvider { hazeLayers ->
        CompositionLocalProvider(
            LocalFilterPillController provides filterPillController,
            LocalEditingCardId provides editingCardId,
            LocalOnEditCard provides { editingCardId = it },
            LocalCardReorderState provides cardReorderState
        ) {
            Box(modifier = Modifier.fillMaxSize()) {
                // 1. Content Layer (Layer 1)
                Scaffold(
                    containerColor = Color.Transparent
                ) { screenPadding ->
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .statusBarsPadding()
                            .aeonHazeSource(hazeLayers.states[HazeLayer.Overlay])
                            .padding(bottom = screenPadding.calculateBottomPadding())
                    ) {
                        when (val state = uiState) {
                            is AccountDetailUiState.Loading -> {
                                AeonShimmer(
                                    innerPadding = PaddingValues(
                                        top = AeonSize.D64.value + AeonTheme.dimensions.spacingMd,
                                        bottom = screenPadding.calculateBottomPadding()
                                    ),
                                    cards = accountDetailCardsConfig.cards.map { it.cardType }
                                )
                            }

                            is AccountDetailUiState.Error -> {
                                Box(
                                    modifier = Modifier.fillMaxSize(),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(state.message)
                                }
                            }

                            is AccountDetailUiState.Success -> {
                                Box(modifier = Modifier.fillMaxSize()) {
                                    HorizontalPager(
                                        state = pagerState,
                                        modifier = Modifier.fillMaxSize(),
                                        verticalAlignment = Alignment.Top
                                    ) { page ->
                                        when (page) {
                                            0 -> {
                                                DynamicAccountDetailCards(
                                                    state = state,
                                                    cardsConfig = accountDetailCardsConfig,
                                                    accountDetailViewModel = accountDetailViewModel,
                                                    cardDataMap = accountDetailCardDataMap,
                                                    onRecordsClick = actions.onRecordsClick,
                                                    savedFilters = savedFilters,
                                                    transactionViewModel = transactionViewModel,
                                                    onCloneTransactionClick = actions.onCloneTransactionClick,
                                                    onEditTransactionClick = actions.onEditTransactionClick,
                                                    onSplitTransactionClick = actions.onSplitTransactionClick,
                                                    onAccountClick = actions.onAccountClick,
                                                    onNavigateToPlannedPaymentList = actions.onNavigateToPlannedPaymentList,
                                                    onNavigateToBudgetList = actions.onNavigateToBudgetList,
                                                    onNavigateToLoanList = actions.onNavigateToLoanList,
                                                    onNavigateToPlannedPaymentDetail = actions.onNavigateToPlannedPaymentDetail,
                                                    onNavigateToBudgetDetail = actions.onNavigateToBudgetDetail,
                                                    onNavigateToLoanDetail = actions.onNavigateToLoanDetail,
                                                    navigate = navigate,
                                                    modifier = Modifier
                                                        .fillMaxSize()
                                                        .padding(horizontal = AeonTheme.dimensions.screenPadding),
                                                    showAddCardDialog = showAddCardDialog,
                                                    onShowAddCardDialogChange = {
                                                        showAddCardDialog = it
                                                    },
                                                    cardAnimationEnabled = cardAnimationEnabled,
                                                    topPadding = pillHeight
                                                )
                                            }

                                            1 -> {
                                                val revealedCardIds by transactionViewModel.revealedCardIdsList.collectAsStateWithLifecycle()
                                                val recentActivitiesBreakdown by accountDetailViewModel.recentActivitiesBreakdown.collectAsStateWithLifecycle()
                                                if (state.listState.isEmpty) {
                                                    Box(
                                                        modifier = Modifier.fillMaxSize(),
                                                        contentAlignment = Alignment.Center
                                                    ) {
                                                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                                            Icon(
                                                                Icons.Rounded.History,
                                                                contentDescription = null,
                                                                modifier = Modifier.size(AeonTheme.dimensions.iconSize3xl),
                                                                tint = AppColors.contentSecondary.copy(
                                                                    alpha = 0.2f
                                                                )
                                                            )
                                                            Spacer(
                                                                modifier = Modifier.height(
                                                                    AeonTheme.dimensions.spacingLg
                                                                )
                                                            )
                                                            Text(
                                                                stringResource(R.string.no_transactions_yet),
                                                                style = AppTextStyles.listItemTitle,
                                                                color = AppColors.border
                                                            )
                                                        }
                                                    }
                                                } else {
                                                    val uiListState =
                                                        com.osscabinet.ossWallet.ui.mapper.TransactionUiMapper.mapToUiState(
                                                            domainState = state.listState,
                                                            baseCurrency = state.baseCurrency
                                                        )
                                                    RecentActivitiesScreen(
                                                        listState = uiListState,
                                                        onRecordsClick = { drillDown ->
                                                            actions.onRecordsClick(
                                                                state.currentFilter.merge(drillDown)
                                                            )
                                                        },
                                                        baseCurrency = state.baseCurrency,
                                                        contentPadding = PaddingValues(
                                                            top = pillHeight + AeonTheme.dimensions.spacingMd,
                                                            bottom = scrollBottomPadding()
                                                        ),
                                                        modifier = Modifier.fillMaxSize(),
                                                        revealedCardIds = revealedCardIds,
                                                        showCurrencyBreakdown = recentActivitiesBreakdown,
                                                        onTransactionEvent = { event ->
                                                            when (event) {
                                                                is TransactionItemEvent.Click -> navigate {
                                                                    if (event.id.startsWith("tx_")) {
                                                                        actions.onEditTransactionClick(
                                                                            event.id.removePrefix("tx_")
                                                                                .toLong()
                                                                        )
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

                                                                is TransactionItemEvent.Clone -> actions.onCloneTransactionClick(
                                                                    event.id
                                                                )

                                                                is TransactionItemEvent.Delete -> transactionViewModel.quickDeleteTransaction(
                                                                    event.id
                                                                )

                                                                is TransactionItemEvent.Split -> actions.onSplitTransactionClick(
                                                                    event.id
                                                                )

                                                                is TransactionItemEvent.Jump -> transactionViewModel.handleJumpRequest(
                                                                    event.id,
                                                                    uiListState.getAllTransactionIds()
                                                                )

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
                                                        filter = state.currentFilter,
                                                        hasStickyHeader = false,
                                                        showHeader = false
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

                // 2. Pill Navigation Layer (sibling of Scaffold — must be outside hazeSource content)
                UnifiedPillNavigation(
                    selectedTab = pagerState.currentPage,
                    onTabSelected = { index -> scope.launch { pagerState.animateScrollToPage(index) } },
                    tabs = tabs,
                    fabPaddingBottom = 16.dp + navigationBarHeight,
                    isFabExpanded = isFabExpanded,
                    onFabToggle = { if (expandableFabEnabled) isFabExpanded = it },
                    onFabClick = {
                        if (!expandableFabEnabled) {
                            navigate {
                                actions.onAddTransactionClick(
                                    config.accountId,
                                    EntryMode.EXPENSE,
                                    null
                                )
                            }
                        }
                    },
                    fabMenu = if (expandableFabEnabled) {
                        {
                            TransactionFabMenu(
                                isExpanded = isFabExpanded,
                                templates = allTemplates,
                                categories = categories,
                                onAddClick = { type, templateId ->
                                    isFabExpanded = false
                                    navigate {
                                        actions.onAddTransactionClick(
                                            config.accountId,
                                            type,
                                            templateId
                                        )
                                    }
                                }
                            )
                        }
                    } else null
                )

                // 2. Snackbar Layer
                AeonSnackbarHost(hostState = snackbarHostState)

                // 3. Top Bar Layer (Layer 3 - Floating Pills)
                AeonTopAppBar(
                    left = AeonTopBarSection(
                        content = {
                            AeonTopBarNavTitlePill(
                                title = stringResource(R.string.account_detail_title),
                                onBackClick = { navigate { actions.onBackClick() } },
                                tabs = tabs.map { it.label },
                                selectedTabIndex = pagerState.currentPage,
                                onTabSelected = { index ->
                                    scope.launch { pagerState.animateScrollToPage(index) }
                                }
                            )
                        }
                    ),
                    right = AeonTopBarSection(
                        content = {
                            AeonTopBarActionPill {
                                when (pagerState.currentPage) {
                                    0 -> {
                                        AppBarIconButton(
                                            icon = Icons.Rounded.Edit,
                                            onClick = { navigate { actions.onEditAccountClick(config.accountId) } })
                                        AppBarIconButton(
                                            icon = Icons.Rounded.Settings,
                                            onClick = { showSettingsDialog = true })
                                        if (cardReorderState.isReordering) {
                                            AppBarIconButton(
                                                icon = Icons.Rounded.Check,
                                                onClick = { cardReorderState.confirm() })
                                        }
                                        AppBarIconButton(
                                            icon = Icons.Rounded.Add,
                                            onClick = { showAddCardDialog = true })
                                    }

                                    1 -> {
                                        AppBarIconButton(
                                            icon = Icons.Rounded.History,
                                            onClick = {
                                                actions.onRecordsClick(
                                                    TransactionFilter(
                                                        accountIds = setOf(config.accountId)
                                                    )
                                                )
                                            }
                                        )
                                    }
                                }
                            }
                        }
                    )
                )

                // 3. Filter Pill Layer (Layer 4)
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
            }
        }
    }
}

@Composable
fun DynamicAccountDetailCards(
    state: AccountDetailUiState.Success,
    cardsConfig: DashboardCardsConfig,
    accountDetailViewModel: AccountDetailViewModel,
    cardDataMap: kotlinx.collections.immutable.ImmutableMap<String, com.osscabinet.ossWallet.domain.card.CardComputeState> = kotlinx.collections.immutable.persistentMapOf(),
    onRecordsClick: (TransactionFilter) -> Unit,
    savedFilters: List<com.osscabinet.ossWallet.domain.model.Filter>,
    transactionViewModel: TransactionViewModel,
    onCloneTransactionClick: (Long) -> Unit,
    onEditTransactionClick: (Long) -> Unit,
    onNavigateToPlannedPaymentList: () -> Unit,
    onNavigateToBudgetList: () -> Unit,
    onNavigateToLoanList: () -> Unit,
    onNavigateToPlannedPaymentDetail: (Long) -> Unit,
    onNavigateToBudgetDetail: (Long) -> Unit,
    onNavigateToLoanDetail: (Long) -> Unit,
    onSplitTransactionClick: (Long) -> Unit = {},
    onAccountClick: (String) -> Unit,
    navigate: (() -> Unit) -> Unit,
    modifier: Modifier = Modifier,
    showAddCardDialog: Boolean,
    onShowAddCardDialogChange: (Boolean) -> Unit,
    cardAnimationEnabled: Boolean,
    topPadding: androidx.compose.ui.unit.Dp = 0.dp
) {
    var showDeleteCardsDialog by remember { mutableStateOf(false) }

    val sharedCallbacks = remember(
        onAccountClick,
        onNavigateToBudgetDetail,
        onNavigateToBudgetList,
        onNavigateToLoanDetail,
        onNavigateToLoanList,
        onEditTransactionClick,
        transactionViewModel,
        onCloneTransactionClick,
        onSplitTransactionClick,
        onNavigateToPlannedPaymentDetail,
        onNavigateToPlannedPaymentList,
        accountDetailViewModel,
        onRecordsClick,
        state.currentFilter
    ) {
        AeonInteractionCallbacks(
            accountActions = AccountActions(
                onClick = { id -> navigate { onAccountClick(id) } },
                onSeeAll = { navigate { onRecordsClick(state.currentFilter) } }
            ),
            budgetActions = BudgetActions(
                onClick = { id -> navigate { onNavigateToBudgetDetail(id) } },
                onSeeAll = { navigate { onNavigateToBudgetList() } }
            ),
            loanActions = LoanActions(
                onClick = { id -> navigate { onNavigateToLoanDetail(id) } },
                onSeeAll = { navigate { onNavigateToLoanList() } }
            ),
            transactionActions = TransactionActions(
                onClick = onEditTransactionClick,
                onItemExpanded = { transactionViewModel.onItemExpanded(it) },
                onItemCollapsed = { transactionViewModel.onItemCollapsed(it) },
                onStatusChange = { id, status ->
                    transactionViewModel.quickUpdateStatus(
                        id,
                        status
                    )
                },
                onClone = { onCloneTransactionClick(it) },
                onDelete = { transactionViewModel.quickDeleteTransaction(it) },
                onSplit = { onSplitTransactionClick(it) },
                onJump = { id, availableIds ->
                    transactionViewModel.handleJumpRequest(id, availableIds)
                },
                onFullSwipeRight = { transactionViewModel.toggleStatusClearedUncleared(it) }
            ),
            paymentActions = PaymentActions(
                onClick = { id -> navigate { onNavigateToPlannedPaymentDetail(id) } },
                onSeeAll = { navigate { onNavigateToPlannedPaymentList() } }
            ),
            tagActions = TagActions(
                onAdd = accountDetailViewModel::addTag
            )
        )
    }

    val accountLockFilter = remember(state.account.id) {
        TransactionFilter(accountIds = setOf(state.account.id))
    }
    val sharedCardInput = remember(state, state.currentFilter) {
        CardInput(
            profileId = state.profileId,
            transactionsVersion = state.version,
            accounts = listOf(state.account),
            categories = state.allCategories,
            tags = state.allTags,
            exchangeRates = state.exchangeRates,
            targetCurrency = state.account.currencyCode,
            filter = state.currentFilter,
            screenContext = ScreenContext.ACCOUNT_DETAIL
        )
    }

    Column(
        modifier = modifier
    ) {
        AeonCardList(
            cards = cardsConfig.cards,
            cardInput = sharedCardInput,
            cardDataMap = cardDataMap,
            cardUpdateHandler = accountDetailViewModel,
            onReorder = { accountDetailViewModel.reorderCards(it) },
            onDelete = { accountDetailViewModel.removeCard(it) },
            onRecordsClick = onRecordsClick,
            modifier = Modifier.weight(1f),
            contentPadding = PaddingValues(
                start = 0.dp,
                end = 0.dp,
                top = AeonTheme.dimensions.spacingMd + topPadding,
                bottom = scrollBottomPadding()
            ),
            animateCards = cardAnimationEnabled,
            filterContext = FilterSelectionContext(
                accounts = listOf(state.account),
                allCategories = state.allCategories,
                allTags = state.allTags,
                savedFilters = savedFilters,
                onSearchTransactions = transactionViewModel::searchTransactions
            ),
            callbacks = sharedCallbacks,
            baseFilter = accountLockFilter,
            screenRules = CardFilterRules.ACCOUNT_LOCKED
        )
    }

    if (showAddCardDialog) {
        AeonCardAtelier(
            screenContext = ScreenContext.ACCOUNT_DETAIL,
            onDismiss = { onShowAddCardDialogChange(false) },
            onCardTypeSelected = { cardType ->
                accountDetailViewModel.addCard(cardType)
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
        title = { Text(stringResource(R.string.delete_cards_title)) },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 300.dp),
                verticalArrangement = Arrangement.spacedBy(AeonTheme.dimensions.spacingSm)
            ) {
                Text(stringResource(R.string.delete_cards_prompt))

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
                Text(stringResource(R.string.action_delete))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.action_cancel))
            }
        }
    )
}
