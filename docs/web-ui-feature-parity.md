# Web UI Feature Parity Tracker

Tracks the gap between the Android app and the Ktor-served web UI (`app/src/main/assets/web/index.html`).

Legend: ✅ Done · 🔲 Missing · ⚠️ Partial

---

## Transaction Form Fields

| Field | Android | Web |
|---|---|---|
| Title | ✅ | ✅ |
| Amount + Currency | ✅ | ✅ |
| Type (Income / Expense / Transfer) | ✅ | ✅ |
| Account | ✅ | ✅ |
| Target Account (transfers) | ✅ | ⚠️ shown but target currency amount not displayed |
| Category | ✅ | ✅ |
| Date / Time | ✅ | ✅ |
| Payee | ✅ | ✅ |
| Note | ✅ | ✅ |
| Tags | ✅ | ✅ |
| Status (Cleared / Uncleared / Reconcile) | ✅ | ✅ |
| Payment Method | ✅ | ✅ dropdown on create/edit form |
| Warranty toggle + expiration date | ✅ | ✅ toggle + date picker on create/edit form |
| GPS Location + location name | ✅ | 🔲 |
| Linked transactions | ✅ | 🔲 |
| Template application (prefill) | ✅ | ✅ via Templates page → Use button |
| Calculator numpad for amount | ✅ | ⚠️ standard `<input>` only |
| Note / Payee autocomplete | ✅ | 🔲 |

---

## Transactions / Records Page

| Feature | Android | Web |
|---|---|---|
| Transaction list | ✅ | ✅ |
| Grouping (by date / category / account) | ✅ | 🔲 |
| Sorting controls | ✅ | 🔲 |
| Search | ✅ | ✅ via filter |
| Transaction detail view | ✅ | ✅ modal with all fields, edit/delete actions |
| Edit transaction | ✅ full form | ✅ full modal with all fields incl. payment method + warranty |
| Delete transaction | ✅ | ✅ |
| Clone transaction | ✅ | ✅ |
| Split transaction | ✅ | 🔲 needs backend schema |
| Linked transactions | ✅ | 🔲 |
| Warranty tracking | ✅ | ✅ shown in detail view and list (🛡️ icon) |
| Location capture / display | ✅ | 🔲 |

---

## Dashboard Cards

| Card | Rendered | Filters | Drill-down / Actions |
|---|---|---|---|
| Net Worth | ✅ | ✅ | 🔲 no drill-down |
| Cash Flow | ✅ | ✅ | 🔲 |
| Currency Balances | ✅ | ✅ | 🔲 |
| Accounts | ✅ | ✅ | 🔲 no account detail link |
| Budgets | ✅ | ✅ | 🔲 no budget detail |
| Planned Payments | ✅ | ✅ | 🔲 no mark-as-paid action |
| Loans | ✅ | ✅ | 🔲 no repayment action |
| Recent Activities | ✅ | ✅ | ⚠️ no tap-to-detail from dashboard card (works in Records page) |
| Category Pie Chart | ✅ | ✅ | 🔲 |
| Category Treemap | ✅ | ✅ | 🔲 |
| Cash Flow Comparison | ✅ | ✅ | 🔲 |
| Income & Expense Bar | ✅ | ✅ | 🔲 |
| Category Spending Trend | ✅ | ✅ | 🔲 |
| Tag UpSet Plot | ✅ | ✅ | 🔲 |

---

## Accounts

| Feature | Android | Web |
|---|---|---|
| Account list | ✅ | ✅ |
| Account detail page (balance, history, charts) | ✅ | ✅ balance + last 20 transactions |
| Create account | ✅ | ✅ |
| Edit account | ✅ | ✅ |
| Delete account | ✅ | ✅ |
| Initial balance on create | ✅ | ✅ |
| Account number field | ✅ | 🔲 |
| Reorder accounts | ✅ | ✅ |
| Allowed currencies list | ✅ | ✅ |
| Account transaction history (filtered view) | ✅ | ⚠️ via Records page with query param |

---

## Budgets

| Feature | Android | Web |
|---|---|---|
| Budget list | ✅ | ✅ |
| Budget detail / drill-down | ✅ | ✅ |
| Create budget | ✅ | ✅ |
| Edit budget | ✅ | ✅ |
| Delete budget | ✅ | ✅ |
| Progress bar / spending display | ✅ | ✅ |
| Category breakdown in detail | ✅ | 🔲 |
| Archive / restore budget | ✅ | 🔲 |

---

## Loans

| Feature | Android | Web |
|---|---|---|
| Loan list | ✅ | ✅ |
| Loan detail page (schedule, interest) | ✅ | ✅ principal, paid, remaining, repayment history |
| Create loan | ✅ | ✅ |
| Edit loan | ✅ | ✅ |
| Delete loan | ✅ | ✅ |
| Log repayment | ✅ | ✅ |
| Delete repayment | ✅ | ✅ |
| Expected settlement date | ✅ | ✅ |
| Interest rate field | ✅ | ✅ |

---

## Planned Payments

