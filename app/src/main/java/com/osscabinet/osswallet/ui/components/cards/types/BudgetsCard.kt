package com.osscabinet.ossWallet.ui.components.cards.types

import com.osscabinet.ossWallet.ui.components.AeonStatusChip
import androidx.compose.ui.text.style.TextOverflow
import com.osscabinet.ossWallet.ui.theme.WarningColor
import com.osscabinet.ossWallet.ui.components.cards.CardSettingsSegmentedRow
import com.osscabinet.ossWallet.domain.service.management.BudgetService
import com.osscabinet.ossWallet.domain.model.settings.BudgetSortMode
import com.osscabinet.ossWallet.domain.model.BudgetPaceStatus
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.layout.width
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
                        showPaceMarker = config.showPaceMarker,
                        showWarningStyle = config.showWarningStyle,
                        previousProgress = if (config.showComparison) previousProgress else null,
                        showDivider = index < items.lastIndex,
                        onClick = onClick
                    )
                }
                if (config.showPaceMarker && items.any { it.elapsedFraction != null }) {
                    Row(
                        modifier = Modifier.padding(top = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .width(2.dp)
                                .height(10.dp)
                                .background(AppColors.contentPrimary.copy(alpha = 0.7f))
                        )
                        Text(
                            text = "WHERE YOU SHOULD BE BY TODAY",
                            style = AppTextStyles.metaLabel.copy(fontSize = 9.sp),
                            color = AppColors.contentSecondary.copy(alpha = 0.7f)
                        )
                    }
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

    CardSettingsSection("Pace") {
        AeonSettingsToggle(
            label = "Pace Marker",
            description = "Mark how far through the period you are on each bar",
            checked = config.showPaceMarker,
            onCheckedChange = { currentOnUpdate.value(config.copy(showPaceMarker = it)) },
            showBackground = false
        )
        AeonSettingsToggle(
            label = "Warning Colour",
            description = "Amber once a budget reaches its alert threshold",
            checked = config.showWarningStyle,
            onCheckedChange = { currentOnUpdate.value(config.copy(showWarningStyle = it)) },
            showBackground = false
        )
    }
    CardSettingsSection("Order") {
        CardSettingsSegmentedRow(
            options = BudgetSortMode.entries,
            selected = config.sortBy,
            onSelected = { currentOnUpdate.value(config.copy(sortBy = it)) },
            labelProvider = { it.displayName }
        )
    }
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
    showPaceMarker: Boolean,
    showWarningStyle: Boolean,
    previousProgress: BudgetProgress? = null,
    showDivider: Boolean,
    onClick: () -> Unit
) {
    val pct = progress.percentage.coerceIn(0f, 1f)
    val status = remember(progress, showPaceMarker, showWarningStyle) {
        // Settings can switch off the pace and warning signals, falling back to what the card showed before.
        when (val raw = BudgetService.paceStatus(progress)) {
            BudgetPaceStatus.AHEAD_OF_PACE -> when {
                showPaceMarker -> raw
                progress.alertLevel == BudgetAlertLevel.WARNING && showWarningStyle -> BudgetPaceStatus.NEAR_LIMIT
                else -> BudgetPaceStatus.ON_TRACK
            }
            BudgetPaceStatus.NEAR_LIMIT -> if (showWarningStyle) raw else BudgetPaceStatus.ON_TRACK
            else -> raw
        }
    }
    val fillColor = when {
        status == BudgetPaceStatus.OVER -> AppColors.negative
        showWarningStyle && (status == BudgetPaceStatus.AHEAD_OF_PACE || status == BudgetPaceStatus.NEAR_LIMIT) -> WarningColor
        else -> AppColors.accent
    }
    val elapsed = progress.elapsedFraction
    val tickColor = AppColors.contentPrimary.copy(alpha = 0.7f)
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
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = progress.budget.name,
                        style = AppTextStyles.listItemTitle.copy(fontWeight = FontWeight.Medium),
                        color = AppColors.contentPrimary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false)
                    )
                    val chip: Pair<String, Color>? = when (status) {
                        BudgetPaceStatus.OVER -> "OVER" to AppColors.negative
                        BudgetPaceStatus.AHEAD_OF_PACE -> "AHEAD OF PACE" to WarningColor
                        BudgetPaceStatus.NEAR_LIMIT -> "NEAR LIMIT" to WarningColor
                        BudgetPaceStatus.ON_TRACK -> if (showPaceMarker && elapsed != null) "ON TRACK" to AppColors.positive else null
                    }
                    if (chip != null) AeonStatusChip(text = chip.first, color = chip.second)
                }
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
                .drawWithContent {
                    drawContent()
                    if (showPaceMarker && elapsed != null) {
                        val x = size.width * elapsed
                        drawLine(
                            color = tickColor,
                            start = Offset(x, -4.dp.toPx()),
                            end = Offset(x, size.height + 4.dp.toPx()),
                            strokeWidth = 2.dp.toPx()
                        )
                    }
                }
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
