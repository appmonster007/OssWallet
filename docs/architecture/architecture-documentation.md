# OssWallet Architecture Documentation

## Overview

OssWallet is an Android personal finance application built with Kotlin and Jetpack Compose, following MVVM architecture with Clean Architecture principles.

## Architecture Layers

### 1. Data Layer (`data/`)

#### Entities (`data/local/entity/`)
- **AccountEntity**: Stores account information (id, name, balance, currency, icon, color, structureType, isArchived)
- **TransactionEntity**: Stores transaction records (id, type, amount, accountId, categoryId, tags, date, note, payee, location, paymentMethod, linkedTransactionId, transferAccountId)
- **CategoryEntity**: Stores transaction categories (id, name, iconName, colorHex, parentId for hierarchy)
- **TagEntity**: Stores transaction labels/tags (id, name, colorHex)
- **TemplateEntity**: Stores transaction templates (id, name, transactionType, amount, categoryId, accountId, currency, tags, note, payee, location, paymentMethod, isStarred)
- **BudgetEntity**: Stores budget goals (id, name, targetAmount, categoryId, periodType, startDate, endDate)
- **LoanEntity**: Stores loans and debts (id, name, type, principalAmount, interestRate, startDate, dueDate, isPaidOff)
- **FilterEntity**: Stores saved transaction filters (id, name, filter criteria as JSON)
- **ProfileEntity**: Stores user profile settings (id, name, baseCurrency, theme, decimalPrecision)

#### DAOs (`data/local/dao/`)
- **AccountDao**: CRUD operations for accounts, including queries by archive status
- **TransactionDao**: CRUD operations for transactions, filtering by date, account, category, tags
- **CategoryDao**: CRUD operations for categories, parent-child hierarchy queries
- **TagDao**: CRUD operations for tags
- **TemplateDao**: CRUD operations for templates, starred templates query
- **BudgetDao**: CRUD operations for budgets
- **LoanDao**: CRUD operations for loans
- **FilterDao**: CRUD operations for filters
- **ProfileDao**: CRUD operations for profile settings

#### Repositories (`data/repository/`)
- **AccountRepository**: Manages account data, handles currency conversions, archive operations
- **TransactionRepository**: Manages transaction data, filtering, linked transactions, transfers
- **CategoryRepository**: Manages category hierarchy, color/icon management
- **TagRepository**: Manages tag operations
- **TemplateRepository**: Manages templates, starred templates
- **BudgetRepository**: Manages budget tracking, progress calculation
- **LoanRepository**: Manages loan/debt tracking
- **FilterRepository**: Manages saved filters
- **SettingsRepository**: Manages app settings, currency preferences, theme

### 2. Domain Layer (`domain/`)

#### Models (`domain/model/`)
- **Account**: Domain model for accounts
- **Transaction**: Domain model for transactions with CashFlowType enum (INCOME, EXPENSE, TRANSFER)
- **Category**: Domain model for categories
- **Tag**: Domain model for tags
- **Template**: Domain model for templates
- **Budget**: Domain model for budgets
- **Loan**: Domain model for loans
- **TransactionFilter**: Domain model for filter criteria
- **TimelineRange**: Sealed class for time filtering (AllTime, ThisPeriod, LastPeriod, CustomRange)
- **CardConfig hierarchy**: Configuration classes for dashboard/analytics cards

#### Services (`domain/service/`)
- **CardComputationEngine**: Interface for card data computation
- **CardComputationService**: Implementation of computation engine using use cases
- **ExchangeRateService**: Manages currency exchange rates, auto-fetching
- **CurrencyConverter**: Handles currency conversion logic

#### Use Cases (`domain/usecase/`)
- **GetAccountsUseCase**: Fetches accounts with optional filtering
- **GetTransactionsUseCase**: Fetches transactions with filtering
- **GetCategoriesUseCase**: Fetches category hierarchy
- **ComputeNetWorthUseCase**: Calculates net worth across accounts
- **ComputeCashFlowUseCase**: Calculates income/expense totals
- **ComputeCurrencyTotalsUseCase**: Aggregates balances by currency
- **ComputeAccountStatsUseCase**: Calculates account statistics
- **CategoryBreakdownUseCase**: Computes category spending analysis
- **TimelineTrajectoryUseCase**: Computes spending over time
- **BalanceForecastUseCase**: Projects future balances

### 3. UI Layer (`ui/`)

