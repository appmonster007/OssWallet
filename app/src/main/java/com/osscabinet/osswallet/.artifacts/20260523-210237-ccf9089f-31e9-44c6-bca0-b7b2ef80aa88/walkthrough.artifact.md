# Walkthrough - Layered Filter Rules

I have refactored the filter locking mechanism into a generic, layered system. This architecture separates **Card Capabilities** from **Screen Constraints**, ensuring that the most restrictive rule always wins.

## Changes

### 1. Domain: Layered Rule Merging
- Added `layeredWith` to [CardFilterRules.kt](file:///Users/prathitaswar/Desktop/Dev/AndroidStudioProjects/OssWallet/app/src/main/java/com/osscabinet/ossWallet/domain/card/CardFilterRules.kt).
- This function performs a logical AND on all permissions (e.g., `allowAccountSelection`).
- Added `CardFilterRules.ACCOUNT_LOCKED` as a standard constraint profile.

### 2. UI: Generic Layering in Card Lists
- Updated [AeonCardList.kt](file:///Users/prathitaswar/Desktop/Dev/AndroidStudioProjects/OssWallet/app/src/main/java/com/osscabinet/ossWallet/ui/components/cards/AeonCardList.kt) to accept `screenRules`.
- The list now automatically layers the card's native rules with the screen's rules before rendering.
- Renamed `disableCardFilterSheet` to `disableFilterSheet` for consistency.

### 3. UI: Standardized Locked State
- Refactored [FilterComponents.kt](file:///Users/prathitaswar/Desktop/Dev/AndroidStudioProjects/OssWallet/app/src/main/java/com/osscabinet/ossWallet/ui/components/transactions/FilterComponents.kt) to implement a consistent "Lock" UI.
- If a field is disabled by rules but contains active filters (e.g., in Account Detail), it shows a **Lock Icon** and is non-interactive.
- If it is disabled and empty, it is hidden.
- This logic is now applied to **Accounts**, **Categories**, **Currencies**, **Statuses**, **Methods**, **Search**, **Tags**, **Amount Range**, and **Warranty**.

### 4. Integration: Account Detail Screen
- Updated [AccountDetailScreen.kt](file:///Users/prathitaswar/Desktop/Dev/AndroidStudioProjects/OssWallet/app/src/main/java/com/osscabinet/ossWallet/ui/screens/accounts/detail/AccountDetailScreen.kt) to use the new `screenRules` instead of specific boolean flags.

## Verification Summary

- **Architecture**: The `layeredWith` logic ensures that a screen cannot accidentally re-enable a feature that a card doesn't support, and vice-versa.
- **UI Consistency**: Every filter field now follows the same visual language for "Locked" states (Lock icon + 40% alpha tint).
- **Flexibility**: The system is now ready for future screens that might need to lock other fields (e.g., a "Category View" that locks Categories).
