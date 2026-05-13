package com.osscabinet.ossWallet.data.seed

import com.osscabinet.ossWallet.data.local.SettingsDataStore
import com.osscabinet.ossWallet.data.local.dao.AccountDao
import com.osscabinet.ossWallet.data.local.dao.CategoryDao
import com.osscabinet.ossWallet.data.local.dao.TransactionDao
import com.osscabinet.ossWallet.data.local.dao.FilterDao
import com.osscabinet.ossWallet.data.local.dao.ProfileDao
import com.osscabinet.ossWallet.data.local.entity.AccountEntity
import com.osscabinet.ossWallet.data.local.entity.CategoryEntity
import com.osscabinet.ossWallet.data.local.entity.TransactionEntity
import com.osscabinet.ossWallet.data.local.entity.FilterEntity
import com.osscabinet.ossWallet.data.local.entity.ProfileEntity
import com.osscabinet.ossWallet.BuildConfig
import android.content.Context
import java.io.BufferedReader
import java.io.InputStreamReader
import java.math.BigDecimal
import javax.inject.Inject
import kotlinx.coroutines.flow.first

private fun parseCsvLine(line: String): List<String> {
    val result = mutableListOf<String>()
    var current = StringBuilder()
    var inQuotes = false

    for (char in line) {
        when (char) {
            '"' -> {
                inQuotes = !inQuotes
            }
            ',' -> {
                if (inQuotes) {
                    current.append(char)
                } else {
                    result.add(current.toString())
                    current = StringBuilder()
                }
            }
            else -> {
                current.append(char)
            }
        }
    }
    result.add(current.toString())
    return result
}

