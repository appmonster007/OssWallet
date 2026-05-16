# OssWallet — UI & Design Directive

**Scope:** Jetpack Compose, Aeon design system, Material 3 theming, card rendering, navigation, recomposition, and Glance widgets.

---

## Goals

- Ship UI that is visually consistent with the Aeon design system across every screen.
- Keep composables pure: data in, events out — no business logic.
- Prevent unnecessary recomposition that causes jank.
- Keep widgets always renderable — never crash on data-load failure.

---

## How to Achieve Them

Before writing a new composable, check `ui/components/` for an existing Aeon component that covers the need. Extend with parameters rather than duplicating. Move all filtering, sorting, and computation to the ViewModel layer. Use stable keys in lazy lists and `derivedStateOf` where state dependencies are indirect.

---

## Aeon Design System

All reusable UI components use the `Aeon` prefix and live in `ui/components/`:

| Need | Component file |
|------|---------------|
| Buttons | `AeonButtons.kt` |
| Cards / surfaces | `AeonCards.kt` |
| Text inputs | `AeonInputs.kt` |
| Alert dialogs | `AeonAlertDialog.kt` |
| Progress indicators | `AeonProgress.kt` |
| Form fields | `AeonFormComponents.kt` |
| Filter chips / timeline selector | `AeonTimelineFilter.kt`, `AeonFilterComponents.kt` |
| Charts | `ui/components/visualization/charts/Aeon*Chart.kt` |

**Rule:** If no existing Aeon component fits, add parameters to the nearest one before creating a new file.

---

## Card Rendering Rules

1. All dashboard and analytics cards are rendered through `UnifiedCardRenderer` — never render card internals directly from a screen composable.
2. Card data is computed inside `rememberCardWithCallbacks()` — screens pass `CardInput`, the registry, and callbacks; they do not call `registry.compute()` themselves.
3. Dashboard card composables live in `ui/dashboard/components/`; analytics cards in `ui/analytics/components/`.
4. Standard card composable signature:
   ```kotlin
   @Composable
   fun XxxCard(
       cardWithCallbacks: CardWithCallbacks.Xxx,
       modifier: Modifier = Modifier,
       showTitle: Boolean = false
   )
   ```
5. Reuse the existing `CardHeader` composable for the title + filter chip row + overflow menu.

---

## Theming

- Material 3 dynamic theming is in `ui/theme/Theme.kt`.
- All color values are in `ui/theme/Color.kt` — **never hardcode hex values in composables**.
- Typography is in `ui/theme/Type.kt`.
- Theme modes (System/Light/Dark) are controlled via `SettingsRepository.theme`.
- Access theme values: `MaterialTheme.colorScheme.*`, `MaterialTheme.typography.*`.
- Color layering and token assignment rules are in `.gemini/color-usage-guide.md` — read it before creating new layout layers.

---

## State and Recomposition

```kotlin
// DO: collect ViewModel state in the composable
val uiState by viewModel.uiState.collectAsStateWithLifecycle()

// DO: derive dependent state to avoid over-recomposition
val isButtonEnabled by remember { derivedStateOf { text.isNotBlank() && amount > 0 } }

// OK: cheap inline computation inside remember
val grouped = remember(transactions) { transactions.groupBy { it.date } }

// DON'T: heavy computation inside a composable without remember
val totals = transactions.sumOf { it.amount }  // recalculates every recomposition
```

- Move filtering, sorting, and aggregation to ViewModel or domain services.
- `@Composable` functions only map `UiState → UI` — zero business logic.

### Recomposition Checklist

- [ ] `LazyColumn`/`LazyRow` items have stable `key =` parameters.
- [ ] Lambdas used in `LazyColumn` items are not created inline — wrap with `remember` or pass from parent.
- [ ] `Modifier.clickable` callbacks are lambdas passed from parent, not created inside `LazyColumn` items.
- [ ] Images use `AsyncImage` (Coil), not raw `Bitmap` operations inside composition.
- [ ] Heavy list operations (`filter`, `groupBy`, `sortedBy`) are in ViewModel, not composables.

---

## Navigation

Navigation is managed entirely in `ui/navigation/OssWalletNavGraph.kt`.

