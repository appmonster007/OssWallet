package com.osscabinet.ossWallet.domain.service

import com.osscabinet.ossWallet.domain.repository.BackupScope
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/** Single source of truth for backup filenames, shared by the auto-backup repository and manual export UI. */
object BackupFileNaming {
    private const val PATTERN = "yyyy_MM_dd_HHmmss"

    enum class Kind(val prefix: String) {
        AUTO("OssWallet_auto_backup"),
        MANUAL("OssWallet_backup")
    }

    fun build(kind: Kind, scope: BackupScope?, extension: String, encrypted: Boolean): String {
        val timestamp = SimpleDateFormat(PATTERN, Locale.US).format(Date())
        val scopeSuffix = when (scope) {
            BackupScope.SINGLE_PROFILE -> "_profile"
            BackupScope.ALL_PROFILES -> "_all"
            null -> ""
        }
        val encSuffix = if (encrypted) ".enc" else ""
        return "${kind.prefix}_$timestamp$scopeSuffix.$extension$encSuffix"
    }
}
