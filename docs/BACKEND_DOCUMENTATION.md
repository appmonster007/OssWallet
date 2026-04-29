# OssWallet Backend Documentation

## Table of Contents
1. [Architecture Overview](#architecture-overview)
2. [Data Layer](#data-layer)
   - [Entities](#entities)
   - [DAOs](#daos)
   - [Repository Implementations](#repository-implementations)
3. [Domain Layer](#domain-layer)
   - [Domain Models](#domain-models)
   - [Repository Interfaces](#repository-interfaces)
   - [Use Cases](#use-cases)
   - [Services](#services)
4. [Filter Mechanism](#filter-mechanism)
5. [Compute Logic](#compute-logic)
6. [Data Flow Patterns](#data-flow-patterns)
7. [Currency Conversion](#currency-conversion)

---

## Architecture Overview

OssWallet follows **Clean Architecture** with three distinct layers:

```
┌─────────────────────────────────────────────────────────┐
│                    UI Layer (ui/)                        │
│  ViewModels, Screens, Components, Navigation              │
└─────────────────────────────────────────────────────────┘
                            ↓
┌─────────────────────────────────────────────────────────┐
│                 Domain Layer (domain/)                   │
│  Models, Repository Interfaces, Use Cases, Services      │
│  Pure Kotlin - NO Android dependencies                    │
└─────────────────────────────────────────────────────────┘
                            ↓
┌─────────────────────────────────────────────────────────┐
│                  Data Layer (data/)                      │
│  Entities, DAOs, Repository Implementations, Remote      │
└─────────────────────────────────────────────────────────┘
```

### Key Principles
- **Domain Layer**: Pure Kotlin, no Android dependencies, business logic
- **Data Layer**: Room entities, DAOs, repository implementations, network calls
- **UI Layer**: ViewModels, Compose screens, components
- **Dependency Injection**: Constructor injection via Hilt/Koin
- **Reactive Streams**: Flow for data observation
- **Immutable State**: StateFlow exposed as immutable via `.asStateFlow()`

---

## Data Layer

### Entities

Room entities represent database tables with proper relationships and constraints.

#### AccountEntity
```kotlin
@Entity(tableName = "accounts", indices = [Index(value = ["profileId"])])
data class AccountEntity(
    @PrimaryKey val id: String,
    val name: String,
    val balance: BigDecimal,
    val currencyCode: String,
    val structureType: String,
    val colorHex: String,
    val icon: String,
    val allowedCurrencies: String, // JSON or comma-separated
    val profileId: Int = 1,
    val isArchived: Boolean = false,
    val displayOrder: Int = 0
)
```
- **Primary Key**: String (UUID-based)
- **Index**: profileId for query optimization
- **Fields**: Account balance, currency, visual customization, profile association

#### TransactionEntity
```kotlin
@Entity(
    tableName = "transactions",
    foreignKeys = [
        ForeignKey(entity = AccountEntity::class, parentColumns = ["id"], childColumns = ["accountId"], onDelete = ForeignKey.CASCADE),
        ForeignKey(entity = CategoryEntity::class, parentColumns = ["id"], childColumns = ["categoryId"], onDelete = ForeignKey.SET_NULL)
    ],
    indices = [Index(value = ["accountId"]), Index(value = ["categoryId"]), Index(value = ["profileId"]), Index(value = ["timestamp"]), Index(value = ["status"])]
)
data class TransactionEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val amount: BigDecimal,
    val category: String,
    val timestamp: Long,
    val currency: String,
    val transactionType: String, // "income" or "expense"
    val accountId: String,
    val categoryId: Long? = null,
    val tags: String = "", // Comma-separated
    val paymentMethod: String = "Cash",
    val payee: String = "",
    val note: String = "",
    val recurring: String? = null,
    val profileId: Int = 1,
    val latitude: Double? = null,
    val longitude: Double? = null,
    val warrantyExpiration: Long? = null,
    val status: String = "cleared"
)
```
- **Foreign Keys**: Account (CASCADE delete), Category (SET NULL)
- **Indexes**: accountId, categoryId, profileId, timestamp, status
- **Timestamps**: Stored as Long (UTC milliseconds)

#### TransactionLinkEntity
```kotlin
@Entity(
    tableName = "transaction_links",
    foreignKeys = [
        ForeignKey(entity = TransactionEntity::class, parentColumns = ["id"], childColumns = ["transactionId"], onDelete = ForeignKey.CASCADE),
        ForeignKey(entity = TransactionEntity::class, parentColumns = ["id"], childColumns = ["linkedTransactionId"], onDelete = ForeignKey.CASCADE)
    ],
    indices = [Index(value = ["transactionId"]), Index(value = ["linkedTransactionId"]), Index(value = ["transactionId", "linkedTransactionId"], unique = true)]
)
data class TransactionLinkEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val transactionId: Long,
    val linkedTransactionId: Long
)
```
- **Purpose**: Junction table for many-to-many transaction relationships
- **Use Cases**: Transfers, clones, manual links
- **Unique Index**: Prevents duplicate links between same transactions

#### CategoryEntity
```kotlin
@Entity(tableName = "categories")
data class CategoryEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val iconName: String,
    val colorHex: Int,
    val isDefault: Boolean = false,
    val parentId: Long? = null
)
```
- **Hierarchy**: Supports subcategories via parentId
- **Visual**: Icon and color for UI

#### BudgetEntity
```kotlin
@Entity(tableName = "budgets")
data class BudgetEntity(
    @PrimaryKey val id: String,
    val name: String,
    val limit: BigDecimal,
    val spent: BigDecimal,
    val currency: String,
    val period: BudgetPeriod,
    val filterJson: String, // TransactionFilter serialized
    val alertThresholdsJson: String, // Set<Float> serialized
    val isPredictiveAlertEnabled: Boolean,
    val startDate: Long?,
    val endDate: Long?,
    val profileId: Int = 1,
    val lastEvaluated: Long = System.currentTimeMillis()
)
```
- **Period**: WEEKLY, MONTHLY, CUSTOM
- **Filter**: Serialized TransactionFilter for budget scope
- **Alerts**: Thresholds and predictive alerts

#### CurrencyRateEntity
```kotlin
@Entity(tableName = "currency_rates")
data class CurrencyRateEntity(
    @PrimaryKey val currencyCode: String,
    val baseCurrency: String,
    val rate: Double,
    val timestamp: Long,
    val nextUpdate: Long
)
```
- **Rate Structure**: Rate from currency to base currency
- **Cache**: Timestamp and nextUpdate for cache validity

#### Other Entities
- **TagEntity**: Simple tag with name and color
- **TemplateEntity**: Reusable transaction templates
- **LoanEntity**: Loan metadata linked to accounts
- **ProfileEntity**: User profiles with base currency
- **FilterEntity**: Saved transaction filters

---

### DAOs

Data Access Objects provide database operations with Flow support for reactive streams.

#### TransactionDao
```kotlin
@Dao
abstract class TransactionDao {
    @Query("SELECT * FROM transactions WHERE profileId = :profileId ORDER BY timestamp DESC")
    abstract fun getAllTransactions(profileId: Int): Flow<List<TransactionEntity>>

    @RawQuery(observedEntities = [TransactionEntity::class])
    abstract fun getFilteredTransactions(query: SupportSQLiteQuery): Flow<List<TransactionEntity>>

    @Transaction
    abstract suspend fun insertTransactionWithBalanceUpdate(transaction: TransactionEntity, accountDao: AccountDao): Long

    @Transaction
    abstract suspend fun deleteTransactionWithBalanceUpdate(transaction: TransactionEntity, accountDao: AccountDao)

    @Transaction
    abstract suspend fun updateTransactionWithBalanceUpdate(oldTransaction: TransactionEntity, newTransaction: TransactionEntity, accountDao: AccountDao)

    @Query("SELECT currency, SUM(CASE WHEN transactionType = 'income' THEN amount ELSE 0 END) as totalIncome, SUM(CASE WHEN transactionType = 'expense' THEN amount ELSE 0 END) as totalExpense FROM transactions WHERE profileId = :profileId GROUP BY currency")
    abstract suspend fun getCurrencySummaries(profileId: Int): List<CurrencySummary>
}
```
- **Transactions**: Balance updates are atomic operations
- **Filtering**: RawQuery support for dynamic SQL
- **Summaries**: Currency-wise income/expense aggregation

#### AccountDao
```kotlin
@Dao
interface AccountDao {
    @Query("SELECT * FROM accounts WHERE profileId = :profileId ORDER BY displayOrder ASC")
    fun getAllAccounts(profileId: Int): Flow<List<AccountEntity>>

    @Query("UPDATE accounts SET balance = balance + :amount WHERE id = :accountId")
    suspend fun updateBalance(accountId: String, amount: BigDecimal)

    @Upsert
    suspend fun upsertAccount(account: AccountEntity)
}
```
- **Balance Updates**: Incremental updates for performance
- **Upsert**: Insert or replace in one operation

#### Other DAOs
- **CategoryDao**: CRUD with hierarchical support
- **BudgetDao**: Budget management
- **CurrencyDao**: Exchange rate caching with transactional replace
- **FilterDao**: Saved filter management
- **LoanDao**: Loan metadata operations
- **ProfileDao**: Profile management
- **TagDao**: Tag CRUD with transaction cleanup
- **TemplateDao**: Template management with starring
- **TransactionLinkDao**: Many-to-many link management

---

### Repository Implementations

Repositories implement domain interfaces, handle data mapping, and coordinate between DAOs.

#### TransactionRepositoryImpl
```kotlin
class TransactionRepositoryImpl(
    private val transactionDao: TransactionDao,
    private val accountDao: AccountDao,
    private val transactionLinkDao: TransactionLinkDao,
    private val database: AppDatabase
) : TransactionRepository {
    
    override fun getFilteredTransactions(profileId: Int, filter: TransactionFilter): Flow<List<Transaction>> {
        val (query, args) = buildFilteredQuery(profileId, filter)
        val sqliteQuery = SimpleSQLiteQuery(query, args.toTypedArray())
        return transactionDao.getFilteredTransactions(sqliteQuery).mapToDomain { entity ->
            entity.toDomain()
        }.map { applyRegexFilter(it, filter) }
    }

    override suspend fun saveTransfer(sourceTransaction: Transaction, targetTransaction: Transaction, profileId: Int): Pair<Long, Long> {
        return database.withTransaction {
            val sourceId = transactionDao.insertTransactionWithBalanceUpdate(sourceTransaction.toEntity(profileId), accountDao)
            val targetId = transactionDao.insertTransactionWithBalanceUpdate(targetTransaction.toEntity(profileId), accountDao)
            // Create bi-directional links
            transactionLinkDao.insertLink(TransactionLinkEntity(transactionId = sourceId, linkedTransactionId = targetId))
            transactionLinkDao.insertLink(TransactionLinkEntity(transactionId = targetId, linkedTransactionId = sourceId))
            Pair(sourceId, targetId)
        }
    }
}
```
- **Dynamic SQL**: Builds queries based on filter criteria
- **Regex Filtering**: Applied after SQL query for note/payee regex
- **Transfers**: Atomic operation with bi-directional links
- **Balance Updates**: Coordinated with accountDao

#### CurrencyRepositoryImpl
```kotlin
class CurrencyRepositoryImpl(
    private val api: CurrencyApi,
    private val dao: CurrencyDao
) : CurrencyRepository {
    
    override suspend fun getExchangeRates(baseCurrency: String, forceRefresh: Boolean): Map<String, Double> {
        return withContext(Dispatchers.IO) {
            val localRates = dao.getAllRates()
            
            if (!forceRefresh && localRates.isNotEmpty() && isCacheValid(localRates.first())) {
                val rates = localRates.associate { it.currencyCode to it.rate }
                if (localRates.first().baseCurrency == baseCurrency) {
                    return@withContext rates
                }
                // Cross-rate derivation: Rate(X to B) = Rate(X to A) / Rate(B to A)
                val newBaseRate = rates[baseCurrency]
                if (newBaseRate != null && newBaseRate != 0.0) {
                    return@withContext rates.mapValues { it.value / newBaseRate }
                }
            }
            
            val freshRates = updateRates(baseCurrency)
            if (freshRates.isNotEmpty()) return@withContext freshRates
            
            localRates.associate { it.currencyCode to it.rate }
        }
    }
}
```
- **Cache Strategy**: Uses provider's nextUpdate timestamp
- **Cross-Rate Derivation**: Converts between cached bases
- **Fallback**: Returns cached data on fetch failure
- **Error Handling**: Network exceptions with fallback to cache

#### BudgetRepositoryImpl
```kotlin
class BudgetRepositoryImpl(
    private val budgetDao: BudgetDao,
    private val transactionDao: TransactionDao
) : BudgetRepository {
    
    override suspend fun evaluateBudgets() {
        val budgets = budgetDao.getAllBudgets().first().map { it.toDomain() }
        val transactions = transactionDao.getAllTransactions(1).first().toDomainList()
        
        budgets.forEach { budget ->
            val matchingTransactions = FilterService.filterTransactions(transactions, budget.filter)
            val totalSpent = matchingTransactions
                .filter { it.transactionType == "expense" }
                .sumOf { it.amount.abs() }
            
            upsertBudget(budget.copy(spent = totalSpent, lastEvaluated = System.currentTimeMillis()))
        }
    }
}
```
- **Evaluation**: Recalculates spent amounts based on filter
- **Filter Integration**: Uses FilterService for scope definition

#### Other Repository Implementations
- **AccountRepositoryImpl**: Account CRUD with balance operations
- **CategoryRepositoryImpl**: Category management
- **FilterRepositoryImpl**: Saved filter CRUD
- **LoanRepositoryImpl**: Loan metadata with account lookup
- **ProfileRepositoryImpl**: Profile management with DataStore integration
- **TagRepositoryImpl**: Tag CRUD with transaction cleanup
- **TemplateRepositoryImpl**: Template management
- **ComputationRepositoryImpl**: Computation context aggregation
- **CardRepositoryImpl**: Card configuration management
- **SettingsRepositoryImpl**: Settings via DataStore
- **BackupRepositoryImpl**: Full database export/import
- **SharedWalletRepositoryImpl**: Mock implementation for collaborative wallets

---

## Domain Layer

Pure Kotlin layer with no Android dependencies. Contains business logic, models, and interfaces.

### Domain Models

#### Transaction
```kotlin
data class Transaction(
    val id: Long = 0,
    val title: String,
    val amount: BigDecimal,
    val category: String,
    override val timestamp: Long,
    val currency: String,
    val transactionType: String, // "income" or "expense"
    val accountId: String,
    val categoryId: Long? = null,
    val tags: List<String> = emptyList(),
    val paymentMethod: String = "Cash",
    val payee: String = "",
    val note: String = "",
    val recurring: String? = null,
    val latitude: Double? = null,
    val longitude: Double? = null,
    val warrantyExpiration: Long? = null,
    val status: TransactionStatus = TransactionStatus.CLEARED
) : Timestamped
```
- **Immutable**: Data class with computed properties
- **Status**: RECONCILE, CLEARED, UNCLEARED
- **Location**: Optional latitude/longitude
- **Warranty**: Optional expiration for tracking

#### Account
```kotlin
data class Account(
    val id: String,
    val name: String,
    val balance: BigDecimal,
    val currencyCode: String,
    val structureType: String,
    val colorHex: String,
    val icon: String,
    val allowedCurrencies: List<String> = emptyList(),
    val isArchived: Boolean = false,
    val displayOrder: Int = 0
)
```

#### TransactionFilter
```kotlin
@Serializable
data class TransactionFilter(
    val timelineRange: TimelineRange = TimelineRange.AllTime,
    val queryNote: String? = null,
    val isNoteRegex: Boolean = false,
    val queryPayee: String? = null,
    val isPayeeRegex: Boolean = false,
    val categoryIds: List<Long> = emptyList(),
    val accountIds: List<String> = emptyList(),
    val startDate: Long? = null,
    val endDate: Long? = null,
    val paymentMethods: List<String> = emptyList(),
    val hasWarranty: Boolean? = null,
    val warrantyExpirationStart: Long? = null,
    val warrantyExpirationEnd: Long? = null,
    val minAmount: BigDecimal? = null,
    val maxAmount: BigDecimal? = null,
    val transactionType: String? = null,
    val currencies: List<String> = emptyList(),
    val tags: List<String> = emptyList(),
    val statuses: List<TransactionStatus> = emptyList()
)
```
- **Timeline**: AllTime, ThisPeriod, LastPeriod, CustomRange
- **Regex**: Support for regex in note/payee
- **Warranty**: Filter by warranty existence and expiration
- **Serialization**: Kotlinx Serializable for persistence

#### Budget
```kotlin
data class Budget(
    val id: String,
    val name: String,
    val limit: BigDecimal,
    val spent: BigDecimal,
    val currency: String = "USD",
    val period: BudgetPeriod = BudgetPeriod.MONTHLY,
    val filter: TransactionFilter = TransactionFilter(),
    val alertThresholds: Set<Float> = setOf(0.5f, 0.8f, 1.0f),
    val isPredictiveAlertEnabled: Boolean = true,
    val startDate: Long? = null,
    val endDate: Long? = null,
    val lastEvaluated: Long = System.currentTimeMillis()
) {
    val progress: Float get() = if (limit.signum() > 0) (spent.divide(limit, 4, RoundingMode.HALF_UP)).toFloat() else 0f
    val remaining: BigDecimal get() = (limit.subtract(spent)).max(BigDecimal.ZERO)
    val isOverBudget: Boolean get() = spent.compareTo(limit) > 0

    fun calculateDailySafeSpend(daysRemaining: Int): BigDecimal {
        if (daysRemaining <= 0) return BigDecimal.ZERO
        return remaining.divide(BigDecimal(daysRemaining), 2, RoundingMode.HALF_UP)
    }

    fun isHighRisk(daysElapsed: Int, totalDays: Int): Boolean {
        if (totalDays <= 0 || daysElapsed <= 0) return false
        val expectedProgress = daysElapsed.toFloat() / totalDays.toFloat()
        return progress > (expectedProgress + 0.15f)
    }
}
```
- **Computed Properties**: Progress, remaining, over-budget
- **Risk Assessment**: High-risk if spending exceeds expected by 15%
- **Daily Safe Spend**: Remaining divided by days left

#### TransactionStatus
```kotlin
@Serializable
enum class TransactionStatus(val displayName: String) {
    RECONCILE("Reconcile"),  // Needs bank statement reconciliation
    CLEARED("Cleared"),      // Confirmed and cleared
    UNCLEARED("Uncleared")  // Not yet cleared
}
```

#### TransactionType
```kotlin
enum class TransactionType(val displayName: String, val value: String) {
    INCOME("Income", "income"),
    EXPENSE("Expense", "expense");

    companion object {
        fun fromValue(value: String?): TransactionType?
        fun fromBoolean(isIncome: Boolean): TransactionType
        const val ALL = "all"  // Special value for filters
    }
}
```

#### TimelineRange
```kotlin
@Serializable
@JsonClassDiscriminator("timeline_type")
sealed class TimelineRange {
    @Serializable object AllTime : TimelineRange()
    @Serializable data class ThisPeriod(val period: ThisPeriodType) : TimelineRange()
    @Serializable data class LastPeriod(val period: LastPeriodType) : TimelineRange()
    @Serializable data class CustomRange(val start: Long, val end: Long) : TimelineRange()
}

enum class ThisPeriodType(val label: String) {
    TODAY("Today"), WEEK("This Week"), MONTH("This Month"), QUARTER("This Quarter"), HALF_YEAR("Half Year"), YEAR("This Year")
}

enum class LastPeriodType(val label: String) {
    LAST_24H("24h"), LAST_7_DAYS("7 Days"), LAST_30_DAYS("30 Days"), LAST_12_WEEKS("12 Weeks"), LAST_6_MONTHS("6 Months"), LAST_1_YEAR("1 Year"), LAST_5_YEARS("5 Years")
}
```

#### FilterManager
```kotlin
class FilterManager {
    private val activeFilters = mutableMapOf<String, TransactionFilter>()
    private val _mergedFilter = MutableStateFlow(mergeFilters(emptyList()))
    val mergedFilter: StateFlow<TransactionFilter> = _mergedFilter

    fun setFilter(key: String, filter: TransactionFilter)
    fun removeFilter(key: String)
    fun hasFilter(key: String): Boolean
    fun getFilter(key: String): TransactionFilter?
    fun clear()
    fun getFilterKeys(): Set<String>
    fun getFilterCount(): Int

    private fun mergeFilters(filters: List<TransactionFilter>): TransactionFilter {
        // Merge rules:
        // - Lists: unioned (OR within field)
        // - Single values: last non-null
        // - Ranges: intersection (max start, min end)
        // - Boolean flags: OR logic
        // - Text fields: joined with space
    }
}
```
- **Purpose**: Manage multiple independent filters
- **Merge Logic**: AND logic across filters, OR within fields
- **Reactive**: Merged filter updates automatically

#### Analytics Models
- **ComputationContext**: Groups baseCurrency, exchangeRates, precision
- **FinancialSummary**: totalAmount, income, expense, transactionCount
- **CategoryBreakdownData**: For pie charts with subcategory support
- **TimelineTrajectoryData**: For multi-line charts
- **CashFlowResult**: Total income/expense with currency breakdown

#### DTOs
- **TransactionRequest**: Groups transaction creation parameters
- **TransactionPrefillDTO**: For cloning/prefilling transaction entry

#### Settings Models
- **CardConfig**: Sealed class for all card configurations
- **ScreenContext**: DASHBOARD, ACCOUNT_DETAIL, ACCOUNTS_VIEW, ANALYTICS
- **CardScope**: GLOBAL, ACCOUNT, PROFILE

---

### Repository Interfaces

Domain layer defines interfaces that data layer implements.

#### TransactionRepository
```kotlin
interface TransactionRepository {
    fun getAllTransactions(profileId: Int): Flow<List<Transaction>>
    suspend fun getAllTransactionsSync(profileId: Int): List<Transaction>
    fun getFilteredTransactions(profileId: Int, filter: TransactionFilter): Flow<List<Transaction>>
    suspend fun getFilteredTransactionsSync(profileId: Int, filter: TransactionFilter): List<Transaction>
    suspend fun insertTransaction(transaction: Transaction, profileId: Int): Long
    suspend fun saveTransfer(sourceTransaction: Transaction, targetTransaction: Transaction, profileId: Int): Pair<Long, Long>
    suspend fun deleteTransaction(id: Long)
    suspend fun updateTransaction(oldTransaction: Transaction, newTransaction: Transaction, profileId: Int)
    suspend fun getTransactionsByAccount(accountId: String): Flow<List<Transaction>>
    suspend fun getTransactionById(id: Long): Transaction?
    
    // Link management
    suspend fun addTransactionLink(transactionId: Long, linkedTransactionId: Long)
    suspend fun removeTransactionLink(transactionId: Long, linkedTransactionId: Long)
    suspend fun clearTransactionLinks(transactionId: Long)
    fun getLinkedTransactionIds(transactionId: Long): Flow<List<Long>>
    fun getLinkedTransactions(transactionId: Long): Flow<List<Transaction>>
    
    suspend fun getCurrencySummaries(profileId: Int): List<CurrencySummary>
    suspend fun getCurrencySummariesInRange(profileId: Int, start: Long, end: Long): List<CurrencySummary>
}
```

#### Other Repository Interfaces
- **AccountRepository**: Account CRUD with balance operations
- **BudgetRepository**: Budget management with evaluation trigger
- **CategoryRepository**: Category CRUD
- **CurrencyRepository**: Exchange rate fetching and conversion
- **FilterRepository**: Saved filter CRUD
- **LoanRepository**: Loan metadata CRUD
- **ProfileRepository**: Profile management with current profile tracking
- **SettingsRepository**: All settings via DataStore
- **TagRepository**: Tag CRUD with transaction cleanup
- **TemplateRepository**: Template CRUD with starring
- **ComputationRepository**: Computation context aggregation
- **CardRepository**: Card configuration management
- **BackupRepository**: Database export/import
- **SharedWalletRepository**: Collaborative wallet operations

---

### Use Cases

Use cases encapsulate single business actions.

#### TransactionUseCases
```kotlin
class TransactionUseCases(
    private val repository: TransactionRepository,
    private val sharedWalletRepository: SharedWalletRepository
) {
    fun getTransactions(profileId: Int = 1): Flow<List<Transaction>>
    fun getFilteredTransactions(profileId: Int, filter: TransactionFilter): Flow<List<Transaction>>
    suspend fun insertTransaction(transaction: Transaction, profileId: Int = 1): Long
    suspend fun deleteTransaction(id: Long)
    suspend fun getTransactionById(id: Long): Transaction?
    suspend fun updateTransaction(oldTransaction: Transaction, newTransaction: Transaction, profileId: Int)
    suspend fun saveTransfer(sourceTransaction: Transaction, targetTransaction: Transaction, profileId: Int): Pair<Long, Long>
    suspend fun syncTransactions(walletId: String)
    
    fun getRecentTransactions(
        allTransactions: List<Transaction>,
        accounts: List<Account>,
        exchangeRates: Map<String, BigDecimal>,
        precision: Int,
        limit: Int = 20,
        days: Int = 7
    ): List<TransactionWithBalance>
    
    // Link management
    suspend fun addTransactionLink(transactionId: Long, linkedTransactionId: Long)
    suspend fun removeTransactionLink(transactionId: Long, linkedTransactionId: Long)
    suspend fun clearTransactionLinks(transactionId: Long)
    fun getLinkedTransactionIds(transactionId: Long): Flow<List<Long>>
    fun getLinkedTransactions(transactionId: Long): Flow<List<Transaction>>
}
```
- **Recent Transactions**: Calculates running balances per account
- **Currency Conversion**: Converts amounts for balance calculation
- **Link Management**: Transaction relationship operations

#### BudgetUseCases
```kotlin
class BudgetUseCases(
    private val budgetRepository: BudgetRepository,
    private val transactionRepository: TransactionRepository
) {
    fun getBudgets(): Flow<List<Budget>>
    suspend fun getBudgetById(id: String)
    suspend fun upsertBudget(budget: Budget)
    suspend fun deleteBudget(id: String)
    
    fun getBudgetsWithAnalysis(profileId: Int = 1): Flow<List<BudgetAnalysis>>
}

data class BudgetAnalysis(
    val budget: Budget,
    val dailySafeSpend: BigDecimal,
    val isHighRisk: Boolean,
    val daysRemaining: Int,
    val totalDays: Int,
    val fixedSpent: BigDecimal = BigDecimal.ZERO,
    val variableSpent: BigDecimal = BigDecimal.ZERO
)
```
- **Analysis**: Calculates daily safe spend, risk assessment
- **Fixed vs Variable**: Separates recurring/fixed from variable spending

#### LoanUseCases
```kotlin
class LoanUseCases(
    private val loanRepository: LoanRepository,
    private val transactionRepository: TransactionRepository
) {
    fun getAllLoans(): Flow<List<Loan>>
    suspend fun getLoanByAccountId(accountId: String)
    suspend fun upsertLoan(loan: Loan)
    suspend fun deleteLoan(accountId: String)
    
    fun getLoansWithAnalysis(profileId: Int = 1): Flow<List<LoanAnalysis>>
}

data class LoanAnalysis(
    val loan: Loan,
    val repayments: List<Transaction>,
    val disbursements: List<Transaction>,
    val interestPaid: BigDecimal,
    val principalPaid: BigDecimal,
    val remainingMaturityDays: Int
)
```
- **Analysis**: Tracks repayments vs disbursements, interest vs principal
- **Maturity**: Calculates remaining days to maturity

#### CategoryUseCases
```kotlin
class CategoryUseCases(
    private val categoryDao: CategoryDao
) {
    val categories: Flow<List<CategoryEntity>>
    suspend fun upsertCategory(id: Long, name: String, iconName: String, colorHex: Int, parentId: Long?, isDefault: Boolean)
    suspend fun deleteCategory(category: CategoryEntity)
}
```

#### AccountUseCases
```kotlin
class AccountUseCases(
    private val repository: AccountRepository
) {
    suspend fun deleteAccount(accountId: String)
}
```

---

### Services

Services contain business logic that spans multiple use cases. Stateless and reusable.

#### TransactionsUtilityService

**Purpose**: Reusable service for aggregating, filtering, and breaking down transaction data.

**Key Features**:
- Stateless object (pure Kotlin)
- Currency conversion support
- Multi-currency breakdowns
- Functional patterns for reusability

**Currency Conversion**:
```kotlin
fun convertToBaseCurrency(
    amount: BigDecimal,
    fromCurrency: String,
    toCurrency: String,
    exchangeRates: Map<String, BigDecimal>
): BigDecimal {
    if (fromCurrency == toCurrency) return amount
    val rate = exchangeRates[fromCurrency]
    if (rate == null || rate.signum() == 0) return amount
    // Match CurrencyService logic: amount / rate (where rate is BASE -> CURRENCY)
    return amount.divide(rate, 10, java.math.RoundingMode.HALF_UP)
}
```

**Filtering Methods**:
- `filter(transactions, filter)`: Main filter via FilterService
- `filterIncome(transactions)`: Positive amounts only
- `filterExpense(transactions)`: Negative amounts only
- `filterByCurrency(transactions, currency)`
- `filterByCategory(transactions, category)`
- `filterByAccount(transactions, accountId)`
- `filterByStatus(transactions, status)`
- `filterByAmountRange(transactions, minAmount, maxAmount)`
- `filterByDateRange(transactions, startTime, endTime)`
- `filterByTags(transactions, tags)`: Any tag match
- `filterByAllTags(transactions, tags)`: All tags match

**Grouping Methods**:
- `groupByCurrency(transactions)`
- `groupByCategory(transactions)`
- `groupByAccount(transactions)`
- `groupByTransactionType(transactions)`
- `groupByPaymentMethod(transactions)`
- `groupByStatus(transactions)`
- `groupByPayee(transactions)`
- `groupByDate(transactions)`: yyyy-MM-dd format
- `groupByMonth(transactions)`: yyyy-MM format
- `groupByTags(transactions)`: One transaction can appear in multiple groups

**Calculation Methods**:
- `calculateTransactionSummary(transactions)`: Total, income, expense, count
- `calculateSummaryByCurrency(transactions)`: Per-currency summaries
- `calculateTotalByCurrency(transactions)`: Currency totals
- `separateIncomeExpense(transactions)`: Pair of income/expense lists
- `calculateIncomeExpenseBreakdown(transactions, exchangeRates, baseCurrency)`: Comprehensive breakdown with currency conversion

**Breakdown Methods**:
- `createBreakdown(transactions, selector, mapper, exchangeRates, baseCurrency)`: Generic helper
- `breakdownByCurrency(transactions)`: Currency breakdown with income/expense
- `breakdownByAccount(transactions, accounts, exchangeRates, baseCurrency)`: Account breakdown with currency sub-grouping

**Advanced Calculations**:
- `calculateCumulativeSum(items, initialBalance)`: Running total for trajectory
- `calculateSummaryDelta(current, previous)`: Period-over-period change
- `calculateSavingsRate(income, expense)`: (Income + Expense) / Income
- `calculateBurnRate(totalExpense, daysActive)`: Monthly average expense
- `calculateDailyNetFlow(transactions, days, exchangeRates, baseCurrency)`: Daily net flow map

**Financial Models**:
```kotlin
data class FinancialSummary(
    val totalAmount: BigDecimal,
    val income: BigDecimal,
    val expense: BigDecimal,
    val transactionCount: Int
) {
    companion object {
        val EMPTY = FinancialSummary(BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, 0)
    }
}

data class IncomeExpenseBreakdown(
    val totalIncome: BigDecimal,
    val totalExpense: BigDecimal,
    val netFlow: BigDecimal,
    val incomeTransactions: List<Transaction>,
    val expenseTransactions: List<Transaction>,
    val incomeCurrencyBreakdown: Map<String, FinancialSummary>,
    val expenseCurrencyBreakdown: Map<String, FinancialSummary>
)

data class CurrencyBreakdown(
    val currency: String,
    val financialSummary: FinancialSummary
)

data class AccountBreakdown(
    val account: Account,
    val financialSummary: FinancialSummary,
    val currencyBreakdown: Map<String, FinancialSummary>
)
```

#### FilterService

**Purpose**: Domain service for unified transaction filtering.

**Implementation**:
```kotlin
object FilterService {
    fun filterTransactions(transactions: List<Transaction>, filter: TransactionFilter): List<Transaction> {
        val (startTime, endTime) = TimelineService.getDateRange(filter.timelineRange)
        
        return transactions.filter { tx ->
            // Timeline check
            val effectiveStart = filter.startDate ?: startTime
            val effectiveEnd = filter.endDate ?: endTime
            val inTimeRange = (effectiveStart == null || tx.timestamp >= effectiveStart) &&
                             (effectiveEnd == null || tx.timestamp <= effectiveEnd)
            
            // Attribute checks
            val matchesAccounts = filter.accountIds.isEmpty() || tx.accountId in filter.accountIds
            val matchesCategories = filter.categoryIds.isEmpty() || tx.categoryId in filter.categoryIds
            val matchesType = filter.transactionType == null || tx.transactionType == filter.transactionType
            val matchesAmount = (filter.minAmount == null || tx.amount.abs() >= filter.minAmount) &&
                               (filter.maxAmount == null || tx.amount.abs() <= filter.maxAmount)
            val matchesStatuses = filter.statuses.isEmpty() || tx.status in filter.statuses
            val matchesCurrencies = filter.currencies.isEmpty() || tx.currency in filter.currencies
            val matchesTags = filter.tags.isEmpty() || tx.tags.any { it in filter.tags }
            
            // Payee match (Regex or string contains)
            val matchesPayee = if (filter.isPayeeRegex && !filter.queryPayee.isNullOrBlank()) {
                try {
                    Regex(filter.queryPayee, RegexOption.IGNORE_CASE).containsMatchIn(tx.payee)
                } catch (e: Exception) { false }
            } else if (!filter.queryPayee.isNullOrBlank()) {
                tx.payee.contains(filter.queryPayee, ignoreCase = true)
            } else true

            // Note match (Regex or string contains)
            val matchesNote = if (filter.isNoteRegex && !filter.queryNote.isNullOrBlank()) {
                try {
                    Regex(filter.queryNote, RegexOption.IGNORE_CASE).containsMatchIn(tx.note)
                } catch (e: Exception) { false }
            } else if (!filter.queryNote.isNullOrBlank()) {
                tx.note.contains(filter.queryNote, ignoreCase = true)
            } else true
            
            // Warranty check
            val matchesWarranty = when (filter.hasWarranty) {
                true -> tx.warrantyExpiration != null
                false -> tx.warrantyExpiration == null
                null -> true
            }
            
            val inWarrantyRange = (filter.warrantyExpirationStart == null || (tx.warrantyExpiration ?: 0) >= filter.warrantyExpirationStart) &&
                                 (filter.warrantyExpirationEnd == null || (tx.warrantyExpiration ?: Long.MAX_VALUE) <= filter.warrantyExpirationEnd)

            val matchesPaymentMethod = filter.paymentMethods.isEmpty() || tx.paymentMethod in filter.paymentMethods

            matchesAccounts && matchesCategories && matchesType && matchesAmount && 
            matchesStatuses && matchesPayee && matchesNote && matchesCurrencies &&
            matchesWarranty && inWarrantyRange && matchesPaymentMethod && matchesTags
        }
    }

    fun getPreviousPeriodFilter(currentFilter: TransactionFilter): TransactionFilter {
        val (prevStart, prevEnd) = TimelineService.getPreviousRange(currentFilter.timelineRange)
        return currentFilter.copy(
            timelineRange = TimelineRange.CustomRange(start = prevStart ?: 0L, end = prevEnd ?: System.currentTimeMillis()),
            startDate = null,
            endDate = null
        )
    }
}
```

**Key Features**:
- **Source of Truth**: Single implementation for in-memory filtering
- **Regex Support**: For note and payee fields
- **Timeline Integration**: Uses TimelineService for date ranges
- **Period Comparison**: Helper for previous period filter

#### CurrencyService

**Purpose**: Domain service for currency-related business logic.

**Key Methods**:
```kotlin
object CurrencyService {
    fun convertAmount(
        amount: BigDecimal,
        fromCurrency: String,
        toCurrency: String,
        exchangeRates: Map<String, BigDecimal>,
        precision: Int = 2
    ): BigDecimal {
        if (fromCurrency == toCurrency) return amount.setScale(precision, RoundingMode.HALF_UP)
        val fromRate = exchangeRates[fromCurrency]
        val toRate = exchangeRates[toCurrency]
        if (fromRate == null || toRate == null || fromRate.signum() == 0) {
            return amount.setScale(precision, RoundingMode.HALF_UP)
        }
        val intermediatePrecision = maxOf(precision + 4, 10)
        val inBase = amount.divide(fromRate, intermediatePrecision, RoundingMode.HALF_UP)
        return inBase.multiply(toRate).setScale(precision, RoundingMode.HALF_UP)
    }

    fun calculateTotalInCurrency(
        currencyTotals: Map<String, BigDecimal>,
        targetCurrency: String,
        exchangeRates: Map<String, BigDecimal>,
        precision: Int = 2
    ): BigDecimal

    fun formatAmount(amount: BigDecimal, precision: Int = 2): String
    fun getCurrencySymbol(currencyCode: String?): String
    fun getCurrencyName(currencyCode: String?): String
    fun formatShortNumber(value: BigDecimal): String  // 1000 -> 1K, 1000000 -> 1M
    fun format(amount: BigDecimal, currency: String, precision: Int): String
    fun formatRight(amount: BigDecimal, currency: String, precision: Int): String
    fun formatLeft(amount: BigDecimal, currency: String, precision: Int): String
    fun getAllCurrencies(): List<String>
}
```

**Features**:
- **Conversion**: Two-step via base currency for accuracy
- **Formatting**: Localized grouping and decimal separators
- **Symbols**: Java Currency API for symbols
- **Short Numbers**: K, M, B suffixes for large values

#### TimelineService

**Purpose**: Domain service for timeline and filtering business logic.

**Key Methods**:
```kotlin
object TimelineService {
    fun getRangeForThis(period: ThisPeriodType, firstDayOfWeek: DayOfWeek = DayOfWeek.MONDAY): Pair<Long, Long>
    fun getRangeForLast(period: LastPeriodType): Pair<Long, Long>
    fun getDateRange(timelineRange: TimelineRange): Pair<Long?, Long?>
    fun getPreviousRange(timelineRange: TimelineRange): Pair<Long?, Long?>
    fun <T : Timestamped> filterByTimelineRange(items: List<T>, timelineRange: TimelineRange): List<T>
}
```

**Features**:
- **This Period**: TODAY, WEEK, MONTH, QUARTER, HALF_YEAR, YEAR
- **Last Period**: LAST_24H, LAST_7_DAYS, LAST_30_DAYS, LAST_12_WEEKS, LAST_6_MONTHS, LAST_1_YEAR, LAST_5_YEARS
- **Custom Range**: Arbitrary start/end timestamps
- **Previous Period**: Calculates period immediately preceding current
- **Timezone**: Uses system default timezone

#### TransactionManager

**Purpose**: Service for centralizing transaction lifecycle logic.

**Key Methods**:
```kotlin
class TransactionManager(
    private val transactionUseCases: TransactionUseCases
) {
    suspend fun handleSaveTransaction(
        existingTransaction: Transaction?,
        request: TransactionRequest,
        profileId: Int
    ): Long

    suspend fun cloneTransaction(transactionId: Long, profileId: Int): Long?
    suspend fun toggleStatus(transactionId: Long, profileId: Int): TransactionStatus?
    suspend fun updateStatus(transactionId: Long, newStatus: TransactionStatus, profileId: Int): Boolean
}
```

**Features**:
- **Save Handling**: Centralizes create/update logic
- **Transfer Support**: Handles transfer creation/update
- **Clone**: Creates new transaction with current timestamp
- **Status Toggle**: Cleared <-> Uncleared toggle
- **Link Cleanup**: Clears links when changing from Transfer to Normal

#### CardComputationEngine

**Purpose**: Unified interface for card computation engines.

```kotlin
interface CardComputationEngine {
    suspend fun computeNetWorth(
        config: NetWorthCardConfig,
        accounts: List<Account>,
        allTransactions: List<Transaction>,
        context: ComputationContext
    ): Pair<BigDecimal, CashFlowResult>
    
    suspend fun computeCurrencyTotals(
        config: CurrencyBalancesCardConfig,
        accounts: List<Account>,
        allTransactions: List<Transaction>,
        context: ComputationContext
    ): List<CurrencyAggregationItem>
    
    suspend fun computeCashFlow(
        config: CashFlowCardConfig,
        allTransactions: List<Transaction>,
        context: ComputationContext
    ): CashFlowResult
    
    suspend fun computeAccountStats(
        config: AccountsCardConfig,
        accounts: List<Account>,
        allTransactions: List<Transaction>,
        context: ComputationContext
    ): Pair<List<Account>, Map<String, AccountSummaryItem>>
}
```

**Purpose**: Abstraction allowing different implementations (CardComputationService, FinancialUseCases) to be used interchangeably.

#### CardComputationService

**Purpose**: Service for computing card data with scope-aware filtering.

**Implementation**: Uses TransactionsUtilityService for computations.

**Methods**:
- `computeCategoryBreakdown`: Pie chart data
- `computeTimelineTrajectory`: Multi-line chart data
- `computeCategoryExpenseTreemap`: Treemap data
- `computeIncomeExpensesSummary`: Summary statistics
- `computePeriodComparison`: Period-over-period comparison
- `computeCashFlowTrend`: Bar + line trend
- `computeCashFlowProgress`: Progress indicators
- `computeBalanceForecast`: Balance projection
- `computePlannedPayments`: Planned payment analysis
- Plus methods from CardComputationEngine interface

---

## Filter Mechanism

### Overview

The filter mechanism provides flexible transaction filtering with support for:
- Multiple criteria (accounts, categories, dates, amounts, etc.)
- Regex pattern matching for text fields
- Timeline-based filtering (This Period, Last Period, Custom)
- Warranty tracking
- Multi-currency support
- Saved filters for reuse

### Filter Flow

```
UI Filter Input
        ↓
TransactionFilter (domain model)
        ↓
FilterService.filterTransactions() (in-memory)
        ↓
Filtered Transactions
```

### Database vs In-Memory Filtering

**Database Filtering** (TransactionRepositoryImpl):
```kotlin
private fun buildFilteredQuery(profileId: Int, filter: TransactionFilter): Pair<String, List<Any>> {
    val queryBuilder = StringBuilder("SELECT * FROM transactions WHERE profileId = ?")
    val args = mutableListOf<Any>(profileId)

    if (filter.startDate != null) {
        queryBuilder.append(" AND timestamp >= ?")
        args.add(filter.startDate)
    }
    if (filter.accountIds.isNotEmpty()) {
        val placeholders = filter.accountIds.joinByComma { "?" }
        queryBuilder.append(" AND accountId IN ($placeholders)")
        args.addAll(filter.accountIds)
    }
    // ... more conditions
    return Pair(queryBuilder.toString(), args)
}
```

**In-Memory Filtering** (FilterService):
```kotlin
fun filterTransactions(transactions: List<Transaction>, filter: TransactionFilter): List<Transaction> {
    val (startTime, endTime) = TimelineService.getDateRange(filter.timelineRange)
    return transactions.filter { tx ->
        // All filter conditions applied
        matchesAccounts && matchesCategories && matchesType && matchesAmount && 
        matchesStatuses && matchesPayee && matchesNote && matchesCurrencies &&
        matchesWarranty && inWarrantyRange && matchesPaymentMethod && matchesTags
    }
}
```

### FilterUtils (UI Helper)

```kotlin
fun countActiveFilters(filter: TransactionFilter): Int {
    var count = 0
    count += filter.accountIds.size
    if (filter.transactionType != null) count++
    count += filter.categoryIds.size
    count += filter.currencies.size
    if (filter.startDate != null || filter.endDate != null) count++
    count += filter.paymentMethods.size
    if (!filter.queryNote.isNullOrBlank()) count++
    if (!filter.queryPayee.isNullOrBlank()) count++
    if (filter.minAmount != null || filter.maxAmount != null) count++
    count += filter.statuses.size
    return count
}

fun removeFilter(filter: TransactionFilter, field: String, value: Any?, onUpdateFilter: (TransactionFilter) -> Unit) {
    val updatedFilter = when (field) {
        "accountIds" -> filter.copy(accountIds = filter.accountIds - (value as String))
        "transactionType" -> filter.copy(transactionType = null)
        "dateRange" -> filter.copy(startDate = null, endDate = null)
        // ... more fields
        else -> filter
    }
    onUpdateFilter(updatedFilter)
}

fun formatDateRange(startDate: Long?, endDate: Long?): String {
    val start = startDate?.let { SimpleDateFormat("MMM dd", Locale.getDefault()).format(Date(it)) } ?: "Start"
    val end = endDate?.let { SimpleDateFormat("MMM dd", Locale.getDefault()).format(Date(it)) } ?: "End"
    return "$start - $end"
}
```

### FilterManager

**Purpose**: Manage multiple independent filters with automatic merging.

```kotlin
class FilterManager {
    private val activeFilters = mutableMapOf<String, TransactionFilter>()
    private val _mergedFilter = MutableStateFlow(mergeFilters(emptyList()))
    val mergedFilter: StateFlow<TransactionFilter> = _mergedFilter

    fun setFilter(key: String, filter: TransactionFilter) {
        activeFilters[key] = filter
        _mergedFilter.value = mergeFilters(activeFilters.values.toList())
    }
    
    fun removeFilter(key: String) {
        activeFilters.remove(key)
        _mergedFilter.value = mergeFilters(activeFilters.values.toList())
    }
}
```

**Merge Logic**:
- **Lists** (categoryIds, accountIds): Unioned (OR within field)
- **Single values** (transactionType, hasWarranty): Last non-null
- **Ranges** (startDate/endDate, minAmount/maxAmount): Intersection (max start, min end)
- **Boolean flags** (isNoteRegex, isPayeeRegex): OR logic
- **Text fields** (queryNote, queryPayee): Joined with space

---

## Compute Logic

### ComputationContext

**Purpose**: Groups recurring parameters to simplify function signatures.

```kotlin
data class ComputationContext(
    val baseCurrency: String,
    val exchangeRates: Map<String, BigDecimal>,
    val precision: Int
)
```

**Usage**: Passed to computation services to avoid parameter repetition.

### TransactionsUtilityService Breakdown

**Financial Summary Calculation**:
```kotlin
fun calculateTransactionSummary(transactions: List<Transaction>): FinancialSummary {
    if (transactions.isEmpty()) return EMPTY_SUMMARY
    
    val currencies = transactions.map { it.currency }.distinct()
    if (currencies.size > 1) {
        throw IllegalArgumentException("calculateTransactionSummary can only process transactions with the same currency")
    }
    
    val total = transactions.sumOf { it.amount }
    val (incomeTransactions, expenseTransactions) = transactions.partition { it.amount.signum() >= 0 }
    val income = incomeTransactions.sumOf { it.amount }
    val expense = expenseTransactions.sumOf { it.amount }
    
    return FinancialSummary(total, income, expense, transactions.size)
}
```

**Currency Conversion**:
```kotlin
fun convertToBaseCurrency(
    amount: BigDecimal,
    fromCurrency: String,
    toCurrency: String,
    exchangeRates: Map<String, BigDecimal>
): BigDecimal {
    if (fromCurrency == toCurrency) return amount
    val rate = exchangeRates[fromCurrency]
    if (rate == null || rate.signum() == 0) return amount
    return amount.divide(rate, 10, java.math.RoundingMode.HALF_UP)
}
```

**Generic Breakdown Helper**:
```kotlin
fun <K, R> createBreakdown(
    transactions: List<Transaction>,
    selector: (Transaction) -> K,
    mapper: (K, Map<String, FinancialSummary>) -> R,
    exchangeRates: Map<String, BigDecimal>? = null,
    baseCurrency: String? = null
): List<R> where K : Any {
    return transactions.groupBy(selector)
        .map { (key, txs) ->
            val summaries = calculateSummaryByCurrency(txs)
            val convertedSummaries = if (exchangeRates != null && baseCurrency != null) {
                summaries.mapValues { (currency, summary) ->
                    convertFinancialSummary(summary, currency, baseCurrency, exchangeRates)
                }
            } else {
                summaries
            }
            mapper(key, convertedSummaries)
        }
}
```

### Card Computation Flow

```
CardConfig (with filter)
        ↓
TransactionsUtilityService.filter(transactions, config.filter)
        ↓
TransactionsUtilityService.calculateIncomeExpenseBreakdown()
        ↓
Currency Conversion (if exchangeRates provided)
        ↓
Card-Specific Data Transformation
        ↓
UI-Ready Data (CategoryBreakdownData, TimelineTrajectoryData, etc.)
```

---

## Data Flow Patterns

### Transaction Creation Flow

```
UI collects input
        ↓
TransactionRequest DTO
        ↓
TransactionManager.handleSaveTransaction()
        ↓
TransactionUseCases.insertTransaction()
        ↓
TransactionRepository.insertTransaction()
        ↓
TransactionDao.insertTransactionWithBalanceUpdate()
        ↓
TransactionEntity inserted + AccountDao.updateBalance()
        ↓
Flow updates trigger UI recomposition
```

### Analytics Computation Flow

```
ViewModel provides CardConfig
        ↓
CardComputationService.compute[CardType]()
        ↓
TransactionsUtilityService.filter(transactions, config.filter)
        ↓
TransactionsUtilityService.calculate[BreakdownType]()
        ↓
Currency conversion (if needed)
        ↓
UI-ready data models
        ↓
UI renders charts/summaries
```

### Filter Application Flow

```
User modifies filter in UI
        ↓
ViewModel updates filter StateFlow
        ↓
FilterManager.setFilter() (if using FilterManager)
        ↓
mergedFilter StateFlow updates
        ↓
ViewModel observes filter changes
        ↓
Repository.getFilteredTransactions() or FilterService.filterTransactions()
        ↓
Filtered transactions Flow
        ↓
UI recomposes with filtered data
```

---

## Currency Conversion

### Exchange Rate Structure

Based on CurrencyRateEntity:
```kotlin
data class CurrencyRateEntity(
    val currencyCode: String,  // e.g., "EUR"
    val baseCurrency: String,    // e.g., "USD"
    val rate: Double,            // e.g., 0.85 (1 USD = 0.85 EUR)
    val timestamp: Long,
    val nextUpdate: Long
)
```

### Conversion Logic

**CurrencyService.convertAmount**:
```kotlin
fun convertAmount(amount, fromCurrency, toCurrency, exchangeRates, precision) {
    // Step 1: Convert to base currency
    val inBase = amount.divide(fromRate, intermediatePrecision, HALF_UP)
    
    // Step 2: Convert from base to target
    return inBase.multiply(toRate).setScale(precision, HALF_UP)
}
```

**Example**: Convert 100 EUR to GBP with USD as base
- EUR rate: 0.85 (1 USD = 0.85 EUR)
- GBP rate: 0.75 (1 USD = 0.75 GBP)
- 100 EUR / 0.85 = 117.65 USD
- 117.65 USD * 0.75 = 88.24 GBP

### Cross-Rate Derivation

When cached rates are in a different base:
```kotlin
val newBaseRate = rates[baseCurrency]
if (newBaseRate != null && newBaseRate != 0.0) {
    return@withContext rates.mapValues { it.value / newBaseRate }
}
```

**Example**: Cache has EUR as base, need USD rates
- EUR -> USD: 1.10
- EUR -> GBP: 0.85
- USD -> GBP = 0.85 / 1.10 = 0.77

### Cache Strategy

- **Validity Check**: Uses provider's nextUpdate timestamp
- **Fallback**: 24 hours if nextUpdate not available
- **Force Refresh**: Parameter to bypass cache
- **Error Handling**: Returns cached data on fetch failure

---

## Appendix

### File Organization

```
com.osscabinet.ossWallet/
├── data/
│   ├── local/
│   │   ├── entity/        # Room entities (11 files)
│   │   ├── dao/           # Room DAOs (11 files)
│   │   ├── database/      # AppDatabase setup
│   │   ├── Converters.kt  # Type converters
│   │   ├── SettingsDataStore.kt
│   │   └── BackupModels.kt
│   ├── mapper/            # Entity ↔ Domain model mappers
│   ├── remote/            # API interfaces
│   ├── repository/        # Repository implementations (15 files)
│   └── seed/              # Database seeding
├── domain/
│   ├── model/             # Domain models (27 files)
│   │   ├── analytics/     # Analytics models
│   │   ├── dto/           # Data transfer objects
│   │   ├── filter/        # Filter operators
│   │   ├── settings/      # Card configurations
│   │   └── timeline/      # Timeline models
│   ├── repository/        # Repository interfaces (15 files)
│   ├── usecase/           # Use cases (5 files)
│   ├── service/           # Business logic services (7 files)
│   ├── util/              # Domain utilities
│   ├── calculator/        # Financial calculations
│   └── formatter/         # Formatting utilities
└── ui/
    ├── viewmodel/         # ViewModels
    ├── screens/           # Compose screens
    ├── components/        # Reusable components
    └── navigation/        # Navigation setup
```

### Key Dependencies

**Data Layer**:
- Room (AndroidX)
- Retrofit (network)
- Kotlin coroutines
- Kotlin serialization

**Domain Layer**:
- Kotlin stdlib
- Kotlin coroutines
- Java BigDecimal
- Java time API

**UI Layer**:
- Jetpack Compose
- AndroidX Navigation
- AndroidX ViewModel
- Kotlin coroutines
- Hilt/Koin (DI)

### Best Practices

1. **No Android in Domain**: Domain layer must not import Android classes
2. **Immutable StateFlow**: Always expose via `.asStateFlow()`
3. **Repository Pattern**: Interface in domain, implementation in data
4. **Flow for Streams**: Use Flow for reactive data streams
5. **BigDecimal for Money**: Use BigDecimal for all monetary values
6. **Timestamps as Long**: Store timestamps as Long (UTC milliseconds)
7. **Constructor Injection**: Use DI for all dependencies
8. **Single Responsibility**: One use case = one business action
9. **Stateless Services**: Services should be stateless objects
10. **Mapper Functions**: Separate entity ↔ domain model conversion

---

*Documentation generated for OssWallet backend architecture*
