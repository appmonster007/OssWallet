package com.osscabinet.ossWallet.data.web.routes

import com.osscabinet.ossWallet.data.web.AuthSessionManager
import com.osscabinet.ossWallet.data.web.authenticate
import com.osscabinet.ossWallet.data.web.dto.*
import com.osscabinet.ossWallet.data.web.toDto
import com.osscabinet.ossWallet.domain.model.AccountStructureType
import com.osscabinet.ossWallet.domain.model.TransactionType
import com.osscabinet.ossWallet.domain.repository.AccountRepository
import com.osscabinet.ossWallet.domain.repository.CategoryRepository
import com.osscabinet.ossWallet.domain.repository.SettingsRepository
import com.osscabinet.ossWallet.domain.repository.TransactionRepository
import com.osscabinet.ossWallet.domain.service.management.TransactionManager
import com.osscabinet.ossWallet.domain.usecase.AccountUseCases
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
    categoryRepository: CategoryRepository,
    accountUseCases: AccountUseCases
) {
    route("/accounts") {
        get {
            if (!authenticate(authSessionManager)) return@get
            try {
                val profileId = settingsRepository.currentProfileId.first()
                val allTransactions =
                    transactionRepository.getAllTransactionsSync(profileId)
                val accounts = accountRepository.getAllAccountsSync(profileId)

                val txByAccount = allTransactions.groupBy { it.accountId }
                val dtos = accounts.map { account ->
                    val txs = txByAccount[account.id] ?: emptyList()
                    val balance = txs.fold(BigDecimal.ZERO) { acc, tx -> acc + tx.amount }
                    val income = txs.filter { it.transactionType == TransactionType.INCOME }
                        .fold(BigDecimal.ZERO) { a, tx -> a + tx.amount.abs() }
                    val expense = txs.filter { it.transactionType == TransactionType.EXPENSE }
                        .fold(BigDecimal.ZERO) { a, tx -> a + tx.amount.abs() }
                    val transferIn = txs.filter { it.transactionType == TransactionType.TRANSFER_INFLOW }
                        .fold(BigDecimal.ZERO) { a, tx -> a + tx.amount.abs() }
                    val transferOut = txs.filter { it.transactionType == TransactionType.TRANSFER_OUTFLOW }
                        .fold(BigDecimal.ZERO) { a, tx -> a + tx.amount.abs() }
                    val distribution = txs.groupBy { it.currency }
                        .mapValues { (_, cTxs) ->
                            cTxs.fold(BigDecimal.ZERO) { a, tx -> a + tx.amount }.toDouble()
                        }.filter { it.value != 0.0 }
                    AccountDto(
                        id = account.id,
                        name = account.name,
                        currency = account.currencyCode,
                        balance = balance.toDouble(),
                        type = account.structureType.name,
                        colorHex = account.colorHex,
                        income = income.toDouble(),
                        expense = expense.toDouble(),
                        transferIn = transferIn.toDouble(),
                        transferOut = transferOut.toDouble(),
                        distribution = distribution
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
                val accountId = accountUseCases.createAccountWithInitialBalances(
                    name = req.name,
                    initialBalances = if (req.initialBalance != null && req.initialBalance != 0.0)
                        mapOf(req.currencyCode to req.initialBalance.toString()) else emptyMap(),
                    currency = req.currencyCode,
                    structureType = AccountStructureType.fromString(req.structureType),
                    colorHex = req.colorHex,
                    icon = req.icon,
                    allowedCurrencies = req.allowedCurrencies,
                    profileId = profileId
                )

                call.respond(
                    HttpStatusCode.Created, ApiResponse(
                        success = true, data = AccountDto(
                            id = accountId,
                            name = req.name,
                            currency = req.currencyCode,
                            balance = req.initialBalance ?: 0.0,
                            type = req.structureType
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
                val balance = txForAccount.fold(BigDecimal.ZERO) { acc, tx -> acc + tx.amount }
                val income = txForAccount.filter { it.transactionType == TransactionType.INCOME }
                    .fold(BigDecimal.ZERO) { a, tx -> a + tx.amount.abs() }
                val expense = txForAccount.filter { it.transactionType == TransactionType.EXPENSE }
                    .fold(BigDecimal.ZERO) { a, tx -> a + tx.amount.abs() }
                val transferIn = txForAccount.filter { it.transactionType == TransactionType.TRANSFER_INFLOW }
                    .fold(BigDecimal.ZERO) { a, tx -> a + tx.amount.abs() }
                val transferOut = txForAccount.filter { it.transactionType == TransactionType.TRANSFER_OUTFLOW }
                    .fold(BigDecimal.ZERO) { a, tx -> a + tx.amount.abs() }
                val distribution = txForAccount.groupBy { it.currency }
                    .mapValues { (_, cTxs) ->
                        cTxs.fold(BigDecimal.ZERO) { a, tx -> a + tx.amount }.toDouble()
                    }.filter { it.value != 0.0 }
                val categoryMap = categoryRepository.getAllCategoriesSync().associateBy { it.id }
                val accountMap = accountRepository.getAllAccountsSync(profileId).associateBy { it.id }
                val recentTx = txForAccount.sortedByDescending { it.timestamp }.take(20).map { it.toDto(categoryMap, accountMap) }
                val accountDto = AccountDto(
                    id = account.id, name = account.name, currency = account.currencyCode,
                    balance = balance.toDouble(), type = account.structureType.name,
                    colorHex = account.colorHex,
                    income = income.toDouble(), expense = expense.toDouble(),
                    transferIn = transferIn.toDouble(), transferOut = transferOut.toDouble(),
                    distribution = distribution
                )
                call.respond(ApiResponse(success = true, data = AccountDetailDto(
                    account = accountDto,
                    recentTransactions = recentTx
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
                    structureType = req.structureType?.let { AccountStructureType.fromString(it) } ?: existing.structureType,
                    colorHex = req.colorHex ?: existing.colorHex,
                    icon = req.icon ?: existing.icon,
                    isArchived = req.isArchived ?: existing.isArchived,
                    allowedCurrencies = req.allowedCurrencies ?: existing.allowedCurrencies
                )
                accountRepository.updateAccount(updated, profileId)
                val allTransactions =
                    transactionRepository.getAllTransactionsSync(profileId)
                val balance = allTransactions.filter { it.accountId == updated.id }
                    .fold(BigDecimal.ZERO) { acc, tx -> acc + tx.amount }
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
