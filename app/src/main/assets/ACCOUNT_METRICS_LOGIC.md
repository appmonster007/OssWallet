# Account Metrics Calculation Logic

This document explains the logic used to calculate the metrics in `account_metrics.json` from the seed data CSV files (`accounts.csv` and `transactions.csv`).

## Overview

The account metrics provide a comprehensive view of all accounts including balances, cash flow, category breakdowns, and multi-currency support. The metrics are calculated from the raw transaction data with currency conversion for multi-currency accounts.

## Data Sources

- **accounts.csv**: Contains account definitions with initial balances
- **transactions.csv**: Contains all transactions with amounts, categories, and currencies

## Calculation Logic

### 1. Account Balance Calculation

For each account, the final balance is calculated as:

```
finalBalance = initialBalance + netCashFlow
```

Where `netCashFlow` is the sum of all transaction amounts (income adds, expenses subtracts).

#### Single-Currency Accounts
For accounts that only support one currency (e.g., acc_cash, acc_credit, acc_invest):
- All transactions are in the account's base currency
- No conversion is needed
- Balance is calculated directly from transaction amounts

Example (acc_cash):
```
initialBalance: 1000.00 USD
expenses: 85.50 + 5.75 + 22.50 + 29.99 + 120.75 + 95.00 = 359.49 USD
finalBalance: 1000.00 - 359.49 = 640.51 USD
```

#### Multi-Currency Accounts
For accounts that support multiple currencies (e.g., acc_bank, acc_savings):
1. Calculate cash flow in each currency separately
2. Convert non-USD currencies to USD using exchange rates
3. Sum all converted amounts to get total USD cash flow
4. Calculate final balance from total USD cash flow

Example (acc_bank):
```
initialBalance: 5000.00 USD

USD Transactions:
  Income: 5000.00 + 850.00 + 2000.00 = 7850.00 USD
  Expenses: 45.00 + 1200.00 + 180.00 + 65.00 = 1490.00 USD
  USD Net: 7850.00 - 1490.00 = 6360.00 USD

INR Transactions:
  Income: 85000.00 INR
  Expenses: 1500.00 + 3500.00 + 1200.00 + 4500.00 = 10700.00 INR
  INR Net: 85000.00 - 10700.00 = 74300.00 INR
  Converted to USD: 74300.00 × 0.01205 = 894.58 USD

EUR Transactions:
  Income: 800.00 EUR
  Expenses: 120.00 + 45.00 + 15.00 + 5.50 = 185.50 EUR
  EUR Net: 800.00 - 185.50 = 614.50 EUR
  Converted to USD: 614.50 × 1.087 = 668.48 USD

Total USD Net: 6360.00 + 894.58 + 668.48 = 7923.06 USD
finalBalance: 5000.00 + 7923.06 = 12923.06 USD
```

### 2. Cash Flow Calculation

Cash flow is calculated per account and includes:

- **totalIncome**: Sum of all income transaction amounts
- **totalExpenses**: Sum of all expense transaction amounts
- **netCashFlow**: totalIncome - totalExpenses

For multi-currency accounts, the cash flow shown in the `cashFlow` object includes converted amounts from all currencies.

```
cashFlow.totalIncome = USD income + (INR income × INR_to_USD) + (EUR income × EUR_to_USD)
cashFlow.totalExpenses = USD expenses + (INR expenses × INR_to_USD) + (EUR expenses × EUR_to_USD)
cashFlow.netCashFlow = cashFlow.totalIncome - cashFlow.totalExpenses
```

### 3. Multi-Currency Cash Flow

The `multiCurrencyCashFlow` object provides a detailed breakdown per currency:

```json
"multiCurrencyCashFlow": {
  "USD": {
    "totalIncome": 7850.00,
    "totalExpenses": 1490.00,
    "netCashFlow": 6360.00
  },
  "INR": {
    "totalIncome": 85000.00,
    "totalExpenses": 10700.00,
    "netCashFlow": 74300.00,
    "convertedToUSD": 894.58
  },
  "EUR": {
    "totalIncome": 800.00,
    "totalExpenses": 185.50,
    "netCashFlow": 614.50,
    "convertedToUSD": 668.48
  }
}
```

