# Story 6.2: Per-Account Currency Selector in Import Account-Mapping Step

Status: ready-for-dev

## Story

As a user,
I want to set each imported account's currency individually during the import flow,
so that my accounts are not all silently assigned the app's global base currency.

## Acceptance Criteria

1. **Given** the account-mapping step **When** I review accounts to import **Then** each account row displays a currency selector pre-populated with the currency detected from the export file (or the app's base currency if none was detected).
2. **Given** an account that already exists in the wallet **When** I map the import row to it **Then** no currency selector is shown (existing accounts keep their stored currency).
3. **Given** I change the currency for a new account **When** the import completes **Then** that account is created with my chosen currency, not the default.
4. **Given** accounts where multiple distinct currencies appear in the CSV rows **When** I view the selector **Then** the first detected currency is pre-selected and I can change it to any shortlisted currency.

## Tasks / Subtasks

- [ ] Extend `AccountToMap` domain model to carry detected currencies (AC: 1, 4)
  - [ ] In `core/domain/src/main/java/com/osscabinet/ossWallet/domain/model/import_csv/ImportPreview.kt`, add `detectedCurrencies: Set<String> = emptySet()` field to `AccountToMap`
- [ ] Pass detected currencies from `CsvImportServiceImpl` (AC: 1, 4)
  - [ ] In `CsvImportServiceImpl.generateBudgetBakersPreview` (~line 422–431), change `AccountToMap(...)` construction to pass `detectedCurrencies = accountDetectedCurrencies[name] ?: emptySet()` in addition to the existing `hasDetectedCurrency` field
  - [ ] Apply the same to `generateCashewPreview` if it populates account currency data
- [ ] Fix `CsvImportViewModel.initializeDefaultMappings` (AC: 1, 2, 3)
  - [ ] In `CsvImportViewModel.kt` line ~153, change:
    ```kotlin
    currencyCode = if (!acc.hasDetectedCurrency) defaultCurrency else null
    ```
    to:
    ```kotlin
    currencyCode = acc.detectedCurrencies.firstOrNull() ?: defaultCurrency
    ```
  - [ ] This ensures every new account always has a non-null `currencyCode` in `EntityMapping`, making the UI selector visible for all new accounts
- [ ] Verify selector is hidden for mapped-to-existing accounts (AC: 2)
  - [ ] In `CsvImportComponents.kt` `EntityMappingRow`, the selector is gated on `isCreateNew && currencyCode != null` (line ~204) — confirm `isCreateNew` is false when an existing account is selected, which hides the selector
- [ ] Integration test (AC: 3)
  - [ ] Import a test CSV with accounts that have currency columns; assert the created `AccountEntity.currencyCode` matches the currency from the file, not the default

## Dev Notes

### Root Cause

`AccountToMap.hasDetectedCurrency` is `Boolean` — it records that a currency was found in the CSV, but discards the actual value. `CsvImportViewModel.initializeDefaultMappings` then checks this boolean with inverted logic:
```kotlin
// WRONG — hides the selector when currency IS detected
currencyCode = if (!acc.hasDetectedCurrency) defaultCurrency else null
```
This means accounts WITH a detected currency get `currencyCode = null`, which prevents `EntityMappingRow` from rendering the selector (selector requires `currencyCode != null`).

### Fix Summary

1. **Domain model** (`ImportPreview.kt`): add `detectedCurrencies: Set<String>` to `AccountToMap`
2. **Data layer** (`CsvImportServiceImpl`): populate `detectedCurrencies` from the already-collected `accountDetectedCurrencies` map
3. **ViewModel** (`CsvImportViewModel`): use `detectedCurrencies.firstOrNull() ?: defaultCurrency` so every new account has a non-null initial currency

### Files to Touch

| File | Change |
|------|--------|
| `core/domain/src/main/java/com/osscabinet/ossWallet/domain/model/import_csv/ImportPreview.kt` | Add `detectedCurrencies: Set<String> = emptySet()` to `AccountToMap` |
| `core/data/src/main/java/com/osscabinet/osswallet/data/service/CsvImportServiceImpl.kt` | Pass `detectedCurrencies` when constructing `AccountToMap` in `generateBudgetBakersPreview` (~line 428) and `generateCashewPreview` (~line 1096) |
| `app/src/main/java/com/osscabinet/ossWallet/ui/screens/import_csv/CsvImportViewModel.kt` | Line ~153: fix `currencyCode` initialisation logic |

### Invariants

- Changing `AccountToMap` is a pure domain model change — no Room migration needed
- `EntityMapping.currencyCode` already carries the selected currency to `executeImport` → `accountDao.upsertAccount(… currencyCode = currency …)`. No additional wiring needed at the execute layer.

### References

- `core/domain/.../import_csv/ImportPreview.kt` — `AccountToMap` definition
- `core/data/.../CsvImportServiceImpl.kt` lines ~347–431 — `generateBudgetBakersPreview` account collection
- `app/.../import_csv/CsvImportViewModel.kt` lines ~145–155 — `initializeDefaultMappings`
- `app/.../import_csv/components/CsvImportComponents.kt` lines ~70–245 — `EntityMappingRow` with currency selector

## Dev Agent Record

### Agent Model Used

### Debug Log References

### Completion Notes List

### File List
