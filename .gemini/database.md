# OssWallet — Database Directive

**Scope:** Room entities, DAOs, migrations, TypeConverters, SQLCipher passphrase handling, and AppDatabase registration.

---

## Goals

- Guarantee zero data loss across schema changes by enforcing explicit migrations.
- Preserve financial precision by using correct SQL and Kotlin types.
- Maintain multi-profile data isolation so one profile never sees another's records.
- Keep the encrypted database passphrase out of source code and logs.

---

## How to Achieve Them

Every schema change triggers: (1) a new entity or modified `@Entity` annotation, (2) an incremented `AppDatabase` version, and (3) a new `Migration` object with SQL that exactly mirrors the annotation. Always verify the SQL column names and types match the Kotlin field names and TypeConverters before committing.

---

## Core Rules

1. **Never use `fallbackToDestructiveMigration()`** — it silently destroys user data. A missing migration must crash, not silently wipe the database.
2. **Write a migration for every schema change** — including development-only changes on a feature branch.
3. **Migration SQL must exactly match the `@Entity` annotation**: column names, types, nullability, and default values.
4. **All new entities require `profileId: Int = 1`** — queries without `profileId` return data from all profiles, which is a data-isolation bug.

---

## Entity Checklist

```kotlin
@Entity(
    tableName = "my_table",
    foreignKeys = [
        ForeignKey(
            entity = AccountEntity::class,
            parentColumns = ["id"],
            childColumns = ["accountId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["profileId"]),    // always index profileId
        Index(value = ["accountId"]),     // always index FK columns
        Index(value = ["status"])         // index frequently filtered columns
    ]
)
data class MyEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val profileId: Int = 1,              // required — multi-profile support
    val amount: BigDecimal,              // stored as TEXT via TypeConverter
    val status: String = "ACTIVE",
    val accountId: Long
)
```

### ForeignKey `onDelete` Policy

