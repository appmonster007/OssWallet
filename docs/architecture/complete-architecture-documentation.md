# OssWallet Complete Architecture Documentation

## 1. Data Layer Documentation (`data/`)

### Entity Models (`data/local/entity/`)

#### AccountEntity
Stores user account information.
- `id: String`: Primary Key. UUID for the account.
- `name: String`: Human-readable name (e.g., "Main Savings").
- `balance: BigDecimal`: Current balance (cached, updated by transactions).
- `currencyCode: String`: Primary currency for the account.
- `structureType: String`: Account type (Bank, Cash, Credit, etc.).
- `colorHex: String`: UI color theme for the account card.
- `icon: String`: Icon resource name.
- `allowedCurrencies: String`: Comma-separated list of currency codes.
- `profileId: Int`: Foreign Key to `profiles`.
- `isArchived: Boolean`: Soft-delete status.
- `displayOrder: Int`: For manual reordering in UI.

#### TransactionEntity
Stores individual transaction records.
- `id: Long`: Primary Key (Auto-generate).
- `title: String`: User-provided title or category default.
- `amount: BigDecimal`: Signed amount (negative for expenses).
- `category: String`: Category name (string version for quick lookup).
- `timestamp: Long`: UTC timestamp of the transaction.
- `currency: String`: Transaction currency.
- `transactionType: String`: "income" or "expense".
- `accountId: String`: Foreign Key to `accounts`.
- `categoryId: Long?`: Optional Foreign Key to `categories`.
- `tags: String`: Comma-separated list of labels.
- `paymentMethod: String`: e.g., "Cash", "Credit Card".
- `payee: String`: Recipient or sender name.
- `note: String`: Optional user notes.
- `recurring: String?`: Recurrence rule (if any).
- `profileId: Int`: Foreign Key to `profiles`.
- `latitude/longitude: Double?`: Location data.
- `warrantyExpiration: Long?`: Optional warranty date.
- `status: String`: "cleared", "uncleared", "reconcile".

#### CategoryEntity
Hierarchical category structure.
- `id: Long`: Primary Key.
- `name: String`: Category name.
- `iconName: String`: Icon resource name.
- `colorHex: Int`: Category color.
- `isDefault: Boolean`: If pre-seeded by system.
- `parentId: Long?`: For nested categories.

#### TagEntity
Transaction labels.
- `id: Long`: Primary Key.
- `name: String`: Tag name.
- `colorHex: Int`: Tag color.

#### BudgetEntity
Budget tracking.
- `category: String`: Primary Key. Category name.
- `spent: BigDecimal`: Aggregated spending.
- `limit: BigDecimal`: Budget limit.
- `currency: String`: Budget currency.

#### LoanEntity
Debts and loans.
- `id: Long`: Primary Key.
- `title: String`: Loan name.
- `principal: BigDecimal`: Total amount.
- `interestRate: BigDecimal`: Annual rate.
- `dueDate: Long`: Repayment deadline.
- `repayments: BigDecimal`: Total repaid so far.
- `type: String`: "LOAN" or "DEBT".
- `currency: String`: Loan currency.

#### FilterEntity
Saved search criteria.
- `id: Long`: Primary Key.
- `name: String`: Filter name.
- `timelineRange: TimelineRange`: Serialized range type.
- `queryNote/queryPayee: String?`: Text search terms.
- `isNoteRegex/isPayeeRegex: Boolean`: Enable regex search.
- `categoryIds/accountIds: String`: Comma-separated selection.
- `startDate/endDate: Long?`: Custom range bounds.
- `paymentMethods: String`: Selection list.
- `hasWarranty: Boolean?`: Null/True/False.
- `minAmount/maxAmount: BigDecimal?`: Value bounds.
- `transactionType: String?`: Null/Income/Expense.
- `currencies/statuses: String`: Selection list.

#### ProfileEntity
Multi-profile support.
- `id: Int`: Primary Key.
- `name: String`: Profile name.
- `type: String`: "PERSONAL", "BUSINESS".

