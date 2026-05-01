---
name: android-testing
description: Use this agent for writing and running tests in OssWallet — unit tests for domain services, ViewModel tests, Room DAO tests, and instrumented tests. Invoke when writing new tests or debugging test failures.
---

# OssWallet Testing Agent

## Test Location Convention

```
app/src/
├── test/java/com/osscabinet/ossWallet/        # JVM unit tests (fast, no Android runtime)
│   ├── domain/
│   │   ├── service/                           # Service tests (pure Kotlin, JUnit4)
│   │   └── model/                             # Model/helper tests
│   └── ui/
│       └── viewmodel/                         # ViewModel tests (needs coroutines-test)
└── androidTest/java/com/osscabinet/ossWallet/ # Instrumented tests (slow, needs device)
    └── data/
        └── local/                             # Room DAO tests (in-memory DB)
```

---

## Domain Service Tests (JUnit4, no Android)

Domain services are pure Kotlin — test them with plain JUnit4. No Robolectric, no Android runner.

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

**Key assertion library:** `assertThat(actual).isEqualByComparingTo(expected)` for `BigDecimal` (avoids scale mismatch). Use Google Truth (`com.google.truth:truth`) if available, otherwise JUnit `assertEquals`.

---

## ViewModel Tests (coroutines-test)

```kotlin
@OptIn(ExperimentalCoroutinesApi::class)
class PlannedPaymentViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()  // replaces Main dispatcher with TestDispatcher

    private val fakeRepo = FakePlannedPaymentRepository()
    private val fakeSettings = FakeSettingsRepository()
    private lateinit var viewModel: PlannedPaymentViewModel

    @Before
    fun setup() {
        viewModel = PlannedPaymentViewModel(fakeRepo, fakeSettings, mockTransactionManager)
    }

    @Test
    fun `uiState emits payments from repository`() = runTest {
        val payments = listOf(/* test data */)
        fakeRepo.emit(payments)

        val state = viewModel.uiState.value
        assertThat(state).isInstanceOf(XxxUiState.Success::class.java)
        assertThat((state as XxxUiState.Success).items).hasSize(1)
    }
}
```

**MainDispatcherRule** — create in `test/util/MainDispatcherRule.kt`:
```kotlin
class MainDispatcherRule(private val dispatcher: TestDispatcher = UnconfinedTestDispatcher()) : TestWatcher() {
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
        val passphrase = byteArrayOf()  // in-memory DB doesn't need encryption
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
        val entity = BudgetEntity(name = "Test", currency = "USD", limitAmount = BigDecimal("100"), period = "MONTHLY", profileId = 1)
        val id = dao.insert(entity)
        val retrieved = dao.getById(id)
        assertThat(retrieved?.name).isEqualTo("Test")
    }
}
```

**Note:** For SQLCipher-encrypted `AppDatabase`, instrumented tests need a real passphrase via `SecurePassphraseManager` OR can use `Room.inMemoryDatabaseBuilder` which skips encryption. Prefer in-memory for speed.

---

## Running Tests

```bash
# All unit tests
./gradlew test

# Single test class
./gradlew testDebugUnitTest --tests "com.osscabinet.ossWallet.domain.service.BudgetServiceTest"

# All instrumented tests (requires connected device or emulator)
./gradlew connectedAndroidTest

# Single instrumented test class
./gradlew connectedDebugAndroidTest --tests "com.osscabinet.ossWallet.data.local.BudgetDaoTest"
```

---

## What to Test Per Feature

For each new domain feature (PlannedPayment, Budget, Loan), the minimum test coverage is:

**Service tests (unit):**
- Happy path computation (correct values returned)
- Edge cases (zero amounts, empty lists, null optional fields)
- Date boundary cases (period start/end, month-end clamping)
- Multi-currency scenarios (exchange rate conversion accuracy)

**DAO tests (instrumented):**
- Insert + retrieve round-trip
- Flow emits on data change
- Filtered queries return correct subset
- Foreign key CASCADE deletes child records

**ViewModel tests (unit):**
- UiState transitions: Loading → Success → Error
- Actions invoke the correct repository methods
- Profile ID is correctly threaded through

---

## Fake Repositories Pattern

Avoid mocking repositories with Mockito — use manual fakes that provide `MutableStateFlow`:

```kotlin
class FakeBudgetRepository : BudgetRepository {
    private val _budgets = MutableStateFlow<List<Budget>>(emptyList())

    fun emit(budgets: List<Budget>) { _budgets.value = budgets }

    override fun getActiveBudgets(profileId: Int): Flow<List<Budget>> = _budgets
    override suspend fun insert(budget: Budget): Long { _budgets.value += budget; return 0L }
    // ... other methods
}
```
