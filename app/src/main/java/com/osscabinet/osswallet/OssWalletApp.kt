package com.osscabinet.ossWallet

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Intent
import android.os.Build
import com.osscabinet.ossWallet.R
import com.osscabinet.ossWallet.data.local.WidgetInvalidationObserver
import com.osscabinet.ossWallet.data.local.query.QueryCache
import com.osscabinet.ossWallet.data.local.AppDatabase
import com.osscabinet.ossWallet.data.local.SettingsDataStore
import com.osscabinet.ossWallet.data.seed.SeedingConfig
import com.osscabinet.ossWallet.domain.cache.CardRenderCache
import com.osscabinet.ossWallet.domain.repository.SettingsRepository
import com.osscabinet.ossWallet.domain.service.BackupScheduler
import com.osscabinet.ossWallet.domain.service.WidgetRefreshTrigger
import com.osscabinet.ossWallet.util.PlannedPaymentWorker
import dagger.hilt.android.HiltAndroidApp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltAndroidApp
class OssWalletApp : Application() {

    @Inject
    lateinit var db: AppDatabase

    @Inject
    lateinit var queryCache: QueryCache

    @Inject
    lateinit var settingsDataStore: SettingsDataStore

    @Inject
    lateinit var widgetRefreshTrigger: WidgetRefreshTrigger

    @Inject
    lateinit var settingsRepository: SettingsRepository

    @Inject
    lateinit var backupScheduler: BackupScheduler

    @Inject
    lateinit var cardRenderCache: CardRenderCache

    private val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    private var widgetInvalidationObserver: WidgetInvalidationObserver? = null

    override fun onCreate() {
        super.onCreate()

        // Initialize osmdroid
        org.osmdroid.config.Configuration.getInstance().load(
            this,
            getSharedPreferences("osmdroid", MODE_PRIVATE)
        )

        // Start the Room InvalidationTracker observer so any DB write (including seeder,
        // backup restore, or direct DAO calls that bypass TransactionManager) still triggers
        // a widget refresh via WidgetRefreshTrigger.
        widgetInvalidationObserver = WidgetInvalidationObserver(db, queryCache, widgetRefreshTrigger).also { it.start() }

        createNotificationChannels()
        PlannedPaymentWorker.schedule(this)

        // Reschedule auto-backup on app startup so it survives device reboots and reinstalls
        applicationScope.launch {
            val config = settingsRepository.autoBackupConfig.first()
            backupScheduler.schedule(config)
        }

        // ARC-19: clear process-wide caches on profile change so the newly-active profile
        // doesn't see stale aggregations or card data computed for the previous profile.
        // drop(1) skips the initial emission so we don't no-op-clear on app start.
        settingsRepository.currentProfileId
            .distinctUntilChanged()
            .drop(1)
            .onEach { newProfileId ->
                queryCache.clear()
                // Belt-and-suspenders: CardRenderCache keys already include (profileId, currency),
                // so a profile switch naturally misses the cache. We still clear the previous
                // profile's slots to keep the cache from growing unboundedly across many switches.
                cardRenderCache.invalidateProfile(newProfileId)
            }
            .launchIn(applicationScope)

        // We don't auto-start the WebServerService here anymore to prevent the app 
        // from staying alive in the background unnecessarily. 
        // The user can start it manually from the "Open in Web" screen.
    }

    private fun createNotificationChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(
                NotificationChannel(
                    "planned_payments",
                    getString(R.string.notification_channel_planned_payments_name),
                    NotificationManager.IMPORTANCE_DEFAULT
                ).apply { description = getString(R.string.notification_channel_planned_payments_description) }
            )
        }
    }
}
