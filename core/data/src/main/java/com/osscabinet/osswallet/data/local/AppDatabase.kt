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
import com.osscabinet.ossWallet.data.local.dao.CardCacheDao
import com.osscabinet.ossWallet.data.local.dao.CategoryDao
import com.osscabinet.ossWallet.data.local.dao.CurrencyDao
import com.osscabinet.ossWallet.data.local.dao.FilterDao
import com.osscabinet.ossWallet.data.local.dao.GoalDao
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
import com.osscabinet.ossWallet.data.local.entity.CardCacheEntity
import com.osscabinet.ossWallet.data.local.entity.CategoryEntity
import com.osscabinet.ossWallet.data.local.entity.CurrencyRateEntity
import com.osscabinet.ossWallet.data.local.entity.FilterEntity
import com.osscabinet.ossWallet.data.local.entity.GoalEntity
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
        LoanPaymentEntity::class,
        CardCacheEntity::class,
        GoalEntity::class
    ],
    version = 2,
    exportSchema = true
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
    abstract fun cardCacheDao(): CardCacheDao
    abstract fun goalDao(): GoalDao

    companion object {
        init {
            System.loadLibrary("sqlcipher")
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

        /** v1 -> v2: savings goals. */
        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS `goals` (" +
                        "`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                        "`name` TEXT NOT NULL, " +
                        "`targetAmount` TEXT NOT NULL, " +
                        "`savedAmount` TEXT NOT NULL, " +
                        "`currency` TEXT NOT NULL, " +
                        "`deadline` INTEGER, " +
                        "`note` TEXT NOT NULL, " +
                        "`profileId` INTEGER NOT NULL, " +
                        "`createdAt` INTEGER NOT NULL, " +
                        "`completedAt` INTEGER)"
                )
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_goals_profileId` ON `goals` (`profileId`)")
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
                    .setJournalMode(RoomDatabase.JournalMode.TRUNCATE)
                    .addMigrations(MIGRATION_1_2)
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
                                    arrayOf<Any?>(
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
                                    arrayOf<Any?>(st.id, st.tagName, st.colorHex)
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
