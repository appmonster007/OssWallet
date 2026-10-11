package com.osscabinet.ossWallet.widget

import android.content.Context
import androidx.datastore.preferences.core.MutablePreferences
import com.osscabinet.ossWallet.di.WidgetDataLoaderEntryPoint
import com.osscabinet.ossWallet.domain.card.CardInput
import com.osscabinet.ossWallet.domain.card.computers.PlannedPaymentsCardComputer
import com.osscabinet.ossWallet.domain.model.EntryMode
import com.osscabinet.ossWallet.domain.model.TransactionFilter
import com.osscabinet.ossWallet.domain.model.settings.PlannedPaymentsCardConfig
import com.osscabinet.ossWallet.domain.model.settings.PlannedPaymentsViewMode
import com.osscabinet.ossWallet.domain.model.timeline.TimelineRange
import com.osscabinet.ossWallet.domain.service.currency.ExchangeRateSnapshot
import com.osscabinet.ossWallet.ui.util.formatCurrency
import com.osscabinet.ossWallet.widget.WidgetDataLoader.CommonData
import dagger.hilt.android.EntryPointAccessors
import kotlinx.coroutines.flow.first
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.text.SimpleDateFormat
import java.util.Locale

@Serializable
data class PlannedPaymentWidgetRow(
    val paymentId: Long,
    val name: String,
    val amountFormatted: String,
    val isIncome: Boolean,
    val scheduledDateFormatted: String,
    val scheduledDate: Long,
    val isOverdue: Boolean
)

class PlannedPaymentsWidgetFetcher : WidgetDataFetcher {
    override suspend fun updateState(
        context: Context,
        widgetId: Int,
        common: CommonData,
        prefs: MutablePreferences
    ) {
        val config = PlannedPaymentsWidgetPrefsStore(context).loadSafe(widgetId)
        val ep = EntryPointAccessors.fromApplication(
            context.applicationContext,
            WidgetDataLoaderEntryPoint::class.java
        )
        val accounts = ep.accountRepository().getAllAccounts(common.profileId).first()
        val plannedPayments = ep.plannedPaymentRepository().getActivePlannedPayments(common.profileId).first()

        val now = System.currentTimeMillis()
        val lookaheadEnd = now + config.lookaheadDays * 24L * 60 * 60 * 1000

        val input = CardInput(
            profileId = common.profileId,
            targetCurrency = common.baseCurrency,
            exchangeRates = ExchangeRateSnapshot(anchor = common.baseCurrency, rates = common.exchangeRates),
            filter = TransactionFilter(timelineRange = TimelineRange.CustomRange(0L, lookaheadEnd)),
            accounts = accounts,
            categories = emptyList(),
            plannedPayments = plannedPayments
        )
        val cardConfig = PlannedPaymentsCardConfig(
            cardId = "planned_payments_widget_$widgetId",
            filter = TransactionFilter(timelineRange = TimelineRange.CustomRange(0L, lookaheadEnd)),
            viewMode = PlannedPaymentsViewMode.BOTH,
            maxItems = config.maxItems
        )

        val renderData = runCatching {
            PlannedPaymentsCardComputer().compute(input, cardConfig)
        }.getOrNull()

        val dateFormat = SimpleDateFormat("MMM d", Locale.getDefault())
        val rows = renderData?.items
            .orEmpty()
            .sortedBy { it.scheduledDate }
            .take(config.maxItems)
            .map { item ->
                PlannedPaymentWidgetRow(
                    paymentId = item.id,
                    name = item.name,
                    amountFormatted = formatCurrency(item.amount, item.currency),
                    isIncome = item.entryMode == EntryMode.INCOME,
                    scheduledDateFormatted = dateFormat.format(java.util.Date(item.scheduledDate)),
                    scheduledDate = item.scheduledDate,
                    isOverdue = item.isOverdue
                )
            }

        prefs[PlannedPaymentsWidgetKeys.IS_LOADED] = "true"
        prefs[PlannedPaymentsWidgetKeys.LOOKAHEAD_DAYS] = config.lookaheadDays.toString()
        prefs[PlannedPaymentsWidgetKeys.ROWS_JSON] = Json.encodeToString(rows)
    }
}
