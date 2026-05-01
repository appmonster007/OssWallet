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
import com.osscabinet.ossWallet.domain.model.Budget
import com.osscabinet.ossWallet.domain.model.BudgetPeriod
import com.osscabinet.ossWallet.domain.model.BudgetTrackType
import com.osscabinet.ossWallet.ui.viewmodel.BudgetViewModel
import java.math.BigDecimal

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BudgetEntryScreen(
    budgetId: Long?,
    viewModel: BudgetViewModel,
    onBackClick: () -> Unit
) {
    val budgets by viewModel.budgets.collectAsStateWithLifecycle()
    val existing = remember(budgetId, budgets) {
        budgetId?.let { id -> budgets.find { it.id == id } }
    }

    var name by remember(existing) { mutableStateOf(existing?.name ?: "") }
    var limitText by remember(existing) { mutableStateOf(existing?.limitAmount?.toPlainString() ?: "") }
    var currency by remember(existing) { mutableStateOf(existing?.currency ?: "USD") }
    var period by remember(existing) { mutableStateOf(existing?.period ?: BudgetPeriod.MONTHLY) }
    var trackType by remember(existing) { mutableStateOf(existing?.trackType ?: BudgetTrackType.EXPENSE) }
    var alertThreshold by remember(existing) { mutableStateOf(existing?.alertThreshold ?: 0.8f) }
    var rollover by remember(existing) { mutableStateOf(existing?.rollover ?: false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (budgetId != null) "Edit Budget" else "New Budget") },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(
                        onClick = {
                            val limit = limitText.toBigDecimalOrNull() ?: BigDecimal.ZERO
                            val budget = (existing ?: viewModel.buildDefault(1)).copy(
                                name = name,
                                limitAmount = limit,
                                currency = currency,
                                period = period,
                                trackType = trackType,
                                alertThreshold = alertThreshold,
                                rollover = rollover
                            )
                            viewModel.save(budget)
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
                label = { Text("Budget Name") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = limitText,
                    onValueChange = { limitText = it },
                    label = { Text("Limit") },
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

            Text("Period", style = MaterialTheme.typography.labelMedium)
            var periodExpanded by remember { mutableStateOf(false) }
            ExposedDropdownMenuBox(expanded = periodExpanded, onExpandedChange = { periodExpanded = it }) {
                OutlinedTextField(
                    value = period.name.lowercase().replaceFirstChar { it.uppercase() },
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Resets every") },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = periodExpanded) },
                    modifier = Modifier.fillMaxWidth().menuAnchor()
                )
                ExposedDropdownMenu(expanded = periodExpanded, onDismissRequest = { periodExpanded = false }) {
                    BudgetPeriod.entries.forEach { p ->
                        DropdownMenuItem(
                            text = { Text(p.name.lowercase().replaceFirstChar { it.uppercase() }) },
                            onClick = { period = p; periodExpanded = false }
                        )
                    }
                }
            }

            Text("Track", style = MaterialTheme.typography.labelMedium)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                BudgetTrackType.entries.forEach { type ->
                    FilterChip(
                        selected = trackType == type,
                        onClick = { trackType = type },
                        label = { Text(type.name.lowercase().replaceFirstChar { it.uppercase() }) }
                    )
                }
            }

            Column {
                Text(
                    "Alert at ${(alertThreshold * 100).toInt()}%",
                    style = MaterialTheme.typography.labelMedium
                )
                Slider(
                    value = alertThreshold,
                    onValueChange = { alertThreshold = it },
                    valueRange = 0.5f..1.0f,
                    steps = 9
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("Roll over unused amount", style = MaterialTheme.typography.bodyMedium)
                Switch(checked = rollover, onCheckedChange = { rollover = it })
            }
        }
    }
}
