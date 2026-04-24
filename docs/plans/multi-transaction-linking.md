# Multi-Transaction Linking Feature - Implementation Plan

## Executive Summary
Update the "Link Transaction" feature from supporting a single linked transaction to supporting multiple linked transactions. This enables use cases like:
- Linking a payment to multiple related transactions
- Linking a refund to its original purchase and related fees
- Linking split transactions to their parent
- Linking clone transactions to their source (current use case)

## Current State Analysis

### Existing Implementation
- **Field:** `linkedTransactionId: Long?` (nullable single ID)
- **Storage:** Direct column in `transactions` table
- **Current Uses:**
  1. **Transfer transactions:** Pairs source (expense) and target (income) transactions
  2. **Clone functionality:** Links cloned transaction to original
  3. **Manual linking:** User can link one transaction to another via UI

### Files Using `linkedTransactionId` (10 files)
1. `domain/model/Transaction.kt` - Domain model
2. `domain/model/dto/TransactionPrefillDTO.kt` - DTO for prefill
3. `data/local/entity/TransactionEntity.kt` - Database entity
4. `data/local/AppDatabase.kt` - Database schema & migrations
5. `data/local/BackupModels.kt` - Backup/restore model
6. `data/repository/TransactionRepositoryImpl.kt` - Business logic for transfers
7. `data/repository/BackupRepositoryImpl.kt` - Backup/restore logic
8. `ui/viewmodel/TransactionViewModel.kt` - Transaction operations
9. `ui/screens/TransactionEntryScreen.kt` - UI for linking
10. `dao/TransactionDao.kt` - No direct queries, but may need updates

### Current Database Schema
```sql
CREATE TABLE transactions (
    ...
    linkedTransactionId INTEGER,  -- Single nullable ID
    ...
)
```

## Proposed Architecture

### Approach: Junction Table (Recommended)
Create a separate `transaction_links` junction table for proper many-to-many relationship.

**Benefits:**
- Proper relational database design
- Efficient queries for linked transactions
- Easy to add/remove links
- Supports bi-directional linking queries
- Scalable for future features

**Trade-offs:**
- Requires migration
- Slightly more complex queries
- Additional table overhead (minimal)

### Alternative: Comma-Separated String (Not Recommended)
Store IDs as comma-separated string in existing column.

**Benefits:**
- No new table
- Simpler migration

**Trade-offs:**
- Cannot query efficiently (no indexes)
- Cannot use foreign key constraints
- String parsing overhead
- Not relational
- Harder to maintain consistency

## Detailed Implementation Plan

### Phase 1: Database Schema Changes

#### 1.1 Create Junction Table Entity
**File:** `data/local/entity/TransactionLinkEntity.kt` (NEW)

```kotlin
@Entity(
    tableName = "transaction_links",
    foreignKeys = [
        ForeignKey(
            entity = TransactionEntity::class,
            parentColumns = ["id"],
            childColumns = ["transactionId"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = TransactionEntity::class,
            parentColumns = ["id"],
            childColumns = ["linkedTransactionId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["transactionId"]),
        Index(value = ["linkedTransactionId"]),
        Index(value = ["transactionId", "linkedTransactionId"], unique = true)
    ]
)
data class TransactionLinkEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val transactionId: Long,
    val linkedTransactionId: Long
)
```

#### 1.2 Create DAO for Junction Table
**File:** `data/local/dao/TransactionLinkDao.kt` (NEW)

```kotlin
@Dao
abstract class TransactionLinkDao {
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    abstract suspend fun insertLink(link: TransactionLinkEntity): Long

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    abstract suspend fun insertLinks(links: List<TransactionLinkEntity>)

    @Query("DELETE FROM transaction_links WHERE transactionId = :transactionId AND linkedTransactionId = :linkedTransactionId")
    abstract suspend fun deleteLink(transactionId: Long, linkedTransactionId: Long)

    @Query("DELETE FROM transaction_links WHERE transactionId = :transactionId")
    abstract suspend fun deleteAllLinksForTransaction(transactionId: Long)

    @Query("SELECT linkedTransactionId FROM transaction_links WHERE transactionId = :transactionId")
    abstract fun getLinkedTransactionIds(transactionId: Long): Flow<List<Long>>

    @Query("SELECT transactionId FROM transaction_links WHERE linkedTransactionId = :linkedTransactionId")
    abstract fun getReverseLinkedTransactionIds(linkedTransactionId: Long): Flow<List<Long>>

    @Query("SELECT * FROM transactions WHERE id IN (SELECT linkedTransactionId FROM transaction_links WHERE transactionId = :transactionId)")
    abstract fun getLinkedTransactions(transactionId: Long): Flow<List<TransactionEntity>>

    @Query("DELETE FROM transaction_links")
    abstract suspend fun clearAll()
}
```

