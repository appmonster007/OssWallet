package com.osscabinet.ossWallet.widget

import android.content.Context
import androidx.datastore.preferences.core.MutablePreferences
import androidx.datastore.preferences.core.stringPreferencesKey
import com.osscabinet.ossWallet.di.WidgetDataLoaderEntryPoint
import com.osscabinet.ossWallet.domain.card.CardInput
import com.osscabinet.ossWallet.domain.card.computers.CategorySpendingTrendCardComputer
import com.osscabinet.ossWallet.domain.model.settings.CategorySpendingTrendCardConfig
import com.osscabinet.ossWallet.domain.service.currency.ExchangeRateSnapshot
import com.osscabinet.ossWallet.widget.WidgetDataLoader.CommonData
import dagger.hilt.android.EntryPointAccessors
import kotlinx.coroutines.flow.first
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

@Serializable
data class WidgetTrendSegmentRow(val colorHex: String, val amount: Double)

@Serializable
data class WidgetTrendBucketRow(val label: String, val segments: List<WidgetTrendSegmentRow>)

@Serializable
data class WidgetTrendLegendRow(val name: String, val colorHex: String, val natureTag: String)

object CategorySpendingTrendWidgetKeys {
    val IS_LOADED = stringPreferencesKey("cst_is_loaded")
    val PERIOD_LABEL = stringPreferencesKey("cst_period_label")
    val CATEGORY_COUNT = stringPreferencesKey("cst_category_count")
    val BUCKETS_JSON = stringPreferencesKey("cst_buckets_json")
    val LEGEND_JSON = stringPreferencesKey("cst_legend_json")
}

class CategorySpendingTrendWidgetFetcher : WidgetDataFetcher {
    override suspend fun updateState(
        context: Context,
        widgetId: Int,
        common: CommonData,
        prefs: MutablePreferences
    ) {
        val config = CategorySpendingTrendWidgetPrefsStore(context).loadSafe(widgetId)
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
        val cardConfig = CategorySpendingTrendCardConfig(
            cardId = "category_spending_trend_widget_$widgetId",
            filter = config.filter
        )

        val renderData = runCatching {
            CategorySpendingTrendCardComputer(common.databaseQueryService).compute(input, cardConfig)
        }.getOrNull()

        fun colorHex(colorInt: Int) = String.format("#%06X", 0xFFFFFF and colorInt)

        val buckets = renderData?.buckets.orEmpty().map { bucket ->
            WidgetTrendBucketRow(
                label = bucket.label,
                segments = bucket.categoryAmounts.map { WidgetTrendSegmentRow(colorHex(it.colorHex), it.amount.abs().toDouble()) }
            )
        }
        val legend = renderData?.activeCategories.orEmpty()
            .sortedByDescending { it.totalAmount.abs() }
            .map {
                WidgetTrendLegendRow(
                    name = it.name,
                    colorHex = colorHex(it.colorHex),
                    natureTag = it.nature.name
                )
            }

        prefs[CategorySpendingTrendWidgetKeys.IS_LOADED] = "true"
        prefs[CategorySpendingTrendWidgetKeys.PERIOD_LABEL] = config.periodLabel
        prefs[CategorySpendingTrendWidgetKeys.CATEGORY_COUNT] = legend.size.toString()
        prefs[CategorySpendingTrendWidgetKeys.BUCKETS_JSON] = Json.encodeToString(buckets)
        prefs[CategorySpendingTrendWidgetKeys.LEGEND_JSON] = Json.encodeToString(legend)
    }
}
