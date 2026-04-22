# Transaction Status Field Implementation Plan

**Author**: Senior Android Architect  
**Date**: April 23, 2026  
**Version**: 1.0  
**Status**: Planning Phase

---

## Executive Summary

This document outlines the comprehensive plan for adding a `status` field to transaction records in OssWallet. The status field will have three possible values: `reconcile`, `cleared`, and `uncleared`. This enhancement requires changes across the entire application stack, from database layer to UI components, following Clean Architecture principles and Material 3 design guidelines.

---

## 1. Requirements Analysis

### 1.1 Functional Requirements
- **FR-1**: Add `status` field to transaction records with enum values: `RECONCILE`, `CLEARED`, `UNCLEARED`
- **FR-2**: Default value for new transactions should be `CLEARED`
- **FR-3**: Users must be able to change transaction status during creation/edit
- **FR-4**: Filtering system must support status-based filtering
- **FR-5**: Transaction lists must display status indicator
- **FR-6**: Database seeder must include status in seed data
- **FR-7**: Existing transactions must be migrated with default status `CLEARED`

### 1.2 Non-Functional Requirements
- **NFR-1**: Database migration must be non-destructive
- **NFR-2**: Performance impact must be minimal (add index on status column)
- **NFR-3**: UI must follow Material 3 design guidelines
- **NFR-4**: Code must maintain Clean Architecture separation
- **NFR-5**: All changes must be backward compatible

---

## 2. Architecture Impact Analysis

### 2.1 Layer-by-Layer Impact

```
┌─────────────────────────────────────────────────────────┐
│                    UI Layer (Presentation)                │
│  - TransactionEntryScreen.kt (Add status selection UI)   │
│  - AeonTransactionComponents.kt (Add status indicator)  │
│  - TransactionFilterSheet.kt (Add status filter UI)      │
│  - FilterSheetContent.kt (Add status filter UI)          │
│  - TransactionListScreen.kt (Display status)              │
└─────────────────────────────────────────────────────────┘
                            ↓
┌─────────────────────────────────────────────────────────┐
│                  ViewModel Layer                         │
│  - TransactionViewModel.kt (Handle status state)        │
│  - AnalyticsViewModel.kt (Consider status in analytics) │
│  - AccountDetailViewModel.kt (Display status)           │
└─────────────────────────────────────────────────────────┘
                            ↓
┌─────────────────────────────────────────────────────────┐
│                   Domain Layer                           │
│  - Transaction.kt (Add status field)                     │
│  - TransactionFilter.kt (Add status filter)              │
│  - TransactionMapper.kt (Map status)                    │
│  - TransactionStatus.kt (NEW: Enum definition)         │
└─────────────────────────────────────────────────────────┘
                            ↓
┌─────────────────────────────────────────────────────────┐
│                    Data Layer                            │
│  - TransactionEntity.kt (Add status column)             │
│  - TransactionDao.kt (Update queries if needed)          │
│  - AppDatabase.kt (Add migration 7→8)                   │
│  - DatabaseSeeder.kt (Add status to seed data)          │
│  - FilterEntity.kt (Add status filter field)            │
└─────────────────────────────────────────────────────────┘
```

### 2.2 Database Schema Changes

**Current Schema (Version 7)**:
```sql
CREATE TABLE transactions (
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
    linkedTransactionId INTEGER,
    profileId INTEGER NOT NULL,
    latitude REAL,
    longitude REAL,
    warrantyExpiration INTEGER,
    FOREIGN KEY(accountId) REFERENCES accounts(id) ON DELETE CASCADE,
    FOREIGN KEY(categoryId) REFERENCES categories(id) ON DELETE SET NULL
)
```

