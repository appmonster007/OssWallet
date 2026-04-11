package com.osscabinet.gleemanager.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.osscabinet.gleemanager.ui.viewmodel.BudgetViewModel
import com.osscabinet.gleemanager.ui.viewmodel.SettingsViewModel
import com.osscabinet.gleemanager.ui.util.formatCurrency

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

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Edit Goal", style = MaterialTheme.typography.titleMedium) },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            Text(
                "Modify your targets",
                style = MaterialTheme.typography.headlineLarge.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onSurface
            )
            
            Text(
                "Adjust your spending limit for $category to align with your monthly financial strategy.",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.outline
            )

            Spacer(modifier = Modifier.height(12.dp))

            EditorialInput(
                value = amount,
                onValueChange = { amount = it },
                label = "LIMIT AMOUNT",
                placeholder = "0.00",
                icon = Icons.Default.Payments
            )

            budget?.let { 
                Text(
                    "Currently spent: ${formatCurrency(it.spent, currency)}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (it.isOverBudget) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.outline
                )
            }

            Spacer(modifier = Modifier.weight(1f))

            Button(
                onClick = { 
                    viewModel.upsertBudget(category, amount.toDoubleOrNull() ?: 0.0)
                    onBackClick()
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                shape = RoundedCornerShape(28.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF006064)),
                enabled = amount.isNotBlank()
            ) {
                Text("SAVE GOAL", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            }
        }
    }
}
