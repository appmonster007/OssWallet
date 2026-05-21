# Architect Slate: Entry Screen UI Standardization Plan

This document defines the definitive UI/UX standards for all financial entry screens in OssWallet (Transactions, Planned Payments, Budgets, and Loans). It ensures a high-fidelity, consistent experience that prioritizes data impact and mobile ergonomics.

---

## 1. Core Layout Philosophy: The Three-Tier Hierarchy

Every entry screen follows a strict three-tier hierarchy: **Impact** (Top), **Context** (Middle), and **Detail** (Bottom).

### Universal Visual Blueprint (ASCII)
```text
+------------------------------------------+  <-- Scaffold Background
| [Back]        SCREEN TITLE       [SAVE]  |  <-- Top Bar (Sticky)
+------------------------------------------+
|                                          |
|  +------------------------------------+  |  <-- IMPACT HEADER CARD
|  |     [ EXP ] [ INC ] [ TRF ]        |  |  <-- Type Selector
|  |                                    |  |
|  |        (CUR)   1,250.00            |  |  <-- Amount Display
|  |                                    |  |
|  |         [Entity Name]              |  |  <-- Optional Name Input
|  +------------------------------------+  |
|                                          |
|  +------------------------------------+  |  <-- ROW 1: PRIMARY ACTION
|  | (IC) PRIMARY ATTRIBUTE (e.g Date)   |  |  (Whole row for (temporal data) or (template selection and temporal data))
|  +------------------------------------+  |
|                                          |
|  +------------------+  +---------------+ |  <-- ROW 2: CONTEXT GRID
|  | (IC) ACCOUNT     |  | (IC) CATEGORY  | |  <-- 2-Column Standard
|  |      Main Bank   |  |      Shopping  | |
|  +------------------+  +---------------+ |
|                                          |
|  (IC) PAYEE   ________________________   |  <-- DETAIL LIST
|  (IC) NOTES   ________________________   |  <-- EditorialInput Style
|  (IC) TAGS    [Tag1] [Tag2] [+]          |  <-- Tags always auxiliary
|                                          |
+------------------------------------------+
```

---

## 2. Granular Justifications & Design Logic

### A. The Impact Header Card
| Item | Location | Design | Justification |
| :--- | :--- | :--- | :--- |
| **Type Selector** | Top of Header | Animated Pill | Establishes the "financial flavor" (Expense/Income/Transfer) before numbers are entered. Prevents entry errors. |
| **Amount** | Center of Header | `displayMedium` | The most critical piece of data. Large font ensures visibility even at a glance. |
| **Currency** | Left of Amount | Selector Icon | Reading order (L to R) context first ($ before 10.00). Informs user of unit before value. |
| **Entity Name** | Bottom of Header | `headlineSmall` | For Planned/Budget/Loan, Name anchors the recurring/long-term commitment. |

### B. The Context Grid & Single Rows
| Item | Location | Design | Justification |
| :--- | :--- | :--- | :--- |
| **Temporal Card** | Row 1 (Full) | Full Width / Split | Temporal data (Date/Schedule) is the "When". In Transactions, it shares the row with Template search. |
| **Template Box** | Row 1 (Left) | Searchable Combo| (Transactions Only) High-speed entry via search. Shows dropdown results as user types. |
| **2-Column Grid** | Row 2+ | 2-Column Split | Optimizes information density in the "Golden Thumb Zone". Reduces vertical scrolling by 50%. |
| **Icon (Left)** | Card Leading | 10% Opacity Tint | Provides a visual anchor and scan-ability without overwhelming the text value. |

---

## 3. The Multi-Stage "Schedule Builder" (Planned Payments)

Tapping the **SCHEDULE** card opens a staged modal, similar to `AeonDateTimePicker`, to handle complex temporal logic with zero friction.

### Flow Logic Stages
1.  **Stage 1: The Timeline:** User selects the **Start Date** via calendar. The system then asks to confirm the **Next Due Date** (the first actual payment), allowing for exceptions.
2.  **Stage 2: Frequency (Tabbed View):**
    *   **PRESETS Tab:** Includes Daily, Weekly, Bi-weekly, Monthly, Quarterly, Semi-annually, Yearly.
    *   **CUSTOM (CRON) Tab:** Provides a raw input for power users with a **Natural Language Preview** below (e.g., "Every 1st of the month").
