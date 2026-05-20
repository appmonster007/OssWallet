package com.osscabinet.ossWallet

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.os.Build
import com.osscabinet.ossWallet.R
import com.osscabinet.ossWallet.data.local.WidgetInvalidationObserver
import com.osscabinet.ossWallet.data.local.query.QueryCache
import com.osscabinet.ossWallet.data.local.AppDatabase
import com.osscabinet.ossWallet.data.local.SettingsDataStore
import com.osscabinet.ossWallet.data.seed.SeedingConfig
import com.osscabinet.ossWallet.domain.service.WidgetRefreshTrigger
import com.osscabinet.ossWallet.util.PlannedPaymentWorker
import dagger.hilt.android.HiltAndroidApp
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