Each currency breakdown shows:
- Raw amounts in that currency
- `convertedToUSD`: The net amount converted to USD (for non-USD currencies)

### 4. Category Breakdown

Categories are aggregated per account by summing all transaction amounts per category:

```
categoryBreakdown[].amount = sum of all transaction amounts in that category
categoryBreakdown[].transactionCount = number of transactions in that category
```

Note: Category amounts are shown in their original currency (not converted).

### 5. Global Currency Breakdown

The `globalCurrencyBreakdown` aggregates all transactions across all accounts by currency:

```
globalCurrencyBreakdown[currency].totalIncome = sum of all income in that currency
globalCurrencyBreakdown[currency].totalExpenses = sum of all expenses in that currency
globalCurrencyBreakdown[currency].netCashFlow = totalIncome - totalExpenses
globalCurrencyBreakdown[currency].accountCount = number of accounts with transactions in that currency
```

The USD breakdown includes converted amounts from other currencies to show the total USD-equivalent cash flow.

### 6. Summary Calculations

The summary aggregates data across all accounts:

```
totalAccounts = number of accounts
totalBalance = sum of all account final balances (in USD)
totalIncome = sum of all income across all accounts (in USD, with conversions)
totalExpenses = sum of all expenses across all accounts (in USD, with conversions)
netCashFlow = totalIncome - totalExpenses
activeAccounts = number of accounts with transactions
accountsWithTransactions = number of accounts with transactions
totalTransactions = total number of transactions
currenciesUsed = list of currencies present in transactions
multiCurrencyAccounts = number of accounts that support multiple currencies
```

## Exchange Rates

The following exchange rates are used for currency conversion:

- **INR to USD**: 0.01205
- **EUR to USD**: 1.087

These rates are approximate and used for testing purposes. In production, live exchange rates should be used.

## Calculation Order

The metrics are calculated in the following order:

1. Load accounts from `accounts.csv` to get initial balances
2. Load transactions from `transactions.csv`
3. Group transactions by account
4. For each account:
   - Calculate cash flow per currency
   - Convert non-USD currencies to USD
   - Calculate total USD cash flow
   - Calculate final balance
   - Aggregate by category
5. Calculate global currency breakdown
6. Calculate summary statistics

## Notes

- Investment accounts (like acc_invest) may have no transactions but can have a balance set in the CSV
- Credit card accounts show negative balances for expenses
- Multi-currency accounts always calculate their final balance in USD for consistency
- The `convertedToUSD` values in multi-currency breakdowns are for reference only
- Category breakdown amounts are in original currency, not converted

## Example: Complete Calculation for acc_bank

```
Initial Balance: 5000.00 USD

USD Transactions:
  Income: 5000.00 (Salary) + 850.00 (Freelance) + 2000.00 (Bonus) = 7850.00
  Expenses: 45.00 (Fuel) + 1200.00 (Rent) + 180.00 (Utilities) + 65.00 (Phone) = 1490.00
  Net: 6360.00

INR Transactions:
  Income: 85000.00 (Salary in INR)
  Expenses: 1500.00 (Dining) + 3500.00 (Groceries) + 1200.00 (Phone) + 4500.00 (Electric) = 10700.00
  Net: 74300.00 INR
  Converted: 74300.00 × 0.01205 = 894.58 USD

EUR Transactions:
  Income: 800.00 (Freelance EUR)
  Expenses: 120.00 (Hotel) + 45.00 (Train) + 15.00 (Museum) + 5.50 (Coffee) = 185.50
  Net: 614.50 EUR
  Converted: 614.50 × 1.087 = 668.48 USD

Total USD Net: 6360.00 + 894.58 + 668.48 = 7923.06
Final Balance: 5000.00 + 7923.06 = 12923.06 USD

Cash Flow (USD):
  totalIncome: 7850.00 + 894.58 + 668.48 = 9743.85
  totalExpenses: 1490.00 + 128.94 + 201.63 = 1820.79
  netCashFlow: 9743.85 - 1820.79 = 7923.06
```

## Usage

This metrics file is used for:
- Testing dashboard cards and analytics without querying the database
- Verifying seed data accuracy
- Providing sample data for UI development
- Validating multi-currency calculations
