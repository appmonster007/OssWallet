package com.osscabinet.ossWallet.domain.card

import com.osscabinet.ossWallet.domain.model.TransactionFilter
import com.osscabinet.ossWallet.domain.model.settings.*
import com.osscabinet.ossWallet.domain.model.settings.CategorySpendingTrendCardConfig
import com.osscabinet.ossWallet.domain.model.settings.IncomeExpenseBarCardConfig
import com.osscabinet.ossWallet.domain.repository.CardRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

/**
 * Robust mix-in interface for handling card configuration updates.
 * ViewModels implement this to provide card update functionality with zero boilerplate
 * by leveraging default interface methods.
 */
interface CardUpdateHandler {
    val cardUpdateScope: CoroutineScope
    val cardRepository: CardRepository
    val cardScreenContext: ScreenContext

    /**
     * Updates the accounting perspective for any card that supports it.
     */
    fun updateCardPerspective(cardId: String, perspective: AccountingPerspective) {
        cardUpdateScope.launch {
            cardRepository.updateCard(cardScreenContext, cardId) { config ->
                val updated = when (config) {
                    is NetWorthCardConfig -> config.copy(perspective = perspective)
                    is CashFlowCardConfig -> config.copy(perspective = perspective)
                    is AccountsCardConfig -> config.copy(perspective = perspective)
                    is AccountHeroCardConfig -> config.copy(perspective = perspective)
                    is CategoryPieChartCardConfig -> config.copy(perspective = perspective)
                    is CategoryTreemapCardConfig -> config.copy(perspective = perspective)
                    is CashFlowComparisonCardConfig -> config.copy(perspective = perspective)
                    is IncomeExpenseBarCardConfig -> config.copy(perspective = perspective)
                    is CategorySpendingTrendCardConfig -> config.copy(perspective = perspective)
                    is TagUpSetPlotCardConfig -> config.copy(perspective = perspective)
                    else -> config
                }
                
                // Safety: If the card has a selected metric that's unsupported in the new
                // perspective (e.g., TRANSFER in CASH_FLOW), reset it to NET.
                val labels = AccountingPerspective.labelsFor(perspective)
                
                when (updated) {
                    is CategoryPieChartCardConfig -> if (updated.selectedMetric !in labels) updated.copy(selectedMetric = ChartMetric.NET) else updated
                    is CategoryTreemapCardConfig -> if (updated.selectedMetric !in labels) updated.copy(selectedMetric = ChartMetric.NET) else updated
                    is CashFlowComparisonCardConfig -> if (updated.selectedMetric !in labels) updated.copy(selectedMetric = ChartMetric.NET) else updated
                    is CategorySpendingTrendCardConfig -> if (updated.selectedMetric !in labels) updated.copy(selectedMetric = ChartMetric.NET) else updated
                    is TagUpSetPlotCardConfig -> if (updated.selectedMetric !in labels) updated.copy(selectedMetric = ChartMetric.NET) else updated
                    else -> updated
                }
            }
        }
    }

    /**
     * Updates excludeInternalTransfers for any card that supports it by updating the CashFlow
     * perspective in-place. No-ops if the current perspective is Accounting.
     */
    fun updateCardExcludeInternalTransfers(cardId: String, exclude: Boolean) {
        cardUpdateScope.launch {
            cardRepository.updateCard(cardScreenContext, cardId) { config ->
                fun updatePerspective(current: AccountingPerspective): AccountingPerspective =
                    if (current is AccountingPerspective.CashFlow) current.copy(excludeInternalTransfers = exclude)
                    else current
                when (config) {
                    is NetWorthCardConfig -> config.copy(perspective = updatePerspective(config.perspective))
                    is CashFlowCardConfig -> config.copy(perspective = updatePerspective(config.perspective))
                    is AccountsCardConfig -> config.copy(perspective = updatePerspective(config.perspective))
                    is AccountHeroCardConfig -> config.copy(perspective = updatePerspective(config.perspective))
                    is CategoryPieChartCardConfig -> config.copy(perspective = updatePerspective(config.perspective))
                    is CategoryTreemapCardConfig -> config.copy(perspective = updatePerspective(config.perspective))
                    is CashFlowComparisonCardConfig -> config.copy(perspective = updatePerspective(config.perspective))
                    is IncomeExpenseBarCardConfig -> config.copy(perspective = updatePerspective(config.perspective))
                    is CategorySpendingTrendCardConfig -> config.copy(perspective = updatePerspective(config.perspective))
                    is TagUpSetPlotCardConfig -> config.copy(perspective = updatePerspective(config.perspective))
                    else -> config
                }
            }
        }
    }

