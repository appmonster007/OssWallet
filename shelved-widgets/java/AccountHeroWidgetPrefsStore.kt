package com.osscabinet.ossWallet.widget

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.preferencesDataStore

private val Context.accountHeroWidgetStore: DataStore<Preferences> by preferencesDataStore("account_hero_widget_prefs")

class AccountHeroWidgetPrefsStore(context: Context) : WidgetPrefsStore<AccountHeroWidgetConfig>(
    store = context.accountHeroWidgetStore,
    keyPrefix = "account_hero_config",
    serializer = AccountHeroWidgetConfig.serializer(),
    defaultValue = AccountHeroWidgetConfig()
)
