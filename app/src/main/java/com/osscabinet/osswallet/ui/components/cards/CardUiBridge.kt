package com.osscabinet.ossWallet.ui.components.cards

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import com.osscabinet.ossWallet.domain.card.*
import com.osscabinet.ossWallet.domain.model.settings.*
import com.osscabinet.ossWallet.ui.navigation.RecordsFilterContext

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
    currentTheme: String = "light"
): CardWithCallbacks? {
    return remember(config, input) {
        val renderData = try {
            when (config) {
                is NetWorthCardConfig -> registry.compute(CardType.NET_WORTH, input, config)
                is CurrencyBalancesCardConfig -> registry.compute(CardType.CURRENCY_BALANCES, input, config)
                is CashFlowCardConfig -> registry.compute(CardType.CASH_FLOW, input, config)
                is AccountsCardConfig -> registry.compute(CardType.ACCOUNTS, input, config)
                is CategoryBreakdownCardConfig -> registry.compute(CardType.CATEGORY_BREAKDOWN, input, config)
                is CategoryExpenseTreemapCardConfig -> registry.compute(CardType.CATEGORY_EXPENSE_TREEMAP, input, config)
                is AccountHeroCardConfig -> registry.compute(CardType.ACCOUNT_HERO, input, config)
            }
        } catch (e: Exception) {
            null
        } ?: return@remember null

        when {
            config is NetWorthCardConfig && renderData is CardRenderData.NetWorth -> {
                CardWithCallbacks.NetWorth(
                    renderData = renderData,
                    callbacks = CardCallbacks.WithFontSizeAndTimeline(
                        onFontSizeChange = { id, size -> updateHandler.updateCardNetWorthFontSize(id, size) },
                        onTimelineRangeChange = { id, range -> updateHandler.updateCardNetWorthTimelineRange(id, range) },
                        onDelete = { onDelete(config.cardId) }
                    )
                )
            }
            config is CurrencyBalancesCardConfig && renderData is CardRenderData.CurrencyBalances -> {
                CardWithCallbacks.CurrencyBalances(
                    renderData = renderData,
                    callbacks = CardCallbacks.CurrencyBalances(
                        onLayoutChange = { id, layout -> updateHandler.updateCardCurrencyBalancesLayout(id, layout) },
                        onShowBaseConversionChange = { id, show -> updateHandler.updateCardCurrencyBalancesShowBaseConversion(id, show) },
                        onTimelineRangeChange = { id, range -> updateHandler.updateCardCurrencyBalancesTimelineRange(id, range) },
                        onCurrencyClick = { currency, range ->
                            val (start, end) = com.osscabinet.ossWallet.domain.service.TimelineService.getDateRange(range)
                            onRecordsClick(RecordsFilterContext.Custom(currencies = listOf(currency), startDate = start, endDate = end))
                        },
                        onDelete = { onDelete(config.cardId) }
                    )
                )
            }
            config is CashFlowCardConfig && renderData is CardRenderData.CashFlow -> {
                CardWithCallbacks.CashFlow(
                    renderData = renderData,
                    callbacks = CardCallbacks.CashFlow(
                        onShowBaseConversionChange = { id, show -> updateHandler.updateCardCashFlowShowBaseConversion(id, show) },
                        onTimelineRangeChange = { id, range -> updateHandler.updateCardCashFlowTimelineRange(id, range) },
                        onClickIncome = { range ->
                            val (start, end) = com.osscabinet.ossWallet.domain.service.TimelineService.getDateRange(range)
                            onRecordsClick(RecordsFilterContext.Custom(isIncome = true, startDate = start, endDate = end))
                        },
                        onClickExpense = { range ->
                            val (start, end) = com.osscabinet.ossWallet.domain.service.TimelineService.getDateRange(range)
                            onRecordsClick(RecordsFilterContext.Custom(isIncome = false, startDate = start, endDate = end))
                        },
                        onDelete = { onDelete(config.cardId) }
                    )
                )
            }
            config is AccountsCardConfig && renderData is CardRenderData.Accounts -> {
                CardWithCallbacks.Accounts(
                    renderData = renderData,
                    callbacks = CardCallbacks.Accounts(
                        onGridColumnsChange = { id, cols -> updateHandler.updateCardAccountsGridColumns(id, cols) },
                        onLayoutChange = { id, layout -> updateHandler.updateCardAccountsLayout(id, layout) },
                        onTimelineRangeChange = { id, range -> updateHandler.updateCardAccountsTimelineRange(id, range) },
                        onShowAddAccountCardChange = { id, show -> updateHandler.updateCardAccountsShowAddAccountCard(id, show) },
                        onManageClick = onManageAccountsClick,
                        onHeaderClick = onAccountsViewClick,
                        onAccountClick = onAccountClick,
                        onRecordsClick = onRecordsClick,
                        onDelete = { onDelete(config.cardId) }
                    )
                )
            }
            config is CategoryBreakdownCardConfig && renderData is CardRenderData.CategoryBreakdown -> {
                CardWithCallbacks.CategoryBreakdown(
                    renderData = renderData,
                    callbacks = CardCallbacks.Analytics(
                        onUpdate = { },
                        onDelete = { onDelete(config.cardId) },
                        onRecordsClick = onRecordsClick
                    )
                )
            }
            config is CategoryExpenseTreemapCardConfig && renderData is CardRenderData.CategoryTreemap -> {
                CardWithCallbacks.CategoryTreemap(
                    renderData = renderData,
                    callbacks = CardCallbacks.Analytics(
                        onUpdate = { },
                        onDelete = { onDelete(config.cardId) },
                        onRecordsClick = onRecordsClick
                    )
                )
            }
            config is AccountHeroCardConfig && renderData is CardRenderData.AccountHero -> {
                CardWithCallbacks.AccountHero(
                    renderData = renderData,
                    callbacks = CardCallbacks.AccountHero(
                        onShowCurrencyBadgesChange = { id, show -> updateHandler.updateCardHeroShowCurrencyBadges(id, show) },
                        onDelete = { onDelete(config.cardId) }
                    )
                )
            }
            else -> null
        }
    }
}
