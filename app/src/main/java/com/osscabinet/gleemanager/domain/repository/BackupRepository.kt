package com.osscabinet.gleemanager.domain.repository

import android.net.Uri

interface BackupRepository {
    suspend fun exportData(uri: Uri): Result<Unit>
    suspend fun importData(uri: Uri): Result<Unit>
}
