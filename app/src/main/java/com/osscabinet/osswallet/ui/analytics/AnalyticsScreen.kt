package com.osscabinet.ossWallet.ui.analytics

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.osscabinet.ossWallet.domain.card.*
import com.osscabinet.ossWallet.domain.model.TransactionFilter
import com.osscabinet.ossWallet.domain.model.settings.*
import com.osscabinet.ossWallet.ui.components.navigation.UnifiedPillNavigation
import com.osscabinet.ossWallet.ui.components.navigation.TransactionFabMenu
import com.osscabinet.ossWallet.ui.components.*
import com.osscabinet.ossWallet.ui.components.cards.UnifiedCardRenderer
import com.osscabinet.ossWallet.ui.components.cards.AeonCardList
import com.osscabinet.ossWallet.ui.components.cards.rememberCardWithCallbacks
import com.osscabinet.ossWallet.ui.viewmodel.*
import kotlinx.coroutines.launch
import com.osscabinet.ossWallet.domain.model.timeline.TimelineRange
import com.osscabinet.ossWallet.domain.model.timeline.LastPeriodType
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AnalyticsScreen(
    viewModel: AnalyticsViewModel,
    accountViewModel: AccountViewModel,
    transactionViewModel: TransactionViewModel,
    templateViewModel: TemplateViewModel,
    settingsViewModel: SettingsViewModel,
    onBackClick: () -> Unit,
    onAccountsViewClick: () -> Unit,
    onRecordsClick: (com.osscabinet.ossWallet.ui.navigation.RecordsFilterContext) -> Unit,
    onExchangeRatesClick: () -> Unit,
    onTemplatesClick: () -> Unit,
    onManageAccountsClick: () -> Unit,
    onSettingsClick: () -> Unit,
    onAccountClick: (String) -> Unit,
    onAddClick: (Int?, String?) -> Unit,
    cardComputerRegistry: CardComputerRegistry,
    onPlannedPaymentsClick: () -> Unit = {},
    onBudgetsClick: () -> Unit = {},
    onLoansClick: () -> Unit = {}
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val cardsConfig by viewModel.cardsConfig.collectAsStateWithLifecycle()
    val accounts by accountViewModel.accounts.collectAsStateWithLifecycle()
    val savedFilters by transactionViewModel.savedFilters.collectAsStateWithLifecycle()
    val nonArchivedAccounts = remember(accounts) { accounts.filter { !it.isArchived } }
    val starredTemplates by templateViewModel.starredTemplates.collectAsStateWithLifecycle()
    val currentProfile by settingsViewModel.currentProfile.collectAsStateWithLifecycle()
    val allTemplates by templateViewModel.templates.collectAsStateWithLifecycle()
    val currentTheme by settingsViewModel.theme.collectAsStateWithLifecycle(initialValue = "System")
    val cardAnimationEnabled by settingsViewModel.cardAnimationEnabled.collectAsStateWithLifecycle(initialValue = true)
    val expandableFabEnabled by settingsViewModel.expandableFabEnabled.collectAsStateWithLifecycle(initialValue = true)

    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val coroutineScope = rememberCoroutineScope()
    var showAddCardDialog by remember { mutableStateOf(false) }
    var isFabExpanded by remember { mutableStateOf(false) }

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            AppDrawerContent(
                currentProfile = currentProfile,
                nonArchivedAccounts = nonArchivedAccounts,
                starredTemplates = starredTemplates,
                selectedDestination = "analytics",
                onDashboardClick = { onBackClick() },
                onAccountsViewClick = { onAccountsViewClick() },
                onRecordsClick = { onRecordsClick(com.osscabinet.ossWallet.ui.navigation.RecordsFilterContext.None) },
                onAnalyticsClick = { coroutineScope.launch { drawerState.close() } },
                onExchangeRatesClick = { onExchangeRatesClick() },
                onTemplatesClick = { onTemplatesClick() },
                onManageAccountsClick = { onManageAccountsClick() },
                onSettingsClick = { onSettingsClick() },
                onAccountClick = { id -> onAccountClick(id) },
                onTemplateClick = { template -> onAddClick(template.transactionType, template.name) },
                onCloseDrawer = { coroutineScope.launch { drawerState.close() } },
                onPlannedPaymentsClick = onPlannedPaymentsClick,
                onBudgetsClick = onBudgetsClick,
                onLoansClick = onLoansClick
            )
        }
    ) {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text("Intelligence", style = MaterialTheme.typography.headlineSmall) },
                    navigationIcon = {
                        IconButton(onClick = { coroutineScope.launch { drawerState.open() } }) {
                            Icon(Icons.Default.Menu, contentDescription = "Menu")
                        }
                    },
                    actions = {
                        IconButton(onClick = { showAddCardDialog = true }) {
                            Icon(
                                imageVector = Icons.Default.Add,
                                contentDescription = "Add card",
                                tint = MaterialTheme.colorScheme.primary
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
                                .background(MaterialTheme.colorScheme.background)
                        ) {
                            AeonCardList(
                                cards = cardsConfig.cards,
                                cardInput = CardInput(
                                    transactions = state.allTransactions,
                                    accounts = state.accounts,
                                    categories = state.allCategories,
                                    exchangeRates = state.exchangeRates,
                                    baseCurrency = state.selectedCurrency,
                                    precision = state.decimalPrecision
                                ),
                                cardComputerRegistry = cardComputerRegistry,
                                cardUpdateHandler = viewModel,
                                onReorder = { viewModel.reorderCards(it) },
                                onDelete = { viewModel.removeCard(it) },
                                onRecordsClick = onRecordsClick,
                                modifier = Modifier.fillMaxSize(),
                                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                                animateCards = cardAnimationEnabled,
                                currentTheme = currentTheme,
                                accounts = state.accounts,
                                allCategories = state.allCategories,
                                savedFilters = savedFilters,
                                onAccountClick = onAccountClick
                            )
                        }
                    }
                }

                UnifiedPillNavigation(
                    selectedTab = 0,
                    onTabSelected = {},
                    tabs = emptyList(),
                    isFabExpanded = isFabExpanded,
                    onFabToggle = { if (expandableFabEnabled) isFabExpanded = it },
                    onFabClick = {
                        if (!expandableFabEnabled) {
                            onAddClick(0, null)
                        }
                    },
                    fabMenu = if (expandableFabEnabled) {
                        {
                            TransactionFabMenu(
                                isExpanded = isFabExpanded,
                                templates = allTemplates,
                                onAddClick = { type, name ->
                                    isFabExpanded = false
                                    onAddClick(type, name)
                                }
                            )
                        }
                    } else null
                )
            }
        }
    }

    if (showAddCardDialog) {
        val analyticsCardOptions = listOf(
            CardTypeOptionData(
                title = "Category Pie Chart",
                description = "View spending by category",
                icon = Icons.Default.PieChart,
                onClick = {
                    viewModel.addCard(
                        CategoryPieChartCardConfig(
                            cardId = "analytics_category_breakdown_${System.currentTimeMillis()}",
                            order = cardsConfig.cards.size,
                            title = "CATEGORY PIE CHART",
                            filter = TransactionFilter(
                                timelineRange = TimelineRange.LastPeriod(LastPeriodType.LAST_30_DAYS),
                                transactionType = "expense"
                            ),
                            showLabels = true,
                            showLegend = true
                        )
                    )
                    showAddCardDialog = false
                }
            ),
            CardTypeOptionData(
                title = "Category Treemap",
                description = "Expense distribution treemap",
                icon = Icons.Default.BarChart,
                onClick = {
                    viewModel.addCard(
                        CategoryTreemapCardConfig(
                            cardId = "analytics_category_expense_treemap_${System.currentTimeMillis()}",
                            order = cardsConfig.cards.size,
                            title = "CATEGORY TREEMAP",
                            filter = TransactionFilter(
                                timelineRange = TimelineRange.LastPeriod(LastPeriodType.LAST_30_DAYS),
                                transactionType = "expense"
                            ),
                            showLabels = true
                        )
                    )
                    showAddCardDialog = false
                }
            )
        )

        GenericAddCardDialog(
            onDismiss = { showAddCardDialog = false },
            cardOptions = analyticsCardOptions
        )
    }
}
