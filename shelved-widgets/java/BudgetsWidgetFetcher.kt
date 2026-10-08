package com.osscabinet.ossWallet.widget

import android.content.Context
import androidx.datastore.preferences.core.MutablePreferences
import com.osscabinet.ossWallet.di.WidgetDataLoaderEntryPoint
import com.osscabinet.ossWallet.domain.card.CardInput
import com.osscabinet.ossWallet.domain.card.computers.BudgetsCardComputer
import com.osscabinet.ossWallet.domain.model.BudgetProgress
import com.osscabinet.ossWallet.domain.model.settings.BudgetsCardConfig
import com.osscabinet.ossWallet.domain.service.currency.ExchangeRateSnapshot
import com.osscabinet.ossWallet.domain.service.timeline.TimelineService
import com.osscabinet.ossWallet.ui.util.formatCurrency
import com.osscabinet.ossWallet.widget.WidgetDataLoader.CommonData
import dagger.hilt.android.EntryPointAccessors
import kotlinx.coroutines.flow.first
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.math.BigDecimal

@Serializable
data class BudgetWidgetRow(
    val budgetId: Long,
    val name: String,
    val spentFormatted: String,
    val limitFormatted: String,
    val remainingFormatted: String,
    val percentage: Float,
    val alertLevel: String,
    val rolloverAmountFormatted: String,
    val hasRollover: Boolean,
    val comparisonText: String?
)

class BudgetsWidgetFetcher : WidgetDataFetcher {
    override suspend fun updateState(
        context: Context,
        widgetId: Int,
        common: CommonData,
        prefs: MutablePreferences
    ) {
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
        val cardConfig = BudgetsCardConfig(
            cardId = "budgets_widget_$widgetId",
            filter = effectiveFilter,
            showComparison = config.showComparison,
            showArchived = config.showArchived,
            maxItems = config.maxItems
        )

        val computer = BudgetsCardComputer(common.databaseQueryService)
        val renderData = runCatching { computer.compute(input, cardConfig) }.getOrNull()
        val items = renderData?.items.orEmpty()

        val previousItems: List<BudgetProgress>? = if (config.showComparison) {
            val (prevStart, prevEnd) = TimelineService.getPreviousRange(effectiveFilter.timelineRange)
            if (prevStart != null && prevEnd != null) {
                val prevFilter = effectiveFilter.copy(
                    timelineRange = com.osscabinet.ossWallet.domain.model.timeline.TimelineRange.CustomRange(prevStart, prevEnd)
                )
                val prevInput = input.copy(filter = prevFilter)
                val prevConfig = cardConfig.copy(cardId = "budgets_widget_prev_$widgetId", filter = prevFilter)
                runCatching { computer.compute(prevInput, prevConfig) }.getOrNull()?.items
            } else null
        } else null

        val rows = items.take(config.maxItems).map { progress ->
            val currency = progress.budget.currency
            val comparisonText = previousItems
                ?.find { it.budget.id == progress.budget.id }
                ?.let { buildComparisonText(progress.spent, it.spent, config.showComparison) }

            BudgetWidgetRow(
                budgetId = progress.budget.id,
                name = progress.budget.name,
                spentFormatted = formatCurrency(progress.spent, currency),
                limitFormatted = formatCurrency(progress.limit, currency),
                remainingFormatted = formatCurrency(progress.remaining, currency),
                percentage = progress.percentage,
                alertLevel = progress.alertLevel.name,
                rolloverAmountFormatted = formatCurrency(progress.rolloverAmount, currency),
                hasRollover = progress.rolloverAmount.signum() != 0,
                comparisonText = comparisonText
            )
        }

        prefs[BudgetsWidgetKeys.IS_LOADED] = "true"
        prefs[BudgetsWidgetKeys.PERIOD_LABEL] = config.periodLabel
        prefs[BudgetsWidgetKeys.SHOW_COMPARISON] = if (config.showComparison) "true" else "false"
        prefs[BudgetsWidgetKeys.ROWS_JSON] = Json.encodeToString(rows)
    }
}
