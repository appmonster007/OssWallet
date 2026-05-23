---
name: project-feature-audit-2026-05
description: Full feature-completeness audit performed May 2026 — findings per feature with severity ratings
metadata:
  type: project
---

Comprehensive audit of all OssWallet features performed 2026-05-23. Key findings:

- Categories and Tags are intentionally profile-global (no profileId column). Confirmed by code comment in CategoryEntity.
- FilterEntity and TagEntity have no profileId by design — they are shared resources (filters are linked to budgets/loans via FK, not directly to a profile).
- BudgetEntryViewModel and LoanEntryViewModel have a dead `filterUsageCount` field that is always 0 — the guard condition `filterUsageCount > 1` can never be true, so shared-filter dialog never shows for budget/loan edits.
- PlannedPaymentEntryViewModel has a delete logic bug: when `deleteTemplateWithPayment == true`, it deletes the template but NOT the planned payment itself, leaving orphaned payment records.
- SharedWallet is intentionally a mock/placeholder with hardcoded invite code "OSSWALLET123".
- Migration gap: AppDatabase version=9 but MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4 are absent — clean installs go through default Room schema creation and only need the 4→9 migrations; but upgrades from version < 4 would crash.
- RECEIVE_BOOT_COMPLETED permission declared but no BroadcastReceiver registered — WorkManager persists across reboots via its own boot receiver so this permission is unused/harmless.
- AccountStructureType in domain/ imports androidx.compose.* — critical layer violation.
- CurrencyRepository.getExchangeRates returns Map<String,Double> which is converted via BigDecimal.valueOf(Double) — precision is limited to Double before BigDecimal wraps it; this is a known pattern throughout exchange rate computation.
- Biometric setting exposed in SettingsRepository permission list but no biometric lock screen is implemented in the UI.

**Why:** To establish a baseline understanding of what is complete vs incomplete.
**How to apply:** Use when assessing whether a new feature or PR requires companion work in adjacent features (e.g. budget filter shared dialog).
