package com.osscabinet.ossWallet.data.service

import androidx.room.withTransaction
import com.osscabinet.ossWallet.core.util.DispatcherProvider
import com.osscabinet.ossWallet.data.local.AppDatabase
import com.osscabinet.ossWallet.data.local.SettingsDataStore
import com.osscabinet.ossWallet.data.local.dao.AccountDao
import com.osscabinet.ossWallet.data.local.dao.CategoryDao
import com.osscabinet.ossWallet.data.local.dao.ProfileDao
import com.osscabinet.ossWallet.data.local.dao.TagDao
import com.osscabinet.ossWallet.data.local.dao.TransactionDao
import com.osscabinet.ossWallet.data.local.dao.TransactionLinkDao
import com.osscabinet.ossWallet.data.local.entity.AccountEntity
import com.osscabinet.ossWallet.data.local.entity.CategoryEntity
import com.osscabinet.ossWallet.data.local.entity.TagEntity
import com.osscabinet.ossWallet.data.local.entity.TransactionEntity
import com.osscabinet.ossWallet.data.local.entity.TransactionLinkEntity
import com.osscabinet.ossWallet.domain.model.Category
import com.osscabinet.ossWallet.domain.model.PaymentMethod
import com.osscabinet.ossWallet.domain.model.TransactionStatus
import com.osscabinet.ossWallet.domain.model.TransactionType
import com.osscabinet.ossWallet.domain.model.import_csv.AccountToMap
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
import jakarta.inject.Inject
import jakarta.inject.Singleton

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
    // Date parsing helpers
    // ──────────────────────────────────────────────────────────────────────────

    private val dateFormats = listOf(
        SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS", Locale.US),
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
    private suspend fun <T> parseBudgetBakersRows(inputStream: InputStream, block: suspend (List<String>, Sequence<Map<String, String>>) -> T): T {
        val format = buildBudgetBakersCsvFormat()
        return CSVParser.parse(inputStream.bufferedReader(Charsets.UTF_8), format).use { parser ->
            val iterator = parser.iterator()
            if (!iterator.hasNext()) return@use block(emptyList(), emptySequence())

            val headers = iterator.next().toList().map { it.trim().lowercase() }
            val rows = iterator.asSequence().map { record ->
                headers.mapIndexed { i, header -> header to (record.get(i).ifEmpty { "" }) }.toMap()
            }
            block(headers, rows)
        }
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
        isTransferRow(row) -> TransactionType.TRANSFER_OUTFLOW
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
    private fun resolveBaseAmount(row: Map<String, String>, delimiter: Char = ','): BigDecimal {
        val ref = normalizeAmount(row["ref_currency_amount"], delimiter)
        val raw = normalizeAmount(row["amount"], delimiter)
        return if (ref.signum() != 0) ref else raw
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
        val mappedName = envelopeId?.let { assetLoader.envelopeMap[it]?.name }
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
        val mappedName = envelopeId?.let { assetLoader.envelopeMap[it]?.name }
        val categoryText = row["category"] ?: ""
        return when {
            !mappedName.isNullOrBlank() && mappedName != "TRANSFER" -> mappedName
            categoryText.isNotBlank() -> categoryText
            else -> "Unknown"
        }
    }

    private fun suggestedParentName(row: Map<String, String>): String? {
        val envelopeId = row["envelope_id"]?.toIntOrNull()
        return envelopeId?.let { assetLoader.envelopeMap[it]?.parentName }
    }

    // ──────────────────────────────────────────────────────────────────────────
    // parseHeaders — sniff delimiter, parse headers; sourceApp is chosen by the user
    // ──────────────────────────────────────────────────────────────────────────

    override suspend fun parseHeaders(inputStream: InputStream): CsvImportSession =
        withContext(dispatcherProvider.io) {
            val reader = inputStream.bufferedReader()
            // Read a sample for delimiter sniffing
            val sampleSize = 2048
            reader.mark(sampleSize + 100)
            val buffer = CharArray(sampleSize)
            val charsRead = reader.read(buffer)
            val sample = if (charsRead > 0) String(buffer, 0, charsRead) else ""
            reader.reset()

            val delimiter = if (sample.contains(';')) ';' else ','

            val format = CSVFormat.DEFAULT.builder()
                .setDelimiter(delimiter)
                .setQuote('"')
                .setIgnoreEmptyLines(true)
                .setTrim(true)
                .build()

            val parser = CSVParser.parse(reader, format)
            val records = parser.records
            if (records.isEmpty()) return@withContext CsvImportSession(emptyList(), emptyList(), emptyMap(), null, delimiter)

            val headers = records.first().toList().map { it.trim().lowercase() }
            val previewRows = records.drop(1).take(5).map { it.toList() }

            // Detect source app if possible
            val detectedApp = if (headers.toSet().containsAll(budgetBakersHeaders)) {
                ImportSourceApp.BUDGET_BAKERS
            } else {
                null
            }

            val mapping = buildInitialMapping(headers, detectedApp ?: ImportSourceApp.GENERIC)

            CsvImportSession(headers, previewRows, mapping, detectedApp, delimiter)
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
            ImportSourceApp.CASHEW -> mapOf(
                OssWalletField.DATE      to headers.indexOf("date").takeIf { it >= 0 },
                OssWalletField.ACCOUNT   to headers.indexOf("account").takeIf { it >= 0 },
                OssWalletField.CATEGORY  to headers.indexOf("category name").takeIf { it >= 0 },
                OssWalletField.AMOUNT    to headers.indexOf("amount").takeIf { it >= 0 },
                OssWalletField.TYPE      to headers.indexOf("income").takeIf { it >= 0 },
                OssWalletField.NOTE      to headers.indexOf("note").takeIf { it >= 0 },
                OssWalletField.PAYEE     to null,
                OssWalletField.PAYMENT_TYPE to null,
                OssWalletField.LABELS    to null,
                OssWalletField.WARRANTY_MONTHS to null,
                OssWalletField.IS_TRANSFER to null
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
    ): ImportPreview = withContext(dispatcherProvider.io) {
        when (session.sourceApp) {
            ImportSourceApp.BUDGET_BAKERS -> generateBudgetBakersPreview(session, inputStream)
            ImportSourceApp.CASHEW -> generateCashewPreview(session, inputStream)
            else -> generateGenericPreview(session, inputStream)
        }
    }

    private suspend fun generateBudgetBakersPreview(
        session: CsvImportSession,
        inputStream: InputStream
    ): ImportPreview = parseBudgetBakersRows(inputStream) { _, rows ->
        // ARC-15: preview must use the active profile's existing data, not every profile's.
        val profileId = settingsDataStore.currentProfileId.first()
        val existingTxs = transactionDao.getAllTransactionsSync(profileId)
        val existingAccountsList = accountDao.getAllAccountsSync(profileId)
        val existingAccountsByName = existingAccountsList.associateBy { it.name.lowercase() }
        val existingCategories = categoryDao.getAllCategoriesSync()

        val existingKeys = existingTxs.map { tx ->
            "${tx.timestamp / 60000}|${tx.amount.abs().toPlainString()}|${tx.note}"
        }.toSet()

        // Collect all unique account names and their row counts/detected currencies
        val accountRowCounts = mutableMapOf<String, Int>()
        val accountDetectedCurrencies = mutableMapOf<String, MutableSet<String>>()

        // key = category name (after mapping), to deduplicate
        val newCategoryMap = mutableMapOf<String, CategoryToMap>()
        val duplicates = mutableListOf<DuplicateGroup>()
        val parseErrors = mutableListOf<ParseError>()
        var validRows = 0
        var totalRows = 0

        // Transfer pairing: group transfer rows by pair key to detect unpaired rows
        val transferGroups = mutableMapOf<String, MutableList<Int>>() // pairKey -> list of row indices

        rows.forEachIndexed { idx, row ->
            totalRows++
            try {
                val accountName = row["account"] ?: ""
                val dateStr = row["date"] ?: ""
                val note = row["note"] ?: ""
                val currency = row["currency"] ?: ""
                val baseAmount = resolveBaseAmount(row, ';')
                val timestamp = parseDate(dateStr)
                val isTransfer = isTransferRow(row)

                if (accountName.isNotBlank()) {
                    accountRowCounts[accountName] = (accountRowCounts[accountName] ?: 0) + 1
                    if (currency.isNotBlank()) {
                        accountDetectedCurrencies.getOrPut(accountName) { mutableSetOf() }.add(currency.uppercase())
                    }
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
                                isCustomCategory = isCustom,
                                suggestedParentName = suggestedParentName(row)
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
                    if (duplicates.size < 500) {
                        duplicates.add(
                            DuplicateGroup(
                                key = dupKey,
                                existingNote = note,
                                incomingRowIndex = idx,
                                incomingValues = row.values.toList()
                            )
                        )
                    }
                } else {
                    validRows++
                }
            } catch (e: Exception) {
                parseErrors.add(ParseError(idx, e.message ?: "Parse error"))
            }
        }

        if (totalRows == 0) {
            return@parseBudgetBakersRows ImportPreview(0, 0, emptyList(), emptyList(), emptyList(), emptyList(), 0)
        }

        // Count unpaired transfer rows (groups with only one entry)
        val unpairedTransferRows = transferGroups.values.count { it.size == 1 }

        val allCsvAccounts = accountRowCounts.map { (name, count) ->
            val match = existingAccountsByName[name.lowercase()]
            AccountToMap(
                name = name,
                suggestedExistingId = match?.id,
                suggestedExistingName = match?.name,
                rowCount = count,
                hasDetectedCurrency = accountDetectedCurrencies[name]?.isNotEmpty() == true
            )
        }.sortedBy { it.name }

        ImportPreview(
            totalRows = totalRows,
            validRows = validRows,
            duplicates = duplicates,
            allCsvAccounts = allCsvAccounts,
            newCategories = newCategoryMap.values.toList(),
            parseErrors = parseErrors,
            unpairedTransferRows = unpairedTransferRows
        )
    }

    private suspend fun generateGenericPreview(
        session: CsvImportSession,
        inputStream: InputStream
    ): ImportPreview = withContext(dispatcherProvider.io) {
        // ARC-15: scope existence checks to the active profile.
        val profileId = settingsDataStore.currentProfileId.first()
        val existingTxs = transactionDao.getAllTransactionsSync(profileId)
        val existingAccounts = accountDao.getAllAccountsSync(profileId).map { it.name.lowercase() }.toSet()
        val existingCategories = categoryDao.getAllCategoriesSync().map { it.name.lowercase() }.toSet()

        val newAccounts = mutableSetOf<String>()
        val newCategories = mutableSetOf<String>()
        val duplicates = mutableListOf<DuplicateGroup>()
        val parseErrors = mutableListOf<ParseError>()
        var validRows = 0
        var totalCount = 0

        val existingKeys = existingTxs.map { tx ->
            "${tx.timestamp / 60000}|${tx.amount.abs().toPlainString()}|${tx.note}"
        }.toSet()

        val genericFormat = CSVFormat.DEFAULT.builder()
            .setDelimiter(session.delimiter)
            .setQuote('"')
            .setIgnoreEmptyLines(true)
            .setTrim(true)
            .build()

        CSVParser.parse(inputStream.bufferedReader(Charsets.UTF_8), genericFormat).use { parser ->
            val iterator = parser.iterator()
            if (!iterator.hasNext()) return@use ImportPreview(0, 0, emptyList(), emptyList(), emptyList(), emptyList())
            
            iterator.next() // Skip header

            iterator.forEachRemaining { record ->
                val idx = totalCount
                totalCount++
                try {
                    val cols = record.toList()
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

                    val amount = normalizeAmount(amountStr, session.delimiter)
                    val timestamp = parseDate(dateStr)

                    val dupKey = "${timestamp / 60000}|${amount.abs().toPlainString()}|$note"
                    if (dupKey in existingKeys) {
                        if (duplicates.size < 500) {
                            duplicates.add(
                                DuplicateGroup(
                                    key = dupKey,
                                    existingNote = note,
                                    incomingRowIndex = idx,
                                    incomingValues = cols
                                )
                            )
                        }
                    } else {
                        validRows++
                    }
                } catch (e: Exception) {
                    if (parseErrors.size < 100) {
                        parseErrors.add(ParseError(idx, e.message ?: "Parse error"))
                    }
                }
            }

            ImportPreview(
                totalRows = totalCount,
                validRows = validRows,
                duplicates = duplicates,
                allCsvAccounts = newAccounts.map { name ->
                    AccountToMap(
                        name = name,
                        suggestedExistingId = null,
                        suggestedExistingName = null,
                        hasDetectedCurrency = session.headers.any { it.contains("currency", ignoreCase = true) }
                    )
                },
                newCategories = newCategories.map { CategoryToMap(name = it) },
                parseErrors = parseErrors,
                unpairedTransferRows = 0
            )
        }
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
    ): Result<ImportSummary> = withContext(dispatcherProvider.io) {
        try {
            when (session.sourceApp) {
                ImportSourceApp.BUDGET_BAKERS -> executeBudgetBakersImport(session, inputStream, duplicatePolicy, entityMappings, selectedDuplicateRowIndices)
                ImportSourceApp.CASHEW -> executeCashewImport(session, inputStream, duplicatePolicy, entityMappings, selectedDuplicateRowIndices)
                else -> executeGenericImport(session, inputStream, duplicatePolicy, entityMappings, selectedDuplicateRowIndices)
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
    ): Result<ImportSummary> = parseBudgetBakersRows(inputStream) { _, rows ->
        val profileId = settingsDataStore.currentProfileId.first()
        val profileBaseCurrency = profileDao.getProfileById(profileId)?.baseCurrency ?: "INR"

        // ARC-15: profile-scoped reads so the import doesn't dedupe against another profile's data.
        val existingAccountEntities = accountDao.getAllAccountsSync(profileId).associateBy { it.name.lowercase() }
        val existingCategoryEntities = categoryDao.getAllCategoriesSync()
        val existingTxs = transactionDao.getAllTransactionsSync(profileId)
        val existingKeys = existingTxs.map { tx ->
            "${tx.timestamp / 60000}|${tx.amount.abs().toPlainString()}|${tx.note}"
        }.toSet()

        val accountMap = existingAccountEntities.mapValues { it.value.id }.toMutableMap()
        val categoryMap = existingCategoryEntities.associate { it.name.lowercase() to it.id }.toMutableMap()

        // Build a mutable list of known tags for in-session tag creation
        val existingTagsList = tagDao.getAllTagsSync().toMutableList()

        var imported = 0
        var skipped = 0
        var transfersRerouted = 0
        val errors = mutableListOf<ParseError>()

        val excludedAccountNames = entityMappings
            .filter { it.entityType == EntityMapping.EntityType.ACCOUNT && it.isExcluded }
            .map { it.unknownName.lowercase() }
            .toSet()

        // Track colors of newly created root categories so subcategories can inherit them
        val newCategoryColorMap = mutableMapOf<Long, Int>()

        // Sort: accounts first, then root categories (no parent), then subcategories
        val sortedMappings = entityMappings.sortedWith(compareBy { em ->
            when {
                em.entityType == EntityMapping.EntityType.ACCOUNT -> -1
                em.parentCategoryId == null && em.parentCategoryName == null -> 0
                else -> 1
            }
        })

        database.withTransaction {
            // Apply entity mappings (account / category decisions from the mapping step)
            sortedMappings.forEach { em ->
                when (em.entityType) {
                    EntityMapping.EntityType.ACCOUNT -> {
                        if (em.isExcluded) return@forEach
                        val id = em.existingId ?: UUID.randomUUID().toString()
                        if (em.existingId == null) {
                            val conflictExists = existingAccountEntities.containsKey(em.unknownName.lowercase())
                            val finalName = if (conflictExists) "${em.unknownName}-${randomAlphanumSuffix()}" else em.unknownName
                            val currency = em.currencyCode ?: profileBaseCurrency
                            accountDao.upsertAccount(
                                AccountEntity(
                                    id = id,
                                    name = finalName,
                                    currencyCode = currency,
                                    structureType = "regular",
                                    colorHex = BRAND_ACCOUNT_COLORS.random(),
                                    icon = "wallet",
                                    allowedCurrencies = currency,
                                    profileId = profileId,
                                    isArchived = false,
                                    displayOrder = 999
                                )
                            )
                        }
                        accountMap[em.unknownName.lowercase()] = id
                    }
                    EntityMapping.EntityType.CATEGORY -> {
                        val existingId = em.existingId
                        if (existingId != null) {
                            categoryMap[em.unknownName.lowercase()] = existingId.toLongOrNull() ?: Category.SYSTEM_OTHERS
                        } else {
                            val resolvedParentId = em.parentCategoryId
                                ?: em.parentCategoryName?.let { name ->
                                    categoryMap[name.lowercase()]
                                        ?: existingCategoryEntities.find { it.name.equals(name, ignoreCase = true) }?.id
                                }
                            val catColor = when {
                                resolvedParentId == null -> BRAND_CATEGORY_COLORS.random()
                                newCategoryColorMap.containsKey(resolvedParentId) -> newCategoryColorMap[resolvedParentId]!!
                                else -> existingCategoryEntities.find { it.id == resolvedParentId }?.colorHex
                                    ?: BRAND_CATEGORY_COLORS.random()
                            }
                            categoryDao.insertCategory(
                                CategoryEntity(
                                    id = 0,
                                    name = em.unknownName,
                                    iconName = "Category",
                                    colorHex = catColor,
                                    isDefault = false,
                                    parentId = resolvedParentId
                                )
                            )
                            val inserted = categoryDao.getCategoryByName(em.unknownName)
                            val insertedId = inserted?.id ?: Category.SYSTEM_OTHERS
                            categoryMap[em.unknownName.lowercase()] = insertedId
                            if (resolvedParentId == null) newCategoryColorMap[insertedId] = catColor
                        }
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
                    val baseAmount = resolveBaseAmount(row, ';')
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
                    val sourceEntry = group.firstOrNull { entry ->
                        normalizeAmount(entry.row["amount"], ';').signum() < 0
                    } ?: group[0]
                    val targetEntry = group.firstOrNull { it != sourceEntry } ?: group[1]

                    val sourceExcluded = sourceEntry.accountName.lowercase() in excludedAccountNames
                    val targetExcluded = targetEntry.accountName.lowercase() in excludedAccountNames

                    when {
                        sourceExcluded && targetExcluded -> {
                            skipped += 2
                            return@forEach
                        }
                        sourceExcluded -> {
                            // Keep target side only → outside-wallet inflow
                            val targetAccountId = accountMap[targetEntry.accountName.lowercase()]
                            if (targetAccountId == null) { skipped++; return@forEach }
                            val tagIds = resolveTagIds(targetEntry.tagNames, existingTagsList)
                            transactionDao.insertTransaction(TransactionEntity(
                                id = 0,
                                name = targetEntry.note.ifBlank { "Transfer" }.take(50),
                                amount = targetEntry.baseAmount.abs(),
                                timestamp = targetEntry.timestamp,
                                currency = targetEntry.currency,
                                transactionType = TransactionType.TRANSFER_INFLOW,
                                accountId = targetAccountId,
                                categoryId = Category.SYSTEM_TRANSFER,
                                tags = tagIds,
                                targetAccountId = null,
                                targetCurrency = null,
                                transferRate = BigDecimal.ONE,
                                paymentMethod = targetEntry.paymentMethod,
                                payee = targetEntry.row["payee"] ?: "",
                                note = targetEntry.note,
                                sourcePlannedPaymentId = null,
                                profileId = profileId,
                                gpsLocation = buildGpsLocation(targetEntry.row),
                                location = null,
                                warrantyExpiration = targetEntry.warrantyExpiration,
                                status = TransactionStatus.CLEARED
                            ))
                            imported++
                            transfersRerouted++
                        }
                        targetExcluded -> {
                            // Keep source side only → outside-wallet outflow
                            val sourceAccountId = accountMap[sourceEntry.accountName.lowercase()]
                            if (sourceAccountId == null) { skipped++; return@forEach }
                            val absAmount = sourceEntry.baseAmount.abs()
                            val dupKey = "${sourceEntry.timestamp / 60000}|${absAmount.toPlainString()}|${sourceEntry.note}"
                            if (dupKey in existingKeys) {
                                if (!handleDuplicate(duplicatePolicy, sourceEntry.idx, selectedDuplicateRowIndices)) {
                                    skipped += 2; return@forEach
                                }
                            }
                            val tagIds = resolveTagIds(sourceEntry.tagNames, existingTagsList)
                            transactionDao.insertTransaction(TransactionEntity(
                                id = 0,
                                name = sourceEntry.note.ifBlank { "Transfer" }.take(50),
                                amount = absAmount.negate(),
                                timestamp = sourceEntry.timestamp,
                                currency = sourceEntry.currency,
                                transactionType = TransactionType.TRANSFER_OUTFLOW,
                                accountId = sourceAccountId,
                                categoryId = Category.SYSTEM_TRANSFER,
                                tags = tagIds,
                                targetAccountId = null,
                                targetCurrency = null,
                                transferRate = BigDecimal.ONE,
                                paymentMethod = sourceEntry.paymentMethod,
                                payee = sourceEntry.row["payee"] ?: "",
                                note = sourceEntry.note,
                                sourcePlannedPaymentId = null,
                                profileId = profileId,
                                gpsLocation = buildGpsLocation(sourceEntry.row),
                                location = null,
                                warrantyExpiration = sourceEntry.warrantyExpiration,
                                status = TransactionStatus.CLEARED
                            ))
                            imported++
                            transfersRerouted++
                        }
                        else -> {
                            // Both accounts present: 2 records + bi-directional link
                            val sourceAccountId = accountMap[sourceEntry.accountName.lowercase()]
                            val targetAccountId = accountMap[targetEntry.accountName.lowercase()]

                            if (sourceAccountId == null || targetAccountId == null) {
                                skipped += 2; return@forEach
                            }

                            val absAmount = sourceEntry.baseAmount.abs()
                            val dupKey = "${sourceEntry.timestamp / 60000}|${absAmount.toPlainString()}|${sourceEntry.note}"
                            if (dupKey in existingKeys) {
                                if (!handleDuplicate(duplicatePolicy, sourceEntry.idx, selectedDuplicateRowIndices)) {
                                    skipped += 2; return@forEach
                                }
                            }

                            val tagIds = resolveTagIds(sourceEntry.tagNames + targetEntry.tagNames, existingTagsList)

                            val transferRate = if (sourceEntry.currency != targetEntry.currency &&
                                sourceEntry.baseAmount.compareTo(BigDecimal.ZERO) != 0) {
                                targetEntry.baseAmount.abs().divide(sourceEntry.baseAmount.abs(), 8, RoundingMode.HALF_UP)
                            } else BigDecimal.ONE

                            val targetAmount = absAmount.multiply(transferRate)

                            val sourceId = transactionDao._insertTransaction(TransactionEntity(
                                id = 0,
                                name = sourceEntry.note.ifBlank { "Transfer" }.take(50),
                                amount = absAmount.negate(),
                                timestamp = sourceEntry.timestamp,
                                currency = sourceEntry.currency,
                                transactionType = TransactionType.TRANSFER_OUTFLOW,
                                accountId = sourceAccountId,
                                categoryId = Category.SYSTEM_TRANSFER,
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
                            ))

                            val inverseRate = if (transferRate.compareTo(BigDecimal.ZERO) != 0)
                                BigDecimal.ONE.divide(transferRate, 8, RoundingMode.HALF_UP) else BigDecimal.ONE

                            val targetId = transactionDao._insertTransaction(TransactionEntity(
                                id = 0,
                                name = targetEntry.note.ifBlank { "Transfer" }.take(50),
                                amount = targetAmount,
                                timestamp = targetEntry.timestamp,
                                currency = targetEntry.currency,
                                transactionType = TransactionType.TRANSFER_INFLOW,
                                accountId = targetAccountId,
                                categoryId = Category.SYSTEM_TRANSFER,
                                tags = tagIds,
                                targetAccountId = sourceAccountId,
                                targetCurrency = sourceEntry.currency,
                                transferRate = inverseRate,
                                paymentMethod = targetEntry.paymentMethod,
                                payee = targetEntry.row["payee"] ?: "",
                                note = targetEntry.note,
                                sourcePlannedPaymentId = null,
                                profileId = profileId,
                                gpsLocation = buildGpsLocation(targetEntry.row),
                                location = targetEntry.row["payee"]?.takeIf { it.isNotBlank() },
                                warrantyExpiration = targetEntry.warrantyExpiration,
                                status = TransactionStatus.CLEARED
                            ))

                            if (sourceId > 0 && targetId > 0) {
                                transactionLinkDao.insertLinks(listOf(
                                    TransactionLinkEntity(transactionId = sourceId, linkedTransactionId = targetId),
                                    TransactionLinkEntity(transactionId = targetId, linkedTransactionId = sourceId)
                                ))
                            }
                            imported += 2
                        }
                    }
                } else {
                    // Unpaired transfer — import as outside-wallet transfer, skip if excluded
                    group.forEach { t ->
                        if (t.accountName.lowercase() in excludedAccountNames) { skipped++; return@forEach }
                        val accountId = accountMap[t.accountName.lowercase()]
                        if (accountId == null) { skipped++; return@forEach }

                        val isOutflow = t.baseAmount < BigDecimal.ZERO
                        val finalAmount = if (isOutflow) t.baseAmount.abs().negate() else t.baseAmount.abs()
                        // Outflow: Wallet → Outside (TRANSFER_OUTFLOW category)
                        // Inflow:  Outside → Wallet (TRANSFER_INFLOW category)
                        val categoryId = Category.SYSTEM_TRANSFER

                        val tagIds = resolveTagIds(t.tagNames, existingTagsList)

                        transactionDao.insertTransaction(
                            TransactionEntity(
                                id = 0,
                                name = t.note.ifBlank { "Transfer" }.take(50),
                                amount = finalAmount,
                                timestamp = t.timestamp,
                                currency = t.currency,
                                transactionType = if (isOutflow) TransactionType.TRANSFER_OUTFLOW else TransactionType.TRANSFER_INFLOW,
                                accountId = accountId,
                                categoryId = categoryId,
                                tags = tagIds,
                                targetAccountId = null,
                                targetCurrency = null,
                                transferRate = BigDecimal.ONE,
                                paymentMethod = t.paymentMethod,
                                payee = t.row["payee"] ?: "",
                                note = t.note,
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
                    if (accountName.lowercase() in excludedAccountNames) { skipped++; return@forEachIndexed }
                    val dateStr = row["date"] ?: ""
                    val note = row["note"] ?: ""
                    val payee = row["payee"] ?: ""
                    val baseAmount = resolveBaseAmount(row, ';')
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
                            categoryMap[displayName.lowercase()] ?: Category.SYSTEM_OTHERS
                        }

                    val finalAmount = when (txType) {
                        TransactionType.EXPENSE -> baseAmount.abs().negate()
                        TransactionType.INCOME -> baseAmount.abs()
                        TransactionType.TRANSFER_INFLOW, TransactionType.TRANSFER_OUTFLOW -> baseAmount.abs()
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

        Result.success(ImportSummary(imported, skipped, errors, transfersRerouted))
    }

    // ──────────────────────────────────────────────────────────────────────────
    // Cashew-specific import
    // ──────────────────────────────────────────────────────────────────────────

    private suspend fun <T> parseCashewRows(inputStream: InputStream, block: suspend (Sequence<Map<String, String>>) -> T): T {
        val format = CSVFormat.DEFAULT.builder()
            .setDelimiter(',').setQuote('"')
            .setIgnoreEmptyLines(true).setTrim(true).build()
        return CSVParser.parse(inputStream.bufferedReader(Charsets.UTF_8), format).use { parser ->
            val iterator = parser.iterator()
            if (!iterator.hasNext()) return@use block(emptySequence())
            val headers = iterator.next().toList().map { it.trim().lowercase() }
            val rows = iterator.asSequence().map { rec ->
                headers.mapIndexed { i, h -> h to (rec.get(i).ifEmpty { "" }) }.toMap()
            }
            block(rows)
        }
    }

    private fun isCashewTransferRow(row: Map<String, String>): Boolean =
        row["category name"]?.equals("Balance Correction", ignoreCase = true) == true

    private fun cashewCombinedNote(row: Map<String, String>): String =
        listOf(row["title"], row["note"]).filter { !it.isNullOrBlank() }.joinToString(" — ")

    private suspend fun generateCashewPreview(
        session: CsvImportSession,
        inputStream: InputStream
    ): ImportPreview = parseCashewRows(inputStream) { rows ->
        // ARC-15: scope existence checks to the active profile.
        val profileId = settingsDataStore.currentProfileId.first()
        val existingAccountsList = accountDao.getAllAccountsSync(profileId)
        val existingAccountsByName = existingAccountsList.associateBy { it.name.lowercase() }
        val existingCategories = categoryDao.getAllCategoriesSync()
        val existingKeys = transactionDao.getAllTransactionsSync(profileId).map { tx ->
            "${tx.timestamp / 60000}|${tx.amount.abs().toPlainString()}|${tx.note}"
        }.toSet()

        val accountRowCounts = mutableMapOf<String, Int>()
        val newCategoryMap = mutableMapOf<String, CategoryToMap>() // key = "parent|leaf"
        val duplicates = mutableListOf<DuplicateGroup>()
        val parseErrors = mutableListOf<ParseError>()
        var validRows = 0
        var totalRows = 0
        val transferGroups = mutableMapOf<String, MutableList<Int>>()

        rows.forEachIndexed { idx, row ->
            totalRows++
            try {
                val accountName = row["account"] ?: ""
                val amount = normalizeAmount(row["amount"], ',')
                val timestamp = parseDate(row["date"] ?: "")
                val categoryName = row["category name"] ?: ""
                val subcategoryName = row["subcategory name"] ?: ""
                val combinedNote = cashewCombinedNote(row)
                val isTransfer = isCashewTransferRow(row)

                if (accountName.isNotBlank())
                    accountRowCounts[accountName] = (accountRowCounts[accountName] ?: 0) + 1

                if (!isTransfer && categoryName.isNotBlank()) {
                    val leafName = subcategoryName.takeIf { it.isNotBlank() } ?: categoryName
                    val parentHint = subcategoryName.takeIf { it.isNotBlank() }?.let { categoryName }
                    val key = "$categoryName|$leafName"
                    if (!newCategoryMap.containsKey(key)) {
                        val resolved = existingCategories.find { it.name.equals(leafName, ignoreCase = true) }
                        if (resolved == null) {
                            newCategoryMap[key] = CategoryToMap(name = leafName, suggestedParentName = parentHint)
                        }
                    }
                }

                if (isTransfer) {
                    val pairKey = "${timestamp / 60000}|${amount.abs().toPlainString()}"
                    transferGroups.getOrPut(pairKey) { mutableListOf() }.add(idx)
                }

                val dupKey = "${timestamp / 60000}|${amount.abs().toPlainString()}|$combinedNote"
                if (dupKey in existingKeys) {
                    if (duplicates.size < 500) {
                        duplicates.add(DuplicateGroup(dupKey, combinedNote, idx, row.values.toList()))
                    }
                } else validRows++
            } catch (e: Exception) {
                if (parseErrors.size < 100) {
                    parseErrors.add(ParseError(idx, e.message ?: "Parse error"))
                }
            }
        }

        if (totalRows == 0) return@parseCashewRows ImportPreview(0, 0, emptyList(), emptyList(), emptyList(), emptyList())

        val allCsvAccounts = accountRowCounts.map { (name, count) ->
            val match = existingAccountsByName[name.lowercase()]
            AccountToMap(name, match?.id, match?.name, count)
        }.sortedBy { it.name }

        ImportPreview(
            totalRows = totalRows,
            validRows = validRows,
            duplicates = duplicates,
            allCsvAccounts = allCsvAccounts,
            newCategories = newCategoryMap.values.toList(),
            parseErrors = parseErrors,
            unpairedTransferRows = transferGroups.values.count { it.size == 1 }
        )
    }

    private suspend fun executeCashewImport(
        session: CsvImportSession,
        inputStream: InputStream,
        duplicatePolicy: DuplicatePolicy,
        entityMappings: List<EntityMapping>,
        selectedDuplicateRowIndices: Set<Int>
    ): Result<ImportSummary> = parseCashewRows(inputStream) { rows ->
        val profileId = settingsDataStore.currentProfileId.first()
        val profileBaseCurrency = profileDao.getProfileById(profileId)?.baseCurrency ?: "INR"

        // ARC-15: scope reads to the active profile (profileId resolved above).
        val existingAccountEntities = accountDao.getAllAccountsSync(profileId).associateBy { it.name.lowercase() }
        val existingCategoryEntities = categoryDao.getAllCategoriesSync()
        val existingKeys = transactionDao.getAllTransactionsSync(profileId).map { tx ->
            "${tx.timestamp / 60000}|${tx.amount.abs().toPlainString()}|${tx.note}"
        }.toSet()

        val accountMap = existingAccountEntities.mapValues { it.value.id }.toMutableMap()
        val categoryMap = existingCategoryEntities.associate { it.name.lowercase() to it.id }.toMutableMap()
        val existingTagsList = tagDao.getAllTagsSync().toMutableList()

        // Build account currency map to ensure transactions use the correct currency
        val accountCurrencyMap = existingAccountEntities.mapValues { it.value.currencyCode }.toMutableMap()
        entityMappings.filter { it.entityType == EntityMapping.EntityType.ACCOUNT }.forEach { em ->
            em.currencyCode?.let { accountCurrencyMap[em.unknownName.lowercase()] = it }
        }

        var imported = 0
        var skipped = 0
        var transfersRerouted = 0
        val errors = mutableListOf<ParseError>()

        val excludedAccountNames = entityMappings
            .filter { it.entityType == EntityMapping.EntityType.ACCOUNT && it.isExcluded }
            .map { it.unknownName.lowercase() }.toSet()

        val newCategoryColorMap = mutableMapOf<Long, Int>()
        val sortedMappings = entityMappings.sortedWith(compareBy { em ->
            when {
                em.entityType == EntityMapping.EntityType.ACCOUNT -> -1
                em.parentCategoryId == null && em.parentCategoryName == null -> 0
                else -> 1
            }
        })

        database.withTransaction {
            // Apply entity mappings
            sortedMappings.forEach { em ->
                when (em.entityType) {
                    EntityMapping.EntityType.ACCOUNT -> {
                        if (em.isExcluded) return@forEach
                        val id = em.existingId ?: UUID.randomUUID().toString()
                        if (em.existingId == null) {
                            val conflictExists = existingAccountEntities.containsKey(em.unknownName.lowercase())
                            val finalName = if (conflictExists) "${em.unknownName}-${randomAlphanumSuffix()}" else em.unknownName
                            val currency = em.currencyCode ?: profileBaseCurrency
                            accountDao.upsertAccount(AccountEntity(
                                id = id, name = finalName,
                                currencyCode = currency, structureType = "regular",
                                colorHex = BRAND_ACCOUNT_COLORS.random(), icon = "wallet",
                                allowedCurrencies = currency,
                                profileId = profileId, isArchived = false, displayOrder = 999
                            ))
                        }
                        accountMap[em.unknownName.lowercase()] = id
                    }
                    EntityMapping.EntityType.CATEGORY -> {
                        val existingId = em.existingId
                        if (existingId != null) {
                            categoryMap[em.unknownName.lowercase()] = existingId.toLongOrNull() ?: Category.SYSTEM_OTHERS
                        } else {
                            val resolvedParentId = em.parentCategoryId
                                ?: em.parentCategoryName?.let { name ->
                                    categoryMap[name.lowercase()]
                                        ?: existingCategoryEntities.find { it.name.equals(name, ignoreCase = true) }?.id
                                }
                            val catColor = when {
                                resolvedParentId == null -> BRAND_CATEGORY_COLORS.random()
                                newCategoryColorMap.containsKey(resolvedParentId) -> newCategoryColorMap[resolvedParentId]!!
                                else -> existingCategoryEntities.find { it.id == resolvedParentId }?.colorHex
                                    ?: BRAND_CATEGORY_COLORS.random()
                            }
                            categoryDao.insertCategory(CategoryEntity(
                                id = 0, name = em.unknownName, iconName = "Category",
                                colorHex = catColor, isDefault = false,
                                parentId = resolvedParentId
                            ))
                            val inserted = categoryDao.getCategoryByName(em.unknownName)
                            val insertedId = inserted?.id ?: Category.SYSTEM_OTHERS
                            categoryMap[em.unknownName.lowercase()] = insertedId
                            if (resolvedParentId == null) newCategoryColorMap[insertedId] = catColor
                        }
                    }
                }
            }

            data class IndexedRow(val idx: Int, val row: Map<String, String>)

            val transferRows = mutableListOf<IndexedRow>()
            val regularRows = mutableListOf<IndexedRow>()
            rows.forEachIndexed { idx, row ->
                if (isCashewTransferRow(row)) transferRows.add(IndexedRow(idx, row))
                else regularRows.add(IndexedRow(idx, row))
            }

            data class ParsedCashewTransfer(
                val idx: Int,
                val row: Map<String, String>,
                val accountName: String,
                val timestamp: Long,
                val note: String,
                val baseAmount: BigDecimal,
                val currency: String
            )

            val parsedTransfers = transferRows.mapNotNull { (idx, row) ->
                try {
                    ParsedCashewTransfer(
                        idx = idx,
                        row = row,
                        accountName = row["account"] ?: return@mapNotNull null,
                        timestamp = parseDate(row["date"] ?: ""),
                        note = cashewCombinedNote(row),
                        baseAmount = normalizeAmount(row["amount"], ','),
                        currency = row["currency"] ?: accountCurrencyMap[row["account"]?.lowercase()] ?: profileBaseCurrency
                    )
                } catch (e: Exception) {
                    errors.add(ParseError(idx, e.message ?: "Transfer parse error"))
                    null
                }
            }

            val pairGroups = parsedTransfers.groupBy { t ->
                "${t.timestamp / 60000}|${t.baseAmount.abs().toPlainString()}"
            }

            pairGroups.forEach { (_, group) ->
                if (group.size == 2) {
                    val sourceEntry = group.firstOrNull { it.baseAmount.signum() < 0 } ?: group[0]
                    val targetEntry = group.firstOrNull { it != sourceEntry } ?: group[1]

                    val sourceExcluded = sourceEntry.accountName.lowercase() in excludedAccountNames
                    val targetExcluded = targetEntry.accountName.lowercase() in excludedAccountNames

                    when {
                        sourceExcluded && targetExcluded -> { skipped += 2; return@forEach }
                        sourceExcluded -> {
                            val targetAccountId = accountMap[targetEntry.accountName.lowercase()]
                            if (targetAccountId == null) { skipped++; return@forEach }
                            transactionDao.insertTransaction(TransactionEntity(
                                id = 0, name = targetEntry.note.ifBlank { "Transfer" }.take(50),
                                amount = targetEntry.baseAmount.abs(), timestamp = targetEntry.timestamp,
                                currency = targetEntry.currency, transactionType = TransactionType.TRANSFER_INFLOW,
                                accountId = targetAccountId, categoryId = Category.SYSTEM_TRANSFER, tags = emptySet(),
                                targetAccountId = null, targetCurrency = null, transferRate = BigDecimal.ONE,
                                paymentMethod = PaymentMethod.MOBILE_PAYMENT, payee = "", note = targetEntry.note,
                                sourcePlannedPaymentId = null, profileId = profileId,
                                gpsLocation = null, location = null, warrantyExpiration = null,
                                status = TransactionStatus.CLEARED
                            ))
                            imported++; transfersRerouted++
                        }
                        targetExcluded -> {
                            val sourceAccountId = accountMap[sourceEntry.accountName.lowercase()]
                            if (sourceAccountId == null) { skipped++; return@forEach }
                            val absAmount = sourceEntry.baseAmount.abs()
                            val dupKey = "${sourceEntry.timestamp / 60000}|${absAmount.toPlainString()}|${sourceEntry.note}"
                            if (dupKey in existingKeys && !handleDuplicate(duplicatePolicy, sourceEntry.idx, selectedDuplicateRowIndices)) {
                                skipped += 2; return@forEach
                            }
                            transactionDao.insertTransaction(TransactionEntity(
                                id = 0, name = sourceEntry.note.ifBlank { "Transfer" }.take(50),
                                amount = absAmount.negate(), timestamp = sourceEntry.timestamp,
                                currency = sourceEntry.currency, transactionType = TransactionType.TRANSFER_OUTFLOW,
                                accountId = sourceAccountId, categoryId = Category.SYSTEM_TRANSFER, tags = emptySet(),
                                targetAccountId = null, targetCurrency = null, transferRate = BigDecimal.ONE,
                                paymentMethod = PaymentMethod.MOBILE_PAYMENT, payee = "", note = sourceEntry.note,
                                sourcePlannedPaymentId = null, profileId = profileId,
                                gpsLocation = null, location = null, warrantyExpiration = null,
                                status = TransactionStatus.CLEARED
                            ))
                            imported++; transfersRerouted++
                        }
                        else -> {
                            val sourceAccountId = accountMap[sourceEntry.accountName.lowercase()]
                            val targetAccountId = accountMap[targetEntry.accountName.lowercase()]
                            if (sourceAccountId == null || targetAccountId == null) { skipped += 2; return@forEach }

                            val absAmount = sourceEntry.baseAmount.abs()
                            val dupKey = "${sourceEntry.timestamp / 60000}|${absAmount.toPlainString()}|${sourceEntry.note}"
                            if (dupKey in existingKeys && !handleDuplicate(duplicatePolicy, sourceEntry.idx, selectedDuplicateRowIndices)) {
                                skipped += 2; return@forEach
                            }

                            val transferRate = if (sourceEntry.currency != targetEntry.currency &&
                                sourceEntry.baseAmount.compareTo(BigDecimal.ZERO) != 0) {
                                targetEntry.baseAmount.abs().divide(sourceEntry.baseAmount.abs(), 8, RoundingMode.HALF_UP)
                            } else BigDecimal.ONE
                            val targetAmount = absAmount.multiply(transferRate)
                            val inverseRate = if (transferRate.compareTo(BigDecimal.ZERO) != 0)
                                BigDecimal.ONE.divide(transferRate, 8, RoundingMode.HALF_UP) else BigDecimal.ONE

                            val sourceId = transactionDao._insertTransaction(TransactionEntity(
                                id = 0, name = sourceEntry.note.ifBlank { "Transfer" }.take(50),
                                amount = absAmount.negate(), timestamp = sourceEntry.timestamp,
                                currency = sourceEntry.currency, transactionType = TransactionType.TRANSFER_OUTFLOW,
                                accountId = sourceAccountId, categoryId = Category.SYSTEM_TRANSFER, tags = emptySet(),
                                targetAccountId = targetAccountId, targetCurrency = targetEntry.currency,
                                transferRate = transferRate, paymentMethod = PaymentMethod.MOBILE_PAYMENT,
                                payee = "", note = sourceEntry.note, sourcePlannedPaymentId = null,
                                profileId = profileId, gpsLocation = null,
                                location = null, warrantyExpiration = null, status = TransactionStatus.CLEARED
                            ))
                            val targetId = transactionDao._insertTransaction(TransactionEntity(
                                id = 0, name = targetEntry.note.ifBlank { "Transfer" }.take(50),
                                amount = targetAmount, timestamp = targetEntry.timestamp,
                                currency = targetEntry.currency, transactionType = TransactionType.TRANSFER_INFLOW,
                                accountId = targetAccountId, categoryId = Category.SYSTEM_TRANSFER, tags = emptySet(),
                                targetAccountId = sourceAccountId, targetCurrency = sourceEntry.currency,
                                transferRate = inverseRate, paymentMethod = PaymentMethod.MOBILE_PAYMENT,
                                payee = "", note = targetEntry.note, sourcePlannedPaymentId = null,
                                profileId = profileId, gpsLocation = null,
                                location = null, warrantyExpiration = null, status = TransactionStatus.CLEARED
                            ))
                            if (sourceId > 0 && targetId > 0) {
                                transactionLinkDao.insertLinks(listOf(
                                    TransactionLinkEntity(transactionId = sourceId, linkedTransactionId = targetId),
                                    TransactionLinkEntity(transactionId = targetId, linkedTransactionId = sourceId)
                                ))
                            }
                            imported += 2
                        }
                    }
                } else {
                    // Unpaired → outside-wallet transfer
                    group.forEach { t ->
                        if (t.accountName.lowercase() in excludedAccountNames) { skipped++; return@forEach }
                        val accountId = accountMap[t.accountName.lowercase()]
                        if (accountId == null) { skipped++; return@forEach }
                        val isOutflow = t.baseAmount.signum() < 0
                        val finalAmount = if (isOutflow) t.baseAmount.abs().negate() else t.baseAmount.abs()
                        transactionDao.insertTransaction(TransactionEntity(
                            id = 0, name = t.note.ifBlank { "Transfer" }.take(50),
                            amount = finalAmount, timestamp = t.timestamp,
                            currency = t.currency, transactionType = if (isOutflow) TransactionType.TRANSFER_OUTFLOW else TransactionType.TRANSFER_INFLOW,
                            accountId = accountId, categoryId = Category.SYSTEM_TRANSFER,
                            tags = emptySet(), targetAccountId = null, targetCurrency = null,
                            transferRate = BigDecimal.ONE, paymentMethod = PaymentMethod.MOBILE_PAYMENT,
                            payee = "", note = t.note, sourcePlannedPaymentId = null,
                            profileId = profileId, gpsLocation = null,
                            location = null, warrantyExpiration = null, status = TransactionStatus.CLEARED
                        ))
                        imported++
                    }
                }
            }

            // Regular (non-transfer) rows
            regularRows.forEachIndexed { _, (idx, row) ->
                try {
                    val accountName = row["account"] ?: ""
                    if (accountName.lowercase() in excludedAccountNames) { skipped++; return@forEachIndexed }

                    val amountRaw = normalizeAmount(row["amount"], ',')
                    val isIncome = row["income"]?.equals("true", ignoreCase = true) == true
                    val finalAmount = if (isIncome) amountRaw.abs() else amountRaw.abs().negate()
                    val txType = if (isIncome) TransactionType.INCOME else TransactionType.EXPENSE
                    val combinedNote = cashewCombinedNote(row)
                    val timestamp = parseDate(row["date"] ?: "")

                    val dupKey = "${timestamp / 60000}|${amountRaw.abs().toPlainString()}|$combinedNote"
                    if (dupKey in existingKeys) {
                        if (!handleDuplicate(duplicatePolicy, idx, selectedDuplicateRowIndices)) {
                            skipped++; return@forEachIndexed
                        }
                    }

                    val accountId = accountMap[accountName.lowercase()]
                    if (accountId == null) { skipped++; return@forEachIndexed }

                    val subcategoryName = row["subcategory name"]?.takeIf { it.isNotBlank() }
                    val categoryName = row["category name"] ?: ""
                    val leafName = subcategoryName ?: categoryName
                    val txName = leafName.ifBlank { "Transaction" }.take(50)

                    val categoryId = existingCategoryEntities.find { it.name.equals(leafName, ignoreCase = true) }?.id
                        ?: categoryMap[leafName.lowercase()]
                        ?: Category.SYSTEM_OTHERS

                    val isRepetitive = row["type"]?.contains("repetitive", ignoreCase = true) == true
                    val baseTagNames: Set<String> = if (isRepetitive) setOf("Planned") else emptySet()
                    val tagIds = resolveTagIds(baseTagNames, existingTagsList)

                    transactionDao.insertTransaction(TransactionEntity(
                        id = 0,
                        name = txName,
                        amount = finalAmount,
                        timestamp = timestamp,
                        currency = row["currency"] ?: accountCurrencyMap[accountName.lowercase()] ?: profileBaseCurrency,
                        transactionType = txType,
                        accountId = accountId,
                        categoryId = categoryId,
                        tags = tagIds,
                        targetAccountId = null,
                        targetCurrency = null,
                        transferRate = BigDecimal.ONE,
                        paymentMethod = PaymentMethod.MOBILE_PAYMENT,
                        payee = "",
                        note = combinedNote,
                        sourcePlannedPaymentId = null,
                        profileId = profileId,
                        gpsLocation = null,
                        location = null,
                        warrantyExpiration = null,
                        status = TransactionStatus.CLEARED
                    ))
                    imported++
                } catch (e: Exception) {
                    errors.add(ParseError(idx, e.message ?: "Row error"))
                }
            }
        }

        Result.success(ImportSummary(imported, skipped, errors, transfersRerouted))
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
    ): Result<ImportSummary> = withContext(dispatcherProvider.io) {
        val profileId = settingsDataStore.currentProfileId.first()
        val profileBaseCurrency = profileDao.getProfileById(profileId)?.baseCurrency ?: "INR"
        
        val genericFormat = CSVFormat.DEFAULT.builder()
            .setDelimiter(session.delimiter)
            .setQuote('"')
            .setIgnoreEmptyLines(true)
            .setTrim(true)
            .build()

        CSVParser.parse(inputStream.bufferedReader(Charsets.UTF_8), genericFormat).use { parser ->
            val iterator = parser.iterator()
            if (!iterator.hasNext()) return@use Result.success(ImportSummary(0, 0, emptyList()))
            
            iterator.next() // Skip header

            // ARC-15: scope reads to the active profile (profileId resolved above).
            val existingAccounts = accountDao.getAllAccountsSync(profileId).associateBy { it.name.lowercase() }
            val existingCategories = categoryDao.getAllCategoriesSync().associateBy { it.name.lowercase() }
            val existingTxs = transactionDao.getAllTransactionsSync(profileId)
            val existingKeys = existingTxs.map { tx ->
                "${tx.timestamp / 60000}|${tx.amount.abs().toPlainString()}|${tx.note}"
            }.toSet()

        val accountMap = existingAccounts.mapValues { it.value.id }.toMutableMap()
        val categoryMap = existingCategories.mapValues { it.value.id }.toMutableMap()

        // Build account currency map to ensure transactions use the correct currency
        val accountCurrencyMap = existingAccounts.mapValues { it.value.currencyCode }.toMutableMap()
        entityMappings.filter { it.entityType == EntityMapping.EntityType.ACCOUNT }.forEach { em ->
            em.currencyCode?.let { accountCurrencyMap[em.unknownName.lowercase()] = it }
        }

        var imported = 0
        var skipped = 0
        val errors = mutableListOf<ParseError>()

        val newCategoryColorMap = mutableMapOf<Long, Int>()
        val sortedMappings = entityMappings.sortedWith(compareBy { em ->
            when {
                em.entityType == EntityMapping.EntityType.ACCOUNT -> -1
                em.parentCategoryId == null && em.parentCategoryName == null -> 0
                else -> 1
            }
        })

        database.withTransaction {
            sortedMappings.forEach { em ->
                when (em.entityType) {
                    EntityMapping.EntityType.ACCOUNT -> {
                        val id = em.existingId ?: UUID.randomUUID().toString()
                        if (em.existingId == null) {
                            val currency = em.currencyCode ?: profileBaseCurrency
                            accountDao.upsertAccount(
                                AccountEntity(
                                    id = id, name = em.unknownName,
                                    currencyCode = currency, structureType = "regular",
                                    colorHex = BRAND_ACCOUNT_COLORS.random(), icon = "wallet",
                                    allowedCurrencies = currency,
                                    profileId = profileId, isArchived = false, displayOrder = 999
                                )
                            )
                        }
                        accountMap[em.unknownName.lowercase()] = id
                    }
                    EntityMapping.EntityType.CATEGORY -> {
                        val existingId = em.existingId
                        if (existingId != null) {
                            categoryMap[em.unknownName.lowercase()] = existingId.toLongOrNull() ?: Category.SYSTEM_OTHERS
                        } else {
                            val resolvedParentId = em.parentCategoryId
                                ?: em.parentCategoryName?.let { name ->
                                    categoryMap[name.lowercase()]
                                        ?: existingCategories.values.find { it.name.equals(name, ignoreCase = true) }?.id
                                }
                            val catColor = when {
                                resolvedParentId == null -> BRAND_CATEGORY_COLORS.random()
                                newCategoryColorMap.containsKey(resolvedParentId) -> newCategoryColorMap[resolvedParentId]!!
                                else -> existingCategories.values.find { it.id == resolvedParentId }?.colorHex
                                    ?: BRAND_CATEGORY_COLORS.random()
                            }
                            categoryDao.insertCategory(
                                CategoryEntity(
                                    id = 0,
                                    name = em.unknownName,
                                    iconName = "Category",
                                    colorHex = catColor,
                                    isDefault = false,
                                    parentId = resolvedParentId
                                )
                            )
                            val inserted = categoryDao.getCategoryByName(em.unknownName)
                            val insertedId = inserted?.id ?: Category.SYSTEM_OTHERS
                            categoryMap[em.unknownName.lowercase()] = insertedId
                            if (resolvedParentId == null) newCategoryColorMap[insertedId] = catColor
                        }
                    }
                }
            }

            var idx = 0
            for (record in iterator) {
                try {
                    val cols = record.toList()
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

                    val amount = normalizeAmount(amountStr, session.delimiter)
                    val timestamp = parseDate(dateStr)

                    val dupKey = "${timestamp / 60000}|${amount.abs().toPlainString()}|$note"
                    if (dupKey in existingKeys) {
                        if (!handleDuplicate(duplicatePolicy, idx, selectedDuplicateRowIndices)) {
                            skipped++
                            idx++
                            continue
                        }
                    }

                    val accountId = accountMap[accountName.lowercase()]
                    if (accountId == null) { 
                        skipped++
                        idx++
                        continue
                    }

                    val categoryId = categoryMap[categoryName.lowercase()] ?: Category.SYSTEM_OTHERS

                    val txType = when {
                        isTransferStr.equals("true", ignoreCase = true) -> TransactionType.TRANSFER_OUTFLOW
                        typeStr.equals("income", ignoreCase = true) -> TransactionType.INCOME
                        else -> TransactionType.EXPENSE
                    }

                    val finalAmount = if (txType == TransactionType.EXPENSE || txType == TransactionType.TRANSFER_OUTFLOW)
                        amount.abs().negate() else amount.abs()

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
                            currency = accountCurrencyMap[accountName.lowercase()] ?: profileBaseCurrency,
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
                idx++
            }
        }

        Result.success(ImportSummary(imported, skipped, errors))
    } }

    // ──────────────────────────────────────────────────────────────────────────
    // Helpers
    // ──────────────────────────────────────────────────────────────────────────

    private fun normalizeAmount(s: String?, delimiter: Char): BigDecimal {
        if (s.isNullOrBlank()) return BigDecimal.ZERO
        
        // Remove common non-numeric fluff like currency symbols or spaces
        val clean = s.filter { it.isDigit() || it == '-' || it == ',' || it == '.' }
        
        val normalized = if (delimiter == ';') {
            // Heuristic: semicolon delimiter usually implies European locale (comma as decimal)
            clean.replace(".", "").replace(",", ".")
        } else {
            // Heuristic: comma delimiter usually implies US/UK locale (dot as decimal)
            clean.replace(",", "")
        }
        return normalized.toBigDecimalOrNull() ?: BigDecimal.ZERO
    }

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

    private fun randomAlphanumSuffix(): String =
        (('a'..'z') + ('0'..'9')).shuffled().take(6).joinToString("")

    companion object {
        private const val DEFAULT_TAG_COLOR: Int = 0xFF9E9E9E.toInt()

        private val BRAND_CATEGORY_COLORS = listOf(
            0xFF004D5F.toInt(), 0xFF1A237E.toInt(), 0xFF1B5E20.toInt(),
            0xFFB71C1C.toInt(), 0xFF4A148C.toInt(), 0xFFE65100.toInt(),
            0xFF006064.toInt(), 0xFF212121.toInt(), 0xFF795548.toInt(),
            0xFF607D8B.toInt()
        )

        private val BRAND_ACCOUNT_COLORS = listOf(
            "#004D5F", "#1A237E", "#1B5E20", "#B71C1C", "#4A148C",
            "#E65100", "#006064", "#212121", "#795548", "#607D8B"
        )
    }
}
