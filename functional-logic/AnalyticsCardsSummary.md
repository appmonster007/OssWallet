# Analytics Cards Functional Logic Summary

## Overview
This document provides functional logic summaries for all 14 analytics cards. For detailed documentation of each card, see individual card documentation files.

## Analytics Cards List

### 1. TimelineTrajectoryCard
- **Purpose**: Line chart showing spending/income over time
- **Data**: TimelineTrajectoryData (date, amount, type)
- **Navigation**: onRecordsClick → TransactionListScreen with date filter
- **Configuration**: Filter (accounts, categories, tags, date range)
- **Visualization**: Line chart with date on x-axis, amount on y-axis

### 2. CategoryExpenseTreemapCard
- **Purpose**: Treemap visualization of category expenses
- **Data**: CategoryExpenseTreemapData (category, amount, color)
- **Navigation**: onRecordsClick → TransactionListScreen with category filter
- **Configuration**: Filter (accounts, categories, tags, date range)
- **Visualization**: Treemap with rectangle size proportional to amount

### 3. IncomeExpensesSummaryCard
- **Purpose**: Summary cards showing total income/expense
- **Data**: IncomeExpensesSummaryData (totalIncome, totalExpense, net)
- **Navigation**: onRecordsClick → TransactionListScreen
- **Configuration**: Filter (accounts, categories, tags, date range)
- **Visualization**: Card-based summary with totals

### 4. PeriodComparisonCard
- **Purpose**: Compare current period with previous period
- **Data**: PeriodComparisonData (currentPeriod, previousPeriod, change, changePercent)
- **Navigation**: onRecordsClick → TransactionListScreen
- **Configuration**: Filter (accounts, categories, tags, date range)
- **Visualization**: Comparison metrics with change indicators

### 5. CashFlowTrendCard
- **Purpose**: Trend line showing cash flow over time
- **Data**: CashFlowTrendData (date, income, expense, net)
- **Navigation**: onRecordsClick → TransactionListScreen
- **Configuration**: Filter (accounts, categories, tags, date range)
- **Visualization**: Multi-line chart (income, expense, net)

### 6. CashFlowProgressCard
- **Purpose**: Progress bars showing cash flow metrics
- **Data**: CashFlowProgressData (income, expense, savingsRate, etc.)
- **Navigation**: onRecordsClick → TransactionListScreen
- **Configuration**: Filter (accounts, categories, tags, date range)
- **Visualization**: Progress bars with labels

### 7. BalanceForecastCard
- **Purpose**: Forecast future balance based on trends
- **Data**: BalanceForecastData (date, forecastedBalance, confidence)
- **Navigation**: onRecordsClick → TransactionListScreen
- **Configuration**: Filter (accounts, categories, tags, date range)
- **Visualization**: Line chart with forecast projection

### 8. PlannedPaymentsCard
- **Purpose**: Display planned/recurring payments
- **Data**: PlannedPaymentsData (payment, amount, date, status)
- **Navigation**: onRecordsClick → TransactionListScreen
- **Configuration**: Filter (accounts, categories, tags, date range)
- **Visualization**: List of planned payments

### 9. IncomeExpensesBookCard
- **Purpose**: Book-style income/expense display
- **Data**: IncomeExpensesBookData (incomeEntries, expenseEntries)
- **Navigation**: onRecordsClick → TransactionListScreen
- **Configuration**: Filter (accounts, categories, tags, date range)
- **Visualization**: Book-style ledger display

### 10. CashFlowTableCard
- **Purpose**: Table view of cash flow by period
- **Data**: CashFlowTableData (period, income, expense, net)
- **Navigation**: onRecordsClick → TransactionListScreen
- **Configuration**: Filter (accounts, categories, tags, date range)
- **Visualization**: Table with period columns

### 11. PlannedPaymentsTimelineCard
- **Purpose**: Timeline view of planned payments
- **Data**: PlannedPaymentsTimelineData (payment, date, amount)
- **Navigation**: onRecordsClick → TransactionListScreen
- **Configuration**: Filter (accounts, categories, tags, date range)
- **Visualization**: Timeline visualization

### 12. BalanceByAccountsCard
- **Purpose**: Bar chart showing balance by account
- **Data**: BalanceByAccountsData (account, balance, currency)
- **Navigation**: onRecordsClick → TransactionListScreen
- **Configuration**: Filter (accounts, categories, tags, date range)
- **Visualization**: Bar chart with account labels

### 13. BalanceTrendCard
- **Purpose**: Line chart showing balance trend over time
- **Data**: BalanceTrendData (date, balance)
- **Navigation**: onRecordsClick → TransactionListScreen
- **Configuration**: Filter (accounts, categories, tags, date range)
- **Visualization**: Line chart with balance trend

### 14. CategoryBreakdownCard
- **Purpose**: Pie chart showing category spending with drill-down
- **Data**: CategoryBreakdownData (category, amount, subcategories)
- **Navigation**: onRecordsClick → TransactionListScreen, onDrillInClick → In-card drill-down
- **Configuration**: Filter, showLegend
- **Visualization**: Pie chart with drill-down capability
- **Special Features**: 
  - Breadcrumb navigation
  - Drill-down to subcategories
  - "Other" category for items beyond top 10

## Common Patterns

### Configuration
All analytics cards share:
- **Filter**: Accounts, categories, tags, date range
- **onUpdate**: Callback to update configuration
- **onDelete**: Callback to delete card
- **onRecordsClick**: Callback to navigate to TransactionListScreen

### Data Flow
```
Parent Screen → Card Component → onRecordsClick(filter) → TransactionListScreen
```

### Settings Sheet
All analytics cards use `AeonCardSettingsSheet` for:
- Filter configuration
- Account selection
- Category selection
- Tag selection
- Saved filter selection

### Computation
All analytics card data is computed by:
- **CardComputationService**: Domain service for card data computation
- **AnalyticsViewModel**: Manages card computation and caching
- **Filter Application**: Data filtered by card configuration

## Visualization Components

### Charts
- **AeonPieChart**: Used by CategoryBreakdownCard
- **AeonLineChart**: Used by TimelineTrajectoryCard, BalanceTrendCard, BalanceForecastCard
- **AeonBarChart**: Used by BalanceByAccountsCard
- **AeonTreemap**: Used by CategoryExpenseTreemapCard

### Tables
- **AeonTable**: Used by CashFlowTableCard

### Progress Indicators
- **AeonProgressBar**: Used by CashFlowProgressCard

### Timelines
- **AeonTimeline**: Used by PlannedPaymentsTimelineCard

## Navigation Context

All analytics cards navigate to TransactionListScreen with `RecordsFilterContext.Custom`:
```kotlin
RecordsFilterContext.Custom(
    accountIds = config.filter.accountIds,
    categoryIds = config.filter.categoryIds,
    tagIds = config.filter.tagIds,
    startDate = config.filter.startDate,
    endDate = config.filter.endDate
)
```

## Dependencies

### Common Dependencies
- **CardComputationService**: Computes card data
- **AnalyticsViewModel**: Manages card state
- **SettingsDataStore**: Persists card configuration
- **AeonCardSettingsSheet**: Unified settings UI
- **Filter Components**: Account, category, tag selection

### Visualization Dependencies
- **AeonPieChart**: Pie chart component
- **AeonLineChart**: Line chart component
- **AeonBarChart**: Bar chart component
- **AeonTreemap**: Treemap component
