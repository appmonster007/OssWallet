package com.osscabinet.ossWallet.widget

import android.content.Context
import androidx.datastore.preferences.core.MutablePreferences
import androidx.datastore.preferences.core.stringPreferencesKey
import com.osscabinet.ossWallet.di.WidgetDataLoaderEntryPoint
import com.osscabinet.ossWallet.domain.card.CardInput
import com.osscabinet.ossWallet.domain.card.computers.BudgetsCardComputer
import com.osscabinet.ossWallet.domain.model.BudgetPaceStatus
import com.osscabinet.ossWallet.domain.model.BudgetProgress
import com.osscabinet.ossWallet.domain.model.settings.BudgetsCardConfig
import com.osscabinet.ossWallet.domain.model.timeline.TimelineRange
import com.osscabinet.ossWallet.domain.service.currency.ExchangeRateSnapshot
import com.osscabinet.ossWallet.domain.service.management.BudgetService
import com.osscabinet.ossWallet.ui.util.formatCurrency
import com.osscabinet.ossWallet.widget.WidgetDataLoader.CommonData
import dagger.hilt.android.EntryPointAccessors
import kotlinx.coroutines.flow.first
import kotlinx.serialization.Serializable
import java.math.BigDecimal

object BudgetsWidgetKeys {
    val IS_LOADED = stringPreferencesKey("budgets_is_loaded")
    /** Number of rows, so the header can tell an empty list from a loaded one. */
    val COUNT = stringPreferencesKey("budgets_count")
    /** Budgets over their limit / at or ahead of their alert pace, across all budgets, not just the visible rows. */
    val OVER_COUNT = stringPreferencesKey("budgets_over_count")
    val NEAR_COUNT = stringPreferencesKey("budgets_near_count")
    /** Name of a [WidgetTheme] constant — see NetWorthWidgetKeys.THEME. */
    val THEME = stringPreferencesKey("budgets_theme")
    val GLASS = stringPreferencesKey("budgets_glass")
}

@Serializable
data class BudgetWidgetRow(
    val budgetId: Long,
    val name: String,
    val spentFormatted: String,
    val limitFormatted: String,
    val remainingFormatted: String,
    /** spent - limit when over; BudgetProgress.remaining is clamped at zero. */
    val overByFormatted: String,
    /** Percent of the limit spent (0..100+); BudgetProgress.percentage is a 0..1+ ratio. */
    val percentage: Float,
    /** [BudgetPaceStatus] name. */
    val pace: String,
    /** Share (0..100) of the budget's period that has passed; null for All Time budgets. */
    val elapsedPercent: Int?,
    val periodLabel: String,
    val rolloverAmountFormatted: String,
    val hasRollover: Boolean
)

/** Everything the Budgets widget shows: the header counts and every row, most urgent first. */
data class BudgetsSnapshot(
    val rows: List<BudgetWidgetRow>,
    val overCount: Int,
    val nearCount: Int,
    val theme: WidgetTheme,
    val glass: Boolean
)

/** Drives the widget off the same [BudgetsCardComputer] the dashboard card uses. */
class BudgetsWidgetFetcher : WidgetDataFetcher {
    override suspend fun updateState(
        context: Context,
        widgetId: Int,
        common: CommonData,
        prefs: MutablePreferences
    ) {
        val snapshot = load(context, widgetId, common)
        prefs[BudgetsWidgetKeys.IS_LOADED] = "true"
        prefs[BudgetsWidgetKeys.COUNT] = snapshot.rows.size.toString()
        prefs[BudgetsWidgetKeys.THEME] = snapshot.theme.name
        prefs[BudgetsWidgetKeys.GLASS] = snapshot.glass.toString()
        prefs[BudgetsWidgetKeys.OVER_COUNT] = snapshot.overCount.toString()
        prefs[BudgetsWidgetKeys.NEAR_COUNT] = snapshot.nearCount.toString()
    }

    companion object {
        suspend fun load(context: Context, widgetId: Int, common: CommonData): BudgetsSnapshot {
            val config = BudgetsWidgetPrefsStore(context).loadSafe(widgetId)
            val ep = EntryPointAccessors.fromApplication(
                context.applicationContext,
                WidgetDataLoaderEntryPoint::class.java
            )
            val accounts = ep.accountRepository().getAllAccounts(common.profileId).first()
            val budgets = ep.budgetRepository().getAllBudgets(common.profileId).first()

            val effectiveFilter = config.filter.copy(
                timelineRange = config.filter.timelineRange.withExcludeFuture(true)
            )
            val input = CardInput(
                profileId = common.profileId,
                targetCurrency = common.baseCurrency,
                exchangeRates = ExchangeRateSnapshot(anchor = common.baseCurrency, rates = common.exchangeRates),
                filter = effectiveFilter,
                accounts = accounts,
                categories = emptyList(),
                budgets = budgets
            )
            // Every budget is computed: the header chip counts them all, and the list scrolls.
            val cardConfig = BudgetsCardConfig(
                cardId = "budgets_widget_$widgetId",
                filter = effectiveFilter,
                showArchived = config.showArchived,
                maxItems = Int.MAX_VALUE
            )
            val items = runCatching { BudgetsCardComputer(common.databaseQueryService).compute(input, cardConfig) }
                .getOrNull()?.items.orEmpty()

            val withPace = items.map { it to BudgetService.paceStatus(it) }
            // Over first, then budgets spending faster than their period, then near their alert
            // threshold, then the rest. sortedBy is stable, so saved order holds inside each group.
            val rows = withPace.sortedBy { (_, pace) -> pace.urgency() }
                .map { (progress, pace) -> progress.toRow(pace) }

            return BudgetsSnapshot(
                rows = rows,
                overCount = withPace.count { it.second == BudgetPaceStatus.OVER },
                nearCount = withPace.count {
                    it.second == BudgetPaceStatus.AHEAD_OF_PACE || it.second == BudgetPaceStatus.NEAR_LIMIT
                },
                theme = config.theme,
                glass = config.glass
            )
        }

        private fun BudgetPaceStatus.urgency() = when (this) {
            BudgetPaceStatus.OVER -> 0
            BudgetPaceStatus.AHEAD_OF_PACE -> 1
            BudgetPaceStatus.NEAR_LIMIT -> 2
            BudgetPaceStatus.ON_TRACK -> 3
        }

        private fun BudgetProgress.toRow(pace: BudgetPaceStatus): BudgetWidgetRow {
            val currency = budget.currency
            return BudgetWidgetRow(
                budgetId = budget.id,
                name = budget.name,
                spentFormatted = formatCurrency(spent, currency),
                limitFormatted = formatCurrency(limit, currency),
                remainingFormatted = formatCurrency(remaining, currency),
                overByFormatted = formatCurrency((spent - limit).max(BigDecimal.ZERO), currency),
                percentage = percentage * 100f,
                pace = pace.name,
                elapsedPercent = BudgetService.paceTargetFraction(this)?.let { (it * 100f).toInt().coerceIn(0, 100) },
                periodLabel = budget.timelineRange.budgetPeriodLabel(),
                rolloverAmountFormatted = formatCurrency(rolloverAmount, currency),
                hasRollover = rolloverAmount.signum() != 0
            )
        }

        private fun TimelineRange.budgetPeriodLabel(): String = when (this) {
            is TimelineRange.AllTime -> "All time"
            is TimelineRange.ThisPeriod -> period.label
            is TimelineRange.LastPeriod -> "Last ${period.label}"
            is TimelineRange.CustomRange -> "Custom range"
            is TimelineRange.Periodic -> recurrenceRule.formatHuman()
        }
    }
}
