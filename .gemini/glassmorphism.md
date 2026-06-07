# Glassmorphism Implementation Guide

This document covers every detail needed to apply, extend, or debug the frosted-glass effect in OssWallet.

---

## The Core Constraint

The effect is powered by **Haze** (`dev.chrisbanes:haze`). Haze captures pixels behind a surface, blurs them, and tints them. This requires **two roles** that must be **layout siblings in the composition tree** — never parent/child:

| Role | Modifier | Responsibility |
|------|----------|---------------|
| **Source** | `hazeSource(state)` | Captures pixels of the content rendered at this node |
| **Effect** | `hazeEffect(state, style)` | Applies blur + tint at this node's background |

Both share the same `HazeState` instance. The source must be drawn **below** the effect in Z-order.

Because children can never register as siblings of their own parent, you cannot simply pass a `HazeState` down to a child composable and say "blur me." The architecture below solves this at every depth level.

---

## Three-Tier Architecture

```
┌─────────────────────────────────────────────┐
│  AeonHazeProvider                            │  ← Tier 1: screen background capture
│  (screens, dialogs, scaffolds)               │    Registers 3 HazeState sources for
│    Box.hazeSource(Surface)                   │    Surface / Overlay / Drawer depths.
│    Box.hazeSource(Overlay)                   │    Provides LocalAeonHaze to the tree.
│    Box.hazeSource(Drawer)                    │
│                                              │
│  ┌───────────────────────────────────────┐   │
│  │  GlassSurface                         │   │  ← Tier 3: pure consumer (hazeEffect only)
│  │  Resolves HazeState from priority     │   │    Resolves which source to blur via
│  │  chain, never registers as source.    │   │    the 3-step priority chain below.
│  │                                       │   │
│  │  ┌─────────────────────────────────┐  │   │
│  │  │  AeonGlassHost                  │  │   │  ← Tier 2: component-level capture
│  │  │  (cards, sheets, form sections) │  │   │    Captures ONE rendered glass surface.
│  │  │  Spacer.hazeSource(innerState)  │  │   │    Provides LocalParentGlassHaze for
│  │  │  LocalParentGlassHaze = inner   │  │   │    nested GlassSurface children to blur.
│  │  │                                 │  │   │
│  │  │  GlassSurface (child)           │  │   │
│  │  │  → blurs parent surface ✓       │  │   │
│  │  └─────────────────────────────────┘  │   │
│  └───────────────────────────────────────┘   │
└─────────────────────────────────────────────┘
```

**Why two separate tiers for sources?** `AeonHazeProvider` captures multiple simultaneous depth levels (screen background, overlay, drawer) from a single `Box`. `AeonGlassHost` captures one glass surface so its children can blur it. The two infrastructures are independent and composable.

---

## Tier 1: `AeonHazeProvider`

**File:** `ui/components/GlassSurface.kt`

Provides the screen background as a blur source for all glass surfaces on a screen.

```kotlin
@Composable
fun AeonHazeProvider(
    backgroundColor: Color = AppColors.screenBackground,
    drawContours: Boolean = true,
    content: @Composable (AeonHazeLayers) -> Unit
)
```

Internally it creates one `Box` that simultaneously registers as `hazeSource` for three `HazeState` instances (Surface, Overlay, Drawer), then renders `backgroundColor` + `ContourBackground()` inside it. It provides `LocalAeonHaze` to the tree.

**When to wrap a screen in `AeonHazeProvider`:**
- Any full screen composable (`*Screen`)
- Full-screen dialogs and scaffolds that render their own background
- Anything that creates a new visual rendering context

**When NOT to add another `AeonHazeProvider`:**
- Cards, list rows, detail sections, bottom sheet content — these are content inside an existing screen, not new screens. `AeonHazeProvider` would paint a second background over the first and break the blur.

---

## Tier 2: `AeonGlassHost`

**File:** `ui/components/GlassSurface.kt`