**New Schema (Version 8)**:
```sql
CREATE TABLE transactions (
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
    linkedTransactionId INTEGER,
    profileId INTEGER NOT NULL,
    latitude REAL,
    longitude REAL,
    warrantyExpiration INTEGER,
    status TEXT NOT NULL DEFAULT 'cleared',  -- NEW COLUMN
    FOREIGN KEY(accountId) REFERENCES accounts(id) ON DELETE CASCADE,
    FOREIGN KEY(categoryId) REFERENCES categories(id) ON DELETE SET NULL
)
```

**New Index**:
```sql
CREATE INDEX index_transactions_status ON transactions (status)
```

---

## 3. Implementation Plan

### Phase 1: Domain Layer (Foundation)
**Estimated Time**: 2-3 hours  
**Risk Level**: Low

#### 3.1.1 Create TransactionStatus Enum
**File**: `app/src/main/java/com/osscabinet/ossWallet/domain/model/TransactionStatus.kt` (NEW)

```kotlin
package com.osscabinet.ossWallet.domain.model

enum class TransactionStatus(val displayName: String) {
    RECONCILE("Reconcile"),
    CLEARED("Cleared"),
    UNCLEARED("Uncleared");

    companion object {
        fun fromString(value: String): TransactionStatus {
            return values().find { it.name.equals(value, ignoreCase = true) } 
                ?: UNCLEARED
        }
    }
}
```

#### 3.1.2 Update Transaction Domain Model
**File**: `app/src/main/java/com/osscabinet/ossWallet/domain/model/Transaction.kt`

**Changes**:
- Add `status: TransactionStatus = TransactionStatus.CLEARED` field
- Update any extension functions if they exist

#### 3.1.3 Update TransactionFilter
**File**: `app/src/main/java/com/osscabinet/ossWallet/domain/model/TransactionFilter.kt`

**Changes**:
- Add `statuses: List<TransactionStatus> = emptyList()` field

#### 3.1.4 Update TransactionMapper
**File**: `app/src/main/java/com/osscabinet/ossWallet/domain/model/TransactionMapper.kt`

**Changes**:
- Update `toDomain()` to map status string to enum
- Update `toEntity()` to map enum to string

---

### Phase 2: Data Layer (Persistence)
**Estimated Time**: 3-4 hours  
**Risk Level**: Medium

#### 3.2.1 Update TransactionEntity
**File**: `app/src/main/java/com/osscabinet/ossWallet/data/local/entity/TransactionEntity.kt`

**Changes**:
- Add `val status: String = "cleared"` field
- Add index for status column in Entity annotation

#### 3.2.2 Create Database Migration (7→8)
**File**: `app/src/main/java/com/osscabinet/ossWallet/data/local/AppDatabase.kt`

**Changes**:
- Add `MIGRATION_7_8` migration object
- Add `status` column with default value `'cleared'`
- Update database version to 8
- Add migration to migration list

**Migration SQL**:
```kotlin
val MIGRATION_7_8 = object : Migration(7, 8) {
    override fun migrate(database: SupportSQLiteDatabase) {
        // Add status column with default value
        database.execSQL("ALTER TABLE transactions ADD COLUMN status TEXT NOT NULL DEFAULT 'cleared'")
        
        // Create index for status
        database.execSQL("CREATE INDEX index_transactions_status ON transactions (status)")
    }
}
```

#### 3.2.3 Update TransactionDao
**File**: `app/src/main/java/com/osscabinet/ossWallet/data/local/dao/TransactionDao.kt`

**Changes**:
- Review all queries to ensure they handle status column
- Add query for filtering by status if needed
- Update summary queries if they need to consider status

#### 3.2.4 Update FilterEntity
**File**: `app/src/main/java/com/osscabinet/ossWallet/data/local/entity/FilterEntity.kt`

**Changes**:
- Add `val statuses: String = ""` field (comma-separated)
- Update migration if needed (may need separate migration)

#### 3.2.5 Update DatabaseSeeder
**File**: `app/src/main/java/com/osscabinet/ossWallet/data/seed/DatabaseSeeder.kt`

