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
| Attachments (photos, PDFs, any file) | ✅ | ✅ in the transaction detail: upload, view images, download others, remove |
| Template application (prefill) | ✅ | ✅ via Templates page → Use button |
| Calculator numpad for amount | ✅ | ⚠️ standard `<input>` only |
| Note / Payee autocomplete | ✅ | 🔲 |

---

## Transactions / Records Page

| Feature | Android | Web |
|---|---|---|
| Transaction list | ✅ | ✅ |
| Grouping (by date / category / account) | ✅ | ✅ client-side group by date / type / category / account |
| Sorting controls | ✅ | ✅ sort by date / amount / title |
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
| Accounts | ✅ | ✅ | ✅ click row → account detail |
| Budgets | ✅ | ✅ | ✅ click row → budget detail |
| Planned Payments | ✅ | ✅ | 🔲 no mark-as-paid action |
| Loans | ✅ | ✅ | ✅ click row → loan detail |
| Recent Activities | ✅ | ✅ | ✅ tap row → transaction detail modal |
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
| Archive / restore budget | ✅ | ✅ archive/restore buttons + archived toggle view |

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
| Drag-to-reorder | ✅ | ⚠️ ↑↓ buttons (no drag-and-drop) |

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
| Saved filter sets | ✅ | ✅ full filters page |
| Create / edit / delete saved filter | ✅ | ✅ |
| Apply saved filter to records | ✅ | ✅ ▶ Apply button navigates to Records with params |

---

## Analytics

| Feature | Android | Web |
|---|---|---|
| Analytics screen (standalone page) | ✅ | ✅ dedicated page with date/type/granularity filters |
| All 14 chart card types | ✅ | ✅ via dashboard |
| Savings rate | ✅ | ✅ shown as summary stat card + per-period in table |
| Expense forecast | ✅ | 🔲 |

---

## Settings

| Setting | Android | Web |
|---|---|---|
| Base currency | ✅ editable | ✅ editable |
| Decimal precision | ✅ editable | ✅ editable |
| Theme (light / dark / system) | ✅ | ✅ Auto / Light / Dark toggle, persisted to localStorage |
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
| Filters (saved sets) | ✅ | ✅ |
| Analytics | ✅ | ✅ |
| Settings | ✅ | ✅ currency + precision + theme + page size |
| Exchange Rates | ✅ | 🔲 |
| Shared Wallet | ✅ | 🔲 |
| Smart Suggestions | ✅ | 🔲 |

---

## Priority Ranking

### ✅ Completed
- Transaction detail view
- Templates (list + create + apply to transaction)
- Payment method on transaction form
- Warranty fields on transaction form
- Planned payments full page (create / edit / delete / auto-confirm)
- Account detail page
- Budget detail page + archive/restore
- Loan detail page
- Settings writability (currency, precision, theme, page size)
- Account initial balance on create + reorder
- Transaction clone
- Transaction list grouping + sorting
- Saved filter sets (CRUD + apply to Records)
- Analytics standalone page + savings rate
- Dashboard drill-downs (accounts → detail, budgets → detail, loans → detail, recent activities → tx detail)
- Tags rename + delete
- Categories reorder (↑↓ buttons)

---

## Remaining Gaps

### 🟠 High — implementable now (no backend changes needed)

| Feature | Notes |
|---|---|
| GPS / location field on transaction form | `location` field exists on `TransactionDto` — missing input on add/edit form and display in list |
| Planned payments standalone page | Nav link exists but page is a stub — needs full list with next-due dates, overdue highlighting |
| Mark as paid on planned payments | API exists: `POST /api/planned-payments/{id}/confirm` |
| Skip occurrence on planned payments | API exists: `POST /api/planned-payments/{id}/skip` |
| Recurrence display (next due, history) | Data returned by API — not yet rendered on the planned payments page |
| Budget category breakdown in detail | Check whether `GET /api/budgets/{id}/categories` (or similar) endpoint already exists |

### 🟡 Medium — implementable now

| Feature | Notes |
|---|---|
| Dashboard chart card drill-downs | Net Worth, Cash Flow, Currency Balances, Category Pie/Treemap, etc. — rendered but no click actions |
| Category icon picker | Currently free-text emoji input — could add a small emoji grid |

### 🔴 Blocked — need backend changes

| Feature | Blocker |
|---|---|
| Split transactions | Needs new DB schema + API endpoint |
| Linked transactions (create/manage) | Needs a linking endpoint; display-only already done in detail view |
| Tag merge | Needs `POST /api/tags/merge` endpoint |
| Note / payee autocomplete | Needs `GET /api/transactions/autocomplete?field=note&q=...` endpoint |
| Account number field | Not in domain `Account` model — needs Room migration + DTO changes |
| Exchange rates page | No management API exposed |
| Shared Wallet | No web API exposed |
| Smart Suggestions | No web API exposed |
| Data export / backup | No web endpoint |
| Profile management | No multi-profile switching API |
| Expense forecast | No forecast API |
