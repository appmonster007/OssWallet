package com.osscabinet.ossWallet.ui.screens

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.osscabinet.ossWallet.data.local.entity.CategoryEntity
import com.osscabinet.ossWallet.data.local.entity.TagEntity
import com.osscabinet.ossWallet.data.local.entity.TemplateEntity
import com.osscabinet.ossWallet.domain.model.Account
import com.osscabinet.ossWallet.domain.model.CashFlowType
import com.osscabinet.ossWallet.domain.model.Transaction
import com.osscabinet.ossWallet.ui.components.*
import com.osscabinet.ossWallet.ui.components.transactions.AeonTransactionItem
import com.osscabinet.ossWallet.ui.theme.OssWalletTheme
import com.osscabinet.ossWallet.ui.util.formatCurrencyRight
import com.osscabinet.ossWallet.ui.viewmodel.*
import java.math.BigDecimal
import java.math.RoundingMode

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TransactionEntryScreen(
    transactionId: Long? = null,
    accountId: String? = null,
    initialType: Int = 0,
    initialTemplate: String? = null,
    viewModel: TransactionViewModel,
    accountViewModel: AccountViewModel,
    analyticsViewModel: AnalyticsViewModel,
    templateViewModel: TemplateViewModel,
    tagViewModel: TagViewModel,
    settingsViewModel: SettingsViewModel,
    onBackClick: () -> Unit
) {
    val transactions by viewModel.transactions.collectAsState()
    val categories by viewModel.categories.collectAsState()
    val accounts by accountViewModel.accounts.collectAsState()
    val exchangeRates by analyticsViewModel.exchangeRates.collectAsState()
    val decimalPrecision by settingsViewModel.decimalPrecision.collectAsState()
    val dbTemplates by templateViewModel.templates.collectAsState()
    val allTags by tagViewModel.tags.collectAsState()

    TransactionEntryContent(
        transactionId = transactionId,
        accountId = accountId,
        initialType = initialType,
        initialTemplate = initialTemplate,
        transactions = transactions,
        categories = categories,
        accounts = accounts,
        exchangeRates = exchangeRates,
        decimalPrecision = decimalPrecision,
        dbTemplates = dbTemplates,
        allTags = allTags,
        onAddTag = { tagViewModel.addTag(it) },
        onSave = { title, amountValue, category, categoryId, isIncome, accId, currency, tags, paymentMethod, timestamp, payee, note, linkedId, isTransfer, targetAcc, targetCurrency, rate, coords, warrantyExp ->
            if (transactionId != null && transactionId != -1L) {
                val existingTransaction = transactions.find { it.id == transactionId }
                if (existingTransaction != null) {
                    val wasTransfer = existingTransaction.category == "Transfer"
                    
                    if (wasTransfer && !isTransfer) {
                        // Changed from Transfer to Income/Expense
                        // Delete linked transaction if it exists
                        existingTransaction.linkedTransactionId?.let { viewModel.deleteTransaction(it) }
                        
                        // Update current transaction to normal income/expense
                        viewModel.updateTransaction(
                            oldTransaction = existingTransaction,
                            title = title.ifBlank { category },
                            amount = amountValue,
                            category = category,
                            isIncome = isIncome,
                            accountId = accId,
                            currency = currency,
                            categoryId = categoryId,
                            tags = tags,
                            timestamp = timestamp,
                            payee = payee,
                            note = note,
                            linkedTransactionId = null,
                            latitude = coords?.first,
                            longitude = coords?.second,
                            warrantyExpiration = warrantyExp
                        )
                    } else if (!wasTransfer && isTransfer) {
                        // Changed from Income/Expense to Transfer
                        // Create target transaction first to get its ID
                        targetAcc?.id?.let { targetId ->
                            val targetAmount = amountValue.multiply(rate)
                            viewModel.addTransaction(
                                title = title.ifBlank { "Transfer from ${accounts.find { it.id == accId }?.name}" },
                                amount = targetAmount,
                                category = "Transfer",
                                isIncome = true,
                                accountId = targetId,
                                currency = targetCurrency,
                                tags = tags,
                                paymentMethod = paymentMethod,
                                timestamp = timestamp,
                                payee = payee,
                                note = note,
                                linkedTransactionId = transactionId,
                                latitude = coords?.first,
                                longitude = coords?.second,
                                warrantyExpiration = warrantyExp,
                                onComplete = { newLinkedId ->
                                    // Update current transaction with link to new target transaction
                                    viewModel.updateTransaction(
                                        oldTransaction = existingTransaction,
                                        title = title.ifBlank { "Transfer to ${targetAcc.name}" },
                                        amount = amountValue,
                                        category = "Transfer",
                                        isIncome = false,
                                        accountId = accId,
                                        currency = currency,
                                        categoryId = null,
                                        tags = tags,
                                        timestamp = timestamp,
                                        payee = payee,
                                        note = note,
                                        linkedTransactionId = newLinkedId,
                                        latitude = coords?.first,
                                        longitude = coords?.second,
                                        warrantyExpiration = warrantyExp
                                    )
                                }
                            )
                        }
                    } else {
                        // Standard update (no type change between Transfer and others)
                        viewModel.updateTransaction(
                            oldTransaction = existingTransaction,
                            title = title.ifBlank { if (isTransfer) "Transfer" else category },
                            amount = amountValue,
                            category = if (isTransfer) "Transfer" else category,
                            isIncome = isIncome,
                            accountId = accId,
                            currency = currency,
                            categoryId = if (isTransfer) null else categoryId,
                            tags = tags,
                            timestamp = timestamp,
                            payee = payee,
                            note = note,
                            linkedTransactionId = linkedId,
                            latitude = coords?.first,
                            longitude = coords?.second,
                            warrantyExpiration = warrantyExp
                        )
                        
                        if (isTransfer && linkedId != null) {
                            val linkedTx = transactions.find { it.id == linkedId }
                            if (linkedTx != null && targetAcc != null) {
                                val targetAmount = amountValue.multiply(rate)
                                viewModel.updateTransaction(
                                    oldTransaction = linkedTx,
                                    title = linkedTx.title,
                                    amount = targetAmount,
                                    category = "Transfer",
                                    isIncome = true,
                                    accountId = targetAcc.id,
                                    currency = targetCurrency,
                                    tags = tags,
                                    timestamp = timestamp,
                                    payee = payee,
                                    note = note,
                                    linkedTransactionId = transactionId,
                                    latitude = coords?.first,
                                    longitude = coords?.second,
                                    warrantyExpiration = warrantyExp
                                )
                            }
                        }
                    }
                }
            } else {
                if (isTransfer) {
                    viewModel.addTransaction(
                        title = title.ifBlank { "Transfer to ${targetAcc?.name}" },
                        amount = amountValue,
                        category = "Transfer",
                        isIncome = false,
                        accountId = accId,
                        currency = currency,
                        tags = tags,
                        paymentMethod = paymentMethod,
                        timestamp = timestamp,
                        payee = payee,
                        note = note,
                        latitude = coords?.first,
                        longitude = coords?.second,
                        warrantyExpiration = warrantyExp
                    )

                    targetAcc?.id?.let { targetId ->
                        val targetAmount = amountValue.multiply(rate)
                        viewModel.addTransaction(
                            title = title.ifBlank { "Transfer from ${accounts.find { it.id == accId }?.name}" },
                            amount = targetAmount,
                            category = "Transfer",
                            isIncome = true,
                            accountId = targetId,
                            currency = targetCurrency,
                            tags = tags,
                            paymentMethod = paymentMethod,
                            timestamp = timestamp,
                            payee = payee,
                            note = note,
                            linkedTransactionId = null,
                            latitude = coords?.first,
                            longitude = coords?.second,
                            warrantyExpiration = warrantyExp
                        )
                    }
                } else {
                    viewModel.addTransaction(
                        title = title.ifBlank { category },
                        amount = amountValue,
                        category = category,
                        categoryId = categoryId,
                        isIncome = isIncome,
                        accountId = accId,
                        currency = currency,
                        tags = tags,
                        paymentMethod = paymentMethod,
                        timestamp = timestamp,
                        payee = payee,
                        note = note,
                        linkedTransactionId = linkedId,
                        latitude = coords?.first,
                        longitude = coords?.second,
                        warrantyExpiration = warrantyExp
                    )
                }
            }
            onBackClick()
        },
        onDelete = { id ->
            viewModel.deleteTransaction(id)
            onBackClick()
        },
        onBackClick = onBackClick
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TransactionEntryContent(
    transactionId: Long? = null,
    accountId: String? = null,
    initialType: Int = 0,
    initialTemplate: String? = null,
    transactions: List<Transaction> = emptyList(),
    categories: List<CategoryEntity> = emptyList(),
    accounts: List<Account> = emptyList(),
    exchangeRates: Map<String, BigDecimal> = emptyMap(),
    decimalPrecision: Int = 2,
    dbTemplates: List<TemplateEntity> = emptyList(),
    allTags: List<TagEntity> = emptyList(),
    onAddTag: (String) -> Unit = {},
    onSave: (String, BigDecimal, String, Long?, Boolean, String, String, List<String>, String, Long, String, String, Long?, Boolean, Account?, String, BigDecimal, Pair<Double, Double>?, Long?) -> Unit,
    onDelete: (Long) -> Unit,
    onBackClick: () -> Unit
) {
    val isEditMode = transactionId != null && transactionId != -1L
    val existingTransaction = remember(transactionId, transactions) {
        if (transactionId != null && transactionId != -1L) {
            transactions.find { it.id == transactionId }
        } else null
    }

    if (isEditMode && existingTransaction == null) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
        return
    }

    var title by remember { mutableStateOf("") }
    var amount by remember { mutableStateOf("") }
    var category by remember { mutableStateOf("Shopping") }
    var selectedCategoryId by remember { mutableStateOf<Long?>(null) }
    var selectedTags by remember { mutableStateOf<List<String>>(emptyList()) }
    var paymentMethod by remember { mutableStateOf("Cash") }
    var transactionType by remember { mutableIntStateOf(initialType) } // 0: Expense, 1: Income, 2: Transfer
    var selectedDateMillis by remember { mutableLongStateOf(System.currentTimeMillis()) }
    var payee by remember { mutableStateOf("") }
    var note by remember { mutableStateOf("") }
    var isWarrantyEnabled by remember { mutableStateOf(false) }
    var warrantyExpirationMillis by remember { mutableLongStateOf(System.currentTimeMillis() + 31536000000L) } // Default +1 year
    var showWarrantyDatePicker by remember { mutableStateOf(false) }
    var locationCoords by remember { mutableStateOf<Pair<Double, Double>?>(null) }
    var linkedTransactionId by remember(existingTransaction) { mutableStateOf(existingTransaction?.linkedTransactionId) }
    val linkedTx = remember(linkedTransactionId, transactions) {
        if (linkedTransactionId != null) {
            transactions.find { it.id == linkedTransactionId }
        } else null
    }
    
    var selectedAccount by remember { mutableStateOf<Account?>(null) }
    var selectedCurrency by remember { mutableStateOf("") }
    
    var targetAccount by remember { mutableStateOf<Account?>(null) }
    var targetCurrency by remember { mutableStateOf("") }
    var exchangeRate by remember { mutableStateOf(BigDecimal.ONE) }
    var selectedTemplateName by remember { mutableStateOf(initialTemplate) }
    var showLocationSheet by remember { mutableStateOf(false) }

    // Initialize state from existing transaction or initial parameters
    LaunchedEffect(existingTransaction, accounts) {
        if (isEditMode && existingTransaction != null) {
            title = existingTransaction.title
            amount = existingTransaction.amount.toPlainString()
            category = existingTransaction.category
            selectedCategoryId = existingTransaction.categoryId
            selectedTags = existingTransaction.tags
            paymentMethod = existingTransaction.paymentMethod
            transactionType = when {
                existingTransaction.category == "Transfer" -> 2
                existingTransaction.isIncome -> 1
                else -> 0
            }
            selectedDateMillis = existingTransaction.timestamp
            payee = existingTransaction.payee
            note = existingTransaction.note
            // Already handled by separate LaunchedEffect for better reactivity
            // linkedTransactionId = existingTransaction.linkedTransactionId
            
            selectedAccount = accounts.find { it.id == existingTransaction.accountId }
            selectedCurrency = existingTransaction.currency

            if (transactionType == 2 && existingTransaction.linkedTransactionId != null) {
                val linkedTx = transactions.find { it.id == existingTransaction.linkedTransactionId }
                if (linkedTx != null) {
                    targetAccount = accounts.find { it.id == linkedTx.accountId }
                    targetCurrency = linkedTx.currency
                    if (existingTransaction.amount.signum() != 0) {
                        exchangeRate = linkedTx.amount.divide(existingTransaction.amount, 8, RoundingMode.HALF_UP).abs()
                    }
                }
            }
        } else {
            if (initialTemplate != null) {
                val template = dbTemplates.find { it.name == initialTemplate }
                if (template != null) {
                    title = template.name
                    category = template.category
                    transactionType = template.transactionType
                    if (template.amount > BigDecimal.ZERO) amount = template.amount.toPlainString()
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
                    when (initialTemplate) {
                        "Grocery Run" -> { category = "Food"; transactionType = 0; title = "Grocery Run" }
                        "Salary Deposit" -> { category = "Salary"; transactionType = 1; title = "Salary Deposit" }
                        "Rent Payment" -> { category = "Rent"; transactionType = 0; title = "Rent Payment" }
                        "Coffee" -> { category = "Food"; transactionType = 0; title = "Coffee" }
                    }
                    selectedTemplateName = initialTemplate
                }
            }
            
            if (selectedAccount == null && accounts.isNotEmpty()) {
                selectedAccount = accounts.find { it.id == accountId } 
                    ?: accounts.find { it.name == "Aeon Prime Savings" } 
                    ?: accounts.first()
                selectedCurrency = selectedAccount?.currencyCode ?: "USD"
            }
            
            if (transactionType == 2 && targetAccount == null && accounts.size > 1) {
                targetAccount = accounts.find { it.id != (selectedAccount?.id ?: accountId) } ?: accounts.last()
                targetCurrency = targetAccount?.currencyCode ?: "USD"
            }
        }
    }

    LaunchedEffect(transactionType == 2, selectedCurrency, targetCurrency, exchangeRates) {
        if (transactionType == 2 && !isEditMode) {
            if (exchangeRates.isNotEmpty()) {
                val sourceRateInUSD = exchangeRates[selectedCurrency] ?: BigDecimal.ONE
                val destRateInUSD = exchangeRates[targetCurrency] ?: BigDecimal.ONE
                if (sourceRateInUSD.signum() != 0) {
                    exchangeRate = destRateInUSD.divide(sourceRateInUSD, 8, RoundingMode.HALF_UP)
                }
            }
        }
    }

    val isIncome = transactionType == 1
    val isTransfer = transactionType == 2

    var showDatePicker by remember { mutableStateOf(false) }
    var showCategorySheet by remember { mutableStateOf(false) }
    var showAccountSheet by remember { mutableStateOf(false) }
    var showTargetAccountSheet by remember { mutableStateOf(false) }
    var showExchangeRateDialog by remember { mutableStateOf(false) }
    var showTemplateSheet by remember { mutableStateOf(false) }
    var showLinkedTransactionSheet by remember { mutableStateOf(false) }
    var showLabelSheet by remember { mutableStateOf(false) }
    var showPaymentMethodSheet by remember { mutableStateOf(false) }
    var isNumpadVisible by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text(if (isEditMode) "Edit Transaction" else "New Transaction") },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    if (isEditMode) {
                        IconButton(onClick = { onDelete(transactionId) }) {
                            Icon(Icons.Rounded.Delete, contentDescription = "Delete", tint = MaterialTheme.colorScheme.error)
                        }
                    }
                    IconButton(
                        onClick = {
                            val amountValue = amount.toBigDecimalOrNull() ?: BigDecimal.ZERO
                            onSave(
                                title,
                                amountValue,
                                category,
                                selectedCategoryId,
                                isIncome,
                                selectedAccount?.id ?: "",
                                selectedCurrency,
                                selectedTags,
                                paymentMethod,
                                selectedDateMillis,
                                payee,
                                note,
                                linkedTransactionId,
                                isTransfer,
                                targetAccount,
                                targetCurrency,
                                exchangeRate,
                                locationCoords,
                                if (isWarrantyEnabled) warrantyExpirationMillis else null
                            )
                        }
                    ) {
                        Icon(Icons.Rounded.Done, contentDescription = "Save")
                    }
                }
            )
        }
    ) { innerPadding ->
        Box(modifier = Modifier.fillMaxSize().padding(innerPadding)) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Header Card: Type & Amount
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(240.dp),
                    shape = RoundedCornerShape(48.dp),
                    color = MaterialTheme.colorScheme.surfaceContainerHigh,
                    tonalElevation = 2.dp
                ) {
                    Column(
                        modifier = Modifier.padding(20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        // Transaction Type Row - Expressive Design
                        Box(
                            modifier = Modifier
                                .height(40.dp)
                                .clip(RoundedCornerShape(32.dp))
                                .background(MaterialTheme.colorScheme.surfaceContainerLowest)
                                .padding(6.dp)
                        ) {
                            val types = listOf(
                                Icons.Rounded.Remove to "Expense",
                                Icons.Rounded.Add to "Income",
                                Icons.Rounded.SwapHoriz to "Transfer"
                            )

                            val expenseColor = lerp(MaterialTheme.colorScheme.primary, Color(0xFFB71C1C), 0.6f)
                            val incomeColor = lerp(MaterialTheme.colorScheme.secondary, Color(0xFF2E7D32), 0.6f)
                            val transferColor = MaterialTheme.colorScheme.tertiary

                            val indicatorColor by animateColorAsState(
                                targetValue = when(transactionType) {
                                    0 -> expenseColor
                                    1 -> incomeColor
                                    else -> transferColor
                                },
                                animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
                                label = "indicatorColor"
                            )

                            val indicatorOffset by animateDpAsState(
                                targetValue = when(transactionType) {
                                    0 -> 0.dp
                                    1 -> 72.dp
                                    else -> 144.dp
                                },
                                animationSpec = spring(
                                    dampingRatio = Spring.DampingRatioLowBouncy,
                                    stiffness = Spring.StiffnessLow
                                ),
                                label = "indicatorOffset"
                            )

                            Box(
                                modifier = Modifier
                                    .offset(x = indicatorOffset)
                                    .width(72.dp)
                                    .fillMaxHeight()
                                    .clip(CircleShape)
                                    .background(indicatorColor)
                            )

                            Row(
                                modifier = Modifier.fillMaxHeight(),
                                horizontalArrangement = Arrangement.spacedBy(0.dp)
                            ) {
                                types.forEachIndexed { index, pair ->
                                    val isSelected = transactionType == index
                                    val scale by animateFloatAsState(
                                        targetValue = if (isSelected) 1.2f else 1.0f,
                                        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
                                        label = "iconScale"
                                    )

                                    Box(
                                        modifier = Modifier
                                            .width(72.dp)
                                            .fillMaxHeight()
                                            .clip(CircleShape)
                                            .clickable(
                                                interactionSource = remember { MutableInteractionSource() },
                                                indication = null
                                            ) { transactionType = index },
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            pair.first,
                                            contentDescription = pair.second,
                                            tint = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                                            modifier = Modifier
                                                .size(26.dp)
                                                .graphicsLayer(scaleX = scale, scaleY = scale)
                                        )
                                    }
                                }
                            }
                        }

                        // Amount & Currency Area
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .clickable(
                                    interactionSource = remember { MutableInteractionSource() },
                                    indication = null
                                ) { isNumpadVisible = true },
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

                                    val prefix = when {
                                        isIncome -> "+"
                                        isTransfer -> ""
                                        else -> "-"
                                    }
                                    val displayAmount = amount.ifEmpty { "0.00" }
                                    Text(
                                        text = "$prefix$displayAmount",
                                        style = TextStyle(
                                            fontSize = 72.sp,
                                            fontWeight = FontWeight.Bold,
                                            textAlign = TextAlign.Center,
                                            color = when {
                                                isIncome -> lerp(MaterialTheme.colorScheme.secondary, Color(0xFF2E7D32), 0.5f)
                                                isTransfer -> MaterialTheme.colorScheme.onSurface
                                                else -> lerp(MaterialTheme.colorScheme.primary, Color(0xFFB71C1C), 0.5f)
                                            }
                                        ),
                                        modifier = Modifier.weight(1f, fill = false)
                                    )

                                    Spacer(modifier = Modifier.width(16.dp))

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
                                            text = "+${convertedAmount.setScale(decimalPrecision, RoundingMode.HALF_UP).toPlainString()}",
                                            style = TextStyle(
                                                fontSize = 32.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = lerp(MaterialTheme.colorScheme.secondary, Color(0xFF2E7D32), 0.5f)
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
                    if (!isEditMode) {
                        RecordFieldCard(
                            icon = Icons.Rounded.AutoAwesome,
                            label = "SELECT TEMPLATE",
                            value = selectedTemplateName ?: "None Selected",
                            trailingIcon = Icons.Rounded.KeyboardArrowDown,
                            onClick = { showTemplateSheet = true },
                            iconContainerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.5f),
                            isSlim = true
                        )
                    }

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
                            val displayCategory = if (isEditMode) {
                                categories.find { it.id == selectedCategoryId }?.name ?: category
                            } else category
                            
                            RecordFieldCard(
                                icon = categoryIcons[displayCategory] ?: Icons.Rounded.Category,
                                label = "CATEGORY",
                                value = displayCategory,
                                onClick = { showCategorySheet = true },
                                modifier = Modifier.weight(1f),
                                iconContainerColor = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.5f),
                                isSlim = true
                            )
                        }
                    }
                }

                // Metadata Section
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(28.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f)
                ) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                        // Note - First item
                        EditorialInput(
                            value = note,
                            onValueChange = { note = it },
                            label = "NOTE",
                            icon = Icons.AutoMirrored.Rounded.Notes
                        )

                        // Payee - Second item
                        EditorialInput(
                            value = payee,
                            onValueChange = { payee = it },
                            label = "PAYEE",
                            icon = Icons.Rounded.Person
                        )
                        
                        // Linked Transaction Field - Last item
                        val currentLinkedTx = linkedTx
                        if (currentLinkedTx != null) {
                            Surface(
                                shape = RoundedCornerShape(28.dp),
                                color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f),
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    val linkedCategory = categories.find { it.name == currentLinkedTx.category }
                                    Surface(
                                        shape = CircleShape,
                                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.2f),
                                        modifier = Modifier.size(48.dp)
                                    ) {
                                        Icon(
                                            categoryIcons[linkedCategory?.iconName] ?: categoryIcons[currentLinkedTx.category] ?: Icons.Rounded.Category,
                                            contentDescription = null,
                                            modifier = Modifier.padding(12.dp).size(24.dp),
                                            tint = MaterialTheme.colorScheme.primary
                                        )
                                    }
                                    Spacer(modifier = Modifier.width(16.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        val displayTitle = currentLinkedTx.note.ifBlank { currentLinkedTx.category }
                                        val amountSign = if (currentLinkedTx.isIncome) "+" else "-"
                                        val formattedAmount = "$amountSign${formatCurrencyRight(currentLinkedTx.amount.abs(), currentLinkedTx.currency, precision = decimalPrecision)}"
                                        
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(
                                                text = displayTitle,
                                                style = MaterialTheme.typography.bodyLarge,
                                                fontWeight = FontWeight.Bold,
                                                color = MaterialTheme.colorScheme.onPrimaryContainer,
                                                modifier = Modifier.weight(1f),
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                            Text(
                                                text = formattedAmount,
                                                style = MaterialTheme.typography.bodyLarge,
                                                fontWeight = FontWeight.Bold,
                                                color = MaterialTheme.colorScheme.onPrimaryContainer
                                            )
                                        }
                                        
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(
                                                text = if (currentLinkedTx.note.isNotBlank()) currentLinkedTx.category else "",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = MaterialTheme.colorScheme.primary,
                                                modifier = Modifier.weight(1f),
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                            Text(
                                                text = formatDateTime(currentLinkedTx.timestamp),
                                                style = MaterialTheme.typography.labelSmall,
                                                color = MaterialTheme.colorScheme.primary
                                            )
                                        }
                                    }
                                    
                                    Spacer(modifier = Modifier.width(8.dp))
                                    IconButton(
                                        onClick = { linkedTransactionId = null },
                                        modifier = Modifier.size(32.dp)
                                    ) {
                                        Icon(
                                            Icons.Rounded.Close,
                                            contentDescription = "Unlink",
                                            modifier = Modifier.size(20.dp),
                                            tint = MaterialTheme.colorScheme.primary
                                        )
                                    }
                                }
                            }
                        } else {
                            RecordFieldCard(
                                icon = Icons.Rounded.Link,
                                label = "LINK TRANSACTION",
                                value = "None Linked",
                                onClick = { showLinkedTransactionSheet = true },
                                trailingIcon = Icons.Rounded.KeyboardArrowDown,
                                iconContainerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
                            )
                        }

                        @OptIn(ExperimentalLayoutApi::class)
                        FlowRow(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.3f),
                                onClick = { /* Recurring toggle */ }
                            ) {
                                Text("RECURRING", modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp), style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                            }

                            selectedTags.forEach { tag ->
                                Surface(
                                    shape = RoundedCornerShape(12.dp),
                                    color = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.4f),
                                    onClick = { showLabelSheet = true }
                                ) {
                                    Text(
                                        tag.uppercase(),
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onTertiaryContainer
                                    )
                                }
                            }

                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = MaterialTheme.colorScheme.surfaceVariant,
                                onClick = { showLabelSheet = true }
                            ) {
                                Row(modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Rounded.Add, contentDescription = null, modifier = Modifier.size(12.dp), tint = MaterialTheme.colorScheme.outline)
                                    Text(
                                        " LABEL",
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    }
                }

                // Occurrence & Payment Method
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    RecordFieldCard(
                        icon = Icons.Rounded.CalendarToday,
                        label = "OCCURRENCE",
                        value = formatDateTime(selectedDateMillis),
                        onClick = { showDatePicker = true },
                        modifier = Modifier.weight(1f),
                        iconContainerColor = Color(0xFF42A5F5).copy(alpha = 0.2f),
                        isSlim = true
                    )

                    val methods = listOf(
                        "Cash" to Icons.Rounded.Payments,
                        "Credit Card" to Icons.Rounded.CreditCard,
                        "Debit Card" to Icons.Rounded.CreditCard,
                        "Bank Transfer" to Icons.Rounded.AccountBalance,
                        "Digital Wallet" to Icons.Rounded.Wallet,
                        "Mobile Payment" to Icons.Rounded.Contactless,
                        "Check" to Icons.Rounded.Description,
                        "Cryptocurrency" to Icons.Rounded.CurrencyBitcoin,
                        "Other" to Icons.Rounded.MoreHoriz
                    )
                    val currentMethodIcon = methods.find { it.first == paymentMethod }?.second ?: Icons.Rounded.Contactless

                    RecordFieldCard(
                        icon = currentMethodIcon,
                        label = "PAYMENT METHOD",
                        value = paymentMethod,
                        onClick = { showPaymentMethodSheet = true },
                        modifier = Modifier.weight(1f),
                        iconContainerColor = Color(0xFF26C6DA).copy(alpha = 0.2f),
                        isSlim = true
                    )
                }

                // Map/Location Card
                Surface(
                    modifier = Modifier.fillMaxWidth().height(160.dp),
                    shape = RoundedCornerShape(28.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.1f),
                    onClick = { showLocationSheet = true }
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
                                Text(
                                    if (locationCoords == null) "Select Location" else "${locationCoords?.first?.format(4)}, ${locationCoords?.second?.format(4)}",
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Bold
                                )
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
                    Column {
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
                        
                        if (isWarrantyEnabled) {
                            HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp), color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { showWarrantyDatePicker = true }
                                    .padding(16.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Rounded.EventAvailable, contentDescription = null, modifier = Modifier.size(20.dp), tint = MaterialTheme.colorScheme.primary)
                                Spacer(modifier = Modifier.width(12.dp))
                                Column {
                                    Text("Expiration Date", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.outline)
                                    Text(formatDateTime(warrantyExpirationMillis), style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                                }
                                Spacer(modifier = Modifier.weight(1f))
                                Icon(Icons.Rounded.ChevronRight, contentDescription = null, tint = MaterialTheme.colorScheme.outline)
                            }
                        }
                    }
                }
                
                Spacer(modifier = Modifier.height(24.dp))
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
        }

        if (showLocationSheet) {
            LocationPickerSheet(
                currentCoords = locationCoords,
                onLocationSelected = { locationCoords = it },
                onDismiss = { showLocationSheet = false }
            )
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

        if (showWarrantyDatePicker) {
            AeonDateTimePicker(
                initialDateTime = warrantyExpirationMillis,
                onDateTimeSelected = { warrantyExpirationMillis = it },
                onDismiss = { showWarrantyDatePicker = false }
            )
        }

        if (showLabelSheet) {
            LabelPickerSheet(
                selectedTags = selectedTags,
                onTagsChanged = { selectedTags = it },
                onDismiss = { showLabelSheet = false },
                allTags = allTags,
                onAddTag = onAddTag
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

        if (showTemplateSheet) {
            ModalBottomSheet(
                onDismissRequest = { showTemplateSheet = false },
                containerColor = MaterialTheme.colorScheme.surface,
                shape = RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp)
            ) {
                Column(modifier = Modifier.padding(24.dp).padding(bottom = 32.dp)) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        AeonSectionHeader(title = "Quick Templates")
                        IconButton(onClick = { /* Navigate to Create Template */ }) {
                            Icon(Icons.Rounded.Add, contentDescription = "Add Template", tint = MaterialTheme.colorScheme.primary)
                        }
                    }
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
                                    if (template.amount > BigDecimal.ZERO) amount = template.amount.toPlainString()
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
                        items(transactions.filter { it.id != transactionId }.sortedByDescending { it.timestamp }) { tx ->
                            AeonTransactionItem(
                                category = tx.category,
                                account = accounts.find { it.id == tx.accountId }?.name ?: "Unknown",
                                amount = formatCurrencyRight(tx.amount.abs(), tx.currency, precision = decimalPrecision),
                                balanceBefore = "",
                                balanceAfter = "",
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
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun LocationPickerSheet(
    currentCoords: Pair<Double, Double>?,
    onLocationSelected: (Pair<Double, Double>) -> Unit,
    onDismiss: () -> Unit
) {
    var searchQuery by remember { mutableStateOf("") }
    var mapCenter by remember { mutableStateOf(currentCoords ?: (37.7749 to -122.4194)) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp)
    ) {
        Column(modifier = Modifier.padding(24.dp).padding(bottom = 32.dp).fillMaxHeight(0.8f)) {
            AeonSectionHeader(title = "Select Location")
            Spacer(modifier = Modifier.height(16.dp))
            
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text("Search for address or place") },
                leadingIcon = { Icon(Icons.Rounded.Search, contentDescription = null) },
                shape = RoundedCornerShape(16.dp),
                singleLine = true
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Map Placeholder
            Surface(
                modifier = Modifier.weight(1f).fillMaxWidth(),
                shape = RoundedCornerShape(24.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    // Map background simulation
                    Image(
                        painter = painterResource(id = android.R.drawable.ic_menu_mapmode),
                        contentDescription = null,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop,
                        alpha = 0.2f
                    )
                    
                    // The Center Pin
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            Icons.Rounded.Place,
                            contentDescription = "Pin",
                            modifier = Modifier.size(40.dp),
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Surface(
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.2f),
                            modifier = Modifier.size(12.dp, 4.dp)
                        ) {}
                    }
                    
                    Text(
                        "Drag map to position pin",
                        modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 16.dp),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.outline
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Rounded.MyLocation, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text("COORDINATES", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.outline)
                    Text(
                        "${mapCenter.first.format(6)}, ${mapCenter.second.format(6)}",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(24.dp))
            
            Button(
                onClick = {
                    onLocationSelected(mapCenter)
                    onDismiss()
                },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp)
            ) {
                Text("Confirm Location")
            }
        }
    }
}

@Composable
private fun TemplateItemRow(
    name: String,
    icon: ImageVector,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(24.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f)
    ) {
        Row(
            modifier = Modifier.padding(20.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                modifier = Modifier.size(40.dp),
                shape = CircleShape,
                color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(icon, contentDescription = null, modifier = Modifier.size(20.dp))
                }
            }
            Spacer(modifier = Modifier.width(16.dp))
            Text(
                name,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.weight(1f))
            Icon(
                Icons.Rounded.ChevronRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.outline
            )
        }
    }
}

