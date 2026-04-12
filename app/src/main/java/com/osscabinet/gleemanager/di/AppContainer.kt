package com.osscabinet.gleemanager.di

import android.content.Context
import com.osscabinet.gleemanager.data.local.AppDatabase
import com.osscabinet.gleemanager.data.local.SettingsDataStore
import com.osscabinet.gleemanager.data.remote.RetrofitClient
import com.osscabinet.gleemanager.data.repository.*
import com.osscabinet.gleemanager.domain.usecase.*

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
        TransactionRepositoryImpl(db.transactionDao())
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
        BackupRepositoryImpl(context.contentResolver, db.profileDao(), db.accountDao(), db.transactionDao(), db.categoryDao(), db.tagDao(), db.templateDao())
    }

    // Use Cases
    val getTransactionsUseCase get() = GetTransactionsUseCase(transactionRepository)
    val insertTransactionUseCase get() = InsertTransactionUseCase(transactionRepository)
    val deleteTransactionUseCase get() = DeleteTransactionUseCase(transactionRepository)
    val updateTransactionUseCase get() = UpdateTransactionUseCase(transactionRepository, accountRepository)
    
    val syncBudgetSpentUseCase get() = SyncBudgetSpentUseCase(budgetRepository, transactionRepository)
    val upsertBudgetUseCase get() = UpsertBudgetUseCase(budgetRepository)
    
    val getExchangeRatesUseCase get() = GetExchangeRatesUseCase(currencyRepository)
}
