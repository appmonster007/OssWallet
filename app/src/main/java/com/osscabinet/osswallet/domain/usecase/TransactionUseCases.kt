package com.osscabinet.ossWallet.domain.usecase

import com.osscabinet.ossWallet.domain.model.Account
import com.osscabinet.ossWallet.domain.model.Transaction
import com.osscabinet.ossWallet.domain.model.TransactionFilter
import com.osscabinet.ossWallet.domain.model.TransactionWithBalance
import com.osscabinet.ossWallet.domain.repository.SharedWalletRepository
import com.osscabinet.ossWallet.domain.repository.TransactionRepository
import com.osscabinet.ossWallet.domain.service.currency.CurrencyService
import com.osscabinet.ossWallet.domain.service.currency.ExchangeRateSnapshot
import kotlinx.coroutines.flow.Flow
import java.math.BigDecimal
import java.math.RoundingMode

import com.osscabinet.ossWallet.domain.repository.AccountRepository
import com.osscabinet.ossWallet.domain.repository.CategoryRepository
import com.osscabinet.ossWallet.domain.repository.TagRepository
import com.osscabinet.ossWallet.domain.repository.TemplateRepository
import com.osscabinet.ossWallet.domain.model.Category
import com.osscabinet.ossWallet.domain.model.Tag
import com.osscabinet.ossWallet.domain.model.Template
import kotlinx.coroutines.flow.combine

