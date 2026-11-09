package com.osscabinet.ossWallet.domain.service.management

import com.osscabinet.ossWallet.domain.model.*
import com.osscabinet.ossWallet.domain.model.dto.TransactionRequest
import com.osscabinet.ossWallet.domain.repository.PlannedPaymentRepository
import com.osscabinet.ossWallet.domain.service.WidgetRefreshTrigger
import com.osscabinet.ossWallet.domain.usecase.TransactionUseCases
import io.mockk.*
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import java.math.BigDecimal

class TransactionManagerTest {

    private val transactionUseCases: TransactionUseCases = mockk()
    private val plannedPaymentRepository: PlannedPaymentRepository = mockk()
    private val widgetRefreshTrigger: WidgetRefreshTrigger = mockk(relaxed = true)

    private lateinit var transactionManager: TransactionManager

    @Before
    fun setup() {
        transactionManager = TransactionManager(
            transactionUseCases,
            plannedPaymentRepository,
            widgetRefreshTrigger
        )
        // handleSaveTransaction wraps its work in runAtomically { ... }; run the block inline
        // so the per-test stubs below are actually exercised.
        coEvery { transactionUseCases.runAtomically<Any?>(any()) } coAnswers {
            firstArg<suspend () -> Any?>().invoke()
        }
        // Opening-balance bookkeeping that runs after every save; by default the account has none.
        coEvery { transactionUseCases.getEarliestNonInitialBalanceTimestamp(any(), any()) } returns null
        coEvery { transactionUseCases.getInitialBalanceTransaction(any(), any(), any()) } returns null
        coEvery { transactionUseCases.getInitialBalanceCurrencies(any(), any()) } returns emptyList()
    }

    @Test
    fun `handleTransferSave internal transfer creates two linked transactions with correct signs`() = runTest {
        val request = TransactionRequest(
            name = "Transfer",
            amount = BigDecimal("100.00"),
            entryMode = EntryMode.TRANSFER,
            accountId = "ACC_1",
            targetAccountId = "ACC_2",
            currency = "USD",
            targetCurrency = "EUR",
            transferRate = BigDecimal("0.85"),
            categoryId = Category.SYSTEM_TRANSFER,
            timestamp = 123456789L
        )

        val sourceSlot = slot<Transaction>()
        val targetSlot = slot<Transaction>()

        coEvery { 
            transactionUseCases.saveTransfer(capture(sourceSlot), capture(targetSlot), any()) 
        } returns Pair(1L, 2L)

        transactionManager.handleSaveTransaction(null, request, 1)

        // Verify Source Transaction
        assertEquals(BigDecimal("-100.00"), sourceSlot.captured.amount)
        assertEquals("ACC_1", sourceSlot.captured.accountId)
        assertEquals("ACC_2", sourceSlot.captured.targetAccountId)
        assertEquals(Category.SYSTEM_TRANSFER, sourceSlot.captured.categoryId)

        // Verify Target Transaction
        assertEquals(0, BigDecimal("85.00").compareTo(targetSlot.captured.amount))
        assertEquals("ACC_2", targetSlot.captured.accountId)
        assertEquals("ACC_1", targetSlot.captured.targetAccountId)
        assertEquals(Category.SYSTEM_TRANSFER, targetSlot.captured.categoryId)
        // In the target transaction, the targetCurrency is the currency of the SOURCE
        assertEquals("USD", targetSlot.captured.targetCurrency) 
    }

    @Test
    fun `balance adjustment saved as a transfer keeps the reconciliation category`() = runTest {
        val request = TransactionRequest(
            name = "Balance Adjustment",
            amount = BigDecimal("30.00"),
            entryMode = EntryMode.TRANSFER,
            accountId = "ACC_1",
            targetAccountId = null,
            currency = "USD",
            categoryId = Category.SYSTEM_RECONCILIATION_ADJUSTMENT,
            timestamp = 123456789L
        )
        val txSlot = slot<Transaction>()
        coEvery { transactionUseCases.insertTransaction(capture(txSlot), any()) } returns 1L

        transactionManager.handleSaveTransaction(null, request, 1)

        assertEquals(0, BigDecimal("-30.00").compareTo(txSlot.captured.amount))
        assertEquals(TransactionType.TRANSFER_OUTFLOW, txSlot.captured.transactionType)
        assertEquals(Category.SYSTEM_RECONCILIATION_ADJUSTMENT, txSlot.captured.categoryId)
    }