#### 1.3 Update TransactionEntity
**File:** `data/local/entity/TransactionEntity.kt`

```kotlin
// Remove: val linkedTransactionId: Long? = null
// Keep other fields unchanged
```

#### 1.4 Update Transaction Domain Model
**File:** `domain/model/Transaction.kt`

```kotlin
// Remove: val linkedTransactionId: Long? = null
// Keep other fields unchanged
```

#### 1.5 Update TransactionPrefillDTO
**File:** `domain/model/dto/TransactionPrefillDTO.kt`

```kotlin
// Remove: val linkedTransactionId: Long? = null
// Add: val linkedTransactionIds: List<Long> = emptyList()
// Keep other fields unchanged
```

#### 1.6 Update BackupModels
**File:** `data/local/BackupModels.kt`

```kotlin
// Remove: val linkedTransactionId: Long? = null
// Add: val linkedTransactionIds: String = "" // Comma-separated for backup
```

#### 1.7 Create Database Migration
**File:** `data/local/AppDatabase.kt`

```kotlin
// Increment version from 8 to 9
version = 9

val MIGRATION_8_9 = object : Migration(8, 9) {
    override fun migrate(database: SupportSQLiteDatabase) {
        // Create junction table
        database.execSQL("""
            CREATE TABLE transaction_links (
                id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                transactionId INTEGER NOT NULL,
                linkedTransactionId INTEGER NOT NULL,
                FOREIGN KEY(transactionId) REFERENCES transactions(id) ON DELETE CASCADE,
                FOREIGN KEY(linkedTransactionId) REFERENCES transactions(id) ON DELETE CASCADE
            )
        """)

        // Create indexes
        database.execSQL("CREATE INDEX index_transaction_links_transactionId ON transaction_links (transactionId)")
        database.execSQL("CREATE INDEX index_transaction_links_linkedTransactionId ON transaction_links (linkedTransactionId)")
        database.execSQL("CREATE UNIQUE INDEX index_transaction_links_transactionId_linkedTransactionId ON transaction_links (transactionId, linkedTransactionId)")

        // Migrate existing linkedTransactionId data to junction table
        database.execSQL("""
            INSERT INTO transaction_links (transactionId, linkedTransactionId)
            SELECT id, linkedTransactionId
            FROM transactions
            WHERE linkedTransactionId IS NOT NULL
        """)

        // Drop linkedTransactionId column from transactions table
        database.execSQL("""
            CREATE TABLE transactions_new (
                id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                title TEXT NOT NULL,
                amount TEXT NOT NULL,
                category TEXT NOT NULL,
                timestamp INTEGER NOT NULL,
                currency TEXT NOT NULL,
                transactionType TEXT NOT NULL,
                accountId TEXT NOT NULL,
                categoryId INTEGER,
                tags TEXT NOT NULL,
                paymentMethod TEXT NOT NULL,
                payee TEXT NOT NULL,
                note TEXT NOT NULL,
                recurring TEXT,
                profileId INTEGER NOT NULL,
                latitude REAL,
                longitude REAL,
                warrantyExpiration INTEGER,
                status TEXT NOT NULL,
                FOREIGN KEY(accountId) REFERENCES accounts(id) ON DELETE CASCADE,
                FOREIGN KEY(categoryId) REFERENCES categories(id) ON DELETE SET NULL
            )
        """)

        database.execSQL("""
            INSERT INTO transactions_new (id, title, amount, category, timestamp, currency, transactionType, accountId, categoryId, tags, paymentMethod, payee, note, recurring, profileId, latitude, longitude, warrantyExpiration, status)
            SELECT id, title, amount, category, timestamp, currency, transactionType, accountId, categoryId, tags, paymentMethod, payee, note, recurring, profileId, latitude, longitude, warrantyExpiration, status
            FROM transactions
        """)

        database.execSQL("DROP TABLE transactions")
        database.execSQL("ALTER TABLE transactions_new RENAME TO transactions")

        // Recreate indexes
        database.execSQL("CREATE INDEX index_transactions_accountId ON transactions (accountId)")
        database.execSQL("CREATE INDEX index_transactions_categoryId ON transactions (categoryId)")
        database.execSQL("CREATE INDEX index_transactions_profileId ON transactions (profileId)")
        database.execSQL("CREATE INDEX index_transactions_timestamp ON transactions (timestamp)")
        database.execSQL("CREATE INDEX index_transactions_status ON transactions (status)")
    }
}

// Add to migration chain
.addMigrations(MIGRATION_4_5, MIGRATION_5_6, MIGRATION_6_7, MIGRATION_7_8, MIGRATION_8_9)
```

