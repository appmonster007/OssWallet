package com.osscabinet.ossWallet.widget

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.preferencesDataStore

private val Context.categoryPieChartWidgetStore: DataStore<Preferences> by preferencesDataStore("category_pie_chart_widget_prefs")

class CategoryPieChartWidgetPrefsStore(context: Context) : WidgetPrefsStore<CategoryPieChartWidgetConfig>(
    store = context.categoryPieChartWidgetStore,
    keyPrefix = "category_pie_chart_config",
    serializer = CategoryPieChartWidgetConfig.serializer(),
    defaultValue = CategoryPieChartWidgetConfig()
)
