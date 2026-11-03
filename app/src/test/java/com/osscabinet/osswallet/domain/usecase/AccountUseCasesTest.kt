package com.osscabinet.ossWallet.domain.usecase

import com.osscabinet.ossWallet.domain.model.Account
import com.osscabinet.ossWallet.domain.model.AccountStructureType
import com.osscabinet.ossWallet.domain.model.Category
import com.osscabinet.ossWallet.domain.model.CreditCardDetails
import com.osscabinet.ossWallet.domain.model.EntryMode
import com.osscabinet.ossWallet.domain.model.Transaction
import com.osscabinet.ossWallet.domain.model.TransactionType
import com.osscabinet.ossWallet.domain.model.dto.TransactionRequest
import com.osscabinet.ossWallet.domain.repository.AccountRepository
import com.osscabinet.ossWallet.domain.service.management.TransactionManager
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.math.BigDecimal

class AccountUseCasesTest {

    private val accountId = "acc1"
    private val profileId = 1
    private val dayMillis = 24L * 60 * 60 * 1000

    private val accountRepository: AccountRepository = mockk(relaxed = true)
    private val transactionManager: TransactionManager = mockk(relaxed = true)
    private val transactionUseCases: TransactionUseCases = mockk()

    private lateinit var useCases: AccountUseCases

    @Before
    fun setup() {
        every { transactionManager.transactionUseCases } returns transactionUseCases
        // Run the atomic blocks inline; the atomicity itself is covered by the instrumented rollback test.
        coEvery { transactionUseCases.runAtomically<Any?>(any()) } coAnswers {
            firstArg<suspend () -> Any?>().invoke()
        }
        coEvery { transactionUseCases.getInitialBalanceTransaction(any(), any(), any()) } returns null
        coEvery { transactionUseCases.getEarliestNonInitialBalanceTimestamp(any(), any()) } returns null
        coEvery { transactionManager.deleteTransaction(any(), any()) } returns Unit
        useCases = AccountUseCases(accountRepository, transactionManager)
    }

    private fun existingOpening(amount: String, timestamp: Long) = Transaction(
        id = 9L,
        name = "Initial Balance",
        amount = BigDecimal(amount),
        timestamp = timestamp,
        currency = "USD",
        transactionType = if (BigDecimal(amount).signum() < 0) TransactionType.EXPENSE else TransactionType.INCOME,
        accountId = accountId,
        categoryId = Category.SYSTEM_INITIAL_BALANCE
    )

    private fun stubEarliest(timestamp: Long?) {
        coEvery {
            transactionUseCases.getEarliestNonInitialBalanceTimestamp(accountId, profileId)
        } returns timestamp
    }

    private fun stubExisting(row: Transaction?) {
        coEvery {
            transactionUseCases.getInitialBalanceTransaction(accountId, "USD", profileId)
        } returns row
    }

    private suspend fun upsert(amount: String) =
        useCases.upsertInitialBalance(accountId, "USD", BigDecimal(amount), profileId)

    private suspend fun verifyNoSave() = coVerify(exactly = 0) {
        transactionManager.handleSaveTransaction(any(), any(), any(), any(), any(), any(), any())
    }

    // ── Balance adjustments: stored like an opening balance, on the reconciliation category ──

    @Test
    fun `gain adjustment is saved as income on the reconciliation category`() = runTest {
        val request = slot<TransactionRequest>()

        useCases.createBalanceAdjustment(accountId, "USD", BigDecimal("50.00"), profileId)

        coVerify {
            transactionManager.handleSaveTransaction(null, capture(request), profileId, any(), any(), any(), any())
        }
        assertEquals(EntryMode.INCOME, request.captured.entryMode)
        assertEquals(accountId, request.captured.accountId)
        assertEquals(null, request.captured.targetAccountId)
        assertEquals(Category.SYSTEM_RECONCILIATION_ADJUSTMENT, request.captured.categoryId)
        assertEquals(0, BigDecimal("50").compareTo(request.captured.amount))
    }

    @Test
    fun `loss adjustment is saved as expense with positive magnitude`() = runTest {
        val request = slot<TransactionRequest>()

        useCases.createBalanceAdjustment(accountId, "USD", BigDecimal("-30.00"), profileId)

        coVerify {
            transactionManager.handleSaveTransaction(null, capture(request), profileId, any(), any(), any(), any())
        }
        assertEquals(EntryMode.EXPENSE, request.captured.entryMode)
        assertEquals(accountId, request.captured.accountId)
        assertEquals(Category.SYSTEM_RECONCILIATION_ADJUSTMENT, request.captured.categoryId)
        assertEquals(0, BigDecimal("30").compareTo(request.captured.amount))
    }

    @Test
    fun `zero adjustment saves nothing`() = runTest {
        useCases.createBalanceAdjustment(accountId, "USD", BigDecimal.ZERO, profileId)

        verifyNoSave()
    }

