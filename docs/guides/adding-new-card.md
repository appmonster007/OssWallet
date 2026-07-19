# Developer Guide: Adding a New Dashboard Card

This guide covers all 15 required steps to add a new card to the OssWallet card system. The system follows the **Open-Closed Principle**: new card types are added by extending registries — the orchestration layer (`UnifiedCardRenderer`, `CardComputerRegistry`, `CardUiBridge`) never requires changes to its core logic.

**Do not skip steps.** Missing any of the 15 entries causes a silent runtime crash on card reorder, not a compile error. Complete them in order.

---

## Files You Will Create (2 new files)

| File | Purpose |
|------|---------|
| `domain/card/computers/DummyCardComputer.kt` | Pure Kotlin — computes render data from `CardInput` |
| `ui/dashboard/components/DummyCard.kt` | Compose composable — renders the card in the UI |

---

## Step 1: Define the Card Type

**File:** `domain/card/CardType.kt`

```kotlin
enum class CardType(val serialName: String, val displayName: String, val description: String) {
    // ... existing types
    DUMMY("dummy", "Dummy Card", "A demonstration card for documentation.")
}
```

---

## Step 2: Create the Configuration Model

**File:** `domain/model/settings/CardConfig.kt`

The config stores user-facing settings (display preferences, filters, etc.). Every field must have a default value.

```kotlin
@Serializable
@SerialName("dummy")
data class DummyCardConfig(
    override val cardId: String,
    override val title: String = "DUMMY",
    override val isEnabled: Boolean = true,
    override val order: Int = 0,
    override val screenContext: ScreenContext = ScreenContext.DASHBOARD,
    override val filter: TransactionFilter = TransactionFilter(),
    override val filterRules: FilterRules = FilterRules(),
    val customMessage: String = "Hello World"  // card-specific setting
) : CardConfig()
```

---

## Step 3: Define the Render Data

**File:** `domain/card/CardRenderData.kt`

The render data is the UI-ready output of the computer. Keep it flat — no business logic, no repositories.

```kotlin
sealed class CardRenderData {
    // ... existing entries
    data class Dummy(
        val config: DummyCardConfig,
        val processedMessage: String
    ) : CardRenderData()
}
```

---

## Step 4: Implement the Computer

**NEW FILE:** `domain/card/computers/DummyCardComputer.kt`

A pure function from `(CardInput, DummyCardConfig) → CardRenderData.Dummy`. No Android imports. No side effects.

```kotlin
class DummyCardComputer : CardDataComputer<DummyCardConfig, CardRenderData.Dummy> {
    override val supportedType = CardType.DUMMY

    override fun compute(input: CardInput, config: DummyCardConfig): CardRenderData.Dummy {
        val message = "${config.customMessage} — precision: ${input.precision}"
        return CardRenderData.Dummy(config, message)
    }
}
```

---

## Step 5: Register the Computer

**File:** `domain/card/CardComputerRegistry.kt`

```kotlin
val DEFAULT = CardComputerRegistry(
    listOf(
        // ... existing computers
        DummyCardComputer()
    )
)
```

---

## Step 6: Add the Config Update Handler

**File:** `domain/card/CardUpdateHandler.kt`

```kotlin
fun updateCardDummyConfig(cardId: String, config: DummyCardConfig) {
    // persist config change — follows existing pattern in this file
}
```

---

## Step 7: Add the Callbacks Type

**File:** `ui/components/cards/CardCallbacks.kt`

Defines the UI events this card can emit.

```kotlin
sealed class CardCallbacks {
    // ... existing
    data class Dummy(
        val onUpdate: (DummyCardConfig) -> Unit,
        val onDelete: () -> Unit,
    ) : CardCallbacks()
}
```

---

## Step 8: Add the CardWithCallbacks Type

**File:** `ui/components/cards/CardWithCallbacks.kt`

Pairs render data with callbacks for the renderer.

```kotlin
sealed class CardWithCallbacks {
    // ... existing
    data class Dummy(
        val renderData: CardRenderData.Dummy,
        val callbacks: CardCallbacks.Dummy,
    ) : CardWithCallbacks()
}
```

---

## Step 9: Wire the UI Bridge

**File:** `ui/components/cards/CardUiBridge.kt`

Add **two** branches inside `rememberCardWithCallbacks()`:

```kotlin
// Compute branch (inside the `when (config)` block):
is DummyCardConfig -> registry.compute(CardType.DUMMY, input, config)

// Pair branch (inside the `when { renderData is ... && config is ... }` block):
renderData is CardRenderData.Dummy && config is DummyCardConfig ->
    CardWithCallbacks.Dummy(
        renderData = renderData,
        callbacks = CardCallbacks.Dummy(
            onUpdate = { updatedConfig -> updateHandler.updateCardDummyConfig(config.cardId, updatedConfig) },
            onDelete = { onDelete(config.cardId) },
        )
    )
```

---

## Step 10: Create the Card Composable

