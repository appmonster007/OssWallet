package com.osscabinet.ossWallet.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.osscabinet.ossWallet.data.local.dao.*
import com.osscabinet.ossWallet.data.local.entity.TransactionLinkEntity
import com.osscabinet.ossWallet.data.local.entity.*
import com.osscabinet.ossWallet.data.local.entity.BudgetEntity
import com.osscabinet.ossWallet.data.local.entity.LoanEntity
import com.osscabinet.ossWallet.data.local.entity.LoanPaymentEntity
import com.osscabinet.ossWallet.data.local.entity.PlannedPaymentEntity
import com.osscabinet.ossWallet.data.local.entity.PlannedPaymentOccurrenceEntity
import net.zetetic.database.sqlcipher.SupportOpenHelperFactory

@Database(
    entities = [
        TransactionEntity::class,
        TransactionLinkEntity::class,
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
    version = 18,
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

        val MIGRATION_7_8 = object : Migration(7, 8) {
            override fun migrate(database: SupportSQLiteDatabase) {
                // Add status column to transactions with default value 'cleared'
                database.execSQL("ALTER TABLE transactions ADD COLUMN status TEXT NOT NULL DEFAULT 'cleared'")

                // Create index for status
                database.execSQL("CREATE INDEX index_transactions_status ON transactions (status)")

                // Add statuses column to transaction_filters with default value ''
                database.execSQL("ALTER TABLE transaction_filters ADD COLUMN statuses TEXT NOT NULL DEFAULT ''")
            }
        }

        val MIGRATION_9_11 = object : Migration(9, 11) {
            override fun migrate(database: SupportSQLiteDatabase) {
                // Remove balance column from accounts (not derived from transactions)
                database.execSQL("""
                    CREATE TABLE accounts_v11 (
                        id TEXT NOT NULL PRIMARY KEY,
                        name TEXT NOT NULL,
                        currencyCode TEXT NOT NULL,
                        structureType TEXT NOT NULL,
                        colorHex TEXT NOT NULL,
                        icon TEXT NOT NULL,
                        allowedCurrencies TEXT NOT NULL,
                        profileId INTEGER NOT NULL DEFAULT 1,
                        isArchived INTEGER NOT NULL DEFAULT 0,
                        displayOrder INTEGER NOT NULL DEFAULT 0
                    )
                """.trimIndent())
                database.execSQL("""
                    INSERT INTO accounts_v11 (id, name, currencyCode, structureType, colorHex, icon, allowedCurrencies, profileId, isArchived, displayOrder)
                    SELECT id, name, currencyCode, structureType, colorHex, icon, allowedCurrencies, profileId, isArchived, displayOrder
                    FROM accounts
                """.trimIndent())
                database.execSQL("DROP TABLE accounts")
                database.execSQL("ALTER TABLE accounts_v11 RENAME TO accounts")
                database.execSQL("CREATE INDEX index_accounts_profileId ON accounts (profileId)")

                // Add timelineRange to saved filters
                database.execSQL("ALTER TABLE transaction_filters ADD COLUMN timelineRange TEXT NOT NULL DEFAULT ''")

                // Add profiles table
                database.execSQL("""
                    CREATE TABLE IF NOT EXISTS profiles (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        name TEXT NOT NULL,
                        baseCurrency TEXT NOT NULL DEFAULT 'USD',
                        type TEXT NOT NULL DEFAULT 'PERSONAL'
                    )
                """.trimIndent())

                // Add templates table
                database.execSQL("""
                    CREATE TABLE IF NOT EXISTS templates (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        name TEXT NOT NULL,
                        category TEXT NOT NULL,
                        amount TEXT NOT NULL,
                        transactionType INTEGER NOT NULL,
                        icon TEXT,
                        colorHex TEXT,
                        accountId TEXT,
                        note TEXT,
                        payee TEXT,
                        location TEXT,
                        paymentMethod TEXT,
                        currency TEXT,
                        tags TEXT NOT NULL DEFAULT '',
                        isStarred INTEGER NOT NULL DEFAULT 0,
                        displayOrder INTEGER NOT NULL DEFAULT 0
                    )
                """.trimIndent())

                // Add currency_rates table
                database.execSQL("""
                    CREATE TABLE IF NOT EXISTS currency_rates (
                        currencyCode TEXT NOT NULL PRIMARY KEY,
                        baseCurrency TEXT NOT NULL,
                        rate REAL NOT NULL,
                        timestamp INTEGER NOT NULL,
                        nextUpdate INTEGER NOT NULL
                    )
                """.trimIndent())
            }
        }

        val MIGRATION_8_9 = object : Migration(8, 9) {
            override fun migrate(database: SupportSQLiteDatabase) {
                // Create junction table for transaction links
                database.execSQL("""
                    CREATE TABLE transaction_links (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        transactionId INTEGER NOT NULL,
                        linkedTransactionId INTEGER NOT NULL,
                        FOREIGN KEY(transactionId) REFERENCES transactions(id) ON DELETE CASCADE,
                        FOREIGN KEY(linkedTransactionId) REFERENCES transactions(id) ON DELETE CASCADE
                    )
                """)

                // Create indexes for junction table
                database.execSQL("CREATE INDEX index_transaction_links_transactionId ON transaction_links (transactionId)")
                database.execSQL("CREATE INDEX index_transaction_links_linkedTransactionId ON transaction_links (linkedTransactionId)")
                database.execSQL("CREATE UNIQUE INDEX index_transaction_links_transactionId_linkedTransactionId ON transaction_links (transactionId, linkedTransactionId)")

                // Migrate existing linkedTransactionId data to junction table
                database.execSQL("""
                    INSERT INTO transaction_links (transactionId, linkedTransactionId)
                    SELECT id, linkedTransactionId
                    FROM transactions
                    WHERE linkedTransactionId IS NOT NULL
                """)

                // For transfers, also create reverse links
                database.execSQL("""
                    INSERT INTO transaction_links (transactionId, linkedTransactionId)
                    SELECT linkedTransactionId, id
                    FROM transactions
                    WHERE linkedTransactionId IS NOT NULL AND category = 'Transfer'
                """)

                // Drop linkedTransactionId column from transactions table
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
                        profileId INTEGER NOT NULL,
                        latitude REAL,
                        longitude REAL,
                        warrantyExpiration INTEGER,
                        status TEXT NOT NULL,
                        FOREIGN KEY(accountId) REFERENCES accounts(id) ON DELETE CASCADE,
                        FOREIGN KEY(categoryId) REFERENCES categories(id) ON DELETE SET NULL
                    )
                """)

                database.execSQL("""
                    INSERT INTO transactions_new (id, title, amount, category, timestamp, currency, transactionType, accountId, categoryId, tags, paymentMethod, payee, note, recurring, profileId, latitude, longitude, warrantyExpiration, status)
                    SELECT id, title, amount, category, timestamp, currency, transactionType, accountId, categoryId, tags, paymentMethod, payee, note, recurring, profileId, latitude, longitude, warrantyExpiration, status
                    FROM transactions
                """)

                database.execSQL("DROP TABLE transactions")
                database.execSQL("ALTER TABLE transactions_new RENAME TO transactions")

                // Recreate indexes
                database.execSQL("CREATE INDEX index_transactions_accountId ON transactions (accountId)")
                database.execSQL("CREATE INDEX index_transactions_categoryId ON transactions (categoryId)")
                database.execSQL("CREATE INDEX index_transactions_profileId ON transactions (profileId)")
                database.execSQL("CREATE INDEX index_transactions_timestamp ON transactions (timestamp)")
                database.execSQL("CREATE INDEX index_transactions_status ON transactions (status)")
            }
        }

                val MIGRATION_11_12 = object : Migration(11, 12) {
                    override fun migrate(database: SupportSQLiteDatabase) {
                        database.execSQL("""
                            CREATE TABLE planned_payments (
                                id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                                title TEXT NOT NULL,
                                amount TEXT NOT NULL,
                                currency TEXT NOT NULL,
                                transactionType TEXT NOT NULL,
                                accountId TEXT NOT NULL,
                                targetAccountId TEXT,
                                categoryId INTEGER,
                                paymentMethod TEXT NOT NULL DEFAULT 'Cash',
                                tags TEXT NOT NULL DEFAULT '',
                                note TEXT NOT NULL DEFAULT '',
                                payee TEXT NOT NULL DEFAULT '',
                                recurrenceType TEXT NOT NULL,
                                recurrenceInterval INTEGER NOT NULL DEFAULT 1,
                                recurrenceDayOfWeek INTEGER,
                                recurrenceDayOfMonth INTEGER,
                                startDate INTEGER NOT NULL,
                                endDate INTEGER,
                                nextDueDate INTEGER NOT NULL,
                                lastTriggeredDate INTEGER,
                                autoConfirm INTEGER NOT NULL DEFAULT 0,
                                status TEXT NOT NULL DEFAULT 'ACTIVE',
                                profileId INTEGER NOT NULL,
                                FOREIGN KEY(accountId) REFERENCES accounts(id) ON DELETE CASCADE
                            )
                        """.trimIndent())
                        database.execSQL("CREATE INDEX index_planned_payments_profileId ON planned_payments (profileId)")
                        database.execSQL("CREATE INDEX index_planned_payments_accountId ON planned_payments (accountId)")
                        database.execSQL("CREATE INDEX index_planned_payments_nextDueDate ON planned_payments (nextDueDate)")
                        database.execSQL("CREATE INDEX index_planned_payments_status ON planned_payments (status)")

                        database.execSQL("""
                            CREATE TABLE planned_payment_occurrences (
                                id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                                plannedPaymentId INTEGER NOT NULL,
                                scheduledDate INTEGER NOT NULL,
                                generatedTransactionId INTEGER,
                                status TEXT NOT NULL DEFAULT 'PENDING',
                                FOREIGN KEY(plannedPaymentId) REFERENCES planned_payments(id) ON DELETE CASCADE
                            )
                        """.trimIndent())
                        database.execSQL("CREATE INDEX index_ppo_plannedPaymentId ON planned_payment_occurrences (plannedPaymentId)")
                        database.execSQL("CREATE INDEX index_ppo_scheduledDate ON planned_payment_occurrences (scheduledDate)")
                        database.execSQL("CREATE INDEX index_ppo_status ON planned_payment_occurrences (status)")
                    }
                }

                val MIGRATION_12_13 = object : Migration(12, 13) {
                    override fun migrate(database: SupportSQLiteDatabase) {
                        database.execSQL("""
                            CREATE TABLE budgets (
                                id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                                name TEXT NOT NULL,
                                currency TEXT NOT NULL,
                                limitAmount TEXT NOT NULL,
                                period TEXT NOT NULL,
                                categoryIds TEXT NOT NULL DEFAULT '',
                                accountIds TEXT NOT NULL DEFAULT '',
                                alertThreshold REAL NOT NULL DEFAULT 0.8,
                                rollover INTEGER NOT NULL DEFAULT 0,
                                trackType TEXT NOT NULL DEFAULT 'EXPENSE',
                                isArchived INTEGER NOT NULL DEFAULT 0,
                                profileId INTEGER NOT NULL,
                                customStartDate INTEGER,
                                customEndDate INTEGER
                            )
                        """.trimIndent())
                        database.execSQL("CREATE INDEX index_budgets_profileId ON budgets (profileId)")
                        database.execSQL("CREATE INDEX index_budgets_isArchived ON budgets (isArchived)")
                    }
                }

                val MIGRATION_13_14 = object : Migration(13, 14) {
                    override fun migrate(database: SupportSQLiteDatabase) {
                        database.execSQL("""
                            CREATE TABLE loans (
                                id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                                name TEXT NOT NULL,
                                principalAmount TEXT NOT NULL,
                                currency TEXT NOT NULL,
                                interestRate REAL,
                                startDate INTEGER NOT NULL,
                                lenderOrBorrowerName TEXT NOT NULL,
                                loanType TEXT NOT NULL,
                                linkedAccountId TEXT NOT NULL,
                                expectedSettlementDate INTEGER,
                                status TEXT NOT NULL DEFAULT 'ACTIVE',
                                profileId INTEGER NOT NULL,
                                note TEXT NOT NULL DEFAULT '',
                                FOREIGN KEY(linkedAccountId) REFERENCES accounts(id) ON DELETE SET NULL
                            )
                        """.trimIndent())
                        database.execSQL("CREATE INDEX index_loans_profileId ON loans (profileId)")
                        database.execSQL("CREATE INDEX index_loans_status ON loans (status)")
                        database.execSQL("CREATE INDEX index_loans_linkedAccountId ON loans (linkedAccountId)")

                        database.execSQL("""
                            CREATE TABLE loan_payments (
                                id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                                loanId INTEGER NOT NULL,
                                transactionId INTEGER,
                                amount TEXT NOT NULL,
                                date INTEGER NOT NULL,
                                note TEXT NOT NULL DEFAULT '',
                                FOREIGN KEY(loanId) REFERENCES loans(id) ON DELETE CASCADE
                            )
                        """.trimIndent())
                        database.execSQL("CREATE INDEX index_loan_payments_loanId ON loan_payments (loanId)")
                        database.execSQL("CREATE INDEX index_loan_payments_transactionId ON loan_payments (transactionId)")
                    }
                }

                val MIGRATION_14_15 = object : Migration(14, 15) {
                    override fun migrate(database: SupportSQLiteDatabase) {
                        database.execSQL("ALTER TABLE budgets ADD COLUMN tags TEXT NOT NULL DEFAULT ''")
                    }
                }

                val MIGRATION_15_16 = object : Migration(15, 16) {
                    override fun migrate(database: SupportSQLiteDatabase) {
                        database.execSQL("ALTER TABLE loans ADD COLUMN categoryIds TEXT NOT NULL DEFAULT ''")
                        database.execSQL("ALTER TABLE loans ADD COLUMN tags TEXT NOT NULL DEFAULT ''")
                    }
                }

                val MIGRATION_16_17 = object : Migration(16, 17) {
                    override fun migrate(database: SupportSQLiteDatabase) {
                        // 1. Seed mandatory categories
                        database.execSQL("INSERT OR IGNORE INTO categories (id, name, iconName, colorHex, isDefault, parentId) VALUES (1000, 'Others', 'Menu', -7829368, 1, NULL)")
                        database.execSQL("INSERT OR IGNORE INTO categories (id, name, iconName, colorHex, isDefault, parentId) VALUES (1001, 'Missing', 'MoreHoriz', -7829368, 1, 1000)")
                        database.execSQL("INSERT OR IGNORE INTO categories (id, name, iconName, colorHex, isDefault, parentId) VALUES (1002, 'Unknown', 'QuestionMark', -7829368, 1, 1000)")
                        database.execSQL("INSERT OR IGNORE INTO categories (id, name, iconName, colorHex, isDefault, parentId) VALUES (1003, 'Transfer', 'SwapHoriz', -7829368, 1, 1000)")

                        // 2. Data cleanup for transactions
                        database.execSQL("UPDATE transactions SET categoryId = 1003 WHERE category = 'Transfer'")
                        database.execSQL("UPDATE transactions SET categoryId = 1002 WHERE categoryId IS NULL")

                        // 3. Recreate transactions table (Remove 'category' column, categoryId NOT NULL)
                        database.execSQL("""
                            CREATE TABLE transactions_v17 (
                                id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                                title TEXT NOT NULL,
                                amount TEXT NOT NULL,
                                timestamp INTEGER NOT NULL,
                                currency TEXT NOT NULL,
                                transactionType TEXT NOT NULL,
                                accountId TEXT NOT NULL,
                                categoryId INTEGER NOT NULL,
                                tags TEXT NOT NULL,
                                paymentMethod TEXT NOT NULL,
                                payee TEXT NOT NULL,
                                note TEXT NOT NULL,
                                recurring TEXT,
                                profileId INTEGER NOT NULL,
                                latitude REAL,
                                longitude REAL,
                                warrantyExpiration INTEGER,
                                status TEXT NOT NULL,
                                FOREIGN KEY(accountId) REFERENCES accounts(id) ON DELETE CASCADE,
                                FOREIGN KEY(categoryId) REFERENCES categories(id) ON DELETE RESTRICT
                            )
                        """)
                        database.execSQL("""
                            INSERT INTO transactions_v17 (id, title, amount, timestamp, currency, transactionType, accountId, categoryId, tags, paymentMethod, payee, note, recurring, profileId, latitude, longitude, warrantyExpiration, status)
                            SELECT id, title, amount, timestamp, currency, transactionType, accountId, categoryId, tags, paymentMethod, payee, note, recurring, profileId, latitude, longitude, warrantyExpiration, status
                            FROM transactions
                        """)
                        database.execSQL("DROP TABLE transactions")
                        database.execSQL("ALTER TABLE transactions_v17 RENAME TO transactions")
                        database.execSQL("CREATE INDEX index_transactions_accountId ON transactions (accountId)")
                        database.execSQL("CREATE INDEX index_transactions_categoryId ON transactions (categoryId)")
                        database.execSQL("CREATE INDEX index_transactions_profileId ON transactions (profileId)")
                        database.execSQL("CREATE INDEX index_transactions_timestamp ON transactions (timestamp)")
                        database.execSQL("CREATE INDEX index_transactions_status ON transactions (status)")

                        // 4. Data cleanup for planned_payments
                        database.execSQL("UPDATE planned_payments SET categoryId = 1002 WHERE categoryId IS NULL")

                        // 5. Recreate planned_payments table (Remove 'category' column, categoryId NOT NULL)
                        database.execSQL("""
                            CREATE TABLE planned_payments_v17 (
                                id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                                title TEXT NOT NULL,
                                amount TEXT NOT NULL,
                                currency TEXT NOT NULL,
                                transactionType TEXT NOT NULL,
                                accountId TEXT NOT NULL,
                                targetAccountId TEXT,
                                categoryId INTEGER NOT NULL,
                                paymentMethod TEXT NOT NULL,
                                tags TEXT NOT NULL,
                                note TEXT NOT NULL,
                                payee TEXT NOT NULL,
                                recurrenceType TEXT NOT NULL,
                                recurrenceInterval INTEGER NOT NULL,
                                recurrenceDayOfWeek INTEGER,
                                recurrenceDayOfMonth INTEGER,
                                startDate INTEGER NOT NULL,
                                endDate INTEGER,
                                nextDueDate INTEGER NOT NULL,
                                lastTriggeredDate INTEGER,
                                autoConfirm INTEGER NOT NULL,
                                status TEXT NOT NULL,
                                profileId INTEGER NOT NULL,
                                FOREIGN KEY(accountId) REFERENCES accounts(id) ON DELETE CASCADE,
                                FOREIGN KEY(categoryId) REFERENCES categories(id) ON DELETE RESTRICT
                            )
                        """)
                        database.execSQL("""
                            INSERT INTO planned_payments_v17 (id, title, amount, currency, transactionType, accountId, targetAccountId, categoryId, paymentMethod, tags, note, payee, recurrenceType, recurrenceInterval, recurrenceDayOfWeek, recurrenceDayOfMonth, startDate, endDate, nextDueDate, lastTriggeredDate, autoConfirm, status, profileId)
                            SELECT id, title, amount, currency, transactionType, accountId, targetAccountId, categoryId, paymentMethod, tags, note, payee, recurrenceType, recurrenceInterval, recurrenceDayOfWeek, recurrenceDayOfMonth, startDate, endDate, nextDueDate, lastTriggeredDate, autoConfirm, status, profileId
                            FROM planned_payments
                        """)
                        database.execSQL("DROP TABLE planned_payments")
                        database.execSQL("ALTER TABLE planned_payments_v17 RENAME TO planned_payments")
                        database.execSQL("CREATE INDEX index_planned_payments_profileId ON planned_payments (profileId)")
                        database.execSQL("CREATE INDEX index_planned_payments_accountId ON planned_payments (accountId)")
                        database.execSQL("CREATE INDEX index_planned_payments_nextDueDate ON planned_payments (nextDueDate)")
                        database.execSQL("CREATE INDEX index_planned_payments_status ON planned_payments (status)")

                        // 6. Recreate templates table (Add 'categoryId', Remove 'category')
                        database.execSQL("""
                            CREATE TABLE templates_v17 (
                                id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                                name TEXT NOT NULL,
                                categoryId INTEGER NOT NULL,
                                amount TEXT NOT NULL,
                                transactionType INTEGER NOT NULL,
                                icon TEXT,
                                colorHex TEXT,
                                accountId TEXT,
                                note TEXT,
                                payee TEXT,
                                location TEXT,
                                paymentMethod TEXT,
                                currency TEXT,
                                tags TEXT NOT NULL,
                                isStarred INTEGER NOT NULL,
                                displayOrder INTEGER NOT NULL,
                                FOREIGN KEY(categoryId) REFERENCES categories(id) ON DELETE RESTRICT
                            )
                        """)
                        database.execSQL("""
                            INSERT INTO templates_v17 (id, name, categoryId, amount, transactionType, icon, colorHex, accountId, note, payee, location, paymentMethod, currency, tags, isStarred, displayOrder)
                            SELECT id, name, 1002, amount, transactionType, icon, colorHex, accountId, note, payee, location, paymentMethod, currency, tags, isStarred, displayOrder
                            FROM templates
                        """)
                        database.execSQL("DROP TABLE templates")
                        database.execSQL("ALTER TABLE templates_v17 RENAME TO templates")
                    }
                }

                val MIGRATION_17_18 = object : Migration(17, 18) {
                    override fun migrate(database: SupportSQLiteDatabase) {
                        database.execSQL("""
                            CREATE TABLE planned_payments_v18 (
                                id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                                title TEXT NOT NULL,
                                amount TEXT NOT NULL,
                                currency TEXT NOT NULL,
                                transactionType TEXT NOT NULL,
                                accountId TEXT NOT NULL,
                                targetAccountId TEXT,
                                targetCurrency TEXT,
                                transferRate TEXT NOT NULL DEFAULT '1.0',
                                categoryId INTEGER NOT NULL,
                                paymentMethod TEXT NOT NULL,
                                tags TEXT NOT NULL,
                                note TEXT NOT NULL,
                                payee TEXT NOT NULL,
                                recurrenceCronExpression TEXT,
                                startDate INTEGER NOT NULL,
                                endDate INTEGER,
                                nextDueDate INTEGER NOT NULL,
                                lastTriggeredDate INTEGER,
                                autoConfirm INTEGER NOT NULL,
                                status TEXT NOT NULL,
                                profileId INTEGER NOT NULL,
                                FOREIGN KEY(accountId) REFERENCES accounts(id) ON DELETE CASCADE,
                                FOREIGN KEY(categoryId) REFERENCES categories(id) ON DELETE RESTRICT
                            )
                        """.trimIndent())

                        database.execSQL("""
                            INSERT INTO planned_payments_v18 (
                                id, title, amount, currency, transactionType, accountId, targetAccountId, 
                                categoryId, paymentMethod, tags, note, payee, 
                                startDate, endDate, nextDueDate, lastTriggeredDate, autoConfirm, status, profileId
                            )
                            SELECT 
                                id, title, amount, currency, transactionType, accountId, targetAccountId, 
                                categoryId, paymentMethod, tags, note, payee, 
                                startDate, endDate, nextDueDate, lastTriggeredDate, autoConfirm, status, profileId
                            FROM planned_payments
                        """.trimIndent())

                        database.execSQL("DROP TABLE planned_payments")
                        database.execSQL("ALTER TABLE planned_payments_v18 RENAME TO planned_payments")

                        database.execSQL("CREATE INDEX index_planned_payments_profileId ON planned_payments (profileId)")
                        database.execSQL("CREATE INDEX index_planned_payments_accountId ON planned_payments (accountId)")
                        database.execSQL("CREATE INDEX index_planned_payments_nextDueDate ON planned_payments (nextDueDate)")
                        database.execSQL("CREATE INDEX index_planned_payments_status ON planned_payments (status)")
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
                            val defaultTags = listOf("Personal", "Work", "Family", "Holiday", "Urgent", "Subscription", "Gift", "Travel", "Health", "Education")
                            defaultTags.forEach { name ->
                                db.execSQL("INSERT INTO tags (name, colorHex) VALUES ('$name', -1)")
                            }

                            // Default categories with realistic structure
                            // Parent categories
                            db.execSQL("INSERT INTO categories (id, name, iconName, colorHex, isDefault, parentId) VALUES (1, 'Food & Dining', 'Restaurant', -12627531, 1, NULL)")
                            db.execSQL("INSERT INTO categories (id, name, iconName, colorHex, isDefault, parentId) VALUES (2, 'Transportation', 'DirectionsCar', -14575885, 1, NULL)")
                            db.execSQL("INSERT INTO categories (id, name, iconName, colorHex, isDefault, parentId) VALUES (3, 'Shopping', 'ShoppingBag', -16121, 1, NULL)")
                            db.execSQL("INSERT INTO categories (id, name, iconName, colorHex, isDefault, parentId) VALUES (4, 'Housing & Utilities', 'Home', -769226, 1, NULL)")
                            db.execSQL("INSERT INTO categories (id, name, iconName, colorHex, isDefault, parentId) VALUES (5, 'Entertainment', 'Movie', -145214, 1, NULL)")
                            db.execSQL("INSERT INTO categories (id, name, iconName, colorHex, isDefault, parentId) VALUES (6, 'Health & Fitness', 'Favorite', -16728276, 1, NULL)")
                            db.execSQL("INSERT INTO categories (id, name, iconName, colorHex, isDefault, parentId) VALUES (7, 'Education', 'School', -16711707, 1, NULL)")
                            db.execSQL("INSERT INTO categories (id, name, iconName, colorHex, isDefault, parentId) VALUES (8, 'Income', 'Payments', -11751600, 1, NULL)")
                            db.execSQL("INSERT INTO categories (id, name, iconName, colorHex, isDefault, parentId) VALUES (9, 'Travel', 'Flight', -16744448, 1, NULL)")
                            db.execSQL("INSERT INTO categories (id, name, iconName, colorHex, isDefault, parentId) VALUES (10, 'Personal Care', 'Spa', -16738680, 1, NULL)")
                            db.execSQL("INSERT INTO categories (id, name, iconName, colorHex, isDefault, parentId) VALUES (11, 'Bills & Subscriptions', 'Receipt', -16724736, 1, NULL)")
                            db.execSQL("INSERT INTO categories (id, name, iconName, colorHex, isDefault, parentId) VALUES (12, 'Investments', 'TrendingUp', -16711707, 1, NULL)")

                            // Others Root structure
                            db.execSQL("INSERT INTO categories (id, name, iconName, colorHex, isDefault, parentId) VALUES (1000, 'Others', 'Menu', -7829368, 1, NULL)")
                            db.execSQL("INSERT INTO categories (id, name, iconName, colorHex, isDefault, parentId) VALUES (1001, 'Missing', 'MoreHoriz', -7829368, 1, 1000)")
                            db.execSQL("INSERT INTO categories (id, name, iconName, colorHex, isDefault, parentId) VALUES (1002, 'Unknown', 'QuestionMark', -7829368, 1, 1000)")
                            db.execSQL("INSERT INTO categories (id, name, iconName, colorHex, isDefault, parentId) VALUES (1003, 'Transfer', 'SwapHoriz', -7829368, 1, 1000)")

                            // Food & Dining subcategories
                            db.execSQL("INSERT INTO categories (name, iconName, colorHex, isDefault, parentId) VALUES ('Groceries', 'LocalGroceryStore', -12627531, 1, 1)")
                            db.execSQL("INSERT INTO categories (name, iconName, colorHex, isDefault, parentId) VALUES ('Dining Out', 'LocalDining', -12627531, 1, 1)")
                            db.execSQL("INSERT INTO categories (name, iconName, colorHex, isDefault, parentId) VALUES ('Coffee & Snacks', 'Coffee', -12627531, 1, 1)")
                            db.execSQL("INSERT INTO categories (name, iconName, colorHex, isDefault, parentId) VALUES ('Alcohol', 'LocalBar', -12627531, 1, 1)")

                            // Transportation subcategories
                            db.execSQL("INSERT INTO categories (name, iconName, colorHex, isDefault, parentId) VALUES ('Fuel', 'LocalGasStation', -14575885, 1, 2)")
                            db.execSQL("INSERT INTO categories (name, iconName, colorHex, isDefault, parentId) VALUES ('Public Transit', 'DirectionsBus', -14575885, 1, 2)")
                            db.execSQL("INSERT INTO categories (name, iconName, colorHex, isDefault, parentId) VALUES ('Rideshare', 'LocalTaxi', -14575885, 1, 2)")
                            db.execSQL("INSERT INTO categories (name, iconName, colorHex, isDefault, parentId) VALUES ('Parking', 'LocalParking', -14575885, 1, 2)")
                            db.execSQL("INSERT INTO categories (name, iconName, colorHex, isDefault, parentId) VALUES ('Car Maintenance', 'Build', -14575885, 1, 2)")

                            // Shopping subcategories
                            db.execSQL("INSERT INTO categories (name, iconName, colorHex, isDefault, parentId) VALUES ('Electronics', 'Devices', -16121, 1, 3)")
                            db.execSQL("INSERT INTO categories (name, iconName, colorHex, isDefault, parentId) VALUES ('Clothing', 'Checkroom', -16121, 1, 3)")
                            db.execSQL("INSERT INTO categories (name, iconName, colorHex, isDefault, parentId) VALUES ('Home & Garden', 'LocalFlorist', -16121, 1, 3)")
                            db.execSQL("INSERT INTO categories (name, iconName, colorHex, isDefault, parentId) VALUES ('Books', 'MenuBook', -16121, 1, 3)")
                            db.execSQL("INSERT INTO categories (name, iconName, colorHex, isDefault, parentId) VALUES ('Online Shopping', 'ShoppingCart', -16121, 1, 3)")

                            // Housing & Utilities subcategories
                            db.execSQL("INSERT INTO categories (name, iconName, colorHex, isDefault, parentId) VALUES ('Rent', 'Key', -769226, 1, 4)")
                            db.execSQL("INSERT INTO categories (name, iconName, colorHex, isDefault, parentId) VALUES ('Electricity', 'Lightbulb', -769226, 1, 4)")
                            db.execSQL("INSERT INTO categories (name, iconName, colorHex, isDefault, parentId) VALUES ('Water', 'WaterDrop', -769226, 1, 4)")
                            db.execSQL("INSERT INTO categories (name, iconName, colorHex, isDefault, parentId) VALUES ('Internet', 'Wifi', -769226, 1, 4)")
                            db.execSQL("INSERT INTO categories (name, iconName, colorHex, isDefault, parentId) VALUES ('Phone', 'Phone', -769226, 1, 4)")
                            db.execSQL("INSERT INTO categories (name, iconName, colorHex, isDefault, parentId) VALUES ('Insurance', 'Shield', -769226, 1, 4)")

                            // Entertainment subcategories
                            db.execSQL("INSERT INTO categories (name, iconName, colorHex, isDefault, parentId) VALUES ('Streaming Services', 'PlayArrow', -145214, 1, 5)")
                            db.execSQL("INSERT INTO categories (name, iconName, colorHex, isDefault, parentId) VALUES ('Movies & Theater', 'Theaters', -145214, 1, 5)")
                            db.execSQL("INSERT INTO categories (name, iconName, colorHex, isDefault, parentId) VALUES ('Gaming', 'SportsEsports', -145214, 1, 5)")
                            db.execSQL("INSERT INTO categories (name, iconName, colorHex, isDefault, parentId) VALUES ('Concerts & Events', 'Event', -145214, 1, 5)")

                            // Health & Fitness subcategories
                            db.execSQL("INSERT INTO categories (name, iconName, colorHex, isDefault, parentId) VALUES ('Gym', 'FitnessCenter', -16728276, 1, 6)")
                            db.execSQL("INSERT INTO categories (name, iconName, colorHex, isDefault, parentId) VALUES ('Medical', 'MedicalServices', -16728276, 1, 6)")
                            db.execSQL("INSERT INTO categories (name, iconName, colorHex, isDefault, parentId) VALUES ('Pharmacy', 'LocalPharmacy', -16728276, 1, 6)")
                            db.execSQL("INSERT INTO categories (name, iconName, colorHex, isDefault, parentId) VALUES ('Sports', 'Sports', -16728276, 1, 6)")

                            // Education subcategories
                            db.execSQL("INSERT INTO categories (name, iconName, colorHex, isDefault, parentId) VALUES ('Courses', 'Class', -16711707, 1, 7)")
                            db.execSQL("INSERT INTO categories (name, iconName, colorHex, isDefault, parentId) VALUES ('Books & Supplies', 'AutoStories', -16711707, 1, 7)")
                            db.execSQL("INSERT INTO categories (name, iconName, colorHex, isDefault, parentId) VALUES ('Tuition', 'School', -16711707, 1, 7)")

                            // Income subcategories
                            db.execSQL("INSERT INTO categories (name, iconName, colorHex, isDefault, parentId) VALUES ('Salary', 'Payments', -11751600, 1, 8)")
                            db.execSQL("INSERT INTO categories (name, iconName, colorHex, isDefault, parentId) VALUES ('Freelance', 'Work', -11751600, 1, 8)")
                            db.execSQL("INSERT INTO categories (name, iconName, colorHex, isDefault, parentId) VALUES ('Investments', 'TrendingUp', -11751600, 1, 8)")
                            db.execSQL("INSERT INTO categories (name, iconName, colorHex, isDefault, parentId) VALUES ('Gifts', 'CardGiftcard', -11751600, 1, 8)")
                            db.execSQL("INSERT INTO categories (name, iconName, colorHex, isDefault, parentId) VALUES ('Refunds', 'ReceiptLong', -11751600, 1, 8)")

                            // Travel subcategories
                            db.execSQL("INSERT INTO categories (name, iconName, colorHex, isDefault, parentId) VALUES ('Flights', 'Flight', -16744448, 1, 9)")
                            db.execSQL("INSERT INTO categories (name, iconName, colorHex, isDefault, parentId) VALUES ('Hotels', 'Hotel', -16744448, 1, 9)")
                            db.execSQL("INSERT INTO categories (name, iconName, colorHex, isDefault, parentId) VALUES ('Car Rental', 'DirectionsCar', -16744448, 1, 9)")
                            db.execSQL("INSERT INTO categories (name, iconName, colorHex, isDefault, parentId) VALUES ('Activities', 'Attractions', -16744448, 1, 9)")

                            // Personal Care subcategories
                            db.execSQL("INSERT INTO categories (name, iconName, colorHex, isDefault, parentId) VALUES ('Haircut', 'ContentCut', -16738680, 1, 10)")
                            db.execSQL("INSERT INTO categories (name, iconName, colorHex, isDefault, parentId) VALUES ('Beauty', 'Face', -16738680, 1, 10)")
                            db.execSQL("INSERT INTO categories (name, iconName, colorHex, isDefault, parentId) VALUES ('Spa & Massage', 'Spa', -16738680, 1, 10)")

                            // Bills & Subscriptions subcategories
                            db.execSQL("INSERT INTO categories (name, iconName, colorHex, isDefault, parentId) VALUES ('Netflix', 'PlayArrow', -16724736, 1, 11)")
                            db.execSQL("INSERT INTO categories (name, iconName, colorHex, isDefault, parentId) VALUES ('Spotify', 'MusicNote', -16724736, 1, 11)")
                            db.execSQL("INSERT INTO categories (name, iconName, colorHex, isDefault, parentId) VALUES ('Software Subscriptions', 'Laptop', -16724736, 1, 11)")
                            db.execSQL("INSERT INTO categories (name, iconName, colorHex, isDefault, parentId) VALUES ('Cloud Storage', 'Cloud', -16724736, 1, 11)")

                            // Investments subcategories
                            db.execSQL("INSERT INTO categories (name, iconName, colorHex, isDefault, parentId) VALUES ('Stocks', 'ShowChart', -16711707, 1, 12)")
                            db.execSQL("INSERT INTO categories (name, iconName, colorHex, isDefault, parentId) VALUES ('Crypto', 'CurrencyBitcoin', -16711707, 1, 12)")
                            db.execSQL("INSERT INTO categories (name, iconName, colorHex, isDefault, parentId) VALUES ('Real Estate', 'Domain', -16711707, 1, 12)")
                        }
                    })
                    .addMigrations(MIGRATION_4_5, MIGRATION_5_6, MIGRATION_6_7, MIGRATION_7_8, MIGRATION_8_9, MIGRATION_9_11, MIGRATION_11_12, MIGRATION_12_13, MIGRATION_13_14, MIGRATION_14_15, MIGRATION_15_16, MIGRATION_16_17, MIGRATION_17_18)
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
