package com.osscabinet.ossWallet.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.osscabinet.ossWallet.domain.model.*
import com.osscabinet.ossWallet.domain.usecase.BudgetAnalysis
import com.osscabinet.ossWallet.ui.components.*
import com.osscabinet.ossWallet.ui.viewmodel.BudgetViewModel
import com.osscabinet.ossWallet.ui.viewmodel.TransactionViewModel
import com.osscabinet.ossWallet.ui.util.formatCurrencyRight
import com.osscabinet.ossWallet.domain.service.FilterService
import com.osscabinet.ossWallet.ui.components.transactions.AeonTransactionItem

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BudgetDetailScreen(
    budgetId: String,
    budgetViewModel: BudgetViewModel,
    transactionViewModel: TransactionViewModel,
    onEditClick: (String) -> Unit,
    onBackClick: () -> Unit
) {
    val state by budgetViewModel.state.collectAsStateWithLifecycle()
    val allTransactionsWithBalance by transactionViewModel.transactionsWithBalance.collectAsStateWithLifecycle()
    
    val analysis = remember(budgetId, state.budgets) {
        state.budgets.find { it.budget.id == budgetId }
    }

    if (analysis == null) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
        return
    }

    val budget = analysis.budget
    val matchingTransactions = remember(budget, allTransactionsWithBalance) {
        allTransactionsWithBalance.filter { 
            FilterService.filterTransactions(listOf(it.transaction), budget.filter).isNotEmpty()
        }.sortedByDescending { it.transaction.timestamp }
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text(budget.name, fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = { onEditClick(budget.id) }) {
                        Icon(Icons.Rounded.Edit, contentDescription = "Edit")
                    }
                }
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
                BudgetAnalysisCard(analysis)
            }

            item {
                AeonFlowSectionHeader(title = "CONSUMING TRANSACTIONS")
            }

            if (matchingTransactions.isEmpty()) {
                item {
                    Box(modifier = Modifier.fillMaxWidth().height(100.dp), contentAlignment = Alignment.Center) {
                        Text("No matching transactions", color = MaterialTheme.colorScheme.outline)
                    }
                }
            } else {
                items(matchingTransactions) { txWithBalance ->
                    val tx = txWithBalance.transaction
                    AeonTransactionItem(
                        category = tx.category,
                        account = tx.accountId, // Should ideally be account name
                        amount = formatCurrencyRight(tx.amount, tx.currency),
                        balanceBefore = formatCurrencyRight(txWithBalance.balanceBefore, tx.currency),
                        balanceAfter = formatCurrencyRight(txWithBalance.balanceAfter, tx.currency),
                        type = if (tx.transactionType == "income") CashFlowType.INCOME else CashFlowType.EXPENSE,
                        timestamp = formatDateTime(tx.timestamp),
                        note = tx.note,
                        tags = tx.tags,
                        status = tx.status,
                        onClick = {}
                    )
                }
            }
        }
    }
}

@Composable
fun BudgetAnalysisCard(analysis: BudgetAnalysis) {
    val budget = analysis.budget
    Surface(
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
                Column {
                    Text("SPENT vs LIMIT", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.outline)
                    Text(
                        "${formatCurrencyRight(budget.spent, budget.currency)} / ${formatCurrencyRight(budget.limit, budget.currency)}",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                }
                
                if (analysis.isHighRisk) {
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.errorContainer,
                        modifier = Modifier.padding(start = 8.dp)
                    ) {
                        Text(
                            "HIGH RISK",
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
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
                color = if (budget.isOverBudget) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
                baseCurrency = budget.currency,
                showLabels = true
            )

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("FIXED", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.outline)
                    Text(formatCurrencyRight(analysis.fixedSpent, budget.currency), style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text("VARIABLE", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.outline)
                    Text(formatCurrencyRight(analysis.variableSpent, budget.currency), style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
                }
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Column(horizontalAlignment = Alignment.Start) {
                    Text("DAILY SAFE", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.outline)
                    Text(
                        formatCurrencyRight(analysis.dailySafeSpend, budget.currency),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text("TIME LEFT", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.outline)
                    Text(
                        "${analysis.daysRemaining} DAYS",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
            
            Text(
                text = "Based on your spending speed, you should keep your daily spend under ${formatCurrencyRight(analysis.dailySafeSpend, budget.currency)} to reach your goal.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
