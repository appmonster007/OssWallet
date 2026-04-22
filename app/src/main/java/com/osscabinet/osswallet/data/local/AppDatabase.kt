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
        CurrencyRateEntity::class,
        BudgetEntity::class,
        LoanEntity::class,
        AccountEntity::class,
        CategoryEntity::class,
        TagEntity::class,
        ProfileEntity::class,
        TemplateEntity::class,
        FilterEntity::class
    ],
    version = 7,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun transactionDao(): TransactionDao
    abstract fun currencyDao(): CurrencyDao
    abstract fun budgetDao(): BudgetDao
    abstract fun loanDao(): LoanDao
    abstract fun accountDao(): AccountDao
    abstract fun categoryDao(): CategoryDao
    abstract fun tagDao(): TagDao
    abstract fun profileDao(): ProfileDao
    abstract fun templateDao(): TemplateDao
    abstract fun filterDao(): FilterDao

    companion object {
        init {
            System.loadLibrary("sqlcipher")
        }

        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context, passphrase: ByteArray): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val factory = SupportOpenHelperFactory(passphrase)
                
                // Migration from version 4 to 5: Drop initialBalance column from accounts table
                val MIGRATION_4_5 = object : Migration(4, 5) {
                    override fun migrate(database: SupportSQLiteDatabase) {
                        // Create new accounts table without initialBalance column
                        database.execSQL("""
                            CREATE TABLE accounts_new (
                                id TEXT NOT NULL PRIMARY KEY,
                                name TEXT NOT NULL,
                                balance TEXT NOT NULL,
                                currencyCode TEXT NOT NULL,
                                structureType TEXT NOT NULL,
                                colorHex TEXT NOT NULL,
                                icon TEXT NOT NULL,
                                allowedCurrencies TEXT NOT NULL,
                                profileId INTEGER NOT NULL,
                                isArchived INTEGER NOT NULL DEFAULT 0,
                                displayOrder INTEGER NOT NULL DEFAULT 0
                            )
                        """)

                        // Copy data from old table to new table (excluding initialBalance)
                        database.execSQL("""
                            INSERT INTO accounts_new (id, name, balance, currencyCode, structureType, colorHex, icon, allowedCurrencies, profileId, isArchived, displayOrder)
                            SELECT id, name, balance, currencyCode, structureType, colorHex, icon, allowedCurrencies, profileId, isArchived, displayOrder
                            FROM accounts
                        """)

                        // Drop old table
                        database.execSQL("DROP TABLE accounts")

                        // Rename new table to accounts
                        database.execSQL("ALTER TABLE accounts_new RENAME TO accounts")

                        // Recreate index
                        database.execSQL("CREATE INDEX index_accounts_profileId ON accounts (profileId)")
                    }
                }

                // Migration from version 5 to 6: Replace isIncome with transactionType
                val MIGRATION_5_6 = object : Migration(5, 6) {
                    override fun migrate(database: SupportSQLiteDatabase) {
                        // Add new transactionType column
                        database.execSQL("ALTER TABLE transactions ADD COLUMN transactionType TEXT")

                        // Migrate data: isIncome = 1 -> "income", isIncome = 0 -> "expense"
                        database.execSQL("""
                            UPDATE transactions
                            SET transactionType = CASE WHEN isIncome = 1 THEN 'income' ELSE 'expense' END
                        """)

                        // Drop old isIncome column (SQLite doesn't support DROP COLUMN directly, need to recreate table)
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
                                linkedTransactionId INTEGER,
                                profileId INTEGER NOT NULL,
                                latitude REAL,
                                longitude REAL,
                                warrantyExpiration INTEGER,
                                FOREIGN KEY(accountId) REFERENCES accounts(id) ON DELETE CASCADE,
                                FOREIGN KEY(categoryId) REFERENCES categories(id) ON DELETE SET NULL
                            )
                        """)

                        // Copy data from old table to new table (excluding isIncome)
                        database.execSQL("""
                            INSERT INTO transactions_new (id, title, amount, category, timestamp, currency, transactionType, accountId, categoryId, tags, paymentMethod, payee, note, recurring, linkedTransactionId, profileId, latitude, longitude, warrantyExpiration)
                            SELECT id, title, amount, category, timestamp, currency, transactionType, accountId, categoryId, tags, paymentMethod, payee, note, recurring, linkedTransactionId, profileId, latitude, longitude, warrantyExpiration
                            FROM transactions
                        """)

                        // Drop old table
                        database.execSQL("DROP TABLE transactions")

                        // Rename new table to transactions
                        database.execSQL("ALTER TABLE transactions_new RENAME TO transactions")

                        // Recreate indexes
                        database.execSQL("CREATE INDEX index_transactions_accountId ON transactions (accountId)")
                        database.execSQL("CREATE INDEX index_transactions_categoryId ON transactions (categoryId)")
                        database.execSQL("CREATE INDEX index_transactions_profileId ON transactions (profileId)")
                        database.execSQL("CREATE INDEX index_transactions_timestamp ON transactions (timestamp)")
                    }
                }

        val MIGRATION_6_7 = object : Migration(6, 7) {
            override fun migrate(database: SupportSQLiteDatabase) {
                // Add new transactionType column
                database.execSQL("ALTER TABLE transaction_filters ADD COLUMN transactionType TEXT")

                // Migrate data: isIncome = 1 -> "income", isIncome = 0 -> "expense", NULL -> NULL
                database.execSQL("""
                    UPDATE transaction_filters
                    SET transactionType = CASE 
                        WHEN isIncome = 1 THEN 'income'
                        WHEN isIncome = 0 THEN 'expense'
                        ELSE NULL
                    END
                """)

                // Drop old isIncome column (SQLite doesn't support DROP COLUMN directly, need to recreate table)
                database.execSQL("""
                    CREATE TABLE transaction_filters_new (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        name TEXT NOT NULL,
                        queryNote TEXT,
                        isNoteRegex INTEGER NOT NULL DEFAULT 0,
                        queryPayee TEXT,
                        isPayeeRegex INTEGER NOT NULL DEFAULT 0,
                        categoryIds TEXT NOT NULL,
                        accountIds TEXT NOT NULL,
                        startDate INTEGER,
                        endDate INTEGER,
                        paymentMethods TEXT NOT NULL,
                        hasWarranty INTEGER,
                        warrantyExpirationStart INTEGER,
                        warrantyExpirationEnd INTEGER,
                        minAmount TEXT,
                        maxAmount TEXT,
                        transactionType TEXT,
                        currencies TEXT NOT NULL
                    )
                """)

                // Copy data from old table to new table (excluding isIncome)
                database.execSQL("""
                    INSERT INTO transaction_filters_new (id, name, queryNote, isNoteRegex, queryPayee, isPayeeRegex, categoryIds, accountIds, startDate, endDate, paymentMethods, hasWarranty, warrantyExpirationStart, warrantyExpirationEnd, minAmount, maxAmount, transactionType, currencies)
                    SELECT id, name, queryNote, isNoteRegex, queryPayee, isPayeeRegex, categoryIds, accountIds, startDate, endDate, paymentMethods, hasWarranty, warrantyExpirationStart, warrantyExpirationEnd, minAmount, maxAmount, transactionType, currencies
                    FROM transaction_filters
                """)

                // Drop old table
                database.execSQL("DROP TABLE transaction_filters")

                // Rename new table to transaction_filters
                database.execSQL("ALTER TABLE transaction_filters_new RENAME TO transaction_filters")
            }
        }
                
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "osswallet_manager_db"
                ).openHelperFactory(factory)
                    .addCallback(object : Callback() {
                        override fun onCreate(db: androidx.sqlite.db.SupportSQLiteDatabase) {
                            super.onCreate(db)
                            // Default tags
                            val defaultTags = listOf("Personal", "Work", "Family", "Holiday", "Urgent", "Subscription", "Gift")
                            defaultTags.forEach { name ->
                                db.execSQL("INSERT INTO tags (name, colorHex) VALUES ('$name', -1)")
                            }

                            // Default categories
                            // Parent categories
                            db.execSQL("INSERT INTO categories (id, name, iconName, colorHex, isDefault, parentId) VALUES (1, 'Food', 'Restaurant', -12627531, 1, NULL)")
                            db.execSQL("INSERT INTO categories (id, name, iconName, colorHex, isDefault, parentId) VALUES (2, 'Transport', 'DirectionsCar', -14575885, 1, NULL)")
                            db.execSQL("INSERT INTO categories (id, name, iconName, colorHex, isDefault, parentId) VALUES (3, 'Shopping', 'ShoppingBag', -16121, 1, NULL)")
                            db.execSQL("INSERT INTO categories (id, name, iconName, colorHex, isDefault, parentId) VALUES (4, 'Housing', 'Home', -769226, 1, NULL)")
                            db.execSQL("INSERT INTO categories (id, name, iconName, colorHex, isDefault, parentId) VALUES (5, 'Entertainment', 'Movie', -145214, 1, NULL)")
                            db.execSQL("INSERT INTO categories (id, name, iconName, colorHex, isDefault, parentId) VALUES (6, 'Salary', 'Payments', -11751600, 1, NULL)")

                            // Sub categories
                            db.execSQL("INSERT INTO categories (name, iconName, colorHex, isDefault, parentId) VALUES ('Groceries', 'LocalGroceryStore', -12627531, 1, 1)")
                            db.execSQL("INSERT INTO categories (name, iconName, colorHex, isDefault, parentId) VALUES ('Dining Out', 'LocalDining', -12627531, 1, 1)")
                            db.execSQL("INSERT INTO categories (name, iconName, colorHex, isDefault, parentId) VALUES ('Fuel', 'LocalGasStation', -14575885, 1, 2)")
                            db.execSQL("INSERT INTO categories (name, iconName, colorHex, isDefault, parentId) VALUES ('Rent', 'Key', -769226, 1, 4)")
                        }
                    })
                    .addMigrations(MIGRATION_4_5, MIGRATION_5_6, MIGRATION_6_7)
                    .fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
