package com.osscabinet.ossWallet.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.osscabinet.ossWallet.data.local.dao.AccountDao
import com.osscabinet.ossWallet.data.local.dao.BudgetDao
import com.osscabinet.ossWallet.data.local.dao.CategoryDao
import com.osscabinet.ossWallet.data.local.dao.CurrencyDao
import com.osscabinet.ossWallet.data.local.dao.FilterDao
import com.osscabinet.ossWallet.data.local.dao.LoanDao
import com.osscabinet.ossWallet.data.local.dao.LoanPaymentDao
import com.osscabinet.ossWallet.data.local.dao.PlannedPaymentDao
import com.osscabinet.ossWallet.data.local.dao.PlannedPaymentOccurrenceDao
import com.osscabinet.ossWallet.data.local.dao.ProfileDao
import com.osscabinet.ossWallet.data.local.dao.TagDao
import com.osscabinet.ossWallet.data.local.dao.TemplateDao
import com.osscabinet.ossWallet.data.local.dao.TransactionDao
import com.osscabinet.ossWallet.data.local.dao.TransactionLinkDao
import com.osscabinet.ossWallet.data.local.entity.AccountEntity
import com.osscabinet.ossWallet.data.local.entity.BudgetEntity
import com.osscabinet.ossWallet.data.local.entity.CategoryEntity
import com.osscabinet.ossWallet.data.local.entity.CurrencyRateEntity
import com.osscabinet.ossWallet.data.local.entity.FilterEntity
import com.osscabinet.ossWallet.data.local.entity.LoanEntity
import com.osscabinet.ossWallet.data.local.entity.LoanPaymentEntity
import com.osscabinet.ossWallet.data.local.entity.PlannedPaymentEntity
import com.osscabinet.ossWallet.data.local.entity.PlannedPaymentOccurrenceEntity
import com.osscabinet.ossWallet.data.local.entity.ProfileEntity
import com.osscabinet.ossWallet.data.local.entity.TagEntity
import com.osscabinet.ossWallet.data.local.entity.TemplateEntity
import com.osscabinet.ossWallet.data.local.entity.TransactionEntity
import com.osscabinet.ossWallet.data.local.entity.TransactionFtsEntity
import com.osscabinet.ossWallet.data.local.entity.TransactionLinkEntity
import com.osscabinet.ossWallet.data.local.entity.TransactionTagCrossRef
import com.osscabinet.ossWallet.domain.model.SystemCategory
import net.zetetic.database.sqlcipher.SupportOpenHelperFactory

