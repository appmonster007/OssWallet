package com.osscabinet.ossWallet.ui.dashboard.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.osscabinet.ossWallet.domain.model.timeline.TimelineRange
import com.osscabinet.ossWallet.domain.usecase.BudgetAnalysis
import com.osscabinet.ossWallet.ui.components.AeonBudgetProgress
import com.osscabinet.ossWallet.ui.components.AeonFlowSectionHeader
import com.osscabinet.ossWallet.ui.components.AeonSettingsToggle
import com.osscabinet.ossWallet.ui.navigation.RecordsFilterContext
import kotlinx.serialization.Serializable

@Serializable
data class BudgetSettings(
    val showProgressLabels: Boolean = true
)

@Composable
fun BudgetsCard(
    onManageClick: () -> Unit,
    onBudgetClick: (String) -> Unit = {},
    onRecordsClick: ((RecordsFilterContext) -> Unit)? = null,
    budgets: List<BudgetAnalysis>,
    baseCurrency: String,
    cardId: String,
    modifier: Modifier = Modifier,
    headerModifier: Modifier = Modifier,
    precision: Int = 2,
    showProgressLabels: Boolean = true,
    timelineRange: TimelineRange = TimelineRange.AllTime,
    onShowProgressLabelsChange: (String, Boolean) -> Unit = { _, _ -> },
    onDelete: () -> Unit = {}
) {
    var showSettings by remember { mutableStateOf(false) }

    // Sort: High Risk > Over Budget > Highest Progress
    val sortedBudgets = remember(budgets) {
        budgets.sortedWith(
            compareByDescending<BudgetAnalysis> { it.isHighRisk }
                .thenByDescending { it.budget.isOverBudget }
                .thenByDescending { it.budget.progress }
        )
    }

    Card(
        modifier = modifier,
        shape = RoundedCornerShape(24.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer)
    ) {
        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            AeonFlowSectionHeader(
                title = "BUDGET INTELLIGENCE",
                modifier = headerModifier,
                trailing = {
                    IconButton(
                        onClick = { showSettings = true },
                        modifier = Modifier.size(24.dp)
                    ) {
                        Icon(
                            Icons.Default.MoreHoriz,
                            contentDescription = "Settings",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            )

            if (sortedBudgets.isEmpty()) {
                Box(modifier = Modifier.fillMaxWidth().height(100.dp), contentAlignment = Alignment.Center) {
                    Text("No active budgets", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.outline)
                }
            } else {
                Column(
                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 4.dp),
                    verticalArrangement = Arrangement.spacedBy(20.dp)
                ) {
                    sortedBudgets.forEach { analysis ->
                        val budget = analysis.budget
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { 
                                    onBudgetClick(budget.id)
                                    onRecordsClick?.let { it(RecordsFilterContext.Custom(
                                        categoryIds = budget.filter.categoryIds.map { it.toString() },
                                        accountIds = budget.filter.accountIds,
                                        startDate = null, // Filter handles period
                                        endDate = null
                                    )) }
                                },
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Text(
                                        text = budget.name.uppercase(),
                                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    if (analysis.isHighRisk) {
                                        Surface(
                                            shape = CircleShape,
                                            color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.5f),
                                            modifier = Modifier.size(16.dp)
                                        ) {
                                            Icon(Icons.Default.Warning, contentDescription = "High Risk", tint = MaterialTheme.colorScheme.error, modifier = Modifier.padding(2.dp))
                                        }
                                    }
                                }
                                
                                if (showProgressLabels) {
                                    Text(
                                        text = "DAILY SAFE: ${analysis.dailySafeSpend} $baseCurrency",
                                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                                        color = if (analysis.isHighRisk) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary.copy(alpha = 0.6f)
                                    )
                                }
                            }

                            AeonBudgetProgress(
                                label = "", // Using custom label row above
                                current = budget.spent,
                                total = budget.limit,
                                color = if (budget.isOverBudget) MaterialTheme.colorScheme.error 
                                       else if (analysis.isHighRisk) Color(0xFFFFA000)
                                       else MaterialTheme.colorScheme.primary,
                                baseCurrency = baseCurrency,
                                precision = precision,
                                showLabels = false // We handle labels customly
                            )
                        }
                    }
                }
            }
        }
    }

    if (showSettings) {
        AlertDialog(
            onDismissRequest = { showSettings = false },
            dismissButton = {
                TextButton(
                    onClick = onDelete,
                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                ) {
                    Icon(Icons.Default.Delete, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Delete")
                }
            },
            confirmButton = {
                TextButton(onClick = { showSettings = false }) {
                    Text("Done")
                }
            },
            title = {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Budget Settings", style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold))
                    IconButton(
                        onClick = { onManageClick(); showSettings = false },
                        colors = IconButtonDefaults.iconButtonColors(
                            containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f),
                            contentColor = MaterialTheme.colorScheme.primary
                        )
                    ) {
                        Icon(Icons.Default.Settings, contentDescription = "Manage")
                    }
                }
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    AeonSettingsToggle(
                        label = "SHOW ANALYTICS LABELS",
                        checked = showProgressLabels,
                        onCheckedChange = { show -> onShowProgressLabelsChange(cardId, show) }
                    )
                }
            },
            shape = RoundedCornerShape(32.dp),
            containerColor = MaterialTheme.colorScheme.surfaceContainer
        )
    }
}
