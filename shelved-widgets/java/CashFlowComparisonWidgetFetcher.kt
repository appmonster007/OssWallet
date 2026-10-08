package com.osscabinet.ossWallet.widget

import android.content.Context
import androidx.datastore.preferences.core.MutablePreferences
import androidx.datastore.preferences.core.stringPreferencesKey
import com.osscabinet.ossWallet.di.WidgetDataLoaderEntryPoint
import com.osscabinet.ossWallet.domain.card.CardInput
import com.osscabinet.ossWallet.domain.card.CashFlowComparisonSeries
import com.osscabinet.ossWallet.domain.card.computers.CashFlowComparisonCardComputer
import com.osscabinet.ossWallet.domain.model.settings.CashFlowComparisonCardConfig
import com.osscabinet.ossWallet.domain.service.currency.ExchangeRateSnapshot
import com.osscabinet.ossWallet.ui.util.formatCurrency
import com.osscabinet.ossWallet.widget.WidgetDataLoader.CommonData
import dagger.hilt.android.EntryPointAccessors
import kotlinx.coroutines.flow.first
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.math.BigDecimal

@Serializable
data class WidgetCashFlowSeriesDto(val points: List<Double>) // cumulative net flow, one per point

object CashFlowComparisonWidgetKeys {
    val IS_LOADED = stringPreferencesKey("cfc_is_loaded")
    val CURRENT_TOTAL_FORMATTED = stringPreferencesKey("cfc_current_total_formatted")
    val CURRENT_SERIES_JSON = stringPreferencesKey("cfc_current_series_json")
    val PREVIOUS_SERIES_JSON = stringPreferencesKey("cfc_previous_series_json")
    val LAST_YEAR_SERIES_JSON = stringPreferencesKey("cfc_last_year_series_json")
    val CURRENT_TOTAL_BUCKETS = stringPreferencesKey("cfc_current_total_buckets")
    val PREVIOUS_TOTAL_BUCKETS = stringPreferencesKey("cfc_previous_total_buckets")
    val LAST_YEAR_TOTAL_BUCKETS = stringPreferencesKey("cfc_last_year_total_buckets")
}

private fun cumulate(series: CashFlowComparisonSeries): List<Double> {
    var running = BigDecimal.ZERO
    return series.points.map { p -> running = running.add(p.metrics.netFlow); running.toDouble() }
}

class CashFlowComparisonWidgetFetcher : WidgetDataFetcher {
    override suspend fun updateState(
        context: Context,
        widgetId: Int,
        common: CommonData,
        prefs: MutablePreferences
    ) {
        val config = CashFlowComparisonWidgetPrefsStore(context).loadSafe(widgetId)
        val ep = EntryPointAccessors.fromApplication(context.applicationContext, WidgetDataLoaderEntryPoint::class.java)
        val accounts = ep.accountRepository().getAllAccounts(common.profileId).first()

        val input = CardInput(
            profileId = common.profileId,
            targetCurrency = common.baseCurrency,
            exchangeRates = ExchangeRateSnapshot(anchor = common.baseCurrency, rates = common.exchangeRates),
            filter = config.filter,
            accounts = accounts,
            categories = emptyList()
        )
        val cardConfig = CashFlowComparisonCardConfig(
            cardId = "cashflow_comparison_widget_$widgetId",
            filter = config.filter
        )

        val renderData = runCatching {
            CashFlowComparisonCardComputer(common.databaseQueryService).compute(input, cardConfig)
        }.getOrNull()

        val currentPoints = renderData?.currentSeries?.let(::cumulate).orEmpty()
        val previousPoints = renderData?.previousSeries?.let(::cumulate).orEmpty()
        val lastYearPoints = renderData?.lastYearSeries?.let(::cumulate).orEmpty()
        val currentTotal = currentPoints.lastOrNull()?.let { BigDecimal.valueOf(it) } ?: BigDecimal.ZERO

        prefs[CashFlowComparisonWidgetKeys.IS_LOADED] = "true"
        prefs[CashFlowComparisonWidgetKeys.CURRENT_TOTAL_FORMATTED] = formatCurrency(currentTotal, common.baseCurrency)
        prefs[CashFlowComparisonWidgetKeys.CURRENT_SERIES_JSON] = Json.encodeToString(WidgetCashFlowSeriesDto(currentPoints))
        prefs[CashFlowComparisonWidgetKeys.PREVIOUS_SERIES_JSON] = Json.encodeToString(WidgetCashFlowSeriesDto(previousPoints))
        prefs[CashFlowComparisonWidgetKeys.LAST_YEAR_SERIES_JSON] = Json.encodeToString(WidgetCashFlowSeriesDto(lastYearPoints))
        prefs[CashFlowComparisonWidgetKeys.CURRENT_TOTAL_BUCKETS] = (renderData?.currentTotalBuckets ?: 0).toString()
        prefs[CashFlowComparisonWidgetKeys.PREVIOUS_TOTAL_BUCKETS] = (renderData?.previousTotalBuckets ?: 0).toString()
        prefs[CashFlowComparisonWidgetKeys.LAST_YEAR_TOTAL_BUCKETS] = (renderData?.lastYearTotalBuckets ?: 0).toString()
    }
}
