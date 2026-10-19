package com.osscabinet.ossWallet.ui.screens.budgets.list

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AccountBalance
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.osscabinet.ossWallet.domain.model.Budget
import com.osscabinet.ossWallet.domain.model.BudgetAlertLevel
import com.osscabinet.ossWallet.domain.service.management.BudgetService
import com.osscabinet.ossWallet.ui.components.AeonHazeProvider
import com.osscabinet.ossWallet.ui.components.AeonTopAppBar
import com.osscabinet.ossWallet.ui.components.AeonTopBarNavTitlePill
import com.osscabinet.ossWallet.ui.components.AeonTopBarSection
import com.osscabinet.ossWallet.ui.components.aeonHazeSource
import com.osscabinet.ossWallet.ui.components.management.ManagementListItem
import com.osscabinet.ossWallet.ui.components.management.ManagementSectionHeader
import com.osscabinet.ossWallet.ui.components.navigation.UnifiedPillNavigation
import com.osscabinet.ossWallet.ui.components.scrollBottomPadding
import com.osscabinet.ossWallet.ui.screens.budgets.BudgetViewModel
import com.osscabinet.ossWallet.ui.theme.AeonSize
import com.osscabinet.ossWallet.ui.theme.AeonTheme
import com.osscabinet.ossWallet.ui.theme.AppColors
import com.osscabinet.ossWallet.ui.theme.AppTextStyles
import com.osscabinet.ossWallet.ui.theme.HazeLayer
import java.math.BigDecimal


/**
 * A Value Object representing a valid configuration for the Budget List screen.
 */
data class BudgetListConfig(
    val baseCurrency: String = "USD"
)

/**
 * Grouped actions for the Budget List screen.
 */
data class BudgetListActions(
    val onBackClick: () -> Unit,
    val onAddClick: () -> Unit,
    val onEditClick: (Long) -> Unit,
    val onDetailClick: (Long) -> Unit
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BudgetListScreen(
    config: BudgetListConfig,
    actions: BudgetListActions,
    viewModel: BudgetViewModel
) {
    val navigationBarHeight = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
    val budgets by viewModel.budgets.collectAsStateWithLifecycle()
    val budgetProgress by viewModel.budgetProgress.collectAsStateWithLifecycle()
    val active = budgets.filter { !it.isArchived }
    val archived = budgets.filter { it.isArchived }

    AeonHazeProvider(backgroundColor = AppColors.screenBackground) { hazeLayers ->
        Box(modifier = Modifier.fillMaxSize()) {
            Scaffold(
                containerColor = Color.Transparent
            ) { innerPadding ->
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .aeonHazeSource(hazeLayers.states[HazeLayer.Overlay])
                ) {
                    if (budgets.isEmpty()) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .statusBarsPadding()
                                .padding(top = AeonSize.D64.value),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(AeonTheme.dimensions.spacingMd)
                            ) {
                                Surface(
                                    shape = CircleShape,
                                    color = AppColors.primary.copy(alpha = 0.08f),
                                    modifier = Modifier.size(AeonTheme.dimensions.iconSize3xl)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Icon(
                                            Icons.Rounded.AccountBalance,
                                            contentDescription = null,
                                            modifier = Modifier.size(AeonTheme.dimensions.iconSizeXl),
                                            tint = AppColors.primary
                                        )
                                    }
                                }
                                Text(
                                    "No Budgets Yet",
                                    style = AppTextStyles.calculatorKey,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    "Create a budget to track\nyour spending goals",
                                    style = AppTextStyles.transactionTitle,
                                    color = AppColors.contentSecondary.copy(alpha = 0.7f),
                                    textAlign = TextAlign.Center
                                )
                            }
                        }
                    } else {
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(
                                start = AeonTheme.dimensions.screenPadding,
                                end = AeonTheme.dimensions.screenPadding,
                                top = AeonSize.D64.value + AeonTheme.dimensions.screenPadding + 48.dp, // approx status bar
                                bottom = innerPadding.calculateBottomPadding() + scrollBottomPadding()
                            ),
                            verticalArrangement = Arrangement.spacedBy(AeonTheme.dimensions.listPadding)
                        ) {
                            if (active.isNotEmpty()) {
                                item {
                                    ManagementSectionHeader(
                                        title = "ACTIVE",
                                        sectionColor = AppColors.primary
                                    )
                                }
                                items(active, key = { it.id }) { budget ->
                                    val progress =
                                        budgetProgress[budget.id] ?: BudgetService.buildProgress(
                                            budget,
                                            BigDecimal.ZERO,
                                            BigDecimal.ZERO
                                        )
                                    BudgetListItem(
                                        budget = budget,
                                        spentText = progress.spent.toPlainString(),
                                        progressFraction = progress.percentage.coerceIn(0f, 1f),
                                        alertLevel = progress.alertLevel,
                                        onTap = { actions.onDetailClick(budget.id) },
                                        onEdit = { actions.onEditClick(budget.id) },
                                        onDelete = { viewModel.delete(budget.id) },
                                        onArchive = { viewModel.archive(budget) }
                                    )
                                }
                            }
                            if (archived.isNotEmpty()) {
                                item {
                                    ManagementSectionHeader(
                                        title = "ARCHIVED",
                                        sectionColor = AppColors.contentSecondary.copy(alpha = 0.4f)
                                    )
                                }
                                items(archived, key = { it.id }) { budget ->
                                    val progress =
                                        budgetProgress[budget.id] ?: BudgetService.buildProgress(
                                            budget,
                                            BigDecimal.ZERO,
                                            BigDecimal.ZERO
                                        )
                                    BudgetListItem(
                                        budget = budget,
                                        spentText = progress.spent.toPlainString(),
                                        progressFraction = progress.percentage.coerceIn(0f, 1f),
                                        alertLevel = progress.alertLevel,
                                        onTap = { actions.onDetailClick(budget.id) },
                                        onEdit = { actions.onEditClick(budget.id) },
                                        onDelete = { viewModel.delete(budget.id) },
                                        onArchive = {}
                                    )
                                }
                            }
                        }
                    }
                }
            }

            UnifiedPillNavigation(
                selectedTab = 0,
                onTabSelected = {},
                tabs = emptyList(),
                fabPaddingBottom = 16.dp + navigationBarHeight,
                onFabClick = actions.onAddClick,
                fabIcon = Icons.Rounded.AccountBalance,
                showFab = true
            )

            AeonTopAppBar(
                left = AeonTopBarSection(
                    content = {
                        AeonTopBarNavTitlePill(
                            title = "Budgets",
                            onBackClick = actions.onBackClick
                        )
                    }
                )
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
        BudgetAlertLevel.EXCEEDED, BudgetAlertLevel.WARNING -> AppColors.error
        BudgetAlertLevel.NONE -> AppColors.primary
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
                com.osscabinet.ossWallet.ui.util.CurrencyReprUi.formatCurrencyAnnotated(
                    amount = budget.limitAmount,
                    currencyCode = budget.currency,
                    spec = com.osscabinet.ossWallet.ui.util.CurrencyDisplaySpec(useSemanticColor = false)
                ),
                style = AppTextStyles.listItemTitle.copy(fontWeight = FontWeight.Bold),
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
                text = { Text("Delete", color = AppColors.error) },
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
                        style = AppTextStyles.transactionSwipeAction,
                        color = AppColors.contentSecondary.copy(alpha = 0.8f)
                    )
                    Text(
                        "${(progressFraction * 100).toInt()}%",
                        style = AppTextStyles.formSectionLabel,
                        color = progressColor
                    )
                }
            }
        }
    )
}
