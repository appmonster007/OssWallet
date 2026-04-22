package com.osscabinet.ossWallet.data.seed

import com.osscabinet.ossWallet.data.local.SettingsDataStore
import com.osscabinet.ossWallet.data.local.dao.AccountDao
import com.osscabinet.ossWallet.data.local.dao.CategoryDao
import com.osscabinet.ossWallet.data.local.dao.TransactionDao
import com.osscabinet.ossWallet.data.local.dao.FilterDao
import com.osscabinet.ossWallet.data.local.entity.AccountEntity
import com.osscabinet.ossWallet.data.local.entity.TransactionEntity
import com.osscabinet.ossWallet.data.local.entity.FilterEntity
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
        android.util.Log.d("DatabaseSeeder", "Cleared existing data")

        // Load seed data from separate CSV files
        val accounts = loadAccountsFromCsv()
        android.util.Log.d("DatabaseSeeder", "Loaded ${accounts.size} accounts from CSV")

        val transactions = loadTransactionsFromCsv()
        android.util.Log.d("DatabaseSeeder", "Loaded ${transactions.size} transactions from CSV")

        // Validate that we have data to seed
        if (accounts.isEmpty()) {
            android.util.Log.e("DatabaseSeeder", "No accounts found in CSV, aborting seeding")
            return
        }

        // Insert accounts
        accountDao.upsertAccounts(accounts)
        android.util.Log.d("DatabaseSeeder", "Inserted ${accounts.size} accounts")

        // Ensure categories exist
        val categories = categoryDao.getAllCategoriesSync()
        if (categories.isEmpty()) {
            android.util.Log.e("DatabaseSeeder", "No categories found, aborting seeding")
            return
        }
        android.util.Log.d("DatabaseSeeder", "Found ${categories.size} categories")

        // Create "Initial Balance" category if it doesn't exist
        val categoryNameToId = mutableMapOf<String, Long>()
        categories.forEach { categoryNameToId[it.name] = it.id }

        if (!categoryNameToId.containsKey("Initial Balance")) {
            val initialBalanceCategory = com.osscabinet.ossWallet.data.local.entity.CategoryEntity(
                name = "Initial Balance",
                iconName = "AccountBalance",
                colorHex = -11751600, // Same color as Salary
                isDefault = true,
                parentId = null
            )
            categoryDao.insertCategory(initialBalanceCategory)
            // Query the category to get its ID after insertion
            val allCategories = categoryDao.getAllCategories().first()
            val initialBalanceCategoryEntity = allCategories.find { it.name == "Initial Balance" }
            initialBalanceCategoryEntity?.let {
                categoryNameToId["Initial Balance"] = it.id
                android.util.Log.d("DatabaseSeeder", "Created Initial Balance category with ID ${it.id}")
            }
        }

        // Insert transactions with correct category IDs
        var successfulTransactions = 0
        var failedTransactions = 0

        transactions.forEach { transaction ->
            try {
                val categoryId = categoryNameToId[transaction.category]
                if (categoryId == null) {
                    android.util.Log.w("DatabaseSeeder", "Category '${transaction.category}' not found for transaction '${transaction.title}'")
                    failedTransactions++
                    return@forEach
                }

                val transactionWithCategoryId = transaction.copy(categoryId = categoryId)
                transactionDao.insertTransaction(transactionWithCategoryId)

                // Update account balance
                val updateAmount = if (transaction.transactionType == "income") transaction.amount else transaction.amount.negate()
                accountDao.updateBalance(transaction.accountId, updateAmount)

                successfulTransactions++
            } catch (e: Exception) {
                android.util.Log.e("DatabaseSeeder", "Failed to insert transaction '${transaction.title}': ${e.message}")
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
                name = "Shopping Expense",
                categoryIds = categoryNameToId["Shopping"]?.toString() ?: "3",
                transactionType = "expense"
            ),
            FilterEntity(
                name = "Food & Dining Expense",
                categoryIds = categoryNameToId["Food"]?.toString() ?: "1",
                transactionType = "expense"
            ),
            FilterEntity(
                name = "Salary",
                categoryIds = categoryNameToId["Salary"]?.toString() ?: "1",
                transactionType = "income"
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

    private fun loadTransactionsFromCsv(): List<TransactionEntity> {
        val transactions = mutableListOf<TransactionEntity>()
        try {
            val inputStream = context.assets.open("transactions.csv")
            val reader = BufferedReader(InputStreamReader(inputStream))

            // Skip header
            reader.readLine()

            reader.useLines { lines ->
                lines.forEach { line ->
                    if (line.isNotBlank()) {
                        try {
                            transactions.add(TransactionSeedRow.fromCsv(line).toTransactionEntity())
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
        val profileId: Int
    ) {
        fun toAccountEntity(): AccountEntity {
            return AccountEntity(
                id = id,
                name = name,
                balance = BigDecimal.ZERO,
                currencyCode = currencyCode,
                structureType = structureType,
                colorHex = colorHex,
                icon = icon,
                allowedCurrencies = allowedCurrencies.removeSurrounding("\""),
                profileId = profileId
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
                    profileId = parts.getOrNull(7)?.toIntOrNull() ?: 1
                )
            }
        }
    }

    private data class TransactionSeedRow(
        val title: String,
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
        val recurring: String?,
        val profileId: Int,
        val status: String = "cleared"
    ) {
        fun toTransactionEntity(): TransactionEntity {
            return TransactionEntity(
                title = title,
                amount = amount,
                category = category,
                timestamp = timestamp ?: System.currentTimeMillis(),
                currency = currency,
                transactionType = transactionType,
                accountId = accountId,
                categoryId = categoryId,
                paymentMethod = paymentMethod,
                payee = payee,
                note = note,
                tags = tags,
                recurring = recurring,
                profileId = profileId,
                status = status
            )
        }

        companion object {
            fun fromCsv(line: String): TransactionSeedRow {
                val parts = parseCsvLine(line)
                return TransactionSeedRow(
                    title = parts.getOrNull(0) ?: "",
                    amount = parts.getOrNull(1)?.toBigDecimalOrNull() ?: BigDecimal.ZERO,
                    category = parts.getOrNull(2) ?: "",
                    categoryId = parts.getOrNull(3)?.toLongOrNull(),
                    timestamp = parts.getOrNull(4)?.toLongOrNull(),
                    currency = parts.getOrNull(5) ?: "USD",
                    transactionType = if (parts.getOrNull(6)?.toBoolean() == true) "income" else "expense",
                    accountId = parts.getOrNull(7) ?: "",
                    paymentMethod = parts.getOrNull(8) ?: "Cash",
                    payee = parts.getOrNull(9) ?: "",
                    note = parts.getOrNull(10) ?: "",
                    tags = parts.getOrNull(11) ?: "",
                    recurring = parts.getOrNull(12),
                    profileId = parts.getOrNull(13)?.toIntOrNull() ?: 1,
                    status = parts.getOrNull(14) ?: "cleared"
                )
            }
        }
    }
}
