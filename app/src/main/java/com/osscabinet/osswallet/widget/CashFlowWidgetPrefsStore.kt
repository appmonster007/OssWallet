package com.osscabinet.ossWallet.widget

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.preferencesDataStore

private val Context.cfWidgetStore: DataStore<Preferences> by preferencesDataStore("cf_widget_prefs")

class CashFlowWidgetPrefsStore(context: Context) : WidgetPrefsStore<CashFlowWidgetConfig>(
    store = context.cfWidgetStore,
    keyPrefix = "cf_config",
    serializer = CashFlowWidgetConfig.serializer(),
    defaultValue = CashFlowWidgetConfig()
)
