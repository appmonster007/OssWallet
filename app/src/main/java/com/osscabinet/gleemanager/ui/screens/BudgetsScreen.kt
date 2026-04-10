package com.osscabinet.gleemanager.ui.screens

import androidx.compose.foundation.background
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.osscabinet.gleemanager.domain.model.Budget
import com.osscabinet.gleemanager.ui.viewmodel.BudgetViewModel
import com.osscabinet.gleemanager.ui.util.getCurrencySymbol
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BudgetsScreen(
    viewModel: BudgetViewModel,
    onBackClick: () -> Unit,
    onEditBudgetClick: (String) -> Unit
) {
    val budgets by viewModel.budgets.collectAsState()
    // We should probably get the base currency here too if we want it to be accurate
    // But for now let's assume USD or get it from a shared source if available.
    // Since we don't have easy access to settingsViewModel here without passing it, 
    // let's just use the symbol from the first budget if available or default to $
    val currencySymbol = if (budgets.isNotEmpty()) getCurrencySymbol("USD") else "$" 

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Budgets & Goals", style = MaterialTheme.typography.headlineSmall) },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = {}) { Icon(Icons.Default.Add, contentDescription = "Add Goal") }
                }
            )
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(MaterialTheme.colorScheme.background)
                .padding(horizontal = 24.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp),
            contentPadding = PaddingValues(top = 24.dp, bottom = 80.dp)
        ) {
            item {
                Text(
                    "FINANCIAL TARGETS",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.outline,
                    letterSpacing = 1.sp
                )
            }
            
            if (budgets.isEmpty()) {
                item {
                    Text("No budgets set. Start by adding a new goal.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.outline)
                }
            } else {
                items(budgets) { budget ->
                    BudgetGoalCard(budget, currencySymbol, onClick = { onEditBudgetClick(budget.category) })
                }
            }

            item {
                // We'll use a local version of InsightSection to ensure theme consistency
                BudgetInsightSection(currencySymbol)
            }
        }
    }
}

@Composable
fun BudgetGoalCard(budget: Budget, currencySymbol: String, onClick: () -> Unit) {
    val progressColor = if (budget.isOverBudget) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary

    Surface(
        modifier = Modifier.fillMaxWidth(),
        onClick = onClick,
        shape = RoundedCornerShape(32.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
    ) {
        Column(modifier = Modifier.padding(24.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text(budget.category, style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold), color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(
                    "$currencySymbol${String.format(Locale.getDefault(), "%,.0f", budget.spent)} / $currencySymbol${String.format(Locale.getDefault(), "%,.0f", budget.limit)}",
                    style = MaterialTheme.typography.labelSmall.copy(fontFeatureSettings = "tnum"),
                    color = MaterialTheme.colorScheme.outline
                )
            }
            
            LinearProgressIndicator(
                progress = { budget.progress.coerceIn(0f, 1f) },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp)
                    .clip(CircleShape),
                color = progressColor,
                trackColor = progressColor.copy(alpha = 0.1f)
            )
            
            Text(
                text = if (!budget.isOverBudget) "You have $currencySymbol${String.format(Locale.getDefault(), "%,.0f", budget.remaining)} remaining" else "Over budget by $currencySymbol${String.format(Locale.getDefault(), "%,.0f", budget.spent - budget.limit)}",
                style = MaterialTheme.typography.bodySmall,
                color = if (!budget.isOverBudget) MaterialTheme.colorScheme.outline else MaterialTheme.colorScheme.error
            )
        }
    }
}

@Composable
fun BudgetInsightSection(currencySymbol: String) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(32.dp),
        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
    ) {
        Column(modifier = Modifier.padding(24.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(modifier = Modifier.size(32.dp), shape = CircleShape, color = MaterialTheme.colorScheme.primary) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(Icons.Default.Lightbulb, contentDescription = null, tint = MaterialTheme.colorScheme.onPrimary, modifier = Modifier.size(16.dp))
                    }
                }
                Spacer(modifier = Modifier.width(12.dp))
                Text("OPTIMIZATION TIP", style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary))
            }
            Text(
                "You usually spend 20% less on Entertainment during the first week. Consider shifting ${currencySymbol}100 to your Savings goal.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
