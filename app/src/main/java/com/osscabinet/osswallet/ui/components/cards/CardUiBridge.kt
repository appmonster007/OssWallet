package com.osscabinet.ossWallet.ui.components.cards

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import com.osscabinet.ossWallet.domain.card.CardComputerRegistry
import com.osscabinet.ossWallet.domain.card.CardInput
import com.osscabinet.ossWallet.domain.card.CardRenderData
import com.osscabinet.ossWallet.domain.card.CardType
import com.osscabinet.ossWallet.domain.card.CardUpdateHandler
import com.osscabinet.ossWallet.domain.model.settings.AccountHeroCardConfig
import com.osscabinet.ossWallet.domain.model.settings.AccountsCardConfig
import com.osscabinet.ossWallet.domain.model.settings.BudgetsCardConfig
import com.osscabinet.ossWallet.domain.model.settings.CardConfig
import com.osscabinet.ossWallet.domain.model.settings.CashFlowCardConfig
import com.osscabinet.ossWallet.domain.model.settings.CategoryPieChartCardConfig
import com.osscabinet.ossWallet.domain.model.settings.CategoryTreemapCardConfig
import com.osscabinet.ossWallet.domain.model.settings.CurrencyBalancesCardConfig
import com.osscabinet.ossWallet.domain.model.settings.LoansCardConfig
import com.osscabinet.ossWallet.domain.model.settings.NetWorthCardConfig
import com.osscabinet.ossWallet.domain.model.settings.RecentActivitiesCardConfig
import com.osscabinet.ossWallet.domain.model.settings.PlannedPaymentsCardConfig
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
    onAccountClick: (String) -> Unit = {},
    onTransactionClick: (Long) -> Unit = {},
    onPlannedPaymentConfirm: ((Long) -> Unit)? = null,
    onPlannedPaymentSkip: ((Long) -> Unit)? = null,
    onItemExpanded: (Long) -> Unit = {},
    onItemCollapsed: (Long) -> Unit = {},
    onStatusChange: (Long, com.osscabinet.ossWallet.domain.model.TransactionStatus) -> Unit = { _, _ -> },
    onClone: (Long) -> Unit = {},
    onDeleteTransaction: (Long) -> Unit = {},
    onSplit: (Long) -> Unit = {},
    onFullSwipeRight: (Long) -> Unit = {},
    currentTheme: String = "light"
): CardWithCallbacks? {
    return remember(
        config,
        input,
        registry,
        updateHandler,
        onDelete,
        onRecordsClick,
        onManageAccountsClick,
        onAccountClick,
        onTransactionClick,
        onPlannedPaymentConfirm,
        onPlannedPaymentSkip,
        onItemExpanded,
        onItemCollapsed,
        onStatusChange,
        onClone,
        onDeleteTransaction,
        onSplit,
        onFullSwipeRight,
        currentTheme
    ) {
        val renderData = try {
            when (config) {
                is NetWorthCardConfig -> registry.compute(CardType.NET_WORTH, input, config)
                is CurrencyBalancesCardConfig -> registry.compute(
                    CardType.CURRENCY_BALANCES,
                    input,
                    config
                )

                is CashFlowCardConfig -> registry.compute(CardType.CASH_FLOW, input, config)
                is AccountsCardConfig -> registry.compute(CardType.ACCOUNTS, input, config)
                is CategoryPieChartCardConfig -> registry.compute(
                    CardType.CATEGORY_PIE_CHART,
                    input,
                    config
                )

                is CategoryTreemapCardConfig -> registry.compute(
                    CardType.CATEGORY_TREEMAP,
                    input,
                    config
                )

                is AccountHeroCardConfig -> registry.compute(CardType.ACCOUNT_HERO, input, config)
                is PlannedPaymentsCardConfig -> registry.compute(
                    CardType.PLANNED_PAYMENTS,
                    input,
                    config
                )

                is BudgetsCardConfig -> registry.compute(CardType.BUDGETS, input, config)
                is LoansCardConfig -> registry.compute(CardType.LOANS, input, config)
                is RecentActivitiesCardConfig -> registry.compute(CardType.RECENT_ACTIVITIES, input, config)
            }
        } catch (e: Exception) {
            android.util.Log.e("CardUiBridge", "Card compute failed [${config.cardId}]", e)
            return@remember CardWithCallbacks.ComputeError(
                cardId = config.cardId,
                message = e.localizedMessage
            )
        }

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
                            val (start, end) = com.osscabinet.ossWallet.domain.service.timeline.TimelineService.getDateRange(
                                range
                            )
                            onRecordsClick(
                                RecordsFilterContext.Custom(
                                    currencies = listOf(currency),
                                    startDate = start,
                                    endDate = end
                                )
                            )
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
                        onClickIncome = { range ->
                            val (start, end) = com.osscabinet.ossWallet.domain.service.timeline.TimelineService.getDateRange(
                                range
                            )
                            onRecordsClick(
                                RecordsFilterContext.Custom(
                                    transactionType = "income",
                                    startDate = start,
                                    endDate = end
                                )
                            )
                        },
                        onClickExpense = { range ->
                            val (start, end) = com.osscabinet.ossWallet.domain.service.timeline.TimelineService.getDateRange(
                                range
                            )
                            onRecordsClick(
                                RecordsFilterContext.Custom(
                                    transactionType = "expense",
                                    startDate = start,
                                    endDate = end
                                )
                            )
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
                        onManageClick = onManageAccountsClick,
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
                                updateHandler.updateCardCategoryPieChartConfig(
                                    config.cardId,
                                    newConfig
                                )
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
                                updateHandler.updateCardCategoryTreemapConfig(
                                    config.cardId,
                                    newConfig
                                )
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
                        onShowCurrencyBadgesChange = { id, show ->
                            updateHandler.updateCardHeroShowCurrencyBadges(
                                id,
                                show
                            )
                        },
                        onFilterChange = { id, filter ->
                            updateHandler.updateCardHeroFilter(
                                id,
                                filter
                            )
                        },
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
                        onUpdate = { newConfig ->
                            updateHandler.updateCardPlannedPaymentsConfig(
                                config.cardId,
                                newConfig
                            )
                        },
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
                        onUpdate = { newConfig ->
                            updateHandler.updateCardBudgetsConfig(
                                config.cardId,
                                newConfig
                            )
                        },
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
                        onUpdate = { newConfig ->
                            updateHandler.updateCardLoansConfig(
                                config.cardId,
                                newConfig
                            )
                        },
                        onLoanClick = {},
                        onDelete = { onDelete(config.cardId) }
                    ),
                    filterRules = config.filterRules,
                    baseFilter = input.filter
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
                        onTransactionClick = onTransactionClick,
                        onRecordsClick = onRecordsClick,
                        onDelete = { onDelete(config.cardId) },
                        onItemExpanded = onItemExpanded,
                        onItemCollapsed = onItemCollapsed,
                        onStatusChange = onStatusChange,
                        onClone = onClone,
                        onDeleteTransaction = onDeleteTransaction,
                        onSplit = onSplit,
                        onFullSwipeRight = onFullSwipeRight
                    ),
                    filterRules = config.filterRules,
                    baseFilter = input.filter
                )
            }

            else -> null
        }
    }
}
