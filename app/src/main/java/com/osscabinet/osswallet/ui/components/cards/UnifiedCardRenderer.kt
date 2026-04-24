package com.osscabinet.ossWallet.ui.components.cards

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.osscabinet.ossWallet.domain.model.Account
import com.osscabinet.ossWallet.ui.analytics.components.CategoryBreakdownCard
import com.osscabinet.ossWallet.ui.analytics.components.CategoryExpenseTreemapCard
import com.osscabinet.ossWallet.ui.analytics.components.TimelineTrajectoryCard
import com.osscabinet.ossWallet.ui.analytics.components.IncomeExpensesSummaryCard
import com.osscabinet.ossWallet.ui.analytics.components.PeriodComparisonCard
import com.osscabinet.ossWallet.ui.analytics.components.CashFlowTrendCard
import com.osscabinet.ossWallet.ui.analytics.components.CashFlowProgressCard
import com.osscabinet.ossWallet.ui.analytics.components.BalanceForecastCard
import com.osscabinet.ossWallet.ui.analytics.components.PlannedPaymentsCard
import com.osscabinet.ossWallet.ui.dashboard.components.NetWorthCard
import com.osscabinet.ossWallet.ui.dashboard.components.CurrencyBalancesCard
import com.osscabinet.ossWallet.ui.dashboard.components.CashFlowCard
import com.osscabinet.ossWallet.ui.dashboard.components.AccountsCard
import com.osscabinet.ossWallet.ui.dashboard.components.BudgetsCard
import com.osscabinet.ossWallet.ui.accountdetail.components.AccountHeroCard
import com.osscabinet.ossWallet.domain.model.timeline.TimelineRange

/**
 * Unified card renderer composable that handles all card types across all screens.
 * 
 * This composable provides a single entry point for rendering any card type by using
 * sealed classes for type-safe dispatch. The renderer separates data fetching (done in
 * the screen) from rendering logic (done here).
 * 
 * @param renderData The data required for the card, wrapped in a sealed class
 * @param callbacks The callbacks for the card, wrapped in a sealed class
 * @param modifier Modifier for the card container
 * @param headerModifier Modifier for the card header (for draggable headers)
 * @param showTitle Whether to show the card title (for cards that support it)
 */
