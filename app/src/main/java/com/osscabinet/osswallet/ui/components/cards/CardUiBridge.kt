package com.osscabinet.ossWallet.ui.components.cards

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import com.osscabinet.ossWallet.domain.card.CardComputeState
import com.osscabinet.ossWallet.domain.card.CardInput
import com.osscabinet.ossWallet.domain.card.CardRenderData
import com.osscabinet.ossWallet.domain.card.CardUpdateHandler
import com.osscabinet.ossWallet.domain.model.TransactionFilter
import com.osscabinet.ossWallet.domain.model.settings.AccountHeroCardConfig
import com.osscabinet.ossWallet.domain.model.settings.AccountsCardConfig
import com.osscabinet.ossWallet.domain.model.settings.BudgetsCardConfig
import com.osscabinet.ossWallet.domain.model.settings.CardConfig
import com.osscabinet.ossWallet.domain.model.settings.CashFlowCardConfig
import com.osscabinet.ossWallet.domain.model.settings.CashFlowComparisonCardConfig
import com.osscabinet.ossWallet.domain.model.settings.CategoryPieChartCardConfig
import com.osscabinet.ossWallet.domain.model.settings.CategorySpendingTrendCardConfig
import com.osscabinet.ossWallet.domain.model.settings.CategoryTreemapCardConfig
import com.osscabinet.ossWallet.domain.model.settings.CurrencyBalancesCardConfig
import com.osscabinet.ossWallet.domain.model.settings.IncomeExpenseBarCardConfig
import com.osscabinet.ossWallet.domain.model.settings.LoansCardConfig
import com.osscabinet.ossWallet.domain.model.settings.NetWorthCardConfig
import com.osscabinet.ossWallet.domain.model.settings.PlannedPaymentsCardConfig
import com.osscabinet.ossWallet.domain.model.settings.RecentActivitiesCardConfig
import com.osscabinet.ossWallet.domain.model.settings.TagUpSetPlotCardConfig
import com.osscabinet.ossWallet.ui.components.cards.types.mapAccountHero
import com.osscabinet.ossWallet.ui.components.cards.types.mapAccounts
import com.osscabinet.ossWallet.ui.components.cards.types.mapBudgets
import com.osscabinet.ossWallet.ui.components.cards.types.mapCashFlow
import com.osscabinet.ossWallet.ui.components.cards.types.mapCashFlowComparison
import com.osscabinet.ossWallet.ui.components.cards.types.mapCategoryPieChart
import com.osscabinet.ossWallet.ui.components.cards.types.mapCategorySpendingTrend
import com.osscabinet.ossWallet.ui.components.cards.types.mapCategoryTreemap
import com.osscabinet.ossWallet.ui.components.cards.types.mapCurrencyBalances
import com.osscabinet.ossWallet.ui.components.cards.types.mapIncomeExpenseBar
import com.osscabinet.ossWallet.ui.components.cards.types.mapLoans
import com.osscabinet.ossWallet.ui.components.cards.types.mapNetWorth
import com.osscabinet.ossWallet.ui.components.cards.types.mapPlannedPayments
import com.osscabinet.ossWallet.ui.components.cards.types.mapRecentActivities
import com.osscabinet.ossWallet.ui.components.cards.types.mapTagUpSetPlot

/**
 * Maps a [CardComputeState] + [config] pair to the matching [CardWithCallbacks] subtype.
 * Handles Loading, Error, and Success states.
 */
@Composable
fun rememberCardWithCallbacks(
    config: CardConfig,
    state: CardComputeState?,
    input: CardInput,
    updateHandler: CardUpdateHandler,
    onDelete: ((String) -> Unit)?,
    onRecordsClick: (TransactionFilter) -> Unit,
    callbacks: AeonInteractionCallbacks = AeonInteractionCallbacks(),
    filterSheetEnabled: Boolean = true,
): CardWithCallbacks = remember(
    config,
    state,
    input.filter,
    input.screenOwnsTimeline,
    updateHandler,
    onDelete,
    onRecordsClick,
    callbacks,
    filterSheetEnabled,
) {
    when (state) {
        is CardComputeState.Loading, null -> CardWithCallbacks.Loading(
            cardId = config.cardId,
            cardType = config.cardType
        )

        is CardComputeState.Error -> CardWithCallbacks.ComputeError(
            cardId = config.cardId,
            cardType = config.cardType,
            message = state.message
        )

        is CardComputeState.Success -> {
            val effectiveOnDelete = if (input.deleteEnabled) onDelete else null
            mapSuccessState(
                config,
                state.data,
                input,
                updateHandler,
                effectiveOnDelete,
                onRecordsClick,
                callbacks,
                filterSheetEnabled
            )
        }
    }
}

