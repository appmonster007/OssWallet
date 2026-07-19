---
baseline_commit: 001f864b331b5b878ff15f6526de8b4a7d579dce
---

# Story 3-4: Transaction List Fast Scroll Scrollbar

## Status: review

## Story

**As a** user with many transactions,
**I want** a fast-scroll scrollbar on transaction lists and the dashboard glance tab,
**so that** I can quickly jump to any point in a long list without endless swiping.

## Background

A partial implementation already exists on branch `feature/tagv46`. The library (`LazyColumnScrollbar` v2.2.0 via JitPack) is already added and wired up. This story is a **fix + complete** task — correct the bugs in the existing implementation and fill the one missing wire-up (dashboard Recent Activities tab ignoring the side setting).

Do **not** re-do what already works. Only change what is listed under "What to fix / complete".

---

## Acceptance Criteria

1. A scrollbar thumb appears on the Glance tab (card list), the Recent Activities tab, and the full Records screen when the user scrolls or drags the thumb. It fades out when idle.
2. On the Recent Activities tab and Records screen, a floating label shows the **group subtitle** (e.g. "Jun 23, 2026" for BY_DAY, "Jun 22 - Jun 28, 2026" for BY_WEEK) while the thumb is being dragged.
3. The label correctly reflects the group the user is currently scrolled into — it changes only when crossing a group boundary.
4. A haptic pulse fires once per group boundary crossed while dragging (not on every recompose).
5. The Glance tab scrollbar shows **no label indicator** — plain thumb only.
6. The fast-scroll side (LEFT / RIGHT) setting in the Records screen top-bar settings panel applies to all three locations.
7. The existing "Fast Scroll Side" toggle in `TransactionListTopAppBar` continues to work and persists via `TransactionSectionSettings.fastScrollSide`.

---

## What to Fix / Complete

### 1. Fix `indexToTitle` in `AeonTransactionList`

**File:** `app/src/main/java/com/osscabinet/ossWallet/ui/components/transactions/AeonTransactionComponents.kt`

The current implementation re-formats raw timestamps with a `DateTimeFormatter` to produce the label. This is wrong for two reasons:
- `BY_WEEK` groups have no meaningful single-item date — the week range is already in `group.headerData.subtitle`
- Year is required (list may span multiple years) — `subtitle` already contains it; `title` does not

**Replace** the entire `indexToTitle` remember block with:

```kotlin
val indexToTitle = remember(listState.groups) {
    buildMap<Int, String> {
        var i = 0
        listState.groups.forEach { group ->
            val label = group.headerData.subtitle  // e.g. "Jun 23, 2026" or "Jun 22 - Jun 28, 2026"
            put(i++, label)                        // the header row (stickyHeader or item)
            repeat(group.items.size) { put(i++, label) }  // all transaction rows in this group
        }
        if (i > 0) put(i, this[i - 1] ?: "")    // footer divider item
    }
}
```

Remove the `java.time.format.DateTimeFormatter` and `java.time.ZoneId` imports that were only used in the old block.

### 2. Fix haptic feedback — fire only on group boundary cross

**File:** same as above, inside `indicatorContent`

The current code puts a `LaunchedEffect(title)` inside `indicatorContent`. `indicatorContent` recomposes on every frame while dragging, so the effect fires far too often.

**Replace** with a remembered previous-label tracker:

```kotlin
indicatorContent = { index: Int, isThumbSelected: Boolean ->
    if (isThumbSelected) {
        val haptic = LocalHapticFeedback.current
        val title = indexToTitle[index] ?: ""
        val prevTitle = remember { mutableStateOf("") }

        if (title != prevTitle.value && title.isNotEmpty()) {
            prevTitle.value = title
            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
        }

        if (title.isNotEmpty()) {
            Box(
                modifier = Modifier
                    .padding(
                        start = if (fastScrollSide == FastScrollSide.LEFT) 36.dp else 16.dp,
                        end = if (fastScrollSide == FastScrollSide.RIGHT) 36.dp else 16.dp
                    )
                    .background(AppColors.primary, RoundedCornerShape(24.dp))
                    .padding(horizontal = 14.dp, vertical = 6.dp)
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.labelMedium.copy(
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp
                    ),
                    color = Color.White,
                    maxLines = 1
                )
            }
        }
    }
}
```

