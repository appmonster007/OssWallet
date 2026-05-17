package com.osscabinet.ossWallet.data.seed

import android.content.Context
import com.osscabinet.ossWallet.data.local.SettingsDataStore
import com.osscabinet.ossWallet.data.local.dao.*
import com.osscabinet.ossWallet.data.local.entity.*
import com.osscabinet.ossWallet.domain.model.PaymentMethod
import com.osscabinet.ossWallet.domain.model.TransactionStatus
import com.osscabinet.ossWallet.domain.model.TransactionType
import kotlinx.coroutines.flow.first
import org.json.JSONArray
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.math.BigDecimal
import javax.inject.Inject

private fun BigDecimal.enforceSign(type: TransactionType): BigDecimal = when (type) {
    TransactionType.EXPENSE -> abs().negate()
    TransactionType.INCOME -> abs()
    else -> this
}

private fun parseCsvLine(line: String): List<String> {
    val result = mutableListOf<String>()
    var current = StringBuilder()
    var inQuotes = false
    for (char in line) {
        when (char) {
            '"' -> inQuotes = !inQuotes
            ',' -> if (inQuotes) current.append(char) else { result.add(current.toString()); current = StringBuilder() }
            else -> current.append(char)
        }
    }
    result.add(current.toString())
    return result
}

