package com.osscabinet.ossWallet.ui.screens

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.produceState
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import com.osscabinet.ossWallet.ui.components.AppDrawerContent
import com.osscabinet.ossWallet.ui.navigation.AccountsFilterContext
import kotlinx.serialization.json.Json
import java.net.URLDecoder
import java.nio.charset.StandardCharsets
import com.osscabinet.ossWallet.domain.model.Transaction
import com.osscabinet.ossWallet.domain.model.Account
import com.osscabinet.ossWallet.domain.model.CashFlowType
import com.osscabinet.ossWallet.domain.usecase.CurrencyAggregationItem
import com.osscabinet.ossWallet.domain.usecase.AccountSummaryItem
import com.osscabinet.ossWallet.domain.usecase.CashFlowResult
import com.osscabinet.ossWallet.ui.viewmodel.AccountViewModel
import com.osscabinet.ossWallet.ui.viewmodel.TransactionViewModel
import com.osscabinet.ossWallet.ui.viewmodel.SettingsViewModel
import com.osscabinet.ossWallet.ui.viewmodel.AnalyticsViewModel
import com.osscabinet.ossWallet.ui.components.AeonFloatingActionButton
import com.osscabinet.ossWallet.ui.dashboard.components.NetWorthCard
import com.osscabinet.ossWallet.ui.dashboard.components.CurrencyBalancesCard
import com.osscabinet.ossWallet.ui.dashboard.components.CashFlowCard
import com.osscabinet.ossWallet.ui.dashboard.components.AccountsCard
import com.osscabinet.ossWallet.ui.components.cards.UnifiedCardRenderer
import com.osscabinet.ossWallet.ui.components.cards.CardRenderData
import com.osscabinet.ossWallet.ui.components.cards.CardCallbacks
import com.osscabinet.ossWallet.ui.components.cards.CardWithCallbacks
import com.osscabinet.ossWallet.ui.components.cards.computeNetWorth
import com.osscabinet.ossWallet.ui.components.cards.computeCurrencyTotalsWithFinancialUseCases
import com.osscabinet.ossWallet.ui.components.cards.computeCashFlowWithFinancialUseCases
import com.osscabinet.ossWallet.ui.components.cards.computeAccountStats
import com.osscabinet.ossWallet.ui.viewmodel.NetWorthUiState
import com.osscabinet.ossWallet.ui.viewmodel.AccountsViewViewModel
import com.osscabinet.ossWallet.ui.viewmodel.AccountsViewUiState
import com.osscabinet.ossWallet.ui.viewmodel.AccountsViewSection
import com.osscabinet.ossWallet.ui.viewmodel.CardManagerViewModel
import com.osscabinet.ossWallet.ui.components.CardTypeOptionData
import com.osscabinet.ossWallet.ui.components.GenericAddCardDialog
import com.osscabinet.ossWallet.domain.model.timeline.TimelineRange
import com.osscabinet.ossWallet.ui.components.*
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import java.math.BigDecimal
import java.math.RoundingMode
import android.graphics.Color as AndroidColor

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
fun AccountsViewScreen(
    onDashboardClick: () -> Unit,
    onRecordsClick: (com.osscabinet.ossWallet.ui.navigation.RecordsFilterContext) -> Unit,
    onAnalyticsClick: () -> Unit,
    onBudgetsClick: () -> Unit,
    onLoansClick: () -> Unit,
    onExchangeRatesClick: () -> Unit,
    onTemplatesClick: () -> Unit,
    onManageAccountsClick: () -> Unit,
    onSettingsClick: () -> Unit,
    onAccountClick: (String) -> Unit,
    onAddAccountClick: () -> Unit,
    onAddClick: (Int, String?) -> Unit,
    accountViewModel: AccountViewModel,
    transactionViewModel: TransactionViewModel,
    settingsViewModel: SettingsViewModel,
    analyticsViewModel: AnalyticsViewModel,
    templateViewModel: com.osscabinet.ossWallet.ui.viewmodel.TemplateViewModel,
    accountsViewViewModel: AccountsViewViewModel,
    cardManagerViewModel: CardManagerViewModel,
    filterContext: String? = null
) {
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()

    var navTriggered by remember { mutableStateOf(false) }

    // Reset navTriggered when returning to this screen
    androidx.lifecycle.compose.LifecycleEventEffect(androidx.lifecycle.Lifecycle.Event.ON_RESUME) {
        navTriggered = false
    }

    // Apply initial filter when screen opens with context
    LaunchedEffect(filterContext) {
        filterContext?.let { contextString ->
            try {
                val decodedContext = URLDecoder.decode(contextString, StandardCharsets.UTF_8.toString())
                val context = Json.decodeFromString<AccountsFilterContext>(decodedContext)
                accountsViewViewModel.initializeAccountsFilter(context)
            } catch (e: Exception) {
                // If context is invalid, use default
                accountsViewViewModel.initializeAccountsFilter(AccountsFilterContext.None)
            }
        } ?: run {
            // If no context provided, ensure filter is cleared
            accountsViewViewModel.clearAccountsFilter()
        }
    }

    // Clear filter when leaving the screen
    DisposableEffect(Unit) {
        onDispose {
            accountsViewViewModel.clearAccountsFilter()
        }
    }

    val navigate: (() -> Unit) -> Unit = { action ->
        if (!navTriggered) {
            navTriggered = true
            action()
        }
    }

    
    val accountsUiState by accountsViewViewModel.uiState.collectAsStateWithLifecycle()
    val sectionOrder by accountsViewViewModel.sectionOrder.collectAsStateWithLifecycle()
    val accountsViewCardsConfig by cardManagerViewModel.accountsViewCardsConfig.collectAsStateWithLifecycle()
    val accountFilter by accountsViewViewModel.accountFilter.collectAsStateWithLifecycle()
    val initialFilterContext by accountsViewViewModel.initialFilterContext.collectAsStateWithLifecycle()
    var showAddCardDialog by remember { mutableStateOf(false) }

    val accounts by accountViewModel.accounts.collectAsStateWithLifecycle()
    val nonArchivedAccounts = remember(accounts) { accounts.filter { !it.isArchived } }
    val accountMap = remember(accounts) { accounts.associateBy { it.id to it.name } }
    val accountNameMap = remember(accounts) { accounts.associate { it.id to it.name } }

    val currentProfile by settingsViewModel.currentProfile.collectAsStateWithLifecycle()
    val currentTheme by settingsViewModel.theme.collectAsStateWithLifecycle(initialValue = "System")
    val cardAnimationEnabled by settingsViewModel.cardAnimationEnabled.collectAsStateWithLifecycle(initialValue = true)

    val dbTemplates by templateViewModel.templates.collectAsStateWithLifecycle()
    val starredTemplates by templateViewModel.starredTemplates.collectAsStateWithLifecycle()
    
    val templates = remember(dbTemplates, starredTemplates) {
        val displayTemplates = if (starredTemplates.isNotEmpty()) {
            starredTemplates
        } else if (dbTemplates.isNotEmpty()) {
            dbTemplates.take(5)
        } else {
            emptyList()
        }

        if (displayTemplates.isEmpty()) {
            listOf(
                Triple("Grocery Run", Icons.Rounded.ShoppingCart, 0),
                Triple("Salary Deposit", Icons.Rounded.Payments, 1),
                Triple("Rent Payment", Icons.Rounded.Home, 0),
                Triple("Coffee", Icons.Rounded.CoffeeMaker, 0)
            )
        } else {
            displayTemplates.map {
                Triple(it.name, when(it.transactionType) {
                    1 -> Icons.Rounded.Payments
                    2 -> Icons.Rounded.SwapHoriz
                    else -> Icons.Rounded.ShoppingCart
                }, it.transactionType)
            }
        }
    }

    var isFabExpanded by remember { mutableStateOf(false) }

    val settingsCurrency by settingsViewModel.currency.collectAsStateWithLifecycle()
    LaunchedEffect(settingsCurrency) {
        analyticsViewModel.onCurrencySelected(settingsCurrency)
    }

    Box(modifier = Modifier.fillMaxSize()) {
        ModalNavigationDrawer(
            drawerState = drawerState,
            modifier = Modifier.fillMaxSize(),
            drawerContent = {
                AppDrawerContent(
                    currentProfile = currentProfile,
                    nonArchivedAccounts = nonArchivedAccounts,
                    starredTemplates = starredTemplates,
                    selectedDestination = "accounts_view",
                    onDashboardClick = { navigate(onDashboardClick) },
                    onAccountsViewClick = { scope.launch { drawerState.close() } },
                    onRecordsClick = { navigate { onRecordsClick(com.osscabinet.ossWallet.ui.navigation.RecordsFilterContext.None) } },
                    onAnalyticsClick = { navigate(onAnalyticsClick) },
                    onBudgetsClick = { navigate(onBudgetsClick) },
                    onLoansClick = { navigate(onLoansClick) },
                    onExchangeRatesClick = { navigate(onExchangeRatesClick) },
                    onTemplatesClick = { navigate(onTemplatesClick) },
                    onManageAccountsClick = { navigate(onManageAccountsClick) },
                    onSettingsClick = { navigate(onSettingsClick) },
                    onAccountClick = { id -> navigate { onAccountClick(id) } },
                    onTemplateClick = { template ->
                        navigate { onAddClick(template.transactionType, template.name) }
                    },
                    onCloseDrawer = { scope.launch { drawerState.close() } }
                )
            }
        ) {
            Scaffold(
                topBar = {
                    TopAppBar(
                        navigationIcon = {
                            IconButton(onClick = { scope.launch { drawerState.open() } }) {
                                Icon(Icons.Default.Menu, contentDescription = "Menu")
                            }
                        },
                        title = {
                            Column {
                                Text("Accounts", style = MaterialTheme.typography.titleLarge)
                                Text("Manage your accounts", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.outline)
                            }
                        }
                    )
                }
            ) { innerPadding ->
                // Context indicator banner (if filter is applied)
                if (initialFilterContext != AccountsFilterContext.None) {
                    AccountFilterIndicator(
                        context = initialFilterContext,
                        accountNames = accountNameMap,
                        onReset = { accountsViewViewModel.resetToInitialFilter() }
                    )
                }

                when (val state = accountsUiState) {
                    is AccountsViewUiState.Loading -> {
                        AeonShimmer(
                            innerPadding = innerPadding,
                            showCashflow = false
                        )
                    }
                    is AccountsViewUiState.Success -> {
                        val netWorthUiState = remember(
                            state.netWorth,
                            state.netIncome,
                            state.netExpense,
                            state.baseCurrency,
                            state.decimalPrecision,
                            state.netWorthFontSize,
                            state.netWorthTimelineRange
                        ) {
                            NetWorthUiState.Success(
                                netWorth = state.netWorth,
                                netIncome = state.netIncome,
                                netExpense = state.netExpense,
                                baseCurrency = state.baseCurrency,
                                precision = state.decimalPrecision,
                                fontSize = state.netWorthFontSize,
                                timelineRange = state.netWorthTimelineRange
                            )
                        }

                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(innerPadding)
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
                                    text = "Accounts View",
                                    style = MaterialTheme.typography.headlineSmall,
                                    fontWeight = FontWeight.Bold
                                )
                                IconButton(
                                    onClick = { showAddCardDialog = true }
                                ) {
                                    Icon(Icons.Default.Add, contentDescription = "Add Card")
                                }
                            }

                            CardReorderableList(
                                items = accountsViewCardsConfig.cards.sortedBy { it.order },
                                key = { it.cardId },
                                onReorder = { cardManagerViewModel.reorderAccountsViewCards(it) },
                                modifier = Modifier.fillMaxWidth(),
                                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = calculateScrollBottomPadding()),
                                verticalArrangement = Arrangement.spacedBy(12.dp),
                                animateCards = cardAnimationEnabled
                            ) { cardConfig, isDragging, dragModifier ->
                            val cardWithCallbacks = when (cardConfig) {
                                is com.osscabinet.ossWallet.domain.model.settings.NetWorthCardConfig -> {
                                    var cardNetWorthData by remember { mutableStateOf<Pair<BigDecimal, CashFlowResult>?>(null) }
                                    LaunchedEffect(cardConfig, state) {
                                        cardNetWorthData = cardConfig.computeNetWorth(
                                            accounts = state.accounts,
                                            allTransactions = state.allTransactions,
                                            financialUseCases = accountsViewViewModel.financialUseCases,
                                            baseCurrency = state.baseCurrency,
                                            exchangeRates = state.exchangeRates,
                                            precision = state.decimalPrecision
                                        )
                                    }

                                    cardNetWorthData?.let { (netWorth, cashFlow) ->
                                        val cardUiState = NetWorthUiState.Success(
                                            netWorth = netWorth,
                                            netIncome = cashFlow.totalIncomeInBase,
                                            netExpense = cashFlow.totalExpensesInBase,
                                            baseCurrency = state.baseCurrency,
                                            precision = state.decimalPrecision,
                                            fontSize = cardConfig.fontSize,
                                            timelineRange = cardConfig.timelineRange
                                        )
                                        CardWithCallbacks(
                                            renderData = CardRenderData.NetWorth(
                                                config = cardConfig,
                                                uiState = cardUiState
                                            ),
                                            callbacks = CardCallbacks.WithFontSizeAndTimeline(
                                                onFontSizeChange = { cardId, fontSize -> accountsViewViewModel.updateCardNetWorthFontSize(cardId, fontSize) },
                                                onTimelineRangeChange = { cardId, range -> accountsViewViewModel.updateCardNetWorthTimelineRange(cardId, range) },
                                                onDelete = { cardManagerViewModel.removeAccountsViewCard(cardConfig.cardId) }
                                            )
                                        )
                                    }
                                }
                                is com.osscabinet.ossWallet.domain.model.settings.CurrencyBalancesCardConfig -> {
                                    var currencyTotals by remember { mutableStateOf<List<CurrencyAggregationItem>?>(null) }
                                    LaunchedEffect(cardConfig, state) {
                                        currencyTotals = cardConfig.computeCurrencyTotalsWithFinancialUseCases(
                                            accounts = state.accounts,
                                            allTransactions = state.allTransactions,
                                            financialUseCases = accountsViewViewModel.financialUseCases,
                                            baseCurrency = state.baseCurrency,
                                            exchangeRates = state.exchangeRates,
                                            precision = state.decimalPrecision
                                        )
                                    }

                                    currencyTotals?.let { totals ->
                                        CardWithCallbacks(
                                            renderData = CardRenderData.CurrencyBalances(
                                                config = cardConfig,
                                                currencyTotals = totals,
                                                baseCurrency = state.baseCurrency,
                                                precision = state.decimalPrecision
                                            ),
                                            callbacks = CardCallbacks.CurrencyBalances(
                                                onLayoutChange = { cardId, layout -> accountsViewViewModel.updateCardCurrencyBalancesLayout(cardId, layout) },
                                                onShowBaseConversionChange = { cardId, show -> accountsViewViewModel.updateCardCurrencyBalancesShowBaseConversion(cardId, show) },
                                                onTimelineRangeChange = { cardId, range -> accountsViewViewModel.updateCardCurrencyBalancesTimelineRange(cardId, range) },
                                                onCurrencyClick = { currency, timelineRange ->
                                                    val (start, end) = com.osscabinet.ossWallet.domain.service.TimelineService.getDateRange(timelineRange)
                                                    onRecordsClick(com.osscabinet.ossWallet.ui.navigation.RecordsFilterContext.Custom(
                                                        currencies = listOf(currency),
                                                        startDate = start,
                                                        endDate = end
                                                    ))
                                                },
                                                onDelete = { cardManagerViewModel.removeAccountsViewCard(cardConfig.cardId) }
                                            )
                                        )
                                    }
                                }
                                is com.osscabinet.ossWallet.domain.model.settings.CashFlowCardConfig -> {
                                    var cashFlowResult by remember { mutableStateOf<CashFlowResult?>(null) }
                                    LaunchedEffect(cardConfig, state) {
                                        cashFlowResult = cardConfig.computeCashFlowWithFinancialUseCases(
                                            allTransactions = state.allTransactions,
                                            financialUseCases = accountsViewViewModel.financialUseCases,
                                            baseCurrency = state.baseCurrency,
                                            exchangeRates = state.exchangeRates,
                                            precision = state.decimalPrecision
                                        )
                                    }

                                    cashFlowResult?.let { result ->
                                        CardWithCallbacks(
                                            renderData = CardRenderData.CashFlow(
                                                config = cardConfig,
                                                cashFlowResult = result,
                                                baseCurrency = state.baseCurrency,
                                                precision = state.decimalPrecision
                                            ),
                                            callbacks = CardCallbacks.CashFlow(
                                                onShowBaseConversionChange = { cardId, show -> accountsViewViewModel.updateCardCashFlowShowBaseConversion(cardId, show) },
                                                onTimelineRangeChange = { cardId, range -> accountsViewViewModel.updateCardCashFlowTimelineRange(cardId, range) },
                                                onClickIncome = { onRecordsClick(com.osscabinet.ossWallet.ui.navigation.RecordsFilterContext.Custom(isIncome = true)) },
                                                onClickExpense = { onRecordsClick(com.osscabinet.ossWallet.ui.navigation.RecordsFilterContext.Custom(isIncome = false)) },
                                                onDelete = { cardManagerViewModel.removeAccountsViewCard(cardConfig.cardId) }
                                            )
                                        )
                                    }
                                }
                                is com.osscabinet.ossWallet.domain.model.settings.AccountsCardConfig -> {
                                    val accountStatsData by produceState<Pair<List<Account>, Map<String, AccountSummaryItem>>?>(initialValue = null, key1 = cardConfig, key2 = state) {
                                        value = cardConfig.computeAccountStats(
                                            accounts = state.accounts,
                                            allTransactions = state.allTransactions,
                                            cardComputationService = accountsViewViewModel.cardComputationService,
                                            baseCurrency = state.baseCurrency,
                                            exchangeRates = state.exchangeRates,
                                            precision = state.decimalPrecision
                                        )
                                    }

                                    accountStatsData?.let { (accounts, accountStats) ->
                                        CardWithCallbacks(
                                            renderData = CardRenderData.Accounts(
                                                config = cardConfig,
                                                accounts = accounts,
                                                accountStats = accountStats,
                                                baseCurrency = state.baseCurrency,
                                                currentTheme = currentTheme,
                                                precision = state.decimalPrecision
                                            ),
                                            callbacks = CardCallbacks.Accounts(
                                                onGridColumnsChange = { cardId, columns -> accountsViewViewModel.updateCardAccountsGridColumns(cardId, columns) },
                                                onLayoutChange = { cardId, layout -> accountsViewViewModel.updateCardAccountsLayout(cardId, layout) },
                                                onTimelineRangeChange = { cardId, range -> accountsViewViewModel.updateCardAccountsTimelineRange(cardId, range) },
                                                onShowAddAccountCardChange = { cardId, show -> /* No-op for AccountsView */ },
                                                onManageClick = { navigate(onManageAccountsClick) },
                                                onAccountClick = { id -> navigate { onAccountClick(id) } },
                                                onHeaderClick = {},
                                                onRecordsClick = { context -> onRecordsClick(context) },
                                                onDelete = { cardManagerViewModel.removeAccountsViewCard(cardConfig.cardId) }
                                            )
                                        )
                                    }
                                }
                                else -> null
                            }
                            
                            cardWithCallbacks?.let {
                                UnifiedCardRenderer(
                                    renderData = it.renderData,
                                    callbacks = it.callbacks,
                                    modifier = Modifier.fillMaxWidth(),
                                    headerModifier = dragModifier,
                                    showTitle = true,
                                    currentTheme = currentTheme
                                )
                            } ?: Box(modifier = Modifier.size(0.dp))
                        }
                        }
                    }
                    is AccountsViewUiState.Error -> {
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Text(state.message)
                        }
                    }
                }
            }
        }

        // Background dimming
        AnimatedVisibility(
            visible = isFabExpanded,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier.fillMaxSize().zIndex(1f)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.4f))
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null
                    ) { isFabExpanded = false }
            )
        }

        // Expandable FAB Menu
        Box(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(16.dp)
                .zIndex(2f),
            contentAlignment = Alignment.BottomEnd
        ) {
            Column(
                horizontalAlignment = Alignment.End,
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.padding(bottom = 72.dp, end = 4.dp)
            ) {
                templates.forEach { (name, icon, type) ->
                    AnimatedVisibility(
                        visible = isFabExpanded,
                        enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
                        exit = slideOutVertically(targetOffsetY = { it }) + fadeOut(),
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Surface(
                                color = MaterialTheme.colorScheme.surface,
                                shape = RoundedCornerShape(8.dp),
                                shadowElevation = 4.dp,
                                onClick = { 
                                    isFabExpanded = false
                                    navigate { onAddClick(type, name) }
                                }
                            ) {
                                Text(
                                    "Template: $name",
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                                    style = MaterialTheme.typography.labelLarge
                                )
                            }
                            AeonFloatingActionButton(
                                onClick = { 
                                    isFabExpanded = false
                                    navigate { onAddClick(type, name) }
                                },
                                modifier = Modifier.size(40.dp),
                                icon = icon,
                                containerColor = MaterialTheme.colorScheme.secondaryContainer,
                                contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                            )
                        }
                    }
                }

                // Add Account
                AnimatedVisibility(
                    visible = isFabExpanded,
                    enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
                    exit = slideOutVertically(targetOffsetY = { it }) + fadeOut(),
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Surface(
                            color = MaterialTheme.colorScheme.surface,
                            shape = RoundedCornerShape(8.dp),
                            shadowElevation = 4.dp,
                            onClick = { 
                                isFabExpanded = false
                                navigate(onAddAccountClick)
                            }
                        ) {
                            Text(
                                "Add Account",
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                                style = MaterialTheme.typography.labelLarge
                            )
                        }
                        AeonFloatingActionButton(
                            onClick = { 
                                isFabExpanded = false
                                navigate(onAddAccountClick)
                            },
                            modifier = Modifier.size(40.dp),
                            icon = Icons.Default.AccountBalance,
                            containerColor = MaterialTheme.colorScheme.secondaryContainer,
                            contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                        )
                    }
                }

                // Transfer
                AnimatedVisibility(
                    visible = isFabExpanded,
                    enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
                    exit = slideOutVertically(targetOffsetY = { it }) + fadeOut(),
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Surface(
                            color = MaterialTheme.colorScheme.surface,
                            shape = RoundedCornerShape(8.dp),
                            shadowElevation = 4.dp,
                            onClick = { 
                                isFabExpanded = false
                                navigate { onAddClick(2, null) }
                            }
                        ) {
                            Text(
                                "Transfer",
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                                style = MaterialTheme.typography.labelLarge
                            )
                        }
                        AeonFloatingActionButton(
                            onClick = { 
                                isFabExpanded = false
                                navigate { onAddClick(2, null) }
                            },
                            modifier = Modifier.size(40.dp),
                            icon = Icons.Default.SyncAlt,
                            containerColor = MaterialTheme.colorScheme.secondaryContainer,
                            contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                        )
                    }
                }

                // New Transaction
                AnimatedVisibility(
                    visible = isFabExpanded,
                    enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
                    exit = slideOutVertically(targetOffsetY = { it }) + fadeOut(),
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Surface(
                            color = MaterialTheme.colorScheme.surface,
                            shape = RoundedCornerShape(8.dp),
                            shadowElevation = 4.dp,
                            onClick = { 
                                isFabExpanded = false
                                navigate { onAddClick(0, null) }
                            }
                        ) {
                            Text(
                                "New Transaction",
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                                style = MaterialTheme.typography.labelLarge
                            )
                        }
                        AeonFloatingActionButton(
                            onClick = { 
                                isFabExpanded = false
                                navigate { onAddClick(0, null) }
                            },
                            modifier = Modifier.size(40.dp),
                            icon = Icons.Default.Add,
                            containerColor = MaterialTheme.colorScheme.secondaryContainer,
                            contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                        )
                    }
                }
            }

            val rotation by animateFloatAsState(
                targetValue = if (isFabExpanded) 45f else 0f,
                animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy)
            )

            AeonFloatingActionButton(
                onClick = { isFabExpanded = !isFabExpanded },
                modifier = Modifier
                    .zIndex(3f)
                    .graphicsLayer { rotationZ = rotation },
                icon = Icons.Default.Add,
                contentDescription = if (isFabExpanded) "Close" else "Add"
            )
        }
    }

    if (showAddCardDialog) {
        AccountsViewCardTypeSelectionDialog(
            onDismiss = { showAddCardDialog = false },
            onCardTypeSelected = { cardType ->
                cardManagerViewModel.addAccountsViewCard(cardType)
                showAddCardDialog = false
            }
        )
    }
}

