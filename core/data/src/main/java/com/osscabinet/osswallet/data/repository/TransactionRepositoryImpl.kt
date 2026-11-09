package com.osscabinet.ossWallet.data.repository

import androidx.room.withTransaction
import androidx.sqlite.db.SimpleSQLiteQuery
import com.osscabinet.ossWallet.data.local.AppDatabase
import com.osscabinet.ossWallet.data.local.dao.AccountDao
import com.osscabinet.ossWallet.data.local.dao.TagDao
import com.osscabinet.ossWallet.data.local.dao.TransactionDao
import com.osscabinet.ossWallet.data.local.dao.TransactionLinkDao
import com.osscabinet.ossWallet.data.local.entity.TagEntity
import com.osscabinet.ossWallet.data.local.entity.TransactionEntity
import com.osscabinet.ossWallet.data.local.entity.TransactionLinkEntity
import com.osscabinet.ossWallet.data.local.entity.TransactionTagCrossRef
import com.osscabinet.ossWallet.data.local.query.FilterSql
import com.osscabinet.ossWallet.data.mapper.toDomain
import com.osscabinet.ossWallet.data.mapper.toEntity
import com.osscabinet.ossWallet.domain.model.Account
import com.osscabinet.ossWallet.domain.model.Category
import com.osscabinet.ossWallet.domain.model.CurrencySummary
import com.osscabinet.ossWallet.domain.model.Transaction
import com.osscabinet.ossWallet.domain.model.TransactionFilter
import com.osscabinet.ossWallet.domain.model.TransactionWithBalance
import com.osscabinet.ossWallet.domain.model.balanceBasis
import com.osscabinet.ossWallet.domain.model.timeline.TimelineRange
import com.osscabinet.ossWallet.domain.repository.TransactionRepository
import com.osscabinet.ossWallet.domain.service.aggregation.TransactionAggregationService
import com.osscabinet.ossWallet.domain.service.currency.CurrencyService
import com.osscabinet.ossWallet.domain.service.currency.ExchangeRateSnapshot
import com.osscabinet.ossWallet.domain.service.ledger.LedgerService
import com.osscabinet.ossWallet.domain.service.query.DatabaseQueryService
import com.osscabinet.ossWallet.domain.service.search.AmountSpec
import com.osscabinet.ossWallet.domain.service.search.ParsedSearch
import com.osscabinet.ossWallet.domain.service.timeline.TimelineService
import com.osscabinet.ossWallet.domain.util.joinByComma
import com.osscabinet.ossWallet.domain.util.toSmartRegex
import jakarta.inject.Inject
import jakarta.inject.Singleton
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import java.math.BigDecimal

