# Adding a new backed-up entity

Backup/restore (`core/data/.../repository/BackupRepositoryImpl.kt`) does not discover new Room
tables automatically. Adding a new entity that should survive export/import requires touching
every one of these places — missing one means the entity silently vanishes from backups, or a
restore silently leaves it empty. There is no compile error for a missed spot.

1. **`domain/repository/BackupRepository.kt`** — add the entity to the `RestorableEntity` enum if
   it should be individually selectable/importable from a single CSV (most entities are).
2. **`BackupRepositoryImpl.createZipBackup()`** — add a `writeCsvToZip(...)` call for the new
   entity's table, filtering by `scope`/`currentProfileId` the same way sibling entities do (direct
   `profileId` column, or via a parent FK set like `templateIds`/`loanIds`).
3. **`BackupRepositoryImpl.restoreZipBackup()`** — add the matching `clearAll()` call inside the
   `database.withTransaction { ... }` block, in dependency order (a child table restores after the
   parent it references), and call the new `restoreX(File(tempDir, "x.csv"))` helper.
4. **A new `restoreX(file: File)` helper**, following the `parseCsv { p -> ... }` pattern used by
   every existing one — note the defensive `?:` fallbacks for every column, since older backups
   written before a column existed won't have it.
5. **`BackupRepositoryImpl.restoreZipBackupSelective()`** — three spots:
   - the FK-dependent expansion logic at the top (if the new entity depends on another, or another
     depends on it, add it to the relevant `safe.add(...)` block)
   - the `should` `when` block that decides which zip entries to extract
   - the `database.withTransaction` block that conditionally clears + restores it
6. **`BackupRepositoryImpl.detectRestorableEntityFromStream()`** — add a filename substring check
   and a header-sniffing heuristic, so a bare single-CSV import/verify can identify the file.
7. **`BackupRepositoryImpl.restoreSingleCsv()`** — add the `RestorableEntity.X -> restoreX(tempFile)`
   branch.
8. **The DAO's `clearAll()`** — needed by `deleteAllData()` and the restore paths above; add it if
   the DAO doesn't already have one.
9. **`RestorableEntitySelectorDialog`** (`ui/screens/settings/DataManagementScreen.kt`) — add the
   new entity to one of the existing groups (or a new group) so users can pick it during a
   selective restore.

## Known limitations (not fixed by the above, documented so they aren't re-discovered as "new" bugs)

- **Cross-device `ALL_PROFILES` restore risk.** A restore re-inserts rows with their original
  numeric `profileId`. Restoring an `ALL_PROFILES` backup onto a *different* device that already
  has profiles of its own can collide with or shadow those profiles' IDs. Scoped (`SINGLE_PROFILE`)
  backups don't have this problem in the same way, but cross-device restore in general isn't a
  supported/tested scenario.
- **Backup versioning.** There's no `schema_version` entry inside the zip. A backup written by an
  older app version with a different CSV column set restores via the `?:` fallbacks in each
  `restoreX` helper, not an explicit migration chain. If a column's *meaning* ever changes (not
  just gets added), those fallbacks won't catch it — only a real `BackupMigration_NtoN+1` step would.