class DatabaseSeeder @Inject constructor(
    private val accountDao: AccountDao,
    private val categoryDao: CategoryDao,
    private val transactionDao: TransactionDao,
    private val filterDao: FilterDao,
    private val profileDao: ProfileDao,
    private val context: Context,
    private val settingsDataStore: SettingsDataStore
) {
    suspend fun seed() {
        if (!BuildConfig.DEBUG) return

        // Check if already seeded
        val isSeeded = settingsDataStore.isDatabaseSeeded.first()
        if (isSeeded) {
            android.util.Log.d("DatabaseSeeder", "Database already seeded, skipping")
            return
        }

        android.util.Log.d("DatabaseSeeder", "Starting database seeding...")

        // Clear existing data for a fresh start in dev
        accountDao.clearAll()
        transactionDao.clearAll()
        categoryDao.clearAll()
        filterDao.clearAll()
        profileDao.clearAll()
        android.util.Log.d("DatabaseSeeder", "Cleared existing data")

        // Insert default profile
        profileDao.insertProfile(ProfileEntity(id = 1, name = "Default Profile", baseCurrency = "USD", type = "PERSONAL"))
        android.util.Log.d("DatabaseSeeder", "Inserted default profile")

        // Re-insert mandatory categories
        categoryDao.insertCategory(CategoryEntity(id = 1000, name = "Others", iconName = "Menu", colorHex = -7829368, isDefault = true, parentId = null))
        categoryDao.insertCategory(CategoryEntity(id = 1001, name = "Missing", iconName = "MoreHoriz", colorHex = -7829368, isDefault = true, parentId = 1000))
        categoryDao.insertCategory(CategoryEntity(id = 1002, name = "Unknown", iconName = "QuestionMark", colorHex = -7829368, isDefault = true, parentId = 1000))
        categoryDao.insertCategory(CategoryEntity(id = 1003, name = "Transfer", iconName = "SwapHoriz", colorHex = -7829368, isDefault = true, parentId = 1000))
        android.util.Log.d("DatabaseSeeder", "Re-inserted mandatory categories")

        // Load seed data from separate CSV files
        val accounts = loadAccountsFromCsv()
        android.util.Log.d("DatabaseSeeder", "Loaded ${accounts.size} accounts from CSV")

        val categories = loadCategoriesFromCsv()
        android.util.Log.d("DatabaseSeeder", "Loaded ${categories.size} categories from CSV")

        val transactions = loadTransactionsFromCsv()
        android.util.Log.d("DatabaseSeeder", "Loaded ${transactions.size} transactions from CSV")

        // Validate that we have data to seed
        if (accounts.isEmpty()) {
            android.util.Log.e("DatabaseSeeder", "No accounts found in CSV, aborting seeding")
            return
        }

        if (categories.isEmpty()) {
            android.util.Log.e("DatabaseSeeder", "No categories found in CSV, aborting seeding")
            return
        }

        // Insert categories (parents first, then children)
        val categoryNameToId = mutableMapOf<String, Long>()
        val categoryNameToColor = mutableMapOf<String, Int>()
        
        // First pass: insert parent categories (where parentName is null or empty)
        val parentCategories = categories.filter { it.parentName.isNullOrEmpty() }
        parentCategories.forEach { category ->
            categoryDao.insertCategory(category.toCategoryEntity())
            categoryNameToColor[category.name] = category.colorHex.removePrefix("#").toLongOrNull(16)?.toInt() ?: -16777216
            android.util.Log.d("DatabaseSeeder", "Inserted parent category: ${category.name}")
        }
        
        // Query all categories to get their IDs
        val allCategories = categoryDao.getAllCategoriesSync()
        allCategories.forEach { category ->
            categoryNameToId[category.name] = category.id
        }
        
        // Second pass: insert child categories with correct parentId and inherited color
        val childCategories = categories.filter { !it.parentName.isNullOrEmpty() }
        childCategories.forEach { category ->
            val parentId = categoryNameToId[category.parentName]
            if (parentId != null) {
                // Inherit parent color if child color is empty
                val colorHex = if (category.colorHex.isBlank()) {
                    categoryNameToColor[category.parentName] ?: -16777216
                } else {
                    category.colorHex.removePrefix("#").toLongOrNull(16)?.toInt() ?: -16777216
                }
                
                val categoryEntity = CategoryEntity(
                    name = category.name,
                    iconName = category.iconName,
                    colorHex = colorHex,
                    isDefault = category.isDefault,
                    parentId = parentId
                )
                categoryDao.insertCategory(categoryEntity)
                android.util.Log.d("DatabaseSeeder", "Inserted child category: ${category.name} with parent ${category.parentName}")
            } else {
                android.util.Log.w("DatabaseSeeder", "Parent category '${category.parentName}' not found for child category '${category.name}'")
            }
        }
        
        // Query all categories again to get the final IDs including children
        val finalCategories = categoryDao.getAllCategoriesSync()
        finalCategories.forEach { category ->
            categoryNameToId[category.name] = category.id
        }
        
        android.util.Log.d("DatabaseSeeder", "Inserted ${categoryNameToId.size} categories total")

        // Insert accounts
        accountDao.upsertAccounts(accounts)
        android.util.Log.d("DatabaseSeeder", "Inserted ${accounts.size} accounts")

        // Insert transactions with correct category IDs
        var successfulTransactions = 0
        var failedTransactions = 0

        transactions.forEach { transaction ->
            try {
                val categoryId = categoryNameToId[transaction.category]
                if (categoryId == null) {
                    android.util.Log.w("DatabaseSeeder", "Category '${transaction.category}' not found for transaction '${transaction.name}'")
                    failedTransactions++
                    return@forEach
                }

                val transactionWithCategoryId = transaction.copy(categoryId = categoryId)
                transactionDao.insertTransaction(transactionWithCategoryId.toTransactionEntity())

                successfulTransactions++
            } catch (e: Exception) {
                android.util.Log.e("DatabaseSeeder", "Failed to insert transaction '${transaction.name}': ${e.message}")
                failedTransactions++
            }
        }

        android.util.Log.d("DatabaseSeeder", "Inserted $successfulTransactions transactions successfully, $failedTransactions failed")

        // Seed preset filters
        seedPresetFilters(categoryNameToId)
        android.util.Log.d("DatabaseSeeder", "Seeded preset filters")

        // Mark as seeded
        settingsDataStore.saveDatabaseSeeded(true)
        android.util.Log.d("DatabaseSeeder", "Database seeding completed successfully")
    }

    private suspend fun seedPresetFilters(categoryNameToId: Map<String, Long>) {
        val presetFilters = listOf(
            FilterEntity(
                name = "Food & Dining Expense",
                categoryIds = categoryNameToId["Food & Dining"]?.toString() ?: "1",
                transactionType = "expense"
            ),
            FilterEntity(
                name = "Shopping Expense",
                categoryIds = categoryNameToId["Shopping"]?.toString() ?: "3",
                transactionType = "expense"
            ),
            FilterEntity(
                name = "Salary Income",
                categoryIds = categoryNameToId["Income"]?.toString() ?: "8",
                transactionType = "income"
            ),
            FilterEntity(
                name = "Transportation Expense",
                categoryIds = categoryNameToId["Transportation"]?.toString() ?: "2",
                transactionType = "expense"
            ),
            FilterEntity(
                name = "Entertainment Expense",
                categoryIds = categoryNameToId["Entertainment"]?.toString() ?: "5",
                transactionType = "expense"
            )
        )

        presetFilters.forEach { filter ->
            try {
                filterDao.insertFilter(filter)
                android.util.Log.d("DatabaseSeeder", "Inserted preset filter: ${filter.name}")
            } catch (e: Exception) {
                android.util.Log.e("DatabaseSeeder", "Failed to insert preset filter '${filter.name}': ${e.message}")
            }
        }
    }

    private fun loadAccountsFromCsv(): List<AccountEntity> {
        val accounts = mutableListOf<AccountEntity>()
        try {
            val inputStream = context.assets.open("accounts.csv")
            val reader = BufferedReader(InputStreamReader(inputStream))

            // Skip header
            reader.readLine()

            reader.useLines { lines ->
                lines.forEach { line ->
                    if (line.isNotBlank()) {
                        try {
                            accounts.add(AccountSeedRow.fromCsv(line).toAccountEntity())
                        } catch (e: Exception) {
                            android.util.Log.e("DatabaseSeeder", "Failed to parse account line: $line, error: ${e.message}")
                        }
                    }
                }
            }
        } catch (e: Exception) {
            android.util.Log.e("DatabaseSeeder", "Failed to load accounts.csv: ${e.message}")
        }
        return accounts
    }

    private fun loadTransactionsFromCsv(): List<TransactionSeedRow> {
        val transactions = mutableListOf<TransactionSeedRow>()
        try {
            val inputStream = context.assets.open("transactions.csv")
            val reader = BufferedReader(InputStreamReader(inputStream))

            // Skip header
            reader.readLine()

            reader.useLines { lines ->
                lines.forEach { line ->
                    if (line.isNotBlank()) {
                        try {
                            transactions.add(TransactionSeedRow.fromCsv(line))
                        } catch (e: Exception) {
                            android.util.Log.e("DatabaseSeeder", "Failed to parse transaction line: $line, error: ${e.message}")
                        }
                    }
                }
            }
        } catch (e: Exception) {
            android.util.Log.e("DatabaseSeeder", "Failed to load transactions.csv: ${e.message}")
        }
        return transactions
    }

    private data class AccountSeedRow(
        val id: String,
        val name: String,
        val currencyCode: String,
        val structureType: String,
        val colorHex: String,
        val icon: String,
        val allowedCurrencies: String,
        val profileId: Int,
        val isArchived: Boolean = false,
        val displayOrder: Int = 0
    ) {
        fun toAccountEntity(): AccountEntity {
            return AccountEntity(
                id = id,
                name = name,
                currencyCode = currencyCode,
                structureType = structureType,
                colorHex = colorHex,
                icon = icon,
                allowedCurrencies = allowedCurrencies.removeSurrounding("\""),
                profileId = profileId,
                isArchived = isArchived,
                displayOrder = displayOrder
            )
        }

        companion object {
            fun fromCsv(line: String): AccountSeedRow {
                val parts = parseCsvLine(line)
                return AccountSeedRow(
                    id = parts.getOrNull(0) ?: "",
                    name = parts.getOrNull(1) ?: "",
                    currencyCode = parts.getOrNull(2) ?: "USD",
                    structureType = parts.getOrNull(3) ?: "REGULAR",
                    colorHex = parts.getOrNull(4) ?: "#000000",
                    icon = parts.getOrNull(5) ?: "",
                    allowedCurrencies = parts.getOrNull(6) ?: "USD",
                    profileId = parts.getOrNull(7)?.toIntOrNull() ?: 1,
                    isArchived = parts.getOrNull(8)?.toBoolean() ?: false,
                    displayOrder = parts.getOrNull(9)?.toIntOrNull() ?: 0
                )
            }
        }
    }

    private fun loadCategoriesFromCsv(): List<CategorySeedRow> {
        val categories = mutableListOf<CategorySeedRow>()
        try {
            val inputStream = context.assets.open("categories.csv")
            val reader = BufferedReader(InputStreamReader(inputStream))

            // Skip header
            reader.readLine()

            reader.useLines { lines ->
                lines.forEach { line ->
                    if (line.isNotBlank()) {
                        try {
                            categories.add(CategorySeedRow.fromCsv(line))
                        } catch (e: Exception) {
                            android.util.Log.e("DatabaseSeeder", "Failed to parse category line: $line, error: ${e.message}")
                        }
                    }
                }
            }
        } catch (e: Exception) {
            android.util.Log.e("DatabaseSeeder", "Failed to load categories.csv: ${e.message}")
        }
        return categories
    }

    private data class CategorySeedRow(
        val name: String,
        val iconName: String,
        val colorHex: String,
        val isDefault: Boolean,
        val parentName: String?
    ) {
        fun toCategoryEntity(): CategoryEntity {
            return CategoryEntity(
                name = name,
                iconName = iconName,
                colorHex = colorHex.removePrefix("#").toLongOrNull(16)?.toInt() ?: -16777216,
                isDefault = isDefault,
                parentId = null // Will be set when inserting child categories
            )
        }

        companion object {
            fun fromCsv(line: String): CategorySeedRow {
                val parts = parseCsvLine(line)
                return CategorySeedRow(
                    name = parts.getOrNull(0) ?: "",
                    iconName = parts.getOrNull(1) ?: "Category",
                    colorHex = parts.getOrNull(2) ?: "#000000",
                    isDefault = parts.getOrNull(3)?.toBoolean() ?: false,
                    parentName = parts.getOrNull(4)?.takeIf { it.isNotBlank() }
                )
            }
        }
    }

    private data class TransactionSeedRow(
        val name: String,
        val amount: BigDecimal,
        val category: String,
        val categoryId: Long?,
        val timestamp: Long?,
        val currency: String,
        val transactionType: String,
        val accountId: String,
        val paymentMethod: String,
        val payee: String,
        val note: String,
        val tags: String,
        val sourcePlannedPaymentId: Long? = null,
        val profileId: Int,
        val latitude: Double? = null,
        val longitude: Double? = null,
        val warrantyExpiration: Long? = null,
        val status: String = "cleared"
    ) {
        fun toTransactionEntity(): TransactionEntity {
            return TransactionEntity(
                name = name,
                amount = amount,
                timestamp = timestamp ?: System.currentTimeMillis(),
                currency = currency,
                transactionType = transactionType,
                accountId = accountId,
                categoryId = categoryId ?: 1002L,
                paymentMethod = paymentMethod,
                payee = payee,
                note = note,
                tags = tags,
                sourcePlannedPaymentId = sourcePlannedPaymentId,
                profileId = profileId,
                latitude = latitude,
                longitude = longitude,
                warrantyExpiration = warrantyExpiration,
                status = status
            )
        }

        companion object {
            fun fromCsv(line: String): TransactionSeedRow {
                val parts = parseCsvLine(line)
                return TransactionSeedRow(
                    name = parts.getOrNull(0) ?: "",
                    amount = parts.getOrNull(1)?.toBigDecimalOrNull() ?: BigDecimal.ZERO,
                    category = parts.getOrNull(2) ?: "",
                    categoryId = parts.getOrNull(3)?.toLongOrNull(),
                    timestamp = parts.getOrNull(4)?.toLongOrNull(),
                    currency = parts.getOrNull(5) ?: "USD",
                    transactionType = parts.getOrNull(6) ?: "expense",
                    accountId = parts.getOrNull(7) ?: "",
                    paymentMethod = parts.getOrNull(8) ?: "Cash",
                    payee = parts.getOrNull(9) ?: "",
                    note = parts.getOrNull(10) ?: "",
                    tags = parts.getOrNull(11) ?: "",
                    sourcePlannedPaymentId = parts.getOrNull(12)?.toLongOrNull(),
                    profileId = parts.getOrNull(13)?.toIntOrNull() ?: 1,
                    latitude = parts.getOrNull(14)?.toDoubleOrNull(),
                    longitude = parts.getOrNull(15)?.toDoubleOrNull(),
                    warrantyExpiration = parts.getOrNull(16)?.toLongOrNull(),
                    status = parts.getOrNull(17) ?: "cleared"
                )
            }
        }
    }
}
