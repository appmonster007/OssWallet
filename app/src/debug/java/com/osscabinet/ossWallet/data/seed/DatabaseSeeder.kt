package com.osscabinet.ossWallet.data.seed

import android.content.Context
import com.osscabinet.ossWallet.data.local.SettingsDataStore
import com.osscabinet.ossWallet.data.local.dao.AccountDao
import com.osscabinet.ossWallet.data.local.dao.BudgetDao
import com.osscabinet.ossWallet.data.local.dao.CategoryDao
import com.osscabinet.ossWallet.data.local.dao.FilterDao
import com.osscabinet.ossWallet.data.local.dao.LoanDao
import com.osscabinet.ossWallet.data.local.dao.PlannedPaymentDao
import com.osscabinet.ossWallet.data.local.dao.ProfileDao
import com.osscabinet.ossWallet.data.local.dao.TagDao
import com.osscabinet.ossWallet.data.local.dao.TemplateDao
import com.osscabinet.ossWallet.data.local.dao.TransactionDao
import com.osscabinet.ossWallet.data.local.dao.TransactionLinkDao
import com.osscabinet.ossWallet.data.local.entity.AccountEntity
import com.osscabinet.ossWallet.data.local.entity.BudgetEntity
import com.osscabinet.ossWallet.data.local.entity.FilterEntity
import com.osscabinet.ossWallet.data.local.entity.LoanEntity
import com.osscabinet.ossWallet.data.local.entity.PlannedPaymentEntity
import com.osscabinet.ossWallet.data.local.entity.ProfileEntity
import com.osscabinet.ossWallet.data.local.entity.RecurrenceScheduleEntity
import com.osscabinet.ossWallet.data.local.entity.TagEntity
import com.osscabinet.ossWallet.data.local.entity.TemplateEntity
import com.osscabinet.ossWallet.data.local.entity.TransactionEntity
import com.osscabinet.ossWallet.data.local.entity.TransactionLinkEntity
import com.osscabinet.ossWallet.domain.model.Category
import com.osscabinet.ossWallet.domain.model.PaymentMethod
import com.osscabinet.ossWallet.domain.model.TransactionStatus
import com.osscabinet.ossWallet.domain.model.TransactionType
import com.osscabinet.ossWallet.domain.model.toEntryMode
import jakarta.inject.Inject
import kotlinx.coroutines.flow.first
import java.io.BufferedReader
import java.io.InputStreamReader
import java.math.BigDecimal

private fun BigDecimal.enforceSign(type: TransactionType): BigDecimal = when (type) {
    TransactionType.EXPENSE, TransactionType.TRANSFER_OUTFLOW -> abs().negate()
    TransactionType.INCOME, TransactionType.TRANSFER_INFLOW -> abs()
}

