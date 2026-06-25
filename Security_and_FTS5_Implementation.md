# Security and FTS5 Implementation Report

## 1. SQLCipher and FTS5 Integration

### 1.1 Inherited Encryption (Security Best Practice)
The project utilizes **SQLCipher** to provide full-disk encryption for the SQLite database. A key architectural decision was made to implement FTS5 (Full-Text Search) using a "Hybrid" approach rather than an external or unmanaged database file.

*   **The Implementation**: The `transactions_fts` virtual table is created within the same encrypted database file as the main `transactions` table.
*   **Security Benefit**: This ensures that the search index, which contains sensitive financial data (payee names, notes, transaction descriptions), **inherits the same 256-bit AES encryption** as the primary data. No sensitive search tokens are ever written to the device in plain text.
*   **Passphrase Management**: The system uses a `SecurePassphraseManager` to generate and retrieve a cryptographically secure 32-byte key. This key is passed to the `SupportOpenHelperFactory`, securing all tables, including virtual FTS tables, under a single security umbrella.

## 2. Resolving "Hard-Coded" Room Limitations

### 2.1 The FTS4 Metadata Shell
Room 2.x natively only supports FTS3 and FTS4. Its internal code generator hard-codes triggers and schema validation for these older versions. Specifically, Room's generated triggers are hard-coded to use the `docid` column, which was replaced by `rowid` in FTS5.

*   **The Issue**: Using `@Fts4` on an actual FTS5 table causes a fatal crash (`SQLiteException: no such column: docid`) because Room's invisible, auto-generated triggers try to sync data using a column that FTS5 does not recognize.
*   **The Fix**: 
    1.  We "lie" to the Room compiler using the `@Fts4` annotation to keep its object-mapping and reactivity (Flow/LiveData) logic intact.
    2.  We manually override the runtime implementation by dropping Room's "hard-coded" triggers in the `onOpen` callback.
    3.  We provide manual, FTS5-compliant triggers (`manual_fts_content_sync_...`) that use the correct `rowid` syntax.

### 2.2 Schema Integrity and Identity Hashes
Room uses an "Identity Hash" (stored in `room_master_table`) to verify that the database schema on disk matches the compiled code. When switching between managed and unmanaged FTS entities, this hash can become desynchronized.

*   **Current Solution**: The database version has been bumped to **26** to force a migration that re-aligns the identity hash and ensures the FTS5 table and triggers are correctly initialized regardless of previous states.
