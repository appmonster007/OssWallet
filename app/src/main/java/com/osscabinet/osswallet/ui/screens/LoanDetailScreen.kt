package com.osscabinet.ossWallet.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.osscabinet.ossWallet.domain.model.LoanPayment
import com.osscabinet.ossWallet.domain.model.LoanStatus
import com.osscabinet.ossWallet.domain.service.LoanService
import com.osscabinet.ossWallet.ui.viewmodel.LoanViewModel
import java.math.BigDecimal
import java.text.SimpleDateFormat
import java.util.*

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

    var showAddPayment by remember { mutableStateOf(false) }
    var paymentAmountText by remember { mutableStateOf("") }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(loan.name) },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    if (loan.status == LoanStatus.ACTIVE) {
                        TextButton(onClick = { viewModel.settleLoad(loan); onBackClick() }) {
                            Text("Settle")
                        }
                    }
                }
            )
        },
        floatingActionButton = {
            if (loan.status == LoanStatus.ACTIVE) {
                FloatingActionButton(onClick = { showAddPayment = true }) {
                    Icon(Icons.Default.Add, contentDescription = "Add Payment")
                }
            }
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                ElevatedCard(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween) {
                            Text("Principal", style = MaterialTheme.typography.bodyMedium)
                            Text("${loan.principalAmount.toPlainString()} ${loan.currency}", fontWeight = FontWeight.SemiBold)
                        }
                        Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween) {
                            Text("Total Paid", style = MaterialTheme.typography.bodyMedium)
                            Text("${summary.totalPaid.toPlainString()} ${loan.currency}")
                        }
                        Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween) {
                            Text("Remaining", style = MaterialTheme.typography.bodyMedium)
                            Text(
                                "${summary.remainingBalance.toPlainString()} ${loan.currency}",
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        if (loan.interestRate != null) {
                            Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween) {
                                Text("Interest Accrued", style = MaterialTheme.typography.bodyMedium)
                                Text("${summary.interestAccrued.setScale(2, java.math.RoundingMode.HALF_UP).toPlainString()} ${loan.currency}")
                            }
                        }
                        val progress = if (loan.principalAmount > BigDecimal.ZERO)
                            (summary.totalPaid / loan.principalAmount).toFloat().coerceIn(0f, 1f)
                        else 0f
                        LinearProgressIndicator(
                            progress = { progress },
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }

            if (payments.isNotEmpty()) {
                item {
                    Text(
                        "Payments",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold
                    )
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
    }

    if (showAddPayment) {
        AlertDialog(
            onDismissRequest = { showAddPayment = false },
            title = { Text("Add Payment") },
            text = {
                OutlinedTextField(
                    value = paymentAmountText,
                    onValueChange = { paymentAmountText = it },
                    label = { Text("Amount (${loan.currency})") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        val amount = paymentAmountText.toBigDecimalOrNull() ?: return@TextButton
                        viewModel.addPayment(
                            LoanPayment(
                                loanId = loanId,
                                amount = amount,
                                date = System.currentTimeMillis()
                            )
                        )
                        paymentAmountText = ""
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
private fun PaymentRow(
    payment: LoanPayment,
    currency: String,
    onDelete: () -> Unit
) {
    val dateStr = remember(payment.date) {
        SimpleDateFormat("MMM d, yyyy", Locale.getDefault()).format(Date(payment.date))
    }
    ElevatedCard(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text("${payment.amount.toPlainString()} $currency", fontWeight = FontWeight.Medium)
                Text(dateStr, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            IconButton(onClick = onDelete) {
                Icon(Icons.Default.Delete, contentDescription = "Delete payment", tint = MaterialTheme.colorScheme.error)
            }
        }
    }
}
