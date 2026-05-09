package com.osscabinet.ossWallet.ui.screens

import androidx.compose.foundation.*
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.Notes
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.math.BigDecimal
import com.osscabinet.ossWallet.domain.model.Tag
import com.osscabinet.ossWallet.domain.model.Template
import com.osscabinet.ossWallet.domain.util.splitByComma
import com.osscabinet.ossWallet.ui.components.entry.*
import com.osscabinet.ossWallet.ui.components.accounts.AccountSelection
import com.osscabinet.ossWallet.ui.components.CalculatorNumpad
import com.osscabinet.ossWallet.ui.components.CurrencySelector
import com.osscabinet.ossWallet.ui.screens.transactions.entry.sheets.PaymentMethodSelection
import com.osscabinet.ossWallet.ui.screens.transactions.entry.sheets.TagSelection
import com.osscabinet.ossWallet.ui.screens.transactions.entry.components.TransactionCategorySelection
import com.osscabinet.ossWallet.ui.viewmodel.AccountViewModel
import com.osscabinet.ossWallet.ui.viewmodel.TagViewModel
import com.osscabinet.ossWallet.ui.viewmodel.TemplateViewModel
import com.osscabinet.ossWallet.ui.viewmodel.TransactionViewModel
import com.osscabinet.ossWallet.ui.viewmodel.SettingsViewModel
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddTemplateScreen(
    templateId: Long? = null,
    templateViewModel: TemplateViewModel,
    accountViewModel: AccountViewModel,
    transactionViewModel: TransactionViewModel,
    tagViewModel: TagViewModel,
    settingsViewModel: SettingsViewModel,
    onBackClick: () -> Unit
) {
    val templates by templateViewModel.templates.collectAsStateWithLifecycle()
    val accounts by accountViewModel.accounts.collectAsStateWithLifecycle()
    val categoriesList by transactionViewModel.categories.collectAsStateWithLifecycle()
    val decimalPrecision by settingsViewModel.decimalPrecision.collectAsStateWithLifecycle()
    val allTags by tagViewModel.tags.collectAsStateWithLifecycle()
    val template = remember(templateId, templates) { templates.find { it.id == templateId } }
    val prefill by transactionViewModel.templatePrefill.collectAsStateWithLifecycle()

    var name by remember { mutableStateOf("") }
    var transactionType by remember { mutableIntStateOf(0) } // 0: Expense, 1: Income
    var amount by remember { mutableStateOf("") }
    var categoryId by remember { mutableLongStateOf(1002L) }
    var categoryName by remember { mutableStateOf("Unknown") }

    LaunchedEffect(categoriesList, categoryId) {
        val cat = categoriesList.find { it.id == categoryId }
        if (cat != null) {
            categoryName = cat.name
        }
    }
    var accountId by remember { mutableStateOf<String?>(null) }
    var currency by remember { mutableStateOf("USD") }
    var note by remember { mutableStateOf("") }
    var payee by remember { mutableStateOf("") }
    var location by remember { mutableStateOf("") }
    var paymentMethod by remember { mutableStateOf("Cash") }
    var tags by remember { mutableStateOf(emptyList<String>()) }

    LaunchedEffect(template, prefill) {
        if (template != null) {
            name = template.name
            transactionType = template.transactionType
            amount = template.amount.toPlainString()
            categoryId = template.categoryId
            accountId = template.accountId
            currency = template.currency ?: "USD"
            note = template.note ?: ""
            payee = template.payee ?: ""
            location = template.location ?: ""
            paymentMethod = template.paymentMethod ?: "Cash"
            tags = template.tags
        } else if (prefill != null) {
            name = prefill!!.name
            transactionType = prefill!!.transactionType
            amount = prefill!!.amount.abs().toPlainString()
            categoryId = prefill!!.categoryId
            accountId = prefill!!.accountId
            currency = prefill!!.currency ?: "USD"
            note = prefill!!.note ?: ""
            payee = prefill!!.payee ?: ""
            location = prefill!!.location ?: ""
            paymentMethod = prefill!!.paymentMethod ?: "Cash"
            tags = prefill!!.tags
            
            // Clear prefill once applied
            transactionViewModel.clearTemplatePrefill()
        }
    }

    val selectedAccount = remember(accountId, accounts) { accounts.find { it.id == accountId } }
    
    var showAccountSheet by remember { mutableStateOf(false) }
    var showCategorySheet by remember { mutableStateOf(false) }
    var showPaymentMethodSheet by remember { mutableStateOf(false) }
    var showTagSheet by remember { mutableStateOf(false) }
    var isNumpadVisible by remember { mutableStateOf(false) }

    var navTriggered by remember { mutableStateOf(false) }

    val navigate: (() -> Unit) -> Unit = { action ->
        if (!navTriggered) {
            navTriggered = true
            action()
        }
    }

    val isIncome = transactionType == 1

    LaunchedEffect(selectedAccount) {
        if (selectedAccount != null && (currency.isEmpty() || !selectedAccount.allowedCurrencies.contains(currency))) {
            currency = selectedAccount.currencyCode
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Scaffold(
            topBar = {
                CenterAlignedTopAppBar(
                    title = { 
                        Text(
                            if (templateId == null) "New Template" else "Edit Template", 
                            style = MaterialTheme.typography.titleLarge.copy(
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        ) 
                    },
                    navigationIcon = {
                        IconButton(onClick = { navigate(onBackClick) }) {
                            Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Back")
                        }
                    },
                    actions = {
                        Surface(
                            modifier = Modifier.padding(end = 16.dp),
                            shape = RoundedCornerShape(24.dp),
                            color = MaterialTheme.colorScheme.primary,
                            onClick = {
                                if (name.isNotBlank()) {
                                    navigate {
                                        val domainTemplate = Template(
                                            id = templateId ?: 0,
                                            name = name,
                                            categoryId = categoryId,
                                            amount = amount.toBigDecimalOrNull() ?: BigDecimal.ZERO,
                                            transactionType = transactionType,
                                            icon = template?.icon,
                                            colorHex = template?.colorHex,
                                            accountId = accountId,
                                            note = note.ifBlank { null },
                                            payee = payee.ifBlank { null },
                                            location = location.ifBlank { null },
                                            paymentMethod = paymentMethod.ifBlank { null },
                                            currency = currency.ifBlank { null },
                                            tags = tags,
                                            isStarred = template?.isStarred ?: false,
                                            displayOrder = template?.displayOrder ?: 0
                                        )
                                        if (templateId == null) {
                                            templateViewModel.addTemplate(
                                                name = domainTemplate.name,
                                                categoryId = domainTemplate.categoryId,
                                                amount = domainTemplate.amount,
                                                transactionType = domainTemplate.transactionType,
                                                note = domainTemplate.note,
                                                accountId = domainTemplate.accountId,
                                                payee = domainTemplate.payee,
                                                location = domainTemplate.location,
                                                tags = tags
                                            )
                                        } else {
                                            templateViewModel.updateTemplate(domainTemplate)
                                        }
                                        onBackClick()
                                    }
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
                    }
                )
            },
            containerColor = MaterialTheme.colorScheme.surface
        ) { innerPadding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .padding(horizontal = 24.dp, vertical = 16.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Header Card: Type & Amount (Aeon Style)
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .wrapContentHeight(),
                    shape = RoundedCornerShape(32.dp),
                    color = MaterialTheme.colorScheme.surfaceContainerLow,
                    border = BorderStroke(0.5.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.15f)),
                    shadowElevation = 0.dp
                ) {
                    Column(
                        modifier = Modifier.padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        // Transaction Type Row
                        EntryTypeSelector(
                            selectedType = transactionType,
                            onTypeSelected = { transactionType = it },
                            types = listOf(
                                Triple(Icons.Rounded.Remove, "Expense", 0),
                                Triple(Icons.Rounded.Add, "Income", 1)
                            ),
                            selectedColor = if (transactionType == 1) Color(0xFF004D40) else Color(0xFFB3261E),
                            itemWidth = 120.dp
                        )

                        Spacer(modifier = Modifier.height(24.dp))

                        // Amount & Currency Area
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(20.dp))
                                .clickable { isNumpadVisible = true }
                                .padding(vertical = 12.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Center
                            ) {
                                CurrencySelector(
                                    selectedCurrency = currency,
                                    onCurrencySelected = { currency = it },
                                    supportedCurrencies = selectedAccount?.allowedCurrencies ?: listOf("USD")
                                )

                                Spacer(modifier = Modifier.width(12.dp))

                                val amountTextValue = if (amount.isEmpty()) "0.00" else amount
                                var multiplier by remember(amountTextValue) { mutableStateOf(1f) }

                                Text(
                                    text = amountTextValue,
                                    style = MaterialTheme.typography.displayMedium.copy(
                                        fontWeight = FontWeight.Bold,
                                        letterSpacing = (-1).sp,
                                        fontSize = (MaterialTheme.typography.displayMedium.fontSize.value * multiplier).sp
                                    ),
                                    maxLines = 1,
                                    softWrap = false,
                                    overflow = androidx.compose.ui.text.style.TextOverflow.Clip,
                                    onTextLayout = { textLayoutResult ->
                                        if (textLayoutResult.didOverflowWidth && multiplier > 0.3f) {
                                            multiplier *= 0.8f
                                        }
                                    },
                                    color = if (transactionType == 1) Color(0xFF004D40) else Color(0xFFB3261E),
                                    modifier = Modifier.weight(1f, fill = false)
                                )
                            }
                        }

                        HorizontalDivider(
                            modifier = Modifier.padding(vertical = 12.dp),
                            thickness = 0.5.dp,
                            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.15f)
                        )

                        EntryEditorialInput(
                            value = name,
                            onValueChange = { name = it },
                            label = "TEMPLATE NAME",
                            icon = Icons.Rounded.Bookmark
                        )
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    EntryActionCard(
                        icon = Icons.Rounded.AccountBalance,
                        label = "ACCOUNT",
                        value = selectedAccount?.name ?: "Select",
                        onClick = { showAccountSheet = true },
                        modifier = Modifier.weight(1f),
                        iconContainerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
                        iconTint = MaterialTheme.colorScheme.primary
                    )

                    EntryActionCard(
                        icon = com.osscabinet.ossWallet.ui.components.categoryIcons[categoryName] ?: Icons.Rounded.Category,
                        label = "CATEGORY",
                        value = categoryName,
                        onClick = { showCategorySheet = true },
                        modifier = Modifier.weight(1f),
                        iconContainerColor = MaterialTheme.colorScheme.tertiary.copy(alpha = 0.1f),
                        iconTint = MaterialTheme.colorScheme.tertiary
                    )
                }

                EntrySection(
                    title = "TEMPLATE DETAILS",
                    icon = Icons.Rounded.MoreHoriz
                ) {
                    EntryEditorialInput(
                        value = payee,
                        onValueChange = { payee = it },
                        label = "PAYEE",
                        icon = Icons.Rounded.Person
                    )

                    EntryTagSection(
                        selectedTags = tags,
                        onShowTagSheet = { showTagSheet = true },
                        onRemoveTag = { tag -> tags = tags - tag }
                    )

                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
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
                        val paymentIcon = methods.find { it.first == paymentMethod }?.second ?: Icons.Rounded.Contactless

                        EntryActionChip(
                            icon = paymentIcon,
                            label = paymentMethod,
                            onClick = { showPaymentMethodSheet = true },
                            modifier = Modifier.weight(1f)
                        )

                        // Placeholder for another chip if needed
                        Spacer(modifier = Modifier.weight(1f))
                    }

                    EntryEditorialInput(
                        value = location,
                        onValueChange = { location = it },
                        label = "LOCATION",
                        icon = Icons.Rounded.LocationOn
                    )

                    EntryEditorialInput(
                        value = note,
                        onValueChange = { note = it },
                        label = "NOTE",
                        icon = Icons.AutoMirrored.Rounded.Notes
                    )
                }

                Spacer(modifier = Modifier.height(80.dp))
            }
        }

        // Numpad Overlay
        if (isNumpadVisible) {
            // Background dimming
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.4f))
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null
                    ) {
                        isNumpadVisible = false
                    }
            )
            
            // Numpad
            Surface(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                shape = RoundedCornerShape(24.dp),
                color = MaterialTheme.colorScheme.surface,
                tonalElevation = 8.dp,
                shadowElevation = 8.dp
            ) {
                CalculatorNumpad(
                    amount = amount,
                    onAmountChange = { newAmount, newType -> 
                        amount = newAmount
                        transactionType = if (newType == "income") 1 else 0
                    },
                    transactionType = if (isIncome) "income" else "expense",
                    decimalPrecision = decimalPrecision,
                    onDone = { isNumpadVisible = false },
                    modifier = Modifier.padding(8.dp)
                )
            }
        }

        // Background dimming for Sheets
        if (showAccountSheet || showCategorySheet || showPaymentMethodSheet || showTagSheet) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.4f))
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null
                    ) { 
                        showAccountSheet = false
                        showCategorySheet = false
                        showPaymentMethodSheet = false
                        showTagSheet = false
                    }
            )
        }
    }

    if (showAccountSheet) {
        AccountSelection(
            accounts = accounts,
            selectedAccount = selectedAccount,
            onAccountSelected = {
                accountId = it.id
                showAccountSheet = false
            },
            onDismiss = { showAccountSheet = false }
        )
    }

    if (showCategorySheet) {
        TransactionCategorySelection(
            categories = categoriesList,
            selectedCategoryId = categoryId,
            onCategorySelected = { name, id ->
                categoryName = name
                categoryId = id
                showCategorySheet = false
            },
            onDismiss = { showCategorySheet = false }
        )
    }

    if (showPaymentMethodSheet) {
        PaymentMethodSelection(
            selectedMethod = paymentMethod,
            onMethodSelected = { paymentMethod = it },
            onDismiss = { showPaymentMethodSheet = false }
        )
    }

    if (showTagSheet) {
        TagSelection(
            tags = allTags,
            selectedTags = tags,
            onTagsSelected = { tags = it },
            onAddTag = { name, color -> tagViewModel.addTag(name, color) },
            onEditTag = { tagViewModel.updateTag(it.name, it) },
            onDeleteTag = { tagViewModel.deleteTag(it) },
            onDismiss = { showTagSheet = false }
        )
    }
}
