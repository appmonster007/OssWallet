package com.osscabinet.ossWallet.data.repository

import androidx.sqlite.db.SimpleSQLiteQuery
import androidx.room.withTransaction
import com.osscabinet.ossWallet.data.local.AppDatabase
import com.osscabinet.ossWallet.data.local.dao.AccountDao
import com.osscabinet.ossWallet.data.local.dao.TransactionDao
import com.osscabinet.ossWallet.data.local.dao.TransactionLinkDao
import com.osscabinet.ossWallet.data.mapper.toDomain
import com.osscabinet.ossWallet.data.mapper.toEntity
import com.osscabinet.ossWallet.domain.model.Transaction
import com.osscabinet.ossWallet.domain.model.TransactionFilter
import com.osscabinet.ossWallet.domain.model.TransactionStatus
import com.osscabinet.ossWallet.domain.repository.TransactionRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class TransactionRepositoryImpl(
    private val transactionDao: TransactionDao,
    private val accountDao: AccountDao,
    private val transactionLinkDao: TransactionLinkDao,
    private val database: AppDatabase
) : TransactionRepository {

    override fun getAllTransactions(profileId: Int): Flow<List<Transaction>> {
        return transactionDao.getAllTransactions(profileId).map { entities ->
            entities.map { it.toDomain() }
        }
    }

    override suspend fun getAllTransactionsSync(profileId: Int): List<Transaction> {
        return transactionDao.getAllTransactionsSync(profileId).map { it.toDomain() }
    }

    override fun getFilteredTransactions(profileId: Int, filter: TransactionFilter): Flow<List<Transaction>> {
        val (query, args) = buildFilteredQuery(profileId, filter)
        val sqliteQuery = SimpleSQLiteQuery(query, args.toTypedArray())
        
        return transactionDao.getFilteredTransactions(sqliteQuery).map { entities ->
            applyRegexFilter(entities.map { it.toDomain() }, filter)
        }
    }

    override suspend fun getFilteredTransactionsSync(profileId: Int, filter: TransactionFilter): List<Transaction> {
        val (query, args) = buildFilteredQuery(profileId, filter)
        val sqliteQuery = SimpleSQLiteQuery(query, args.toTypedArray())

        return applyRegexFilter(transactionDao.getFilteredTransactionsSync(sqliteQuery).map { it.toDomain() }, filter)
    }

    private fun buildFilteredQuery(profileId: Int, filter: TransactionFilter): Pair<String, List<Any>> {
        val queryBuilder = StringBuilder("SELECT * FROM transactions WHERE profileId = ?")
        val args = mutableListOf<Any>(profileId)

        if (filter.startDate != null) {
            queryBuilder.append(" AND timestamp >= ?")
            args.add(filter.startDate)
        }
        if (filter.endDate != null) {
            queryBuilder.append(" AND timestamp <= ?")
            args.add(filter.endDate)
        }
        if (filter.accountIds.isNotEmpty()) {
            val placeholders = filter.accountIds.joinToString(",") { "?" }
            queryBuilder.append(" AND accountId IN ($placeholders)")
            args.addAll(filter.accountIds)
        }
        if (filter.categoryIds.isNotEmpty()) {
            val placeholders = filter.categoryIds.joinToString(",") { "?" }
            queryBuilder.append(" AND categoryId IN ($placeholders)")
            args.addAll(filter.categoryIds)
        }
        if (filter.transactionType != null) {
            queryBuilder.append(" AND transactionType = ?")
            args.add(filter.transactionType)
        }
        if (filter.minAmount != null) {
            queryBuilder.append(" AND amount >= ?")
            args.add(filter.minAmount)
        }
        if (filter.maxAmount != null) {
            queryBuilder.append(" AND amount <= ?")
            args.add(filter.maxAmount)
        }
        if (filter.paymentMethods.isNotEmpty()) {
            val placeholders = filter.paymentMethods.joinToString(",") { "?" }
            queryBuilder.append(" AND paymentMethod IN ($placeholders)")
            args.addAll(filter.paymentMethods)
        }
        if (filter.hasWarranty == true) {
            queryBuilder.append(" AND warrantyExpiration IS NOT NULL")
        } else if (filter.hasWarranty == false) {
            queryBuilder.append(" AND warrantyExpiration IS NULL")
        }
        if (filter.warrantyExpirationStart != null) {
            queryBuilder.append(" AND warrantyExpiration >= ?")
            args.add(filter.warrantyExpirationStart)
        }
        if (filter.warrantyExpirationEnd != null) {
            queryBuilder.append(" AND warrantyExpiration <= ?")
            args.add(filter.warrantyExpirationEnd)
        }
        if (filter.currencies.isNotEmpty()) {
            val placeholders = filter.currencies.joinToString(",") { "?" }
            queryBuilder.append(" AND currency IN ($placeholders)")
            args.addAll(filter.currencies)
        }
        if (!filter.queryNote.isNullOrBlank() && !filter.isNoteRegex) {
            // Use COLLATE NOCASE for case-insensitive matching (SQLite default)
            queryBuilder.append(" AND note LIKE ? COLLATE NOCASE")
            args.add("%${filter.queryNote}%")
        }
        if (!filter.queryPayee.isNullOrBlank() && !filter.isPayeeRegex) {
            // Use COLLATE NOCASE for case-insensitive matching (SQLite default)
            queryBuilder.append(" AND payee LIKE ? COLLATE NOCASE")
            args.add("%${filter.queryPayee}%")
        }
        if (filter.statuses.isNotEmpty()) {
            val placeholders = filter.statuses.joinToString(",") { "?" }
            queryBuilder.append(" AND status IN ($placeholders)")
            args.addAll(filter.statuses.map { it.name })
        }
        queryBuilder.append(" ORDER BY timestamp DESC")
        return Pair(queryBuilder.toString(), args)
    }

    private fun applyRegexFilter(domainTransactions: List<Transaction>, filter: TransactionFilter): List<Transaction> {
        return if (filter.isNoteRegex || filter.isPayeeRegex) {
            domainTransactions.filter { transaction ->
                val matchesNote = if (filter.isNoteRegex && !filter.queryNote.isNullOrBlank()) {
                    try {
                        Regex(filter.queryNote, RegexOption.IGNORE_CASE).containsMatchIn(transaction.note)
                    } catch (e: Exception) {
                        false
                    }
                } else true

                val matchesPayee = if (filter.isPayeeRegex && !filter.queryPayee.isNullOrBlank()) {
                    try {
                        Regex(filter.queryPayee, RegexOption.IGNORE_CASE).containsMatchIn(transaction.payee)
                    } catch (e: Exception) {
                        false
                    }
                } else true

                matchesNote && matchesPayee
            }
        } else {
            domainTransactions
        }
    }

    override suspend fun insertTransaction(transaction: Transaction, profileId: Int): Long {
        return transactionDao.insertTransactionWithBalanceUpdate(transaction.toEntity(profileId), accountDao)
    }

    override suspend fun saveTransfer(
        sourceTransaction: Transaction,
        targetTransaction: Transaction,
        profileId: Int
    ): Pair<Long, Long> {
        return database.withTransaction {
            val sourceId = transactionDao.insertTransactionWithBalanceUpdate(sourceTransaction.toEntity(profileId), accountDao)
            val targetId = transactionDao.insertTransactionWithBalanceUpdate(targetTransaction.toEntity(profileId), accountDao)

            // Create bi-directional links for transfers
            transactionLinkDao.insertLink(
                com.osscabinet.ossWallet.data.local.entity.TransactionLinkEntity(
                    transactionId = sourceId,
                    linkedTransactionId = targetId
                )
            )
            transactionLinkDao.insertLink(
                com.osscabinet.ossWallet.data.local.entity.TransactionLinkEntity(
                    transactionId = targetId,
                    linkedTransactionId = sourceId
                )
            )

            Pair(sourceId, targetId)
        }
    }

    override suspend fun updateTransaction(oldTransaction: Transaction, newTransaction: Transaction, profileId: Int) {
        transactionDao.updateTransactionWithBalanceUpdate(
            oldTransaction.toEntity(profileId),
            newTransaction.toEntity(profileId),
            accountDao
        )
    }

    override suspend fun deleteTransaction(id: Long) {
        val transaction = transactionDao.getTransactionById(id)
        transaction?.let {
            transactionDao.deleteTransactionWithBalanceUpdate(it, accountDao)
        }
    }

    override suspend fun getTransactionsByAccount(accountId: String): Flow<List<Transaction>> {
        return transactionDao.getTransactionsByAccount(accountId).map { entities ->
            entities.map { it.toDomain() }
        }
    }

    override suspend fun getTransactionById(id: Long): Transaction? {
        return transactionDao.getTransactionById(id)?.toDomain()
    }

    override suspend fun getCurrencySummaries(profileId: Int): List<com.osscabinet.ossWallet.data.local.dao.CurrencySummary> {
        return transactionDao.getCurrencySummaries(profileId)
    }

    override suspend fun getCurrencySummariesInRange(profileId: Int, start: Long, end: Long): List<com.osscabinet.ossWallet.data.local.dao.CurrencySummary> {
        return transactionDao.getCurrencySummariesInRange(profileId, start, end)
    }

    // Link management implementations
    override suspend fun addTransactionLink(transactionId: Long, linkedTransactionId: Long) {
        transactionLinkDao.insertLink(
            com.osscabinet.ossWallet.data.local.entity.TransactionLinkEntity(
                transactionId = transactionId,
                linkedTransactionId = linkedTransactionId
            )
        )
    }

    override suspend fun removeTransactionLink(transactionId: Long, linkedTransactionId: Long) {
        transactionLinkDao.deleteLink(transactionId, linkedTransactionId)
    }

    override suspend fun clearTransactionLinks(transactionId: Long) {
        transactionLinkDao.deleteAllLinksForTransaction(transactionId)
    }

    override fun getLinkedTransactionIds(transactionId: Long): Flow<List<Long>> {
        return transactionLinkDao.getLinkedTransactionIds(transactionId)
    }

    override fun getLinkedTransactions(transactionId: Long): Flow<List<Transaction>> {
        println("Repository: getLinkedTransactions called for txId=$transactionId")
        return transactionLinkDao.getLinkedTransactions(transactionId).map { entities ->
            println("Repository: Found ${entities.size} linked transactions for txId=$transactionId")
            entities.map { it.toDomain() }
        }
    }
}
