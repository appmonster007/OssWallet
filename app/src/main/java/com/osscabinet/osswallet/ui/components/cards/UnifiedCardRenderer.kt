package com.osscabinet.ossWallet.ui.components.cards

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.osscabinet.ossWallet.domain.card.CardRenderData
import com.osscabinet.ossWallet.domain.model.Account
import com.osscabinet.ossWallet.domain.model.Category
import com.osscabinet.ossWallet.domain.model.Filter
import com.osscabinet.ossWallet.ui.analytics.components.CategoryBreakdownCard
import com.osscabinet.ossWallet.ui.analytics.components.CategoryExpenseTreemapCard
import com.osscabinet.ossWallet.ui.dashboard.components.NetWorthCard
import com.osscabinet.ossWallet.ui.dashboard.components.CurrencyBalancesCard
import com.osscabinet.ossWallet.ui.dashboard.components.CashFlowCard
import com.osscabinet.ossWallet.ui.dashboard.components.AccountsCard
import com.osscabinet.ossWallet.ui.accountdetail.components.AccountHeroCard
import com.osscabinet.ossWallet.domain.model.timeline.TimelineRange
import com.osscabinet.ossWallet.ui.analytics.components.CategoryBreakdownData
import com.osscabinet.ossWallet.ui.analytics.components.CategoryExpenseTreemapData
import java.math.RoundingMode

/**
 * Unified card renderer composable that handles all card types across all screens.
 *
 * ⚠️ TYPE SAFETY: This renderer validates that the [callbacks] type matches the
 * [renderData] type at runtime. Passing an incompatible callback type will throw
 * an [IllegalArgumentException] immediately rather than silently failing.
 *
 * Expected pairings:
 * - NetWorth → WithFontSizeAndTimeline
 * - CurrencyBalances → CurrencyBalances
 * - CashFlow → CashFlow
 * - CategoryBreakdown → Analytics
 * - CategoryTreemap → Analytics
 * - AccountHero → AccountHero
 * - Accounts → Accounts
 */
@Composable
fun UnifiedCardRenderer(
    renderData: CardRenderData,
    callbacks: CardCallbacks,
    modifier: Modifier = Modifier,
    headerModifier: Modifier = Modifier,
    showTitle: Boolean = false,
    currentTheme: String = "light",
    accounts: List<Account> = emptyList(),
    allCategories: List<Category> = emptyList(),
    savedFilters: List<Filter> = emptyList()
) {
    when (renderData) {
        is CardRenderData.NetWorth -> {
            val cb = callbacks as? CardCallbacks.WithFontSizeAndTimeline
                ?: throw IllegalArgumentException(
                    "Expected CardCallbacks.WithFontSizeAndTimeline for NetWorth card, got ${callbacks::class.simpleName}"
                )
            
            NetWorthCard(
                netWorth = renderData.value,
                netIncome = renderData.cashFlowResult.totalIncomeInBase,
                netExpense = renderData.cashFlowResult.totalExpensesInBase,
                baseCurrency = renderData.baseCurrency,
                precision = renderData.precision,
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
                ?: throw IllegalArgumentException(
                    "Expected CardCallbacks.CurrencyBalances for CurrencyBalances card, got ${callbacks::class.simpleName}"
                )
            
            CurrencyBalancesCard(
                currencyTotals = renderData.items,
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
                ?: throw IllegalArgumentException(
                    "Expected CardCallbacks.CashFlow for CashFlow card, got ${callbacks::class.simpleName}"
                )
            
            CashFlowCard(
                cashFlowResult = renderData.result,
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
            val cb = callbacks as? CardCallbacks.Analytics
                ?: throw IllegalArgumentException(
                    "Expected CardCallbacks.Analytics for CategoryBreakdown card, got ${callbacks::class.simpleName}"
                )
            
            val uiData = renderData.items.map { metric ->
                CategoryBreakdownData(
                    category = metric.category,
                    categoryId = metric.categoryId,
                    netBalance = metric.netBalance,
                    income = metric.income,
                    expense = metric.expense,
                    colorHex = metric.colorHex,
                    percentage = metric.percentage,
                    hasSubcategories = metric.hasSubcategories,
                    subcategoryData = emptyList() // Simplified mapping
                )
            }

            CategoryBreakdownCard(
                config = renderData.config,
                data = uiData,
                modifier = modifier.fillMaxWidth(),
                headerModifier = headerModifier,
                onUpdate = { cb.onUpdate(it) },
                onDelete = { cb.onDelete() },
                onRecordsClick = { cb.onRecordsClick(it) },
                accounts = accounts,
                allCategories = allCategories,
                savedFilters = savedFilters
            )
        }
        
        is CardRenderData.CategoryTreemap -> {
            val cb = callbacks as? CardCallbacks.Analytics
                ?: throw IllegalArgumentException(
                    "Expected CardCallbacks.Analytics for CategoryTreemap card, got ${callbacks::class.simpleName}"
                )
            
            val uiData = renderData.items.map { metric ->
                CategoryExpenseTreemapData(
                    category = metric.category,
                    categoryId = metric.categoryId,
                    expense = metric.expense.abs(),
                    colorHex = metric.colorHex,
                    percentage = metric.percentage,
                    hasSubcategories = metric.hasSubcategories,
                    subcategoryData = emptyList() // Simplified mapping
                )
            }

            CategoryExpenseTreemapCard(
                config = renderData.config,
                data = uiData,
                modifier = modifier.fillMaxWidth(),
                headerModifier = headerModifier,
                onUpdate = { cb.onUpdate(it) },
                onDelete = { cb.onDelete() },
                onRecordsClick = { cb.onRecordsClick(it) },
                accounts = accounts,
                allCategories = allCategories,
                savedFilters = savedFilters
            )
        }

        is CardRenderData.AccountHero -> {
            val cb = callbacks as? CardCallbacks.AccountHero
                ?: throw IllegalArgumentException(
                    "Expected CardCallbacks.AccountHero for AccountHero card, got ${callbacks::class.simpleName}"
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
                onShowCurrencyBadgesChange = { show -> cb.onShowCurrencyBadgesChange(renderData.config.cardId, show) },
                onClick = cb.onClick,
                onDelete = { cb.onDelete() }
            )
        }
        
        is CardRenderData.Accounts -> {
            val cb = callbacks as? CardCallbacks.Accounts
                ?: throw IllegalArgumentException(
                    "Expected CardCallbacks.Accounts for Accounts card, got ${callbacks::class.simpleName}"
                )
            
            AccountsCard(
                onManageClick = cb.onManageClick,
                onHeaderClick = cb.onHeaderClick,
                onAccountClick = cb.onAccountClick,
                onRecordsClick = { cb.onRecordsClick(it) },
                onGridColumnsChange = { _, _ -> cb.onGridColumnsChange(renderData.config.cardId, renderData.config.gridColumns) },
                accounts = renderData.updatedAccounts,
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
    }
}
