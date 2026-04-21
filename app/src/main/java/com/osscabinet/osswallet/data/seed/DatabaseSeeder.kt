package com.osscabinet.ossWallet.data.seed

import com.osscabinet.ossWallet.data.local.SettingsDataStore
import com.osscabinet.ossWallet.data.local.dao.AccountDao
import com.osscabinet.ossWallet.data.local.dao.CategoryDao
import com.osscabinet.ossWallet.data.local.dao.TransactionDao
import com.osscabinet.ossWallet.data.local.entity.AccountEntity
import com.osscabinet.ossWallet.data.local.entity.TransactionEntity
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

        // Create a map of category names to IDs
        val categoryNameToId = categories.associate { it.name to it.id }

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
                val updateAmount = if (transaction.isIncome) transaction.amount else transaction.amount.negate()
                accountDao.updateBalance(transaction.accountId, updateAmount)

                successfulTransactions++
            } catch (e: Exception) {
                android.util.Log.e("DatabaseSeeder", "Failed to insert transaction '${transaction.title}': ${e.message}")
                failedTransactions++
            }
        }

        android.util.Log.d("DatabaseSeeder", "Inserted $successfulTransactions transactions successfully, $failedTransactions failed")

        // Mark as seeded
        settingsDataStore.saveDatabaseSeeded(true)
        android.util.Log.d("DatabaseSeeder", "Database seeding completed successfully")
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
        val initialBalance: BigDecimal,
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
                balance = initialBalance,
                initialBalance = initialBalance,
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
                    initialBalance = parts.getOrNull(2)?.toBigDecimalOrNull() ?: BigDecimal.ZERO,
                    currencyCode = parts.getOrNull(3) ?: "USD",
                    structureType = parts.getOrNull(4) ?: "REGULAR",
                    colorHex = parts.getOrNull(5) ?: "#000000",
                    icon = parts.getOrNull(6) ?: "",
                    allowedCurrencies = parts.getOrNull(7) ?: "USD",
                    profileId = parts.getOrNull(8)?.toIntOrNull() ?: 1
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
        val isIncome: Boolean,
        val accountId: String,
        val paymentMethod: String,
        val payee: String,
        val note: String,
        val tags: String,
        val recurring: String?,
        val profileId: Int
    ) {
        fun toTransactionEntity(): TransactionEntity {
            return TransactionEntity(
                title = title,
                amount = amount,
                category = category,
                timestamp = timestamp ?: System.currentTimeMillis(),
                currency = currency,
                isIncome = isIncome,
                accountId = accountId,
                categoryId = categoryId,
                paymentMethod = paymentMethod,
                payee = payee,
                note = note,
                tags = tags,
                recurring = recurring,
                profileId = profileId
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
                    isIncome = parts.getOrNull(6)?.toBoolean() ?: false,
                    accountId = parts.getOrNull(7) ?: "",
                    paymentMethod = parts.getOrNull(8) ?: "Cash",
                    payee = parts.getOrNull(9) ?: "",
                    note = parts.getOrNull(10) ?: "",
                    tags = parts.getOrNull(11) ?: "",
                    recurring = parts.getOrNull(12),
                    profileId = parts.getOrNull(13)?.toIntOrNull() ?: 1
                )
            }
        }
    }
}
