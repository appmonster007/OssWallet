package com.osscabinet.ossWallet.ui.util

import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

object TimeUtils {
    
    fun formatDateTime(millis: Long, is24Hour: Boolean): String {
        val pattern = if (is24Hour) "MMM dd, yyyy HH:mm" else "MMM dd, yyyy hh:mm a"
        val formatter = DateTimeFormatter.ofPattern(pattern, Locale.getDefault())
            .withZone(ZoneId.systemDefault())
        return formatter.format(Instant.ofEpochMilli(millis))
    }

    fun formatTime(millis: Long, is24Hour: Boolean): String {
        val pattern = if (is24Hour) "HH:mm" else "hh:mm a"
        val formatter = DateTimeFormatter.ofPattern(pattern, Locale.getDefault())
            .withZone(ZoneId.systemDefault())
        return formatter.format(Instant.ofEpochMilli(millis))
    }

    fun formatDate(millis: Long): String {
        val formatter = DateTimeFormatter.ofPattern("MMM dd, yyyy", Locale.getDefault())
            .withZone(ZoneId.systemDefault())
        return formatter.format(Instant.ofEpochMilli(millis))
    }
}
