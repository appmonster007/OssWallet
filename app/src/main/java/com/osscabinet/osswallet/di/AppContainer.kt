package com.osscabinet.ossWallet.di

import android.content.Context
import com.osscabinet.ossWallet.data.local.AppDatabase
import com.osscabinet.ossWallet.data.local.SecurePassphraseManager
import com.osscabinet.ossWallet.data.local.SettingsDataStore
import com.osscabinet.ossWallet.data.remote.RetrofitClient
import com.osscabinet.ossWallet.data.local.query.DatabaseQueryServiceImpl
import com.osscabinet.ossWallet.data.local.query.QueryCache
import com.osscabinet.ossWallet.data.local.query.TransactionQueryBuilder
import com.osscabinet.ossWallet.data.repository.AccountRepositoryImpl
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
import com.osscabinet.ossWallet.data.repository.SettingsRepositoryImpl
import com.osscabinet.ossWallet.data.repository.SharedWalletRepositoryImpl
import com.osscabinet.ossWallet.data.repository.TagRepositoryImpl
import com.osscabinet.ossWallet.data.repository.TemplateRepositoryImpl
import com.osscabinet.ossWallet.data.repository.TransactionRepositoryImpl
import com.osscabinet.ossWallet.domain.card.CardComputerRegistry
import com.osscabinet.ossWallet.domain.card.computers.AccountHeroCardComputer
import com.osscabinet.ossWallet.domain.card.computers.AccountsCardComputer
import com.osscabinet.ossWallet.domain.card.computers.BudgetsCardComputer
import com.osscabinet.ossWallet.domain.card.computers.CashFlowCardComputer
import com.osscabinet.ossWallet.domain.card.computers.CategoryPieChartCardComputer
import com.osscabinet.ossWallet.domain.card.computers.CategoryTreemapCardComputer
import com.osscabinet.ossWallet.domain.card.computers.CurrencyBalancesCardComputer
import com.osscabinet.ossWallet.domain.card.computers.LoansCardComputer
import com.osscabinet.ossWallet.domain.card.computers.NetWorthCardComputer
import com.osscabinet.ossWallet.domain.card.computers.PlannedPaymentsCardComputer
import com.osscabinet.ossWallet.domain.card.computers.RecentActivitiesCardComputer
import com.osscabinet.ossWallet.domain.repository.AccountRepository
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
import com.osscabinet.ossWallet.domain.repository.SettingsRepository
import com.osscabinet.ossWallet.domain.repository.SharedWalletRepository
import com.osscabinet.ossWallet.domain.repository.TagRepository
import com.osscabinet.ossWallet.domain.repository.TemplateRepository
import com.osscabinet.ossWallet.domain.repository.TransactionRepository
import com.osscabinet.ossWallet.domain.service.WidgetRefreshTrigger
import com.osscabinet.ossWallet.domain.service.management.TransactionManager
import com.osscabinet.ossWallet.domain.service.query.AggregationPlanner
import com.osscabinet.ossWallet.domain.service.query.DatabaseQueryService
import com.osscabinet.ossWallet.domain.usecase.AccountUseCases
import com.osscabinet.ossWallet.domain.usecase.ComputeCardUseCase
import com.osscabinet.ossWallet.domain.usecase.TransactionUseCases
import com.osscabinet.ossWallet.widget.WorkManagerWidgetRefreshTrigger
import com.osscabinet.ossWallet.data.local.WidgetInvalidationObserver
import com.osscabinet.ossWallet.data.seed.SeedingConfig
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
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

    init {
        // Run database seeding if enabled (debug builds only)
        if (SeedingConfig.shouldSeed()) {
            CoroutineScope(Dispatchers.IO).launch {
                SeedingConfig.createSeeder(db, settingsDataStore, context)?.seed()
            }
        }
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

    val cardComputerRegistry: CardComputerRegistry by lazy {
        CardComputerRegistry(listOf(
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
            CategoryTreemapCardComputer()
        ))
    }

    val computeCardUseCase: ComputeCardUseCase by lazy {
        ComputeCardUseCase(cardComputerRegistry)
    }

    val widgetRefreshTrigger: WidgetRefreshTrigger by lazy {
        WorkManagerWidgetRefreshTrigger(context, queryCache)
    }

    private var widgetInvalidationObserver: WidgetInvalidationObserver? = null

    fun startWidgetObserver() {
        widgetInvalidationObserver = WidgetInvalidationObserver(db, queryCache, widgetRefreshTrigger).also { it.start() }
    }

    // Repositories
    val transactionRepository: TransactionRepository by lazy {
        TransactionRepositoryImpl(db.transactionDao(), db.accountDao(), db.transactionLinkDao(), db)
    }

    val accountRepository: AccountRepository by lazy {
        AccountRepositoryImpl(db.accountDao())
    }

    val currencyRepository: CurrencyRepository by lazy {
        CurrencyRepositoryImpl(RetrofitClient.currencyApi, db.currencyDao())
    }

    val profileRepository: ProfileRepository by lazy {
        ProfileRepositoryImpl(db.profileDao(), settingsDataStore)
    }

    val sharedWalletRepository: SharedWalletRepository by lazy {
        SharedWalletRepositoryImpl(db.transactionDao())
    }

    val backupRepository: BackupRepository by lazy {
        BackupRepositoryImpl(
            context.contentResolver,
            db.profileDao(),
            db.accountDao(),
            db.transactionDao(),
            db.categoryDao(),
            db.tagDao(),
            db.templateDao(),
            db.transactionLinkDao(),
            db.budgetDao(),
            db.loanDao(),
            db.loanPaymentDao(),
            db.plannedPaymentDao(),
            db.plannedPaymentOccurrenceDao(),
            db.filterDao(),
            db.currencyDao(),
            db,
            context,
            settingsDataStore
        )
    }

    val tagRepository: TagRepository by lazy {
        TagRepositoryImpl(db.tagDao(), db.transactionDao())
    }

    val plannedPaymentRepository: PlannedPaymentRepository by lazy {
        PlannedPaymentRepositoryImpl(db.plannedPaymentDao(), db.plannedPaymentOccurrenceDao())
    }

    val budgetRepository: BudgetRepository by lazy {
        BudgetRepositoryImpl(db.budgetDao())
    }

    val loanRepository: LoanRepository by lazy {
        LoanRepositoryImpl(db.loanDao(), db.loanPaymentDao())
    }

    private val queryCache: QueryCache by lazy { QueryCache() }

    val databaseQueryService: DatabaseQueryService by lazy {
        DatabaseQueryServiceImpl(
            db.transactionDao(),
            TransactionQueryBuilder(),
            AggregationPlanner(),
            queryCache,
            db.invalidationTracker
        )
    }

    // Consolidated Use Cases
    val transactionUseCases by lazy {
        TransactionUseCases(transactionRepository, sharedWalletRepository)
    }

    val accountUseCases by lazy {
        AccountUseCases(accountRepository, transactionRepository)
    }

    // Services
    val transactionManager: TransactionManager by lazy {
        TransactionManager(transactionUseCases, widgetRefreshTrigger)
    }
}
