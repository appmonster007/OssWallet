package com.osscabinet.ossWallet.ui.screens.accounts.entry

import com.osscabinet.ossWallet.core.util.MainDispatcherRule
import com.osscabinet.ossWallet.domain.model.Account
import com.osscabinet.ossWallet.domain.model.AccountStructureType
import com.osscabinet.ossWallet.domain.model.Category
import com.osscabinet.ossWallet.domain.model.Transaction
import com.osscabinet.ossWallet.domain.model.TransactionType
import com.osscabinet.ossWallet.domain.service.query.DatabaseQueryService
import com.osscabinet.ossWallet.domain.usecase.AccountUseCases
import com.osscabinet.ossWallet.test.FakeAccountRepository
import com.osscabinet.ossWallet.test.FakeComputationRepository
import com.osscabinet.ossWallet.test.FakeSettingsRepository
import com.osscabinet.ossWallet.test.FakeTransactionRepository
import io.mockk.clearMocks
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import java.math.BigDecimal

@OptIn(ExperimentalCoroutinesApi::class)
class AccountEntryViewModelTest {

    private val testDispatcher = StandardTestDispatcher()

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule(testDispatcher)

    private lateinit var accountUseCases: AccountUseCases
    private lateinit var queryService: DatabaseQueryService
    private lateinit var fakeAccounts: FakeAccountRepository
    private lateinit var fakeTx: FakeTransactionRepository
    private lateinit var viewModel: AccountEntryViewModel

    private val existingAccount = Account(
        id = "acc1",
        name = "Cash",
        currencyCode = "USD",
        structureType = AccountStructureType.CASH,
        colorHex = "#000000",
        icon = "ic",
        allowedCurrencies = listOf("USD")
    )

    @Before
    fun setup() {
        accountUseCases = mockk(relaxed = true)
        queryService = mockk(relaxed = true)
        // Edit mode aggregates balances from the query service; an empty result is enough here
        coEvery {
            queryService.aggregate(any(), any(), any(), any(), any(), any(), any())
        } returns Result.success(emptyList())

        fakeAccounts = FakeAccountRepository(listOf(existingAccount))
        fakeTx = FakeTransactionRepository()
        viewModel = AccountEntryViewModel(
            accountUseCases = accountUseCases,
            accountRepository = fakeAccounts,
            transactionRepository = fakeTx,
            computationRepository = FakeComputationRepository(),
            queryService = queryService,
            settingsRepository = FakeSettingsRepository(initialProfileId = 1, initialCurrency = "USD")
        )
    }

    /** Opens the create form in [currency], the way a new account starts. */
    private fun TestScope.openCreateForm(currency: String = "USD") {
        backgroundScope.launch { viewModel.state.collect {} }
        viewModel.initialize(prefillCurrency = currency)
        advanceUntilIdle()
    }

    /** Opens the existing account, with a USD opening balance and an orphaned EUR one. */
    private fun TestScope.openEditForm() {
        fakeTx.openingBalances["acc1|USD"] = openingBalance(1L, "1000", "USD")
        fakeTx.openingBalances["acc1|EUR"] = openingBalance(2L, "500", "EUR")
        backgroundScope.launch { viewModel.state.collect {} }
        viewModel.initialize(accountId = "acc1")
        advanceUntilIdle()
    }

    private fun openingBalance(id: Long, amount: String, currency: String) = Transaction(
        id = id,
        name = "Initial Balance",
        amount = BigDecimal(amount),
        timestamp = 1_000L,
        currency = currency,
        transactionType = TransactionType.INCOME,
        accountId = "acc1",
        categoryId = Category.SYSTEM_INITIAL_BALANCE
    )

    private fun TestScope.openBalanceSheet(currency: String = "USD") {
        viewModel.onEvent(AccountEntryEvent.OpenBalanceUpdate(currency))
        advanceUntilIdle()
    }

    // ── Issue 8: before the account exists, "By Record" is the opening balance ──

    @Test
    fun `create mode - recording a balance sets the opening balance, not an adjustment`() = runTest(testDispatcher) {
        openCreateForm()

        openBalanceSheet()
        assertEquals(BalanceUpdateMode.INITIAL_BALANCE, viewModel.state.value.balanceUpdateMode)
        viewModel.onEvent(AccountEntryEvent.SetBalanceUpdateAmount("750"))
        viewModel.onEvent(AccountEntryEvent.ConfirmBalanceUpdate)
        advanceUntilIdle()

        assertEquals("750", viewModel.state.value.initialBalances["USD"])
        assertTrue(viewModel.state.value.pendingAdjustmentDeltas.isEmpty())
    }

