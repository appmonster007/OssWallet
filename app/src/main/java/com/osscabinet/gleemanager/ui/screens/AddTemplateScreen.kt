package com.osscabinet.gleemanager.ui.screens

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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.osscabinet.gleemanager.data.local.entity.TemplateEntity
import com.osscabinet.gleemanager.ui.components.*
import com.osscabinet.gleemanager.ui.viewmodel.AccountViewModel
import com.osscabinet.gleemanager.ui.viewmodel.TagViewModel
import com.osscabinet.gleemanager.ui.viewmodel.TemplateViewModel
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddTemplateScreen(
    templateId: Long? = null,
    templateViewModel: TemplateViewModel,
    accountViewModel: AccountViewModel,
    tagViewModel: TagViewModel,
    onBackClick: () -> Unit
) {
    val templates by templateViewModel.templates.collectAsState()
    val accounts by accountViewModel.accounts.collectAsState()
    val template = remember(templateId, templates) { templates.find { it.id == templateId } }

    var name by remember { mutableStateOf(template?.name ?: "") }
    var transactionType by remember { mutableIntStateOf(template?.transactionType ?: 0) } // 0: Expense, 1: Income
    var amount by remember { mutableStateOf(template?.amount?.toString() ?: "") }
    var category by remember { mutableStateOf(template?.category ?: "Shopping") }
    var accountId by remember { mutableStateOf(template?.accountId) }
    var currency by remember { mutableStateOf(template?.currency ?: "USD") }
    var note by remember { mutableStateOf(template?.note ?: "") }
    var payee by remember { mutableStateOf(template?.payee ?: "") }
    var location by remember { mutableStateOf(template?.location ?: "") }
    var paymentMethod by remember { mutableStateOf(template?.paymentMethod ?: "Cash") }
    var tags by remember { mutableStateOf(template?.tags?.split(",")?.filter { it.isNotBlank() } ?: emptyList<String>()) }

    val selectedAccount = remember(accountId, accounts) { accounts.find { it.id == accountId } }
    
    var showAccountSheet by remember { mutableStateOf(false) }
    var showCategorySheet by remember { mutableStateOf(false) }
    var showPaymentMethodSheet by remember { mutableStateOf(false) }
    var showLabelSheet by remember { mutableStateOf(false) }
    var isNumpadVisible by remember { mutableStateOf(false) }

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
                        IconButton(onClick = onBackClick) {
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
                                    val entity = TemplateEntity(
                                        id = templateId ?: 0,
                                        name = name,
                                        category = category,
                                        amount = amount.toDoubleOrNull() ?: 0.0,
                                        transactionType = transactionType,
                                        accountId = accountId,
                                        note = note.ifBlank { null },
                                        payee = payee.ifBlank { null },
                                        location = location.ifBlank { null },
                                        paymentMethod = paymentMethod.ifBlank { null },
                                        currency = currency.ifBlank { null },
                                        tags = tags.joinToString(","),
                                        isStarred = template?.isStarred ?: false
                                    )
                                    if (templateId == null) {
                                        templateViewModel.addTemplate(
                                            name = entity.name,
                                            category = entity.category,
                                            amount = entity.amount,
                                            type = entity.transactionType,
                                            note = entity.note,
                                            accountId = entity.accountId,
                                            payee = entity.payee,
                                            location = entity.location,
                                            paymentMethod = entity.paymentMethod,
                                            currency = entity.currency,
                                            tags = tags
                                        )
                                    } else {
                                        templateViewModel.updateTemplate(entity)
                                    }
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
                    }
                )
            },
            containerColor = MaterialTheme.colorScheme.surface
        ) { innerPadding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .padding(horizontal = 16.dp, vertical = 8.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Header Card: Type & Amount (Aeon Style)
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp),
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
                                Icons.Rounded.AddCircleOutline
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
                            modifier = Modifier.fillMaxSize().clickable { isNumpadVisible = true },
                            contentAlignment = Alignment.Center
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Center
                            ) {
                                Text(
                                    text = if (amount.isEmpty()) "0.00" else (if (isIncome) "+" else "-") + amount,
                                    style = TextStyle(
                                        fontSize = 56.sp,
                                        fontWeight = FontWeight.Black,
                                        textAlign = TextAlign.Center,
                                        color = if (isIncome) Color(0xFF2E7D32) else MaterialTheme.colorScheme.error
                                    )
                                )

                                Spacer(modifier = Modifier.width(12.dp))

                                CurrencySelector(
                                    selectedCurrency = currency,
                                    onCurrencySelected = { currency = it },
                                    supportedCurrencies = selectedAccount?.allowedCurrencies ?: listOf("USD")
                                )
                            }
                        }
                    }
                }

                EditorialInput(
                    value = name,
                    onValueChange = { name = it },
                    label = "Template Name",
                    icon = Icons.Rounded.Label
                )

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    RecordFieldCard(
                        icon = Icons.Rounded.AccountBalance,
                        label = "ACCOUNT",
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

                EditorialInput(
                    value = payee,
                    onValueChange = { payee = it },
                    label = "Payee",
                    icon = Icons.Rounded.Person
                )

                EditorialInput(
                    value = paymentMethod,
                    onValueChange = { paymentMethod = it },
                    label = "Payment Method",
                    icon = Icons.Rounded.Payment,
                    readOnly = true,
                    onClick = { showPaymentMethodSheet = true }
                )

                EditorialInput(
                    value = if (tags.isEmpty()) "None" else tags.joinToString(", "),
                    onValueChange = { },
                    label = "Labels",
                    icon = Icons.Rounded.Label,
                    readOnly = true,
                    onClick = { showLabelSheet = true }
                )

                EditorialInput(
                    value = location,
                    onValueChange = { location = it },
                    label = "Location",
                    icon = Icons.Rounded.LocationOn
                )

                EditorialInput(
                    value = note,
                    onValueChange = { note = it },
                    label = "Note",
                    icon = Icons.AutoMirrored.Rounded.Notes
                )
                
                Spacer(modifier = Modifier.height(80.dp))
            }
        }

        if (isNumpadVisible) {
            Surface(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surface),
                shadowElevation = 16.dp
            ) {
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.End
                    ) {
                        TextButton(onClick = { isNumpadVisible = false }) {
                            Text("Done", fontWeight = FontWeight.Bold)
                        }
                    }
                    Numpad(
                        onNumberClick = { input ->
                            if (input == ".") {
                                if (!amount.contains(".")) {
                                    amount = if (amount.isEmpty()) "0." else amount + "."
                                }
                            } else {
                                amount = if (amount == "0") input else amount + input
                            }
                        },
                        onDeleteClick = {
                            if (amount.isNotEmpty()) {
                                amount = amount.dropLast(1)
                            }
                        },
                        onClearClick = { amount = "" },
                        onOperatorClick = {},
                        onEqualClick = { isNumpadVisible = false },
                        modifier = Modifier.height(320.dp)
                    )
                }
            }
        }
        
        // Background dimming for Sheets
        if (showAccountSheet || showCategorySheet || showPaymentMethodSheet || showLabelSheet) {
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
                        showLabelSheet = false
                    }
            )
        }
    }

    if (showAccountSheet) {
        AccountPickerSheet(
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
        CategoryPickerSheet(
            selectedCategory = category,
            onCategorySelected = {
                category = it
                showCategorySheet = false
            },
            onDismiss = { showCategorySheet = false }
        )
    }

    if (showPaymentMethodSheet) {
        PaymentMethodPickerSheet(
            selectedMethod = paymentMethod,
            onMethodSelected = { paymentMethod = it },
            onDismiss = { showPaymentMethodSheet = false }
        )
    }

    if (showLabelSheet) {
        LabelPickerSheet(
            selectedTags = tags,
            onTagsChanged = { tags = it },
            onDismiss = { showLabelSheet = false },
            tagViewModel = tagViewModel
        )
    }
}
