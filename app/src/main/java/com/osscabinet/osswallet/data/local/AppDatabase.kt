package com.osscabinet.ossWallet.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.sqlite.db.SupportSQLiteDatabase
import com.osscabinet.ossWallet.data.local.dao.*
import com.osscabinet.ossWallet.data.local.entity.*
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
    version = 2,
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
                
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "osswallet_manager_db"
                ).openHelperFactory(factory)
                    .fallbackToDestructiveMigration(true)
                    .addCallback(object : Callback() {
                        override fun onCreate(db: SupportSQLiteDatabase) {
                            super.onCreate(db)
                            
                            // Default Tags
                            val defaultTags = listOf("Personal", "Work", "Family", "Holiday", "Urgent", "Subscription", "Gift", "Travel", "Health", "Education")
                            defaultTags.forEach { name ->
                                db.execSQL("INSERT INTO tags (name, colorHex) VALUES ('$name', -1)")
                            }

                            // Parent Categories
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

                            // Special System Categories
                            db.execSQL("INSERT INTO categories (id, name, iconName, colorHex, isDefault, parentId) VALUES (1000, 'Others', 'Menu', -7829368, 1, NULL)")
                            db.execSQL("INSERT INTO categories (id, name, iconName, colorHex, isDefault, parentId) VALUES (1001, 'Missing', 'MoreHoriz', -7829368, 1, 1000)")
                            db.execSQL("INSERT INTO categories (id, name, iconName, colorHex, isDefault, parentId) VALUES (1002, 'Unknown', 'QuestionMark', -7829368, 1, 1000)")
                            db.execSQL("INSERT INTO categories (id, name, iconName, colorHex, isDefault, parentId) VALUES (1003, 'Transfer', 'SwapHoriz', -7829368, 1, 1000)")

                            // Subcategories
                            db.execSQL("INSERT INTO categories (name, iconName, colorHex, isDefault, parentId) VALUES ('Groceries', 'LocalGroceryStore', -12627531, 1, 1)")
                            db.execSQL("INSERT INTO categories (name, iconName, colorHex, isDefault, parentId) VALUES ('Dining Out', 'LocalDining', -12627531, 1, 1)")
                            db.execSQL("INSERT INTO categories (name, iconName, colorHex, isDefault, parentId) VALUES ('Coffee & Snacks', 'Coffee', -12627531, 1, 1)")
                            db.execSQL("INSERT INTO categories (name, iconName, colorHex, isDefault, parentId) VALUES ('Alcohol', 'LocalBar', -12627531, 1, 1)")
                            db.execSQL("INSERT INTO categories (name, iconName, colorHex, isDefault, parentId) VALUES ('Fuel', 'LocalGasStation', -14575885, 1, 2)")
                            db.execSQL("INSERT INTO categories (name, iconName, colorHex, isDefault, parentId) VALUES ('Public Transit', 'DirectionsBus', -14575885, 1, 2)")
                            db.execSQL("INSERT INTO categories (name, iconName, colorHex, isDefault, parentId) VALUES ('Rideshare', 'LocalTaxi', -14575885, 1, 2)")
                            db.execSQL("INSERT INTO categories (name, iconName, colorHex, isDefault, parentId) VALUES ('Parking', 'LocalParking', -14575885, 1, 2)")
                            db.execSQL("INSERT INTO categories (name, iconName, colorHex, isDefault, parentId) VALUES ('Car Maintenance', 'Build', -14575885, 1, 2)")
                            db.execSQL("INSERT INTO categories (name, iconName, colorHex, isDefault, parentId) VALUES ('Electronics', 'Devices', -16121, 1, 3)")
                            db.execSQL("INSERT INTO categories (name, iconName, colorHex, isDefault, parentId) VALUES ('Clothing', 'Checkroom', -16121, 1, 3)")
                            db.execSQL("INSERT INTO categories (name, iconName, colorHex, isDefault, parentId) VALUES ('Home & Garden', 'LocalFlorist', -16121, 1, 3)")
                            db.execSQL("INSERT INTO categories (name, iconName, colorHex, isDefault, parentId) VALUES ('Books', 'MenuBook', -16121, 1, 3)")
                            db.execSQL("INSERT INTO categories (name, iconName, colorHex, isDefault, parentId) VALUES ('Online Shopping', 'ShoppingCart', -16121, 1, 3)")
                            db.execSQL("INSERT INTO categories (name, iconName, colorHex, isDefault, parentId) VALUES ('Rent', 'Key', -769226, 1, 4)")
                            db.execSQL("INSERT INTO categories (name, iconName, colorHex, isDefault, parentId) VALUES ('Electricity', 'Lightbulb', -769226, 1, 4)")
                            db.execSQL("INSERT INTO categories (name, iconName, colorHex, isDefault, parentId) VALUES ('Water', 'WaterDrop', -769226, 1, 4)")
                            db.execSQL("INSERT INTO categories (name, iconName, colorHex, isDefault, parentId) VALUES ('Internet', 'Wifi', -769226, 1, 4)")
                            db.execSQL("INSERT INTO categories (name, iconName, colorHex, isDefault, parentId) VALUES ('Phone', 'Phone', -769226, 1, 4)")
                            db.execSQL("INSERT INTO categories (name, iconName, colorHex, isDefault, parentId) VALUES ('Insurance', 'Shield', -769226, 1, 4)")
                            db.execSQL("INSERT INTO categories (name, iconName, colorHex, isDefault, parentId) VALUES ('Streaming Services', 'PlayArrow', -145214, 1, 5)")
                            db.execSQL("INSERT INTO categories (name, iconName, colorHex, isDefault, parentId) VALUES ('Movies & Theater', 'Theaters', -145214, 1, 5)")
                            db.execSQL("INSERT INTO categories (name, iconName, colorHex, isDefault, parentId) VALUES ('Gaming', 'SportsEsports', -145214, 1, 5)")
                            db.execSQL("INSERT INTO categories (name, iconName, colorHex, isDefault, parentId) VALUES ('Concerts & Events', 'Event', -145214, 1, 5)")
                            db.execSQL("INSERT INTO categories (name, iconName, colorHex, isDefault, parentId) VALUES ('Gym', 'FitnessCenter', -16728276, 1, 6)")
                            db.execSQL("INSERT INTO categories (name, iconName, colorHex, isDefault, parentId) VALUES ('Medical', 'MedicalServices', -16728276, 1, 6)")
                            db.execSQL("INSERT INTO categories (name, iconName, colorHex, isDefault, parentId) VALUES ('Pharmacy', 'LocalPharmacy', -16728276, 1, 6)")
                            db.execSQL("INSERT INTO categories (name, iconName, colorHex, isDefault, parentId) VALUES ('Sports', 'Sports', -16728276, 1, 6)")
                            db.execSQL("INSERT INTO categories (name, iconName, colorHex, isDefault, parentId) VALUES ('Courses', 'Class', -16711707, 1, 7)")
                            db.execSQL("INSERT INTO categories (name, iconName, colorHex, isDefault, parentId) VALUES ('Books & Supplies', 'AutoStories', -16711707, 1, 7)")
                            db.execSQL("INSERT INTO categories (name, iconName, colorHex, isDefault, parentId) VALUES ('Tuition', 'School', -16711707, 1, 7)")
                            db.execSQL("INSERT INTO categories (name, iconName, colorHex, isDefault, parentId) VALUES ('Salary', 'Payments', -11751600, 1, 8)")
                            db.execSQL("INSERT INTO categories (name, iconName, colorHex, isDefault, parentId) VALUES ('Freelance', 'Work', -11751600, 1, 8)")
                            db.execSQL("INSERT INTO categories (name, iconName, colorHex, isDefault, parentId) VALUES ('Investments', 'TrendingUp', -11751600, 1, 8)")
                            db.execSQL("INSERT INTO categories (name, iconName, colorHex, isDefault, parentId) VALUES ('Gifts', 'CardGiftcard', -11751600, 1, 8)")
                            db.execSQL("INSERT INTO categories (name, iconName, colorHex, isDefault, parentId) VALUES ('Refunds', 'ReceiptLong', -11751600, 1, 8)")
                            db.execSQL("INSERT INTO categories (name, iconName, colorHex, isDefault, parentId) VALUES ('Flights', 'Flight', -16744448, 1, 9)")
                            db.execSQL("INSERT INTO categories (name, iconName, colorHex, isDefault, parentId) VALUES ('Hotels', 'Hotel', -16744448, 1, 9)")
                            db.execSQL("INSERT INTO categories (name, iconName, colorHex, isDefault, parentId) VALUES ('Car Rental', 'DirectionsCar', -16744448, 1, 9)")
                            db.execSQL("INSERT INTO categories (name, iconName, colorHex, isDefault, parentId) VALUES ('Activities', 'Attractions', -16744448, 1, 9)")
                            db.execSQL("INSERT INTO categories (name, iconName, colorHex, isDefault, parentId) VALUES ('Haircut', 'ContentCut', -16738680, 1, 10)")
                            db.execSQL("INSERT INTO categories (name, iconName, colorHex, isDefault, parentId) VALUES ('Beauty', 'Face', -16738680, 1, 10)")
                            db.execSQL("INSERT INTO categories (name, iconName, colorHex, isDefault, parentId) VALUES ('Spa & Massage', 'Spa', -16738680, 1, 10)")
                            db.execSQL("INSERT INTO categories (name, iconName, colorHex, isDefault, parentId) VALUES ('Netflix', 'PlayArrow', -16724736, 1, 11)")
                            db.execSQL("INSERT INTO categories (name, iconName, colorHex, isDefault, parentId) VALUES ('Spotify', 'MusicNote', -16724736, 1, 11)")
                            db.execSQL("INSERT INTO categories (name, iconName, colorHex, isDefault, parentId) VALUES ('Software Subscriptions', 'Laptop', -16724736, 1, 11)")
                            db.execSQL("INSERT INTO categories (name, iconName, colorHex, isDefault, parentId) VALUES ('Cloud Storage', 'Cloud', -16724736, 1, 11)")
                            db.execSQL("INSERT INTO categories (name, iconName, colorHex, isDefault, parentId) VALUES ('Stocks', 'ShowChart', -16711707, 1, 12)")
                            db.execSQL("INSERT INTO categories (name, iconName, colorHex, isDefault, parentId) VALUES ('Crypto', 'CurrencyBitcoin', -16711707, 1, 12)")
                            db.execSQL("INSERT INTO categories (name, iconName, colorHex, isDefault, parentId) VALUES ('Real Estate', 'Domain', -16711707, 1, 12)")
                        }
                    })
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
