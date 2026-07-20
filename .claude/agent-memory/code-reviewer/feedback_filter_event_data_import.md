---
name: FilterEvent data layer import
description: FilterEvent.kt (ui layer) imports TransactionSectionSettings from data.local — a layer boundary violation to flag in reviews
type: feedback
---

`FilterEvent.kt` at `ui/components/transactions/FilterEvent.kt` imports `com.osscabinet.ossWallet.data.local.TransactionSectionSettings` for its `UpdateSettings` variant.

`TransactionSectionSettings` is defined in `data/local/SettingsDataStore.kt` (package `com.osscabinet.ossWallet.data.local`), making this a ui→data import that violates the Clean Architecture boundary.

**Why:** `TransactionSectionSettings` should either live in `domain/` or have a domain-layer equivalent that `FilterEvent` can reference. This was introduced as part of the `FilterEvent` callback refactor on `feature/tagv22`.

**How to apply:** Flag any future `ui/` file that imports from `data.local` as a **Critical** layer boundary violation per the code review rules.
