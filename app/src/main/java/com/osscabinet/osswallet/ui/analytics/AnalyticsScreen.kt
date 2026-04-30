package com.osscabinet.ossWallet.ui.analytics

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.osscabinet.ossWallet.domain.card.*
import com.osscabinet.ossWallet.domain.model.TransactionFilter
import com.osscabinet.ossWallet.domain.model.settings.*
import com.osscabinet.ossWallet.ui.analytics.components.CategoryBreakdownData
import com.osscabinet.ossWallet.ui.analytics.components.CategoryExpenseTreemapData
import com.osscabinet.ossWallet.ui.components.*
import com.osscabinet.ossWallet.ui.components.cards.CardCallbacks
import com.osscabinet.ossWallet.ui.components.cards.CardWithCallbacks
import com.osscabinet.ossWallet.ui.components.cards.UnifiedCardRenderer
import com.osscabinet.ossWallet.ui.util.formatCurrencyRight
import com.osscabinet.ossWallet.ui.viewmodel.*
import kotlinx.coroutines.launch
import java.math.BigDecimal

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
    cardComputerRegistry: CardComputerRegistry
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val cardsConfig by viewModel.analyticsCardsConfig.collectAsStateWithLifecycle()
    val accounts by accountViewModel.accounts.collectAsStateWithLifecycle()
    val allCategories by transactionViewModel.categories.collectAsStateWithLifecycle()
    val savedFilters by transactionViewModel.savedFilters.collectAsStateWithLifecycle()
    val nonArchivedAccounts = remember(accounts) { accounts.filter { !it.isArchived } }
    val starredTemplates by templateViewModel.starredTemplates.collectAsStateWithLifecycle()
    val currentProfile by settingsViewModel.currentProfile.collectAsStateWithLifecycle()
    val allTemplates by templateViewModel.templates.collectAsStateWithLifecycle()

    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val coroutineScope = rememberCoroutineScope()
    var showAddCardDialog by remember { mutableStateOf(false) }

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
                onCloseDrawer = { coroutineScope.launch { drawerState.close() } }
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
                        IconButton(
                            onClick = { showAddCardDialog = true }
                        ) {
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
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                        .background(MaterialTheme.colorScheme.background)
                ) {
                    // Analytics Cards from config
                    CardReorderableList(
                        items = cardsConfig.cards.sortedBy { it.order },
                        key = { it.cardId },
                        onReorder = { reorderedCards ->
                            coroutineScope.launch { viewModel.reorderCards(reorderedCards) }
                        },
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(horizontal = 24.dp, vertical = 0.dp),
                        verticalArrangement = Arrangement.spacedBy(32.dp)
                    ) { cardConfig, isDragging, dragModifier ->
                        val input = CardInput(
                            transactions = uiState.allTransactions,
                            accounts = uiState.accounts,
                            categories = allCategories,
                            exchangeRates = uiState.exchangeRates,
                            baseCurrency = uiState.selectedCurrency,
                            precision = uiState.decimalPrecision
                        )

                        val cardWithCallbacks = com.osscabinet.ossWallet.ui.components.cards.rememberCardWithCallbacks(
                            config = cardConfig,
                            input = input,
                            registry = cardComputerRegistry,
                            updateHandler = viewModel,
                            onDelete = { coroutineScope.launch { viewModel.removeCard(it) } },
                            onRecordsClick = onRecordsClick,
                            onAccountClick = onAccountClick,
                            currentTheme = "light" // Or observe from settings
                        )

                        cardWithCallbacks?.let {
                            UnifiedCardRenderer(
                                cardWithCallbacks = it,
                                headerModifier = dragModifier,
                                accounts = nonArchivedAccounts,
                                allCategories = allCategories,
                                savedFilters = savedFilters
                            )
                        }
                    }
                }

                AeonExpandableFab(
                    onAddClick = { type, name -> onAddClick(type, name) },
                    templates = allTemplates,
                    fabPaddingBottom = 16.dp
                )
            }
        }
    }

    if (showAddCardDialog) {
        val analyticsCardOptions = listOf(
            CardTypeOptionData(
                title = "Category Breakdown",
                description = "View spending by category",
                icon = Icons.Default.PieChart,
                onClick = {
                    coroutineScope.launch {
                        viewModel.addCard(
                            CategoryBreakdownCardConfig(
                                cardId = "analytics_category_breakdown_${System.currentTimeMillis()}",
                                order = cardsConfig.cards.size,
                                title = "CATEGORY BREAKDOWN",
                                filter = TransactionFilter(timelineRange = com.osscabinet.ossWallet.domain.model.timeline.TimelineRange.LastPeriod(com.osscabinet.ossWallet.domain.model.timeline.LastPeriodType.LAST_30_DAYS)),
                                showLabels = true,
                                showLegend = true
                            )
                        )
                    }
                    showAddCardDialog = false
                }
            ),
            CardTypeOptionData(
                title = "Category Expenses",
                description = "Expense distribution treemap",
                icon = Icons.Default.BarChart,
                onClick = {
                    coroutineScope.launch {
                        viewModel.addCard(
                            CategoryExpenseTreemapCardConfig(
                                cardId = "analytics_category_expense_treemap_${System.currentTimeMillis()}",
                                order = cardsConfig.cards.size,
                                title = "CATEGORY EXPENSES",
                                filter = TransactionFilter(timelineRange = com.osscabinet.ossWallet.domain.model.timeline.TimelineRange.LastPeriod(com.osscabinet.ossWallet.domain.model.timeline.LastPeriodType.LAST_30_DAYS)),
                                showLabels = true
                            )
                        )
                    }
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
