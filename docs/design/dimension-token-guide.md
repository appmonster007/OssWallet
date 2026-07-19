# Aeon Dimension Token Guide

This guide lists all available `AeonSize` and `AeonDimensions` tokens and explains when to use them versus literal `dp` values. Stories 3.2 and 3.3 should use this guide as the source of truth when replacing hardcoded dimension values.

---

## AeonSize — Primitive Grid Values

`AeonSize` is a 2 dp grid enum covering 2–64 dp. Every value is a multiple of 2 (with the single exception of `D43`).

| Token | Value | Notes |
|-------|-------|-------|
| `AeonSize.D2` | 2.dp | Hairline dividers, micro offsets |
| `AeonSize.D4` | 4.dp | — |
| `AeonSize.D6` | 6.dp | — |
| `AeonSize.D8` | 8.dp | — |
| `AeonSize.D10` | 10.dp | — |
| `AeonSize.D12` | 12.dp | — |
| `AeonSize.D14` | 14.dp | — |
| `AeonSize.D16` | 16.dp | — |
| `AeonSize.D18` | 18.dp | — |
| `AeonSize.D20` | 20.dp | — |
| `AeonSize.D22` | 22.dp | — |
| `AeonSize.D24` | 24.dp | — |
| `AeonSize.D26` | 26.dp | — |
| `AeonSize.D28` | 28.dp | — |
| `AeonSize.D30` | 30.dp | — |
| `AeonSize.D32` | 32.dp | — |
| `AeonSize.D34` | 34.dp | — |
| `AeonSize.D36` | 36.dp | — |
| `AeonSize.D38` | 38.dp | — |
| `AeonSize.D40` | 40.dp | — |
| `AeonSize.D42` | 42.dp | — |
| `AeonSize.D43` | 43.dp | Off-grid; retained for existing icon sizes — prefer `D44` for new code |
| `AeonSize.D44` | 44.dp | Standard touch target minimum |
| `AeonSize.D46` | 46.dp | — |
| `AeonSize.D48` | 48.dp | — |
| `AeonSize.D50` | 50.dp | — |
| `AeonSize.D52` | 52.dp | — |
| `AeonSize.D54` | 54.dp | — |
| `AeonSize.D56` | 56.dp | — |
| `AeonSize.D58` | 58.dp | — |
| `AeonSize.D60` | 60.dp | — |
| `AeonSize.D64` | 64.dp | Upper bound; large decorative icons |

Access via `.value`: `AeonSize.D16.value` → `16.dp`.

---

## AeonDimensions — Semantic Spacing Tokens

Accessed at runtime via `AeonTheme.dimensions`. All values scale with the user's UI Scale setting (stored in `LocalAeonDimensions`).

### Layout Paddings

| Token | Default | Use |
|-------|---------|-----|
| `screenPadding` | 14.dp | Outer horizontal margin of all primary screens |
| `cardPadding` | 12.dp | Internal content margin inside structural cards |
| `chipPadding` | 8.dp | Internal padding for chips and small action items |
| `listPadding` | 10.dp | Horizontal padding for `LazyColumn` items |
| `gridPadding` | 10.dp | Spacing between items in a `LazyVerticalGrid` |

### Semantic Spacing

| Token | Default | Use |
|-------|---------|-----|
| `spacingXs` | 4.dp | Micro-adjustments — icon + text in a chip |
| `spacingSm` | 8.dp | Tight groups — label and its value |
| `spacingMd` | 12.dp | Standard component gap — between form fields |
| `spacingLg` | 16.dp | Comfortable section gap |
| `spacingXl` | 24.dp | Large structural breaks |
| `spacing2xl` | 32.dp | Major screen divisions |
| `spacing3xl` | 48.dp | Hero sections or large empty states |

### Icon Sizes

| Token | Default | Use |
|-------|---------|-----|
| `iconSizeXs` | 12.dp | Tiny inline indicators |
| `iconSizeSm` | 18.dp | Secondary actions, small list icons |
| `iconSizeMd` | 24.dp | Primary list icons, action buttons |
| `iconSizeLg` | 32.dp | Highlighted headers, feature cards |
| `iconSizeXl` | 44.dp | Large profile icons, hero cards |
| `iconSize2xl` | 52.dp | Decorative feature icons |
| `iconSize3xl` | 64.dp | Massive empty state or landing icons |

### Corner Radii

| Token | Default | Use |
|-------|---------|-----|
| `cardCornerRadius` | 28.dp | Main structural cards (Accounts, Budgets, Loans) |
| `dialogCornerRadius` | 32.dp | Full-screen bottom sheets and alert dialogs |
| `containerCornerRadius` | 24.dp | Generic grouping surfaces inside screens |
| `componentCornerRadius` | 16.dp | Interactive elements — chips, input fields |
| `chipCornerRadius` | 20.dp | Action chips, mini buttons |
| `buttonCornerRadius` | 28.dp | Main action buttons (FAB, submit) |

---

## When to Use a Token vs. Literal dp

**Use `AeonTheme.dimensions.*`** when:
- Sizing a reusable component (icons, cards, inputs, dialogs).
- Setting the padding or margin of any structural screen container.
- Spacing between sibling components with `Arrangement.spacedBy(...)`.
- Setting corner radii on any surface or container.

**Use `AeonSize.DXX.value`** when:
- The value is hardcoded in a preview or non-scaled context (e.g., `TransactionHeader` glass surface padding).
- The component's size should not respond to the user's UI Scale setting.

**Use a literal `dp` value** when:
- It is a one-off positional offset (e.g., `offset(x = (-4).dp, y = 4.dp)`).
- The value is fractional or otherwise cannot map to a 2 dp grid step (e.g., `1.5.dp` stroke width, `0.5.dp` divider thickness).
- The value is part of an algorithmically generated layout (proportional, responsive, or animated).

**Never** use a literal `dp` for icon sizes, corner radii, screen/card paddings, or inter-component spacing — those are exactly what the token system exists to unify.

---

## Usage Example

```kotlin
// Preferred — responds to UI Scale
val dims = AeonTheme.dimensions
Column(
    modifier = Modifier.padding(horizontal = dims.screenPadding),
    verticalArrangement = Arrangement.spacedBy(dims.spacingMd)
) {
    Icon(
        imageVector = icon,
        modifier = Modifier.size(dims.iconSizeMd)
    )
}

// Acceptable — preview-only or non-scaled fixed layout
GlassSurface(
    modifier = Modifier.padding(horizontal = AeonSize.D16.value, vertical = AeonSize.D8.value)
)

// Acceptable — fractional one-off
HorizontalDivider(thickness = 0.5.dp)
Box(modifier = Modifier.offset(x = (-4).dp, y = 4.dp))
```
