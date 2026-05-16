# OssWallet — Architecture Directive

**Scope:** Clean Architecture, layer boundaries, dependency injection, card system, navigation, ViewModel design.

---

## Goals

- Enforce strict layer separation so business logic never leaks into the UI or data layers.
- Keep `domain/` free of Android platform types so it is independently testable.
- Route all dependency wiring through `AppContainer` — no hidden singletons.
- Prevent silent runtime crashes in the card registry by treating every `when` block as exhaustive.

---

## How to Achieve Them

Follow the dependency rule: `ui → domain ← data`. Every class lives in exactly one layer and depends only inward. The DI container (`di/AppContainer.kt`) is the only place where layers are wired together.

---

## Layer Rules

### `domain/` — Pure Kotlin only

- Contains: interfaces, models, use cases, services, domain enums.
- Zero `android.*` or `androidx.*` imports. No `Context` references.
- Services are either a Kotlin `object` (stateless) or a `class` with constructor-injected collaborators.
- Repository interfaces define the contract; all implementations belong in `data/`.

### `data/` — Android-aware implementations

- Implements all `domain/repository/` interfaces.
- Owns: Room entities, DAOs, Retrofit clients, mappers.
- Mappers convert `Entity ↔ Domain model` and `DTO ↔ Domain model`.
- Never expose Room entities or Retrofit DTOs outside this layer.

### `ui/` — Compose + ViewModels

- ViewModels depend on `domain/` (interfaces, services, use cases) — never on `data/` directly.
- Composables receive data and lambdas, do no business logic.
- State is `StateFlow<UiState>`, collected with `collectAsStateWithLifecycle()`.
- Use `viewModelScope.launch` for coroutines; `WhileSubscribed(5000)` for `stateIn()`.

### `di/AppContainer.kt` — The sole wiring point

- All repositories and services are declared as `lazy` properties here.
- ViewModels are created by factories that accept `AppContainer` members.
- No static singletons or `companion object` instances outside `AppContainer`.

---

## ViewModel Patterns

```kotlin
class XxxViewModel(
    private val repo: XxxRepository,
    private val settingsRepository: SettingsRepository
) : BaseViewModel() {

    val uiState: StateFlow<XxxUiState> = settingsRepository.currentProfileId
        .flatMapLatest { profileId -> repo.getItems(profileId) }
        .map { items -> XxxUiState.Success(items) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), XxxUiState.Loading)

    fun doAction(param: String) {
        viewModelScope.launch { /* suspend work */ }
    }
}
```

- Always `flatMapLatest` on `settingsRepository.currentProfileId` for profile-scoped data.
- `UiState` is a sealed class with `Loading`, `Success`, and `Error` variants.
- Use `GenericViewModelFactory` for simple ViewModels; create a dedicated factory for complex ones.

---

## Card System — 15-File Checklist

Adding a new `CardType` requires updating ALL of the following in order. Missing any causes a compile error or a silent runtime crash on card reorder:

1. `domain/card/CardType.kt` — add enum entry with `serialName`, `displayName`, `description`
2. `domain/model/settings/CardConfig.kt` — add `@Serializable @SerialName("<serialName>") data class XxxCardConfig` extending `CardConfig()`; include `override val filter` and `override val filterRules`
3. `domain/card/CardRenderData.kt` — add `data class Xxx(...) : CardRenderData()`
4. `domain/card/computers/XxxCardComputer.kt` — NEW FILE implementing `CardDataComputer<XxxCardConfig, CardRenderData.Xxx>`
5. `domain/card/CardComputerRegistry.kt` — add `XxxCardComputer()` to the `DEFAULT` list
6. `domain/card/CardUpdateHandler.kt` — add `fun updateCardXxxConfig(cardId: String, config: XxxCardConfig)`
7. `ui/components/cards/CardCallbacks.kt` — add `data class Xxx(...) : CardCallbacks()`
8. `ui/components/cards/CardWithCallbacks.kt` — add `data class Xxx(...)  : CardWithCallbacks()`
9. `ui/components/cards/CardUiBridge.kt` — add branch in `rememberCardWithCallbacks()` for both the compute branch (`is XxxCardConfig ->`) and the pair branch (`is CardRenderData.Xxx && config is XxxCardConfig ->`)
10. `ui/dashboard/components/XxxCard.kt` — NEW FILE: the Compose composable for this card
11. `ui/components/cards/UnifiedCardRenderer.kt` — add `is CardWithCallbacks.Xxx -> XxxCard(...)`
12. `data/repository/CardRepositoryImpl.kt` — add `is XxxCardConfig -> card.copy(order = index)` in ALL THREE `reorder*` `when` blocks (dashboard, accountDetail, accountsView)
13. `ui/viewmodel/CardLayoutViewModel.kt` — add `CardType.XXX ->` branch in `addDashboardCard()` `when` block
14. `ui/navigation/Screen.kt` — add route objects for any new screens this card needs
15. `ui/navigation/OssWalletNavGraph.kt` — wire new screens with `composable()` destinations

**`CardInput` invariant:** Every field added to `CardInput` must have a default value. Always use named parameters when constructing `CardInput`.

---

## New Service vs. Extending Existing

**Create a new `domain/service/XxxService.kt`** when:
- The feature owns a distinct domain concept (e.g., PlannedPayment, Budget, Loan).
- Logic is cohesive and self-contained.
- The service is a pure Kotlin `object` or an injected `class`.

**Extend `AggregationService`** when:
- The computation is a new breakdown or grouping of existing transaction data.

**Extend `TransactionManager`** when:
- The operation creates, modifies, or links transactions as a side effect.

---

## Navigation

- Single-activity (`MainActivity`). All routes are in `ui/navigation/OssWalletNavGraph.kt`.
- Routes defined in `ui/navigation/Screen.kt`.
- Context objects (`DashboardViewContext`, `TransactionFilter`, `AccountsFilterContext`) are passed through the nav graph for screen-to-screen state sharing.
- Never use `NavController` inside a screen composable directly — pass navigation callbacks as lambdas.

---

## Best Practices

- Prefer `object` for stateless domain services; prefer `class` when collaborators must be injected.
- Keep sealed class hierarchies (`UiState`, `CardRenderData`, `CardConfig`) exhaustive — treat unhandled branches as compile errors.
- Scope all repository queries by `profileId` — data without a profile association is invisible to the user.
- Prefer `Flow` for reactive data; `suspend` for one-shot operations.

---

## What Not To Do

- Do not import `android.*` or `androidx.*` in `domain/`.
- Do not instantiate `AppDatabase` directly — always use `AppContainer.db`.
- Do not expose Room entities or Retrofit DTOs to `ui/` or `domain/`.
- Do not add business logic to `@Composable` functions.
- Do not use `Eagerly` or `Lazily` with `stateIn()` — always use `WhileSubscribed(5000)`.
- Do not add a new `CardType` without completing all 15 checklist items — partial additions cause crashes.
- Do not create static singletons outside `AppContainer`.
- Do not hardcode string routes — always use the `Screen` sealed objects.
