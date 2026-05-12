package com.osscabinet.ossWallet.ui.screens.budgets.entry

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Done
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.osscabinet.ossWallet.domain.model.*
import com.osscabinet.ossWallet.domain.service.currency.CurrencyService
import com.osscabinet.ossWallet.ui.screens.budgets.entry.components.BudgetEntrySheets
import com.osscabinet.ossWallet.ui.screens.budgets.entry.components.BudgetFormBody
import com.osscabinet.ossWallet.ui.viewmodel.BudgetViewModel
import java.math.BigDecimal

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
    val transactions by viewModel.transactions.collectAsStateWithLifecycle()
    val profileId by viewModel.currentProfileId.collectAsStateWithLifecycle()
    
    val uniqueNotes = remember(transactions) {
        transactions.map { it.note }.filter { it.isNotBlank() }.distinct().sorted()
    }
    val existing = remember(budgetId, budgets) {
        budgetId?.let { id -> budgets.find { it.id == id } }
    }

    var name by remember(existing) { mutableStateOf(existing?.name ?: "") }
    var limitText by remember(existing) {
        mutableStateOf(
            existing?.limitAmount?.toPlainString() ?: ""
        )
    }
    var currency by remember(existing) { mutableStateOf(existing?.currency ?: "USD") }
    var period by remember(existing) { mutableStateOf(existing?.period ?: BudgetPeriod.MONTHLY) }
    var selectedTrackTypes by remember(existing) {
        mutableStateOf(existing?.trackTypes?.toSet() ?: setOf(BudgetTrackType.EXPENSE))
    }
    var alertThreshold by remember(existing) {
        mutableFloatStateOf(
            existing?.alertThreshold ?: 0.8f
        )
    }
    var rollover by remember(existing) { mutableStateOf(existing?.rollover ?: false) }
    var note by remember(existing) { mutableStateOf("") }

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

    var accountQuery by remember { mutableStateOf("") }
    var categoryQuery by remember { mutableStateOf("") }

    val allCurrencies = remember { CurrencyService.getAllCurrencies() }
    val canSave = name.isNotBlank() && 
                 limitText.toBigDecimalOrNull() != null && 
                 selectedCategoryIds.isNotEmpty()

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
                    IconButton(
                        onClick = {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            val limit = limitText.toBigDecimalOrNull() ?: BigDecimal.ZERO
                            val budget = (existing ?: viewModel.buildDefault(profileId)).copy(
                                id = budgetId ?: 0L,
                                name = name,
                                limitAmount = limit,
                                currency = currency,
                                period = period,
                                trackTypes = selectedTrackTypes.toList()
                                    .ifEmpty { listOf(BudgetTrackType.EXPENSE) },
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
        BudgetFormBody(
            name = name,
            onNameChange = { name = it },
            amount = limitText,
            currency = currency,
            onCurrencyChange = { currency = it },
            supportedCurrencies = allCurrencies,
            onAmountClick = { showNumpad = true },
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
            note = note,
            onNoteChange = { note = it },
            alertThreshold = alertThreshold,
            onAlertThresholdChange = { alertThreshold = it },
            rollover = rollover,
            onRolloverChange = { rollover = it },
            selectedTags = selectedTags,
            onShowTagSheet = { showTagSheet = true },
            onRemoveTag = { tag -> selectedTags = selectedTags - tag },
            noteSuggestions = uniqueNotes,
            modifier = Modifier.padding(innerPadding)
        )

        BudgetEntrySheets(
            showCustomStartPicker = showCustomStartPicker,
            onDismissCustomStartPicker = { showCustomStartPicker = false },
            customStartDateMillis = customStartDateMillis,
            onCustomStartDateSelected = { customStartDateMillis = it; showCustomStartPicker = false },
            showCustomEndPicker = showCustomEndPicker,
            onDismissCustomEndPicker = { showCustomEndPicker = false },
            customEndDateMillis = customEndDateMillis,
            onCustomEndDateSelected = { customEndDateMillis = it; showCustomEndPicker = false },
            showNumpad = showNumpad,
            onDismissNumpad = { showNumpad = false },
            limitText = limitText,
            onLimitChanged = { limitText = it },
            showAccountSheet = showAccountSheet,
            onDismissAccountSheet = { showAccountSheet = false },
            accountQuery = accountQuery,
            onAccountQueryChange = { accountQuery = it },
            accounts = accounts,
            selectedAccountIds = selectedAccountIds,
            onAccountSelectionChange = { selectedAccountIds = it },
            showPeriodSheet = showPeriodSheet,
            onDismissPeriodSheet = { showPeriodSheet = false },
            period = period,
            onPeriodSelected = { 
                period = it
                if (period == BudgetPeriod.CUSTOM) {
                    showCustomStartPicker = true
                }
                showPeriodSheet = false
            },
            showTrackTypeSheet = showTrackTypeSheet,
            onDismissTrackTypeSheet = { showTrackTypeSheet = false },
            selectedTrackTypes = selectedTrackTypes,
            onTrackTypeSelectionChange = { selectedTrackTypes = it },
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
