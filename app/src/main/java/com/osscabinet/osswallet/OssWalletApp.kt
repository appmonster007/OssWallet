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
import com.osscabinet.ossWallet.domain.repository.WebServerRepository
import com.osscabinet.ossWallet.domain.service.BackupScheduler
import com.osscabinet.ossWallet.domain.service.WidgetRefreshTrigger
import com.osscabinet.ossWallet.service.WebServerService
import com.osscabinet.ossWallet.util.PlannedPaymentWorker
import com.osscabinet.ossWallet.widget.WidgetColors
import dagger.hilt.android.HiltAndroidApp
import androidx.hilt.work.HiltWorkerFactory
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ProcessLifecycleOwner
import androidx.work.Configuration
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.combine
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

    @Inject
    lateinit var webServerRepository: WebServerRepository

    @Inject
    lateinit var attachmentRepository: com.osscabinet.ossWallet.domain.repository.AttachmentRepository

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

        // Note: TRUNCATE journal mode uses a single connection (no reader pool), so the observer
        // above already keys the only connection via PBKDF2. No additional warm-up is needed.

        createNotificationChannels()
        PlannedPaymentWorker.schedule(this)
        com.osscabinet.ossWallet.reminder.ReminderWorker.schedule(this)
        com.osscabinet.ossWallet.reminder.RecurringScanWorker.schedule(this)

        // Receipts whose transaction was deleted elsewhere (bulk delete, account delete) are cleaned up here.
        applicationScope.launch { runCatching { attachmentRepository.removeOrphans() } }

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
                    widgetRefreshTrigger.trigger()
                }
                previousProfileId = profileId
            }
            .launchIn(applicationScope)

        // Currency and precision change what widgets display without touching any observed table.
        combine(settingsRepository.currency, settingsRepository.decimalPrecision) { currency, precision ->
            currency to precision
        }
            .distinctUntilChanged()
            .drop(1)
            .onEach { widgetRefreshTrigger.trigger() }
            .launchIn(applicationScope)

        // Widgets on the default "App" theme take the app's own theme (Light/Dark/Amoled). Set it
        // before the first widget render, and re-render them when the user changes it.
        settingsRepository.theme
            .distinctUntilChanged()
            .onEach { theme ->
                val changed = WidgetColors.appTheme != null && WidgetColors.appTheme != theme
                WidgetColors.appTheme = theme
                if (changed) widgetRefreshTrigger.trigger()
            }
            .launchIn(applicationScope)

        // Auto-stop WebServer if it shouldn't stay alive in background
        ProcessLifecycleOwner.get().lifecycle.addObserver(object : DefaultLifecycleObserver {
            override fun onStop(owner: LifecycleOwner) {
                applicationScope.launch {
                    val stayAlive = webServerRepository.stayAlive.first()
                    if (!stayAlive && webServerRepository.isRunning()) {
                        val intent = Intent(this@OssWalletApp, WebServerService::class.java).apply {
                            action = WebServerService.ACTION_STOP
                        }
                        startService(intent)
                    }
                }
            }
        })

        // Ensure it's disabled on startup if not staying alive
        applicationScope.launch {
            if (!webServerRepository.stayAlive.first()) {
                webServerRepository.setEnabled(false)
            }
        }

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
            manager.createNotificationChannel(
                NotificationChannel(
                    "recurring_suggestions",
                    "Recurring payment suggestions",
                    NotificationManager.IMPORTANCE_LOW
                ).apply { description = "Repeated payments that could become planned payments" }
            )
            manager.createNotificationChannel(
                NotificationChannel(
                    "budget_alerts",
                    getString(R.string.notification_channel_budget_alerts_name),
                    NotificationManager.IMPORTANCE_DEFAULT
                ).apply { description = getString(R.string.notification_channel_budget_alerts_description) }
            )
        }
    }
}
