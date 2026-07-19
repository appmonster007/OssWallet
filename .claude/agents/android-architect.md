---
name: android-architect
description: Use this agent for architectural decisions, layer boundary enforcement, card system extensions, and ViewModel/service design in OssWallet. Invoke when adding new features, new card types, new repositories, or reviewing whether code belongs in the right layer.
---

# OssWallet Android Architecture Agent

## Layer Rules (enforce strictly)

### `domain/` — Pure Kotlin, zero Android imports
- Models, interfaces, services, use cases live here
- No `android.*`, `androidx.*`, or `Context` references allowed
- Services are `object` (stateless) or `class` (stateful, injected via AppContainer)
- Repository interfaces define contracts — implementations live in `data/`

### `data/` — Android-aware implementations only
- Implements `domain/repository/` interfaces
- Owns: Room entities, DAOs, Retrofit clients, mappers, repository impls
- Mappers convert: `Entity ↔ Domain model` and `DTO ↔ Domain model`
- Never expose Room entities or Retrofit DTOs to `ui/` or `domain/`

### `ui/` — Compose + ViewModels only
- ViewModels depend on `domain/` (use cases, repositories via interfaces) — never on `data/` directly
- Composables receive data and lambdas — no business logic
- State is `StateFlow<UiState>` exposed from ViewModel, collected with `collectAsStateWithLifecycle()`
- Use `viewModelScope.launch` for coroutines; `WhileSubscribed(5000)` for `stateIn()`

### `di/AppContainer.kt` — The only wiring point
- All repositories and services are lazy properties here
- ViewModels are created by factories that take `AppContainer` members
- No static singletons outside AppContainer

---

## Card System Extension Checklist

When adding a new `CardType`, update ALL 15 of these files in order. Missing any causes either a compile error or a silent runtime crash on card reorder:

1. `domain/card/CardType.kt` — add enum entry with `serialName`, `displayName`, `description`
2. `domain/model/settings/CardConfig.kt` — add `@Serializable @SerialName("<serialName>") data class XxxCardConfig` extending `CardConfig()`; must have `override val filter` and `override val filterRules`
3. `domain/card/CardRenderData.kt` — add `data class Xxx(val config: XxxCardConfig, ...) : CardRenderData()`
4. `domain/card/computers/XxxCardComputer.kt` — NEW FILE implementing `CardDataComputer<XxxCardConfig, CardRenderData.Xxx>`
5. `domain/card/CardComputerRegistry.kt` — add `XxxCardComputer()` to the `DEFAULT` list
6. `domain/card/CardUpdateHandler.kt` — add `fun updateCardXxxConfig(cardId: String, config: XxxCardConfig)`
7. `ui/components/cards/CardCallbacks.kt` — add `data class Xxx(...) : CardCallbacks()`
8. `ui/components/cards/CardWithCallbacks.kt` — add `data class Xxx(val renderData: CardRenderData.Xxx, val callbacks: CardCallbacks.Xxx, override val filterRules, override val baseFilter) : CardWithCallbacks()`
9. `ui/components/cards/CardUiBridge.kt` — add branch in `rememberCardWithCallbacks()`: compute branch (`is XxxCardConfig ->`) AND pair branch (`is CardRenderData.Xxx && config is XxxCardConfig ->`)
10. `ui/dashboard/components/XxxCard.kt` — NEW FILE: the Compose composable for this card
11. `ui/components/cards/UnifiedCardRenderer.kt` — add `is CardWithCallbacks.Xxx -> XxxCard(...)`
12. `data/repository/CardRepositoryImpl.kt` — add `is XxxCardConfig -> card.copy(order = index)` in ALL THREE `reorder*` `when` blocks (dashboard, accountDetail, accountsView)
13. `ui/viewmodel/CardLayoutViewModel.kt` — add `CardType.XXX ->` branch in `addDashboardCard()` when block
14. `ui/navigation/Screen.kt` — add route objects for any new screens
15. `ui/navigation/OssWalletNavGraph.kt` — wire new screens with composable() destinations

---

## New Service vs Extending Existing

**Create a new `domain/service/XxxService.kt`** when:
- The feature has its own domain concept (PlannedPayment, Budget, Loan)
- Logic is cohesive and unlikely to be needed across multiple services
- The service can be a pure Kotlin `object` (stateless computation)

**Extend `AggregationService`** when:
- The computation is a new breakdown/grouping of existing transaction data
- The result feeds into existing analytics views

**Extend `TransactionManager`** when:
- The operation creates, modifies, or links transactions as a side effect

---

## ViewModel Patterns

```kotlin
// Standard pattern
class XxxViewModel(
    private val repo: XxxRepository,
    private val settingsRepository: SettingsRepository
) : ViewModel() {

    val uiState: StateFlow<XxxUiState> = settingsRepository.currentProfileId
        .flatMapLatest { profileId -> repo.getItems(profileId) }
        .map { items -> XxxUiState.Success(items) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), XxxUiState.Loading)

    fun doAction(param: String) {
        viewModelScope.launch {
            // suspend work
        }
    }
}
```

- Always use `WhileSubscribed(5000)` — not `Eagerly` or `Lazily`
- Profile-scoped data: always `flatMapLatest` on `settingsRepository.currentProfileId`
- Keep `UiState` as a sealed class with `Loading`, `Success`, `Error` states

---

## Adding a New Widget Type (7-step checklist)

Each new widget type needs exactly these 7 artifacts. Missing any one causes either a crash or a manifest registration failure.

1. **`widget/XxxGlanceWidget.kt`** — extends `GlanceAppWidget`. All data loading in `provideGlance()` before `provideContent {}`. Wrap all fetching in `runCatching` so `provideContent` is always reached.
2. **`widget/XxxWidgetProvider.kt`** — extends `GlanceAppWidgetReceiver`. Override `glanceAppWidget = XxxGlanceWidget()`. Companion: `suspend fun updateAllWidgets(context)` calls `XxxGlanceWidget().updateAll(context)`.
3. **`widget/XxxWidgetConfig.kt`** — `@Serializable data class` with timeline, font size, and any widget-specific settings. Include a computed `val timelineLabel: String`.
4. **`widget/XxxWidgetPrefsStore.kt`** — DataStore-backed store keyed by widget ID. Copy `NetWorthWidgetPrefsStore` pattern, use a distinct DataStore name (`"xxx_widget_prefs"`).
5. **`widget/XxxWidgetConfigActivity.kt`** — `ComponentActivity` with Compose UI shown when the user places the widget. On save: `prefsStore.save(widgetId, config)` then update via `GlanceAppWidgetManager` with `getGlanceIds().firstOrNull { getAppWidgetId(it) == widgetId }`.
6. **Resources** — `res/xml/xxx_widget_info.xml` (copy `net_worth_widget_info.xml`, update `configure` class name and `initialLayout`) + `res/layout/widget_xxx.xml` (empty `FrameLayout` placeholder).
7. **`AndroidManifest.xml` + `WidgetUpdateCoordinator`** — register the receiver with `APPWIDGET_UPDATE` action + meta-data pointing to the xml. Register the config activity with `APPWIDGET_CONFIGURE` action. Add `launch { XxxGlanceWidget().updateAll(context) }` to `WidgetUpdateCoordinator.updateAll()`.
