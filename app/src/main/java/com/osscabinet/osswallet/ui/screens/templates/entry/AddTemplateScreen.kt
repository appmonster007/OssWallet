package com.osscabinet.ossWallet.ui.screens.templates.entry

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Done
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.osscabinet.ossWallet.domain.model.Template
import com.osscabinet.ossWallet.ui.screens.templates.entry.components.TemplateEntrySheets
import com.osscabinet.ossWallet.ui.screens.templates.entry.components.TemplateFormBody
import com.osscabinet.ossWallet.ui.viewmodel.*
import java.math.BigDecimal

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
    val uniquePayees by transactionViewModel.uniquePayees.collectAsStateWithLifecycle()
    val uniqueNotes by transactionViewModel.uniqueNotes.collectAsStateWithLifecycle()
    val template = remember(templateId, templates) { templates.find { it.id == templateId } }
    val prefill by transactionViewModel.templatePrefill.collectAsStateWithLifecycle()

    var name by remember { mutableStateOf("") }
    var transactionType by remember { mutableIntStateOf(0) }
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
    var targetAccountId by remember { mutableStateOf<String?>(null) }
    var currency by remember { mutableStateOf("USD") }
    var targetCurrency by remember { mutableStateOf<String?>(null) }
    var transferRate by remember { mutableStateOf(BigDecimal.ONE) }
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
            targetAccountId = template.targetAccountId
            currency = template.currency ?: "USD"
            targetCurrency = template.targetCurrency
            transferRate = template.transferRate
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
            transactionViewModel.clearTemplatePrefill()
        }
    }

    val selectedAccount = remember(accountId, accounts) { accounts.find { it.id == accountId } }
    val selectedTargetAccount = remember(targetAccountId, accounts) { accounts.find { it.id == targetAccountId } }
    
    var showAccountSheet by remember { mutableStateOf(false) }
    var showTargetAccountSheet by remember { mutableStateOf(false) }
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

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { 
                    Text(
                        if (templateId == null) "New Template" else "Edit Template", 
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontWeight = FontWeight.Bold,
                            letterSpacing = (-0.5).sp
                        )
                    ) 
                },
                navigationIcon = {
                    IconButton(onClick = { navigate(onBackClick) }) {
                        Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    if (templateId != null) {
                        IconButton(
                            onClick = {
                                template?.let {
                                    templateViewModel.deleteTemplate(it)
                                    onBackClick()
                                }
                            }
                        ) {
                            Icon(
                                Icons.Rounded.Delete,
                                contentDescription = "Delete",
                                tint = MaterialTheme.colorScheme.error
                            )
                        }
                    }
                    IconButton(
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
                                        targetAccountId = targetAccountId,
                                        targetCurrency = targetCurrency,
                                        transferRate = transferRate,
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
                                            targetAccountId = domainTemplate.targetAccountId,
                                            targetCurrency = domainTemplate.targetCurrency,
                                            transferRate = domainTemplate.transferRate,
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
                        },
                        enabled = name.isNotBlank()
                    ) {
                        Icon(
                            Icons.Rounded.Done,
                            contentDescription = "Save",
                            tint = if (name.isNotBlank()) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.38f)
                        )
                    }
                }
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { innerPadding ->
        TemplateFormBody(
            name = name,
            onNameChange = { name = it },
            transactionType = transactionType,
            onTypeSelected = { transactionType = it },
            amount = amount,
            currency = currency,
            onAmountClick = { isNumpadVisible = true },
            selectedAccount = selectedAccount,
            onAccountClick = { showAccountSheet = true },
            targetAccount = selectedTargetAccount,
            onTargetAccountClick = { showTargetAccountSheet = true },
            categoryName = categoryName,
            categoryObj = categoriesList.find { it.id == categoryId },
            onCategoryClick = { showCategorySheet = true },
            payee = payee,
            onPayeeChange = { payee = it },
            uniquePayees = uniquePayees,
            tags = tags,
            onShowTagSheet = { showTagSheet = true },
            onRemoveTag = { tag -> tags = tags - tag },
            paymentMethod = paymentMethod,
            onPaymentMethodClick = { showPaymentMethodSheet = true },
            location = location,
            onLocationChange = { location = it },
            note = note,
            onNoteChange = { note = it },
            uniqueNotes = uniqueNotes,
            modifier = Modifier.padding(innerPadding)
        )

        TemplateEntrySheets(
            isNumpadVisible = isNumpadVisible,
            onDismissNumpad = { isNumpadVisible = false },
            amount = amount,
            onAmountChange = { newAmount, newType -> 
                amount = newAmount
                transactionType = when (newType) {
                    "income" -> 1
                    "transfer" -> 2
                    else -> 0
                }
            },
            transactionType = transactionType,
            decimalPrecision = decimalPrecision,
            showAccountSheet = showAccountSheet,
            onDismissAccountSheet = { showAccountSheet = false },
            showTargetAccountSheet = showTargetAccountSheet,
            onDismissTargetAccountSheet = { showTargetAccountSheet = false },
            accounts = accounts,
            selectedAccount = selectedAccount,
            onAccountSelected = {
                accountId = it.id
                showAccountSheet = false
            },
            targetAccount = selectedTargetAccount,
            onTargetAccountSelected = {
                targetAccountId = it.id
                targetCurrency = it.currencyCode
                showTargetAccountSheet = false
            },
            showCategorySheet = showCategorySheet,
            onDismissCategorySheet = { showCategorySheet = false },
            categories = categoriesList,
            selectedCategoryId = categoryId,
            onCategorySelected = { name, id ->
                categoryName = name
                categoryId = id
                showCategorySheet = false
            },
            showPaymentMethodSheet = showPaymentMethodSheet,
            onDismissPaymentMethodSheet = { showPaymentMethodSheet = false },
            paymentMethod = paymentMethod,
            onPaymentMethodSelected = { 
                paymentMethod = it
                showPaymentMethodSheet = false
            },
            showTagSheet = showTagSheet,
            onDismissTagSheet = { showTagSheet = false },
            allTags = allTags,
            selectedTags = tags,
            onTagsSelected = { tags = it },
            onAddTag = { name, color -> tagViewModel.addTag(name, color) },
            onEditTag = { tagViewModel.updateTag(it.name, it) },
            onDeleteTag = { tagViewModel.deleteTag(it) }
        )
    }
}
