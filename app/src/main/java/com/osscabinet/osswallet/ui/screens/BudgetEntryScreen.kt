package com.osscabinet.ossWallet.ui.screens

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.Label
import androidx.compose.material.icons.automirrored.rounded.Notes
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
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
import com.osscabinet.ossWallet.ui.components.AeonEntryActionCard
import com.osscabinet.ossWallet.ui.components.CurrencySelector
import com.osscabinet.ossWallet.ui.screens.transactions.entry.components.EditorialInput
import com.osscabinet.ossWallet.ui.screens.transactions.entry.sheets.CalculatorSheet
import com.osscabinet.ossWallet.ui.viewmodel.BudgetViewModel
import java.math.BigDecimal
import java.text.SimpleDateFormat
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
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
    var note by remember(existing) { mutableStateOf("") } // Placeholder until model is updated

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

    val allCurrencies = remember { CurrencyService.getAllCurrencies() }
    val canSave = name.isNotBlank()
    val tealColor = Color(0xFF004D40)

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
                transactionType = 0,
                decimalPrecision = 2,
                onAmountChanged = { limitText = it },
                onDismiss = { showNumpad = false }
            )
        }
        if (showAccountSheet) {
            MultiSelectSheet(
                title = "Select Accounts",
                options = accounts.map { it.id to it.name },
                selectedIds = selectedAccountIds,
                onSelectionChanged = { selectedAccountIds = it },
                onDismiss = { showAccountSheet = false }
            )
        }
        if (showTrackTypeSheet) {
            MultiSelectSheet(
                title = "Select Track Types",
                options = BudgetTrackType.entries.map { it.name to it.name.lowercase().replaceFirstChar { c -> c.uppercase() } },
                selectedIds = selectedTrackTypes.map { it.name }.toSet(),
                onSelectionChanged = { ids -> selectedTrackTypes = ids.map { BudgetTrackType.valueOf(it) }.toSet() },
                onDismiss = { showTrackTypeSheet = false }
            )
        }
        if (showCategorySheet) {
            MultiSelectSheet(
                title = "Select Categories",
                options = categories.map { it.id.toString() to it.name },
                selectedIds = selectedCategoryIds.map { it.toString() }.toSet(),
                onSelectionChanged = { ids -> selectedCategoryIds = ids.map { it.toLong() }.toSet() },
                onDismiss = { showCategorySheet = false }
            )
        }
        if (showTagSheet) {
            MultiSelectSheet(
                title = "Select Labels",
                options = availableTags.map { it to it },
                selectedIds = selectedTags,
                onSelectionChanged = { selectedTags = it },
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
            // Header card: budget name + limit amount
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(32.dp),
                color = MaterialTheme.colorScheme.surfaceContainerLow,
                border = BorderStroke(0.5.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.15f)),
                shadowElevation = 0.dp
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    EditorialInput(
                        value = name,
                        onValueChange = { name = it },
                        label = "BUDGET NAME",
                        icon = Icons.Rounded.Title
                    )
                    
                    HorizontalDivider(
                        modifier = Modifier.padding(vertical = 12.dp),
                        thickness = 0.5.dp,
                        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.15f)
                    )

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        CurrencySelector(
                            selectedCurrency = currency,
                            onCurrencySelected = { currency = it },
                            supportedCurrencies = allCurrencies
                        )

                        Spacer(modifier = Modifier.width(16.dp))

                        Box(
                            modifier = Modifier
                                .clickable(
                                    interactionSource = remember { MutableInteractionSource() },
                                    indication = null
                                ) { showNumpad = true }
                        ) {
                            Text(
                                text = if (limitText.isEmpty()) "0.00" else limitText,
                                style = MaterialTheme.typography.displaySmall.copy(
                                    fontWeight = FontWeight.Bold,
                                    letterSpacing = (-1).sp
                                ),
                                color = if (limitText.isEmpty()) tealColor.copy(alpha = 0.3f) else tealColor
                            )
                        }
                    }
                }
            }

            // Row 1: PERIOD card (full width)
            AeonEntryActionCard(
                icon = Icons.Rounded.DateRange,
                label = "PERIOD",
                value = if (period == BudgetPeriod.CUSTOM) {
                    "${formatBudgetEntryDate(customStartDateMillis)} - ${formatBudgetEntryDate(customEndDateMillis)}"
                } else {
                    period.name.lowercase().replaceFirstChar { it.uppercase() }
                },
                onClick = { /* Could open period picker sheet */ },
                modifier = Modifier.fillMaxWidth(),
                iconContainerColor = MaterialTheme.colorScheme.secondary.copy(alpha = 0.1f),
                iconTint = MaterialTheme.colorScheme.secondary
            )
            
            // Inline period selector
            StandardCard {
                Row(
                    modifier = Modifier.padding(12.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    BudgetPeriod.entries.forEach { p ->
                        FilterChip(
                            selected = period == p,
                            onClick = { period = p },
                            label = { Text(p.name.lowercase().replaceFirstChar { it.uppercase() }, style = MaterialTheme.typography.labelSmall) }
                        )
                    }
                }
            }

            // Row 2: Account + Tracks
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                AeonEntryActionCard(
                    icon = Icons.Rounded.AccountBalance,
                    label = "ACCOUNT",
                    value = when {
                        selectedAccountIds.isEmpty() -> "All Accounts"
                        selectedAccountIds.size == 1 -> accounts.find { it.id == selectedAccountIds.first() }?.name ?: "1 Account"
                        else -> "${selectedAccountIds.size} Accounts"
                    },
                    onClick = { showAccountSheet = true },
                    modifier = Modifier.weight(1f),
                    iconContainerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
                    iconTint = MaterialTheme.colorScheme.primary
                )

                AeonEntryActionCard(
                    icon = Icons.Rounded.ShowChart,
                    label = "TRACKS",
                    value = if (selectedTrackTypes.size == BudgetTrackType.entries.size) "All Types"
                            else selectedTrackTypes.joinToString { it.name.lowercase().replaceFirstChar { c -> c.uppercase() } },
                    onClick = { showTrackTypeSheet = true },
                    modifier = Modifier.weight(1f),
                    iconContainerColor = MaterialTheme.colorScheme.tertiary.copy(alpha = 0.1f),
                    iconTint = MaterialTheme.colorScheme.tertiary
                )
            }

            // Row 3: FILTERS (Categories)
            StandardCard {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(bottom = 8.dp)
                    ) {
                        Icon(
                            Icons.Rounded.FilterList,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp),
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "FILTERS",
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                        )
                    }
                    
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        if (selectedCategoryIds.isEmpty()) {
                            SuggestionChip(onClick = { showCategorySheet = true }, label = { Text("All Categories") })
                        } else {
                            selectedCategoryIds.forEach { id ->
                                categories.find { it.id == id }?.let { cat ->
                                    InputChip(
                                        selected = true,
                                        onClick = { selectedCategoryIds = selectedCategoryIds - id },
                                        label = { Text(cat.name) },
                                        trailingIcon = { Icon(Icons.Rounded.Close, null, modifier = Modifier.size(14.dp)) }
                                    )
                                }
                            }
                        }
                        IconButton(
                            onClick = { showCategorySheet = true },
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(Icons.Rounded.Add, null, modifier = Modifier.size(20.dp), tint = MaterialTheme.colorScheme.primary)
                        }
                    }
                }
            }

            // Row 4: TAGS
            StandardCard {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(bottom = 8.dp)
                    ) {
                        Icon(
                            Icons.AutoMirrored.Rounded.Label,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp),
                            tint = MaterialTheme.colorScheme.secondary
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "TAGS",
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                        )
                    }
                    
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        if (selectedTags.isEmpty()) {
                            SuggestionChip(onClick = { showTagSheet = true }, label = { Text("All Tags") })
                        } else {
                            selectedTags.forEach { tag ->
                                InputChip(
                                    selected = true,
                                    onClick = { selectedTags = selectedTags - tag },
                                    label = { Text(tag) },
                                    trailingIcon = { Icon(Icons.Rounded.Close, null, modifier = Modifier.size(14.dp)) }
                                )
                            }
                        }
                        IconButton(
                            onClick = { showTagSheet = true },
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(Icons.Rounded.Add, null, modifier = Modifier.size(20.dp), tint = MaterialTheme.colorScheme.secondary)
                        }
                    }
                }
            }

            // Detail/Settings Card
            StandardCard {
                Column(
                    modifier = Modifier.padding(24.dp),
                    verticalArrangement = Arrangement.spacedBy(20.dp)
                ) {
                    BudgetEntrySectionHeader(icon = Icons.Rounded.Tune, label = "SETTINGS")

                    EditorialInput(
                        value = note,
                        onValueChange = { note = it },
                        label = "NOTES",
                        icon = Icons.AutoMirrored.Rounded.Notes
                    )

                    HorizontalDivider(thickness = 0.5.dp, color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.2f))

                    // Alert threshold
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.secondary.copy(alpha = 0.1f),
                            modifier = Modifier.size(32.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    Icons.Rounded.Shield,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp),
                                    tint = MaterialTheme.colorScheme.secondary
                                )
                            }
                        }
                        Column(modifier = Modifier.weight(1f)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "Alert threshold",
                                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold)
                                    )
                                    Text(
                                        text = "at ${(alertThreshold * 100).toInt()}%",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                                    )
                                }
                            }
                            Slider(
                                value = alertThreshold,
                                onValueChange = { alertThreshold = it },
                                valueRange = 0.5f..1.0f,
                                steps = 9,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }

                    // Rollover toggle
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.secondary.copy(alpha = 0.1f),
                            modifier = Modifier.size(32.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    Icons.Rounded.Autorenew,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp),
                                    tint = MaterialTheme.colorScheme.secondary
                                )
                            }
                        }
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Roll over balance",
                                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold)
                            )
                            Text(
                                text = "Carry unused budget forward",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                            )
                        }
                        Switch(
                            checked = rollover,
                            onCheckedChange = { rollover = it },
                            colors = SwitchDefaults.colors(
                                checkedTrackColor = Color(0xFF004D40)
                            )
                        )
                    }
                }
            }
        }
    }
}