**Changes**:
- Update CSV parsing to include status column
- Add default status `'cleared'` if CSV doesn't include it
- Update `loadTransactionsFromCsv()` method
- Update seed CSV files to include status column

---

### Phase 3: ViewModel Layer
**Estimated Time**: 2-3 hours  
**Risk Level**: Low

#### 3.3.1 Update TransactionViewModel
**File**: `app/src/main/java/com/osscabinet/ossWallet/ui/viewmodel/TransactionViewModel.kt`

**Changes**:
- Add `status` state variable
- Update `saveTransaction()` to handle status
- Update `updateTransaction()` to handle status
- Update `loadTransaction()` to load status

#### 3.3.2 Update AnalyticsViewModel
**File**: `app/src/main/java/com/osscabinet/ossWallet/ui/viewmodel/AnalyticsViewModel.kt`

**Changes**:
- Review if analytics should consider status (likely not needed initially)
- Add status filtering if required

#### 3.3.3 Update AccountDetailViewModel
**File**: `app/src/main/java/com/osscabinet/ossWallet/ui/viewmodel/AccountDetailViewModel.kt`

**Changes**:
- Ensure transaction loading includes status
- Update UI state if needed

---

### Phase 4: UI Layer - Transaction Entry/Edit
**Estimated Time**: 4-5 hours  
**Risk Level**: Medium

#### 3.4.1 Update TransactionEntryScreen
**File**: `app/src/main/java/com/osscabinet/ossWallet/ui/screens/TransactionEntryScreen.kt`

**Changes**:
- Add status selection UI component
- Add status state variable
- Update form to include status field
- Update save logic to include status
- Material 3 design: Use DropdownMenu or SegmentedButton

**UI Component Suggestion**:
```kotlin
// Status Selection
var selectedStatus by remember { mutableStateOf(TransactionStatus.CLEARED) }

ExposedDropdownMenuBox(
    expanded = expanded,
    onExpandedChange = { expanded = !expanded }
) {
    OutlinedTextField(
        value = selectedStatus.displayName,
        onValueChange = {},
        readOnly = true,
        label = { Text("Status") },
        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded) },
        modifier = Modifier.menuAnchor()
    )
    ExposedDropdownMenu(
        expanded = expanded,
        onDismissRequest = { expanded = false }
    ) {
        TransactionStatus.values().forEach { status ->
            DropdownMenuItem(
                text = { Text(status.displayName) },
                onClick = {
                    selectedStatus = status
                    expanded = false
                }
            )
        }
    }
}
```

#### 3.4.2 Update TransactionEntryContent
**File**: `app/src/main/java/com/osscabinet/ossWallet/ui/screens/TransactionEntryScreen.kt` (inner composable)

**Changes**:
- Pass status parameter
- Handle status in edit mode
- Display current status when editing

---

### Phase 5: UI Layer - Transaction Display
**Estimated Time**: 3-4 hours  
**Risk Level**: Low

#### 3.5.1 Update AeonTransactionComponents
**File**: `app/src/main/java/com/osscabinet/ossWallet/ui/components/transactions/AeonTransactionComponents.kt`

**Changes**:
- Add status indicator to transaction item
- Use Material 3 Badge or Chip for status
- Color coding:
  - RECONCILE: Primary color
  - CLEARED: Success color (green)
  - UNCLEARED: Neutral/Warning color

**UI Component Suggestion**:
```kotlin
// Status Badge
SuggestionChip(
    onClick = { },
    label = { Text(transaction.status.displayName) },
    colors = SuggestionChipDefaults.suggestionChipColors(
        containerColor = when (transaction.status) {
            TransactionStatus.RECONCILE -> MaterialTheme.colorScheme.primaryContainer
            TransactionStatus.CLEARED -> Color(0xFF4CAF50).copy(alpha = 0.2f)
            TransactionStatus.UNCLEARED -> MaterialTheme.colorScheme.surfaceVariant
        }
    )
)
```

