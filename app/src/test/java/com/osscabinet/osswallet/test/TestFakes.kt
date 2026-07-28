package com.osscabinet.ossWallet.test

import com.osscabinet.ossWallet.domain.model.Account
import com.osscabinet.ossWallet.domain.model.Category
import com.osscabinet.ossWallet.domain.model.CurrencySummary
import com.osscabinet.ossWallet.domain.model.SharedWallet
import com.osscabinet.ossWallet.domain.model.Tag
import com.osscabinet.ossWallet.domain.model.Template
import com.osscabinet.ossWallet.domain.model.Transaction
import com.osscabinet.ossWallet.domain.model.TransactionFilter
import com.osscabinet.ossWallet.domain.model.TransactionWithBalance
import com.osscabinet.ossWallet.domain.model.settings.GlassSettings
import com.osscabinet.ossWallet.domain.model.settings.TransactionSectionSettings
import com.osscabinet.ossWallet.domain.repository.AccountRepository
import com.osscabinet.ossWallet.domain.repository.AutoBackupConfig
import com.osscabinet.ossWallet.domain.repository.CategoryRepository
import com.osscabinet.ossWallet.domain.repository.ComputationRepository
import com.osscabinet.ossWallet.domain.repository.RateRefreshError
import com.osscabinet.ossWallet.domain.repository.SettingsRepository
import com.osscabinet.ossWallet.domain.repository.SharedWalletRepository
import com.osscabinet.ossWallet.domain.repository.TagRepository
import com.osscabinet.ossWallet.domain.repository.TemplateRepository
import com.osscabinet.ossWallet.domain.repository.TransactionRepository
import com.osscabinet.ossWallet.domain.model.analytics.ComputationContext
import com.osscabinet.ossWallet.domain.model.import_csv.CsvImportSession
import com.osscabinet.ossWallet.domain.model.import_csv.DuplicatePolicy
import com.osscabinet.ossWallet.domain.model.import_csv.EntityMapping
import com.osscabinet.ossWallet.domain.model.import_csv.ImportPreview
import com.osscabinet.ossWallet.domain.model.import_csv.ImportSourceApp
import com.osscabinet.ossWallet.domain.model.import_csv.ImportSummary
import com.osscabinet.ossWallet.domain.service.CsvImportService
import com.osscabinet.ossWallet.domain.service.currency.ExchangeRateSnapshot
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import java.io.InputStream
import java.math.BigDecimal

// ── FakeSettingsRepository ──────────────────────────────────────────────────

