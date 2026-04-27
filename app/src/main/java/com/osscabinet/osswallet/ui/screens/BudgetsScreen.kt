package com.osscabinet.ossWallet.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.osscabinet.ossWallet.domain.usecase.BudgetAnalysis
import com.osscabinet.ossWallet.ui.components.AeonFlowSectionHeader
import com.osscabinet.ossWallet.ui.components.AeonFloatingActionButton
import com.osscabinet.ossWallet.ui.components.AeonBudgetProgress
import com.osscabinet.ossWallet.ui.viewmodel.BudgetViewModel
import com.osscabinet.ossWallet.ui.viewmodel.SettingsViewModel
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.graphics.Color
import com.osscabinet.ossWallet.ui.util.formatCurrencyRight

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BudgetsScreen(
    viewModel: BudgetViewModel,
    settingsViewModel: SettingsViewModel,
    onBackClick: () -> Unit,
    onAddBudgetClick: () -> Unit,
    onBudgetClick: (String) -> Unit
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val baseCurrency by settingsViewModel.currency.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("Budgets & Intelligence", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        },
        floatingActionButton = {
            AeonFloatingActionButton(
                onClick = onAddBudgetClick,
                icon = Icons.Rounded.Add,
                contentDescription = "New Budget"
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            item {
                AeonFlowSectionHeader(title = "ACTIVE BUDGETS")
            }

            if (state.budgets.isEmpty()) {
                item {
                    Box(modifier = Modifier.fillMaxWidth().height(200.dp), contentAlignment = Alignment.Center) {
                        Text("No active budgets", color = MaterialTheme.colorScheme.outline)
                    }
                }
            } else {
                items(state.budgets) { analysis ->
                    BudgetAnalysisCard(
                        analysis = analysis,
                        baseCurrency = baseCurrency,
                        onClick = { onBudgetClick(analysis.budget.id) }
                    )
                }
            }
        }
    }
}

@Composable
fun BudgetAnalysisCard(
    analysis: BudgetAnalysis,
    baseCurrency: String,
    onClick: () -> Unit
) {
    val budget = analysis.budget
    Surface(
        modifier = Modifier.fillMaxWidth(),
        onClick = onClick,
        shape = RoundedCornerShape(32.dp),
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        tonalElevation = 2.dp
    ) {
        Column(modifier = Modifier.padding(24.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    budget.name.uppercase(),
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.outline
                )
                
                if (analysis.isHighRisk) {
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.errorContainer
                    ) {
                        Text(
                            "HIGH RISK",
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                }
            }

            AeonBudgetProgress(
                label = "",
                current = budget.spent,
                total = budget.limit,
                color = if (budget.isOverBudget) MaterialTheme.colorScheme.error 
                       else if (analysis.isHighRisk) Color(0xFFFFA000)
                       else MaterialTheme.colorScheme.primary,
                baseCurrency = baseCurrency,
                showLabels = true
            )
            
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(
                    "DAILY SAFE: ${formatCurrencyRight(analysis.dailySafeSpend, budget.currency)}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    "${analysis.daysRemaining} DAYS LEFT",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.outline
                )
            }
        }
    }
}
