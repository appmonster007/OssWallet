package com.osscabinet.ossWallet.data.seed

import android.content.Context
import com.osscabinet.ossWallet.BuildConfig
import com.osscabinet.ossWallet.data.local.AppDatabase
import com.osscabinet.ossWallet.data.local.SettingsDataStore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

object SeedingConfig {

    private val seedingEnabled = BuildConfig.ENABLE_SEEDING

    fun launchSeedingIfEnabled(
        database: AppDatabase,
        settingsDataStore: SettingsDataStore,
        context: Context
    ) {
        if (!seedingEnabled) return
        CoroutineScope(Dispatchers.IO).launch {
            DatabaseSeeder(
                accountDao = database.accountDao(),
                categoryDao = database.categoryDao(),
                transactionDao = database.transactionDao(),
                transactionLinkDao = database.transactionLinkDao(),
                filterDao = database.filterDao(),
                profileDao = database.profileDao(),
                budgetDao = database.budgetDao(),
                loanDao = database.loanDao(),
                templateDao = database.templateDao(),
                plannedPaymentDao = database.plannedPaymentDao(),
                tagDao = database.tagDao(),
                context = context,
                settingsDataStore = settingsDataStore,
                database = database
            ).seed()
        }
    }

    suspend fun triggerManualSeeding(
        database: AppDatabase,
        settingsDataStore: SettingsDataStore,
        context: Context
    ) {
        DatabaseSeeder(
            accountDao = database.accountDao(),
            categoryDao = database.categoryDao(),
            transactionDao = database.transactionDao(),
            transactionLinkDao = database.transactionLinkDao(),
            filterDao = database.filterDao(),
            profileDao = database.profileDao(),
            budgetDao = database.budgetDao(),
            loanDao = database.loanDao(),
            templateDao = database.templateDao(),
            plannedPaymentDao = database.plannedPaymentDao(),
            tagDao = database.tagDao(),
            context = context,
            settingsDataStore = settingsDataStore,
            database = database
        ).seed()
    }
}