@Singleton
class TransactionRepositoryImpl @Inject constructor(
    private val transactionDao: TransactionDao,
    private val accountDao: AccountDao,
    private val tagDao: TagDao,
    private val transactionLinkDao: TransactionLinkDao,
    private val database: AppDatabase,
    private val databaseQueryService: DatabaseQueryService
) : TransactionRepository {

    // region Queries

    override fun getAllTransactions(profileId: Int): Flow<List<Transaction>> =
        transactionDao.getAllTransactions(profileId).mapToDomain { it.toDomain() }

    override fun getLatestTransactionVersion(profileId: Int): Flow<Long?> =
        transactionDao.getLatestTransactionVersion(profileId)

    override suspend fun getAllTransactionsSync(profileId: Int): List<Transaction> =
        transactionDao.getAllTransactionsSync(profileId).mapToDomain { it.toDomain() }

    override fun getFilteredTransactions(profileId: Int, filter: TransactionFilter): Flow<List<Transaction>> =
        getFilteredTransactionsBasis(profileId, filter).map { applyRegexFilter(it, filter) }

    override suspend fun getFilteredTransactionsSync(profileId: Int, filter: TransactionFilter): List<Transaction> {
        val sqliteQuery = filteredQuery(profileId, filter)
        return applyRegexFilter(
            transactionDao.getFilteredTransactionsSync(sqliteQuery).mapToDomain { it.toDomain() },
            filter
        )
    }

    // The SQL half of a filter only; regex criteria cannot be expressed in SQL and are applied afterwards.
    private fun getFilteredTransactionsBasis(profileId: Int, filter: TransactionFilter): Flow<List<Transaction>> =
        transactionDao.getFilteredTransactions(filteredQuery(profileId, filter)).mapToDomain { it.toDomain() }

    private fun filteredQuery(profileId: Int, filter: TransactionFilter): SimpleSQLiteQuery {
        val query = StringBuilder("SELECT * FROM transactions WHERE profileId = ? AND status != 'DELETED'")
        val args = mutableListOf<Any>(profileId)

        val (startTime, endTime) = TimelineService.getDateRange(filter.timelineRange)
        if (startTime != null) {
            query.append(" AND timestamp >= ?")
            args.add(startTime)
        }
        if (endTime != null) {
            query.append(" AND timestamp <= ?")
            args.add(endTime)
        }
        if (filter.accountIds.isNotEmpty()) {
            query.append(" AND accountId IN (${filter.accountIds.toList().joinByComma { "?" }})")
            args.addAll(filter.accountIds)
        }
        FilterSql.appendCriteria(query, args, filter)
        query.append(" ORDER BY timestamp DESC")
        return SimpleSQLiteQuery(query.toString(), args.toTypedArray())
    }

    private fun applyRegexFilter(transactions: List<Transaction>, filter: TransactionFilter): List<Transaction> {
        if (!filter.isNoteRegex && !filter.isPayeeRegex) return transactions
        return transactions.filter { transaction ->
            (!filter.isNoteRegex || matchesRegex(filter.queryNote, transaction.note)) &&
                (!filter.isPayeeRegex || matchesRegex(filter.queryPayee, transaction.payee))
        }
    }

    // A blank pattern matches everything; an invalid one matches nothing.
    private fun matchesRegex(pattern: String?, text: String): Boolean {
        if (pattern.isNullOrBlank()) return true
        return try {
            pattern.toSmartRegex().containsMatchIn(text)
        } catch (e: Exception) {
            false
        }
    }

    override fun getTransactionsByAccount(accountId: String, profileId: Int): Flow<List<Transaction>> =
        transactionDao.getTransactionsByAccount(accountId, profileId).mapToDomain { it.toDomain() }

    override suspend fun getTransactionById(id: Long, profileId: Int): Transaction? =
        transactionDao.getTransactionById(id, profileId)?.toDomain()

    override suspend fun getInitialBalanceTransaction(
        accountId: String,
        currency: String,
        profileId: Int
    ): Transaction? = transactionDao.getInitialBalanceTransaction(
        accountId,
        currency,
        listOf(Category.SYSTEM_INITIAL_BALANCE),
        profileId
    )?.toDomain()

    override suspend fun getInitialBalanceCurrencies(accountId: String, profileId: Int): List<String> =
        transactionDao.getCurrenciesForCategory(accountId, Category.SYSTEM_INITIAL_BALANCE, profileId)

    override suspend fun getCurrenciesWithBalanceData(accountId: String, profileId: Int): List<String> =
        transactionDao.getCurrenciesForCategories(accountId, BALANCE_DATA_CATEGORIES, profileId)

    override suspend fun getEarliestNonInitialBalanceTimestamp(accountId: String, profileId: Int): Long? =
        transactionDao.getEarliestTimestampExcluding(accountId, BALANCE_DATA_CATEGORIES, profileId)

    override suspend fun getCurrencySummaries(profileId: Int): List<CurrencySummary> =
        transactionDao.getCurrencySummaries(profileId).map { dao ->
            CurrencySummary(dao.currency, totalIncome = dao.totalIncome, totalExpense = dao.totalExpense)
        }

    override suspend fun getCurrencySummariesInRange(profileId: Int, start: Long, end: Long): List<CurrencySummary> =
        transactionDao.getCurrencySummariesInRange(profileId, start, end).map { dao ->
            CurrencySummary(dao.currency, totalIncome = dao.totalIncome, totalExpense = dao.totalExpense)
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
            transactionDao.searchTransactionsUnified(profileId, sanitizedQuery, trimmed).mapToDomain { it.toDomain() }
        }
    }

    override suspend fun searchForGlobal(profileId: Int, query: ParsedSearch, limit: Int): List<Transaction> {
        // The full-text engine chokes on punctuation, so keep letters and digits only.
        val words = query.words.map { it.replace(Regex("[^\\p{L}\\p{N}]"), "") }.filter { it.isNotEmpty() }
        val amount = query.amount

        // One source finds the candidates and the caller's post-filter narrows them: words if there are any,
        // else tags, else amounts. A lone number is both a possible word and a possible amount.
        val rows = when {
            words.isNotEmpty() -> {
                val text = transactionDao.searchText(
                    profileId, words.joinToString(" ") { "$it*" }, query.words.joinToString(" "), limit
                )
                if (query.numberOnly && amount != null) text + searchByAmount(profileId, amount, limit) else text
            }
            query.tags.isNotEmpty() -> transactionDao.searchByTag(profileId, query.tags.first(), limit)
            amount != null -> searchByAmount(profileId, amount, limit)
            else -> emptyList()
        }
        return rows.distinctBy { it.id }.sortedByDescending { it.timestamp }.take(limit).map { it.toDomain() }
    }

    private suspend fun searchByAmount(profileId: Int, spec: AmountSpec, limit: Int): List<TransactionEntity> {
        // A single value gets half a cent of slack for the text-to-number conversion.
        val tolerance = if (spec.min != null && spec.min == spec.max) 0.005 else 0.0
        return transactionDao.searchByAmount(
            profileId,
            (spec.min?.toDouble() ?: 0.0) - tolerance,
            (spec.max?.toDouble() ?: Double.MAX_VALUE) + tolerance,
            limit
        )
    }

    override suspend fun getUniquePayees(profileId: Int): List<String> = transactionDao.getUniquePayees(profileId)

    override fun observeUniquePayees(profileId: Int): Flow<List<String>> =
        transactionDao.observeUniquePayees(profileId)

    override suspend fun getUniqueNotes(profileId: Int): List<String> = transactionDao.getUniqueNotes(profileId)

    override fun observeUniqueNotes(profileId: Int): Flow<List<String>> =
        transactionDao.observeUniqueNotes(profileId)

    // endregion

    // region Writes

    override suspend fun insertTransaction(transaction: Transaction, profileId: Int): Long =
        database.withTransaction {
            val id = transactionDao.insertTransactionWithBalanceUpdate(transaction.toEntity(profileId), accountDao)
            syncTransactionTags(id, transaction.tags, profileId)
            id
        }

    override suspend fun saveTransfer(
        sourceTransaction: Transaction,
        targetTransaction: Transaction,
        profileId: Int
    ): Pair<Long, Long> = database.withTransaction {
        val sourceId = transactionDao.insertTransactionWithBalanceUpdate(sourceTransaction.toEntity(profileId), accountDao)
        val targetId = transactionDao.insertTransactionWithBalanceUpdate(targetTransaction.toEntity(profileId), accountDao)

        if (sourceId > 0) syncTransactionTags(sourceId, sourceTransaction.tags, profileId)
        if (targetId > 0) syncTransactionTags(targetId, targetTransaction.tags, profileId)
        if (sourceId > 0 && targetId > 0) insertLinkBothWays(sourceId, targetId)

        Pair(sourceId, targetId)
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
            if (id > 0) syncTransactionTags(id, newTransaction.tags, profileId)
        }
    }

    override suspend fun deleteTransaction(id: Long, profileId: Int) {
        transactionDao.getTransactionById(id, profileId)?.let {
            transactionDao.deleteTransactionWithBalanceUpdate(it, accountDao)
        }
    }

    // Replaces the row's tag links with [tagNames], creating any tag that does not exist yet.
    private suspend fun syncTransactionTags(transactionId: Long, tagNames: Set<String>, profileId: Int) {
        if (transactionId <= 0) return

        transactionDao.deleteTransactionTagCrossRefs(transactionId)

        val names = tagNames.map { it.trim() }.filter { it.isNotEmpty() }.toSet()
        val tagIds = names.mapNotNull { name ->
            tagDao.getTagIdByName(name, profileId) ?: run {
                val newId = tagDao.insertTag(TagEntity(name = name, colorHex = -1, profileId = profileId))
                if (newId > 0) newId else tagDao.getTagIdByName(name, profileId)
            }
        }.toSet()

        tagIds.forEach { tagId ->
            transactionDao.insertTransactionTagCrossRef(TransactionTagCrossRef(transactionId, tagId))
        }
    }

    // endregion

    // region Links

    override suspend fun addTransactionLink(transactionId: Long, linkedTransactionId: Long) {
        database.withTransaction { insertLinkBothWays(transactionId, linkedTransactionId) }
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

    override fun getAllTransactionLinks(): Flow<Map<Long, List<Long>>> =
        transactionLinkDao.getAllLinks().map { links ->
            links.groupBy { it.transactionId }
                .mapValues { (_, entities) -> entities.map { it.linkedTransactionId } }
        }

    override fun getLinkedTransactionIds(transactionId: Long): Flow<List<Long>> =
        transactionLinkDao.getLinkedTransactionIds(transactionId)

    override fun getLinkedTransactions(transactionId: Long): Flow<List<Transaction>> =
        transactionLinkDao.getLinkedTransactions(transactionId).mapToDomain { it.toDomain() }

    private suspend fun insertLinkBothWays(first: Long, second: Long) {
        transactionLinkDao.insertLink(TransactionLinkEntity(transactionId = first, linkedTransactionId = second))
        transactionLinkDao.insertLink(TransactionLinkEntity(transactionId = second, linkedTransactionId = first))
    }

    // endregion

    // region Running balance

    @OptIn(ExperimentalCoroutinesApi::class)
    override fun observeFilteredTransactionsWithBalance(
        profileId: Int,
        filter: TransactionFilter,
        baseCurrency: String,
        exchangeRates: ExchangeRateSnapshot,
        limit: Int?
    ): Flow<Result<List<TransactionWithBalance>>> {
        // The balance basis is account scope + time window ONLY - never category, tag, status,
        // currency, amount, etc. Those are display-only and must be applied to the result of the
        // balance computation, not to its input, or every balance after an excluded row is wrong.
        return getFilteredTransactionsBasis(profileId, filter.balanceBasis()).flatMapLatest { basis ->
            flow {
                val accounts = accountDao.getAllAccountsSync(profileId).map { it.toDomain() }
                loadStartingBalances(profileId, filter, basis, accounts, baseCurrency, exchangeRates)
                    .onSuccess { startingBalances ->
                        val currencyByAccount = accounts.associate { it.id to it.currencyCode }
                        val ledger = LedgerService.withBalances(
                            basis, startingBalances, CurrencyService.precision
                        ) { transaction ->
                            CurrencyService.convertAmount(
                                amount = transaction.amount,
                                fromCurrency = transaction.currency,
                                toCurrency = currencyByAccount[transaction.accountId] ?: baseCurrency,
                                exchangeRates = exchangeRates,
                                precision = CurrencyService.precision + 4
                            )
                        }.associateBy { it.transaction.id }
                        // The display filter is resolved the normal way, reusing the existing filtering SQL,
                        // and each surviving row gets its balance from the unfiltered ledger.
                        val displayed = getFilteredTransactionsSync(profileId, filter)
                        val rows = displayed
                            .mapNotNull { transaction -> ledger[transaction.id]?.copy(transaction = transaction) }
                            .sortedByDescending { it.transaction.timestamp }
                        emit(Result.success(if (limit != null) rows.take(limit) else rows))
                    }
                    .onFailure { emit(Result.failure(it)) }
            }
        }
    }

    // Each account's balance just before the window opens (the anchor the ledger is rebuilt from),
    // scoped the same way the basis rows are.
    private suspend fun loadStartingBalances(
        profileId: Int,
        filter: TransactionFilter,
        basis: List<Transaction>,
        accounts: List<Account>,
        baseCurrency: String,
        exchangeRates: ExchangeRateSnapshot
    ): Result<Map<String, BigDecimal>> {
        if (basis.isEmpty()) return Result.success(emptyMap())

        val beforeWindow = TransactionFilter(
            accountIds = filter.accountIds,
            timelineRange = TimelineRange.CustomRange(end = basis.minOf { it.timestamp } - 1)
        )
        return TransactionAggregationService.getSummaryByAccountAndCurrency(databaseQueryService, profileId, beforeWindow)
            .map { summaries ->
                summaries.mapValues { (accountId, byCurrency) ->
                    val accountCurrency = accounts.find { it.id == accountId }?.currencyCode ?: baseCurrency
                    CurrencyService.calculateTotalFinancialSummary(byCurrency, accountCurrency, exchangeRates).totalAmount
                }
            }
    }

    // endregion

    override suspend fun <T> runAtomically(block: suspend () -> T): T = database.withTransaction { block() }

    private companion object {
        // Opening balances and adjustments: the rows that are not "real" activity on an account.
        val BALANCE_DATA_CATEGORIES = listOf(
            Category.SYSTEM_INITIAL_BALANCE,
            Category.SYSTEM_RECONCILIATION_ADJUSTMENT
        )
    }
}