class FakeSettingsRepository(
    initialProfileId: Int = 1,
    initialCurrency: String = "USD"
) : SettingsRepository {
    val currentProfileIdFlow = MutableStateFlow(initialProfileId)
    val currencyFlow = MutableStateFlow(initialCurrency)

    override val currentProfileId: Flow<Int> = currentProfileIdFlow
    override val currency: Flow<String> = currencyFlow
    override val mapsAccessEnabled: Flow<Boolean> = flowOf(false)
    override val recentActivitiesBreakdown: Flow<Boolean> = flowOf(false)
    override val shortlistedCurrencies: Flow<Set<String>?> = flowOf(null)
    override val theme: Flow<String> = flowOf("DARK")
    override val decimalPrecision: Flow<Int> = flowOf(2)
    override val autoSaveExchangeRates: Flow<Boolean> = flowOf(false)
    override val isOnboardingCompleted: Flow<Boolean> = flowOf(true)
    override val cardAnimationEnabled: Flow<Boolean> = flowOf(true)
    override val cardOpenAnimation: Flow<String> = flowOf("FLIP")
    override val expandableFabEnabled: Flow<Boolean> = flowOf(true)
    override val isDatabaseSeeded: Flow<Boolean> = flowOf(false)
    override val use24HourFormat: Flow<Boolean> = flowOf(false)
    override val currencyProvider: Flow<String> = flowOf("openexchangerates")
    override val userName: Flow<String> = flowOf("")
    override val useSystemFont: Flow<Boolean> = flowOf(true)
    override val selectedFont: Flow<String> = flowOf("System")
    override val uiIdentity: Flow<String> = flowOf("default")
    override val dynamicAccent: Flow<Boolean> = flowOf(false)
    override val numberingSystem: Flow<String> = flowOf("western")
    override val recordsSettings: Flow<TransactionSectionSettings> = flowOf(TransactionSectionSettings())
    override val dashboardCardsConfig: Flow<String> = flowOf("")
    override val accountDetailCardsConfig: Flow<String> = flowOf("")
    override val accountsViewCardsConfig: Flow<String> = flowOf("")
    override val analyticsCardsConfig: Flow<String> = flowOf("")
    override val analyticsCommonFilterEnabled: Flow<Boolean> = flowOf(false)
    override val glassmorphismEnabled: Flow<Boolean> = flowOf(false)
    override val contourBgVersion: Flow<Int> = flowOf(0)
    override val contourLinesEnabled: Flow<Boolean> = flowOf(false)
    override val randomizeContourEachLoad: Flow<Boolean> = flowOf(false)
    override val glassSettings: Flow<GlassSettings> = flowOf(GlassSettings())
    override val autoBackupConfig: Flow<AutoBackupConfig> = flowOf(AutoBackupConfig())
    override val uiDensity: Flow<String> = flowOf("default")

    override suspend fun saveCurrency(currency: String) { currencyFlow.value = currency }
    override suspend fun saveTheme(theme: String) {}
    override suspend fun saveCurrentProfileId(profileId: Int) { currentProfileIdFlow.value = profileId }
    override suspend fun saveDecimalPrecision(precision: Int) {}
    override suspend fun saveAutoSaveExchangeRates(enabled: Boolean) {}
    override suspend fun saveOnboardingCompleted(completed: Boolean) {}
    override suspend fun saveCardAnimationEnabled(enabled: Boolean) {}
    override suspend fun saveCardOpenAnimation(name: String) {}
    override suspend fun saveExpandableFabEnabled(enabled: Boolean) {}
    override suspend fun saveDatabaseSeeded(seeded: Boolean) {}
    override suspend fun saveUse24HourFormat(use24Hour: Boolean) {}
    override suspend fun saveMapsAccessEnabled(enabled: Boolean) {}
    override suspend fun saveRecentActivitiesBreakdown(enabled: Boolean) {}
    override suspend fun saveCurrencyProvider(providerId: String) {}
    override suspend fun saveUserName(name: String) {}
    override suspend fun saveUseSystemFont(enabled: Boolean) {}
    override suspend fun saveSelectedFont(fontName: String) {}
    override suspend fun saveUiIdentity(identity: String) {}
    override suspend fun saveDynamicAccent(enabled: Boolean) {}
    override suspend fun saveNumberingSystem(system: String) {}
    override suspend fun updateRecordsSettings(update: (TransactionSectionSettings) -> TransactionSectionSettings) {}
    override suspend fun saveDashboardCardsConfig(config: String) {}
    override suspend fun saveAccountDetailCardsConfig(config: String) {}
    override suspend fun saveAccountsViewCardsConfig(config: String) {}
    override suspend fun saveAnalyticsCardsConfig(config: String) {}
    override suspend fun saveAnalyticsCommonFilterEnabled(enabled: Boolean) {}
    override suspend fun saveShortlistedCurrencies(currencies: Set<String>) {}
    override suspend fun saveGlassmorphismEnabled(enabled: Boolean) {}
    override suspend fun saveContourBgVersion(version: Int) {}
    override suspend fun saveContourLinesEnabled(enabled: Boolean) {}
    override suspend fun saveRandomizeContourEachLoad(enabled: Boolean) {}
    override suspend fun updateGlassSettings(update: (GlassSettings) -> GlassSettings) {}
    override suspend fun saveAutoBackupConfig(config: AutoBackupConfig) {}
    override suspend fun saveUiDensity(name: String) {}
}

// ── FakeComputationRepository ───────────────────────────────────────────────

class FakeComputationRepository(
    initialRates: ExchangeRateSnapshot = ExchangeRateSnapshot("USD", mapOf("EUR" to BigDecimal("0.85")))
) : ComputationRepository {
    val ratesFlow = MutableStateFlow(initialRates)

    override val exchangeRatesSnapshot: Flow<ExchangeRateSnapshot> = ratesFlow
    override val exchangeRates: Flow<Map<String, BigDecimal>> = flowOf(initialRates.rates)
    override val computationContext: Flow<ComputationContext?> = flowOf(null)
    override val selectedCurrency: Flow<String> = flowOf(initialRates.anchor)
    override val hasLoadedExchangeRates: Flow<Boolean> = flowOf(true)
    override val rateRefreshErrors: Flow<RateRefreshError> = MutableSharedFlow()
}

// ── FakeAccountRepository ───────────────────────────────────────────────────

