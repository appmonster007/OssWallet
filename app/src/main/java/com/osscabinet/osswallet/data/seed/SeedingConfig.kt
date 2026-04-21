package com.osscabinet.ossWallet.data.seed

import android.content.Context
import com.osscabinet.ossWallet.BuildConfig
import com.osscabinet.ossWallet.data.local.AppDatabase
import com.osscabinet.ossWallet.data.local.SettingsDataStore

/**
 * Central configuration for database seeding.
 * 
 * TO DISABLE SEEDING:
 * 1. Set ENABLE_SEEDING = false in app/build.gradle.kts
 * 2. Or comment out the seeding call in MainActivity
 * 
 * TO REMOVE SEEDING COMPLETELY:
 * 1. Delete the entire /data/seed package
 * 2. Remove the seeding call from MainActivity
 * 3. Remove ENABLE_SEEDING from build.gradle.kts
 */
object SeedingConfig {

    /**
     * Master toggle for seeding functionality.
     * Change this to false to disable seeding globally.
     */
    val SEEDING_ENABLED = BuildConfig.ENABLE_SEEDING && BuildConfig.DEBUG

    /**
     * Check if seeding should run based on configuration
     */
    fun shouldSeed(): Boolean = SEEDING_ENABLED

    /**
     * Creates a DatabaseSeeder instance only if seeding is enabled.
     * Returns null if seeding is disabled.
     */
    fun createSeeder(
        database: AppDatabase,
        settingsDataStore: SettingsDataStore,
        context: Context
    ): DatabaseSeeder? {
        return if (shouldSeed()) {
            DatabaseSeeder(
                accountDao = database.accountDao(),
                categoryDao = database.categoryDao(),
                transactionDao = database.transactionDao(),
                filterDao = database.filterDao(),
                context = context,
                settingsDataStore = settingsDataStore
            )
        } else {
            null
        }
    }
}
