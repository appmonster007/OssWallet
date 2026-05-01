---
name: android-compose
description: Use this agent for all Jetpack Compose UI work in OssWallet — screens, components, cards, navigation, theming, and Glance widgets. Invoke when building new screens, new card UIs, reusable components, or debugging composition/recomposition issues.
---

# OssWallet Compose UI Agent

## Design System — Aeon Components

All reusable UI components use the `Aeon` prefix and live in `ui/components/`:

| Need | Use |
|------|-----|
| Buttons | `AeonButtons.kt` |
| Cards/surfaces | `AeonCards.kt` |
| Text inputs | `AeonInputs.kt` |
| Alert dialogs | `AeonAlertDialog.kt` |
| Progress indicators | `AeonProgress.kt` |
| Form fields | `AeonFormComponents.kt` |
| Filter chips/timeline | `AeonTimelineFilter.kt`, `AeonFilterComponents.kt` |
| Charts | `ui/components/visualization/charts/Aeon*Chart.kt` |

**Before creating a new component**, check if an existing Aeon component covers the use case. Extend existing ones with parameters rather than creating near-duplicates.

---

## Card Rendering Rules

1. **All dashboard/analytics cards go through `UnifiedCardRenderer`** — never render card internals directly from a screen composable.
2. **Card data is computed in `rememberCardWithCallbacks()`** — screens pass `CardInput`, `registry`, and callbacks; they do not call `registry.compute()` themselves.
3. **Card Compose files live in `ui/dashboard/components/`** for dashboard cards, `ui/analytics/components/` for analytics cards.
4. **Each card Composable signature:**
   ```kotlin
   @Composable
   fun XxxCard(
       cardWithCallbacks: CardWithCallbacks.Xxx,
       modifier: Modifier = Modifier,
       showTitle: Boolean = false
   )
   ```
5. **Card header pattern** — reuse the existing `CardHeader` composable (search for it in `ui/components/`) for the title + filter chip row + overflow menu.

---

## State and Recomposition

```kotlin
// DO: derive state from ViewModel's StateFlow
val uiState by viewModel.uiState.collectAsStateWithLifecycle()

// DON'T: derive UI state inside a composable
val filtered = remember(transactions) { transactions.filter { ... } }  // ok for cheap ops
val expensiveResult = remember(input) { heavyComputation(input) }      // move to ViewModel

// DO: use derivedStateOf for state that depends on other state
val isButtonEnabled by remember { derivedStateOf { text.isNotBlank() && amount > 0 } }
```

- Move all filtering, sorting, and computation to ViewModel or domain services
- Composables only transform `UiState → UI`; no business logic in `@Composable` functions

---

## Navigation

Navigation is managed entirely in `ui/navigation/OssWalletNavGraph.kt`.

**Adding a new screen:**
1. Add a route to `ui/navigation/Screen.kt`:
   ```kotlin
   object MyScreen : Screen("my_screen")
   // With args:
   object MyDetail : Screen("my_detail/{id}") {
       fun createRoute(id: Long) = "my_detail/$id"
       val argId = navArgument("id") { type = NavType.LongType }
   }
   ```
2. Add a `composable()` destination in `OssWalletNavGraph.kt`
3. Pass navigation lambdas into the screen — never use `LocalContext` or `NavController` inside a screen composable directly; always navigate via lambdas

---

## Theming

- Material3 dynamic theming via `ui/theme/Theme.kt`
- Colors defined in `ui/theme/Color.kt` — do not hardcode hex values in composables
- Typography defined in `ui/theme/Type.kt`
- Theme modes: System/Light/Dark — controlled via `SettingsRepository.theme`
- Access current theme: `MaterialTheme.colorScheme.*`, `MaterialTheme.typography.*`

---

## Glance Widget UI Rules

Glance (home screen widgets) has different composables from regular Compose. Key differences:
- Use `androidx.glance.*` imports, not `androidx.compose.*`
- Layout: `Column`, `Row`, `Box` from `androidx.glance.layout`
- Text: `Text` from `androidx.glance`
- No `remember`, `LaunchedEffect`, or state in Glance composables — all state comes from `provideContent { }` suspension
- Tap actions: use `clickable(actionStartActivity<MainActivity>(...))` or `actionRunCallback<MyAction>()`
- Colors: use `GlanceTheme.colors.*` or hardcoded `ColorProvider`

Widget Compose files live in `widget/` alongside their `GlanceAppWidget` class.

---

## Performance Checklist

- [ ] No lambdas created inside `@Composable` without `remember` when used with `LazyColumn` items
- [ ] `LazyColumn`/`LazyRow` items have stable `key =` parameters
- [ ] Images use `AsyncImage` from Coil, not raw `Bitmap` operations in composition
- [ ] Heavy list operations (`filter`, `groupBy`, `sortedBy`) are in ViewModel, not in composables
- [ ] `Modifier.clickable` callbacks are lambdas passed from parent, not created inline in `LazyColumn` items
