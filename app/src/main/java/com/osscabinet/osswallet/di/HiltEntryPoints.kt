package com.osscabinet.ossWallet.di

import com.osscabinet.ossWallet.data.local.AppDatabase
import com.osscabinet.ossWallet.data.local.SettingsDataStore
import com.osscabinet.ossWallet.domain.repository.AccountRepository
import com.osscabinet.ossWallet.domain.repository.CategoryRepository
import com.osscabinet.ossWallet.domain.repository.PlannedPaymentRepository
import com.osscabinet.ossWallet.domain.repository.SettingsRepository
import com.osscabinet.ossWallet.domain.service.management.TransactionManager
import com.osscabinet.ossWallet.domain.service.query.DatabaseQueryService
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@EntryPoint
@InstallIn(SingletonComponent::class)
interface WidgetDataLoaderEntryPoint {
    fun settingsDataStore(): SettingsDataStore
    fun db(): AppDatabase
    fun databaseQueryService(): DatabaseQueryService
    fun categoryRepository(): CategoryRepository
    fun accountRepository(): AccountRepository
}

@EntryPoint
@InstallIn(SingletonComponent::class)
interface PlannedPaymentWorkerEntryPoint {
    fun plannedPaymentRepository(): PlannedPaymentRepository
    fun settingsRepository(): SettingsRepository
    fun categoryRepository(): CategoryRepository
    fun transactionManager(): TransactionManager
}
