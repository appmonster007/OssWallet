package com.osscabinet.ossWallet.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
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
        TemplateEntity::class
    ],
    version = 3,
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

    companion object {
        init {
            System.loadLibrary("sqlcipher")
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
                    .fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