@Database(
    entities = [
        TransactionEntity::class,
        TransactionFtsEntity::class,
        TransactionLinkEntity::class,
        TransactionTagCrossRef::class,
        CurrencyRateEntity::class,
        AccountEntity::class,
        CategoryEntity::class,
        TagEntity::class,
        ProfileEntity::class,
        TemplateEntity::class,
        FilterEntity::class,
        PlannedPaymentEntity::class,
        PlannedPaymentOccurrenceEntity::class,
        BudgetEntity::class,
        LoanEntity::class,
        LoanPaymentEntity::class
    ],
    version = 28,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun transactionDao(): TransactionDao
    abstract fun transactionLinkDao(): TransactionLinkDao
    abstract fun currencyDao(): CurrencyDao
    abstract fun accountDao(): AccountDao
    abstract fun categoryDao(): CategoryDao
    abstract fun tagDao(): TagDao
    abstract fun profileDao(): ProfileDao
    abstract fun templateDao(): TemplateDao
    abstract fun filterDao(): FilterDao
    abstract fun plannedPaymentDao(): PlannedPaymentDao
    abstract fun plannedPaymentOccurrenceDao(): PlannedPaymentOccurrenceDao
    abstract fun budgetDao(): BudgetDao
    abstract fun loanDao(): LoanDao
    abstract fun loanPaymentDao(): LoanPaymentDao

    companion object {
        init {
            System.loadLibrary("sqlcipher")
        }

        val MIGRATION_27_28 = object : Migration(27, 28) {
            override fun migrate(db: SupportSQLiteDatabase) {
                // Add profileId to categories — existing rows assigned to profile 1.
                db.execSQL("ALTER TABLE categories ADD COLUMN profileId INTEGER NOT NULL DEFAULT 1")
                db.execSQL("DROP INDEX IF EXISTS `index_categories_name`")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_categories_name_profileId` ON `categories` (`name`, `profileId`)")

                // Tags: add profileId, then recreate table to replace the global name-unique
                // index with a per-profile (name, profileId) unique index.
                db.execSQL("ALTER TABLE tags ADD COLUMN profileId INTEGER NOT NULL DEFAULT 1")
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS tags_new (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        name TEXT NOT NULL,
                        colorHex INTEGER NOT NULL,
                        profileId INTEGER NOT NULL DEFAULT 1
                    )
                """.trimIndent()
                )
                db.execSQL("INSERT INTO tags_new (id, name, colorHex, profileId) SELECT id, name, colorHex, profileId FROM tags")
                db.execSQL("DROP TABLE tags")
                db.execSQL("ALTER TABLE tags_new RENAME TO tags")
                db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS index_tags_name_profileId ON tags (name, profileId)")
            }
        }

        val MIGRATION_26_27 = object : Migration(26, 27) {
            override fun migrate(db: SupportSQLiteDatabase) {
                upgradeToFts4(db)
            }
        }

        val MIGRATION_25_26 = object : Migration(25, 26) {
            override fun migrate(db: SupportSQLiteDatabase) {
                // Fix for identity hash mismatch and ensuring FTS5 is stable
                upgradeToFts5(db)
            }
        }

        val MIGRATION_24_25 = object : Migration(24, 25) {
            override fun migrate(db: SupportSQLiteDatabase) {
                // Ensure FTS5 is correctly set up and Room triggers are gone
                upgradeToFts5(db)
            }
        }

        val MIGRATION_23_24 = object : Migration(23, 24) {
            override fun migrate(db: SupportSQLiteDatabase) {
                // Ensure FTS5 is correctly set up after removing @Fts4 annotation
                upgradeToFts5(db)
            }
        }

        val MIGRATION_22_23 = object : Migration(22, 23) {
            override fun migrate(db: SupportSQLiteDatabase) {
                upgradeToFts5(db)
            }
        }

        val MIGRATION_21_22 = object : Migration(21, 22) {
            override fun migrate(db: SupportSQLiteDatabase) {
                // Upgrade from FTS4 to FTS5 for better search performance and ranking.
                // FTS5 uses a different delete mechanism: special 'delete' command insert.
                db.execSQL("DROP TRIGGER IF EXISTS transactions_fts_content_sync_ins")
                db.execSQL("DROP TRIGGER IF EXISTS transactions_fts_content_sync_upd")
                db.execSQL("DROP TRIGGER IF EXISTS transactions_fts_content_sync_del")
                db.execSQL("DROP TABLE IF EXISTS transactions_fts")

                db.execSQL(
                    "CREATE VIRTUAL TABLE IF NOT EXISTS `transactions_fts` " +
                            "USING fts5(note, payee, name, content=`transactions`, content_rowid=`id`)"
                )
                db.execSQL(
                    """
                    CREATE TRIGGER IF NOT EXISTS transactions_fts_content_sync_ins AFTER INSERT ON `transactions` BEGIN
                      INSERT INTO transactions_fts(rowid, note, payee, name) VALUES (new.id, new.note, new.payee, new.name);
                    END
                    """.trimIndent()
                )
                db.execSQL(
                    """
                    CREATE TRIGGER IF NOT EXISTS transactions_fts_content_sync_upd AFTER UPDATE ON `transactions` BEGIN
                      INSERT INTO transactions_fts(transactions_fts, rowid, note, payee, name) VALUES ('delete', old.id, old.note, old.payee, old.name);
                      INSERT INTO transactions_fts(rowid, note, payee, name) VALUES (new.id, new.note, new.payee, new.name);
                    END
                    """.trimIndent()
                )
                db.execSQL(
                    """
                    CREATE TRIGGER IF NOT EXISTS transactions_fts_content_sync_del AFTER DELETE ON `transactions` BEGIN
                      INSERT INTO transactions_fts(transactions_fts, rowid, note, payee, name) VALUES ('delete', old.id, old.note, old.payee, old.name);
                    END
                    """.trimIndent()
                )
                // Rebuild the FTS5 index from existing transactions.
                db.execSQL("INSERT INTO transactions_fts(rowid, note, payee, name) SELECT id, note, payee, name FROM `transactions`")
            }
        }

        val MIGRATION_20_21 = object : Migration(20, 21) {
            override fun migrate(db: SupportSQLiteDatabase) {
                // ARC-69: Revert FTS5 to FTS4 to match Room's @Fts4 annotation and avoid
                // 'docid' column missing errors during synchronization triggers.
                db.execSQL("DROP TRIGGER IF EXISTS transactions_fts_content_sync_ins")
                db.execSQL("DROP TRIGGER IF EXISTS transactions_fts_content_sync_upd")
                db.execSQL("DROP TRIGGER IF EXISTS transactions_fts_content_sync_del")
                db.execSQL("DROP TABLE IF EXISTS transactions_fts")

                db.execSQL(
                    "CREATE VIRTUAL TABLE IF NOT EXISTS `transactions_fts` " +
                            "USING fts4(note, payee, name, content=`transactions`)"
                )
                db.execSQL(
                    """
                    CREATE TRIGGER IF NOT EXISTS transactions_fts_content_sync_ins AFTER INSERT ON `transactions` BEGIN
                      INSERT INTO transactions_fts(docid, note, payee, name) VALUES (new.id, new.note, new.payee, new.name);
                    END
                    """.trimIndent()
                )
                db.execSQL(
                    """
                    CREATE TRIGGER IF NOT EXISTS transactions_fts_content_sync_upd AFTER UPDATE ON `transactions` BEGIN
                      DELETE FROM transactions_fts WHERE docid = old.id;
                      INSERT INTO transactions_fts(docid, note, payee, name) VALUES (new.id, new.note, new.payee, new.name);
                    END
                    """.trimIndent()
                )
                db.execSQL(
                    """
                    CREATE TRIGGER IF NOT EXISTS transactions_fts_content_sync_del AFTER DELETE ON `transactions` BEGIN
                      DELETE FROM transactions_fts WHERE docid = old.id;
                    END
                    """.trimIndent()
                )
                db.execSQL("INSERT INTO transactions_fts(docid, note, payee, name) SELECT id, note, payee, name FROM `transactions`")
            }
        }

        val MIGRATION_19_20 = object : Migration(19, 20) {
            override fun migrate(db: SupportSQLiteDatabase) {
                // ARC-08/09/10: composite PK (currencyCode, baseCurrency) so different
                // base-currency rate sets can coexist. Cache is regeneratable from network,
                // so a destructive recreate is acceptable here.
                db.execSQL("DROP TABLE IF EXISTS currency_rates")
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `currency_rates` (
                        `currencyCode` TEXT NOT NULL,
                        `baseCurrency` TEXT NOT NULL,
                        `rate` TEXT NOT NULL,
                        `timestamp` INTEGER NOT NULL,
                        `nextUpdate` INTEGER NOT NULL,
                        PRIMARY KEY(`currencyCode`, `baseCurrency`)
                    )
                    """.trimIndent()
                )
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_currency_rates_baseCurrency` ON `currency_rates`(`baseCurrency`)")
            }
        }

        val MIGRATION_18_19 = object : Migration(18, 19) {
            override fun migrate(db: SupportSQLiteDatabase) {
                // Re-creating FTS table to ensure consistency with current TransactionEntity columns.
                // Note: We use FTS4 to match Room's @Fts4 annotation and avoid 'docid' missing errors.
                db.execSQL("DROP TRIGGER IF EXISTS transactions_fts_content_sync_ins")
                db.execSQL("DROP TRIGGER IF EXISTS transactions_fts_content_sync_upd")
                db.execSQL("DROP TRIGGER IF EXISTS transactions_fts_content_sync_del")
                db.execSQL("DROP TABLE IF EXISTS transactions_fts")

                db.execSQL(
                    "CREATE VIRTUAL TABLE IF NOT EXISTS `transactions_fts` " +
                            "USING fts4(note, payee, name, content=`transactions`)"
                )
                db.execSQL(
                    """
                    CREATE TRIGGER IF NOT EXISTS transactions_fts_content_sync_ins AFTER INSERT ON `transactions` BEGIN
                      INSERT INTO transactions_fts(docid, note, payee, name) VALUES (new.id, new.note, new.payee, new.name);
                    END
                    """.trimIndent()
                )
                db.execSQL(
                    """
                    CREATE TRIGGER IF NOT EXISTS transactions_fts_content_sync_upd AFTER UPDATE ON `transactions` BEGIN
                      DELETE FROM transactions_fts WHERE docid = old.id;
                      INSERT INTO transactions_fts(docid, note, payee, name) VALUES (new.id, new.note, new.payee, new.name);
                    END
                    """.trimIndent()
                )
                db.execSQL(
                    """
                    CREATE TRIGGER IF NOT EXISTS transactions_fts_content_sync_del AFTER DELETE ON `transactions` BEGIN
                      DELETE FROM transactions_fts WHERE docid = old.id;
                    END
                    """.trimIndent()
                )
                // Rebuild the FTS index from existing transactions.
                db.execSQL("INSERT INTO transactions_fts(docid, note, payee, name) SELECT id, note, payee, name FROM `transactions`")
            }
        }

        val MIGRATION_17_18 = object : Migration(17, 18) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("DROP INDEX IF EXISTS index_planned_payment_occurrences_scheduledDate")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_planned_payment_occurrences_plannedPaymentId_scheduledDate ON planned_payment_occurrences(plannedPaymentId, scheduledDate)")
            }
        }

        val MIGRATION_16_17 = object : Migration(16, 17) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("CREATE INDEX IF NOT EXISTS index_transactions_profileId_timestamp ON transactions(profileId, timestamp)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_transactions_profileId_accountId ON transactions(profileId, accountId)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_templates_profileId_isStarred ON templates(profileId, isStarred)")
            }
        }

        val MIGRATION_15_16 = object : Migration(15, 16) {
            override fun migrate(db: SupportSQLiteDatabase) {
                // SQLite cannot ALTER COLUMN type, so recreate the table with rate as TEXT
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS currency_rates_new (
                        currencyCode TEXT NOT NULL PRIMARY KEY,
                        baseCurrency TEXT NOT NULL,
                        rate TEXT NOT NULL,
                        timestamp INTEGER NOT NULL,
                        nextUpdate INTEGER NOT NULL
                    )
                """
                )
                db.execSQL(
                    """
                    INSERT INTO currency_rates_new
                    SELECT currencyCode, baseCurrency, CAST(rate AS TEXT), timestamp, nextUpdate
                    FROM currency_rates
                """
                )
                db.execSQL("DROP TABLE currency_rates")
                db.execSQL("ALTER TABLE currency_rates_new RENAME TO currency_rates")
            }
        }

        val MIGRATION_14_15 = object : Migration(14, 15) {
            override fun migrate(db: SupportSQLiteDatabase) {
                // 1. Resolve duplicates before creating unique index
                // If a user has both 1003 and 1004 for the same account/currency, delete the 1004 one
                db.execSQL(
                    """
                    DELETE FROM transactions 
                    WHERE categoryId = 1004 
                    AND EXISTS (
                        SELECT 1 FROM transactions t2 
                        WHERE t2.accountId = transactions.accountId 
                        AND t2.currency = transactions.currency 
                        AND t2.categoryId = 1003
                    )
                """
                )

                // 2. Collapse categories in transactions table
                db.execSQL("UPDATE transactions SET categoryId = 1003 WHERE categoryId = 1004")
                db.execSQL("UPDATE transactions SET categoryId = 1005 WHERE categoryId = 1006")

                // 2. Collapse categories in templates table
                db.execSQL("UPDATE templates SET categoryId = 1003 WHERE categoryId = 1004")
                db.execSQL("UPDATE templates SET categoryId = 1005 WHERE categoryId = 1006")

                // 3. Collapse categories in planned_payments table
                // (Note: categoryId is in template, but if it exists directly anywhere else)
                // PlannedPaymentEntity doesn't have categoryId directly, it's via Template.

                // 4. Cleanup categories table
                db.execSQL("DELETE FROM categories WHERE id IN (1004, 1006)")
                db.execSQL("UPDATE categories SET name = 'Initial Balance', iconName = 'AccountBalance' WHERE id = 1003")
                db.execSQL("UPDATE categories SET name = 'Transfer', iconName = 'SwapHoriz' WHERE id = 1005")

                // 5. Consolidate Unique Indices for Initial Balance
                db.execSQL("DROP INDEX IF EXISTS idx_transactions_initial_balance_inflow_unique")
                db.execSQL("DROP INDEX IF EXISTS idx_transactions_initial_balance_outflow_unique")
                db.execSQL(
                    """CREATE UNIQUE INDEX IF NOT EXISTS idx_transactions_initial_balance_unique
                              ON transactions(accountId, currency, categoryId) WHERE categoryId = 1003"""
                )
            }
        }

        val MIGRATION_7_8 = object : Migration(7, 8) {
            override fun migrate(db: SupportSQLiteDatabase) {
                // ── transactions: replace latitude+longitude with gpsLocation ──────────
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `transactions_new` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `name` TEXT NOT NULL,
                        `amount` TEXT NOT NULL,
                        `timestamp` INTEGER NOT NULL,
                        `currency` TEXT NOT NULL,
                        `transactionType` TEXT NOT NULL,
                        `accountId` TEXT,
                        `categoryId` INTEGER NOT NULL,
                        `tags` TEXT NOT NULL,
                        `paymentMethod` TEXT NOT NULL,
                        `payee` TEXT NOT NULL,
                        `note` TEXT NOT NULL,
                        `sourcePlannedPaymentId` INTEGER,
                        `profileId` INTEGER NOT NULL,
                        `gpsLocation` TEXT,
                        `warrantyExpiration` INTEGER,
                        `status` TEXT NOT NULL,
                        FOREIGN KEY(`accountId`) REFERENCES `accounts`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE,
                        FOREIGN KEY(`categoryId`) REFERENCES `categories`(`id`) ON UPDATE NO ACTION ON DELETE RESTRICT
                    )
                """.trimIndent()
                )
                db.execSQL(
                    """
                    INSERT INTO `transactions_new`
                    SELECT `id`, `name`, `amount`, `timestamp`, `currency`, `transactionType`,
                           `accountId`, `categoryId`, `tags`, `paymentMethod`, `payee`, `note`,
                           `sourcePlannedPaymentId`, `profileId`,
                           CASE WHEN `latitude` IS NOT NULL AND `longitude` IS NOT NULL
                                THEN (`latitude` || ',' || `longitude`)
                                ELSE NULL END,
                           `warrantyExpiration`, `status`
                    FROM `transactions`
                """.trimIndent()
                )
                db.execSQL("DROP TABLE `transactions`")
                db.execSQL("ALTER TABLE `transactions_new` RENAME TO `transactions`")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_transactions_accountId` ON `transactions` (`accountId`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_transactions_categoryId` ON `transactions` (`categoryId`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_transactions_profileId` ON `transactions` (`profileId`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_transactions_timestamp` ON `transactions` (`timestamp`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_transactions_status` ON `transactions` (`status`)")
                // Recreate FTS triggers pointing at the new table
                db.execSQL("DROP TRIGGER IF EXISTS transactions_fts_content_sync_ins")
                db.execSQL("DROP TRIGGER IF EXISTS transactions_fts_content_sync_upd")
                db.execSQL("DROP TRIGGER IF EXISTS transactions_fts_content_sync_del")
                db.execSQL(
                    """
                    CREATE TRIGGER IF NOT EXISTS transactions_fts_content_sync_ins AFTER INSERT ON `transactions` BEGIN
                      INSERT INTO transactions_fts(rowid, note, payee, name) VALUES (new.id, new.note, new.payee, new.name);
                    END
                """.trimIndent()
                )
                db.execSQL(
                    """
                    CREATE TRIGGER IF NOT EXISTS transactions_fts_content_sync_upd AFTER UPDATE ON `transactions` BEGIN
                      DELETE FROM transactions_fts WHERE rowid = old.id;
                      INSERT INTO transactions_fts(rowid, note, payee, name) VALUES (new.id, new.note, new.payee, new.name);
                    END
                """.trimIndent()
                )
                db.execSQL(
                    """
                    CREATE TRIGGER IF NOT EXISTS transactions_fts_content_sync_del AFTER DELETE ON `transactions` BEGIN
                      DELETE FROM transactions_fts WHERE rowid = old.id;
                    END
                """.trimIndent()
                )

                // ── templates: replace latitude+longitude with gpsLocation ────────────
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `templates_new` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `name` TEXT NOT NULL,
                        `categoryId` INTEGER NOT NULL,
                        `amount` TEXT NOT NULL,
                        `transactionType` TEXT NOT NULL,
                        `icon` TEXT,
                        `colorHex` TEXT,
                        `accountId` TEXT,
                        `targetAccountId` TEXT,
                        `targetCurrency` TEXT,
                        `transferRate` TEXT NOT NULL,
                        `note` TEXT,
                        `payee` TEXT,
                        `location` TEXT,
                        `gpsLocation` TEXT,
                        `warrantyExpiration` INTEGER,
                        `transactionStatus` TEXT NOT NULL,
                        `paymentMethod` TEXT,
                        `currency` TEXT NOT NULL,
                        `tags` TEXT NOT NULL,
                        `isStarred` INTEGER NOT NULL,
                        `displayOrder` INTEGER NOT NULL,
                        `profileId` INTEGER NOT NULL,
                        FOREIGN KEY(`categoryId`) REFERENCES `categories`(`id`) ON UPDATE NO ACTION ON DELETE RESTRICT
                    )
                """.trimIndent()
                )
                db.execSQL(
                    """
                    INSERT INTO `templates_new`
                    SELECT `id`, `name`, `categoryId`, `amount`, `transactionType`, `icon`, `colorHex`,
                           `accountId`, `targetAccountId`, `targetCurrency`, `transferRate`, `note`, `payee`,
                           `location`,
                           CASE WHEN `latitude` IS NOT NULL AND `longitude` IS NOT NULL
                                THEN (`latitude` || ',' || `longitude`)
                                ELSE NULL END,
                           `warrantyExpiration`, `transactionStatus`, `paymentMethod`, `currency`,
                           `tags`, `isStarred`, `displayOrder`, `profileId`
                    FROM `templates`
                """.trimIndent()
                )
                db.execSQL("DROP TABLE `templates`")
                db.execSQL("ALTER TABLE `templates_new` RENAME TO `templates`")
            }
        }

        val MIGRATION_6_7 = object : Migration(6, 7) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `transaction_tag_cross_ref` (
                        `transactionId` INTEGER NOT NULL, 
                        `tagId` INTEGER NOT NULL, 
                        PRIMARY KEY(`transactionId`, `tagId`), 
                        FOREIGN KEY(`transactionId`) REFERENCES `transactions`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE, 
                        FOREIGN KEY(`tagId`) REFERENCES `tags`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE
                    )
                """.trimIndent()
                )
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_transaction_tag_cross_ref_transactionId` ON `transaction_tag_cross_ref` (`transactionId`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_transaction_tag_cross_ref_tagId` ON `transaction_tag_cross_ref` (`tagId`)")
            }
        }

        val MIGRATION_4_5 = object : Migration(4, 5) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("CREATE VIRTUAL TABLE IF NOT EXISTS `transactions_fts` USING fts4(content=`transactions`, `note`, `payee`, `name`)")
                db.execSQL(
                    """
                    CREATE TRIGGER IF NOT EXISTS transactions_fts_content_sync_ins AFTER INSERT ON `transactions` BEGIN
                      INSERT INTO transactions_fts(rowid, note, payee, name) VALUES (new.id, new.note, new.payee, new.name);
                    END
                """.trimIndent()
                )
                db.execSQL(
                    """
                    CREATE TRIGGER IF NOT EXISTS transactions_fts_content_sync_upd AFTER UPDATE ON `transactions` BEGIN
                      DELETE FROM transactions_fts WHERE rowid = old.id;
                      INSERT INTO transactions_fts(rowid, note, payee, name) VALUES (new.id, new.note, new.payee, new.name);
                    END
                """.trimIndent()
                )
                db.execSQL(
                    """
                    CREATE TRIGGER IF NOT EXISTS transactions_fts_content_sync_del AFTER DELETE ON `transactions` BEGIN
                      DELETE FROM transactions_fts WHERE rowid = old.id;
                    END
                """.trimIndent()
                )
                db.execSQL("INSERT INTO transactions_fts(rowid, note, payee, name) SELECT id, note, payee, name FROM `transactions`")
            }
        }

        val MIGRATION_8_9 = object : Migration(8, 9) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE `transactions` ADD COLUMN `location` TEXT")
            }
        }

        val MIGRATION_5_6 = object : Migration(5, 6) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_categories_name` ON `categories` (`name`)")
            }
        }

        val MIGRATION_10_11 = object : Migration(10, 11) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """CREATE UNIQUE INDEX IF NOT EXISTS idx_transactions_initial_balance_unique
                       ON transactions(accountId, currency, categoryId)
                       WHERE categoryId = 1003"""
                )
            }
        }

        val MIGRATION_11_12 = object : Migration(11, 12) {
            override fun migrate(db: SupportSQLiteDatabase) {
                // Shift existing transfer rows BEFORE inserting new category rows
                db.execSQL("UPDATE transactions SET categoryId = 1006 WHERE categoryId = 1005")
                db.execSQL("UPDATE transactions SET categoryId = 1005 WHERE categoryId = 1004")
                // Update the categories table
                db.execSQL("UPDATE categories SET id = 1006, name = 'Transfer Outflow', iconName = 'CallMade'  WHERE id = 1005")
                db.execSQL("UPDATE categories SET id = 1005, name = 'Transfer Inflow',  iconName = 'CallReceived' WHERE id = 1004")
                db.execSQL("UPDATE categories SET name = 'Initial Balance', iconName = 'CallReceived' WHERE id = 1003")
                db.execSQL("INSERT OR IGNORE INTO categories (id, name, iconName, colorHex, isDefault, parentId, nature) VALUES (1004, 'Initial Balance', 'CallMade', -7829368, 1, 1000, 'need')")
                // Drop old partial index and re-create for both 1003 and 1004
                db.execSQL("DROP INDEX IF EXISTS idx_transactions_initial_balance_unique")
                db.execSQL(
                    """CREATE UNIQUE INDEX IF NOT EXISTS idx_transactions_initial_balance_inflow_unique
                              ON transactions(accountId, currency, categoryId) WHERE categoryId = 1003"""
                )
                db.execSQL(
                    """CREATE UNIQUE INDEX IF NOT EXISTS idx_transactions_initial_balance_outflow_unique
                              ON transactions(accountId, currency, categoryId) WHERE categoryId = 1004"""
                )
            }
        }

        val MIGRATION_12_13 = object : Migration(12, 13) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE `transaction_filters` ADD COLUMN `matchAllTags` INTEGER NOT NULL DEFAULT 1")
            }
        }

        val MIGRATION_13_14 = object : Migration(13, 14) {
            override fun migrate(db: SupportSQLiteDatabase) {
                // ── transactions.transactionType: split legacy 'transfer' by amount sign ──
                db.execSQL("UPDATE `transactions` SET `transactionType` = 'transfer_inflow'  WHERE `transactionType` = 'transfer' AND CAST(`amount` AS REAL) > 0")
                db.execSQL("UPDATE `transactions` SET `transactionType` = 'transfer_outflow' WHERE `transactionType` = 'transfer' AND CAST(`amount` AS REAL) < 0")
                // Zero-amount transfer rows: bucket as outflow so they remain visible.
                db.execSQL("UPDATE `transactions` SET `transactionType` = 'transfer_outflow' WHERE `transactionType` = 'transfer'")

                // ── templates: rename transactionType -> entryMode via table-recreate ──
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `templates_new` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `name` TEXT NOT NULL,
                        `categoryId` INTEGER NOT NULL,
                        `amount` TEXT NOT NULL,
                        `entryMode` TEXT NOT NULL,
                        `icon` TEXT,
                        `colorHex` TEXT,
                        `accountId` TEXT,
                        `targetAccountId` TEXT,
                        `targetCurrency` TEXT,
                        `transferRate` TEXT NOT NULL,
                        `note` TEXT,
                        `payee` TEXT,
                        `location` TEXT,
                        `gpsLocation` TEXT,
                        `warrantyExpiration` INTEGER,
                        `transactionStatus` TEXT NOT NULL,
                        `paymentMethod` TEXT,
                        `currency` TEXT NOT NULL,
                        `tags` TEXT NOT NULL,
                        `isStarred` INTEGER NOT NULL,
                        `displayOrder` INTEGER NOT NULL,
                        `profileId` INTEGER NOT NULL,
                        FOREIGN KEY(`categoryId`) REFERENCES `categories`(`id`) ON UPDATE NO ACTION ON DELETE RESTRICT
                    )
                """.trimIndent()
                )
                // Existing values 'expense', 'income', 'transfer' all match EntryMode.value verbatim.
                db.execSQL(
                    """
                    INSERT INTO `templates_new`
                    SELECT `id`, `name`, `categoryId`, `amount`, `transactionType`, `icon`, `colorHex`,
                           `accountId`, `targetAccountId`, `targetCurrency`, `transferRate`, `note`, `payee`,
                           `location`, `gpsLocation`, `warrantyExpiration`, `transactionStatus`, `paymentMethod`,
                           `currency`, `tags`, `isStarred`, `displayOrder`, `profileId`
                    FROM `templates`
                """.trimIndent()
                )
                db.execSQL("DROP TABLE `templates`")
                db.execSQL("ALTER TABLE `templates_new` RENAME TO `templates`")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_templates_categoryId` ON `templates` (`categoryId`)")
            }
        }

        val MIGRATION_9_10 = object : Migration(9, 10) {
            override fun migrate(db: SupportSQLiteDatabase) {
                // 1. Clean up potential case-duplicates before enforcing unique index
                // We keep the one with the lowest ID
                db.execSQL(
                    """
                    DELETE FROM tags 
                    WHERE id NOT IN (
                        SELECT MIN(id) 
                        FROM tags 
                        GROUP BY LOWER(name)
                    )
                    """.trimIndent()
                )
                // 2. Create the unique case-insensitive index
                db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS `index_tags_name` ON `tags` (`name`)")
            }
        }

        private fun upgradeToFts4(db: SupportSQLiteDatabase) {
            // Drop ALL possible FTS triggers (Room-generated, manual, old, new)
            db.execSQL("DROP TRIGGER IF EXISTS room_fts_content_sync_transactions_fts_BEFORE_UPDATE")
            db.execSQL("DROP TRIGGER IF EXISTS room_fts_content_sync_transactions_fts_BEFORE_DELETE")
            db.execSQL("DROP TRIGGER IF EXISTS room_fts_content_sync_transactions_fts_AFTER_UPDATE")
            db.execSQL("DROP TRIGGER IF EXISTS room_fts_content_sync_transactions_fts_AFTER_INSERT")
            db.execSQL("DROP TRIGGER IF EXISTS transactions_fts_content_sync_ins")
            db.execSQL("DROP TRIGGER IF EXISTS transactions_fts_content_sync_upd")
            db.execSQL("DROP TRIGGER IF EXISTS transactions_fts_content_sync_del")
            db.execSQL("DROP TRIGGER IF EXISTS manual_fts_content_sync_ins")
            db.execSQL("DROP TRIGGER IF EXISTS manual_fts_content_sync_upd")
            db.execSQL("DROP TRIGGER IF EXISTS manual_fts_content_sync_del")

            // Recreate table as FTS4 (Room natively supports FTS4)
            db.execSQL("DROP TABLE IF EXISTS transactions_fts")
            db.execSQL(
                "CREATE VIRTUAL TABLE IF NOT EXISTS `transactions_fts` " +
                        "USING fts4(note, payee, name, content=`transactions`)"
            )

            // Create FTS4 triggers using docid (Room's standard for FTS4)
            db.execSQL(
                """
                CREATE TRIGGER room_fts_content_sync_transactions_fts_AFTER_INSERT AFTER INSERT ON `transactions` BEGIN
                  INSERT INTO transactions_fts(docid, note, payee, name) VALUES (new.id, new.note, new.payee, new.name);
                END
                """.trimIndent()
            )
            db.execSQL(
                """
                CREATE TRIGGER room_fts_content_sync_transactions_fts_AFTER_UPDATE AFTER UPDATE ON `transactions` BEGIN
                  DELETE FROM transactions_fts WHERE docid = old.id;
                  INSERT INTO transactions_fts(docid, note, payee, name) VALUES (new.id, new.note, new.payee, new.name);
                END
                """.trimIndent()
            )
            db.execSQL(
                """
                CREATE TRIGGER room_fts_content_sync_transactions_fts_AFTER_DELETE AFTER DELETE ON `transactions` BEGIN
                  DELETE FROM transactions_fts WHERE docid = old.id;
                END
                """.trimIndent()
            )

            // Initial index build
            db.execSQL("INSERT INTO transactions_fts(docid, note, payee, name) SELECT id, note, payee, name FROM `transactions`")
        }

        private fun upgradeToFts5(db: SupportSQLiteDatabase) {
            // Drop ALL possible FTS triggers (Room-generated, manual, old, new)
            db.execSQL("DROP TRIGGER IF EXISTS room_fts_content_sync_transactions_fts_BEFORE_UPDATE")
            db.execSQL("DROP TRIGGER IF EXISTS room_fts_content_sync_transactions_fts_BEFORE_DELETE")
            db.execSQL("DROP TRIGGER IF EXISTS room_fts_content_sync_transactions_fts_AFTER_UPDATE")
            db.execSQL("DROP TRIGGER IF EXISTS room_fts_content_sync_transactions_fts_AFTER_INSERT")
            db.execSQL("DROP TRIGGER IF EXISTS transactions_fts_content_sync_ins")
            db.execSQL("DROP TRIGGER IF EXISTS transactions_fts_content_sync_upd")
            db.execSQL("DROP TRIGGER IF EXISTS transactions_fts_content_sync_del")
            db.execSQL("DROP TRIGGER IF EXISTS manual_fts_content_sync_ins")
            db.execSQL("DROP TRIGGER IF EXISTS manual_fts_content_sync_upd")
            db.execSQL("DROP TRIGGER IF EXISTS manual_fts_content_sync_del")

            // Recreate table as FTS5 (dropping whatever was there - could be FTS4 or a dummy Room table)
            db.execSQL("DROP TABLE IF EXISTS transactions_fts")
            db.execSQL(
                "CREATE VIRTUAL TABLE IF NOT EXISTS `transactions_fts` " +
                        "USING fts5(note, payee, name, content=`transactions`, content_rowid=`id`)"
            )

            // Create FTS5 triggers using rowid. We use 'manual_' prefix to avoid Room's magic.
            db.execSQL(
                """
                CREATE TRIGGER manual_fts_content_sync_ins AFTER INSERT ON `transactions` BEGIN
                  INSERT INTO transactions_fts(rowid, note, payee, name) VALUES (new.id, new.note, new.payee, new.name);
                END
                """.trimIndent()
            )
            db.execSQL(
                """
                CREATE TRIGGER manual_fts_content_sync_upd AFTER UPDATE ON `transactions` BEGIN
                  INSERT INTO transactions_fts(transactions_fts, rowid, note, payee, name) VALUES ('delete', old.id, old.note, old.payee, old.name);
                  INSERT INTO transactions_fts(rowid, note, payee, name) VALUES (new.id, new.note, new.payee, new.name);
                END
                """.trimIndent()
            )
            db.execSQL(
                """
                CREATE TRIGGER manual_fts_content_sync_del AFTER DELETE ON `transactions` BEGIN
                  INSERT INTO transactions_fts(transactions_fts, rowid, note, payee, name) VALUES ('delete', old.id, old.note, old.payee, old.name);
                END
                """.trimIndent()
            )

            // Initial index build
            db.execSQL("INSERT INTO transactions_fts(rowid, note, payee, name) SELECT id, note, payee, name FROM `transactions`")
        }

        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context, passphrase: ByteArray): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val factory = SupportOpenHelperFactory(passphrase)

                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "osswallet_manager_db"
                ).openHelperFactory(factory)
                    .addMigrations(
                        MIGRATION_4_5,
                        MIGRATION_5_6,
                        MIGRATION_6_7,
                        MIGRATION_7_8,
                        MIGRATION_8_9,
                        MIGRATION_9_10,
                        MIGRATION_10_11,
                        MIGRATION_11_12,
                        MIGRATION_12_13,
                        MIGRATION_13_14,
                        MIGRATION_14_15,
                        MIGRATION_15_16,
                        MIGRATION_16_17,
                        MIGRATION_17_18,
                        MIGRATION_18_19,
                        MIGRATION_19_20,
                        MIGRATION_20_21,
                        MIGRATION_21_22,
                        MIGRATION_22_23,
                        MIGRATION_23_24,
                        MIGRATION_24_25,
                        MIGRATION_25_26,
                        MIGRATION_26_27,
                        MIGRATION_27_28
                    )
                    .addCallback(object : Callback() {
                        override fun onCreate(db: SupportSQLiteDatabase) {
                            super.onCreate(db)
                            upgradeToFts4(db)
                            ensureSystemCategories(db)
                            ensureSystemTags(db)
                            context.applicationContext.assets.open("default_data.sql")
                                .bufferedReader()
                                .useLines { lines ->
                                    lines.map { it.trim() }
                                        .filter { it.isNotBlank() && !it.startsWith("--") }
                                        .forEach { db.execSQL(it) }
                                }
                        }

                        override fun onOpen(db: SupportSQLiteDatabase) {
                            super.onOpen(db)
                            ensureSystemCategories(db)
                            ensureSystemTags(db)
                        }

                        private fun ensureSystemCategories(db: SupportSQLiteDatabase) {
                            SystemCategory.entries.forEach { sc ->
                                // Using REPLACE to override user changes to system categories
                                db.execSQL(
                                    "INSERT OR IGNORE INTO categories (id, name, iconName, colorHex, isDefault, parentId, nature) VALUES (?, ?, ?, ?, 1, ?, ?)",
                                    arrayOf(
                                        sc.id,
                                        sc.displayName,
                                        sc.iconName,
                                        sc.colorHex,
                                        sc.parentId,
                                        sc.nature.value
                                    )
                                )
                            }
                        }

                        private fun ensureSystemTags(db: SupportSQLiteDatabase) {
                            com.osscabinet.ossWallet.domain.model.SystemTag.entries.forEach { st ->
                                // For tags, name is unique.
                                db.execSQL(
                                    "INSERT OR IGNORE INTO tags (id, name, colorHex) VALUES (?, ?, ?)",
                                    arrayOf(st.id, st.tagName, st.colorHex)
                                )
                            }
                        }
                    })
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
