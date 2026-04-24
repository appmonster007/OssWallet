# Transaction Swipe Actions Implementation Plan

## Overview
Add swipe gesture support to transaction items in RecentActivities and Records screens for quick actions.

## Requirements

### Swipe Actions

#### Right Swipe (Status Change)
- **Half Swipe**: Reveal status change buttons
  - Show statuses that are NOT currently set
  - Left side: RECONCILE → CLEARED
  - Right side: UNCLEARED
- **Full Swipe**: Toggle between CLEARED and UNCLEARED

#### Left Swipe (Transaction Actions)
- **Half Swipe**: Reveal action buttons
  - Split transaction
  - Clone transaction
  - Delete transaction

## Implementation Plan

### Phase 1: Create Swipeable Component
1. Create `SwipeableTransactionItem` composable
   - Based on reference DraggableCard code
   - Support left and right swipe detection
   - Reveal action buttons on half swipe
   - Handle full swipe for status toggle

2. Create `TransactionActionsRow` composables
   - `StatusActionsRow` for right swipe (status change buttons)
   - `TransactionActionsRow` for left swipe (split, clone, delete)

### Phase 2: Update ViewModels
1. Update `TransactionViewModel`
   - Add `revealedCardIdsList` StateFlow to track revealed items
   - Add `onItemExpanded(cardId)` and `onItemCollapsed(cardId)` methods
   - Add methods for quick actions:
     - `quickUpdateStatus(transactionId, newStatus)`
     - `quickCloneTransaction(transactionId)`
     - `quickDeleteTransaction(transactionId)`
     - `quickSplitTransaction(transactionId)` (if applicable)

### Phase 3: Integrate into Screens
1. Update `RecentActivitiesCard`
   - Replace AeonTransactionItem with SwipeableTransactionItem
   - Wire up ViewModel methods
   - Update UI to show action buttons

2. Update Records screen (AeonTransactionComponents)
   - Replace AeonTransactionItem with SwipeableTransactionItem
   - Wire up ViewModel methods

### Phase 4: Action Implementation
1. **Status Change Actions**
   - Quick update transaction status
   - Show success feedback (snackbar/toast)

2. **Clone Transaction**
   - Create copy of transaction with new timestamp
   - Navigate to edit screen or save directly

3. **Delete Transaction**
   - Show confirmation dialog
   - Delete transaction and update balances

4. **Split Transaction** (if applicable)
   - Create split transaction logic
   - May require additional UI for split configuration

### Phase 5: Visual Design
1. Design action buttons with appropriate icons and colors
2. Status actions:
   - RECONCILE: Blue with sync icon
   - CLEARED: Green with check icon
   - UNCLEARED: Orange with schedule icon
3. Transaction actions:
   - Split: Purple with split icon
   - Clone: Gray with copy icon
   - Delete: Red with trash icon

### Phase 6: Testing
1. Test swipe gestures
2. Test action buttons
3. Test full swipe status toggle
4. Test state restoration on scroll
5. Test edge cases (empty list, single item, etc.)

## Technical Details

### Swipe Thresholds
- Half swipe: ~80-100dp (reveal actions)
- Full swipe: ~200dp+ (trigger action)

### State Management
- Use StateFlow to track revealed card IDs
- Only one card revealed at a time (optional, can allow multiple)

### Animation
- Use Compose Animation API for smooth transitions
- Animate card offset and background color
- Duration: 300-500ms

### Constants
```kotlin
const val ACTION_ITEM_SIZE = 56
const val CARD_HEIGHT = 80 // Transaction item height
const val HALF_SWIPE_THRESHOLD = 100f
const val FULL_SWIPE_THRESHOLD = 200f
```

## File Changes

### New Files
- `ui/components/transactions/SwipeableTransactionItem.kt`
- `ui/components/transactions/TransactionActionsRow.kt`

### Modified Files
- `ui/viewmodel/TransactionViewModel.kt`
- `ui/components/transactions/RecentActivitiesCard.kt`
- `ui/components/transactions/AeonTransactionComponents.kt`

## Priority
- Phase 1-3: High (Core swipe functionality)
- Phase 4: High (Action implementation)
- Phase 5: Medium (Visual polish)
- Phase 6: Medium (Testing)
