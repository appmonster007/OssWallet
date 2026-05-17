# Implementation Plan - Account Entry Screen Redesign (Architect Slate)

This plan outlines the refactoring and redesign of the "Add/Edit Account" screen to strictly align with the **"Architect Slate"** UI/UX standards. This includes the "Three-Tier Hierarchy" (Impact, Context, Detail), standardized geometry, and haptic feedback patterns.

## User Review Required

- **Type Selector Mapping**: The "Type Selector" in the Impact header will map to `structureType`. I will define specific icons for Savings, Credit, etc., to fit the animated pill design.
- **Initial Balances in Edit Mode**: I will maintain the existing behavior of hiding initial balances in Edit mode (since they are transactions), but "Current Balance" will be the primary metric in the Impact header.

## Proposed Changes

### Domain & State Management

#### [AccountEntryState.kt](file:///Users/prathitaswar/Desktop/Dev/AndroidStudioProjects/OssWallet/app/src/main/java/com/osscabinet/ossWallet/ui/screens/accounts/entry/AccountEntryState.kt)
- Add `currentBalance: BigDecimal?` for the header display.
- Add visibility flags for full-screen selection screens: `showIconSheet`, `showColorSheet`, `showCurrencySheet`.

#### [AccountEntryViewModel.kt](file:///Users/prathitaswar/Desktop/Dev/AndroidStudioProjects/OssWallet/app/src/main/java/com/osscabinet/ossWallet/ui/screens/accounts/entry/AccountEntryViewModel.kt)
- Move balance calculation logic from Composable to ViewModel for better performance.
- Observe exchange rates and transactions to update the header balance reactively.

---

### UI Components (Architect Slate Standard)

#### [AccountFormBody.kt](file:///Users/prathitaswar/Desktop/Dev/AndroidStudioProjects/OssWallet/app/src/main/java/com/osscabinet/ossWallet/ui/screens/accounts/entry/components/AccountFormBody.kt)
- **Impact Tier**: Use `AeonImpactHeaderCard`.
    - **Type Selector**: Pill-style selection for `structureType`.
    - **Amount**: Large `displayMedium` display of the primary balance.
    - **Name**: Integrated `EntryEditorialInput` for the account name at the bottom of the card.
- **Context Tier**: Use `AeonContextGrid` (2-column layout).
    - **Visual Row**: Action cards for **Icon** and **Color**.
    - **Asset Row**: Action card for **Currencies** (showing count).
- **Detail Tier**: Use `AeonDetailSection`.
    - **Initial Balances** (Create mode only): Grouped editorial inputs for multi-currency accounts.
    - **Settings**: Toggle for "Archived" status (if applicable).

#### [AccountFormComponents.kt](file:///Users/prathitaswar/Desktop/Dev/AndroidStudioProjects/OssWallet/app/src/main/java/com/osscabinet/ossWallet/ui/screens/accounts/entry/components/AccountFormComponents.kt)
- Implement `AccountEntrySheets` to handle the new `AeonSelectionScreen` transitions.
- Standardize geometry: Use `32dp` radius for the primary card and `28dp` for sheets.
- Add haptic feedback: `LongPress` for save, `TextHandleMove` for grid taps.
- Implement press states: `graphicsLayer` scaling (0.98x) on all interactive cards.

---

## Verification Plan

### Automated Tests
- Run `AggregationServiceTest` to ensure balance calculation logic remains correct.
- `gradle_build("app:assembleDebug")` to verify compilation.

### Manual Verification
1.  **Create Account Flow**:
    - Verify `AeonImpactHeaderCard` correctly updates as name and initial balance are entered.
    - Verify selecting multiple currencies correctly shows initial balance inputs for each.
    - Verify icon and color selection sheets work.
2.  **Edit Account Flow**:
    - Verify "Current Balance" is correctly calculated and displayed in the header.
    - Verify changing the name or structure type updates the UI immediately.
    - Verify "Delete" functionality works from the scaffold.
3.  **Performance Check**:
    - Observe scrolling smoothness and UI responsiveness after moving balance calculation to ViewModel.
