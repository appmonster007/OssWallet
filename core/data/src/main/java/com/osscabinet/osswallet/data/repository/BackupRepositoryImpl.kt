package com.osscabinet.ossWallet.data.repository

import android.content.Context
import androidx.core.net.toUri
import androidx.room.withTransaction
import com.osscabinet.ossWallet.core.util.DispatcherProvider
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
import org.apache.commons.csv.CSVFormat
import org.apache.commons.csv.CSVParser
import org.apache.commons.csv.CSVPrinter
import com.osscabinet.ossWallet.domain.model.OccurrenceStatus
import com.osscabinet.ossWallet.domain.model.PaymentMethod
import com.osscabinet.ossWallet.domain.model.PlannedPaymentStatus
import com.osscabinet.ossWallet.domain.model.TransactionStatus
import com.osscabinet.ossWallet.domain.model.TransactionType
import com.osscabinet.ossWallet.domain.model.isTransfer

import com.osscabinet.ossWallet.domain.model.EntryMode

import com.osscabinet.ossWallet.domain.model.timeline.TimelineRange
import com.osscabinet.ossWallet.domain.repository.BackupMeta
import com.osscabinet.ossWallet.domain.repository.BackupRepository
import com.osscabinet.ossWallet.domain.repository.BackupScope
import com.osscabinet.ossWallet.domain.repository.BackupType
import com.osscabinet.ossWallet.domain.repository.RestorableEntity
import com.osscabinet.ossWallet.domain.util.splitByComma
import com.osscabinet.ossWallet.domain.model.*
import com.osscabinet.ossWallet.domain.model.CategoryNature
import android.util.Log
import dagger.hilt.android.qualifiers.ApplicationContext
import jakarta.inject.Inject
import jakarta.inject.Singleton
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.coroutines.flow.first
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream
import java.io.OutputStreamWriter
import java.math.BigDecimal
import java.util.zip.ZipInputStream

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
    private val dispatcherProvider: DispatcherProvider,
    private val json: Json,
    @param:ApplicationContext private val context: Context
) : BackupRepository {

    override suspend fun exportData(uriString: String, type: BackupType, scope: BackupScope): Result<Unit> =
        withContext(dispatcherProvider.io) {
            try {
                val uri = uriString.toUri()
                val currentProfileId = settingsDataStore.currentProfileId.first()
                context.contentResolver.openOutputStream(uri)?.use { outputStream ->
                    when (type) {
                        BackupType.CSV_ZIP -> createZipBackup(outputStream, scope, currentProfileId)
                        BackupType.SQLITE -> exportSqlite(outputStream)
                        BackupType.FLAT_CSV -> exportFlatCsv(outputStream, scope)
                    }
                }
                Result.success(Unit)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }

    private suspend fun exportFlatCsv(outputStream: OutputStream, scope: BackupScope) {
        val currentProfileId = settingsDataStore.currentProfileId.first()
        val transactions = if (scope == BackupScope.SINGLE_PROFILE)
            transactionDao.getAllTransactionsSync().filter { it.profileId == currentProfileId }
        else
            transactionDao.getAllTransactionsSync()

        val accountsById = accountDao.getAllAccountsSync().associateBy { it.id }
        val categoriesById = categoryDao.getAllCategoriesSync().associateBy { it.id }

        val headers = listOf("date", "account", "category", "type", "amount", "currency", "payee", "note", "payment_type", "warranty_months", "labels", "is_transfer")
        val format = CSVFormat.DEFAULT.builder()
            .setHeader(*headers.toTypedArray())
            .get()

        val writer = OutputStreamWriter(outputStream, Charsets.UTF_8)
        CSVPrinter(writer, format).use { printer ->
            transactions.forEach { tx ->
                val warrantyMonths = tx.warrantyExpiration?.let {
                    val months = ((it - tx.timestamp) / (1000L * 60 * 60 * 24 * 30)).toInt()
                    months.coerceAtLeast(0)
                } ?: 0
                val isTransfer = tx.transactionType.isTransfer
                val tags = tx.tags.joinToString("|")
                printer.printRecord(
                    tx.timestamp.toString(),
                    accountsById[tx.accountId]?.name ?: tx.accountId ?: "",
                    categoriesById[tx.categoryId]?.name ?: tx.categoryId.toString(),
                    tx.transactionType.name,
                    tx.amount.toPlainString(),
                    tx.currency,
                    tx.payee,
                    tx.note ?: "",
                    tx.paymentMethod.name,
                    warrantyMonths.toString(),
                    tags,
                    isTransfer.toString()
                )
            }
            printer.flush()
        }
    }

    private suspend fun createZipBackup(
        outputStream: OutputStream,
        scope: BackupScope = BackupScope.ALL_PROFILES,
        currentProfileId: Int = 1
    ) {
        val zos = java.util.zip.ZipOutputStream(outputStream)

        val allProfiles = profileDao.getAllProfilesSync()
        val profiles = if (scope == BackupScope.SINGLE_PROFILE)
            allProfiles.filter { it.id == currentProfileId } else allProfiles

        writeCsvToZip(
            zos, "profiles.csv", listOf("id", "name", "baseCurrency", "type"),
            profiles.map { listOf(it.id, it.name, it.baseCurrency, it.type) })

        val allAccounts = accountDao.getAllAccountsSync()
        val accounts = if (scope == BackupScope.SINGLE_PROFILE)
            allAccounts.filter { it.profileId == currentProfileId } else allAccounts

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
            accounts.map {
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
            listOf("id", "name", "iconName", "colorHex", "isDefault", "parentId", "nature"),
            categoryDao.getAllCategoriesSync().map {
                listOf(
                    it.id,
                    it.name,
                    it.iconName,
                    it.colorHex,
                    it.isDefault,
                    it.parentId,
                    it.nature.name
                )
            })

        val allTransactions = transactionDao.getAllTransactionsSync()
        val transactions = if (scope == BackupScope.SINGLE_PROFILE)
            allTransactions.filter { it.profileId == currentProfileId } else allTransactions
        val transactionIds = transactions.map { it.id }.toSet()

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
            transactions.map {
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
                    it.tags.joinToString("|"),
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

        val allLinks = transactionLinkDao.getAllLinksSync()
        val links = if (scope == BackupScope.SINGLE_PROFILE)
            allLinks.filter { it.transactionId in transactionIds && it.linkedTransactionId in transactionIds }
        else allLinks

        writeCsvToZip(
            zos, "transaction_links.csv", listOf("id", "transactionId", "linkedTransactionId"),
            links.map { listOf(it.id, it.transactionId, it.linkedTransactionId) })

        val allBudgets = budgetDao.getAllBudgetsSync()
        val budgets = if (scope == BackupScope.SINGLE_PROFILE)
            allBudgets.filter { it.budget.profileId == currentProfileId } else allBudgets
        val budgetFilterIds = budgets.map { it.budget.filterId }.toSet()

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
            budgets.map {
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

        val allTemplates = templateDao.getAllTemplatesSync()
        val templates = if (scope == BackupScope.SINGLE_PROFILE)
            allTemplates.filter { it.profileId == currentProfileId } else allTemplates
        val templateIds = templates.map { it.id }.toSet()

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
            templates.map {
                listOf(
                    it.id,
                    it.name,
                    it.categoryId,
                    it.amount,
                    it.entryMode,
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
                    it.tags.joinToString("|"),
                    it.isStarred,
                    it.displayOrder,
                    it.profileId,
                    it.transactionStatus.name
                )
            })

        val allPlannedPayments = plannedPaymentDao.getAllPlannedPaymentsSync()
        val plannedPayments = if (scope == BackupScope.SINGLE_PROFILE)
            allPlannedPayments.filter { it.payment.profileId == currentProfileId &&
                it.payment.templateId in templateIds }
        else allPlannedPayments
        val plannedPaymentIds = plannedPayments.map { it.payment.id }.toSet()

        writeCsvToZip(
            zos, "planned_payments.csv", listOf(
                "id", "autoConfirm", "status", "profileId", "templateId",
                "recurrenceType", "interval", "dayOfWeek", "dayOfMonth", "recurrenceCronExpression",
                "startDate", "endDate", "nextDueDate", "lastTriggeredDate"
            ),
            plannedPayments.map {
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

        val allOccurrences = plannedPaymentOccurrenceDao.getAllOccurrencesSync()
        val occurrences = if (scope == BackupScope.SINGLE_PROFILE)
            allOccurrences.filter { it.plannedPaymentId in plannedPaymentIds } else allOccurrences

        writeCsvToZip(
            zos,
            "planned_payment_occurrences.csv",
            listOf("id", "plannedPaymentId", "scheduledDate", "generatedTransactionId", "status"),
            occurrences.map {
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

        val allLoans = loanDao.getAllLoansSync()
        val loans = if (scope == BackupScope.SINGLE_PROFILE)
            allLoans.filter { it.loan.profileId == currentProfileId } else allLoans
        val loanIds = loans.map { it.loan.id }.toSet()
        val loanFilterIds = loans.map { it.loan.filterId }.toSet()

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
            loans.map {
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

        val allLoanPayments = loanPaymentDao.getAllPaymentsSync()
        val loanPayments = if (scope == BackupScope.SINGLE_PROFILE)
            allLoanPayments.filter { it.loanId in loanIds } else allLoanPayments

        writeCsvToZip(
            zos,
            "loan_payments.csv",
            listOf("id", "loanId", "amount", "date", "transactionId", "note"),
            loanPayments.map { listOf(it.id, it.loanId, it.amount, it.date, it.transactionId, it.note) })

        val allFilters = filterDao.getAllFiltersSync()
        val filters = if (scope == BackupScope.SINGLE_PROFILE) {
            val scopedFilterIds = budgetFilterIds + loanFilterIds
            allFilters.filter { it.id in scopedFilterIds }
        } else allFilters

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
                "tags"
            ),
            filters.map {
                listOf(
                    it.id,
                    it.name,
                    json.encodeToString(it.timelineRange),
                    it.queryNote,
                    it.isNoteRegex,
                    it.queryPayee,
                    it.isPayeeRegex,
                    json.encodeToString(it.categoryIds),
                    json.encodeToString(it.accountIds),
                    it.startDate,
                    it.endDate,
                    json.encodeToString(it.paymentMethods),
                    it.hasWarranty,
                    it.warrantyExpirationStart,
                    it.warrantyExpirationEnd,
                    it.minAmount,
                    it.maxAmount,
                    json.encodeToString(it.transactionTypes),
                    json.encodeToString(it.currencies),
                    json.encodeToString(it.statuses),
                    json.encodeToString(it.linkedTransactionIds),
                    json.encodeToString(it.tags)
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
        val writer = OutputStreamWriter(zos, Charsets.UTF_8)
        val format = CSVFormat.DEFAULT.builder()
            .setHeader(*headers.toTypedArray())
            .build()
        val printer = CSVPrinter(writer, format)
        rows.forEach { row ->
            printer.printRecord(row)
        }
        printer.flush()
        // We do NOT close the printer/writer here as it would close the underlying ZipOutputStream.
        // ZipOutputStream.closeEntry() is sufficient.
        zos.closeEntry()
    }

    private fun exportSqlite(outputStream: OutputStream) {
        val dbFile = context.getDatabasePath("osswallet_manager_db")
        if (dbFile.exists()) {
            FileInputStream(dbFile).use { it.copyTo(outputStream) }
        }
    }

    override suspend fun importData(uriString: String, type: BackupType): Result<Unit> =
        withContext(dispatcherProvider.io) {
            try {
                val uri = uriString.toUri()
                context.contentResolver.openInputStream(uri)?.use { inputStream ->
                    when (type) {
                        BackupType.CSV_ZIP -> restoreZipBackup(inputStream)
                        BackupType.SQLITE -> restoreSqliteBackup(inputStream)
                        BackupType.FLAT_CSV -> throw UnsupportedOperationException("FLAT_CSV import not supported via importData; use importDataSelective or a dedicated importer")
                    }
                }
                Result.success(Unit)
            } catch (e: Exception) {
                Log.e("BackupRepo", "importData failed", e)
                Result.failure(e)
            }
        }

    override suspend fun deleteAllData(): Result<Unit> = withContext(dispatcherProvider.io) {
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
                categoryDao.clearAll()
                currencyDao.clearAll()

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

            Result.success(Unit)
        } catch (e: Exception) {
            Log.e("BackupRepo", "deleteAllData failed", e)
            Result.failure(e)
        }
    }

    private fun restoreSqliteBackup(inputStream: InputStream) {
        val dbFile = context.getDatabasePath("osswallet_manager_db")
        database.close()

        val maxTotalSize = 1024 * 1024 * 1024L // 1GB limit
        var totalSize = 0L

        FileOutputStream(dbFile).use { output ->
            val buffer = ByteArray(4096)
            var n: Int
            while (inputStream.read(buffer).also { n = it } != -1) {
                output.write(buffer, 0, n)
                totalSize += n
                if (totalSize > maxTotalSize) throw IOException("SQLite backup exceeds 1GB limit")
            }
        }
        // Restart will be handled by the caller to allow UI to show a message.
    }

    private suspend fun restoreZipBackup(inputStream: InputStream) {
        val tempDir = File(context.cacheDir, "restore_temp")
        tempDir.deleteRecursively()
        tempDir.mkdirs()

        val maxTotalSize = 1024 * 1024 * 1024L // 1GB limit
        var totalSize = 0L

        ZipInputStream(inputStream).use { zis ->
            var entry = zis.nextEntry
            while (entry != null) {
                val file = File(tempDir, entry.name)
                // ZIP Slip Validation
                if (!file.canonicalPath.startsWith(tempDir.canonicalPath)) {
                    throw SecurityException("Malicious ZIP entry detected: ${entry.name}")
                }
                if (!entry.isDirectory) {
                    file.parentFile?.mkdirs()
                    FileOutputStream(file).use { fos ->
                        val buffer = ByteArray(4096)
                        var n: Int
                        while (zis.read(buffer).also { n = it } != -1) {
                            fos.write(buffer, 0, n)
                            totalSize += n
                            if (totalSize > maxTotalSize) throw IOException("Zip Bomb detected: uncompressed size exceeds 1GB")
                        }
                    }
                }
                entry = zis.nextEntry
            }
        }

        database.withTransaction {
            // Clear in dependency order: children before parents to satisfy FK constraints.
            // transactions.categoryId has ON DELETE RESTRICT — transactions must be cleared
            // before categories. transaction_links and loan_payments reference transactions.
            transactionLinkDao.clearAll()
            transactionDao.clearAllCrossRefs()
            loanPaymentDao.clearAll()
            plannedPaymentOccurrenceDao.clearAll()
            transactionDao.clearAll()
            templateDao.clearAll()
            budgetDao.clearAll()
            loanDao.clearAll()
            plannedPaymentDao.clearAll()
            accountDao.clearAll()
            filterDao.clearAll()
            tagDao.clearAll()
            categoryDao.clearAll()
            profileDao.clearAll()

            // Restore in strict parent-before-child order to satisfy all FK constraints.
            // filters must precede budgets (budgets.filterId → filters.id) and
            // loans (loans.filterId → filters.id).
            // templates must precede planned_payments (planned_payments.templateId → templates.id).
            // planned_payments must precede planned_payment_occurrences.
            // loans must precede loan_payments.
            restoreProfiles(File(tempDir, "profiles.csv"))
            restoreAccounts(File(tempDir, "accounts.csv"))
            restoreCategories(File(tempDir, "categories.csv"))
            restoreTags(File(tempDir, "tags.csv"))
            restoreFilters(File(tempDir, "filters.csv"))
            restoreTemplates(File(tempDir, "templates.csv"))
            restoreTransactions(File(tempDir, "transactions.csv"))
            restoreLinks(File(tempDir, "transaction_links.csv"))
            restoreBudgets(File(tempDir, "budgets.csv"))
            restoreLoans(File(tempDir, "loans.csv"))
            restoreLoanPayments(File(tempDir, "loan_payments.csv"))
            restorePlannedPayments(File(tempDir, "planned_payments.csv"))
            restorePlannedPaymentOccurrences(File(tempDir, "planned_payment_occurrences.csv"))
            restoreCurrencyRates(File(tempDir, "currency_rates.csv"))
        }
        tempDir.deleteRecursively()
    }

    private suspend fun parseCsv(file: File, action: suspend (Map<String, String>) -> Unit) {
        if (!file.exists()) return
        val format = CSVFormat.DEFAULT.builder()
            .setHeader()
            .setSkipHeaderRecord(true)
            .setIgnoreEmptyLines(true)
            .setTrim(true)
            .build()
        file.reader(Charsets.UTF_8).use { reader ->
            CSVParser.parse(reader, format).use { parser ->
                for (record in parser) {
                    action(record.toMap())
                }
            }
        }
    }

    private suspend fun restoreProfiles(file: File) = file.takeIf { it.exists() }?.let { f ->
        parseCsv(f) { p ->
            profileDao.insertProfile(
                ProfileEntity(
                    id = p["id"]?.toIntOrNull() ?: 0,
                    name = p["name"] ?: "",
                    baseCurrency = p["baseCurrency"] ?: "USD",
                    type = p["type"] ?: "PERSONAL"
                )
            )
        }
    }

    private suspend fun restoreAccounts(file: File) = file.takeIf { it.exists() }?.let { f ->
        parseCsv(f) { p ->
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

    private suspend fun restoreCategories(file: File) = file.takeIf { it.exists() }?.let { f ->
        parseCsv(f) { p ->
            categoryDao.insertCategory(
                CategoryEntity(
                    id = p["id"]?.toLongOrNull() ?: 0,
                    name = p["name"] ?: "",
                    iconName = p["iconName"] ?: p["icon"] ?: "",
                    colorHex = p["colorHex"]?.toIntOrNull() ?: 0,
                    isDefault = p["isDefault"]?.toBoolean() ?: false,
                    parentId = p["parentId"]?.toLongOrNull(),
                    nature = p["nature"]?.let { n -> runCatching { CategoryNature.valueOf(n) }.getOrNull() }
                        ?: CategoryNature.WANT
                )
            )
        }
    }

    private suspend fun restoreTransactions(file: File) = file.takeIf { it.exists() }?.let { f ->
        val batch = mutableListOf<TransactionEntity>()
        parseCsv(f) { p ->
            batch.add(
                TransactionEntity(
                    id = p["id"]?.toLongOrNull() ?: 0,
                    name = p["name"] ?: "",
                    amount = p["amount"]?.toBigDecimalOrNull() ?: BigDecimal.ZERO,
                    timestamp = p["timestamp"]?.toLongOrNull() ?: 0,
                    currency = p["currency"] ?: "USD",
                    transactionType = TransactionType.fromValue(p["transactionType"]),
                    accountId = p["accountId"] ?: "",
                    categoryId = p["categoryId"]?.toLongOrNull() ?: 1002,
                    tags = parseTagNames(p["tags"]),
                    targetAccountId = p["targetAccountId"],
                    targetCurrency = p["targetCurrency"],
                    transferRate = p["transferRate"]?.toBigDecimalOrNull() ?: BigDecimal.ONE,
                    paymentMethod = p["paymentMethod"]?.let { m -> PaymentMethod.fromString(m) }
                        ?: PaymentMethod.CASH,
                    payee = p["payee"] ?: "",
                    note = p["note"] ?: "",
                    sourcePlannedPaymentId = p["sourcePlannedPaymentId"]?.toLongOrNull(),
                    profileId = p["profileId"]?.toIntOrNull() ?: 1,
                    gpsLocation = p["gpsLocation"],
                    location = p["location"],
                    warrantyExpiration = p["warrantyExpiration"]?.toLongOrNull(),
                    status = p["status"]?.let { s -> TransactionStatus.fromString(s) }
                        ?: TransactionStatus.CLEARED
                )
            )
            if (batch.size >= 100) {
                transactionDao.insertTransactions(batch.toList())
                batch.clear()
            }
        }
        if (batch.isNotEmpty()) transactionDao.insertTransactions(batch)
    }

    private suspend fun restoreLinks(file: File) = file.takeIf { it.exists() }?.let { f ->
        val batch = mutableListOf<TransactionLinkEntity>()
        parseCsv(f) { p ->
            batch.add(
                TransactionLinkEntity(
                    id = p["id"]?.toLongOrNull() ?: 0,
                    transactionId = p["transactionId"]?.toLongOrNull() ?: 0,
                    linkedTransactionId = p["linkedTransactionId"]?.toLongOrNull() ?: 0
                )
            )
            if (batch.size >= 100) {
                transactionLinkDao.insertLinks(batch.toList())
                batch.clear()
            }
        }
        if (batch.isNotEmpty()) transactionLinkDao.insertLinks(batch)
    }

    private suspend fun restoreTemplates(file: File) = file.takeIf { it.exists() }?.let { f ->
        parseCsv(f) { p ->
            templateDao.insertTemplate(
                TemplateEntity(
                    id = p["id"]?.toLongOrNull() ?: 0,
                    name = p["name"] ?: "",
                    categoryId = p["categoryId"]?.toLongOrNull() ?: 1002,
                    amount = p["amount"]?.toBigDecimalOrNull() ?: BigDecimal.ZERO,
                    entryMode = when (p["transactionType"]?.toIntOrNull() ?: 0) {
                        1 -> EntryMode.INCOME
                        2 -> EntryMode.TRANSFER
                        else -> EntryMode.EXPENSE
                    },
                    icon = p["icon"],
                    colorHex = p["colorHex"],
                    accountId = p["accountId"],
                    targetAccountId = p["targetAccountId"],
                    targetCurrency = p["targetCurrency"],
                    transferRate = p["transferRate"]?.toBigDecimalOrNull() ?: BigDecimal.ONE,
                    note = p["note"],
                    payee = p["payee"],
                    location = p["location"],
                    paymentMethod = p["paymentMethod"]?.let { m -> PaymentMethod.fromString(m) },
                    currency = p["currency"] ?: "USD",
                    tags = parseTagNames(p["tags"]),
                    isStarred = p["isStarred"]?.toBoolean() ?: false,
                    displayOrder = p["displayOrder"]?.toIntOrNull() ?: 0,
                    profileId = p["profileId"]?.toIntOrNull() ?: 1,
                    transactionStatus = p["transactionStatus"]?.let { s ->
                        TransactionStatus.fromString(s)
                    } ?: TransactionStatus.CLEARED
                ))
        }
    }

    private suspend fun restoreBudgets(file: File) = file.takeIf { it.exists() }?.let { f ->
        parseCsv(f) { p ->
            val filterId = p["filterId"]?.toLongOrNull()
            if (filterId != null) {
                budgetDao.insert(
                    BudgetEntity(
                        id = p["id"]?.toLongOrNull() ?: 0,
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

    private suspend fun restorePlannedPayments(file: File) = file.takeIf { it.exists() }?.let { f ->
        parseCsv(f) { p ->
            val templateId = p["templateId"]?.toLongOrNull()
            if (templateId != null) {
                plannedPaymentDao.insert(
                    PlannedPaymentEntity(
                        id = p["id"]?.toLongOrNull() ?: 0,
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
                        status = p["status"]?.let { s -> PlannedPaymentStatus.fromString(s) }
                            ?: PlannedPaymentStatus.ACTIVE,
                        profileId = p["profileId"]?.toIntOrNull() ?: 1,
                        templateId = templateId
                    ))
            }
        }
    }

    private suspend fun restorePlannedPaymentOccurrences(file: File) =
        file.takeIf { it.exists() }?.let { f ->
            val batch = mutableListOf<PlannedPaymentOccurrenceEntity>()
            parseCsv(f) { p ->
                batch.add(
                    PlannedPaymentOccurrenceEntity(
                        id = p["id"]?.toLongOrNull() ?: 0,
                        plannedPaymentId = p["plannedPaymentId"]?.toLongOrNull() ?: 0,
                        scheduledDate = p["scheduledDate"]?.toLongOrNull() ?: 0,
                        generatedTransactionId = p["generatedTransactionId"]?.toLongOrNull(),
                        status = p["status"]?.let { s -> OccurrenceStatus.fromString(s) }
                            ?: OccurrenceStatus.PENDING
                    )
                )
                if (batch.size >= 100) {
                    plannedPaymentOccurrenceDao.insertOccurrences(batch.toList())
                    batch.clear()
                }
            }
            if (batch.isNotEmpty()) plannedPaymentOccurrenceDao.insertOccurrences(batch)
        }

    private suspend fun restoreTags(file: File) = file.takeIf { it.exists() }?.let { f ->
        parseCsv(f) { p ->
            tagDao.insertTag(
                TagEntity(
                    id = p["id"]?.toLongOrNull() ?: 0,
                    name = p["name"] ?: "",
                    colorHex = p["colorHex"]?.toIntOrNull() ?: 0
                )
            )
        }
    }

    private fun parseTagNames(labelsStr: String?): Set<String> {
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

    private suspend fun restoreLoans(file: File) = file.takeIf { it.exists() }?.let { f ->
        parseCsv(f) { p ->
            val filterId = p["filterId"]?.toLongOrNull()
            if (filterId != null) {
                loanDao.insert(
                    LoanEntity(
                        id = p["id"]?.toLongOrNull() ?: 0,
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

    private suspend fun restoreLoanPayments(file: File) = file.takeIf { it.exists() }?.let { f ->
        val batch = mutableListOf<LoanPaymentEntity>()
        parseCsv(f) { p ->
            batch.add(
                LoanPaymentEntity(
                    id = p["id"]?.toLongOrNull() ?: 0,
                    loanId = p["loanId"]?.toLongOrNull() ?: 0,
                    amount = p["amount"]?.toBigDecimalOrNull() ?: BigDecimal.ZERO,
                    date = p["date"]?.toLongOrNull() ?: 0,
                    transactionId = p["transactionId"]?.toLongOrNull(),
                    note = p["note"] ?: ""
                )
            )
            if (batch.size >= 100) {
                loanPaymentDao.insertPayments(batch.toList())
                batch.clear()
            }
        }
        if (batch.isNotEmpty()) loanPaymentDao.insertPayments(batch)
    }

    private suspend fun restoreFilters(file: File) = file.takeIf { it.exists() }?.let { f ->
        parseCsv(f) { p ->
            filterDao.insertFilter(
                FilterEntity(
                    id = p["id"]?.toLongOrNull() ?: 0,
                    name = p["name"] ?: "",
                    timelineRange = p["timelineRange"]?.let { tr ->
                        try {
                            json.decodeFromString<TimelineRange>(tr)
                        } catch (_: Exception) {
                            TimelineRange.AllTime()
                        }
                    } ?: TimelineRange.AllTime(),
                    queryNote = p["queryNote"],
                    isNoteRegex = p["isNoteRegex"]?.toBoolean() ?: false,
                    queryPayee = p["queryPayee"],
                    isPayeeRegex = p["isPayeeRegex"]?.toBoolean() ?: false,
                    categoryIds = p["categoryIds"]?.let { cids ->
                        try {
                            json.decodeFromString<Set<Long>>(cids)
                        } catch (_: Exception) {
                            emptySet()
                        }
                    } ?: emptySet(),
                    accountIds = p["accountIds"]?.let { aids ->
                        try {
                            json.decodeFromString<Set<String>>(aids)
                        } catch (_: Exception) {
                            emptySet()
                        }
                    } ?: emptySet(),
                    startDate = p["startDate"]?.toLongOrNull(),
                    endDate = p["endDate"]?.toLongOrNull(),
                    paymentMethods = p["paymentMethods"]?.let { pms ->
                        try {
                            json.decodeFromString<Set<PaymentMethod>>(pms)
                        } catch (_: Exception) {
                            emptySet()
                        }
                    } ?: emptySet(),
                    hasWarranty = p["hasWarranty"]?.let { hw -> if (hw == "true") true else if (hw == "false") false else null },
                    warrantyExpirationStart = p["warrantyExpirationStart"]?.toLongOrNull(),
                    warrantyExpirationEnd = p["warrantyExpirationEnd"]?.toLongOrNull(),
                    minAmount = p["minAmount"]?.toBigDecimalOrNull(),
                    maxAmount = p["maxAmount"]?.toBigDecimalOrNull(),
                    transactionTypes = p["transactionTypes"]?.let { tts ->
                        try {
                            json.decodeFromString<Set<TransactionType>>(tts)
                        } catch (_: Exception) {
                            emptySet()
                        }
                    } ?: emptySet(),
                    currencies = p["currencies"]?.let { curs ->
                        try {
                            json.decodeFromString<Set<String>>(curs)
                        } catch (_: Exception) {
                            emptySet()
                        }
                    } ?: emptySet(),
                    statuses = p["statuses"]?.let { st ->
                        try {
                            json.decodeFromString<Set<TransactionStatus>>(st)
                        } catch (_: Exception) {
                            emptySet()
                        }
                    } ?: emptySet(),
                    linkedTransactionIds = p["linkedTransactionIds"]?.let { ltids ->
                        try {
                            json.decodeFromString<Set<Long>>(ltids)
                        } catch (_: Exception) {
                            emptySet()
                        }
                    } ?: emptySet(),
                    tags = p["tags"]?.let { tgs ->
                        try {
                            json.decodeFromString<Set<String>>(tgs)
                        } catch (_: Exception) {
                            emptySet()
                        }
                    } ?: emptySet()
                ))
        }
    }

    private suspend fun restoreCurrencyRates(file: File) = file.takeIf { it.exists() }?.let { f ->
        // Backup-restore path: wipe and re-insert. clearAll() is safe here because the
        // backup contains all cached bases; ARC-09's scoped delete is for runtime fetches.
        currencyDao.clearAll()
        val rates = mutableListOf<CurrencyRateEntity>()
        parseCsv(f) { p ->
            rates.add(
                CurrencyRateEntity(
                    currencyCode = p["currencyCode"] ?: "",
                    baseCurrency = p["baseCurrency"] ?: "USD",
                    rate = p["rate"] ?: "1.0",
                    timestamp = p["timestamp"]?.toLongOrNull() ?: 0,
                    nextUpdate = p["nextUpdate"]?.toLongOrNull() ?: 0
                )
            )
            if (rates.size >= 100) {
                currencyDao.insertRates(rates.toList())
                rates.clear()
            }
        }
        if (rates.isNotEmpty()) currencyDao.insertRates(rates)
    }

    override suspend fun importDataSelective(
        uriString: String,
        type: BackupType,
        entities: Set<RestorableEntity>
    ): Result<Unit> = withContext(dispatcherProvider.io) {
        try {
            val uri = uriString.toUri()
            context.contentResolver.openInputStream(uri)?.use { inputStream ->
                when (type) {
                    BackupType.CSV_ZIP -> restoreZipBackupSelective(inputStream, entities)
                    else -> throw UnsupportedOperationException("Selective restore only supported for CSV_ZIP")
                }
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private suspend fun restoreZipBackupSelective(inputStream: InputStream, entities: Set<RestorableEntity>) {
        val safeEntities = entities.toMutableSet()
        if (RestorableEntity.TRANSACTIONS in safeEntities) {
            safeEntities.add(RestorableEntity.ACCOUNTS)
            safeEntities.add(RestorableEntity.CATEGORIES)
            safeEntities.add(RestorableEntity.TAGS)
        }
        if (RestorableEntity.BUDGETS in safeEntities && RestorableEntity.FILTERS !in safeEntities) {
            safeEntities.add(RestorableEntity.FILTERS)
        }
        if (RestorableEntity.LOANS in safeEntities && RestorableEntity.FILTERS !in safeEntities) {
            safeEntities.add(RestorableEntity.FILTERS)
        }
        if (RestorableEntity.PLANNED_PAYMENTS in safeEntities && RestorableEntity.TEMPLATES !in safeEntities) {
            safeEntities.add(RestorableEntity.TEMPLATES)
        }

        val tempDir = File(context.cacheDir, "restore_temp_selective")
        tempDir.deleteRecursively()
        tempDir.mkdirs()

        val maxTotalSize = 1024 * 1024 * 1024L // 1GB limit
        var totalSize = 0L

        ZipInputStream(inputStream).use { zis ->
            var entry = zis.nextEntry
            while (entry != null) {
                val file = File(tempDir, entry.name)
                // ZIP Slip Validation
                if (!file.canonicalPath.startsWith(tempDir.canonicalPath)) {
                    throw SecurityException("Malicious ZIP entry detected: ${entry.name}")
                }
                
                val fileName = entry.name
                val shouldRestore = safeEntities.any { entity ->
                    when (entity) {
                        RestorableEntity.PROFILES -> fileName == "profiles.csv"
                        RestorableEntity.ACCOUNTS -> fileName == "accounts.csv"
                        RestorableEntity.CATEGORIES -> fileName == "categories.csv"
                        RestorableEntity.TRANSACTIONS -> fileName == "transactions.csv" || fileName == "transaction_links.csv"
                        RestorableEntity.BUDGETS -> fileName == "budgets.csv"
                        RestorableEntity.LOANS -> fileName == "loans.csv" || fileName == "loan_payments.csv"
                        RestorableEntity.PLANNED_PAYMENTS -> fileName == "planned_payments.csv" || fileName == "planned_payment_occurrences.csv"
                        RestorableEntity.TEMPLATES -> fileName == "templates.csv"
                        RestorableEntity.FILTERS -> fileName == "filters.csv"
                        RestorableEntity.TAGS -> fileName == "tags.csv"
                        RestorableEntity.CURRENCY_RATES -> fileName == "currency_rates.csv"
                    }
                }

                if (shouldRestore && !entry.isDirectory) {
                    file.parentFile?.mkdirs()
                    FileOutputStream(file).use { fos ->
                        val buffer = ByteArray(4096)
                        var n: Int
                        while (zis.read(buffer).also { n = it } != -1) {
                            fos.write(buffer, 0, n)
                            totalSize += n
                            if (totalSize > maxTotalSize) throw IOException("Zip Bomb detected: uncompressed size exceeds 1GB")
                        }
                    }
                }
                entry = zis.nextEntry
            }
        }

        database.withTransaction {
            // Restore in strict parent-before-child order to satisfy all FK constraints.
            // Only clear and restore if the file actually exists in the ZIP to prevent accidental wipes.
            if (RestorableEntity.PROFILES in safeEntities) File(tempDir, "profiles.csv").let { if (it.exists()) { profileDao.clearAll(); restoreProfiles(it) } }
            if (RestorableEntity.ACCOUNTS in safeEntities) File(tempDir, "accounts.csv").let { if (it.exists()) { accountDao.clearAll(); restoreAccounts(it) } }
            if (RestorableEntity.CATEGORIES in safeEntities) File(tempDir, "categories.csv").let { if (it.exists()) { categoryDao.clearAll(); restoreCategories(it) } }
            if (RestorableEntity.TAGS in safeEntities) File(tempDir, "tags.csv").let { if (it.exists()) { tagDao.clearAll(); restoreTags(it) } }
            if (RestorableEntity.FILTERS in safeEntities) File(tempDir, "filters.csv").let { if (it.exists()) { filterDao.clearAll(); restoreFilters(it) } }
            if (RestorableEntity.TEMPLATES in safeEntities) File(tempDir, "templates.csv").let { if (it.exists()) { templateDao.clearAll(); restoreTemplates(it) } }
            if (RestorableEntity.TRANSACTIONS in safeEntities) File(tempDir, "transactions.csv").let { if (it.exists()) { transactionDao.clearAll(); transactionLinkDao.clearAll(); restoreTransactions(it); restoreLinks(File(tempDir, "transaction_links.csv")) } }
            if (RestorableEntity.BUDGETS in safeEntities) File(tempDir, "budgets.csv").let { if (it.exists()) { budgetDao.clearAll(); restoreBudgets(it) } }
            if (RestorableEntity.LOANS in safeEntities) File(tempDir, "loans.csv").let { if (it.exists()) { loanDao.clearAll(); loanPaymentDao.clearAll(); restoreLoans(it); restoreLoanPayments(File(tempDir, "loan_payments.csv")) } }
            if (RestorableEntity.PLANNED_PAYMENTS in safeEntities) File(tempDir, "planned_payments.csv").let { if (it.exists()) { plannedPaymentDao.clearAll(); plannedPaymentOccurrenceDao.clearAll(); restorePlannedPayments(it); restorePlannedPaymentOccurrences(File(tempDir, "planned_payment_occurrences.csv")) } }
            if (RestorableEntity.CURRENCY_RATES in safeEntities) File(tempDir, "currency_rates.csv").let { if (it.exists()) { restoreCurrencyRates(it) } }
        }
        tempDir.deleteRecursively()
    }

    override suspend fun exportToPrivateStorage(scope: BackupScope): Result<String> =
        withContext(dispatcherProvider.io) {
            try {
                val currentProfileId = settingsDataStore.currentProfileId.first()
                val dir = File(context.filesDir, "backups").apply { mkdirs() }
                val scopeSuffix = if (scope == BackupScope.SINGLE_PROFILE) "_profile" else "_all"
                val fileName = "osswallet_backup_${System.currentTimeMillis()}${scopeSuffix}.zip"
                val file = File(dir, fileName)
                FileOutputStream(file).use { createZipBackup(it, scope, currentProfileId) }
                Result.success(file.absolutePath)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }

    override suspend fun listPrivateBackups(): List<BackupMeta> = withContext(dispatcherProvider.io) {
        val dir = File(context.filesDir, "backups")
        if (!dir.exists()) return@withContext emptyList()
        dir.listFiles { f -> f.name.endsWith(".zip") || f.name.endsWith(".sqlite") }
            ?.sortedByDescending { it.lastModified() }
            ?.map { f ->
                BackupMeta(
                    id = f.name,
                    path = f.absolutePath,
                    createdAt = f.lastModified(),
                    sizeBytes = f.length(),
                    scope = if (f.name.contains("_profile")) BackupScope.SINGLE_PROFILE else BackupScope.ALL_PROFILES
                )
            } ?: emptyList()
    }

    override suspend fun deleteBackup(id: String): Result<Unit> = withContext(dispatcherProvider.io) {
        try {
            val file = File(context.filesDir, "backups/$id")
            if (file.exists()) file.delete()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun pruneOldBackups(maxRetained: Int) {
        withContext(dispatcherProvider.io) {
            val backups = listPrivateBackups()
            if (backups.size > maxRetained) {
                backups.drop(maxRetained).forEach { deleteBackup(it.id) }
            }
        }
    }
}