class DatabaseSeeder @Inject constructor(
    private val accountDao: AccountDao,
    private val categoryDao: CategoryDao,
    private val transactionDao: TransactionDao,
    private val transactionLinkDao: TransactionLinkDao,
    private val filterDao: FilterDao,
    private val profileDao: ProfileDao,
    private val budgetDao: BudgetDao,
    private val loanDao: LoanDao,
    private val templateDao: TemplateDao,
    private val plannedPaymentDao: PlannedPaymentDao,
    private val context: Context,
    private val settingsDataStore: SettingsDataStore
) {
    suspend fun seed() {
        if (settingsDataStore.isDatabaseSeeded.first()) {
            android.util.Log.d("DatabaseSeeder", "Database already seeded, skipping")
            return
        }

        android.util.Log.d("DatabaseSeeder", "Starting database seeding...")

        // Categories are owned by default_data.sql (AppDatabase.onCreate) — never clear them.
        accountDao.clearAll()
        transactionDao.clearAll()   // cascade-deletes transaction_links rows
        transactionLinkDao.clearAll()
        filterDao.clearAll()
        profileDao.clearAll()
        budgetDao.clearAll()
        loanDao.clearAll()
        templateDao.clearAll()
        plannedPaymentDao.clearAll()

        profileDao.insertProfile(loadDefaultProfile())

        // Resolve category IDs from DB (already populated by default_data.sql).
        val categoryNameToId = categoryDao.getAllCategoriesSync().associate { it.name to it.id }
        android.util.Log.d("DatabaseSeeder", "Resolved ${categoryNameToId.size} categories from DB")

        val accounts = loadAccountsFromCsv()
        val csvTransactions = loadTransactionsFromCsv()

        if (accounts.isEmpty()) {
            android.util.Log.e("DatabaseSeeder", "No accounts in CSV, aborting seeding")
            return
        }

        accountDao.upsertAccounts(accounts)

        var ok = 0; var fail = 0
        csvTransactions.forEach { tx ->
            try {
                val catId = categoryNameToId[tx.category] ?: run { fail++; return@forEach }
                transactionDao.insertTransaction(tx.copy(categoryId = catId).toTransactionEntity())
                ok++
            } catch (e: Exception) {
                android.util.Log.e("DatabaseSeeder", "Failed to insert transaction: ${e.message}")
                fail++
            }
        }
        android.util.Log.d("DatabaseSeeder", "Inserted $ok transactions, $fail failed")

        seedPresetFilters(categoryNameToId)
        val templateIds = seedTemplates(categoryNameToId)
        seedPlannedPayments(templateIds)
        seedBudgets(categoryNameToId)
        seedLoans()
        seedScenarioTransactions(categoryNameToId)
        seedTransfers()

        settingsDataStore.saveDatabaseSeeded(seeded = true)
        android.util.Log.d("DatabaseSeeder", "Database seeding completed successfully")
    }

    // ── Profile ──────────────────────────────────────────────────────────────

    private fun loadDefaultProfile(): ProfileEntity {
        return try {
            val json = JSONObject(readAsset("seed_profile.json"))
            ProfileEntity(
                id = json.getInt("id"),
                name = json.getString("name"),
                baseCurrency = json.getString("baseCurrency"),
                type = json.getString("type")
            )
        } catch (e: Exception) {
            android.util.Log.e("DatabaseSeeder", "Failed to load seed_profile.json: ${e.message}")
            ProfileEntity(id = 1, name = "Default Profile", baseCurrency = "USD", type = "PERSONAL")
        }
    }

    // ── Preset filters ────────────────────────────────────────────────────────

    private suspend fun seedPresetFilters(categoryNameToId: Map<String, Long>) {
        try {
            val arr = JSONArray(readAsset("seed_filters.json"))
            for (i in 0 until arr.length()) {
                try {
                    filterDao.insertFilter(parseFilter(arr.getJSONObject(i), categoryNameToId))
                } catch (e: Exception) {
                    android.util.Log.e("DatabaseSeeder", "Failed to insert preset filter: ${e.message}")
                }
            }
        } catch (e: Exception) {
            android.util.Log.e("DatabaseSeeder", "Failed to load seed_filters.json: ${e.message}")
        }
    }

    // ── Templates ─────────────────────────────────────────────────────────────

    private suspend fun seedTemplates(categoryNameToId: Map<String, Long>): Map<String, Long> {
        val ids = mutableMapOf<String, Long>()
        try {
            val arr = JSONArray(readAsset("seed_templates.json"))
            for (i in 0 until arr.length()) {
                val obj = arr.getJSONObject(i)
                val name = obj.getString("name")
                val catId = categoryNameToId[obj.getString("categoryName")] ?: continue
                val id = templateDao.insertTemplate(
                    TemplateEntity(
                        name = name,
                        categoryId = catId,
                        amount = BigDecimal(obj.getString("amount")),
                        transactionType = TransactionType.fromValue(obj.getString("transactionType")),
                        currency = obj.getString("currency"),
                        paymentMethod = PaymentMethod.fromString(obj.getString("paymentMethod")),
                        payee = obj.getString("payee"),
                        note = obj.optString("note", ""),
                        tags = obj.optJSONArray("tags")?.toStringSet() ?: emptySet()
                    )
                )
                ids[name] = id
            }
        } catch (e: Exception) {
            android.util.Log.e("DatabaseSeeder", "Failed to load seed_templates.json: ${e.message}")
        }
        return ids
    }

    // ── Planned payments ──────────────────────────────────────────────────────

    private suspend fun seedPlannedPayments(templateIds: Map<String, Long>) {
        try {
            val now = System.currentTimeMillis()
            val arr = JSONArray(readAsset("seed_planned_payments.json"))
            for (i in 0 until arr.length()) {
                val obj = arr.getJSONObject(i)
                val templateId = templateIds[obj.getString("templateName")] ?: continue
                val offsetMs = obj.getLong("offsetDays") * 86_400_000L
                plannedPaymentDao.insert(
                    PlannedPaymentEntity(
                        templateId = templateId,
                        schedule = RecurrenceScheduleEntity(
                            recurrenceType = obj.getString("recurrenceType"),
                            dayOfMonth = obj.optInt("dayOfMonth", 0).takeIf { it != 0 },
                            dayOfWeek = obj.optInt("dayOfWeek", 0).takeIf { it != 0 },
                            startDate = now,
                            nextDueDate = now + offsetMs
                        ),
                        autoConfirm = obj.getBoolean("autoConfirm")
                    )
                )
            }
        } catch (e: Exception) {
            android.util.Log.e("DatabaseSeeder", "Failed to load seed_planned_payments.json: ${e.message}")
        }
    }

    // ── Budgets ───────────────────────────────────────────────────────────────

    private suspend fun seedBudgets(categoryNameToId: Map<String, Long>) {
        try {
            val arr = JSONArray(readAsset("seed_budgets.json"))
            for (i in 0 until arr.length()) {
                val obj = arr.getJSONObject(i)
                val filterId = filterDao.insertFilter(parseFilter(obj.getJSONObject("filter"), categoryNameToId))
                budgetDao.insert(
                    BudgetEntity(
                        name = obj.getString("name"),
                        currency = obj.getString("currency"),
                        limitAmount = BigDecimal(obj.getString("limitAmount")),
                        filterId = filterId,
                        profileId = 1
                    )
                )
            }
        } catch (e: Exception) {
            android.util.Log.e("DatabaseSeeder", "Failed to load seed_budgets.json: ${e.message}")
        }
    }

    // ── Loans ─────────────────────────────────────────────────────────────────

    private suspend fun seedLoans() {
        try {
            val arr = JSONArray(readAsset("seed_loans.json"))
            for (i in 0 until arr.length()) {
                val obj = arr.getJSONObject(i)
                val filterId = filterDao.insertFilter(parseFilter(obj.getJSONObject("filter"), emptyMap()))
                loanDao.insert(
                    LoanEntity(
                        name = obj.getString("name"),
                        principalAmount = BigDecimal(obj.getString("principalAmount")),
                        currency = obj.getString("currency"),
                        interestRate = obj.optDouble("interestRate", 0.0).toFloat(),
                        startDate = System.currentTimeMillis() - obj.getLong("startDaysAgo") * 86_400_000L,
                        lenderOrBorrowerName = obj.getString("lenderOrBorrowerName"),
                        loanType = obj.getString("loanType"),
                        linkedAccountId = obj.getString("linkedAccountId"),
                        profileId = 1,
                        filterId = filterId
                    )
                )
            }
        } catch (e: Exception) {
            android.util.Log.e("DatabaseSeeder", "Failed to load seed_loans.json: ${e.message}")
        }
    }

    // ── Scenario transactions ─────────────────────────────────────────────────

    private suspend fun seedScenarioTransactions(categoryNameToId: Map<String, Long>) {
        try {
            val now = System.currentTimeMillis()
            val arr = JSONArray(readAsset("seed_scenario_transactions.json"))
            for (i in 0 until arr.length()) {
                val obj = arr.getJSONObject(i)
                val catId = categoryNameToId[obj.getString("categoryName")] ?: continue
                val txType = TransactionType.fromValue(obj.getString("transactionType"))
                transactionDao.insertTransaction(
                    TransactionEntity(
                        name = obj.getString("name"),
                        amount = BigDecimal(obj.getString("amount")).enforceSign(txType),
                        timestamp = now - obj.getLong("daysAgo") * 86_400_000L,
                        currency = obj.getString("currency"),
                        transactionType = txType,
                        accountId = obj.getString("accountId"),
                        categoryId = catId,
                        paymentMethod = PaymentMethod.fromString(obj.getString("paymentMethod")),
                        payee = obj.optString("payee", ""),
                        note = obj.optString("note", ""),
                        tags = obj.optJSONArray("tags")?.toStringSet() ?: emptySet(),
                        profileId = 1
                    )
                )
            }
        } catch (e: Exception) {
            android.util.Log.e("DatabaseSeeder", "Failed to load seed_scenario_transactions.json: ${e.message}")
        }
    }

    // ── Transfers ─────────────────────────────────────────────────────────────

    private suspend fun seedTransfers() {
        try {
            val now = System.currentTimeMillis()
            val arr = JSONArray(readAsset("seed_transfers.json"))
            for (i in 0 until arr.length()) {
                val obj = arr.getJSONObject(i)
                val name = obj.getString("name")
                val timestamp = now - obj.getLong("daysAgo") * 86_400_000L
                val note = obj.optString("note", "")
                val tags = obj.optJSONArray("tags")?.toStringSet() ?: emptySet()
                val status = TransactionStatus.fromString(obj.optString("status", "cleared"))

                val src = obj.getJSONObject("source")
                val tgt = obj.getJSONObject("target")

                // Source leg: money leaves → negative amount
                val sourceId = transactionDao._insertTransaction(
                    TransactionEntity(
                        name = name,
                        amount = BigDecimal(src.getString("amount")).negate(),
                        timestamp = timestamp,
                        currency = src.getString("currency"),
                        transactionType = TransactionType.TRANSFER,
                        accountId = src.getString("accountId"),
                        categoryId = 1003L,
                        paymentMethod = PaymentMethod.fromString(src.getString("paymentMethod")),
                        note = note,
                        tags = tags,
                        status = status,
                        profileId = 1
                    )
                )

                // Target leg: money arrives → positive amount
                val targetId = transactionDao._insertTransaction(
                    TransactionEntity(
                        name = name,
                        amount = BigDecimal(tgt.getString("amount")),
                        timestamp = timestamp,
                        currency = tgt.getString("currency"),
                        transactionType = TransactionType.TRANSFER,
                        accountId = tgt.getString("accountId"),
                        categoryId = 1003L,
                        paymentMethod = PaymentMethod.fromString(tgt.getString("paymentMethod")),
                        note = note,
                        tags = tags,
                        status = status,
                        profileId = 1
                    )
                )

                // Bi-directional link mirrors what TransactionRepository.saveTransfer does
                transactionLinkDao.insertLinks(
                    listOf(
                        TransactionLinkEntity(transactionId = sourceId, linkedTransactionId = targetId),
                        TransactionLinkEntity(transactionId = targetId, linkedTransactionId = sourceId)
                    )
                )
            }
        } catch (e: Exception) {
            android.util.Log.e("DatabaseSeeder", "Failed to load seed_transfers.json: ${e.message}")
        }
    }

    // ── CSV loaders ───────────────────────────────────────────────────────────

    private fun loadAccountsFromCsv(): List<AccountEntity> {
        val list = mutableListOf<AccountEntity>()
        try {
            val reader = BufferedReader(InputStreamReader(context.assets.open("accounts.csv")))
            reader.readLine()
            reader.useLines { lines ->
                lines.forEach { line ->
                    if (line.isNotBlank()) {
                        try { list.add(AccountSeedRow.fromCsv(line).toAccountEntity()) }
                        catch (e: Exception) { android.util.Log.e("DatabaseSeeder", "Bad account row: ${e.message}") }
                    }
                }
            }
        } catch (e: Exception) { android.util.Log.e("DatabaseSeeder", "Failed to load accounts.csv: ${e.message}") }
        return list
    }

    private fun loadTransactionsFromCsv(): List<TransactionSeedRow> {
        val list = mutableListOf<TransactionSeedRow>()
        try {
            val reader = BufferedReader(InputStreamReader(context.assets.open("transactions.csv")))
            reader.readLine()
            reader.useLines { lines ->
                lines.forEach { line ->
                    if (line.isNotBlank()) {
                        try { list.add(TransactionSeedRow.fromCsv(line)) }
                        catch (e: Exception) { android.util.Log.e("DatabaseSeeder", "Bad transaction row: ${e.message}") }
                    }
                }
            }
        } catch (e: Exception) { android.util.Log.e("DatabaseSeeder", "Failed to load transactions.csv: ${e.message}") }
        return list
    }

    // ── Shared filter parser ──────────────────────────────────────────────────

    private fun parseFilter(obj: JSONObject, categoryNameToId: Map<String, Long>): FilterEntity {
        val categoryIds = obj.optJSONArray("categoryNames")
            ?.let { arr -> (0 until arr.length()).mapNotNull { categoryNameToId[arr.getString(it)] }.toSet() }
            ?: emptySet()

        val transactionTypes = obj.optJSONArray("transactionTypes")
            ?.let { arr -> (0 until arr.length()).map { TransactionType.fromValue(arr.getString(it)) }.toSet() }
            ?: emptySet()

        val currencies = obj.optJSONArray("currencies")
            ?.let { arr -> (0 until arr.length()).map { arr.getString(it) }.toSet() }
            ?: emptySet()

        val tags = obj.optJSONArray("tags")
            ?.toStringSet()
            ?: emptySet()

        return FilterEntity(
            name = obj.getString("name"),
            categoryIds = categoryIds,
            transactionTypes = transactionTypes,
            currencies = currencies,
            tags = tags,
            queryPayee = obj.optString("queryPayee").ifBlank { null }
        )
    }

    // ── Utilities ─────────────────────────────────────────────────────────────

    private fun readAsset(filename: String): String =
        context.assets.open(filename).bufferedReader().use { it.readText() }

    private fun JSONArray.toStringSet(): Set<String> =
        (0 until length()).map { getString(it) }.toSet()

    // ── Seed row data classes ─────────────────────────────────────────────────

    private data class AccountSeedRow(
        val id: String, val name: String, val currencyCode: String,
        val structureType: String, val colorHex: String, val icon: String,
        val allowedCurrencies: String, val profileId: Int,
        val isArchived: Boolean = false, val displayOrder: Int = 0
    ) {
        fun toAccountEntity() = AccountEntity(
            id = id, name = name, currencyCode = currencyCode,
            structureType = structureType, colorHex = colorHex, icon = icon,
            allowedCurrencies = allowedCurrencies.removeSurrounding("\""),
            profileId = profileId, isArchived = isArchived, displayOrder = displayOrder
        )

        companion object {
            fun fromCsv(line: String): AccountSeedRow {
                val p = parseCsvLine(line)
                return AccountSeedRow(
                    id = p.getOrNull(0) ?: "", name = p.getOrNull(1) ?: "",
                    currencyCode = p.getOrNull(2) ?: "USD", structureType = p.getOrNull(3) ?: "REGULAR",
                    colorHex = p.getOrNull(4) ?: "#000000", icon = p.getOrNull(5) ?: "",
                    allowedCurrencies = p.getOrNull(6) ?: "USD",
                    profileId = p.getOrNull(7)?.toIntOrNull() ?: 1,
                    isArchived = p.getOrNull(8)?.toBoolean() ?: false,
                    displayOrder = p.getOrNull(9)?.toIntOrNull() ?: 0
                )
            }
        }
    }

    private data class TransactionSeedRow(
        val amount: BigDecimal, val category: String, val categoryId: Long?,
        val timestamp: Long?, val currency: String,
        val transactionType: TransactionType, val accountId: String,
        val paymentMethod: PaymentMethod, val payee: String, val note: String,
        val tags: String, val sourcePlannedPaymentId: Long? = null,
        val profileId: Int, val latitude: Double? = null, val longitude: Double? = null,
        val warrantyExpiration: Long? = null,
        val status: TransactionStatus = TransactionStatus.CLEARED
    ) {
        fun toTransactionEntity() = TransactionEntity(
            name = category, amount = amount.enforceSign(transactionType),
            timestamp = timestamp ?: System.currentTimeMillis(),
            currency = currency, transactionType = transactionType,
            accountId = accountId, categoryId = categoryId ?: 1002L,
            paymentMethod = paymentMethod, payee = payee, note = note,
            tags = tags.split(",").filter { it.isNotBlank() }.toSet(),
            sourcePlannedPaymentId = sourcePlannedPaymentId,
            profileId = profileId, latitude = latitude, longitude = longitude,
            warrantyExpiration = warrantyExpiration, status = status
        )

        companion object {
            fun fromCsv(line: String): TransactionSeedRow {
                val p = parseCsvLine(line)
                return TransactionSeedRow(
                    amount = p.getOrNull(1)?.toBigDecimalOrNull() ?: BigDecimal.ZERO,
                    category = p.getOrNull(2) ?: "", categoryId = p.getOrNull(3)?.toLongOrNull(),
                    timestamp = p.getOrNull(4)?.toLongOrNull(), currency = p.getOrNull(5) ?: "USD",
                    transactionType = TransactionType.fromValue(p.getOrNull(6) ?: "expense"),
                    accountId = p.getOrNull(7) ?: "",
                    paymentMethod = PaymentMethod.fromString(p.getOrNull(8) ?: "Cash"),
                    payee = p.getOrNull(9) ?: "", note = p.getOrNull(10) ?: "",
                    tags = p.getOrNull(11) ?: "", sourcePlannedPaymentId = p.getOrNull(12)?.toLongOrNull(),
                    profileId = p.getOrNull(13)?.toIntOrNull() ?: 1,
                    latitude = p.getOrNull(14)?.toDoubleOrNull(), longitude = p.getOrNull(15)?.toDoubleOrNull(),
                    warrantyExpiration = p.getOrNull(16)?.toLongOrNull(),
                    status = TransactionStatus.fromString(p.getOrNull(17) ?: "cleared")
                )
            }
        }
    }
}
