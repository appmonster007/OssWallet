# Dashboard UI Refactor Plan
## Navigation Pill & FAB Enhancement

**Author:** Senior Android Architect  
**Date:** April 22, 2026  
**Status:** Planning Phase

---

## Executive Summary

This document outlines a comprehensive plan to refactor the Dashboard screen's navigation and floating action button (FAB) implementation to align with Material 3 Design Guidelines and resolve current UX issues. The primary issue is that the custom NavigationPill and FAB remain visible above the left navigation drawer when it opens, creating a confusing user experience.

---

## Current Implementation Analysis

### Architecture Overview

```
DashboardScreen
├── Box (root container)
│   ├── ModalNavigationDrawer
│   │   └── Scaffold
│   │       └── TopAppBar
│   │       └── DashboardContent (Pager)
│   ├── NavigationPill (custom, outside Scaffold)
│   └── AeonExpandableFab (custom, outside Scaffold)
```

### Visual Layout Comparison

#### Current Implementation (With Drawer Open)

```
┌─────────────────────────────────────────────────────┐
│  ☰  Intelligence        Personal Account           │ ← TopAppBar
├─────────────────────────────────────────────────────┤
│                                                     │
│  ┌──────────────┐  ┌──────────────┐  ┌──────────┐ │
│  │  Net Worth   │  │   Currency   │  │  Cash    │ │
│  │   $12,450    │  │   Balances   │  │  Flow    │ │
│  └──────────────┘  └──────────────┘  └──────────┘ │
│                                                     │
│  ┌──────────────┐  ┌──────────────┐               │
│  │   Accounts   │  │   Budgets    │               │
│  │              │  │              │               │
│  └──────────────┘  └──────────────┘               │
│                                                     │
│                                                     │
├───────────────┬───────────────────────────────────┤ ← Drawer Edge
│               │                                   │
│  DRAWER       │  [⚪ ⚫]                           │ ← NavigationPill
│  CONTENT      │                                   │  (OVERLAYS DRAWER!)
│               │                            [+]     │ ← FAB
│  Dashboard    │                                   │  (OVERLAYS DRAWER!)
│  Records      │                                   │
│  Analytics    │                                   │
│  Budgets      │                                   │
│  Loans        │                                   │
│  Exchange     │                                   │
│  Templates    │                                   │
│  Accounts     │                                   │
│  Settings     │                                   │
│               │                                   │
└───────────────┴───────────────────────────────────┘
```

**Problem:** NavigationPill and FAB are visible above the drawer (z-index issue)

---

#### Proposed Solution A - Material 3 NavigationBar (Recommended)

```
┌─────────────────────────────────────────────────────┐
│  ☰  Intelligence        Personal Account           │ ← TopAppBar
├─────────────────────────────────────────────────────┤
│                                                     │
│  ┌──────────────┐  ┌──────────────┐  ┌──────────┐ │
│  │  Net Worth   │  │   Currency   │  │  Cash    │ │
│  │   $12,450    │  │   Balances   │  │  Flow    │ │
│  └──────────────┘  └──────────────┘  └──────────┘ │
│                                                     │
│  ┌──────────────┐  ┌──────────────┐               │
│  │   Accounts   │  │   Budgets    │               │
│  │              │  │              │               │
│  └──────────────┘  └──────────────┘               │
│                                                     │
│                                                     │
├─────────────────────────────────────────────────────┤
│  ○ Glance        ● Recent Activities               │ ← NavigationBar
├─────────────────────────────────────────────────────┤
│                                        [+] Add     │ ← Extended FAB
└─────────────────────────────────────────────────────┘
```

**With Drawer Open:**
```
┌───────────────┬───────────────────────────────────┐
│               │  ☰  Intelligence        Personal  │
│  DRAWER       │  ┌──────────────┐  ┌──────────┐ │
│  CONTENT      │  │  Net Worth   │  │  Cash    │ │
│               │  └──────────────┘  └──────────┘ │
│  Dashboard    │                                   │
│  Records      │  (Content dimmed/hidden)          │
│  Analytics    │                                   │
│  Budgets      │                                   │
│  Loans        │                                   │
│  Exchange     │                                   │
│  Templates    │                                   │
│  Accounts     │                                   │
│  Settings     │                                   │
│               │                                   │
└───────────────┴───────────────────────────────────┘
```