Captures ONE rendered glass surface and provides it as `LocalParentGlassHaze` for all descendant `GlassSurface` composables to blur.

```kotlin
@Composable
fun AeonGlassHost(
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit,
)
```

**Layout structure (critical):**

```kotlin
// CompositionLocalProvider is outside the Box — not a layout node.
// content() receives BoxScope from the Box and sees the surfaceHaze local.
CompositionLocalProvider(
    LocalParentGlassHaze provides surfaceHaze,
    LocalHazeDepth provides LocalHazeDepth.current + 1,
) {
    Box(modifier = modifier) {
        Spacer(Modifier.matchParentSize().hazeSource(surfaceHaze))  // sibling A
        content()                                                   // sibling B
    }
}
```

The `Spacer` and `content()` are siblings inside the Box — satisfying Haze's constraint. `CompositionLocalProvider` emits no layout node, so it does not create an extra nesting level.

**When to wrap content in `AeonGlassHost`:**
- Any glass container whose children will render `GlassSurface` composables that should blur it
- `AeonExpressiveCard` already does this internally — you do not need to add it again
- `EntrySection` and `AeonGlassSheet` already do this internally

**Nesting is unlimited.** Each `AeonGlassHost` shadows `LocalParentGlassHaze` for its subtree. Reading `LocalParentGlassHaze.current` always gives the **nearest** enclosing glass surface:

```
Card → AeonGlassHost (LocalParentGlassHaze = cardHaze)
  Row → AeonGlassHost (LocalParentGlassHaze = rowHaze)
    Badge → GlassSurface (blurs rowHaze ✓)
```

---

## Tier 3: `GlassSurface`

**File:** `ui/components/GlassSurface.kt`

The canonical glass surface shell. Pure `hazeEffect` consumer — never registers as `hazeSource`. All glass surfaces in OssWallet must use this composable.

```kotlin
@Composable
fun GlassSurface(
    modifier: Modifier = Modifier,
    shape: Shape,
    hazeState: HazeState? = null,        // optional explicit override
    hazeLayer: Int? = null,              // HazeLayer.Surface/Overlay/Drawer
    hazeStyle: HazeStyle = GlassTokens.style(),
    glassVariant: String? = null,        // GlassVariant.* constant
    fallbackColor: Color = AppColors.cardSurfaceProminent,
    shadowElevation: Dp? = null,
    borderColor: Color? = null,
    borderAlpha: Float? = null,
    borderWidth: Dp? = null,
    content: @Composable BoxScope.() -> Unit
)
```

### Priority Chain

`GlassSurface` resolves which `HazeState` to blur using this three-step chain:

```
1. explicit hazeState parameter          (caller override — e.g. AppDrawer, DesignLab)
       ↓ null?
2. LocalParentGlassHaze.current          (nearest AeonGlassHost ancestor — glass-on-glass)
       ↓ null? (outside any AeonGlassHost)
3. LocalAeonHaze.current?.states[depth]  (screen background — AeonHazeProvider)
```

Level 2 is `null` outside any `AeonGlassHost`, so screen-level cards (no host ancestor) automatically fall through to level 3 and blur the screen background. No configuration required.

### Fallback Behaviour

When glassmorphism is disabled (`LocalGlassmorphismEnabled = false`) or no `HazeState` is resolvable (e.g., in a `@Preview`), `GlassSurface` renders `fallbackColor` as a solid background. Every component degrades gracefully without code changes.

---

## Design Tokens (`ui/theme/GlassTokens.kt`)

### Style Factories

Always use a `GlassTokens` style factory — never hardcode blur values inline.