    // Net Worth
    fun updateCardNetWorthFontSize(cardId: String, fontSize: String) {
        cardUpdateScope.launch {
            cardRepository.updateCard(cardScreenContext, cardId) { config ->
                if (config is NetWorthCardConfig) config.copy(fontSize = fontSize) else config
            }
        }
    }

    fun updateCardNetWorthShowComparison(cardId: String, showComparison: Boolean) {
        cardUpdateScope.launch {
            cardRepository.updateCard(cardScreenContext, cardId) { config ->
                if (config is NetWorthCardConfig) config.copy(showComparison = showComparison) else config
            }
        }
    }

    fun updateCardNetWorthFilter(cardId: String, filter: TransactionFilter) {
        cardUpdateScope.launch {
            cardRepository.updateCard(cardScreenContext, cardId) { config ->
                if (config is NetWorthCardConfig) config.copy(filter = filter) else config
            }
        }
    }
    
    // Currency Balances
    fun updateCardCurrencyBalancesLayout(cardId: String, layout: String) {
        cardUpdateScope.launch {
            cardRepository.updateCard(cardScreenContext, cardId) { config ->
                if (config is CurrencyBalancesCardConfig) config.copy(layout = layout) else config
            }
        }
    }

    fun updateCardCurrencyBalancesGridColumns(cardId: String, columns: Int) {
        cardUpdateScope.launch {
            cardRepository.updateCard(cardScreenContext, cardId) { config ->
                if (config is CurrencyBalancesCardConfig) config.copy(gridColumns = columns) else config
            }
        }
    }
    
    fun updateCardCurrencyBalancesShowBaseConversion(cardId: String, show: Boolean) {
        cardUpdateScope.launch {
            cardRepository.updateCard(cardScreenContext, cardId) { config ->
                if (config is CurrencyBalancesCardConfig) config.copy(showBaseConversion = show) else config
            }
        }
    }
    
    fun updateCardCurrencyBalancesFilter(cardId: String, filter: TransactionFilter) {
        cardUpdateScope.launch {
            cardRepository.updateCard(cardScreenContext, cardId) { config ->
                if (config is CurrencyBalancesCardConfig) config.copy(filter = filter) else config
            }
        }
    }
    
    // Cash Flow
    fun updateCardCashFlowShowBaseConversion(cardId: String, show: Boolean) {
        cardUpdateScope.launch {
            cardRepository.updateCard(cardScreenContext, cardId) { config ->
                if (config is CashFlowCardConfig) config.copy(showBaseConversion = show) else config
            }
        }
    }
    
    fun updateCardCashFlowFilter(cardId: String, filter: TransactionFilter) {
        cardUpdateScope.launch {
            cardRepository.updateCard(cardScreenContext, cardId) { config ->
                if (config is CashFlowCardConfig) config.copy(filter = filter) else config
            }
        }
    }

    fun updateCardCashFlowShowNetColumn(cardId: String, show: Boolean) {
        cardUpdateScope.launch {
            cardRepository.updateCard(cardScreenContext, cardId) { config ->
                if (config is CashFlowCardConfig) config.copy(showNetColumn = show) else config
            }
        }
    }

    // Accounts
    fun updateCardAccountsLayout(cardId: String, layout: String) {
        cardUpdateScope.launch {
            cardRepository.updateCard(cardScreenContext, cardId) { config ->
                if (config is AccountsCardConfig) config.copy(layout = layout) else config
            }
        }
    }
    
    fun updateCardAccountsGridColumns(cardId: String, columns: Int) {
        cardUpdateScope.launch {
            cardRepository.updateCard(cardScreenContext, cardId) { config ->
                if (config is AccountsCardConfig) config.copy(gridColumns = columns) else config
            }
        }
    }
    
    fun updateCardAccountsShowAddAccountCard(cardId: String, show: Boolean) {
        cardUpdateScope.launch {
            cardRepository.updateCard(cardScreenContext, cardId) { config ->
                if (config is AccountsCardConfig) config.copy(showAddAccountCard = show) else config
            }
        }
    }

    fun updateCardAccountsFilter(cardId: String, filter: TransactionFilter) {
        cardUpdateScope.launch {
            cardRepository.updateCard(cardScreenContext, cardId) { config ->
                if (config is AccountsCardConfig) config.copy(filter = filter) else config
            }
        }
    }
    