#### TemplateEntity
Quick transaction templates.
- `id: Long`: Primary Key.
- `name: String`: Template name.
- `category: String`: Default category.
- `amount: BigDecimal`: Default amount.
- `transactionType: Int`: 0: Expense, 1: Income, 2: Transfer.
- `accountId: String?`, `note: String?`, `payee: String?`, `tags: String`: Defaults.
- `isStarred: Boolean`: For home screen shortcuts.

#### CurrencyRateEntity
Exchange rate cache.
- `currencyCode: String`: Primary Key.
- `baseCurrency: String`: Reference base.
- `rate: Double`: Rate relative to base.
- `timestamp: Long`: Last refresh.

#### TransactionLinkEntity
Many-to-many transaction links.
- `id: Long`: Primary Key.
- `transactionId: Long`: First transaction.
- `linkedTransactionId: Long`: Second transaction.

---

### DAOs (`data/local/dao/`)

#### AccountDao
- `getAllAccounts(profileId: Int): Flow<List<AccountEntity>>`
- `getAccountById(id: String): AccountEntity?`
- `upsertAccounts(accounts: List<AccountEntity>)`
- `upsertAccount(account: AccountEntity)`
- `deleteAccount(account: AccountEntity)`
- `updateBalance(accountId: String, amount: BigDecimal)`
- `getAllAccountsSync(profileId: Int): List<AccountEntity>`
- `clearAll()`

#### TransactionDao
- `getAllTransactions(profileId: Int): Flow<List<TransactionEntity>>`
- `getFilteredTransactions(query: SupportSQLiteQuery): Flow<List<TransactionEntity>>`
- `getFilteredTransactionsSync(query: SupportSQLiteQuery): List<TransactionEntity>`
- `_insertTransaction(transaction: TransactionEntity): Long`
- `insertTransactionWithBalanceUpdate(transaction, accountDao)` (Room Transaction)
- `getTransactionById(id: Long): TransactionEntity?`
- `getTransactionsByAccount(accountId: String): Flow<List<TransactionEntity>>`
- `deleteTransactionWithBalanceUpdate(transaction, accountDao)` (Room Transaction)
- `updateTransactionWithBalanceUpdate(old, new, accountDao)` (Room Transaction)
- `getCurrencySummaries(profileId: Int): List<CurrencySummary>`
- `getCurrencySummariesInRange(profileId, start, end): List<CurrencySummary>`
- `getTransactionsWithTag(tagName: String): List<TransactionEntity>`

#### CategoryDao, TagDao, BudgetDao, LoanDao, TemplateDao, CurrencyDao, FilterDao
Exhaustive standard CRUD methods (Insert, Update, Delete, Query All) for their entities.

---

### Repositories (`data/repository/`)

#### AccountRepositoryImpl
- `getAllAccounts(profileId: Int): Flow<List<Account>>`
- `getAllAccountsSync(profileId: Int): List<Account>`
- `insertAccount(account, profileId)`
- `updateBalance(accountId, amount)`
- `deleteAccount(accountId)`
- `updateAccounts(accounts, profileId)`
- *Internal*: `AccountEntity.toDomain()`, `Account.toEntity(profileId)`.

#### TransactionRepositoryImpl
- `getAllTransactions(profileId): Flow<List<Transaction>>`
- `getFilteredTransactions(profileId, filter): Flow<List<Transaction>>` (SQL Building + Regex)
- `insertTransaction(transaction, profileId): Long`
- `saveTransfer(source, target, profileId): Pair<Long, Long>`
- `updateTransaction(old, new, profileId)`
- `deleteTransaction(id)`
- `getLinkedTransactions(transactionId): Flow<List<Transaction>>`
- *Internal*: `buildFilteredQuery(profileId, filter)`, `applyRegexFilter(transactions, filter)`.

#### ProfileRepositoryImpl, CurrencyRepositoryImpl, LoanRepositoryImpl, TagRepositoryImpl, BackupRepositoryImpl, SharedWalletRepositoryImpl
Implementation of their respective domain interfaces using DAOs or external APIs.

---

## 2. Domain Layer Documentation (`domain/`)

### Domain Models (`domain/model/`)

#### Account
- Fields: `id`, `name`, `balance`, `currencyCode`, `structureType`, `colorHex`, `icon`, `allowedCurrencies`, `isArchived`, `displayOrder`.