#### 1.8 Update AppDatabase
**File:** `data/local/AppDatabase.kt`

```kotlin
abstract fun transactionLinkDao(): TransactionLinkDao
```

### Phase 2: Repository Layer Updates

#### 2.1 Update TransactionRepositoryImpl
**File:** `data/repository/TransactionRepositoryImpl.kt`

**Changes:**
- Remove `linkedTransactionId` from all transaction creation/updates
- Add methods for managing links:
  ```kotlin
  suspend fun addTransactionLink(transactionId: Long, linkedTransactionId: Long)
  suspend fun removeTransactionLink(transactionId: Long, linkedTransactionId: Long)
  suspend fun getLinkedTransactionIds(transactionId: Long): Flow<List<Long>>
  suspend fun getLinkedTransactions(transactionId: Long): Flow<List<Transaction>>
  ```

- Update `saveTransfer` to use link management:
  ```kotlin
  // Old: val finalSource = sourceTransaction.copy(id = sourceId, linkedTransactionId = targetId)
  // New:
  transactionLinkDao.insertLink(TransactionLinkEntity(transactionId = sourceId, linkedTransactionId = targetId))
  transactionLinkDao.insertLink(TransactionLinkEntity(transactionId = targetId, linkedTransactionId = sourceId))
  ```

- Update `addTransaction` and `updateTransaction` to accept `linkedTransactionIds: List<Long>` instead of `linkedTransactionId: Long?`

#### 2.2 Update BackupRepositoryImpl
**File:** `data/repository/BackupRepositoryImpl.kt`

**Changes:**
- Update backup serialization: Convert `List<Long>` to comma-separated string
- Update restore deserialization: Parse comma-separated string to `List<Long>`
- Migrate existing backup data if needed

### Phase 3: ViewModel Layer Updates

#### 3.1 Update TransactionViewModel
**File:** `ui/viewmodel/TransactionViewModel.kt`

**Changes:**
- Update `addTransaction` signature:
  ```kotlin
  // Old: linkedTransactionId: Long? = null
  // New: linkedTransactionIds: List<Long> = emptyList()
  ```

- Update `updateTransaction` signature:
  ```kotlin
  // Old: linkedTransactionId: Long? = null
  // New: linkedTransactionIds: List<Long> = emptyList()
  ```

- Add methods for link management:
  ```kotlin
  fun addTransactionLink(transactionId: Long, linkedTransactionId: Long)
  fun removeTransactionLink(transactionId: Long, linkedTransactionId: Long)
  ```

- Update `saveTransfer` to use new link management

- Update `quickCloneTransaction` to handle multiple links (if needed)

### Phase 4: UI Layer Updates

#### 4.1 Update TransactionEntryScreen
**File:** `ui/screens/TransactionEntryScreen.kt`

**Changes:**
- Change state from single ID to list:
  ```kotlin
  // Old: var linkedTransactionId by remember { mutableStateOf<Long?>(null) }
  // New: var linkedTransactionIds by remember { mutableStateOf<List<Long>>(emptyList()) }
  ```

- Update linked transaction display:
  - Show list of linked transactions instead of single
  - Allow adding multiple links
  - Allow removing individual links

- Update UI component:
  - Change "Link Transaction" button to show count (e.g., "Link Transaction (2)")
  - When clicked, show multi-select dialog instead of single-select
  - Display linked transactions as a list with remove buttons

- Update save logic:
  ```kotlin
  // Old: linkedTransactionId = linkedId
  // New: linkedTransactionIds = linkedIds
  ```

- Update transfer logic:
  - Ensure bi-directional linking for transfers
  - Handle existing links when converting transfer to regular transaction

#### 4.2 Update Transaction Detail Views
**Files:** Potentially multiple screens showing transaction details

**Changes:**
- Display all linked transactions
- Show count of linked transactions
- Allow viewing linked transaction details
- Allow adding/removing links from detail view

### Phase 5: DTO Updates

#### 5.1 Update TransactionPrefillDTO
**File:** `domain/model/dto/TransactionPrefillDTO.kt`