    // Account Hero
    fun updateCardHeroShowCurrencyBadges(cardId: String, show: Boolean) {
        cardUpdateScope.launch {
            cardRepository.updateCard(cardScreenContext, cardId) { config ->
                if (config is AccountHeroCardConfig) config.copy(showCurrencyBadges = show) else config
            }
        }
    }



    fun updateCardHeroFilter(cardId: String, filter: TransactionFilter) {
        cardUpdateScope.launch {
            cardRepository.updateCard(cardScreenContext, cardId) { config ->
                if (config is AccountHeroCardConfig) config.copy(filter = filter) else config
            }
        }
    }

    // Analytics
    fun updateCardCategoryPieChartConfig(cardId: String, config: CategoryPieChartCardConfig) {
        cardUpdateScope.launch {
            cardRepository.updateCard(cardScreenContext, cardId) { config }
        }
    }
    
    fun updateCardCategoryTreemapConfig(cardId: String, config: CategoryTreemapCardConfig) {
        cardUpdateScope.launch {
            cardRepository.updateCard(cardScreenContext, cardId) { config }
        }
    }

    // New Cards
    fun updateCardPlannedPaymentsConfig(cardId: String, config: PlannedPaymentsCardConfig) {
        cardUpdateScope.launch {
            cardRepository.updateCard(cardScreenContext, cardId) { config }
        }
    }

    fun updateCardPlannedPaymentsFilter(cardId: String, filter: TransactionFilter) {
        cardUpdateScope.launch {
            cardRepository.updateCard(cardScreenContext, cardId) { config ->
                if (config is PlannedPaymentsCardConfig) config.copy(filter = filter) else config
            }
        }
    }

    fun updateCardBudgetsConfig(cardId: String, config: BudgetsCardConfig) {
        cardUpdateScope.launch {
            cardRepository.updateCard(cardScreenContext, cardId) { config }
        }
    }

    fun updateCardBudgetsFilter(cardId: String, filter: TransactionFilter) {
        cardUpdateScope.launch {
            cardRepository.updateCard(cardScreenContext, cardId) { config ->
                if (config is BudgetsCardConfig) config.copy(filter = filter) else config
            }
        }
    }

    fun updateCardLoansConfig(cardId: String, config: LoansCardConfig) {
        cardUpdateScope.launch {
            cardRepository.updateCard(cardScreenContext, cardId) { config }
        }
    }

    fun updateCardLoansFilter(cardId: String, filter: TransactionFilter) {
        cardUpdateScope.launch {
            cardRepository.updateCard(cardScreenContext, cardId) { config ->
                if (config is LoansCardConfig) config.copy(filter = filter) else config
            }
        }
    }

    fun updateCardRecentActivitiesConfig(cardId: String, config: RecentActivitiesCardConfig) {
        cardUpdateScope.launch {
            cardRepository.updateCard(cardScreenContext, cardId) { config }
        }
    }

    fun updateCashFlowComparisonConfig(cardId: String, config: CashFlowComparisonCardConfig) {
        cardUpdateScope.launch {
            cardRepository.updateCard(cardScreenContext, cardId) { config }
        }
    }

    fun updateIncomeExpenseBarConfig(cardId: String, config: IncomeExpenseBarCardConfig) {
        cardUpdateScope.launch {
            cardRepository.updateCard(cardScreenContext, cardId) { config }
        }
    }

    fun updateCategorySpendingTrendConfig(cardId: String, config: CategorySpendingTrendCardConfig) {
        cardUpdateScope.launch {
            cardRepository.updateCard(cardScreenContext, cardId) { config }
        }
    }

    fun updateTagUpSetPlotConfig(cardId: String, config: TagUpSetPlotCardConfig) {
        cardUpdateScope.launch {
            cardRepository.updateCard(cardScreenContext, cardId) { config }
        }
    }

    // Structural Updates
    fun addCard(type: CardType) {
        cardUpdateScope.launch {
            val config = CardConfig.createDefault(type, cardScreenContext)
            cardRepository.addCard(cardScreenContext, config)
        }
    }

    fun addCard(config: CardConfig) {
        cardUpdateScope.launch {
            cardRepository.addCard(cardScreenContext, config)
        }
    }

    fun removeCard(cardId: String) {
        cardUpdateScope.launch {
            cardRepository.removeCard(cardScreenContext, cardId)
        }
    }

    fun reorderCards(newOrder: List<CardConfig>) {
        cardUpdateScope.launch {
            cardRepository.reorderCards(cardScreenContext, newOrder)
        }
    }
}