/**
 * Standard container card with 32dp radius and 0dp elevation as per Slate standards.
 */
@Composable
private fun StandardCard(
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    content: @Composable () -> Unit
) {
    val haptic = LocalHapticFeedback.current
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(if (isPressed) 0.98f else 1f, label = "cardScale")

    Surface(
        onClick = {
            onClick?.let {
                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                it()
            }
        },
        enabled = onClick != null,
        interactionSource = interactionSource,
        modifier = modifier
            .fillMaxWidth()
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            },
        shape = RoundedCornerShape(32.dp),
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        border = BorderStroke(0.5.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.15f)),
        shadowElevation = 0.dp,
        content = content
    )
}

@Composable
private fun BudgetEntrySectionHeader(icon: ImageVector, label: String) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Surface(
            shape = CircleShape,
            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
            modifier = Modifier.size(32.dp)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    icon,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
            }
        }
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall.copy(
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.sp,
                fontSize = 10.sp
            ),
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun MultiSelectSheet(
    title: String,
    options: List<Pair<String, String>>, // ID to Display Name
    selectedIds: Set<String>,
    onSelectionChanged: (Set<String>) -> Unit,
    onDismiss: () -> Unit
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
        containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
    ) {
        Column(
            modifier = Modifier
                .padding(horizontal = 24.dp)
                .padding(bottom = 32.dp)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold)
            )
            Spacer(modifier = Modifier.height(16.dp))
            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(options) { (id, label) ->
                    val isSelected = id in selectedIds
                    Surface(
                        onClick = {
                            val newSelected = if (isSelected) selectedIds - id else selectedIds + id
                            onSelectionChanged(newSelected)
                        },
                        shape = RoundedCornerShape(16.dp),
                        color = if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)
                                else MaterialTheme.colorScheme.surfaceContainerLowest,
                        border = BorderStroke(
                            0.5.dp,
                            if (isSelected) MaterialTheme.colorScheme.primary
                            else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.2f)
                        )
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(text = label, modifier = Modifier.weight(1f))
                            Checkbox(
                                checked = isSelected,
                                onCheckedChange = {
                                    val newSelected = if (it == true) selectedIds + id else selectedIds - id
                                    onSelectionChanged(newSelected)
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}

private fun formatBudgetEntryDate(millis: Long): String {
    return SimpleDateFormat("MMM d, yyyy", Locale.getDefault()).format(java.util.Date(millis))
}
