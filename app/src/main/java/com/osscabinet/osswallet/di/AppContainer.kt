package com.osscabinet.ossWallet.di

import android.content.Context
import com.osscabinet.ossWallet.data.local.AppDatabase
import com.osscabinet.ossWallet.data.local.SettingsDataStore
import com.osscabinet.ossWallet.data.remote.RetrofitClient
import com.osscabinet.ossWallet.data.repository.*
import com.osscabinet.ossWallet.data.seed.DatabaseSeeder
import com.osscabinet.ossWallet.domain.service.CardComputationService
import com.osscabinet.ossWallet.domain.service.CardComputationServiceImpl
import com.osscabinet.ossWallet.domain.usecase.*

/**
 * Dependency container for the application.
 * Follows the Dependency Injection pattern manually to keep the app lightweight
 * while adhering to SOLID principles.
 */
class AppContainer(private val context: Context) {

    val db: AppDatabase by lazy {
        AppDatabase.getDatabase(context, "secret_passphrase".toByteArray())
    }

    val settingsDataStore: SettingsDataStore by lazy {
        SettingsDataStore(context)
    }

    // Repositories
    val transactionRepository: TransactionRepositoryImpl by lazy {
        TransactionRepositoryImpl(db.transactionDao(), db.accountDao(), db)
    }

    val accountRepository: AccountRepositoryImpl by lazy {
        AccountRepositoryImpl(db.accountDao())
    }

    val budgetRepository: BudgetRepositoryImpl by lazy {
        BudgetRepositoryImpl(db.budgetDao())
    }

    val currencyRepository: CurrencyRepositoryImpl by lazy {
        CurrencyRepositoryImpl(RetrofitClient.currencyApi, db.currencyDao())
    }

    val profileRepository: ProfileRepositoryImpl by lazy {
        ProfileRepositoryImpl(db.profileDao(), settingsDataStore)
    }

    val loanRepository: LoanRepositoryImpl by lazy {
        LoanRepositoryImpl(db.loanDao())
    }
    
    val sharedWalletRepository: SharedWalletRepositoryImpl by lazy {
        SharedWalletRepositoryImpl(db.transactionDao())
    }

    val backupRepository: BackupRepositoryImpl by lazy {
        BackupRepositoryImpl(context.contentResolver, db.profileDao(), db.accountDao(), db.transactionDao(), db.categoryDao(), db.tagDao(), db.templateDao(), context)
    }

    val tagRepository: TagRepositoryImpl by lazy {
        TagRepositoryImpl(db.tagDao(), db.transactionDao())
    }

    val databaseSeeder: DatabaseSeeder by lazy {
        DatabaseSeeder(db.accountDao(), db.categoryDao(), db.transactionDao(), context)
    }

    // Consolidated Use Cases
    val transactionUseCases by lazy { 
        TransactionUseCases(transactionRepository, sharedWalletRepository) 
    }
    
    val financialUseCases by lazy { 
        FinancialUseCases(transactionRepository, currencyRepository) 
    }
    
    val budgetUseCases by lazy { 
        BudgetUseCases(budgetRepository, transactionRepository) 
    }
    
    val accountUseCases by lazy {
        AccountUseCases(accountRepository)
    }

    // Services
    val cardComputationService: CardComputationService by lazy {
        CardComputationServiceImpl(financialUseCases)
    }
}
