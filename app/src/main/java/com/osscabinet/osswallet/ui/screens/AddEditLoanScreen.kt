package com.osscabinet.ossWallet.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.osscabinet.ossWallet.domain.model.*
import com.osscabinet.ossWallet.ui.components.*
import com.osscabinet.ossWallet.ui.viewmodel.LoanViewModel
import com.osscabinet.ossWallet.ui.viewmodel.LoanIntent
import java.math.BigDecimal
import java.util.UUID

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddEditLoanScreen(
    accountId: String? = null,
    loanViewModel: LoanViewModel,
    onBackClick: () -> Unit
) {
    val state by loanViewModel.state.collectAsStateWithLifecycle()
    
    val editingLoan = remember(accountId, state.loans) {
        state.loans.find { it.loan.account.id == accountId }?.loan
    }

    var title by remember { mutableStateOf(editingLoan?.account?.name ?: "") }
    var principal by remember { mutableStateOf(editingLoan?.principal?.toString() ?: "") }
    var interestRate by remember { mutableStateOf(editingLoan?.interestRate?.toString() ?: "0.0") }
    var maturityDate by remember { mutableLongStateOf(editingLoan?.maturityDate ?: (System.currentTimeMillis() + 31536000000L)) }
    var loanType by remember { mutableStateOf(editingLoan?.loanType ?: LoanType.CREDIT) }
    var currency by remember { mutableStateOf(editingLoan?.account?.currencyCode ?: "USD") }
    
    var isNumpadVisible by remember { mutableStateOf(false) }
    var showDatePicker by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text(if (accountId == null) "New Loan/Debt" else "Edit Loan", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    if (accountId != null) {
                        IconButton(onClick = { 
                            loanViewModel.handleIntent(LoanIntent.DeleteLoan(accountId))
                            onBackClick()
                        }) {
                            Icon(Icons.Rounded.Delete, contentDescription = "Delete", tint = MaterialTheme.colorScheme.error)
                        }
                    }
                    Button(
                        onClick = {
                            val principalVal = principal.toBigDecimalOrNull() ?: BigDecimal.ZERO
                            val account = Account(
                                id = accountId ?: UUID.randomUUID().toString(),
                                name = title,
                                balance = if (loanType == LoanType.CREDIT) principalVal.negate() else principalVal,
                                currencyCode = currency,
                                structureType = "Loan",
                                colorHex = "#FF5722",
                                icon = "CreditCard"
                            )
                            val loan = Loan(
                                account = account,
                                principal = principalVal,
                                interestRate = interestRate.toBigDecimalOrNull() ?: BigDecimal.ZERO,
                                maturityDate = maturityDate,
                                loanType = loanType
                            )
                            loanViewModel.handleIntent(LoanIntent.UpsertLoan(loan))
                            onBackClick()
                        },
                        enabled = title.isNotBlank() && principal.toBigDecimalOrNull() != null,
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("Save")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            // Principle Card
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(140.dp)
                    .clickable { isNumpadVisible = true },
                shape = RoundedCornerShape(32.dp),
                color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.3f)
            ) {
                Column(
                    modifier = Modifier.fillMaxSize(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Text("PRINCIPAL AMOUNT", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.secondary)
                    Text(
                        text = principal.ifEmpty { "0.00" },
                        style = MaterialTheme.typography.displayMedium,
                        fontWeight = FontWeight.Black,
                        color = MaterialTheme.colorScheme.secondary
                    )
                }
            }

            // Loan Type Toggle
            AeonFlowSectionHeader(title = "LOAN CLASSIFICATION")
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                LoanType.entries.forEach { type ->
                    FilterChip(
                        selected = loanType == type,
                        onClick = { loanType = type },
                        label = { Text(type.name) },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp)
                    )
                }
            }

            EditorialInput(
                value = title,
                onValueChange = { title = it },
                label = "LOAN TITLE / PROVIDER",
                icon = Icons.Rounded.Badge
            )

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                EditorialInput(
                    value = interestRate,
                    onValueChange = { interestRate = it },
                    label = "INTEREST RATE (%)",
                    icon = Icons.Rounded.Percent,
                    modifier = Modifier.weight(1f)
                )

                RecordFieldCard(
                    icon = Icons.Rounded.CalendarToday,
                    label = "MATURITY",
                    value = formatDateTime(maturityDate),
                    onClick = { showDatePicker = true },
                    modifier = Modifier.weight(1.2f),
                    isSlim = true
                )
            }
        }

        if (isNumpadVisible) {
            ModalBottomSheet(onDismissRequest = { isNumpadVisible = false }) {
                CalculatorNumpad(
                    amount = principal,
                    onAmountChange = { newAmount, _ -> principal = newAmount },
                    onDone = { isNumpadVisible = false },
                    modifier = Modifier.padding(16.dp)
                )
            }
        }

        if (showDatePicker) {
            AeonDateTimePicker(
                initialDateTime = maturityDate,
                onDateTimeSelected = { maturityDate = it },
                onDismiss = { showDatePicker = false }
            )
        }
    }
}