3.  **Stage 3: Confirmation:** Final summary view of the logic before closing the sheet.

---

## 4. Screen-Specific ASCII Visuals

### A. Transaction Entry (Benchmark)
```text
+------------------------------------------+
| < Back      New Transaction       [Save] |
+------------------------------------------+
|  +------------------------------------+  |
|  |     [ EXP ] [ INC ] [ TRF ]        |  |
|  |                                    |  |
|  |        USD    0.00                 |  |
|  +------------------------------------+  |
|                                          |
|  +------------------+  +---------------+ |
|  | (S) TEMPLATE     |  | (I) DATE      | | <-- Searchable Templates + Date (template + temporal selection)
|  |      [ Search.. ]|  |     Oct 24    | |
|  +------------------+  +---------------+ |
|                                          |
|  +------------------+  +---------------+ |
|  | (I) ACCOUNT      |  | (I) CATEGORY  | |
|  |     Savings      |  |     Shopping  | |
|  +------------------+  +---------------+ |
|                                          |
|  (I) PAYEE   _________________________   |
|  (I) NOTES   _________________________   |
|  (I) TAGS    [Personal] [+]              |
+------------------------------------------+
```

### B. Planned Payment Entry (Enhanced)
```text
+------------------------------------------+
| < Back        New Payment         [Save] |
+------------------------------------------+
|  +------------------------------------+  |
|  |    NAME: [ Netflix Subscription ]  |  | <-- Prefills Note: "Netflix... - "
|  |                                    |  |
|  |     [ EXP ] [ INC ] [ TRF ]        |  |
|  |                                    |  |
|  |        USD    15.99                |  |
|  +------------------------------------+  |
|                                          |
|  +------------------+  +---------------+ |
|  | (S) TEMPLATE     |  | (I) SCHEDULE  | | <-- Template + Schedule Row
|  |      [ Search.. ]|  |     Monthly   | |
|  +------------------+  +---------------+ |
|                                          |
|  +------------------+  +---------------+ |
|  | (I) ACCOUNT      |  | (I) CATEGORY  | |
|  |     Visa Card    |  |     Services  | |
|  +------------------+  +---------------+ |
|                                          |
|  (I) PAYEE   _________________________   |
|  (I) NOTES   _________________________   | <-- Mapped to prefilled Note as per input title: "<Title> - <UserNote>"
|  (I) TAGS    [Recurring] [Fixed] [+]     |
|  (I) END DATE [X] (I) AUTO-LOG [X]       |
+------------------------------------------+
```

### C. Budget Entry
```text
+------------------------------------------+
| < Back        New Budget          [Save] |
+------------------------------------------+
|  +------------------------------------+  |
|  |    NAME: [ Vacation Fund ]         |  |
|  |                                    |  |
|  |        USD    2,000.00             |  |
|  +------------------------------------+  |
|                                          |
|  +------------------------------------+  |
|  | (I) PERIOD: [ Yearly ]             |  | <-- Full Row Temporal
|  +------------------------------------+  |
|                                          |
|  +------------------+  +---------------+ |
|  | (I) ACCOUNT      |  | (I) TRACKS    | | <-- Account multi select and Transaction Type multi selection (expense / income) 
|  |     All Accounts |  |     Expense   | |
|  +------------------+  +---------------+ |
|                                          |
|  +------------------------------------+  |
|  | (I) FILTERS:                       |  | <-- Category multi select 
|  | [Shopping] [Personal] [+]          |  | 
|  +------------------------------------+  |
|                                          |
|  +------------------------------------+  | 
|  | (I) TAGS:                          |  | <-- Tags multi select
|  | [abc] [def] [+]                    |  | 
|  +------------------------------------+  |
|                                          |
|  (I) NOTES   _________________________   | <-- Notes for the budget, not mapped to any field from transaction
|  (I) THRESHOLD [---O--] 80% (I) ROLLOVER | <-- Threshold, rollover and alert notification switches for risk of overflow and overlow
+------------------------------------------+
```

