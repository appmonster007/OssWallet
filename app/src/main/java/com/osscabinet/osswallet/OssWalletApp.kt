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
import com.osscabinet.ossWallet.data.web.WebServerPreferences
import com.osscabinet.ossWallet.domain.service.WidgetRefreshTrigger
import com.osscabinet.ossWallet.service.WebServerService
import com.osscabinet.ossWallet.util.PlannedPaymentWorker
import dagger.hilt.android.HiltAndroidApp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
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
    lateinit var webServerPreferences: WebServerPreferences

    private val appScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
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

        SeedingConfig.launchSeedingIfEnabled(db, settingsDataStore, this)

        createNotificationChannels()
        PlannedPaymentWorker.schedule(this)

        appScope.launch {
            if (webServerPreferences.isEnabled.first()) {
                val intent = Intent(this@OssWalletApp, WebServerService::class.java).apply {
                    action = WebServerService.ACTION_START
                }
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    startForegroundService(intent)
                } else {
                    startService(intent)
                }
            }
        }
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