class FakeAccountRepository(
    initialAccounts: List<Account> = emptyList()
) : AccountRepository {
    val accountsFlow = MutableStateFlow(initialAccounts)

    override fun getAllAccounts(profileId: Int): Flow<List<Account>> = accountsFlow
    override suspend fun getAllAccountsSync(profileId: Int): List<Account> = accountsFlow.value
    override suspend fun insertAccount(account: Account, profileId: Int) {
        accountsFlow.value = accountsFlow.value + account
    }
    override suspend fun updateAccount(account: Account, profileId: Int) {
        accountsFlow.value = accountsFlow.value.map { if (it.id == account.id) account else it }
    }
    override suspend fun deleteAccount(accountId: String, profileId: Int) {
        accountsFlow.value = accountsFlow.value.filter { it.id != accountId }
    }
    override suspend fun updateAccounts(accounts: List<Account>, profileId: Int) {
        accountsFlow.value = accounts
    }
}

// ── FakeTransactionRepository ───────────────────────────────────────────────

class FakeTransactionRepository : TransactionRepository {
    private val txFlow = MutableStateFlow<List<Transaction>>(emptyList())

    override fun getAllTransactions(profileId: Int): Flow<List<Transaction>> = txFlow
    override fun getLatestTransactionVersion(profileId: Int): Flow<Long?> = flowOf(0L)
    override suspend fun getAllTransactionsSync(profileId: Int): List<Transaction> = txFlow.value
    override fun getFilteredTransactions(profileId: Int, filter: TransactionFilter): Flow<List<Transaction>> = txFlow
    override suspend fun getFilteredTransactionsSync(profileId: Int, filter: TransactionFilter): List<Transaction> = emptyList()
    override suspend fun insertTransaction(transaction: Transaction, profileId: Int): Long = 1L
    override suspend fun saveTransfer(sourceTransaction: Transaction, targetTransaction: Transaction, profileId: Int): Pair<Long, Long> = 1L to 2L
    override suspend fun deleteTransaction(id: Long, profileId: Int) {}
    override suspend fun updateTransaction(oldTransaction: Transaction, newTransaction: Transaction, profileId: Int) {}
    override fun getTransactionsByAccount(accountId: String, profileId: Int): Flow<List<Transaction>> = txFlow
    override suspend fun getTransactionById(id: Long, profileId: Int): Transaction? = null
    override suspend fun addTransactionLink(transactionId: Long, linkedTransactionId: Long) {}
    override suspend fun removeTransactionLink(transactionId: Long, linkedTransactionId: Long) {}
    override suspend fun clearTransactionLinks(transactionId: Long) {}
    override fun getAllTransactionLinks(): Flow<Map<Long, List<Long>>> = flowOf(emptyMap())
    override fun getLinkedTransactionIds(transactionId: Long): Flow<List<Long>> = flowOf(emptyList())
    override fun getLinkedTransactions(transactionId: Long): Flow<List<Transaction>> = flowOf(emptyList())
    override fun searchTransactions(profileId: Int, query: String): Flow<List<Transaction>> = txFlow
    override fun observeFilteredTransactionsWithBalance(
        profileId: Int,
        filter: TransactionFilter,
        baseCurrency: String,
        exchangeRates: ExchangeRateSnapshot,
        limit: Int?
    ): Flow<Result<List<TransactionWithBalance>>> = flowOf(Result.success(emptyList()))
    override suspend fun getUniquePayees(profileId: Int): List<String> = emptyList()
    override fun observeUniquePayees(profileId: Int): Flow<List<String>> = flowOf(emptyList())
    override suspend fun getUniqueNotes(profileId: Int): List<String> = emptyList()
    override fun observeUniqueNotes(profileId: Int): Flow<List<String>> = flowOf(emptyList())
    override suspend fun getCurrencySummaries(profileId: Int): List<CurrencySummary> = emptyList()
    override suspend fun getCurrencySummariesInRange(profileId: Int, start: Long, end: Long): List<CurrencySummary> = emptyList()
    override suspend fun getInitialBalanceTransaction(accountId: String, currency: String, profileId: Int): Transaction? = null
    override suspend fun <T> runAtomically(block: suspend () -> T): T = block()
}

// ── FakeTemplateRepository ──────────────────────────────────────────────────