    @Test
    fun `saving an account edit writes everything inside one atomic block`() = runTest {
        val account = Account(
            id = accountId,
            name = "Cash",
            currencyCode = "USD",
            structureType = AccountStructureType.CASH,
            colorHex = "#000000",
            icon = "ic",
            allowedCurrencies = listOf("USD")
        )

        useCases.saveAccountEdits(
            account = account,
            creditDetails = null,
            openingBalances = mapOf("USD" to BigDecimal("500")),
            pendingAdjustments = mapOf("USD" to BigDecimal("25")),
            profileId = profileId
        )

        coVerify(exactly = 1) { transactionUseCases.runAtomically<Any?>(any()) }
        coVerify { accountRepository.updateAccount(account, profileId) }
        coVerify {
            transactionManager.handleSaveTransaction(null, any(), profileId, any(), any(), any(), any())
        }
    }

    // ── Issue 10: a currency that holds a balance is always allowed on the account ──

    @Test
    fun `create adds a currency that holds an opening balance to the allowed list`() = runTest {
        useCases.createAccountWithInitialBalances(
            name = "Wallet", initialBalances = mapOf("USD" to "100", "EUR" to "50", "GBP" to "0"),
            currency = "USD", structureType = AccountStructureType.CASH, colorHex = "#000000", icon = "ic",
            allowedCurrencies = listOf("USD"), profileId = profileId
        )

        coVerify {
            accountRepository.insertAccount(
                match { it.allowedCurrencies.toSet() == setOf("USD", "EUR") },
                profileId
            )
        }
    }

    // ── Issue 7: account writes are one atomic unit ─────────────────────────

    @Test
    fun `create writes the account, credit details and opening balance in one atomic block`() = runTest {
        val credit = CreditCardDetails(
            accountId = "new-id",
            creditLimit = BigDecimal("5000"),
            statementClosingDay = 5,
            paymentDueDay = 25,
            interestRateApr = 18f
        )

        val id = useCases.createAccountWithInitialBalances(
            name = "Card", initialBalances = mapOf("USD" to "100"), currency = "USD",
            structureType = AccountStructureType.CREDIT, colorHex = "#000000", icon = "ic",
            allowedCurrencies = listOf("USD"), profileId = profileId,
            accountId = "new-id", creditDetails = credit
        )

        assertEquals("new-id", id)
        coVerify(exactly = 1) { transactionUseCases.runAtomically<Any?>(any()) }
        coVerify { accountRepository.insertAccount(match { it.id == "new-id" }, profileId) }
        coVerify { accountRepository.saveCreditCardDetails(credit) }
        coVerify { transactionManager.handleSaveTransaction(null, any(), profileId, any(), any(), any(), any()) }
    }

    @Test
    fun `a failure part-way through an account edit propagates so the atomic block rolls back`() = runTest {
        val account = Account(
            id = accountId,
            name = "Cash",
            currencyCode = "USD",
            structureType = AccountStructureType.CASH,
            colorHex = "#000000",
            icon = "ic",
            allowedCurrencies = listOf("USD")
        )
        coEvery {
            transactionManager.handleSaveTransaction(any(), any(), any(), any(), any(), any(), any())
        } throws IllegalStateException("disk full")

        val failure = runCatching {
            useCases.saveAccountEdits(
                account = account,
                creditDetails = null,
                openingBalances = mapOf("USD" to BigDecimal("500")),
                pendingAdjustments = emptyMap(),
                profileId = profileId
            )
        }.exceptionOrNull()

        // The real rollback is verified by the instrumented test; here we check the error escapes the block.
        assertTrue(failure is IllegalStateException)
        coVerify(exactly = 1) { transactionUseCases.runAtomically<Any?>(any()) }
    }

    // ── Create path ──────────────────────────────────────────────────────────

    @Test
    fun `create with positive balance saves income opening balance dated a day before now`() = runTest {
        val request = slot<TransactionRequest>()
        val before = System.currentTimeMillis()

        useCases.createAccountWithInitialBalances(
            name = "Cash", initialBalances = mapOf("USD" to "1000.00"), currency = "USD",
            structureType = AccountStructureType.CASH, colorHex = "#000000", icon = "ic",
            allowedCurrencies = listOf("USD"), profileId = profileId
        )
        val after = System.currentTimeMillis()

        coVerify {
            transactionManager.handleSaveTransaction(null, capture(request), profileId, any(), any(), any(), any())
        }
        assertEquals(EntryMode.INCOME, request.captured.entryMode)
        assertEquals(0, BigDecimal("1000").compareTo(request.captured.amount))
        assertEquals(Category.SYSTEM_INITIAL_BALANCE, request.captured.categoryId)
        assertTrue(request.captured.timestamp in (before - dayMillis)..(after - dayMillis))
    }

