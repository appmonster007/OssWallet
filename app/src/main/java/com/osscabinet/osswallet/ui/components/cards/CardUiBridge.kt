package com.osscabinet.ossWallet.ui.components.cards

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import com.osscabinet.ossWallet.domain.card.*
import com.osscabinet.ossWallet.domain.model.settings.*
import com.osscabinet.ossWallet.ui.navigation.RecordsFilterContext

// PlannedPaymentsCardConfig is included via domain.model.settings.* wildcard import

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
    onRecordsClick: (RecordsFilterContext) -> Unit,
    onManageAccountsClick: () -> Unit = {},
    onAccountsViewClick: () -> Unit = {},
    onAccountClick: (String) -> Unit = {},
    onPlannedPaymentConfirm: ((Long) -> Unit)? = null,
    onPlannedPaymentSkip: ((Long) -> Unit)? = null,
    currentTheme: String = "light"
): CardWithCallbacks? {
    return remember(config, input, registry, updateHandler, onDelete, onRecordsClick, onManageAccountsClick, onAccountsViewClick, onAccountClick, onPlannedPaymentConfirm, onPlannedPaymentSkip, currentTheme) {
        val renderData = try {
            when (config) {
                is NetWorthCardConfig -> registry.compute(CardType.NET_WORTH, input, config)
                is CurrencyBalancesCardConfig -> registry.compute(CardType.CURRENCY_BALANCES, input, config)
                is CashFlowCardConfig -> registry.compute(CardType.CASH_FLOW, input, config)
                is AccountsCardConfig -> registry.compute(CardType.ACCOUNTS, input, config)
                is CategoryPieChartCardConfig -> registry.compute(CardType.CATEGORY_PIE_CHART, input, config)
                is CategoryTreemapCardConfig -> registry.compute(CardType.CATEGORY_TREEMAP, input, config)
                is AccountHeroCardConfig -> registry.compute(CardType.ACCOUNT_HERO, input, config)
                is PlannedPaymentsCardConfig -> registry.compute(CardType.PLANNED_PAYMENTS, input, config)
                is BudgetsCardConfig -> registry.compute(CardType.BUDGETS, input, config)
                is LoansCardConfig -> registry.compute(CardType.LOANS, input, config)
            }
        } catch (e: Exception) {
            android.util.Log.e("CardUiBridge", "Card compute failed [${config.cardId}]", e)
            return@remember CardWithCallbacks.ComputeError(cardId = config.cardId, message = e.localizedMessage)
        }

        when {
            config is NetWorthCardConfig && renderData is CardRenderData.NetWorth -> {
                CardWithCallbacks.NetWorth(
                    renderData = renderData,
                    callbacks = CardCallbacks.NetWorth(
                        onFontSizeChange = { id, size -> updateHandler.updateCardNetWorthFontSize(id, size) },
                        onFilterChange = { id, filter -> updateHandler.updateCardNetWorthFilter(id, filter) },
                        onDelete = { onDelete(config.cardId) }
                    ),
                    filterRules = config.filterRules,
                    baseFilter = input.filter
                )
            }
            config is CurrencyBalancesCardConfig && renderData is CardRenderData.CurrencyBalances -> {
                CardWithCallbacks.CurrencyBalances(
                    renderData = renderData,
                    callbacks = CardCallbacks.CurrencyBalances(
                        onLayoutChange = { id, layout -> updateHandler.updateCardCurrencyBalancesLayout(id, layout) },
                        onShowBaseConversionChange = { id, show -> updateHandler.updateCardCurrencyBalancesShowBaseConversion(id, show) },
                        onFilterChange = { id, filter -> updateHandler.updateCardCurrencyBalancesFilter(id, filter) },
                        onCurrencyClick = { currency, range ->
                            val (start, end) = com.osscabinet.ossWallet.domain.service.TimelineService.getDateRange(range)
                            onRecordsClick(RecordsFilterContext.Custom(currencies = listOf(currency), startDate = start, endDate = end))
                        },
                        onDelete = { onDelete(config.cardId) }
                    ),
                    filterRules = config.filterRules,
                    baseFilter = input.filter
                )
            }
            config is CashFlowCardConfig && renderData is CardRenderData.CashFlow -> {
                CardWithCallbacks.CashFlow(
                    renderData = renderData,
                    callbacks = CardCallbacks.CashFlow(
                        onShowBaseConversionChange = { id, show -> updateHandler.updateCardCashFlowShowBaseConversion(id, show) },
                        onFilterChange = { id, filter -> updateHandler.updateCardCashFlowFilter(id, filter) },
                        onClickIncome = { range ->
                            val (start, end) = com.osscabinet.ossWallet.domain.service.TimelineService.getDateRange(range)
                            onRecordsClick(RecordsFilterContext.Custom(isIncome = true, startDate = start, endDate = end))
                        },
                        onClickExpense = { range ->
                            val (start, end) = com.osscabinet.ossWallet.domain.service.TimelineService.getDateRange(range)
                            onRecordsClick(RecordsFilterContext.Custom(isIncome = false, startDate = start, endDate = end))
                        },
                        onDelete = { onDelete(config.cardId) }
                    ),
                    filterRules = config.filterRules,
                    baseFilter = input.filter
                )
            }
            config is AccountsCardConfig && renderData is CardRenderData.Accounts -> {
                CardWithCallbacks.Accounts(
                    renderData = renderData,
                    callbacks = CardCallbacks.Accounts(
                        onGridColumnsChange = { id, cols -> updateHandler.updateCardAccountsGridColumns(id, cols) },
                        onLayoutChange = { id, layout -> updateHandler.updateCardAccountsLayout(id, layout) },
                        onFilterChange = { id, filter -> updateHandler.updateCardAccountsFilter(id, filter) },
                        onShowAddAccountCardChange = { id, show -> updateHandler.updateCardAccountsShowAddAccountCard(id, show) },
                        onManageClick = onManageAccountsClick,
                        onHeaderClick = onAccountsViewClick,
                        onAccountClick = onAccountClick,
                        onRecordsClick = onRecordsClick,
                        onDelete = { onDelete(config.cardId) }
                    ),
                    filterRules = config.filterRules,
                    baseFilter = input.filter
                )
            }
            config is CategoryPieChartCardConfig && renderData is CardRenderData.CategoryPieChart -> {
                CardWithCallbacks.CategoryPieChart(
                    renderData = renderData,
                    callbacks = CardCallbacks.Analytics(
                        onUpdate = { newConfig -> 
                            if (newConfig is CategoryPieChartCardConfig) {
                                updateHandler.updateCardCategoryPieChartConfig(config.cardId, newConfig)
                            }
                        },
                        onDelete = { onDelete(config.cardId) },
                        onRecordsClick = onRecordsClick
                    ),
                    filterRules = config.filterRules,
                    baseFilter = input.filter
                )
            }
            config is CategoryTreemapCardConfig && renderData is CardRenderData.CategoryTreemap -> {
                CardWithCallbacks.CategoryTreemap(
                    renderData = renderData,
                    callbacks = CardCallbacks.Analytics(
                        onUpdate = { newConfig -> 
                            if (newConfig is CategoryTreemapCardConfig) {
                                updateHandler.updateCardCategoryTreemapConfig(config.cardId, newConfig)
                            }
                        },
                        onDelete = { onDelete(config.cardId) },
                        onRecordsClick = onRecordsClick
                    ),
                    filterRules = config.filterRules,
                    baseFilter = input.filter
                )
            }
            config is AccountHeroCardConfig && renderData is CardRenderData.AccountHero -> {
                CardWithCallbacks.AccountHero(
                    renderData = renderData,
                    callbacks = CardCallbacks.AccountHero(
                        onShowCurrencyBadgesChange = { id, show -> updateHandler.updateCardHeroShowCurrencyBadges(id, show) },
                        onFilterChange = { id, filter -> updateHandler.updateCardHeroFilter(id, filter) },
                        onDelete = { onDelete(config.cardId) }
                    ),
                    filterRules = config.filterRules,
                    baseFilter = input.filter
                )
            }
            config is PlannedPaymentsCardConfig && renderData is CardRenderData.PlannedPayments -> {
                CardWithCallbacks.PlannedPayments(
                    renderData = renderData,
                    callbacks = CardCallbacks.PlannedPayments(
                        onUpdate = { newConfig -> updateHandler.updateCardPlannedPaymentsConfig(config.cardId, newConfig) },
                        onItemClick = {},
                        onConfirm = onPlannedPaymentConfirm,
                        onSkip = onPlannedPaymentSkip,
                        onDelete = { onDelete(config.cardId) }
                    ),
                    filterRules = config.filterRules,
                    baseFilter = input.filter
                )
            }
            config is BudgetsCardConfig && renderData is CardRenderData.Budgets -> {
                CardWithCallbacks.Budgets(
                    renderData = renderData,
                    callbacks = CardCallbacks.Budgets(
                        onUpdate = { newConfig -> updateHandler.updateCardBudgetsConfig(config.cardId, newConfig) },
                        onBudgetClick = {},
                        onDelete = { onDelete(config.cardId) }
                    ),
                    filterRules = config.filterRules,
                    baseFilter = input.filter
                )
            }
            config is LoansCardConfig && renderData is CardRenderData.Loans -> {
                CardWithCallbacks.Loans(
                    renderData = renderData,
                    callbacks = CardCallbacks.Loans(
                        onUpdate = { newConfig -> updateHandler.updateCardLoansConfig(config.cardId, newConfig) },
                        onLoanClick = {},
                        onDelete = { onDelete(config.cardId) }
                    ),
                    filterRules = config.filterRules,
                    baseFilter = input.filter
                )
            }
            else -> null
        }
    }
}
