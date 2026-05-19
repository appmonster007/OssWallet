package com.osscabinet.ossWallet.ui.components.cards

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyItemScope
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ErrorOutline
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.osscabinet.ossWallet.domain.card.*
import com.osscabinet.ossWallet.domain.model.*
import com.osscabinet.ossWallet.domain.model.settings.CardConfig
import com.osscabinet.ossWallet.ui.components.CardReorderableList
import com.osscabinet.ossWallet.domain.model.TransactionFilter
import com.osscabinet.ossWallet.ui.theme.AeonTheme
import com.osscabinet.ossWallet.ui.theme.AppColors
import com.osscabinet.ossWallet.ui.theme.AppTextStyles

/**
 * Robust, unified card list component for all analytical screens.
 * Standardizes padding, spacing, reordering, and rendering logic.
 */
@Composable
fun AeonCardList(
    cards: List<CardConfig>,
    cardInput: CardInput,
    cardComputerRegistry: CardComputerRegistry,
    cardUpdateHandler: CardUpdateHandler,
    onReorder: (List<CardConfig>) -> Unit,
    onDelete: (String) -> Unit,
    onRecordsClick: (TransactionFilter) -> Unit,
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues(AeonTheme.dimensions.screenPadding),
    verticalArrangement: Arrangement.Vertical = Arrangement.spacedBy(AeonTheme.dimensions.spacingMd),
    animateCards: Boolean = true,
    currentTheme: String = "light",
    filterContext: FilterSelectionContext = FilterSelectionContext(),
    callbacks: CardListCallbacks = CardListCallbacks(),
    baseFilter: TransactionFilter = TransactionFilter(),
    forceDisableAccountSelection: Boolean = false,
    disableCardFilterSheet: Boolean = false,
    header: @Composable (LazyItemScope.() -> Unit)? = null,
    cardDataMap: Map<String, CardRenderData> = emptyMap()
) {
    CardReorderableList(
        items = cards.sortedBy { it.order },
        key = { it.cardId },
        onReorder = onReorder,
        modifier = modifier,
        contentPadding = contentPadding,
        verticalArrangement = verticalArrangement,
        animateCards = animateCards,
        header = header
    ) { cardConfig, isDragging, dragModifier ->

        val preComputedData = cardDataMap[cardConfig.cardId]

        // cardInput.filter is the screen's base scope. Card computers apply config.filter on top
        // of that in their own two-step logic — do NOT overwrite input.filter here.
        val cardWithCallbacks = rememberCardWithCallbacks(
            config = cardConfig,
            input = cardInput,
            registry = cardComputerRegistry,
            updateHandler = cardUpdateHandler,
            onDelete = onDelete,
            onRecordsClick = onRecordsClick,
            callbacks = callbacks,
            currentTheme = currentTheme,
            preComputedData = preComputedData,
            filterSheetEnabled = !disableCardFilterSheet
        )

        when (val card = cardWithCallbacks) {
            is CardWithCallbacks.ComputeError -> CardErrorState(card.message)
            null -> Box(modifier = Modifier.size(0.dp))
            else -> {
                val effectiveCard = if (forceDisableAccountSelection || disableCardFilterSheet) {
                    card.copyWithRules(
                        rules = card.filterRules.copy(allowAccountSelection = if (forceDisableAccountSelection) false else card.filterRules.allowAccountSelection),
                        baseFilter = baseFilter,
                        filterSheetEnabled = !disableCardFilterSheet
                    )
                } else card

                UnifiedCardRenderer(
                    cardWithCallbacks = effectiveCard,
                    modifier = Modifier.fillMaxWidth(),
                    headerModifier = dragModifier,
                    currentTheme = currentTheme,
                    filterContext = filterContext,
                )
            }
        }
    }
}

/**
 * Helper extension to clone CardWithCallbacks with updated rules or base filters.
 */
private fun CardWithCallbacks.copyWithRules(
    rules: CardFilterRules,
    baseFilter: TransactionFilter,
    filterSheetEnabled: Boolean = true
): CardWithCallbacks = when (this) {
    is CardWithCallbacks.NetWorth -> copy(filterRules = rules, baseFilter = baseFilter, filterSheetEnabled = filterSheetEnabled)
    is CardWithCallbacks.CurrencyBalances -> copy(filterRules = rules, baseFilter = baseFilter, filterSheetEnabled = filterSheetEnabled)
    is CardWithCallbacks.CashFlow -> copy(filterRules = rules, baseFilter = baseFilter, filterSheetEnabled = filterSheetEnabled)
    is CardWithCallbacks.Accounts -> copy(filterRules = rules, baseFilter = baseFilter, filterSheetEnabled = filterSheetEnabled)
    is CardWithCallbacks.CategoryPieChart -> copy(filterRules = rules, baseFilter = baseFilter, filterSheetEnabled = filterSheetEnabled)
    is CardWithCallbacks.CategoryTreemap -> copy(filterRules = rules, baseFilter = baseFilter, filterSheetEnabled = filterSheetEnabled)
    is CardWithCallbacks.AccountHero -> copy(filterRules = rules, baseFilter = baseFilter, filterSheetEnabled = filterSheetEnabled)
    is CardWithCallbacks.PlannedPayments -> copy(filterRules = rules, baseFilter = baseFilter, filterSheetEnabled = filterSheetEnabled)
    is CardWithCallbacks.Budgets -> copy(filterRules = rules, baseFilter = baseFilter, filterSheetEnabled = filterSheetEnabled)
    is CardWithCallbacks.Loans -> copy(filterRules = rules, baseFilter = baseFilter, filterSheetEnabled = filterSheetEnabled)
    is CardWithCallbacks.RecentActivities -> copy(filterRules = rules, baseFilter = baseFilter, filterSheetEnabled = filterSheetEnabled)
    is CardWithCallbacks.CashFlowComparison -> copy(filterRules = rules, baseFilter = baseFilter, filterSheetEnabled = filterSheetEnabled)
    is CardWithCallbacks.IncomeExpenseBar -> copy(filterRules = rules, baseFilter = baseFilter, filterSheetEnabled = filterSheetEnabled)
    is CardWithCallbacks.CategorySpendingTrend -> copy(filterRules = rules, baseFilter = baseFilter, filterSheetEnabled = filterSheetEnabled)
    is CardWithCallbacks.ComputeError -> this
}

@Composable
private fun CardErrorState(message: String?) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(AeonTheme.dimensions.componentCornerRadius),
        colors = CardDefaults.cardColors(
            containerColor = AppColors.errorContainer.copy(alpha = 0.2f)
        ),
        border = androidx.compose.foundation.BorderStroke(0.5.dp, AppColors.error.copy(alpha = 0.2f)),
        elevation = CardDefaults.cardElevation(0.dp)
    ) {
        Row(
            modifier = Modifier.padding(AeonTheme.dimensions.spacingMd),
            horizontalArrangement = Arrangement.spacedBy(AeonTheme.dimensions.spacingSm),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Rounded.ErrorOutline,
                contentDescription = null,
                tint = AppColors.onErrorContainer,
                modifier = Modifier.size(20.dp)
            )
            Text(
                text = message ?: "Failed to load card",
                style = AppTextStyles.transactionTitle,
                color = AppColors.onErrorContainer
            )
        }
    }
}
