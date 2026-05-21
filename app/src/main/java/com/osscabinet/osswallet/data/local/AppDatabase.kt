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
    version = 7,
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
                    .addMigrations(MIGRATION_4_5, MIGRATION_5_6, MIGRATION_6_7)
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
