package com.osscabinet.gleemanager.data.repository

import com.osscabinet.gleemanager.data.local.dao.TransactionDao
import com.osscabinet.gleemanager.data.local.entity.TransactionEntity
import com.osscabinet.gleemanager.domain.model.Transaction
import com.osscabinet.gleemanager.domain.repository.TransactionRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class TransactionRepositoryImpl(
    private val transactionDao: TransactionDao
) : TransactionRepository {
    override fun getAllTransactions(profileId: Int): Flow<List<Transaction>> {
        return transactionDao.getAllTransactions(profileId).map { entities ->
            entities.map { it.toDomain() }
        }
    }

    override suspend fun insertTransaction(transaction: Transaction, profileId: Int) {
        transactionDao.insertTransaction(transaction.toEntity(profileId))
    }

    override suspend fun deleteTransaction(id: Long) {
        transactionDao.getTransactionById(id)?.let {
            transactionDao.deleteTransaction(it)
        }
    }

    override suspend fun getTransactionsByAccount(accountId: String): Flow<List<Transaction>> {
        return transactionDao.getTransactionsByAccount(accountId).map { entities ->
            entities.map { it.toDomain() }
        }
    }

    private fun TransactionEntity.toDomain(): Transaction {
        return Transaction(
            id = id,
            title = title,
            amount = amount,
            category = category,
            timestamp = timestamp,
            currency = currency,
            isIncome = isIncome,
            accountId = accountId,
            categoryId = categoryId,
            tags = tags.split(",").filter { it.isNotBlank() }
        )
    }

    private fun Transaction.toEntity(profileId: Int): TransactionEntity {
        return TransactionEntity(
            id = id,
            title = title,
            amount = amount,
            category = category,
            timestamp = timestamp,
            currency = currency,
            isIncome = isIncome,
            accountId = accountId,
            categoryId = categoryId,
            tags = tags.joinToString(","),
            profileId = profileId
        )
    }
}
