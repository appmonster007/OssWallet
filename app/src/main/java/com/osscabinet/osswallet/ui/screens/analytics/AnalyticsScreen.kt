package com.osscabinet.ossWallet.ui.screens.analytics

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.osscabinet.ossWallet.domain.card.*
import com.osscabinet.ossWallet.domain.model.TransactionFilter
import com.osscabinet.ossWallet.domain.model.TransactionType
import com.osscabinet.ossWallet.domain.model.settings.*
import com.osscabinet.ossWallet.ui.components.navigation.UnifiedPillNavigation
import com.osscabinet.ossWallet.ui.components.navigation.TransactionFabMenu
import com.osscabinet.ossWallet.ui.components.*
import com.osscabinet.ossWallet.ui.components.cards.UnifiedCardRenderer
import com.osscabinet.ossWallet.ui.components.cards.AeonCardList
import com.osscabinet.ossWallet.ui.components.cards.CardListCallbacks
import com.osscabinet.ossWallet.ui.components.cards.FilterSelectionContext
import com.osscabinet.ossWallet.ui.components.cards.rememberCardWithCallbacks
import com.osscabinet.ossWallet.ui.screens.accounts.AccountViewModel
import com.osscabinet.ossWallet.ui.screens.transactions.TransactionViewModel
import com.osscabinet.ossWallet.ui.screens.templates.TemplateViewModel
import com.osscabinet.ossWallet.ui.screens.settings.SettingsViewModel
import com.osscabinet.ossWallet.ui.screens.analytics.AnalyticsViewModel
import com.osscabinet.ossWallet.ui.theme.AeonTheme
import kotlinx.coroutines.launch
import com.osscabinet.ossWallet.domain.model.timeline.TimelineRange
import com.osscabinet.ossWallet.domain.model.timeline.LastPeriodType
import com.osscabinet.ossWallet.ui.theme.AppColors
import com.osscabinet.ossWallet.ui.theme.AppTextStyles
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AnalyticsScreen(
    onMenuClick: () -> Unit,
    onBackClick: () -> Unit = {},
    isTopLevel: Boolean = false,
    viewModel: AnalyticsViewModel,
    accountViewModel: AccountViewModel,
    transactionViewModel: TransactionViewModel,
    templateViewModel: TemplateViewModel,
    settingsViewModel: SettingsViewModel,
    onRecordsClick: (TransactionFilter) -> Unit,
    onAccountClick: (String) -> Unit,
    onEditTransactionClick: (Long) -> Unit,
    onCloneTransactionClick: (Long) -> Unit = {},
    onSplitTransactionClick: (Long) -> Unit = {},
    onAddClick: (TransactionType?, Long?) -> Unit,
    cardComputerRegistry: CardComputerRegistry
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val cardsConfig by viewModel.cardsConfig.collectAsStateWithLifecycle()
    val cardDataMap by viewModel.cardDataMap.collectAsStateWithLifecycle()
    val accounts by accountViewModel.accounts.collectAsStateWithLifecycle()
    val savedFilters by transactionViewModel.savedFilters.collectAsStateWithLifecycle()
    val nonArchivedAccounts = remember(accounts) { accounts.filter { !it.isArchived } }
    val starredTemplates by templateViewModel.starredTemplates.collectAsStateWithLifecycle()
    val currentProfile by settingsViewModel.currentProfile.collectAsStateWithLifecycle()
    val allTemplates by templateViewModel.templates.collectAsStateWithLifecycle()
    val currentTheme by settingsViewModel.theme.collectAsStateWithLifecycle(initialValue = "System")
    val cardAnimationEnabled by settingsViewModel.cardAnimationEnabled.collectAsStateWithLifecycle(initialValue = true)
    val expandableFabEnabled by settingsViewModel.expandableFabEnabled.collectAsStateWithLifecycle(initialValue = true)

    val navigationBarHeight = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
    var showAddCardDialog by remember { mutableStateOf(false) }
    var isFabExpanded by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Intelligence", style = AppTextStyles.calculatorKey) },
                navigationIcon = {
                    if (isTopLevel) {
                        IconButton(onClick = onMenuClick) {
                            Icon(Icons.Default.Menu, contentDescription = "Menu")
                        }
                    } else {
                        IconButton(onClick = onBackClick) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                        }
                    }
                },
                actions = {
                    IconButton(onClick = { showAddCardDialog = true }) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = "Add card",
                            tint = AppColors.primary
                        )
                    }
                }
            )
        }
    ) { innerPadding ->
            Box(modifier = Modifier.fillMaxSize()) {
                when (val state = uiState) {
                    is AnalyticsUiState.Loading -> {
                        AeonShimmer(
                            innerPadding = innerPadding,
                            showCashflow = false,
                            showAccounts = false
                        )
                    }
                    is AnalyticsUiState.Error -> {
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = androidx.compose.ui.Alignment.Center) {
                            Text(state.message)
                        }
                    }
                    is AnalyticsUiState.Success -> {
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(innerPadding)
                                .background(AppColors.screenBackground)
                        ) {
                            AeonCardList(
                                cards = cardsConfig.cards,
                                cardDataMap = cardDataMap,
                                cardInput = CardInput(
                                    transactions = state.allTransactions,
                                    accounts = state.accounts,
                                    categories = state.allCategories,
                                    exchangeRates = state.exchangeRates,
                                    baseCurrency = state.selectedCurrency,
                                    plannedPayments = state.plannedPayments,
                                    budgets = state.budgets,
                                    loans = state.loans
                                ),
                                cardComputerRegistry = cardComputerRegistry,
                                cardUpdateHandler = viewModel,
                                onReorder = { viewModel.reorderCards(it) },
                                onDelete = { viewModel.removeCard(it) },
                                onRecordsClick = onRecordsClick,
                                modifier = Modifier.fillMaxSize(),
                                contentPadding = PaddingValues(
                                    start = AeonTheme.dimensions.screenPadding,
                                    end = AeonTheme.dimensions.screenPadding,
                                    top = AeonTheme.dimensions.spacingMd,
                                    bottom = scrollBottomPadding()
                                ),
                                animateCards = cardAnimationEnabled,
                                currentTheme = currentTheme,
                                filterContext = FilterSelectionContext(
                                    accounts = state.accounts,
                                    allCategories = state.allCategories,
                                    savedFilters = savedFilters
                                ),
                                callbacks = CardListCallbacks(
                                    onAccountClick = onAccountClick,
                                    onTransactionClick = onEditTransactionClick,
                                    onClone = { onCloneTransactionClick(it) },
                                    onDeleteTransaction = { transactionViewModel.quickDeleteTransaction(it) },
                                    onSplit = { onSplitTransactionClick(it) },
                                    onFullSwipeRight = { transactionViewModel.toggleStatusClearedUncleared(it) },
                                ),
                            )
                        }
                    }
                }

                UnifiedPillNavigation(
                    selectedTab = 0,
                    onTabSelected = {},
                    tabs = emptyList(),
                    fabPaddingBottom = 16.dp + navigationBarHeight,
                    isFabExpanded = isFabExpanded,
                    onFabToggle = { if (expandableFabEnabled) isFabExpanded = it },
                    onFabClick = {
                        if (!expandableFabEnabled) {
                            onAddClick(TransactionType.EXPENSE, null)
                        }
                    },
                    fabMenu = if (expandableFabEnabled) {
                        {
                            TransactionFabMenu(
                                isExpanded = isFabExpanded,
                                templates = allTemplates,
                                onAddClick = { type, templateId ->
                                    isFabExpanded = false
                                    onAddClick(type, templateId)
                                }
                            )
                        }
                    } else null
                )
            }
        }

    if (showAddCardDialog) {
        CardTypeSelectionDialog(
            screenContext = ScreenContext.ANALYTICS,
            onDismiss = { showAddCardDialog = false },
            onCardTypeSelected = { cardType ->
                val defaultFilter = TransactionFilter(
                    timelineRange = TimelineRange.LastPeriod(LastPeriodType.LAST_30_DAYS),
                    transactionTypes = setOf(TransactionType.EXPENSE)
                )
                when (cardType) {
                    CardType.CATEGORY_PIE_CHART -> viewModel.addCard(
                        CategoryPieChartCardConfig(
                            cardId = "analytics_category_breakdown_${System.currentTimeMillis()}",
                            order = cardsConfig.cards.size,
                            title = "CATEGORY PIE CHART",
                            filter = defaultFilter,
                            showLabels = true,
                            showLegend = true
                        )
                    )
                    CardType.CATEGORY_TREEMAP -> viewModel.addCard(
                        CategoryTreemapCardConfig(
                            cardId = "analytics_category_expense_treemap_${System.currentTimeMillis()}",
                            order = cardsConfig.cards.size,
                            title = "CATEGORY TREEMAP",
                            filter = defaultFilter,
                            showLabels = true
                        )
                    )
                    else -> viewModel.addCard(cardType)
                }
                showAddCardDialog = false
            }
        )
    }
}
