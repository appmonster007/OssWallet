package com.osscabinet.ossWallet.domain.usecase

import com.osscabinet.ossWallet.domain.model.Account
import com.osscabinet.ossWallet.domain.model.Transaction
import com.osscabinet.ossWallet.domain.model.TransactionFilter
import com.osscabinet.ossWallet.domain.model.TransactionWithBalance
import com.osscabinet.ossWallet.domain.repository.SharedWalletRepository
import com.osscabinet.ossWallet.domain.repository.TransactionRepository
import com.osscabinet.ossWallet.ui.util.convertAmount
import kotlinx.coroutines.flow.Flow
import java.math.BigDecimal
import java.math.RoundingMode

class TransactionUseCases(
    private val repository: TransactionRepository,
    private val sharedWalletRepository: SharedWalletRepository
) {
    fun getTransactions(profileId: Int = 1): Flow<List<Transaction>> {
        return repository.getAllTransactions(profileId)
    }

    fun getFilteredTransactions(profileId: Int, filter: TransactionFilter): Flow<List<Transaction>> {
        return repository.getFilteredTransactions(profileId, filter)
    }

    suspend fun insertTransaction(transaction: Transaction, profileId: Int = 1): Long {
        return repository.insertTransaction(transaction, profileId)
    }

    suspend fun deleteTransaction(id: Long) {
        repository.deleteTransaction(id)
    }

    suspend fun getTransactionById(id: Long): Transaction? {
        return repository.getTransactionById(id)
    }

    suspend fun updateTransaction(oldTransaction: Transaction, newTransaction: Transaction, profileId: Int) {
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
        exchangeRates: Map<String, BigDecimal>,
        precision: Int,
        limit: Int = 20,
        days: Int = 7
    ): List<TransactionWithBalance> {
        val accountMap = accounts.associateBy { it.id }
        val nonArchivedAccountIds = accounts.filter { !it.isArchived }.map { it.id }.toSet()

        // Calculate running balances for all transactions first (to ensure accurate balances)
        val transactionsWithBalance = allTransactions.sortedBy { it.timestamp }
            .groupBy { it.accountId }
            .flatMap { (accId, accountTransactions) ->
                val account = accountMap[accId]
                val accountCurrency = account?.currencyCode ?: "USD"

                var runningBalance = BigDecimal.ZERO
                accountTransactions.map { transaction ->
                    val txAmountInAccountCurrency = convertAmount(
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
            .filter { it.transaction.accountId in nonArchivedAccountIds }
            .sortedByDescending { it.transaction.timestamp }

        // Get transactions from last 7 days
        val cutoffTime = System.currentTimeMillis() - (days * 24 * 60 * 60 * 1000L)
        val transactionsFromLastDays = transactionsWithBalance.filter { it.transaction.timestamp >= cutoffTime }

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

    fun getLinkedTransactionIds(transactionId: Long): Flow<List<Long>> {
        return repository.getLinkedTransactionIds(transactionId)
    }

    fun getLinkedTransactions(transactionId: Long): Flow<List<Transaction>> {
        return repository.getLinkedTransactions(transactionId)
    }
}
