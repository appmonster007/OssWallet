package com.osscabinet.gleemanager.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.osscabinet.gleemanager.data.local.dao.*
import com.osscabinet.gleemanager.data.local.entity.*
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
    version = 20,
    exportSchema = false
)
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
                    "glee_manager_db"
                ).openHelperFactory(factory)
                    .fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
