package com.osscabinet.gleemanager.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.osscabinet.gleemanager.domain.model.Account
import com.osscabinet.gleemanager.domain.model.Transaction
import com.osscabinet.gleemanager.ui.viewmodel.AccountViewModel
import com.osscabinet.gleemanager.ui.viewmodel.TransactionViewModel
import com.osscabinet.gleemanager.ui.util.formatCurrency
import com.osscabinet.gleemanager.ui.util.getCurrencySymbol
import com.osscabinet.gleemanager.ui.util.isDark
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AccountDetailScreen(
    accountId: String,
    accountViewModel: AccountViewModel,
    transactionViewModel: TransactionViewModel,
    settingsViewModel: com.osscabinet.gleemanager.ui.viewmodel.SettingsViewModel,
    analyticsViewModel: com.osscabinet.gleemanager.ui.viewmodel.AnalyticsViewModel,
    onBackClick: () -> Unit,
    onEditAccountClick: (String) -> Unit,
    onEditTransactionClick: (Long) -> Unit
) {
    val accounts by accountViewModel.accounts.collectAsState()
    val account = accounts.find { it.id == accountId }
    
    val transactions by transactionViewModel.transactions.collectAsState()
    val accountTransactions = transactions.filter { it.accountId == accountId }
    
    val baseCurrency by settingsViewModel.currency.collectAsState()
    val exchangeRates by analyticsViewModel.exchangeRates.collectAsState()

    val totalBalanceInAccountCurrency by remember(account, accountTransactions, exchangeRates, baseCurrency) {
        derivedStateOf {
            val currentAccount = account ?: return@derivedStateOf 0.0
            val initialBalance = currentAccount.initialBalance
            
            val convertedTxSum = accountTransactions.sumOf { tx ->
                val amount = if (tx.isIncome) tx.amount else -tx.amount
                val rateFrom = if (tx.currency == baseCurrency) 1.0 else (exchangeRates[tx.currency] ?: 1.0)
                val rateTo = if (currentAccount.currencyCode == baseCurrency) 1.0 else (exchangeRates[currentAccount.currencyCode] ?: 1.0)
                if (rateFrom != 0.0) (amount / rateFrom) * rateTo else amount
            }
            initialBalance + convertedTxSum
        }
    }

    val incomeInAccountCurrency by remember(account, accountTransactions, exchangeRates, baseCurrency) {
        derivedStateOf {
            val currentAccount = account ?: return@derivedStateOf 0.0
            accountTransactions.filter { it.isIncome }.sumOf { tx ->
                val rateFrom = if (tx.currency == baseCurrency) 1.0 else (exchangeRates[tx.currency] ?: 1.0)
                val rateTo = if (currentAccount.currencyCode == baseCurrency) 1.0 else (exchangeRates[currentAccount.currencyCode] ?: 1.0)
                if (rateFrom != 0.0) (tx.amount / rateFrom) * rateTo else tx.amount
            }
        }
    }

    val expenseInAccountCurrency by remember(account, accountTransactions, exchangeRates, baseCurrency) {
        derivedStateOf {
            val currentAccount = account ?: return@derivedStateOf 0.0
            accountTransactions.filter { !it.isIncome }.sumOf { tx ->
                val rateFrom = if (tx.currency == baseCurrency) 1.0 else (exchangeRates[tx.currency] ?: 1.0)
                val rateTo = if (currentAccount.currencyCode == baseCurrency) 1.0 else (exchangeRates[currentAccount.currencyCode] ?: 1.0)
                if (rateFrom != 0.0) (tx.amount / rateFrom) * rateTo else tx.amount
            }
        }
    }

    val netInAccountCurrency = incomeInAccountCurrency - expenseInAccountCurrency

    val supportedCurrencies = remember(account, accountTransactions) {
        val list = mutableSetOf<String>()
        account?.let {
            list.add(it.currencyCode)
            list.addAll(it.allowedCurrencies)
        }
        // Also add any currencies present in transactions even if not in "allowed"
        accountTransactions.forEach { list.add(it.currency) }
        list.toList().sorted()
    }

    val incomeBreakdown by remember(supportedCurrencies, accountTransactions, exchangeRates, baseCurrency) {
        derivedStateOf {
            val txsByCurrency = accountTransactions.filter { it.isIncome }.groupBy { it.currency }
            supportedCurrencies.map { curr ->
                val amount = txsByCurrency[curr]?.sumOf { it.amount } ?: 0.0
                val rate = if (curr == baseCurrency) 1.0 else (exchangeRates[curr] ?: 1.0)
                val inBase = if (rate != 0.0) amount / rate else amount
                Triple(curr, amount, inBase)
            }
        }
    }

    val expenseBreakdown by remember(supportedCurrencies, accountTransactions, exchangeRates, baseCurrency) {
        derivedStateOf {
            val txsByCurrency = accountTransactions.filter { !it.isIncome }.groupBy { it.currency }
            supportedCurrencies.map { curr ->
                val amount = txsByCurrency[curr]?.sumOf { it.amount } ?: 0.0
                val rate = if (curr == baseCurrency) 1.0 else (exchangeRates[curr] ?: 1.0)
                val inBase = if (rate != 0.0) amount / rate else amount
                Triple(curr, amount, inBase)
            }
        }
    }

    LaunchedEffect(baseCurrency) {
        analyticsViewModel.onCurrencySelected(baseCurrency)
    }

    val accountColor = try {
        Color(android.graphics.Color.parseColor(account?.colorHex ?: "#004D5F"))
    } catch (e: Exception) {
        MaterialTheme.colorScheme.primary
    }

    val iconMap = mapOf(
        "AccountBalance" to Icons.Rounded.AccountBalance,
        "Wallet" to Icons.Rounded.Wallet,
        "RocketLaunch" to Icons.Rounded.RocketLaunch,
        "ShowChart" to Icons.Rounded.ShowChart,
        "Payments" to Icons.Rounded.Payments,
        "Savings" to Icons.Rounded.Savings,
        "Paid" to Icons.Rounded.Paid,
        "AccountBalanceWallet" to Icons.Rounded.AccountBalanceWallet,
        "CreditCard" to Icons.Rounded.CreditCard,
        "MonetizationOn" to Icons.Rounded.MonetizationOn,
        "Analytics" to Icons.Rounded.Analytics,
        "Foundation" to Icons.Rounded.Foundation,
        "Stars" to Icons.Rounded.Stars,
        "Diamond" to Icons.Rounded.Diamond,
        "AccountTree" to Icons.Rounded.AccountTree,
        "AutoGraph" to Icons.Rounded.AutoGraph
    )

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(account?.name ?: "Account", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)) },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = { onEditAccountClick(accountId) }) { 
                        Icon(Icons.Rounded.Edit, contentDescription = "Edit") 
                    }
                    IconButton(onClick = { /* More actions */ }) { 
                        Icon(Icons.Rounded.MoreVert, contentDescription = "More") 
                    }
                }
            )
        }
    ) { innerPadding ->
        if (account == null) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .background(MaterialTheme.colorScheme.background)
            ) {
                item {
                    // Hero Account Card
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        shape = RoundedCornerShape(32.dp),
                        color = accountColor,
                        contentColor = if (accountColor.isDark()) Color.White else Color.Black,
                        shadowElevation = 0.dp // No shadow for No-Line philosophy
                    ) {
                        Column(modifier = Modifier.padding(24.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column {
                                    Text(
                                        account.structureType.uppercase(),
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            fontWeight = FontWeight.Black,
                                            letterSpacing = 1.5.sp
                                        ),
                                        color = LocalContentColor.current.copy(alpha = 0.7f)
                                    )
                                    Text(
                                        formatCurrency(totalBalanceInAccountCurrency, account.currencyCode),
                                        style = MaterialTheme.typography.displaySmall.copy(fontWeight = FontWeight.Black)
                                    )
                                    if (account.currencyCode != baseCurrency) {
                                        val rate = exchangeRates[account.currencyCode] ?: 1.0
                                        val convertedBalance = if (rate != 0.0) totalBalanceInAccountCurrency / rate else 0.0
                                        Text(
                                            "≈ ${formatCurrency(convertedBalance, baseCurrency)}",
                                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                            color = LocalContentColor.current.copy(alpha = 0.7f)
                                        )
                                    }
                                }
                                Icon(
                                    iconMap[account.icon] ?: Icons.Rounded.AccountBalance,
                                    contentDescription = null,
                                    modifier = Modifier.size(32.dp),
                                    tint = LocalContentColor.current.copy(alpha = 0.7f)
                                )
                            }
                            Spacer(modifier = Modifier.height(24.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.Bottom
                              ) {
                                Column {
                                    Text(
                                        "MONTHLY NET",
                                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                        color = LocalContentColor.current.copy(alpha = 0.7f)
                                    )
                                    Text(
                                        formatCurrency(netInAccountCurrency, account.currencyCode),
                                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Black)
                                    )
                                }
                                Text(
                                    account.id.takeLast(4).let { "**** $it" },
                                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                    color = LocalContentColor.current.copy(alpha = 0.5f)
                                )
                            }
                        }
                    }
                }

                item {
                    // Flow Breakdown
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        // Income Section
                        Surface(
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(24.dp),
                            color = Color(0xFF2E7D32).copy(alpha = 0.12f)
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Rounded.ArrowDownward, contentDescription = null, tint = Color(0xFF2E7D32), modifier = Modifier.size(14.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("INCOME", style = MaterialTheme.typography.labelSmall, color = Color(0xFF2E7D32), fontWeight = FontWeight.Black)
                                }
                                Spacer(modifier = Modifier.height(12.dp))
                                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                    incomeBreakdown.forEach { (curr, amount, inBase) ->
                                        FlowBreakdownItem(curr, amount, inBase, baseCurrency)
                                    }
                                }
                            }
                        }

                        // Expense Section
                        Surface(
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(24.dp),
                            color = Color(0xFFC62828).copy(alpha = 0.12f)
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Rounded.ArrowUpward, contentDescription = null, tint = Color(0xFFC62828), modifier = Modifier.size(14.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("EXPENSE", style = MaterialTheme.typography.labelSmall, color = Color(0xFFC62828), fontWeight = FontWeight.Black)
                                }
                                Spacer(modifier = Modifier.height(12.dp))
                                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                    expenseBreakdown.forEach { (curr, amount, inBase) ->
                                        FlowBreakdownItem(curr, amount, inBase, baseCurrency)
                                    }
                                }
                            }
                        }
                    }
                }

                item {
                    Spacer(modifier = Modifier.height(24.dp))

                    // Transaction History Header
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            "TRANSACTION HISTORY",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.outline,
                            letterSpacing = 1.sp
                        )
                        Text(
                            "${accountTransactions.size} total",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.outline
                        )
                    }
                }

                if (accountTransactions.isEmpty()) {
                    item {
                        Box(modifier = Modifier.fillMaxWidth().padding(24.dp), contentAlignment = Alignment.Center) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(Icons.Rounded.History, contentDescription = null, modifier = Modifier.size(48.dp), tint = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))
                                Spacer(modifier = Modifier.height(8.dp))
                                Text("No transactions yet", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.outline)
                            }
                        }
                    }
                }

                item {
                    // Group by currency to show separate balances
                    val finalBalances = remember(account, accountTransactions, supportedCurrencies) {
                        val currentAccount = account ?: return@remember emptyMap<String, Double>()
                        val initialBalance = currentAccount.initialBalance
                        
                        val txsByCurrency = accountTransactions.groupBy { it.currency }
                        supportedCurrencies.associateWith { curr ->
                            val txSum = txsByCurrency[curr]?.sumOf { if (it.isIncome) it.amount else -it.amount } ?: 0.0
                            if (curr == currentAccount.currencyCode) initialBalance + txSum else txSum
                        }
                    }

                    Column(modifier = Modifier.padding(16.dp)) {
                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(28.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                        ) {
                            Column(modifier = Modifier.padding(20.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        "CURRENCY BREAKDOWN",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.outline,
                                        letterSpacing = 0.5.sp
                                    )
//                                    Icon(
//                                        Icons.Rounded.AccountBalanceWallet,
//                                        contentDescription = null,
//                                        tint = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f),
//                                        modifier = Modifier.size(16.dp)
//                                    )
                                }
                                Spacer(modifier = Modifier.height(16.dp))
                                finalBalances.forEach { (curr, bal) ->
                                    Column(modifier = Modifier.padding(vertical = 8.dp)) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
//                                                Surface(
//                                                    modifier = Modifier.size(40.dp),
//                                                    shape = CircleShape,
//                                                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
//                                                ) {
//                                                    Box(contentAlignment = Alignment.Center) {
//                                                        Text(
//                                                            getCurrencySymbol(curr),
//                                                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Black)
//                                                        )
//                                                    }
//                                                }
//                                                Spacer(modifier = Modifier.width(16.dp))
                                                Text(
                                                    curr,
                                                    style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold)
                                                )
                                            }
                                            Column(horizontalAlignment = Alignment.End) {
                                                Text(
                                                    formatCurrency(bal, curr),
                                                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Black)
                                                )
                                                if (curr != baseCurrency) {
                                                    val rate = exchangeRates[curr] ?: 1.0
                                                    val convertedBal = if (rate != 0.0) bal / rate else bal
                                                    Text(
                                                        "≈ ${formatCurrency(convertedBal, baseCurrency)}",
                                                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                                        color = MaterialTheme.colorScheme.outline
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                        Spacer(modifier = Modifier.height(16.dp))
                    }
                }

                items(accountTransactions.sortedByDescending { it.timestamp }, key = { it.id }) { transaction ->
                    TransactionItem(
                        transaction = transaction,
                        onClick = { onEditTransactionClick(transaction.id) }
                    )
                }
            }
        }
    }
}

