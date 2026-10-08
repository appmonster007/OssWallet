package com.osscabinet.ossWallet.widget

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.preferencesDataStore

private val Context.plannedPaymentsWidgetStore: DataStore<Preferences> by preferencesDataStore("planned_payments_widget_prefs")

class PlannedPaymentsWidgetPrefsStore(context: Context) : WidgetPrefsStore<PlannedPaymentsWidgetConfig>(
    store = context.plannedPaymentsWidgetStore,
    keyPrefix = "planned_payments_config",
    serializer = PlannedPaymentsWidgetConfig.serializer(),
    defaultValue = PlannedPaymentsWidgetConfig()
)
