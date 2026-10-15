package com.osscabinet.ossWallet.data.di

import com.osscabinet.ossWallet.data.repository.AccountRepositoryImpl
import com.osscabinet.ossWallet.data.repository.AppLockRepositoryImpl
import com.osscabinet.ossWallet.data.repository.BackupRepositoryImpl
import com.osscabinet.ossWallet.data.repository.BudgetRepositoryImpl
import com.osscabinet.ossWallet.data.repository.CardRepositoryImpl
import com.osscabinet.ossWallet.data.repository.CategoryRepositoryImpl
import com.osscabinet.ossWallet.data.repository.ComputationRepositoryImpl
import com.osscabinet.ossWallet.data.repository.CurrencyRepositoryImpl
import com.osscabinet.ossWallet.data.repository.FilterRepositoryImpl
import com.osscabinet.ossWallet.data.repository.LoanRepositoryImpl
import com.osscabinet.ossWallet.data.repository.PlannedPaymentRepositoryImpl
import com.osscabinet.ossWallet.data.repository.ProfileRepositoryImpl
import com.osscabinet.ossWallet.data.repository.ReminderRepositoryImpl
import com.osscabinet.ossWallet.data.repository.SettingsRepositoryImpl
import com.osscabinet.ossWallet.data.repository.SharedWalletRepositoryImpl
import com.osscabinet.ossWallet.data.repository.TagRepositoryImpl
import com.osscabinet.ossWallet.data.repository.TemplateRepositoryImpl
import com.osscabinet.ossWallet.data.repository.TransactionRepositoryImpl
import com.osscabinet.ossWallet.data.service.BackupSchedulerImpl
import com.osscabinet.ossWallet.data.service.CsvImportServiceImpl
import com.osscabinet.ossWallet.domain.repository.AccountRepository
import com.osscabinet.ossWallet.domain.repository.AppLockRepository
import com.osscabinet.ossWallet.domain.repository.BackupRepository
import com.osscabinet.ossWallet.domain.repository.BudgetRepository
import com.osscabinet.ossWallet.domain.repository.CardRepository
import com.osscabinet.ossWallet.domain.repository.CategoryRepository
import com.osscabinet.ossWallet.domain.repository.ComputationRepository
import com.osscabinet.ossWallet.domain.repository.CurrencyRepository
import com.osscabinet.ossWallet.domain.repository.FilterRepository
import com.osscabinet.ossWallet.domain.repository.LoanRepository
import com.osscabinet.ossWallet.domain.repository.PlannedPaymentRepository
import com.osscabinet.ossWallet.domain.repository.ProfileRepository
import com.osscabinet.ossWallet.domain.repository.ReminderRepository
import com.osscabinet.ossWallet.domain.repository.SettingsRepository
import com.osscabinet.ossWallet.domain.repository.SharedWalletRepository
import com.osscabinet.ossWallet.domain.repository.TagRepository
import com.osscabinet.ossWallet.domain.repository.TemplateRepository
import com.osscabinet.ossWallet.domain.repository.TransactionRepository
import com.osscabinet.ossWallet.domain.service.BackupScheduler
import com.osscabinet.ossWallet.domain.service.CsvImportService
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import jakarta.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {

    @Binds
    @Singleton
    abstract fun bindAccountRepository(impl: AccountRepositoryImpl): AccountRepository
    @Binds
    @Singleton
    abstract fun bindTransactionRepository(impl: TransactionRepositoryImpl): TransactionRepository
    @Binds
    @Singleton
    abstract fun bindCategoryRepository(impl: CategoryRepositoryImpl): CategoryRepository
    @Binds
    @Singleton
    abstract fun bindTagRepository(impl: TagRepositoryImpl): TagRepository
    @Binds
    @Singleton
    abstract fun bindTemplateRepository(impl: TemplateRepositoryImpl): TemplateRepository
    @Binds
    @Singleton
    abstract fun bindBudgetRepository(impl: BudgetRepositoryImpl): BudgetRepository
    @Binds
    @Singleton
    abstract fun bindLoanRepository(impl: LoanRepositoryImpl): LoanRepository
    @Binds
    @Singleton
    abstract fun bindPlannedPaymentRepository(impl: PlannedPaymentRepositoryImpl): PlannedPaymentRepository
    @Binds
    @Singleton
    abstract fun bindFilterRepository(impl: FilterRepositoryImpl): FilterRepository
    @Binds
    @Singleton
    abstract fun bindCurrencyRepository(impl: CurrencyRepositoryImpl): CurrencyRepository
    @Binds
    @Singleton
    abstract fun bindProfileRepository(impl: ProfileRepositoryImpl): ProfileRepository
    @Binds
    @Singleton
    abstract fun bindReminderRepository(impl: ReminderRepositoryImpl): ReminderRepository
    @Binds
    @Singleton
    abstract fun bindAppLockRepository(impl: AppLockRepositoryImpl): AppLockRepository
    @Binds
    @Singleton
    abstract fun bindSettingsRepository(impl: SettingsRepositoryImpl): SettingsRepository
    @Binds
    @Singleton
    abstract fun bindCardRepository(impl: CardRepositoryImpl): CardRepository
    @Binds
    @Singleton
    abstract fun bindBackupRepository(impl: BackupRepositoryImpl): BackupRepository
    @Binds
    @Singleton
    abstract fun bindSharedWalletRepository(impl: SharedWalletRepositoryImpl): SharedWalletRepository
    @Binds
    @Singleton
    abstract fun bindComputationRepository(impl: ComputationRepositoryImpl): ComputationRepository
    @Binds
    @Singleton
    abstract fun bindBackupScheduler(impl: BackupSchedulerImpl): BackupScheduler
    @Binds
    @Singleton
    abstract fun bindCsvImportService(impl: CsvImportServiceImpl): CsvImportService
}
