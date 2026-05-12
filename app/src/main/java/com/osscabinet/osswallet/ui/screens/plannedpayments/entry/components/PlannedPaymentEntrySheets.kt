package com.osscabinet.ossWallet.ui.screens.plannedpayments.entry.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.HelpOutline
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.osscabinet.ossWallet.domain.model.*
import com.osscabinet.ossWallet.domain.service.management.PlannedPaymentService
import com.osscabinet.ossWallet.ui.components.*
import com.osscabinet.ossWallet.ui.components.entry.EntryActionChip
import com.osscabinet.ossWallet.ui.screens.transactions.entry.components.TransactionAccountSelection
import com.osscabinet.ossWallet.ui.screens.transactions.entry.components.TransactionCategorySelection
import com.osscabinet.ossWallet.ui.screens.transactions.entry.sheets.CalculatorSheet
import com.osscabinet.ossWallet.ui.screens.transactions.entry.sheets.PaymentMethodSelection
import com.osscabinet.ossWallet.ui.screens.transactions.entry.sheets.TagSelection
import java.text.SimpleDateFormat
import java.time.Instant
import java.time.ZoneOffset
import java.util.*

@Composable
fun PlannedPaymentEntrySheets(
    showScheduleBuilder: Boolean,
    onDismissScheduleBuilder: () -> Unit,
    startDateMillis: Long,
    recurrenceType: RecurrenceType,
    recurrenceInterval: Int,
    dayOfWeek: Int?,
    dayOfMonth: Int?,
    cronExpression: String?,
    onScheduleConfirm: (Long, RecurrenceType, Int, Int?, Int?, String?) -> Unit,
    showEndDatePicker: Boolean,
    onDismissEndDatePicker: () -> Unit,
    endDateMillis: Long,
    onEndDateSelected: (Long) -> Unit,
    showCategorySheet: Boolean,
    onDismissCategorySheet: () -> Unit,
    categories: List<Category>,
    selectedCategoryId: Long,
    onCategorySelected: (Category) -> Unit,
    showAccountSheet: Boolean,
    onDismissAccountSheet: () -> Unit,
    accounts: List<Account>,
    selectedAccountId: String,
    onAccountSelected: (Account?) -> Unit,
    showPaymentMethodSheet: Boolean,
    onDismissPaymentMethodSheet: () -> Unit,
    paymentMethod: String,
    onPaymentMethodSelected: (String) -> Unit,
    showTagSheet: Boolean,
    onDismissTagSheet: () -> Unit,
    availableTags: List<Tag>,
    selectedTagNames: List<String>,
    onTagsSelected: (List<String>) -> Unit,
    showStatusSheet: Boolean,
    onDismissStatusSheet: () -> Unit,
    status: PlannedPaymentStatus,
    onStatusSelected: (PlannedPaymentStatus) -> Unit,
    showNumpad: Boolean,
    onDismissNumpad: () -> Unit,
    amountText: String,
    transactionType: String,
    onAmountChanged: (String) -> Unit,
    showCurrencySheet: Boolean,
    onDismissCurrencySheet: () -> Unit,
    currency: String,
    onCurrencySelected: (String) -> Unit,
    allCurrencies: List<String>
) {
    if (showScheduleBuilder) {
        ScheduleBuilderSheet(
            initialStartDate = startDateMillis,
            initialRecurrenceType = recurrenceType,
            initialInterval = recurrenceInterval,
            initialDayOfWeek = dayOfWeek,
            initialDayOfMonth = dayOfMonth,
            initialCronExpression = cronExpression,
            onConfirm = onScheduleConfirm,
            onDismiss = onDismissScheduleBuilder
        )
    }
    if (showEndDatePicker) {
        AeonDatePickerDialog(
            selectedDateMillis = endDateMillis,
            onDateSelected = onEndDateSelected,
            onDismiss = onDismissEndDatePicker
        )
    }
    if (showCategorySheet) {
        TransactionCategorySelection(
            categories = categories,
            selectedCategoryId = selectedCategoryId,
            onCategorySelected = { _, id ->
                categories.find { it.id == id }?.let { onCategorySelected(it) }
            },
            onDismiss = onDismissCategorySheet
        )
    }
    if (showAccountSheet) {
        TransactionAccountSelection(
            accounts = accounts,
            selectedAccountId = selectedAccountId,
            onAccountSelected = onAccountSelected,
            onDismiss = onDismissAccountSheet
        )
    }
    if (showPaymentMethodSheet) {
        PaymentMethodSelection(
            selectedMethod = paymentMethod,
            onMethodSelected = onPaymentMethodSelected,
            onDismiss = onDismissPaymentMethodSheet
        )
    }
    if (showTagSheet) {
        TagSelection(
            tags = availableTags,
            selectedTags = selectedTagNames,
            onTagsSelected = onTagsSelected,
            onAddTag = { _, _ -> },
            onEditTag = { },
            onDeleteTag = { },
            onDismiss = onDismissTagSheet
        )
    }
    if (showStatusSheet) {
        PlannedPaymentStatusSelection(
            selectedStatus = status,
            onStatusSelected = onStatusSelected,
            onDismiss = onDismissStatusSheet
        )
    }
    if (showNumpad) {
        CalculatorSheet(
            currentAmount = amountText,
            transactionType = if (transactionType == "income") 1 else 0,
            decimalPrecision = 2,
            onAmountChanged = onAmountChanged,
            onDismiss = onDismissNumpad
        )
    }
    if (showCurrencySheet) {
        AeonFullScreenDialog(onDismissRequest = onDismissCurrencySheet) {
            AeonSelectionScreen(
                title = "Select Currency",
                onBack = onDismissCurrencySheet
            ) { padding ->
                var query by remember { mutableStateOf("") }
                val filtered = remember(query) {
                    if (query.isBlank()) allCurrencies
                    else allCurrencies.filter { it.contains(query, ignoreCase = true) }
                }

                Column(modifier = Modifier.padding(padding).padding(horizontal = 24.dp)) {
                    AeonSearchField(
                        query = query,
                        onQueryChange = { query = it },
                        placeholder = "Search currency..."
                    )
                    Spacer(modifier = Modifier.height(24.dp))
                    Box(modifier = Modifier.weight(1f, fill = false)) {
                        Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                            AeonHybridSelector(
                                items = filtered,
                                mode = AeonSelectMode.SINGLE,
                                selectedIds = setOf(currency),
                                onSelectionChange = { ids ->
                                    ids.firstOrNull()?.let { onCurrencySelected(it) }
                                    onDismissCurrencySheet()
                                },
                                idMapper = { it },
                                labelMapper = { it },
                                iconMapper = { Icons.Rounded.CurrencyExchange }
                            )
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
private fun ScheduleBuilderSheet(
    initialStartDate: Long,
    initialRecurrenceType: RecurrenceType,
    initialInterval: Int,
    initialDayOfWeek: Int?,
    initialDayOfMonth: Int?,
    initialCronExpression: String?,
    onConfirm: (Long, RecurrenceType, Int, Int?, Int?, String?) -> Unit,
    onDismiss: () -> Unit
) {
    var stage by remember { mutableIntStateOf(1) }
    var localStart by remember { mutableLongStateOf(initialStartDate) }
    var localType by remember { mutableStateOf(initialRecurrenceType) }
    var localInterval by remember { mutableIntStateOf(initialInterval) }
    var localDayOfWeek by remember { mutableStateOf(initialDayOfWeek) }
    var localDayOfMonth by remember { mutableStateOf(initialDayOfMonth) }
    var cronText by remember { mutableStateOf(initialCronExpression ?: "") }

    if (stage == 1) {
        val datePickerState = rememberDatePickerState(initialSelectedDateMillis = localStart)
        DatePickerDialog(
            onDismissRequest = onDismiss,
            confirmButton = {
                TextButton(onClick = {
                    localStart = datePickerState.selectedDateMillis ?: localStart
                    stage = 2
                }) { Text("NEXT") }
            },
            dismissButton = { TextButton(onClick = onDismiss) { Text("CANCEL") } },
            shape = RoundedCornerShape(28.dp)
        ) { DatePicker(state = datePickerState) }
    }

    if (stage == 2) {
        data class Preset(val label: String, val type: RecurrenceType, val interval: Int)
        val presets = listOf(
            Preset("One Time", RecurrenceType.ONE_TIME, 1),
            Preset("Daily", RecurrenceType.DAILY, 1),
            Preset("Weekly", RecurrenceType.WEEKLY, 1),
            Preset("Bi-weekly", RecurrenceType.EVERY_N_WEEKS, 2),
            Preset("Monthly", RecurrenceType.MONTHLY, 1),
            Preset("Quarterly", RecurrenceType.EVERY_N_MONTHS, 3),
            Preset("Semi-annually", RecurrenceType.EVERY_N_MONTHS, 6),
            Preset("Yearly", RecurrenceType.YEARLY, 1)
        )
        var selectedTabIndex by remember { mutableIntStateOf(if (localType == RecurrenceType.CRON) 1 else 0) }

        val parsedCronRule =
            remember(cronText) { if (cronText.isNotBlank()) PlannedPaymentService.parseCronToRule(cronText) else null }

        LaunchedEffect(parsedCronRule) {
            parsedCronRule?.let { rule ->
                localType = rule.type
                localInterval = rule.interval
                localDayOfWeek = rule.dayOfWeek
                localDayOfMonth = rule.dayOfMonth
            }
        }

        val needsIntervalLocal = localType in listOf(
            RecurrenceType.EVERY_N_DAYS, RecurrenceType.EVERY_N_WEEKS, RecurrenceType.EVERY_N_MONTHS
        )
        val needsDayOfWeekLocal = localType in listOf(RecurrenceType.WEEKLY, RecurrenceType.EVERY_N_WEEKS)
        val needsDayOfMonthLocal = localType in listOf(RecurrenceType.MONTHLY, RecurrenceType.EVERY_N_MONTHS)

        AlertDialog(
            onDismissRequest = onDismiss,
            shape = RoundedCornerShape(28.dp),
            title = { Text("Frequency", style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)) },
            text = {
                Column(
                    modifier = Modifier.verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    TabRow(selectedTabIndex = selectedTabIndex) {
                        Tab(selected = selectedTabIndex == 0, onClick = { selectedTabIndex = 0 }, text = { Text("PRESETS") })
                        Tab(selected = selectedTabIndex == 1, onClick = { selectedTabIndex = 1 }, text = { Text("CUSTOM") })
                    }

                    if (selectedTabIndex == 0) {
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            presets.forEach { preset ->
                                val isSelected = localType == preset.type && localInterval == preset.interval
                                EntryActionChip(
                                    icon = when(preset.type) {
                                        RecurrenceType.ONE_TIME -> Icons.Rounded.Done
                                        RecurrenceType.DAILY -> Icons.Rounded.Update
                                        RecurrenceType.WEEKLY -> Icons.Rounded.DateRange
                                        RecurrenceType.MONTHLY -> Icons.Rounded.CalendarMonth
                                        RecurrenceType.YEARLY -> Icons.Rounded.EventAvailable
                                        else -> Icons.Rounded.AutoMode
                                    },
                                    label = preset.label,
                                    isSelected = isSelected,
                                    onClick = {
                                        localType = preset.type
                                        localInterval = preset.interval
                                        localDayOfWeek = null
                                        localDayOfMonth = null
                                    }
                                )
                            }
                        }
                    } else {
                        var showCronHelp by remember { mutableStateOf(false) }

                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                OutlinedTextField(
                                    value = cronText,
                                    onValueChange = { cronText = it },
                                    label = { Text("Cron Expression") },
                                    placeholder = { Text("e.g. 0 0 * * *") },
                                    modifier = Modifier.weight(1f),
                                    shape = RoundedCornerShape(12.dp),
                                    singleLine = true,
                                    supportingText = {
                                        Text("Unix format: Min Hour Day Month Weekday")
                                    }
                                )
                                IconButton(
                                    onClick = { showCronHelp = true },
                                    modifier = Modifier.padding(top = 8.dp)
                                ) {
                                    Icon(
                                        Icons.AutoMirrored.Rounded.HelpOutline,
                                        contentDescription = "Cron Help",
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                }
                            }

                            if (showCronHelp) {
                                Surface(
                                    color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.5f),
                                    shape = RoundedCornerShape(12.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Column(modifier = Modifier.padding(12.dp)) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(
                                                "Cron Usage Guide",
                                                style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold)
                                            )
                                            IconButton(
                                                onClick = { showCronHelp = false },
                                                modifier = Modifier.size(24.dp)
                                            ) {
                                                Icon(Icons.Rounded.Close, null, modifier = Modifier.size(16.dp))
                                            }
                                        }
                                        Spacer(modifier = Modifier.height(4.dp))
                                        val helpText = """
                                            Format: min hour day month weekday
                                            • * : any value
                                            • , : value list (1,3,5)
                                            • - : range (1-5)
                                            • / : step values (*/2)
                                            
                                            Examples:
                                            • 0 0 * * * : Daily at midnight
                                            • 0 0 1 * * : 1st of every month
                                            • 0 12 * * 1-5 : Noon on weekdays
                                            • 0 0 */2 * * : Every 2 days
                                        """.trimIndent()
                                        Text(helpText, style = MaterialTheme.typography.bodySmall)
                                    }
                                }
                            }
                        }
                        
                        if (needsIntervalLocal) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text("Every", modifier = Modifier.weight(1f))
                                FilledIconButton(onClick = { localInterval = (localInterval - 1).coerceAtLeast(1) }) {
                                    Icon(Icons.Rounded.Remove, null)
                                }
                                Text(localInterval.toString(), modifier = Modifier.width(40.dp), textAlign = TextAlign.Center)
                                FilledIconButton(onClick = { localInterval = (localInterval + 1).coerceAtLeast(1) }) {
                                    Icon(Icons.Rounded.Add, null)
                                }
                            }
                        }
                    }
                }
            },
            dismissButton = { TextButton(onClick = { stage = 1 }) { Text("BACK") } },
            confirmButton = { TextButton(onClick = { stage = 3 }) { Text("NEXT") } }
        )
    }

    if (stage == 3) {
        val rule = RecurrenceRule(localType, localInterval, localDayOfWeek, localDayOfMonth)
        val summary = PlannedPaymentService.buildScheduleSummary(localStart, rule)
        
        AlertDialog(
            onDismissRequest = onDismiss,
            title = { Text("Confirm Schedule") },
            text = { 
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Verify your recurrence settings before saving.")
                    Surface(
                        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = summary,
                            modifier = Modifier.padding(16.dp),
                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            },
            dismissButton = { TextButton(onClick = { stage = 2 }) { Text("BACK") } },
            confirmButton = {
                TextButton(onClick = {
                    onConfirm(localStart, localType, localInterval, localDayOfWeek, localDayOfMonth, if (localType == RecurrenceType.CRON) cronText else null)
                    onDismiss()
                }) { Text("DONE") }
            }
        )
    }
}

