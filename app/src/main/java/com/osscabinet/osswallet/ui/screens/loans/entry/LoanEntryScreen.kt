package com.osscabinet.ossWallet.ui.screens.loans.entry

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Done
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.osscabinet.ossWallet.domain.model.Account
import com.osscabinet.ossWallet.domain.model.Category
import com.osscabinet.ossWallet.domain.model.LoanType
import com.osscabinet.ossWallet.domain.service.currency.CurrencyService
import com.osscabinet.ossWallet.ui.screens.loans.entry.components.LoanEntrySheets
import com.osscabinet.ossWallet.ui.screens.loans.entry.components.LoanFormBody
import com.osscabinet.ossWallet.ui.viewmodel.LoanViewModel
import java.math.BigDecimal

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LoanEntryScreen(
    loanId: Long?,
    viewModel: LoanViewModel,
    accounts: List<Account> = emptyList(),
    categories: List<Category> = emptyList(),
    onBackClick: () -> Unit
) {
    val loans by viewModel.loans.collectAsStateWithLifecycle()
    val transactions by viewModel.transactions.collectAsStateWithLifecycle()
    val profileId by viewModel.currentProfileId.collectAsStateWithLifecycle()
    
    val uniquePayees = remember(transactions) { 
        transactions.map { it.payee }.filter { it.isNotBlank() }.distinct().sorted() 
    }
    val uniqueNotes = remember(transactions) {
        transactions.map { it.note }.filter { it.isNotBlank() }.distinct().sorted()
    }
    val existing = remember(loanId, loans) {
        loanId?.let { id -> loans.find { it.id == id } }
    }

    var name by remember(existing) { mutableStateOf(existing?.name ?: "") }
    var principalText by remember(existing) {
        mutableStateOf(
            existing?.principalAmount?.toPlainString() ?: ""
        )
    }
    var currency by remember(existing) { mutableStateOf(existing?.currency ?: "USD") }
    var loanType by remember(existing) { mutableStateOf(existing?.loanType ?: LoanType.BORROWED) }
    var counterparty by remember(existing) { mutableStateOf(existing?.lenderOrBorrowerName ?: "") }
    var interestText by remember(existing) {
        mutableStateOf(
            existing?.interestRate?.toString() ?: ""
        )
    }
    var note by remember(existing) { mutableStateOf(existing?.note ?: "") }

    var selectedCategoryIds by remember(existing) {
        mutableStateOf(existing?.categoryIds?.toSet() ?: emptySet<Long>())
    }
    var selectedTags by remember(existing) {
        mutableStateOf(existing?.tags?.toSet() ?: emptySet<String>())
    }

    val now = remember { System.currentTimeMillis() }
    var startDateMillis by remember(existing) { mutableLongStateOf(existing?.startDate ?: now) }
    var hasSettlementDate by remember(existing) { mutableStateOf(existing?.expectedSettlementDate != null) }
    var settlementDateMillis by remember(existing) {
        mutableLongStateOf(existing?.expectedSettlementDate ?: now)
    }

    var linkedAccountId by remember(existing) {
        mutableStateOf(
            existing?.linkedAccountId?.ifBlank { null }
                ?: accounts.firstOrNull()?.id
                ?: ""
        )
    }

    val isEditing = loanId != null
    val haptic = LocalHapticFeedback.current

    var showStartDatePicker by remember { mutableStateOf(false) }
    var showSettlementDatePicker by remember { mutableStateOf(false) }
    var showNumpad by remember { mutableStateOf(false) }
    var showInterestNumpad by remember { mutableStateOf(false) }
    var showAccountSheet by remember { mutableStateOf(false) }
    var showCategorySheet by remember { mutableStateOf(false) }
    var showTagSheet by remember { mutableStateOf(false) }

    var accountQuery by remember { mutableStateOf("") }
    var categoryQuery by remember { mutableStateOf("") }

    val allCurrencies = remember { CurrencyService.getAllCurrencies() }
    val canSave = name.isNotBlank() && 
                 principalText.toBigDecimalOrNull() != null && 
                 selectedCategoryIds.isNotEmpty()

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(
                        text = if (isEditing) "Edit Loan" else "New Loan",
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontWeight = FontWeight.Bold,
                            letterSpacing = (-0.5).sp
                        )
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    if (isEditing) {
                        IconButton(
                            onClick = {
                                viewModel.deleteLoan(loanId!!)
                                onBackClick()
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
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            val principal = principalText.toBigDecimalOrNull() ?: BigDecimal.ZERO
                            val rate = interestText.toFloatOrNull()
                            val loan = (existing ?: viewModel.buildDefaultLoan(profileId)).copy(
                                name = name,
                                principalAmount = principal,
                                currency = currency,
                                loanType = loanType,
                                lenderOrBorrowerName = counterparty,
                                interestRate = rate,
                                note = note,
                                startDate = startDateMillis,
                                expectedSettlementDate = if (hasSettlementDate) settlementDateMillis else null,
                                linkedAccountId = linkedAccountId,
                                categoryIds = selectedCategoryIds.toList(),
                                tags = selectedTags.toList()
                            )
                            viewModel.saveLoan(loan)
                            onBackClick()
                        },
                        enabled = canSave
                    ) {
                        Icon(
                            Icons.Rounded.Done,
                            contentDescription = "Save",
                            tint = if (canSave) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.38f)
                        )
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        }
    ) { innerPadding ->
        LoanFormBody(
            loanType = loanType,
            onTypeSelected = { loanType = it },
            name = name,
            onNameChange = { name = it },
            amount = principalText,
            currency = currency,
            onCurrencyChange = { currency = it },
            supportedCurrencies = allCurrencies,
            onAmountClick = { showNumpad = true },
            startDateMillis = startDateMillis,
            onStartDateClick = { showStartDatePicker = true },
            interestRate = interestText,
            onInterestClick = { showInterestNumpad = true },
            linkedAccountId = linkedAccountId,
            accounts = accounts,
            onAccountClick = { showAccountSheet = true },
            hasSettlementDate = hasSettlementDate,
            settlementDateMillis = settlementDateMillis,
            onSettlementClick = { showSettlementDatePicker = true },
            selectedCategoryIds = selectedCategoryIds,
            categories = categories,
            onCategoryClick = { showCategorySheet = true },
            counterparty = counterparty,
            onCounterpartyChange = { counterparty = it },
            note = note,
            onNoteChange = { note = it },
            onHasSettlementDateChange = { hasSettlementDate = it },
            selectedTags = selectedTags,
            onShowTagSheet = { showTagSheet = true },
            onRemoveTag = { tag -> selectedTags = selectedTags - tag },
            counterpartySuggestions = uniquePayees,
            noteSuggestions = uniqueNotes,
            modifier = Modifier.padding(innerPadding)
        )

        val availableTags by viewModel.availableTags.collectAsStateWithLifecycle()
        LoanEntrySheets(
            showStartDatePicker = showStartDatePicker,
            onDismissStartDatePicker = { showStartDatePicker = false },
            startDateMillis = startDateMillis,
            onStartDateSelected = { startDateMillis = it; showStartDatePicker = false },
            showSettlementDatePicker = showSettlementDatePicker,
            onDismissSettlementDatePicker = { showSettlementDatePicker = false },
            settlementDateMillis = settlementDateMillis,
            onSettlementDateSelected = { settlementDateMillis = it; showSettlementDatePicker = false },
            showNumpad = showNumpad,
            onDismissNumpad = { showNumpad = false },
            principalText = principalText,
            onPrincipalChanged = { principalText = it },
            showInterestNumpad = showInterestNumpad,
            onDismissInterestNumpad = { showInterestNumpad = false },
            interestText = interestText,
            onInterestChanged = { interestText = it },
            showAccountSheet = showAccountSheet,
            onDismissAccountSheet = { showAccountSheet = false },
            accountQuery = accountQuery,
            onAccountQueryChange = { accountQuery = it },
            accounts = accounts,
            linkedAccountId = linkedAccountId,
            onAccountSelected = { linkedAccountId = it },
            showCategorySheet = showCategorySheet,
            onDismissCategorySheet = { showCategorySheet = false },
            categoryQuery = categoryQuery,
            onCategoryQueryChange = { categoryQuery = it },
            categories = categories,
            selectedCategoryIds = selectedCategoryIds,
            onCategorySelectionChange = { selectedCategoryIds = it },
            showTagSheet = showTagSheet,
            onDismissTagSheet = { showTagSheet = false },
            availableTags = availableTags,
            selectedTags = selectedTags,
            onTagSelectionChange = { selectedTags = it },
            onAddTag = { selectedTags = selectedTags + it }
        )
    }
}
