package com.osscabinet.ossWallet.widget

import android.content.Context
import androidx.datastore.preferences.core.MutablePreferences
import com.osscabinet.ossWallet.domain.model.TransactionFilter
import com.osscabinet.ossWallet.domain.service.currency.CurrencyFormatter.formatCurrencyRight
import com.osscabinet.ossWallet.widget.WidgetDataLoader.CommonData
import kotlinx.coroutines.flow.first
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import com.osscabinet.ossWallet.widget.RecentTransactionsGlanceWidget
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Serializable
data class WidgetTransaction(
    val id: Long,
    val title: String,
    val amount: String,
    val currency: String,
    val type: String,
    val category: String,
    val timestamp: String,
    val note: String,
    val iconName: String?
)

class RecentTransactionsWidgetFetcher : WidgetDataFetcher {
    override suspend fun updateState(
        context: Context,
        widgetId: Int,
        common: CommonData,
        prefs: MutablePreferences
    ) {
        val config = runCatching {
            RecentTransactionsWidgetPrefsStore(context).load(widgetId)
        }.getOrElse { RecentTransactionsWidgetConfig() }

        val filter = TransactionFilter(excludeFuture = true)
        
        val transactions = common.databaseQueryService
            .observeFilteredTransactions(common.profileId, filter, limit = config.limit)
            .first()

        val categoryMap = common.categoryRepository.categories.first().associateBy { it.id }

        val timeFormatter = SimpleDateFormat("HH:mm", Locale.getDefault())
        val dateFormatter = SimpleDateFormat("MMM dd", Locale.getDefault())

        val widgetTransactions = transactions.map { tx ->
            WidgetTransaction(
                id = tx.id,
                title = tx.title,
                amount = formatCurrencyRight(tx.amount, tx.currency, common.precision),
                currency = tx.currency,
                type = tx.transactionType,
                category = categoryMap[tx.categoryId]?.name ?: "Unknown",
                timestamp = if (isSameDay(tx.timestamp, System.currentTimeMillis())) {
                    timeFormatter.format(Date(tx.timestamp))
                } else {
                    dateFormatter.format(Date(tx.timestamp))
                },
                note = tx.note,
                iconName = null
            )
        }

        prefs[RecentTransactionsGlanceWidget.Keys.IS_LOADED] = "true"
        prefs[RecentTransactionsGlanceWidget.Keys.TRANSACTIONS_JSON] = Json.encodeToString(widgetTransactions)
    }

    private fun isSameDay(t1: Long, t2: Long): Boolean {
        val fmt = SimpleDateFormat("yyyyMMdd", Locale.getDefault())
        return fmt.format(Date(t1)) == fmt.format(Date(t2))
    }
}