@Composable
private fun PlannedPaymentStatusSelection(
    selectedStatus: PlannedPaymentStatus,
    onStatusSelected: (PlannedPaymentStatus) -> Unit,
    onDismiss: () -> Unit
) {
    val statuses = PlannedPaymentStatus.entries
    AeonFullScreenDialog(onDismissRequest = onDismiss) {
        AeonSelectionScreen(title = "Payment Status", onBack = onDismiss) { padding ->
            Column(modifier = Modifier.padding(padding).padding(horizontal = 24.dp)) {
                AeonHybridSelector(
                    items = statuses,
                    mode = AeonSelectMode.SINGLE,
                    selectedIds = setOf(selectedStatus.name),
                    onSelectionChange = { ids ->
                        statuses.find { it.name == ids.firstOrNull() }?.let {
                            onStatusSelected(it)
                            onDismiss()
                        }
                    },
                    idMapper = { it.name },
                    labelMapper = { it.name.lowercase().replaceFirstChar { it.uppercase() } },
                    iconMapper = {
                        when (it) {
                            PlannedPaymentStatus.ACTIVE -> Icons.Rounded.CheckCircle
                            PlannedPaymentStatus.PAUSED -> Icons.Rounded.PauseCircle
                            PlannedPaymentStatus.COMPLETED -> Icons.Rounded.CheckCircle
                            PlannedPaymentStatus.CANCELLED -> Icons.Rounded.Cancel
                        }
                    },
                    colorMapper = {
                        when (it) {
                            PlannedPaymentStatus.ACTIVE -> Color(0xFF4CAF50)
                            PlannedPaymentStatus.PAUSED -> Color(0xFFFFC107)
                            PlannedPaymentStatus.COMPLETED -> Color(0xFF2196F3)
                            PlannedPaymentStatus.CANCELLED -> Color(0xFFF44336)
                        }
                    }
                )
            }
        }
    }
}