#### ViewModels (`ui/viewmodel/`)
- **AccountViewModel**: Manages account-related UI state
- **TransactionViewModel**: Manages transaction operations, filtering
- **CategoryViewModel**: Manages category hierarchy
- **TagViewModel**: Manages tags
- **TemplateViewModel**: Manages templates
- **BudgetViewModel**: Manages budget tracking
- **LoanViewModel**: Manages loans
- **SettingsViewModel**: Manages app settings
- **CurrencyViewModel**: Manages exchange rates
- **AnalyticsViewModel**: Manages analytics computations
- **AccountsViewViewModel**: Manages accounts view card configurations
- **CardManagerViewModel**: Manages dashboard card configurations

#### Screens (`ui/screens/`)
- **AccountManagementScreen**: Account list with reorder, archive, multi-delete
- **AccountSetupScreen**: New account creation form
- **AccountsViewScreen**: Dashboard with reorderable cards
- **AddTemplateScreen**: Template creation/editing
- **AnalyticsScreen**: Analytics cards management
- **BudgetsScreen**: Budget list with progress
- **CategoryManagementScreen**: Category hierarchy management
- **EditAccountScreen**: Account editing
- **EditBudgetScreen**: Budget editing
- **EditFilterScreen**: Filter creation/editing
- **ExchangeRatesScreen**: Exchange rates display and converter
- **FiltersSettingsScreen**: Saved filters management
- **LoansScreen**: Loan/debt tracking
- **ManageLabelsScreen**: Tag management
- **OnboardingScreen**: Initial app setup
- **SettingsScreen**: App settings
- **SharedWalletScreen**: Shared wallet management
- **TemplatesScreen**: Template list with reorder
- **TransactionEntryScreen**: Transaction creation/editing
- **TransactionListScreen**: Transaction list with filtering
- **TransactionListContent**: Transaction list component

#### Components (`ui/components/`)

**Core Components:**
- **AeonButtons.kt**: Dashed button, FAB, extended FAB
- **AeonCards.kt**: Category item component
- **AeonDateTimePicker.kt**: Date/time picker with two-step flow
- **AeonInputs.kt**: Record field card, editorial input, currency selector
- **AeonLayouts.kt**: Section header, detail drawer, shimmer loading
- **AeonLists.kt**: Category list with hierarchy
- **AeonPickers.kt**: Account, label, payment method, category picker sheets
- **AeonProgress.kt**: Budget progress bar
- **AeonTimelineFilter.kt**: Timeline filter bar with date range picker
- **AppDrawer.kt**: Navigation drawer with accounts and templates

**Specialized Components:**
- **AccountFormComponents.kt**: Currency management section with grid
- **AccountIcons.kt**: Icon map for accounts/categories (100+ icons)
- **AeonExpandableFab.kt**: Expandable FAB with template shortcuts
- **AeonFilterComponents.kt**: Segmented control, dropdown picker
- **AeonFlowComponents.kt**: Settings toggles, layout toggles, section headers, badges
- **AeonFormComponents.kt**: Hero header, form header, color picker, visual identity section
- **CalculatorDialog.kt**: Simple calculator dialog
- **CalculatorNumpad.kt**: Advanced calculator with expression parsing
- **CardManagementComponents.kt**: Card type option, generic add card dialog
- **CardReorderableList.kt**: Advanced reorderable list for cards
- **CategoryFormComponents.kt**: Category edit sheet with hierarchy
- **CategoryMultiSelectPicker.kt**: Multi-select category picker
- **NavigationPill.kt**: Navigation pill for tab selection
- **OCRScanner.kt**: OCR for receipt scanning
- **ReorderableList.kt**: Standard reorderable list component

**Account Components (`ui/components/accounts/`):**
- **AeonAccountCards.kt**: Small, medium, large account cards, account picker sheet

**Card Components (`ui/components/cards/`):**
- **CardCallbacks.kt**: Sealed class hierarchy for card callbacks
- **CardCompute.kt**: Extension functions for card data computation
- **CardFilter.kt**: Extension functions for card data filtering
- **CardRenderData.kt**: Sealed class hierarchy for card render data
- **AeonCardSettingsSheet.kt**: Unified card settings sheet
- **UnifiedCardRenderer.kt**: Unified renderer for all card types

## Data Flow Diagrams

### Transaction Creation Flow

```
User Input (TransactionEntryScreen)
    ↓
ViewModel (TransactionViewModel)
    ↓
Repository (TransactionRepository)
    ↓
DAO (TransactionDao)
    ↓
Database (Room)
    ↓
UI Updates (collectAsStateWithLifecycle)
```

