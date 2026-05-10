package com.osscabinet.ossWallet.ui.components.cards

import androidx.compose.foundation.layout.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.osscabinet.ossWallet.domain.model.Account
import com.osscabinet.ossWallet.domain.model.Category
import com.osscabinet.ossWallet.domain.model.Filter
import com.osscabinet.ossWallet.domain.service.financial.CategoryTreeMetrics
import com.osscabinet.ossWallet.ui.analytics.components.CategoryPieChartCard
import com.osscabinet.ossWallet.ui.analytics.components.CategoryTreemapCard
import java.math.RoundingMode
import com.osscabinet.ossWallet.ui.dashboard.components.NetWorthCard
import com.osscabinet.ossWallet.ui.dashboard.components.CurrencyBalancesCard
import com.osscabinet.ossWallet.ui.dashboard.components.CashFlowCard
import com.osscabinet.ossWallet.ui.dashboard.components.AccountsCard
import com.osscabinet.ossWallet.ui.accountdetail.components.AccountHeroCard
import com.osscabinet.ossWallet.ui.analytics.components.CategoryPieChartData
import com.osscabinet.ossWallet.ui.analytics.components.CategoryTreemapData
import com.osscabinet.ossWallet.ui.dashboard.components.BudgetsCard
import com.osscabinet.ossWallet.ui.dashboard.components.LoansCard
import com.osscabinet.ossWallet.ui.dashboard.components.PlannedPaymentsCard
import com.osscabinet.ossWallet.ui.dashboard.components.RecentActivitiesCard

/**
 * Unified card renderer composable that handles all card types across all screens.
 *
 * ⚠️ TYPE SAFETY: This renderer uses the [CardWithCallbacks] sealed class to ensure
 * that the callbacks and render data are always correctly paired. This eliminates
 * the need for runtime casts and makes the renderer more robust.
 */
