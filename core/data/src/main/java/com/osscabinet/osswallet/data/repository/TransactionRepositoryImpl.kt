package com.osscabinet.ossWallet.data.repository

import androidx.room.withTransaction
import androidx.sqlite.db.SimpleSQLiteQuery
import com.osscabinet.ossWallet.data.local.AppDatabase
import com.osscabinet.ossWallet.data.local.dao.AccountDao
import com.osscabinet.ossWallet.data.local.dao.TransactionDao
import com.osscabinet.ossWallet.data.local.dao.TransactionLinkDao
import com.osscabinet.ossWallet.data.mapper.toDomain
import com.osscabinet.ossWallet.data.mapper.toEntity
import com.osscabinet.ossWallet.domain.model.CurrencySummary
import com.osscabinet.ossWallet.domain.model.Transaction
import com.osscabinet.ossWallet.domain.model.TransactionFilter
import com.osscabinet.ossWallet.domain.model.TransactionWithBalance
import com.osscabinet.ossWallet.domain.service.currency.ExchangeRateSnapshot
import com.osscabinet.ossWallet.domain.repository.TransactionRepository
import com.osscabinet.ossWallet.domain.service.timeline.TimelineService
import com.osscabinet.ossWallet.domain.util.joinByComma
import com.osscabinet.ossWallet.domain.util.toSmartRegex
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import java.math.BigDecimal
import java.math.RoundingMode
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TransactionRepositoryImpl @Inject constructor(
    private val transactionDao: TransactionDao,
    private val accountDao: AccountDao,
    private val tagDao: com.osscabinet.ossWallet.data.local.dao.TagDao,
    private val transactionLinkDao: TransactionLinkDao,
    private val database: AppDatabase,
    private val databaseQueryService: com.osscabinet.ossWallet.domain.service.query.DatabaseQueryService
) : TransactionRepository {

    override fun getAllTransactions(profileId: Int): Flow<List<Transaction>> {
        return transactionDao.getAllTransactions(profileId).mapToDomain { it.toDomain() }
    }

    override suspend fun getAllTransactionsSync(profileId: Int): List<Transaction> {
        return transactionDao.getAllTransactionsSync(profileId).mapToDomain { it.toDomain() }
    }

    override fun getFilteredTransactions(
        profileId: Int,
        filter: TransactionFilter
    ): Flow<List<Transaction>> {
        val (query, args) = buildFilteredQuery(profileId, filter)
        val sqliteQuery = SimpleSQLiteQuery(query, args.toTypedArray())

        return transactionDao.getFilteredTransactions(sqliteQuery).mapToDomain { entity ->
            // Note: applyRegexFilter still needs to be applied after mapping or inside mapToDomain
            entity.toDomain()
        }.map { applyRegexFilter(it, filter) }
    }

    override suspend fun getFilteredTransactionsSync(
        profileId: Int,
        filter: TransactionFilter
    ): List<Transaction> {
        val (query, args) = buildFilteredQuery(profileId, filter)
        val sqliteQuery = SimpleSQLiteQuery(query, args.toTypedArray())

        return applyRegexFilter(
            transactionDao.getFilteredTransactionsSync(sqliteQuery).mapToDomain { it.toDomain() },
            filter
        )
    }

    private fun buildFilteredQuery(
        profileId: Int,
        filter: TransactionFilter
    ): Pair<String, List<Any>> {
        val queryBuilder = StringBuilder("SELECT * FROM transactions WHERE profileId = ? AND status != 'DELETED'")
        val args = mutableListOf<Any>(profileId)

        val (startTime, endTime) = TimelineService.getDateRange(filter.timelineRange)

        if (startTime != null) {
            queryBuilder.append(" AND timestamp >= ?")
            args.add(startTime)
        }
        if (endTime != null) {
            queryBuilder.append(" AND timestamp <= ?")
            args.add(endTime)
        }
        if (filter.accountIds.isNotEmpty()) {
            val placeholders = filter.accountIds.toList().joinByComma { "?" }
            queryBuilder.append(" AND accountId IN ($placeholders)")
            args.addAll(filter.accountIds)
        }
        if (filter.categoryIds.isNotEmpty()) {
            val placeholders = filter.categoryIds.toList().joinByComma { "?" }
            queryBuilder.append(" AND categoryId IN ($placeholders)")
            args.addAll(filter.categoryIds)
        }
        if (filter.transactionTypes.isNotEmpty()) {
            val placeholders = filter.transactionTypes.toList().joinByComma { "?" }
            queryBuilder.append(" AND transactionType IN ($placeholders)")
            args.addAll(filter.transactionTypes.map { it.value })
        }
        val minAmount = filter.minAmount
        if (minAmount != null) {
            queryBuilder.append(" AND amount >= ?")
            args.add(minAmount)
        }
        val maxAmount = filter.maxAmount
        if (maxAmount != null) {
            queryBuilder.append(" AND amount <= ?")
            args.add(maxAmount)
        }
        if (filter.paymentMethods.isNotEmpty()) {
            val placeholders = filter.paymentMethods.toList().joinByComma { "?" }
            queryBuilder.append(" AND paymentMethod IN ($placeholders)")
            args.addAll(filter.paymentMethods.map { it.name })
        }
        if (filter.hasWarranty == true) {
            queryBuilder.append(" AND warrantyExpiration IS NOT NULL")
        } else if (filter.hasWarranty == false) {
            queryBuilder.append(" AND warrantyExpiration IS NULL")
        }
        val warrantyExpirationStart = filter.warrantyExpirationStart
        if (warrantyExpirationStart != null) {
            queryBuilder.append(" AND warrantyExpiration >= ?")
            args.add(warrantyExpirationStart)
        }
        val warrantyExpirationEnd = filter.warrantyExpirationEnd
        if (warrantyExpirationEnd != null) {
            queryBuilder.append(" AND warrantyExpiration <= ?")
            args.add(warrantyExpirationEnd)
        }
        if (filter.currencies.isNotEmpty()) {
            val placeholders = filter.currencies.toList().joinByComma { "?" }
            queryBuilder.append(" AND currency IN ($placeholders)")
            args.addAll(filter.currencies)
        }
        val queryNote = filter.queryNote
        if (!queryNote.isNullOrBlank() && !filter.isNoteRegex) {
            // Use COLLATE NOCASE for case-insensitive matching (SQLite default)
            queryBuilder.append(" AND note LIKE ? COLLATE NOCASE")
            args.add("%${queryNote}%")
        }
        val queryPayee = filter.queryPayee
        if (!queryPayee.isNullOrBlank() && !filter.isPayeeRegex) {
            // Use COLLATE NOCASE for case-insensitive matching (SQLite default)
            queryBuilder.append(" AND payee LIKE ? COLLATE NOCASE")
            args.add("%${queryPayee}%")
        }
        if (filter.statuses.isNotEmpty()) {
            val placeholders = filter.statuses.toList().joinByComma { "?" }
            queryBuilder.append(" AND status IN ($placeholders)")
            args.addAll(filter.statuses.map { it.name })
        }
        queryBuilder.append(" ORDER BY timestamp DESC")
        return Pair(queryBuilder.toString(), args)
    }

    private fun applyRegexFilter(
        domainTransactions: List<Transaction>,
        filter: TransactionFilter
    ): List<Transaction> {
        return if (filter.isNoteRegex || filter.isPayeeRegex) {
            domainTransactions.filter { transaction ->
                val queryNote = filter.queryNote
                val matchesNote = if (filter.isNoteRegex && !queryNote.isNullOrBlank()) {
                    try {
                        queryNote.toSmartRegex().containsMatchIn(transaction.note)
                    } catch (e: Exception) {
                        false
                    }
                } else true

                val queryPayee = filter.queryPayee
                val matchesPayee = if (filter.isPayeeRegex && !queryPayee.isNullOrBlank()) {
                    try {
                        queryPayee.toSmartRegex().containsMatchIn(transaction.payee)
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
        return database.withTransaction {
            val id = transactionDao.insertTransactionWithBalanceUpdate(
                transaction.toEntity(profileId),
                accountDao
            )
            syncTransactionTags(id, transaction.tags)
            id
        }
    }

    private suspend fun syncTransactionTags(transactionId: Long, tagNames: Set<String>) {
        if (transactionId <= 0) return
        
        val sanitizedNames = tagNames.filter { it.isNotBlank() }.map { it.trim() }.toSet()
        
        // 1. Clear existing links for this transaction to allow a clean sync
        transactionDao.deleteTransactionTagCrossRefs(transactionId)
        
        if (sanitizedNames.isEmpty()) return

        // 2. Ensure tags exist and collect their IDs uniquely by name
        val tagIds = sanitizedNames.mapNotNull { name ->
            tagDao.getTagIdByName(name) ?: run {
                val newId = tagDao.insertTag(
                    com.osscabinet.ossWallet.data.local.entity.TagEntity(
                        name = name,
                        colorHex = -1
                    )
                )
                if (newId > 0) newId else tagDao.getTagIdByName(name)
            }
        }.toSet()

        // 3. Insert cross-references
        tagIds.forEach { tagId ->
            transactionDao.insertTransactionTagCrossRef(
                com.osscabinet.ossWallet.data.local.entity.TransactionTagCrossRef(
                    transactionId = transactionId,
                    tagId = tagId
                )
            )
        }
    }

    override suspend fun saveTransfer(
        sourceTransaction: Transaction,
        targetTransaction: Transaction,
        profileId: Int
    ): Pair<Long, Long> {
        return database.withTransaction {
            val sourceId = transactionDao.insertTransactionWithBalanceUpdate(
                sourceTransaction.toEntity(profileId), accountDao
            )
            val targetId = transactionDao.insertTransactionWithBalanceUpdate(
                targetTransaction.toEntity(profileId), accountDao
            )

            if (sourceId > 0) syncTransactionTags(sourceId, sourceTransaction.tags)
            if (targetId > 0) syncTransactionTags(targetId, targetTransaction.tags)

            if (sourceId > 0 && targetId > 0) {
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
            }

            Pair(sourceId, targetId)
        }
    }

    override suspend fun updateTransaction(
        oldTransaction: Transaction,
        newTransaction: Transaction,
        profileId: Int
    ) {
        database.withTransaction {
            val id = transactionDao.updateTransactionWithBalanceUpdate(
                oldTransaction.toEntity(profileId),
                newTransaction.toEntity(profileId),
                accountDao
            )
            if (id > 0) {
                syncTransactionTags(id, newTransaction.tags)
            }
        }
    }

    override suspend fun deleteTransaction(id: Long, profileId: Int) {
        val transaction = transactionDao.getTransactionById(id, profileId)
        transaction?.let {
            transactionDao.deleteTransactionWithBalanceUpdate(it, accountDao)
        }
    }

    override fun getTransactionsByAccount(
        accountId: String,
        profileId: Int
    ): Flow<List<Transaction>> {
        return transactionDao.getTransactionsByAccount(accountId, profileId)
            .mapToDomain { it.toDomain() }
    }

    override suspend fun getTransactionById(id: Long, profileId: Int): Transaction? {
        return transactionDao.getTransactionById(id, profileId)?.toDomain()
    }

    override suspend fun getInitialBalanceTransaction(accountId: String, currency: String, profileId: Int): Transaction? {
        return transactionDao.getInitialBalanceTransaction(
            accountId, currency, listOf(com.osscabinet.ossWallet.domain.model.Category.SYSTEM_INITIAL_BALANCE), profileId
        )?.toDomain()
    }

    override suspend fun getCurrencySummaries(profileId: Int): List<CurrencySummary> {
        return transactionDao.getCurrencySummaries(profileId).map { dao ->
            CurrencySummary(
                currency = dao.currency,
                totalIncome = dao.totalIncome,
                totalExpense = dao.totalExpense
            )
        }
    }

    override suspend fun getCurrencySummariesInRange(
        profileId: Int,
        start: Long,
        end: Long
    ): List<CurrencySummary> {
        return transactionDao.getCurrencySummariesInRange(profileId, start, end).map { dao ->
            CurrencySummary(
                currency = dao.currency,
                totalIncome = dao.totalIncome,
                totalExpense = dao.totalExpense
            )
        }
    }

    // Link management implementations
    override suspend fun addTransactionLink(transactionId: Long, linkedTransactionId: Long) {
        database.withTransaction {
            transactionLinkDao.insertLink(
                com.osscabinet.ossWallet.data.local.entity.TransactionLinkEntity(
                    transactionId = transactionId,
                    linkedTransactionId = linkedTransactionId
                )
            )
            transactionLinkDao.insertLink(
                com.osscabinet.ossWallet.data.local.entity.TransactionLinkEntity(
                    transactionId = linkedTransactionId,
                    linkedTransactionId = transactionId
                )
            )
        }
    }

    override suspend fun removeTransactionLink(transactionId: Long, linkedTransactionId: Long) {
        database.withTransaction {
            transactionLinkDao.deleteLink(transactionId, linkedTransactionId)
            transactionLinkDao.deleteLink(linkedTransactionId, transactionId)
        }
    }

    override suspend fun clearTransactionLinks(transactionId: Long) {
        transactionLinkDao.deleteAnyLink(transactionId)
    }

    override fun getAllTransactionLinks(): Flow<Map<Long, List<Long>>> {
        return transactionLinkDao.getAllLinks().map { links ->
            links.groupBy { it.transactionId }
                .mapValues { (_, entities) -> entities.map { it.linkedTransactionId } }
        }
    }

    override fun getLinkedTransactionIds(transactionId: Long): Flow<List<Long>> {
        return transactionLinkDao.getLinkedTransactionIds(transactionId)
    }

    override fun getLinkedTransactions(transactionId: Long): Flow<List<Transaction>> {
        return transactionLinkDao.getLinkedTransactions(transactionId).mapToDomain { entity ->
            entity.toDomain()
        }
    }

    override fun searchTransactions(profileId: Int, query: String): Flow<List<Transaction>> {
        val trimmed = query.trim()
        val sanitizedQuery = trimmed
            .split(Regex("\\s+"))
            .filter { it.isNotBlank() }
            .joinToString(" ") { "$it*" }

        return if (sanitizedQuery.isBlank()) {
            getAllTransactions(profileId)
        } else {
            transactionDao.searchTransactionsUnified(profileId, sanitizedQuery, trimmed)
                .mapToDomain { it.toDomain() }
        }
    }

    @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
    override fun observeFilteredTransactionsWithBalance(
        profileId: Int,
        filter: TransactionFilter,
        baseCurrency: String,
        exchangeRates: ExchangeRateSnapshot,
        limit: Int?
    ): Flow<Result<List<TransactionWithBalance>>> {
        return getFilteredTransactions(profileId, filter).flatMapLatest { transactions ->
            kotlinx.coroutines.flow.flow {
                val accounts = accountDao.getAllAccountsSync(profileId).map { it.toDomain() }
                val exchangeRateSnapshot = exchangeRates

                // 1. Fetch starting balances (Anchor)
                val startingBalancesResult = if (transactions.isNotEmpty()) {
                    val windowStart = transactions.minOf { it.timestamp }
                    com.osscabinet.ossWallet.domain.service.aggregation.TransactionAggregationService.getSummaryByAccountAndCurrency(
                        databaseQueryService, profileId,
                        TransactionFilter(
                            accountIds = filter.accountIds,
                            timelineRange = com.osscabinet.ossWallet.domain.model.timeline.TimelineRange.CustomRange(end = windowStart - 1)
                        )
                    ).map { summaryMap ->
                        summaryMap.mapValues { (accountId, byCurrency) ->
                            val accountCurrency = accounts.find { it.id == accountId }?.currencyCode ?: baseCurrency
                            com.osscabinet.ossWallet.domain.service.currency.CurrencyService.calculateTotalFinancialSummary(byCurrency, accountCurrency, exchangeRateSnapshot).totalAmount
                        }
                    }
                } else Result.success(emptyMap<String, BigDecimal>())

                startingBalancesResult.onSuccess { startingBalances ->
                    // 2. Compute Delta balances
                    val accountMap = accounts.associateBy { it.id }
                    val result = transactions.sortedBy { it.timestamp }
                        .groupBy { it.accountId }
                        .flatMap { (accId, accountTransactions) ->
                            val accountCurrency = accountMap[accId]?.currencyCode ?: baseCurrency
                            var runningBalance = startingBalances[accId ?: ""] ?: BigDecimal.ZERO
                            
                            accountTransactions.map { transaction ->
                                val txAmountInAccountCurrency = com.osscabinet.ossWallet.domain.service.currency.CurrencyService.convertAmount(
                                    amount = transaction.amount,
                                    fromCurrency = transaction.currency,
                                    toCurrency = accountCurrency,
                                    exchangeRates = exchangeRates,
                                    precision = com.osscabinet.ossWallet.domain.service.currency.CurrencyService.precision + 4
                                )

                                val balanceBefore = runningBalance
                                runningBalance = runningBalance.add(txAmountInAccountCurrency)
                                
                                TransactionWithBalance(
                                    transaction = transaction,
                                    balanceBefore = balanceBefore.setScale(com.osscabinet.ossWallet.domain.service.currency.CurrencyService.precision, RoundingMode.HALF_UP),
                                    balanceAfter = runningBalance.setScale(com.osscabinet.ossWallet.domain.service.currency.CurrencyService.precision, RoundingMode.HALF_UP)
                                )
                            }
                        }
                        .sortedByDescending { it.transaction.timestamp }
                        .let { if (limit != null) it.take(limit) else it }

                    emit(Result.success(result))
                }.onFailure { exception ->
                    emit(Result.failure(exception))
                }
            }
        }
    }

    override suspend fun getUniquePayees(profileId: Int): List<String> {
        return transactionDao.getUniquePayees(profileId)
    }

    override suspend fun getUniqueNotes(profileId: Int): List<String> {
        return transactionDao.getUniqueNotes(profileId)
    }

    override suspend fun <T> runAtomically(block: suspend () -> T): T =
        database.withTransaction { block() }
}
