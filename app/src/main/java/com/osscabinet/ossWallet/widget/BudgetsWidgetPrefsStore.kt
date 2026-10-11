package com.osscabinet.ossWallet.widget

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.preferencesDataStore

private val Context.budgetsWidgetStore: DataStore<Preferences> by preferencesDataStore("budgets_widget_prefs")

class BudgetsWidgetPrefsStore(context: Context) : WidgetPrefsStore<BudgetsWidgetConfig>(
    store = context.budgetsWidgetStore,
    keyPrefix = "budgets_config",
    serializer = BudgetsWidgetConfig.serializer(),
    defaultValue = BudgetsWidgetConfig()
)
