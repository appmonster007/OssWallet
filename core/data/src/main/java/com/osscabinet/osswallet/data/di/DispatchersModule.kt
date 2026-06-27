package com.osscabinet.ossWallet.data.di

import com.osscabinet.ossWallet.core.util.DispatcherProvider
import com.osscabinet.ossWallet.core.util.RealDispatcherProvider
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DispatchersModule {

    @Provides
    @Singleton
    fun provideDispatcherProvider(): DispatcherProvider = RealDispatcherProvider()
}
