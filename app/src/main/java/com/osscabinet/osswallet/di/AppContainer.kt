package com.osscabinet.ossWallet.di

import android.content.Context
import com.osscabinet.ossWallet.data.local.AppDatabase
import com.osscabinet.ossWallet.data.local.SettingsDataStore
import com.osscabinet.ossWallet.data.remote.RetrofitClient
import com.osscabinet.ossWallet.data.repository.*
import com.osscabinet.ossWallet.data.seed.DatabaseSeeder
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
        TransactionRepositoryImpl(db.transactionDao(), db.accountDao(), context, db)
    }

    val accountRepository: AccountRepositoryImpl by lazy {
        AccountRepositoryImpl(db.accountDao(), context)
    }

    val budgetRepository: BudgetRepositoryImpl by lazy {
        BudgetRepositoryImpl(db.budgetDao(), context)
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

    // Use Cases
    val getTransactionsUseCase get() = GetTransactionsUseCase(transactionRepository)
    val insertTransactionUseCase get() = InsertTransactionUseCase(transactionRepository)
    val saveTransferUseCase get() = SaveTransferUseCase(transactionRepository)
    val deleteTransactionUseCase get() = DeleteTransactionUseCase(transactionRepository)
    val updateTransactionUseCase get() = UpdateTransactionUseCase(transactionRepository)
    
    val syncBudgetSpentUseCase get() = SyncBudgetSpentUseCase(budgetRepository, transactionRepository)
    val upsertBudgetUseCase get() = UpsertBudgetUseCase(budgetRepository)
    
    val getExchangeRatesUseCase get() = GetExchangeRatesUseCase(currencyRepository)

    val calculateNetWorthUseCase get() = CalculateNetWorthUseCase()
    val getCurrencyAggregationUseCase get() = GetCurrencyAggregationUseCase()
    val getAccountSummariesUseCase get() = GetAccountSummariesUseCase()
    val getTotalBalancesUseCase get() = GetTotalBalancesUseCase()
    val getRecentTransactionsUseCase get() = GetRecentTransactionsUseCase()
    val calculateCashFlowUseCase get() = CalculateCashFlowUseCase()
}
