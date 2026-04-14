package com.osscabinet.ossWallet.ui.screens

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.osscabinet.ossWallet.domain.model.Loan
import com.osscabinet.ossWallet.domain.model.LoanType
import com.osscabinet.ossWallet.ui.components.AeonFloatingActionButton
import com.osscabinet.ossWallet.ui.util.formatCurrencyRight
import com.osscabinet.ossWallet.ui.viewmodel.LoanViewModel
import com.osscabinet.ossWallet.ui.viewmodel.SettingsViewModel
import java.math.BigDecimal
import java.math.RoundingMode
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LoansScreen(
    viewModel: LoanViewModel,
    settingsViewModel: SettingsViewModel,
    onBackClick: () -> Unit
) {
    val loans by viewModel.loans.collectAsState()
    val baseCurrency by settingsViewModel.currency.collectAsState()
    val decimalPrecision by settingsViewModel.decimalPrecision.collectAsState()
    var showAddDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Debt & Credit", style = MaterialTheme.typography.headlineSmall) },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        },
        floatingActionButton = {
            AeonFloatingActionButton(
                onClick = { showAddDialog = true },
                contentDescription = "Add Loan"
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(MaterialTheme.colorScheme.surface)
        ) {
            val totalDebt = loans.filter { it.type == LoanType.DEBT }
                .fold(BigDecimal.ZERO) { acc, loan -> acc.add(loan.principal.subtract(loan.repayments)) }
            val totalCredit = loans.filter { it.type == LoanType.LOAN }
                .fold(BigDecimal.ZERO) { acc, loan -> acc.add(loan.principal.subtract(loan.repayments)) }

            LoanSummaryHeader(totalDebt, totalCredit, baseCurrency, decimalPrecision)

            if (loans.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("No active loans or debts", style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.outline)
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    items(loans) { loan ->
                        LoanItem(
                            loan = loan,
                            precision = decimalPrecision,
                            onRepayClick = { viewModel.updateRepayment(loan, BigDecimal("100.0")) }, // Mock repay $100
                            onDeleteClick = { viewModel.deleteLoan(loan) }
                        )
                    }
                }
            }
        }
    }

    if (showAddDialog) {
        AddLoanDialog(
            defaultCurrency = baseCurrency,
            onDismiss = { showAddDialog = false },
            onConfirm = { title, amount, rate, type ->
                viewModel.addLoan(title, amount, rate, System.currentTimeMillis() + 2592000000L, type)
                showAddDialog = false
            }
        )
    }
}

@Composable
fun LoanSummaryHeader(debt: BigDecimal, credit: BigDecimal, currency: String, precision: Int) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(24.dp),
        horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        SummaryCard(
            modifier = Modifier.weight(1f),
            label = "TOTAL DEBT",
            amount = debt,
            currency = currency,
            precision = precision,
            color = Color(0xFFC62828)
        )
        SummaryCard(
            modifier = Modifier.weight(1f),
            label = "TOTAL CREDIT",
            amount = credit,
            currency = currency,
            precision = precision,
            color = Color(0xFF2E7D32)
        )
    }
}

@Composable
fun SummaryCard(modifier: Modifier, label: String, amount: BigDecimal, currency: String, precision: Int, color: Color) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(24.dp),
        color = color.copy(alpha = 0.1f)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(label, style = MaterialTheme.typography.labelSmall, color = color)
            Text(
                formatCurrencyRight(amount, currency, precision = precision),
                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold, fontFeatureSettings = "tnum")
            )
        }
    }
}

@Composable
fun LoanItem(loan: Loan, precision: Int, onRepayClick: () -> Unit, onDeleteClick: () -> Unit) {
    val progress = if (loan.principal.signum() > 0) {
        loan.repayments.divide(loan.principal, 4, RoundingMode.HALF_UP).toFloat().coerceIn(0f, 1f)
    } else 0f
    val color = if (loan.type == LoanType.DEBT) Color(0xFFC62828) else Color(0xFF2E7D32)

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(32.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
    ) {
        Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                Column {
                    Text(loan.title, style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold))
                    Text(
                        "${if (loan.type == LoanType.DEBT) "Due" else "Receive"} by ${SimpleDateFormat("dd MMM yyyy", Locale.getDefault()).format(Date(loan.dueDate))}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.outline
                    )
                }
                IconButton(onClick = onDeleteClick) {
                    Icon(Icons.Default.Delete, contentDescription = "Delete", tint = MaterialTheme.colorScheme.outline)
                }
            }

            LinearProgressIndicator(
                progress = { progress },
                modifier = Modifier.fillMaxWidth().height(8.dp).clip(RoundedCornerShape(4.dp)),
                color = color,
                trackColor = color.copy(alpha = 0.1f)
            )

            Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text(
                    "${formatCurrencyRight(loan.repayments, loan.currency, precision = precision)} / ${formatCurrencyRight(loan.principal, loan.currency, precision = precision)}",
                    style = MaterialTheme.typography.labelMedium.copy(fontFeatureSettings = "tnum")
                )
                TextButton(onClick = onRepayClick) {
                    Text("REPAY ${formatCurrencyRight(BigDecimal("100.0"), loan.currency, precision = precision)}", color = color)
                }
            }
        }
    }
}

@Composable
fun AddLoanDialog(defaultCurrency: String, onDismiss: () -> Unit, onConfirm: (String, BigDecimal, BigDecimal, LoanType) -> Unit) {
    var title by remember { mutableStateOf("") }
    var amount by remember { mutableStateOf("") }
    var rate by remember { mutableStateOf("") }
    var type by remember { mutableStateOf(LoanType.DEBT) }

    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            Button(onClick = { 
                onConfirm(
                    title, 
                    amount.toBigDecimalOrNull() ?: BigDecimal.ZERO, 
                    rate.toBigDecimalOrNull() ?: BigDecimal.ZERO, 
                    type
                ) 
            }) {
                Text("ADD")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("CANCEL") }
        },
        title = { Text("New Loan/Debt") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                TextField(value = title, onValueChange = { title = it }, label = { Text("Title") })
                TextField(value = amount, onValueChange = { amount = it }, label = { Text("Principal Amount") })
                TextField(value = rate, onValueChange = { rate = it }, label = { Text("Interest Rate (%)") })
                Row(verticalAlignment = Alignment.CenterVertically) {
                    RadioButton(selected = type == LoanType.DEBT, onClick = { type = LoanType.DEBT })
                    Text("Debt (I owe)")
                    Spacer(modifier = Modifier.width(8.dp))
                    RadioButton(selected = type == LoanType.LOAN, onClick = { type = LoanType.LOAN })
                    Text("Loan (I lent)")
                }
            }
        }
    )
}
