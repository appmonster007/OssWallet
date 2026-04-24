package com.osscabinet.ossWallet.ui.components.cards

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
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
                cardId = renderData.cardId,
                fontSize = renderData.fontSize,
                timelineRange = renderData.timelineRange,
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
                layout = renderData.layout,
                showBaseConversion = renderData.showBaseConversion,
                timelineRange = renderData.timelineRange,
                cardId = renderData.cardId,
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
                cardId = renderData.cardId,
                modifier = modifier.fillMaxWidth(),
                headerModifier = headerModifier,
                showTitle = showTitle,
                precision = renderData.precision,
                showBaseConversion = renderData.showBaseConversion,
                timelineRange = renderData.timelineRange,
                onShowBaseConversionChange = cb.onShowBaseConversionChange,
                onTimelineRangeChange = cb.onTimelineRangeChange,
                onDelete = { cb.onDelete() },
                onClickIncome = cb.onClickIncome,
                onClickExpense = cb.onClickExpense
            )
        }
        
        is CardRenderData.CategoryBreakdown -> {
            val cb = callbacks as? CardCallbacks.Analytics
                ?: CardCallbacks.Analytics(onDelete = {})
            
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
            val cb = callbacks as? CardCallbacks.Analytics
                ?: CardCallbacks.Analytics(onDelete = {})
            
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
            val cb = callbacks as? CardCallbacks.Analytics
                ?: CardCallbacks.Analytics(onDelete = {})
            
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
            val cb = callbacks as? CardCallbacks.Analytics
                ?: CardCallbacks.Analytics(onDelete = {})
            
            IncomeExpensesSummaryCard(
                config = renderData.config,
                data = renderData.data,
                modifier = modifier.fillMaxWidth(),
                onUpdate = { cb.onUpdate(it) },
                onDelete = { cb.onDelete() }
            )
        }
        
        is CardRenderData.PeriodComparison -> {
            val cb = callbacks as? CardCallbacks.Analytics
                ?: CardCallbacks.Analytics(onDelete = {})
            
            PeriodComparisonCard(
                config = renderData.config,
                data = renderData.data,
                modifier = modifier.fillMaxWidth(),
                onUpdate = { cb.onUpdate(it) },
                onDelete = { cb.onDelete() }
            )
        }
        
        is CardRenderData.CashFlowTrend -> {
            val cb = callbacks as? CardCallbacks.Analytics
                ?: CardCallbacks.Analytics(onDelete = {})
            
            CashFlowTrendCard(
                config = renderData.config,
                data = renderData.data,
                modifier = modifier.fillMaxWidth(),
                onUpdate = { cb.onUpdate(it) },
                onDelete = { cb.onDelete() }
            )
        }
        
        is CardRenderData.CashFlowProgress -> {
            val cb = callbacks as? CardCallbacks.Analytics
                ?: CardCallbacks.Analytics(onDelete = {})
            
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
            val cb = callbacks as? CardCallbacks.Analytics
                ?: CardCallbacks.Analytics(onDelete = {})
            
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
                    onShowCurrencyBadgesChange = {},
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
                showCurrencyBadges = renderData.config.showCurrencyBadges,
                onShowCurrencyBadgesChange = cb.onShowCurrencyBadgesChange,
                onClick = cb.onClick,
                onDelete = { cb.onDelete() }
            )
        }
        
        is CardRenderData.AccountCashFlow -> {
            val cb = callbacks as? CardCallbacks.AccountCashFlow
                ?: CardCallbacks.AccountCashFlow(
                    onShowBaseConversionChange = {},
                    onTimelineRangeChange = {},
                    onClickIncome = null,
                    onClickExpense = null,
                    onDelete = {}
                )
            
            CashFlowCard(
                cashFlowResult = renderData.cashFlowResult,
                baseCurrency = renderData.baseCurrency,
                currentTheme = currentTheme,
                cardId = renderData.config.cardId,
                modifier = modifier.fillMaxWidth(),
                headerModifier = headerModifier,
                showTitle = showTitle,
                precision = renderData.precision,
                showBaseConversion = renderData.config.showBaseConversion,
                timelineRange = renderData.config.timelineRange,
                onShowBaseConversionChange = { _, _ -> cb.onShowBaseConversionChange(renderData.config.showBaseConversion) },
                onTimelineRangeChange = { _, _ -> cb.onTimelineRangeChange(renderData.config.timelineRange) },
                onDelete = { cb.onDelete() },
                onClickIncome = { cb.onClickIncome?.invoke() },
                onClickExpense = { cb.onClickExpense?.invoke() }
            )
        }
        
        is CardRenderData.AccountCurrencyBalances -> {
            val cb = callbacks as? CardCallbacks.AccountCurrencyBalances
                ?: CardCallbacks.AccountCurrencyBalances(
                    onLayoutChange = {},
                    onShowBaseConversionChange = {},
                    onTimelineRangeChange = {},
                    onCurrencyClick = { _, _ -> },
                    onDelete = {}
                )
            
            CurrencyBalancesCard(
                currencyTotals = renderData.currencyTotals,
                baseCurrency = renderData.baseCurrency,
                cardId = renderData.config.cardId,
                modifier = modifier.fillMaxWidth(),
                headerModifier = headerModifier,
                precision = renderData.precision,
                layout = renderData.config.layout,
                showBaseConversion = renderData.config.showBaseConversion,
                timelineRange = renderData.config.timelineRange,
                onLayoutChange = { _, _ -> cb.onLayoutChange(renderData.config.layout) },
                onShowBaseConversionChange = { _, _ -> cb.onShowBaseConversionChange(renderData.config.showBaseConversion) },
                onTimelineRangeChange = { _, _ -> cb.onTimelineRangeChange(renderData.config.timelineRange) },
                onDelete = { cb.onDelete() },
                onCurrencyClick = cb.onCurrencyClick
            )
        }
        
        is CardRenderData.Accounts -> {
            val cb = callbacks as? CardCallbacks.Accounts
                ?: CardCallbacks.Accounts(
                    onGridColumnsChange = {},
                    onLayoutChange = {},
                    onTimelineRangeChange = {},
                    onShowAddAccountCardChange = {},
                    onDelete = {}
                )
            
            AccountsCard(
                onManageClick = cb.onManageClick,
                onHeaderClick = cb.onHeaderClick,
                onAccountClick = cb.onAccountClick,
                onRecordsClick = { cb.onRecordsClick(it) },
                onGridColumnsChange = { _, _ -> cb.onGridColumnsChange(renderData.config.gridColumns) },
                accounts = renderData.accounts,
                accountStats = renderData.accountStats,
                baseCurrency = renderData.baseCurrency,
                currentTheme = renderData.currentTheme,
                modifier = modifier.fillMaxWidth(),
                gridColumns = renderData.config.gridColumns,
                precision = renderData.precision,
                layout = renderData.config.layout,
                onLayoutChange = { _, _ -> cb.onLayoutChange(renderData.config.layout) },
                timelineRange = renderData.config.timelineRange,
                onTimelineRangeChange = { _, _ -> cb.onTimelineRangeChange(renderData.config.timelineRange) },
                showAddAccountCard = renderData.config.showAddAccountCard,
                onShowAddAccountCardChange = { _, _ -> cb.onShowAddAccountCardChange(renderData.config.showAddAccountCard) },
                cardId = renderData.config.cardId,
                headerModifier = headerModifier.fillMaxWidth(),
                settingsTitle = "Dashboard Settings",
                onDelete = { cb.onDelete() }
            )
        }
        
        is CardRenderData.Budgets -> {
            val cb = callbacks as? CardCallbacks.Budgets
                ?: CardCallbacks.Budgets(
                    onShowProgressLabelsChange = {},
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
                onShowProgressLabelsChange = { _, _ -> cb.onShowProgressLabelsChange(renderData.config.showProgressLabels) },
                onDelete = { cb.onDelete() }
            )
        }
        
        is CardRenderData.AccountsViewNetWorth -> {
            val cb = callbacks as? CardCallbacks.AccountsViewNetWorth
                ?: CardCallbacks.AccountsViewNetWorth(
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
                onClickIncome = { cb.onClickIncome?.invoke(it) },
                onClickExpense = { cb.onClickExpense?.invoke(it) },
                modifier = modifier.fillMaxWidth(),
                headerModifier = headerModifier
            )
        }
        
        is CardRenderData.AccountsViewCurrencyBalances -> {
            val cb = callbacks as? CardCallbacks.AccountsViewCurrencyBalances
                ?: CardCallbacks.AccountsViewCurrencyBalances(
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
        
        is CardRenderData.AccountsViewCashFlow -> {
            val cb = callbacks as? CardCallbacks.AccountsViewCashFlow
                ?: CardCallbacks.AccountsViewCashFlow(
                    onShowBaseConversionChange = { _, _ -> },
                    onTimelineRangeChange = { _, _ -> },
                    onClickIncome = null,
                    onClickExpense = null,
                    onDelete = {}
                )
            
            CashFlowCard(
                cashFlowResult = renderData.cashFlowResult,
                baseCurrency = renderData.baseCurrency,
                currentTheme = currentTheme,
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
                onClickIncome = { cb.onClickIncome?.invoke(it) },
                onClickExpense = { cb.onClickExpense?.invoke(it) }
            )
        }
        
        is CardRenderData.AccountsViewAccountsList -> {
            val cb = callbacks as? CardCallbacks.AccountsViewAccountsList
                ?: CardCallbacks.AccountsViewAccountsList(
                    onGridColumnsChange = { _, _ -> },
                    onLayoutChange = { _, _ -> },
                    onTimelineRangeChange = { _, _ -> },
                    onDelete = {}
                )
            
            AccountsCard(
                onManageClick = cb.onManageClick ?: {},
                onHeaderClick = cb.onHeaderClick ?: {},
                onAccountClick = cb.onAccountClick ?: {},
                onRecordsClick = { cb.onRecordsClick?.invoke(it) },
                onGridColumnsChange = cb.onGridColumnsChange,
                accounts = renderData.accounts,
                accountStats = renderData.accountStats,
                baseCurrency = renderData.baseCurrency,
                currentTheme = renderData.currentTheme,
                modifier = modifier.fillMaxWidth(),
                gridColumns = renderData.config.gridColumns,
                precision = renderData.precision,
                layout = renderData.config.layout,
                onLayoutChange = cb.onLayoutChange,
                timelineRange = renderData.config.timelineRange,
                onTimelineRangeChange = cb.onTimelineRangeChange,
                showAddAccountCard = false,
                onShowAddAccountCardChange = { _, _ -> },
                cardId = renderData.config.cardId,
                headerModifier = headerModifier.fillMaxWidth(),
                settingsTitle = "Accounts View Settings",
                onDelete = { cb.onDelete() }
            )
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
