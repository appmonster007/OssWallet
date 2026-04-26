# OssWallet Complete Architecture Documentation

## Overview
OssWallet is a personal finance Android application built with Kotlin and Jetpack Compose. It follows the Clean Architecture principles with MVVM (Model-View-ViewModel) pattern.

---

## 1. Data Layer Documentation (`data/`)

### Entity Models (`data/local/entity/`)

#### AccountEntity
Stores information about user accounts.
- `id: String`: Unique identifier (UUID).
- `name: String`: Account name.
- `balance: BigDecimal`: Current balance.
- `currencyCode: String`: Primary currency code.
- `structureType: String`: Account type (e.g., Bank, Cash).
- `colorHex: String`: Hex color code for UI.
- `icon: String`: Icon name/identifier.
- `allowedCurrencies: String`: Comma-separated list of currency codes allowed for this account.
- `profileId: Int`: Reference to the owner profile.
- `isArchived: Boolean`: Archive status.
- `displayOrder: Int`: Order for UI display.
- **Indices**: `profileId`.

#### TransactionEntity
Stores individual transaction records.
- `id: Long`: Auto-generated primary key.
- `title: String`: Transaction title.
- `amount: BigDecimal`: Transaction amount (signed).
- `category: String`: Category name.
- `timestamp: Long`: Date and time of transaction.
- `currency: String`: Transaction currency.
- `transactionType: String`: "income" or "expense".
- `accountId: String`: Reference to the associated account.
- `categoryId: Long?`: Reference to the category entity.
- `tags: String`: Comma-separated list of tags.
- `paymentMethod: String`: Payment method used.
- `payee: String`: Payee or payer name.
- `note: String`: Optional notes.
- `recurring: String?`: Recurring pattern if applicable.
- `profileId: Int`: Reference to the owner profile.
- `latitude: Double?`, `longitude: Double?`: Location coordinates.
- `warrantyExpiration: Long?`: Warranty end date.
- `status: String`: Transaction status ("cleared", "uncleared", "reconcile").
- **Foreign Keys**: `accountId` (AccountEntity), `categoryId` (CategoryEntity).
- **Indices**: `accountId`, `categoryId`, `profileId`, `timestamp`, `status`.

#### CategoryEntity
Stores transaction categories.
- `id: Long`: Auto-generated primary key.
- `name: String`: Category name.
- `iconName: String`: Icon identifier.
- `colorHex: Int`: Color integer.
- `isDefault: Boolean`: Whether it's a pre-seeded category.
- `parentId: Long?`: Reference for hierarchical categories.

#### TagEntity
Stores transaction labels/tags.
- `id: Long`: Auto-generated primary key.
- `name: String`: Tag name.
- `colorHex: Int`: Color integer.

#### BudgetEntity
Stores budget limits per category.
- `category: String`: Category name (Primary Key).
- `spent: BigDecimal`: Amount spent so far.
- `limit: BigDecimal`: Budget limit.
- `currency: String`: Budget currency.

#### LoanEntity
Stores loan and debt records.
- `id: Long`: Auto-generated primary key.
- `title: String`: Loan title.
- `principal: BigDecimal`: Initial amount.
- `interestRate: BigDecimal`: Annual interest rate.
- `dueDate: Long`: Expected repayment date.
- `repayments: BigDecimal`: Total amount repaid.
- `type: String`: "LOAN" or "DEBT".
- `currency: String`: Loan currency.

#### FilterEntity
Stores saved transaction filters.
- `id: Long`: Auto-generated primary key.
- `name: String`: Saved filter name.
- `timelineRange: TimelineRange`: Serialized timeline range.
- `queryNote: String?`, `queryPayee: String?`: Text search criteria.
- `categoryIds: String`, `accountIds: String`: Comma-separated lists of selected IDs.
- `startDate: Long?`, `endDate: Long?`: Custom date range.
- `minAmount: BigDecimal?`, `maxAmount: BigDecimal?`: Amount range.
- `transactionType: String?`: Income, Expense, or null.
- `currencies: String`, `statuses: String`: Comma-separated criteria.

#### ProfileEntity
Stores user profiles.
- `id: Int`: Auto-generated primary key.
- `name: String`: Profile name.
- `type: String`: "PERSONAL" or "BUSINESS".

#### TemplateEntity
Stores transaction templates for quick entry.
- `id: Long`: Auto-generated primary key.
- `name: String`: Template name.
- `category: String`: Category name.
- `amount: BigDecimal`: Default amount.
- `transactionType: Int`: 0 (Expense), 1 (Income), 2 (Transfer).
- `accountId: String?`, `currency: String?`: Default account and currency.
- `isStarred: Boolean`: Starred status for quick access.

