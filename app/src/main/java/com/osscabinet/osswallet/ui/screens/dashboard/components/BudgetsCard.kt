package com.osscabinet.ossWallet.ui.screens.dashboard.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.DonutLarge
import androidx.compose.material.icons.rounded.ErrorOutline
import androidx.compose.material.icons.rounded.Savings
import androidx.compose.foundation.clickable
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.osscabinet.ossWallet.domain.card.CardFilterRules
import com.osscabinet.ossWallet.domain.model.BudgetAlertLevel
import com.osscabinet.ossWallet.domain.model.BudgetProgress
import com.osscabinet.ossWallet.domain.model.TransactionFilter
import com.osscabinet.ossWallet.domain.model.timeline.TimelineRange
import com.osscabinet.ossWallet.domain.model.settings.BudgetsCardConfig
import com.osscabinet.ossWallet.domain.service.currency.CurrencyFormatter.formatAuto
import com.osscabinet.ossWallet.ui.components.cards.AeonExpressiveCard
import com.osscabinet.ossWallet.ui.components.cards.CardCallbacks
import com.osscabinet.ossWallet.ui.components.entry.EntrySwitchRow
import com.osscabinet.ossWallet.ui.components.cards.CardSettingsSection
import com.osscabinet.ossWallet.ui.components.cards.CardSettingsSheet
import com.osscabinet.ossWallet.ui.components.cards.FilterSelectionContext
import com.osscabinet.ossWallet.ui.components.GlassSurface
import com.osscabinet.ossWallet.ui.theme.AppColors
import com.osscabinet.ossWallet.ui.theme.AppTextStyles
import com.osscabinet.ossWallet.ui.theme.GlassVariant
import com.osscabinet.ossWallet.ui.theme.PositiveAmountColor

