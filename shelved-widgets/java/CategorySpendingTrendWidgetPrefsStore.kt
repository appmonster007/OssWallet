package com.osscabinet.ossWallet.widget

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.preferencesDataStore

private val Context.categorySpendingTrendWidgetStore: DataStore<Preferences> by preferencesDataStore("category_spending_trend_widget_prefs")

class CategorySpendingTrendWidgetPrefsStore(context: Context) : WidgetPrefsStore<CategorySpendingTrendWidgetConfig>(
    store = context.categorySpendingTrendWidgetStore,
    keyPrefix = "category_spending_trend_config",
    serializer = CategorySpendingTrendWidgetConfig.serializer(),
    defaultValue = CategorySpendingTrendWidgetConfig()
)