**Benefit:** Drawer properly overlays all content including navigation

---

#### Proposed Solution B - Custom Elements Inside Scaffold

```
┌─────────────────────────────────────────────────────┐
│  ☰  Intelligence        Personal Account           │ ← TopAppBar
├─────────────────────────────────────────────────────┤
│                                                     │
│  ┌──────────────┐  ┌──────────────┐  ┌──────────┐ │
│  │  Net Worth   │  │   Currency   │  │  Cash    │ │
│  │   $12,450    │  │   Balances   │  │  Flow    │ │
│  └──────────────┘  └──────────────┘  └──────────┘ │
│                                                     │
│  ┌──────────────┐  ┌──────────────┐               │
│  │   Accounts   │  │   Budgets    │               │
│  │              │  │              │               │
│  └──────────────┘  └──────────────┘               │
│                                                     │
│                                                     │
├─────────────────────────────────────────────────────┤
│              [⚪ ⚫]                                 │ ← NavigationPill (inside Scaffold)
├─────────────────────────────────────────────────────┤
│                                        [+]         │ ← FAB (inside Scaffold)
└─────────────────────────────────────────────────────┘
```

**With Drawer Open:**
```
┌───────────────┬───────────────────────────────────┐
│               │  ☰  Intelligence        Personal  │
│  DRAWER       │                                   │
│  CONTENT      │  (NavigationPill & FAB hidden)     │
│               │                                   │
│  Dashboard    │  [Content dimmed/hidden]          │
│  Records      │                                   │
│  Analytics    │                                   │
│  Budgets      │                                   │
│  Loans        │                                   │
│  Exchange     │                                   │
│  Templates    │                                   │
│  Accounts     │                                   │
│  Settings     │                                   │
│               │                                   │
└───────────────┴───────────────────────────────────┘
```

**Benefit:** Preserves custom aesthetic, elements hidden when drawer opens

---

#### Proposed Solution C - Hybrid (NavigationBar + Custom Styling)

```
┌─────────────────────────────────────────────────────┐
│  ☰  Intelligence        Personal Account           │ ← TopAppBar
├─────────────────────────────────────────────────────┤
│                                                     │
│  ┌──────────────┐  ┌──────────────┐  ┌──────────┐ │
│  │  Net Worth   │  │   Currency   │  │  Cash    │ │
│  │   $12,450    │  │   Balances   │  │  Flow    │ │
│  └──────────────┘  └──────────────┘  └──────────┘ │
│                                                     │
│  ┌──────────────┐  ┌──────────────┐               │
│  │   Accounts   │  │   Budgets    │               │
│  │              │  │              │               │
│  └──────────────┘  └──────────────┘               │
│                                                     │
│                                                     │
├─────────────────────────────────────────────────────┤
│  ○ Glance        ● Recent Activities               │ ← NavigationBar (styled)
├─────────────────────────────────────────────────────┤
│                              [+] Add Transaction   │ ← Extended FAB
└─────────────────────────────────────────────────────┘
```

**With FAB Expanded (Template Selection):**
```
┌─────────────────────────────────────────────────────┐
│  ☰  Intelligence        Personal Account           │
├─────────────────────────────────────────────────────┤
│                                                     │
│  ┌──────────────┐  ┌──────────────┐  ┌──────────┐ │
│  │  Net Worth   │  │   Currency   │  │  Cash    │ │
│  └──────────────┘  └──────────────┘  └──────────┘ │
│                                                     │
│  ┌──────────────┐  ┌──────────────┐               │
│  │   Accounts   │  │   Budgets    │               │
│  └──────────────┘  └──────────────┘               │
│                                                     │
│                                                     │
│                                    ┌─────────────┐ │
│                                    │ Transfer    │ │ ← Template option
│                                    └─────────────┘ │
│                              [+]  ┌─────────────┐ │
│                                    │ Shopping    │ │ ← Template option
│                                    └─────────────┘ │
│                              [+]  ┌─────────────┐ │
│                                    │ Payments    │ │ ← Template option
│                                    └─────────────┘ │
│                              [+]  ┌─────────────┐ │
│                                    │ New Trans.  │ │ ← Template option
│                                    └─────────────┘ │
│                              [✕]  │             │ │
├─────────────────────────────────────────────────────┤
│  ○ Glance        ● Recent Activities               │ ← NavigationBar
└─────────────────────────────────────────────────────┘
```

