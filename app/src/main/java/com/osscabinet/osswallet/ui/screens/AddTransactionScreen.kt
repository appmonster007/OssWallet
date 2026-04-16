package com.osscabinet.ossWallet.ui.screens

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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.osscabinet.ossWallet.domain.model.Account
import com.osscabinet.ossWallet.domain.model.CashFlowType
import com.osscabinet.ossWallet.ui.components.*
import com.osscabinet.ossWallet.ui.components.formatDateTime
import com.osscabinet.ossWallet.ui.util.formatCurrencyRight
import com.osscabinet.ossWallet.ui.viewmodel.AccountViewModel
import com.osscabinet.ossWallet.ui.viewmodel.AnalyticsViewModel
import com.osscabinet.ossWallet.ui.viewmodel.TagViewModel
import com.osscabinet.ossWallet.ui.viewmodel.TransactionViewModel
import com.osscabinet.ossWallet.ui.viewmodel.TemplateViewModel
import java.math.BigDecimal
import java.math.RoundingMode
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddTransactionScreen(
    accountId: String? = null,
    initialType: Int = 0,
    initialTemplate: String? = null,
    viewModel: TransactionViewModel,
    accountViewModel: AccountViewModel,
    analyticsViewModel: AnalyticsViewModel,
    templateViewModel: TemplateViewModel,
    tagViewModel: TagViewModel,
    settingsViewModel: com.osscabinet.ossWallet.ui.viewmodel.SettingsViewModel,
    onBackClick: () -> Unit
) {
    var title by remember { mutableStateOf("") }
    var amount by remember { mutableStateOf("") }
    var category by remember { mutableStateOf("Shopping") }
    var selectedCategoryId by remember { mutableStateOf<Long?>(null) }
    var selectedTags by remember { mutableStateOf<List<String>>(emptyList()) }
    var paymentMethod by remember { mutableStateOf("Cash") }
    var transactionType by remember { mutableIntStateOf(initialType) } // 0: Expense, 1: Income, 2: Transfer
    val isIncome = transactionType == 1
    val isTransfer = transactionType == 2
    var selectedDateMillis by remember { mutableStateOf(System.currentTimeMillis()) }
    var payee by remember { mutableStateOf("") }
    var note by remember { mutableStateOf("") }
    var isWarrantyEnabled by remember { mutableStateOf(false) }
    var linkedTransactionId by remember { mutableStateOf<Long?>(null) }
    
    val accounts by accountViewModel.accounts.collectAsState()
    val transactions by viewModel.transactions.collectAsState()
    val categories by viewModel.categories.collectAsState()
    val exchangeRates by analyticsViewModel.exchangeRates.collectAsState()
    val decimalPrecision by settingsViewModel.decimalPrecision.collectAsState()
    var selectedAccount by remember { mutableStateOf<Account?>(null) }
    var selectedCurrency by remember { mutableStateOf("") }
    
    var targetAccount by remember { mutableStateOf<Account?>(null) }
    var targetCurrency by remember { mutableStateOf("") }
    var exchangeRate by remember { mutableStateOf(BigDecimal.ONE) }


    val dbTemplates by templateViewModel.templates.collectAsState()
    remember(dbTemplates) {
        if (dbTemplates.isEmpty()) {
            listOf(
                Triple("Grocery Run", Icons.Rounded.ShoppingCart, 0),
                Triple("Salary Deposit", Icons.Rounded.Payments, 1),
                Triple("Rent Payment", Icons.Rounded.Home, 0),
                Triple("Coffee", Icons.Rounded.CoffeeMaker, 0)
            )
        } else {
            dbTemplates.map { Triple(it.name, Icons.Rounded.Category, it.transactionType) }
        }
    }
    var selectedTemplateName by remember { mutableStateOf(initialTemplate) }

    LaunchedEffect(initialTemplate, dbTemplates, accounts) {
        if (initialTemplate != null) {
            val template = dbTemplates.find { it.name == initialTemplate }
            if (template != null) {
                title = template.name
                category = template.category
                transactionType = template.transactionType
                if (template.amount.compareTo(BigDecimal.ZERO) > 0) amount = template.amount.toPlainString()
                selectedTemplateName = template.name
                template.note?.let { note = it }
                if (template.tags.isNotEmpty()) {
                    selectedTags = template.tags.split(",").filter { it.isNotBlank() }
                }
                template.accountId?.let { id ->
                    val acc = accounts.find { it.id == id }
                    if (acc != null) {
                        selectedAccount = acc
                        selectedCurrency = acc.currencyCode
                    }
                }
            } else {
                // Fallback for default templates if DB is empty or template not found
                when (initialTemplate) {
                    "Grocery Run" -> { category = "Food"; transactionType = 0; title = "Grocery Run" }
                    "Salary Deposit" -> { category = "Salary"; transactionType = 1; title = "Salary Deposit" }
                    "Rent Payment" -> { category = "Rent"; transactionType = 0; title = "Rent Payment" }
                    "Coffee" -> { category = "Food"; transactionType = 0; title = "Coffee" }
                }
                selectedTemplateName = initialTemplate
            }
        }
    }
    
    var showOCR by remember { mutableStateOf(false) }
    var showDatePicker by remember { mutableStateOf(false) }
    var showCategorySheet by remember { mutableStateOf(false) }
    var showAccountSheet by remember { mutableStateOf(false) }
    var showTargetAccountSheet by remember { mutableStateOf(false) }
    var showExchangeRateDialog by remember { mutableStateOf(false) }
    var isDetailDrawerOpen by remember { mutableStateOf(false) }
    var showTemplateSheet by remember { mutableStateOf(false) }
    var showLinkedTransactionSheet by remember { mutableStateOf(false) }
    var showLabelSheet by remember { mutableStateOf(false) }
    var showPaymentMethodSheet by remember { mutableStateOf(false) }
    var isNumpadVisible by remember { mutableStateOf(false) }

    LaunchedEffect(accounts) {
        if (selectedAccount == null && accounts.isNotEmpty()) {
            selectedAccount = accounts.find { it.id == accountId } 
                ?: accounts.find { it.name == "Aeon Prime Savings" } 
                ?: accounts.first()
            selectedCurrency = selectedAccount?.currencyCode ?: "USD"
        }
        if (targetAccount == null && accounts.size > 1) {
            targetAccount = accounts.find { it.id != (selectedAccount?.id ?: accountId) } ?: accounts.last()
            targetCurrency = targetAccount?.currencyCode ?: "USD"
        } else if (targetAccount == null && accounts.isNotEmpty()) {
            targetAccount = accounts.first()
            targetCurrency = targetAccount?.currencyCode ?: "USD"
        }
    }

    LaunchedEffect(isTransfer, selectedCurrency, targetCurrency, exchangeRates) {
        if (isTransfer) {
            val baseRates = exchangeRates
            if (baseRates.isNotEmpty()) {
                val sourceRateInUSD = baseRates[selectedCurrency] ?: BigDecimal.ONE
                val destRateInUSD = baseRates[targetCurrency] ?: BigDecimal.ONE
                if (sourceRateInUSD.signum() != 0) {
                    exchangeRate = destRateInUSD.divide(sourceRateInUSD, 8, RoundingMode.HALF_UP)
                }
            }
        }
    }

    rememberDatePickerState(initialSelectedDateMillis = selectedDateMillis)

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
                                val amountValue = amount.toBigDecimalOrNull() ?: BigDecimal.ZERO
                                selectedAccount?.id?.let { accountId ->
                                    if (isTransfer) {
                                        // 1. Withdrawal from source
                                        viewModel.addTransaction(
                                            title = title.ifBlank { "Transfer to ${targetAccount?.name}" },
                                            amount = amountValue,
                                            category = "Transfer",
                                            isIncome = false,
                                            accountId = accountId,
                                            currency = selectedCurrency,
                                            tags = selectedTags,
                                            paymentMethod = paymentMethod,
                                            timestamp = selectedDateMillis,
                                            payee = payee,
                                            note = note
                                        )

                                        // 2. Deposit to target
                                        targetAccount?.id?.let { targetId ->
                                            val targetAmount = amountValue.multiply(exchangeRate)
                                            viewModel.addTransaction(
                                                title = title.ifBlank { "Transfer from ${selectedAccount?.name}" },
                                                amount = targetAmount,
                                                category = "Transfer",
                                                isIncome = true,
                                                accountId = targetId,
                                                currency = targetCurrency,
                                                tags = selectedTags,
                                                paymentMethod = paymentMethod,
                                                timestamp = selectedDateMillis,
                                                payee = payee,
                                                note = note
                                            )
                                        }
                                    } else {
                                        viewModel.addTransaction(
                                            title = title.ifBlank { category },
                                            amount = amountValue,
                                            category = category,
                                            categoryId = selectedCategoryId,
                                            isIncome = isIncome,
                                            accountId = accountId,
                                            currency = selectedCurrency,
                                            tags = selectedTags,
                                            paymentMethod = paymentMethod,
                                            timestamp = selectedDateMillis,
                                            payee = payee,
                                            note = note,
                                            linkedTransactionId = linkedTransactionId
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
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Header Card: Type & Amount (Aeon Style)
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
                        // Transaction Type Row (Fixed at top)
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

                        // Amount & Currency Area (Fills remaining space and centers content)
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .clickable { isNumpadVisible = true },
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
                                        text = if (amount.isEmpty()) "0.".padEnd(decimalPrecision + 2, '0') else (if (isIncome) "+" else if (isTransfer) "" else "-") + amount,
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
                                    val amountValue = amount.toBigDecimalOrNull() ?: BigDecimal.ZERO
                                    val convertedAmount = amountValue.multiply(exchangeRate)

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
                                            " (1:${exchangeRate.setScale(3, RoundingMode.HALF_UP).toPlainString()})",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.padding(start = 8.dp)
                                        )
                                        Spacer(modifier = Modifier.width(12.dp))
                                        Text(
                                            text = convertedAmount.setScale(decimalPrecision, RoundingMode.HALF_UP).toPlainString(),
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
                    RecordFieldCard(
                        icon = Icons.Rounded.AutoAwesome,
                        label = "SELECT TEMPLATE",
                        value = selectedTemplateName ?: "None Selected",
                        trailingIcon = Icons.Rounded.KeyboardArrowDown,
                        onClick = { showTemplateSheet = true },
                        iconContainerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.5f),
                        isSlim = true
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
            }
        }

        // Numpad Overlay
        Numpad(
            amount = amount,
            onAmountChange = { amount = it },
            decimalPrecision = decimalPrecision,
            onDone = { isNumpadVisible = false },
            isVisible = isNumpadVisible,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
        )

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
                                        if (selectedTags.isEmpty()) "LABEL" else "${selectedTags.size} LABELS",
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
                                Text(formatDateTime(selectedDateMillis), style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                    Surface(
                        modifier = Modifier.weight(1f).height(80.dp),
                        shape = RoundedCornerShape(28.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f),
                        onClick = { showPaymentMethodSheet = true }
                    ) {
                        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.Center) {
                            Text("PAYMENT METHOD", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.outline)
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Rounded.Contactless, contentDescription = null, modifier = Modifier.size(16.dp), tint = Color(0xFF26C6DA))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(paymentMethod, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold)
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
        AeonDateTimePicker(
            initialDateTime = selectedDateMillis,
            onDateTimeSelected = { selectedDateMillis = it },
            onDismiss = { showDatePicker = false }
        )
    }

    if (showLabelSheet) {
        LabelPickerSheet(
            selectedTags = selectedTags,
            onTagsChanged = { selectedTags = it },
            onDismiss = { showLabelSheet = false },
            tagViewModel = tagViewModel
        )
    }

    if (showPaymentMethodSheet) {
        PaymentMethodPickerSheet(
            selectedMethod = paymentMethod,
            onMethodSelected = { paymentMethod = it },
            onDismiss = { showPaymentMethodSheet = false }
        )
    }

    if (showCategorySheet) {
        CategoryPickerSheet(
            categories = categories,
            selectedCategoryId = selectedCategoryId,
            onCategorySelected = {
                category = it.name
                selectedCategoryId = it.id
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
                AeonSectionHeader(title = "Quick Templates")
                Spacer(modifier = Modifier.height(24.dp))
                LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    if (dbTemplates.isEmpty()) {
                        val defaultTemplates = listOf(
                            Triple("Grocery Run", Icons.Rounded.ShoppingCart, 0),
                            Triple("Salary Deposit", Icons.Rounded.Payments, 1),
                            Triple("Rent Payment", Icons.Rounded.Home, 0),
                            Triple("Coffee", Icons.Rounded.CoffeeMaker, 0)
                        )
                        items(defaultTemplates) { (name, icon, type) ->
                            TemplateItemRow(name, icon) {
                                selectedTemplateName = name
                                title = name
                                transactionType = type
                                when (name) {
                                    "Grocery Run" -> category = "Food"
                                    "Salary Deposit" -> category = "Salary"
                                    "Rent Payment" -> category = "Rent"
                                    "Coffee" -> category = "Food"
                                }
                                showTemplateSheet = false
                            }
                        }
                    } else {
                        items(dbTemplates) { template ->
                            TemplateItemRow(template.name, Icons.Rounded.Category) {
                                selectedTemplateName = template.name
                                title = template.name
                                category = template.category
                                transactionType = template.transactionType
                                if (template.amount.compareTo(BigDecimal.ZERO) > 0) amount = template.amount.toPlainString()
                                template.note?.let { note = it }
                                template.accountId?.let { id ->
                                    val acc = accounts.find { it.id == id }
                                    if (acc != null) {
                                        selectedAccount = acc
                                        selectedCurrency = acc.currencyCode
                                    }
                                }
                                showTemplateSheet = false
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
                    items(transactions.sortedByDescending { it.timestamp }) { tx ->
                        AeonTransactionItem(
                            category = tx.category,
                            account = accounts.find { it.id == tx.accountId }?.name ?: "Unknown",
                            amount = formatCurrencyRight(if (tx.isIncome) tx.amount else tx.amount.negate(), tx.currency, precision = decimalPrecision),
                            balanceBefore = "", // Not needed in picker
                            balanceAfter = "",  // Not needed in picker
                            type = if (tx.isIncome) CashFlowType.INCOME else CashFlowType.EXPENSE,
                            timestamp = formatDateTime(tx.timestamp),
                            note = tx.note,
                            tags = tx.tags,
                            icon = categoryIcons[tx.category] ?: Icons.Rounded.Category,
                            precision = decimalPrecision,
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

@Composable
fun TemplateItemRow(name: String, icon: ImageVector, onClick: () -> Unit) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
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