### Account Balance Calculation Flow

```
AccountViewModel
    ↓
GetAccountsUseCase
    ↓
AccountRepository
    ↓
AccountDao.getAccounts()
    ↓
TransactionDao.getTransactionsByAccount()
    ↓
Balance Calculation (Repository)
    ↓
UI State Update
```

### Analytics Computation Flow

```
AnalyticsViewModel
    ↓
CardComputationService
    ↓
Use Cases (ComputeCashFlow, ComputeNetWorth, etc.)
    ↓
Repositories (TransactionRepository, AccountRepository)
    ↓
DAOs
    ↓
Data Aggregation & Computation
    ↓
CardRenderData
    ↓
UnifiedCardRenderer
    ↓
UI Display
```

### Currency Conversion Flow

```
SettingsViewModel / TransactionViewModel
    ↓
ExchangeRateService
    ↓
Fetch Exchange Rates (API)
    ↓
Cache in Database
    ↓
CurrencyConverter.convert()
    ↓
Display Converted Amount
```

### Filter Application Flow

```
User Selects Filter (TransactionListScreen)
    ↓
TransactionViewModel.updateFilter()
    ↓
TransactionRepository.getFilteredTransactions()
    ↓
DAO Queries with WHERE clauses
    ↓
Filtered Results
    ↓
UI Update
```

## Compute Logic Algorithms

### Net Worth Calculation

```kotlin
// ComputeNetWorthUseCase
suspend fun execute(accounts: List<Account>, transactions: List<Transaction>): BigDecimal {
    val accountBalances = accounts.associate { it.id to it.balance }
    val accountCurrencies = accounts.associate { it.id to it.currencyCode }
    
    var totalNetWorth = BigDecimal.ZERO
    
    accounts.forEach { account ->
        val balance = account.balance
        val convertedBalance = if (account.currencyCode != baseCurrency) {
            exchangeRateService.convert(balance, account.currencyCode, baseCurrency)
        } else {
            balance
        }
        totalNetWorth += convertedBalance
    }
    
    return totalNetWorth
}
```

### Cash Flow Calculation

```kotlin
// ComputeCashFlowUseCase
suspend fun execute(transactions: List<Transaction>): CashFlowResult {
    val income = transactions
        .filter { it.type == CashFlowType.INCOME }
        .sumOf { convertToBaseCurrency(it.amount, it.currencyCode) }
    
    val expense = transactions
        .filter { it.type == CashFlowType.EXPENSE }
        .sumOf { convertToBaseCurrency(it.amount, it.currencyCode) }
    
    val net = income - expense
    
    return CashFlowResult(income, expense, net)
}
```

### Category Breakdown Calculation

```kotlin
// CategoryBreakdownUseCase
suspend fun execute(transactions: List<Transaction>): List<CategoryBreakdownData> {
    return transactions
        .filter { it.type == CashFlowType.EXPENSE }
        .groupBy { it.categoryId }
        .map { (categoryId, txs) ->
            val total = txs.sumOf { convertToBaseCurrency(it.amount, it.currencyCode) }
            CategoryBreakdownData(
                categoryId = categoryId,
                categoryName = getCategoryName(categoryId),
                amount = total,
                percentage = (total / totalExpense * 100).toFloat()
            )
        }
        .sortedByDescending { it.amount }
}
```

### Budget Progress Calculation

```kotlin
// BudgetProgressCalculation in BudgetRepository
fun calculateBudgetProgress(budget: Budget, transactions: List<Transaction>): BudgetProgress {
    val budgetTransactions = transactions.filter { tx ->
        tx.categoryId == budget.categoryId &&
        tx.date in budget.startDate..budget.endDate &&
        tx.type == CashFlowType.EXPENSE
    }
    
    val spent = budgetTransactions.sumOf { it.amount }
    val remaining = budget.targetAmount - spent
    val percentage = (spent / budget.targetAmount * 100).toFloat().coerceIn(0f, 100f)
    
    return BudgetProgress(spent, remaining, percentage)
}
```

### Currency Conversion Algorithm

```kotlin
// CurrencyConverter
fun convert(amount: BigDecimal, fromCurrency: String, toCurrency: String): BigDecimal {
    if (fromCurrency == toCurrency) return amount
    
    val rate = exchangeRateService.getRate(fromCurrency, toCurrency)
        ?: throw IllegalArgumentException("Exchange rate not found")
    
    return amount * rate
}
```

