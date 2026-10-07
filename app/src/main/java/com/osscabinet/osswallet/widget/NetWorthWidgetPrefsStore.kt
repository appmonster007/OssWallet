package com.osscabinet.ossWallet.widget

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.preferencesDataStore

private val Context.nwWidgetStore: DataStore<Preferences> by preferencesDataStore("nw_widget_prefs")

class NetWorthWidgetPrefsStore(context: Context) : WidgetPrefsStore<NetWorthWidgetConfig>(
    store = context.nwWidgetStore,
    keyPrefix = "nw_config",
    serializer = NetWorthWidgetConfig.serializer(),
    defaultValue = NetWorthWidgetConfig()
)