#### CurrencyRateEntity
Stores exchange rates.
- `currencyCode: String`: Target currency (Primary Key).
- `baseCurrency: String`: Reference currency.
- `rate: Double`: Exchange rate.
- `timestamp: Long`: Last update time.

#### TransactionLinkEntity
Junction table for linking transactions (e.g., transfers).
- `id: Long`: Auto-generated primary key.
- `transactionId: Long`: Primary transaction ID.
- `linkedTransactionId: Long`: Associated transaction ID.

---

### DAOs (`data/local/dao/`)

#### AccountDao
- `getAllAccounts(profileId: Int): Flow<List<AccountEntity>>`: Returns all accounts for a profile.
- `getAccountById(id: String): AccountEntity?`: Fetches a single account.
- `upsertAccount(account: AccountEntity)`: Inserts or updates an account.
- `updateBalance(accountId: String, amount: BigDecimal)`: Adjusts account balance.
- `deleteAccount(account: AccountEntity)`: Deletes an account.

#### TransactionDao
- `getAllTransactions(profileId: Int): Flow<List<TransactionEntity>>`: Returns all transactions.
- `getFilteredTransactions(query: SupportSQLiteQuery): Flow<List<TransactionEntity>>`: Executes raw SQL query for filtering.
- `insertTransactionWithBalanceUpdate(transaction: TransactionEntity, accountDao: AccountDao)`: Inserts a transaction and updates the associated account balance within a Room transaction.
- `deleteTransactionWithBalanceUpdate(transaction: TransactionEntity, accountDao: AccountDao)`: Deletes a transaction and reverses the balance update.
- `updateTransactionWithBalanceUpdate(oldTransaction, newTransaction, accountDao)`: Handles transaction updates by reversing the old impact and applying the new one.
- `getCurrencySummaries(profileId: Int)`: Aggregates income/expense by currency.

#### CategoryDao, TagDao, BudgetDao, LoanDao, FilterDao, ProfileDao, CurrencyDao, TemplateDao, TransactionLinkDao
Standard CRUD operations for their respective entities.

---

### Repositories (`data/repository/`)

#### AccountRepositoryImpl
Implements `AccountRepository`.
- `getAllAccounts(profileId: Int): Flow<List<Account>>`: Maps `AccountEntity` to domain `Account`.
- `insertAccount(account: Account, profileId: Int)`: Converts domain model to entity and calls DAO.
- `updateBalance(accountId: String, amount: BigDecimal)`: Delegates to DAO.

#### TransactionRepositoryImpl
Implements `TransactionRepository`.
- `getFilteredTransactions(profileId, filter): Flow<List<Transaction>>`: 
  1. Builds dynamic SQL query using `SimpleSQLiteQuery`.
  2. Executes via DAO.
  3. Applies post-processing Regex filtering for notes and payees.
- `saveTransfer(sourceTransaction, targetTransaction, profileId)`: 
  1. Inserts both transactions.
  2. Creates bi-directional links in `TransactionLinkEntity` using `TransactionLinkDao`.
  3. All within a `database.withTransaction` block.

---

## 2. Domain Layer Documentation (`domain/`)

### Domain Models (`domain/model/`)

#### Account
- Fields: `id`, `name`, `balance`, `currencyCode`, `structureType`, `colorHex`, `icon`, `allowedCurrencies`, `isArchived`, `displayOrder`.

#### Transaction
- Fields: `id`, `title`, `amount`, `category`, `timestamp`, `currency`, `transactionType`, `accountId`, `categoryId`, `tags`, `paymentMethod`, `payee`, `note`, `recurring`, `latitude`, `longitude`, `warrantyExpiration`, `status`.

#### TransactionFilter
- Encapsulates all filtering criteria including `TimelineRange`, amount bounds, status, and regex-enabled fields.

#### TimelineRange
- Sealed class hierarchy: `AllTime`, `ThisPeriod`, `LastPeriod`, `CustomRange`.

---

### Services (`domain/service/`)

#### CurrencyService
- `convertAmount(amount, from, to, rates, precision)`: Converts amount between currencies using provided rates.
- `calculateTotalInCurrency(currencyTotals, target, rates, precision)`: Aggregates multiple currency amounts into a single target currency.

