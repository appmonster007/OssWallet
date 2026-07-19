---
name: backup-security
description: Known security defects and patch-verification results for BackupRepositoryImpl/CsvImportServiceImpl on feature/tagv47
metadata:
  type: project
---

Audit performed on branch feature/tagv47. A round of security patches was applied; their correctness was verified on 2026-07-07.

## Patches verified CLEAN (no new issues from these)
- `writer.flush()` replaces `writer.close()` in `exportFlatCsv` — correct, avoids premature stream close.
- Filter scoping (`scopedFilterIds`): `BudgetEntity.filterId` and `LoanEntity.filterId` are both non-nullable `Long`, so `Set<Long>` arithmetic is sound; no `Set<Long?>` confusion.
- `templateId in templateIds` guard: `TemplateEntity.id: Long` and `PlannedPaymentEntity.templateId: Long` — types match, guard is correct.
- `parseCsv` explicit `settings.format.setDelimiter(',')` — correct univocity-parsers 2.9.1 API.
- `p["iconName"] ?: p["icon"] ?: ""` fallback in `restoreCategories` — correct backwards compat.
- `toBigDecimalOrNull()` applied at all 9 numeric restore sites (transactions×2, templates×2, budgets×1, loan_payments×1, loans×1, filters×2).
- `normalizeAmount` positional heuristic (no ICU4J) — no ICU4J imports present; heuristic is correct.

## Remaining defects after patching

**HIGH — Zip Slip path-prefix bypass (lines 267, 561):**
```
if (!file.canonicalPath.startsWith(tempDir.canonicalPath)) throw SecurityException("Zip Slip!")
```
A sibling directory named `restore_temp2` passes the check because `"…/restore_temp2/x".startsWith("…/restore_temp")` is true. Fix: append `File.separator` to `tempDir.canonicalPath` in the check.

**MEDIUM — Per-entry zip bomb counter (not cumulative, line 616-625):**
`copyToLimited` resets the 512 MB counter on every call. A zip with N entries each just under 512 MB can extract N×512 MB to disk. Counter should be shared across the entire extraction loop.

**MEDIUM — Temp directory not cleaned on exception (lines 298, 593):**
`tempDir.deleteRecursively()` is only reached on the happy path. If `withTransaction` throws, extracted restore data persists in the cache directory. Needs `try/finally`.

**Why:** These survived the patch round and represent the most critical open issues in the backup subsystem.

**How to apply:** On any future backup PR, verify: (1) Zip Slip check uses `+ File.separator`, (2) zip extraction byte counter is cumulative, (3) temp dir cleanup is in a `finally` block.