**Benefit:** Material 3 components with custom styling, template selection in BottomSheet

---

#### Responsive Design - NavigationRail (Tablet/Foldable)

```
┌─────────────────────────────────────────────────────────────────┐
│  ☰  Intelligence        Personal Account                        │
├──────┬──────────────────────────────────────────────────────────┤
│      │  ┌──────────────┐  ┌──────────────┐  ┌──────────┐      │
│ ○    │  │  Net Worth   │  │   Currency   │  │  Cash    │      │
│ Glance│  │   $12,450    │  │   Balances   │  │  Flow    │      │
│      │  └──────────────┘  └──────────────┘  └──────────┘      │
│ ●    │                                                          │
│ Recent│  ┌──────────────┐  ┌──────────────┐                   │
│ Activ.│  │   Accounts   │  │   Budgets    │                   │
│      │  │              │  │              │                   │
│      │  └──────────────┘  └──────────────┘                   │
│      │                                                          │
│      │                                                          │
│      │                                    [+] Add Transaction   │
└──────┴──────────────────────────────────────────────────────────┘
```

**NavigationRail on left side for larger screens (>600dp width)**

### Problem Identification

#### 1. **Z-Index Issue - Drawer Overlay**
- **Problem:** NavigationPill and FAB are placed in a `Box` outside the `Scaffold`, giving them a higher z-index than the drawer
- **Impact:** When the drawer opens, the NavigationPill and FAB remain visible on top of the drawer content
- **User Confusion:** Users see navigation elements overlaying drawer content, breaking the expected modal behavior

#### 2. **Custom Implementation vs Material 3 Components**
- **NavigationPill:** Custom pill-shaped tab switcher with sliding indicator
- **AeonExpandableFab:** Custom expandable FAB with template shortcuts
- **Material 3 Alternatives:**
  - `NavigationBar` for bottom navigation
  - `FloatingActionButton` for primary actions
  - `NavigationRail` for larger screens

#### 3. **Accessibility Concerns**
- Custom implementations may not follow Material 3 accessibility guidelines
- Touch targets, semantic descriptions, and screen reader support may be incomplete
- No standard focus management

#### 4. **Responsive Design Gaps**
- Fixed positioning doesn't adapt to different screen sizes
- No consideration for foldable devices or tablets
- NavigationRail should be used on larger screens per Material 3 guidelines

---

## Material 3 Design Guidelines

### Navigation Patterns

#### Bottom Navigation (NavigationBar)
- **Use Case:** 3-5 top-level destinations
- **Placement:** Fixed at bottom of screen
- **Behavior:** Tappable icons with optional labels
- **Elevation:** Automatically handles z-index with other components
- **Responsive:** Automatically switches to NavigationRail on larger screens

#### Navigation Rail
- **Use Case:** Tablets, foldables, desktop-sized screens
- **Placement:** Left side of screen
- **Behavior:** Vertical navigation with icons and labels
- **Responsive:** Adapts window size classes

#### Floating Action Button (FAB)
- **Use Case:** Primary action on screen
- **Placement:** Bottom-right corner (or center-bottom with NavigationBar)
- **Variants:** Standard, Extended, Large, Small
- **Behavior:** Can be anchored to NavigationBar or Scaffold

### Elevation & Z-Index Hierarchy (Material 3)

```
Highest (Top Layer)
├── Modal sheets (BottomSheet, Drawer)
├── Dialogs
├── NavigationBar (when present)
├── FAB
├── TopAppBar
└── Content (Lowest)
```

**Key Principle:** Modals (drawers, sheets) should always overlay navigation elements.

---

## Recommended Solution

### Option A: Material 3 NavigationBar with Standard FAB (Recommended)

#### Architecture
```
DashboardScreen
└── Scaffold
    ├── TopAppBar
    ├── HorizontalPager (content)
    ├── NavigationBar (bottom)
    └── FloatingActionButton (anchored to NavigationBar)
```

#### Implementation Details

**1. Replace NavigationPill with NavigationBar**
```kotlin
NavigationBar(
    modifier = Modifier.fillMaxWidth()
) {
    tabs.forEachIndexed { index, (icon, label) ->
        NavigationBarItem(
            selected = pagerState.currentPage == index,
            onClick = { scope.launch { pagerState.animateScrollToPage(index) } },
            icon = { Icon(icon, contentDescription = label) },
            label = { Text(label) }
        )
    }
}
```

