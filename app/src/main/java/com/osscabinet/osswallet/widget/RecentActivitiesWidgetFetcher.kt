package com.osscabinet.ossWallet.widget

import android.content.Context
import androidx.datastore.preferences.core.MutablePreferences
import androidx.datastore.preferences.core.stringPreferencesKey
import com.osscabinet.ossWallet.domain.model.TransactionFilter
import com.osscabinet.ossWallet.widget.WidgetDataLoader.CommonData
import kotlinx.serialization.json.Json
import java.net.URLEncoder
import java.nio.charset.StandardCharsets

object RecentActivitiesWidgetKeys {
    val IS_LOADED = stringPreferencesKey("ra_is_loaded")
    /** URL-encoded JSON TransactionFilter — same "VIEW ALL" target as RecentActivitiesCard's own onSeeAll/onRecordsClick(config.filter). */
    val VIEW_ALL_FILTER = stringPreferencesKey("ra_view_all_filter")
    /** Name of a [WidgetTheme] constant — see NetWorthWidgetKeys.THEME. */
    val THEME = stringPreferencesKey("ra_theme")
    val GLASS = stringPreferencesKey("ra_glass")
}

/**
 * Row data isn't fetched here — RecentActivitiesRemoteViewsFactory queries it directly when the
 * ListView asks for it (see that class). This fetcher only prepares the "VIEW ALL" tap target's
 * filter, a static derivation of config, not a DB round trip.
 */
class RecentActivitiesWidgetFetcher : WidgetDataFetcher {
    override suspend fun updateState(
        context: Context,
        widgetId: Int,
        common: CommonData,
        prefs: MutablePreferences
    ) {
        val config = RecentActivitiesWidgetPrefsStore(context).loadSafe(widgetId)
        prefs[RecentActivitiesWidgetKeys.IS_LOADED] = "true"
        prefs[RecentActivitiesWidgetKeys.VIEW_ALL_FILTER] = encodeFilter(config.filter)
        prefs[RecentActivitiesWidgetKeys.THEME] = config.theme.name
        prefs[RecentActivitiesWidgetKeys.GLASS] = config.glass.toString()
    }
}

/** Same encoding [com.osscabinet.ossWallet.ui.navigation.NavigationExtensions.navigateToRecords] uses for TransactionListRoute.filterJson. */
private fun encodeFilter(filter: TransactionFilter): String =
    URLEncoder.encode(Json.encodeToString(filter), StandardCharsets.UTF_8.toString())