@Composable
fun BudgetsCard(
    items: List<BudgetProgress>,
    config: BudgetsCardConfig,
    callbacks: CardCallbacks.Budgets = CardCallbacks.Budgets(),
    filterContext: FilterSelectionContext = FilterSelectionContext(),
    filterRules: CardFilterRules? = null,
    baseFilter: TransactionFilter = TransactionFilter(),
    inheritedFilter: TransactionFilter = TransactionFilter(),
    screenOwnsTimeline: Boolean = false,
    showFilterButton: Boolean = true,
    modifier: Modifier = Modifier,
    headerModifier: Modifier = Modifier
) {
    val displayFilter =
        if (screenOwnsTimeline || inheritedFilter.timelineRange !is TimelineRange.AllTime)
            config.filter.copy(timelineRange = inheritedFilter.timelineRange)
        else config.filter

    AeonExpressiveCard(
        title = config.title,
        icon = Icons.Rounded.DonutLarge,
        modifier = modifier,
        headerModifier = headerModifier,
        onClick = callbacks.onSeeAll,
        filterEnabled = showFilterButton,
        filter = displayFilter,
        baseFilter = baseFilter,
        filterRules = filterRules ?: config.filterRules,
        filterContext = filterContext,
        onFilterChange = { callbacks.onFilterChange(config.cardId, it) },
        onSaveFilterClick = callbacks.onSaveFilterClick,
        displaySettings = { onDismiss ->
            BudgetsSettingsSheet(config = config, callbacks = callbacks, onDismiss = onDismiss)
        }
    ) {
        if (items.isEmpty()) {
            Text(
                text = "No budgets yet",
                style = AppTextStyles.caption,
                color = AppColors.contentSecondary,
                modifier = Modifier.padding(vertical = 8.dp)
            )
        } else {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                items.forEach { progress ->
                    BudgetProgressRow(
                        progress = progress,
                        onClick = { callbacks.onBudgetClick(progress.budget.id) }
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun BudgetsSettingsSheet(
    config: BudgetsCardConfig,
    callbacks: CardCallbacks.Budgets,
    onDismiss: () -> Unit
) {
    CardSettingsSheet(title = "Budgets", onDismiss = onDismiss, onDelete = callbacks.onDelete) {
        CardSettingsSection("Display") {
            EntrySwitchRow(
                label = "Show archived budgets",
                checked = config.showArchived,
                onCheckedChange = { callbacks.onUpdate(config.copy(showArchived = it)) },
                isFlush = true
            )
        }
    }
}

@Composable
private fun BudgetProgressRow(
    progress: BudgetProgress,
    onClick: () -> Unit
) {
    val trackColor = when (progress.alertLevel) {
        BudgetAlertLevel.EXCEEDED -> AppColors.error
        BudgetAlertLevel.WARNING -> AppColors.error.copy(alpha = 0.8f)
        BudgetAlertLevel.NONE -> PositiveAmountColor
    }

    GlassSurface(
        modifier = Modifier.clickable { onClick() },
        shape = RoundedCornerShape(24.dp),
        glassVariant = GlassVariant.Item,
        fallbackColor = AppColors.cardSurfaceProminent,
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        if (progress.alertLevel == BudgetAlertLevel.EXCEEDED) Icons.Rounded.ErrorOutline else Icons.Rounded.Savings,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                        tint = trackColor.copy(alpha = 0.8f)
                    )
                    Text(
                        text = progress.budget.name,
                        style = AppTextStyles.screenSubTitle.copy(
                            fontWeight = FontWeight.Bold,
                            letterSpacing = (-0.2).sp
                        ),
                        color = AppColors.contentPrimary
                    )
                }
                Text(
                    text = "${(progress.percentage * 100).toInt()}%",
                    style = AppTextStyles.drawerItemLabel.copy(
                        fontWeight = FontWeight.Black,
                        fontFeatureSettings = "tnum"
                    ),
                    color = trackColor
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            LinearProgressIndicator(
                progress = { progress.percentage.coerceIn(0f, 1f) },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(10.dp),
                color = trackColor,
                trackColor = AppColors.componentOnCard,
                strokeCap = StrokeCap.Round
            )

            Spacer(modifier = Modifier.height(14.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = if (progress.alertLevel == BudgetAlertLevel.EXCEEDED) "OVER BUDGET" else "REMAINING",
                        style = AppTextStyles.transactionSwipeAction.copy(
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.sp,
                            fontSize = 12.sp
                        ),
                        color = trackColor
                    )
                    Text(
                        text = if (progress.alertLevel == BudgetAlertLevel.EXCEEDED) {
                            formatAuto(
                                progress.spent.subtract(progress.limit),
                                progress.budget.currency,
                                shorten = true
                            )
                        } else {
                            formatAuto(
                                progress.remaining,
                                progress.budget.currency,
                                shorten = true
                            )
                        },
                        style = AppTextStyles.listItemTitle.copy(
                            fontWeight = FontWeight.ExtraBold,
                            color = if (progress.alertLevel == BudgetAlertLevel.EXCEEDED) AppColors.error else AppColors.contentPrimary
                        )
                    )
                }

                Surface(
                    color = AppColors.insetSurface, // Level 3: Deepest unit
                    shape = RoundedCornerShape(8.dp),
                    border = BorderStroke(1.dp, AppColors.border) // Mandatory for Level 3
                ) {
                    Text(
                        text = "${formatAuto(progress.spent)} / ${formatAuto(progress.limit)}",
                        style = AppTextStyles.transactionSwipeAction.copy(
                            fontWeight = FontWeight.Bold,
                            fontFeatureSettings = "tnum",
                            fontSize = 12.sp
                        ),
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        color = AppColors.contentSecondary // Paired with SurfaceVariant
                    )
                }
            }
        }
    }
}

@androidx.compose.ui.tooling.preview.Preview(showBackground = true)
@Composable
fun BudgetsCardPreview() {
    val budget1 = com.osscabinet.ossWallet.domain.model.Budget(
        id = 1L,
        name = "Food",
        currency = "USD",
        limitAmount = java.math.BigDecimal("1000.00"),
        timelineRange = com.osscabinet.ossWallet.domain.model.timeline.TimelineRange.ThisPeriod(com.osscabinet.ossWallet.domain.model.timeline.ThisPeriodType.MONTH),
        profileId = 1
    )
    val budget2 = com.osscabinet.ossWallet.domain.model.Budget(
        id = 2L,
        name = "Rent",
        currency = "USD",
        limitAmount = java.math.BigDecimal("1200.00"),
        timelineRange = com.osscabinet.ossWallet.domain.model.timeline.TimelineRange.ThisPeriod(com.osscabinet.ossWallet.domain.model.timeline.ThisPeriodType.MONTH),
        profileId = 1
    )

    val budgets = listOf(
        BudgetProgress(
            budget = budget1,
            spent = java.math.BigDecimal("450.00"),
            limit = java.math.BigDecimal("1000.00"),
            remaining = java.math.BigDecimal("550.00"),
            percentage = 0.45f,
            alertLevel = BudgetAlertLevel.NONE
        ),
        BudgetProgress(
            budget = budget2,
            spent = java.math.BigDecimal("1250.00"),
            limit = java.math.BigDecimal("1200.00"),
            remaining = java.math.BigDecimal("-50.00"),
            percentage = 1.04f,
            alertLevel = BudgetAlertLevel.EXCEEDED
        )
    )

    com.osscabinet.ossWallet.ui.theme.OssWalletTheme {
        Box(modifier = Modifier.padding(16.dp)) {
            BudgetsCard(
                items = budgets,
                config = BudgetsCardConfig(cardId = "preview", title = "Budgets"),
                callbacks = CardCallbacks.Budgets(onUpdate = {}, onBudgetClick = {}, onDelete = {})
            )
        }
    }
}
