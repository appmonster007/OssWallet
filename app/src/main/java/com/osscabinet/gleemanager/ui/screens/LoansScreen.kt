package com.osscabinet.gleemanager.ui.screens

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
import androidx.compose.ui.draw.clip as composeClip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.osscabinet.gleemanager.domain.model.Loan
import com.osscabinet.gleemanager.domain.model.LoanType
import com.osscabinet.gleemanager.ui.util.formatCurrency
import com.osscabinet.gleemanager.ui.util.getCurrencySymbol
import com.osscabinet.gleemanager.ui.viewmodel.LoanViewModel
import com.osscabinet.gleemanager.ui.viewmodel.SettingsViewModel
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
            FloatingActionButton(
                onClick = { showAddDialog = true },
                containerColor = Color(0xFF004D40),
                contentColor = Color.White,
                shape = CircleShape
            ) {
                Icon(Icons.Default.Add, contentDescription = "Add Loan")
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(MaterialTheme.colorScheme.surface)
        ) {
            val totalDebt = loans.filter { it.type == LoanType.DEBT }.sumOf { it.principal - it.repayments }
            val totalCredit = loans.filter { it.type == LoanType.LOAN }.sumOf { it.principal - it.repayments }

            LoanSummaryHeader(totalDebt, totalCredit, baseCurrency)

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
                            onRepayClick = { viewModel.updateRepayment(loan, 100.0) }, // Mock repay $100
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
fun LoanSummaryHeader(debt: Double, credit: Double, currency: String) {
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
            color = Color(0xFFC62828)
        )
        SummaryCard(
            modifier = Modifier.weight(1f),
            label = "TOTAL CREDIT",
            amount = credit,
            currency = currency,
            color = Color(0xFF2E7D32)
        )
    }
}

@Composable
fun SummaryCard(modifier: Modifier, label: String, amount: Double, currency: String, color: Color) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(24.dp),
        color = color.copy(alpha = 0.1f)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(label, style = MaterialTheme.typography.labelSmall, color = color)
            Text(
                formatCurrency(amount, currency),
                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold, fontFeatureSettings = "tnum")
            )
        }
    }
}

@Composable
fun LoanItem(loan: Loan, onRepayClick: () -> Unit, onDeleteClick: () -> Unit) {
    val progress = (loan.repayments / loan.principal).toFloat().coerceIn(0f, 1f)
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
                modifier = Modifier.fillMaxWidth().height(8.dp).myClip(RoundedCornerShape(4.dp)),
                color = color,
                trackColor = color.copy(alpha = 0.1f)
            )

            Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text(
                    "${formatCurrency(loan.repayments, loan.currency)} / ${formatCurrency(loan.principal, loan.currency)}",
                    style = MaterialTheme.typography.labelMedium.copy(fontFeatureSettings = "tnum")
                )
                TextButton(onClick = onRepayClick) {
                    Text("REPAY ${formatCurrency(100.0, loan.currency)}", color = color)
                }
            }
        }
    }
}

@Composable
fun AddLoanDialog(defaultCurrency: String, onDismiss: () -> Unit, onConfirm: (String, Double, Double, LoanType) -> Unit) {
    var title by remember { mutableStateOf("") }
    var amount by remember { mutableStateOf("") }
    var rate by remember { mutableStateOf("") }
    var type by remember { mutableStateOf(LoanType.DEBT) }

    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            Button(onClick = { onConfirm(title, amount.toDoubleOrNull() ?: 0.0, rate.toDoubleOrNull() ?: 0.0, type) }) {
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

fun Modifier.myClip(shape: androidx.compose.ui.graphics.Shape): Modifier = this.composeClip(shape)
