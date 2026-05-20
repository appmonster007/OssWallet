package com.osscabinet.ossWallet.widget

import android.content.Context
import androidx.datastore.preferences.core.MutablePreferences
import com.osscabinet.ossWallet.domain.model.TransactionFilter
import com.osscabinet.ossWallet.domain.service.filter.TransactionFilterService
import com.osscabinet.ossWallet.domain.service.currency.CurrencyFormatter.formatCurrencyRight
import com.osscabinet.ossWallet.widget.WidgetDataLoader.CommonData
import kotlinx.coroutines.flow.first
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Serializable
data class WidgetTransaction(
    val id: Long,
    val amount: String,
    val currency: String,
    val type: String,
    val category: String,
    val timestamp: String,
    val note: String,
    val accountName: String,
    val iconName: String?,
    val categoryColor: Int? = null
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
        
        // Fetch all transactions for the profile
        val allTransactions = common.databaseQueryService
            .observeFilteredTransactions(common.profileId, filter)
            .first()

        // Apply "last 20 or last 7 days" logic
        val recentTransactions = TransactionFilterService.filterToRecent(
            items = allTransactions,
            getTimestamp = { it.timestamp },
            limit = maxOf(config.limit, 20),
            days = 7,
            excludeFuture = true
        )

        val categoryMap = common.categoryRepository.categories.first().associateBy { it.id }
        
        // Load accounts to map account names
        val ep = dagger.hilt.android.EntryPointAccessors.fromApplication(
            context.applicationContext,
            com.osscabinet.ossWallet.di.WidgetDataLoaderEntryPoint::class.java
        )
        val accountsMap = ep.accountRepository().getAllAccounts(common.profileId).first().associateBy { it.id }

        val timeFormatter = SimpleDateFormat("HH:mm", Locale.getDefault())
        val dateFormatter = SimpleDateFormat("MMM dd", Locale.getDefault())

        val widgetTransactions = recentTransactions.map { tx ->
            val category = categoryMap[tx.categoryId]
            val account = accountsMap[tx.accountId]
            WidgetTransaction(
                id = tx.id,
                amount = formatCurrencyRight(tx.amount.abs(), tx.currency),
                currency = tx.currency,
                type = tx.transactionType.value,
                category = category?.name ?: "Unknown",
                timestamp = if (isSameDay(tx.timestamp, System.currentTimeMillis())) {
                    timeFormatter.format(Date(tx.timestamp))
                } else {
                    dateFormatter.format(Date(tx.timestamp))
                },
                note = tx.note,
                accountName = account?.name ?: "Account",
                iconName = category?.iconName,
                categoryColor = category?.colorHex
            )
        }

        prefs[RecentTransactionsGlanceWidget.Keys.IS_LOADED] = "true"
        prefs[RecentTransactionsGlanceWidget.Keys.PERIOD_LABEL] = "Latest"
        prefs[RecentTransactionsGlanceWidget.Keys.TRANSACTIONS_JSON] = Json.encodeToString(widgetTransactions)
        prefs[RecentTransactionsGlanceWidget.Keys.PRIMARY_DISPLAY] = config.primaryDisplay.name
    }

    private fun isSameDay(t1: Long, t2: Long): Boolean {
        val fmt = SimpleDateFormat("yyyyMMdd", Locale.getDefault())
        return fmt.format(Date(t1)) == fmt.format(Date(t2))
    }
}
