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
import com.univocity.parsers.common.record.Record
import com.univocity.parsers.csv.CsvParser
import com.univocity.parsers.csv.CsvParserSettings
import jakarta.inject.Inject
import kotlinx.coroutines.flow.first
import java.math.BigDecimal

private fun BigDecimal.enforceSign(type: TransactionType): BigDecimal = when (type) {
    TransactionType.EXPENSE, TransactionType.TRANSFER_OUTFLOW -> abs().negate()
    TransactionType.INCOME, TransactionType.TRANSFER_INFLOW -> abs()
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
        ensureInfrastructureExists()

        // Clear existing user data
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

        // 2. Categories
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

        // 6. Transaction Links
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
        if (categoryCount > 20) return

        try {
            val sdb = database.openHelper.writableDatabase
            sdb.beginTransaction()
            try {
                context.assets.open("default_data.sql")
                    .bufferedReader()
                    .useLines { lines ->
                        lines.map { it.trim() }
                            .filter { it.isNotBlank() && !it.startsWith("--") }
                            .forEach { sdb.execSQL(it) }
                    }

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
        } catch (e: Exception) {
            android.util.Log.e("DatabaseSeeder", "Failed to restore infrastructure", e)
        }
    }

    // ── CSV Loaders ───────────────────────────────────────────────────────────

    private fun loadProfilesFromCsv(): List<ProfileEntity> =
        loadFromCsv("profiles.csv") { ProfileSeedRow.fromRecord(it).toEntity() }

    private fun loadFiltersFromCsv(): List<FilterSeedRow> =
        loadFromCsv("filters.csv") { FilterSeedRow.fromRecord(it) }

    private fun loadBudgetsFromCsv(): List<BudgetSeedRow> =
        loadFromCsv("budgets.csv") { BudgetSeedRow.fromRecord(it) }

    private fun loadLoansFromCsv(): List<LoanSeedRow> =
        loadFromCsv("loans.csv") { LoanSeedRow.fromRecord(it) }

    private fun loadTemplatesFromCsv(): List<TemplateSeedRow> =
        loadFromCsv("templates.csv") { TemplateSeedRow.fromRecord(it) }

    private fun loadPlannedPaymentsFromCsv(): List<PlannedPaymentSeedRow> =
        loadFromCsv("planned_payments.csv") { PlannedPaymentSeedRow.fromRecord(it) }

    private fun loadTransactionLinksFromCsv(): List<Pair<Long, Long>> =
        loadFromCsv("transaction_links.csv") { record ->
            val s1 = record.getString("transactionSeedId")?.toLongOrNull()
            val s2 = record.getString("linkedTransactionSeedId")?.toLongOrNull()
            if (s1 != null && s2 != null) s1 to s2 else null
        }

    private fun loadAccountsFromCsv(): List<AccountEntity> =
        loadFromCsv("accounts.csv") { AccountSeedRow.fromRecord(it).toAccountEntity() }

    private fun loadTransactionsFromCsv(): List<TransactionSeedRow> =
        loadFromCsv("transactions.csv") { TransactionSeedRow.fromRecord(it) }

    private fun loadTagsFromCsv(): List<TagSeedRow> =
        loadFromCsv("tags.csv") { TagSeedRow.fromRecord(it) }

    private fun <T> loadFromCsv(filename: String, parser: (Record) -> T?): List<T> {
        val list = mutableListOf<T>()
        try {
            val settings = CsvParserSettings().apply {
                isHeaderExtractionEnabled = true
                skipEmptyLines = true
            }
            val csvParser = CsvParser(settings)
            context.assets.open(filename).use { inputStream ->
                csvParser.iterateRecords(inputStream).forEach { record ->
                    try {
                        parser(record)?.let { list.add(it) }
                    } catch (e: Exception) {
                        android.util.Log.e(
                            "DatabaseSeeder",
                            "Error parsing $filename: ${e.message}"
                        )
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
        val id: Int, val name: String, val baseCurrency: String, val type: String
    ) {
        fun toEntity() =
            ProfileEntity(id = id, name = name, baseCurrency = baseCurrency, type = type)

        companion object {
            fun fromRecord(record: Record) = ProfileSeedRow(
                id = record.getString("id")?.toIntOrNull() ?: 1,
                name = record.getString("name") ?: "",
                baseCurrency = record.getString("baseCurrency") ?: "USD",
                type = record.getString("type") ?: "PERSONAL"
            )
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
            fun fromRecord(record: Record) = FilterSeedRow(
                id = record.getString("id")?.toLongOrNull() ?: 0L,
                name = record.getString("name") ?: "",
                categoryNames = record.getString("categoryNames") ?: "",
                transactionTypes = record.getString("transactionTypes") ?: "",
                currencies = record.getString("currencies") ?: "",
                tags = record.getString("tags") ?: "",
                queryPayee = record.getString("queryPayee")?.takeIf { it.isNotBlank() }
            )
        }
    }

    private data class BudgetSeedRow(
        val id: Long, val name: String, val currency: String,
        val limitAmount: BigDecimal, val filterSeedId: Long, val profileId: Int
    ) {
        fun toEntity(filterDbId: Long) = BudgetEntity(
            name = name, currency = currency, limitAmount = limitAmount,
            filterId = filterDbId, profileId = profileId
        )

        companion object {
            fun fromRecord(record: Record) = BudgetSeedRow(
                id = record.getString("id")?.toLongOrNull() ?: 0L,
                name = record.getString("name") ?: "",
                currency = record.getString("currency") ?: "USD",
                limitAmount = record.getString("limitAmount")?.toBigDecimalOrNull()
                    ?: BigDecimal.ZERO,
                filterSeedId = record.getString("filterId")?.toLongOrNull() ?: 0L,
                profileId = record.getString("profileId")?.toIntOrNull() ?: 1
            )
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
            fun fromRecord(record: Record) = LoanSeedRow(
                id = record.getString("id")?.toLongOrNull() ?: 0L,
                name = record.getString("name") ?: "",
                principalAmount = record.getString("principalAmount")?.toBigDecimalOrNull()
                    ?: BigDecimal.ZERO,
                currency = record.getString("currency") ?: "USD",
                interestRate = record.getString("interestRate")?.toFloatOrNull() ?: 0f,
                startDaysAgo = record.getString("startDaysAgo")?.toLongOrNull() ?: 0L,
                lenderOrBorrowerName = record.getString("lenderOrBorrowerName") ?: "",
                loanType = record.getString("loanType") ?: "BORROWED",
                linkedAccountId = record.getString("linkedAccountId") ?: "",
                profileId = record.getString("profileId")?.toIntOrNull() ?: 1,
                filterSeedId = record.getString("filterId")?.toLongOrNull() ?: 0L
            )
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
            tags = parseTagNames(tags), accountId = accountId.takeIf { !it.isNullOrBlank() }
        )

        companion object {
            fun fromRecord(record: Record) = TemplateSeedRow(
                id = record.getString("id")?.toLongOrNull() ?: 0L,
                name = record.getString("name") ?: "",
                categoryName = record.getString("categoryName") ?: "",
                amount = record.getString("amount")?.toBigDecimalOrNull() ?: BigDecimal.ZERO,
                transactionType = TransactionType.fromValue(
                    record.getString("transactionType") ?: "expense"
                ),
                currency = record.getString("currency") ?: "USD",
                paymentMethod = PaymentMethod.fromString(
                    record.getString("paymentMethod") ?: "Cash"
                ),
                payee = record.getString("payee") ?: "",
                note = record.getString("note") ?: "",
                tags = record.getString("tags") ?: "",
                accountId = record.getString("accountId")?.takeIf { it.isNotBlank() }
            )
        }
    }

    private data class PlannedPaymentSeedRow(
        val id: Long, val templateName: String, val recurrenceType: String,
        val dayOfMonth: Int?, val dayOfWeek: Int?, val offsetDays: Long, val autoConfirm: Boolean
    ) {
        fun toEntity(templateId: Long) = PlannedPaymentEntity(
            templateId = templateId,
            schedule = RecurrenceScheduleEntity(
                recurrenceType = recurrenceType, dayOfMonth = dayOfMonth, dayOfWeek = dayOfWeek,
                startDate = System.currentTimeMillis(),
                nextDueDate = System.currentTimeMillis() + offsetDays * 86_400_000L
            ),
            autoConfirm = autoConfirm
        )

        companion object {
            fun fromRecord(record: Record) = PlannedPaymentSeedRow(
                id = record.getString("id")?.toLongOrNull() ?: 0L,
                templateName = record.getString("templateName") ?: "",
                recurrenceType = record.getString("recurrenceType") ?: "MONTHLY",
                dayOfMonth = record.getString("dayOfMonth")?.toIntOrNull(),
                dayOfWeek = record.getString("dayOfWeek")?.toIntOrNull(),
                offsetDays = record.getString("offsetDays")?.toLongOrNull() ?: 0L,
                autoConfirm = record.getString("autoConfirm")?.toBoolean() ?: false
            )
        }
    }

    private data class TagSeedRow(val name: String, val colorHex: Int) {
        fun toTagEntity() = TagEntity(name = name, colorHex = colorHex)

        companion object {
            fun fromRecord(record: Record) = TagSeedRow(
                name = record.getString("name") ?: "",
                colorHex = record.getString("colorHex")?.toIntOrNull() ?: -7829368
            )
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
            fun fromRecord(record: Record) = AccountSeedRow(
                id = record.getString("id") ?: "",
                name = record.getString("name") ?: "",
                currencyCode = record.getString("currencyCode") ?: "USD",
                structureType = record.getString("structureType") ?: "REGULAR",
                colorHex = record.getString("colorHex") ?: "#000000",
                icon = record.getString("icon") ?: "",
                allowedCurrencies = record.getString("allowedCurrencies") ?: "USD",
                profileId = record.getString("profileId")?.toIntOrNull() ?: 1,
                isArchived = record.getString("isArchived")?.toBoolean() ?: false,
                displayOrder = record.getString("displayOrder")?.toIntOrNull() ?: 0
            )
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
            currency = currency,
            transactionType = transactionType,
            accountId = accountId,
            categoryId = categoryId ?: Category.SYSTEM_UNKNOWN,
            paymentMethod = paymentMethod,
            payee = payee,
            note = note,
            tags = parseTagNames(tags),
            sourcePlannedPaymentId = sourcePlannedPaymentId,
            profileId = profileId,
            gpsLocation = gpsLocation,
            warrantyExpiration = warrantyExpiration,
            status = status,
            targetAccountId = targetAccountId,
            targetCurrency = targetCurrency,
            transferRate = transferRate
        )

        companion object {
            fun fromRecord(record: Record) = TransactionSeedRow(
                seedId = record.getString("seedId")?.toLongOrNull(),
                title = record.getString("title") ?: "",
                amount = record.getString("amount")?.toBigDecimalOrNull() ?: BigDecimal.ZERO,
                category = record.getString("category") ?: "",
                categoryId = record.getString("categoryId")?.toLongOrNull(),
                timestamp = record.getString("timestamp")?.toLongOrNull(),
                currency = record.getString("currency") ?: "USD",
                transactionType = run {
                    val raw = record.getString("transactionType") ?: "expense"
                    if (raw == "transfer") {
                        val amt =
                            record.getString("amount")?.toBigDecimalOrNull() ?: BigDecimal.ZERO
                        if (amt >= BigDecimal.ZERO) TransactionType.TRANSFER_INFLOW else TransactionType.TRANSFER_OUTFLOW
                    } else TransactionType.fromValue(raw)
                },
                accountId = record.getString("accountId") ?: "",
                paymentMethod = PaymentMethod.fromString(
                    record.getString("paymentMethod") ?: "Cash"
                ),
                payee = record.getString("payee") ?: "",
                note = record.getString("note") ?: "",
                tags = record.getString("tags") ?: "",
                sourcePlannedPaymentId = record.getString("recurring")?.toLongOrNull(),
                profileId = record.getString("profileId")?.toIntOrNull() ?: 1,
                gpsLocation = run {
                    val lat = record.getString("latitude")?.toDoubleOrNull()
                    val lon = record.getString("longitude")?.toDoubleOrNull()
                    if (lat != null && lon != null) "$lat,$lon" else null
                },
                warrantyExpiration = record.getString("warrantyExpiration")?.toLongOrNull(),
                status = TransactionStatus.fromString(record.getString("status") ?: "cleared"),
                daysAgo = record.getString("daysAgo")?.toLongOrNull(),
                targetAccountId = record.getString("targetAccountId")?.takeIf { it.isNotBlank() },
                targetCurrency = record.getString("targetCurrency")?.takeIf { it.isNotBlank() },
                transferRate = record.getString("transferRate")?.toBigDecimalOrNull()
                    ?: BigDecimal.ONE
            )
        }
    }
}
