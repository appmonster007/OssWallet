package com.osscabinet.ossWallet.data.repository

import android.content.Context
import androidx.core.net.toUri
import androidx.room.withTransaction
import com.osscabinet.ossWallet.data.local.AppDatabase
import com.osscabinet.ossWallet.data.local.SettingsDataStore
import com.osscabinet.ossWallet.data.local.dao.AccountDao
import com.osscabinet.ossWallet.data.local.dao.BudgetDao
import com.osscabinet.ossWallet.data.local.dao.CategoryDao
import com.osscabinet.ossWallet.data.local.dao.CurrencyDao
import com.osscabinet.ossWallet.data.local.dao.FilterDao
import com.osscabinet.ossWallet.data.local.dao.LoanDao
import com.osscabinet.ossWallet.data.local.dao.LoanPaymentDao
import com.osscabinet.ossWallet.data.local.dao.PlannedPaymentDao
import com.osscabinet.ossWallet.data.local.dao.PlannedPaymentOccurrenceDao
import com.osscabinet.ossWallet.data.local.dao.ProfileDao
import com.osscabinet.ossWallet.data.local.dao.TagDao
import com.osscabinet.ossWallet.data.local.dao.TemplateDao
import com.osscabinet.ossWallet.data.local.dao.TransactionDao
import com.osscabinet.ossWallet.data.local.dao.TransactionLinkDao
import com.osscabinet.ossWallet.data.local.entity.AccountEntity
import com.osscabinet.ossWallet.data.local.entity.BudgetEntity
import com.osscabinet.ossWallet.data.local.entity.CategoryEntity
import com.osscabinet.ossWallet.data.local.entity.CurrencyRateEntity
import com.osscabinet.ossWallet.data.local.entity.FilterEntity
import com.osscabinet.ossWallet.data.local.entity.LoanEntity
import com.osscabinet.ossWallet.data.local.entity.LoanPaymentEntity
import com.osscabinet.ossWallet.data.local.entity.PlannedPaymentEntity
import com.osscabinet.ossWallet.data.local.entity.PlannedPaymentOccurrenceEntity
import com.osscabinet.ossWallet.data.local.entity.ProfileEntity
import com.osscabinet.ossWallet.data.local.entity.RecurrenceScheduleEntity
import com.osscabinet.ossWallet.data.local.entity.TagEntity
import com.osscabinet.ossWallet.data.local.entity.TemplateEntity
import com.osscabinet.ossWallet.data.local.entity.TransactionEntity
import com.osscabinet.ossWallet.data.local.entity.TransactionLinkEntity
import com.osscabinet.ossWallet.data.repository.util.CsvProcessor
import com.osscabinet.ossWallet.domain.model.OccurrenceStatus
import com.osscabinet.ossWallet.domain.model.PaymentMethod
import com.osscabinet.ossWallet.domain.model.PlannedPaymentStatus
import com.osscabinet.ossWallet.domain.model.TransactionStatus
import com.osscabinet.ossWallet.domain.model.TransactionType
import com.osscabinet.ossWallet.domain.model.timeline.TimelineRange
import com.osscabinet.ossWallet.domain.repository.BackupRepository
import com.osscabinet.ossWallet.domain.repository.BackupType
import com.osscabinet.ossWallet.domain.util.splitByComma
import com.osscabinet.ossWallet.domain.model.*
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.InputStream
import java.io.OutputStream
import java.math.BigDecimal
import java.util.zip.ZipInputStream
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class BackupRepositoryImpl @Inject constructor(
    private val profileDao: ProfileDao,
    private val accountDao: AccountDao,
    private val transactionDao: TransactionDao,
    private val categoryDao: CategoryDao,
    private val tagDao: TagDao,
    private val templateDao: TemplateDao,
    private val transactionLinkDao: TransactionLinkDao,
    private val budgetDao: BudgetDao,
    private val loanDao: LoanDao,
    private val loanPaymentDao: LoanPaymentDao,
    private val plannedPaymentDao: PlannedPaymentDao,
    private val plannedPaymentOccurrenceDao: PlannedPaymentOccurrenceDao,
    private val filterDao: FilterDao,
    private val currencyDao: CurrencyDao,
    private val database: AppDatabase,
    private val settingsDataStore: SettingsDataStore,
    @ApplicationContext private val context: Context
) : BackupRepository {

    override suspend fun exportData(uriString: String, type: BackupType): Result<Unit> =
        withContext(Dispatchers.IO) {
            try {
                val uri = uriString.toUri()
                context.contentResolver.openOutputStream(uri)?.use { outputStream ->
                    when (type) {
                        BackupType.CSV_ZIP -> createZipBackup(outputStream)
                        BackupType.SQLITE -> exportSqlite(outputStream)
                    }
                }
                Result.success(Unit)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }

    private suspend fun createZipBackup(outputStream: OutputStream) {
        val zos = java.util.zip.ZipOutputStream(outputStream)

        writeCsvToZip(
            zos, "profiles.csv", listOf("id", "name", "baseCurrency", "type"),
            profileDao.getAllProfilesSync()
                .map { listOf(it.id, it.name, it.baseCurrency, it.type) })

        writeCsvToZip(
            zos,
            "accounts.csv",
            listOf(
                "id",
                "name",
                "currencyCode",
                "structureType",
                "colorHex",
                "icon",
                "allowedCurrencies",
                "profileId",
                "isArchived",
                "displayOrder"
            ),
            accountDao.getAllAccountsSync().map {
                listOf(
                    it.id,
                    it.name,
                    it.currencyCode,
                    it.structureType,
                    it.colorHex,
                    it.icon,
                    it.allowedCurrencies,
                    it.profileId,
                    it.isArchived,
                    it.displayOrder
                )
            })

        writeCsvToZip(
            zos,
            "categories.csv",
            listOf("id", "name", "iconName", "colorHex", "isDefault", "parentId"),
            categoryDao.getAllCategoriesSync().map {
                listOf(
                    it.id,
                    it.name,
                    it.iconName,
                    it.colorHex,
                    it.isDefault,
                    it.parentId
                )
            })

        writeCsvToZip(
            zos,
            "transactions.csv",
            listOf(
                "id",
                "name",
                "amount",
                "timestamp",
                "currency",
                "transactionType",
                "accountId",
                "targetAccountId",
                "targetCurrency",
                "transferRate",
                "categoryId",
                "tags",
                "paymentMethod",
                "payee",
                "note",
                "sourcePlannedPaymentId",
                "profileId",
                "gpsLocation",
                "location",
                "warrantyExpiration",
                "status"
            ),
            transactionDao.getAllTransactionsSync().map {
                listOf(
                    it.id,
                    it.name,
                    it.amount,
                    it.timestamp,
                    it.currency,
                    it.transactionType,
                    it.accountId,
                    it.targetAccountId,
                    it.targetCurrency,
                    it.transferRate,
                    it.categoryId,
                    it.tags,
                    it.paymentMethod.name,
                    it.payee,
                    it.note,
                    it.sourcePlannedPaymentId,
                    it.profileId,
                    it.gpsLocation,
                    it.location,
                    it.warrantyExpiration,
                    it.status.name
                )
            })

        writeCsvToZip(
            zos, "transaction_links.csv", listOf("id", "transactionId", "linkedTransactionId"),
            transactionLinkDao.getAllLinksSync()
                .map { listOf(it.id, it.transactionId, it.linkedTransactionId) })

        writeCsvToZip(
            zos,
            "budgets.csv",
            listOf(
                "id",
                "name",
                "currency",
                "limitAmount",
                "alertThreshold",
                "rollover",
                "isArchived",
                "profileId",
                "filterId"
            ),
            budgetDao.getAllBudgetsSync().map {
                val b = it.budget
                listOf(
                    b.id,
                    b.name,
                    b.currency,
                    b.limitAmount,
                    b.alertThreshold,
                    b.rollover,
                    b.isArchived,
                    b.profileId,
                    b.filterId
                )
            })

        writeCsvToZip(
            zos,
            "templates.csv",
            listOf(
                "id",
                "name",
                "categoryId",
                "amount",
                "transactionType",
                "icon",
                "colorHex",
                "accountId",
                "targetAccountId",
                "targetCurrency",
                "transferRate",
                "note",
                "payee",
                "location",
                "paymentMethod",
                "currency",
                "tags",
                "isStarred",
                "displayOrder",
                "profileId",
                "transactionStatus"
            ),
            templateDao.getAllTemplatesSync().map {
                listOf(
                    it.id,
                    it.name,
                    it.categoryId,
                    it.amount,
                    it.transactionType,
                    it.icon,
                    it.colorHex,
                    it.accountId,
                    it.targetAccountId,
                    it.targetCurrency,
                    it.transferRate,
                    it.note,
                    it.payee,
                    it.location,
                    it.paymentMethod?.name,
                    it.currency,
                    it.tags,
                    it.isStarred,
                    it.displayOrder,
                    it.profileId,
                    it.transactionStatus.name
                )
            })

        writeCsvToZip(
            zos, "planned_payments.csv", listOf(
                "id", "autoConfirm", "status", "profileId", "templateId",
                "recurrenceType", "interval", "dayOfWeek", "dayOfMonth", "recurrenceCronExpression",
                "startDate", "endDate", "nextDueDate", "lastTriggeredDate"
            ),
            plannedPaymentDao.getAllPlannedPaymentsSync().map {
                listOf(
                    it.payment.id,
                    it.payment.autoConfirm,
                    it.payment.status.name,
                    it.payment.profileId,
                    it.payment.templateId,
                    it.payment.schedule.recurrenceType,
                    it.payment.schedule.interval,
                    it.payment.schedule.dayOfWeek,
                    it.payment.schedule.dayOfMonth,
                    it.payment.schedule.recurrenceCronExpression,
                    it.payment.schedule.startDate,
                    it.payment.schedule.endDate,
                    it.payment.schedule.nextDueDate,
                    it.payment.schedule.lastTriggeredDate
                )
            })

        writeCsvToZip(
            zos,
            "planned_payment_occurrences.csv",
            listOf("id", "plannedPaymentId", "scheduledDate", "generatedTransactionId", "status"),
            plannedPaymentOccurrenceDao.getAllOccurrencesSync().map {
                listOf(
                    it.id,
                    it.plannedPaymentId,
                    it.scheduledDate,
                    it.generatedTransactionId,
                    it.status.name
                )
            })

        writeCsvToZip(
            zos, "tags.csv", listOf("id", "name", "colorHex"),
            tagDao.getAllTagsSync().map { listOf(it.id, it.name, it.colorHex) })

        writeCsvToZip(
            zos,
            "loans.csv",
            listOf(
                "id",
                "name",
                "principalAmount",
                "currency",
                "interestRate",
                "startDate",
                "lenderOrBorrowerName",
                "loanType",
                "linkedAccountId",
                "expectedSettlementDate",
                "status",
                "profileId",
                "note",
                "filterId"
            ),
            loanDao.getAllLoansSync().map {
                val l = it.loan
                listOf(
                    l.id,
                    l.name,
                    l.principalAmount,
                    l.currency,
                    l.interestRate,
                    l.startDate,
                    l.lenderOrBorrowerName,
                    l.loanType,
                    l.linkedAccountId,
                    l.expectedSettlementDate,
                    l.status,
                    l.profileId,
                    l.note,
                    l.filterId
                )
            })

        writeCsvToZip(
            zos,
            "loan_payments.csv",
            listOf("id", "loanId", "amount", "date", "transactionId", "note"),
            loanPaymentDao.getAllPaymentsSync()
                .map { listOf(it.id, it.loanId, it.amount, it.date, it.transactionId, it.note) })

        writeCsvToZip(
            zos,
            "filters.csv",
            listOf(
                "id",
                "name",
                "timelineRange",
                "queryNote",
                "isNoteRegex",
                "queryPayee",
                "isPayeeRegex",
                "categoryIds",
                "accountIds",
                "startDate",
                "endDate",
                "paymentMethods",
                "hasWarranty",
                "warrantyExpirationStart",
                "warrantyExpirationEnd",
                "minAmount",
                "maxAmount",
                "transactionTypes",
                "currencies",
                "statuses",
                "linkedTransactionIds",
                "tags",
                "excludeFuture"
            ),
            filterDao.getAllFiltersSync().map {
                listOf(
                    it.id,
                    it.name,
                    Json.encodeToString(it.timelineRange),
                    it.queryNote,
                    it.isNoteRegex,
                    it.queryPayee,
                    it.isPayeeRegex,
                    Json.encodeToString(it.categoryIds),
                    Json.encodeToString(it.accountIds),
                    it.startDate,
                    it.endDate,
                    Json.encodeToString(it.paymentMethods),
                    it.hasWarranty,
                    it.warrantyExpirationStart,
                    it.warrantyExpirationEnd,
                    it.minAmount,
                    it.maxAmount,
                    Json.encodeToString(it.transactionTypes),
                    Json.encodeToString(it.currencies),
                    Json.encodeToString(it.statuses),
                    Json.encodeToString(it.linkedTransactionIds),
                    Json.encodeToString(it.tags),
                    it.excludeFuture
                )
            })

        writeCsvToZip(
            zos,
            "currency_rates.csv",
            listOf("currencyCode", "baseCurrency", "rate", "timestamp", "nextUpdate"),
            currencyDao.getAllRates().map {
                listOf(
                    it.currencyCode,
                    it.baseCurrency,
                    it.rate,
                    it.timestamp,
                    it.nextUpdate
                )
            })

        zos.close()
    }

    private fun writeCsvToZip(
        zos: java.util.zip.ZipOutputStream,
        fileName: String,
        headers: List<String>,
        rows: List<List<Any?>>
    ) {
        zos.putNextEntry(java.util.zip.ZipEntry(fileName))
        val csv = CsvProcessor.buildCsv(headers, rows)
        zos.write(csv.toByteArray())
        zos.closeEntry()
    }

    private fun exportSqlite(outputStream: OutputStream) {
        val dbFile = context.getDatabasePath("osswallet_manager_db")
        if (dbFile.exists()) {
            FileInputStream(dbFile).use { it.copyTo(outputStream) }
        }
    }

    override suspend fun importData(uriString: String, type: BackupType): Result<Unit> =
        withContext(Dispatchers.IO) {
            try {
                val uri = uriString.toUri()
                context.contentResolver.openInputStream(uri)?.use { inputStream ->
                    when (type) {
                        BackupType.CSV_ZIP -> restoreZipBackup(inputStream)
                        BackupType.SQLITE -> restoreSqliteBackup(inputStream)
                    }
                }
                Result.success(Unit)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }

    override suspend fun deleteAllData(): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            database.withTransaction {
                // 1. Clear linked/junction tables first (no dependencies)
                transactionLinkDao.clearAll()
                transactionDao.clearAllCrossRefs()

                // 2. Clear dependent data (references accounts/categories/loans)
                loanPaymentDao.clearAll()
                plannedPaymentOccurrenceDao.clearAll()

                // 3. Clear data that references categories (due to RESTRICT)
                transactionDao.clearAll()
                templateDao.clearAll()
                budgetDao.clearAll()

                // 4. Clear base entities (reference profile)
                loanDao.clearAll()
                plannedPaymentDao.clearAll()
                accountDao.clearAll()
                filterDao.clearAll()

                // 5. Clear infrastructure
                tagDao.clearAll()
                // 5. Clear infrastructure
                tagDao.clearAll()
                categoryDao.clearAll()
                currencyDao.deleteAllRates()

                // 6. Clear profiles last
                profileDao.clearAll()

                // 7. Re-seed essential system categories (Reserved IDs)
                SystemCategory.entries.forEach { sc ->
                    categoryDao.insertCategory(
                        CategoryEntity(
                            id = sc.id,
                            name = sc.displayName,
                            iconName = sc.iconName,
                            colorHex = sc.colorHex,
                            isDefault = true,
                            parentId = sc.parentId,
                            nature = sc.nature
                        )
                    )
                }

                // 8. Re-seed system tags
                SystemTag.entries.forEach { st ->
                    tagDao.insertTag(
                        TagEntity(
                            id = st.id,
                            name = st.tagName,
                            colorHex = st.colorHex
                        )
                    )
                }
            }

            // 9. Clear user preferences
            settingsDataStore.clearAll()

            // 10. Force process kill to ensure fresh state on next launch
            android.os.Process.killProcess(android.os.Process.myPid())

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun restoreSqliteBackup(inputStream: InputStream) {
        val dbFile = context.getDatabasePath("osswallet_manager_db")
        database.close()
        FileOutputStream(dbFile).use { output ->
            inputStream.copyTo(output)
        }
        // Force process kill to ensure DB is re-opened with new file on next launch
        android.os.Process.killProcess(android.os.Process.myPid())
    }

    private suspend fun restoreZipBackup(inputStream: InputStream) {
        val tempDir = File(context.cacheDir, "restore_temp")
        tempDir.deleteRecursively()
        tempDir.mkdirs()

        ZipInputStream(inputStream).use { zis ->
            var entry = zis.nextEntry
            while (entry != null) {
                val file = File(tempDir, entry.name)
                FileOutputStream(file).use { fos -> zis.copyTo(fos) }
                entry = zis.nextEntry
            }
        }

        database.withTransaction {
            profileDao.clearAll()
            accountDao.clearAll()
            categoryDao.clearAll()
            transactionDao.clearAll()
            transactionLinkDao.clearAll()
            budgetDao.clearAll()
            templateDao.clearAll()
            plannedPaymentDao.clearAll()
            plannedPaymentOccurrenceDao.clearAll()
            tagDao.clearAll()
            loanDao.clearAll()
            loanPaymentDao.clearAll()
            filterDao.clearAll()

            restoreProfiles(File(tempDir, "profiles.csv"))
            restoreAccounts(File(tempDir, "accounts.csv"))
            restoreCategories(File(tempDir, "categories.csv"))
            restoreTransactions(File(tempDir, "transactions.csv"))
            restoreLinks(File(tempDir, "transaction_links.csv"))
            restoreBudgets(File(tempDir, "budgets.csv"))
            restoreTemplates(File(tempDir, "templates.csv"))
            restorePlannedPayments(File(tempDir, "planned_payments.csv"))
            restorePlannedPaymentOccurrences(File(tempDir, "planned_payment_occurrences.csv"))
            restoreTags(File(tempDir, "tags.csv"))
            restoreLoans(File(tempDir, "loans.csv"))
            restoreLoanPayments(File(tempDir, "loan_payments.csv"))
            restoreFilters(File(tempDir, "filters.csv"))
            restoreCurrencyRates(File(tempDir, "currency_rates.csv"))
        }
        tempDir.deleteRecursively()
    }

    private suspend fun restoreProfiles(file: File) = file.takeIf { it.exists() }?.let {
        CsvProcessor.parseCsv(it).forEach { p ->
            profileDao.insertProfile(
                ProfileEntity(
                    id = p["id"]?.toInt() ?: 0,
                    name = p["name"] ?: "",
                    baseCurrency = p["baseCurrency"] ?: "USD",
                    type = p["type"] ?: "PERSONAL"
                )
            )
        }
    }

    private suspend fun restoreAccounts(file: File) = file.takeIf { it.exists() }?.let {
        CsvProcessor.parseCsv(it).forEach { p ->
            accountDao.upsertAccount(
                AccountEntity(
                    id = p["id"] ?: "",
                    name = p["name"] ?: "",
                    currencyCode = p["currencyCode"] ?: "USD",
                    structureType = p["structureType"] ?: "regular",
                    colorHex = p["colorHex"] ?: "#000000",
                    icon = p["icon"] ?: "",
                    allowedCurrencies = p["allowedCurrencies"] ?: "USD",
                    profileId = p["profileId"]?.toIntOrNull() ?: 1,
                    isArchived = p["isArchived"]?.toBoolean() ?: false,
                    displayOrder = p["displayOrder"]?.toIntOrNull() ?: 0
                )
            )
        }
    }

    private suspend fun restoreCategories(file: File) = file.takeIf { it.exists() }?.let {
        CsvProcessor.parseCsv(it).forEach { p ->
            categoryDao.insertCategory(
                CategoryEntity(
                    id = p["id"]?.toLong() ?: 0,
                    name = p["name"] ?: "",
                    iconName = p["iconName"] ?: p["icon"] ?: "",
                    colorHex = p["colorHex"]?.toIntOrNull() ?: 0,
                    isDefault = p["isDefault"]?.toBoolean() ?: false,
                    parentId = p["parentId"]?.toLongOrNull()
                )
            )
        }
    }

    private suspend fun restoreTransactions(file: File) = file.takeIf { it.exists() }?.let {
        CsvProcessor.parseCsv(it).forEach { p ->
            transactionDao.insertTransaction(
                TransactionEntity(
                    id = p["id"]?.toLong() ?: 0,
                    name = p["name"] ?: "",
                    amount = p["amount"]?.toBigDecimalOrNull() ?: BigDecimal.ZERO,
                    timestamp = p["timestamp"]?.toLongOrNull() ?: 0,
                    currency = p["currency"] ?: "USD",
                    transactionType = TransactionType.fromValue(p["transactionType"]),
                    accountId = p["accountId"],
                    categoryId = p["categoryId"]?.toLongOrNull() ?: 1002,
                    tags = p["tags"]?.splitByComma()?.toSet() ?: emptySet(),
                    targetAccountId = p["targetAccountId"],
                    targetCurrency = p["targetCurrency"],
                    transferRate = p["transferRate"]?.toBigDecimalOrNull() ?: BigDecimal.ONE,
                    paymentMethod = p["paymentMethod"]?.let { PaymentMethod.fromString(it) }
                        ?: PaymentMethod.CASH,
                    payee = p["payee"] ?: "",
                    note = p["note"] ?: "",
                    sourcePlannedPaymentId = p["sourcePlannedPaymentId"]?.toLongOrNull(),
                    profileId = p["profileId"]?.toIntOrNull() ?: 1,
                    gpsLocation = p["gpsLocation"],
                    location = p["location"],
                    warrantyExpiration = p["warrantyExpiration"]?.toLongOrNull(),
                    status = p["status"]?.let { TransactionStatus.fromString(it) }
                        ?: TransactionStatus.CLEARED
                ))
        }
    }

    private suspend fun restoreLinks(file: File) = file.takeIf { it.exists() }?.let {
        CsvProcessor.parseCsv(it).forEach { p ->
            transactionLinkDao.insertLink(
                TransactionLinkEntity(
                    id = p["id"]?.toLong() ?: 0,
                    transactionId = p["transactionId"]?.toLong() ?: 0,
                    linkedTransactionId = p["linkedTransactionId"]?.toLong() ?: 0
                )
            )
        }
    }

    private suspend fun restoreTemplates(file: File) = file.takeIf { it.exists() }?.let {
        CsvProcessor.parseCsv(it).forEach { p ->
            templateDao.insertTemplate(
                TemplateEntity(
                    id = p["id"]?.toLong() ?: 0,
                    name = p["name"] ?: "",
                    categoryId = p["categoryId"]?.toLongOrNull() ?: 1002,
                    amount = p["amount"]?.toBigDecimalOrNull() ?: BigDecimal.ZERO,
                    transactionType = TransactionType.fromInt(
                        p["transactionType"]?.toIntOrNull() ?: 0
                    ),
                    icon = p["icon"],
                    colorHex = p["colorHex"],
                    accountId = p["accountId"],
                    targetAccountId = p["targetAccountId"],
                    targetCurrency = p["targetCurrency"],
                    transferRate = p["transferRate"]?.toBigDecimalOrNull() ?: BigDecimal.ONE,
                    note = p["note"],
                    payee = p["payee"],
                    location = p["location"],
                    paymentMethod = p["paymentMethod"]?.let { PaymentMethod.fromString(it) },
                    currency = p["currency"] ?: "USD",
                    tags = p["tags"]?.splitByComma()?.toSet() ?: emptySet(),
                    isStarred = p["isStarred"]?.toBoolean() ?: false,
                    displayOrder = p["displayOrder"]?.toIntOrNull() ?: 0,
                    profileId = p["profileId"]?.toIntOrNull() ?: 1,
                    transactionStatus = p["transactionStatus"]?.let {
                        TransactionStatus.fromString(
                            it
                        )
                    } ?: TransactionStatus.CLEARED
                ))
        }
    }

    private suspend fun restoreBudgets(file: File) = file.takeIf { it.exists() }?.let {
        CsvProcessor.parseCsv(it).forEach { p ->
            val filterId = p["filterId"]?.toLongOrNull()
            if (filterId != null) {
                budgetDao.insert(
                    BudgetEntity(
                        id = p["id"]?.toLong() ?: 0,
                        name = p["name"] ?: "",
                        currency = p["currency"] ?: "USD",
                        limitAmount = p["limitAmount"]?.toBigDecimalOrNull() ?: BigDecimal.ZERO,
                        alertThreshold = p["alertThreshold"]?.toFloatOrNull() ?: 0.8f,
                        rollover = p["rollover"]?.toBoolean() ?: false,
                        isArchived = p["isArchived"]?.toBoolean() ?: false,
                        profileId = p["profileId"]?.toIntOrNull() ?: 1,
                        filterId = filterId
                    )
                )
            }
        }
    }

    private suspend fun restorePlannedPayments(file: File) = file.takeIf { it.exists() }?.let {
        CsvProcessor.parseCsv(it).forEach { p ->
            val templateId = p["templateId"]?.toLongOrNull()
            if (templateId != null) {
                plannedPaymentDao.insert(
                    PlannedPaymentEntity(
                        id = p["id"]?.toLong() ?: 0,
                        schedule = RecurrenceScheduleEntity(
                            recurrenceType = p["recurrenceType"] ?: "MONTHLY",
                            interval = p["interval"]?.toIntOrNull() ?: 1,
                            dayOfWeek = p["dayOfWeek"]?.toIntOrNull(),
                            dayOfMonth = p["dayOfMonth"]?.toIntOrNull(),
                            recurrenceCronExpression = p["recurrenceCronExpression"],
                            startDate = p["startDate"]?.toLongOrNull() ?: 0,
                            endDate = p["endDate"]?.toLongOrNull(),
                            nextDueDate = p["nextDueDate"]?.toLongOrNull() ?: 0,
                            lastTriggeredDate = p["lastTriggeredDate"]?.toLongOrNull()
                        ),
                        autoConfirm = p["autoConfirm"]?.toBoolean() ?: false,
                        status = p["status"]?.let { PlannedPaymentStatus.fromString(it) }
                            ?: PlannedPaymentStatus.ACTIVE,
                        profileId = p["profileId"]?.toIntOrNull() ?: 1,
                        templateId = templateId
                    ))
            } else {
                // Handle legacy data without templateId if necessary
                // For now, if we force non-nullable, we might need to create a dummy template or skip
            }
        }
    }

    private suspend fun restorePlannedPaymentOccurrences(file: File) =
        file.takeIf { it.exists() }?.let {
            CsvProcessor.parseCsv(it).forEach { p ->
                plannedPaymentOccurrenceDao.insert(
                    PlannedPaymentOccurrenceEntity(
                        id = p["id"]?.toLong() ?: 0,
                        plannedPaymentId = p["plannedPaymentId"]?.toLongOrNull() ?: 0,
                        scheduledDate = p["scheduledDate"]?.toLongOrNull() ?: 0,
                        generatedTransactionId = p["generatedTransactionId"]?.toLongOrNull(),
                        status = p["status"]?.let { OccurrenceStatus.fromString(it) }
                            ?: OccurrenceStatus.PENDING
                    ))
            }
        }

    private suspend fun restoreTags(file: File) = file.takeIf { it.exists() }?.let {
        CsvProcessor.parseCsv(it).forEach { p ->
            tagDao.insertTag(
                TagEntity(
                    id = p["id"]?.toLong() ?: 0,
                    name = p["name"] ?: "",
                    colorHex = p["colorHex"]?.toIntOrNull() ?: 0
                )
            )
        }
    }

    private suspend fun restoreLoans(file: File) = file.takeIf { it.exists() }?.let {
        CsvProcessor.parseCsv(it).forEach { p ->
            val filterId = p["filterId"]?.toLongOrNull()
            if (filterId != null) {
                loanDao.insert(
                    LoanEntity(
                        id = p["id"]?.toLong() ?: 0,
                        name = p["name"] ?: "",
                        principalAmount = p["principalAmount"]?.toBigDecimalOrNull()
                            ?: BigDecimal.ZERO,
                        currency = p["currency"] ?: "USD",
                        interestRate = p["interestRate"]?.toFloatOrNull(),
                        startDate = p["startDate"]?.toLongOrNull() ?: 0,
                        lenderOrBorrowerName = p["lenderOrBorrowerName"] ?: "",
                        loanType = p["loanType"] ?: "debt",
                        linkedAccountId = p["linkedAccountId"] ?: "",
                        expectedSettlementDate = p["expectedSettlementDate"]?.toLongOrNull(),
                        status = p["status"] ?: "ACTIVE",
                        profileId = p["profileId"]?.toIntOrNull() ?: 1,
                        note = p["note"] ?: "",
                        filterId = filterId
                    )
                )
            }
        }
    }

    private suspend fun restoreLoanPayments(file: File) = file.takeIf { it.exists() }?.let {
        CsvProcessor.parseCsv(it).forEach { p ->
            loanPaymentDao.insert(
                LoanPaymentEntity(
                    id = p["id"]?.toLong() ?: 0,
                    loanId = p["loanId"]?.toLongOrNull() ?: 0,
                    amount = p["amount"]?.toBigDecimalOrNull() ?: BigDecimal.ZERO,
                    date = p["date"]?.toLongOrNull() ?: 0,
                    transactionId = p["transactionId"]?.toLongOrNull(),
                    note = p["note"] ?: ""
                )
            )
        }
    }

    private suspend fun restoreFilters(file: File) = file.takeIf { it.exists() }?.let {
        CsvProcessor.parseCsv(it).forEach { p ->
            filterDao.insertFilter(
                FilterEntity(
                    id = p["id"]?.toLong() ?: 0,
                    name = p["name"] ?: "",
                    timelineRange = p["timelineRange"]?.let {
                        try {
                            Json.decodeFromString<TimelineRange>(it)
                        } catch (_: Exception) {
                            TimelineRange.AllTime
                        }
                    } ?: TimelineRange.AllTime,
                    queryNote = p["queryNote"],
                    isNoteRegex = p["isNoteRegex"]?.toBoolean() ?: false,
                    queryPayee = p["queryPayee"],
                    isPayeeRegex = p["isPayeeRegex"]?.toBoolean() ?: false,
                    categoryIds = p["categoryIds"]?.let {
                        try {
                            Json.decodeFromString<Set<Long>>(it)
                        } catch (_: Exception) {
                            emptySet()
                        }
                    } ?: emptySet(),
                    accountIds = p["accountIds"]?.let {
                        try {
                            Json.decodeFromString<Set<String>>(it)
                        } catch (_: Exception) {
                            emptySet()
                        }
                    } ?: emptySet(),
                    startDate = p["startDate"]?.toLongOrNull(),
                    endDate = p["endDate"]?.toLongOrNull(),
                    paymentMethods = p["paymentMethods"]?.let {
                        try {
                            Json.decodeFromString<Set<PaymentMethod>>(it)
                        } catch (_: Exception) {
                            emptySet()
                        }
                    } ?: emptySet(),
                    hasWarranty = p["hasWarranty"]?.let { if (it == "true") true else if (it == "false") false else null },
                    warrantyExpirationStart = p["warrantyExpirationStart"]?.toLongOrNull(),
                    warrantyExpirationEnd = p["warrantyExpirationEnd"]?.toLongOrNull(),
                    minAmount = p["minAmount"]?.toBigDecimalOrNull(),
                    maxAmount = p["maxAmount"]?.toBigDecimalOrNull(),
                    transactionTypes = p["transactionTypes"]?.let {
                        try {
                            Json.decodeFromString<Set<TransactionType>>(it)
                        } catch (_: Exception) {
                            emptySet()
                        }
                    } ?: emptySet(),
                    currencies = p["currencies"]?.let {
                        try {
                            Json.decodeFromString<Set<String>>(it)
                        } catch (_: Exception) {
                            emptySet()
                        }
                    } ?: emptySet(),
                    statuses = p["statuses"]?.let {
                        try {
                            Json.decodeFromString<Set<TransactionStatus>>(it)
                        } catch (_: Exception) {
                            emptySet()
                        }
                    } ?: emptySet(),
                    linkedTransactionIds = p["linkedTransactionIds"]?.let {
                        try {
                            Json.decodeFromString<Set<Long>>(it)
                        } catch (_: Exception) {
                            emptySet()
                        }
                    } ?: emptySet(),
                    tags = p["tags"]?.let {
                        try {
                            Json.decodeFromString<Set<String>>(it)
                        } catch (_: Exception) {
                            emptySet()
                        }
                    } ?: emptySet(),
                    excludeFuture = p["excludeFuture"]?.toBoolean() ?: true
                ))
        }
    }

    private suspend fun restoreCurrencyRates(file: File) = file.takeIf { it.exists() }?.let {
        currencyDao.replaceRates(CsvProcessor.parseCsv(it).map { p ->
            CurrencyRateEntity(
                currencyCode = p["currencyCode"] ?: "",
                baseCurrency = p["baseCurrency"] ?: "USD",
                rate = p["rate"]?.toDoubleOrNull() ?: 1.0,
                timestamp = p["timestamp"]?.toLongOrNull() ?: 0,
                nextUpdate = p["nextUpdate"]?.toLongOrNull() ?: 0
            )
        })
    }
}