    @Test
    fun `create mode - recording again replaces the opening balance instead of adding to it`() = runTest(testDispatcher) {
        openCreateForm()
        openBalanceSheet()
        viewModel.onEvent(AccountEntryEvent.SetBalanceUpdateAmount("750"))
        viewModel.onEvent(AccountEntryEvent.ConfirmBalanceUpdate)
        advanceUntilIdle()

        openBalanceSheet()
        assertEquals("750", viewModel.state.value.balanceUpdateInputAmount)
        viewModel.onEvent(AccountEntryEvent.SetBalanceUpdateAmount("900"))
        viewModel.onEvent(AccountEntryEvent.ConfirmBalanceUpdate)
        advanceUntilIdle()

        assertEquals("900", viewModel.state.value.initialBalances["USD"])
    }

    @Test
    fun `create mode - saving writes the typed balance as the opening balance and no adjustments`() = runTest(testDispatcher) {
        openCreateForm()
        viewModel.onEvent(AccountEntryEvent.SetName("Cash"))
        openBalanceSheet()
        viewModel.onEvent(AccountEntryEvent.SetBalanceUpdateAmount("900"))
        viewModel.onEvent(AccountEntryEvent.ConfirmBalanceUpdate)
        advanceUntilIdle()

        viewModel.saveAccount {}
        advanceUntilIdle()

        coVerify(exactly = 1) {
            accountUseCases.createAccountWithInitialBalances(
                any(),
                match { it["USD"] == "900" },
                any(), any(), any(), any(), any(), any(), any(), any()
            )
        }
        coVerify(exactly = 0) { accountUseCases.createBalanceAdjustment(any(), any(), any(), any()) }
        coVerify(exactly = 0) { accountUseCases.saveAccountEdits(any(), any(), any(), any(), any()) }
    }

    // ── Issue 7: a second tap before the first save finishes must not repeat it ──

    @Test
    fun `a second save while the first is in flight is ignored`() = runTest(testDispatcher) {
        openCreateForm()
        viewModel.onEvent(AccountEntryEvent.SetName("Cash"))
        advanceUntilIdle()

        viewModel.saveAccount {}
        viewModel.saveAccount {}
        advanceUntilIdle()

        coVerify(exactly = 1) {
            accountUseCases.createAccountWithInitialBalances(
                any(), any(), any(), any(), any(), any(), any(), any(), any(), any()
            )
        }
    }

    @Test
    fun `saving is allowed again once the first save has finished`() = runTest(testDispatcher) {
        openCreateForm()
        viewModel.onEvent(AccountEntryEvent.SetName("Cash"))
        advanceUntilIdle()

        viewModel.saveAccount {}
        advanceUntilIdle()
        viewModel.saveAccount {}
        advanceUntilIdle()

        coVerify(exactly = 2) {
            accountUseCases.createAccountWithInitialBalances(
                any(), any(), any(), any(), any(), any(), any(), any(), any(), any()
            )
        }
    }

    // ── Issue 10: removing a currency that holds an opening balance needs confirmation ──

    @Test
    fun `removing a currency with no balance applies straight away`() = runTest(testDispatcher) {
        openCreateForm()
        viewModel.onEvent(AccountEntryEvent.SetAllowedCurrencies(setOf("USD", "EUR")))
        advanceUntilIdle()

        viewModel.onEvent(AccountEntryEvent.SetAllowedCurrencies(setOf("USD")))
        advanceUntilIdle()

        assertEquals(setOf("USD"), viewModel.state.value.allowedCurrencies)
        assertTrue(viewModel.state.value.pendingCurrencyRemoval.isEmpty())
    }

    @Test
    fun `removing a currency with a balance is held until the user confirms`() = runTest(testDispatcher) {
        openCreateForm()
        viewModel.onEvent(AccountEntryEvent.SetAllowedCurrencies(setOf("USD", "EUR")))
        viewModel.onEvent(AccountEntryEvent.SetInitialBalance("EUR", "500"))
        advanceUntilIdle()

        viewModel.onEvent(AccountEntryEvent.SetAllowedCurrencies(setOf("USD")))
        advanceUntilIdle()

        assertEquals(setOf("USD", "EUR"), viewModel.state.value.allowedCurrencies)
        assertEquals(setOf("EUR"), viewModel.state.value.pendingRemovalWithBalance)
    }

