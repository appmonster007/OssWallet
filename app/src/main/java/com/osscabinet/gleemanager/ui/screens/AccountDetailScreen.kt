package com.osscabinet.gleemanager.ui.screens

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
    onBackClick: () -> Unit,
    onEditAccountClick: (String) -> Unit
) {
    val accounts by accountViewModel.accounts.collectAsState()
    val account = accounts.find { it.id == accountId }
    
    val transactions by transactionViewModel.transactions.collectAsState()
    val accountTransactions = transactions.filter { it.accountId == accountId }
    
    val income = accountTransactions.filter { it.isIncome }.sumOf { it.amount }
    val expense = accountTransactions.filter { !it.isIncome }.sumOf { it.amount }
    val net = income - expense

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
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .background(MaterialTheme.colorScheme.background)
            ) {
                // Hero Account Card
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    shape = RoundedCornerShape(32.dp),
                    color = accountColor,
                    contentColor = if (accountColor.isDark()) Color.White else Color.Black
                ) {
                    Column(modifier = Modifier.padding(24.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column {
                                Text(account.structureType.uppercase(), style = MaterialTheme.typography.labelSmall, color = LocalContentColor.current.copy(alpha = 0.7f))
                                Text(
                                    "${getCurrencySymbol(account.currencyCode)}${String.format("%,.2f", account.balance)}",
                                    style = MaterialTheme.typography.displaySmall.copy(fontWeight = FontWeight.Bold)
                                )
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
                                Text("MONTHLY NET", style = MaterialTheme.typography.labelSmall, color = LocalContentColor.current.copy(alpha = 0.7f))
                                Text(
                                    "${if (net >= 0) "+" else ""}${getCurrencySymbol(account.currencyCode)}${String.format("%,.2f", net)}",
                                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                                )
                            }
                            Text(account.id.takeLast(4).let { "**** $it" }, style = MaterialTheme.typography.labelSmall, color = LocalContentColor.current.copy(alpha = 0.5f))
                        }
                    }
                }

                // Stats Row
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    StatCard(
                        Modifier.weight(1f),
                        "INCOME",
                        "${getCurrencySymbol(account.currencyCode)}${String.format("%,.0f", income)}",
                        Color(0xFF2E7D32)
                    )
                    StatCard(
                        Modifier.weight(1f),
                        "EXPENSE",
                        "${getCurrencySymbol(account.currencyCode)}${String.format("%,.0f", expense)}",
                        Color(0xFFC62828)
                    )
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Transaction History
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

                if (accountTransactions.isEmpty()) {
                    Box(modifier = Modifier.fillMaxSize().padding(bottom = 64.dp), contentAlignment = Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(Icons.Rounded.History, contentDescription = null, modifier = Modifier.size(64.dp), tint = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))
                            Spacer(modifier = Modifier.height(16.dp))
                            Text("No transactions yet", style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.outline)
                        }
                    }
                } else {
                    // Group by currency to show separate balances
                    val currencyBalances = accountTransactions.groupBy { it.currency }.mapValues { (curr, txs) ->
                        val txSum = txs.sumOf { if (it.isIncome) it.amount else -it.amount }
                        if (curr == account.currencyCode) account.balance + txSum else txSum
                    }
                    
                    val finalBalances = if (!currencyBalances.containsKey(account.currencyCode)) {
                        currencyBalances + (account.currencyCode to account.balance)
                    } else {
                        currencyBalances
                    }

                    LazyColumn(modifier = Modifier.fillMaxSize()) {
                        item {
                            Column(modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp)) {
                                Text("CURRENCY BREAKDOWN", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.outline)
                                Spacer(modifier = Modifier.height(8.dp))
                                finalBalances.forEach { (curr, bal) ->
                                    Row(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                                        Text(curr, style = MaterialTheme.typography.bodyMedium)
                                        Text("${getCurrencySymbol(curr)}${String.format("%,.2f", bal)}", style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold))
                                    }
                                }
                                Spacer(modifier = Modifier.height(16.dp))
                            }
                        }
                        items(accountTransactions.sortedByDescending { it.timestamp }) { transaction ->
                            TransactionItem(
                                transaction = transaction,
                                currencySymbol = getCurrencySymbol(transaction.currency),
                                onClick = { /* View transaction detail */ }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun StatCard(modifier: Modifier, label: String, amount: String, color: Color) {
    Surface(
        modifier = modifier.height(80.dp),
        shape = RoundedCornerShape(24.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.Center
        ) {
            Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.outline)
            Text(amount, style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold, color = color))
        }
    }
}

@Composable
fun TransactionItem(
    transaction: Transaction,
    currencySymbol: String,
    onClick: () -> Unit
) {
    val dateFormat = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())
    
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp)
            .clickable { onClick() },
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f)
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
                Text(transaction.title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                Text(transaction.category, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.outline)
            }
            
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    "${if (transaction.isIncome) "+" else "-"}$currencySymbol${String.format("%,.2f", transaction.amount)}",
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                    color = if (transaction.isIncome) Color(0xFF2E7D32) else MaterialTheme.colorScheme.onSurface
                )
                Text(
                    dateFormat.format(Date(transaction.timestamp)),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.outline
                )
            }
        }
    }
}
