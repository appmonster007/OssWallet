package com.osscabinet.ossWallet.data.service

import androidx.core.graphics.toColorInt
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
import com.osscabinet.ossWallet.domain.model.CategoryNature
import com.osscabinet.ossWallet.domain.model.PaymentMethod
import com.osscabinet.ossWallet.domain.model.TransactionStatus
import com.osscabinet.ossWallet.domain.model.TransactionType
import com.osscabinet.ossWallet.domain.util.joinByComma
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
import com.univocity.parsers.csv.CsvParser
import com.univocity.parsers.csv.CsvParserSettings
import jakarta.inject.Inject
import jakarta.inject.Singleton
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
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
    private val dispatcherProvider: DispatcherProvider,
    private val json: Json
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
        .append(
            DateTimeFormatter.ofPattern(
                "[yyyy-MM-dd HH:mm:ss.SSS]" +
                        "[yyyy-MM-dd HH:mm:ss]" +
                        "[yyyy-MM-dd'T'HH:mm:ss]" +
                        "[yyyy-MM-dd]" +
                        "[dd/MM/yyyy HH:mm:ss]" +
                        "[dd/MM/yyyy]" +
                        "[MM/dd/yyyy]" +
                        "[dd.MM.yyyy]"
            )
        )
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
        } catch (_: Exception) {
            try {
                java.time.LocalDate.parse(dateStr, lenientDateFormatter)
                    .atStartOfDay(ZoneId.systemDefault())
                    .toInstant()
                    .toEpochMilli()
            } catch (_: Exception) {
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
            settings.format.delimiter = delimiter
        } else {
            settings.detectFormatAutomatically()
        }
        settings.isHeaderExtractionEnabled = true
        settings.skipEmptyLines = true
        settings.ignoreLeadingWhitespaces = true
        settings.ignoreTrailingWhitespaces = true
        settings.emptyValue = ""
        settings.nullValue = ""
        return CsvParser(settings)
    }

    private suspend fun <T> parseRows(
        inputStream: InputStream,
        delimiter: Char? = null,
        encoding: String = "UTF-8",
        block: suspend (List<String>, Sequence<Map<String, String>>) -> T
    ): T = withContext(dispatcherProvider.io) {
        val parser = createParser(delimiter)
        val headers = mutableListOf<String>()
        val recordsIterator = parser.iterateRecords(inputStream, encoding).iterator()

        val rowsSequence = sequence {
            if (recordsIterator.hasNext()) {
                headers.addAll(parser.getContext().headers()?.map { it.lowercase() } ?: emptyList())
                while (recordsIterator.hasNext()) {
                    val record = recordsIterator.next()
                    yield(headers.zip(record.values).toMap())
                }
            }
        }

        block(headers, rowsSequence)
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
        row["type"]?.trimEnd('s')
            ?.equals("expense", ignoreCase = true) == true -> TransactionType.EXPENSE

        row["type"]?.equals("income", ignoreCase = true) == true -> TransactionType.INCOME
        else -> TransactionType.EXPENSE
    }

    private fun transferPairKey(timestamp: Long, note: String, absAmount: BigDecimal): String =
        "${timestamp / 60000}|$note|${absAmount.toPlainString()}"

    private fun resolveCategory(
        row: Map<String, String>,
        existingCats: List<CategoryEntity>
    ): Long? {
        if (isTransferRow(row)) return null
        val envelopeId = row["envelope_id"]?.toIntOrNull()
        val categoryText = row["category"] ?: ""
        val mappedName = envelopeId?.let { assetLoader.envelopeMap[it]?.name }
        if (!mappedName.isNullOrBlank() && mappedName != "TRANSFER") {
            val match = existingCats.find { it.name.equals(mappedName, ignoreCase = true) }
            return match?.id
        }
        if (categoryText.isNotBlank()) {
            val fuzzy = existingCats.find { cat ->
                cat.name.equals(categoryText, ignoreCase = true) ||
                        cat.name.lowercase().contains(categoryText.lowercase()) ||
                        categoryText.lowercase().contains(cat.name.lowercase())
            }
            return fuzzy?.id
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
    // Shared entity resolution (accounts + categories), used by all execute*Import paths
    // ──────────────────────────────────────────────────────────────────────────

    private data class ResolvedEntities(
        val accountMap: MutableMap<String, String>,
        val categoryMap: MutableMap<String, Long>,
        val accountCurrencyMap: MutableMap<String, String>
    )

    private suspend fun resolveAndCreateEntities(
        entityMappings: List<EntityMapping>,
        existingAccounts: Map<String, AccountEntity>,
        existingCats: List<CategoryEntity>,
        profileId: Int,
        profileBaseCurrency: String
    ): ResolvedEntities {
        val accountMap = existingAccounts.mapValues { it.value.id }.toMutableMap()
        val accountCurrencyMap = existingAccounts.mapValues { it.value.currencyCode }.toMutableMap()
        val categoryMap = existingCats.associateBy({ it.name.lowercase() }, { it.id }).toMutableMap()
        val newCategoryColorMap = mutableMapOf<Long, Int>()

        entityMappings
            .sortedBy { if (it.entityType == EntityMapping.EntityType.ACCOUNT) -1 else 0 }
            .forEach { em ->
                when (em.entityType) {
                    EntityMapping.EntityType.ACCOUNT -> {
                        if (em.isExcluded) return@forEach
                        val id = em.existingId ?: UUID.randomUUID().toString()
                        if (em.existingId == null) {
                            val finalName =
                                if (existingAccounts.containsKey(em.unknownName.lowercase()))
                                    "${em.unknownName}-${randomSuffix()}" else em.unknownName
                            val primaryCurrency = em.currencyCode ?: profileBaseCurrency
                            val allowed = em.allowedCurrencies.ifEmpty { listOf(primaryCurrency) }
                            accountDao.upsertAccount(
                                AccountEntity(
                                    id = id,
                                    name = finalName,
                                    currencyCode = primaryCurrency,
                                    structureType = em.structureType ?: "regular",
                                    colorHex = em.colorHex ?: BRAND_COLORS.random(),
                                    icon = em.iconName ?: "wallet",
                                    allowedCurrencies = allowed.joinByComma(),
                                    profileId = profileId,
                                    isArchived = false,
                                    displayOrder = 999
                                )
                            )
                            em.openingBalances.forEach { (currency, amountStr) ->
                                val amount = amountStr.toBigDecimalOrNull() ?: BigDecimal.ZERO
                                if (amount.signum() != 0) {
                                    transactionDao.insertTransaction(
                                        TransactionEntity(
                                            name = "Initial Balance",
                                            amount = amount,
                                            timestamp = System.currentTimeMillis(),
                                            currency = currency,
                                            transactionType = if (amount.signum() >= 0) TransactionType.INCOME else TransactionType.EXPENSE,
                                            accountId = id,
                                            categoryId = Category.SYSTEM_INITIAL_BALANCE,
                                            tags = emptySet(),
                                            paymentMethod = PaymentMethod.CASH,
                                            payee = "",
                                            note = "Opening balance before import",
                                            profileId = profileId,
                                            status = TransactionStatus.CLEARED
                                        )
                                    )
                                }
                            }
                        }
                        accountMap[em.unknownName.lowercase()] = id
                        em.currencyCode?.let { accountCurrencyMap[em.unknownName.lowercase()] = it }
                    }

                    EntityMapping.EntityType.CATEGORY -> {
                        val existingId = em.existingId
                        if (existingId != null) {
                            categoryMap[em.unknownName.lowercase()] =
                                existingId.toLongOrNull() ?: Category.SYSTEM_OTHERS
                        } else {
                            val parentId = em.parentCategoryId
                                ?: em.parentCategoryName?.let { categoryMap[it.lowercase()] }
                            val color = em.colorHex?.let { runCatching { it.toColorInt() }.getOrNull() }
                                ?: parentId?.let { pid ->
                                    newCategoryColorMap[pid]
                                        ?: existingCats.find { c -> c.id == pid }?.colorHex
                                } ?: BRAND_COLORS.random().toColorInt()
                            val nature = em.nature
                                ?.let { runCatching { CategoryNature.valueOf(it) }.getOrNull() }
                                ?: CategoryNature.WANT

                            categoryDao.insertCategory(
                                CategoryEntity(
                                    id = 0,
                                    name = em.unknownName,
                                    iconName = em.iconName ?: "Category",
                                    colorHex = color,
                                    isDefault = false,
                                    parentId = parentId,
                                    nature = nature,
                                    profileId = profileId
                                )
                            )
                            val insertedId = categoryDao.getCategoryByName(em.unknownName)?.id
                                ?: Category.SYSTEM_OTHERS
                            categoryMap[em.unknownName.lowercase()] = insertedId
                            if (parentId == null) newCategoryColorMap[insertedId] = color
                        }
                    }
                }
            }
        return ResolvedEntities(accountMap, categoryMap, accountCurrencyMap)
    }

    // ──────────────────────────────────────────────────────────────────────────
    // Service API
    // ──────────────────────────────────────────────────────────────────────────

    override suspend fun parseHeaders(
        inputStream: InputStream,
        delimiter: Char?,
        encoding: String
    ): CsvImportSession =
        withContext(dispatcherProvider.io) {
            val parser = createParser(delimiter)
            val records = inputStream.use { parser.parseAllRecords(it, encoding) }
            val headers =
                parser.getContext().headers()?.map { it.trim().lowercase() } ?: emptyList()
            val resolvedDelimiter = delimiter ?: parser.getDetectedFormat()?.getDelimiter() ?: ','

            if (headers.isEmpty()) return@withContext CsvImportSession(
                emptyList(),
                emptyList(),
                emptyMap(),
                null,
                resolvedDelimiter,
                encoding
            )

            val previewRows = records.take(25).map { it.values.toList() }
            val detectedApp = if (headers.toSet()
                    .containsAll(budgetBakersHeaders)
            ) ImportSourceApp.BUDGET_BAKERS else null
            val mapping = buildInitialMapping(headers, detectedApp ?: ImportSourceApp.GENERIC)

            CsvImportSession(headers, previewRows, mapping, detectedApp, resolvedDelimiter, encoding)
        }

    private fun buildInitialMapping(
        headers: List<String>,
        sourceApp: ImportSourceApp
    ): Map<OssWalletField, Int?> = when (sourceApp) {
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

    override suspend fun generatePreview(
        session: CsvImportSession,
        inputStream: InputStream
    ): ImportPreview =
        when (session.sourceApp) {
            ImportSourceApp.BUDGET_BAKERS -> generateBudgetBakersPreview(session, inputStream)
            ImportSourceApp.CASHEW -> generateCashewPreview(session, inputStream)
            else -> generateGenericPreview(session, inputStream)
        }

    private suspend fun generateBudgetBakersPreview(
        session: CsvImportSession,
        inputStream: InputStream
    ): ImportPreview =
        parseRows(inputStream, session.delimiter, session.encoding) { _, rows ->
            val profileId = settingsDataStore.currentProfileId.first()
            val existingTxs = transactionDao.getAllTransactionsSync(profileId)
            val existingAccounts =
                accountDao.getAllAccountsSync(profileId).associateBy { it.name.lowercase() }
            val existingCategories = categoryDao.getAllCategoriesSync()
            val existingKeys = existingTxs.map {
                "${it.timestamp / 60000}|${
                    it.amount.abs().toPlainString()
                }|${it.note}"
            }.toSet()

            val accountRowCounts = mutableMapOf<String, Int>()
            val accountCurrencies = mutableMapOf<String, MutableSet<String>>()
            val newCategoryMap = mutableMapOf<String, CategoryToMap>()
            val duplicates = mutableListOf<DuplicateGroup>()
            val parseErrors = mutableListOf<ParseError>()
            var validRows = 0
            var totalDuplicateCount = 0
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
                        if (currency.isNotBlank()) accountCurrencies.getOrPut(accountName) { mutableSetOf() }
                            .add(currency.uppercase())
                    }

                    if (!isTransferRow(row)) {
                        if (resolveCategory(row, existingCategories) == null) {
                            val displayName = unresolvedCategoryName(row)
                            newCategoryMap.getOrPut(displayName) {
                                CategoryToMap(
                                    displayName,
                                    row["envelope_id"]?.toIntOrNull(),
                                    row["custom_category"]?.toBoolean() ?: false,
                                    suggestedParentName(row)
                                )
                            }
                        }
                    } else {
                        val pairKey = transferPairKey(timestamp, note, amount.abs())
                        transferGroups.getOrPut(pairKey) { mutableListOf() }.add(idx)
                    }

                    val dupKey = "${timestamp / 60000}|${amount.abs().toPlainString()}|$note"
                    if (dupKey in existingKeys) {
                        totalDuplicateCount++
                        if (duplicates.size < 500) duplicates.add(
                            DuplicateGroup(
                                dupKey,
                                note,
                                idx,
                                row.values.toList()
                            )
                        )
                    } else validRows++
                } catch (e: Exception) {
                    parseErrors.add(ParseError(idx, e.message ?: "Parse error"))
                }
            }

            ImportPreview(
                totalRows = totalRows,
                validRows = validRows,
                duplicates = duplicates,
                totalDuplicateCount = totalDuplicateCount,
                allCsvAccounts = accountRowCounts.map { (name, count) ->
                    val match = existingAccounts[name.lowercase()]
                    AccountToMap(
                        name,
                        match?.id,
                        match?.name,
                        count,
                        accountCurrencies[name]?.isNotEmpty() == true
                    )
                }.sortedBy { it.name },
                newCategories = newCategoryMap.values.toList(),
                parseErrors = parseErrors,
                unpairedTransferRows = transferGroups.values.count { it.size == 1 }
            )
        }

    private suspend fun generateGenericPreview(
        session: CsvImportSession,
        inputStream: InputStream
    ): ImportPreview =
        parseRows(inputStream, session.delimiter, session.encoding) { headers, rows ->
            val profileId = settingsDataStore.currentProfileId.first()
            val existingTxs = transactionDao.getAllTransactionsSync(profileId)
            val existingAccounts =
                accountDao.getAllAccountsSync(profileId).map { it.name.lowercase() }.toSet()
            val existingCategories =
                categoryDao.getAllCategoriesSync().map { it.name.lowercase() }.toSet()
            val existingKeys = existingTxs.map {
                "${it.timestamp / 60000}|${
                    it.amount.abs().toPlainString()
                }|${it.note}"
            }.toSet()

            val newAccounts = mutableSetOf<String>()
            val newCategories = mutableSetOf<String>()
            val duplicates = mutableListOf<DuplicateGroup>()
            val parseErrors = mutableListOf<ParseError>()
            var validRows = 0
            var totalDuplicateCount = 0
            var totalCount = 0

            rows.forEachIndexed { idx, row ->
                totalCount++
                try {
                    val accountName =
                        session.mapping[OssWalletField.ACCOUNT]?.let { headers.getOrNull(it) }
                            ?.let { row[it] } ?: ""
                    val categoryName =
                        session.mapping[OssWalletField.CATEGORY]?.let { headers.getOrNull(it) }
                            ?.let { row[it] } ?: ""
                    val amountStr =
                        session.mapping[OssWalletField.AMOUNT]?.let { headers.getOrNull(it) }
                            ?.let { row[it] } ?: "0"
                    val dateStr =
                        session.mapping[OssWalletField.DATE]?.let { headers.getOrNull(it) }
                            ?.let { row[it] } ?: ""
                    val note = session.mapping[OssWalletField.NOTE]?.let { headers.getOrNull(it) }
                        ?.let { row[it] } ?: ""

                    if (accountName.isNotBlank() && accountName.lowercase() !in existingAccounts) newAccounts.add(
                        accountName
                    )
                    if (categoryName.isNotBlank() && categoryName.lowercase() !in existingCategories) newCategories.add(
                        categoryName
                    )

                    val amount = normalizeAmount(amountStr)
                    val timestamp = parseDate(dateStr)
                    val dupKey = "${timestamp / 60000}|${amount.abs().toPlainString()}|$note"
                    if (dupKey in existingKeys) {
                        totalDuplicateCount++
                        if (duplicates.size < 500) duplicates.add(
                            DuplicateGroup(
                                dupKey,
                                note,
                                idx,
                                row.values.toList()
                            )
                        )
                    } else validRows++
                } catch (e: Exception) {
                    parseErrors.add(ParseError(idx, e.message ?: "Parse error"))
                }
            }

            ImportPreview(
                totalRows = totalCount,
                validRows = validRows,
                duplicates = duplicates,
                totalDuplicateCount = totalDuplicateCount,
                allCsvAccounts = newAccounts.map { AccountToMap(it, null, null) },
                newCategories = newCategories.map { CategoryToMap(it) },
                parseErrors = parseErrors,
                unpairedTransferRows = 0
            )
        }

    private suspend fun generateCashewPreview(
        session: CsvImportSession,
        inputStream: InputStream
    ): ImportPreview =
        parseRows(inputStream, session.delimiter, session.encoding) { _, rows ->
            val profileId = settingsDataStore.currentProfileId.first()
            val existingTxs = transactionDao.getAllTransactionsSync(profileId)
            val existingAccounts =
                accountDao.getAllAccountsSync(profileId).associateBy { it.name.lowercase() }
            val existingCategories = categoryDao.getAllCategoriesSync()
            val existingKeys = existingTxs.map {
                "${it.timestamp / 60000}|${
                    it.amount.abs().toPlainString()
                }|${it.note}"
            }.toSet()

            val accountRowCounts = mutableMapOf<String, Int>()
            val newCategoryMap = mutableMapOf<String, CategoryToMap>()
            val duplicates = mutableListOf<DuplicateGroup>()
            val parseErrors = mutableListOf<ParseError>()
            var validRows = 0
            var totalDuplicateCount = 0
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
                    val note = listOf(row["title"], row["note"]).filter { !it.isNullOrBlank() }
                        .joinToString(" — ")

                    if (accountName.isNotBlank()) accountRowCounts[accountName] =
                        (accountRowCounts[accountName] ?: 0) + 1

                    if (row["category name"]?.equals(
                            "Balance Correction",
                            ignoreCase = true
                        ) == true
                    ) {
                        val pairKey = "${timestamp / 60000}|${amount.abs().toPlainString()}"
                        transferGroups.getOrPut(pairKey) { mutableListOf() }.add(idx)
                    } else if (cat.isNotBlank()) {
                        val leaf = sub.takeIf { it.isNotBlank() } ?: cat
                        if (existingCategories.none { it.name.equals(leaf, ignoreCase = true) }) {
                            newCategoryMap.getOrPut(leaf) {
                                CategoryToMap(
                                    leaf,
                                    suggestedParentName = sub.takeIf { it.isNotBlank() }
                                        ?.let { cat })
                            }
                        }
                    }

                    val dupKey = "${timestamp / 60000}|${amount.abs().toPlainString()}|$note"
                    if (dupKey in existingKeys) {
                        totalDuplicateCount++
                        if (duplicates.size < 500) duplicates.add(
                            DuplicateGroup(
                                dupKey,
                                note,
                                idx,
                                row.values.toList()
                            )
                        )
                    } else validRows++
                } catch (e: Exception) {
                    parseErrors.add(ParseError(idx, e.message ?: "Parse error"))
                }
            }

            ImportPreview(
                totalRows = totalRows,
                validRows = validRows,
                duplicates = duplicates,
                totalDuplicateCount = totalDuplicateCount,
                allCsvAccounts = accountRowCounts.map { (name, count) ->
                    val match = existingAccounts[name.lowercase()]
                    AccountToMap(name, match?.id, match?.name, count)
                },
                newCategories = newCategoryMap.values.toList(),
                parseErrors = parseErrors,
                unpairedTransferRows = transferGroups.values.count { it.size == 1 })
        }

    override suspend fun executeImport(
        session: CsvImportSession,
        inputStream: InputStream,
        duplicatePolicy: DuplicatePolicy,
        entityMappings: List<EntityMapping>,
        selectedDuplicateRowIndices: Set<Int>,
        totalRows: Int,
        onProgress: (Float) -> Unit
    ): Result<ImportSummary> = try {
        when (session.sourceApp) {
            ImportSourceApp.BUDGET_BAKERS -> executeBudgetBakersImport(
                session,
                inputStream,
                duplicatePolicy,
                entityMappings,
                selectedDuplicateRowIndices,
                totalRows,
                onProgress
            )

            ImportSourceApp.CASHEW -> executeCashewImport(
                session,
                inputStream,
                duplicatePolicy,
                entityMappings,
                selectedDuplicateRowIndices,
                totalRows,
                onProgress
            )

            else -> executeGenericImport(
                session,
                inputStream,
                duplicatePolicy,
                entityMappings,
                selectedDuplicateRowIndices,
                totalRows,
                onProgress
            )
        }
    } catch (e: Exception) {
        Result.failure(e)
    }

    private fun rowProgress(rowIndex: Int, totalRows: Int): Float =
        if (totalRows <= 0) 0f else (((rowIndex + 1).toFloat() / totalRows).coerceAtMost(0.99f))

    private suspend fun executeBudgetBakersImport(
        session: CsvImportSession,
        inputStream: InputStream,
        duplicatePolicy: DuplicatePolicy,
        entityMappings: List<EntityMapping>,
        selectedDuplicateRowIndices: Set<Int>,
        totalRows: Int,
        onProgress: (Float) -> Unit
    ): Result<ImportSummary> = withContext(dispatcherProvider.io) {
        val profileId = settingsDataStore.currentProfileId.first()
        val profile = profileDao.getProfileById(profileId)
        val profileBaseCurrency = profile?.baseCurrency ?: "INR"

        database.withTransaction {
            val existingAccounts =
                accountDao.getAllAccountsSync(profileId).associateBy { it.name.lowercase() }
            val existingCats = categoryDao.getAllCategoriesSync()
            val existingTxs = transactionDao.getAllTransactionsSync(profileId)
            val existingKeys =
                existingTxs.map { getDuplicateKey(it.timestamp, it.amount, it.note) }.toMutableSet()
            val existingTags = tagDao.getAllTagsSync().toMutableList()

            val resolved = resolveAndCreateEntities(
                entityMappings,
                existingAccounts,
                existingCats,
                profileId,
                profileBaseCurrency
            )
            val accountMap = resolved.accountMap
            val categoryMap = resolved.categoryMap

            var imported = 0
            var skipped = 0
            var rerouted = 0
            val errors = mutableListOf<ParseError>()
            val excludedAccounts =
                entityMappings.filter { it.entityType == EntityMapping.EntityType.ACCOUNT && it.isExcluded }
                    .map { it.unknownName.lowercase() }.toSet()

            val pendingTransfers = mutableMapOf<String, BudgetTransfer>()

            parseRows(inputStream, session.delimiter, session.encoding) { _, rowsSequence ->
                rowsSequence.forEachIndexed { idx, row ->
                    try {
                        if (isTransferRow(row)) {
                            val t = parseBudgetTransfer(idx, row)
                            val key = transferPairKey(t.timestamp, t.note, t.amount.abs())
                            val match = pendingTransfers.remove(key)
                            if (match != null) {
                                val (source, target) = if (t.amount.signum() < 0) t to match else match to t
                                val srcExc = source.account.lowercase() in excludedAccounts
                                val tgtExc = target.account.lowercase() in excludedAccounts
                                when {
                                    srcExc && tgtExc -> skipped += 2
                                    srcExc -> {
                                        val tid = accountMap[target.account.lowercase()]
                                        if (tid != null) {
                                            insertBudgetTx(
                                                target,
                                                tid,
                                                TransactionType.TRANSFER_INFLOW,
                                                target.amount.abs(),
                                                null,
                                                profileId,
                                                existingTags,
                                                existingKeys
                                            )
                                            imported++; rerouted++
                                        } else skipped++
                                    }

                                    tgtExc -> {
                                        val sid = accountMap[source.account.lowercase()]
                                        if (sid != null && handleDup(
                                                source,
                                                existingKeys,
                                                duplicatePolicy,
                                                selectedDuplicateRowIndices
                                            )
                                        ) {
                                            insertBudgetTx(
                                                source,
                                                sid,
                                                TransactionType.TRANSFER_OUTFLOW,
                                                source.amount.abs().negate(),
                                                null,
                                                profileId,
                                                existingTags,
                                                existingKeys
                                            )
                                            imported++; rerouted++
                                        } else skipped += 2
                                    }

                                    else -> {
                                        val sid = accountMap[source.account.lowercase()]
                                        val tid = accountMap[target.account.lowercase()]
                                        if (sid != null && tid != null && handleDup(
                                                source,
                                                existingKeys,
                                                duplicatePolicy,
                                                selectedDuplicateRowIndices
                                            )
                                        ) {
                                            insertBudgetTransferPair(
                                                source,
                                                target,
                                                sid,
                                                tid,
                                                profileId,
                                                existingTags,
                                                existingKeys
                                            )
                                            imported += 2
                                        } else skipped += 2
                                    }
                                }
                            } else {
                                pendingTransfers[key] = t
                            }
                        } else {
                            val accName = row["account"]?.lowercase() ?: ""
                            if (accName in excludedAccounts) {
                                skipped++
                            } else {
                                val amount = resolveBaseAmount(row)
                                val timestamp = parseDate(row["date"] ?: "")
                                val note = row["note"] ?: ""
                                if (handleDup(
                                        idx,
                                        timestamp,
                                        amount,
                                        note,
                                        existingKeys,
                                        duplicatePolicy,
                                        selectedDuplicateRowIndices
                                    )
                                ) {
                                    val aid = accountMap[accName]
                                    if (aid != null) {
                                        val cid = resolveCategory(row, existingCats)
                                            ?: categoryMap[unresolvedCategoryName(row).lowercase()]
                                            ?: Category.SYSTEM_OTHERS
                                        val type = detectTransactionType(row)
                                        val finalAmount =
                                            if (type == TransactionType.EXPENSE || type == TransactionType.TRANSFER_OUTFLOW) amount.abs()
                                                .negate() else amount.abs()
                                        transactionDao.insertTransaction(
                                            TransactionEntity(
                                                name = (row["category"] ?: note).take(50),
                                                amount = finalAmount,
                                                timestamp = timestamp,
                                                currency = row["currency"] ?: "INR",
                                                transactionType = type,
                                                accountId = aid,
                                                categoryId = cid,
                                                tags = resolveTagIds(
                                                    parseTags(row["labels"] ?: ""),
                                                    existingTags
                                                ),
                                                paymentMethod = resolvePaymentMethod(row),
                                                payee = row["payee"] ?: "",
                                                note = note,
                                                profileId = profileId,
                                                gpsLocation = buildGps(row),
                                                location = row["payee"]?.takeIf { it.isNotBlank() },
                                                warrantyExpiration = row["warranty_in_month"]?.toIntOrNull()
                                                    ?.let { timestamp + (it * 2592000000L) },
                                                status = TransactionStatus.CLEARED
                                            )
                                        )
                                        existingKeys.add(getDuplicateKey(timestamp, amount, note))
                                        imported++
                                    } else skipped++
                                } else skipped++
                            }
                        }
                    } catch (e: Exception) {
                        errors.add(ParseError(idx, e.message ?: "Row error"))
                    }
                    onProgress(rowProgress(idx, totalRows))
                }
            }

            pendingTransfers.values.forEach { t ->
                if (t.account.lowercase() in excludedAccounts) {
                    skipped++
                } else {
                    val aid = accountMap[t.account.lowercase()]
                    if (aid != null) {
                        val isOut = t.amount.signum() < 0
                        insertBudgetTx(
                            t,
                            aid,
                            if (isOut) TransactionType.TRANSFER_OUTFLOW else TransactionType.TRANSFER_INFLOW,
                            if (isOut) t.amount.abs().negate() else t.amount.abs(),
                            null,
                            profileId,
                            existingTags,
                            existingKeys
                        )
                        imported++
                    } else skipped++
                }
            }
            onProgress(1.0f)
            Result.success(ImportSummary(imported, skipped, errors, rerouted))
        }
    }

    private suspend fun executeCashewImport(
        session: CsvImportSession,
        inputStream: InputStream,
        duplicatePolicy: DuplicatePolicy,
        entityMappings: List<EntityMapping>,
        selectedDuplicateRowIndices: Set<Int>,
        totalRows: Int,
        onProgress: (Float) -> Unit
    ): Result<ImportSummary> = withContext(dispatcherProvider.io) {
        val profileId = settingsDataStore.currentProfileId.first()
        val profileBaseCurrency = profileDao.getProfileById(profileId)?.baseCurrency ?: "INR"

        database.withTransaction {
            val existingAccounts =
                accountDao.getAllAccountsSync(profileId).associateBy { it.name.lowercase() }
            val existingCats = categoryDao.getAllCategoriesSync()
            val existingTxs = transactionDao.getAllTransactionsSync(profileId)
            val existingKeys =
                existingTxs.map { getDuplicateKey(it.timestamp, it.amount, it.note) }.toMutableSet()
            val existingTags = tagDao.getAllTagsSync().toMutableList()

            val resolved = resolveAndCreateEntities(
                entityMappings,
                existingAccounts,
                existingCats,
                profileId,
                profileBaseCurrency
            )
            val accountMap = resolved.accountMap
            val categoryMap = resolved.categoryMap
            val accountCurrencyMap = resolved.accountCurrencyMap

            var imported = 0
            var skipped = 0
            var rerouted = 0
            val errors = mutableListOf<ParseError>()
            val excludedAccounts =
                entityMappings.filter { it.entityType == EntityMapping.EntityType.ACCOUNT && it.isExcluded }
                    .map { it.unknownName.lowercase() }.toSet()

            val pendingTransfers = mutableMapOf<String, BudgetTransfer>()

            parseRows(inputStream, session.delimiter, session.encoding) { _, rowsSequence ->
                rowsSequence.forEachIndexed { idx, row ->
                    try {
                        val acc = row["account"] ?: ""
                        if (row["category name"]?.equals(
                                "Balance Correction",
                                ignoreCase = true
                            ) == true
                        ) {
                            val amount = normalizeAmount(row["amount"])
                            val timestamp = parseDate(row["date"] ?: "")
                            val note =
                                listOf(row["title"], row["note"]).filter { !it.isNullOrBlank() }
                                    .joinToString(" — ")
                            val t = BudgetTransfer(
                                idx, row, acc, timestamp, note, amount,
                                row["currency"] ?: accountCurrencyMap[acc.lowercase()]
                                ?: profileBaseCurrency,
                                PaymentMethod.MOBILE_PAYMENT, null, emptySet()
                            )
                            val key = "${t.timestamp / 60000}|${t.amount.abs().toPlainString()}"
                            val match = pendingTransfers.remove(key)
                            if (match != null) {
                                val (source, target) = if (t.amount.signum() < 0) t to match else match to t
                                val srcExc = source.account.lowercase() in excludedAccounts
                                val tgtExc = target.account.lowercase() in excludedAccounts
                                when {
                                    srcExc && tgtExc -> skipped += 2
                                    srcExc -> {
                                        val tid = accountMap[target.account.lowercase()]
                                        if (tid != null) {
                                            insertBudgetTx(
                                                target,
                                                tid,
                                                TransactionType.TRANSFER_INFLOW,
                                                target.amount.abs(),
                                                null,
                                                profileId,
                                                existingTags,
                                                existingKeys
                                            )
                                            imported++; rerouted++
                                        } else skipped++
                                    }

                                    tgtExc -> {
                                        val sid = accountMap[source.account.lowercase()]
                                        if (sid != null && handleDup(
                                                source,
                                                existingKeys,
                                                duplicatePolicy,
                                                selectedDuplicateRowIndices
                                            )
                                        ) {
                                            insertBudgetTx(
                                                source,
                                                sid,
                                                TransactionType.TRANSFER_OUTFLOW,
                                                source.amount.abs().negate(),
                                                null,
                                                profileId,
                                                existingTags,
                                                existingKeys
                                            )
                                            imported++; rerouted++
                                        } else skipped += 2
                                    }

                                    else -> {
                                        val sid = accountMap[source.account.lowercase()]
                                        val tid = accountMap[target.account.lowercase()]
                                        if (sid != null && tid != null && handleDup(
                                                source,
                                                existingKeys,
                                                duplicatePolicy,
                                                selectedDuplicateRowIndices
                                            )
                                        ) {
                                            insertBudgetTransferPair(
                                                source,
                                                target,
                                                sid,
                                                tid,
                                                profileId,
                                                existingTags,
                                                existingKeys
                                            )
                                            imported += 2
                                        } else skipped += 2
                                    }
                                }
                            } else {
                                pendingTransfers[key] = t
                            }
                        } else {
                            if (acc.lowercase() in excludedAccounts) {
                                skipped++
                            } else {
                                val amount = normalizeAmount(row["amount"])
                                val timestamp = parseDate(row["date"] ?: "")
                                val note =
                                    listOf(row["title"], row["note"]).filter { !it.isNullOrBlank() }
                                        .joinToString(" — ")
                                if (handleDup(
                                        idx,
                                        timestamp,
                                        amount,
                                        note,
                                        existingKeys,
                                        duplicatePolicy,
                                        selectedDuplicateRowIndices
                                    )
                                ) {
                                    val aid = accountMap[acc.lowercase()]
                                    if (aid != null) {
                                        val cat = row["category name"] ?: ""
                                        val sub = row["subcategory name"] ?: ""
                                        val leaf = sub.takeIf { it.isNotBlank() } ?: cat
                                        val cid = existingCats.find {
                                            it.name.equals(
                                                leaf,
                                                ignoreCase = true
                                            )
                                        }?.id ?: categoryMap[leaf.lowercase()]
                                        ?: Category.SYSTEM_OTHERS
                                        val isInc = row["income"]?.toBoolean() ?: false
                                        val type =
                                            if (isInc) TransactionType.INCOME else TransactionType.EXPENSE
                                        val finalAmount =
                                            if (type == TransactionType.INCOME) amount.abs() else amount.abs()
                                                .negate()
                                        val tags = if (row["type"]?.contains(
                                                "repetitive",
                                                true
                                            ) == true
                                        ) setOf("Planned") else emptySet()

                                        transactionDao.insertTransaction(
                                            TransactionEntity(
                                                name = leaf.take(50),
                                                amount = finalAmount,
                                                timestamp = timestamp,
                                                currency = row["currency"]
                                                    ?: accountCurrencyMap[acc.lowercase()]
                                                    ?: profileBaseCurrency,
                                                transactionType = type,
                                                accountId = aid,
                                                categoryId = cid,
                                                tags = resolveTagIds(tags, existingTags),
                                                paymentMethod = PaymentMethod.MOBILE_PAYMENT,
                                                note = note,
                                                profileId = profileId,
                                                status = TransactionStatus.CLEARED
                                            )
                                        )
                                        existingKeys.add(getDuplicateKey(timestamp, amount, note))
                                        imported++
                                    } else skipped++
                                } else skipped++
                            }
                        }
                    } catch (e: Exception) {
                        errors.add(ParseError(idx, e.message ?: "Row error"))
                    }
                    onProgress(rowProgress(idx, totalRows))
                }
            }

            pendingTransfers.values.forEach { t ->
                if (t.account.lowercase() in excludedAccounts) {
                    skipped++
                } else {
                    val aid = accountMap[t.account.lowercase()]
                    if (aid != null) {
                        val isOut = t.amount.signum() < 0
                        insertBudgetTx(
                            t,
                            aid,
                            if (isOut) TransactionType.TRANSFER_OUTFLOW else TransactionType.TRANSFER_INFLOW,
                            if (isOut) t.amount.abs().negate() else t.amount.abs(),
                            null,
                            profileId,
                            existingTags,
                            existingKeys
                        )
                        imported++
                    } else skipped++
                }
            }
            onProgress(1.0f)
            Result.success(ImportSummary(imported, skipped, errors, rerouted))
        }
    }

    private suspend fun executeGenericImport(
        session: CsvImportSession,
        inputStream: InputStream,
        duplicatePolicy: DuplicatePolicy,
        entityMappings: List<EntityMapping>,
        selectedDuplicateRowIndices: Set<Int>,
        totalRows: Int,
        onProgress: (Float) -> Unit
    ): Result<ImportSummary> = withContext(dispatcherProvider.io) {
        val profileId = settingsDataStore.currentProfileId.first()
        val profileBaseCurrency = profileDao.getProfileById(profileId)?.baseCurrency ?: "INR"

        database.withTransaction {
            val existingAccounts =
                accountDao.getAllAccountsSync(profileId).associateBy { it.name.lowercase() }
            val existingCats = categoryDao.getAllCategoriesSync()
            val existingTxs = transactionDao.getAllTransactionsSync(profileId)
            val existingKeys =
                existingTxs.map { getDuplicateKey(it.timestamp, it.amount, it.note) }.toMutableSet()
            val existingTags = tagDao.getAllTagsSync().toMutableList()

            val resolved = resolveAndCreateEntities(
                entityMappings,
                existingAccounts,
                existingCats,
                profileId,
                profileBaseCurrency
            )
            val accountMap = resolved.accountMap
            val categoryMap = resolved.categoryMap
            val accountCurrencyMap = resolved.accountCurrencyMap

            var imported = 0
            var skipped = 0
            val errors = mutableListOf<ParseError>()

            parseRows(inputStream, session.delimiter, session.encoding) { headers, rowsSequence ->
                rowsSequence.forEachIndexed { idx, row ->
                    try {
                        val acc =
                            session.mapping[OssWalletField.ACCOUNT]?.let { headers.getOrNull(it) }
                                ?.let { row[it] } ?: ""
                        val cat =
                            session.mapping[OssWalletField.CATEGORY]?.let { headers.getOrNull(it) }
                                ?.let { row[it] } ?: ""
                        val amount = normalizeAmount(session.mapping[OssWalletField.AMOUNT]?.let {
                            headers.getOrNull(it)
                        }?.let { row[it] })
                        val timestamp = parseDate(session.mapping[OssWalletField.DATE]?.let {
                            headers.getOrNull(it)
                        }?.let { row[it] } ?: "")
                        val note =
                            session.mapping[OssWalletField.NOTE]?.let { headers.getOrNull(it) }
                                ?.let { row[it] } ?: ""

                        if (!handleDup(
                                idx,
                                timestamp,
                                amount,
                                note,
                                existingKeys,
                                duplicatePolicy,
                                selectedDuplicateRowIndices
                            )
                        ) {
                            skipped++; return@forEachIndexed
                        }
                        val aid =
                            accountMap[acc.lowercase()] ?: run { skipped++; return@forEachIndexed }

                        val type = when {
                            row[headers.getOrNull(
                                session.mapping[OssWalletField.IS_TRANSFER] ?: -1
                            )]?.toBoolean() == true -> TransactionType.TRANSFER_OUTFLOW

                            row[headers.getOrNull(
                                session.mapping[OssWalletField.TYPE] ?: -1
                            )]?.contains("income", true) == true -> TransactionType.INCOME

                            else -> TransactionType.EXPENSE
                        }
                        val finalAmount =
                            if (type == TransactionType.EXPENSE || type == TransactionType.TRANSFER_OUTFLOW) amount.abs()
                                .negate() else amount.abs()

                        transactionDao.insertTransaction(
                            TransactionEntity(
                                name = cat.ifBlank { note.take(30) },
                                amount = finalAmount,
                                timestamp = timestamp,
                                currency = accountCurrencyMap[acc.lowercase()]
                                    ?: profileBaseCurrency,
                                transactionType = type,
                                accountId = aid,
                                categoryId = categoryMap[cat.lowercase()] ?: Category.SYSTEM_OTHERS,
                                tags = resolveTagIds(
                                    parseTags(
                                        row[headers.getOrNull(
                                            session.mapping[OssWalletField.LABELS] ?: -1
                                        )] ?: ""
                                    ), existingTags
                                ),
                                paymentMethod = resolveGenericMethod(
                                    row[headers.getOrNull(
                                        session.mapping[OssWalletField.PAYMENT_TYPE] ?: -1
                                    )]
                                ),
                                payee = row[headers.getOrNull(
                                    session.mapping[OssWalletField.PAYEE] ?: -1
                                )] ?: "",
                                note = note,
                                profileId = profileId,
                                status = TransactionStatus.CLEARED
                            )
                        )
                        existingKeys.add(getDuplicateKey(timestamp, amount, note))
                        imported++
                    } catch (e: Exception) {
                        errors.add(ParseError(idx, e.message ?: "Row error"))
                    }
                    onProgress(rowProgress(idx, totalRows))
                }
            }
            onProgress(1.0f)
            Result.success(ImportSummary(imported, skipped, errors))
        }
    }

    // ──────────────────────────────────────────────────────────────────────────
    // Internal Utilities
    // ──────────────────────────────────────────────────────────────────────────

    private data class BudgetTransfer(
        val idx: Int,
        val row: Map<String, String>,
        val account: String,
        val timestamp: Long,
        val note: String,
        val amount: BigDecimal,
        val currency: String,
        val method: PaymentMethod,
        val warranty: Long?,
        val tags: Set<String>
    )

    private fun parseBudgetTransfer(idx: Int, row: Map<String, String>): BudgetTransfer {
        val amount = resolveBaseAmount(row)
        val timestamp = parseDate(row["date"] ?: "")
        val note = row["note"] ?: ""
        val tags = parseTags(row["labels"] ?: "")
        val warranty = row["warranty_in_month"]?.toIntOrNull()
            ?.let { timestamp + (it * 2592000000L) }
        return BudgetTransfer(
            idx, row, row["account"] ?: "", timestamp, note, amount,
            row["currency"] ?: "INR", resolvePaymentMethod(row), warranty, tags
        )
    }

    private suspend fun insertBudgetTx(
        t: BudgetTransfer,
        aid: String,
        type: TransactionType,
        amount: BigDecimal,
        targetAid: String?,
        profileId: Int,
        existingTags: MutableList<TagEntity>,
        existingKeys: MutableSet<String>
    ) {
        transactionDao.insertTransaction(
            createTx(t, aid, type, amount, targetAid, profileId, existingTags)
        )
        existingKeys.add(getDuplicateKey(t.timestamp, t.amount, t.note))
    }

    private suspend fun insertBudgetTransferPair(
        source: BudgetTransfer,
        target: BudgetTransfer,
        sid: String,
        tid: String,
        profileId: Int,
        existingTags: MutableList<TagEntity>,
        existingKeys: MutableSet<String>
    ) {
        val rate = if (source.currency != target.currency && source.amount.signum() != 0) {
            target.amount.abs().divide(source.amount.abs(), 8, RoundingMode.HALF_UP)
        } else BigDecimal.ONE

        val sourceTxId = transactionDao._insertTransaction(
            createTx(
                source,
                sid,
                TransactionType.TRANSFER_OUTFLOW,
                source.amount.abs().negate(),
                tid,
                profileId,
                existingTags,
                target.currency,
                rate
            )
        )
        val targetTxId = transactionDao._insertTransaction(
            createTx(
                target,
                tid,
                TransactionType.TRANSFER_INFLOW,
                source.amount.abs().multiply(rate).setScale(8, RoundingMode.HALF_UP),
                sid,
                profileId,
                existingTags,
                source.currency,
                if (rate.signum() != 0) BigDecimal.ONE.divide(
                    rate,
                    8,
                    RoundingMode.HALF_UP
                ) else BigDecimal.ONE
            )
        )
        if (sourceTxId > 0 && targetTxId > 0) {
            transactionLinkDao.insertLinks(
                listOf(
                    TransactionLinkEntity(0, sourceTxId, targetTxId),
                    TransactionLinkEntity(0, targetTxId, sourceTxId)
                )
            )
            existingKeys.add(getDuplicateKey(source.timestamp, source.amount, source.note))
        }
    }

    private suspend fun createTx(
        t: BudgetTransfer,
        aid: String,
        type: TransactionType,
        amount: BigDecimal,
        targetAid: String?,
        profileId: Int,
        existingTags: MutableList<TagEntity>,
        targetCurrency: String? = null,
        rate: BigDecimal = BigDecimal.ONE
    ) =
        TransactionEntity(
            id = 0,
            name = t.note.ifBlank { "Transfer" }.take(50),
            amount = amount,
            timestamp = t.timestamp,
            currency = t.currency,
            transactionType = type,
            accountId = aid,
            categoryId = Category.SYSTEM_TRANSFER,
            tags = resolveTagIds(t.tags, existingTags),
            paymentMethod = t.method,
            payee = t.row["payee"] ?: "",
            note = t.note,
            sourcePlannedPaymentId = null,
            profileId = profileId,
            gpsLocation = buildGps(t.row),
            location = t.row["payee"]?.takeIf { it.isNotBlank() },
            warrantyExpiration = t.warranty,
            status = TransactionStatus.CLEARED,
            targetAccountId = targetAid,
            targetCurrency = targetCurrency,
            transferRate = rate
        )

    private fun handleDup(
        t: BudgetTransfer,
        keys: Set<String>,
        policy: DuplicatePolicy,
        indices: Set<Int>
    ) = handleDup(t.idx, t.timestamp, t.amount, t.note, keys, policy, indices)

    private fun handleDup(
        idx: Int,
        timestamp: Long,
        amount: BigDecimal,
        note: String,
        keys: Set<String>,
        policy: DuplicatePolicy,
        indices: Set<Int>
    ): Boolean {
        val key = getDuplicateKey(timestamp, amount, note)
        return if (key in keys) when (policy) {
            DuplicatePolicy.SKIP -> false
            DuplicatePolicy.IMPORT_BOTH -> true
            DuplicatePolicy.REPLACE_EXISTING -> idx in indices
        } else true
    }

    private fun getDuplicateKey(timestamp: Long, amount: BigDecimal, note: String): String =
        "${timestamp / 60000}|${amount.abs().toPlainString()}|$note"

    private fun resolvePaymentMethod(row: Map<String, String>): PaymentMethod =
        PaymentMethod.fromString(
            assetLoader.paymentTypeMap[row["payment_type"]] ?: "MOBILE_PAYMENT"
        )

    private fun resolveGenericMethod(s: String?): PaymentMethod =
        when (s?.uppercase()?.replace(" ", "_")) {
            "MOBILE_PAYMENT" -> PaymentMethod.MOBILE_PAYMENT
            "CREDIT_CARD" -> PaymentMethod.CREDIT_CARD
            "TRANSFER", "BANK_TRANSFER" -> PaymentMethod.BANK_TRANSFER
            "CASH" -> PaymentMethod.CASH
            else -> PaymentMethod.MOBILE_PAYMENT
        }

    private fun parseTags(s: String): Set<String> {
        if (s.isBlank()) return emptySet()
        val trimmed = s.trim()
        if (trimmed.startsWith("[") && trimmed.endsWith("]")) {
            return try {
                json.decodeFromString<Set<String>>(trimmed)
            } catch (_: Exception) {
                trimmed.removePrefix("[").removeSuffix("]")
                    .split("|", ";", ",")
                    .map { it.trim().trim('"').trim() }
                    .filter { it.isNotBlank() }.toSet()
            }
        }
        return trimmed.split("|", ";", ",")
            .map { it.trim().trim('"').trim() }
            .filter { it.isNotBlank() }.toSet()
    }

    private suspend fun resolveTagIds(
        names: Set<String>,
        list: MutableList<TagEntity>
    ): Set<String> {
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

    private fun buildGps(row: Map<String, String>): String? = row["gps_latitude"]?.toDoubleOrNull()
        ?.let { lat -> row["gps_longitude"]?.toDoubleOrNull()?.let { lon -> "$lat,$lon" } }

    private fun randomSuffix(): String =
        (('a'..'z') + ('0'..'9')).shuffled().take(6).joinToString("")

    companion object {
        private val BRAND_COLORS = listOf(
            "#004D5F",
            "#1A237E",
            "#1B5E20",
            "#B71C1C",
            "#4A148C",
            "#E65100",
            "#006064",
            "#212121",
            "#795548",
            "#607D8B"
        )
    }
}
