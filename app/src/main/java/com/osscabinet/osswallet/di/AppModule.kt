package com.osscabinet.ossWallet.di

import android.content.Context
import com.osscabinet.ossWallet.data.di.qualifiers.ApplicationScope
import com.osscabinet.ossWallet.data.local.query.QueryCache
import com.osscabinet.ossWallet.domain.service.WidgetRefreshTrigger
import com.osscabinet.ossWallet.widget.WorkManagerWidgetRefreshTrigger
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import jakarta.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides
    @Singleton
    @ApplicationScope
    fun provideApplicationScope(): CoroutineScope =
        CoroutineScope(SupervisorJob() + Dispatchers.Default)

    @Provides
    @Singleton
    fun provideWidgetRefreshTrigger(
        @ApplicationContext context: Context,
        queryCache: QueryCache
    ): WidgetRefreshTrigger {
        return WorkManagerWidgetRefreshTrigger(context, queryCache)
    }
}