**NEW FILE:** `ui/dashboard/components/DummyCard.kt`

Use `AeonExpressiveCard` as the shell — it provides the glass surface, shadow, border, header row, timeline selector, and filter button automatically. Do not call `GlassSurface` or `AeonGlassHost` directly in card composables.

```kotlin
@Composable
fun DummyCard(
    cardWithCallbacks: CardWithCallbacks.Dummy,
    modifier: Modifier = Modifier,
    showTitle: Boolean = false,
) {
    val data = cardWithCallbacks.renderData
    val callbacks = cardWithCallbacks.callbacks

    AeonExpressiveCard(
        title = data.config.title,
        modifier = modifier,
        icon = Icons.Rounded.Info,
        showSettingsButton = false,
    ) {
        // content() lambda — already inside AeonGlassHost, so children that use
        // GlassSurface internally will automatically blur this card surface.
        Text(
            text = data.processedMessage,
            style = AppTextStyles.body,
            color = AppColors.contentPrimary,
        )
    }
}
```

**Glass behaviour is automatic.** `AeonExpressiveCard` wraps `GlassSurface → Card → AeonGlassHost`. Any `GlassSurface` inside `content {}` blurs the card surface via `LocalParentGlassHaze`. Screen-level blur is resolved via `LocalAeonHaze[HazeLayer.Surface]`. No glass wiring needed in the card composable.

See `.gemini/glassmorphism.md` — Pattern A for details.

---

## Step 11: Register in UnifiedCardRenderer

**File:** `ui/components/cards/UnifiedCardRenderer.kt`

```kotlin
@Composable
fun UnifiedCardRenderer(cardWithCallbacks: CardWithCallbacks, modifier: Modifier) {
    when (cardWithCallbacks) {
        // ... existing
        is CardWithCallbacks.Dummy -> DummyCard(
            cardWithCallbacks = cardWithCallbacks,
            modifier = modifier,
        )
    }
}
```

---

## Step 12: Update CardRepositoryImpl

**File:** `data/repository/CardRepositoryImpl.kt`

Add a branch in **all three** `reorder*` `when` blocks (dashboard, accountDetail, accountsView):

```kotlin
// In reorderDashboardCards(), reorderAccountDetailCards(), reorderAccountsViewCards():
is DummyCardConfig -> card.copy(order = index)
```

Missing any of the three blocks causes a silent crash when the user reorders cards.

---

## Step 13: Update CardLayoutViewModel

**File:** `ui/viewmodel/CardLayoutViewModel.kt`

```kotlin
fun addDashboardCard(type: CardType) {
    val config = when (type) {
        // ... existing
        CardType.DUMMY -> DummyCardConfig(cardId = generateId())
    }
    // ...
}
```

---

## Step 14: Add Navigation Routes (if the card opens new screens)

**File:** `ui/navigation/Screen.kt`

Skip this step if the card opens no new screens.

```kotlin
object DummyDetail : Screen("dummy_detail/{id}") {
    fun createRoute(id: Long) = "dummy_detail/$id"
    val argId = navArgument("id") { type = NavType.LongType }
}
```

---

## Step 15: Wire New Screens in the Nav Graph (if applicable)

**File:** `ui/navigation/OssWalletNavGraph.kt`

Skip this step if step 14 was skipped.

```kotlin
composable(
    route = Screen.DummyDetail.route,
    arguments = listOf(Screen.DummyDetail.argId)
) { backStack ->
    val id = backStack.arguments?.getLong("id") ?: return@composable
    DummyDetailScreen(id = id, onBack = { navController.popBackStack() })
}
```

---

## Verification Checklist

```bash
# Compile check — most missing steps produce compile errors after this refactor
./gradlew assembleDebug

# Confirm no unhandled when branches (these would be runtime crashes)
./gradlew testDebugUnitTest
```

**Manual checks on device/emulator:**
- [ ] Card appears in the "Add card" sheet
- [ ] Card renders on dashboard with correct glass effect (screen background blurs through)
- [ ] Card header, subtitle, and content render correctly
- [ ] Filter button (if enabled) opens filter sheet and applies correctly
- [ ] Reordering cards (long-press drag) doesn't crash
- [ ] Deleting card removes it cleanly
- [ ] Glass Effect toggle in settings degrades card to solid surface correctly
- [ ] Card renders correctly in Light, Dark, and AMOLED themes

---

## Architecture Benefits

| Property | How it's achieved |
|----------|------------------|
| **Type safety** | `CardDataComputer<Config, RenderData>` interface ensures config/render-data pairing |
| **Automatic caching** | `CardComputerRegistry` caches computed render data without any per-card code |
| **ViewModel decoupling** | `DashboardViewModel` and `AnalyticsViewModel` need zero changes |
| **Testability** | `DummyCardComputer.compute()` is a pure function — test it with plain JUnit4, no Android runtime |
| **Glass automatic** | `AeonExpressiveCard` handles all glass wiring; card composables are pure UI |