@Composable
fun UnifiedCardRenderer(
    renderData: CardRenderData,
    callbacks: CardCallbacks,
    modifier: Modifier = Modifier,
    headerModifier: Modifier = Modifier,
    showTitle: Boolean = false,
    currentTheme: String = "light"
) {
    when (renderData) {
        is CardRenderData.NetWorth -> {
            val cb = callbacks as? CardCallbacks.WithFontSizeAndTimeline 
                ?: CardCallbacks.WithFontSizeAndTimeline(
                    onFontSizeChange = { _, _ -> },
                    onTimelineRangeChange = { _, _ -> },
                    onDelete = {}
                )
            
            NetWorthCard(
                uiState = renderData.uiState,
                cardId = renderData.config.cardId,
                fontSize = renderData.config.fontSize,
                timelineRange = renderData.config.timelineRange,
                onFontSizeChange = cb.onFontSizeChange,
                onTimelineRangeChange = cb.onTimelineRangeChange,
                onDelete = { cb.onDelete() },
                onClickIncome = {},
                onClickExpense = {},
                modifier = modifier.fillMaxWidth(),
                headerModifier = headerModifier
            )
        }
        
        is CardRenderData.CurrencyBalances -> {
            val cb = callbacks as? CardCallbacks.CurrencyBalances
                ?: CardCallbacks.CurrencyBalances(
                    onLayoutChange = { _, _ -> },
                    onShowBaseConversionChange = { _, _ -> },
                    onTimelineRangeChange = { _, _ -> },
                    onCurrencyClick = null,
                    onDelete = {}
                )
            
            CurrencyBalancesCard(
                currencyTotals = renderData.currencyTotals,
                baseCurrency = renderData.baseCurrency,
                modifier = modifier.fillMaxWidth(),
                headerModifier = headerModifier,
                precision = renderData.precision,
                layout = renderData.config.layout,
                showBaseConversion = renderData.config.showBaseConversion,
                timelineRange = renderData.config.timelineRange,
                cardId = renderData.config.cardId,
                onLayoutChange = cb.onLayoutChange,
                onShowBaseConversionChange = cb.onShowBaseConversionChange,
                onTimelineRangeChange = cb.onTimelineRangeChange,
                onDelete = { cb.onDelete() },
                onCurrencyClick = cb.onCurrencyClick ?: { _, _ -> }
            )
        }
        
        is CardRenderData.CashFlow -> {
            val cb = callbacks as? CardCallbacks.CashFlow
                ?: CardCallbacks.CashFlow(
                    onShowBaseConversionChange = { _, _ -> },
                    onTimelineRangeChange = { _, _ -> },
                    onClickIncome = {},
                    onClickExpense = {},
                    onDelete = {}
                )
            
            CashFlowCard(
                cashFlowResult = renderData.cashFlowResult,
                baseCurrency = renderData.baseCurrency,
                currentTheme = "light",
                cardId = renderData.config.cardId,
                modifier = modifier.fillMaxWidth(),
                headerModifier = headerModifier,
                showTitle = showTitle,
                precision = renderData.precision,
                showBaseConversion = renderData.config.showBaseConversion,
                timelineRange = renderData.config.timelineRange,
                onShowBaseConversionChange = cb.onShowBaseConversionChange,
                onTimelineRangeChange = cb.onTimelineRangeChange,
                onDelete = { cb.onDelete() },
                onClickIncome = cb.onClickIncome,
                onClickExpense = cb.onClickExpense
            )
        }
        
        is CardRenderData.CategoryBreakdown -> {
            val cb = callbacks as? CardCallbacks.AnalyticsWithTimeline
                ?: CardCallbacks.AnalyticsWithTimeline(
                    onTimelineRangeChange = { _, _ -> },
                    onDelete = {}
                )
            
            CategoryBreakdownCard(
                config = renderData.config,
                data = renderData.data,
                modifier = modifier.fillMaxWidth(),
                headerModifier = headerModifier,
                onUpdate = { cb.onUpdate(it) },
                onDelete = { cb.onDelete() }
            )
        }
        
        is CardRenderData.TimelineTrajectory -> {
            val cb = callbacks as? CardCallbacks.AnalyticsWithTimeline
                ?: CardCallbacks.AnalyticsWithTimeline(
                    onTimelineRangeChange = { _, _ -> },
                    onDelete = {}
                )
            
            TimelineTrajectoryCard(
                config = renderData.config,
                data = renderData.data,
                modifier = modifier.fillMaxWidth(),
                headerModifier = headerModifier,
                onUpdate = { cb.onUpdate(it) },
                onDelete = { cb.onDelete() }
            )
        }
        
        is CardRenderData.CategoryExpenseTreemap -> {
            val cb = callbacks as? CardCallbacks.AnalyticsWithTimeline
                ?: CardCallbacks.AnalyticsWithTimeline(
                    onTimelineRangeChange = { _, _ -> },
                    onDelete = {}
                )
            
            CategoryExpenseTreemapCard(
                config = renderData.config,
                data = renderData.data,
                modifier = modifier.fillMaxWidth(),
                headerModifier = headerModifier,
                onUpdate = { cb.onUpdate(it) },
                onDelete = { cb.onDelete() }
            )
        }
        
        is CardRenderData.IncomeExpensesSummary -> {
            val cb = callbacks as? CardCallbacks.AnalyticsWithTimeline
                ?: CardCallbacks.AnalyticsWithTimeline(
                    onTimelineRangeChange = { _, _ -> },
                    onDelete = {}
                )
            
            IncomeExpensesSummaryCard(
                config = renderData.config,
                data = renderData.data,
                modifier = modifier.fillMaxWidth(),
                onUpdate = { cb.onUpdate(it) },
                onDelete = { cb.onDelete() }
            )
        }
        
        is CardRenderData.PeriodComparison -> {
            val cb = callbacks as? CardCallbacks.AnalyticsWithTimeline
                ?: CardCallbacks.AnalyticsWithTimeline(
                    onTimelineRangeChange = { _, _ -> },
                    onDelete = {}
                )
            
            PeriodComparisonCard(
                config = renderData.config,
                data = renderData.data,
                modifier = modifier.fillMaxWidth(),
                onUpdate = { cb.onUpdate(it) },
                onDelete = { cb.onDelete() }
            )
        }
        
        is CardRenderData.CashFlowTrend -> {
            val cb = callbacks as? CardCallbacks.AnalyticsWithTimeline
                ?: CardCallbacks.AnalyticsWithTimeline(
                    onTimelineRangeChange = { _, _ -> },
                    onDelete = {}
                )
            
            CashFlowTrendCard(
                config = renderData.config,
                data = renderData.data,
                modifier = modifier.fillMaxWidth(),
                onUpdate = { cb.onUpdate(it) },
                onDelete = { cb.onDelete() }
            )
        }
        
        is CardRenderData.CashFlowProgress -> {
            val cb = callbacks as? CardCallbacks.AnalyticsWithTimeline
                ?: CardCallbacks.AnalyticsWithTimeline(
                    onTimelineRangeChange = { _, _ -> },
                    onDelete = {}
                )
            
            CashFlowProgressCard(
                config = renderData.config,
                data = renderData.data,
                modifier = modifier.fillMaxWidth(),
                onUpdate = { cb.onUpdate(it) },
                onDelete = { cb.onDelete() }
            )
        }
        
        is CardRenderData.BalanceForecast -> {
            val cb = callbacks as? CardCallbacks.Analytics
                ?: CardCallbacks.Analytics(onDelete = {})
            
            BalanceForecastCard(
                config = renderData.config,
                data = renderData.data,
                modifier = modifier.fillMaxWidth(),
                onUpdate = { cb.onUpdate(it) },
                onDelete = { cb.onDelete() }
            )
        }
        
        is CardRenderData.PlannedPayments -> {
            val cb = callbacks as? CardCallbacks.AnalyticsWithTimeline
                ?: CardCallbacks.AnalyticsWithTimeline(
                    onTimelineRangeChange = { _, _ -> },
                    onDelete = {}
                )
            
            PlannedPaymentsCard(
                config = renderData.config,
                data = renderData.data,
                modifier = modifier.fillMaxWidth(),
                onUpdate = { cb.onUpdate(it) },
                onDelete = { cb.onDelete() }
            )
        }
        
        is CardRenderData.AccountHero -> {
            val cb = callbacks as? CardCallbacks.AccountHero
                ?: CardCallbacks.AccountHero(
                    onShowCurrencyBadgesChange = { _, _ -> },
                    onClick = null,
                    onDelete = {}
                )
            
            AccountHeroCard(
                account = renderData.account,
                balance = renderData.balance,
                netBalance = renderData.netBalance,
                baseCurrency = renderData.baseCurrency,
                exchangeRates = renderData.exchangeRates,
                modifier = modifier.fillMaxWidth(),
                precision = renderData.precision,
                showCurrencyBadges = (renderData.config as? com.osscabinet.ossWallet.domain.model.settings.AccountHeroCardConfig)?.showCurrencyBadges ?: true,
                onShowCurrencyBadgesChange = { show -> cb.onShowCurrencyBadgesChange(renderData.config.cardId, show) },
                onClick = cb.onClick,
                onDelete = { cb.onDelete() }
            )
        }
        
        is CardRenderData.Accounts -> {
            val cb = callbacks as? CardCallbacks.Accounts
                ?: CardCallbacks.Accounts(
                    onGridColumnsChange = { _, _ -> },
                    onLayoutChange = { _, _ -> },
                    onTimelineRangeChange = { _, _ -> },
                    onShowAddAccountCardChange = { _, _ -> },
                    onDelete = {}
                )
            
            AccountsCard(
                onManageClick = cb.onManageClick,
                onHeaderClick = cb.onHeaderClick,
                onAccountClick = cb.onAccountClick,
                onRecordsClick = { cb.onRecordsClick(it) },
                onGridColumnsChange = { _, _ -> cb.onGridColumnsChange(renderData.config.cardId, renderData.config.gridColumns) },
                accounts = renderData.accounts,
                accountStats = renderData.accountStats,
                baseCurrency = renderData.baseCurrency,
                currentTheme = renderData.currentTheme,
                modifier = modifier.fillMaxWidth(),
                gridColumns = renderData.config.gridColumns,
                precision = renderData.precision,
                layout = renderData.config.layout,
                onLayoutChange = { _, _ -> cb.onLayoutChange(renderData.config.cardId, renderData.config.layout) },
                timelineRange = renderData.config.timelineRange,
                onTimelineRangeChange = { _, _ -> cb.onTimelineRangeChange(renderData.config.cardId, renderData.config.timelineRange) },
                showAddAccountCard = renderData.config.showAddAccountCard,
                onShowAddAccountCardChange = { _, _ -> cb.onShowAddAccountCardChange(renderData.config.cardId, renderData.config.showAddAccountCard) },
                cardId = renderData.config.cardId,
                headerModifier = headerModifier.fillMaxWidth(),
                settingsTitle = "Settings",
                onDelete = { cb.onDelete() }
            )
        }
        
        is CardRenderData.Budgets -> {
            val cb = callbacks as? CardCallbacks.Budgets
                ?: CardCallbacks.Budgets(
                    onShowProgressLabelsChange = { _, _ -> },
                    onDelete = {}
                )
            
            BudgetsCard(
                onManageClick = cb.onManageClick,
                onBudgetClick = cb.onBudgetClick,
                onRecordsClick = { cb.onRecordsClick(it) },
                budgets = renderData.budgets,
                baseCurrency = renderData.baseCurrency,
                modifier = modifier.fillMaxWidth(),
                precision = renderData.precision,
                headerModifier = headerModifier.fillMaxWidth(),
                showProgressLabels = renderData.config.showProgressLabels,
                timelineRange = com.osscabinet.ossWallet.domain.model.timeline.TimelineRange.AllTime,
                cardId = renderData.config.cardId,
                onShowProgressLabelsChange = { _, _ -> cb.onShowProgressLabelsChange(renderData.config.cardId, renderData.config.showProgressLabels) },
                onDelete = { cb.onDelete() }
            )
        }
        
        is CardRenderData.IncomeExpensesBook -> {
            val cb = callbacks as? CardCallbacks.AnalyticsWithTimeline
                ?: CardCallbacks.AnalyticsWithTimeline(
                    onTimelineRangeChange = { _, _ -> },
                    onDelete = {}
                )
            
            // Placeholder - would need to create IncomeExpensesBookCard component
            Box(modifier = modifier.fillMaxWidth()) {
                Text("Income & Expenses Book Card - Component not yet implemented")
            }
        }
        
        is CardRenderData.CashFlowTable -> {
            val cb = callbacks as? CardCallbacks.AnalyticsWithTimeline
                ?: CardCallbacks.AnalyticsWithTimeline(
                    onTimelineRangeChange = { _, _ -> },
                    onDelete = {}
                )
            
            // Placeholder - would need to create CashFlowTableCard component
            Box(modifier = modifier.fillMaxWidth()) {
                Text("Cash Flow Table Card - Component not yet implemented")
            }
        }
        
        is CardRenderData.PlannedPaymentsTimeline -> {
            val cb = callbacks as? CardCallbacks.AnalyticsWithTimeline
                ?: CardCallbacks.AnalyticsWithTimeline(
                    onTimelineRangeChange = { _, _ -> },
                    onDelete = {}
                )
            
            // Placeholder - would need to create PlannedPaymentsTimelineCard component
            Box(modifier = modifier.fillMaxWidth()) {
                Text("Planned Payments Timeline Card - Component not yet implemented")
            }
        }
        
        is CardRenderData.BalanceByAccounts -> {
            val cb = callbacks as? CardCallbacks.AnalyticsWithTimeline
                ?: CardCallbacks.AnalyticsWithTimeline(
                    onTimelineRangeChange = { _, _ -> },
                    onDelete = {}
                )
            
            // Placeholder - would need to create BalanceByAccountsCard component
            Box(modifier = modifier.fillMaxWidth()) {
                Text("Balance By Accounts Card - Component not yet implemented")
            }
        }
        
        is CardRenderData.BalanceTrend -> {
            val cb = callbacks as? CardCallbacks.AnalyticsWithTimeline
                ?: CardCallbacks.AnalyticsWithTimeline(
                    onTimelineRangeChange = { _, _ -> },
                    onDelete = {}
                )
            
            // Placeholder - would need to create BalanceTrendCard component
            Box(modifier = modifier.fillMaxWidth()) {
                Text("Balance Trend Card - Component not yet implemented")
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun UnifiedCardRendererPreview() {
    // Preview would require mock data for all card types
    // This is a placeholder to show the structure
}

/**
 * Data class that pairs card render data with its callbacks
 */
data class CardWithCallbacks(
    val renderData: CardRenderData,
    val callbacks: CardCallbacks
)

/**
 * Renders a list of cards using the unified renderer.
 * 
 * This composable iterates through a list of CardWithCallbacks and renders each card
 * using the UnifiedCardRenderer. This allows screens to pass a list of all possible
 * cards they support, with their corresponding callbacks.
 * 
 * @param cards List of cards to render, each with its own data and callbacks
 * @param modifier Modifier for each card
 * @param headerModifier Modifier for each card header (for draggable headers)
 * @param showTitle Whether to show the card title (for cards that support it)
 */
@Composable
fun UnifiedCardListRenderer(
    cards: List<CardWithCallbacks>,
    modifier: Modifier = Modifier,
    headerModifier: Modifier = Modifier,
    showTitle: Boolean = false
) {
    cards.forEach { card ->
        UnifiedCardRenderer(
            renderData = card.renderData,
            callbacks = card.callbacks,
            modifier = modifier,
            headerModifier = headerModifier,
            showTitle = showTitle
        )
    }
}
