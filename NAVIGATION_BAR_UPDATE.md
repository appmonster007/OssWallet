# Navigation Bar Update - Standard Material 3 with Expressive Colors

## Change Summary
Reverted `ExpressiveNavigationBar` to use the standard Material 3 `NavigationBar` component with the new expressive color system. This ensures proper z-index handling so the navigation bar sits behind drawers when opened, while still benefiting from the vibrant color revision.

## Implementation Details

### Design Decision
Reverted from custom implementation to standard Material 3 `NavigationBar` because:
- Proper z-index handling for drawers (sits behind when drawer opens)
- Bigger, more prominent indicator bubble (Material 3 standard)
- Better accessibility and touch targets
- Consistent with Material 3 guidelines
- Smoother built-in animations

### Current Implementation
- Uses standard Material 3 `NavigationBar` and `NavigationBarItem`
- Indicator (bubble) appears behind icon only (Material 3 standard)
- Text displayed below icon in both selected and unselected states
- Applies new expressive colors from Material 3 expressive color revision

## Visual States

### Selected State
```
┌─────────────────────────┐
│    [●]                 │  ← Bubble behind icon
│      Icon              │
│     Label              │
└─────────────────────────┘
```

### Unselected State
```
┌─────────────────────────┐
│                         │
│      Icon              │
│     Label              │
└─────────────────────────┘
```

## Color Usage

### Selected State
- **Indicator (Bubble)**: `MaterialTheme.colorScheme.primary` (vibrant teal)
- **Icon**: `MaterialTheme.colorScheme.onPrimary` (white)
- **Text**: `MaterialTheme.colorScheme.onPrimary` (white)

### Unselected State
- **Icon**: `MaterialTheme.colorScheme.onSurfaceVariant`
- **Text**: `MaterialTheme.colorScheme.onSurfaceVariant`

### Navigation Bar Background
- **Container Color**: `MaterialTheme.colorScheme.surfaceVariant`
- **Elevation**: 8dp

## Benefits

✅ **Proper Z-Index**: Navigation bar sits behind drawers when opened (fixed by moving to Scaffold bottomBar)
✅ **Bigger Bubble**: Material 3 standard indicator is more prominent
✅ **Vibrant Colors**: Uses new expressive color system
✅ **Better Accessibility**: Standard Material 3 touch targets and semantics
✅ **Consistent**: Follows Material 3 guidelines
✅ **Smooth Animations**: Built-in Material 3 animations

## Files Modified

- `app/src/main/java/com/osscabinet/ossWallet/ui/components/navigation/ExpressiveNavigationBar.kt`
  - Reverted to standard Material 3 `NavigationBar` implementation
  - Removed custom bubble animation
  - Applied new expressive colors to NavigationBarItemDefaults
  - Maintained proper z-index handling for drawers

- `app/src/main/java/com/osscabinet/ossWallet/ui/dashboard/DashboardScreen.kt`
  - Moved ExpressiveNavigationBar to Scaffold's `bottomBar` parameter
  - Moved AeonExpandableFab inside ModalNavigationDrawer but outside Scaffold
  - Removed duplicate navigation bar and FAB that were outside Scaffold
  - **Fixed**: Navigation bar now sits behind left drawer when opened (proper z-index)
  - **Fixed**: FAB now sits behind left drawer but above navigation bar
  - **Fixed**: Clicking FAB when drawer is open closes drawer instead of opening FAB menu

- `app/src/main/java/com/osscabinet/ossWallet/ui/accountdetail/AccountDetailScreen.kt`
  - Moved ExpressiveNavigationBar to Scaffold's `bottomBar` parameter for consistency
  - Removed duplicate navigation bar that was in Box
  - FAB remains in Box (outside Scaffold) to hover over whole screen
  - **Consistent**: Same structure as DashboardScreen (no drawer in AccountDetailScreen, so no z-index issue)