| Policy | When to use |
|--------|------------|
| `CASCADE` | Child record is meaningless without parent (e.g., transactions when account is deleted) |
| `SET_NULL` | Child can exist independently with a null FK (e.g., a loan's optional linked account) |
| `RESTRICT` | Parent deletion must be blocked if children exist (use sparingly) |

### Index Naming

Room auto-generates names as `index_<tablename>_<columnname>`. Verify the generated name in the migration SQL when creating or dropping an index manually.

---

## TypeConverters

Existing converters are in `data/local/Converters.kt`:

| Kotlin type | SQL type | Converter |
|------------|----------|-----------|
| `BigDecimal` | `TEXT NOT NULL` | `BigDecimalConverter` |
| `LocalDateTime` | `INTEGER NOT NULL` | `DateTimeConverter` |
| `List<String>` | `TEXT NOT NULL` | `StringListConverter` |

**Rules:**
- `BigDecimal` is stored as `TEXT` — **never `REAL`**. Floating-point representation loses precision for financial values.
- Construct `BigDecimal` from `String` literals: `BigDecimal("10.50")` — not `BigDecimal(10.5)`.
- For complex lists (e.g., `List<Long>` for category IDs), use `kotlinx.serialization.json.Json.encodeToString()` in the **mapper** — not in a new TypeConverter.
- Do not create duplicate TypeConverters. Check `Converters.kt` before adding any.

---

## SQL Type Mapping

| Kotlin type | SQL DDL |
|------------|---------|
| `Long` / `Int` | `INTEGER NOT NULL` |
| `Long?` / `Int?` | `INTEGER` |
| `String` | `TEXT NOT NULL` |
| `String?` | `TEXT` |
| `BigDecimal` | `TEXT NOT NULL` |
| `Boolean` | `INTEGER NOT NULL DEFAULT 0` |
| `Float` | `REAL NOT NULL` |
| `Float?` | `REAL` |

---

## Migration Template

```kotlin
// In AppDatabase.kt
val MIGRATION_11_12 = object : Migration(11, 12) {
    override fun migrate(database: SupportSQLiteDatabase) {
        database.execSQL("""
            CREATE TABLE my_table (
                id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                profileId INTEGER NOT NULL,
                amount TEXT NOT NULL,
                status TEXT NOT NULL DEFAULT 'ACTIVE',
                accountId INTEGER NOT NULL,
                FOREIGN KEY(accountId) REFERENCES accounts(id) ON DELETE CASCADE
            )
        """.trimIndent())
        database.execSQL("CREATE INDEX index_my_table_profileId ON my_table (profileId)")
        database.execSQL("CREATE INDEX index_my_table_accountId ON my_table (accountId)")
    }
}
```

- Each `execSQL` call is a single SQL statement — do not chain multiple statements with semicolons in one call.
- Use `trimIndent()` on multiline strings for readable migration code.
- Test the migration path on a real device with a previous-version database before merging.

---

## DAO Patterns

```kotlin
@Dao
interface MyDao {
    // Reactive: returns Flow — used by ViewModels
    @Query("SELECT * FROM my_table WHERE profileId = :profileId AND status = 'ACTIVE' ORDER BY createdAt DESC")
    fun getActive(profileId: Int): Flow<List<MyEntity>>

    // One-shot: used by workers, widgets, and services
    @Query("SELECT * FROM my_table WHERE profileId = :profileId")
    suspend fun getAllSync(profileId: Int): List<MyEntity>

    @Query("SELECT * FROM my_table WHERE id = :id")
    suspend fun getById(id: Long): MyEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entity: MyEntity): Long

    @Update
    suspend fun update(entity: MyEntity)

    // Targeted update — avoids full fetch-modify-write cycle
    @Query("UPDATE my_table SET status = :status WHERE id = :id")
    suspend fun updateStatus(id: Long, status: String)

    @Query("DELETE FROM my_table WHERE id = :id")
    suspend fun deleteById(id: Long)
}
```

**Naming convention:**
- `Flow`-returning methods: present tense (`getActive`, `getAll`).
- `suspend` one-shot methods: add `Sync` suffix only when a `Flow` variant also exists for the same data (`getAllSync` alongside `getAll`).

**Security rule:** All query parameters use Room's `:param` binding. Never build a query string by concatenating user input — that is SQL injection.

---

## SQLCipher / Passphrase

The database is encrypted with SQLCipher. The passphrase is managed by `SecurePassphraseManager`.

**In application code:** Access the database exclusively through `AppContainer.db`. Never instantiate `AppDatabase` directly elsewhere.

**In widgets and workers:**

```kotlin
val passphrase = SecurePassphraseManager(context).getOrGeneratePassphrase()
val db = AppDatabase.getDatabase(context, passphrase)
```

**Security rules:**
- Never pass `byteArrayOf()` as the passphrase in production code — that opens a separate, empty, unencrypted database.
- Never log the passphrase value, even under `BuildConfig.DEBUG`.
- Never store the passphrase in `SharedPreferences` in plaintext — `SecurePassphraseManager` handles secure storage.

---

## AppDatabase Registration Checklist

When adding a new entity:

1. Add the entity class to the `@Database(entities = [...])` annotation.
2. Increment `version = N` by exactly 1.
3. Add an abstract DAO accessor: `abstract fun myDao(): MyDao`.
4. Add a `MIGRATION_N_M` object in the companion object.
5. Add `MIGRATION_N_M` to `.addMigrations(...)` in `getDatabase()`.
6. Verify that the DAO is exposed through `AppContainer` before wiring it into a repository.

---

## Best Practices

- Always include `profileId` in every `WHERE` clause that retrieves user data.
- Prefer `@Insert(onConflict = OnConflictStrategy.REPLACE)` for upsert semantics over separate insert + update logic.
- Use targeted `@Query("UPDATE ...")` for single-field updates — avoids reading and re-writing the entire entity.
- Run migration validation with `Room.databaseBuilder(...).addMigrations(...).build()` in a test to catch SQL errors before production.
- Keep DAO methods focused: one query per method. Do not combine filtering logic inside a DAO — apply filters in the service layer.

---

## What Not To Do

- Do not use `fallbackToDestructiveMigration()`.
- Do not store monetary values as `REAL` (use `TEXT` via TypeConverter).
- Do not construct `BigDecimal` from floating-point literals.
- Do not write raw SQL with user-supplied string concatenation.
- Do not pass `byteArrayOf()` as the production database passphrase.
- Do not expose Room entities to `ui/` or `domain/` — map to domain models in `data/mapper/`.
- Do not skip `profileId` in queries — missing it causes cross-profile data leakage.
- Do not create new TypeConverters that duplicate existing ones in `Converters.kt`.
- Do not use `allowMainThreadQueries()` outside of test code.
