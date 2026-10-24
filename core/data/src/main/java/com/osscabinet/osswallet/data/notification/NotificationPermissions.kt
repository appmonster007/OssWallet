package com.osscabinet.ossWallet.data.notification

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat

/**
 * Single source of truth for "would a notification posted now actually be shown" — the
 * POST_NOTIFICATIONS runtime permission (API 33+) plus the user's notifications-enabled setting.
 * Lives in core/data (not app) so both app-module notifiers and core/data workers can share it
 * without a reverse app->data dependency.
 */
object NotificationPermissions {
    fun canPost(context: Context): Boolean {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) !=
            PackageManager.PERMISSION_GRANTED
        ) return false
        return NotificationManagerCompat.from(context).areNotificationsEnabled()
    }
}
