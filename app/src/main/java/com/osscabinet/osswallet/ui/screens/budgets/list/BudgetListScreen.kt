package com.osscabinet.ossWallet.ui.screens.budgets.list

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.rounded.AccountBalance
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
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
import com.osscabinet.ossWallet.domain.service.management.BudgetService
import com.osscabinet.ossWallet.ui.theme.AeonTheme
import com.osscabinet.ossWallet.ui.components.navigation.UnifiedPillNavigation
import com.osscabinet.ossWallet.ui.components.management.ManagementListItem
import com.osscabinet.ossWallet.ui.components.management.ManagementSectionHeader
import com.osscabinet.ossWallet.ui.screens.budgets.BudgetViewModel
import java.math.BigDecimal

private val TealHealthy = Color(0xFF004D40)
private val RedWarning = Color(0xFFB3261E)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BudgetListScreen(
    onBackClick: () -> Unit,
    viewModel: BudgetViewModel,
    onAddClick: () -> Unit,
    onEditClick: (Long) -> Unit,
    onDetailClick: (Long) -> Unit,
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
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        }
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize()) {
            if (budgets.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(AeonTheme.dimensions.spacingMd)
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.08f),
                            modifier = Modifier.size(AeonTheme.dimensions.iconSize3xl)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    Icons.Rounded.AccountBalance,
                                    contentDescription = null,
                                    modifier = Modifier.size(AeonTheme.dimensions.iconSizeXl),
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
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding),
                    contentPadding = PaddingValues(AeonTheme.dimensions.screenPadding),
                    verticalArrangement = Arrangement.spacedBy(AeonTheme.dimensions.listPadding)
                ) {
                    if (active.isNotEmpty()) {
                        item {
                            ManagementSectionHeader(
                                title = "ACTIVE",
                                sectionColor = MaterialTheme.colorScheme.primary
                            )
                        }
                        items(active, key = { it.id }) { budget ->
                            val progress = budgetProgress[budget.id] ?: BudgetService.buildProgress(
                                budget,
                                BigDecimal.ZERO,
                                BigDecimal.ZERO
                            )
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
                            ManagementSectionHeader(
                                title = "ARCHIVED",
                                sectionColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
                            )
                        }
                        items(archived, key = { it.id }) { budget ->
                            val progress = budgetProgress[budget.id] ?: BudgetService.buildProgress(
                                budget,
                                BigDecimal.ZERO,
                                BigDecimal.ZERO
                            )
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

            UnifiedPillNavigation(
                selectedTab = 0,
                onTabSelected = {},
                tabs = emptyList(),
                onFabClick = onAddClick,
                fabIcon = Icons.Rounded.AccountBalance,
                showFab = true
            )
        }
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
    val progressColor = when (alertLevel) {
        BudgetAlertLevel.EXCEEDED, BudgetAlertLevel.WARNING -> RedWarning
        BudgetAlertLevel.NONE -> TealHealthy
    }
    val periodLabel = budget.timelineRange.toDisplayString()

    ManagementListItem(
        onClick = onTap,
        icon = Icons.Rounded.AccountBalance,
        iconTint = progressColor,
        title = budget.name,
        subtitle = periodLabel,
        isWarning = alertLevel != BudgetAlertLevel.NONE,
        trailingContent = {
            Text(
                "${budget.limitAmount.toPlainString()} ${budget.currency}",
                style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold),
                color = progressColor
            )
        },
        menuItems = { onDismiss ->
            DropdownMenuItem(
                text = { Text("Edit") },
                onClick = { onDismiss(); onEdit() })
            if (!budget.isArchived) {
                DropdownMenuItem(
                    text = { Text("Archive") },
                    onClick = { onDismiss(); onArchive() }
                )
            }
            DropdownMenuItem(
                text = { Text("Delete", color = MaterialTheme.colorScheme.error) },
                onClick = { onDismiss(); onDelete() }
            )
        },
        additionalContent = {
            Column(verticalArrangement = Arrangement.spacedBy(AeonTheme.dimensions.spacingSm)) {
                LinearProgressIndicator(
                    progress = { progressFraction },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(AeonTheme.dimensions.spacingSm),
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
    )
}
