package com.osscabinet.ossWallet.ui.screens

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.Notes
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.osscabinet.ossWallet.domain.model.*
import com.osscabinet.ossWallet.domain.service.currency.CurrencyService
import com.osscabinet.ossWallet.domain.service.management.PlannedPaymentService
import com.osscabinet.ossWallet.ui.components.*
import com.osscabinet.ossWallet.ui.components.entry.*
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
    transactionViewModel: com.osscabinet.ossWallet.ui.viewmodel.TransactionViewModel? = null,
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
    
    val prefill by transactionViewModel?.plannedPaymentPrefill?.collectAsStateWithLifecycle() ?: remember { mutableStateOf(null) }

    var title by remember { mutableStateOf("") }
    var amountText by remember { mutableStateOf("") }
    var currency by remember { mutableStateOf("USD") }
    var transactionType by remember { mutableStateOf("expense") }
    var selectedAccountId by remember { mutableStateOf("") }
    var recurrenceType by remember { mutableStateOf(RecurrenceType.MONTHLY) }
    var recurrenceInterval by remember { mutableIntStateOf(1) }
    var dayOfWeek by remember { mutableStateOf<Int?>(null) }
    var dayOfMonth by remember { mutableStateOf<Int?>(null) }
    var autoConfirm by remember { mutableStateOf(false) }
    var startDateMillis by remember { mutableLongStateOf(0L) }
    var hasEndDate by remember { mutableStateOf(false) }
    var endDateMillis by remember { mutableLongStateOf(0L) }
    var selectedCategoryId by remember { mutableStateOf<Long?>(null) }
    var payee by remember { mutableStateOf("") }
    var noteUserPart by remember { mutableStateOf("") }
    var paymentMethod by remember { mutableStateOf("Cash") }
    var selectedTagNames by remember { mutableStateOf(emptyList<String>()) }
    var status by remember { mutableStateOf(PlannedPaymentStatus.ACTIVE) }

    LaunchedEffect(existing, prefill) {
        if (existing != null) {
            title = existing.title
            amountText = existing.amount.toPlainString()
            currency = existing.currency
            transactionType = existing.transactionType
            selectedAccountId = existing.accountId
            recurrenceType = existing.recurrenceRule.type
            recurrenceInterval = existing.recurrenceRule.interval
            dayOfWeek = existing.recurrenceRule.dayOfWeek
            dayOfMonth = existing.recurrenceRule.dayOfMonth
            autoConfirm = existing.autoConfirm
            startDateMillis = existing.startDate
            hasEndDate = existing.endDate != null
            endDateMillis = existing.endDate ?: existing.startDate
            selectedCategoryId = existing.categoryId
            payee = existing.payee
            val prefix = "${existing.title} - "
            noteUserPart = if (existing.note.startsWith(prefix)) existing.note.removePrefix(prefix) else existing.note
            paymentMethod = existing.paymentMethod
            selectedTagNames = existing.tags
            status = existing.status
        } else if (prefill != null) {
            title = prefill!!.title
            amountText = prefill!!.amount.abs().toPlainString()
            currency = prefill!!.currency
            transactionType = prefill!!.transactionType
            selectedAccountId = prefill!!.accountId
            recurrenceType = prefill!!.recurrenceRule.type
            recurrenceInterval = prefill!!.recurrenceRule.interval
            dayOfWeek = prefill!!.recurrenceRule.dayOfWeek
            dayOfMonth = prefill!!.recurrenceRule.dayOfMonth
            autoConfirm = true // Smart suggestions default to auto-confirm
            startDateMillis = prefill!!.startDate
            hasEndDate = false
            endDateMillis = prefill!!.startDate
            selectedCategoryId = prefill!!.categoryId
            payee = prefill!!.payee
            noteUserPart = prefill!!.note.removePrefix("${prefill!!.title} - ")
            paymentMethod = prefill!!.paymentMethod
            selectedTagNames = prefill!!.tags
            status = PlannedPaymentStatus.ACTIVE
            
            transactionViewModel?.clearPlannedPaymentPrefill()
        } else {
            // Default values for new entry
            val now = LocalDate.now(ZoneOffset.UTC).atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()
            startDateMillis = now
            endDateMillis = now
            selectedAccountId = accounts.firstOrNull()?.id ?: ""
        }
    }
    val derivedNote = if (noteUserPart.isBlank()) title else "$title - $noteUserPart"

    val isEditing = plannedPaymentId != null

    var showScheduleBuilder by remember { mutableStateOf(false) }
    var showEndDatePicker by remember { mutableStateOf(false) }
    var showCategorySheet by remember { mutableStateOf(false) }
    var showPaymentMethodSheet by remember { mutableStateOf(false) }
    var showTagSheet by remember { mutableStateOf(false) }
    var showStatusSheet by remember { mutableStateOf(false) }
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
                                RecurrenceType.EVERY_N_DAYS,
                                RecurrenceType.EVERY_N_WEEKS,
                                RecurrenceType.EVERY_N_MONTHS
                            )
                            val needsDayOfWeekLocal = recurrenceType in listOf(
                                RecurrenceType.WEEKLY,
                                RecurrenceType.EVERY_N_WEEKS
                            )
                            val needsDayOfMonthLocal = recurrenceType in listOf(
                                RecurrenceType.MONTHLY,
                                RecurrenceType.EVERY_N_MONTHS
                            )
                            val resolvedDayOfMonth = if (needsDayOfMonthLocal) {
                                dayOfMonth ?: Instant.ofEpochMilli(startDateMillis)
                                    .atZone(ZoneOffset.UTC).toLocalDate().dayOfMonth
                            } else null
                            val rule = RecurrenceRule(
                                type = recurrenceType,
                                interval = if (needsIntervalLocal) recurrenceInterval.coerceAtLeast(
                                    1
                                ) else 1,
                                dayOfWeek = if (needsDayOfWeekLocal) dayOfWeek else null,
                                dayOfMonth = resolvedDayOfMonth
                            )
                            val resolvedAccountId = selectedAccountId.ifBlank {
                                accounts.firstOrNull()?.id ?: ""
                            }
                            val payment = (existing ?: viewModel.buildDefault(
                                resolvedAccountId,
                                profileId
                            )).copy(
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
                                nextDueDate = existing?.nextDueDate ?: startDateMillis,
                                status = status
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
                        Icon(
                            Icons.Rounded.Done,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            "Save",
                            style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold)
                        )
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
                onCategorySelected = { _, id ->
                    selectedCategoryId = id; showCategorySheet = false
                },
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
        if (showStatusSheet) {
            PlannedPaymentStatusSelection(
                selectedStatus = status,
                onStatusSelected = { status = it; showStatusSheet = false },
                onDismiss = { showStatusSheet = false }
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
                border = BorderStroke(
                    0.5.dp,
                    MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.15f)
                ),
                shadowElevation = 0.dp
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    EntryEditorialInput(
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
                    EntryTypeSelector(
                        selectedType = transactionType == "income",
                        onTypeSelected = { isIncome ->
                            transactionType = if (isIncome) "income" else "expense"
                        },
                        types = listOf(
                            Triple(Icons.Rounded.Remove, "Expense", false),
                            Triple(Icons.Rounded.Add, "Income", true)
                        ),
                        selectedColor = if (transactionType == "income") Color(0xFF004D40) else Color(0xFFB3261E),
                        itemWidth = 120.dp
                    )

                    Spacer(modifier = Modifier.height(20.dp))

                    // Amount row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        com.osscabinet.ossWallet.ui.components.CurrencySelector(
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
            EntryActionCard(
                icon = Icons.Rounded.DateRange,
                label = "SCHEDULE",
                value = buildScheduleSummary(
                    startDateMillis,
                    recurrenceType,
                    recurrenceInterval,
                    dayOfWeek,
                    dayOfMonth
                ),
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
                    EntryActionCard(
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
                                onClick = {
                                    selectedAccountId = account.id; accountExpanded = false
                                }
                            )
                        }
                    }
                }

                EntryActionCard(
                    icon = Icons.Rounded.Category,
                    label = "CATEGORY",
                    value = categories.find { it.id == selectedCategoryId }?.name ?: "None",
                    onClick = { showCategorySheet = true },
                    modifier = Modifier.weight(1f),
                    iconContainerColor = MaterialTheme.colorScheme.tertiary.copy(alpha = 0.1f),
                    iconTint = MaterialTheme.colorScheme.tertiary
                )
            }

            // Details card
            EntrySection(
                title = "PAYMENT DETAILS",
                icon = Icons.Rounded.MoreHoriz
            ) {
                EntryEditorialInput(
                    value = payee,
                    onValueChange = { payee = it },
                    label = "PAYEE",
                    icon = Icons.Rounded.Person
                )
                EntryEditorialInput(
                    value = noteUserPart,
                    onValueChange = { noteUserPart = it },
                    label = "NOTES",
                    icon = Icons.AutoMirrored.Rounded.Notes,
                    maxLines = 4
                )

                EntryTagSection(
                    selectedTags = selectedTagNames,
                    onShowTagSheet = { showTagSheet = true },
                    onRemoveTag = { tag -> selectedTagNames = selectedTagNames - tag }
                )

                // Row for Payment Method and Status
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    val methods = listOf(
                        "Cash" to Icons.Rounded.Payments,
                        "Credit Card" to Icons.Rounded.CreditCard,
                        "Debit Card" to Icons.Rounded.CreditCard,
                        "Bank Transfer" to Icons.Rounded.AccountBalance,
                        "Digital Wallet" to Icons.Rounded.Wallet,
                        "Mobile Payment" to Icons.Rounded.Contactless,
                        "Check" to Icons.Rounded.Description,
                        "Cryptocurrency" to Icons.Rounded.CurrencyBitcoin,
                        "Other" to Icons.Rounded.MoreHoriz
                    )
                    val paymentIcon = methods.find { it.first == paymentMethod }?.second ?: Icons.Rounded.Contactless

                    EntryActionChip(
                        icon = paymentIcon,
                        label = paymentMethod,
                        onClick = { showPaymentMethodSheet = true },
                        modifier = Modifier.weight(1f)
                    )

                    val (statusIcon, statusColor) = when (status) {
                        PlannedPaymentStatus.ACTIVE -> Icons.Rounded.CheckCircle to Color(0xFF4CAF50)
                        PlannedPaymentStatus.PAUSED -> Icons.Rounded.PauseCircle to Color(0xFFFFC107)
                        PlannedPaymentStatus.COMPLETED -> Icons.Rounded.CheckCircle to Color(0xFF2196F3)
                        PlannedPaymentStatus.CANCELLED -> Icons.Rounded.Cancel to Color(0xFFF44336)
                    }

                    EntryActionChip(
                        icon = statusIcon,
                        label = status.name.lowercase().replaceFirstChar { it.uppercase() },
                        onClick = { showStatusSheet = true },
                        modifier = Modifier.weight(1f),
                        iconContainerColor = statusColor.copy(alpha = 0.1f),
                        iconTint = statusColor
                    )
                }

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
        val parsedCronRule =
            remember(cronText) { if (cronText.isNotBlank()) parseCronToRule(cronText) else null }

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
        val needsDayOfWeekLocal =
            localType in listOf(RecurrenceType.WEEKLY, RecurrenceType.EVERY_N_WEEKS)
        val needsDayOfMonthLocal =
            localType in listOf(RecurrenceType.MONTHLY, RecurrenceType.EVERY_N_MONTHS)

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
                        val onSurfaceVariant = MaterialTheme.colorScheme.onSurfaceVariant
                        val primaryColor = MaterialTheme.colorScheme.primary
                        val errorColor = MaterialTheme.colorScheme.error
                        val outlineVariant = MaterialTheme.colorScheme.outlineVariant
                        val surfaceContainerLowest =
                            MaterialTheme.colorScheme.surfaceContainerLowest

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
                                previewText = "Interpreting as: ${
                                    PlannedPaymentService.formatRecurrenceHuman(parsedCronRule)
                                }"
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
                                    onClick = {
                                        localInterval = (localInterval - 1).coerceAtLeast(1)
                                    },
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
                                    onClick = {
                                        localInterval = (localInterval + 1).coerceAtLeast(1)
                                    },
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
                                FlowRow(
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    val dayNames =
                                        listOf("Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun")
                                    dayNames.forEachIndexed { index, name ->
                                        val dayNum = index + 1
                                        val isSelected = localDayOfWeek == dayNum
                                        EntryActionChip(
                                            icon = Icons.Rounded.Today,
                                            label = name,
                                            isSelected = isSelected,
                                            onClick = {
                                                localDayOfWeek =
                                                    if (localDayOfWeek == dayNum) null else dayNum
                                            }
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
                                    onClick = {
                                        localDayOfMonth = (currentDay - 1).coerceIn(1, 31)
                                    },
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
                                    onClick = {
                                        localDayOfMonth = (currentDay + 1).coerceIn(1, 31)
                                    },
                                    colors = IconButtonDefaults.filledIconButtonColors(
                                        containerColor = MaterialTheme.colorScheme.secondaryContainer
                                    )
                                ) {
                                    Icon(Icons.Rounded.Add, contentDescription = null)
                                }
                            }
                        }
                    }

                    val previewRule =
                        RecurrenceRule(localType, localInterval, localDayOfWeek, localDayOfMonth)
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
        val startFormatted =
            SimpleDateFormat("MMM d, yyyy", Locale.getDefault()).format(Date(localStart))
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
            day.removePrefix("*/").toIntOrNull()
                ?.let { n -> RecurrenceRule(RecurrenceType.EVERY_N_DAYS, interval = n) }

        day.toIntOrNull() != null && month.startsWith("*/") && weekday == "*" ->
            month.removePrefix("*/").toIntOrNull()?.let { n ->
                RecurrenceRule(
                    RecurrenceType.EVERY_N_MONTHS,
                    interval = n,
                    dayOfMonth = day.toIntOrNull()
                )
            }

        else -> null
    }
}

@Composable
fun PlannedPaymentStatusSelection(
    selectedStatus: PlannedPaymentStatus,
    onStatusSelected: (PlannedPaymentStatus) -> Unit,
    onDismiss: () -> Unit
) {
    val statuses = PlannedPaymentStatus.entries

    AeonFullScreenDialog(onDismissRequest = onDismiss) {
        AeonSelectionScreen(
            title = "Payment Status",
            onBack = onDismiss
        ) { padding ->
            Column(
                modifier = Modifier
                    .padding(padding)
                    .padding(horizontal = 24.dp)
            ) {
                Text(
                    text = "Control the activity state of this payment schedule",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                )

                Spacer(modifier = Modifier.height(24.dp))

                Box(modifier = Modifier.weight(1f, fill = false)) {
                    Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                        AeonHybridSelector(
                            items = statuses,
                            mode = AeonSelectMode.SINGLE,
                            selectedIds = setOf(selectedStatus.name),
                            onSelectionChange = { ids ->
                                ids.firstOrNull()?.let { name ->
                                    statuses.find { it.name == name }?.let {
                                        onStatusSelected(it)
                                        onDismiss()
                                    }
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
    }
}
