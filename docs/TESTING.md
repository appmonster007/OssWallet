# Testing Guide — OssWallet

**Scope:** Unit tests, instrumented tests, fake repositories, and coverage requirements.

## 1. Test Locations
- **`app/src/test/`**: JVM unit tests (Services, ViewModels). Fast, no Android runtime.
- **`app/src/androidTest/`**: Instrumented tests (DAOs, In-memory DB). Requires device/emulator.

---

## 2. Unit Testing Strategy

### 2.1 Domain Services
Pure Kotlin logic should be tested with plain JUnit4 and Truth assertions.
- Use `isEqualByComparingTo()` for `BigDecimal` comparisons to ignore scale.

### 2.2 ViewModels & Coroutines
- Use `runTest` from `kotlinx-coroutines-test`.
- Use a `MainDispatcherRule` to override the Main dispatcher in tests.
- **Fakes over Mocks**: Use manual `FakeRepository` implementations backed by `MutableStateFlow` instead of Mockito/MockK.

---

## 3. Instrumented Testing (DAOs)
- Use `Room.inMemoryDatabaseBuilder()` in `@Before`.
- Always close the DB in `@After`.
- Verify `Flow` emissions using `flow.first()`.

---

## 4. Coverage Expectations
For every new feature, the following must be covered:
- **Service**: Happy path, edge cases (zero/null), and multi-currency math.
- **DAO**: Insert/Retrieve, `Flow` reactivity, and `profileId` isolation.
- **ViewModel**: `UiState` transitions and repository interaction.

---

## 5. Build Commands
```bash
# Run all JVM tests
./gradlew test

# Run all instrumented tests
./gradlew connectedAndroidTest
```

---

## 6. Best Practices
- Use backtick function names: `` `returns zero on empty list` ``.
- Seed test data **inline** in each test method to avoid shared-state bugs.
- Do not use `allowMainThreadQueries()` in production code—only in tests.