#### 3.5.2 Update TransactionListContent
**File**: `app/src/main/java/com/osscabinet/ossWallet/ui/screens/TransactionListContent.kt`

**Changes**:
- Ensure status is displayed in list items
- Consider adding status filter quick access

---

### Phase 6: UI Layer - Filtering System
**Estimated Time**: 4-5 hours  
**Risk Level**: Medium

#### 3.6.1 Update TransactionFilterSheet
**File**: `app/src/main/java/com/osscabinet/ossWallet/ui/components/transactions/TransactionFilterSheet.kt`

**Changes**:
- Add status filter section
- Use Material 3 FilterChip for multi-select
- Update filter state to include statuses

#### 3.6.2 Update FilterSheetContent
**File**: `app/src/main/java/com/osscabinet/ossWallet/ui/components/transactions/FilterSheetContent.kt`

**Changes**:
- Add status filter UI
- Handle status filter state
- Update apply filter logic

**UI Component Suggestion**:
```kotlin
// Status Filter Chips
Text("Status", style = MaterialTheme.typography.titleSmall)
Row(
    horizontalArrangement = Arrangement.spacedBy(8.dp)
) {
    TransactionStatus.values().forEach { status ->
        FilterChip(
            selected = selectedStatuses.contains(status),
            onClick = {
                selectedStatuses = if (selectedStatuses.contains(status)) {
                    selectedStatuses - status
                } else {
                    selectedStatuses + status
                }
            },
            label = { Text(status.displayName) },
            leadingIcon = if (selectedStatuses.contains(status)) {
                { Icon(Icons.Default.Check, contentDescription = null) }
            } else null
        )
    }
}
```

#### 3.6.3 Update FilterComponents
**File**: `app/src/main/java/com/osscabinet/ossWallet/ui/components/transactions/FilterComponents.kt`

**Changes**:
- Add status filter component
- Update filter building logic

#### 3.6.4 Update FilterUtils
**File**: `app/src/main/java/com/osscabinet/ossWallet/ui/util/FilterUtils.kt`

**Changes**:
- Add status filtering logic
- Update SQL query building to include status

---

### Phase 7: Testing & Validation
**Estimated Time**: 3-4 hours  
**Risk Level**: Low

#### 3.7.1 Database Migration Testing
- Test migration from version 7 to 8
- Verify default value is applied
- Verify index is created
- Test data integrity

#### 3.7.2 UI Testing
- Test status selection in transaction entry
- Test status display in transaction list
- Test status filtering
- Test edit mode preserves status

#### 3.7.3 Integration Testing
- Test full flow: create → display → filter → edit
- Test with existing data (migration)
- Test with new data (default status)

---

## 4. Risk Assessment & Mitigation

### 4.1 High-Risk Areas

| Risk | Impact | Probability | Mitigation Strategy |
|------|--------|-------------|---------------------|
| Database migration failure | Critical | Low | Use non-destructive migration (ALTER TABLE), test on backup data |
| Data loss during migration | Critical | Low | Use default value, backup before migration, rollback plan |
| Performance degradation | Medium | Low | Add index on status column, test with large datasets |
| UI inconsistency | Medium | Medium | Follow Material 3 guidelines, use existing components |

### 4.2 Rollback Plan
1. Keep migration 7→8 reversible if possible
2. Maintain version 7 database schema as backup
3. Feature flag status field if needed
4. Provide option to reset to default status

---

## 5. Timeline & Dependencies

### 5.1 Implementation Timeline (Total: 21-28 hours)

| Phase | Duration | Dependencies |
|-------|----------|--------------|
| Phase 1: Domain Layer | 2-3 hours | None |
| Phase 2: Data Layer | 3-4 hours | Phase 1 |
| Phase 3: ViewModel Layer | 2-3 hours | Phase 1, 2 |
| Phase 4: UI - Entry/Edit | 4-5 hours | Phase 1, 2, 3 |
| Phase 5: UI - Display | 3-4 hours | Phase 1, 2, 3 |
| Phase 6: UI - Filtering | 4-5 hours | Phase 1, 2, 3 |
| Phase 7: Testing | 3-4 hours | All phases |