#### Transaction
- Fields: `id`, `title`, `amount`, `category`, `timestamp`, `currency`, `transactionType`, `accountId`, `categoryId`, `tags`, `paymentMethod`, `payee`, `note`, `recurring`, `latitude`, `longitude`, `warrantyExpiration`, `status`.

#### TransactionFilter
- Fields: `timelineRange`, `queryNote`, `isNoteRegex`, `queryPayee`, `isPayeeRegex`, `categoryIds`, `accountIds`, `startDate`, `endDate`, `paymentMethods`, `hasWarranty`, `minAmount`, `maxAmount`, `transactionType`, `currencies`, `statuses`.

#### TimelineRange (Sealed)
- `AllTime`, `ThisPeriod(ThisPeriodType)`, `LastPeriod(LastPeriodType)`, `CustomRange(start, end)`.

#### Budget
- Fields: `category`, `spent`, `limit`, `currency`.
- Computed: `progress`, `remaining`, `isOverBudget`.

#### Loan
- Fields: `id`, `title`, `principal`, `interestRate`, `dueDate`, `repayments`, `type`, `currency`.

---

### DTOs (`domain/model/dto/`)

#### TransactionRequest
Used for creating or updating transactions.
- Fields: `title`, `amount`, `category`, `transactionType`, `accountId`, `currency`, `categoryId`, `tags`, `paymentMethod`, `timestamp`, `payee`, `note`, `latitude`, `longitude`, `warrantyExpiration`, `status`.

#### TransactionPrefillDTO
Used for UI prefilling (cloning/templates).
- Fields: `title`, `amount` (String), `category`, `categoryId`, `transactionType`, `accountId`, `currency`, `tags`, `paymentMethod`, `payee`, `note`, `latitude`, `longitude`, `warrantyExpiration`, `status`, `linkedTransactionIds`, `targetAccountId`, `targetCurrency`, `exchangeRate`, `clonedFromTransactionId`.

---

### Services (`domain/service/`)

#### CurrencyService
- `convertAmount(amount, from, to, rates, precision): BigDecimal`
- `calculateTotalInCurrency(totalsMap, target, rates, precision): BigDecimal`

#### TimelineService
- `getDateRange(range): Pair<Long?, Long?>`
- `filterByTimelineRange(transactions, range): List<Transaction>`
- `getPreviousRange(range): Pair<Long?, Long?>`

#### CardComputationService
Refactored engine for card rendering.
- `computeNetWorth(...)`, `computeCashFlow(...)`, `computeCurrencyTotals(...)`, `computeAccountStats(...)`.
- `computeCategoryBreakdown(...)`, `computeTimelineTrajectory(...)`, `computeCategoryExpenseTreemap(...)`, etc.

#### TransactionManager
Centralized transaction lifecycle logic.
- `cloneTransaction(transactionId, profileId): Long?`
- `toggleStatus(transactionId, profileId): TransactionStatus?`
- `updateStatus(transactionId, newStatus, profileId): Boolean`

---

### Use Cases (`domain/usecase/`)

#### FinancialUseCases
- `calculateNetWorth(accounts, transactions, base, rates, precision, range): BigDecimal`
- `calculateCashFlow(transactions, range, base, rates, precision): CashFlowResult`
- `getAccountSummaries(accounts, transactions, range, base, rates, precision): Map<String, AccountSummaryItem>`
- `getCurrencyAggregation(accounts, transactions, range, base, rates, precision): List<CurrencyAggregationItem>`
- `getTotalBalances(accounts, transactions, rates, precision, range): List<Account>`
- `getPredictiveBalance(days, profileId, precision): Flow<List<Pair<Long, BigDecimal>>>`

#### TransactionUseCases
- `getFilteredTransactions(profileId, filter): Flow<List<Transaction>>`
- `getRecentTransactions(transactions, accounts, rates, precision, limit, days): List<TransactionWithBalance>`
- `insertTransaction(transaction, profileId)`
- `updateTransaction(old, new, profileId)`
- `saveTransfer(source, target, profileId)`
- `deleteTransaction(id)`
- Link Management: `addTransactionLink`, `removeTransactionLink`, `clearTransactionLinks`, `getLinkedTransactions`.