**Adding a new screen:**

1. Add a route to `ui/navigation/Screen.kt`:
   ```kotlin
   object MyScreen : Screen("my_screen")

   // With arguments:
   object MyDetail : Screen("my_detail/{id}") {
       fun createRoute(id: Long) = "my_detail/$id"
       val argId = navArgument("id") { type = NavType.LongType }
   }
   ```
2. Add a `composable()` destination in `OssWalletNavGraph.kt`.
3. Pass navigation lambdas into the screen — never use `NavController` inside a screen composable directly.

---

## Glance Widget UI Rules

Glance composables are fundamentally different from standard Compose. Key rules:

- Use `androidx.glance.*` imports — not `androidx.compose.*`.
- Layout primitives: `Column`, `Row`, `Box` from `androidx.glance.layout`.
- Text: `Text` from `androidx.glance`.
- No `remember`, `LaunchedEffect`, or Compose state in Glance composables — all state flows from the `provideContent {}` suspension block.
- Tap actions: `clickable(actionStartActivity(intent))` where `intent` is a standard Android `Intent`.
- Colors: `GlanceTheme.colors.*` or a hardcoded `ColorProvider`.
- Widget composable files live in `widget/` alongside their `GlanceAppWidget` class.

### `provideGlance()` Structure

All data fetching happens **before** `provideContent {}` — the composable is pure display:

```kotlin
override suspend fun provideGlance(context: Context, id: GlanceId) {
    val widgetId = GlanceAppWidgetManager(context).getAppWidgetId(id)
    val config = runCatching { XxxWidgetPrefsStore(context).load(widgetId) }
        .getOrElse { XxxWidgetConfig() }

    val result = runCatching {
        val common = WidgetDataLoader.loadCommon(context)
        // compute render data ...
        Triple(renderData, common.baseCurrency, common.precision)
    }.getOrNull()

    provideContent {
        XxxWidgetContent(context, config, result?.first, result?.second ?: "", result?.third ?: 2)
    }
}
```

The outer `runCatching` ensures `provideContent {}` is always reached — widgets must never fail to render.

### Tap-to-Navigate Pattern

```kotlin
.clickable(
    actionStartActivity(
        Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP
            putExtra(MainActivity.EXTRA_WIDGET_NAV, "route_name")
        }
    )
)
```

### Chart Rendering in Widgets

Glance cannot use Canvas-based Compose charts. Use `WidgetChartRenderer` to produce a `Bitmap`:

```kotlin
val bitmap = WidgetChartRenderer.renderProgressBars(widthPx, heightPx, items, density)
Image(provider = ImageProvider(bitmap), contentDescription = null)
```

---

## Accessibility

- Every interactive element must have a `contentDescription` or semantic label.
- Minimum touch target: 48dp × 48dp.
- Text contrast must meet WCAG 2.1 AA (4.5:1 for body text, 3.0:1 for large text and boundaries). Refer to `color-usage-guide.md` for token pairings.
- Do not convey information through color alone — pair color cues with icons or text labels.
- Use `Modifier.semantics { }` for custom components that are not inherently accessible (e.g., custom toggle, progress ring).

---

## Best Practices

- Always provide `modifier: Modifier = Modifier` as the last non-trailing-lambda parameter on composables.
- Use `@Preview` annotations for new components; include light and dark theme variants.
- For bottom sheets and dialogs, prefer `ModalBottomSheet` and `AlertDialog` from Material 3.
- Keep composable function bodies focused: if a composable scrolls past one screen in the editor, split it.

---

## What Not To Do

- Do not hardcode color hex values (`0xFF1A1A1A`) in composables — use theme tokens.
- Do not import `androidx.compose.*` in Glance widget files.
- Do not place business logic, repository calls, or `Flow.collect` inside a `@Composable` function body.
- Do not create duplicate components that mirror existing Aeon components — extend them instead.
- Do not use `LocalContext.current` to navigate — pass navigation lambdas from the nav graph.
- Do not create `LazyColumn` items with inline lambda callbacks — they break recomposition stability.
- Do not suppress accessibility warnings without adding equivalent accessibility semantics.
