package com.osscabinet.ossWallet.widget

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.preferencesDataStore

private val Context.loansWidgetStore: DataStore<Preferences> by preferencesDataStore("loans_widget_prefs")

class LoansWidgetPrefsStore(context: Context) : WidgetPrefsStore<LoansWidgetConfig>(
    store = context.loansWidgetStore,
    keyPrefix = "loans_config",
    serializer = LoansWidgetConfig.serializer(),
    defaultValue = LoansWidgetConfig()
)
