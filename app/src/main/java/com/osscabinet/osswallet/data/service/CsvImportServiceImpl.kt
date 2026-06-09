package com.osscabinet.ossWallet.data.service

import androidx.room.withTransaction
import com.osscabinet.ossWallet.data.local.AppDatabase
import com.osscabinet.ossWallet.data.local.SettingsDataStore
import com.osscabinet.ossWallet.data.local.dao.AccountDao
import com.osscabinet.ossWallet.data.local.dao.CategoryDao
import com.osscabinet.ossWallet.data.local.dao.ProfileDao
import com.osscabinet.ossWallet.data.local.dao.TagDao
import com.osscabinet.ossWallet.data.local.dao.TransactionDao
import com.osscabinet.ossWallet.data.local.entity.AccountEntity
import com.osscabinet.ossWallet.data.local.entity.CategoryEntity
import com.osscabinet.ossWallet.data.local.entity.TagEntity
import com.osscabinet.ossWallet.data.local.entity.TransactionEntity
import com.osscabinet.ossWallet.domain.model.PaymentMethod
import com.osscabinet.ossWallet.domain.model.TransactionStatus
import com.osscabinet.ossWallet.domain.model.TransactionType
import com.osscabinet.ossWallet.domain.model.import_csv.CategoryToMap
import com.osscabinet.ossWallet.domain.model.import_csv.CsvImportSession
import com.osscabinet.ossWallet.domain.model.import_csv.DuplicateGroup
import com.osscabinet.ossWallet.domain.model.import_csv.DuplicatePolicy
import com.osscabinet.ossWallet.domain.model.import_csv.EntityMapping
import com.osscabinet.ossWallet.domain.model.import_csv.ImportPreview
import com.osscabinet.ossWallet.domain.model.import_csv.ImportSourceApp
import com.osscabinet.ossWallet.domain.model.import_csv.ImportSummary
import com.osscabinet.ossWallet.domain.model.import_csv.OssWalletField
import com.osscabinet.ossWallet.domain.model.import_csv.ParseError
import com.osscabinet.ossWallet.domain.service.CsvImportService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import org.apache.commons.csv.CSVFormat
import org.apache.commons.csv.CSVParser
import java.io.InputStream
import java.math.BigDecimal
import java.math.RoundingMode
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CsvImportServiceImpl @Inject constructor(
    private val accountDao: AccountDao,
    private val categoryDao: CategoryDao,
    private val transactionDao: TransactionDao,
    private val tagDao: TagDao,
    private val profileDao: ProfileDao,
    private val database: AppDatabase,
    private val settingsDataStore: SettingsDataStore,
    private val assetLoader: BudgetBakersAssetLoader
) : CsvImportService {

    private val budgetBakersHeaders = setOf(
        "account", "category", "currency", "amount", "ref_currency_amount",
        "type", "payment_type", "payment_type_local", "note", "date",
        "gps_latitude", "gps_longitude", "gps_accuracy_in_meters",
        "warranty_in_month", "transfer", "payee", "labels"
    )

    // ──────────────────────────────────────────────────────────────────────────
    // Date parsing helpers
    // ──────────────────────────────────────────────────────────────────────────

    private val dateFormats = listOf(
        SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US),
        SimpleDateFormat("yyyy-MM-dd", Locale.US),
        SimpleDateFormat("dd/MM/yyyy", Locale.US),
        SimpleDateFormat("MM/dd/yyyy", Locale.US)
    )

    private fun parseDate(dateStr: String): Long =
        dateFormats.firstNotNullOfOrNull { fmt ->
            try { fmt.parse(dateStr)?.time } catch (_: Exception) { null }
        } ?: 0L

    // ──────────────────────────────────────────────────────────────────────────
    // CSV format helpers
    // ──────────────────────────────────────────────────────────────────────────

    private fun buildBudgetBakersCsvFormat(): CSVFormat =
        CSVFormat.DEFAULT.builder()
            .setDelimiter(';')
            .setQuote('"')
            .setIgnoreEmptyLines(true)
            .setTrim(true)
            .build()

    /**
     * Parse the input stream using Apache Commons CSV, returning (headers, rows).
     * Each row is a Map<columnName, value>.
     */
    private fun parseBudgetBakersRows(inputStream: InputStream): Pair<List<String>, List<Map<String, String>>> {
        val format = buildBudgetBakersCsvFormat()
        val parser = CSVParser.parse(inputStream.bufferedReader(), format)
        val records = parser.records
        if (records.isEmpty()) return emptyList<String>() to emptyList()

        val headers = records.first().toList().map { it.trim().lowercase() }
        val rows = records.drop(1).map { record ->
            headers.mapIndexed { i, header -> header to (record.get(i).ifEmpty { "" }) }.toMap()
        }
        return headers to rows
    }

    // ──────────────────────────────────────────────────────────────────────────
    // Transfer / type detection
    // ──────────────────────────────────────────────────────────────────────────

    /**
     * A row is a transfer only when the dedicated `transfer` boolean column is true,
     * OR the `category` text equals "TRANSFER".
     *
     * IMPORTANT: payment_type=TRANSFER means "bank transfer" payment method — it does NOT
     * make a transaction a transfer. Salary rows have payment_type=TRANSFER and must stay INCOME.
     */
    private fun isTransferRow(row: Map<String, String>): Boolean =
        row["transfer"]?.equals("true", ignoreCase = true) == true ||
                row["category"]?.equals("TRANSFER", ignoreCase = true) == true

    private fun detectTransactionType(row: Map<String, String>): TransactionType = when {
        isTransferRow(row) -> TransactionType.TRANSFER
        row["type"]?.trimEnd('s')?.equals("expense", ignoreCase = true) == true -> TransactionType.EXPENSE
        row["type"]?.equals("income", ignoreCase = true) == true -> TransactionType.INCOME
        else -> TransactionType.EXPENSE
    }

    // ──────────────────────────────────────────────────────────────────────────
    // Transfer pairing key
    // ──────────────────────────────────────────────────────────────────────────

    /**
     * Two transfer rows belong to the same pair when they share:
     *   - the same minute-truncated timestamp
     *   - the same note (possibly empty)
     *   - the same absolute amount
     *
     * Using the separator "|" to distinguish `note` from `amount` so that a note "5|100"
     * does not collide with (note="5", amount=100).
     */
    private fun transferPairKey(timestamp: Long, note: String, absAmount: BigDecimal): String =
        "${timestamp / 60000}|${note}|${absAmount.toPlainString()}"

    // ──────────────────────────────────────────────────────────────────────────
    // Amount helpers
    // ──────────────────────────────────────────────────────────────────────────

    /**
     * Returns the base-currency amount to store:
     * - ref_currency_amount is the authoritative base amount (always in the profile's base currency)
     * - amount is the original/foreign amount
     * If ref_currency_amount is absent or zero, fall back to amount.
     */
    private fun resolveBaseAmount(row: Map<String, String>): BigDecimal {
        val ref = row["ref_currency_amount"]?.replace(",", "")?.toBigDecimalOrNull()
        val raw = row["amount"]?.replace(",", "")?.toBigDecimalOrNull() ?: BigDecimal.ZERO
        return if (ref != null && ref.compareTo(BigDecimal.ZERO) != 0) ref else raw
    }

    // ──────────────────────────────────────────────────────────────────────────
    // Category resolution
    // ──────────────────────────────────────────────────────────────────────────

    private fun resolveCategory(
        row: Map<String, String>,
        existingCats: List<CategoryEntity>
    ): Long? {
        if (isTransferRow(row)) return null // transfers don't need a category

        val envelopeId = row["envelope_id"]?.toIntOrNull()
        val categoryText = row["category"] ?: ""

        // 1. Try envelope_id → mapped OssWallet name
        val mappedName = envelopeId?.let { assetLoader.envelopeMap[it] }
        if (!mappedName.isNullOrBlank() && mappedName != "TRANSFER") {
            val match = existingCats.find { it.name.equals(mappedName, ignoreCase = true) }
            if (match != null) return match.id
        }

        // 2. Fuzzy match on category text column
        if (categoryText.isNotBlank()) {
            val fuzzy = existingCats.find { cat ->
                cat.name.equals(categoryText, ignoreCase = true) ||
                        cat.name.lowercase().contains(categoryText.lowercase()) ||
                        categoryText.lowercase().contains(cat.name.lowercase())
            }
            if (fuzzy != null) return fuzzy.id
        }

        return null // unresolved
    }

    /**
     * Returns the best display name for a category that could not be resolved.
     * Prefers the mapped OssWallet name (from envelope_id) over the raw CSV text.
     */
    private fun unresolvedCategoryName(row: Map<String, String>): String {
        val envelopeId = row["envelope_id"]?.toIntOrNull()
        val mappedName = envelopeId?.let { assetLoader.envelopeMap[it] }
        val categoryText = row["category"] ?: ""
        return when {
            !mappedName.isNullOrBlank() && mappedName != "TRANSFER" -> mappedName
            categoryText.isNotBlank() -> categoryText
            else -> "Unknown"
        }
    }

    // ──────────────────────────────────────────────────────────────────────────
    // parseHeaders — detect format, build column mapping
    // ──────────────────────────────────────────────────────────────────────────

    override suspend fun parseHeaders(inputStream: InputStream): CsvImportSession =
        withContext(Dispatchers.IO) {
            val bufferedReader = inputStream.bufferedReader()
            val firstLineCheck = bufferedReader.readLine() ?: return@withContext CsvImportSession(emptyList(), emptyList(), emptyMap(), null)

            val isBudgetBakers = firstLineCheck.contains(';') ||
                budgetBakersHeaders.any { h -> firstLineCheck.lowercase().contains(h) }

            if (isBudgetBakers) {
                val format = buildBudgetBakersCsvFormat()
                // Re-combine the first line with the rest since we already read one line
                val remaining = firstLineCheck + "\n" + bufferedReader.readText()
                val parser = CSVParser.parse(remaining.reader(), format)
                val records = parser.records
                if (records.isEmpty()) return@withContext CsvImportSession(emptyList(), emptyList(), emptyMap(), ImportSourceApp.BUDGET_BAKERS, ';')

                val headers = records.first().toList().map { it.trim().lowercase() }
                val previewRows = records.drop(1).take(5).map { rec -> rec.toList() }
                val sourceApp = ImportSourceApp.BUDGET_BAKERS
                val mapping = buildInitialMapping(headers, sourceApp)

                CsvImportSession(headers, previewRows, mapping, sourceApp, ';')
            } else {
                val delimiter = ','
                val headers = firstLineCheck.split(delimiter).map { it.trim().lowercase().replace("\"", "") }
                val sourceApp = ImportSourceApp.GENERIC
                val mapping = buildInitialMapping(headers, sourceApp)
                val previewRows = bufferedReader.readLines().take(5).map { row ->
                    row.split(delimiter).map { c -> c.trim().replace("\"", "") }
                }

                CsvImportSession(headers, previewRows, mapping, sourceApp, delimiter)
            }
        }

    private fun buildInitialMapping(
        headers: List<String>,
        sourceApp: ImportSourceApp
    ): Map<OssWalletField, Int?> {
        return when (sourceApp) {
            ImportSourceApp.BUDGET_BAKERS -> mapOf(
                OssWalletField.DATE to headers.indexOf("date").takeIf { it >= 0 },
                OssWalletField.ACCOUNT to headers.indexOf("account").takeIf { it >= 0 },
                OssWalletField.CATEGORY to headers.indexOf("category").takeIf { it >= 0 },
                OssWalletField.AMOUNT to headers.indexOf("amount").takeIf { it >= 0 },
                OssWalletField.TYPE to headers.indexOf("type").takeIf { it >= 0 },
                OssWalletField.NOTE to headers.indexOf("note").takeIf { it >= 0 },
                OssWalletField.PAYEE to headers.indexOf("payee").takeIf { it >= 0 },
                OssWalletField.PAYMENT_TYPE to headers.indexOf("payment_type").takeIf { it >= 0 },
                OssWalletField.LABELS to headers.indexOf("labels").takeIf { it >= 0 },
                OssWalletField.WARRANTY_MONTHS to headers.indexOf("warranty_in_month").takeIf { it >= 0 },
                OssWalletField.IS_TRANSFER to headers.indexOf("transfer").takeIf { it >= 0 }
            )
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
    }

    // ──────────────────────────────────────────────────────────────────────────
    // generatePreview
    // ──────────────────────────────────────────────────────────────────────────

    override suspend fun generatePreview(
        session: CsvImportSession,
        inputStream: InputStream
    ): ImportPreview = withContext(Dispatchers.IO) {
        if (session.sourceApp == ImportSourceApp.BUDGET_BAKERS) {
            generateBudgetBakersPreview(session, inputStream)
        } else {
            generateGenericPreview(session, inputStream)
        }
    }

    private suspend fun generateBudgetBakersPreview(
        session: CsvImportSession,
        inputStream: InputStream
    ): ImportPreview {
        val (_, rows) = parseBudgetBakersRows(inputStream)
        if (rows.isEmpty()) {
            return ImportPreview(0, 0, emptyList(), emptyList(), emptyList(), emptyList(), 0)
        }

        val existingTxs = transactionDao.getAllTransactionsSync()
        val existingAccounts = accountDao.getAllAccountsSync().map { it.name.lowercase() }.toSet()
        val existingCategories = categoryDao.getAllCategoriesSync()

        val existingKeys = existingTxs.map { tx ->
            "${tx.timestamp / 60000}|${tx.amount.abs().toPlainString()}|${tx.note}"
        }.toSet()

        val newAccounts = mutableSetOf<String>()
        // key = category name (after mapping), to deduplicate
        val newCategoryMap = mutableMapOf<String, CategoryToMap>()
        val duplicates = mutableListOf<DuplicateGroup>()
        val parseErrors = mutableListOf<ParseError>()
        var validRows = 0

        // Transfer pairing: group transfer rows by pair key to detect unpaired rows
        val transferGroups = mutableMapOf<String, MutableList<Int>>() // pairKey -> list of row indices

        rows.forEachIndexed { idx, row ->
            try {
                val accountName = row["account"] ?: ""
                val dateStr = row["date"] ?: ""
                val note = row["note"] ?: ""
                val baseAmount = resolveBaseAmount(row)
                val timestamp = parseDate(dateStr)
                val isTransfer = isTransferRow(row)

                if (accountName.isNotBlank() && accountName.lowercase() !in existingAccounts) {
                    newAccounts.add(accountName)
                }

                // Unresolved category tracking
                if (!isTransfer) {
                    val resolved = resolveCategory(row, existingCategories)
                    if (resolved == null) {
                        val displayName = unresolvedCategoryName(row)
                        if (!newCategoryMap.containsKey(displayName)) {
                            val envelopeId = row["envelope_id"]?.toIntOrNull()
                            val isCustom = row["custom_category"]?.equals("true", ignoreCase = true) == true
                            newCategoryMap[displayName] = CategoryToMap(
                                name = displayName,
                                envelopeId = envelopeId,
                                isCustomCategory = isCustom
                            )
                        }
                    }
                }

                // Transfer pairing tracking
                if (isTransfer) {
                    val pairKey = transferPairKey(timestamp, note, baseAmount.abs())
                    transferGroups.getOrPut(pairKey) { mutableListOf() }.add(idx)
                }

                // Duplicate detection
                val dupKey = "${timestamp / 60000}|${baseAmount.abs().toPlainString()}|${note}"
                if (dupKey in existingKeys) {
                    duplicates.add(
                        DuplicateGroup(
                            key = dupKey,
                            existingNote = note,
                            incomingRowIndex = idx,
                            incomingValues = row.values.toList()
                        )
                    )
                } else {
                    validRows++
                }
            } catch (e: Exception) {
                parseErrors.add(ParseError(idx, e.message ?: "Parse error"))
            }
        }

        // Count unpaired transfer rows (groups with only one entry)
        val unpairedTransferRows = transferGroups.values.count { it.size == 1 }

        return ImportPreview(
            totalRows = rows.size,
            validRows = validRows,
            duplicates = duplicates,
            newAccounts = newAccounts.toList(),
            newCategories = newCategoryMap.values.toList(),
            parseErrors = parseErrors,
            unpairedTransferRows = unpairedTransferRows
        )
    }

    private suspend fun generateGenericPreview(
        session: CsvImportSession,
        inputStream: InputStream
    ): ImportPreview {
        val lines = inputStream.bufferedReader().readLines().drop(1)
        val existingTxs = transactionDao.getAllTransactionsSync()
        val existingAccounts = accountDao.getAllAccountsSync().map { it.name.lowercase() }.toSet()
        val existingCategories = categoryDao.getAllCategoriesSync().map { it.name.lowercase() }.toSet()

        val newAccounts = mutableSetOf<String>()
        val newCategories = mutableSetOf<String>()
        val duplicates = mutableListOf<DuplicateGroup>()
        val parseErrors = mutableListOf<ParseError>()
        var validRows = 0

        val existingKeys = existingTxs.map { tx ->
            "${tx.timestamp / 60000}|${tx.amount.abs().toPlainString()}|${tx.note}"
        }.toSet()

        lines.forEachIndexed { idx, line ->
            try {
                val cols = line.split(session.delimiter).map { it.trim().replace("\"", "") }
                val accountName = session.mapping[OssWalletField.ACCOUNT]?.let { cols.getOrNull(it) } ?: ""
                val categoryName = session.mapping[OssWalletField.CATEGORY]?.let { cols.getOrNull(it) } ?: ""
                val amountStr = session.mapping[OssWalletField.AMOUNT]?.let { cols.getOrNull(it) } ?: "0"
                val dateStr = session.mapping[OssWalletField.DATE]?.let { cols.getOrNull(it) } ?: ""
                val note = session.mapping[OssWalletField.NOTE]?.let { cols.getOrNull(it) } ?: ""

                if (accountName.isNotBlank() && accountName.lowercase() !in existingAccounts) {
                    newAccounts.add(accountName)
                }
                if (categoryName.isNotBlank() && categoryName.lowercase() !in existingCategories) {
                    newCategories.add(categoryName)
                }

                val amount = amountStr.replace(",", "").toBigDecimalOrNull() ?: BigDecimal.ZERO
                val timestamp = parseDate(dateStr)

                val dupKey = "${timestamp / 60000}|${amount.abs().toPlainString()}|$note"
                if (dupKey in existingKeys) {
                    duplicates.add(
                        DuplicateGroup(
                            key = dupKey,
                            existingNote = note,
                            incomingRowIndex = idx,
                            incomingValues = cols
                        )
                    )
                } else {
                    validRows++
                }
            } catch (e: Exception) {
                parseErrors.add(ParseError(idx, e.message ?: "Parse error"))
            }
        }

        return ImportPreview(
            totalRows = lines.size,
            validRows = validRows,
            duplicates = duplicates,
            newAccounts = newAccounts.toList(),
            newCategories = newCategories.map { CategoryToMap(name = it) },
            parseErrors = parseErrors,
            unpairedTransferRows = 0
        )
    }

    // ──────────────────────────────────────────────────────────────────────────
    // executeImport
    // ──────────────────────────────────────────────────────────────────────────

    override suspend fun executeImport(
        session: CsvImportSession,
        inputStream: InputStream,
        duplicatePolicy: DuplicatePolicy,
        entityMappings: List<EntityMapping>,
        selectedDuplicateRowIndices: Set<Int>
    ): Result<ImportSummary> = withContext(Dispatchers.IO) {
        try {
            if (session.sourceApp == ImportSourceApp.BUDGET_BAKERS) {
                executeBudgetBakersImport(session, inputStream, duplicatePolicy, entityMappings, selectedDuplicateRowIndices)
            } else {
                executeGenericImport(session, inputStream, duplicatePolicy, entityMappings, selectedDuplicateRowIndices)
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // ──────────────────────────────────────────────────────────────────────────
    // BudgetBakers-specific import
    // ──────────────────────────────────────────────────────────────────────────

    private suspend fun executeBudgetBakersImport(
        session: CsvImportSession,
        inputStream: InputStream,
        duplicatePolicy: DuplicatePolicy,
        entityMappings: List<EntityMapping>,
        selectedDuplicateRowIndices: Set<Int>
    ): Result<ImportSummary> {
        val profileId = settingsDataStore.currentProfileId.first()
        val profileBaseCurrency = profileDao.getProfileById(profileId)?.baseCurrency ?: "INR"
        val (_, rows) = parseBudgetBakersRows(inputStream)

        val existingAccountEntities = accountDao.getAllAccountsSync().associateBy { it.name.lowercase() }
        val existingCategoryEntities = categoryDao.getAllCategoriesSync()
        val existingTxs = transactionDao.getAllTransactionsSync()
        val existingKeys = existingTxs.map { tx ->
            "${tx.timestamp / 60000}|${tx.amount.abs().toPlainString()}|${tx.note}"
        }.toSet()

        val accountMap = existingAccountEntities.mapValues { it.value.id }.toMutableMap()
        val categoryMap = existingCategoryEntities.associate { it.name.lowercase() to it.id }.toMutableMap()

        // Build a mutable list of known tags for in-session tag creation
        val existingTagsList = tagDao.getAllTagsSync().toMutableList()

        var imported = 0
        var skipped = 0
        val errors = mutableListOf<ParseError>()

        database.withTransaction {
            // Apply entity mappings (account / category decisions from the mapping step)
            entityMappings.forEach { em ->
                when (em.entityType) {
                    EntityMapping.EntityType.ACCOUNT -> {
                        val id = em.existingId ?: UUID.randomUUID().toString()
                        if (em.existingId == null) {
                            accountDao.upsertAccount(
                                AccountEntity(
                                    id = id,
                                    name = em.unknownName,
                                    currencyCode = profileBaseCurrency,
                                    structureType = "regular",
                                    colorHex = "#607D8B",
                                    icon = "wallet",
                                    allowedCurrencies = profileBaseCurrency,
                                    profileId = profileId,
                                    isArchived = false,
                                    displayOrder = 999
                                )
                            )
                        }
                        accountMap[em.unknownName.lowercase()] = id
                    }
                    EntityMapping.EntityType.CATEGORY -> {
                        val mapId = em.existingId?.toLongOrNull() ?: 1000L
                        categoryMap[em.unknownName.lowercase()] = mapId
                    }
                }
            }

            // ──────────────────────────────────────────────────────────────────
            // Separate transfer rows from regular rows
            // ──────────────────────────────────────────────────────────────────

            data class IndexedRow(val idx: Int, val row: Map<String, String>)

            val transferRows = mutableListOf<IndexedRow>()
            val regularRows = mutableListOf<IndexedRow>()

            rows.forEachIndexed { idx, row ->
                if (isTransferRow(row)) {
                    transferRows.add(IndexedRow(idx, row))
                } else {
                    regularRows.add(IndexedRow(idx, row))
                }
            }

            // ──────────────────────────────────────────────────────────────────
            // Group transfer rows by pair key
            // ──────────────────────────────────────────────────────────────────

            data class ParsedTransfer(
                val idx: Int,
                val row: Map<String, String>,
                val accountName: String,
                val timestamp: Long,
                val note: String,
                val baseAmount: BigDecimal,
                val currency: String,
                val paymentMethod: PaymentMethod,
                val warrantyExpiration: Long?,
                val tagNames: Set<String>
            )

            val parsedTransfers = transferRows.mapNotNull { (idx, row) ->
                try {
                    val accountName = row["account"] ?: return@mapNotNull null
                    val dateStr = row["date"] ?: ""
                    val note = row["note"] ?: ""
                    val baseAmount = resolveBaseAmount(row)
                    val timestamp = parseDate(dateStr)
                    val currency = row["currency"] ?: "INR"
                    val paymentMethod = resolvePaymentMethod(row)
                    val warrantyMonths = row["warranty_in_month"]?.toIntOrNull() ?: 0
                    val warrantyExpiration = if (warrantyMonths > 0)
                        timestamp + (warrantyMonths * 30L * 24 * 60 * 60 * 1000) else null
                    val tagNames = parseTagNames(row)
                    ParsedTransfer(idx, row, accountName, timestamp, note, baseAmount, currency, paymentMethod, warrantyExpiration, tagNames)
                } catch (e: Exception) {
                    errors.add(ParseError(idx, e.message ?: "Transfer parse error"))
                    null
                }
            }

            // Group by pair key
            val pairGroups = parsedTransfers.groupBy { t ->
                transferPairKey(t.timestamp, t.note, t.baseAmount.abs())
            }

            pairGroups.forEach { (_, group) ->
                if (group.size == 2) {
                    // Paired transfer: identify source (negative amount) vs target (positive amount)
                    // using the signed `amount` column; ref_currency_amount is always positive.
                    val sourceEntry = group.firstOrNull { entry ->
                        (entry.row["amount"]?.replace(",", "")?.toBigDecimalOrNull()?.signum() ?: 1) < 0
                    } ?: group[0]
                    val targetEntry = group.firstOrNull { it != sourceEntry } ?: group[1]

                    val sourceAccountId = accountMap[sourceEntry.accountName.lowercase()]
                    val targetAccountId = accountMap[targetEntry.accountName.lowercase()]

                    if (sourceAccountId == null || targetAccountId == null) {
                        skipped += 2
                        return@forEach
                    }

                    val absAmount = sourceEntry.baseAmount.abs()
                    val dupKey = "${sourceEntry.timestamp / 60000}|${absAmount.toPlainString()}|${sourceEntry.note}"
                    if (dupKey in existingKeys) {
                        if (!handleDuplicate(duplicatePolicy, sourceEntry.idx, selectedDuplicateRowIndices)) {
                            skipped += 2
                            return@forEach
                        }
                    }

                    val tagIds = resolveTagIds(sourceEntry.tagNames + targetEntry.tagNames, existingTagsList)

                    val transferRate = if (sourceEntry.currency != targetEntry.currency &&
                        sourceEntry.baseAmount.compareTo(BigDecimal.ZERO) != 0) {
                        targetEntry.baseAmount.abs().divide(sourceEntry.baseAmount.abs(), 8, RoundingMode.HALF_UP)
                    } else BigDecimal.ONE

                    transactionDao.insertTransaction(
                        TransactionEntity(
                            id = 0,
                            name = sourceEntry.note.ifBlank { "Transfer" }.take(50),
                            amount = absAmount,
                            timestamp = sourceEntry.timestamp,
                            currency = sourceEntry.currency,
                            transactionType = TransactionType.TRANSFER,
                            accountId = sourceAccountId,
                            categoryId = 1000L, // system OTHERS — transfers have no meaningful category
                            tags = tagIds,
                            targetAccountId = targetAccountId,
                            targetCurrency = targetEntry.currency,
                            transferRate = transferRate,
                            paymentMethod = sourceEntry.paymentMethod,
                            payee = sourceEntry.row["payee"] ?: "",
                            note = sourceEntry.note,
                            sourcePlannedPaymentId = null,
                            profileId = profileId,
                            gpsLocation = buildGpsLocation(sourceEntry.row),
                            location = sourceEntry.row["payee"]?.takeIf { it.isNotBlank() },
                            warrantyExpiration = sourceEntry.warrantyExpiration,
                            status = TransactionStatus.CLEARED
                        )
                    )
                    imported++
                } else {
                    // Unpaired transfer — import as EXPENSE/INCOME with "[outside wallet]" note
                    group.forEach { t ->
                        val accountId = accountMap[t.accountName.lowercase()]
                        if (accountId == null) { skipped++; return@forEach }

                        val absAmount = t.baseAmount.abs()
                        val txType = if (t.baseAmount < BigDecimal.ZERO) TransactionType.EXPENSE else TransactionType.INCOME
                        val finalNote = if (t.note.isBlank()) "[outside wallet]" else "${t.note} [outside wallet]"
                        val finalAmount = if (txType == TransactionType.EXPENSE) absAmount.negate() else absAmount

                        val tagIds = resolveTagIds(t.tagNames, existingTagsList)

                        transactionDao.insertTransaction(
                            TransactionEntity(
                                id = 0,
                                name = t.note.ifBlank { if (txType == TransactionType.INCOME) "Income" else "Expense" }.take(50),
                                amount = finalAmount,
                                timestamp = t.timestamp,
                                currency = t.currency,
                                transactionType = txType,
                                accountId = accountId,
                                categoryId = 1000L,
                                tags = tagIds,
                                targetAccountId = null,
                                targetCurrency = null,
                                transferRate = BigDecimal.ONE,
                                paymentMethod = t.paymentMethod,
                                payee = t.row["payee"] ?: "",
                                note = finalNote,
                                sourcePlannedPaymentId = null,
                                profileId = profileId,
                                gpsLocation = buildGpsLocation(t.row),
                                location = null,
                                warrantyExpiration = t.warrantyExpiration,
                                status = TransactionStatus.CLEARED
                            )
                        )
                        imported++
                    }
                }
            }

            // ──────────────────────────────────────────────────────────────────
            // Regular (non-transfer) rows
            // ──────────────────────────────────────────────────────────────────

            regularRows.forEachIndexed { listIdx, (idx, row) ->
                try {
                    val accountName = row["account"] ?: ""
                    val dateStr = row["date"] ?: ""
                    val note = row["note"] ?: ""
                    val payee = row["payee"] ?: ""
                    val baseAmount = resolveBaseAmount(row)
                    val timestamp = parseDate(dateStr)
                    val currency = row["currency"] ?: "INR"
                    val txType = detectTransactionType(row)
                    val paymentMethod = resolvePaymentMethod(row)
                    val warrantyMonths = row["warranty_in_month"]?.toIntOrNull() ?: 0
                    val warrantyExpiration = if (warrantyMonths > 0)
                        timestamp + (warrantyMonths * 30L * 24 * 60 * 60 * 1000) else null
                    val tagNames = parseTagNames(row)

                    val dupKey = "${timestamp / 60000}|${baseAmount.abs().toPlainString()}|$note"
                    if (dupKey in existingKeys) {
                        if (!handleDuplicate(duplicatePolicy, idx, selectedDuplicateRowIndices)) {
                            skipped++
                            return@forEachIndexed
                        }
                    }

                    val accountId = accountMap[accountName.lowercase()]
                    if (accountId == null) { skipped++; return@forEachIndexed }

                    val categoryId = resolveCategory(row, existingCategoryEntities)
                        ?: run {
                            val displayName = unresolvedCategoryName(row)
                            categoryMap[displayName.lowercase()] ?: 1000L
                        }

                    val finalAmount = when (txType) {
                        TransactionType.EXPENSE -> baseAmount.abs().negate()
                        TransactionType.INCOME -> baseAmount.abs()
                        TransactionType.TRANSFER -> baseAmount.abs()
                    }

                    val tagIds = resolveTagIds(tagNames, existingTagsList)

                    transactionDao.insertTransaction(
                        TransactionEntity(
                            id = 0,
                            name = (row["category"] ?: note).ifBlank { "Transaction" }.take(50),
                            amount = finalAmount,
                            timestamp = timestamp,
                            currency = currency,
                            transactionType = txType,
                            accountId = accountId,
                            categoryId = categoryId,
                            tags = tagIds,
                            targetAccountId = null,
                            targetCurrency = null,
                            transferRate = BigDecimal.ONE,
                            paymentMethod = paymentMethod,
                            payee = payee,
                            note = note,
                            sourcePlannedPaymentId = null,
                            profileId = profileId,
                            gpsLocation = buildGpsLocation(row),
                            location = null,
                            warrantyExpiration = warrantyExpiration,
                            status = TransactionStatus.CLEARED
                        )
                    )
                    imported++
                } catch (e: Exception) {
                    errors.add(ParseError(idx, e.message ?: "Row error"))
                }
            }
        }

        return Result.success(ImportSummary(imported, skipped, errors))
    }

    // ──────────────────────────────────────────────────────────────────────────
    // Generic CSV import (unchanged logic, adapted to use new ImportPreview type)
    // ──────────────────────────────────────────────────────────────────────────

    private suspend fun executeGenericImport(
        session: CsvImportSession,
        inputStream: InputStream,
        duplicatePolicy: DuplicatePolicy,
        entityMappings: List<EntityMapping>,
        selectedDuplicateRowIndices: Set<Int>
    ): Result<ImportSummary> {
        val profileId = settingsDataStore.currentProfileId.first()
        val profileBaseCurrency = profileDao.getProfileById(profileId)?.baseCurrency ?: "INR"
        val lines = inputStream.bufferedReader().readLines().drop(1)
        val existingAccounts = accountDao.getAllAccountsSync().associateBy { it.name.lowercase() }
        val existingCategories = categoryDao.getAllCategoriesSync().associateBy { it.name.lowercase() }
        val existingTxs = transactionDao.getAllTransactionsSync()
        val existingKeys = existingTxs.map { tx ->
            "${tx.timestamp / 60000}|${tx.amount.abs().toPlainString()}|${tx.note}"
        }.toSet()

        val accountMap = existingAccounts.mapValues { it.value.id }.toMutableMap()
        val categoryMap = existingCategories.mapValues { it.value.id }.toMutableMap()

        var imported = 0
        var skipped = 0
        val errors = mutableListOf<ParseError>()

        database.withTransaction {
            entityMappings.forEach { em ->
                when (em.entityType) {
                    EntityMapping.EntityType.ACCOUNT -> {
                        val id = em.existingId ?: UUID.randomUUID().toString()
                        if (em.existingId == null) {
                            accountDao.upsertAccount(
                                AccountEntity(
                                    id = id, name = em.unknownName,
                                    currencyCode = profileBaseCurrency, structureType = "regular",
                                    colorHex = "#607D8B", icon = "wallet",
                                    allowedCurrencies = profileBaseCurrency,
                                    profileId = profileId, isArchived = false, displayOrder = 999
                                )
                            )
                        }
                        accountMap[em.unknownName.lowercase()] = id
                    }
                    EntityMapping.EntityType.CATEGORY -> {
                        val mapId = em.existingId?.toLongOrNull() ?: 1000L
                        categoryMap[em.unknownName.lowercase()] = mapId
                    }
                }
            }

            lines.forEachIndexed { idx, line ->
                try {
                    val cols = line.split(session.delimiter).map { it.trim().replace("\"", "") }
                    val accountName = session.mapping[OssWalletField.ACCOUNT]?.let { cols.getOrNull(it) } ?: ""
                    val categoryName = session.mapping[OssWalletField.CATEGORY]?.let { cols.getOrNull(it) } ?: ""
                    val amountStr = session.mapping[OssWalletField.AMOUNT]?.let { cols.getOrNull(it) } ?: "0"
                    val dateStr = session.mapping[OssWalletField.DATE]?.let { cols.getOrNull(it) } ?: ""
                    val note = session.mapping[OssWalletField.NOTE]?.let { cols.getOrNull(it) } ?: ""
                    val payee = session.mapping[OssWalletField.PAYEE]?.let { cols.getOrNull(it) } ?: ""
                    val typeStr = session.mapping[OssWalletField.TYPE]?.let { cols.getOrNull(it) } ?: "Expenses"
                    val paymentTypeStr = session.mapping[OssWalletField.PAYMENT_TYPE]?.let { cols.getOrNull(it) } ?: ""
                    val labelsStr = session.mapping[OssWalletField.LABELS]?.let { cols.getOrNull(it) } ?: ""
                    val warrantyStr = session.mapping[OssWalletField.WARRANTY_MONTHS]?.let { cols.getOrNull(it) } ?: "0"
                    val isTransferStr = session.mapping[OssWalletField.IS_TRANSFER]?.let { cols.getOrNull(it) } ?: "false"

                    val amount = amountStr.replace(",", "").toBigDecimalOrNull() ?: BigDecimal.ZERO
                    val timestamp = parseDate(dateStr)

                    val dupKey = "${timestamp / 60000}|${amount.abs().toPlainString()}|$note"
                    if (dupKey in existingKeys) {
                        if (!handleDuplicate(duplicatePolicy, idx, selectedDuplicateRowIndices)) {
                            skipped++; return@forEachIndexed
                        }
                    }

                    val accountId = accountMap[accountName.lowercase()]
                    if (accountId == null) { skipped++; return@forEachIndexed }

                    val categoryId = categoryMap[categoryName.lowercase()] ?: 1000L

                    val txType = when {
                        isTransferStr.equals("true", ignoreCase = true) -> TransactionType.TRANSFER
                        typeStr.equals("income", ignoreCase = true) -> TransactionType.INCOME
                        else -> TransactionType.EXPENSE
                    }

                    val finalAmount = if (txType == TransactionType.EXPENSE) amount.abs().negate() else amount.abs()

                    val paymentMethod = when (paymentTypeStr.uppercase().replace(" ", "_")) {
                        "MOBILE_PAYMENT" -> PaymentMethod.MOBILE_PAYMENT
                        "CREDIT_CARD" -> PaymentMethod.CREDIT_CARD
                        "TRANSFER", "BANK_TRANSFER" -> PaymentMethod.BANK_TRANSFER
                        "CASH" -> PaymentMethod.CASH
                        else -> PaymentMethod.MOBILE_PAYMENT
                    }

                    val tags = if (labelsStr.isNotBlank())
                        labelsStr.split("|").map { it.trim() }.filter { it.isNotBlank() }.toSet()
                    else emptySet()

                    val warrantyMonths = warrantyStr.toIntOrNull() ?: 0
                    val warrantyExpiration = if (warrantyMonths > 0)
                        timestamp + (warrantyMonths * 30L * 24 * 60 * 60 * 1000)
                    else null

                    transactionDao.insertTransaction(
                        TransactionEntity(
                            id = 0,
                            name = categoryName.ifBlank { note.take(30) },
                            amount = finalAmount,
                            timestamp = timestamp,
                            currency = profileBaseCurrency,
                            transactionType = txType,
                            accountId = accountId,
                            categoryId = categoryId,
                            tags = tags,
                            targetAccountId = null,
                            targetCurrency = null,
                            transferRate = BigDecimal.ONE,
                            paymentMethod = paymentMethod,
                            payee = payee,
                            note = note,
                            sourcePlannedPaymentId = null,
                            profileId = profileId,
                            gpsLocation = null,
                            location = null,
                            warrantyExpiration = warrantyExpiration,
                            status = TransactionStatus.CLEARED
                        )
                    )
                    imported++
                } catch (e: Exception) {
                    errors.add(ParseError(idx, e.message ?: "Row error"))
                }
            }
        }

        return Result.success(ImportSummary(imported, skipped, errors))
    }

    // ──────────────────────────────────────────────────────────────────────────
    // Helpers
    // ──────────────────────────────────────────────────────────────────────────

    private fun resolvePaymentMethod(row: Map<String, String>): PaymentMethod {
        val bbType = row["payment_type"] ?: ""
        val ossName = assetLoader.paymentTypeMap[bbType] ?: "MOBILE_PAYMENT"
        return PaymentMethod.fromString(ossName)
    }

    private fun parseTagNames(row: Map<String, String>): Set<String> {
        val labelsStr = row["labels"] ?: ""
        return labelsStr.split("|", ";")
            .map { it.trim() }
            .filter { it.isNotBlank() }
            .toSet()
    }

    /**
     * Resolve a set of label names: ensures each name exists as a TagEntity row (for the tags
     * management screen), and returns the normalized tag names to store on TransactionEntity.tags.
     *
     * TransactionEntity.tags stores tag names (Set<String>), NOT tag IDs.
     * [existingTagsList] is mutated in-place so that the same tag is not double-inserted
     * within a single import transaction.
     */
    private suspend fun resolveTagIds(
        names: Set<String>,
        existingTagsList: MutableList<TagEntity>
    ): Set<String> {
        val resolvedNames = mutableSetOf<String>()
        for (name in names) {
            val existing = existingTagsList.find { it.name.equals(name, ignoreCase = true) }
            if (existing != null) {
                resolvedNames.add(existing.name)
            } else {
                val newTag = TagEntity(id = 0, name = name, colorHex = DEFAULT_TAG_COLOR)
                val newId = tagDao.insertTag(newTag)
                if (newId > 0) {
                    existingTagsList.add(newTag.copy(id = newId))
                }
                resolvedNames.add(name)
            }
        }
        return resolvedNames
    }

    private fun buildGpsLocation(row: Map<String, String>): String? {
        val lat = row["gps_latitude"]?.toDoubleOrNull()
        val lng = row["gps_longitude"]?.toDoubleOrNull()
        return if (lat != null && lng != null) "$lat,$lng" else null
    }

    /**
     * Returns true when a duplicate row should be imported (based on policy and user selections).
     */
    private fun handleDuplicate(
        policy: DuplicatePolicy,
        rowIdx: Int,
        selectedIndices: Set<Int>
    ): Boolean = when (policy) {
        DuplicatePolicy.SKIP -> false
        DuplicatePolicy.IMPORT_BOTH -> true
        DuplicatePolicy.REPLACE_EXISTING -> rowIdx in selectedIndices
    }

    companion object {
        /** Default tag color (grey) stored as an Int, matching TagEntity.colorHex: Int */
        private const val DEFAULT_TAG_COLOR: Int = 0xFF9E9E9E.toInt()
    }
}