| Feature | Android | Web |
|---|---|---|
| Upcoming / overdue list | ✅ | ✅ via dashboard card |
| Full planned payments page | ✅ | 🔲 |
| Create planned payment | ✅ | ✅ |
| Edit planned payment | ✅ | ✅ |
| Delete planned payment | ✅ | ✅ |
| Mark as paid (generate transaction) | ✅ | 🔲 |
| Skip occurrence | ✅ | 🔲 |
| Recurrence display (next due, history) | ✅ | 🔲 |
| Auto-confirm toggle | ✅ | ✅ |

---

## Categories

| Feature | Android | Web |
|---|---|---|
| Category list (hierarchical) | ✅ | ✅ |
| Create category | ✅ | ✅ |
| Edit category | ✅ | ✅ |
| Delete category | ✅ | ✅ |
| Parent / child hierarchy display | ✅ | ✅ |
| Nature (Must / Need / Want) | ✅ | ✅ |
| Color picker | ✅ | ✅ |
| Icon picker | ✅ | 🔲 |
| Drag-to-reorder | ✅ | 🔲 |

---

## Tags

| Feature | Android | Web |
|---|---|---|
| Tag list | ✅ | ✅ |
| Create tag | ✅ | ✅ |
| Rename tag | ✅ | ✅ |
| Delete tag | ✅ | ✅ |
| Merge tags | ✅ | 🔲 |
| Tag transaction count | ✅ | ✅ |

---

## Templates

| Feature | Android | Web |
|---|---|---|
| Template list | ✅ | ✅ |
| Create template | ✅ | ✅ |
| Edit template | ✅ | ✅ |
| Delete template | ✅ | ✅ |
| Apply template to new transaction | ✅ | ✅ Use button → prefills Add Record form |

---

## Filters

| Feature | Android | Web |
|---|---|---|
| Per-card filter sheet (all dimensions) | ✅ | ✅ |
| Saved filter sets | ✅ | 🔲 |
| Create / edit / delete saved filter | ✅ | 🔲 |
| Apply saved filter to records | ✅ | 🔲 |

---

## Analytics

| Feature | Android | Web |
|---|---|---|
| Analytics screen (standalone page) | ✅ | 🔲 no dedicated page |
| All 14 chart card types | ✅ | ✅ via dashboard |
| Savings rate | ✅ | 🔲 |
| Expense forecast | ✅ | 🔲 |

---

## Settings

| Setting | Android | Web |
|---|---|---|
| Base currency | ✅ editable | ✅ editable |
| Decimal precision | ✅ editable | ✅ editable |
| Theme (light / dark / system) | ✅ | 🔲 |
| Display density | ✅ | 🔲 |
| Shortlisted currencies | ✅ | 🔲 |
| Exchange rates setup | ✅ | 🔲 |
| Data export / backup | ✅ | 🔲 |
| PIN / biometric security | ✅ | 🔲 |
| Notification settings | ✅ | 🔲 |
| Profile management | ✅ | 🔲 |

---

## Pages / Navigation Sections

| Section | Android | Web |
|---|---|---|
| Dashboard | ✅ | ✅ |
| Transactions / Records | ✅ | ✅ |
| Accounts | ✅ | ✅ |
| Account Detail | ✅ | ✅ |
| Budgets | ✅ | ✅ |
| Loans | ✅ | ✅ |
| Planned Payments | ✅ | ✅ full page with confirm/skip/create/edit/delete |
| Templates | ✅ | ✅ |
| Categories | ✅ | ✅ |
| Tags | ✅ | ✅ |
| Filters (saved sets) | ✅ | 🔲 |
| Analytics | ✅ | 🔲 |
| Settings | ✅ | ✅ currency + precision editable |
| Exchange Rates | ✅ | 🔲 |
| Shared Wallet | ✅ | 🔲 |
| Smart Suggestions | ✅ | 🔲 |

---

## Priority Ranking

### 🔴 Critical
- ~~Transaction detail view~~ ✅ implemented
- ~~Templates (list + create + apply to transaction)~~ ✅ implemented
- ~~Payment method on transaction form~~ ✅ implemented
- ~~Warranty fields on transaction form~~ ✅ implemented
- ~~Planned payments full page (mark-as-paid, skip)~~ ✅ was already complete
- Split transactions — needs new DB schema + API (deferred)

### 🟠 High
- ~~Account detail page~~ ✅ implemented
- ~~Budget detail page~~ ✅ implemented
- ~~Loan detail page~~ ✅ implemented
- ~~Settings writability (currency, precision)~~ ✅ implemented
- ~~Account initial balance on create~~ ✅ implemented
- ~~Transaction clone~~ ✅ implemented
- Linked transactions
- GPS / location fields

### 🟡 Medium
- Transaction list grouping + sorting
- Filter saved sets
- Tag merge
- Exchange rates management
- Category icon picker
- Category drag-to-reorder
- Note / payee autocomplete
- Recurrence display on planned payments
- Archive / restore budgets

### 🟢 Low / Future
- Analytics standalone page
- Savings rate + expense forecast
- Shared wallet
- Smart suggestions
- Notification settings
- Onboarding flow
- Data export / backup from web
- Theme toggle in web
