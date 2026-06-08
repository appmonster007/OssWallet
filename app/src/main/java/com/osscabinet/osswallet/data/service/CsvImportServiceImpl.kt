package com.osscabinet.ossWallet.data.service

import androidx.room.withTransaction
import com.osscabinet.ossWallet.data.local.AppDatabase
import com.osscabinet.ossWallet.data.local.SettingsDataStore
import com.osscabinet.ossWallet.data.local.dao.AccountDao
import com.osscabinet.ossWallet.data.local.dao.CategoryDao
import com.osscabinet.ossWallet.data.local.dao.TransactionDao
import com.osscabinet.ossWallet.data.local.entity.AccountEntity
import com.osscabinet.ossWallet.data.local.entity.TransactionEntity
import com.osscabinet.ossWallet.domain.model.PaymentMethod
import com.osscabinet.ossWallet.domain.model.TransactionStatus
import com.osscabinet.ossWallet.domain.model.TransactionType
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
import java.io.InputStream
import java.math.BigDecimal
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
    private val database: AppDatabase,
    private val settingsDataStore: SettingsDataStore
) : CsvImportService {

    private val budgetBakersHeaders = setOf(
        "account", "category", "currency", "amount", "ref_currency_amount",
        "type", "payment_type", "payment_type_local", "note", "date",
        "gps_latitude", "gps_longitude", "gps_accuracy_in_meters",
        "warranty_in_month", "transfer", "payee", "labels"
    )

    override suspend fun parseHeaders(inputStream: InputStream): CsvImportSession =
        withContext(Dispatchers.IO) {
            val text = inputStream.bufferedReader().readLines()
            if (text.isEmpty()) return@withContext CsvImportSession(emptyList(), emptyList(), emptyMap(), null)

            val firstLine = text.first()
            val delimiter = if (firstLine.contains(';')) ';' else ','
            val headers = firstLine.split(delimiter).map { it.trim().lowercase().replace("\"", "") }

            val sourceApp = when {
                budgetBakersHeaders.any { it in headers } -> ImportSourceApp.BUDGET_BAKERS
                else -> ImportSourceApp.GENERIC
            }

            val mapping = buildInitialMapping(headers, sourceApp)
            val previewRows = text.drop(1).take(5).map { it.split(delimiter).map { c -> c.trim().replace("\"", "") } }

            CsvImportSession(headers, previewRows, mapping, sourceApp, delimiter)
        }

    private fun buildInitialMapping(headers: List<String>, sourceApp: ImportSourceApp): Map<OssWalletField, Int?> {
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

    override suspend fun generatePreview(session: CsvImportSession, inputStream: InputStream): ImportPreview =
        withContext(Dispatchers.IO) {
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
                "${tx.timestamp / 60000}_${tx.amount.toPlainString()}_${tx.accountId}_${tx.note}"
            }.toSet()

            val dateFormats = listOf(
                SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US),
                SimpleDateFormat("yyyy-MM-dd", Locale.US),
                SimpleDateFormat("dd/MM/yyyy", Locale.US),
                SimpleDateFormat("MM/dd/yyyy", Locale.US)
            )

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

                    val amount = amountStr.toBigDecimalOrNull() ?: BigDecimal.ZERO
                    val timestamp = dateFormats.firstNotNullOfOrNull { fmt ->
                        try { fmt.parse(dateStr)?.time } catch (_: Exception) { null }
                    } ?: 0L

                    val dupKey = "${timestamp / 60000}_${amount.abs().toPlainString()}_${accountName}_$note"
                    if (dupKey in existingKeys) {
                        duplicates.add(DuplicateGroup(
                            key = dupKey,
                            existingNote = note,
                            incomingRowIndex = idx,
                            incomingValues = cols
                        ))
                    } else {
                        validRows++
                    }
                } catch (e: Exception) {
                    parseErrors.add(ParseError(idx, e.message ?: "Parse error"))
                }
            }

            ImportPreview(
                totalRows = lines.size,
                validRows = validRows,
                duplicates = duplicates,
                newAccounts = newAccounts.toList(),
                newCategories = newCategories.toList(),
                parseErrors = parseErrors
            )
        }

    override suspend fun executeImport(
        session: CsvImportSession,
        inputStream: InputStream,
        duplicatePolicy: DuplicatePolicy,
        entityMappings: List<EntityMapping>,
        selectedDuplicateRowIndices: Set<Int>
    ): Result<ImportSummary> = withContext(Dispatchers.IO) {
        try {
            val profileId = settingsDataStore.currentProfileId.first()
            val lines = inputStream.bufferedReader().readLines().drop(1)
            val existingAccounts = accountDao.getAllAccountsSync().associateBy { it.name.lowercase() }
            val existingCategories = categoryDao.getAllCategoriesSync().associateBy { it.name.lowercase() }
            val existingTxs = transactionDao.getAllTransactionsSync()
            val existingKeys = existingTxs.map { tx ->
                "${tx.timestamp / 60000}_${tx.amount.toPlainString()}_${tx.accountId}_${tx.note}"
            }.toSet()

            val dateFormats = listOf(
                SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US),
                SimpleDateFormat("yyyy-MM-dd", Locale.US),
                SimpleDateFormat("dd/MM/yyyy", Locale.US)
            )

            val accountMap = mutableMapOf<String, String>()
            val categoryMap = mutableMapOf<String, Long>()

            existingAccounts.forEach { (k, v) -> accountMap[k] = v.id }
            existingCategories.forEach { (k, v) -> categoryMap[k] = v.id }

            var imported = 0
            var skipped = 0
            val errors = mutableListOf<ParseError>()

            database.withTransaction {
                entityMappings.forEach { em ->
                    when (em.entityType) {
                        EntityMapping.EntityType.ACCOUNT -> {
                            val id = em.existingId ?: UUID.randomUUID().toString()
                            if (em.existingId == null) {
                                accountDao.upsertAccount(AccountEntity(
                                    id = id, name = em.unknownName,
                                    currencyCode = "INR", structureType = "regular",
                                    colorHex = "#607D8B", icon = "wallet",
                                    allowedCurrencies = "INR",
                                    profileId = profileId, isArchived = false, displayOrder = 999
                                ))
                            }
                            accountMap[em.unknownName.lowercase()] = id
                        }
                        EntityMapping.EntityType.CATEGORY -> {
                            val mapId = em.existingId?.toLongOrNull() ?: 11000L
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
                        val timestamp = dateFormats.firstNotNullOfOrNull { fmt ->
                            try { fmt.parse(dateStr)?.time } catch (_: Exception) { null }
                        } ?: 0L

                        val dupKey = "${timestamp / 60000}_${amount.abs().toPlainString()}_${accountName}_$note"
                        if (dupKey in existingKeys) {
                            when (duplicatePolicy) {
                                DuplicatePolicy.SKIP -> { skipped++; return@forEachIndexed }
                                DuplicatePolicy.IMPORT_BOTH -> { /* proceed */ }
                                DuplicatePolicy.REPLACE_EXISTING -> { /* proceed */ }
                            }
                            if (idx !in selectedDuplicateRowIndices && duplicatePolicy != DuplicatePolicy.IMPORT_BOTH) {
                                skipped++; return@forEachIndexed
                            }
                        }

                        val accountId = accountMap[accountName.lowercase()]
                        if (accountId == null) { skipped++; return@forEachIndexed }

                        val categoryId = categoryMap[categoryName.lowercase()] ?: 11000L

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

                        transactionDao.insertTransaction(TransactionEntity(
                            id = 0,
                            name = categoryName.ifBlank { note.take(30) },
                            amount = finalAmount,
                            timestamp = timestamp,
                            currency = "INR",
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
                        ))
                        imported++
                    } catch (e: Exception) {
                        errors.add(ParseError(idx, e.message ?: "Row error"))
                    }
                }
            }
            Result.success(ImportSummary(imported, skipped, errors))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
