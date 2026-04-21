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
            return
        }

        // Clear existing data for a fresh start in dev
        accountDao.clearAll()
        transactionDao.clearAll()

        // Load seed data from CSV
        val seedData = loadSeedDataFromCsv()
        val accounts = seedData.filter { it.type == "account" }.map { it.toAccountEntity() }
        val transactions = seedData.filter { it.type == "transaction" }.map { it.toTransactionEntity() }

        // Insert accounts
        accountDao.upsertAccounts(accounts)

        // Ensure categories exist
        val categories = categoryDao.getAllCategoriesSync()
        if (categories.isEmpty()) return // Should be seeded by DB callback

        // Create a map of category names to IDs
        val categoryNameToId = categories.associate { it.name to it.id }

        // Insert transactions with correct category IDs
        transactions.forEach { transaction ->
            val categoryId = categoryNameToId[transaction.category]
            val transactionWithCategoryId = transaction.copy(categoryId = categoryId)
            transactionDao.insertTransaction(transactionWithCategoryId)

            // Update account balance
            val updateAmount = if (transaction.isIncome) transaction.amount else transaction.amount.negate()
            accountDao.updateBalance(transaction.accountId, updateAmount)
        }

        // Mark as seeded
        settingsDataStore.saveDatabaseSeeded(true)
    }

    private fun loadSeedDataFromCsv(): List<SeedDataRow> {
        val rows = mutableListOf<SeedDataRow>()
        try {
            val inputStream = context.assets.open("seed_data.csv")
            val reader = BufferedReader(InputStreamReader(inputStream))
            
            // Skip header
            reader.readLine()
            
            reader.useLines { lines ->
                lines.forEach { line ->
                    if (line.isNotBlank()) {
                        rows.add(SeedDataRow.fromCsv(line))
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return rows
    }

    private data class SeedDataRow(
        val type: String,
        val id: String?,
        val name: String?,
        val balance: BigDecimal?,
        val initialBalance: BigDecimal?,
        val currencyCode: String?,
        val structureType: String?,
        val colorHex: String?,
        val icon: String?,
        val allowedCurrencies: String?,
        val title: String?,
        val amount: BigDecimal?,
        val category: String?,
        val categoryId: Long?,
        val timestamp: Long?,
        val currency: String?,
        val isIncome: Boolean?,
        val accountId: String?,
        val paymentMethod: String?,
        val payee: String?,
        val note: String?,
        val tags: String?,
        val recurring: String?,
        val profileId: Int?
    ) {
        fun toAccountEntity(): AccountEntity {
            return AccountEntity(
                id = id ?: "",
                name = name ?: "",
                balance = balance ?: BigDecimal.ZERO,
                initialBalance = initialBalance ?: BigDecimal.ZERO,
                currencyCode = currencyCode ?: "USD",
                structureType = structureType ?: "REGULAR",
                colorHex = colorHex ?: "#000000",
                icon = icon ?: "",
                allowedCurrencies = allowedCurrencies ?: "USD",
                profileId = profileId ?: 1
            )
        }

        fun toTransactionEntity(): TransactionEntity {
            return TransactionEntity(
                title = title ?: "",
                amount = amount ?: BigDecimal.ZERO,
                category = category ?: "",
                timestamp = timestamp ?: System.currentTimeMillis(),
                currency = currency ?: "USD",
                isIncome = isIncome ?: false,
                accountId = accountId ?: "",
                categoryId = categoryId,
                paymentMethod = paymentMethod ?: "Cash",
                payee = payee ?: "",
                note = note ?: "",
                tags = tags ?: "",
                recurring = recurring,
                profileId = profileId ?: 1
            )
        }

        companion object {
            fun fromCsv(line: String): SeedDataRow {
                val parts = parseCsvLine(line)
                return SeedDataRow(
                    type = parts.getOrNull(0) ?: "",
                    id = parts.getOrNull(1),
                    name = parts.getOrNull(2),
                    balance = parts.getOrNull(3)?.toBigDecimalOrNull(),
                    initialBalance = parts.getOrNull(4)?.toBigDecimalOrNull(),
                    currencyCode = parts.getOrNull(5),
                    structureType = parts.getOrNull(6),
                    colorHex = parts.getOrNull(7),
                    icon = parts.getOrNull(8),
                    allowedCurrencies = parts.getOrNull(9)?.removeSurrounding("\""),
                    title = parts.getOrNull(10),
                    amount = parts.getOrNull(11)?.toBigDecimalOrNull(),
                    category = parts.getOrNull(12),
                    categoryId = parts.getOrNull(13)?.toLongOrNull(),
                    timestamp = parts.getOrNull(14)?.toLongOrNull(),
                    currency = parts.getOrNull(15),
                    isIncome = parts.getOrNull(16)?.toBoolean(),
                    accountId = parts.getOrNull(17),
                    paymentMethod = parts.getOrNull(18),
                    payee = parts.getOrNull(19),
                    note = parts.getOrNull(20),
                    tags = parts.getOrNull(21),
                    recurring = parts.getOrNull(22),
                    profileId = parts.getOrNull(23)?.toIntOrNull()
                )
            }

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
        }
    }
}