Note: the side-aware padding on the indicator label is already correct in your implementation — keep it.

### 3. Wire Recent Activities tab to respect `fastScrollSide` setting

**File:** `app/src/main/java/com/osscabinet/ossWallet/ui/screens/dashboard/DashboardScreen.kt`

The `DashboardRecentActivitiesTab` composable currently hardcodes `fastScrollSide = FastScrollSide.RIGHT`. It must read from `recordsSettings`.

Locate `DashboardRecentActivitiesTab` and its call site in `DashboardContent`. Thread `recordsSettings.fastScrollSide` down and pass it through:

- `DashboardContent` already receives `recordsSettings` (or the full display settings) — find the parameter and pass `recordsSettings.fastScrollSide` to `DashboardRecentActivitiesTab`
- Inside `DashboardRecentActivitiesTab`, replace the hardcoded `FastScrollSide.RIGHT` with the parameter

The `DashboardGlanceTab` (`AeonCardList`) has no side parameter — leave it as-is (always right, no indicator).

### 4. Verify `ScrollbarSettings` fade behaviour

**File:** `AeonTransactionComponents.kt` and `AeonCardList.kt`

The library default for `alwaysShowScrollbar` is `false`, which means the thumb fades in on scroll and out when idle. Verify this is not being overridden. If `alwaysShowScrollbar` is not explicitly set in your `ScrollbarSettings.Default.copy(...)` calls, it defaults to the library's behaviour — that is correct and no change is needed.

If you see `alwaysShowScrollbar = true` anywhere, remove it.

---

## What NOT to change

- The `FastScrollSide` enum in `DisplaySettings.kt` — correct as-is
- The `fastScrollSide` field in `TransactionSectionSettings` — correct as-is
- The "Fast Scroll Side" toggle in `TransactionListTopAppBar` — correct as-is
- The library version and JitPack repository declaration — correct as-is
- The `AeonCardList` scrollbar settings (plain thumb, no indicator) — correct as-is
- The `contentType` additions to `stickyHeader` / `itemsIndexed` / footer — keep them
- Parameter threading through `RecentActivitiesScreen` and `AeonTransactionList` — correct as-is
- The `lazyListState` hoisting in `DashboardScreenLayout` — correct as-is

---

## Key Files

| File | Change |
|---|---|
| `ui/components/transactions/AeonTransactionComponents.kt` | Fix `indexToTitle` + fix haptic |
| `ui/screens/dashboard/DashboardScreen.kt` | Thread `fastScrollSide` to `DashboardRecentActivitiesTab` |

All other modified files in the branch diff are already correct and need no further changes.

---

## Dev Notes

- `group.headerData.subtitle` format is owned by `TimelineService.formatGroupSubtitle()`. Do not re-format it in the UI layer.
  - BY_DAY example: `"Jun 23, 2026"`
  - BY_WEEK example: `"Jun 22 - Jun 28, 2026"`
- The flat index counting in `indexToTitle` must match the exact item order in the `LazyColumn`: header row first, then all `itemsIndexed` rows for that group, then the next group's header, etc. The footer `item("footer_divider")` is the very last index.
- `LazyColumnScrollbar`'s `indicatorContent` lambda runs inside composition — side effects (haptic) must not be triggered from the lambda body directly. Use the `mutableStateOf` + comparison pattern shown above (not `LaunchedEffect`) since `LaunchedEffect` in this context reruns too aggressively.
- The `modifier` on the outer `LazyColumnScrollbar` in `AeonCardList` does not carry `reorderable` — that stays on the inner `LazyColumn`. This is already correct in the current implementation.