    @Test
    fun `cancelling the removal keeps the currency and its balance`() = runTest(testDispatcher) {
        openCreateForm()
        viewModel.onEvent(AccountEntryEvent.SetAllowedCurrencies(setOf("USD", "EUR")))
        viewModel.onEvent(AccountEntryEvent.SetInitialBalance("EUR", "500"))
        viewModel.onEvent(AccountEntryEvent.SetAllowedCurrencies(setOf("USD")))
        advanceUntilIdle()

        viewModel.onEvent(AccountEntryEvent.CancelCurrencyRemoval)
        advanceUntilIdle()

        assertEquals(setOf("USD", "EUR"), viewModel.state.value.allowedCurrencies)
        assertEquals("500", viewModel.state.value.initialBalances["EUR"])
        assertTrue(viewModel.state.value.pendingCurrencyRemoval.isEmpty())
    }

    @Test
    fun `confirming the removal drops the currency and zeroes its balance`() = runTest(testDispatcher) {
        openCreateForm()
        viewModel.onEvent(AccountEntryEvent.SetAllowedCurrencies(setOf("USD", "EUR")))
        viewModel.onEvent(AccountEntryEvent.SetInitialBalance("EUR", "500"))
        viewModel.onEvent(AccountEntryEvent.SetAllowedCurrencies(setOf("USD")))
        advanceUntilIdle()

        viewModel.onEvent(AccountEntryEvent.ConfirmCurrencyRemoval)
        advanceUntilIdle()

        assertEquals(setOf("USD"), viewModel.state.value.allowedCurrencies)
        assertEquals("0", viewModel.state.value.initialBalances["EUR"])
        assertTrue(viewModel.state.value.pendingCurrencyRemoval.isEmpty())
    }

    @Test
    fun `an opening balance in a currency the account no longer allows is still shown when editing`() = runTest(testDispatcher) {
        openEditForm()

        // EUR must surface for display/removal but never silently rejoin the persisted allowed set.
        assertTrue("EUR" in viewModel.state.value.displayedCurrencies)
        assertTrue("EUR" in viewModel.state.value.orphanedCurrencies)
        assertTrue("EUR" !in viewModel.state.value.allowedCurrencies)
        assertEquals("500", viewModel.state.value.initialBalances["EUR"])
    }

    @Test
    fun `saving without touching currencies does not resurrect an orphaned currency`() = runTest(testDispatcher) {
        openEditForm()

        viewModel.onEvent(AccountEntryEvent.SetName("Savings"))
        advanceUntilIdle()
        viewModel.saveAccount {}
        advanceUntilIdle()

        coVerify {
            accountUseCases.saveAccountEdits(
                match { it.allowedCurrencies == listOf("USD") },
                any(), any(), any(), any()
            )
        }
    }

    @Test
    fun `a currency with only a balance adjustment also needs confirmation before removal`() = runTest(testDispatcher) {
        fakeTx.adjustmentOnlyCurrencies += "acc1|GBP"
        backgroundScope.launch { viewModel.state.collect {} }
        viewModel.initialize(accountId = "acc1")
        advanceUntilIdle()

        assertTrue("GBP" in viewModel.state.value.orphanedCurrencies)
        viewModel.onEvent(AccountEntryEvent.SetAllowedCurrencies(setOf("USD")))
        advanceUntilIdle()

        assertEquals(setOf("GBP"), viewModel.state.value.pendingCurrencyRemoval)
    }

    @Test
    fun `saving after confirming the removal of an orphaned balance deletes it`() = runTest(testDispatcher) {
        openEditForm()
        viewModel.onEvent(AccountEntryEvent.SetAllowedCurrencies(setOf("USD")))
        advanceUntilIdle()
        viewModel.onEvent(AccountEntryEvent.ConfirmCurrencyRemoval)
        advanceUntilIdle()

        viewModel.saveAccount {}
        advanceUntilIdle()

        coVerify {
            accountUseCases.saveAccountEdits(
                any(),
                any(),
                match { it["EUR"]?.signum() == 0 && it["USD"]?.compareTo(BigDecimal("1000")) == 0 },
                any(),
                any()
            )
        }
    }

    // ── Issue 13: typing a name must not re-run the balance queries ──

    @Test
    fun `changing the name does not re-run the balance queries`() = runTest(testDispatcher) {
        openEditForm()
        clearMocks(queryService, answers = false)

        viewModel.onEvent(AccountEntryEvent.SetName("Savings"))
        advanceUntilIdle()

        coVerify(exactly = 0) { queryService.aggregate(any(), any(), any(), any(), any(), any(), any()) }
        assertEquals("Savings", viewModel.state.value.accountName)
    }

    @Test
    fun `changing a balance does re-run the balance queries`() = runTest(testDispatcher) {
        openEditForm()
        clearMocks(queryService, answers = false)

        viewModel.onEvent(AccountEntryEvent.SetInitialBalance("USD", "1200"))
        advanceUntilIdle()

        coVerify(atLeast = 1) { queryService.aggregate(any(), any(), any(), any(), any(), any(), any()) }
    }
}
