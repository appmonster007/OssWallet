package com.osscabinet.ossWallet.widget

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.preferencesDataStore

private val Context.rtWidgetStore: DataStore<Preferences> by preferencesDataStore("rt_widget_prefs")

class RecentTransactionsWidgetPrefsStore(context: Context) : WidgetPrefsStore<RecentTransactionsWidgetConfig>(
    store = context.rtWidgetStore,
    keyPrefix = "rt_config",
    serializer = RecentTransactionsWidgetConfig.serializer(),
    defaultValue = RecentTransactionsWidgetConfig()
)
