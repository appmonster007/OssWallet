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
        container = AppContainer(this)
        createNotificationChannels()
        PlannedPaymentWorker.schedule(this)
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
