package com.osscabinet.ossWallet.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.osscabinet.ossWallet.data.local.dao.*
import com.osscabinet.ossWallet.data.local.entity.*
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
    version = 9,
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
                db.execSQL("""
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
                """.trimIndent())
                db.execSQL("""
                    INSERT INTO `transactions_new`
                    SELECT `id`, `name`, `amount`, `timestamp`, `currency`, `transactionType`,
                           `accountId`, `categoryId`, `tags`, `paymentMethod`, `payee`, `note`,
                           `sourcePlannedPaymentId`, `profileId`,
                           CASE WHEN `latitude` IS NOT NULL AND `longitude` IS NOT NULL
                                THEN (`latitude` || ',' || `longitude`)
                                ELSE NULL END,
                           `warrantyExpiration`, `status`
                    FROM `transactions`
                """.trimIndent())
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
                db.execSQL("""
                    CREATE TRIGGER IF NOT EXISTS transactions_fts_content_sync_ins AFTER INSERT ON `transactions` BEGIN
                      INSERT INTO transactions_fts(rowid, note, payee, name) VALUES (new.id, new.note, new.payee, new.name);
                    END
                """.trimIndent())
                db.execSQL("""
                    CREATE TRIGGER IF NOT EXISTS transactions_fts_content_sync_upd AFTER UPDATE ON `transactions` BEGIN
                      DELETE FROM transactions_fts WHERE rowid = old.id;
                      INSERT INTO transactions_fts(rowid, note, payee, name) VALUES (new.id, new.note, new.payee, new.name);
                    END
                """.trimIndent())
                db.execSQL("""
                    CREATE TRIGGER IF NOT EXISTS transactions_fts_content_sync_del AFTER DELETE ON `transactions` BEGIN
                      DELETE FROM transactions_fts WHERE rowid = old.id;
                    END
                """.trimIndent())

                // ── templates: replace latitude+longitude with gpsLocation ────────────
                db.execSQL("""
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
                """.trimIndent())
                db.execSQL("""
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
                """.trimIndent())
                db.execSQL("DROP TABLE `templates`")
                db.execSQL("ALTER TABLE `templates_new` RENAME TO `templates`")
            }
        }

        val MIGRATION_6_7 = object : Migration(6, 7) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS `transaction_tag_cross_ref` (
                        `transactionId` INTEGER NOT NULL, 
                        `tagId` INTEGER NOT NULL, 
                        PRIMARY KEY(`transactionId`, `tagId`), 
                        FOREIGN KEY(`transactionId`) REFERENCES `transactions`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE, 
                        FOREIGN KEY(`tagId`) REFERENCES `tags`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE
                    )
                """.trimIndent())
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_transaction_tag_cross_ref_transactionId` ON `transaction_tag_cross_ref` (`transactionId`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_transaction_tag_cross_ref_tagId` ON `transaction_tag_cross_ref` (`tagId`)")
            }
        }

        val MIGRATION_4_5 = object : Migration(4, 5) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("CREATE VIRTUAL TABLE IF NOT EXISTS `transactions_fts` USING fts4(content=`transactions`, `note`, `payee`, `name`)")
                db.execSQL("""
                    CREATE TRIGGER IF NOT EXISTS transactions_fts_content_sync_ins AFTER INSERT ON `transactions` BEGIN
                      INSERT INTO transactions_fts(rowid, note, payee, name) VALUES (new.id, new.note, new.payee, new.name);
                    END
                """.trimIndent())
                db.execSQL("""
                    CREATE TRIGGER IF NOT EXISTS transactions_fts_content_sync_upd AFTER UPDATE ON `transactions` BEGIN
                      DELETE FROM transactions_fts WHERE rowid = old.id;
                      INSERT INTO transactions_fts(rowid, note, payee, name) VALUES (new.id, new.note, new.payee, new.name);
                    END
                """.trimIndent())
                db.execSQL("""
                    CREATE TRIGGER IF NOT EXISTS transactions_fts_content_sync_del AFTER DELETE ON `transactions` BEGIN
                      DELETE FROM transactions_fts WHERE rowid = old.id;
                    END
                """.trimIndent())
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
                    .addMigrations(MIGRATION_4_5, MIGRATION_5_6, MIGRATION_6_7, MIGRATION_7_8, MIGRATION_8_9)
                    .addCallback(object : Callback() {
                        override fun onCreate(db: SupportSQLiteDatabase) {
                            super.onCreate(db)
                            context.applicationContext.assets.open("default_data.sql")
                                .bufferedReader()
                                .useLines { lines ->
                                    lines.map { it.trim() }
                                        .filter { it.isNotBlank() && !it.startsWith("--") }
                                        .forEach { db.execSQL(it) }
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