    @Test
    fun `handleTransferSave update existing internal transfer updates both linked records`() = runTest {
        val existingTx = Transaction(
            id = 10L,
            name = "Old",
            amount = BigDecimal("-50.00"),
            transactionType = TransactionType.TRANSFER_OUTFLOW,
            accountId = "ACC_1",
            categoryId = Category.SYSTEM_TRANSFER,
            timestamp = 1000L,
            currency = "USD",
            targetAccountId = "ACC_2"
        )

        val request = TransactionRequest(
            name = "Updated",
            amount = BigDecimal("150.00"),
            entryMode = EntryMode.TRANSFER,
            accountId = "ACC_1",
            targetAccountId = "ACC_3", // Changed target account
            currency = "USD",
            targetCurrency = "EUR",
            transferRate = BigDecimal("0.9"),
            categoryId = Category.SYSTEM_TRANSFER,
            timestamp = 2000L
        )

        coEvery { transactionUseCases.getLinkedTransactionIds(10L) } returns flowOf(listOf(11L))
        coEvery { transactionUseCases.getTransactionById(11L, any()) } returns Transaction(
            id = 11L,
            name = "Old Target",
            amount = BigDecimal("50.00"),
            transactionType = TransactionType.TRANSFER_INFLOW,
            accountId = "ACC_2",
            categoryId = Category.SYSTEM_TRANSFER,
            timestamp = 1000L,
            currency = "USD",
            targetAccountId = "ACC_1"
        )

        val updatedSource = slot<Transaction>()
        val updatedTarget = slot<Transaction>()

        coEvery { transactionUseCases.updateTransaction(match { it.id == 10L }, capture(updatedSource), any()) } returns Unit
        coEvery { transactionUseCases.updateTransaction(match { it.id == 11L }, capture(updatedTarget), any()) } returns Unit
        coEvery { transactionUseCases.removeTransactionLink(any(), any()) } returns Unit
        coEvery { transactionUseCases.addTransactionLink(any(), any()) } returns Unit

        transactionManager.handleSaveTransaction(existingTx, request, 1)

        // Verify Source update
        assertEquals(BigDecimal("-150.00"), updatedSource.captured.amount)
        assertEquals("ACC_3", updatedSource.captured.targetAccountId)

        // Verify Target update
        assertEquals(0, BigDecimal("135.00").compareTo(updatedTarget.captured.amount))
        assertEquals("ACC_3", updatedTarget.captured.accountId)
        assertEquals("ACC_1", updatedTarget.captured.targetAccountId)
    }

    @Test
    fun `converting a linked transfer leg to an opening balance drops its links`() = runTest {
        val existing = Transaction(
            id = 5L,
            name = "Initial Balance",
            amount = BigDecimal("1000.00"),
            timestamp = 42L,
            currency = "USD",
            transactionType = TransactionType.TRANSFER_INFLOW,
            accountId = "ACC_1",
            categoryId = Category.SYSTEM_INITIAL_BALANCE
        )
        coEvery { transactionUseCases.clearTransactionLinks(any()) } just Runs
        coEvery { transactionUseCases.updateTransaction(any(), any(), any()) } just Runs
        coEvery { transactionUseCases.getLinkedTransactionIds(any()) } returns flowOf(emptyList())

        transactionManager.handleSaveTransaction(
            existing, openingBalanceRequest(null, "ACC_1", "1000.00"), 1
        )

        coVerify(exactly = 1) { transactionUseCases.clearTransactionLinks(5L) }
        coVerify { transactionUseCases.updateTransaction(existing, any(), 1) }
    }