#### TimelineService
- `filterByTimelineRange(transactions, range)`: Filters a list of transactions based on the selected period.
- `getDateRange(range)`: Returns start and end timestamps for a given `TimelineRange`.

#### CardComputationService
- Computes render data for various cards (Cash Flow, Net Worth, Category Breakdown, etc.) based on `CardConfig` and transaction data.
- Handles scope-aware filtering (Global vs. Account-specific).

---

### Use Cases (`domain/usecase/`)

#### FinancialUseCases
- `calculateNetWorth(...)`: Aggregates balances across all accounts, converted to base currency.
- `calculateCashFlow(...)`: Computes total income and expenses for a period.
- `getAccountSummaries(...)`: Computes statistics per account.
- `getCurrencyAggregation(...)`: Totals balances grouped by currency.

#### TransactionUseCases
- `getFilteredTransactions(...)`: Bridges to `TransactionRepository`.
- `getRecentTransactions(...)`: Computes running balances and returns a limited list of transactions with their balances.
- `saveTransfer(...)`: Orchestrates transfer creation.

---

## 3. UI Layer Documentation (`ui/`)

### ViewModels (`ui/viewmodel/`)

#### AccountViewModel
- `accounts: StateFlow<List<Account>>`: Observed by UI for account lists.
- `addAccount(...)`: Creates an account and an initial balance transaction if amount > 0.
- `updateAccountsOrder(orderedAccounts)`: Persists reordered accounts.

#### TransactionViewModel
- `recordsFilter: StateFlow<TransactionFilter>`: Managed via `FilterManager`.
- `transactionsWithBalance: StateFlow<List<TransactionWithBalance>>`: The main data source for the transaction list, computed reactively based on filters, accounts, and exchange rates.
- `addTransaction(...)`, `updateTransaction(...)`: Handles CRUD via `TransactionUseCases`.
- `quickCloneTransaction(id)`: Duplicates a transaction with the current timestamp.
- `quickUpdateStatus(id, status)`: Fast status toggle.

#### AnalyticsViewModel
- Orchestrates `CardComputationService` to provide data for analytics cards.

---

### Screens (`ui/screens/`)

#### TransactionListScreen
- Displays transactions using `AeonTransactionItem`.
- Integrates `FilterManager` for real-time filtering.
- Supports swipe actions for quick clone, delete, and status updates.

#### TransactionEntryScreen
- Unified screen for creating and editing transactions and transfers.
- Integrates `CalculatorNumpad` for amount entry.
- Supports multi-link selection for relating transactions.

---

## 4. Data Flow Documentation

### Transaction Creation Flow
1. **UI**: User inputs data in `TransactionEntryScreen`.
2. **ViewModel**: `TransactionViewModel.addTransaction()` is called.
3. **UseCase**: `TransactionUseCases.insertTransaction()` validates and calls repository.
4. **Repository**: `TransactionRepositoryImpl.insertTransaction()` converts to entity.
5. **DAO**: `TransactionDao.insertTransactionWithBalanceUpdate()` executes within a DB transaction.
6. **Database**: Updates `transactions` table and `accounts` balance.
7. **Flow**: `TransactionViewModel.transactionsWithBalance` automatically re-emits updated data to UI.

### Transaction Filtering Flow
1. **UI**: User interacts with filter chips or `EditFilterScreen`.
2. **ViewModel**: `TransactionViewModel.updateRecordsFilter()` updates `FilterManager`.
3. **Flow**: `recordsFilter` emits new `TransactionFilter`.
4. **Repository**: `TransactionRepositoryImpl.getFilteredTransactions()` builds SQL and fetches data.
5. **Post-processing**: Repository applies Regex filters if enabled.
6. **UI**: `TransactionListContent` recomposes with new list.

---

## 5. Compute Logic Summary

### Net Worth Calculation
1. Filter transactions by `TimelineRange`.
2. Group transactions by account.
3. For each account:
   - Start with zero balance.
   - Sum transactions in the period (respecting their currencies).
4. Aggregate currency totals across all accounts.
5. Convert all totals to `baseCurrency` using `ExchangeRateService`.

### Running Balance Algorithm
1. Take all transactions for an account.
2. Sort by `timestamp` ascending.
3. Iterate and accumulate amount (converted to account currency if different).
4. `balanceAfter = runningTotal`, `balanceBefore = runningTotal - txAmount`.

### Currency Conversion Formula
`ConvertedAmount = OriginalAmount * (TargetRate / SourceRate)`
(Handled via `CurrencyService` using a base currency as pivot if necessary).