@Composable
fun TransactionMetadataPreview() {
    OssWalletTheme {
        Surface(
            modifier = Modifier.padding(16.dp),
            color = MaterialTheme.colorScheme.background
        ) {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(28.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f)
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    // Note first
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.AutoMirrored.Rounded.Notes, contentDescription = null, modifier = Modifier.size(20.dp), tint = MaterialTheme.colorScheme.outline)
                        Spacer(modifier = Modifier.width(12.dp))
                        Text("Grocery shopping for the week", style = MaterialTheme.typography.bodyLarge)
                    }

                    // Payee second
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Rounded.Person, contentDescription = null, modifier = Modifier.size(20.dp), tint = MaterialTheme.colorScheme.outline)
                        Spacer(modifier = Modifier.width(12.dp))
                        Text("Whole Foods Market", style = MaterialTheme.typography.bodyLarge)
                    }
                    
                    // Link last
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Rounded.Link, contentDescription = null, modifier = Modifier.size(20.dp), tint = MaterialTheme.colorScheme.outline)
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text("Monthly Rent", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onPrimaryContainer, fontWeight = FontWeight.Bold)
                            Text("Housing • -$1,200.00 • Oct 12, 2023 14:30", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
                        }
                    }

                    @OptIn(ExperimentalLayoutApi::class)
                    FlowRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Surface(shape = RoundedCornerShape(12.dp), color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.3f)) {
                            Text("RECURRING", modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp), style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                        }
                        Surface(shape = RoundedCornerShape(12.dp), color = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.4f)) {
                            Text("FOOD", modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp), style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                        }
                        Surface(shape = RoundedCornerShape(12.dp), color = MaterialTheme.colorScheme.surfaceVariant) {
                            Row(modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Rounded.Add, contentDescription = null, modifier = Modifier.size(12.dp))
                                Text(" LABEL", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun TransactionEntryWithLongLinkPreview() {
    val sampleAccounts = listOf(
        Account("1", "Aeon Prime Savings", BigDecimal("1250.50"), BigDecimal("1000.00"), "USD", "Bank", "#FF0000", "account_balance"),
    )

    val sampleCategories = listOf(
        CategoryEntity(1, "Food", "Restaurant", 0xFF4CAF50.toInt()),
    )

    val sampleTransactions = listOf(
        Transaction(
            id = 100L,
            title = "Very long title for the linked transaction that might push the close button off screen",
            amount = BigDecimal("1234.56"),
            category = "Extremely Long Category Name For Testing Purposes",
            isIncome = false,
            accountId = "1",
            currency = "USD",
            timestamp = System.currentTimeMillis(),
            note = "Very long note that might push the close button off screen if not handled properly in the UI layout"
        ),
        Transaction(
            id = 1L,
            title = "Main Transaction",
            amount = BigDecimal("100.00"),
            category = "Food",
            isIncome = false,
            accountId = "1",
            currency = "USD",
            timestamp = System.currentTimeMillis(),
            linkedTransactionId = 100L
        )
    )

    OssWalletTheme {
        TransactionEntryContent(
            transactionId = 1L,
            accounts = sampleAccounts,
            categories = sampleCategories,
            transactions = sampleTransactions,
            onSave = { _, _, _, _, _, _, _, _, _, _, _, _, _, _, _, _, _, _, _ -> },
            onDelete = {},
            onBackClick = {}
        )
    }
}

fun Double.format(digits: Int) = "%.${digits}f".format(this)

@Preview(showBackground = true)
@Composable
fun TransactionEntryPreview() {
    val sampleAccounts = listOf(
        Account("1", "Aeon Prime Savings", BigDecimal("1250.50"), BigDecimal("1000.00"), "USD", "Bank", "#FF0000", "account_balance"),
        Account("2", "Cash Wallet", BigDecimal("45.00"), BigDecimal("50.00"), "USD", "Cash", "#00FF00", "wallet")
    )

    val sampleCategories = listOf(
        CategoryEntity(1, "Food", "Restaurant", 0xFF4CAF50.toInt()),
        CategoryEntity(2, "Transport", "DirectionsCar", 0xFF2196F3.toInt()),
        CategoryEntity(3, "Shopping", "ShoppingBag", 0xFFFFC107.toInt())
    )

    val sampleTags = listOf(
        TagEntity(1, "Essential", 0xFFE91E63.toInt()),
        TagEntity(2, "Work", 0xFF2196F3.toInt()),
        TagEntity(3, "Vacation", 0xFF009688.toInt())
    )
    
    OssWalletTheme {
        TransactionEntryContent(
            accounts = sampleAccounts,
            categories = sampleCategories,
            allTags = sampleTags,
            onSave = { _, _, _, _, _, _, _, _, _, _, _, _, _, _, _, _, _, _, _ -> },
            onDelete = {},
            onBackClick = {}
        )
    }
}
