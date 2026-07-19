# Database Directive — OssWallet

**Scope:** Room entities, DAOs, migrations, TypeConverters, SQLCipher passphrase handling, and AppDatabase registration.

## 1. Core Principles
- **No Data Loss**: Enforce explicit migrations. `fallbackToDestructiveMigration()` is strictly forbidden.
- **Precision**: Store monetary values as `TEXT` (via `BigDecimal`)—never `REAL`.
- **Isolation**: Every query must include a `profileId` filter to maintain multi-profile data isolation.
- **Security**: The database is encrypted with **SQLCipher** (AES-256).

---

## 2. Entity Checklist
When creating a new entity, ensure:
- `profileId: Int = 1` is present for isolation.
- Primary keys and Foreign Keys are indexed.
- `onDelete = ForeignKey.CASCADE` is used for child records that cannot exist without a parent.

### Example
```kotlin
@Entity(tableName = "my_table", indices = [Index(value = ["profileId"])])
data class MyEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val profileId: Int = 1,
    val amount: BigDecimal, // Stored as TEXT via TypeConverter
    val accountId: Long
)
```

---

## 3. TypeConverters (`Converters.kt`)
| Kotlin type | SQL type | Converter |
|------------|----------|-----------|
| `BigDecimal` | `TEXT NOT NULL` | `BigDecimalConverter` |
| `LocalDateTime` | `INTEGER NOT NULL` | `DateTimeConverter` |
| `List<String>` | `TEXT NOT NULL` | `StringListConverter` |

**Rule:** Always construct `BigDecimal` from `String` literals: `BigDecimal("10.50")`.

---

## 4. Migrations
Every schema change requires an explicit `Migration` object. 
- Revert FTS5 to **FTS4** if using Room's `@Fts4` to avoid `docid` mismatch issues.
- Use `manual_` prefix for FTS triggers to avoid Room's automatic (and sometimes conflicting) trigger generation.

---

## 5. Security & Passphrase
The database is initialized in `DatabaseModule` using a key provided by `SecurePassphraseManager`.
- **Widget/Worker Access**:
```kotlin
val passphrase = SecurePassphraseManager(context).getOrGeneratePassphrase()
val db = AppDatabase.getDatabase(context, passphrase)
```

---

## 6. What Not To Do
- **Do not** use `REAL` or `Float` for money.
- **Do not** write raw SQL via string concatenation (use `:param` binding).
- **Do not** skip `profileId` in queries (causes cross-profile leakage).
- **Do not** use `byteArrayOf()` as a production passphrase.
