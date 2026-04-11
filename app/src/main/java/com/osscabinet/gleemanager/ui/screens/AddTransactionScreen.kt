package com.osscabinet.gleemanager.ui.screens

import androidx.compose.animation.*
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.*
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowForward
import androidx.compose.material.icons.automirrored.rounded.Backspace
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.rounded.Notes
import androidx.compose.material.icons.automirrored.rounded.TrendingUp
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.osscabinet.gleemanager.domain.model.Account
import com.osscabinet.gleemanager.ui.components.*
import com.osscabinet.gleemanager.ui.util.formatCurrency
import com.osscabinet.gleemanager.ui.viewmodel.AccountViewModel
import com.osscabinet.gleemanager.ui.viewmodel.AnalyticsViewModel
import com.osscabinet.gleemanager.ui.viewmodel.TransactionViewModel
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddTransactionScreen(
    viewModel: TransactionViewModel,
    accountViewModel: AccountViewModel,
    analyticsViewModel: AnalyticsViewModel,
    onBackClick: () -> Unit
) {
    var title by remember { mutableStateOf("") }
    var amount by remember { mutableStateOf("") }
    var category by remember { mutableStateOf("Shopping") }
    var tagsInput by remember { mutableStateOf("") }
    var transactionType by remember { mutableStateOf(0) } // 0: Expense, 1: Income, 2: Transfer, 3: Refund
    val isIncome = transactionType == 1
    val isTransfer = transactionType == 2
    val isRefund = transactionType == 3
    var selectedDateMillis by remember { mutableStateOf(System.currentTimeMillis()) }
    var payee by remember { mutableStateOf("") }
    var note by remember { mutableStateOf("") }
    var isWarrantyEnabled by remember { mutableStateOf(false) }
    
    val accounts by accountViewModel.accounts.collectAsState()
    val exchangeRates by analyticsViewModel.exchangeRates.collectAsState()
    var selectedAccount by remember { mutableStateOf<Account?>(null) }
    var selectedCurrency by remember { mutableStateOf("") }
    
    var targetAccount by remember { mutableStateOf<Account?>(null) }
    var targetCurrency by remember { mutableStateOf("") }
    var exchangeRate by remember { mutableStateOf(1.0) }

    // Calculator State
    var firstOperand by remember { mutableStateOf<Double?>(null) }
    var operator by remember { mutableStateOf<String?>(null) }
    var isEnteringSecondOperand by remember { mutableStateOf(false) }

    val templates = listOf(
        "Grocery Run" to (Icons.Rounded.ShoppingCart to "Shopping"),
        "Salary Deposit" to (Icons.Rounded.Payments to "Income"),
        "Rent Payment" to (Icons.Rounded.Home to "Housing"),
        "Coffee" to (Icons.Rounded.CoffeeMaker to "Food")
    )
    var selectedTemplate by remember { mutableStateOf<String?>(null) }
    
    var showOCR by remember { mutableStateOf(false) }
    var showDatePicker by remember { mutableStateOf(false) }
    var showCategorySheet by remember { mutableStateOf(false) }
    var showAccountSheet by remember { mutableStateOf(false) }
    var showTargetAccountSheet by remember { mutableStateOf(false) }
    var showExchangeRateDialog by remember { mutableStateOf(false) }
    var isDetailDrawerOpen by remember { mutableStateOf(false) }
    var showTemplateSheet by remember { mutableStateOf(false) }

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

    LaunchedEffect(accounts) {
        if (selectedAccount == null && accounts.isNotEmpty()) {
            selectedAccount = accounts.find { it.name == "Aeon Prime Savings" } ?: accounts.first()
            selectedCurrency = selectedAccount?.currencyCode ?: "USD"
        }
        if (targetAccount == null && accounts.size > 1) {
            targetAccount = accounts.find { it.id != selectedAccount?.id } ?: accounts.last()
            targetCurrency = targetAccount?.currencyCode ?: "USD"
        } else if (targetAccount == null && accounts.isNotEmpty()) {
            targetAccount = accounts.first()
            targetCurrency = targetAccount?.currencyCode ?: "USD"
        }
    }

    LaunchedEffect(selectedCurrency, targetCurrency, exchangeRates) {
        if (isTransfer) {
            val baseRates = exchangeRates
            if (baseRates.isNotEmpty()) {
                val sourceRateInUSD = baseRates[selectedCurrency] ?: 1.0
                val destRateInUSD = baseRates[targetCurrency] ?: 1.0
                // If rates are relative to USD (1 USD = X Currency)
                // To get 1 Source = Y Dest:
                // 1 Source = (1/sourceRateInUSD) USD
                // (1/sourceRateInUSD) USD * destRateInUSD = destRateInUSD / sourceRateInUSD
                if (sourceRateInUSD != 0.0) {
                    exchangeRate = destRateInUSD / sourceRateInUSD
                }
            }
        }
    }

    val datePickerState = rememberDatePickerState(initialSelectedDateMillis = selectedDateMillis)

    Box(modifier = Modifier.fillMaxSize()) {
        Scaffold(
            topBar = {
                CenterAlignedTopAppBar(
                    title = { 
                        Text(
                            "New Record", 
                            style = MaterialTheme.typography.titleLarge.copy(
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        ) 
                    },
                    navigationIcon = {
                        IconButton(onClick = onBackClick) {
                            Icon(Icons.Rounded.Close, contentDescription = "Close", tint = MaterialTheme.colorScheme.onSurface)
                        }
                    },
                    actions = {
                        Surface(
                            modifier = Modifier.padding(end = 16.dp),
                            shape = RoundedCornerShape(24.dp),
                            color = MaterialTheme.colorScheme.primary,
                            onClick = {
                                val amountDouble = amount.toDoubleOrNull() ?: 0.0
                                selectedAccount?.id?.let { accountId ->
                                    val tagsList = tagsInput.split(",").map { it.trim() }.filter { it.isNotBlank() }
                                    
                                    if (isTransfer) {
                                        // 1. Withdrawal from source
                                        viewModel.addTransaction(
                                            title = if (title.isBlank()) "Transfer to ${targetAccount?.name}" else title,
                                            amount = amountDouble,
                                            category = "Transfer",
                                            isIncome = false,
                                            accountId = accountId,
                                            currency = selectedCurrency,
                                            tags = tagsList,
                                            timestamp = selectedDateMillis,
                                            payee = payee,
                                            note = note
                                        )
                                        accountViewModel.updateBalance(accountId, -amountDouble)

                                        // 2. Deposit to target
                                        targetAccount?.id?.let { targetId ->
                                            val targetAmount = amountDouble * exchangeRate
                                            viewModel.addTransaction(
                                                title = if (title.isBlank()) "Transfer from ${selectedAccount?.name}" else title,
                                                amount = targetAmount,
                                                category = "Transfer",
                                                isIncome = true,
                                                accountId = targetId,
                                                currency = targetCurrency,
                                                tags = tagsList,
                                                timestamp = selectedDateMillis,
                                                payee = payee,
                                                note = note
                                            )
                                            accountViewModel.updateBalance(targetId, targetAmount)
                                        }
                                    } else {
                                        viewModel.addTransaction(
                                            title = if (title.isBlank()) category else title,
                                            amount = amountDouble,
                                            category = category,
                                            isIncome = isIncome || isRefund,
                                            accountId = accountId,
                                            currency = selectedCurrency,
                                            tags = tagsList,
                                            timestamp = selectedDateMillis,
                                            payee = payee,
                                            note = note
                                        )
                                        
                                        val amountInAccountCurrency = if (selectedCurrency == selectedAccount?.currencyCode) {
                                            amountDouble
                                        } else {
                                            val txRate = exchangeRates[selectedCurrency] ?: 1.0
                                            val accountRate = exchangeRates[selectedAccount?.currencyCode] ?: 1.0
                                            if (txRate != 0.0) (amountDouble / txRate) * accountRate else amountDouble
                                        }

                                        accountViewModel.updateBalance(
                                            accountId,
                                            if (isIncome || isRefund) amountInAccountCurrency else -amountInAccountCurrency
                                        )
                                    }
                                    onBackClick()
                                }
                            },
                            enabled = selectedAccount != null
                        ) {
                            Text(
                                "Save",
                                modifier = Modifier.padding(horizontal = 24.dp, vertical = 10.dp),
                                style = MaterialTheme.typography.labelLarge.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onPrimary
                                )
                            )
                        }
                    },
                    colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    )
                )
            },
            containerColor = MaterialTheme.colorScheme.surface
        ) { innerPadding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .padding(horizontal = 20.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Header Card: Type & Amount (Aeon Style)
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
                        // Transaction Type Row
                        Row(
                            modifier = Modifier
                                .clip(RoundedCornerShape(32.dp))
                                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                                .padding(4.dp),
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            val types = listOf(
                                Icons.Rounded.RemoveCircleOutline,
                                Icons.Rounded.AddCircleOutline,
                                Icons.Rounded.SyncAlt,
                                Icons.Rounded.Restore
                            )
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

                        // Amount & Currency Row
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Center
                            ) {
                                if (isTransfer) {
                                    IconButton(onClick = { showExchangeRateDialog = true }) {
                                        Icon(Icons.Rounded.CurrencyExchange, contentDescription = "Exchange Rate", tint = MaterialTheme.colorScheme.primary)
                                    }
                                    Spacer(modifier = Modifier.width(8.dp))
                                }

                                Text(
                                    text = if (amount.isEmpty()) "0.00" else (if (isIncome || isRefund) "+" else if (isTransfer) "" else "-") + amount,
                                    style = TextStyle(
                                        fontSize = 48.sp,
                                        fontWeight = FontWeight.Black,
                                        textAlign = TextAlign.Center,
                                        color = when {
                                            isIncome || isRefund -> Color(0xFF2E7D32)
                                            isTransfer -> MaterialTheme.colorScheme.onSurface
                                            else -> MaterialTheme.colorScheme.error
                                        }
                                    ),
                                    modifier = Modifier.weight(1f, fill = false)
                                )
                                
                                Spacer(modifier = Modifier.width(12.dp))
                                
                                CurrencySelector(
                                    selectedCurrency = selectedCurrency,
                                    onCurrencySelected = { selectedCurrency = it },
                                    supportedCurrencies = selectedAccount?.allowedCurrencies ?: listOf("USD")
                                )
                            }
                            
                            if (isTransfer) {
                                val amountDouble = amount.toDoubleOrNull() ?: 0.0
                                val convertedAmount = amountDouble * exchangeRate
                                
                                Icon(
                                    Icons.Rounded.KeyboardArrowDown,
                                    contentDescription = null,
                                    modifier = Modifier.size(24.dp).padding(vertical = 4.dp),
                                    tint = MaterialTheme.colorScheme.outline
                                )

                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.Center
                                ) {
                                    Text(
                                        " (1:${String.format(Locale.getDefault(), "%.3f", exchangeRate)})",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.padding(start = 8.dp)
                                    )
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Text(
                                        text = String.format(Locale.getDefault(), "%.2f", convertedAmount),
                                        style = TextStyle(
                                            fontSize = 32.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Color(0xFF2E7D32)
                                        )
                                    )
                                    Spacer(modifier = Modifier.width(12.dp))
                                    CurrencySelector(
                                        selectedCurrency = targetCurrency,
                                        onCurrencySelected = { targetCurrency = it },
                                        supportedCurrencies = targetAccount?.allowedCurrencies ?: listOf("USD")
                                    )
                                }
                            }
                        }
                    }
                }

                // Slim Action Cards
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    RecordFieldCard(
                        icon = Icons.Rounded.AutoAwesome,
                        label = "SELECT TEMPLATE",
                        value = selectedTemplate ?: "None Selected",
                        trailingIcon = Icons.Rounded.KeyboardArrowDown,
                        onClick = { showTemplateSheet = true },
                        iconContainerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.5f),
                        isSlim = true
                    )

                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        RecordFieldCard(
                            icon = Icons.Rounded.AccountBalance,
                            label = if (isTransfer) "FROM" else "LEDGER",
                            value = selectedAccount?.name ?: "Select",
                            onClick = { showAccountSheet = true },
                            modifier = Modifier.weight(1f),
                            iconContainerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f),
                            isSlim = true
                        )
                        
                        if (isTransfer) {
                            RecordFieldCard(
                                icon = Icons.Rounded.AccountBalance,
                                label = "TO",
                                value = targetAccount?.name ?: "Select",
                                onClick = { showTargetAccountSheet = true },
                                modifier = Modifier.weight(1f),
                                iconContainerColor = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.5f),
                                isSlim = true
                            )
                        } else {
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
                }

                Spacer(modifier = Modifier.weight(1f))

                // Number Pad
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
            }
        }

        // Right Detail Drawer
        DetailDrawer(
            isOpen = isDetailDrawerOpen,
            onClose = { isDetailDrawerOpen = false },
            onSwipeOpen = { isDetailDrawerOpen = true }
        ) {
            Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                // Metadata Section
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
                                decorationBox = { innerTextField ->
                                    if (payee.isEmpty()) Text("Add Payee", color = MaterialTheme.colorScheme.outline)
                                    innerTextField()
                                }
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
                                decorationBox = { innerTextField ->
                                    if (note.isEmpty()) Text("Add Note", color = MaterialTheme.colorScheme.outline)
                                    innerTextField()
                                }
                            )
                        }

                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.3f),
                                onClick = { /* Recurring toggle */ }
                            ) {
                                Text("RECURRING", modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp), style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                            }
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = MaterialTheme.colorScheme.surfaceVariant,
                                onClick = { /* Tag dialog */ }
                            ) {
                                Row(modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Rounded.Add, contentDescription = null, modifier = Modifier.size(12.dp), tint = MaterialTheme.colorScheme.outline)
                                    Text("LABEL", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }

                // Occurrence & Payment Method
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
                                Text("Today, 14:30", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                    Surface(
                        modifier = Modifier.weight(1f).height(80.dp),
                        shape = RoundedCornerShape(28.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f)
                    ) {
                        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.Center) {
                            Text("PAYMENT METHOD", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.outline)
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Rounded.Contactless, contentDescription = null, modifier = Modifier.size(16.dp), tint = Color(0xFF26C6DA))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Digital Wallet", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }

                // Map/Location Card
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

                // Warranty Toggle
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(28.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f)
                ) {
                    Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                        Surface(shape = CircleShape, color = Color(0xFFB3E5FC), modifier = Modifier.size(40.dp)) {
                            Icon(Icons.Rounded.GppGood, contentDescription = null, modifier = Modifier.padding(10.dp), tint = Color(0xFF0288D1))
                        }
                        Spacer(modifier = Modifier.width(16.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Warranty Tracking", style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Bold)
                            Text("Notify before expiration", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.outline)
                        }
                        Switch(checked = isWarrantyEnabled, onCheckedChange = { isWarrantyEnabled = it })
                    }
                }
            }
        }
    }

    if (showAccountSheet) {
        AccountPickerSheet(
            accounts = accounts,
            selectedAccount = selectedAccount,
            onAccountSelected = { account ->
                selectedAccount = account
                selectedCurrency = account.currencyCode
                showAccountSheet = false
            },
            onDismiss = { showAccountSheet = false }
        )
    }

    if (showTargetAccountSheet) {
        AccountPickerSheet(
            accounts = accounts,
            selectedAccount = targetAccount,
            onAccountSelected = { account ->
                targetAccount = account
                targetCurrency = account.currencyCode
                showTargetAccountSheet = false
            },
            onDismiss = { showTargetAccountSheet = false }
        )
    }

    if (showExchangeRateDialog) {
        ExchangeRateDialog(
            initialRate = exchangeRate,
            onDismiss = { showExchangeRateDialog = false },
            onConfirm = { 
                exchangeRate = it
                showExchangeRateDialog = false
            }
        )
    }

    if (showDatePicker) {
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    selectedDateMillis = datePickerState.selectedDateMillis ?: System.currentTimeMillis()
                    showDatePicker = false
                }) { Text("CONFIRM") }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) { Text("CANCEL") }
            }
        ) {
            DatePicker(state = datePickerState)
        }
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

    if (showOCR) {
        OCRScanner(
            onResult = { result ->
                if (result != null) {
                    amount = result.toString()
                }
                showOCR = false
            },
            onDismiss = { showOCR = false }
        )
    }

    if (showTemplateSheet) {
        ModalBottomSheet(
            onDismissRequest = { showTemplateSheet = false },
            containerColor = MaterialTheme.colorScheme.surface,
            shape = RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp)
        ) {
            Column(modifier = Modifier.padding(24.dp).padding(bottom = 32.dp)) {
                Text("QUICK TEMPLATES", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
                Spacer(modifier = Modifier.height(24.dp))
                LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    items(templates) { (name, info) ->
                        val (icon, cat) = info
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    selectedTemplate = name
                                    category = cat
                                    transactionType = if (cat == "Income") 1 else 0
                                    showTemplateSheet = false
                                },
                            shape = RoundedCornerShape(24.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f)
                        ) {
                            Row(
                                modifier = Modifier.padding(20.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                                Spacer(modifier = Modifier.width(16.dp))
                                Text(name, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Medium)
                            }
                        }
                    }
                }
            }
        }
    }
}