@Composable
fun UnifiedCardRenderer(
    cardWithCallbacks: CardWithCallbacks,
    modifier: Modifier = Modifier,
    headerModifier: Modifier = Modifier,
    showTitle: Boolean = false,
    currentTheme: String = "light",
    accounts: List<Account> = emptyList(),
    allCategories: List<Category> = emptyList(),
    savedFilters: List<Filter> = emptyList(),
) {
    when (cardWithCallbacks) {
        is CardWithCallbacks.NetWorth -> {
            val renderData = cardWithCallbacks.renderData
            val cb = cardWithCallbacks.callbacks
            
            NetWorthCard(
                currentMetrics = renderData.currentMetrics,
                baseCurrency = renderData.baseCurrency,
                precision = renderData.precision,
                cardId = renderData.config.cardId,
                fontSize = renderData.config.fontSize,
                showComparison = renderData.config.showComparison,
                filter = renderData.config.filter,
                onFontSizeChange = cb.onFontSizeChange,
                onShowComparisonChange = cb.onShowComparisonChange,
                onFilterChange = cb.onFilterChange,
                onDelete = { cb.onDelete() },
                onClickIncome = { filter -> cb.onClickIncome?.invoke(filter) },
                onClickExpense = { filter -> cb.onClickExpense?.invoke(filter) },
                modifier = modifier.fillMaxWidth(),
                headerModifier = headerModifier,
                filterRules = renderData.config.filterRules,
                baseFilter = cardWithCallbacks.baseFilter,
                accounts = accounts,
                allCategories = allCategories,
                savedFilters = savedFilters,
                currentTheme = currentTheme,
                previousMetrics = renderData.previousMetrics
            )
        }
        
        is CardWithCallbacks.CurrencyBalances -> {
            val renderData = cardWithCallbacks.renderData
            val cb = cardWithCallbacks.callbacks
            
            CurrencyBalancesCard(
                currencyTotals = renderData.items,
                baseCurrency = renderData.baseCurrency,
                modifier = modifier.fillMaxWidth(),
                headerModifier = headerModifier,
                precision = renderData.precision,
                layout = renderData.config.layout,
                showBaseConversion = renderData.config.showBaseConversion,
                filter = renderData.config.filter,
                cardId = renderData.config.cardId,
                onLayoutChange = cb.onLayoutChange,
                onShowBaseConversionChange = cb.onShowBaseConversionChange,
                onFilterChange = cb.onFilterChange,
                onDelete = { cb.onDelete() },
                onCurrencyClick = cb.onCurrencyClick ?: { _, _ -> },
                filterRules = renderData.config.filterRules,
                baseFilter = cardWithCallbacks.baseFilter,
                accounts = accounts,
                allCategories = allCategories,
                savedFilters = savedFilters
            )
        }
        
        is CardWithCallbacks.CashFlow -> {
            val renderData = cardWithCallbacks.renderData
            val cb = cardWithCallbacks.callbacks
            
            CashFlowCard(
                cashFlowResult = renderData.result,
                baseCurrency = renderData.baseCurrency,
                currentTheme = currentTheme,
                cardId = renderData.config.cardId,
                modifier = modifier.fillMaxWidth(),
                headerModifier = headerModifier,
                showTitle = showTitle,
                precision = renderData.precision,
                showBaseConversion = renderData.config.showBaseConversion,
                filter = renderData.config.filter,
                onShowBaseConversionChange = cb.onShowBaseConversionChange,
                onFilterChange = cb.onFilterChange,
                onDelete = { cb.onDelete() },
                onClickIncome = { filter -> cb.onClickIncome?.invoke(filter) },
                onClickExpense = { filter -> cb.onClickExpense?.invoke(filter) },
                filterRules = renderData.config.filterRules,
                baseFilter = cardWithCallbacks.baseFilter,
                accounts = accounts,
                allCategories = allCategories,
                savedFilters = savedFilters
            )
        }
        
        is CardWithCallbacks.CategoryPieChart -> {
            val renderData = cardWithCallbacks.renderData
            val cb = cardWithCallbacks.callbacks

            fun mapMetrics(metric: CategoryTreeMetrics): CategoryPieChartData = CategoryPieChartData(
                category = metric.category,
                categoryId = metric.categoryId,
                amount = metric.netBalance.setScale(renderData.precision, RoundingMode.HALF_UP),
                colorHex = metric.colorHex,
                percentage = metric.percentage,
                hasSubcategories = metric.hasSubcategories,
                subcategoryCount = metric.subcategoryData.size,
                subcategoryData = metric.subcategoryData.map { mapMetrics(it) }
            )

            val uiData = renderData.items.map { mapMetrics(it) }

            CategoryPieChartCard(
                config = renderData.config,
                data = uiData,
                modifier = modifier.fillMaxWidth(),
                headerModifier = headerModifier,
                onUpdate = { cb.onUpdate(it) },
                onDelete = { cb.onDelete() },
                onRecordsClick = { cb.onRecordsClick(it) },
                accounts = accounts,
                allCategories = allCategories,
                savedFilters = savedFilters,
                baseFilter = cardWithCallbacks.baseFilter,
                currentTheme = currentTheme,
                precision = renderData.precision
            )
        }
        
        is CardWithCallbacks.CategoryTreemap -> {
            val renderData = cardWithCallbacks.renderData
            val cb = cardWithCallbacks.callbacks

            fun mapMetrics(metric: CategoryTreeMetrics): CategoryTreemapData = CategoryTreemapData(
                category = metric.category,
                categoryId = metric.categoryId,
                amount = metric.netBalance.setScale(renderData.precision, RoundingMode.HALF_UP),
                colorHex = metric.colorHex,
                percentage = metric.percentage,
                hasSubcategories = metric.hasSubcategories,
                subcategoryCount = metric.subcategoryData.size,
                subcategoryData = metric.subcategoryData.map { mapMetrics(it) }
            )

            val uiData = renderData.items.map { mapMetrics(it) }

            CategoryTreemapCard(
                config = renderData.config,
                data = uiData,
                modifier = modifier.fillMaxWidth(),
                headerModifier = headerModifier,
                onUpdate = { cb.onUpdate(it) },
                onDelete = { cb.onDelete() },
                onRecordsClick = { cb.onRecordsClick(it) },
                accounts = accounts,
                allCategories = allCategories,
                savedFilters = savedFilters,
                filterRules = cardWithCallbacks.filterRules,
                baseFilter = cardWithCallbacks.baseFilter,
                currentTheme = currentTheme,
                precision = renderData.precision
            )
        }

        is CardWithCallbacks.AccountHero -> {
            val renderData = cardWithCallbacks.renderData
            val cb = cardWithCallbacks.callbacks
            
            AccountHeroCard(
                account = renderData.account,
                balance = renderData.balance,
                income = renderData.income,
                expense = renderData.expense,
                baseCurrency = renderData.baseCurrency,
                exchangeRates = renderData.exchangeRates,
                currencyBalances = renderData.currencyBalances,
                modifier = modifier.fillMaxWidth(),
                headerModifier = headerModifier,
                precision = renderData.precision,
                showCurrencyBadges = renderData.config.showCurrencyBadges,
                filter = renderData.config.filter,
                onShowCurrencyBadgesChange = { show -> cb.onShowCurrencyBadgesChange(renderData.config.cardId, show) },
                onFilterChange = { filter -> cb.onFilterChange(renderData.config.cardId, filter) },
                onClickIncome = { drillDown -> cb.onClickIncome?.invoke(drillDown) },
                onClickExpense = { drillDown -> cb.onClickExpense?.invoke(drillDown) },
                onClick = cb.onClick,
                onDelete = { cb.onDelete() },
                filterRules = renderData.config.filterRules,
                baseFilter = cardWithCallbacks.baseFilter,
                accounts = accounts,
                allCategories = allCategories,
                savedFilters = savedFilters,
                currentTheme = currentTheme
            )
        }
        
        is CardWithCallbacks.Accounts -> {
            val renderData = cardWithCallbacks.renderData
            val cb = cardWithCallbacks.callbacks

            AccountsCard(
                onManageClick = cb.onManageClick,
                onAccountClick = cb.onAccountClick,
                onRecordsClick = { cb.onRecordsClick(it) },
                onGridColumnsChange = { id, cols -> cb.onGridColumnsChange(id, cols) },
                accounts = renderData.updatedAccounts,
                accountStats = renderData.accountStats,
                baseCurrency = renderData.baseCurrency,
                currentTheme = currentTheme,
                modifier = modifier.fillMaxWidth(),
                gridColumns = renderData.config.gridColumns,
                precision = renderData.precision,
                layout = renderData.config.layout,
                onLayoutChange = { id, layout -> cb.onLayoutChange(id, layout) },
                filter = renderData.config.filter,
                onFilterChange = { id, filter -> cb.onFilterChange(id, filter) },
                showAddAccountCard = renderData.config.showAddAccountCard,
                onShowAddAccountCardChange = { id, show -> cb.onShowAddAccountCardChange(id, show) },
                cardId = renderData.config.cardId,
                headerModifier = headerModifier.fillMaxWidth(),
                onDelete = { cb.onDelete() },
                filterRules = renderData.config.filterRules,
                baseFilter = cardWithCallbacks.baseFilter,
                allCategories = allCategories,
                savedFilters = savedFilters
            )
        }

        is CardWithCallbacks.PlannedPayments -> {
            val renderData = cardWithCallbacks.renderData
            val cb = cardWithCallbacks.callbacks

            PlannedPaymentsCard(
                upcoming = renderData.upcoming,
                overdue = renderData.overdue,
                config = renderData.config,
                onUpdate = cb.onUpdate,
                onItemClick = cb.onItemClick,
                onConfirmPayment = cb.onConfirm?.let { onConfirm -> { payment -> onConfirm(payment.id) } },
                onSkipPayment = cb.onSkip?.let { onSkip -> { payment -> onSkip(payment.id) } },
                onDelete = { cb.onDelete() },
                modifier = modifier.fillMaxWidth(),
                headerModifier = headerModifier,
                precision = renderData.precision
            )
        }

        is CardWithCallbacks.Budgets -> {
            val renderData = cardWithCallbacks.renderData
            val cb = cardWithCallbacks.callbacks

            BudgetsCard(
                items = renderData.items,
                config = renderData.config,
                onUpdate = cb.onUpdate,
                onBudgetClick = cb.onBudgetClick,
                onDelete = { cb.onDelete() },
                modifier = modifier.fillMaxWidth(),
                headerModifier = headerModifier,
                precision = renderData.precision
            )
        }

        is CardWithCallbacks.Loans -> {
            val renderData = cardWithCallbacks.renderData
            val cb = cardWithCallbacks.callbacks

            LoansCard(
                items = renderData.items,
                config = renderData.config,
                onUpdate = cb.onUpdate,
                onLoanClick = cb.onLoanClick,
                onDelete = { cb.onDelete() },
                modifier = modifier.fillMaxWidth(),
                headerModifier = headerModifier,
                precision = renderData.precision
            )
        }

        is CardWithCallbacks.RecentActivities -> {
            val renderData = cardWithCallbacks.renderData
            val cb = cardWithCallbacks.callbacks

            RecentActivitiesCard(
                transactions = renderData.transactions,
                accountNamesMap = renderData.accountNamesMap,
                categoryMap = renderData.categoryMap,
                config = renderData.config,
                onUpdate = cb.onUpdate,
                onTransactionClick = cb.onTransactionClick,
                onRecordsClick = cb.onRecordsClick,
                onDelete = { cb.onDelete() },
                modifier = modifier.fillMaxWidth(),
                headerModifier = headerModifier,
                precision = renderData.precision,
                baseCurrency = renderData.baseCurrency,
                exchangeRates = renderData.exchangeRates,
                onItemExpanded = cb.onItemExpanded,
                onItemCollapsed = cb.onItemCollapsed,
                onStatusChange = cb.onStatusChange,
                onClone = cb.onClone,
                onDeleteTransaction = cb.onDeleteTransaction,
                onSplit = cb.onSplit,
                onFullSwipeRight = cb.onFullSwipeRight,
                accounts = accounts,
                allCategories = allCategories,
                savedFilters = savedFilters
            )
        }

        is CardWithCallbacks.ComputeError -> {
            // ComputeError is rendered by AeonCardList before reaching UnifiedCardRenderer.
            // This branch is a compile-required safety net.
        }
    }
}
