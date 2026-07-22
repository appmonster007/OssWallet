package com.osscabinet.ossWallet.ui.components.cards.types

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
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.osscabinet.ossWallet.domain.card.CardInput
import com.osscabinet.ossWallet.domain.card.CardRenderData
import com.osscabinet.ossWallet.domain.card.CardUpdateHandler
import com.osscabinet.ossWallet.domain.model.BudgetAlertLevel
import com.osscabinet.ossWallet.domain.model.BudgetProgress
import com.osscabinet.ossWallet.domain.model.TransactionFilter
import com.osscabinet.ossWallet.domain.model.settings.BudgetsCardConfig
import com.osscabinet.ossWallet.domain.model.timeline.TimelineRange
import com.osscabinet.ossWallet.ui.components.AeonSettingsToggle
import com.osscabinet.ossWallet.ui.components.aeonDottedUnderline
import com.osscabinet.ossWallet.ui.components.cards.AeonExpressiveCard
import com.osscabinet.ossWallet.ui.components.cards.AeonInteractionCallbacks
import com.osscabinet.ossWallet.ui.components.cards.CardCallbacks
import com.osscabinet.ossWallet.ui.components.cards.CardLayoutProps
import com.osscabinet.ossWallet.ui.components.cards.CardSettingsSection
import com.osscabinet.ossWallet.ui.components.cards.CardWithCallbacks
import com.osscabinet.ossWallet.ui.components.cards.resolveDrillDown
import com.osscabinet.ossWallet.ui.components.cards.resolveSeeAll
import com.osscabinet.ossWallet.ui.theme.AppColors
import com.osscabinet.ossWallet.ui.theme.AppTextStyles
import com.osscabinet.ossWallet.ui.theme.OssWalletTheme
import com.osscabinet.ossWallet.ui.util.CurrencyDisplaySpec
import com.osscabinet.ossWallet.ui.util.CurrencyReprUi

@Composable
fun BudgetsCard(
    items: List<BudgetProgress>,
    config: BudgetsCardConfig,
    previousItems: List<BudgetProgress>? = null,
    callbacks: CardCallbacks.Budgets = CardCallbacks.Budgets(),
    layoutProps: CardLayoutProps = CardLayoutProps(),
) {
    val displayFilter = layoutProps.computeDisplayFilter(config.filter)
    val currentOnFilterChange = rememberUpdatedState(callbacks.onFilterChange)

    AeonExpressiveCard(
        title = config.title,
        icon = Icons.Rounded.DonutLarge,
        modifier = layoutProps.modifier,
        headerModifier = layoutProps.headerModifier,
        onClick = callbacks.onSeeAll,
        filterEnabled = layoutProps.showFilterButton,
        filter = displayFilter,
        baseFilter = layoutProps.baseFilter,
        filterRules = layoutProps.filterRules ?: config.filterRules,
        filterContext = layoutProps.filterContext,
        onFilterChange = { currentOnFilterChange.value.invoke(config.cardId, it) },
        onSaveFilterClick = callbacks.onSaveFilterClick,
        onDelete = callbacks.onDelete,
        settingsContent = {
            BudgetsSettingsContent(config = config, callbacks = callbacks)
        },
    ) {
        if (items.isEmpty()) {
            Text(
                text = "No budgets yet",
                style = AppTextStyles.caption,
                color = AppColors.contentSecondary,
                modifier = Modifier.padding(vertical = 8.dp)
            )
        } else {
            val onBudgetClick = callbacks.onBudgetClick
            Column(modifier = Modifier.fillMaxWidth()) {
                items.forEachIndexed { index, progress ->
                    val budgetId = progress.budget.id
                    val onClick = remember(budgetId, onBudgetClick) { { onBudgetClick(budgetId) } }
                    val previousProgress = previousItems?.find { it.budget.id == budgetId }
                    BudgetProgressRow(
                        progress = progress,
                        previousProgress = if (config.showComparison) previousProgress else null,
                        showDivider = index < items.lastIndex,
                        onClick = onClick
                    )
                }
            }
        }
    }
}

