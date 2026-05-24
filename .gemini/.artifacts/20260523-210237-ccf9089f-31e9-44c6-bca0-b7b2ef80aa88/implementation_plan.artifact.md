# Layered Filter Rules Refactor

This plan refactors the existing specific filter locking mechanism (e.g., `forceDisableAccountSelection`) into a generic, layered system. The **Screen** defines global constraints, and the **Card** defines its capabilities. The final behavior is the intersection of both ("Most restrictive rule wins").

## User Review Required

- **Locked vs. Hidden**: Currently, forbidden fields are hidden from the filter sheet. This plan proposes showing "locked" fields (lock icon + non-interactive) if they contain active filters from a base scope (like the current Account), but keeping them hidden if they are completely disabled by the card computer.

## Proposed Changes

### Domain Layer

#### [CardFilterRules.kt](file:///Users/prathitaswar/Desktop/Dev/AndroidStudioProjects/OssWallet/app/src/main/java/com/osscabinet/ossWallet/domain/card/CardFilterRules.kt)

- Add `layeredWith(constraints: CardFilterRules): CardFilterRules` function.
- Implement logical AND for all permission booleans.
- Implement precedence/intersection logic for forced and allowed transaction types.
- Add static `CardFilterRules.LOCKED_ACCOUNT` and other common constraint profiles.

---

### UI Component Layer

#### [AeonCardList.kt](file:///Users/prathitaswar/Desktop/Dev/AndroidStudioProjects/OssWallet/app/src/main/java/com/osscabinet/ossWallet/ui/components/cards/AeonCardList.kt)

- Replace `forceDisableAccountSelection: Boolean` with `screenRules: CardFilterRules = CardFilterRules.DEFAULT`.
- Update `copyWithRules` usage to apply `card.filterRules.layeredWith(screenRules)`.
- Rename `disableCardFilterSheet` to `disableFilterSheet` for consistency.

#### [FilterFields.kt](file:///Users/prathitaswar/Desktop/Dev/AndroidStudioProjects/OssWallet/app/src/main/java/com/osscabinet/ossWallet/ui/components/transactions/FilterComponents.kt)

- Refactor `EntryActionCard` calls to consistently check `filterRules` for "Locked" states.
- If `filterRules.allowX` is false AND `filter.X` is not empty -> Show `EntryActionCard` with lock icon and `enabled = false`.
- If `filterRules.allowX` is false AND `filter.X` is empty -> Hide the card (existing behavior).

---

### Screen Layer

#### [AccountDetailScreen.kt](file:///Users/prathitaswar/Desktop/Dev/AndroidStudioProjects/OssWallet/app/src/main/java/com/osscabinet/ossWallet/ui/screens/accounts/detail/AccountDetailScreen.kt)

- Update `AeonCardList` call to pass `screenRules = CardFilterRules(allowAccountSelection = false)`.

---

## Verification Plan

### Manual Verification
1. **Account Detail Screen**:
    - Open any card's filter sheet.
    - Verify "Accounts" field shows the lock icon and is not clickable.
    - Verify "Date Range" and "Categories" are still clickable.
2. **Dashboard Screen**:
    - Open the same card types.
    - Verify "Accounts" field is interactive and allows selection.
3. **Static Analysis**:
    - Run `analyze_file` on `CardFilterRules.kt` and `FilterFields.kt` to ensure no regression in logic.
