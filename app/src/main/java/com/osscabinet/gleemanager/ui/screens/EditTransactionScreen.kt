package com.osscabinet.gleemanager.ui.screens

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.Notes
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.osscabinet.gleemanager.domain.model.Account
import com.osscabinet.gleemanager.ui.components.*
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
    
    // Calculator State
    var firstOperand by remember { mutableStateOf<Double?>(null) }
    var operator by remember { mutableStateOf<String?>(null) }
    var isEnteringSecondOperand by remember { mutableStateOf(false) }

    fun calculateResult() {
        if (firstOperand != null && operator != null && amount.isNotEmpty()) {
            val secondOperand = amount.toDoubleOrNull() ?: 0.0
            val result = when (operator) {
                "+" -> firstOperand!! + secondOperand
                "−" -> firstOperand!! - secondOperand
                "×" -> firstOperand!! * secondOperand
                "÷" -> if (secondOperand != 0.0) firstOperand!! / secondOperand else 0.0
                else -> secondOperand
            }
            amount = if (result % 1 == 0.0) result.toLong().toString() else String.format(Locale.getDefault(), "%.2f", result)
            firstOperand = null
            operator = null
            isEnteringSecondOperand = false
        }
    }

    var category by remember { mutableStateOf(transaction.category) }
    var tagsInput by remember { mutableStateOf(transaction.tags.joinToString(", ")) }
    var transactionType by remember { mutableStateOf(if (transaction.isIncome) 1 else 0) }
    val isIncome = transactionType == 1
    var selectedDateMillis by remember { mutableStateOf(transaction.timestamp) }
    var payee by remember { mutableStateOf("") } // Assuming these might be added to the model later
    var note by remember { mutableStateOf("") }
    var isWarrantyEnabled by remember { mutableStateOf(false) }
    
    val accounts by accountViewModel.accounts.collectAsState()
    val exchangeRates by analyticsViewModel.exchangeRates.collectAsState()
    var selectedAccount by remember { mutableStateOf(accounts.find { it.id == transaction.accountId }) }
    var selectedCurrency by remember { mutableStateOf(transaction.currency) }
    
    var showDatePicker by remember { mutableStateOf(false) }
    var showCategorySheet by remember { mutableStateOf(false) }
    var showAccountSheet by remember { mutableStateOf(false) }
    var isDetailDrawerOpen by remember { mutableStateOf(false) }

    val datePickerState = rememberDatePickerState(initialSelectedDateMillis = selectedDateMillis)
    val formatter = SimpleDateFormat("MMMM dd, yyyy", Locale.getDefault())

    Box(modifier = Modifier.fillMaxSize()) {
        Scaffold(
            topBar = {
                CenterAlignedTopAppBar(
                    title = { Text("Edit Record", style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)) },
                    navigationIcon = {
                        IconButton(onClick = onBackClick) {
                            Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Back")
                        }
                    },
                    actions = {
                        IconButton(onClick = {
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
                    .padding(horizontal = 20.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Header Card
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(40.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                ) {
                    Column(
                        modifier = Modifier.padding(20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .clip(RoundedCornerShape(32.dp))
                                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                                .padding(4.dp),
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            val types = listOf(Icons.Rounded.RemoveCircleOutline, Icons.Rounded.AddCircleOutline)
                            types.forEachIndexed { index, icon ->
                                Box(
                                    modifier = Modifier
                                        .size(44.dp)
                                        .clip(CircleShape)
                                        .background(if (transactionType == index) MaterialTheme.colorScheme.primary else Color.Transparent)
                                        .clickable { transactionType = index },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        icon,
                                        contentDescription = null,
                                        tint = if (transactionType == index) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Text(
                                text = if (amount.isEmpty()) "0.00" else (if (isIncome) "+" else "-") + amount,
                                style = TextStyle(
                                    fontSize = 48.sp,
                                    fontWeight = FontWeight.Black,
                                    textAlign = TextAlign.Center,
                                    color = if (isIncome) Color(0xFF2E7D32) else MaterialTheme.colorScheme.error
                                )
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            CurrencySelector(
                                selectedCurrency = selectedCurrency,
                                onCurrencySelected = { selectedCurrency = it },
                                supportedCurrencies = selectedAccount?.allowedCurrencies ?: listOf("USD")
                            )
                        }
                    }
                }

                // Field Cards
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    EditorialInput(value = title, onValueChange = { title = it }, label = "RECORD TITLE", icon = Icons.Rounded.Edit)
                    
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        RecordFieldCard(
                            icon = Icons.Rounded.AccountBalance,
                            label = "LEDGER",
                            value = selectedAccount?.name ?: "Select",
                            onClick = { showAccountSheet = true },
                            modifier = Modifier.weight(1f),
                            iconContainerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f),
                            isSlim = true
                        )
                        RecordFieldCard(
                            icon = categoryIcons[category] ?: Icons.Rounded.Category,
                            label = "CATEGORY",
                            value = category,
                            onClick = { showCategorySheet = true },
                            modifier = Modifier.weight(1f),
                            iconContainerColor = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.5f),
                            isSlim = true
                        )
                    }
                }

                Spacer(modifier = Modifier.weight(1f))

                Numpad(
                    onNumberClick = { input ->
                        if (isEnteringSecondOperand) {
                            amount = if (input == ".") "0." else input
                            isEnteringSecondOperand = false
                        } else {
                            if (input == ".") {
                                if (!amount.contains(".")) {
                                    amount += if (amount.isEmpty()) "0." else "."
                                }
                            } else if (input == "00") {
                                if (amount.isNotEmpty() && amount != "0") amount += "00"
                            } else {
                                amount = if (amount == "0") input else amount + input
                            }
                        }
                    },
                    onDeleteClick = {
                        if (amount.isNotEmpty()) amount = amount.dropLast(1)
                    },
                    onOperatorClick = { op ->
                        if (amount.isNotEmpty()) {
                            if (operator != null) calculateResult()
                            firstOperand = amount.toDoubleOrNull()
                            operator = op
                            isEnteringSecondOperand = true
                        }
                    },
                    onEqualClick = { calculateResult() },
                    onClearClick = {
                        amount = ""
                        firstOperand = null
                        operator = null
                        isEnteringSecondOperand = false
                    },
                    modifier = Modifier.fillMaxWidth()
                )

                Button(
                    onClick = {
                        val amountDouble = amount.toDoubleOrNull() ?: 0.0
                        selectedAccount?.let { account ->
                            val oldAmountInAccountCurrency = if (transaction.currency == account.currencyCode) {
                                transaction.amount
                            } else {
                                val txRate = exchangeRates[transaction.currency] ?: 1.0
                                val accountRate = exchangeRates[account.currencyCode] ?: 1.0
                                if (txRate != 0.0) (transaction.amount / txRate) * accountRate else transaction.amount
                            }
                            accountViewModel.updateBalance(transaction.accountId, if (transaction.isIncome) -oldAmountInAccountCurrency else oldAmountInAccountCurrency)

                            val newAmountInAccountCurrency = if (selectedCurrency == account.currencyCode) {
                                amountDouble
                            } else {
                                val txRate = exchangeRates[selectedCurrency] ?: 1.0
                                val accountRate = exchangeRates[account.currencyCode] ?: 1.0
                                if (txRate != 0.0) (amountDouble / txRate) * accountRate else amountDouble
                            }
                            accountViewModel.updateBalance(account.id, if (isIncome) newAmountInAccountCurrency else -newAmountInAccountCurrency)

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
                    Text("SAVE CHANGES", fontWeight = FontWeight.Bold)
                }
            }
        }

        DetailDrawer(
            isOpen = isDetailDrawerOpen,
            onClose = { isDetailDrawerOpen = false },
            onSwipeOpen = { isDetailDrawerOpen = true }
        ) {
            Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(28.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f)
                ) {
                    Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Rounded.Person, contentDescription = null, modifier = Modifier.size(20.dp), tint = MaterialTheme.colorScheme.outline)
                            Spacer(modifier = Modifier.width(12.dp))
                            BasicTextField(
                                value = payee,
                                onValueChange = { payee = it },
                                modifier = Modifier.fillMaxWidth(),
                                textStyle = MaterialTheme.typography.bodyLarge.copy(color = MaterialTheme.colorScheme.onSurface),
                                decorationBox = { if (payee.isEmpty()) Text("Add Payee", color = MaterialTheme.colorScheme.outline); it() }
                            )
                        }
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.AutoMirrored.Rounded.Notes, contentDescription = null, modifier = Modifier.size(20.dp), tint = MaterialTheme.colorScheme.outline)
                            Spacer(modifier = Modifier.width(12.dp))
                            BasicTextField(
                                value = note,
                                onValueChange = { note = it },
                                modifier = Modifier.fillMaxWidth(),
                                textStyle = MaterialTheme.typography.bodyLarge.copy(color = MaterialTheme.colorScheme.onSurface),
                                decorationBox = { if (note.isEmpty()) Text("Add Note", color = MaterialTheme.colorScheme.outline); it() }
                            )
                        }
                    }
                }

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Surface(
                        modifier = Modifier.weight(1f).height(80.dp),
                        shape = RoundedCornerShape(28.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f),
                        onClick = { showDatePicker = true }
                    ) {
                        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.Center) {
                            Text("OCCURRENCE", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.outline)
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Rounded.CalendarToday, contentDescription = null, modifier = Modifier.size(16.dp), tint = Color(0xFF42A5F5))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(formatter.format(Date(selectedDateMillis)), style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }

                Surface(
                    modifier = Modifier.fillMaxWidth().height(160.dp),
                    shape = RoundedCornerShape(28.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.1f)
                ) {
                    Box {
                        Image(
                            painter = painterResource(id = android.R.drawable.ic_menu_mapmode),
                            contentDescription = null,
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop,
                            alpha = 0.3f
                        )
                        Surface(
                            modifier = Modifier.align(Alignment.Center),
                            shape = RoundedCornerShape(20.dp),
                            color = MaterialTheme.colorScheme.surface,
                            shadowElevation = 4.dp
                        ) {
                            Row(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Rounded.Place, contentDescription = null, modifier = Modifier.size(18.dp), tint = MaterialTheme.colorScheme.primary)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Select Location", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }
    }

    if (showAccountSheet) {
        AccountPickerSheet(
            accounts = accounts,
            selectedAccount = selectedAccount,
            onAccountSelected = { selectedAccount = it; selectedCurrency = it.currencyCode; showAccountSheet = false },
            onDismiss = { showAccountSheet = false }
        )
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
        CategoryPickerSheet(
            selectedCategory = category,
            onCategorySelected = {
                category = it
                showCategorySheet = false
            },
            onDismiss = { showCategorySheet = false }
        )
    }
}
