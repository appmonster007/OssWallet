package com.osscabinet.ossWallet.di

import com.osscabinet.ossWallet.domain.repository.*
import com.osscabinet.ossWallet.domain.usecase.*
import com.osscabinet.ossWallet.domain.card.CardComputerRegistry
import com.osscabinet.ossWallet.domain.service.query.DatabaseQueryService
import com.osscabinet.ossWallet.domain.card.computers.*
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object UseCaseModule {

    @Provides
    @Singleton
    fun provideTransactionUseCases(
        transactionRepository: TransactionRepository,
        sharedWalletRepository: SharedWalletRepository
    ): TransactionUseCases {
        return TransactionUseCases(transactionRepository, sharedWalletRepository)
    }

    @Provides
    @Singleton
    fun provideAccountUseCases(
        accountRepository: AccountRepository,
        transactionRepository: TransactionRepository
    ): AccountUseCases {
        return AccountUseCases(accountRepository, transactionRepository)
    }

    @Provides
    @Singleton
    fun provideCardComputerRegistry(databaseQueryService: DatabaseQueryService): CardComputerRegistry {
        return CardComputerRegistry(listOf(
            NetWorthCardComputer(databaseQueryService),
            CashFlowCardComputer(databaseQueryService),
            CurrencyBalancesCardComputer(databaseQueryService),
            AccountsCardComputer(databaseQueryService),
            AccountHeroCardComputer(databaseQueryService),
            RecentActivitiesCardComputer(databaseQueryService),
            BudgetsCardComputer(),
            LoansCardComputer(),
            PlannedPaymentsCardComputer(),
            CategoryPieChartCardComputer(),
            CategoryTreemapCardComputer(),
            CashFlowComparisonCardComputer(databaseQueryService),
            IncomeExpenseBarCardComputer(databaseQueryService),
            CategorySpendingTrendCardComputer(),
            TagUpSetPlotCardComputer(databaseQueryService)
        ))
    }

    @Provides
    @Singleton
    fun provideComputeCardUseCase(cardComputerRegistry: CardComputerRegistry): ComputeCardUseCase {
        return ComputeCardUseCase(cardComputerRegistry)
    }
}
