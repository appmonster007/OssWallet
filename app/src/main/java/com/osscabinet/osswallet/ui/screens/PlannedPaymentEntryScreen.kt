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
import com.osscabinet.ossWallet.domain.model.PlannedPayment
import com.osscabinet.ossWallet.domain.model.PlannedPaymentStatus
import com.osscabinet.ossWallet.domain.model.RecurrenceRule
import com.osscabinet.ossWallet.domain.model.RecurrenceType
import com.osscabinet.ossWallet.ui.viewmodel.AccountViewModel
import com.osscabinet.ossWallet.ui.viewmodel.PlannedPaymentViewModel
import java.math.BigDecimal
import java.time.LocalDate
import java.time.ZoneOffset

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlannedPaymentEntryScreen(
    plannedPaymentId: Long?,
    viewModel: PlannedPaymentViewModel,
    accountViewModel: AccountViewModel,
    onBackClick: () -> Unit
) {
    val accounts by accountViewModel.accounts.collectAsStateWithLifecycle()
    val payments by viewModel.plannedPayments.collectAsStateWithLifecycle()
    val existing = remember(plannedPaymentId, payments) {
        plannedPaymentId?.let { id -> payments.find { it.id == id } }
    }

    var title by remember(existing) { mutableStateOf(existing?.title ?: "") }
    var amountText by remember(existing) { mutableStateOf(existing?.amount?.toPlainString() ?: "") }
    var currency by remember(existing) { mutableStateOf(existing?.currency ?: "USD") }
    var transactionType by remember(existing) { mutableStateOf(existing?.transactionType ?: "expense") }
    var selectedAccountId by remember(existing) { mutableStateOf(existing?.accountId ?: accounts.firstOrNull()?.id ?: "") }
    var recurrenceType by remember(existing) { mutableStateOf(existing?.recurrenceRule?.type ?: RecurrenceType.MONTHLY) }
    var autoConfirm by remember(existing) { mutableStateOf(existing?.autoConfirm ?: false) }

    val isEditing = plannedPaymentId != null

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (isEditing) "Edit Planned Payment" else "New Planned Payment") },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(
                        onClick = {
                            val amount = amountText.toBigDecimalOrNull() ?: BigDecimal.ZERO
                            val now = LocalDate.now(ZoneOffset.UTC)
                                .atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()
                            val payment = (existing ?: viewModel.buildDefault(selectedAccountId, 1)).copy(
                                title = title,
                                amount = amount,
                                currency = currency,
                                transactionType = transactionType,
                                accountId = selectedAccountId,
                                recurrenceRule = RecurrenceRule(type = recurrenceType),
                                autoConfirm = autoConfirm,
                                nextDueDate = existing?.nextDueDate ?: now,
                                startDate = existing?.startDate ?: now
                            )
                            viewModel.save(payment)
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
                value = title,
                onValueChange = { title = it },
                label = { Text("Title") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = amountText,
                    onValueChange = { amountText = it },
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

            Text("Type", style = MaterialTheme.typography.labelMedium)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf("expense", "income").forEach { type ->
                    FilterChip(
                        selected = transactionType == type,
                        onClick = { transactionType = type },
                        label = { Text(type.replaceFirstChar { it.uppercase() }) }
                    )
                }
            }

            if (accounts.isNotEmpty()) {
                Text("Account", style = MaterialTheme.typography.labelMedium)
                var expanded by remember { mutableStateOf(false) }
                ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = it }) {
                    OutlinedTextField(
                        value = accounts.find { it.id == selectedAccountId }?.name ?: "",
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Account") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .menuAnchor()
                    )
                    ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                        accounts.forEach { account ->
                            DropdownMenuItem(
                                text = { Text(account.name) },
                                onClick = { selectedAccountId = account.id; expanded = false }
                            )
                        }
                    }
                }
            }

            Text("Recurrence", style = MaterialTheme.typography.labelMedium)
            var recurrenceExpanded by remember { mutableStateOf(false) }
            ExposedDropdownMenuBox(expanded = recurrenceExpanded, onExpandedChange = { recurrenceExpanded = it }) {
                OutlinedTextField(
                    value = recurrenceType.name.lowercase().replace('_', ' ').replaceFirstChar { it.uppercase() },
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Repeats") },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = recurrenceExpanded) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .menuAnchor()
                )
                ExposedDropdownMenu(expanded = recurrenceExpanded, onDismissRequest = { recurrenceExpanded = false }) {
                    RecurrenceType.entries.forEach { type ->
                        DropdownMenuItem(
                            text = { Text(type.name.lowercase().replace('_', ' ').replaceFirstChar { it.uppercase() }) },
                            onClick = { recurrenceType = type; recurrenceExpanded = false }
                        )
                    }
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("Auto-confirm", style = MaterialTheme.typography.bodyMedium)
                Switch(checked = autoConfirm, onCheckedChange = { autoConfirm = it })
            }
        }
    }
}
