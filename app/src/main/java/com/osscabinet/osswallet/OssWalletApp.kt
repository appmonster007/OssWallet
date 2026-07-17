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
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import jakarta.inject.Inject

@HiltAndroidApp
class OssWalletApp : Application(), Configuration.Provider {

    @Inject
    lateinit var workerFactory: HiltWorkerFactory

    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder()
            .setWorkerFactory(workerFactory)
            .build()

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

        // Eagerly open remaining SQLCipher connection pool slots so PBKDF2 key derivation
        // happens during app startup (overlapping with Activity/Compose inflation) rather than
        // when the DashboardViewModel subscribes to 12+ flows and triggers pool expansion.
        // The observer above opens the first connection; we open the remaining 2 (pool max = 2
        // readers + 1 writer). They serialize through arch_disk_io but start ~4s earlier.
        repeat(2) {
            applicationScope.launch(kotlinx.coroutines.Dispatchers.IO) {
                try { db.query("SELECT 1", null).close() } catch (_: Exception) {}
            }
        }

        createNotificationChannels()
        PlannedPaymentWorker.schedule(this)

        // Reschedule auto-backup on app startup so it survives device reboots and reinstalls
        applicationScope.launch {
            val config = settingsRepository.autoBackupConfig.first()
            backupScheduler.schedule(config)
        }

        // ARC-19: clear process-wide caches on profile change so the newly-active profile
        // doesn't see stale aggregations or card data computed for the previous profile.
        // Track previousProfileId so we evict the OLD profile's render slots (not the new one).
        var previousProfileId: Int? = null
        settingsRepository.currentProfileId
            .distinctUntilChanged()
            .onEach { profileId ->
                val prev = previousProfileId
                if (prev != null) {
                    queryCache.clear()
                    // Belt-and-suspenders: CardRenderCache keys already include (profileId, currency),
                    // so a profile switch naturally misses the cache. We still clear the previous
                    // profile's slots to keep the cache from growing unboundedly across many switches.
                    cardRenderCache.invalidateProfile(prev)
                }
                previousProfileId = profileId
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
