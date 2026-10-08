package com.osscabinet.ossWallet.widget

import android.content.Context
import androidx.datastore.preferences.core.MutablePreferences
import androidx.datastore.preferences.core.stringPreferencesKey
import com.osscabinet.ossWallet.di.WidgetDataLoaderEntryPoint
import com.osscabinet.ossWallet.domain.card.CardInput
import com.osscabinet.ossWallet.domain.card.computers.CategoryPieChartCardComputer
import com.osscabinet.ossWallet.domain.model.settings.CategoryPieChartCardConfig
import com.osscabinet.ossWallet.domain.service.currency.ExchangeRateSnapshot
import com.osscabinet.ossWallet.ui.util.formatCurrency
import com.osscabinet.ossWallet.widget.WidgetDataLoader.CommonData
import dagger.hilt.android.EntryPointAccessors
import kotlinx.coroutines.flow.first

object CategoryPieChartWidgetKeys {
    val IS_LOADED = stringPreferencesKey("cpc_is_loaded")
    val PERIOD_LABEL = stringPreferencesKey("cpc_period_label")
    val EXPENSE_TOTAL_FORMATTED = stringPreferencesKey("cpc_expense_total_formatted")
    val SLICES_JSON = stringPreferencesKey("cpc_slices_json")
}

class CategoryPieChartWidgetFetcher : WidgetDataFetcher {
    override suspend fun updateState(
        context: Context,
        widgetId: Int,
        common: CommonData,
        prefs: MutablePreferences
    ) {
        val config = CategoryPieChartWidgetPrefsStore(context).loadSafe(widgetId)
        val ep = EntryPointAccessors.fromApplication(context.applicationContext, WidgetDataLoaderEntryPoint::class.java)
        val accounts = ep.accountRepository().getAllAccounts(common.profileId).first()
        val categories = common.categoryRepository.getAllCategories(common.profileId).first()

        val input = CardInput(
            profileId = common.profileId,
            targetCurrency = common.baseCurrency,
            exchangeRates = ExchangeRateSnapshot(anchor = common.baseCurrency, rates = common.exchangeRates),
            filter = config.filter,
            accounts = accounts,
            categories = categories
        )
        val cardConfig = CategoryPieChartCardConfig(
            cardId = "category_pie_chart_widget_$widgetId",
            filter = config.filter
        )

        val renderData = runCatching {
            CategoryPieChartCardComputer(common.databaseQueryService).compute(input, cardConfig)
        }.getOrNull()

        val slices = if (renderData != null) {
            CategoryChartDataMapper.flattenTopLevel(
                items = renderData.items,
                selectedMetric = cardConfig.selectedMetric,
                formatAmount = { amt -> formatCurrency(amt, common.baseCurrency) }
            )
        } else emptyList()
        // All slices are kept (donut bitmap needs every slice); the renderer collapses anything
        // beyond the top 4 into a single "Other" legend row.

        prefs[CategoryPieChartWidgetKeys.IS_LOADED] = "true"
        prefs[CategoryPieChartWidgetKeys.PERIOD_LABEL] = config.periodLabel
        prefs[CategoryPieChartWidgetKeys.EXPENSE_TOTAL_FORMATTED] =
            formatCurrency(renderData?.metrics?.expense, common.baseCurrency)
        prefs[CategoryPieChartWidgetKeys.SLICES_JSON] = CategoryChartDataMapper.encode(slices)
    }
}
