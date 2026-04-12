package com.osscabinet.gleemanager.ui.screens

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
import com.osscabinet.gleemanager.ui.components.AppDrawerContent
import com.osscabinet.gleemanager.ui.viewmodel.AccountViewModel
import com.osscabinet.gleemanager.ui.viewmodel.TransactionViewModel
import com.osscabinet.gleemanager.ui.viewmodel.SettingsViewModel
import com.osscabinet.gleemanager.ui.viewmodel.AnalyticsViewModel
import com.osscabinet.gleemanager.ui.util.formatCurrencyRight
import com.osscabinet.gleemanager.ui.util.toContainerColor
import com.osscabinet.gleemanager.ui.util.toOnContainerColor
import com.osscabinet.gleemanager.ui.components.AccountIconMap
import kotlinx.coroutines.launch
import android.graphics.Color as AndroidColor

@OptIn(ExperimentalMaterial3Api::class)
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
    templateViewModel: com.osscabinet.gleemanager.ui.viewmodel.TemplateViewModel
) {
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()
    
    val accounts by accountViewModel.accounts.collectAsState()
    val nonArchivedAccounts = remember(accounts) { accounts.filter { !it.isArchived } }
    val transactions by transactionViewModel.transactions.collectAsState()
    val nonArchivedAccountIds = remember(nonArchivedAccounts) { nonArchivedAccounts.map { it.id }.toSet() }
    val activeTransactions = remember(transactions, nonArchivedAccountIds) {
        transactions.filter { it.accountId in nonArchivedAccountIds }
    }
    val currentProfile by settingsViewModel.currentProfile.collectAsState()
    val baseCurrency by settingsViewModel.currency.collectAsState()
    val exchangeRates by analyticsViewModel.exchangeRates.collectAsState()
    val currentTheme by settingsViewModel.theme.collectAsState(initial = "System")
    val starredTemplates by templateViewModel.starredTemplates.collectAsState()

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
                FloatingActionButton(
                    onClick = onAddAccountClick,
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary,
                    shape = CircleShape
                ) {
                    Icon(Icons.Default.Add, contentDescription = "Add Account")
                }
            }
        ) { innerPadding ->
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .background(MaterialTheme.colorScheme.background),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(24.dp)
            ) {
                item {
                    NetWorthSection(nonArchivedAccounts, activeTransactions, baseCurrency, exchangeRates)
                }
                
                item {
                    CurrencyAggregationSection(nonArchivedAccounts, activeTransactions, baseCurrency, exchangeRates)
                }
                
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Accounts", style = MaterialTheme.typography.titleMedium)
                        TextButton(onClick = onManageAccountsClick) {
                            Text("Manage", style = MaterialTheme.typography.labelLarge)
                        }
                    }
                }

                items(nonArchivedAccounts, key = { it.id }) { account ->
                    val accountTransactions = transactions.filter { it.accountId == account.id }
                    
                    val supportedCurrencies = remember(account, accountTransactions) {
                        val list = mutableSetOf<String>()
                        list.add(account.currencyCode)
                        list.addAll(account.allowedCurrencies)
                        accountTransactions.forEach { list.add(it.currency) }
                        list.toList().sorted()
                    }

                    val accountCurrencyBalances = supportedCurrencies.associateWith { curr ->
                        val txSum = accountTransactions.filter { it.currency == curr }.sumOf { if (it.isIncome) it.amount else -it.amount }
                        if (curr == account.currencyCode) account.initialBalance + txSum else txSum
                    }

                    val balanceInBase = accountCurrencyBalances.entries.sumOf { (curr, amount) ->
                        val rate = exchangeRates[curr] ?: 1.0
                        if (rate != 0.0) amount / rate else amount
                    }

                    val accountRate = exchangeRates[account.currencyCode] ?: 1.0
                    val incomeInAccountCurrency = accountTransactions.filter { it.isIncome }.sumOf { tx ->
                        val rate = exchangeRates[tx.currency] ?: 1.0
                        val inBase = if (rate != 0.0) tx.amount / rate else tx.amount
                        if (accountRate != 0.0) inBase * accountRate else inBase
                    }

                    val expenseInAccountCurrency = accountTransactions.filter { !it.isIncome }.sumOf { tx ->
                        val rate = exchangeRates[tx.currency] ?: 1.0
                        val inBase = if (rate != 0.0) tx.amount / rate else tx.amount
                        if (accountRate != 0.0) inBase * accountRate else inBase
                    }

                    val accountColor = try { Color(AndroidColor.parseColor(account.colorHex)) } catch (_: Exception) { MaterialTheme.colorScheme.primary }

                    val isDarkTheme = when (currentTheme) {
                        "Light" -> false
                        "Dark", "Amoled" -> true
                        else -> isSystemInDarkTheme()
                    }
                    val isAmoled = currentTheme == "Amoled"
                    
                    val containerColor = accountColor.toContainerColor(isDarkTheme, isAmoled)
                    val contentColor = accountColor.toOnContainerColor(isDarkTheme, isAmoled)

                    AccountCard(
                        title = account.name.uppercase(),
                        balance = formatCurrencyRight(balanceInBase, baseCurrency),
                        currencyBalances = accountCurrencyBalances,
                        incomeInAccountCurrency = incomeInAccountCurrency,
                        expenseInAccountCurrency = expenseInAccountCurrency,
                        accountCurrencyCode = account.currencyCode,
                        icon = AccountIconMap[account.icon] ?: Icons.Default.AccountBalance,
                        containerColor = containerColor,
                        contentColor = contentColor,
                        subAccounts = listOf(account.structureType),
                        border = isAmoled,
                        onClick = { onAccountClick(account.id) }
                    )
                }
                
                item {
                    Spacer(modifier = Modifier.height(64.dp)) // Space for FAB
                }
            }
        }
    }
}

