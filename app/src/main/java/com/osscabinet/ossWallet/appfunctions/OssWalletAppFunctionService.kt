package com.osscabinet.ossWallet.appfunctions

import androidx.annotation.RequiresApi
import androidx.appfunctions.AppFunctionAppUnknownException
import androidx.appfunctions.AppFunctionDeclaration
import androidx.appfunctions.AppFunctionDeniedException
import androidx.appfunctions.AppFunctionDisabledException
import androidx.appfunctions.AppFunctionElementNotFoundException
import androidx.appfunctions.AppFunctionException
import androidx.appfunctions.AppFunctionInvalidArgumentException
import androidx.appfunctions.AppFunctionService
import androidx.appfunctions.AppFunctionServiceEntryPoint
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CancellationException

@EntryPoint
@InstallIn(SingletonComponent::class)
interface AssistantEntryPoint {
    fun assistantFunctions(): AssistantFunctions
}

/**
 * Registers OssWallet's functions with the system so AI assistants can call them. Every function ships disabled and
 * is switched on from Settings (see [AssistantAccessController]); each also refuses to run while the app is locked.
 * The work itself lives in [AssistantFunctions]; this class only describes the functions and translates errors.
 */
@RequiresApi(36)
@AppFunctionServiceEntryPoint(
    serviceName = "OssWalletAppFunctionService",
    appFunctionXmlFileName = "osswallet_app_functions"
)
abstract class BaseOssWalletAppFunctionService : AppFunctionService() {

    // Hilt can't inject into a class the compiler subclasses, so look the logic up when first needed.
    private val functions: AssistantFunctions by lazy {
        EntryPointAccessors.fromApplication(applicationContext, AssistantEntryPoint::class.java).assistantFunctions()
    }

    /**
     * Lists the user's accounts (name and currency only, never balances). Call this before adding a record so you use an
     * account name that exists.
     *
     * @return The accounts that records can be added to.
     */
    @AppFunctionDeclaration(isEnabled = false, isDescribedByKDoc = true)
    suspend fun listAccounts(): AccountList = guard { functions.listAccounts() }

    /**
     * Lists the user's spending and income categories. Call this before adding a record so you use a category name that
     * exists.
     *
     * @return The categories that records can be filed under.
     */
    @AppFunctionDeclaration(isEnabled = false, isDescribedByKDoc = true)
    suspend fun listCategories(): CategoryList = guard { functions.listCategories() }

    /**
     * Lists the filters the user saved in OssWallet. A saved filter is a named set of conditions (dates, categories,
     * tags, amounts) that can be used to search or analyse.
     *
     * @return The saved filters, with a one-line description of each.
     */
    @AppFunctionDeclaration(isEnabled = false, isDescribedByKDoc = true)
    suspend fun listSavedFilters(): SavedFilterList = guard { functions.listSavedFilters() }

    /**
     * Adds one new expense or income record to the user's finances. This changes the user's data, so only call it when
     * the user has clearly asked for the record to be added, and tell them what was recorded afterwards. It can only
     * add, never edit or delete. Asking twice for the same record just returns the existing one.
     *
     * @param params What to record.
     * @return The record that was created.
     */
    @AppFunctionDeclaration(isEnabled = false, isDescribedByKDoc = true)
    suspend fun createTransaction(params: CreateTransactionParams): CreatedTransaction =
        guard { functions.createTransaction(params) }

    /**
     * Searches the user's transactions by words, tag, amount, period, account, type or a saved filter, newest first.
     * Returns at most 50 transactions and the total number that match. Use it to answer "when did I last pay X" or
     * "show my biggest expenses this year".
     *
     * @param params What to look for.
     * @return The matching transactions.
     */
    @AppFunctionDeclaration(isEnabled = false, isDescribedByKDoc = true)
    suspend fun searchTransactions(params: SearchTransactionsParams): TransactionSearchResult =
        guard { functions.searchTransactions(params) }

    /**
     * Analyses the user's income and spending for a period, or for one of their saved filters: totals per currency (with
     * the change from the previous period), spending broken down by category, account, tag or payment method, and the
     * trend over time. Currencies are never added together. Can also draw a chart image of the trend.
     *
     * @param params What to analyse.
     * @return The statistics, ready to summarise or chart.
     */
    @AppFunctionDeclaration(isEnabled = false, isDescribedByKDoc = true)
    suspend fun getStatistics(params: StatisticsParams): StatisticsResult = guard { functions.statistics(params) }

    private suspend fun <T> guard(block: suspend () -> T): T = try {
        block()
    } catch (e: CancellationException) {
        throw e
    } catch (e: AppFunctionException) {
        throw e
    } catch (e: AssistantException) {
        throw when (e.kind) {
            AssistantException.Kind.INVALID_ARGUMENT -> AppFunctionInvalidArgumentException(e.message)
            AssistantException.Kind.NOT_FOUND -> AppFunctionElementNotFoundException(e.message)
            AssistantException.Kind.DENIED -> AppFunctionDeniedException(e.message)
            AssistantException.Kind.DISABLED -> AppFunctionDisabledException(e.message)
        }
    } catch (e: Exception) {
        // Details of an unexpected failure stay on the device (in its log), never in the reply.
        android.util.Log.e("OssWalletAssistant", "An assistant request failed", e)
        throw AppFunctionAppUnknownException("OssWallet couldn't complete that request.")
    }
}
