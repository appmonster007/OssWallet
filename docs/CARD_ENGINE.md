# Card Computing Engine — OssWallet

The Dashboard in OssWallet is powered by a dynamic, strategy-based computing engine. This allows for extensible "widgets" (Cards) that compute financial metrics in the background.

## 1. Core Concepts

### 1.1 CardInput
Every computer receives a `CardInput` object. This is a massive "Snapshot" of the current state, including:
- All transactions, accounts, and categories.
- Exchange rates for the current base currency.
- Active profile metadata (Budgets, Loans, Planned Payments).
- Pre-built `CategoryHierarchyCache` to avoid redundant tree traversals.

### 1.2 CardComputerRegistry
This registry acts as the "Strategy Factory". It maps a `CardType` (e.g., `NET_WORTH`, `CASHFLOW`) to its specific `CardComputer` implementation.

---

## 2. Card Types & Logic

| Card Type | Logic Description |
| :--- | :--- |
| `NET_WORTH` | Sums all account balances converted to base currency. Computes historical data points for line charts. |
| `CASHFLOW` | Groups transactions by month/day. Calculates Income vs. Expense ratios. |
| `BUDGET_PROGRESS` | Maps transactions to active budgets and calculates percentage of spent vs. limit. |
| `LOAN_SUMMARY` | Calculates remaining principal by subtracting payments from the initial loan amount. |
| `CURRENCY_DISTRIBUTION` | Aggregates balances by currency and calculates their weight in the total portfolio. |

---

## 3. Performance & Optimization

### 3.1 Parallel Execution
When the dashboard loads, the `DashboardViewModel` triggers all card computations in parallel using `Dispatchers.Default`. This prevents the UI from stuttering during heavy math operations.

### 3.2 Result Caching (`CardRenderCache`)
Computed `CardRenderData` is cached in memory.
- **Key**: `(profileId, baseCurrency)`.
- **Invalidation**: The cache is cleared on profile switch or whenever a transaction write occurs (via `WidgetInvalidationObserver`).

---

## 4. Implementation Pattern

To add a new card:
1.  Define a new `CardType` enum.
2.  Implement the `CardComputer` interface.
3.  Register the implementation in `CardComputerRegistry`.
4.  Define the UI representation in `DashboardScreen.kt` using a `when` branch on the rendered data.