**2. Replace AeonExpandableFab with Standard FAB**
```kotlin
Scaffold(
    bottomBar = { NavigationBar(...) },
    floatingActionButton = {
        FloatingActionButton(
            onClick = { onAddClick(0, null) }
        ) {
            Icon(Icons.Default.Add, contentDescription = "Add Transaction")
        }
    }
) { innerPadding ->
    // Content
}
```

**3. Handle FAB Expansion with BottomSheet**
For template shortcuts, use a standard BottomSheet instead of custom expansion:
```kotlin
val bottomSheetState = rememberBottomSheetScaffoldState(
    bottomSheetState = rememberStandardBottomSheetState(
        initialValue = SheetValue.Hidden
    )
)

BottomSheetScaffold(
    scaffoldState = bottomSheetState,
    sheetContent = {
        // Template selection sheet
    },
    floatingActionButton = {
        FloatingActionButton(
            onClick = { 
                scope.launch { bottomSheetState.bottomSheetState.expand() }
            }
        ) {
            Icon(Icons.Default.Add, contentDescription = "Add")
        }
    }
)
```

#### Pros
- ✅ Fully Material 3 compliant
- ✅ Automatic z-index handling (drawer always on top)
- ✅ Built-in accessibility support
- ✅ Standard animations and transitions
- ✅ Responsive design support
- ✅ Less custom code to maintain

#### Cons
- ❌ Loses custom pill aesthetic
- ❌ Template expansion becomes a sheet instead of inline menu
- ❌ Requires significant refactoring

---

### Option B: Keep Custom Elements with Proper Z-Index Management

#### Architecture
```
DashboardScreen
├── ModalNavigationDrawer
│   └── Scaffold
│       ├── TopAppBar
│       ├── HorizontalPager
│       └── NavigationPill (inside Scaffold, as bottomBar)
│   └── AeonExpandableFab (inside Scaffold, as FAB)
```

#### Implementation Details

**1. Move NavigationPill Inside Scaffold as BottomBar**
```kotlin
Scaffold(
    topBar = { TopAppBar(...) },
    bottomBar = {
        NavigationPill(
            pagerState = pagerState,
            tabs = tabs,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
        )
    }
) { innerPadding ->
    // Content
}
```

**2. Move FAB Inside Scaffold**
```kotlin
Scaffold(
    topBar = { TopAppBar(...) },
    bottomBar = { NavigationPill(...) },
    floatingActionButton = {
        AeonExpandableFab(
            onAddClick = onAddClick,
            templates = templates,
            fabPaddingBottom = 16.dp
        )
    }
) { innerPadding ->
    // Content
}
```

**3. Hide Elements When Drawer Opens**
```kotlin
val isDrawerOpen by drawerState.isOpen

Scaffold(
    // ...
) { innerPadding ->
    AnimatedVisibility(
        visible = !isDrawerOpen,
        enter = fadeIn(),
        exit = fadeOut()
    ) {
        // Content that should hide when drawer opens
    }
}
```

#### Pros
- ✅ Preserves custom aesthetic
- ✅ Minimal refactoring required
- ✅ Fixes z-index issue
- ✅ Maintains current UX patterns

#### Cons
- ❌ Still uses custom components
- ❌ Manual accessibility implementation required
- ❌ Not fully Material 3 compliant
- ❌ Requires manual responsive design handling

---

### Option C: Hybrid Approach (Best for Current State)

#### Architecture
```
DashboardScreen
└── ModalNavigationDrawer
    └── Scaffold
        ├── TopAppBar
        ├── HorizontalPager
        ├── NavigationBar (Material 3, with custom styling)
        └── Extended FAB with BottomSheet for templates
```

#### Implementation Details

**1. Use NavigationBar with Custom Styling**
```kotlin
NavigationBar(
    containerColor = MaterialTheme.colorScheme.surfaceVariant,
    tonalElevation = 8.dp
) {
    tabs.forEachIndexed { index, (icon, label) ->
        NavigationBarItem(
            selected = pagerState.currentPage == index,
            onClick = { scope.launch { pagerState.animateScrollToPage(index) } },
            icon = { Icon(icon, contentDescription = label) },
            label = { Text(label) },
            colors = NavigationBarItemDefaults.colors(
                selectedIconColor = MaterialTheme.colorScheme.onPrimary,
                selectedTextColor = MaterialTheme.colorScheme.onPrimary,
                indicatorColor = MaterialTheme.colorScheme.primary
            )
        )
    }
}
```

