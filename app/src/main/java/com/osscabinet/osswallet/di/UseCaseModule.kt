package com.osscabinet.ossWallet.di

import com.osscabinet.ossWallet.domain.card.CardComputerRegistry
import com.osscabinet.ossWallet.domain.card.computers.AccountHeroCardComputer
import com.osscabinet.ossWallet.domain.card.computers.AccountsCardComputer
import com.osscabinet.ossWallet.domain.card.computers.BudgetsCardComputer
import com.osscabinet.ossWallet.domain.card.computers.CashFlowCardComputer
import com.osscabinet.ossWallet.domain.card.computers.CashFlowComparisonCardComputer
import com.osscabinet.ossWallet.domain.card.computers.CategoryPieChartCardComputer
import com.osscabinet.ossWallet.domain.card.computers.CategorySpendingTrendCardComputer
import com.osscabinet.ossWallet.domain.card.computers.CategoryTreemapCardComputer
import com.osscabinet.ossWallet.domain.card.computers.CurrencyBalancesCardComputer
import com.osscabinet.ossWallet.domain.card.computers.IncomeExpenseBarCardComputer
import com.osscabinet.ossWallet.domain.card.computers.LoansCardComputer
import com.osscabinet.ossWallet.domain.card.computers.NetWorthCardComputer
import com.osscabinet.ossWallet.domain.card.computers.PlannedPaymentsCardComputer
import com.osscabinet.ossWallet.domain.card.computers.RecentActivitiesCardComputer
import com.osscabinet.ossWallet.domain.card.computers.TagUpSetPlotCardComputer
import com.osscabinet.ossWallet.domain.repository.AccountRepository
import com.osscabinet.ossWallet.domain.repository.SharedWalletRepository
import com.osscabinet.ossWallet.domain.repository.TransactionRepository
import com.osscabinet.ossWallet.domain.service.query.DatabaseQueryService
import com.osscabinet.ossWallet.domain.usecase.AccountUseCases
import com.osscabinet.ossWallet.domain.usecase.ComputeCardUseCase
import com.osscabinet.ossWallet.domain.usecase.TransactionUseCases
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
        return CardComputerRegistry(
            listOf(
                NetWorthCardComputer(databaseQueryService),
                CashFlowCardComputer(databaseQueryService),
                CurrencyBalancesCardComputer(databaseQueryService),
                AccountsCardComputer(databaseQueryService),
                AccountHeroCardComputer(databaseQueryService),
                RecentActivitiesCardComputer(),
                BudgetsCardComputer(),
                LoansCardComputer(),
                PlannedPaymentsCardComputer(),
                CategoryPieChartCardComputer(),
                CategoryTreemapCardComputer(),
                CashFlowComparisonCardComputer(databaseQueryService),
                IncomeExpenseBarCardComputer(databaseQueryService),
                CategorySpendingTrendCardComputer(),
                TagUpSetPlotCardComputer(databaseQueryService)
            )
        )
    }

    @Provides
    @Singleton
    fun provideComputeCardUseCase(cardComputerRegistry: CardComputerRegistry): ComputeCardUseCase {
        return ComputeCardUseCase(cardComputerRegistry)
    }
}
