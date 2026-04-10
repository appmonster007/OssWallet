package com.osscabinet.gleemanager.data.repository

import android.content.ContentResolver
import android.net.Uri
import com.osscabinet.gleemanager.data.local.*
import com.osscabinet.gleemanager.data.local.dao.*
import com.osscabinet.gleemanager.data.local.entity.*
import com.osscabinet.gleemanager.domain.repository.BackupRepository
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
    private val tagDao: TagDao
) : BackupRepository {

    private val json = Json { ignoreUnknownKeys = true; prettyPrint = true }

    override suspend fun exportData(uri: Uri): Result<Unit> = runCatching {
        val backup = GleeBackup(
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
                TransactionBackup(it.id, it.title, it.amount, it.category, it.timestamp, it.currency, it.isIncome, it.accountId, it.categoryId, it.tags, it.profileId)
            },
            categories = categoryDao.getAllCategoriesSync().map { 
                CategoryBackup(it.id, it.name, it.iconName, it.colorHex, it.isDefault)
            },
            tags = tagDao.getAllTagsSync().map { TagBackup(it.id, it.name, it.colorHex) }
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

        val backup = json.decodeFromString<GleeBackup>(jsonString)

        // Clear existing data
        profileDao.clearAll()
        accountDao.clearAll()
        transactionDao.clearAll()
        categoryDao.clearAll()
        tagDao.clearAll()

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
            transactionDao.insertTransaction(TransactionEntity(it.id, it.title, it.amount, it.category, it.timestamp, it.currency, it.isIncome, it.accountId, it.categoryId, it.tags, it.profileId))
        }
        backup.categories.forEach { 
            categoryDao.insertCategory(CategoryEntity(it.id, it.name, it.iconName, it.colorHex, it.isDefault))
        }
        backup.tags.forEach { 
            tagDao.insertTag(TagEntity(it.id, it.name, it.colorHex))
        }
    }
}
