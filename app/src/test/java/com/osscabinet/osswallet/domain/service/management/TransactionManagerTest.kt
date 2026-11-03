package com.osscabinet.ossWallet.domain.service.management

import com.osscabinet.ossWallet.domain.model.*
import com.osscabinet.ossWallet.domain.model.dto.TransactionRequest
import com.osscabinet.ossWallet.domain.repository.PlannedPaymentRepository
import com.osscabinet.ossWallet.domain.service.WidgetRefreshTrigger
import com.osscabinet.ossWallet.domain.usecase.TransactionUseCases
import io.mockk.*
import kotlinx.coroutines.flow.flowOf
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
    fun `handleTransferSave initial balance inflow is positive`() = runTest {
        val request = TransactionRequest(
            name = "Initial Balance",
            amount = BigDecimal("500.00"),
            entryMode = EntryMode.TRANSFER,
            accountId = null,
            targetAccountId = "ACC_1",
            currency = "USD",
            categoryId = Category.SYSTEM_INITIAL_BALANCE,
            timestamp = 123456789L
        )

        val txSlot = slot<Transaction>()
        coEvery { transactionUseCases.insertTransaction(capture(txSlot), any()) } returns 1L

        transactionManager.handleSaveTransaction(null, request, 1)

        assertEquals(0, BigDecimal("500.00").compareTo(txSlot.captured.amount))
        assertEquals("ACC_1", txSlot.captured.accountId)
        assertEquals(Category.SYSTEM_INITIAL_BALANCE, txSlot.captured.categoryId)
    }

    @Test
    fun `handleTransferSave initial balance outflow is negative`() = runTest {
        val request = TransactionRequest(
            name = "Initial Balance",
            amount = BigDecimal("300.00"),
            entryMode = EntryMode.TRANSFER,
            accountId = "ACC_1",
            targetAccountId = null,
            currency = "USD",
            categoryId = Category.SYSTEM_INITIAL_BALANCE,
            timestamp = 123456789L
        )

        val txSlot = slot<Transaction>()
        coEvery { transactionUseCases.insertTransaction(capture(txSlot), any()) } returns 1L

        transactionManager.handleSaveTransaction(null, request, 1)

        assertEquals(0, BigDecimal("-300.00").compareTo(txSlot.captured.amount))
        assertEquals("ACC_1", txSlot.captured.accountId)
        assertEquals(Category.SYSTEM_INITIAL_BALANCE, txSlot.captured.categoryId)
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

    // ── Opening balances saved as income/expense must never take the transfer path ──

    private fun openingBalanceRequest(entryMode: EntryMode, amount: String) = TransactionRequest(
        name = "Initial Balance",
        amount = BigDecimal(amount),
        entryMode = entryMode,
        accountId = "ACC_1",
        currency = "USD",
        categoryId = Category.SYSTEM_INITIAL_BALANCE,
        note = "Initial Balance",
        timestamp = 42L
    )

    @Test
    fun `new income opening balance is inserted as positive income, not a transfer outflow`() = runTest {
        coEvery { transactionUseCases.insertTransaction(any(), any()) } returns 5L
        val inserted = slot<Transaction>()

        transactionManager.handleSaveTransaction(
            null, openingBalanceRequest(EntryMode.INCOME, "1000.00"), 1
        )

        coVerify { transactionUseCases.insertTransaction(capture(inserted), 1) }
        coVerify(exactly = 0) { transactionUseCases.saveTransfer(any(), any(), any()) }
        assertEquals(0, BigDecimal("1000").compareTo(inserted.captured.amount))
        assertEquals(TransactionType.INCOME, inserted.captured.transactionType)
        assertEquals(Category.SYSTEM_INITIAL_BALANCE, inserted.captured.categoryId)
    }

    @Test
    fun `editing an income opening balance keeps it positive income and its timestamp`() = runTest {
        val existing = Transaction(
            id = 5L,
            name = "Initial Balance",
            amount = BigDecimal("1000.00"),
            timestamp = 42L,
            currency = "USD",
            transactionType = TransactionType.INCOME,
            accountId = "ACC_1",
            categoryId = Category.SYSTEM_INITIAL_BALANCE
        )
        coEvery { transactionUseCases.clearTransactionLinks(any()) } just Runs
        coEvery { transactionUseCases.updateTransaction(any(), any(), any()) } just Runs
        coEvery { transactionUseCases.getLinkedTransactionIds(any()) } returns flowOf(emptyList())
        val updated = slot<Transaction>()

        transactionManager.handleSaveTransaction(
            existing, openingBalanceRequest(EntryMode.INCOME, "1000.00"), 1
        )

        coVerify { transactionUseCases.updateTransaction(existing, capture(updated), 1) }
        coVerify(exactly = 0) { transactionUseCases.saveTransfer(any(), any(), any()) }
        assertEquals(0, BigDecimal("1000").compareTo(updated.captured.amount))
        assertEquals(TransactionType.INCOME, updated.captured.transactionType)
        assertEquals(42L, updated.captured.timestamp)
    }

    @Test
    fun `editing an expense opening balance keeps it negative expense`() = runTest {
        val existing = Transaction(
            id = 6L,
            name = "Initial Balance",
            amount = BigDecimal("-250.00"),
            timestamp = 42L,
            currency = "USD",
            transactionType = TransactionType.EXPENSE,
            accountId = "ACC_1",
            categoryId = Category.SYSTEM_INITIAL_BALANCE
        )
        coEvery { transactionUseCases.clearTransactionLinks(any()) } just Runs
        coEvery { transactionUseCases.updateTransaction(any(), any(), any()) } just Runs
        coEvery { transactionUseCases.getLinkedTransactionIds(any()) } returns flowOf(emptyList())
        val updated = slot<Transaction>()

        transactionManager.handleSaveTransaction(
            existing, openingBalanceRequest(EntryMode.EXPENSE, "250.00"), 1
        )

        coVerify { transactionUseCases.updateTransaction(existing, capture(updated), 1) }
        assertEquals(0, BigDecimal("-250").compareTo(updated.captured.amount))
        assertEquals(TransactionType.EXPENSE, updated.captured.transactionType)
    }
}