    @Test
    fun `create with negative balance saves expense opening balance with positive magnitude`() = runTest {
        val request = slot<TransactionRequest>()

        useCases.createAccountWithInitialBalances(
            name = "Card", initialBalances = mapOf("USD" to "-300.00"), currency = "USD",
            structureType = AccountStructureType.CREDIT, colorHex = "#000000", icon = "ic",
            allowedCurrencies = listOf("USD"), profileId = profileId
        )

        coVerify {
            transactionManager.handleSaveTransaction(null, capture(request), profileId, any(), any(), any(), any())
        }
        assertEquals(EntryMode.EXPENSE, request.captured.entryMode)
        assertEquals(0, BigDecimal("300").compareTo(request.captured.amount))
    }

    @Test
    fun `create with zero balance saves nothing`() = runTest {
        useCases.createAccountWithInitialBalances(
            name = "Empty", initialBalances = mapOf("USD" to "0"), currency = "USD",
            structureType = AccountStructureType.CASH, colorHex = "#000000", icon = "ic",
            allowedCurrencies = listOf("USD"), profileId = profileId
        )

        verifyNoSave()
    }

    // ── Upsert: timestamp rule (one day before the earliest non-opening-balance row) ──

    @Test
    fun `new opening balance is dated one day before the earliest other transaction`() = runTest {
        val earliest = 1_700_000_000_000L
        stubEarliest(earliest)
        stubExisting(null)
        val request = slot<TransactionRequest>()

        upsert("500.00")

        coVerify {
            transactionUseCases.getEarliestNonInitialBalanceTimestamp(accountId, profileId)
            transactionManager.handleSaveTransaction(null, capture(request), profileId, any(), any(), any(), any())
        }
        assertEquals(earliest - dayMillis, request.captured.timestamp)
    }

    @Test
    fun `changed amount moves an existing opening balance to one day before the earliest other transaction`() = runTest {
        val earliest = 1_700_000_000_000L
        stubEarliest(earliest)
        stubExisting(existingOpening("500.00", timestamp = earliest - 30 * dayMillis))
        val request = slot<TransactionRequest>()

        upsert("750.00")

        coVerify {
            transactionManager.handleSaveTransaction(any(), capture(request), profileId, any(), any(), any(), any())
        }
        assertEquals(earliest - dayMillis, request.captured.timestamp)
        assertEquals(0, BigDecimal("750").compareTo(request.captured.amount))
    }

    @Test
    fun `unchanged opening balance already on the rule is not rewritten`() = runTest {
        val earliest = 1_700_000_000_000L
        stubEarliest(earliest)
        stubExisting(existingOpening("500.00", timestamp = earliest - dayMillis))

        upsert("500.00")

        verifyNoSave()
    }

    @Test
    fun `unchanged amount still resyncs a date that drifted from the rule`() = runTest {
        val earliest = 1_700_000_000_000L
        stubEarliest(earliest)
        stubExisting(existingOpening("500.00", timestamp = earliest - 5 * dayMillis))
        val request = slot<TransactionRequest>()

        upsert("500.00")

        coVerify {
            transactionManager.handleSaveTransaction(any(), capture(request), profileId, any(), any(), any(), any())
        }
        assertEquals(earliest - dayMillis, request.captured.timestamp)
    }

    @Test
    fun `unchanged amount with no other transactions keeps its existing date`() = runTest {
        val existingTs = 1_650_000_000_000L
        stubEarliest(null)
        stubExisting(existingOpening("500.00", timestamp = existingTs))

        upsert("500.00")

        verifyNoSave()
    }

    @Test
    fun `new opening balance with no other transactions is dated a day before now`() = runTest {
        stubEarliest(null)
        stubExisting(null)
        val request = slot<TransactionRequest>()
        val before = System.currentTimeMillis()

        upsert("500.00")
        val after = System.currentTimeMillis()

        coVerify {
            transactionManager.handleSaveTransaction(null, capture(request), profileId, any(), any(), any(), any())
        }
        assertTrue(request.captured.timestamp in (before - dayMillis)..(after - dayMillis))
    }

    // ── Upsert: amount handling ──────────────────────────────────────────────

    @Test
    fun `sign flip from income to expense is saved as expense with positive magnitude`() = runTest {
        stubEarliest(1_700_000_000_000L)
        stubExisting(existingOpening("500.00", timestamp = 1_000L))
        val request = slot<TransactionRequest>()

        upsert("-500.00")

        coVerify {
            transactionManager.handleSaveTransaction(any(), capture(request), profileId, any(), any(), any(), any())
        }
        assertEquals(EntryMode.EXPENSE, request.captured.entryMode)
        assertEquals(0, BigDecimal("500").compareTo(request.captured.amount))
    }

    @Test
    fun `zero amount deletes an existing opening balance`() = runTest {
        stubEarliest(1_700_000_000_000L)
        stubExisting(existingOpening("500.00", timestamp = 1_000L))

        upsert("0")

        coVerify { transactionManager.deleteTransaction(9L, profileId) }
        verifyNoSave()
    }

    @Test
    fun `zero amount with no existing opening balance does nothing`() = runTest {
        stubEarliest(1_700_000_000_000L)
        stubExisting(null)

        upsert("0")

        coVerify(exactly = 0) { transactionManager.deleteTransaction(any(), any()) }
        verifyNoSave()
    }
}