private fun parseCsvLine(line: String): List<String> {
    val result = mutableListOf<String>()
    var current = StringBuilder()
    var inQuotes = false
    for (char in line) {
        when (char) {
            '"' -> inQuotes = !inQuotes
            ',' -> if (inQuotes) current.append(char) else {
                result.add(current.toString()); current = StringBuilder()
            }

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
    private val tagDao: TagDao,
    private val context: Context,
    private val settingsDataStore: SettingsDataStore,
    private val database: com.osscabinet.ossWallet.data.local.AppDatabase,
) {
    suspend fun seed() {
        if (settingsDataStore.isDatabaseSeeded.first()) {
            android.util.Log.d("DatabaseSeeder", "Database already seeded, skipping")
            return
        }

        android.util.Log.d("DatabaseSeeder", "Starting database seeding...")

        // 0. Ensure Base Infrastructure (Categories/Tags) exists before seeding
        // This fixes FOREIGN KEY constraint errors after a "Delete Everything" wipe
        ensureInfrastructureExists()

        // Clear existing user data (but keep the categories we just ensured)
        accountDao.clearAll()
        transactionDao.clearAll()
        transactionLinkDao.clearAll()
        filterDao.clearAll()
        profileDao.clearAll()
        budgetDao.clearAll()
        loanDao.clearAll()
        templateDao.clearAll()
        plannedPaymentDao.clearAll()
        tagDao.clearAll()

        // 1. Profiles
        val profiles = loadProfilesFromCsv()
        profiles.forEach { profileDao.insertProfile(it) }

        // 2. Categories (resolved from DB populated by default_data.sql)
        val categoryNameToId =
            categoryDao.getAllCategoriesSync().associateBy({ it.name }, { it.id })
        android.util.Log.d("DatabaseSeeder", "Resolved ${categoryNameToId.size} categories from DB")

        // 3. Accounts
        val accounts = loadAccountsFromCsv()
        if (accounts.isEmpty()) {
            android.util.Log.e("DatabaseSeeder", "No accounts in CSV, aborting seeding")
            return
        }
        accountDao.upsertAccounts(accounts)

        // 4. Filters
        val filterSeedRows = loadFiltersFromCsv()
        val filterSeedIdToDbId = mutableMapOf<Long, Long>()
        filterSeedRows.forEach { row ->
            val dbId = filterDao.insertFilter(row.toEntity(categoryNameToId))
            filterSeedIdToDbId[row.id] = dbId
        }

        // 5. Tags & Transactions
        val csvTransactions = loadTransactionsFromCsv()
        val csvTags = loadTagsFromCsv()
        seedTags(csvTags, csvTransactions)

        val transactionSeedIdToDbId = mutableMapOf<Long, Long>()
        var ok = 0
        var fail = 0
        csvTransactions.forEach { tx ->
            try {
                val catId =
                    categoryNameToId[tx.category] ?: tx.categoryId ?: Category.SYSTEM_UNKNOWN
                val entity = tx.copy(categoryId = catId).toTransactionEntity()
                val dbId = transactionDao._insertTransaction(entity)
                tx.seedId?.let { transactionSeedIdToDbId[it] = dbId }
                ok++
            } catch (e: Exception) {
                android.util.Log.e(
                    "DatabaseSeeder",
                    "Failed to insert transaction ${tx.title}: ${e.message}"
                )
                fail++
            }
        }
        android.util.Log.d("DatabaseSeeder", "Inserted $ok transactions, $fail failed")

        // 6. Transaction Links (Transfers etc)
        val links = loadTransactionLinksFromCsv()
        val linkEntities = links.mapNotNull { link ->
            val txId = transactionSeedIdToDbId[link.first]
            val linkedId = transactionSeedIdToDbId[link.second]
            if (txId != null && linkedId != null) {
                TransactionLinkEntity(transactionId = txId, linkedTransactionId = linkedId)
            } else null
        }
        transactionLinkDao.insertLinks(linkEntities)

        // 7. Templates
        val templateSeedRows = loadTemplatesFromCsv()
        val templateNameToId = mutableMapOf<String, Long>()
        templateSeedRows.forEach { row ->
            val dbId = templateDao.insertTemplate(row.toEntity(categoryNameToId))
            templateNameToId[row.name] = dbId
        }

        // 8. Planned Payments
        val plannedPaymentRows = loadPlannedPaymentsFromCsv()
        plannedPaymentRows.forEach { row ->
            val templateId = templateNameToId[row.templateName]
            if (templateId != null) {
                plannedPaymentDao.insert(row.toEntity(templateId))
            }
        }

        // 9. Budgets
        val budgetRows = loadBudgetsFromCsv()
        budgetRows.forEach { row ->
            val filterId = filterSeedIdToDbId[row.filterSeedId]
            if (filterId != null) {
                budgetDao.insert(row.toEntity(filterId))
            }
        }

        // 10. Loans
        val loanRows = loadLoansFromCsv()
        loanRows.forEach { row ->
            val filterId = filterSeedIdToDbId[row.filterSeedId]
            if (filterId != null) {
                loanDao.insert(row.toEntity(filterId))
            }
        }

        settingsDataStore.saveDatabaseSeeded(seeded = true)
        android.util.Log.d("DatabaseSeeder", "Database seeding completed successfully")
    }

    private suspend fun seedTags(
        csvTags: List<TagSeedRow>,
        transactions: List<TransactionSeedRow>
    ) {
        for (tagRow in csvTags) {
            tagDao.insertTag(tagRow.toTagEntity())
        }
        val existingTagNames = csvTags.mapTo(mutableSetOf()) { it.name }
        val allTxTags =
            transactions.flatMap { it.tags.split(",").filter { t -> t.isNotBlank() } }.toSet()
        val missingTags = allTxTags - existingTagNames
        for (tagName in missingTags) {
            tagDao.insertTag(TagEntity(name = tagName, colorHex = -7829368))
        }
    }

    private suspend fun ensureInfrastructureExists() {
        val categoryCount = categoryDao.getAllCategoriesSync().size
        if (categoryCount > 20) {
            android.util.Log.d(
                "DatabaseSeeder",
                "Infrastructure exists ($categoryCount categories)"
            )
            return
        }

        android.util.Log.d(
            "DatabaseSeeder",
            "Infrastructure missing. Restoring from default_data.sql..."
        )
        try {
            val sdb = database.openHelper.writableDatabase
            sdb.beginTransaction()
            try {
                // 1. Load default categories from SQL asset
                context.assets.open("default_data.sql")
                    .bufferedReader()
                    .useLines { lines ->
                        lines.map { it.trim() }
                            .filter { it.isNotBlank() && !it.startsWith("--") }
                            .forEach { sdb.execSQL(it) }
                    }

                // 2. Ensure System Categories (Reserved IDs)
                com.osscabinet.ossWallet.domain.model.SystemCategory.entries.forEach { sc ->
                    sdb.execSQL(
                        "INSERT OR IGNORE INTO categories (id, name, iconName, colorHex, isDefault, parentId, nature) VALUES (?, ?, ?, ?, 1, ?, ?)",
                        arrayOf<Any?>(
                            sc.id,
                            sc.displayName,
                            sc.iconName,
                            sc.colorHex,
                            sc.parentId,
                            sc.nature.value
                        )
                    )
                }

                // 3. Ensure System Tags
                com.osscabinet.ossWallet.domain.model.SystemTag.entries.forEach { st ->
                    sdb.execSQL(
                        "INSERT OR IGNORE INTO tags (id, name, colorHex) VALUES (?, ?, ?)",
                        arrayOf<Any?>(st.id, st.tagName, st.colorHex)
                    )
                }
                sdb.setTransactionSuccessful()
            } finally {
                sdb.endTransaction()
            }
            android.util.Log.d("DatabaseSeeder", "Infrastructure restoration complete")
        } catch (e: Exception) {
            android.util.Log.e("DatabaseSeeder", "Failed to restore infrastructure", e)
        }
    }

    // ── CSV Loaders ───────────────────────────────────────────────────────────

    private fun loadProfilesFromCsv(): List<ProfileEntity> =
        loadFromCsv("profiles.csv") { ProfileSeedRow.fromCsv(it).toEntity() }

    private fun loadFiltersFromCsv(): List<FilterSeedRow> =
        loadFromCsv("filters.csv") { FilterSeedRow.fromCsv(it) }

    private fun loadBudgetsFromCsv(): List<BudgetSeedRow> =
        loadFromCsv("budgets.csv") { BudgetSeedRow.fromCsv(it) }

    private fun loadLoansFromCsv(): List<LoanSeedRow> =
        loadFromCsv("loans.csv") { LoanSeedRow.fromCsv(it) }

    private fun loadTemplatesFromCsv(): List<TemplateSeedRow> =
        loadFromCsv("templates.csv") { TemplateSeedRow.fromCsv(it) }

    private fun loadPlannedPaymentsFromCsv(): List<PlannedPaymentSeedRow> =
        loadFromCsv("planned_payments.csv") { PlannedPaymentSeedRow.fromCsv(it) }

    private fun loadTransactionLinksFromCsv(): List<Pair<Long, Long>> =
        loadFromCsv("transaction_links.csv") { line ->
            val p = parseCsvLine(line)
            val s1 = p.getOrNull(0)?.toLongOrNull()
            val s2 = p.getOrNull(1)?.toLongOrNull()
            if (s1 != null && s2 != null) s1 to s2 else null
        }

    private fun loadAccountsFromCsv(): List<AccountEntity> =
        loadFromCsv("accounts.csv") { AccountSeedRow.fromCsv(it).toAccountEntity() }

    private fun loadTransactionsFromCsv(): List<TransactionSeedRow> =
        loadFromCsv("transactions.csv") { TransactionSeedRow.fromCsv(it) }

    private fun loadTagsFromCsv(): List<TagSeedRow> =
        loadFromCsv("tags.csv") { TagSeedRow.fromCsv(it) }

    private fun <T> loadFromCsv(filename: String, parser: (String) -> T?): List<T> {
        val list = mutableListOf<T>()
        try {
            val reader = BufferedReader(InputStreamReader(context.assets.open(filename)))
            reader.readLine() // skip header
            reader.useLines { lines ->
                lines.forEach { line ->
                    if (line.isNotBlank()) {
                        try {
                            parser(line)?.let { list.add(it) }
                        } catch (e: Exception) {
                            android.util.Log.e(
                                "DatabaseSeeder",
                                "Error parsing $filename line: ${e.message}"
                            )
                        }
                    }
                }
            }
        } catch (e: Exception) {
            android.util.Log.e("DatabaseSeeder", "Failed to load $filename: ${e.message}")
        }
        return list
    }

    companion object {
        fun parseTagNames(labelsStr: String?): Set<String> {
            if (labelsStr.isNullOrBlank()) return emptySet()
            return labelsStr
                .trim()
                .removePrefix("[")
                .removeSuffix("]")
                .split("|", ";", ",")
                .map { it.trim().removePrefix("\"").removeSuffix("\"").trim() }
                .filter { it.isNotBlank() }
                .toSet()
        }
    }

    // ── Seed row data classes ─────────────────────────────────────────────────

    private data class ProfileSeedRow(
        val id: Int,
        val name: String,
        val baseCurrency: String,
        val type: String
    ) {
        fun toEntity() =
            ProfileEntity(id = id, name = name, baseCurrency = baseCurrency, type = type)

        companion object {
            fun fromCsv(line: String): ProfileSeedRow {
                val p = parseCsvLine(line)
                return ProfileSeedRow(
                    id = p.getOrNull(0)?.toIntOrNull() ?: 1,
                    name = p.getOrNull(1) ?: "",
                    baseCurrency = p.getOrNull(2) ?: "USD",
                    type = p.getOrNull(3) ?: "PERSONAL"
                )
            }
        }
    }

    private data class FilterSeedRow(
        val id: Long, val name: String, val categoryNames: String,
        val transactionTypes: String, val currencies: String, val tags: String,
        val queryPayee: String?
    ) {
        fun toEntity(categoryNameToId: Map<String, Long>) = FilterEntity(
            name = name,
            categoryIds = categoryNames.split(",").filter { it.isNotBlank() }
                .mapNotNull { categoryNameToId[it] }.toSet(),
            transactionTypes = transactionTypes.split(",").filter { it.isNotBlank() }
                .map { TransactionType.fromValue(it) }.toSet(),
            currencies = currencies.split(",").filter { it.isNotBlank() }.toSet(),
            tags = parseTagNames(tags),
            queryPayee = queryPayee
        )

        companion object {
            fun fromCsv(line: String): FilterSeedRow {
                val p = parseCsvLine(line)
                return FilterSeedRow(
                    id = p.getOrNull(0)?.toLongOrNull() ?: 0L,
                    name = p.getOrNull(1) ?: "",
                    categoryNames = p.getOrNull(2) ?: "",
                    transactionTypes = p.getOrNull(3) ?: "",
                    currencies = p.getOrNull(4) ?: "",
                    tags = p.getOrNull(5) ?: "",
                    queryPayee = p.getOrNull(6)?.takeIf { it.isNotBlank() }
                )
            }
        }
    }

    private data class BudgetSeedRow(
        val id: Long,
        val name: String,
        val currency: String,
        val limitAmount: BigDecimal,
        val filterSeedId: Long,
        val profileId: Int
    ) {
        fun toEntity(filterDbId: Long) = BudgetEntity(
            name = name,
            currency = currency,
            limitAmount = limitAmount,
            filterId = filterDbId,
            profileId = profileId
        )

        companion object {
            fun fromCsv(line: String): BudgetSeedRow {
                val p = parseCsvLine(line)
                return BudgetSeedRow(
                    id = p.getOrNull(0)?.toLongOrNull() ?: 0L,
                    name = p.getOrNull(1) ?: "",
                    currency = p.getOrNull(2) ?: "USD",
                    limitAmount = p.getOrNull(3)?.toBigDecimalOrNull() ?: BigDecimal.ZERO,
                    filterSeedId = p.getOrNull(4)?.toLongOrNull() ?: 0L,
                    profileId = p.getOrNull(5)?.toIntOrNull() ?: 1
                )
            }
        }
    }

    private data class LoanSeedRow(
        val id: Long,
        val name: String,
        val principalAmount: BigDecimal,
        val currency: String,
        val interestRate: Float,
        val startDaysAgo: Long,
        val lenderOrBorrowerName: String,
        val loanType: String,
        val linkedAccountId: String,
        val profileId: Int,
        val filterSeedId: Long
    ) {
        fun toEntity(filterDbId: Long) = LoanEntity(
            name = name,
            principalAmount = principalAmount,
            currency = currency,
            interestRate = interestRate,
            startDate = System.currentTimeMillis() - startDaysAgo * 86_400_000L,
            lenderOrBorrowerName = lenderOrBorrowerName,
            loanType = loanType,
            linkedAccountId = linkedAccountId,
            profileId = profileId,
            filterId = filterDbId
        )

        companion object {
            fun fromCsv(line: String): LoanSeedRow {
                val p = parseCsvLine(line)
                return LoanSeedRow(
                    id = p.getOrNull(0)?.toLongOrNull() ?: 0L,
                    name = p.getOrNull(1) ?: "",
                    principalAmount = p.getOrNull(2)?.toBigDecimalOrNull() ?: BigDecimal.ZERO,
                    currency = p.getOrNull(3) ?: "USD",
                    interestRate = p.getOrNull(4)?.toFloatOrNull() ?: 0f,
                    startDaysAgo = p.getOrNull(5)?.toLongOrNull() ?: 0L,
                    lenderOrBorrowerName = p.getOrNull(6) ?: "",
                    loanType = p.getOrNull(7) ?: "BORROWED",
                    linkedAccountId = p.getOrNull(8) ?: "",
                    profileId = p.getOrNull(9)?.toIntOrNull() ?: 1,
                    filterSeedId = p.getOrNull(10)?.toLongOrNull() ?: 0L
                )
            }
        }
    }

    private data class TemplateSeedRow(
        val id: Long,
        val name: String,
        val categoryName: String,
        val amount: BigDecimal,
        val transactionType: TransactionType,
        val currency: String,
        val paymentMethod: PaymentMethod,
        val payee: String,
        val note: String,
        val tags: String,
        val accountId: String?
    ) {
        fun toEntity(categoryNameToId: Map<String, Long>) = TemplateEntity(
            name = name, categoryId = categoryNameToId[categoryName] ?: Category.SYSTEM_UNKNOWN,
            amount = amount, entryMode = transactionType.toEntryMode(), currency = currency,
            paymentMethod = paymentMethod, payee = payee, note = note,
            tags = parseTagNames(tags),
            accountId = accountId.takeIf { !it.isNullOrBlank() }
        )

        companion object {
            fun fromCsv(line: String): TemplateSeedRow {
                val p = parseCsvLine(line)
                return TemplateSeedRow(
                    id = p.getOrNull(0)?.toLongOrNull() ?: 0L,
                    name = p.getOrNull(1) ?: "",
                    categoryName = p.getOrNull(2) ?: "",
                    amount = p.getOrNull(3)?.toBigDecimalOrNull() ?: BigDecimal.ZERO,
                    transactionType = TransactionType.fromValue(p.getOrNull(4) ?: "expense"),
                    currency = p.getOrNull(5) ?: "USD",
                    paymentMethod = PaymentMethod.fromString(p.getOrNull(6) ?: "Cash"),
                    payee = p.getOrNull(7) ?: "",
                    note = p.getOrNull(8) ?: "",
                    tags = p.getOrNull(9) ?: "",
                    accountId = p.getOrNull(10)?.takeIf { it.isNotBlank() }
                )
            }
        }
    }

    private data class PlannedPaymentSeedRow(
        val id: Long, val templateName: String, val recurrenceType: String,
        val dayOfMonth: Int?, val dayOfWeek: Int?, val offsetDays: Long, val autoConfirm: Boolean
    ) {
        fun toEntity(templateId: Long) = PlannedPaymentEntity(
            templateId = templateId,
            schedule = RecurrenceScheduleEntity(
                recurrenceType = recurrenceType,
                dayOfMonth = dayOfMonth,
                dayOfWeek = dayOfWeek,
                startDate = System.currentTimeMillis(),
                nextDueDate = System.currentTimeMillis() + offsetDays * 86_400_000L
            ),
            autoConfirm = autoConfirm
        )

        companion object {
            fun fromCsv(line: String): PlannedPaymentSeedRow {
                val p = parseCsvLine(line)
                return PlannedPaymentSeedRow(
                    id = p.getOrNull(0)?.toLongOrNull() ?: 0L,
                    templateName = p.getOrNull(1) ?: "",
                    recurrenceType = p.getOrNull(2) ?: "MONTHLY",
                    dayOfMonth = p.getOrNull(3)?.toIntOrNull(),
                    dayOfWeek = p.getOrNull(4)?.toIntOrNull(),
                    offsetDays = p.getOrNull(5)?.toLongOrNull() ?: 0L,
                    autoConfirm = p.getOrNull(6)?.toBoolean() ?: false
                )
            }
        }
    }

    private data class TagSeedRow(val name: String, val colorHex: Int) {
        fun toTagEntity() = TagEntity(name = name, colorHex = colorHex)

        companion object {
            fun fromCsv(line: String): TagSeedRow {
                val p = parseCsvLine(line)
                return TagSeedRow(
                    name = p.getOrNull(0) ?: "",
                    colorHex = p.getOrNull(1)?.toIntOrNull() ?: -7829368
                )
            }
        }
    }

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
                    id = p.getOrNull(0) ?: "",
                    name = p.getOrNull(1) ?: "",
                    currencyCode = p.getOrNull(2) ?: "USD",
                    structureType = p.getOrNull(3) ?: "REGULAR",
                    colorHex = p.getOrNull(4) ?: "#000000",
                    icon = p.getOrNull(5) ?: "",
                    allowedCurrencies = p.getOrNull(6) ?: "USD",
                    profileId = p.getOrNull(7)?.toIntOrNull() ?: 1,
                    isArchived = p.getOrNull(8)?.toBoolean() ?: false,
                    displayOrder = p.getOrNull(9)?.toIntOrNull() ?: 0
                )
            }
        }
    }

    private data class TransactionSeedRow(
        val seedId: Long?,
        val title: String,
        val amount: BigDecimal,
        val category: String,
        val categoryId: Long?,
        val timestamp: Long?,
        val currency: String,
        val transactionType: TransactionType,
        val accountId: String,
        val paymentMethod: PaymentMethod,
        val payee: String,
        val note: String,
        val tags: String,
        val sourcePlannedPaymentId: Long? = null,
        val profileId: Int,
        val gpsLocation: String? = null,
        val warrantyExpiration: Long? = null,
        val status: TransactionStatus = TransactionStatus.CLEARED,
        val daysAgo: Long? = null,
        val targetAccountId: String? = null,
        val targetCurrency: String? = null,
        val transferRate: BigDecimal = BigDecimal.ONE
    ) {
        fun toTransactionEntity() = TransactionEntity(
            name = title.ifBlank { category },
            amount = amount.enforceSign(transactionType),
            timestamp = daysAgo?.let { System.currentTimeMillis() - it * 86_400_000L } ?: timestamp
            ?: System.currentTimeMillis(),
            currency = currency, transactionType = transactionType,
            accountId = accountId, categoryId = categoryId ?: Category.SYSTEM_UNKNOWN,
            paymentMethod = paymentMethod, payee = payee, note = note,
            tags = parseTagNames(tags),
            sourcePlannedPaymentId = sourcePlannedPaymentId,
            profileId = profileId, gpsLocation = gpsLocation,
            warrantyExpiration = warrantyExpiration, status = status,
            targetAccountId = targetAccountId, targetCurrency = targetCurrency,
            transferRate = transferRate
        )

        companion object {
            fun fromCsv(line: String): TransactionSeedRow {
                val p = parseCsvLine(line)
                return TransactionSeedRow(
                    seedId = p.getOrNull(0)?.toLongOrNull(),
                    title = p.getOrNull(1) ?: "",
                    amount = p.getOrNull(2)?.toBigDecimalOrNull() ?: BigDecimal.ZERO,
                    category = p.getOrNull(3) ?: "",
                    categoryId = p.getOrNull(4)?.toLongOrNull(),
                    timestamp = p.getOrNull(5)?.toLongOrNull(),
                    currency = p.getOrNull(6) ?: "USD",
                    transactionType = run {
                        val raw = p.getOrNull(7) ?: "expense"
                        if (raw == "transfer") {
                            val amt = p.getOrNull(2)?.toBigDecimalOrNull() ?: BigDecimal.ZERO
                            if (amt >= BigDecimal.ZERO) TransactionType.TRANSFER_INFLOW else TransactionType.TRANSFER_OUTFLOW
                        } else {
                            TransactionType.fromValue(raw)
                        }
                    },
                    accountId = p.getOrNull(8) ?: "",
                    paymentMethod = PaymentMethod.fromString(p.getOrNull(9) ?: "Cash"),
                    payee = p.getOrNull(10) ?: "",
                    note = p.getOrNull(11) ?: "",
                    tags = p.getOrNull(12) ?: "",
                    sourcePlannedPaymentId = p.getOrNull(13)?.toLongOrNull(),
                    profileId = p.getOrNull(14)?.toIntOrNull() ?: 1,
                    gpsLocation = run {
                        val lat = p.getOrNull(15)?.toDoubleOrNull()
                        val lon = p.getOrNull(16)?.toDoubleOrNull()
                        if (lat != null && lon != null) "$lat,$lon" else null
                    },
                    warrantyExpiration = p.getOrNull(17)?.toLongOrNull(),
                    status = TransactionStatus.fromString(p.getOrNull(18) ?: "cleared"),
                    daysAgo = p.getOrNull(19)?.toLongOrNull(),
                    targetAccountId = p.getOrNull(20)?.takeIf { it.isNotBlank() },
                    targetCurrency = p.getOrNull(21)?.takeIf { it.isNotBlank() },
                    transferRate = p.getOrNull(22)?.toBigDecimalOrNull() ?: BigDecimal.ONE
                )
            }
        }
    }
}