class TransactionUseCases(
    private val repository: TransactionRepository,
    private val sharedWalletRepository: SharedWalletRepository,
    private val accountRepository: AccountRepository,
    private val categoryRepository: CategoryRepository,
    private val tagRepository: TagRepository,
    private val templateRepository: TemplateRepository
) {
    /**
     * Unified data structure for entry form initialization.
     */
    data class EntryCoreData(
        val accounts: List<Account>,
        val categories: List<Category>,
        val tags: List<Tag>,
        val templates: List<Template>,
        val payees: List<String>,
        val notes: List<String>
    )

    /**
     * Consolidates retrieval of all master data required for an entry form.
     */
    fun observeEntryCoreData(profileId: Int): Flow<EntryCoreData> {
        return combine(
            combine(
                accountRepository.getAllAccounts(profileId),
                categoryRepository.getAllCategories(profileId),
                tagRepository.getAllTags(profileId),
                templateRepository.getAllTemplates(profileId)
            ) { accounts, categories, tags, templates ->
                accounts to Triple(categories, tags, templates)
            },
            repository.observeUniquePayees(profileId),
            repository.observeUniqueNotes(profileId)
        ) { (accounts, triple), payees, notes ->
            val (categories, tags, templates) = triple
            EntryCoreData(accounts, categories, tags, templates, payees, notes)
        }
    }

    fun getTransactions(profileId: Int = 1): Flow<List<Transaction>> {
        return repository.getAllTransactions(profileId)
    }

    fun getFilteredTransactions(
        profileId: Int,
        filter: TransactionFilter
    ): Flow<List<Transaction>> {
        return repository.getFilteredTransactions(profileId, filter)
    }

    suspend fun insertTransaction(transaction: Transaction, profileId: Int = 1): Long {
        return repository.insertTransaction(transaction, profileId)
    }

    suspend fun deleteTransaction(id: Long, profileId: Int) {
        repository.deleteTransaction(id, profileId)
    }

    suspend fun getTransactionById(id: Long, profileId: Int): Transaction? {
        return repository.getTransactionById(id, profileId)
    }

    suspend fun updateTransaction(
        oldTransaction: Transaction,
        newTransaction: Transaction,
        profileId: Int
    ) {
        repository.updateTransaction(oldTransaction, newTransaction, profileId)
    }

    suspend fun saveTransfer(
        sourceTransaction: Transaction,
        targetTransaction: Transaction,
        profileId: Int
    ): Pair<Long, Long> {
        return repository.saveTransfer(sourceTransaction, targetTransaction, profileId)
    }

    suspend fun syncTransactions(walletId: String) {
        sharedWalletRepository.syncTransactions(walletId)
    }

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
        val accountMap = accounts.associateBy { it.id }
        val nonArchivedAccountIds = accounts.filter { !it.isArchived }.map { it.id }.toSet()

        val now = System.currentTimeMillis()
        val filteredTransactions = if (excludeFuture) {
            allTransactions.filter { it.timestamp <= now }
        } else {
            allTransactions
        }

        // Calculate running balances for filtered transactions
        val transactionsWithBalance = filteredTransactions.sortedBy { it.timestamp }
            .groupBy { it.accountId }
            .flatMap { (accId, accountTransactions) ->
                val account = accountMap[accId]
                val accountCurrency = account?.currencyCode ?: "USD"

                var runningBalance = startingBalances[accId ?: ""] ?: BigDecimal.ZERO
                accountTransactions.map { transaction ->
                    val txAmountInAccountCurrency = CurrencyService.convertAmount(
                        amount = transaction.amount,
                        fromCurrency = transaction.currency,
                        toCurrency = accountCurrency,
                        exchangeRates = exchangeRates,
                        precision = precision + 4
                    )

                    val balanceBefore = runningBalance
                    // Amount already carries the sign (positive for income, negative for expense)
                    runningBalance = runningBalance.add(txAmountInAccountCurrency)
                    TransactionWithBalance(
                        transaction = transaction,
                        balanceBefore = balanceBefore.setScale(precision, RoundingMode.HALF_UP),
                        balanceAfter = runningBalance.setScale(precision, RoundingMode.HALF_UP)
                    )
                }
            }
            .let { list -> if (includeArchived) list else list.filter { it.transaction.accountId in nonArchivedAccountIds } }
            .sortedByDescending { it.transaction.timestamp }

        if (limit == Int.MAX_VALUE) return transactionsWithBalance

        // Get transactions from last 7 days
        val cutoffTime = System.currentTimeMillis() - (days * 24 * 60 * 60 * 1000L)
        val transactionsFromLastDays =
            transactionsWithBalance.filter { it.transaction.timestamp >= cutoffTime }

        // Get last 20 transactions
        val lastLimitTransactions = transactionsWithBalance.take(limit)

        // Return whichever has more transactions
        return if (transactionsFromLastDays.size > lastLimitTransactions.size) {
            transactionsFromLastDays
        } else {
            lastLimitTransactions
        }
    }

    // Link management methods
    suspend fun addTransactionLink(transactionId: Long, linkedTransactionId: Long) {
        repository.addTransactionLink(transactionId, linkedTransactionId)
    }

    suspend fun removeTransactionLink(transactionId: Long, linkedTransactionId: Long) {
        repository.removeTransactionLink(transactionId, linkedTransactionId)
    }

    suspend fun clearTransactionLinks(transactionId: Long) {
        repository.clearTransactionLinks(transactionId)
    }

    fun getAllTransactionLinks(): Flow<Map<Long, List<Long>>> {
        return repository.getAllTransactionLinks()
    }

    fun getLinkedTransactionIds(transactionId: Long): Flow<List<Long>> {
        return repository.getLinkedTransactionIds(transactionId)
    }

    fun getLinkedTransactions(transactionId: Long): Flow<List<Transaction>> {
        return repository.getLinkedTransactions(transactionId)
    }

    fun searchTransactions(profileId: Int, query: String): Flow<List<Transaction>> {
        return repository.searchTransactions(profileId, query)
    }

    suspend fun getUniquePayees(profileId: Int): List<String> {
        return repository.getUniquePayees(profileId)
    }

    suspend fun getUniqueNotes(profileId: Int): List<String> {
        return repository.getUniqueNotes(profileId)
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
    ): Flow<Result<List<TransactionWithBalance>>> {
        return repository.observeFilteredTransactionsWithBalance(
            profileId, filter, baseCurrency, exchangeRates, limit
        )
    }

    suspend fun <T> runAtomically(block: suspend () -> T): T =
        repository.runAtomically(block)
}
