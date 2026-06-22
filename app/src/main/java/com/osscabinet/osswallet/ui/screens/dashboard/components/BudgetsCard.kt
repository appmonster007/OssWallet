package com.osscabinet.ossWallet.ui.screens.dashboard.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.DonutLarge
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import com.osscabinet.ossWallet.domain.card.CardFilterRules
import com.osscabinet.ossWallet.domain.model.BudgetAlertLevel
import com.osscabinet.ossWallet.domain.model.BudgetProgress
import com.osscabinet.ossWallet.domain.model.TransactionFilter
import com.osscabinet.ossWallet.domain.model.timeline.TimelineRange
import com.osscabinet.ossWallet.domain.model.settings.BudgetsCardConfig
import com.osscabinet.ossWallet.ui.util.CurrencyDisplaySpec
import com.osscabinet.ossWallet.ui.util.CurrencyReprUi
import com.osscabinet.ossWallet.ui.components.AeonSettingsToggle
import com.osscabinet.ossWallet.ui.components.aeonDottedUnderline
import com.osscabinet.ossWallet.ui.components.cards.AeonExpressiveCard
import com.osscabinet.ossWallet.ui.components.cards.CardCallbacks
import com.osscabinet.ossWallet.ui.components.cards.CardSettingsSection
import com.osscabinet.ossWallet.ui.components.cards.CardSettingsSheet
import com.osscabinet.ossWallet.ui.components.cards.FilterSelectionContext
import com.osscabinet.ossWallet.ui.theme.AppColors
import com.osscabinet.ossWallet.ui.theme.AppTextStyles

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
            val onBudgetClick = callbacks::onBudgetClick
            Column(modifier = Modifier.fillMaxWidth()) {
                items.forEachIndexed { index, progress ->
                    val budgetId = progress.budget.id
                    val onClick = remember(budgetId) { { onBudgetClick(budgetId) } }
                    BudgetProgressRow(
                        progress = progress,
                        showDivider = index < items.lastIndex,
                        onClick = onClick
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
            AeonSettingsToggle(
                label = "Show archived budgets",
                checked = config.showArchived,
                onCheckedChange = { callbacks.onUpdate(config.copy(showArchived = it)) },
                showBackground = false
            )
        }
    }
}

@Composable
private fun BudgetProgressRow(
    progress: BudgetProgress,
    showDivider: Boolean,
    onClick: () -> Unit
) {
    val pct = progress.percentage.coerceIn(0f, 1f)
    val over = progress.alertLevel == BudgetAlertLevel.EXCEEDED
    val fillColor = if (over) AppColors.negative else AppColors.accent
    val currency = progress.budget.currency

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .then(
                if (showDivider) Modifier.aeonDottedUnderline(AppColors.outlineVariant)
                else Modifier
            )
            .padding(vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(9.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Bottom
        ) {
            Text(
                text = progress.budget.name,
                style = AppTextStyles.listItemTitle.copy(fontWeight = FontWeight.Medium),
                color = AppColors.contentPrimary
            )
            Text(
                text = buildAnnotatedString {
                    append(
                        CurrencyReprUi.formatCurrencyAnnotated(
                            amount = progress.spent,
                            currencyCode = currency,
                            spec = CurrencyDisplaySpec(useSemanticColor = false)
                        )
                    )
                    withStyle(SpanStyle(color = AppColors.contentSecondary.copy(alpha = 0.8f))) {
                        append(" / ")
                        append(
                            CurrencyReprUi.formatCurrencyAnnotated(
                                amount = progress.limit,
                                currencyCode = currency,
                                spec = CurrencyDisplaySpec(useSemanticColor = false)
                            )
                        )
                        append(" · ${(progress.percentage * 100).toInt()}%")
                    }
                },
                style = AppTextStyles.metaLabel.copy(fontWeight = FontWeight.Medium)
            )
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(2.dp)
                .background(AppColors.outlineVariant)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(pct)
                    .fillMaxHeight()
                    .background(fillColor)
            )
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
