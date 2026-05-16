# OssWallet — Testing Directive

**Scope:** Unit tests for domain services, ViewModel tests with coroutines, Room DAO instrumented tests, fake repositories, and coverage expectations per feature.

---

## Goals

- Catch business-logic regressions before they reach production.
- Keep domain services fully testable without an Android runtime.
- Validate database migrations and DAO queries against a real Room/SQLite engine.
- Make test suites fast by isolating layers correctly — use in-memory databases and `TestDispatcher`, not real devices for pure logic.

---

## How to Achieve Them

Place tests in the correct source set. Use fake repositories (not mocks) for ViewModel tests. Test domain services with plain JUnit4 — no Android runner, no Robolectric. Test DAOs with an in-memory `AppDatabase` in `androidTest`. Cover the happy path, edge cases, and boundary conditions for every new feature.

---

## Test Location Convention

```
app/src/
├── test/java/com/osscabinet/ossWallet/          # JVM unit tests — fast, no Android runtime
│   ├── domain/
│   │   ├── service/                             # Service tests (pure Kotlin, JUnit4)
│   │   └── model/                              # Model / helper tests
│   └── ui/
│       └── viewmodel/                          # ViewModel tests (coroutines-test)
└── androidTest/java/com/osscabinet/ossWallet/   # Instrumented tests — requires device/emulator
    └── data/
        └── local/                              # Room DAO tests (in-memory DB)
```

---

## Domain Service Tests (JUnit4, no Android)

Domain services are pure Kotlin — test with plain JUnit4. No Robolectric, no Android runner.

```kotlin
class BudgetServiceTest {

    @Test
    fun `calculateSpent returns zero when no transactions match`() {
        val budget = Budget(
            id = 1, name = "Food", currency = "USD",
            limitAmount = BigDecimal("500"), period = BudgetPeriod.MONTHLY,
            categoryIds = listOf(1L), accountIds = emptyList(),
            profileId = 1
        )
        val result = BudgetService.calculateSpent(
            budget = budget,
            transactions = emptyList(),
            exchangeRates = emptyMap(),
            now = System.currentTimeMillis()
        )
        assertThat(result).isEqualByComparingTo(BigDecimal.ZERO)
    }
}
```

**Key assertion rule:** Use `.isEqualByComparingTo()` for `BigDecimal` — avoids scale mismatch failures (`BigDecimal("0")` vs `BigDecimal("0.00")`). Use Google Truth (`com.google.truth:truth`) assertions throughout.

---

## ViewModel Tests (coroutines-test)

```kotlin
@OptIn(ExperimentalCoroutinesApi::class)
class PlannedPaymentViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val fakeRepo = FakePlannedPaymentRepository()
    private val fakeSettings = FakeSettingsRepository()
    private lateinit var viewModel: PlannedPaymentViewModel

    @Before
    fun setup() {
        viewModel = PlannedPaymentViewModel(fakeRepo, fakeSettings)
    }

    @Test
    fun `uiState emits success when repository has items`() = runTest {
        val payments = listOf(PlannedPayment(id = 1, name = "Rent", amount = BigDecimal("1000"), profileId = 1))
        fakeRepo.emit(payments)

        val state = viewModel.uiState.value
        assertThat(state).isInstanceOf(PlannedPaymentUiState.Success::class.java)
        assertThat((state as PlannedPaymentUiState.Success).items).hasSize(1)
    }
}
```

**`MainDispatcherRule`** — create once in `test/util/MainDispatcherRule.kt` and reuse across all ViewModel tests:

```kotlin
class MainDispatcherRule(
    private val dispatcher: TestDispatcher = UnconfinedTestDispatcher()
) : TestWatcher() {
    override fun starting(description: Description) = Dispatchers.setMain(dispatcher)
    override fun finished(description: Description) = Dispatchers.resetMain()
}
```

---

## Room DAO Tests (instrumented, in-memory DB)

```kotlin
@RunWith(AndroidJUnit4::class)
class BudgetDaoTest {

    private lateinit var db: AppDatabase
    private lateinit var dao: BudgetDao

    @Before
    fun setup() {
        db = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            AppDatabase::class.java
        ).allowMainThreadQueries().build()
        dao = db.budgetDao()
    }

    @After
    fun teardown() = db.close()

    @Test
    fun insertAndRetrieveBudget() = runBlocking {
        val entity = BudgetEntity(
            name = "Test", currency = "USD",
            limitAmount = BigDecimal("100"), period = "MONTHLY",
            profileId = 1
        )
        val id = dao.insert(entity)
        val retrieved = dao.getById(id)
        assertThat(retrieved?.name).isEqualTo("Test")
    }

    @Test
    fun flowEmitsOnDataChange() = runTest {
        val flow = dao.getActive(profileId = 1)
        val entity = BudgetEntity(name = "Rent", currency = "USD", limitAmount = BigDecimal("800"), period = "MONTHLY", profileId = 1)
        dao.insert(entity)
        val result = flow.first()
        assertThat(result).hasSize(1)
    }
}
```