@Composable
fun FlowBreakdownItem(curr: String, amount: Double, inBase: Double, baseCurrency: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column {
            Text(
                curr,
                style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp, fontWeight = FontWeight.Black),
                color = MaterialTheme.colorScheme.outline
            )
            Text(
                formatCurrency(amount, curr),
                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Black)
            )
        }
        if (curr != baseCurrency) {
            Text(
                "≈ ${formatCurrency(inBase, baseCurrency)}",
                style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp, fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.outline
            )
        }
    }
}

@Composable
fun TransactionItem(
    transaction: Transaction,
    onClick: () -> Unit
) {
    val dateFormat = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())
    
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp),
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f),
        onClick = onClick // Using the built-in onClick which handles ripple
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                modifier = Modifier.size(40.dp),
                shape = CircleShape,
                color = if (transaction.isIncome) Color(0xFF2E7D32).copy(alpha = 0.1f) else Color(0xFFC62828).copy(alpha = 0.1f)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        if (transaction.isIncome) Icons.Rounded.ArrowDownward else Icons.Rounded.ArrowUpward,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp),
                        tint = if (transaction.isIncome) Color(0xFF2E7D32) else Color(0xFFC62828)
                    )
                }
            }
            
            Spacer(modifier = Modifier.width(16.dp))
            
            Column(modifier = Modifier.weight(1f)) {
                Text(transaction.title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Black)
                Text(transaction.category, style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold), color = MaterialTheme.colorScheme.outline)
            }
            
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    formatCurrency(if (transaction.isIncome) transaction.amount else -transaction.amount, transaction.currency),
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Black),
                    color = if (transaction.isIncome) Color(0xFF2E7D32) else MaterialTheme.colorScheme.onSurface
                )
                Text(
                    dateFormat.format(Date(transaction.timestamp)),
                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.outline
                )
            }
        }
    }
}
