package com.osscabinet.ossWallet.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.Icons.AutoMirrored.Rounded
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.TrendingDown
import androidx.compose.material.icons.automirrored.rounded.TrendingUp
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Done
import androidx.compose.material.icons.rounded.Update
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.osscabinet.ossWallet.domain.model.Account
import com.osscabinet.ossWallet.domain.model.BudgetPeriod
import com.osscabinet.ossWallet.domain.model.BudgetTrackType
import com.osscabinet.ossWallet.domain.model.Category
import com.osscabinet.ossWallet.domain.service.currency.CurrencyService
import com.osscabinet.ossWallet.ui.components.getAccountIconByName
import com.osscabinet.ossWallet.ui.components.getIconByName
import com.osscabinet.ossWallet.ui.components.AeonDatePickerDialog
import com.osscabinet.ossWallet.ui.components.AeonFullScreenDialog
import com.osscabinet.ossWallet.ui.components.AeonHierarchicalHybridSelector
import com.osscabinet.ossWallet.ui.components.AeonHybridSelector
import com.osscabinet.ossWallet.ui.components.AeonSelectMode
import com.osscabinet.ossWallet.ui.components.AeonSelectionScreen
import com.osscabinet.ossWallet.ui.screens.budgets.entry.components.BudgetActionCards
import com.osscabinet.ossWallet.ui.screens.budgets.entry.components.BudgetDetailSection
import com.osscabinet.ossWallet.ui.screens.budgets.entry.components.BudgetHeaderCard
import com.osscabinet.ossWallet.ui.screens.budgets.entry.components.BudgetTagSelection
import com.osscabinet.ossWallet.ui.screens.transactions.entry.sheets.CalculatorSheet
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
    val profileId by viewModel.currentProfileId.collectAsStateWithLifecycle()
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
                        Icon(Rounded.ArrowBack, contentDescription = "Back")
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
                                id = budgetId
                                    ?: 0L, // Ensure ID is preserved during edit even if 'existing' is not yet loaded
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
            AeonFullScreenDialog(onDismissRequest = { showAccountSheet = false }) {
                AeonSelectionScreen(
                    title = "Select Accounts",
                    onBack = { showAccountSheet = false }
                ) { padding ->
                    Column(modifier = Modifier
                        .padding(padding)
                        .padding(horizontal = 24.dp)) {
                        Text(
                            text = "Choose which accounts to include in this budget",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                        )
                        Spacer(modifier = Modifier.height(24.dp))
                        Box(modifier = Modifier.weight(1f, fill = false)) {
                            Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                                AeonHybridSelector(
                                    items = accounts,
                                    mode = AeonSelectMode.MULTI,
                                    selectedIds = selectedAccountIds,
                                    onSelectionChange = { selectedAccountIds = it },
                                    idMapper = { it.id },
                                    labelMapper = { it.name },
                                    detailMapper = { it.currencyCode },
                                    iconMapper = { getAccountIconByName(it.icon) },
                                    colorMapper = { Color(android.graphics.Color.parseColor(it.colorHex)) }
                                )
                            }
                        }
                    }
                }
            }
        }

        if (showPeriodSheet) {
            AeonFullScreenDialog(onDismissRequest = { showPeriodSheet = false }) {
                AeonSelectionScreen(
                    title = "Reset Period",
                    onBack = { showPeriodSheet = false }
                ) { padding ->
                    Column(modifier = Modifier
                        .padding(padding)
                        .padding(horizontal = 24.dp)) {
                        Text(
                            text = "How often should this budget reset?",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                        )
                        Spacer(modifier = Modifier.height(24.dp))
                        Box(modifier = Modifier.weight(1f, fill = false)) {
                            Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                                AeonHybridSelector(
                                    items = BudgetPeriod.entries,
                                    mode = AeonSelectMode.SINGLE,
                                    selectedIds = setOf(period.ordinal.toLong()),
                                    onSelectionChange = { ids ->
                                        ids.firstOrNull()?.let { ordinal ->
                                            period = BudgetPeriod.entries.getOrNull(ordinal.toInt())
                                                ?: period
                                            if (period == BudgetPeriod.CUSTOM) {
                                                showCustomStartPicker = true
                                            }
                                            showPeriodSheet = false
                                        }
                                    },
                                    idMapper = { it.ordinal.toLong() },
                                    labelMapper = {
                                        it.name.lowercase().replaceFirstChar { c -> c.uppercase() }
                                    },
                                    iconMapper = { Icons.Rounded.Update }
                                )
                            }
                        }
                    }
                }
            }
        }

        if (showTrackTypeSheet) {
            AeonFullScreenDialog(onDismissRequest = { showTrackTypeSheet = false }) {
                AeonSelectionScreen(
                    title = "Track Types",
                    onBack = { showTrackTypeSheet = false }
                ) { padding ->
                    Column(modifier = Modifier
                        .padding(padding)
                        .padding(horizontal = 24.dp)) {
                        Text(
                            text = "Should this budget track expenses, income, or both?",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                        )
                        Spacer(modifier = Modifier.height(24.dp))
                        Box(modifier = Modifier.weight(1f, fill = false)) {
                            Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                                AeonHybridSelector(
                                    items = BudgetTrackType.entries,
                                    mode = AeonSelectMode.MULTI,
                                    selectedIds = selectedTrackTypes.map { it.ordinal.toLong() }
                                        .toSet(),
                                    onSelectionChange = { ids ->
                                        if (ids.isNotEmpty()) {
                                            selectedTrackTypes = ids.mapNotNull { ordinal ->
                                                BudgetTrackType.entries.getOrNull(ordinal.toInt())
                                            }.toSet()
                                        }
                                    },
                                    idMapper = { it.ordinal.toLong() },
                                    labelMapper = {
                                        it.name.lowercase().replaceFirstChar { c -> c.uppercase() }
                                    },
                                    iconMapper = {
                                        when (it) {
                                            BudgetTrackType.EXPENSE -> Rounded.TrendingDown
                                            BudgetTrackType.INCOME -> Rounded.TrendingUp
                                        }
                                    }
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
                    Column(modifier = Modifier
                        .padding(padding)
                        .padding(24.dp)) {
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
                                    iconMapper = { getIconByName(it.iconName) },
                                    colorMapper = {
                                        if (it.colorHex == -1) Color(0xFF9E9E9E) else Color(
                                            it.colorHex
                                        )
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }

        if (showTagSheet) {
            BudgetTagSelection(
                availableTags = availableTags,
                selectedTags = selectedTags,
                onSelectionChanged = { selectedTags = it },
                onAddTag = { /* ViewModel handles derive from transactions, but for quick UX: */
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
                onCategoryClick = { showCategorySheet = true }
            )

            // Detail Section
            BudgetDetailSection(
                note = note,
                onNoteChange = { note = it },
                alertThreshold = alertThreshold,
                onAlertThresholdChange = { alertThreshold = it },
                rollover = rollover,
                onRolloverChange = { rollover = it },
                selectedTags = selectedTags,
                onShowTagSheet = { showTagSheet = true },
                onRemoveTag = { tag -> selectedTags = selectedTags - tag }
            )
        }
    }
}
