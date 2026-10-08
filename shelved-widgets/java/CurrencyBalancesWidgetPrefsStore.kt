package com.osscabinet.ossWallet.widget

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.preferencesDataStore

private val Context.currencyBalancesWidgetStore: DataStore<Preferences> by preferencesDataStore("currency_balances_widget_prefs")

class CurrencyBalancesWidgetPrefsStore(context: Context) : WidgetPrefsStore<CurrencyBalancesWidgetConfig>(
    store = context.currencyBalancesWidgetStore,
    keyPrefix = "currency_balances_config",
    serializer = CurrencyBalancesWidgetConfig.serializer(),
    defaultValue = CurrencyBalancesWidgetConfig()
)
