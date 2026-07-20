package com.osscabinet.ossWallet.appfunctions

import androidx.appfunctions.AppFunctionContext
import com.osscabinet.ossWallet.core.util.MainDispatcherRule
import com.osscabinet.ossWallet.domain.model.Account
import com.osscabinet.ossWallet.domain.model.AccountStructureType
import com.osscabinet.ossWallet.domain.model.Category
import com.osscabinet.ossWallet.domain.model.CategoryNature
import com.osscabinet.ossWallet.domain.model.EntryMode
import com.osscabinet.ossWallet.domain.model.PaymentMethod
import com.osscabinet.ossWallet.domain.model.Transaction
import com.osscabinet.ossWallet.domain.model.TransactionStatus
import com.osscabinet.ossWallet.domain.model.TransactionType
import com.osscabinet.ossWallet.domain.model.dto.TransactionRequest
import com.osscabinet.ossWallet.domain.repository.AccountRepository
import com.osscabinet.ossWallet.domain.repository.CategoryRepository
import com.osscabinet.ossWallet.domain.repository.SettingsRepository
import com.osscabinet.ossWallet.domain.repository.TransactionRepository
import com.osscabinet.ossWallet.domain.service.management.TransactionManager
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import java.math.BigDecimal
import java.time.LocalDate

class TransactionAppFunctionsTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private lateinit var transactionManager: TransactionManager
    private lateinit var transactionRepository: TransactionRepository
    private lateinit var accountRepository: AccountRepository
    private lateinit var categoryRepository: CategoryRepository
    private lateinit var settingsRepository: SettingsRepository
    private lateinit var appFunctions: TransactionAppFunctions
    private lateinit var ctx: AppFunctionContext

    private val profileIdFlow = MutableStateFlow(1)

    private val cashAccount = Account(
        id = "acc-1",
        name = "Cash",
        currencyCode = "USD",
        structureType = AccountStructureType.CASH,
        colorHex = "#4CAF50",
        icon = "Wallet"
    )
    private val savingsAccount = Account(
        id = "acc-2",
        name = "Savings",
        currencyCode = "USD",
        structureType = AccountStructureType.SAVINGS,
        colorHex = "#2196F3",
        icon = "AccountBalance"
    )
    private val foodCategory = Category(
        id = 10L,
        name = "Food",
        iconName = "Restaurant",
        colorHex = -0x100,
        isDefault = true,
        parentId = null,
        nature = CategoryNature.WANT,
        profileId = 1
    )

    private fun makeTransaction(
        id: Long = 1L,
        type: TransactionType = TransactionType.EXPENSE,
        amount: BigDecimal = BigDecimal("-42.50"),
        accountId: String = "acc-1",
        categoryId: Long = 10L,
        timestamp: Long = System.currentTimeMillis()
    ) = Transaction(
        id = id,
        name = "Test",
        amount = amount,
        timestamp = timestamp,
        currency = "USD",
        transactionType = type,
        accountId = accountId,
        categoryId = categoryId,
        tags = emptySet(),
        paymentMethod = PaymentMethod.CASH,
        payee = "",
        note = "",
        sourcePlannedPaymentId = null,
        gpsLocation = null,
        location = null,
        warrantyExpiration = null,
        status = TransactionStatus.CLEARED,
        targetAccountId = null,
        targetCurrency = null,
        transferRate = BigDecimal.ONE
    )

    @Before
    fun setup() {
        transactionManager = mockk(relaxed = true)
        transactionRepository = mockk(relaxed = true)
        accountRepository = mockk(relaxed = true)
        categoryRepository = mockk(relaxed = true)
        settingsRepository = mockk(relaxed = true)
        ctx = mockk(relaxed = true)

        every { settingsRepository.currentProfileId } returns profileIdFlow
        every { accountRepository.getAllAccounts(any()) } returns flowOf(listOf(cashAccount, savingsAccount))
        every { categoryRepository.getAllCategories(any()) } returns flowOf(listOf(foodCategory))
        every { transactionRepository.getAllTransactions(any()) } returns flowOf(emptyList())
        coEvery { transactionManager.handleSaveTransaction(any(), any(), any(), any(), any(), any(), any()) } returns 99L

        appFunctions = TransactionAppFunctions(
            transactionManager = transactionManager,
            transactionRepository = transactionRepository,
            accountRepository = accountRepository,
            categoryRepository = categoryRepository,
            settingsRepository = settingsRepository
        )
    }

    @Test
    fun `createTransaction expense succeeds with valid params`() = runTest {
        val result = appFunctions.createTransaction(
            ctx,
            CreateTransactionParams(
                entryMode = "expense",
                amount = "42.50",
                accountName = "Cash",
                categoryName = "Food",
                note = "Lunch"
            )
        )

        assertTrue(result.success)
        assertEquals(99L, result.transactionId)
        assertEquals("42.50", result.amount)
        assertEquals("expense", result.type)
        assertEquals("Cash", result.accountName)
        assertEquals("Food", result.categoryName)
        assertEquals("Lunch", result.note)
    }

    @Test
    fun `createTransaction passes negated amount for expense`() = runTest {
        appFunctions.createTransaction(
            ctx,
            CreateTransactionParams(entryMode = "expense", amount = "42.50", accountName = "Cash", categoryName = "Food")
        )

        coVerify {
            transactionManager.handleSaveTransaction(
                existingTransaction = null,
                request = match { it.amount == BigDecimal("-42.50") && it.entryMode == EntryMode.EXPENSE },
                profileId = 1
            )
        }
    }

    @Test
    fun `createTransaction passes positive amount for income`() = runTest {
        appFunctions.createTransaction(
            ctx,
            CreateTransactionParams(entryMode = "income", amount = "100.00", accountName = "Cash")
        )

        coVerify {
            transactionManager.handleSaveTransaction(
                existingTransaction = null,
                request = match { it.amount == BigDecimal("100.00") && it.entryMode == EntryMode.INCOME },
                profileId = 1
            )
        }
    }

    @Test
    fun `createTransaction returns error for unknown account`() = runTest {
        val result = appFunctions.createTransaction(
            ctx,
            CreateTransactionParams(entryMode = "expense", amount = "10.00", accountName = "NonExistent")
        )

        assertFalse(result.success)
        assertTrue(result.errorMessage?.contains("NonExistent") == true)
    }

    @Test
    fun `createTransaction returns error for invalid entryMode`() = runTest {
        val result = appFunctions.createTransaction(
            ctx,
            CreateTransactionParams(entryMode = "refund", amount = "10.00", accountName = "Cash")
        )

        assertFalse(result.success)
        assertTrue(result.errorMessage?.contains("refund") == true)
    }

    @Test
    fun `createTransaction returns error for invalid amount`() = runTest {
        val result = appFunctions.createTransaction(
            ctx,
            CreateTransactionParams(entryMode = "expense", amount = "not-a-number", accountName = "Cash")
        )

        assertFalse(result.success)
    }

    @Test
    fun `createTransaction falls back to SYSTEM_OTHERS when category not found`() = runTest {
        appFunctions.createTransaction(
            ctx,
            CreateTransactionParams(entryMode = "expense", amount = "10.00", accountName = "Cash", categoryName = null)
        )

        coVerify {
            transactionManager.handleSaveTransaction(
                existingTransaction = null,
                request = match { it.categoryId == Category.SYSTEM_OTHERS },
                profileId = 1
            )
        }
    }

    @Test
    fun `createTransaction resolves both account sides for transfer`() = runTest {
        appFunctions.createTransaction(
            ctx,
            CreateTransactionParams(
                entryMode = "transfer",
                amount = "200.00",
                accountName = "Cash",
                targetAccountName = "Savings"
            )
        )

        coVerify {
            transactionManager.handleSaveTransaction(
                existingTransaction = null,
                request = match {
                    it.entryMode == EntryMode.TRANSFER &&
                            it.accountId == "acc-1" &&
                            it.targetAccountId == "acc-2"
                },
                profileId = 1
            )
        }
    }

    @Test
    fun `createTransaction uses outside-wallet for transfer with no target account`() = runTest {
        appFunctions.createTransaction(
            ctx,
            CreateTransactionParams(entryMode = "transfer", amount = "50.00", accountName = "Cash")
        )

        coVerify {
            transactionManager.handleSaveTransaction(
                existingTransaction = null,
                request = match { it.targetAccountId == "" },
                profileId = 1
            )
        }
    }

    @Test
    fun `createTransaction parses ISO-8601 date correctly`() = runTest {
        val today = LocalDate.now()
        appFunctions.createTransaction(
            ctx,
            CreateTransactionParams(
                entryMode = "expense",
                amount = "10.00",
                accountName = "Cash",
                date = today.toString()
            )
        )

        coVerify {
            transactionManager.handleSaveTransaction(
                existingTransaction = null,
                request = match { it.timestamp > 0L },
                profileId = 1
            )
        }
    }

    @Test
    fun `createTransaction parses comma-separated tags`() = runTest {
        appFunctions.createTransaction(
            ctx,
            CreateTransactionParams(entryMode = "expense", amount = "10.00", accountName = "Cash", tags = "food, work, fun")
        )

        coVerify {
            transactionManager.handleSaveTransaction(
                existingTransaction = null,
                request = match { it.tags == setOf("food", "work", "fun") },
                profileId = 1
            )
        }
    }

    @Test
    fun `getTransactions filters by type expense`() = runTest {
        val txs = listOf(
            makeTransaction(id = 1L, type = TransactionType.EXPENSE),
            makeTransaction(id = 2L, type = TransactionType.INCOME, amount = BigDecimal("100"))
        )
        every { transactionRepository.getAllTransactions(any()) } returns flowOf(txs)

        val result = appFunctions.getTransactions(ctx, GetTransactionsParams(type = "expense"))

        assertTrue(result.success)
        assertEquals(1, result.totalCount)
        assertEquals(1L, result.transactions[0].transactionId)
    }

    @Test
    fun `getTransactions filters by type income`() = runTest {
        val txs = listOf(
            makeTransaction(id = 1L, type = TransactionType.EXPENSE),
            makeTransaction(id = 2L, type = TransactionType.INCOME, amount = BigDecimal("100"))
        )
        every { transactionRepository.getAllTransactions(any()) } returns flowOf(txs)

        val result = appFunctions.getTransactions(ctx, GetTransactionsParams(type = "income"))

        assertTrue(result.success)
        assertEquals(1, result.totalCount)
        assertEquals(2L, result.transactions[0].transactionId)
    }

    @Test
    fun `getTransactions filters by account name`() = runTest {
        val txs = listOf(
            makeTransaction(id = 1L, accountId = "acc-1"),
            makeTransaction(id = 2L, accountId = "acc-2")
        )
        every { transactionRepository.getAllTransactions(any()) } returns flowOf(txs)

        val result = appFunctions.getTransactions(ctx, GetTransactionsParams(accountName = "Cash"))

        assertTrue(result.success)
        assertEquals(1, result.totalCount)
        assertEquals(1L, result.transactions[0].transactionId)
    }

    @Test
    fun `getTransactions respects limit`() = runTest {
        val txs = (1L..10L).map { makeTransaction(id = it) }
        every { transactionRepository.getAllTransactions(any()) } returns flowOf(txs)

        val result = appFunctions.getTransactions(ctx, GetTransactionsParams(limit = 3))

        assertTrue(result.success)
        assertEquals(3, result.totalCount)
    }

    @Test
    fun `getTransactions returns error for unknown account filter`() = runTest {
        val result = appFunctions.getTransactions(ctx, GetTransactionsParams(accountName = "Ghost"))

        assertFalse(result.success)
        assertTrue(result.errorMessage?.contains("Ghost") == true)
    }

    @Test
    fun `updateTransaction merges only non-null fields`() = runTest {
        val existing = makeTransaction(id = 5L, amount = BigDecimal("-20.00"))
        coEvery { transactionRepository.getTransactionById(5L, 1) } returns existing

        appFunctions.updateTransaction(
            ctx,
            UpdateTransactionParams(transactionId = 5L, note = "Updated note")
        )

        coVerify {
            transactionManager.handleSaveTransaction(
                existingTransaction = existing,
                request = match {
                    it.note == "Updated note" && it.amount == BigDecimal("-20.00")
                },
                profileId = 1
            )
        }
    }

    @Test
    fun `updateTransaction returns error when transaction not found`() = runTest {
        coEvery { transactionRepository.getTransactionById(999L, 1) } returns null

        val result = appFunctions.updateTransaction(
            ctx,
            UpdateTransactionParams(transactionId = 999L, note = "test")
        )

        assertFalse(result.success)
        assertTrue(result.errorMessage?.contains("999") == true)
    }

    @Test
    fun `deleteTransaction calls transactionManager with correct id and profileId`() = runTest {
        appFunctions.deleteTransaction(ctx, DeleteByIdParams(id = 7L))

        coVerify { transactionManager.deleteTransaction(7L, 1) }
    }

    @Test
    fun `deleteTransaction returns success`() = runTest {
        val result = appFunctions.deleteTransaction(ctx, DeleteByIdParams(id = 7L))

        assertTrue(result.success)
    }
}