class FakeTemplateRepository(
    initialTemplates: List<Template> = emptyList()
) : TemplateRepository {
    private val templatesFlow = MutableStateFlow(initialTemplates)

    override fun getAllTemplates(profileId: Int): Flow<List<Template>> = templatesFlow
    override fun getStarredTemplates(): Flow<List<Template>> = flowOf(templatesFlow.value.filter { it.isStarred })
    override fun getStarredTemplates(profileId: Int): Flow<List<Template>> = flowOf(templatesFlow.value.filter { it.isStarred && it.profileId == profileId })
    override suspend fun updateStarStatus(id: Long, isStarred: Boolean) {
        templatesFlow.value = templatesFlow.value.map { if (it.id == id) it.copy(isStarred = isStarred) else it }
    }
    override suspend fun insertTemplate(template: Template): Long = 1L
    override suspend fun updateTemplate(template: Template) {
        templatesFlow.value = templatesFlow.value.map { if (it.id == template.id) template else it }
    }
    override suspend fun deleteTemplate(template: Template) {
        templatesFlow.value = templatesFlow.value.filter { it.id != template.id }
    }
    override suspend fun getTemplateByName(name: String): Template? = templatesFlow.value.find { it.name == name }
    override suspend fun getTemplateById(id: Long): Template? = templatesFlow.value.find { it.id == id }
    override suspend fun updateTemplates(templates: List<Template>) { templatesFlow.value = templates }
    override suspend fun getAllTemplatesSync(profileId: Int): List<Template> = templatesFlow.value.filter { it.profileId == profileId }
    override suspend fun clearAll() { templatesFlow.value = emptyList() }
}

// ── FakeCategoryRepository ──────────────────────────────────────────────────

class FakeCategoryRepository(
    initialCategories: List<Category> = emptyList()
) : CategoryRepository {
    private val categoriesFlow = MutableStateFlow(initialCategories)

    override val categories: Flow<List<Category>> = categoriesFlow
    override fun getAllCategories(profileId: Int): Flow<List<Category>> = categoriesFlow
    override suspend fun upsertCategory(category: Category) {
        categoriesFlow.value = categoriesFlow.value.filter { it.id != category.id } + category
    }
    override suspend fun deleteCategory(category: Category) {
        categoriesFlow.value = categoriesFlow.value.filter { it.id != category.id }
    }
    override suspend fun getAllCategoriesSync(): List<Category> = categoriesFlow.value
}

// ── FakeTagRepository ───────────────────────────────────────────────────────

class FakeTagRepository : TagRepository {
    private val tagsFlow = MutableStateFlow<List<Tag>>(emptyList())

    override fun getAllTags(): Flow<List<Tag>> = tagsFlow
    override fun getAllTags(profileId: Int): Flow<List<Tag>> = tagsFlow
    override suspend fun insertTag(tag: Tag) {}
    override suspend fun deleteTag(tag: Tag) {}
    override suspend fun deleteTagAndRemoveFromTransactions(tag: Tag) {}
    override suspend fun getTransactionCountForTag(tagName: String, profileId: Int): Int = 0
    override suspend fun updateTag(oldName: String, newTag: Tag) {}
}

// ── FakeCsvImportService ─────────────────────────────────────────────────────

class FakeCsvImportService(
    var sessionToReturn: CsvImportSession = CsvImportSession(
        headers = listOf("date", "account", "amount"),
        previewRows = emptyList(),
        mapping = emptyMap(),
        sourceApp = ImportSourceApp.GENERIC
    ),
    var previewToReturn: ImportPreview = ImportPreview(
        totalRows = 0,
        validRows = 0,
        duplicates = emptyList(),
        allCsvAccounts = emptyList(),
        newCategories = emptyList(),
        parseErrors = emptyList()
    ),
    var executeImportResult: Result<ImportSummary> = Result.success(ImportSummary(0, 0, emptyList()))
) : CsvImportService {
    override suspend fun parseHeaders(
        inputStream: InputStream,
        delimiter: Char?,
        encoding: String
    ): CsvImportSession = sessionToReturn

    override suspend fun generatePreview(
        session: CsvImportSession,
        inputStream: InputStream
    ): ImportPreview = previewToReturn

    override suspend fun executeImport(
        session: CsvImportSession,
        inputStream: InputStream,
        duplicatePolicy: DuplicatePolicy,
        entityMappings: List<EntityMapping>,
        selectedDuplicateRowIndices: Set<Int>,
        totalRows: Int,
        onProgress: (Float) -> Unit
    ): Result<ImportSummary> = executeImportResult
}

// ── FakeSharedWalletRepository ──────────────────────────────────────────────

class FakeSharedWalletRepository : SharedWalletRepository {
    override fun getSharedWallets(): Flow<List<SharedWallet>> = flowOf(emptyList())
    override fun getTransactionsForWallet(walletId: String): Flow<List<Transaction>> = flowOf(emptyList())
    override suspend fun createWallet(name: String): String = ""
    override suspend fun joinWallet(inviteCode: String) {}
    override suspend fun addTransactionToWallet(walletId: String, transaction: Transaction) {}
    override suspend fun syncTransactions(walletId: String) {}
}
