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
import com.osscabinet.ossWallet.domain.model.Budget
import com.osscabinet.ossWallet.domain.model.BudgetPeriod
import com.osscabinet.ossWallet.domain.model.BudgetTrackType
import com.osscabinet.ossWallet.domain.model.Category
import com.osscabinet.ossWallet.domain.service.CurrencyService
import com.osscabinet.ossWallet.ui.components.AeonDatePickerDialog
import com.osscabinet.ossWallet.ui.components.AeonMultiSelectPicker
import com.osscabinet.ossWallet.ui.components.MultiSelectOption
import com.osscabinet.ossWallet.ui.screens.budgets.entry.components.BudgetActionCards
import com.osscabinet.ossWallet.ui.screens.budgets.entry.components.BudgetDetailSection
import com.osscabinet.ossWallet.ui.screens.budgets.entry.components.BudgetHeaderCard
import com.osscabinet.ossWallet.ui.screens.budgets.entry.components.BudgetLabelSheet
import com.osscabinet.ossWallet.ui.screens.transactions.entry.sheets.CalculatorSheet
import com.osscabinet.ossWallet.ui.viewmodel.BudgetViewModel
import java.math.BigDecimal
import androidx.compose.material.icons.Icons.AutoMirrored.Rounded
import androidx.compose.material.icons.automirrored.rounded.TrendingDown
import androidx.compose.material.icons.automirrored.rounded.TrendingUp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BudgetEntryScreen(
    budgetId: Long?,
    viewModel: BudgetViewModel,
    accounts: List<Account> = emptyList(),
    categories: List<Category> = emptyList(),
    onBackClick: () -> Unit
) {
    val budgets by viewModel.budgets.collectAsStateWithLifecycle()
    val profileId by viewModel.currentProfileId.collectAsStateWithLifecycle()
    val existing = remember(budgetId, budgets) {
        budgetId?.let { id -> budgets.find { it.id == id } }
    }

    var name by remember(existing) { mutableStateOf(existing?.name ?: "") }
    var limitText by remember(existing) { mutableStateOf(existing?.limitAmount?.toPlainString() ?: "") }
    var currency by remember(existing) { mutableStateOf(existing?.currency ?: "USD") }
    var period by remember(existing) { mutableStateOf(existing?.period ?: BudgetPeriod.MONTHLY) }
    var selectedTrackTypes by remember(existing) {
        mutableStateOf(existing?.trackTypes?.toSet() ?: setOf(BudgetTrackType.EXPENSE))
    }
    var alertThreshold by remember(existing) { mutableFloatStateOf(existing?.alertThreshold ?: 0.8f) }
    var rollover by remember(existing) { mutableStateOf(existing?.rollover ?: false) }
    var note by remember(existing) { mutableStateOf("") } // Placeholder until model updated

    var selectedCategoryIds by remember(existing) {
        mutableStateOf(existing?.categoryIds?.toSet() ?: emptySet<Long>())
    }
    var selectedAccountIds by remember(existing) {
        mutableStateOf(existing?.accountIds?.toSet() ?: emptySet<String>())
    }
    var selectedTags by remember(existing) {
        mutableStateOf(existing?.tags?.toSet() ?: emptySet<String>())
    }
    val availableTags by viewModel.availableTags.collectAsStateWithLifecycle()

    val now = remember { System.currentTimeMillis() }
    var customStartDateMillis by remember(existing) {
        mutableLongStateOf(existing?.customStartDate ?: now)
    }
    var customEndDateMillis by remember(existing) {
        mutableLongStateOf(existing?.customEndDate ?: now)
    }

    val isEditing = budgetId != null
    val haptic = LocalHapticFeedback.current

    var showCustomStartPicker by remember { mutableStateOf(false) }
    var showCustomEndPicker by remember { mutableStateOf(false) }
    var showNumpad by remember { mutableStateOf(false) }
    var showAccountSheet by remember { mutableStateOf(false) }
    var showTrackTypeSheet by remember { mutableStateOf(false) }
    var showCategorySheet by remember { mutableStateOf(false) }
    var showTagSheet by remember { mutableStateOf(false) }
    var showPeriodSheet by remember { mutableStateOf(false) }

    val allCurrencies = remember { CurrencyService.getAllCurrencies() }
    val canSave = name.isNotBlank() && limitText.toBigDecimalOrNull() != null

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(
                        text = if (isEditing) "Edit Budget" else "New Budget",
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
                                viewModel.delete(budgetId!!)
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
                            val limit = limitText.toBigDecimalOrNull() ?: BigDecimal.ZERO
                            val budget = (existing ?: viewModel.buildDefault(profileId)).copy(
                                id = budgetId ?: 0L, // Ensure ID is preserved during edit even if 'existing' is not yet loaded
                                name = name,
                                limitAmount = limit,
                                currency = currency,
                                period = period,
                                trackTypes = selectedTrackTypes.toList().ifEmpty { listOf(BudgetTrackType.EXPENSE) },
                                alertThreshold = alertThreshold,
                                rollover = rollover,
                                categoryIds = selectedCategoryIds.toList(),
                                accountIds = selectedAccountIds.toList(),
                                tags = selectedTags.toList(),
                                customStartDate = if (period == BudgetPeriod.CUSTOM) customStartDateMillis else null,
                                customEndDate = if (period == BudgetPeriod.CUSTOM) customEndDateMillis else null
                            )
                            viewModel.save(budget)
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

        // Sheet Logic
        if (showCustomStartPicker) {
            AeonDatePickerDialog(
                selectedDateMillis = customStartDateMillis,
                onDateSelected = { customStartDateMillis = it; showCustomStartPicker = false },
                onDismiss = { showCustomStartPicker = false }
            )
        }
        if (showCustomEndPicker) {
            AeonDatePickerDialog(
                selectedDateMillis = customEndDateMillis,
                onDateSelected = { customEndDateMillis = it; showCustomEndPicker = false },
                onDismiss = { showCustomEndPicker = false }
            )
        }
        if (showNumpad) {
            CalculatorSheet(
                currentAmount = limitText,
                transactionType = 1, // Positive for budget limit
                decimalPrecision = 2,
                onAmountChanged = { limitText = it },
                onDismiss = { showNumpad = false }
            )
        }

        if (showAccountSheet) {
            AeonMultiSelectPicker(
                title = "Select Accounts",
                description = "Choose which accounts to include in this budget",
                options = accounts.map { 
                    MultiSelectOption(
                        id = it.id, 
                        label = it.name, 
                        detail = it.currencyCode, 
                        icon = Icons.Rounded.AccountBalance,
                        color = Color(android.graphics.Color.parseColor(it.colorHex))
                    ) 
                },
                selectedIds = selectedAccountIds,
                onSelectionChanged = { selectedAccountIds = it },
                onDismiss = { showAccountSheet = false }
            )
        }

        if (showPeriodSheet) {
            AeonMultiSelectPicker(
                title = "Reset Period",
                description = "How often should this budget reset?",
                options = BudgetPeriod.entries.map {
                    MultiSelectOption(
                        id = it.name,
                        label = it.name.lowercase().replaceFirstChar { c -> c.uppercase() },
                        icon = Icons.Rounded.Update
                    )
                },
                selectedIds = setOf(period.name),
                onSelectionChanged = { 
                    it.firstOrNull()?.let { name -> 
                        period = BudgetPeriod.valueOf(name)
                        if (period == BudgetPeriod.CUSTOM) {
                            showCustomStartPicker = true
                        }
                    }
                },
                onDismiss = { showPeriodSheet = false }
            )
        }

        if (showTrackTypeSheet) {
            AeonMultiSelectPicker(
                title = "Track Types",
                description = "Should this budget track expenses, income, or both?",
                options = BudgetTrackType.entries.map {
                    val (label, icon) = when (it) {
                        BudgetTrackType.EXPENSE -> "Expense" to Rounded.TrendingDown
                        BudgetTrackType.INCOME -> "Income" to Rounded.TrendingUp
                    }
                    MultiSelectOption(
                        id = it.name,
                        label = label,
                        icon = icon
                    )
                },
                selectedIds = selectedTrackTypes.map { it.name }.toSet(),
                onSelectionChanged = { ids -> 
                    if (ids.isNotEmpty()) {
                        selectedTrackTypes = ids.map { BudgetTrackType.valueOf(it) }.toSet()
                    }
                },
                onDismiss = { showTrackTypeSheet = false }
            )
        }

        if (showCategorySheet) {
            AeonMultiSelectPicker(
                title = "Categories",
                description = "Filter budget by specific categories",
                options = categories.map {
                    MultiSelectOption(
                        id = it.id.toString(),
                        label = it.name,
                        icon = Icons.Rounded.Category,
                        color = Color(it.colorHex)
                    )
                },
                selectedIds = selectedCategoryIds.map { it.toString() }.toSet(),
                onSelectionChanged = { ids -> selectedCategoryIds = ids.map { it.toLong() }.toSet() },
                onDismiss = { showCategorySheet = false }
            )
        }

        if (showTagSheet) {
            BudgetLabelSheet(
                availableLabels = availableTags,
                selectedLabels = selectedTags,
                onSelectionChanged = { selectedTags = it },
                onAddLabel = { /* ViewModel handles derive from transactions, but for quick UX: */
                     selectedTags = selectedTags + it
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
            BudgetHeaderCard(
                name = name,
                onNameChange = { name = it },
                amount = limitText,
                currency = currency,
                onCurrencyChange = { currency = it },
                supportedCurrencies = allCurrencies,
                onAmountClick = { showNumpad = true }
            )

            // Context Action Cards Grid
            BudgetActionCards(
                period = period,
                onPeriodClick = { showPeriodSheet = true },
                selectedAccountIds = selectedAccountIds,
                accounts = accounts,
                onAccountClick = { showAccountSheet = true },
                selectedTrackTypes = selectedTrackTypes,
                onTrackTypeClick = { showTrackTypeSheet = true },
                selectedCategoryIds = selectedCategoryIds,
                categories = categories,
                onCategoryClick = { showCategorySheet = true },
                selectedTags = selectedTags,
                onTagClick = { showTagSheet = true }
            )

            // Detail Section
            BudgetDetailSection(
                note = note,
                onNoteChange = { note = it },
                alertThreshold = alertThreshold,
                onAlertThresholdChange = { alertThreshold = it },
                rollover = rollover,
                onRolloverChange = { rollover = it }
            )
        }
    }
}
