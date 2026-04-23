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
import androidx.compose.runtime.rememberCoroutineScope
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.osscabinet.ossWallet.data.local.entity.CategoryEntity
import com.osscabinet.ossWallet.data.local.entity.TagEntity
import com.osscabinet.ossWallet.data.local.entity.TemplateEntity
import com.osscabinet.ossWallet.domain.model.Account
import com.osscabinet.ossWallet.domain.model.CashFlowType
import com.osscabinet.ossWallet.domain.model.Transaction
import com.osscabinet.ossWallet.domain.model.dto.TransactionPrefillDTO
import com.osscabinet.ossWallet.ui.components.*
import com.osscabinet.ossWallet.ui.components.transactions.AeonTransactionItem
import com.osscabinet.ossWallet.ui.theme.OssWalletTheme
import com.osscabinet.ossWallet.domain.service.CurrencyService
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
    val transactions by viewModel.transactions.collectAsStateWithLifecycle()
    val categories by viewModel.categories.collectAsStateWithLifecycle()
    val prefillDto by viewModel.prefillDto.collectAsStateWithLifecycle()
    val accounts by accountViewModel.accounts.collectAsStateWithLifecycle()
    val exchangeRates by analyticsViewModel.exchangeRates.collectAsStateWithLifecycle()
    val decimalPrecision by settingsViewModel.decimalPrecision.collectAsStateWithLifecycle()
    val dbTemplates by templateViewModel.templates.collectAsStateWithLifecycle()
    val allTags by tagViewModel.tags.collectAsStateWithLifecycle()

    var navTriggered by remember { mutableStateOf(false) }

    // Clear prefill DTO when leaving the screen
    DisposableEffect(Unit) {
        onDispose {
            viewModel.clearPrefillDto()
        }
    }

    // Reset navTriggered when returning to this screen
    androidx.lifecycle.compose.LifecycleEventEffect(androidx.lifecycle.Lifecycle.Event.ON_RESUME) {
        navTriggered = false
    }

    val navigateBack = {
        if (!navTriggered) {
            navTriggered = true
            onBackClick()
        }
    }

    val initialTimestamp = remember { System.currentTimeMillis() }

    // Observe linked transactions for the current transaction
    val linkedTxs by if (transactionId != null && transactionId != -1L) {
        viewModel.getLinkedTransactions(transactionId).collectAsStateWithLifecycle(initialValue = emptyList())
    } else {
        remember { mutableStateOf(emptyList()) }
    }

    TransactionEntryContent(
        transactionId = transactionId,
        accountId = accountId,
        initialType = initialType,
        initialTemplate = initialTemplate,
        initialTimestamp = initialTimestamp,
        prefillDto = prefillDto,
        transactions = transactions,
        categories = categories,
        accounts = accounts,
        exchangeRates = exchangeRates,
        decimalPrecision = decimalPrecision,
        dbTemplates = dbTemplates,
        allTags = allTags,
        onAddLink = { txId, linkedId -> viewModel.addTransactionLink(txId, linkedId) },
        onRemoveLink = { txId, linkedId -> viewModel.removeTransactionLink(txId, linkedId) },
        linkedTxs = linkedTxs,
        onAddTag = { tagViewModel.addTag(it) },
        onSave = { title, amountValue, category, categoryId, isIncome, accId, currency, tags, paymentMethod, timestamp, payee, note, linkedIds, isTransfer, targetAcc, targetCurrency, rate, coords, warrantyExp, status ->
            val isEditMode = transactionId != null && transactionId != -1L
            val finalTimestamp = if (!isEditMode && timestamp == initialTimestamp) System.currentTimeMillis() else timestamp
            if (isEditMode) {
                val existingTransaction = transactions.find { it.id == transactionId }
                if (existingTransaction != null) {
                    val wasTransfer = existingTransaction.category == "Transfer"
                    
                    if (wasTransfer && !isTransfer) {
                        // Changed from Transfer to Income/Expense
                        // Clear all links for this transaction
                        viewModel.clearTransactionLinks(existingTransaction.id)

                        // Update current transaction to normal income/expense
                        viewModel.updateTransaction(
                            oldTransaction = existingTransaction,
                            title = title.ifBlank { category },
                            amount = amountValue,
                            category = category,
                            transactionType = if (isIncome) "income" else "expense",
                            accountId = accId,
                            currency = currency,
                            categoryId = categoryId,
                            tags = tags,
                            timestamp = finalTimestamp,
                            payee = payee,
                            note = note,
                            latitude = coords?.first,
                            longitude = coords?.second,
                            warrantyExpiration = warrantyExp,
                            status = status
                        )
                    } else if (!wasTransfer && isTransfer) {
                        // Changed from Income/Expense to Transfer
                        targetAcc?.id?.let { targetId ->
                            val sourceTx = existingTransaction.copy(
                                title = title.ifBlank { "Transfer to ${targetAcc.name}" },
                                amount = amountValue,
                                category = "Transfer",
                                transactionType = "expense",
                                accountId = accId,
                                currency = currency,
                                categoryId = null,
                                tags = tags,
                                timestamp = finalTimestamp,
                                payee = payee,
                                note = note,
                                latitude = coords?.first,
                                longitude = coords?.second,
                                warrantyExpiration = warrantyExp,
                                status = status
                            )

                            val targetTx = Transaction(
                                title = title.ifBlank { "Transfer from ${accounts.find { it.id == accId }?.name}" },
                                amount = amountValue.multiply(rate),
                                category = "Transfer",
                                transactionType = "income",
                                accountId = targetId,
                                currency = targetCurrency,
                                tags = tags,
                                paymentMethod = paymentMethod,
                                timestamp = finalTimestamp,
                                payee = payee,
                                note = note,
                                latitude = coords?.first,
                                longitude = coords?.second,
                                warrantyExpiration = warrantyExp,
                                status = status
                            )

                            viewModel.saveTransfer(sourceTx, targetTx)
                        }
                    } else if (wasTransfer && isTransfer) {
                        // Update existing transfer
                        targetAcc?.id?.let { targetId ->
                            val sourceTx = existingTransaction.copy(
                                title = title.ifBlank { "Transfer to ${targetAcc.name}" },
                                amount = amountValue,
                                category = "Transfer",
                                transactionType = "expense",
                                accountId = accId,
                                currency = currency,
                                categoryId = null,
                                tags = tags,
                                timestamp = finalTimestamp,
                                payee = payee,
                                note = note,
                                latitude = coords?.first,
                                longitude = coords?.second,
                                warrantyExpiration = warrantyExp,
                                status = status
                            )

                            // Update source transaction
                            viewModel.updateTransaction(
                                oldTransaction = existingTransaction,
                                title = title.ifBlank { "Transfer to ${targetAcc.name}" },
                                amount = amountValue,
                                category = "Transfer",
                                transactionType = "expense",
                                accountId = accId,
                                currency = currency,
                                categoryId = null,
                                tags = tags,
                                paymentMethod = paymentMethod,
                                timestamp = finalTimestamp,
                                payee = payee,
                                note = note,
                                latitude = coords?.first,
                                longitude = coords?.second,
                                warrantyExpiration = warrantyExp,
                                status = status
                            )

                            // Get linked target transaction and update it
                            // Note: This is a simplified approach - in production we'd need to observe the Flow
                            // For now, we'll search the transactions list
                            val linkedTxs = transactions.filter { tx ->
                                tx.category == "Transfer" &&
                                tx.accountId == targetId &&
                                tx.transactionType == "income" &&
                                Math.abs(tx.timestamp - existingTransaction.timestamp) < 1000 // Same transfer (within 1 second)
                            }

                            linkedTxs.firstOrNull()?.let { targetTx ->
                                viewModel.updateTransaction(
                                    oldTransaction = targetTx,
                                    title = title.ifBlank { "Transfer from ${accounts.find { it.id == accId }?.name}" },
                                    amount = amountValue.multiply(rate),
                                    category = "Transfer",
                                    transactionType = "income",
                                    accountId = targetId,
                                    currency = targetCurrency,
                                    tags = tags,
                                    paymentMethod = paymentMethod,
                                    timestamp = finalTimestamp,
                                    payee = payee,
                                    note = note,
                                    latitude = coords?.first,
                                    longitude = coords?.second,
                                    warrantyExpiration = warrantyExp,
                                    status = status
                                )
                            }
                        }
                    } else {
                        // Standard update (not a transfer)
                        viewModel.updateTransaction(
                            oldTransaction = existingTransaction,
                            title = title.ifBlank { category },
                            amount = amountValue,
                            category = category,
                            transactionType = if (isIncome) "income" else "expense",
                            accountId = accId,
                            currency = currency,
                            categoryId = categoryId,
                            tags = tags,
                            timestamp = finalTimestamp,
                            payee = payee,
                            note = note,
                            latitude = coords?.first,
                            longitude = coords?.second,
                            warrantyExpiration = warrantyExp,
                            status = status
                        )
                    }
                }
            } else {
                if (isTransfer) {
                    targetAcc?.id?.let { targetId ->
                        val sourceTx = Transaction(
                            title = title.ifBlank { "Transfer to ${targetAcc.name}" },
                            amount = amountValue,
                            category = "Transfer",
                            transactionType = "expense",
                            accountId = accId,
                            currency = currency,
                            tags = tags,
                            paymentMethod = paymentMethod,
                            timestamp = finalTimestamp,
                            payee = payee,
                            note = note,
                            latitude = coords?.first,
                            longitude = coords?.second,
                            warrantyExpiration = warrantyExp,
                            status = status
                        )

                        val targetTx = Transaction(
                            title = title.ifBlank { "Transfer from ${accounts.find { it.id == accId }?.name}" },
                            amount = amountValue.multiply(rate),
                            category = "Transfer",
                            transactionType = "income",
                            accountId = targetId,
                            currency = targetCurrency,
                            tags = tags,
                            paymentMethod = paymentMethod,
                            timestamp = finalTimestamp,
                            payee = payee,
                            note = note,
                            latitude = coords?.first,
                            longitude = coords?.second,
                            warrantyExpiration = warrantyExp,
                            status = status
                        )
                        
                        viewModel.saveTransfer(sourceTx, targetTx)
                    }
                } else {
                    // Add transaction and then handle links
                    viewModel.addTransaction(
                        title = title.ifBlank { category },
                        amount = amountValue,
                        category = category,
                        categoryId = categoryId,
                        transactionType = if (isIncome) "income" else "expense",
                        accountId = accId,
                        currency = currency,
                        tags = tags,
                        paymentMethod = paymentMethod,
                        timestamp = finalTimestamp,
                        payee = payee,
                        note = note,
                        latitude = coords?.first,
                        longitude = coords?.second,
                        warrantyExpiration = warrantyExp,
                        status = status
                    ) { newTxId ->
                        // Add links for cloned transaction
                        val clonedId = prefillDto?.clonedFromTransactionId
                        if (clonedId != null) {
                            viewModel.addTransactionLink(newTxId, clonedId)
                        }

                        // Add any additional links
                        linkedIds.forEach { linkedId ->
                            viewModel.addTransactionLink(newTxId, linkedId)
                        }
                    }
                }
            }
            navigateBack()
        },
        onDelete = { id ->
            viewModel.deleteTransaction(id)
            navigateBack()
        },
        onBackClick = navigateBack
    )

}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TransactionEntryContent(
    transactionId: Long? = null,
    accountId: String? = null,
    initialType: Int = 0,
    initialTemplate: String? = null,
    initialTimestamp: Long = System.currentTimeMillis(),
    prefillDto: TransactionPrefillDTO? = null,
    transactions: List<Transaction> = emptyList(),
    categories: List<CategoryEntity> = emptyList(),
    accounts: List<Account> = emptyList(),
    exchangeRates: Map<String, BigDecimal> = emptyMap(),
    decimalPrecision: Int = 2,
    dbTemplates: List<TemplateEntity> = emptyList(),
    allTags: List<TagEntity> = emptyList(),
    onAddTag: (String) -> Unit = {},
    onSave: (String, BigDecimal, String, Long?, Boolean, String, String, List<String>, String, Long, String, String, List<Long>, Boolean, Account?, String, BigDecimal, Pair<Double, Double>?, Long?, com.osscabinet.ossWallet.domain.model.TransactionStatus) -> Unit,
    onDelete: (Long) -> Unit,
    onBackClick: () -> Unit,
    onAddLink: (Long, Long) -> Unit = { _, _ -> },
    onRemoveLink: (Long, Long) -> Unit = { _, _ -> },
    linkedTxs: List<Transaction> = emptyList()
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
    var status by remember { mutableStateOf(com.osscabinet.ossWallet.domain.model.TransactionStatus.CLEARED) }
    var transactionType by remember { mutableIntStateOf(initialType) } // 0: Expense, 1: Income, 2: Transfer
    var selectedDateMillis by remember { mutableLongStateOf(initialTimestamp) }
    var payee by remember { mutableStateOf("") }
    var note by remember { mutableStateOf("") }
    var isWarrantyEnabled by remember { mutableStateOf(false) }
    var warrantyExpirationMillis by remember { mutableLongStateOf(System.currentTimeMillis() + 31536000000L) } // Default +1 year
    var showWarrantyDatePicker by remember { mutableStateOf(false) }
    var locationCoords by remember { mutableStateOf<Pair<Double, Double>?>(null) }
    var linkedTransactionIds by remember { mutableStateOf<List<Long>>(emptyList()) }

    // Unified linked transactions display state
    // For existing transactions: use database-linked transactions (linkedTxs)
    // For new transactions: derive from local linkedTransactionIds state
    val displayLinkedTxs = remember(linkedTxs, linkedTransactionIds, transactions, isEditMode) {
        if (isEditMode) {
            linkedTxs
        } else {
            // For new transactions, look up the transactions by IDs from the local state
            linkedTransactionIds.mapNotNull { id -> transactions.find { it.id == id } }
        }
    }

    var selectedAccount by remember { mutableStateOf<Account?>(null) }
    var selectedCurrency by remember { mutableStateOf("") }
    
    var targetAccount by remember { mutableStateOf<Account?>(null) }
    var targetCurrency by remember { mutableStateOf("") }
    var exchangeRate by remember { mutableStateOf(BigDecimal.ONE) }
    var selectedTemplateName by remember { mutableStateOf(initialTemplate) }
    var showLocationSheet by remember { mutableStateOf(false) }

    // Initialize state from existing transaction or initial parameters
    LaunchedEffect(existingTransaction, accounts, prefillDto) {
        if (isEditMode && existingTransaction != null) {
            title = existingTransaction.title
            amount = existingTransaction.amount.toPlainString()
            category = existingTransaction.category
            selectedCategoryId = existingTransaction.categoryId
            selectedTags = existingTransaction.tags
            paymentMethod = existingTransaction.paymentMethod
            status = existingTransaction.status
            transactionType = when {
                existingTransaction.category == "Transfer" -> 2
                existingTransaction.transactionType == "income" -> 1
                else -> 0
            }
            selectedDateMillis = existingTransaction.timestamp
            payee = existingTransaction.payee
            note = existingTransaction.note

            selectedAccount = accounts.find { it.id == existingTransaction.accountId }
            selectedCurrency = existingTransaction.currency

            // Load transfer link logic using TransactionLinkDao
            if (transactionType == 2) { // Transfer
                // Use the linkedTxs observed from ViewModel
                val linkedTransfer = linkedTxs.find { tx ->
                    tx.category == "Transfer" &&
                    tx.transactionType == "income" &&
                    Math.abs(tx.timestamp - existingTransaction.timestamp) < 1000 // Same transfer (within 1 second)
                }
                if (linkedTransfer != null) {
                    targetAccount = accounts.find { it.id == linkedTransfer.accountId }
                    targetCurrency = linkedTransfer.currency
                    if (existingTransaction.amount.signum() != 0) {
                        exchangeRate = linkedTransfer.amount.divide(existingTransaction.amount, 8, RoundingMode.HALF_UP).abs()
                    }
                }
            }

        } else {
            // Prefill from DTO if provided (for clone functionality)
            if (prefillDto != null) {
                title = prefillDto.title
                amount = prefillDto.amount
                category = prefillDto.category
                selectedCategoryId = prefillDto.categoryId
                selectedTags = prefillDto.tags
                paymentMethod = prefillDto.paymentMethod
                status = prefillDto.status
                transactionType = when (prefillDto.transactionType.lowercase()) {
                    "income" -> 1
                    "transfer" -> 2
                    else -> 0
                }
                payee = prefillDto.payee
                note = prefillDto.note
                locationCoords = if (prefillDto.latitude != null && prefillDto.longitude != null) {
                    Pair(prefillDto.latitude, prefillDto.longitude)
                } else null
                warrantyExpirationMillis = prefillDto.warrantyExpiration ?: System.currentTimeMillis() + 31536000000L
                isWarrantyEnabled = prefillDto.warrantyExpiration != null

                // Set up clone linking using TransactionLinkDao
                linkedTransactionIds = if (prefillDto.clonedFromTransactionId != null) {
                    listOf(prefillDto.clonedFromTransactionId)
                } else {
                    emptyList()
                }

                selectedAccount = accounts.find { it.id == prefillDto.accountId }
                selectedCurrency = prefillDto.currency

                // Handle transfer-specific fields
                if (transactionType == 2) {
                    prefillDto.targetAccountId?.let { targetId ->
                        targetAccount = accounts.find { it.id == targetId }
                    }
                    targetCurrency = prefillDto.targetCurrency ?: "USD"
                    exchangeRate = prefillDto.exchangeRate.toBigDecimalOrNull() ?: BigDecimal.ONE
                }
            } else if (initialTemplate != null) {
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
                // Use CurrencyService for exchange rate calculation (business logic in domain layer)
                exchangeRate = CurrencyService.convertAmount(
                    amount = BigDecimal.ONE,
                    fromCurrency = selectedCurrency,
                    toCurrency = targetCurrency,
                    exchangeRates = exchangeRates,
                    precision = 8
                )
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
    var showStatusSheet by remember { mutableStateOf(false) }
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
                                linkedTransactionIds,
                                isTransfer,
                                targetAccount,
                                targetCurrency,
                                exchangeRate,
                                locationCoords,
                                if (isWarrantyEnabled) warrantyExpirationMillis else null,
                                status
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
                verticalArrangement = Arrangement.spacedBy(8.dp)
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
                                    val fullText = "$prefix$displayAmount"
                                    AdaptiveAmountText(
                                        text = fullText,
                                        color = when {
                                            isIncome -> lerp(MaterialTheme.colorScheme.secondary, Color(0xFF2E7D32), 0.5f)
                                            isTransfer -> MaterialTheme.colorScheme.onSurface
                                            else -> lerp(MaterialTheme.colorScheme.primary, Color(0xFFB71C1C), 0.5f)
                                        },
                                        modifier = Modifier
                                            .weight(1f)
                                            .padding(horizontal = 4.dp),
                                        maxFontSize = 72.sp,
                                        minFontSize = 24.sp
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
                                        modifier = Modifier.fillMaxWidth(),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.Center
                                    ) {
                                        Text(
                                            text = "1:${exchangeRate.setScale(3, RoundingMode.HALF_UP).toPlainString()}",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.primary,
                                            maxLines = 1
                                        )
                                        val convertedText = "+${convertedAmount.setScale(decimalPrecision, RoundingMode.HALF_UP).toPlainString()}"
                                        Spacer(modifier = Modifier.width(12.dp))
                                        AdaptiveAmountText(
                                            text = convertedText,
                                            color = lerp(MaterialTheme.colorScheme.secondary, Color(0xFF2E7D32), 0.5f),
                                            modifier = Modifier.weight(1f),
                                            maxFontSize = 32.sp,
                                            minFontSize = 14.sp
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
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
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
                        if (displayLinkedTxs.isNotEmpty()) {
                            Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Text(
                                    text = "LINKED TRANSACTIONS (${displayLinkedTxs.size})",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.padding(horizontal = 4.dp)
                                )
                                displayLinkedTxs.forEach { currentLinkedTx ->
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
                                                val amountSign = if (currentLinkedTx.transactionType == "income") "+" else "-"
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
                                                onClick = {
                                                    if (transactionId != null && transactionId != -1L) {
                                                        onRemoveLink(transactionId, currentLinkedTx.id)
                                                    }
                                                },
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
                                }
                            }
                        } else {
                            RecordFieldCard(
                                icon = Icons.Rounded.Link,
                                label = "LINK TRANSACTION",
                                value = if (transactionId != null && transactionId != -1L) "None Linked" else "Add Links (Optional)",
                                onClick = { showLinkedTransactionSheet = true },
                                trailingIcon = Icons.Rounded.KeyboardArrowDown,
                                iconContainerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
                            )
                        }


                        @OptIn(ExperimentalLayoutApi::class)
                        FlowRow(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(0.dp)
                        ) {
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.3f),
                                onClick = { /* Recurring toggle */ }
                            ) {
                                Text("RECURRING", modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp), style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
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
                                Row(modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
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

                // Status
                RecordFieldCard(
                    icon = Icons.Rounded.CheckCircle,
                    label = "STATUS",
                    value = status.displayName,
                    onClick = { showStatusSheet = true },
                    modifier = Modifier.fillMaxWidth(),
                    iconContainerColor = when (status) {
                        com.osscabinet.ossWallet.domain.model.TransactionStatus.CLEARED -> Color(0xFF4CAF50).copy(alpha = 0.2f)
                        com.osscabinet.ossWallet.domain.model.TransactionStatus.RECONCILE -> Color(0xFF2196F3).copy(alpha = 0.2f)
                        com.osscabinet.ossWallet.domain.model.TransactionStatus.UNCLEARED -> Color(0xFFFF9800).copy(alpha = 0.2f)
                    },
                    isSlim = true
                )

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

        if (showStatusSheet) {
            ModalBottomSheet(
                onDismissRequest = { showStatusSheet = false },
                containerColor = MaterialTheme.colorScheme.surface,
                shape = RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp)
            ) {
                Column(modifier = Modifier.padding(24.dp).padding(bottom = 32.dp)) {
                    AeonFlowSectionHeader(title = "SELECT STATUS")
                    Spacer(modifier = Modifier.height(24.dp))
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        com.osscabinet.ossWallet.domain.model.TransactionStatus.values().forEach { statusOption ->
                            Surface(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(16.dp),
                                color = if (status == statusOption) {
                                    when (statusOption) {
                                        com.osscabinet.ossWallet.domain.model.TransactionStatus.CLEARED -> Color(0xFF4CAF50).copy(alpha = 0.2f)
                                        com.osscabinet.ossWallet.domain.model.TransactionStatus.RECONCILE -> Color(0xFF2196F3).copy(alpha = 0.2f)
                                        com.osscabinet.ossWallet.domain.model.TransactionStatus.UNCLEARED -> Color(0xFFFF9800).copy(alpha = 0.2f)
                                    }
                                } else {
                                    MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                                },
                                onClick = {
                                    status = statusOption
                                    showStatusSheet = false
                                }
                            ) {
                                Row(
                                    modifier = Modifier.padding(16.dp),
                                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = when (statusOption) {
                                            com.osscabinet.ossWallet.domain.model.TransactionStatus.CLEARED -> Icons.Rounded.CheckCircle
                                            com.osscabinet.ossWallet.domain.model.TransactionStatus.RECONCILE -> Icons.Rounded.Sync
                                            com.osscabinet.ossWallet.domain.model.TransactionStatus.UNCLEARED -> Icons.Rounded.Schedule
                                        },
                                        contentDescription = null,
                                        tint = when (statusOption) {
                                            com.osscabinet.ossWallet.domain.model.TransactionStatus.CLEARED -> Color(0xFF4CAF50)
                                            com.osscabinet.ossWallet.domain.model.TransactionStatus.RECONCILE -> Color(0xFF2196F3)
                                            com.osscabinet.ossWallet.domain.model.TransactionStatus.UNCLEARED -> Color(0xFFFF9800)
                                        }
                                    )
                                    Text(
                                        text = statusOption.displayName,
                                        style = MaterialTheme.typography.bodyLarge,
                                        color = if (status == statusOption) {
                                            when (statusOption) {
                                                com.osscabinet.ossWallet.domain.model.TransactionStatus.CLEARED -> Color(0xFF4CAF50)
                                                com.osscabinet.ossWallet.domain.model.TransactionStatus.RECONCILE -> Color(0xFF2196F3)
                                                com.osscabinet.ossWallet.domain.model.TransactionStatus.UNCLEARED -> Color(0xFFFF9800)
                                            }
                                        } else {
                                            MaterialTheme.colorScheme.onSurfaceVariant
                                        }
                                    )
                                    if (status == statusOption) {
                                        Spacer(modifier = Modifier.weight(1f))
                                        Icon(
                                            imageVector = Icons.Rounded.Check,
                                            contentDescription = "Selected",
                                            tint = when (statusOption) {
                                                com.osscabinet.ossWallet.domain.model.TransactionStatus.CLEARED -> Color(0xFF4CAF50)
                                                com.osscabinet.ossWallet.domain.model.TransactionStatus.RECONCILE -> Color(0xFF2196F3)
                                                com.osscabinet.ossWallet.domain.model.TransactionStatus.UNCLEARED -> Color(0xFFFF9800)
                                            }
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
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
                        AeonFlowSectionHeader(title = "QUICK TEMPLATES")
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

        // Multi-link selection sheet
        if (showLinkedTransactionSheet) {
            var selectedTxIds by remember { mutableStateOf<Set<Long>>(displayLinkedTxs.map { it.id }.toSet()) }
            var isSaving by remember { mutableStateOf(false) }
            val coroutineScope = rememberCoroutineScope()

            ModalBottomSheet(
                onDismissRequest = { if (!isSaving) showLinkedTransactionSheet = false },
                containerColor = MaterialTheme.colorScheme.surface,
                shape = RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp)
            ) {
                Column(modifier = Modifier.fillMaxHeight(0.7f)) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 24.dp, vertical = 16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        AeonFlowSectionHeader(
                            title = "SELECT TRANSACTIONS",
                            modifier = Modifier.weight(1f)
                        )

                        Button(
                            onClick = {
                                isSaving = true
                                if (transactionId != null && transactionId != -1L) {
                                    // Update existing transaction links
                                    val currentIds = displayLinkedTxs.map { it.id }.toSet()
                                    val toRemove = currentIds - selectedTxIds
                                    val toAdd = selectedTxIds - currentIds

                                    toRemove.forEach { id ->
                                        onRemoveLink(transactionId, id)
                                    }
                                    toAdd.forEach { id ->
                                        onAddLink(transactionId, id)
                                    }
                                } else {
                                    // Update new transaction links
                                    linkedTransactionIds = selectedTxIds.toList()
                                }
                                // Close sheet after operations complete
                                coroutineScope.launch {
                                    delay(500) // Increased delay to ensure database operations complete
                                    isSaving = false
                                    showLinkedTransactionSheet = false
                                }
                            },
                            enabled = selectedTxIds.isNotEmpty() && !isSaving
                        ) {
                            Text(
                                text = "Link ${selectedTxIds.size}",
                                style = MaterialTheme.typography.labelLarge
                            )
                        }
                    }

                    LazyColumn(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(0.dp),
                        contentPadding = PaddingValues(bottom = 100.dp)
                    ) {
                        items(transactions.filter { it.id != transactionId }.sortedByDescending { it.timestamp }) { tx ->
                            val isSelected = tx.id in selectedTxIds
                            val txAccount = accounts.find { it.id == tx.accountId }
                            val txCategory = categories.find { it.name == tx.category }
                            val icon = categoryIcons[txCategory?.iconName] ?: categoryIcons[tx.category] ?: Icons.Rounded.Category
                            val iconBg = txCategory?.colorHex?.let { Color(it) }

                            Box {
                                AeonTransactionItem(
                                    category = tx.category,
                                    account = txAccount?.name ?: "Unknown",
                                    amount = formatCurrencyRight(tx.amount.abs(), tx.currency, precision = decimalPrecision),
                                    balanceBefore = formatCurrencyRight(BigDecimal.ZERO, tx.currency, precision = decimalPrecision),
                                    balanceAfter = formatCurrencyRight(tx.amount, tx.currency, precision = decimalPrecision),
                                    type = if (tx.transactionType == "income") CashFlowType.INCOME else CashFlowType.EXPENSE,
                                    timestamp = formatDateTime(tx.timestamp),
                                    note = tx.note,
                                    tags = tx.tags,
                                    icon = icon,
                                    iconBg = iconBg,
                                    precision = decimalPrecision,
                                    showOpeningBalance = false,
                                    showClosingBalance = false,
                                    status = tx.status,
                                    onClick = {
                                        selectedTxIds = if (isSelected) {
                                            selectedTxIds - tx.id
                                        } else {
                                            selectedTxIds + tx.id
                                        }
                                    }
                                )

                                // Checkbox overlay
                                Surface(
                                    modifier = Modifier
                                        .align(Alignment.TopStart)
                                        .padding(horizontal = 8.dp, vertical = 8.dp),
                                    shape = CircleShape,
                                    color = MaterialTheme.colorScheme.surface.copy(alpha = 0.9f),
                                    border = BorderStroke(2.dp, if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant)
                                ) {
                                    Box(
                                        modifier = Modifier.size(24.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        if (isSelected) {
                                            Icon(
                                                Icons.Rounded.Check,
                                                contentDescription = "Selected",
                                                tint = MaterialTheme.colorScheme.primary,
                                                modifier = Modifier.size(16.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

private data class AdaptiveAmountTextMetrics(
    val fontSize: TextUnit,
    val scaleX: Float
)

@Composable
private fun AdaptiveAmountText(
    text: String,
    color: Color,
    modifier: Modifier = Modifier,
    maxFontSize: TextUnit,
    minFontSize: TextUnit
) {
    BoxWithConstraints(
        modifier = modifier,
        contentAlignment = Alignment.Center
    ) {
        val density = LocalDensity.current
        val textMeasurer = rememberTextMeasurer()
        val baseStyle = remember(color) {
            TextStyle(
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
                color = color
            )
        }
        val maxWidthPx = remember(maxWidth, density) {
            with(density) { maxWidth.roundToPx().coerceAtLeast(1) }
        }
        val textMetrics = remember(text, baseStyle, maxWidthPx, maxFontSize, minFontSize) {
            resolveAdaptiveAmountTextMetrics(
                textMeasurer = textMeasurer,
                text = text,
                maxWidthPx = maxWidthPx,
                baseStyle = baseStyle,
                maxFontSize = maxFontSize,
                minFontSize = minFontSize
            )
        }

        Text(
            text = text,
            style = baseStyle.copy(fontSize = textMetrics.fontSize),
            modifier = Modifier
                .fillMaxWidth()
                .graphicsLayer {
                    scaleX = textMetrics.scaleX
                    transformOrigin = TransformOrigin.Center
                },
            maxLines = 1,
            softWrap = false,
            overflow = TextOverflow.Clip
        )
    }
}

private fun resolveAdaptiveAmountTextMetrics(
    textMeasurer: TextMeasurer,
    text: String,
    maxWidthPx: Int,
    baseStyle: TextStyle,
    maxFontSize: TextUnit,
    minFontSize: TextUnit
): AdaptiveAmountTextMetrics {
    if (text.isBlank()) {
        return AdaptiveAmountTextMetrics(fontSize = maxFontSize, scaleX = 1f)
    }

    fun measure(fontSize: TextUnit) = textMeasurer.measure(
        text = AnnotatedString(text),
        style = baseStyle.copy(fontSize = fontSize),
        maxLines = 1,
        softWrap = false,
        overflow = TextOverflow.Clip,
        constraints = Constraints(maxWidth = maxWidthPx)
    )

    val preferredFontSize = preferredAmountFontSize(
        textLength = text.length,
        maxFontSize = maxFontSize,
        minFontSize = minFontSize
    )
    val preferredLayout = measure(preferredFontSize)

    if (!preferredLayout.hasVisualOverflow) {
        return AdaptiveAmountTextMetrics(fontSize = preferredFontSize, scaleX = 1f)
    }

    var low = minFontSize.value
    var high = preferredFontSize.value
    var best = minFontSize.value

    while (high - low > 0.5f) {
        val mid = (low + high) / 2f
        val fits = !measure(mid.sp).hasVisualOverflow
        if (fits) {
            best = mid
            low = mid
        } else {
            high = mid
        }
    }

    val resolvedFontSize = best.sp
    val resolvedLayout = measure(resolvedFontSize)

    if (!resolvedLayout.hasVisualOverflow) {
        return AdaptiveAmountTextMetrics(fontSize = resolvedFontSize, scaleX = 1f)
    }

    val compressedScale = (maxWidthPx / resolvedLayout.size.width.toFloat())
        .coerceIn(0.72f, 1f)

    return AdaptiveAmountTextMetrics(
        fontSize = resolvedFontSize,
        scaleX = compressedScale
    )
}

private fun preferredAmountFontSize(
    textLength: Int,
    maxFontSize: TextUnit,
    minFontSize: TextUnit
): TextUnit {
    val shrinkStart = 7
    val fullShrink = 18
    val progress = ((textLength - shrinkStart).toFloat() / (fullShrink - shrinkStart))
        .coerceIn(0f, 1f)
    val easedProgress = FastOutSlowInEasing.transform(progress)

    return lerpSp(maxFontSize, minFontSize, easedProgress)
}

private fun lerpSp(
    start: TextUnit,
    stop: TextUnit,
    fraction: Float
): TextUnit = (start.value + (stop.value - start.value) * fraction).sp

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
                        verticalArrangement = Arrangement.spacedBy(0.dp)
                    ) {
                        Surface(shape = RoundedCornerShape(12.dp), color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.3f)) {
                            Text("RECURRING", modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp), style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                        }
                        Surface(shape = RoundedCornerShape(12.dp), color = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.4f)) {
                            Text("FOOD", modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp), style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                        }
                        Surface(shape = RoundedCornerShape(12.dp), color = MaterialTheme.colorScheme.surfaceVariant) {
                            Row(modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
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

@Composable
private fun TransactionAmountPreviewCard(
    primaryText: String,
    primaryColor: Color,
    primaryCurrency: String,
    convertedText: String? = null,
    convertedCurrency: String? = null,
    exchangeRateLabel: String? = null
) {
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
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                AdaptiveAmountText(
                    text = primaryText,
                    color = primaryColor,
                    modifier = Modifier.weight(1f),
                    maxFontSize = 72.sp,
                    minFontSize = 24.sp
                )

                Spacer(modifier = Modifier.width(16.dp))

                CurrencySelector(
                    selectedCurrency = primaryCurrency,
                    onCurrencySelected = {},
                    supportedCurrencies = listOf(primaryCurrency)
                )
            }

            if (convertedText != null && convertedCurrency != null && exchangeRateLabel != null) {
                Spacer(modifier = Modifier.height(12.dp))
                Icon(
                    Icons.Rounded.KeyboardArrowDown,
                    contentDescription = null,
                    modifier = Modifier.size(24.dp),
                    tint = MaterialTheme.colorScheme.outline
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = exchangeRateLabel,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    AdaptiveAmountText(
                        text = convertedText,
                        color = lerp(MaterialTheme.colorScheme.secondary, Color(0xFF2E7D32), 0.5f),
                        modifier = Modifier.weight(1f),
                        maxFontSize = 32.sp,
                        minFontSize = 14.sp
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    CurrencySelector(
                        selectedCurrency = convertedCurrency,
                        onCurrencySelected = {},
                        supportedCurrencies = listOf(convertedCurrency)
                    )
                }
            }
        }
    }
}

@Preview(showBackground = true, widthDp = 390, name = "Amount Overflow Expense")
@Composable
private fun TransactionAmountOverflowPreview() {
    OssWalletTheme {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            TransactionAmountPreviewCard(
                primaryText = "-123456789012345.6789",
                primaryColor = lerp(MaterialTheme.colorScheme.primary, Color(0xFFB71C1C), 0.5f),
                primaryCurrency = "USD"
            )
        }
    }
}

@Preview(showBackground = true, widthDp = 390, name = "Amount Overflow Transfer")
@Composable
private fun TransactionTransferAmountOverflowPreview() {
    OssWalletTheme {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            TransactionAmountPreviewCard(
                primaryText = "98765432109876.54321",
                primaryColor = MaterialTheme.colorScheme.onSurface,
                primaryCurrency = "JPY",
                convertedText = "+12345678901234567.89",
                convertedCurrency = "INR",
                exchangeRateLabel = "1:124.875"
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
fun TransactionEntryWithLongLinkPreview() {
    val sampleAccounts = listOf(
        Account("1", "Aeon Prime Savings", BigDecimal("1250.50"), "USD", "Bank", "#FF0000", "account_balance", listOf("account_balance")),
    )

    val sampleCategories = listOf(
        CategoryEntity(1, "Food", "Restaurant", 0xFF4CAF50.toInt()),
    )

    val sampleTransactions = listOf(
        Transaction(
            id = 1000L,
            title = "Very long title for the linked transaction that might push the close button off screen",
            amount = BigDecimal("1234.56"),
            category = "Extremely Long Category Name For Testing Purposes",
            transactionType = "expense",
            accountId = "1",
            currency = "USD",
            timestamp = System.currentTimeMillis(),
            note = "Very long note that might push the close button off screen if not handled properly in the UI layout",
            status = com.osscabinet.ossWallet.domain.model.TransactionStatus.CLEARED
        ),
        Transaction(
            id = 1L,
            title = "Main Transaction",
            amount = BigDecimal("100.00"),
            category = "Food",
            transactionType = "expense",
            accountId = "1",
            currency = "USD",
            timestamp = System.currentTimeMillis(),
            status = com.osscabinet.ossWallet.domain.model.TransactionStatus.CLEARED
        )
    )

    OssWalletTheme {
        TransactionEntryContent(
            transactionId = 1L,
            accounts = sampleAccounts,
            categories = sampleCategories,
            transactions = sampleTransactions,
            onSave = { _, _, _, _, _, _, _, _, _, _, _, _, _, _, _, _, _, _, _, _ -> },
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
        Account("1", "Aeon Prime Savings", BigDecimal("1250.50"), "USD", "Bank", "#FF0000", "account_balance", listOf("account_balance")),
        Account("2", "Cash Wallet", BigDecimal("45.00"), "USD", "Cash", "#00FF00", "wallet", listOf("wallet"))
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
            onSave = { _, _, _, _, _, _, _, _, _, _, _, _, _, _, _, _, _, _, _, _ -> },
            onDelete = {},
            onBackClick = {}
        )
    }
}