    @Test
    fun `saving a transfer leg as a normal row clears its links`() = runTest {
        val existing = Transaction(
            id = 7L,
            name = "Transfer",
            amount = BigDecimal("-100.00"),
            timestamp = 42L,
            currency = "USD",
            transactionType = TransactionType.TRANSFER_OUTFLOW,
            accountId = "ACC_1",
            targetAccountId = "ACC_2",
            categoryId = Category.SYSTEM_TRANSFER
        )
        val request = TransactionRequest(
            name = "Groceries",
            amount = BigDecimal("100.00"),
            entryMode = EntryMode.EXPENSE,
            accountId = "ACC_1",
            currency = "USD",
            categoryId = Category.SYSTEM_UNKNOWN,
            timestamp = 42L
        )
        coEvery { transactionUseCases.clearTransactionLinks(any()) } just Runs
        coEvery { transactionUseCases.updateTransaction(any(), any(), any()) } just Runs
        coEvery { transactionUseCases.getLinkedTransactionIds(any()) } returns flowOf(emptyList())

        transactionManager.handleSaveTransaction(existing, request, 1)

        coVerify(exactly = 1) { transactionUseCases.clearTransactionLinks(7L) }
    }

    // ── Opening balances/adjustments are single-leg Outside Wallet transfers ──

    // accountId null = Outside Wallet is the source (inflow); targetAccountId null = Outside
    // Wallet is the target (outflow) - exactly one of the two must be null.
    private fun openingBalanceRequest(accountId: String?, targetAccountId: String?, amount: String) =
        TransactionRequest(
            name = "Initial Balance",
            amount = BigDecimal(amount),
            entryMode = EntryMode.TRANSFER,
            accountId = accountId,
            targetAccountId = targetAccountId,
            currency = "USD",
            categoryId = Category.SYSTEM_INITIAL_BALANCE,
            note = "Initial Balance",
            timestamp = 42L
        )

    @Test
    fun `new gain opening balance is inserted as a positive transfer inflow`() = runTest {
        coEvery { transactionUseCases.insertTransaction(any(), any()) } returns 5L
        val inserted = slot<Transaction>()

        transactionManager.handleSaveTransaction(
            null, openingBalanceRequest(accountId = null, targetAccountId = "ACC_1", amount = "1000.00"), 1
        )

        coVerify { transactionUseCases.insertTransaction(capture(inserted), 1) }
        coVerify(exactly = 0) { transactionUseCases.saveTransfer(any(), any(), any()) }
        assertEquals(0, BigDecimal("1000").compareTo(inserted.captured.amount))
        assertEquals(TransactionType.TRANSFER_INFLOW, inserted.captured.transactionType)
        assertEquals("ACC_1", inserted.captured.accountId)
        assertEquals(Category.SYSTEM_INITIAL_BALANCE, inserted.captured.categoryId)
    }

    @Test
    fun `editing a gain opening balance keeps it a positive transfer inflow and its timestamp`() = runTest {
        val existing = Transaction(
            id = 5L,
            name = "Initial Balance",
            amount = BigDecimal("1000.00"),
            timestamp = 42L,
            currency = "USD",
            transactionType = TransactionType.TRANSFER_INFLOW,
            accountId = "ACC_1",
            categoryId = Category.SYSTEM_INITIAL_BALANCE
        )
        coEvery { transactionUseCases.clearTransactionLinks(any()) } just Runs
        coEvery { transactionUseCases.updateTransaction(any(), any(), any()) } just Runs
        coEvery { transactionUseCases.getLinkedTransactionIds(any()) } returns flowOf(emptyList())
        val updated = slot<Transaction>()

        transactionManager.handleSaveTransaction(
            existing, openingBalanceRequest(accountId = null, targetAccountId = "ACC_1", amount = "1000.00"), 1
        )

        coVerify { transactionUseCases.updateTransaction(existing, capture(updated), 1) }
        coVerify(exactly = 0) { transactionUseCases.saveTransfer(any(), any(), any()) }
        assertEquals(0, BigDecimal("1000").compareTo(updated.captured.amount))
        assertEquals(TransactionType.TRANSFER_INFLOW, updated.captured.transactionType)
        assertEquals(42L, updated.captured.timestamp)
    }