@Composable
private fun BudgetsSettingsContent(
    config: BudgetsCardConfig,
    callbacks: CardCallbacks.Budgets,
) {
    val currentOnUpdate = rememberUpdatedState(callbacks.onUpdate)
    val currentOnShowComparisonChange = rememberUpdatedState(callbacks.onShowComparisonChange)

    CardSettingsSection("Display") {
        AeonSettingsToggle(
            label = "Show archived budgets",
            checked = config.showArchived,
            onCheckedChange = { currentOnUpdate.value(config.copy(showArchived = it)) },
            showBackground = false
        )
        AeonSettingsToggle(
            label = "Show Comparison",
            description = "Show change from previous period",
            checked = config.showComparison,
            onCheckedChange = { currentOnShowComparisonChange.value(config.cardId, it) },
            showBackground = false
        )
    }
}

@Composable
private fun BudgetProgressRow(
    progress: BudgetProgress,
    previousProgress: BudgetProgress? = null,
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
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = progress.budget.name,
                    style = AppTextStyles.listItemTitle.copy(fontWeight = FontWeight.Medium),
                    color = AppColors.contentPrimary
                )
                if (previousProgress != null) {
                    val change = progress.spent.subtract(previousProgress.spent)
                    if ((change.signum() != 0) && (previousProgress.spent.signum() != 0)) {
                        val percentage = change.multiply(java.math.BigDecimal("100"))
                            .divide(previousProgress.spent.abs(), 1, java.math.RoundingMode.HALF_UP)
                        val up = change.signum() > 0
                        Text(
                            text = "${if (up) "▲" else "▼"} ${if (up) "+" else ""}$percentage% VS PRIOR",
                            style = AppTextStyles.metaLabel.copy(
                                fontWeight = FontWeight.Bold,
                                fontSize = 9.sp
                            ),
                            color = if (up) AppColors.negative else AppColors.positive
                        )
                    }
                }
            }
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

/**
 * Maps [BudgetsCardConfig] + [CardRenderData.Budgets] to [CardWithCallbacks.Budgets].
 */
fun mapBudgets(
    config: BudgetsCardConfig,
    renderData: CardRenderData.Budgets,
    input: CardInput,
    updateHandler: CardUpdateHandler,
    onDelete: ((String) -> Unit)?,
    onRecordsClick: (TransactionFilter) -> Unit,
    callbacks: AeonInteractionCallbacks,
    filterSheetEnabled: Boolean
) = CardWithCallbacks.Budgets(
    renderData = renderData,
    callbacks = CardCallbacks.Budgets(
        onUpdate = { newConfig -> updateHandler.updateCardBudgetsConfig(config.cardId, newConfig) },
        onShowComparisonChange = updateHandler::updateCardShowComparison,
        onFilterChange = updateHandler::updateCardBudgetsFilter,
        onBudgetClick = callbacks.budgetActions.onClick,
        onRecordsClick = { drillDown ->
            onRecordsClick(
                resolveDrillDown(
                    input,
                    config,
                    drillDown
                )
            )
        },
        onSeeAll = {
            onRecordsClick(
                resolveSeeAll(
                    input,
                    config
                )
            )
        },
        onDelete = onDelete?.let { { it(config.cardId) } },
        onSaveFilterClick = callbacks.filterActions.onSaveClick
    ),
    filterRules = config.filterRules,
    baseFilter = input.filter,
    filterSheetEnabled = filterSheetEnabled
)

@androidx.compose.ui.tooling.preview.Preview(showBackground = true)
@Composable
fun BudgetsCardPreview() {
    val budget1 = com.osscabinet.ossWallet.domain.model.Budget(
        id = 1L,
        name = "Food",
        currency = "USD",
        limitAmount = java.math.BigDecimal("1000.00"),
        timelineRange = TimelineRange.ThisPeriod(com.osscabinet.ossWallet.domain.model.timeline.ThisPeriodType.MONTH),
        profileId = 1
    )
    val budget2 = com.osscabinet.ossWallet.domain.model.Budget(
        id = 2L,
        name = "Rent",
        currency = "USD",
        limitAmount = java.math.BigDecimal("1200.00"),
        timelineRange = TimelineRange.ThisPeriod(com.osscabinet.ossWallet.domain.model.timeline.ThisPeriodType.MONTH),
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

    OssWalletTheme {
        Box(modifier = Modifier.padding(16.dp)) {
            BudgetsCard(
                items = budgets,
                config = BudgetsCardConfig(cardId = "preview", title = "Budgets"),
                callbacks = CardCallbacks.Budgets(onUpdate = {}, onBudgetClick = {}, onDelete = {})
            )
        }
    }
}
