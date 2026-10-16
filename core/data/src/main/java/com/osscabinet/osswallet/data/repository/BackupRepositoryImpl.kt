package com.osscabinet.ossWallet.data.repository

import android.content.Context
import android.util.Log
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
import com.osscabinet.ossWallet.domain.model.CategoryNature
import com.osscabinet.ossWallet.domain.model.EntryMode
import com.osscabinet.ossWallet.domain.model.OccurrenceStatus
import com.osscabinet.ossWallet.domain.model.PaymentMethod
import com.osscabinet.ossWallet.domain.model.PlannedPaymentStatus
import com.osscabinet.ossWallet.domain.model.SystemCategory
import com.osscabinet.ossWallet.domain.model.SystemTag
import com.osscabinet.ossWallet.domain.model.TransactionStatus
import com.osscabinet.ossWallet.domain.model.TransactionType
import com.osscabinet.ossWallet.domain.model.isTransfer
import com.osscabinet.ossWallet.domain.model.timeline.TimelineRange
import com.osscabinet.ossWallet.domain.repository.BackupMeta
import com.osscabinet.ossWallet.domain.repository.BackupRepository
import com.osscabinet.ossWallet.domain.repository.BackupScope
import com.osscabinet.ossWallet.domain.repository.BackupType
import com.osscabinet.ossWallet.domain.repository.RestorableEntity
import com.univocity.parsers.csv.CsvParser
import com.univocity.parsers.csv.CsvParserSettings
import com.univocity.parsers.csv.CsvWriter
import com.univocity.parsers.csv.CsvWriterSettings
import dagger.hilt.android.qualifiers.ApplicationContext
import jakarta.inject.Inject
import jakarta.inject.Singleton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream
import java.io.OutputStreamWriter
import java.math.BigDecimal
import java.util.zip.ZipInputStream
import com.osscabinet.ossWallet.data.local.BackupKeyVault
import com.osscabinet.ossWallet.data.local.AttachmentStore
import com.osscabinet.ossWallet.data.local.dao.AttachmentDao
import com.osscabinet.ossWallet.data.local.dao.GoalDao
import com.osscabinet.ossWallet.data.local.entity.AttachmentEntity
import com.osscabinet.ossWallet.data.local.entity.GoalEntity
import com.osscabinet.ossWallet.domain.repository.BackupPassphraseRequiredException
import com.osscabinet.ossWallet.domain.repository.BackupVerification
import com.osscabinet.ossWallet.domain.service.security.BackupCrypto
import java.io.BufferedInputStream
import java.util.zip.ZipOutputStream

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
    private val goalDao: GoalDao,
    private val attachmentDao: AttachmentDao,
    private val attachmentStore: AttachmentStore,
    private val backupKeyVault: BackupKeyVault,
    private val database: AppDatabase,
    private val settingsDataStore: SettingsDataStore,
    private val dispatcherProvider: DispatcherProvider,
    private val json: Json,
    @param:ApplicationContext private val context: Context,
) : BackupRepository {

    companion object {
        private const val MAX_RESTORE_BYTES = 512L * 1024 * 1024 // 512 MB
        const val ENCRYPTED_SUFFIX = ".enc"
        private const val SQLITE_DB_ENTRY = "database.db"
        private const val SQLITE_ATTACHMENTS_DIR = "attachments/"

        /** Sink for verification reads (OutputStream.nullOutputStream() needs API 33). */
        private val DiscardOutputStream = object : OutputStream() {
            override fun write(b: Int) {}
            override fun write(b: ByteArray, off: Int, len: Int) {}
        }
    }

    override suspend fun exportData(
        uriString: String,
        type: BackupType,
        scope: BackupScope,
        passphrase: CharArray?
    ): Result<Unit> =
        withContext(dispatcherProvider.io) {
            try {
                val uri = uriString.toUri()
                val currentProfileId = settingsDataStore.currentProfileId.first()
                context.contentResolver.openOutputStream(uri)?.use { raw ->
                    val outputStream = if (passphrase != null) BackupCrypto.encrypt(raw, passphrase) else raw
                    // Closing the encrypting stream writes the final authenticated chunk.
                    outputStream.use {
                        when (type) {
                            BackupType.CSV_ZIP -> createZipBackup(it, scope, currentProfileId)
                            BackupType.SQLITE -> exportSqlite(it)
                            BackupType.FLAT_CSV -> exportFlatCsv(it, scope)
                        }
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

        val headers = arrayOf(
            "date",
            "account",
            "category",
            "type",
            "amount",
            "currency",
            "payee",
            "note",
            "payment_type",
            "warranty_months",
            "labels",
            "is_transfer"
        )

        val settings = CsvWriterSettings().apply {
            setHeaders(*headers)
        }
        val writer = CsvWriter(OutputStreamWriter(outputStream, Charsets.UTF_8), settings)
        writer.writeHeaders()

        transactions.forEach { tx ->
            val warrantyMonths = tx.warrantyExpiration?.let {
                ((it - tx.timestamp) / (1000L * 60 * 60 * 24 * 30)).toInt().coerceAtLeast(0)
            } ?: 0
            writer.writeRow(
                tx.timestamp.toString(),
                accountsById[tx.accountId]?.name ?: tx.accountId ?: "",
                categoriesById[tx.categoryId]?.name ?: tx.categoryId.toString(),
                tx.transactionType.name,
                tx.amount.toPlainString(),
                tx.currency,
                tx.payee,
                tx.note,
                tx.paymentMethod.name,
                warrantyMonths.toString(),
                tx.tags.joinToString("|"),
                tx.transactionType.isTransfer.toString()
            )
        }
        writer.flush()
    }

    private suspend fun createZipBackup(
        outputStream: OutputStream,
        scope: BackupScope = BackupScope.ALL_PROFILES,
        currentProfileId: Int = 1
    ) {
        val zos = ZipOutputStream(outputStream)

        val allProfiles = profileDao.getAllProfilesSync()
        val profiles = if (scope == BackupScope.SINGLE_PROFILE)
            allProfiles.filter { it.id == currentProfileId } else allProfiles
        writeCsvToZip(
            zos, "profiles.csv", arrayOf("id", "name", "baseCurrency", "type"),
            profiles.map { arrayOf(it.id, it.name, it.baseCurrency, it.type) })

        val allAccounts = accountDao.getAllAccountsSync()
        val accounts = if (scope == BackupScope.SINGLE_PROFILE)
            allAccounts.filter { it.profileId == currentProfileId } else allAccounts
        writeCsvToZip(
            zos,
            "accounts.csv",
            arrayOf(
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
                arrayOf(
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
            arrayOf("id", "name", "iconName", "colorHex", "isDefault", "parentId", "nature"),
            categoryDao.getAllCategoriesSync().map {
                arrayOf(
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
        val transactionIds = transactions.asSequence().map { it.id }.toSet()
        writeCsvToZip(
            zos,
            "transactions.csv",
            arrayOf(
                "id",
                "name",
                "amount",
                "timestamp",
                "currency",
                "transactionType",
                "accountId",
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
                "status",
                "targetAccountId",
                "targetCurrency",
                "transferRate"
            ),
            transactions.map {
                arrayOf(
                    it.id,
                    it.name,
                    it.amount,
                    it.timestamp,
                    it.currency,
                    it.transactionType.name,
                    it.accountId,
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
                    it.status.name,
                    it.targetAccountId,
                    it.targetCurrency,
                    it.transferRate
                )
            })

        val allLinks = transactionLinkDao.getAllLinksSync()
        val links = if (scope == BackupScope.SINGLE_PROFILE)
            allLinks.filter { it.transactionId in transactionIds && it.linkedTransactionId in transactionIds }
        else allLinks
        writeCsvToZip(
            zos, "transaction_links.csv", arrayOf("id", "transactionId", "linkedTransactionId"),
            links.map { arrayOf(it.id, it.transactionId, it.linkedTransactionId) })

        val budgets = budgetDao.getAllBudgetsSync()
            .let { if (scope == BackupScope.SINGLE_PROFILE) it.filter { b -> b.budget.profileId == currentProfileId } else it }
        writeCsvToZip(
            zos,
            "budgets.csv",
            arrayOf(
                "id",
                "name",
                "currency",
                "limitAmount",
                "alertThreshold",
                "rollover",
                "isArchived",
                "profileId",
                "note",
                "filterId"
            ),
            budgets.map {
                val b = it.budget; arrayOf(
                b.id,
                b.name,
                b.currency,
                b.limitAmount,
                b.alertThreshold,
                b.rollover,
                b.isArchived,
                b.profileId,
                b.note,
                b.filterId
            )
            })

        val templates = templateDao.getAllTemplatesSync()
            .let { if (scope == BackupScope.SINGLE_PROFILE) it.filter { t -> t.profileId == currentProfileId } else it }
        writeCsvToZip(
            zos,
            "templates.csv",
            arrayOf(
                "id",
                "name",
                "categoryId",
                "amount",
                "entryMode",
                "icon",
                "colorHex",
                "accountId",
                "targetAccountId",
                "targetCurrency",
                "transferRate",
                "note",
                "payee",
                "location",
                "gpsLocation",
                "warrantyExpiration",
                "transactionStatus",
                "paymentMethod",
                "currency",
                "tags",
                "isStarred",
                "displayOrder",
                "profileId"
            ),
            templates.map {
                arrayOf(
                    it.id,
                    it.name,
                    it.categoryId,
                    it.amount,
                    it.entryMode.name,
                    it.icon,
                    it.colorHex,
                    it.accountId,
                    it.targetAccountId,
                    it.targetCurrency,
                    it.transferRate,
                    it.note,
                    it.payee,
                    it.location,
                    it.gpsLocation,
                    it.warrantyExpiration,
                    it.transactionStatus.name,
                    it.paymentMethod?.name,
                    it.currency,
                    it.tags.joinToString("|"),
                    it.isStarred,
                    it.displayOrder,
                    it.profileId
                )
            })

        val templateIds = templates.map { it.id }.toSet()
        val plannedPayments = plannedPaymentDao.getAllPlannedPaymentsSync().let {
            if (scope == BackupScope.SINGLE_PROFILE)
                it.filter { p -> p.payment.profileId == currentProfileId && p.payment.templateId in templateIds }
            else it
        }
        val plannedPaymentIds = plannedPayments.map { it.payment.id }.toSet()
        writeCsvToZip(
            zos,
            "planned_payments.csv",
            arrayOf(
                "id",
                "autoConfirm",
                "status",
                "profileId",
                "templateId",
                "recurrenceType",
                "interval",
                "dayOfWeek",
                "dayOfMonth",
                "recurrenceCronExpression",
                "startDate",
                "endDate",
                "nextDueDate",
                "lastTriggeredDate"
            ),
            plannedPayments.map {
                val p = it.payment; arrayOf(
                p.id,
                p.autoConfirm,
                p.status.name,
                p.profileId,
                p.templateId,
                p.schedule.recurrenceType,
                p.schedule.interval,
                p.schedule.dayOfWeek,
                p.schedule.dayOfMonth,
                p.schedule.recurrenceCronExpression,
                p.schedule.startDate,
                p.schedule.endDate,
                p.schedule.nextDueDate,
                p.schedule.lastTriggeredDate
            )
            })

        val occurrences = plannedPaymentOccurrenceDao.getAllOccurrencesSync()
            .let { if (scope == BackupScope.SINGLE_PROFILE) it.filter { o -> o.plannedPaymentId in plannedPaymentIds } else it }
        writeCsvToZip(
            zos,
            "planned_payment_occurrences.csv",
            arrayOf("id", "plannedPaymentId", "scheduledDate", "generatedTransactionId", "status"),
            occurrences.map {
                arrayOf(
                    it.id,
                    it.plannedPaymentId,
                    it.scheduledDate,
                    it.generatedTransactionId,
                    it.status.name
                )
            })

        writeCsvToZip(
            zos,
            "tags.csv",
            arrayOf("id", "name", "colorHex"),
            tagDao.getAllTagsSync().map { arrayOf(it.id, it.name, it.colorHex) })

        val loans = loanDao.getAllLoansSync()
            .let { if (scope == BackupScope.SINGLE_PROFILE) it.filter { l -> l.loan.profileId == currentProfileId } else it }
        val loanIds = loans.map { it.loan.id }.toSet()
        writeCsvToZip(
            zos,
            "loans.csv",
            arrayOf(
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
                val l = it.loan; arrayOf(
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

        val loanPayments = loanPaymentDao.getAllPaymentsSync()
            .let { if (scope == BackupScope.SINGLE_PROFILE) it.filter { lp -> lp.loanId in loanIds } else it }
        writeCsvToZip(
            zos,
            "loan_payments.csv",
            arrayOf("id", "loanId", "transactionId", "amount", "date", "note"),
            loanPayments.map {
                arrayOf(
                    it.id,
                    it.loanId,
                    it.transactionId,
                    it.amount,
                    it.date,
                    it.note
                )
            })

        val allFilters = filterDao.getAllFiltersSync()
        val filters = if (scope == BackupScope.SINGLE_PROFILE) {
            val scopedFilterIds =
                (budgets.map { it.budget.filterId } + loans.map { it.loan.filterId }).toSet()
            allFilters.filter { it.id in scopedFilterIds }
        } else allFilters
        writeCsvToZip(
            zos,
            "filters.csv",
            arrayOf(
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
                arrayOf(
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

        val allGoals = goalDao.getAllGoalsForBackup()
        val goals = if (scope == BackupScope.SINGLE_PROFILE)
            allGoals.filter { it.profileId == currentProfileId } else allGoals
        writeCsvToZip(
            zos, "goals.csv",
            arrayOf(
                "id", "name", "targetAmount", "savedAmount", "currency", "deadline",
                "note", "profileId", "createdAt", "completedAt"
            ),
            goals.map {
                arrayOf(
                    it.id, it.name, it.targetAmount, it.savedAmount, it.currency, it.deadline,
                    it.note, it.profileId, it.createdAt, it.completedAt
                )
            })

        writeAttachmentsToZip(zos, scope, currentProfileId)

        writeCsvToZip(
            zos,
            "currency_rates.csv",
            arrayOf("currencyCode", "baseCurrency", "rate", "timestamp", "nextUpdate"),
            currencyDao.getAllRates().map {
                arrayOf(
                    it.currencyCode,
                    it.baseCurrency,
                    it.rate,
                    it.timestamp,
                    it.nextUpdate
                )
            })

        withContext(Dispatchers.IO) {
            zos.close()
        }
    }

    private fun writeCsvToZip(
        zos: ZipOutputStream,
        fileName: String,
        headers: Array<String>,
        rows: List<Array<Any?>>
    ) {
        zos.putNextEntry(java.util.zip.ZipEntry(fileName))
        val settings = CsvWriterSettings().apply {
            setHeaders(*headers)
        }
        val writer = CsvWriter(OutputStreamWriter(zos, Charsets.UTF_8), settings)
        writer.writeHeaders()
        rows.forEach { writer.writeRow(*it) }
        writer.flush()
        zos.closeEntry()
    }

    /**
     * The database file plus the sealed attachment files it refers to, as one zip. Database-file backups
     * only restore on this install (the file is bound to a device key), and so are the sealed files, which
     * is why they are copied as they are. Older backups are the bare database file; restore still takes those.
     */
    private fun exportSqlite(outputStream: OutputStream) {
        val dbFile = context.getDatabasePath("osswallet_manager_db")
        val zos = ZipOutputStream(outputStream)
        if (dbFile.exists()) {
            zos.putNextEntry(java.util.zip.ZipEntry(SQLITE_DB_ENTRY))
            FileInputStream(dbFile).use { it.copyTo(zos) }
            zos.closeEntry()
        }
        attachmentStore.listNames().sorted().forEach { name ->
            val file = attachmentStore.sealedFile(name) ?: return@forEach
            zos.putNextEntry(java.util.zip.ZipEntry("$SQLITE_ATTACHMENTS_DIR$name"))
            FileInputStream(file).use { it.copyTo(zos) }
            zos.closeEntry()
        }
        zos.finish()
    }

    override suspend fun importData(
        uriString: String,
        type: BackupType,
        passphrase: CharArray?
    ): Result<Unit> =
        withContext(dispatcherProvider.io) {
            try {
                withPlainInput(uriString, passphrase) { inputStream ->
                    when (type) {
                        BackupType.CSV_ZIP -> restoreZipBackup(inputStream)
                        BackupType.SQLITE -> restoreSqliteBackup(inputStream)
                        BackupType.FLAT_CSV -> throw UnsupportedOperationException("FLAT_CSV import not supported")
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
                transactionLinkDao.clearAll(); transactionDao.clearAllCrossRefs()
                loanPaymentDao.clearAll(); plannedPaymentOccurrenceDao.clearAll()
                transactionDao.clearAll(); templateDao.clearAll(); budgetDao.clearAll()
                loanDao.clearAll(); plannedPaymentDao.clearAll(); accountDao.clearAll(); filterDao.clearAll()
                goalDao.clearAll(); attachmentDao.clearAll()
                tagDao.clearAll(); categoryDao.clearAll(); currencyDao.clearAll()
                profileDao.clearAll()

                SystemCategory.entries.forEach { sc ->
                    categoryDao.insertCategory(
                        CategoryEntity(
                            sc.id,
                            sc.displayName,
                            sc.iconName,
                            sc.colorHex,
                            true,
                            sc.parentId,
                            sc.nature
                        )
                    )
                }
                SystemTag.entries.forEach { st ->
                    tagDao.insertTag(TagEntity(st.id, st.tagName, st.colorHex))
                }
            }
            attachmentStore.deleteAll()
            settingsDataStore.clearAll()
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e("BackupRepo", "deleteAllData failed", e)
            Result.failure(e)
        }
    }

    private fun restoreSqliteBackup(rawInput: InputStream) {
        val inputStream = if (rawInput.markSupported()) rawInput else java.io.BufferedInputStream(rawInput)
        inputStream.mark(4)
        val magic = ByteArray(4).also { inputStream.read(it) }
        inputStream.reset()
        val isBundle = magic[0] == 'P'.code.toByte() && magic[1] == 'K'.code.toByte() && magic[2] == 3.toByte()

        val dbFile = context.getDatabasePath("osswallet_manager_db")
        // P1: Write to a temp file in the same directory so renameTo is atomic (same mountpoint).
        // DB is closed only after the write succeeds, so a write failure leaves the live DB intact.
        val tempDb = File(
            dbFile.parentFile ?: context.cacheDir,
            "osswallet_restore_${System.currentTimeMillis()}.tmp"
        )
        val stagedAttachments = File(context.cacheDir, "restore_attachments_${System.currentTimeMillis()}")
        try {
            if (isBundle) {
                stageSqliteBundle(inputStream, tempDb, stagedAttachments)
            } else {
                FileOutputStream(tempDb).use { out -> inputStream.copyToLimited(out, longArrayOf(0L)) }
            }
            database.close()
            if (!tempDb.renameTo(dbFile)) tempDb.copyTo(dbFile, overwrite = true)
            // The database now matches the backup, so the files it refers to must too. An older, bare
            // database backup carries none, so it leaves the current files alone.
            if (isBundle) attachmentStore.replaceAll(stagedAttachments)
        } finally {
            tempDb.delete()
            stagedAttachments.deleteRecursively()
        }
    }

    /** Unpacks a database-file bundle without touching live data: the db to [tempDb], sealed files to [stagingDir]. */
    private fun stageSqliteBundle(input: InputStream, tempDb: File, stagingDir: File) {
        stagingDir.deleteRecursively(); stagingDir.mkdirs()
        val counter = longArrayOf(0L)
        var sawDb = false
        ZipInputStream(input).use { zis ->
            var entry = zis.nextEntry
            while (entry != null) {
                when {
                    entry.isDirectory -> Unit
                    entry.name == SQLITE_DB_ENTRY -> {
                        FileOutputStream(tempDb).use { zis.copyToLimited(it, counter) }
                        sawDb = true
                    }
                    entry.name.startsWith(SQLITE_ATTACHMENTS_DIR) -> {
                        // Only names this app generates are taken, and only by their last path segment.
                        val name = File(entry.name).name
                        if (AttachmentStore.isStoreFileName(name)) {
                            FileOutputStream(File(stagingDir, name)).use { zis.copyToLimited(it, counter) }
                        }
                    }
                }
                entry = zis.nextEntry
            }
        }
        if (!sawDb) throw IOException("Not a valid OssWallet database backup")
    }

    private suspend fun restoreZipBackup(inputStream: InputStream) {
        val tempDir = File(context.cacheDir, "restore_temp").apply { deleteRecursively(); mkdirs() }
        // P2: pre-compute with separator suffix so "restore_temp_evil/..." can't pass startsWith.
        val canonicalTempDir = tempDir.canonicalPath + File.separator
        // P4: single counter shared across all entries — a zip bomb spread over many entries is caught.
        val extractionCounter = longArrayOf(0L)
        ZipInputStream(inputStream).use { zis ->
            var entry = zis.nextEntry
            while (entry != null) {
                val file = File(tempDir, entry.name)
                if (!file.canonicalPath.startsWith(canonicalTempDir)) throw SecurityException("Zip Slip!")
                if (!entry.isDirectory) {
                    file.parentFile?.mkdirs()
                    FileOutputStream(file).use { fos -> zis.copyToLimited(fos, extractionCounter) }
                }
                entry = zis.nextEntry
            }
        }

        // Old receipt files are only deleted once the restore has committed; new ones are removed if it fails.
        val oldReceiptFiles = attachmentDao.getAllFileNames()
        val newReceiptFiles = mutableListOf<String>()
        // P5: tempDir cleanup is in finally so unencrypted CSVs are always removed, even on transaction failure.
        try {
            database.withTransaction {
                transactionLinkDao.clearAll(); transactionDao.clearAllCrossRefs()
                loanPaymentDao.clearAll(); plannedPaymentOccurrenceDao.clearAll()
                transactionDao.clearAll(); templateDao.clearAll(); budgetDao.clearAll()
                loanDao.clearAll(); plannedPaymentDao.clearAll(); accountDao.clearAll(); filterDao.clearAll()
                goalDao.clearAll(); attachmentDao.clearAll()
                tagDao.clearAll(); categoryDao.clearAll(); profileDao.clearAll()

                restoreProfiles(File(tempDir, "profiles.csv"))
                restoreAccounts(File(tempDir, "accounts.csv"))
                restoreCategories(File(tempDir, "categories.csv"))
                restoreTags(File(tempDir, "tags.csv"))
                restoreFilters(File(tempDir, "filters.csv"))
                restoreTemplates(File(tempDir, "templates.csv"))
                restoreTransactions(File(tempDir, "transactions.csv"))
                restoreLinks(File(tempDir, "transaction_links.csv"))
                restoreAttachments(File(tempDir, "attachments.csv"), File(tempDir, "attachments"), newReceiptFiles)
                restoreBudgets(File(tempDir, "budgets.csv"))
                restoreLoans(File(tempDir, "loans.csv"))
                restoreLoanPayments(File(tempDir, "loan_payments.csv"))
                restoreGoals(File(tempDir, "goals.csv"))
                restorePlannedPayments(File(tempDir, "planned_payments.csv"))
                restorePlannedPaymentOccurrences(File(tempDir, "planned_payment_occurrences.csv"))
                restoreCurrencyRates(File(tempDir, "currency_rates.csv"))
            }
            oldReceiptFiles.forEach(attachmentStore::delete)
        } catch (e: Exception) {
            newReceiptFiles.forEach(attachmentStore::delete)
            throw e
        } finally {
            tempDir.deleteRecursively()
        }
    }

    private suspend fun parseCsv(file: File, action: suspend (Map<String, String>) -> Unit) {
        if (!file.exists()) return
        val settings = CsvParserSettings()
        settings.format.delimiter = ','
        settings.isHeaderExtractionEnabled = true
        settings.ignoreLeadingWhitespaces = true
        settings.ignoreTrailingWhitespaces = true
        val parser = CsvParser(settings)
        withContext(Dispatchers.IO) {
            file.reader(Charsets.UTF_8).use { reader ->
                val headers = mutableListOf<String>()
                val iterator = parser.iterateRecords(reader).iterator()
                if (iterator.hasNext()) {
                    headers.addAll(parser.getContext().headers()?.map { it.trim() } ?: emptyList())
                    while (iterator.hasNext()) {
                        val record = iterator.next()
                        action(headers.zip(record.values).toMap())
                    }
                }
            }
        }
    }

    private suspend fun restoreProfiles(file: File) = parseCsv(file) { p ->
        profileDao.insertProfile(
            ProfileEntity(
                p["id"]?.toIntOrNull() ?: 0,
                p["name"] ?: "",
                p["baseCurrency"] ?: "USD",
                p["type"] ?: "PERSONAL"
            )
        )
    }

    private suspend fun restoreAccounts(file: File) = parseCsv(file) { p ->
        accountDao.upsertAccount(
            AccountEntity(
                p["id"] ?: "",
                p["name"] ?: "",
                p["currencyCode"] ?: "USD",
                p["structureType"] ?: "regular",
                p["colorHex"] ?: "#000000",
                p["icon"] ?: "",
                p["allowedCurrencies"] ?: "USD",
                p["profileId"]?.toIntOrNull() ?: 1,
                p["isArchived"]?.toBoolean() ?: false,
                p["displayOrder"]?.toIntOrNull() ?: 0
            )
        )
    }

    private suspend fun restoreCategories(file: File) = parseCsv(file) { p ->
        categoryDao.insertCategory(
            CategoryEntity(
                p["id"]?.toLongOrNull() ?: 0,
                p["name"] ?: "",
                p["iconName"] ?: p["icon"] ?: "",
                p["colorHex"]?.toIntOrNull() ?: 0,
                p["isDefault"]?.toBoolean() ?: false,
                p["parentId"]?.toLongOrNull(),
                p["nature"]?.let { n -> runCatching { CategoryNature.valueOf(n) }.getOrNull() }
                    ?: CategoryNature.WANT))
    }

    private suspend fun restoreTransactions(file: File) {
        val batch = mutableListOf<TransactionEntity>()
        parseCsv(file) { p ->
            batch.add(
                TransactionEntity(
                    id = p["id"]?.toLongOrNull() ?: 0,
                    name = p["name"] ?: "",
                    amount = p["amount"]?.toBigDecimalOrNull() ?: BigDecimal.ZERO,
                    timestamp = p["timestamp"]?.toLongOrNull() ?: 0,
                    currency = p["currency"] ?: "USD",
                    transactionType = TransactionType.fromValue(p["transactionType"]),
                    accountId = p["accountId"],
                    categoryId = p["categoryId"]?.toLongOrNull() ?: 1002,
                    tags = parseTagNames(p["tags"]),
                    paymentMethod = p["paymentMethod"]?.let { m -> PaymentMethod.fromString(m) }
                        ?: PaymentMethod.CASH,
                    payee = p["payee"] ?: "",
                    note = p["note"] ?: "",
                    sourcePlannedPaymentId = p["sourcePlannedPaymentId"]?.toLongOrNull(),
                    profileId = p["profileId"]?.toIntOrNull() ?: 1,
                    gpsLocation = p["gpsLocation"],
                    location = p["location"],
                    warrantyExpiration = p["warrantyExpiration"]?.toLongOrNull(),
                    status = TransactionStatus.fromString(p["status"] ?: "CLEARED"),
                    targetAccountId = p["targetAccountId"],
                    targetCurrency = p["targetCurrency"],
                    transferRate = p["transferRate"]?.toBigDecimalOrNull() ?: BigDecimal.ONE
                ))
            if (batch.size >= 100) {
                transactionDao.insertTransactions(batch.toList()); batch.clear()
            }
        }
        if (batch.isNotEmpty()) transactionDao.insertTransactions(batch)
    }

    private suspend fun restoreLinks(file: File) {
        val batch = mutableListOf<TransactionLinkEntity>()
        parseCsv(file) { p ->
            batch.add(
                TransactionLinkEntity(
                    p["id"]?.toLongOrNull() ?: 0,
                    p["transactionId"]?.toLongOrNull() ?: 0,
                    p["linkedTransactionId"]?.toLongOrNull() ?: 0
                )
            )
            if (batch.size >= 100) {
                transactionLinkDao.insertLinks(batch.toList()); batch.clear()
            }
        }
        if (batch.isNotEmpty()) transactionLinkDao.insertLinks(batch)
    }

    private suspend fun restoreTemplates(file: File) = parseCsv(file) { p ->
        templateDao.insertTemplate(
            TemplateEntity(
                id = p["id"]?.toLongOrNull() ?: 0,
                name = p["name"] ?: "",
                categoryId = p["categoryId"]?.toLongOrNull() ?: 1002,
                amount = p["amount"]?.toBigDecimalOrNull() ?: BigDecimal.ZERO,
                entryMode = EntryMode.valueOf(p["entryMode"] ?: "EXPENSE"),
                icon = p["icon"],
                colorHex = p["colorHex"],
                accountId = p["accountId"],
                targetAccountId = p["targetAccountId"],
                targetCurrency = p["targetCurrency"],
                transferRate = p["transferRate"]?.toBigDecimalOrNull() ?: BigDecimal.ONE,
                note = p["note"],
                payee = p["payee"],
                location = p["location"],
                gpsLocation = p["gpsLocation"],
                warrantyExpiration = p["warrantyExpiration"]?.toLongOrNull(),
                transactionStatus = TransactionStatus.fromString(
                    p["transactionStatus"] ?: "CLEARED"
                ),
                paymentMethod = p["paymentMethod"]?.let { m -> PaymentMethod.fromString(m) },
                currency = p["currency"] ?: "USD",
                tags = parseTagNames(p["tags"]),
                isStarred = p["isStarred"]?.toBoolean() ?: false,
                displayOrder = p["displayOrder"]?.toIntOrNull() ?: 0,
                profileId = p["profileId"]?.toIntOrNull() ?: 1
            )
        )
    }

    private suspend fun restoreBudgets(file: File) = parseCsv(file) { p ->
        p["filterId"]?.toLongOrNull()?.let { fid ->
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
                    note = p["note"] ?: "",
                    filterId = fid
                )
            )
        }
    }

    private suspend fun restorePlannedPayments(file: File) = parseCsv(file) { p ->
        p["templateId"]?.toLongOrNull()?.let { tid ->
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
                    status = PlannedPaymentStatus.fromString(p["status"] ?: "ACTIVE"),
                    profileId = p["profileId"]?.toIntOrNull() ?: 1,
                    templateId = tid
                )
            )
        }
    }

    private suspend fun restorePlannedPaymentOccurrences(file: File) {
        val batch = mutableListOf<PlannedPaymentOccurrenceEntity>()
        parseCsv(file) { p ->
            batch.add(
                PlannedPaymentOccurrenceEntity(
                    id = p["id"]?.toLongOrNull() ?: 0,
                    plannedPaymentId = p["plannedPaymentId"]?.toLongOrNull() ?: 0,
                    scheduledDate = p["scheduledDate"]?.toLongOrNull() ?: 0,
                    generatedTransactionId = p["generatedTransactionId"]?.toLongOrNull(),
                    status = OccurrenceStatus.fromString(p["status"] ?: "PENDING")
                )
            )
            if (batch.size >= 100) {
                plannedPaymentOccurrenceDao.insertOccurrences(batch.toList()); batch.clear()
            }
        }
        if (batch.isNotEmpty()) plannedPaymentOccurrenceDao.insertOccurrences(batch)
    }

    /** Attachments travel inside the zip as decrypted files (`attachments/<id>.bin`); the zip itself is what gets encrypted. */
    private suspend fun writeAttachmentsToZip(zos: ZipOutputStream, scope: BackupScope, currentProfileId: Int) {
        val all = attachmentDao.getAllForBackup()
        val rows = if (scope == BackupScope.SINGLE_PROFILE) all.filter { it.profileId == currentProfileId } else all
        val written = mutableListOf<AttachmentEntity>()
        rows.forEach { row ->
            val bytes = attachmentStore.read(row.fileName) ?: return@forEach // unreadable: skip, don't fail the backup
            zos.putNextEntry(java.util.zip.ZipEntry("attachments/${row.id}.bin"))
            zos.write(bytes)
            zos.closeEntry()
            written += row
        }
        writeCsvToZip(
            zos, "attachments.csv",
            arrayOf("id", "transactionId", "profileId", "mimeType", "sizeBytes", "createdAt", "displayName"),
            written.map { arrayOf(it.id, it.transactionId, it.profileId, it.mimeType, it.sizeBytes, it.createdAt, it.displayName) }
        )
    }

    /** Stores each attachment under a fresh random name; [created] collects them for rollback. */
    private suspend fun restoreAttachments(csv: File, imagesDir: File, created: MutableList<String>) {
        parseCsv(csv) { p ->
            val id = p["id"]?.toLongOrNull() ?: return@parseCsv
            // Backups made before attachments supported other file types stored images as ".jpg".
            val image = File(imagesDir, "$id.bin").takeIf { it.isFile } ?: File(imagesDir, "$id.jpg")
            if (!image.isFile) return@parseCsv
            val bytes = image.readBytes()
            val name = attachmentStore.write(bytes)
            created += name
            attachmentDao.insert(
                AttachmentEntity(
                    id = id,
                    transactionId = p["transactionId"]?.toLongOrNull() ?: return@parseCsv,
                    profileId = p["profileId"]?.toIntOrNull() ?: 1,
                    fileName = name,
                    mimeType = p["mimeType"] ?: "image/jpeg",
                    sizeBytes = bytes.size.toLong(),
                    createdAt = p["createdAt"]?.toLongOrNull() ?: System.currentTimeMillis(),
                    displayName = p["displayName"].orEmpty()
                )
            )
        }
    }

    private suspend fun restoreGoals(file: File) = parseCsv(file) { p ->
        goalDao.insert(
            GoalEntity(
                id = p["id"]?.toLongOrNull() ?: 0,
                name = p["name"] ?: "",
                targetAmount = p["targetAmount"]?.toBigDecimalOrNull() ?: BigDecimal.ZERO,
                savedAmount = p["savedAmount"]?.toBigDecimalOrNull() ?: BigDecimal.ZERO,
                currency = p["currency"] ?: "USD",
                deadline = p["deadline"]?.toLongOrNull(),
                note = p["note"] ?: "",
                profileId = p["profileId"]?.toIntOrNull() ?: 1,
                createdAt = p["createdAt"]?.toLongOrNull() ?: System.currentTimeMillis(),
                completedAt = p["completedAt"]?.toLongOrNull()
            )
        )
    }

    private suspend fun restoreTags(file: File) = parseCsv(file) { p ->
        tagDao.insertTag(
            TagEntity(
                id = p["id"]?.toLongOrNull() ?: 0,
                name = p["name"] ?: "",
                colorHex = p["colorHex"]?.toIntOrNull() ?: 0
            )
        )
    }

    private fun parseTagNames(s: String?): Set<String> {
        if (s.isNullOrBlank()) return emptySet()
        if (s.startsWith("[")) {
            try {
                return json.decodeFromString<Set<String>>(s)
            } catch (e: Exception) {
                // fall through to legacy split
            }
        }
        return s.trim().removePrefix("[").removeSuffix("]")
            .split("|", ";", ",").map { it.trim().removePrefix("\"").removeSuffix("\"").trim() }
            .filter { it.isNotBlank() }.toSet()
    }

    private suspend fun restoreLoans(file: File) = parseCsv(file) { p ->
        p["filterId"]?.toLongOrNull()?.let { fid ->
            loanDao.insert(
                LoanEntity(
                    id = p["id"]?.toLongOrNull() ?: 0,
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
                    note = p["note"] ?: "",
                    filterId = fid
                )
            )
        }
    }

    private suspend fun restoreLoanPayments(file: File) {
        val batch = mutableListOf<LoanPaymentEntity>()
        parseCsv(file) { p ->
            batch.add(
                LoanPaymentEntity(
                    id = p["id"]?.toLongOrNull() ?: 0,
                    loanId = p["loanId"]?.toLongOrNull() ?: 0,
                    transactionId = p["transactionId"]?.toLongOrNull(),
                    amount = p["amount"]?.toBigDecimalOrNull() ?: BigDecimal.ZERO,
                    date = p["date"]?.toLongOrNull() ?: 0,
                    note = p["note"] ?: ""
                )
            )
            if (batch.size >= 100) {
                loanPaymentDao.insertPayments(batch.toList()); batch.clear()
            }
        }
        if (batch.isNotEmpty()) loanPaymentDao.insertPayments(batch)
    }

    private suspend fun restoreFilters(file: File) = parseCsv(file) { p ->
        filterDao.insertFilter(
            FilterEntity(
                id = p["id"]?.toLongOrNull() ?: 0,
                name = p["name"] ?: "",
                timelineRange = p["timelineRange"]?.let {
                    runCatching {
                        json.decodeFromString<TimelineRange>(
                            it
                        )
                    }.getOrDefault(TimelineRange.AllTime())
                } ?: TimelineRange.AllTime(),
                queryNote = p["queryNote"],
                isNoteRegex = p["isNoteRegex"]?.toBoolean() ?: false,
                queryPayee = p["queryPayee"],
                isPayeeRegex = p["isPayeeRegex"]?.toBoolean() ?: false,
                categoryIds = p["categoryIds"]?.let {
                    runCatching {
                        json.decodeFromString<Set<Long>>(
                            it
                        )
                    }.getOrDefault(emptySet())
                } ?: emptySet(),
                accountIds = p["accountIds"]?.let {
                    runCatching {
                        json.decodeFromString<Set<String>>(
                            it
                        )
                    }.getOrDefault(emptySet())
                } ?: emptySet(),
                startDate = p["startDate"]?.toLongOrNull(),
                endDate = p["endDate"]?.toLongOrNull(),
                paymentMethods = p["paymentMethods"]?.let {
                    runCatching {
                        json.decodeFromString<Set<PaymentMethod>>(
                            it
                        )
                    }.getOrDefault(emptySet())
                } ?: emptySet(),
                hasWarranty = p["hasWarranty"]?.toBoolean(),
                warrantyExpirationStart = p["warrantyExpirationStart"]?.toLongOrNull(),
                warrantyExpirationEnd = p["warrantyExpirationEnd"]?.toLongOrNull(),
                minAmount = p["minAmount"]?.toBigDecimalOrNull(),
                maxAmount = p["maxAmount"]?.toBigDecimalOrNull(),
                transactionTypes = p["transactionTypes"]?.let {
                    runCatching {
                        json.decodeFromString<Set<TransactionType>>(
                            it
                        )
                    }.getOrDefault(emptySet())
                } ?: emptySet(),
                currencies = p["currencies"]?.let {
                    runCatching {
                        json.decodeFromString<Set<String>>(
                            it
                        )
                    }.getOrDefault(emptySet())
                } ?: emptySet(),
                statuses = p["statuses"]?.let {
                    runCatching {
                        json.decodeFromString<Set<TransactionStatus>>(
                            it
                        )
                    }.getOrDefault(emptySet())
                } ?: emptySet(),
                linkedTransactionIds = p["linkedTransactionIds"]?.let {
                    runCatching {
                        json.decodeFromString<Set<Long>>(
                            it
                        )
                    }.getOrDefault(emptySet())
                } ?: emptySet(),
                tags = p["tags"]?.let {
                    runCatching { json.decodeFromString<Set<String>>(it) }.getOrDefault(
                        emptySet()
                    )
                } ?: emptySet()
            ))
    }

    private suspend fun restoreCurrencyRates(file: File) = with(currencyDao) {
        clearAll()
        val rates = mutableListOf<CurrencyRateEntity>()
        parseCsv(file) { p ->
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
                insertRates(rates.toList()); rates.clear()
            }
        }
        if (rates.isNotEmpty()) insertRates(rates)
    }

    override suspend fun importDataSelective(
        uriString: String,
        type: BackupType,
        entities: Set<RestorableEntity>,
        passphrase: CharArray?
    ): Result<Unit> = withContext(dispatcherProvider.io) {
        try {
            withPlainInput(uriString, passphrase) { inputStream ->
                if (type == BackupType.CSV_ZIP) restoreZipBackupSelective(inputStream, entities)
                else throw UnsupportedOperationException("Selective restore only for CSV_ZIP")
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private suspend fun restoreZipBackupSelective(
        inputStream: InputStream,
        entities: Set<RestorableEntity>
    ) {
        val safe = entities.toMutableSet()
        if (RestorableEntity.TRANSACTIONS in safe) {
            safe.add(RestorableEntity.ACCOUNTS); safe.add(RestorableEntity.CATEGORIES); safe.add(
                RestorableEntity.TAGS
            )
        }
        if (RestorableEntity.BUDGETS in safe || RestorableEntity.LOANS in safe) safe.add(
            RestorableEntity.FILTERS
        )
        if (RestorableEntity.PLANNED_PAYMENTS in safe) safe.add(RestorableEntity.TEMPLATES)

        val tempDir =
            File(context.cacheDir, "restore_temp_sel").apply { deleteRecursively(); mkdirs() }
        val canonicalTempDir = tempDir.canonicalPath + File.separator
        val extractionCounter = longArrayOf(0L)
        ZipInputStream(inputStream).use { zis ->
            var entry = zis.nextEntry
            while (entry != null) {
                val f = File(tempDir, entry.name)
                if (!f.canonicalPath.startsWith(canonicalTempDir)) throw SecurityException("Zip Slip!")
                val should = safe.any { e ->
                    when (e) {
                        RestorableEntity.PROFILES -> entry.name == "profiles.csv"
                        RestorableEntity.ACCOUNTS -> entry.name == "accounts.csv"
                        RestorableEntity.CATEGORIES -> entry.name == "categories.csv"
                        RestorableEntity.TRANSACTIONS -> entry.name == "transactions.csv" || entry.name == "transaction_links.csv" ||
                            entry.name == "attachments.csv" || entry.name.startsWith("attachments/")
                        RestorableEntity.BUDGETS -> entry.name == "budgets.csv"
                        RestorableEntity.LOANS -> entry.name == "loans.csv" || entry.name == "loan_payments.csv"
                        RestorableEntity.PLANNED_PAYMENTS -> entry.name == "planned_payments.csv" || entry.name == "planned_payment_occurrences.csv"
                        RestorableEntity.TEMPLATES -> entry.name == "templates.csv"
                        RestorableEntity.FILTERS -> entry.name == "filters.csv"
                        RestorableEntity.TAGS -> entry.name == "tags.csv"
                        RestorableEntity.CURRENCY_RATES -> entry.name == "currency_rates.csv"
                        RestorableEntity.GOALS -> entry.name == "goals.csv"
                    }
                }
                if (should && !entry.isDirectory) {
                    f.parentFile?.mkdirs(); FileOutputStream(f).use {
                        zis.copyToLimited(
                            it,
                            extractionCounter
                        )
                    }
                }
                entry = zis.nextEntry
            }
        }

        val oldReceiptFiles = attachmentDao.getAllFileNames()
        val newReceiptFiles = mutableListOf<String>()
        try {
            database.withTransaction {
                if (RestorableEntity.PROFILES in safe) File(
                    tempDir,
                    "profiles.csv"
                ).let {
                    if (it.exists()) {
                        profileDao.clearAll(); restoreProfiles(it)
                    }
                }
                if (RestorableEntity.ACCOUNTS in safe) File(
                    tempDir,
                    "accounts.csv"
                ).let { if (it.exists()) restoreAccounts(it) }
                if (RestorableEntity.CATEGORIES in safe) File(
                    tempDir,
                    "categories.csv"
                ).let { if (it.exists()) restoreCategories(it) }
                if (RestorableEntity.TAGS in safe) File(
                    tempDir,
                    "tags.csv"
                ).let { if (it.exists()) restoreTags(it) }
                if (RestorableEntity.FILTERS in safe) File(
                    tempDir,
                    "filters.csv"
                ).let {
                    if (it.exists()) {
                        filterDao.clearAll(); restoreFilters(it)
                    }
                }
                if (RestorableEntity.TEMPLATES in safe) File(
                    tempDir,
                    "templates.csv"
                ).let {
                    if (it.exists()) {
                        templateDao.clearAll(); restoreTemplates(it)
                    }
                }
                if (RestorableEntity.TRANSACTIONS in safe) File(
                    tempDir,
                    "transactions.csv"
                ).let {
                    if (it.exists()) {
                        transactionDao.clearAll(); transactionLinkDao.clearAll(); restoreTransactions(
                            it
                        ); restoreLinks(
                            File(tempDir, "transaction_links.csv")
                        )
                        // Receipts belong to their transactions, so they are replaced together.
                        attachmentDao.clearAll()
                        restoreAttachments(File(tempDir, "attachments.csv"), File(tempDir, "attachments"), newReceiptFiles)
                    }
                }
                if (RestorableEntity.BUDGETS in safe) File(
                    tempDir,
                    "budgets.csv"
                ).let {
                    if (it.exists()) {
                        budgetDao.clearAll(); restoreBudgets(it)
                    }
                }
                if (RestorableEntity.LOANS in safe) File(
                    tempDir,
                    "loans.csv"
                ).let {
                    if (it.exists()) {
                        loanDao.clearAll(); loanPaymentDao.clearAll(); restoreLoans(it); restoreLoanPayments(
                            File(tempDir, "loan_payments.csv")
                        )
                    }
                }
                if (RestorableEntity.PLANNED_PAYMENTS in safe) File(
                    tempDir,
                    "planned_payments.csv"
                ).let {
                    if (it.exists()) {
                        plannedPaymentDao.clearAll(); plannedPaymentOccurrenceDao.clearAll(); restorePlannedPayments(
                            it
                        ); restorePlannedPaymentOccurrences(
                            File(
                                tempDir,
                                "planned_payment_occurrences.csv"
                            )
                        )
                    }
                }
                if (RestorableEntity.GOALS in safe) File(
                    tempDir,
                    "goals.csv"
                ).let {
                    if (it.exists()) {
                        goalDao.clearAll(); restoreGoals(it)
                    }
                }
                if (RestorableEntity.CURRENCY_RATES in safe) File(
                    tempDir,
                    "currency_rates.csv"
                ).let { if (it.exists()) restoreCurrencyRates(it) }
            }
            if (RestorableEntity.TRANSACTIONS in safe && File(tempDir, "transactions.csv").exists()) {
                oldReceiptFiles.forEach(attachmentStore::delete)
            }
        } catch (e: Exception) {
            newReceiptFiles.forEach(attachmentStore::delete)
            throw e
        } finally {
            tempDir.deleteRecursively()
        }
    }

    override suspend fun exportToPrivateStorage(scope: BackupScope): Result<String> =
        withContext(dispatcherProvider.io) {
            try {
                val profileId = settingsDataStore.currentProfileId.first()
                val dir = File(context.filesDir, "backups").apply { mkdirs() }
                val encrypt = settingsDataStore.autoBackupConfig.first().encrypt
                // Never fall back to a plaintext file when encryption was asked for.
                val vaultKey = if (encrypt) {
                    backupKeyVault.load() ?: throw IllegalStateException(
                        "Backup encryption is on but its key is unavailable. " +
                            "Re-enter the backup passphrase in Data Management."
                    )
                } else null
                val file = File(
                    dir,
                    "osswallet_backup_${System.currentTimeMillis()}${if (scope == BackupScope.SINGLE_PROFILE) "_profile" else "_all"}.zip" +
                        if (encrypt) ENCRYPTED_SUFFIX else ""
                )
                try {
                    FileOutputStream(file).use { raw ->
                        val out = if (vaultKey != null)
                            BackupCrypto.encrypt(raw, vaultKey.key, vaultKey.salt, vaultKey.iterations)
                        else raw
                        out.use { createZipBackup(it, scope, profileId) }
                    }
                } catch (e: Exception) {
                    file.delete() // no half-written backup left in the retention list
                    throw e
                }
                Result.success(file.absolutePath)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }

    override suspend fun listPrivateBackups(): List<BackupMeta> =
        withContext(dispatcherProvider.io) {
            File(context.filesDir, "backups").takeIf { it.exists() }
                ?.listFiles { f -> f.name.endsWith(".zip") || f.name.endsWith(".sqlite") || f.name.endsWith(ENCRYPTED_SUFFIX) }
                ?.sortedByDescending { it.lastModified() }
                ?.map { f ->
                    BackupMeta(
                        f.name,
                        f.absolutePath,
                        f.lastModified(),
                        f.length(),
                        if (f.name.contains("_profile")) BackupScope.SINGLE_PROFILE else BackupScope.ALL_PROFILES,
                        encrypted = f.name.endsWith(ENCRYPTED_SUFFIX)
                    )
                } ?: emptyList()
        }

    override suspend fun deleteBackup(id: String): Result<Unit> =
        withContext(dispatcherProvider.io) {
            try {
                File(
                    context.filesDir,
                    "backups/$id"
                ).let { if (it.exists()) it.delete() }; Result.success(Unit)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }

    override suspend fun isEncryptedBackup(uriString: String): Boolean =
        withContext(dispatcherProvider.io) {
            runCatching {
                context.contentResolver.openInputStream(uriString.toUri())?.use { input ->
                    val head = ByteArray(BackupCrypto.MAGIC_LENGTH)
                    var n = 0
                    while (n < head.size) {
                        val r = input.read(head, n, head.size - n)
                        if (r < 0) break
                        n += r
                    }
                    n == head.size && BackupCrypto.isEncrypted(head)
                } ?: false
            }.getOrDefault(false)
        }

    override suspend fun verifyBackup(
        uriString: String,
        type: BackupType,
        passphrase: CharArray?
    ): Result<BackupVerification> = withContext(dispatcherProvider.io) {
        try {
            val encrypted = isEncryptedBackup(uriString)
            val result = withPlainInput(uriString, passphrase) { input ->
                var entries = 0
                var bytes = 0L
                when (type) {
                    BackupType.CSV_ZIP -> {
                        var sawProfiles = false
                        val counter = longArrayOf(0L)
                        ZipInputStream(input).use { zis ->
                            var e = zis.nextEntry
                            while (e != null) {
                                entries++
                                if (e.name == "profiles.csv") sawProfiles = true
                                // Reading each entry through also surfaces zip-level corruption.
                                zis.copyToLimited(DiscardOutputStream, counter)
                                e = zis.nextEntry
                            }
                        }
                        bytes = counter[0]
                        if (entries == 0 || !sawProfiles) throw IOException("Not a valid OssWallet backup")
                    }
                    else -> {
                        val counter = longArrayOf(0L)
                        input.copyToLimited(DiscardOutputStream, counter)
                        bytes = counter[0]
                        entries = 1
                        if (bytes == 0L) throw IOException("Backup is empty")
                    }
                }
                BackupVerification(encrypted = encrypted, entryCount = entries, sizeBytes = bytes)
            }
            Result.success(result)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun enableBackupEncryption(passphrase: CharArray): Result<Unit> =
        withContext(dispatcherProvider.io) {
            try {
                val salt = BackupCrypto.newSalt()
                val iterations = BackupCrypto.DEFAULT_ITERATIONS
                backupKeyVault.store(BackupCrypto.deriveKey(passphrase, salt, iterations), salt, iterations)
                Result.success(Unit)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }

    override suspend fun disableBackupEncryption() = backupKeyVault.clear()

    override suspend fun hasBackupEncryptionKey(): Boolean = backupKeyVault.load() != null

    /**
     * Hands [block] a plaintext stream of the backup at [uriString]. Encrypted files are fully
     * decrypted and authenticated into a temp file first, so a wrong passphrase or tampering fails
     * before any restore starts and never leaves a half-applied import.
     */
    private suspend fun <T> withPlainInput(
        uriString: String,
        passphrase: CharArray?,
        block: suspend (InputStream) -> T
    ): T {
        val raw = context.contentResolver.openInputStream(uriString.toUri())
            ?: throw IOException("Cannot open backup")
        return raw.use { rawStream ->
            val input = BufferedInputStream(rawStream)
            input.mark(BackupCrypto.MAGIC_LENGTH)
            val head = ByteArray(BackupCrypto.MAGIC_LENGTH)
            val read = input.read(head)
            input.reset()
            if (read == head.size && BackupCrypto.isEncrypted(head)) {
                if (passphrase == null) throw BackupPassphraseRequiredException()
                val temp = File.createTempFile("restore_dec_", ".tmp", context.cacheDir)
                try {
                    FileOutputStream(temp).use { BackupCrypto.decrypt(input, it, passphrase) }
                    FileInputStream(temp).use { block(it) }
                } finally {
                    temp.delete()
                }
            } else {
                block(input)
            }
        }
    }

    // P4: shared counter so cumulative extraction across all zip entries is bounded, not per-entry.
    private fun InputStream.copyToLimited(out: OutputStream, totalBytesWritten: LongArray) {
        val buffer = ByteArray(8192)
        var n: Int
        while (read(buffer).also { n = it } != -1) {
            out.write(buffer, 0, n)
            totalBytesWritten[0] += n
            if (totalBytesWritten[0] > MAX_RESTORE_BYTES)
                throw IOException("Restore aborted: extracted size exceeds limit (${MAX_RESTORE_BYTES / 1024 / 1024} MB)")
        }
    }

    override suspend fun pruneOldBackups(maxRetained: Int) {
        withContext(dispatcherProvider.io) {
            val backups = listPrivateBackups()
            if (backups.size > maxRetained) backups.drop(maxRetained)
                .forEach { deleteBackup(it.id) }
        }
    }
}