    @Test
    fun `editing a loss opening balance keeps it a negative transfer outflow`() = runTest {
        val existing = Transaction(
            id = 6L,
            name = "Initial Balance",
            amount = BigDecimal("-250.00"),
            timestamp = 42L,
            currency = "USD",
            transactionType = TransactionType.TRANSFER_OUTFLOW,
            accountId = "ACC_1",
            categoryId = Category.SYSTEM_INITIAL_BALANCE
        )
        coEvery { transactionUseCases.clearTransactionLinks(any()) } just Runs
        coEvery { transactionUseCases.updateTransaction(any(), any(), any()) } just Runs
        coEvery { transactionUseCases.getLinkedTransactionIds(any()) } returns flowOf(emptyList())
        val updated = slot<Transaction>()

        transactionManager.handleSaveTransaction(
            existing, openingBalanceRequest(accountId = "ACC_1", targetAccountId = null, amount = "250.00"), 1
        )

        coVerify { transactionUseCases.updateTransaction(existing, capture(updated), 1) }
        assertEquals(0, BigDecimal("-250").compareTo(updated.captured.amount))
        assertEquals(TransactionType.TRANSFER_OUTFLOW, updated.captured.transactionType)
    }

    @Test
    fun `an initial balance row reassigned to an ordinary category can be saved as real income`() = runTest {
        val request = TransactionRequest(
            name = "Gift",
            amount = BigDecimal("1000.00"),
            entryMode = EntryMode.INCOME,
            accountId = "ACC_1",
            currency = "USD",
            categoryId = Category.SYSTEM_OTHERS,
            timestamp = 42L
        )
        val inserted = slot<Transaction>()
        coEvery { transactionUseCases.insertTransaction(capture(inserted), any()) } returns 5L

        transactionManager.handleSaveTransaction(null, request, 1)

        assertEquals(TransactionType.INCOME, inserted.captured.transactionType)
        assertEquals(Category.SYSTEM_OTHERS, inserted.captured.categoryId)
    }

    // ── Transfer categories are transfer-typed by definition ──

    private fun incomeOrExpense(mode: EntryMode, categoryId: Long) = TransactionRequest(
        name = "x",
        amount = BigDecimal("500.00"),
        entryMode = mode,
        accountId = "ACC_1",
        currency = "USD",
        categoryId = categoryId,
        timestamp = 123456789L
    )

    @Test
    fun `income on the opening balance category becomes a single-leg transfer inflow`() = runTest {
        val inserted = slot<Transaction>()
        coEvery { transactionUseCases.insertTransaction(capture(inserted), any()) } returns 1L

        transactionManager.handleSaveTransaction(null, incomeOrExpense(EntryMode.INCOME, Category.SYSTEM_INITIAL_BALANCE), 1)

        assertEquals(TransactionType.TRANSFER_INFLOW, inserted.captured.transactionType)
        assertEquals("ACC_1", inserted.captured.accountId)
        assertEquals(0, BigDecimal("500").compareTo(inserted.captured.amount))
        assertEquals(Category.SYSTEM_INITIAL_BALANCE, inserted.captured.categoryId)
    }

    @Test
    fun `expense on the adjustment category becomes a single-leg transfer outflow`() = runTest {
        val inserted = slot<Transaction>()
        coEvery { transactionUseCases.insertTransaction(capture(inserted), any()) } returns 1L

        transactionManager.handleSaveTransaction(
            null, incomeOrExpense(EntryMode.EXPENSE, Category.SYSTEM_RECONCILIATION_ADJUSTMENT), 1
        )

        assertEquals(TransactionType.TRANSFER_OUTFLOW, inserted.captured.transactionType)
        assertEquals(0, BigDecimal("-500").compareTo(inserted.captured.amount))
        assertEquals(Category.SYSTEM_RECONCILIATION_ADJUSTMENT, inserted.captured.categoryId)
    }

    @Test
    fun `income on the transfer category becomes a single-leg transfer inflow`() = runTest {
        val inserted = slot<Transaction>()
        coEvery { transactionUseCases.insertTransaction(capture(inserted), any()) } returns 1L

        transactionManager.handleSaveTransaction(null, incomeOrExpense(EntryMode.INCOME, Category.SYSTEM_TRANSFER), 1)

        assertEquals(TransactionType.TRANSFER_INFLOW, inserted.captured.transactionType)
        assertEquals(Category.SYSTEM_TRANSFER, inserted.captured.categoryId)
    }

