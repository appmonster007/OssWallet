package com.osscabinet.ossWallet.domain.usecase

import com.osscabinet.ossWallet.domain.model.Account
import com.osscabinet.ossWallet.domain.model.Transaction
import com.osscabinet.ossWallet.domain.model.TransactionFilter
import com.osscabinet.ossWallet.domain.model.TransactionWithBalance
import com.osscabinet.ossWallet.domain.repository.SharedWalletRepository
import com.osscabinet.ossWallet.domain.repository.TransactionRepository
import com.osscabinet.ossWallet.domain.service.currency.CurrencyService
import com.osscabinet.ossWallet.domain.service.currency.ExchangeRateSnapshot
import com.osscabinet.ossWallet.domain.service.ledger.LedgerService
import kotlinx.coroutines.flow.Flow
import java.math.BigDecimal

class TransactionUseCases(
    private val repository: TransactionRepository,
    private val sharedWalletRepository: SharedWalletRepository
) {
    // region Transactions

    fun getTransactions(profileId: Int = 1): Flow<List<Transaction>> = repository.getAllTransactions(profileId)

    fun getLatestTransactionVersion(profileId: Int): Flow<Long?> = repository.getLatestTransactionVersion(profileId)

    fun getFilteredTransactions(profileId: Int, filter: TransactionFilter): Flow<List<Transaction>> =
        repository.getFilteredTransactions(profileId, filter)

    suspend fun insertTransaction(transaction: Transaction, profileId: Int = 1): Long =
        repository.insertTransaction(transaction, profileId)

    suspend fun deleteTransaction(id: Long, profileId: Int) = repository.deleteTransaction(id, profileId)

    suspend fun getTransactionById(id: Long, profileId: Int): Transaction? =
        repository.getTransactionById(id, profileId)

    suspend fun updateTransaction(oldTransaction: Transaction, newTransaction: Transaction, profileId: Int) =
        repository.updateTransaction(oldTransaction, newTransaction, profileId)

    suspend fun saveTransfer(
        sourceTransaction: Transaction,
        targetTransaction: Transaction,
        profileId: Int
    ): Pair<Long, Long> = repository.saveTransfer(sourceTransaction, targetTransaction, profileId)

    suspend fun syncTransactions(walletId: String) = sharedWalletRepository.syncTransactions(walletId)

    fun searchTransactions(profileId: Int, query: String): Flow<List<Transaction>> =
        repository.searchTransactions(profileId, query)

    suspend fun getUniquePayees(profileId: Int): List<String> = repository.getUniquePayees(profileId)

    suspend fun getUniqueNotes(profileId: Int): List<String> = repository.getUniqueNotes(profileId)

    suspend fun <T> runAtomically(block: suspend () -> T): T = repository.runAtomically(block)

    // endregion

    // region Links

    suspend fun addTransactionLink(transactionId: Long, linkedTransactionId: Long) =
        repository.addTransactionLink(transactionId, linkedTransactionId)

    suspend fun removeTransactionLink(transactionId: Long, linkedTransactionId: Long) =
        repository.removeTransactionLink(transactionId, linkedTransactionId)

    suspend fun clearTransactionLinks(transactionId: Long) = repository.clearTransactionLinks(transactionId)

    fun getAllTransactionLinks(): Flow<Map<Long, List<Long>>> = repository.getAllTransactionLinks()

    fun getLinkedTransactionIds(transactionId: Long): Flow<List<Long>> =
        repository.getLinkedTransactionIds(transactionId)

    fun getLinkedTransactions(transactionId: Long): Flow<List<Transaction>> =
        repository.getLinkedTransactions(transactionId)

    // endregion

    // region Opening balances

    suspend fun getInitialBalanceTransaction(accountId: String, currency: String, profileId: Int): Transaction? =
        repository.getInitialBalanceTransaction(accountId, currency, profileId)

    suspend fun getInitialBalanceCurrencies(accountId: String, profileId: Int): List<String> =
        repository.getInitialBalanceCurrencies(accountId, profileId)

    suspend fun getEarliestNonInitialBalanceTimestamp(accountId: String, profileId: Int): Long? =
        repository.getEarliestNonInitialBalanceTimestamp(accountId, profileId)

    // endregion

    // region Running balances

    /**
     * Recent activity with running balances: whichever is larger of the last [limit] transactions or
     * every transaction in the last [days] days. Archived accounts are left out unless [includeArchived].
     */
    fun getRecentTransactions(
        allTransactions: List<Transaction>,
        accounts: List<Account>,
        exchangeRates: ExchangeRateSnapshot,
        precision: Int = CurrencyService.precision,
        limit: Int = 20,
        days: Int = 7,
        excludeFuture: Boolean = true,
        includeArchived: Boolean = false,
        startingBalances: Map<String, BigDecimal>
    ): List<TransactionWithBalance> {
        val now = System.currentTimeMillis()
        val candidates = if (excludeFuture) allTransactions.filter { it.timestamp <= now } else allTransactions
        val activeAccountIds = accounts.filterNot { it.isArchived }.map { it.id }.toSet()

        val withBalances = withRunningBalances(candidates, accounts, exchangeRates, precision, startingBalances)
            .filter { includeArchived || it.transaction.accountId in activeAccountIds }
            .sortedByDescending { it.transaction.timestamp }

        if (limit == Int.MAX_VALUE) return withBalances

        val cutoff = now - days * MILLIS_PER_DAY
        val fromLastDays = withBalances.filter { it.transaction.timestamp >= cutoff }
        val lastN = withBalances.take(limit)
        return if (fromLastDays.size > lastN.size) fromLastDays else lastN
    }

    // Each account's rows are folded into a ledger in that account's own currency; amounts already
    // carry their sign (positive for income, negative for expense).
    private fun withRunningBalances(
        transactions: List<Transaction>,
        accounts: List<Account>,
        exchangeRates: ExchangeRateSnapshot,
        precision: Int,
        startingBalances: Map<String, BigDecimal>
    ): List<TransactionWithBalance> {
        val currencyByAccount = accounts.associate { it.id to it.currencyCode }
        return LedgerService.withBalances(transactions, startingBalances, precision) { transaction ->
            CurrencyService.convertAmount(
                amount = transaction.amount,
                fromCurrency = transaction.currency,
                toCurrency = currencyByAccount[transaction.accountId] ?: DEFAULT_CURRENCY,
                exchangeRates = exchangeRates,
                precision = precision + 4
            )
        }
    }

    /**
     * Observes filtered transactions and calculates anchored running balances.
     * Use this for history feeds that require financial accuracy.
     */
    fun observeHistoryWithBalances(
        profileId: Int,
        filter: TransactionFilter,
        baseCurrency: String,
        exchangeRates: ExchangeRateSnapshot,
        limit: Int? = null
    ): Flow<Result<List<TransactionWithBalance>>> =
        repository.observeFilteredTransactionsWithBalance(profileId, filter, baseCurrency, exchangeRates, limit)

    // endregion

    private companion object {
        const val DEFAULT_CURRENCY = "USD"
        const val MILLIS_PER_DAY = 24 * 60 * 60 * 1000L
    }
}
