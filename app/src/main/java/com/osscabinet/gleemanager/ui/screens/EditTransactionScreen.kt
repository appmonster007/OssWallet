package com.osscabinet.gleemanager.ui.screens

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
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
import com.osscabinet.gleemanager.domain.model.CashFlowType
import com.osscabinet.gleemanager.ui.components.*
import com.osscabinet.gleemanager.ui.util.formatCurrencyRight
import com.osscabinet.gleemanager.ui.viewmodel.AccountViewModel
import com.osscabinet.gleemanager.ui.viewmodel.AnalyticsViewModel
import com.osscabinet.gleemanager.ui.viewmodel.SettingsViewModel
import com.osscabinet.gleemanager.ui.viewmodel.TagViewModel
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
    tagViewModel: TagViewModel,
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
    var amount by remember { mutableStateOf(if (transaction.amount % 1 == 0.0) transaction.amount.toLong().toString() else String.format(Locale.getDefault(), "%.2f", transaction.amount)) }
    var linkedTransactionId by remember { mutableStateOf(transaction.linkedTransactionId) }
    
    // Calculator State
    var firstOperand by remember { mutableStateOf<Double?>(null) }
    var operator by remember { mutableStateOf<String?>(null) }
    var isEnteringSecondOperand by remember { mutableStateOf(false) }

    fun calculateResult() {
        if (firstOperand != null && operator != null && amount.isNotEmpty()) {
            val secondOperand = amount.toDoubleOrNull() ?: 0.0
            val rawResult = when (operator) {
                "+" -> firstOperand!! + secondOperand
                "−" -> firstOperand!! - secondOperand
                "×" -> firstOperand!! * secondOperand
                "÷" -> if (secondOperand != 0.0) firstOperand!! / secondOperand else 0.0
                else -> secondOperand
            }
            val result = kotlin.math.abs(rawResult)
            amount = if (result % 1 == 0.0) result.toLong().toString() else String.format(Locale.getDefault(), "%.2f", result)
            firstOperand = null
            operator = null
            isEnteringSecondOperand = false
        }
    }

    var category by remember { mutableStateOf(transaction.category) }
    var tagsInput by remember { mutableStateOf(transaction.tags.joinToString(", ")) }
    
    // Inferred transaction type: 0: Expense, 1: Income, 2: Transfer
    var transactionType by remember { 
        mutableStateOf(
            when {
                transaction.category == "Transfer" -> 2
                transaction.isIncome -> 1
                else -> 0
            }
        ) 
    }
    
    val isIncome = transactionType == 1
    val isTransfer = transactionType == 2
    
    var selectedDateMillis by remember { mutableStateOf(transaction.timestamp) }
    var payee by remember { mutableStateOf(transaction.payee) }
    var note by remember { mutableStateOf(transaction.note) }
    var isWarrantyEnabled by remember { mutableStateOf(false) }
    
    val accounts by accountViewModel.accounts.collectAsState()
    val exchangeRates by analyticsViewModel.exchangeRates.collectAsState()
    var selectedAccount by remember { mutableStateOf(accounts.find { it.id == transaction.accountId }) }
    var selectedCurrency by remember { mutableStateOf(transaction.currency) }
    
    var targetAccount by remember { mutableStateOf<Account?>(null) }
    var targetCurrency by remember { mutableStateOf("") }
    var exchangeRate by remember { mutableStateOf(1.0) }

    var showDatePicker by remember { mutableStateOf(false) }
    var showCategorySheet by remember { mutableStateOf(false) }
    var showAccountSheet by remember { mutableStateOf(false) }
    var showTargetAccountSheet by remember { mutableStateOf(false) }
    var showExchangeRateDialog by remember { mutableStateOf(false) }
    var isDetailDrawerOpen by remember { mutableStateOf(false) }
    var showTemplateSheet by remember { mutableStateOf(false) }
    var showLinkedTransactionSheet by remember { mutableStateOf(false) }
    var showLabelSheet by remember { mutableStateOf(false) }

    val templates = listOf(
        "Grocery Run" to (Icons.Rounded.ShoppingCart to "Shopping"),
        "Salary Deposit" to (Icons.Rounded.Payments to "Income"),
        "Rent Payment" to (Icons.Rounded.Home to "Housing"),
        "Coffee" to (Icons.Rounded.CoffeeMaker to "Food")
    )
    var selectedTemplate by remember { mutableStateOf<String?>(null) }

    val datePickerState = rememberDatePickerState(initialSelectedDateMillis = selectedDateMillis)

    // Sync exchange rate for transfers
    LaunchedEffect(isTransfer, selectedCurrency, targetCurrency, exchangeRates) {
        if (isTransfer && targetAccount != null) {
            val baseRates = exchangeRates
            if (baseRates.isNotEmpty()) {
                val sourceRateInUSD = baseRates[selectedCurrency] ?: 1.0
                val destRateInUSD = baseRates[targetCurrency] ?: 1.0
                if (sourceRateInUSD != 0.0) {
                    exchangeRate = destRateInUSD / sourceRateInUSD
                }
            }
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Scaffold(
            topBar = {
                CenterAlignedTopAppBar(
                    title = { 
                        Text(
                            "Edit Record", 
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

                        Surface(
                            modifier = Modifier.padding(end = 16.dp, start = 8.dp),
                            shape = RoundedCornerShape(24.dp),
                            color = MaterialTheme.colorScheme.primary,
                            onClick = {
                                val amountDouble = amount.toDoubleOrNull() ?: 0.0
                                selectedAccount?.let { account ->
                                    // 1. Revert old balance impact
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

                                    // 2. Apply new balance impact
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

                                    // 3. Update transaction record
                                    viewModel.updateTransaction(
                                        oldTransaction = transaction,
                                        title = title.ifBlank { if (isTransfer) "Transfer" else category },
                                        amount = amountDouble,
                                        category = if (isTransfer) "Transfer" else category,
                                        isIncome = isIncome,
                                        accountId = account.id,
                                        currency = selectedCurrency,
                                        tags = tagsInput.split(",").map { it.trim() }.filter { it.isNotBlank() },
                                        timestamp = selectedDateMillis,
                                        payee = payee,
                                        note = note,
                                        linkedTransactionId = linkedTransactionId
                                    )
                                    onBackClick()
                                }
                            }
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
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Header Card: Type & Amount
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(240.dp),
                    shape = RoundedCornerShape(40.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
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
                                Icons.Rounded.SyncAlt
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

                        // Amount & Currency Area
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
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
                                        text = if (amount.isEmpty()) "0.00" else (if (isIncome) "+" else if (isTransfer) "" else "-") + amount,
                                        style = TextStyle(
                                            fontSize = if (isTransfer) 48.sp else 64.sp,
                                            fontWeight = FontWeight.Black,
                                            textAlign = TextAlign.Center,
                                            color = when {
                                                isIncome -> Color(0xFF2E7D32)
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
                }

                // Slim Action Cards
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    EditorialInput(
                        value = title, 
                        onValueChange = { title = it }, 
                        label = "RECORD TITLE", 
                        icon = Icons.Rounded.Edit
                    )

                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        RecordFieldCard(
                            icon = Icons.Rounded.AccountBalance,
                            label = if (isTransfer) "FROM" else "ACCOUNT",
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
                    modifier = Modifier.fillMaxWidth().weight(1f)
                )
            }
        }

        // Right Detail Drawer
        DetailDrawer(
            isOpen = isDetailDrawerOpen,
            onClose = { isDetailDrawerOpen = false },
            onSwipeOpen = { isDetailDrawerOpen = true },
            tabAlignment = Alignment.TopEnd,
            tabOffsetY = 180.dp
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                // Metadata Section
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(28.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f)
                ) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
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

                        // Linked Transaction Field
                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            onClick = { showLinkedTransactionSheet = true },
                            color = Color.Transparent
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Rounded.Link, contentDescription = null, modifier = Modifier.size(20.dp), tint = MaterialTheme.colorScheme.outline)
                                Spacer(modifier = Modifier.width(12.dp))
                                Column {
                                    val linkedTx = transactions.find { it.id == linkedTransactionId }
                                    Text(
                                        text = linkedTx?.let { "${it.title} (${it.amount})" } ?: "Link to Transaction",
                                        style = MaterialTheme.typography.bodyLarge,
                                        color = if (linkedTransactionId != null) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline
                                    )
                                    if (linkedTransactionId != null) {
                                        Text("Tap to change", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.outline)
                                    }
                                }
                                if (linkedTransactionId != null) {
                                    Spacer(modifier = Modifier.weight(1f))
                                    IconButton(onClick = { linkedTransactionId = null }) {
                                        Icon(Icons.Rounded.Close, contentDescription = "Unlink", modifier = Modifier.size(16.dp))
                                    }
                                }
                            }
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
                                onClick = { showLabelSheet = true }
                            ) {
                                Row(modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Rounded.Add, contentDescription = null, modifier = Modifier.size(12.dp), tint = MaterialTheme.colorScheme.outline)
                                    Text(
                                        if (tagsInput.isEmpty()) "LABEL" else "${tagsInput.split(",").filter { it.isNotBlank() }.size} LABELS",
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold
                                    )
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
                                val calendar = Calendar.getInstance().apply { timeInMillis = selectedDateMillis }
                                Text("${calendar.get(Calendar.DAY_OF_MONTH)}/${calendar.get(Calendar.MONTH) + 1}", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold)
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

    if (showLabelSheet) {
        LabelPickerSheet(
            selectedTags = tagsInput.split(",").map { it.trim() }.filter { it.isNotBlank() },
            onTagsChanged = { tagsInput = it.joinToString(", ") },
            onDismiss = { showLabelSheet = false },
            tagViewModel = tagViewModel
        )
    }

    if (showTemplateSheet) {
        ModalBottomSheet(
            onDismissRequest = { showTemplateSheet = false },
            containerColor = MaterialTheme.colorScheme.surface,
            shape = RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp)
        ) {
            Column(modifier = Modifier.padding(24.dp).padding(bottom = 32.dp)) {
                AeonSectionHeader(title = "Quick Templates")
                Spacer(modifier = Modifier.height(24.dp))
                LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    items(templates.sortedBy { it.first }) { (name, info) ->
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

    if (showLinkedTransactionSheet) {
        ModalBottomSheet(
            onDismissRequest = { showLinkedTransactionSheet = false },
            containerColor = MaterialTheme.colorScheme.surface,
            shape = RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp)
        ) {
            Column(modifier = Modifier.padding(24.dp).padding(bottom = 32.dp).fillMaxHeight(0.6f)) {
                AeonSectionHeader(title = "Select Transaction To Link")
                Spacer(modifier = Modifier.height(24.dp))
                LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    items(transactions.filter { it.id != transactionId }.sortedByDescending { it.timestamp }) { tx ->
                        AeonTransactionItem(
                            category = tx.category,
                            account = accounts.find { it.id == tx.accountId }?.name ?: "Unknown",
                            amount = formatCurrencyRight(if (tx.isIncome) tx.amount else -tx.amount, tx.currency),
                            balanceBefore = "",
                            balanceAfter = "",
                            type = if (tx.isIncome) CashFlowType.INCOME else CashFlowType.EXPENSE,
                            timestamp = SimpleDateFormat("MMM dd, HH:mm", Locale.getDefault()).format(Date(tx.timestamp)),
                            note = tx.note,
                            tags = tx.tags,
                            icon = categoryIcons[tx.category] ?: Icons.Rounded.Category,
                            onClick = {
                                linkedTransactionId = tx.id
                                showLinkedTransactionSheet = false
                            },
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }
        }
    }
}
