package com.osscabinet.ossWallet.widget

import android.content.Context
import androidx.datastore.preferences.core.MutablePreferences
import androidx.datastore.preferences.core.stringPreferencesKey
import com.osscabinet.ossWallet.di.WidgetDataLoaderEntryPoint
import com.osscabinet.ossWallet.domain.card.CardInput
import com.osscabinet.ossWallet.domain.card.computers.IncomeExpenseBarCardComputer
import com.osscabinet.ossWallet.domain.model.settings.IncomeExpenseBarCardConfig
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
data class WidgetIncomeExpenseBucketRow(
    val label: String,
    val income: Double,
    val expense: Double,
    val net: Double,
    val isFuture: Boolean
)

object IncomeExpenseBarWidgetKeys {
    val IS_LOADED = stringPreferencesKey("ieb_is_loaded")
    val PERIOD_LABEL = stringPreferencesKey("ieb_period_label")
    val NET_SO_FAR_FORMATTED = stringPreferencesKey("ieb_net_so_far_formatted")
    val BUCKETS_JSON = stringPreferencesKey("ieb_buckets_json")
    val TODAY_BUCKET_INDEX = stringPreferencesKey("ieb_today_bucket_index")
}

class IncomeExpenseBarWidgetFetcher : WidgetDataFetcher {
    override suspend fun updateState(
        context: Context,
        widgetId: Int,
        common: CommonData,
        prefs: MutablePreferences
    ) {
        val config = IncomeExpenseBarWidgetPrefsStore(context).loadSafe(widgetId)
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
        val cardConfig = IncomeExpenseBarCardConfig(
            cardId = "income_expense_bar_widget_$widgetId",
            filter = config.filter
        )

        val renderData = runCatching {
            IncomeExpenseBarCardComputer(common.databaseQueryService).compute(input, cardConfig)
        }.getOrNull()

        val todayIdx = renderData?.todayBucketIndex ?: -1
        val rows = renderData?.buckets?.mapIndexed { idx, point ->
            WidgetIncomeExpenseBucketRow(
                label = point.label,
                income = point.metrics.income.toDouble(),
                expense = point.metrics.expense.abs().toDouble(),
                net = point.metrics.netFlow.toDouble(),
                isFuture = todayIdx >= 0 && idx > todayIdx
            )
        }.orEmpty()

        val netSoFar = renderData?.buckets
            ?.filterIndexed { idx, _ -> todayIdx < 0 || idx <= todayIdx }
            ?.fold(BigDecimal.ZERO) { acc, p -> acc.add(p.metrics.netFlow) }
            ?: BigDecimal.ZERO

        prefs[IncomeExpenseBarWidgetKeys.IS_LOADED] = "true"
        prefs[IncomeExpenseBarWidgetKeys.PERIOD_LABEL] = config.periodLabel
        prefs[IncomeExpenseBarWidgetKeys.NET_SO_FAR_FORMATTED] = formatCurrency(netSoFar, common.baseCurrency)
        prefs[IncomeExpenseBarWidgetKeys.BUCKETS_JSON] = Json.encodeToString(rows)
        prefs[IncomeExpenseBarWidgetKeys.TODAY_BUCKET_INDEX] = todayIdx.toString()
    }
}