@Composable
fun AccountCard(
    title: String,
    balance: String,
    currencyBalances: Map<String, Double> = emptyMap(),
    incomeInAccountCurrency: Double = 0.0,
    expenseInAccountCurrency: Double = 0.0,
    accountCurrencyCode: String = "",
    icon: ImageVector,
    containerColor: Color,
    contentColor: Color,
    subAccounts: List<String>,
    cardNumber: String? = null,
    extraLabel: String? = null,
    border: Boolean = false,
    onClick: () -> Unit = {}
) {
    var cardSize by remember { mutableStateOf(IntSize.Zero) }
    val density = LocalDensity.current

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .onSizeChanged { cardSize = it },
        colors = CardDefaults.cardColors(containerColor = containerColor),
        shape = RoundedCornerShape(32.dp),
        border = if (border) BorderStroke(1.dp, contentColor.copy(alpha = 0.3f)) else null
    ) {
        Box(modifier = Modifier.fillMaxWidth()) {
            // Background Icon
            val iconHeight = with(density) { (cardSize.height * 0.75f).toDp() }
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier
                    .align(Alignment.CenterEnd)
//                    .offset { IntOffset(x = (cardSize.width * 0.1f).toInt(), y = 0) }
                    .size(iconHeight)
                    .alpha(0.15f),
                tint = contentColor
            )

            Column(modifier = Modifier.padding(24.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(title, style = MaterialTheme.typography.labelSmall, color = contentColor.copy(alpha = 0.7f))
                    
                    // Income/Expense Badges
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        val incomeColor = Color(0xFF4CAF50)
                        val expenseColor = Color(0xFFF44336)
                        val badgeTextColor = contentColor

                        Surface(
                            color = incomeColor.copy(alpha = 0.4f),
                            shape = RoundedCornerShape(8.dp),
                            border = BorderStroke(1.dp, incomeColor.copy(alpha = 0.2f))
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Icon(Icons.Rounded.ArrowDownward, contentDescription = null, tint = incomeColor, modifier = Modifier.size(12.dp))
                                Text(
                                    com.osscabinet.gleemanager.ui.util.formatAmount(incomeInAccountCurrency),
                                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.ExtraBold),
                                    color = badgeTextColor
                                )
                            }
                        }

                        Surface(
                            color = expenseColor.copy(alpha = 0.4f),
                            shape = RoundedCornerShape(8.dp),
                            border = BorderStroke(1.dp, expenseColor.copy(alpha = 0.2f))
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Icon(Icons.Rounded.ArrowUpward, contentDescription = null, tint = expenseColor, modifier = Modifier.size(12.dp))
                                Text(
                                    com.osscabinet.gleemanager.ui.util.formatAmount(expenseInAccountCurrency),
                                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.ExtraBold),
                                    color = badgeTextColor
                                )
                            }
                        }
                    }
                }
                
                Spacer(modifier = Modifier.height(8.dp))
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        balance,
                        style = MaterialTheme.typography.displayMedium.copy(fontWeight = FontWeight.Bold),
                        color = contentColor
                    )
//                    Icon(icon, contentDescription = null, tint = contentColor.copy(alpha = 0.7f), modifier = Modifier.size(28.dp))
                }
                
                Spacer(modifier = Modifier.height(24.dp))
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Horizontal scrollable list for subAccounts and currency codes
                    Row(
                        modifier = Modifier.horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        subAccounts.forEach { sub ->
                            Surface(
                                color = contentColor.copy(alpha = 0.2f),
                                shape = RoundedCornerShape(16.dp)
                            ) {
                                Text(
                                    sub.uppercase(),
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp, fontWeight = FontWeight.Bold),
                                    color = contentColor
                                )
                            }
                        }
                        
                        currencyBalances.forEach { (curr, bal) ->
                            Surface(
                                color = contentColor.copy(alpha = 0.15f),
                                shape = RoundedCornerShape(16.dp)
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        curr,
                                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp, fontWeight = FontWeight.ExtraBold),
                                        color = contentColor.copy(alpha = 0.6f)
                                    )
                                    Text(
                                        com.osscabinet.gleemanager.ui.util.formatAmount(bal),
                                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp, fontWeight = FontWeight.Bold),
                                        color = contentColor
                                    )
                                }
                            }
                        }
                    }
                    
                    Spacer(modifier = Modifier.weight(1f))

                    if (cardNumber != null) {
                        Text(cardNumber, style = MaterialTheme.typography.labelSmall, color = contentColor.copy(alpha = 0.5f))
                    }
                    if (extraLabel != null) {
                        Text(extraLabel, style = MaterialTheme.typography.labelSmall, color = contentColor.copy(alpha = 0.5f))
                    }
                }
            }
        }
    }
}
