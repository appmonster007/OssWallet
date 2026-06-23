package com.osscabinet.ossWallet.data.web.routes

import com.osscabinet.ossWallet.data.web.AuthSessionManager
import com.osscabinet.ossWallet.data.web.authenticate
import com.osscabinet.ossWallet.data.web.dto.*
import com.osscabinet.ossWallet.data.web.toDto
import com.osscabinet.ossWallet.domain.model.Account
import com.osscabinet.ossWallet.domain.model.AccountStructureType
import com.osscabinet.ossWallet.domain.model.Transaction
import com.osscabinet.ossWallet.domain.model.TransactionType
import com.osscabinet.ossWallet.domain.repository.AccountRepository
import com.osscabinet.ossWallet.domain.repository.CategoryRepository
import com.osscabinet.ossWallet.domain.repository.SettingsRepository
import com.osscabinet.ossWallet.domain.repository.TransactionRepository
import io.ktor.http.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import kotlinx.coroutines.flow.first
import java.math.BigDecimal

fun Route.accountRoutes(
    authSessionManager: AuthSessionManager,
    accountRepository: AccountRepository,
    transactionRepository: TransactionRepository,
    settingsRepository: SettingsRepository,
    categoryRepository: CategoryRepository
) {
    route("/accounts") {
        get {
            if (!authenticate(authSessionManager)) return@get
            try {
                val profileId = settingsRepository.currentProfileId.first()
                val allTransactions =
                    transactionRepository.getAllTransactionsSync(profileId)
                val accounts = accountRepository.getAllAccountsSync(profileId)

                val dtos = accounts.map { account ->
                    val balance = allTransactions
                        .filter { it.accountId == account.id }
                        .fold(BigDecimal.ZERO) { acc, tx ->
                            when (tx.transactionType) {
                                TransactionType.INCOME -> acc + tx.amount
                                TransactionType.EXPENSE -> acc - tx.amount
                                TransactionType.TRANSFER_INFLOW, TransactionType.TRANSFER_OUTFLOW -> acc + tx.amount
                            }
                        }
                    AccountDto(
                        id = account.id,
                        name = account.name,
                        currency = account.currencyCode,
                        balance = balance.toDouble(),
                        type = account.structureType.name
                    )
                }
                call.respond(ApiResponse(success = true, data = dtos))
            } catch (e: Exception) {
                call.respond(
                    HttpStatusCode.InternalServerError,
                    ApiResponse<Unit>(
                        success = false,
                        error = "Internal error"
                    )
                )
            }
        }

        post {
            if (!authenticate(authSessionManager)) return@post
            try {
                val profileId = settingsRepository.currentProfileId.first()
                val req = call.receive<CreateAccountRequest>()
                val account = Account(
                    id = java.util.UUID.randomUUID().toString(),
                    name = req.name,
                    currencyCode = req.currencyCode,
                    structureType = AccountStructureType.entries.find {
                        it.name.equals(
                            req.structureType,
                            ignoreCase = true
                        )
                    }
                        ?: AccountStructureType.SAVINGS,
                    colorHex = req.colorHex,
                    icon = req.icon,
                    allowedCurrencies = req.allowedCurrencies
                )
                accountRepository.insertAccount(account, profileId)
                if (req.initialBalance != null && req.initialBalance != 0.0) {
                    val initTx = Transaction(
                        name = "Initial Balance",
                        amount = BigDecimal.valueOf(kotlin.math.abs(req.initialBalance)),
                        currency = account.currencyCode,
                        transactionType = if (req.initialBalance >= 0) TransactionType.INCOME else TransactionType.EXPENSE,
                        accountId = account.id,
                        categoryId = 0L,
                        timestamp = System.currentTimeMillis()
                    )
                    transactionRepository.insertTransaction(initTx, profileId)
                }
                val allTransactions =
                    transactionRepository.getAllTransactionsSync(profileId)
                val balance = allTransactions.filter { it.accountId == account.id }
                    .fold(BigDecimal.ZERO) { acc, tx ->
                        when (tx.transactionType) {
                            TransactionType.INCOME -> acc + tx.amount
                            TransactionType.EXPENSE -> acc - tx.amount
                            TransactionType.TRANSFER_INFLOW, TransactionType.TRANSFER_OUTFLOW -> acc + tx.amount
                        }
                    }
                call.respond(
                    HttpStatusCode.Created, ApiResponse(
                        success = true, data = AccountDto(
                            id = account.id,
                            name = account.name,
                            currency = account.currencyCode,
                            balance = balance.toDouble(),
                            type = account.structureType.name
                        )
                    )
                )
            } catch (e: Exception) {
                call.respond(
                    HttpStatusCode.InternalServerError,
                    ApiResponse<Unit>(
                        success = false,
                        error = "Internal error"
                    )
                )
            }
        }

        get("/{id}") {
            if (!authenticate(authSessionManager)) return@get
            try {
                val profileId = settingsRepository.currentProfileId.first()
                val id = call.parameters["id"]
                    ?: return@get call.respond(HttpStatusCode.BadRequest, ApiResponse<Unit>(success = false, error = "Invalid id"))
                val account = accountRepository.getAllAccountsSync(profileId).find { it.id == id }
                    ?: return@get call.respond(HttpStatusCode.NotFound, ApiResponse<Unit>(success = false, error = "Account not found"))
                val allTransactions = transactionRepository.getAllTransactionsSync(profileId)
                val txForAccount = allTransactions.filter { it.accountId == account.id }
                val balance = txForAccount.fold(BigDecimal.ZERO) { acc, tx ->
                    when (tx.transactionType) {
                        TransactionType.INCOME -> acc + tx.amount
                        TransactionType.EXPENSE -> acc - tx.amount
                        TransactionType.TRANSFER_INFLOW, TransactionType.TRANSFER_OUTFLOW -> acc + tx.amount
                    }
                }
                val categoryMap = categoryRepository.getAllCategoriesSync().associateBy { it.id }
                val accountMap = accountRepository.getAllAccountsSync(profileId).associateBy { it.id }
                val recentTx = txForAccount.sortedByDescending { it.timestamp }.take(20).map { it.toDto(categoryMap, accountMap) }
                call.respond(ApiResponse(success = true, data = mapOf(
                    "account" to AccountDto(id = account.id, name = account.name, currency = account.currencyCode, balance = balance.toDouble(), type = account.structureType.name),
                    "recentTransactions" to recentTx
                )))
            } catch (e: Exception) {
                call.respond(HttpStatusCode.InternalServerError, ApiResponse<Unit>(success = false, error = e.message ?: "Internal error"))
            }
        }

        put("/{id}") {
            if (!authenticate(authSessionManager)) return@put
            try {
                val profileId = settingsRepository.currentProfileId.first()
                val id = call.parameters["id"]
                    ?: return@put call.respond(
                        HttpStatusCode.BadRequest,
                        ApiResponse<Unit>(success = false, error = "Invalid id")
                    )
                val existing =
                    accountRepository.getAllAccountsSync(profileId).find { it.id == id }
                        ?: return@put call.respond(
                            HttpStatusCode.NotFound,
                            ApiResponse<Unit>(success = false, error = "Account not found")
                        )
                val req = call.receive<UpdateAccountRequest>()
                val updated = existing.copy(
                    name = req.name ?: existing.name,
                    currencyCode = req.currencyCode ?: existing.currencyCode,
                    structureType = req.structureType?.let { t ->
                        AccountStructureType.entries.find {
                            it.name.equals(
                                t,
                                ignoreCase = true
                            )
                        } ?: existing.structureType
                    } ?: existing.structureType,
                    colorHex = req.colorHex ?: existing.colorHex,
                    icon = req.icon ?: existing.icon,
                    isArchived = req.isArchived ?: existing.isArchived,
                    allowedCurrencies = req.allowedCurrencies ?: existing.allowedCurrencies
                )
                accountRepository.updateAccount(updated, profileId)
                val allTransactions =
                    transactionRepository.getAllTransactionsSync(profileId)
                val balance = allTransactions.filter { it.accountId == updated.id }
                    .fold(BigDecimal.ZERO) { acc, tx ->
                        when (tx.transactionType) {
                            TransactionType.INCOME -> acc + tx.amount
                            TransactionType.EXPENSE -> acc - tx.amount
                            TransactionType.TRANSFER_INFLOW, TransactionType.TRANSFER_OUTFLOW -> acc + tx.amount
                        }
                    }
                call.respond(
                    ApiResponse(
                        success = true, data = AccountDto(
                            id = updated.id,
                            name = updated.name,
                            currency = updated.currencyCode,
                            balance = balance.toDouble(),
                            type = updated.structureType.name
                        )
                    )
                )
            } catch (e: Exception) {
                call.respond(
                    HttpStatusCode.InternalServerError,
                    ApiResponse<Unit>(
                        success = false,
                        error = "Internal error"
                    )
                )
            }
        }

        delete("/{id}") {
            if (!authenticate(authSessionManager)) return@delete
            try {
                val id = call.parameters["id"]
                    ?: return@delete call.respond(
                        HttpStatusCode.BadRequest,
                        ApiResponse<Unit>(success = false, error = "Invalid id")
                    )
                val profileId = settingsRepository.currentProfileId.first()
                accountRepository.deleteAccount(id, profileId)
                call.respond(ApiResponse(success = true, data = mapOf("deleted" to id)))
            } catch (e: Exception) {
                call.respond(
                    HttpStatusCode.InternalServerError,
                    ApiResponse<Unit>(
                        success = false,
                        error = "Internal error"
                    )
                )
            }
        }

        put("/reorder") {
            if (!authenticate(authSessionManager)) return@put
            try {
                val profileId = settingsRepository.currentProfileId.first()
                val req = call.receive<ReorderAccountsRequest>()
                val allAccounts = accountRepository.getAllAccountsSync(profileId)
                val accountMap = allAccounts.associateBy { it.id }
                val reordered = req.accountIds.mapNotNull { accountMap[it] }
                accountRepository.updateAccounts(reordered, profileId)
                call.respond(
                    ApiResponse(
                        success = true,
                        data = mapOf("reordered" to reordered.size)
                    )
                )
            } catch (e: Exception) {
                call.respond(
                    HttpStatusCode.InternalServerError,
                    ApiResponse<Unit>(
                        success = false,
                        error = "Internal error"
                    )
                )
            }
        }
    }
}
