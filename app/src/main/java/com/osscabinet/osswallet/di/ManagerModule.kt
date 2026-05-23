package com.osscabinet.ossWallet.di

import com.osscabinet.ossWallet.domain.repository.PlannedPaymentRepository
import com.osscabinet.ossWallet.domain.service.WidgetRefreshTrigger
import com.osscabinet.ossWallet.domain.service.management.TransactionManager
import com.osscabinet.ossWallet.domain.usecase.TransactionUseCases
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object ManagerModule {

    @Provides
    @Singleton
    fun provideTransactionManager(
        transactionUseCases: TransactionUseCases,
        plannedPaymentRepository: PlannedPaymentRepository,
        widgetRefreshTrigger: WidgetRefreshTrigger
    ): TransactionManager {
        return TransactionManager(
            transactionUseCases,
            plannedPaymentRepository,
            widgetRefreshTrigger
        )
    }
}