**2. Use Extended FAB with Template BottomSheet**
```kotlin
val bottomSheetState = rememberBottomSheetScaffoldState(
    bottomSheetState = rememberStandardBottomSheetState(
        initialValue = SheetValue.Hidden
    )
)

BottomSheetScaffold(
    scaffoldState = bottomSheetState,
    sheetContent = {
        LazyColumn {
            item {
                Text("New Transaction", style = MaterialTheme.typography.titleLarge)
            }
            items(templates) { template ->
                TemplateItem(
                    template = template,
                    onClick = { 
                        scope.launch { 
                            bottomSheetState.bottomSheetState.hide()
                            onAddClick(template.transactionType, template.name)
                        }
                    }
                )
            }
        }
    },
    floatingActionButton = {
        ExtendedFloatingActionButton(
            onClick = { 
                scope.launch { bottomSheetState.bottomSheetState.expand() }
            },
            icon = { Icon(Icons.Default.Add, contentDescription = null) },
            text = { Text("Add Transaction") },
            expanded = true
        )
    }
)
```

#### Pros
- ✅ Material 3 components with custom styling
- ✅ Preserves some custom aesthetic
- ✅ Proper z-index handling
- ✅ Built-in accessibility
- ✅ Template selection in standard BottomSheet

#### Cons
- ❌ Still requires refactoring
- ❌ Loses pill shape (but gains standard NavigationBar)

---

## Recommended Implementation Plan

### Phase 1: Immediate Fix (1-2 days)
**Goal:** Fix z-index issue with minimal changes

1. Move NavigationPill inside Scaffold as `bottomBar`
2. Move AeonExpandableFab inside Scaffold as `floatingActionButton`
3. Add animation to hide elements when drawer opens
4. Test on all screen sizes

**Files to Modify:**
- `DashboardScreen.kt`
- No changes to `NavigationPill.kt` or `AeonExpandableFab.kt`

---

### Phase 2: Material 3 Migration (3-5 days)
**Goal:** Replace custom components with Material 3 equivalents

1. Replace NavigationPill with NavigationBar
2. Replace AeonExpandableFab with standard FAB + BottomSheet
3. Update color schemes and styling
4. Add proper accessibility labels
5. Test accessibility with TalkBack

**Files to Modify:**
- `DashboardScreen.kt` (major refactor)
- Create new `TemplateSelectionSheet.kt`
- Deprecate `NavigationPill.kt` and `AeonExpandableFab.kt`

---

### Phase 3: Responsive Design (2-3 days)
**Goal:** Add NavigationRail for larger screens

1. Implement window size class detection
2. Add NavigationRail for tablets/foldables
3. Adaptive layout switching
4. Test on various screen sizes

**Files to Modify:**
- `DashboardScreen.kt`
- Create `AdaptiveNavigation.kt`

---

## Technical Specifications

### Window Size Classes

```kotlin
enum class WindowSizeClass {
    Compact,   // Mobile phones (<600dp)
    Medium,    // Foldables/Tablets in portrait (600-840dp)
    Expanded   // Tablets/Large screens (>840dp)
}

@Composable
fun rememberWindowSizeClass(): WindowSizeClass {
    val configuration = LocalConfiguration.current
    val windowWidth = configuration.screenWidthDp
    return when {
        windowWidth < 600 -> WindowSizeClass.Compact
        windowWidth < 840 -> WindowSizeClass.Medium
        else -> WindowSizeClass.Expanded
    }
}
```

### Adaptive Navigation Component

```kotlin
@Composable
fun AdaptiveNavigation(
    selectedTab: Int,
    onTabSelected: (Int) -> Unit,
    tabs: List<Pair<ImageVector, String>>,
    windowSizeClass: WindowSizeClass
) {
    when (windowSizeClass) {
        WindowSizeClass.Compact -> {
            NavigationBar {
                tabs.forEachIndexed { index, (icon, label) ->
                    NavigationBarItem(
                        selected = selectedTab == index,
                        onClick = { onTabSelected(index) },
                        icon = { Icon(icon, contentDescription = label) },
                        label = { Text(label) }
                    )
                }
            }
        }
        WindowSizeClass.Medium, WindowSizeClass.Expanded -> {
            NavigationRail {
                tabs.forEachIndexed { index, (icon, label) ->
                    NavigationRailItem(
                        selected = selectedTab == index,
                        onClick = { onTabSelected(index) },
                        icon = { Icon(icon, contentDescription = label) },
                        label = { Text(label) }
                    )
                }
            }
        }
    }
}
```

