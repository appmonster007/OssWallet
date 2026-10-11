package com.osscabinet.ossWallet.widget

import android.content.Context
import androidx.datastore.preferences.core.MutablePreferences
import com.osscabinet.ossWallet.di.WidgetDataLoaderEntryPoint
import com.osscabinet.ossWallet.domain.card.CardInput
import com.osscabinet.ossWallet.domain.model.ConfirmedTransactionUiModel
import com.osscabinet.ossWallet.domain.model.PlannedPaymentUiModel
import com.osscabinet.ossWallet.domain.model.RecordUiModel
import com.osscabinet.ossWallet.domain.model.TagUiModel
import com.osscabinet.ossWallet.domain.model.settings.RecentActivitiesCardConfig
import com.osscabinet.ossWallet.domain.service.aggregation.CardAggregationService
import com.osscabinet.ossWallet.domain.service.currency.ExchangeRateSnapshot
import com.osscabinet.ossWallet.ui.util.formatCurrency
import com.osscabinet.ossWallet.widget.WidgetDataLoader.CommonData
import dagger.hilt.android.EntryPointAccessors
import kotlinx.coroutines.flow.first
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

/**
 * Full [RecordUiModel] surface for one Recent Activities widget row (see RECENT_ACTIVITIES
 * target data model in docs/card-data-summary.md) plus a widget-only [dateLabel] /
 * [isIncome] convenience the renderer needs that the card doesn't carry per-row (the card
 * gets those from [com.osscabinet.ossWallet.domain.model.timeline.GroupHeaderData] / the
 * transaction type respectively).
 */
@Serializable
data class RecentActivityWidgetRow(
    val id: String,
    val dateLabel: String,
    val isOverdueGroup: Boolean,
    val timestampFormatted: String,
    val amountFormatted: String,
    val amountInBaseFormatted: String?,
    val currency: String,
    val categoryName: String,
    val categoryIcon: String?,
    val categoryColor: Int,
    val accountName: String,
    val accountCurrency: String,
    val note: String?,
    val tags: List<TagUiModel> = emptyList(),
    val kind: String, // "confirmed" | "planned"
    val isIncome: Boolean,
    // confirmed-only
    val status: String? = null,
    val linkedTransactionIds: List<Long> = emptyList(),
    // planned-only
    val paymentId: Long? = null,
    val occurrenceId: Long? = null,
    val isOverdue: Boolean = false
)

class RecentTransactionsWidgetFetcher : WidgetDataFetcher {
    override suspend fun updateState(
        context: Context,
        widgetId: Int,
        common: CommonData,
        prefs: MutablePreferences
    ) {
        val config = RecentTransactionsWidgetPrefsStore(context).loadSafe(widgetId)
        val filter = config.filter.copy(
            timelineRange = config.filter.timelineRange.withExcludeFuture(true)
        )

        val ep = EntryPointAccessors.fromApplication(
            context.applicationContext,
            WidgetDataLoaderEntryPoint::class.java
        )
        val accounts = ep.accountRepository().getAllAccounts(common.profileId).first()
        val categories = common.categoryRepository.getAllCategories(common.profileId).first()

        // Goes through the SAME card computer the Compose RecentActivitiesCard uses (per
        // docs/card-data-summary.md RECENT_ACTIVITIES target model), rather than hand-rolling
        // a widget-local query — keeps tags, status, and linked-transfer data intact.
        val renderData = runCatching {
            CardAggregationService.computeRecentActivitiesCard(
                queryService = common.databaseQueryService,
                profileId = common.profileId,
                input = CardInput(
                    profileId = common.profileId,
                    targetCurrency = common.baseCurrency,
                    exchangeRates = ExchangeRateSnapshot(
                        anchor = common.baseCurrency,
                        rates = common.exchangeRates
                    ),
                    filter = filter,
                    accounts = accounts,
                    categories = categories
                ),
                config = RecentActivitiesCardConfig(
                    cardId = "ra_widget_$widgetId",
                    filter = filter
                )
            )
        }.getOrNull()

        val timeFormatter = SimpleDateFormat("h:mm a", Locale.getDefault())
        val dateFormatter = SimpleDateFormat("MMM d", Locale.getDefault())

        val rows = renderData?.listState?.groups.orEmpty().flatMap { group ->
            val dateLabel = group.headerData.title
            val isOverdueGroup = dateLabel.equals("Due", ignoreCase = true) ||
                dateLabel.contains("overdue", ignoreCase = true)
            group.transactions.map { record ->
                record.toWidgetRow(
                    dateLabel = dateLabel,
                    isOverdueGroup = isOverdueGroup,
                    baseCurrency = common.baseCurrency,
                    timeFormatter = timeFormatter,
                    dateFormatter = dateFormatter
                )
            }
        }.take(maxOf(config.limit, 20))

        prefs[RecentTransactionsWidgetKeys.IS_LOADED] = "true"
        prefs[RecentTransactionsWidgetKeys.PERIOD_LABEL] = config.timelineLabel
        prefs[RecentTransactionsWidgetKeys.ROWS_JSON] = Json.encodeToString(rows)
        prefs[RecentTransactionsWidgetKeys.PRIMARY_DISPLAY] = config.primaryDisplay.name
    }

