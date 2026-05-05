package com.osscabinet.ossWallet.ui.screens

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.Notes
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.osscabinet.ossWallet.domain.model.Category
import com.osscabinet.ossWallet.domain.model.PlannedPayment
import com.osscabinet.ossWallet.domain.model.RecurrenceRule
import com.osscabinet.ossWallet.domain.model.RecurrenceType
import com.osscabinet.ossWallet.domain.model.Tag
import com.osscabinet.ossWallet.domain.service.CurrencyService
import com.osscabinet.ossWallet.domain.service.PlannedPaymentService
import com.osscabinet.ossWallet.ui.components.AeonDatePickerDialog
import com.osscabinet.ossWallet.ui.components.AeonEntryActionCard
import com.osscabinet.ossWallet.ui.components.CurrencySelector
import com.osscabinet.ossWallet.ui.screens.transactions.entry.components.EditorialInput
import com.osscabinet.ossWallet.ui.screens.transactions.entry.components.TransactionCategorySelection
import com.osscabinet.ossWallet.ui.screens.transactions.entry.sheets.CalculatorSheet
import com.osscabinet.ossWallet.ui.screens.transactions.entry.sheets.PaymentMethodSelection
import com.osscabinet.ossWallet.ui.screens.transactions.entry.sheets.TagSelection
import com.osscabinet.ossWallet.ui.viewmodel.AccountViewModel
import com.osscabinet.ossWallet.ui.viewmodel.PlannedPaymentViewModel
import java.math.BigDecimal
import java.text.SimpleDateFormat
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlannedPaymentEntryScreen(
    plannedPaymentId: Long?,
    viewModel: PlannedPaymentViewModel,
    accountViewModel: AccountViewModel,
    categories: List<Category> = emptyList(),
    availableTags: List<Tag> = emptyList(),
    onBackClick: () -> Unit
) {
    val accounts by accountViewModel.accounts.collectAsStateWithLifecycle()
    val payments by viewModel.plannedPayments.collectAsStateWithLifecycle()
    val profileId by viewModel.currentProfileId.collectAsStateWithLifecycle()
    val existing = remember(plannedPaymentId, payments) {
        plannedPaymentId?.let { id -> payments.find { it.id == id } }
    }

    var title by remember(existing) { mutableStateOf(existing?.title ?: "") }
    var amountText by remember(existing) { mutableStateOf(existing?.amount?.toPlainString() ?: "") }
    var currency by remember(existing) { mutableStateOf(existing?.currency ?: "USD") }
    var transactionType by remember(existing) { mutableStateOf(existing?.transactionType ?: "expense") }
    var selectedAccountId by remember(existing) {
        mutableStateOf(existing?.accountId ?: accounts.firstOrNull()?.id ?: "")
    }
    var recurrenceType by remember(existing) {
        mutableStateOf(existing?.recurrenceRule?.type ?: RecurrenceType.MONTHLY)
    }
    var recurrenceInterval by remember(existing) {
        mutableIntStateOf(existing?.recurrenceRule?.interval ?: 1)
    }
    var dayOfWeek by remember(existing) {
        mutableStateOf(existing?.recurrenceRule?.dayOfWeek)
    }
    var dayOfMonth by remember(existing) {
        mutableStateOf(existing?.recurrenceRule?.dayOfMonth)
    }
    var autoConfirm by remember(existing) { mutableStateOf(existing?.autoConfirm ?: false) }

    val nowEpoch = remember {
        LocalDate.now(ZoneOffset.UTC).atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()
    }
    var startDateMillis by remember(existing) {
        mutableLongStateOf(existing?.startDate ?: nowEpoch)
    }
    var hasEndDate by remember(existing) {
        mutableStateOf(existing?.endDate != null)
    }
    var endDateMillis by remember(existing) {
        mutableLongStateOf(existing?.endDate ?: nowEpoch)
    }
    var selectedCategoryId by remember(existing) {
        mutableStateOf(existing?.categoryId)
    }
    var payee by remember(existing) { mutableStateOf(existing?.payee ?: "") }
    var noteUserPart by remember(existing) {
        val raw = existing?.note ?: ""
        val prefix = "${existing?.title ?: ""} - "
        mutableStateOf(if (raw.startsWith(prefix)) raw.removePrefix(prefix) else raw)
    }
    val derivedNote = if (noteUserPart.isBlank()) title else "$title - $noteUserPart"
    LaunchedEffect(title) { }
    var paymentMethod by remember(existing) { mutableStateOf(existing?.paymentMethod ?: "Cash") }
    var selectedTagNames by remember(existing) {
        mutableStateOf(existing?.tags ?: emptyList<String>())
    }

    val isEditing = plannedPaymentId != null

    var showScheduleBuilder by remember { mutableStateOf(false) }
    var showEndDatePicker by remember { mutableStateOf(false) }
    var showCategorySheet by remember { mutableStateOf(false) }
    var showPaymentMethodSheet by remember { mutableStateOf(false) }
    var showTagSheet by remember { mutableStateOf(false) }
    var showNumpad by remember { mutableStateOf(false) }

    val allCurrencies = remember { CurrencyService.getAllCurrencies() }

    val canSave = title.isNotBlank() && amountText.toBigDecimalOrNull()?.signum() == 1

    val expenseColor = Color(0xFFB3261E)
    val incomeColor = Color(0xFF004D40)
    val amountColor = if (transactionType == "income") incomeColor else expenseColor

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(
                        text = if (isEditing) "Edit Payment" else "New Payment",
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
                                viewModel.delete(plannedPaymentId!!)
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
                            val amount = amountText.toBigDecimalOrNull() ?: BigDecimal.ZERO
                            val needsIntervalLocal = recurrenceType in listOf(
                                RecurrenceType.EVERY_N_DAYS, RecurrenceType.EVERY_N_WEEKS, RecurrenceType.EVERY_N_MONTHS
                            )
                            val needsDayOfWeekLocal = recurrenceType in listOf(RecurrenceType.WEEKLY, RecurrenceType.EVERY_N_WEEKS)
                            val needsDayOfMonthLocal = recurrenceType in listOf(RecurrenceType.MONTHLY, RecurrenceType.EVERY_N_MONTHS)
                            // For MONTHLY/EVERY_N_MONTHS, always record the intended day-of-month
                            // so that clamping logic in calculateNextDueDate restores the correct
                            // day after short months (e.g., Jan 31 → Feb 28 → Mar 31, not Mar 28).
                            val resolvedDayOfMonth = if (needsDayOfMonthLocal) {
                                dayOfMonth ?: Instant.ofEpochMilli(startDateMillis)
                                    .atZone(ZoneOffset.UTC).toLocalDate().dayOfMonth
                            } else null
                            val rule = RecurrenceRule(
                                type = recurrenceType,
                                interval = if (needsIntervalLocal) recurrenceInterval.coerceAtLeast(1) else 1,
                                dayOfWeek = if (needsDayOfWeekLocal) dayOfWeek else null,
                                dayOfMonth = resolvedDayOfMonth
                            )
                            val resolvedAccountId = selectedAccountId.ifBlank {
                                accounts.firstOrNull()?.id ?: ""
                            }
                            val payment = (existing ?: viewModel.buildDefault(resolvedAccountId, profileId)).copy(
                                title = title,
                                amount = amount,
                                currency = currency,
                                transactionType = transactionType,
                                accountId = resolvedAccountId,
                                categoryId = selectedCategoryId,
                                paymentMethod = paymentMethod,
                                tags = selectedTagNames,
                                payee = payee,
                                note = derivedNote,
                                recurrenceRule = rule,
                                autoConfirm = autoConfirm,
                                startDate = startDateMillis,
                                endDate = if (hasEndDate) endDateMillis else null,
                                nextDueDate = existing?.nextDueDate ?: startDateMillis
                            )
                            viewModel.save(payment)
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

        if (showScheduleBuilder) {
            ScheduleBuilderSheet(
                initialStartDate = startDateMillis,
                initialRecurrenceType = recurrenceType,
                initialInterval = recurrenceInterval,
                initialDayOfWeek = dayOfWeek,
                initialDayOfMonth = dayOfMonth,
                onConfirm = { newStart, newType, newInterval, newDow, newDom ->
                    startDateMillis = newStart
                    recurrenceType = newType
                    recurrenceInterval = newInterval
                    dayOfWeek = newDow
                    dayOfMonth = newDom
                },
                onDismiss = { showScheduleBuilder = false }
            )
        }
        if (showEndDatePicker) {
            AeonDatePickerDialog(
                selectedDateMillis = endDateMillis,
                onDateSelected = { endDateMillis = it; showEndDatePicker = false },
                onDismiss = { showEndDatePicker = false }
            )
        }
        if (showCategorySheet) {
            TransactionCategorySelection(
                categories = categories,
                selectedCategoryId = selectedCategoryId,
                onCategorySelected = { _, id -> selectedCategoryId = id; showCategorySheet = false },
                onDismiss = { showCategorySheet = false }
            )
        }
        if (showPaymentMethodSheet) {
            PaymentMethodSelection(
                selectedMethod = paymentMethod,
                onMethodSelected = { paymentMethod = it; showPaymentMethodSheet = false },
                onDismiss = { showPaymentMethodSheet = false }
            )
        }
        if (showTagSheet) {
            TagSelection(
                tags = availableTags,
                selectedTags = selectedTagNames,
                onTagsSelected = { selectedTagNames = it; showTagSheet = false },
                onAddTag = { },
                onDismiss = { showTagSheet = false }
            )
        }
        if (showNumpad) {
            CalculatorSheet(
                currentAmount = amountText,
                transactionType = if (transactionType == "income") 1 else 0,
                decimalPrecision = 2,
                onAmountChanged = { amountText = it },
                onDismiss = { showNumpad = false }
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
            // Header card: type selector + amount
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
                        value = title,
                        onValueChange = { title = it },
                        label = "PAYMENT NAME",
                        icon = Icons.Rounded.Title
                    )
                    HorizontalDivider(
                        modifier = Modifier.padding(vertical = 4.dp),
                        thickness = 0.5.dp,
                        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.15f)
                    )
                    PaymentTypeSelector(
                        isIncome = transactionType == "income",
                        onTypeSelected = { isIncome -> transactionType = if (isIncome) "income" else "expense" }
                    )

                    Spacer(modifier = Modifier.height(20.dp))

                    // Amount row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
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
                                .weight(1f, fill = false)
                                .clickable(
                                    interactionSource = remember { MutableInteractionSource() },
                                    indication = null
                                ) { showNumpad = true }
                        ) {
                            Text(
                                text = if (amountText.isEmpty()) "0.00" else amountText,
                                style = MaterialTheme.typography.displaySmall.copy(
                                    fontWeight = FontWeight.Bold,
                                    letterSpacing = (-1).sp
                                ),
                                color = if (amountText.isEmpty()) amountColor.copy(alpha = 0.3f) else amountColor
                            )
                        }
                    }
                }
            }

            // Row 1: SCHEDULE card (full width)
            AeonEntryActionCard(
                icon = Icons.Rounded.DateRange,
                label = "SCHEDULE",
                value = buildScheduleSummary(startDateMillis, recurrenceType, recurrenceInterval, dayOfWeek, dayOfMonth),
                onClick = { showScheduleBuilder = true },
                modifier = Modifier.fillMaxWidth(),
                iconContainerColor = MaterialTheme.colorScheme.secondary.copy(alpha = 0.1f),
                iconTint = MaterialTheme.colorScheme.secondary
            )

            // Row 2: Account + Category
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                var accountExpanded by remember { mutableStateOf(false) }
                ExposedDropdownMenuBox(
                    expanded = accountExpanded,
                    onExpandedChange = { accountExpanded = it },
                    modifier = Modifier.weight(1f)
                ) {
                    AeonEntryActionCard(
                        icon = Icons.Rounded.AccountBalance,
                        label = "ACCOUNT",
                        value = accounts.find { it.id == selectedAccountId }?.name ?: "Select",
                        onClick = { accountExpanded = true },
                        modifier = Modifier
                            .fillMaxWidth()
                            .menuAnchor(),
                        iconContainerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
                        iconTint = MaterialTheme.colorScheme.primary
                    )
                    ExposedDropdownMenu(
                        expanded = accountExpanded,
                        onDismissRequest = { accountExpanded = false }
                    ) {
                        accounts.forEach { account ->
                            DropdownMenuItem(
                                text = { Text(account.name) },
                                onClick = { selectedAccountId = account.id; accountExpanded = false }
                            )
                        }
                    }
                }

                AeonEntryActionCard(
                    icon = Icons.Rounded.Category,
                    label = "CATEGORY",
                    value = categories.find { it.id == selectedCategoryId }?.name ?: "None",
                    onClick = { showCategorySheet = true },
                    modifier = Modifier.weight(1f),
                    iconContainerColor = MaterialTheme.colorScheme.tertiary.copy(alpha = 0.1f),
                    iconTint = MaterialTheme.colorScheme.tertiary
                )
            }

            // Row 3: Payment Method + Tags
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                AeonEntryActionCard(
                    icon = Icons.Rounded.Payments,
                    label = "PAYMENT METHOD",
                    value = paymentMethod,
                    onClick = { showPaymentMethodSheet = true },
                    modifier = Modifier.weight(1f),
                    iconContainerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
                    iconTint = MaterialTheme.colorScheme.primary
                )
                AeonEntryActionCard(
                    icon = Icons.Rounded.LocalOffer,
                    label = "TAGS",
                    value = if (selectedTagNames.isEmpty()) "None"
                            else selectedTagNames.take(2).joinToString(", ") +
                                 if (selectedTagNames.size > 2) " +${selectedTagNames.size - 2}" else "",
                    onClick = { showTagSheet = true },
                    modifier = Modifier.weight(1f),
                    iconContainerColor = MaterialTheme.colorScheme.secondary.copy(alpha = 0.1f),
                    iconTint = MaterialTheme.colorScheme.secondary
                )
            }

            // Details card
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(32.dp),
                color = MaterialTheme.colorScheme.surfaceContainerLow,
                border = BorderStroke(0.5.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.15f)),
                shadowElevation = 0.dp
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    verticalArrangement = Arrangement.spacedBy(20.dp)
                ) {
                    EntrySectionHeader(icon = Icons.Rounded.MoreHoriz, label = "PAYMENT DETAILS")

                    EditorialInput(
                        value = payee,
                        onValueChange = { payee = it },
                        label = "PAYEE",
                        icon = Icons.Rounded.Person
                    )
                    EditorialInput(
                        value = noteUserPart,
                        onValueChange = { noteUserPart = it },
                        label = "NOTES",
                        icon = Icons.AutoMirrored.Rounded.Notes,
                        maxLines = 4
                    )

                    HorizontalDivider(
                        thickness = 0.5.dp,
                        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.2f)
                    )

                    // End date toggle row
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
                                    Icons.Rounded.EventBusy,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp),
                                    tint = MaterialTheme.colorScheme.secondary
                                )
                            }
                        }
                        Text(
                            text = "Set end date",
                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                            modifier = Modifier.weight(1f)
                        )
                        Switch(
                            checked = hasEndDate,
                            onCheckedChange = { hasEndDate = it },
                            colors = SwitchDefaults.colors(
                                checkedTrackColor = Color(0xFF004D40)
                            )
                        )
                    }

                    if (hasEndDate) {
                        EntryDateCard(
                            icon = Icons.Rounded.CalendarMonth,
                            label = "END DATE",
                            dateMillis = endDateMillis,
                            onClick = { showEndDatePicker = true }
                        )
                    }

                    // Auto-confirm toggle row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
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
                                    Icons.Rounded.AutoMode,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp),
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Auto-confirm",
                                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold)
                            )
                            Text(
                                text = "Automatically log when due",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                            )
                        }
                        Switch(
                            checked = autoConfirm,
                            onCheckedChange = { autoConfirm = it },
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

private fun buildScheduleSummary(
    startDateMillis: Long,
    recurrenceType: RecurrenceType,
    recurrenceInterval: Int,
    dayOfWeek: Int?,
    dayOfMonth: Int?
): String {
    val rule = RecurrenceRule(recurrenceType, recurrenceInterval, dayOfWeek, dayOfMonth)
    val recurrenceLabel = PlannedPaymentService.formatRecurrenceHuman(rule)
    val startLabel = SimpleDateFormat("MMM d", Locale.getDefault()).format(Date(startDateMillis))
    return "$recurrenceLabel · Starts $startLabel"
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
private fun ScheduleBuilderSheet(
    initialStartDate: Long,
    initialRecurrenceType: RecurrenceType,
    initialInterval: Int,
    initialDayOfWeek: Int?,
    initialDayOfMonth: Int?,
    onConfirm: (startDate: Long, type: RecurrenceType, interval: Int, dayOfWeek: Int?, dayOfMonth: Int?) -> Unit,
    onDismiss: () -> Unit
) {
    var stage by remember { mutableIntStateOf(1) }
    var localStart by remember { mutableLongStateOf(initialStartDate) }
    var localType by remember { mutableStateOf(initialRecurrenceType) }
    var localInterval by remember { mutableIntStateOf(initialInterval) }
    var localDayOfWeek by remember { mutableStateOf(initialDayOfWeek) }
    var localDayOfMonth by remember { mutableStateOf(initialDayOfMonth) }

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
        var selectedTabIndex by remember { mutableIntStateOf(0) }
        var cronText by remember { mutableStateOf("") }
        val parsedCronRule = remember(cronText) { if (cronText.isNotBlank()) parseCronToRule(cronText) else null }

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
            title = {
                Text(
                    "Frequency",
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
                )
            },
            text = {
                Column(
                    modifier = Modifier.verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    TabRow(selectedTabIndex = selectedTabIndex) {
                        Tab(
                            selected = selectedTabIndex == 0,
                            onClick = { selectedTabIndex = 0 },
                            text = { Text("PRESETS") }
                        )
                        Tab(
                            selected = selectedTabIndex == 1,
                            onClick = { selectedTabIndex = 1 },
                            text = { Text("CUSTOM") }
                        )
                    }

                    if (selectedTabIndex == 0) {
                        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            presets.forEach { preset ->
                                FilterChip(
                                    selected = localType == preset.type && localInterval == preset.interval,
                                    onClick = {
                                        localType = preset.type
                                        localInterval = preset.interval
                                        localDayOfWeek = null
                                        localDayOfMonth = null
                                    },
                                    label = { Text(preset.label) }
                                )
                            }
                        }
                    } else {
                        val onSurfaceVariant = MaterialTheme.colorScheme.onSurfaceVariant
                        val primaryColor = MaterialTheme.colorScheme.primary
                        val errorColor = MaterialTheme.colorScheme.error
                        val outlineVariant = MaterialTheme.colorScheme.outlineVariant
                        val surfaceContainerLowest = MaterialTheme.colorScheme.surfaceContainerLowest

                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = surfaceContainerLowest,
                            border = BorderStroke(0.5.dp, outlineVariant.copy(alpha = 0.3f))
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp, vertical = 12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                BasicTextField(
                                    value = cronText,
                                    onValueChange = { cronText = it },
                                    modifier = Modifier.weight(1f),
                                    textStyle = MaterialTheme.typography.bodyMedium.copy(
                                        color = onSurfaceVariant
                                    ),
                                    cursorBrush = SolidColor(primaryColor),
                                    singleLine = true,
                                    decorationBox = { inner ->
                                        if (cronText.isEmpty()) {
                                            Text(
                                                text = "e.g. 0 9 1 * *",
                                                style = MaterialTheme.typography.bodyMedium,
                                                color = onSurfaceVariant.copy(alpha = 0.4f)
                                            )
                                        }
                                        inner()
                                    }
                                )
                                Icon(
                                    Icons.Rounded.Info,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp),
                                    tint = onSurfaceVariant.copy(alpha = 0.4f)
                                )
                            }
                        }

                        if (cronText.isNotBlank()) {
                            val previewText: String
                            val previewColor: Color
                            if (parsedCronRule != null) {
                                previewText = "Interpreting as: ${PlannedPaymentService.formatRecurrenceHuman(parsedCronRule)}"
                                previewColor = primaryColor
                            } else {
                                previewText = "Unrecognized pattern — using current settings"
                                previewColor = errorColor
                            }
                            Text(
                                text = previewText,
                                style = MaterialTheme.typography.bodySmall,
                                color = previewColor
                            )
                        }

                        if (needsIntervalLocal) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(
                                    text = "Every",
                                    style = MaterialTheme.typography.bodyMedium,
                                    modifier = Modifier.weight(1f)
                                )
                                FilledIconButton(
                                    onClick = { localInterval = (localInterval - 1).coerceAtLeast(1) },
                                    colors = IconButtonDefaults.filledIconButtonColors(
                                        containerColor = MaterialTheme.colorScheme.secondaryContainer
                                    )
                                ) {
                                    Icon(Icons.Rounded.Remove, contentDescription = null)
                                }
                                Text(
                                    text = localInterval.toString(),
                                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                    modifier = Modifier.width(40.dp),
                                    textAlign = TextAlign.Center
                                )
                                FilledIconButton(
                                    onClick = { localInterval = (localInterval + 1).coerceAtLeast(1) },
                                    colors = IconButtonDefaults.filledIconButtonColors(
                                        containerColor = MaterialTheme.colorScheme.secondaryContainer
                                    )
                                ) {
                                    Icon(Icons.Rounded.Add, contentDescription = null)
                                }
                            }
                        }

                        if (needsDayOfWeekLocal) {
                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Text(
                                    text = "DAY OF WEEK",
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontWeight = FontWeight.Bold,
                                        letterSpacing = 0.5.sp,
                                        fontSize = 10.sp
                                    ),
                                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                                )
                                FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                    val dayNames = listOf("Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun")
                                    dayNames.forEachIndexed { index, name ->
                                        val dayNum = index + 1
                                        FilterChip(
                                            selected = localDayOfWeek == dayNum,
                                            onClick = { localDayOfWeek = if (localDayOfWeek == dayNum) null else dayNum },
                                            label = { Text(name) }
                                        )
                                    }
                                }
                            }
                        }

                        if (needsDayOfMonthLocal) {
                            val currentDay = localDayOfMonth ?: 1
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(
                                    text = "Day of month",
                                    style = MaterialTheme.typography.bodyMedium,
                                    modifier = Modifier.weight(1f)
                                )
                                FilledIconButton(
                                    onClick = { localDayOfMonth = (currentDay - 1).coerceIn(1, 31) },
                                    colors = IconButtonDefaults.filledIconButtonColors(
                                        containerColor = MaterialTheme.colorScheme.secondaryContainer
                                    )
                                ) {
                                    Icon(Icons.Rounded.Remove, contentDescription = null)
                                }
                                Text(
                                    text = currentDay.toString(),
                                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                    modifier = Modifier.width(40.dp),
                                    textAlign = TextAlign.Center
                                )
                                FilledIconButton(
                                    onClick = { localDayOfMonth = (currentDay + 1).coerceIn(1, 31) },
                                    colors = IconButtonDefaults.filledIconButtonColors(
                                        containerColor = MaterialTheme.colorScheme.secondaryContainer
                                    )
                                ) {
                                    Icon(Icons.Rounded.Add, contentDescription = null)
                                }
                            }
                        }
                    }

                    val previewRule = RecurrenceRule(localType, localInterval, localDayOfWeek, localDayOfMonth)
                    Text(
                        text = PlannedPaymentService.formatRecurrenceHuman(previewRule),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            },
            dismissButton = {
                TextButton(onClick = { stage = 1 }) { Text("BACK") }
            },
            confirmButton = {
                TextButton(onClick = { stage = 3 }) { Text("NEXT") }
            }
        )
    }

    if (stage == 3) {
        val startFormatted = SimpleDateFormat("MMM d, yyyy", Locale.getDefault()).format(Date(localStart))
        val rule = RecurrenceRule(localType, localInterval, localDayOfWeek, localDayOfMonth)
        val repeatsLabel = PlannedPaymentService.formatRecurrenceHuman(rule)
        val nextOccurrence = if (localType == RecurrenceType.ONE_TIME) {
            startFormatted
        } else {
            val from = Instant.ofEpochMilli(localStart).atZone(ZoneOffset.UTC).toLocalDate()
            val nextLocal = when (rule.type) {
                RecurrenceType.DAILY, RecurrenceType.EVERY_N_DAYS -> from.plusDays(rule.interval.toLong())
                RecurrenceType.WEEKLY, RecurrenceType.EVERY_N_WEEKS -> from.plusWeeks(rule.interval.toLong())
                RecurrenceType.MONTHLY, RecurrenceType.EVERY_N_MONTHS -> {
                    val candidate = from.plusMonths(rule.interval.toLong())
                    rule.dayOfMonth?.let { day ->
                        candidate.withDayOfMonth(day.coerceAtMost(candidate.lengthOfMonth()))
                    } ?: candidate
                }
                RecurrenceType.YEARLY -> from.plusYears(rule.interval.toLong())
                RecurrenceType.ONE_TIME -> from
            }
            val nextMillis = nextLocal.atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()
            SimpleDateFormat("MMM d, yyyy", Locale.getDefault()).format(Date(nextMillis))
        }
        AlertDialog(
            onDismissRequest = onDismiss,
            shape = RoundedCornerShape(28.dp),
            title = {
                Text(
                    "Confirm Schedule",
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
                )
            },
            text = {
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = MaterialTheme.colorScheme.surfaceContainerHighest
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "Starting",
                                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold)
                            )
                            Text(
                                text = startFormatted,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        HorizontalDivider(
                            thickness = 0.5.dp,
                            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "Repeats",
                                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold)
                            )
                            Text(
                                text = repeatsLabel,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        HorizontalDivider(
                            thickness = 0.5.dp,
                            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "Next occurrence",
                                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold)
                            )
                            Text(
                                text = nextOccurrence,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }
            },
            dismissButton = {
                TextButton(onClick = { stage = 2 }) { Text("BACK") }
            },
            confirmButton = {
                TextButton(onClick = {
                    onConfirm(localStart, localType, localInterval, localDayOfWeek, localDayOfMonth)
                    onDismiss()
                }) { Text("DONE") }
            }
        )
    }
}

