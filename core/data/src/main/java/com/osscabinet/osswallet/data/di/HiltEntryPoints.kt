package com.osscabinet.ossWallet.data.di

import com.osscabinet.ossWallet.data.local.SettingsDataStore
import com.osscabinet.ossWallet.domain.repository.BackupRepository
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@EntryPoint
@InstallIn(SingletonComponent::class)
interface AutoBackupWorkerEntryPoint {
    fun backupRepository(): BackupRepository
    fun settingsDataStore(): SettingsDataStore
}