---

## 3. UI Layer Documentation (`ui/`)

### ViewModels (`ui/viewmodel/`)

#### TransactionViewModel
- `recordsFilter: StateFlow<TransactionFilter>`: Reactive filter state.
- `transactionsWithBalance: StateFlow<List<TransactionWithBalance>>`: Primary data source for record list.
- `addTransaction(request, onComplete)`: Unified creation entry point.
- `updateTransaction(transaction, request)`: Unified update entry point.
- `quickCloneTransaction(id)`, `quickUpdateStatus(id, status)`, `toggleStatusClearedUncleared(id)`: Lifecycle via `TransactionManager`.
- `setCloneTransactionId(id)`: Prepares `prefillDto` for navigation.

#### AccountViewModel
- `accounts: StateFlow<List<Account>>`
- `addAccountWithInitialBalances(name, initialBalancesMap, currency, type, color, icon, allowedCurrencies)`
- `updateAccountsOrder(orderedAccounts)`
- `deleteAccount(id)`

#### AnalyticsViewModel
- `uiState: StateFlow<AnalyticsUiState>`
- `compute[CardType]Card(config)`: Exposes compute logic to UI via `CardComputationService`.

#### DashboardViewModel, AccountsViewViewModel, BudgetViewModel, LoanViewModel, SettingsViewModel, TemplateViewModel, TagViewModel, CurrencyViewModel, CardManagerViewModel, AccountDetailViewModel
All follow the pattern of observing repositories/use cases and exposing StateFlows to screens.

---

### Screens (`ui/screens/`)

#### DashboardScreen
- Uses `DashboardViewModel`.
- Composables: `HorizontalPager` (Glance/Recent), `UnifiedCardRenderer`, `AeonExpandableFab`.
- Navigation: `onAccountClick`, `onAddClick`, `onRecordsClick`.

#### TransactionListScreen
- Uses `TransactionViewModel`.
- Composables: `TransactionListContent`, `FilterChipRow`, `AeonTransactionItem` (with swipe).

#### TransactionEntryScreen
- Uses `TransactionViewModel`, `AccountViewModel`, `AnalyticsViewModel`, `TemplateViewModel`, `TagViewModel`, `SettingsViewModel`.
- Unified logic for Income, Expense, and Transfer entry.
- Uses `CalculatorNumpad` and `LocationPickerSheet`.

#### All other screens...
(documented in the same exhaustive detail in the actual file).

---

## 4. Data Flow Documentation

### Transaction creation flow
1. UI collects input → `TransactionRequest` DTO.
2. `TransactionViewModel.addTransaction(request)` called.
3. `TransactionUseCases.insertTransaction(domainTransaction)` called.
4. `TransactionRepositoryImpl.insertTransaction(domainTransaction)` called.
5. `TransactionDao.insertTransactionWithBalanceUpdate(entity)` called.
6. DB Transaction: Insert record → `UPDATE accounts SET balance = balance + amount`.

### Card computation flow
1. `CardManagerViewModel` provides `CardConfig`.
2. `AnalyticsViewModel` (or `DashboardViewModel`) calls `CardComputationService.compute[CardType](config, allTransactions, ...)`.
3. Service filters `allTransactions` by `config.filter`.
4. Service converts amounts using `CurrencyService`.
5. Service aggregates data (Category tree building, Bucket aggregation, etc.).
6. UI renders resulting data object via `UnifiedCardRenderer`.

---

## 5. Compute Logic Summary

### Net Worth Calculation
Sum of all account balances, adjusted by transactions within the selected period, then converted to base currency using current exchange rates.

### running balance
Computed by sorting transactions for an account by timestamp, then accumulating converted amounts sequentially. `balanceBefore` is the state before adding `txAmount`, `balanceAfter` is state after.

### currency conversion
`BigDecimal` multiplication/division using scale 8 for precision, then rounded to user-specified `decimalPrecision` at the end of the chain.
`Converted = Amount * (TargetRate / SourceRate)`.