@Composable
fun AccountFilterIndicator(
    context: AccountsFilterContext,
    accountNames: Map<String, String>,
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
                imageVector = Icons.Default.FilterList,
                contentDescription = "Applied filter",
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(16.dp)
            )
            Text(
                text = getAccountContextDisplayName(context, accountNames),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.primary
            )
        }
        TextButton(onClick = onReset) {
            Text("Reset")
        }
    }
}

private fun getAccountContextDisplayName(context: AccountsFilterContext, accountNames: Map<String, String>): String {
    return when (context) {
        is AccountsFilterContext.None -> "All"
        is AccountsFilterContext.ByType -> "Type: ${context.accountType}"
        is AccountsFilterContext.ByCurrency -> "Currency: ${context.currency}"
        is AccountsFilterContext.Custom -> {
            val parts = mutableListOf<String>()
            if (context.accountTypes.isNotEmpty()) parts += "Types: ${context.accountTypes.joinToString()}"
            if (context.currencies.isNotEmpty()) parts += "Currencies: ${context.currencies.joinToString()}"
            if (context.minBalance != null || context.maxBalance != null) {
                parts += "Balance: ${context.minBalance ?: "0"} - ${context.maxBalance ?: "∞"}"
            }
            if (parts.isEmpty()) "Custom" else parts.joinToString(", ")
        }
    }
}

