package com.osscabinet.ossWallet.data.seed

import android.content.Context
import com.osscabinet.ossWallet.data.local.AppDatabase
import com.osscabinet.ossWallet.data.local.SettingsDataStore

object SeedingConfig {

    fun launchSeedingIfEnabled(
        database: AppDatabase,
        settingsDataStore: SettingsDataStore,
        context: Context
    ) {
        // no-op in release builds
    }
}
