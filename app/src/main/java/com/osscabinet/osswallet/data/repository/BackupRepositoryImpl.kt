package com.osscabinet.ossWallet.data.repository

import android.content.ContentResolver
import android.net.Uri
import com.osscabinet.ossWallet.data.local.*
import com.osscabinet.ossWallet.data.local.dao.*
import com.osscabinet.ossWallet.data.local.entity.*
import com.osscabinet.ossWallet.domain.repository.BackupRepository
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.BufferedReader
import java.io.InputStreamReader

class BackupRepositoryImpl(
    private val contentResolver: ContentResolver,
    private val profileDao: ProfileDao,
    private val accountDao: AccountDao,
    private val transactionDao: TransactionDao,
    private val categoryDao: CategoryDao,
    private val tagDao: TagDao,
    private val templateDao: TemplateDao
) : BackupRepository {

    private val json = Json { ignoreUnknownKeys = true; prettyPrint = true }

    override suspend fun exportData(uri: Uri): Result<Unit> = runCatching {
        val backup = OssWalletBackup(
            profiles = profileDao.getAllProfilesSync().map { ProfileBackup(it.id, it.name, it.type) },
            accounts = accountDao.getAllAccountsSync().map { 
                AccountBackup(
                    id = it.id,
                    name = it.name,
                    balance = it.balance,
                    currencyCode = it.currencyCode,
                    structureType = it.structureType,
                    colorHex = it.colorHex,
                    icon = it.icon,
                    allowedCurrencies = it.allowedCurrencies,
                    profileId = it.profileId
                ) 
            },
            transactions = transactionDao.getAllTransactionsSync().map { 
                TransactionBackup(it.id, it.title, it.amount, it.category, it.timestamp, it.currency, it.isIncome, it.accountId, it.categoryId, it.tags, it.paymentMethod, it.profileId)
            },
            categories = categoryDao.getAllCategoriesSync().map { 
                CategoryBackup(it.id, it.name, it.iconName, it.colorHex, it.isDefault)
            },
            tags = tagDao.getAllTagsSync().map { TagBackup(it.id, it.name, it.colorHex) },
            templates = templateDao.getAllTemplatesSync().map {
                TemplateBackup(
                    id = it.id,
                    name = it.name,
                    category = it.category,
                    amount = it.amount,
                    transactionType = it.transactionType,
                    accountId = it.accountId,
                    note = it.note,
                    payee = it.payee,
                    location = it.location,
                    paymentMethod = it.paymentMethod,
                    currency = it.currency,
                    tags = it.tags,
                    isStarred = it.isStarred,
                    displayOrder = it.displayOrder
                )
            }
        )

        val jsonString = json.encodeToString(backup)
        contentResolver.openOutputStream(uri)?.use { outputStream ->
            outputStream.write(jsonString.toByteArray())
        }
    }

    override suspend fun importData(uri: Uri): Result<Unit> = runCatching {
        val jsonString = contentResolver.openInputStream(uri)?.use { inputStream ->
            BufferedReader(InputStreamReader(inputStream)).readText()
        } ?: throw Exception("Failed to open input stream")

        val backup = json.decodeFromString<OssWalletBackup>(jsonString)

        // Clear existing data
        profileDao.clearAll()
        accountDao.clearAll()
        transactionDao.clearAll()
        categoryDao.clearAll()
        tagDao.clearAll()
        templateDao.clearAll()

        // Restore data
        backup.profiles.forEach { profileDao.insertProfile(ProfileEntity(it.id, it.name, it.type)) }
        backup.accounts.forEach { 
            accountDao.upsertAccount(
                AccountEntity(
                    id = it.id,
                    name = it.name,
                    balance = it.balance,
                    currencyCode = it.currencyCode,
                    structureType = it.structureType,
                    colorHex = it.colorHex,
                    icon = it.icon,
                    allowedCurrencies = it.allowedCurrencies,
                    profileId = it.profileId
                )
            )
        }
        backup.transactions.forEach { 
            transactionDao.insertTransaction(TransactionEntity(it.id, it.title, it.amount, it.category, it.timestamp, it.currency, it.isIncome, it.accountId, it.categoryId, it.tags, it.paymentMethod, it.profileId))
        }
        backup.categories.forEach { 
            categoryDao.insertCategory(CategoryEntity(it.id, it.name, it.iconName, it.colorHex, it.isDefault))
        }
        backup.tags.forEach { 
            tagDao.insertTag(TagEntity(it.id, it.name, it.colorHex))
        }
        backup.templates.forEach {
            templateDao.insertTemplate(
                TemplateEntity(
                    id = it.id,
                    name = it.name,
                    category = it.category,
                    amount = it.amount,
                    transactionType = it.transactionType,
                    accountId = it.accountId,
                    note = it.note,
                    payee = it.payee,
                    location = it.location,
                    paymentMethod = it.paymentMethod,
                    currency = it.currency,
                    tags = it.tags,
                    isStarred = it.isStarred,
                    displayOrder = it.displayOrder
                )
            )
        }
    }
}
