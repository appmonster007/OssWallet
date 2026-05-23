package com.osscabinet.ossWallet.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.osscabinet.ossWallet.MainActivity
import com.osscabinet.ossWallet.R
import com.osscabinet.ossWallet.domain.model.web.WebServerState
import com.osscabinet.ossWallet.domain.repository.WebServerRepository
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class WebServerService : Service() {

    @Inject
    lateinit var webServerRepository: WebServerRepository

    private val serviceScope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START -> startWebServer()
            ACTION_STOP -> stopWebServer()
            else -> stopSelf()
        }
        return START_NOT_STICKY
    }

    private fun startWebServer() {
        startForeground(NOTIFICATION_ID, buildNotification("Starting web server..."))
        serviceScope.launch {
            webServerRepository.start()
            webServerRepository.serverState.collectLatest { state ->
                val notificationText = when (state) {
                    is WebServerState.Running -> "Active at ${state.accessUrl}"
                    is WebServerState.Starting -> "Starting web server..."
                    is WebServerState.Error -> "Error: ${state.message}"
                    is WebServerState.Stopped -> {
                        stopSelf()
                        return@collectLatest
                    }
                }
                val nm = getSystemService(NotificationManager::class.java)
                nm.notify(NOTIFICATION_ID, buildNotification(notificationText))
            }
        }
    }

    private fun stopWebServer() {
        serviceScope.launch {
            webServerRepository.stop()
            stopForeground(STOP_FOREGROUND_REMOVE)
            stopSelf()
        }
    }

    private fun buildNotification(text: String): Notification {
        val mainIntent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP
        }
        val mainPi = PendingIntent.getActivity(
            this, 0, mainIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val stopIntent = Intent(this, WebServerService::class.java).apply {
            action = ACTION_STOP
        }
        val stopPi = PendingIntent.getService(
            this, 1, stopIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("OssWallet Web Access")
            .setContentText(text)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentIntent(mainPi)
            .addAction(0, "Stop", stopPi)
            .setOngoing(true)
            .setShowWhen(false)
            .build()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Web Server",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Shown while web access is active"
                setShowBadge(false)
            }
            getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
        }
    }

    override fun onDestroy() {
        serviceScope.cancel()
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    companion object {
        const val ACTION_START = "com.osscabinet.ossWallet.WEB_SERVER_START"
        const val ACTION_STOP = "com.osscabinet.ossWallet.WEB_SERVER_STOP"
        private const val NOTIFICATION_ID = 2001
        private const val CHANNEL_ID = "oss_web_server"
    }
}
