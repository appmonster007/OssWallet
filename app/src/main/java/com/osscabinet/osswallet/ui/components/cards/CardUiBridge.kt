package com.osscabinet.ossWallet.ui.components.cards

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.osscabinet.ossWallet.domain.card.CardComputerRegistry
import com.osscabinet.ossWallet.domain.card.CardInput
import com.osscabinet.ossWallet.domain.card.CardRenderData
import com.osscabinet.ossWallet.domain.card.CardUpdateHandler
import com.osscabinet.ossWallet.domain.card.resolveEffectiveFilter
import com.osscabinet.ossWallet.domain.model.TransactionFilter
import com.osscabinet.ossWallet.domain.model.merge
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

/**
 * Bridge that maps CardConfig to CardWithCallbacks.
 * This decouples screens from specific card implementations.
 */
@Composable
fun rememberCardWithCallbacks(
    config: CardConfig,
    input: CardInput,
    registry: CardComputerRegistry,
    updateHandler: CardUpdateHandler,
    onDelete: (String) -> Unit,
    onRecordsClick: (TransactionFilter) -> Unit,
    callbacks: AeonInteractionCallbacks = AeonInteractionCallbacks(),
    currentTheme: String = "light",
    preComputedData: CardRenderData? = null,
    filterSheetEnabled: Boolean = true
): CardWithCallbacks? {
    // ⚠️ STABILITY: We key by cardId rather than the entire config/input object.
    // This ensures that the card's ephemeral UI state (like "pill is open") is preserved
    // while the data for the new filter/config is being computed in the background.
    var renderDataState by remember(config.cardId) {
        mutableStateOf(preComputedData)
    }

    // Sync path: Dashboard provides preComputedData.
    if (preComputedData != null) {
        LaunchedEffect(preComputedData) {
            renderDataState = preComputedData
        }
    } else {
        // Async path: Analytics/Detail screens compute data on the fly.
        // We keep the old renderDataState (stale-while-recompute) to prevent the card
        // from unmounting and losing its "pill open" state.
        LaunchedEffect(config, input) {
            try {
                val newData = registry.compute(config.cardType, input, config)
                renderDataState = newData
            } catch (e: Exception) {
                android.util.Log.e("CardUiBridge", "Card compute failed [${config.cardId}]", e)
            }
        }
    }

    val renderData = renderDataState ?: return null

    return remember(
        config,
        input,
        registry,
        updateHandler,
        onDelete,
        onRecordsClick,
        callbacks,
        currentTheme,
        renderData,
        filterSheetEnabled
    ) {
        when {
            config is NetWorthCardConfig && renderData is CardRenderData.NetWorth -> {
                CardWithCallbacks.NetWorth(
                    renderData = renderData,
                    callbacks = CardCallbacks.NetWorth(
                        onFontSizeChange = { id, size ->
                            updateHandler.updateCardNetWorthFontSize(
                                id,
                                size
                            )
                        },
                        onShowComparisonChange = { id, show ->
                            updateHandler.updateCardNetWorthShowComparison(
                                id,
                                show
                            )
                        },
                        onFilterChange = { id, filter ->
                            updateHandler.updateCardNetWorthFilter(
                                id,
                                filter
                            )
                        },
                        onClickIncome = { drillDown ->
                            onRecordsClick(
                                resolveEffectiveFilter(input.filter, config.filter, config.filterRules, input.screenOwnsTimeline).merge(drillDown)
                            )
                        },
                        onClickExpense = { drillDown ->
                            onRecordsClick(
                                resolveEffectiveFilter(input.filter, config.filter, config.filterRules, input.screenOwnsTimeline).merge(drillDown)
                            )
                        },
                        onSeeAll = {
                            onRecordsClick(resolveEffectiveFilter(input.filter, config.filter, config.filterRules, input.screenOwnsTimeline))
                        },
                        onDelete = { onDelete(config.cardId) },
                        onSaveFilterClick = callbacks.filterActions.onSaveClick
                    ),
                    filterRules = config.filterRules,
                    baseFilter = input.filter,
                    filterSheetEnabled = filterSheetEnabled
                )
            }

            config is CurrencyBalancesCardConfig && renderData is CardRenderData.CurrencyBalances -> {
                CardWithCallbacks.CurrencyBalances(
                    renderData = renderData,
                    callbacks = CardCallbacks.CurrencyBalances(
                        onLayoutChange = { id, layout ->
                            updateHandler.updateCardCurrencyBalancesLayout(
                                id,
                                layout
                            )
                        },
                        onShowBaseConversionChange = { id, show ->
                            updateHandler.updateCardCurrencyBalancesShowBaseConversion(
                                id,
                                show
                            )
                        },
                        onFilterChange = { id, filter ->
                            updateHandler.updateCardCurrencyBalancesFilter(
                                id,
                                filter
                            )
                        },
                        onCurrencyClick = { currency, range ->
                            val drillDown = TransactionFilter(
                                currencies = setOf(currency),
                                timelineRange = range
                            )
                            onRecordsClick(
                                resolveEffectiveFilter(input.filter, config.filter, config.filterRules, input.screenOwnsTimeline).merge(drillDown)
                            )
                        },
                        onSeeAll = {
                            onRecordsClick(resolveEffectiveFilter(input.filter, config.filter, config.filterRules, input.screenOwnsTimeline))
                        },
                        onDelete = { onDelete(config.cardId) },
                        onSaveFilterClick = callbacks.filterActions.onSaveClick
                    ),
                    filterRules = config.filterRules,
                    baseFilter = input.filter,
                    filterSheetEnabled = filterSheetEnabled
                )
            }

            config is CashFlowCardConfig && renderData is CardRenderData.CashFlow -> {
                CardWithCallbacks.CashFlow(
                    renderData = renderData,
                    callbacks = CardCallbacks.CashFlow(
                        onShowBaseConversionChange = { id, show ->
                            updateHandler.updateCardCashFlowShowBaseConversion(
                                id,
                                show
                            )
                        },
                        onFilterChange = { id, filter ->
                            updateHandler.updateCardCashFlowFilter(
                                id,
                                filter
                            )
                        },
                        onClickIncome = { drillDown ->
                            onRecordsClick(
                                resolveEffectiveFilter(input.filter, config.filter, config.filterRules, input.screenOwnsTimeline).merge(drillDown)
                            )
                        },
                        onClickExpense = { drillDown ->
                            onRecordsClick(
                                resolveEffectiveFilter(input.filter, config.filter, config.filterRules, input.screenOwnsTimeline).merge(drillDown)
                            )
                        },
                        onSeeAll = {
                            onRecordsClick(resolveEffectiveFilter(input.filter, config.filter, config.filterRules, input.screenOwnsTimeline))
                        },
                        onDelete = { onDelete(config.cardId) },
                        onSaveFilterClick = callbacks.filterActions.onSaveClick
                    ),
                    filterRules = config.filterRules,
                    baseFilter = input.filter,
                    filterSheetEnabled = filterSheetEnabled
                )
            }

            config is AccountsCardConfig && renderData is CardRenderData.Accounts -> {
                CardWithCallbacks.Accounts(
                    renderData = renderData,
                    callbacks = CardCallbacks.Accounts(
                        onGridColumnsChange = { id, cols ->
                            updateHandler.updateCardAccountsGridColumns(
                                id,
                                cols
                            )
                        },
                        onLayoutChange = { id, layout ->
                            updateHandler.updateCardAccountsLayout(
                                id,
                                layout
                            )
                        },
                        onFilterChange = { id, filter ->
                            updateHandler.updateCardAccountsFilter(
                                id,
                                filter
                            )
                        },
                        onShowAddAccountCardChange = { id, show ->
                            updateHandler.updateCardAccountsShowAddAccountCard(
                                id,
                                show
                            )
                        },
                        onManageClick = callbacks.accountActions.onManageClick,
                        onAccountClick = callbacks.accountActions.onClick,
                        onRecordsClick = { drillDown ->
                            onRecordsClick(
                                resolveEffectiveFilter(input.filter, config.filter, config.filterRules, input.screenOwnsTimeline).merge(drillDown)
                            )
                        },
                        onSeeAll = {
                            onRecordsClick(resolveEffectiveFilter(input.filter, config.filter, config.filterRules, input.screenOwnsTimeline))
                        },
                        onDelete = { onDelete(config.cardId) },
                        onSaveFilterClick = callbacks.filterActions.onSaveClick
                    ),
                    filterRules = config.filterRules,
                    baseFilter = input.filter,
                    filterSheetEnabled = filterSheetEnabled
                )
            }

            config is CategoryPieChartCardConfig && renderData is CardRenderData.CategoryPieChart -> {
                CardWithCallbacks.CategoryPieChart(
                    renderData = renderData,
                    callbacks = CardCallbacks.Analytics(
                        onUpdate = { newConfig ->
                            if (newConfig is CategoryPieChartCardConfig) {
                                updateHandler.updateCardCategoryPieChartConfig(
                                    config.cardId,
                                    newConfig
                                )
                            }
                        },
                        onDelete = { onDelete(config.cardId) },
                        onRecordsClick = { drillDown ->
                            onRecordsClick(
                                resolveEffectiveFilter(input.filter, config.filter, config.filterRules, input.screenOwnsTimeline).merge(drillDown)
                            )
                        },
                        onSaveFilterClick = callbacks.filterActions.onSaveClick
                    ),
                    filterRules = config.filterRules,
                    baseFilter = input.filter,
                    filterSheetEnabled = filterSheetEnabled
                )
            }

            config is CategoryTreemapCardConfig && renderData is CardRenderData.CategoryTreemap -> {
                CardWithCallbacks.CategoryTreemap(
                    renderData = renderData,
                    callbacks = CardCallbacks.Analytics(
                        onUpdate = { newConfig ->
                            if (newConfig is CategoryTreemapCardConfig) {
                                updateHandler.updateCardCategoryTreemapConfig(
                                    config.cardId,
                                    newConfig
                                )
                            }
                        },
                        onDelete = { onDelete(config.cardId) },
                        onRecordsClick = { drillDown ->
                            onRecordsClick(
                                resolveEffectiveFilter(input.filter, config.filter, config.filterRules, input.screenOwnsTimeline).merge(drillDown)
                            )
                        },
                        onSaveFilterClick = callbacks.filterActions.onSaveClick
                    ),
                    filterRules = config.filterRules,
                    baseFilter = input.filter,
                    filterSheetEnabled = filterSheetEnabled
                )
            }

            config is AccountHeroCardConfig && renderData is CardRenderData.AccountHero -> {
                CardWithCallbacks.AccountHero(
                    renderData = renderData,
                    callbacks = CardCallbacks.AccountHero(
                        onClickIncome = { drillDown ->
                            onRecordsClick(
                                resolveEffectiveFilter(input.filter, config.filter, config.filterRules, input.screenOwnsTimeline).merge(drillDown)
                            )
                        },
                        onClickExpense = { drillDown ->
                            onRecordsClick(
                                resolveEffectiveFilter(input.filter, config.filter, config.filterRules, input.screenOwnsTimeline).merge(drillDown)
                            )
                        },
                        onClickCurrency = { currency ->
                            val drillDown = TransactionFilter(currencies = setOf(currency))
                            onRecordsClick(
                                resolveEffectiveFilter(input.filter, config.filter, config.filterRules, input.screenOwnsTimeline).merge(drillDown)
                            )
                        },
                        onSeeAll = {
                            onRecordsClick(resolveEffectiveFilter(input.filter, config.filter, config.filterRules, input.screenOwnsTimeline))
                        },
                        onClick = { callbacks.accountActions.onClick(renderData.account.id) },
                        onSaveFilterClick = callbacks.filterActions.onSaveClick
                    ),
                    filterRules = config.filterRules,
                    baseFilter = input.filter,
                    filterSheetEnabled = filterSheetEnabled
                )
            }

            config is PlannedPaymentsCardConfig && renderData is CardRenderData.PlannedPayments -> {
                CardWithCallbacks.PlannedPayments(
                    renderData = renderData,
                    callbacks = CardCallbacks.PlannedPayments(
                        onUpdate = { newConfig ->
                            updateHandler.updateCardPlannedPaymentsConfig(
                                config.cardId,
                                newConfig
                            )
                        },
                        onItemClick = callbacks.paymentActions.onClick,
                        onConfirm = callbacks.paymentActions.onConfirm,
                        onSkip = callbacks.paymentActions.onSkip,
                        onSeeAll = callbacks.paymentActions.onSeeAll,
                        onDelete = { onDelete(config.cardId) },
                        onSaveFilterClick = callbacks.filterActions.onSaveClick
                    ),
                    filterRules = config.filterRules,
                    baseFilter = input.filter,
                    filterSheetEnabled = filterSheetEnabled
                )
            }

            config is BudgetsCardConfig && renderData is CardRenderData.Budgets -> {
                CardWithCallbacks.Budgets(
                    renderData = renderData,
                    callbacks = CardCallbacks.Budgets(
                        onUpdate = { newConfig ->
                            updateHandler.updateCardBudgetsConfig(
                                config.cardId,
                                newConfig
                            )
                        },
                        onBudgetClick = callbacks.budgetActions.onClick,
                        onSeeAll = callbacks.budgetActions.onSeeAll,
                        onDelete = { onDelete(config.cardId) },
                        onSaveFilterClick = callbacks.filterActions.onSaveClick
                    ),
                    filterRules = config.filterRules,
                    baseFilter = input.filter,
                    filterSheetEnabled = filterSheetEnabled
                )
            }

            config is LoansCardConfig && renderData is CardRenderData.Loans -> {
                CardWithCallbacks.Loans(
                    renderData = renderData,
                    callbacks = CardCallbacks.Loans(
                        onUpdate = { newConfig ->
                            updateHandler.updateCardLoansConfig(
                                config.cardId,
                                newConfig
                            )
                        },
                        onLoanClick = callbacks.loanActions.onClick,
                        onSeeAll = callbacks.loanActions.onSeeAll,
                        onDelete = { onDelete(config.cardId) },
                        onSaveFilterClick = callbacks.filterActions.onSaveClick
                    ),
                    filterRules = config.filterRules,
                    baseFilter = input.filter,
                    filterSheetEnabled = filterSheetEnabled
                )
            }

            config is RecentActivitiesCardConfig && renderData is CardRenderData.RecentActivities -> {
                CardWithCallbacks.RecentActivities(
                    renderData = renderData,
                    callbacks = CardCallbacks.RecentActivities(
                        onUpdate = { newConfig ->
                            updateHandler.updateCardRecentActivitiesConfig(
                                config.cardId,
                                newConfig
                            )
                        },
                        onTransactionClick = callbacks.transactionActions.onClick,
                        onRecordsClick = { drillDown ->
                            onRecordsClick(
                                resolveEffectiveFilter(input.filter, config.filter, config.filterRules, input.screenOwnsTimeline).merge(drillDown)
                            )
                        },
                        onSeeAll = {
                            onRecordsClick(resolveEffectiveFilter(input.filter, config.filter, config.filterRules, input.screenOwnsTimeline))
                        },
                        onDelete = { onDelete(config.cardId) },
                        onItemExpanded = callbacks.transactionActions.onItemExpanded,
                        onItemCollapsed = callbacks.transactionActions.onItemCollapsed,
                        onStatusChange = callbacks.transactionActions.onStatusChange,
                        onClone = callbacks.transactionActions.onClone,
                        onDeleteTransaction = callbacks.transactionActions.onDelete,
                        onSplit = callbacks.transactionActions.onSplit,
                        onFullSwipeRight = callbacks.transactionActions.onFullSwipeRight,
                        onSaveFilterClick = callbacks.filterActions.onSaveClick
                    ),
                    filterRules = config.filterRules,
                    baseFilter = input.filter,
                    filterSheetEnabled = filterSheetEnabled
                )
            }

            config is CashFlowComparisonCardConfig && renderData is CardRenderData.CashFlowComparison -> {
                CardWithCallbacks.CashFlowComparison(
                    renderData = renderData,
                    callbacks = CardCallbacks.CashFlowComparison(
                        onUpdate = { newConfig ->
                            updateHandler.updateCashFlowComparisonConfig(
                                config.cardId,
                                newConfig
                            )
                        },
                        onDelete = { onDelete(config.cardId) },
                        onRecordsClick = { drillDown ->
                            onRecordsClick(
                                resolveEffectiveFilter(input.filter, config.filter, config.filterRules, input.screenOwnsTimeline).merge(drillDown)
                            )
                        },
                        onSaveFilterClick = callbacks.filterActions.onSaveClick
                    ),
                    filterRules = config.filterRules,
                    baseFilter = input.filter,
                    filterSheetEnabled = filterSheetEnabled
                )
            }

            config is IncomeExpenseBarCardConfig && renderData is CardRenderData.IncomeExpenseBar -> {
                CardWithCallbacks.IncomeExpenseBar(
                    renderData = renderData,
                    callbacks = CardCallbacks.IncomeExpenseBar(
                        onUpdate = { newConfig ->
                            updateHandler.updateIncomeExpenseBarConfig(
                                config.cardId,
                                newConfig
                            )
                        },
                        onDelete = { onDelete(config.cardId) },
                        onRecordsClick = { drillDown ->
                            onRecordsClick(
                                resolveEffectiveFilter(input.filter, config.filter, config.filterRules, input.screenOwnsTimeline).merge(drillDown)
                            )
                        },
                        onSaveFilterClick = callbacks.filterActions.onSaveClick
                    ),
                    filterRules = config.filterRules,
                    baseFilter = input.filter,
                    filterSheetEnabled = filterSheetEnabled
                )
            }

            config is CategorySpendingTrendCardConfig && renderData is CardRenderData.CategorySpendingTrend -> {
                CardWithCallbacks.CategorySpendingTrend(
                    renderData = renderData,
                    callbacks = CardCallbacks.CategorySpendingTrend(
                        onUpdate = { newConfig ->
                            updateHandler.updateCategorySpendingTrendConfig(
                                config.cardId,
                                newConfig
                            )
                        },
                        onDelete = { onDelete(config.cardId) },
                        onRecordsClick = { drillDown ->
                            onRecordsClick(
                                resolveEffectiveFilter(input.filter, config.filter, config.filterRules, input.screenOwnsTimeline).merge(drillDown)
                            )
                        },
                        onSaveFilterClick = callbacks.filterActions.onSaveClick
                    ),
                    filterRules = config.filterRules,
                    baseFilter = input.filter,
                    filterSheetEnabled = filterSheetEnabled
                )
            }

            config is TagUpSetPlotCardConfig && renderData is CardRenderData.TagUpSetPlot -> {
                CardWithCallbacks.TagUpSetPlot(
                    renderData = renderData,
                    callbacks = CardCallbacks.TagUpSetPlot(
                        onUpdate = { newConfig ->
                            updateHandler.updateTagUpSetPlotConfig(
                                config.cardId,
                                newConfig
                            )
                        },
                        onDelete = { onDelete(config.cardId) },
                        onSaveFilterClick = callbacks.filterActions.onSaveClick
                    ),
                    onRecordsClick = { drillDown ->
                        onRecordsClick(
                            resolveEffectiveFilter(input.filter, config.filter, config.filterRules, input.screenOwnsTimeline).merge(drillDown)
                        )
                    },
                    filterRules = config.filterRules,
                    baseFilter = input.filter,
                    filterSheetEnabled = filterSheetEnabled
                )
            }

            else -> null
        }
    }
}

