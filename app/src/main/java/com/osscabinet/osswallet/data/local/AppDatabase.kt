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
import com.osscabinet.ossWallet.domain.model.SystemCategory
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
    version = 12,
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
                db.execSQL("""CREATE UNIQUE INDEX IF NOT EXISTS idx_transactions_initial_balance_inflow_unique
                              ON transactions(accountId, currency, categoryId) WHERE categoryId = 1003""")
                db.execSQL("""CREATE UNIQUE INDEX IF NOT EXISTS idx_transactions_initial_balance_outflow_unique
                              ON transactions(accountId, currency, categoryId) WHERE categoryId = 1004""")
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
                        MIGRATION_11_12
                    )
                    .addCallback(object : Callback() {
                        override fun onCreate(db: SupportSQLiteDatabase) {
                            super.onCreate(db)
                            ensureSystemCategories(db)
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
                        }

                        private fun ensureSystemCategories(db: SupportSQLiteDatabase) {
                            SystemCategory.entries.forEach { sc ->
                                db.execSQL(
                                    "INSERT OR IGNORE INTO categories (id, name, iconName, colorHex, isDefault, parentId, nature) VALUES (?, ?, ?, ?, 1, ?, ?)",
                                    arrayOf(sc.id, sc.displayName, sc.iconName, sc.colorHex, sc.parentId, sc.nature.value)
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