| Factory | Used by | Blur | Dark α | Light α |
|---------|---------|------|--------|---------|
| `style()` | Narrow overlays, sticky list headers | 8 dp | 0.45 | 0.30 |
| `cardStyle()` | Dashboard and analytics cards | 12 dp | 0.40 | 0.25 |
| `pillStyle()` | Nav pill, top bar pills, FAB chips | 10 dp | 0.50 | 0.35 |
| `sheetStyle()` | Bottom sheets, modals | 14 dp | 0.55 | 0.40 |
| `drawerStyle()` | Navigation drawer | 16 dp | 0.82 | 0.55 |
| `popupStyle()` | Tooltips, FAB menu label chips | 8 dp | 0.60 | 0.45 |

The tint color is derived from the relevant `AppColors.*Surface` token, not the raw screen background. This makes the glass look elevated rather than transparent — critical for readability in both light and dark themes.

### `GlassVariant` Constants

Pass via `glassVariant = GlassVariant.*` to `GlassSurface` for automatic style and token resolution:

```kotlin
object GlassVariant {
    const val Standard = "standard"
    const val Card     = "card"
    const val Pill     = "pill"
    const val Drawer   = "drawer"
    const val Sheet    = "sheet"
    const val Popup    = "popup"
}
```

### `HazeLayer` Constants

Semantic depth levels for screen-level glass resolution (tier 3 fallback):

```kotlin
object HazeLayer {
    const val Surface = 0   // cards, form action cards, header cards — blurs screen bg
    const val Overlay = 1   // nav pill, top bar pills — blurs bg + surface-level content
    const val Drawer  = 2   // navigation drawer — blurs all visible screen content
}
```

---

## CompositionLocals Reference

| Local | Provided by | Read by | Value when absent |
|-------|-------------|---------|-------------------|
| `LocalAeonHaze` | `AeonHazeProvider` | `GlassSurface` (tier 3 fallback) | `null` |
| `LocalParentGlassHaze` | `AeonGlassHost` | `GlassSurface` (tier 2) | `null` (outside any host) |
| `LocalHazeDepth` | `AeonGlassHost` (+1 per level) | `GlassSurface`, `GlassTokens.config()` | `0` |
| `LocalGlassmorphismEnabled` | `MainActivity` | `GlassSurface`, all glass consumers | `true` |
| `LocalGlassSettings` | `OssWalletTheme` | `GlassTokens` style factories | `GlassSettings()` defaults |
| `LocalContourLinesEnabled` | `OssWalletTheme` | `ContourBackground` | `true` |

---

## Patterns

### Pattern A: Dashboard Card (Screen Background Blur)

This is the standard pattern for all dashboard and analytics card composables. `AeonExpressiveCard` wraps `GlassSurface → Card → AeonGlassHost` internally — callers do not interact with any glass infrastructure directly.

```kotlin
@Composable
fun MyCard(
    cardWithCallbacks: CardWithCallbacks.My,
    modifier: Modifier = Modifier,
    showTitle: Boolean = false
) {
    AeonExpressiveCard(
        title = "My Card",
        modifier = modifier,
    ) {
        // content() is inside AeonGlassHost already — children can blur the card surface
        MyCardContent(cardWithCallbacks.renderData)
    }
}
```

The `AeonExpressiveCard` shell resolves its own `GlassSurface` via `LocalAeonHaze[HazeLayer.Surface]` (level 3). Any `GlassSurface` inside `content {}` automatically blurs the card surface via `LocalParentGlassHaze` (level 2).

### Pattern B: Glass-on-Glass (Nested Elements Blurring Their Container)

When a custom container (not `AeonExpressiveCard`) should expose itself as blurable by children, wrap its content in `AeonGlassHost`:

```kotlin
GlassSurface(
    modifier = modifier,
    shape = RoundedCornerShape(16.dp),
    glassVariant = GlassVariant.Card,
    fallbackColor = AppColors.surfaceContainerLow,
) {
    AeonGlassHost(modifier = Modifier.fillMaxWidth()) {
        // GlassSurface composables inside here automatically blur this card surface
        Column(modifier = Modifier.padding(16.dp)) {
            NestedGlassRow()  // if this uses GlassSurface internally, it blurs the card
            content()
        }
    }
}
```

