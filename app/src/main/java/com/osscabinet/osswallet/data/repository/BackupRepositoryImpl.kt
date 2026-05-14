package com.osscabinet.ossWallet.data.repository

import android.content.ContentResolver
import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.room.withTransaction
import com.osscabinet.ossWallet.data.local.*
import com.osscabinet.ossWallet.data.local.dao.*
import com.osscabinet.ossWallet.data.local.entity.*
import com.osscabinet.ossWallet.data.repository.util.CsvProcessor
import com.osscabinet.ossWallet.domain.model.*
import com.osscabinet.ossWallet.domain.repository.BackupRepository
import com.osscabinet.ossWallet.domain.repository.BackupType
import com.osscabinet.ossWallet.domain.model.timeline.TimelineRange
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.*
import java.math.BigDecimal
import java.util.zip.ZipInputStream

class BackupRepositoryImpl(
    private val contentResolver: ContentResolver,
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
    private val context: Context,
    private val settingsDataStore: SettingsDataStore
) : BackupRepository {

    override suspend fun exportData(uriString: String, type: BackupType): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val uri = android.net.Uri.parse(uriString)
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
        
        writeCsvToZip(zos, "profiles.csv", listOf("id", "name", "baseCurrency", "type"), 
            profileDao.getAllProfilesSync().map { listOf(it.id, it.name, it.baseCurrency, it.type) })
        
        writeCsvToZip(zos, "accounts.csv", listOf("id", "name", "currencyCode", "structureType", "colorHex", "icon", "allowedCurrencies", "profileId", "isArchived", "displayOrder"), 
            accountDao.getAllAccountsSync().map { listOf(it.id, it.name, it.currencyCode, it.structureType, it.colorHex, it.icon, it.allowedCurrencies, it.profileId, it.isArchived, it.displayOrder) })
        
        writeCsvToZip(zos, "categories.csv", listOf("id", "name", "iconName", "colorHex", "isDefault", "parentId"), 
            categoryDao.getAllCategoriesSync().map { listOf(it.id, it.name, it.iconName, it.colorHex, it.isDefault, it.parentId) })
        
        writeCsvToZip(zos, "transactions.csv", listOf("id", "name", "amount", "timestamp", "currency", "transactionType", "accountId", "categoryId", "tags", "paymentMethod", "payee", "note", "sourcePlannedPaymentId", "profileId", "latitude", "longitude", "warrantyExpiration", "status"), 
            transactionDao.getAllTransactionsSync().map { listOf(it.id, it.name, it.amount, it.timestamp, it.currency, it.transactionType, it.accountId, it.categoryId, it.tags, it.paymentMethod.name, it.payee, it.note, it.sourcePlannedPaymentId, it.profileId, it.latitude, it.longitude, it.warrantyExpiration, it.status.name) })
        
        writeCsvToZip(zos, "transaction_links.csv", listOf("id", "transactionId", "linkedTransactionId"), 
            transactionLinkDao.getAllLinksSync().map { listOf(it.id, it.transactionId, it.linkedTransactionId) })
        
        writeCsvToZip(zos, "budgets.csv", listOf("id", "name", "currency", "limitAmount", "period", "categoryIds", "accountIds", "tags", "alertThreshold", "rollover", "trackType", "isArchived", "profileId", "customStartDate", "customEndDate"), 
            budgetDao.getAllBudgetsSync().map { listOf(it.id, it.name, it.currency, it.limitAmount, it.period, it.categoryIds, it.accountIds, it.tags, it.alertThreshold, it.rollover, it.trackType, it.isArchived, it.profileId, it.customStartDate, it.customEndDate) })
        
        writeCsvToZip(zos, "templates.csv", listOf("id", "name", "categoryId", "amount", "transactionType", "icon", "colorHex", "accountId", "targetAccountId", "targetCurrency", "transferRate", "note", "payee", "location", "paymentMethod", "currency", "tags", "isStarred", "displayOrder", "profileId", "transactionStatus"), 
            templateDao.getAllTemplatesSync().map { listOf(it.id, it.name, it.categoryId, it.amount, it.transactionType, it.icon, it.colorHex, it.accountId, it.targetAccountId, it.targetCurrency, it.transferRate, it.note, it.payee, it.location, it.paymentMethod?.name, it.currency, it.tags, it.isStarred, it.displayOrder, it.profileId, it.transactionStatus.name) })
        
        writeCsvToZip(zos, "planned_payments.csv", listOf(
            "id", "name", "categoryId", "amount", "transactionType", "accountId", "targetAccountId", 
            "targetCurrency", "transferRate", "note", "payee", "location", "latitude", "longitude", 
            "warrantyExpiration", "transactionStatus", "paymentMethod", "currency", "tags", 
            "autoConfirm", "status", "profileId", "templateId", 
            "recurrenceType", "interval", "dayOfWeek", "dayOfMonth", "recurrenceCronExpression", 
            "startDate", "endDate", "nextDueDate", "lastTriggeredDate"
        ), 
            plannedPaymentDao.getAllPlannedPaymentsSync().map { 
                listOf(
                    it.id, it.name, it.categoryId, it.amount, it.transactionType, it.accountId, it.targetAccountId,
                    it.targetCurrency, it.transferRate, it.note, it.payee, it.location, it.latitude, it.longitude,
                    it.warrantyExpiration, it.transactionStatus.name, it.paymentMethod?.name, it.currency, it.tags,
                    it.autoConfirm, it.status.name, it.profileId, it.templateId,
                    it.schedule.recurrenceType, it.schedule.interval, it.schedule.dayOfWeek, it.schedule.dayOfMonth, it.schedule.recurrenceCronExpression,
                    it.schedule.startDate, it.schedule.endDate, it.schedule.nextDueDate, it.schedule.lastTriggeredDate
                )
            })
        
        writeCsvToZip(zos, "planned_payment_occurrences.csv", listOf("id", "plannedPaymentId", "scheduledDate", "generatedTransactionId", "status"), 
            plannedPaymentOccurrenceDao.getAllOccurrencesSync().map { listOf(it.id, it.plannedPaymentId, it.scheduledDate, it.generatedTransactionId, it.status.name) })

        writeCsvToZip(zos, "tags.csv", listOf("id", "name", "colorHex"), 
            tagDao.getAllTagsSync().map { listOf(it.id, it.name, it.colorHex) })

        writeCsvToZip(zos, "loans.csv", listOf("id", "name", "principalAmount", "currency", "interestRate", "startDate", "lenderOrBorrowerName", "loanType", "linkedAccountId", "expectedSettlementDate", "status", "profileId", "note"), 
            loanDao.getAllLoansSync().map { listOf(it.id, it.name, it.principalAmount, it.currency, it.interestRate, it.startDate, it.lenderOrBorrowerName, it.loanType, it.linkedAccountId, it.expectedSettlementDate, it.status, it.profileId, it.note) })

        writeCsvToZip(zos, "loan_payments.csv", listOf("id", "loanId", "amount", "date", "transactionId", "note"), 
            loanPaymentDao.getAllPaymentsSync().map { listOf(it.id, it.loanId, it.amount, it.date, it.transactionId, it.note) })

        writeCsvToZip(zos, "filters.csv", listOf("id", "name", "timelineRange", "queryNote", "isNoteRegex", "queryPayee", "isPayeeRegex", "categoryIds", "accountIds", "startDate", "endDate", "paymentMethods", "hasWarranty", "warrantyExpirationStart", "warrantyExpirationEnd", "minAmount", "maxAmount", "transactionType", "currencies", "statuses"), 
            filterDao.getAllFiltersSync().map { 
                listOf(it.id, it.name, Json.encodeToString(it.timelineRange), it.queryNote, it.isNoteRegex, it.queryPayee, it.isPayeeRegex, it.categoryIds, it.accountIds, it.startDate, it.endDate, it.paymentMethods, it.hasWarranty, it.warrantyExpirationStart, it.warrantyExpirationEnd, it.minAmount, it.maxAmount, it.transactionType, it.currencies, it.statuses) 
            })

        writeCsvToZip(zos, "currency_rates.csv", listOf("currencyCode", "baseCurrency", "rate", "timestamp", "nextUpdate"), 
            currencyDao.getAllRates().map { listOf(it.currencyCode, it.baseCurrency, it.rate, it.timestamp, it.nextUpdate) })

        zos.close()
    }

    private fun writeCsvToZip(zos: java.util.zip.ZipOutputStream, fileName: String, headers: List<String>, rows: List<List<Any?>>) {
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

    override suspend fun importData(uriString: String, type: BackupType): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val uri = android.net.Uri.parse(uriString)
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
            profileDao.insertProfile(ProfileEntity(
                id = p["id"]?.toInt() ?: 0, 
                name = p["name"] ?: "", 
                baseCurrency = p["baseCurrency"] ?: "USD",
                type = p["type"] ?: "PERSONAL"
            ))
        }
    }

    private suspend fun restoreAccounts(file: File) = file.takeIf { it.exists() }?.let {
        CsvProcessor.parseCsv(it).forEach { p ->
            accountDao.upsertAccount(AccountEntity(
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
            ))
        }
    }

    private suspend fun restoreCategories(file: File) = file.takeIf { it.exists() }?.let {
        CsvProcessor.parseCsv(it).forEach { p ->
            categoryDao.insertCategory(CategoryEntity(
                id = p["id"]?.toLong() ?: 0, 
                name = p["name"] ?: "", 
                iconName = p["iconName"] ?: p["icon"] ?: "", 
                colorHex = p["colorHex"]?.toIntOrNull() ?: 0, 
                isDefault = p["isDefault"]?.toBoolean() ?: false,
                parentId = p["parentId"]?.toLongOrNull()
            ))
        }
    }

    private suspend fun restoreTransactions(file: File) = file.takeIf { it.exists() }?.let {
        CsvProcessor.parseCsv(it).forEach { p ->
            transactionDao.insertTransaction(TransactionEntity(
                id = p["id"]?.toLong() ?: 0,
                name = p["name"] ?: "",
                amount = p["amount"]?.toBigDecimalOrNull() ?: BigDecimal.ZERO, 
                timestamp = p["timestamp"]?.toLongOrNull() ?: 0, 
                currency = p["currency"] ?: "USD", 
                transactionType = p["transactionType"] ?: "expense", 
                accountId = p["accountId"],
                categoryId = p["categoryId"]?.toLongOrNull() ?: 1002, 
                tags = p["tags"] ?: "", 
                paymentMethod = p["paymentMethod"]?.let { PaymentMethod.fromString(it) } ?: PaymentMethod.CASH, 
                payee = p["payee"] ?: "", 
                note = p["note"] ?: "", 
                sourcePlannedPaymentId = p["sourcePlannedPaymentId"]?.toLongOrNull(), 
                profileId = p["profileId"]?.toIntOrNull() ?: 1,
                latitude = p["latitude"]?.toDoubleOrNull(), 
                longitude = p["longitude"]?.toDoubleOrNull(), 
                warrantyExpiration = p["warrantyExpiration"]?.toLongOrNull(), 
                status = p["status"]?.let { TransactionStatus.fromString(it) } ?: TransactionStatus.CLEARED
            ))
        }
    }

    private suspend fun restoreLinks(file: File) = file.takeIf { it.exists() }?.let {
        CsvProcessor.parseCsv(it).forEach { p ->
            transactionLinkDao.insertLink(TransactionLinkEntity(
                id = p["id"]?.toLong() ?: 0, 
                transactionId = p["transactionId"]?.toLong() ?: 0, 
                linkedTransactionId = p["linkedTransactionId"]?.toLong() ?: 0
            ))
        }
    }

    private suspend fun restoreTemplates(file: File) = file.takeIf { it.exists() }?.let {
        CsvProcessor.parseCsv(it).forEach { p ->
            templateDao.insertTemplate(TemplateEntity(
                id = p["id"]?.toLong() ?: 0, 
                name = p["name"] ?: "", 
                categoryId = p["categoryId"]?.toLongOrNull() ?: 1002,
                amount = p["amount"]?.toBigDecimalOrNull() ?: BigDecimal.ZERO, 
                transactionType = p["transactionType"]?.toIntOrNull() ?: 0,
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
                tags = p["tags"] ?: "",
                isStarred = p["isStarred"]?.toBoolean() ?: false, 
                displayOrder = p["displayOrder"]?.toIntOrNull() ?: 0,
                profileId = p["profileId"]?.toIntOrNull() ?: 1,
                transactionStatus = p["transactionStatus"]?.let { TransactionStatus.fromString(it) } ?: TransactionStatus.CLEARED
            ))
        }
    }

    private suspend fun restoreBudgets(file: File) = file.takeIf { it.exists() }?.let {
        CsvProcessor.parseCsv(it).forEach { p ->
            budgetDao.insert(BudgetEntity(
                id = p["id"]?.toLong() ?: 0, 
                name = p["name"] ?: "", 
                currency = p["currency"] ?: "USD", 
                limitAmount = p["limitAmount"]?.toBigDecimalOrNull() ?: BigDecimal.ZERO, 
                period = p["period"] ?: "MONTHLY", 
                categoryIds = p["categoryIds"] ?: "", 
                accountIds = p["accountIds"] ?: "", 
                tags = p["tags"] ?: "",
                alertThreshold = p["alertThreshold"]?.toFloatOrNull() ?: 0.8f, 
                rollover = p["rollover"]?.toBoolean() ?: false, 
                trackType = p["trackType"] ?: "EXPENSE", 
                isArchived = p["isArchived"]?.toBoolean() ?: false, 
                profileId = p["profileId"]?.toIntOrNull() ?: 1, 
                customStartDate = p["customStartDate"]?.toLongOrNull(),
                customEndDate = p["customEndDate"]?.toLongOrNull()
            ))
        }
    }

    private suspend fun restorePlannedPayments(file: File) = file.takeIf { it.exists() }?.let {
        CsvProcessor.parseCsv(it).forEach { p ->
            plannedPaymentDao.insert(PlannedPaymentEntity(
                id = p["id"]?.toLong() ?: 0,
                name = p["name"] ?: "",
                categoryId = p["categoryId"]?.toLongOrNull() ?: 1002L,
                amount = p["amount"]?.toBigDecimalOrNull() ?: BigDecimal.ZERO,
                transactionType = p["transactionType"]?.toIntOrNull() ?: 0,
                accountId = p["accountId"],
                targetAccountId = p["targetAccountId"],
                targetCurrency = p["targetCurrency"],
                transferRate = p["transferRate"]?.toBigDecimalOrNull() ?: BigDecimal.ONE,
                note = p["note"],
                payee = p["payee"],
                location = p["location"],
                latitude = p["latitude"]?.toDoubleOrNull(),
                longitude = p["longitude"]?.toDoubleOrNull(),
                warrantyExpiration = p["warrantyExpiration"]?.toLongOrNull(),
                transactionStatus = p["transactionStatus"]?.let { TransactionStatus.fromString(it) } ?: TransactionStatus.CLEARED,
                paymentMethod = p["paymentMethod"]?.let { PaymentMethod.fromString(it) },
                currency = p["currency"] ?: "USD",
                tags = p["tags"] ?: "",
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
                status = p["status"]?.let { PlannedPaymentStatus.fromString(it) } ?: PlannedPaymentStatus.ACTIVE,
                profileId = p["profileId"]?.toIntOrNull() ?: 1,
                templateId = p["templateId"]?.toLongOrNull()
            ))
        }
    }

    private suspend fun restorePlannedPaymentOccurrences(file: File) = file.takeIf { it.exists() }?.let {
        CsvProcessor.parseCsv(it).forEach { p ->
            plannedPaymentOccurrenceDao.insert(PlannedPaymentOccurrenceEntity(
                id = p["id"]?.toLong() ?: 0, 
                plannedPaymentId = p["plannedPaymentId"]?.toLongOrNull() ?: 0, 
                scheduledDate = p["scheduledDate"]?.toLongOrNull() ?: 0, 
                generatedTransactionId = p["generatedTransactionId"]?.toLongOrNull(), 
                status = p["status"]?.let { OccurrenceStatus.fromString(it) } ?: OccurrenceStatus.PENDING
            ))
        }
    }

    private suspend fun restoreTags(file: File) = file.takeIf { it.exists() }?.let {
        CsvProcessor.parseCsv(it).forEach { p ->
            tagDao.insertTag(TagEntity(
                id = p["id"]?.toLong() ?: 0, 
                name = p["name"] ?: "", 
                colorHex = p["colorHex"]?.toIntOrNull() ?: 0
            ))
        }
    }

    private suspend fun restoreLoans(file: File) = file.takeIf { it.exists() }?.let {
        CsvProcessor.parseCsv(it).forEach { p ->
            loanDao.insert(LoanEntity(
                id = p["id"]?.toLong() ?: 0,
                name = p["name"] ?: "",
                principalAmount = p["principalAmount"]?.toBigDecimalOrNull() ?: BigDecimal.ZERO,
                currency = p["currency"] ?: "USD",
                interestRate = p["interestRate"]?.toFloatOrNull(),
                startDate = p["startDate"]?.toLongOrNull() ?: 0,
                lenderOrBorrowerName = p["lenderOrBorrowerName"] ?: "",
                loanType = p["loanType"] ?: "debt",
                linkedAccountId = p["linkedAccountId"] ?: "",
                expectedSettlementDate = p["expectedSettlementDate"]?.toLongOrNull(),
                status = p["status"] ?: "ACTIVE",
                profileId = p["profileId"]?.toIntOrNull() ?: 1,
                note = p["note"] ?: ""
            ))
        }
    }

    private suspend fun restoreLoanPayments(file: File) = file.takeIf { it.exists() }?.let {
        CsvProcessor.parseCsv(it).forEach { p ->
            loanPaymentDao.insert(LoanPaymentEntity(
                id = p["id"]?.toLong() ?: 0,
                loanId = p["loanId"]?.toLongOrNull() ?: 0,
                amount = p["amount"]?.toBigDecimalOrNull() ?: BigDecimal.ZERO,
                date = p["date"]?.toLongOrNull() ?: 0,
                transactionId = p["transactionId"]?.toLongOrNull(),
                note = p["note"] ?: ""
            ))
        }
    }

    private suspend fun restoreFilters(file: File) = file.takeIf { it.exists() }?.let {
        CsvProcessor.parseCsv(it).forEach { p ->
            filterDao.insertFilter(FilterEntity(
                id = p["id"]?.toLong() ?: 0,
                name = p["name"] ?: "",
                timelineRange = p["timelineRange"]?.let { 
                    try { Json.decodeFromString<TimelineRange>(it) } catch (_: Exception) { TimelineRange.AllTime }
                } ?: TimelineRange.AllTime,
                queryNote = p["queryNote"],
                isNoteRegex = p["isNoteRegex"]?.toBoolean() ?: false,
                queryPayee = p["queryPayee"],
                isPayeeRegex = p["isPayeeRegex"]?.toBoolean() ?: false,
                categoryIds = p["categoryIds"] ?: "",
                accountIds = p["accountIds"] ?: "",
                startDate = p["startDate"]?.toLongOrNull(),
                endDate = p["endDate"]?.toLongOrNull(),
                paymentMethods = p["paymentMethods"] ?: "",
                hasWarranty = p["hasWarranty"]?.let { if (it == "true") true else if (it == "false") false else null },
                warrantyExpirationStart = p["warrantyExpirationStart"]?.toLongOrNull(),
                warrantyExpirationEnd = p["warrantyExpirationEnd"]?.toLongOrNull(),
                minAmount = p["minAmount"]?.toBigDecimalOrNull(),
                maxAmount = p["maxAmount"]?.toBigDecimalOrNull(),
                transactionType = p["transactionType"],
                currencies = p["currencies"] ?: "",
                statuses = p["statuses"] ?: ""
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
