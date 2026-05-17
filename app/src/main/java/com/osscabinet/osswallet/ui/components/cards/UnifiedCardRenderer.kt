package com.osscabinet.ossWallet.ui.components.cards

import androidx.compose.foundation.layout.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.osscabinet.ossWallet.domain.service.financial.CategoryTreeMetrics
import com.osscabinet.ossWallet.ui.screens.analytics.components.CategoryPieChartCard
import com.osscabinet.ossWallet.ui.screens.analytics.components.CategoryTreemapCard
import java.math.RoundingMode
import com.osscabinet.ossWallet.ui.screens.dashboard.components.NetWorthCard
import com.osscabinet.ossWallet.ui.screens.dashboard.components.CurrencyBalancesCard
import com.osscabinet.ossWallet.ui.screens.dashboard.components.CashFlowCard
import com.osscabinet.ossWallet.ui.screens.dashboard.components.AccountsCard
import com.osscabinet.ossWallet.ui.screens.accounts.detail.components.AccountHeroCard
import com.osscabinet.ossWallet.ui.screens.analytics.components.CategoryPieChartData
import com.osscabinet.ossWallet.ui.screens.analytics.components.CategoryTreemapData
import com.osscabinet.ossWallet.ui.screens.dashboard.components.BudgetsCard
import com.osscabinet.ossWallet.ui.screens.dashboard.components.LoansCard
import com.osscabinet.ossWallet.ui.screens.dashboard.components.PlannedPaymentsCard
import com.osscabinet.ossWallet.ui.screens.dashboard.components.RecentActivitiesCard

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
    currentTheme: String = "light",
    filterContext: FilterSelectionContext = FilterSelectionContext(),
) {
    when (cardWithCallbacks) {
        is CardWithCallbacks.NetWorth -> {
            val renderData = cardWithCallbacks.renderData
            val cb = cardWithCallbacks.callbacks

            NetWorthCard(
                currentMetrics = renderData.currentMetrics,
                baseCurrency = renderData.baseCurrency,
                config = renderData.config,
                previousMetrics = renderData.previousMetrics,
                callbacks = cb,
                filterContext = filterContext,
                baseFilter = cardWithCallbacks.baseFilter,
                modifier = modifier.fillMaxWidth(),
                headerModifier = headerModifier,
            )
        }

        is CardWithCallbacks.CurrencyBalances -> {
            val renderData = cardWithCallbacks.renderData
            val cb = cardWithCallbacks.callbacks

            CurrencyBalancesCard(
                currencyTotals = renderData.items,
                baseCurrency = renderData.baseCurrency,
                config = renderData.config,
                callbacks = cb,
                filterContext = filterContext,
                baseFilter = cardWithCallbacks.baseFilter,
                modifier = modifier.fillMaxWidth(),
                headerModifier = headerModifier,
            )
        }

        is CardWithCallbacks.CashFlow -> {
            val renderData = cardWithCallbacks.renderData
            val cb = cardWithCallbacks.callbacks

            CashFlowCard(
                cashFlowResult = renderData.result,
                baseCurrency = renderData.baseCurrency,
                config = renderData.config,
                callbacks = cb,
                filterContext = filterContext,
                baseFilter = cardWithCallbacks.baseFilter,
                modifier = modifier.fillMaxWidth(),
                headerModifier = headerModifier,
            )
        }

        is CardWithCallbacks.CategoryPieChart -> {
            val renderData = cardWithCallbacks.renderData
            val cb = cardWithCallbacks.callbacks

            fun mapMetrics(metric: CategoryTreeMetrics): CategoryPieChartData = CategoryPieChartData(
                category = metric.category,
                categoryId = metric.categoryId,
                amount = metric.netBalance,
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
                callbacks = cb,
                filterContext = filterContext,
                baseFilter = cardWithCallbacks.baseFilter,
                currentTheme = currentTheme,
                modifier = modifier.fillMaxWidth(),
                headerModifier = headerModifier,
            )
        }

        is CardWithCallbacks.CategoryTreemap -> {
            val renderData = cardWithCallbacks.renderData
            val cb = cardWithCallbacks.callbacks

            fun mapMetrics(metric: CategoryTreeMetrics): CategoryTreemapData = CategoryTreemapData(
                category = metric.category,
                categoryId = metric.categoryId,
                amount = metric.netBalance,
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
                callbacks = cb,
                filterContext = filterContext,
                baseFilter = cardWithCallbacks.baseFilter,
                currentTheme = currentTheme,
                modifier = modifier.fillMaxWidth(),
                headerModifier = headerModifier,
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
                config = renderData.config,
                currencyBalances = renderData.currencyBalances,
                callbacks = cb,
                modifier = modifier.fillMaxWidth(),
                headerModifier = headerModifier,
            )
        }

        is CardWithCallbacks.Accounts -> {
            val renderData = cardWithCallbacks.renderData
            val cb = cardWithCallbacks.callbacks

            AccountsCard(
                accounts = renderData.updatedAccounts,
                accountStats = renderData.accountStats,
                baseCurrency = renderData.baseCurrency,
                currentTheme = currentTheme,
                config = renderData.config,
                callbacks = cb,
                filterContext = filterContext,
                baseFilter = cardWithCallbacks.baseFilter,
                modifier = modifier.fillMaxWidth(),
                headerModifier = headerModifier.fillMaxWidth(),
            )
        }

        is CardWithCallbacks.PlannedPayments -> {
            val renderData = cardWithCallbacks.renderData
            val cb = cardWithCallbacks.callbacks

            PlannedPaymentsCard(
                upcoming = renderData.upcoming,
                overdue = renderData.overdue,
                config = renderData.config,
                callbacks = cb,
                modifier = modifier.fillMaxWidth(),
                headerModifier = headerModifier
            )
        }

        is CardWithCallbacks.Budgets -> {
            val renderData = cardWithCallbacks.renderData
            val cb = cardWithCallbacks.callbacks

            BudgetsCard(
                items = renderData.items,
                config = renderData.config,
                callbacks = cb,
                modifier = modifier.fillMaxWidth(),
                headerModifier = headerModifier
            )
        }

        is CardWithCallbacks.Loans -> {
            val renderData = cardWithCallbacks.renderData
            val cb = cardWithCallbacks.callbacks

            LoansCard(
                items = renderData.items,
                config = renderData.config,
                callbacks = cb,
                modifier = modifier.fillMaxWidth(),
                headerModifier = headerModifier
            )
        }

        is CardWithCallbacks.RecentActivities -> {
            val renderData = cardWithCallbacks.renderData
            val cb = cardWithCallbacks.callbacks

            RecentActivitiesCard(
                listState = renderData.listState,
                config = renderData.config,
                callbacks = cb,
                baseCurrency = renderData.baseCurrency,
                exchangeRates = renderData.exchangeRates,
                filterContext = filterContext,
                baseFilter = cardWithCallbacks.baseFilter,
                modifier = modifier.fillMaxWidth(),
                headerModifier = headerModifier,
            )
        }

        is CardWithCallbacks.ComputeError -> {
            // ComputeError is rendered by AeonCardList before reaching UnifiedCardRenderer.
            // This branch is a compile-required safety net.
        }
    }
}
