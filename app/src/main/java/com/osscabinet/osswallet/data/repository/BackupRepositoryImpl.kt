package com.osscabinet.ossWallet.data.repository

import android.content.ContentResolver
import android.content.Context
import android.net.Uri
import androidx.room.withTransaction
import com.osscabinet.ossWallet.data.local.*
import com.osscabinet.ossWallet.data.local.dao.*
import com.osscabinet.ossWallet.data.local.entity.*
import com.osscabinet.ossWallet.data.repository.util.CsvProcessor
import com.osscabinet.ossWallet.domain.model.timeline.TimelineRange
import com.osscabinet.ossWallet.domain.repository.BackupRepository
import com.osscabinet.ossWallet.domain.repository.BackupType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.*
import java.math.BigDecimal
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream

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
    private val db: AppDatabase,
    private val context: Context,
    private val settingsDataStore: SettingsDataStore
) : BackupRepository {

    private val json = Json { ignoreUnknownKeys = true }

    override suspend fun exportData(uriString: String, type: BackupType): Result<Unit> = runCatching {
        withContext(Dispatchers.IO) {
            val uri = Uri.parse(uriString)
            when (type) {
                BackupType.SQLITE -> exportSqlite(uri)
                BackupType.CSV_ZIP -> exportCsvZip(uri)
            }
        }
    }

    override suspend fun importData(uriString: String, type: BackupType): Result<Unit> = runCatching {
        withContext(Dispatchers.IO) {
            val uri = Uri.parse(uriString)
            val inputStream = contentResolver.openInputStream(uri) ?: throw Exception("Failed to open input stream")
            when (type) {
                BackupType.SQLITE -> importSqlite(inputStream)
                BackupType.CSV_ZIP -> importCsvZip(inputStream)
            }
        }
    }

    private fun exportSqlite(uri: Uri) {
        // Ensure all WAL data is committed to the main DB file
        db.openHelper.writableDatabase.query("PRAGMA wal_checkpoint(FULL)").close()
        
        val dbFile = context.getDatabasePath("osswallet_manager_db")
        if (!dbFile.exists()) throw Exception("Database file not found")
        
        contentResolver.openOutputStream(uri)?.use { outputStream ->
            dbFile.inputStream().use { it.copyTo(outputStream) }
        }
    }

    private fun importSqlite(inputStream: InputStream) {
        val dbFile = context.getDatabasePath("osswallet_manager_db")
        val walFile = File(dbFile.path + "-wal")
        val shmFile = File(dbFile.path + "-shm")
        
        // Close DB connection before replacement
        db.close()
        
        FileOutputStream(dbFile).use { fos -> inputStream.copyTo(fos) }
        
        // Delete stale WAL/SHM files to avoid corruption
        if (walFile.exists()) walFile.delete()
        if (shmFile.exists()) shmFile.delete()
    }

    private suspend fun exportCsvZip(uri: Uri) {
        val tempZipFile = File(context.cacheDir, "osswallet_backup_${System.currentTimeMillis()}.zip")
        ZipOutputStream(FileOutputStream(tempZipFile)).use { zos ->
            // 1. Export CSVs
            addCsvEntry(zos, "profiles.csv", exportProfiles())
            addCsvEntry(zos, "accounts.csv", exportAccounts())
            addCsvEntry(zos, "transactions.csv", exportTransactions())
            addCsvEntry(zos, "categories.csv", exportCategories())
            addCsvEntry(zos, "tags.csv", exportTags())
            addCsvEntry(zos, "templates.csv", exportTemplates())
            addCsvEntry(zos, "transaction_links.csv", exportTransactionLinks())
            addCsvEntry(zos, "budgets.csv", exportBudgets())
            addCsvEntry(zos, "loans.csv", exportLoans())
            addCsvEntry(zos, "loan_payments.csv", exportLoanPayments())
            addCsvEntry(zos, "planned_payments.csv", exportPlannedPayments())
            addCsvEntry(zos, "planned_payment_occurrences.csv", exportPlannedPaymentOccurrences())
            addCsvEntry(zos, "filters.csv", exportFilters())
            
            // 2. Export Card Configs
            val cardConfigs = mapOf(
                "dashboard" to settingsDataStore.dashboardCardsConfig.first(),
                "accountDetail" to settingsDataStore.accountDetailCardsConfig.first(),
                "accountsView" to settingsDataStore.accountsViewCardsConfig.first(),
                "analytics" to settingsDataStore.analyticsCardsConfig.first()
            )
            
            val entry = ZipEntry("card_configs.json")
            zos.putNextEntry(entry)
            zos.write(json.encodeToString(cardConfigs).toByteArray())
            zos.closeEntry()
        }

        contentResolver.openOutputStream(uri)?.use { outputStream ->
            tempZipFile.inputStream().use { it.copyTo(outputStream) }
        }
        tempZipFile.delete()
    }

    private suspend fun importCsvZip(inputStream: InputStream) {
        val tempDir = File(context.cacheDir, "import_temp_${System.currentTimeMillis()}")
        tempDir.mkdirs()

        ZipInputStream(inputStream).use { zis ->
            var entry: ZipEntry? = zis.nextEntry
            while (entry != null) {
                val file = File(tempDir, entry.name)
                FileOutputStream(file).use { fos -> zis.copyTo(fos) }
                zis.closeEntry()
                entry = zis.nextEntry
            }
        }

        restoreFromCsvs(tempDir)
        tempDir.deleteRecursively()
    }

    private fun addCsvEntry(zos: ZipOutputStream, entryName: String, csvContent: String) {
        val entry = ZipEntry(entryName)
        zos.putNextEntry(entry)
        zos.write(csvContent.toByteArray())
        zos.closeEntry()
    }

    // --- Export Mappers ---

    private suspend fun exportProfiles() = CsvProcessor.buildCsv(
        listOf("id", "name", "baseCurrency", "type"),
        profileDao.getAllProfilesSync().map { listOf(it.id, it.name, it.baseCurrency, it.type) }
    )

    private suspend fun exportAccounts() = CsvProcessor.buildCsv(
        listOf("id", "name", "currencyCode", "structureType", "colorHex", "icon", "allowedCurrencies", "profileId", "isArchived", "displayOrder"),
        accountDao.getAllAccountsSync().map { listOf(it.id, it.name, it.currencyCode, it.structureType, it.colorHex, it.icon, it.allowedCurrencies, it.profileId, it.isArchived, it.displayOrder) }
    )

    private suspend fun exportTransactions() = CsvProcessor.buildCsv(
        listOf("id", "title", "amount", "timestamp", "currency", "transactionType", "accountId", "categoryId", "tags", "paymentMethod", "payee", "note", "recurring", "profileId", "latitude", "longitude", "warrantyExpiration", "status"),
        transactionDao.getAllTransactionsSync().map { listOf(it.id, it.title, it.amount, it.timestamp, it.currency, it.transactionType, it.accountId, it.categoryId, it.tags, it.paymentMethod, it.payee, it.note, it.recurring, it.profileId, it.latitude, it.longitude, it.warrantyExpiration, it.status) }
    )

    private suspend fun exportCategories() = CsvProcessor.buildCsv(
        listOf("id", "name", "iconName", "colorHex", "isDefault", "parentId"),
        categoryDao.getAllCategoriesSync().map { listOf(it.id, it.name, it.iconName, it.colorHex, it.isDefault, it.parentId) }
    )

    private suspend fun exportTags() = CsvProcessor.buildCsv(
        listOf("id", "name", "colorHex"),
        tagDao.getAllTagsSync().map { listOf(it.id, it.name, it.colorHex) }
    )

    private suspend fun exportTemplates() = CsvProcessor.buildCsv(
        listOf("id", "name", "categoryId", "amount", "transactionType", "icon", "colorHex", "accountId", "note", "payee", "location", "paymentMethod", "currency", "tags", "isStarred", "displayOrder"),
        templateDao.getAllTemplatesSync().map { listOf(it.id, it.name, it.categoryId, it.amount, it.transactionType, it.icon, it.colorHex, it.accountId, it.note, it.payee, it.location, it.paymentMethod, it.currency, it.tags, it.isStarred, it.displayOrder) }
    )

    private suspend fun exportTransactionLinks() = CsvProcessor.buildCsv(
        listOf("id", "transactionId", "linkedTransactionId"),
        transactionLinkDao.getAllLinksSync().map { listOf(it.id, it.transactionId, it.linkedTransactionId) }
    )

    private suspend fun exportBudgets() = CsvProcessor.buildCsv(
        listOf("id", "name", "currency", "limitAmount", "period", "categoryIds", "accountIds", "tags", "alertThreshold", "rollover", "trackType", "isArchived", "profileId", "customStartDate", "customEndDate"),
        budgetDao.getAllBudgetsSync().map { listOf(it.id, it.name, it.currency, it.limitAmount, it.period, it.categoryIds, it.accountIds, it.tags, it.alertThreshold, it.rollover, it.trackType, it.isArchived, it.profileId, it.customStartDate, it.customEndDate) }
    )

    private suspend fun exportLoans() = CsvProcessor.buildCsv(
        listOf("id", "name", "principalAmount", "currency", "interestRate", "startDate", "lenderOrBorrowerName", "loanType", "linkedAccountId", "expectedSettlementDate", "status", "profileId", "note", "categoryIds", "tags"),
        loanDao.getAllLoansSync().map { listOf(it.id, it.name, it.principalAmount, it.currency, it.interestRate, it.startDate, it.lenderOrBorrowerName, it.loanType, it.linkedAccountId, it.expectedSettlementDate, it.status, it.profileId, it.note, it.categoryIds, it.tags) }
    )

    private suspend fun exportLoanPayments() = CsvProcessor.buildCsv(
        listOf("id", "loanId", "transactionId", "amount", "date", "note"),
        loanPaymentDao.getAllPaymentsSync().map { listOf(it.id, it.loanId, it.transactionId, it.amount, it.date, it.note) }
    )

    private suspend fun exportPlannedPayments() = CsvProcessor.buildCsv(
        listOf("id", "title", "amount", "currency", "transactionType", "accountId", "targetAccountId", "categoryId", "paymentMethod", "tags", "note", "payee", "recurrenceType", "recurrenceInterval", "recurrenceDayOfWeek", "recurrenceDayOfMonth", "startDate", "endDate", "nextDueDate", "lastTriggeredDate", "autoConfirm", "status", "profileId"),
        plannedPaymentDao.getAllPlannedPaymentsSync().map { listOf(it.id, it.title, it.amount, it.currency, it.transactionType, it.accountId, it.targetAccountId, it.categoryId, it.paymentMethod, it.tags, it.note, it.payee, it.recurrenceType, it.recurrenceInterval, it.recurrenceDayOfWeek, it.recurrenceDayOfMonth, it.startDate, it.endDate, it.nextDueDate, it.lastTriggeredDate, it.autoConfirm, it.status, it.profileId) }
    )

    private suspend fun exportPlannedPaymentOccurrences() = CsvProcessor.buildCsv(
        listOf("id", "plannedPaymentId", "scheduledDate", "generatedTransactionId", "status"),
        plannedPaymentOccurrenceDao.getAllOccurrencesSync().map { listOf(it.id, it.plannedPaymentId, it.scheduledDate, it.generatedTransactionId, it.status) }
    )

    private suspend fun exportFilters() = CsvProcessor.buildCsv(
        listOf("id", "name", "timelineRange", "queryNote", "isNoteRegex", "queryPayee", "isPayeeRegex", "categoryIds", "accountIds", "startDate", "endDate", "paymentMethods", "hasWarranty", "warrantyExpirationStart", "warrantyExpirationEnd", "minAmount", "maxAmount", "transactionType", "currencies", "statuses"),
        filterDao.getAllFiltersSync().map { listOf(it.id, it.name, json.encodeToString(it.timelineRange), it.queryNote, it.isNoteRegex, it.queryPayee, it.isPayeeRegex, it.categoryIds, it.accountIds, it.startDate, it.endDate, it.paymentMethods, it.hasWarranty, it.warrantyExpirationStart, it.warrantyExpirationEnd, it.minAmount, it.maxAmount, it.transactionType, it.currencies, it.statuses) }
    )

    // --- Import Mappers ---

    private suspend fun restoreFromCsvs(dir: File) {
        db.withTransaction {
            clearAllTables()
            
            restoreTags(File(dir, "tags.csv"))
            restoreCategories(File(dir, "categories.csv"))
            restoreProfiles(File(dir, "profiles.csv"))
            restoreAccounts(File(dir, "accounts.csv"))
            restoreTransactions(File(dir, "transactions.csv"))
            restoreLinks(File(dir, "transaction_links.csv"))
            restoreTemplates(File(dir, "templates.csv"))
            restoreBudgets(File(dir, "budgets.csv"))
            restoreLoans(File(dir, "loans.csv"))
            restoreLoanPayments(File(dir, "loan_payments.csv"))
            restorePlannedPayments(File(dir, "planned_payments.csv"))
            restorePlannedPaymentOccurrences(File(dir, "planned_payment_occurrences.csv"))
            restoreFilters(File(dir, "filters.csv"))
            restoreCardConfigs(File(dir, "card_configs.json"))
        }
    }

    private suspend fun clearAllTables() {
        // Reverse order of FK dependencies
        plannedPaymentOccurrenceDao.clearAll()
        plannedPaymentDao.clearAll()
        loanPaymentDao.clearAll()
        loanDao.clearAll()
        budgetDao.clearAll()
        transactionLinkDao.clearAll()
        transactionDao.clearAll()
        templateDao.clearAll()
        accountDao.clearAll()
        categoryDao.clearAll()
        tagDao.clearAll()
        profileDao.clearAll()
        filterDao.clearAll()
    }

    private suspend fun restoreTags(file: File) = file.takeIf { it.exists() }?.let {
        CsvProcessor.parseCsv(it).forEach { p ->
            tagDao.insertTag(TagEntity(id = p["id"]?.toLong() ?: 0, name = p["name"] ?: "", colorHex = p["colorHex"]?.toIntOrNull() ?: 0))
        }
    }

    private suspend fun restoreCategories(file: File) = file.takeIf { it.exists() }?.let {
        CsvProcessor.parseCsv(it).sortedBy { it["parentId"]?.toLongOrNull() }.forEach { p ->
            categoryDao.insertCategory(CategoryEntity(
                id = p["id"]?.toLong() ?: 0, name = p["name"] ?: "", iconName = p["iconName"] ?: "", 
                colorHex = p["colorHex"]?.toIntOrNull() ?: 0, isDefault = p["isDefault"]?.toBoolean() ?: false, 
                parentId = p["parentId"]?.toLongOrNull()
            ))
        }
    }

    private suspend fun restoreProfiles(file: File) = file.takeIf { it.exists() }?.let {
        CsvProcessor.parseCsv(it).forEach { p ->
            profileDao.insertProfile(ProfileEntity(id = p["id"]?.toIntOrNull() ?: 0, name = p["name"] ?: "", baseCurrency = p["baseCurrency"] ?: "USD", type = p["type"] ?: "PERSONAL"))
        }
    }

    private suspend fun restoreAccounts(file: File) = file.takeIf { it.exists() }?.let {
        CsvProcessor.parseCsv(it).forEach { p ->
            accountDao.upsertAccount(AccountEntity(
                id = p["id"] ?: "", name = p["name"] ?: "", currencyCode = p["currencyCode"] ?: "USD", 
                structureType = p["structureType"] ?: "REGULAR", colorHex = p["colorHex"] ?: "#000000", 
                icon = p["icon"] ?: "", allowedCurrencies = p["allowedCurrencies"] ?: "", 
                profileId = p["profileId"]?.toIntOrNull() ?: 1, isArchived = p["isArchived"]?.toBoolean() ?: false,
                displayOrder = p["displayOrder"]?.toIntOrNull() ?: 0
            ))
        }
    }

    private suspend fun restoreTransactions(file: File) = file.takeIf { it.exists() }?.let {
        CsvProcessor.parseCsv(it).forEach { p ->
            transactionDao.insertTransaction(TransactionEntity(
                id = p["id"]?.toLong() ?: 0, title = p["title"] ?: "", 
                amount = p["amount"]?.toBigDecimalOrNull() ?: BigDecimal.ZERO, timestamp = p["timestamp"]?.toLongOrNull() ?: 0, 
                currency = p["currency"] ?: "USD", transactionType = p["transactionType"] ?: "expense", accountId = p["accountId"] ?: "", 
                categoryId = p["categoryId"]?.toLongOrNull() ?: 1002, tags = p["tags"] ?: "", paymentMethod = p["paymentMethod"] ?: "Cash", 
                payee = p["payee"] ?: "", note = p["note"] ?: "", recurring = p["recurring"], profileId = p["profileId"]?.toIntOrNull() ?: 1, 
                latitude = p["latitude"]?.toDoubleOrNull(), longitude = p["longitude"]?.toDoubleOrNull(), 
                warrantyExpiration = p["warrantyExpiration"]?.toLongOrNull(), status = p["status"] ?: "cleared"
            ))
        }
    }

    private suspend fun restoreLinks(file: File) = file.takeIf { it.exists() }?.let {
        CsvProcessor.parseCsv(it).forEach { p ->
            transactionLinkDao.insertLink(TransactionLinkEntity(id = p["id"]?.toLong() ?: 0, transactionId = p["transactionId"]?.toLong() ?: 0, linkedTransactionId = p["linkedTransactionId"]?.toLong() ?: 0))
        }
    }

    private suspend fun restoreTemplates(file: File) = file.takeIf { it.exists() }?.let {
        CsvProcessor.parseCsv(it).forEach { p ->
            templateDao.insertTemplate(TemplateEntity(
                id = p["id"]?.toLong() ?: 0, name = p["name"] ?: "", categoryId = p["categoryId"]?.toLongOrNull() ?: 1002,
                amount = p["amount"]?.toBigDecimalOrNull() ?: BigDecimal.ZERO, transactionType = p["transactionType"]?.toIntOrNull() ?: 0,
                icon = p["icon"], colorHex = p["colorHex"], accountId = p["accountId"], note = p["note"], payee = p["payee"],
                location = p["location"], paymentMethod = p["paymentMethod"], currency = p["currency"], tags = p["tags"] ?: "",
                isStarred = p["isStarred"]?.toBoolean() ?: false, displayOrder = p["displayOrder"]?.toIntOrNull() ?: 0
            ))
        }
    }

    private suspend fun restoreBudgets(file: File) = file.takeIf { it.exists() }?.let {
        CsvProcessor.parseCsv(it).forEach { p ->
            budgetDao.insert(BudgetEntity(
                id = p["id"]?.toLong() ?: 0, name = p["name"] ?: "", currency = p["currency"] ?: "USD", 
                limitAmount = p["limitAmount"]?.toBigDecimalOrNull() ?: BigDecimal.ZERO, period = p["period"] ?: "MONTHLY", 
                categoryIds = p["categoryIds"] ?: "", accountIds = p["accountIds"] ?: "", tags = p["tags"] ?: "",
                alertThreshold = p["alertThreshold"]?.toFloatOrNull() ?: 0.8f, rollover = p["rollover"]?.toBoolean() ?: false, 
                trackType = p["trackType"] ?: "EXPENSE", isArchived = p["isArchived"]?.toBoolean() ?: false, 
                profileId = p["profileId"]?.toIntOrNull() ?: 1, customStartDate = p["customStartDate"]?.toLongOrNull(), 
                customEndDate = p["customEndDate"]?.toLongOrNull()
            ))
        }
    }

    private suspend fun restoreLoans(file: File) = file.takeIf { it.exists() }?.let {
        CsvProcessor.parseCsv(it).forEach { p ->
            loanDao.insert(LoanEntity(
                id = p["id"]?.toLong() ?: 0, name = p["name"] ?: "", principalAmount = p["principalAmount"]?.toBigDecimalOrNull() ?: BigDecimal.ZERO,
                currency = p["currency"] ?: "USD", interestRate = p["interestRate"]?.toFloatOrNull(), startDate = p["startDate"]?.toLongOrNull() ?: 0,
                lenderOrBorrowerName = p["lenderOrBorrowerName"] ?: "", loanType = p["loanType"] ?: "BORROWED", linkedAccountId = p["linkedAccountId"] ?: "",
                expectedSettlementDate = p["expectedSettlementDate"]?.toLongOrNull(), status = p["status"] ?: "ACTIVE", profileId = p["profileId"]?.toIntOrNull() ?: 1,
                note = p["note"] ?: "", categoryIds = p["categoryIds"] ?: "", tags = p["tags"] ?: ""
            ))
        }
    }

    private suspend fun restoreLoanPayments(file: File) = file.takeIf { it.exists() }?.let {
        CsvProcessor.parseCsv(it).forEach { p ->
            loanPaymentDao.insert(LoanPaymentEntity(id = p["id"]?.toLong() ?: 0, loanId = p["loanId"]?.toLongOrNull() ?: 0, transactionId = p["transactionId"]?.toLongOrNull(), amount = p["amount"]?.toBigDecimalOrNull() ?: BigDecimal.ZERO, date = p["date"]?.toLongOrNull() ?: 0, note = p["note"] ?: ""))
        }
    }

    private suspend fun restorePlannedPayments(file: File) = file.takeIf { it.exists() }?.let {
        CsvProcessor.parseCsv(it).forEach { p ->
            plannedPaymentDao.insert(PlannedPaymentEntity(
                id = p["id"]?.toLong() ?: 0, title = p["title"] ?: "", amount = p["amount"]?.toBigDecimalOrNull() ?: BigDecimal.ZERO,
                currency = p["currency"] ?: "USD", transactionType = p["transactionType"] ?: "expense", accountId = p["accountId"] ?: "",
                targetAccountId = p["targetAccountId"], categoryId = p["categoryId"]?.toLongOrNull() ?: 1002, paymentMethod = p["paymentMethod"] ?: "Cash",
                tags = p["tags"] ?: "", note = p["note"] ?: "", payee = p["payee"] ?: "", recurrenceType = p["recurrenceType"] ?: "MONTHLY",
                recurrenceInterval = p["recurrenceInterval"]?.toIntOrNull() ?: 1, recurrenceDayOfWeek = p["recurrenceDayOfWeek"]?.toIntOrNull(),
                recurrenceDayOfMonth = p["recurrenceDayOfMonth"]?.toIntOrNull(), startDate = p["startDate"]?.toLongOrNull() ?: 0,
                endDate = p["endDate"]?.toLongOrNull(), nextDueDate = p["nextDueDate"]?.toLongOrNull() ?: 0, lastTriggeredDate = p["lastTriggeredDate"]?.toLongOrNull(),
                autoConfirm = p["autoConfirm"]?.toBoolean() ?: false, status = p["status"] ?: "ACTIVE", profileId = p["profileId"]?.toIntOrNull() ?: 1
            ))
        }
    }

    private suspend fun restorePlannedPaymentOccurrences(file: File) = file.takeIf { it.exists() }?.let {
        CsvProcessor.parseCsv(it).forEach { p ->
            plannedPaymentOccurrenceDao.insert(PlannedPaymentOccurrenceEntity(id = p["id"]?.toLong() ?: 0, plannedPaymentId = p["plannedPaymentId"]?.toLongOrNull() ?: 0, scheduledDate = p["scheduledDate"]?.toLongOrNull() ?: 0, generatedTransactionId = p["generatedTransactionId"]?.toLongOrNull(), status = p["status"] ?: "PENDING"))
        }
    }

    private suspend fun restoreFilters(file: File) = file.takeIf { it.exists() }?.let {
        CsvProcessor.parseCsv(it).forEach { p ->
            filterDao.insertFilter(FilterEntity(
                id = p["id"]?.toLong() ?: 0, name = p["name"] ?: "",
                timelineRange = p["timelineRange"]?.let { tr -> try { json.decodeFromString<TimelineRange>(tr) } catch(e: Exception) { TimelineRange.AllTime } } ?: TimelineRange.AllTime,
                queryNote = p["queryNote"], isNoteRegex = p["isNoteRegex"]?.toBoolean() ?: false, queryPayee = p["queryPayee"], isPayeeRegex = p["isPayeeRegex"]?.toBoolean() ?: false,
                categoryIds = p["categoryIds"] ?: "", accountIds = p["accountIds"] ?: "", startDate = p["startDate"]?.toLongOrNull(), endDate = p["endDate"]?.toLongOrNull(),
                paymentMethods = p["paymentMethods"] ?: "", hasWarranty = p["hasWarranty"]?.toBooleanStrictOrNull(), warrantyExpirationStart = p["warrantyExpirationStart"]?.toLongOrNull(),
                warrantyExpirationEnd = p["warrantyExpirationEnd"]?.toLongOrNull(), minAmount = p["minAmount"]?.toBigDecimalOrNull(), maxAmount = p["maxAmount"]?.toBigDecimalOrNull(),
                transactionType = p["transactionType"], currencies = p["currencies"] ?: "", statuses = p["statuses"] ?: ""
            ))
        }
    }

    private suspend fun restoreCardConfigs(file: File) = file.takeIf { it.exists() }?.let {
        val configs = json.decodeFromString<Map<String, String>>(it.readText())
        configs["dashboard"]?.let { settingsDataStore.saveDashboardCardsConfig(it) }
        configs["accountDetail"]?.let { settingsDataStore.saveAccountDetailCardsConfig(it) }
        configs["accountsView"]?.let { settingsDataStore.saveAccountsViewCardsConfig(it) }
        configs["analytics"]?.let { settingsDataStore.saveAnalyticsCardsConfig(it) }
    }
}
