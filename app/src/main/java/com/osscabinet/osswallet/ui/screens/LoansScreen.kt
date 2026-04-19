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
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.material.icons.rounded.Add
import com.osscabinet.ossWallet.domain.model.Loan
import com.osscabinet.ossWallet.domain.model.LoanType
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.unit.IntSize
import com.osscabinet.ossWallet.domain.model.CashFlowType
import com.osscabinet.ossWallet.ui.dashboard.components.AeonFlowCard
import com.osscabinet.ossWallet.ui.components.AeonFlowSectionHeader
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
    val loans by viewModel.loans.collectAsStateWithLifecycle()
    val baseCurrency by settingsViewModel.currency.collectAsStateWithLifecycle()
    val decimalPrecision by settingsViewModel.decimalPrecision.collectAsStateWithLifecycle()
    var showAddDialog by remember { mutableStateOf(false) }

    var navTriggered by remember { mutableStateOf(false) }
    val navigate: (() -> Unit) -> Unit = { action ->
        if (!navTriggered) {
            navTriggered = true
            action()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Debt & Credit", style = MaterialTheme.typography.headlineSmall) },
                navigationIcon = {
                    IconButton(onClick = { navigate(onBackClick) }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        },
        floatingActionButton = {
            AeonFloatingActionButton(
                onClick = { showAddDialog = true },
                icon = Icons.Rounded.Add,
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

            AeonFlowSectionHeader(
                title = "SUMMARY",
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
            )
            LoanSummaryHeader(totalDebt, totalCredit, baseCurrency, decimalPrecision)

            AeonFlowSectionHeader(
                title = "YOUR LOANS",
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
            )
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
            .padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        AeonFlowCard(
            modifier = Modifier.weight(1f),
            label = "TOTAL DEBT",
            totalInBase = debt,
            baseCurrency = currency,
            precision = precision,
            type = CashFlowType.EXPENSE,
            content = {}
        )
        AeonFlowCard(
            modifier = Modifier.weight(1f),
            label = "TOTAL CREDIT",
            totalInBase = credit,
            baseCurrency = currency,
            precision = precision,
            type = CashFlowType.INCOME,
            content = {}
        )
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
        shape = RoundedCornerShape(28.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
    ) {
        Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        loan.title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        "${if (loan.type == LoanType.DEBT) "Due" else "Receive"} by ${
                            SimpleDateFormat(
                                "dd MMM yyyy",
                                Locale.getDefault()
                            ).format(Date(loan.dueDate))
                        }",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.outline
                    )
                }
                IconButton(onClick = onDeleteClick) {
                    Icon(
                        Icons.Default.Delete,
                        contentDescription = "Delete",
                        tint = MaterialTheme.colorScheme.error.copy(alpha = 0.7f),
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            LinearProgressIndicator(
                progress = { progress },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp)
                    .clip(CircleShape),
                color = color,
                trackColor = color.copy(alpha = 0.1f)
            )

            Row(
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "${formatCurrencyRight(loan.repayments, loan.currency, precision = precision)} / ${
                        formatCurrencyRight(
                            loan.principal,
                            loan.currency,
                            precision = precision
                        )
                    }",
                    style = MaterialTheme.typography.labelLarge.copy(
                        fontFeatureSettings = "tnum",
                        fontWeight = FontWeight.SemiBold
                    ),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                TextButton(
                    onClick = onRepayClick,
                    colors = ButtonDefaults.textButtonColors(contentColor = color),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(
                        "REPAY",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold
                    )
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
