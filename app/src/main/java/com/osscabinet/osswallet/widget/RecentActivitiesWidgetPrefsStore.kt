package com.osscabinet.ossWallet.widget

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.preferencesDataStore

private val Context.recentActivitiesWidgetStore: DataStore<Preferences> by preferencesDataStore("ra_widget_prefs")

class RecentActivitiesWidgetPrefsStore(context: Context) : WidgetPrefsStore<RecentActivitiesWidgetConfig>(
    store = context.recentActivitiesWidgetStore,
    keyPrefix = "ra_config",
    serializer = RecentActivitiesWidgetConfig.serializer(),
    defaultValue = RecentActivitiesWidgetConfig()
)
