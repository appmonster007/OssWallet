package com.osscabinet.ossWallet.data.local

import androidx.room.testing.MigrationTestHelper
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

private const val TEST_DB = "migration-test"

/**
 * Verifies [AppDatabase.MIGRATION_1_2] — the first real Room migration in this project — against
 * the exported v1/v2 schemas (see `core/data/schemas/`). A pre-existing transaction, seeded using
 * only columns that existed at v1, must survive the migration with a null `importBatchId`, and the
 * new `import_batches` table must exist and be usable afterward.
 */
@RunWith(AndroidJUnit4::class)
class AppDatabaseMigrationTest {

    @get:Rule
    val helper: MigrationTestHelper = MigrationTestHelper(
        InstrumentationRegistry.getInstrumentation(),
        AppDatabase::class.java
    )

    @Test
    fun migrate1To2_preservesExistingTransactions_andAddsImportTables() {
        // Create a version-1 database and seed it using only columns that existed at v1.
        helper.createDatabase(TEST_DB, 1).apply {
            execSQL(
                "INSERT INTO accounts " +
                    "(id, name, currencyCode, structureType, colorHex, icon, allowedCurrencies, profileId, isArchived, displayOrder) " +
                    "VALUES ('acc1', 'Checking', 'USD', 'BANK', '#fff', 'bank', 'USD', 1, 0, 0)"
            )
            execSQL(
                "INSERT INTO categories " +
                    "(id, name, iconName, colorHex, isDefault, parentId, nature, profileId) " +
                    "VALUES (1, 'Food', 'food', 0, 0, NULL, 'want', 1)"
            )
            execSQL(
                "INSERT INTO transactions " +
                    "(id, name, amount, timestamp, currency, transactionType, accountId, categoryId, tags, " +
                    "paymentMethod, payee, note, sourcePlannedPaymentId, profileId, gpsLocation, location, " +
                    "warrantyExpiration, status, targetAccountId, targetCurrency, transferRate, lastUpdated) " +
                    "VALUES (1, 'Groceries', '10.00', 1000, 'USD', 'expense', 'acc1', 1, '[]', " +
                    "'CASH', '', '', NULL, 1, NULL, NULL, NULL, 'CLEARED', NULL, NULL, '1', 1000)"
            )
            close()
        }

        // Migrate to version 2 and validate the result against the exported v2 schema.
        val migrated = helper.runMigrationsAndValidate(
            TEST_DB, 2, true, AppDatabase.MIGRATION_1_2
        )

        // The pre-existing transaction survives, with a null importBatchId.
        migrated.query("SELECT name, importBatchId FROM transactions WHERE id = 1").use { cursor ->
            assertTrue(cursor.moveToFirst())
            assertEquals("Groceries", cursor.getString(0))
            assertTrue(cursor.isNull(1))
        }

        // The new import_batches table exists and is usable.
        migrated.execSQL(
            "INSERT INTO import_batches " +
                "(id, sourceApp, createdAt, transactionCount, createdAccountIds, createdCategoryIds) " +
                "VALUES ('batch-1', 'GENERIC', 2000, 1, '[]', '[]')"
        )
        migrated.query("SELECT COUNT(*) FROM import_batches").use { cursor ->
            assertTrue(cursor.moveToFirst())
            assertEquals(1, cursor.getInt(0))
        }

        // The new import_templates table exists and is usable.
        migrated.execSQL(
            "INSERT INTO import_templates (sourceApp, mappingJson, duplicatePolicy, updatedAt) " +
                "VALUES ('GENERIC', '{}', 'SKIP', 3000)"
        )
        migrated.query("SELECT COUNT(*) FROM import_templates").use { cursor ->
            assertTrue(cursor.moveToFirst())
            assertEquals(1, cursor.getInt(0))
        }

        migrated.close()
    }
}
