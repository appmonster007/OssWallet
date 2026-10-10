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
     * ARC-33: generic, type-safe update for a single card type's config. Use this
     * when the change applies to ONE specific card type (e.g., a setting that only
     * NetWorthCardConfig has). For shared fields that exist on many card types
     * (perspective, excludeInternalTransfers, etc.), prefer the dedicated helper
     * methods below — they apply the change to every card type that supports it
     * in a single call.
     *
     * Example:
     * ```kotlin
     * updateCardConfig<NetWorthCardConfig>(cardId) { it.copy(showComparison = true) }
     * ```
     *
     * The cast is checked at runtime; a mismatched [C] vs the persisted config type
     * for [cardId] leaves the config unchanged (the transform is skipped) instead of
     * crashing the UI.
     */
    fun <C : CardConfig> updateCardConfig(
        cardId: String,
        configClass: Class<C>,
        transform: (C) -> C,
    ) {
        cardUpdateScope.launch {
            cardRepository.updateCard(cardScreenContext, cardId) { config ->
                @Suppress("UNCHECKED_CAST")
                if (configClass.isInstance(config)) transform(config as C) else config
            }
        }
    }

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
                // perspective (e.g., TRANSFER in CASH_FLOW), reset it to NET or map it appropriately.
                // Pass splitTransfers so that TRANSFER_IN/OUT are not reset when split is on.
                fun labelsForUpdated(splitTransfers: Boolean) =
                    AccountingPerspective.labelsFor(perspective, splitTransfers)

                fun adjustMetric(selectedMetric: ChartMetric, splitTransfers: Boolean): ChartMetric {
                    val labels = labelsForUpdated(splitTransfers)
                    if (selectedMetric in labels) return selectedMetric
                    
                    return when {
                        perspective is AccountingPerspective.CashFlow && selectedMetric == ChartMetric.INCOME -> ChartMetric.INFLOW
                        perspective is AccountingPerspective.CashFlow && selectedMetric == ChartMetric.EXPENSE -> ChartMetric.OUTFLOW
                        perspective is AccountingPerspective.Accounting && selectedMetric == ChartMetric.INFLOW -> ChartMetric.INCOME
                        perspective is AccountingPerspective.Accounting && selectedMetric == ChartMetric.OUTFLOW -> ChartMetric.EXPENSE
                        // Preserve transfer metrics even if not in current labels (they'll be hidden but not lost)
                        selectedMetric == ChartMetric.TRANSFER || 
                        selectedMetric == ChartMetric.TRANSFER_IN || 
                        selectedMetric == ChartMetric.TRANSFER_OUT -> selectedMetric
                        else -> ChartMetric.NET
                    }
                }

                when (updated) {
                    is CategoryPieChartCardConfig -> updated.copy(selectedMetric = adjustMetric(updated.selectedMetric, updated.splitTransfers))
                    is CategoryTreemapCardConfig -> updated.copy(selectedMetric = adjustMetric(updated.selectedMetric, updated.splitTransfers))
                    is CashFlowComparisonCardConfig -> updated.copy(selectedMetric = adjustMetric(updated.selectedMetric, updated.splitTransfers))
                    is CategorySpendingTrendCardConfig -> updated.copy(selectedMetric = adjustMetric(updated.selectedMetric, updated.splitTransfers))
                    is TagUpSetPlotCardConfig -> updated.copy(selectedMetric = adjustMetric(updated.selectedMetric, updated.splitTransfers))
                    is IncomeExpenseBarCardConfig -> {
                        val remapped = updated.visibleMetrics.map { m -> adjustMetric(m, updated.splitTransfers) }.toSet()
                        updated.copy(visibleMetrics = remapped)
                    }
                    else -> updated
                }
            }
        }
    }

    /**
     * Updates excludeInternalTransfers for any card that supports it.
     */
    fun updateCardExcludeInternalTransfers(cardId: String, exclude: Boolean) {
        cardUpdateScope.launch {
            cardRepository.updateCard(cardScreenContext, cardId) { config ->
                when (config) {
                    is NetWorthCardConfig -> config.copy(excludeInternalTransfers = exclude)
                    is CashFlowCardConfig -> config.copy(excludeInternalTransfers = exclude)
                    is AccountsCardConfig -> config.copy(excludeInternalTransfers = exclude)
                    is AccountHeroCardConfig -> config.copy(excludeInternalTransfers = exclude)
                    is CategoryPieChartCardConfig -> config.copy(excludeInternalTransfers = exclude)
                    is CategoryTreemapCardConfig -> config.copy(excludeInternalTransfers = exclude)
                    is CashFlowComparisonCardConfig -> config.copy(excludeInternalTransfers = exclude)
                    is IncomeExpenseBarCardConfig -> config.copy(excludeInternalTransfers = exclude)
                    is CategorySpendingTrendCardConfig -> config.copy(excludeInternalTransfers = exclude)
                    is TagUpSetPlotCardConfig -> config.copy(excludeInternalTransfers = exclude)
                    else -> config
                }
            }
        }
    }

    fun updateCardSplitTransfers(cardId: String, split: Boolean) {
        cardUpdateScope.launch {
            cardRepository.updateCard(cardScreenContext, cardId) { config ->
                when (config) {
                    is NetWorthCardConfig -> config.copy(splitTransfers = split)
                    is CashFlowCardConfig -> config.copy(splitTransfers = split)
                    is AccountsCardConfig -> config.copy(splitTransfers = split)
                    is AccountHeroCardConfig -> config.copy(splitTransfers = split)
                    else -> config
                }
            }
        }
    }

    // Net Worth
    fun updateCardNetWorthFontSize(cardId: String, fontSize: String) {
        cardUpdateScope.launch {
            cardRepository.updateCard(cardScreenContext, cardId) { config ->
                (config as? NetWorthCardConfig)?.copy(fontSize = fontSize) ?: config
            }
        }
    }

    fun updateCardNetWorthShowTrendLine(cardId: String, show: Boolean) {
        cardUpdateScope.launch {
            cardRepository.updateCard(cardScreenContext, cardId) { config ->
                (config as? NetWorthCardConfig)?.copy(showTrendLine = show) ?: config
            }
        }
    }

    fun updateCardShowComparison(cardId: String, show: Boolean) {
        cardUpdateScope.launch {
            cardRepository.updateCard(cardScreenContext, cardId) { config ->
                when (config) {
                    is NetWorthCardConfig -> config.copy(showComparison = show)
                    is AccountsCardConfig -> config.copy(showComparison = show)
                    is AccountHeroCardConfig -> config.copy(showComparison = show)
                    is CategoryPieChartCardConfig -> config.copy(showComparison = show)
                    is CategoryTreemapCardConfig -> config.copy(showComparison = show)
                    is CashFlowCardConfig -> config.copy(showComparison = show)
                    is BudgetsCardConfig -> config.copy(showComparison = show)
                    is PlannedPaymentsCardConfig -> config.copy(showComparison = show)
                    else -> config
                }
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

    fun resetCardsToDefault() {
        cardUpdateScope.launch {
            cardRepository.resetToDefault(cardScreenContext)
        }
    }
}

/**
 * ARC-33: reified-type ergonomic wrapper for [CardUpdateHandler.updateCardConfig].
 *
 * ```kotlin
 * updateCardConfig<NetWorthCardConfig>(cardId) { it.copy(showComparison = true) }
 * ```
 *
 * Inline + reified means `C::class.java` is resolved at the call site, so no Class<T>
 * parameter is required. Defined as a top-level extension to keep the interface
 * non-inline (interfaces can't have inline members).
 */
inline fun <reified C : CardConfig> CardUpdateHandler.updateCardConfig(
    cardId: String,
    noinline transform: (C) -> C
) = updateCardConfig(cardId, C::class.java, transform)
