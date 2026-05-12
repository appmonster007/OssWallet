package com.osscabinet.ossWallet.ui.screens.plannedpayments.entry

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Done
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.osscabinet.ossWallet.domain.model.*
import com.osscabinet.ossWallet.domain.service.currency.CurrencyService
import com.osscabinet.ossWallet.domain.service.management.PlannedPaymentService
import com.osscabinet.ossWallet.ui.screens.plannedpayments.entry.components.PlannedPaymentEntrySheets
import com.osscabinet.ossWallet.ui.screens.plannedpayments.entry.components.PlannedPaymentFormBody
import com.osscabinet.ossWallet.ui.viewmodel.AccountViewModel
import com.osscabinet.ossWallet.ui.viewmodel.PlannedPaymentViewModel
import java.math.BigDecimal
import java.text.SimpleDateFormat
import java.time.LocalDate
import java.time.ZoneOffset
import java.util.*

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
    val uniquePayees by transactionViewModel?.uniquePayees?.collectAsStateWithLifecycle() ?: remember { mutableStateOf(emptyList()) }
    val uniqueNotes by transactionViewModel?.uniqueNotes?.collectAsStateWithLifecycle() ?: remember { mutableStateOf(emptyList()) }

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
    var selectedCategoryId by remember { mutableLongStateOf(1002L) }
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
            autoConfirm = true
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
            val now = LocalDate.now(ZoneOffset.UTC).atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()
            startDateMillis = now
            endDateMillis = now
            selectedAccountId = accounts.firstOrNull()?.id ?: ""
        }
    }

    val isEditing = plannedPaymentId != null

    var showScheduleBuilder by remember { mutableStateOf(false) }
    var showEndDatePicker by remember { mutableStateOf(false) }
    var showCategorySheet by remember { mutableStateOf(false) }
    var showAccountSheet by remember { mutableStateOf(false) }
    var showPaymentMethodSheet by remember { mutableStateOf(false) }
    var showTagSheet by remember { mutableStateOf(false) }
    var showStatusSheet by remember { mutableStateOf(false) }
    var showNumpad by remember { mutableStateOf(false) }
    var showCurrencySheet by remember { mutableStateOf(false) }

    val allCurrencies = remember { CurrencyService.getAllCurrencies() }
    val canSave = title.isNotBlank() && amountText.toBigDecimalOrNull()?.signum() == 1

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
                    IconButton(
                        onClick = {
                            val amount = amountText.toBigDecimalOrNull() ?: BigDecimal.ZERO
                            val rule = RecurrenceRule(
                                type = recurrenceType,
                                interval = recurrenceInterval.coerceAtLeast(1),
                                dayOfWeek = dayOfWeek,
                                dayOfMonth = dayOfMonth
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
                                note = if (noteUserPart.isBlank()) title else "$title - $noteUserPart",
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
        PlannedPaymentFormBody(
            title = title,
            onTitleChange = { title = it },
            transactionType = transactionType,
            onTypeSelected = { isIncome ->
                transactionType = if (isIncome) "income" else "expense"
            },
            amountText = amountText,
            currency = currency,
            onCurrencyClick = { showCurrencySheet = true },
            onAmountClick = { showNumpad = true },
            scheduleSummary = buildScheduleSummary(startDateMillis, recurrenceType, recurrenceInterval, dayOfWeek, dayOfMonth),
            onScheduleClick = { showScheduleBuilder = true },
            selectedAccount = accounts.find { it.id == selectedAccountId },
            onAccountClick = { showAccountSheet = true },
            categoryObj = categories.find { it.id == selectedCategoryId },
            onCategoryClick = { showCategorySheet = true },
            payee = payee,
            onPayeeChange = { payee = it },
            uniquePayees = uniquePayees,
            noteUserPart = noteUserPart,
            onNoteUserPartChange = { noteUserPart = it },
            uniqueNotes = uniqueNotes,
            selectedTagNames = selectedTagNames,
            onShowTagSheet = { showTagSheet = true },
            onRemoveTag = { tag -> selectedTagNames = selectedTagNames - tag },
            paymentMethod = paymentMethod,
            onPaymentMethodClick = { showPaymentMethodSheet = true },
            status = status,
            onStatusClick = { showStatusSheet = true },
            hasEndDate = hasEndDate,
            onHasEndDateChange = { hasEndDate = it },
            endDateMillis = endDateMillis,
            onEndDateClick = { showEndDatePicker = true },
            autoConfirm = autoConfirm,
            onAutoConfirmChange = { autoConfirm = it },
            allCurrencies = allCurrencies,
            modifier = Modifier.padding(innerPadding)
        )

        PlannedPaymentEntrySheets(
            showScheduleBuilder = showScheduleBuilder,
            onDismissScheduleBuilder = { showScheduleBuilder = false },
            startDateMillis = startDateMillis,
            recurrenceType = recurrenceType,
            recurrenceInterval = recurrenceInterval,
            dayOfWeek = dayOfWeek,
            dayOfMonth = dayOfMonth,
            onScheduleConfirm = { start, type, interval, dow, dom ->
                startDateMillis = start
                recurrenceType = type
                recurrenceInterval = interval
                dayOfWeek = dow
                dayOfMonth = dom
            },
            showEndDatePicker = showEndDatePicker,
            onDismissEndDatePicker = { showEndDatePicker = false },
            endDateMillis = endDateMillis,
            onEndDateSelected = { endDateMillis = it; showEndDatePicker = false },
            showCategorySheet = showCategorySheet,
            onDismissCategorySheet = { showCategorySheet = false },
            categories = categories,
            selectedCategoryId = selectedCategoryId,
            onCategorySelected = { category ->
                selectedCategoryId = category.id
                showCategorySheet = false
            },
            showAccountSheet = showAccountSheet,
            onDismissAccountSheet = { showAccountSheet = false },
            accounts = accounts,
            selectedAccountId = selectedAccountId,
            onAccountSelected = { account ->
                account?.let { selectedAccountId = it.id }
                showAccountSheet = false
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
            availableTags = availableTags,
            selectedTagNames = selectedTagNames,
            onTagsSelected = { selectedTagNames = it; showTagSheet = false },
            showStatusSheet = showStatusSheet,
            onDismissStatusSheet = { showStatusSheet = false },
            status = status,
            onStatusSelected = { status = it; showStatusSheet = false },
            showNumpad = showNumpad,
            onDismissNumpad = { showNumpad = false },
            amountText = amountText,
            transactionType = transactionType,
            onAmountChanged = { amountText = it },
            showCurrencySheet = showCurrencySheet,
            onDismissCurrencySheet = { showCurrencySheet = false },
            currency = currency,
            onCurrencySelected = { currency = it },
            allCurrencies = allCurrencies
        )
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