private fun mapSuccessState(
    config: CardConfig,
    renderData: CardRenderData,
    input: CardInput,
    updateHandler: CardUpdateHandler,
    onDelete: ((String) -> Unit)?,
    onRecordsClick: (TransactionFilter) -> Unit,
    callbacks: AeonInteractionCallbacks,
    filterSheetEnabled: Boolean
): CardWithCallbacks {
    // P4: CardRenderData.Error carries the actual error message — surface it directly.
    if (renderData is CardRenderData.Error) {
        return CardWithCallbacks.ComputeError(config.cardId, config.cardType, renderData.message)
    }
    // CardType mismatch (e.g. during a card-type transition): treat as transient error.
    if (config.cardType != renderData.cardType) {
        return typeMismatch(config, renderData)
    }

    // P3: Exhaustive match — no else branch; compiler enforces all sealed subtypes are handled.
    // CardRenderData.Error is listed last as unreachable (always caught by the early guard above).
    return when (renderData) {
        is CardRenderData.NetWorth ->
            mapNetWorth(
                config as NetWorthCardConfig,
                renderData,
                input,
                updateHandler,
                onDelete,
                onRecordsClick,
                callbacks,
                filterSheetEnabled
            )

        is CardRenderData.CurrencyBalances ->
            mapCurrencyBalances(
                config as CurrencyBalancesCardConfig,
                renderData,
                input,
                updateHandler,
                onDelete,
                onRecordsClick,
                callbacks,
                filterSheetEnabled
            )

        is CardRenderData.CashFlow ->
            mapCashFlow(
                config as CashFlowCardConfig,
                renderData,
                input,
                updateHandler,
                onDelete,
                onRecordsClick,
                callbacks,
                filterSheetEnabled
            )

        is CardRenderData.Accounts ->
            mapAccounts(
                config as AccountsCardConfig,
                renderData,
                input,
                updateHandler,
                onDelete,
                onRecordsClick,
                callbacks,
                filterSheetEnabled
            )

        is CardRenderData.CategoryPieChart ->
            mapCategoryPieChart(
                config as CategoryPieChartCardConfig,
                renderData,
                input,
                updateHandler,
                onDelete,
                onRecordsClick,
                callbacks,
                filterSheetEnabled
            )

        is CardRenderData.CategoryTreemap ->
            mapCategoryTreemap(
                config as CategoryTreemapCardConfig,
                renderData,
                input,
                updateHandler,
                onDelete,
                onRecordsClick,
                callbacks,
                filterSheetEnabled
            )

        is CardRenderData.AccountHero ->
            mapAccountHero(
                config as AccountHeroCardConfig,
                renderData,
                input,
                updateHandler,
                onRecordsClick,
                callbacks,
                filterSheetEnabled
            )

        is CardRenderData.PlannedPayments ->
            mapPlannedPayments(
                config as PlannedPaymentsCardConfig,
                renderData,
                input,
                updateHandler,
                onDelete,
                callbacks,
                filterSheetEnabled
            )

        is CardRenderData.Budgets ->
            mapBudgets(
                config as BudgetsCardConfig,
                renderData,
                input,
                updateHandler,
                onDelete,
                callbacks,
                filterSheetEnabled
            )

        is CardRenderData.Loans ->
            mapLoans(
                config as LoansCardConfig,
                renderData,
                input,
                updateHandler,
                onDelete,
                callbacks,
                filterSheetEnabled
            )

        is CardRenderData.RecentActivities ->
            mapRecentActivities(
                config as RecentActivitiesCardConfig,
                renderData,
                input,
                updateHandler,
                onDelete,
                onRecordsClick,
                callbacks,
                filterSheetEnabled
            )

        is CardRenderData.CashFlowComparison ->
            mapCashFlowComparison(
                config as CashFlowComparisonCardConfig,
                renderData,
                input,
                updateHandler,
                onDelete,
                onRecordsClick,
                callbacks,
                filterSheetEnabled
            )

        is CardRenderData.IncomeExpenseBar ->
            mapIncomeExpenseBar(
                config as IncomeExpenseBarCardConfig,
                renderData,
                input,
                updateHandler,
                onDelete,
                onRecordsClick,
                callbacks,
                filterSheetEnabled
            )

        is CardRenderData.CategorySpendingTrend ->
            mapCategorySpendingTrend(
                config as CategorySpendingTrendCardConfig,
                renderData,
                input,
                updateHandler,
                onDelete,
                onRecordsClick,
                callbacks,
                filterSheetEnabled
            )

        is CardRenderData.TagUpSetPlot ->
            mapTagUpSetPlot(
                config as TagUpSetPlotCardConfig,
                renderData,
                input,
                updateHandler,
                onDelete,
                onRecordsClick,
                callbacks,
                filterSheetEnabled
            )

        is CardRenderData.Error -> typeMismatch(config, renderData)
    }
}

private fun typeMismatch(config: CardConfig, renderData: CardRenderData) =
    CardWithCallbacks.ComputeError(
        cardId = config.cardId,
        cardType = config.cardType,
        message = "Type mismatch: ${config.cardType} vs ${renderData.cardType ?: "Error"}"
    )
