package com.osscabinet.ossWallet.widget

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.preferencesDataStore

private val Context.accountsWidgetStore: DataStore<Preferences> by preferencesDataStore("accounts_widget_prefs")

class AccountsWidgetPrefsStore(context: Context) : WidgetPrefsStore<AccountsWidgetConfig>(
    store = context.accountsWidgetStore,
    keyPrefix = "accounts_config",
    serializer = AccountsWidgetConfig.serializer(),
    defaultValue = AccountsWidgetConfig()
)
