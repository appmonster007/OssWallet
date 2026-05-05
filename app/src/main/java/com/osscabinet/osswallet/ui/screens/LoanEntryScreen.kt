package com.osscabinet.ossWallet.ui.screens

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.osscabinet.ossWallet.domain.model.Account
import com.osscabinet.ossWallet.domain.model.Category
import com.osscabinet.ossWallet.domain.model.LoanType
import com.osscabinet.ossWallet.domain.service.CurrencyService
import com.osscabinet.ossWallet.ui.components.AeonDatePickerDialog
import com.osscabinet.ossWallet.ui.components.AeonHybridSelector
import com.osscabinet.ossWallet.ui.components.AeonHierarchicalHybridSelector
import com.osscabinet.ossWallet.ui.components.AeonSelectMode
import com.osscabinet.ossWallet.ui.components.AeonFullScreenDialog
import com.osscabinet.ossWallet.ui.components.AeonSelectionScreen
import com.osscabinet.ossWallet.ui.components.AccountIconMap
import com.osscabinet.ossWallet.ui.screens.loans.entry.components.LoanActionCards
import com.osscabinet.ossWallet.ui.screens.loans.entry.components.LoanDetailSection
import com.osscabinet.ossWallet.ui.screens.loans.entry.components.LoanHeaderCard
import com.osscabinet.ossWallet.ui.screens.loans.entry.components.LoanTagSelection
import com.osscabinet.ossWallet.ui.screens.transactions.entry.sheets.CalculatorSheet
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
    val profileId by viewModel.currentProfileId.collectAsStateWithLifecycle()
    val existing = remember(loanId, loans) {
        loanId?.let { id -> loans.find { it.id == id } }
    }

    var name by remember(existing) { mutableStateOf(existing?.name ?: "") }
    var principalText by remember(existing) { mutableStateOf(existing?.principalAmount?.toPlainString() ?: "") }
    var currency by remember(existing) { mutableStateOf(existing?.currency ?: "USD") }
    var loanType by remember(existing) { mutableStateOf(existing?.loanType ?: LoanType.BORROWED) }
    var counterparty by remember(existing) { mutableStateOf(existing?.lenderOrBorrowerName ?: "") }
    var interestText by remember(existing) { mutableStateOf(existing?.interestRate?.toString() ?: "") }
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

    val allCurrencies = remember { CurrencyService.getAllCurrencies() }
    val canSave = name.isNotBlank() && principalText.toBigDecimalOrNull() != null

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
                    Button(
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
                        enabled = canSave,
                        modifier = Modifier.padding(end = 8.dp),
                        shape = RoundedCornerShape(12.dp),
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 0.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary,
                            contentColor = Color.White
                        )
                    ) {
                        Icon(Icons.Rounded.Done, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Save", style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold))
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        }
    ) { innerPadding ->

        // Sheets
        if (showStartDatePicker) {
            AeonDatePickerDialog(
                selectedDateMillis = startDateMillis,
                onDateSelected = { startDateMillis = it; showStartDatePicker = false },
                onDismiss = { showStartDatePicker = false }
            )
        }
        if (showSettlementDatePicker) {
            AeonDatePickerDialog(
                selectedDateMillis = settlementDateMillis,
                onDateSelected = { settlementDateMillis = it; showSettlementDatePicker = false },
                onDismiss = { showSettlementDatePicker = false }
            )
        }
        if (showNumpad) {
            CalculatorSheet(
                currentAmount = principalText,
                transactionType = 1, // Positive for principal
                decimalPrecision = 2,
                onAmountChanged = { principalText = it },
                onDismiss = { showNumpad = false }
            )
        }
        if (showInterestNumpad) {
            CalculatorSheet(
                currentAmount = interestText,
                transactionType = 1,
                decimalPrecision = 2,
                onAmountChanged = { interestText = it },
                onDismiss = { showInterestNumpad = false }
            )
        }

        if (showAccountSheet) {
            AeonFullScreenDialog(onDismissRequest = { showAccountSheet = false }) {
                AeonSelectionScreen(
                    title = "Select Account",
                    onBack = { showAccountSheet = false }
                ) { padding ->
                    Column(modifier = Modifier.padding(padding).padding(horizontal = 24.dp)) {
                        Text(
                            text = "Choose the source/destination account for this loan",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                        )
                        Spacer(modifier = Modifier.height(24.dp))
                        Box(modifier = Modifier.weight(1f, fill = false)) {
                            Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                                AeonHybridSelector(
                                    items = accounts,
                                    mode = AeonSelectMode.SINGLE,
                                    selectedIds = setOf(linkedAccountId),
                                    onSelectionChange = { ids ->
                                        ids.firstOrNull()?.let { linkedAccountId = it }
                                        showAccountSheet = false
                                    },
                                    idMapper = { it.id },
                                    labelMapper = { it.name },
                                    detailMapper = { it.currencyCode },
                                    iconMapper = { AccountIconMap[it.icon] },
                                    colorMapper = { Color(android.graphics.Color.parseColor(it.colorHex)) }
                                )
                            }
                        }
                    }
                }
            }
        }

        if (showCategorySheet) {
            AeonFullScreenDialog(onDismissRequest = { showCategorySheet = false }) {
                AeonSelectionScreen(
                    title = "Categories",
                    onBack = { showCategorySheet = false }
                ) { padding ->
                    Column(modifier = Modifier.padding(padding).padding(24.dp)) {
                        Box(modifier = Modifier.weight(1f, fill = false)) {
                            Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                                AeonHierarchicalHybridSelector(
                                    items = categories,
                                    mode = AeonSelectMode.MULTI,
                                    selectedIds = selectedCategoryIds,
                                    onSelectionChange = { selectedCategoryIds = it },
                                    idMapper = { it.id },
                                    labelMapper = { it.name },
                                    parentIdMapper = { it.parentId },
                                    iconMapper = { AccountIconMap[it.iconName] },
                                    colorMapper = { if (it.colorHex == -1) Color(0xFF9E9E9E) else Color(it.colorHex) }
                                )
                            }
                        }
                    }
                }
            }
        }

        if (showTagSheet) {
            val availableTags by viewModel.availableTags.collectAsStateWithLifecycle()
            LoanTagSelection(
                availableTags = availableTags,
                selectedTags = selectedTags,
                onSelectionChanged = { selectedTags = it },
                onAddTag = { tag ->
                    // Tag will be added to selection via onSelectionChanged callback
                },
                onDismiss = { showTagSheet = false }
            )
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Impact Header Card
            LoanHeaderCard(
                loanType = loanType,
                onTypeSelected = { loanType = it },
                name = name,
                onNameChange = { name = it },
                amount = principalText,
                currency = currency,
                onCurrencyChange = { currency = it },
                supportedCurrencies = allCurrencies,
                onAmountClick = { showNumpad = true }
            )

            // Context Action Cards
            LoanActionCards(
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
                selectedTags = selectedTags,
                onTagClick = { showTagSheet = true }
            )

            // Detail Section
            LoanDetailSection(
                counterparty = counterparty,
                onCounterpartyChange = { counterparty = it },
                loanType = loanType,
                note = note,
                onNoteChange = { note = it },
                hasSettlementDate = hasSettlementDate,
                onHasSettlementDateChange = { hasSettlementDate = it }
            )
        }
    }
}
