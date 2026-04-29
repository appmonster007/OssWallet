package com.osscabinet.ossWallet.di

import android.content.Context
import com.osscabinet.ossWallet.data.local.AppDatabase
import com.osscabinet.ossWallet.data.local.SecurePassphraseManager
import com.osscabinet.ossWallet.data.local.SettingsDataStore
import com.osscabinet.ossWallet.data.remote.RetrofitClient
import com.osscabinet.ossWallet.data.repository.*
import com.osscabinet.ossWallet.domain.repository.*
import com.osscabinet.ossWallet.domain.service.TransactionManager
import com.osscabinet.ossWallet.domain.usecase.*
import com.osscabinet.ossWallet.domain.model.analytics.*

/**
 * Dependency container for the application.
 * Follows the Dependency Injection pattern manually to keep the app lightweight
 * while adhering to SOLID principles.
 */
class AppContainer(private val context: Context) {

    private val securePassphraseManager by lazy {
        SecurePassphraseManager(context)
    }

    val db: AppDatabase by lazy {
        AppDatabase.getDatabase(context, securePassphraseManager.getOrGeneratePassphrase())
    }

    val settingsDataStore: SettingsDataStore by lazy {
        SettingsDataStore(context)
    }

    val settingsRepository: SettingsRepository by lazy {
        SettingsRepositoryImpl(settingsDataStore)
    }

    val templateRepository: TemplateRepository by lazy {
        TemplateRepositoryImpl(db.templateDao())
    }

    val categoryRepository: CategoryRepository by lazy {
        CategoryRepositoryImpl(db.categoryDao())
    }

    val filterRepository: FilterRepository by lazy {
        FilterRepositoryImpl(db.filterDao())
    }

    val computationRepository: ComputationRepository by lazy {
        ComputationRepositoryImpl(settingsRepository, currencyRepository)
    }

    val cardRepository: CardRepository by lazy {
        CardRepositoryImpl(settingsDataStore)
    }

    // Repositories
    val transactionRepository: TransactionRepository by lazy {
        TransactionRepositoryImpl(db.transactionDao(), db.accountDao(), db.transactionLinkDao(), db)
    }

    val accountRepository: AccountRepository by lazy {
        AccountRepositoryImpl(db.accountDao())
    }

    val budgetRepository: BudgetRepository by lazy {
        BudgetRepositoryImpl(db.budgetDao(), db.transactionDao())
    }

    val currencyRepository: CurrencyRepository by lazy {
        CurrencyRepositoryImpl(RetrofitClient.currencyApi, db.currencyDao())
    }

    val profileRepository: ProfileRepository by lazy {
        ProfileRepositoryImpl(db.profileDao(), settingsDataStore)
    }

    val loanRepository: LoanRepository by lazy {
        LoanRepositoryImpl(db.loanDao(), db.accountDao())
    }

    val sharedWalletRepository: SharedWalletRepository by lazy {
        SharedWalletRepositoryImpl(db.transactionDao())
    }

    val backupRepository: BackupRepository by lazy {
        BackupRepositoryImpl(context.contentResolver, db.profileDao(), db.accountDao(), db.transactionDao(), db.categoryDao(), db.tagDao(), db.templateDao(), db.transactionLinkDao(), context)
    }

    val tagRepository: TagRepository by lazy {
        TagRepositoryImpl(db.tagDao(), db.transactionDao())
    }

    // Consolidated Use Cases
    val transactionUseCases by lazy { 
        TransactionUseCases(transactionRepository, sharedWalletRepository) 
    }
    
    val budgetUseCases by lazy { 
        BudgetUseCases(budgetRepository, transactionRepository) 
    }
    
    val accountUseCases by lazy {
        AccountUseCases(accountRepository)
    }

    val loanUseCases by lazy {
        LoanUseCases(loanRepository, transactionRepository)
    }

    // Services
    val transactionManager: TransactionManager by lazy {
        TransactionManager(transactionUseCases)
    }
}