    @Test
    fun `a two-sided transfer is never an opening balance`() = runTest {
        val request = openingBalanceRequest("ACC_1", "ACC_2", "500.00")
        val source = slot<Transaction>()
        val target = slot<Transaction>()
        coEvery { transactionUseCases.saveTransfer(capture(source), capture(target), any()) } returns Pair(1L, 2L)

        transactionManager.handleSaveTransaction(null, request, 1)

        assertEquals(Category.SYSTEM_TRANSFER, source.captured.categoryId)
        assertEquals(Category.SYSTEM_TRANSFER, target.captured.categoryId)
    }

    @Test
    fun `a two-sided transfer saved with the Unknown default is stored as Transfer`() = runTest {
        val request = openingBalanceRequest("ACC_1", "ACC_2", "100.00").copy(categoryId = Category.SYSTEM_UNKNOWN)
        val source = slot<Transaction>()
        val target = slot<Transaction>()
        coEvery { transactionUseCases.saveTransfer(capture(source), capture(target), any()) } returns Pair(1L, 2L)

        transactionManager.handleSaveTransaction(null, request, 1)

        assertEquals(Category.SYSTEM_TRANSFER, source.captured.categoryId)
        assertEquals(Category.SYSTEM_TRANSFER, target.captured.categoryId)
    }

    @Test
    fun `a single-leg transfer with a stale ordinary category is stored as Transfer`() = runTest {
        val inserted = slot<Transaction>()
        coEvery { transactionUseCases.insertTransaction(capture(inserted), any()) } returns 1L
        val request = openingBalanceRequest(null, "ACC_1", "100.00").copy(categoryId = Category.SYSTEM_OTHERS)

        transactionManager.handleSaveTransaction(null, request, 1)

        assertEquals(Category.SYSTEM_TRANSFER, inserted.captured.categoryId)
    }

    @Test
    fun `deleting a transfer leg decouples its counterpart whatever its category`() = runTest {
        val leg = Transaction(
            id = 3L, name = "t", amount = BigDecimal("-10"), timestamp = 1L, currency = "USD",
            transactionType = TransactionType.TRANSFER_OUTFLOW, accountId = "ACC_1",
            targetAccountId = "ACC_2", categoryId = Category.SYSTEM_UNKNOWN
        )
        val counterpart = leg.copy(
            id = 4L, amount = BigDecimal("10"), transactionType = TransactionType.TRANSFER_INFLOW,
            accountId = "ACC_2", targetAccountId = "ACC_1"
        )
        val updated = slot<Transaction>()
        coEvery { transactionUseCases.getTransactionById(3L, 1) } returns leg
        coEvery { transactionUseCases.getTransactionById(4L, 1) } returns counterpart
        coEvery { transactionUseCases.getLinkedTransactionIds(3L) } returns flowOf(listOf(4L))
        coEvery { transactionUseCases.removeTransactionLink(any(), any()) } just Runs
        coEvery { transactionUseCases.updateTransaction(counterpart, capture(updated), 1) } just Runs
        coEvery { transactionUseCases.deleteTransaction(3L, 1) } just Runs

        transactionManager.deleteTransaction(3L, 1)

        coVerify { transactionUseCases.removeTransactionLink(3L, 4L) }
        coVerify { transactionUseCases.removeTransactionLink(4L, 3L) }
        // The surviving leg forgets the deleted leg's account, so it is a standalone leg from now on.
        assertEquals(null, updated.captured.targetAccountId)
        assertEquals("ACC_2", updated.captured.accountId)
    }

    // ── One opening balance per account + currency ──

    private fun existingOpeningBalance(id: Long, timestamp: Long = 10L) = Transaction(
        id = id, name = "Initial Balance", amount = BigDecimal("900"), timestamp = timestamp, currency = "USD",
        transactionType = TransactionType.TRANSFER_INFLOW, accountId = "ACC_1",
        categoryId = Category.SYSTEM_INITIAL_BALANCE
    )

    @Test
    fun `a second opening balance fails unless the caller agreed to replace the first`() = runTest {
        val first = existingOpeningBalance(id = 50L)
        coEvery { transactionUseCases.getInitialBalanceTransaction("ACC_1", "USD", 1) } returns first

        val failure = runCatching {
            transactionManager.handleSaveTransaction(null, openingBalanceRequest(null, "ACC_1", "1000.00"), 1)
        }.exceptionOrNull()

        assertEquals(true, failure is DuplicateInitialBalanceException)
        assertEquals(50L, (failure as DuplicateInitialBalanceException).existing.id)
        coVerify(exactly = 0) { transactionUseCases.insertTransaction(any(), any()) }
        coVerify(exactly = 0) { transactionUseCases.deleteTransaction(any(), any()) }
    }