**Changes:**
- Update `fromTransaction` to handle multiple links
- Update clone logic to preserve links if needed:
  ```kotlin
  // Option 1: Clone preserves links
  linkedTransactionIds = transaction.linkedTransactionIds + listOf(transactionId)
  
  // Option 2: Clone only links to original
  linkedTransactionIds = listOf(transactionId)
  ```

### Phase 6: Testing Strategy

#### 6.1 Unit Tests
- Test TransactionLinkDao CRUD operations
- Test migration from old schema to new
- Test repository link management
- Test ViewModel link operations

#### 6.2 Integration Tests
- Test end-to-end link creation
- Test transfer creation with new linking
- Test clone functionality with new linking
- Test backup/restore with links

#### 6.3 UI Tests
- Test multi-select link dialog
- Test link display in transaction entry
- Test link display in transaction detail
- Test link removal

### Phase 7: Migration Considerations

#### 7.1 Data Migration
- Existing `linkedTransactionId` values will be migrated to junction table
- Bi-directional links will be created for transfers
- Single-directional links for other use cases

#### 7.2 Backward Compatibility
- Backup files with old schema need handling
- Consider versioning backup format

#### 7.3 Rollback Plan
- Keep migration reversible if possible
- Test migration on production-like data

## Implementation Phases (Recommended Order)

### Phase 1: Foundation (Week 1)
1. Create TransactionLinkEntity and TransactionLinkDao
2. Update database schema and migration
3. Update TransactionEntity and Transaction domain model
4. Write unit tests for DAO

### Phase 2: Repository Layer (Week 1-2)
1. Update TransactionRepositoryImpl
2. Add link management methods
3. Update BackupRepositoryImpl
4. Write integration tests

### Phase 3: ViewModel Layer (Week 2)
1. Update TransactionViewModel
2. Add link management methods
3. Update saveTransfer logic
4. Write ViewModel tests

### Phase 4: UI Layer - Part 1 (Week 2-3)
1. Update TransactionEntryScreen state
2. Update save logic
3. Write UI tests

### Phase 5: UI Layer - Part 2 (Week 3)
1. Create multi-select link dialog
2. Update link display components
3. Update transaction detail views
4. Write UI tests

### Phase 6: Testing & Polish (Week 3-4)
1. End-to-end testing
2. Performance testing
3. Migration testing
4. Bug fixes

### Phase 7: Documentation (Week 4)
1. Update API documentation
2. Create migration guide
3. Update user documentation

## Risk Assessment

### High Risk
- **Data loss during migration:** Mitigation: Test migration thoroughly, create backup before migration
- **Performance degradation:** Mitigation: Add proper indexes, test with large datasets
- **Transfer logic breaking:** Mitigation: Comprehensive testing of transfer flows

### Medium Risk
- **UI complexity increase:** Mitigation: Keep UI simple, use existing patterns
- **Backup/restore compatibility:** Mitigation: Version backup format, handle both schemas

### Low Risk
- **Code complexity increase:** Mitigation: Proper abstraction, documentation
- **Test coverage gaps:** Mitigation: Comprehensive test plan

## Success Criteria

1. ✅ Database migration completes without data loss
2. ✅ All existing transfer transactions work correctly
3. ✅ Clone functionality works with new linking
4. ✅ Users can link multiple transactions
5. ✅ Users can unlink transactions
6. ✅ Backup/restore preserves links
7. ✅ Performance remains acceptable
8. ✅ All tests pass
9. ✅ No regression in existing features

## Open Questions

1. **Link Directionality:** Should links be bi-directional or uni-directional?
   - Current: Bi-directional for transfers
   - Proposal: Bi-directional for all links (easier to query)

2. **Clone Link Behavior:** Should cloned transactions preserve all links or only link to original?
   - Option A: Preserve all links (might create circular references)
   - Option B: Only link to original (cleaner)
   - **Recommendation:** Option B

3. **UI Complexity:** How to display multiple links in limited space?
   - Option A: Show count, tap to expand
   - Option B: Show first 2, tap to see all
   - Option C: Scrollable list
   - **Recommendation:** Option A (count with expandable list)

4. **Link Types:** Should we distinguish between link types (transfer, clone, manual)?
   - Option A: Add link_type column to junction table
   - Option B: Infer from context
   - **Recommendation:** Option B for simplicity, can add later if needed

## Timeline Estimate

**Total Duration:** 4 weeks

- Week 1: Database + Repository layer
- Week 2: ViewModel + UI foundation
- Week 3: UI completion + testing
- Week 4: Polish + documentation + deployment

## Next Steps

1. Review this plan with stakeholders
2. Decide on open questions
3. Create detailed task breakdown
4. Begin Phase 1 implementation
