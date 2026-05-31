# Glassmorphism Implementation Guide

This document captures every detail needed to apply the frosted-glass effect to new components in OssWallet.

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

## `GlassSurface` — The Canonical Shell (`ui/components/GlassSurface.kt`)

All glass surfaces in OssWallet are built on this single composable. **Never write the raw two-sibling pattern inline.** Call `GlassSurface` instead.

```kotlin
@Composable
fun GlassSurface(
    modifier: Modifier = Modifier,
    shape: Shape,
    hazeState: HazeState?,             // pass the relevant CompositionLocal
    hazeStyle: HazeStyle = GlassTokens.style(),
    fallbackColor: Color = AppColors.cardSurfaceProminent,
    shadowElevation: Dp = 0.dp,
    ambientShadowAlpha: Float = 0.06f,
    spotShadowAlpha: Float = 0.12f,
    borderColor: Color? = null,        // null = no border
    content: @Composable BoxScope.() -> Unit
)
```

Key behaviours:

- **Always renders `fallbackColor`** as the surface background, so the component degrades gracefully in Previews and on screens that haven't wired up a hazeSource.
- **Shadow on its own `RenderNode`** — `hazeEffect` installs a `RenderEffect` that silently drops `shadowElevation` when both share the same `graphicsLayer`. The shadow `Spacer` is added automatically only when `glassEnabled && shadowElevation > 0.dp`.
- **Border gated on `glassEnabled && hazeState != null`** — avoids rendering glass chrome (white border) when the blur effect is absent.

### Component-to-`GlassSurface` mapping

| Component | `hazeState` source | `hazeStyle` | `shadowElevation` |
|-----------|-------------------|-------------|-------------------|
| `AppDrawerContent` | `LocalHazeState.current` | `GlassTokens.drawerStyle()` | 16 dp |
| `PillNavigationBar` | `LocalPillHazeState.current` | `GlassTokens.pillStyle()` | 8 dp |
| `TransactionHeader` | `LocalHazeState.current` | `GlassTokens.style()` | 12 dp |
| `AeonExpressiveCard` | `LocalCardHazeState.current` | `GlassTokens.cardStyle()` | 0 dp |

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
                ItemRow(item)
            }
        }
    }
}
```

**Critical:** `hazeSource` must wrap the _scrolling content_, not the entire `LazyColumn`. The sticky header should NOT be the source — it is the effect recipient.

### 2. Sticky header — use `GlassSurface`

```kotlin
@Composable
fun GlassHeader(…) {
    val hazeState = LocalHazeState.current
    val isDark = isSystemInDarkTheme()

    GlassSurface(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
        shape = RoundedCornerShape(12.dp),
        hazeState = hazeState,
        hazeStyle = GlassTokens.style(),
        fallbackColor = AppColors.cardSurfaceProminent,
        shadowElevation = 12.dp,
        borderColor = if (isDark) Color.White.copy(alpha = 0.12f) else Color.White.copy(alpha = 0.6f)
    ) {
        // header content
    }
}
```

#### Why the shadow needs a sibling RenderNode (historical context)

`hazeEffect` installs a `RenderEffect` on its `graphicsLayer`. A `RenderEffect` processes only pixels within that node and silently drops `shadowElevation` set on the same node. `GlassSurface` encodes the fix: shadow goes on a sibling `Spacer` with its own `graphicsLayer { clip = false }` behind the blur node.

---

## Pattern: Overlay Drawer (ModalNavigationDrawer)

The drawer slides over the main app content, so the entire `NavHost` is the source and the drawer sheet is the effect recipient. The `HazeState` is provided at the **navigation graph level**.

### 1. NavGraph host — create `HazeState`, wrap NavHost with `hazeSource`

In `OssWalletNavGraph.kt`:
```kotlin
val drawerHazeState = remember { HazeState() }

CompositionLocalProvider(LocalHazeState provides drawerHazeState) {
    ModalNavigationDrawer(
        drawerContent = { AppDrawerContent(…) }
    ) {
        Box(modifier = Modifier.fillMaxSize().hazeSource(drawerHazeState)) {
            NavHost(…)
        }
    }
}
```

**Important:** Name this state something specific (`drawerHazeState`) to avoid confusion — screens inside the `NavHost` create their own `HazeState` for list headers. `CompositionLocalProvider` scoping handles the override correctly.

### 2. Drawer content — use `GlassSurface`

`ModalDrawerSheet` does not support `hazeEffect` directly — its `Surface` owns the clip shape. `AppDrawerContent` replaces it with `GlassSurface`:

```kotlin
@Composable
fun AppDrawerContent(…) {
    val hazeState = LocalHazeState.current
    val isDark = isSystemInDarkTheme()
    val drawerShape = RoundedCornerShape(topEnd = …, bottomEnd = …)

    GlassSurface(
        modifier = Modifier.width(280.dp).fillMaxHeight(),
        shape = drawerShape,
        hazeState = hazeState,
        hazeStyle = GlassTokens.drawerStyle(),
        fallbackColor = AppColors.drawerSurface,
        shadowElevation = 16.dp,
        ambientShadowAlpha = 0.08f,
        spotShadowAlpha = 0.18f,
        borderColor = if (isDark) Color.White.copy(alpha = 0.08f) else Color.White.copy(alpha = 0.50f)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .windowInsetsPadding(DrawerDefaults.windowInsets)
        ) {
            // … drawer content
        }
    }
}
```

**Why `DrawerDefaults.windowInsets`?** `ModalDrawerSheet` applied window insets internally via its `Surface`. After replacing it, the Column must call `.windowInsetsPadding(DrawerDefaults.windowInsets)` explicitly.

---

## Pattern: Floating Card with Glass Background

For non-scrolling glass cards (e.g., dashboard cards over a background):

1. The _parent_ composable marks its background with `hazeSource(hazeState)`.
2. The card passes `LocalCardHazeState.current` to `GlassSurface`.
3. `GlassSurface` always renders `fallbackColor` first, so the card looks correct on screens that haven't wired up a hazeSource.

---

## Applying Glass to a New Component — Checklist

1. **Decide the blur source.** What content should bleed through the glass? That content gets `.hazeSource(hazeState)`.
2. **Create `HazeState` in the nearest common ancestor.** Provide via `CompositionLocalProvider` using the appropriate `Local*HazeState`.
3. **Call `GlassSurface`** in the glass composable — pass the relevant `Local*HazeState.current`, choose a `GlassTokens.*Style()`, and set `borderColor` for a white-alpha rim.
4. **Do not write the two-sibling pattern inline.** `GlassSurface` encodes it correctly and prevents future drift.

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
