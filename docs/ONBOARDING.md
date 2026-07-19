# Onboarding & Testing Guide — OssWallet

Welcome to the OssWallet development team. This guide covers setup and the project's testing philosophy.

## 1. Setup

### 1.1 Prerequisites
- **JDK**: 17 (Targeting Java 11 bytecode).
- **SDK**: Compile/Target 36, Min 26.
- **Local Properties**: Add `CURRENCY_API_KEY=your_key` to `local.properties`.

### 1.2 Build Commands
```bash
./gradlew assembleDebug   # Assemble the app
./gradlew test             # Run all JVM unit tests
./gradlew connectedCheck   # Run all instrumented tests (emulator required)
```

---

## 2. Testing Philosophy

We use a layered testing approach to ensure stability.

### 2.1 Test Locations
- **`app/src/test/`**: JVM unit tests for Domain Services and ViewModels.
- **`app/src/androidTest/`**: Instrumented tests for DAOs and Database migrations.

### 2.2 Core Testing Rules
- **Fakes Over Mocks**: Use manual `FakeRepository` implementations (backed by `MutableStateFlow`) instead of Mockito/MockK.
- **MainDispatcherRule**: Mandatory for ViewModel tests to handle `Dispatchers.Main` in unit tests.
- **BigDecimal Assertions**: Use `assertThat(a).isEqualByComparingTo(b)` to avoid scale mismatch failures.
- **In-Memory DB**: Use `Room.inMemoryDatabaseBuilder()` for DAO tests.

---

## 3. Contribution Rules
1. **Never use `Double` for Money**: Always `BigDecimal`.
2. **Profile Scoping**: Always include `profileId` in repository and DAO queries.
3. **Typography**: Use `AppTextStyles` from our design system.
4. **No Destructive Migrations**: Never use `fallbackToDestructiveMigration()`.