@Composable
fun AccountsViewCardTypeSelectionDialog(
    onDismiss: () -> Unit,
    onCardTypeSelected: (com.osscabinet.ossWallet.ui.viewmodel.AccountsViewCardType) -> Unit
) {
    GenericAddCardDialog(
        onDismiss = onDismiss,
        cardOptions = listOf(
            CardTypeOptionData(
                title = "Net Worth",
                description = "Display your net worth over time",
                onClick = { onCardTypeSelected(com.osscabinet.ossWallet.ui.viewmodel.AccountsViewCardType.NET_WORTH) }
            ),
            CardTypeOptionData(
                title = "Currency Balances",
                description = "Show currency breakdown",
                onClick = { onCardTypeSelected(com.osscabinet.ossWallet.ui.viewmodel.AccountsViewCardType.CURRENCY_BALANCES) }
            ),
            CardTypeOptionData(
                title = "Cash Flow",
                description = "Track income and expenses",
                onClick = { onCardTypeSelected(com.osscabinet.ossWallet.ui.viewmodel.AccountsViewCardType.CASH_FLOW) }
            ),
            CardTypeOptionData(
                title = "Accounts List",
                description = "Display your accounts",
                onClick = { onCardTypeSelected(com.osscabinet.ossWallet.ui.viewmodel.AccountsViewCardType.ACCOUNTS_LIST) }
            )
        )
    )
}