### Timeline Trajectory Calculation

```kotlin
// TimelineTrajectoryUseCase
suspend fun execute(transactions: List<Transaction>, range: TimelineRange): List<TimelineTrajectoryData> {
    val filteredTransactions = filterByTimeline(transactions, range)
    val groupedByDate = filteredTransactions
        .groupBy { formatDate(it.date) }
        .toSortedMap()
    
    return groupedByDate.map { (date, txs) ->
        val income = txs.filter { it.type == CashFlowType.INCOME }
            .sumOf { convertToBaseCurrency(it.amount, it.currencyCode) }
        val expense = txs.filter { it.type == CashFlowType.EXPENSE }
            .sumOf { convertToBaseCurrency(it.amount, it.currencyCode) }
        
        TimelineTrajectoryData(date, income, expense)
    }
}
```

## Key Design Patterns

### 1. MVVM Architecture
- ViewModels manage UI state and business logic
- Views observe state via `collectAsStateWithLifecycle()`
- Clear separation between UI and data layers

### 2. Repository Pattern
- Repositories abstract data sources (Room database, APIs)
- Provide clean APIs for ViewModels
- Handle data transformation and caching

### 3. Use Case Pattern
- Use cases encapsulate single business operations
- Promote reusability and testability
- Clear separation of concerns

### 4. Sealed Classes for Type Safety
- `CardRenderData`: Type-safe card data hierarchy
- `CardCallbacks`: Type-safe callback hierarchy
- `TimelineRange`: Type-safe time filtering options

### 5. Composable Components
- Reusable UI components in `ui/components/`
- Parameterized for flexibility
- Consistent design system (Aeon components)

### 6. State Management
- ViewModels use `StateFlow` for reactive state
- Compose state with `remember` and `mutableStateOf`
- Lifecycle-aware state collection

## Card Architecture

### Card Configuration System

The app uses a unified card system that works across multiple screens:

1. **CardConfig**: Sealed class hierarchy defining card configuration
2. **CardRenderData**: Sealed class hierarchy defining render data
3. **CardCallbacks**: Sealed class hierarchy defining card callbacks
4. **CardCompute**: Extension functions for data computation
5. **CardFilter**: Extension functions for data filtering
6. **UnifiedCardRenderer**: Single renderer for all card types

### Supported Card Types

**Dashboard Cards:**
- NetWorthCard
- CurrencyBalancesCard
- CashFlowCard
- AccountsCard
- BudgetsCard

**Analytics Cards:**
- CategoryBreakdownCard
- TimelineTrajectoryCard
- CategoryExpenseTreemapCard
- IncomeExpensesSummaryCard
- PeriodComparisonCard
- CashFlowTrendCard
- CashFlowProgressCard
- BalanceForecastCard
- PlannedPaymentsCard

**Account Detail Cards:**
- AccountHeroCard

## Navigation Architecture

### Navigation Structure

- **Dashboard**: Main entry point with cards
- **AccountsView**: Account management dashboard
- **Records**: Transaction list with filtering
- **Analytics**: Analytics cards management
- **Budgets**: Budget tracking
- **Loans**: Loan/debt management
- **Settings**: App configuration

### Filter Context Navigation

Transactions can be filtered by context:
- Account-specific transactions
- Category-specific transactions
- Budget-specific transactions
- Saved filter presets
- Custom date ranges

## Theme System

### Color System

- Dynamic color support via Material 3
- Custom accent colors per account/category
- Dark/Light theme switching
- AMOLED black theme option

### Typography

- Material 3 typography scale
- Custom font weights for emphasis
- Consistent spacing and sizing

## Performance Optimizations

### Database
- Room with proper indexing
- Efficient queries with joins
- Pagination for large datasets

### UI
- LazyColumn for efficient scrolling
- Compose recomposition optimization
- Image loading with Coil

### Computation
- Suspending functions for async operations
- Caching of computed results
- Efficient data aggregation

## Security Considerations

- Data stored locally on device
- No external API authentication required
- Exchange rates fetched from public APIs
- Backup/restore functionality for data portability

## Future Enhancement Opportunities

1. Cloud sync for multi-device support
2. Biometric authentication for app lock
3. Advanced analytics with ML predictions
4. Budget alerts and notifications
5. Receipt attachment support
6. Recurring transaction automation
7. Multi-currency account support
8. Investment tracking
