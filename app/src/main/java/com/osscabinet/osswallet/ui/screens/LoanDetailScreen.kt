package com.osscabinet.ossWallet.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.ui.draw.clip
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.ArrowDownward
import androidx.compose.material.icons.rounded.ArrowUpward
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material.icons.rounded.KeyboardArrowUp
import androidx.compose.material.icons.rounded.Payments
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.osscabinet.ossWallet.domain.model.AmortizationEntry
import com.osscabinet.ossWallet.domain.model.LoanPayment
import com.osscabinet.ossWallet.domain.model.LoanStatus
import com.osscabinet.ossWallet.domain.model.LoanType
import com.osscabinet.ossWallet.domain.service.LoanService
import com.osscabinet.ossWallet.ui.components.AeonDateField
import com.osscabinet.ossWallet.ui.components.cards.AeonExpressiveCard
import com.osscabinet.ossWallet.ui.viewmodel.LoanViewModel
import java.math.BigDecimal
import java.math.RoundingMode
import java.text.SimpleDateFormat
import java.util.*

private val TealHealthy = Color(0xFF004D40)
private val RedWarning = Color(0xFFB3261E)
private val LentColor = Color(0xFF004D40)
private val BorrowedColor = Color(0xFFE65100)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LoanDetailScreen(
    loanId: Long,
    viewModel: LoanViewModel,
    onEditClick: () -> Unit,
    onBackClick: () -> Unit
) {
    val loans by viewModel.loans.collectAsStateWithLifecycle()
    val loan = remember(loanId, loans) { loans.find { it.id == loanId } } ?: return
    val payments by viewModel.getPaymentsForLoan(loanId).collectAsStateWithLifecycle(emptyList())

    val now = System.currentTimeMillis()
    val summary = remember(loan, payments, now) {
        LoanService.buildSummary(loan, payments, now)
    }

    val progress = if (loan.principalAmount > BigDecimal.ZERO)
        (summary.totalPaid.divide(loan.principalAmount, 4, RoundingMode.HALF_UP)).toFloat().coerceIn(0f, 1f)
    else 0f
    val accentColor = if (loan.loanType == LoanType.LENT) LentColor else BorrowedColor

    var showAddPayment by remember { mutableStateOf(false) }
    var paymentAmountText by remember { mutableStateOf("") }
    var paymentDateMillis by remember { mutableLongStateOf(now) }
    var paymentNote by remember { mutableStateOf("") }

    var showAmortization by remember { mutableStateOf(false) }
    var showAllAmortization by remember { mutableStateOf(false) }

    val amortizationSchedule = remember(loan, showAmortization) {
        if (!showAmortization || loan.interestRate == null) emptyList()
        else {
            val monthlyPayment = loan.principalAmount.divide(BigDecimal(12), 2, RoundingMode.HALF_UP)
            LoanService.getAmortizationSchedule(loan, monthlyPayment, loan.startDate)
        }
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(
                        loan.name,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = (-0.5).sp
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    if (loan.status == LoanStatus.ACTIVE) {
                        TextButton(onClick = { viewModel.settleLoan(loan); onBackClick() }) {
                            Text("Settle")
                        }
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        },
        floatingActionButton = {
            if (loan.status == LoanStatus.ACTIVE) {
                FloatingActionButton(onClick = { showAddPayment = true }) {
                    Icon(Icons.Rounded.Add, contentDescription = "Add Payment")
                }
            }
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(horizontal = 24.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                AeonExpressiveCard(
                    title = loan.name,
                    subtitle = "${loan.loanType.name.lowercase().replaceFirstChar { it.uppercase() }} — ${loan.currency}",
                    accentColor = accentColor,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    // Loan type badge
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = accentColor.copy(alpha = 0.1f),
                        border = BorderStroke(0.5.dp, accentColor.copy(alpha = 0.2f))
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                if (loan.loanType == LoanType.LENT) Icons.Rounded.ArrowUpward else Icons.Rounded.ArrowDownward,
                                contentDescription = null,
                                modifier = Modifier.size(14.dp),
                                tint = accentColor
                            )
                            Text(
                                if (loan.loanType == LoanType.LENT) "LENT" else "BORROWED",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontWeight = FontWeight.Bold,
                                    letterSpacing = 0.5.sp
                                ),
                                color = accentColor
                            )
                        }
                    }
                    Spacer(Modifier.height(8.dp))
                    // Remaining balance prominent
                    Text(
                        summary.remainingBalance.toPlainString(),
                        style = MaterialTheme.typography.displaySmall.copy(fontWeight = FontWeight.Bold),
                        color = accentColor
                    )
                    Text(
                        "of ${loan.principalAmount.toPlainString()} ${loan.currency} remaining",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                    )
                    Spacer(Modifier.height(8.dp))
                    // Data rows wrapped in surface
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.surfaceContainerLowest,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            DataRow(
                                label = "PRINCIPAL",
                                value = "${loan.principalAmount.toPlainString()} ${loan.currency}"
                            )
                            DataRow(
                                label = "TOTAL PAID",
                                value = "${summary.totalPaid.toPlainString()} ${loan.currency}"
                            )
                            DataRow(
                                label = "REMAINING",
                                value = "${summary.remainingBalance.toPlainString()} ${loan.currency}",
                                valueColor = accentColor
                            )
                            if (loan.interestRate != null) {
                                DataRow(
                                    label = "INTEREST ACCRUED",
                                    value = "${summary.interestAccrued.setScale(2, RoundingMode.HALF_UP).toPlainString()} ${loan.currency}"
                                )
                            }
                        }
                    }
                    Spacer(Modifier.height(8.dp))
                    // Progress bar
                    LinearProgressIndicator(
                        progress = { progress },
                        modifier = Modifier.fillMaxWidth().height(8.dp),
                        color = accentColor,
                        trackColor = accentColor.copy(alpha = 0.15f),
                        strokeCap = StrokeCap.Round
                    )
                }
            }

            if (loan.interestRate != null) {
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        LoanSectionHeaderInline(
                            title = "AMORTIZATION SCHEDULE",
                            sectionColor = MaterialTheme.colorScheme.primary
                        )
                        IconButton(onClick = { showAmortization = !showAmortization }) {
                            Icon(
                                if (showAmortization) Icons.Rounded.KeyboardArrowUp else Icons.Rounded.KeyboardArrowDown,
                                contentDescription = if (showAmortization) "Collapse" else "Expand"
                            )
                        }
                    }
                }
            }

            if (showAmortization && amortizationSchedule.isNotEmpty()) {
                val displayRows = if (showAllAmortization) amortizationSchedule else amortizationSchedule.take(12)
                items(displayRows, key = { it.period }) { entry ->
                    AmortizationRow(entry = entry, currency = loan.currency)
                }
                if (amortizationSchedule.size > 12) {
                    item {
                        TextButton(
                            onClick = { showAllAmortization = !showAllAmortization },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(if (showAllAmortization) "Show less" else "Show all (${amortizationSchedule.size} periods)")
                        }
                    }
                }
            }

            item {
                LoanSectionHeaderInline(
                    title = "PAYMENTS",
                    sectionColor = MaterialTheme.colorScheme.primary
                )
            }

            if (payments.isEmpty() && loan.status == LoanStatus.ACTIVE) {
                item {
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = MaterialTheme.colorScheme.surfaceContainerLowest,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            "No payments recorded yet",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                            modifier = Modifier.padding(16.dp),
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }

            items(payments, key = { it.id }) { payment ->
                PaymentRow(
                    payment = payment,
                    currency = loan.currency,
                    onDelete = { viewModel.deletePayment(payment.id) }
                )
            }
        }
    }

    if (showAddPayment) {
        AlertDialog(
            onDismissRequest = { showAddPayment = false },
            shape = RoundedCornerShape(28.dp),
            title = { Text("Add Payment") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedTextField(
                        value = paymentAmountText,
                        onValueChange = { paymentAmountText = it },
                        label = { Text("Amount (${loan.currency})") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        singleLine = true,
                        shape = RoundedCornerShape(16.dp)
                    )
                    AeonDateField(
                        label = "Payment Date",
                        dateMillis = paymentDateMillis,
                        onDateSelected = { paymentDateMillis = it }
                    )
                    OutlinedTextField(
                        value = paymentNote,
                        onValueChange = { paymentNote = it },
                        label = { Text("Note (optional)") },
                        singleLine = true,
                        shape = RoundedCornerShape(16.dp)
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        val amount = paymentAmountText.toBigDecimalOrNull() ?: return@TextButton
                        viewModel.addPayment(
                            LoanPayment(
                                loanId = loanId,
                                amount = amount,
                                date = paymentDateMillis,
                                note = paymentNote
                            )
                        )
                        paymentAmountText = ""
                        paymentNote = ""
                        paymentDateMillis = now
                        showAddPayment = false
                    }
                ) { Text("Add") }
            },
            dismissButton = {
                TextButton(onClick = { showAddPayment = false }) { Text("Cancel") }
            }
        )
    }
}

