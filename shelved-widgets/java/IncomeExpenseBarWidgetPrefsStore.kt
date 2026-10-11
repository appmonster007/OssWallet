package com.osscabinet.ossWallet.widget

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.preferencesDataStore

private val Context.incomeExpenseBarWidgetStore: DataStore<Preferences> by preferencesDataStore("income_expense_bar_widget_prefs")

class IncomeExpenseBarWidgetPrefsStore(context: Context) : WidgetPrefsStore<IncomeExpenseBarWidgetConfig>(
    store = context.incomeExpenseBarWidgetStore,
    keyPrefix = "income_expense_bar_config",
    serializer = IncomeExpenseBarWidgetConfig.serializer(),
    defaultValue = IncomeExpenseBarWidgetConfig()
)