- In-memory `AppDatabase` does not require the SQLCipher passphrase — no need for `SecurePassphraseManager` in tests.
- `allowMainThreadQueries()` is only acceptable in test code.
- Always call `db.close()` in `@After` to avoid leaked database handles between tests.

---

## Fake Repositories Pattern

Use manual fakes backed by `MutableStateFlow` — not Mockito mocks:

```kotlin
class FakeBudgetRepository : BudgetRepository {
    private val _budgets = MutableStateFlow<List<Budget>>(emptyList())

    fun emit(budgets: List<Budget>) { _budgets.value = budgets }

    override fun getActiveBudgets(profileId: Int): Flow<List<Budget>> = _budgets
    override suspend fun insert(budget: Budget): Long {
        _budgets.value = _budgets.value + budget
        return _budgets.value.size.toLong()
    }
    override suspend fun deleteById(id: Long) {
        _budgets.value = _budgets.value.filter { it.id != id }
    }
}
```

Fakes are deterministic and do not require a mocking framework. They also exercise the interface contract, unlike mocks that can silently pass even when the interface changes.

---

## Coverage Expectations Per Feature

For every new domain feature (e.g., PlannedPayment, Budget, Loan), the minimum test coverage is:

### Service tests (unit)
- Happy path computation — correct values for normal input.
- Edge cases — zero amounts, empty lists, null optional fields.
- Date boundary cases — period start/end, month-end clamping, leap years.
- Multi-currency scenarios — exchange rate conversion accuracy using `BigDecimal`.

### DAO tests (instrumented)
- Insert + retrieve round-trip — data survives serialization.
- `Flow` emits on data change — reactive queries reflect inserts and deletes.
- Filtered queries return correct subset — `profileId` and status filtering work.
- Foreign key `CASCADE` — deleting a parent removes child records.

### ViewModel tests (unit)
- `UiState` transitions: `Loading → Success` and `Loading → Error`.
- Actions invoke the correct repository methods.
- `profileId` is correctly threaded through via `flatMapLatest`.

---

## Running Tests

```bash
# All JVM unit tests
./gradlew test

# Single unit test class
./gradlew testDebugUnitTest --tests "com.osscabinet.ossWallet.domain.service.BudgetServiceTest"

# All instrumented tests (requires connected device or emulator)
./gradlew connectedAndroidTest

# Single instrumented test class
./gradlew connectedDebugAndroidTest --tests "com.osscabinet.ossWallet.data.local.BudgetDaoTest"
```

---

## Best Practices

- Name test functions with backtick strings: `` `returns zero when no transactions match` `` — this appears clearly in the test report.
- Use `@Before` for shared setup — never repeat construction logic across test methods.
- Keep each test focused on a single behaviour — one assertion concept per test (not one `assertThat` call).
- Test the domain model with explicit `BigDecimal` string construction: `BigDecimal("1.23")` not `1.23.toBigDecimal()`.
- Seed test data inline in each test, not as shared class-level state — shared state causes test-ordering bugs.
- For `Flow`-returning DAOs, use `flow.first()` (with `runTest`) rather than `runBlocking` for coroutine correctness.

---

## What Not To Do

- Do not use Mockito or MockK to mock repositories in ViewModel tests — use manual `Fake*` implementations.
- Do not test Composables in unit tests — UI correctness is verified by running the app.
- Do not use `runBlocking` in ViewModel tests — use `runTest` from `kotlinx-coroutines-test`.
- Do not use `allowMainThreadQueries()` in production code — only in instrumented tests.
- Do not assert on `BigDecimal` with `assertEquals` — use `isEqualByComparingTo()` to avoid scale mismatches.
- Do not share mutable test state across test methods — each test must be independently runnable.
- Do not skip migration tests when changing the Room schema — a migration bug destroys user data silently.
- Do not use `byteArrayOf()` as a production passphrase — acceptable only in `Room.inMemoryDatabaseBuilder` test setup.
