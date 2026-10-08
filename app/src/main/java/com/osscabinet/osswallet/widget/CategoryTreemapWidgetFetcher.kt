package com.osscabinet.ossWallet.widget

import android.content.Context
import androidx.datastore.preferences.core.MutablePreferences
import androidx.datastore.preferences.core.stringPreferencesKey
import com.osscabinet.ossWallet.di.WidgetDataLoaderEntryPoint
import com.osscabinet.ossWallet.domain.card.CardInput
import com.osscabinet.ossWallet.domain.card.computers.CategoryTreemapCardComputer
import com.osscabinet.ossWallet.domain.model.settings.CategoryTreemapCardConfig
import com.osscabinet.ossWallet.domain.service.currency.ExchangeRateSnapshot
import com.osscabinet.ossWallet.ui.util.formatCurrency
import com.osscabinet.ossWallet.widget.WidgetDataLoader.CommonData
import dagger.hilt.android.EntryPointAccessors
import kotlinx.coroutines.flow.first

object CategoryTreemapWidgetKeys {
    val IS_LOADED = stringPreferencesKey("ctm_is_loaded")
    val PERIOD_LABEL = stringPreferencesKey("ctm_period_label")
    val EXPENSE_TOTAL_FORMATTED = stringPreferencesKey("ctm_expense_total_formatted")
    val SLICES_JSON = stringPreferencesKey("ctm_slices_json")
}

class CategoryTreemapWidgetFetcher : WidgetDataFetcher {
    override suspend fun updateState(
        context: Context,
        widgetId: Int,
        common: CommonData,
        prefs: MutablePreferences
    ) {
        val config = CategoryTreemapWidgetPrefsStore(context).loadSafe(widgetId)
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
        val cardConfig = CategoryTreemapCardConfig(
            cardId = "category_treemap_widget_$widgetId",
            filter = config.filter
        )

        val renderData = runCatching {
            CategoryTreemapCardComputer(common.databaseQueryService).compute(input, cardConfig)
        }.getOrNull()

        val slices = if (renderData != null) {
            CategoryChartDataMapper.flattenTopLevel(
                items = renderData.items,
                selectedMetric = cardConfig.selectedMetric,
                formatAmount = { amt -> formatCurrency(amt, common.baseCurrency) }
            )
        } else emptyList()

        prefs[CategoryTreemapWidgetKeys.IS_LOADED] = "true"
        prefs[CategoryTreemapWidgetKeys.PERIOD_LABEL] = config.periodLabel
        prefs[CategoryTreemapWidgetKeys.EXPENSE_TOTAL_FORMATTED] =
            formatCurrency(renderData?.metrics?.expense, common.baseCurrency)
        prefs[CategoryTreemapWidgetKeys.SLICES_JSON] = CategoryChartDataMapper.encode(slices)
    }
}
