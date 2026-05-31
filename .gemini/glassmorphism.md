# Glassmorphism Implementation Guide

This document captures every detail needed to apply the frosted-glass effect to new components in OssWallet, derived from the implementation in `TransactionHeader`.

---

## Overview

The effect is powered by **Haze** (`dev.chrisbanes:haze`). Haze works by capturing a snapshot of the content rendered _behind_ a surface, blurring it, and tinting it. This requires two roles in the composition tree:

| Role | Modifier | What it does |
|------|----------|--------------|
| **Source** | `hazeSource(hazeState)` | Marks content whose pixels will be sampled for blurring |
| **Effect** | `hazeEffect(hazeState, style)` | Applies the blur + tint to this composable's background |

Source and effect must share the same `HazeState` instance.

---

## Design Tokens (`ui/theme/GlassTokens.kt`)

```kotlin
object GlassTokens {
    val BlurRadius = 8.dp
    val DrawerBlurRadius = 16.dp
    val NoiseFactor = 0f       // Keep at 0 for a clean modern look

    // Narrow overlay surfaces (sticky headers over list rows).
    @Composable
    fun style(): HazeStyle { … }

    // Large overlay surfaces (drawer, full-screen sheets).
    @Composable
    fun drawerStyle(): HazeStyle { … }
}
```

**Never hardcode blur values inline.** Always use `GlassTokens` so tuning is centralised.

The tint derives from `AppColors.cardSurfaceProminent` (Material 3 `surfaceContainerHigh`) rather than the raw screen background. This makes the glass look _elevated_ rather than transparent, which is critical on both light and dark themes.

### Choosing between `style()` and `drawerStyle()`

| Variant | Blur radius | Dark tint α | Light tint α | Use when |
|---------|-------------|-------------|--------------|----------|
| `style()` | 8 dp | 0.45 | 0.30 | Narrow strips over neutral-coloured list content (transaction headers) |
| `drawerStyle()` | 16 dp | 0.82 | 0.55 | Large surfaces over the full app (drawer, full-screen modals) |

**Why large surfaces need heavier values:** The drawer blurs the entire visible app — colourful dashboard cards, charts, account colours — producing a vivid backdrop. In dark mode, `onSurface` text is near-white; if the blurred background is too bright, contrast collapses. Raising the tint alpha to `0.82f` pushes the surface toward the dark `surfaceContainerHigh` tone so text stays readable. The wider `16 dp` blur radius averages out colour hotspots so the background is neutrally tinted rather than patchy.

---

## CompositionLocals

Two CompositionLocals are defined in `GlassTokens.kt`:

```kotlin
val LocalHazeState = staticCompositionLocalOf<HazeState?> { null }
val LocalGlassmorphismEnabled = staticCompositionLocalOf { true }
```

- `LocalHazeState` carries the `HazeState` created by the list/scroll host down to leaf composables that apply `hazeEffect`.
- `LocalGlassmorphismEnabled` carries the user's on/off preference from `MainActivity` to every composable in the tree. When `false`, skip all haze modifiers.

`LocalGlassmorphismEnabled` is provided in `MainActivity.setContent` by reading `settingsRepository.glassmorphismEnabled`:

```kotlin
val glassmorphismEnabled by settingsRepository.glassmorphismEnabled
    .collectAsStateWithLifecycle(initialValue = true)

OssWalletTheme(...) {
    CompositionLocalProvider(LocalGlassmorphismEnabled provides glassmorphismEnabled) {
        // entire app tree
    }
}
```

---

## Pattern: Scrollable List with Sticky Glass Header

This is the pattern used in `AeonTransactionList` + `TransactionHeader`.

### 1. List host — create and provide `HazeState`

```kotlin
val hazeState = remember { HazeState() }
CompositionLocalProvider(LocalHazeState provides hazeState) {
    LazyColumn(…) {
        stickyHeader {
            GlassHeader(…)          // reads LocalHazeState internally
        }
        itemsIndexed(items) { _, item ->
            Box(modifier = Modifier.hazeSource(hazeState)) {
                ItemRow(item)        // content that gets blurred behind the header
            }
        }
    }
}
```

**Critical:** `hazeSource` must wrap the _scrolling content_, not the entire `LazyColumn`. The sticky header composable should NOT be marked as the source — it is the effect recipient.

### 2. Sticky header — apply `hazeEffect`

