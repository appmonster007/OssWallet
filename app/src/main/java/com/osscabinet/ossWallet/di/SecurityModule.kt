package com.osscabinet.ossWallet.di

import com.osscabinet.ossWallet.domain.service.security.AppLockGate
import com.osscabinet.ossWallet.ui.security.AppLockManager
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import jakarta.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class SecurityModule {
    @Binds
    @Singleton
    abstract fun bindAppLockGate(impl: AppLockManager): AppLockGate
}
