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
import com.osscabinet.ossWallet.ui.util.calculateTotalInCurrency
import com.osscabinet.ossWallet.ui.util.convertAmount
import com.osscabinet.ossWallet.ui.util.formatCurrencyRight
import com.osscabinet.ossWallet.ui.util.toContainerColor
import androidx.compose.material.icons.filled.MoreHoriz
import com.osscabinet.ossWallet.ui.viewmodel.AccountsViewViewModel
import com.osscabinet.ossWallet.ui.viewmodel.AccountsViewUiState
import com.osscabinet.ossWallet.ui.viewmodel.AccountsViewSection
import com.osscabinet.ossWallet.ui.viewmodel.AccountStats
import com.osscabinet.ossWallet.ui.components.*
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
    accountsViewViewModel: AccountsViewViewModel
) {
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()
    
    val accountsUiState by accountsViewViewModel.uiState.collectAsState()
    val sectionOrder by accountsViewViewModel.sectionOrder.collectAsState()

    val accounts by accountViewModel.accounts.collectAsState()
    val nonArchivedAccounts = remember(accounts) { accounts.filter { !it.isArchived } }
    
    val currentProfile by settingsViewModel.currentProfile.collectAsState()
    val currentTheme by settingsViewModel.theme.collectAsState(initial = "System")
    
    val dbTemplates by templateViewModel.templates.collectAsState()
    val starredTemplates by templateViewModel.starredTemplates.collectAsState()
    
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

    val settingsCurrency by settingsViewModel.currency.collectAsState()
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
                    onDashboardClick = onDashboardClick,
                    onAccountsViewClick = { scope.launch { drawerState.close() } },
                    onRecordsClick = onRecordsClick,
                    onAnalyticsClick = onAnalyticsClick,
                    onBudgetsClick = onBudgetsClick,
                    onLoansClick = onLoansClick,
                    onExchangeRatesClick = onExchangeRatesClick,
                    onTemplatesClick = onTemplatesClick,
                    onManageAccountsClick = onManageAccountsClick,
                    onSettingsClick = onSettingsClick,
                    onAccountClick = onAccountClick,
                    onTemplateClick = { template ->
                        onAddClick(template.transactionType, template.name)
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
                        CardReorderableList(
                            items = sectionOrder,
                            key = { it.ordinal },
                            onReorder = { accountsViewViewModel.updateSectionOrder(it) },
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(innerPadding)
                                .background(MaterialTheme.colorScheme.background),
                            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 4.dp, bottom = 8.dp),
                            verticalArrangement = Arrangement.spacedBy(2.dp)
                        ) { section, isDragging, dragModifier ->
                            when (section) {
                                AccountsViewSection.NET_WORTH -> {
                                    NetWorthSection(
                                        accounts = state.accounts,
                                        baseCurrency = state.baseCurrency,
                                        exchangeRates = state.exchangeRates,
                                        precision = state.decimalPrecision,
                                        fontSize = state.netWorthFontSize,
                                        onFontSizeChange = { accountsViewViewModel.updateNetWorthFontSize(it) },
                                        headerModifier = dragModifier,
                                        modifier = Modifier.fillMaxWidth()
                                    )
                                }
                                AccountsViewSection.CURRENCY_BALANCES -> {
                                    CurrencyAggregationSection(
                                        currencyTotals = state.currencyTotals,
                                        baseCurrency = state.baseCurrency,
                                        exchangeRates = state.exchangeRates,
                                        precision = state.decimalPrecision,
                                        layout = state.currencyBalancesLayout,
                                        onLayoutChange = { accountsViewViewModel.updateCurrencyBalancesLayout(it) },
                                        headerModifier = dragModifier,
                                        modifier = Modifier.fillMaxWidth()
                                    )
                                }
                                AccountsViewSection.ACCOUNTS_LIST -> {
                                    AeonAccountsSection(
                                        onManageClick = onManageAccountsClick,
                                        onAccountClick = onAccountClick,
                                        onGridColumnsChange = { accountsViewViewModel.updateGridColumns(it) },
                                        accounts = state.accounts,
                                        accountStats = state.accountStats,
                                        baseCurrency = state.baseCurrency,
                                        exchangeRates = state.exchangeRates,
                                        currentTheme = currentTheme,
                                        gridColumns = state.gridColumns,
                                        precision = state.decimalPrecision,
                                        showAddAccountCard = state.showAddAccountCard,
                                        onShowAddAccountCardChange = { settingsViewModel.setAccountsViewShowAddAccountCard(it) },
                                        headerModifier = dragModifier.fillMaxWidth(),
                                        modifier = Modifier.fillMaxWidth(),
                                        settingsTitle = "Accounts List Settings"
                                    )
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
                                    onAddClick(type, name)
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
                                    onAddClick(type, name)
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
                                onAddAccountClick()
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
                                onAddAccountClick()
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
                                onAddClick(2, null)
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
                                onAddClick(2, null)
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
                                onAddClick(0, null)
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
                                onAddClick(0, null)
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
}