### 5.2 Critical Path
Domain → Data → ViewModel → UI Entry → UI Display → UI Filtering → Testing

---

## 6. Success Criteria

### 6.1 Functional Success
- [ ] All transactions have status field
- [ ] Default status is `CLEARED` for new transactions
- [ ] Users can select status during transaction creation
- [ ] Status is displayed in transaction lists
- [ ] Filtering by status works correctly
- [ ] Database migration succeeds without data loss
- [ ] Seed data includes status values

### 6.2 Non-Functional Success
- [ ] Database performance not degraded
- [ ] UI follows Material 3 guidelines
- [ ] Code maintains Clean Architecture
- [ ] No regressions in existing functionality
- [ ] Migration is reversible

---

## 7. Post-Implementation Considerations

### 7.1 Future Enhancements
- Status-based analytics (cleared vs uncleared balance)
- Bulk status update (mark all as cleared)
- Status-based notifications (uncleared transactions reminder)
- Status history tracking
- Status workflow automation (auto-clear after X days)

### 7.2 Documentation Updates
- Update user guide to explain status field
- Update API documentation if applicable
- Update database schema documentation
- Update migration documentation

### 7.3 Monitoring
- Track status distribution in analytics
- Monitor query performance with status filter
- Collect user feedback on status feature

---

## 8. Appendix: File Change Summary

### 8.1 New Files (1)
1. `app/src/main/java/com/osscabinet/ossWallet/domain/model/TransactionStatus.kt`

### 8.2 Modified Files (15)

**Domain Layer (3)**:
1. `app/src/main/java/com/osscabinet/ossWallet/domain/model/Transaction.kt`
2. `app/src/main/java/com/osscabinet/ossWallet/domain/model/TransactionFilter.kt`
3. `app/src/main/java/com/osscabinet/ossWallet/domain/model/TransactionMapper.kt`

**Data Layer (5)**:
4. `app/src/main/java/com/osscabinet/ossWallet/data/local/entity/TransactionEntity.kt`
5. `app/src/main/java/com/osscabinet/ossWallet/data/local/AppDatabase.kt`
6. `app/src/main/java/com/osscabinet/ossWallet/data/local/dao/TransactionDao.kt`
7. `app/src/main/java/com/osscabinet/ossWallet/data/local/entity/FilterEntity.kt`
8. `app/src/main/java/com/osscabinet/ossWallet/data/seed/DatabaseSeeder.kt`

**ViewModel Layer (3)**:
9. `app/src/main/java/com/osscabinet/ossWallet/ui/viewmodel/TransactionViewModel.kt`
10. `app/src/main/java/com/osscabinet/ossWallet/ui/viewmodel/AnalyticsViewModel.kt`
11. `app/src/main/java/com/osscabinet/ossWallet/ui/viewmodel/AccountDetailViewModel.kt`

**UI Layer (4)**:
12. `app/src/main/java/com/osscabinet/ossWallet/ui/screens/TransactionEntryScreen.kt`
13. `app/src/main/java/com/osscabinet/ossWallet/ui/components/transactions/AeonTransactionComponents.kt`
14. `app/src/main/java/com/osscabinet/ossWallet/ui/components/transactions/TransactionFilterSheet.kt`
15. `app/src/main/java/com/osscabinet/ossWallet/ui/components/transactions/FilterSheetContent.kt`

---

## 9. Approval & Sign-Off

| Role | Name | Signature | Date |
|------|------|----------|------|
| Architect | [Pending] | | |
| Tech Lead | [Pending] | | |
| Product Owner | [Pending] | | |

---

**Document Status**: Ready for Review  
**Next Steps**: Schedule architecture review meeting
