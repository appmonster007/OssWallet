package com.osscabinet.ossWallet.widget

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.preferencesDataStore

private val Context.cashFlowComparisonWidgetStore: DataStore<Preferences> by preferencesDataStore("cashflow_comparison_widget_prefs")

class CashFlowComparisonWidgetPrefsStore(context: Context) : WidgetPrefsStore<CashFlowComparisonWidgetConfig>(
    store = context.cashFlowComparisonWidgetStore,
    keyPrefix = "cashflow_comparison_config",
    serializer = CashFlowComparisonWidgetConfig.serializer(),
    defaultValue = CashFlowComparisonWidgetConfig()
)
