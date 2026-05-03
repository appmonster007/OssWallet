package com.osscabinet.ossWallet.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.AccountBalance
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.osscabinet.ossWallet.domain.model.Budget
import com.osscabinet.ossWallet.domain.model.BudgetAlertLevel
import com.osscabinet.ossWallet.domain.service.BudgetService
import com.osscabinet.ossWallet.ui.viewmodel.BudgetViewModel
import java.math.BigDecimal

private val TealHealthy = Color(0xFF004D40)
private val RedWarning = Color(0xFFB3261E)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BudgetListScreen(
    viewModel: BudgetViewModel,
    onAddClick: () -> Unit,
    onEditClick: (Long) -> Unit,
    onDetailClick: (Long) -> Unit,
    onBackClick: () -> Unit,
    baseCurrency: String = "USD",
    precision: Int = 2
) {
    val budgets by viewModel.budgets.collectAsStateWithLifecycle()
    val budgetProgress by viewModel.budgetProgress.collectAsStateWithLifecycle()
    val active = budgets.filter { !it.isArchived }
    val archived = budgets.filter { it.isArchived }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(
                        "Budgets",
                        fontWeight = FontWeight.Bold,
                        letterSpacing = (-0.5).sp
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = onAddClick,
                text = { Text("New Budget") },
                icon = { Icon(Icons.Rounded.AccountBalance, contentDescription = null) }
            )
        }
    ) { padding ->
        if (budgets.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize().padding(padding),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Surface(
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.08f),
                        modifier = Modifier.size(72.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                Icons.Rounded.AccountBalance,
                                contentDescription = null,
                                modifier = Modifier.size(36.dp),
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                    Text(
                        "No Budgets Yet",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        "Create a budget to track\nyour spending goals",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                        textAlign = TextAlign.Center
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(padding),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                if (active.isNotEmpty()) {
                    item {
                        BudgetSectionHeader(
                            title = "ACTIVE",
                            sectionColor = MaterialTheme.colorScheme.primary
                        )
                    }
                    items(active, key = { it.id }) { budget ->
                        val progress = budgetProgress[budget.id] ?: BudgetService.buildProgress(budget, BigDecimal.ZERO, BigDecimal.ZERO)
                        BudgetListItem(
                            budget = budget,
                            spentText = progress.spent.toPlainString(),
                            progressFraction = progress.percentage.coerceIn(0f, 1f),
                            alertLevel = progress.alertLevel,
                            onTap = { onDetailClick(budget.id) },
                            onEdit = { onEditClick(budget.id) },
                            onDelete = { viewModel.delete(budget.id) },
                            onArchive = { viewModel.archive(budget) }
                        )
                    }
                }
                if (archived.isNotEmpty()) {
                    item {
                        BudgetSectionHeader(
                            title = "ARCHIVED",
                            sectionColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
                        )
                    }
                    items(archived, key = { it.id }) { budget ->
                        val progress = budgetProgress[budget.id] ?: BudgetService.buildProgress(budget, BigDecimal.ZERO, BigDecimal.ZERO)
                        BudgetListItem(
                            budget = budget,
                            spentText = progress.spent.toPlainString(),
                            progressFraction = progress.percentage.coerceIn(0f, 1f),
                            alertLevel = progress.alertLevel,
                            onTap = { onDetailClick(budget.id) },
                            onEdit = { onEditClick(budget.id) },
                            onDelete = { viewModel.delete(budget.id) },
                            onArchive = {}
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun BudgetSectionHeader(title: String, sectionColor: Color) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.padding(vertical = 8.dp, horizontal = 4.dp)
    ) {
        Box(
            modifier = Modifier
                .size(10.dp)
                .clip(CircleShape)
                .background(sectionColor)
        )
        Text(
            title,
            style = MaterialTheme.typography.labelSmall.copy(
                fontWeight = FontWeight.ExtraBold,
                fontSize = 11.sp,
                letterSpacing = 1.5.sp
            ),
            color = sectionColor
        )
    }
}

@Composable
private fun BudgetListItem(
    budget: Budget,
    spentText: String,
    progressFraction: Float,
    alertLevel: BudgetAlertLevel,
    onTap: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onArchive: () -> Unit
) {
    var showMenu by remember { mutableStateOf(false) }
    val progressColor = when (alertLevel) {
        BudgetAlertLevel.EXCEEDED, BudgetAlertLevel.WARNING -> RedWarning
        BudgetAlertLevel.NONE -> TealHealthy
    }
    val periodLabel = budget.period.name.lowercase().replaceFirstChar { it.uppercase() }

    Surface(
        onClick = onTap,
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        border = BorderStroke(0.5.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.15f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Icon circle
                Surface(
                    shape = CircleShape,
                    color = progressColor.copy(alpha = 0.1f),
                    modifier = Modifier.size(40.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            Icons.Rounded.AccountBalance,
                            contentDescription = null,
                            modifier = Modifier.size(20.dp),
                            tint = progressColor
                        )
                    }
                }
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    Text(
                        budget.name,
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        periodLabel,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                    )
                }
                Text(
                    "${budget.limitAmount.toPlainString()} ${budget.currency}",
                    style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold),
                    color = progressColor
                )
                Box {
                    IconButton(
                        onClick = { showMenu = true },
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            Icons.Rounded.MoreVert,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    DropdownMenu(expanded = showMenu, onDismissRequest = { showMenu = false }) {
                        DropdownMenuItem(text = { Text("Edit") }, onClick = { showMenu = false; onEdit() })
                        if (!budget.isArchived) {
                            DropdownMenuItem(
                                text = { Text("Archive") },
                                onClick = { showMenu = false; onArchive() }
                            )
                        }
                        DropdownMenuItem(
                            text = { Text("Delete", color = MaterialTheme.colorScheme.error) },
                            onClick = { showMenu = false; onDelete() }
                        )
                    }
                }
            }
            LinearProgressIndicator(
                progress = { progressFraction },
                modifier = Modifier.fillMaxWidth().height(8.dp),
                color = progressColor,
                trackColor = progressColor.copy(alpha = 0.15f),
                strokeCap = StrokeCap.Round
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    "$spentText spent",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                )
                Text(
                    "${(progressFraction * 100).toInt()}%",
                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                    color = progressColor
                )
            }
        }
    }
}
