package com.osscabinet.ossWallet.data.service

import android.graphics.Color
import androidx.room.withTransaction
import com.osscabinet.ossWallet.core.util.DispatcherProvider
import com.osscabinet.ossWallet.data.local.AppDatabase
import com.osscabinet.ossWallet.data.local.SettingsDataStore
import com.osscabinet.ossWallet.data.local.dao.*
import com.osscabinet.ossWallet.data.local.entity.*
import com.osscabinet.ossWallet.domain.model.Category
import com.osscabinet.ossWallet.domain.model.PaymentMethod
import com.osscabinet.ossWallet.domain.model.TransactionStatus
import com.osscabinet.ossWallet.domain.model.TransactionType
import com.osscabinet.ossWallet.domain.model.import_csv.*
import com.osscabinet.ossWallet.domain.service.CsvImportService
import com.univocity.parsers.csv.CsvParser
import com.univocity.parsers.csv.CsvParserSettings
import jakarta.inject.Inject
import jakarta.inject.Singleton
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import java.io.InputStream
import java.math.BigDecimal
import java.math.RoundingMode
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeFormatterBuilder
import java.time.temporal.ChronoField
import java.util.UUID

@Singleton
class CsvImportServiceImpl @Inject constructor(
    private val accountDao: AccountDao,
    private val categoryDao: CategoryDao,
    private val transactionDao: TransactionDao,
    private val tagDao: TagDao,
    private val profileDao: ProfileDao,
    private val database: AppDatabase,
    private val settingsDataStore: SettingsDataStore,
    private val assetLoader: BudgetBakersAssetLoader,
    private val transactionLinkDao: TransactionLinkDao,
    private val dispatcherProvider: DispatcherProvider
) : CsvImportService {

    private val budgetBakersHeaders = setOf(
        "account", "category", "currency", "amount", "ref_currency_amount",
        "type", "payment_type", "payment_type_local", "note", "date",
        "gps_latitude", "gps_longitude", "gps_accuracy_in_meters",
        "warranty_in_month", "transfer", "payee", "labels"
    )

    // ──────────────────────────────────────────────────────────────────────────
    // Date parsing (Robust java.time)
    // ──────────────────────────────────────────────────────────────────────────

    private val lenientDateFormatter = DateTimeFormatterBuilder()
        .append(DateTimeFormatter.ofPattern("[yyyy-MM-dd HH:mm:ss.SSS]" +
                                           "[yyyy-MM-dd HH:mm:ss]" +
                                           "[yyyy-MM-dd'T'HH:mm:ss]" +
                                           "[yyyy-MM-dd]" +
                                           "[dd/MM/yyyy HH:mm:ss]" +
                                           "[dd/MM/yyyy]" +
                                           "[MM/dd/yyyy]" +
                                           "[dd.MM.yyyy]"))
        .parseDefaulting(ChronoField.HOUR_OF_DAY, 0)
        .parseDefaulting(ChronoField.MINUTE_OF_HOUR, 0)
        .parseDefaulting(ChronoField.SECOND_OF_MINUTE, 0)
        .toFormatter()

    private fun parseDate(dateStr: String): Long {
        if (dateStr.isBlank()) return 0L
        return try {
            LocalDateTime.parse(dateStr, lenientDateFormatter)
                .atZone(ZoneId.systemDefault())
                .toInstant()
                .toEpochMilli()
        } catch (e: Exception) {
            try {
                java.time.LocalDate.parse(dateStr, lenientDateFormatter)
                    .atStartOfDay(ZoneId.systemDefault())
                    .toInstant()
                    .toEpochMilli()
            } catch (e2: Exception) {
                0L
            }
        }
    }

    // ──────────────────────────────────────────────────────────────────────────
    // CSV format helpers (univocity-parsers)
    // ──────────────────────────────────────────────────────────────────────────

    private fun createParser(delimiter: Char? = null): CsvParser {
        val settings = CsvParserSettings()
        if (delimiter != null) {
            settings.format.setDelimiter(delimiter)
        } else {
            settings.detectFormatAutomatically()
        }
        settings.setHeaderExtractionEnabled(true)
        settings.setSkipEmptyLines(true)
        settings.setIgnoreLeadingWhitespaces(true)
        settings.setIgnoreTrailingWhitespaces(true)
        settings.setEmptyValue("")
        settings.setNullValue("")
        return CsvParser(settings)
    }

    private suspend fun <T> parseRows(
        inputStream: InputStream,
        delimiter: Char? = null,
        block: suspend (List<String>, Sequence<Map<String, String>>) -> T
    ): T = withContext(dispatcherProvider.io) {
        val parser = createParser(delimiter)
        val records = inputStream.use { parser.parseAllRecords(it) }
        val headers = parser.getContext().headers()?.map { it.lowercase() } ?: emptyList()
        val rows = records.asSequence().map { record ->
            headers.zip(record.values).toMap()
        }
        block(headers, rows)
    }

    // ──────────────────────────────────────────────────────────────────────────
    // Amount normalization (ICU4J)
    // ──────────────────────────────────────────────────────────────────────────

    private fun normalizeAmount(s: String?): BigDecimal {
        if (s.isNullOrBlank()) return BigDecimal.ZERO
        val clean = s.filter { it.isDigit() || it == '-' || it == ',' || it == '.' }
        if (clean.isEmpty()) return BigDecimal.ZERO
        return try {
            val lastComma = clean.lastIndexOf(',')
            val lastDot = clean.lastIndexOf('.')
            when {
                lastComma > lastDot -> {
                    val digitsAfterComma = clean.length - lastComma - 1
                    if (lastDot < 0 && digitsAfterComma == 3) {
                        // "1,234" or "1,234,567" — comma is a thousands separator, not decimal
                        clean.replace(",", "").toBigDecimal()
                    } else {
                        // European decimal: "1.234,56" or bare "1,23"
                        clean.replace(".", "").replace(",", ".").toBigDecimal()
                    }
                }
                else ->
                    // US/UK: "1,234.56" — commas are thousands separators
                    clean.replace(",", "").toBigDecimal()
            }
        } catch (e: Exception) {
            BigDecimal.ZERO
        }
    }

    private fun resolveBaseAmount(row: Map<String, String>): BigDecimal {
        val ref = normalizeAmount(row["ref_currency_amount"])
        val raw = normalizeAmount(row["amount"])
        return if (ref.signum() != 0) ref else raw
    }

    // ──────────────────────────────────────────────────────────────────────────
    // Logic detection
    // ──────────────────────────────────────────────────────────────────────────

    private fun isTransferRow(row: Map<String, String>): Boolean =
        row["transfer"]?.equals("true", ignoreCase = true) == true ||
                row["category"]?.equals("TRANSFER", ignoreCase = true) == true

    private fun detectTransactionType(row: Map<String, String>): TransactionType = when {
        isTransferRow(row) -> TransactionType.TRANSFER_OUTFLOW
        row["type"]?.trimEnd('s')?.equals("expense", ignoreCase = true) == true -> TransactionType.EXPENSE
        row["type"]?.equals("income", ignoreCase = true) == true -> TransactionType.INCOME
        else -> TransactionType.EXPENSE
    }

    private fun transferPairKey(timestamp: Long, note: String, absAmount: BigDecimal): String =
        "${timestamp / 60000}|${note}|${absAmount.toPlainString()}"

    private fun resolveCategory(row: Map<String, String>, existingCats: List<CategoryEntity>): Long? {
        if (isTransferRow(row)) return null
        val envelopeId = row["envelope_id"]?.toIntOrNull()
        val categoryText = row["category"] ?: ""
        val mappedName = envelopeId?.let { assetLoader.envelopeMap[it]?.name }
        if (!mappedName.isNullOrBlank() && mappedName != "TRANSFER") {
            val match = existingCats.find { it.name.equals(mappedName, ignoreCase = true) }
            if (match != null) return match.id
        }
        if (categoryText.isNotBlank()) {
            val fuzzy = existingCats.find { cat ->
                cat.name.equals(categoryText, ignoreCase = true) ||
                        cat.name.lowercase().contains(categoryText.lowercase()) ||
                        categoryText.lowercase().contains(cat.name.lowercase())
            }
            if (fuzzy != null) return fuzzy.id
        }
        return null
    }

    private fun unresolvedCategoryName(row: Map<String, String>): String {
        val envelopeId = row["envelope_id"]?.toIntOrNull()
        val mappedName = envelopeId?.let { assetLoader.envelopeMap[it]?.name }
        val categoryText = row["category"] ?: ""
        return when {
            !mappedName.isNullOrBlank() && mappedName != "TRANSFER" -> mappedName
            categoryText.isNotBlank() -> categoryText
            else -> "Unknown"
        }
    }

    private fun suggestedParentName(row: Map<String, String>): String? =
        row["envelope_id"]?.toIntOrNull()?.let { assetLoader.envelopeMap[it]?.parentName }

    // ──────────────────────────────────────────────────────────────────────────
    // Service API
    // ──────────────────────────────────────────────────────────────────────────

    override suspend fun parseHeaders(inputStream: InputStream): CsvImportSession = withContext(dispatcherProvider.io) {
        val parser = createParser()
        val records = inputStream.use { parser.parseAllRecords(it) }
        val headers = parser.getContext().headers()?.map { it.trim().lowercase() } ?: emptyList()
        val delimiter = parser.getDetectedFormat()?.getDelimiter() ?: ','
        
        if (headers.isEmpty()) return@withContext CsvImportSession(emptyList(), emptyList(), emptyMap(), null, delimiter)

        val previewRows = records.take(5).map { it.values.toList() }
        val detectedApp = if (headers.toSet().containsAll(budgetBakersHeaders)) ImportSourceApp.BUDGET_BAKERS else null
        val mapping = buildInitialMapping(headers, detectedApp ?: ImportSourceApp.GENERIC)

        CsvImportSession(headers, previewRows, mapping, detectedApp, delimiter)
    }

    private fun buildInitialMapping(headers: List<String>, sourceApp: ImportSourceApp): Map<OssWalletField, Int?> = when (sourceApp) {
        ImportSourceApp.BUDGET_BAKERS -> mapOf(
            OssWalletField.DATE to headers.indexOf("date"),
            OssWalletField.ACCOUNT to headers.indexOf("account"),
            OssWalletField.CATEGORY to headers.indexOf("category"),
            OssWalletField.AMOUNT to headers.indexOf("amount"),
            OssWalletField.TYPE to headers.indexOf("type"),
            OssWalletField.NOTE to headers.indexOf("note"),
            OssWalletField.PAYEE to headers.indexOf("payee"),
            OssWalletField.PAYMENT_TYPE to headers.indexOf("payment_type"),
            OssWalletField.LABELS to headers.indexOf("labels"),
            OssWalletField.WARRANTY_MONTHS to headers.indexOf("warranty_in_month"),
            OssWalletField.IS_TRANSFER to headers.indexOf("transfer")
        ).mapValues { if (it.value < 0) null else it.value }
        ImportSourceApp.CASHEW -> mapOf(
            OssWalletField.DATE to headers.indexOf("date"),
            OssWalletField.ACCOUNT to headers.indexOf("account"),
            OssWalletField.CATEGORY to headers.indexOf("category name"),
            OssWalletField.AMOUNT to headers.indexOf("amount"),
            OssWalletField.TYPE to headers.indexOf("income"),
            OssWalletField.NOTE to headers.indexOf("note"),
            OssWalletField.PAYEE to null,
            OssWalletField.PAYMENT_TYPE to null,
            OssWalletField.LABELS to null,
            OssWalletField.WARRANTY_MONTHS to null,
            OssWalletField.IS_TRANSFER to null
        ).mapValues { if (it.value != null && it.value!! < 0) null else it.value }
        else -> OssWalletField.entries.associateWith { field ->
            val guesses = when (field) {
                OssWalletField.DATE -> listOf("date", "datetime", "time")
                OssWalletField.ACCOUNT -> listOf("account", "wallet", "bank")
                OssWalletField.CATEGORY -> listOf("category", "cat")
                OssWalletField.AMOUNT -> listOf("amount", "value", "sum")
                OssWalletField.TYPE -> listOf("type", "transaction_type", "kind")
                OssWalletField.NOTE -> listOf("note", "description", "memo")
                OssWalletField.PAYEE -> listOf("payee", "merchant", "to")
                OssWalletField.PAYMENT_TYPE -> listOf("payment_type", "payment", "method")
                OssWalletField.LABELS -> listOf("labels", "tags", "label")
                OssWalletField.WARRANTY_MONTHS -> listOf("warranty_in_month", "warranty")
                OssWalletField.IS_TRANSFER -> listOf("transfer", "is_transfer")
            }
            guesses.firstNotNullOfOrNull { g -> headers.indexOf(g).takeIf { it >= 0 } }
        }
    }

    override suspend fun generatePreview(session: CsvImportSession, inputStream: InputStream): ImportPreview =
        when (session.sourceApp) {
            ImportSourceApp.BUDGET_BAKERS -> generateBudgetBakersPreview(session, inputStream)
            ImportSourceApp.CASHEW -> generateCashewPreview(session, inputStream)
            else -> generateGenericPreview(session, inputStream)
        }

    private suspend fun generateBudgetBakersPreview(session: CsvImportSession, inputStream: InputStream): ImportPreview = 
        parseRows(inputStream, session.delimiter) { _, rows ->
            val profileId = settingsDataStore.currentProfileId.first()
            val existingTxs = transactionDao.getAllTransactionsSync(profileId)
            val existingAccounts = accountDao.getAllAccountsSync(profileId).associateBy { it.name.lowercase() }
            val existingCategories = categoryDao.getAllCategoriesSync()
            val existingKeys = existingTxs.map { "${it.timestamp / 60000}|${it.amount.abs().toPlainString()}|${it.note}" }.toSet()

            val accountRowCounts = mutableMapOf<String, Int>()
            val accountCurrencies = mutableMapOf<String, MutableSet<String>>()
            val newCategoryMap = mutableMapOf<String, CategoryToMap>()
            val duplicates = mutableListOf<DuplicateGroup>()
            val parseErrors = mutableListOf<ParseError>()
            var validRows = 0
            var totalRows = 0
            val transferGroups = mutableMapOf<String, MutableList<Int>>()

            rows.forEachIndexed { idx, row ->
                totalRows++
                try {
                    val accountName = row["account"] ?: ""
                    val amount = resolveBaseAmount(row)
                    val timestamp = parseDate(row["date"] ?: "")
                    val note = row["note"] ?: ""
                    val currency = row["currency"] ?: ""

                    if (accountName.isNotBlank()) {
                        accountRowCounts[accountName] = (accountRowCounts[accountName] ?: 0) + 1
                        if (currency.isNotBlank()) accountCurrencies.getOrPut(accountName) { mutableSetOf() }.add(currency.uppercase())
                    }

                    if (!isTransferRow(row)) {
                        if (resolveCategory(row, existingCategories) == null) {
                            val displayName = unresolvedCategoryName(row)
                            newCategoryMap.getOrPut(displayName) {
                                CategoryToMap(displayName, row["envelope_id"]?.toIntOrNull(), 
                                    row["custom_category"]?.toBoolean() ?: false, suggestedParentName(row))
                            }
                        }
                    } else {
                        val pairKey = transferPairKey(timestamp, note, amount.abs())
                        transferGroups.getOrPut(pairKey) { mutableListOf() }.add(idx)
                    }

                    val dupKey = "${timestamp / 60000}|${amount.abs().toPlainString()}|$note"
                    if (dupKey in existingKeys) {
                        if (duplicates.size < 500) duplicates.add(DuplicateGroup(dupKey, note, idx, row.values.toList()))
                    } else validRows++
                } catch (e: Exception) {
                    parseErrors.add(ParseError(idx, e.message ?: "Parse error"))
                }
            }

            ImportPreview(
                totalRows = totalRows,
                validRows = validRows,
                duplicates = duplicates,
                allCsvAccounts = accountRowCounts.map { (name, count) ->
                    val match = existingAccounts[name.lowercase()]
                    AccountToMap(name, match?.id, match?.name, count, accountCurrencies[name]?.isNotEmpty() == true)
                }.sortedBy { it.name },
                newCategories = newCategoryMap.values.toList(),
                parseErrors = parseErrors,
                unpairedTransferRows = transferGroups.values.count { it.size == 1 }
            )
        }

    private suspend fun generateGenericPreview(session: CsvImportSession, inputStream: InputStream): ImportPreview =
        parseRows(inputStream, session.delimiter) { headers, rows ->
            val profileId = settingsDataStore.currentProfileId.first()
            val existingTxs = transactionDao.getAllTransactionsSync(profileId)
            val existingAccounts = accountDao.getAllAccountsSync(profileId).map { it.name.lowercase() }.toSet()
            val existingCategories = categoryDao.getAllCategoriesSync().map { it.name.lowercase() }.toSet()
            val existingKeys = existingTxs.map { "${it.timestamp / 60000}|${it.amount.abs().toPlainString()}|${it.note}" }.toSet()

            val newAccounts = mutableSetOf<String>()
            val newCategories = mutableSetOf<String>()
            val duplicates = mutableListOf<DuplicateGroup>()
            val parseErrors = mutableListOf<ParseError>()
            var validRows = 0
            var totalCount = 0

            rows.forEachIndexed { idx, row ->
                totalCount++
                try {
                    val accountName = session.mapping[OssWalletField.ACCOUNT]?.let { headers.getOrNull(it) }?.let { row[it] } ?: ""
                    val categoryName = session.mapping[OssWalletField.CATEGORY]?.let { headers.getOrNull(it) }?.let { row[it] } ?: ""
                    val amountStr = session.mapping[OssWalletField.AMOUNT]?.let { headers.getOrNull(it) }?.let { row[it] } ?: "0"
                    val dateStr = session.mapping[OssWalletField.DATE]?.let { headers.getOrNull(it) }?.let { row[it] } ?: ""
                    val note = session.mapping[OssWalletField.NOTE]?.let { headers.getOrNull(it) }?.let { row[it] } ?: ""

                    if (accountName.isNotBlank() && accountName.lowercase() !in existingAccounts) newAccounts.add(accountName)
                    if (categoryName.isNotBlank() && categoryName.lowercase() !in existingCategories) newCategories.add(categoryName)

                    val amount = normalizeAmount(amountStr)
                    val timestamp = parseDate(dateStr)
                    val dupKey = "${timestamp / 60000}|${amount.abs().toPlainString()}|$note"
                    if (dupKey in existingKeys) {
                        if (duplicates.size < 500) duplicates.add(DuplicateGroup(dupKey, note, idx, row.values.toList()))
                    } else validRows++
                } catch (e: Exception) {
                    parseErrors.add(ParseError(idx, e.message ?: "Parse error"))
                }
            }

            ImportPreview(totalCount, validRows, duplicates, newAccounts.map { AccountToMap(it, null, null) }, 
                newCategories.map { CategoryToMap(it) }, parseErrors, 0)
        }

    private suspend fun generateCashewPreview(session: CsvImportSession, inputStream: InputStream): ImportPreview = 
        parseRows(inputStream, session.delimiter) { _, rows ->
            val profileId = settingsDataStore.currentProfileId.first()
            val existingTxs = transactionDao.getAllTransactionsSync(profileId)
            val existingAccounts = accountDao.getAllAccountsSync(profileId).associateBy { it.name.lowercase() }
            val existingCategories = categoryDao.getAllCategoriesSync()
            val existingKeys = existingTxs.map { "${it.timestamp / 60000}|${it.amount.abs().toPlainString()}|${it.note}" }.toSet()

            val accountRowCounts = mutableMapOf<String, Int>()
            val newCategoryMap = mutableMapOf<String, CategoryToMap>()
            val duplicates = mutableListOf<DuplicateGroup>()
            val parseErrors = mutableListOf<ParseError>()
            var validRows = 0
            var totalRows = 0
            val transferGroups = mutableMapOf<String, MutableList<Int>>()

            rows.forEachIndexed { idx, row ->
                totalRows++
                try {
                    val accountName = row["account"] ?: ""
                    val amount = normalizeAmount(row["amount"])
                    val timestamp = parseDate(row["date"] ?: "")
                    val cat = row["category name"] ?: ""
                    val sub = row["subcategory name"] ?: ""
                    val note = listOf(row["title"], row["note"]).filter { !it.isNullOrBlank() }.joinToString(" — ")

                    if (accountName.isNotBlank()) accountRowCounts[accountName] = (accountRowCounts[accountName] ?: 0) + 1
                    
                    if (row["category name"]?.equals("Balance Correction", ignoreCase = true) == true) {
                        val pairKey = "${timestamp / 60000}|${amount.abs().toPlainString()}"
                        transferGroups.getOrPut(pairKey) { mutableListOf() }.add(idx)
                    } else if (cat.isNotBlank()) {
                        val leaf = sub.takeIf { it.isNotBlank() } ?: cat
                        if (existingCategories.none { it.name.equals(leaf, ignoreCase = true) }) {
                            newCategoryMap.getOrPut(leaf) { CategoryToMap(leaf, suggestedParentName = sub.takeIf { it.isNotBlank() }?.let { cat }) }
                        }
                    }

                    val dupKey = "${timestamp / 60000}|${amount.abs().toPlainString()}|$note"
                    if (dupKey in existingKeys) {
                        if (duplicates.size < 500) duplicates.add(DuplicateGroup(dupKey, note, idx, row.values.toList()))
                    } else validRows++
                } catch (e: Exception) {
                    parseErrors.add(ParseError(idx, e.message ?: "Parse error"))
                }
            }

            ImportPreview(totalRows, validRows, duplicates, accountRowCounts.map { (name, count) ->
                val match = existingAccounts[name.lowercase()]
                AccountToMap(name, match?.id, match?.name, count)
            }, newCategoryMap.values.toList(), parseErrors, transferGroups.values.count { it.size == 1 })
        }

    override suspend fun executeImport(
        session: CsvImportSession,
        inputStream: InputStream,
        duplicatePolicy: DuplicatePolicy,
        entityMappings: List<EntityMapping>,
        selectedDuplicateRowIndices: Set<Int>
    ): Result<ImportSummary> = try {
        when (session.sourceApp) {
            ImportSourceApp.BUDGET_BAKERS -> executeBudgetBakersImport(session, inputStream, duplicatePolicy, entityMappings, selectedDuplicateRowIndices)
            ImportSourceApp.CASHEW -> executeCashewImport(session, inputStream, duplicatePolicy, entityMappings, selectedDuplicateRowIndices)
            else -> executeGenericImport(session, inputStream, duplicatePolicy, entityMappings, selectedDuplicateRowIndices)
        }
    } catch (e: Exception) {
        Result.failure(e)
    }

    private suspend fun executeBudgetBakersImport(
        session: CsvImportSession,
        inputStream: InputStream,
        duplicatePolicy: DuplicatePolicy,
        entityMappings: List<EntityMapping>,
        selectedDuplicateRowIndices: Set<Int>
    ): Result<ImportSummary> = parseRows(inputStream, session.delimiter) { _, rows ->
        val profileId = settingsDataStore.currentProfileId.first()
        val profile = profileDao.getProfileById(profileId)
        val profileBaseCurrency = profile?.baseCurrency ?: "INR"

        database.withTransaction {
            val existingAccounts = accountDao.getAllAccountsSync(profileId).associateBy { it.name.lowercase() }
            val existingCats = categoryDao.getAllCategoriesSync()
            val existingTxs = transactionDao.getAllTransactionsSync(profileId)
            val existingKeys = existingTxs.map { "${it.timestamp / 60000}|${it.amount.abs().toPlainString()}|${it.note}" }.toSet()
            val existingTags = tagDao.getAllTagsSync().toMutableList()

            val accountMap = existingAccounts.mapValues { it.value.id }.toMutableMap()
            val categoryMap = existingCats.associate { it.name.lowercase() to it.id }.toMutableMap()
            val newCategoryColorMap = mutableMapOf<Long, Int>()

            entityMappings.sortedBy { if (it.entityType == EntityMapping.EntityType.ACCOUNT) -1 else 0 }.forEach { em ->
                when (em.entityType) {
                    EntityMapping.EntityType.ACCOUNT -> {
                        if (em.isExcluded) return@forEach
                        val id = em.existingId ?: UUID.randomUUID().toString()
                        if (em.existingId == null) {
                            val finalName = if (existingAccounts.containsKey(em.unknownName.lowercase())) "${em.unknownName}-${randomSuffix()}" else em.unknownName
                            accountDao.upsertAccount(AccountEntity(id, finalName, em.currencyCode ?: profileBaseCurrency, "regular", BRAND_COLORS.random(), "wallet", em.currencyCode ?: profileBaseCurrency, profileId, false, 999))
                        }
                        accountMap[em.unknownName.lowercase()] = id
                    }
                    EntityMapping.EntityType.CATEGORY -> {
                        val existingId = em.existingId
                        if (existingId != null) {
                            categoryMap[em.unknownName.lowercase()] = existingId.toLongOrNull() ?: Category.SYSTEM_OTHERS
                        } else {
                            val parentId = em.parentCategoryId ?: em.parentCategoryName?.let { categoryMap[it.lowercase()] }
                            val color = parentId?.let { newCategoryColorMap[it] ?: existingCats.find { c -> c.id == it }?.colorHex } ?: Color.parseColor(BRAND_COLORS.random())
                            categoryDao.insertCategory(CategoryEntity(0, em.unknownName, "Category", color, false, parentId))
                            val insertedId = categoryDao.getCategoryByName(em.unknownName)?.id ?: Category.SYSTEM_OTHERS
                            categoryMap[em.unknownName.lowercase()] = insertedId
                            if (parentId == null) newCategoryColorMap[insertedId] = color
                        }
                    }
                }
            }

            var imported = 0; var skipped = 0; var rerouted = 0; val errors = mutableListOf<ParseError>()
            val excludedAccounts = entityMappings.filter { it.entityType == EntityMapping.EntityType.ACCOUNT && it.isExcluded }.map { it.unknownName.lowercase() }.toSet()
            
            val rowsList = rows.toList()
            val (transfers, regulars) = rowsList.mapIndexed { idx, row -> idx to row }.partition { isTransferRow(it.second) }
            val parsedTransfers = transfers.mapNotNull { (idx, row) ->
                try {
                    val amount = resolveBaseAmount(row)
                    val timestamp = parseDate(row["date"] ?: "")
                    val tags = parseTags(row["labels"] ?: "")
                    val warranty = row["warranty_in_month"]?.toIntOrNull()?.let { timestamp + (it * 2592000000L) }
                    BudgetTransfer(idx, row, row["account"] ?: "", timestamp, row["note"] ?: "", amount, row["currency"] ?: "INR", resolvePaymentMethod(row), warranty, tags)
                } catch (e: Exception) { errors.add(ParseError(idx, e.message ?: "Transfer parse error")); null }
            }

            parsedTransfers.groupBy { transferPairKey(it.timestamp, it.note, it.amount.abs()) }.forEach { (_, group) ->
                if (group.size == 2) {
                    val source = group.firstOrNull { normalizeAmount(it.row["amount"]).signum() < 0 } ?: group[0]
                    val target = group.firstOrNull { it != source } ?: group[1]
                    val srcExc = source.account in excludedAccounts; val tgtExc = target.account in excludedAccounts
                    when {
                        srcExc && tgtExc -> skipped += 2
                        srcExc -> {
                            val targetId = accountMap[target.account.lowercase()] ?: return@forEach
                            transactionDao.insertTransaction(createTx(target, targetId, TransactionType.TRANSFER_INFLOW, target.amount.abs(), null, profileId, existingTags))
                            imported++; rerouted++
                        }
                        tgtExc -> {
                            val sourceId = accountMap[source.account.lowercase()] ?: return@forEach
                            if (handleDup(source, existingKeys, duplicatePolicy, selectedDuplicateRowIndices)) {
                                transactionDao.insertTransaction(createTx(source, sourceId, TransactionType.TRANSFER_OUTFLOW, source.amount.abs().negate(), null, profileId, existingTags))
                                imported++; rerouted++
                            } else skipped += 2
                        }
                        else -> {
                            val sid = accountMap[source.account.lowercase()]; val tid = accountMap[target.account.lowercase()]
                            if (sid != null && tid != null && handleDup(source, existingKeys, duplicatePolicy, selectedDuplicateRowIndices)) {
                                val rate = if (source.currency != target.currency && source.amount.signum() != 0) target.amount.abs().divide(source.amount.abs(), 8, RoundingMode.HALF_UP) else BigDecimal.ONE
                                val sourceTxId = transactionDao._insertTransaction(createTx(source, sid, TransactionType.TRANSFER_OUTFLOW, source.amount.abs().negate(), tid, profileId, existingTags, target.currency, rate))
                                val targetTxId = transactionDao._insertTransaction(createTx(target, tid, TransactionType.TRANSFER_INFLOW, source.amount.abs().multiply(rate), sid, profileId, existingTags, source.currency, if (rate.signum() != 0) BigDecimal.ONE.divide(rate, 8, RoundingMode.HALF_UP) else BigDecimal.ONE))
                                if (sourceTxId > 0 && targetTxId > 0) transactionLinkDao.insertLinks(listOf(TransactionLinkEntity(0, sourceTxId, targetTxId), TransactionLinkEntity(0, targetTxId, sourceTxId)))
                                imported += 2
                            } else skipped += 2
                        }
                    }
                } else {
                    group.forEach { t ->
                        if (t.account.lowercase() in excludedAccounts) skipped++
                        else {
                            val aid = accountMap[t.account.lowercase()] ?: return@forEach
                            val isOut = t.amount.signum() < 0
                            transactionDao.insertTransaction(createTx(t, aid, if (isOut) TransactionType.TRANSFER_OUTFLOW else TransactionType.TRANSFER_INFLOW, if (isOut) t.amount.abs().negate() else t.amount.abs(), null, profileId, existingTags))
                            imported++
                        }
                    }
                }
            }

            regulars.forEach { (idx, row) ->
                try {
                    val acc = row["account"] ?: ""; if (acc.lowercase() in excludedAccounts) { skipped++; return@forEach }
                    val amount = resolveBaseAmount(row); val timestamp = parseDate(row["date"] ?: ""); val note = row["note"] ?: ""
                    if (!handleDup(idx, timestamp, amount, note, existingKeys, duplicatePolicy, selectedDuplicateRowIndices)) { skipped++; return@forEach }
                    val aid = accountMap[acc.lowercase()] ?: return@forEach
                    val cid = resolveCategory(row, existingCats) ?: categoryMap[unresolvedCategoryName(row).lowercase()] ?: Category.SYSTEM_OTHERS
                    val type = detectTransactionType(row)
                    val finalAmount = if (type == TransactionType.EXPENSE) amount.abs().negate() else amount.abs()
                    val warranty = row["warranty_in_month"]?.toIntOrNull()?.let { timestamp + (it * 2592000000L) }
                    transactionDao.insertTransaction(TransactionEntity(0, (row["category"] ?: note).take(50), finalAmount, timestamp, row["currency"] ?: "INR", type, aid, cid, resolveTagIds(parseTags(row["labels"] ?: ""), existingTags), resolvePaymentMethod(row), row["payee"] ?: "", note, null, profileId, buildGps(row), null, warranty, TransactionStatus.CLEARED))
                    imported++
                } catch (e: Exception) { errors.add(ParseError(idx, e.message ?: "Row error")) }
            }
            Result.success(ImportSummary(imported, skipped, errors, rerouted))
        }
    }

    private suspend fun executeCashewImport(session: CsvImportSession, inputStream: InputStream, duplicatePolicy: DuplicatePolicy, entityMappings: List<EntityMapping>, selectedDuplicateRowIndices: Set<Int>): Result<ImportSummary> = parseRows(inputStream, session.delimiter) { _, rows ->
        val profileId = settingsDataStore.currentProfileId.first()
        val profileBaseCurrency = profileDao.getProfileById(profileId)?.baseCurrency ?: "INR"
        database.withTransaction {
            val existingAccounts = accountDao.getAllAccountsSync(profileId).associateBy { it.name.lowercase() }
            val existingCats = categoryDao.getAllCategoriesSync()
            val existingKeys = transactionDao.getAllTransactionsSync(profileId).map { "${it.timestamp / 60000}|${it.amount.abs().toPlainString()}|${it.note}" }.toSet()
            val existingTags = tagDao.getAllTagsSync().toMutableList()

            val accountMap = existingAccounts.mapValues { it.value.id }.toMutableMap()
            val categoryMap = existingCats.associate { it.name.lowercase() to it.id }.toMutableMap()
            val accountCurrencyMap = existingAccounts.mapValues { it.value.currencyCode }.toMutableMap()
            entityMappings.filter { it.entityType == EntityMapping.EntityType.ACCOUNT }.forEach { em ->
                if (!em.isExcluded) {
                    val id = em.existingId ?: UUID.randomUUID().toString()
                    if (em.existingId == null) accountDao.upsertAccount(AccountEntity(id, em.unknownName, em.currencyCode ?: profileBaseCurrency, "regular", BRAND_COLORS.random(), "wallet", em.currencyCode ?: profileBaseCurrency, profileId, false, 999))
                    accountMap[em.unknownName.lowercase()] = id
                    em.currencyCode?.let { accountCurrencyMap[em.unknownName.lowercase()] = it }
                }
            }
            entityMappings.filter { it.entityType == EntityMapping.EntityType.CATEGORY }.forEach { em ->
                val existingId = em.existingId
                if (existingId != null) categoryMap[em.unknownName.lowercase()] = existingId.toLongOrNull() ?: Category.SYSTEM_OTHERS
                else {
                    val parentId = em.parentCategoryId ?: em.parentCategoryName?.let { categoryMap[it.lowercase()] }
                    categoryDao.insertCategory(CategoryEntity(0, em.unknownName, "Category", Color.parseColor(BRAND_COLORS.random()), false, parentId))
                    categoryMap[em.unknownName.lowercase()] = categoryDao.getCategoryByName(em.unknownName)?.id ?: Category.SYSTEM_OTHERS
                }
            }

            var imported = 0; var skipped = 0; var rerouted = 0; val errors = mutableListOf<ParseError>()
            val excludedAccounts = entityMappings.filter { it.entityType == EntityMapping.EntityType.ACCOUNT && it.isExcluded }.map { it.unknownName.lowercase() }.toSet()
            val rowsList = rows.toList()
            val (transfers, regulars) = rowsList.mapIndexed { idx, row -> idx to row }.partition { it.second["category name"]?.equals("Balance Correction", ignoreCase = true) == true }
            
            val parsedTransfers = transfers.mapNotNull { (idx, row) ->
                try {
                    val amount = normalizeAmount(row["amount"])
                    val timestamp = parseDate(row["date"] ?: "")
                    val note = listOf(row["title"], row["note"]).filter { !it.isNullOrBlank() }.joinToString(" — ")
                    BudgetTransfer(idx, row, row["account"] ?: "", timestamp, note, amount, row["currency"] ?: accountCurrencyMap[row["account"]?.lowercase()] ?: profileBaseCurrency, PaymentMethod.MOBILE_PAYMENT, null, emptySet())
                } catch (e: Exception) { errors.add(ParseError(idx, e.message ?: "Transfer error")); null }
            }

            parsedTransfers.groupBy { "${it.timestamp / 60000}|${it.amount.abs().toPlainString()}" }.forEach { (_, group) ->
                if (group.size == 2) {
                    val source = group.firstOrNull { it.amount.signum() < 0 } ?: group[0]
                    val target = group.firstOrNull { it != source } ?: group[1]
                    val srcExc = source.account in excludedAccounts; val tgtExc = target.account in excludedAccounts
                    when {
                        srcExc && tgtExc -> skipped += 2
                        srcExc -> {
                            val tid = accountMap[target.account.lowercase()] ?: return@forEach
                            transactionDao.insertTransaction(createTx(target, tid, TransactionType.TRANSFER_INFLOW, target.amount.abs(), null, profileId, existingTags))
                            imported++; rerouted++
                        }
                        tgtExc -> {
                            val sid = accountMap[source.account.lowercase()] ?: return@forEach
                            if (handleDup(source, existingKeys, duplicatePolicy, selectedDuplicateRowIndices)) {
                                transactionDao.insertTransaction(createTx(source, sid, TransactionType.TRANSFER_OUTFLOW, source.amount.abs().negate(), null, profileId, existingTags))
                                imported++; rerouted++
                            } else skipped += 2
                        }
                        else -> {
                            val sid = accountMap[source.account.lowercase()]; val tid = accountMap[target.account.lowercase()]
                            if (sid != null && tid != null && handleDup(source, existingKeys, duplicatePolicy, selectedDuplicateRowIndices)) {
                                val rate = if (source.currency != target.currency && source.amount.signum() != 0) target.amount.abs().divide(source.amount.abs(), 8, RoundingMode.HALF_UP) else BigDecimal.ONE
                                val sourceTxId = transactionDao._insertTransaction(createTx(source, sid, TransactionType.TRANSFER_OUTFLOW, source.amount.abs().negate(), tid, profileId, existingTags, target.currency, rate))
                                val targetTxId = transactionDao._insertTransaction(createTx(target, tid, TransactionType.TRANSFER_INFLOW, source.amount.abs().multiply(rate), sid, profileId, existingTags, source.currency, if (rate.signum() != 0) BigDecimal.ONE.divide(rate, 8, RoundingMode.HALF_UP) else BigDecimal.ONE))
                                if (sourceTxId > 0 && targetTxId > 0) transactionLinkDao.insertLinks(listOf(TransactionLinkEntity(0, sourceTxId, targetTxId), TransactionLinkEntity(0, targetTxId, sourceTxId)))
                                imported += 2
                            } else skipped += 2
                        }
                    }
                } else group.forEach { t ->
                    if (t.account.lowercase() !in excludedAccounts) {
                        val aid = accountMap[t.account.lowercase()] ?: return@forEach
                        val isOut = t.amount.signum() < 0
                        transactionDao.insertTransaction(createTx(t, aid, if (isOut) TransactionType.TRANSFER_OUTFLOW else TransactionType.TRANSFER_INFLOW, if (isOut) t.amount.abs().negate() else t.amount.abs(), null, profileId, existingTags))
                        imported++
                    } else skipped++
                }
            }

            regulars.forEach { (idx, row) ->
                try {
                    val acc = row["account"] ?: ""; if (acc.lowercase() in excludedAccounts) { skipped++; return@forEach }
                    val amount = normalizeAmount(row["amount"]); val timestamp = parseDate(row["date"] ?: ""); val note = listOf(row["title"], row["note"]).filter { !it.isNullOrBlank() }.joinToString(" — ")
                    if (!handleDup(idx, timestamp, amount, note, existingKeys, duplicatePolicy, selectedDuplicateRowIndices)) { skipped++; return@forEach }
                    val aid = accountMap[acc.lowercase()] ?: return@forEach
                    val cat = row["category name"] ?: ""; val sub = row["subcategory name"] ?: ""; val leaf = sub.takeIf { it.isNotBlank() } ?: cat
                    val cid = existingCats.find { it.name.equals(leaf, ignoreCase = true) }?.id ?: categoryMap[leaf.lowercase()] ?: Category.SYSTEM_OTHERS
                    val isInc = row["income"]?.toBoolean() ?: false
                    val finalAmount = if (isInc) amount.abs() else amount.abs().negate()
                    val tags = resolveTagIds(if (row["type"]?.contains("repetitive", true) == true) setOf("Planned") else emptySet(), existingTags)
                    transactionDao.insertTransaction(TransactionEntity(0, leaf.take(50), finalAmount, timestamp, row["currency"] ?: accountCurrencyMap[acc.lowercase()] ?: profileBaseCurrency, if (isInc) TransactionType.INCOME else TransactionType.EXPENSE, aid, cid, tags, PaymentMethod.MOBILE_PAYMENT, "", note, null, profileId, null, null, null, TransactionStatus.CLEARED))
                    imported++
                } catch (e: Exception) { errors.add(ParseError(idx, e.message ?: "Row error")) }
            }
            Result.success(ImportSummary(imported, skipped, errors, rerouted))
        }
    }

    private suspend fun executeGenericImport(session: CsvImportSession, inputStream: InputStream, duplicatePolicy: DuplicatePolicy, entityMappings: List<EntityMapping>, selectedDuplicateRowIndices: Set<Int>): Result<ImportSummary> = parseRows(inputStream, session.delimiter) { headers, rows ->
        val profileId = settingsDataStore.currentProfileId.first()
        val profileBaseCurrency = profileDao.getProfileById(profileId)?.baseCurrency ?: "INR"
        database.withTransaction {
            val existingAccounts = accountDao.getAllAccountsSync(profileId).associateBy { it.name.lowercase() }
            val existingCats = categoryDao.getAllCategoriesSync().associateBy { it.name.lowercase() }
            val existingKeys = transactionDao.getAllTransactionsSync(profileId).map { "${it.timestamp / 60000}|${it.amount.abs().toPlainString()}|${it.note}" }.toSet()
            val accountMap = existingAccounts.mapValues { it.value.id }.toMutableMap()
            val categoryMap = existingCats.mapValues { it.value.id }.toMutableMap()
            val accountCurrencyMap = existingAccounts.mapValues { it.value.currencyCode }.toMutableMap()

            entityMappings.forEach { em ->
                when (em.entityType) {
                    EntityMapping.EntityType.ACCOUNT -> {
                        val id = em.existingId ?: UUID.randomUUID().toString()
                        if (em.existingId == null) accountDao.upsertAccount(AccountEntity(id, em.unknownName, em.currencyCode ?: profileBaseCurrency, "regular", BRAND_COLORS.random(), "wallet", em.currencyCode ?: profileBaseCurrency, profileId, false, 999))
                        accountMap[em.unknownName.lowercase()] = id
                        em.currencyCode?.let { accountCurrencyMap[em.unknownName.lowercase()] = it }
                    }
                    EntityMapping.EntityType.CATEGORY -> {
                        val existingId = em.existingId
                        if (existingId != null) categoryMap[em.unknownName.lowercase()] = existingId.toLongOrNull() ?: Category.SYSTEM_OTHERS
                        else {
                            val parentId = em.parentCategoryId ?: em.parentCategoryName?.let { categoryMap[it.lowercase()] }
                            categoryDao.insertCategory(CategoryEntity(0, em.unknownName, "Category", Color.parseColor(BRAND_COLORS.random()), false, parentId))
                            categoryMap[em.unknownName.lowercase()] = categoryDao.getCategoryByName(em.unknownName)?.id ?: Category.SYSTEM_OTHERS
                        }
                    }
                }
            }

            var imported = 0; var skipped = 0; val errors = mutableListOf<ParseError>()
            rows.forEachIndexed { idx, row ->
                try {
                    val acc = session.mapping[OssWalletField.ACCOUNT]?.let { headers.getOrNull(it) }?.let { row[it] } ?: ""
                    val cat = session.mapping[OssWalletField.CATEGORY]?.let { headers.getOrNull(it) }?.let { row[it] } ?: ""
                    val amount = normalizeAmount(session.mapping[OssWalletField.AMOUNT]?.let { headers.getOrNull(it) }?.let { row[it] })
                    val timestamp = parseDate(session.mapping[OssWalletField.DATE]?.let { headers.getOrNull(it) }?.let { row[it] } ?: "")
                    val note = session.mapping[OssWalletField.NOTE]?.let { headers.getOrNull(it) }?.let { row[it] } ?: ""
                    if (!handleDup(idx, timestamp, amount, note, existingKeys, duplicatePolicy, selectedDuplicateRowIndices)) { skipped++; return@forEachIndexed }
                    val aid = accountMap[acc.lowercase()] ?: run { skipped++; return@forEachIndexed }
                    val type = when {
                        row[headers.getOrNull(session.mapping[OssWalletField.IS_TRANSFER] ?: -1)]?.toBoolean() == true -> TransactionType.TRANSFER_OUTFLOW
                        row[headers.getOrNull(session.mapping[OssWalletField.TYPE] ?: -1)]?.contains("income", true) == true -> TransactionType.INCOME
                        else -> TransactionType.EXPENSE
                    }
                    val finalAmount = if (type == TransactionType.INCOME) amount.abs() else amount.abs().negate()
                    transactionDao.insertTransaction(TransactionEntity(0, cat.ifBlank { note.take(30) }, finalAmount, timestamp, accountCurrencyMap[acc.lowercase()] ?: profileBaseCurrency, type, aid, categoryMap[cat.lowercase()] ?: Category.SYSTEM_OTHERS, parseTags(row[headers.getOrNull(session.mapping[OssWalletField.LABELS] ?: -1)] ?: ""), resolveGenericMethod(row[headers.getOrNull(session.mapping[OssWalletField.PAYMENT_TYPE] ?: -1)]), row[headers.getOrNull(session.mapping[OssWalletField.PAYEE] ?: -1)] ?: "", note, null, profileId, null, null, null, TransactionStatus.CLEARED))
                    imported++
                } catch (e: Exception) { errors.add(ParseError(idx, e.message ?: "Row error")) }
            }
            Result.success(ImportSummary(imported, skipped, errors))
        }
    }

    // ──────────────────────────────────────────────────────────────────────────
    // Internal Utilities
    // ──────────────────────────────────────────────────────────────────────────

    private data class BudgetTransfer(val idx: Int, val row: Map<String, String>, val account: String, val timestamp: Long, val note: String, val amount: BigDecimal, val currency: String, val method: PaymentMethod, val warranty: Long?, val tags: Set<String>)

    private suspend fun createTx(t: BudgetTransfer, aid: String, type: TransactionType, amount: BigDecimal, targetAid: String?, profileId: Int, existingTags: MutableList<TagEntity>, targetCurrency: String? = null, rate: BigDecimal = BigDecimal.ONE) =
        TransactionEntity(0, t.note.ifBlank { "Transfer" }.take(50), amount, t.timestamp, t.currency, type, aid, Category.SYSTEM_TRANSFER, resolveTagIds(t.tags, existingTags), t.method, t.row["payee"] ?: "", t.note, null, profileId, buildGps(t.row), t.row["payee"]?.takeIf { it.isNotBlank() }, t.warranty, TransactionStatus.CLEARED, targetAid, targetCurrency, rate)

    private fun handleDup(t: BudgetTransfer, keys: Set<String>, policy: DuplicatePolicy, indices: Set<Int>) = handleDup(t.idx, t.timestamp, t.amount, t.note, keys, policy, indices)
    private fun handleDup(idx: Int, timestamp: Long, amount: BigDecimal, note: String, keys: Set<String>, policy: DuplicatePolicy, indices: Set<Int>): Boolean {
        val key = "${timestamp / 60000}|${amount.abs().toPlainString()}|$note"
        return if (key in keys) when (policy) {
            DuplicatePolicy.SKIP -> false
            DuplicatePolicy.IMPORT_BOTH -> true
            DuplicatePolicy.REPLACE_EXISTING -> idx in indices
        } else true
    }

    private fun resolvePaymentMethod(row: Map<String, String>): PaymentMethod = PaymentMethod.fromString(assetLoader.paymentTypeMap[row["payment_type"]] ?: "MOBILE_PAYMENT")
    private fun resolveGenericMethod(s: String?): PaymentMethod = when (s?.uppercase()?.replace(" ", "_")) {
        "MOBILE_PAYMENT" -> PaymentMethod.MOBILE_PAYMENT
        "CREDIT_CARD" -> PaymentMethod.CREDIT_CARD
        "TRANSFER", "BANK_TRANSFER" -> PaymentMethod.BANK_TRANSFER
        "CASH" -> PaymentMethod.CASH
        else -> PaymentMethod.MOBILE_PAYMENT
    }
    private fun parseTags(s: String): Set<String> = if (s.isBlank()) emptySet() else s.trim().removePrefix("[").removeSuffix("]").split("|", ";", ",").map { it.trim().removePrefix("\"").removeSuffix("\"").trim() }.filter { it.isNotBlank() }.toSet()
    private suspend fun resolveTagIds(names: Set<String>, list: MutableList<TagEntity>): Set<String> {
        val resolved = mutableSetOf<String>()
        for (name in names) {
            val existing = list.find { it.name.equals(name, true) }
            if (existing != null) resolved.add(existing.name)
            else {
                val newTag = TagEntity(0, name, 0xFF9E9E9E.toInt())
                val id = tagDao.insertTag(newTag); if (id > 0) list.add(newTag.copy(id = id))
                resolved.add(name)
            }
        }
        return resolved
    }
    private fun buildGps(row: Map<String, String>): String? = row["gps_latitude"]?.toDoubleOrNull()?.let { lat -> row["gps_longitude"]?.toDoubleOrNull()?.let { lon -> "$lat,$lon" } }
    private fun randomSuffix(): String = (('a'..'z') + ('0'..'9')).shuffled().take(6).joinToString("")

    companion object {
        private val BRAND_COLORS = listOf("#004D5F", "#1A237E", "#1B5E20", "#B71C1C", "#4A148C", "#E65100", "#006064", "#212121", "#795548", "#607D8B")
    }
}
