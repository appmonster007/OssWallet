package com.osscabinet.ossWallet.di

import android.content.Context
import com.osscabinet.ossWallet.data.web.WebServerManager
import com.osscabinet.ossWallet.data.web.WebServerPreferences
import com.osscabinet.ossWallet.domain.repository.WebServerRepository
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import jakarta.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class WebServerModule {

    @Binds
    @Singleton
    abstract fun bindWebServerRepository(impl: WebServerManager): WebServerRepository

    companion object {
        @Provides
        @Singleton
        fun provideWebServerPreferences(@ApplicationContext context: Context): WebServerPreferences {
            return WebServerPreferences(context)
        }
    }
}