@Composable
private fun LoanSectionHeaderInline(title: String, sectionColor: Color) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.padding(vertical = 4.dp, horizontal = 4.dp)
    ) {
        Box(
            modifier = Modifier
                .size(10.dp)
                .clip(CircleShape)
                .background(sectionColor)
        )
        Text(
            title,
            style = MaterialTheme.typography.labelSmall.copy(
                fontWeight = FontWeight.ExtraBold,
                fontSize = 11.sp,
                letterSpacing = 1.5.sp
            ),
            color = sectionColor
        )
    }
}

@Composable
private fun DataRow(
    label: String,
    value: String,
    valueColor: Color? = null
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            label,
            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
        )
        Text(
            value,
            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
            color = valueColor ?: MaterialTheme.colorScheme.onSurface
        )
    }
}

@Composable
private fun AmortizationRow(entry: AmortizationEntry, currency: String) {
    val dateStr = remember(entry.date) {
        SimpleDateFormat("MMM yyyy", Locale.getDefault()).format(Date(entry.date))
    }
    Surface(
        color = MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.2f),
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(0.5.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.08f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                "${entry.period}",
                style = MaterialTheme.typography.labelSmall,
                modifier = Modifier.width(28.dp),
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(dateStr, style = MaterialTheme.typography.bodySmall, modifier = Modifier.width(70.dp))
            Text(
                entry.principal.setScale(2, RoundingMode.HALF_UP).toPlainString(),
                style = MaterialTheme.typography.bodySmall
            )
            Text(
                entry.interest.toPlainString(),
                style = MaterialTheme.typography.bodySmall,
                color = RedWarning
            )
            Text(
                entry.balance.setScale(2, RoundingMode.HALF_UP).toPlainString(),
                style = MaterialTheme.typography.bodySmall,
                color = TealHealthy
            )
        }
    }
}

@Composable
private fun PaymentRow(
    payment: LoanPayment,
    currency: String,
    onDelete: () -> Unit
) {
    val dateStr = remember(payment.date) {
        SimpleDateFormat("MMM d, yyyy", Locale.getDefault()).format(Date(payment.date))
    }
    Surface(
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        shape = RoundedCornerShape(20.dp),
        border = BorderStroke(0.5.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.15f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(12.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Left icon circle
            Surface(
                shape = CircleShape,
                color = TealHealthy.copy(alpha = 0.1f),
                modifier = Modifier.size(40.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        Icons.Rounded.Payments,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp),
                        tint = TealHealthy
                    )
                }
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    "${payment.amount.toPlainString()} $currency",
                    style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold),
                    color = TealHealthy
                )
                Text(
                    dateStr,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                if (payment.note.isNotBlank()) {
                    Text(
                        payment.note,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            IconButton(onClick = onDelete) {
                Icon(
                    Icons.Rounded.Delete,
                    contentDescription = "Delete payment",
                    tint = MaterialTheme.colorScheme.error
                )
            }
        }
    }
}