    private fun RecordUiModel.toWidgetRow(
        dateLabel: String,
        isOverdueGroup: Boolean,
        baseCurrency: String,
        timeFormatter: SimpleDateFormat,
        dateFormatter: SimpleDateFormat
    ): RecentActivityWidgetRow {
        val timestampFormatted = if (isSameDay(timestamp, System.currentTimeMillis())) {
            timeFormatter.format(Date(timestamp))
        } else {
            dateFormatter.format(Date(timestamp))
        }
        val amountFormatted = formatCurrency(amount.abs(), currency)
        val amountInBaseFormatted = amountInBase
            ?.takeIf { currency != baseCurrency }
            ?.let { formatCurrency(it.abs(), baseCurrency) }

        return when (this) {
            is ConfirmedTransactionUiModel -> RecentActivityWidgetRow(
                id = id,
                dateLabel = dateLabel,
                isOverdueGroup = isOverdueGroup,
                timestampFormatted = timestampFormatted,
                amountFormatted = amountFormatted,
                amountInBaseFormatted = amountInBaseFormatted,
                currency = currency,
                categoryName = categoryName,
                categoryIcon = categoryIcon,
                categoryColor = categoryColor,
                accountName = accountName,
                accountCurrency = accountCurrency,
                note = note,
                tags = tags,
                kind = "confirmed",
                isIncome = amount.signum() > 0,
                status = status.name,
                linkedTransactionIds = linkedTransactionIds
            )
            is PlannedPaymentUiModel -> RecentActivityWidgetRow(
                id = id,
                dateLabel = dateLabel,
                isOverdueGroup = isOverdueGroup,
                timestampFormatted = timestampFormatted,
                amountFormatted = amountFormatted,
                amountInBaseFormatted = amountInBaseFormatted,
                currency = currency,
                categoryName = categoryName,
                categoryIcon = categoryIcon,
                categoryColor = categoryColor,
                accountName = accountName,
                accountCurrency = accountCurrency,
                note = note,
                tags = tags,
                kind = "planned",
                isIncome = amount.signum() > 0,
                paymentId = paymentId,
                occurrenceId = occurrenceId,
                isOverdue = isOverdue
            )
            else -> RecentActivityWidgetRow(
                id = id,
                dateLabel = dateLabel,
                isOverdueGroup = isOverdueGroup,
                timestampFormatted = timestampFormatted,
                amountFormatted = amountFormatted,
                amountInBaseFormatted = amountInBaseFormatted,
                currency = currency,
                categoryName = categoryName,
                categoryIcon = categoryIcon,
                categoryColor = categoryColor,
                accountName = accountName,
                accountCurrency = accountCurrency,
                note = note,
                tags = tags,
                kind = "confirmed",
                isIncome = amount.signum() > 0
            )
        }
    }

    private fun isSameDay(t1: Long, t2: Long): Boolean {
        val c1 = Calendar.getInstance().apply { timeInMillis = t1 }
        val c2 = Calendar.getInstance().apply { timeInMillis = t2 }
        return c1.get(Calendar.YEAR) == c2.get(Calendar.YEAR) &&
            c1.get(Calendar.DAY_OF_YEAR) == c2.get(Calendar.DAY_OF_YEAR)
    }
}
