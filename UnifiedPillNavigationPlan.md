# Unified Pill Navigation Implementation Plan

This plan outlines the steps to create a new, unified floating pill navigation system that combines the bottom navigation bar and the expandable FAB into a single, cohesive UI element as seen in the provided screenshot.

## Proposed Changes

### Navigation Components

#### [NEW] [PillNavigation.kt](file:///Users/prathitaswar/Desktop/Dev/AndroidStudioProjects/OssWallet/app/src/main/java/com/osscabinet/ossWallet/ui/components/navigation/PillNavigation.kt)

Create a new file to host the unified pill navigation components.

- `PillNavigation`: A floating pill-shaped container for navigation items with a fluid sliding indicator.
- `PillFab`: A matching circular FAB component that supports expansion.
- `UnifiedPillNavigation`: A wrapper component that positions both the `PillNavigation` and `PillFab` side-by-side at the bottom of the screen.

Key features:
- Material 3 Expressive aesthetics.
- Fluid motion using `animateDpAsState` with spring physics for the indicator.
- Support for background dimming when the FAB is expanded.
- Integrated template actions from `AeonExpandableFab`.

```kotlin
@Composable
fun UnifiedPillNavigation(
    selectedTab: Int,
    onTabSelected: (Int) -> Unit,
    tabs: List<NavigationTab>,
    fabOptions: FabOptions,
    modifier: Modifier = Modifier
) {
    Box(modifier = Modifier.fillMaxSize()) {
        // Dimming background for FAB expansion
        // ...
        Row(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 24.dp)
                .padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            PillNavigationBar(
                selectedTab = selectedTab,
                onTabSelected = onTabSelected,
                tabs = tabs
            )
            PillFab(
                options = fabOptions
            )
        }
    }
}
```

#### [DELETE] [NavigationPill.kt](file:///Users/prathitaswar/Desktop/Dev/AndroidStudioProjects/OssWallet/app/src/main/java/com/osscabinet/ossWallet/ui/components/NavigationPill.kt)

Remove the legacy implementation.

---

### Screen Updates

#### [DashboardScreen.kt](file:///Users/prathitaswar/Desktop/Dev/AndroidStudioProjects/OssWallet/app/src/main/java/com/osscabinet/ossWallet/ui/dashboard/DashboardScreen.kt)

- Replace `ExpressiveNavigationBar` and `AeonExpandableFab` with the new `UnifiedPillNavigation`.
- Adjust padding to accommodate the new floating element.

#### [AccountDetailScreen.kt](file:///Users/prathitaswar/Desktop/Dev/AndroidStudioProjects/OssWallet/app/src/main/java/com/osscabinet/ossWallet/ui/accountdetail/AccountDetailScreen.kt)

- Replace `ExpressiveNavigationBar` and `AeonExpandableFab` with the new `UnifiedPillNavigation`.

---

## Verification Plan

### Automated Tests
- Since these are purely UI changes, we will verify using Compose Previews.
- Run `gradle_build` to ensure no regressions in compilation.

### Manual Verification
- Render the Compose Preview for `UnifiedPillNavigation` to verify the layout and motion.
- Deploy the app and verify:
    - Smooth transition between "Glance" and "Recent Activities" in Dashboard.
    - FAB expansion and menu visibility.
    - Background dimming effect.
    - Responsiveness on different screen sizes.