**Do not add `AeonGlassHost` if children are plain Compose composables** (Text, Icon, Row, etc. without their own `GlassSurface`). The host is only needed when children render `GlassSurface`.

### Pattern C: Bottom Sheet

Use `AeonGlassSheet`. It wraps all the glass wiring internally:

```kotlin
if (showSheet) {
    AeonGlassSheet(onDismiss = { showSheet = false }) {
        // ColumnScope content — AeonGlassHost is already applied inside the sheet
        Text("Sheet content")
        AeonButton(onClick = { ... }) { Text("Action") }
    }
}
```

For sheets that need a different `hazeLayer` (e.g., inside a screen that is itself a glass surface):

```kotlin
AeonGlassSheet(
    onDismiss = { ... },
    hazeLayer = HazeLayer.Overlay,
) {
    content()
}
```

### Pattern D: Navigation Drawer

The app drawer blurs the entire `NavHost`. It uses an explicit `hazeState` parameter (priority level 1) that bypasses `LocalParentGlassHaze`:

```kotlin
// In OssWalletNavGraph.kt — the HazeState is owned at the nav graph level
GlassSurface(
    modifier = Modifier.fillMaxHeight().width(280.dp),
    shape = drawerShape,
    hazeState = drawerHazeState,          // explicit — bypasses LocalParentGlassHaze
    hazeStyle = GlassTokens.drawerStyle(),
    glassVariant = GlassVariant.Drawer,
    fallbackColor = AppColors.drawerSurface,
) {
    // drawer content
}
```

The `NavHost` is wrapped with `hazeSource(drawerHazeState)` in the same composable scope, making them siblings.

### Pattern E: Preview / Design Lab Isolation

`DesignLabScreen` and `GlassLabScreen` need independent glass state for live theme previews. Pass `hazeState` explicitly to `GlassSurface` — this takes priority level 1 and bypasses the entire `LocalParentGlassHaze` / `LocalAeonHaze` chain:

```kotlin
val previewHaze = remember { HazeState() }

Box(modifier = Modifier.hazeSource(previewHaze)) {
    // preview background content
}

GlassSurface(
    hazeState = previewHaze,              // explicit — isolated from app hierarchy
    hazeStyle = GlassTokens.cardStyle(),
    ...
) {
    // preview card
}
```

**Never modify Design Lab glass wiring.** It is intentionally isolated from the three-tier system.

---

## Adding Glass to a New Component — Checklist

**Step 1 — Determine the blur source.** Ask: what should bleed through the glass?

| Scenario | Source tier |
|----------|-------------|
| Component blurs the screen background | Tier 3 automatic — no action needed |
| Component blurs a parent glass container | Tier 2 — parent must wrap content in `AeonGlassHost` |
| Component blurs a specific, non-standard source | Tier 1 — create `HazeState`, wrap source with `hazeSource()`, pass state explicitly |

**Step 2 — Use `GlassSurface` as the shell.** Never write `hazeEffect` inline:

```kotlin
GlassSurface(
    modifier = modifier,
    shape = RoundedCornerShape(AeonTheme.dimensions.cardCornerRadius),
    glassVariant = GlassVariant.Card,            // picks style and tokens automatically
    fallbackColor = AppColors.surfaceContainerLow,
) {
    content()
}
```

**Step 3 — Choose `glassVariant` or supply `hazeStyle` explicitly.** Prefer `glassVariant` — it resolves both the `HazeStyle` and border/shadow tokens from `GlassTokens`. Only supply `hazeStyle` directly when you need a one-off style not covered by the variant system.

**Step 4 — Add `AeonGlassHost` if children will render glass surfaces.** If `content()` only contains plain composables (Text, Icon, Row), skip this. If it contains nested `GlassSurface` composables that should blur this surface, wrap:

```kotlin
GlassSurface(...) {
    AeonGlassHost(modifier = Modifier.fillMaxWidth()) {
        content()
    }
}
```

