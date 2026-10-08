package com.osscabinet.ossWallet.widget

import android.content.Context
import androidx.datastore.preferences.core.MutablePreferences
import androidx.datastore.preferences.core.stringPreferencesKey
import com.osscabinet.ossWallet.di.WidgetDataLoaderEntryPoint
import com.osscabinet.ossWallet.domain.card.CardInput
import com.osscabinet.ossWallet.domain.card.computers.TagUpSetPlotCardComputer
import com.osscabinet.ossWallet.domain.model.settings.TagUpSetPlotCardConfig
import com.osscabinet.ossWallet.domain.service.currency.ExchangeRateSnapshot
import com.osscabinet.ossWallet.ui.util.formatCurrency
import com.osscabinet.ossWallet.widget.WidgetDataLoader.CommonData
import dagger.hilt.android.EntryPointAccessors
import kotlinx.coroutines.flow.first
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

@Serializable
data class WidgetUpSetTagDto(val name: String, val colorHex: String, val totalAmountFormatted: String)

@Serializable
data class WidgetUpSetIntersectionDto(val tagIndices: List<Int>, val amount: Double, val txCount: Int)

object TagUpSetPlotWidgetKeys {
    val IS_LOADED = stringPreferencesKey("tup_is_loaded")
    val PERIOD_LABEL = stringPreferencesKey("tup_period_label")
    val TOTAL_FORMATTED = stringPreferencesKey("tup_total_formatted")
    val TX_COUNT = stringPreferencesKey("tup_tx_count")
    val TAGS_JSON = stringPreferencesKey("tup_tags_json")
    val INTERSECTIONS_JSON = stringPreferencesKey("tup_intersections_json")
}

class TagUpSetPlotWidgetFetcher : WidgetDataFetcher {
    override suspend fun updateState(
        context: Context,
        widgetId: Int,
        common: CommonData,
        prefs: MutablePreferences
    ) {
        val config = TagUpSetPlotWidgetPrefsStore(context).loadSafe(widgetId)
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
        val cardConfig = TagUpSetPlotCardConfig(
            cardId = "tag_upset_plot_widget_$widgetId",
            filter = config.filter
        )

        val renderData = runCatching {
            TagUpSetPlotCardComputer(common.databaseQueryService).compute(input, cardConfig)
        }.getOrNull()

        // Cap to top 3 tags by total amount, top 6 intersections by amount — the explicitly
        // allowed Phase 3 simplification for this chart.
        val topTags = renderData?.sets.orEmpty()
            .sortedByDescending { it.totalAmount.abs() }
            .take(3)
        val tagIndexByName = topTags.mapIndexed { idx, t -> t.tagName to idx }.toMap()

        val topIntersections = renderData?.intersections.orEmpty()
            .sortedByDescending { it.amount.abs() }
            .take(6)

        val tagDtos = topTags.map {
            WidgetUpSetTagDto(it.tagName, colorHex(it.colorHex), formatCurrency(it.totalAmount, common.baseCurrency))
        }
        val intersectionDtos = topIntersections.map { intersection ->
            WidgetUpSetIntersectionDto(
                tagIndices = intersection.tags.mapNotNull { tagIndexByName[it] },
                amount = intersection.amount.abs().toDouble(),
                txCount = intersection.transactionCount
            )
        }.filter { it.tagIndices.isNotEmpty() }

        val totalAmount = topTags.fold(java.math.BigDecimal.ZERO) { acc, t -> acc.add(t.totalAmount.abs()) }
        val totalTxCount = topTags.sumOf { it.transactionCount }

        prefs[TagUpSetPlotWidgetKeys.IS_LOADED] = "true"
        prefs[TagUpSetPlotWidgetKeys.PERIOD_LABEL] = config.periodLabel
        prefs[TagUpSetPlotWidgetKeys.TOTAL_FORMATTED] = formatCurrency(totalAmount, common.baseCurrency)
        prefs[TagUpSetPlotWidgetKeys.TX_COUNT] = totalTxCount.toString()
        prefs[TagUpSetPlotWidgetKeys.TAGS_JSON] = Json.encodeToString(tagDtos)
        prefs[TagUpSetPlotWidgetKeys.INTERSECTIONS_JSON] = Json.encodeToString(intersectionDtos)
    }

    private fun colorHex(colorInt: Int): String =
        if (colorInt == -1) "#9AA8A5" else String.format("#%06X", 0xFFFFFF and colorInt)
}