    @Test
    fun `confirming the replacement removes the first opening balance and saves the new one`() = runTest {
        val first = existingOpeningBalance(id = 50L)
        coEvery { transactionUseCases.getInitialBalanceTransaction("ACC_1", "USD", 1) } returns first
        coEvery { transactionUseCases.deleteTransaction(50L, 1) } just Runs
        coEvery { transactionUseCases.insertTransaction(any(), any()) } returns 51L

        transactionManager.handleSaveTransaction(
            null, openingBalanceRequest(null, "ACC_1", "1000.00"), 1, replaceExistingInitialBalance = true
        )

        coVerifyOrder {
            transactionUseCases.deleteTransaction(50L, 1)
            transactionUseCases.insertTransaction(any(), 1)
        }
    }

    @Test
    fun `editing the opening balance itself is not a duplicate`() = runTest {
        val existing = existingOpeningBalance(id = 50L)
        coEvery { transactionUseCases.getInitialBalanceTransaction("ACC_1", "USD", 1) } returns existing
        coEvery { transactionUseCases.clearTransactionLinks(any()) } just Runs
        coEvery { transactionUseCases.updateTransaction(any(), any(), any()) } just Runs
        coEvery { transactionUseCases.getLinkedTransactionIds(any()) } returns flowOf(emptyList())

        transactionManager.handleSaveTransaction(existing, openingBalanceRequest(null, "ACC_1", "1000.00"), 1)

        coVerify { transactionUseCases.updateTransaction(existing, any(), 1) }
        coVerify(exactly = 0) { transactionUseCases.deleteTransaction(any(), any()) }
    }

    // ── The opening balance follows the earliest real row ──

    private val oneDay = 24L * 60 * 60 * 1000

    @Test
    fun `saving a backdated row moves the opening balance to the day before it`() = runTest {
        val opening = existingOpeningBalance(id = 50L, timestamp = 500_000_000L)
        coEvery { transactionUseCases.insertTransaction(any(), any()) } returns 9L
        coEvery { transactionUseCases.getEarliestNonInitialBalanceTimestamp("ACC_1", 1) } returns 400_000_000L
        coEvery { transactionUseCases.getInitialBalanceCurrencies("ACC_1", 1) } returns listOf("USD")
        coEvery { transactionUseCases.getInitialBalanceTransaction("ACC_1", "USD", 1) } returns opening
        val moved = slot<Transaction>()
        coEvery { transactionUseCases.updateTransaction(opening, capture(moved), 1) } just Runs

        transactionManager.handleSaveTransaction(null, incomeOrExpense(EntryMode.EXPENSE, Category.SYSTEM_OTHERS), 1)

        assertEquals(400_000_000L - oneDay, moved.captured.timestamp)
    }

    @Test
    fun `deleting the earliest row moves the opening balance forward again`() = runTest {
        val opening = existingOpeningBalance(id = 50L, timestamp = 100L)
        val earliest = Transaction(
            id = 8L, name = "Coffee", amount = BigDecimal("-3"), timestamp = 200L, currency = "USD",
            transactionType = TransactionType.EXPENSE, accountId = "ACC_1", categoryId = Category.SYSTEM_OTHERS
        )
        coEvery { transactionUseCases.getTransactionById(8L, 1) } returns earliest
        coEvery { transactionUseCases.deleteTransaction(8L, 1) } just Runs
        coEvery { transactionUseCases.getEarliestNonInitialBalanceTimestamp("ACC_1", 1) } returns 900_000_000L
        coEvery { transactionUseCases.getInitialBalanceCurrencies("ACC_1", 1) } returns listOf("USD")
        coEvery { transactionUseCases.getInitialBalanceTransaction("ACC_1", "USD", 1) } returns opening
        val moved = slot<Transaction>()
        coEvery { transactionUseCases.updateTransaction(opening, capture(moved), 1) } just Runs

        transactionManager.deleteTransaction(8L, 1)

        assertEquals(900_000_000L - oneDay, moved.captured.timestamp)
    }
}
