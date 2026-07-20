package com.osscabinet.ossWallet.appfunctions

import androidx.appfunctions.AppFunctionContext
import androidx.appfunctions.service.AppFunction
import com.osscabinet.ossWallet.domain.model.Category
import com.osscabinet.ossWallet.domain.model.EntryMode
import com.osscabinet.ossWallet.domain.model.PaymentMethod
import com.osscabinet.ossWallet.domain.model.Transaction
import com.osscabinet.ossWallet.domain.model.TransactionStatus
import com.osscabinet.ossWallet.domain.model.TransactionType
import com.osscabinet.ossWallet.domain.model.dto.TransactionRequest
import com.osscabinet.ossWallet.domain.model.toEntryMode
import com.osscabinet.ossWallet.domain.repository.AccountRepository
import com.osscabinet.ossWallet.domain.repository.CategoryRepository
import com.osscabinet.ossWallet.domain.repository.SettingsRepository
import com.osscabinet.ossWallet.domain.repository.TransactionRepository
import com.osscabinet.ossWallet.domain.service.management.TransactionManager
import jakarta.inject.Inject
import jakarta.inject.Singleton
import kotlinx.coroutines.flow.first
import java.math.BigDecimal
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeParseException
import java.time.temporal.TemporalAdjusters

@Singleton
class TransactionAppFunctions @Inject constructor(
    private val transactionManager: TransactionManager,
    private val transactionRepository: TransactionRepository,
    private val accountRepository: AccountRepository,
    private val categoryRepository: CategoryRepository,
    private val settingsRepository: SettingsRepository
) {

    /**
     * Creates a new financial transaction in OssWallet.
     * Supports expense, income, and transfer types with full metadata:
     * category, account, tags, payment method, date, payee, and note.
     * For transfers, specify targetAccountName; null or blank means outside-of-wallet.
     */
    @AppFunction(isDescribedByKDoc = true)
    suspend fun createTransaction(
        ctx: AppFunctionContext,
        params: CreateTransactionParams
    ): TransactionResult {
        return try {
            val profileId = settingsRepository.currentProfileId.first()
            val accounts = accountRepository.getAllAccounts(profileId).first()
            val categories = categoryRepository.getAllCategories(profileId).first()

            val entryMode = parseEntryMode(params.entryMode)
                ?: return TransactionResult(
                    success = false,
                    errorMessage = "Invalid entryMode '${params.entryMode}'. Use: expense, income, transfer"
                )

            val rawAmount = params.amount.toBigDecimalOrNull()
                ?: return TransactionResult(success = false, errorMessage = "Invalid amount '${params.amount}'")

            val amount = when (entryMode) {
                EntryMode.EXPENSE -> rawAmount.negate()
                EntryMode.INCOME -> rawAmount
                EntryMode.TRANSFER -> rawAmount
            }

            val accountNameStr = params.accountName?.trim().orEmpty()
            val account = if (accountNameStr.isNotBlank()) {
                accounts.find { it.name.equals(accountNameStr, ignoreCase = true) }
                    ?: return TransactionResult(
                        success = false,
                        errorMessage = "Account '$accountNameStr' not found. Available: ${accounts.joinToString { it.name }}"
                    )
            } else null

            val targetNameStr = params.targetAccountName?.trim().orEmpty()
            val targetAccount = if (targetNameStr.isNotBlank()) {
                accounts.find { it.name.equals(targetNameStr, ignoreCase = true) }
                    ?: return TransactionResult(
                        success = false,
                        errorMessage = "Target account '$targetNameStr' not found. Available: ${accounts.joinToString { it.name }}"
                    )
            } else null

            val categoryNameStr = params.categoryName?.trim().orEmpty()
            val category = categories.find { it.name.equals(categoryNameStr, ignoreCase = true) }
            val categoryId = category?.id ?: Category.SYSTEM_OTHERS

            val currency = when {
                !params.currency.isNullOrBlank() -> params.currency.trim()
                account != null -> account.currencyCode
                else -> return TransactionResult(
                    success = false,
                    errorMessage = "Currency is required when no account is specified"
                )
            }

            val targetCurrency = when {
                !params.targetCurrency.isNullOrBlank() -> params.targetCurrency.trim()
                targetAccount != null -> targetAccount.currencyCode
                else -> currency
            }

            val dateStr = params.date?.trim().orEmpty()
            val timestamp = if (dateStr.isBlank()) {
                System.currentTimeMillis()
            } else {
                parseDate(dateStr)
                    ?: return TransactionResult(
                        success = false,
                        errorMessage = "Invalid date '$dateStr'. Use ISO-8601 format: YYYY-MM-DD"
                    )
            }

            val tags = params.tags?.toTagSet() ?: emptySet()
            val name = params.name?.trim()?.ifBlank { null } ?: category?.name ?: "Transaction"

            val request = TransactionRequest(
                name = name,
                amount = amount,
                entryMode = entryMode,
                accountId = account?.id,
                currency = currency,
                categoryId = categoryId,
                tags = tags,
                paymentMethod = parsePaymentMethod(params.paymentMethod),
                timestamp = timestamp,
                payee = params.payee?.trim().orEmpty(),
                note = params.note?.trim().orEmpty(),
                status = parseStatus(params.status),
                targetAccountId = if (entryMode == EntryMode.TRANSFER) targetAccount?.id ?: "" else null,
                targetCurrency = if (entryMode == EntryMode.TRANSFER) targetCurrency else null,
                transferRate = params.transferRate?.toBigDecimalOrNull() ?: BigDecimal.ONE
            )

            val newId = transactionManager.handleSaveTransaction(
                existingTransaction = null,
                request = request,
                profileId = profileId
            )

            TransactionResult(
                success = true,
                transactionId = newId,
                name = name,
                amount = rawAmount.toPlainString(),
                currency = currency,
                type = entryMode.value,
                accountName = account?.name,
                categoryName = category?.name ?: "Others",
                date = epochMillisToDateString(timestamp),
                note = params.note?.trim(),
                payee = params.payee?.trim()
            )
        } catch (e: Exception) {
            TransactionResult(success = false, errorMessage = e.message ?: "Unknown error")
        }
    }

    /**
     * Returns a list of transactions matching the given filters.
     * Supports filtering by type (expense/income/transfer), account, category, and time period.
     * Results are sorted by date descending and capped by the limit parameter.
     */
    @AppFunction(isDescribedByKDoc = true)
    suspend fun getTransactions(
        ctx: AppFunctionContext,
        params: GetTransactionsParams
    ): TransactionListResult {
        return try {
            val profileId = settingsRepository.currentProfileId.first()
            val accounts = accountRepository.getAllAccounts(profileId).first()
            val categories = categoryRepository.getAllCategories(profileId).first()

            val accountNameStr = params.accountName?.trim().orEmpty()
            val filterAccountId = if (accountNameStr.isNotBlank()) {
                accounts.find { it.name.equals(accountNameStr, ignoreCase = true) }?.id
                    ?: return TransactionListResult(
                        success = false,
                        errorMessage = "Account '$accountNameStr' not found"
                    )
            } else null

            val categoryNameStr = params.categoryName?.trim().orEmpty()
            val filterCategoryId = if (categoryNameStr.isNotBlank()) {
                categories.find { it.name.equals(categoryNameStr, ignoreCase = true) }?.id
                    ?: return TransactionListResult(
                        success = false,
                        errorMessage = "Category '$categoryNameStr' not found"
                    )
            } else null

            val (periodStart, periodEnd) = parseTimePeriod(params.timePeriod)
            val limit = params.limit.coerceAtLeast(1).coerceAtMost(500)
            val typeFilter = params.type?.trim().orEmpty()

            var transactions = transactionRepository.getAllTransactions(profileId).first()

            if (typeFilter.isNotBlank()) {
                transactions = transactions.filter { tx ->
                    when (typeFilter.lowercase()) {
                        "expense" -> tx.transactionType == TransactionType.EXPENSE
                        "income" -> tx.transactionType == TransactionType.INCOME
                        "transfer" -> tx.transactionType == TransactionType.TRANSFER_INFLOW
                                || tx.transactionType == TransactionType.TRANSFER_OUTFLOW
                        else -> true
                    }
                }
            }
            if (filterAccountId != null) {
                transactions = transactions.filter { it.accountId == filterAccountId }
            }
            if (filterCategoryId != null) {
                transactions = transactions.filter { it.categoryId == filterCategoryId }
            }
            if (periodStart != null && periodEnd != null) {
                transactions = transactions.filter { it.timestamp in periodStart..periodEnd }
            }

            val accountMap = accounts.associateBy { it.id }
            val categoryMap = categories.associateBy { it.id }

            val results = transactions
                .sortedByDescending { it.timestamp }
                .take(limit)
                .map { tx ->
                    tx.toResult(
                        accountName = accountMap[tx.accountId]?.name,
                        categoryName = categoryMap[tx.categoryId]?.name
                    )
                }

            TransactionListResult(success = true, transactions = results, totalCount = results.size)
        } catch (e: Exception) {
            TransactionListResult(success = false, errorMessage = e.message ?: "Unknown error")
        }
    }

    /**
     * Updates an existing transaction by ID.
     * Only the fields you supply will be changed; null fields keep their current values.
     * Supports changing type, amount, account, category, date, tags, and all metadata fields.
     */
    @AppFunction(isDescribedByKDoc = true)
    suspend fun updateTransaction(
        ctx: AppFunctionContext,
        params: UpdateTransactionParams
    ): TransactionResult {
        return try {
            val profileId = settingsRepository.currentProfileId.first()
            val existing = transactionRepository.getTransactionById(params.transactionId, profileId)
                ?: return TransactionResult(
                    success = false,
                    errorMessage = "Transaction ${params.transactionId} not found"
                )

            val accounts = accountRepository.getAllAccounts(profileId).first()
            val categories = categoryRepository.getAllCategories(profileId).first()

            val existingEntryMode = existing.transactionType.toEntryMode()
            val entryMode = if (!params.entryMode.isNullOrBlank()) {
                parseEntryMode(params.entryMode)
                    ?: return TransactionResult(success = false, errorMessage = "Invalid entryMode '${params.entryMode}'")
            } else existingEntryMode

            val existingAbsAmount = existing.amount.abs()
            val rawAmount = if (!params.amount.isNullOrBlank()) {
                params.amount.toBigDecimalOrNull()
                    ?: return TransactionResult(success = false, errorMessage = "Invalid amount '${params.amount}'")
            } else existingAbsAmount

            val amount = when (entryMode) {
                EntryMode.EXPENSE -> rawAmount.negate()
                EntryMode.INCOME -> rawAmount
                EntryMode.TRANSFER -> rawAmount
            }

            val accountNameStr = params.accountName?.trim().orEmpty()
            val account = if (accountNameStr.isNotBlank()) {
                accounts.find { it.name.equals(accountNameStr, ignoreCase = true) }
                    ?: return TransactionResult(success = false, errorMessage = "Account '$accountNameStr' not found")
            } else null
            val accountId = account?.id ?: existing.accountId

            val targetNameStr = params.targetAccountName?.trim().orEmpty()
            val targetAccount = if (targetNameStr.isNotBlank()) {
                accounts.find { it.name.equals(targetNameStr, ignoreCase = true) }
            } else null
            val targetAccountId = targetAccount?.id ?: existing.targetAccountId

            val categoryNameStr = params.categoryName?.trim().orEmpty()
            val category = if (categoryNameStr.isNotBlank()) {
                categories.find { it.name.equals(categoryNameStr, ignoreCase = true) }
            } else null
            val categoryId = category?.id ?: existing.categoryId

            val currency = when {
                !params.currency.isNullOrBlank() -> params.currency.trim()
                account != null -> account.currencyCode
                else -> existing.currency
            }

            val targetCurrency = when {
                !params.targetCurrency.isNullOrBlank() -> params.targetCurrency.trim()
                targetAccount != null -> targetAccount.currencyCode
                else -> existing.targetCurrency
            }

            val dateStr = params.date?.trim().orEmpty()
            val timestamp = if (dateStr.isNotBlank()) {
                parseDate(dateStr) ?: return TransactionResult(success = false, errorMessage = "Invalid date '$dateStr'")
            } else existing.timestamp

            val tags = if (!params.tags.isNullOrBlank()) params.tags.toTagSet() else existing.tags
            val name = params.name?.trim()?.ifBlank { null } ?: existing.name
            val note = params.note?.trim() ?: existing.note
            val payee = params.payee?.trim() ?: existing.payee
            val paymentMethod = if (!params.paymentMethod.isNullOrBlank()) parsePaymentMethod(params.paymentMethod) else existing.paymentMethod
            val status = if (!params.status.isNullOrBlank()) parseStatus(params.status) else existing.status
            val transferRate = params.transferRate?.toBigDecimalOrNull() ?: existing.transferRate

            val request = TransactionRequest(
                name = name,
                amount = amount,
                entryMode = entryMode,
                accountId = accountId,
                currency = currency,
                categoryId = categoryId,
                tags = tags,
                paymentMethod = paymentMethod,
                timestamp = timestamp,
                payee = payee,
                note = note,
                status = status,
                targetAccountId = targetAccountId,
                targetCurrency = targetCurrency,
                transferRate = transferRate
            )

            transactionManager.handleSaveTransaction(
                existingTransaction = existing,
                request = request,
                profileId = profileId
            )

            val accountName = accounts.find { it.id == accountId }?.name
            val categoryName = categories.find { it.id == categoryId }?.name

            TransactionResult(
                success = true,
                transactionId = existing.id,
                name = name,
                amount = rawAmount.toPlainString(),
                currency = currency,
                type = entryMode.value,
                accountName = accountName,
                categoryName = categoryName,
                date = epochMillisToDateString(timestamp),
                note = note,
                payee = payee
            )
        } catch (e: Exception) {
            TransactionResult(success = false, errorMessage = e.message ?: "Unknown error")
        }
    }

    /**
     * Deletes a transaction by ID.
     * For linked internal transfers, the counterpart leg is decoupled (not deleted)
     * and becomes a standalone outside-wallet transfer.
     */
    @AppFunction(isDescribedByKDoc = true)
    suspend fun deleteTransaction(
        ctx: AppFunctionContext,
        params: DeleteByIdParams
    ): SuccessResult {
        return try {
            val profileId = settingsRepository.currentProfileId.first()
            transactionManager.deleteTransaction(params.id, profileId)
            SuccessResult(success = true)
        } catch (e: Exception) {
            SuccessResult(success = false, errorMessage = e.message ?: "Unknown error")
        }
    }

    // --- Private helpers ---

    private fun parseEntryMode(value: String): EntryMode? = when (value.lowercase().trim()) {
        "expense" -> EntryMode.EXPENSE
        "income" -> EntryMode.INCOME
        "transfer" -> EntryMode.TRANSFER
        else -> null
    }

    private fun parsePaymentMethod(value: String?): PaymentMethod = when (value?.lowercase()?.trim()) {
        "credit_card" -> PaymentMethod.CREDIT_CARD
        "debit_card" -> PaymentMethod.DEBIT_CARD
        "bank_transfer" -> PaymentMethod.BANK_TRANSFER
        "digital_wallet" -> PaymentMethod.DIGITAL_WALLET
        "mobile_payment" -> PaymentMethod.MOBILE_PAYMENT
        "check" -> PaymentMethod.CHECK
        "cryptocurrency" -> PaymentMethod.CRYPTOCURRENCY
        "other" -> PaymentMethod.OTHER
        else -> PaymentMethod.CASH
    }

    private fun parseStatus(value: String?): TransactionStatus = when (value?.lowercase()?.trim()) {
        "uncleared" -> TransactionStatus.UNCLEARED
        "reconcile" -> TransactionStatus.RECONCILE
        else -> TransactionStatus.CLEARED
    }

    private fun parseDate(dateStr: String): Long? {
        return try {
            LocalDate.parse(dateStr)
                .atStartOfDay(ZoneId.systemDefault())
                .toInstant()
                .toEpochMilli()
        } catch (e: DateTimeParseException) {
            null
        }
    }

    private fun parseTimePeriod(period: String?): Pair<Long?, Long?> {
        val zone = ZoneId.systemDefault()
        val today = LocalDate.now(zone)
        return when (period?.lowercase()?.trim()) {
            "today" -> {
                val start = today.atStartOfDay(zone).toInstant().toEpochMilli()
                val end = today.plusDays(1).atStartOfDay(zone).toInstant().toEpochMilli() - 1
                start to end
            }
            "this_week" -> {
                val start = today.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
                    .atStartOfDay(zone).toInstant().toEpochMilli()
                val end = today.with(TemporalAdjusters.nextOrSame(DayOfWeek.SUNDAY))
                    .plusDays(1).atStartOfDay(zone).toInstant().toEpochMilli() - 1
                start to end
            }
            "this_month" -> {
                val start = today.withDayOfMonth(1).atStartOfDay(zone).toInstant().toEpochMilli()
                val end = today.withDayOfMonth(today.lengthOfMonth())
                    .plusDays(1).atStartOfDay(zone).toInstant().toEpochMilli() - 1
                start to end
            }
            "this_year" -> {
                val start = today.withDayOfYear(1).atStartOfDay(zone).toInstant().toEpochMilli()
                val end = today.withDayOfYear(today.lengthOfYear())
                    .plusDays(1).atStartOfDay(zone).toInstant().toEpochMilli() - 1
                start to end
            }
            else -> null to null
        }
    }

    private fun epochMillisToDateString(millis: Long): String =
        LocalDate.ofEpochDay(millis / 86_400_000L).toString()

    private fun String.toTagSet(): Set<String> =
        split(",").map { it.trim() }.filter { it.isNotBlank() }.toSet()

    private fun Transaction.toResult(accountName: String?, categoryName: String?) = TransactionResult(
        success = true,
        transactionId = id,
        name = name,
        amount = amount.abs().toPlainString(),
        currency = currency,
        type = transactionType.value,
        accountName = accountName,
        categoryName = categoryName,
        date = epochMillisToDateString(timestamp),
        note = note,
        payee = payee
    )
}
