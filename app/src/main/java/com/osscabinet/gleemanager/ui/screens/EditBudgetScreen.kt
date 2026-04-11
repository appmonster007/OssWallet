package com.osscabinet.gleemanager.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.osscabinet.gleemanager.ui.components.EditorialInput
import com.osscabinet.gleemanager.ui.components.Numpad
import com.osscabinet.gleemanager.ui.util.formatCurrencyRight
import com.osscabinet.gleemanager.ui.viewmodel.BudgetViewModel
import com.osscabinet.gleemanager.ui.viewmodel.SettingsViewModel
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditBudgetScreen(
    category: String,
    viewModel: BudgetViewModel,
    settingsViewModel: SettingsViewModel,
    onBackClick: () -> Unit
) {
    val budgets by viewModel.budgets.collectAsState()
    val currency by settingsViewModel.currency.collectAsState()
    val budget = budgets.find { it.category == category }
    var amount by remember { mutableStateOf(budget?.limit?.toString() ?: "") }

    // Calculator State
    var firstOperand by remember { mutableStateOf<Double?>(null) }
    var operator by remember { mutableStateOf<String?>(null) }
    var isEnteringSecondOperand by remember { mutableStateOf(false) }

    fun calculateResult() {
        if (firstOperand != null && operator != null && amount.isNotEmpty()) {
            val secondOperand = amount.toDoubleOrNull() ?: 0.0
            val rawResult = when (operator) {
                "+" -> firstOperand!! + secondOperand
                "−" -> firstOperand!! - secondOperand
                "×" -> firstOperand!! * secondOperand
                "÷" -> if (secondOperand != 0.0) firstOperand!! / secondOperand else 0.0
                else -> secondOperand
            }
            val result = kotlin.math.abs(rawResult)
            amount = if (result % 1 == 0.0) result.toLong().toString() else String.format(Locale.getDefault(), "%.2f", result)
            firstOperand = null
            operator = null
            isEnteringSecondOperand = false
        }
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("Edit Goal", style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)) },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 20.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Header Card: Large Amount Display (Aeon Style)
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(40.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        "SET SPENDING LIMIT",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.outline,
                        letterSpacing = 1.sp
                    )

                    Text(
                        text = amount.ifEmpty { "0.00" },
                        style = TextStyle(
                            fontSize = 48.sp,
                            fontWeight = FontWeight.Black,
                            textAlign = TextAlign.Center,
                            color = MaterialTheme.colorScheme.primary
                        )
                    )
                    
                    Text(
                        currency,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.outline
                    )
                }
            }

            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    "Target Details",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 8.dp)
                )

                EditorialInput(
                    value = category,
                    onValueChange = { },
                    label = "CLASSIFICATION",
                    icon = Icons.Rounded.Category,
                    readOnly = true
                )

                budget?.let {
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(28.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f)
                    ) {
                        Row(
                            modifier = Modifier.padding(20.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                if (it.isOverBudget) Icons.Rounded.Warning else Icons.Rounded.Info,
                                contentDescription = null,
                                tint = if (it.isOverBudget) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(16.dp))
                            Column {
                                Text(
                                    "CURRENT CONSUMPTION",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.outline
                                )
                                Text(
                                    formatCurrencyRight(it.spent, currency),
                                    style = MaterialTheme.typography.bodyLarge.copy(
                                        fontWeight = FontWeight.Bold,
                                        color = if (it.isOverBudget) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface
                                    )
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.weight(1f))

            Numpad(
                onNumberClick = { input ->
                    if (isEnteringSecondOperand) {
                        amount = if (input == ".") "0." else input
                        isEnteringSecondOperand = false
                    } else {
                        if (input == ".") {
                            if (!amount.contains(".")) {
                                amount += if (amount.isEmpty()) "0." else "."
                            }
                        } else if (input == "00") {
                            if (amount.isNotEmpty() && amount != "0") amount += "00"
                        } else {
                            amount = if (amount == "0") input else amount + input
                        }
                    }
                },
                onDeleteClick = {
                    if (amount.isNotEmpty()) amount = amount.dropLast(1)
                },
                onOperatorClick = { op ->
                    if (amount.isNotEmpty()) {
                        if (operator != null) calculateResult()
                        firstOperand = amount.toDoubleOrNull()
                        operator = op
                        isEnteringSecondOperand = true
                    }
                },
                onEqualClick = { calculateResult() },
                onClearClick = {
                    amount = ""
                    firstOperand = null
                    operator = null
                    isEnteringSecondOperand = false
                },
                modifier = Modifier.fillMaxWidth().weight(1f)
            )

            Button(
                onClick = { 
                    viewModel.upsertBudget(category, amount.toDoubleOrNull() ?: 0.0)
                    onBackClick()
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(64.dp),
                shape = RoundedCornerShape(32.dp),
                enabled = amount.isNotBlank()
            ) {
                Text("SAVE GOAL", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
            }
        }
    }
}
