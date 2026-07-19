# High-Level Architecture — OssWallet

OssWallet follows a **Clean Architecture** pattern combined with **MVVM** and a highly reactive **Unidirectional Data Flow (UDF)**.

## 1. Architectural Layers

### 1.1 Domain Layer (`com.osscabinet.ossWallet.domain`)
- **Pure Kotlin**: Zero `android.*` or `androidx.*` imports.
- **Single Source of Truth**: Interfaces (Repositories), Models, UseCases, and Services.
- **State Management**: UI State hierarchies (Sealed classes) live here.

### 1.2 Data Layer (`com.osscabinet.ossWallet.data`)
- **Implementation**: Implements domain repositories.
- **Android Aware**: Owns Room entities, DAOs, Retrofit clients, and Mappers.
- **Isolation**: Never expose Room entities or Retrofit DTOs to the UI.

### 1.3 UI Layer (`com.osscabinet.ossWallet.ui`)
- **Compose**: Purely declarative. Zero business logic in Composables.
- **ViewModels**: Orchestrate state using `StateFlow` and `collectAsStateWithLifecycle`.

---

## 2. Card Computing Engine (Checklist)

Adding a new `CardType` is a 15-step process to ensure exhaustive handling:
1.  **Registry**: Add enum to `CardType.kt`.
2.  **Config**: Create `@Serializable` `XxxCardConfig`.
3.  **Data**: Create `Xxx` data class in `CardRenderData.kt`.
4.  **Computer**: Implement `XxxCardComputer.kt`.
5.  **Registry**: Register in `CardComputerRegistry.kt`.
6.  **Handler**: Update `CardUpdateHandler.kt`.
7.  **Callbacks**: Add `Xxx` to `CardCallbacks.kt`.
8.  **Bridge**: Add to `CardWithCallbacks.kt`.
9.  **Bridge UI**: Add branches in `CardUiBridge.kt`.
10. **Composable**: Create `XxxCard.kt` using `AeonExpressiveCard`.
11. **Renderer**: Add to `UnifiedCardRenderer.kt`.
12. **Repo**: Update reorder logic in `CardRepositoryImpl.kt`.
13. **ViewModel**: Add to `CardLayoutViewModel.kt`.
14. **Navigation**: Add route to `Screen.kt`.
15. **Graph**: Wire in `OssWalletNavGraph.kt`.

---

## 3. Concurrency & Navigation

### 3.1 Threading Model
- **`Dispatchers.Main`**: UI updates.
- **`Dispatchers.IO`**: Database, DataStore, Network.
- **`Dispatchers.Default`**: Card computations, list grouping, regex filtering.

### 3.2 Navigation
- **Single Activity**: `MainActivity` hosts the `NavHost`.
- **Type-Safe**: Uses serializable objects for routes.
- **Logic**: Navigation is triggered via lambdas passed to screens; never use `NavController` directly in a Screen Composable.

---

## 4. Feature-Specific Logic
- **Unified Search**: Combines FTS4 prefix matching with category fallbacks.
- **Budget Rollover**: Parallel computation of past periods in `computeBudgetRollover`.
- **Empty Set Semantics**: In `TransactionFilter`, an empty set means "No Constraint" (matches all).