private fun parseCronToRule(cron: String): RecurrenceRule? {
    val parts = cron.trim().split("\\s+".toRegex())
    if (parts.size != 5) return null
    val (_, _, day, month, weekday) = parts
    return when {
        day == "*" && month == "*" && weekday == "*" -> RecurrenceRule(RecurrenceType.DAILY)
        day == "*" && month == "*" && weekday.toIntOrNull() != null -> {
            val dow = weekday.toInt().let { if (it == 0 || it == 7) 7 else it }
            RecurrenceRule(RecurrenceType.WEEKLY, dayOfWeek = dow)
        }
        day.toIntOrNull() != null && month == "*" && weekday == "*" ->
            RecurrenceRule(RecurrenceType.MONTHLY, dayOfMonth = day.toInt())
        day.toIntOrNull() != null && month.toIntOrNull() != null && weekday == "*" ->
            RecurrenceRule(RecurrenceType.YEARLY)
        day.startsWith("*/") && month == "*" && weekday == "*" ->
            day.removePrefix("*/").toIntOrNull()?.let { n -> RecurrenceRule(RecurrenceType.EVERY_N_DAYS, interval = n) }
        day.toIntOrNull() != null && month.startsWith("*/") && weekday == "*" ->
            month.removePrefix("*/").toIntOrNull()?.let { n ->
                RecurrenceRule(RecurrenceType.EVERY_N_MONTHS, interval = n, dayOfMonth = day.toIntOrNull())
            }
        else -> null
    }
}

