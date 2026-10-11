package com.osscabinet.ossWallet.widget

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.preferencesDataStore

private val Context.categoryTreemapWidgetStore: DataStore<Preferences> by preferencesDataStore("category_treemap_widget_prefs")

class CategoryTreemapWidgetPrefsStore(context: Context) : WidgetPrefsStore<CategoryTreemapWidgetConfig>(
    store = context.categoryTreemapWidgetStore,
    keyPrefix = "category_treemap_config",
    serializer = CategoryTreemapWidgetConfig.serializer(),
    defaultValue = CategoryTreemapWidgetConfig()
)
