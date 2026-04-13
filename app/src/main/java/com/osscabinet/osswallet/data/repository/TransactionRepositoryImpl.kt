package com.osscabinet.ossWallet.data.repository

import com.osscabinet.ossWallet.data.local.dao.AccountDao
import com.osscabinet.ossWallet.data.local.dao.TransactionDao
import com.osscabinet.ossWallet.data.local.entity.TransactionEntity
import com.osscabinet.ossWallet.domain.model.Transaction
import com.osscabinet.ossWallet.domain.repository.TransactionRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class TransactionRepositoryImpl(
    private val transactionDao: TransactionDao,
    private val accountDao: AccountDao
) : TransactionRepository {
    override fun getAllTransactions(profileId: Int): Flow<List<Transaction>> {
        return transactionDao.getAllTransactions(profileId).map { entities ->
            entities.map { it.toDomain() }
        }
    }

    override suspend fun insertTransaction(transaction: Transaction, profileId: Int) {
        transactionDao.insertTransactionWithBalanceUpdate(transaction.toEntity(profileId), accountDao)
    }

    override suspend fun deleteTransaction(id: Long) {
        transactionDao.getTransactionById(id)?.let {
            transactionDao.deleteTransactionWithBalanceUpdate(it, accountDao)
        }
    }

    override suspend fun updateTransaction(oldTransaction: Transaction, newTransaction: Transaction, profileId: Int) {
        transactionDao.updateTransactionWithBalanceUpdate(
            oldTransaction.toEntity(profileId),
            newTransaction.toEntity(profileId),
            accountDao
        )
    }

    override suspend fun getTransactionsByAccount(accountId: String): Flow<List<Transaction>> {
        return transactionDao.getTransactionsByAccount(accountId).map { entities ->
            entities.map { it.toDomain() }
        }
    }

    override suspend fun getTransactionById(id: Long): Transaction? {
        return transactionDao.getTransactionById(id)?.toDomain()
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
            tags = tags.split(",").filter { it.isNotBlank() },
            paymentMethod = paymentMethod
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
            profileId = profileId,
            paymentMethod = paymentMethod
        )
    }
}