@Composable
private fun PaymentTypeSelector(
    isIncome: Boolean,
    onTypeSelected: (Boolean) -> Unit
) {
    val expenseColor = Color(0xFFB3261E)
    val incomeColor = Color(0xFF004D40)

    val indicatorColor by animateColorAsState(
        targetValue = if (isIncome) incomeColor else expenseColor,
        animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
        label = "indicatorColor"
    )

    val indicatorOffset by animateDpAsState(
        targetValue = if (isIncome) 120.dp else 0.dp,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioLowBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "indicatorOffset"
    )

    Box(
        modifier = Modifier
            .height(44.dp)
            .clip(RoundedCornerShape(32.dp))
            .background(MaterialTheme.colorScheme.surfaceContainerLowest)
            .border(
                0.5.dp,
                MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.15f),
                RoundedCornerShape(32.dp)
            )
            .padding(4.dp)
    ) {
        Box(
            modifier = Modifier
                .offset(x = indicatorOffset)
                .width(120.dp)
                .fillMaxHeight()
                .clip(CircleShape)
                .background(indicatorColor)
        )

        Row(modifier = Modifier.fillMaxHeight()) {
            listOf(
                false to (Icons.Rounded.Remove to "Expense"),
                true to (Icons.Rounded.Add to "Income")
            ).forEach { (typeIsIncome, iconLabel) ->
                val (icon, label) = iconLabel
                val isSelected = isIncome == typeIsIncome
                val scale by animateFloatAsState(
                    targetValue = if (isSelected) 1.1f else 1.0f,
                    animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
                    label = "scale"
                )
                Row(
                    modifier = Modifier
                        .width(120.dp)
                        .fillMaxHeight()
                        .clip(CircleShape)
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null
                        ) { onTypeSelected(typeIsIncome) },
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = label,
                        tint = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                        modifier = Modifier
                            .size(20.dp)
                            .graphicsLayer(scaleX = scale, scaleY = scale)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = label,
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                        color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                        modifier = Modifier.graphicsLayer(scaleX = scale, scaleY = scale)
                    )
                }
            }
        }
    }
}

@Composable
private fun EntrySectionHeader(icon: ImageVector, label: String) {
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

@Composable
private fun EntryDateCard(
    icon: ImageVector,
    label: String,
    dateMillis: Long,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surfaceContainerLowest,
        border = BorderStroke(0.5.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.2f))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                icon,
                contentDescription = null,
                modifier = Modifier.size(20.dp),
                tint = MaterialTheme.colorScheme.secondary
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                )
                Text(
                    text = formatEntryDate(dateMillis),
                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold)
                )
            }
            Icon(
                Icons.Rounded.ChevronRight,
                contentDescription = null,
                modifier = Modifier.size(20.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
            )
        }
    }
}

private fun formatEntryDate(millis: Long): String {
    return SimpleDateFormat("MMM d, yyyy", Locale.getDefault()).format(java.util.Date(millis))
}
