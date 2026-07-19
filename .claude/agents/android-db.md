---
name: android-db
description: Use this agent for all Room database work in OssWallet — entities, DAOs, migrations, TypeConverters, and SQLCipher passphrase handling. Invoke when adding tables, modifying schema, writing DAO queries, or debugging database issues.
---

# OssWallet Database Agent

## Core Rules

1. **Never use `fallbackToDestructiveMigration()`** — it silently destroys all user data. If a migration is missing, the app must crash.
2. **Always write a migration** for every schema change, even in development. Increment `AppDatabase` version number.
3. **Migration SQL must exactly match the `@Entity` annotation** — column names, types, nullability, and defaults.
4. **All new entities require `profileId: Int = 1`** — the app supports multiple profiles; data without a profileId is invisible.

---

## Entity Checklist

When creating a new `@Entity`:

```kotlin
@Entity(
    tableName = "my_table",
    foreignKeys = [
        ForeignKey(
            entity = AccountEntity::class,
            parentColumns = ["id"],
            childColumns = ["accountId"],
            onDelete = ForeignKey.CASCADE   // use CASCADE for child records
        )
    ],
    indices = [
        Index(value = ["profileId"]),       // always index profileId
        Index(value = ["accountId"]),        // always index FK columns
        Index(value = ["status"])            // index frequently filtered columns
    ]
)
data class MyEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val profileId: Int = 1,                 // required for multi-profile support
    val amount: BigDecimal,                  // stored as TEXT via TypeConverter
    // ...
)
```

### ForeignKey `onDelete` policy:
- **CASCADE** — child record deleted when parent deleted (e.g., transactions on account delete)
- **SET_NULL** — FK becomes null when parent deleted (e.g., loan's linkedAccountId if account archived)
- **RESTRICT** — prevent parent deletion if children exist (use sparingly)

### Index naming convention: `index_<tablename>_<columnname>`
Example: `Index(value = ["profileId"])` generates `index_my_table_profileId`

---

## TypeConverters

`BigDecimal` is stored as `TEXT` — never `REAL` (floating point loses precision for financial values).

Existing converters are in `data/local/Converters.kt`. Do not create duplicate converters. The existing ones handle:
- `BigDecimal ↔ String`
- `LocalDateTime ↔ Long`
- `List<String> ↔ String` (comma-separated for simple lists)

For complex lists (e.g., `List<Long>` for category IDs), use `kotlinx.serialization.json.Json.encodeToString()` / `decodeFromString()` in the mapper — not a TypeConverter — to keep the converter file clean.

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
                optionalField INTEGER,
                FOREIGN KEY(accountId) REFERENCES accounts(id) ON DELETE CASCADE
            )
        """.trimIndent())
        database.execSQL("CREATE INDEX index_my_table_profileId ON my_table (profileId)")
        database.execSQL("CREATE INDEX index_my_table_status ON my_table (status)")
    }
}
```

SQL type mapping:
- `Long` / `Int` → `INTEGER NOT NULL`
- `Long?` / `Int?` → `INTEGER`
- `String` → `TEXT NOT NULL`
- `String?` → `TEXT`
- `BigDecimal` → `TEXT NOT NULL` (via TypeConverter)
- `Boolean` → `INTEGER NOT NULL DEFAULT 0` (Room stores as 0/1)
- `Float` → `REAL NOT NULL`
- `Float?` → `REAL`

---

## DAO Patterns

```kotlin
@Dao
interface MyDao {
    // Reactive queries always return Flow
    @Query("SELECT * FROM my_table WHERE profileId = :profileId AND status = 'ACTIVE' ORDER BY createdAt DESC")
    fun getActive(profileId: Int): Flow<List<MyEntity>>

    // One-shot queries for workers/widgets use suspend
    @Query("SELECT * FROM my_table WHERE profileId = :profileId")
    suspend fun getAllSync(profileId: Int): List<MyEntity>

    // Single-item lookup
    @Query("SELECT * FROM my_table WHERE id = :id")
    suspend fun getById(id: Long): MyEntity?

    // Upsert
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entity: MyEntity): Long

    @Update
    suspend fun update(entity: MyEntity)

    // Targeted update to avoid full-entity fetch-modify-write
    @Query("UPDATE my_table SET status = :status WHERE id = :id")
    suspend fun updateStatus(id: Long, status: String)

    @Query("DELETE FROM my_table WHERE id = :id")
    suspend fun deleteById(id: Long)
}
```

**Name convention:** Methods that return `Flow` use present tense (`getActive`, `getAll`). Methods that return `suspend` use `Sync` suffix only when a Flow variant also exists for the same data (`getAllSync` alongside `getAll`).

---

## SQLCipher / Passphrase

The database is encrypted with SQLCipher. The passphrase is managed by `SecurePassphraseManager`.

**In application code:** Always access the DB via `AppContainer.db` — never instantiate `AppDatabase` directly.

**In widgets and workers:** Use:
```kotlin
val passphrase = SecurePassphraseManager(context).getOrGeneratePassphrase()
val db = AppDatabase.getDatabase(context, passphrase)
```
Never pass `byteArrayOf()` as passphrase — that opens a separate, empty, unencrypted database.

---

## AppDatabase Registration Checklist

When a new entity is added, update `AppDatabase.kt`:
1. Add entity class to `@Database(entities = [...])` annotation
2. Increment `version = N`
3. Add abstract DAO accessor: `abstract fun myDao(): MyDao`
4. Add `MIGRATION_N_M` object in companion object
5. Add `MIGRATION_N_M` to `.addMigrations(...)` in `getDatabase()`
