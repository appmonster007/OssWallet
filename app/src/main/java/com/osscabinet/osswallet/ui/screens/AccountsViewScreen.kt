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
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import com.osscabinet.ossWallet.ui.components.AppDrawerContent
import com.osscabinet.ossWallet.domain.model.Transaction
import com.osscabinet.ossWallet.domain.model.CashFlowType
import com.osscabinet.ossWallet.ui.viewmodel.AccountViewModel
import com.osscabinet.ossWallet.ui.viewmodel.TransactionViewModel
import com.osscabinet.ossWallet.ui.viewmodel.SettingsViewModel
import com.osscabinet.ossWallet.ui.viewmodel.AnalyticsViewModel
import com.osscabinet.ossWallet.ui.components.AeonFloatingActionButton
import com.osscabinet.ossWallet.ui.dashboard.components.NetWorthCard
import com.osscabinet.ossWallet.ui.dashboard.components.CurrencyBalancesCard
import com.osscabinet.ossWallet.ui.dashboard.components.CashFlowCard
import com.osscabinet.ossWallet.ui.dashboard.components.AccountsCard
import com.osscabinet.ossWallet.ui.viewmodel.NetWorthUiState
import com.osscabinet.ossWallet.ui.viewmodel.AccountsViewViewModel
import com.osscabinet.ossWallet.ui.viewmodel.AccountsViewUiState
import com.osscabinet.ossWallet.ui.viewmodel.AccountsViewSection
import com.osscabinet.ossWallet.ui.viewmodel.CardManagerViewModel
import com.osscabinet.ossWallet.domain.model.timeline.TimelineRange
import com.osscabinet.ossWallet.ui.components.*
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import java.math.BigDecimal
import java.math.RoundingMode
import android.graphics.Color as AndroidColor

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class, ExperimentalFoundationApi::class)
@Composable
fun AccountsViewScreen(
    onDashboardClick: () -> Unit,
    onRecordsClick: () -> Unit,
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
    cardManagerViewModel: CardManagerViewModel
) {
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()

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

    
    val accountsUiState by accountsViewViewModel.uiState.collectAsStateWithLifecycle()
    val sectionOrder by accountsViewViewModel.sectionOrder.collectAsStateWithLifecycle()
    val accountsViewCardsConfig by cardManagerViewModel.accountsViewCardsConfig.collectAsStateWithLifecycle()
    var showAddCardDialog by remember { mutableStateOf(false) }

    val accounts by accountViewModel.accounts.collectAsStateWithLifecycle()
    val nonArchivedAccounts = remember(accounts) { accounts.filter { !it.isArchived } }

    val currentProfile by settingsViewModel.currentProfile.collectAsStateWithLifecycle()
    val currentTheme by settingsViewModel.theme.collectAsStateWithLifecycle(initialValue = "System")

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
                    onRecordsClick = { navigate(onRecordsClick) },
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
                                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 16.dp),
                                verticalArrangement = Arrangement.spacedBy(12.dp)
                            ) { cardConfig, isDragging, dragModifier ->
                            when (cardConfig) {
                                is com.osscabinet.ossWallet.domain.model.settings.AccountsViewNetWorthCardConfig -> {
                                    NetWorthCard(
                                        uiState = netWorthUiState,
                                        cardId = cardConfig.cardId,
                                        fontSize = cardConfig.fontSize,
                                        timelineRange = cardConfig.timelineRange,
                                        onFontSizeChange = { _, it ->
                                            cardManagerViewModel.updateAccountsViewCard(cardConfig.cardId) { config ->
                                                if (config is com.osscabinet.ossWallet.domain.model.settings.AccountsViewNetWorthCardConfig) {
                                                    config.copy(fontSize = it)
                                                } else config
                                            }
                                        },
                                        onTimelineRangeChange = { _, it ->
                                            cardManagerViewModel.updateAccountsViewCard(cardConfig.cardId) { config ->
                                                if (config is com.osscabinet.ossWallet.domain.model.settings.AccountsViewNetWorthCardConfig) {
                                                    config.copy(timelineRange = it)
                                                } else config
                                            }
                                        },
                                        modifier = Modifier.fillMaxWidth(),
                                        headerModifier = dragModifier
                                    )
                                }
                                is com.osscabinet.ossWallet.domain.model.settings.AccountsViewCurrencyBalancesCardConfig -> {
                                    CurrencyBalancesCard(
                                        currencyTotals = state.currencyTotals,
                                        baseCurrency = state.baseCurrency,
                                        cardId = cardConfig.cardId,
                                        precision = state.decimalPrecision,
                                        layout = cardConfig.layout,
                                        showBaseConversion = cardConfig.showBaseConversion,
                                        timelineRange = cardConfig.timelineRange,
                                        onLayoutChange = { _, it ->
                                            cardManagerViewModel.updateAccountsViewCard(cardConfig.cardId) { config ->
                                                if (config is com.osscabinet.ossWallet.domain.model.settings.AccountsViewCurrencyBalancesCardConfig) {
                                                    config.copy(layout = it)
                                                } else config
                                            }
                                        },
                                        onShowBaseConversionChange = { _, it ->
                                            cardManagerViewModel.updateAccountsViewCard(cardConfig.cardId) { config ->
                                                if (config is com.osscabinet.ossWallet.domain.model.settings.AccountsViewCurrencyBalancesCardConfig) {
                                                    config.copy(showBaseConversion = it)
                                                } else config
                                            }
                                        },
                                        onTimelineRangeChange = { _, it ->
                                            cardManagerViewModel.updateAccountsViewCard(cardConfig.cardId) { config ->
                                                if (config is com.osscabinet.ossWallet.domain.model.settings.AccountsViewCurrencyBalancesCardConfig) {
                                                    config.copy(timelineRange = it)
                                                } else config
                                            }
                                        },
                                        headerModifier = dragModifier,
                                        modifier = Modifier.fillMaxWidth()
                                    )
                                }
                                is com.osscabinet.ossWallet.domain.model.settings.AccountsViewCashFlowCardConfig -> {
                                    CashFlowCard(
                                        cashFlowResult = state.cashFlowResult,
                                        baseCurrency = state.baseCurrency,
                                        currentTheme = currentTheme,
                                        cardId = cardConfig.cardId,
                                        modifier = Modifier.fillMaxWidth(),
                                        headerModifier = dragModifier,
                                        showTitle = true,
                                        precision = state.decimalPrecision,
                                        showBaseConversion = cardConfig.showBaseConversion,
                                        timelineRange = cardConfig.timelineRange,
                                        onShowBaseConversionChange = { _, it ->
                                            cardManagerViewModel.updateAccountsViewCard(cardConfig.cardId) { config ->
                                                if (config is com.osscabinet.ossWallet.domain.model.settings.AccountsViewCashFlowCardConfig) {
                                                    config.copy(showBaseConversion = it)
                                                } else config
                                            }
                                        },
                                        onTimelineRangeChange = { _, it ->
                                            cardManagerViewModel.updateAccountsViewCard(cardConfig.cardId) { config ->
                                                if (config is com.osscabinet.ossWallet.domain.model.settings.AccountsViewCashFlowCardConfig) {
                                                    config.copy(timelineRange = it)
                                                } else config
                                            }
                                        }
                                    )
                                }
                                is com.osscabinet.ossWallet.domain.model.settings.AccountsViewAccountsListCardConfig -> {
                                    AccountsCard(
                                        onManageClick = { navigate(onManageAccountsClick) },
                                        onAccountClick = { id -> navigate { onAccountClick(id) } },
                                        onGridColumnsChange = { _, it ->
                                            cardManagerViewModel.updateAccountsViewCard(cardConfig.cardId) { config ->
                                                if (config is com.osscabinet.ossWallet.domain.model.settings.AccountsViewAccountsListCardConfig) {
                                                    config.copy(gridColumns = it)
                                                } else config
                                            }
                                        },
                                        accounts = state.accounts,
                                        accountStats = state.accountStats,
                                        baseCurrency = state.baseCurrency,
                                        cardId = cardConfig.cardId,
                                        currentTheme = currentTheme,
                                        gridColumns = cardConfig.gridColumns,
                                        precision = state.decimalPrecision,
                                        layout = cardConfig.layout,
                                        onLayoutChange = { _, it ->
                                            cardManagerViewModel.updateAccountsViewCard(cardConfig.cardId) { config ->
                                                if (config is com.osscabinet.ossWallet.domain.model.settings.AccountsViewAccountsListCardConfig) {
                                                    config.copy(layout = it)
                                                } else config
                                            }
                                        },
                                        timelineRange = cardConfig.timelineRange,
                                        onTimelineRangeChange = { _, it ->
                                            cardManagerViewModel.updateAccountsViewCard(cardConfig.cardId) { config ->
                                                if (config is com.osscabinet.ossWallet.domain.model.settings.AccountsViewAccountsListCardConfig) {
                                                    config.copy(timelineRange = it)
                                                } else config
                                            }
                                        },
                                        showAddAccountCard = cardConfig.showAddAccountCard,
                                        onShowAddAccountCardChange = { _, it ->
                                            cardManagerViewModel.updateAccountsViewCard(cardConfig.cardId) { config ->
                                                if (config is com.osscabinet.ossWallet.domain.model.settings.AccountsViewAccountsListCardConfig) {
                                                    config.copy(showAddAccountCard = it)
                                                } else config
                                            }
                                        },
                                        headerModifier = dragModifier.fillMaxWidth(),
                                        modifier = Modifier.fillMaxWidth(),
                                        settingsTitle = "Accounts List Settings"
                                    )
                                }
                            }
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
fun AccountsViewCardTypeSelectionDialog(
    onDismiss: () -> Unit,
    onCardTypeSelected: (com.osscabinet.ossWallet.ui.viewmodel.AccountsViewCardType) -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add Card") },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text("Select a card type to add:")
                CardTypeOption(
                    title = "Net Worth",
                    description = "Display your net worth over time",
                    onClick = { onCardTypeSelected(com.osscabinet.ossWallet.ui.viewmodel.AccountsViewCardType.NET_WORTH) }
                )
                CardTypeOption(
                    title = "Currency Balances",
                    description = "Show currency breakdown",
                    onClick = { onCardTypeSelected(com.osscabinet.ossWallet.ui.viewmodel.AccountsViewCardType.CURRENCY_BALANCES) }
                )
                CardTypeOption(
                    title = "Cash Flow",
                    description = "Track income and expenses",
                    onClick = { onCardTypeSelected(com.osscabinet.ossWallet.ui.viewmodel.AccountsViewCardType.CASH_FLOW) }
                )
                CardTypeOption(
                    title = "Accounts List",
                    description = "Display your accounts",
                    onClick = { onCardTypeSelected(com.osscabinet.ossWallet.ui.viewmodel.AccountsViewCardType.ACCOUNTS_LIST) }
                )
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@Composable
fun CardTypeOption(
    title: String,
    description: String,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(
            modifier = Modifier.padding(12.dp)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium
            )
            Text(
                text = description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}



