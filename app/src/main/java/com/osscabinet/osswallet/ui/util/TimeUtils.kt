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

    fun formatRelativeDate(dateMillis: Long, now: Long): String {
        val zone = ZoneId.systemDefault()
        val target = Instant.ofEpochMilli(dateMillis).atZone(zone).toLocalDate()
        val today = Instant.ofEpochMilli(now).atZone(zone).toLocalDate()
        val diffDays = java.time.temporal.ChronoUnit.DAYS.between(target, today)
        return when {
            target == today -> "Today"
            target == today.plusDays(1) -> "Tomorrow"
            target.isAfter(today) -> DateTimeFormatter.ofPattern("MMM d").format(target)
            diffDays == 1L -> "Yesterday"
            diffDays < 7 -> "$diffDays days ago"
            else -> DateTimeFormatter.ofPattern("MMM d").format(target)
        }
    }
}
