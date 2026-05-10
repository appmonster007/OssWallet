package com.osscabinet.ossWallet.ui.analytics

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
    onMenuClick: () -> Unit,
    onBackClick: () -> Unit = {},
    isTopLevel: Boolean = false,
    viewModel: AnalyticsViewModel,
    accountViewModel: AccountViewModel,
    transactionViewModel: TransactionViewModel,
    templateViewModel: TemplateViewModel,
    settingsViewModel: SettingsViewModel,
    onRecordsClick: (com.osscabinet.ossWallet.ui.navigation.RecordsFilterContext) -> Unit,
    onAccountClick: (String) -> Unit,
    onEditTransactionClick: (Long) -> Unit,
    onAddClick: (Int?, Long?) -> Unit,
    cardComputerRegistry: CardComputerRegistry
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

    var showAddCardDialog by remember { mutableStateOf(false) }
    var isFabExpanded by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Intelligence", style = MaterialTheme.typography.headlineSmall) },
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
                                    precision = state.decimalPrecision,
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
                                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                                animateCards = cardAnimationEnabled,
                                currentTheme = currentTheme,
                                accounts = state.accounts,
                                allCategories = state.allCategories,
                                savedFilters = savedFilters,
                                onAccountClick = onAccountClick,
                                onTransactionClick = onEditTransactionClick
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
                    transactionType = "expense"
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
