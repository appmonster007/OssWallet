package com.osscabinet.ossWallet

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.os.Build
import com.osscabinet.ossWallet.di.AppContainer
import com.osscabinet.ossWallet.util.PlannedPaymentWorker

class OssWalletApp : Application() {
    lateinit var container: AppContainer

    override fun onCreate() {
        super.onCreate()

        // Initialize osmdroid
        org.osmdroid.config.Configuration.getInstance().load(
            this,
            getSharedPreferences("osmdroid", MODE_PRIVATE)
        )

        container = AppContainer(this)
        // Start the Room InvalidationTracker observer so any DB write (including seeder,
        // backup restore, or direct DAO calls that bypass TransactionManager) still triggers
        // a widget refresh via WidgetRefreshTrigger.
        container.startWidgetObserver()
        createNotificationChannels()
        PlannedPaymentWorker.schedule(this)
        // Note: widget refresh is event-driven via WorkManagerWidgetRefreshTrigger and
        // WidgetInvalidationObserver. No periodic WorkManager schedule is needed here —
        // the widget XML sets updatePeriodMillis for system-periodic updates, and the
        // NetWorthWidgetProvider.onReceive() forwards those to WidgetRefreshWorker.
    }

    private fun createNotificationChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(
                NotificationChannel(
                    "planned_payments",
                    "Planned Payments",
                    NotificationManager.IMPORTANCE_DEFAULT
                ).apply { description = "Reminders for upcoming planned payments" }
            )
        }
    }
}
