package com.osscabinet.ossWallet.widget

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.preferencesDataStore

private val Context.tagUpSetPlotWidgetStore: DataStore<Preferences> by preferencesDataStore("tag_upset_plot_widget_prefs")

class TagUpSetPlotWidgetPrefsStore(context: Context) : WidgetPrefsStore<TagUpSetPlotWidgetConfig>(
    store = context.tagUpSetPlotWidgetStore,
    keyPrefix = "tag_upset_plot_config",
    serializer = TagUpSetPlotWidgetConfig.serializer(),
    defaultValue = TagUpSetPlotWidgetConfig()
)