**Step 5 — Verify the fallback.** Run a `@Preview` with no `CompositionLocalProvider` setup. The component must render with `fallbackColor` as a solid background — no crash, no blank surface.

**Step 6 — Verify the on/off toggle.** In `DisplaySettingsScreen`, toggle the "Glass Effect" switch. The component must degrade to `fallbackColor` when disabled.

---

## User Setting Wire-Up

The glassmorphism on/off preference is already wired app-wide. No new wiring is needed when adding a component.

| Layer | File | Symbol |
|-------|------|--------|
| DataStore key | `SettingsDataStore` | `GLASSMORPHISM_ENABLED_KEY` |
| Repository | `SettingsRepository` | `glassmorphismEnabled`, `saveGlassmorphismEnabled` |
| ViewModel | `SettingsViewModel` | `glassmorphismEnabled: StateFlow<Boolean>`, `setGlassmorphismEnabled()` |
| UI provider | `MainActivity` | `CompositionLocalProvider(LocalGlassmorphismEnabled provides …)` |
| UI toggle | `DisplaySettingsScreen` | Switch item "Glass Effect" in APPEARANCE section |

`LocalGlassmorphismEnabled` propagates to every composable in the tree automatically. `GlassSurface` reads it and switches to `fallbackColor` when `false`.

---

## Common Pitfalls

| Symptom | Root cause | Fix |
|---------|-----------|-----|
| Blur renders as solid white/black rectangle | `hazeSource` is drawn *after* (on top of) the `hazeEffect` recipient | Source must be lower in Z-order; in a `Box`, place the source `Spacer` before content |
| Items inside a card blur the screen bg, not the card | `AeonGlassHost` missing from the card's content | Wrap the card's content in `AeonGlassHost` |
| Child `GlassSurface` shows no blur at all | `AeonGlassHost`'s `Spacer` is not sized correctly | Use `Modifier.matchParentSize()` on the source Spacer, not `fillMaxSize()` |
| `hazeEffect` drops `shadowElevation` silently | Both share the same `graphicsLayer` — `RenderEffect` installed by Haze discards the shadow | `GlassSurface` handles this: shadow is on a sibling `Spacer` with its own `graphicsLayer { clip = false }` — do not replicate shadow manually |
| Preview crashes with `LocalAeonHaze` NPE | `AeonHazeProvider` not in preview tree | `LocalAeonHaze`'s default is `null` and `GlassSurface` uses `?.states?.getOrNull()` — preview should render as `fallbackColor`. If it crashes, check for non-null assertions on `LocalAeonHaze.current` |
| Glass stops working after navigating away | `HazeState` created outside `remember` | Always `remember { HazeState() }` at the composition scope that owns it |
| `LocalGlassmorphismEnabled` change not reflected | Read outside a `@Composable` function body | Read `LocalGlassmorphismEnabled.current` inside the composable, not in a helper `object` |
| Adding `AeonHazeProvider` inside a card / sheet | Creates a second background over the first and breaks the blur source tree | `AeonHazeProvider` belongs only at screen / dialog / scaffold entry points |
| Double boxing after adding `AeonGlassHost` | Outer `Box` was left in place after adding `AeonGlassHost` whose content is already `BoxScope` | Remove redundant wrapping `Box`; `AeonGlassHost` already exposes `BoxScope` to content |

---

## Haze Library Version Notes

- The library migrated from `.hazeChild()` to `.hazeEffect()` in a breaking API change. Always use `.hazeEffect()`.
- `HazeTint` takes a `Color`, not a `Brush`. Avoid `HazeTint(brush = …)` unless intentionally painting a gradient frost.
- `NoiseFactor` adds film-grain texture. Keep at `0f` for the clean Aeon aesthetic.
- Haze 1.7.2+ crashes if a composable is simultaneously registered as `hazeSource` and `hazeEffect`. `aeonHazeSource()` is a null-safe wrapper that prevents this; use it instead of `.hazeSource()` directly.
