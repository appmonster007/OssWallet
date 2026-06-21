# Review & Implementation Plan: Accounting vs. Cash Flow Perspectives

This document outlines the architectural shift from a hardcoded transaction filter to a dual-perspective system (**Accounting** vs. **Cash Flow**) across all OssWallet analytics cards.

---

## 1. Core Logic: Defining the Two Perspectives

To standardize the behavior across all 15 cards, we define the logic for transaction inclusion as follows:

| Perspective | Primary Focus | Included Transaction Types | Excluded Types |
| :--- | :--- | :--- | :--- |
| **Accounting** | Net Wealth Change | Income, Expense | All Transfers (Inflow, Outflow, Internal) |
| **Cash Flow** | Liquidity Movement | Income, Expense, Transfer Inflow, Transfer Outflow | Internal Transfers (Same owner, different account) |

**Note on Transfers:** 
*   **Transfer Inflow/Outflow** are treated as "External" (money entering or leaving the system/wallet).
*   **Transfer (Internal)** is ignored in both modes as it is a zero-sum movement between tracked accounts.

---

## 2. Perspective Matrix & Card Review

Review of the analytics cards (excluding Planning/Tracking, Recent Activities, and Currency Balances).

### A. Primary Analytics Suite
| Card | Supported Modes | Why? | Pros/Cons |
| :--- | :--- | :--- | :--- |
| **Income & Expense** | **Both** | **Accounting:** Classic "How much did I earn vs. spend". <br>**Cash Flow:** "How much cash actually hit my accounts". | **Pro:** Identifies gap between earnings and actual liquidity. <br>**Con:** Cash Flow mode might be skewed by large one-time external transfers. |
| **Category Spending** | **Accounting** | Focuses on *where* money goes. Transfers (even In/Out) usually lack granular categories. | **Pro:** Clean expense reports. <br>**Con:** Excluding Transfer Outflows might hide "Investment" movements if categorized as transfers. |
| **Cashflow Comparison**| **Cash Flow** | By definition, this compares flows over time. | **Pro:** Essential for runway and burn-rate calculations. |
| **Category Treemap** | **Both** | **Accounting:** Spending hierarchy. <br>**Cash Flow:** Shows the scale of "External Transfers" relative to spending. | **Pro:** Visualizes if "Investing" (Transfer Out) is larger than "Dining Out" (Expense). |
| **Category Piechart** | **Both** | Same as Treemap. | **Pro:** Quick percentage breakdown of all outflows. |
| **Tag UpSet Plot** | **Both** | Users often tag transfers (e.g., #Taxable, #Business). | **Pro:** Allows cross-sectional analysis of tags across both income and external transfers. |

### B. Summary & Context Cards
| Card | Supported Modes | Implementation Detail |
| :--- | :--- | :--- |
| **Net Worth** | **Accounting** | Inherently tracks wealth. Cash movement (transfers) does not impact the bottom line. |
| **Account Hero** | **Cash Flow** | Displays the activity of a specific account; external transfers are vital here. |
| **Budgets** | **Accounting** | Budgets are almost always based on Income/Expense categories. |
| **Loans/Debt** | **Cash Flow** | Loan repayments are often transfers to a debt account (Inflow/Outflow). |

---

## 3. Standardization Strategy

### A. Dynamic Label Dictionary
Instead of hardcoding "Income" or "Expense" in the UI, we use a dictionary based on the active perspective:

data class PerspectiveLabels(
    val incomeLabel: String,    // "Income" (Accounting) vs "Inflow" (Cash Flow)
    val expenseLabel: String,   // "Expense" (Accounting) vs "Outflow" (Cash Flow)
    val netLabel: String,       // "Savings" (Accounting) vs "Net Flow" (Cash Flow)
    val transferLabel: String = "Transfer"
)

### B. The `AccountingPerspective` Enum
Replace the boolean `excludeSameCurrencyTransfers` or `displayMode` where applicable with a unified perspective:

enum class AccountingPerspective {
    ACCOUNTING, // Income/Expense only
    CASH_FLOW   // Income/Expense + External Transfers
}

---

## 4. Implementation Checklist (10 Target Cards)

1.  **Model Updates:** 
    *   Add `val perspective: AccountingPerspective` to all `CardConfig` data classes.
    *   Remove legacy `val displayMode: CashFlowDisplayMode` if it strictly duplicated perspective logic.
2.  **Repository/Use-Case Refactor:**
    *   Remove hardcoded `!tx.transactionType.isTransfer` filters.
    *   Implement logic: `if (perspective == ACCOUNTING) filter { !it.isTransfer } else filter { it.isExternal }`.
3.  **UI/ViewModel Updates:**
    *   Standardize the toggle UI (Segmented Button: Accounting vs. Cash Flow).
    *   Map the `PerspectiveLabels` to the chart legends and headers.
4.  **`CardUpdateHandler` Extension:**
    *   Add `updateCardPerspective(cardId, perspective) ` to allow seamless switching from any screen.

---

## 5. Summary of Transfer Logic
*   **Accounting Mode:** Filters by `TransactionType.INCOME` and `TransactionType.EXPENSE`.
*   **Cash Flow Mode:** Includes above + `TransactionType.TRANSFER` where the Category is specifically `Transfer Inflow` or `Transfer Outflow`.
*   **Standardization:** Both modes exclude "Internal Transfers" (moving money between two accounts you own within the app).