```kotlin
@Composable
fun GlassHeader(…) {
    val hazeState = LocalHazeState.current
    val glassEnabled = LocalGlassmorphismEnabled.current
    val glassShape = RoundedCornerShape(12.dp)
    val glassStyle = GlassTokens.style()

    Box(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp)) {

        // Shadow lives on a separate sibling RenderNode because hazeEffect's internal
        // RenderEffect silently drops shadowElevation on the same graphicsLayer.
        if (glassEnabled) {
            Spacer(
                modifier = Modifier
                    .matchParentSize()
                    .graphicsLayer {
                        shape = glassShape
                        clip = false
                        shadowElevation = 12.dp.toPx()
                        ambientShadowColor = Color.Black.copy(alpha = 0.06f)
                        spotShadowColor = Color.Black.copy(alpha = 0.12f)
                    }
            )
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .graphicsLayer { shape = glassShape; clip = true }
                .background(AppColors.cardSurfaceProminent, glassShape)
                .then(
                    if (glassEnabled && hazeState != null)
                        Modifier.hazeEffect(hazeState, glassStyle)
                    else Modifier
                )
                .border(
                    width = 0.5.dp,
                    color = if (isDark) Color.White.copy(alpha = 0.12f)
                            else Color.White.copy(alpha = 0.6f),
                    shape = glassShape
                )
        ) {
            // header content
        }
    }
}
```

#### Why two nested `Box` composables?

`hazeEffect` installs a `RenderEffect` on the inner `graphicsLayer`. A `RenderEffect` processes only pixels within that node — it cannot composite with a shadow on the same node. The outer `Spacer` places the shadow on a completely independent `RenderNode` that draws behind the glass surface, so both are visible simultaneously.

---

## Pattern: Overlay Drawer (ModalNavigationDrawer)

The drawer slides over the main app content, so the entire `NavHost` is the source and the drawer sheet is the effect recipient. Unlike the sticky header pattern, the `HazeState` must be provided at the **navigation graph level** so the drawer's composable subtree can access it.

### 1. NavGraph host — create `HazeState`, wrap NavHost with `hazeSource`

In `OssWalletNavGraph.kt`:
```kotlin
val drawerHazeState = remember { HazeState() }

CompositionLocalProvider(LocalHazeState provides drawerHazeState) {
    ModalNavigationDrawer(
        drawerContent = { AppDrawerContent(…) }  // reads LocalHazeState internally
    ) {
        Box(modifier = Modifier.fillMaxSize().hazeSource(drawerHazeState)) {
            NavHost(…)
        }
        BackHandler(…)
    }
}
```

**Important:** Name this state something specific (`drawerHazeState`) to avoid confusion — screens inside the `NavHost` create their own `HazeState` for list headers. Compose's `CompositionLocalProvider` scoping handles the override correctly: screens provide their own `LocalHazeState` locally, which takes precedence over the nav-graph-level one within their subtree.

### 2. Drawer content — replace `ModalDrawerSheet` with custom glass container

`ModalDrawerSheet` does not support `hazeEffect` directly — its `Surface` owns the clip shape. Replacing it with `Box(width=280.dp, fillMaxHeight)` gives full control. Window insets are handled via `DrawerDefaults.windowInsets`:

```kotlin
@Composable
fun AppDrawerContent(…) {
    val hazeState = LocalHazeState.current
    val glassEnabled = LocalGlassmorphismEnabled.current
    val isDark = isSystemInDarkTheme()
    val drawerShape = RoundedCornerShape(topEnd = …, bottomEnd = …)

    Box(modifier = Modifier.width(280.dp).fillMaxHeight()) {

        // Shadow on its own RenderNode (same rule as the sticky header)
        if (glassEnabled) {
            Spacer(
                modifier = Modifier.matchParentSize().graphicsLayer {
                    shape = drawerShape; clip = false
                    shadowElevation = 16.dp.toPx()
                    ambientShadowColor = Color.Black.copy(alpha = 0.08f)
                    spotShadowColor = Color.Black.copy(alpha = 0.18f)
                }
            )
        }

        Box(
            modifier = Modifier
                .matchParentSize()
                .graphicsLayer { shape = drawerShape; clip = true }
                .background(AppColors.drawerSurface, drawerShape)
                .then(
                    if (glassEnabled && hazeState != null)
                        Modifier.hazeEffect(hazeState, GlassTokens.style())
                    else Modifier
                )
                .border(0.5.dp, borderColor, drawerShape)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .windowInsetsPadding(DrawerDefaults.windowInsets)
            ) {
                // … drawer content unchanged
            }
        }
    }
}
```

**Why `DrawerDefaults.windowInsets`?** `ModalDrawerSheet` applied window insets internally via its `Surface`. After replacing it, the Column must call `.windowInsetsPadding(DrawerDefaults.windowInsets)` explicitly — otherwise the profile header overlaps the status bar on edge-to-edge displays.