---

## Accessibility Requirements

### Semantics

```kotlin
NavigationBarItem(
    selected = pagerState.currentPage == index,
    onClick = { scope.launch { pagerState.animateScrollToPage(index) } },
    icon = { Icon(icon, contentDescription = label) },
    label = { Text(label) },
    modifier = Modifier.semantics {
        contentDescription = "$label tab"
        stateDescription = if (pagerState.currentPage == index) "Selected" else "Not selected"
        role = Role.Tab
    }
)
```

### Focus Management

```kotlin
val focusRequester = remember { FocusRequester() }

NavigationBarItem(
    // ...
    modifier = Modifier.focusRequester(focusRequester)
)

LaunchedEffect(pagerState.currentPage) {
    focusRequester.requestFocus()
}
```

---

## Testing Strategy

### Unit Tests
- [ ] Navigation state management
- [ ] FAB expansion/collapse logic
- [ ] Drawer state synchronization

### UI Tests
- [ ] NavigationPill/NavigationBar tab switching
- [ ] FAB click and template selection
- [ ] Drawer open/close with element visibility
- [ ] Accessibility with TalkBack

### Device Testing
- [ ] Mobile phones (various screen sizes)
- [ ] Foldable devices (unfolded/folded)
- [ ] Tablets (portrait/landscape)
- [ ] Dark/Light theme modes

---

## Migration Checklist

### Phase 1: Immediate Fix
- [ ] Move NavigationPill inside Scaffold
- [ ] Move FAB inside Scaffold
- [ ] Add drawer open/close animations
- [ ] Test drawer overlay behavior
- [ ] Verify no regression in functionality

### Phase 2: Material 3 Migration
- [ ] Implement NavigationBar
- [ ] Implement standard FAB
- [ ] Create TemplateSelectionSheet
- [ ] Update color schemes
- [ ] Add accessibility labels
- [ ] Test with TalkBack
- [ ] Deprecate old components

### Phase 3: Responsive Design
- [ ] Implement window size class detection
- [ ] Add NavigationRail
- [ ] Test adaptive layouts
- [ ] Update documentation

---

## Risks & Mitigation

### Risk 1: User Resistance to UI Changes
**Mitigation:** 
- Provide setting to opt-in to new design
- Gradual rollout with A/B testing
- Clear communication about improvements

### Risk 2: Regression in Functionality
**Mitigation:**
- Comprehensive testing before release
- Feature flags for gradual rollout
- Rollback plan if issues arise

### Risk 3: Performance Impact
**Mitigation:**
- Profile animations
- Optimize recomposition
- Use lazy loading for templates

---

## Success Metrics

### UX Metrics
- [ ] Drawer opens without UI conflicts
- [ ] Navigation elements follow Material 3 patterns
- [ ] Accessibility score >90%
- [ ] User satisfaction >4.5/5

### Technical Metrics
- [ ] Zero z-index related bugs
- [ ] Compose recomposition count reduced
- [ ] Code complexity reduced
- [ ] Test coverage >80%

---

## Conclusion

The recommended approach is **Option C (Hybrid)** as a phased implementation:
1. **Phase 1:** Fix immediate z-index issue (Option B)
2. **Phase 2:** Migrate to Material 3 components (Option A with custom styling)
3. **Phase 3:** Add responsive design (NavigationRail)

This approach balances immediate UX improvements with long-term maintainability and Material 3 compliance, while minimizing user disruption through gradual changes.

---

## References

- [Material 3 Navigation Components](https://m3.material.io/components/navigation-bar/overview)
- [Material 3 FAB Guidelines](https://m3.material.io/components/floating-action-button/overview)
- [Material 3 Elevation System](https://m3.material.io/styles/elevation/overview)
- [Compose Window Size Classes](https://developer.android.com/jetpack/compose/layouts/adaptive)
- [Material 3 Accessibility](https://m3.material.io/foundations/accessible-design/overview)
