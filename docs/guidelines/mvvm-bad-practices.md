# MVVM & Architectural Bad Practices to Avoid

This document identifies anti-patterns and technical debt found or commonly occurring in Android projects that should be refactored for better maintainability.

## 1. Architectural Anti-Patterns

### ❌ Manual Dependency Injection (Boilerplate)
*   **The Issue:** Creating `ViewModelFactory` for every ViewModel and passing dependencies manually through an `AppContainer`.
*   **Why it's bad:** It leads to massive amounts of boilerplate code and makes the `NavGraph` or `Activities` cluttered with instantiation logic.
*   **Fix:** Use a DI framework like **Hilt** or **Koin** to automate dependency injection.

### ❌ Repository Knowing About UI (Context Leakage)
*   **The Issue:** Passing `Context` into a Repository implementation (e.g., to update Widgets or access SharedPreferences).
*   **Why it's bad:** It breaks the separation of concerns. The data layer should not know about the Android UI framework or Widgets. It makes unit testing almost impossible without mocks of the entire Android system.
*   **Fix:** Use a separate `WorkManager` task or an `Observer` pattern where the UI/App layer listens for data changes and updates widgets independently.

### ❌ "God" ViewModels
*   **The Issue:** ViewModels containing hundreds of lines of business logic, calculations (like Net Worth or Currency conversion), and data formatting.
*   **Why it's bad:** ViewModels become "Fat," hard to read, and impossible to unit test effectively. They should only manage UI state and delegate logic.
*   **Fix:** Move business logic to **UseCases** and formatting to **UI Mappers**.

## 2. Code & Implementation Bad Practices

### ❌ Business Logic in the UI Layer
*   **The Issue:** Performing calculations or data filtering directly inside a `@Composable` or `Activity`.
*   **Why it's bad:** UI code should be "dumb." Logic in the UI is hard to test and leads to bugs during recomposition.
*   **Fix:** Move all logic to the ViewModel or UseCase.

### ❌ Exposing Mutable State
*   **The Issue:** Exposing `MutableStateFlow` directly from a ViewModel.
*   **Why it's bad:** Any class can then modify the state from outside the ViewModel, breaking the Unidirectional Data Flow (UDF).
*   **Fix:** Always expose an immutable `StateFlow` using `.asStateFlow()`.

### ❌ Hardcoded Magic Strings
*   **The Issue:** Using hardcoded strings for database tables, shared preference keys, or default values (e.g., `"USD"`, `"profile_1"`).
*   **Why it's bad:** Leads to "String typing" errors where a typo causes a silent crash or logic failure.
*   **Fix:** Use `const val` in a `Constants` file or `sealed classes` for types.

## 3. Data & Concurrency Issues

### ❌ Blocking the Main Thread
*   **The Issue:** Running database queries or heavy calculations on `Dispatchers.Main`.
*   **Why it's bad:** Causes UI "jank," dropped frames, and ANR (Application Not Responding) dialogs.
*   **Fix:** Use `Dispatchers.IO` for disk/network and `Dispatchers.Default` for CPU-intensive tasks.

### ❌ Neglecting Process Death
*   **The Issue:** Storing temporary user input only in ViewModel variables without using `SavedStateHandle`.
*   **Why it's bad:** If the system kills the app in the background, the user loses their progress when they return.
*   **Fix:** Use `SavedStateHandle` to persist simple state across process death.
