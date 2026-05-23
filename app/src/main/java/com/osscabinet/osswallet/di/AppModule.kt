package com.osscabinet.ossWallet.di

import android.content.Context
import com.osscabinet.ossWallet.data.local.AppDatabase
import com.osscabinet.ossWallet.data.local.SettingsDataStore
import com.osscabinet.ossWallet.data.local.dao.TransactionDao
import com.osscabinet.ossWallet.data.local.query.DatabaseQueryServiceImpl
import com.osscabinet.ossWallet.data.local.query.QueryCache
import com.osscabinet.ossWallet.data.local.query.TransactionQueryBuilder
import com.osscabinet.ossWallet.data.remote.RetrofitClient
import com.osscabinet.ossWallet.data.remote.provider.ExchangeRateApiProvider
import com.osscabinet.ossWallet.data.remote.provider.FrankfurterProvider
import com.osscabinet.ossWallet.domain.service.WidgetRefreshTrigger
import com.osscabinet.ossWallet.domain.service.currency.provider.ExchangeRateProviderRegistry
import com.osscabinet.ossWallet.domain.service.query.AggregationPlanner
import com.osscabinet.ossWallet.domain.service.query.DatabaseQueryService
import com.osscabinet.ossWallet.widget.WorkManagerWidgetRefreshTrigger
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import javax.inject.Qualifier
import javax.inject.Singleton

@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class ApplicationScope

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
    fun provideSettingsDataStore(@ApplicationContext context: Context): SettingsDataStore {
        return SettingsDataStore(context)
    }

    @Provides
    @Singleton
    fun provideQueryCache(): QueryCache = QueryCache()

    @Provides
    @Singleton
    fun provideWidgetRefreshTrigger(
        @ApplicationContext context: Context,
        queryCache: QueryCache
    ): WidgetRefreshTrigger {
        return WorkManagerWidgetRefreshTrigger(context, queryCache)
    }

    @Provides
    @Singleton
    fun provideDatabaseQueryService(
        transactionDao: TransactionDao,
        queryCache: QueryCache,
        db: AppDatabase
    ): DatabaseQueryService {
        return DatabaseQueryServiceImpl(
            transactionDao,
            TransactionQueryBuilder(),
            AggregationPlanner(),
            queryCache,
            db.invalidationTracker
        )
    }

    @Provides
    @Singleton
    fun provideExchangeRateProviderRegistry(): ExchangeRateProviderRegistry {
        return ExchangeRateProviderRegistry(
            listOf(
                ExchangeRateApiProvider(RetrofitClient.exchangeRateApi),
                FrankfurterProvider(RetrofitClient.frankfurterApi)
            )
        )
    }
}