### D. Loan Entry
```text
+------------------------------------------+
| < Back         New Loan           [Save] |
+------------------------------------------+
|  +------------------------------------+  |
|  |     [ LENT ] [ BORROWED ]          |  |
|  |                                    |  |
|  |        USD    5,000.00             |  |
|  +------------------------------------+  |
|                                          |
|  +------------------------------------+  |
|  | (I) START DATE   |  | (I) INT. RATE % | <-- Full Row Temporal + interest rate selection
|  | [ Oct 24, 2024 ] |  | [ 5.99 %]       |
|  +------------------------------------+  |
|                                          |
|  +------------------+  +---------------+ |
|  | (I) ACCOUNT      |  | (I) SETTLEMENT| |
|  |     Savings      |  |     Dec 31    | |
|  +------------------+  +---------------+ |
|                                          |
|  +------------------------------------+  |
|  | (I) FILTERS:                       |  | <-- Category multi select 
|  | [Shopping] [Personal] [+]          |  | 
|  +------------------------------------+  |
|                                          |
|  +------------------------------------+  | 
|  | (I) TAGS:                          |  | <-- Tags multi select
|  | [abc] [def] [+]                    |  | 
|  +------------------------------------+  |
|                                          |
|  (I) LOAN NAME (Note) ________________   | <-- Mapped to Note
|  (I) COUNTERPARTY (Payee) _____________  | <-- Mapped to Payee
+------------------------------------------+
```

### E. Template Creation
```text
+------------------------------------------+
| < Back       New Template         [Save] |
+------------------------------------------+
|  +------------------------------------+  |
|  |    NAME: [ Weekly Grocery ]        |  |
|  |                                    |  |
|  |     [ EXP ] [ INC ] [ TRF ]        |  |
|  |                                    |  |
|  |        USD    0.00                 |  |
|  +------------------------------------+  |
|                                          |
|  +------------------------------------+  |
|  | (I) DEFAULT DATE: [ Use Current ]  |  | <-- Full Row Temporal (suggested behavior)
|  +------------------------------------+  |
|                                          |
|  +------------------+  +---------------+ |
|  | (I) ACCOUNT      |  | (I) CATEGORY  | |
|  |     Savings      |  |     Shopping  | |
|  +------------------+  +---------------+ |
|                                          |
|  (I) PAYEE   _________________________   |
|  (I) NOTES   _________________________   |
|  (I) TAGS    [Essential] [+]             |
+------------------------------------------+
```

---

## 5. Interaction & Haptic Standards

- **Template Selection:** Must use a searchable input field. As the user types, a dropdown appears below with filtered templates. Selecting a template auto-fills the Amount, Category, and Account.
- **Press State:** All cards MUST implement `graphicsLayer { scaleX = 0.98; scaleY = 0.98 }`.
- **Haptic Feedback:** 
    - Primary Save/Add: `HapticFeedbackType.LongPress`.
    - Grid Item Taps: `HapticFeedbackType.TextHandleMove`.
- **Architect Slate Geometry:** Primary containers use `32dp` radius. Secondary/staged sheets use `28dp`.
- **Depth:** Strictly `0.dp` elevation. Depth created via contrast and hairline borders.

---

## 6. Model Property Mapping

To maintain a lean database schema while providing a rich UI, specialized labels map to standard properties:

### A. Loan Entry
- **Loan Name Label** -> Persistent to the `Note` field in the database.
- **Counterparty Label** -> Persistent to the `Payee` field in the database.
- **Visual Distinction:** The UI labels remain "Loan Name" and "Lender/Borrower" to provide financial context.

### B. Planned Payment Entry
- **Payment Name Label** -> Persistent to the `title` field in the database.
- **Note Pre-filling:** Typing in the "Name" field automatically pre-fills the "Notes" field with `"<Title> - "`.
- **Persistent Note:** The final stored `note` is `"<Title> - <UserNote>"`. This maintains a rich description while using the standard model field.

---

