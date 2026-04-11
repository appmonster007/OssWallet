package com.osscabinet.gleemanager.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.Label
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.osscabinet.gleemanager.domain.model.Account
import com.osscabinet.gleemanager.ui.components.CalculatorDialog
import com.osscabinet.gleemanager.ui.util.formatCurrency
import com.osscabinet.gleemanager.ui.util.getCurrencySymbol
import com.osscabinet.gleemanager.ui.viewmodel.AccountViewModel
import com.osscabinet.gleemanager.ui.viewmodel.AnalyticsViewModel
import com.osscabinet.gleemanager.ui.viewmodel.SettingsViewModel
import com.osscabinet.gleemanager.ui.viewmodel.TransactionViewModel
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditTransactionScreen(
    transactionId: Long,
    viewModel: TransactionViewModel,
    accountViewModel: AccountViewModel,
    analyticsViewModel: AnalyticsViewModel,
    settingsViewModel: SettingsViewModel,
    onBackClick: () -> Unit
) {
    val transactions by viewModel.transactions.collectAsState()
    val transaction = transactions.find { it.id == transactionId }

    if (transaction == null) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
        return
    }

    var title by remember { mutableStateOf(transaction.title) }
    var amount by remember { mutableStateOf(transaction.amount.toString()) }
    var category by remember { mutableStateOf(transaction.category) }
    var tagsInput by remember { mutableStateOf(transaction.tags.joinToString(", ")) }
    var isIncome by remember { mutableStateOf(transaction.isIncome) }
    var selectedDateMillis by remember { mutableStateOf(transaction.timestamp) }
    
    val accounts by accountViewModel.accounts.collectAsState()
    val exchangeRates by analyticsViewModel.exchangeRates.collectAsState()
    var selectedAccount by remember { mutableStateOf(accounts.find { it.id == transaction.accountId }) }
    var selectedCurrency by remember { mutableStateOf(transaction.currency) }
    
    var showCalculator by remember { mutableStateOf(false) }
    var showDatePicker by remember { mutableStateOf(false) }
    var showCategorySheet by remember { mutableStateOf(false) }
    var showAccountSheet by remember { mutableStateOf(false) }

    val datePickerState = rememberDatePickerState(initialSelectedDateMillis = selectedDateMillis)
    val formatter = SimpleDateFormat("MMMM dd, yyyy", Locale.getDefault())

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("EDIT RECORD", fontWeight = FontWeight.Bold, letterSpacing = 1.sp) },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = {
                        // Reverse impact in account currency
                        selectedAccount?.let { account ->
                            val oldAmountInAccountCurrency = if (transaction.currency == account.currencyCode) {
                                transaction.amount
                            } else {
                                val txRate = exchangeRates[transaction.currency] ?: 1.0
                                val accountRate = exchangeRates[account.currencyCode] ?: 1.0
                                if (txRate != 0.0) (transaction.amount / txRate) * accountRate else transaction.amount
                            }
                            
                            accountViewModel.updateBalance(
                                account.id,
                                if (transaction.isIncome) -oldAmountInAccountCurrency else oldAmountInAccountCurrency
                            )
                        }
                        viewModel.deleteTransaction(transactionId)
                        onBackClick()
                    }) {
                        Icon(Icons.Rounded.Delete, contentDescription = "Delete", tint = MaterialTheme.colorScheme.error)
                    }
                }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(32.dp)
        ) {
            // Amount Input (Same as AddTransactionScreen)
            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                Text("TRANSACTION AMOUNT", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.outline)
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 16.dp)) {
                    Text(getCurrencySymbol(selectedCurrency), style = MaterialTheme.typography.displayMedium.copy(color = MaterialTheme.colorScheme.primary))
                    TextField(
                        value = amount,
                        onValueChange = { if (it.isEmpty() || it.toDoubleOrNull() != null) amount = it },
                        textStyle = MaterialTheme.typography.displayMedium.copy(fontWeight = FontWeight.Bold),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        colors = TextFieldDefaults.colors(focusedContainerColor = Color.Transparent, unfocusedContainerColor = Color.Transparent, focusedIndicatorColor = Color.Transparent, unfocusedIndicatorColor = Color.Transparent),
                        modifier = Modifier.width(IntrinsicSize.Min)
                    )
                }
            }

            // Rest of the fields (Title, Account, Category, Date, Tags)
            Column(verticalArrangement = Arrangement.spacedBy(24.dp)) {
                EditorialInput(value = title, onValueChange = { title = it }, label = "RECORD TITLE", icon = Icons.Rounded.Edit)
                
                EditorialInput(
                    value = selectedAccount?.name ?: "Select Ledger",
                    onValueChange = { },
                    label = "SOURCE ACCOUNT",
                    icon = Icons.Rounded.AccountBalanceWallet,
                    readOnly = true,
                    onClick = { showAccountSheet = true }
                )

                EditorialInput(
                    value = category,
                    onValueChange = { },
                    label = "CLASSIFICATION",
                    icon = categoryIcons[category] ?: Icons.Rounded.Category,
                    readOnly = true,
                    onClick = { showCategorySheet = true }
                )

                EditorialInput(
                    value = formatter.format(Date(selectedDateMillis)),
                    onValueChange = { },
                    label = "ENTRY DATE",
                    icon = Icons.Rounded.CalendarToday,
                    readOnly = true,
                    onClick = { showDatePicker = true }
                )
            }

            Button(
                onClick = {
                    val amountDouble = amount.toDoubleOrNull() ?: 0.0
                    selectedAccount?.let { account ->
                        // 1. Reverse old impact
                        val oldAmountInAccountCurrency = if (transaction.currency == account.currencyCode) {
                            transaction.amount
                        } else {
                            val txRate = exchangeRates[transaction.currency] ?: 1.0
                            val accountRate = exchangeRates[account.currencyCode] ?: 1.0
                            if (txRate != 0.0) (transaction.amount / txRate) * accountRate else transaction.amount
                        }
                        accountViewModel.updateBalance(
                            transaction.accountId,
                            if (transaction.isIncome) -oldAmountInAccountCurrency else oldAmountInAccountCurrency
                        )

                        // 2. Apply new impact
                        val newAmountInAccountCurrency = if (selectedCurrency == account.currencyCode) {
                            amountDouble
                        } else {
                            val txRate = exchangeRates[selectedCurrency] ?: 1.0
                            val accountRate = exchangeRates[account.currencyCode] ?: 1.0
                            if (txRate != 0.0) (amountDouble / txRate) * accountRate else amountDouble
                        }
                        accountViewModel.updateBalance(
                            account.id,
                            if (isIncome) newAmountInAccountCurrency else -newAmountInAccountCurrency
                        )

                        viewModel.updateTransaction(
                            oldTransaction = transaction,
                            title = title,
                            amount = amountDouble,
                            category = category,
                            isIncome = isIncome,
                            accountId = account.id,
                            currency = selectedCurrency,
                            tags = tagsInput.split(",").map { it.trim() }.filter { it.isNotBlank() },
                            timestamp = selectedDateMillis
                        )
                        onBackClick()
                    }
                },
                modifier = Modifier.fillMaxWidth().height(64.dp),
                shape = RoundedCornerShape(32.dp)
            ) {
                Text("SAVE RECORD", fontWeight = FontWeight.Bold)
            }
        }
    }

    // Reuse Sheets from AddTransactionScreen (Simplified for brevity or extracted to common components)
    if (showAccountSheet) {
        ModalBottomSheet(onDismissRequest = { showAccountSheet = false }) {
            Column(modifier = Modifier.padding(24.dp).padding(bottom = 32.dp)) {
                Text("SELECT LEDGER", fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(24.dp))
                LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    items(accounts) { account ->
                        Surface(
                            modifier = Modifier.fillMaxWidth().clickable {
                                selectedAccount = account
                                selectedCurrency = account.currencyCode
                                showAccountSheet = false
                            },
                            shape = RoundedCornerShape(24.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f)
                        ) {
                            Row(modifier = Modifier.padding(20.dp), verticalAlignment = Alignment.CenterVertically) {
                                Box(modifier = Modifier.size(16.dp).clip(CircleShape).background(Color(android.graphics.Color.parseColor(account.colorHex))))
                                Spacer(modifier = Modifier.width(16.dp))
                                Text(account.name, fontWeight = FontWeight.Medium)
                                Spacer(modifier = Modifier.weight(1f))
                                Text(
                                    formatCurrency(account.balance, account.currencyCode),
                                    style = MaterialTheme.typography.bodyMedium.copy(fontFeatureSettings = "tnum"),
                                    color = MaterialTheme.colorScheme.outline
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    if (showDatePicker) {
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    selectedDateMillis = datePickerState.selectedDateMillis ?: selectedDateMillis
                    showDatePicker = false
                }) { Text("CONFIRM") }
            }
        ) { DatePicker(state = datePickerState) }
    }

    if (showCategorySheet) {
        ModalBottomSheet(onDismissRequest = { showCategorySheet = false }) {
            Column(modifier = Modifier.padding(24.dp).padding(bottom = 32.dp)) {
                Text("CLASSIFICATION", fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(32.dp))
                LazyVerticalGrid(columns = GridCells.Fixed(4), verticalArrangement = Arrangement.spacedBy(24.dp), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    items(categoryIcons.toList()) { (name, icon) ->
                        CategoryItem(name = name, icon = icon, isSelected = category == name, onClick = { category = name; showCategorySheet = false })
                    }
                }
            }
        }
    }
}