---

## Pattern: Floating Card with Glass Background

For non-scrolling glass cards (e.g., a modal card floating over a background):

1. The _parent_ composable that owns the blurred background marks itself (or its children) with `hazeSource(hazeState)`.
2. The floating card reads `LocalHazeState.current` and applies `hazeEffect`.
3. If there is no ambient `LocalHazeState` (i.e. the card is used in a screen that hasn't set one up), the effect silently degrades to the solid `cardSurfaceProminent` background because of the `.background(…)` call before `.hazeEffect(…)`.

This graceful degradation is **intentional** — always apply the base background colour before `hazeEffect` so the component looks correct with or without Haze.

---

## Applying Glass to a New Component — Checklist

1. **Decide the blur source.** What content should bleed through the glass? That content gets `.hazeSource(hazeState)`.
2. **Create `HazeState` in the nearest common ancestor** that owns both source and effect. Provide it via `CompositionLocalProvider(LocalHazeState provides hazeState)`.
3. **In the glass composable**, read both locals:
   ```kotlin
   val hazeState = LocalHazeState.current
   val glassEnabled = LocalGlassmorphismEnabled.current
   ```
4. **Apply the modifier stack in order:**
   ```kotlin
   Modifier
       .graphicsLayer { shape = yourShape; clip = true }
       .background(AppColors.cardSurfaceProminent, yourShape)   // fallback always present
       .then(
           if (glassEnabled && hazeState != null)
               Modifier.hazeEffect(hazeState, GlassTokens.style())
           else Modifier
       )
       .border(0.5.dp, borderColor, yourShape)
   ```
5. **If you need an elevation shadow**, add a sibling `Spacer` with its own `graphicsLayer { shadowElevation = … }` — do NOT put it on the same `graphicsLayer` as the `hazeEffect`.
6. **Gate on `glassEnabled`** everywhere (shadow spacer, hazeEffect, and hazeSource are all unnecessary when disabled).

---

## Settings Wire-Up

The user toggle is stored in DataStore under key `glassmorphism_enabled` (default `true`).

| Layer | File | Symbol |
|-------|------|--------|
| DataStore key | `SettingsDataStore` | `GLASSMORPHISM_ENABLED_KEY` |
| DataStore flow | `SettingsDataStore` | `glassmorphismEnabled: Flow<Boolean>` |
| Repository interface | `SettingsRepository` | `glassmorphismEnabled`, `saveGlassmorphismEnabled` |
| Repository impl | `SettingsRepositoryImpl` | delegates to DataStore |
| ViewModel | `SettingsViewModel` | `glassmorphismEnabled: StateFlow<Boolean>`, `setGlassmorphismEnabled()` |
| UI provider | `MainActivity` | `CompositionLocalProvider(LocalGlassmorphismEnabled provides …)` |
| UI toggle | `DisplaySettingsScreen` — "APPEARANCE" section | Switch item "Glass Effect" |

When adding a new glassmorphism component, no changes to this chain are needed — the preference is already propagated globally via `LocalGlassmorphismEnabled`.

---

## Common Pitfalls

| Pitfall | Cause | Fix |
|---------|-------|-----|
| Blur renders as solid white/black rectangle | `hazeSource` is on a composable drawn _after_ (on top of) the effect recipient | Ensure source content is drawn _below_ the glass in the Z-order; in a `LazyColumn` the items must wrap with `hazeSource` |
| Shadow is invisible when glass is on | Both shadow and `hazeEffect` share the same `graphicsLayer` node | Put shadow in a sibling `Spacer` with its own `graphicsLayer` |
| Glass stops working after navigating away | `HazeState` was created outside `remember` | Always `remember { HazeState() }` at the composition scope that owns the list |
| Effect not recomposing when toggle changes | `LocalGlassmorphismEnabled` not read inside the composable | Read it inside the `@Composable` body, not in a helper function |
| Preview crashes | No `LocalHazeState` provided in preview | The null default in `staticCompositionLocalOf<HazeState?> { null }` and the `if (hazeState != null)` guard handle this — previews degrade to solid background automatically |

---

## Haze Library Version Notes

- The library migrated from `.hazeChild()` to `.hazeEffect()` in a breaking API change. Always use `.hazeEffect()`.
- `HazeTint` takes a `Color`, not a `Brush`. Avoid `HazeTint(brush = …)` unless intentionally painting a gradient frost.
- `NoiseFactor` adds film-grain texture. Keep at `0f` for the clean Aeon aesthetic.
