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
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BudgetsScreen(
    viewModel: BudgetViewModel,
    onBackClick: () -> Unit,
    onEditBudgetClick: (String) -> Unit
) {
    val budgets by viewModel.budgets.collectAsState()

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
                .background(MaterialTheme.colorScheme.surface)
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
                    BudgetGoalCard(budget, onClick = { onEditBudgetClick(budget.category) })
                }
            }

            item {
                InsightSection()
            }
        }
    }
}

@Composable
fun BudgetGoalCard(budget: Budget, onClick: () -> Unit) {
    val color = when(budget.category.lowercase()) {
        "lifestyle", "lifestyle & leisure" -> Color(0xFF004D40)
        "housing", "housing & utilities" -> Color(0xFFC62828)
        else -> Color(0xFF006064)
    }

    Surface(
        modifier = Modifier.fillMaxWidth(),
        onClick = onClick,
        shape = RoundedCornerShape(32.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
    ) {
        Column(modifier = Modifier.padding(24.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text(budget.category, style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold))
                Text(
                    "$${String.format(Locale.getDefault(), "%,.0f", budget.spent)} / $${String.format(Locale.getDefault(), "%,.0f", budget.limit)}",
                    style = MaterialTheme.typography.labelSmall.copy(fontFeatureSettings = "tnum"),
                    color = MaterialTheme.colorScheme.outline
                )
            }
            
            LinearProgressIndicator(
                progress = { budget.progress },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp)
                    .clip(CircleShape),
                color = color,
                trackColor = color.copy(alpha = 0.1f)
            )
            
            Text(
                text = if (!budget.isOverBudget) "You have $${String.format(Locale.getDefault(), "%,.0f", budget.remaining)} remaining" else "Over budget by $${String.format(Locale.getDefault(), "%,.0f", budget.spent - budget.limit)}",
                style = MaterialTheme.typography.bodySmall,
                color = if (!budget.isOverBudget) MaterialTheme.colorScheme.outline else Color(0xFFC62828)
            )
        }
    }
}
