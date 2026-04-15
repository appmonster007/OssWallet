package com.osscabinet.ossWallet.ui.screens

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.rounded.ArrowDownward
import androidx.compose.material.icons.rounded.ArrowUpward
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.unit.IntOffset
import androidx.compose.foundation.layout.Box
import androidx.compose.ui.Alignment
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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
    val starredTemplates by templateViewModel.starredTemplates.collectAsState()

    val settingsCurrency by settingsViewModel.currency.collectAsState()
    LaunchedEffect(settingsCurrency) {
        analyticsViewModel.onCurrencySelected(settingsCurrency)
    }

    ModalNavigationDrawer(
        drawerState = drawerState,
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
            },
            floatingActionButton = {
                AeonFloatingActionButton(
                    onClick = onAddAccountClick,
                    icon = Icons.Default.Add,
                    contentDescription = "Add Account"
                )
            }
        ) { innerPadding ->
            when (val state = accountsUiState) {
                is AccountsViewUiState.Loading -> {
                    // Reuse DashboardShimmer or create a specific one
                    DashboardShimmer(innerPadding)
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
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(24.dp)
                    ) { section, isDragging, dragModifier ->
                        when (section) {
                            AccountsViewSection.NET_WORTH -> {
                                NetWorthSection(
                                    accounts = state.accounts,
                                    baseCurrency = state.baseCurrency,
                                    exchangeRates = state.exchangeRates,
                                    precision = state.decimalPrecision,
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
                                    headerModifier = dragModifier.fillMaxWidth(),
                                    modifier = Modifier.fillMaxWidth()
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
}




