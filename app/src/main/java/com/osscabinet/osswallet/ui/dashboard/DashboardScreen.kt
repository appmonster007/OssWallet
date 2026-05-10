package com.osscabinet.ossWallet.ui.dashboard

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
import com.osscabinet.ossWallet.domain.model.TransactionListState
import com.osscabinet.ossWallet.domain.model.settings.DashboardCardsConfig
import com.osscabinet.ossWallet.domain.model.settings.ScreenContext
import com.osscabinet.ossWallet.ui.components.AeonShimmer
import com.osscabinet.ossWallet.ui.components.CardTypeSelectionDialog
import com.osscabinet.ossWallet.ui.components.cards.AeonCardList
import com.osscabinet.ossWallet.ui.components.navigation.NavigationTab
import com.osscabinet.ossWallet.ui.components.navigation.TransactionFabMenu
import com.osscabinet.ossWallet.ui.components.navigation.UnifiedPillNavigation
import com.osscabinet.ossWallet.ui.components.transactions.RecentActivitiesScreen
import com.osscabinet.ossWallet.ui.navigation.DashboardViewContext
import com.osscabinet.ossWallet.ui.viewmodel.AccountViewModel
import com.osscabinet.ossWallet.ui.viewmodel.AnalyticsViewModel
import com.osscabinet.ossWallet.ui.viewmodel.SettingsViewModel
import com.osscabinet.ossWallet.ui.viewmodel.TemplateViewModel
import com.osscabinet.ossWallet.ui.viewmodel.TransactionViewModel
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import java.net.URLDecoder
import java.nio.charset.StandardCharsets

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
    onAddClick: (Int?, Long?) -> Unit,
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
                            verticalArrangement = Arrangement.spacedBy(2.dp)
                        ) {
                            Text(
                                currentProfile?.name ?: "Intelligence",
                                style = MaterialTheme.typography.titleLarge
                            )
                            Text(
                                "Personal Account",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.outline
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
                        .padding(horizontal = 12.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Glance",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                    IconButton(
                        onClick = { onShowAddCardDialogChange(true) },
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = "Add card",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp)
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
                            start = 12.dp,
                            end = 12.dp,
                            top = 6.dp,
                            bottom = calculateScrollBottomPadding()
                        ),
                        animateCards = cardAnimationEnabled,
                        currentTheme = currentTheme,
                        accounts = state.accounts,
                        allCategories = state.allCategories,
                        savedFilters = savedFilters,
                        onManageAccountsClick = onManageAccountsClick,
                        onAccountClick = onAccountClick,
                        onTransactionClick = onEditTransactionClick,
                        onPlannedPaymentConfirm = { id -> dashboardViewModel.quickConfirmById(id) },
                        onPlannedPaymentSkip = { id -> dashboardViewModel.quickSkipById(id) },
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
                        onFullSwipeRight = { transactionViewModel.toggleStatusClearedUncleared(it) }
                    )
                }
            }

            1 -> {
                val revealedCardIds by transactionViewModel.revealedCardIdsList.collectAsStateWithLifecycle()

                RecentActivitiesScreen(
                    listState = state.recentActivitiesState,
                    onRecordsClick = onRecordsClick,
                    onTransactionClick = onEditTransactionClick,
                    baseCurrency = state.baseCurrency,
                    contentPadding = PaddingValues(bottom = calculateScrollBottomPadding()),
                    modifier = Modifier.fillMaxSize(),
                    revealedCardIds = revealedCardIds,
                    onItemExpanded = { transactionViewModel.onItemExpanded(it) },
                    onItemCollapsed = { transactionViewModel.onItemCollapsed(it) },
                    onStatusChange = { transactionId, status ->
                        transactionViewModel.quickUpdateStatus(
                            transactionId,
                            status
                        )
                    },
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
            .background(MaterialTheme.colorScheme.primaryContainer)
            .padding(horizontal = 12.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.Visibility,
                contentDescription = "View focus",
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(14.dp)
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