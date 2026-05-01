package com.osscabinet.ossWallet.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.osscabinet.ossWallet.domain.model.LoanType
import com.osscabinet.ossWallet.ui.viewmodel.LoanViewModel
import java.math.BigDecimal

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LoanEntryScreen(
    loanId: Long?,
    viewModel: LoanViewModel,
    onBackClick: () -> Unit
) {
    val loans by viewModel.loans.collectAsStateWithLifecycle()
    val existing = remember(loanId, loans) {
        loanId?.let { id -> loans.find { it.id == id } }
    }

    var name by remember(existing) { mutableStateOf(existing?.name ?: "") }
    var principalText by remember(existing) { mutableStateOf(existing?.principalAmount?.toPlainString() ?: "") }
    var currency by remember(existing) { mutableStateOf(existing?.currency ?: "USD") }
    var loanType by remember(existing) { mutableStateOf(existing?.loanType ?: LoanType.BORROWED) }
    var counterparty by remember(existing) { mutableStateOf(existing?.lenderOrBorrowerName ?: "") }
    var interestText by remember(existing) { mutableStateOf(existing?.interestRate?.toString() ?: "") }
    var note by remember(existing) { mutableStateOf(existing?.note ?: "") }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (loanId != null) "Edit Loan" else "New Loan") },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(
                        onClick = {
                            val principal = principalText.toBigDecimalOrNull() ?: BigDecimal.ZERO
                            val rate = interestText.toFloatOrNull()
                            val loan = (existing ?: viewModel.buildDefaultLoan(1)).copy(
                                name = name,
                                principalAmount = principal,
                                currency = currency,
                                loanType = loanType,
                                lenderOrBorrowerName = counterparty,
                                interestRate = rate,
                                note = note
                            )
                            viewModel.saveLoan(loan)
                            onBackClick()
                        }
                    ) {
                        Icon(Icons.Default.Check, contentDescription = "Save")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text("Loan Name") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            Text("Type", style = MaterialTheme.typography.labelMedium)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                LoanType.entries.forEach { type ->
                    FilterChip(
                        selected = loanType == type,
                        onClick = { loanType = type },
                        label = { Text(if (type == LoanType.LENT) "Lent" else "Borrowed") }
                    )
                }
            }

            OutlinedTextField(
                value = counterparty,
                onValueChange = { counterparty = it },
                label = { Text(if (loanType == LoanType.LENT) "Lent to" else "Borrowed from") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = principalText,
                    onValueChange = { principalText = it },
                    label = { Text("Amount") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.weight(1f),
                    singleLine = true
                )
                OutlinedTextField(
                    value = currency,
                    onValueChange = { currency = it.uppercase().take(3) },
                    label = { Text("Currency") },
                    modifier = Modifier.width(100.dp),
                    singleLine = true
                )
            }

            OutlinedTextField(
                value = interestText,
                onValueChange = { interestText = it },
                label = { Text("Interest Rate % / year (optional)") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            OutlinedTextField(
                value = note,
                onValueChange = { note = it },
                label = { Text("Note (optional)") },
                modifier = Modifier.fillMaxWidth(),
                minLines = 2
            )
        }
    }
}
