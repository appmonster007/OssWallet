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
import com.osscabinet.gleemanager.ui.components.OCRScanner
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
    
    var showOCR by remember { mutableStateOf(false) }
    var showDatePicker by remember { mutableStateOf(false) }
    var showCategorySheet by remember { mutableStateOf(false) }
    var showAccountSheet by remember { mutableStateOf(false) }
    var showTargetAccountSheet by remember { mutableStateOf(false) }
    var showExchangeRateDialog by remember { mutableStateOf(false) }
    var isDetailDrawerOpen by remember { mutableStateOf(false) }

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
                                            title = "Transfer to ${targetAccount?.name}",
                                            amount = amountDouble,
                                            category = "Transfer",
                                            isIncome = false,
                                            accountId = accountId,
                                            currency = selectedCurrency,
                                            tags = tagsList,
                                            timestamp = selectedDateMillis
                                        )
                                        accountViewModel.updateBalance(accountId, -amountDouble)

                                        // 2. Deposit to target
                                        targetAccount?.id?.let { targetId ->
                                            val targetAmount = amountDouble * exchangeRate
                                            viewModel.addTransaction(
                                                title = "Transfer from ${selectedAccount?.name}",
                                                amount = targetAmount,
                                                category = "Transfer",
                                                isIncome = true,
                                                accountId = targetId,
                                                currency = targetCurrency,
                                                tags = tagsList,
                                                timestamp = selectedDateMillis
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
                                            timestamp = selectedDateMillis
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
                            enabled = amount.isNotBlank() && selectedAccount != null
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
                        Row(
                            modifier = Modifier.fillMaxWidth(),
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
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Center,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Icon(Icons.AutoMirrored.Rounded.ArrowForward, contentDescription = null, modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.outline)
                                Spacer(modifier = Modifier.width(12.dp))
                                CurrencySelector(
                                    selectedCurrency = targetCurrency,
                                    onCurrencySelected = { targetCurrency = it },
                                    supportedCurrencies = targetAccount?.allowedCurrencies ?: listOf("USD")
                                )
                                if (exchangeRate != 1.0) {
                                    Text(
                                        " (1:${String.format(Locale.getDefault(), "%.2f", exchangeRate)})",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.padding(start = 8.dp)
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
                        value = "None Selected",
                        trailingIcon = Icons.Rounded.KeyboardArrowDown,
                        onClick = { /* TODO */ },
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
                        if (input == ".") {
                            if (!amount.contains(".")) {
                                amount += if (amount.isEmpty()) "0." else "."
                            }
                        } else {
                            amount += input
                        }
                    },
                    onDeleteClick = {
                        if (amount.isNotEmpty()) amount = amount.dropLast(1)
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
        ModalBottomSheet(
            onDismissRequest = { showCategorySheet = false },
            containerColor = MaterialTheme.colorScheme.surface,
            shape = RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp)
        ) {
            Column(modifier = Modifier.padding(24.dp).padding(bottom = 32.dp)) {
                Text("CLASSIFICATION", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
                Spacer(modifier = Modifier.height(32.dp))
                LazyVerticalGrid(
                    columns = GridCells.Fixed(4),
                    verticalArrangement = Arrangement.spacedBy(24.dp),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    items(categoryIcons.toList()) { (name, icon) ->
                        CategoryItem(
                            name = name,
                            icon = icon,
                            isSelected = category == name,
                            onClick = {
                                category = name
                                showCategorySheet = false
                            }
                        )
                    }
                }
            }
        }
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
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AccountPickerSheet(
    accounts: List<Account>,
    selectedAccount: Account?,
    onAccountSelected: (Account) -> Unit,
    onDismiss: () -> Unit
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp)
    ) {
        Column(modifier = Modifier.padding(24.dp).padding(bottom = 32.dp)) {
            Text("SELECT LEDGER", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
            Spacer(modifier = Modifier.height(24.dp))
            LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                items(accounts) { account ->
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onAccountSelected(account) },
                        shape = RoundedCornerShape(24.dp),
                        color = if (selectedAccount?.id == account.id) Color(android.graphics.Color.parseColor(account.colorHex)).copy(alpha = 0.1f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f),
                        border = if (selectedAccount?.id == account.id) BorderStroke(2.dp, Color(android.graphics.Color.parseColor(account.colorHex))) else null
                    ) {
                        Row(
                            modifier = Modifier.padding(20.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(modifier = Modifier.size(16.dp).clip(CircleShape).background(Color(android.graphics.Color.parseColor(account.colorHex))))
                            Spacer(modifier = Modifier.width(16.dp))
                            Text(account.name, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Medium)
                            Spacer(modifier = Modifier.weight(1f))
                            Text(formatCurrency(account.balance, account.currencyCode), style = MaterialTheme.typography.bodyMedium.copy(fontFeatureSettings = "tnum"), color = MaterialTheme.colorScheme.outline)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun CurrencySelector(
    selectedCurrency: String,
    onCurrencySelected: (String) -> Unit,
    supportedCurrencies: List<String>
) {
    var expanded by remember { mutableStateOf(false) }
    Box {
        Surface(
            modifier = Modifier.clickable { expanded = true },
            shape = RoundedCornerShape(12.dp),
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    selectedCurrency.ifEmpty { "USD" },
                    style = MaterialTheme.typography.labelMedium.copy(
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                )
                Icon(
                    Icons.Rounded.KeyboardArrowDown,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            if (supportedCurrencies.isEmpty()) {
                DropdownMenuItem(text = { Text("USD") }, onClick = { onCurrencySelected("USD"); expanded = false })
            } else {
                supportedCurrencies.forEach { currency ->
                    DropdownMenuItem(
                        text = { Text(currency) },
                        onClick = {
                            onCurrencySelected(currency)
                            expanded = false
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun ExchangeRateDialog(
    initialRate: Double,
    onDismiss: () -> Unit,
    onConfirm: (Double) -> Unit
) {
    var rate by remember { mutableStateOf(initialRate.toString()) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Exchange Rate", fontWeight = FontWeight.Bold) },
        text = {
            OutlinedTextField(
                value = rate,
                onValueChange = { rate = it },
                label = { Text("Rate (Source to Target)") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth()
            )
        },
        confirmButton = {
            Button(
                onClick = { onConfirm(rate.toDoubleOrNull() ?: 1.0) },
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("Confirm")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@Composable
fun DetailDrawer(
    isOpen: Boolean,
    onClose: () -> Unit,
    onSwipeOpen: () -> Unit,
    content: @Composable () -> Unit
) {
    val screenWidth = LocalConfiguration.current.screenWidthDp.dp
    val offset by animateDpAsState(if (isOpen) 0.dp else screenWidth)

    Box(
        modifier = Modifier
            .fillMaxSize()
            .pointerInput(Unit) {
                detectHorizontalDragGestures { change, dragAmount ->
                    change.consume()
                    if (dragAmount < -20) onSwipeOpen()
                    if (dragAmount > 20) onClose()
                }
            }
    ) {
        // Tab indicator on the right edge when closed
        if (!isOpen) {
            Surface(
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .offset(x = 8.dp) // Hide slightly
                    .width(48.dp)
                    .height(80.dp)
                    .clickable { onSwipeOpen() },
                shape = RoundedCornerShape(topStart = 24.dp, bottomStart = 24.dp),
                color = MaterialTheme.colorScheme.primaryContainer,
                shadowElevation = 8.dp
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        Icons.AutoMirrored.Rounded.KeyboardArrowLeft, 
                        contentDescription = "Open Details",
                        modifier = Modifier.padding(end = 12.dp)
                    )
                }
            }
        }

        // Drawer content overlay
        Surface(
            modifier = Modifier
                .fillMaxHeight()
                .width(screenWidth)
                .offset(x = offset),
            color = MaterialTheme.colorScheme.surface,
            shadowElevation = 24.dp
        ) {
            Column {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = onClose) {
                        Icon(Icons.AutoMirrored.Rounded.ArrowForward, contentDescription = "Back")
                    }
                    Text(
                        "Additional Details", 
                        style = MaterialTheme.typography.titleMedium, 
                        fontWeight = FontWeight.Bold
                    )
                }
                Box(modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
                    content()
                }
            }
        }
    }
}

@Composable
fun Numpad(
    onNumberClick: (String) -> Unit,
    onDeleteClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val keys = listOf(
        "7", "8", "9",
        "4", "5", "6",
        "1", "2", "3",
        ".", "0", "DEL"
    )
    
    LazyVerticalGrid(
        columns = GridCells.Fixed(3),
        modifier = modifier.padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        userScrollEnabled = false
    ) {
        items(keys) { key ->
            Surface(
                onClick = { if (key == "DEL") onDeleteClick() else onNumberClick(key) },
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
            ) {
                Box(
                    modifier = Modifier.padding(vertical = 12.dp),
                    contentAlignment = Alignment.Center
                ) {
                    if (key == "DEL") {
                        Icon(Icons.AutoMirrored.Rounded.Backspace, contentDescription = null, modifier = Modifier.size(24.dp))
                    } else {
                        Text(
                            text = key,
                            style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun RecordFieldCard(
    icon: ImageVector,
    label: String,
    value: String,
    trailingIcon: ImageVector? = null,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    iconContainerColor: Color = MaterialTheme.colorScheme.primaryContainer,
    isSlim: Boolean = false
) {
    val verticalPadding = if (isSlim) 10.dp else 16.dp
    val iconSize = if (isSlim) 36.dp else 48.dp
    val innerIconSize = if (isSlim) 18.dp else 24.dp

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(if (isSlim) 20.dp else 28.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = verticalPadding),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                modifier = Modifier.size(iconSize),
                shape = CircleShape,
                color = iconContainerColor
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        icon,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.size(innerIconSize)
                    )
                }
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    label,
                    style = MaterialTheme.typography.labelSmall.copy(
                        color = MaterialTheme.colorScheme.outline,
                        fontSize = if (isSlim) 9.sp else 11.sp,
                        letterSpacing = 0.5.sp
                    )
                )
                Text(
                    value,
                    style = MaterialTheme.typography.bodyLarge.copy(
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                        fontSize = if (isSlim) 14.sp else 16.sp
                    ),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            if (trailingIcon != null) {
                Icon(
                    trailingIcon,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.outline,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}

@Composable
fun CategoryItem(name: String, icon: ImageVector, isSelected: Boolean, onClick: () -> Unit) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.clickable(onClick = onClick)
    ) {
        Surface(
            modifier = Modifier.size(64.dp),
            shape = CircleShape,
            color = if (isSelected) Color(0xFF004D56) else Color(0xFFF1F3F4)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    icon,
                    contentDescription = name,
                    tint = if (isSelected) Color.White else Color(0xFF546E7A),
                    modifier = Modifier.size(28.dp)
                )
            }
        }
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            name,
            style = MaterialTheme.typography.labelSmall,
            color = if (isSelected) Color(0xFF004D56) else Color(0xFF546E7A)
        )
    }
}

val categoryIcons = mapOf(
    "Food" to Icons.Rounded.Restaurant,
    "Transport" to Icons.Rounded.DirectionsCar,
    "Shopping" to Icons.Rounded.ShoppingBag,
    "Electronics" to Icons.Rounded.Smartphone,
    "Entertainment" to Icons.Rounded.SportsEsports,
    "Health" to Icons.Rounded.MedicalServices,
    "Bills" to Icons.Rounded.Receipt,
    "Investment" to Icons.AutoMirrored.Rounded.TrendingUp,
    "Housing" to Icons.Rounded.Home,
    "Education" to Icons.Rounded.School,
    "Other" to Icons.Rounded.MoreHoriz
)
