package com.osscabinet.ossWallet.data.repository

import com.osscabinet.ossWallet.data.local.dao.ImportBatchDao
import com.osscabinet.ossWallet.data.local.entity.ImportBatchEntity
import com.osscabinet.ossWallet.domain.repository.ImportBatchRepository
import jakarta.inject.Inject
import jakarta.inject.Singleton

@Singleton
class ImportBatchRepositoryImpl @Inject constructor(
    private val dao: ImportBatchDao
) : ImportBatchRepository {

    override suspend fun recordBatch(
        batchId: String,
        sourceApp: String,
        transactionCount: Int,
        createdAccountIds: List<String>,
        createdCategoryIds: List<Long>
    ) {
        dao.insertBatch(
            ImportBatchEntity(
                id = batchId,
                sourceApp = sourceApp,
                createdAt = System.currentTimeMillis(),
                transactionCount = transactionCount,
                createdAccountIds = createdAccountIds,
                createdCategoryIds = createdCategoryIds
            )
        )
    }

    override suspend fun undoImport(batchId: String): Boolean {
        val batch = dao.getBatchById(batchId) ?: return false

        dao.deleteTransactionsForBatch(batchId)

        batch.createdAccountIds.forEach { accountId ->
            if (dao.countTransactionsForAccount(accountId) == 0) {
                dao.deleteAccountById(accountId)
            }
        }
        batch.createdCategoryIds.forEach { categoryId ->
            if (dao.countTransactionsForCategory(categoryId) == 0) {
                dao.deleteCategoryById(categoryId)
            }
        }

        dao.deleteBatch(batchId)
        return true
    }
}
